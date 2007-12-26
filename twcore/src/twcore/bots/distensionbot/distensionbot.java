package twcore.bots.distensionbot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
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
import twcore.core.events.TurretEvent;
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
 * - MAP: Add LVZ displaying rules
 *
 * Lower priority (in order):
 * - !intro, !intro2, !intro3, etc.
 * - !emp for terr "targetted EMP" ability, and appropriate player data.  This involves negative full charge.
 * - F1 Help -- item descriptions?  At least say which slot is which, if not providing info on the specials
 * - LVZ stuff: replacement sounds
 *
 * @author dugwyler
 */
public class distensionbot extends SubspaceBot {

    private final boolean DEBUG = true;                    // Debug mode.  Displays various info that would
                                                           // normally be annoying in a public release.
    private final float DEBUG_MULTIPLIER = 3.3f;          // Amount of RP to give extra in debug mode

    private final int NUM_UPGRADES = 14;                   // Number of upgrade slots allotted per ship
    private final int AUTOSAVE_DELAY = 5;                 // How frequently autosave occurs, in minutes
    private final int MESSAGE_SPAM_DELAY = 75;             // Delay in ms between a long list of spammed messages
    private final int PRIZE_SPAM_DELAY = 15;               // Delay in ms between prizes for individual players
    private final int UPGRADE_DELAY = 50;                  // How often the prize queue rechecks for prizing
    private final int DELAYS_BEFORE_TICK = 10;             // How many UPGRADE_DELAYs before prize queue runs a tick
    private final int TICKS_BEFORE_SPAWN = 10;             // # of UPGRADE_DELAYs * DELAYS_BEFORE_TICK before respawn
    private final int IDLE_FREQUENCY_CHECK = 10;           // In seconds, how frequently to check for idlers
    private final int IDLE_TICKS_BEFORE_DOCK = 10;         // # IDLE_FREQUENCY_CHECKS in idle before player is docked
    private final int LAGOUT_VALID_SECONDS = 120;          // # seconds since lagout in which you can use !lagout
    private final int LAGOUT_WAIT_SECONDS = 30;            // # seconds a player must wait to be placed back in the game
    private final int PROFIT_SHARING_FREQUENCY = 2 * 60;   // # seconds between adding up profit sharing for terrs
    private final int STREAK_RANK_PROXIMITY = 12;          // Max rank difference for a kill to count toward a streak
    private final float SUPPORT_RATE = 1.5f;               // Multiplier for support's cut of end round bonus
    private final String DB_PROB_MSG = "That last one didn't go through.  Database problem, it looks like.  Please send a ?help message ASAP.";
    private final float EARLY_RANK_FACTOR = 1.6f;          // Factor for rank increases (lvl 1-9)
    private final float LOW_RANK_FACTOR = 1.15f;           // Factor for rank increases (lvl 10+)
    private final float NORMAL_RANK_FACTOR = 1.10f;        // Factor for rank increases (lvl 25+)
    private final float HIGH_RANK_FACTOR = 1.2f;          // Factor for rank increases (lvl 50+)
    private final float STUPIDLY_HIGH_RANK_FACTOR = 1.5f;  // Factor for rank increases (lvl 70+)
    private final int RANK_DIFF_LOW = 10;                  // Rank difference calculations
    private final int RANK_DIFF_MED = 15;                  // for humiliation and low-rank RP caps
    private final int RANK_DIFF_HIGH = 20;
    private final int RANK_DIFF_VHIGH = 30;
    private final int RANK_DIFF_HIGHEST = 50;
    private final int RANK_0_STRENGTH = 10;                // How much str a rank 0 player adds to army (rank1 = 1 + rank0str, etc)
    private final int RANK_REQ_SHIP2 = 5;    // 15
    private final int RANK_REQ_SHIP3 = 2;    //  5
    private final int RANK_REQ_SHIP4 = 6;    // 20
    private final int RANK_REQ_SHIP5 = 3;    //  7
    private final int RANK_REQ_SHIP6 = 4;    // N/A (only has level for beta)
    private final int RANK_REQ_SHIP7 = 10;   // N/A (only has level for beta)
    private final int RANK_REQ_SHIP8 = 1;    //  2
    private final int RANK_REQ_SHIP9 = 99;   // 25

    // Spawn, safe, and basewarp coords
    private final int SPAWN_BASE_0_Y_COORD = 456;               // Y coord around which base 0 owners (top) spawn
    private final int SPAWN_BASE_1_Y_COORD = 566;               // Y coord around which base 1 owners (bottom) spawn
    private final int SPAWN_Y_SPREAD = 90;                      // # tiles * 2 from above coords to spawn players
    private final int SPAWN_X_SPREAD = 275;                     // # tiles * 2 from x coord 512 to spawn players
    private final int REARM_AREA_TOP_Y = 199;                   // Y coords of center of rearm areas
    private final int REARM_AREA_BOTTOM_Y = 824;
    private final int REARM_SAFE_TOP_Y = 192;                   // Y coords of safe parts of rearm areas
    private final int REARM_SAFE_BOTTOM_Y = 832;
    private final int BASE_CENTER_0_Y_COORD = 335;
    private final int BASE_CENTER_1_Y_COORD = 688;

    // Coords used for !terr and !whereis
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


    private Vector <ShipProfile>m_shipGeneralData;          // Generic (nonspecific) purchasing data for ships.  Uses 1-8 for #
    private Map <String,DistensionPlayer>m_players;         // In-game data on players (Name -> Player)
    private HashMap <Integer,DistensionArmy>m_armies;       // In-game data on armies  (ID -> Army)

    private PrizeQueue m_prizeQueue;                        // Queuing system for prizes (so as not to crash bot)
    private SpecialAbilityTask m_specialAbilityPrizer;      // Prizer for special abilities (run once every 30s)
    private TimerTask entranceWaitTask;                     // For when bot first enters the arena
    private TimerTask autoSaveTask;                         // For autosaving player data frequently
    private TimerTask idleSpecTask;                         // For docking idlers
    private TimerTask profitSharingTask;                    // For sharing profits with terrs

    private boolean beginDelayedShutdown;                   // True if, at round end, a shutdown should be initiated
    private boolean readyForPlay = false;                   // True if bot has entered arena and is ready to go
    private boolean dying = false;                          // True if bot is in its death throes
    private int[] m_flagOwner = {-1, -1};                   // ArmyIDs of flag owners; -1 for none
    private List <String>m_mineClearedPlayers;              // Players who have already cleared mines this battle
    private LinkedList <String>m_msgBetaPlayers;            // Players to send beta msg to
    private HashMap <String,Integer>m_defectors;            // Players wishing to defect who need to confirm defection

    // LIMITING SYSTEM
    private final int MAX_PLAYERS = 40;                     // Max # players allowed in game
    private TimerTask timeIncrementTask;                    // Task firing every minute that increments time playing
    private LinkedList <DistensionPlayer>m_waitingToEnter;  // List of players waiting to enter the game

    // ASSIST SYSTEM
    private final float ADVERT_WEIGHT_IMBALANCE = 0.85f;    // At what point to advert that there's an imbalance
    private final float ASSIST_WEIGHT_IMBALANCE = 0.89f;    // At what point an army is considered imbalanced
    private final int ASSIST_NUMBERS_IMBALANCE = 3;         // # of pilot difference before considered imbalanced
    private final int ASSIST_REWARD_TIME = 1000 * 60 * 1;   // Time between adverting and rewarding assists (1 min def.)
    private long lastAssistReward;                          // Last time assister was given points
    private long lastAssistAdvert;                          // Last time an advert was sent for assistance
    private boolean checkForAssistAdvert = false;           // True if armies may be unbalanced, requiring !assist advert
    private TimerTask assistAdvertTask;                     // Task used to advert for assisting the other army

    // SPECIAL ABILITY PRIZE #s
    public final int ABILITY_PRIORITY_REARM = -1;
    public final int ABILITY_TERR_REGEN = -2;
    public final int ABILITY_REFUELER = -3;
    public final int ABILITY_TARGETTED_EMP = -4;
    public final int ABILITY_SUPER = -5;
    public final int ABILITY_SHARK_REGEN = -6;
    public final int ABILITY_PROFIT_SHARING = -7;

    // TACTICAL OPS DATA
    public final int DEFAULT_MAX_OP = 3;                    // Max OP points when not upgraded
    public final int DEFAULT_OP_REGEN = 2;                  // Default % chance OP regen (2 = 20%)
    public final int DEFAULT_OP_MAX_COMMS = 3;              // Max # communications Ops can save up

    // TACTICAL OPS ABILITY PRIZE #s
    public final int OPS_INCREASE_MAX_OP = -11;
    public final int OPS_REGEN_RATE = -12;
    public final int OPS_COMMUNICATIONS = -13;
    public final int OPS_RADAR = -14;
    public final int OPS_FAST_TEAM_REARM = -15;
    public final int OPS_COVER = -16;
    public final int OPS_DOOR_CONTROL = -17;
    public final int OPS_SECLUSION = -18;
    public final int OPS_MINEFIELD = -19;
    public final int OPS_SHROUD = -10;
    public final int OPS_FLASH = -21;
    public final int OPS_TEAM_SHIELDS = -22;



    // DATA FOR FLAG TIMER
    private static final int MAX_FLAGTIME_ROUNDS = 7;   // Max # rounds (odd numbers only)
    private static final int SECTOR_CHANGE_SECONDS = 10; // Seconds it takes to secure hold or break one
    private static final int INTERMISSION_SECS = 90;    // Seconds between end of round & start of next
    private boolean flagTimeStarted;                    // True if flag time is enabled
    private boolean stopFlagTime;                       // True if flag time will stop at round end

    private int freq0Score, freq1Score;                 // # rounds won
    private int flagMinutesRequired = 1;                // Flag minutes required to win
    private HashMap <String,Integer>m_playerTimes;      // Roundtime of player on freq
    private HashMap <String,Integer>m_lagouts;          // Players who have potentially lagged out, + time they lagged out
    private HashMap <String,Integer>m_lagShips;         // Ships of players who were DC'd, for !lagout use

    private FlagCountTask flagTimer;                    // Flag time main class
    private StartRoundTask startTimer;                  // TimerTask to start round
    private IntermissionTask intermissionTimer;         // TimerTask for round intermission
    private AuxLvzTask scoreDisplay;                    // Displays score lvz
    private AuxLvzTask scoreRemove;                     // Removes score lvz
    private AuxLvzConflict delaySetObj;                 // Schedules a task after an amount of time
    private Objset flagTimerObjs;                       // For keeping track of flagtimer objs
    private Objset flagObjs;                            // For keeping track of flag-related objs
    private Objset playerObjs;                          // For keeping track of player-specific objs


    // LVZ OBJ# DEFINES ( < 100 reserved for flag timer counter )
    private final int LVZ_REARMING = 200;               // Rearming / attach at own risk
    private final int LVZ_RANKUP = 201;                 // RANK UP: Congratulations
    private final int LVZ_STREAK = 202;                 // Streak!
    private final int LVZ_TK = 203;                     // TK!
    private final int LVZ_TKD = 204;                    // TKd!
    private final int LVZ_PRIZEDUP = 205;               // Strange animation showing you're prized up
    private final int LVZ_TOPBASE_EMPTY = 251;          // Flag display
    private final int LVZ_TOPBASE_ARMY0 = 252;
    private final int LVZ_TOPBASE_ARMY1 = 253;
    private final int LVZ_BOTBASE_EMPTY = 254;
    private final int LVZ_BOTBASE_ARMY0 = 255;
    private final int LVZ_BOTBASE_ARMY1 = 256;
    private final int LVZ_SECTOR_HOLD = 257;            // Sector hold "glow" around flag display
    private final int LVZ_PROGRESS_BAR = 260;           // Progress bar; 261-269 are progress pieces
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
        m_players = Collections.synchronizedMap( new HashMap<String,DistensionPlayer>() );
        m_armies = new HashMap<Integer,DistensionArmy>();
        m_playerTimes = new HashMap<String,Integer>();
        m_lagouts = new HashMap<String,Integer>();
        m_lagShips = new HashMap<String,Integer>();
        m_mineClearedPlayers = Collections.synchronizedList( new LinkedList<String>() );
        m_msgBetaPlayers = new LinkedList<String>();
        m_defectors = new HashMap<String,Integer>();
        m_waitingToEnter = new LinkedList<DistensionPlayer>();
        flagTimerObjs = m_botAction.getObjectSet();
        flagObjs = new Objset();
        playerObjs = new Objset();
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
        beginDelayedShutdown = false;
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
        req.request(EventRequester.TURRET_EVENT);
    }


    /**
     * Performs startup tasks once the bot has entered the arena.
     */
    public void init() {
        m_botAction.sendUnfilteredPublicMessage("?chat=distension" );
        //m_botAction.setMessageLimit( 12 );
        m_botAction.setReliableKills( 1 );
        m_botAction.setPlayerPositionUpdating( 400 );
        m_botAction.setLowPriorityPacketCap( 40 );
        m_botAction.specAll();
        m_botAction.toggleLocked();
        m_botAction.resetFlagGame();
        m_prizeQueue = new PrizeQueue();
        m_botAction.scheduleTaskAtFixedRate(m_prizeQueue, UPGRADE_DELAY, UPGRADE_DELAY);
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
        if( IDLE_FREQUENCY_CHECK != 0 ) {
            idleSpecTask = new TimerTask() {
                public void run() {
                    // Check for idlers, but only while round is going.
                    if( flagTimer != null && flagTimer.isRunning() )
                        for( DistensionPlayer p : m_players.values() )
                            p.checkIdleStatus();
                }
            };
            m_botAction.scheduleTask( idleSpecTask, IDLE_FREQUENCY_CHECK * 1000, IDLE_FREQUENCY_CHECK * 1000);
        }
        timeIncrementTask = new TimerTask() {
            public void run() {
                for( DistensionPlayer p : m_players.values() )
                    p.incrementTime();
            }
        };
        m_botAction.scheduleTask( timeIncrementTask, 60000, 60000 );
        profitSharingTask = new TimerTask() {
            public void run() {
                int army0profits = m_armies.get(0).getProfits();
                int army1profits = m_armies.get(1).getProfits();

                for( DistensionPlayer p : m_players.values() )
                    if( p.getArmyID() == 0 )
                        p.shareProfits(army0profits);
                    else
                        p.shareProfits(army1profits);
            }
        };
        m_botAction.scheduleTask( profitSharingTask, PROFIT_SHARING_FREQUENCY * 1000, PROFIT_SHARING_FREQUENCY * 1000 );
        // Do not advert/reward for rectifying imbalance in the first 1 min of a game
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
                if( m_flagOwner[0] == helpOutArmy && m_flagOwner[1] == helpOutArmy ) {
                    // If they're holding both flags, they don't need help.
                    // However, don't reset assist advert time so that we check again in 20sec
                    // if someone leaves or switches ships.
                    checkForAssistAdvert = false;
                    return;
                }
                if( helpOutArmy != -1 ) {
                    // Attempt to return an assister who is needed back on freq;
                    // if none, advert an army imbalance.
                    boolean helped = false;
                    int maxStrToAssist = (int)Math.abs(armyStr0 - armyStr1) - 1;
                    for( DistensionPlayer p : m_players.values() )
                        if( !helped && p.isAssisting() && p.getNaturalArmyID() == helpOutArmy ) {
                            if( p.getStrength() <= maxStrToAssist ) {
                                cmdAssist( p.getName(), ":auto:" );
                                helped = true;
                            }
                        }
                    if( !helped ) {
                        if( maxStrToAssist > RANK_0_STRENGTH )	// Only display if assisting is possible
                            m_botAction.sendOpposingTeamMessageByFrequency( msgArmy, "IMBALANCE: !pilot lower rank ships, or use !assist " + helpOutArmy + "  (max assist rank: " + (maxStrToAssist - RANK_0_STRENGTH) + ").   [ " + army0.getTotalStrength() + " vs " + army1.getTotalStrength() + " ]");
                    }
                // Check if teams are imbalanced in numbers, if not strength
                } else {
                    if( army0.getPilotsInGame() <= army1.getPilotsInGame() - ASSIST_NUMBERS_IMBALANCE )
                        m_botAction.sendOpposingTeamMessageByFrequency( 0, "NOTICE: Your army has fewer pilots but is close in strength; if you need help, !pilot lower-ranked ships to allow !assist." );
                    else if( army1.getPilotsInGame() <= army0.getPilotsInGame() - ASSIST_NUMBERS_IMBALANCE )
                        m_botAction.sendOpposingTeamMessageByFrequency( 1, "NOTICE: Your army has fewer pilots but is close in strength; if you need help, !pilot lower-ranked ships to allow !assist." );
                }
                lastAssistAdvert = System.currentTimeMillis();
                checkForAssistAdvert = false;
            }
        };
        m_botAction.scheduleTask( assistAdvertTask, 20000, 20000 );

        if( DEBUG ) {
            m_botAction.sendUnfilteredPublicMessage("?find dugwyler" );
            m_botAction.sendChatMessage("Distension BETA initialized.  ?go #distension");
            m_botAction.sendArenaMessage("Distension BETA loaded.  Use !return (~) to return to your current ship or !enlist if you're new.  Please see the beta thread on the forums for bug reports & suggestions.");
            // Reset all times at each load
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fnTime='0' WHERE 1" );
            } catch (SQLException e ) {  }
            m_botAction.scoreResetAll();
        }
    }


    /**
     * Registers all commands necessary.
     *
     */
    private void registerCommands() {
        int acceptedMessages = Message.PRIVATE_MESSAGE;

        // Shortcuts
        m_commandInterpreter.registerCommand( "!", acceptedMessages, this, "cmdHelp" );
        m_commandInterpreter.registerCommand( "?", acceptedMessages, this, "cmdHelp" );
        m_commandInterpreter.registerCommand( "~", acceptedMessages, this, "cmdReturn" );
        m_commandInterpreter.registerCommand( "!a", acceptedMessages, this, "cmdArmory" );
        m_commandInterpreter.registerCommand( "!ar", acceptedMessages, this, "cmdArmies" );
        m_commandInterpreter.registerCommand( "!as", acceptedMessages, this, "cmdAssist" );
        m_commandInterpreter.registerCommand( "!b", acceptedMessages, this, "cmdBeta" );  // BETA CMD
        m_commandInterpreter.registerCommand( "!bi", acceptedMessages, this, "cmdBattleInfo" );
        m_commandInterpreter.registerCommand( "!bw", acceptedMessages, this, "cmdBaseWarp" );
        m_commandInterpreter.registerCommand( "!cm", acceptedMessages, this, "cmdClearMines" );
        m_commandInterpreter.registerCommand( "!d", acceptedMessages, this, "cmdDock" );
        m_commandInterpreter.registerCommand( "!de", acceptedMessages, this, "cmdDefect" );
        m_commandInterpreter.registerCommand( "!e", acceptedMessages, this, "cmdEnlist" );
        m_commandInterpreter.registerCommand( "!h", acceptedMessages, this, "cmdHangar" );
        m_commandInterpreter.registerCommand( "!k", acceptedMessages, this, "cmdKillMsg" );
        m_commandInterpreter.registerCommand( "!l", acceptedMessages, this, "cmdLeave" );
        m_commandInterpreter.registerCommand( "!la", acceptedMessages, this, "cmdLagout" );
        m_commandInterpreter.registerCommand( "!mh", acceptedMessages, this, "cmdModHelp" );
        m_commandInterpreter.registerCommand( "!p", acceptedMessages, this, "cmdPilot" );
        m_commandInterpreter.registerCommand( "!r", acceptedMessages, this, "cmdReturn" );
        m_commandInterpreter.registerCommand( "!s", acceptedMessages, this, "cmdStatus" );
        m_commandInterpreter.registerCommand( "!sc", acceptedMessages, this, "cmdScrap" );
        m_commandInterpreter.registerCommand( "!sca", acceptedMessages, this, "cmdScrapAll" );
        m_commandInterpreter.registerCommand( "!t", acceptedMessages, this, "cmdTerr" );
        m_commandInterpreter.registerCommand( "!tm", acceptedMessages, this, "cmdTeam" );
        m_commandInterpreter.registerCommand( "!u", acceptedMessages, this, "cmdUpgrade" );
        m_commandInterpreter.registerCommand( "!upg", acceptedMessages, this, "cmdUpgrade" );
        m_commandInterpreter.registerCommand( "!ui", acceptedMessages, this, "cmdUpgInfo" );
        m_commandInterpreter.registerCommand( "!w", acceptedMessages, this, "cmdWarp" );
        m_commandInterpreter.registerCommand( "!wh", acceptedMessages, this, "cmdWhereIs" );
        // Full trigger versions
        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "cmdHelp" );
        m_commandInterpreter.registerCommand( "!modhelp", acceptedMessages, this, "cmdModHelp" );
        m_commandInterpreter.registerCommand( "!enlist", acceptedMessages, this, "cmdEnlist" );
        m_commandInterpreter.registerCommand( "!defect", acceptedMessages, this, "cmdDefect" );
        m_commandInterpreter.registerCommand( "!return", acceptedMessages, this, "cmdReturn" );
        m_commandInterpreter.registerCommand( "!leave", acceptedMessages, this, "cmdLeave" );
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
        m_commandInterpreter.registerCommand( "!scrapall", acceptedMessages, this, "cmdScrapAll" );
        m_commandInterpreter.registerCommand( "!intro", acceptedMessages, this, "cmdIntro" );
        m_commandInterpreter.registerCommand( "!warp", acceptedMessages, this, "cmdWarp" );
        m_commandInterpreter.registerCommand( "!basewarp", acceptedMessages, this, "cmdBaseWarp" );
        m_commandInterpreter.registerCommand( "!terr", acceptedMessages, this, "cmdTerr" );
        m_commandInterpreter.registerCommand( "!whereis", acceptedMessages, this, "cmdWhereIs" );
        m_commandInterpreter.registerCommand( "!assist", acceptedMessages, this, "cmdAssist" );
        m_commandInterpreter.registerCommand( "!upginfo", acceptedMessages, this, "cmdUpgInfo" );
        m_commandInterpreter.registerCommand( "!team", acceptedMessages, this, "cmdTeam" );
        m_commandInterpreter.registerCommand( "!clearmines", acceptedMessages, this, "cmdClearMines" );
        m_commandInterpreter.registerCommand( "!lagout", acceptedMessages, this, "cmdLagout" );
        m_commandInterpreter.registerCommand( "!killmsg", acceptedMessages, this, "cmdKillMsg" );
        m_commandInterpreter.registerCommand( "!battleinfo", acceptedMessages, this, "cmdBattleInfo" );
        m_commandInterpreter.registerCommand( "!opshelp", acceptedMessages, this, "cmdOpsHelp" );
        m_commandInterpreter.registerCommand( "!opsmsg", acceptedMessages, this, "cmdOpsMsg" );
        m_commandInterpreter.registerCommand( "!beta", acceptedMessages, this, "cmdBeta" );  // BETA CMD
        m_commandInterpreter.registerCommand( "!msgbeta", acceptedMessages, this, "cmdMsgBeta", OperatorList.HIGHMOD_LEVEL ); // BETA CMD
        m_commandInterpreter.registerCommand( "!grant", acceptedMessages, this, "cmdGrant", OperatorList.HIGHMOD_LEVEL );     // BETA CMD
        m_commandInterpreter.registerCommand( "!info", acceptedMessages, this, "cmdInfo", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!ban", acceptedMessages, this, "cmdBan", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!unban", acceptedMessages, this, "cmdUnban", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!savedata", acceptedMessages, this, "cmdSaveData", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!die", acceptedMessages, this, "cmdDie", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!savedie", acceptedMessages, this, "cmdSaveDie", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!shutdown", acceptedMessages, this, "cmdShutdown", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!db-changename", acceptedMessages, this, "cmdDBChangeName", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!db-wipeship", acceptedMessages, this, "cmdDBWipeShip", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!db-wipeplayer", acceptedMessages, this, "cmdDBWipePlayer", OperatorList.HIGHMOD_LEVEL );  // Not published in !help
        m_commandInterpreter.registerCommand( "!db-randomarmies", acceptedMessages, this, "cmdDBRandomArmies", OperatorList.HIGHMOD_LEVEL );

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
        if( message.equals( "Arena UNLOCKED" ) && !dying )
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
            if( i >= 1 && i <= 9 )      // For lazy pilots
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
                    "    CIVILIAN CONSOLE  ",
                    ".-----------------------",
                    "| !enlist           !e  |  Enlist in the army that needs your services most",
                    "| !armies           !ar |  View all public armies and their IDs",
                    "| !enlist <army#>   !e  |  Enlist specifically in <army#>",
                    "| !intro                |  An introduction to Distension",
                    "| !return           ~   |  Return to your current position in the war",
                    "| !battleinfo       !bi |  Display current battle status",
                    "|______________________/"
            };
            spamWithDelay(p.getArenaPlayerID(), helps);
        } else if( shipNum == 0 ) {
            String[] helps = {
                    "     HANGAR CONSOLE",
                    ".-----------------------",
                    "| !pilot <ship>     !p  |  Pilot <ship> if available in hangar",
                    "| <shipnum>             |  Shortcut for !pilot <shipnum>",
                    "| !hangar           !h  |  View your ships & those available for purchase",
                    "| !leave            !l  |  Leave the battle, opening your position",
                    "| !lagout           !la |  Return to last ship, maintaining participation",
                    "| !assist <army>    !as |  Temporarily assist <army> at no penalty to you",
                    "| !defect <army>    !d  |  Change to <army>.  All ships LOSE A FULL RANK,",
                    "|                       |     unless the new army has 4+ fewer pilots.",
                    "| !warp             !w  |  Toggle waiting in spawn vs. being autowarped out",
                    "| !basewarp         !bw |  Toggle warping into base vs. spawn at round start",
                    "| !killmsg          !k  |  Toggle kill messages on and off (+1% RP for off)",
                    "| !team             !tm |  Show all players on team and their upg. levels",
                    "| !terr             !t  |  Show approximate location of all army terriers",
                    "| !whereis <name>   !wh |  Show approximate location of pilot <name>",
                    "| !armies           !ar |  View size and strength of armies",
                    "| !battleinfo       !bi |  Display current battle status",
                    "|______________________/"
            };
            spamWithDelay(p.getArenaPlayerID(), helps);
        } else {
            String[] helps = {
                    "      PILOT CONSOLE  ",
                    ".-----------------------",
                    "| !progress         .   |  See your progress toward next advancement",
                    "| !status           !s  |  View current ship's level and upgrades",
                    "| !armory           !a  |  View ship upgrades available in the armory",
                    "| !upgrade <upg>    !u  |  Upgrade your ship with <upg> from the armory",
                    "| !upginfo <upg>    !ui |  Shows any available information about <upg>",
                    "| !scrap <upg>      !sc |  Trade in <upg>.  *** Restarts ship at current rank!",
                    "| !scrapall <#/S/M/E/*> |  Scrap all of ...   *: ALL upg        #: Given upg",
                    "|                   !sca|    S: Special upgs  M: Maneuver upgs  E: En/chg upgs",
                    "| !hangar           !h  |  View your ships & those available for purchase",
                    "| !dock             !d  |  Dock your ship, saving all progress",
                    "| !leave            !l  |  Leave the battle, opening your position",
                    "| !pilot <ship>     !p  |  Change to <ship> if available in hangar",
                    "| <shipnum>             |  Shortcut for !pilot <shipnum>",
                    "| !assist <army>    !as |  Temporarily assists <army> at no penalty to you",
                    "| !clearmines       !cm |  Clear all mines, if in a mine-laying ship",
                    "| !warp             !w  |  Toggle waiting in spawn vs. being autowarped out",
                    "| !basewarp         !bw |  Toggle warping into base vs. spawn at round start",
                    "| !killmsg          !k  |  Toggle kill messages on and off (+1% RP for off)",
                    "| !team             !tm |  Show all players on team and their upg. levels",
                    "| !terr             !t  |  Show approximate location of all army terriers",
                    "| !whereis <name>   !wh |  Show approximate location of pilot <name>",
                    "| !armies           !ar |  View size and strength of armies",
                    "| !battleinfo       !bi |  Display current battle status",
                    "|______________________/"
            };
            spamWithDelay(p.getArenaPlayerID(), helps);
        }

        if( shipNum == 9 )
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "!opshelp - Tactical Ops commands." );

        if( m_botAction.getOperatorList().isHighmod(name) )
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "!modhelp - HighMod+ operator commands." );

    }

    /**
     * Display help for HighMod+ operators.
     * @param name
     * @param msg
     */
    public void cmdModHelp( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null ) {
            return;
        }
        String[] helps = {
                "    OPERATOR CONSOLE  ",
                ".-----------------------",
                "| !modhelp          !mh |  This display",
                "| !info <name>          |  Gets info on <name> from their !status screen",
                "| !ban <name>           |  Bans a player from playing Distension",
                "| !unban <name>         |  Unbans banned player",
                "| !shutdown <time>      |  Shuts down bot after <time>, extended to round end",
                "| !savedata             |  Saves all player data to database",
                "| !savedie              |  Saves all player data and runs a delayed !die",
                "| !die                  |  Kills DistensionBot -- use !savedie instead!",
                "|______________________/",
                "    DB COMMANDS",
                "  !db-changename <oldname>:<newname>   - Changes name from oldname to newname.",
                "  !db-wipeship <name>:<ship#>          - Wipes ship# from name's record.",
                "  !db-randomarmies                     - Randomizes all armies."
        };
        spamWithDelay(p.getArenaPlayerID(), helps);
        if( m_botAction.getOperatorList().isSmod(name) ) {
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "Hidden command: !db-wipeplayer <name>    - Wipes all records of a player from DB.");
        }
    }

    /**
     * Display help for Tactical Ops players.
     * @param name
     * @param msg
     */
    public void cmdOpsHelp( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;

        if( p.getShipNum() != 9 ) {
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "You are not at a Tactical Ops console, and have no way to refer to the Ops manual.");
            return;
        }


        if( msg.equals("") ) {
            String[] helps = {
                    "   TACTICAL OPS CONSOLE",
                    ".-----------------------",
                    "| COMMUNICATIONS    Cost|",
                    "|  !opsmsg <#>        1 |  Msg army.  See !opshelp msg for available messages",
                    "|  !opsPM <name>:<#>  1 |  Msg specific players.  See !opshelp msg",
                    "|  !opssab            2 |  Sabotage msg to enemy.  See !opshelp msg",
                    "|  !opsradar          1 |  Shows approx. location of all pilots, + Terr info",
                    "| ACTIONS               |",
                    "|  !opsrearm          1 |  Increase rearm/decrease enemy rearm for short time",
                    "|  !opscover <#>      1 |  Deploy cover in home base.  1:MidLeft 2:MidRight",
                    "|                       |    3:MidCenter  4:FR  5:Ears  6:Tube  7:LowCenter",
                    "|  !opsdoor <#>   1/2/3 |  Close doors.  1:Sides  2:Tube  (L2- 3:FR  4: Flag)",
                    "|                       |  (L3- Enemy doors):  5:Sides  6:Tube  7:FR  8:Flag",
                    "|  !opsmine <#>       2 |  False minefield in home base.  1:MidBase  2:In FR",
                    "|                       |    3:FR Entrance  4:TopTube  5:In Tube  6:LowCenter",
                    "|  !opsorb <name>   2/5 |  Cover enemy with orb.  L2: All NME in base (cost 5)",
                    "|  !opsdark <name>  2/5 |  Cone of darkness.  L2: All NME in base (cost 5)",
                    "|                   3/7 |   L3: Shroud (cost 3).  L4: Shroud all (cost 7)",
                    "|  !opsblind      3/4/5 |  Briefly blind all NME in base.  L2,3: Longer.",
                    "|  !opsshield         5 |  Shield all friendlies in home base.",
                    "|  !opsemp            6 |  Quick EMP all enemies in home base to 0 energy.",
                    "|______________________/",
            };
            spamWithDelay(p.getArenaPlayerID(), helps);
        } else if( msg.equalsIgnoreCase("msg") ) {
            String[] helps = {
                    "      OPS MESSAGES  -  Each uses 1 COMM, +1 if msg is a sabotage",
                    ".-----------------------------------------------------------------.",
                    "| TIER 1: !opsmsg #                    | To all army pilots       |",
                    "| TIER 2: !opsPM name:#  (T/S for name)| To 1 pilot/Terrs/Sharks  |",
                    "| TIER 3: !opssab msg|PM #|name:#      | Msg or PM sent to enemy  |",
                    ".______________________________________|_________________________/",
                    "| !opsmsg 1             |  Defend/assault top base (friend/foe #s, name of terr)",
                    "|         2             |  Defend/assault bottom base",
                    "|         3             |  Terr needed ASAP; requesting change of ships",
                    "|         4             |  Shark needed ASAP; requesting change of ships",
                    "| !opsPM <name>:1       |  (To individual) Order to secure and hold top base",
                    "|               2       |  (To individual) Order to secure and hold bottom base",
                    "| !opsPM T:1            |  (To all Terrs)  Terr needed at top base immediately",
                    "|          2            |  (To all Terrs)  Terr needed at bottom base immediately",
                    "| !opsPM S:1            |  (To all Sharks) Shark needed at top base immediately",
                    "|          2            |  (To all Sharks) Shark needed at bottom base immediately",
                    "| !opssab               |  Works like above commands but sends to enemy army. Ex:",
                    "|                       |  '!opssab msg 2' sends !opsmsg 2 to enemy w/ fake data.",
                    "|                       |  (False pilot counts, says there is no Terr, etc.)",
                    "|______________________/",
            };
            spamWithDelay(p.getArenaPlayerID(), helps);
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
        // If mid-round in a flag game, show appropriate flag info
        if( flagTimeStarted && flagTimer != null && flagTimer.isRunning() ) {
        	String flagString = "";
        	switch( m_flagOwner[0] ) {
        	case -1:
        		flagString += "+" + LVZ_TOPBASE_EMPTY + ",";
        		break;
        	case 0:
        		flagString += "+" + LVZ_TOPBASE_ARMY0 + ",";
        		break;
        	case 1:
        		flagString += "+" + LVZ_TOPBASE_ARMY1 + ",";
        		break;
        	}
        	switch( m_flagOwner[1] ) {
        	case -1:
        		flagString += "+" + LVZ_BOTBASE_EMPTY + ",";
        		break;
        	case 0:
        		flagString += "+" + LVZ_BOTBASE_ARMY0 + ",";
        		break;
        	case 1:
        		flagString += "+" + LVZ_BOTBASE_ARMY1 + ",";
        		break;
        	}
        	if( flagTimer.getSecondsHeld() > 0 )
        		flagString += "+" + LVZ_SECTOR_HOLD + ",";
        	m_botAction.manuallySetObjects(flagString, event.getPlayerID());
        }
        DistensionPlayer p = new DistensionPlayer(name);
        m_players.put( name, p );
        // Properly set up last ship player was in, if that info is available
        if( m_lagShips.containsKey( name ) ) {
            p.lastShipNum = m_lagShips.remove( name );
        }
        if( flagTimer != null && flagTimer.isRunning() ) {
            Integer lagTime = m_lagouts.get( name );
            if( lagTime != null && p.canUseLagout() ) {
                int diff = lagTime + LAGOUT_VALID_SECONDS - flagTimer.getTotalSecs();
                if( diff > 0 )
                    m_botAction.sendPrivateMessage( name, "!return and then !lagout in the next " + diff + " seconds to return to battle and keep participation." );
            }
        }
    }


    /**
     * Save player data if unexpectedly leaving Distension.
     * @param event Event to handle.
     */
    public void handleEvent(PlayerLeft event) {
        String name = m_botAction.getPlayerName(event.getPlayerID());
        DistensionPlayer player = m_players.get( name );
        for( DistensionArmy a : m_armies.values() )
            a.recalculateFigures();
        if( player == null )
            return;
        if( player.getShipNum() > 0 ) {
            checkFlagTimeStop();
            if( System.currentTimeMillis() > lastAssistAdvert + ASSIST_REWARD_TIME )
                checkForAssistAdvert = true;
            if( flagTimer != null && flagTimer.isRunning() && player.canUseLagout() ) {
                m_lagouts.put( name, new Integer(flagTimer.getTotalSecs()) );
                m_lagShips.put( name, player.getShipNum() );
            }
        } else {
            m_lagouts.remove(name);
            m_lagShips.remove(name);
        }
        player.saveCurrentShipToDBNow();
        player.savePlayerTimeToDB();
        m_specialAbilityPrizer.removePlayer( player );
        m_waitingToEnter.remove( player );
        doSwapOut( player, false );
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
        boolean holdSecuring = false;
        boolean flagTimeRunning = (flagTimer != null && flagTimer.isRunning());
        int oldOwner = m_flagOwner[flagID];
        m_flagOwner[flagID] = p.getFrequency();

        // If flag is already claimed, try to take it away from old freq
        if( oldOwner != -1 ) {
            DistensionArmy army = m_armies.get( new Integer( oldOwner ) );
            if( army != null ) {
                if( army.getNumFlagsOwned() == 2 && flagTimer != null && flagTimer.isRunning())
                    holdBreaking = true;
                army.adjustFlags( -1 );
                if( flagTimeRunning ) {
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
            }
        } else {
            if( flagTimeRunning ) {
                if( flagID == 0 )
                    flagObjs.hideObject(LVZ_TOPBASE_EMPTY);
                else
                    flagObjs.hideObject(LVZ_BOTBASE_EMPTY);
            }
        }
        DistensionArmy army = m_armies.get( new Integer( p.getFrequency() ) );
        if( army != null ) {
            army.adjustFlags( 1 );
            if( flagTimeRunning ) {
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
                holdSecuring = true;
        }
        if( !flagTimeRunning )
            return;

        m_botAction.manuallySetObjects( flagObjs.getObjects() );
        if( holdBreaking )
            flagTimer.holdBreaking( army.getID(), p.getPlayerName() );
        if( holdSecuring )
            flagTimer.sectorClaiming( army.getID(), p.getPlayerName() );
    }


    /**
     * Dock player if they spec.
     * @param event Event to handle.
     */
    public void handleEvent(FrequencyShipChange event) {
        if( !readyForPlay )         // If bot has not fully started up,
            return;                 // don't operate normally when speccing players.
        DistensionPlayer p = m_players.get( m_botAction.getPlayerName( event.getPlayerID() ) );
        if( event.getShipType() == 0 && p != null ) {
            doDock( p );
            if( flagTimer != null && flagTimer.isRunning() ) {
                if( p.canUseLagout() ) {    // Essentially: did player spec or !dock (not !leave)
                    if ( !m_waitingToEnter.isEmpty() ) {
                        doSwapOut( p, true );
                    } else {
                        m_lagouts.put( p.getName(), flagTimer.getTotalSecs() );
                        m_botAction.sendPrivateMessage( p.getName(), "Use !lagout in the next " + LAGOUT_VALID_SECONDS + " seconds to return to battle and keep participation." );
                    }
                }

            }
        }
        if( p != null )
            m_botAction.hideObjectForPlayer(p.getArenaPlayerID(), LVZ_REARMING);

        if( System.currentTimeMillis() > lastAssistAdvert + ASSIST_REWARD_TIME )
            checkForAssistAdvert = true;
    }

    /**
     * Deprize Lanc bombs when attached, and reprize when detaching.
     * @param event Event to handle.
     */
    public void handleEvent(TurretEvent event) {
        DistensionPlayer p = m_players.get( m_botAction.getPlayerName( event.getAttacherID() ) );
        if( p != null )
            p.checkLancAttachEvent( event.isAttaching() );
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
            boolean isTeK = (loser.getShipNum() == Tools.Ship.TERRIER);
            boolean isBTeK = false;
            if( isTeK ) {
                Player p = m_botAction.getPlayer( loser.getArenaPlayerID() );
                if( p.getYTileLocation() <= TOP_FR || p.getYTileLocation() >= BOT_FR )
                    isBTeK = true;
            }
            boolean isSharkK = (loser.getShipNum() == Tools.Ship.SHARK);
            boolean isMaxReward = false;
            boolean isMinReward = false;
            boolean isRepeatKillLight = false;
            boolean isRepeatKillHard = false;
            boolean isFirstKill = (victor.getRecentlyEarnedRP() == 0);
            boolean endedStreak = false;

            // IF TK: TKer loses points equal to half their level, and they are notified
            // of it if they have not yet been notified this match.  Successive kills are
            // also cleared.
            if( killed.getFrequency() == killer.getFrequency() ) {
                float div;
                // Sharks get off a little easier for TKs
                if( killer.getShipType() == Tools.Ship.SHARK || loser.getShipNum() == Tools.Ship.WARBIRD )
                    div = 8.0f;
                else {
                    if( loser.isSupportShip() )
                        div = 1.0f;
                    else
                        div = 2.0f;
                }
                // If lowbies get TKd, it shouldn't hurt as much, because ... well, it's easy to TK them.
                if( loser.getRank() < 10 )
                    div *= 1.5f;
                int loss = Math.round((float)victor.getRank() / div);
                victor.addRankPoints( -loss );
                victor.clearSuccessiveKills();
                if( loss > 0 && victor.wantsKillMsg() )
                    m_botAction.sendPrivateMessage( killer.getPlayerName(), "-" + loss + " RP for TKing " + killed.getPlayerName() + "." );
                m_botAction.showObjectForPlayer(victor.getArenaPlayerID(), LVZ_TK);
                m_botAction.showObjectForPlayer(loser.getArenaPlayerID(), LVZ_TKD);
            } else {
                // Otherwise: Add points via level scheme
                DistensionArmy killerarmy = m_armies.get( new Integer(killer.getFrequency()) );
                DistensionArmy killedarmy = m_armies.get( new Integer(killed.getFrequency()) );
                if( killerarmy == null || killedarmy == null )
                    return;
                endedStreak = loser.clearSuccessiveKills();
                int points;
                int loserRank = Math.max( 1, loser.getRank() );
                int victorRank = Math.max( 1, victor.getRank() );
                int rankDiff = loserRank - victorRank;

                // Loser is many levels above victor:
                //   Victor may have a cap, but loser is humiliated with some point loss
                if( rankDiff >= RANK_DIFF_MED ) {

                    if( rankDiff >= RANK_DIFF_HIGH ) {
                        points = victorRank + RANK_DIFF_HIGH;
                        isMaxReward = true;
                    } else
                        points = loserRank;

                    // Support ships are not humiliated; assault are
                    if( ! loser.isSupportShip() ) {
                        int loss = 0;
                        if( rankDiff >= RANK_DIFF_HIGHEST )
                            loss = loserRank;
                        else if( rankDiff >= RANK_DIFF_VHIGH )
                            loss = points;
                        else if( rankDiff >= RANK_DIFF_HIGH )
                            loss = (points / 2);
                        else
                            loss = (points / 3);    // Default
                        if( points > 0 ) {
                            loser.addRankPoints( -loss );
                            if( loser.wantsKillMsg() )
                                m_botAction.sendPrivateMessage(loser.getArenaPlayerID(), "HUMILIATION!  -" + loss + "RP for being killed by " + victor.getName() + "(" + victor.getRank() + ")");
                        }
                    }

                    // Loser is 10 or more levels below victor: victor gets fewer points
                } else if( rankDiff <= -RANK_DIFF_LOW ) {
                    isMinReward = true;
                    if( rankDiff <= -RANK_DIFF_HIGHEST )
                        points = 1;
                    else if( rankDiff <= -RANK_DIFF_VHIGH )
                        points = loserRank / 4;
                    else if( rankDiff <= -RANK_DIFF_HIGH )
                        points = loserRank / 3;
                    else if( rankDiff <= -RANK_DIFF_MED )
                        points = loserRank / 2;
                    else
                        points = (int)(loserRank / 1.5f);

                    // Normal kill:
                    //   Victor earns the rank of the loser in points.  Level 0 players are worth 1 point.
                } else {
                    points = loser.getRank();
                }

                // Points adjusted based on size of victor's army v. loser's
                float armySizeWeight;
                float killedArmyStr = killedarmy.getTotalStrength();
                float killerArmyStr = killerarmy.getTotalStrength();
                if( killedArmyStr <= 0 ) killedArmyStr = 1;
                if( killerArmyStr <= 0 ) killerArmyStr = 1;
                armySizeWeight = killedArmyStr / killerArmyStr;
                if( armySizeWeight > 3.0f )
                    armySizeWeight = 3.0f;
                else if( armySizeWeight < 0.2f )
                    armySizeWeight = 0.2f;

                float flagMulti = killerarmy.getNumFlagsOwned();
                if( flagMulti == 0f ) {
                    if( armySizeWeight > ASSIST_WEIGHT_IMBALANCE ) {
                        flagMulti = 0.5f;
                    } else {
                        // Reduced RP for 0 flag rule doesn't apply if armies are imbalanced.
                        flagMulti = 1;
                    }
                } else if( flagMulti == 2f ) {
                    flagMulti = 1.5f;
                }

                points = Math.round(((float)points * armySizeWeight));
                points = (int)((float)points * flagMulti);

                if( killedarmy.getPilotsInGame() != 1 ) {
                    switch( victor.getRepeatKillAmount( event.getKilleeID() ) ) {
                        case 3:
                            points /= 2;
                            isRepeatKillLight = true;
                            if( victor.wantsKillMsg() )
                                m_botAction.sendPrivateMessage( victor.getArenaPlayerID(), "For repeatedly killing " + loser.getName() + " you earn only half the normal amount of RP." );
                            break;
                        case 4:
                            points = 1;
                            isRepeatKillHard = true;
                            if( victor.wantsKillMsg() )
                                m_botAction.sendPrivateMessage( victor.getArenaPlayerID(), "For repeatedly killing " + loser.getName() + " you earn only 1 RP." );
                            break;
                    }
                }

                if( isTeK ) {
                    if( isBTeK )
                        points = Math.round((float)points * 1.50f);
                    else
                        points = Math.round((float)points * 1.10f);
                } else if( isSharkK )
                    points -= Math.round((float)points * 0.20f);

                // Track successive kills for weasel unlock & streaks
                if( rankDiff > -STREAK_RANK_PROXIMITY ) {   // Streaks only count players close to your lvl
                    if( victor.addSuccessiveKill() ) {
                        // If player earned weasel off this kill, check if loser/killed player has weasel ...
                        // and remove it if they do!
                    }
                }

                if( endedStreak )
                    points = Math.round((float)points * 1.50f);

                if( points < 1 )
                    points = 1;
                // Check if player ranked up from the kill
                if( victor.addRankPoints( points ) ) {
                    // ... and taunt loser if he/she did
                    if( loser.wantsKillMsg() )
                        m_botAction.sendPrivateMessage( loser.getArenaPlayerID(), "INSULT TO INJURY: " + victor.getName() + " just ranked up from your kill!", Tools.Sound.CRYING );
                }
                victor.getArmy().addSharedProfit( points );

                if( ! victor.wantsKillMsg() )
                    return;

                if( DEBUG )     // For DISPLAY purposes only; intentionally done after points added.
                    points = Math.round((float)points * DEBUG_MULTIPLIER);
                String msg = "KILL: " + points + " RP - " + loser.getName() + "(" + loser.getRank() + ")";
                if( isMinReward )
                    msg += " [Low rank kill: CAP]";
                else if( isMaxReward )
                    msg += " [High rank kill: CAP]";
                if( isRepeatKillLight )
                    msg += " [Repeat kill: -50%]";
                else if( isRepeatKillHard )
                    msg += " [Repeat kill: 1 RP]";
                if( isTeK )
                    if( isBTeK )
                        msg += " [Base Terr: +50%]";
                    else
                        msg += " [Terr: +10%]";
                else if( isSharkK )
                    msg += " [Shark: -20%]";
                if( flagMulti == 1.5f )
                    msg += " [SECTOR HOLD: +50% RP]";
                if( flagMulti == 0.5f )
                    msg += " [No flags: -50%]";
                if( endedStreak )
                    msg += " [Ended a streak: +50%]";
                if( DEBUG )     // For DISPLAY purposes only; intentionally done after points added.
                    msg += " [x" + DEBUG_MULTIPLIER + " beta]";
                if( isFirstKill )
                    msg += " (!killmsg turns off this msg & gives +1% kill bonus)";
                m_botAction.sendPrivateMessage(victor.getName(), msg);
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
        DistensionPlayer p = m_players.get( name );
        if( p == null ) {
            return;
        }
        String[] intro1 = {
                "DISTENSION - The Trench Wars RPG - by G. Dugwyler",
                "Presently in beta.  Intro to come soon.  Type !beta for info on recent updates."
        };
        spamWithDelay(p.getArenaPlayerID(), intro1);
    }


    /**
     * Shows what prizes are associated with the basic upgrades.
     * @param name
     * @param msg
     */
    public void cmdUpgInfo( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null || p.getShipNum() == -1 ) {
            m_botAction.sendPrivateMessage( name, "Sorry, I don't recognize you.  If you !return to your army, maybe I can help." );
            return;
        }
        int ship = p.getShipNum();

        if( ship < 1 ) {
            m_botAction.sendPrivateMessage( name, "Sorry.  You'll need to be in a ship or I can't tell you a damn thing." );
            return;
        }

        Integer upgNum = 0;
        try {
            upgNum = Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            m_botAction.sendPrivateMessage( name, "What upgrade do you want info on?  Do I look like a mind-reader?  Check the !armory before you start asking ..." );
            return;
        }

        if( upgNum < 1 || upgNum > NUM_UPGRADES ) {
            m_botAction.sendPrivateMessage( name, "What the hell upgrade is that?  You check the !armory -- find out that you're making this crap up ..." );
            return;
        }

        String desc = getUpgradeText(m_shipGeneralData.get(ship).getUpgrade(upgNum - 1).getPrizeNum());
        if( desc == "" )
            m_botAction.sendPrivateMessage( name, "Upgrade #" + upgNum + ": Can not find information!  Notify a mod immediately." );
        else
            m_botAction.sendPrivateMessage( name, "Upgrade #" + upgNum + ": " + desc );
    }


    /**
     * Provides a brief introduction to the game for a player.  This should support
     * the A1 LVZ help, and the F1 help.
     * @param name
     * @param msg
     */
    public void cmdBeta( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null ) {
            return;
        }
        String[] beta = {
                "BETA INFO - BASIC",
                "-----------------",
                " - Data is saved, but will be cleared at release (coming soon)",
                " - Top 3 players (combined earned RP) awarded bonus points in public release",
                " - For every bug reported, points will be awarded (?message dugwyler)",
                ".",
                "RECENT UPDATES  -  12/20/07",
                " - !! Converted upgrade system from 1UP/rank to 10UP/rank.  See new costs !!",
                "   NOTE: All upgrades were refunded; you will need to buy all upgrades again!",
                " - Fixed rotation bug!",
                " - Due to the new upgrade system, !scrap will be free for a while.  Merry Kwanza!",
                " - Rank 25+ to-next-rank amount required reduced yet again",
                " - !scrap now free for guns/bombs/multi",
                " - End round bonuses less round-time- dependent.",
                " - Adjustment of WB toward a sniper ship which will not do well in close quarters",
                " - !scrap within 15% of start of rank is free; >15% returns you to 15%",
                " - !scrapall to scrap all of a specific upgrade, all upgrades, etc.",
                " - Serious tweaking of upgrades to encourage more speed-based ships",
        };
        spamWithDelay( p.getArenaPlayerID(), beta );
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
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fnArmyID FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name  ) +"'" );
            if( r.next() ) {
                m_botAction.sendPrivateMessage( name, "Enlist?  You're already signed up!  I know you: you can !return any time.  After that, maybe you'd like to !defect to an army more worthy of your skills?");
                m_botAction.SQLClose(r);
                return;
            }
            m_botAction.SQLClose(r);
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
        m_botAction.sendPrivateMessage( name, "Welcome aboard.  If you need an !intro to how things work, I'd be glad to !help out.  Or if you just want to get some action, jump in your new Warbird.  (!pilot 1)" );
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

        // For those defecting to an army w/ close to the same # pilots, !defect yes to confirm loss of 1 rank in every ship
        if( msg.equals("yes") ) {
            if( !m_defectors.containsKey(name) )
                return;
            DistensionArmy army = m_armies.get( m_defectors.get(name) );
            if( army == null ) {
                m_botAction.sendPrivateMessage( name, "Can't find the army to which you want to defect!  May want to contact a mod about this one ..." );
                return;
            }
            try {
                String query = "SELECT fnPlayerID, fnShipNum, fnRank FROM tblDistensionShip WHERE fnPlayerID='" + p.getID() + "'";
                ResultSet r = m_botAction.SQLQuery( m_database, query );
                if( r != null ) {
                    while( r.next() ) {
                        int ship = r.getInt("fnShipNum");
                        m_botAction.SQLQueryAndClose(m_database, "UPDATE tblDistensionShip SET fnRankPoints='" + m_shipGeneralData.get(ship).getNextRankCost(r.getInt("fnRank") - 2) + "'"+
                                " WHERE fnShipNum='" + ship + "' AND fnPlayerID='" + p.getID() + "'");
                    }
                }
                m_botAction.SQLClose(r);
            } catch (SQLException e ) {
                m_botAction.sendPrivateMessage( name, DB_PROB_MSG );
                return;
            }
            p.doDefect( army.getID() );
            m_botAction.sendPrivateMessage( name, "OK -- the ship controls are similar, but you're going to have a lot to relearn ...  Anyway, should be all set.  Good luck." );
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

        boolean weak = (oldarmy.getPilotsTotal() > army.getPilotsTotal() + 3 );

        // Free !defect if army has less pilots
        if( weak ) {
            p.doDefect( armyNum );
            m_botAction.sendPrivateMessage( name, "The ship controls are mostly the same... but since you're defecting to a weaker army, hell -- I'll train you right quick!  OK, should be all set.  Good luck." );
        } else {
            m_defectors.remove(name);
            m_defectors.put(name, new Integer(armyNum) );
            m_botAction.sendPrivateMessage( name, "So you want to betray them all, eh?  That can be arranged ... but it won't be easy.  You'll lose a full rank in every ship you fly, plus any progress you've made to the next rank." );
            m_botAction.sendPrivateMessage( name, "Sorry that it has to be this way ... but they're not going to trust a traitor just yet.  If this arrangement is acceptable, we'll get started.  (PM !defect yes TO ACCEPT)" );
        }
    }


    /**
     * Logs a player in / allows them to return to the game.
     * @param name
     * @param msg
     */
    public void cmdReturn( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null ) {
            p = new DistensionPlayer(name);
            m_players.put( name, p );
        }

        int ship = p.getShipNum();
        if( ship != -1 ) {
            if( ship == 0 )
                m_botAction.sendPrivateMessage( name, "You are currently docked at " + p.getArmyName().toUpperCase() + " HQ.  You may !pilot a ship at any time.  You may !leave to record all data and stop the battle timer." );
            else
                m_botAction.sendPrivateMessage( name, "You are currently in flight." );
            return;
        }

        if( m_waitingToEnter.contains(p) ) {
            m_botAction.sendPrivateMessage( name, "You're already in the queue to enter in to the battle." );
            return;
        }

        if( !p.getPlayerFromDB() ) {
            if( p.isBanned() ) {
                m_botAction.sendPrivateMessage( name, "ERROR: Civilians and discharged pilots are NOT authorized to enter this military zone." );
            } else {
                m_botAction.sendPrivateMessage( name, "PM me with !enlist if you need to enlist in an army.  Or use !armies to see your army choices, and !enlist # to enlist in a specific army. -" + m_botAction.getBotName() );
            }
            return;
        }

        int players = 0;
        int highestTime = 0;
        LinkedList <DistensionPlayer>dockedPlayers = new LinkedList<DistensionPlayer>();
        for( DistensionPlayer p2 : m_players.values() ) {
            if( p2.getShipNum() >= 0 ) {
                players++;
                if( p2.getShipNum() == 0 )
                    dockedPlayers.add(p2);
                if( p2.getMinutesPlayed() > highestTime )
                    highestTime = p2.getMinutesPlayed();
            }
        }
        if( players >= MAX_PLAYERS ) {
            // If a player is docked and another player wants to get in, swap them out, no matter the time
            if( !dockedPlayers.isEmpty() ) {
                DistensionPlayer leavingPlayer = dockedPlayers.getFirst();
                cmdLeave(leavingPlayer.getName(), "");
                m_botAction.sendPrivateMessage( leavingPlayer.getName(), "Another player wishes to enter the game; you have been removed to allow them to play." );
            } else {
                if( p.getMinutesPlayed() >= highestTime ) {
                    m_botAction.sendPrivateMessage( name, "Sorry, the battle is at maximum capacity and you've flown more today than anyone else here.  Try again later." );
                } else {
                    m_botAction.sendPrivateMessage( name, "Pilot limit reached: " + MAX_PLAYERS + ".  You are in the queue to replace the pilot who has flown the longest at the end of the battle." );
                    m_waitingToEnter.add(p);
                }
                return;
            }
        }

        m_botAction.sendPrivateMessage( name, p.getName().toUpperCase() + " authorized as a pilot of " + p.getArmyName().toUpperCase() + ".  Returning you to HQ." );
        p.setShipNum( 0 );
    }


    /**
     * Logs a player out, saves their time information, and opens their slot for another player.
     * @param name
     * @param msg
     */
    public void cmdLeave( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        p.setLagoutAllowed(false);

        if( p.getShipNum() == -1 ) {
            m_botAction.sendPrivateMessage( name, "You are not currently in the battle.  Use !return to return to your army." );
            return;
        } else if( p.getShipNum() > 0 ) {
            m_botAction.specWithoutLock( name );
        }

        m_botAction.sendPrivateMessage( name, p.getName().toUpperCase() + " leaving hangars of " + p.getArmyName().toUpperCase() + ".  Your flight timer has stopped." );
        p.setShipNum( -1 );
        p.savePlayerTimeToDB();
    }


        /**
     * Enters a player as a given ship.
     * @param name
     * @param msg
     */
    public void cmdPilot( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;

        if( p.getShipNum() == -1 ) {
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

        if( p.getShipNum() == shipNum ) {
            m_botAction.sendPrivateMessage( name, "You're already in that ship." );
            return;
        }

        if( !p.shipIsAvailable( shipNum ) ) {
            m_botAction.sendPrivateMessage( name, "You don't own that ship.  Check your !hangar before you try flying something you don't have." );
            return;
        }

        if( p.isRespawning() ) {
            m_botAction.sendPrivateMessage( name, "Please wait until your current ship is rearmed before attempting to pilot a new one." );
            return;
        }

        // Check if Tactical Ops position is available
        if( shipNum == 9 ) {
            for( DistensionPlayer p2 : m_players.values() )
                if( p2.getShipNum() == 9 && p2.getArmyID() == p.getArmyID() ) {
                    m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Sorry, " + p2.getName() + " is already sitting at the Tactical Ops console." );
                    return;
                }
        }

        if( p.getShipNum() > 0 ) {
            String shipname = (p.getShipNum() == 9 ? "Tactical Ops" : Tools.shipName(p.getShipNum()));
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Total earned in " + shipname + ": " + p.getRecentlyEarnedRP() + " RP" );
            if( ! p.saveCurrentShipToDBNow() ) {
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "PROBLEM SAVING SHIP BEFORE CHANGE -- Notify a member of staff immediately." );
                return;
            }
            // Simple fix to cause sharks and terrs to not lose MVP
            if( shipNum == Tools.Ship.TERRIER || shipNum == Tools.Ship.SHARK ) {
                if( flagTimer != null && flagTimer.isRunning() )
                    if( flagTimer.getHoldingFreq() == p.getArmyID() && flagTimer.getSecondsHeld() > 0 ) {
                        // If player is changing to a support ship while their freq is securing a hold,
                        // they're probably just doing it to steal the points; don't keep MVP
                        m_botAction.sendPrivateMessage( name, "You changed to a support ship, but your participation has still been reset, as you presently have a sector hold." );
                        m_playerTimes.remove( name );
                    } else {
                        m_botAction.sendPrivateMessage( name, "For switching to a needed support ship, your participation counter has not been reset." );
                    }
            } else
                m_playerTimes.remove( name );
        } else {
            m_playerTimes.remove( name );
        }

        p.setShipNum( shipNum );
        if( !p.getCurrentShipFromDB() ) {
            m_botAction.sendPrivateMessage( name, "Having trouble getting that ship for you.  Please contact a mod." );
            p.setShipNum( 0 );
            return;
        }

        for( DistensionArmy a : m_armies.values() )
            a.recalculateFigures();
        p.putInCurrentShip();
        p.prizeUpgradesNow();
        m_lagouts.remove( name );
        m_lagShips.remove( name );
        p.setLagoutAllowed(true);
        if( flagTimer != null && flagTimer.isRunning() ) {
            if( m_playerTimes.get( name ) == null )
                m_playerTimes.put( name, new Integer( flagTimer.getTotalSecs() ) );
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
     * @param p Player to dock
     */
    public void doDock( DistensionPlayer p ) {
        if( p == null ) {
            m_botAction.sendArenaMessage( "Dock did not find player!  Status not saved." );
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();
            return;
        }
        DistensionArmy army = p.getArmy();
        if( army != null ) {
            army.recalculateFigures();
            p.setAssist(-1);
        } else {
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();
            p.assistArmyID = -1;
        }
        checkFlagTimeStop();
        if( p.saveCurrentShipToDBNow() ) {
            String shipname = (p.getShipNum() == 9 ? "Tactical Ops" : Tools.shipName(p.getShipNum()));
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Ship status logged to our records.  Total earned in " + shipname + ": " + p.getRecentlyEarnedRP() + " RP.  You are now docked.");
            p.setShipNum( 0 );
        } else {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Ship status was NOT logged.  Please notify a member of staff immediately!");
        }
    }


    /**
     * Shows list of public armies with their current counts and enlistment bonuses.
     * @param name
     * @param msg
     */
    public void cmdArmies( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        boolean inGame;
        if( p == null )
            inGame = false;
        else
            inGame = !(p.getShipNum() == -1);

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
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;

        int currentShip = p.getShipNum();
        if( currentShip == -1 ) {
            m_botAction.sendPrivateMessage( name, "You about to !return to your army, or do you need to !enlist in one?  Don't recognize you quite." );
            return;
        }

        HashMap <Integer,Integer>shipRanks = new HashMap<Integer,Integer>();
        try {
            String query = "SELECT fnShipNum, fnRank FROM tblDistensionShip WHERE fnPlayerID='" + p.getID() + "'";
            ResultSet r = m_botAction.SQLQuery( m_database, query );
            if( r != null ) {
                while( r.next() )
                    shipRanks.put( r.getInt("fnShipNum"), r.getInt("fnRank") );
                m_botAction.SQLClose(r);
            }
        } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); }


        ShipProfile pf;
        Integer rank = 0;
        for( int i = 1; i < 10; i++ ) {
            String shipname = Tools.formatString( ( i == 9 ? "Tactical Ops" : Tools.shipName(i) ), 20 );
            if( p.shipIsAvailable(i) ) {
                if( currentShip == i ) {
                    if( i != 9 )
                        m_botAction.sendPrivateMessage( name, "  " + i + "   " + shipname + "IN FLIGHT: Rank " + p.getRank() + " (" + p.getUpgradeLevel() + " upg)" );
                    else
                        m_botAction.sendPrivateMessage( name, "  " + i + "   " + shipname + "AT CONSOLE: Rank " + p.getRank() + " (" + p.getUpgradeLevel() + " upg)" );
                } else {
                    rank = shipRanks.get(i);
                    if( rank != null ) {
                        m_botAction.sendPrivateMessage( name, "  " + i + "   " + shipname + "HANGAR: Rank " + rank );
                    } else {
                        m_botAction.sendPrivateMessage( name, "  " + i + "   " + shipname + "HANGAR: Rank unknown" );
                    }
                }
            } else {
                pf = m_shipGeneralData.get(i);
                if( pf.getRankUnlockedAt() == -1 )
                    m_botAction.sendPrivateMessage( name, "  " + i + "   " + shipname + "LOCKED" );
                else
                    m_botAction.sendPrivateMessage( name, "  " + i + "   " + shipname + "RANK " + pf.getRankUnlockedAt() + " NEEDED"  );
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
     * Shows current status of ship.
     * @param name
     * @param msg
     * @param mod Mod to give info to (rather than player)
     */
    public void cmdStatus( String name, String msg, String mod ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        String theName;
        if( mod != null )
            theName = mod;
        else
            theName = name;
        int shipNum = p.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( theName, "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );
            return;
        } else if( shipNum == 0 ){
            m_botAction.sendPrivateMessage( theName, p.getName().toUpperCase() + ": DOCKED.  [Your data is saved; safe to leave arena.]" );
            return;
        }
        String shipname = ( shipNum == 9 ? "Tactical Ops" : Tools.shipName(shipNum) );
        m_botAction.sendPrivateMessage( theName, p.getName().toUpperCase() + " of " + p.getArmyName().toUpperCase() + ":  " + shipname.toUpperCase() +
                " - RANK " + p.getRank() );

        m_botAction.sendPrivateMessage( theName, "Total " + p.getRankPoints() + " RP earned; " + p.getRecentlyEarnedRP() + " RP earned this session." );
        m_botAction.sendPrivateMessage( theName,  p.getUpgradeLevel() + " upgrades installed.  " + p.getUpgradePoints() + " UP available for further upgrades." );
        if( flagTimer != null && flagTimer.isRunning() ) {
            float secs = flagTimer.getTotalSecs();
            Integer inttime = m_playerTimes.get( p.getName() );
            if( inttime != null ) {
                float time = inttime;
                float percentOnFreq = (secs - time) / secs;
                m_botAction.sendPrivateMessage( theName,  "Current total participation this round: " + (int)(percentOnFreq * 100) + "%" );
            }
       }
        if( shipNum == 9 )
            m_botAction.sendPrivateMessage( theName, "OP:  ( " + p.getCurrentOP() + " / " + p.getMaxOP() + " )   Comm authorizations: " + p.getCurrentComms() );
        if( mod == null )
            cmdProgress( name, msg, null );
        else
            cmdProgress( name, msg, mod );
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
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( mod != null )
            name = mod;
        int shipNum = p.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( name, "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );
            return;
        } else if( shipNum == 0 ) {
            m_botAction.sendPrivateMessage( name, "You must be in-ship to see your progress toward the next rank." );
            return;
        }

        double pointsSince = p.getPointsSinceLastRank();
        double pointsNext = p.getNextRankPointsProgressive();
        int progChars = 0;
        int percent = 0;
        if( pointsSince > 0 ) {
            progChars = (int)(( pointsSince / pointsNext ) * 20);
            percent = (int)(( pointsSince / pointsNext ) * 100 );
        }
        String progString = Tools.formatString("", progChars, "=" );
        progString = Tools.formatString(progString, 20 );
        m_botAction.sendPrivateMessage( name, "Progress:  " + (int)pointsSince + " / " + (int)pointsNext + "  (" + p.getPointsToNextRank() + " to Rank " + (p.getRank() + 1) + ")    [" + progString + "]  " + percent + "%" );
    }


    /**
     * Shows upgrades available for purchase.
     * @param name
     * @param msg
     */
    public void cmdArmory( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        int shipNum = p.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( name, "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );
            return;
        } else if( shipNum == 0 ){
            m_botAction.sendPrivateMessage( name, "If you want to see the armory's selection for a ship, you'll need to !pilot one first." );
            return;
        }

        m_botAction.sendPrivateMessage( name, " #  Name                                  Curr /  Max       UP      Requirements" );
        Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum ).getAllUpgrades();
        ShipUpgrade currentUpgrade;
        int[] purchasedUpgrades = p.getPurchasedUpgrades();
        String printmsg;
        boolean printCost;
        String[] display = new String[NUM_UPGRADES + 1];
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
                    printmsg += Tools.formatString("( " + (purchasedUpgrades[i] < 10 ? " " : "") + purchasedUpgrades[i] + " / " +
                            (currentUpgrade.getMaxLevel() < 10 ? " " : "") + currentUpgrade.getMaxLevel() + " )", 18);
                    if(currentUpgrade.getMaxLevel() == purchasedUpgrades[i]) {
                        printmsg += " [MAX]";
                        printCost = false;
                    }
                }
                if( printCost ) {
                    int cost = currentUpgrade.getCostDefine( purchasedUpgrades[i] );
                    int diff = cost - p.getUpgradePoints();
                    printmsg += Tools.formatString( "" + cost, 11 );
                    int req = currentUpgrade.getRankRequired( purchasedUpgrades[i] );
                    if( req <= p.getRank() ) {
                        if( diff <= 0 )
                            printmsg += "AVAIL!";
                        else
                            printmsg += diff + " more UP" + (diff == 1 ? "" : "s");
                    } else
                        printmsg += "Rank " + (req < 10 ? " " : "") + req;
                }
                display[i] = printmsg;
            }
        }
        display[NUM_UPGRADES] = "RANK: " + p.getRank() + "  UPGRADES: " + p.getUpgradeLevel() + "  UP: " + p.getUpgradePoints()
                                            + (p.getUpgradePoints() == 0?"  (Rank up for +1UP)":"");
        spamWithDelay(p.getArenaPlayerID(), display);
    }


    /**
     * Upgrades a particular aspect of the current ship.
     * @param name
     * @param msg
     */
    public void cmdUpgrade( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        int shipNum = p.getShipNum();
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
            return;
        }
        upgradeNum--;  // Fix number to correspond with internal numbering scheme
        if( upgradeNum < 0 || upgradeNum >= NUM_UPGRADES ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Maybe check the !armory first.  #" + (upgradeNum + 1) + " doesn't work for me." );
            return;
        }
        ShipUpgrade upgrade = m_shipGeneralData.get( shipNum ).getUpgrade( upgradeNum );
        int currentUpgradeLevel = p.getPurchasedUpgrade( upgradeNum );
        if( upgrade == null || currentUpgradeLevel == -1 ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Maybe check the !armory first.  #" + (upgradeNum + 1) + " doesn't work for me." );
            return;
        }
        if( currentUpgradeLevel >= upgrade.getMaxLevel() ) {
            m_botAction.sendPrivateMessage( name, "You've upgraded that one as much as you can." );
            return;
        }
        int req = upgrade.getRankRequired( currentUpgradeLevel );
        if( p.getRank() < req ) {
            m_botAction.sendPrivateMessage( name, "Sorry... I'm not allowed to fit your ship with that until you're at least a rank " + req + " pilot.");
            return;
        }

        int cost = upgrade.getCostDefine( currentUpgradeLevel );
        if( p.getUpgradePoints() < cost ) {
            m_botAction.sendPrivateMessage( name, "You'll need more army upgrade authorization points (UP) before I can fit your ship with that.  And the only way to get those is to go up in rank... by -- you got it! -- killin'." );
            return;
        }
        p.addUpgPoints( -cost );
        p.modifyUpgrade( upgradeNum, 1 );
        boolean prized = true;
        if( upgrade.getPrizeNum() == ABILITY_PRIORITY_REARM )
            p.setFastRespawn(true);
        else {
            if( !p.isRespawning() )
                if( upgrade.getPrizeNum() > 0 ) {
                    // Non-depletable prizes can be prized when upgraded
                    if( upgradeNum < 9 )
                        m_botAction.specificPrize( name, upgrade.getPrizeNum() );
                    else
                        prized = false;
                }
        }
        if( upgrade.getMaxLevel() == 1 ) {
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " [" + getUpgradeText(upgrade.getPrizeNum()) + "] installed.  -" + cost + "UP from army allowance." + (prized?"":"  You will receive this upgrade at next rearm.") );
        } else {
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " [" + getUpgradeText(upgrade.getPrizeNum()) + "] upgraded to Level " + (currentUpgradeLevel + 1) + ".  -" + cost + "UP from army allowance." + (prized?"":"  You will receive this upgrade at next rearm.") );
        }
        if( upgrade.getPrizeNum() == Tools.Prize.GUNS || upgrade.getPrizeNum() == Tools.Prize.BOMBS || upgrade.getPrizeNum() == Tools.Prize.MULTIFIRE )
            m_botAction.sendPrivateMessage( name, "--- IMPORTANT NOTE !! ---: Your new weapon may require too much energy for you to use.  If this is the case, !scrap " + (upgradeNum + 1) + " to return to your old weapon free of charge.");
    }


    /**
     * Scraps a previously-installed upgrade.
     * @param name
     * @param msg
     */
    public void cmdScrap( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        int shipNum = p.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( name, "You'll need to !return to your army before you scrap any upgrades." );
            return;
        } else if( shipNum == 0 ){
            m_botAction.sendPrivateMessage( name, "If you want to remove upgrades on a ship, you'll need to !pilot one first." );
            return;
        }
        if( msg.equals("all") ) {
            // Wrapper for !scrapall *
            cmdScrapAll( name, "*" );
            return;
        }

        int upgradeNum = 0;
        try {
            upgradeNum = Integer.parseInt( msg );
        } catch ( Exception e ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Give me a number.  Maybe check the !armory first." );
            return;
        }
        upgradeNum--;  // Fix number to correspond with internal numbering scheme
        if( upgradeNum < 0 || upgradeNum >= NUM_UPGRADES ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Maybe check the !armory first.  #" + upgradeNum + " doesn't work for me." );
            return;
        }
        ShipUpgrade upgrade = m_shipGeneralData.get( shipNum ).getUpgrade( upgradeNum );
        int currentUpgradeLevel = p.getPurchasedUpgrade( upgradeNum );
        if( upgrade == null || currentUpgradeLevel == -1 ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Maybe check the !armory first.  #" + upgradeNum + " doesn't work for me." );
            return;
        }
        if( currentUpgradeLevel <= 0 ) {
            m_botAction.sendPrivateMessage( name, "You haven't exactly upgraded that, now have you?" );
            return;
        }

        int cost = upgrade.getCostDefine( currentUpgradeLevel - 1);
        p.addUpgPoints( cost );
        p.modifyUpgrade( upgradeNum, -1 );
        if( upgrade.getPrizeNum() == ABILITY_PRIORITY_REARM )
            p.setFastRespawn(false);
        else if( upgrade.getPrizeNum() > 0 )
            m_botAction.specificPrize( name, -upgrade.getPrizeNum() );
        if( upgrade.getMaxLevel() == 1 ) {
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " [" + getUpgradeText(upgrade.getPrizeNum()) + "] removed.  +" + cost + "UP to army allowance." );
        } else {
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " [" + getUpgradeText(upgrade.getPrizeNum()) + "] downgraded to level " + (currentUpgradeLevel - 1) + ".  +" + cost + "UP to army allowance." );
        }
        /* TEMPORARY: FREE SCRAP WHILE PEOPLE ADJUST TO NEW UPGRADE SYSTEM
        // Gun/bomb/multi is a free scrap, as sometimes you can't fire after upgrading it
        if( upgrade.getPrizeNum() == Tools.Prize.GUNS || upgrade.getPrizeNum() == Tools.Prize.BOMBS || upgrade.getPrizeNum() == Tools.Prize.MULTIFIRE ) {
            m_botAction.sendPrivateMessage( name, "No rank progress lost (gun/bomb/multifire scraps are free)." );
        } else {
            int pointsSince = player.getPointsSinceLastRank();
            int fifteenPercentOfRank = player.getRankPointsForPercentage( 15.0f );
            if( pointsSince >= fifteenPercentOfRank ) {
                int points = player.getPointsSinceLastRank() - fifteenPercentOfRank;
                if( points < 0 )
                    points = 0;
                player.addRankPoints( -points );
                m_botAction.sendPrivateMessage( name, "Ship returned to 15% progress; -" + points + "RP." );
            } else {
                m_botAction.sendPrivateMessage( name, "Scrap done within 15% of start of rank; no RP lost." );
            }
        }
        */
    }


    /**
     * Mass-scraps previously-installed upgrades.  Options are all of one slot (#),
     * all maneuver upgrades (m), all special upgrades (s), and every upgrade on the ship (*).
     * @param name
     * @param msg
     */
    public void cmdScrapAll( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        int shipNum = p.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "You'll need to !return to your army before you scrap any upgrades." );
            return;
        } else if( shipNum == 0 ){
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "If you want to remove upgrades on a ship, you'll need to !pilot one first." );
            return;
        }

        msg = msg.toLowerCase();
        int pointsReturned = 0;
        if( msg.equals("m") )
            pointsReturned = massScrap(p, 0, 2);
        else if( msg.equals("e") )
            pointsReturned = massScrap(p, 3, 4);
        else if( msg.equals("s") )
            pointsReturned = massScrap(p, 5, NUM_UPGRADES - 1 );
        else if( msg.equals("*") )
            pointsReturned = massScrap(p, 0, NUM_UPGRADES - 1 );
        else {
            int upgradeNum = 0;
            try {
                upgradeNum = Integer.parseInt( msg );
            } catch ( Exception e ) {
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Try !scrapall with * to scrap all upgrades, # for all of a specific upg, S for all special upgs, M for all maneuver upgs, and E for all energy/recharge upgs." );
                return;
            }
            upgradeNum--;  // Fix number to correspond with internal numbering scheme
            if( upgradeNum < 0 || upgradeNum >= NUM_UPGRADES ) {
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Exactly which do you mean there?  Maybe check the !armory first.  #" + upgradeNum + " doesn't work for me." );
                return;
            }
            ShipUpgrade upgrade = m_shipGeneralData.get( shipNum ).getUpgrade( upgradeNum );
                int currentUpgradeLevel = p.getPurchasedUpgrade( upgradeNum );
            if( upgrade == null || currentUpgradeLevel == -1 ) {
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Exactly which do you mean there?  Maybe check the !armory first.  #" + upgradeNum + " doesn't work for me." );
                return;
            }
            if( currentUpgradeLevel <= 0 ) {
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "You haven't exactly upgraded that, now have you?" );
                return;
            }
            pointsReturned = massScrap( p, upgradeNum, upgradeNum );
        }

        if( pointsReturned > 0 ) {
            int points = p.getPointsSinceLastRank();
            if( points < 0 )
                points = 0;
            //player.addRankPoints( -points );
            p.addUpgPoints( pointsReturned );
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Mass scrap successful.  +" + pointsReturned + "UP returned.  You now have " + p.getUpgradePoints() + "UP." );
            //m_botAction.sendPrivateMessage( name, "Mass scrap successful.  Ship returned to the start of rank; -" + points + "RP.  +" + pointsReturned + "UP returned.  You now have " + player.getUpgradePoints() + "UP." );
        } else {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Mass scrap failed: nothing to scrap!" );
        }
    }


    /**
     * Scraps all upgrades on a player between a starting and ending upgrade number.
     * @param p Player to scrap on
     * @param startingUpg Starting upgrade
     * @param endingUpg Ending upgrade
     * @return Total point value of all upgrades scrapped (add these back to the player)
     */
    public int massScrap( DistensionPlayer p, int startingUpg, int endingUpg ) {
        int pointsReturned = 0;
        for( int i = startingUpg; i < endingUpg + 1; i++ ) {
            ShipUpgrade upgrade = m_shipGeneralData.get( p.getShipNum() ).getUpgrade( i );
            while( p.getPurchasedUpgrade( i ) > 0 ) {
                pointsReturned += upgrade.getCostDefine( p.getPurchasedUpgrade( i ) - 1);
                p.modifyUpgrade( i, -1 );
                if( upgrade.getPrizeNum() == ABILITY_PRIORITY_REARM )
                    p.setFastRespawn(false);
                else if( upgrade.getPrizeNum() > 0 )
                    m_botAction.specificPrize( p.getArenaPlayerID(), -upgrade.getPrizeNum() );
            }
        }
        return pointsReturned;
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
                if( p.getYTileLocation() != 0 && (p.getYTileLocation() <= TOP_SAFE || p.getYTileLocation() >= BOT_SAFE) && !p.isAttached() )
                    player.doWarp(false);
            }
        }
    }


    /**
     * Toggles between the player warping to base at round start, or warping to spawn.
     * @param name
     * @param msg
     */
    public void cmdBaseWarp( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.warpInBase() )
            m_botAction.sendPrivateMessage( name, "You will be warped into your base at round start." );
        else
            m_botAction.sendPrivateMessage( name, "You will be warped to spawn at round start." );
    }


    /**
     * Toggles sending kill messages or not.
     * @param name
     * @param msg
     */
    public void cmdKillMsg( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.sendKillMessages() )
            m_botAction.sendPrivateMessage( name, "Messages ON: kills, repeats, profit-sharing, TKs, humiliation.  1% bonus no longer in effect.  This setting is SAVED." );
        else
            m_botAction.sendPrivateMessage( name, "Messages OFF: kills, repeats, profit-sharing, TKs, humiliation.  +1% bonus to all RP earned.  This setting is SAVED." );
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
        Iterator <Player>i = m_botAction.getFreqPlayerIterator(p.getArmyID());
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
        if( p.getShipNum() == 9 ) {
            m_botAction.sendPrivateMessage( name, "Tactical Ops can't assist ... you'll have to !dock back at HQ in your shuttle before you even think about it!" );
        }
        int armyToAssist = -1;
        boolean autoReturn = msg.equals(":auto:");
        if( msg.equals("") || autoReturn ) {
            armyToAssist = p.getNaturalArmyID();
        } else {
            try {
                armyToAssist = Integer.parseInt( msg );
            } catch (NumberFormatException e) {
                m_botAction.sendPrivateMessage( name, "Which of the !armies would you like to assist?" );
                return;
            }
        }

        DistensionArmy assistArmy = m_armies.get( new Integer( armyToAssist ) );
        if( assistArmy == null ) {
            m_botAction.sendPrivateMessage( name, "Exactly which of those !armies you trying to help out there?" );
            return;
        }
        float armySizeWeight, assistArmyWeightAfterChange;
        float assistArmyStr = assistArmy.getTotalStrength();
        float currentArmyStr = p.getArmy().getTotalStrength();
        if( assistArmyStr <= 0 ) assistArmyStr = 1;
        if( currentArmyStr <= 0 ) currentArmyStr = 1;
        armySizeWeight = assistArmyStr / currentArmyStr;
        assistArmyWeightAfterChange = (currentArmyStr - p.getStrength()) / (assistArmyStr + p.getStrength());

        if( p.getNaturalArmyID() == armyToAssist ) {
            if( !p.isAssisting() ) {
                m_botAction.sendPrivateMessage( name, "You aren't assisting any army presently.  Use !assist armyID# to assist an army in need." );
            } else {
                if( assistArmyWeightAfterChange < ASSIST_WEIGHT_IMBALANCE ) {
                    if( !autoReturn )
                        m_botAction.sendPrivateMessage( name, "Returning to your army now would make things too imbalanced!  Sorry, no can do." );
                    return;
                }
                if( autoReturn ) {
                    m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), name.toUpperCase() + " is automatically returning to their army to provide needed support." );
                    m_botAction.sendOpposingTeamMessageByFrequency(p.getNaturalArmyID(), name.toUpperCase() + " has automatically returned to the army." );
                } else {
                    m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), name.toUpperCase() + " has finished assistance and is returning to their army." );
                    m_botAction.sendOpposingTeamMessageByFrequency(p.getNaturalArmyID(), name.toUpperCase() + " has returned to the army." );
                }
                if( p.getShipNum() == 0 ) {
                    p.setAssist( -1 );
                } else {
                    p.setAssist( -1 );
                    m_botAction.setFreq(name, armyToAssist );
                    if( !p.isRespawning() ) {
                        m_botAction.shipReset(name);
                        p.prizeUpgradesNow();
                    }
                    for( DistensionArmy a : m_armies.values() )
                        a.recalculateFigures();
                }
                if( msg.equals(":auto:") )
                    m_botAction.sendPrivateMessage( name, "To maintain balance, you have been returned to " + p.getArmyName() + ".");
                else {
                    if( armySizeWeight > ASSIST_WEIGHT_IMBALANCE ) {
                        // Kill participation if return assist was not all that needed
                        m_playerTimes.remove( name );
                        m_playerTimes.put( name, new Integer( flagTimer.getTotalSecs() ) );
                        m_botAction.sendPrivateMessage( name, "You have returned to " + p.getArmyName() + ", but your participation has been reset.");
                    } else {
                        m_botAction.sendPrivateMessage( name, "You have returned to " + p.getArmyName() + ".");
                    }
                }
            }
            return;
        }

        if( armySizeWeight < ASSIST_WEIGHT_IMBALANCE && !autoReturn ) {
            if( assistArmyWeightAfterChange < ASSIST_WEIGHT_IMBALANCE ) {
                m_botAction.sendPrivateMessage( name, "Assisting with your current ship will only continue the imbalance!  First !pilot a lower-ranked ship if you want to !assist." );
                return;
            }
            if( m_flagOwner[0] == armyToAssist && m_flagOwner[1] == armyToAssist ) {
                m_botAction.sendPrivateMessage( name, "While army strengths are imbalanced, that army seems to be doing fine!  Try again later." );
                return;
            }

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
                    if( m_flagOwner[0] == p.getArmyID() && m_flagOwner[1] == p.getArmyID() ) {
                        reward *= 2;
                        m_botAction.sendPrivateMessage( name, "For your extremely noble assistance, you also receive a " + (DEBUG ? (int)(reward * DEBUG_MULTIPLIER) : reward ) + " RP bonus.", 1 );
                    } else {
                        m_botAction.sendPrivateMessage( name, "For your noble assistance, you also receive a " + (DEBUG ? (int)(reward * DEBUG_MULTIPLIER) : reward ) + " RP bonus.", 1 );
                    }
                    p.addRankPoints(reward);
                }
                p.setAssist( armyToAssist );
                m_botAction.setFreq(name, armyToAssist );
                if( !p.isRespawning() ) {
                    m_botAction.shipReset(name);
                    p.prizeUpgradesNow();
                }
            } else {
                p.setAssist( armyToAssist );
            }
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();
            m_botAction.sendOpposingTeamMessageByFrequency(p.getNaturalArmyID(), name.toUpperCase() + " has left to assist the other army." );
            m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), name.toUpperCase() + " is now assisting your army." );
        } else {
            m_botAction.sendPrivateMessage( name, "Not overly imbalanced -- consider flying a lower-rank ship to even the battle instead!" );
            if( DEBUG )
                m_botAction.sendPrivateMessage( name, "(" + armySizeWeight + " weight, need " + ASSIST_WEIGHT_IMBALANCE + " or less)" );
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
                text += p2.getName() + "(" + p2.getRank() + ")  ";
                players++;
                shipStrength += p2.getStrength();
            }
            m_botAction.sendPrivateMessage(name, num + Tools.formatString( (" " + Tools.shipNameSlang(i) + (num==1 ? "":"s")), 8 )
                                                 + (shipStrength > 0 ? ("  " + shipStrength + " STR" + text) : "") );
            totalStrength += shipStrength;
        }
        if( team.get(0).size() > 0 ) {
            DistensionPlayer p2 = m_players.get( team.get(0).get(0) );
            players++;
            m_botAction.sendPrivateMessage(name, "+ Ops    " + p2.getStrength() + " STR   " + p2.getName() + "(" + p2.getRank() + ")");
        }

        m_botAction.sendPrivateMessage(name, players + " players, " + totalStrength + " total strength.  (STR = rank + " + RANK_0_STRENGTH + ")" );
        if( totalStrength == 0 && DEBUG )
            m_botAction.sendArenaMessage("0 strength found for Army " + p.getArmyID() + ".  Assisting: " + (p.isAssisting() ? "Yes" : "No") + "   Natural ID: " + p.getNaturalArmyID() );
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

        if( m_mineClearedPlayers.contains( name ) ) {
            m_botAction.sendPrivateMessage( name, "Well now... you've already cleared your mines once this battle.  Tough!" );
            return;
        }
        if( p.getShipNum() != Tools.Ship.SHARK && p.getShipNum() != Tools.Ship.LEVIATHAN && p.getShipNum() != Tools.Ship.WARBIRD ) {
            m_botAction.sendPrivateMessage( name, "You must be in a mine-laying ship in order for me to clear your mines.  Didn't think I'd have to tell you!" );
            return;
        }
        if( p.isRespawning() ) {
            m_botAction.sendPrivateMessage( name, "You can't reset your mines while rearming." );
            return;
        }

        m_botAction.setShip( name, 1 );
        m_botAction.setShip( name, p.getShipNum() );
        p.prizeUpgradesNow();
        m_mineClearedPlayers.add( name );
        m_botAction.sendPrivateMessage( name, "Mines cleared.  You may do this once per battle.  Use safety areas in rearmament zone to clear mines manually." );
    }

    /**
     * Puts a player back into the game with participation bonus intact.
     * @param name
     * @param msg
     */
    public void cmdLagout( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() == -1 ) {
            m_botAction.sendPrivateMessage( name, "Please !return before you try getting back in." );
            return;
        }
        if( p.getShipNum() != 0 ) {
            m_botAction.sendPrivateMessage( name, "You're already in the battle!" );
            return;
        }

        if( flagTimer != null && flagTimer.isRunning() ) {
            Integer lagoutTime = m_lagouts.get( name );
            if( !p.canUseLagout() || lagoutTime == null ) {
                m_botAction.sendPrivateMessage( name, "There's no record of you having lagged out, or your lagout is in effect and you will be returned to the fray soon." );
                return;
            }
            if( lagoutTime + LAGOUT_VALID_SECONDS < flagTimer.getTotalSecs() ) {
                m_botAction.sendPrivateMessage( name, "Sorry, it's been too long.  You'll have to !pilot the normal way." );
                return;
            }
            m_lagouts.remove(name);
            m_botAction.sendPrivateMessage( name, "You'll be placed back on your army in " + LAGOUT_WAIT_SECONDS + " seconds.  You may manually !pilot a ship before this time, but you will lose your participation." );
            m_botAction.scheduleTask(new LagoutTask(name), LAGOUT_WAIT_SECONDS * 1000 );
        } else {
            m_botAction.sendPrivateMessage( name, "No need to !lagout ... no battle's going!  Just hop right in." );
        }
    }

    /**
     * Shows a player basic info on the current battle.
     * @param name
     * @param msg
     */
    public void cmdBattleInfo( String name, String msg ) {
        if( flagTimeStarted )
            if( flagTimer != null )
                flagTimer.sendTimeRemaining( name );
            else
                m_botAction.sendPrivateMessage( name, "The battle is just about to begin." );
        else
            m_botAction.sendPrivateMessage( name, "The battle is not presently being waged..." );

    }


    // ***** TACTICAL OPS COMMANDS *****

    /**
     * Wrapper for cmdOpsMsg(String,String,int)
     */
    public void cmdOpsMsg( String name, String msg ) {
        cmdOpsMsg( name, msg, -1 );
    }

    /**
     * Sends out a tactical ops msg via the bot.
     * @param name
     * @param msg
     * @param armyIDSpoof Army ID to spoof; -1 if not sabotage
     */
    public void cmdOpsMsg( String name, String msg, int armyIDSpoof ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "You must be at the Tactical Ops station to do this." );
            return;
        }
        int msgNum = 0;
        if( Tools.isAllDigits(msg) ) {
            msgNum = Integer.parseInt(msg);
            if( msgNum < 1 || msgNum > 4 ) {
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Please provide an appropriate message number (1-4).  Use !opsmsg for assistance." );
                return;
            }
        } else {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Please provide a message number (1-4).  Use !opsmsg for assistance." );
            return;
        }
        if( p.getCurrentComms() < 1 ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "You need a communication authorization to send out a message.  (+1 every minute)" );
            return;
        }
        p.adjustComms(-1);
        boolean falsify = (armyIDSpoof != -1);

        if( msgNum == 1 || msgNum == 2 ) {
            int friendlies = 0;
            int foes = 0;
            int friendTerr = 0;
            int foeTerr = 0;
            String bestTerr = null;
            int bestTerrLoc = 0;
            if( falsify ) {
                // Falsify it to look as though help is needed
                friendlies = (int)(Math.random() * 5);
                foes = (int)(Math.random() * 15);
                foeTerr = (int)(Math.random() * 2);
            } else {
                for( Player p2 : m_botAction.getPlayingPlayers() ) {
                    if( p2.getYTileLocation() > (msgNum==1 ? TOP_ROOF : BOT_MID)
                     && p2.getYTileLocation() < (msgNum==1 ? TOP_MID  : BOT_ROOF) ) {
                        if( p2.getFrequency() == p.getArmyID() ) {
                            if( p2.getShipType() == Tools.Ship.TERRIER ) {
                                friendTerr++;
                                if( bestTerr == null ||
                                    (msgNum==1 ?
                                            p2.getYTileLocation() < bestTerrLoc :
                                            p2.getYTileLocation() > bestTerrLoc ) ) {
                                    bestTerr = p2.getPlayerName();
                                    bestTerrLoc = p2.getYTileLocation();
                                }
                            }
                            friendlies++;
                        } else {
                            if( p2.getShipType() == Tools.Ship.TERRIER )
                                foeTerr++;
                            foes++;
                        }
                    }
                }
            }
            int freq = (falsify ? armyIDSpoof : p.getArmyID() );
            m_botAction.sendOpposingTeamMessageByFrequency( freq,
                    "OPS:  " + (m_flagOwner[0] == freq ? "Defend" : "Assault") +
                              (msgNum==1 ? " TOP BASE" : " BOTTOM BASE" ) +
                    "!!  Friend: " + friendlies + " (" + friendTerr + " Terr) " +
                    "-vs- Enemy: " + foes + " (" + foeTerr + " Terr)" +
                    (bestTerr!=null ?
                            " ...  Attach to: " + bestTerr + " (" + getLocation(bestTerrLoc) + ")" :
                            "") );
        } else if( msgNum == 3 || msgNum == 4 ) {
            int searchship = 0;
            if( !falsify ) {
                for( Player p2 : m_botAction.getPlayingPlayers() ) {
                    if( p2.getFrequency() == p.getArmyID() )
                        if( msgNum == 3 ) {
                            if( p2.getShipType() == Tools.Ship.TERRIER )
                                searchship++;
                        } else if( msgNum == 4 ) {
                            if( p2.getShipType() == Tools.Ship.SHARK )
                                searchship++;
                        }
                }
            }
            int freq = (falsify ? armyIDSpoof : p.getArmyID() );
            m_botAction.sendOpposingTeamMessageByFrequency( freq, "OPS:  Requesting 1 additional " +
                    (msgNum==3 ? "TERRIER" : "SHARK" ) + " pilot.  (Currently have " + searchship + ")");
        }
    }



    // ***** HIGHMOD+ COMMANDS

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
                p.savePlayerTimeToDB();
                if( p.saveCurrentShipToDBNow() == false ) {
                    m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Your data could not be saved.  Please use !dock to save your data.  Contact a mod with ?help if this does not work." );
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
                m_botAction.sendArenaMessage( "Saved all " + players + " players in " + timeDiff + "ms.  -" + name, 1 );
            else
                m_botAction.sendArenaMessage( "Saved " + players + " players in " + timeDiff + "ms.  " + playersunsaved + " players could not be saved.", 2 );
        }
        if( beginDelayedShutdown ) {
            m_botAction.sendPrivateMessage( name, "IMPORTANT NOTE TO MODERATOR: Bot will automatically save and shut down at end of this round." );
        }
    }


    /**
     * Kills the bot.
     * @param name
     * @param msg
     */
    public void cmdDie( String name, String msg ) {
    	readyForPlay = false;	// To prevent spec-docking / unnecessary DB accesses
        dying = true;
        m_botAction.specAll();
        m_botAction.toggleLocked();
        flagObjs.hideAllObjects();
        flagTimerObjs.hideAllObjects();
        m_botAction.hideObject(LVZ_REARMING);
        Integer id;
        Iterator <Integer>i = m_botAction.getPlayerIDIterator();
        while( i.hasNext() ) {
            id = i.next();
            playerObjs.hideAllObjects( id );
        }
        m_botAction.setObjects();
        m_botAction.manuallySetObjects(flagObjs.getObjects());
        m_botAction.scoreResetAll();
        m_botAction.sendArenaMessage( "Distension going down for maintenance ...", 1 );
        Thread.yield();
        try { Thread.sleep(500); } catch (Exception e) {};
        if( msg.equals("shutdown") ) {
            TimerTask dieTask = new TimerTask() {
                public void run() {
                    m_botAction.die( "mod-initiated by !shutdown");
                }
            };
            m_botAction.scheduleTask(dieTask, 1000);
        } else {
            m_botAction.die( "!die by " + name );
        }
    }


    /**
     * Saves all data and then kills the bot on a delayed shutdown.
     * @param name
     * @param msg
     */
    public void cmdSaveDie( String name, String msg ) {
        cmdSaveData(name, msg);
        cmdDie(name, "shutdown");
    }


    /**
     * Starts a task to kill the bot at the end of the next round following a certain time limit.
     * @param name
     * @param msg
     */
    public void cmdShutdown( String name, String msg ) {
        if( beginDelayedShutdown ) {
            m_botAction.sendPrivateMessage( name, "Shutdown cancelled." );
            beginDelayedShutdown = false;
            return;
        }
        Integer minToShutdown = 0;
        try {
            minToShutdown = Integer.parseInt( msg );
        } catch (NumberFormatException e) {
            m_botAction.sendPrivateMessage( name, "Improper format.  !shutdown <minutes>.  Or use !shutdown 0 and then !shutdown to cancel." );
            return;
        }
        m_botAction.sendPrivateMessage( name, "Shutting down at the next end of round occuring after " + minToShutdown + " minutes." );
        TimerTask delayedShutdownTask = new TimerTask() {
            public void run() {
                beginDelayedShutdown = true;
            }
        };
        try {
            m_botAction.scheduleTask(delayedShutdownTask, minToShutdown * 1000 * 60 );
        } catch (Exception e) {
            try {
                m_botAction.cancelTask(delayedShutdownTask);
                m_botAction.sendPrivateMessage( name, "Prior shutdown cancelled.  Please try !shutdown again momentarily..." );
            } catch (Exception e2) {
                m_botAction.sendPrivateMessage( name, "Shutdown already scheduled!  You may try again momentarily..." );
            }
        }
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
            player = new DistensionPlayer(msg);
            if( !player.getPlayerFromDB() ) {
                m_botAction.sendPrivateMessage( name, "Can't find player '" + msg + "' in DB.  Check spelling." );
                return;
            }
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
            player = new DistensionPlayer(msg);
            if( !player.getPlayerFromDB() ) {
                m_botAction.sendPrivateMessage( name, "Can't find player '" + msg + "' in DB.  Check spelling." );
                return;
            }
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


    /**
     * Changes player's name in the database.
     * @param name
     * @param msg
     */
    public void cmdDBChangeName( String name, String msg ) {
        String[] args = msg.split(":");
        if( args.length != 2 || args[0].equals("") || args[1].equals("") ) {
            m_botAction.sendPrivateMessage( name, "Syntax: !db-changename <currentname>:<newname>" );
            return;
        }
        DistensionPlayer player = m_players.get( args[0] );
        if( player != null ) {
            m_botAction.sendPrivateMessage( name, args[0] + " found in arena.  Player must leave arena in order to use this command." );
            return;
        }

        try {
            m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcName='" + args[1] + "' WHERE fcName='" + Tools.addSlashesToString( args[0] ) + "'" );
        } catch (SQLException e ) {
            m_botAction.sendPrivateMessage( name, "DB command not successful." );
            return;
        }
        m_botAction.sendPrivateMessage( name, "Name '" + args[0] + "' changed to '" + args[1] + "' in database." );
        Tools.printLog(name + " changed " + args[0] + "'s name to " + args[1] + " in Distension DB." );
    }


    /**
     * Removes a player's ship from the DB.
     * @param name
     * @param msg
     */
    public void cmdDBWipeShip( String name, String msg ) {
        String[] args = msg.split(":");
        if( args.length != 2 || args[0].equals("") || args[1].equals("") ) {
            m_botAction.sendPrivateMessage( name, "Syntax: !db-wipeship <name>:<ship#>" );
            return;
        }
        DistensionPlayer player = m_players.get( args[0] );
        if( player == null ) {
            m_botAction.sendPrivateMessage( name, args[0] + " not found in arena.  Player must be in arena in order to use this command." );
            return;
        }
        int shipNumToWipe = 0;
        try {
            shipNumToWipe = Integer.parseInt( args[1] );
        } catch (NumberFormatException e) {
            m_botAction.sendPrivateMessage( name, "Invalid ship #.  Syntax: !db-wipeship <name>:<ship#>" );
            return;
        }
        if( shipNumToWipe < 1 || shipNumToWipe > 8 ) {
            m_botAction.sendPrivateMessage( name, "Invalid ship #.  Syntax: !db-wipeship <name>:<ship#>" );
            return;
        }
        if( shipNumToWipe == player.getShipNum() ) {
            m_botAction.sendPrivateMessage( name, "Player is currently in that ship.  They must be in a different ship in order for the procedure to work." );
            return;
        }

        player.removeShipFromDB( shipNumToWipe );
        String shipname = ( player.getShipNum() == 9 ? "Tactical Ops" : Tools.shipName(shipNumToWipe) );
        m_botAction.sendPrivateMessage( name, shipname + " has been removed from " + player.getName() + "'s hangar." );
        Tools.printLog(name + " deleted " + args[0] + "'s ship #" + shipNumToWipe + " from Distension DB." );
    }


    /**
     * Wipes all traces of player from DB.
     * @param name
     * @param msg
     */
    public void cmdDBWipePlayer( String name, String msg ) {
        DistensionPlayer player = m_players.get( msg );
        if( player != null ) {
            m_botAction.sendPrivateMessage( name, msg + " found in arena.  Player must leave arena in order to use this command." );
            return;
        }

        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT * FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString(msg) + "'" );
            if( r.next() ) {
                int id;
                id = r.getInt("fnID");
                m_botAction.SQLQueryAndClose( m_database, "DELETE FROM tblDistensionPlayer WHERE fnID='" + id + "'" );
                m_botAction.SQLQueryAndClose( m_database, "DELETE FROM tblDistensionShip WHERE fnPlayerID='" + id + "'" );
                m_botAction.sendPrivateMessage( name, msg + " deleted from DB!" );
                Tools.printLog(name + " deleted " + msg + " from Distension DB." );
            } else {
                m_botAction.sendPrivateMessage( name, "Player not found." );
            }
        } catch (SQLException e ) {
            m_botAction.sendPrivateMessage( name, "DB command not successful." );
        }
    }


    /**
     * Randomizes army for all players.
     * @param name
     * @param msg
     */
    public void cmdDBRandomArmies( String name, String msg ) {
        int army0Count = 0;
        int army1Count = 0;
        int totalCount = 0;
        LinkedList <Integer>newArmy0 = new LinkedList<Integer>();
        LinkedList <Integer>newArmy1 = new LinkedList<Integer>();
        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fnID, fnArmyID FROM tblDistensionPlayer ORDER BY fnArmyID" );
            if( r == null ) {
                m_botAction.sendPrivateMessage( name, "DB command not successful." );
                return;
            }
            while( r.next() ) {
                if( totalCount % 2 == 0 ) {
                    if( r.getInt("fnArmyID") == 1 )         // Only change army if needed
                        newArmy0.add( r.getInt("fnID") );
                    army0Count++;
                } else {
                    if( r.getInt("fnArmyID") == 0 )         // Only change army if needed
                        newArmy1.add( r.getInt("fnID") );
                    army1Count++;
                }
                totalCount++;
            }
            m_botAction.specAll();
            for( int pid : newArmy0 )
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fnArmyID='0' WHERE fnID='" + pid + "'" );
            for( int pid : newArmy1 )
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fnArmyID='1' WHERE fnID='" + pid + "'" );
            m_armies.get(0).manuallySetPilotsTotal( army0Count );
            m_armies.get(1).manuallySetPilotsTotal( army1Count );
            m_botAction.sendPrivateMessage( name, "Army reconfiguration complete; all " + totalCount + " players reassigned.  Army 0: " + army0Count + " pilots; Army 1: " + army1Count + " pilots." );
            cmdSaveDie(name,"");
        } catch (SQLException e ) {
            m_botAction.sendPrivateMessage( name, "DB command not successful." );
            return;
        }
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
                m_msgBetaPlayers.add(r.getString("fcName"));
                players++;
            }
            m_botAction.sendPrivateMessage( name, players + " players added to notify list." );
            m_botAction.SQLClose(r);
        } catch (SQLException e) {}

        TimerTask msgTask = new TimerTask() {
            public void run() {
                if( !m_msgBetaPlayers.isEmpty() )
                    m_botAction.sendRemotePrivateMessage( m_msgBetaPlayers.remove(), "DISTENSION BETA TEST: ?go #distension if you can participate." );
                else
                    this.cancel();
            }
        };
        m_botAction.scheduleTask(msgTask, 100, 100);
    }


    // ***** COMMAND ASSISTANCE METHODS

    /**
     * Swaps out given player for next player waiting to enter.
     * @param p Player to swap out
     */
    public void doSwapOut( DistensionPlayer p, boolean playerStillInArena ) {
        if( p == null || m_waitingToEnter.isEmpty() )
            return;
        if( playerStillInArena ) {
            cmdLeave(p.getName(), "");
            m_botAction.sendPrivateMessage( p.getName(), "Another player wishes to enter the battle; you have been removed to allow them to play." );
        }
        DistensionPlayer enteringPlayer = m_waitingToEnter.remove();
        String name = enteringPlayer.getName();
        m_botAction.sendPrivateMessage( name, "A slot has opened up.  You may now join the battle." );
        enteringPlayer.setShipNum(0);
        m_botAction.sendPrivateMessage( name, name.toUpperCase() + " authorized as a pilot of " + enteringPlayer.getArmyName().toUpperCase() + ".  Returning you to HQ." );
    }

    /**
     * Based on provided coords, returns location of player as a String.
     * @return Last location recorded of player, as a String
     */
    public String getPlayerLocation( Player p, boolean isStaff ) {
        int x = p.getXTileLocation();
        int y = p.getYTileLocation();
        String exact = "";
        if( isStaff )
            exact = "  (" + x + "," + y + ")";
        if( x==0 && y==0 )
            return "Not yet spotted" + exact;
        return getLocation(y) + exact;
    }

    /**
     * Gets String location name based on a Y coordinate.
     * @param y Y tile coord to check.
     * @return
     */
    public String getLocation( int y ) {
        if( y <= TOP_SAFE || y >= BOT_SAFE )
            return "Resupplying";
        if( y <= TOP_ROOF )
            return "Roofing Top ...";
        if( y <= TOP_FR )
            return "Top FR";
        if( y <= TOP_MID )
            return "Top Mid";
        if( y <= TOP_LOW )
            return "Top Lower";
        if( y >= BOT_LOW )
            return "Bottom Lower";
        if( y >= BOT_MID )
            return "Bottom Mid";
        if( y >= BOT_FR )
            return "Bottom FR";
        if( y >= BOT_ROOF )
            return "Roofing Bottom ...";
        return "Neutral zone";
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
        m_flagOwner[0] = -1;
        m_flagOwner[1] = -1;
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
        Iterator <Player>i = m_botAction.getFreqPlayerIterator(army);
        while( i.hasNext() ) {
            Player p = (Player)i.next();
            team.get(p.getShipType()).add(p.getPlayerName());
        }
        return team;
    }

    /**
     * Based on a prize number, return a description of the upgrade.
     * @param prizeNum Prize of interest
     * @return Description of upgrade
     */
    public String getUpgradeText( int prizeNum ) {
        String desc = "";
        switch( prizeNum ) {
            // Upgrades 1-10 on !armory list
            case Tools.Prize.ROTATION:
                desc = "Rotation and turning speed";
                break;
            case Tools.Prize.THRUST:
                desc = "Thrust and acceleration (NOT afterburner)";
                break;
            case Tools.Prize.TOPSPEED:
                desc = "Top speed";
                break;
            case Tools.Prize.RECHARGE:
                desc = "Energy recharge speed";
                break;
            case Tools.Prize.ENERGY:
                desc = "Maximum energy";
                break;
            case Tools.Prize.GUNS:
                desc = "Gunning systems";
                break;
            case Tools.Prize.BOMBS:
                desc = "Bombing systems";
                break;
            case Tools.Prize.MULTIFIRE:
                desc = "Multifire";
                break;
            case Tools.Prize.XRADAR:
                desc = "X-Radar";
                break;
            case Tools.Prize.DECOY:
                desc = "Decoy";
                break;
            // Additional (ship-specific) upgrades, A-Z
            case Tools.Prize.ANTIWARP:
                desc = "Anti-Warp";
                break;
            case Tools.Prize.BRICK:
                desc = "Brick";
                break;
            case Tools.Prize.BURST:
                desc = "Burst";
                break;
            case Tools.Prize.CLOAK:
                desc = "Cloak";
                break;
            case Tools.Prize.PORTAL:
                desc = "Portal";
                break;
            case Tools.Prize.PROXIMITY:
                desc = "Proximity Detonation";
                break;
            case Tools.Prize.REPEL:
                desc = "Repel";
                break;
            case Tools.Prize.ROCKET:
                desc = "Rocket";
                break;
            case Tools.Prize.SHRAPNEL:
                desc = "Shrapnel";
                break;
            case Tools.Prize.STEALTH:
                desc = "Stealth";
                break;
            case Tools.Prize.THOR:
                desc = "Thor's Hammer";
                break;
            /*      Special upgrade numbers, controlled by bot
             * -1:  Fast respawn (puts you at the head of the respawning queue when you die)
             * -2:  Prizing burst + warp every 60 seconds, for terriers
             * -3:  Full charge chance every 30 seconds, for spiders
             * -4:  Targetted EMP against all enemies (uses !emp command)
             * -5:  Super chance every 30 seconds, for spiders
             * -6:  Repel chance every 30 seconds, for sharks
             * -7:  Profit sharing
             */
            case ABILITY_PRIORITY_REARM:
                desc = "Always first in line to rearm";
                break;
            case ABILITY_TERR_REGEN:
                desc = "+5% chance of burst/portal every 30 seconds";
                break;
            case ABILITY_REFUELER:
                desc = "+50% chance of full charge every 30 seconds";
                break;
            case ABILITY_TARGETTED_EMP:
                desc = "EMP ALL enemies with !emp (every 20 minutes)";
                break;
            case ABILITY_SUPER:
                desc = "+5% chance of super every 30 seconds";
                break;
            case ABILITY_SHARK_REGEN:
                desc = "+10% chance of repel every 30 seconds";
                break;
            case ABILITY_PROFIT_SHARING:
                desc = "+1% of team's kill RP earned per level";
                break;
        }
        return desc;
    }

    /**
     * Spams a player with a String array based on a default delay.
     * @param arenaID ID of person to spam
     * @param msgs Array of Strings to spam
     */
    public void spamWithDelay( int arenaID, String[] msgs ) {
        SpamTask spamTask = new SpamTask();
        spamTask.setMsgs( arenaID, msgs );
        m_botAction.scheduleTask(spamTask, MESSAGE_SPAM_DELAY, MESSAGE_SPAM_DELAY );
    }

    /**
     * Spams a player with a String array based on a given delay.
     * @param arenaID ID of person to spam
     * @param msgs Array of Strings to spam
     * @param delay Delay, in ms, to wait in between messages
     */
    public void spamWithDelay( int arenaID, String[] msgs, int delay ) {
        SpamTask spamTask = new SpamTask();
        spamTask.setMsgs( arenaID, msgs );
        m_botAction.scheduleTask(spamTask, delay, delay );
    }

    /**
     * Prizes a player up using a delay, turns off an LVZ (the rearm), and
     * warps the player when done prizing.
     * @param arenaID ID of person to prize
     * @param msgs Array of Strings to spam
     * @param delay Delay, in ms, to wait in between messages
     */
    public void prizeSpam( int arenaID, LinkedList <Integer>prizes, boolean warp ) {
        PrizeSpamTask prizeSpamTask = new PrizeSpamTask();
        prizeSpamTask.setup( arenaID, prizes, warp );
        m_botAction.scheduleTask( prizeSpamTask, PRIZE_SPAM_DELAY, PRIZE_SPAM_DELAY );
    }

    /**
     * Task used to send a spam of messages at a slower rate than normal.
     */
    private class SpamTask extends TimerTask {
        LinkedList <String>remainingMsgs = new LinkedList<String>();
        int arenaIDToSpam = -1;

        public void setMsgs( int id, String[] list ) {
            arenaIDToSpam = id;
            for( int i = 0; i < list.length; i++ )
                remainingMsgs.add( list[i] );
        }

        public void run() {
            if( remainingMsgs == null || remainingMsgs.isEmpty() ) {
                this.cancel();
            } else {
                String msg = remainingMsgs.remove();
                if( msg != null )
                    m_botAction.sendUnfilteredPrivateMessage( arenaIDToSpam, msg );
            }
        }
    }

    /**
     * Task used to prize a player at a slower-than-instant rate, and warp player
     * at end/remove rearm LVZ.
     */
    private class PrizeSpamTask extends TimerTask {
        LinkedList <Integer>remainingPrizes = new LinkedList<Integer>();
        int arenaIDToSpam = -1;
        boolean doWarp = false;

        public void setup( int id, LinkedList <Integer>list, boolean warp ) {
            arenaIDToSpam = id;
            remainingPrizes = list;
            doWarp = warp;
        }

        public void run() {
            if( remainingPrizes == null || remainingPrizes.isEmpty() ) {
                m_botAction.hideObjectForPlayer( arenaIDToSpam, LVZ_REARMING );
                Player p = m_botAction.getPlayer( arenaIDToSpam );
                try {
                    if( doWarp )
                        m_players.get( p.getPlayerName() ).doWarp(false);
                } catch (Exception e) {}
                this.cancel();
            } else {
                Integer prize = remainingPrizes.remove();
                if( prize != null )
                    m_botAction.sendUnfilteredPrivateMessage( arenaIDToSpam, "*prize #" + prize );
            }
        }
    }




    // ***** INTERNAL CLASSES
    // Players, armies, prizing queue

    /**
     * Used to keep track of player data retreived from the DB, and update data
     * during play, to be later synch'd with the DB.
     */
    private class DistensionPlayer {
        private String name;    // Playername
        private int arenaPlayerID;    // ID as understood by Arena
        private int playerID;   // PlayerID as found in DB (not as in Arena); -1 if not logged in
        private int timePlayed; // Time, in minutes, played today;            -1 if not logged in
        private int shipNum;    // Current ship: 1-8, 0 if docked/spectating; -1 if not logged in
        private int lastShipNum;// Last ship used (for lagouts);              -1 if not logged in
        private int rank;       // Current rank (# upgrade points awarded);   -1 if docked/not logged in
        private int rankPoints; // Current rank points for ship;              -1 if docked/not logged in
        private int nextRank;   // Amount of points needed to increase rank;  -1 if docked/not logged in
        private int rankStart;  // Amount of points at which rank started;    -1 if docked/not logged in
        private int upgPoints;  // Current upgrade points available for ship; -1 if docked/not logged in
        private int armyID;     // 0-9998;                                    -1 if not logged in
        private int progress;   // Progress to next rank, 0 to 9, for bar;    -1 if not logged in
        private int successiveKills;            // # successive kills (for unlocking weasel)
        private int[]     purchasedUpgrades;    // Upgrades purchased for current ship
        private boolean[] shipsAvail;           // Marks which ships are available
        private int[]     lastIDsKilled = { -1, -1, -1, -1 };  // ID of last player killed (feeding protection)
        private int       spawnTicks;           // # queue "ticks" until spawn
        private int       idleTicks;            // # ticks player has been idle
        private int       assistArmyID;         // ID of army player is assisting; -1 if not assisting
        private int       recentlyEarnedRP;     // RP earned since changing to this ship
        private int       currentOP;            // Current # OP points (for Tactical Ops)
        private int       maxOP;                // Max # OP points (for Tactical Ops)
        private int       currentComms;         // Current # communications saved up (for Tactical Ops)
        private double    bonusBuildup;         // Bonus for !killmsg that is "building up" over time
        private boolean   warnedForTK;          // True if they TKd / notified of penalty this match
        private boolean   banned;               // True if banned from playing
        private boolean   shipDataSaved;        // True if ship data on record equals ship data in DB
        private boolean   fastRespawn;          // True if player respawns at the head of the queue
        private boolean   isRespawning;         // True if player is currently in respawn process
        private boolean   waitInSpawn;          // True if player would like to warp out manually at respawn
        private boolean   warpInBase;           // True if warping player to FR rather than spawn @ round start
        private boolean   specialRespawn;       // True if in spawn queue w/o actually spawning
        private boolean   sendKillMessages;     // True if player wishes to receive kill msgs
        private boolean   allowLagout;          // True if !lagout should be allowed

        public DistensionPlayer( String name ) {
            this.name = name;
            arenaPlayerID = m_botAction.getPlayerID(name);
            playerID = -1;
            timePlayed = -1;
            shipNum = -1;
            lastShipNum = -1;
            rank = -1;
            rankPoints = -1;
            nextRank = -1;
            upgPoints = -1;
            armyID = -1;
            progress = -1;
            currentOP = 0;
            maxOP = 0;
            successiveKills = 0;
            spawnTicks = 0;
            idleTicks = 0;
            assistArmyID = -1;
            recentlyEarnedRP = 0;
            bonusBuildup = 0.0;
            purchasedUpgrades = new int[NUM_UPGRADES];
            shipsAvail = new boolean[9];
            for( int i = 0; i < 9; i++ )
                shipsAvail[i] = false;
            warnedForTK = false;
            banned = false;
            shipDataSaved = false;
            fastRespawn = false;
            isRespawning = false;
            waitInSpawn = true;
            warpInBase = true;
            specialRespawn = false;
            sendKillMessages = true;
            allowLagout = true;
        }


        // DB METHODS

        /**
         * Creates a player record in the database.
         */
        public void addPlayerToDB() {
            try {
                ResultSet r = m_botAction.SQLQuery( m_database, "INSERT INTO tblDistensionPlayer ( fcName , fnArmyID ) " +
                        "VALUES ('" + Tools.addSlashesToString( name ) + "', '" + armyID + "')" );
                if( r != null && r.next() )
                    playerID = r.getInt(1);
                m_botAction.SQLClose(r);
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
                    playerID = r.getInt("fnID");
                    armyID = r.getInt( "fnArmyID" );
                    timePlayed = r.getInt( "fnTime" );
                    sendKillMessages = r.getString( "fcSendKillMsg" ).equals("y");
                    for( int i = 0; i < 9; i++ )
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
         * Saves player time to DB.
         */
        public void savePlayerTimeToDB() {
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fnTime='" + timePlayed +"' WHERE fnID='" + playerID + "'" );
            } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); }
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
            if( shipNumToAdd < 1 || shipNumToAdd > 9 )
                return;
            shipsAvail[ shipNumToAdd - 1 ] = true;
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcShip" + shipNumToAdd + "='y' WHERE fnID='" + playerID + "'" );
                if( startingRankPoints == 0 )
                    m_botAction.SQLQueryAndClose( m_database, "INSERT INTO tblDistensionShip ( fnPlayerID , fnShipNum ) VALUES (" + playerID + ", " + shipNumToAdd + ")" );
                else
                    m_botAction.SQLQueryAndClose( m_database, "INSERT INTO tblDistensionShip ( fnPlayerID , fnShipNum , fnRankPoints ) VALUES (" + playerID + ",  " + shipNumToAdd + ", " + startingRankPoints + ")" );
            } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); }
        }

        /**
         * Removes a ship as being available on a player's record, and wipes all ship data.
         * @param shipNum Ship # to remove
         */
        public void removeShipFromDB( int shipNumToRemove ) {
            if( shipNumToRemove < 1 || shipNumToRemove > 8 )
                return;
            shipsAvail[ shipNumToRemove - 1 ] = false;
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcShip" + shipNumToRemove + "='n' WHERE fnID='" + playerID + "'" );
                m_botAction.SQLQueryAndClose( m_database, "DELETE FROM tblDistensionShip WHERE fnPlayerID='" + playerID + "' AND fnShipNum='" + shipNumToRemove + "'" );
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
            if( shipNum < 1 || shipNum > 9 || shipDataSaved )
                return true;

            String query = "UPDATE tblDistensionShip ship SET ";

            query +=       "ship.fnRank=" + rank + "," +
            "ship.fnRankPoints=" + rankPoints + "," +
            "ship.fnUpgradePoints=" + upgPoints + "," +
            "ship.fnStat1=" + purchasedUpgrades[0];

            for( int i = 1; i < NUM_UPGRADES; i++ )
                query +=   ", ship.fnStat" + (i + 1) + "=" + purchasedUpgrades[i];

            query +=       " WHERE ship.fnPlayerID='" + playerID + "' AND ship.fnShipNum='" + shipNum + "'";
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
            if( shipNum < 1 || shipNum > 9 )
                return false;
            try {
                String query = "SELECT * FROM tblDistensionShip ship " +
                "WHERE ship.fnPlayerID='" + playerID + "' AND ship.fnShipNum='" + shipNum + "'";

                ResultSet r = m_botAction.SQLQuery( m_database, query );
                if( r == null )
                    return false;
                if( r.next() ) {
                    // Init rank level, rank points and upgrade points
                    rank = r.getInt( "fnRank" );
                    rankPoints = r.getInt( "fnRankPoints" );
                    upgPoints = r.getInt( "fnUpgradePoints" );
                    if( rank >= 80 )
                        nextRank = 999999999;
                    else
                        nextRank = m_shipGeneralData.get( shipNum ).getNextRankCost(rank);
                    rankStart = m_shipGeneralData.get( shipNum ).getNextRankCost(rank-1);

                    // Init all upgrades
                    for( int i = 0; i < NUM_UPGRADES; i++ )
                        purchasedUpgrades[i] = r.getInt( "fnStat" + (i + 1) );
                    shipDataSaved = true;
                } else {
                    shipDataSaved = false;
                }

                fastRespawn = false;
                if( shipNum == 9 )
                    maxOP = DEFAULT_MAX_OP;
                // Setup special (aka unusual) abilities
                Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum ).getAllUpgrades();
                for( int i = 0; i < NUM_UPGRADES; i++ ) {
                    if( upgrades.get( i ).getPrizeNum() == ABILITY_PRIORITY_REARM && purchasedUpgrades[i] > 0 )
                        fastRespawn = true;
                    else if( upgrades.get( i ).getPrizeNum() == OPS_INCREASE_MAX_OP )
                        maxOP += purchasedUpgrades[i];
                }

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
            if( spawnTicks == 3 ) { // 3 ticks (1.5 sec) before spawn end, warp to safe and shipreset
                m_botAction.showObjectForPlayer(arenaPlayerID, LVZ_REARMING);
                doRearmAreaWarp();
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
                isRespawning = false;
                m_prizeQueue.resumeSpawningAfterDelay( prizeUpgrades( false ) );
                return true;
            }
            if( spawnTicks > 0 )
                return false;
            if( !isRespawning )
                return true;
            isRespawning = false;
            m_prizeQueue.resumeSpawningAfterDelay( prizeUpgrades( true ) );
            return true;
        }

        /**
         * Prizes upgrades to player based on what has been purchased, with delay,
         * but without holding up the prizing queue.
         */
        public void prizeUpgradesNow() {
            prizeUpgrades( false );
        }

        /**
         * Prizes upgrades to player based on what has been purchased, with delay.
         * @param Whether or not to warp player
         * @return Time, in ms, to delay until next spawn is allowed
         */
        public int prizeUpgrades( boolean warp ) {
            if( shipNum < 1 )
                return 0;
            LinkedList <Integer>prizing = new LinkedList<Integer>();
            Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum ).getAllUpgrades();
            int prize = -1;
            for( int i = 0; i < NUM_UPGRADES; i++ ) {
                prize = upgrades.get( i ).getPrizeNum();
                if( prize > 0 )
                    for( int j = 0; j < purchasedUpgrades[i]; j++ ) {
                        // Special case: on Lanc, only prize bombs if not attached
                        if( shipNum == 7 && i == 11 && purchasedUpgrades[11] > 0 ) {
                            Player p = m_botAction.getPlayer(arenaPlayerID);
                            if( p != null )
                                if( !p.isAttached() )
                                    prizing.add( prize );
                        }
                        else
                            prizing.add( prize );
                    }
            }
            if( fastRespawn )
                prizing.add( Tools.Prize.FULLCHARGE );
            prizeSpam( arenaPlayerID, prizing, warp );
            return prizing.size() * PRIZE_SPAM_DELAY;
        }

        /**
         * Prizes any special abilities that are reprized every 30 seconds.  This should only
         * be called if a player actually has one of these special abilities.
         *
         * Special prize #s that are prized with this method:
         * -2:  Prizing burst + warp for terriers
         * -3:  Full charge for spiders
         * -4:  Targetted EMP against all enemies -- recharged every 10 min or so, and not lost on death
         * -5:  Super for spiders
         * -6:  Repels for sharks
         *
         * @param tick Number of times this method has been run; used for intermittent abilities
         */
        public void prizeSpecialAbilities( int tick ) {
            boolean prized = false;
            if( shipNum == 5 ) {
                // Regeneration ability; each level worth an additional 5% of prizing either port or burst
                //                       (+5% for each, up to a total of 50%)
                double portChance = Math.random() * 20.0;
                double burstChance = Math.random() * 20.0;
                if( (double)purchasedUpgrades[11] > portChance ) {
                    m_botAction.specificPrize( name, Tools.Prize.PORTAL );
                    prized = true;
                }
                if( (double)purchasedUpgrades[11] > burstChance ) {
                    m_botAction.specificPrize( name, Tools.Prize.BURST );
                    prized = true;
                }
                // EMP ability; re-enable every 20 ticks
                //if( purchasedUpgrades[13] > 0 )
            }
            else if( shipNum == 3) {
                // Refueling ability; each level worth an additional 50%
                double fcChance = Math.random() * 2.0;
                if( (double)purchasedUpgrades[10] > fcChance ) {
                    m_botAction.specificPrize( name, Tools.Prize.FULLCHARGE );
                    prized = true;
                }
                // Energy stream ability; each level worth an additional 5%
                double superChance = Math.random() * 20.0;
                if( (double)purchasedUpgrades[11] > superChance ) {
                    m_botAction.specificPrize( name, Tools.Prize.SUPER );
                    prized = true;
                }
            } else if( shipNum == 8) {
                // Repel regen ability; each level worth an additional 10%
                double repChance = Math.random() * 10.0;
                if( (double)purchasedUpgrades[9] > repChance ) {
                    m_botAction.specificPrize( name, Tools.Prize.REPEL );
                    prized = true;
                }
            } else if( shipNum == 9 ) {
                // Allow another Comm every minute, up to max allowed
                if( tick % 2 == 0 ) {
                    if( currentComms < DEFAULT_OP_MAX_COMMS )
                        currentComms++;
                }

                // Regenerate OP
                double regenOPChance = Math.random() * 10.0;
                if( (double)purchasedUpgrades[2] + DEFAULT_OP_REGEN > regenOPChance ) {
                    m_botAction.sendPrivateMessage( arenaPlayerID, "+1 OP" );
                    currentOP++;
                }
            }
            if( prized )
                m_botAction.showObjectForPlayer( arenaPlayerID, LVZ_PRIZEDUP );
        }

        /**
         * Warps player to the appropriate spawning location (near a specific base).
         * Used after prizing.
         * @param roundStart True if the round is starting (ignore settings)
         */
        public void doWarp( boolean roundStart ) {
            if( waitInSpawn && !roundStart )
                return;
            int x = 0, y = 0;
            int base;
            base = getArmyID() % 2;
            if( roundStart && warpInBase ) {
                int xmod = (int)(Math.random() * 4) - 2;
                int ymod = (int)(Math.random() * 4) - 2;
                x = 512 + xmod;
                if( base == 0 )
                    y = BASE_CENTER_0_Y_COORD;
                else
                    y = BASE_CENTER_1_Y_COORD;
                y += ymod;
            } else {
                Random r = new Random();
                x = 512 + (r.nextInt(SPAWN_X_SPREAD) - (SPAWN_X_SPREAD / 2));
                if( base == 0 )
                    y = SPAWN_BASE_0_Y_COORD + (r.nextInt(SPAWN_Y_SPREAD) - (SPAWN_Y_SPREAD / 2));
                else
                    y = SPAWN_BASE_1_Y_COORD + (r.nextInt(SPAWN_Y_SPREAD) - (SPAWN_Y_SPREAD / 2));
            }
            m_botAction.warpTo(name, x, y);
        }

        /**
         * Sets up player for respawning.
         */
        public void doSetupRespawn() {
            isRespawning = true;
            if( fastRespawn ) {
                m_prizeQueue.addHighPriorityPlayer( this );
            } else {
                m_prizeQueue.addPlayer( this );
            }
            spawnTicks = TICKS_BEFORE_SPAWN;
        }

        /**
         * Sets up player for "respawning" at round start in order to wipe ports
         * and mines.  Does not use spawn ticks, so is considerably faster, prizing
         * players essentially at the normal spawn rate.
         */
        public void doSetupSpecialRespawn() {
            specialRespawn = true;
            if( fastRespawn ) {
                m_prizeQueue.addHighPriorityPlayer( this );
                /*
                if( getArmyID() == 0 )
                    m_prizeQueue_army0.addHighPriorityPlayer( this );
                else
                    m_prizeQueue_army1.addHighPriorityPlayer( this );
                */
            } else {
                m_prizeQueue.addPlayer( this );
                /*
                if( getArmyID() == 0 )
                    m_prizeQueue_army0.addPlayer( this );
                else
                    m_prizeQueue_army1.addPlayer( this );
                */
            }
        }

        /**
         * Warps player to the rearm area, no strings attached.
         */
        public void doRearmAreaWarp() {
            int base = getArmyID() % 2;
            int xmod = (int)(Math.random() * 4) - 2;
            int ymod = (int)(Math.random() * 4) - 2;
            if( base == 0 )
                m_botAction.warpTo(name, 512 + xmod, REARM_AREA_TOP_Y + ymod);
            else
                m_botAction.warpTo(name, 512 + xmod, REARM_AREA_BOTTOM_Y + ymod);
        }

        /**
         * Warps player to the safety part of the rearm area, to reset various
         */
        public void doRearmSafeWarp() {
            int base = getArmyID() % 2;
            if( base == 0 )
                m_botAction.warpTo(name, 512, REARM_SAFE_TOP_Y );
            else
                m_botAction.warpTo(name, 512, REARM_SAFE_BOTTOM_Y );
        }

        /**
         * Advances the player to the next rank.  Wrapper.
         */
        public void doAdvanceRank() {
            doAdvanceRank(1);
        }

        /**
         * Advances the player to the next rank.
         * @param multiRanks Number of ranks to gain.  Default is 1
         */
        public void doAdvanceRank( int numRanks ) {
            if( rank >= 80 ) {
                m_botAction.sendPrivateMessage(name, "-=(  FINAL RANK ATTAINED  )=-" );
                String shipname = ( shipNum == 9 ? "Tactical Ops" : Tools.shipName(shipNum) );
                m_botAction.sendPrivateMessage(name, "YOU ARE NOW A MASTER " + shipname.toUpperCase() + ".  ALL WILL FOREVER REMEMBER THE NAME OF " + name.toUpperCase() + " ... CONGRATULATIONS!!!", Tools.Sound.VICTORY_BELL );
                m_botAction.sendArenaMessage( name.toUpperCase() + " has become a MASTER " + shipname.toUpperCase() + ".  SALUTE THIS LIVING LEGEND!!", Tools.Sound.PLAY_MUSIC_ONCE );
                m_botAction.showObjectForPlayer(arenaPlayerID, LVZ_RANKUP);
                nextRank = 999999999;
                return;
            }
            rank++;
            upgPoints += 10;
            rankStart = nextRank;
            nextRank = m_shipGeneralData.get( shipNum ).getNextRankCost(rank);
            if( rank >= 25 || rank == 10 || rank == 15 || rank == 20 ) {
                String shipname = ( shipNum == 9 ? "Tactical Ops" : Tools.shipName(shipNum) );
                m_botAction.sendArenaMessage( name.toUpperCase() + " of " + getArmyName() + " has been promoted to RANK " + rank + " in the " + shipname + "!", 1 );
            }

            if( nextRank - rankPoints > 0 ) {
                String shipname = ( shipNum == 9 ? "Tactical Ops" : Tools.shipName(shipNum) );
                if( numRanks > 1 )
                    m_botAction.sendPrivateMessage(name, "-=(  " + numRanks + " RANKS UP !!  )=-  You are now a RANK " + rank + " " + shipname + ".  Next rank in " + ( nextRank - rankPoints )+ " RP.", Tools.Sound.VICTORY_BELL );
                else
                    m_botAction.sendPrivateMessage(name, "-=(  RANK UP!  )=-  You are now a RANK " + rank + " " + shipname + " pilot.  Next rank in " + ( nextRank - rankPoints )+ " RP.", Tools.Sound.VICTORY_BELL );
                m_botAction.showObjectForPlayer(arenaPlayerID, LVZ_RANKUP);
                m_botAction.sendPrivateMessage(name, "Gained +" + numRanks + "0 UP for any !upgrade available in the !armory." + ((upgPoints > 1) ? ("  (" + upgPoints + " available)") : "") );
            } else {
                // Advanced more than one rank; refire the method
                doAdvanceRank( numRanks + 1 );
                return;
            }
            resetProgressBar();
            initProgressBar();

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
            if ( rank >= RANK_REQ_SHIP9 ) {
                if( shipsAvail[8] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to provide battle support in a Tactical Ops position.  A shuttle craft that will take you to your operations terminal is now in your !hangar.");
                    //m_botAction.sendPrivateMessage(name, "SHARK: The Shark is piloted by our most clever and resourceful pilots.  Unsung heroes of the army, Sharks are both our main line of defense and leaders of every assault.  Advanced Sharks enjoy light gun capabilities and a cloaking device.");
                    addShipToDB(9);
                }
            }
        }

        /**
         * Adds # rank points to total rank point amt.
         * @param points Amt to add
         * @return True if player ranked up by adding these points
         */
        public boolean addRankPoints( int points ) {
            if( shipNum < 1 )
                return false;
            boolean rankedUp = false;
            if( points > 0 ) {
                if( DEBUG )
                    points = (int)((float)points * DEBUG_MULTIPLIER);
                if( !sendKillMessages ) {
                    bonusBuildup += points / 100;
                    while( bonusBuildup > 1.0 ) {
                        points++;
                        bonusBuildup--;
                    }
                }
            }
            rankPoints += points;
            recentlyEarnedRP += points;
            checkProgress();
            if( rankPoints >= nextRank ) {
                doAdvanceRank();
                rankedUp = true;
            }
            shipDataSaved = false;
            return rankedUp;
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
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();
            if( (shipNum == 3 && (purchasedUpgrades[10] > 0 || purchasedUpgrades[11] > 0)) ||
                (shipNum == 5 && (purchasedUpgrades[11] > 0 || purchasedUpgrades[13] > 0)) ||
                (shipNum == 8 && (purchasedUpgrades[9] > 0) ) ||
                (shipNum == 9) )
                m_specialAbilityPrizer.addPlayer(this);
            else
                m_specialAbilityPrizer.removePlayer(this);
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
                if( this.shipNum > 0 ) {
                    m_botAction.specWithoutLock( name );
                    lastShipNum = this.shipNum;     // Record for lagout
                }
                turnOffProgressBar();
            }
            if( this.shipNum == 0 )
                turnOnProgressBar();
            this.shipNum = shipNum;
            isRespawning = false;
            successiveKills = 0;
            recentlyEarnedRP = 0;
            currentOP = 0;
        }

        /**
         * Puts player in the currently-set ship and sets them as having respawned.
         */
        public void putInCurrentShip() {
            if( shipNum != 9 )
                m_botAction.setShip( name, shipNum );
            m_botAction.setFreq( name, getArmyID() );
            isRespawning = false;
            resetProgressBar();
            initProgressBar();
            if( (shipNum == 3 && (purchasedUpgrades[10] > 0 || purchasedUpgrades[11] > 0)) ||
                    (shipNum == 5 && (purchasedUpgrades[11] > 0 || purchasedUpgrades[13] > 0)) ||
                    (shipNum == 8 && (purchasedUpgrades[9] > 0) ) ||
                    (shipNum == 9) )
                    m_specialAbilityPrizer.addPlayer(this);
                else
                    m_specialAbilityPrizer.removePlayer(this);
        }

        /**
         * Bans the player from playing Distension.
         */
        public void ban() {
            banned = true;
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcBanned='y' WHERE fnID='" + playerID + "'" );
                saveCurrentShipToDB();
                m_botAction.sendSmartPrivateMessage(name, "You have been forcefully discharged from your army, and are now considered a civilian.  You may no longer play Distension." );
                m_botAction.sendUnfilteredPrivateMessage( name, "*kill" );
            } catch (SQLException e ) {
                Tools.printLog( "Error banning player " + name );
            }
        }

        /**
         * Unbans the player.
         */
        public void unban() {
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcBanned='n' WHERE fnID='" + playerID + "'" );
                m_botAction.sendSmartPrivateMessage(name, "You are no longer banned in Distension." );
                banned = false;
            } catch (SQLException e ) {
                Tools.printLog( "Error banning player " + name );
            }
        }

        /**
         * Increments successive kills.
         */
        public boolean addSuccessiveKill( ) {
            successiveKills++;

            int award = 0;
            if( successiveKills == 5 ) {
                award = 3;
                if( rank > 1 )
                    award = rank * 3;

                m_botAction.sendPrivateMessage(name, "Streak!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", 19 );
            } else if( successiveKills == 10 ) {
                award = 5;
                if( rank > 1 )
                    award = rank * 5;
                m_botAction.sendPrivateMessage(name, "ON FIRE!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", 20 );
            } else if( successiveKills == 15 ) {
                award = 7;
                if( rank > 1 )
                    award = rank * 7;
                m_botAction.sendPrivateMessage(name, "UNSTOPPABLE!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.VIOLENT_CONTENT );
            } else if( successiveKills == 20 ) {
                if( shipsAvail[5] == false ) {
                    String query = "SELECT * FROM tblDistensionShip WHERE fnPlayerID='" + playerID + "' AND ship.fnShipNum='6'";

                    try {
                        ResultSet r = m_botAction.SQLQuery( m_database, query );
                        if( r.next() ) {
                            m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcShip6='y' WHERE fnID='" + playerID + "'" );
                            m_botAction.sendPrivateMessage(name, "For 20 successive kills, your Weasel has been returned to the hangar!");
                        } else {
                            m_botAction.sendPrivateMessage(name, "AWARD FOR MASTERFUL DOGFIGHTING.  You are quite the pilot, and have proven yourself capable of joining our stealth operations.  The Weasel is now available in your !hangar." );
                            m_botAction.sendPrivateMessage(name, "WEASEL: The Weasel heads Covert Operations, providing scout reconnaissance to the rest of the army.  Its small size and cloaking allows it a freedom no others have.  Our newest Weasels now have the ability to cut off pursuit instantly!");
                            addShipToDB(6);
                        }
                        m_botAction.SQLClose(r);
                        return true;
                    } catch (SQLException e ) { return false; }
                } else {
                    award = 10;
                    if( rank > 1 )
                        award = rank * 7;
                    m_botAction.sendPrivateMessage(name, "INCONCEIVABLE!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.INCONCEIVABLE );
                }
            } else if( successiveKills == 50 ) {
                award = 15;
                if( rank > 1 )
                    award = rank * 15;
                m_botAction.sendPrivateMessage(name, "YOU'RE PROBABLY CHEATING!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.SCREAM );
            } else if( successiveKills == 99 ) {
                award = 20;
                if( rank > 1 )
                    award = rank * 30;
                m_botAction.sendPrivateMessage(name, "99 KILLS -- ... ORGASMIC !!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.ORGASM_DO_NOT_USE );
            }
            if( award > 0 ) {
                m_botAction.showObjectForPlayer(arenaPlayerID, LVZ_STREAK);
                addRankPoints(award);
            }
            return false;
        }

        /**
         * Performs defection to another army.
         * @param armyID ID of army which to defect
         */
        public void doDefect( int armyID ) {
            DistensionArmy oldarmy = getArmy();
            oldarmy.adjustPilotsTotal(-1);
            setArmy( armyID );
            getArmy().adjustPilotsTotal(1);
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fnArmyID=" + armyID + " WHERE fnID='" + playerID + "'" );
            } catch (SQLException e) {
                m_botAction.sendPrivateMessage( name, "ERROR CHANGING ARMY!  Report to a mod immediately!" );
            }
            m_botAction.sendPrivateMessage( name, "So you're defecting to " + getArmyName().toUpperCase() + "?  Can't blame you.  You'll be pilot #" + getArmy().getPilotsTotal() + "." );
            m_botAction.sendOpposingTeamMessageByFrequency(oldarmy.getID(), "TRAITOR!  Villainous dog!  " + name.toUpperCase() + " has betrayed us all for " + getArmyName().toUpperCase() + " !!  Spare not this worm a gruesome death ...");
            m_botAction.sendOpposingTeamMessageByFrequency(armyID, "Glory be to " + getArmyName().toUpperCase() + "!  " + name.toUpperCase() + " has joined our ranks!  Welcome this brave new pilot.");
            if( shipNum > 0 )
                m_botAction.setFreq(arenaPlayerID, armyID);
        }

        /**
         * Checks player for idling, and docks them if they are idle too long.
         */
        public void checkIdleStatus() {
            Player p = m_botAction.getPlayer(arenaPlayerID);
            if( p == null ) return;
            if( shipNum > 0 && shipNum != 9 && (p.getYTileLocation() <= TOP_SAFE || p.getYTileLocation() >= BOT_SAFE) ) {
                idleTicks++;
                if( idleTicks == IDLE_TICKS_BEFORE_DOCK - 1)
                    m_botAction.sendPrivateMessage(arenaPlayerID, "You appear to be idle; you will be automatically docked in " + IDLE_FREQUENCY_CHECK + " seconds if you do not move out of the rearmament area.");
                else if( idleTicks >= IDLE_TICKS_BEFORE_DOCK )
                    cmdDock(name, "");
            } else
                idleTicks = 0;
        }

        /**
         * Shares a portion of the RP "profits" earned in the last minute with a terr that
         * has the profit-sharing ability.
         * @param profits RP earned in the last minute by teammates.
         */
        public void shareProfits( int profits ) {
            if( isSupportShip() ) {
                float sharingPercent;
                if( rank >= 10 )
                    sharingPercent = 2.0f;
                else if( rank >= 5 )
                    sharingPercent = 1.0f;
                else
                    sharingPercent = 0.5f;
                if( shipNum == 5 )
                    sharingPercent += purchasedUpgrades[8];
                if( shipNum == 9 )
                    sharingPercent += purchasedUpgrades[0];
                int shared = Math.round((float)profits * (sharingPercent / 100.0f ));
                if( shared > 0 ) {
                    if( sendKillMessages )
                        m_botAction.sendPrivateMessage(arenaPlayerID, "Profit-sharing: +" + (DEBUG ? (int)(DEBUG_MULTIPLIER * shared) : shared ) + "RP" );
                    addRankPoints(shared);
                }
            }
        }

        /**
         * Checks for attaching in a Lanc, disabling the bomb ability when they
         * attach and re-enabling it when they detach.
         * @param attaching True if ship is attaching; false if detaching
         */
        public void checkLancAttachEvent( boolean attaching ) {
            // Ignore non-Lancs and lancs without the bomb upgrade
            if( shipNum != 7 || purchasedUpgrades[11] < 1 )
                return;
            if( attaching )
                m_botAction.specificPrize( arenaPlayerID, -Tools.Prize.BOMBS );
            else
                m_botAction.specificPrize( arenaPlayerID, Tools.Prize.BOMBS );
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
        public boolean clearSuccessiveKills() {
            boolean hadStreak = successiveKills >= 5;
            successiveKills = 0;
            return hadStreak;
        }

        /**
         * Toggles wait status (whether or not player will warp out into spawn).
         */
        public boolean waitInSpawn() {
            waitInSpawn = !waitInSpawn;
            return waitInSpawn;
        }

        /**
         * Toggles between warping into home base or spawn area at round start.
         */
        public boolean warpInBase() {
            warpInBase = !warpInBase;
            return warpInBase;
        }

        /**
         * Toggles between warping into home base or spawn area at round start.
         */
        public boolean sendKillMessages() {
            sendKillMessages = !sendKillMessages;
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcSendKillMsg='" + (sendKillMessages?'y':'n') +"' WHERE fnID='" + playerID + "'" );
            } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); }
            return sendKillMessages;
        }

        /**
         * Sets the player as assisting the army ID provided.  -1 to disable.
         * @param newArmyID ID of army player is assisting; -1 to disable assisting
         */
        public void setAssist( int newArmyID ) {
            assistArmyID = newArmyID;
        }

        /**
         * Increments time played by one minute.
         */
        public void incrementTime() {
            if( shipNum >= 0 )
                timePlayed++;
        }

        /**
         * Set lagout allowance.
         * @param allowed True if lagout allowed
         */
        public void setLagoutAllowed( boolean allowed ) {
            allowLagout = allowed;
        }

        /**
         * Adjust OP.
         * @param adjustment Amount by which to adjust
         */
        public void adjustOP( int adjustment ) {
            currentOP += adjustment;
            if( currentOP < 0 )
                currentOP = 0;
            else if( currentOP > maxOP )
                currentOP = maxOP;
        }

        /**
         * Adjust Comms.
         * @param adjustment Amount by which to adjust
         */
        public void adjustComms( int adjustment ) {
            currentComms += adjustment;
            if( currentComms < 0 )
                currentComms = 0;
            else if( currentComms > DEFAULT_OP_MAX_COMMS )
                currentComms = DEFAULT_OP_MAX_COMMS;
        }

        // GETTERS

        /**
         * @return Returns the name.
         */
        public String getName() {
            return name;
        }

        /**
         * @return Returns the ID as found in the DB (not as found in Arena).
         */
        public int getID() {
            return playerID;
        }

        /**
         * @return Returns the ID as found in the DB (not as found in Arena).
         */
        public int getArenaPlayerID() {
            return arenaPlayerID;
        }

        /**
         * @return Returns the ship number this player is currently playing, 1-8.  0 for spec, -1 for not logged in.
         */
        public int getShipNum() {
            return shipNum;
        }

        /**
         * @return Returns the ship number this player last used (lagged out in).
         */
        public int getLastShipNum() {
            return lastShipNum;
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
         * @return Returns strength of ship (upgrade level + default player strength).
         */
        public int getStrength() {
            return getRank() + RANK_0_STRENGTH;
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
         * @return Returns points earned since last rank was earned
         */
        public int getPointsSinceLastRank() {
            return rankPoints - rankStart;
        }

        /**
         * Use in combination w/ getPointsSinceLastRank()
         * @return Returns point amount needed for next rank, minus the amount needed for the last rank
         */
        public int getNextRankPointsProgressive() {
            return nextRank - rankStart;
        }

        /**
         * Returns the amount of points equal to a certain percentage of progress in a rank.
         * @param percent
         * @return
         */
        public int getRankPointsForPercentage( float percent ) {
            return (int)((float)getNextRankPointsProgressive() * (percent / 100.0f));
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
         * @return ID of opposing army, in two army system.
         */
        public int getOpposingArmyID() {
            if( getArmyID() == 0 )
                return 1;
            else
                return 0;
        }

        /**
         * @return Returns DistensionArmy the player to which the player is joined.
         */
        public DistensionArmy getArmy() {
            return m_armies.get( new Integer( getArmyID() ) );
        }

        /**
         * @return Returns DistensionArmy opposing the player, in two army system.
         */
        public DistensionArmy getOpposingArmy() {
            return m_armies.get( new Integer( getOpposingArmyID() ) );
        }

        /**
         * @return Returns army name.
         */
        public String getArmyName() {
            int id = getArmyID();
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
            if( shipNumToCheck < 1 || shipNumToCheck > 9 )
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
         * @return True if player is currently playing / in ship.
         */
        public boolean isInShip() {
            return shipNum > 0;
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
         * @return True if player is a support ship (5 or 8)
         */
        public boolean isSupportShip() {
            return (shipNum == 5 || shipNum == 8 || shipNum == 9);
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
            for( int i = 0; i<4; i++ ) {
                if( killedPlayerID == lastIDsKilled[i] )
                    repeats++;
            }
            // Cycle through
            lastIDsKilled[3] = lastIDsKilled[2];
            lastIDsKilled[2] = lastIDsKilled[1];
            lastIDsKilled[1] = lastIDsKilled[0];
            lastIDsKilled[0] = killedPlayerID;
            return repeats;
        }

        /**
         * @return RP earned since round start with current ship.
         */
        public int getRecentlyEarnedRP() {
            return recentlyEarnedRP;
        }

        /**
         * @return Number of minutes player has played since time was reset.
         */
        public int getMinutesPlayed() {
            return timePlayed;
        }

        /**
         * @return True if player wishes to receive kill msgs (RP award).
         */
        public boolean wantsKillMsg() {
            return sendKillMessages;
        }

        /**
         * @return True if player can use !lagout.
         */
        public boolean canUseLagout() {
            return allowLagout;
        }

        /**
         * @return Current OP points.
         */
        public int getCurrentOP() {
            return currentOP;
        }

        /**
         * @return Current communications left.
         */
        public int getCurrentComms() {
            return currentComms;
        }

        /**
         * @return Max OP points.
         */
        public int getMaxOP() {
            return maxOP;
        }

        // PROGRESS BAR
        public void checkProgress() {
            float pointsSince = getPointsSinceLastRank();
            int prog = 0;
            if( pointsSince > 0 )
                prog = (int)((pointsSince / (float)getNextRankPointsProgressive()) * 10.0f);
            if( prog > progress )
                updateProgressBar( prog );
            else if( prog < progress ) {
                resetProgressBar();
                initProgressBar();
            }
        }

        public void turnOnProgressBar() {
            playerObjs.showObject(arenaPlayerID, LVZ_PROGRESS_BAR );
            setPlayerObjects();
            initProgressBar();
        }

        public void turnOffProgressBar() {
            playerObjs.hideObject(arenaPlayerID, LVZ_PROGRESS_BAR );
            for( int i = 1; i < 10; i++ )
                playerObjs.hideObject(arenaPlayerID, LVZ_PROGRESS_BAR + i );
            setPlayerObjects();
        }

        /**
         * Initialize bars inside progress bar.
         */
        public void initProgressBar() {
            float pointsSince = getPointsSinceLastRank();
            progress = 0;
            int prog = 0;
            if( pointsSince > 0 )
                prog = (int)((pointsSince / (float)getNextRankPointsProgressive()) * 10.0f);
            updateProgressBar( prog );
        }

        /**
         * Wipe progress in bar clean.
         */
        public void resetProgressBar() {
            for( int i = 1; i < 10; i++ )
                playerObjs.hideObject(arenaPlayerID, LVZ_PROGRESS_BAR + i );
            setPlayerObjects();
            progress = 0;
        }

        /**
         * Update with given amount of progress.
         * @param newProgress Progress at present, which should be larger than old progress
         */
        public void updateProgressBar( int newProgress ) {
            if( newProgress == 0 || newProgress > 9 )
                return;
            // Make additions to bar between progress (old display) and prog (new amt/display)
            for( int i = progress + 1; i < newProgress + 1; i++ )
                playerObjs.showObject(arenaPlayerID, LVZ_PROGRESS_BAR + i );
            progress = newProgress;
            setPlayerObjects();
        }

        public void setPlayerObjects() {
            m_botAction.manuallySetObjects(playerObjs.getObjects(arenaPlayerID), arenaPlayerID);
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
        int profitShareRP;              // Amount of RP made from profit shares
        List <String>profitSharers;     // Players with profit-sharing on freq

        /**
         * Creates record for an army, given an ID.
         * @param armyID ID of the army
         */
        public DistensionArmy( int armyID ) {
            this.armyID = armyID;
            totalStrength = 0;
            flagsOwned = 0;
            try {
                ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fcArmyName, fnNumPilots, fcDefaultArmy, fcPrivateArmy, fcPassword FROM tblDistensionArmy WHERE fnArmyID='"+ armyID + "'" );
                if( r.next() ) {
                    armyName = r.getString( "fcArmyName" );
                    pilotsTotal = r.getInt( "fnNumPilots" );
                    isDefault = r.getString( "fcDefaultArmy" ).equals("y");
                    isPrivate = r.getString( "fcPrivateArmy" ).equals("y");
                    password = r.getString( "fcPassword" );
                }
                m_botAction.SQLClose(r);
            } catch( SQLException e ) {
                m_botAction.sendArenaMessage("Problem loading Army " + armyID );
            }
            profitShareRP = 0;
            profitSharers = Collections.synchronizedList( new LinkedList<String>() );
        }

        // SETTERS

        /**
         * Adjusts number of pilots total on army, both for internal purposes, and in DB.
         * @param value Pilot amount by which to adjust
         */
        public void adjustPilotsTotal( int value ) {
            pilotsTotal += value;
            if( pilotsTotal < 0 )
                pilotsTotal = 0;
            m_botAction.SQLBackgroundQuery( m_database, null, "UPDATE tblDistensionArmy SET fnNumPilots='" + pilotsTotal + "' WHERE fnArmyID='" + armyID + "'");
        }

        /**
         * Forcibly sets number of pilots total on army, both for internal purposes, and in DB.
         * @param value Pilot amount to be set
         */
        public void manuallySetPilotsTotal( int value ) {
            pilotsTotal = value;
            m_botAction.SQLBackgroundQuery( m_database, null, "UPDATE tblDistensionArmy SET fnNumPilots='" + pilotsTotal + "' WHERE fnArmyID='" + armyID + "'");
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
                    strength += p.getStrength();
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

        public void addSharedProfit( int shared ) {
            profitShareRP += shared;
        }

        // GETTERS

        /**
         * Gets profits earned, and sets to 0.
         */
        public int getProfits() {
            int share = profitShareRP;
            profitShareRP = 0;
            return share;
        }

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
        float baseRankCost;               // Points needed to reach pilot rank 1 in this ship;
        //   subsequent levels are determined by the scaling factor.

        public ShipProfile( int rankUnlockedAt, float baseRankCost) {
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
                return (int)(baseRankCost);

            float amt = baseRankCost;
            for(int i = 0; i < currentRank; i++ ) {
                if( i < 9 )
                    amt *= EARLY_RANK_FACTOR;
                else
                    if( i < 25 )
                        amt *= LOW_RANK_FACTOR;
                    else
                        if( i < 50)
                            amt *= NORMAL_RANK_FACTOR;
                        else
                            if( i < 70 )
                                amt *= HIGH_RANK_FACTOR;
                            else
                                amt *= STUPIDLY_HIGH_RANK_FACTOR;
            }
            return (int)( getNextRankCost( currentRank - 1 ) + amt );
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
        LinkedList <DistensionPlayer>priorityPlayers = new LinkedList<DistensionPlayer>();
        LinkedList <DistensionPlayer>players = new LinkedList<DistensionPlayer>();
        int runs = 0;       // # times queue has run since last spawn tick
        int delayTillNextSpawn = 0;

        /**
         * Adds a player to the end of the prizing queue.
         * @param p Player to add
         */
        public void addPlayer( DistensionPlayer p ) {
            if( !players.contains(p) )
                players.addLast(p);
        }

        /**
         * Adds a player to the head of the prizing queue.  For terrs and sharks
         * with the ability.
         * @param p Player to add
         */
        public void addHighPriorityPlayer( DistensionPlayer p ) {
            if( !priorityPlayers.contains(p) )
                priorityPlayers.addLast(p);
        }

        /**
         * Sets the time in ms until the next spawn is allowed.
         * @param delay Time in ms until next spawn is allowed
         */
        public void resumeSpawningAfterDelay( int delay ) {
            delayTillNextSpawn = delay;
        }

        public void run() {
            boolean spawned = false;
            boolean doTick = false;
            delayTillNextSpawn -= UPGRADE_DELAY;
            runs++;
            if( runs % DELAYS_BEFORE_TICK == 0 ) {
                doTick = true;
                runs = 0;
            }
            if( !priorityPlayers.isEmpty() ) {
                if( doTick )
                    for( DistensionPlayer p : priorityPlayers )
                        p.doSpawnTick();
                // If another player is not in the process of being prized, attempt to spawn
                if( delayTillNextSpawn <= 0 ) {
                    DistensionPlayer currentPlayer = priorityPlayers.peek();
                    spawned = currentPlayer.doSpawn();
                    if( spawned )
                        priorityPlayers.removeFirst();
                }
            }
            if( players.isEmpty() )
                return;
            if( doTick )
                for( DistensionPlayer p : players )
                    p.doSpawnTick();
            if( spawned )   // If high priority player was spawned, do not try to spawn normal player
                return;
            if( delayTillNextSpawn <= 0 ) {
                DistensionPlayer currentPlayer = players.peek();
                if( currentPlayer.doSpawn() )
                    players.removeFirst();
            }
        }
    }


    /**
     * Special ability prizer.
     */
    private class SpecialAbilityTask extends TimerTask {
        LinkedList <DistensionPlayer>players = new LinkedList<DistensionPlayer>();
        int ticks = 0;      // # times task has been run

        /**
         * Adds a player to be checked for random special prizing every 30 seconds.
         * @param p Player to add
         */
        public void addPlayer( DistensionPlayer p ) {
            players.remove(p);
            players.add(p);
        }

        public void removePlayer( DistensionPlayer p ) {
            players.remove(p);
        }

        /**
         * Checks all players added for special prizing.
         */
        public void run() {
            ticks++;
            for( DistensionPlayer p : players )
                p.prizeSpecialAbilities( ticks );
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

        m_mineClearedPlayers.clear();
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

        if( beginDelayedShutdown ) {
            cmdDie("", "shutdown");
            return;
        }
        int maxScore = (MAX_FLAGTIME_ROUNDS + 1) / 2;  // Score needed to win
        int roundNum = freq0Score + freq1Score + 1;

        String roundTitle = "";
        switch( roundNum ) {
            case 1:
                m_botAction.sendArenaMessage( "To win the battle, hold both flags for " + flagMinutesRequired + " minute" + (flagMinutesRequired == 1 ? "" : "s") + "  Winning " + maxScore + " battles will win the sector conflict." );
                roundTitle = "A new conflict";
                break;
            case MAX_FLAGTIME_ROUNDS:
                roundTitle = "FINAL BATTLE";
                break;
            default:
                roundTitle = "Battle " + roundNum;
        }

        String warning = "";
        if( freq0Score >= maxScore - 1 || freq1Score >= maxScore - 1 )
            warning = "  VICTORY IS IMMINENT!!";
        m_botAction.sendArenaMessage( roundTitle + " begins in " + getTimeString( INTERMISSION_SECS ) + ".  (Battles won: " + freq0Score + " to " + freq1Score + ")" + warning );

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
                m_botAction.sendArenaMessage( "THE CONFLICT IS OVER!!  " + m_armies.get(winningArmyID).getName() + " has laid total claim to the sector after " + (freq0Score + freq1Score) + " battles.", Tools.Sound.HALLELUJAH );
                m_botAction.sendArenaMessage( "---(   Double points awarded for winning the war!   )---" );
                gameOver = true;
            } else {
                int roundNum = freq0Score + freq1Score;
                m_botAction.sendArenaMessage( "BATTLE " + roundNum + " ENDED: " + m_armies.get(winningArmyID).getName() + " gains control of the sector after " + getTimeString( flagTimer.getTotalSecs() ) +
                        ".  Battles won: " + freq0Score + " to " + freq1Score, Tools.Sound.HALLELUJAH );
            }
        } else {
            m_botAction.sendArenaMessage( "END BATTLE: " + m_armies.get(winningArmyID).getName() + " gains control of the sector after " + getTimeString( flagTimer.getTotalSecs() ), Tools.Sound.HALLELUJAH );
        }

        float winnerStrCurrent;
        float loserStrCurrent;
        int losingArmyID;
        if( winningArmyID == 0 ) {
            winnerStrCurrent = m_armies.get(0).getTotalStrength();
            loserStrCurrent = m_armies.get(1).getTotalStrength();
            losingArmyID = 1;
        } else {
            winnerStrCurrent = m_armies.get(1).getTotalStrength();
            loserStrCurrent = m_armies.get(0).getTotalStrength();
            losingArmyID = 0;
        }
        if( winnerStrCurrent <= 0 ) winnerStrCurrent = 1;
        if( loserStrCurrent <= 0 ) loserStrCurrent = 1;

        boolean avarice = false;

        // Special case: if teams are imbalanced at end people do not !assist so they will win the round,
        //               fewer points are earned.
        if( loserStrCurrent / winnerStrCurrent < ASSIST_WEIGHT_IMBALANCE - 0.1f ) {
            // Abuse prevention: only fire avarice if losing team was also hurting in last snapshot
            float lastMajorStrengthSnapshot = flagTimer.getLastMajorStrength(losingArmyID);
            if( lastMajorStrengthSnapshot != -1 ) {      // If -1, no major snapshot is recorded and avarice can not be determined
                if( lastMajorStrengthSnapshot / winnerStrCurrent < ASSIST_WEIGHT_IMBALANCE) {
                    m_botAction.sendArenaMessage( "AVARICE DETECTED: Armies imbalanced.  -75% end round award!", 1 );
                    avarice = true;
                }
            }
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

        // Make sure to give a reasonable amount of points even if it's a short match
        if( minsToWin < 12 )
            minsToWin = 12;
        // Cap at 40 to keep extreme bonuses down
        if( minsToWin > 40 )
            minsToWin = 40;

        // Points to be divided up by army
        float totalPoints = (float)(minsToWin * 0.3f) * (float)opposingStrengthAvg * armyDiffWeight;
        float totalLvlSupport = 0;
        float totalLvlAttack = 0;
        float numSupport = 0;
        float numAttack = 0;
        Iterator <DistensionPlayer>i = m_players.values().iterator();
        while( i.hasNext() ) {
            DistensionPlayer p = i.next();
            if( p.getArmyID() == winningArmyID ) {
                Integer time = m_playerTimes.get( p.getName() );
                float percentOnFreq = 0;
                if( time != null )
                    percentOnFreq = (float)(secs - time) / (float)secs;
                if( p.isSupportShip() ) {
                    totalLvlSupport += ((float)p.getRank() * percentOnFreq );
                    numSupport++;
                } else {
                    totalLvlAttack += ((float)p.getRank() * percentOnFreq );
                    numAttack++;
                }
            }
        }
        if( totalLvlSupport < 1.0f) totalLvlSupport = 1.0f;
        if( totalLvlAttack < 1.0f) totalLvlAttack = 1.0f;
        float percentSupport = numSupport / ( numSupport + numAttack );
        float percentAttack;
        // If support makes up 40% or less of the team, support cut is percentage * support rate
        int supportPoints;
        int attackPoints;
        if( percentSupport < 0.4f ) {
            percentSupport = percentSupport * SUPPORT_RATE;
        } else {
            // >=40% support means support gets capped max of 40% * support rate
            percentSupport = 0.4f * SUPPORT_RATE;
        }
        supportPoints = Math.round( totalPoints * percentSupport );
        percentAttack = 1.0f - percentSupport;
        attackPoints = Math.round( totalPoints * percentAttack );

        // For display purposes only
        float attack, support, combo;
        if( avarice ) {
            attack = attackPoints / 4.0f;
            support = supportPoints / 4.0f;
            combo = attackPoints / 4.0f + supportPoints / 4.0f;
        } else {
            attack = attackPoints;
            support = supportPoints;
            combo = attackPoints + supportPoints;
        }
        if( DEBUG ) {
            attack *= DEBUG_MULTIPLIER;
            support *= DEBUG_MULTIPLIER;
            combo *= DEBUG_MULTIPLIER;
        }

        m_botAction.sendArenaMessage( "Total Victory Award: " + (int)combo + "RP  ...  Avg " + (int)(support / numSupport) + "RP for " + (int)numSupport + " on support (" + (int)(percentSupport * 100.0f) +
                                      "%); avg " + (int)(attack / numAttack) + "RP for " + (int)numAttack + " on attack (" + (int)(percentAttack * 100.0f) + "%)" );

        // Point formula: (min played/2 * avg opposing strength * weight) * your upgrade level / avg team strength
        i = m_players.values().iterator();
        int playerRank = 0;
        String topHolder = "N/A", topBreaker = "N/A";
        int topHolds = 0, topBreaks = 0;
        float points = 0;
        while( i.hasNext() ) {
            DistensionPlayer p = i.next();
            if( p.getArmyID() == winningArmyID && p.getShipNum() > 0 ) {
                playerRank = p.getRank();
                if( playerRank == 0 )
                    playerRank = 1;
                if( p.isSupportShip() )
                    points = (float)supportPoints * ((float)playerRank / totalLvlSupport);
                else
                    points = (float)attackPoints * ((float)playerRank / totalLvlAttack);
                if( avarice )
                    points /= 4;
                Integer time = m_playerTimes.get( p.getName() );
                if( time != null ) {
                    float percentOnFreq = (float)(secs - time) / (float)secs;
                    int modPoints = Math.max(1, Math.round(points * percentOnFreq) );
                    String victoryMsg;
                    if( gameOver ) {
                        modPoints *= 2;
                        victoryMsg = (int)(DEBUG ? modPoints * DEBUG_MULTIPLIER : modPoints ) + "RP (double) for the final victory (" + (int)(percentOnFreq * 100) + "% participation)" + ( avarice ? " [-75% for avarice]" : "" ) ;
                    } else {
                        victoryMsg = (int)(DEBUG ? modPoints * DEBUG_MULTIPLIER : modPoints ) + "RP for the victory (" + (int)(percentOnFreq * 100) + "% participation)" + ( avarice ? " [-75% for avarice]" : "" );
                    }
                    int holds = flagTimer.getSectorHolds( p.getName() );
                    int breaks = flagTimer.getSectorBreaks( p.getName() );
                    if( holds == topHolds && topHolds > 0 )
                        topHolder += ", " + p.getName();
                    else if( holds > topHolds ) {
                        topHolds = holds;
                        topHolder = p.getName();
                    }
                    if( breaks == topBreaks && topBreaks > 0 )
                        topBreaker += ", " + p.getName();
                    else if( breaks > topBreaks ) {
                        topBreaks = breaks;
                        topBreaker = p.getName();
                    }

                    int bonus = Math.max( 1, (holds * ( playerRank / 2 ) + breaks * (playerRank / 3)) );
                    int totalDisplay = (int)(DEBUG ? (bonus + modPoints) * DEBUG_MULTIPLIER : bonus + modPoints );
                    if( holds != 0 && breaks != 0 ) {
                        victoryMsg += ", + " + (int)(DEBUG ? bonus * DEBUG_MULTIPLIER : bonus ) + "RP for " + holds + " sector holds and " + breaks +" sector breaks = " + totalDisplay + " RP!" ;
                    } else if( holds != 0 ) {
                        victoryMsg += ", + " + (int)(DEBUG ? bonus * DEBUG_MULTIPLIER : bonus ) + "RP for " + holds + " sector holds = " + totalDisplay + "RP!";
                    } else if( breaks != 0 ) {
                        victoryMsg += ", + " + (int)(DEBUG ? bonus * DEBUG_MULTIPLIER : bonus ) + "RP for " + breaks +" sector breaks = " + totalDisplay + "RP!";
                    } else {
                        victoryMsg += "!";
                    }
                    m_botAction.sendPrivateMessage(p.getArenaPlayerID(), victoryMsg );
                    modPoints += bonus;
                    p.addRankPoints(modPoints);
                }
            }
        }
        m_botAction.sendArenaMessage( "Lead Defense: " + topBreaker + " [" + topBreaks + " breaks]  ...  Lead Assault: " + topHolder + " [" + topHolds + " holds]" );

        int intermissionTime;
        if( gameOver ) {
            intermissionTime = 20000;
            freq0Score = 0;
            freq1Score = 0;
        } else {
            intermissionTime = 10000;
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
            return;
        }

        if( beginDelayedShutdown ) {
            m_botAction.sendArenaMessage( "AUTOMATED SHUTDOWN INITIATED ...  Thank you for testing!" );
            cmdSaveData(m_botAction.getBotName(), "");
        }

        doScores(intermissionTime);

        // Swap out players waiting to enter
        if( !m_waitingToEnter.isEmpty() ) {
            LinkedList <DistensionPlayer>removals = new LinkedList<DistensionPlayer>();
            for( DistensionPlayer waitingPlayer : m_waitingToEnter ) {
                DistensionPlayer highestPlayer = null;

                for( DistensionPlayer p : m_players.values() ) {
                    if( p.getShipNum() >= 0 ) {
                        if( p.getMinutesPlayed() > highestPlayer.getMinutesPlayed() )
                            highestPlayer = p;
                    }
                }
                if( highestPlayer != null && waitingPlayer.getMinutesPlayed() < highestPlayer.getMinutesPlayed() ) {
                    cmdLeave(highestPlayer.getName(), "");
                    m_botAction.sendPrivateMessage( highestPlayer.getName(), "Another player wishes to enter the battle, and you have been playing the longest of any player.  Your slot has been given up to allow them to play." );
                    String name = waitingPlayer.getName();
                    m_botAction.sendPrivateMessage( name, "A slot has opened up.  You may now join the battle." );
                    waitingPlayer.setShipNum(0);
                    m_botAction.sendPrivateMessage( name, name.toUpperCase() + " authorized as a pilot of " + waitingPlayer.getArmyName().toUpperCase() + ".  Returning you to HQ." );
                    removals.add(waitingPlayer);
                }
            }
            for( DistensionPlayer p : removals )
                m_waitingToEnter.remove(p);
            for( DistensionPlayer p : m_waitingToEnter )
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "No suitable slot could be found for you this battle.  Please continue waiting and you will be placed in as soon as possible." );
        }

        intermissionTimer = new IntermissionTask();
        m_botAction.scheduleTask( intermissionTimer, intermissionTime );
    }


    /**
     * Adds all players to the hashmap which stores the time, in flagTimer time,
     * when they joined their freq.
     */
    public void setupPlayerTimes() {
        m_playerTimes.clear();

        Iterator <Player>i = m_botAction.getPlayingPlayerIterator();
        Player player;

        try {
            while( i.hasNext() ) {
                player = (Player)i.next();
                m_playerTimes.put( player.getPlayerName(), new Integer(0) );
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
     * Warp all players to the safety inside the rearm area 10 seconds before
     * starting a round.  Builds tension and removes mines from ships that may
     * only occasionally carry them (and so need not be respawned with the
     * special respawn).
     */
    private void safeWarp() {
        Iterator <DistensionPlayer>i = m_players.values().iterator();
        DistensionPlayer p;
        while( i.hasNext() ) {
            p = i.next();
            if( p != null && p.getShipNum() > 0 && !p.isRespawning() )
                p.doRearmSafeWarp();
        }
    }


    /**
     * Refreshes all support ships with their items, and removes portals where any
     * are in play before round starts.
     */
    private void refreshSupportShips() {
        Iterator <DistensionPlayer>i = m_players.values().iterator();
        DistensionPlayer p;
        while( i.hasNext() ) {
            p = i.next();
            if( (p.isSupportShip() && p.getShipNum() != 9) || p.getShipNum() == 4 || p.getShipNum() == 6 ) {
                m_botAction.setShip( p.getArenaPlayerID(), 1 );
                m_botAction.setShip( p.getArenaPlayerID(), p.getShipNum() );
                if( !p.isRespawning() )
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

    private class LagoutTask extends TimerTask {
        public String laggerName;

        public LagoutTask( String name ) {
            laggerName = name;
        }

        public void run() {
            DistensionPlayer player = m_players.get(laggerName);
            if( player != null && player.getShipNum() == 0 && flagTimer != null && flagTimer.isRunning() ) {
                String name = player.getName();

                player.setShipNum( player.getLastShipNum() );
                if( !player.getCurrentShipFromDB() ) {
                    m_botAction.sendPrivateMessage( name, "Error getting back in with that ship!  Please contact a mod." );
                    player.setShipNum( 0 );
                    return;
                }
                for( DistensionArmy a : m_armies.values() )
                    a.recalculateFigures();
                player.putInCurrentShip();
                player.prizeUpgradesNow();
                m_lagouts.remove( name );
                if( !flagTimeStarted || stopFlagTime ) {
                    checkFlagTimeStart();
                }
                cmdProgress( name, null );
                if( m_playerTimes.get( name ) == null ) {
                    m_playerTimes.put( name, new Integer( flagTimer.getTotalSecs() ) );
                    m_botAction.sendPrivateMessage( name, "No record of you in this battle ... starting your participation from scratch." );
                } else
                    m_botAction.sendPrivateMessage( name, "You have safely returned to your army." );
            }
        }
    }


    /**
     * This private class counts the consecutive flag time an individual team racks up.
     * Upon reaching the time needed to win, it fires the end of the round.
     */
    private class FlagCountTask extends TimerTask {
        int sectorHoldingArmyID, breakingArmyID, securingArmyID;
        int secondsHeld, totalSecs, breakSeconds, securingSeconds, preTimeCount;
        String breakerName = "";
        String securerName = "";
        boolean isStarted, isRunning, claimBeingBroken, claimBeingEstablished;
        HashMap <String,Integer>sectorHolds;        // Name -> # Holds
        HashMap <String,Integer>sectorBreaks;       // Name -> # Breaks
        HashMap <Integer,Integer>armyStrengths;     // ArmyID -> total cumulative strengths (60sec snapshots)
        int lastArmyStrength0;
        int lastArmyStrength1;

        /**
         * FlagCountTask Constructor
         */
        public FlagCountTask( ) {
            sectorHoldingArmyID = -1;
            breakingArmyID = -1;
            securingArmyID = -1;
            secondsHeld = 0;
            totalSecs = 0;
            breakSeconds = 0;
            securingSeconds = 0;
            isStarted = false;
            isRunning = false;
            claimBeingBroken = false;
            claimBeingEstablished = false;
            sectorHolds = new HashMap<String,Integer>();
            sectorBreaks = new HashMap<String,Integer>();
            armyStrengths = new HashMap<Integer,Integer>();
            lastArmyStrength0 = -1;
            lastArmyStrength1 = -1;
        }

        /**
         * Called by FlagClaimed when BOTH flags have been claimed by an army.
         *
         * @param armyID Frequency of flag claimer
         * @param pid PlayerID of flag claimer
         */
        public void sectorClaiming( int armyID, String pName ) {
            if( isRunning == false )
                return;

            // Failed sector claim break (securing took it back); give it back to sector-securing army
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

            // Start securing
            claimBeingBroken = false;
            breakingArmyID = -1;
            securingArmyID = armyID;
            securerName = pName;
            claimBeingEstablished = true;
            securingSeconds = 0;
        }

        /**
         * Called when a hold over the sector is established.
         */
        public void doSectorHold() {
            sectorHoldingArmyID = securingArmyID;
            m_botAction.showObject(LVZ_FLAG_CLAIMED); // Shows flag claimed lvz
            secondsHeld = 0;
            claimBeingEstablished = false;
            securingSeconds = 0;
            securingArmyID = -1;
            DistensionPlayer p = m_players.get(securerName);
            flagObjs.showObject(LVZ_SECTOR_HOLD);
            m_botAction.manuallySetObjects( flagObjs.getObjects() );
            if( p != null ) {
                addSectorHold( p.getName() );
                m_botAction.sendArenaMessage( "SECTOR HOLD: " + p.getArmyName() + " - " + p.getName() );
            } else {
                DistensionArmy a = m_armies.get( securingArmyID );
                if( a != null )
                    m_botAction.sendArenaMessage( "SECTOR HOLD: " + a.getName());
                else
                    m_botAction.sendArenaMessage( "Sector Hold." );
            }
        }

        /**
         * Called when a sector hold is in the process of being broken.
         */
        public void holdBreaking( int armyID, String pName ) {
            if( isRunning == false )
                return;

            // Failed sector securing; give it back to the old army but realize that
            // it's not a sector break
            if( claimBeingEstablished ) {
                claimBeingEstablished = false;
                breakingArmyID = -1;
                securingSeconds = 0;
                sectorHoldingArmyID = -1;
                return;
            }

            // Start breaking
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
                    m_botAction.sendArenaMessage( "SECTOR HOLD BROKEN!!  " + p.getArmyName() + " ("  + p.getName() + ") at 0:0" + remain + "!!", Tools.Sound.INCONCEIVABLE );
                else if( remain < 10 )
                    m_botAction.sendArenaMessage( "SECTOR HOLD BROKEN: "   + p.getArmyName() + " ("  + p.getName() + ") at 0:0" + remain + "!", Tools.Sound.CROWD_OOO );
                else
                    m_botAction.sendArenaMessage( "HOLD BROKEN: "   + p.getArmyName() + " - " + p.getName());
                addSectorBreak( p.getName() );
            } else {
                DistensionArmy a = m_armies.get( breakingArmyID );
                if( a != null )
                    m_botAction.sendArenaMessage( "HOLD BROKEN: " + a.getName());
                else
                    m_botAction.sendArenaMessage( "HOLD BROKEN" );
            }
            claimBeingBroken = false;
            breakSeconds = 0;
            breakingArmyID = -1;
            sectorHoldingArmyID = -1;
            secondsHeld = 0;
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
         * Records a "major" strength snapshot, which erases the last major, acting
         * as a ruler for general army strength within the last 5 minutes.
         */
        public void recordMajorStrengthSnapshot() {
            lastArmyStrength0 = m_armies.get(0).getTotalStrength();
            lastArmyStrength1 = m_armies.get(1).getTotalStrength();
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
         * @return ID of army that currently holds the sector (if any)
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
         * @param Army to get last strength info on
         * @return Last army strength of a particular army; -1 if no major snapshot taken yet.
         */
        public int getLastMajorStrength( int army ) {
            if( army == 0 )
                return lastArmyStrength0;
            else
                return lastArmyStrength1;
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
                recordMajorStrengthSnapshot();
            }

            if( claimBeingBroken ) {
                breakSeconds++;
                if( breakSeconds >= SECTOR_CHANGE_SECONDS ) {
                    doSectorBreak();
                }
                return;
            }

            if( claimBeingEstablished ) {
                securingSeconds++;
                if( securingSeconds >= SECTOR_CHANGE_SECONDS ) {
                    doSectorHold();
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
                float winnerStrCurrent;
                float loserStrCurrent;
                int losingArmyID;
                if( sectorHoldingArmyID == 0 ) {
                    winnerStrCurrent = Math.max(1, m_armies.get(0).getTotalStrength());
                    loserStrCurrent = Math.max(1, m_armies.get(1).getTotalStrength());
                    losingArmyID = 1;
                } else {
                    winnerStrCurrent = Math.max(1, m_armies.get(1).getTotalStrength());
                    loserStrCurrent = Math.max(1, m_armies.get(0).getTotalStrength());
                    losingArmyID = 0;
                }

                // Inform players that avarice may be a problem
                if( loserStrCurrent / winnerStrCurrent < ASSIST_WEIGHT_IMBALANCE - 0.1f ) {
                    m_botAction.sendOpposingTeamMessageByFrequency(sectorHoldingArmyID, "WARNING: Potential AVARICE; use !assist " + losingArmyID + " (rewarded; participation saved) or !pilot lower-ranked ships!" );
                    lastAssistReward = 0;           // Ensure anyone changing gets a reward
                    checkForAssistAdvert = true;    // ... and advert.
                }
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
     * -6:  Repel chance every 30 seconds, for sharks
     * -7:  Profit sharing (+1% of RP of each teammate's kills per level), for terrs / sharks
     *
     * Order of speed of rank upgrades (high speed to low speed, lower # being faster ranks):
     * Terr   - 10.2  (unlock @ 7)
     * Shark  - 11    (unlock @ 2)
     * Lanc   - 14    (unlock @ 10)
     * WB     - 15    (start)
     * Weasel - 15    (unlock by successive kills)
     * Spider - 16    (unlock @ 5)
     * Levi   - 16    (unlock @ 20)
     * Jav    - 16    (unlock @ 15)
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
        // 24: Decoy
        // 31: L3 Guns
        // 36: Mines
        // 44: Thor
        // 48: XRadar
        ship = new ShipProfile( 0, 15f );
        //                                                    | <--- this mark and beyond is not seen for upg names
        upg = new ShipUpgrade( "Side Thrusters           [ROT]", Tools.Prize.ROTATION, new int[]{7,7,7,8,8,9,10,12}, 0, 8 );           // 20 x8
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Density Reduction Unit   [THR]", Tools.Prize.THRUST, new int[]{4,5,6,6,6,7, 7, 8, 8, 9, 9,10,10,11,11,12}, 0, 16 );            // 1  x16
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Drag Balancer            [SPD]", Tools.Prize.TOPSPEED, new int[]{5,5,5,6,6,6,7,7,7,8,8,9,9,10}, 0, 14 );          // 200 x14
        ship.addUpgrade( upg );
        int costs1[] =          { 10,11, 12,13, 16,  19,25,37, 55,70 };
        int rechargeLevels1[] = { 0,  0, 10, 0, 20,  25, 0, 0, 40, 0 };
        upg = new ShipUpgrade( "Regeneration Drives      [CHG]", Tools.Prize.RECHARGE, costs1, rechargeLevels1, 10 );     // 300 x10
        ship.addUpgrade( upg );
        //                               L2     L2Mult    L3           L3 Multi
        //                 550    850   1150     1450    1750    2050
        //                     700  1000    1300     1600    1900     2200
        int costs1b[] =       {10,10,11, 13, 15,  21, 28, 35, 45, 60,  80 };
        int energyLevels1[] = { 0, 3, 5,  7, 15,  20, 25, 30, 35, 40,  45 };
        upg = new ShipUpgrade( "Microfiber Armor         [NRG]", Tools.Prize.ENERGY, costs1b, energyLevels1, 11 );    // 150 x12
        ship.addUpgrade( upg );
        int p1a1[] = {10, 25 };
        int p1a2[] = { 8, 31 };
        upg = new ShipUpgrade( "High-Impact Cannon", Tools.Prize.GUNS, p1a1, p1a2, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombs only as special)", Tools.Prize.BOMBS, 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Beam-Splitter", Tools.Prize.MULTIFIRE, 19, 17, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 55, 48, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Warbird Reiterator", Tools.Prize.DECOY, 14, 24, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Energy Concentrator", Tools.Prize.BOMBS, 21, 36, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Matter-to-Antimatter Converter", Tools.Prize.THOR, 34, 44, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S3", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S4", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // JAVELIN -- rank 15
        // Very slow upg speed; standard upgrades but recharge & energy have +2 max
        // 9:  L1 Guns
        // 15: Shrap (3 levels)
        // 26: Multi
        // 26: L2 Guns
        // 28: Decoy 1
        // 30: Shrap (7 more levels)
        // 35: Rocket
        // 40: L2 Bombs
        // 42: XRadar
        // 50: Decoy 2
        // 55: Shrap (10 more levels)
        // 60: L3 Guns
        // 80: L3 Bombs
        ship = new ShipProfile( RANK_REQ_SHIP2, 16f );
        upg = new ShipUpgrade( "Balancing Streams        [ROT]", Tools.Prize.ROTATION, new int[]{8,9,9,9,10,10,11,12,13,15}, 0, 10 );       // 20 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Fuel Economizer          [THR]", Tools.Prize.THRUST, new int[]{4,5,6,7,7,8,8,9,9,12,15,20}, 0, 12 );         //  1 x12
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Engine Reoptimization    [SPD]", Tools.Prize.TOPSPEED, new int[]{4,4,5,5,6,6,7,8,9,10}, 0, 10 );       // 200 x10
        ship.addUpgrade( upg );
        int costs2a[] = {10,11,11,12,12,  27,13,14,15,20,  34, 68,100 };
        int p2a2[] =    { 0, 0, 0, 0, 0,  15, 0,25,35,45,  55, 70, 80 };
        upg = new ShipUpgrade( "Tactical Engineering     [CHG]", Tools.Prize.RECHARGE, costs2a, p2a2, 13 ); // 150 x13
        ship.addUpgrade( upg );
        int costs2b[] =       { 9,10, 11, 13, 13, 24, 15, 16, 16, 19, 27,100 };
        int energyLevels2[] = { 2, 5, 10, 15, 20, 25, 30, 35, 40, 45, 55, 70 };
        upg = new ShipUpgrade( "Reinforced Plating       [NRG]", Tools.Prize.ENERGY, costs2b, energyLevels2, 12 );  // 146 x12
        ship.addUpgrade( upg );
        int p2b1[] = {12, 28, 55 };
        int p2b2[] = { 9, 26, 60 };
        upg = new ShipUpgrade( "Rear Defense System", Tools.Prize.GUNS, p2b1, p2b2, 3 );
        ship.addUpgrade( upg );
        int p2c1[] = { 50,200 };
        int p2c2[] = { 40, 80 };
        upg = new ShipUpgrade( "Mortar Explosive Enhancement", Tools.Prize.BOMBS, p2c1, p2c2, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Modified Defense Cannon", Tools.Prize.MULTIFIRE, 18, 23, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Detection System", Tools.Prize.XRADAR, 31, 42, 1 );
        ship.addUpgrade( upg );
        int p2d1[] = { 8,  35 };
        int p2d2[] = { 28, 50 };
        upg = new ShipUpgrade( "Javelin Reiterator", Tools.Prize.DECOY, p2d1, p2d2, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Emergency Fuel Supply", Tools.Prize.ROCKET, 25, 35, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Splintering Mortar 1", Tools.Prize.SHRAPNEL, 9, 15, 2 );
        ship.addUpgrade( upg );
        int p2e2[] = { 30, 40, 50 };
        upg = new ShipUpgrade( "Splintering Mortar 2", Tools.Prize.SHRAPNEL, 11, p2e2, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Splintering Mortar 3", Tools.Prize.SHRAPNEL, 18, 55, 5 );
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
        ship = new ShipProfile( RANK_REQ_SHIP3, 16f );
        upg = new ShipUpgrade( "Central Realigner        [ROT]", Tools.Prize.ROTATION, new int[]{6,7,8,8,8,9,9,9,10}, 0, 9 );       // 20 x9
        ship.addUpgrade( upg );
        int p3a1[] = { 3, 5, 5, 6, 7,   8,  9, 10, 11, 12 };
        int p3a2[] = { 0, 5, 6, 7, 8,  10, 15, 25, 35, 45 };
        upg = new ShipUpgrade( "Sling Drive              [THR]", Tools.Prize.THRUST, p3a1, p3a2, 10 );     //   1 x10
        ship.addUpgrade( upg );
        int p3b1[] = { 4, 4, 5, 6, 7, 8,   9,  9, 10, 11, 12, 14 };
        int p3b2[] = { 0, 2, 3, 4, 5, 6,  10, 15, 25, 35, 45, 50 };
        upg = new ShipUpgrade( "Spacial Filtering        [SPD]", Tools.Prize.TOPSPEED, p3b1, p3b2, 12 );   // 250 x12
        ship.addUpgrade( upg );
        int p3c1[] = {10,10,11,13,15,17,19,20,20,22,24,27,28,29,34,39,43,50,60,80 };
        int p3c2[] = { 0, 0, 0, 0, 0, 0, 0, 25, 0, 0,50,50,50,60,60,70,70,80,80,80 };
        upg = new ShipUpgrade( "Recompensator            [CHG]", Tools.Prize.RECHARGE, p3c1, p3c2, 20 );     // 230 x20
        ship.addUpgrade( upg );
        int costs3[] =        {11,11,12,13,14,15,16,17,19,25,29,44 };
        int energyLevels3[] = { 0, 3, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50 };
        upg = new ShipUpgrade( "Molecular Shield         [NRG]", Tools.Prize.ENERGY, costs3, energyLevels3, 12 ); // 140 x12
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Rapid Disintigrator", Tools.Prize.GUNS, 40, 40, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombing ability disabled)", Tools.Prize.BOMBS, 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Split Projector", Tools.Prize.MULTIFIRE, 28, 26, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 45, 42, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Spider Reiterator", Tools.Prize.DECOY, new int[]{9,15,23}, new int[]{18,29,47}, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "+50% Refeuler", ABILITY_REFUELER, new int[]{12,17}, new int[]{9,33}, 2 );
        ship.addUpgrade( upg );
        int p3f1[] = { 13, 15, 17, 19, 20 };
        int p3f2[] = { 15, 25, 35, 45, 55 };
        upg = new ShipUpgrade( "+5% Infinite Stream", ABILITY_SUPER, p3f1, p3f2, 5 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Warp Field Stabilizer", Tools.Prize.ANTIWARP, 35, 38, 1 );
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
        // 25: Stealth
        // 28: Portal
        // 30: Multi (close shotgun)
        // 35: L3 Guns
        // 40: Decoy
        // 44: XRadar
        // 48: L3 Bombs
        // 60: Prox
        // 70: Shrapnel
        ship = new ShipProfile( RANK_REQ_SHIP4, 16f );
        upg = new ShipUpgrade( "Gravitational Modifier   [ROT]", Tools.Prize.ROTATION, new int[]{9,5,6,9,10,10,11,12}, 0, 8 );       // 20 x8
        ship.addUpgrade( upg );
        int p4a1[] = {20, 30, 40, 50, 60 };
        int p4a2[] = { 8, 20, 40, 50, 60 };
        upg = new ShipUpgrade( "Force Thrusters          [THR]", Tools.Prize.THRUST, p4a1, p4a2, 5 );    // 4 x5
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Collection Drive         [SPD]", Tools.Prize.TOPSPEED, 15, 0, 7 );      //1000 x7
        ship.addUpgrade( upg );
        int costs4[] = { 9, 10, 12, 14, 15, 16,  18, 20, 22, 27, 30, 35 };
        upg = new ShipUpgrade( "Power Recirculator       [CHG]", Tools.Prize.RECHARGE, costs4, 0, 12 );  // 140 x12
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Carbon-Forced Armor      [NRG]", Tools.Prize.ENERGY, 10, 0, 20 );         // 100 x20
        ship.addUpgrade( upg );
        int p4b1[] = { 10,40 };
        int p4b2[] = { 8, 35 };
        upg = new ShipUpgrade( "Spill Guns", Tools.Prize.GUNS, p4b1, p4b2, 2 );         // DEFINE
        ship.addUpgrade( upg );
        int p4c1[] = { 7, 20, 55 };
        int p4c2[] = { 4, 13, 48 };
        upg = new ShipUpgrade( "Chronos(TM) Disruptor", Tools.Prize.BOMBS, p4c1, p4c2, 3 );        // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Sidekick Cannons", Tools.Prize.MULTIFIRE, 16, 30, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 22, 44, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Leviathan Reiterator", Tools.Prize.DECOY, 15, 40, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Wormhole Inducer", Tools.Prize.PORTAL, 43, 28, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Scrambler", Tools.Prize.STEALTH, 18, 25, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Proximity Bomb Detonator", Tools.Prize.PROXIMITY, 60, 60, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Splintering Mortar", Tools.Prize.SHRAPNEL, 45, 70, 1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // TERRIER -- rank 7
        // Very fast upg speed; rotation, energy has only 9 lvls; thrust starts -1 but has HIGH max
        // 2:  Burst 1
        // 3: XRadar
        // 5: +10% Regeneration (and 1 level available every 5 levels)
        // 7:  Portal 1
        // 9:  Priority respawn
        // 13: Profit-sharing 1
        // 16: Multi (slightly more forward than regular)
        // 21: Burst 2
        // 23: Profit-sharing 2
        // 29: Portal 2
        // 33: Profit-sharing 3
        // 36: L2 Guns
        // 40: (Can attach to other terrs)
        // 43: Profit-sharing 4
        // 46: Burst 3
        // 48: Portal 3
        // 50: Targetted EMP (negative full charge to all of the other team)
        // 53: Profit-sharing 5
        // 55: Burst 4
        // 60: Portal 4
        // 65: Burst 5
        // 70: Portal 5
        // 80: Burst 6
        ship = new ShipProfile( RANK_REQ_SHIP5, 10.4f );
        upg = new ShipUpgrade( "Correction Engine        [ROT]", Tools.Prize.ROTATION, 7, 0, 15 );         // 20 x15
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Interwoven Propulsor     [THR]", Tools.Prize.THRUST, 7, 0, 10 );           // 2 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Microspiral Drive        [SPD]", Tools.Prize.TOPSPEED, 7, 0, 16 );         // 325 x16
        ship.addUpgrade( upg );
        int costs5a[] = { 10, 13, 15, 18, 22, 26, 35, 40, 45, 50 };
        upg = new ShipUpgrade( "Hull Reconstructor       [CHG]", Tools.Prize.RECHARGE, costs5a, 0, 10 );  // 180 x10
        ship.addUpgrade( upg );
        int costs5b[] = { 10, 12, 13, 14, 15,  20, 23, 27, 30 };
        upg = new ShipUpgrade( "Hull Capacity            [NRG]", Tools.Prize.ENERGY, costs5b, 0, 9 );      // 150 x9
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Defense Systems", Tools.Prize.GUNS, 28, 36, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Multiple Cannons", Tools.Prize.MULTIFIRE, 14, 16, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 5, 0, 1 );
        ship.addUpgrade( upg );
        int p5d1[] = {  8, 10, 14, 18, 20 };
        int p5d2[] = { 13, 23, 33, 43, 53 };
        upg = new ShipUpgrade( "+1% Profit Sharing", ABILITY_PROFIT_SHARING, p5d1, p5d2, 5 );
        ship.addUpgrade( upg );
        int p5a1[] = {12, 25, 30, 35, 40 };
        int p5a2[] = { 7, 29, 48, 60, 70 };
        upg = new ShipUpgrade( "Wormhole Creation Kit", Tools.Prize.PORTAL, p5a1, p5a2, 5 );        // DEFINE
        ship.addUpgrade( upg );
        int p5b1[] = { 6, 17, 28, 30, 40, 50 };
        int p5b2[] = { 2, 21, 46, 55, 65, 80 };
        upg = new ShipUpgrade( "Rebounding Burst", Tools.Prize.BURST, p5b1, p5b2, 6 );       // DEFINE
        ship.addUpgrade( upg );
        int p5c1[] = { 5, 10, 15, 20, 25, 30, 35, 40, 45, 50 };
        upg = new ShipUpgrade( "+5% Regeneration", ABILITY_TERR_REGEN, 12, p5c1, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Priority Rearmament", ABILITY_PRIORITY_REARM, 8, 9, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Targetted EMP", ABILITY_TARGETTED_EMP, 40, 50, 1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // WEASEL -- Unlocked by 20(tweak?) successive kills w/o dying
        // Slow-medium upg speed; all upg start 2 levels higher than normal calcs, but 1st level (3rd) costs 2pts
        //                        and all have level requirements; special upgrades come somewhat early
        //  5: XRadar
        //  7: Rocket 1
        // 13: L2 Guns
        // 18: Multifire
        // 23: Cloak
        // 29: Portal
        // 35: L3 Guns
        // 40: Stealth
        // 42: Decoy
        // 46: Rocket 2
        // 50: Brick
        // 60: Rocket 3
        ship = new ShipProfile( RANK_REQ_SHIP6, 15f );
        int p6a1a[] = {15, 9,  8,  7,  7,  6, 5 };
        int p6a2a[] = { 3, 8, 10, 15, 20, 30, 1 };
        upg = new ShipUpgrade( "Orbital Force Unit       [ROT]", Tools.Prize.ROTATION, p6a1a, p6a2a, 7 );       // 20 x7
        ship.addUpgrade( upg );
        int p6a1b[] = { 15,10,  9,  8, 8, 7, 6, 5 };
        int p6a2b[] = { 3,  8, 10, 20, 1, 1, 1, 1 };
        upg = new ShipUpgrade( "Gravity Shifter          [THR]", Tools.Prize.THRUST, p6a1b, p6a2b, 8 );         // 1 x8
        ship.addUpgrade( upg );
        int p6a1c[] = { 15, 9,  9,  8,  7, 6, 6, 5, 4, 3 };
        int p6a2c[] = { 3,  8, 10, 15, 20, 1, 1, 1, 1, 1  };
        upg = new ShipUpgrade( "Time Distorter           [SPD]", Tools.Prize.TOPSPEED, p6a1c, p6a2c, 10 );       // 150 x10
        ship.addUpgrade( upg );
        int p6a1d[] = { 15,14,13, 12, 14, 18, 24, 29, 33, 40 };
        int p6a2d[] = { 5, 8, 10, 15, 20, 30, 40, 50, 60, 70 };
        upg = new ShipUpgrade( "Influx Recapitulator     [CHG]", Tools.Prize.RECHARGE, p6a1d, p6a2d, 10 );      // 150 x10
        ship.addUpgrade( upg );
        int p6a1e[] = { 15,16,17, 18, 19, 20, 30, 20, 20, 24, 27, 50, 60, 75 };
        int p6a2e[] = {  3, 5, 8, 10, 15, 20, 30, 40, 50, 55, 60, 65, 70, 75 };
        upg = new ShipUpgrade( "Cerebral Shielding       [NRG]", Tools.Prize.ENERGY, p6a1e, p6a2e, 14 );        // 100 x14
        ship.addUpgrade( upg );
        int p6b1[] = { 16, 32 };
        int p6b2[] = { 13, 35 };
        upg = new ShipUpgrade( "Low Propulsion Cannons", Tools.Prize.GUNS, p6b1, p6b2, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombing ability disabled)", Tools.Prize.BOMBS, 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Cannon Distributor", Tools.Prize.MULTIFIRE, 19, 18, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 8, 5, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Escape Tunnel", Tools.Prize.PORTAL, 33, 29, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Scrambler", Tools.Prize.STEALTH, 40, 40, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Light-Bending Unit", Tools.Prize.CLOAK, 25, 23, 1 );
        ship.addUpgrade( upg );
        int p6c1[] = {12, 21, 30 };
        int p6c2[] = { 9, 46, 60 };
        upg = new ShipUpgrade( "Assault Boosters", Tools.Prize.ROCKET, p6c1, p6c2, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Movement Inhibitor", Tools.Prize.BRICK, 39, 50, 1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // LANCASTER -- special unlock  (10 for beta)
        // Fast upgrade speed; all upgrades only get lanc to 120% of stock lanc, but energy has few level requirements.
        // 0: +1 Guns (other gun is level 38 but has no upgrade cost)
        // 20: Multifire
        // 26: Bombing special ability
        // 38: +1 Guns (other gun costs 6 but is available from start; these are free)
        // 45: The Firebloom
        // 55: Prox
        // 60: XRadar
        // 69: Shrap (10 levels)
        // 80: Decoy
        ship = new ShipProfile( 10, 14f );       // Level 10 unlock: beta only
        upg = new ShipUpgrade( "Directive Realigner      [ROT]", Tools.Prize.ROTATION, new int[]{5,5,5,5,5}, 0, 5 );        //  20 x5
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "InitiaTek Burst Engine   [THR]", Tools.Prize.THRUST, new int[]{4,5,6,6,7, 7,8,9,9,10}, 0, 10 );         //   1 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Streamlining Unit        [SPD]", Tools.Prize.TOPSPEED, new int[]{3,5,5,5,5,6,6,5,7,7}, 0, 10 );        // 150 x10
        ship.addUpgrade( upg );
        int costs7[] = { 11, 12, 15, 16, 21, 22, 29, 35 };
        upg = new ShipUpgrade( "Pneumatic Refiltrator    [CHG]", Tools.Prize.RECHARGE, costs7, 0, 8 );        // 250 x8
        ship.addUpgrade( upg );
        int p7a1[] = { 0, 0, 5, 10, 15, 20, 25, 30 };
        upg = new ShipUpgrade( "Interlocked Deflector    [NRG]", Tools.Prize.ENERGY, costs7, p7a1, 8 );       // 150 x8
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Modernized Projector 1", Tools.Prize.GUNS, 45, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Modernized Projector 2", Tools.Prize.GUNS, 0, 38, 1 );  // best of all ... IT'S FREE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Magnified Output Force", Tools.Prize.MULTIFIRE, 23, 20, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 30, 60, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Lancaster Reiterator", Tools.Prize.DECOY, 50, 80, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "The Firebloom", Tools.Prize.BURST, 32, 45, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Lancaster Special!", Tools.Prize.BOMBS, 21, 26, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Proximity Bomb Detonator", Tools.Prize.PROXIMITY, 42, 55, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Shrapnel", Tools.Prize.SHRAPNEL, 15, 69, 10 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // SHARK -- rank 2
        // Very fast upg speed; rotation has small spread (+2start) and few upgrades; thrust starts 1 down
        //                      but has high max; energy starts high, has low level req initially, but
        //                      has high req later on (designed to give bomb capability early)
        // (Starts with 1 repel, so that repel 1 is actually second)
        //  4: Repel 1
        // 12: Guns
        // 15: Repel 2
        // 17: XRadar
        // 20: Priority Rearmament
        // 22: Shrap 1
        // 25: Repel 3
        // 28: Multifire
        // 30: +10% Repel Regen 1
        // 38: Cloak
        // 40: Repel 4
        // 41: Shrap 2
        // 45: L2 Bombs
        // 48: +10% Repel Regen 2
        // 50: Repel 5
        // 55: +10% Repel Regen 3
        // 60: Repel 6
        // 65: Shrap 3
        // 70: Repel 7
        // 80: Repel 8
        ship = new ShipProfile( RANK_REQ_SHIP8, 11f );
        int p8a1[] = {  7,  8,  7,  8,  7,  8, 15 };
        int p8a2[] = { 10, 11, 12, 13, 14, 15, 16 };
        upg = new ShipUpgrade( "Runningside Correctors   [ROT]", Tools.Prize.ROTATION, p8a1, p8a2, 7 );     // 20 x7
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Spitfire Thrusters       [THR]", Tools.Prize.THRUST, new int[]{5,6,6,7,7,8,8,9,9,10,11,12}, 0, 12 );            // 1  x12
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Space-Force Emulsifier   [SPD]", Tools.Prize.TOPSPEED, new int[]{4,5,6,6,7,7,8,8,9, 9,10,12}, 0, 12 );          // 200 x12
        ship.addUpgrade( upg );
        int costs8[] = {10,11,11,12,15, 18,21,23,25,29,55 };
        upg = new ShipUpgrade( "Light Charging Mechanism [CHG]", Tools.Prize.RECHARGE, costs8, 1, 11 );     // 150 x11
        ship.addUpgrade( upg );
        int p8b1[] = {10,10, 12, 14, 16, 20, 30, 80 };
        int p8b2[] = { 3, 5, 10, 20, 30, 40, 50, 75 };
        upg = new ShipUpgrade( "Projectile Slip PlateS   [NRG]", Tools.Prize.ENERGY, p8b1, p8b2, 8 );       // 150 x8
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Emergency Defense Cannon", Tools.Prize.GUNS, 10, 12, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Plasma-Infused Weaponry", Tools.Prize.BOMBS, 35, 45, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Spreadshot", Tools.Prize.MULTIFIRE, 24, 28, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 15, 17, 1 );
        ship.addUpgrade( upg );
        int p8ca1[] = { 13, 18, 30 };
        int p8ca2[] = { 25, 48, 55 };
        upg = new ShipUpgrade( "+10% Repulsor Regeneration", -6, p8ca1, p8ca2, 3 );
        ship.addUpgrade( upg );
        int p8c1[] = { 6, 14, 17, 20, 30, 40, 50, 60 };
        int p8c2[] = { 4, 18, 30, 40, 50, 60, 70, 80 };
        upg = new ShipUpgrade( "Gravitational Repulsor", Tools.Prize.REPEL, p8c1, p8c2, 8 );    // DEFINE
        ship.addUpgrade( upg );
        int p8d2[] = { 22, 41, 65 };
        upg = new ShipUpgrade( "Splintering Unit", Tools.Prize.SHRAPNEL, 11, p8d2, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Nonexistence Device", Tools.Prize.CLOAK, 55, 38, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Priority Rearmament", ABILITY_PRIORITY_REARM, 9, 20, 1 );   // DEFINE
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // TACTICAL OPS -- rank 25
        ship = new ShipProfile( RANK_REQ_SHIP9, 12f );
        upg = new ShipUpgrade( "+1% Profit Sharing", ABILITY_PROFIT_SHARING, 0, 0, 1 ); //
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "+1 Max OP Points", OPS_INCREASE_MAX_OP, 0, 0, 1 );      //
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "+20% OP Regen Rate", OPS_REGEN_RATE, 0, 0, 4 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Communications Systems", OPS_COMMUNICATIONS, 0, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Systems", OPS_RADAR, 0, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Efficient Rearmament", OPS_FAST_TEAM_REARM, 0, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Emergency Base Cover", OPS_COVER, 0, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Security Door Systems", OPS_DOOR_CONTROL, 0, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Orb of Seclusion", OPS_SECLUSION, 0, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "False Minefield", OPS_MINEFIELD, 0, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Shroud of Darkness", OPS_SHROUD, 0, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Full Sensor Disable", OPS_FLASH, 0, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Defensive Shields", OPS_TEAM_SHIELDS, 0, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "EMP Pulse", ABILITY_TARGETTED_EMP, 0, 0, 1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );
    }

}