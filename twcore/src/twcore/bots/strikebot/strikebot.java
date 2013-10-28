package twcore.bots.strikebot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.TimerTask;
import java.util.TreeMap;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.BallPosition;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.SoccerGoal;
import twcore.core.game.Player;
import twcore.core.game.Ship;
import twcore.core.lvz.Objset;
import twcore.core.util.Point;
import twcore.core.util.Spy;
import twcore.core.util.Tools;

/**
 * Class StrikeBot
 * <p>
 * Used for hosting ?go strikeball
 * Main framework stolen from hockeybot.
 *
 * @author Trancid
 */
public class strikebot extends SubspaceBot {

    // Various flags
    private boolean lockArena;
    private boolean lockLastGame;
    
    // Various lists
    private SBConfig config;                                //Game configuration
    private SBTeam team0;                                   //Teams
    private SBTeam team1;
    private ArrayList<SBTeam> teams;                        //Simple arraylist to optimize some routines.
    private SBBall ball;                                    //the ball in arena
    private Spy racismWatcher;                              //Racism watcher
    private ArrayList<String> listNotplaying;               //List of notplaying players
    private ArrayList<String> listAlert;                    //List of players who toggled !subscribe on
    
    // LVZ display
    private Overlay scoreOverlay;                           // Manages the LVZ overlay display.
    
    // Game tickers & other time related stuff
    private Gameticker gameticker;                          // General ticker of the statemachine for this bot.
    private TimerTask fo_botUpdateTimer;                    // Timer that runs during the face off.
    private TimerTask ballDelay;                            // Delay for when the ball is brought into play. (Face off)
    private TimerTask statsDelay;                           // Timer that delays the display of the stats at the end of a game.
    private TimerTask mvpDelay;                             // Timer that delays the display of the MVP at the end of a game.
    private long timeStamp;                                 // Used to track the time of various key-moments.
    private int roundTime;                                  // Currently referred to in the code, but never read out. Will be used in the future.
    private int gameTime;                                   // Total (active) game time.
    
    // Zoner related stuff
    private long zonerTimestamp;                            //Timestamp of the last zoner
    private long manualZonerTimestamp;                      //Timestamp of the last manualzoner
    private int zonerWaitTime = 7;                          // Time in minutes for the automatic zoner.
    
    // Other stuff
    private int maxTimeouts;                                //Maximum allowed timeouts per game.
    
    // Frequencies
    private static final int FREQ_SPEC = 8025;              //Frequency of specced players.
    private static final int FREQ_NOTPLAYING = 2;           //Frequency of players that are !np
    

    /**
     * Game states.
     * <p>
     * This holds the various states that can be used during the game. It currently holds the following enums:
     * <ul>
     *  <li>OFF: Bot is active, but game is disabled.
     *  <li>WAITING_FOR_CAPS: First phase, adding of the captains.
     *  <li>ADDING_PLAYERS: State in which captains are setting up their teams.
     *  <li>FACE_OFF: State during the face off.
     *  <li>GAME_IN_PROGRESS: State when the ball is in play for the players.
     *  <li>REVIEW: State right after a goal is made, where it is judged if it is valid. 
     *      This is for both the automatic review as well as the manual review.
     *  <li>TIMEOUT: State during a time out.
     *  <li>GAME_OVER: State when the final stats are being displayed.
     *  <li>WAIT: Used during transition of states to prevent racing conditions.
     * </ul>
     * This enum also holds the following {@link EnumSet EnumSets}.
     * <ul>
     *  <li>ACTIVEGAME: A collection of states of the periods where there is player interaction. Basically MIDGAME, including the setup phase.
     *  It contains the following states:
     *  <ul>
     *      <li>ADDING_PLAYERS;
     *      <li>FACE_OFF;
     *      <li>GAME_IN_PROGRESS;
     *      <li>TIMEOUT;
     *      <li>WAIT.
     *  </ul>
     * </ul>
     * 
     * @see Gameticker
     * @see EnumSet
     * @author unknown, Trancid
     *
     */
    private static enum SBState {
        OFF, WAITING_FOR_CAPS, ADDING_PLAYERS, FACE_OFF,
        GAME_IN_PROGRESS, TIMEOUT, GAME_OVER, 
        WAIT;
        
        // Collection of commonly together used SBStates.
        private static final EnumSet<SBState> ACTIVEGAME = EnumSet.of(ADDING_PLAYERS,
                FACE_OFF, GAME_IN_PROGRESS, TIMEOUT, WAIT);
        
        private static final EnumSet<SBState> MIDGAME = EnumSet.of(FACE_OFF, GAME_IN_PROGRESS);
        
    };    

    private SBState currentState;   // This keeps track of the current, active state.
    
    /** Class constructor */
    public strikebot(BotAction botAction) {
        super(botAction);
        initializeVariables();  //Initialize variables
        requestEvents();        //Request Subspace Events
    }

    /** Initializes all the variables used in this class */
    private void initializeVariables() {
        config = new SBConfig();                    //Game configuration
        currentState = SBState.OFF;                 //Game state
        
        ball = new SBBall();
        team0 = new SBTeam(0);                      //Team: Freq 0
        team1 = new SBTeam(1);                      //Team: Freq 1
        
        // List containing the teams. Mainly used to optimize/shorten code.
        teams = new ArrayList<SBTeam>();
        teams.add(team0);
        teams.add(team1);
        teams.trimToSize();
        
        maxTimeouts = 1;                            // Default value of maximum timeouts.

        racismWatcher = new Spy(ba);                //Racism watcher

        listNotplaying = new ArrayList<String>();   // List of not-playing players,
        listNotplaying.add(ba.getBotName());        // including the bot.
        listAlert = new ArrayList<String>();        // List of the players who want to get alerts.

        lockArena = true;
        lockLastGame = false;
        
        scoreOverlay = new Overlay();               // LVZ display overlay.

    }

    /** Requests Subspace events */
    private void requestEvents() {
        EventRequester req = ba.getEventRequester();

        req.request(EventRequester.ARENA_JOINED);           //Bot joined arena
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);  //Player changed frequency/ship
        req.request(EventRequester.LOGGED_ON);              //Bot logged on
        req.request(EventRequester.MESSAGE);                //Bot received message
        req.request(EventRequester.PLAYER_ENTERED);         //Player entered arena
        req.request(EventRequester.PLAYER_LEFT);            //Player left arena
        req.request(EventRequester.PLAYER_DEATH);           //Player died
        req.request(EventRequester.BALL_POSITION);          //Watch ball position
        req.request(EventRequester.SOCCER_GOAL);            //A goal has been made
    }

    /*
     * Events
     */
    /**
     * Handles ArenaJoined event
     * <ul>
     *  <li>Sets up reliable kills
     *  <li>Sets up chats
     *  <li>Auto-starts bot
     * </ul>
     */
    @Override
    public void handleEvent(ArenaJoined event) {
        ba.getShip().setFreq(FREQ_NOTPLAYING);
        ba.stopReliablePositionUpdating();
        ba.sendUnfilteredPublicMessage("?chat=" + config.getChats());  //Join all the chats
    }

    /**
     * Handles FrequencyShipChange event
     * <ul>
     *  <li>since this event looks almost the same as FrequencyChange
     *  <li>event its passed on to checkFCandFSC(name, frequency, ship).
     * </ul>
     */
    @Override
    public void handleEvent(FrequencyShipChange event) {
        if (currentState != SBState.OFF && currentState != SBState.WAITING_FOR_CAPS) {
            Player p;

            p = ba.getPlayer(event.getPlayerID());
            
            SBTeam team = null;
            team = getTeam(p.getPlayerName());
            
            if (team == null){
                return;
            } else {
            if (p != null && !p.getPlayerName().equals(ba.getBotName()))
                checkFCandFSC(p.getPlayerName(), p.getFrequency(), p.getShipType());
            }
        }
    }

    /**
     * Handles LoggedOn event
     * <ul>
     *  <li>Join arena
     *  <li>Set antispam measurements
     * </ul>
     */
    @Override
    public void handleEvent(LoggedOn event) {
        short resolution;   //Screen resolution of the bot

        resolution = 3392;  //Set the maximum allowed resolution

        /* Join Arena */
        try {
            ba.joinArena(config.getArena(), resolution, resolution);
        } catch (Exception e) {
            ba.joinArena(config.getArena());
        }

        ba.setMessageLimit(10);    //Set antispam measurements
    }

    /**
     * Handles Message event
     * <ul>
     *  <li>Racism watcher
     *  <li>Arena lock
     *  <li>Player commands
     * </ul>
     */
    @Override
    public void handleEvent(Message event) {
        String message;     //Message
        String sender;      //Sender of the message
        int messageType;    //Message type

        message = event.getMessage();
        sender = ba.getPlayerName(event.getPlayerID());
        messageType = event.getMessageType();
        
        // Although the sender check is done in the racismWatcher,
        // doing it here as well saves a few executions.
        if(sender != null) {
            racismWatcher.handleEvent(event);   //Racism watcher
        }
        
        if (messageType == Message.ARENA_MESSAGE) {
            checkArenaLock(message);    //Checks if the arena should be locked
        } else if (messageType == Message.PRIVATE_MESSAGE) {
            if (sender != null) {
                handleCommand(sender, message);   //Handle commands
            }
        }
    }

    /**
     * Handles PlayerEntered event
     * <ul>
     *  <li>Sends welcome message
     *  <li>Puts the player on the corresponding frequency
     * </ul>
     */
    @Override
    public void handleEvent(PlayerEntered event) {
        if (currentState != SBState.OFF) {
            String name;    //Name of the player that entered the zone
            int pID;        //ID of the player that entered the arena
            
            name = ba.getPlayerName(event.getPlayerID());
            
            Player p;
            p = ba.getPlayer(event.getPlayerID());
            
            if (p != null) {
                pID = p.getPlayerID();
                scoreOverlay.displayAll(pID);
            }
            
            if (name != null) {
                sendWelcomeMessage(name);   //Sends welcome message with status info to the player
                putOnFreq(name);            //Puts the player on the corresponding frequency
                
            }
        } else if (currentState == SBState.OFF && config.getAllowAutoCaps()) {
            if(ba.getFrequencySize(FREQ_SPEC) 
                    + ba.getFrequencySize(team0.getFrequency()) 
                    + ba.getFrequencySize(team1.getFrequency()) 
                    >= config.getMinPlayers() * 2) {
                start();
            }
        }
    }

    /**
     * Handles PlayerLeft event
     * <ul>
     *  <li>Checks if the player that left was a captain
     *  <li>Checks if the player that left lagged out
     * </ul>
     */
    @Override
    public void handleEvent(PlayerLeft event) {
        if (currentState != SBState.OFF && currentState != SBState.WAITING_FOR_CAPS) {
            String name;    //Name of the player that left

            name = ba.getPlayerName(event.getPlayerID());
            SBTeam team = null;
            
            team = getTeam(name);
            
            if (team == null){
                return;
            }
            if (name != null) {
                //Check if the player that left was a captain
                checkCaptainLeft(name);
                //Check if the player that left was IN the game
                checkLagout(name, Tools.Ship.SPECTATOR);
            }
        }
    }

    /**
     * Handles PlayerDeath event
     */
    public void handleEvent(PlayerDeath event) {
        int idKillee, idKiller;
        SBPlayer pKillee, pKiller;
        SBTeam tKillee, tKiller;
        String killee, killer;
        
        //Get the IDs of the killer and killee from the packet.
        idKillee = event.getKilleeID();
        idKiller = event.getKillerID();
        
        //Lookup their names.
        killee = ba.getPlayerName(idKillee);
        killer = ba.getPlayerName(idKiller);
        
        //Check if we actually got their names.
        if(killee == null || killer == null)
            return;
        
        //Get the teams
        tKillee = getTeam(killee);
        tKiller = getTeam(killer);
        
        //Again, check if the fetch succeeded.
        if(tKillee == null || tKiller == null)
            return;
        
        //Get their player object
        pKillee = tKillee.getPlayer(killee);
        pKiller = tKiller.getPlayer(killer);
        
        //Final null checks
        if(pKillee == null || pKiller == null)
            return;
        
        pKillee.addDeath();
        pKiller.addKill();
    }
    
    /**
     * Handles the BallPosition event.
     * <p>
     * This will update the ball's data each time a ball update packet has been received.
     */
    @Override
    public void handleEvent(BallPosition event) {
        ball.update(event);
    }

    /**
     * Handles the SoccerGoal event
     * <p>
     * Starts the review of the goal, if it was valid or not.
     */
    @Override
    public void handleEvent(SoccerGoal event) {
        if (currentState == SBState.GAME_IN_PROGRESS) {
            startReview(event);
        }
    }

    /**
     * Handles a disconnect
     * <li>cancel all tasks
     */
    @Override
    public void handleDisconnect() {
        ba.cancelTasks();

    }

    /**
     * Removes the ball from play
     */
    public void doRemoveBall() {
        doGetBall(config.getBallTimeOut());
        
        ballDelay = new TimerTask() {
            @Override
            public void run() {
                dropBall();
            }
        }; ba.scheduleTask(ballDelay, 2 * Tools.TimeInMillis.SECOND);
    }
    
    /**
     * Causes the bot to grab the ball and goes to a specific location
     * 
     * @param location Location of where to move the ball to.
     */
    public void doGetBall(Point location) {
        fo_botUpdateTimer = new TimerTask() {
            @Override
            public void run() {
                if (ba.getShip().needsToBeSent())
                    ba.getShip().sendPositionPacket();
            }
        }; ba.scheduleTask(fo_botUpdateTimer, 0, 500);
        
        if (ba.getShip().getShip() != Ship.INTERNAL_SPIDER || !ball.holding) {
            ba.getShip().setShip(Ship.INTERNAL_SPIDER);
            ba.getShip().move(location.x, location.y);
            ba.getBall(ball.getBallID(), ball.getTimeStamp());
            ball.holding = true;
        }
    }

    /**
     * Drops the ball at current location
     */
    public void dropBall() {
        ba.cancelTask(fo_botUpdateTimer);
        //ba.specFreqAndKeepFreq(FREQ_NOTPLAYING);
        ba.getShip().setShip(Ship.INTERNAL_SPECTATOR);
        ba.getShip().setFreq(FREQ_NOTPLAYING);
        ball.holding = false;
    }

    /*
     * Commands
     */
    /**
     * Handles player commands
     *
     * @param name Sender of the command
     * @param command command
     */
    private void handleCommand(String name, String command) {
        String cmd = command.toLowerCase();
        String args = "";

        //Separate the command from its arguments if applicable.
        if(command.contains(" ")) {
            int index = command.indexOf(" ");
            cmd = cmd.substring(0, index);
            if(command.length() > ++index)
                args = command.substring(index).trim();
        }

        /* Captain commands */
        if (isCaptain(name)) {
            if (cmd.equals("!add")) {
                cmd_add(name, args);
            } else if (cmd.equals("!ready")) {
                cmd_ready(name);
            } else if (cmd.equals("!remove")) {
                cmd_remove(name, args);
            } else if (cmd.equals("!sub")) {
                cmd_sub(name, args);
            } else if (cmd.equals("!timeout") && config.getAllowAutoCaps() && (maxTimeouts > 0)) {
                cmd_timeout(name);
            }
        }

        /* Player commands */
        if (cmd.equals("!cap")) {
            cmd_cap(name);
        } else if (cmd.startsWith("!help")) {
            cmd_help(name, args);
        } else if (cmd.equals("!return")) {
            cmd_lagout(name);
        } else if (cmd.equals("!lagout")) {
            cmd_lagout(name);
        } else if (cmd.equals("!list")) {
            cmd_list(name);
        } else if (cmd.equals("!notplaying") || cmd.equals("!np")) {
            cmd_notplaying(name);
        } else if (cmd.equals("!status")) {
            cmd_status(name);
        } else if (cmd.equals("!subscribe")) {
            cmd_subscribe(name);
        }


        /* Staff commands ZH+ */
        if (ba.getOperatorList().isZH(name)) {
            if (cmd.equals("!start")) {
                cmd_start(name, args);
            } else if (cmd.equals("!stop")) {
                cmd_stop(name);
            } else if (cmd.equals("!zone")) {
                cmd_zone(name, args);
            } else if (cmd.equals("!off")) {
                cmd_off(name);
            } else if (cmd.equals("!forcenp")) {
                cmd_forcenp(name, args);
            } else if (cmd.equals("!setcaptain") || cmd.equals("!sc")) {
                cmd_setCaptain(name, args);
            } else if (cmd.equals("!remcaptain") || cmd.equals("!rc")) {
                cmd_removecap(name, args);
            } else if (cmd.equals("!ball")) {
                cmd_ball(name);
            } else if (cmd.equals("!drop")) {
                cmd_drop(name);
            } else if (cmd.equals("!hosttimeout") || cmd.equals("!hto")) {
                cmd_hosttimeout(name);
            } else if (cmd.equals("!setteamname") || cmd.equals("!stn")) {
                cmd_setteamname(name, args);
            }
        }
       
        /* Staff commands ER+ */
        if (ba.getOperatorList().isER(name)) {
            if (cmd.equals("!settimeout")) {
                cmd_settimeout(name, args);
            }
        }

        /* Staff commands Moderator+ */
        if (ba.getOperatorList().isModerator(name)) {
            if (cmd.equals("!die")) {
                ba.die();
            } else if(cmd.equals("!autocap") || cmd.equals("!ac")) {
                cmd_autocap(name);
            }
        }

        /* Staff commands SMOD+ */
        if (ba.getOperatorList().isSmod(name)) {
            if (cmd.equals("!allowzoner")) {
                cmd_allowZoner(name);
            }
        }
    }

    /** 
     * Handles the !add command (cap)
     *
     * @param name Name of the player who issued the command.
     * @param args the player to be added
     * @param override Override number, -1 for default, 0 for Freq 0, 1 for Freq 1
     */
    private void cmd_add(String name, String args) {
        Player p;           //Specified player (in args)
        String pName;       //Specified player's name in normal case
        SBTeam t;           //Team

        /* Check if name is a captain or that the command is overriden */
        if (!isCaptain(name)) {
            return;
        }

        t = getTeam(name);    //Retrieve team

        /* Check if it is the team's turn to pick during ADDING_PLAYERS */
        if (currentState == SBState.ADDING_PLAYERS) {
            if (!t.isTurn()) {
                ba.sendPrivateMessage(name, "Error: Not your turn to pick!");
                return;
            }
        }

        /* Check command syntax */
        if (args.isEmpty()) {
            ba.sendPrivateMessage(name, "Error: Please specify atleast a playername, !add <player>");
            return;
        }

        if (SBState.ACTIVEGAME.contains(currentState)) {

            p = ba.getFuzzyPlayer(args);    //Find <player>

            /* Check if p has been found */
            if (p == null) {
                ba.sendPrivateMessage(name, "Error: " + args + " could not be found.");
                return;
            }

            pName = p.getPlayerName();

            /* Check if p is a bot */
            if (ba.getOperatorList().isBotExact(pName.toLowerCase())) {
                ba.sendPrivateMessage(name, "Error: Pick again, bots are not allowed to play.");
                return;
            }

            /* Check if the player is set to notplaying */
            if (listNotplaying.contains(pName)) {
                ba.sendPrivateMessage(name, "Error: " + p.getPlayerName() + " is set to notplaying.");
                return;
            }

            /* Check if the maximum amount of players IN is already reached */
            if (t.getSizeIN() >= config.getMaxPlayers()) {
                ba.sendPrivateMessage(name, "Error: Maximum amount of players already reached.");
                return;
            }

            /* Check if the player is already on the team and playing */
            if (t.isIN(pName)) {
                ba.sendPrivateMessage(name, "Error: " + p.getPlayerName()
                        + " is already on your team, check with !list");
                return;
            }

            /* Check if the player was already on the other team */
            if (getOtherTeam(name).isOnTeam(pName)) {
                ba.sendPrivateMessage(name, "Error: Player is already on the other team.");
                return;
            }

            /*
             * All checks are done
             */

            /* Add player */
            t.addPlayer(p, config.getDefaultShipType(t.getFrequency()));

            /* Toggle turn */
            if (currentState == SBState.ADDING_PLAYERS) {
                t.picked();
                determineTurn();
            }
        }
    }

    /**
     * Handles the !allowzoner command (sMod+)
     * 
     * @param name name of the player who issued the command.
     */
    private void cmd_allowZoner(String name) {
        zonerTimestamp = zonerTimestamp - (zonerWaitTime * Tools.TimeInMillis.MINUTE);
        manualZonerTimestamp = manualZonerTimestamp - (10 * Tools.TimeInMillis.MINUTE);
        ba.sendPrivateMessage(name, "Zone message timestamps have been reset.");
    }
    
    /**
     * Handles the !autocap command (Mod+)
     * 
     * @param name name of the player who issued the command.
     */
    private void cmd_autocap(String name) {
        config.toggleAllowAutoCaps();
        ba.sendSmartPrivateMessage(name, "AutoCaps is now " + (config.getAllowAutoCaps() ? "en" : "dis") + "abled.");
    }
    /**
     * Handles the !ball command (ZH+)
     * 
     * @param name Name of the player who issued the command.
     */
    private void cmd_ball(String name) {
        int xCoord = ball.getBallX();
        int yCoord = ball.getBallY();
        
        ba.sendPrivateMessage(name, "Ball was located at: " + xCoord
                + ", " + yCoord);
        doGetBall(config.getBallDrop());
    }
    
    /**
     * Handles the !cap command
     *
     * @param name player that issued the !cap command
     */
    private void cmd_cap(String name) {
        SBTeam t;

        /* Check if bot is turned on */
        if (currentState == SBState.OFF) {
            return;
        }

        /* Check if auto captains is allowed */
        if (!config.getAllowAutoCaps()) {
            sendCaptainList(name);
            return;
        }

        /* Check if sender is on the not playing list */
        if (listNotplaying.contains(name)) {
            sendCaptainList(name);
            return;
        }

        /* Check if captain spots are already taken */
        if (team0.hasCaptain() && team1.hasCaptain()) {
            sendCaptainList(name);
            return;
        }

        /*
         * Check if the sender is already on one of the teams
         * If so he can only get captain of his own team
         */
        t = getTeam(name);
        if (t != null) {
            if (t.hasCaptain()) {
                sendCaptainList(name);
                return;
            } else {
                t.setCaptain(name);
                return;
            }
        }

        /* Check if game state is waiting for caps, or adding players */
        if (currentState == SBState.WAITING_FOR_CAPS
                || currentState == SBState.ADDING_PLAYERS) {
            if (!team0.hasCaptain()) {
                team0.setCaptain(name);
                return;
            } else if (!team1.hasCaptain()) {
                team1.setCaptain(name);
                return;
            } else {
                sendCaptainList(name);
                return;
            }
        } else {
            sendCaptainList(name);
            return;
        }
    }
    
    /** 
     * Handles the !drop command (ZH+)
     *
     * @param name Name of the player who issued the command.
     */
    private void cmd_drop(String name) {
        dropBall();
    }
    
    /**
     * Handles the !forcenp command (ZH+)
     * Forces a player to !notplaying
     *
     * @param name name of the player that issued the command
     * @param args name of the player that needs to get forced into !notplaying
     */
    private void cmd_forcenp(String name, String args) {
        Player p;

        if(args.isEmpty()) {
            ba.sendSmartPrivateMessage(name, "Error: Please provide a player's name, '!forcenp <player>'");
            return;
        }
        
        p = ba.getFuzzyPlayer(args);

        if (p == null) {
            ba.sendPrivateMessage(name, args + " could not be found.");
            return;
        }

        if (listNotplaying.contains(p.getPlayerName())) {
            ba.sendPrivateMessage(name, p.getPlayerName() + " is already set to !notplaying.");
            return;
        }

        cmd_notplaying(p.getPlayerName());

        ba.sendPrivateMessage(name, p.getPlayerName() + " has been set to !notplaying.");
    }

    /**
     * Handles the !help command
     * 
     * @param name name of the player
     * @param args The arguments of the full command message
     */
    private void cmd_help(String name, String args) {

        ArrayList<String> help = new ArrayList<String>();   //Help messages

        if (currentState == SBState.OFF) {
            help.add("StrikeBall Help Menu");
            help.add("-----------------------------------------------------------------------");
            help.add("!subscribe                           Toggles alerts in private messages");
            if (ba.getOperatorList().isZH(name)) {
                help.add("!start [<tn1>:<tn2>]       starts the bot, optional with teamnames <tn1> and <tn2>");
            }
            String[] spam = help.toArray(new String[help.size()]);
            ba.privateMessageSpam(name, spam);
         } else {
             if (!args.contains("cap") && !args.contains("staff")){
            help.add("StrikeBall Help Menu");
            help.add("-----------------------------------------------------------------------");
            help.add("!notplaying                       Toggles not playing mode  (short !np)");
            help.add("!cap                                            shows current captains!");
            help.add("!lagout              Puts you back into the game if you have lagged out");
            help.add("!list                                    Lists all players on this team");
            help.add("!status                                        Display status and score");
            help.add("!subscribe                           Toggles alerts in private messages");
            help.add("-----------------------------------------------------------------------");
            help.add("For more help: Private Mesage Me !help <topic>           ex. !help cap ");
            help.add("                                                                       ");
            help.add("Topics            Cap (Captain commands for before and during the game)");
                                  
             if (ba.getOperatorList().isZH(name))
                 help.add("              Staff (The staff commands for before and during the game)");
             }
                
                String[] spam = help.toArray(new String[help.size()]);
                ba.privateMessageSpam(name, spam);
            
            if (args.contains("cap")) {
                
                 ArrayList<String> hCap = new ArrayList<String>();
                 
                 hCap.add("StrikeBall Help Menu: Captain Controls");
                 hCap.add("-----------------------------------------------------------------------");
                 hCap.add("!add <player>                        Adds player (Default Ship: Spider)");
                 hCap.add("!add <player>:<ship>                  Adds player in the specified ship");
                 hCap.add("!remove <player>                              Removes specified player)");
                 hCap.add("!change <player>:<ship>           Sets the player in the specified ship");
                 hCap.add("!sub <playerA>:<playerB>           Substitutes <playerA> with <playerB>");
                 hCap.add("!switch <player>:<player>            Exchanges the ship of both players");
                 if(config.getAllowAutoCaps())
                     hCap.add("!timeout                       During faceoff, request a 30 sec timeout");
                 hCap.add("!ready                    Use this when you're done setting your lineup");
                 hCap.add("-----------------------------------------------------------------------");
                 
                String[] spamCap = hCap.toArray(new String[hCap.size()]);
                ba.privateMessageSpam(name, spamCap);
                
            }
            if (args.contains("staff")) {
                if (ba.getOperatorList().isZH(name)) {
                    
                    ArrayList<String> hStaff = new ArrayList<String>();
                    
                    hStaff.add("StrikeBall Help Menu: Staff Controls");
                    hStaff.add("----------------------------------------------------------------------------------");
                    hStaff.add("!start [<tn1>:<tn2>]       starts the bot, optional with teamnames <tn1> and <tn2>");
                    hStaff.add("!stop                                                                stops the bot");
                    hStaff.add("!ball                                                           retrieves the ball");
                    hStaff.add("!drop                                                               drops the ball");
                    hStaff.add("!zone <message>                  sends time-restricted advert, message is optional");
                    hStaff.add("!forcenp <player>                                     Sets <player> to !notplaying");
                    hStaff.add("!setcaptain <# freq>:<player>   Sets <player> as captain for <# freq> (short: !sc)");
                    hStaff.add("!remcaptain <# freq>             Removes the captain of freq <# freq> (short: !rc)");
                    hStaff.add("!setteamname <# freq>:<name>          Sets team's name for <# freq>. (short: !stn)");
                    hStaff.add("!hosttimeout                             Request a 30 second timeout (short: !hto)");
                    if (ba.getOperatorList().isER(name)) {
                        hStaff.add("!settimeout <amount>                Sets captain timeouts to <amount> (default: 1)");
                    }
                    if (ba.getOperatorList().isModerator(name)) {
                        hStaff.add("!autocap                            Enables the auto captain feature. (short: !ac)");
                        hStaff.add("!off                                          stops the bot after the current game");
                        hStaff.add("!die                                                           disconnects the bot");
                    }
                    if (ba.getOperatorList().isSmod(name)) {
                        hStaff.add("!allowzoner                         Forces the zone timers to reset allowing !zone");
                                      
                    }
                    String[] spamStaff = hStaff.toArray(new String[hStaff.size()]);
                    ba.privateMessageSpam(name, spamStaff);
                }
            }
         }

    }
    
    /**
     * Handles the !hosttimeout command. (ZH+)
     * 
     * @param name name of the host.
     */
    private void cmd_hosttimeout(String name) {
        // Completely ignore the command if the bot is off.
        if(currentState == SBState.OFF)
            return;
        
        // If the host requests a timeout, check if the current phase allows it.   
        if(!(SBState.MIDGAME.contains(currentState))) {
            ba.sendPrivateMessage(name, "This is currently only available during a FaceOff.");
        } else {
            // Send a nice message ...
            ba.sendArenaMessage(name + 
                    " has issued a 30-second timeout.", Tools.Sound.BEEP1);
            // ... and start the timeout
            startTimeout();
        }
    }
    
    /**
     * Handles the !lagout/!return command
     *
     * @param name name of the player that issued the !lagout command
     */
    private void cmd_lagout(String name) {
        if (SBState.ACTIVEGAME.contains(currentState)) {
            SBTeam t;

            t = getTeam(name);

            /* Check if player was on a team */
            if (t == null) {
                return;
            }

            /* Check if the player was at least IN, or LAGGED OUT */
            if (!t.laggedOut(name)) {
                return;
            }

            /* Check if a return is possible */
            if (!t.laginAllowed(name)) {
                ba.sendPrivateMessage(name, t.getLaginErrorMessage(name)); //Send error message
                return;
            }

            t.lagin(name); //Puts the player in again
        }
    }

    /**
     * Handles !list command
     *
     * @param name player that issued the !list command
     */
    private void cmd_list(String name) {
        SBTeam t;

        if (currentState != SBState.OFF && currentState != SBState.WAITING_FOR_CAPS
                && currentState != SBState.GAME_OVER) {
            t = getTeam(name);   //Retrieve teamnumber

            /* Check if the player is a staff member (In order to show the list of both teams to the staff member) */
            if (ba.getOperatorList().isER(name)) {
                t = null;
            }

            /* Display set up */
            ArrayList<String> list = new ArrayList<String>();
            if (t == null) {
                /* Display both teams */
                list.addAll(listTeam(0));
                list.add("`");
                list.addAll(listTeam(1));
            } else {
                /* Display one team */
                list = listTeam(t.getFrequency());
            }

            String[] spam = list.toArray(new String[list.size()]);
            ba.privateMessageSpam(name, spam);
        }
    }

    /**
     * Creates a sorted list of players for the !list command.
     * @param frequency Frequency of the team to create the list of.
     * @return A textual representation of this team, ready to be spammed in PM.
     */
    private ArrayList<String> listTeam(int frequency) {
        /* Set up sorting */
        Comparator<SBPlayer> comparator = new Comparator<SBPlayer>() {

            @Override
            public int compare(SBPlayer pa, SBPlayer pb) {
                if (pa.getCurrentState() < pb.getCurrentState()) {
                    return -1;
                } else if (pa.getCurrentState() > pb.getCurrentState()) {
                    return 1;
                } else if (pa.getCurrentShipType() < pb.getCurrentShipType()) {
                    return -1;
                } else if (pa.getCurrentShipType() > pb.getCurrentShipType()) {
                    return 1;
                } else if (pb.getName().compareTo(pa.getName()) < 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
        ArrayList<String> list = new ArrayList<String>();
        SBTeam t = frequency == 0 ? team0 : team1;
        /* Display one team */
        list.add(t.getName() + " (captain: " + t.getCaptainName() + ")");
        list.add(Tools.formatString("Name:", 23) + " - "
                + Tools.formatString("Ship:", 10) + " - " + "Status:");

        SBPlayer[] players = t.players.values().toArray(
                new SBPlayer[t.players.values().size()]);
        Arrays.sort(players, comparator);

        for (SBPlayer p : players) {
            list.add(Tools.formatString(p.p_name, 23) + " - "
                    + Tools.formatString(Tools.shipName(p.getCurrentShipType()), 10) + " - " + p.getStatus());
        }
        return list;
    }

    /**
     * Handles the !notplaying command
     *
     * @param name name of the player that issued the !notplaying command
     */
    private void cmd_notplaying(String name) {
        SBTeam t;

        if (currentState != SBState.OFF) {
            t = getTeam(name);

            /* Check if player is on the notplaying list and if so remove him from that list */
            if (listNotplaying.contains(name)) {
                listNotplaying.remove(name);  //Remove from him from the notplaying list
                ba.sendPrivateMessage(name,
                        "You have been removed from the not playing list.");   //Notify the player
                /* Put the player on the spectator frequency */
                ba.setShip(name, 1);
                ba.specWithoutLock(name);
                return;
            }

            /* Add the player to the notplaying list */
            listNotplaying.add(name); //Add the player to the notplaying list
            ba.sendPrivateMessage(name, "You have been added to the not playing list. "
                    + "(Captains will be unable to add or sub you in.)"); //Notify the player
            ba.specWithoutLock(name);  //Spectate the player
            ba.setFreq(name, FREQ_NOTPLAYING);  //Set the player to the notplaying frequency

            /* Check if the player was on one of the teams */
            if (t != null) {
                /* Check if the player was a captain */
                if (isCaptain(name)) {
                    t.captainLeft();   //Remove the player as captain
                }

                if (currentState == SBState.ADDING_PLAYERS) {
                    if (t.isOnTeam(name)) {
                        ba.sendArenaMessage(name + " has been removed from the game. (not playing)");
                        t.removePlayer(name);
                    }
                }

                /* Check if a player was in and set him to "out but subable" status */
                if (currentState != SBState.ADDING_PLAYERS
                        && currentState != SBState.GAME_OVER) {
                    if (t.isOnTeam(name)) {
                        if (t.isPlaying(name) || t.laggedOut(name)) {
                            ba.sendArenaMessage(
                                    name + " has been removed from the game. (not playing)"); //Notify the player
                            t.setOutNotPlaying(name); //Set player to out, but subable status
                        }
                    }
                }

                ba.setFreq(name, FREQ_NOTPLAYING);     //Set the player to the notplaying frequency
            }
        }
    }

    /**
     * Handles the !off command (Mod+)
     *
     * @param name name of the player that issued the !off command
     */
    private void cmd_off(String name) {
        switch (currentState) {
            case OFF:
                ba.sendPrivateMessage(name, "Bot is already OFF");
                break;
            case WAITING_FOR_CAPS:
                cmd_stop(name);
                break;
            default:
                ba.sendPrivateMessage(name, "Turning OFF after this game");
                lockLastGame = true;
        }
    }
    
    /**
     * Handles the !ready command (cap)
     *
     * @param name name of the player that issued the command
     */
    private void cmd_ready(String name) {
        SBTeam t;

        if (currentState == SBState.ADDING_PLAYERS) {
            t = getTeam(name); //Retrieve teamnumber

            t.ready();   //Ready team

            /* Check if both teams are ready */
            if (team0.isReady() && team1.isReady()) {
                checkLineup(false); //Check lineups
            }
        }
    }

    /**
     * Handles the !remove command (cap)
     *
     * @param name name of the player that issued the !remove command
     * @param args command parameters
     */
    private void cmd_remove(String name, String args) {
        SBTeam t;
        SBPlayer p;   //Player to be removed

        if (SBState.ACTIVEGAME.contains(currentState)) {
            t = getTeam(name); //Retrieve team

            /* Check command syntax */
            if (args.isEmpty()) {
                ba.sendPrivateMessage(name, "Error: Please specify a player, '!remove <player>'");
                return;
            }

            /* Search for the to be removed player */
            // First try an exact match.
            p = t.searchPlayer(args);
            
            // If it fails, try a fuzzy match.
            if (p == null)
                p = t.searchPlayer(ba.getFuzzyPlayerName(args));

            /* Check if player has been found */
            if (p == null) {
                ba.sendPrivateMessage(name, "Error: Player could not be found");
                return;
            }

            t.removePlayer(p.getName());

            if (currentState == SBState.ADDING_PLAYERS)
                determineTurn();
        }
    }

    /**
     * Handles the !remcaptain command (ZH+)
     *
     * @param name name of the player that issued the !removecap command
     * @param args Frequency of the captain to be removed.
     */
    private void cmd_removecap(String name, String args) {
        SBTeam t;
        Integer freq = null;

        // Initial checks
        if (currentState == SBState.OFF || currentState == SBState.GAME_OVER) {
            // Ignore, no game in progress.
            return;
        }
        
        if(args.isEmpty()) {
            // No valid arguments sent.
            ba.sendSmartPrivateMessage(name, 
                    "Please specify the frequency of the captain you want to remove. '!remcaptain <#freq>'");
            return;
        }
        
        // Check if we received a correct frequency.
        try {
            freq = Integer.parseInt(args);
        } catch (NumberFormatException e) {
            // If no valid number has been provided, then this will be taken care of in the next if statement, which combines some stuff.
        }
        
        // If the previous catch triggered or an invalid freq has been given, this if statement will be valid.
        if(freq == null || freq < 0 || freq > 1) {
            // No valid arguments sent.
            ba.sendSmartPrivateMessage(name, 
                    "Please specify the frequency of the captain you want to remove. '!remcaptain <#freq>'");
            return;
        }
        
        t = teams.get(freq);
        
        if(t != null && t.hasCaptain()) {
            t.captainLeft();   //Remove captain, will auto-sent a message.
        } else {
            // There was no captain on this team.
            ba.sendSmartPrivateMessage(name, "Freq " + freq + " does not have a captain.");
        }
        
        return;
    }
    
    /**
     * Handles the !setcaptain command (ZH+)
     *
     * @param name name of the player that issued the !setcaptain command
     * @param args command parameters
     */
    private void cmd_setCaptain(String name, String args) {
        int frequency;
        Player p;
        String[] splitCmd;

        if (currentState != SBState.OFF && currentState != SBState.GAME_OVER) {

            /* Check command syntax */
            if (args.isEmpty()) {
                ba.sendPrivateMessage(name, 
                        "Error: please specify a player and frequency, '!setcaptain <# freq>:<player>'");
                return;
            }

            splitCmd = args.split(":"); //Split parameters

            /* Check command syntax */
            if (splitCmd.length < 2) {
                ba.sendPrivateMessage(name, 
                        "Error: please specify a player, '!setcaptain <# freq>:<player>'");
                return;
            }

            p = ba.getFuzzyPlayer(splitCmd[1]); //Search player

            /* Check if player has been found */
            if (p == null) {
                ba.sendPrivateMessage(name, "Error: Unknown player");
                return;
            }

            /* Retrieve teamnumber or frequency number */
            try {
                frequency = Integer.parseInt(splitCmd[0]);
            } catch (Exception e) {
                ba.sendPrivateMessage(name, 
                        "Error: please specify a correct frequency, '!setcaptain <# freq>:<player>'");
                return;
            }

            /* Check if frequency is valid */
            if (frequency == 0) {
                team0.setCaptain(p.getPlayerName()); //Set player to captain
            } else if (frequency == 1) {
                team1.setCaptain(p.getPlayerName());
            } else {
                ba.sendPrivateMessage(name, 
                        "Error: please specify a correct frequency, '!setcaptain <# freq>:<player>'");
            }

        }
    }
    
    /**
     * Handles the !setteamname command. (ZH+)
     * <p>
     * This command allows the host to change a team's name.
     * 
     * @param name Name of the player who issued the commmand.
     * @param args Command parameters.
     */
    private void cmd_setteamname(String name, String args) {
        SBTeam t;
        String splitArgs[];
        int freq = -1;
        String teamName = "";
        
        if(!SBState.ACTIVEGAME.contains(currentState)) {
            // Not currently in a correct state.
            return;
        }
        
        if (args.isEmpty()) {
            // Invalid command syntax.
            ba.sendSmartPrivateMessage(name, 
                    "Error: please specify a frequency and team name, '!setteamname <freq>:<name>'");
            return;
        }
        
        splitArgs = args.split(":", 2);
        
        if(splitArgs.length == 2) {
            try {
                freq = Integer.parseInt(splitArgs[0]);
                teamName = splitArgs[1];
            } catch (NumberFormatException e) {
                // Do nothing, situation will be handled down the line.
            }
        }
        
        if(freq == -1 || teamName.isEmpty()) {
            // Invalid command syntax.
            ba.sendSmartPrivateMessage(name, 
                    "Error: please specify a frequency team name, '!setteamname <freq>:<name>'");
            return;           
        }
        
        if(freq != 0 && freq != 1) {
            // Invalid frequency number.
            ba.sendSmartPrivateMessage(name, 
                    "Error: please specify a correct frequency, '!setteamname <freq>:<name>'");
            return;
        }
        
        t = teams.get(freq);
        
        if(t == null) {
            // Shouldn't happen, since a captain's check was done before this function could be called, however, better safe than sorry.
            ba.sendSmartPrivateMessage(name, "Seems that the team you specified doesn't exist.");
            return;
        }
        
        if(t.getName().equals(teamName)) {
            // Team already has this name.
            ba.sendSmartPrivateMessage(name, "That team already has that name.");
            return;
        }
        
        t.setName(teamName);
        
        ba.sendArenaMessage(name + " has changed the team name of Freq "+ freq + " to: " + teamName);
    }

    /**
     * Handles the !settimeout command. (ER+)
     * Intended to be used to disable the system in case of abuse by the captains.
     * 
     * @param name Name of the player that issued the command.
     * @param args The arguments of the issued command.
     */
    private void cmd_settimeout(String name, String args) {
        int value;
        
        if(currentState == SBState.OFF)
            return;
        
        if (!(currentState == SBState.GAME_OVER
                || currentState == SBState.WAITING_FOR_CAPS
                || currentState == SBState.ADDING_PLAYERS)) {
            // Only allowed to change the setting when no game is in progress.
            ba.sendPrivateMessage(name, "Changing the timeout " +
                "setting is not allowed at this stage of the game.");
            return;
        }
        try {
            value = Integer.parseInt(args);
        } catch (Exception e) {
         // No argument or an invalid argument given.
            ba.sendPrivateMessage(name, "Please provide a valid number. " +
                    "(Usage: !settimeout <number>)");
            return;
        }
        
        // If value is less than 0, set maxTimeouts to 0. Otherwise the value provided.
        maxTimeouts = (value < 0)?0:value;
        
        ba.sendPrivateMessage(name, "Maximum timeouts set to " + maxTimeouts + ".");
            
    }
    
    /**
     * Handles the !start command (ZH+)
     *
     * @param name player that issued the !start command
     * @param args Optional: [Name_team1:Name_team2]
     */
    private void cmd_start(String name, String args) {
        if (currentState == SBState.OFF) {
            start();
            if(!args.isEmpty()) {
                String teamNames[] = args.split(":",2);
                if(teamNames.length == 2) {
                    team0.setName(teamNames[0]);
                    team1.setName(teamNames[1]);
                }
            }
        } else {
            ba.sendPrivateMessage(name, "Error: Bot is already ON");
        }
    }

    /**
     * Handles the !status command
     *
     * @param name name of the player that issued the command
     */
    private void cmd_status(String name) {
        String[] status;    //Status message

        status = new String[2];
        status[0] = ""; //Default value
        status[1] = ""; //Default value

        switch (currentState) {
            case OFF:
                status[0] = "Bot turned off, no games can be started at this moment.";
                break;
            case WAITING_FOR_CAPS:
                if (config.getAllowAutoCaps()) {
                    status[0] = "A new game will start when two people message me with !cap";
                } else {
                    status[0] = "Request a new game with '?help start strikeball please'";
                }
                break;
            case ADDING_PLAYERS:
                status[0] = "Teams: " + team0.getName() + " vs. " + team1.getName()
                        + ". We are currently arranging lineups";
                break;
            case FACE_OFF:
                status[0] = "Teams: " + team0.getName() + " vs. " + team1.getName()
                        + ". We are currently facing off";
                break;
            case TIMEOUT:
                status[0] = "Teams: " + team0.getName() + " vs. " + team1.getName()
                        + ". We are currently in a timeout";
                break;
            case GAME_IN_PROGRESS:
                status[0] = "Game is in progress.";
                status[1] = "Score " + team0.getName() + " vs. " + team1.getName() + ": " + score();
                break;
            case GAME_OVER:
                status[0] = "Teams: " + team0.getName() + " vs. " + team1.getName()
                        + ". We are currently ending the game";
                break;
        }

        /* Send status message */
        if (!status[0].isEmpty()) {
            ba.sendPrivateMessage(name, status[0]);
        }

        if (!status[1].isEmpty()) {
            ba.sendPrivateMessage(name, status[1]);
        }
    }

    /**
     * Handles the !stop command (ZH+)
     *
     * @param name player that issued the !stop command
     */
    private void cmd_stop(String name) {
        if (currentState != SBState.OFF) {
            ba.sendArenaMessage("Bot has been turned OFF");
            currentState = SBState.OFF;
            reset();
            unlockArena();
        } else {
            ba.sendPrivateMessage(name, "Error: Bot is already OFF");
        }
    }

    /**
     * Handles the !sub command (cap)
     *
     * @param name name of the player that issued the !sub command
     * @param args command parameters
     */
    private void cmd_sub(String name, String args) {
        SBTeam t;
        String[] splitCmd;
        SBPlayer playerA;
        SBPlayer playerB;
        Player playerBnew;

        if (SBState.ACTIVEGAME.contains(currentState)) {
            t = getTeam(name); //Retrieve teamnumber

            if (t == null) {
                return;
            }

            /* Check command syntax */
            if (args.isEmpty()) {
                ba.sendPrivateMessage(name, "Error: Specify players, !sub <playerA>:<playerB>");
                return;
            }

            splitCmd = args.split(":");

            /* Check command syntax */
            if (splitCmd.length < 2) {
                ba.sendPrivateMessage(name, "Error: Specify players, !sub <playerA>:<playerB>");
                return;
            }

            /* Check if team has any substitutes left */
            if (!t.hasSubtitutesLeft()) {
                ba.sendPrivateMessage(name, "Error: You have 0 substitutes left.");
                return;
            }

            /* Try to get an exact match first. */
            playerA = t.searchPlayer(splitCmd[0]);
            
            // If an exact match fails, try a fuzzy match.
            if(playerA == null)
                playerA = t.searchPlayer(ba.getFuzzyPlayerName(splitCmd[0]));   //Search for <playerA>
            
            // Player B must be in spec to be able to be subbed, so no need to go for an exact match first.
            playerBnew = ba.getFuzzyPlayer(splitCmd[1]);   //Search for <playerB>

            /* Check if players can be found */
            if (playerA == null || playerBnew == null) {
                ba.sendPrivateMessage(name, "Error: Player could not be found");
                return;
            }

            /* Check if sub is a bot */
            if (ba.getOperatorList().isBotExact(playerBnew.getPlayerName())) {
                ba.sendPrivateMessage(name, "Error: Bots are not allowed to play.");
                return;
            }

            /* Check if <playerA> is already out and thus cannot be subbed */
            if (playerA.p_state > SBPlayer.OUT_SUBABLE) {
                ba.sendPrivateMessage(name, "Error: Cannot substitute a player that is already out");
                return;
            }

            /* Check if <playerB> is on the notplaying list */
            if (listNotplaying.contains(playerBnew.getPlayerName())) {
                ba.sendPrivateMessage(name,
                        "Error: " + playerBnew.getPlayerName() + " is set to not playing.");
                return;
            }

            /* Check if <playerB> is already on the other team */
            if (getOtherTeam(t).isOnTeam(playerBnew.getPlayerName())) {
                ba.sendPrivateMessage(name, "Error: Substitute is already on the other team");
                return;
            }

            /* Check if <playerB> was already on the team */
            playerB = t.searchPlayer(playerBnew.getPlayerName());
            if (playerB != null) {
                /* Check when last !sub was and if this sub is allowed */
                if (!playerB.isSubAllowed()) {
                    ba.sendPrivateMessage(name, "Error: Sub not allowed yet for this player, wait "
                            + playerB.getTimeUntilNextSub() + " more seconds before next !sub");
                    return;
                }
            }

            t.sub(playerA, playerBnew); //Execute the substitute
        }
    }
    
    /**
     * Handles the !subscribe command
     *
     * @param name player that issued the !subscribe command
     */
    private void cmd_subscribe(String name) {
        if (currentState != SBState.OFF) {
            if (listAlert.contains(name)) {
                listAlert.remove(name);
                ba.sendPrivateMessage(name, "You have been removed from the alert list.");
            } else {
                listAlert.add(name);
                ba.sendPrivateMessage(name, "You have been added to the alert list.");
            }
        }
    }

    /**
     * Handles the !timeout command (cap)
     * <p>
     * Will only work when automatic caps is enabled.
     * 
     * @param name name of the player that issued the command.
     */
    private void cmd_timeout(String name) {
        // If a captain requests a timeout, get his team's info.
        SBTeam t = getTeam(name);
        
        // Check if the request is valid
        if(!(SBState.ACTIVEGAME.contains(currentState))) {
            ba.sendPrivateMessage(name, "You can only request a timeout during the FaceOff.");
        } else if(t.timeout == 0) {
            // Checks if the captain has any timeouts left to use
            ba.sendPrivateMessage(name, "You have already used your timeout" + 
                    ((maxTimeouts > 1)?"s":"") + ".");
        } else {
            // Good to go. Lower the amount of available timeouts ...
            t.useTimeOut();
            // .. send a nice message ...
            ba.sendArenaMessage(name + 
                    " has requested a 30-second timeout for team: " +
                    t.getName()+ ".", Tools.Sound.CROWD_GEE);
            // ... and start the timeout.
            startTimeout();
        }
    }
    
    /**
     * Handles the !zone command (ZH+)
     *
     * @param name name of the player that issued the command
     * @param args message to use for zoner
     */
    private void cmd_zone(String name, String args) {
        if (!allowManualZoner()) {
            ba.sendPrivateMessage(name, "Zoner not allowed yet.");
            return;
        }

        if (!(currentState == SBState.GAME_OVER
                || currentState == SBState.WAITING_FOR_CAPS
                || currentState == SBState.ADDING_PLAYERS)) {
            ba.sendPrivateMessage(name, "Zoner not allowed at this stage of the game.");
            return;
        }

        //args can go through regardless if it has a valid value. This is taken care of in the newGameAlert function.
        newGameAlert(name, args);
    }

    /*
     * Game modes
     */
    /**
     * Starts the bot
     */
    private void start() {
        ba.setMessageLimit(8, false);
        ba.receiveAllPlayerDeaths();
        ba.setLowPriorityPacketCap(8);
        lockLastGame = false;
        lockDoors();
        setSpecAndFreq();

        try {
            gameticker.cancel();
        } catch (Exception e) {
        }

        gameticker = new Gameticker();
        ba.scheduleTask(gameticker, Tools.TimeInMillis.SECOND, Tools.TimeInMillis.SECOND);

        startWaitingForCaps();
    }
  
    /**
     * Starts waiting for caps
     * 
     * @see Gameticker#doWaitingForCaps()
     */
    private void startWaitingForCaps() {
        // To avoid any racing conditions, set the current state to WAIT.
        // This prevents the bot from accidentally doing stuff that influences the commands here.
        currentState = SBState.WAIT;
        reset();

        if (config.getAllowAutoCaps()) {
            ba.sendArenaMessage("A new game will start when two people message me with !cap -"
                    + ba.getBotName(), Tools.Sound.BEEP2);
        } else {
            ba.sendArenaMessage("Request a new game with '?help start strikeball please'"
                    + " -" + ba.getBotName(), Tools.Sound.BEEP2);
        }
        currentState = SBState.WAITING_FOR_CAPS;
    }

    /**
     * Start adding players state.
     * <ul> 
     *  <li>Notify arena
     *  <li>Notify chats
     *  <li>Determine next pick
     * </ul>
     * 
     * @see Gameticker#doAddingPlayers()
     */
    private void startAddingPlayers() {
        // To avoid any racing conditions, set the current state to WAIT.
        // This prevents the bot from accidentally doing stuff that influences the commands here.
        currentState = SBState.WAIT;
               
        lockArena();
        ba.specAll();
        timeStamp = System.currentTimeMillis();
        ba.sendArenaMessage("Captains you have 10 minutes to set up your lineup correctly!",
                Tools.Sound.BEEP2);
        
        roundTime = 10 * 60;
        
        scoreOverlay.updateNames();
        scoreOverlay.updateTime(roundTime);

        if (config.getAllowAutoCaps()) {
            newGameAlert(null, null);
        }

        if (team0.hasCaptain()) {
            team0.putCaptainInList();
        }

        if (team1.hasCaptain()) {
            team1.putCaptainInList();
        }
        
        currentState = SBState.ADDING_PLAYERS;
        determineTurn();
    }

    /**
     * Starts pre game
     * 
     * @see Gameticker#doFaceOff()
     */
    private void startFaceOff() {
        // To avoid any racing conditions, set the current state to WAIT.
        // This prevents the bot from accidentally doing stuff that influences the commands here.
        currentState = SBState.WAIT;

        scoreOverlay.updateAll(gameTime);

        ball.clear();

        ba.sendArenaMessage("Prepare For FaceOff", Tools.Sound.CROWD_OOO);

        timeStamp = System.currentTimeMillis();
        ball.dropDelay = (int) (Math.random() * 9 + 15);
        ball.holding = false;
        doGetBall(config.getBallDrop());
        
        currentState = SBState.FACE_OFF;
    }

    /**
     * Starts the automated review period after a goal has been made.
     * <p>
     * This function will check if a violation has occured, or if the goal was an own goal, or if it was a clean goal.
     * Furthermore will it assign the goal to a player and increase the scorecount of the scoring team.
     * <p>
     * When this is a non-timed game, on the final goal, a manual review will be done after the default checks, through {@link #startFinalReview()}.
     * @param event The original SoccerGoal event.
     */
    private void startReview(SoccerGoal event) {
        
        int freq = event.getFrequency();
        
        // Check if the goal was clean
        if (freq == 0 || freq == 1) {
            // Increase the score.
            teams.get(freq).increaseScore();
            // Award point to the scorer and to anyone who has assisted with this goal.
            addPlayerGoalWithAssist();
        }

        // Check if the game is finished
        if (Math.abs(team0.getScore() - team1.getScore()) >= config.getScoreDifference()
                && (team0.getScore() >= config.getScoreTarget() 
                    || team1.getScore() >= config.getScoreTarget())) {
            gameOver();
        } else {
            for(int i = 0; i <= 1; i++)
                ba.warpFreqToLocation(teams.get(i).getFrequency(), config.getTeamEntryPoint(i).x, config.getTeamEntryPoint(i).y);
        }
    }
    
    /**
     * Starts a game
     * 
     * @see Gameticker#doStartGame()
     */
    private void startGame() {
        // To avoid any racing conditions, set the current state to WAIT.
        // This prevents the bot from accidentally doing stuff that influences the commands here.
        currentState = SBState.WAIT;

        timeStamp = System.currentTimeMillis();
        ba.sendArenaMessage("Go Go Go !!!", Tools.Sound.VICTORY_BELL);
        
        currentState = SBState.GAME_IN_PROGRESS;
    }
    
    /**
     * Initiates the timeout state.
     * 
     * @see Gameticker#doTimeout()
     */
    private void startTimeout() {
        // To avoid any racing conditions, set the current state to WAIT.
        // This prevents the bot from accidentally doing stuff that influences the commands here.
        currentState = SBState.WAIT;
        
        timeStamp = System.currentTimeMillis();
        doRemoveBall();
        
        // When looking at doRemoveBall(), this code seems to be redundant.
        // However, due to the bot not always having the latest ball positions, this 
        // safeguard is needed to make it function properly, for now.
        ba.getShip().move(config.getBallTimeOut().x, config.getBallTimeOut().y);
        ba.getBall(ball.getBallID(), ball.getTimeStamp());
        dropBall();

        currentState = SBState.TIMEOUT;
    }
    
    /**
     * What to do with when game is over.
     * <p>
     * This starts several timers to display results and whatnot.
     * <ul>
     *  <li>After 2 seconds:
     *  <ul>
     *      <li>Display "GAME OVER" message.
     *      <li>Display the final score.
     *  </ul>
     *  <li>After 5 seconds:
     *  <ul>
     *      <li>Display the MVP.
     *  </ul>
     *  <li>After 10 seconds:
     *  <ul>
     *      <li>Specs everyone.
     *      <li>Restarts the game at adding captains or stops the bot.
     *  </ul>
     * </ul>
     * 
     * @see Gameticker#doGameOver()
     */
    private void gameOver() {
        // To avoid any racing conditions, set the current state to WAIT.
        // This prevents the bot from accidentally doing stuff that influences the commands here.
        currentState = SBState.WAIT;
        
        scoreOverlay.clearAllObjects();
        scoreOverlay.resetVariables();

        //Cancel timer
        ba.setTimer(0);

        statsDelay = new TimerTask() {
            @Override
            public void run() {
                ba.sendArenaMessage("------------ GAME OVER ------------");
                ba.sendArenaMessage("Result of " + team0.getName() + " vs. "
                        + team1.getName(), Tools.Sound.HALLELUJAH);
                dispResults();
            }
        }; ba.scheduleTask(statsDelay, Tools.TimeInMillis.SECOND * 2);

        mvpDelay = new TimerTask() {
            @Override
            public void run() {
                ba.sendArenaMessage("MVP: " + getMVP() + "!", Tools.Sound.INCONCEIVABLE);
            }
        }; ba.scheduleTask(mvpDelay, Tools.TimeInMillis.SECOND * 5);
        
        timeStamp = System.currentTimeMillis();
        
        currentState = SBState.GAME_OVER;
        
    }
    
    /**
     * Display the statistics of the game
     */
    private void dispResults() {
        ArrayList<String> spam = new ArrayList<String>();
        for(SBTeam t : teams) {
            spam.add("+----------------------+-------+---------+-------+--------+-----------+--------+");
            spam.add("| " + Tools.centerString(t.getName(), 20)
                                         + " | Goals | Assists | Kills | Deaths | K/D-Ratio | Rating |");
            spam.add("+----------------------+-------+---------+-------+--------+-----------+--------+");
            ////////("012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901");
            ////////("0         1         2         3         4         5         6         7         8         9         10");
            
            spam.addAll(addTeamStats(t));
        }
        
        spam.add("+----------------------+-------+---------+-------+--------+-----------+--------+");

        ba.arenaMessageSpam(spam.toArray(new String[spam.size()]));
    }
    
    /**
     * Looks up all the statistics of the players of a team and neatly formats them into a list.
     * 
     * @param team SBTeam for which to look up the statistics.
     * @return ArrayList<String> of the statistics. 
     */
    private ArrayList<String> addTeamStats(SBTeam team) {
        ArrayList<String> stats = new ArrayList<String>();
        for (SBPlayer p : team.players.values()) {
            // Short piece of code in case the K/D ratio is well, a division by zero.
            String kdRatio = "";
            Float f = p.getKDRatio();
            if(f == null)
                kdRatio = "Infinite";
            else
                kdRatio = Float.toString(f);
            
            stats.add("| " + Tools.formatString(p.getName(), 20)
                    + " |" + Tools.rightString(Integer.toString(p.getGoals()), 6)
                    + " |" + Tools.rightString(Integer.toString(p.getAssists()), 8)
                    + " |" + Tools.rightString(Integer.toString(p.getKills()), 6)
                    + " |" + Tools.rightString(Integer.toString(p.getDeaths()), 7)
                    + " |" + Tools.rightString(kdRatio, 10)
                    + " |" + Tools.rightString(Integer.toString(p.getTotalRating()), 7)
                    + " |");
        }
        return stats;
    }

    /**
     * Handles the assigning of stats when a player scored a goal, including any assists.
     * <p>
     * This function should only be called upon valid goals. When adding stats for an own goal, please use {@link #addOwnGoal()}.
     */
    private void addPlayerGoalWithAssist() {
        try {
            // Get the player who scored
            String scorer = ball.getLastCarrierName();
            SBTeam t = getTeam(scorer);
            
            if(t != null) {
                // Add a point to the team's score.
                t.getPlayer(scorer).madeGoal();
                String assister = ball.getLastCarrierName();
                // Check if there was a valid assister.
                if (t.isOnTeam(assister)) {
                    // Add the stat to the assister.
                    t.getPlayer(assister).madeAssist();
                }
            }
        } catch (Exception e) {
            // This will most likely trigger if getLastCarrierName gives a NULL.
            // This should never happen on the scorer himself, but can happen in rare cases with looking up the assister.
            // If it happens then, then there was simply no assister, and there is nothing else we need to do.
        }

    }

    /*
     * Tools
     */
    /**
     * Determines the MVP of the match
     * 
     * @return name of the MVP
     */
    private String getMVP() {
        String mvp = "";
        int highestRating = 0;

        // Go through each team ...
        for (SBTeam t : teams) {
            // ... and every player ..
            for (SBPlayer p : t.players.values()) {
                // ... and keep the one with the highest rating.
                if (highestRating < p.getTotalRating()) {
                    highestRating = p.getTotalRating();
                    mvp = p.getName();
                }
            }
        }
        
        return mvp;
    }

    /**
     * Check if there are enough captains to start the game
     */
    private void checkIfEnoughCaps() {
        if (currentState == SBState.WAITING_FOR_CAPS) {
            if (team0.hasCaptain() && team1.hasCaptain()) {
                startAddingPlayers();
            }
        }
    }

    /**
     * Alerts players that a new game is starting
     * <ul>
     *  <li>Send alert to chats;
     *  <li>Send alert to subscribers;
     *  <li>Send alert to zone.
     * </ul>
     * 
     * @param name Name of the person who's issuing the game alert.
     * @param message The custom message to be used, if any.
     */
    private void newGameAlert(String name, String message) {

        String nameTag = " -" + ba.getBotName();

        //Build generic message if a custom one isn't passed to this function.
        if (message == null || message.isEmpty()) {
            message = "A game of strikeball is starting! Type ?go strikeball to play.";
        } else if (message.toLowerCase().contains("?go")) {
            // Don't need to double up on the ?go's.
            ba.sendPrivateMessage(name, "Please do not include ?go strikeball in the zoner as I will add this for you automatically.");
            return;
        } else if (message.toLowerCase().contains("-" + name.toLowerCase())) {
            // Don't need to double up on the names.
            ba.sendPrivateMessage(name, "Please do not include your name in the zoner as I will provide mine automatically.");
            return;
        } else {
            // Add the ?go part if a valid custom message is provided.
            message += " ?go " + ba.getArenaName();
        }

        //Alert Chats
        for (int i = 1; i < 11; i++) {
            ba.sendChatMessage(i, message + nameTag);
        }

        //Alert Subscribers
        if (name == null && listAlert.size() > 0) {
            for (int i = 0; i < listAlert.size(); i++) {
                ba.sendSmartPrivateMessage(listAlert.get(i), message);
            }
        }

        //Alert zoner, (max once every zonerWaitTime (minutes))
        if ((allowZoner() && config.getAllowZoner()) || (allowManualZoner() && !config.getAllowAutoCaps())) {
            ba.sendZoneMessage(message + nameTag, Tools.Sound.BEEP2);
            zonerTimestamp = System.currentTimeMillis();
            manualZonerTimestamp = zonerTimestamp;
        }
    }

    /**
     * Returns if a zoner can be send or not
     * @return True if a zoner can be send, else false
     */
    private boolean allowZoner() {
        // If more time has passed than the waiting time, return true.
        return ((System.currentTimeMillis() - zonerTimestamp) > (zonerWaitTime * Tools.TimeInMillis.MINUTE));
    }

    /**
     * Returns if a zoner can be send or not
     * @return True if a zoner can be send, else false
     */
    private boolean allowManualZoner() {
        // If more than 10 minutes has passed since the last manual zoner, return true.
        return ((System.currentTimeMillis() - manualZonerTimestamp) > (10 * Tools.TimeInMillis.MINUTE));
    }

    /**
     * Returns the score in form of a String
     * @return game score
     */
    private String score() {
        return team0.getName() + " (" + team0.getScore() + ") - "
                + team1.getName() + " (" + team1.getScore() + ")";
    }

    /**
     * Checks if name was a captain on one of the teams and notifies the team of the leave
     *
     * @param name name of the player that left the game and could be captain
     */
    private void checkCaptainLeft(String name) {
        for(SBTeam t : teams) {
            if(t.getCaptainName().equalsIgnoreCase(name)) {
                if (currentState != SBState.WAITING_FOR_CAPS) {
                    t.captainLeftArena();
                } else {
                    t.captainLeft();
                }
            }
        }
    }

    /** 
     * Sends the captain list to the player 
     * 
     * @param name Who to send the list to.
     */
    private void sendCaptainList(String name) {
        for(SBTeam t : teams) 
            ba.sendPrivateMessage(name, t.getCaptainName() + " is captain of " + t.getName() + ".");
    }

    /**
     * Puts a player on a frequency if not playing
     *
     * @param name name of the player that should be put on a frequency
     */
    private void putOnFreq(String name) {
        if (listNotplaying.contains(name)) {
            ba.setFreq(name, FREQ_NOTPLAYING);
            ba.sendPrivateMessage(name, "You are on the !notplaying-list, "
                    + "captains are unable to sub or put you in. "
                    + "Message me with !notplaying again to get you off this list.");
            return;
        }
    }

    /**
     * Sends a welcome message with status info to the player
     *
     * @param name Name of the player that should receive the welcome message
     */
    private void sendWelcomeMessage(String name) {
        ba.sendPrivateMessage(name, "Welcome to StrikeBall.");
        cmd_status(name);    //Sends status info to the player
    }

    /**
     * Handles FrequencyChange event and FrequencyShipChange event
     * <ul>
     *  <li>Checks if the player has lagged out
     *  <li>Checks if the player is allowed in
     * </ul>
     *
     * @param name Name of the player
     * @param frequency Frequency of the player
     * @param ship Ship type of the player
     */
    private void checkFCandFSC(String name, int frequency, int ship) {
        checkLagout(name, ship);  //Check if the player has lagged out
        checkPlayer(name, frequency, ship);  //Check if the player is allowed in
    }

    /**
     * Checks if a player has lagged out
     * <ul>
     *  <li>Check if the player is on one of the teams
     *  <li>Check if the player is in spectator mode
     *  <li>Check if the player is a player on the team
     * </ul>
     *
     * @param name Name of the player
     * @param frequency Frequency of the player
     * @param ship Ship type of the player
     */
    private void checkLagout(String name, int ship) {
        SBTeam t;

        t = getTeam(name);  //Retrieve team

        //Check if the player is in one of the teams, if not exit method
        if (t == null) {
            return;
        }

        //Check if player is in spectator mode, if not exit method
        if (ship != Tools.Ship.SPECTATOR) {
            return;
        }

        //Check if the player is in the team, if not it could just be a captain
        if (!t.isPlayer(name)) {
            return;
        }

        //Check if player is already listed as lagged out/sub/out
        if (!t.isPlaying(name)) {
            return;
        }

        t.lagout(name); //Notify the team that a player lagged out
    }

    /**
     * Checks if a player is allowed in or not
     *
     * @param name Name of the player
     * @param frequency Frequency of the player
     * @param ship Ship type of the player
     */
    private void checkPlayer(String name, int frequency, int ship) {
        SBTeam t;

        //Check if the player is in a ship (atleast not in spectating mode)
        if (ship == Tools.Ship.SPECTATOR) {
            return;
        }

        t = getTeam(name); //Retrieve team, null if not found

        //Check if the player is on one of the teams, if not spectate the player
        if (t == null) {
            ba.specWithoutLock(name);
            return;
        }

        if (t.getPlayerState(name) != SBPlayer.IN) {
            ba.specWithoutLock(name);
        }
    }

    /** 
     * Determines who's turn it is to pick a player
     * <p>
     * Checks which team currently has the least amount of players in the field and sets the turn to pick to that team.
     * On a tie, freq 0 is allowed to pick. 
     */
    private void determineTurn() {
        if (team0.getSizeIN() <= team1.getSizeIN()) {
            if (team0.getSizeIN() != config.getMaxPlayers()) {
                ba.sendArenaMessage(team0.captainName + " pick a player!", Tools.Sound.BEEP2);
                team1.picked();
                team0.setTurn();
            }
        } else if (team1.getSizeIN() < team0.getSizeIN()) {
            if (team1.getSizeIN() != config.getMaxPlayers()) {
                ba.sendArenaMessage(team1.captainName + " pick a player!", Tools.Sound.BEEP2);
                team0.picked();
                team1.setTurn();
            }
        }
    }

    /**
     * Returns SBTeam of the player with "name"
     *
     * @param name name of the player
     * @return SBTeam of the player, null if not on any team
     */
    private SBTeam getTeam(String name) {
        SBTeam t;

        if (team0.isOnTeam(name)) {
            t = team0;
        } else if (team1.isOnTeam(name)) {
            t = team1;
        } else {
            t = null;
        }

        return t;
    }

    /**
     * Returns the opposite team of the player
     *
     * @param name name of the player
     * @param freq Frequency of the team that shouldn't be returned.
     * @return SBTeam, null if player doesn't belong to any team
     */
    private SBTeam getOtherTeam(String name) {
        if (team0.isOnTeam(name)) {
            return team1;
        } else if (team1.isOnTeam(name)) {
            return team0;
        }

        return null;
    }

    /**
     * Returns the opposite team according to SBTeam
     *
     * @param t current team
     * @return other team
     */
    private SBTeam getOtherTeam(SBTeam t) {
        if (t.getFrequency() == 0) {
            return team1;
        } else if (t.getFrequency() == 1) {
            return team0;
        } else {
            return null;
        }
    }

    /**
     * Checks if the arena should be locked or not
     *
     * @param message Arena message
     */
    private void checkArenaLock(String message) {
        if (message.equals("Arena UNLOCKED") && lockArena)
            ba.toggleLocked();
        else if (message.equals("Arena LOCKED") && !lockArena )
            ba.toggleLocked();
    }

    /**
     * Checks if name is a captain on one of the teams
     * Returns true if true, else false
     *
     * @param name Name of the player that could be captain
     * @return true if name is captain, else false
     */
    private boolean isCaptain(String name) {
        boolean isCaptain;
        SBTeam t;

        isCaptain = false;
        t = getTeam(name);

        if (t != null) {
            if (t.getCaptainName().equalsIgnoreCase(name)) {
                isCaptain = true;
            }
        }

        return isCaptain;
    }

    /**
     * Checks if lineups are ok
     * @param timeExpired Must be set to true if this lineup check is caused by exceeding the initial time limit. Otherwise, use false.
     */
    private void checkLineup(boolean timeExpired) {
        int sizeTeam0, sizeTeam1;
        
        sizeTeam0 = team0.getSizeIN();
        sizeTeam1 = team1.getSizeIN();
        
        // Extended lineup check
        if(sizeTeam0 < config.getMinPlayers()) {
            ba.sendArenaMessage("Freq 0 does not have enough players. " +
                    "(Current: " + sizeTeam0 + " players; Needed: " + 
                    config.getMinPlayers() + " players)");
        } else if(sizeTeam1 < config.getMinPlayers()) {
            ba.sendArenaMessage("Freq 1 does not have enough players. " +
                    "(Current: " + sizeTeam1 + " players; Needed: " + 
                    config.getMinPlayers() + " players)");
        } else if(sizeTeam0 > config.getMaxPlayers()) {
            ba.sendArenaMessage("Freq 0 has too many players. " +
                    "(Current: " + sizeTeam0 + " players; Maximum: " + 
                    config.getMaxPlayers() + " players)");
        } else if(sizeTeam1 > config.getMaxPlayers()) {
            ba.sendArenaMessage("Freq 1 has too many players. " +
                    "(Current: " + sizeTeam1 + " players; Maximum: " + 
                    config.getMaxPlayers() + " players)");
        } else if(sizeTeam0 != sizeTeam1) {
            ba.sendArenaMessage("Teams are unequal. " +
                    "(Freq 0: " + sizeTeam0 + " players; Freq 1: " + sizeTeam1 + " players)");
        } else {
            ba.sendUnfilteredPublicMessage("*restart");
            
            currentState = SBState.FACE_OFF;
            ba.sendArenaMessage("Lineups are ok! Game will start in 30 seconds!", Tools.Sound.CROWD_OOO);
            
            // Inform the players of the type of game.
            ba.sendArenaMessage("First team to score " + config.getScoreTarget() + " goals wins!");

            ba.sendArenaMessage("Team: " + team0.getName() + " (Freq 0) <---  |  ---> Team: " + team1.getName() + " (Freq 1)");
            team0.timeout = maxTimeouts;
            team1.timeout = maxTimeouts;
            scoreOverlay.updateAll(null);

            startFaceOff();
            return;
        }
        
        // Code will only go here if the lineups are not ok, otherwise, the return above kicks in.
        if(timeExpired) {
            // When the maximum lineup time has expired, stop the game.
            ba.sendArenaMessage("Lineups are NOT ok! " 
                    + "Game has been cancelled.", Tools.Sound.CROWD_GEE);
            startWaitingForCaps();
        } else {
            // When the time hasn't expired yet, give the captains a chance to fix their teams.
            ba.sendArenaMessage("Lineups are NOT ok! Status of teams set to NOT ready. " 
                    + "Captains, fix your lineups and try again.", Tools.Sound.CROWD_GEE);
            team0.ready();
            team1.ready();
        }

    }

    /**
     * Resets variables to their default value
     */
    private void reset() {
        gameTime = 0;
        team0.resetVariables();
        team1.resetVariables();
        
        scoreOverlay.clearAllObjects();
        scoreOverlay.resetVariables();
        
        ball.clear();

        setSpecAndFreq();
    }

    /**
     * Locks arena
     */
    private void lockArena() {
        lockArena = true;
        ba.toggleLocked();
    }

    /**
     * Unlocks arena
     */
    private void unlockArena() {
        lockArena = false;
        ba.toggleLocked();
    }

    /**
     * Locks all doors in the arena
     */
    private void lockDoors() {
        ba.setDoors(255);
    }

    /**
     * Sets everyone in spec and on right frequency
     */
    private void setSpecAndFreq() {
        for (Iterator<Player> it = ba.getPlayerIterator(); it.hasNext();) {
            Player i = it.next();
            int id = i.getPlayerID();
            int freq = i.getFrequency();
            if (ba.getPlayerName(id) == ba.getBotName()){
                return;
            } else {
                if (i.getShipType() != Tools.Ship.SPECTATOR) {
                    ba.specWithoutLock(id);
                }
                if (listNotplaying.contains(i.getPlayerName()) && freq != FREQ_NOTPLAYING) {
                    ba.setFreq(id, FREQ_NOTPLAYING);
                } else if (freq != FREQ_SPEC && !listNotplaying.contains(i.getPlayerName())) {
                    ba.setShip(id, 1);
                    ba.specWithoutLock(id);
                }
            }
        }
    }

    /* 
     * Game classes 
     */
    /**
     * This holds the configuration for this bot.
     * <p>
     * This class uses the various configuration files to load up the default settings.
     * 
     * @see BottSettings
     * @author unknown
     *
     */
    private class SBConfig {

        /*
         * Settings from strikebot.cfg
         */
        private BotSettings botSettings;            // Settings from a configuration file.
        private String chats;                       // Various chats this bot joins.
        private String arena;                       // The arena this bot joins as default.
        private int maxLagouts;                     // Maximum allowed lagouts for a player. Unlimited when set to -1.
        private int maxSubs;                        // Maximum subs allowed per team. Unlimited when set to -1.
        private int maxPlayers;                     // Maximum number of active players allowed per team.
        private int minPlayers;                     // Minimum number of active players needed per team.
        private int[] defaultShipType;              // Default used ship for each freq.
        private int scoreTarget;
        private int scoreDifference;
        private boolean allowAutoCaps;              // Allow players to !cap themselves when true, or need a ZH+ to !setcaptain captains when false.
        private boolean allowZoner;                 // Whether or not the bot automatically sends out zoners.

        //Coordinate for ball drop.
        private Point ballDropLocation;                  // Points
        //Coordinates for ball during timeout.
        private Point ballTimeOutLocation;               // Points
        //Coordinates for the warp in points.
        private Point[] teamEntryPoint;             // Tiles

        /** Class constructor */
        private SBConfig() {
            botSettings = ba.getBotSettings();

            //Arena
            arena = botSettings.getString("Arena");
            
            //Allow Zoner
            allowZoner = (botSettings.getInt("SendZoner") == 1);

            //Allow automation of captains
            allowAutoCaps = (botSettings.getInt("AllowAuto") == 1);

            //Chats
            chats = botSettings.getString("Chats");

            //Default Ship Type
            defaultShipType = botSettings.getIntArray("DefaultShips", ",");

            //Max Lagouts
            maxLagouts = botSettings.getInt("MaxLagouts");

            //Min Players
            minPlayers = botSettings.getInt("MinPlayers");
            
            //Max Players
            maxPlayers = botSettings.getInt("MaxPlayers");
            
            //Max Amount of Substitutes Allowed
            maxSubs = botSettings.getInt("MaxSubs");

            // Gamemode
            scoreTarget = botSettings.getInt("ScoreTarget");
            scoreDifference = botSettings.getInt("ScoreDifference");
            
            // Various coordinates
            ballDropLocation = botSettings.getPoint("BallDropLocation", ":");
            ballTimeOutLocation = botSettings.getPoint("BallTimeOutLocation", ":");
            
            teamEntryPoint = botSettings.getPointArray("TeamSpawnLocation", ",", ":");
            
        }

        /**
         * Returns the default ship type
         *
         * @return default ship type
         */
        private int getDefaultShipType(int freq) {
            if(freq != 0 && freq != 1) 
                return Tools.Ship.SPECTATOR;
            return defaultShipType[freq];
        }

        /**
         * Returns string with chats
         *
         * @return String with all the chats
         */
        private String getChats() {
            return chats;
        }

        /**
         * Returns the amount of maximum lag outs
         *
         * @return amount of maximum lag outs, -1 if unlimited
         */
        private int getMaxLagouts() {
            return maxLagouts;
        }

        /**
         * Returns the arena name
         *
         * @return arena name
         */
        private String getArena() {
            return arena;
        }

        /**
         * Returns the maximum amount of players allowed
         *
         * @return maximum amount of players allowed
         */
        private int getMaxPlayers() {
            return maxPlayers;
        }

        /**
         * Returns true if auto caps is on, else false
         *
         * @return Returns true if auto caps is on, else false
         */
        private boolean getAllowAutoCaps() {
            return allowAutoCaps;
        }
        
        private void toggleAllowAutoCaps() {
            allowAutoCaps = !allowAutoCaps;
        }

        /**
         * Returns if a zoner can be send
         *
         * @return true if a zoner can be send, else false
         */
        private boolean getAllowZoner() {
            return allowZoner;
        }

        /**
         * Returns minimal amount of players needed in
         *
         * @return minimal amount of players
         */
        private int getMinPlayers() {
            return minPlayers;
        }

        /**
         * Returns maximum allowed substitutes
         *
         * @return maximum allowed substitutes
         */
        private int getMaxSubs() {
            return maxSubs;
        }

        private int getScoreTarget() {
            return scoreTarget;
        }
        
        private int getScoreDifference() {
            return scoreDifference;
        }
        
        /**
         * Returns the coordinates of the ball drop location.
         * @return the ballDropLocation
         */
        public Point getBallDrop() {
            return ballDropLocation;
        }

        /**
         * Returns the coordinates of timeout location of the ball
         * @return the ballTimeOutLocation
         */
        public Point getBallTimeOut() {
            return ballTimeOutLocation;
        }
 
        /**
         * Returns the coordinates of the warp-in point of a specific team.
         * @param freq Frequency of target team.
         * @return the entrypoint of the requested team.
         */
        public Point getTeamEntryPoint(int freq) {
            if(teamEntryPoint.length == 2 && (freq == 0 || freq == 1))
                return teamEntryPoint[freq];
            return null;
        }
        
    }

    /**
     * This class keeps track of anything player related. 
     * It is mainly used for stat tracking and some other player related values.
     * @author unknown
     *
     */
    private class SBPlayer {

        private String p_name;                              // Player's name
        private int p_currentShip;                          // The current ship of the player.
        private int p_state;                                // The current state of the player (IN, SUBBED, etc.)
        private long p_timestampLagout;                     // Timestamp of the last time the player was lagged out. 
        private long p_timestampSub;                        // Timestamp of the last time the player was subbed.
        private int p_lagouts;                              // Number of lagouts for this player.
        private int p_frequency;                            // Player's current frequency.
        
        //Ship states for p_state.
        private static final int IN = 0;                    // Player is in a ship and active.
        private static final int LAGOUT = 1;                // Player is lagged out.
        private static final int OUT_SUBABLE = 2;           // Player is out, but allowed to be subbed.
        private static final int SUBBED = 3;                // Player has been subbed.
        private static final int OUT = 4;                   // Player is out.
        //Static variables
        private static final int SUB_WAIT_TIME = 15;        // Wait time between !subs, in seconds
        private static final int LAGOUT_TIME = 15 * Tools.TimeInMillis.SECOND;  // Time in which the player is allowed back into the game with !lagout, in milliseconds.
        
        //Player statistics.
        private int assists = 0;                            // Assists on clean goals.
        private int goals = 0;                              // Clean goals made.
        private int kills = 0;
        private int deaths = 0;

        /** Class constructor */
        private SBPlayer(String player, int shipType, int frequency) {
            p_name = player;
            p_currentShip = shipType;
            p_frequency = frequency;
            p_lagouts = 0;

            ba.scoreReset(p_name);
            addPlayer();

            p_timestampLagout = 0;
            p_timestampSub = 0;
        }

        /**
         * Adds a player into the game.
         */
        private void addPlayer() {
            // Set state to in (active) and ticks the current ship as used.
            p_state = IN;

            // Can't do anything if we cannot find the player.
            if (ba.getPlayer(p_name) == null) {
                return;
            }

            // Puts the player in an actual ship and in the right frequency.
            ba.setShip(p_name, p_currentShip);
            ba.setFreq(p_name, p_frequency);

            // Warps the player to the correct warp in point.
            ba.warpTo(p_name, config.getTeamEntryPoint(p_frequency));
        }

        /**
         * Increases the assist stat by one.
         */
        public void madeAssist() {
            this.assists++;
        }

        /**
         * Adds a goal to the player stats.
         * If the goal was made by the player himself, adds one to the shotsOnGoal as well.
         */
        public void madeGoal() {
            this.goals++;

            if (this.goals == 3) {
                ba.sendArenaMessage("HAT TRICK by " + this.getName() + "!", 19);
            }
        }

        /**
         * Puts player IN in shiptype
         *
         * @param shipType ship type
         */
        private void putIN(int shipType) {
            p_currentShip = shipType;
            addPlayer();
        }

        /**
         * Returns the current ship state
         *
         * @return int current ship state
         */
        private int getCurrentState() {
            return p_state;
        }

        /**
         * Handles a lagout event
         * <ul>
         *  <li>Notes down the timestamp of the lagout
         *  <li>Adds one to the lagout counter
         *  <li>Check if the player is out due maximum of lagouts
         *  <li>Tell the player how to get back in
         * </ul>
         */
        private void lagout() {
            p_state = LAGOUT;
            p_timestampLagout = System.currentTimeMillis();

            if (currentState == SBState.GAME_IN_PROGRESS) {
                p_lagouts++;

                //Notify the team of the lagout
                ba.sendOpposingTeamMessageByFrequency(p_frequency, p_name + " lagged out or specced.");

                //Check if player is out due maximum of lagouts
                if ((config.getMaxLagouts() != -1) && p_lagouts >= config.getMaxLagouts()) {
                    //Extra check if player is not already set to OUT, due death limit (the +1 death thing)
                    if (p_state < OUT) {
                        out("lagout limit");
                    }
                }

                //Message player how to get back in if he is not out
                if (p_state != OUT) {
                    ba.sendPrivateMessage(p_name, "PM me \"!lagout\" to get back in.");
                }
            }
        }

        /**
         * This method handles a player going out
         * <ul>
         *  <li>Changes player state
         *  <li>Spectates the player
         *  <li>Notifies the arena
         *  <li>Change state according to reason
         * </ul>
         * 
         * @param reason Reason why the player went out
         */
        private void out(String reason) {
            String arenaMessage = "";

            p_state = OUT;

            //Spectate the player if he is in the arena
            if (ba.getPlayer(p_name) != null) {
                ba.specWithoutLock(p_name);
                ba.setFreq(p_name, p_frequency);
            }

            //Notify arena and change state if player is still subable
            if (reason.equals("out not playing")) {
                arenaMessage = p_name + " is out, (set himself to notplaying). NOTICE: Player is still subable.";
                p_state = OUT_SUBABLE;
            }

            ba.sendArenaMessage(arenaMessage);
        }

        /**
         * Returns the name of this player
         *
         * @return Returns the name of this player
         */
        private String getName() {
            return p_name;
        }

        /**
         * Returns current type of ship
         *
         * @return Returns current type of ship
         */
        private int getCurrentShipType() {
            return p_currentShip;
        }

        /**
         * Returns whether a !sub is allowed on this player
         *
         * @return true if sub is allowed, else false
         */
        private boolean isSubAllowed() {
            if ((System.currentTimeMillis() - p_timestampSub) <= (SUB_WAIT_TIME * Tools.TimeInMillis.SECOND)) {
                return false;
            } else {
                return true;
            }
        }

        /**
         * Returns time in seconds until next sub
         *
         * @return Returns time in seconds until next sub
         */
        private long getTimeUntilNextSub() {
            return (SUB_WAIT_TIME - ((System.currentTimeMillis() - p_timestampSub) / Tools.TimeInMillis.SECOND));
        }

        /**
         * Returns lagout time
         *
         * @return lagout time
         */
        private long getLagoutTimestamp() {
            return p_timestampLagout;
        }

        /**
         * Returns a player into the game
         */
        private void lagin() {
            ba.sendOpposingTeamMessageByFrequency(p_frequency, p_name + " returned from lagout.");
            addPlayer();
        }

        /**
         * Returns status
         *
         * @return status
         */
        private String getStatus() {
            switch (p_state) {
                case (IN):
                    return "IN";
                case (LAGOUT):
                    return "LAGGED OUT";
                case (SUBBED):
                    return "SUBSTITUTED";
                case (OUT):
                    return "OUT";
                case (OUT_SUBABLE):
                    return "OUT (still substitutable)";
                default:
                    return "";
            }
        }

        /**
         * Subs the player OUT
         */
        private void sub() {
            p_state = SUBBED;
            if (ba.getPlayer(p_name) != null) {
                ba.specWithoutLock(p_name);
                if (!listNotplaying.contains(p_name)) {
                    ba.setFreq(p_name, p_frequency);
                }
            }

            p_timestampSub = System.currentTimeMillis();
        }
        
        private int getGoals() {
            return goals;
        }
        
        private int getAssists() {
            return assists;
        }
        private void addKill() {
            kills++;
        }
        
        private void addDeath() {
            deaths++;
        }
        
        private int getKills() {
            return kills;
        }
        
        private int getDeaths() {
            return deaths;
        }
        
        private Float getKDRatio() {
            if(deaths == 0)
                return null;
            
            return (float) kills/deaths;
        }
        
        /**
         * Calculates the rating of a player.
         * Used weights might require to be changed in the future
         * 
         * @return players rating
         */
        private int getTotalRating() {
            // Random formula, based on twht's one.
            return (goals * 100 + assists * 10 + kills - deaths);
        }
    }

    /**
     * This class handles all team related functions and stat tracking.
     * 
     * @author unknown
     *
     */
    private class SBTeam {

        private boolean turnToPick;                         // True if it's this team's turn to pick a player.
        private boolean ready;                              // True if this team has finished its line up and is ready to begin.
        private int timeout;                                // Amount of time outs this team is still able to use. This is set at the start of a game.
        private int frequency;                              // Frequency of this team.
        private TreeMap<String, SBPlayer> players;      // List of names the players on this team, linked to their SBPlayer class object.
        private TreeMap<Short, String> captains;
        private short captainsIndex;
        private String captainName;                         // Current captain's name.
        private String lastCaptainName;                     // Last captain's name.
        private String teamName;                            // Team name.
        private long captainTimestamp;
        private int substitutesLeft;                        // Amount of substitutes left for this team.
        private int teamScore;                              // Current score for this team.
        
        /** Class constructor */
        private SBTeam(int frequency) {
            this.frequency = frequency;

            players = new TreeMap<String, SBPlayer>();
            captains = new TreeMap<Short, String>();

            resetVariables();
        }

        /**
         * Resets all variables except frequency
         */
        private void resetVariables() {
            players.clear();
            turnToPick = false;
            teamName = "Freq " + frequency;
            captainName = "[NONE]";
            lastCaptainName = "[NONE]";
            captains.clear();
            ready = false;
            substitutesLeft = config.getMaxSubs();
            captainsIndex = -1;
            teamScore = 0;
            
        }


        /**
         * Increases team score by 1.
         */
        private void increaseScore() {
            teamScore++;
            scoreOverlay.updateScores();
        }
        
        /**
         * Lowers the amount of remaining allowed time outs by one.
         */
        private void useTimeOut() {
            if(timeout > 0) timeout--;
        }

        /**
         * Returns the teamname
         *
         * @return teamname
         */
        private String getName() {
            return teamName;
        }
        
        /**
         * Sets the teamname to the parameter provided.
         * For formatting and safety issues, currently limited to 20 chars max.
         * @param newName The new name of the team.
         */
        private void setName(String newName) {
            if(newName.length() > 20) {
                newName = newName.substring(0, 20);
            }
            teamName = newName;
            scoreOverlay.updateNames();
        }

        /**
         * Returns the current state of the player
         *
         * @param name name of the player
         * @return current state of the player
         */
        private int getPlayerState(String name) {
            int playerState;    //Current state of the player

            playerState = -1;   //-1 if not found

            try {
                if (players.containsKey(name)) {
                    playerState = players.get(name).getCurrentState();
                }
            } catch (Exception e) {
            }

            return playerState;
        }

        /**
         * Checks if the player is on this team
         *
         * @param name name of the player
         * @return true if on team, false if not
         */
        private boolean isOnTeam(String name) {
            boolean isOnTeam = false;

            try {
                if (players.containsKey(name)) {
                    isOnTeam = true;
                } else if (name.equalsIgnoreCase(captainName)) {
                    isOnTeam = true;
                } else {
                    isOnTeam = false;
                }
            } catch (Exception e) {
            }

            return isOnTeam;
        }

        /**
         * Checks if the player is a player on the team
         *
         * @param name Name of the player
         * @return true if is a player, false if not
         */
        private boolean isPlayer(String name) {
            boolean isPlayer = false;

            try {
                if (players.containsKey(name)) {
                    isPlayer = true;
                } else {
                    isPlayer = false;
                }
            } catch (Exception e) {
            }

            return isPlayer;
        }

        /**
         * Checks if the player is playing or has played for the team
         *
         * @param name Name of the player
         * @return return if player was IN, else false
         */
        private boolean isIN(String name) {
            boolean isIN = false;

            try {
                if (players.containsKey(name)) {
                    if (players.get(name).getCurrentState() != SBPlayer.SUBBED) {
                        isIN = true;
                    }
                }
            } catch (Exception e) {
            }

            return isIN;
        }

        /**
         * Checks if a player has lagged out or not
         *
         * @param name name of the player that could have lagged out
         * @return true if player has lagged out, else false
         */
        private boolean laggedOut(String name) {
            SBPlayer p;
            Player player;

            try {
                if (!players.containsKey(name)) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }

            p = players.get(name);

            if (p.getCurrentState() == SBPlayer.IN) {
                player = ba.getPlayer(name);

                if (player == null) {
                    return false;
                }

                if (player.getShipType() == Tools.Ship.SPECTATOR) {
                    return true;
                }
            }

            if (p.getCurrentState() == SBPlayer.LAGOUT) {
                return true;
            }

            return false;
        }

        /**
         * Handles a lagout event
         * - Sends a lagout event to the player
         * - Notify the captain
         *
         * @param name Name of the player that lagged out
         */
        private void lagout(String name) {

            try {
                if (players.containsKey(name)) {
                    players.get(name).lagout();
                }
            } catch (Exception e) {
            }

            //Notify captain if captian is in the arena
            if (ba.getPlayer(captainName) != null) {
                ba.sendPrivateMessage(captainName, name + " lagged out!");
            }

            //NOTE: the team is notified in the SBPlayer lagout() method
        }

        /**
         * Returns the name of the current captain
         *
         * @return name of the current captain
         */
        private String getCaptainName() {
            return captainName;
        }

        /**
         * Removes captain
         * - Notifies arena
         * - Sets captainName to [NONE]
         */
        private void captainLeft() {
            ba.sendArenaMessage(captainName + " has been removed as captain of " + teamName + ".");
            lastCaptainName = captainName;
            captainName = "[NONE]";
        }

        /**
         * Notify the arena that the captain has left the arena
         */
        private void captainLeftArena() {
            if (config.getAllowAutoCaps()) {
                ba.sendArenaMessage("The captain of " + teamName
                        + " has left the arena, anyone of Freq " + frequency + " can claim cap with !cap");
            } else {
                ba.sendArenaMessage("The captain of " + teamName
                        + " has left the arena.");
            }
        }

        /**
         * Returns if its the team's turn to pick
         *
         * @return true if its the team's turn, else false
         */
        private boolean isTurn() {
            return turnToPick;
        }

        /**
         * Returns the amount of players in the team.
         * Meaning all the players but the subbed ones
         *
         * @return amount of players IN
         */
        private int getSizeIN() {
            int sizeIn;

            sizeIn = 0;

            for (SBPlayer i : players.values()) {
                if (i.p_state != SBPlayer.SUBBED) {
                    sizeIn++;
                }
            }

            return sizeIn;
        }

        /**
         * Adds a player to the team
         * - Sending the arena, captain and the player a message
         * - Adding the player
         *
         * @param p Player that is added
         * @param shipType shiptype
         */
        private void addPlayer(Player p, int shipType) {
            String arenaMessage;    //Arena message
            String captainMessage;  //Captain message
            String playerMessage;   //Player message
            String pName;           //Player name

            pName = p.getPlayerName();

            playerMessage = "You've been added to the game.";
            captainMessage = pName + " has been added.";
            arenaMessage = pName + " is in for " + teamName + ".";


            /* Send the messages */
            ba.sendArenaMessage(arenaMessage);
            ba.sendPrivateMessage(captainName, captainMessage);
            ba.sendPrivateMessage(pName, playerMessage);

            if (!players.containsKey(pName)) {
                players.put(pName, new SBPlayer(pName, shipType, frequency));
            } else {
                players.get(pName).putIN(shipType);
            }
        }

        /**
         * Team has picked a player
         * - turnToPick set to false
         */
        private void picked() {
            turnToPick = false;
        }

        /**
         * Sets turn to true
         */
        private void setTurn() {
            turnToPick = true;
        }

        /**
         * Checks if the team has a captain
         * <ul>
         *  <li>Checks if captainName is equal to [NONE]
         *  <li>Checks if captain is in the arena
         * </ul>
         * @return true if the team has a captain, else false
         */
        private boolean hasCaptain() {
            if (captainName.equals("[NONE]")) {
                return false;
            } else if (ba.getPlayer(captainName) == null) {
                return false;
            } else {
                return true;
            }
        }

        /**
         * Sets captain
         * <ul>
         *  <li>Sets timestamp
         *  <li>Sends arena message
         * </ul>
         *
         * @param name Name of the captain
         */
        private void setCaptain(String name) {
            Player p;

            p = ba.getPlayer(name);

            if (p == null) {
                return;
            }

            /* Last check to prevent arena spamming */
            if (name.equalsIgnoreCase(lastCaptainName)) {
                if ((System.currentTimeMillis() - captainTimestamp) <= (5 * Tools.TimeInMillis.SECOND)) {
                    ba.sendPrivateMessage(name, "You will have to wait "
                            + (System.currentTimeMillis() - captainTimestamp) / 1000
                            + " more seconds before you can claim cap again.");
                    sendCaptainList(name);
                    return;
                }
            }

            captainName = p.getPlayerName();
            captainTimestamp = System.currentTimeMillis();

            ba.sendArenaMessage(captainName + " is assigned as captain for "
                    + teamName, Tools.Sound.BEEP1);

            if (currentState != SBState.WAITING_FOR_CAPS) {
                captainsIndex++;
                captains.put(captainsIndex, captainName);
            }
        }

        /**
         * Sets the current captain in the captain list
         */
        private void putCaptainInList() {
            captainsIndex++;
            captains.put(captainsIndex, captainName);
        }

        /**
         * Searches for name in team
         *
         * @param name name of the player that needs to get found
         * @return SBPlayer if found, else null
         */
        private SBPlayer searchPlayer(String name) {
            SBPlayer p;

            if(name == null)
                return null;
            
            p = null;
            name = name.toLowerCase();

            for (SBPlayer i : players.values()) {
                if (i.getName().toLowerCase().startsWith(name)) {
                    if (p == null) {
                        p = i;
                    } else if (i.getName().toLowerCase().compareTo(p.getName().toLowerCase()) > 0) {
                        p = i;
                    }
                }
            }

            return p;
        }

        /**
         * Determines if a player is allowed back in with !lagout/!return
         *
         * @param name name of the player that needs to get checked
         * @return true if allowed back in, else false
         */
        private boolean laginAllowed(String name) {
            SBPlayer p;
            Player player;
            boolean skipLagoutTime;

            skipLagoutTime = false;

            if (!players.containsKey(name)) {
                return false;
            }

            p = players.get(name);

            switch (p.getCurrentState()) {
                case SBPlayer.IN:
                    player = ba.getPlayer(name);

                    if (player == null) {
                        return false;
                    }

                    if (player.getShipType() != Tools.Ship.SPECTATOR) {
                        return false;
                    }

                    skipLagoutTime = true;
                    break;
                case SBPlayer.LAGOUT:
                    break;
                default:
                    return false;
            }

            //Check if enough time has passed
            if (!skipLagoutTime) {
                if (System.currentTimeMillis() - p.getLagoutTimestamp() < SBPlayer.LAGOUT_TIME) {
                    return false;
                }
            }

            /*
             * All checks done
             */
            return true;
        }

        /**
         * Returns the corresponding error message of a not allowed !lagout
         *
         * @param name name of the player that issued the !lagout command
         * @return Error message string
         */
        private String getLaginErrorMessage(String name) {
            SBPlayer p;
            Player player;
            boolean skipLagoutTime;

            skipLagoutTime = false;

            if (!players.containsKey(name)) {
                return "ERROR: You are not on one of the teams.";
            }

            p = players.get(name);

            switch (p.getCurrentState()) {
                case SBPlayer.IN:
                    player = ba.getPlayer(name);

                    if (player == null) {
                        return "ERROR: Unknown";
                    }

                    if (player.getShipType() != Tools.Ship.SPECTATOR) {
                        return "Error: You have not lagged out.";
                    }

                    skipLagoutTime = true;
                    break;
                case SBPlayer.LAGOUT:
                    break;
                default:
                    return "ERROR: You have not lagged out.";
            }

            //Check if enough time has passed
            if (!skipLagoutTime) {
                if (System.currentTimeMillis() - p.getLagoutTimestamp() < SBPlayer.LAGOUT_TIME) {
                    return "You must wait for " + (SBPlayer.LAGOUT_TIME
                            - (System.currentTimeMillis() - p.getLagoutTimestamp())) / Tools.TimeInMillis.SECOND
                            + " more seconds before you can return into the game.";
                }
            }

            return "ERROR: Unknown";
        }

        /**
         * Returns player into the game
         *
         * @param name name of the player
         */
        private void lagin(String name) {
            if (!players.containsKey(name)) {
                return;
            }

            players.get(name).lagin();
        }

        /**
         * Returns if player is currently playing or not
         *
         * @param name name of the player
         * @return true if the player is IN, else false
         */
        private boolean isPlaying(String name) {
            if (!players.containsKey(name)) {
                return false;
            }

            if (players.get(name).getCurrentState() == SBPlayer.IN) {
                return true;
            } else {
                return false;
            }
        }

        /**
         * Returns team's frequency
         *
         * @return frequency
         */
        private int getFrequency() {
            return frequency;
        }

        /**
         * Completely removes player from the team
         *
         * @param name name of the player that needs to get removed
         */
        private void removePlayer(String name) {
            Player p;

            if (players.containsKey(name)) {
                //Remove the player from the team.
                players.remove(name);
            }
            
            ba.sendArenaMessage(name + " has been removed from " + teamName);

            p = ba.getPlayer(name);

            if (p == null) {
                return;
            }

            if (p.getShipType() != Tools.Ship.SPECTATOR) {
                ba.specWithoutLock(name);
            }

            if (listNotplaying.contains(name)) {
                ba.setFreq(name, FREQ_NOTPLAYING);
            } else {
                ba.setFreq(name, FREQ_SPEC);
            }
        }
        
        /**
         * Sets player to not playing modus, player will still be subable
         *
         * @param name Name of the player that should be set to out notplaying
         */
        private void setOutNotPlaying(String name) {
            if (players.containsKey(name)) {
                players.get(name).out("out not playing");
            }
        }

        /**
         * Readies the team or sets it to not ready
         */
        private void ready() {
            if (!ready) {
                if (players.size() >= config.getMinPlayers()) {
                    ba.sendArenaMessage("Team " + teamName + " is ready to begin.");
                    ready = true;
                } else {
                    ba.sendPrivateMessage(captainName, "Cannot ready, not enough players in.");
                }
            } else {
                notReady();
            }
        }

        /**
         * Sets the team to not ready
         */
        private void notReady() {
            ready = false;
            ba.sendArenaMessage(teamName + " is NOT ready to begin.");
        }

        /**
         * Returns if team is ready or not
         *
         * @return true if team is ready, else false
         */
        private boolean isReady() {
            if (ready) {
                return true;
            } else {
                return false;
            }
        }

        /**
         * Returns if the team has any substitutes left
         *
         * @return True if team has substitutes left, else false
         */
        private boolean hasSubtitutesLeft() {
            if (substitutesLeft > 0 || substitutesLeft == -1) {
                return true;
            } else {
                return false;
            }
        }

        /**
         * Handles the sub further
         *
         * @param playerOne player one
         * @param playerTwo player two
         */
        private void sub(SBPlayer playerOne, Player playerTwo) {
            int shipType = playerOne.p_currentShip;
            String p2Name = playerTwo.getPlayerName();      //Name player 2

            //Removing player
            playerOne.sub();
            
            //Adding substitute
            if (players.containsKey(p2Name)) {
                SBPlayer p = players.get(p2Name);
                p.p_currentShip = shipType;
                p.addPlayer();
            } else {
                players.put(p2Name,
                        new SBPlayer(p2Name, shipType, frequency));
            }

            ba.sendSmartPrivateMessage(p2Name, "You are subbed in the game.");

            ba.sendArenaMessage(playerOne.p_name + " has been substituted by " + p2Name);

            if (substitutesLeft != -1) {
                substitutesLeft--;
            }

            if (substitutesLeft >= 0) {
                ba.sendSmartPrivateMessage(captainName, "You have "
                        + substitutesLeft + " substitutes left.");
            }
        }

        /**
         * Returns the timestamp when the captain was set
         *
         * @return timestamp of when the captain was set
         */
        private long getCaptainTimeStamp() {
            return captainTimestamp;
        }

        /**
         * Returns sum of scores
         *
         * @return sum of scores
         */
        private int getScore() {
            return teamScore;
        }

        /**
         * Returns a player's {@link SBPlayer} object from this team's players list..
         * @param name Name of the player to look up
         * @return The SBPlayer object associated with this name, or null when the player isn't found.
         */
        public SBPlayer getPlayer(String name) {
            return players.get(name);
        }
    }

    /**
     * This class keeps track of anything related to the ball/ball.
     * 
     * @author unknown
     *
     */
    private class SBBall {

        private byte ballID;                    // The ID of the ball/ball.
        private int timestamp;                  // Timestamp of the last BallPosition event.
        private short ballX;                    // Current X-coordinate of the ball.
        private short ballY;                    // Current Y-coordinate of the ball.
        private boolean carried;                // True if the ball is being carried by someone.
        private String carrier;                 // Name of the current carrier.
        private final Stack<String> carriers;   // List of the current and all previous carries of the ball. Sorted according LIFO.
        private Stack<Point> releases;          // List of points (X- and Y-coordinate) of where the ball was previously released. 
        private boolean holding;                // True if the bot is currently carrying the ball.
        private int dropDelay;                  // Delay before the ball is being dropped during face off.

        /** Class constructor */
        public SBBall() {
            carrier = null;
            carriers = new Stack<String>();
            releases = new Stack<Point>();
            carried = false;
            holding = false;
        }

        /**
         * Called by handleEvent(BallPosition event)
         * <p>
         * Updates almost anything ball related:
         * <ul>
         *  <li>BallID.
         *  <li>Timestamp.
         *  <li>Coordinates.
         *  <li>Current velocity.
         *  <li>Current carrier and carriers stack, if applicable.
         *  <li>Pick up point, if applicable.
         *  <li>Release point stack, if applicable.
         *  <li>Carried and/or holding, if applicable.
         * </ul>
         * @param event the ball position
         */
        public void update(BallPosition event) {
            ballID = event.getBallID();
            this.timestamp = event.getTimeStamp();
            ballX = event.getXLocation();
            ballY = event.getYLocation();
            short carrierID = event.getCarrier();
            if (carrierID != -1) {
                carrier = ba.getPlayerName(carrierID);
            } else {
                carrier = null;
            }

            if (carrier != null && !carrier.equals(ba.getBotName())) {
                if (!carried && currentState == SBState.GAME_IN_PROGRESS) {
                    carriers.push(carrier);
                }
                carried = true;
            } else if (carrier == null && carried) {
                if (carried && currentState == SBState.GAME_IN_PROGRESS) {
                    releases.push(new Point(ballX, ballY));
                }
                carried = false;
            } else if (carrier != null && carrier.equals(ba.getBotName())) {
                holding = true;
            } else if (carrier == null && holding) {
                if (holding && currentState == SBState.GAME_IN_PROGRESS) {
                    releases.push(new Point(ballX, ballY));
                }
                holding = false;
            }

        }

        /**
         * Clears local data for ball
         */
        public void clear() {
            carrier = null;
            try {
                carriers.clear();
                releases.clear();
            } catch (Exception e) {
            }
        }

        /**
         * Returns the current ball ID.
         * @return ballID
         */
        public byte getBallID() {
            return ballID;
        }

        /**
         * Returns the timestamp of the last ball update.
         * @return timestamp
         */
        public int getTimeStamp() {
            return timestamp;
        }

        /**
         * Returns the last known X-coordinate of the ball.
         * @return ballX
         */
        public short getBallX() {
            return ballX;
        }

        /**
         * Returns the last known Y-coordinate of the ball.
         * @return ballY
         */
        public short getBallY() {
            return ballY;
        }

        /**
         * Gets last ball carrier (removes it from stack).
         * @return short player id or null if empty
         */
        public String getLastCarrierName() {
            String id = null;
            if (!carriers.empty()) {
                id = carriers.pop();
            }
            return id;
        }
    }
    
    /**
     * This class handles all the overlay related actions.
     * <p>
     * Using the default {@link Objset} seems chunky, but might fully convert to that system eventually.
     * 
     * @author Trancid
     *
     */
    private class Overlay {
        ArrayList<String> teamNames;        // Currently active (displayed) teamnames
        ArrayList<Integer> activeObjects;   // Currently active (displayed) objects.
        ArrayList<Integer> scores;          // Currently active (displayed) scores.
        Integer time;                       // Currenly active (displayed) time.

        /** Overlay constructor */
        public Overlay() {
            // Initiate main members.
            activeObjects = new ArrayList<Integer>();
            scores = new ArrayList<Integer>();
            teamNames = new ArrayList<String>();
            
            resetVariables();
        }
        
        /**
         * Resets member variables to their initial state.
         */
        public void resetVariables() {
            if(!activeObjects.isEmpty())
                activeObjects.clear();
            if(!scores.isEmpty())
                scores.clear();
            if(!teamNames.isEmpty()) 
                teamNames.clear();
            
            scores.add(9);          // Odd value, but otherwise the initial 0 will not be displayed.
            scores.add(9);
            teamNames.add("     ");
            teamNames.add("     ");
            time = null;          
        }
        
        /*
         * Functions that update only that which differs. 
         */
        /**
         * Run all the update functions, to update the display for all players.
         * @param newTime Time to set the display on. Null if no time is wanted.
         */
        public void updateAll(Integer newTime) {
            updateNames();
            updateScores();
            updateTime(newTime);
        }
        
        /**
         * Updates the currently displayed scores to the real scores, if possible.
         */
        public void updateScores() {
            int oldScore = 0;
            int newScore = 0;
            int freq = 0;
            
            for(SBTeam t : teams) {
                // Can't do anything if we don't have a proper team.
                if(t == null)
                    continue;
                
                freq = t.getFrequency();
                oldScore = scores.get(freq);
                newScore = t.getScore();
                
                // If the old score differs from the new score.
                if(oldScore != newScore) {
                    // Update the score.
                    scores.set(freq, newScore);
                    
                    // For each digit, check which ones are different.
                    for(int i = 0; i < 5; i++) {
                        if(oldScore%10 != newScore%10) {
                            // Update the digit if needed. (I.e. they differ, but don't display leading zeros.)
                            if(i == 0 || oldScore != 0)
                                removeObject(getObjIDScore(freq, i, oldScore%10));
                            if(i == 0 || newScore != 0)
                                dispObject(getObjIDScore(freq, i, newScore%10));
                        }
                        
                        // Remove the last checked digit from the score.
                        oldScore/=10;
                        newScore/=10;
                        
                        // Saves a bit of math to check if there are any significant digits left.
                        if(oldScore == 0 && newScore == 0)
                            break;
                    }
                }
            }
        }
        
        /**
         * Updates the currently displayed names to the current team names.
         * Only the first five characters will be displayed, from which only the alphanumeric characters will be displayed.
         * Any non-alphanumeric characters will be treated as spaces.
         */
        public void updateNames() {
            String oldName, newName;
            int freq = 0;
            
            for(SBTeam t : teams) {
                // Can't do anything if we don't have a proper team.
                if(t == null)
                    continue;
                
                freq = t.getFrequency();
                oldName = teamNames.get(freq).toUpperCase();
                // Get the new name and remove the spaces to make a more useful tag.
                newName = t.getName().toUpperCase().replace(" ", "");
                
                // old_name should be stored as five characters. For comparison, new_name should be trimmed down or extended to this as well.
                if(newName.length() != 5) {
                    newName = newName.concat("     ").substring(0, 5);
                }
                
                // If the names differ, find out what differs and change it accordingly.
                if(!oldName.equals(newName)) {
                    // Update the records.
                    teamNames.set(freq, newName);
                    
                    // Iterate through the strings to compare and update.
                    for(int i = 0; i < 5; i++) {
                        char oldCh = oldName.charAt(i);
                        char newCh = newName.charAt(i);
                        
                        if(oldCh != newCh) {
                            // Update the letter/number, but only if it's alphanumeric.
                            if(Character.isLetter(oldCh) || Character.isDigit(oldCh))
                                removeObject(getObjIDName(freq, i, oldCh));
                            if(Character.isLetter(newCh) || Character.isDigit(newCh))
                                dispObject(getObjIDName(freq, i, newCh));
                        }
                    }
                }
            }
            
        }
        
        /**
         * Compares the given time and updates the LVZ display accordingly.
         * Anything above 99 minutes will be truncated, and anything negative will be treated as null.
         * 
         * @param newTime The new time to be displayed, in seconds. When null is sent, the time is erased.
         */
        public void updateTime(Integer newTime) {
            Integer oldTime = time;
            
            // Treat negative time as null.
            if(newTime != null && newTime < 0) 
                newTime = null;
            
            if(oldTime == null && newTime != null) {
                // No time previously displayed.
                // Funky math to convert seconds into mm:ss, while keeping it in one int.
                newTime = (newTime * 100 / 60) + (newTime % 60);
                time = newTime;
                
                for(int i = 0; i < 4; i++) {
                    // Simply display the new time.
                    dispObject(getObjIDTime(i, newTime%10));
                    newTime /= 10;
                }
                
            } else if(oldTime != null && newTime == null) {
                // Hide the time.
                time = newTime;
                
                for(int i = 0; i < 4; i++) {
                    // Simply remove the old time.
                    removeObject(getObjIDTime(i, oldTime%10));
                    oldTime /= 10;
                }
                
            } else if(oldTime != null && newTime != null) {
                // Update the time, but only where digits differ.
                // Funky math to convert seconds into mm:ss, while keeping it in one int.
                newTime = (newTime / 60) * 100 + (newTime % 60);
                time = newTime;
                
                for(int i = 0; i < 4; i++) {
                    if(oldTime%10 != newTime %10) {
                        // Remove old digit.
                        removeObject(getObjIDTime(i, oldTime%10));
                        // Display new digit.
                        dispObject(getObjIDTime(i, newTime%10));
                    }
                    
                    // Remove the last digits.
                    oldTime /= 10;
                    newTime /= 10;
                }
            }
        }
        
        /*
         * Functions that display.
         */
        /**
         * Displays/shows a LVZ object to all the players.
         * @param objID ID of object to be enabled.
         */
        private void dispObject(int objID) {
            if(activeObjects.isEmpty() || !activeObjects.contains(objID)) {
                activeObjects.add(objID);
                ba.showObject(objID);
            }
        }

        /**
         * Displays all active LVZ objects to a player.
         * Only to be used when a player enters the arena.
         * 
         * @param playerID ID of player for whom the update is.
         */
        public void displayAll(int playerID) {
            if(!activeObjects.isEmpty()) {
                Objset batchObjects = ba.getObjectSet();
                for(int objID : activeObjects) {
                    batchObjects.showObject(playerID, objID);
                }
                ba.setObjects(playerID);
            }
        }
        
        /*
         * Functions that remove.
         */
        /**
         * Removes/hides a single active LVZ object for all players.
         * @param objID ID of object that needs to be removed.
         */
        private void removeObject(Integer objID) {
            if(!activeObjects.isEmpty() && activeObjects.contains(objID)) {
                activeObjects.remove(objID);
                ba.hideObject(objID);
            }
        }
        
        /**
         * Removes/hides all the active LVZ objects for all the players.
         */
        public void clearAllObjects() {
            if(!activeObjects.isEmpty()) {
                Objset batchObjects = ba.getObjectSet();
                for(int objID : activeObjects) {
                    batchObjects.hideObject(objID);
                }
                ba.setObjects();
                activeObjects.clear();
            }
        }
        
        /*
         * Actual LVZ helper functions.
         */
        /**
         * Converts a given letter or number in the name box to its object ID.
         * This is done according to the following formatting:
         * <pre>
         * Boxes: [FREQ0] [FREQ1]
         * Offset: 01234   01234</pre>
         * 
         * Valid values for value are A-Z, a-z and 0-9.
         * 
         * @param freq The frequency for which to look up the object id.
         * @param offset The offset in the name box for the specific frequency.
         * @param value The letter or digit that needs to be converted.
         * @return The object ID associated with the given data.
         */
        private int getObjIDName(int freq, int offset, char value) {
            // Adjust for frequency offset.
            offset = offset + freq * 5;
            if(Character.isDigit(value)) {
                return ((value + 8) * 10 + offset);
            } else {
                return ((Character.toUpperCase(value) - 35) * 10 + offset);
            }
        }
        
        /**
         * Converts a given number in the score box to its object ID.
         * This is done according to the following formatting:
         * <pre>
         * Boxes: [FREQ0] [FREQ1]
         * Offset: 43210   43210</pre>
         * 
         * Valid values for number are 0-9.
         * @param freq The frequency for which to look up the object id.
         * @param offset The offset in the score box for the specific frequency.
         * @param number The digit that needs to be converted.
         * @return The object ID associated with the given data.
         */
        private int getObjIDScore(int freq, int offset, int number) {
            freq++;
            return (freq * 100 + offset * 10 + number);
        }
        
        /**
         * Converts a given number in the time box to its object ID.
         * This is done according to the following formatting:
         * <pre>
         * Box:   [TI:ME]
         * Offset: 32 10 </pre>
         * 
         * Valid values for number are 0-9.
         * @param offset The offset in the time box.
         * @param number The digit that needs to be converted.
         * @return The object ID associated with the given data.
         */
        private int getObjIDTime(int offset, int number) {
            return (700 + offset * 10 + number);
        }
    }

    /**
     * Class Gameticker
     * 
     * This class is the engine of the strikebot. 
     * <p>
     * In essence this is a state machine.
     * Each tick this class performs a check to see in which state it currently is.
     * Depending on the current state, the accompanied "do"-functions get called and executed.
     * <p>
     * At the current default settings, this class' run function is executed every second.
     * <p>
     * Please keep in mind that the run function is threaded, i.e. it runs quasi-simultaniously
     * to the other functions in this bot. This can lead to racing conditions, if no safeguards are used.
     * 
     * @author Unknown
     * 
     * @see TimerTask
     *
     */
    private class Gameticker extends TimerTask {

        /**
         * The core of the Gameticker class.
         * This function is run on every tick and determines what to do next.
         */
        @Override
        public void run() {
            switch (currentState) {
                case OFF:
                    break;
                case WAITING_FOR_CAPS:
                    doWaitingForCaps();
                    break;
                case ADDING_PLAYERS:
                    doAddingPlayers();
                    break;
                case FACE_OFF:
                    doFaceOff();
                    break;
                case GAME_IN_PROGRESS:
                    doStartGame();
                    break;
                case GAME_OVER:
                    doGameOver();
                    break;
                case TIMEOUT:
                    doTimeout();
                    break;
                case WAIT:
                    /* 
                     * This state is intended to make the timer skip a round.
                     * This is useful when someone wants to switch between certain states,
                     * but not cause any racing conditions.
                     * 
                     * For example: 
                     * When switching from FACE_OFF to GAME_IN_PROGRESS there is a moment when the
                     * bot is actually in between states. During this period, the bot should not refresh
                     * holding the ball, but also not yet start the GAME_IN_PROGRESS part while the correct
                     * things are being set up and done.
                     */
                    break;
            }
        }

        /**
         * Handles the state in which the captains are assigned.
         */
        private void doWaitingForCaps() {
            /*
             * Need two captains within one minute, else remove captain
             */
            if (team0.hasCaptain()) {
                if ((System.currentTimeMillis() - team0.getCaptainTimeStamp()) >= Tools.TimeInMillis.MINUTE) {
                    team0.captainLeft();
                }
            }

            if (team1.hasCaptain()) {
                if ((System.currentTimeMillis() - team1.getCaptainTimeStamp()) >= Tools.TimeInMillis.MINUTE) {
                    team1.captainLeft();
                }
            }

            checkIfEnoughCaps();
        }

        /**
         * Checks if the timelimit has past for adding players.
         * Initiates {@link strikebot#checkLineup() checkLineup()} if this is the case.
         */
        private void doAddingPlayers() {
            // Reduce countdown by one second.
            roundTime--;
            // Update display.
            scoreOverlay.updateTime(roundTime);
            
            if ((System.currentTimeMillis() - timeStamp) >= Tools.TimeInMillis.MINUTE * 10) {
                ba.sendArenaMessage("Time is up! Checking lineups..");
                checkLineup(true);
            }
        }

        /**
         * During the faceoff, this function checks the following:
         * <ul>
         * <li>Checks if the bot needs to pick up the ball;
         * <li>Checks if it's time to issue a drop warning;
         * <li>Checks if the ball needs to be dropped;
         * <li>Checks if a player is offside and warn or penalize them.
         * </ul>
         * If there is a faceoff crease, then it will restart the faceoff after
         * penalizing the offending player(s). Otherwise, it will start the game.
         * <p>
         * @see strikebot#startFaceOff()
         * @see strikebot#startGame()
         */
        private void doFaceOff() {
            long time = (System.currentTimeMillis() - timeStamp) / Tools.TimeInMillis.SECOND;
            
            // When looking at startFaceOff(), this code seems to be redundant.
            // However, due to the bot not always having the latest ball positions, this 
            // safeguard is needed to make it function properly.
            if(!ball.holding && time < ball.dropDelay) {
                doGetBall(config.getBallDrop());
            }
            
            //DROP WARNING
            if (time == 10) {
                ba.sendArenaMessage("Get READY! THE BALL WILL BE DROPPED SOON.", 1);
            }

            //CHECK PENALTIES AND DROP
            if (time >= ball.dropDelay && ball.holding) {
                dropBall();

                ba.sendArenaMessage("test");
                //startGame();
            }
        }

        /**
         * This function is active when the ball is in play.
         * <p>
         * Its main tasks is to keep track of various stats, like steals, turnovers and saves,
         * as well as checking for possible penalties. It also handles the {@link strikebot#gameTime gameTime} counter.
         * <p>
         * Depending on the type of crease, this function might initiate a new {@link strikebot#startFaceOff() Faceoff}.
         */
        private void doStartGame() {
            gameTime++;
            scoreOverlay.updateTime(gameTime);
        }

 
        /**
         * Handles the GAME_OVER state.
         * 
         * This state is active after the game has ended and the final stats have been displayed.
         * Depending on the settings, it will either automatically start a new game or 
         * cleans everything up and shuts the strikebot down.
         * 
         * @see strikebot#cmd_off(String)
         */
        private void doGameOver() {
            long time;

            time = (System.currentTimeMillis() - timeStamp) / Tools.TimeInMillis.SECOND;

            if (!lockLastGame && (time >= 10)) {
                startWaitingForCaps();
            } else if (time >= 10) {
                currentState = SBState.OFF;
                ba.sendArenaMessage("Bot has been shutdown.", Tools.Sound.GAME_SUCKS);
                reset();
                unlockArena();
                scoreOverlay.clearAllObjects();
            }
        }
        
        /**
         * Handles the TIMEOUT state.
         * 
         * Checks if a 10 second warning needs to be fired, or
         * if the timeout has ended. In case of the latter, 
         * a new {@link strikebot#startFaceOff() faceoff} will be started.
         */
        private void doTimeout() {
            long time;
            
            time = (System.currentTimeMillis() - timeStamp) / Tools.TimeInMillis.SECOND;
            
            // The timeout has finished, going back to the faceoff.
            if(time >= 30) {
                ba.sendArenaMessage("The timeout has ended.", Tools.Sound.HALLELUJAH);
                startFaceOff();
            } else if (time == 20) {
                ba.sendArenaMessage("Timeout will end in 10 seconds.");
            } else {
                doRemoveBall();
            }
            
        }
    }

    /**
     * Used for debugging purposes only. When committing the code, please either temporary remove
     * the @SuppressWarnings line to doublecheck that this function throws the being unused warning, or
     * check if you get an "Unnescessary @SuppressWarnings("unused")" message.
     * <p>
     * When choosing to send the debugmessages in game, please be aware of the location you are
     * putting your calls to this function, because this can easily get your bot kicked for flooding.
     * 
     * @param msg Message to be sent to either the console and/or ingame.
     */
    @SuppressWarnings("unused")
    private void debugMessage(String msg) {
        //ba.sendSmartPrivateMessage("ThePAP", msg);
        System.out.println(msg);
    }
}

