package twcore.bots.distensionbot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.TimerTask;
import java.util.LinkedList;
import java.util.Random;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FlagClaimed;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.FrequencyShipChange;
import twcore.core.game.Player;
import twcore.core.lvz.Objset;
import twcore.core.util.Tools;


/**
 * DistensionBot -- for the progressive war-game, 'Distension'.
 *
 *
 * *** Development notes ***
 * 
 * Add:
 * - last few ships
 * - MAP: fix A1; add LVZ displaying rules; safes
 * - special prize for conflict win
 * 
 * Lower priority (in order):
 * - !intro, !intro2, !intro3, etc.
 * - 60-second timer that does full charge for spiders, burst/warp for terrs, restores !emp every 20th run, etc.
 * - !emp for terr "targetted EMP" ability, and appropriate player data.  This involves negative full charge.
 * - F1 Help -- item descriptions?  At least say which slot is which, if not providing info on the specials
 * - LVZ stuff: replacement sounds; point counter; display showing flags owned
 *
 *
 * Adding up points:
 *   Flag multiplier - No flags=all rewards are 1; 1 flag=regular; 2 flags=x2
 *   15 levels lower or more           = 1 pt
 *   14 levels below to 9 levels above = level of ship
 *   10 levels above or more           = killer's level + 10
 *
 * @author dugwyler
 */
public class distensionbot extends SubspaceBot {

    private final boolean DEBUG = true;                    // Debug mode.  Displays various info that would
                                                           // normally be annoying in a public release.
    private final float DEBUG_MULTIPLIER = 1.5f;           // Amount of RP to give extra in debug mode

    private final int AUTOSAVE_DELAY = 15;                 // How frequently autosave occurs, in minutes 
    private final int UPGRADE_DELAY = 300;                 // Delay for prizing players, in ms  
    private final int TICKS_BEFORE_SPAWN = 10;             // # of UPGRADE_DELAYs player must wait before respawn
    private final int RESPAWN_SAFETY_TIME = 1500;          // #ms while a player is spawn protected  
    private final String DB_PROB_MSG = "That last one didn't go through.  Database problem, it looks like.  Please send a ?help message ASAP.";
    private final int NUM_UPGRADES = 14;                   // Number of upgrade slots allotted per ship
    private final double EARLY_RANK_FACTOR = 1.6;          // Factor for rank increases (lvl 1-10)
    private final double NORMAL_RANK_FACTOR = 1.15;        // Factor for rank increases (lvl 11+)
    private final double RANK_0_STRENGTH = 5;              // How much str a rank 0 player adds to army (rank1 = 1 + rank0str, etc)
    private final int RANK_REQ_SHIP2 = 5;    // 15
    private final int RANK_REQ_SHIP3 = 2;    //  4
    private final int RANK_REQ_SHIP4 = 6;    // 20
    private final int RANK_REQ_SHIP5 = 3;    //  7
    private final int RANK_REQ_SHIP6 = 4;    // N/A (only has level for beta)
    private final int RANK_REQ_SHIP7 = 778;  // 10  (beta: 4)
    private final int RANK_REQ_SHIP8 = 1;    //  2

    private final int SPAWN_BASE_0_Y_COORD = 456;               // Y coord around which base 0 owners (top) spawn
    private final int SPAWN_BASE_1_Y_COORD = 566;               // Y coord around which base 1 owners (bottom) spawn
    private final int SPAWN_Y_SPREAD = 90;                      // # tiles * 2 from above coords to spawn players
    private final int SPAWN_X_SPREAD = 275;                     // # tiles * 2 from x coord 512 to spawn players  
    private final int SAFE_TOP_Y = 199;                         // Y coords of safes, for warping in
    private final int SAFE_BOTTOM_Y = 824;
    
    // These coords are used for !terr and !whereis
    private final int TOP_SAFE = 242;
    private final int TOP_ROOF = 270;
    private final int TOP_FR =   315;
    private final int TOP_MID =  380;
    private final int TOP_LOW =  437;
    private final int BOT_LOW =  586;
    private final int BOT_MID =  643;
    private final int BOT_FR =   708;
    private final int BOT_ROOF = 753;
    private final int BOT_SAFE = 781;

    private String m_database;                              // DB to connect to

    private BotSettings m_botSettings;
    private CommandInterpreter m_commandInterpreter;
    private PrizeQueue m_prizeQueue;                        // Queuing system for prizes (so as not to crash bot)
    private SpecialAbilityTask m_specialAbilityPrizer;      // Prizer for special abilities (run once every 30s)

    private Vector <ShipProfile>m_shipGeneralData;          // Generic (nonspecific) purchasing data for ships.  Uses 1-8 for #
    private HashMap <String,DistensionPlayer>m_players;     // In-game data on players (Name -> Player)
    private HashMap <Integer,DistensionArmy>m_armies;       // In-game data on armies  (ID -> Army)

    private TimerTask entranceWaitTask;                     // For when bot first enters the arena
    private TimerTask autoSaveTask;                         // For autosaving player data frequently
    private boolean readyForPlay = false;                   // True if bot has entered arena and is ready to go
    private int[] flagOwner = {-1, -1};                     // ArmyIDs of flag owners; -1 for none
    private List <String>mineClearedPlayers;                // Players who have already cleared mines this battle

    // ASSIST SYSTEM
    private final float ADVERT_WEIGHT_IMBALANCE = 0.9f;     // At what point to advert that there's an imbalance
    private final float ASSIST_WEIGHT_IMBALANCE = 0.9f;     // At what point an army is considered imbalanced
    private final int ASSIST_NUMBERS_IMBALANCE = 4;         // # of pilot difference before considered imbalanced
    private final int ASSIST_REWARD_TIME = 1000 * 60 * 1;   // Time between adverting and rewarding assists (1 min def.)
    private long lastAssistReward;                          // Last time assister was given points
    private long lastAssistAdvert;                          // Last time an advert was sent for assistance
    private boolean checkForAssistAdvert = false;           // True if armies may be unbalanced, requiring !assist advert
    private TimerTask assistAdvertTask;

    // DATA FOR FLAG TIMER
    private static final int MAX_FLAGTIME_ROUNDS = 7;   // Max # rounds (odd numbers only)
    private static final int SECTOR_BREAK_SECONDS = 3;  // Seconds it takes to break sector
    private static final int INTERMISSION_SECS = 90;    // Seconds between end of round & start of next
    private boolean flagTimeStarted;                    // True if flag time is enabled
    private boolean stopFlagTime;                       // True if flag time will stop at round end

    private int freq0Score, freq1Score;                 // # rounds won
    private int flagMinutesRequired = 1;                // Flag minutes required to win
    private HashMap <String,Integer>playerTimes;        // Roundtime of player on freq

    private FlagCountTask flagTimer;                    // Flag time main class
    private StartRoundTask startTimer;                  // TimerTask to start round
    private IntermissionTask intermissionTimer;         // TimerTask for round intermission    
    private AuxLvzTask scoreDisplay;                    // Displays score lvz
    private AuxLvzTask scoreRemove;                     // Removes score lvz
    private AuxLvzConflict delaySetObj;                 // Schedules a task after an amount of time
    private Objset flagTimerObjs;                       // For keeping track of flagtimer objs
    private Objset flagObjs;                            // For keeping track of flag-related objs

    
    // LVZ OBJ# DEFINES ( < 100 reserved for flag timer counter )
    private final int LVZ_REARMING = 200;               // Rearming / attach at own risk
    private final int LVZ_TOPBASE_EMPTY = 251;          // Flag display
    private final int LVZ_TOPBASE_ARMY0 = 252;
    private final int LVZ_TOPBASE_ARMY1 = 253;
    private final int LVZ_BOTBASE_EMPTY = 254;
    private final int LVZ_BOTBASE_ARMY0 = 255;
    private final int LVZ_BOTBASE_ARMY1 = 256;
    private final int LVZ_SECTOR_HOLD = 257;            // Sector hold, above flag display
    private final int LVZ_INTERMISSION = 1000;          // Green intermission "highlight around" gfx
    private final int LVZ_ROUND_COUNTDOWN = 2300;       // Countdown before round start
    private final int LVZ_FLAG_CLAIMED = 2400;          // Flag claimed "brightening"





    /**
     * Constructor.
     * @param botAction Reference to available BotAction instantiation.
     */
    public distensionbot(BotAction botAction) {
        super(botAction);

        m_botSettings = m_botAction.getBotSettings();
        m_commandInterpreter = new CommandInterpreter( botAction );

        m_database = m_botSettings.getString("Database");

        requestEvents();
        registerCommands();

        m_shipGeneralData = new Vector<ShipProfile>();
        m_players = new HashMap<String,DistensionPlayer>();
        m_armies = new HashMap<Integer,DistensionArmy>();
        playerTimes = new HashMap<String,Integer>();
        mineClearedPlayers = Collections.synchronizedList( new LinkedList<String>() );
        flagTimerObjs = m_botAction.getObjectSet();
        flagObjs = new Objset();
        setupPrices();
        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fnArmyID FROM tblDistensionArmy" );
            while( r.next() ) {
                int id = r.getInt( "fnArmyID");
                DistensionArmy army = new DistensionArmy(id);
                m_armies.put(id, army );
            }
            m_botAction.SQLClose(r);
        } catch (SQLException e) {
            Tools.printLog("Error retrieving army data on startup.");
        }
        flagTimeStarted = false;
        stopFlagTime = false;
        m_specialAbilityPrizer = new SpecialAbilityTask();
        m_botAction.scheduleTaskAtFixedRate(m_specialAbilityPrizer, 30000, 30000 );
    }


    /**
     * Request events that this bot requires to receive.
     *
     */
    private void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.ARENA_JOINED);
        req.request(EventRequester.PLAYER_ENTERED);
        req.request(EventRequester.PLAYER_LEFT);
        req.request(EventRequester.PLAYER_DEATH);
        req.request(EventRequester.LOGGED_ON);
        req.request(EventRequester.FLAG_CLAIMED);
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
    }


    /**
     * Performs startup tasks once the bot has entered the arena.
     */
    public void init() {
        m_botAction.sendUnfilteredPublicMessage("?chat=distension" );
        m_botAction.setMessageLimit( 10 );
        m_botAction.setReliableKills(1);
        m_botAction.setPlayerPositionUpdating(500);
        m_botAction.specAll();
        m_botAction.toggleLocked();
        m_botAction.resetFlagGame();
        m_prizeQueue = new PrizeQueue();
        m_botAction.scheduleTaskAtFixedRate(m_prizeQueue, 1000, UPGRADE_DELAY);
        entranceWaitTask = new TimerTask() {
            public void run() {
                readyForPlay = true;
            }
        };
        m_botAction.scheduleTask( entranceWaitTask, 3000 );
        if( AUTOSAVE_DELAY != 0 ) {
            autoSaveTask = new TimerTask() {
                public void run() {
                    cmdSaveData( ":autosave:", null );
                }
            };
            m_botAction.scheduleTask( autoSaveTask, AUTOSAVE_DELAY * 60000, AUTOSAVE_DELAY * 60000 );
        }        
        // Do not advert/reward for rectifying imbalance in the first 5 min of a game 
        lastAssistReward = System.currentTimeMillis();
        lastAssistAdvert = System.currentTimeMillis();
        assistAdvertTask = new TimerTask() {
            public void run() {
                if( !checkForAssistAdvert )
                    return;
                DistensionArmy army0 = m_armies.get(0);
                DistensionArmy army1 = m_armies.get(1);             
                float armyStr0 = army0.getTotalStrength();
                float armyStr1 = army1.getTotalStrength();
                if( armyStr1 == 0 ) armyStr1 = 1;
                if( armyStr0 == 0 ) armyStr1 = 1;
                float armyWeight0 = armyStr0 / armyStr1;
                float armyWeight1 = armyStr1 / armyStr0;
                int helpOutArmy = -1;
                int msgArmy = -1;
                if( armyWeight1 < ADVERT_WEIGHT_IMBALANCE ) {
                    helpOutArmy = 1;
                    msgArmy = 0;
                } else if( armyWeight0 < ADVERT_WEIGHT_IMBALANCE ) {
                    helpOutArmy = 0;
                    msgArmy = 1;
                }
                if( helpOutArmy != -1 ) {
                    m_botAction.sendOpposingTeamMessageByFrequency( msgArmy, "ARMIES IMBALANCED: !pilot lower rank ships, or !assist " + helpOutArmy + " to even the battle (rewarded).  [ " + army0.getTotalStrength() + " vs " + army1.getTotalStrength() + " ]");
                    lastAssistAdvert = System.currentTimeMillis();
                // Check if teams are imbalanced in numbers, if not strength
                } else {
                    if( army0.getPilotsInGame() <= army1.getPilotsInGame() - ASSIST_NUMBERS_IMBALANCE )
                        m_botAction.sendOpposingTeamMessageByFrequency( 0, "ARMY SIZE NOTICE: Your army has fewer pilots but is close in strength; if you need help, !pilot lower-ranked ships so the other army can !assist yours." );
                    else if( army1.getPilotsInGame() <= army0.getPilotsInGame() - ASSIST_NUMBERS_IMBALANCE )
                        m_botAction.sendOpposingTeamMessageByFrequency( 1, "ARMY SIZE NOTICE: Your army has fewer pilots but is close in strength; if you need help, !pilot lower-ranked ships so the other army can !assist yours." );
                }
                checkForAssistAdvert = false;
            }
        };
        m_botAction.scheduleTask( assistAdvertTask, 20000, 20000 );
        
        if( DEBUG ) {
            m_botAction.sendUnfilteredPublicMessage("?find dugwyler" );
            m_botAction.sendChatMessage("Distension BETA initialized.  ?go #distension");
        }
    }


    /**
     * Registers all commands necessary.
     *
     */
    private void registerCommands() {
        int acceptedMessages = Message.PRIVATE_MESSAGE;        

        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "cmdHelp" );
        m_commandInterpreter.registerCommand( "!enlist", acceptedMessages, this, "cmdEnlist" );
        m_commandInterpreter.registerCommand( "!defect", acceptedMessages, this, "cmdDefect" );
        m_commandInterpreter.registerCommand( "!return", acceptedMessages, this, "cmdReturn" );
        m_commandInterpreter.registerCommand( "!pilot", acceptedMessages, this, "cmdPilot" );
        m_commandInterpreter.registerCommand( "!ship", acceptedMessages, this, "cmdPilot" );    // For the confused, such as me
        m_commandInterpreter.registerCommand( "!dock", acceptedMessages, this, "cmdDock" );
        m_commandInterpreter.registerCommand( "!armies", acceptedMessages, this, "cmdArmies" );
        m_commandInterpreter.registerCommand( "!hangar", acceptedMessages, this, "cmdHangar" );
        m_commandInterpreter.registerCommand( "!status", acceptedMessages, this, "cmdStatus" );
        m_commandInterpreter.registerCommand( "!progress", acceptedMessages, this, "cmdProgress" );
        m_commandInterpreter.registerCommand( ".", acceptedMessages, this, "cmdProgress" );
        m_commandInterpreter.registerCommand( "!armory", acceptedMessages, this, "cmdArmory" );
        m_commandInterpreter.registerCommand( "!armoury", acceptedMessages, this, "cmdArmory" ); // For those that can't spell
        m_commandInterpreter.registerCommand( "!upgrade", acceptedMessages, this, "cmdUpgrade" );
        m_commandInterpreter.registerCommand( "!scrap", acceptedMessages, this, "cmdScrap" );
        m_commandInterpreter.registerCommand( "!intro", acceptedMessages, this, "cmdIntro" );
        m_commandInterpreter.registerCommand( "!warp", acceptedMessages, this, "cmdWarp" );
        m_commandInterpreter.registerCommand( "!terr", acceptedMessages, this, "cmdTerr" );
        m_commandInterpreter.registerCommand( "!whereis", acceptedMessages, this, "cmdWhereIs" );
        m_commandInterpreter.registerCommand( "!assist", acceptedMessages, this, "cmdAssist" );
        m_commandInterpreter.registerCommand( "!upginfo", acceptedMessages, this, "cmdUpgInfo" );
        m_commandInterpreter.registerCommand( "!team", acceptedMessages, this, "cmdTeam" );
        m_commandInterpreter.registerCommand( "!clearmines", acceptedMessages, this, "cmdClearMines" );
        m_commandInterpreter.registerCommand( "!beta", acceptedMessages, this, "cmdBeta" );  // BETA CMD
        m_commandInterpreter.registerCommand( "!msgbeta", acceptedMessages, this, "cmdMsgBeta", OperatorList.HIGHMOD_LEVEL ); // BETA CMD
        m_commandInterpreter.registerCommand( "!grant", acceptedMessages, this, "cmdGrant", OperatorList.HIGHMOD_LEVEL );     // BETA CMD
        m_commandInterpreter.registerCommand( "!info", acceptedMessages, this, "cmdInfo", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!ban", acceptedMessages, this, "cmdBan", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!unban", acceptedMessages, this, "cmdUnban", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!savedata", acceptedMessages, this, "cmdSaveData", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!die", acceptedMessages, this, "cmdDie", OperatorList.HIGHMOD_LEVEL );

        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "handleInvalidMessage" );
        m_commandInterpreter.registerDefaultCommand( Message.REMOTE_PRIVATE_MESSAGE, this, "handleRemoteMessage" );
        m_commandInterpreter.registerDefaultCommand( Message.ARENA_MESSAGE, this, "handleArenaMessage" );
    }


    /**
     * Catches unlocking of arena and re-locks.
     * @param name
     * @param message
     */
    public void handleArenaMessage( String name, String message ) {
        if( message.equals( "Arena UNLOCKED" ) )
            m_botAction.toggleLocked();
        
        // Beta should not be loaded w/o me; ?find dugwyler done at start to ensure this.
        if( DEBUG )
            if( message.startsWith("Not online, last seen") ) {
                m_botAction.sendUnfilteredPublicMessage("?message dugwyler:Unauthorized load of Distension BETA." );
                Tools.printLog("Unauthorized load of Distension BETA.");
                m_botAction.sendArenaMessage("Unauthorized load of Distension BETA; shutting down.");
                m_botAction.sendChatMessage("Unauthorized load of Distension BETA; shutting down.");
                cmdDie(name, message);
            }
    }


    /**
     * Display appropriate message for invalid commands.
     * @param name
     * @param msg
     */
    public void handleInvalidMessage( String name, String msg ) {
        if( name.equals( m_botAction.getBotName() ) )
            return;
        try {
            Integer i = Integer.parseInt(msg);
            if( i >= 1 && i <= 8 )      // For lazy pilots
                cmdPilot( name, msg );
        } catch (NumberFormatException e) {
        }
    }


    /**
     * Display non-acceptance message for remote msgs.
     * @param name
     * @param msg
     */
    public void handleRemoteMessage( String name, String msg ) {
        m_botAction.sendSmartPrivateMessage( name, "Can't quite hear ya.  C'mere, and maybe I'll !help you."  );
    }


    /**
     * Display help based on access level.
     * @param name
     * @param msg
     */
    public void cmdHelp( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null ) {
            p = new DistensionPlayer(name);
            m_players.put( name, p );
        }
        int shipNum = p.getShipNum();

        if( shipNum == -1 ) {
            String[] helps = {
                    "   CIVILIAN CONSOLE  ",
                    ".---------------------",
                    "| !enlist             |  Enlist in the army that needs your services most",
                    "| !armies             |  View all public armies and their IDs",
                    "| !enlist <army#>     |  Enlist specifically in <army#>",
                    "| !intro              |  Gives an introduction to Distension",                    
                    "| !return             |  Return to your current position in the war",
            };
            m_botAction.privateMessageSpam(name, helps);
        } else if( shipNum == 0 ) {
            String[] helps = {
                    "    HANGAR CONSOLE",
                    ".---------------------",
                    "| !pilot <ship>       |  Pilot <ship> if available in hangar",
                    "| <shipnum>           |  Shortcut for !pilot <shipnum>",
                    "| !hangar             |  View your ships & those available for purchase",
                    "| !assist <army>      |  Temporarily assists <army> at no penalty to you",
                    "| !defect <army>      |  Change to <army>.  All ships return to start of rank,",
                    "|                     |     unless the new army has 4 or more fewer pilots.",
                    "| !upginfo <upg>      |  Shows any available information about <upg>",
                    "| !warp               |  Toggles waiting in spawn vs. being autowarped out",
                    "| !team               |  Shows all players on team and their upg. levels",
                    "| !terr               |  Shows approximate location of all army terriers",
                    "| !whereis <name>     |  Shows approximate location of pilot <name>",
            };
            m_botAction.privateMessageSpam(name, helps);
        } else {
            String[] helps = {
                    "    PILOT CONSOLE  ",
                    ".---------------------",
                    "| !progress (or .)    |  See your progress toward next advancement",
                    "| !status             |  View current ship's level and upgrades",
                    "| !armory             |  View ship upgrades available in the armory",
                    "| !upgrade <upg>      |  Upgrade your ship with <upg> from the armory",
                    "| !upginfo <upg>      |  Shows any available information about <upg>",
                    "| !scrap <upg>        |  Trade in <upg>.  *** Restarts ship at current rank!",
                    "| !hangar             |  View your ships & those available for purchase",
                    "| !dock               |  Dock your ship, recording status to headquarters",
                    "| !pilot <ship>       |  Change to <ship> if available in hangar",
                    "| <shipnum>           |  Shortcut for !pilot <shipnum>",
                    "| !assist <army>      |  Temporarily assists <army> at no penalty to you",
                    "| !warp               |  Toggles waiting in spawn vs. being autowarped out",
                    "| !team               |  Shows all players on team and their upg. levels",
                    "| !terr               |  Shows approximate location of all army terriers",
                    "| !whereis <name>     |  Shows approximate location of pilot <name>",
                    "| !clearmines         |  Clears all mines, if in a mine-laying ship"
            };
            m_botAction.privateMessageSpam(name, helps);
        }

        if( m_botAction.getOperatorList().isHighmod(name) ) {
            String[] helps = {
                    "| !info <name>        |  Gets info on <name> from their !status screen",
                    "| !ban <name>         |  Bans a player from playing Distension",
                    "| !unban <name>       |  Unbans banned player",
                    "| !savedata           |  Saves all player data to database",
                    "| !die                |  Kills DistensionBot -- use !savedata first!",
                    "|____________________/"
            };
            m_botAction.privateMessageSpam(name, helps);
        } else {
            m_botAction.sendPrivateMessage(name, 
                    "|____________________/" );
        }
        
    }



    // ***** EVENT PROCESSING

    /**
     * Send all commands to CI.
     * @param event Event to handle.
     */
    public void handleEvent(Message event) {
        m_commandInterpreter.handleEvent( event );
    }


    /**
     * Join the proper arena, and set up spam protection.
     * @param event Event to handle.
     */
    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botSettings.getString("Arena"));
    }


    /**
     * Enable reliable tracking of kills on joining of arena.
     * @param event Event to handle.
     */
    public void handleEvent(ArenaJoined event) {
        init();
    }


    /**
     * Track players as they enter, and provide basic help on playing Distension.
     * @param event Event to handle.
     */
    public void handleEvent(PlayerEntered event) {
        String name = event.getPlayerName();
        if( name == null )
            return;
        m_players.remove( name );
        m_botAction.sendPrivateMessage( name, "Thanks for beta-testing.  Your help is welcome, but bugs are present.  Join ?chat=distension to keep up on tests.  PM DistensionBot with !beta for the latest updates and testing agreement." );
        if( DEBUG && DEBUG_MULTIPLIER != 1 )
            m_botAction.sendPrivateMessage(name, "BETA RP Bonus: x" + DEBUG_MULTIPLIER );
        cmdReturn( name, "" );
    }


    /**
     * Save player data if unexpectedly leaving Distension.
     * @param event Event to handle.
     */
    public void handleEvent(PlayerLeft event) {
        String name = m_botAction.getPlayerName(event.getPlayerID());
        if( name == null ) {
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();
            return;
        }
        DistensionPlayer player = m_players.get( name );
        if( player == null ) {
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();
            return;
        }
        if( player.getShipNum() > 0 ) {
            checkFlagTimeStop();
            player.getArmy().recalculateFigures();
            if( System.currentTimeMillis() > lastAssistAdvert + ASSIST_REWARD_TIME )
                checkForAssistAdvert = true;
        }
        player.saveCurrentShipToDBNow();        
        playerTimes.remove( name );
        m_specialAbilityPrizer.removePlayer( player );
        m_players.remove( name );
    }


    /**
     * Save player data if unexpectedly leaving Distension.
     * @param event Event to handle.
     */
    public void handleEvent(FlagClaimed event) {
        int flagID = event.getFlagID();
        Player p = m_botAction.getPlayer( event.getPlayerID() );
        if( p == null )
            return;
        boolean holdBreaking = false;
        boolean holdSecured = false;
        int oldOwner = flagOwner[flagID];
        flagOwner[flagID] = p.getFrequency();
        if( oldOwner == p.getFrequency() ) {
            // To save bot from two people "flag sitting"
            return;
        }
        
        // If flag is already claimed, try to take it away from old freq
        if( oldOwner != -1 ) { 
            DistensionArmy army = m_armies.get( new Integer( oldOwner ) );
            if( army != null ) {
                if( army.getNumFlagsOwned() == 2 && flagTimer != null && flagTimer.isRunning())
                    holdBreaking = true;
                army.adjustFlags( -1 );
                if( flagTimer != null && flagTimer.isRunning() ) {
                    if( flagID == 0 )
                        if( army.getID() == 0 )
                            flagObjs.hideObject(LVZ_TOPBASE_ARMY0);
                        else
                            flagObjs.hideObject(LVZ_TOPBASE_ARMY1);
                    else
                        if( army.getID() == 0 )
                            flagObjs.hideObject(LVZ_BOTBASE_ARMY0);
                        else
                            flagObjs.hideObject(LVZ_BOTBASE_ARMY1);
                }
                if( DEBUG )
                    m_botAction.sendPrivateMessage( p.getPlayerName(), "Flag #" + flagID + " taken from army #" + oldOwner );
            }            
        } else {
            if( flagTimer != null && flagTimer.isRunning() ) {
                if( flagID == 0 )
                    flagObjs.hideObject(LVZ_TOPBASE_EMPTY);
                else
                    flagObjs.hideObject(LVZ_BOTBASE_EMPTY);
            }
        }
        DistensionArmy army = m_armies.get( new Integer( p.getFrequency() ) );
        if( army != null ) {
            army.adjustFlags( 1 );
            if( flagTimer != null && flagTimer.isRunning() ) {
                if( flagID == 0 )
                    if( army.getID() == 0 )
                        flagObjs.showObject(LVZ_TOPBASE_ARMY0);
                    else
                        flagObjs.showObject(LVZ_TOPBASE_ARMY1);
                else
                    if( army.getID() == 0 )
                        flagObjs.showObject(LVZ_BOTBASE_ARMY0);
                    else
                        flagObjs.showObject(LVZ_BOTBASE_ARMY1);
            }
            if( army.getNumFlagsOwned() == 2 && flagTimer != null && flagTimer.isRunning())
                holdSecured = true;
            if( DEBUG )
                m_botAction.sendPrivateMessage( p.getPlayerName(), "Flag #" + flagID + " added to your army; " + army.getNumFlagsOwned() + " flags now owned");
        }
        if( flagTimer != null && flagTimer.isRunning() )
            m_botAction.manuallySetObjects( flagObjs.getObjects() );
        if( holdBreaking )
            flagTimer.holdBreaking( army.getID(), p.getPlayerName() );
        if( holdSecured )
            flagTimer.sectorClaimed( army.getID(), p.getPlayerName() );
    }


    /**
     * Dock player if they spec.
     * @param event Event to handle.
     */
    public void handleEvent(FrequencyShipChange event) {
        if( !readyForPlay )         // If bot has not fully started up,
            return;                 // don't operate normally when speccing players.
        DistensionPlayer p = m_players.get( m_botAction.getPlayerName( event.getPlayerID() ) );
        if( event.getShipType() == 0 ) {
            doDock( p );
        }
        
        if( System.currentTimeMillis() > lastAssistAdvert + ASSIST_REWARD_TIME )
            checkForAssistAdvert = true;
    }


    /**
     * DEATH HANDLING
     *
     * Upon death, assign points based on level to the victor, and prize the
     * loser back into existence.

     * Adding up points:
     *   Flag multiplier:  No flags=all rewards are 1; 1 flag=regular; 2 flags=x2
     *
     *   15 levels lower or more           = 1 pt
     *   14 levels below to 9 levels above = level of ship
     *   10 levels above or more           = killer's level + 10
     *
     * @param event Event to handle.
     */
    public void handleEvent(PlayerDeath event) {
        Player killer = m_botAction.getPlayer( event.getKillerID() );
        Player killed = m_botAction.getPlayer( event.getKilleeID() );
        if( killed == null )
            return;

        // Prize back to life the player who was killed
        DistensionPlayer loser = m_players.get( killed.getPlayerName() );
        if( loser != null )
            loser.doSetupRespawn();
        else
            return;

        if( killer != null ) {
            DistensionPlayer victor = m_players.get( killer.getPlayerName() );
            if( victor == null )
                return;

            // IF TK: TKer loses points equal to half their level, and they are notified
            // of it if they have not yet been notified this match.  Successive kills are
            // also cleared.
            if( killed.getFrequency() == killer.getFrequency() ) {
                float div;
                // Sharks get off a little easier for TKs
                if( killer.getShipType() == Tools.Ship.SHARK )
                    div = 10;
                else
                    div = 2;
                int loss = Math.round((float)victor.getUpgradeLevel() / div);
                victor.addRankPoints( -loss );
                victor.clearSuccessiveKills();
                //if( !victor.wasWarnedForTK() ) {
                if( loss > 0 )
                    m_botAction.sendPrivateMessage( killer.getPlayerName(), "-" + loss + " RP for TKing " + killed.getPlayerName() + "." );
                //    victor.setWarnedForTK();
                //}
                // Otherwise: Add points via level scheme
            } else {
                DistensionArmy killerarmy = m_armies.get( new Integer(killer.getFrequency()) );
                DistensionArmy killedarmy = m_armies.get( new Integer(killed.getFrequency()) );
                if( killerarmy == null || killedarmy == null )
                    return;
                loser.clearSuccessiveKills();
                                
                if( loser.justRespawned() ) {
                    m_botAction.sendPrivateMessage( killer.getPlayerName(), "DEBUG: " + loser.getName() + " just spawned; 0 RP earned." );
                    return;
                }

                if( victor.justRespawned() ) {
                    m_botAction.sendPrivateMessage( killer.getPlayerName(), "DEBUG: You are spawn-protected; 0 RP earned." );
                    return;
                }

                int points;
                int levelDiff = loser.getUpgradeLevel() - victor.getUpgradeLevel(); 
                
                // Loser is 10 or more levels above victor:
                //   Victor earns loser's level in RP, and loser loses half of that amount from due shame
                if( levelDiff >= 10 ) {
                    points = loser.getUpgradeLevel();
                    
                    // Support ships are not humiliated
                    if( ! victor.isSupportShip() ) {
                        loser.addRankPoints( -(points / 2) );
                        m_botAction.sendPrivateMessage(loser.getName(), "HUMILIATION!  -" + (points / 2) + "RP for being killed by " + victor.getName() + "(" + victor.getUpgradeLevel() + ")");
                    }

                    // Loser is 10 or more levels below victor:
                    //   Victor only gets 1 point, and loser loses nothing
                } else if( levelDiff <= -10 ) {
                    points = 1;

                    // Normal kill:
                    //   Victor earns the level of the loser in points.  Level 0 players are worth 1 point. 
                } else {
                    if( loser.getUpgradeLevel() == 0 )
                        points = 1;
                    else {
                        points = loser.getUpgradeLevel();
                    }
                }

                // Points adjusted based on size of victor's army v. loser's
                float armySizeWeight;
                float killedArmyStr = killedarmy.getTotalStrength();
                float killerArmyStr = killerarmy.getTotalStrength();
                if( killedArmyStr <= 0 ) killedArmyStr = 1;
                if( killerArmyStr <= 0 ) killerArmyStr = 1;
                armySizeWeight = killedArmyStr / killerArmyStr;
                if( armySizeWeight == Float.POSITIVE_INFINITY || armySizeWeight == Float.NEGATIVE_INFINITY ) {
                    if( DEBUG )
                        m_botAction.sendArenaMessage( "DEBUG: Infinity/neg infinity found.  Army " + killedarmy.getID() + " str: " + killedarmy.getTotalStrength() + " / Army " + killerarmy.getID() + " str: " + killerarmy.getTotalStrength() );
                    armySizeWeight = 1;
                }
                if( armySizeWeight > 3.0f )
                    armySizeWeight = 3.0f;
                else if( armySizeWeight < 0.2f )
                    armySizeWeight = 0.2f;

                if( killerarmy.getNumFlagsOwned() == 0 ) {
                    // 1 RP for 0 flag rule only applies if armies are imbalanced.
                    if( armySizeWeight > ASSIST_WEIGHT_IMBALANCE ) {
                        if( DEBUG )
                            m_botAction.sendPrivateMessage( killer.getPlayerName(), "DEBUG: 1 RP for kill.  (0 flags owned)" );
                        victor.addRankPoints( 1 );
                        return;
                    }
                }
                
                int preWeight = points; 
                points = Math.round(((float)points * armySizeWeight));
                points *= killerarmy.getNumFlagsOwned();
                
                if( killedarmy.getPilotsInGame() != 1 ) {                    
                    switch( victor.getRepeatKillAmount( event.getKilleeID() ) ) {
                        case 2:
                            points /= 2;
                            m_botAction.sendPrivateMessage( killer.getPlayerName(), "For repeatedly killing " + loser.getName() + " you earn only half the normal amount of RP." );
                            break;
                        case 3:
                            points = 1;
                            m_botAction.sendPrivateMessage( killer.getPlayerName(), "For repeatedly killing " + loser.getName() + " you earn only 1 RP." );
                            break;
                    }
                }
                if( points < 1 )
                    points = 1;

                victor.addRankPoints( points );
                // Track successive kills for weasel unlock & streaks
                if( levelDiff > -5 )   // Streaks only count players close to your lvl 
                    if( victor.addSuccessiveKill() ) {
                        // If player earned weasel off this kill, check if loser/killed player has weasel ... 
                        // and remove it if they do!
                    }
                if( DEBUG ) {
                    points *= DEBUG_MULTIPLIER;
                    String msg = "KILL: " + points + " RP - " + loser.getName() + "(" + loser.getUpgradeLevel() + ")  [" + preWeight + " * "
                                          + armySizeWeight + " weight" + ((killerarmy.getNumFlagsOwned() == 2) ? " * 2 flags" : "" );
                    if( DEBUG_MULTIPLIER > 1.0f )
                        msg += " * " + DEBUG_MULTIPLIER + " beta bonus]";
                    else
                        msg += "]";
                    m_botAction.sendPrivateMessage( killer.getPlayerName(), msg );
                }
            }
        }
    }



    // ***** COMMAND PROCESSING

    /**
     * Provides a brief introduction to the game for a player.  This should support
     * the A1 LVZ help, and the F1 help.
     * @param name
     * @param msg
     */
    public void cmdIntro( String name, String msg ) {
        String[] intro1 = {
                "DISTENSION - The Trench Wars RPG - G. Dugwyler",
                "Presently in beta.  Intro to come soon.  Type !beta for info on recent updates."
        };
        m_botAction.privateMessageSpam( name, intro1 );
    }
    
    
    /**
     * Shows what prizes are associated with the basic upgrades. 
     * @param name
     * @param msg
     */
    public void cmdUpgInfo( String name, String msg ) {
        Integer upgNum = 0;
        try {
            upgNum = Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            m_botAction.sendPrivateMessage( name, "What upgrade do you want info on?  Do I look like a mind-reader?  Check the !armory before you start asking ..." );
            return;
        }
        
        if( upgNum < 1 ) {
            m_botAction.sendPrivateMessage( name, "What the hell upgrade is that?  You check the !armory -- find out that you're making this crap up ..." );
            return;
        }
        if( upgNum > 10 ) {
            m_botAction.sendPrivateMessage( name, "Now that's an excellent question.  To be honest, I don't know what that upgrade does ... you'll just have to see for yourself!" );
            return;
        }
        String desc = "";
        switch( upgNum ) {
            case 1:
                desc = "Rotation and turning speed";
                break;
            case 2:
                desc = "Thrust and acceleration";
                break;
            case 3:
                desc = "Top speed";
                break;
            case 4:
                desc = "Energy recharge speed";
                break;
            case 5:
                desc = "Energy, armor and shields";
                break;
            case 6:
                desc = "Gunning systems";
                break;
            case 7:
                desc = "Bombing systems (on Terrier: Multifire)";
                break;
            case 8:
                desc = "Multifire (on Terrier: X-Radar)";
                break;
            case 9:
                desc = "X-Radar (on Terrier: Decoy)";
                break;
            case 10:
                desc = "Decoy (on Terrier: Portal)";
                break;            
        }
        m_botAction.sendPrivateMessage( name, "Upgrade #" + upgNum + ": " + desc );
    }

    
    /**
     * Provides a brief introduction to the game for a player.  This should support
     * the A1 LVZ help, and the F1 help.
     * @param name
     * @param msg
     */
    public void cmdBeta( String name, String msg ) {
        String[] beta = {
                "BETA INFO - BASIC",
                "-----------------",
                " - Beta is expected to continue for several months before public release",
                " - Data is saved, but will be cleared at release",
                " - Top 3 players will be awarded bonus points in public release",
                " - For every bug reported, points will be awarded (?message dugwyler)",
                " - You may be sent PMs by the bot when a new test is starting",
                " - Everything is subject to change while testing!",
                ".",
                "RECENT UPDATES  -  10/19/07",
                " - LVZ for flags and rearming added",
                " - !clearmines will clear your mines, if you have any laid",
                " - Time-on-freq saved for legitimate changes to shark & terr",
                " - !team command displays team breakdown, with upg.lvl and strengths",
                " - During beta, testers receive extra RP!  Bonus displayed @ arena enter",
                " - !upginfo <upg> shows information on a specific upgrade",
        };
        m_botAction.privateMessageSpam( name, beta );
    }

    
    /**
     * Enlists in the supplied public army (or private army w/ pwd).  Those choosing default
     * armies may receive an enistment bonus.
     * @param name
     * @param msg
     */
    public void cmdEnlist( String name, String msg ) {
        if( msg == null ) {
            m_botAction.sendPrivateMessage( name, "Hmm.  Try that one again, hot shot.  Maybe tell me where you want to !enlist this time.");
            return;
        }

        // Easy fix: Disallow names >18 chars to avoid name hacking.
        // (Extra char padding ensures no-one can imitate, in case "name" String only shows 19 total)
        if( name.length() > 18 ) {
            m_botAction.sendPrivateMessage( name, "Whoa there, is that a name you have, or a novel?  You need something short and snappy.  Hell, I'd reckon that anything 20 letters or more just won't cut it.  Come back when you have a better name.");
            return;            
        }

        DistensionPlayer p = m_players.get( name );
        if( p == null ) {
            p = new DistensionPlayer(name);
            m_players.put( name, p );
        } else            
            if( p.getShipNum() != -1 ) {
                m_botAction.sendPrivateMessage( name, "Ah, but you're already enlisted.  Maybe you'd like to !defect to an army more worthy of your skills?");
                return;
            }

        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fnArmyID FROM tblDistensionPlayer WHERE fcName = '" + Tools.addSlashesToString( name  ) +"'" );
            if( r.next() ) {
                m_botAction.sendPrivateMessage( name, "Enlist?  You're already signed up!  I know you: you can !return any time.  After that, maybe you'd like to !defect to an army more worthy of your skills?");
                return;
            }
        } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); return; }

        Integer armyNum;
        String pwd = "";

        try {
            int privateIndex = msg.indexOf( ':' );
            if( privateIndex > 0 ) {
                armyNum = Integer.parseInt( msg.substring( 0, privateIndex ) );
                pwd = msg.substring( privateIndex + 1 );
            } else {
                armyNum = Integer.parseInt( msg );
            }
        } catch (Exception e) {
            // If army is not specified, give them the one with the fewest players.
            int count = 99999;
            armyNum = 0;
            for( DistensionArmy a : m_armies.values() )
                if( a.getPilotsTotal() < count ) {
                    if( !a.isPrivate() ) {
                        armyNum = a.getID();
                        count = a.getPilotsTotal();
                    }
                }
        }

        DistensionArmy army = m_armies.get(armyNum);
        if( army == null ) {
            m_botAction.sendPrivateMessage( name, "You making stuff up now?  Maybe you should join one of those !armies that ain't just make believe..." );
            return;
        }

        int bonus = 0;
        if( army.isPrivate() ) {
            if( pwd != null && !pwd.equals(army.getPassword()) ) {
                m_botAction.sendPrivateMessage( name, "That's a private army there.  And the password doesn't seem to match up.  Duff off." );
                return;
            }
        } else {
            bonus = calcEnlistmentBonus( army );
        }

        m_botAction.sendPrivateMessage( name, "Ah, joining " + army.getName().toUpperCase() + "?  Excellent.  You are pilot #" + (army.getPilotsTotal() + 1) + "." );

        p.setArmy( armyNum );
        p.addPlayerToDB();
        p.setShipNum( 0 );
        army.adjustPilotsTotal(1);
        m_botAction.sendPrivateMessage( name, "Welcome aboard." );
        m_botAction.sendPrivateMessage( name, "If you need an !intro to how things work, I'd be glad to !help out.  Or if you just want to get some action, jump in your new Warbird.  (!pilot 1)" );
        if( bonus > 0 ) {
            m_botAction.sendPrivateMessage( name, "Your contract also entitles you to a " + bonus + " RP signing bonus!  Congratulations." );
            p.addShipToDB( 1, bonus );
        } else {
            p.addShipToDB( 1 );
        }
    }


    /**
     * Defects to another army.  This causes all ships to return to the start of the rank
     * in terms of progress toward the next.
     * @param name
     * @param msg
     */
    public void cmdDefect( String name, String msg ) {
        if( msg == null ) {
            m_botAction.sendPrivateMessage( name, "If you want to defect to one of the other !armies, you've got to at least have an idea which one..." );
            return;
        }

        DistensionPlayer p = m_players.get( name );
        if( p == null ) {
            m_botAction.sendPrivateMessage( name, "Sorry, I don't recognize you.  If you !return to your army (such as it is), maybe I can help." );
            return;            
        }

        if( p.getShipNum() != 0 ) {
            m_botAction.sendPrivateMessage( name, "Please !dock before trying to hop over to another army." );
            return;
        }
        
        if( p.getArmyID() != p.getNaturalArmyID() ) {
            m_botAction.sendPrivateMessage( name, "If you're going to !defect, do you really need to !assist all that much?...  Go back to your normal army first!" );
            return;            
        }

        Integer armyNum;
        String pwd = "";

        try {
            int privateIndex = msg.indexOf( ':' );
            if( privateIndex > 0 ) {
                armyNum = Integer.parseInt( msg.substring( 0, privateIndex ) );
                pwd = msg.substring( privateIndex + 1 );
            } else {
                armyNum = Integer.parseInt( msg );
            }
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( name, "Hmm... which army do you want to !defect to?  You sure that's one of the !armies here?" );
            return;
        }

        DistensionArmy army = m_armies.get(armyNum);
        if( army == null ) {
            m_botAction.sendPrivateMessage( name, "You making stuff up now?  Maybe you should defect to one of those !armies that ain't just make believe..." );
            return;
        }

        if( army.isPrivate() ) {
            if( pwd != null && !pwd.equals(army.getPassword()) ) {
                m_botAction.sendPrivateMessage( name, "That's a private army there.  And the password doesn't seem to match up.  Duff off." );
                return;
            }
        }

        DistensionArmy oldarmy = m_armies.get( p.getArmyID() );
        if( oldarmy != null ) {
            if( oldarmy.getID() == army.getID() ) {                
                m_botAction.sendPrivateMessage( name, "Now that's just goddamn stupid.  You're already in that army!" );
                return;
            }
        } else {
            m_botAction.sendPrivateMessage( name, "Whoa, and what the hell army are you in now?  You're confusing me... might want to tell someone important that I told you this." );
            return;
        }

        m_botAction.sendPrivateMessage( name, "So you're defecting to " + army.getName().toUpperCase() + "?  I can't blame you, to be honest.  You'll be pilot #" + ( army.getPilotsTotal() + 1) + "." );

        p.setArmy( armyNum );
        oldarmy.adjustPilotsTotal(-1);
        army.adjustPilotsTotal(1);
        
        // Free !defect if army has less pilots
        boolean weak = (oldarmy.getPilotsTotal() > army.getPilotsTotal() + 3 ); 

        if( weak ) {
            m_botAction.sendPrivateMessage( name, "The ship controls are mostly the same... but since you're defecting to a weaker army, hell -- I'll train you back to exactly where you were before!  OK, should be all set.  Good luck." );
        } else {
            try {
                String query = "SELECT ship.fnPlayerID, ship.fnShipNum, ship.fnRank, ship.fnRankPoints FROM tblDistensionShip ship, tblDistensionPlayer player " +
                "WHERE player.fcName = '" + Tools.addSlashesToString( name ) + "' AND ship.fnPlayerID = player.fnID";
                ResultSet r = m_botAction.SQLQuery( m_database, query );
                while( r.next() ) {
                    int ship = r.getInt("fnShipNum");
                    m_botAction.SQLQueryAndClose(m_database, "UPDATE tblDistensionShip SET fnRankPoints=" + m_shipGeneralData.get(ship).getNextRankCost(r.getInt("fnRank") - 1) + 
                            " WHERE fnShipNum=" + ship + " AND fnPlayerID=" + r.getInt("fnPlayerID") );
                }
            } catch (SQLException e ) {
                m_botAction.sendPrivateMessage( name, DB_PROB_MSG );
                return;
            }
            m_botAction.sendPrivateMessage( name, "The ship controls are mostly the same, but you might have to relearn a little bit...  OK, should be all set.  Good luck." );
        }
    }


    /**
     * Logs a player in / allows them to return to the game.
     * @param name
     * @param msg
     */
    public void cmdReturn( String name, String msg ) {
        DistensionPlayer player = m_players.get( name );
        if( player == null ) {
            player = new DistensionPlayer(name);
            m_players.put( name, player );
        }

        int ship = player.getShipNum();
        if( ship != -1 ) {
            if( ship == 0 )
                m_botAction.sendPrivateMessage( name, "You are currently docked at " + player.getArmyName().toUpperCase() + " HQ.  You may !pilot a ship at any time." );
            else
                m_botAction.sendPrivateMessage( name, "You are currently in flight." );
            return;
        }

        m_botAction.sendPrivateMessage( name, "Authorizing ... " );
        if( player.getPlayerFromDB() == true ) {
            m_botAction.sendPrivateMessage( name, player.getName().toUpperCase() + " authorized as a pilot of " + player.getArmyName().toUpperCase() + ".  Returning you to HQ." );
            player.setShipNum( 0 );
        } else {
            if( player.isBanned() ) {
                m_botAction.sendPrivateMessage( name, "ERROR: Civilians and discharged pilots are NOT authorized to enter this military zone." );
                m_botAction.sendUnfilteredPrivateMessage(name, "*kill 1" );
            } else                
                m_botAction.sendPrivateMessage( name, "Welcome.  PM me with !enlist if you need to enlist in an army.  Or use !armies to see your army choices, and !enlist # to enlist in a specific army. -" + m_botAction.getBotName() );
        }
    }


    /**
     * Enters a player as a given ship.
     * @param name
     * @param msg
     */
    public void cmdPilot( String name, String msg ) {
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;

        if( player.getShipNum() == -1 ) {
            m_botAction.sendPrivateMessage( name, "You'll need to !return to your army or !enlist in a new one before you go flying off." );
            return;
        }

        int shipNum = 0;
        try {
            shipNum = Integer.parseInt( msg );
        } catch ( Exception e ) {
            m_botAction.sendPrivateMessage( name, "Exactly which ship do you mean there?  Give me a number.  Maybe check the !hangar first." );
            return;
        }

        if( player.getShipNum() == shipNum ) {
            m_botAction.sendPrivateMessage( name, "You're already in that ship." );
            return;            
        }

        if( !player.shipIsAvailable( shipNum ) ) {
            m_botAction.sendPrivateMessage( name, "You don't own that ship.  Check your !hangar before you try flying something you don't have." );
            return;
        }

        if( player.getShipNum() > 0 ) {
            player.saveCurrentShipToDBNow();            
            // Simple fix to cause sharks and terrs to not lose MVP 
            if( shipNum == Tools.Ship.TERRIER || shipNum == Tools.Ship.SHARK ) {
                if( flagTimer != null && flagTimer.isRunning() )
                    if( flagTimer.getHoldingFreq() == player.getArmyID() && flagTimer.getSecondsHeld() > 0 ) {
                        // If player is changing to a support ship while their freq is securing a hold,
                        // they're probably just doing it to steal the points; don't keep MVP
                        playerTimes.remove( name );
                    } else {
                        m_botAction.sendPrivateMessage( name, "For switching to a needed support ship, your participation counter has not been reset." );                        
                    }
            } else                
                playerTimes.remove( name );
        }

        player.setShipNum( shipNum );
        if( !player.getCurrentShipFromDB() ) {
            m_botAction.sendPrivateMessage( name, "Having trouble getting that ship for you.  Please contact a mod." );
            player.setShipNum( 0 );
            return;
        }
        
        for( DistensionArmy a : m_armies.values() )
            a.recalculateFigures();
        player.putInCurrentShip();
        player.prizeUpgrades();
        if( flagTimer != null && flagTimer.isRunning() ) {
            if( playerTimes.get( name ) == null )
                playerTimes.put( name, new Integer( flagTimer.getTotalSecs() ) );
        }
        if( !flagTimeStarted || stopFlagTime ) {
            checkFlagTimeStart();
        }                    
        cmdProgress( name, null );                
    }


    /**
     * Docks player (that is, sends them to spectator mode -- not the sex act, or the TW sysop).
     * This also saves ship data to the DB.  The command !dock is the same as just going to
     * spectator mode the manual way.
     * @param name
     * @param msg
     */
    public void cmdDock( String name, String msg ) {
        m_botAction.specWithoutLock( name );
    }

    /**
     * Workhorse of docking, used by the FrequencyShipChange message.
     * @param player Player to dock
     */
    public void doDock( DistensionPlayer player ) {
        if( player == null ) {
            m_botAction.sendArenaMessage( "Dock did not find player!  Status not saved." );
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();
            return;
        }
        DistensionArmy army = player.getArmy();
        if( army != null )
            army.recalculateFigures();
        else
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();            
        playerTimes.remove( player.getName() );
        checkFlagTimeStop();
        if( player.saveCurrentShipToDBNow() ) {
            m_botAction.sendPrivateMessage( player.getName(), "Ship status confirmed and logged to our records.  You are now docked.");
            player.setShipNum( 0 );
        } else {
            m_botAction.sendPrivateMessage( player.getName(), "Ship status was NOT logged.  Please notify a member of staff immediately!");
        }
    }


    /**
     * Shows list of public armies with their current counts and enlistment bonuses.
     * @param name
     * @param msg
     */
    public void cmdArmies( String name, String msg ) {
        DistensionPlayer player = m_players.get( name );
        boolean inGame;
        if( player == null )
            inGame = false;
        else
            inGame = !(player.getShipNum() == -1);

        if( inGame ) 
            m_botAction.sendPrivateMessage( name, "#   " + Tools.formatString("Army Name", 35 ) + "Total     Playing   Strength    Flags" );
        else {
            m_botAction.sendPrivateMessage( name, "ARMY ENLISTMENT TIPS" );
            m_botAction.sendPrivateMessage( name, "--------------------" );
            m_botAction.sendPrivateMessage( name, " - The army you choose is permanent.  You lose points when you !defect to another." );
            m_botAction.sendPrivateMessage( name, " - Choose the army with the biggest enlistment bonus -- free points to get you started!" );
            m_botAction.sendPrivateMessage( name, " - Don't worry about the number of players currently playing.  I'll adjust for it." );
            m_botAction.sendPrivateMessage( name, "#   " + Tools.formatString("Army Name", 35 ) + "Total     Playing   Bonus" );
        }
        //                                                                                     #234567890#23456789#23456789012#

        if( inGame ) {
            //for( DistensionArmy a : m_armies.values() ) {
            for( int i = 0; i < 2; i++ ) {
                DistensionArmy a = m_armies.get(i);
                m_botAction.sendPrivateMessage( name, Tools.formatString( ""+a.getID(), 4 ) +
                        Tools.formatString( a.getName(), 38 ) +
                        Tools.formatString( ""+a.getPilotsTotal(), 10 ) +
                        Tools.formatString( ""+a.getPilotsInGame(), 9 ) +
                        Tools.formatString( ""+a.getTotalStrength(), 12 ) +
                        a.getNumFlagsOwned() );
            }
        } else {
            //for( DistensionArmy a : m_armies.values() ) {
            for( int i = 0; i < 2; i++ ) {
                DistensionArmy a = m_armies.get(i);
                int bonus = 0;
                if( a.isDefault() )
                    bonus = calcEnlistmentBonus( a ); 
                m_botAction.sendPrivateMessage( name, Tools.formatString( ""+a.getID(), 4 ) +
                        Tools.formatString( a.getName(), 38 ) +
                        Tools.formatString( ""+a.getPilotsTotal(), 10 ) +
                        Tools.formatString( ""+a.getPilotsInGame(), 9 ) +
                        bonus + " RP" ); 
            }
        }
    }


    /**
     * Shows the status of all ships available in the hangar, and costs for those
     * not yet purchased, if they can be purchased.
     * @param name
     * @param msg
     */
    public void cmdHangar( String name, String msg ) {
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;

        int currentShip = player.getShipNum();
        if( currentShip == -1 ) {
            m_botAction.sendPrivateMessage( name, "You about to !return to your army, or do you need to !enlist in one?  Don't recognize you quite." );
            return;
        }

        ShipProfile pf;
        for( int i = 1; i < 9; i++ ) {
            if( player.shipIsAvailable(i) ) {
                if( currentShip == i ) {
                    m_botAction.sendPrivateMessage( name, "  " + i + "   " + Tools.formatString( Tools.shipName( i ), 20 ) + "IN FLIGHT: Rank " + player.getRank() + " (" + player.getUpgradeLevel() + " upg)" );
                } else {
                    m_botAction.sendPrivateMessage( name, "  " + i + "   " + Tools.formatString( Tools.shipName( i ), 20 ) + "AVAILABLE" );
                }
            } else {
                pf = m_shipGeneralData.get(i);
                if( pf.getRankUnlockedAt() == -1 )
                    m_botAction.sendPrivateMessage( name, "  " + i + "   " + Tools.formatString( Tools.shipName( i ), 20 ) + "LOCKED" );
                else
                    m_botAction.sendPrivateMessage( name, "  " + i + "   " + Tools.formatString( Tools.shipName( i ), 20 ) + "RANK " + pf.getRankUnlockedAt() + " NEEDED"  );
            }
        }
    }


    /**
     * Wrapper for !status command.
     * @param name
     * @param msg
     */
    public void cmdStatus( String name, String msg ) {
        cmdStatus( name, msg, null );
    }

    /**
     * Shows current status of ship -- its level and upgrades.
     * @param name
     * @param msg
     * @param mod Mod to give info to (rather than player)
     */
    public void cmdStatus( String name, String msg, String mod ) {
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;
        String theName;
        if( mod != null )
            theName = mod;
        else
            theName = name;
        int shipNum = player.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( theName, "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );
            return;
        } else if( shipNum == 0 ){
            m_botAction.sendPrivateMessage( theName, player.getName().toUpperCase() + ": DOCKED.  [Your data is saved; safe to leave arena.]" );
            return;
        }
        m_botAction.sendPrivateMessage( theName, player.getName().toUpperCase() + " of " + player.getArmyName().toUpperCase() + ":  " + Tools.shipName( shipNum ).toUpperCase() +
                " - RANK " + player.getRank() + " (" + player.getUpgradeLevel() + " upgrades)" );
        Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum ).getAllUpgrades();
        ShipUpgrade currentUpgrade;
        int[] purchasedUpgrades = player.getPurchasedUpgrades();
        String printmsg;
        for( int i = 0; i < NUM_UPGRADES; i++ ) {
            currentUpgrade = upgrades.get( i );
            if( currentUpgrade.getMaxLevel() != -1 ) {
                printmsg = (i+1 < 10 ? " " : "") + (i + 1) + ": " + Tools.formatString( currentUpgrade.getName(), 30) + "- ";
                if( currentUpgrade.getMaxLevel() == 0 )
                    printmsg += "N/A";
                else if( currentUpgrade.getMaxLevel() == 1 )
                    if( purchasedUpgrades[i] == 1 )
                        printmsg += "(INSTALLED)";
                    else
                        printmsg += "(NOT INSTALLED)";
                else {
                    printmsg += "LVL " + purchasedUpgrades[i];
                    if(currentUpgrade.getMaxLevel() == purchasedUpgrades[i])
                        printmsg += "(MAX)";
                }
                m_botAction.sendPrivateMessage( theName, printmsg );
            }
        }
        if( mod == null )
            cmdProgress( name, msg, null );
        else
            cmdProgress( name, msg, mod );
        m_botAction.sendPrivateMessage( theName, player.getUpgradePoints() + " Upgrade Points available." );
    }


    /**
     * Wrapper for !progress.
     */
    public void cmdProgress( String name, String msg ) {
        cmdProgress( name, msg, null );
    }


    /**
     * Quick view of progress toward next advancement.
     * @param name
     * @param msg
     */
    public void cmdProgress( String name, String msg, String mod ) {
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;
        if( mod != null )
            name = mod;
        int shipNum = player.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( name, "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );
            return;
        } else if( shipNum == 0 ) {
            m_botAction.sendPrivateMessage( name, "You must be in-ship to see your progress toward the next rank." );
            return;            
        }

        m_botAction.sendPrivateMessage( name, "Rank " + player.getRank() + " " + Tools.shipName(shipNum) + " (" + player.getRankPoints() + " RP), " + player.getUpgradeLevel() + " upg." );

        double pointsSince = player.getPointsSinceLastRank();
        double pointsNext = player.getNextRankPointsProgressive();
        int progChars = 0;
        if( pointsSince != 0 )
            progChars = (int)(( pointsSince / pointsNext ) * 20);
        String progString = Tools.formatString("", progChars, "=" );
        progString = Tools.formatString(progString, 20 );
        m_botAction.sendPrivateMessage( name, "Progress:  " + (int)pointsSince + " / " + (int)pointsNext + "     [" + progString + "]     " + player.getPointsToNextRank() + " RP to next." );
    }


    /**
     * Shows upgrades available for purchase.
     * @param name
     * @param msg
     */
    public void cmdArmory( String name, String msg ) {
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;
        int shipNum = player.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( name, "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );
            return;
        } else if( shipNum == 0 ){
            m_botAction.sendPrivateMessage( name, "If you want to see the armory's selection for a ship, you'll need to !pilot one first." );
            return;
        }
        m_botAction.sendPrivateMessage( name, Tools.shipName( shipNum ).toUpperCase() + " ARMORY    " + player.getUpgradePoints() + " upgrade points available");
        m_botAction.sendPrivateMessage( name, " #  Name                                  Curr /  Max     Points    Rank Req." );
        Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum ).getAllUpgrades();
        ShipUpgrade currentUpgrade;
        int[] purchasedUpgrades = player.getPurchasedUpgrades();
        String printmsg;
        boolean printCost;
        for( int i = 0; i < NUM_UPGRADES; i++ ) {
            printCost = true;
            currentUpgrade = upgrades.get( i );
            if( currentUpgrade.getMaxLevel() != -1 ) {
                printmsg = (i+1 < 10 ? " " : "") + (i + 1) + ": " + Tools.formatString( currentUpgrade.getName(), 38);
                if( currentUpgrade.getMaxLevel() == 0 ) {
                    printmsg += "N/A";
                    printCost = false;
                } else if( currentUpgrade.getMaxLevel() == 1 ) {
                    if( purchasedUpgrades[i] == 1 ) {
                        printmsg += "(INSTALLED)";
                        printCost = false;
                    } else {
                        printmsg += "(NOT INSTALLED)   ";
                    }
                } else {
                    if(currentUpgrade.getMaxLevel() == purchasedUpgrades[i]) {
                        printmsg += "( MAX )";
                        printCost = false;
                    } else {
                        printmsg += Tools.formatString("( " + (purchasedUpgrades[i] < 10 ? " " : "") + purchasedUpgrades[i] + " / " +
                                (currentUpgrade.getMaxLevel() < 10 ? " " : "") + currentUpgrade.getMaxLevel() + " )", 18);
                    }
                }
                if( printCost ) {
                    printmsg += Tools.formatString( "" + currentUpgrade.getCostDefine( purchasedUpgrades[i] ), 11 );
                    int req = currentUpgrade.getRankRequired( purchasedUpgrades[i] );
                    if( req <= player.getRank() )
                        printmsg += "AVAIL!";
                    else
                        printmsg += (req < 10 ? " " : "") + req;
                }
                m_botAction.sendPrivateMessage( name, printmsg );
            }
        }
    }


    /**
     * Upgrades a particular aspect of the current ship.
     * @param name
     * @param msg
     */
    public void cmdUpgrade( String name, String msg ) {
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;
        int shipNum = player.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( name, "You'll need to !return to your army or !enlist in a new one before upgrading a ship." );
            return;
        } else if( shipNum == 0 ){
            m_botAction.sendPrivateMessage( name, "If you want to upgrade a ship, you'll need to !pilot one first." );
            return;
        }
        int upgradeNum = 0;
        try {
            upgradeNum = Integer.parseInt( msg );
        } catch ( Exception e ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Give me a number.  Maybe check the !armory first." );
        }
        upgradeNum--;  // Fix number to correspond with internal numbering scheme
        if( upgradeNum < 0 || upgradeNum > NUM_UPGRADES ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Maybe check the !armory first.  #" + upgradeNum + " doesn't work for me." );
            return;            
        }            
        ShipUpgrade upgrade = m_shipGeneralData.get( shipNum ).getUpgrade( upgradeNum );
        int currentUpgradeLevel = player.getPurchasedUpgrade( upgradeNum );
        if( upgrade == null || currentUpgradeLevel == -1 ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Maybe check the !armory first.  #" + upgradeNum + " doesn't work for me." );
            return;
        }
        if( currentUpgradeLevel >= upgrade.getMaxLevel() ) {
            m_botAction.sendPrivateMessage( name, "You've upgraded that one as much as you can." );
            return;
        }
        int req = upgrade.getRankRequired( currentUpgradeLevel );
        if( player.getRank() < req ) {
            m_botAction.sendPrivateMessage( name, "Sorry... I'm not allowed to fit your ship with that until you're at least a rank " + req + " pilot.");
            return;            
        }

        int cost = upgrade.getCostDefine( currentUpgradeLevel );
        if( player.getUpgradePoints() < cost ) {
            m_botAction.sendPrivateMessage( name, "You'll need more army upgrade authorization points before I can fit your ship with that.  And the only way to get those is to go up in rank... by -- you got it! -- killin'." );
            return;
        }
        player.addUpgPoints( -cost );
        player.modifyUpgrade( upgradeNum, 1 );
        if( upgrade.getPrizeNum() == -1 )
            player.setFastRespawn(true);
        else {
            if( !player.isRespawning() )
                if( upgrade.getPrizeNum() > 0 )
                    m_botAction.specificPrize( name, upgrade.getPrizeNum() );
        }
        if( upgrade.getMaxLevel() == 1 )
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " has been installed on the " + Tools.shipName( shipNum ) + "." );
        else
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " on the " + Tools.shipName( shipNum ) + " upgraded to level " + (currentUpgradeLevel + 1) + "." );
        if( upgrade.getPrizeNum() == Tools.Prize.GUNS || upgrade.getPrizeNum() == Tools.Prize.BOMBS )
            m_botAction.sendPrivateMessage( name, "IMPORTANT NOTE: If you find you can not fire after this upgrade, your ship may not yet have enough energy!  In this case you will need to !scrap " + (upgradeNum + 1) + " to return weapons capability.");
    }


    /**
     * Scraps a previously-installed upgrade.
     * @param name
     * @param msg
     */
    public void cmdScrap( String name, String msg ) {
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;
        int shipNum = player.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( name, "You'll need to !return to your army before you scrap any upgrades." );
            return;
        } else if( shipNum == 0 ){
            m_botAction.sendPrivateMessage( name, "If you want to remove upgrades on a ship, you'll need to !pilot one first." );
            return;
        }
        int upgradeNum = 0;
        try {
            upgradeNum = Integer.parseInt( msg );
        } catch ( Exception e ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Give me a number.  Maybe check the !armory first." );
        }
        upgradeNum--;  // Fix number to correspond with internal numbering scheme
        if( upgradeNum < 0 || upgradeNum > NUM_UPGRADES ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Maybe check the !armory first.  #" + upgradeNum + " doesn't work for me." );
            return;            
        }            
        ShipUpgrade upgrade = m_shipGeneralData.get( shipNum ).getUpgrade( upgradeNum );
        int currentUpgradeLevel = player.getPurchasedUpgrade( upgradeNum );
        if( upgrade == null || currentUpgradeLevel == -1 ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Maybe check the !armory first.  #" + upgradeNum + " doesn't work for me." );
            return;
        }
        if( currentUpgradeLevel <= 0 ) {
            m_botAction.sendPrivateMessage( name, "You haven't exactly upgraded that, now have you?" );
            return;
        }

        int cost = upgrade.getCostDefine( currentUpgradeLevel - 1);
        player.addUpgPoints( cost );
        player.modifyUpgrade( upgradeNum, -1 );
        if( upgrade.getPrizeNum() == -1 )
            player.setFastRespawn(false);
        else
            m_botAction.specificPrize( name, -upgrade.getPrizeNum() );
        if( upgrade.getMaxLevel() == 1 )
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " has been removed from the " + Tools.shipName( shipNum ) + ", and " +
                    cost + " upgrade point" + (cost==1?"":"s") + " have been returned." );
        else
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " on the " + Tools.shipName( shipNum ) + " downgraded to level " + (currentUpgradeLevel - 1) + ".  " +
                    cost + " upgrade point" + (cost==1?"":"s") + " returned." );
        int points = player.getPointsSinceLastRank();
        player.addRankPoints( -points );
        m_botAction.sendPrivateMessage( name, "The process has returned you to the start of your rank, losing " + points + " rank point" + (points==1?"":"s") + "." );        
    }


    /**
     * Toggles between the player waiting in the safe spawn area and respawning in the center
     * after prizes are awarded.
     * @param name
     * @param msg
     */
    public void cmdWarp( String name, String msg ) {
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;
        if( player.waitInSpawn() )
            m_botAction.sendPrivateMessage( name, "You will not be warped out of the resupply area after being armed, and will need to attach to a Terrier or warp out manually." );
        else {
            m_botAction.sendPrivateMessage( name, "You will be warped out of the resupply area after being armed." );
            Player p = m_botAction.getPlayer(name);
            if( p != null ) {
                if( p.getYLocation() <= TOP_SAFE || p.getYLocation() >= BOT_SAFE )
                    player.doWarp(true);
            }
        }
    }
    
    
    /**
     * Shows terriers on an army, and their last observed locations.
     * @param name
     * @param msg
     */
    public void cmdTerr( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() == -1 ) {
            m_botAction.sendPrivateMessage( name, "Please !return to your army or !enlist first." );
            return;
        }
        Iterator i = m_botAction.getFreqPlayerIterator(p.getArmyID());
        if( !i.hasNext() ) {
            m_botAction.sendPrivateMessage( name, "No pilots detected in your army!" );
            return;
        }
        m_botAction.sendPrivateMessage(name, "Name of Terrier          Approx. location");
        while( i.hasNext() ) {
            Player terr = (Player)i.next();
            if( terr.getShipType() == Tools.Ship.TERRIER )
                m_botAction.sendPrivateMessage( name, Tools.formatString(terr.getPlayerName(), 25) + getPlayerLocation(terr, false) );
        }
    }
    
    
    /**
     * Shows last seen location of a given individual.  Wrapper.
     * @param name
     * @param msg
     */
    public void cmdWhereIs( String name, String msg ) {
        cmdWhereIs( name, msg, m_botAction.getOperatorList().isHighmod(name) );
    }

    
    /**
     * Shows last seen location of a given individual.
     * @param name
     * @param msg
     * @param isStaff True if player is staff/can !whereis anyone
     */
    public void cmdWhereIs( String name, String msg, boolean isStaff ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() == -1 && !isStaff ) {
            m_botAction.sendPrivateMessage( name, "You must !return or !enlist in an army first." );
            return;
        }
        Player p2;
        p2 = m_botAction.getPlayer( msg );
        if( p2 == null )
            p2 = m_botAction.getFuzzyPlayer( msg );
        if( p2 == null ) {
            m_botAction.sendPrivateMessage( name, "Player '" + msg + "' can not be located." );
            return;
        }
        if( p.getArmyID() != p2.getFrequency() && !isStaff ) {
            m_botAction.sendPrivateMessage( name, p2.getPlayerName() + " is not a member of your army!" );
            return;
        }
        m_botAction.sendPrivateMessage( name, p2.getPlayerName() + " last seen: " + getPlayerLocation( p2, isStaff ));
    }    


    /**
     * Assists an army other than your own, if help is needed.
     * @param name
     * @param msg
     */
    public void cmdAssist( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() == -1 ) {
            m_botAction.sendPrivateMessage( name, "You must !return or !enlist in an army first." );
            return;
        }
        int armyToAssist = -1;
        if( msg.equals("") ) {
            armyToAssist = p.getNaturalArmyID();
        } else {
            try {
                armyToAssist = Integer.parseInt( msg );
            } catch (NumberFormatException e) {
                m_botAction.sendPrivateMessage( name, "Which of the !armies would you like to assist?" );
                return;
            }
        }
        if( p.getNaturalArmyID() == armyToAssist ) {
            if( p.getNaturalArmyID() == p.getArmyID() ) {
                m_botAction.sendPrivateMessage( name, "You aren't assisting any army presently.  Use !assist armyID# to assist an army in need." );
            } else {
                m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), name.toUpperCase() + " has finished assistance and is returning to their army." );
                if( p.getShipNum() == 0 ) {
                    p.setAssist( -1 );
                } else {
                    p.setAssist( -1 );
                    m_botAction.setFreq(name, armyToAssist );
                    if( !p.isRespawning() )
                        p.prizeUpgrades();
                    for( DistensionArmy a : m_armies.values() )
                        a.recalculateFigures();
                }
                
                m_botAction.sendPrivateMessage( name, "You have returned to " + p.getArmyName() + ".");
            }
            return;
        }
        
        DistensionArmy assistArmy = m_armies.get( new Integer( armyToAssist ) );
        if( assistArmy == null ) {
            m_botAction.sendPrivateMessage( name, "Exactly which of them !armies you trying to help out there?" );
            return;
        }
        float armySizeWeight;
        float assistArmyStr = assistArmy.getTotalStrength();
        float currentArmyStr = p.getArmy().getTotalStrength();
        if( assistArmyStr <= 0 ) assistArmyStr = 1;
        if( currentArmyStr <= 0 ) currentArmyStr = 1;
        armySizeWeight = assistArmyStr / currentArmyStr;

        if( armySizeWeight < ASSIST_WEIGHT_IMBALANCE ) {
            m_botAction.sendPrivateMessage( name, "Now an honorary pilot of " + assistArmy.getName().toUpperCase() + ".  Use !assist to return to your army when you would like." );
            if( p.getShipNum() != 0 ) {
                if( System.currentTimeMillis() > lastAssistReward + ASSIST_REWARD_TIME ) {
                    lastAssistReward = System.currentTimeMillis();
                    int reward = p.getRank();
                    if( armySizeWeight < .5 )
                        reward = p.getRank() * 5;
                    else if( armySizeWeight < .6 )
                        reward = p.getRank() * 4;
                    else if( armySizeWeight < .7 )
                        reward = p.getRank() * 3;
                    else if( armySizeWeight < .8 )
                        reward = p.getRank() * 2;
                    m_botAction.sendPrivateMessage( name, "For your noble assistance, you also receive a " + reward + " RP bonus.", 1 );
                    p.addRankPoints(reward);
                }
                p.setAssist( armyToAssist );
                m_botAction.setFreq(name, armyToAssist );
                p.prizeUpgrades();
                for( DistensionArmy a : m_armies.values() )
                    a.recalculateFigures();
            } else {
                p.setAssist( armyToAssist );
            }
            m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), name.toUpperCase() + " is now assisting your army." );
        } else {
            m_botAction.sendPrivateMessage( name, "Not overly imbalanced -- consider flying a lower-rank ship to even the battle instead!" );
            if( DEBUG )
                m_botAction.sendPrivateMessage( name, "(" + armySizeWeight + " weight, need .9 or less)" );
        }
    }
    
    
    /**
     * Shows the team breakdown, including levels of players
     * @param name
     * @param msg
     */
    public void cmdTeam( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() == -1 ) {
            m_botAction.sendPrivateMessage( name, "You must !return or !enlist in an army first." );
            return;
        }
        
        ArrayList<Vector<String>>  team = getArmyData( p.getArmyID() );
        int players = 0;
        int totalStrength = 0;
        for(int i = 1; i < 9; i++ ) {
            int shipStrength = 0;
            int num = team.get(i).size();
            String text = "   ";
            for( int j = 0; j < team.get(i).size(); j++) {
                DistensionPlayer p2 = m_players.get( team.get(i).get(j) ); 
                text += p2.getName() + "(" + p2.getUpgradeLevel() + ")  ";
                players++;
                shipStrength += p2.getUpgradeLevel() + RANK_0_STRENGTH;
            }
            m_botAction.sendPrivateMessage(name, num + Tools.formatString( (" " + Tools.shipNameSlang(i) + (num==1 ? "":"s")), 8 )
                                                 + (shipStrength > 0 ? ("  " + shipStrength + " STR" + text) : "") );
            totalStrength += shipStrength;
        }
        
        m_botAction.sendPrivateMessage(name, players + " players, " + totalStrength + " total strength.  (STR = upgs + " + RANK_0_STRENGTH + ")" );
    }

    
    /**
     * Clears all of a player's mines, but only once per round.
     * @param name
     * @param msg
     */
    public void cmdClearMines( String name, String msg ) {                        
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        
        if( mineClearedPlayers.contains( name ) ) {
            m_botAction.sendPrivateMessage( name, "Well now... you've already cleared your mines once this battle.  Tough!" );
            return;
        }
        if( p.getShipNum() != Tools.Ship.SHARK && p.getShipNum() != Tools.Ship.LEVIATHAN && p.getShipNum() != Tools.Ship.WARBIRD ) {
            m_botAction.sendPrivateMessage( name, "You must be in a mine-laying ship in order for me to clear your mines.  Didn't think I'd have to tell you!" );
            return;
        }

        m_botAction.setShip( name, 1 );
        m_botAction.setShip( name, p.getShipNum() );
        p.prizeUpgrades();
        mineClearedPlayers.add( name );
        m_botAction.sendPrivateMessage( name, "Your mines have been cleared.  You may only do this once per battle." );
    }
    
    
    
    // HIGHMOD+ COMMANDS
    
    /**
     * Save all player data.  Sends arena msgs.
     * @param name
     * @param msg
     */
    public void cmdSaveData( String name, String msg ) {
        boolean autosave = ":autosave:".equals(name);
        if( !autosave )
            m_botAction.sendArenaMessage( "Saving player data ..." ,1 );
        int players = 0;
        int playersunsaved = 0;
        long starttime = System.currentTimeMillis();
        for( DistensionPlayer p : m_players.values() ) {
            if( autosave ) {
                p.saveCurrentShipToDB();
            } else {
                if( p.saveCurrentShipToDBNow() == false ) {
                    m_botAction.sendPrivateMessage( p.getName(), "Your data could not be saved.  Please use !dock to save your data.  Contact a mod with ?help if this does not work." );
                    playersunsaved++;
                } else {
                    players++;
                }
            }
        }
        long timeDiff = System.currentTimeMillis() - starttime;
        if( autosave )
            m_botAction.sendArenaMessage( "AUTOSAVE: All players saved to DB in " + timeDiff + "ms.");
        else {
            if( playersunsaved == 0 )
                m_botAction.sendArenaMessage( "Saved " + players + " players in " + timeDiff + "ms.  -" + name, 1 );
            else
                m_botAction.sendArenaMessage( "Saved " + players + " players in " + timeDiff + "ms.  " + playersunsaved + " players could not be saved.", 2 );
        }
    }


    /**
     * Kills the bot.
     * @param name
     * @param msg
     */
    public void cmdDie( String name, String msg ) {
        m_botAction.specAll();
        flagObjs.hideAllObjects();
        flagTimerObjs.hideAllObjects();
        m_botAction.setObjects();
        m_botAction.manuallySetObjects(flagObjs.getObjects());
        m_botAction.sendArenaMessage( "Distension going down for maintenance ...", 1 );
        try { Thread.sleep(500); } catch (Exception e) {};
        m_botAction.die();
    }


    /**
     * Bans a player from playing Distension.
     * @param name 
     * @param msg
     */
    public void cmdBan( String name, String msg ) {
        DistensionPlayer player = m_players.get( msg );
        if( player == null ) {
            m_botAction.sendPrivateMessage( name, "Can't find player '" + msg + "' in arena ... retrieving from DB." );
            player = new DistensionPlayer(name);
            player.getPlayerFromDB();
        }

        if( ! player.isBanned() ) {
            player.ban();
            m_botAction.sendPrivateMessage( name, "Player '" + msg + "' banned from playing Distension." );
        } else {
            m_botAction.sendPrivateMessage( name, "Player '" + msg + "' is already banned." );                
        }
    }


    /**
     * Unbans a player from playing Distension.
     * @param name 
     * @param msg
     */
    public void cmdUnban( String name, String msg ) {
        DistensionPlayer player = m_players.get( msg );
        if( player == null ) {
            m_botAction.sendPrivateMessage( name, "Can't find player '" + msg + "' in arena ... retrieving from DB." );
            player = new DistensionPlayer(name);
            player.getPlayerFromDB();
        }

        if( player.isBanned() ) {
            player.unban();
            m_botAction.sendPrivateMessage( name, "Player '" + msg + "' unbanned from playing Distension." );
        } else {
            m_botAction.sendPrivateMessage( name, "Player '" + msg + "' is not banned." );                
        }
    }


    /**
     * Checks info on a player by running !status as though from their computer,
     * but printing the results to the mod.
     * @param name
     * @param msg
     */
    public void cmdInfo( String name, String msg ) {
        cmdStatus( msg, null, name );
    }

    
    // BETA-ONLY COMMANDS
    
    /**
     * Grants a player credits (only for debug mode).
     * @param name 
     * @param msg
     */
    public void cmdGrant( String name, String msg ) {
        if( !DEBUG ) { 
            m_botAction.sendPrivateMessage( name, "This command disabled during normal operation." );
            return;
        }

        String[] args = msg.split(":");
        if( args.length != 2 ) {
            m_botAction.sendPrivateMessage( name, "Improper format.  !grant name:points" );
            return;
        }

        DistensionPlayer player = m_players.get( args[0] );
        if( player == null ) {
            m_botAction.sendPrivateMessage( name, "Can't find player '" + msg + "' in arena." );            
            return;
        }

        Integer points = 0;
        try {
            points = Integer.parseInt( args[1] ); 
        } catch (NumberFormatException e) {
            m_botAction.sendPrivateMessage( name, "Improper format.  !grant name:points" );            
            return;
        }

        m_botAction.sendPrivateMessage( name, "Granted " + points + "RP to " + args[0] + ".", 1 );
        m_botAction.sendPrivateMessage( args[0], "You have been granted " + points + "RP by " + name + ".", 1 );
        player.addRankPoints( points );
    }
    
    
    /**
     * Sends a message to all beta-testers.
     * @param name 
     * @param msg
     */
    public void cmdMsgBeta( String name, String msg ) {
        if( !DEBUG ) { 
            m_botAction.sendPrivateMessage( name, "This command disabled during normal operation." );
            return;
        }
        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fcName FROM tblDistensionPlayer WHERE 1" );
            int players = 0;
            while( r.next() ) {
                m_botAction.sendRemotePrivateMessage( r.getString("fcName"), "DISTENSION BETA TEST beginning shortly.  Please ?go #distension if you are willing to participate." );
                players++;
            }
            m_botAction.sendPrivateMessage( name, players + " players notified." );
        } catch (SQLException e) {}
    }


    // COMMAND ASSISTANCE METHODS

    /**
     * Based on provided coords, returns location of player as a String.
     * @return Last location recorded of player, as a String  
     */
    public String getPlayerLocation( Player p, boolean isStaff ) {
        int x = p.getXLocation() / 16;
        int y = p.getYLocation() / 16;
        String exact = "";
        if( isStaff )
            exact = "  (" + x + "," + y + ")";  
        if( x==0 && y==0 )
            return "Not yet spotted" + exact;
        if( y <= TOP_SAFE || y >= BOT_SAFE )
            return "Resupplying" + exact;
        if( y <= TOP_ROOF )
            return "Roofing Top ..." + exact;
        if( y <= TOP_FR )
            return "Top FR" + exact;
        if( y <= TOP_MID )
            return "Top Mid" + exact;
        if( y <= TOP_LOW )
            return "Top Lower" + exact;
        if( y >= BOT_LOW )
            return "Bottom Lower" + exact;
        if( y >= BOT_MID )
            return "Bottom Mid" + exact;
        if( y >= BOT_FR )
            return "Bottom FR" + exact;
        if( y >= BOT_ROOF )
            return "Roofing Bottom ..." + exact;        
        return "Neutral zone" + exact;
    }

    /**
     * Checks if flag time should be started.
     */
    public void checkFlagTimeStart() {
        Iterator <DistensionArmy>i = m_armies.values().iterator();
        boolean foundOne = false;
        while( i.hasNext() ) {
            DistensionArmy army = i.next();
            if( army.getPilotsInGame() > 0 ) {
                if( !foundOne )
                    foundOne = true;
                else {
                    // Two armies now have players; start game, or continue if already started
                    if( !flagTimeStarted ) {
                        m_botAction.sendArenaMessage( "A war is brewing ... " );
                        m_botAction.sendArenaMessage( "To win the battle, hold both flags for " + flagMinutesRequired + " minute" + (flagMinutesRequired == 1 ? "" : "s") + ".  Winning " + ( MAX_FLAGTIME_ROUNDS + 1) / 2 + " battles will win the sector conflict." );
                        m_botAction.scheduleTask( new StartRoundTask(), 60000 );
                        flagTimeStarted = true;
                    }
                    stopFlagTime = false;       // Cancel stopping; new opposing player entered
                    return;
                }
            }
        }
    }

    /**
     * Checks if flag time should be stopped.
     */
    public void checkFlagTimeStop() {
        if( !flagTimeStarted )
            return;
        Iterator <DistensionArmy>i = m_armies.values().iterator();
        boolean foundOne = false;
        while( i.hasNext() ) {
            DistensionArmy army = i.next();
            if( army.getPilotsInGame() > 0 ) {
                if( !foundOne )
                    foundOne = true;
                else {
                    // Two armies have players; do not stop game
                    return;
                }
            }
        }
        stopFlagTime = true;
        if( !foundOne ) {
            resetAllFlagData();
        }
    }
        
    /**
     * Resets all internal flag data, and issues a flag reset.
     */
    public void resetAllFlagData() {
        m_botAction.resetFlagGame();
        for( DistensionArmy army : m_armies.values() )
            army.removeAllFlags();
        flagOwner[0] = -1;
        flagOwner[1] = -1;
        flagObjs.showObject(LVZ_TOPBASE_EMPTY);
        flagObjs.showObject(LVZ_BOTBASE_EMPTY);
        m_botAction.manuallySetObjects( flagObjs.getObjects() );
    }

    /**
     * @return Enlistment bonus for a given default army, based on the size of other default armies.
     */
    public int calcEnlistmentBonus( DistensionArmy army ) {
        int pilots = 0;
        int numOtherArmies = 0;
        if( army == null || !army.isDefault() )
            return 0;
        int ourcount = army.getPilotsTotal();

        for( DistensionArmy a : m_armies.values() ) {
            if( a.getID() != army.getID() && a.isDefault() ) {
                pilots += a.getPilotsTotal();
                numOtherArmies++;
            }
        }
        
        if( pilots == 0 )
            return 0;
        pilots /= numOtherArmies;     // Figure average size of other armies
        pilots = pilots - ourcount;   // Figure how much under, if any, ours is

        // If avg # pilots is close enough, no bonus
        if( pilots < 3 )
            return 0;
        // Starting at a difference of 3, we give a 5 point bonus
        // So diff 3 = 5 bonus, 4 = 10, 5 = 15, 6 = 20, 7 = 25, up to a diff of 12 and a bonus of 50
        pilots -= 2;
        if( pilots > 10 )
            pilots = 10;
        pilots *= 5;
        return pilots;
    }

    /**
     * Collects names of players on an army into a Vector ArrayList by ship.
     * @param army Frequency to collect info on
     * @return Vector array containing player names on given freq
     */
    public ArrayList<Vector<String>> getArmyData( int army ) {
        ArrayList<Vector<String>> team = new ArrayList<Vector<String>>();
        // 8 ships plus potential spectators
        for( int i = 0; i < 9; i++ ) {
            team.add( new Vector<String>() );
        }
        Iterator i = m_botAction.getFreqPlayerIterator(army);
        while( i.hasNext() ) {
            Player p = (Player)i.next();
            team.get(p.getShipType()).add(p.getPlayerName());
        }
        return team;
    }




    // ***** INTERNAL CLASSES
    // Players, armies, prizing queue

    /**
     * Used to keep track of player data retreived from the DB, and update data
     * during play, to be later synch'd with the DB.
     */
    private class DistensionPlayer {
        private String name;    // Playername
        private int shipNum;    // Current ship: 1-8, 0 if docked/spectating; -1 if not logged in
        private int rank;       // Current rank (# upgrade points awarded);   -1 if docked/not logged in
        private int rankPoints; // Current rank points for ship;              -1 if docked/not logged in
        private int nextRank;   // Amount of points needed to increase rank;  -1 if docked/not logged in
        private int rankStart;  // Amount of points at which rank started;    -1 if docked/not logged in
        private int upgPoints;  // Current upgrade points available for ship; -1 if docked/not logged in
        private int armyID;     // 0-9998;                                    -1 if not logged in
        private int successiveKills;            // # successive kills (for unlocking weasel)
        private int[]     purchasedUpgrades;    // Upgrades purchased for current ship
        private boolean[] shipsAvail;           // Marks which ships are available
        private int[]     lastIDsKilled = { -1, -1, -1 };  // ID of last player killed (feeding protection)
        private long      respawnedAt;          // Time last respawned (spawn protection) 
        private int       spawnTicks;           // # queue "ticks" until spawn 
        private int       assistArmyID;         // ID of army player is assisting; -1 if not assisting
        private boolean   warnedForTK;          // True if they TKd / notified of penalty this match
        private boolean   banned;               // True if banned from playing
        private boolean   shipDataSaved;        // True if ship data on record equals ship data in DB
        private boolean   fastRespawn;          // True if player respawns at the head of the queue
        private boolean   isRespawning;         // True if player is currently in respawn process
        private boolean   waitInSpawn;          // True if player would like to warp out manually at respawn
        private boolean   specialRespawn;       // True if in spawn queue w/o actually spawning 

        public DistensionPlayer( String name ) {
            this.name = name;
            shipNum = -1;
            rank = -1;
            rankPoints = -1;
            nextRank = -1;
            upgPoints = -1;
            armyID = -1;
            successiveKills = 0;
            respawnedAt = -1;
            spawnTicks = 0;
            assistArmyID = -1;
            purchasedUpgrades = new int[NUM_UPGRADES];
            shipsAvail = new boolean[8];
            for( int i = 0; i < 8; i++ )
                shipsAvail[i] = false;
            warnedForTK = false;
            banned = false;
            shipDataSaved = false;
            fastRespawn = false;
            isRespawning = false;
            waitInSpawn = true;
            specialRespawn = false;
        }


        // DB METHODS

        /**
         * Creates a player record in the database.
         */
        public void addPlayerToDB() {
            try {
                m_botAction.SQLQueryAndClose( m_database, "INSERT INTO tblDistensionPlayer ( fcName , fnArmyID ) " +
                        "VALUES ('" + Tools.addSlashesToString( name ) + "', '" + armyID + "')" );
            } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); }
        }

        /**
         * Reads in data about the player from the database.
         * @return True if data is retrieved; false if it wasn't.
         */
        public boolean getPlayerFromDB() {
            try {
                boolean success = false;
                ResultSet r = m_botAction.SQLQuery( m_database, "SELECT * FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
                if( r.next() ) {
                    banned = r.getString( "fcBanned" ).equals( "y" );
                    if( banned == true )
                        return false;

                    armyID = r.getInt( "fnArmyID" );
                    for( int i = 0; i < 8; i++ )
                        shipsAvail[i] = ( r.getString( "fcShip" + (i + 1) ).equals( "y" ) ? true : false );
                    success = true;
                }
                m_botAction.SQLClose(r);
                return success;
            } catch (SQLException e ) {
                Tools.printStackTrace("Problem fetching returning player: " + name, e);
                return false;
            }
        }
        
        /**
         * Adds a ship as available on a player's record.  Wrapper for addShipToDB(int,int).
         * @param shipNum Ship # to make available
         */
        public void addShipToDB( int shipNumToAdd ) {
            addShipToDB(shipNumToAdd, 0);
        }

        /**
         * Adds a ship as available on a player's record, optionally giving them upgrade
         * points to begin with.
         * @param shipNum Ship # to make available
         */
        public void addShipToDB( int shipNumToAdd, int startingRankPoints ) {
            if( shipNumToAdd < 1 || shipNumToAdd > 8 )
                return;
            shipsAvail[ shipNumToAdd - 1 ] = true;
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcShip" + shipNumToAdd + "='y' WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
                if( startingRankPoints == 0 )
                    m_botAction.SQLQueryAndClose( m_database, "INSERT INTO tblDistensionShip ( fnPlayerID , fnShipNum ) VALUES ((SELECT fnID FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name ) + "'), " + shipNumToAdd + ")" );
                else
                    m_botAction.SQLQueryAndClose( m_database, "INSERT INTO tblDistensionShip ( fnPlayerID , fnShipNum , fnRankPoints ) VALUES ((SELECT fnID FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name ) + "'), " + shipNumToAdd + ", " + startingRankPoints + ")" );
            } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); }
        }

        /**
         * Saves current ship to DB immediately.  This will block the program thread
         * while the operation completes (usually very fast).
         */
        public boolean saveCurrentShipToDBNow() {
            return saveCurrentShipToDB( true );
        }

        /**
         * Saves current ship to DB via background query.
         */
        public void saveCurrentShipToDB() {
            saveCurrentShipToDB( false );
        }

        /**
         * Saves the current ship to the DB.  Only runs if the data has not already been saved.
         * 
         * @param saveNow True if the query should be foreground; false for background
         * @return True if data was saved successfully (background query always returns true)
         */
        public boolean saveCurrentShipToDB( boolean saveNow ) {
            if( shipNum < 1 || shipNum > 8 || shipDataSaved )
                return true;

            String query = "UPDATE tblDistensionShip ship, tblDistensionPlayer player SET ";

            query +=       "ship.fnRank=" + rank + "," +
            "ship.fnRankPoints=" + rankPoints + "," +
            "ship.fnUpgradePoints=" + upgPoints + "," +
            "ship.fnStat1=" + purchasedUpgrades[0];

            for( int i = 1; i < NUM_UPGRADES; i++ )
                query +=   ", ship.fnStat" + (i + 1) + "=" + purchasedUpgrades[i];

            query +=       " WHERE player.fcName = '" + Tools.addSlashesToString( name ) + "' AND " +
            "ship.fnPlayerID = player.fnID AND " +
            "ship.fnShipNum = " + shipNum;
            shipDataSaved = true;
            if( saveNow ) {
                try {
                    m_botAction.SQLQueryAndClose( m_database, query );
                } catch (SQLException e) {
                    shipDataSaved = false;
                    Tools.printLog("DistensionBot unable to save " + name + " to DB.");
                }
            } else {
                m_botAction.SQLBackgroundQuery( m_database, null, query );
            }
            return shipDataSaved;
        }

        /**
         * Retrieves the ship from DB.  Does not auto-set the player into the ship.
         * @return true if successful
         */
        public boolean getCurrentShipFromDB() {
            if( shipNum < 1 || shipNum > 8 )
                return false;
            try {
                String query = "SELECT * FROM tblDistensionShip ship, tblDistensionPlayer player " +
                "WHERE player.fcName = '" + Tools.addSlashesToString( name ) + "' AND " +
                "ship.fnPlayerID = player.fnID AND " +
                "ship.fnShipNum = '" + shipNum + "'";

                ResultSet r = m_botAction.SQLQuery( m_database, query );
                if( r == null )
                    return false;
                if( r.next() ) {
                    // Init rank level, rank points and upgrade points
                    rank = r.getInt( "fnRank" );
                    rankPoints = r.getInt( "fnRankPoints" );
                    upgPoints = r.getInt( "fnUpgradePoints" );
                    nextRank = m_shipGeneralData.get( shipNum ).getNextRankCost(rank); 
                    rankStart = m_shipGeneralData.get( shipNum ).getNextRankCost(rank-1); 

                    // Init all upgrades
                    for( int i = 0; i < NUM_UPGRADES; i++ )
                        purchasedUpgrades[i] = r.getInt( "fnStat" + (i + 1) );
                    shipDataSaved = true;
                } else {
                    shipDataSaved = false;
                }

                // Setup special (aka unusual) abilities
                Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum ).getAllUpgrades();
                for( int i = 0; i < NUM_UPGRADES; i++ )
                    if( upgrades.get( i ).getPrizeNum() == -1 && purchasedUpgrades[i] > 0 )
                        fastRespawn = true;

                m_botAction.SQLClose(r);
                return shipDataSaved;
            } catch (SQLException e ) {
                return false;
            }
        }


        // COMPLEX ACTIONS / SETTERS
        
        /**
         * Decrements spawn ticker, which must be decremented to 0 before player is
         * allowed back in.  During this time the player will see the familiar countdown.
         * This ensures weapons will continue firing after death, won't clear mines,
         * and provides a decent pause to break up the action. 
         * @return True if the player is ready to respawn 
         */
        public void doSpawnTick() {
            spawnTicks--;
            if( spawnTicks == 3 ) { // 3 ticks (1.5 seconds) before end, warp to safe and shipreset
                m_botAction.showObjectForPlayer(m_botAction.getPlayerID(name), LVZ_REARMING);
                doSafeWarp();
                m_botAction.shipReset(name);
            }
        }
        
        /**
         * Performs necessary actions to spawn the player, if ready.
         * @param specialRespawn True if this is a special (not real) spawn
         */
        public boolean doSpawn() {
            if( specialRespawn ) {
                specialRespawn = false;
                prizeUpgrades();
                return true;
            }
            if( spawnTicks > 0 )
                return false;
            if( !isRespawning )
                return true;
            doWarp(false);
            respawnedAt = System.currentTimeMillis();
            isRespawning = false;
            prizeUpgrades();
            m_botAction.hideObjectForPlayer(m_botAction.getPlayerID(name), LVZ_REARMING);
            return true;
        }

        /**
         * Prizes upgrades to player based on what has been purchased.
         */
        public void prizeUpgrades() {
            if( shipNum < 1 )
                return;
            Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum ).getAllUpgrades();
            int prize = -1;
            for( int i = 0; i < NUM_UPGRADES; i++ ) {
                prize = upgrades.get( i ).getPrizeNum();
                if( prize > 0 )
                    for( int j = 0; j < purchasedUpgrades[i]; j++ )
                        m_botAction.specificPrize( name, prize );
            }
        }
        
        /**
         * Prizes any special abilities that are reprized every 30 seconds.
         * 
         * Special prize #s that are prized with this method:
         * -2:  Prizing burst + warp for terriers
         * -3:  Full charge for spiders
         * -4:  Targetted EMP against all enemies -- recharged every 10 min or so, and not lost on death
         */
        public void prizeSpecialAbilities() {
            if( shipNum == 5 ) {
                // Regeneration ability; each level worth an additional 10% of prizing either port or burst
                //                       (+5% for each, up to a total of 50%)
                if( purchasedUpgrades[11] > 0 ) {
                    long portChance = Math.round(Math.random() * 20.0);
                    long burstChance = Math.round(Math.random() * 20.0);
                    if( purchasedUpgrades[11] >= portChance )
                        m_botAction.specificPrize( name, Tools.Prize.PORTAL );
                    if( purchasedUpgrades[11] >= burstChance )
                        m_botAction.specificPrize( name, Tools.Prize.BURST );                    
                }
                // EMP ability; re-enable every 20 ticks
                //if( purchasedUpgrades[13] > 0 )
            }
            if( shipNum == 3) {
                // Refueling ability; each level worth an additional 50%
                if( purchasedUpgrades[11] > 0 ) {
                    long fcChance = Math.round( Math.random() * 2.0 );
                    if( purchasedUpgrades[11] >= fcChance )
                        m_botAction.specificPrize( name, Tools.Prize.FULLCHARGE );                    
                }
                // Energy stream ability; each level worth an additional 5%
                if( purchasedUpgrades[12] > 0 ) {
                    long superChance = Math.round( Math.random() * 20.0 );
                    if( purchasedUpgrades[12] >= superChance )
                        m_botAction.specificPrize( name, Tools.Prize.SUPER );
                    
                }
            }
        }

        /**
         * Warps player to the appropriate spawning location (near a specific base).
         * Used after prizing. 
         * @param roundStart True if the rounnd is starting (ignore settings)
         */
        public void doWarp( boolean roundStart ) {
            if( waitInSpawn && !roundStart )
                return;
            int base;
            if( assistArmyID == -1 )
                base = armyID % 2;
            else
                base = assistArmyID % 2;
            Random r = new Random();
            int x = 512 + (r.nextInt(SPAWN_X_SPREAD) - (SPAWN_X_SPREAD / 2));
            int y;
            if( base == 0 )            
                y = SPAWN_BASE_0_Y_COORD + (r.nextInt(SPAWN_Y_SPREAD) - (SPAWN_Y_SPREAD / 2));
            else
                y = SPAWN_BASE_1_Y_COORD + (r.nextInt(SPAWN_Y_SPREAD) - (SPAWN_Y_SPREAD / 2));
            m_botAction.warpTo(name, x, y);
        }

        /**
         * Sets up player for respawning.
         */
        public void doSetupRespawn() {
            isRespawning = true;
            if( hasFastRespawn() ) {
                m_prizeQueue.addHighPriorityPlayer( this );                
            } else {
                m_prizeQueue.addPlayer( this );
            }
            spawnTicks = TICKS_BEFORE_SPAWN;
        }

        /**
         * Sets up player for respawning.
         */
        public void doSetupSpecialRespawn() {
            specialRespawn = true;
            m_prizeQueue.addPlayer( this );
        }

        /**
         * Warps player to the safe, no strings attached.
         */
        public void doSafeWarp() {
            int base;
            if( assistArmyID == -1 )
                base = armyID % 2;
            else
                base = assistArmyID % 2;
            if( base == 0 )
                m_botAction.warpTo(name, 512, SAFE_TOP_Y);
            else
                m_botAction.warpTo(name, 512, SAFE_BOTTOM_Y);
        }

        /**
         * Advances the player to the next rank.
         */
        public void doAdvanceRank() {
            rank++;
            upgPoints++;
            rankStart = nextRank;
            nextRank = m_shipGeneralData.get( shipNum ).getNextRankCost(rank);
            m_botAction.sendPrivateMessage(name, "-=(  RANK UP!  )=-  You have advanced to RANK " + rank + " in the " + Tools.shipName(shipNum) + ".", Tools.Sound.VICTORY_BELL );
            if( nextRank - rankPoints > 0 ) {
                m_botAction.sendPrivateMessage(name, "You will reach the next rank in " + ( nextRank - rankPoints )+ " rank points (total " + nextRank + "), and have earned 1 upgrade point to spend in the !armory." + ((upgPoints > 1) ? ("  (" + upgPoints + " avail).") : "") );            
            }else {
                // Advanced more than one rank; refire the method
                doAdvanceRank();
            }

            if( rank >= RANK_REQ_SHIP2 ) {
                if( shipsAvail[1] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Javelin.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "JAVELIN: The Javelin is a difficult ship to pilot, but one of the most potentially dangerous.  Users of the original Javelin model will feel right at home.  Our Javelins are extremely upgradeable, devastating other craft in high ranks.");
                    addShipToDB(2);
                }
            }
            if ( rank >= RANK_REQ_SHIP3 ) {
                if( shipsAvail[2] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Spider.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "SPIDER: The Spider is the mainstay support gunner of every army and the most critical element of base defense.  Upgraded spiders receive regular refuelings, wormhole plugging capabilities, and 10-second post-rearmament energy streams.");
                    addShipToDB(3);
                }
            } 
            if ( rank >= RANK_REQ_SHIP4 ) {
                if( shipsAvail[3] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the experimental Leviathan.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "LEVIATHAN: The Leviathan is an experimental craft, as yet untested.  It is unmaneuvarable but capable of great speeds -- in reverse.  Its guns are formidable, its bombs can cripple an entire base, but it is a difficult ship to master.");
                    addShipToDB(4);
                }
            }
            if ( rank >= RANK_REQ_SHIP5 ) {
                if( shipsAvail[4] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Terrier.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "TERRIER: The Terrier is our most important ship, providing a point of support into the fray.  Also the most rapidly-advancing craft, advanced Terriers enjoy rearmament preference and resupply of weapons and wormhole kits.");
                    addShipToDB(5);
                }
            }
            // BETA ONLY
            if ( rank >= RANK_REQ_SHIP6 ) {
                if( shipsAvail[5] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Weasel.  One has been requisitioned for your use, and is now waiting in your !hangar.  (UNLOCKED BY RANK IN BETA ONLY)");
                    addShipToDB(6);
                }
            }
            if ( rank >= RANK_REQ_SHIP7 ) {
                if( shipsAvail[6] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Lancaster.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "LANCASTER: The Lancaster is an unusual ship with a host of surprises onboard.  Pilots can upgrade its most basic components rapidly.  The Firebloom and the Lanc's evasive-bombing capability make this a fantastic choice for advanced pilots.");
                    addShipToDB(7);
                }
            } 
            if ( rank >= RANK_REQ_SHIP8 ) {
                if( shipsAvail[7] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Shark.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "SHARK: The Shark is piloted by our most clever and resourceful pilots.  Unsung heroes of the army, Sharks are both our main line of defense and leaders of every assault.  Advanced Sharks enjoy light gun capabilities and a cloaking device.");
                    addShipToDB(8);
                }
            }
        }

        /**
         * Adds # rank points to total rank point amt.
         * @param points Amt to add
         */
        public void addRankPoints( int points ) {
            if( shipNum < 1 )
                return;
            if( DEBUG )
                points = (int)((float)points * DEBUG_MULTIPLIER); 
            rankPoints += points;
            if( rankPoints >= nextRank )
                doAdvanceRank();
            shipDataSaved = false;
        }

        /**
         * Adds # upgrade points to total rank point amt.
         * @param points Amt to add
         */
        public void addUpgPoints( int points ) {
            upgPoints += points;
            shipDataSaved = false;
        }

        /**
         * Modifies the value of a particular upgrade.
         */
        public void modifyUpgrade( int upgrade, int amt ) {
            if( upgrade < 0 || upgrade > NUM_UPGRADES)
                return;
            if( purchasedUpgrades[upgrade] + amt < 0 )
                return;
            purchasedUpgrades[upgrade] += amt;
            if( assistArmyID == -1 )
                m_armies.get(armyID).recalculateFigures();
            else
                m_armies.get(assistArmyID).recalculateFigures();
            shipDataSaved = false;
        }

        /**
         * Sets the current ship player is using.
         * @param shipNum # of ship (1-8)
         */
        public void setShipNum( int shipNum ) {
            if( shipNum < 1 ) {
                rank = -1;
                rankPoints = -1;
                rankStart = -1;
                nextRank = -1;
                upgPoints = -1;
                if( this.shipNum > 0 )
                    m_botAction.specWithoutLock( name );
            }
            this.shipNum = shipNum;
            isRespawning = false;
            successiveKills = 0;
            if( shipNum == 3 || shipNum == 5 )
                m_specialAbilityPrizer.addPlayer(this);
            else
                m_specialAbilityPrizer.removePlayer(this);
        }

        /**
         * Puts player in the currently-set ship and sets them as having respawned.
         */
        public void putInCurrentShip() {
            m_botAction.setShip( name, shipNum );
            m_botAction.setFreq( name, getArmyID() );
            isRespawning = false;
            respawnedAt = System.currentTimeMillis();
        }

        /**
         * Bans the player from playing Distension.
         */
        public void ban() {
            banned = true;
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcBanned='y' WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
            } catch (SQLException e ) { 
                Tools.printLog( "Error banning player " + name );
            }
            saveCurrentShipToDB();
            m_botAction.sendSmartPrivateMessage(name, "You have been forcefully discharged from your army, and are now considered a civilian.  You may no longer play Distension." );
            m_botAction.sendUnfilteredPrivateMessage( name, "*kill 1" );
        }

        /**
         * Unbans the player.
         */
        public void unban() {
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcBanned='n' WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
            } catch (SQLException e ) { 
                Tools.printLog( "Error banning player " + name );
            }
            m_botAction.sendSmartPrivateMessage(name, "You are no longer banned in Distension." );
            banned = false;
        }

        /**
         * Increments successive kills.
         */
        public boolean addSuccessiveKill( ) {
            
            successiveKills++;            

            if( successiveKills == 5 ) {
                int award = 3;
                if( rank > 1 )
                    award = rank * 3;
                m_botAction.sendPrivateMessage(name, "Streak!  (" + award + " RP bonus.)", 19 );
                addRankPoints(award);
            } else if( successiveKills == 10 ) {
                int award = 5;
                if( rank > 1 )
                    award = rank * 5;
                m_botAction.sendPrivateMessage(name, "ON FIRE!  (" + award + " RP bonus.)", 20 );
                addRankPoints(award);
            } else if( successiveKills == 15 ) {
                int award = 7;
                if( rank > 1 )
                    award = rank * 7;
                m_botAction.sendPrivateMessage(name, "UNSTOPPABLE!  (" + award + " RP bonus.)", Tools.Sound.VIOLENT_CONTENT );
                addRankPoints(award);
            } else if( successiveKills == 20 ) {
                if( shipsAvail[5] == false ) {                                        
                    String query = "SELECT * FROM tblDistensionShip ship, tblDistensionPlayer player " +
                    "WHERE player.fcName = '" + Tools.addSlashesToString( name ) + "' AND " +
                    "ship.fnPlayerID = player.fnID AND ship.fnShipNum = '6'";
                    
                    try {
                        ResultSet r = m_botAction.SQLQuery( m_database, query );
                        if( r.next() ) {
                            m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcShip6='y' WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
                            m_botAction.sendPrivateMessage(name, "For 20 successive kills, your Weasel has been returned to the hangar!");
                        } else {
                            m_botAction.sendPrivateMessage(name, "AWARD FOR MASTERFUL DOGFIGHTING.  You are quite the pilot, and have proven yourself capable of joining our stealth operations.  The Weasel is now available in your !hangar." );
                            m_botAction.sendPrivateMessage(name, "WEASEL: The Weasel heads Covert Operations, providing scout reconnaissance to the rest of the army.  Its small size and cloaking allows it a freedom no others have.  Our newest Weasels now have the ability to cut off pursuit instantly!");
                            addShipToDB(6);                            
                        }
                        return true;
                    } catch (SQLException e ) { return false; }                    
                } else {
                    int award = 10;
                    if( rank > 1 )
                        award = rank * 7;
                    m_botAction.sendPrivateMessage(name, "INCONCEIVABLE!  (" + award + " RP bonus.)", Tools.Sound.INCONCEIVABLE );
                    addRankPoints(award);                    
                }
            } else if( successiveKills == 50 ) {
                int award = 15;
                if( rank > 1 )
                    award = rank * 15;
                m_botAction.sendPrivateMessage(name, "YOU'RE PROBABLY CHEATING!  (" + award + " RP bonus.)", Tools.Sound.SCREAM );                
            } else if( successiveKills == 99 ) {
                int award = 20;
                if( rank > 1 )
                    award = rank * 30;
                m_botAction.sendPrivateMessage(name, "99 KILLS -- ... ORGASMIC !!  (" + award + " RP bonus.)", Tools.Sound.ORGASM_DO_NOT_USE );                
            }
            return false;
        }
        
        
        // BASIC SETTERS

        /**
         * Sets the army of the player.
         * @param armyID ID of the army
         */
        public void setArmy( int armyID ) {
            this.armyID = armyID;
        }

        /**
         * Sets player as having been warned for TK.
         */
        public void setWarnedForTK() {
            warnedForTK = true;
        }

        /**
         * Sets whether or not player respawns at the head of the prize queue.
         */
        public void setFastRespawn( boolean value ) {
            fastRespawn = value;
        }
        
        /**
         * Sets successive kills to 0.
         */
        public void clearSuccessiveKills() {
            successiveKills = 0;
        }
        
        /**
         * Toggles wait status (whether or not player will warp out into spawn).
         */
        public boolean waitInSpawn() {
            waitInSpawn = !waitInSpawn; 
            return waitInSpawn;
        }

        /**
         * Sets the player as assisting the army ID provided.  -1 to disable.
         * @param newArmyID ID of army player is assisting; -1 to disable assisting
         */
        public void setAssist( int newArmyID ) {
            assistArmyID = newArmyID;
        }


        // GETTERS

        /**
         * @return Returns the name.
         */
        public String getName() {
            return name;
        }

        /**
         * @return Returns the ship number this player is currently playing, 1-8.  0 for spec, -1 for not logged in.
         */
        public int getShipNum() {
            return shipNum;
        }

        /**
         * @return Pilot rank in ship (essentially number of upgrade points awarded)
         */
        public int getRank() {
            return rank;
        }

        /**
         * @return Returns upgrade level of ship (combined levels of all upgrades).
         */
        public int getUpgradeLevel() {
            int upgLevel = 0;
            for( int i = 0; i < NUM_UPGRADES; i++ )
                upgLevel += purchasedUpgrades[i];
            return upgLevel;
        }

        /**
         * @return Returns current rank points in the present ship; -1 if not in a ship
         */
        public int getRankPoints() {
            return rankPoints;
        }

        /**
         * @return Returns the rank point amount needed for the next rank; -1 if not in a ship
         */
        public int getNextRankPoints() {
            return nextRank;
        }

        /**
         * @return Returns rank point amount at which this rank started; -1 if not in a ship
         */
        public int getRankPointStart() {
            return rankStart;
        }

        /**
         * @return Returns points needed to the next rank
         */
        public int getPointsToNextRank() {
            return nextRank - rankPoints;
        }

        /**
         * Use in combination w/ getNextRankPointsProgressive()
         * @return Returns points earned since last rank was earned (never negative) 
         */
        public int getPointsSinceLastRank() {
            if( rankPoints >= rankStart )
                return rankPoints - rankStart;
            else
                return 0;
        }

        /**
         * Use in combination w/ getPointsSinceLastRank()
         * @return Returns point amount needed for next rank, minus the amount needed for the last rank 
         */
        public int getNextRankPointsProgressive() {
            return nextRank - rankStart;
        }

        /**
         * @return Returns # of upgrade points available for present ship; -1 if not in a ship
         */
        public int getUpgradePoints() {
            return upgPoints;
        }

        /**
         * @return Returns army ID (same as frequency).
         */
        public int getArmyID() {
            if( assistArmyID == -1 )
                return armyID;
            else
                return assistArmyID;
        }

        /**
         * @return Returns natural army ID -- the player's true army regardless of assists.
         */
        public int getNaturalArmyID() {
            return armyID;
        }

        /**
         * @return Returns DistensionArmy the player to which the player is joined.
         */
        public DistensionArmy getArmy() {
            if( assistArmyID == -1 )
                return m_armies.get( new Integer( armyID ) );
            else
                return m_armies.get( new Integer( assistArmyID ) );
        }

        /**
         * @return Returns army name.
         */
        public String getArmyName() {
            int id;
            if( assistArmyID == -1 )
                id = armyID;
            else
                id = assistArmyID;
            DistensionArmy army = m_armies.get( new Integer( id ) );
            if( army == null ) {
                army = new DistensionArmy( new Integer( id ) );
                m_armies.put( new Integer( id ), army );
            }
            return army.getName();
        }

        /**
         * @return True if the given ship is available (1 to 8).
         */
        public boolean shipIsAvailable( int shipNumToCheck ) {
            if( shipNumToCheck < 1 || shipNumToCheck > 8 )
                return false;
            return shipsAvail[shipNumToCheck - 1];
        }

        /**
         * @param upgrade Upgrade of which to return level
         * @return Level of purchased upgrade
         */
        public int getPurchasedUpgrade( int upgrade ) {
            if( upgrade < 0 || upgrade > NUM_UPGRADES)
                return -1;
            return purchasedUpgrades[upgrade];
        }

        /**
         * @return Returns the purchasedUpgrades.
         */
        public int[] getPurchasedUpgrades() {
            return purchasedUpgrades;
        }

        /**
         * @return True if player has been warned for teamkilling this session.
         */
        public boolean wasWarnedForTK() {
            return warnedForTK;
        }

        /**
         * @return True if player has been banned from playing Distension.
         */
        public boolean isBanned() {
            return banned;
        }

        /**
         * @return True if player is currently respawning and should not be warped.
         */
        public boolean isRespawning() {
            return isRespawning;
        }

        /**
         * @return True if player should go to head of respawn queue
         */
        public boolean hasFastRespawn() {
            return fastRespawn;
        }
        
        /**
         * @return True if player is a support ship (5 or 8)
         */
        public boolean isSupportShip() {
            return (shipNum == 5 || shipNum == 8 );
        }
        
        /**
         * @return True if player is currently assisting an army and is not on their original one.
         */
        public boolean isAssisting() {
            return assistArmyID != -1;
        }
        
        /**
         * Returns # of kills recently made of a player, and cycles IDs of players
         * killed appropriately.
         */
        public int getRepeatKillAmount( int killedPlayerID ) {
            int repeats = 0;
            for( int i = 0; i<3; i++ ) {
                if( killedPlayerID == lastIDsKilled[i] )
                    repeats++;                
            }
            // Cycle through
            lastIDsKilled[2] = lastIDsKilled[1]; 
            lastIDsKilled[1] = lastIDsKilled[0]; 
            lastIDsKilled[0] = killedPlayerID; 
            return repeats;
        }
        
        /**
         * Checks if player spawned in last 2 seconds.
         */
        public boolean justRespawned() {
            // 3 second spawn protection
            if( System.currentTimeMillis() - respawnedAt > RESPAWN_SAFETY_TIME )
                return false;
            else
                return true;
        }
    }




    /**
     * Used to keep track of players on a given army, and info retrieved from the DB
     * about the army.
     */
    private class DistensionArmy {
        int armyID;                     // ID, same as frequency
        String armyName;                // Name of army
        String password;
        int totalStrength;              // Combined levels of all pilots of the army
        int flagsOwned;                 // # flags currently owned
        int pilotsInGame;               // # pilots playing right now
        int pilotsTotal;                // # pilots in entire army
        boolean isDefault;              // True if army is available by default (not user-created)
        boolean isPrivate;              // True if army can't be seen on !armies screen

        /**
         * Creates record for an army, given an ID.
         * @param armyID ID of the army
         */
        public DistensionArmy( int armyID ) {
            this.armyID = armyID;
            totalStrength = 0;
            flagsOwned = 0;
            try {
                ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fcArmyName, fnNumPilots, fcDefaultArmy, fcPrivateArmy, fcPassword FROM tblDistensionArmy WHERE fnArmyID = '"+ armyID + "'" );
                if( r.next() ) {
                    armyName = r.getString( "fcArmyName" );
                    pilotsTotal = r.getInt( "fnNumPilots" ); 
                    isDefault = r.getString( "fcDefaultArmy" ).equals("y");
                    isPrivate = r.getString( "fcPrivateArmy" ).equals("y");
                    password = r.getString( "fcPassword" );
                }
            } catch( SQLException e ) {
                m_botAction.sendArenaMessage("Problem loading Army " + armyID );
            }
        }

        // SETTERS

        public void adjustPilotsTotal( int value ) {
            pilotsTotal += value;
            if( pilotsTotal < 0 )
                pilotsTotal = 0;
            m_botAction.SQLBackgroundQuery( m_database, null, "UPDATE tblDistensionArmy SET fnNumPilots=" + pilotsTotal + " WHERE fnArmyID=" + armyID );
        }
        
        /**
         * Called when a player has left, been added, or changed ships to recalculate
         * pilots in game and army strength.
         */
        public void recalculateFigures() {
            int pilots = 0;
            int strength = 0;
            for( DistensionPlayer p : m_players.values() ) {
                if( p.getArmyID() == armyID && p.getShipNum() > 0 ) {
                    pilots++;
                    strength += p.getUpgradeLevel() + RANK_0_STRENGTH;
                }
            }
            pilotsInGame = pilots;
            totalStrength = strength;
        }

        public void adjustFlags( int value ) {
            flagsOwned += value;
            if( flagsOwned < 0 )
                flagsOwned = 0;
        }

        public void removeAllFlags() {
            flagsOwned = 0;
        }

        // GETTERS

        public String getName() {
            return armyName;
        }

        public int getID() {
            return armyID;
        }

        public int getTotalStrength() {
            return totalStrength;
        }

        public int getNumFlagsOwned() {
            return flagsOwned;
        }

        public int getPilotsInGame() {
            return pilotsInGame;
        }

        public int getPilotsTotal() {
            return pilotsTotal;
        }

        public String getPassword() {
            return password;
        }

        /**
         * @return True if army is available by default, and gives enlistment bonuses
         */
        public boolean isDefault() {
            return isDefault;
        }

        /**
         * @return True if army is not publicly-known (shown on lists)
         */
        public boolean isPrivate() {
            return isPrivate;
        }
    }


    // ***** GENERIC INTERNAL DATA CLASSES (for static info defined in price setup)

    /**
     * Generic (not player specific) class used for holding basic info on a ship --
     * its cost (if applicable), data on upgrades available, a base cost for
     * pilot ranks, and a factor by which that cost increases.  Costs are determined
     * as follows:
     * 
     *      Rank 1: baseRankCost
     *      Rank 2: Rank 1 cost * costFactor
     *      Rank 3: Rank 2 cost * costFactor ... and so on.
     *      
     *      If baseRankCost == 10 & costFactor == 1.8 ...
     *      
     *      Rank 1: 10
     *      Rank 2: 18 more
     *      Rank 3: 32 more
     *      Rank 4: 58 more
     *      Rank 5: 104 more
     *      
     *      10,1.3: 106@10(10) 1461@20(73)  20141@30(671) 277662@40(6941)   3.8M@50(76K)
     *      20,1.15: 70@10(7)   284@20(14)
     *      30,1.2: 154@10(15)  953@20(47)   5903@30(196)  36555@40(913)  226344@50(4526)
     *      40,1.15 140         569          2303
     *      30(1-10,1.5)1153@10(115) 
     *      20(1-10,1.6)1374@10(137)
     *      WINRAR SO FAR:
     *      10(1-10,1.6) 687@10(68)
     *      15(1-10,1.6)1030@10(103)
     *      18(1-10,1.6)1236@10(123)
     *      10(>10,1.15)       2780@20()
     *      15(>10,1.15)       4170@20(208) 16870@30(562)  68250@40(1706) 276111@50(5522)
     */
    private class ShipProfile {
        int rankUnlockedAt;               // Rank at which the ship becomes available; -1 if special condition
        Vector <ShipUpgrade>upgrades;     // Vector of upgrades available for this ship
        int baseRankCost;                 // Points needed to reach pilot rank 1 in this ship;
        //   subsequent levels are determined by the scaling factor.

        public ShipProfile( int rankUnlockedAt, int baseRankCost) {
            this.rankUnlockedAt = rankUnlockedAt;
            this.baseRankCost = baseRankCost;
            upgrades = new Vector<ShipUpgrade>(NUM_UPGRADES);
        }

        public void addUpgrade( ShipUpgrade upgrade ) {
            upgrades.add( upgrade );
        }

        public ShipUpgrade getUpgrade( int upgradeNum ) {
            if( upgradeNum < 0 || upgradeNum > NUM_UPGRADES )
                return null;
            else
                return upgrades.get( upgradeNum );
        }

        public Vector<ShipUpgrade> getAllUpgrades() {
            return upgrades;
        }

        public int getRankUnlockedAt() {
            return rankUnlockedAt;
        }

        /**
         * Recursion, bitches.  Used to get point amount needed for next rank.
         * @param currentRank Current rank of player
         * @return Amount of points needed for next rank
         */
        public int getNextRankCost( int currentRank ) {
            if( currentRank < 0 )
                return 0;
            if( currentRank == 0 )
                return baseRankCost;

            int amt = baseRankCost;
            for(int i = 0; i < currentRank; i++ ) {
                if( i < 10 )
                    amt *= EARLY_RANK_FACTOR;
                else
                    amt *= NORMAL_RANK_FACTOR;
            }
            return getNextRankCost( currentRank - 1 ) + amt;
        }
    }



    /**
     * Generic (not player specific) class used for holding info on upgrades for
     * a specific ship -- name of the upgrade, prize number associated with it,
     * cost, and max level.  The starting level of all upgrades is 0.
     * The base upgrades generally range from level 0-10, with major exceptions.
     */
    private class ShipUpgrade {
        String name;                // Upgrade name
        int prizeNum;               // Prize associated with the upgrade; negative numbers for special functions
        int cost;                   // Cost of the upgrade, in upgrade points.  -1 if array-defined.
        int[] costDefines;          // (For precise cost definitions not based on standard formula)
        int numUpgrades;            // # levels of this upgrade.  1=one-time upgrade; 0=not available; -1=NA and don't list
        int rankRequired;           // Rank required to utilize this upgrade
        int[] rankDefines;          // (For implementing rank requirements for each level of upgrade) 

        public ShipUpgrade( String name, int prizeNum, int cost, int rankRequired, int numUpgrades ) {
            this.name = name;
            this.prizeNum = prizeNum;
            this.cost = cost;
            this.rankRequired = rankRequired;
            this.numUpgrades = numUpgrades;
        }

        public ShipUpgrade( String name, int prizeNum, int[] costDefines, int rankRequired, int numUpgrades ) {
            if( numUpgrades != costDefines.length ) {
                Tools.printLog("ERROR: Upgrade '" + name + "' # of upgrades does not match cost define" );
                return;
            }                
            this.name = name;
            this.prizeNum = prizeNum;
            this.cost = -1;
            this.costDefines = costDefines;
            this.rankRequired = rankRequired;
            this.numUpgrades = numUpgrades;
        }

        public ShipUpgrade( String name, int prizeNum, int cost, int[] rankDefines, int numUpgrades ) {
            if( numUpgrades != rankDefines.length ) {
                Tools.printLog("ERROR: Upgrade '" + name + "' # of upgrades does not match rank define" );
                return;
            }                
            this.name = name;
            this.prizeNum = prizeNum;
            this.cost = cost;
            this.rankRequired = -1;
            this.rankDefines = rankDefines;
            this.numUpgrades = numUpgrades;
        }

        public ShipUpgrade( String name, int prizeNum, int[] costDefines, int[] rankDefines, int numUpgrades ) {
            if( numUpgrades != costDefines.length ) {
                Tools.printLog("ERROR: Upgrade '" + name + "' # of upgrades does not match cost define" );
                return;
            } else if( numUpgrades != rankDefines.length ) {
                Tools.printLog("ERROR: Upgrade '" + name + "' # of upgrades does not match rank define" );
                return;
            }                
            this.name = name;
            this.prizeNum = prizeNum;
            this.numUpgrades = numUpgrades;
            this.costDefines = costDefines;
            this.rankDefines = rankDefines;
            this.cost = -1;
            this.rankRequired = -1;
        }        


        /**
         * @return Returns the cost.  -1 if array-defined.
         */
        public int getCost() {
            return cost;
        }

        /**
         * @return Returns max level of the upgrade; 1=one time upgrade; -1=not available
         */
        public int getMaxLevel() {
            return numUpgrades;
        }

        /**
         * @return Returns the name of the upgrade.
         */
        public String getName() {
            return name;
        }

        /**
         * @return Returns the prizeNum.
         */
        public int getPrizeNum() {
            return prizeNum;
        }

        /**
         * @return Returns the prizeNum.
         */
        public int getRankRequired( int currentLevel ) {
            if( rankRequired == -1 )
                return rankDefines[ currentLevel ];
            else
                return rankRequired;
        }

        /**
         * @return Cost of upgrading to next level of upgrade
         */
        public int getCostDefine( int currentLevel ) {
            if( cost == -1 )
                return costDefines[ currentLevel ];
            else
                return cost;
        }
    }


    /**
     * Prize queuer, for preventing bot lockups.
     */
    private class PrizeQueue extends TimerTask {
        LinkedList <DistensionPlayer>players = new LinkedList<DistensionPlayer>();

        /**
         * Adds a player to the end of the prizing queue.
         * @param p Player to add 
         */
        public void addPlayer( DistensionPlayer p ) {
            players.addLast(p);
        }

        /**
         * Adds a player to the head of the prizing queue.  For terrs and sharks
         * with the ability.
         * @param p Player to add
         */
        public void addHighPriorityPlayer( DistensionPlayer p ) {
            players.addFirst(p);
        }

        public void run() {
            if( players.isEmpty() )
                return;
            for( DistensionPlayer p : players )
                p.doSpawnTick();
            DistensionPlayer currentPlayer = players.peek();
            if( currentPlayer.doSpawn() )
                players.removeFirst();
        }
    }


    /**
     * Special ability prizer.
     */
    private class SpecialAbilityTask extends TimerTask {
        LinkedList <DistensionPlayer>players = new LinkedList<DistensionPlayer>();  

        /**
         * Adds a player to be checked for random special prizing every 30 seconds. 
         * @param p Player to add
         */
        public void addPlayer( DistensionPlayer p ) {
            players.add(p);
        }
        
        public void removePlayer( DistensionPlayer p ) {
            players.remove(p);
        }
        
        /**
         * Checks all players added for special prizing.
         */
        public void run() {
            for( DistensionPlayer p : players )
                p.prizeSpecialAbilities();            
        }
    }



    // ***** FLAG TIMER METHODS

    /**
     * Starts a game of flag time mode.
     */
    private void doStartRound() {
        if(!flagTimeStarted)
            return;

        try {
            flagTimer.endBattle();
            m_botAction.cancelTask(flagTimer);
        } catch (Exception e ) {
        }

        mineClearedPlayers.clear();
        flagTimer = new FlagCountTask();
        m_botAction.showObject(LVZ_ROUND_COUNTDOWN); // Turns on countdown lvz
        m_botAction.hideObject(LVZ_INTERMISSION);    // Turns off intermission lvz
        m_botAction.scheduleTaskAtFixedRate( flagTimer, 100, 1000);
    }


    /**
     * Displays rules and pauses for intermission.
     */
    private void doIntermission() {
        if(!flagTimeStarted || stopFlagTime )
            return;

        int roundNum = freq0Score + freq1Score + 1;

        String roundTitle = "";
        switch( roundNum ) {
            case 1:
                m_botAction.sendArenaMessage( "To win the battle, hold both flags for " + flagMinutesRequired + " minute" + (flagMinutesRequired == 1 ? "" : "s") + "  Winning " + ( MAX_FLAGTIME_ROUNDS + 1) / 2 + " battles will win the sector conflict." );
                roundTitle = "A new conflict";
                break;
            case MAX_FLAGTIME_ROUNDS:
                roundTitle = "Final Battle";
                break;
            default:
                roundTitle = "Battle " + roundNum;
        }

        m_botAction.sendArenaMessage( roundTitle + " begins in " + getTimeString( INTERMISSION_SECS ) + ".  (Battles won: " + freq0Score + " to " + freq1Score + ")" );

        m_botAction.cancelTask(startTimer);

        startTimer = new StartRoundTask();
        m_botAction.scheduleTask( startTimer, INTERMISSION_SECS * 1000 );
    }


    /**
     * Ends a battle, awarding prizes.
     * After, sets up an intermission, followed by a new battle.
     */
    private void doEndRound( ) {
        if( !flagTimeStarted || flagTimer == null )
            return;

        //HashSet <String>MVPs = new HashSet<String>();
        boolean gameOver     = false;
        int winningArmyID    = flagTimer.getHoldingFreq();
        int maxScore         = (MAX_FLAGTIME_ROUNDS + 1) / 2;  // Score needed to win
        int secs             = flagTimer.getTotalSecs();
        int minsToWin        = flagTimer.getTotalSecs() / 60;
        
        int opposingStrengthAvg = 1;
        int friendlyStrengthAvg = 1;
        float armyDiffWeight;
        HashMap <Integer,Integer>armyStrengths = flagTimer.getArmyStrengthSnapshots();

        if( winningArmyID == 0 || winningArmyID == 1 ) {
            if( winningArmyID == 0 )
                freq0Score++;
            else
                freq1Score++;

            if( freq0Score >= maxScore || freq1Score >= maxScore ) {
                gameOver = true;
            } else {
                int roundNum = freq0Score + freq1Score;
                m_botAction.sendArenaMessage( "BATTLE " + roundNum + " ENDED: " + m_armies.get(winningArmyID).getName() + " gains control of the sector after " + getTimeString( flagTimer.getTotalSecs() ) +
                        "  Battles won: " + freq0Score + " to " + freq1Score, Tools.Sound.VICTORY_BELL );
            }
        } else {
            m_botAction.sendArenaMessage( "END BATTLE: " + m_armies.get(winningArmyID).getName() + " gains control of the sector after " + getTimeString( flagTimer.getTotalSecs() ), Tools.Sound.VICTORY_BELL );
        }
        
        // Special case: if teams are imbalanced at end, and people do not !assist so they
        //               will win the round, NO points are earned!
        float winnerStrCurrent;
        float loserStrCurrent;
        if( winningArmyID == 0 ) {
            winnerStrCurrent = m_armies.get(0).getTotalStrength();
            loserStrCurrent = m_armies.get(1).getTotalStrength();
        } else {
            winnerStrCurrent = m_armies.get(1).getTotalStrength();
            loserStrCurrent = m_armies.get(0).getTotalStrength();            
        }
        if( winnerStrCurrent <= 0 ) winnerStrCurrent = 1;
        if( loserStrCurrent <= 0 ) loserStrCurrent = 1;

        if( loserStrCurrent / winnerStrCurrent < ASSIST_WEIGHT_IMBALANCE - 0.1f ) {
            m_botAction.sendArenaMessage( "AVARICE DETECTED.  Armies too imbalanced; NO POINTS AWARDED!", 1 );
            return;
        }


        for( Integer i : armyStrengths.keySet() ) {
            if( i == winningArmyID )
                friendlyStrengthAvg = armyStrengths.get( i ) / minsToWin;
            else
                opposingStrengthAvg += armyStrengths.get( i ) / minsToWin;
        }

        if( friendlyStrengthAvg == 0 )
            friendlyStrengthAvg = 1;
        if( opposingStrengthAvg == 0 )
            opposingStrengthAvg = 1;
        armyDiffWeight = ((float)opposingStrengthAvg / (float)friendlyStrengthAvg);
        if( armyDiffWeight > 3.0f ) {
            armyDiffWeight = 3.0f;
        } else if( armyDiffWeight < 0.3f ) {
            armyDiffWeight = 0.3f;            
        }
        
        // Points to be divided up by army
        float totalPoints = (float)(minsToWin / 2.0f) * (float)opposingStrengthAvg * armyDiffWeight;
        // Terrs and sharks receive 65% of point reward to divide up
        int supportPoints = Math.round( totalPoints * 0.65f );
        // Attackers (all others) receive 35%
        int attackPoints = Math.round( totalPoints * 0.35f );
        int totalLvlSupport = 0;
        int totalLvlAttack = 0;
        Iterator <DistensionPlayer>i = m_players.values().iterator();
        while( i.hasNext() ) {
            DistensionPlayer p = i.next();
            Integer time = playerTimes.get( p.getName() );
            float percentOnFreq = 0;
            if( time != null )
                percentOnFreq = (float)(secs - time) / (float)secs;             
            if( p.isSupportShip() )
                totalLvlSupport += Math.round( (float)p.getRank() * percentOnFreq );
            else
                totalLvlAttack += Math.round( (float)p.getRank() * percentOnFreq );            
        }
        
        if( DEBUG )
            m_botAction.sendArenaMessage( "DEBUG: ((" + minsToWin + "min battle / 2) * " + opposingStrengthAvg + " enemy strength * " + armyDiffWeight + " weight) = " + totalPoints + "RP won (" + supportPoints + " for support, " + attackPoints + " for attack)" );

        // Point formula: (min played/2 * avg opposing strength * weight) * your upgrade level / avg team strength        
        i = m_players.values().iterator();
        int playerRank = 0;
        float points = 0;
        while( i.hasNext() ) {
            DistensionPlayer p = i.next();
            if( p.getArmyID() == winningArmyID ) {
                playerRank = p.getRank();
                if( playerRank == 0 )
                    playerRank = 1;
                if( totalLvlSupport == 0) totalLvlSupport = 1;
                if( totalLvlAttack == 0) totalLvlAttack = 1;
                if( p.isSupportShip() )
                    points = (float)supportPoints * ((float)playerRank / (float)totalLvlSupport);
                else
                    points = (float)attackPoints * ((float)playerRank / (float)totalLvlAttack);
                Integer time = playerTimes.get( p.getName() );
                if( time != null ) {
                    float percentOnFreq = (float)(secs - time) / (float)secs;
                    int modPoints = Math.max(1, Math.round(points * percentOnFreq) );                    
                    if( DEBUG )
                        if( p.isSupportShip() )
                            m_botAction.sendPrivateMessage(p.getName(), "DEBUG: " + modPoints + " RP for victory = (rank " + playerRank + " / total support strentgh " + totalLvlSupport + " (" + playerRank / totalLvlSupport + ")) * support points:" + supportPoints + " * " + percentOnFreq * 100 + "% participation" );
                        else
                            m_botAction.sendPrivateMessage(p.getName(), "DEBUG: " + modPoints + " RP for victory = (rank " + playerRank + " / total attack strentgh " + totalLvlAttack + " (" + playerRank / totalLvlAttack + ")) * assault points:" + attackPoints + " * " + percentOnFreq * 100 + "% participation" );
                    else
                        m_botAction.sendPrivateMessage(p.getName(), "You receive " + modPoints + " RP for your role in the victory." );
                    int holds = flagTimer.getSectorHolds( p.getName() );
                    int breaks = flagTimer.getSectorBreaks( p.getName() );                    
                    int bonus = 0;
                    if( holds != 0 && breaks != 0 ) {
                        bonus = Math.max(1, (int)( modPoints * (((float)holds / 10.0) + ((float)breaks / 20.0)) ));
                        m_botAction.sendPrivateMessage( p.getName(), "For " + holds + " sector holds and " + breaks +" sector breaks, you also receive an additional " + bonus + " RP." );
                    } else if( holds != 0 ) {
                        bonus = Math.max(1, (int)( modPoints * ((float)holds / 10.0) ));
                        m_botAction.sendPrivateMessage( p.getName(), "For " + holds + " sector holds, you also receive an additional " + bonus + " RP." );
                    } else if( breaks != 0 ) {
                        bonus = Math.max(1, (int)( modPoints * ((float)breaks / 20.0) ));
                        m_botAction.sendPrivateMessage( p.getName(), "For " + breaks + " sector breaks, you also receive an additional " + bonus + " RP." );                        
                    }
                    modPoints += bonus;
                    p.addRankPoints(modPoints);
                }
            }
        }

        /*
       try {
            String[] leaderInfo = flagTimer.getTeamLeader( MVPs );
            if( leaderInfo.length != 3 )
                return;
            String name, MVplayers = "";
            MVPs.remove( leaderInfo[0] );
            if( !leaderInfo[2].equals("") ) {
                String otherleaders[] = leaderInfo[2].split(", ");
                for( int j = 0; j<otherleaders.length; j++ )
                    MVPs.remove( otherleaders[j] );
            }
                MVplayers = (String)i.next();
                int grabs = flagTimer.getSectorHolds(MVplayers);
                if( grabs > 0 )
                    MVplayers += "(" + grabs + ")";
            }
            int grabs = 0;
            while( i.hasNext() ) {
                name = (String)i.next();
                grabs = flagTimer.getSectorHolds(name);
                if( grabs > 0 )
                    MVplayers = MVplayers + ", " + name + "(" + grabs + ")";
                else
                    MVplayers = MVplayers + ", " + name;
            }

            if( leaderInfo[0] != "" ) {
                if( leaderInfo[2] == "" )
                    m_botAction.sendArenaMessage( "Team Leader was " + leaderInfo[0] + "!  (" + leaderInfo[1] + " flag claim(s) + MVP)" );
                else
                    m_botAction.sendArenaMessage( "Team Leaders were " + leaderInfo[2] + "and " + leaderInfo[0] + "!  (" + leaderInfo[1] + " flag claim(s) + MVP)" );
            }
            if( MVplayers != "" )
                m_botAction.sendArenaMessage( "MVPs (+ claims): " + MVplayers );
        } catch(Exception e) {
            Tools.printStackTrace( e );
        }
             */

        int intermissionTime = 10000;

        if( gameOver ) {
            // Need special reward for conflict win
            intermissionTime = 20000;
            m_botAction.sendArenaMessage( "THE CONFLICT IS OVER!  " + m_armies.get(winningArmyID).getName() + " has laid total claim to the sector, after " + (freq0Score + freq1Score) + " total battles.", Tools.Sound.HALLELUJAH );
            freq0Score = 0;
            freq1Score = 0;
        }

        try {
            flagTimer.endBattle();
            m_botAction.cancelTask(flagTimer);
            m_botAction.cancelTask(intermissionTimer);
        } catch (Exception e ) {
        }

        if( stopFlagTime ) {
            m_botAction.sendArenaMessage( "The war is over ... at least, for now." );
            freq0Score = 0;
            freq1Score = 0;
            flagTimeStarted = false; 
        } else {
            doScores(intermissionTime);
            intermissionTimer = new IntermissionTask();
            m_botAction.scheduleTask( intermissionTimer, intermissionTime );
        }
    }


    /**
     * Adds all players to the hashmap which stores the time, in flagTimer time,
     * when they joined their freq.
     */
    public void setupPlayerTimes() {
        playerTimes.clear();

        Iterator i = m_botAction.getPlayingPlayerIterator();
        Player player;

        try {
            while( i.hasNext() ) {
                player = (Player)i.next();
                playerTimes.put( player.getPlayerName(), new Integer(0) );
            }
        } catch (Exception e) {
        }
    }


    /**
     * Formats an integer time as a String.
     * @param time Time in seconds.
     * @return Formatted string in 0:00 format.
     */
    public String getTimeString( int time ) {
        if( time <= 0 ) {
            return "0:00";
        } else {
            int minutes = time / 60;
            int seconds = time % 60;
            return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        }
    }


    /**
     * Warps all players at round start, if they are not presently spawning.
     */
    private void warpPlayers() {
        Iterator <DistensionPlayer>i = m_players.values().iterator();
        DistensionPlayer p;
        while( i.hasNext() ) {
            p = i.next();
            if( p != null && p.getShipNum() > 0 && !p.isRespawning() ) {
                p.doWarp( true );
            }
        }
    }


    /**
     * Warp all players to a "safe" 10 seconds before starting round.
     * Largely for building tension.
     */
    private void safeWarp() {
        Iterator <DistensionPlayer>i = m_players.values().iterator();
        DistensionPlayer p;
        while( i.hasNext() ) {
            p = i.next();
            if( p != null && p.getShipNum() > 0 && !p.isRespawning() )
                p.doSafeWarp();
        }
    }


    /**
     * Refreshes all support ships with their items.
     */
    private void refreshSupportShips() {
        Iterator <DistensionPlayer>i = m_players.values().iterator();
        DistensionPlayer p;
        while( i.hasNext() ) {
            p = i.next();
            if( p.isSupportShip() && !p.isRespawning() ) {
                m_botAction.setShip( p.getName(), 1 );
                m_botAction.setShip( p.getName(), p.getShipNum() );
                p.doSetupSpecialRespawn();
            }
        }
    }
        
    
    /**
     * Shows and hides scores (used at intermission only).
     * @param time Time after which the score should be removed
     */    
    private void doScores(int time) {
        int[] objs1 = {2000,(freq0Score<10 ? 60 + freq0Score : 50 + freq0Score), (freq0Score<10 ? 80 + freq1Score : 70 + freq1Score)};
        boolean[] objs1Display = {true,true,true};
        scoreDisplay = new AuxLvzTask(objs1, objs1Display);
        int[] objs2 = {2200,2000,(freq0Score<10 ? 60 + freq0Score : 50 + freq0Score), (freq0Score<10 ? 80 + freq1Score : 70 + freq1Score)};
        boolean[] objs2Display = {true,false,false,false};
        scoreRemove = new AuxLvzTask(objs2, objs2Display);
        delaySetObj = new AuxLvzConflict(scoreRemove);
        scoreDisplay.init();                                // Initialize score display
        m_botAction.scheduleTask(scoreDisplay, 1000);       // Do score display
        m_botAction.scheduleTask(delaySetObj, 2000);        // Initialize score removal
        m_botAction.scheduleTask(scoreRemove, time-1000);   // Do score removal
        m_botAction.showObject(2100);

    }

    // ***** FLAG TIMER TASKS

    /**
     * This private class starts the round.
     */
    private class StartRoundTask extends TimerTask {

        /**
         * Starts the round when scheduled, if it has not been stopped by lack of
         * participation.
         */
        public void run() {
            if( stopFlagTime ) {
                try {
                    flagTimeStarted = false;
                    flagTimer.endBattle();
                    m_botAction.cancelTask(flagTimer);
                } catch (Exception e ) {
                }
                m_botAction.hideObject(LVZ_INTERMISSION); // Turns off intermission lvz
            } else {
                doStartRound();
            }
        }
    }


    /**
     * This private class provides a pause before starting the round.
     */
    private class IntermissionTask extends TimerTask {

        /**
         * Starts the intermission/rule display when scheduled.
         */
        public void run() {
            doIntermission();
            m_botAction.showObject(LVZ_INTERMISSION); //Shows intermission lvz
        }
    }

    /**
     * Used to turn on/off a set of LVZ objects at a particular time.
     */    
    private class AuxLvzTask extends TimerTask {
        public int[] objNums;
        public boolean[] showObj;

        /**
         * Creates a new AuxLvzTask, given obj numbers defined in the LVZ and whether
         * or not to turn them on or off.  Cardinality of the two arrays must be the same.
         * @param objNums Numbers of objs defined in the LVZ to turn on or off
         * @param showObj For each index, true to show the obj; false to hide it
         */
        public AuxLvzTask(int[] objNums, boolean[] showObj) {
            if( objNums.length != showObj.length )
                throw new RuntimeException("AuxLvzTask constructor error: Arrays must have same cardinality.");
            this.objNums = objNums;
            this.showObj = showObj;
        }

        /**
         * Initializes the task by setting up each object to show or hide
         * using the core Objset class.
         */
        public void init()  {
            for(int i=0 ; i<objNums.length ; i++)   {
                if(showObj[i])
                    flagTimerObjs.showObject(objNums[i]);
                else
                    flagTimerObjs.hideObject(objNums[i]);
            }
        }

        /**
         * Shows and hides set objects.
         */
        public void run() {
            m_botAction.setObjects();
        }
    }

    /**
     * Schedules an initialization, generally for removing an LVZ set by
     * AuxLvzTask; used to resolve LVZ conflicts occuring from using a single
     * Objset class.
     */
    private class AuxLvzConflict extends TimerTask  {
        public AuxLvzTask myTask;

        /**
         * @param task AuxLvzTask to init on run
         */
        public AuxLvzConflict(AuxLvzTask task)  {
            myTask = task;
        }

        public void run()   {
            myTask.init();
        }

    }

    /**
     * This private class counts the consecutive flag time an individual team racks up.
     * Upon reaching the time needed to win, it fires the end of the round.
     */
    private class FlagCountTask extends TimerTask {
        int sectorHoldingArmyID, breakingArmyID;
        int secondsHeld, totalSecs, breakSeconds, preTimeCount;
        String breakerName = "";
        boolean isStarted, isRunning, claimBeingBroken;
        HashMap <String,Integer>sectorHolds;        // Name -> # Holds
        HashMap <String,Integer>sectorBreaks;       // Name -> # Breaks
        HashMap <Integer,Integer>armyStrengths;     // ArmyID -> total cumulative strengths (60sec snapshots)

        /**
         * FlagCountTask Constructor
         */
        public FlagCountTask( ) {
            sectorHoldingArmyID = -1;
            breakingArmyID = -1;
            secondsHeld = 0;
            totalSecs = 0;
            breakSeconds = 0;
            isStarted = false;
            isRunning = false;
            claimBeingBroken = false;
            sectorHolds = new HashMap<String,Integer>();
            sectorBreaks = new HashMap<String,Integer>();
            armyStrengths = new HashMap<Integer,Integer>();
        }

        /**
         * Called by FlagClaimed when BOTH flags have been claimed by an army.
         * 
         * @param armyID Frequency of flag claimer
         * @param pid PlayerID of flag claimer
         */
        public void sectorClaimed( int armyID, String pName ) {
            if( isRunning == false )
                return;

            // Failed sector claim break; give it back to sector-securing army
            if( armyID == sectorHoldingArmyID ) {
                if( claimBeingBroken ) {
                    claimBeingBroken = false;
                    breakingArmyID = -1;
                    breakSeconds = 0;
                    return;
                } else
                    return; // If this army already holds, and no claim is being broken,
                // then this is a false notify; ignore.
            }

            // Sector secure
            sectorHoldingArmyID = armyID;
            m_botAction.showObject(LVZ_FLAG_CLAIMED); // Shows flag claimed lvz
            claimBeingBroken = false;
            breakingArmyID = -1;
            secondsHeld = 0;            
            DistensionPlayer p = m_players.get(pName);
            if( p == null )
                return;
            addSectorHold( p.getName() );
            flagObjs.showObject(LVZ_SECTOR_HOLD);
            m_botAction.manuallySetObjects( flagObjs.getObjects() );
            m_botAction.sendArenaMessage( "SECTOR HOLD:  " + p.getName() + " of " + p.getArmyName() + " has secured a hold over the sector.", 2 );
        }

        /**
         * Called when a sector hold is in the process of being broken. 
         */
        public void holdBreaking( int armyID, String pName ) {
            if( armyID != sectorHoldingArmyID ) {
                breakerName = pName;
                breakingArmyID = armyID;
                claimBeingBroken = true;
                breakSeconds = 0;
            }            
        }        

        /**
         * Called when a hold over the sector has been broken (when an army who was holding the
         * sector no longer holds it).
         */
        public void doSectorBreak() {
            int remain = (flagMinutesRequired * 60) - secondsHeld;
            DistensionPlayer p = m_players.get(breakerName);
            if( p != null ) {
                if( remain < 4 )
                    m_botAction.sendArenaMessage( "SECTOR HOLD BROKEN!!  " + p.getName() + " of " + p.getArmyName() + " breaks the hold with just " + remain + " seconds left!!" );
                else if( remain < 11 )
                    m_botAction.sendArenaMessage( "SECTOR HOLD BROKEN: " + p.getName() + " of " + p.getArmyName() + " breaks the hold with just " + remain + " seconds left!" );
                else
                    m_botAction.sendArenaMessage( "Sector hold broken by " + p.getName() + " of " + p.getArmyName() + "." );
            }
            claimBeingBroken = false;
            breakSeconds = 0;
            breakingArmyID = -1;
            sectorHoldingArmyID = -1;
            secondsHeld = 0;
            addSectorBreak( p.getName() );
            flagObjs.hideObject(LVZ_SECTOR_HOLD);
            m_botAction.manuallySetObjects( flagObjs.getObjects() );
            do_updateTimer();
        }

        /**
         * Increments a count for player holding the sector.
         * @param name Name of player.
         */
        public void addSectorHold( String name ) {
            Integer count = sectorHolds.get( name );
            if( count == null ) {
                sectorHolds.put( name, new Integer(1) );
            } else {
                sectorHolds.remove( name );
                sectorHolds.put( name, new Integer( count.intValue() + 1) );
            }
        }

        /**
         * Increments a count for player holding the sector.
         * @param name Name of player.
         */
        public void addSectorBreak( String name ) {
            Integer count = sectorHolds.get( name );
            if( count == null ) {
                sectorBreaks.put( name, new Integer(1) );
            } else {
                sectorBreaks.remove( name );
                sectorBreaks.put( name, new Integer( count.intValue() + 1) );
            }
        }

        /**
         * Ends the battle for the timer's internal purposes; clears flag timer
         * and flag-owning visual info.
         */
        public void endBattle() {
            flagTimerObjs.hideAllObjects();
            flagObjs.hideAllObjects();
            m_botAction.setObjects();
            m_botAction.manuallySetObjects( flagObjs.getObjects() );
            isRunning = false;
        }

        /**
         * Sends time info to requested player.
         * @param name Person to send info to
         */
        public void sendTimeRemaining( String name ) {
            m_botAction.sendSmartPrivateMessage( name, getTimeInfo() );
        }
        
        /**
         * Records all army strengths (ran each 60 seconds).  Values stored are
         * incremental, to be divided by # minutes at end.
         */
        public void recordArmyStrengthSnapshot() {
            Integer str;
            for( DistensionArmy army : m_armies.values() ) {
                str = armyStrengths.get( army.getID() );
                if( str == null )
                    armyStrengths.put(army.getID(), army.getTotalStrength() );
                else
                    armyStrengths.put(army.getID(), str + army.getTotalStrength() );
            }
        }

        /**
         * @return True if a game is currently running; false if not
         */
        public boolean isRunning() {
            return isRunning;
        }

        /**
         * Gives the name of the top flag claimers out of the MVPs.  If there is
         * a tie, does not care because it's only bragging rights anyway. :P
         * @return Array of size 2, index 0 being the team leader and 1 being # flaggrabs
         */
        public String[] getTeamLeader( HashSet<String> MVPs ) {
            String[] leaderInfo = {"", "", ""};
            HashSet <String>ties = new HashSet<String>();

            if( MVPs == null )
                return leaderInfo;
            try {
                Iterator<String> i = MVPs.iterator();
                Integer dummyClaim, highClaim = new Integer(0);
                String leader = "", dummyPlayer;

                while( i.hasNext() ) {
                    dummyPlayer = i.next();
                    dummyClaim = sectorHolds.get( dummyPlayer );
                    if( dummyClaim != null ) {
                        if( dummyClaim.intValue() > highClaim.intValue() ) {
                            leader = dummyPlayer;
                            highClaim = dummyClaim;
                            ties.clear();
                        } else if ( dummyClaim.intValue() == highClaim.intValue() ) {
                            ties.add(dummyPlayer);
                        }
                    }
                }
                leaderInfo[0] = leader;
                leaderInfo[1] = highClaim.toString();
                i = ties.iterator();
                while( i.hasNext() )
                    leaderInfo[2] += i.next() + ", ";
                return leaderInfo;

            } catch (Exception e ) {
                Tools.printStackTrace( e );
                return leaderInfo;
            }

        }

        /**
         * Returns number of sector holds for given player.
         * @param name Name of player
         * @return Flag grabs
         */
        public int getSectorHolds( String name ) {
            Integer grabs = sectorHolds.get( name );
            if( grabs == null )
                return 0;
            else
                return grabs;
        }

        /**
         * Returns number of hold breaks for given player.
         * @param name Name of player
         * @return Flag grabs
         */
        public int getSectorBreaks( String name ) {
            Integer breaks = sectorBreaks.get( name );
            if( breaks == null )
                return 0;
            else
                return breaks;
        }

        /**
         * @return Time-based status of game
         */
        public String getTimeInfo() {
            int roundNum = freq0Score + freq1Score + 1;

            if( isRunning == false ) {
                if( roundNum == 1 )
                    return "The first battle of a new conflict is just about to begin.";
                else
                    return "We are currently in between battles (battle " + roundNum + " starting soon).  Battles won: " + freq0Score + " to " + freq1Score;
            }
            if( sectorHoldingArmyID == -1 )
                return "BATTLE " + roundNum + " Stats:  No army holds the sector.  [Time: " + getTimeString( totalSecs ) + "]  Battles won: " + freq0Score + " to " + freq1Score;            
            return "BATTLE " + roundNum + " Stats: " + m_armies.get(sectorHoldingArmyID).getName() + " has held the sector for " + getTimeString(secondsHeld) + ", needs " + getTimeString( (flagMinutesRequired * 60) - secondsHeld ) + " more.  [Time: " + getTimeString( totalSecs ) + "]  Battles won: " + freq0Score + " to " + freq1Score;
        }

        /**
         * @return Total number of seconds round has been running
         */
        public int getTotalSecs() {
            return totalSecs;
        }

        /**
         * @return Total number of seconds flag has been held
         */
        public int getSecondsHeld() {
            return secondsHeld;
        }

        /**
         * @return ID of army that currently holds the flag
         */
        public int getHoldingFreq() {
            return sectorHoldingArmyID;
        }

        /**
         * @return Frequency that currently holds the flag
         */
        public HashMap<Integer,Integer> getArmyStrengthSnapshots() {
            return armyStrengths;
        }

        /**
         * Timer running once per second that handles the starting of a round,
         * displaying of information updates every 5 minutes, the flag claiming
         * timer, and total flag holding time/round ends.
         */
        public void run() {
            if( isStarted == false ) {
                int roundNum = freq0Score + freq1Score + 1;
                if( preTimeCount == 0 ) {
                    m_botAction.sendArenaMessage( "The next battle is just beginning . . .", 1 );                    
                    refreshSupportShips();
                    safeWarp();
                }
                preTimeCount++;

                if( preTimeCount >= 10 ) {
                    isStarted = true;
                    isRunning = true;
                    m_botAction.sendArenaMessage( ( roundNum == MAX_FLAGTIME_ROUNDS ? "THE FINAL BATTLE" : "BATTLE " + roundNum) + " HAS BEGUN!  Capture both flags for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win the battle.", Tools.Sound.GOGOGO );
                    resetAllFlagData();
                    setupPlayerTimes();
                    warpPlayers();
                    return;
                }
            }

            if( isRunning == false )
                return;

            totalSecs++;
            
            // Take strength snapshots to average the value over the battle.
            // (Prevents cheating by losing team docking at end of match.)
            if( totalSecs % 60 == 0 )
                recordArmyStrengthSnapshot();

            // Display info at 5 min increments, unless we are in the last 30 seconds of a battle
            if( (totalSecs % (5 * 60)) == 0 && ( (flagMinutesRequired * 60) - secondsHeld > 30) ) {
                m_botAction.sendArenaMessage( getTimeInfo() );
            }

            if( claimBeingBroken ) {
                breakSeconds++;
                if( breakSeconds >= SECTOR_BREAK_SECONDS ) {
                    breakSeconds = 0;
                    doSectorBreak();
                }
                return;
            }

            if( sectorHoldingArmyID == -1 )
                return;

            secondsHeld++;

            do_updateTimer();

            int flagSecsReq = flagMinutesRequired * 60;
            if( secondsHeld >= flagSecsReq ) {
                endBattle();
                doEndRound();
            } else if( flagSecsReq - secondsHeld == 30 ) {                
                m_botAction.sendArenaMessage( m_armies.get(sectorHoldingArmyID).getName() + " will win the battle in 30 seconds." );
            } else if( flagSecsReq - secondsHeld == 10 ) {
                m_botAction.sendArenaMessage( m_armies.get(sectorHoldingArmyID).getName() + " will win the battle in 10 seconds . . ." );
            }
        }

        /**
         * Runs the LVZ-based timer.
         */
        private void do_updateTimer() {
            int secsNeeded = flagMinutesRequired * 60 - secondsHeld;
            flagTimerObjs.hideAllObjects();
            int minutes = secsNeeded / 60;
            int seconds = secsNeeded % 60;
            if( minutes < 1 ) flagTimerObjs.showObject( 1100 );
            if( minutes > 10 )
                flagTimerObjs.showObject( 10 + ((minutes - minutes % 10)/10) );
            flagTimerObjs.showObject( 20 + (minutes % 10) );
            flagTimerObjs.showObject( 30 + ((seconds - seconds % 10)/10) );
            flagTimerObjs.showObject( 40 + (seconds % 10) );
            m_botAction.setObjects();
        }
    }





    // ***** SHIP DATA

    /**
     * Add ships and their appropriate upgrades to m_shipGeneralData.
     * 
     * Special prizes:
     * -1:  Fast respawn (puts you at the head of the respawning queue when you die)
     * -2:  Prizing burst + warp every 60 seconds, for terriers
     * -3:  Full charge chance every 30 seconds, for spiders
     * -4:  Targetted EMP against all enemies (uses !emp command)
     * -5:  Super chance every 30 seconds, for spiders
     * 
     * 
     * Order of speed of rank upgrades (high speed to low speed, lower # being faster ranks):
     * Terr   - 10  (unlock @ 7)
     * Shark  - 11  (unlock @ 2)
     * Lanc   - 12  (unlock @ 10)
     * WB     - 15  (start)
     * Weasel - 15  (unlock by successive kills)
     * Spider - 16  (unlock @ 4)
     * Levi   - 17  (unlock @ 20)
     * Jav    - 18  (unlock @ 15)
     * 
     * Prize format: Name, Prize#, Cost([]), Rank([]), # upgrades 
     */
    public void setupPrices() {
        /*
         *  Some now invalid data.   
         *  Cost     Level      Pt/kill     Kills req/upg
            15000~    10        48          312 (1562)
            7500~      9        43          174 (872)
            3600~      8        38          94 (473)
            1920       7        33          58 (290)
            960        6        28          34 (170)
            480        5        23          20 (100)
            240        4        18          13 (65)
            120        3        13          9.2 (46)
            60         2        8           7.5 (37.5)
            30         1        3           10 (50)           
         */

        // Ship 0 -- dummy (for ease of access)
        ShipProfile ship = new ShipProfile( -1, -1 );
        m_shipGeneralData.add( ship );

        ShipUpgrade upg;

        // WARBIRD -- starting ship
        // Med upg speed; rotation starts +1, energy has smaller spread, smaller max, & starts v. low
        // 4:  L2 Guns
        // 17: Multi
        // 21: Decoy
        // 27: L3 Guns
        // 36: Mines
        // 42: Thor
        // 48: XRadar
        ship = new ShipProfile( 0, 15 );
        upg = new ShipUpgrade( "Side Thrusters", Tools.Prize.ROTATION, 1, 0, 9 );           // 20 x9
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Density Reduction Unit", Tools.Prize.THRUST, 1, 0, 10 );    // 1 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Drag Balancer", Tools.Prize.TOPSPEED, 1, 0, 10 );           // 200 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Regeneration Drives", Tools.Prize.RECHARGE, 1, 0, 10 );     // 400 x10
        ship.addUpgrade( upg );
        int energyLevels1[] = { 0, 3, 5, 10, 15, 20, 25, 30, 35, 40 };
        upg = new ShipUpgrade( "Microfiber Armor", Tools.Prize.ENERGY, 1, energyLevels1, 10 );          // 150 x10
        ship.addUpgrade( upg );
        int p1a1[] = { 1, 1 };
        int p1a2[] = { 4, 27 };
        upg = new ShipUpgrade( "High-Impact Cannon", Tools.Prize.GUNS, p1a1, p1a2, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombs only as special)", Tools.Prize.BOMBS, 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Beam-Splitter", Tools.Prize.MULTIFIRE, 2, 17, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 1, 48, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Warbird Reiterator", Tools.Prize.DECOY, 1, 21, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Energy Concentrator", Tools.Prize.BOMBS, 1, 36, 1 );
        ship.addUpgrade( upg );
        //                                                    | <--- this mark and beyond is not seen
        upg = new ShipUpgrade( "Matter-to-Antimatter Converter", Tools.Prize.THOR, 1, 42, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S3", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S4", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // JAVELIN -- rank 15
        // Very slow upg speed; standard upgrades but recharge & energy have +2 max 
        // 9:  L1 Guns
        // 15: L2 Bombs
        // 18: Multi
        // 23: L2 Guns
        // 28: Decoy 1
        // 30: Shrap (20 total levels)
        // 35: Rocket
        // 42: XRadar
        // 50: Decoy 2
        // 60: L3 Guns
        // 80: L3 Bombs
        ship = new ShipProfile( RANK_REQ_SHIP2, 18 );
        upg = new ShipUpgrade( "Balancing Streams", Tools.Prize.ROTATION, 1, 0, 10 );       // 20 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Fuel Economizer", Tools.Prize.THRUST, 1, 0, 10 );           // 1 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Engine Reoptimization", Tools.Prize.TOPSPEED, 1, 0, 10 );   // 200 x10
        ship.addUpgrade( upg );
        int p2a1[] = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 10 };
        int p2a2[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 55, 70 };
        upg = new ShipUpgrade( "Tactical Engineering Crew", Tools.Prize.RECHARGE, p2a1, p2a2, 12 ); // 150 x12
        ship.addUpgrade( upg );
        int energyLevels2[] = { 2, 5, 10, 15, 20, 25, 30, 35, 40, 45, 55, 70 };
        upg = new ShipUpgrade( "Reinforced Plating", Tools.Prize.ENERGY, p2a1, energyLevels2, 12 );  // 146 x12
        ship.addUpgrade( upg );
        int p2b1[] = { 1,  2,  3 };
        int p2b2[] = { 9, 23, 60 };
        upg = new ShipUpgrade( "Rear Defense System", Tools.Prize.GUNS, p2b1, p2b2, 3 );
        ship.addUpgrade( upg );
        int p2c1[] = {  3,  8 };
        int p2c2[] = { 15, 80 };
        upg = new ShipUpgrade( "Mortar Explosive Enhancement", Tools.Prize.BOMBS, p2c1, p2c2, 2 );
        ship.addUpgrade( upg );        
        upg = new ShipUpgrade( "Modified Defense Cannon", Tools.Prize.MULTIFIRE, 2, 18, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Detection System", Tools.Prize.XRADAR, 1, 42, 1 );
        ship.addUpgrade( upg );        
        int p2d1[] = { 1, 3 };
        int p2d2[] = { 28, 50 };
        upg = new ShipUpgrade( "Javelin Reiterator", Tools.Prize.DECOY, p2d1, p2d2, 2 );
        ship.addUpgrade( upg );        
        upg = new ShipUpgrade( "Splintering Mortar", Tools.Prize.SHRAPNEL, 1, 30, 20 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Emergency Fuel Supply", Tools.Prize.ROCKET, 2, 35, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S3", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S4", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // SPIDER -- rank 4
        // Med upg speed; lvl req for thurst & speed; thrust perm+1; speed has wide spread; recharge has 20 lvls
        //  9: +50% Refueler 1
        // 15: +5% Super 1
        // 18: Decoy 1
        // 25: +5% Super 2
        // 26: Multi (rear)
        // 29: Decoy 2
        // 33: +50% Refueler 2
        // 35: +5% Super 3
        // 38: Anti
        // 40: L2 Guns
        // 42: XRadar
        // 45: +5% Super 4
        // 47: Decoy 3
        // 55: +5% Super 5
        ship = new ShipProfile( RANK_REQ_SHIP3, 16 );
        upg = new ShipUpgrade( "Central Realigner", Tools.Prize.ROTATION, 1, 0, 10 );       // 20 x10
        ship.addUpgrade( upg );
        int p3a1[] = { 0, 0, 0, 0, 0, 20, 24, 28, 32, 36 };
        upg = new ShipUpgrade( "Sling Drive", Tools.Prize.THRUST, 1, p3a1, 10 );            // 1 x10
        ship.addUpgrade( upg );
        int p3b1[] = { 0, 5, 10, 15, 20, 25, 30, 35, 40, 45 };
        upg = new ShipUpgrade( "Spacial Filtering", Tools.Prize.TOPSPEED, 1, p3b1, 10 );    // 240 x10
        ship.addUpgrade( upg );
        int p3c1[] = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 3, 3, 4, 4, 5, 6, 8 };
        int p3c2[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,50,50,50,60,60,70,70,80,90,100 };
        upg = new ShipUpgrade( "Recompensator", Tools.Prize.RECHARGE, p3c1, p3c2, 20 );     // 250 x20
        ship.addUpgrade( upg );
        int energyLevels3[] = { 0, 3, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50 };
        upg = new ShipUpgrade( "Molecular Shield", Tools.Prize.ENERGY, 1, energyLevels3, 12 ); // 140 x12
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Rapid Disintigrator", Tools.Prize.GUNS, 1, 40, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombing ability disabled)", Tools.Prize.BOMBS, 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Rear Projector", Tools.Prize.MULTIFIRE, 1, 26, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 2, 42, 1 );
        ship.addUpgrade( upg );
        int p3d1[] = { 18, 29, 47 };
        upg = new ShipUpgrade( "Spider Reiterator", Tools.Prize.DECOY, 1, p3d1, 3 );
        ship.addUpgrade( upg );
        int p3e1[] = { 9, 33 };
        upg = new ShipUpgrade( "+50% Refeuler", -3, 1, p3e1, 2 );
        ship.addUpgrade( upg );
        int p3f1[] = { 15, 25, 35, 45, 55 };
        upg = new ShipUpgrade( "+5% Infinite Stream", -5, 1, p3f1, 5 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Warp Field Stabilizer", Tools.Prize.ANTIWARP, 1, 38, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S4", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // LEVIATHAN -- rank 20
        // Slow upg speed; thrust has 5 lvls, all costly; energy starts high, upg's slow, but high max;
        //                 speed has 7 lvls, upgrades fast; rotation has 25 levels, upg's slow;
        //                 energy does not have the rank requirements other ships have
        // 4:  L1 Bombs
        // 8:  L2 Guns
        // 15: L2 Bombs
        // 19: Decoy
        // 25: Stealth
        // 28: Portal
        // 30: Multi (close shotgun)
        // 35: L3 Guns
        // 42: XRadar
        // 48: L3 Bombs
        // 60: Prox
        // 70: Shrapnel        
        ship = new ShipProfile( RANK_REQ_SHIP4, 17 );
        upg = new ShipUpgrade( "Gravitational Modifier", Tools.Prize.ROTATION, 1, 0, 25 );      // 10 x25
        ship.addUpgrade( upg );
        int p4a1[] = { 8, 20, 40, 50, 60 };
        upg = new ShipUpgrade( "Force Thrusters", Tools.Prize.THRUST, 2, p4a1, 5 );                // 4 x5
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Collection Drive", Tools.Prize.TOPSPEED, 1, 0, 7 );             // 1000 x7
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Power Recirculator", Tools.Prize.RECHARGE, 1, 0, 10 );          // 140 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Carbon-Forced Armor", Tools.Prize.ENERGY, 1, 0, 20 );           // 100 x20
        ship.addUpgrade( upg );
        int p4b1[] = { 1,  4 };
        int p4b2[] = { 8, 35 };
        upg = new ShipUpgrade( "Spill Guns", Tools.Prize.GUNS, p4b1, p4b2, 2 );         // DEFINE
        ship.addUpgrade( upg );
        int p4c1[] = { 1,  1,  3 };
        int p4c2[] = { 4, 15, 48 };
        upg = new ShipUpgrade( "Chronos(TM) Disruptor", Tools.Prize.BOMBS, p4c1, p4c2, 3 );        // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Sidekick Cannons", Tools.Prize.MULTIFIRE, 1, 30, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 1, 42, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Leviathan Reiterator", Tools.Prize.DECOY, 2, 19, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Wormhole Inducer", Tools.Prize.PORTAL, 3, 28, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Scrambler", Tools.Prize.STEALTH, 1, 25, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Proximity Bomb Detonator", Tools.Prize.PROXIMITY, 3, 60, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Splintering Mortar", Tools.Prize.SHRAPNEL, 5, 70, 1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // TERRIER -- rank 7
        // Very fast upg speed; rotation, thrust and energy have only 9 lvls; recharge max is normal terr max
        // 2:  Burst 1
        // 5: +10% Regeneration (and 1 level available every 5 levels)
        // 7:  Portal 1
        // 9:  Priority respawn
        // 13: XRadar
        // 16: Multi (regular)
        // 21: Burst 2 
        // 27: Decoy
        // 29: Portal 2
        // 30: (Can attach to other terrs)
        // 36: Guns
        // 44: Burst 3
        // 48: Portal 3
        // 50: Targetted EMP (negative full charge to all of the other team)
        // 55: Burst 4
        // 60: Portal 4
        // 65: Burst 5
        // 70: Portal 5
        // 80: Burst 6
        ship = new ShipProfile( RANK_REQ_SHIP5, 10 );
        upg = new ShipUpgrade( "Correction Engine", Tools.Prize.ROTATION, 1, 0, 9 );            // 30 x9
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Interwoven Propulsor", Tools.Prize.THRUST, 1, 0, 9 );           // 2 x9
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Microspiral Drive", Tools.Prize.TOPSPEED, 1, 0, 10 );           // 400 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Hull Reconstructor", Tools.Prize.RECHARGE, 1, 0, 5 );           // 180 x5
        ship.addUpgrade( upg );
        int energyLevels5[] = { 1, 3, 5, 8, 10, 15, 20, 25, 40 };
        upg = new ShipUpgrade( "Hull Capacity", Tools.Prize.ENERGY, 1, energyLevels5, 9 );      // 150 x9
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Defense Systems", Tools.Prize.GUNS, 2, 36, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Multiple Cannons", Tools.Prize.MULTIFIRE, 1, 16, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 1, 13, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Terrier Reiterator", Tools.Prize.DECOY, 1, 27, 1 );
        ship.addUpgrade( upg );
        int p5a1[] = { 1,  1,  2,  3,  5 };
        int p5a2[] = { 7, 29, 48, 60, 70 };
        upg = new ShipUpgrade( "Wormhole Creation Kit", Tools.Prize.PORTAL, p5a1, p5a2, 5 );        // DEFINE
        ship.addUpgrade( upg );
        int p5b1[] = { 1,  1,  2,  3,  5,  8 };
        int p5b2[] = { 2, 21, 44, 55, 65, 80 };
        upg = new ShipUpgrade( "Rebounding Burst", Tools.Prize.BURST, p5b1, p5b2, 6 );       // DEFINE
        ship.addUpgrade( upg );
        int p5c1[] = { 5, 10, 15, 20, 25, 30, 35, 40, 45, 50 };
        upg = new ShipUpgrade( "+10% Regeneration", -2, 1, p5c1, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Priority Rearmament", -1, 1, 9, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Targetted EMP", -4, 5, 50, 1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        //ship = new ShipProfile( -1, 2 );
        //m_shipGeneralData.add( ship );

        // WEASEL -- Unlocked by 20(tweak?) successive kills w/o dying
        // Slow-medium upg speed; all upg start 2 levels higher than normal calcs, but 1st level (3rd) costs 2pts
        //                        and all have level requirements; special upgrades come somewhat early
        //  5: XRadar
        //  9: Stealth
        // 14: L2 Guns
        // 19: Multifire
        // 23: Cloak
        // 27: Rocket 1
        // 35: L3 Guns
        // 42: Decoy
        // 46: Rocket 2
        // 50: Brick
        // 60: Rocket 3
        ship = new ShipProfile( RANK_REQ_SHIP6, 15 );
        int p6a1[] = { 2, 1, 1, 1,  1,  1,  1,  1 };
        int p6a2[] = { 5, 8, 10, 15, 20, 30, 40, 50 };
        upg = new ShipUpgrade( "Orbital Force Unit", Tools.Prize.ROTATION, p6a1, p6a2, 8 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Gravity Shifter", Tools.Prize.THRUST, p6a1, p6a2, 8 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Time Distortion Mechanism", Tools.Prize.TOPSPEED, p6a1, p6a2, 8 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Influx Recapitulator", Tools.Prize.RECHARGE, p6a1, p6a2, 8 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Cerebral Shielding", Tools.Prize.ENERGY, p6a1, p6a2, 8 );
        ship.addUpgrade( upg );
        int p6b1[] = { 1, 2 };
        int p6b2[] = { 14, 35 };
        upg = new ShipUpgrade( "Low Propulsion Cannons", Tools.Prize.GUNS, p6b1, p6b2, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombing ability disabled)", Tools.Prize.BOMBS, 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Cannon Distributor", Tools.Prize.MULTIFIRE, 1, 19, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 1, 5, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Weasel Reiterator", Tools.Prize.DECOY, 3, 42, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Scrambler", Tools.Prize.STEALTH, 1, 9, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Light-Bending Unit", Tools.Prize.CLOAK, 2, 23, 1 );
        ship.addUpgrade( upg );
        int p6c1[] = { 1, 2, 5 };
        int p6c2[] = { 27, 46, 60 };
        upg = new ShipUpgrade( "Assault Boosters", Tools.Prize.ROCKET, p6c1, p6c2, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Movement Inhibitor", Tools.Prize.BRICK, 2, 50, 1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        ship = new ShipProfile( -1, 0 );
        m_shipGeneralData.add( ship );

        /*
        // LANCASTER -- rank 10
        ship = new ShipProfile( RANK_REQ_SHIP_7, 12 );
        upg = new ShipUpgrade( "Directive Realigner", Tools.Prize.ROTATION, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "InitiaTek Burst Engine", Tools.Prize.THRUST, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Streamlining Unit", Tools.Prize.TOPSPEED, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Pneumatic Refiltrator", Tools.Prize.RECHARGE, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Interlocked Deflector", Tools.Prize.ENERGY, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Modernized Projector", Tools.Prize.GUNS, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombs show as special)", Tools.Prize.BOMBS, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Magnified Output Force", Tools.Prize.MULTIFIRE, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Lancaster Reiterator", Tools.Prize.DECOY, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Firebloom", Tools.Prize.BURST, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Lancaster Surprise", Tools.Prize.BOMBS, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Proximity Bomb Detonator", Tools.Prize.PROXIMITY, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S4", 0, 0, 0 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );
        */
        
        // SHARK -- rank 2
        // Very fast upg speed; rotation has small spread (+2start) and few upgrades; thrust starts 1 down
        //                      but has high max; energy starts high, has low level req initially, but
        //                      has high req later on (designed to give bomb capability early)
        // (Starts with 1 repel, so that repel 1 is actually second)
        //  1: Repel 1
        //  7: Repel 2
        // 12: Guns
        // 15: Repel 3
        // 17: XRadar
        // 20: Priority Rearmament
        // 22: Shrap 1
        // 26: Repel 4
        // 29: Multifire
        // 34: Repel 5
        // 38: Cloak
        // 40: Repel 6
        // 41: Shrap 2
        // 45: L2 Bombs
        // 50: Repel 7
        // 55: Repel 8
        // 60: Shrap 3
        // 70: Repel 9
        // 80: Repel 10
        ship = new ShipProfile( RANK_REQ_SHIP8, 11 );
        int p8a1[] = { 1, 1, 1, 1, 1, 2 };
        int p8a2[] = { 10, 11, 12, 13, 14, 15 };
        upg = new ShipUpgrade( "Runningside Correctors", Tools.Prize.ROTATION, p8a1, p8a2, 6 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Spitfire Thrusters", Tools.Prize.THRUST, 1, 0, 13 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Space-Force Emulsifier", Tools.Prize.TOPSPEED, 1, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Light Charging Mechanism", Tools.Prize.RECHARGE, 1, 1, 10 );
        ship.addUpgrade( upg );
        int p8b1[] = { 1, 1,  1,  1,  1,  1,  2,  3 };
        int p8b2[] = { 1, 2, 10, 20, 30, 40, 50, 75 };
        upg = new ShipUpgrade( "Projectile Slip Plates", Tools.Prize.ENERGY, p8b1, p8b2, 8 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Emergency Defense Cannon", Tools.Prize.GUNS, 1, 12, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Plasma-Infused Weaponry", Tools.Prize.BOMBS, 4, 45, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Spreadshot", Tools.Prize.MULTIFIRE, 1, 29, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 1, 17, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Decoy disabled)", Tools.Prize.DECOY, 0, 0, -1 );
        ship.addUpgrade( upg );
        int p8c1[] = { 1, 1,  1,  1,  1,  1,  1,  1,  1,  1 };
        int p8c2[] = { 1, 7, 15, 26, 34, 40, 50, 55, 70, 80 };        
        upg = new ShipUpgrade( "Gravitational Repulsor", Tools.Prize.REPEL, p8c1, p8c2, 10 );    // DEFINE
        ship.addUpgrade( upg );
        int p8d2[] = { 22, 41, 60 };
        upg = new ShipUpgrade( "Splintering Unit", Tools.Prize.SHRAPNEL, 1, p8d2, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Nonexistence Device", Tools.Prize.CLOAK, 3, 38, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Priority Rearmament", -1, 1, 20, 1 );   // DEFINE
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );        
    }

}