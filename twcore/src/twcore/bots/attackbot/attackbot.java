package twcore.bots.attackbot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.BallPosition;
import twcore.core.events.LoggedOn;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.SQLResultEvent;
import twcore.core.events.SoccerGoal;
import twcore.core.game.Player;
import twcore.core.game.Ship;
import twcore.core.lvz.Objset;
import twcore.core.util.Tools;

/**
 * This class runs the BallGame event. Two teams trying to score on their
 * enemy's goal. Players may use ships 1, 2, 3 or 7 only. If a team captures all
 * four flags in the middle, the doors at the enemy goal will open. First team
 * to 3 wins.
 * 
 * Made by fLaReD & WingZero.
 */
public class attackbot extends SubspaceBot {
    public BotAction ba;
    public EventRequester events; // event requester
    public OperatorList oplist; // operator list
    public BotSettings rules;
    private Objset scoreboard; 
    private Ball ball;
    private int[] safes;
    private int goals, pick, gameTime;
    private boolean autoMode; // if true then game is run with caps who volunteer via !cap
    private boolean timed;
    private boolean DEBUG;
    private String debugger;
    private LinkedList<String> notplaying;
    private LinkedList<String> lagouts;
    private String[] pastStats;
    
    private Team[] team;
    
    private MasterControl mc;
    
    private TimerTask advert;
    private long lastZoner;
    
    public static final String db = "website";
    public static final int ZONER_TIME = 5;
    public static final int MAX_CHARS = 220;
    
    // Objons
    public static final int TEN_SECONDS = 1;
    public static final int FIVE_SECONDS = 2;
    public static final int GOGOGO = 3;
    public static final int GAMEOVER = 4;
    public static final int SUDDENDEATH = 833;
    
    // Bot states
    public int state;
    public static final int OFF = -1;
    public static final int WAITING = 0;
    public static final int PICKING = 1;
    public static final int STARTING = 2;
    public static final int PLAYING = 3;
    
    public static final int NP_FREQ = 666;
    public static final int MAX_GOALS = 15;
    public static final int MAX_TIME = 30;
    public static final int SPEC_FREQ = 9999;
    public static final long MAX_POS = 15000; // 15 seconds max carry time
    public static int[] SHIP_LIMITS;
    

    /**
     * Requests events, sets up bot.
     */
    public attackbot(BotAction botAction) {
        super(botAction);
        ba = m_botAction;
        oplist = ba.getOperatorList();
        rules = ba.getBotSettings();
        events = ba.getEventRequester();
        events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(EventRequester.LOGGED_ON);
        events.request(EventRequester.MESSAGE);
        events.request(EventRequester.SOCCER_GOAL);
        events.request(EventRequester.BALL_POSITION);
        events.request(EventRequester.ARENA_JOINED);
        events.request(EventRequester.PLAYER_ENTERED);
        events.request(EventRequester.PLAYER_LEFT);
        events.request(EventRequester.PLAYER_DEATH);
        events.request(EventRequester.PLAYER_POSITION);
        notplaying  = new LinkedList<String>();
        lagouts = new LinkedList<String>();
        autoMode = false;
        pastStats = null;
        timed = true;
        gameTime = 10;
        DEBUG = false;
        debugger = "";
        lastZoner = 0;
    }

    /** Handles the LoggedOn event **/
    public void handleEvent(LoggedOn event) {
        ba.joinArena(ba.getBotSettings().getString("InitialArena"));
        ba.sendUnfilteredPublicMessage("?chat=attack");
        state = WAITING;
        goals = rules.getInt("Goals");
        safes = rules.getIntArray("Safes", ",");
        SHIP_LIMITS = rules.getIntArray("Ships", ",");
        if (SHIP_LIMITS.length != 8)
            SHIP_LIMITS = new int[] { 1, 1, rules.getInt("MaxPlayers"), 0, 2, 0, rules.getInt("MaxPlayers"), 2 };
        team = new Team[] { new Team(0), new Team(1) };
    }

    /** Handles the ArenaJoined event **/
    public void handleEvent(ArenaJoined event) {
        scoreboard = ba.getObjectSet();
        ba.setPlayerPositionUpdating(300);
        ba.setReliableKills(1);  //Reliable kills so the bot receives every packet
        try {
            mc.cancel();
        } catch (Exception e) {};
        mc = new MasterControl();
        ba.scheduleTaskAtFixedRate(mc, Tools.TimeInMillis.SECOND, Tools.TimeInMillis.SECOND);
        ball = new Ball();
        ba.toggleLocked();
        if (autoMode)
            ba.sendArenaMessage("A new game will begin when two players PM me with !cap -" + ba.getBotName(), Tools.Sound.CROWD_GEE);
        else
            ba.sendArenaMessage("Request a new game with '?help start attack please'", Tools.Sound.CROWD_GEE);
        ba.specAll();
        ba.setAlltoFreq(SPEC_FREQ);
        ba.setTimer(0);
        
        advert = new TimerTask() {
            public void run() {
                ba.sendChatMessage("Don't forget to signup for the Attack tournament by typing !signup to bot or in the Attack chat.");
                ba.sendArenaMessage("Don't forget to signup for the Attack tournament by typing !signup to bot or in the Attack chat.");
            }
        };
        ba.scheduleTask(advert, 0, 45 * Tools.TimeInMillis.MINUTE);
    }
    
    /** Handles the PlayerEntered event **/
    public void handleEvent(PlayerEntered event) {
        String name = event.getPlayerName();
        if (name == null) 
            name = ba.getPlayerName(event.getPlayerID());
        if (name == null) return;
        
        if (state == WAITING) {
            if (autoMode)
                ba.sendSmartPrivateMessage(name, "A new game will begin after two players PM me with !cap");
            else
                ba.sendSmartPrivateMessage(name, "Request a new game with '?help start attack please'");
        } else if (state == PICKING) {
            String msg = "A game is about to start. ";
            if (team[0].cap != null)
                msg += team[0].cap + " and ";
            else
                msg += "-no captain- and ";
            if (team[1].cap != null)
                msg += team[1].cap + " ";
            else
                msg += "-no captain- ";
            msg += "are picking teams.";
            ba.sendSmartPrivateMessage(name, msg);
        } else if (state >= STARTING) {
            if (timed) 
                ba.sendSmartPrivateMessage(name, "A timed game to " + gameTime + " minutes is currently being played. Score: " + team[0].score + " - " + team[1].score);
            else
                ba.sendSmartPrivateMessage(name, "A game to " + goals + " goals is currently being played. Score: " + team[0].score + " - " + team[1].score);
        }
        
        if (lagouts.contains(name.toLowerCase()))
            ba.sendSmartPrivateMessage(name, "Use !lagout to return to the game.");
        if (notplaying.contains(name.toLowerCase())) {
            ba.setFreq(name, NP_FREQ);
            ba.sendSmartPrivateMessage(name, "You are still set as not playing and captains will be unable to pick you. If you want to play, use !notplaying again.");
        }
    }
    
    /** Handles the PlayerLeft event (lagouts) **/
    public void handleEvent(PlayerLeft event) {
        if (state < PICKING) return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null) return;
        
        if (isPlaying(name) && !lagouts.contains(name.toLowerCase())) {
            lagouts.add(name.toLowerCase());
            getPlayer(name).addLagout();
        }
        Team t = getTeam(name);
        if (t != null && t.cap != null && !t.isCap(name))
            ba.sendSmartPrivateMessage(t.cap, name + " has lagged out or left the arena.");
    }

    /** Monitors goal scoring **/
    public void handleEvent(SoccerGoal event) {
        if (state == PLAYING) {
            short scoringFreq = event.getFrequency();
            if (scoringFreq == team[0].freq) {
                team[0].score++;
                String name = ball.getLastCarrierName();
                if (name != null && isNotBot(name)) {
                    Attacker p = team[0].getPlayerStats(name);
                    if (p != null)
                        p.addGoal();
                }
            } else if (scoringFreq == team[1].freq) {
                team[1].score++;
                String name = ball.getLastCarrierName();
                if (name != null && isNotBot(name)) {
                    Attacker p = team[1].getPlayerStats(name);
                    if (p != null)
                        p.addGoal();
                }
            }
            TimerTask drop = new TimerTask() {
                public void run() {
                    if (state == PLAYING) {
                        ba.sendArenaMessage("Score: " + team[0].score + " - " + team[1].score);
                        ba.resetFlagGame();
                        warpTeams();
                        dropBall();
                        mc.announceTimeLeft();
                    }
                }
            };
            ba.scheduleTask(drop, 1100);
        }
    }
    
    public void handleEvent(PlayerDeath event) {
        if (state != PLAYING) return;
        String killer = ba.getPlayerName(event.getKillerID());
        String killee = ba.getPlayerName(event.getKilleeID());
        if (killer != null && killee != null) {
            Team t = getTeam(killee);
            if (t != null)
                t.handleDeath(killee, killer);
        }
    }
    
    public void handleEvent(PlayerPosition event) {
        Player p = ba.getPlayer(event.getPlayerID());
        if (p.getShipType() != 5) return;
        Team t = getTeam(p.getPlayerName());
        if (t != null)
            t.updateTerr(p.getPlayerName(), event.getXLocation(), event.getYLocation());
        
    }

    public void handleEvent(BallPosition event) {
        ball.update(event);
    }

    /** Handles the FrequencyShipChange event indicating potential lagouts **/
    public void handleEvent(FrequencyShipChange event) {
        if (state < PICKING) return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null) return;
        if (event.getShipType() == 0 && isPlaying(name) && !lagouts.contains(name.toLowerCase())) {
            lagouts.add(name.toLowerCase());
            getPlayer(name).addLagout();
            ba.sendSmartPrivateMessage(name, "Use !lagout to return to the game.");
            Team t = getTeam(name);
            if (t != null && !t.isCap(name) && t.cap != null)
                ba.sendSmartPrivateMessage(t.cap, name + " has lagged out.");
        }
        
        
        if (state > 0) {
            byte shipType = event.getShipType();
            int playerName = event.getPlayerID();
            if (shipType == 4 || shipType == 6) {
                ba.sendPrivateMessage(playerName,
                        "This ship type is not allowed. You may use any ships besides the leviathan and the weasel.");
                ba.setShip(playerName, 1);
            }
        }
    }
    
    public void handleEvent(SQLResultEvent event) {
        String name = event.getIdentifier();
        String msg = "";
        ResultSet rs = event.getResultSet();
        try {
            if (rs.next()) {
                do
                    msg += rs.getString("fcName") + ", ";
                while (rs.next());
                msg = msg.substring(0, msg.lastIndexOf(','));
                ba.sendSmartPrivateMessage(name, "Registerd players:");
                ba.smartPrivateMessageSpam(name, wrapLines(msg));
            } else
                ba.sendSmartPrivateMessage(name, "No registered players found.");
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        } finally {
            ba.SQLClose(rs);
        }
    }

    /**
     * Command handler.
     */
    public void handleEvent(Message event) {
        String name = event.getMessager();
        if (name == null || name.length() < 1)
            name = ba.getPlayerName(event.getPlayerID());
        String msg = event.getMessage();
        int type = event.getMessageType();
        
        if (type == Message.ARENA_MESSAGE) {
            if (msg.contains("Arena UNLOCKED"))
                ba.toggleLocked();
        }
        
        if (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) {
            if (msg.equalsIgnoreCase("!caps"))
                cmd_caps(name);
            else if (msg.equalsIgnoreCase("!ready"))
                cmd_ready(name);
            else if (msg.equalsIgnoreCase("!cap"))
                cmd_cap(name);
            else if (msg.startsWith("!add "))
                cmd_add(name, msg);
            else if (msg.startsWith("!remove "))
                cmd_remove(name, msg);
            else if (msg.startsWith("!change "))
                cmd_changeShip(name, msg);
            else if (msg.startsWith("!sub "))
                cmd_sub(name, msg);
            else if (msg.startsWith("!switch "))
                cmd_switchShips(name, msg);
            else if (msg.equalsIgnoreCase("!lagout"))
                cmd_lagout(name);
            else if (msg.equalsIgnoreCase("!list"))
                cmd_list(name);
            else if (msg.equalsIgnoreCase("!notplaying") || msg.equalsIgnoreCase("!np"))
                cmd_notPlaying(name);
            else if (msg.equalsIgnoreCase("!removecap"))
                cmd_removeCap(name);
            else if (msg.startsWith("!t"))
                cmd_terrs(name);
            else if (msg.startsWith("!myf"))
                cmd_myFreq(name);

            if (oplist.isZH(name)) {
                if (msg.equalsIgnoreCase("!drop"))
                    dropBall();
                else if (msg.equalsIgnoreCase("!start"))
                    mc.startGame();
                else if (msg.startsWith("!kill"))
                    cmd_stop(name, true);
                else if (msg.startsWith("!end"))
                    cmd_stop(name, false);
                else if (msg.equalsIgnoreCase("!die"))
                    cmd_die(name);
                else if (msg.startsWith("!setcap "))
                    cmd_setCap(name, msg);
                else if (msg.equalsIgnoreCase("!debug"))
                    cmd_debug(name);
                else if (msg.startsWith("!settime "))
                    cmd_setTime(name, msg);
                else if (msg.startsWith("!setgoals "))
                    cmd_setGoals(name, msg);
                else if (msg.startsWith("!al"))
                    cmd_allTerrs(name);
                else if (msg.equalsIgnoreCase("!autocap"))
                    cmd_autocap(name);
                else if (msg.startsWith("!zone "))
                    cmd_zone(name, msg);
            }
        }
        
        if (type == Message.CHAT_MESSAGE || type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) {

            if (msg.equalsIgnoreCase("!help"))
                cmd_help(name);
            else if (msg.equalsIgnoreCase("!about"))
                cmd_about(name);
            else if (msg.equalsIgnoreCase("!status"))
                cmd_status(name);
            else if (msg.equalsIgnoreCase("!signup"))
                cmd_signup(name);
            else if (msg.equalsIgnoreCase("!count"))
                cmd_count(name);
            else if (msg.equalsIgnoreCase("!stats"))
                cmd_stats(name);
            else if (msg.equalsIgnoreCase("!rules"))
                cmd_rules(name);
            
            if (oplist.isZH(name)) {
                if (msg.startsWith("!reg"))
                    cmd_registered(name);
            }
            
            if (oplist.isSmod(name) || name.equalsIgnoreCase("diakka")) {
                if (msg.startsWith("!greet "))
                    cmd_greet(name, msg);
                else if (msg.startsWith("!per "))
                    cmd_periodic(name, msg);
                else if (msg.equalsIgnoreCase("!per"))
                    cmd_periodic(name);
            }
        }
    }

    private void cmd_zone(String name, String cmd) {
        long now = System.currentTimeMillis();
        if (now - lastZoner > ZONER_TIME * Tools.TimeInMillis.MINUTE || oplist.isSmod(name)) {
            lastZoner = now;
            String msg = "";
            if (cmd.length() > 7) {
                msg = cmd.substring(cmd.indexOf(" ") + 1);
                if (msg.toLowerCase().contains("?go")) {
                    ba.sendSmartPrivateMessage(name, "Please do not include ?go attack in the zoner as I will add this for you automatically.");
                    return;
                } else if (msg.toLowerCase().contains("-" + name.toLowerCase())) {
                    ba.sendSmartPrivateMessage(name, "Please do not include your name in the zoner as I will provide mine automatically.");
                    return;
                }
            } else
                msg = "A game of ATTACK is about to begin! Type ?go ATTACK to play. -" + ba.getBotName();
            ba.sendZoneMessage(msg, 2);
        } else {
            int mins = (int)(((ZONER_TIME * Tools.TimeInMillis.MINUTE) - (now - lastZoner)) / Tools.TimeInMillis.MINUTE);
            int secs = (int)(((ZONER_TIME * Tools.TimeInMillis.MINUTE) - (now - lastZoner) - (mins / Tools.TimeInMillis.MINUTE)) / Tools.TimeInMillis.SECOND);
            ba.sendSmartPrivateMessage(name, "Next zoner will be available in " + mins + " minutes " + secs + " seconds");
        }
    }
    
    private void cmd_greet(String name, String cmd) {
        if (cmd.length() < 8) return;
        ba.sendUnfilteredPublicMessage("?set misc:greetmessage:" + cmd.substring(cmd.indexOf(" ") + 1));
        ba.sendSmartPrivateMessage(name, "Greeting set to: " + cmd.substring(cmd.indexOf(" ") + 1));
    }

    private void cmd_about(String name) {
        String[] about = { 
                "+-- Rules of Attack ------------------------------------------------------------------------.",
                "| The goal of attack is to score more points than the opposing team in the given amount of  |",
                "| time. Work as a team to move the ball in to the enemy base and shoot the ball in to their |",
                "| goal to score a point.                                                                    |",
                "+-- Ship Restrictions --------------------------------------------------------------------- |",
                "| Teams may use a maximum of:                                                               |",
                "|  -1 Warbird          -2 Terriers                                                          |",
                "|  -1 Javelin          -0 Weasels                                                           |",
                "|  -10 Spiders         -10 Lancasters                                                       |",
                "|  -0 Leviathans       -2 Sharks                                                            |",
                "+-- Tips ---------------------------------------------------------------------------------- |",
                "| -When holding the ball, your ship will receive upgraded guns, bombs, and mines. Use this  |",
                "|  to your advantage.                                                                       |",
                "| -Use the doors located on the far end of each base as a way to escape your base with the  |",
                "|  the ball. If you are carrying the ball when the door changes states, you will be warped  |",
                "|  to your spawn area.                                                                      |",
                "| -Collect greens to help your Terriers increase bounty. When a Terrier has 20 bounty, he   |",
                "|  may attach to the other Terrier.                                                         |",
                "| -Use both of your Terriers. When one Terrier is attacking, the other terr can start a     |",
                "|  flank to help destroy the enemy. Just be sure not to leave your base unguarded!          |",
                "`-------------------------------------------------------------------------------------------'", 
        };
        ba.smartPrivateMessageSpam(name, about);
    }

    /**
     * !help Displays help message.
     */
    private void cmd_help(String name) {
        String[] help = {
                "+-- AttackBot Commands ---------------------------------------------------------------------.",
                "| !signup                  - Registers for the Attack tournament                            |",
                "| !about                   - Rules, tips and general information regarding Attack           |",
                "| !terr                    - Reports location of your team terriers (!t)                    |",
                "| !cap                     - Claims captain of a team if a team is missing a cap            |",
                "| !status                  - Displays the current game status                               |",
                "| !stats                   - Spams the current game stats (same as at the end of the game)  |",
                "| !list                    - Displays list of each teams player information                 |",
                "| !caps                    - Displays the captains of each team                             |",
                "| !np                      - Not playing toggler prevents/enables being added (!notplaying) |",
                "| !lagout                  - Returns a player to the game after lagging out                 |",
                "| !rules                   - Briefly displays the rules the bot is currently set to         |",
        };
        
        String[] cap = {
                "+-- Captain Commands -----------------------------------------------------------------------+",
                "| !myfreq                  - Moves you to your team's freq while in spec                    |",
                "| !ready                   - Use after picking when ready to begin the game                 |",
                "| !add <name>:<ship>       - Adds <name> to game in ship number <ship>                      |",
                "| !remove <name>           - Removes <name> from game (during picking)                      |",
                "| !change <name>:<ship>    - Changes <name> to ship number <ship>                           |",
                "| !switch <name1>:<name2>  - Switches the ships of <name1> and <name2>                      |",
                "| !sub <player>:<specer>   - Substitutes <player> for <specer>                              |",
                "| !list                    - Lists players, ships, and statuses for each team               |",
                "| !removecap               - Removes you from captain allowing someone else to cap          |",
        };
        
        String[] staff = {
                "+-- Staff Commands -------------------------------------------------------------------------+",
                "| !zone                    - Sends a default zoner or one that you provide                  |",
                "| !autocap                 - Allows/dissallows player set captains                          |",
                "| !setcap <team>:<name>    - Sets <name> as captain of <team> (0 or 1)                      |",
                "| !settime <mins>          - Changes game to a timed game to <mins> minutes                 |",
                "| !setgoals <goals>        - Changes game rules to first to <goals> goals wins              |",
                "| !die                     - Kills bot                                                      |",
                "| !endgame                 - Prematurely ends the current game with stats and scores        |",
                "| !killgame                - Abruptly kills the current game without a winner or stats      |",
                "| !reg                     - Short for !registered lists all the players signed up          |",
        };
        
        String[] staff2 = {
                "| !per <mins>:<message>    - Sends chat and arena messages every <mins> minutes             |",
                "| !per                     - Disable the periodic messages if currently enabled             |",
                "| !greet <message>         - Sets the arena greet message to <message>                      |",
        };
        String end = "`-------------------------------------------------------------------------------------------'";
        ba.smartPrivateMessageSpam(name, help);
        if (isCaptain(name))
            ba.smartPrivateMessageSpam(name, cap);
        if (oplist.isZH(name))
            ba.smartPrivateMessageSpam(name, staff);
        if (oplist.isSmod(name) || name.equalsIgnoreCase("diakka"))
            ba.smartPrivateMessageSpam(name, staff2);
        ba.sendSmartPrivateMessage(name, end);
    }

    private void cmd_periodic(String name) {
        if (advert != null) {
            ba.cancelTask(advert);
            advert = null;
            ba.sendSmartPrivateMessage(name, "Periodic messages have been disabled.");
        } else
            ba.sendSmartPrivateMessage(name, "No periodic messages are currently set.");
    }
    
    private void cmd_periodic(String name, String cmd) {
        if (cmd.length() < 10 || !cmd.contains(":")) return;
        int delay = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":")));
        final String msg = cmd.substring(cmd.indexOf(":") + 1);
        if (delay > 0 && msg.length() > 1) {
            if (advert != null)
                ba.cancelTask(advert);
            advert = new TimerTask() {
                public void run() {
                    ba.sendChatMessage(msg);
                    ba.sendArenaMessage(msg);
                }
            };
            ba.scheduleTask(advert, 0, delay * Tools.TimeInMillis.MINUTE);
            ba.sendSmartPrivateMessage(name, "Periodic every " + delay + "min: " + msg);
        } else
            ba.sendSmartPrivateMessage(name, "ERROR: Delay and message must be greater than 0.");
    }
    
    private void cmd_signup(String name) {
        ResultSet rs = null;
        
        try {
            rs = ba.SQLQuery(db, "SELECT ftUpdated as t FROM tblAttack WHERE fcName = '" + Tools.addSlashesToString(name) + "' LIMIT 1");
            if (rs.next()) {
                String t = rs.getString("t");
                t = t.substring(0, 10) + " at " + t.substring(11, 16);
                ba.sendSmartPrivateMessage(name, "You already signed up on " + t + ".");
            } else {
                ba.SQLBackgroundQuery(db, null, "INSERT INTO tblAttack (fcName, ftUpdated) VALUES('" + Tools.addSlashesToString(name) + "', NOW())");
                ba.sendSmartPrivateMessage(name, "Signup successful!");
            }
        } catch (SQLException e) {
            ba.sendSmartPrivateMessage(name, "An error occured and your signup did not complete.");
            Tools.printStackTrace(e);
        } finally {
            ba.SQLClose(rs);
        }
    }
    
    private void cmd_count(String name) {
        ResultSet rs = null;
        try {
            rs = ba.SQLQuery(db, "SELECT COUNT(fnAttackID) as c FROM tblAttack");
            
            if (rs.next())
                ba.sendSmartPrivateMessage(name, "Total players registerd: " + rs.getInt("c"));
            else
                ba.sendSmartPrivateMessage(name, "No players have signed up.");
        } catch (SQLException e) {
            ba.sendSmartPrivateMessage(name, "An error occured and your request could not be completed.");
            Tools.printStackTrace(e);
        } finally {
            ba.SQLClose(rs);
        }
    }
    
    private void cmd_registered(String name) {
        ba.sendSmartPrivateMessage(name, "Processing request...");
        ba.SQLBackgroundQuery(db, name, "SELECT fcName FROM tblAttack ORDER BY ftUpdated ASC");
    }
    
    private void cmd_rules(String name) {
        if (timed)
            ba.sendSmartPrivateMessage(name, "RULES: Timed game to " + gameTime + " minutes most goals wins or sudden death. 10 players in basing ships per team. Ship limits: 1WB 1JAV 2TERR 2SHARK");
        else
            ba.sendSmartPrivateMessage(name, "RULES: First to " + goals + " goals wins. Max of 10 players in basing ships per team. Ship limits: 1WB 1JAV 2TERR 2SHARK");
    }
    
    /** Handles the !notplaying command which prevents a player from being added and/or removes them from the game **/
    private void cmd_notPlaying(String name) {
        if (!isPlaying(name)) {
            if (!notplaying.contains(name.toLowerCase())) {
                notplaying.add(name.toLowerCase());
                ba.setFreq(name, NP_FREQ);
                ba.sendSmartPrivateMessage(name, "You have been added to not playing. Captains will not be able to add you. If you wish to return, do !notplaying again.");
            } else {
                notplaying.remove(name.toLowerCase());
                ba.setShip(name, 1);
                ba.specWithoutLock(name);
                ba.sendSmartPrivateMessage(name, "You have been removed from not playing. Captains will be able to add you.");
            }
        } else {
            getTeam(name).notPlaying(name);
            ba.setFreq(name, NP_FREQ);
            ba.sendSmartPrivateMessage(name, "You have been added to not playing. Captains will not be able to add you. If you wish to return, do !notplaying again.");
        }
    }
    
    private void cmd_myFreq(String name) {
        if (!isCaptain(name)) return;
        Team t = getTeam(name);
        if (t != null)
            ba.setFreq(name, t.freq);
    }
    
    /** Handles the !setcap command which assigns a player as captain of a team (0 or 1) **/
    private void cmd_setCap(String name, String cmd) {
        if (cmd.length() < 8 || !cmd.contains(" ") || !cmd.contains(":")) return;
        String cap = ba.getFuzzyPlayerName(cmd.substring(cmd.indexOf(":") + 1));
        if (cap == null) {
            ba.sendSmartPrivateMessage(name, "Player not found in this arena.");
            return;
        }
        
        if (isCaptain(cap)) {
            ba.sendSmartPrivateMessage(name, cap + " is already the captain of a team.");
            return;
        } else if (!isNotBot(cap)) {
            ba.sendSmartPrivateMessage(name, cap + " is a bot.");
            return;
        }
        
        try {
            int freq = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":")));
            if (freq == 0 || freq == 1) {
                if (isPlaying(cap) && getTeam(cap).freq != freq) {
                    ba.sendSmartPrivateMessage(name, cap + " is playing on the other team.");
                    return;
                }
                team[freq].cap = cap;
                ba.sendArenaMessage(cap + " has been assigned captain of freq " + team[freq].freq, Tools.Sound.BEEP1);
            }
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(name, "Invalid team number (must be 0 or 1).");
        }
    }
    
    /**
     * Handles the !cap command which will assign the sender as captain of a team without a captain
     * if a game or picking has not yet begun. Otherwise, if a captain has left the arena then the
     * sender will replace the absent captain unless sender is on opposite team.
     * @param name
     */
    private void cmd_cap(String name) {
        if (isCaptain(name) || !autoMode) {
            cmd_caps(name);
            return;
        }
        if (state == WAITING) {
            if (team[0].cap == null) {
                team[0].cap = name;
                ba.sendArenaMessage(name + " has been assigned captain of Freq " + team[0].freq, Tools.Sound.BEEP1);
            } else if (team[1].cap == null) {
                team[1].cap = name;
                ba.sendArenaMessage(name + " has been assigned captain of Freq " + team[1].freq, Tools.Sound.BEEP1);                
            } else {
                cmd_caps(name);
            }
        } else if (!isCaptain(name)) {
            if (isPlaying(name)) {
                Team t = getTeam(name);
                if (ba.getFuzzyPlayerName(t.cap) == null) {
                    ba.sendArenaMessage(name + " has replaced " + t.cap + " as captain of Freq " + t.freq);
                    t.cap = name;
                } else
                    cmd_caps(name);
            } else {
                if (ba.getFuzzyPlayerName(team[0].cap) == null) {
                    ba.sendArenaMessage(name + " has replaced " + team[0].cap + " as captain of Freq " + team[0].freq);
                    team[0].cap = name;
                } else if (ba.getFuzzyPlayerName(team[1].cap) == null) {
                    ba.sendArenaMessage(name + " has replaced " + team[1].cap + " as captain of Freq " + team[1].freq);
                    team[1].cap = name;
                } else
                    cmd_caps(name);
            }
        }
    }
    
    /** Handles the !removecap command which removes the captain from being captain **/
    private void cmd_removeCap(String name) {
        if (!isCaptain(name)) return;
        getTeam(name).removeCap();
    }
    
    /** Handles the !caps command which returns the current team captains **/
    private void cmd_caps(String name) {
        String msg = "";
        if (team[0].cap != null)
            msg = team[0].cap + " is captain of Freq " + team[0].freq + ".";
        else msg = "Captain needed for Freq " + team[0].freq;
        ba.sendSmartPrivateMessage(name, msg);
        if (team[1].cap != null)
            msg = team[1].cap + " is captain of Freq " + team[1].freq + ".";
        else msg = "Captain needed for Freq " + team[1].freq;
        ba.sendSmartPrivateMessage(name, msg);
    }
    
    /** Handles the !list command which sends a list of team players with ships and statuses **/
    private void cmd_list(String name) {
        team[0].sendTeam(name);
        ba.sendSmartPrivateMessage(name, "`");
        team[1].sendTeam(name);
    }

    /** Handles the !add command if given by a captain **/
    private void cmd_add(String name, String msg) {
        if (state < PICKING || !isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 5) return;
        Team t = getTeam(name);
        if (t == null) return;
        if (state == PICKING && !t.pick) {
            ba.sendSmartPrivateMessage(name, "It is not your turn");
            return;
        }
        String player = msg.substring(msg.indexOf(" ") + 1, msg.indexOf(":"));
        int ship = 3;
        try {
            ship = Integer.valueOf(msg.substring(msg.indexOf(":") + 1).trim());
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(name, "Invalid ship number");
            return;
        }
        if (ship > 0 && ship < 9) {
            String res = t.addPlayer(player, ship);
            ba.sendSmartPrivateMessage(name, res);
        }
    }

    /** Handles the !remove command if given by a captain **/
    private void cmd_remove(String name, String msg) {
        if (state != PICKING || !isCaptain(name) || !msg.contains(" ") || msg.length() < 8) return;
        Team t = getTeam(name);
        if (t == null) return;
        String res = t.remove(msg.substring(msg.indexOf(" ") + 1));
        if (res != null)
            ba.sendSmartPrivateMessage(name, res);
        
    }

    /** Handles the !sub command if given by a captain **/
    private void cmd_sub(String name, String msg) {
        if (state < STARTING|| !isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 5) return;
        Team t = getTeam(name);
        if (t == null) return;
        String[] players = msg.substring(msg.indexOf(" ") + 1).split(":");
        String res = t.subPlayer(players[0], players[1]);
        if (res != null)
            ba.sendSmartPrivateMessage(name, res);
    }
    
    /** Handles the !change command if given by a captain **/
    private void cmd_changeShip(String name, String msg) {
        if (!isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 8) return;
        Team t = getTeam(name);
        if (t == null) return;
        String player = msg.substring(msg.indexOf(" ") + 1, msg.indexOf(":"));
        int ship = 3;
        try {
            ship = Integer.valueOf(msg.substring(msg.indexOf(":") + 1).trim());
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(name, "Invalid ship number");
            return;
        }
        if (ship > 0 && ship < 9) {
            String res = t.changeShip(player, ship);
            if (res != null)
                ba.sendSmartPrivateMessage(name, res);
        }
    }
    
    /** Handles the !switch command if given by a captain **/
    private void cmd_switchShips(String name, String msg) {
        if (!isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 8) return;
        Team t = getTeam(name);
        if (t == null) return;
        String[] players = msg.substring(msg.indexOf(" ") + 1).split(":");
        String res = t.switchPlayers(players[0], players[1]);
        if (res != null)
            ba.sendSmartPrivateMessage(name, res);
    }
    
    /** Handles the !ready command if given by a team captain **/
    private void cmd_ready(String name) {
        if (state != PICKING || !isCaptain(name)) return;
        if (team[0].isCap(name)) {
            if (team[0].ready) {
                team[0].ready = false;
                ba.sendArenaMessage("Freq " + team[0].freq + " is not ready", Tools.Sound.BEEP1);
            } else {
                team[0].ready = true;
                ba.sendArenaMessage("Freq " + team[0].freq + " is ready", Tools.Sound.BEEP1);
            }
        } else if (team[1].isCap(name)) {
            if (team[1].ready) {
                team[1].ready = false;
                ba.sendArenaMessage("Freq " + team[1].freq + " is not ready", Tools.Sound.BEEP1);
            } else {
                team[1].ready = true;
                ba.sendArenaMessage("Freq " + team[1].freq + " is ready", Tools.Sound.BEEP1);
            }
        }
    }
    
    /** Returns a lagged out player to the game **/
    private void cmd_lagout(String name) {
        if (lagouts.contains(name.toLowerCase())) {
            lagouts.remove(name.toLowerCase());
            Team t = getTeam(name);
            if (t == null) return;
            ba.setShip(name, t.getShip(name));
            ba.setFreq(name, t.freq);
        } else 
            ba.sendSmartPrivateMessage(name, "You are not lagged out and/or not in the game.");
    }
    
    private void cmd_terrs(String name) {
        Team t = getTeam(name);
        if (t != null)
            t.locateTerrs(name);
    }
    
    private void cmd_allTerrs(String name) {
        ba.sendSmartPrivateMessage(name, "Freq " + team[0].freq + " terrs:");
        team[0].locateTerrs(name);
        ba.sendSmartPrivateMessage(name, "Freq " + team[1].freq + " terrs:");
        team[1].locateTerrs(name);
    }

    /** Handles !die command which kills the bot **/
    private void cmd_die(String name) {
        if (state > WAITING) {
            cmd_stop(name, true);
        }
        ba.sendSmartPrivateMessage(name, "I'm melting! I'm melting...");
        ba.cancelTasks();
        ba.die();
    }

    /** Handles !setgoals which will change the game to first to goals win if possible **/
    private void cmd_setGoals(String name, String cmd) {
        if (cmd.length() < 10 || !cmd.contains(" ")) return;
        int winGoal = 0;
        try {
            winGoal = Integer.valueOf(cmd.substring(cmd.indexOf(" ")+1));
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(name, "Syntax error: please use !setgoals <goals>");
            return;
        }
        if (winGoal < 1 || winGoal > MAX_GOALS) {
            ba.sendSmartPrivateMessage(name, "Goals can only be set between 1 and " + MAX_GOALS + ".");
            return;
        } 
        if (!timed) {
            if (goals != winGoal) {
                if (state == PLAYING && (winGoal <= team[0].score || winGoal <= team[1].score))
                    ba.sendSmartPrivateMessage(name, "Setting goals to " + winGoal + " conflicts with the current game score.");
                else {
                    goals = winGoal;
                    ba.sendArenaMessage("Game type set to first to " + goals + " goals");                    
                }
            } else
                ba.sendSmartPrivateMessage(name, "Goals already set to " + goals + ".");
        } else {
            if (state == PLAYING)
                ba.sendSmartPrivateMessage(name, "Game type cannot be changed to goals while a timed game is being played.");
            else {
                timed = false;
                goals = winGoal;
                ba.sendArenaMessage("Game type changed to first to " + goals + " goals"); 
            }
        }
    }
    
    /** Handles !settime which will change the game to a timed game if possible **/
    private void cmd_setTime(String name, String cmd) {
        if (cmd.length() < 9 || !cmd.contains(" ")) return;
        int mins = 0;
        try {
            mins = Integer.valueOf(cmd.substring(cmd.indexOf(" ")+1));
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(name, "Syntax error: please use !settime <minutes>");
            return;
        }
        if (mins < 2 || mins > MAX_TIME) {
            ba.sendSmartPrivateMessage(name, "Timed game can only be between 2 and " + MAX_TIME + " minutes.");
        } else if (timed && mins == gameTime) {
            ba.sendSmartPrivateMessage(name, "Time already set to " + gameTime + " minutes.");
        } else if (state == PLAYING) {
            ba.sendSmartPrivateMessage(name, "Game type cannot be changed to timed if a game is being played."); 
        } else if (timed) {
            gameTime = mins;
            ba.sendArenaMessage("Game type changed to " + gameTime + " minute timed");
        } else {
            timed = true;
            gameTime = mins;
            ba.sendArenaMessage("Game type changed to " + gameTime + " minute timed");
        }
    }
    
    /** Handles !stats which spams private messages with the current game stats just like at the end of the game **/
    private void cmd_stats(String name) {
        if (pastStats != null) {
            ba.smartPrivateMessageSpam(name, pastStats);
            return;
        }
        String msg = "Result of Freq " + team[0].freq + " vs. Freq " + team[1].freq + ": " + team[0].score + " - " + team[1].score;
        ba.sendSmartPrivateMessage(name, msg);
        String[] msgs = {
                ",-------------------------------+------+------+------+------+----+--------+--------+----------+----.",
                "|                             S |    K |    D |   TK |  TeK | LO |     TO | Steals |  Poss(s) |  G |",
                "|                          ,----+------+------+------+------+----+--------+--------+----------+----+",
                "| Freq " + team[0].freq + "                  /     |" + padNumber(team[0].getKills(), 5) + " |" + padNumber(team[0].getDeaths(), 5) + " |" + padNumber(team[0].getTeamKills(), 5) + " |" + padNumber(team[0].getTerrKills(), 5) + " |" + padNumber(team[0].getLagouts(), 3) + " |" + padNumber(team[0].getTurnovers(), 7) + " |"+ padNumber(team[0].getSteals(), 7) + " |" + padNumber(team[0].getPossession(), 9) + " |" + padNumber(team[0].score, 3) + " |",
                "+------------------------'      |      |      |      |      |    |        |        |          |    |",
        };
        ba.smartPrivateMessageSpam(name, msgs);
        for (Attacker p : team[0].stats.values()) {
            msg = "|  " + padString(p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |";
            ba.sendSmartPrivateMessage(name, msg);
        }
        for (Attacker p : team[0].oldStats) {
            msg = "|  " + padString("-" + p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |";
            ba.sendSmartPrivateMessage(name, msg);
        }
        
        msgs = new String[] {
                "+-------------------------------+------+------+------+------+----+--------+--------+----------+----+",
                "|                          ,----+------+------+------+------+----+--------+--------+----------+----+",
                "| Freq " + team[1].freq + "                  /     |" + padNumber(team[1].getKills(), 5) + " |" + padNumber(team[1].getDeaths(), 5) + " |" + padNumber(team[1].getTeamKills(), 5) + " |" + padNumber(team[1].getTerrKills(), 5) + " |" + padNumber(team[1].getLagouts(), 3) + " |" + padNumber(team[1].getTurnovers(), 7) + " |"+ padNumber(team[1].getSteals(), 7) + " |" + padNumber(team[1].getPossession(), 9) + " |" + padNumber(team[1].score, 3) + " |",
                "+------------------------'      |      |      |      |      |    |        |        |          |    |",
        };
        ba.smartPrivateMessageSpam(name, msgs);
        for (Attacker p : team[1].stats.values()) {
            msg = "|  " + padString(p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |";
            ba.sendSmartPrivateMessage(name, msg);
        }
        for (Attacker p : team[1].oldStats) {
            msg = "|  " + padString("-" + p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |";
            ba.sendSmartPrivateMessage(name, msg);
        }
        ba.sendSmartPrivateMessage(name, "`-------------------------------+------+------+------+------+----+--------+--------+----------+----'");
    }

    /**
     * Handles the !kill and !end (game) commands. !killgame destroys the game and !endgame 
     * ends the game as if it had ended naturally by printing winner scores and stats.
     * @param name
     * @param kill true if !killgame otherwise !endgame with stats
     */
    private void cmd_stop(String name, boolean kill) {
        if (state > 0) {
            if (kill) {
                scoreboard.hideAllObjects();
                ba.setObjects();
                state = WAITING;
                ba.sendArenaMessage("This game has been killed by " + name);
                ba.setTimer(0);
                lagouts.clear();
                ba.specAll();
                team[0].reset();
                team[1].reset();
            } else if (state == PLAYING) {
                if (team[0].score > team[1].score)
                    gameOver(team[0].freq);
                else if (team[1].score > team[0].score)
                    gameOver(team[1].freq);
                else
                    gameOver(-1);
            }
        } else if (state == WAITING || state == OFF)
            ba.sendSmartPrivateMessage(name, "There is no game currently running.");
    }

    /** Handles the !status command which displays the score if a game is running **/
    private void cmd_status(String name) {
        if (state == PLAYING) {
            ba.sendSmartPrivateMessage(name, "[---  SCORE  ---]");
            ba.sendSmartPrivateMessage(name, "[--Freq 0: " + team[0].score + " --]");
            ba.sendSmartPrivateMessage(name, "[--Freq 1: " + team[1].score + " --]");
        } else if (state == WAITING) {
            if (autoMode)
                ba.sendSmartPrivateMessage(name, "A new game will begin after two players volunteer to captain using !cap");
            else 
                ba.sendSmartPrivateMessage(name, "There is no game currently running.");
        } else if (state == PICKING) {
            String msg = "We are currently picking teams. Captains: ";
            if (team[0].cap != null)
                msg += team[0].cap;
            else
                msg += "[needs captain]";
            msg += " vs ";
            if (team[1].cap != null)
                msg += team[1].cap;
            else
                msg += "[needs captain]";
            ba.sendSmartPrivateMessage(name, msg);
        } else if (state == STARTING) {
            ba.sendSmartPrivateMessage(name, "We are about to start a new game.");
        }
    }   
    
    /** Handles the !autocap command which toggles staff set captains and player set captains **/
    private void cmd_autocap(String name) {
        autoMode = !autoMode;
        if (autoMode)
            ba.sendSmartPrivateMessage(name, "Captains can now be set by players using !cap.");
        else
            ba.sendSmartPrivateMessage(name, "Captains can now only be set by staff.");
    }
    
    private String getLocation(short x, short y, boolean team0) {
        if (x == -1 || y == -1)
            return " location unknown";
        final int SPAWN = 0;
        final int LEFT = 1;
        final int RIGHT = 2;
        final int ENTRANCE = 0;
        final int FRONT = 1;
        final int BACK = 2;
        final int MIDDLE = 0;
        final int TOP = 1;
        final int BOTTOM = 2;
        String pos = " ";
        int base = -1; // spawn, left, right
        int hor = -1;  // entrance, front, back
        int ver = -1;  // center, top, bottom

        debug("" + x + " and " + y);
        if (y > 8000 && y < 8384)
            ver = MIDDLE;
        else if (y < 8000)
            ver = TOP;
        else if (y > 8384)
            ver = BOTTOM;
        
        if (x < 7440) {
            base = LEFT;
            if (x > 6080 && y > 7680 && y < 8800)
                hor = ENTRANCE;
            else if (x > 5120)
                hor = FRONT;
            else
                hor = BACK;
                
            if (hor == ENTRANCE) {
                pos = "ENTRANCE";
            } else if (hor == FRONT) {
                if (ver == MIDDLE)
                    pos = "GOAL ROOM";
                else if (ver == TOP)
                    pos = "NORTH FRONT corner of base";
                else if (ver == BOTTOM)
                    pos = "SOUTH FRONT corner of base";
            } else if (hor == BACK) {
                if (ver == TOP)
                    pos = "NORTH BACK end of base";
                else if (ver == BOTTOM) 
                    pos = "SOUTH BACK end of base";
                else
                    pos = "CENTRAL BACK end of base";
            }
        } else if (x > 8944) {
            base = RIGHT;
            if (x < 10304 && y > 7680 && y < 8800)
                hor = ENTRANCE;
            else if (x < 11264)
                hor = FRONT;
            else
                hor = BACK;
            
            if (hor == ENTRANCE) {
                pos = "ENTRANCE";
            } else if (hor == FRONT) {
                if (ver == MIDDLE)
                    pos = "GOAL ROOM";
                else if (ver == TOP)
                    pos = "NORTH FRONT corner of base";
                else if (ver == BOTTOM) {
                    pos = "SOUTH FRONT corner of base";
                }
            } else if (hor == BACK) {
                if (ver == TOP)
                    pos = "NORTH BACK end of base";
                else if (ver == BOTTOM) 
                    pos = "SOUTH BACK end of base";
                else
                    pos = "CENTRAL BACK end of base";
            }
        } else {
            base = SPAWN;
            pos = " in SPAWN AREA";
        }
        
        if (base != SPAWN) {
            if (base == LEFT) {
                if (team0)
                    pos = " @ HOME in " + pos;
                else
                    pos = " @ ENEMY in " + pos;
            } else if (base == RIGHT) {
                if (team0)
                    pos = " @ ENEMY in " + pos;
                else
                    pos = " @ HOME in " + pos;
            }
        }
        return pos;
    }
    
    /**
     * Gets the Team object of a player
     * @param name
     * @return Team object or null if player is not on a team
     */
    private Team getTeam(String name) {
        if (!isNotBot(name)) return null;
        if (team[0].isPlayersTeam(name) || team[0].isCap(name))
            return team[0];
        else if (team[1].isPlayersTeam(name) || team[1].isCap(name))
            return team[1];
        else return null;
    }

    /**
     * Gets the Player object from the team of a player
     * @param name
     * @return Player object or null if player is not in the game
     */
    private Attacker getPlayer(String name) {
        if (!isNotBot(name)) return null;
        Attacker p = team[0].getPlayerStats(name);
        if (p == null)
            p = team[1].getPlayerStats(name);
        return p;
    }
    
    /** Determines if the given player is playing in the game or not **/
    private boolean isPlaying(String name) {
        if (state != WAITING && team[0].isPlayersTeam(name) || team[1].isPlayersTeam(name))
            return true;
        else
            return false;
    }
    
    /** Determines if the given player is assigned as captain of a team **/
    private boolean isCaptain(String name) {
        return team[0].isCap(name) || team[1].isCap(name);
    }    
    
    /** Helper determines if player is a bot (oplist doesn't work for bots on other cores) **/
    private boolean isNotBot(String name) {
        if (oplist.isBotExact(name) || (oplist.isSysop(name) && !oplist.isOwner(name) && !name.equalsIgnoreCase("wiibimbo") && !name.equalsIgnoreCase("Dral") && !name.equalsIgnoreCase("flared") && !name.equalsIgnoreCase("Witness") && !name.equalsIgnoreCase("Pure_Luck")))
            return false;
        else return true;
    }
    
    /** Helper method adds spaces to a string to meet a certain length **/
    private String padString(String str, int length) {
        for (int i = str.length(); i < length; i++)
            str += " ";
        return str.substring(0, length);
    }
    
    private void warpTeams() {
        ba.shipResetAll();
        team[0].teamWarp();
        team[1].teamWarp();
    }
    
    /** Helper method prints all the game statistics to an array for temporary post game access **/
    private void printStats() {
        ArrayList<String> statArray = new ArrayList<String>();
        statArray.add("Result of Freq " + team[0].freq + " vs. Freq " + team[1].freq + ": " + team[0].score + " - " + team[1].score);

        statArray.add(",-------------------------------+------+------+------+------+----+--------+--------+----------+----.");
        statArray.add("|                             S |    K |    D |   TK |  TeK | LO |     TO | Steals |  Poss(s) |  G |");
        statArray.add("|                          ,----+------+------+------+------+----+--------+--------+----------+----+");
        statArray.add("| Freq " + team[0].freq + "                  /     |" + padNumber(team[0].getKills(), 5) + " |" + padNumber(team[0].getDeaths(), 5) + " |" + padNumber(team[0].getTeamKills(), 5) + " |" + padNumber(team[0].getTerrKills(), 5) + " |" + padNumber(team[0].getLagouts(), 3) + " |" + padNumber(team[0].getTurnovers(), 7) + " |"+ padNumber(team[0].getSteals(), 7) + " |" + padNumber(team[0].getPossession(), 9) + " |" + padNumber(team[0].score, 3) + " |");
        statArray.add("+------------------------'      |      |      |      |      |    |        |        |          |    |");

        for (Attacker p : team[0].stats.values())
            statArray.add("|  " + padString(p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |");

        for (Attacker p : team[0].oldStats)
            statArray.add("|  " + padString("-" + p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |");

        statArray.add("+-------------------------------+------+------+------+------+----+--------+--------+----------+----+");
        statArray.add("|                          ,----+------+------+------+------+----+--------+--------+----------+----+");
        statArray.add("| Freq " + team[1].freq + "                  /     |" + padNumber(team[1].getKills(), 5) + " |" + padNumber(team[1].getDeaths(), 5) + " |" + padNumber(team[1].getTeamKills(), 5) + " |" + padNumber(team[1].getTerrKills(), 5) + " |" + padNumber(team[1].getLagouts(), 3) + " |" + padNumber(team[1].getTurnovers(), 7) + " |"+ padNumber(team[1].getSteals(), 7) + " |" + padNumber(team[1].getPossession(), 9) + " |" + padNumber(team[1].score, 3) + " |");
        statArray.add("+------------------------'      |      |      |      |      |    |        |        |          |    |");

        for (Attacker p : team[1].stats.values())
            statArray.add("|  " + padString(p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |");

        for (Attacker p : team[1].oldStats)
            statArray.add("|  " + padString("-" + p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |");
        statArray.add("`-------------------------------+------+------+------+------+----+--------+--------+----------+----'");
        pastStats = statArray.toArray(new String[10]);
    }
    
    /** Helper method prints all the game statistics usually after game ends **/
    private void printTotals() {
        String msg = "Result of Freq " + team[0].freq + " vs. Freq " + team[1].freq + ": " + team[0].score + " - " + team[1].score;
        ba.sendArenaMessage(msg);
        HashMap<String, Attacker> stats = team[0].getStatMap();
        String[] msgs = {
                "+---------------------------------+------+------+------+----+--------+--------+----------+----+",
                "|                               K |    D |   TK |  TeK | LO |     TO | Steals |  Poss(s) |  G |",
                "|                          ,------+------+------+------+----+--------+--------+----------+----+",
                "| Freq " + team[0].freq + "                  / " + padNumber(team[0].getKills(), 5) + " |" + padNumber(team[0].getDeaths(), 5) + " |" + padNumber(team[0].getTeamKills(), 5) + " |" + padNumber(team[0].getTerrKills(), 5) + " |" + padNumber(team[0].getLagouts(), 3) + " |" + padNumber(team[0].getTurnovers(), 7) + " |"+ padNumber(team[0].getSteals(), 7) + " |" + padNumber(team[0].getPossession(), 9) + " |" + padNumber(team[0].score, 3) + " |",
                "+------------------------'        |      |      |      |    |        |        |          |    |",
        };
        ba.arenaMessageSpam(msgs);
        for (Attacker p : stats.values()) {
            if (p.ship == -1)
                msg = "|  " + padString("-" + p.name, 25) + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |";
            else
                msg = "|  " + padString(p.name, 25) + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |";
            ba.sendArenaMessage(msg);
        }
        stats = team[1].getStatMap();
        msgs = new String[] {
                "+---------------------------------+------+------+------+----+--------+--------+----------+----+",
                "|                          ,------+------+------+------+----+--------+--------+----------+----+",
                "| Freq " + team[1].freq + "                  / " + padNumber(team[1].getKills(), 5) + " |" + padNumber(team[1].getDeaths(), 5) + " |" + padNumber(team[1].getTeamKills(), 5) + " |" + padNumber(team[1].getTerrKills(), 5) + " |" + padNumber(team[1].getLagouts(), 3) + " |" + padNumber(team[1].getTurnovers(), 7) + " |"+ padNumber(team[1].getSteals(), 7) + " |" + padNumber(team[1].getPossession(), 9) + " |" + padNumber(team[1].score, 3) + " |",
                "+------------------------'        |      |      |      |    |        |        |          |    |",
        };
        ba.arenaMessageSpam(msgs);
        for (Attacker p : stats.values()) {
            if (p.ship == -1)
                msg = "|  " + padString("-" + p.name, 25) + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |";
            else
                msg = "|  " + padString(p.name, 25) + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 3) + " |";
            ba.sendArenaMessage(msg);
        }
        
        ba.sendArenaMessage("`---------------------------------+------+------+------+----+--------+--------+----------+----'");
    }
    
    /**
     * Helper method prints winning freq and game stats in arena messages
     * @param freq Winning freq
     */
    private void gameOver(int freq) {
        ba.showObject(4);
        printTotals();
        printStats();
        if (freq > -1)
            ba.sendArenaMessage("GAME OVER: Freq " + freq + " wins!", 5);
        else
            ba.sendArenaMessage("GAME OVER: Tie!", 5);
        ba.sendArenaMessage("Final score: " + team[0].score + " - " + team[1].score + "  Detailed stats (!stats) available until picking starts");
        state = WAITING;
        lagouts.clear();
        if (autoMode)
            ba.sendArenaMessage("A new game will begin when two players PM me !cap -" + ba.getBotName());
        team[0].reset();
        team[1].reset();
        
        TimerTask sb = new TimerTask() {
            public void run() {
                scoreboard.hideAllObjects();
                ba.setObjects();
            }
        };
        ba.scheduleTask(sb, 3000);
        ba.sendChatMessage("A game of ATTACK has just ended, so a new one will start soon! ?go ATTACK");
    }

    /** Bot grabs the ball regardless of its status and drops it into the center after warping each team **/
    public void dropBall() {
        Ship s = ba.getShip();
        s.setShip(0);
        s.setFreq(1234);
        final TimerTask drop = new TimerTask() {
            public void run() {
                ba.getShip().move(512 * 16, 600 * 16);
                ba.getShip().sendPositionPacket();
                try {
                    Thread.sleep(75);
                } catch (InterruptedException e) {}
                ba.getBall(ball.getBallID(), (int) ball.getTimeStamp());
                ba.getShip().move(512 * 16, 512 * 16);
                ba.getShip().sendPositionPacket();
                try {
                    Thread.sleep(75);
                } catch (InterruptedException e) {}
            }
        };
        drop.run();
        s.setShip(8);
        ba.specWithoutLock(ba.getBotName());
        ball.clear();
        ba.setPlayerPositionUpdating(300);
    }

    /** MasterControl is responsible for the start and end of the game as well as time keeping if game is timed or goal checking if not **/
    private class MasterControl extends TimerTask {

        boolean suddenDeath;
        int timer; // countdown in seconds
        
        public MasterControl() {
            suddenDeath = false;
            timer = 0;
        }
        
        public void run() {
            switch (state) {
                case OFF : break;
                case WAITING : checkCaps(); break;
                case PICKING : nextPick(); break;
                case STARTING : preGame(); break;
                case PLAYING : tick(); break;
            }
        }
        
        private void tick() {
            // Called every single second of the game
            updateScoreboard();
            if (timed) {
                timer--;
                if (timer < 1)
                    checkGameScore();
                else if (timer < 5)
                    ba.setTimer(0);
            } else
                checkGameScore();
        }
        
        private void preGame() {
            timer--;
            if (timer == 5)
                ba.showObject(FIVE_SECONDS);
            if (timer < 1)
                runGame();
        }
        
        /** Begins the player picking process if both team's have a captain **/
        private void checkCaps() {
            if (team[0].cap != null && team[1].cap != null) {
                if (state == WAITING)
                    ba.specAll();
                state = PICKING;
                pick = 0;
                team[0].pick = true;
                team[1].pick = false;
                ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);
            }
        }
        
        /** Determines which team should have the next pick turn and executes **/
        private void nextPick() {
            if (!team[0].pick && !team[1].pick && (!team[0].isFull() || !team[1].isFull())) {
                if (team[1].size() < team[0].size()) {
                    pick = 1;
                    team[1].pick = true;
                    team[0].pick = false;
                    ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);  
                } else if (team[0].size() < team[1].size()){
                    pick = 0;
                    team[0].pick = true;
                    team[1].pick = false;
                    ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);  
                } else if (pick == 0) {
                    pick = 1;
                    team[1].pick = true;
                    team[0].pick = false;
                    ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);  
                } else {
                    pick = 0;
                    team[0].pick = true;
                    team[1].pick = false;
                    ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);
                }
            }
            if (team[0].ready && team[1].ready)
                startGame();
        }
        
        /**
         * Updates the scoreboard
         */
        private void updateScoreboard() {
            scoreboard.hideAllObjects();

            String lastPos = ball.peekLastCarrierName();
            //Flag status
            if (lastPos == null || !isNotBot(lastPos)) {
                scoreboard.showObject(740);
                scoreboard.showObject(742);
                scoreboard.hideObject(741);
                scoreboard.hideObject(743);
            } else if(team[0].isPlayersTeam(lastPos)) {
                scoreboard.showObject(740);
                scoreboard.showObject(743);
                scoreboard.hideObject(741);
                scoreboard.hideObject(742);
            } else if(team[1].isPlayersTeam(lastPos)) {
                scoreboard.showObject(741);
                scoreboard.showObject(742);
                scoreboard.hideObject(743);
                scoreboard.hideObject(740);
            }
            
            String scoreTeam1 = "" + team[0].score;
            String scoreTeam2 = "" + team[1].score;

            for (int i = scoreTeam1.length() - 1; i > -1; i--)
                scoreboard.showObject(Integer.parseInt("" + scoreTeam1.charAt(i)) + 100 + (scoreTeam1.length() - 1 - i) * 10);
            for (int i = scoreTeam2.length() - 1; i > -1; i--)
                scoreboard.showObject(Integer.parseInt("" + scoreTeam2.charAt(i)) + 200 + (scoreTeam2.length() - 1 - i) * 10);
            /* Time Left if timed */
            if (timed && timer >= 0) {
                int seconds = timer % 60;
                int minutes = (timer - seconds) / 60;
                scoreboard.showObject(730 + ((minutes - minutes % 10) / 10));
                scoreboard.showObject(720 + (minutes % 10));
                scoreboard.showObject(710 + ((seconds - seconds % 10) / 10));
                scoreboard.showObject(700 + (seconds % 10));
            }
            
            // Show Team Names
            String n1 = "freq" + team[0].freq;
            String n2 = "freq" + team[1].freq;
            String s1 = "", s2 = "";
            for (int i = 0; i < n1.length(); i++)
                if ((n1.charAt(i) >= '0') && (n1.charAt(i) <= 'z') && (s1.length() < 5))
                    s1 = s1 + n1.charAt(i);

            for (int i = 0; i < n2.length(); i++)
                if ((n2.charAt(i) >= '0') && (n2.charAt(i) <= 'z') && (s2.length() < 5))
                    s2 = s2 + n2.charAt(i);

            for (int i = 0; i < s1.length(); i++) {
                int t = new Integer(Integer.toString(
                        ((s1.getBytes()[i]) - 97) + 30) + Integer.toString(i + 0)).intValue();
                if (t < -89) {
                    t = new Integer(Integer.toString(((s1.getBytes()[i])) + 30) + Integer.toString(i + 0)).intValue();
                    t -= 220;
                }
                scoreboard.showObject(t);
            }
            
            for (int i = 0; i < s2.length(); i++) {
                int t = new Integer(Integer.toString(
                        ((s2.getBytes()[i]) - 97) + 30) + Integer.toString(i + 5)).intValue();
                if (t < -89) {
                    t = new Integer(Integer.toString(((s2.getBytes()[i])) + 30) + Integer.toString(i + 5)).intValue();
                    t -= 220;
                }
                scoreboard.showObject(t);
            }
            
            //Display everything
            ba.setObjects();
        }

        /** Begins the game starter sequence **/
        public void startGame() {
            pastStats = null;
            pick = -1;
            team[0].pick = false;
            team[1].pick = false;
            state = STARTING;
            timer = 10;
            ba.showObject(TEN_SECONDS);
            ba.sendArenaMessage("Both teams are ready, game will start in 10 seconds!", 1);
        }

        /** Starts the game after reseting ships and scores **/
        public void runGame() {
            scoreboard = ba.getObjectSet();
            ba.showObject(GOGOGO);
            team[0].score = 0;
            team[1].score = 0;
            warpTeams();
            if (timed) {
                timer = gameTime * 60;
                suddenDeath = false;
                ba.setTimer(gameTime);
                ba.sendArenaMessage("RULES: Most goals after " + gameTime + " minutes wins or sudden death if scores tied.");
            } else 
                ba.sendArenaMessage("RULES: First to " + goals + " goals wins!");

            state = PLAYING;
            ba.sendArenaMessage("GO GO GO!!!", 104);
            ba.scoreResetAll();
            ba.resetFlagGame();
            dropBall();
        } 
        
        public void checkGameScore() {
            if (!timed) {
                if (team[0].score == goals)
                    gameOver(team[0].freq);
                else if (team[1].score == goals)
                    gameOver(team[1].freq);
            } else if (timed && timer < 1 && !suddenDeath) {
                int wins = -1;
                if (team[0].score > team[1].score)
                    wins = team[0].freq;
                else if (team[1].score > team[0].score)
                    wins = team[1].freq;
                if (wins < 0) {
                    ba.setTimer(0);
                    suddenDeath = true;
                    ba.showObject(SUDDENDEATH);
                    ba.sendArenaMessage("NOTICE: End of regulation period -- Score TIED -- BEGIN SUDDEN DEATH", Tools.Sound.SCREAM);
                } else {
                    ba.setTimer(0);
                    suddenDeath = false;
                    gameOver(wins);
                }
            } else if (suddenDeath) {
                if (team[0].score > team[1].score) {
                    suddenDeath = false;
                    gameOver(team[0].freq);
                } else if (team[1].score > team[0].score) {
                    suddenDeath = false;
                    gameOver(team[1].freq);
                }
            }
        }
        
        public void announceTimeLeft() {
            if (timed && timer > 0) {
                int sec = timer % 60;
                int min = (timer - sec) / 60;
                if (min > 0)
                    ba.sendArenaMessage("Time Left: " + min + " minutes " + sec + " seconds", 1);
                else
                    ba.sendArenaMessage("Time Left: " + min + " minutes " + sec + " seconds", 1);
            }
        }
    }
    
    /** Player object holding all the stats of a player for one ship **/
    private class Attacker {
        String name;
        int kills, deaths, goals;
        int ship, terrKills, teamKills;
        int steals, turnovers, lagouts;
        long possession;
        
        public Attacker(String name, int ship) {
            this.name = name;
            this.ship = ship;
            kills = 0;
            deaths = 0;
            goals = 0;
            terrKills = 0;
            teamKills = 0;
            possession = 0;
            lagouts = 0;
        }
        
        /** increments kill stat **/
        public void addKill() {
            kills++;
        }

        /** increments death stat **/
        public void addDeath() {
            deaths++;
        }

        /** increments goals stat **/
        public void addGoal() {
            goals++;
        }

        /** accrues possession time stat **/
        public void addPoss(long t) {
            possession += t;
        }

        /** increments terrier kills stat **/
        public void addTerrKill() {
            terrKills++;
        }

        /** increments teamkills stat **/
        public void addTeamKill() {
            teamKills++;
        }

        /** increments lagout count **/
        public void addLagout() {
            lagouts++;
        }

        /** increments steal stat **/
        public void addSteal() {
            steals++;
        }

        /** increments turnover stat **/
        public void addTurnover() {
            turnovers++;
        }
    }
    
    private class Terr {
        String name;
        short x, y;
        long timestamp;
        
        public Terr(String name) {
            x = -1;
            y = -1;
            this.name = name;
            timestamp = System.currentTimeMillis();
        }
        
        public void update(short x, short y) {
            this.x = x;
            this.y = y;
            timestamp = System.currentTimeMillis();
        }
        
        public String getName() {
            return name;
        }
        
        public short getX() {
            return x;
        }
        
        public short getY() {
            return y;
        }
        
        public long getTimePassed() {
            return (System.currentTimeMillis() - timestamp) / 1000;
        }
    }
    
    /** Team object used to hold all relevant information for one team on a freq **/
    private class Team {
        // ship counts
        int[] ships;
        // player list of ships
        HashMap<String, Integer> players;
        HashMap<String, Attacker> stats;
        LinkedList<Attacker> oldStats;
        Terr[] terrs;
        int freq, score;
        String cap;
        boolean ready, pick;
        
        public Team(int f) {
            cap = null;
            freq = f;
            ships = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
            players = new HashMap<String, Integer>();
            stats = new HashMap<String, Attacker>();
            oldStats = new LinkedList<Attacker>();
            terrs = new Terr[SHIP_LIMITS[4]];
            for (int i = 0; i < terrs.length; i++)
                terrs[i] = null;
            score = 0;
            ready = false;
            pick = false;
        }
        
        public void updateTerr(String name, short x, short y) {
            if (terrs[0] != null && terrs[0].name.equalsIgnoreCase(name))
                terrs[0].update(x, y);
            else if (terrs[1] != null && terrs[1].name.equalsIgnoreCase(name))
                terrs[1].update(x, y);
            else
                debug("Error: " + name + " not found in terrs");
        }
        
        /**
         * Adds player for this team in a given ship
         * @param name
         * @param ship
         * @return Message for player or null if successful 
         */
        public String addPlayer(String name, int ship) {
            String result = "";
            String p = ba.getFuzzyPlayerName(name);
            if (p == null)
                return "Could not find " + name;
            if (!isNotBot(p))
                return "Unable to add bot " + name;
            if (notplaying.contains(p.toLowerCase()))
                return "" + p + " is currently not playing";
            if (isPlaying(p) || (!p.equalsIgnoreCase(cap) && isCaptain(p)))
                return "" + p + " is already on a team";
            if (ships[ship-1] >= SHIP_LIMITS[ship-1])
                return "Only " + SHIP_LIMITS[ship-1] + " " + Tools.shipName(ship) + "(s) are allowed per team";
            if (players.size() >= rules.getInt("MaxPlayers"))
                return "Team is full";
            if (ship == 5) {
                if (terrs[0] != null)
                    terrs[1] = new Terr(p);
                else
                    terrs[0] = new Terr(p);
            }
            ba.setShip(p, ship);
            ba.setFreq(p, freq);
            ba.scoreReset(p);
            ships[ship-1]++;
            players.put(p.toLowerCase(), ship);
            stats.put(p.toLowerCase(), new Attacker(p, ship));
            result = "Player " + p + " added to game";
            ba.sendSmartPrivateMessage(p, "You have been put in the game");
            ba.sendArenaMessage(p + " in for Freq " + freq + " with ship " + ship);
            if (pick)
                pick = false;
            return result;
        }
        
        /**
         * Removes a player from this team (should only be called during picking)
         * @param name
         * @return Message for player or null if successful
         */
        public String remove(String name) {
            String result = "";
            String p = findPlayer(name);
            if (p != null) {
                ba.specWithoutLock(p);
                int ship = players.remove(p);
                if (ship == 5) {
                    if (terrs[0] != null && terrs[0].name.equalsIgnoreCase(p))
                        terrs[0] = null;
                    else if (terrs[1] != null && terrs[1].name.equalsIgnoreCase(p))
                        terrs[1] = null;
                    else
                        debug("Could not find terr " + p + " to remove.");
                }
                ships[ship-1]--;
                if (state == PLAYING)
                    oldStats.add(stats.remove(p));
                else
                    stats.remove(p);
                ba.sendArenaMessage(p + " has been removed from Freq " + freq);
                result = null;
            } else
                result = "Player not found.";
            return result;
        }
        
        /** Removes a player from the game after using !notplaying while in **/
        public void notPlaying(String name) {
            int ship = players.remove(name.toLowerCase());
            if (ship == 5) {
                if (terrs[0] != null && terrs[0].name.equalsIgnoreCase(name))
                    terrs[0] = null;
                else if (terrs[1] != null && terrs[1].name.equalsIgnoreCase(name))
                    terrs[1] = null;
                else
                    debug("Could not find terr " + name + " to remove.");
            }
            ships[ship-1]--;
            if (state == PLAYING)
                oldStats.add(stats.remove(name.toLowerCase()));
            else
                stats.remove(name.toLowerCase());
            ba.specWithoutLock(name);
            ba.sendArenaMessage(name + " has been removed from the game. (not playing)");
            if (isCap(name))
                removeCap();
        }
        
        /**
         * Substitutes a player on this team for a player from spec
         * @param out
         * @param in
         * @return Message for player or null if successful
         */
        public String subPlayer(String out, String in) {
            String result = "";
            String subin = ba.getFuzzyPlayerName(in);
            String subout = findPlayer(out);
            if (subin != null && subout != null) {
                if (!this.isPlayersTeam(subout))
                    return out + " is not on your team or in the game";
                if (isPlaying(subin))
                    return in + " is already playing in this game";
                if (notplaying.contains(subin.toLowerCase()))
                    return "" + subin + " is currently not playing";
                int ship = players.remove(subout.toLowerCase());
                oldStats.add(stats.remove(subout.toLowerCase()));
                if (ship == 5) {
                    if (terrs[0] != null && terrs[0].name.equalsIgnoreCase(subout))
                        terrs[0] = new Terr(subin);
                    else if (terrs[1] != null && terrs[1].name.equalsIgnoreCase(subout))
                        terrs[1] = new Terr(subin);
                    else
                        debug("Could not find terr " + subout + " to replace.");
                }
                ba.specWithoutLock(subout);
                ba.setFreq(subout, freq);
                ba.setShip(subin, ship);
                ba.setFreq(subin, freq);
                players.put(subin.toLowerCase(), ship);
                stats.put(subin.toLowerCase(), new Attacker(subin, ship));
                ba.sendSmartPrivateMessage(subin, "You have been put in the game");
                ba.sendArenaMessage(subout + " has been substituted by " + subin + ".");
                result = null;
            } else {
                if (subout == null)
                    result = subout + " is not on your team";
                else if (subin == null)
                    result = "Player " + in + " not found";
            }
            return result;
        }
        
        /**
         * Switches the ships of two players from this team
         * @param name1
         * @param name2
         * @return Message for player or null if successful
         */
        public String switchPlayers(String name1, String name2) {
            String result = "";
            String p1 = findPlayer(name1);
            String p2 = findPlayer(name2);
            if (p1 != null && p2 != null) {
                int preship1 = players.get(p1);
                int preship2 = players.get(p2);
                if (preship1 == preship2)
                    result = p1 + " and " + p2 + " are in the same ship";
                else {
                    ba.setShip(p1, preship2);
                    players.put(p1, preship2);
                    ba.setShip(p2, preship1);
                    players.put(p2, preship1);
                    name1 = ba.getFuzzyPlayerName(p1);
                    name2 = ba.getFuzzyPlayerName(p2);
                    if (name1 != null)
                        result += name1 + " (" + preship1 + ") and ";
                    else result += p1 + " (" + preship1 + ") and ";
                    if (name2 != null)
                        result += name2 + " (" + preship2 + ") ";
                    else result += p2 + " (" + preship2 + ") ";
                    result += "switched ships.";
                    
                    if (name1 != null && name2 != null) {
                        if (preship1 == 5) {
                            if (terrs[0] != null && terrs[0].name.equalsIgnoreCase(name1))
                                terrs[0] = new Terr(name2);
                            else if (terrs[1] != null && terrs[1].name.equalsIgnoreCase(name1))
                                terrs[1] = new Terr(name2);
                            else
                                debug("Could not find terr " + name1 + " to replace.");
                        } else if (preship2 == 5) {
                            if (terrs[0] != null && terrs[0].name.equalsIgnoreCase(name2))
                                terrs[0] = new Terr(name1);
                            else if (terrs[1] != null && terrs[1].name.equalsIgnoreCase(name2))
                                terrs[1] = new Terr(name1);
                            else
                                debug("Could not find terr " + name2 + " to replace.");
                        }
                        if (state == PLAYING) {
                            oldStats.add(stats.remove(name1.toLowerCase()));
                            oldStats.add(stats.remove(name2.toLowerCase()));
                        }
                        stats.put(name1.toLowerCase(), new Attacker(name1, preship2));
                        stats.put(name2.toLowerCase(), new Attacker(name2, preship1));
                    } else {
                        if (preship1 == 5) {
                            if (terrs[0] != null && terrs[0].name.equalsIgnoreCase(p1))
                                terrs[0] = new Terr(p2);
                            else if (terrs[1] != null && terrs[1].name.equalsIgnoreCase(p1))
                                terrs[1] = new Terr(p2);
                            else
                                debug("Could not find terr " + p1 + " to replace.");
                        } else if (preship2 == 5) {
                            if (terrs[0] != null && terrs[0].name.equalsIgnoreCase(p2))
                                terrs[0] = new Terr(p1);
                            else if (terrs[1] != null && terrs[1].name.equalsIgnoreCase(p2))
                                terrs[1] = new Terr(p1);
                            else
                                debug("Could not find terr " + p2 + " to replace.");
                        }
                        if (state == PLAYING) {
                            oldStats.add(stats.remove(p1.toLowerCase()));
                            oldStats.add(stats.remove(p2.toLowerCase()));
                        }
                        stats.put(p1.toLowerCase(), new Attacker(p1, preship2));
                        stats.put(p2.toLowerCase(), new Attacker(p2, preship1));
                    }
                    
                    ba.sendArenaMessage(result);
                    result = null;
                }
            }
            return result;
        }
        
        /**
         * Changes the ship of a player
         * @param name
         * @param ship
         * @return Message for player or null if successful
         */
        public String changeShip(String name, int ship) {
            String result = "";
            String p = findPlayer(name);
            if (p != null) {
                name = ba.getFuzzyPlayerName(p);
                if (name == null)
                    name = p;
                if (ship == players.get(p))
                    result = name + " is already in ship " + ship;
                else if (ships[ship-1] >= SHIP_LIMITS[ship-1])
                    if (SHIP_LIMITS[ship-1] == 0)
                        result = "Ship not allowed";
                    else result = "Only " + SHIP_LIMITS[ship-1] + " " + Tools.shipName(ship) + " are allowed per team";
                else {
                    if (state == PLAYING)
                        oldStats.add(stats.remove(p));
                    stats.put(p, new Attacker(name, ship));
                    if (ship == 5) {
                        if (terrs[0] != null)
                            terrs[1] = new Terr(name);
                        else
                            terrs[0] = new Terr(name);
                    } else if (players.get(p) != null && players.get(p) == 5) {
                        if (terrs[0] != null && terrs[0].name.equalsIgnoreCase(p))
                            terrs[0] = null;
                        else if (terrs[1] != null && terrs[1].name.equalsIgnoreCase(p))
                            terrs[1] = null;
                        else
                            debug("Could not find terr " + p + " to remove.");
                    }
                    ba.setShip(name, ship);
                    ba.sendArenaMessage(name + " changed from ship " + players.get(p) + " to ship " + ship);
                    ships[players.get(p)-1]--;
                    ships[ship-1]++;
                    players.put(p, ship);
                    result = null;
                }
            }
            return result;
        }
        
        /**
         * Handles death events: checks for teamkills and terr kills, increments kills/deaths
         * @param name player died
         * @param killer name
         */
        public void handleDeath(String name, String killer) {
            if (isPlayersTeam(killer)) {
                Attacker p = getPlayerStats(killer);
                if (p != null)
                    p.addTeamKill();
                else
                    debug("Null player stats for " + killer);
                p = getPlayerStats(name);
                if (p != null)
                    p.addDeath();
                else
                    debug("Null player stats for " + name);
            } else {
                Integer get = players.get(name.toLowerCase());
                Attacker ker = getPlayer(killer);
                if (get != null && players.get(name.toLowerCase()) == 5)
                    ker.addTerrKill();
                else if (get == null) {
                    debug("PlayerDeath event for " + name + " killer: " + killer + " caused a NULL get");
                    return;
                }
                ker.addKill();
                getPlayerStats(name).addDeath();
            }
        }

        public void teamWarp() {
            if (freq == 0)
                ba.warpFreqToLocation(freq, safes[0], safes[1]);
            else
                ba.warpFreqToLocation(freq, safes[2], safes[3]);
        }
        
        /** Used by the current captain to remove themselves from captain **/
        public void removeCap() {
            ba.sendArenaMessage(cap + " has been removed from captain of Freq " + freq + ". Use !cap to claim captainship.", Tools.Sound.GAME_SUCKS);
            cap = null;
        }
        
        /** Retrieves the Player object holding the stats for the player with the specified name or null if not found **/
        public Attacker getPlayerStats(String name) {
            return stats.get(name.toLowerCase());
        }
        
        public void locateTerrs(String name) {
            boolean team0 = false;
            if (team[0].freq == freq)
                team0 = true;
            
            String msg = "";
            for (int i = 0; i < terrs.length; i++) {
                if (terrs[i] != null) {
                    msg = getLocation(terrs[i].getX(), terrs[i].getY(), team0);
                    ba.sendSmartPrivateMessage(name, terrs[i].getName() + msg + " [" + terrs[i].getTimePassed() + "s ago]");
                }
            }
        }
        
        /**
         * Searches the team players with a name or part of a name
         * @param name
         * @return The name of the player in lowercase or null if not found
         */
        private String findPlayer(String name) {
            String best = null;
            for (String i : players.keySet()) {
                if (i.startsWith(name.toLowerCase()))
                    if (best == null)
                        best = i;
                    else if (best.compareToIgnoreCase(i) > 0)
                        best = i;
                
                if (best != null && best.equalsIgnoreCase(name))
                    return best.toLowerCase();
            }
            return best;
        }
        
        /** 
         * Returns the ship of a player on the team
         * @param name
         * @return Ship of player or null if not found
         */
        public int getShip(String name) {
            return players.get(name.toLowerCase());
        }
        
        /** Check if the given player is on this team **/
        public boolean isPlayersTeam(String p) {
            if (players.containsKey(p.toLowerCase()))
                return true;
            else
                return false;
        }
        
        /** Check if the given player is the team's captain **/
        public boolean isCap(String name) {
            if (cap != null && cap.equalsIgnoreCase(name))
                return true;
            else return false;
        }
        
        /** Returns the current size of the players in for this team **/
        public int size() {
            return players.size();
        }
        
        public boolean isFull() {
            return players.size() == rules.getInt("MaxPlayers");
        }
        
        /** PMs a list of the current players on this team to name **/
        public void sendTeam(String name) {
            String msg = "Freq " + freq + " (captain: ";
            if (cap != null)
                msg += cap;
            msg += ")";
            ba.sendSmartPrivateMessage(name, msg);
            if (size() < 1) return;
            msg = "Name:                   - Ship:      - Status:";
            ba.sendSmartPrivateMessage(name, msg);
            for (String p : players.keySet()) {
                msg = padString(p, 23);
                msg += " - ";
                msg += padString(Tools.shipName(players.get(p)), 10);
                msg += " - ";
                if (lagouts.contains(p))
                    msg += "LAGGED OUT";
                else 
                    msg += "IN";
                ba.sendSmartPrivateMessage(name, msg);
            }
        }
        
        /** In a timed game during half time, switches all players to the opposite freq (only works if team freqs are 0 and 1) **/
        @SuppressWarnings("unused")
        public void switchSide() {
            if (freq == 0)
                freq = 1;
            else if (freq == 1)
                freq = 0;
            else return;

            for (String n : players.keySet())
                ba.setFreq(n, freq);
        }
        
        /** Gets the total kills for this team **/
        public int getKills() {
            int k = 0;
            for (Attacker p : stats.values())
                k += p.kills;
            for (Attacker p : oldStats)
                k += p.kills;
            return k;
        }

        /** Gets the total deaths for this team **/
        public int getDeaths() {
            int d = 0;
            for (Attacker p : stats.values())
                d += p.deaths;
            for (Attacker p : oldStats)
                d += p.deaths;
            return d;
        }

        /** Gets the total terrkills for this team **/
        public int getTerrKills() {
            int x = 0;
            for (Attacker p : stats.values())
                x += p.terrKills;
            for (Attacker p : oldStats)
                x += p.terrKills;
            return x;
        }

        /** Gets the total teamkills for this team **/
        public int getTeamKills() {
            int x = 0;
            for (Attacker p : stats.values())
                x += p.teamKills;
            for (Attacker p : oldStats)
                x += p.teamKills;
            return x;
        }

        /** Gets the total possession time in seconds for this team **/
        public int getPossession() {
            long x = 0;
            for (Attacker p : stats.values())
                x += p.possession;
            for (Attacker p : oldStats)
                x += p.possession;
            return (int) x/1000;
        }

        /** Gets the total lagouts for this team **/
        public int getLagouts() {
            int x = 0;
            for (Attacker p : stats.values())
                x += p.lagouts;
            for (Attacker p : oldStats)
                x += p.lagouts;
            return x;
        }

        /** Gets the total steals for this team **/
        public int getSteals() {
            int x = 0;
            for (Attacker p : stats.values())
                x += p.steals;
            for (Attacker p : oldStats)
                x += p.steals;
            return x;
        }

        /** Gets the total turnovers for this team **/
        public int getTurnovers() {
            int x = 0;
            for (Attacker p : stats.values())
                x += p.turnovers;
            for (Attacker p : oldStats)
                x += p.turnovers;
            return x;
        }
        
        public HashMap<String, Attacker> getStatMap() {
            HashMap<String, Attacker> result = new HashMap<String, Attacker>();
            for (Attacker p : stats.values()) {
                if (!result.containsKey(p.name.toLowerCase())) {
                    Attacker tot = new Attacker(p.name, p.ship);
                    tot.deaths = p.deaths;
                    tot.kills = p.kills;
                    tot.lagouts = p.lagouts;
                    tot.goals = p.goals;
                    tot.teamKills = p.teamKills;
                    tot.steals = p.steals;
                    tot.terrKills = p.terrKills;
                    tot.turnovers = p.turnovers;
                    tot.possession = p.possession;
                    result.put(p.name.toLowerCase(), tot);
                } else {
                    Attacker tot = result.get(p.name.toLowerCase());
                    tot.deaths += p.deaths;
                    tot.kills += p.kills;
                    tot.lagouts += p.lagouts;
                    tot.goals += p.goals;
                    tot.teamKills += p.teamKills;
                    tot.steals += p.steals;
                    tot.terrKills += p.terrKills;
                    tot.turnovers += p.turnovers;
                    tot.possession += p.possession;
                    result.put(tot.name.toLowerCase(), tot);
                }
            }
            for (Attacker p : oldStats) {
                if (!result.containsKey(p.name.toLowerCase())) {
                    Attacker tot = new Attacker(p.name, -1);
                    tot.deaths = p.deaths;
                    tot.kills = p.kills;
                    tot.lagouts = p.lagouts;
                    tot.goals = p.goals;
                    tot.teamKills = p.teamKills;
                    tot.steals = p.steals;
                    tot.terrKills = p.terrKills;
                    tot.turnovers = p.turnovers;
                    tot.possession = p.possession;
                    result.put(p.name.toLowerCase(), tot);
                } else {
                    Attacker tot = result.get(p.name.toLowerCase());
                    tot.deaths += p.deaths;
                    tot.kills += p.kills;
                    tot.lagouts += p.lagouts;
                    tot.goals += p.goals;
                    tot.teamKills += p.teamKills;
                    tot.steals += p.steals;
                    tot.terrKills += p.terrKills;
                    tot.turnovers += p.turnovers;
                    tot.possession += p.possession;
                    result.put(tot.name.toLowerCase(), tot);
                }
            }
            return result;
        }
        
        /** Resets all team variables to defaults **/
        public void reset() {
            score = 0;
            cap = null;
            players.clear();
            stats.clear();
            oldStats.clear();
            ships = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
            ready = false;
            for (int i = 0; i < terrs.length; i++)
                terrs[i] = null;
            ba.setFreqtoFreq(freq, SPEC_FREQ);
        }
    }

    /** Ball object holds and maintains all relevant information about the game ball **/
    private class Ball {

        private byte ballID;
        private long timestamp;
        private long carryTime;
        // absCarrier = Absolute carrier -> always contains the last player to have the ball (no nulls)
        private String carrier, absCarrier;
        private final Stack<String> carriers;

        public Ball() {
            carrier = null;
            carriers = new Stack<String>();
            carryTime = 0;
        }

        /**
         * Called by handleEvent(BallPosition event)
         * 
         * @param event
         *            the ball position
         */
        public void update(BallPosition event) {
            ballID = event.getBallID();
            this.timestamp = event.getTimeStamp();
            long now = System.currentTimeMillis();
            short carrierID = event.getCarrier();
            String newCarrier;
            if (carrierID != -1)
                newCarrier = ba.getPlayerName(carrierID);
            else
                newCarrier = null;
            
            // Possibilities:
            // Carrier a player & Carrier passed to enemy
            // Carrier a player & Carrier passed to teammate
            // Carrier a player & no newCarrier
            // Carrier was null & Ball picked up by NewCarrier
            // Carrier was null & no newCarrier
            
            if (carrier != null && newCarrier != null) {
                if (state == PLAYING && !carrier.equalsIgnoreCase(newCarrier)) {
                    // Player passes to another player
                    Attacker last = getPlayer(carrier);
                    Attacker curr = getPlayer(newCarrier);
                    Team passer = getTeam(carrier);
                    if (passer != null && !passer.isPlayersTeam(newCarrier) && last != null && curr != null) {
                        // steal + turnover
                        last.addTurnover();
                        curr.addSteal();
                    }
                    if (last != null) {
                        long pos = now - carryTime;
                        if (pos < MAX_POS)
                            last.addPoss(pos);
                    }
                    carryTime = now;
                }
                if (state == PLAYING && isNotBot(newCarrier)) {
                    carriers.push(newCarrier);
                    absCarrier = newCarrier;
                }
            } else if (carrier != null && newCarrier == null) {
                // loss of possession
                if (state == PLAYING) {
                    Attacker last = getPlayer(carrier);
                    if (last != null) {
                        long pos = now - carryTime;
                        last.addPoss(pos);                    
                    }
                }
                carryTime = now;
            } else if (carrier == null && newCarrier != null) {
                // ball wasn't passed but was picked up
                    // need to check exactly who had it last to see if it was a turnover
                Team ct = getTeam(newCarrier);
                if (state == PLAYING && ct != null && absCarrier != null && isNotBot(absCarrier)) {
                    if (!absCarrier.equalsIgnoreCase(newCarrier)) {
                        Attacker last = getPlayer(absCarrier);
                        Attacker curr = getPlayer(newCarrier);
                        if (!ct.isPlayersTeam(absCarrier) && last != null && curr != null) {
                            // steal + turnover
                            last.addTurnover();
                            curr.addSteal();
                        }
                    }
                    absCarrier = newCarrier;
                }
                carryTime = now;
                if (state == PLAYING && isNotBot(newCarrier))
                    carriers.push(newCarrier);
            } else {
                carryTime = now;
            }
            carrier = newCarrier;
        }

        /**
         * Gets last ball carrier (removes it from stack)
         * @return short player id or null if empty
         */
        public String getLastCarrierName() {
            String id = null;
            if (!carriers.empty())
                id = carriers.pop();
            return id;
        }

        /**
         * Peeks at last ball carrier without removing them from stack
         * @return short player id or null if empty
         */
        public String peekLastCarrierName() {
            String id = null;
            if (!carriers.empty()) {
                id = carriers.peek();
            }
            return id;
        }

        /**
         * clears carrier data for ball
         */
        public void clear() {
            carryTime = System.currentTimeMillis();
            absCarrier = ba.getBotName();
            carrier = null;
            try {
                carriers.clear();
            } catch (Exception e) {}
        }
        
        /** Gets the ball's ID **/
        public byte getBallID() {
            return ballID;
        }

        /** Gets the last ball update timestamp **/
        public long getTimeStamp() {
            return timestamp;
        }
    }  
    
    /**
     * Helper method adds spaces in front of a number to fit a certain length
     * @param n
     * @param length
     * @return String of length with spaces preceeding a number
     */
    private String padNumber(int n, int length) {
        String str = "";
        String x = "" + n;
        for (int i = 0; i + x.length() < length; i++)
            str += " ";
        return str + x;
    }
    
    private String[] wrapLines(String msg) {
        ArrayList<String> lines = new ArrayList<String>();
        while (msg.length() > 0) {
            if (msg.length() > MAX_CHARS) {
                int sp = MAX_CHARS - msg.substring(0, MAX_CHARS).lastIndexOf(' ');
                lines.add(msg.substring(0, sp));
                msg = msg.substring(sp + 1);
            } else {
                lines.add(msg);
                msg = "";
            }
        }
        return lines.toArray(new String[lines.size()]);
    }
    
    private void cmd_debug(String name) {
        if (!DEBUG) {
            debugger = name;
            DEBUG = true;
            ba.sendSmartPrivateMessage(name, "Debugging ENABLED. You are now set as the debugger.");
        } else if (debugger.equalsIgnoreCase(name)){
            debugger = "";
            DEBUG = false;
            ba.sendSmartPrivateMessage(name, "Debugging DISABLED and debugger reset.");
        } else {
            ba.sendChatMessage(name + " has overriden " + debugger + " as the target of debug messages.");
            ba.sendSmartPrivateMessage(name, "Debugging still ENABLED and you have replaced " + debugger + " as the debugger.");
            debugger = name;
        }
    }
    
    private void debug(String msg) {
        if (DEBUG)
            ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }
}
