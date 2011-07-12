package twcore.bots.attackbot;

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
import twcore.core.events.SoccerGoal;
import twcore.core.game.Ship;
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
    private Ball ball;
    private int[] attack; // attack arena coords
    private int[] attack2; // attack2 arena coords
    private int goals, pick, timer;
    private boolean autoMode; // if true then game is run with caps who volunteer via !cap
    private boolean timed;
    private boolean DEBUG;
    private String debugger;
    private LinkedList<String> notplaying;
    private LinkedList<String> lagouts;
    
    private Team[] team;
    
    private MasterControl mc;
    
    // Bot states
    public int state;
    public static final int IDLE = 0;
    public static final int STARTING = 1;
    public static final int PLAYING = 2;
    
    public static final int NP_FREQ = 666;
    public static final int MAX_GOALS = 15;
    public static final int MAX_TIME = 20;
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
        events = ba.getEventRequester();
        rules = ba.getBotSettings();
        events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(EventRequester.LOGGED_ON);
        events.request(EventRequester.MESSAGE);
        events.request(EventRequester.SOCCER_GOAL);
        events.request(EventRequester.BALL_POSITION);
        events.request(EventRequester.ARENA_JOINED);
        events.request(EventRequester.PLAYER_ENTERED);
        events.request(EventRequester.PLAYER_LEFT);
        events.request(EventRequester.PLAYER_DEATH);
        notplaying  = new LinkedList<String>();
        lagouts = new LinkedList<String>();
        autoMode = true;
        timed = false;
        DEBUG = false;
        debugger = "";
    }

    /** Handles the FrequencyShipChange event indicating potential lagouts **/
    public void handleEvent(FrequencyShipChange event) {
        if (state == IDLE) return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null) return;
        if (event.getShipType() == 0 && isPlaying(name) && !lagouts.contains(name.toLowerCase())) {
            lagouts.add(name.toLowerCase());
            getPlayer(name).addLagout();
            ba.sendPrivateMessage(name, "Use !lagout to return to the game.");
            Team t = getTeam(name);
            if (t != null && !t.isCap(name) && t.cap != null)
                ba.sendPrivateMessage(t.cap, name + " has lagged out.");
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

    /** Handles the LoggedOn event **/
    public void handleEvent(LoggedOn event) {
        ba.joinArena(ba.getBotSettings().getString("InitialArena"));
        state = IDLE;
        attack = new int[] { 478, 511, 544, 511 };
        attack2 = new int[] { 475, 512, 549, 512 };
        team = new Team[] { new Team(0), new Team(1) };
        goals = rules.getInt("Goals");
        SHIP_LIMITS = rules.getIntArray("Ships", ",");
        if (SHIP_LIMITS.length != 8)
            SHIP_LIMITS = new int[] { 1, 1, rules.getInt("MaxPlayers"), 0, 2, 0, rules.getInt("MaxPlayers"), 2 };
    }

    /** Handles the PlayerDeath event **/
    public void handleEvent(ArenaJoined event) {
        mc = new MasterControl();
        ball = new Ball();
        ba.toggleLocked();
        if (autoMode)
            ba.sendArenaMessage("A new game will begin when two players PM me with !cap", Tools.Sound.CROWD_GEE);
        ba.specAll();
        ba.setAlltoFreq(SPEC_FREQ);
        ba.setPlayerPositionUpdating(300);
        ba.receiveAllPlayerDeaths();
    }
    
    /** Handles the PlayerEntered event **/
    public void handleEvent(PlayerEntered event) {
        String name = event.getPlayerName();
        if (name == null) 
            name = ba.getPlayerName(event.getPlayerID());
        if (name == null) return;
        
        if (state == IDLE)
            ba.sendPrivateMessage(name, "A new game will begin after two players PM me with !cap");
        else if (state == STARTING) {
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
            ba.sendPrivateMessage(name, msg);
        } else if (state == PLAYING) {
            ba.sendPrivateMessage(name, "A game is currently being played. Score: " + team[0].score + " - " + team[1].score);
        }
        
        if (lagouts.contains(name.toLowerCase()))
            ba.sendPrivateMessage(name, "Use !lagout to return to the game.");
        if (notplaying.contains(name.toLowerCase())) {
            ba.setFreq(name, NP_FREQ);
            ba.sendPrivateMessage(name, "You are still set as not playing and captains will be unable to pick you. If you want to play, use !notplaying again.");
        }
    }
    
    /** Handles the PlayerLeft event (lagouts) **/
    public void handleEvent(PlayerLeft event) {
        if (state == IDLE) return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null) return;
        
        if (isPlaying(name) && !lagouts.contains(name.toLowerCase())) {
            lagouts.add(name.toLowerCase());
            getPlayer(name).addLagout();
        }
        Team t = getTeam(name);
        if (t != null && t.cap != null && !t.isCap(name))
            ba.sendPrivateMessage(t.cap, name + " has lagged out or left the arena.");
    }

    /**
     * Monitors goals scored.
     */
    public void handleEvent(SoccerGoal event) {
        if (state == PLAYING) {
            short scoringFreq = event.getFrequency();
            if (scoringFreq == 0 && team[0].freq == 0) {
                team[0].score++;
                String name = ball.getLastCarrierName();
                if (name != null && isNotBot(name)) {
                    Player p = team[0].getPlayerStats(name);
                    if (p != null)
                        p.addGoal();
                }
            } else if (scoringFreq == 1 && team[1].freq == 1) {
                team[1].score++;
                String name = ball.getLastCarrierName();
                if (name != null && isNotBot(name)) {
                    Player p = team[1].getPlayerStats(name);
                    if (p != null)
                        p.addGoal();
                }
            }
            mc.checkGameScore();
        }
    }
    
    public void handleEvent(PlayerDeath event) {
        if (state != PLAYING) return;
        String killer = ba.getPlayerName(event.getKillerID());
        String killee = ba.getPlayerName(event.getKilleeID());
        if (killer == null || killee == null) return;
        
        Team t = getTeam(killee);
        if (t != null)
            t.handleDeath(killee, killer);
        
    }

    public void handleEvent(BallPosition event) {
        ball.update(event);
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
            if (autoMode && msg.contains("Arena UNLOCKED"))
                ba.toggleLocked();
        }
        
        if (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) {

            if (msg.equalsIgnoreCase("!help"))
                help(name);
            else if (msg.equalsIgnoreCase("!status"))
                getStatus(name);
            else if (msg.equalsIgnoreCase("!caps"))
                getCaps(name);
            else if (msg.equalsIgnoreCase("!ready"))
                ready(name);
            else if (msg.equalsIgnoreCase("!cap"))
                setCaptain(name);
            else if (msg.startsWith("!add "))
                add(name, msg);
            else if (msg.startsWith("!remove "))
                remove(name, msg);
            else if (msg.startsWith("!change "))
                changeShip(name, msg);
            else if (msg.startsWith("!sub "))
                sub(name, msg);
            else if (msg.startsWith("!switch "))
                switchShips(name, msg);
            else if (msg.equalsIgnoreCase("!lagout"))
                lagout(name);
            else if (msg.equalsIgnoreCase("!list"))
                sendTeams(name);
            else if (msg.equalsIgnoreCase("!notplaying") || msg.equalsIgnoreCase("!np"))
                notPlaying(name);
            else if (msg.equalsIgnoreCase("!removecap"))
                removeCap(name);
            else if (msg.equalsIgnoreCase("!stats"))
                printStats(name);
            else if (msg.equalsIgnoreCase("!rules"))
                getRules(name);

            if (oplist.isZH(name)) {
                if (msg.equalsIgnoreCase("!drop"))
                    dropBall();
                else if (msg.equalsIgnoreCase("!start"))
                    mc.startGame();
                else if (msg.startsWith("!kill"))
                    stopGame(name, true);
                else if (msg.startsWith("!end"))
                    stopGame(name, false);
                else if (msg.equalsIgnoreCase("!die"))
                    die(name);
                else if (msg.startsWith("!go "))
                    go(name, msg);
                else if (msg.startsWith("!setcap "))
                    setCaptain(name, msg);
                else if (msg.equalsIgnoreCase("!debug"))
                    debugger(name);
                else if (msg.startsWith("!settime "))
                    setTime(name, msg);
                else if (msg.startsWith("!setgoals "))
                    setGoals(name, msg);
            }
        }
    }

    /**
     * !help Displays help message.
     */
    public void help(String name) {
        String[] help = {
                "+-- AttackBot Commands ---------------------------------------------------------------------.",
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
                "| !setcap <team>:<name>    - Sets <name> as captain of <team> (0 or 1)                      |",
                "| !settime <mins>          - Changes game to a timed game to <mins> minutes                 |",
                "| !setgoals <goals>        - Changes game rules to first to <goals> goals wins              |",
                "| !go <arena>              - Sends bot to <arena>                                           |",
                "| !die                     - Kills bot                                                      |",
                "| !endgame                 - Prematurely ends the current game with stats and scores        |",
                "| !killgame                - Abruptly kills the current game without a winner or stats      |",
        };
        String end = "`-------------------------------------------------------------------------------------------'";
        ba.privateMessageSpam(name, help);
        if (isCaptain(name))
            ba.privateMessageSpam(name, cap);
        if (oplist.isZH(name))
            ba.privateMessageSpam(name, staff);
        ba.sendPrivateMessage(name, end);
    }
    
    public void getRules(String name) {
        if (timed)
            ba.sendPrivateMessage(name, "RULES: Timed game to " + timer + " minutes most goals wins or sudden death. 10 players in basing ships per team. Ship limits: 1WB 1JAV 2TERR 2SHARK");
        else
            ba.sendPrivateMessage(name, "RULES: First to " + goals + " goals wins. Max of 10 players in basing ships per team. Ship limits: 1WB 1JAV 2TERR 2SHARK");
    }
    
    /** Handles the !notplaying command which prevents a player from being added and/or removes them from the game **/
    public void notPlaying(String name) {
        if (!isPlaying(name)) {
            if (!notplaying.contains(name.toLowerCase())) {
                notplaying.add(name.toLowerCase());
                ba.setFreq(name, NP_FREQ);
                ba.sendPrivateMessage(name, "You have been added to not playing. Captains will not be able to add you. If you wish to return, do !notplaying again.");
            } else {
                notplaying.remove(name.toLowerCase());
                ba.setShip(name, 1);
                ba.specWithoutLock(name);
                ba.sendPrivateMessage(name, "You have been removed from not playing. Captains will be able to add you.");
            }
        } else {
            getTeam(name).notPlaying(name);
            ba.setFreq(name, NP_FREQ);
            ba.sendPrivateMessage(name, "You have been added to not playing. Captains will not be able to add you. If you wish to return, do !notplaying again.");
        }
    }
    
    /** Handles the !setcap command which assigns a player as captain of a team (0 or 1) **/
    public void setCaptain(String name, String cmd) {
        if (cmd.length() < 8 || !cmd.contains(" ") || !cmd.contains(":")) return;
        String cap = ba.getFuzzyPlayerName(cmd.substring(cmd.indexOf(":") + 1));
        if (cap == null) {
            ba.sendPrivateMessage(name, "Player not found in this arena.");
            return;
        }
        
        if (isCaptain(cap)) {
            ba.sendPrivateMessage(name, cap + " is already the captain of a team.");
            return;
        }
        
        
        try {
            int freq = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":")));
            if (freq == 0 || freq == 1) {
                if (isPlaying(cap) && getTeam(cap).freq != freq) {
                    ba.sendPrivateMessage(name, cap + " is playing on the other team.");
                    return;
                }
                team[freq].cap = cap;
                ba.sendArenaMessage(cap + " has been assigned captain of freq " + team[freq].freq, Tools.Sound.BEEP1);
                mc.checkCaps();
            }
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Invalid team number (must be 0 or 1).");
        }
    }
    
    /**
     * Handles the !cap command which will assign the sender as captain of a team without a captain
     * if a game or picking has not yet begun. Otherwise, if a captain has left the arena then the
     * sender will replace the absent captain unless sender is on opposite team.
     * @param name
     */
    public void setCaptain(String name) {
        if (isCaptain(name)) {
            getCaps(name);
            return;
        }
        if (state == IDLE) {
            if (team[0].cap == null) {
                team[0].cap = name;
                ba.sendArenaMessage(name + " has been assigned captain of Freq " + team[0].freq, Tools.Sound.BEEP1);
            } else if (team[1].cap == null) {
                team[1].cap = name;
                ba.sendArenaMessage(name + " has been assigned captain of Freq " + team[1].freq, Tools.Sound.BEEP1);                
            } else {
                getCaps(name);
                return;
            }
            
            mc.checkCaps();
        } else if (!isCaptain(name)) {
            if (isPlaying(name)) {
                Team t = getTeam(name);
                if (ba.getFuzzyPlayerName(t.cap) == null) {
                    t.cap = name;
                    ba.sendArenaMessage(name + " is now the captain of Freq " + t.freq);
                } else
                    getCaps(name);
            } else {
                if (ba.getFuzzyPlayerName(team[0].cap) == null) {
                    team[0].cap = name;
                    ba.sendArenaMessage(name + " is now the captain of Freq " + team[0].freq);
                } else if (ba.getFuzzyPlayerName(team[1].cap) == null) {
                    team[1].cap = name;
                    ba.sendArenaMessage(name + " is now the captain of Freq " + team[1].freq);
                } else
                    getCaps(name);
            }
        }
    }
    
    /** Handles the !removecap command which removes the captain from being captain **/
    public void removeCap(String name) {
        if (!isCaptain(name)) return;
        getTeam(name).removeCap();
    }
    
    /** Handles the !caps command which returns the current team captains **/
    public void getCaps(String name) {
        String msg = "";
        if (team[0].cap != null)
            msg = team[0].cap + " is captain of Freq " + team[0].freq + ".";
        else msg = "Captain needed for Freq " + team[0].freq;
        ba.sendPrivateMessage(name, msg);
        if (team[1].cap != null)
            msg = team[1].cap + " is captain of Freq " + team[1].freq + ".";
        else msg = "Captain needed for Freq " + team[1].freq;
        ba.sendPrivateMessage(name, msg);
    }
    
    /** Handles the !list command which sends a list of team players with ships and statuses **/
    public void sendTeams(String name) {
        team[0].sendTeam(name);
        ba.sendPrivateMessage(name, "`");
        team[1].sendTeam(name);
    }

    /** Handles the !add command if given by a captain **/
    public void add(String name, String msg) {
        if (state == IDLE || !isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 5) return;
        Team t = getTeam(name);
        if (t == null) return;
        if (state == STARTING && !t.pick) {
            ba.sendPrivateMessage(name, "It is not your turn");
            return;
        }
        String player = msg.substring(msg.indexOf(" ") + 1, msg.indexOf(":"));
        int ship = 3;
        try {
            ship = Integer.valueOf(msg.substring(msg.indexOf(":") + 1).trim());
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Invalid ship number");
            return;
        }
        if (ship > 0 && ship < 9) {
            String res = t.addPlayer(player, ship);
            ba.sendPrivateMessage(name, res);
            if (state == STARTING && res.contains("added to game"))
                mc.nextPick();
        }
    }

    /** Handles the !remove command if given by a captain **/
    public void remove(String name, String msg) {
        if (state != STARTING || !isCaptain(name) || !msg.contains(" ") || msg.length() < 8) return;
        Team t = getTeam(name);
        if (t == null) return;
        String res = t.remove(msg.substring(msg.indexOf(" ") + 1));
        if (res != null)
            ba.sendPrivateMessage(name, res);
        
    }

    /** Handles the !sub command if given by a captain **/
    public void sub(String name, String msg) {
        if (state == IDLE || !isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 5) return;
        Team t = getTeam(name);
        if (t == null) return;
        String[] players = msg.substring(msg.indexOf(" ") + 1).split(":");
        String res = t.subPlayer(players[0], players[1]);
        if (res != null)
            ba.sendPrivateMessage(name, res);
    }
    
    /** Handles the !change command if given by a captain **/
    public void changeShip(String name, String msg) {
        if (!isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 8) return;
        Team t = getTeam(name);
        if (t == null) return;
        String player = msg.substring(msg.indexOf(" ") + 1, msg.indexOf(":"));
        int ship = 3;
        try {
            ship = Integer.valueOf(msg.substring(msg.indexOf(":") + 1).trim());
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Invalid ship number");
            return;
        }
        if (ship > 0 && ship < 9) {
            String res = t.changeShip(player, ship);
            if (res != null)
                ba.sendPrivateMessage(name, res);
        }
    }
    
    /** Handles the !switch command if given by a captain **/
    public void switchShips(String name, String msg) {
        if (!isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 8) return;
        Team t = getTeam(name);
        if (t == null) return;
        String[] players = msg.substring(msg.indexOf(" ") + 1).split(":");
        String res = t.switchPlayers(players[0], players[1]);
        if (res != null)
            ba.sendPrivateMessage(name, res);
    }
    
    /** Handles the !ready command if given by a team captain **/
    public void ready(String name) {
        if (state != STARTING || !isCaptain(name)) return;
        if (team[0].isCap(name)) {
            if (team[0].ready) {
                team[0].ready = false;
                ba.sendArenaMessage("Freq " + team[0].freq + " is not ready", Tools.Sound.BEEP1);
            } else if (team[1].ready) {
                team[0].ready = true;
                ba.sendArenaMessage("Freq " + team[0].freq + " is ready", Tools.Sound.BEEP1);
                mc.startGame();
            } else {
                team[0].ready = true;
                ba.sendArenaMessage("Freq " + team[0].freq + " is ready", Tools.Sound.BEEP1);
            }
        } else if (team[1].isCap(name)) {
            if (team[1].ready) {
                team[1].ready = false;
                ba.sendArenaMessage("Freq " + team[1].freq + " is not ready", Tools.Sound.BEEP1);
            } else if (team[0].ready) {
                team[1].ready = true;
                ba.sendArenaMessage("Freq " + team[1].freq + " is ready", Tools.Sound.BEEP1);
                mc.startGame();
            } else {
                team[1].ready = true;
                ba.sendArenaMessage("Freq " + team[1].freq + " is ready", Tools.Sound.BEEP1);
            }
        }
    }
    
    /** Returns a lagged out player to the game **/
    public void lagout(String name) {
        if (lagouts.contains(name.toLowerCase())) {
            lagouts.remove(name.toLowerCase());
            Team t = getTeam(name);
            if (t == null) return;
            ba.setShip(name, t.getShip(name));
            ba.setFreq(name, t.freq);
        } else 
            ba.sendPrivateMessage(name, "You are not lagged out and/or not in the game.");
    }

    /** Sends the bot to a specified arena (only attack and attack2 are currently supported) **/
    public void go(String name, String cmd) {
        String arena = cmd.substring(cmd.indexOf(" ") + 1);
        if (arena.length() > 0) {
            if (state > IDLE) {
                stopGame(name, true);
            }
            ba.sendSmartPrivateMessage(name, "Moving to " + arena);
            ba.changeArena(arena);
        }
    }

    /** Handles !die command which kills the bot **/
    public void die(String name) {
        if (state > IDLE) {
            stopGame(name, true);
        }
        ba.sendSmartPrivateMessage(name, "I'm melting! I'm melting...");
        ba.cancelTasks();
        ba.die();
    }

    /** Handles !setgoals which will change the game to first to goals win if possible **/
    public void setGoals(String name, String cmd) {
        if (cmd.length() < 10 || !cmd.contains(" ")) return;
        int winGoal = 0;
        try {
            winGoal = Integer.valueOf(cmd.substring(cmd.indexOf(" ")+1));
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Syntax error: please use !setgoals <goals>");
            return;
        }
        if (winGoal < 1 || winGoal > MAX_GOALS) {
            ba.sendPrivateMessage(name, "Goals must be between 1 and " + MAX_GOALS + ".");
            return;
        } 
        if (!timed) {
            if (goals != winGoal) {
                if (state == PLAYING && (winGoal <= team[0].score || winGoal <= team[1].score))
                    ba.sendPrivateMessage(name, "Setting goals to " + winGoal + " conflicts with the current game's score.");
                else {
                    goals = winGoal;
                    ba.sendPrivateMessage(name, "Game changed to FIRST to " + goals + " GOALS");
                    ba.sendArenaMessage("Game set to FIRST to " + goals + " GOALS");                    
                }
            } else
                ba.sendPrivateMessage(name, "Goals already set to " + goals + ".");
        } else {
            if (state == PLAYING)
                ba.sendPrivateMessage(name, "Game rules cannot be changed to goals in the middle of a timed game.");
            else {
                if (goals != winGoal) {
                    timed = false;
                    goals = winGoal;
                    ba.sendPrivateMessage(name, "Game changed from TIMED to FIRST to " + goals + " GOALS");
                    ba.sendArenaMessage("Game changed to FIRST to " + goals + " GOALS"); 
                } else
                    ba.sendPrivateMessage(name, "Goals were already set to " + goals + ".");
            }
        }
    }
    
    /** Handles !settime which will change the game to a timed game if possible **/
    public void setTime(String name, String cmd) {
        if (cmd.length() < 9 || !cmd.contains(" ")) return;
        int mins = 0;
        try {
            mins = Integer.valueOf(cmd.substring(cmd.indexOf(" ")+1));
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Syntax error: please use !settime <minutes>");
            return;
        }
        if (mins < 2 || mins > MAX_TIME) {
            ba.sendPrivateMessage(name, "Time must be between 2 and " + MAX_TIME + ".");
        } else if (timed && mins == timer) {
            ba.sendPrivateMessage(name, "Timed game was already set to " + timer + " minutes.");
        } else if (state == PLAYING) {
            ba.sendPrivateMessage(name, "Game cannot be changed to timed while being played."); 
        } else if (timed) {
            timer = mins;
            ba.sendPrivateMessage(name, "Game time set to " + timer + " minutes");
            ba.sendArenaMessage("Game set to TIMED to " + timer + " minutes");
        } else {
            timed = true;
            timer = mins;
            ba.sendPrivateMessage(name, "Game changed from goals to a TIMED to " + timer + " minutes");
            ba.sendArenaMessage("Game set to TIMED to " + timer + " minutes");
        }
    }

    /**
     * Handles the !kill and !end (game) commands. !killgame destroys the game and !endgame 
     * ends the game as if it had ended naturally by printing winner scores and stats.
     * @param name
     * @param kill true if !killgame otherwise !endgame with stats
     */
    public void stopGame(String name, boolean kill) {
        if (state > 0) {
            mc.killGame();
            if (kill) {
                ba.sendArenaMessage("This game has been killed by " + name);
                lagouts.clear();
                ba.specAll();
            } else if (state == PLAYING) {
                if (team[0].score > team[1].score)
                    mc.gameOver(team[0].freq);
                else if (team[1].score > team[0].score)
                    mc.gameOver(team[1].freq);
                else
                    mc.gameOver(-1);
            }
        } else if (state == IDLE)
            ba.sendPrivateMessage(name, "There is no game currently running.");
    }

    /** Handles the !status command which displays the score if a game is running **/
    public void getStatus(String name) {
        if (state == PLAYING) {
            ba.sendPrivateMessage(name, "[---  SCORE  ---]");
            ba.sendPrivateMessage(name, "[--Freq 0: " + team[0].score + " --]");
            ba.sendPrivateMessage(name, "[--Freq 1: " + team[1].score + " --]");
        } else if (state == IDLE) {
            if (autoMode)
                ba.sendPrivateMessage(name, "A new game will begin after two players volunteer to captain using !cap");
            else 
                ba.sendPrivateMessage(name, "There is no game currently running.");
        } else if (autoMode) {
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
            ba.sendPrivateMessage(name, msg);
        }
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
    private Player getPlayer(String name) {
        if (!isNotBot(name)) return null;
        Player p = team[0].getPlayerStats(name);
        if (p == null)
            p = team[1].getPlayerStats(name);
        return p;
    }
    
    /** Determines if the given player is playing in the game or not **/
    private boolean isPlaying(String name) {
        if (state != IDLE && team[0].isPlayersTeam(name) || team[1].isPlayersTeam(name))
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
        if (oplist.isBotExact(name) || (oplist.isSysop(name) && !oplist.isOwner(name) && !name.equalsIgnoreCase("Joyrider") && !name.equalsIgnoreCase("AvenDragon") && !name.equalsIgnoreCase("dral") && !name.equalsIgnoreCase("flared") && !name.equalsIgnoreCase("Witness") && !name.equalsIgnoreCase("Pure_Luck")))
            return false;
        else return true;
    }
    
    /** Helper method adds spaces to a string to meet a certain length **/
    private String padString(String str, int length) {
        for (int i = str.length(); i < length; i++)
            str += " ";
        return str.substring(0, length);
    }
    
    /** Handles !stats which spams private messages with the current game stats just like at the end of the game **/
    public void printStats(String name) {
        String msg = "Result of Freq " + team[0].freq + " vs. Freq " + team[1].freq + ": " + team[0].score + " - " + team[1].score;
        ba.sendPrivateMessage(name, msg);
        String[] msgs = {
                ",-------------------------------+------+------+------+------+----+--------+--------+----------+-------.",
                "|                             S |    K |    D |   TK |  TeK | LO |     TO | Steals |  Poss(s) | Goals |",
                "|                          ,----+------+------+------+------+----+--------+--------+----------+-------+",
                "| Freq " + team[0].freq + "                  /     |" + padNumber(team[0].getKills(), 5) + " |" + padNumber(team[0].getDeaths(), 5) + " |" + padNumber(team[0].getTeamKills(), 5) + " |" + padNumber(team[0].getTerrKills(), 5) + " |" + padNumber(team[0].getLagouts(), 3) + " |" + padNumber(team[0].getTurnovers(), 7) + " |"+ padNumber(team[0].getSteals(), 7) + " |" + padNumber(team[0].getPossession(), 9) + " |" + padNumber(team[0].score, 6) + " |",
                "+------------------------'      |      |      |      |      |    |        |        |          |       |",
        };
        ba.privateMessageSpam(name, msgs);
        for (Player p : team[0].stats.values()) {
            msg = "|  " + padString(p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            ba.sendPrivateMessage(name, msg);
        }
        for (Player p : team[0].oldStats) {
            msg = "|  " + padString("-" + p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            ba.sendPrivateMessage(name, msg);
        }
        
        msgs = new String[] {
                "+-------------------------------+------+------+------+------+----+--------+--------+----------+-------+",
                "|                          ,----+------+------+------+------+----+--------+--------+----------+-------+",
                "| Freq " + team[1].freq + "                  /     |" + padNumber(team[1].getKills(), 5) + " |" + padNumber(team[1].getDeaths(), 5) + " |" + padNumber(team[1].getTeamKills(), 5) + " |" + padNumber(team[1].getTerrKills(), 5) + " |" + padNumber(team[1].getLagouts(), 3) + " |" + padNumber(team[1].getTurnovers(), 7) + " |"+ padNumber(team[1].getSteals(), 7) + " |" + padNumber(team[1].getPossession(), 9) + " |" + padNumber(team[1].score, 6) + " |",
                "+------------------------'      |      |      |      |      |    |        |        |          |       |",
        };
        ba.privateMessageSpam(name, msgs);
        for (Player p : team[1].stats.values()) {
            msg = "|  " + padString(p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            ba.sendPrivateMessage(name, msg);
        }
        for (Player p : team[1].oldStats) {
            msg = "|  " + padString("-" + p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            ba.sendPrivateMessage(name, msg);
        }
        ba.sendPrivateMessage(name, "`-------------------------------+------+------+------+------+----+--------+--------+----------+-------'");
    }
    
    /** Helper method prints all the game statistics usually after game ends **/
    private void printStats() {
        String msg = "Result of Freq " + team[0].freq + " vs. Freq " + team[1].freq + ": " + team[0].score + " - " + team[1].score;
        ba.sendArenaMessage(msg);
        String[] msgs = {
                ",-------------------------------+------+------+------+------+----+--------+--------+----------+-------.",
                "|                             S |    K |    D |   TK |  TeK | LO |     TO | Steals |  Poss(s) | Goals |",
                "|                          ,----+------+------+------+------+----+--------+--------+----------+-------+",
                "| Freq " + team[0].freq + "                  /     |" + padNumber(team[0].getKills(), 5) + " |" + padNumber(team[0].getDeaths(), 5) + " |" + padNumber(team[0].getTeamKills(), 5) + " |" + padNumber(team[0].getTerrKills(), 5) + " |" + padNumber(team[0].getLagouts(), 3) + " |" + padNumber(team[0].getTurnovers(), 7) + " |"+ padNumber(team[0].getSteals(), 7) + " |" + padNumber(team[0].getPossession(), 9) + " |" + padNumber(team[0].score, 6) + " |",
                "+------------------------'      |      |      |      |      |    |        |        |          |       |",
        };
        ba.arenaMessageSpam(msgs);
        for (Player p : team[0].stats.values()) {
            msg = "|  " + padString(p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            ba.sendArenaMessage(msg);
        }
        for (Player p : team[0].oldStats) {
            msg = "|  " + padString("-" + p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            ba.sendArenaMessage(msg);
        }
        
        msgs = new String[] {
                "+-------------------------------+------+------+------+------+----+--------+--------+----------+-------+",
                "|                          ,----+------+------+------+------+----+--------+--------+----------+-------+",
                "| Freq " + team[1].freq + "                  /     |" + padNumber(team[1].getKills(), 5) + " |" + padNumber(team[1].getDeaths(), 5) + " |" + padNumber(team[1].getTeamKills(), 5) + " |" + padNumber(team[1].getTerrKills(), 5) + " |" + padNumber(team[1].getLagouts(), 3) + " |" + padNumber(team[1].getTurnovers(), 7) + " |"+ padNumber(team[1].getSteals(), 7) + " |" + padNumber(team[1].getPossession(), 9) + " |" + padNumber(team[1].score, 6) + " |",
                "+------------------------'      |      |      |      |      |    |        |        |          |       |",
        };
        ba.arenaMessageSpam(msgs);
        for (Player p : team[1].stats.values()) {
            msg = "|  " + padString(p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            ba.sendArenaMessage(msg);
        }
        for (Player p : team[1].oldStats) {
            msg = "|  " + padString("-" + p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            ba.sendArenaMessage(msg);
        }
        ba.sendArenaMessage("`-------------------------------+------+------+------+------+----+--------+--------+----------+-------'");
    }
    
    /** Helper method prints all the game statistics usually after game ends **/
    private void printTotals() {
        String msg = "Result of Freq " + team[0].freq + " vs. Freq " + team[1].freq + ": " + team[0].score + " - " + team[1].score;
        ba.sendArenaMessage(msg);
        HashMap<String, Player> stats = team[0].getStatMap();
        String[] msgs = {
                "+---------------------------------+------+------+------+----+--------+--------+----------+-------+",
                "|                               K |    D |   TK |  TeK | LO |     TO | Steals |  Poss(s) | Goals |",
                "|                          ,------+------+------+------+----+--------+--------+----------+-------+",
                "| Freq " + team[0].freq + "                  / " + padNumber(team[0].getKills(), 5) + " |" + padNumber(team[0].getDeaths(), 5) + " |" + padNumber(team[0].getTeamKills(), 5) + " |" + padNumber(team[0].getTerrKills(), 5) + " |" + padNumber(team[0].getLagouts(), 3) + " |" + padNumber(team[0].getTurnovers(), 7) + " |"+ padNumber(team[0].getSteals(), 7) + " |" + padNumber(team[0].getPossession(), 9) + " |" + padNumber(team[0].score, 6) + " |",
                "+------------------------'        |      |      |      |    |        |        |          |       |",
        };
        ba.arenaMessageSpam(msgs);
        for (Player p : stats.values()) {
            if (p.ship == -1)
                msg = "|  " + padString("-" + p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            else
                msg = "|  " + padString(p.name, 25) + "  " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            ba.sendArenaMessage(msg);
        }
        stats = team[1].getStatMap();
        msgs = new String[] {
                "+---------------------------------+------+------+------+----+--------+--------+----------+-------+",
                "|                          ,------+------+------+------+----+--------+--------+----------+-------+",
                "| Freq " + team[1].freq + "                  / " + padNumber(team[1].getKills(), 5) + " |" + padNumber(team[1].getDeaths(), 5) + " |" + padNumber(team[1].getTeamKills(), 5) + " |" + padNumber(team[1].getTerrKills(), 5) + " |" + padNumber(team[1].getLagouts(), 3) + " |" + padNumber(team[1].getTurnovers(), 7) + " |"+ padNumber(team[1].getSteals(), 7) + " |" + padNumber(team[1].getPossession(), 9) + " |" + padNumber(team[1].score, 6) + " |",
                "+------------------------'        |      |      |      |    |        |        |          |       |",
        };
        ba.arenaMessageSpam(msgs);
        for (Player p : stats.values()) {
            if (p.ship == -1)
                msg = "|  " + padString("-" + p.name, 25) + "  " + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            else
                msg = "|  " + padString(p.name, 25) + "  " + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.teamKills, 5) + " |" + padNumber(p.terrKills, 5) + " |" + padNumber(p.lagouts, 3) + " |" + padNumber(p.turnovers, 7) + " |" + padNumber(p.steals, 7) + " |" + padNumber((int) p.possession/1000, 9) + " |" + padNumber(p.goals, 6) + " |";
            ba.sendArenaMessage(msg);
        }
        
        ba.sendArenaMessage("`-------------------------------+------+------+------+----+--------+--------+----------+-------'");
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
    
    /**
     * Helper method used to get the opposing team freq
     * @param f freq
     * @return freq of the opposing team
     */
    private int getOpFreq(int f) {
        if (team[0].freq == f)
            return team[1].freq;
        else return team[0].freq;
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
        ba.specWithoutLock(ba.getBotName());
        ball.clear();
    }

    /** Handles post-goal operations **/
    private void handleGoal() {
        ba.sendArenaMessage("Score: " + team[0].score + " - " + team[1].score);
        ba.shipResetAll();
        ba.resetFlagGame();
        if (ba.getArenaName().equalsIgnoreCase("attack")) {
            ba.warpFreqToLocation(0, attack[0], attack[1]);
            ba.warpFreqToLocation(1, attack[2], attack[3]);
        } else {
            ba.warpFreqToLocation(0, attack2[0], attack2[1]);
            ba.warpFreqToLocation(1, attack2[2], attack2[3]);
        }
        dropBall();
    }

    /** MasterControl is responsible for the start and end of the game as well as time keeping if game is timed or goal checking if not **/
    private class MasterControl {

        TimerTask time;
        boolean suddenDeath;
        
        public MasterControl() {
            suddenDeath = false;
        }
        
        /** Begins the player picking process if both team's have a captain **/
        public void checkCaps() {
            if (team[0].cap == null || team[1].cap == null)
                return;
            state = STARTING;
            team[0].reset();
            team[1].reset();
            pick = 0;
            team[0].pick = true;
            team[1].pick = false;
            ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);        
        }
        
        /** Determines which team should have the next pick turn and executes **/
        private void nextPick() {
            if (pick == 0) {
                if (team[1].size() <= team[0].size()) {
                    if (!team[1].isFull()) {
                        pick = 1;
                        team[1].pick = true;
                        team[0].pick = false;
                        ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);  
                    }
                } else if (!team[0].isFull())
                    ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);
            } else {
                if (team[0].size() <= team[1].size()) {
                    if (!team[0].isFull()) {
                        pick = 0;
                        team[0].pick = true;
                        team[1].pick = false;
                        ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2); 
                    }
                } else if (!team[1].isFull())
                    ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);
            }
        }

        /** Begins the game starter sequence **/
        public void startGame() {
            state = PLAYING;
            ba.sendArenaMessage("Both teams are ready, game will start in 10 seconds!", 1);
            TimerTask t = new TimerTask() {
                public void run() {
                    runGame();
                }
            };
            ba.scheduleTask(t, 10000);
        }

        /** Starts the game after reseting ships and scores **/
        public void runGame() {
            team[0].score = 0;
            team[1].score = 0;
            ba.shipResetAll();
            if (ba.getArenaName().equalsIgnoreCase("attack")) {
                ba.warpFreqToLocation(0, attack[0], attack[1]);
                ba.warpFreqToLocation(1, attack[2], attack[3]);
            } else {
                ba.warpFreqToLocation(0, attack2[0], attack2[1]);
                ba.warpFreqToLocation(1, attack2[2], attack2[3]);
            }
            
            if (timed) {
                time = new TimerTask() {
                    public void run() {
                        suddenDeath = true;
                        checkGameScore();
                    }
                };
                ba.setTimer(timer);
                ba.scheduleTask(time, timer*Tools.TimeInMillis.MINUTE - 1900);
                ba.sendArenaMessage("RULES: Most goals after " + timer + " minutes wins or sudden death if tied.");
            } else 
                ba.sendArenaMessage("RULES: First to " + goals + " goals.");

            ba.sendArenaMessage("GO GO GO!!!", 104);
            ba.scoreResetAll();
            ba.resetFlagGame();
            dropBall();
        } 
        
        public void checkGameScore() {
            if (state != PLAYING) return;
            if (!timed && !suddenDeath) {
                if (team[0].score == goals)
                    gameOver(team[0].freq);
                else if (team[1].score == goals)
                    gameOver(team[1].freq);
                else
                    handleGoal();
            } else if (timed && suddenDeath){
                int wins = -1;
                if (team[0].score > team[1].score)
                    wins = team[0].freq;
                else if (team[1].score > team[0].score)
                    wins = team[1].freq;
                if (wins < 0) {
                    ba.setTimer(0);
                    timed = false;
                    ba.sendArenaMessage("NOTICE: End of regulation period -- Score TIED -- BEGIN SUDDEN DEATH", Tools.Sound.SCREAM);
                } else {
                    final int f = wins;
                    TimerTask end = new TimerTask() {
                        public void run() {
                            gameOver(f);
                        }
                    };
                    ba.scheduleTask(end, 2100);
                }
            } else if (timed && !suddenDeath) {
                handleGoal();
            } else if (!timed && suddenDeath) {
                if (team[0].score > team[1].score)
                    gameOver(team[0].freq);
                else if (team[1].score > team[0].score)
                    gameOver(team[1].freq);
                else
                    handleGoal(); //uhh no?
            }
        }
        
        
        /**
         * Helper method prints winning freq and game stats in arena messages
         * @param freq Winning freq
         */
        private void gameOver(int freq) {
            printTotals();
            if (freq > -1)
                ba.sendArenaMessage("GAME OVER: Freq " + freq + " wins!", 5);
            else
                ba.sendArenaMessage("GAME OVER: Tie!", 5);
            ba.sendArenaMessage("Final score: " + team[0].score + " - " + team[1].score + "  Detailed stats available until picking starts using !stats");
            state = IDLE;
            lagouts.clear();
            if (autoMode) {
                pick = 0;
                ba.sendArenaMessage("A new game will begin when two players PM me !cap");
                ba.specAll();
            }
        }
        
        public void killGame() {
            if (state == PLAYING && timed && !suddenDeath)
                ba.cancelTask(time);
        }
        
    }
    
    /** Player object holding all the stats of a player for one ship **/
    private class Player {
        String name;
        int kills, deaths, goals;
        int ship, terrKills, teamKills;
        int steals, turnovers, lagouts;
        long possession;
        
        public Player(String name, int ship) {
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
    
    /** Team object used to hold all relevant information for one team on a freq **/
    private class Team {
        // ship counts
        int[] ships;
        // player list of ships
        HashMap<String, Integer> players;
        HashMap<String, Player> stats;
        LinkedList<Player> oldStats;
        LinkedList<String> onFreq;
        int freq, score;
        String cap;
        boolean ready, pick;
        
        public Team(int f) {
            cap = null;
            freq = f;
            ships = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
            players = new HashMap<String, Integer>();
            stats = new HashMap<String, Player>();
            oldStats = new LinkedList<Player>();
            onFreq = new LinkedList<String>();
            score = 0;
            ready = false;
            pick = false;
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
            if (!isNotBot(name))
                return "Unable to add bot " + name;
            if (notplaying.contains(p.toLowerCase()))
                return "" + p + " is currently not playing";
            if (isPlaying(p) || (!p.equalsIgnoreCase(cap) && isCaptain(p)))
                return "" + p + " is already on a team";
            if (ships[ship-1] >= SHIP_LIMITS[ship-1])
                return "Only " + SHIP_LIMITS[ship-1] + " " + Tools.shipName(ship) + "(s) are allowed per team";
            if (players.size() >= rules.getInt("MaxPlayers"))
                return "Team is full";
            ba.setShip(p, ship);
            ba.setFreq(p, freq);
            ba.scoreReset(p);
            ships[ship-1]++;
            players.put(p.toLowerCase(), ship);
            stats.put(p.toLowerCase(), new Player(p, ship));
            result = "Player " + p + " added to game";
            ba.sendPrivateMessage(p, "You have been put in the game");
            ba.sendArenaMessage(p + " in for Freq " + freq + " with ship " + ship);
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
                ships[players.remove(p)-1]--;
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
            ships[players.remove(name.toLowerCase())-1]--;
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
                ba.specWithoutLock(subout);
                ba.setFreq(subout, freq);
                ba.setShip(subin, ship);
                ba.setFreq(subin, freq);
                players.put(subin.toLowerCase(), ship);
                stats.put(subin.toLowerCase(), new Player(subin, ship));
                ba.sendPrivateMessage(subin, "You have been put in the game");
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
                        if (state == PLAYING) {
                            oldStats.add(stats.remove(name1.toLowerCase()));
                            oldStats.add(stats.remove(name2.toLowerCase()));
                        }
                        stats.put(name1.toLowerCase(), new Player(name1, preship2));
                        stats.put(name2.toLowerCase(), new Player(name2, preship1));
                    } else {
                        if (state == PLAYING) {
                            oldStats.add(stats.remove(p1.toLowerCase()));
                            oldStats.add(stats.remove(p2.toLowerCase()));
                        }
                        stats.put(p1.toLowerCase(), new Player(p1, preship2));
                        stats.put(p2.toLowerCase(), new Player(p2, preship1));
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
                    stats.put(p, new Player(name, ship));
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
                getPlayerStats(killer).addTeamKill();
                getPlayerStats(name).addDeath();
            } else {
                Player ker = getPlayer(killer);
                if (players.get(name.toLowerCase()) == 5)
                    ker.addTerrKill();
                ker.addKill();
                getPlayerStats(name).addDeath();
            }
        }
        
        /** Used by the current captain to remove themselves from captain **/
        public void removeCap() {
            ba.sendArenaMessage(cap + " has been removed from captain of Freq " + freq + ". Use !cap to claim captainship.", Tools.Sound.GAME_SUCKS);
            cap = null;
        }
        
        /** Retrieves the Player object holding the stats for the player with the specified name or null if not found **/
        public Player getPlayerStats(String name) {
            return stats.get(name.toLowerCase());
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
            ba.sendPrivateMessage(name, msg);
            if (size() < 1) return;
            msg = "Name:                   - Ship:      - Status:";
            ba.sendPrivateMessage(name, msg);
            for (String p : players.keySet()) {
                msg = padString(p, 23);
                msg += " - ";
                msg += padString(Tools.shipName(players.get(p)), 10);
                msg += " - ";
                if (lagouts.contains(p))
                    msg += "LAGGED OUT";
                else 
                    msg += "IN";
                ba.sendPrivateMessage(name, msg);
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
            for (String n : onFreq)
                ba.setFreq(n, freq);
        }
        
        /** Gets the total kills for this team **/
        public int getKills() {
            int k = 0;
            for (Player p : stats.values())
                k += p.kills;
            for (Player p : oldStats)
                k += p.kills;
            return k;
        }

        /** Gets the total deaths for this team **/
        public int getDeaths() {
            int d = 0;
            for (Player p : stats.values())
                d += p.deaths;
            for (Player p : oldStats)
                d += p.deaths;
            return d;
        }

        /** Gets the total terrkills for this team **/
        public int getTerrKills() {
            int x = 0;
            for (Player p : stats.values())
                x += p.terrKills;
            for (Player p : oldStats)
                x += p.terrKills;
            return x;
        }

        /** Gets the total teamkills for this team **/
        public int getTeamKills() {
            int x = 0;
            for (Player p : stats.values())
                x += p.teamKills;
            for (Player p : oldStats)
                x += p.teamKills;
            return x;
        }

        /** Gets the total possession time in seconds for this team **/
        public int getPossession() {
            long x = 0;
            for (Player p : stats.values())
                x += p.possession;
            for (Player p : oldStats)
                x += p.possession;
            return (int) x/1000;
        }

        /** Gets the total lagouts for this team **/
        public int getLagouts() {
            int x = 0;
            for (Player p : stats.values())
                x += p.lagouts;
            for (Player p : oldStats)
                x += p.lagouts;
            return x;
        }

        /** Gets the total steals for this team **/
        public int getSteals() {
            int x = 0;
            for (Player p : stats.values())
                x += p.steals;
            for (Player p : oldStats)
                x += p.steals;
            return x;
        }

        /** Gets the total turnovers for this team **/
        public int getTurnovers() {
            int x = 0;
            for (Player p : stats.values())
                x += p.turnovers;
            for (Player p : oldStats)
                x += p.turnovers;
            return x;
        }
        
        public HashMap<String, Player> getStatMap() {
            HashMap<String, Player> result = new HashMap<String, Player>();
            for (Player p : stats.values()) {
                if (!result.containsKey(p.name.toLowerCase())) {
                    Player tot = new Player(p.name, p.ship);
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
                    Player tot = result.get(p.name.toLowerCase());
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
            for (Player p : oldStats) {
                if (!result.containsKey(p.name.toLowerCase())) {
                    Player tot = new Player(p.name, -1);
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
                    Player tot = result.get(p.name.toLowerCase());
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
            players.clear();
            stats.clear();
            oldStats.clear();
            onFreq.clear();
            ships = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
            ready = false;
        }
    }

    /** Ball object holds and maintains all relevant information about the game ball **/
    private class Ball {

        private byte ballID;
        private long timestamp;
        private long carryTime;
        private short ballX;
        private short ballY;
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
            ballX = event.getXLocation();
            ballY = event.getYLocation();
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
                    Player last = getPlayer(carrier);
                    Player curr = getPlayer(newCarrier);
                    Team passer = getTeam(carrier);
                    if (passer != null && !passer.isPlayersTeam(newCarrier) && last != null && curr != null) {
                        debug("Case 1: " + carrier + " passed to enemy " + newCarrier);
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
                debug("Case 2: " + carrier + " lost possession");
                if (state == PLAYING) {
                    Player last = getPlayer(carrier);
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
                        Player last = getPlayer(absCarrier);
                        Player curr = getPlayer(newCarrier);
                        if (!ct.isPlayersTeam(absCarrier) && last != null && curr != null) {
                            debug("Case 3: abs" + absCarrier + " passed to enemy " + newCarrier);
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
                debug("Case 4: All nulls");
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

        /** Most recent ball x coordinate **/
        public short getBallX() {
            return ballX;
        }

        /** Most recent ball y coordinate **/
        public short getBallY() {
            return ballY;
        }
    }  
    
    private void debugger(String name) {
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
