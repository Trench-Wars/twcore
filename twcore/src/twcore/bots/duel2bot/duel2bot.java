package twcore.bots.duel2bot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;

import twcore.bots.duel2bot.DuelBox;
import twcore.bots.duel2bot.DuelGame;
import twcore.bots.duel2bot.DuelPlayer;
import twcore.bots.duel2bot.DuelTeam;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.SQLResultEvent;
import twcore.core.game.Player;
import twcore.core.lag.LagHandler;
import twcore.core.lag.LagReport;
import twcore.core.util.Tools;

/**
 * TeamDuel bot for TWEL 2v2 scrimmage and league play. Uses a more detailed
 * stat tracking system (from new elim bot). It is built on a simplified game-play
 * setup mechanics where an unlocked arena allows certain duel settings to be 
 * inferred rather than specified as would have been done by typed commands previously. 
 *
 * @author WingZero
 */
public class duel2bot extends SubspaceBot{

    public static final String db = "website";
    final int MSG_LIMIT = 8;
    
    BotSettings settings;
    OperatorList oplist;
    BotAction ba;
    
    int d_spawnTime;
    int d_spawnLimit;
    int d_season;
    int d_deathTime;
    int d_maxLagouts;
    int d_noCount;
    int d_challengeTime;
    int d_duelLimit;
    int d_duelDays;

    CinSet                         banned;      // HashSet of banned players *needs to be updated in sync with db
    CinSet                         zonerBanned; // List of players banned from using !zone *stored in cfg file
    
    LagHandler                     lagHandler;
    Vector<String>                 lagChecks;
    TimerTask                      lagCheck;
    
    //private boolean                dying;
    private int                    zonerDelay = 10;
    private boolean                zoners;
    private long                   lastZoner;
    private int                    scrimID;     // current non-league duel id number
    private int                    gameID;      // current league duel id number
    private String                 greeting;
    private String                 debugger;
    private boolean                DEBUG;
    
    CinSet                         ops;
    CinSet                         heads;
    
    HashMap<Integer, DuelGame>     games;       // non-league teams will be identified by negative numbers
    HashMap<String, Integer>       playing;     // list of players who are playing tied to their duel id
    HashMap<String, DuelBox>       boxes;       // list of DuelBoxes
    HashMap<String, DuelPlayer>    players;     // list of players and associated DuelPlayer profile objects 
    HashMap<Integer, DuelTeam>     teams;       // list of teams by freq
    HashMap<String, DuelPlayer>    laggers;     // list of players currently lagged out
    HashMap<String, DuelChallenge> challs;      // list of challenges by String type key containing freqs
    Vector<Integer>                freqs;       // list of used frequencies
    
    TreeMap<String, String>        alias;       /* not really used technically */
    
    public duel2bot(BotAction botAction) {
        super(botAction);
        ba = botAction;
        greeting = "";
        EventRequester events = ba.getEventRequester();
        events.request(EventRequester.MESSAGE);
        events.request(EventRequester.LOGGED_ON);
        events.request(EventRequester.ARENA_JOINED);
        events.request(EventRequester.PLAYER_DEATH);
        events.request(EventRequester.PLAYER_POSITION);
        events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(EventRequester.FREQUENCY_CHANGE);
        events.request(EventRequester.PLAYER_LEFT);
        events.request(EventRequester.PLAYER_ENTERED);
        banned = new CinSet();
        zonerBanned = new CinSet();
        ops = new CinSet();
        heads = new CinSet();
        boxes = new HashMap<String, DuelBox>();
        games = new HashMap<Integer, DuelGame>();
        teams = new HashMap<Integer, DuelTeam>();
        players = new HashMap<String, DuelPlayer>();
        playing = new HashMap<String, Integer>();
        laggers = new HashMap<String, DuelPlayer>();
        challs = new HashMap<String, DuelChallenge>();
        freqs = new Vector<Integer>();
        alias = new TreeMap<String, String>();
        zoners = true;
        DEBUG = false;
        debugger = "";
        lastZoner = 0;
    }

    @Override
    public void handleEvent(LoggedOn event) {
        settings = ba.getBotSettings();
        oplist = ba.getOperatorList();
        ba.joinArena(settings.getString("Arena"));
        greeting = settings.getString("Greeting");
        zonerDelay = settings.getInt("ZonerDelay");
        lagChecks = new Vector<String>();
        lagCheck = null;
        lagHandler = new LagHandler(ba, settings, this, "handleLagReport");

        // Create new box Objects
        int boxCount = settings.getInt("BoxCount");
        for (int i = 1; i <= boxCount; i++) {
            String boxset[] = settings.getString("Box" + i).split(",");
            String warps[] = settings.getString("Warp" + i).split(",");
            String area[] = settings.getString("Area" + i).split(",");
            if (boxset.length == 17) boxes.put("" + i, new DuelBox(boxset, warps, area, i));
        }
        
        String[] zonebans = settings.getString("ZonerBanned").split(",");
        for (String zban : zonebans)
            zonerBanned.add(zban.toLowerCase());
        sql_loadBans();
        // Reads in general settings for dueling
        d_season = settings.getInt("Season");
        d_spawnTime = settings.getInt("SpawnAfter");
        d_spawnLimit = settings.getInt("SpawnLimit");
        d_deathTime = settings.getInt("SpawnTime");
        d_noCount = settings.getInt("NoCount");
        d_maxLagouts = settings.getInt("LagLimit");
        d_challengeTime = settings.getInt("ChallengeTime");
        d_duelLimit = settings.getInt("DuelLimit");
        d_duelDays = settings.getInt("DuelDays");
        ba.setReliableKills(1);
        gameID = 0;
        scrimID = 0;
        getLeagueOps();
    }

    @Override
    public void handleEvent(ArenaJoined event) {
        // handle player list
        String arena = ba.getArenaName();
        if (!arena.equalsIgnoreCase("duel2")) return;
        ba.shipResetAll();
        ba.warpAllToLocation(512, 502);
        
        Iterator<Player> i = ba.getPlayerIterator();
        while (i.hasNext()) {
            Player p = i.next();
            players.put(p.getPlayerName().toLowerCase(), new DuelPlayer(p, this));
        }
        
        lagCheck = new TimerTask() {
            public void run() {
                String name = lagChecks.remove(0);
                lagChecks.add(name);
                lagHandler.requestLag(name);
            }
        };
        ba.scheduleTask(lagCheck, 4000);
    }
    
    @Override
    public void handleEvent(PlayerEntered event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        /* Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null. */
        if (ptest == null) return;
        String name = ptest.getPlayerName();

        // greet
        // add to player list
        // retrieve/create player profile
        // refresh teams list
        if (laggers.containsKey(name.toLowerCase())) {
            players.put(name.toLowerCase(), laggers.get(name.toLowerCase()));
            players.get(name.toLowerCase()).handleReturn();
            lagChecks.add(name.toLowerCase());
        } else {
            players.put(name.toLowerCase(), new DuelPlayer(ptest, this));
            ba.sendPrivateMessage(name, greeting);
        }
    }

    @Override
    public void handleEvent(PlayerLeft event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        if (ptest == null) return;
        String name = ptest.getPlayerName();
        // remove from player lists
        // refresh teams list
        lagChecks.remove(name.toLowerCase());
        DuelPlayer p = getPlayer(name);
        if (p != null) {
            if (!laggers.containsKey(name.toLowerCase()) && playing.containsKey(name.toLowerCase()) 
                    && (p.getStatus() == DuelPlayer.PLAYING || p.getStatus() == DuelPlayer.IN))
                players.remove(name.toLowerCase()).handleLagout();
            else if (!playing.containsKey(name.toLowerCase())) {
                players.remove(name.toLowerCase());
                removeChalls(p.freq);
            }
        }
    }

    @Override
    public void handleEvent(FrequencyChange event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        /* Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null. */
        if (ptest == null) return;

        String name = ptest.getPlayerName();
        if (players.containsKey(name.toLowerCase()))
            players.get(name.toLowerCase()).handleFreq(event);
    }

    @Override
    public void handleEvent(PlayerDeath event) {
        Player ptest = ba.getPlayer(event.getKilleeID());
        Player ptest2 = ba.getPlayer(event.getKillerID());
        /* Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null. */
        if (ptest == null || ptest2 == null) return;

        // 1. alert player of kill, and death
        // 2a. check for tk - if tk dont add kill - else add kill to score
        // 2b. check for double kill

        String killee = ba.getPlayerName(event.getKilleeID());
        String killer = ba.getPlayerName(event.getKillerID());
        if (killee == null || killer == null) return;
        players.get(killee.toLowerCase()).handleDeath(killer);
    }

    @Override
    public void handleEvent(PlayerPosition event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        /* Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null. */
        if (ptest == null) return;

        String name = ptest.getPlayerName();
        // grab player profile
        // check game status
        // check position -> appropriate action
        if (players.containsKey(name.toLowerCase()))
            players.get(name.toLowerCase()).handlePosition(event);
    }

    @Override
    public void handleEvent(FrequencyShipChange event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        if (ptest == null) return;
        // grab player profile
        // check game status
        // check ship and freq -> appropriate action
        String name = ba.getPlayerName(event.getPlayerID());
        if (players.containsKey(name.toLowerCase()))
            players.get(name.toLowerCase()).handleFSC(event);
    }
    
    /** Handles a lag report received from the lag handler */
    public void handleLagReport(LagReport report) {
        if (!report.isBotRequest())
            ba.privateMessageSpam(report.getRequester(), report.getLagStats());
        if (report.isOverLimits()) {
            if (!report.isBotRequest())
                ba.sendPrivateMessage(report.getRequester(), report.getLagReport());
            DuelPlayer p = getPlayer(report.getName());
            if (p != null && ba.getPlayer(report.getName()).getShipType() != 0 && !p.isSpecced()) {  
                ba.sendPrivateMessage(report.getName(), report.getLagReport());
                p.handleLagout();
                ba.spec(report.getName());
                ba.spec(report.getName());
                debug("Speccing player for lag: " + p.getName());
            } else if (p == null)
                debug("p==null:" + (p==null) + (p!=null ? " status:" + p.getStatus() : ""));
        }
    }
    
    public void handleEvent(SQLResultEvent event) {
        ResultSet rs = event.getResultSet();
        String[] args = event.getIdentifier().split(":");
        try {
            if (args[0].equals("rating")) {
                if (args.length == 2) {
                    if (rs.next())
                        ba.sendSmartPrivateMessage(args[1], "Current " + getDivision(rs.getInt("fnDivision")) + " rating: " + rs.getInt(1));
                    else
                        ba.sendSmartPrivateMessage(args[1], "No rating found.");
                } else if (args.length == 3) {
                    if (rs.next())
                        ba.sendSmartPrivateMessage(args[1], "Rating of " + args[2] + " in " + getDivision(rs.getInt("fnDivision")) + ": " + rs.getInt(1));
                    else
                        ba.sendSmartPrivateMessage(args[1], "No rating found for: " + args[2]);
                }
            } else if (args[0].equals("info")) {
                if (rs.next()) {
                    String msg = "This name is registered";
                    if (rs.getInt("e") == 1)
                        msg += " and enabled for play.";
                    else
                        msg += " but disabled.";
                    ba.sendSmartPrivateMessage(args[1], msg);
                    //String ip = rs.getString("ip");
                    //String query = "SELECT u.fcUserName as n FROM tblDuel2__player p LEFT JOIN tblUser u ON p.fnUserID = u.fnUserID WHERE ";
                    //query += "fnEnabled = 1 AND (fcIP = '" + ip + "' OR (fcIP = '" + ip + "' AND fnMID = " + rs.getInt("mid") + "))";
                    String query = "SELECT U.fcUserName as n FROM tblDuel2__player DP LEFT JOIN tblUser U ON DP.fnUserID = U.fnUserID WHERE " +
                    		"DP.fnUserID IN (SELECT a2.fnUserID FROM tblAlias a1 JOIN tblAlias a2 ON a1.fnIP = a2.fnIP WHERE " +
                    			"a1.fnUserID = " + rs.getInt("id") + " AND fnEnabled = 1 LIMIT 100";
                    //ba.SQLBackgroundQuery(db, "alias:" + args[1], query);
                    ba.SQLClose(rs);
                    rs = ba.SQLQuery(db, query);
                    if (rs.next()) {
                        msg = "Registered aliases: " + rs.getString("n");
                        while (rs.next())
                            msg += ", " + rs.getString("n");
                        ba.sendSmartPrivateMessage(args[1], msg);
                    } else
                        ba.sendSmartPrivateMessage(args[1], "No aliases found.");
                } else {
                    if (alias.containsKey(args[2].toLowerCase()))
                        ba.sendSmartPrivateMessage(args[1], "Someone else is trying to alias check this player as well. Please try again later.");
                    else {
                        String msg = "This name is NOT registered.";
                        ba.sendSmartPrivateMessage(args[1], msg);
                        Player p = ba.getPlayer(args[2]);
                        if (p != null) {
                            alias.put(args[2].toLowerCase(), args[1]);
                            ba.sendUnfilteredPrivateMessage(args[2], "*info");
                        }
                    }
                }
            } else if (args[0].equals("alias")) {
                if (rs.next()) {
                    String msg = "Registered aliases: " + rs.getString("n");
                    while (rs.next())
                        msg += ", " + rs.getString("n");
                    ba.sendSmartPrivateMessage(args[1], msg);
                } else
                    ba.sendSmartPrivateMessage(args[1], "No aliases found.");
            } else if (args[0].equals("league")) {
                DuelPlayer p = getPlayer(args[3]);
                int r = 1000;
                if (!rs.next())
                    ba.SQLQueryAndClose(db, "INSERT INTO tblDuel2__league (fnUserID, fnSeason, fnDivision) VALUES(" + args[1] + ", " + d_season + ", " + args[2] + ")");
                else
                    r = rs.getInt("fnRating");
                if (p != null)
                    p.setRating(r);
                else
                    debug("[SQLrating] p was null");
            } else if (args[0].equals("topteams")) {
                String name = args[1];
                if (!rs.next()) {
                    ba.sendSmartPrivateMessage(name, "No teams found in that division.");
                    return;
                }
                ba.sendSmartPrivateMessage(name, "Top 3 " + getDivision(rs.getInt("divis")) + " Teams:");
                int i = 1;
                do {
                    String msg = " " + i + ") " + rs.getString("name1") + " and " + rs.getString("name2");
                    msg += ": " + rs.getInt("wins") + "-" + rs.getInt("losses");
                    ba.sendSmartPrivateMessage(name, msg);
                    i++;
                } while (rs.next());
            }
        } catch (SQLException e) {
            debug("[SQL_ERROR] Exception handling SQLResultEvent with ID: " + event.getIdentifier());
            e.printStackTrace();
        } finally {
            ba.SQLClose(rs);
        }
    }

    @Override
    public void handleEvent(Message event) {
        String msg = event.getMessage();
        String cmd = msg.toLowerCase();
        int type = event.getMessageType();

        if (type == Message.ARENA_MESSAGE) {
            if (msg.startsWith("IP:"))
                handleInfo(msg);
            lagHandler.handleLagMessage(msg);
        }
        
        String name = ba.getPlayerName(event.getPlayerID());
        if (oplist.isBotExact(name)) return;

        if (type == Message.PRIVATE_MESSAGE || type == Message.PUBLIC_MESSAGE) {
            if (cmd.equals("!signup"))
                cmd_signup(name);
            else if (cmd.equals("!disable"))
                cmd_disable(name, msg);
            else if (cmd.equals("!enable"))
                cmd_enable(name, msg);
            else if (cmd.equals("!ops"))
                cmd_ops(name);
            else if (cmd.startsWith("!ch+ ") || cmd.startsWith("!chr ") || cmd.startsWith("!challenge+ "))
                cmd_challenge(name, splitArgs(msg), true);
            else if (cmd.startsWith("!ch ") || cmd.startsWith("!challenge "))
                cmd_challenge(name, splitArgs(msg), false);
            else if (cmd.startsWith("!a ") || cmd.startsWith("!accept "))
                cmd_accept(name, splitArgs(msg));
            else if (cmd.equals("!cancel"))
                cmd_cancel(name);
            else if (cmd.startsWith("!lagout"))
                cmd_lagout(name);
            else if (cmd.startsWith("!help") || (cmd.startsWith("!h")))
                cmd_help(name);
            else if (cmd.startsWith("!ab"))
                cmd_about(name);
            else if (cmd.startsWith("!score")) 
                cmd_score(name, msg);
            else if (cmd.equals("!teams"))
                cmd_teams(name);
            else if (cmd.startsWith("!rating"))
                cmd_rating(name, msg);
            else if (cmd.equals("!rec"))
                cmd_rec(name);
            else if (cmd.startsWith("!stats"))
                cmd_stats(name, msg);
            else if (!cmd.startsWith("!lagout") && cmd.startsWith("!lag"))
                cmd_lag(name, msg);
            else if (cmd.equals("!zone") || cmd.equals("!spam"))
                cmd_zone(name);
            else if (cmd.startsWith("!duels") || cmd.startsWith("!games"))
                cmd_duels(name);
            else if (cmd.startsWith("!top"))
                cmd_topTeams(name, msg);
        }

        if (name != null && isOp(name)
                && (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE)) {
            if (cmd.startsWith("!die"))
                cmd_die(name);
            else if (cmd.startsWith("!ban ") && cmd.length() > 5)
                cmd_ban(name, msg);
            else if (cmd.startsWith("!unban ") && cmd.length() > 6)
                cmd_unban(name, msg);
            else if (cmd.startsWith("!alias ") && cmd.length() > 8)
                cmd_alias(name, msg);
            else if (cmd.startsWith("!signup ") && cmd.length() > 9)
                cmd_signup(name, msg);
            else if (cmd.startsWith("!enable "))
                cmd_enable(name, msg);
            else if (cmd.startsWith("!disable "))
                cmd_disable(name, msg);
            else if (cmd.startsWith("!debug"))
                cmd_debug(name);
            else if (cmd.startsWith("!cancel "))
                cmd_cancel(name, msg);
            else if (cmd.startsWith("!players"))
                cmd_players();
            else if (cmd.startsWith("!freqs"))
                cmd_freqs();
            else if (cmd.startsWith("!challs")) 
                cmd_challs();
        }
        
        if (oplist.isSmod(name)) {
            if (cmd.startsWith("!greet"))
                cmd_greet(name, msg);
            else if (cmd.startsWith("!zoneban"))
                cmd_zoneBan(name, msg);
            else if (cmd.startsWith("!zoneunban"))
                cmd_zoneUnban(name, msg);
            else if (cmd.equals("!zoners"))
                cmd_zoners(name);
            else if (cmd.equals("!reload"))
                cmd_reload(name);
        }
    }

    /** Handles the !help command */
    private void cmd_help(String name) {
        String[] help = {
                "+-COMMANDS----------------------------------------------------------------------------------------.",
                "| !about                      - Information about 2v2 TWEL                                        |",
                "| !ops                        - Displays current league operators                                 |",
                "| !signup                     - Registers you for 2v2 TWEL league duels                           |",
                "| !ch <player>:<division>     - Challenges the freq with <player> to a CASUAL duel in <division#> |",
                "| !ch+ <player>:<division>    - Challenges the freq with <player> to a RANKED duel in <division>  |",
                "|                               * You must have exactly 2 players per freq                        |",
                "|                               * Divisions: 1-Warbird, 2-Javelin, 3-Spider, 4-Lancaster, 5-Mixed |",
                "| !a <player>                 - Accepts a challenge from <player>                                 |",
                "| !topteams <division>        - (or !top) Shows the top 3 teams (in wins) in <division>           |",
                "| !zone                       - Sends a zone message requesting opponents and players to play     |",
                "| !cancel                     - Requests or accepts a duel cancelation if playing                 |", 
                "| !teams                      - Lists current teams eligible for ranked league play               |", 
                "| !duels                      - Lists duels currently in progress                                 |", 
                "| !score <player>             - Displays the score of <player>'s duel, if dueling                 |",
                "| !disable                    - Disables name to allow for the enabling of another name           |",
                "| !enable                     - Enables name if already registered but disabled                   |",
                "| !rec                        - Shows your current record if dueling                              |",
                "| !stats                      - Shows stats of your current duel if dueling                       |",
                "| !stats <player>             - Shows stats of <player>'s current duel if dueling                 |",
                "| !rating                     - Shows your rating for your current duel's division                |",
                "| !rating <division>          - Shows your rating for <division>                                  |",
                "| !rating <name>:<division>   - Shows the <division> rating of <name>                             |",
                "| !lag <name>                 - Requests the lag information for <name> (leave blank for self)    |",
                };
        ba.privateMessageSpam(name, help);
        if (!oplist.isModerator(name)) return;
        help = new String[] {
                "+-STAFF COMMANDS----------------------------------------------------------------------------------+",
                "| !zoners                     - Enables/Disables the !zone command                                |",
                "| !zoneban <name>             - Prevents <name> from using !zone                                  |",
                "| !zoneunban <name>           - Unbans <name> from using !zone                                    |",
                "| !alias <name>               - Lists enabled aliases of <name>                                   |",
                "| !signup <name>              - Force registers <name> regardless of aliases                      |",
                "| !enable <name>              - Force enables a registered but disabled <name> despite any aliases|",
                "| !disable <name>             - Disables a registered but enabled <name>                          |",
                "| !cancel <name>              - Force cancels a duel involving <name>                             |",
                "| !ban <name>                 - Bans <name> from playing in 2v2 TWEL (effective after duel)       |",
                "| !unban <name>               - Unbans <name> if banned                                           |",
                "| !reload                     - Reloads ban list and zoner delay time setting                     |",
                "| !die                        - Kills the bot                                                     |",
                "| !greet <message>            - Changes the greet message the bot PMs each player upon entering   |",
        };
        ba.privateMessageSpam(name, help);
        ba.sendPrivateMessage(name, 
                "`-------------------------------------------------------------------------------------------------'");
    }
    
    private void cmd_about(String name) {
        String[] about = new String[] {
                "+-ABOUT-------------------------------------------------------------------------------------------.",
                "| This is a 2v2 TWEL duel arena, however, casual or scrimmage duels are also available. To play   |",
                "| a casual duel enter a ship with one other player on the same freq. Challenge another freq of    |",
                "| two players using the !ch <name>:<division> command. For ranked (league) duels all participants |",
                "| must be registered with !signup. Ranked challenges are sent using the !ch+ command.             |",
                "`-------------------------------------------------------------------------------------------------'"
        };
        ba.privateMessageSpam(name, about);
    }
    
    private void cmd_ops(String name) {
        String str = "Head Operators: ";
        for (String n : heads)
            str += n + ", ";
        ba.sendSmartPrivateMessage(name, str.substring(0, str.length() - 2));
        str = "League Operators: ";
        for (String n : ops)
            str += n + ", ";
        ba.sendSmartPrivateMessage(name, str.substring(0, str.length() - 2));
    }
    
    
    private void cmd_reload(String name) {
        sql_loadBans();
        zonerDelay = settings.getInt("ZonerDelay");
        ba.sendSmartPrivateMessage(name, "Ban list and zoner time delay setting have been reloaded.");
    }
    
    private void cmd_topTeams(String name, String cmd) {
        int div = -1;
        try {
            div = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1));
        } catch (Exception e) {
            ba.sendSmartPrivateMessage(name, "Invalid syntax, please use !top <division>");
            return;
        }
        if (div < 1 || div > 5) {
            ba.sendSmartPrivateMessage(name, "Invalid division number.");
            return;
        }
        String query = "SELECT t.fnDivision as divis, u1.fcUserName as name1, u2.fcUserName as name2, t.fnWins as wins, t.fnLosses as losses FROM tblDuel2__team t LEFT JOIN tblUser u1 ON u1.fnUserID = t.fnUser1 LEFT JOIN tblUser u2 ON u2.fnUserID = t.fnUser2 WHERE t.fnDivision = " + div + " ORDER BY t.fnWins DESC LIMIT 3";
        ba.SQLBackgroundQuery(db, "topteams:" + name, query);
    }
    
    private void cmd_greet(String name, String cmd) {
        String msg = cmd.substring(cmd.indexOf(" ") + 1);
        greeting = msg;
        settings.put("Greeting", msg);
        settings.save();
        ba.sendSmartPrivateMessage(name, "Greeting set to: " + msg);
    }
    
    private void cmd_zoners(String name) {
        zoners = !zoners;
        if (zoners)
            ba.sendPrivateMessage(name, "The !zone command has been ENABLED.");
        else
            ba.sendPrivateMessage(name, "The !zone command has been DISABLED.");
    }
    
    private void cmd_zoneBan(String name, String cmd) {
        String p = cmd.substring(cmd.indexOf(" ") + 1);
        String plc = p.toLowerCase();
        if (zonerBanned.contains(plc)) {
            ba.sendSmartPrivateMessage(name, p + " is already zoner banned.");
            return;
        }
        zonerBanned.add(plc);
        String str = "";
        for (String n : zonerBanned)
            str += n + ",";
        str = str.substring(0, str.length() - 1);
        settings.put("ZonerBanned", str);
        settings.save();
        ba.sendPrivateMessage(name, p + " has been banned from the zone command.");
    }
    
    private void cmd_zoneUnban(String name, String cmd) {
        String p = cmd.substring(cmd.indexOf(" ") + 1);
        String plc = p.toLowerCase();
        if (!zonerBanned.contains(plc)) {
            ba.sendSmartPrivateMessage(name, p + " is not zoner banned.");
            return;
        }
        zonerBanned.remove(plc);
        String str = "";
        for (String n : zonerBanned)
            str += n + ",";
        str = str.substring(0, str.length() - 1);
        settings.put("ZonerBanned", str);
        settings.save();
        ba.sendPrivateMessage(name, p + " is unbanned and can use the zone command again.");
    }
    
    private void cmd_zone(String name) {
        if (!zoners) {
            ba.sendPrivateMessage(name, "The !zone command is not currently enabled.");
            return;
        }
        if (zonerBanned.contains(name.toLowerCase())) {
            ba.sendPrivateMessage(name, "You are not allowed to use this command.");
            return;
        }
        DuelPlayer p = getPlayer(name);
        if (p != null && (!p.isRegistered() || !p.isEnabled())) {
            ba.sendPrivateMessage(name, "That command is only available to registered players.");
            return;
        } else if (p == null)
            return;
            
        long now = System.currentTimeMillis();
        if (now - lastZoner > zonerDelay * Tools.TimeInMillis.MINUTE) {
            if (playing.containsKey(name.toLowerCase())) {
                ba.sendPrivateMessage(name, "You must not be playing in order to use this command.");
                return;
            }
            String zone = "";
            String partner = getPartner(name);
            if (partner != null) {
                DuelPlayer pp = getPlayer(partner);
                if (pp != null && (!pp.isRegistered() || !pp.isEnabled())) {
                    ba.sendPrivateMessage(name, "That command is only available to registered teams.");
                    return;
                } else if (pp == null)
                    return;
                zone = name + " and " + partner + " are looking for worthy 2v2 opponents! Type ?go duel2 to accept their challenge -" + ba.getBotName();
            } else
                zone = name + " is looking for worthy 2v2 players! Type ?go duel2 to accept the challenge -" + ba.getBotName();
            ba.sendZoneMessage(zone, 1);
            lastZoner = now;
        } else {
            long dt = zonerDelay * Tools.TimeInMillis.MINUTE - (now - lastZoner);
            int mins = (int) dt / Tools.TimeInMillis.MINUTE;
            int secs = (int) (dt - mins * Tools.TimeInMillis.MINUTE) / Tools.TimeInMillis.SECOND;
            ba.sendSmartPrivateMessage(name, "The next zoner will be available in " + mins + " minutes " + secs + " seconds");
        }
    }
    
    private void cmd_signup(String name) {
        DuelPlayer p = null;
        p = getPlayer(name);
        
        if (p == null) {
            Player info = ba.getPlayer(name);
            if (info == null)
                return;
            p = new DuelPlayer(info, this);
            players.put(name.toLowerCase(), p);
        }
        if (p != null)
            p.doSignup(null);
    }
    
    private void cmd_signup(String name, String cmd) {
        DuelPlayer p = null;
        String player = null;
        String[] args = splitArgs(cmd);
        if (args != null && args.length == 1)
            player = args[0];
        else
            return;
        p = getPlayer(player);
        if (p == null) {
            Player info = ba.getPlayer(player);
            if (info == null) {
                ba.sendSmartPrivateMessage(name, "Player '" + args[0] + "' must be in the arena to be registered.");
                return;
            }
            p = new DuelPlayer(info, this);
            players.put(player.toLowerCase(), p);
        }
        if (p != null)
            p.doSignup(name);
    }
    
    private void cmd_disable(String name, String cmd) {
        DuelPlayer p = null;
        String[] args = null;
        if (cmd.contains(" "))
            args = splitArgs(cmd);
        if (args != null && args.length == 1)
            p = getPlayer(args[0]);
        else
            p = getPlayer(name);
        if (p == null) {
            if (args != null)
                p = new DuelPlayer(args[0], this);
            else
                p = new DuelPlayer(name, this);
            players.put(name.toLowerCase(), p);
        }
        if (args == null)
            name = null;
        if (p != null)
            p.doDisable(name);
    }
    
    private void cmd_enable(String name, String cmd) {
        DuelPlayer p = null;
        String[] args = null;
        if (cmd.contains(" "))
            args = splitArgs(cmd);
        if (args != null && args.length == 1)
            p = getPlayer(args[0]);
        else
            p = getPlayer(name);
        if (p == null) {
            if (args != null)
                p = new DuelPlayer(args[0], this);
            else
                p = new DuelPlayer(name, this);
            players.put(name.toLowerCase(), p);
        }
        if (args == null)
            name = null;
        if (p != null)
            p.doEnable(name);
    }
    
    private void cmd_rating(String name, String cmd) {
        String[] args = splitArgs(cmd);
        
        try {
            if (args != null) {
                Player partial = ba.getFuzzyPlayer(args[0]);
                if (partial != null)
                    args[0] = partial.getPlayerName();
                if (args.length == 2) {
                    UserData u = new UserData(ba, db, args[0]);
                    int div = Integer.valueOf(args[1]);
                    if (div < 1 || div > 5)
                        throw new NumberFormatException();
                    int id = u.getUserID();
                    if (id > 0) {
                        String query = "SELECT fnRating, fnDivision FROM tblDuel2__league WHERE fnUserID = " + id + " AND fnDivision = " + div + " LIMIT 1";
                        ba.SQLBackgroundQuery(db, "rating:" + name + ":" + args[0], query);
                    } else
                        ba.sendSmartPrivateMessage(name, "Error retreiving user information for: " + args[0]);
                } else if (args.length == 1) {
                    UserData u = new UserData(ba, db, name);
                    int div = Integer.valueOf(args[0]);
                    if (div < 1 || div > 5)
                        throw new NumberFormatException();
                    String query = "SELECT fnRating, fnDivision FROM tblDuel2__league WHERE fnUserID = " + u.getUserID() + " AND fnDivision = " + div + " LIMIT 1";
                    ba.SQLBackgroundQuery(db, "rating:" + name, query);
                } else
                    ba.sendSmartPrivateMessage(name, "Error parsing rating command.");
            } else if (playing.containsKey(name)) {
                DuelPlayer p = getPlayer(name);
                if (p != null)
                    p.doRating();
            } else 
                ba.sendSmartPrivateMessage(name, "You are not currently in a duel.");
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(name, "Error recognizing division identification number.");
        }
    }

    private void cmd_rec(String name) {
        if (playing.containsKey(name.toLowerCase()))
            getPlayer(name.toLowerCase()).doRec();
        else
            ba.sendPrivateMessage(name, "You are not dueling.");
    }
    
    private void cmd_alias(String name, String cmd) {
        String p = cmd.substring(cmd.indexOf(" ") + 1);
        sql_getUserInfo(name, p);
    }
    
    private void cmd_ban(String name, String cmd) {
        String[] args = splitArgs(cmd);
        if (args == null) return;
        if (args.length == 2)
            sql_banPlayer(name, args[0], args[1]);
        else 
            ba.sendSmartPrivateMessage(name, "Invalid syntax, please use !ban <player>:<comment>");
    }
    
    private void cmd_unban(String name, String cmd) {
        String p = cmd.substring(cmd.indexOf(" ") + 1);
        sql_unbanPlayer(name, p);
    }
    
    private void cmd_teams(String name) {
        TreeMap<Short, Player> freqs = new TreeMap<Short, Player>();
        Iterator<Player> i = ba.getPlayingPlayerIterator();
        while (i.hasNext()) {
            Player p = i.next();
            if (ba.getFrequencySize(p.getFrequency()) == 2) {
                DuelPlayer dp = getPlayer(p.getPlayerName());
                if (dp != null) {
                    if (dp.canPlay()) {
                        if (freqs.containsKey(p.getFrequency()))
                            ba.sendSmartPrivateMessage(name, "Team: '" + freqs.get(p.getFrequency()).getPlayerName() + "' and '" + p.getPlayerName() + "'");
                        else
                            freqs.put(p.getFrequency(), p);
                    }
                }
            }
        }
        if (freqs.isEmpty())
            ba.sendSmartPrivateMessage(name, "No eligable teams of two could be found.");
    }

    /** Handles the !debug command which enables or disables debug mode */
    private void cmd_debug(String name) {
        if (!DEBUG) {
            debugger = name;
            DEBUG = true;
            ba.sendSmartPrivateMessage(name, "Debugging ENABLED. You are now set as the debugger.");
        } else if (debugger.equalsIgnoreCase(name)) {
            debugger = "";
            DEBUG = false;
            ba.sendSmartPrivateMessage(name, "Debugging DISABLED and debugger reset.");
        } else {
            ba.sendChatMessage(name + " has overriden " + debugger
                    + " as the target of debug messages.");
            ba.sendSmartPrivateMessage(name, "Debugging still ENABLED and you have replaced "
                    + debugger + " as the debugger.");
            debugger = name;
        }
    }

    /** Handles the !players debugging command */
    private void cmd_players() {
        for (String s : players.keySet())
            debug("" + s);
    }

    /** Handles the !duels command */
    private void cmd_duels(String name) {
        for (Integer s : games.keySet()) {
            DuelGame g = games.get(s);
            ba.sendSmartPrivateMessage(name, (s < 0 ? "[CASUAL] " : "[RANKED] ") + g.getScore());
        }
    }

    /** Handles the !challs debugging command */
    private void cmd_challs() {
        for (String s : challs.keySet())
            debug("" + s);
    }

    /** Handles the !freqs debugging command */
    private void cmd_freqs() {
        for (Integer s : teams.keySet())
            debug("" + s);

        debug("now freqs...");
        for (Integer s : freqs)
            debug("" + s);
    }

    /** Handles the !cancel command */
    private void cmd_cancel(String name, String msg) {
        if (msg.contains(" ") && msg.length() > 7) {
            msg = msg.substring(msg.indexOf(" ") + 1);
            if (players.containsKey(msg.toLowerCase()))
                players.get(msg.toLowerCase()).cancelGame(name);
            else {
                String p = ba.getFuzzyPlayerName(msg);
                if (p != null && players.containsKey(p.toLowerCase()))
                    players.get(p.toLowerCase()).cancelGame(name);
                else
                    ba.sendPrivateMessage(name, "Player not found");
            }
        } else
            ba.sendPrivateMessage(name, "Invalid syntax");
    }

    /** Handles the !ch command
     * 
     * @param name
     * @param args !ch p:div
     */
    private void cmd_challenge(String name, String[] args, boolean ranked) {
        if (args.length != 2) return;
        int div = -1;
        try {
            div = Integer.valueOf(args[1]);
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Invalid division number.");
            return;
        }

        if (div < 1 || div > 5) {
            ba.sendPrivateMessage(name, "Invalid division number (1-5).");
            return;
        } else if ((div == 2 && !getBoxOpen(2)) || !getBoxOpen(1)) {
            ba.sendPrivateMessage(name, "All duel boxes for that division are in use. Please try again later.");
            return;
        }
        
        Player p = ba.getPlayer(name);
        
        if (ba.getFrequencySize(p.getFrequency()) != 2) {
            ba.sendPrivateMessage(name, "Your freq size must be 2 players exactly to challenge for a 2v2 duel.");
            return;
        }
        
        Player o = ba.getFuzzyPlayer(args[0]);
        if (o == null || ba.getFrequencySize(o.getFrequency()) != 2) {
            ba.sendPrivateMessage(name, "The enemy freq size must be 2 players exactly to challenge for a 2v2 duel.");
            return;
        }
        
        if (p.getFrequency() == o.getFrequency()) {
            ba.sendPrivateMessage(name, "You cannot challenge your own team.");
            return;
        } else if (teams.containsKey(Integer.valueOf(o.getFrequency()))) {
            ba.sendPrivateMessage(name, "The opposing team is currently in a duel and cannot be challenged.");
            return;
        } else if (teams.containsKey(Integer.valueOf(p.getFrequency()))) {
            ba.sendPrivateMessage(name, "You are currently in a duel.");
            return;
        }

        String[] names1 = { "", "" };
        String[] names2 = { "", "" };
        int freq1 = p.getFrequency();
        int freq2 = o.getFrequency();
        Iterator<Player> i = ba.getFreqPlayerIterator(freq1);
        Iterator<Player> j = ba.getFreqPlayerIterator(freq2);
        while (i.hasNext() && j.hasNext()) {
            Player p1 = i.next();
            Player p2 = j.next();

            if (!players.containsKey(p1.getPlayerName().toLowerCase()) || !players.containsKey(p2.getPlayerName().toLowerCase())) return;

            if (names1[0].length() > 0)
                names1[1] = p1.getPlayerName();
            else
                names1[0] = p1.getPlayerName();

            if (names2[0].length() > 0)
                names2[1] = p2.getPlayerName();
            else
                names2[0] = p2.getPlayerName();
        }
        
        DuelPlayer[] tests = new DuelPlayer[] {
                players.get(names1[0].toLowerCase()),
                players.get(names1[1].toLowerCase()),
                players.get(names2[0].toLowerCase()),
                players.get(names2[1].toLowerCase())
        };
        
        if (ranked) {
            boolean clear = true;
            for (DuelPlayer dp : tests) {
                if (!dp.canPlay()) {
                    clear = false;
                    ba.sendOpposingTeamMessageByFrequency(freq1, "[ERROR] Player has been disabled or is not registered for ranked play: " + dp.getName());
                }
            }
            if (!clear)
                return;
        }

        final String key = "" + freq1 + " " + freq2 + "";
        if (challs.containsKey(key)) {
            DuelChallenge ch = challs.get(key);
            ba.cancelTask(ch);
            challs.remove(key);
            ba.sendOpposingTeamMessageByFrequency(freq1, "Previous challenge is being replaced...");
        }
        
        tests[0].setDuel(names1[1], freq1);
        tests[1].setDuel(names1[0], freq1);
        tests[2].setDuel(names2[1], freq2);
        tests[3].setDuel(names2[0], freq2);

        DuelChallenge chall = new DuelChallenge(this, ba, ranked, freq1, freq2, names1, names2, div);
        challs.put(key, chall);
        ba.scheduleTask(chall, 60000);
        ba.sendOpposingTeamMessageByFrequency(freq1, (ranked ? "[RANKED] " : "[SCRIM] ")
                + "You have challenged " + names2[0] + " and " + names2[1] + " to a "
                + (ranked ? "RANKED " : "CASUAL ") + getDivision(div)
                + " duel. This challenge will expire in 1 minute.", 26);
        ba.sendOpposingTeamMessageByFrequency(freq2, (ranked ? "[RANKED] " : "[SCRIM] ")
                + "You are being challenged to a " + (ranked ? "RANKED " : "CASUAL ")
                + getDivision(div) + " duel by " + names1[0] + " and " + names1[1]
                + ". Use !a <name> (<name> is one of your opponenents) to accept.", 26);
    }

    /** Handles the !a accept challenge command */
    private void cmd_accept(String name, String[] args) {
        if (args.length != 1) return;

        Player nme = ba.getFuzzyPlayer(args[0]);
        if (nme == null) {
            ba.sendPrivateMessage(name, "Player not found.");
            return;
        }

        Player p = ba.getPlayer(name);
        if (teams.containsKey(Integer.valueOf(p.getFrequency()))) {
            ba.sendPrivateMessage(name, "You are already dueling.");
            return;
        } else if (teams.containsKey(Integer.valueOf(nme.getFrequency()))) return;
        
        String key = "" + nme.getFrequency() + " " + p.getFrequency() + "";
        if (!challs.containsKey(key)) {
            ba.sendPrivateMessage(name, "Challenge not found.");
            return;
        }

        DuelChallenge chall = challs.remove(key);
        ba.cancelTask(chall);
        if (chall.getDiv() == 2 && getBoxOpen(2)) {
            DuelGame game = new DuelGame(getDuelBox(2), chall, ba, this);
            game.startGame();
            removeChalls(nme.getFrequency(), p.getFrequency());
        } else if (chall.getDiv() != 2 && getBoxOpen(1)) {
            DuelGame game = new DuelGame(getDuelBox(1), chall, ba, this);
            game.startGame();
            removeChalls(nme.getFrequency(), p.getFrequency());
        } else {
            ba.sendOpposingTeamMessageByFrequency(
                    chall.freq1(),
                    "No duel boxes are currently available for this division. Please try again later.",
                    26);
            ba.sendOpposingTeamMessageByFrequency(
                    chall.freq2(),
                    "No duel boxes are currently available for this division. Please try again later.",
                    26);
        }
    }

    /** Handles the !score command */
    private void cmd_score(String name, String msg) {
        String p = msg.substring(msg.indexOf(" ") + 1);
        if (p == null || p.length() < 1) {
            ba.sendPrivateMessage(name, "Invalid player name entered");
            return;
        }
        if (playing.containsKey(p.toLowerCase()))
            ba.sendPrivateMessage(name, games.get(playing.get(p.toLowerCase())).getScore());
        else {
            p = ba.getFuzzyPlayerName(p);
            if (p != null && playing.containsKey(p.toLowerCase()))
                ba.sendPrivateMessage(name, games.get(playing.get(p.toLowerCase())).getScore());
            else
                ba.sendPrivateMessage(name, "Player or duel not found");
        }
    }
    
    private void cmd_stats(String name, String msg) {
        if (msg.contains(" ") && msg.length() > 7) {
            String p = ba.getFuzzyPlayerName(msg.substring(msg.indexOf(" ") + 1));
            if (p != null && playing.containsKey(p.toLowerCase())) {
                String[] stats = games.get(playing.get(p.toLowerCase())).getStats();
                if (stats != null)
                    ba.smartPrivateMessageSpam(name, stats);
                else
                    ba.sendSmartPrivateMessage(name, "Error getting duel stats.");
            } else
                ba.sendSmartPrivateMessage(name, "Player not dueling.");
        } else {
            if (playing.containsKey(name.toLowerCase())) {
                String[] stats = games.get(playing.get(name.toLowerCase())).getStats();
                if (stats != null)
                    ba.smartPrivateMessageSpam(name, stats);
                else
                    ba.sendSmartPrivateMessage(name, "Error getting duel stats.");
            } else
                ba.sendSmartPrivateMessage(name, "You are not currently in a duel.");
        }
    }
    
    private void cmd_lag(String name, String cmd) {
        String[] args = splitArgs(cmd);
        if (args != null) {
            if (players.containsKey(args[0].toLowerCase()))
                lagHandler.requestLag(args[0], name);
            else
                ba.sendSmartPrivateMessage(name, "Could not process lag request on missing player '" + args[0] + "'");
        } else
            lagHandler.requestLag(name, name);
    }

    /** Handles the !lagout command */
    private void cmd_lagout(String name) {
        if (laggers.containsKey(name.toLowerCase()))
            laggers.get(name.toLowerCase()).doLagout();
        else
            ba.sendPrivateMessage(name, "You are not lagged out.");
    }
    
    
    private void cmd_cancel(String name) {
        String orig = name;
        name = name.toLowerCase();
        if (playing.containsKey(name)) {
            DuelGame game = games.get(playing.get(name));
            DuelTeam[] team = game.getTeams(name);
            if (game.getState() == DuelGame.ENDING) {
                ba.sendPrivateMessage(name, "You cannot cancel the duel at this time.");
                return;
            }
            if (team[0].setCancel()) {
                if (team[1].getCancel())
                    game.cancelDuel(null);
                else {
                    ba.sendOpposingTeamMessageByFrequency(team[0].getFreq(), orig + " has sent a request to cancel this duel.", 26);
                    ba.sendOpposingTeamMessageByFrequency(team[1].getFreq(), orig + " wishes to cancel this duel. Private message !cancel to " + ba.getBotName() + ", to accept.", 26);
                }
            } else {
                ba.sendOpposingTeamMessageByFrequency(team[0].getFreq(), orig + " has removed the request to cancel this duel.", 26);
                ba.sendOpposingTeamMessageByFrequency(team[1].getFreq(), orig + " has removed the request to cancel this duel.", 26);
            }
        } else
            ba.sendPrivateMessage(name, "You are not currently dueling.");
    }
    
    private void cmd_die(String name) {
        ba.sendSmartPrivateMessage(name, "Disconnecting...");
        this.handleDisconnect();
    }
    
    @Override
    public void handleDisconnect() {
        ba.cancelTasks();
        ba.scheduleTask((new TimerTask() {
            public void run() {
                ba.die();
            }
        }), 2000);
    }
    
    /** Removes all challenges involving two specific freqs
     * 
     * @param freq1
     * @param freq2 */
    public void removeChalls(int freq1, int freq2) {
        Vector<String> keys = new Vector<String>();
        for (String k : challs.keySet())
            if (k.contains("" + freq1) || k.contains("" + freq2)) keys.add(k);

        while (!keys.isEmpty()) {
            String k = keys.remove(0);
            ba.cancelTask(challs.remove(k));
        }
    }

    /** Removes all challenges involving a specific freq
     * 
     * @param freq */
    public void removeChalls(int freq) {
        Vector<String> keys = new Vector<String>();
        for (String k : challs.keySet())
            if (k.contains("" + freq)) keys.add(k);

        while (!keys.isEmpty()) {
            String k = keys.remove(0);
            ba.cancelTask(challs.remove(k));
        }
    }

    /** Returns the division name for a given id
     * 
     * @param div
     *            division id number
     * @return division String name */
    public String getDivision(int div) {
        if (div == 1)
            return "Warbird";
        else if (div == 2)
            return "Javelin";
        else if (div == 3)
            return "Spider";
        else if (div == 4 || div == 7)
            return "Lancaster";
        else if (div == 5)
            return "Mixed";
        else
            return "Unknown";
    }

    /** @return current game id */
    public int getID(boolean ranked) {
        if (ranked) {
            gameID++;
            return gameID;
        } else {
            scrimID--;
            return scrimID;
        }
    }
    
    public DuelPlayer getPlayer(String name) {
        if (players.containsKey(name.toLowerCase()))
                return players.get(name.toLowerCase());
        return laggers.get(name.toLowerCase());
    }
    
    private String getPartner(String name) {
        String partner = null;
        Player p = ba.getPlayer(name);
        if (p != null) {
            int freq = p.getFrequency();
            if (ba.getFrequencySize(freq) == 2) {
                Iterator<Player> i = ba.getFreqPlayerIterator(freq);
                while (i.hasNext()) {
                    partner = i.next().getPlayerName();
                    if (!name.equalsIgnoreCase(partner))
                        return partner;
                }
            }
        }
        return partner;
    }

    /** Checks to see if a DuelBox is open for a given division
     * 
     * @param division
     *            the division id number
     * @return true if an open box exists 
     */
    private boolean getBoxOpen(int division) {
        int i = 0;
        Iterator<String> it = boxes.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            DuelBox b = boxes.get(key);
            if (!b.inUse() && b.gameType(division)) i++;
        }
        if (i == 0)
            return false;
        else
            return true;
    }

    /** Returns an open DuelBox
     * 
     * @param division
     *            the division id number
     * @return an open DuelBox for the given division */
    private DuelBox getDuelBox(int division) {
        Vector<DuelBox> v = new Vector<DuelBox>();
        Iterator<String> it = boxes.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            DuelBox b = boxes.get(key);
            if (!b.inUse() && b.gameType(division)) v.add(b);
        }
        if (v.size() == 0)
            return null;
        else {
            Random generator = new Random();
            return v.elementAt(generator.nextInt(v.size()));
        }
    }
    
    private void getLeagueOps() {
        String[] args = settings.getString("HeadOps").split(",");
        for (String n : args)
            heads.add(n);
        args = settings.getString("LeagueOps").split(",");
        for (String n : args)
            ops.add(n);
    }

    private void handleInfo(String msg) {
        //Sorts information from *info
        String[] pieces = msg.split("  ");
        String name = pieces[3].substring(10);
        String ip = pieces[0].substring(3);
        String mid = pieces[5].substring(10);
        if (alias.containsKey(name.toLowerCase())) {
            //String query = "SELECT u.fcUserName as n FROM tblDuel2__player p LEFT JOIN tblUser u ON p.fnUserID = u.fnUserID WHERE ";
            //query += "fnEnabled = 1 AND (fcIP = '" + ip + "' OR (fcIP = '" + ip + "' AND fnMID = " + mid + "))";    		
            String query = "SELECT U.fcUserName as n FROM tblDuel2__player DP LEFT JOIN tblUser U ON DP.fnUserID = U.fnUserID WHERE DP.fnUserID IN (SELECT a2.fnUserID FROM tblAlias a1 JOIN tblAlias a2 ON a1.fnIP = a2.fnIP WHERE a1.fnUserID = (SELECT fnUserID FROM tblUser WHERE fcUserName = '"+Tools.addSlashesToString(name)+"' ORDER BY fnUserID ASC LIMIT 1)) AND fnEnabled = 1 LIMIT 100";
    		
            ba.SQLBackgroundQuery(db, "alias:" + alias.remove(name.toLowerCase()), query);
        }
        DuelPlayer p = getPlayer(name);
        if (p != null)
            p.sql_createPlayer(ip, mid);
    }
    
    private void sql_loadBans() {
        banned = new CinSet();
        String query = "SELECT u.fcUserName FROM tblDuel2__ban b LEFT JOIN tblUser u ON u.fnUserID = b.fnUserID WHERE b.fnActive = 1";
        ResultSet rs = null;
        try {
            rs = ba.SQLQuery(db, query);
            while (rs.next())
                banned.add(rs.getString(1));
        } catch (SQLException e) {
            debug("Error loading bans.");
            Tools.printStackTrace(e);
        } finally {
            ba.SQLClose(rs);
        }
    }
    
    private void sql_banPlayer(String name, String player, String comment) {
        if (banned.contains(player))
            ba.sendSmartPrivateMessage(name, player + " is already banned.");
        else {
            int id = -1;
            DuelPlayer p = getPlayer(player);
            if (p != null) {
                id = p.userID;
                p.banned = true;
            } else {
                UserData d = new UserData(ba, player);
                id = d.getUserID();
            }
            if (id < 0) {
                ba.sendSmartPrivateMessage(name, "Error getting user ID for " + player + ".");
                return;
            }
            banned.add(player);
            String query = "INSERT INTO tblDuel2__ban (fnUserID, fcComment, fcStaffer) VALUES(" + id + ", '" + Tools.addSlashesToString(comment) + "', '" + Tools.addSlashesToString(name) + "')";
            ba.SQLBackgroundQuery(db, null, query);
            ba.sendSmartPrivateMessage(name, player + " has been banned from 2v2 league dueling.");
        }
    }
    
    private void sql_unbanPlayer(String name, String player) {
        if (!banned.contains(player))
            ba.sendSmartPrivateMessage(name, player + " is not banned.");
        else {
            int id = -1;
            DuelPlayer p = getPlayer(player);
            if (p != null) {
                id = p.userID;
                p.banned = false;
            } else {
                UserData d = new UserData(ba, player);
                id = d.getUserID();
            }
            if (id < 0) {
                ba.sendSmartPrivateMessage(name, "Error getting user ID for " + player + ".");
                return;
            }
            banned.remove(player);
            String query = "UPDATE tblDuel2__ban SET fnActive = 0 WHERE fnUserID = " + id;
            ba.SQLBackgroundQuery(db, null, query);
            ba.sendSmartPrivateMessage(name, player + " is allowed to play in 2v2 league duels again.");
        }
    }

    private void sql_getUserInfo(String staff, String name) {
        String query = "SELECT P.fnUserID as id, P.fnEnabled as e, P.fcIP as ip, P.fnMID as mid FROM tblDuel2__player P WHERE P.fnUserID = (SELECT fnUserID FROM tblUser WHERE fcUserName = '"
                                    + Tools.addSlashesToString(name) + "' ORDER BY fnUserID ASC LIMIT 1) LIMIT 1";
        ba.SQLBackgroundQuery(db, "info:" + staff + ":" + name, query);
    }
    
    private boolean isOp(String name) {
        return oplist.isSmod(name) || ops.contains(name) || heads.contains(name);
    }

    /** Splits the arguments of a given command separated by colons
     * 
     * @param cmd
     *            command String
     * @return String array of the results args */
    private String[] splitArgs(String cmd) {
        String[] result = null;
        if (cmd.contains(" ") && ((cmd.indexOf(" ") + 1) != cmd.length())) 
            if (!cmd.contains(":")) {
                result = new String[1];
                result[0] = cmd.substring(cmd.indexOf(" ") + 1);
            } else
                result = cmd.substring(cmd.indexOf(" ") + 1).split(":");
        return result;
    }

    /** Debug message handler */
    public void debug(String msg) {
        if (DEBUG) ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }
}
