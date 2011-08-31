package twcore.bots.roboref;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.TimerTask;
import java.util.TreeSet;

import twcore.bots.roboref.ElimPlayer.Status;
import twcore.bots.roboref.StatType;
import twcore.core.SubspaceBot;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.SQLResultEvent;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;
import twcore.core.util.Spy;
import twcore.core.util.Tools;

/**
 * New bot to manage elim jav and wb
 * @author WingZero
 */
public class roboref extends SubspaceBot {
    
    BotAction ba;
    OperatorList oplist;
    BotSettings rules;
    
    public enum State { OFF, IDLE, WAITING, VOTING, STARTING, PLAYING, ENDING, UPDATING }
    public enum VoteType { NA, SHIP, DEATHS, SHRAP, GAME }
    public enum ShipType {
        WARBIRD(1, 0), 
        JAVELIN(2, 1), 
        SPIDER(3, 0), 
        LEVIATHAN(4, 0), 
        TERRIER(5, 1), 
        WEASEL(6, 1),
        LANCASTER(7, 0), 
        SHARK(8, 1);
        
        private int ship, freq;
        
        private static final Map<Integer, ShipType> lookup = new HashMap<Integer, ShipType>();
        
        static {
            for (ShipType s : EnumSet.allOf(ShipType.class))
                lookup.put(s.getNum(), s);
        }
        
        private ShipType(int num, int startFreq) {
            ship = num;
            freq = startFreq;
        }
        
        public static ShipType type(int n) {
            return lookup.get(n);
        }
        
        public int getNum() {
            return ship;
        }
        
        public boolean hasShrap() {
            return (ship == 8 || ship == 2);
        }
        
        public int getFreq() {
            return freq;
        }
        
        public boolean inBase() {
            if (freq == 0)
                return false;
            else
                return true;
        }
        
        public String getType() {
            if (inBase())
                return "BASEELIM";
            else
                return "ELIM";
        }
    }
    
    State state;
    VoteType voteType;
    ShipType shipType;
    
    Random random;
    
    ElimGame game;
    HashMap<String, Integer> votes;
    HashSet<String> alerts;
    
    boolean DEBUG;
    String debugger;
    HashSet<String> debugStatPlayers;
    
    private String connectionID;
    static final String db = "website";
    static final int INITIAL_RATING = 300;
    static final int MIN_ZONER = 15;       // The minimum amount of minutes in between zoners
    static final int ALERT_DELAY = 2;      // Minimum amount of time between alert messages
    
    TimerTask timer;
    String arena;
    ElimPlayer lastWinner;
    ElimPlayer winner;
    int deaths;
    int winStreak;
    int voteTime;
    long lastZoner, lastAlert;
    int[] voteStats;                       // ( # wb games, # jav games, # voted wb but got jav, # voted jav but got wb, # unanimous wb/jav, ties )
    String[] updateFields;
    boolean shrap;
    boolean arenaLock;
    boolean hiderCheck;

    private Spy spy;
    
    private PreparedStatement updateStats;
    //private PreparedStatement updateRank;
    
    public roboref(BotAction botAction) {
        super(botAction);
        ba = botAction;
        connectionID = "elimplayerstats"; 
        random = new Random();
    }

    /** Handles the LoggedOn event which prepares the bot to run the elim arena */
    public void handleEvent(LoggedOn event) {
        requestEvents();
        oplist = ba.getOperatorList();
        rules = ba.getBotSettings();
        arena = rules.getString("Arena1");
        if (rules.getInt("Zoners") == 1)
            lastZoner = System.currentTimeMillis();
        else
            lastZoner = -1;
        lastAlert = 0;
        spy = new Spy(ba);
        DEBUG = false;
        debugger = "";
        voteStats = new int[] { 0, 0, 0, 0, 0, 0, 0 };
        votes = new HashMap<String, Integer>();
        alerts = new HashSet<String>();
        debugStatPlayers = new HashSet<String>();
        state = State.IDLE;
        voteType = VoteType.NA;
        updateFields = "fnKills, fnDeaths, fnMultiKills, fnKillStreak, fnDeathStreak, fnWinStreak, fnShots, fnKillJoys, fnKnockOuts, fnTopMultiKill, fnTopKillStreak, fnTopDeathStreak, fnTopWinStreak, fnAve, fnRating, fnAim, fnWins, fnGames, fnShip, fcName".split(", ");
        updateStats = ba.createPreparedStatement(db, connectionID, "UPDATE tblElim__Player SET fnKills = ?, fnDeaths = ?, fnMultiKills = ?, fnKillStreak = ?, fnDeathStreak = ?, fnWinStreak = ?, fnShots = ?, fnKillJoys = ?, fnKnockOuts = ?, fnTopMultiKill = ?, fnTopKillStreak = ?, fnTopDeathStreak = ?, fnTopWinStreak = ?, fnAve = ?, fnRating = ?, fnAim = ?, fnWins = ?, fnGames = ?, ftUpdated = NOW() WHERE fnShip = ? AND fcName = ?");
        //updateRank = ba.createPreparedStatement(db, connectionID, "SET @i=0; UPDATE tblElim__Player SET fnRank = (@i:=@i+1) WHERE fnShip = ? AND (fnKills + fnDeaths) > " + INITIAL_RATING + " ORDER BY fnRating DESC");
        if (updateStats == null) {
            debug("Update was null.");
            this.handleDisconnect();
        }
        ba.joinArena(arena);
    }
    
    /** Handles ArenaJoined event which initializes bot startup */
    public void handleEvent(ArenaJoined event) {
        voteTime = rules.getInt("Time");
        ba.sendUnfilteredPublicMessage("?chat=" + rules.getString("Chats"));
        game = null;
        shipType = null;
        ba.receiveAllPlayerDeaths();
        ba.setPlayerPositionUpdating(300);
        winStreak = 0;
        lastWinner = null;
        winner = null;
        arenaLock = false;
        hiderCheck = true;
        ba.toggleLocked();
        ba.specAll();
        state = State.IDLE;
        handleState();
    }

    /** Handles PlayerEntered events announcing state of elim */
    public void handleEvent(PlayerEntered event) {
        if (state == State.OFF) return; 
        String name = event.getPlayerName();
        if (name == null || name.length() < 1)
            name = ba.getPlayerName(event.getPlayerID());
        cmd_status(name);
    }

    /** Handles PlayerLeft events */
    public void handleEvent(PlayerLeft event) {
        if (state == State.OFF || game == null) return; 
        game.handleEvent(event);
    }

    /** Handles the WeaponFired event which reports shot stats if a game is being played */
    public void handleEvent(WeaponFired event) {
        if (state != State.PLAYING || game == null) return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (name != null)
            game.getPlayer(name).handleShot();
    }

    /** Handles ship and freq change events if a game is being played */
    public void handleEvent(FrequencyShipChange event) {
        if (state == State.PLAYING || state == State.STARTING) {
            if (game != null);
                game.handleEvent(event);
        }
        if (state == State.WAITING)
            handleState();
    }

    /** Handles PlayerPosition events */
    public void handleEvent(PlayerPosition event) {
        if (game != null && (state == State.PLAYING || state == State.STARTING))
            game.handleEvent(event);            
    }

    /** Death event handler sends if a game is being played */
    public void handleEvent(PlayerDeath event) {
        if (state != State.PLAYING || game == null) return;
        game.handleEvent(event);
    }
    
    /** Handles received background queries for stats */
    public void handleEvent(SQLResultEvent event) {
        String id = event.getIdentifier();
        ResultSet rs = event.getResultSet();
        try {
            if (state == State.OFF) { 
                ba.SQLClose(rs);
                return;
            } else if (game != null && id.startsWith("load:")) {
                String name = id.substring(id.indexOf(":") + 1).toLowerCase();
                if (rs.next() && rs.getInt("fnShip") == shipType.getNum()) {
                    game.handleStats(name, rs);
                } else {
                    ba.SQLQueryAndClose(db, "INSERT INTO tblElim__Player (fcName, fnShip) VALUES('" + Tools.addSlashesToString(name) + "', " + shipType.getNum() + ")");
                    ba.SQLBackgroundQuery(db, "load:" + name, "SELECT * FROM tblElim__Player WHERE fnShip = " + shipType.getNum() + " AND fcName = '" + Tools.addSlashesToString(name) + "' LIMIT 1");
                }
            } else if (id.startsWith("stats:")) {
                String[] args = id.split(":");
                if (args.length == 4) {
                    ElimStats stats = new ElimStats(Integer.valueOf(args[3]));
                    if (rs.next()) {
                        stats.loadStats(rs);
                        ba.privateMessageSpam(args[1], stats.getStats(args[2]));
                    } else
                        ba.sendPrivateMessage(args[1], "No " + Tools.shipName(Integer.valueOf(args[3])) + " stats found for " + args[2]);
                }
            } else if (id.startsWith("allstats:")) {
                String[] args = id.split(":");
                if (args.length == 3) {
                    ElimStats stats = new ElimStats(1);
                    if (rs.next()) {
                        stats.loadAllShips(rs);
                        ba.privateMessageSpam(args[1], stats.getAll(args[2]));
                    } else
                        ba.sendPrivateMessage(args[1], "No stats found for " + args[2]);
                }
            } else if (id.startsWith("rec:")) {
                String[] args = id.split(":");
                if (rs.next()) {
                    int k = rs.getInt("k");
                    int d = rs.getInt("d");
                    String target = rs.getString("n");
                    if (args.length == 3)
                        ba.sendPrivateMessage(args[1], "" + target + "[" + args[2] + "]: (" + k + "-" + d + ")");
                    else if (args.length == 2)
                        ba.sendPrivateMessage(args[1], "" + target + "[all]: (" + k + "-" + d + ")");
                } else
                    ba.sendPrivateMessage(args[1], "Error processing record request for: " + args[2]);
            } else if (id.startsWith("rank:")) {
                String[] args = id.split(":");
                if (rs.next()) {
                    String target = rs.getString("n");
                    int r = rs.getInt("r");
                    int s = rs.getInt("s");
                    ba.sendPrivateMessage(args[1], target + " rank in " + ShipType.type(s).toString() + ": " + r);
                } else
                    ba.sendPrivateMessage(args[1], args[2] + " is not yet ranked in " + ShipType.type(Integer.valueOf(args[3])).toString());
            }
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
        ba.SQLClose(rs);
    }

    /** Message event handler checks arena lock and diffuses commands */
    public void handleEvent(Message event) {
        int type = event.getMessageType();
        String msg = event.getMessage();
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null)
            name = event.getMessager();
        
        if (type == Message.ARENA_MESSAGE) {
            if (msg.equals("Arena UNLOCKED") && arenaLock)
                ba.toggleLocked();
            else if (msg.equals("Arena LOCKED") && !arenaLock)
                ba.toggleLocked();
            else if (game != null)
                game.handleHider(msg);
        }
        
        if (type == Message.PUBLIC_MESSAGE) {
            if (state == State.VOTING && Tools.isAllDigits(msg))
                handleVote(name, msg);
        }
        
        msg = msg.toLowerCase();
        
        if (type == Message.PRIVATE_MESSAGE) {
            if (state == State.OFF)
                ba.sendPrivateMessage(name, "Bot disabled.");
            else if (msg.equals("!help"))
                cmd_help(name);
            else if (msg.equals("!lagout"))
                cmd_lagout(name);
            else if (msg.startsWith("!rank "))
                cmd_rank(name, msg);
            else if (msg.startsWith("!rec "))
                cmd_rec(name, msg);
            else if (msg.startsWith("!stats"))
                cmd_stats(name, msg);
            else if (msg.startsWith("!streak"))
                cmd_streak(name, msg);
            else if (msg.startsWith("!scorereset") || msg.startsWith("!sr"))
                cmd_scorereset(name, msg);
        }
        
        if (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) {
            if (msg.equals("!alert"))
                cmd_alert(name);
            else if (msg.equals("!who"))
                cmd_who(name);
            else if (msg.startsWith("!mvp"))
                cmd_mvp(name);
            else if (msg.equals("!deaths"))
                cmd_deaths(name);
            else if (msg.equals("!status"))
                cmd_status(name);
            else if (msg.equals("!votes") || msg.equals("!vi"))
                cmd_votes(name);
            
            if (oplist.isZH(name)) {
                if (msg.equals("!die"))
                    cmd_die(name);
                else if (msg.equals("!off") || msg.equals("!stop"))
                    cmd_stop(name);
                else if (msg.equals("!on") || msg.equals("!start"))
                    cmd_start(name);
                else if (msg.equals("!zone"))
                    cmd_zone(name);
                else if (msg.startsWith("!remove ") || msg.startsWith("!rem ") || msg.startsWith("!rm "))
                    cmd_remove(name, msg);
            }
            if (oplist.isSmod(name)) {
                if (msg.startsWith("!debug"))
                    cmd_debug(name);
                else if (msg.startsWith("!ds "))
                    cmd_debugStats(name, msg);
                else if (msg.startsWith("!hide"))
                    cmd_hiderFinder(name);
                else if (msg.startsWith("!greet "))
                    cmd_greet(name, msg);
                else if (msg.startsWith("!game "))
                    cmd_game(name, msg);
            }
        }
        spy.handleEvent(event);
    }
    
    /** Handles potential votes read from public chat during a voting period */
    public void handleVote(String name, String cmd) {
        Player p = ba.getPlayer(name);
        if (p != null && p.getShipType() == 0) {
            ba.sendPrivateMessage(name, "You must be in a ship in order to vote.");
            return;
        }
        name = name.toLowerCase();
        int vote = -1;
        try {
            vote = Integer.valueOf(cmd);
        } catch (NumberFormatException e) {
            return;
        }
        if (voteType == VoteType.SHIP) {
            if (vote > 0 && vote < 9) {
                votes.put(name, vote);
                ba.sendPrivateMessage(name, "Vote counted for: " + ShipType.type(vote).toString());
            }
        } else if (voteType == VoteType.DEATHS) {
            if (vote > 0 && vote <= rules.getInt("MaxDeaths")) {
                votes.put(name, vote);
                ba.sendPrivateMessage(name, "Vote counted for: " + vote + " deaths");
            }
        } else if (voteType == VoteType.SHRAP) {
            if (vote > -1 && vote < 2) {
                votes.put(name, vote);
                String shrap = "ON";
                if (vote == 0)
                    shrap = "OFF";
                ba.sendPrivateMessage(name, "Vote counted for: " + shrap);
            }
        }
    }
    
    /** Handles the !help command */
    public void cmd_help(String name) {
        String[] msg = new String[] {
                ",-- Robo Ref Commands -------------------------------------------------------------------.",
                "|!votes            - Warbird and Javelin vote analysis                                   |",
                "|!alert            - Toggles new game private message alerts on or off                   |",
                "|!lagout           - Return to game after lagging out                                    |",
                "|!status           - Displays current game status                                        |",
                "|!rank <#>         - Shows your current rank in ship <#>                                 |",
                "|!rank <name>:<#>  - Shows the rank of <name> in ship <#>                                |",
                "|!rec <#>          - Gets your own record in ship <#>                                    |",
                "|!rec <name>:<#>   - Gets the record of <name> in ship <#>                               |",
                "|                     NOTE: Use -1 as <#> for all ships                                  |",
                "|!who              - Lists the remaining players and their records                       |",
                "|!stats <#>        - Spams all of your ship <#> statistics if available                  |",
                "|!stats <name>:<#> - Spams all statistic information for <name> in ship <#> if available |",
                "|                     NOTE: Use -1 as <#> for all ships                                  |",
                "|!streak           - Displays your current streak information                            |",
                "|!streak <name>    - Displays streak information for <name>                              |",
                "|!mvp              - Lists current game best/worst player record information             |",
                "|!deaths           - Lists current game most/least player death information              |",
                "|!scorereset <#>   - Resets all scores and statistics for the ship <#> specified (!sr)   |",
        };
        ba.privateMessageSpam(name, msg);
        if (oplist.isZH(name)) {
            msg = new String[] {
                    ":-- Staff Commands ----------------------------------------------------------------------+",
                    "|!die              - Forces the bot to shutdown and log off                              |",
                    "|!stop             - Kills current game and prevents any future games (!off)             |",
                    "|!start            - Begins game and enables games to continue running (!on)             |",
                    "|!zone             - Forces the bot to send a default zone message                       |",
                    //"|!remove <name>    - Removes <name> from the current game                                |",
            };
            ba.privateMessageSpam(name, msg);
        }
        if (oplist.isSmod(name)) {
            msg = new String[] {
                    ":-- Smod Commands -----------------------------------------------------------------------+",
                    "|!hider            - Disables the hiding player checker/reporter (only during games)     |",
                    "|!debug            - Toggles the debugger which sends debug messages to you when enabled |",
                    "|!greet <msg>      - Changes the greeting message for the arena                          |",
            };
            ba.privateMessageSpam(name, msg);
        }
        ba.sendPrivateMessage(name, "`----------------------------------------------------------------------------------------'");
    }
    
    public void cmd_votes(String name) {
        String[] msg = {
                "Games     WB: " + padNum("" + voteStats[0], 3) + " | Jav: " + padNum("" + voteStats[1], 3),
                "Outvoted  WB: " + padNum("" + voteStats[3], 3) + " | Jav: " + padNum("" + voteStats[2], 3),
                "Unanimous WB: " + padNum("" + voteStats[4], 3) + " | Jav: " + padNum("" + voteStats[5], 3),
                "Ties: " + voteStats[6],
        };
        ba.smartPrivateMessageSpam(name, msg);
    }
    
    private String padNum(String str, int len) {
        while (str.length() < len)
            str = " " + str;
        return str;
    }
    
    /** Handles the !remove player command which will spec the player and erase stats */
    public void cmd_remove(String name, String cmd) {
        if (cmd.indexOf(" ") + 1 == cmd.length()) return;
        if (game != null && (state == State.STARTING || state == State.PLAYING))
            game.do_remove(name, cmd.substring(cmd.indexOf(" ") + 1));
        else
            ba.sendSmartPrivateMessage(name, "There is currently no game being played.");
    }
    
    /** Handles the !status command which displays current bot or game state */
    public void cmd_status(String name) {
        if (state == State.WAITING)
            ba.sendSmartPrivateMessage(name, "A new game will begin when there are at least two (2) people playing.");
        else if (state == State.VOTING)
            ba.sendSmartPrivateMessage(name, "We are voting on the next game.");
        else if (state == State.STARTING || state == State.PLAYING || state == State.ENDING)
            ba.sendSmartPrivateMessage(name, game.toString());
    }
    
    /** Handles the !scorereset (sr) command which resets the stats for the specified ship */
    public void cmd_scorereset(String name, String cmd) {
        if (!cmd.contains(" ")) {
            ba.sendPrivateMessage(name, "You have to specify the ship whose scores you want to reset.");
            return;
        }
        if (cmd.indexOf(" ") + 1 >= cmd.length()) return;
        int ship = -1;
        try {
            ship = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1));
            if (ship < 1 || ship > 8) {
                ba.sendPrivateMessage(name, "Invalid ship number, " + ship + "!");
                return;
            }
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Invalid syntax, please use !sr <#>");
            return;
        }
        ElimPlayer ep = null;
        if (game != null) {
            ep = game.getPlayer(name);
            if (ep != null) {
                if (shipType.getNum() == ship && ep.isPlaying() && ep.getStatus() != Status.LAGGED) {
                    ba.sendPrivateMessage(name, "You can not do a scorereset while playing a game with the ship you want to reset.");
                    return;
                } else 
                    ep.scorereset(ship);
            }
        }
        try {
            ba.SQLQueryAndClose(db, "DELETE FROM tblElim__Player WHERE fnShip = " + ship + " AND fcName = '" + Tools.addSlashesToString(name) + "'");
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
        if (shipType != null && ship == shipType.getNum() && ep != null && state == State.STARTING)
            ba.SQLBackgroundQuery(db, "load:" + name, "SELECT * FROM tblElim__Player WHERE fnShip = " + ship + " AND fcName = '" + Tools.addSlashesToString(name) + "' LIMIT 1");
        ba.sendPrivateMessage(name, "Your " + ShipType.type(ship) + " scores have been reset.");
    }
    
    /** Handles the !stats command which displays stats for a given ship if possible */
    public void cmd_stats(String name, String cmd) {
        int ship = -1;
        String target = name;
        if (!cmd.contains(":")) {
            try {
                ship = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1));
                if (ship != -1 && (ship < 1 || ship > 8)) {
                    ba.sendPrivateMessage(name, "Invalid ship: " + ship);
                    return;
                }
            } catch (NumberFormatException e) {
                ba.sendPrivateMessage(name, "Error processing specified ship number! Please use !stats <#> or !stats <name>:<#>");
                return;
            }
        } else if (cmd.indexOf(":") < cmd.length()) {
            target = cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":"));
            try {
                ship = Integer.valueOf(cmd.substring(cmd.indexOf(":") + 1));
            } catch (NumberFormatException e) {
                ba.sendPrivateMessage(name, "Error parsing ship number!");
                return;
            }
            if (ship != -1 && (ship < 1 || ship > 8)) {
                ba.sendPrivateMessage(name, "Invalid ship: " + ship);
                return;
            }
        } else {
            ba.sendPrivateMessage(name, "Error, invalid syntax! Please use !stats <name>:<#> or !stats <#>");
            return;
        }
        if (ship != -1)
            ba.SQLBackgroundQuery(db, "stats:" + name + ":" + target + ":" + ship, "SELECT * FROM tblElim__Player WHERE fnShip = " + ship + " AND fcName = '" + Tools.addSlashesToString(target) + "' LIMIT 1");
        else
            ba.SQLBackgroundQuery(db, "allstats:" + name + ":" + target, "SELECT * FROM tblElim__Player WHERE fcName = '" + Tools.addSlashesToString(target) + "' LIMIT 8");
    }
    
    /** Handles the !who command which displays the remaining players and their records */
    public void cmd_who(String name) {
        if (game != null && state == State.PLAYING)
            game.do_who(name);
        else 
            ba.sendPrivateMessage(name, "There is no game being played at the moment.");
    }
    
    /** Handles the !mvp command which lists the top 3 best and worst players */
    public void cmd_mvp(String name) {
        if (game != null && state == State.PLAYING)
            game.do_mvp(name);
        else 
            ba.sendPrivateMessage(name, "There is no game being played at the moment.");
    }
    
    /** Handles the !deaths command which lists the players with most deaths and least deaths */
    public void cmd_deaths(String name) {
        if (game != null && state == State.PLAYING)
            game.do_deaths(name);
        else 
            ba.sendPrivateMessage(name, "There is no game being played at the moment.");
    }
    
    /** Handles the !streak command which will show current streak stats if a game is being played */
    public void cmd_streak(String name, String cmd) {
        if (game != null && state == State.PLAYING)
            game.do_streak(name, cmd);
        else 
            ba.sendPrivateMessage(name, "There is no game being played at the moment.");        
    }
    
    /** Handles the !rank command which returns a players rank according to ship */
    public void cmd_rank(String name, String cmd) {
        if (cmd.length() < 7) return;
        int ship = 1;
        String target = name;
        if (!cmd.contains(":")) {
            try {
                ship = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1));
                if (ship < 1 || ship > 8) {
                    ba.sendPrivateMessage(name, "Invalid ship: " + ship);
                    return;
                }
            } catch (NumberFormatException e) {
                ba.sendPrivateMessage(name, "Error processing specified ship number! Please use !rank <#>");
                return;
            }
        } else if (cmd.indexOf(":") < cmd.length()) {
            debug(":=" + cmd.indexOf(":") + " length=" + cmd.length());
            target = cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":"));
            try {
                ship = Integer.valueOf(cmd.substring(cmd.indexOf(":") + 1));
            } catch (NumberFormatException e) {
                ba.sendPrivateMessage(name, "Error parsing ship number!");
                return;
            }
            if (ship < 1 || ship > 8) {
                ba.sendPrivateMessage(name, "Invalid ship: " + ship);
                return;
            }
        } else {
            ba.sendPrivateMessage(name, "Error, invalid syntax! Please use !rank <name>:<#> or !rank <#>");
            return;
        }
        String query = "SELECT fcName as n, fnRank as r, fnShip as s FROM tblElim__Player WHERE fnShip = " + ship + " AND fcName = '" + Tools.addSlashesToString(target) + "' LIMIT 1";
        ba.SQLBackgroundQuery(db, "rank:" + name + ":" + target + ":" + ship, query);
    }
    
    /** Handles the !rec command which displays a player's current overall record */
    public void cmd_rec(String name, String cmd) {
        if (cmd.length() < 5) return;
        int ship = -1;
        String target = name;
        if (!cmd.contains(":")) {
            try {
                ship = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1));
                if (ship != -1 && (ship < 1 || ship > 8)) {
                    ba.sendPrivateMessage(name, "Invalid ship: " + ship);
                    return;
                }
            } catch (NumberFormatException e) {
                ba.sendPrivateMessage(name, "Error processing specified ship number! Please use !rec <#> or !rec <name>:<#>");
                return;
            }
        } else if (cmd.indexOf(":") < cmd.length()) {
            target = cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":"));
            try {
                ship = Integer.valueOf(cmd.substring(cmd.indexOf(":") + 1));
            } catch (NumberFormatException e) {
                ba.sendPrivateMessage(name, "Error parsing ship number!");
                return;
            }
            if (ship != -1 && (ship < 1 || ship > 8)) {
                ba.sendPrivateMessage(name, "Invalid ship: " + ship);
                return;
            }
        } else {
            ba.sendPrivateMessage(name, "Error, invalid syntax! Please use !rec <name>:<#> or !rec <#>");
            return;
        }
        if (ship != -1)
            ba.SQLBackgroundQuery(db, "rec:" + name + ":" + ship, "SELECT fnKills as k, fnDeaths as d, fcName as n FROM tblElim__Player WHERE fnShip = " + ship + " AND fcName = '" + Tools.addSlashesToString(target) + "' LIMIT 1");
        else
            ba.SQLBackgroundQuery(db, "rec:" + name, "SELECT SUM(fnKills) as k, SUM(fnDeaths) as d, fcName as n FROM tblElim__Player WHERE fcName = '" + Tools.addSlashesToString(target) + "' LIMIT 8");
    }
    
    /** Handles the !lagout command which returns a lagged out player to the game */
    public void cmd_lagout(String name) {
        if (game != null) {
            ElimPlayer ep = game.getPlayer(name);
            if (ep != null) {
                if (ep.getStatus() == Status.LAGGED)
                    game.do_lagout(name);
                else
                    ba.sendPrivateMessage(name, "You are not in the game.");
            } else
                ba.sendPrivateMessage(name, "You are not in the game.");
        } else
            ba.sendPrivateMessage(name, "There is not a game being played.");
    }
    
    /** Handles the !alert command which enables or disables new game alert pms */
    public void cmd_alert(String name) {
        if (alerts.remove(name.toLowerCase()))
            ba.sendSmartPrivateMessage(name, "New game alert messages DISABLED.");
        else {
            alerts.add(name.toLowerCase());
            ba.sendSmartPrivateMessage(name, "New game alert messages ENABLED.");
        }
    }
    
    public void cmd_game(String name, String cmd) {
        // !game 2;10;0
        state = State.OFF;
        ba.cancelTasks();
        game = null;
        if (!cmd.contains(";") || cmd.length() < 10) return;
        String[] args = cmd.substring(cmd.indexOf(" ") + 1).split(";");
        if (args.length != 3) return;
        try {
            int ship = Integer.valueOf(args[0]);
            int deaths = Integer.valueOf(args[1]);
            int shrap = Integer.valueOf(args[2]);
            if (ship < 1 || ship > 8)
                ba.sendPrivateMessage(name, "Invalid ship: " + ship);
            else if (deaths < 1 || deaths > 15)
                ba.sendPrivateMessage(name, "Invalid deaths: " + deaths);
            else {
                shipType = ShipType.type(ship);
                this.deaths = deaths;
                if (shrap == 1)
                    this.shrap = true;
                else
                    this.shrap = false;
                ba.sendArenaMessage("Game of " + shipType.toString() + " elim to " + deaths + " forcefully created by " + name + "!");
                state = State.STARTING;
                handleState();                
            }
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Invalid syntax, please use !game ship#;deaths#;shrap#");
            return;
        }
    }
    
    /** Handles the !debug command which enables or disables debug mode */
    public void cmd_debug(String name) {
        if (!DEBUG) {
            debugger = name;
            DEBUG = true;
            ba.sendSmartPrivateMessage(name, "Debugging ENABLED. You are now set as the debugger.");
        } else if (debugger.equalsIgnoreCase(name)){
            debugger = "";
            DEBUG = false;
            ba.sendSmartPrivateMessage(name, "Debugging DISABLED and debugger reset.");
            debugStatPlayers.clear();
        } else {
            ba.sendChatMessage(name + " has overriden " + debugger + " as the target of debug messages.");
            ba.sendSmartPrivateMessage(name, "Debugging still ENABLED and you have replaced " + debugger + " as the debugger.");
            debugger = name;
        }
    }
    
    public void cmd_debugStats(String name, String cmd) {
        if (!debugger.equalsIgnoreCase(name)) {
            ba.sendSmartPrivateMessage(name, "You are not set as the debugger.");
            return;
        }
        if (cmd.contains(" ") && cmd.length() > 7) {
            String p = cmd.substring(cmd.indexOf(" ") + 1);
            if (debugStatPlayers.remove(p.toLowerCase()))
                ba.sendSmartPrivateMessage(name, p + " has been removed from the debug list.");
            else {
                debugStatPlayers.add(p.toLowerCase());
                ba.sendSmartPrivateMessage(name, p + " has been added to the debug list.");
            }
        }
    }
    
    /** Handles the !greet command which is used to change the arena greeting message */
    public void cmd_greet(String name, String cmd) {
        if (cmd.length() < 8) return;
        ba.sendUnfilteredPublicMessage("?set misc:greetmessage:" + cmd.substring(cmd.indexOf(" ") + 1));
        ba.sendSmartPrivateMessage(name, "Greeting set to: " + cmd.substring(cmd.indexOf(" ") + 1));
    }
    
    /** Handles the !hider command which will start or stop the hiding player task in the current game */
    public void cmd_hiderFinder(String name) {
        hiderCheck = !hiderCheck;
        if (game != null && state == State.PLAYING)
            game.do_hiderFinder(name);
        else {
            if (hiderCheck)
                ba.sendSmartPrivateMessage(name, "The hider finder has been ENABLED for subsequent games.");
            else
                ba.sendSmartPrivateMessage(name, "The hider finder has been DISABLED for subsequent games.");
        }
    }
    
    /** Kills the bot */
    public void cmd_die(String name) {
        ba.sendSmartPrivateMessage(name, "Disconnecting...");
        this.handleDisconnect();
    }
    
    /** Handles the !stop command which turns the bot off and prevents games from running */
    public void cmd_stop(String name) {
        state = State.OFF;
        ba.cancelTasks();
        if (game != null) {
            game = null;
            ba.sendArenaMessage("Bot disabled and current game aborted.", Tools.Sound.NOT_DEALING_WITH_ATT);
        } else
            ba.sendArenaMessage("Bot has been disabled.", Tools.Sound.NOT_DEALING_WITH_ATT);
    }
    
    /** Handles the !start command which restarts the bot after being stopped */
    public void cmd_start(String name) {
        state = State.IDLE;
        handleState();
    }
    
    /** Forces a zone message to be sent regardless of how long ago the last zoner was */
    public void cmd_zone(String name) {
        lastZoner = 0;
        sendZoner();
    }
    
    /** Sets the winner of the last elim event prompting end game routines */
    public void setWinner(ElimPlayer winner) {
        this.winner = winner;
        handleState();
    }
    
    /**
     * Saves a particular player's stats to the database using a prepared statement. 
     * It then reports the player updated to determine if updates are complete.
     */
    public void updatePlayer(ElimPlayer name) {
        ElimStats stats = name.getStats();
        try {
            updateStats.clearParameters();
            String debugs = "";
            for (int i = 0; i < updateFields.length; i++) {
                String col = updateFields[i];
                if (col.equals("fcName")) {
                    updateStats.setString(i + 1, Tools.addSlashesToString(name.name));
                    debugs += "Name(" + name.name + ") ";
                } else if (col.equals("fnShip")) {
                    updateStats.setInt(i + 1, stats.getShip());
                    debugs += col + "(" + stats.getShip() + ") ";
                } else {
                    StatType stat = StatType.sql(col);
                    if (stat.isInt()) {
                        updateStats.setInt(i + 1, stats.getDB(stat));
                        debugs += col + "(" + stats.getDB(stat) + ") ";
                    } else if (stat.isDouble()) {
                        updateStats.setDouble(i + 1, stats.getAimDB(stat));
                        debugs += col + "(" + stats.getAimDB(stat) + ") ";
                    } else if (stat.isFloat()) {
                        updateStats.setFloat(i + 1, stats.getAveDB(stat));
                        debugs += col + "(" + stats.getAveDB(stat) + ") ";
                    }
                }
            }
            updateStats.execute();
            if (debugStatPlayers.contains(name.name.toLowerCase()))
                debug(debugs);
            else
                debug("Updated player: " + name.name + " Count: " + updateStats.getUpdateCount());
            if (game.gotUpdate(name.name)) {
                state = State.ENDING;
                handleState();
            }
        } catch (SQLException e) {
            Tools.printStackTrace("Elim player stats update error!", e);
        }
    }
    
    /** Handles game state by calling the appropriate state methods */
    public void handleState() {
        switch (state) {
            case IDLE: doIdle(); break;
            case WAITING: doWaiting(); break;
            case VOTING: doVoting(); break;
            case STARTING: doStarting(); break;
            case PLAYING: doPlaying(); break;
            case ENDING: doEnding(); break;
        }
    }
    
    /** Idle state simply puts the bot into motion after spawning or being disabled */
    private void doIdle() {
        ba.sendArenaMessage("Starting up...", Tools.Sound.CROWD_OOO);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (ba.getNumPlaying() < 2) 
                    ba.sendArenaMessage("A new game will begin when there are at least two (2) people playing.");
                state = State.WAITING;
                handleState();
            }
        };
        ba.scheduleTask(task, 3000);
    }
    
    /** Waiting state stalls until there are enough players to continue */
    private void doWaiting() {
        sendZoner();
        if (ba.getNumPlaying() > 1) {
            sendAlerts();
            state = State.VOTING;
            votes.clear();
            voteType = VoteType.NA;
            handleState();
        }
    }
    
    /** Voting state runs in between vote periods to call for next vote after counting prior */
    private void doVoting() {
        if (state != State.VOTING) return;
        if (voteType == VoteType.NA) {
            ba.sendChatMessage(2, "The next game of ELIMBETA is about to begin. We are voting on ship.");
            ba.sendChatMessage(3, "The next game of ELIMBETA is about to begin. We are voting on ship.");
            voteType = VoteType.SHIP;
            ba.sendArenaMessage("VOTE: 1-Warbird, 2-Javelin, 3-Spider, 4-Leviathen, 5-Terrier, 6-Weasel, 7-Lancaster, 8-Shark", Tools.Sound.BEEP3);
        } else if (voteType == VoteType.SHIP) {
            voteType = VoteType.DEATHS;
            ba.sendArenaMessage("This will be " + Tools.shipName(shipType.getNum()) + " elim. VOTE: How many deaths? (1-" + rules.getInt("MaxDeaths") + ")");
        } else if (voteType == VoteType.DEATHS) {
            if (shipType.hasShrap()) {
                voteType = VoteType.SHRAP;
                ba.sendArenaMessage("" + Tools.shipName(shipType.getNum()) + " elim to " + deaths + ". VOTE: Shrap on or off? 0-OFF, 1-ON");
            } else {
                shrap = false;
                state = State.STARTING;
                String msg = "" + Tools.shipName(shipType.getNum()) + " elim to " + deaths + ". ";
                ba.sendArenaMessage(msg);
                handleState();
                return;
            }
        } else {
            state = State.STARTING;
            String msg = "" + Tools.shipName(shipType.getNum()) + " elim to " + deaths + ". ";
            if (shipType.hasShrap()) {
                if (shrap)
                    msg += "Shrap: [ON]";
                else
                    msg += "Shrap: [OFF]";
            }
            ba.sendArenaMessage(msg);
            handleState();
            return;            
        }
        
        TimerTask task = new TimerTask() {
            public void run() {
                countVotes();
            }
        };
        ba.scheduleTask(task, 10 * Tools.TimeInMillis.SECOND);
    }
    
    /** Starting state creates new ElimGame and initiates player stat trackers */
    private void doStarting() {
        game = new ElimGame(this, shipType, deaths, shrap);
        ba.sendArenaMessage("Enter to play. Arena will be locked in 15 seconds!", 9);
        timer = new TimerTask() {
            public void run() {
                if (ba.getNumPlaying() < 2) {
                    game.stop();
                    abort();
                } else {
                    String erules = "RULES: One player per freq and NO TEAMING! Die " + deaths + " times and you're out. ";
                    if (shipType == ShipType.WEASEL)
                        erules += "Warping is illegal and will be penalized with by a death.";
                    ba.sendArenaMessage(erules);
                    arenaLock = true;
                    ba.toggleLocked();
                    game.checkStats();
                }
            }
        };
        ba.scheduleTask(timer, 15000);
    }
    
    /** Playing state runs after winner is set and ends game accordingly */
    private void doPlaying() {
        if (winner != null && game != null && game.mvp != null) {
            ba.sendArenaMessage("Game over. Winner: " + winner.name + "! ", 5);
            final String mvp = game.mvp;
            ba.sendChatMessage(2, "" + winner.name + " has won " + shipType.toString() + " elim.");
            TimerTask t = new TimerTask() {
                public void run() {
                    ba.sendArenaMessage("MVP: " + mvp, Tools.Sound.INCONCEIVABLE);  
                }
            };
            ba.scheduleTask(t, 3000);
            game.storeLosses();
            updatePlayer(winner);  
            if (lastWinner != null && lastWinner.name.equalsIgnoreCase(winner.name))
                winStreak++;
            else
                winStreak = 0;
            lastWinner = winner;
            winner = null;
        }
    }
    
    /** Ending state updates rankings and prepares for next event */
    private void doEnding() {
        state = State.UPDATING;
        arenaLock = false;
        game = null;
        ba.toggleLocked();
        TimerTask enter = new TimerTask() {
            public void run() {
                updateRanks();
                state = State.WAITING;
                if (ba.getNumPlaying() < 2)
                    ba.sendArenaMessage("A new game will begin when 2 or more players enter a ship. -" + ba.getBotName());
            }
        };
        ba.scheduleTask(enter, 3000);
    }
    
    /** Executes the updateRank statement which adjusts the ranks of every elim player */
    private void updateRanks() {
        try {
            long now = System.currentTimeMillis();
            // removed: AND (fnKills + fnDeaths) > " + INITIAL_RATING + "
            ResultSet rs = ba.SQLQuery(db, "SET @i=0; UPDATE tblElim__Player SET fnRank = (@i:=@i+1) WHERE fnShip = " + shipType.getNum() + "  ORDER BY fnRating DESC");
            if (rs.next())
                debug("Rank update for " + shipType.toString() + " was successful");
            else
                debug("Rank update for " + shipType.toString() + " returned an empty result set");
            ba.SQLClose(rs);
            //updateRank.setInt(1, shipType.getNum());
            //updateRank.execute();
            debug("Update completed in " + ((System.currentTimeMillis() - now) / Tools.TimeInMillis.SECOND) + " seconds.");
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }
    
    /** Counts votes after voting ends and acts accordingly */
    private void countVotes() {
        if (voteType == VoteType.SHIP) {
            int[] count = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
            for (Integer i : votes.values())
                count[i - 1]++;
            TreeSet<Integer> wins = new TreeSet<Integer>();
            int high = count[0];
            int ship = 1;
            wins.add(ship);
            for (int i = 1; i < 8; i++) {
                if (count[i] > high) {
                    wins.clear();
                    high = count[i];
                    ship = i + 1;
                    wins.add(ship);
                } else if (count[i] == high)
                    wins.add(i + 1);
            }
            if (wins.size() > 1) {
                voteStats[6]++;
                if (high > 0) {
                    int num = random.nextInt(wins.size());
                    ship = wins.toArray(new Integer[wins.size()])[num];
                } else
                    ship = random.nextInt(2) + 1;
            } else
                ship = wins.first();
            // ( # wb games, # jav games, # voted wb but got jav, # voted jav but got wb, # unanimous wb/jav, ties )
            if (ship == 1) {
                voteStats[0]++;
                if (count[1] > 0)
                    voteStats[3] += count[1];
                else
                    voteStats[4]++;
            } else if (ship == 2) {
                voteStats[1]++;
                if (count[0] > 0)
                    voteStats[2] += count[0];
                else
                    voteStats[5]++;
            }
            shipType = ShipType.type(ship);
            votes.clear();
        } else if (voteType == VoteType.DEATHS) {
            int[] count = new int[rules.getInt("MaxDeaths")];
            for (int i = 0; i < count.length; i++)
                count[i] = 0;
            for (Integer i : votes.values())
                count[i - 1]++;
            HashSet<Integer> wins = new HashSet<Integer>();
            int high = count[0];
            int val = 1;
            wins.add(val);
            for (int i = 0; i < rules.getInt("MaxDeaths"); i++) {
                if (count[i] > high) {
                    wins.clear();
                    high = count[i];
                    val = i + 1;
                    wins.add(val);
                } else if (count[i] == high)
                    wins.add(i + 1);
            }
            if (wins.size() > 1) {
                if (high > 0) {
                    int num = random.nextInt(wins.size());
                    this.deaths = wins.toArray(new Integer[wins.size()])[num];
                } else
                    this.deaths = random.nextInt(rules.getInt("MaxDeaths")) + 1;
            } else
                this.deaths = wins.iterator().next();
            votes.clear();
        } else if (voteType == VoteType.SHRAP) {
            int[] count = new int[] { 0, 0 };
            for (Integer i : votes.values())
                count[i]++;
            if (count[0] > count[1])
                shrap = false;
            else if (count[1] > count[0])
                shrap = true;
            else
                shrap = false;
            votes.clear();
        }
        handleState();
    }
    
    /** Checks to make sure it didn't already send a recent alert and then sends pms */
    private void sendAlerts() {
        long now = System.currentTimeMillis();
        if ((now - lastAlert) < ALERT_DELAY * Tools.TimeInMillis.MINUTE) return; 
        for (String p : alerts)
            ba.sendSmartPrivateMessage(p, "The next game of elim is about to begin in ?go " + arena + " (reply with !alert to disable this message)");
        lastAlert = now;
    }
    
    /** Sends periodic zone messages advertising elim and announcing streaks */
    private void sendZoner() {
        if (lastZoner == -1) return;
        else if ((System.currentTimeMillis() - lastZoner) < (MIN_ZONER * Tools.TimeInMillis.MINUTE)) return;
        
        if (winStreak == 1)
            ba.sendZoneMessage("Next elim is starting. Last round's winner was " + lastWinner.name + " (" + lastWinner.getKills() + ":" + lastWinner.getDeaths() + ")! Type ?go " + arena + " to play -" + ba.getBotName());
        else if(winStreak > 1)
            switch (winStreak) {
                case 2: ba.sendZoneMessage("Next elim is starting. " + lastWinner.name + " (" + lastWinner.getKills() + ":" + lastWinner.getDeaths() + ") has won 2 back to back! Type ?go " + arena + " to play -" + ba.getBotName());
                    break;
                case 3: ba.sendZoneMessage(shipType.getType() + ": " + lastWinner.name + " (" + lastWinner.getKills() + ":" + lastWinner.getDeaths() + ") is on fire with a triple win! Type ?go " + arena + " to end the win streak! -" + ba.getBotName(), Tools.Sound.CROWD_OOO);
                    break;
                case 4: ba.sendZoneMessage(shipType.getType() + ": " + lastWinner.name + " (" + lastWinner.getKills() + ":" + lastWinner.getDeaths() + ") is on a rampage! 4 kills in a row! Type ?go " + arena + " to put a stop to the carnage! -" + ba.getBotName(), Tools.Sound.CROWD_GEE);
                    break;
                case 5: ba.sendZoneMessage(shipType.getType() + ": " + lastWinner.name + " (" + lastWinner.getKills() + ":" + lastWinner.getDeaths() + ") is dominating with a 5 game winning streak! Type ?go " + arena + " to end this madness! -" + ba.getBotName(), Tools.Sound.SCREAM);
                    break;
                default: ba.sendZoneMessage(shipType.getType() + ": " + lastWinner.name + " (" + lastWinner.getKills() + ":" + lastWinner.getDeaths() + ") is bringing the zone to shame with " + winStreak + " consecutive wins! Type ?go " + arena + " to redeem yourselves! -" + ba.getBotName(), Tools.Sound.INCONCEIVABLE);
                    break;
            }
        else
            m_botAction.sendZoneMessage("Next elim is starting. Type ?go " + arena + " to play -" + ba.getBotName());
        lastZoner = System.currentTimeMillis();
    }
    
    /** Requests the needed events */
    private void requestEvents() {
        EventRequester er = ba.getEventRequester();
        er.request(EventRequester.ARENA_JOINED);
        er.request(EventRequester.LOGGED_ON);
        er.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        er.request(EventRequester.MESSAGE);
        er.request(EventRequester.PLAYER_DEATH);
        er.request(EventRequester.PLAYER_ENTERED);
        er.request(EventRequester.PLAYER_LEFT);
        er.request(EventRequester.PLAYER_POSITION);
        er.request(EventRequester.WEAPON_FIRED);
    }
    
    /** Prevents the game from starting usually due to lack of players */
    public void abort() {
        game = null;
        state = State.WAITING;
        voteType = VoteType.NA;
        votes.clear();
        arenaLock = false;
        ba.toggleLocked();
        ba.sendArenaMessage("A new game will begin when there are at least two (2) people playing.");
        handleState();
    }
    
    /** Debug message handler */
    public void debug(String msg) {
        if (DEBUG)
            ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }
    
    @Override
    public void handleDisconnect() {
        ba.cancelTasks();
        ba.closePreparedStatement(db, connectionID, this.updateStats);
        //ba.closePreparedStatement(db, connectionID, this.updateRank);
        ba.scheduleTask(new Die(), 2000);
    }
    
    /** TimerTask used to guarantee the MVP has had enough time to be determined */
    public class MVP extends TimerTask {
        public MVP() {
            ba.scheduleTask(this, 4000);
        }
        
        public void run() {
            ba.sendArenaMessage("MVP: " + game.mvp, Tools.Sound.INCONCEIVABLE);
        }
    }
    
    /** Cleanly kills bot */
    private class Die extends TimerTask {
        
        @Override
        public void run() {
            ba.die();
        }
        
    }
}
