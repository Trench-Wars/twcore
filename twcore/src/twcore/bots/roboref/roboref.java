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
import java.util.Vector;

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
import twcore.core.util.Tools;

/**
 * New bot to manage elim jav and wb
 * @author WingZero
 */
public class roboref extends SubspaceBot {
    
    BotAction ba;
    OperatorList oplist;
    BotSettings rules;
    
    public enum State { OFF, IDLE, WAITING, VOTING, STARTING, PLAYING, ENDING }
    public enum VoteType { NA, SHIP, DEATHS, SHRAP, GAME }
    public enum ShipType {
        WARBIRD(1, 0, false), 
        JAVELIN(2, 1, true), 
        SPIDER(3, 0, false), 
        LEVIATHEN(4, 1, true), 
        TERRIER(5, 0, false), 
        WEASEL(6, 0, false),
        LANCASTER(7, 0, false), 
        SHARK(8, 1, true);
        
        private int ship, freq;
        private boolean shrap;
        
        private static final Map<Integer, ShipType> lookup = new HashMap<Integer, ShipType>();
        
        static {
            for (ShipType s : EnumSet.allOf(ShipType.class))
                lookup.put(s.getNum(), s);
        }
        
        private ShipType(int num, int startFreq, boolean canShrap) {
            ship = num;
            freq = startFreq;
            shrap = canShrap;
        }
        
        public static ShipType type(int n) {
            return lookup.get(n);
        }
        
        public int getNum() {
            return ship;
        }
        
        public boolean hasShrap() {
            return shrap;
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
    }
    
    State state;
    VoteType voteType;
    ShipType shipType;
    
    Random random;
    
    ElimGame game;
    Vector<ElimGame> gameLog;
    HashMap<String, Integer> votes;
    
    boolean DEBUG;
    String debugger;
    
    private String connectionID;
    static final String db = "website";
    static final int INITIAL_RATING = 300;
    
    TimerTask timer;
    String lastWinner;
    ElimPlayer winner;
    int deaths;
    int winStreak;
    int voteTime;
    String[] updateFields;
    boolean shrap;
    boolean arenaLock;

    private PreparedStatement updateStats, updateRank;
    
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
        DEBUG = true;
        debugger = "WingZero";
        gameLog = new Vector<ElimGame>();
        votes = new HashMap<String, Integer>();
        state = State.IDLE;
        voteType = VoteType.NA;
        updateFields = "fnKills, fnDeaths, fnMultiKills, fnKillStreak, fnDeathStreak, fnWinStreak, fnShots, fnKillJoys, fnKnockOuts, fnTopMultiKill, fnTopKillStreak, fnTopDeathStreak, fnTopWinStreak, fnAve, fnRating, fnAim, fnWins, fnGames, fnShip, fcName".split(", ");
        updateStats = ba.createPreparedStatement(db, connectionID, "UPDATE tblElim__Player SET fnKills = ?, fnDeaths = ?, fnMultiKills = ?, fnKillStreak = ?, fnDeathStreak = ?, fnWinStreak = ?, fnShots = ?, fnKillJoys = ?, fnKnockOuts = ?, fnTopMultiKill = ?, fnTopKillStreak = ?, fnTopDeathStreak = ?, fnTopWinStreak = ?, fnAve = ?, fnRating = ?, fnAim = ?, fnWins = ?, fnGames = ? WHERE fnShip = ? AND fcName = ?");
        updateRank = ba.createPreparedStatement(db, connectionID, "SET @i=0; UPDATE tblElim__Player SET fnRank = (@i:=@i+1) WHERE fnShip = ? AND (fnKills + fnDeaths) > " + INITIAL_RATING + " ORDER BY fnRating DESC");
        if (updateStats == null || updateRank == null) {
            debug("Update was null.");
            new Die(debugger);
        }
        ba.joinArena(rules.getString("Arena1"));
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
        if (state == State.WAITING)
            ba.sendPrivateMessage(name, "A new game will begin when there are at least two (2) people playing.");
        else if (state == State.VOTING)
            ba.sendPrivateMessage(name, "We are voting for the next game.");
        else if (state == State.STARTING || state == State.PLAYING || state == State.ENDING)
            ba.sendPrivateMessage(name, game.toString());
    }

    /** Handles PlayerLeft events */
    public void handleEvent(PlayerLeft event) {
        if (state == State.OFF || game == null) return; 
        game.handleEvent(event);
    }

    /** Handles the WeaponFired event which reports shot stats if a game is being played */
    public void handleEvent(WeaponFired event) {
        if (state == State.OFF) return; 
        if (state != State.PLAYING || game == null) return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (name != null)
            game.getPlayer(name).handleShot();
    }

    /** Handles ship and freq change events if a game is being played */
    public void handleEvent(FrequencyShipChange event) {
        if (state == State.OFF) return; 
        if (state == State.PLAYING || state == State.STARTING)
            game.handleEvent(event);
        if (state == State.WAITING)
            handleState();
    }
    
    /** Handles received background queries for stats */
    public void handleEvent(SQLResultEvent event) {
        String id = event.getIdentifier();
        debug("SQL event id: " + id);
        ResultSet rs = event.getResultSet();
        try {
            if (state == State.OFF) { 
                ba.SQLClose(rs);
                return;
            } else if (game != null && id.startsWith("load:")) {
                String name = id.substring(id.indexOf(":") + 1).toLowerCase();
                ElimPlayer ep = game.getPlayer(name);
                if (ep == null) return;
                if (rs.next() && rs.getInt("fnShip") == shipType.getNum()) {
                    ep.loadStats(rs);
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
            }
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
        ba.SQLClose(rs);
    }

    public void handleEvent(PlayerPosition event) {
        if (game != null && state == State.PLAYING)
            game.handleEvent(event);            
    }

    /** Death event handler sends if a game is being played */
    public void handleEvent(PlayerDeath event) {
        if (state != State.PLAYING || game == null) return;
        game.handleEvent(event);
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
        }
        
        if (type == Message.PUBLIC_MESSAGE) {
            if (state == State.VOTING && Tools.isAllDigits(msg))
                handleVote(name, msg);
        }
        
        if (type == Message.PRIVATE_MESSAGE) {
            if (state == State.OFF)
                ba.sendPrivateMessage(name, "Bot disabled.");
            else if (msg.equals("!lagout"))
                cmd_lagout(name);
            else if (msg.startsWith("!stats"))
                cmd_stats(name, msg);
            else if (msg.startsWith("!get"))
                cmd_get(name, msg);
            if (game != null) {
                if (msg.startsWith("!mvp"))
                    game.cmd_mvp(name);
            }
        }
        
        if (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) {
            if (oplist.isSmod(name)) {
                if (msg.startsWith("!die"))
                    cmd_die(name);
                else if (msg.equals("!debug"))
                    cmd_debug(name);
                else if (msg.equals("!off"))
                    cmd_stop(name);
                else if (msg.equals("!on"))
                    cmd_start(name);
            }
        }
    }
    
    /** Handles the !stats command which displays stats from the current game if possible */
    public void cmd_stats(String name, String cmd) {
        if (game == null) {
            ba.sendPrivateMessage(name, "There must be a game running.");
            return;
        }
        ElimPlayer ep;
        if (cmd.contains(" ") && cmd.length() > 7) {
            ep = game.getPlayer(cmd.substring(cmd.indexOf(" ") + 1));
        } else
            ep = game.getPlayer(name);
        if (ep != null)
            ba.privateMessageSpam(name, ep.getStatStrings());
        else
            ba.sendPrivateMessage(name, "Error!");
    }
    
    /** Handles the !get command which displays stats from the database for a particular ship */
    public void cmd_get(String name, String cmd) {
        if (cmd.contains(" ") && cmd.length() > 5) {
            String p = name;
            int ship = 1;
            try {
                if (cmd.contains(":")) {
                    p = cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":"));
                    ship = Integer.valueOf(cmd.substring(cmd.indexOf(":") + 1).trim());
                } else
                    ship = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1).trim());
            } catch (NumberFormatException e) {
                ba.sendPrivateMessage(name, "Invalid ship number.");
                return;
            }
            String query = "SELECT * FROM tblElim__Player WHERE fnShip = " + ship + " and fcName = '" + Tools.addSlashesToString(p) + "'";
            ba.SQLBackgroundQuery(db, "stats:" + name + ":" + p + ":" + ship, query);
        } else
            ba.sendPrivateMessage(name, "Invalid command!");        
    }
    
    /** Handles the !lagout command which returns a lagged out player to the game */
    public void cmd_lagout(String name) {
        if (game != null) {
            ElimPlayer ep = game.getPlayer(name);
            if (ep != null) {
                if (ep.status == Status.LAGGED)
                    game.handleLagin(name);
                else
                    ba.sendPrivateMessage(name, "You are not in the game.");
            } else
                ba.sendPrivateMessage(name, "You are not in the game.");
        } else
            ba.sendPrivateMessage(name, "There is not a game being played.");
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
        } else {
            ba.sendChatMessage(name + " has overriden " + debugger + " as the target of debug messages.");
            ba.sendSmartPrivateMessage(name, "Debugging still ENABLED and you have replaced " + debugger + " as the debugger.");
            debugger = name;
        }
    }
    
    /** Kills the bot */
    public void cmd_die(String name) {
        ba.sendSmartPrivateMessage(name, "Disconnecting...");
        new Die(name);
    }
    
    public void cmd_stop(String name) {
        state = State.OFF;
        ba.cancelTasks();
        if (game != null) {
            game = null;
            ba.sendArenaMessage("Bot disabled and current game aborted.", Tools.Sound.NOT_DEALING_WITH_ATT);
        } else
            ba.sendArenaMessage("Bot has been disabled.", Tools.Sound.NOT_DEALING_WITH_ATT);
    }
    
    public void cmd_start(String name) {
        state = State.IDLE;
        handleState();
    }
    
    /** Handles potential votes read from public chat during a voting period */
    public void handleVote(String name, String cmd) {
        name = name.toLowerCase();
        int vote = Integer.valueOf(cmd);
        if (voteType == VoteType.SHIP) {
            if (vote > 0 && vote < 9) {
                votes.put(name, vote);
                ba.sendPrivateMessage(name, "Vote counted for: " + vote);
            }
        } else if (voteType == VoteType.DEATHS) {
            if (vote > 0 && vote <= rules.getInt("MaxDeaths")) {
                votes.put(name, vote);
                ba.sendPrivateMessage(name, "Vote counted for: " + vote);
            }
        } else if (voteType == VoteType.SHRAP) {
            if (vote > -1 && vote < 2) {
                votes.put(name, vote);
                ba.sendPrivateMessage(name, "Vote counted for: " + vote);
            }
        }
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
            for (int i = 0; i < updateFields.length; i++) {
                String col = updateFields[i];
                if (col.equals("fcName"))
                    updateStats.setString(i + 1, name.name);
                else if (col.equals("fnShip"))
                    updateStats.setInt(i + 1, stats.getShip());
                else {
                    StatType stat = StatType.sql(col);
                    if (stat.isInt())
                        updateStats.setInt(i + 1, stats.getDB(stat));
                    else if (stat.isDouble())
                        updateStats.setDouble(i + 1, stats.getAimDB(stat));
                    else if (stat.isFloat())
                        updateStats.setFloat(i + 1, stats.getAveDB(stat));
                }
            }
            updateStats.executeUpdate();
            if (game.gotUpdate(name.name)) {
                state = State.ENDING;
                handleState();
            }
            debug("Updated " + name.name);
        } catch (SQLException e) {
            Tools.printStackTrace("Elim player stats update error!", e);
        }
    }
    
    /** Handles game state by calling the appropriate state methods */
    private void handleState() {
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
        if (ba.getNumPlayers() > 1) {
            state = State.VOTING;
            voteType = VoteType.NA;
            handleState();
        }
    }
    
    /** Voting state runs in between vote periods to call for next vote after counting prior */
    private void doVoting() {
        if (voteType == VoteType.NA) {
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
        ba.scheduleTask(task, 3 * Tools.TimeInMillis.SECOND);
    }
    
    /** Starting state creates new ElimGame and initiates player stat trackers */
    private void doStarting() {
        game = new ElimGame(this, shipType, deaths, shrap);
        ba.sendArenaMessage("Enter to play. Game will be locked in 30 seconds!", 9);
        timer = new TimerTask() {
            public void run() {
                arenaLock = true;
                ba.toggleLocked();
                game.startGame();
            }
        };
        ba.scheduleTask(timer, 10000);
    }
    
    /** Playing state runs after winner is set and ends game accordingly */
    private void doPlaying() {
        if (winner != null && game != null && game.mvp != null) {
            ba.sendArenaMessage("Game over. Winner: " + winner.name + "! ", 5);
            ba.sendArenaMessage("MVP: " + game.mvp, Tools.Sound.INCONCEIVABLE);    
            winner.saveWin();
            game.storeLosses();
            if (lastWinner != null && lastWinner.equalsIgnoreCase(winner.name))
                winStreak++;
            else
                winStreak = 0;
            lastWinner = winner.name;
            updatePlayer(winner);
            winner = null;        
        }
    }
    
    /** Ending state stores old game reference, updates rankings and prepares for next event */
    private void doEnding() {
        state = State.WAITING;
        gameLog.add(0, game);
        game = null;
        arenaLock = false;
        ba.toggleLocked();
        TimerTask enter = new TimerTask() {
            public void run() {
                updateRanks();
                debug("Rank update executed for ship " + shipType.getNum());
                if (ba.getNumPlayers() < 2)
                    ba.sendArenaMessage("A new game will begin when 2 or more players enter a ship. -" + ba.getBotName());
                handleState();
            }
        };
        ba.scheduleTask(enter, 3000);
    }
    
    /** Executes the updateRank statement which adjusts the ranks of every elim player */
    private void updateRanks() {
        try {
            updateRank.setInt(1, shipType.getNum());
            updateRank.execute();
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
            if (wins.size() > 0) {
                if (high > 0) {
                    int num = random.nextInt(wins.size());
                    ship = wins.toArray(new Integer[wins.size()])[num];
                } else
                    ship = random.nextInt(2) + 1;
            } else
                ship = wins.first();
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
    
    /** Debug message handler */
    private void debug(String msg) {
        if (DEBUG)
            ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }
    
    @Override
    public void handleDisconnect() {
        new Die("disconnect handler");
    }
    
    public class MVP extends TimerTask {
        public MVP() {
            ba.scheduleTask(this, 4000);
        }
        
        public void run() {
            ba.sendArenaMessage("MVP: " + game.mvp, Tools.Sound.INCONCEIVABLE);
        }
    }
    
    /** Cleanly kills bot tasks and closes statements */
    private class Die extends TimerTask {

        String name;
        
        public Die(String name) {
            this.name = name;
            ba.scheduleTask(this, 1000);
            ba.closePreparedStatement(db, connectionID, updateStats);
            ba.closePreparedStatement(db, connectionID, updateRank);
        }
        
        @Override
        public void run() {
            ba.cancelTasks();
            ba.die("Discconnect requested by: " + name);
        }
    }
}
