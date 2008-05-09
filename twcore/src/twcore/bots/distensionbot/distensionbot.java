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
import java.lang.reflect.Field;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.command.CommandInterpreter;
import twcore.core.command.TWCoreException;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FlagClaimed;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.FrequencyChange;
import twcore.core.events.TurretEvent;
import twcore.core.events.PlayerPosition;
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
 * - Prismatic array LVZ
 * - LVZ for sudden death and hybrid flag mode
 * - LVZ for ship unlocks
 *
 *
 * Lower priority (in order):
 * - !intro, !intro2, !intro3, etc.
 * - F1 Help -- item descriptions?
 *
 * @author dugwyler
 */
public class distensionbot extends SubspaceBot {

    private boolean DEBUG = true;                         // Debug mode.
    private final float DEBUG_MULTIPLIER = 4.7f;          // Amount of RP to give extra in debug mode

    private final int NUM_UPGRADES = 15;                   // Number of upgrade slots allotted per ship
    private final int AUTOSAVE_DELAY = 5;                  // How frequently autosave occurs, in minutes
    private final int MESSAGE_SPAM_DELAY = 75;             // Delay in ms between a long list of spammed messages
    private final int PRIZE_SPAM_DELAY = 10;               // Delay in ms between prizes for individual players
    private final int UPGRADE_DELAY = 50;                  // How often the prize queue rechecks for prizing
    private final int DELAYS_BEFORE_TICK = 10;             // How many UPGRADE_DELAYs before prize queue runs a tick
    private final int TICKS_BEFORE_SPAWN = 10;             // # of UPGRADE_DELAYs * DELAYS_BEFORE_TICK before respawn
    private final int IDLE_FREQUENCY_CHECK = 10;           // In seconds, how frequently to check for idlers
    private final int IDLE_TICKS_BEFORE_DOCK = 18;         // # IDLE_FREQUENCY_CHECKS in idle before player is docked
    private final int OPS_IDLE_TICKS_BEFORE_DOCK = 30;     // # IDLE_FREQUENCY_CHECKS in idle before Ops is docked
    private final int LAGOUT_VALID_SECONDS = 60;           // # seconds since lagout in which you can use !lagout
    private final int LAGOUTS_ALLOWED = 3;                 // # lagouts allowed per round
    private final int PROFIT_SHARING_FREQUENCY = 2 * 60;   // # seconds between adding up profit sharing for terrs
    private final int SCRAP_CLEARING_FREQUENCY = 60;       // # seconds after the most recent scrap is forgotten
    private final int WARP_POINT_CHECK_FREQUENCY = 9;      // # seconds between checking warp points for players
    private final int VENGEFUL_VALID_SECONDS = 12;         // # seconds after V.B. fire in which VB gets RP bonus
    private final int STREAK_RANK_PROXIMITY_DIVISOR = 2;   // Divisor for rank to determine streak rank prox (50 / 2 = 25 rank prox)
    private final int STREAK_RANK_PROXIMITY_MINIMUM = 10;  // Min streak rank proximity allowed
    private final int PILOTS_REQ_EACH_ARMY = 3;            // # players needed on each army for game to start
    private final float SUPPORT_RATE = 1.5f;               // Multiplier for support's cut of end round bonus
    private final String DB_PROB_MSG = "That last one didn't go through.  Database problem, it looks like.  Please send a ?help message ASAP.";
    private final float EARLY_RANK_FACTOR = 1.6f;          // Factor for rank increases (lvl 1-9)
    private final float LOW_RANK_FACTOR = 1.15f;           // Factor for rank increases (lvl 10+)
    private final float NORMAL_RANK_FACTOR = 1.10f;        // Factor for rank increases (lvl 25+)
    private final float HIGH_RANK_FACTOR = 1.25f;          // Factor for rank increases (lvl 50+)
    private final float STUPIDLY_HIGH_RANK_FACTOR = 1.7f;  // Factor for rank increases (lvl 70+)
    private final int RANK_DIFF_MED = 20;                  // Rank difference calculations
    private final int RANK_DIFF_VHIGH = 40;                // for humiliation and rank RP caps
    private final int RANK_DIFF_HIGHEST = 50;
    private final int RANK_0_STRENGTH = 25;                // How much str a rank 0 player adds to army (rank1 = 1 + rank0str, etc)

    private final int RANK_REQ_SHIP2 = 6;    // 15
    private final int RANK_REQ_SHIP3 = 4;    //  5
    private final int RANK_REQ_SHIP4 = 8;    // 20
    private final int RANK_REQ_SHIP8 = 2;    //  2
    // Specials
    private final int RANK_REQ_SHIP6 = 4;    // N/A (only has level for beta)
    private final int RANK_REQ_SHIP7 = 10;   // N/A (only has level for beta)
    private final int RANK_REQ_SHIP9 = 20;   // All ships rank 10

    // Required number of battles won to be promoted up the various ranks
    private final int WINS_REQ_RANK_CADET_4TH_CLASS = 1;
    private final int WINS_REQ_RANK_CADET_3RD_CLASS = 3;
    private final int WINS_REQ_RANK_CADET_2ND_CLASS = 7;
    private final int WINS_REQ_RANK_CADET_1ST_CLASS = 13;
    private final int WINS_REQ_RANK_ENSIGN = 20;
    private final int WINS_REQ_RANK_2ND_LIEUTENANT = 30;
    private final int WINS_REQ_RANK_LIEUTENANT = 45;
    private final int WINS_REQ_RANK_LIEUTENANT_COMMANDER = 70;
    private final int WINS_REQ_RANK_COMMANDER = 100;
    private final int WINS_REQ_RANK_CAPTAIN = 145;
    private final int WINS_REQ_RANK_FLEET_CAPTAIN = 200;
    private final int WINS_REQ_RANK_COMMODORE = 260;
    private final int WINS_REQ_RANK_REAR_ADMIRAL = 330;
    private final int WINS_REQ_RANK_VICE_ADMIRAL = 410;
    private final int WINS_REQ_RANK_ADMIRAL = 500;
    private final int WINS_REQ_RANK_FLEET_ADMIRAL = 1000;
    private final int WINS_REQ_OFFICER = WINS_REQ_RANK_ENSIGN;          // Officer is Ensign+
    private final int WINS_REQ_FLAG_OFFICER = WINS_REQ_RANK_COMMODORE;  // Flag Officer is Commodore+

    // COORDS

    // Spawn, safe, and basewarp coords
    private final int SPAWN_BASE_0_Y_COORD = 456;               // Y coord around which base 0 owners (top) spawn
    private final int SPAWN_BASE_1_Y_COORD = 566;               // Y coord around which base 1 owners (bottom) spawn
    private final int SPAWN_Y_SPREAD = 90;                      // # tiles * 2 from above coords to spawn players
    private final int SPAWN_X_SPREAD = 275;                     // # tiles * 2 from x coord 512 to spawn players
    private final int REARM_AREA_TOP_Y = 199;                   // Y coords of center of rearm areas
    private final int REARM_AREA_BOTTOM_Y = 824;
    private final int REARM_SAFE_TOP_Y = 192;                   // Y coords of safe parts of rearm areas
    private final int REARM_SAFE_BOTTOM_Y = 832;
    private final int BASE_CENTER_0_Y_COORD = 426;
    private final int BASE_CENTER_1_Y_COORD = 597;
    // Old base warp coords (inside base)
    //private final int BASE_CENTER_0_Y_COORD = 335;
    //private final int BASE_CENTER_1_Y_COORD = 688;

    // Wormhole warp points
    private final int WARP_CENTER_0_Y_COORD = 455;
    private final int WARP_CENTER_1_Y_COORD = 568;
    private final int WARP_TO_ROOF_0_Y_COORD = 477;
    private final int WARP_TO_ROOF_1_Y_COORD = 545;
    private final int WARP_FROM_ROOF_0_Y_COORD = 254;
    private final int WARP_FROM_ROOF_1_Y_COORD = 770;
    private final int WARP_LOWER_LEFT_X_COORD = 426;
    private final int WARP_LOWER_RIGHT_X_COORD = 598;
    private final int WARP_LOWER_0_Y_COORD = 377;
    private final int WARP_LOWER_1_Y_COORD = 646;
    private final int WARP_MID_LEFT_X_COORD = 452;
    private final int WARP_MID_RIGHT_X_COORD = 572;
    private final int WARP_MID_0_Y_COORD = 316;
    private final int WARP_MID_1_Y_COORD = 707;

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

    // Coords used for ops nav
    private final int TOP_FR_NAV =   295;
    private final int TOP_MID_NAV =  345;
    private final int TOP_TUBE_NAV = 386;
    private final int BOT_TUBE_NAV = 637;
    private final int BOT_MID_NAV =  678;
    private final int BOT_FR_NAV =   728;

    // Ops !warp coords
    public final int OPS_TOP_WARP1_X = 512; // mid
    public final int OPS_TOP_WARP1_Y = 374;
    public final int OPS_TOP_WARP2_X = 450; // left
    public final int OPS_TOP_WARP2_Y = 314;
    public final int OPS_TOP_WARP3_X = 574; // right
    public final int OPS_TOP_WARP3_Y = 314;
    public final int OPS_TOP_WARP4_X = 512; // before fr
    public final int OPS_TOP_WARP4_Y = 327;
    public final int OPS_TOP_WARP5_X = 512; // roof
    public final int OPS_TOP_WARP5_Y = 265;
    public final int OPS_TOP_WARP6_X = 512; // fr
    public final int OPS_TOP_WARP6_Y = 302;
    public final int OPS_BOT_WARP1_X = 512; // mid
    public final int OPS_BOT_WARP1_Y = 649;
    public final int OPS_BOT_WARP2_X = 450; // left
    public final int OPS_BOT_WARP2_Y = 709;
    public final int OPS_BOT_WARP3_X = 574; // right
    public final int OPS_BOT_WARP3_Y = 709;
    public final int OPS_BOT_WARP4_X = 512; // before fr
    public final int OPS_BOT_WARP4_Y = 696;
    public final int OPS_BOT_WARP5_X = 512; // roof
    public final int OPS_BOT_WARP5_Y = 758;
    public final int OPS_BOT_WARP6_X = 512; // fr
    public final int OPS_BOT_WARP6_Y = 721;

    // 1-flag earwarp
    public final int LEFT_EAR_X = 474;
    public final int LEFT_EAR_Y = 278;
    public final int RIGHT_EAR_X = 550;
    public final int RIGHT_EAR_Y = 278;




    // MEMBERS

    private String m_database;                              // DB to connect to
    private BotSettings m_botSettings;
    private CommandInterpreter m_commandInterpreter;


    private Vector <ShipProfile>m_shipGeneralData;          // Generic (nonspecific) purchasing data for ships.  Uses 1-8 for #
    private Map <String,DistensionPlayer>m_players;         // In-game data on players (Name -> Player)
    private HashMap <Integer,DistensionArmy>m_armies;       // In-game data on armies  (ID -> Army)

    private PrizeQueue m_prizeQueue;                        // Queuing system for prizes (so as not to crash bot)
    private SpecialAbilityTask m_specialAbilityPrizer;      // Prizer for special abilities (run once every 30s)
    private TimerTask m_entranceWaitTask;                   // For when bot first enters the arena
    private TimerTask m_autoSaveTask;                       // For autosaving player data frequently
    private TimerTask m_idleSpecTask;                       // For docking idlers
    private TimerTask m_profitSharingTask;                  // For sharing profits with terrs
    private TimerTask m_scrapClearTask;                     // Clears remembered scraps
    private TimerTask m_warpPointTask;                      // Checks for special warp points

    private boolean m_beginDelayedShutdown;                 // True if, at round end, a shutdown should be initiated
    private long m_shutdownTimeMillis;                      // Time at which to shutdown, in ms
    private boolean m_readyForPlay = false;                 // True if bot has entered arena and is ready to go
    private boolean m_roundGettingStarted = false;          // True if round is being started (all players in rearm areas)
    private int[] m_flagOwner = {-1, -1};                   // ArmyIDs of flag owners; -1 for none
    private List <String>m_mineClearedPlayers;              // Players who have already cleared mines this battle
    private Map <String,Integer>m_scrappingPlayers;         // Players who have scrapped recently, and what they scrapped
    private LinkedList <String>m_msgBetaPlayers;            // Players to send beta msg to
    private HashMap <String,Integer>m_defectors;            // Players wishing to defect who need to confirm defection
    private long m_lastSave;                                // Last time data was saved

    // LIMITING SYSTEM
    private final int MAX_PLAYERS = 40;                     // Max # players allowed in game
    private TimerTask timeIncrementTask;                    // Task firing every minute that increments time playing
    private LinkedList <DistensionPlayer>m_waitingToEnter;  // List of players waiting to enter the game

    // ASSIST SYSTEM
    private final float ADVERT_WEIGHT_IMBALANCE = 0.85f;    // At what point to advert that there's an imbalance
    private final float ASSIST_WEIGHT_IMBALANCE = 0.89f;    // At what point an army is considered imbalanced
    private final int ASSIST_NUMBERS_IMBALANCE = 3;         // # of pilot difference before considered imbalanced
    private final int ASSIST_REWARD_TIME = 1000 * 60 * 1;   // Time between adverting and rewarding assists (1 min def.)
    private final int TERRSHARK_REWARD_TIME = 1000 * 60 * 3;// Time between rewarding new terrs/sharks (3 min def.)
    private long lastAssistReward;                          // Last time assister was given points
    private long lastAssistAdvert;                          // Last time an advert was sent for assistance
    private long lastTerrSharkReward;                       // Last time a new terr or shark was given a bonus
    private boolean checkForAssistAdvert = false;           // True if armies may be unbalanced, requiring !assist advert
    private TimerTask assistAdvertTask;                     // Task used to advert for assisting the other army

    // SPECIAL ABILITY PRIZE #s
    public final int ABILITY_PRIORITY_REARM = -1;
    public final int ABILITY_TERR_REGEN = -2;
    public final int ABILITY_ENERGY_TANK = -3;
    public final int ABILITY_TARGETED_EMP = -4;
    public final int ABILITY_SUPER = -5;
    public final int ABILITY_SHARK_REGEN = -6;
    public final int ABILITY_PROFIT_SHARING = -7;
    public final int ABILITY_VENGEFUL_BASTARD = -8;
    public final int ABILITY_ESCAPE_POD = -9;
    public final int ABILITY_LEECHING = -10;
    public final int ABILITY_JUMPSPACE = -11;
    public final int ABILITY_THOR = -12;
    public final int ABILITY_MASTER_DRIVE = -13;
    public final int ABILITY_PRISMATIC_ARRAY = -14;
    public final int ABILITY_FIREBLOOM = -15;
    public final int ABILITY_SUMMONING_AUTH = -16;


    // TACTICAL OPS DATA
    public final int DEFAULT_MAX_OP = 2;                    // Max OP points when not upgraded
    public final int DEFAULT_OP_REGEN = 1;                  // Default # OP regenerated every minute
    public final int DEFAULT_OP_MAX_COMMS = 3;              // Max # communications Ops can save up
    public boolean m_army0_fastRearm = false;
    public boolean m_army1_fastRearm = false;
    public RearmTask m_army0_fastRearmTask;
    public RearmTask m_army1_fastRearmTask;
    public TimerTask m_doorOffTask;

    // TACTICAL OPS ABILITY PRIZE #s
    public final int OPS_INCREASE_MAX_OP = -21;
    public final int OPS_REGEN_RATE = -22;
    public final int OPS_COMMUNICATIONS = -23;
    public final int OPS_WARP = -24;
    public final int OPS_FAST_TEAM_REARM = -25;
    public final int OPS_COVER = -26;
    public final int OPS_DOOR_CONTROL = -27;
    public final int OPS_SECLUSION = -28;
    public final int OPS_MINEFIELD = -29;
    public final int OPS_SHROUD = -30;
    public final int OPS_FLASH = -31;
    public final int OPS_TEAM_SHIELDS = -32;



    // DATA FOR FLAG TIMER
    private static final int SCORE_REQUIRED_FOR_WIN = 3;// Max # rounds (odd numbers only)
    private static final int SECTOR_CHANGE_SECONDS = 4; // Seconds it takes to secure hold or break one
    private static final int INTERMISSION_SECS = 60;    // Seconds between end of free play & start of next battle
    private static final int PLAYERS_FOR_2_FLAGS = 25;  // Minimum # players required to activate 2 flags\
    private static final int SUDDEN_DEATH_MINUTES = 35; // Minutes after which round ends with a truce
    private boolean flagTimeStarted;                    // True if flag time is enabled
    private boolean stopFlagTime;                       // True if flag time will stop at round end
    private boolean m_singleFlagMode;                   // True if flag mode is working on just a single flag

    private int m_flagRules = 0;                        // 0: Use original rules (pub-style timer)
                                                        // 1: Use hybrid rules (tug-a-war)
    private int m_freq0Score, m_freq1Score;             // # rounds won
    private int m_roundNum = 0;							// Current round
    private int flagSecondsRequiredDoubleFlag = 60;     // Flag seconds required to win
    private int flagSecondsRequiredSingleFlag = 120;    // Flag seconds required to win with 1 flag
    private float flagSecondsHybridFactor = 2.5f;       // Factor by which flag seconds required to win
                                                        //    increase when using hybrid mode
    private int flagHybridReversionRate = 3;            // How many seconds per second held by a team presently
                                                        //    not holding advantage are taken off the clock
    private HashMap <String,Integer>m_playerTimes;      // Roundtime of player on freq
    private HashMap <String,Integer>m_lagouts;          // Players who have potentially lagged out, + time they lagged out
    //private HashMap <String,Integer>m_lagShips;         // Ships of players who were DC'd, for !lagout use

    private FlagCountTask flagTimer;                    // Flag time main class
    private StartRoundTask startTimer;                  // TimerTask to start round
    private IntermissionTask intermissionTimer;         // TimerTask for round intermission
    private FreePlayTask freePlayTimer;                 // TimerTask for delaying announcing free play
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
    private final int LVZ_EMP = 206;                    // EMP ready graphic
    private final int LVZ_ENERGY_TANK = 207;            // Energy Tank ready graphic
    private final int LVZ_JUMPSPACE = 209;              // JumpSpace ready graphic
    private final int LVZ_SUPER = 208;                  // Super!
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

    private final int LVZ_OPS_SPHERE = 300;             // OPS special ability LVZ
    private final int LVZ_OPS_BLIND1 = 301;
    private final int LVZ_OPS_BLIND2 = 302;
    private final int LVZ_OPS_BLIND3 = 303;
    private final int LVZ_OPS_SHROUD_SM = 304;
    private final int LVZ_OPS_SHROUD_LG = 305;
    private final int LVZ_OPS_FAST_REARM = 306;
    private final int LVZ_OPS_COVER_TOP_FIRST = 320;
    private final int LVZ_OPS_COVER_BOT_FIRST = 330;

    // SOUND DEFINES
    private final int SOUND_START_ROUND = 255;
    private final int SOUND_END_IS_NIGH = 254;



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
        //m_lagShips = new HashMap<String,Integer>();
        m_mineClearedPlayers = Collections.synchronizedList( new LinkedList<String>() );
        m_scrappingPlayers = Collections.synchronizedMap( new HashMap<String,Integer>() );
        m_msgBetaPlayers = new LinkedList<String>();
        m_defectors = new HashMap<String,Integer>();
        m_waitingToEnter = new LinkedList<DistensionPlayer>();
        flagTimerObjs = m_botAction.getObjectSet();
        flagObjs = new Objset();
        playerObjs = new Objset();
        setupPrices();
        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fnArmyID FROM tblDistensionArmy" );
            if( r == null ) {
                Tools.printLog( "Null ResultSet returned for query: 'SELECT fnArmyID FROM tblDistensionArmy' on connection '" + m_database + "'" );
                cmdDie("DistensionInternal", "now");
            } else {
                while( r.next() ) {
                    int id = r.getInt( "fnArmyID");
                    DistensionArmy army = new DistensionArmy(id);
                    m_armies.put(id, army );
                }
                m_botAction.SQLClose(r);
            }
        } catch (SQLException e) {
            Tools.printStackTrace( "Error retrieving army data on startup!", e );
            cmdDie("DistensionInternal", "now");
        }
        flagTimeStarted = false;
        stopFlagTime = false;
        m_singleFlagMode = true;
        m_beginDelayedShutdown = false;
        m_shutdownTimeMillis = 0;
        if( m_botSettings.getInt("Debug") == 1 )
            DEBUG = true;
        else
            DEBUG = false;
        m_lastSave = System.currentTimeMillis();
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
        req.request(EventRequester.FREQUENCY_CHANGE);
        req.request(EventRequester.TURRET_EVENT);
    }


    /**
     * Performs startup tasks once the bot has entered the arena.
     */
    public void init() {
        m_botAction.sendUnfilteredPublicMessage("?chat=distension" );
        m_botAction.setMessageLimit( 12, false );
        m_botAction.setReliableKills( 1 );
        m_botAction.setPlayerPositionUpdating( 400 );
        m_botAction.setLowPriorityPacketCap( 23 );
        m_botAction.specAll();
        m_botAction.resetFlagGame();
        m_botAction.setDoors( 240 ); // All bottom doors closed

        if( DEBUG ) {
            //m_botAction.sendUnfilteredPublicMessage("?find dugwyler" );
            if( m_botSettings.getInt("DisplayLoadedMsg") == 1 ) {
                m_botAction.sendChatMessage("Distension BETA initialized.  ?go #distension");
                m_botAction.sendArenaMessage("Distension BETA loaded.  Enter into a ship to start playing (1 and 5 are starting ships).  Please see the beta thread on the forums for bug reports & suggestions.");
            }
            // Reset all times at each load
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fnTime='0' WHERE 1" );
            } catch (SQLException e ) {  }
            m_botAction.scoreResetAll();
            String sendmsg = m_botSettings.getString("InitialMsg");
            if( sendmsg != null && !sendmsg.equals("")) {
                String msgs[] = sendmsg.split(";");
                for( int i = 0; i<msgs.length; i++ )
                    m_botAction.sendUnfilteredPublicMessage( msgs[i] );
            }
        }
        setupTimerTasks();
    }

    /**
     * Set up and schedule all regular repeating TimerTasks.
     */
    public void setupTimerTasks() {
        m_specialAbilityPrizer = new SpecialAbilityTask();
        m_botAction.scheduleTaskAtFixedRate(m_specialAbilityPrizer, 30000, 30000 );

        m_prizeQueue = new PrizeQueue();
        m_botAction.scheduleTaskAtFixedRate(m_prizeQueue, UPGRADE_DELAY, UPGRADE_DELAY);

        m_entranceWaitTask = new TimerTask() {
            public void run() {
                m_readyForPlay = true;
            }
        };
        m_botAction.scheduleTask( m_entranceWaitTask, 3000 );

        if( AUTOSAVE_DELAY != 0 ) {
            m_autoSaveTask = new TimerTask() {
                public void run() {
                    cmdSaveData( ":autosave:", null );
                }
            };
            m_botAction.scheduleTask( m_autoSaveTask, AUTOSAVE_DELAY * 60000, AUTOSAVE_DELAY * 60000 );
        }

        if( IDLE_FREQUENCY_CHECK != 0 ) {
            m_idleSpecTask = new TimerTask() {
                public void run() {
                    // Check for idlers, but only while round is going.
                    if( flagTimer != null && flagTimer.isRunning() )
                        for( DistensionPlayer p : m_players.values() )
                            p.checkIdleStatus();
                }
            };
            m_botAction.scheduleTask( m_idleSpecTask, IDLE_FREQUENCY_CHECK * 1000, IDLE_FREQUENCY_CHECK * 1000);
        }

        timeIncrementTask = new TimerTask() {
            public void run() {
                for( DistensionPlayer p : m_players.values() )
                    p.incrementTime();
            }
        };
        m_botAction.scheduleTask( timeIncrementTask, 60000, 60000 );

        m_profitSharingTask = new TimerTask() {
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
        m_botAction.scheduleTask( m_profitSharingTask, PROFIT_SHARING_FREQUENCY * 1000, PROFIT_SHARING_FREQUENCY * 1000 );

        m_scrapClearTask = new TimerTask() {
            public void run() {
                m_scrappingPlayers.clear();
            }
        };
        m_botAction.scheduleTask( m_scrapClearTask, SCRAP_CLEARING_FREQUENCY * 1000, SCRAP_CLEARING_FREQUENCY * 1000 );

        // Check various warp points, and warp those in them
        m_warpPointTask = new TimerTask() {
            public void run() {
                for( Player p : m_botAction.getPlayingPlayers() ) {
                    // Warps from and to: center blocks; center bar to roof
                    if( p.getXTileLocation() >= 511 && p.getXTileLocation() <= 513 ) {
                        if( p.getYTileLocation() >= WARP_CENTER_0_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_CENTER_0_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), 512, WARP_CENTER_1_Y_COORD );
                        } else if( p.getYTileLocation() >= WARP_CENTER_1_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_CENTER_1_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), 512, WARP_CENTER_0_Y_COORD );
                        } else if( p.getYTileLocation() >= WARP_TO_ROOF_0_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_TO_ROOF_0_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), 512, WARP_FROM_ROOF_0_Y_COORD );
                        } else if( p.getYTileLocation() >= WARP_TO_ROOF_1_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_TO_ROOF_1_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), 512, WARP_FROM_ROOF_1_Y_COORD );
                        } else if( p.getYTileLocation() >= WARP_FROM_ROOF_0_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_FROM_ROOF_0_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), 512, WARP_TO_ROOF_0_Y_COORD );
                        } else if( p.getYTileLocation() >= WARP_FROM_ROOF_1_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_FROM_ROOF_1_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), 512, WARP_TO_ROOF_1_Y_COORD );
                        }
                    // Warps from and to: lower shoulders to mid shoulders
                    } else if( p.getXTileLocation() >= WARP_LOWER_LEFT_X_COORD - 1
                            && p.getXTileLocation() <= WARP_LOWER_LEFT_X_COORD + 1 ) {
                        if( p.getYTileLocation() >= WARP_LOWER_0_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_LOWER_0_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), WARP_MID_LEFT_X_COORD, WARP_MID_1_Y_COORD );
                        } else if( p.getYTileLocation() >= WARP_LOWER_1_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_LOWER_1_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), WARP_MID_LEFT_X_COORD, WARP_MID_0_Y_COORD );
                        }
                    } else if( p.getXTileLocation() >= WARP_LOWER_RIGHT_X_COORD - 1
                            && p.getXTileLocation() <= WARP_LOWER_RIGHT_X_COORD + 1 ) {
                        if( p.getYTileLocation() >= WARP_LOWER_0_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_LOWER_0_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), WARP_MID_RIGHT_X_COORD, WARP_MID_1_Y_COORD );
                        } else if( p.getYTileLocation() >= WARP_LOWER_1_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_LOWER_1_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), WARP_MID_RIGHT_X_COORD, WARP_MID_0_Y_COORD );
                        }
                    } else if( p.getXTileLocation() >= WARP_MID_LEFT_X_COORD - 1
                            && p.getXTileLocation() <= WARP_MID_LEFT_X_COORD + 1 ) {
                        if( p.getYTileLocation() >= WARP_MID_0_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_MID_0_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), WARP_LOWER_LEFT_X_COORD, WARP_LOWER_1_Y_COORD );
                        } else if( p.getYTileLocation() >= WARP_MID_1_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_MID_1_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), WARP_LOWER_LEFT_X_COORD, WARP_LOWER_0_Y_COORD );
                        }
                    } else if( p.getXTileLocation() >= WARP_MID_RIGHT_X_COORD - 1
                            && p.getXTileLocation() <= WARP_MID_RIGHT_X_COORD + 1 ) {
                        if( p.getYTileLocation() >= WARP_MID_0_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_MID_0_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), WARP_LOWER_RIGHT_X_COORD, WARP_LOWER_1_Y_COORD );
                        } else if( p.getYTileLocation() >= WARP_MID_1_Y_COORD - 1 &&
                                p.getYTileLocation() <= WARP_MID_1_Y_COORD + 1 ) {
                            m_botAction.warpTo(p.getPlayerID(), WARP_LOWER_RIGHT_X_COORD, WARP_LOWER_0_Y_COORD );
                        }
                    }
                }
            }
        };
        m_botAction.scheduleTask( m_warpPointTask, WARP_POINT_CHECK_FREQUENCY * 1000, WARP_POINT_CHECK_FREQUENCY * 1000 );

        // Do not advert/reward for rectifying imbalance in the first 1 min of a game
        lastAssistReward = System.currentTimeMillis();
        lastAssistAdvert = System.currentTimeMillis();
        lastTerrSharkReward = System.currentTimeMillis();
        assistAdvertTask = new TimerTask() {
            public void run() {
                if( !checkForAssistAdvert )
                    return;
                DistensionArmy army0 = m_armies.get(0);
                DistensionArmy army1 = m_armies.get(1);
                int army0Terrs = 0;
                int army1Terrs = 0;
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
                                try {
                                    cmdAssist( p.getName(), ":auto:" );
                                    helped = true;
                                } catch (TWCoreException e ) {}
                            }
                        }
                    if( !helped ) {
                        if( maxStrToAssist > RANK_0_STRENGTH )  // Only display if assisting is possible
                            m_botAction.sendOpposingTeamMessageByFrequency( msgArmy, "IMBALANCE: Pilot lower rank ships, or use !assist " + helpOutArmy + "  (max assist rank: " + (maxStrToAssist - RANK_0_STRENGTH) + ").   [ " + army0.getTotalStrength() + " vs " + army1.getTotalStrength() + " ]");
                    }
                    // Check if teams are imbalanced in numbers, if not strength
                } else {
                    if( army0.getPilotsInGame() <= army1.getPilotsInGame() - ASSIST_NUMBERS_IMBALANCE )
                        m_botAction.sendOpposingTeamMessageByFrequency( 0, "NOTICE: Your army has fewer pilots but is close in strength; if you need help, pilot lower-ranked ships to allow !assist." );
                    else if( army1.getPilotsInGame() <= army0.getPilotsInGame() - ASSIST_NUMBERS_IMBALANCE )
                        m_botAction.sendOpposingTeamMessageByFrequency( 1, "NOTICE: Your army has fewer pilots but is close in strength; if you need help, pilot lower-ranked ships to allow !assist." );
                }

                if( System.currentTimeMillis() > lastTerrSharkReward + TERRSHARK_REWARD_TIME ) {
                    for( DistensionPlayer p : m_players.values() ) {
                        if( p.getShipNum() == Tools.Ship.TERRIER )
                            if( p.getArmyID() == 0 )
                                army0Terrs++;
                            else
                                army1Terrs++;
                    }
                    if( army0Terrs < 2 ) {
                        if( army0Terrs == 1 && army0.getPilotsInGame() > 9 ) {
                            m_botAction.sendOpposingTeamMessageByFrequency( 0, "TERR NEEDED: One additional Terrier pilot requested by HQ.  Switch ships to receive a bonus." );
                        } else if( army0Terrs == 0 && army0.getPilotsInGame() > 3 ) {
                            m_botAction.sendOpposingTeamMessageByFrequency( 0, "TERR NEEDED!: Terrier pilot urgently requested by HQ.  Switch ships to receive a bonus." );
                        }
                        if( army1Terrs == 1 && army1.getPilotsInGame() > 9 ) {
                            m_botAction.sendOpposingTeamMessageByFrequency( 1, "TERR NEEDED: One additional Terrier pilot requested by HQ.  Switch ships to receive a bonus." );
                        } else if( army1Terrs == 0 && army1.getPilotsInGame() > 3 ) {
                            m_botAction.sendOpposingTeamMessageByFrequency( 1, "TERR NEEDED!: Terrier pilot urgently requested by HQ.  Switch ships to receive a bonus." );
                        }
                    }
                }

                lastAssistAdvert = System.currentTimeMillis();
                checkForAssistAdvert = false;
            }
        };
        m_botAction.scheduleTask( assistAdvertTask, 20000, 20000 );
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
        m_commandInterpreter.registerCommand( "!r", acceptedMessages, this, "cmdReturn" );
        m_commandInterpreter.registerCommand( "!p", acceptedMessages, this, "cmdProgress" );
        m_commandInterpreter.registerCommand( "!s", acceptedMessages, this, "cmdStatus" );
        m_commandInterpreter.registerCommand( "!sc", acceptedMessages, this, "cmdScrap" );
        m_commandInterpreter.registerCommand( "!sca", acceptedMessages, this, "cmdScrapAll" );
        m_commandInterpreter.registerCommand( "!su", acceptedMessages, this, "cmdSummon" );
        m_commandInterpreter.registerCommand( "!t", acceptedMessages, this, "cmdTerr" );
        m_commandInterpreter.registerCommand( "!tm", acceptedMessages, this, "cmdTeam" );
        m_commandInterpreter.registerCommand( "!u", acceptedMessages, this, "cmdUpgrade" );
        m_commandInterpreter.registerCommand( "!upg", acceptedMessages, this, "cmdUpgrade" );
        m_commandInterpreter.registerCommand( "!ui", acceptedMessages, this, "cmdUpgInfo" );
        m_commandInterpreter.registerCommand( "!w", acceptedMessages, this, "cmdWarp" );
        m_commandInterpreter.registerCommand( "!wh", acceptedMessages, this, "cmdWhereIs" );
        m_commandInterpreter.registerCommand( "!mo", acceptedMessages, this, "cmdManOps" );
        // Ops shortcuts
        m_commandInterpreter.registerCommand( ".h", acceptedMessages, this, "cmdOpsHelp" );
        m_commandInterpreter.registerCommand( "..", acceptedMessages, this, "cmdOpsStatus" );
        m_commandInterpreter.registerCommand( ".m", acceptedMessages, this, "cmdOpsMsg" );
        m_commandInterpreter.registerCommand( ".pm", acceptedMessages, this, "cmdOpsPM" );
        m_commandInterpreter.registerCommand( ".sm", acceptedMessages, this, "cmdOpsSab" );
        m_commandInterpreter.registerCommand( ".r", acceptedMessages, this, "cmdOpsRadar" );
        m_commandInterpreter.registerCommand( ".re", acceptedMessages, this, "cmdOpsRearm" );
        m_commandInterpreter.registerCommand( ".d", acceptedMessages, this, "cmdOpsDoor" );
        m_commandInterpreter.registerCommand( ".c", acceptedMessages, this, "cmdOpsCover" );
        m_commandInterpreter.registerCommand( ".w", acceptedMessages, this, "cmdOpsWarp" );
        m_commandInterpreter.registerCommand( ".o", acceptedMessages, this, "cmdOpsOrb" );
        m_commandInterpreter.registerCommand( ".da", acceptedMessages, this, "cmdOpsDark" );
        m_commandInterpreter.registerCommand( ".b", acceptedMessages, this, "cmdOpsBlind" );
        m_commandInterpreter.registerCommand( ".s", acceptedMessages, this, "cmdOpsShield" );
        m_commandInterpreter.registerCommand( ".e", acceptedMessages, this, "cmdOpsEMP" );
        m_commandInterpreter.registerCommand( ",1", acceptedMessages, this, "cmdOpsNav1" );
        m_commandInterpreter.registerCommand( ",11", acceptedMessages, this, "cmdOpsNav11" );
        m_commandInterpreter.registerCommand( ",12", acceptedMessages, this, "cmdOpsNav12" );
        m_commandInterpreter.registerCommand( ",13", acceptedMessages, this, "cmdOpsNav13" );
        m_commandInterpreter.registerCommand( ",2", acceptedMessages, this, "cmdOpsNav2" );
        m_commandInterpreter.registerCommand( ",21", acceptedMessages, this, "cmdOpsNav21" );
        m_commandInterpreter.registerCommand( ",22", acceptedMessages, this, "cmdOpsNav22" );
        m_commandInterpreter.registerCommand( ",3", acceptedMessages, this, "cmdOpsNav3" );

        // Full trigger versions
        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "cmdHelp" );
        m_commandInterpreter.registerCommand( "!modhelp", acceptedMessages, this, "cmdModHelp" );
        m_commandInterpreter.registerCommand( "!enlist", acceptedMessages, this, "cmdEnlist" );
        m_commandInterpreter.registerCommand( "!defect", acceptedMessages, this, "cmdDefect" );
        m_commandInterpreter.registerCommand( "!return", acceptedMessages, this, "cmdReturn" );
        m_commandInterpreter.registerCommand( "!leave", acceptedMessages, this, "cmdLeave" );
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
        m_commandInterpreter.registerCommand( "!summon", acceptedMessages, this, "cmdSummon" );
        m_commandInterpreter.registerCommand( "!!", acceptedMessages, this, "cmdEnergyTank" );
        m_commandInterpreter.registerCommand( "!emp", acceptedMessages, this, "cmdTargetedEMP" );
        m_commandInterpreter.registerCommand( ">>>", acceptedMessages, this, "cmdJumpSpace" );
        m_commandInterpreter.registerCommand( "---", acceptedMessages, this, "cmdPrismaticArray" );
        m_commandInterpreter.registerCommand( "!manops", acceptedMessages, this, "cmdManOps" );
        m_commandInterpreter.registerCommand( "!pilot", acceptedMessages, this, "cmdPilotDefunct" );
        m_commandInterpreter.registerCommand( "!ship", acceptedMessages, this, "cmdPilotDefunct" );
        m_commandInterpreter.registerCommand( "!opshelp", acceptedMessages, this, "cmdOpsHelp" );
        m_commandInterpreter.registerCommand( "!opsstatus", acceptedMessages, this, "cmdOpsStatus" );
        m_commandInterpreter.registerCommand( "!opsmsg", acceptedMessages, this, "cmdOpsMsg" );
        m_commandInterpreter.registerCommand( "!opspm", acceptedMessages, this, "cmdOpsPM" );
        m_commandInterpreter.registerCommand( "!opssab", acceptedMessages, this, "cmdOpsSab" );
        m_commandInterpreter.registerCommand( "!opsradar", acceptedMessages, this, "cmdOpsRadar" );
        m_commandInterpreter.registerCommand( "!opsrearm", acceptedMessages, this, "cmdOpsRearm" );
        m_commandInterpreter.registerCommand( "!opsdoor", acceptedMessages, this, "cmdOpsDoor" );
        m_commandInterpreter.registerCommand( "!opscover", acceptedMessages, this, "cmdOpsCover" );
        m_commandInterpreter.registerCommand( "!opswarp", acceptedMessages, this, "cmdOpsWarp" );
        m_commandInterpreter.registerCommand( "!opsorb", acceptedMessages, this, "cmdOpsOrb" );
        m_commandInterpreter.registerCommand( "!opsdark", acceptedMessages, this, "cmdOpsDark" );
        m_commandInterpreter.registerCommand( "!opsblind", acceptedMessages, this, "cmdOpsBlind" );
        m_commandInterpreter.registerCommand( "!opsshield", acceptedMessages, this, "cmdOpsShield" );
        m_commandInterpreter.registerCommand( "!opsemp", acceptedMessages, this, "cmdOpsEMP" );
        m_commandInterpreter.registerCommand( "!beta", acceptedMessages, this, "cmdBeta" );  // BETA CMD
        m_commandInterpreter.registerCommand( "!msgbeta", acceptedMessages, this, "cmdMsgBeta", OperatorList.HIGHMOD_LEVEL ); // BETA CMD
        m_commandInterpreter.registerCommand( "!grant", acceptedMessages, this, "cmdGrant", OperatorList.HIGHMOD_LEVEL );     // BETA CMD
        m_commandInterpreter.registerCommand( "!info", acceptedMessages, this, "cmdInfo", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!ban", acceptedMessages, this, "cmdBan", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!unban", acceptedMessages, this, "cmdUnban", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!savedata", acceptedMessages, this, "cmdSaveData", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!diewithoutsave", acceptedMessages, this, "cmdDie", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!savedie", acceptedMessages, this, "cmdSaveDie", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!shutdown", acceptedMessages, this, "cmdShutdown", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!shutdowninfo", acceptedMessages, this, "cmdShutdownInfo", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!db-changename", acceptedMessages, this, "cmdDBChangeName", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!db-addship", acceptedMessages, this, "cmdDBWipeShip", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!db-wipeship", acceptedMessages, this, "cmdDBWipeShip", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!db-wipeplayer", acceptedMessages, this, "cmdDBWipePlayer", OperatorList.HIGHMOD_LEVEL );  // Not published in !help
        m_commandInterpreter.registerCommand( "!db-randomarmies", acceptedMessages, this, "cmdDBRandomArmies", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!debug-getint", acceptedMessages, this, "cmdGetInt", OperatorList.DEV_LEVEL );
        m_commandInterpreter.registerCommand( "!debug-getbool", acceptedMessages, this, "cmdGetBool", OperatorList.DEV_LEVEL );
        m_commandInterpreter.registerCommand( "!debug-setvar", acceptedMessages, this, "cmdSetVar", OperatorList.DEV_LEVEL );

        m_commandInterpreter.registerDefaultCommand( Message.REMOTE_PRIVATE_MESSAGE, this, "handleRemoteMessage" );
        m_commandInterpreter.registerDefaultCommand( Message.ARENA_MESSAGE, this, "handleArenaMessage" );
        m_commandInterpreter.registerDefaultCommand( Message.PUBLIC_MESSAGE, this, "handlePublicMessage" );
    }


    /**
     * Catches unlocking of arena and re-locks.
     * @param name
     * @param message
     */
    public void handleArenaMessage( String name, String message ) {
        // Beta should not be loaded w/o me; ?find dugwyler done at start to ensure this.
        if( DEBUG )
            if( message.startsWith("Not online, last seen") ) {
                m_botAction.sendUnfilteredPublicMessage("?message dugwyler:Unauthorized load of Distension BETA." );
                Tools.printLog("Unauthorized load of Distension BETA.");
                m_botAction.sendArenaMessage("Unauthorized load of Distension BETA; shutting down.");
                m_botAction.sendChatMessage("Unauthorized load of Distension BETA; shutting down.");
                cmdDie(m_botAction.getBotName(), "now");
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
     * Reset away timer whenever player speaks.
     * @param name
     * @param msg
     */
    public void handlePublicMessage( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p != null )
            p.resetIdle();
    }


    // ***** HELPS (AND VARIOUS OTHER INFORMATION-GATHERING COMMANDS) *****

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

        if( msg.equals("2") ) {
            String[] helps = {
            ".-----------------------",
            "| !intro                |  An introduction to Distension.",
            "| !warp             !w  |  Toggle waiting in spawn vs. being autowarped out",
            "| !basewarp         !bw |  Toggle warping into base vs. spawn at round start",
            "| !killmsg          !k  |  Toggle kill messages on and off (+1% RP for off)",
            "| !team             !tm |  Show all players on team and their upg. levels",
            "| !terr             !t  |  Show approximate location of all army terriers",
            "| !whereis <name>   !wh |  Show approximate location of pilot <name>",
            "| !armies           !ar |  View size and strength of armies",
            "| !battleinfo       !bi |  Display current battle status",
            "| !clearmines       !cm |  Clear all mines, if in a mine-laying ship",
            "| !summon (Terr)    !su |  Summons others to you.  !summon s for sharks.",
            "| !summon (others)  !su |  Toggles allowing summoning by Terriers",
            "|______________________/"
            };
            m_botAction.privateMessageSpam(p.getArenaPlayerID(), helps);
            return;
        }

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
            m_botAction.privateMessageSpam(p.getArenaPlayerID(), helps);
        } else if( shipNum == 0 ) {
            String[] helps = {
                    "     HANGAR CONSOLE",
                    ".-----------------------",
                    "| !hangar           !h  |  View your ships & those available for purchase",
                    "| !leave            !l  |  Leave the battle, opening your position",
                    "| !lagout           !la |  Return to last ship, maintaining participation",
                    "| !assist <army>    !as |  Temporarily assist <army> at no penalty to you",
                    "| !defect <army>    !d  |  Change to <army>.  All ships LOSE A FULL RANK,",
                    "|                       |     unless the new army has 4+ fewer pilots.",
                    "| !manops           !mo |  Mans the Tactical Ops console, if you have clearance",
                    "|______________________/",
                    "               -=(  Use  !help 2  for additional commands  )=-"
            };
            m_botAction.privateMessageSpam(p.getArenaPlayerID(), helps);
        } else {
            String[] helps = {
                    ".-----------------------",
                    "| !progress         .   |  See your progress toward next advancement",
                    "| !status           !s  |  View current ship's level and upgrades",
                    "| !armory           !a  |  View ship upgrades available in the armory",
                    "| !upgrade <upg>    !u  |  Upgrade your ship with <upg> from the armory",
                    "| !upginfo <upg>    !ui |  Shows any available information about <upg>",
                    "| !upginfo <upg>:<ship> |  Shows info on <upg> for <ship>, if you own it.",
                    "| !scrap <upg>      !sc |  Trade in <upg>.  *** Restarts ship at current rank!",
                    "| !scrapall <#/S/M/E/*> |  Scrap all of ...   *: ALL upg        #: Given upg",
                    "|                   !sca|    S: Special upgs  M: Maneuver upgs  E: En/chg upgs",
                    "| !hangar           !h  |  View your ships & those available for purchase",
                    "| !dock             !d  |  Dock your ship, saving all progress",
                    "| !leave            !l  |  Leave the battle, opening your position",
                    "| !assist <army>    !as |  Temporarily assists <army> at no penalty to you",
                    "| !manops           !mo |  Mans the Tactical Ops console, if you have clearance",
                    "|______________________/",
                    "               -=(  Use  !help 2  for additional commands  )=-",
            };
            m_botAction.privateMessageSpam(p.getArenaPlayerID(), helps);
        }

        if( shipNum == 9 ) {
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "            -=(  Use  !opshelp (.h) for Tactical Ops commands  )=-" );
            p.resetIdle();
        }

        if( m_botAction.getOperatorList().isHighmod(name) )
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "              -=(  Use  !modhelp (!mh) for Moderator commands  )=-" );

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
                "| !shutdowninfo         |  Shows time until shutdown",
                "| !savedata             |  Saves all player data to database",
                "| !savedie              |  Saves all player data and runs a delayed !die",
                "| !diewithoutsave       |  Kills DistensionBot -- use !savedie instead!",
                "|______________________/",
                "    DB COMMANDS",
                "  !db-changename <oldname>:<newname>   - Changes name from oldname to newname.",
                "  !db-addship <name>:<ship#>           - Adds ship# to name's record.",
                "  !db-wipeship <name>:<ship#>          - Wipes ship# from name's record.",
                "  !db-randomarmies                     - Randomizes all armies."
        };
        m_botAction.privateMessageSpam(p.getArenaPlayerID(), helps);
        if( m_botAction.getOperatorList().isSmod(name) ) {
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "Hidden command: !db-wipeplayer <name>  - Wipes all records of a player from DB.");
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

        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You are not at a Tactical Ops console, and have no way to refer to the Ops manual.");
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub

        if( msg.equals("") ) {
            String[] helps = {
                    ".--------------------------",
                    "|OBSERVATION        Cost   | Snaps view to a location based on the number.",
                    "| ,#                  0    |  1: HomeFR  11: HomeMid  12: HomeTube  13:HomeRearm",
                    "|                          |  2: NME FR  21: NME Mid  22: NME Tube   3: Center",
                    "|COMMUNICATIONS            |",
                    "| !opsradar           1 .r |  Shows approx. location of all pilots, + Terr info",
                    "| !opsmsg <#>         1 .m |  Msg army.  See !opshelp msg (!oh msg) for avail. msgs",
                    "| !opsPM <nm>:<#>     1 .pm|  Msg specific players.  See !opshelp msg (or .h msg)",
                    "| !opssab             2 .sm|  Sabotage msg to enemy.  See !opshelp msg (or .h msg)",
                    "|ACTIONS                   |",
                    "| !opsrearm           2 .re|  Fast rearm/slow enemy rearm for 20 seconds/level",
                    "| !opsdoor <#>    4/6/8 .d |  Close doors.  1:Sides  2:Tube   (L2) 3:FR  4:Flag",
                    "|                          |    Enemy doors:(L3)  5:Sides  6:Tube  7:FR  8:Flag",
                    "| !opswarp <nm>:<#>     .w |  Warps <name> to...  1:Tube   2:LeftMid  3:RightMid",
                    "|                 2/4/10   |                  (L2)4:FR Ent 5:Roof (L3)6:FR",
                    "| !opscover <#>       1 .c |  Deploy cover in home base.   1:MidLeft  2:MidRight",
                    "|                          |       3:Before FR    4:Flag   5:Tube     6:Entrance",
// TODO: Implement in LVZ.  Pain in the ass.
//                    "| !opsmine <#>        2 .mi|  False minefield @ home base. 1:FR Entrance  2:In FR",
//                    "|                          |       3:Midbase   4:TubeTop   5:Inside/Mid Tube",
                    "| !opsorb <nm>      2/6 .o |  Cover enemy w/ orb.  (L2)<all> = All NME in base",
                    "| !opsdark <nm>     3/8 .da|  Cone of darkness.    (L2)<all> = All NME in base",
                    "|                          |      L3: Larger cone.  L4: Larger cone, all NME.",
                    "| !opsblind <#>  7/9/11 .b |  Blind all NME in base.  <#> specifies which level",
                    "| !opsemp            15 .e |  EMP all enemies to 0 energy and shut down engines",
                    "| !opsshield <nm>  8/20 .s |  Shield <name>.  (L2)<all> = All friendlies in base",
                    "|__________________________/",
            };
            m_botAction.privateMessageSpam(p.getArenaPlayerID(), helps);
        } else if( msg.equalsIgnoreCase("msg") ) {
            String[] helps = {
                    "      OPS MESSAGES  -  Each uses 1 COMM, +1 if msg is a sabotage",
                    ".-----------------------------------------------------------------.",
                    "| TIER 1: !opsmsg #                    | To all army pilots       |",
                    "| TIER 2: !opsPM name:#  (T/S for name)| To 1 pilot/Terrs/Sharks  |",
                    "| TIER 3: !opssab msg|PM #|name:#      | Msg or PM sent to enemy  |",
                    ".______________________________________|_________________________/",
                    "| !opsmsg 1          .m |  Defend/assault top base (friend/foe #s, name of terr)",
                    "|         2             |  Defend/assault bottom base",
                    "|         3             |  Terr needed ASAP; requesting change of ships",
                    "|         4             |  Shark needed ASAP; requesting change of ships",
                    "| !opsPM <name>:1    .pm|  (To individual) Order to secure and hold top base",
                    "|               2       |  (To individual) Order to secure and hold bottom base",
                    "| !opsPM T:1            |  (To all Terrs)  Terr needed at top base immediately",
                    "|          2            |  (To all Terrs)  Terr needed at bottom base immediately",
                    "| !opsPM S:1            |  (To all Sharks) Shark needed at top base immediately",
                    "|          2            |  (To all Sharks) Shark needed at bottom base immediately",
                    "| !opssab            .sm|  Works like above commands but sends to enemy army. Ex:",
                    "|                       |  '!opssab msg 2' sends !opsmsg 2 to enemy w/ fake data.",
                    "|                       |  (False pilot counts, says there is no Terr, etc.)",
                    "|______________________/",
            };
            m_botAction.privateMessageSpam(p.getArenaPlayerID(), helps);
        }
    }

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
                "         DISTENSION   -   Upgradeable Trench Wars Basing    -   by G. Dugwyler",
                "Presently in beta.  Intro to come soon.  Type !beta for info on recent updates.",
                "Join ?chat=distension for help, or see the forums.trenchwars.org thread for more info."
        };
        m_botAction.privateMessageSpam(p.getArenaPlayerID(), intro1);
    }


    /**
     * Shows what prizes are associated with the basic upgrades.
     * @param name
     * @param msg
     */
    public void cmdUpgInfo( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null || p.getShipNum() == -1 )
            throw new TWCoreException( "Sorry, I don't recognize you.  If you !return to your army, maybe I can help." );

        int ship = 0;

        String[] msgs = msg.split(":");
        if( msgs.length == 1 )
            ship = p.getShipNum();
        else {
            try {
                ship = Integer.parseInt( msgs[1] );
            } catch (NumberFormatException e) {
                throw new TWCoreException( "Sorry, but if you're providing a ship number I need to be able to read it...  Uh, try !ui upg#:ship# ..." );
            }
        }

        if( ship < 1 || ship > 9 )
            throw new TWCoreException( "Sorry, only info is available on ships 1 through 9 ..." );

        if( !p.shipIsAvailable( ship ) )
            throw new TWCoreException( "You don't seem to own that ship ... can't help you there!" );

        Integer upgNum = 0;
        try {
            upgNum = Integer.parseInt(msgs[0]);
        } catch (NumberFormatException e) {
            throw new TWCoreException( "What upgrade do you want info on?  Do I look like a mind-reader?  Check the !armory before you start asking ..." );
        }

        if( upgNum < 1 || upgNum > NUM_UPGRADES )
            throw new TWCoreException( "What the hell upgrade is that?  You check the !armory -- find out that you're making this crap up ..." );

        ShipUpgrade up = m_shipGeneralData.get(ship).getUpgrade(upgNum - 1);
        if( up.getMaxLevel() == -1 || up.getMaxLevel() == 0 )
            throw new TWCoreException( "What the hell upgrade is that?  You check the !armory -- find out that you're making this crap up ..." );

        String desc = getUpgradeText(up.getPrizeNum());
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
                " - Data is saved, but will be cleared at release (coming soon)",
                " - Top 3 players (combined earned RP) awarded bonus points in public release",
                " - For every bug reported, points will be awarded (?message dugwyler)",
                " - Stats are up here courtesy of Foreign: http://www.trenchwars.org/distension",
                ".",
                "RECENT UPDATES  -  2/22/07",
                " - Changes are becoming too numerous to update here.  See this thread instead:",
                "          http://forums.trenchwars.org/showthread.php?t=31676",
        };
        m_botAction.privateMessageSpam( p.getArenaPlayerID(), beta );
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
        if( name.startsWith("^") ) {
            // Biller down ... OH SHI--
            m_botAction.sendArenaMessage("BILLING SERVER DOWN: Automatic shutdown initiated!", Tools.Sound.LISTEN_TO_ME );
            cmdSaveDie("DistensionBot","");
            return;
        }
        m_players.remove( name );
        Player player = m_botAction.getPlayer(event.getPlayerID());
        if( player != null ) {
            if( player.getShipType() != 0 )
                m_botAction.specWithoutLock(player.getPlayerID());
        }
        // If mid-round in a flag game, show appropriate flag info
        if( flagTimeStarted && flagTimer != null && flagTimer.isRunning() ) {
            HashMap <Integer,Boolean>flags = new HashMap<Integer,Boolean>();
            if( m_singleFlagMode ) {
                switch( m_flagOwner[0] ) {
                    case 0:
                        flags.put(LVZ_TOPBASE_ARMY0, true);
                        break;
                    case 1:
                        flags.put(LVZ_TOPBASE_ARMY1, true);
                        break;
                    default:
                        flags.put(LVZ_TOPBASE_EMPTY, true);
                    break;
                }
            } else {
                switch( m_flagOwner[0] ) {
                    case 0:
                        flags.put(LVZ_TOPBASE_ARMY0, true);
                        break;
                    case 1:
                        flags.put(LVZ_TOPBASE_ARMY1, true);
                        break;
                    default:
                        flags.put(LVZ_TOPBASE_EMPTY, true);
                    break;
                }
                switch( m_flagOwner[1] ) {
                    case 0:
                        flags.put(LVZ_BOTBASE_ARMY0, true);
                        break;
                    case 1:
                        flags.put(LVZ_BOTBASE_ARMY1, true);
                        break;
                    default:
                        flags.put(LVZ_BOTBASE_EMPTY, true);
                    break;
                }
            }
            if( flagTimer.getSecondsHeld() > 0 )
                flags.put(LVZ_SECTOR_HOLD, true);
            m_botAction.manuallySetObjects(flags, event.getPlayerID());
        }
        DistensionPlayer p = new DistensionPlayer(name);
        m_players.put( name, p );
    }


    /**
     * Save player data if unexpectedly leaving Distension.
     * @param event Event to handle.
     */
    public void handleEvent(PlayerLeft event) {
        Player pInternal = m_botAction.getPlayer(event.getPlayerID());
        if( pInternal == null ) {
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();
            Tools.printLog("Unable to find player leaving -- did not save!");
            if( DEBUG ) {
                m_botAction.sendArenaMessage( "Unable to find player leaving -- did not save!  Notify staff immediately." );
                m_botAction.sendUnfilteredPublicMessage("?message dugwyler:Unable to find player leaving!  Did not save.");
            }
            return;
        }
        String name = pInternal.getPlayerName();
        m_lagouts.remove( name );
        DistensionPlayer player = m_players.get( name );
        if( player == null ) {
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();
            Tools.printLog("Unable to find player leaving -- did not save '" + name + "'!");
            if( DEBUG ) {
                m_botAction.sendArenaMessage( "Unable to find player leaving -- did not save '" + name + "'!  Notify staff immediately." );
                m_botAction.sendUnfilteredPublicMessage("?message dugwyler:Unable to find player leaving!  Did not save '" + name + "'!");
            }
            return;
        }
        if( player.getShipNum() > 0 ) {
            checkFlagTimeStop();
            if( System.currentTimeMillis() > lastAssistAdvert + ASSIST_REWARD_TIME )
                checkForAssistAdvert = true;
        }
        player.saveCurrentShipToDBNow();
        player.savePlayerDataToDB();
        m_specialAbilityPrizer.removePlayer( player );
        m_waitingToEnter.remove( player );
        doSwapOut( player, false );
        m_players.remove( name );
        for( DistensionArmy a : m_armies.values() )
            a.recalculateFigures();
    }


    /**
     * Set flag information appropriately based on single or two flag game.
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
        int oldOwnerID = m_flagOwner[flagID];
        m_flagOwner[flagID] = p.getFrequency();

        // If flag is already claimed, try to take it away from old freq
        if( oldOwnerID != -1 ) {
            DistensionArmy oldOwnerArmy = m_armies.get( new Integer( oldOwnerID ) );
            if( oldOwnerArmy != null ) {
                if( m_singleFlagMode ) {
                    // Ignore bottom flag and only check top flag / ID 0.
                    // Also, do not deal with sector breaks in single flag mode
                    if( flagID == 0 ) {
                        if( flagTimeRunning ) {
                            if( oldOwnerID == 0 )
                                flagObjs.hideObject(LVZ_TOPBASE_ARMY0);
                            else
                                flagObjs.hideObject(LVZ_TOPBASE_ARMY1);
                        }
                        oldOwnerArmy.adjustFlags( -1 );
                    }
                } else {
                    if( flagTimeRunning ) {
                        if( oldOwnerArmy.getNumFlagsOwned() == 2 )
                            holdBreaking = true;
                        if( flagID == 0 )
                            if( oldOwnerID == 0 )
                                flagObjs.hideObject(LVZ_TOPBASE_ARMY0);
                            else
                                flagObjs.hideObject(LVZ_TOPBASE_ARMY1);
                        else
                            if( oldOwnerID == 0 )
                                flagObjs.hideObject(LVZ_BOTBASE_ARMY0);
                            else
                                flagObjs.hideObject(LVZ_BOTBASE_ARMY1);
                    }
                    oldOwnerArmy.adjustFlags( -1 );
                }
            }
        } else {
            if( flagTimeRunning ) {
                if( m_singleFlagMode ) {
                    if( flagID == 0 )
                        flagObjs.hideObject(LVZ_TOPBASE_EMPTY);
                } else {
                    if( flagID == 0 )
                        flagObjs.hideObject(LVZ_TOPBASE_EMPTY);
                    else
                        flagObjs.hideObject(LVZ_BOTBASE_EMPTY);
                }
            }
        }

        DistensionArmy newOwnerArmy = m_armies.get( new Integer( p.getFrequency() ) );
        if( newOwnerArmy != null ) {
            if( m_singleFlagMode ) {
                if( flagID == 0 ) {
                    newOwnerArmy.adjustFlags( 1 );
                    if( flagTimeRunning ) {
                        if( newOwnerArmy.getID() == 0 )
                            flagObjs.showObject(LVZ_TOPBASE_ARMY0);
                        else
                            flagObjs.showObject(LVZ_TOPBASE_ARMY1);
                        holdSecuring = true;
                    }
                }
            } else {
                newOwnerArmy.adjustFlags( 1 );
                if( flagTimeRunning ) {
                    if( flagID == 0 )
                        if( newOwnerArmy.getID() == 0 )
                            flagObjs.showObject(LVZ_TOPBASE_ARMY0);
                        else
                            flagObjs.showObject(LVZ_TOPBASE_ARMY1);
                    else
                        if( newOwnerArmy.getID() == 0 )
                            flagObjs.showObject(LVZ_BOTBASE_ARMY0);
                        else
                            flagObjs.showObject(LVZ_BOTBASE_ARMY1);
                    if( newOwnerArmy.getNumFlagsOwned() == 2 )
                        holdSecuring = true;
                }
            }

        }
        if( !flagTimeRunning )
            return;

        m_botAction.manuallySetObjects( flagObjs.getObjects() );
        if( holdBreaking )
            flagTimer.holdBreaking( newOwnerArmy.getID(), p.getPlayerName() );
        if( holdSecuring )
            flagTimer.sectorClaiming( newOwnerArmy.getID(), p.getPlayerName() );
    }

    /**
     * Load player data files if they change into a ship (spec if they don't own
     * the ship), and dock player if they spec.
     * @param event Event to handle.
     */
    public void handleEvent(FrequencyShipChange event) {
        DistensionPlayer p = m_players.get( m_botAction.getPlayerName( event.getPlayerID() ) );

        if( p == null  ) {
            if( System.currentTimeMillis() > lastAssistAdvert + ASSIST_REWARD_TIME )
                checkForAssistAdvert = true;
            return;
        }

        // This clause catches for when we are aligning bot-recorded ship num with in-game ship num,
        // and also when we are aligning bot-recorded army ID with in-game frequency, as *setfreq
        // fires frequencyshipchange and not frequencychange.  In the case of checking freq,
        // we ignore any changes unless it's to a frequency that is not the army ID, so setfreq to
        // armyID is always safe/left unhandled by this event method.
        if( p.getShipNum() == event.getShipType() || p.getShipNum() == 9 && event.getShipType() == 0 ) {
            if( p.ignoreShipChanges() )         // If we've been ignoring their shipchanges and they returned to
                p.setIgnoreShipChanges(false);  // their old ship, mission complete.
            else {
                if( p.getArmyID() != event.getFrequency() ) {
                    if( p.getShipNum() == 9 ) {
                        m_botAction.setFreq( p.getArenaPlayerID(), p.getArmyID() );
                    } else {
                        //m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Hey, what're you trying to pull?  If you want to !assist the other army, do it the right way!" );
                        m_botAction.specWithoutLock( p.getArenaPlayerID() );
                    }
                }
            }
            return;
        }

        if( event.getShipType() != 0 ) {
            if( p.ignoreShipChanges() )
                return;
            // If player hops into a ship and has not yet done !return (or !enlist),
            // try to autoreturn (or enlist)
            if( p.getShipNum() == -1 ) {
                try {
                    cmdReturn( p.getName(), "" );
                    cmdPilot( p.getName(), "" + event.getShipType() );
                } catch( TWCoreException e ) {
                    m_botAction.specWithoutLock(p.getArenaPlayerID());
                    m_botAction.sendPrivateMessage(p.getArenaPlayerID(), e.getMessage() );
                }
            } else {
                try {
                    cmdPilot( p.getName(), "" + event.getShipType() );
                } catch( TWCoreException e ) {
                    m_botAction.specWithoutLock(p.getArenaPlayerID());
                    m_botAction.sendPrivateMessage(p.getArenaPlayerID(), e.getMessage() );
                }
            }
        }

        if( !m_readyForPlay )         // If bot has not fully started up,
            return;                   // don't operate normally here.
        if( event.getShipType() == 0 ) {
            if( p.getShipNum() == 9 && p.ignoreShipChanges() ) {
                p.setIgnoreShipChanges(false);
                doDock( p );
            } else if( p.getShipNum() > 0 && p.getShipNum() != 9 ) {
                doDock( p );
                if( flagTimer != null && flagTimer.isRunning() ) {
                    if( p.canUseLagout() ) {    // Essentially: did player spec or !dock (not !leave)
                        if( !p.isAtMaxLagouts() ) {
                            m_lagouts.put( p.getName(), flagTimer.getTotalSecs() );
                            m_botAction.sendPrivateMessage( p.getName(), "Use !lagout in the next " + LAGOUT_VALID_SECONDS + " seconds to return to battle and keep participation." );
                        }
                    } else {
                        if ( !m_waitingToEnter.isEmpty() )
                            doSwapOut( p, true );
                    }
                }
            }
        }

        m_botAction.setupObject( p.getArenaPlayerID(), LVZ_REARMING, false );
        m_botAction.setupObject( p.getArenaPlayerID(), LVZ_EMP, false );
        m_botAction.setupObject( p.getArenaPlayerID(), LVZ_ENERGY_TANK, false );
        m_botAction.setupObject( p.getArenaPlayerID(), LVZ_JUMPSPACE, false );
        m_botAction.setupObject( p.getArenaPlayerID(), LVZ_OPS_FAST_REARM, false );
        m_botAction.sendSetupObjectsForPlayer( p.getArenaPlayerID() );

        if( System.currentTimeMillis() > lastAssistAdvert + ASSIST_REWARD_TIME )
            checkForAssistAdvert = true;
    }

    /**
     * Check if someone used =#, and spec if they did.
     */
    public void handleEvent(FrequencyChange event) {
        DistensionPlayer p = m_players.get( m_botAction.getPlayerName( event.getPlayerID() ) );

        if( p == null ) {
            if( System.currentTimeMillis() > lastAssistAdvert + ASSIST_REWARD_TIME )
                checkForAssistAdvert = true;
            if( DEBUG )
                m_botAction.sendPrivateMessage("dugwyler", event.getPlayerID() + " had null playerget in freqchange event." );
            return;
        }
        if( p.ignoreShipChanges() ) {       // If we've been ignoring their shipchanges and they returned to
            p.setIgnoreShipChanges(false);  // their old ship, mission complete.
            return;
        }

        if( p.getArmyID() != event.getFrequency() ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Hey, what're you trying to pull?  If you want to !assist the other army, do it the right way!" );
            m_botAction.specWithoutLock( p.getArenaPlayerID() );
        }
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
     * If just starting a round, ensure everyone stays in the rearm area.
     * @param event Event to handle.
     */
    public void handleEvent(PlayerPosition event) {
        if( !m_roundGettingStarted )
            return;
        Player player = m_botAction.getPlayer( event.getPlayerID() );
        DistensionPlayer p = m_players.get( m_botAction.getPlayerName( event.getPlayerID() ) );
        if( p != null && player != null ) {
            if( player.getYTileLocation() > TOP_SAFE && player.getYTileLocation() < BOT_SAFE )
                // Out of bounds
                p.doRearmSafeWarp();
        }
    }


    /**
     * DEATH HANDLING
     *
     * Upon death, assign points based on level to the victor, and prize the
     * loser back into existence.

     * Adding up points:
     *   Flag multiplier:  No flags=1/2; 1 flag=regular; 2 flags=+50%
     *
     * Ranks and army strengths modify point values as well as other extraneous factors.
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

        if( killer == null ) {
            loser.clearSuccessiveKills();
            return;
        }

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
        boolean isVictorWeasel = victor.getShipNum() == 6;
        boolean isMaxReward = false;
        boolean isRepeatKillLight = false;
        boolean isRepeatKillHard = false;
        boolean isFirstKill = (victor.getRecentlyEarnedRP() == 0);
        boolean endedStreak = false;
        int loserRank = Math.max( 1, loser.getRank() );
        int victorRank = Math.max( 1, victor.getRank() );
        int rankDiff = loserRank - victorRank;

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
                    div = 1.5f;
                else
                    div = 2.5f;
            }
            // If lowbies get TKd, it shouldn't hurt as much, because ... well, it's easy to TK them.
            if( rankDiff <= -RANK_DIFF_MED )
                div *= 1.5f;
            int loss = Math.round((float)victor.getRank() / div);
            if( DEBUG )
                loss = Math.round((float)loss * DEBUG_MULTIPLIER);
            victor.addRankPoints( -loss );
            // Teammate dying on a Shark or WB's mines does not clear streak
            if( killer.getShipType() != Tools.Ship.SHARK && killer.getShipType() != Tools.Ship.WARBIRD )
                victor.clearSuccessiveKills();
            if( loss > 0 && victor.wantsKillMsg() )
                m_botAction.sendPrivateMessage( killer.getPlayerName(), "-" + loss + " RP for TKing " + killed.getPlayerName() + "." );
            m_botAction.showObjectForPlayer(victor.getArenaPlayerID(), LVZ_TK);
            m_botAction.showObjectForPlayer(loser.getArenaPlayerID(), LVZ_TKD);
        } else {
            endedStreak = loser.clearSuccessiveKills();
            // Otherwise: Add points via level scheme
            DistensionArmy killerarmy = m_armies.get( new Integer(killer.getFrequency()) );
            DistensionArmy killedarmy = m_armies.get( new Integer(killed.getFrequency()) );
            if( killerarmy == null || killedarmy == null )
                return;
            int points;

            // Loser is many levels above victor:
            //   Victor capped, but loser is humiliated with some point loss
            if( rankDiff >= RANK_DIFF_MED ) {

                points = victorRank + RANK_DIFF_MED;
                isMaxReward = true;

                // Support ships are not humiliated; assault are
                if( ! loser.isSupportShip() ) {
                    int loss = 0;
                    if( rankDiff >= RANK_DIFF_HIGHEST )
                        loss = points;
                    else if( rankDiff >= RANK_DIFF_VHIGH )
                        loss = (points / 2);
                    else
                        loss = (points / 3);
                    if( points > 0 ) {
                        loser.addRankPoints( -loss );
                        if( loser.wantsKillMsg() )
                            m_botAction.sendPrivateMessage(loser.getArenaPlayerID(), "HUMILIATION!  -" + loss + "RP for being killed by " + victor.getName() + "(" + victor.getRank() + ")");
                    }
                }
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

            points = Math.round(((float)points * armySizeWeight));

            int prox = Math.max(STREAK_RANK_PROXIMITY_MINIMUM, (victorRank / STREAK_RANK_PROXIMITY_DIVISOR));
            boolean addedToStreak = -rankDiff <= prox;
            boolean killInBase = true;

            float flagMulti = -1;
            // Flags don't matter while the flag timer is running.
            if( flagTimer != null && flagTimer.isRunning() ) {
                flagMulti = killerarmy.getNumFlagsOwned();
                if( m_singleFlagMode ) {
                    if( flagMulti == 1.0f )
                        flagMulti = 1.1f;
                } else {
                    if( flagMulti == 0f ) {
                        if( armySizeWeight > ASSIST_WEIGHT_IMBALANCE ) {
                            flagMulti = 0.5f;
                        } else {
                            // Reduced RP for 0 flag rule doesn't apply if armies are imbalanced.
                            flagMulti = 1;
                        }
                    } else if( flagMulti == 2.0f ) {
                        flagMulti = 1.5f;
                    }
                    points = (int)((float)points * flagMulti);
                }

                if( flagTimer != null && flagTimer.isRunning() ) {
                    // Don't count streak if the player making the kill was not in base & round is going
                    if( ! ((killer.getYTileLocation() > TOP_ROOF && killer.getYTileLocation() < TOP_LOW) ||
                            (killer.getYTileLocation() > BOT_LOW  && killer.getYTileLocation() < BOT_ROOF)) ) {
                        killInBase = false;
                        addedToStreak = false;
                    }
                }
            }

            // Track successive kills for weasel unlock & streaks
            if( addedToStreak ) {   // Streaks only count on players close to your lvl & when in base
                if( victor.addSuccessiveKill() ) {
                    // TODO: If player earned weasel off this kill, check if loser/killed player has weasel ...
                    // and remove it if they do!
                }
                // Check if M.A.S.T.E.R. Drive should fire (every 5 successive kills, has a chance)
                victor.checkMasterDrive();
            }

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
                    if( isVictorWeasel )
                        points = Math.round((float)points * 2.0f);
                    else
                        points = Math.round((float)points * 1.50f);
                else
                    if( isVictorWeasel )
                        points = Math.round((float)points * 1.20f);
                    else
                        points = Math.round((float)points * 1.10f);
            }

            if( endedStreak )
                points = Math.round((float)points * 1.50f);

            if( ! killInBase )
                points -= Math.round((float)points * 0.2f);

            if( points < 1 )
                points = 1;


            int actualEarnedPoints = victor.addRankPoints( points );

            // Check if player ranked up from the kill
            if( victor.didRankUpFromLastKill() ) {
                // ... and taunt loser if he/she did
                if( loser.wantsKillMsg() )
                    m_botAction.sendPrivateMessage( loser.getArenaPlayerID(), "INSULT TO INJURY: " + victor.getName() + " just ranked up from your kill!", Tools.Sound.CRYING );
            }
            victor.getArmy().addSharedProfit( points );

            // Weasel kills destroy escape pods
            if( victor.getShipNum() == Tools.Ship.WEASEL ) {
                loser.escapePodFired = true;
            }

            // Determine whether or not vengeance is to be inflicted
            boolean revenged = loser.checkVengefulBastard( victor.getArenaPlayerID() );
            if( revenged ) {
                if( victor.wantsKillMsg() ) {
                    m_botAction.sendPrivateMessage(victor.getArenaPlayerID(), loser.getName() + " is a VENGEFUL BASTARD!" );
                }
                victor.setVenge( loser.getName() );
            }
            String venger = loser.checkVenge();
            if( venger != null ) {
                DistensionPlayer pveng = m_players.get(venger);
                if( pveng != null && pveng.getArenaPlayerID() != victor.getArenaPlayerID() ) {
                    int vengRP = (int)(points / 1.5);
                    pveng.addRankPoints( vengRP );
                    if( DEBUG )     // For DISPLAY purposes only; intentionally done after points added.
                        vengRP = Math.round((float)vengRP * DEBUG_MULTIPLIER);
                    if( pveng.wantsKillMsg() )
                        m_botAction.sendPrivateMessage( pveng.getArenaPlayerID(), "Vengeful Bastard assist on " + loser.getName() + ": +" + vengRP + " RP" );
                }
            }
            // Determine if the victor's Leeching should fire (full charge prized after a kill)
            victor.checkLeeching();
            victor.resetIdle();

            if( ! victor.wantsKillMsg() )
                return;

            points = actualEarnedPoints; // For DISPLAY purposes only.
            String msg = "+" + points + " RP: " + loser.getName() + "(" + loser.getRank() + ")";
            if( isMaxReward )
                msg += " [High rank cap]";
            if( isRepeatKillLight )
                msg += " [Repeat: -50%]";
            else if( isRepeatKillHard )
                msg += " [Multi-Repeat: 1 RP]";
            if( isTeK )
                if( isBTeK )
                    if( isVictorWeasel )
                        msg += " [BTerr: DOUBLE]";
                    else
                        msg += " [BTerr: +50%]";
                else
                    if( isVictorWeasel )
                        msg += " [Terr: +20%]";
                    else
                        msg += " [Terr: +10%]";
            if( flagMulti == 1.5f )
                msg += " [Both flags: +50%]";
            else if( flagMulti == 0.5f )
                msg += " [No flags: -50%]";
            else if( flagMulti == 1.1f )
                msg += " [Flag held: +10%]";
            if( endedStreak )
                msg += " [Ended streak: +50%]";
            if( !killInBase )
                msg += " [Outside base: -20%]";
            if( DEBUG )     // For DISPLAY purposes only; intentionally done after points added.
                msg += " [x" + DEBUG_MULTIPLIER + " beta]";
            if( isFirstKill )
                msg += " (!killmsg turns off this msg & gives +1% kill bonus)";
            int suc = victor.getSuccessiveKills();
            if( suc > 1 ) {
                msg += "  Streak: " + suc;
                if( !addedToStreak ) {
                    if( killInBase )
                        msg+= "[low rank/no inc.]";
                    else
                        msg+= "[outside base/no inc.]";
                }
            }
            m_botAction.sendPrivateMessage(victor.getName(), msg);
        }
    }



    // ***** MAJOR COMMAND PROCESSING

    /**
     * Enlists in the supplied public army (or private army w/ pwd).  Those choosing default
     * armies may receive an enistment bonus.
     * @param name
     * @param msg
     */
    public void cmdEnlist( String name, String msg ) {
        if( msg == null )
            throw new TWCoreException( "Hmm.  Try that one again, hot shot.  Maybe tell me where you want to !enlist this time.");

        // Easy fix: Disallow names >18 chars to avoid name hacking.
        // (Extra char padding ensures no-one can imitate, in case "name" String only shows 19 total)
        if( name.length() > 18 )
            throw new TWCoreException( "Whoa there, is that a name you have, or a novel?  You need something short and snappy.  Hell, I'd reckon that anything 19 letters or more just won't cut it.  Come back when you have a better name.");
        if( name.startsWith("^") )
            throw new TWCoreException( "The billing server is down.  Distension is now shutting down to prevent record corruption...");

        DistensionPlayer p = m_players.get( name );
        if( p == null ) {
            p = new DistensionPlayer(name);
            m_players.put( name, p );
        } else
            if( p.getShipNum() != -1 )
                throw new TWCoreException( "Ah, but you're already enlisted.  Maybe you'd like to !defect to an army more worthy of your skills?");

        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fnArmyID FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name  ) +"'" );
            if( r.next() ) {
                m_botAction.sendPrivateMessage( name, "Enlist?  You're already signed up!  I know you: you can !return any time.  After that, maybe you'd like to !defect to an army more worthy of your skills?");
                m_botAction.SQLClose(r);
                return;
            }
            m_botAction.SQLClose(r);
        } catch (Exception e ) {
            m_botAction.sendPrivateMessage( name, DB_PROB_MSG );
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
        if( army == null )
            throw new TWCoreException( "You making stuff up now?  Maybe you should join one of those !armies that ain't just make believe..." );

        int bonus = 0;
        if( army.isPrivate() ) {
            if( pwd != null && !pwd.equals(army.getPassword()) )
                throw new TWCoreException( "That's a private army there.  And the password doesn't seem to match up.  Duff off." );
        } else {
            bonus = calcEnlistmentBonus( army );
        }

        m_botAction.sendPrivateMessage( name, "Ah, joining " + army.getName().toUpperCase() + "?  Excellent.  You are pilot #" + (army.getPilotsTotal() + 1) + "." );

        p.setArmy( armyNum );
        p.addPlayerToDB();
        p.setShipNum( 0 );
        army.adjustPilotsTotal(1);
        m_botAction.sendPrivateMessage( name, "Welcome aboard.  If you need an !intro to the game, I'll !help you out.  Or if you just want some action, jump in your new Warbird or Terrier." );
        if( bonus > 0 ) {
            m_botAction.sendPrivateMessage( name, "Your contract also entitles you to a " + bonus + " RP signing bonus!  Congratulations." );
            p.addShipToDB( 1, bonus );
            p.addShipToDB( 5, bonus );
        } else {
            p.addShipToDB( 1 );
            p.addShipToDB( 5 );
        }
        if( DEBUG ) {
            m_botAction.sendPrivateMessage( name, "Welcome to the beta test!  PM !help for commands, and !beta for updates.  Join ?chat=distension for questions and to stay up on tests.  (NOTE: you may now be PM'd when new tests start.)" );
        }
    }


    /**
     * Defects to another army.  This causes all ships to return to the start of the rank
     * in terms of progress toward the next.
     * @param name
     * @param msg
     */
    public void cmdDefect( String name, String msg ) {
        if( msg == null )
            throw new TWCoreException( "If you want to defect to one of the other !armies, you've got to at least have an idea which one..." );

        DistensionPlayer p = m_players.get( name );
        if( p == null )
            throw new TWCoreException( "Sorry, I don't recognize you.  If you !return to your army (such as it is), maybe I can help." );

        // For those defecting to an army w/ close to the same # pilots, !defect yes to confirm loss of 1 rank in every ship
        if( msg.equals("yes") ) {
            if( !m_defectors.containsKey(name) )
                return;
            DistensionArmy army = m_armies.get( m_defectors.get(name) );
            if( army == null )
                throw new TWCoreException( "Can't find the army to which you want to defect!  May want to contact a mod about this one ..." );
            try {
                String query = "SELECT fnPlayerID, fnShipNum, fnRank FROM tblDistensionShip WHERE fnPlayerID='" + p.getDatabaseID() + "'";
                ResultSet r = m_botAction.SQLQuery( m_database, query );
                if( r != null ) {
                    while( r.next() ) {
                        int ship = r.getInt("fnShipNum");
                        m_botAction.SQLQueryAndClose(m_database, "UPDATE tblDistensionShip SET fnRankPoints='" + m_shipGeneralData.get(ship).getNextRankCost(r.getInt("fnRank") - 2) + "'"+
                                " WHERE fnShipNum='" + ship + "' AND fnPlayerID='" + p.getDatabaseID() + "'");
                    }
                }
                m_botAction.SQLClose(r);
            } catch (Exception e ) {
                Tools.printStackTrace( "Error getting ship data for !defect from DB.", e );
                throw new TWCoreException( DB_PROB_MSG );
            }
            p.doDefect( army.getID() );
            m_botAction.sendPrivateMessage( name, "OK -- the ship controls are similar, but you're going to have a lot to relearn ...  Anyway, should be all set.  Good luck." );
            return;
        }


        if( p.getShipNum() != 0 )
            throw new TWCoreException( "Please !dock before trying to hop over to another army." );

        if( p.getArmyID() != p.getNaturalArmyID() )
            throw new TWCoreException( "If you're going to !defect, do you really need to !assist all that much?...  Go back to your normal army first!" );

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
            throw new TWCoreException( "Hmm... which army do you want to !defect to?  You sure that's one of the !armies here?" );
        }

        DistensionArmy army = m_armies.get(armyNum);
        if( army == null )
            throw new TWCoreException( "You making stuff up now?  Maybe you should defect to one of those !armies that ain't just make believe..." );

        if( army.isPrivate() )
            if( pwd != null && !pwd.equals(army.getPassword()) )
                throw new TWCoreException( "That's a private army there.  And the password doesn't seem to match up.  Duff off." );

        DistensionArmy oldarmy = m_armies.get( p.getNaturalArmyID() );
        if( oldarmy != null ) {
            if( oldarmy.getID() == army.getID() )
                throw new TWCoreException( "Now that's just goddamn stupid.  You're already in that army!" );
        } else
            throw new TWCoreException( "Whoa, and what the hell army are you in now?  You're confusing me... might want to tell someone important that I told you this." );


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
                throw new TWCoreException( "You are currently docked at " + p.getArmyName().toUpperCase() + " HQ.  You may pilot a ship at any time by hitting ESC + #.  You may !leave to record all data and stop the battle timer." );
            else
                throw new TWCoreException( "You are currently in flight." );
        }

        if( m_waitingToEnter.contains(p) )
            throw new TWCoreException( "You're already in the queue to enter in to the battle." );

        if( !p.getPlayerFromDB() ) {
            if( p.isBanned() ) {
                throw new TWCoreException( "ERROR: Civilians and discharged pilots are NOT authorized to enter this military zone." );
            } else {
                // If not banned, player has not yet enlisted.  Auto-enlist.
                try {
                    cmdEnlist(name, "");
                } catch (TWCoreException e) {
                    // Explicitly shown that we throw back to FrequencyShipChange if failed
                    throw e;
                }
            }
        }

        int players = 0;
        int highestTime = 0;
        LinkedList <DistensionPlayer>dockedPlayers = new LinkedList<DistensionPlayer>();
        for( DistensionPlayer p2 : m_players.values() ) {
            if( p2.getShipNum() >= 0 ) {
                players++;
                if( p2.getArenaPlayerID() != p.getArenaPlayerID() ) {
                    if( p2.getShipNum() == 0 )
                        dockedPlayers.add(p2);
                    else
                        if( p2.getMinutesPlayed() > highestTime )
                            highestTime = p2.getMinutesPlayed();
                }
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

        m_botAction.sendPrivateMessage( name, p.getPlayerRankString() + " " + p.getName().toUpperCase() + " authorized as a pilot of " + p.getArmyName().toUpperCase() + ".  Returning you to HQ." );
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
            throw new TWCoreException( "You are not currently in the battle.  Use !return to return to your army." );
        } else if( p.getShipNum() > 0 ) {
            cmdDock(name,"");
        }

        m_botAction.sendPrivateMessage( name, p.getName().toUpperCase() + " leaving hangars of " + p.getArmyName().toUpperCase() + ".  Time played today: " + p.getMinutesPlayed() + " min." );
        p.setShipNum( -1 );
        p.savePlayerDataToDB();
    }


    /**
     * Mans Tactical Ops station.
     * @param name
     * @param msg
     */
    public void cmdManOps( String name, String msg ) {
        cmdPilot( name, "9" );
    }

    /**
     * For players that are used to !pilot/!ship.
     * @param name
     * @param msg
     */
    public void cmdPilotDefunct( String name, String msg ) {
        throw new TWCoreException( "You do not need to use !pilot anymore to get into a ship.  Just hit ESC+#.  Check your !hangar to see which ships are available." );
    }

    /**
     * Enters a player as a given ship.  No longer accessible as !pilot.
     * @param name
     * @param msg
     */
    public void cmdPilot( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        int lastShipNum = p.getShipNum();
        if( lastShipNum == -1 )
            throw new TWCoreException( "You'll need to !return to your army or !enlist in a new one before you go flying off." );

        int shipNum = 0;
        try {
            shipNum = Integer.parseInt( msg );
        } catch ( Exception e ) {
            throw new TWCoreException( "Exactly which ship do you mean there?  Give me a number.  Maybe check the !hangar first." );
        }
        if( lastShipNum == shipNum )
            throw new TWCoreException( "You're already in that ship." );
        if( !p.shipIsAvailable( shipNum ) )
            throw new TWCoreException( "You don't own that ship.  Check your !hangar before you try flying something you don't have." );
        if( p.isRespawning() ) {
            p.isRespawning = false;
            m_botAction.hideObjectForPlayer( p.getArenaPlayerID(), LVZ_REARMING );
        }
        m_prizeQueue.removePlayer(p);

        // Check if Tactical Ops position is available
        if( shipNum == 9 ) {
            if( m_singleFlagMode )
                throw new TWCoreException( "Sorry, the Tactical Ops console is not active while only a single base is in contention." );
            for( DistensionPlayer p2 : m_players.values() )
                if( p2.getShipNum() == 9 && p2.getArmyID() == p.getArmyID() )
                    throw new TWCoreException( "Sorry, " + p2.getName() + " is already sitting at the Tactical Ops console." );
            // Let Tac Ops change to spec
            if( lastShipNum > 0 )
                p.setIgnoreShipChanges(true);
        }

        if( lastShipNum > 0 ) {
            String shipname = (lastShipNum == 9 ? "Tactical Ops" : Tools.shipName(p.getShipNum()));
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Total earned in " + shipname + ": " + p.getRecentlyEarnedRP() + " RP" );
            if( ! p.saveCurrentShipToDBNow() )
                throw new TWCoreException( "PROBLEM SAVING SHIP BEFORE CHANGE -- Notify a member of staff immediately." );
            if( lastShipNum == 9 )
                m_botAction.sendOpposingTeamMessageByFrequency( p.getArmyID(), p.getName() + " has left the Tactical Ops console." );
        } else {
            m_playerTimes.remove( name );
        }

        // Check for shark and terr balance -- do not overbalance with one or the other.
        if( shipNum == Tools.Ship.SHARK || shipNum == Tools.Ship.TERRIER ) {
            int friendly = 0;
            int enemy = 0;
            for( DistensionPlayer p2 : m_players.values() ) {
                if( p2.getShipNum() == shipNum )
                    if( p2.getArmyID() == p.getArmyID() )
                        friendly++;
                    else
                        enemy++;
            }
            if( shipNum == Tools.Ship.SHARK && friendly > 2 )
                throw new TWCoreException( "Sorry, only two Sharks are allowed per army." );
            // If we already have more than they do (2 more needed for terr), do not allow the change
            if( shipNum == Tools.Ship.SHARK && friendly > enemy + 1 )
                throw new TWCoreException( "Sorry, the other army doesn't have enough Sharks to allow you to Shark fairly!" );
            if( shipNum == Tools.Ship.TERRIER && friendly > enemy + 2 )
                throw new TWCoreException( "Sorry, the other army doesn't have enough Terriers to allow you to Terr fairly!" );
        }

        p.setShipNum( shipNum );
        if( !p.getCurrentShipFromDB() ) {
            m_botAction.sendPrivateMessage( name, "Having trouble getting that ship for you.  Please contact a mod." );
            p.setShipNum( 0 );
            return;
        }

        if( shipNum == 9 )
            m_botAction.sendOpposingTeamMessageByFrequency( p.getArmyID(), p.getName() + " is now manning the Tactical Ops console." );

        // Simple fix to cause sharks and terrs to not lose MVP
        if( (lastShipNum > 0) && (shipNum == Tools.Ship.TERRIER || shipNum == Tools.Ship.SHARK ) ) {
            int reward = 0;
            if( System.currentTimeMillis() > lastTerrSharkReward + TERRSHARK_REWARD_TIME ) {
                int ships = 0;
                for( DistensionPlayer p2 : m_players.values() ) {
                    if( p2.getShipNum() == shipNum )
                        if( p2.getArmyID() == p.getArmyID() )
                            ships++;
                }
                int pilots = p.getArmy().getPilotsInGame();
                int rank = Math.max(1, p.getRank());
                if( shipNum == Tools.Ship.TERRIER ) {
                    if( ships == 0 && pilots > 3 ) {
                        reward = rank * 5;
                        m_botAction.sendPrivateMessage( name, "You receive a rank bonus of " + (DEBUG ? ((int)(DEBUG_MULTIPLIER * (float)reward)) : reward) + "RP for switching to Terrier when one was badly needed." );
                    } else if( ships == 1 && pilots > 9 ){
                        reward = rank * 3;
                        m_botAction.sendPrivateMessage( name, "You receive a rank bonus of " + (DEBUG ? ((int)(DEBUG_MULTIPLIER * (float)reward)) : reward) + "RP for switching to Terrier when a second one was needed." );
                    }
                } else {
                    if( ships == 0 && pilots > 7 ) {
                        reward = rank * 3;
                        m_botAction.sendPrivateMessage( name, "You receive a rank bonus of " + (DEBUG ? ((int)(DEBUG_MULTIPLIER * (float)reward)) : reward) + "RP for switching to Shark when one was needed." );
                    }
                }
                if( reward > 0 ) {
                    p.addRankPoints(reward, false);
                    lastTerrSharkReward = System.currentTimeMillis();
                }
            }
        }
        if( flagTimer != null && flagTimer.isRunning() ) {
            if( flagTimer.getHoldingFreq() == p.getArmyID() && flagTimer.getSecondsHeld() > 0 ) {
                // If player is changing ships while their freq is securing a hold,
                // they may just be doing it to scoop round-end points; don't keep MVP.
                // However, let players switch to Sharks and Terrs if only 20 seconds have passed
                // for the sector hold
                if( p.getShipNum() == Tools.Ship.SHARK || p.getShipNum() == Tools.Ship.TERRIER ) {
                    if( flagTimer.getSecondsHeld() > 20 ) {
                        m_botAction.sendPrivateMessage( name, "For changing ships while your army has had a sector hold for more than 20 seconds, your participation counter has been reset." );
                        m_playerTimes.remove( name );
                    }
                }
            }
            if( m_playerTimes.get( name ) == null )
                m_playerTimes.put( name, new Integer( flagTimer.getTotalSecs() ) );
        }

        for( DistensionArmy a : m_armies.values() )
            a.recalculateFigures();
        p.putInCurrentShip();
        p.prizeUpgradesNow();
        m_lagouts.remove( name );
        m_waitingToEnter.remove(p);
        if( shipNum != 9 )
            p.setLagoutAllowed(true);
        if( !flagTimeStarted || stopFlagTime ) {
            checkFlagTimeStart();
        }
        cmdProgress( name, null );
        p.addRankPoints(0,false); // If player has enough RP to level, rank them up.
        p.setIgnoreShipChanges(false);

        // Make sure a player knows they can upgrade if they have no upgrades installed (such as after a refund)
        if( p.getUpgradeLevel() == 0 && p.getUpgradePoints() >= 10 ) {
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "TIP: *** You have no upgrades installed! ***  Use !armory to see your upgrade options, and !upgrade # to upgrade a specific part of your ship.", 1 );
        }
    }


    /**
     * Docks player (that is, sends them to spectator mode -- not the sex act, or the TW sysop).
     * This also saves ship data to the DB.  The command !dock is the same as just going to
     * spectator mode the manual way.
     * @param name
     * @param msg
     */
    public void cmdDock( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() == 9 ) {
            m_botAction.sendOpposingTeamMessageByFrequency( p.getArmyID(), p.getName() + " has left the Tactical Ops console." );
            p.setIgnoreShipChanges(true);
            m_botAction.setShip( p.getArenaPlayerID(), 1 );
            m_botAction.specWithoutLock( p.getArenaPlayerID() );
        } else
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
        checkFlagTimeStop();
        if( p.saveCurrentShipToDBNow() ) {
            String shipname = (p.getShipNum() == 9 ? "Tactical Ops" : Tools.shipName(p.getShipNum()));
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Ship status saved; you are now docked.  " + p.getRecentlyEarnedRP() + " RP earned in " + shipname + ".  Time played today: " + p.getMinutesPlayed() + " min.  !leave to stop timer." );
            p.setIgnoreShipChanges(false);
            p.setShipNum( 0 );
        } else {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Ship status was NOT logged.  Please notify a member of staff immediately!");
        }
        DistensionArmy army = p.getArmy();
        if( army != null ) {
            army.recalculateFigures();
            if( !p.canUseLagout() )
                p.setAssist(-1);
        } else {
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();
            if( !p.canUseLagout() )
                p.assistArmyID = -1;
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
        if( currentShip == -1 )
            throw new TWCoreException( "You about to !return to your army, or do you need to !enlist in one?  Don't recognize you quite." );

        HashMap <Integer,Integer>shipRanks = new HashMap<Integer,Integer>();
        try {
            String query = "SELECT fnShipNum, fnRank FROM tblDistensionShip WHERE fnPlayerID='" + p.getDatabaseID() + "'";
            ResultSet r = m_botAction.SQLQuery( m_database, query );
            if( r != null ) {
                while( r.next() )
                    shipRanks.put( r.getInt("fnShipNum"), r.getInt("fnRank") );
                m_botAction.SQLClose(r);
            }
        } catch (Exception e ) {
            Tools.printStackTrace( "Error getting ship ranks for !hangar from DB.", e );
            m_botAction.sendPrivateMessage( name, DB_PROB_MSG );
            return;
        }


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
                    if( i != 9 )
                        m_botAction.sendPrivateMessage( name, "  " + i + "   " + shipname + "RANK " + pf.getRankUnlockedAt() + " NEEDED"  );
                    else
                        m_botAction.sendPrivateMessage( name, "  " + i + "   " + shipname + "RANK " + pf.getRankUnlockedAt() + " NEEDED IN ALL SHIPS + Officer");
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
        if( shipNum == -1 )
            throw new TWCoreException( "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );

        if( shipNum == 0 ) {
            m_botAction.sendPrivateMessage( theName, p.getPlayerRankString() + " " + p.getName().toUpperCase() + " of " + p.getArmyName() + ": DOCKED.  [Your data is saved; safe to leave arena.]" );
        } else {
            String shipname = ( shipNum == 9 ? "Tactical Ops" : Tools.shipName(shipNum) );
            m_botAction.sendPrivateMessage( theName, p.getPlayerRankString() + " " + p.getName().toUpperCase() + " of " + p.getArmyName().toUpperCase() + ":  " + shipname.toUpperCase() +
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
            if( p.isSupportShip() ) {
                float sharingPercent;
                float calcRank = (float)p.getRank();
                if( shipNum == 4 )
                    if( calcRank > 10.0f )
                        calcRank = 10.0f;
                if( shipNum == 6 )
                    if( calcRank > 8.0f )
                        calcRank = 8.0f;
                sharingPercent = calcRank / 10.0f;
                m_botAction.sendPrivateMessage( theName, "Profit sharing: " + sharingPercent + "%" );
            }
            if( shipNum == 9 )
                m_botAction.sendPrivateMessage( theName, "OP ( " + p.getCurrentOP() + " / " + p.getMaxOP() + " )   Comm authorizations ( " + p.getCurrentComms() + " / 3 )" );
            if( mod == null )
                cmdProgress( name, msg, null );
            else
                cmdProgress( name, msg, mod );
        }
        // Display command rank progress info, w/ separate display for cadets enrolled in the academy.
        if( p.getBattlesWon() < WINS_REQ_RANK_ENSIGN ) {
            if( p.getBattlesWon() < WINS_REQ_RANK_CADET_4TH_CLASS ) {
                m_botAction.sendPrivateMessage( theName, "You are a recruit candidate for the Officer's Academy.  Estimated you will be accepted to the Academy after " + p.getWinsRequiredForNextCommandRank() + " more battle(s) won.");
            } else {
                m_botAction.sendPrivateMessage( theName, "Enrolled in the Officer's Academy.  Estimated you will graduate to the next class after " + p.getWinsRequiredForNextCommandRank() + " more battle(s) won.");
            }
        } else {
            if( p.getBattlesWon() >= WINS_REQ_RANK_FLEET_ADMIRAL )
                m_botAction.sendPrivateMessage( theName, "You are the Fleet Admiral, the highest and most honorable rank of all." );
            else
                m_botAction.sendPrivateMessage( theName, "Estimated promotion after " + p.getWinsRequiredForNextCommandRank() + " more battle(s) won." );
        }
        m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "Total battles won with 50% or more participation: " + p.getBattlesWon() );
        m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "Time played today: " + p.getMinutesPlayed() + " minutes.   Current Streak: " + p.getSuccessiveKills() + " kills." );
        if( shipNum == 9 )
            m_botAction.sendPrivateMessage( theName, "Use !opsstatus (shortcut: ..) to see only OP points and comm auths." );
    }


    /**
     * Sends Ops single-line status msg about OP and comm auths.  In this section because it's
     * more properly grouped as a status command (provides no additional info).
     * @param name
     * @param msg
     */
    public void cmdOpsStatus( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() == 9 ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "OP ( " + p.getCurrentOP() + " / " + p.getMaxOP() + " )   Comm authorizations ( " + p.getCurrentComms() + " / 3 )" );
            p.resetIdle();
        }

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
        if( shipNum == -1 )
            throw new TWCoreException( "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );
        else if( shipNum == 0 )
            throw new TWCoreException( "You must be in-ship to see your progress toward the next rank." );

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
        m_botAction.sendPrivateMessage( name, "Progress:  " + (int)pointsSince + " / " + (int)pointsNext + "  (" + p.getPointsToNextRank() + " to Rank " + (p.getRank() + 1) + ")    [" + progString + "]  " + percent + "%  UP: " + p.getUpgradePoints() );
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
        if( shipNum == -1 )
            throw new TWCoreException( "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );
        else if( shipNum == 0 )
            throw new TWCoreException( "If you want to see the armory's selection for a ship, you'll need to pilot one first." );

        m_botAction.sendPrivateMessage( name, " #  Name                                  Curr /  Max       UP      Requirements" );
        Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum ).getAllUpgrades();
        ShipUpgrade currentUpgrade;
        int[] purchasedUpgrades = p.getPurchasedUpgrades();
        String printmsg;
        boolean printCost;
        //String[] display = new String[NUM_UPGRADES + 1];
        LinkedList <String>display = new LinkedList<String>();
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
                        printmsg += "[MAX]";
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
                display.add(printmsg);
            }
        }
        display.add( "RANK: " + p.getRank() + "  UPGRADES: " + p.getUpgradeLevel() + "  UP: " + p.getUpgradePoints()
        + (p.getUpgradePoints() == 0?"  (Rank up for +10UP)":"") );
        m_botAction.privateMessageSpam(p.getArenaPlayerID(), display);
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
        if( shipNum == -1 )
            throw new TWCoreException( "You'll need to !return to your army or !enlist in a new one before upgrading a ship." );
        else if( shipNum == 0 )
            throw new TWCoreException( "If you want to upgrade a ship, you'll need to pilot one first." );
        int upgradeNum = 0;
        try {
            upgradeNum = Integer.parseInt( msg );
        } catch ( Exception e ) {
            throw new TWCoreException( "Exactly which do you mean there?  Give me a number.  Maybe check the !armory first." );
        }
        upgradeNum--;  // Fix number to correspond with internal numbering scheme
        if( upgradeNum < 0 || upgradeNum >= NUM_UPGRADES )
            throw new TWCoreException( "Exactly which do you mean there?  Maybe check the !armory first.  #" + (upgradeNum + 1) + " doesn't work for me." );
        ShipUpgrade upgrade = m_shipGeneralData.get( shipNum ).getUpgrade( upgradeNum );
        int currentUpgradeLevel = p.getPurchasedUpgrade( upgradeNum );
        if( upgrade == null || currentUpgradeLevel == -1 )
            throw new TWCoreException( "Exactly which do you mean there?  Maybe check the !armory first.  #" + (upgradeNum + 1) + " doesn't work for me." );
        if( currentUpgradeLevel >= upgrade.getMaxLevel() )
            throw new TWCoreException(  "You've upgraded that one as much as you can." );
        int req = upgrade.getRankRequired( currentUpgradeLevel );
        if( p.getRank() < req )
            throw new TWCoreException( "Sorry... I'm not allowed to fit your ship with that until you're at least a rank " + req + " pilot.");
        int cost = upgrade.getCostDefine( currentUpgradeLevel );
        if( p.getUpgradePoints() < cost )
            throw new TWCoreException( "You'll need more army upgrade authorization points (UP) before I can fit your ship with that.  And the only way to get those is to go up in rank... by -- you got it! -- killin'." );

        Integer scrapped = m_scrappingPlayers.get(name);
        if( scrapped != null && scrapped == upgradeNum )
            throw new TWCoreException( "You just took that off, and now you want me to put it back on, just like that?  What am I, a machine?" );
        if( p.modifyUpgrade( upgradeNum, 1 ) == false )
            throw new TWCoreException( "ERROR modifying upgrade " + upgradeNum + ".  Notify a mod immediately." );

        p.addUpgPoints( -cost );
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

        // Get the    [ROT] crap out.
        String[] upgparse = upgrade.getName().split("  ", 2);
        if( upgrade.getMaxLevel() == 1 ) {
            m_botAction.sendPrivateMessage( name, upgparse[0] + " [" + getUpgradeText(upgrade.getPrizeNum()) + "] installed.  -" + cost + "UP from army allowance." + (prized?"":"  You will receive this upgrade at next rearm.") );
        } else {
            m_botAction.sendPrivateMessage( name, upgparse[0] + " [" + getUpgradeText(upgrade.getPrizeNum()) + "] upgraded to Level " + (currentUpgradeLevel + 1) + ".  -" + cost + "UP from army allowance." + (prized?"":"  You will receive this upgrade at next rearm.") );
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
        if( shipNum == -1 )
            throw new TWCoreException( "You'll need to !return to your army before you scrap any upgrades." );
        else if( shipNum == 0 )
            throw new TWCoreException( "If you want to remove upgrades on a ship, you'll need to pilot one first." );
        if( msg.equals("all") ) {
            // Wrapper for !scrapall *
            cmdScrapAll( name, "*" );
            return;
        }

        int upgradeNum = 0;
        try {
            upgradeNum = Integer.parseInt( msg );
        } catch ( Exception e ) {
            throw new TWCoreException( "Exactly which do you mean there?  Give me a number.  Maybe check the !armory first." );
        }
        upgradeNum--;  // Fix number to correspond with internal numbering scheme
        if( upgradeNum < 0 || upgradeNum >= NUM_UPGRADES )
            throw new TWCoreException( "Exactly which do you mean there?  Maybe check the !armory first.  #" + (upgradeNum + 1) + " doesn't work for me." );
        ShipUpgrade upgrade = m_shipGeneralData.get( shipNum ).getUpgrade( upgradeNum );
        int currentUpgradeLevel = p.getPurchasedUpgrade( upgradeNum );
        if( upgrade == null || currentUpgradeLevel == -1 )
            throw new TWCoreException( "Exactly which do you mean there?  Maybe check the !armory first.  #" + (upgradeNum + 1) + " doesn't work for me." );
        if( currentUpgradeLevel <= 0 )
            throw new TWCoreException( "You haven't exactly upgraded that, now have you?" );

        int cost = upgrade.getCostDefine( currentUpgradeLevel - 1);
        if( p.modifyUpgrade( upgradeNum, -1 ) == false )
            throw new TWCoreException( "ERROR modifying upgrade " + upgradeNum + ".  Notify a mod immediately." );
        p.addUpgPoints( cost );
        if( upgrade.getPrizeNum() == ABILITY_PRIORITY_REARM )
            p.setFastRespawn(false);
        else if( upgrade.getPrizeNum() > 0 )
            m_botAction.specificPrize( name, -upgrade.getPrizeNum() );
        if( upgrade.getMaxLevel() == 1 ) {
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " [" + getUpgradeText(upgrade.getPrizeNum()) + "] removed.  +" + cost + "UP to army allowance." );
        } else {
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " [" + getUpgradeText(upgrade.getPrizeNum()) + "] downgraded to level " + (currentUpgradeLevel - 1) + ".  +" + cost + "UP to army allowance." );
        }
        m_scrappingPlayers.remove(name);
        m_scrappingPlayers.put(name,upgradeNum);

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
        if( shipNum == -1 )
            throw new TWCoreException( "You'll need to !return to your army before you scrap any upgrades." );
        else if( shipNum == 0 )
            throw new TWCoreException( "If you want to remove upgrades on a ship, you'll need to pilot one first." );

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
                throw new TWCoreException( "Try !scrapall with * to scrap all upgrades, # for all of a specific upg, S for all special upgs, M for all maneuver upgs, and E for all energy/recharge upgs." );
            }
            upgradeNum--;  // Fix number to correspond with internal numbering scheme
            if( upgradeNum < 0 || upgradeNum >= NUM_UPGRADES )
                throw new TWCoreException( "Exactly which do you mean there?  Maybe check the !armory first.  #" + upgradeNum + " doesn't work for me." );
            ShipUpgrade upgrade = m_shipGeneralData.get( shipNum ).getUpgrade( upgradeNum );
            int currentUpgradeLevel = p.getPurchasedUpgrade( upgradeNum );
            if( upgrade == null || currentUpgradeLevel == -1 )
                throw new TWCoreException( "Exactly which do you mean there?  Maybe check the !armory first.  #" + upgradeNum + " doesn't work for me." );
            if( currentUpgradeLevel <= 0 )
                throw new TWCoreException( "You haven't exactly upgraded that, now have you?" );
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
                pointsReturned += upgrade.getCostDefine( p.getPurchasedUpgrade( i ) - 1 );
                if( p.modifyUpgrade( i, -1 ) == false )
                    m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "ERROR modifying upgrade " + i + ".  Notify a mod immediately." );
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
        if( p.getShipNum() == -1 )
            throw new TWCoreException( "Please !return to your army or !enlist first." );
        Iterator <Player>i = m_botAction.getFreqPlayerIterator(p.getArmyID());
        if( !i.hasNext() )
            throw new TWCoreException( "No pilots detected in your army!" );
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
        if( p.getShipNum() == -1 && !isStaff )
            throw new TWCoreException( "You must !return or !enlist in an army first." );
        Player p2;
        p2 = m_botAction.getPlayer( msg );
        if( p2 == null )
            p2 = m_botAction.getFuzzyPlayer( msg );
        if( p2 == null )
            throw new TWCoreException( "Player '" + msg + "' can not be located." );
        if( p.getArmyID() != p2.getFrequency() && !isStaff )
            throw new TWCoreException( p2.getPlayerName() + " is not a member of your army!" );
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
        if( p.getShipNum() == -1 )
            throw new TWCoreException( "You must !return or !enlist in an army first." );
        boolean autoReturn = msg.equals(":auto:");
        if( p.isRespawning() )
            throw new TWCoreException( "Please wait until your current ship is rearmed before attempting to assist." );
        int armyToAssist = -1;
        if( msg.equals("") || autoReturn ) {
            armyToAssist = p.getNaturalArmyID();
        } else {
            try {
                armyToAssist = Integer.parseInt( msg );
            } catch (NumberFormatException e) {
                throw new TWCoreException( "Which of the !armies would you like to assist?" );
            }
        }

        DistensionArmy assistArmy = m_armies.get( new Integer( armyToAssist ) );
        if( assistArmy == null )
            throw new TWCoreException(  "Exactly which of those !armies you trying to help out there?" );

        // Check if Tactical Ops position is available
        if( p.getShipNum() == 9 ) {
            for( DistensionPlayer p2 : m_players.values() )
                if( p2.getShipNum() == 9 && p2.getArmyID() == armyToAssist )
                    throw new TWCoreException( "Sorry, " + p2.getName() + " is already sitting at the Tactical Ops console on that army.  If you wish to assist, change to another ship." );
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
                    if( DEBUG && p.getNaturalArmyID() == p.getArmyID() )
                        m_botAction.sendSmartPrivateMessage("dugwyler", "Natural ID=new Army ID in auto return assist: " + p.getArmyID() + "  armyToAssist=" + armyToAssist );
                } else {
                    m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), name.toUpperCase() + " has finished assistance and is returning to their army." );
                    m_botAction.sendOpposingTeamMessageByFrequency(p.getNaturalArmyID(), name.toUpperCase() + " has returned to the army." );
                    if( DEBUG && p.getNaturalArmyID() == p.getArmyID() )
                        m_botAction.sendSmartPrivateMessage("dugwyler", "Natural ID=new Army ID in return assist: " + p.getArmyID() + "  armyToAssist=" + armyToAssist );
                }
                if( p.getShipNum() == 0 ) {
                    p.setAssist( -1 );
                } else {
                    p.setAssist( -1 );
                    m_botAction.setFreq(name, p.getNaturalArmyID() );
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
                    if( armySizeWeight >= 1.0f ||
                            (m_flagOwner[0] == p.getArmyID() && m_flagOwner[1] == p.getArmyID() && flagTimer != null && flagTimer.isRunning()) ) {
                        // Kill participation if return assist was not all that needed, and
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
            if( assistArmyWeightAfterChange < ASSIST_WEIGHT_IMBALANCE )
                throw new TWCoreException( "Assisting with your current ship will only continue the imbalance!  First pilot a lower-ranked ship if you want to !assist." );
            if( m_flagOwner[0] == armyToAssist && m_flagOwner[1] == armyToAssist )
                throw new TWCoreException( "While army strengths are imbalanced, that army seems to be doing fine as far as winning the battle goes!  Try again later." );

            m_botAction.sendPrivateMessage( name, "Now an honorary pilot of " + assistArmy.getName().toUpperCase() + ".  Use !assist to return to your army when you would like." );
            if( p.getShipNum() != 0 ) {
                if( System.currentTimeMillis() > lastAssistReward + ASSIST_REWARD_TIME ) {
                    lastAssistReward = System.currentTimeMillis();
                    int reward = p.getRank();
                    if( reward == 0 ) reward = 1;

                    // Increased bonuses for higher ranks, as it takes more to make a dent in
                    // their to-rank amounts
                    if( reward >= 70 )
                        reward *= 10;
                    else if( reward >= 60 )
                        reward *= 8;
                    else if( reward >= 50 )
                        reward *= 5;
                    else if( reward >= 40 )
                        reward *= 3;
                    else if( reward >= 30 )
                        reward *= 2;

                    if( armySizeWeight < .5 )
                        reward = p.getRank() * 6;
                    else if( armySizeWeight < .6 )
                        reward = p.getRank() * 5;
                    else if( armySizeWeight < .7 )
                        reward = p.getRank() * 4;
                    else if( armySizeWeight < .8 )
                        reward = p.getRank() * 3;
                    reward = p.addRankPoints(reward,false); // Show actual amount added
                    if( m_flagOwner[0] == p.getArmyID() && m_flagOwner[1] == p.getArmyID() &&
                            flagTimer != null && flagTimer.isRunning() &&
                            !flagTimer.isBeingBroken() ) {
                        if( flagTimer.getSecondsHeld() >= 35 ) {
                            reward *= 2;
                            m_botAction.sendPrivateMessage( name, "For your extremely noble assistance, HQ awards you a " + reward + " RP bonus.", 1 );
                        } else {
                            reward *= 3.5;
                            m_botAction.sendPrivateMessage( name, "SAINT BONUS!  For assisting this army in their most desperate hour, their HQ rewards you a " + reward + " RP bonus.", 1 );
                        }
                    } else {
                        m_botAction.sendPrivateMessage( name, "For your extremely noble assistance, HQ awards you a " + reward + " RP bonus.", 1 );
                    }
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
            if( DEBUG && p.getNaturalArmyID() == p.getArmyID() )
                m_botAction.sendSmartPrivateMessage("dugwyler", "Natural ID=new Army ID in assist: " + p.getArmyID() + "  armyToAssist=" + armyToAssist );
        } else {
            m_botAction.sendPrivateMessage( name, "Not overly imbalanced -- consider flying a lower-rank ship to even the battle instead! " + "(Weight: " + armySizeWeight + "; need <" + ASSIST_WEIGHT_IMBALANCE + ")" );
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
        if( p.getShipNum() == -1 )
            throw new TWCoreException( "You must !return or !enlist in an army first." );

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

        if( m_mineClearedPlayers.contains( name ) )
            throw new TWCoreException( "Well now... you've already cleared your mines once this battle.  Tough!" );
        if( p.getShipNum() != Tools.Ship.SHARK && p.getShipNum() != Tools.Ship.LEVIATHAN && p.getShipNum() != Tools.Ship.WARBIRD )
            throw new TWCoreException( "You must be in a mine-laying ship in order for me to clear your mines.  Didn't think I'd have to tell you!" );
        if( p.isRespawning() )
            throw new TWCoreException( "You can't reset your mines while rearming." );

        p.setIgnoreShipChanges(true);
        if( p.getShipNum() == Tools.Ship.WARBIRD )
            m_botAction.setShip( name, 2 );
        else
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
            m_botAction.sendPrivateMessage( name, "You must !return before you !lagout.  Attempting to !return you automatically.  NOTE: If you were DC'd, you will not be able to !lagout." );
            cmdReturn( name, msg );
            return;
        }
        if( p.getShipNum() != 0 )
            throw new TWCoreException( "You're already in the battle!" );

        if( flagTimer != null && flagTimer.isRunning() ) {
            Integer lagoutTime = m_lagouts.get( name );
            if( p.isAtMaxLagouts() )
                throw new TWCoreException( "Sorry, you may only lagout " + LAGOUTS_ALLOWED + " times per round." );
            if( !p.canUseLagout() || lagoutTime == null )
                throw new TWCoreException( "Sorry, I have no record of you having lagged out (you may not use !lagout after a disconnect)." );
            if( lagoutTime + LAGOUT_VALID_SECONDS < flagTimer.getTotalSecs() )
                throw new TWCoreException( "Sorry, it's been too long.  You'll have to pilot the normal way." );

            p.setIgnoreShipChanges(true);

            p.setShipNum( p.getLastShipNum() );
            if( !p.getCurrentShipFromDB() ) {
                m_botAction.sendPrivateMessage( name, "Error getting back in with that ship!  Please contact a mod." );
                p.setShipNum( 0 );
                return;
            }
            for( DistensionArmy a : m_armies.values() )
                a.recalculateFigures();
            m_botAction.setShip(p.getArenaPlayerID(), p.getLastShipNum());
            p.putInCurrentShip();
            p.prizeUpgradesNow();
            m_lagouts.remove( name );
            p.incrementLagouts();
            if( !flagTimeStarted || stopFlagTime ) {
                checkFlagTimeStart();
            }
            if( m_playerTimes.get( name ) == null ) {
                m_playerTimes.put( name, new Integer( flagTimer.getTotalSecs() ) );
                m_botAction.sendPrivateMessage( name, "No record of you in this battle ... starting your participation from scratch." );
            } else
                m_botAction.sendPrivateMessage( name, "You have safely returned to your army." );
            cmdProgress( name, null );
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

    /**
     * Toggles summoning if not a Terr, and summons players if in a Terr.
     * @param name
     * @param msg
     */
    public void cmdSummon( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 5 ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Allow Terrier Summoning: " + (p.toggleSummon() ? "[ENABLED]" : "[DISABLED]") );
            return;
        }

        // Doing a Terr summon: Officers and Flag Officers get extras for this
        if( flagTimer == null || !flagTimer.isRunning() )
            throw new TWCoreException( "There is no need to issue a summon order while not in battle!" );

        int permissions = p.getPurchasedUpgrade(14);
        if( p.getBattlesWon() >= WINS_REQ_OFFICER )
            permissions++;
        if( p.getBattlesWon() >= WINS_REQ_FLAG_OFFICER )
            permissions++;
        if( permissions == 0 )
            throw new TWCoreException( "You do not yet have summoning order permissions.  You may upgrade them, and will also have additional permissions on reaching Officer and Flag Officer." );

        long timeUntilNext = p.timeUntilNextSummon( permissions );
        if( timeUntilNext > 0 )
            throw new TWCoreException( "You may not issue a summon order for another " + getTimeString( timeUntilNext ) + "." );

        boolean summonOnlySharks = false;
        if( msg.equalsIgnoreCase("s") ) {
            if( permissions < 2 )
                throw new TWCoreException( "You do not yet have the appropriate permissions to summon Sharks." );
            summonOnlySharks = true;
        }

        Player playerObj = m_botAction.getPlayer( p.getArenaPlayerID() );
        if( playerObj == null )
            return;

        int targets = 0;
        for( DistensionPlayer target : m_players.values() ) {
            if( target.getArmyID() == p.getArmyID() && target.getShipNum() != 5 && target.getShipNum() != 9 ) {
                Player targetObj = m_botAction.getPlayer( target.getArenaPlayerID() );
                if( targetObj != null &&
                        targetObj.getYTileLocation() > TOP_FR &&
                        targetObj.getYTileLocation() < BOT_FR ) {
                    if( (!summonOnlySharks && target.getShipNum() != Tools.Ship.SHARK) ||
                        (summonOnlySharks && target.getShipNum() == Tools.Ship.SHARK) ) {
                        if( !target.isRespawning() || permissions == 3 ) {
                            if( target.doesAllowSummon() ) {
                                m_botAction.warpTo( targetObj.getPlayerID(), playerObj.getXTileLocation(), playerObj.getYTileLocation() );
                                target.respawnImmediately();
                                targets++;
                            }
                        }
                    }
                }
            }
        }
        if( targets == 0 ) {
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "No " + (summonOnlySharks ? "Sharks" : "assault pilots") + " available; summon order not issued." );
        } else {
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), targets + (summonOnlySharks ? " Shark" : " assault pilot") + (targets > 1 ? "s":"") +
                    " summoned.  Next summon in " + getTimeString( p.useSummon(permissions) ) + "." );
        }
    }

    /**
     * Uses energy tank ability, if available.
     * @param name
     * @param msg
     */
    public void cmdEnergyTank( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() == -1 )
            throw new TWCoreException( "You must !return first." );
        if( p.getShipNum() == 0 )
            throw new TWCoreException( "You must pilot your ship first." );
        if( p.getShipNum() != Tools.Ship.SPIDER )
            throw new TWCoreException( "Only Spiders possess the Energy Tank ability." );
        p.useEnergyTank();
    }

    /**
     * Uses Targeted EMP ability, if available.
     * @param name
     * @param msg
     */
    public void cmdTargetedEMP( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() == -1 )
            throw new TWCoreException( "You must !return first." );
        if( p.getShipNum() == 0 )
            throw new TWCoreException( "You must pilot your ship first." );
        if( p.getShipNum() != Tools.Ship.TERRIER )
            throw new TWCoreException( "Only Terriers possess the Targeted EMP ability." );
        if( !p.useTargetedEMP() )
            throw new TWCoreException( "You have no EMP charged.  Average recharge time: 15 minutes." );
        int freq = p.getArmyID();
        for( DistensionPlayer p3 : m_players.values() ) {
            if( p3.getArmyID() != freq ) {
                m_botAction.specificPrize( p3.getArenaPlayerID(), Tools.Prize.ENERGY_DEPLETED );
                m_botAction.specificPrize( p3.getArenaPlayerID(), Tools.Prize.ENGINE_SHUTDOWN );
            }
        }
        m_botAction.hideObjectForPlayer( p.getArenaPlayerID(), LVZ_EMP );
        m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), p.getName() + " unleashed an ELECTRO-MAGNETIC PULSE on the enemy!" );
        m_botAction.sendOpposingTeamMessageByFrequency(p.getOpposingArmyID(), p.getName() + " unleashed an ELECTRO-MAGNETIC PULSE on your army!" );
    }

    /**
     * Uses JumpSpace ability, if available.
     * @param name
     * @param msg
     */
    public void cmdJumpSpace( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        m_botAction.spectatePlayerImmediately( p.getArenaPlayerID() );
        if( p.getShipNum() != Tools.Ship.JAVELIN )
            throw new TWCoreException( "Only Javelins possess the JumpSpace ability." );
        if( p.getRank() < 15 )
            throw new TWCoreException( "You do not yet have access to the JumpSpace Drive.  It will become available when you reach rank 15." );

        int modx = 0;
        int mody = 0;

        if( msg.equals("N") )
            mody = -1;
        else if( msg.equals("S") )
            mody = 1;
        else if( msg.equals("W") )
            modx = -1;
        else if( msg.equals("E") )
            modx = 1;
        else if( msg.equals("NW") ) {
            mody = -1;
            modx = -1;
        } else if( msg.equals("NE") ) {
            mody = -1;
            modx = 1;
        } else if( msg.equals("SE") ) {
            mody = 1;
            modx = 1;
        } else if( msg.equals("SW") ) {
            mody = 1;
            modx = -1;
        } else {
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "Evasive Jump initiated!  (Provide a direction to jump to a specific location, i.e.: >>> NW)" );
            // Provide random values between -1 and 1 to give a random direction
            mody = (int)Math.round( ((Math.random() * 3.0f) - 1.5f) );
            mody = (int)Math.round( ((Math.random() * 3.0f) - 1.5f) );
        }

        Player pdata = m_botAction.getPlayer(p.getArenaPlayerID());

        if( !p.useJumpSpace() )
            throw new TWCoreException( "JumpSpace is not yet charged." );

        // upgfactor: 0.5 to 2.0, representing amount by which velocity will affect jump location.
        // Estimating velocity to be between 1 and ~3000 based on present CFG, with
        // top velocity w/o speed upgrades or afterburner being 900.
        // Divided by 200:  x0.5) 900 = 2.25; 3000 = 7.5   x2.0) 900 = 9; 3000 = 30
        //float upgfactor = (float)(p.getPurchasedUpgrade(9) + 1) / 2.0f;
        //int jumpx = x + Math.round( ((float)pdata.getXVelocity() / 200.0f ) * upgfactor );
        //int jumpy = y + Math.round( ((float)pdata.getYVelocity() / 200.0f ) * upgfactor );

        // New method: flat amount
        int upgfactor = (p.getPurchasedUpgrade(9) + 1) * 8;
        int x = pdata.getXTileLocation();
        int y = pdata.getYTileLocation();

        // Add direction
        int jumpx = x + (modx * upgfactor);
        int jumpy = y + (mody * upgfactor);

        if( DEBUG )
            m_botAction.sendPrivateMessage("dugwyler", "Jump: " + name + "  ("+ x + "," + y + ") -> (" + jumpx + "," + jumpy + ") Factor=" + upgfactor );
                    /*
                    "  Vel:" + pdata.getXVelocity() + "," + pdata.getYVelocity() + "  Factor=" + upgfactor + "  " +
                    "XAdd=" + Math.round( ((float)pdata.getXVelocity() / 200.0f ) * upgfactor ) +
                    " YAdd=" + Math.round( ((float)pdata.getYVelocity() / 200.0f ) * upgfactor ) ); */

        // Search for Naughtiness (those going outside arena limits)
        final int arenaRadius = 4192;
        // Formula for getting the distance from the center borrowed from 2dragons' fallout module.
        double dist = Math.sqrt( Math.pow(( 8192 - (jumpx * 16) ), 2) + Math.pow(( 8192 - (jumpy * 16) ), 2) );
        if( dist >= arenaRadius ) {
            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "Naughty!", Tools.Sound.BURP );
            return;
        }

        // Levels 0, 1 and 2 all have negative (and cumulative) effects.  3 has none.
        switch( p.getPurchasedUpgrade(9) ) {
            case 0:
                m_botAction.specificPrize( p.getArenaPlayerID(), Tools.Prize.ENGINE_SHUTDOWN );
            case 1:
                m_botAction.specificPrize( p.getArenaPlayerID(), Tools.Prize.ENERGY_DEPLETED );
            case 2:
                m_botAction.specificPrize( p.getArenaPlayerID(), -Tools.Prize.RECHARGE );
        }
        m_botAction.warpTo( p.getArenaPlayerID(), jumpx, jumpy );
        m_botAction.hideObjectForPlayer( p.getArenaPlayerID(), LVZ_JUMPSPACE );
    }

    /**
     * Uses Prismatic Array ability, if available.
     * @param name
     * @param msg
     */
    public void cmdPrismaticArray( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() == -1 )
            throw new TWCoreException( "You must !return first." );
        if( p.getShipNum() == 0 )
            throw new TWCoreException( "You must pilot your ship first." );
        if( p.getShipNum() != Tools.Ship.WEASEL )
            throw new TWCoreException( "Only Weasels possess the Prismatic Array ability." );
        p.usePrismaticArray();
    }



    // ***** TACTICAL OPS COMMANDS *****

    // Wrappers for the ops nav ability
    public void cmdOpsNav1(String name,String msg) { cmdOpsNav(name,1); }
    public void cmdOpsNav11(String name,String msg) { cmdOpsNav(name,11); }
    public void cmdOpsNav12(String name,String msg) { cmdOpsNav(name,12); }
    public void cmdOpsNav13(String name,String msg) { cmdOpsNav(name,13); }
    public void cmdOpsNav2(String name,String msg) { cmdOpsNav(name,2); }
    public void cmdOpsNav21(String name,String msg) { cmdOpsNav(name,21); }
    public void cmdOpsNav22(String name,String msg) { cmdOpsNav(name,22); }
    public void cmdOpsNav3(String name,String msg) { cmdOpsNav(name,3); }

    /**
     * Sends Ops to a particular spot on the map.
     * @param name
     * @param msg
     */
    public void cmdOpsNav( String name, int spot ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        if( p.getArmyID() % 2 == 0 ) {
            switch( spot ) {
                case 1:   m_botAction.warpTo( p.getArenaPlayerID(), 512, TOP_FR_NAV ); break;
                case 11:  m_botAction.warpTo( p.getArenaPlayerID(), 512, TOP_MID_NAV ); break;
                case 12:  m_botAction.warpTo( p.getArenaPlayerID(), 512, TOP_TUBE_NAV ); break;
                case 13:  m_botAction.warpTo( p.getArenaPlayerID(), 512, REARM_AREA_TOP_Y ); break;
                case 2:   m_botAction.warpTo( p.getArenaPlayerID(), 512, BOT_FR_NAV ); break;
                case 21:  m_botAction.warpTo( p.getArenaPlayerID(), 512, BOT_MID_NAV ); break;
                case 22:  m_botAction.warpTo( p.getArenaPlayerID(), 512, BOT_TUBE_NAV ); break;
                case 3:   m_botAction.warpTo( p.getArenaPlayerID(), 512, 512 ); break;
            }
        } else {
            switch( spot ) {
                case 1:   m_botAction.warpTo( p.getArenaPlayerID(), 512, BOT_FR_NAV ); break;
                case 11:  m_botAction.warpTo( p.getArenaPlayerID(), 512, BOT_MID_NAV ); break;
                case 12:  m_botAction.warpTo( p.getArenaPlayerID(), 512, BOT_TUBE_NAV ); break;
                case 13:  m_botAction.warpTo( p.getArenaPlayerID(), 512, REARM_AREA_BOTTOM_Y ); break;
                case 2:   m_botAction.warpTo( p.getArenaPlayerID(), 512, TOP_FR_NAV ); break;
                case 21:  m_botAction.warpTo( p.getArenaPlayerID(), 512, TOP_MID_NAV ); break;
                case 22:  m_botAction.warpTo( p.getArenaPlayerID(), 512, TOP_TUBE_NAV ); break;
                case 3:   m_botAction.warpTo( p.getArenaPlayerID(), 512, 512 ); break;
            }
        }
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub
    }

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
     * @return True if the command was successful.
     */
    public boolean cmdOpsMsg( String name, String msg, int armyIDSpoof ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return false;
        if( p.getShipNum() != 9 ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "You must be at the Tactical Ops station to do this." );
            return false;
        }
        if( p.getPurchasedUpgrade(3) < 1 ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Your communication systems must receive the proper !upgrade before you can use this ability." );
            return false;
        }
        int msgNum = 0;
        if( Tools.isAllDigits(msg) ) {
            msgNum = Integer.parseInt(msg);
            if( msgNum < 1 || msgNum > 4 ) {
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Please provide an appropriate message number (1-4).  Use '!opshelp msg' for assistance." );
                return false;
            }
        } else {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Please provide a message number (1-4).  Use '!opshelp msg' for assistance." );
            return false;
        }
        if( p.getCurrentComms() < 1 ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "You need a communication authorization to send out a message.  (+1 every minute)" );
            return false;
        }
        p.adjustComms(-1);
        boolean falsify = (armyIDSpoof != -1);

        if( msgNum == 1 || msgNum == 2 ) {
            int friendlies = 0;
            int foes = 0;
            String bestTerr = null;
            int bestTerrLoc = 0;
            if( falsify ) {
                // Falsify it to look as though help is needed
                int total = m_botAction.getNumPlaying();
                friendlies = (int)(Math.random() * (total / 4));
                foes = (int)(Math.random() * (total / 2));
            } else {
                for( Player p2 : m_botAction.getPlayingPlayers() ) {
                    if( p2.getYTileLocation() > (msgNum==1 ? TOP_ROOF : BOT_MID)
                            && p2.getYTileLocation() < (msgNum==1 ? TOP_MID  : BOT_ROOF) ) {
                        if( p2.getFrequency() == p.getArmyID() ) {
                            if( p2.getShipType() == Tools.Ship.TERRIER ) {
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
                            foes++;
                        }
                    }
                }
            }
            int freq = (falsify ? armyIDSpoof : p.getArmyID() );
            int diff = Math.abs(friendlies - foes);
            String diffText;
            if( diff == 0 ) {
                diffText = "Even: " + friendlies + "v" + foes;
            } else {
                if( friendlies > foes ) {
                    diffText = "We have +" + diff + ": " + friendlies + "v" + foes;
                } else {
                    diffText = "They have +" + diff + ": " + friendlies + "v" + foes;
                }
            }
            String messageToSend =
                "OPS: " + (m_flagOwner[0] == freq ? "HELP @" : "ATTACK @") +
                (msgNum==1 ? " TOP" : " BOTTOM" ) +
                (bestTerr != null ?
                        " ->  Attach to " + bestTerr + " (" + getLocation(bestTerrLoc) + ")" : "->  ... need Terrier!" ) +
                "   (" + diffText + ")";
            m_botAction.sendOpposingTeamMessageByFrequency( freq, messageToSend );
            if( falsify )
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Sent to enemy:  " + messageToSend );
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
            String messageToSend =
                "OPS (to Army):  Requesting 1 additional " +
                (msgNum==3 ? "TERRIER" : "SHARK" ) + " pilot.  (Currently have " + searchship + ")";
            m_botAction.sendOpposingTeamMessageByFrequency( freq, messageToSend);
            if( falsify )
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Sent to enemy:  " + messageToSend );
        }
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub
        return true;
    }

    /**
     * Wrapper for cmdOpsPM(String,String,int)
     */
    public void cmdOpsPM( String name, String msg ) {
        cmdOpsPM( name, msg, -1 );
    }

    /**
     * Sends out a tactical ops PM via the bot.
     * @param name
     * @param msg
     * @param armyIDSpoof Army ID to spoof; -1 if not sabotage
     * @return True if the command was successful.
     */
    public boolean cmdOpsPM( String name, String msg, int armyIDSpoof ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return false;
        if( p.getShipNum() != 9 ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "You must be at the Tactical Ops station to do this." );
            return false;
        }
        if( p.getPurchasedUpgrade(3) < 2 ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Your communication systems must receive the proper !upgrade before you can use this ability." );
            return false;
        }
        int msgNum = 0;
        String[] params = msg.split(":", 2);
        String target;
        boolean falsify = (armyIDSpoof != -1);
        int freq = (falsify ? armyIDSpoof : p.getArmyID() );

        if( params[0].equalsIgnoreCase("T") || params[0].equalsIgnoreCase("S") )
            target = params[0];
        else {
            Player p2 = m_botAction.getPlayer(params[0]);
            if( p2 == null ) {
                p2 = m_botAction.getFuzzyPlayer(params[0]);
            }
            if( p2 == null ) {
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Can't find pilot '" + params[0] + "'." );
                return false;
            } else {
                if( p2.getFrequency() != freq ) {
                    m_botAction.sendPrivateMessage( p.getArenaPlayerID(), p2.getPlayerName() + " isn't on the army!" );
                    return false;
                }
                target = p2.getPlayerName();
            }
        }
        if( Tools.isAllDigits(params[1]) ) {
            msgNum = Integer.parseInt(params[1]);
            if( msgNum < 1 || msgNum > 2 ) {
                m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Please provide an appropriate message number (1 or 2).  Use '!opshelp msg' for assistance." );
                return false;
            }
        } else {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Please provide a message number (1 or 2).  Use '!opshelp msg' for assistance." );
            return false;
        }
        if( p.getCurrentComms() < 1 ) {
            m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "You need a communication authorization to send out a message.  (+1 every minute)" );
            return false;
        }
        p.adjustComms(-1);

        int friendlies = 0;
        int foes = 0;
        int friendTerr = 0;
        int foeTerr = 0;
        String bestTerr = null;
        int bestTerrLoc = 0;
        LinkedList <String>targetNames = new LinkedList<String>();
        if( falsify ) {
            // Falsify it to look as though help is needed
            int total = m_botAction.getNumPlaying();
            friendlies = (int)(Math.random() * (total / 4));
            foes = (int)(Math.random() * (total / 2));
            foeTerr = (int)(Math.random() * 2);
            if( target.equalsIgnoreCase("T") || target.equalsIgnoreCase("S")) {
                for( Player p2 : m_botAction.getPlayingPlayers() ) {
                    if( p2.getFrequency() == armyIDSpoof ) {
                        if( (p2.getShipType() == Tools.Ship.TERRIER && target.equals("T")) ||
                                (p2.getShipType() == Tools.Ship.SHARK   && target.equals("S")) ) {
                            targetNames.add(p2.getPlayerName());
                        }
                    }
                }
            } else {
                targetNames.add( target );
            }
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
                            if( target.equalsIgnoreCase("T") )
                                targetNames.add(p2.getPlayerName());
                        } else if( p2.getShipType() == Tools.Ship.SHARK && target.equalsIgnoreCase("S") ) {
                            targetNames.add(p2.getPlayerName());
                        }
                        friendlies++;
                    } else {
                        if( p2.getShipType() == Tools.Ship.TERRIER )
                            foeTerr++;
                        foes++;
                    }
                }
            }
            if( !( target.equalsIgnoreCase("T") || target.equalsIgnoreCase("S") ) )
                targetNames.add( target );
        }

        String targetPrefix;
        if( target.equalsIgnoreCase("T") )
            targetPrefix = "to all Terrs";
        else if( target.equalsIgnoreCase("S") )
            targetPrefix = "to all Sharks";
        else
            targetPrefix = "privately to you";

        String messageToSend =
            "OPS (" + targetPrefix + "):  Your immediate assistance required to " +
            (m_flagOwner[0] == freq ? "defend" : "secure") +
            (msgNum==1 ? " TOP BASE" : " BOTTOM BASE" ) +
            "!!  Friend: " + friendlies + " (" + friendTerr + " Terr) " +
            "-vs- Enemy: " + foes + " (" + foeTerr + " Terr) ... " +
            ( !target.equalsIgnoreCase("T") && bestTerr != null ?
                    "Attach to: " + bestTerr + " (" + getLocation(bestTerrLoc) + ")" :
            "[No Terr found]");

        for( String pmName : targetNames )
            m_botAction.sendPrivateMessage( pmName, messageToSend );
        m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "Sent" + (falsify ? " to enemy":"") + ":  " + messageToSend );
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub

        return true;
    }

    /**
     * Sends out a tactical ops sabotage msg via the bot.
     * @param name
     * @param msg
     */
    public void cmdOpsSab( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        if( p.getPurchasedUpgrade(3) < 3 )
            throw new TWCoreException( "Your communication systems must receive the proper !upgrade before you can use this ability." );
        if( p.getCurrentComms() < 2 )
            throw new TWCoreException( "You need 2 communication authorizations to send a sabotaged message.  (+1 every minute)" );
        String[] params = msg.split(" ", 2);
        boolean success;
        if( !( params[0].equalsIgnoreCase("msg") || params[0].equalsIgnoreCase("pm") ) ) {
            throw new TWCoreException( "You must choose either msg or PM.  Example: !opssab pm t:2" );
        } else if( params[0].equalsIgnoreCase("msg") ) {
            success = cmdOpsMsg( name, params[1], p.getOpposingArmyID() );
        } else {
            success = cmdOpsPM( name, params[1], p.getOpposingArmyID() );
        }

        // The sabotage fee (the other half is charged by the method called)
        if( success )
            p.adjustComms(-1);
    }

    /**
     * Sends out a radar sweep (shows ship breakdown).
     * @param name
     * @param msg
     */
    public void cmdOpsRadar( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        if( p.getCurrentComms() < 1 )
            throw new TWCoreException( "You need 1 communication authorization to send out a radar sweep.  (+1 every minute)" );
        p.adjustComms(-1);

        int y, index;
        int fships[] = new int[10];
        int fterrs[] = new int[10];
        int fsharks[] = new int[10];
        int eships[] = new int[10];
        int eterrs[] = new int[10];
        int esharks[] = new int[10];
        for( int i = 0; i<10; i++ )
            fships[i]=fterrs[i]=eships[i]=eterrs[i]=0;

        for( Player p2 : m_botAction.getPlayingPlayers() ) {
            y = p2.getYTileLocation();
            if( y <= TOP_SAFE || y >= BOT_SAFE )
                index = 0;
            else if( y <= TOP_ROOF )
                index = 1;
            else if( y <= TOP_FR )
                index = 2;
            else if( y <= TOP_MID )
                index = 3;
            else if( y <= TOP_LOW )
                index = 4;
            else if( y >= BOT_ROOF )
                index = 5;
            else if( y >= BOT_FR )
                index = 6;
            else if( y >= BOT_MID )
                index = 7;
            else if( y >= BOT_LOW )
                index = 8;
            else index = 9; // Neutral zone
            if( p2.getFrequency() == p.getArmyID() )
                if( p2.getShipType() == Tools.Ship.TERRIER )
                    fterrs[index]++;
                else
                    if( p2.getShipType() == Tools.Ship.SHARK )
                        fsharks[index]++;
                    else
                        fships[index]++;
            else
                if( p2.getShipType() == Tools.Ship.TERRIER )
                    eterrs[index]++;
                else
                    if( p2.getShipType() == Tools.Ship.SHARK )
                        esharks[index]++;
                    else
                        eships[index]++;
        }
        LinkedList <String>display = new LinkedList<String>();
        display.add( "|Location   |OurShip#|OurTerr#|OurShrk#|NMEShip#|NMETerr#|NMEShrk#|" );
        display.add( "|-----------|--------|--------|--------|--------|--------|--------|" );
        display.add( "|Top Roof   |" + makeBar( fships[1], 8) + "|" + makeBar( fterrs[1], 8) + "|" +
                                       makeBar( fsharks[1], 8) + "|" +
                                       makeBar( eships[1], 8) + "|" + makeBar( eterrs[1], 8) + "|" + makeBar( esharks[1], 8) + "|" );
        display.add( "|Top FR     |" + makeBar( fships[2], 8) + "|" + makeBar( fterrs[2], 8) + "|" +
                                       makeBar( fsharks[2], 8) + "|" +
                                       makeBar( eships[2], 8) + "|" + makeBar( eterrs[2], 8) + "|" + makeBar( esharks[2], 8) + "|" );
        display.add( "|Top Mid    |" + makeBar( fships[3], 8) + "|" + makeBar( fterrs[3], 8) + "|" +
                                       makeBar( fsharks[3], 8) + "|" +
                                       makeBar( eships[3], 8) + "|" + makeBar( eterrs[3], 8) + "|" + makeBar( esharks[3], 8) + "|" );
        display.add( "|Top Low    |" + makeBar( fships[4], 8) + "|" + makeBar( fterrs[4], 8) + "|" +
                                       makeBar( fsharks[4], 8) + "|" +
                                       makeBar( eships[4], 8) + "|" + makeBar( eterrs[4], 8) + "|" + makeBar( esharks[4], 8) + "|" );
        display.add( "|No-Man's   |" + makeBar( fships[9], 8) + "|" + makeBar( fterrs[9], 8) + "|" +
                                       makeBar( fsharks[9], 8) + "|" +
                                       makeBar( eships[9], 8) + "|" + makeBar( eterrs[9], 8) + "|" + makeBar( esharks[9], 8) + "|" );
        display.add( "|Bottom Low |" + makeBar( fships[8], 8) + "|" + makeBar( fterrs[8], 8) + "|" +
                                       makeBar( fsharks[8], 8) + "|" +
                                       makeBar( eships[8], 8) + "|" + makeBar( eterrs[8], 8) + "|" + makeBar( esharks[8], 8) + "|" );
        display.add( "|Bottom Mid |" + makeBar( fships[7], 8) + "|" + makeBar( fterrs[7], 8) + "|" +
                                       makeBar( fsharks[7], 8) + "|" +
                                       makeBar( eships[7], 8) + "|" + makeBar( eterrs[7], 8) + "|" + makeBar( esharks[7], 8) + "|" );
        display.add( "|Bottom FR  |" + makeBar( fships[6], 8) + "|" + makeBar( fterrs[6], 8) + "|" +
                                       makeBar( fsharks[6], 8) + "|" +
                                       makeBar( eships[6], 8) + "|" + makeBar( eterrs[6], 8) + "|" + makeBar( esharks[6], 8) + "|" );
        display.add( "|Bottom Roof|" + makeBar( fships[5], 8) + "|" + makeBar( fterrs[5], 8) + "|" +
                                       makeBar( fsharks[5], 8) + "|" +
                                       makeBar( eships[5], 8) + "|" + makeBar( eterrs[5], 8) + "|" + makeBar( esharks[5], 8) + "|" );
        display.add( "|(Rearming) |" + makeBar( fships[0], 8) + "|" + makeBar( fterrs[0], 8) + "|" +
                                       makeBar( fsharks[0], 8) + "|" +
                                       makeBar( eships[0], 8) + "|" + makeBar( eterrs[0], 8) + "|" + makeBar( esharks[0], 8) + "|" );
        m_botAction.privateMessageSpam( p.getArenaPlayerID(), display );
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub
    }

    /**
     * Uses fast respawn ability, allowing entire team to respawn faster than the other
     * team for a certain amount of time (unless, of course, the other team also uses
     * the ability).
     * @param name
     * @param msg
     */
    public void cmdOpsRearm( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        if( (p.getArmyID() == 0 && m_army0_fastRearm) || (p.getArmyID() == 1 && m_army1_fastRearm) )
            throw new TWCoreException( "Already using a fast rearm -- we can't rearm any faster!" );
        if( p.getPurchasedUpgrade(4) < 1 )
            throw new TWCoreException( "You are not yet able to use this ability -- first you must install the appropriate !upgrade." );
        if( p.getCurrentOP() < 2 )
            throw new TWCoreException( "Insufficient OP; need 2.  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );
        p.adjustOP(-2);
        int time;
        if( p.getPurchasedUpgrade(4) == 1 )
            time = 20 * Tools.TimeInMillis.SECOND;
        else if ( p.getPurchasedUpgrade(4) == 2 )
            time = 40 * Tools.TimeInMillis.SECOND;
        else
            time = 60 * Tools.TimeInMillis.SECOND;

        if( p.getArmyID() == 0 ) {
            if( m_army0_fastRearmTask != null ) {
                try {
                    m_army0_fastRearmTask.cancel();
                } catch (Exception e) {}
            }
            m_army0_fastRearmTask = new RearmTask();
            m_army0_fastRearmTask.setID( p.getArenaPlayerID() );
            m_army0_fastRearmTask.setTeam(0);
            m_botAction.scheduleTask( m_army0_fastRearmTask, time );
            m_army0_fastRearm = true;
        } else {
            if( m_army1_fastRearmTask != null ) {
                try {
                    m_army1_fastRearmTask.cancel();
                } catch (Exception e) {}
            }
            m_army1_fastRearmTask = new RearmTask();
            m_army1_fastRearmTask.setID( p.getArenaPlayerID() );
            m_army1_fastRearmTask.setTeam(1);
            m_botAction.scheduleTask( m_army1_fastRearmTask, time );
            m_army1_fastRearm = true;
        }
        m_botAction.sendOpposingTeamMessageByFrequency( p.getArmyID(), "OPS used FAST REARM: Enabled for the next " + (time / 1000) + " seconds." );
        m_botAction.showObjectForPlayer( p.getArenaPlayerID(), LVZ_OPS_FAST_REARM );
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub
    }

    /**
     * Allows ops to control various doors on the map.  Tube or sides cost 1, FR entrance or flag cost 2,
     * and all enemy doors cost 3 OP to close.
     * @param name
     * @param msg
     */
    public void cmdOpsDoor( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        int upgLevel = p.getPurchasedUpgrade(6);
        if( upgLevel < 1 )
            throw new TWCoreException( "You are not yet able to use this ability -- first you must install the appropriate !upgrade." );

        Integer doorNum;
        try {
            doorNum = Integer.parseInt(msg);
        } catch ( NumberFormatException e ) {
            throw new TWCoreException( "Please specify a door # between 1 and 8.  Use !opshelp for more info." );
        }
        if( doorNum < 1 || doorNum > 8 )
            throw new TWCoreException( "Please specify a door # between 1 and 8.  Use !opshelp for more info." );
        if( (upgLevel == 1 && doorNum > 2) ||
            (upgLevel <= 2 && doorNum > 4) )
            throw new TWCoreException( "You are not able to control that door at your level of clearance.  Consider spending more points on door operations training, if available." );
        if( (p.getCurrentOP() < 4) ||
            (p.getCurrentOP() < 6 && doorNum > 2) ||
            (p.getCurrentOP() < 8 && doorNum > 4) )
            throw new TWCoreException( "Insufficient OP.  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );
        if( doorNum > 4 )
            p.adjustOP(-8);
        else if( doorNum > 2 )
            p.adjustOP(-6);
        else
            p.adjustOP(-4);
        int time;
        switch( p.getPurchasedUpgrade(6) ) {
            case 1:
                time = 10000;
                break;
            case 2:
                time = 15000;
                break;
            default:
                time = 20000;
                break;
        }

        String doorName;
        int realDoorNum = doorNum;
        // Translate door #s if it's a top army
        if( p.getArmyID() % 2 == 1 ) {
            if( doorNum > 4 ) {
                doorName = "TOP ";
                realDoorNum = doorNum - 4;
            } else {
                doorName = "BOTTOM ";
                realDoorNum = doorNum + 4;
            }
        } else {
            if( doorNum > 4 ) {
                doorName = "BOTTOM ";
            } else {
                doorName = "TOP ";
            }
        }
        m_botAction.setDoors((int) Math.pow(2, (realDoorNum - 1)));

        if( m_doorOffTask != null ) {
            try {
                m_doorOffTask.cancel();
            } catch (Exception e) {}
        }
        m_doorOffTask = new TimerTask() {
            public void run() {
                m_botAction.setDoors(0);
            }
        };
        m_botAction.scheduleTask( m_doorOffTask, time );
        switch( doorNum ) {
            case 1:
            case 5:
                doorName += "SIDE";
                break;
            case 2:
            case 6:
                doorName += "TUBE";
                break;
            case 3:
            case 7:
                doorName += "FR ENTRANCE";
                break;
            case 4:
            case 8:
                doorName += "FLAG";
                break;
        }
        m_botAction.sendOpposingTeamMessageByFrequency( p.getArmyID(), "OPS closed " + doorName + " GATES for the next " + (time / 1000) + " seconds." );
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub
    }

    /**
     * Allows Ops to deply cover at left, right, before FR, over the flag, in the tube, and at the
     * lower base entrance.
     * @param name
     * @param msg
     */
    public void cmdOpsCover( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        int upgLevel = p.getPurchasedUpgrade(5);
        if( upgLevel < 1 )
            throw new TWCoreException( "You are not yet able to use this ability -- first you must install the appropriate !upgrade." );

        Integer coverNum;
        try {
            coverNum = Integer.parseInt(msg);
        } catch ( NumberFormatException e ) {
            throw new TWCoreException( "Please specify a number between 1 and 6.  Use !opshelp for more info." );
        }
        if( coverNum < 1 || coverNum > 6 )
            throw new TWCoreException( "Please specify a number between 1 and 6.  Use !opshelp for more info." );
        if( p.getCurrentOP() < 1 )
            throw new TWCoreException( "Insufficient OP; need 1.  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );
        p.adjustOP(-1);

        String coverName;
        if( p.getArmyID() % 2 == 0 ) {
            coverName = "TOP, ";
            m_botAction.showObject( LVZ_OPS_COVER_TOP_FIRST + coverNum - 1 );
        } else {
            coverName = "BOTTOM, ";
            m_botAction.showObject( LVZ_OPS_COVER_BOT_FIRST + coverNum - 1 );
        }

        switch( coverNum ) {
            case 1:
                coverName += "LEFT SIDE";
                break;
            case 2:
                coverName += "RIGHT SIDE";
                break;
            case 3:
                coverName += "FR ENTRANCE";
                break;
            case 4:
                coverName += "OVER FLAG";
                break;
            case 5:
                coverName += "IN TUBE";
                break;
            case 6:
                coverName += "LOWER ENTRANCE";
                break;
        }
        m_botAction.sendOpposingTeamMessageByFrequency( p.getArmyID(), "OPS deployed COVER at " + coverName + " for 15 seconds." );
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub
    }

    /**
     * Ops ability to warp a specific player to various locations in the home base.
     * @param name
     * @param msg
     */
    public void cmdOpsWarp( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        int upgLevel = p.getPurchasedUpgrade(7);
        if( upgLevel < 1 )
            throw new TWCoreException( "You are not yet able to use this ability -- first you must install the appropriate !upgrade." );
        int warpPoint = 0;
        String[] params = msg.split(":", 2);
        int freq = p.getArmyID();
        Player p2 = m_botAction.getPlayer(params[0]);
        if( p2 == null ) {
            p2 = m_botAction.getFuzzyPlayer(params[0]);
        }
        if( p2 == null ) {
            throw new TWCoreException( "Can't find pilot '" + params[0] + "'." );
        } else {
            if( p2.getFrequency() != freq )
                throw new TWCoreException( p2.getPlayerName() + " isn't on the army!" );
            if( p2.getShipType() == Tools.Ship.SPECTATOR )
                throw new TWCoreException( p2.getPlayerName() + " isn't in a ship!" );
        }
        if( Tools.isAllDigits(params[1]) ) {
            warpPoint = Integer.parseInt(params[1]);
            if( warpPoint < 1 || warpPoint > 6 )
                throw new TWCoreException( "Please provide an appropriate warp point (1 through 6).  Use !opshelp for assistance." );
        } else
            throw new TWCoreException( "Please provide an appropriate warp point (1 through 6).  Use !opshelp for assistance." );
        if( (upgLevel == 1 && warpPoint > 3) ||
            (upgLevel <= 2 && warpPoint > 5) )
                throw new TWCoreException( "You are not able to warp to that point at your level of clearance.  Consider spending more points on warp operations training." );
        if( (p.getCurrentOP() < 2) ||
            (p.getCurrentOP() < 4 && warpPoint > 3) ||
            (p.getCurrentOP() < 10 && warpPoint == 6) )
                throw new TWCoreException( "Insufficient OP.  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );
        if( warpPoint == 6 )
            p.adjustOP(-10);
        else if( warpPoint > 3 )
            p.adjustOP(-4);
        else
            p.adjustOP(-2);
        if( freq % 2 == 0 ) {
            switch( warpPoint ) {
                case 1:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_TOP_WARP1_X, OPS_TOP_WARP1_Y );  break;
                case 2:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_TOP_WARP2_X, OPS_TOP_WARP2_Y );  break;
                case 3:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_TOP_WARP3_X, OPS_TOP_WARP3_Y );  break;
                case 4:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_TOP_WARP4_X, OPS_TOP_WARP4_Y );  break;
                case 5:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_TOP_WARP5_X, OPS_TOP_WARP5_Y );  break;
                case 6:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_TOP_WARP6_X, OPS_TOP_WARP6_Y );  break;
            }
        } else {
            switch( warpPoint ) {
                case 1:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_BOT_WARP1_X, OPS_BOT_WARP1_Y );  break;
                case 2:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_BOT_WARP2_X, OPS_BOT_WARP2_Y );  break;
                case 3:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_BOT_WARP3_X, OPS_BOT_WARP3_Y );  break;
                case 4:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_BOT_WARP4_X, OPS_BOT_WARP4_Y );  break;
                case 5:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_BOT_WARP5_X, OPS_BOT_WARP5_Y );  break;
                case 6:
                    m_botAction.warpTo(p2.getPlayerID(), OPS_BOT_WARP6_X, OPS_BOT_WARP6_Y );  break;
            }
        }
        m_botAction.sendPrivateMessage(p2.getPlayerID(), "OPS opened a WORMHOLE on your ship." );
        m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "Opened WORMHOLE on " + p2.getPlayerName() + "'s ship." );
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub
    }

    /**
     * Ops ability to cover the enemy with an orb.
     * @param name
     * @param msg
     */
    public void cmdOpsOrb( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        int upgLevel = p.getPurchasedUpgrade(8);
        if( upgLevel < 1 )
            throw new TWCoreException( "You are not yet able to use this ability -- first you must install the appropriate !upgrade." );
        if( msg.equals("") )
            msg = "all";
        if( upgLevel < 2 && msg.equals("all") )
            throw new TWCoreException( "You are not yet able to use this ability on all players in the base -- first you must install the appropriate !upgrade." );

        Player p2 = null;
        if( !msg.equals("all") ) {
            p2 = m_botAction.getPlayer( msg );
            if( p2 == null ) {
                p2 = m_botAction.getFuzzyPlayer( msg );
            }
            if( p2 == null ) {
                throw new TWCoreException( "Can't find pilot '" + msg + "'." );
            } else {
                if( p2.getFrequency() == p.getArmyID() )
                    throw new TWCoreException( p2.getPlayerName() + " is on your army!" );
                if( p2.getShipType() == Tools.Ship.SPECTATOR )
                    throw new TWCoreException( p2.getPlayerName() + " isn't in a ship!" );
            }
        }

        if( msg.equals("all") ) {
            if( p.getCurrentOP() < 6 )
                throw new TWCoreException( "Insufficient OP; need 6.  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );

            p.adjustOP(-6);

            int freq = p.getArmyID();
            for( DistensionPlayer p3 : m_players.values() ) {
                if( p3.getArmyID() != freq ) {
                    Player pobj = m_botAction.getPlayer( p3.getArenaPlayerID() );
                    if( freq % 2 == 0 ) {
                        if( pobj != null && pobj.getYLocation() > TOP_ROOF && pobj.getYLocation() < TOP_LOW )
                            m_botAction.showObjectForPlayer( pobj.getPlayerID(), LVZ_OPS_SPHERE );
                    } else {
                        if( pobj != null && pobj.getYLocation() > BOT_LOW && pobj.getYLocation() < BOT_ROOF )
                            m_botAction.showObjectForPlayer( pobj.getPlayerID(), LVZ_OPS_SPHERE );
                    }
                }
            }

            m_botAction.sendOpposingTeamMessageByFrequency(p.getOpposingArmyID(), "ENEMY OPS placed SPHERE OF SECLUSION over all your army's pilots in " + (freq % 2 == 0 ? "TOP BASE!" : "BOTTOM BASE!") );
            m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), "OPS covered all enemies in " + (freq % 2 == 0 ? "TOP BASE" : "BOTTOM BASE") + " with SPHERE OF SECLUSION!" );
        } else {
            if( p.getCurrentOP() < 2 )
                throw new TWCoreException( "Insufficient OP; need 2.  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );
            else
                p.adjustOP(-2);
            m_botAction.showObjectForPlayer( p2.getPlayerID(), LVZ_OPS_SPHERE );
            m_botAction.sendPrivateMessage(p2.getPlayerID(), "ENEMY OPS has covered you with the SPHERE OF SECLUSION!" );
            m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), "OPS covered " + p2.getPlayerName() + " with SPHERE OF SECLUSION." );
        }
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub
    }

    /**
     * Ops ability to cover the enemy with shroud of darkness.
     * @param name
     * @param msg
     */
    public void cmdOpsDark( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        int upgLevel = p.getPurchasedUpgrade(10);
        if( upgLevel < 1 )
            throw new TWCoreException( "You are not yet able to use this ability -- first you must install the appropriate !upgrade." );
        if( msg.equals("") )
            msg = "all";
        if( upgLevel < 2 && msg.equals("all") )
            throw new TWCoreException( "You are not yet able to use this ability on all players in the base -- first you must install the appropriate !upgrade." );

        Player p2 = null;
        if( !msg.equals("all") ) {
            p2 = m_botAction.getPlayer( msg );
            if( p2 == null ) {
                p2 = m_botAction.getFuzzyPlayer( msg );
            }
            if( p2 == null ) {
                throw new TWCoreException( "Can't find pilot '" + msg + "'." );
            } else {
                if( p2.getFrequency() == p.getArmyID() )
                    throw new TWCoreException( p2.getPlayerName() + " is on your army!" );
                if( p2.getShipType() == Tools.Ship.SPECTATOR )
                    throw new TWCoreException( p2.getPlayerName() + " isn't in a ship!" );
            }
        }

        if( msg.equals("all") ) {
            if( p.getCurrentOP() < 8 )
                throw new TWCoreException( "Insufficient OP; need 8.  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );

            p.adjustOP(-8);
            int lvzNum = p.getPurchasedUpgrade(10) > 3 ? LVZ_OPS_SHROUD_LG : LVZ_OPS_SHROUD_SM;
            int freq = p.getArmyID();
            for( DistensionPlayer p3 : m_players.values() ) {
                if( p3.getArmyID() != freq ) {
                    Player pobj = m_botAction.getPlayer( p3.getArenaPlayerID() );
                    if( freq % 2 == 0 ) {
                        if( pobj != null && pobj.getYLocation() > TOP_ROOF && pobj.getYLocation() < TOP_LOW )
                            m_botAction.showObjectForPlayer( pobj.getPlayerID(), lvzNum );
                    } else {
                        if( pobj != null && pobj.getYLocation() > BOT_LOW && pobj.getYLocation() < BOT_ROOF )
                            m_botAction.showObjectForPlayer( pobj.getPlayerID(), lvzNum );
                    }
                }
            }
            m_botAction.sendOpposingTeamMessageByFrequency(p.getOpposingArmyID(), "ENEMY OPS placed SHROUD OF DARKNESS over all your army's pilots in " + (freq % 2 == 0 ? "TOP BASE!" : "BOTTOM BASE!") );
            m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), "OPS covered all enemies in " + (freq % 2 == 0 ? "TOP BASE" : "BOTTOM BASE") + " with SHROUD OF DARKNESS!" );
        } else {
            if( p.getCurrentOP() < 3 )
                throw new TWCoreException( "Insufficient OP; need 3.  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );
            p.adjustOP(-3);
            int lvzNum = p.getPurchasedUpgrade(10) > 2 ? LVZ_OPS_SHROUD_LG : LVZ_OPS_SHROUD_SM;
            m_botAction.showObjectForPlayer( p2.getPlayerID(), lvzNum );
            m_botAction.sendPrivateMessage(p2.getPlayerID(), "ENEMY OPS has covered you with the SHROUD OF DARKNESS!" );
            m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), "OPS covered " + p2.getPlayerName() + " with SHROUD OF DARKNESS." );
        }
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub
    }

    /**
     * Ops ability to blind all enemies completely for a short period of time.
     * Does this by showing scary picture of MES from The Fall over a black backdrop.
     * @param name
     * @param msg
     */
    public void cmdOpsBlind( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        int upgLevel = p.getPurchasedUpgrade(11);
        if( upgLevel < 1 )
            throw new TWCoreException( "You are not yet able to use this ability -- first you must install the appropriate !upgrade." );

        Integer blindLevel;
        if( msg.equals("") )
            blindLevel = 1;
        else {
            try {
                blindLevel = Integer.parseInt(msg);
            } catch ( NumberFormatException e ) {
                throw new TWCoreException( "Please specify a blindness intensity between 1 and 3.  Use !opshelp for more info." );
            }
            if( blindLevel < 1 || blindLevel > 3 )
                throw new TWCoreException( "Please specify a blindness intensity between 1 and 3.  Use !opshelp for more info." );
        }
        if( blindLevel > p.getPurchasedUpgrade(11) )
            throw new TWCoreException( "You are not yet able to use the field of blindness at that level -- first you must install the appropriate !upgrade." );

        int cost = 5 + blindLevel * 2;
        if( p.getCurrentOP() < cost )
            throw new TWCoreException( "Insufficient OP; need " + cost + ".  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );

        p.adjustOP(-cost);

        int lvzNum;
        String desc;
        if( blindLevel == 3 ) {
            lvzNum = LVZ_OPS_BLIND3;
            desc = "MINOR";
        } else if( blindLevel == 2 ) {
            lvzNum = LVZ_OPS_BLIND2;
            desc = "MAJOR";
        } else {
            lvzNum = LVZ_OPS_BLIND1;
            desc = "MASSIVE";
        }

        int freq = p.getArmyID();
        for( DistensionPlayer p3 : m_players.values() ) {
            if( p3.getArmyID() != freq )
                m_botAction.showObjectForPlayer( p3.getArenaPlayerID(), lvzNum );
        }
        m_botAction.sendOpposingTeamMessageByFrequency(p.getOpposingArmyID(), "ENEMY OPS disabled all army sensors with FIELD OF BLINDNESS!" );
        m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), "OPS knocked out all enemy sensors with a " + desc + " FIELD OF BLINDNESS!" );
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub

    }

    /**
     * Ops ability to place shields over one player, or all friendly players in base.
     * @param name
     * @param msg
     */
    public void cmdOpsShield( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        int upgLevel = p.getPurchasedUpgrade(12);
        if( upgLevel < 1 )
            throw new TWCoreException( "You are not yet able to use this ability -- first you must install the appropriate !upgrade." );
        if( msg.equals("") )
            msg = "all";
        if( upgLevel < 2 && msg.equals("all") )
            throw new TWCoreException( "You are not yet able to use this ability on all players in the base -- first you must install the appropriate !upgrade." );

        Player p2 = null;
        if( !msg.equals("all") ) {
            p2 = m_botAction.getPlayer( msg );
            if( p2 == null ) {
                p2 = m_botAction.getFuzzyPlayer( msg );
            }
            if( p2 == null ) {
                throw new TWCoreException( "Can't find pilot '" + msg + "'." );
            } else {
                if( p2.getFrequency() != p.getArmyID() )
                    throw new TWCoreException( p2.getPlayerName() + " isn't on your army!" );
                if( p2.getShipType() == Tools.Ship.SPECTATOR )
                    throw new TWCoreException( p2.getPlayerName() + " isn't in a ship!" );
            }
        }

        if( msg.equals("all") ) {
            if( p.getCurrentOP() < 20 )
                throw new TWCoreException( "Insufficient OP; need 20.  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );
            p.adjustOP(-20);

            int freq = p.getArmyID();
            for( DistensionPlayer p3 : m_players.values() ) {
                if( p3.getArmyID() == freq ) {
                    Player pobj = m_botAction.getPlayer( p3.getArenaPlayerID() );
                    if( freq % 2 == 0 ) {
                        if( pobj != null && pobj.getYLocation() > TOP_ROOF && pobj.getYLocation() < TOP_LOW )
                            m_botAction.specificPrize( pobj.getPlayerID(), Tools.Prize.SHIELDS );
                    } else {
                        if( pobj != null && pobj.getYLocation() > BOT_LOW && pobj.getYLocation() < BOT_ROOF )
                            m_botAction.specificPrize( pobj.getPlayerID(), Tools.Prize.SHIELDS );
                    }
                }
            }
            m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), "OPS provided PROTECTIVE SHIELDING for all friendly pilots in " + (freq % 2 == 0 ? "TOP BASE!" : "BOTTOM BASE!") );

        } else {
            if( p.getCurrentOP() < 8 )
                throw new TWCoreException( "Insufficient OP; need 8.  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );
            p.adjustOP(-8);
            m_botAction.specificPrize( p2.getPlayerID(), Tools.Prize.SHIELDS );
            m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), "OPS provided PROTECTIVE SHIELDING for " + p2.getPlayerName() + "." );
        }
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub
    }

    /**
     * EMPs all enemies, reducing their energy to 0 and shutting down engines.
     * @param name
     * @param msg
     */
    public void cmdOpsEMP( String name, String msg ) {
        DistensionPlayer p = m_players.get( name );
        if( p == null )
            return;
        if( p.getShipNum() != 9 )
            throw new TWCoreException( "You must be at the Tactical Ops station to do this." );
        int upgLevel = p.getPurchasedUpgrade(13);
        if( upgLevel < 1 )
            throw new TWCoreException( "You are not yet able to use this ability -- first you must install the appropriate !upgrade." );

        if( p.getCurrentOP() < 15 )
            throw new TWCoreException( "Insufficient OP; need 15.  Current: [" + p.getCurrentOP() + "/" + p.getMaxOP() + "]" );
        p.adjustOP(-15);

        int freq = p.getArmyID();
        for( DistensionPlayer p3 : m_players.values() ) {
            if( p3.getArmyID() != freq ) {
                m_botAction.specificPrize( p3.getArenaPlayerID(), Tools.Prize.ENERGY_DEPLETED );
                m_botAction.specificPrize( p3.getArenaPlayerID(), Tools.Prize.ENGINE_SHUTDOWN_EXTENDED );
            }
        }
        m_botAction.sendOpposingTeamMessageByFrequency(p.getOpposingArmyID(), "ENEMY OPS unleashed an EMP PULSE over your entire army!" );
        m_botAction.sendOpposingTeamMessageByFrequency(p.getArmyID(), "OPS unleashed an EMP PULSE over all enemies!" );
        p.resetIdle();      // Ops can only reset idle by using ops cmds and talking in pub
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
                p.savePlayerDataToDB();
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
        if( m_beginDelayedShutdown ) {
            m_botAction.sendPrivateMessage( name, "IMPORTANT NOTE TO MODERATOR: Bot will automatically save and shut down at end of this round." );
        }
        m_lastSave = System.currentTimeMillis();
    }


    /**
     * Kills the bot.
     * @param name
     * @param msg
     */
    public void cmdDie( String name, String msg ) {
        // If we're sure we want to bypass saving, override.
        if( msg.equals("now") )
            m_lastSave = System.currentTimeMillis();
        m_readyForPlay = false;	// To prevent spec-docking / unnecessary DB accesses
        m_botAction.specAll();
        flagObjs.hideAllObjects();
        flagTimerObjs.hideAllObjects();
        m_botAction.setupObject( LVZ_REARMING, false );
        m_botAction.setupObject( LVZ_EMP, false );
        m_botAction.setupObject( LVZ_ENERGY_TANK, false );
        m_botAction.setupObject( LVZ_JUMPSPACE, false );
        m_botAction.setupObject( LVZ_OPS_FAST_REARM, false );
        m_botAction.sendSetupObjects();
        m_botAction.setDoors(0);
        // Dock Ops so they are put on the spec freq properly
        for( DistensionPlayer p : m_players.values() )
            if( p.getShipNum() == 9 )
                doDock( p );
        Integer id;
        Iterator <Integer>i = m_botAction.getPlayerIDIterator();
        while( i.hasNext() ) {
            id = i.next();
            playerObjs.hideAllObjects( id );
            HashMap <Integer,Boolean>pobjs = playerObjs.getObjects(id);
            if( pobjs != null )
                m_botAction.manuallySetObjects(pobjs, id);
        }
        m_botAction.setObjects();
        m_botAction.manuallySetObjects(flagObjs.getObjects());
        if( !DEBUG ) {
            m_botAction.sendArenaMessage( "Distension going down for maintenance ...", 1 );
        } else {
            m_botAction.sendArenaMessage( "Distension Beta Test concluded.  Thanks for testing!  Join ?chat=distension to ask questions and stay up on tests.  See this thread for bugs and suggestions: http://forums.trenchwars.org/showthread.php?t=31676" );
            m_botAction.sendArenaMessage( "Check out the top 5 ranked players in every ship at: http://www.trenchwars.org/distension   Thanks goes to Foreign for the site.", 5 );
        }
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
        if( m_beginDelayedShutdown ) {
            m_botAction.sendPrivateMessage( name, "Shutdown cancelled." );
            m_beginDelayedShutdown = false;
            m_shutdownTimeMillis = 0;
            return;
        }
        Integer minToShutdown = 0;
        try {
            minToShutdown = Integer.parseInt( msg );
        } catch (NumberFormatException e) {
            throw new TWCoreException( "Improper format.  !shutdown <minutes>.  Or use !shutdown 0 and then !shutdown to cancel." );
        }
        TimerTask delayedShutdownTask = new TimerTask() {
            public void run() {
                m_beginDelayedShutdown = true;
            }
        };
        try {
            m_botAction.scheduleTask(delayedShutdownTask, minToShutdown * 1000 * 60 );
        } catch (Exception e) {
            try {
                m_botAction.cancelTask(delayedShutdownTask);
                throw new TWCoreException( "Prior shutdown cancelled.  Please try !shutdown again momentarily, if you wish..." );
            } catch (Exception e2) {
                throw new TWCoreException( "Shutdown already scheduled!  You may try again momentarily..." );
            }
        }
        m_botAction.sendPrivateMessage( name, "Shutting down at the next end of round occuring after " + minToShutdown + " minutes." );
        if( DEBUG )
            m_botAction.setTimer( minToShutdown );
        m_shutdownTimeMillis = System.currentTimeMillis() + (minToShutdown * Tools.TimeInMillis.MINUTE);
    }


    /**
     * Shows time at which shutdown will occur, approximately.
     * @param name
     * @param msg
     */
    public void cmdShutdownInfo( String name, String msg ) {
        if( m_shutdownTimeMillis <= 0 )
            throw new TWCoreException( "Shutdown mode is not enabled." );
        if( m_beginDelayedShutdown )
            throw new TWCoreException( "Shutdown mode is enabled, and will shut down at the end of this round (timer has reached 0)." );

        m_botAction.sendPrivateMessage(name, "Shutdown mode will be enabled in approximately " + Tools.getTimeDiffString(m_shutdownTimeMillis, false) + "." );
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
            if( !player.getPlayerFromDB() )
                throw new TWCoreException( "Can't find player '" + msg + "' in DB.  Check spelling." );
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
            if( !player.getPlayerFromDB() )
                throw new TWCoreException( "Can't find player '" + msg + "' in DB.  Check spelling." );
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
        if( m_players.get(msg) == null )
            throw new TWCoreException( "Player '" + msg + "' not found.  Note that you must use exact case." );
        cmdStatus( msg, null, name );
    }


    /**
     * Changes player's name in the database.
     * @param name
     * @param msg
     */
    public void cmdDBChangeName( String name, String msg ) {
        String[] args = msg.split(":");
        if( args.length != 2 || args[0].equals("") || args[1].equals("") )
            throw new TWCoreException( "Syntax: !db-changename <currentname>:<newname>" );
        DistensionPlayer player = m_players.get( args[0] );
        if( player != null )
            throw new TWCoreException( args[0] + " found in arena.  Player must leave arena in order to use this command." );

        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT * FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( args[1] ) + "'" );
            if( r != null && r.next() )
                throw new TWCoreException( "'" + args[1] + "' already exists in the DB (probably already enlisted)!  Use !db-wipeplayer to remove from the DB first before using this command." );

        } catch (SQLException e ) {
            throw new TWCoreException( "DB command not successful." );
        }

        try {
            m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcName='" + Tools.addSlashesToString(args[1]) + "' WHERE fcName='" + Tools.addSlashesToString( args[0] ) + "'" );
        } catch (SQLException e ) {
            throw new TWCoreException( "DB command not successful." );
        }
        m_botAction.sendPrivateMessage( name, "Name '" + args[0] + "' changed to '" + args[1] + "' in database." );
        Tools.printLog(name + " changed " + args[0] + "'s name to " + args[1] + " in Distension DB." );
    }


    /**
     * Adds a ship to a player's profile in the DB.
     * @param name
     * @param msg
     */
    public void cmdDBAddShip( String name, String msg ) {
        String[] args = msg.split(":");
        if( args.length != 2 || args[0].equals("") || args[1].equals("") )
            throw new TWCoreException( "Syntax: !db-addship <name>:<ship#>" );
        DistensionPlayer player = m_players.get( args[0] );
        if( player == null ) {
            m_botAction.sendPrivateMessage( name, args[0] + " not found in arena.  Player must be in arena in order to use this command." );
            return;
        }
        int shipNumToAdd = 0;
        try {
            shipNumToAdd = Integer.parseInt( args[1] );
        } catch (NumberFormatException e) {
            throw new TWCoreException( "Invalid ship #.  Syntax: !db-addship <name>:<ship#>" );
        }
        if( shipNumToAdd < 1 || shipNumToAdd > 8 )
            throw new TWCoreException( "Invalid ship #.  Syntax: !db-addship <name>:<ship#>" );
        if( player.shipIsAvailable(shipNumToAdd))
            throw new TWCoreException( "That ship is already available to that player." );

        player.addShipToDB( shipNumToAdd );
        String shipname = ( player.getShipNum() == 9 ? "Tactical Ops" : Tools.shipName(shipNumToAdd) );
        m_botAction.sendPrivateMessage( name, shipname + " has been added to " + player.getName() + "'s hangar." );
        Tools.printLog( name + " added ship #" + shipNumToAdd + " to " + args[0] + "'s hangar in Distension DB." );
    }


    /**
     * Removes a player's ship from the DB.
     * @param name
     * @param msg
     */
    public void cmdDBWipeShip( String name, String msg ) {
        String[] args = msg.split(":");
        if( args.length != 2 || args[0].equals("") || args[1].equals("") )
            throw new TWCoreException( "Syntax: !db-wipeship <name>:<ship#>" );
        DistensionPlayer player = m_players.get( args[0] );
        if( player == null ) {
            m_botAction.sendPrivateMessage( name, args[0] + " not found in arena.  Player must be in arena in order to use this command." );
            return;
        }
        int shipNumToWipe = 0;
        try {
            shipNumToWipe = Integer.parseInt( args[1] );
        } catch (NumberFormatException e) {
            throw new TWCoreException( "Invalid ship #.  Syntax: !db-wipeship <name>:<ship#>" );
        }
        if( shipNumToWipe < 1 || shipNumToWipe > 8 )
            throw new TWCoreException( "Invalid ship #.  Syntax: !db-wipeship <name>:<ship#>" );
        if( shipNumToWipe == player.getShipNum() )
            throw new TWCoreException( "Player is currently in that ship.  They must be in a different ship in order for the procedure to work." );

        player.removeShipFromDB( shipNumToWipe );
        String shipname = ( player.getShipNum() == 9 ? "Tactical Ops" : Tools.shipName(shipNumToWipe) );
        m_botAction.sendPrivateMessage( name, shipname + " has been removed from " + player.getName() + "'s hangar." );
        Tools.printLog( name + " deleted " + args[0] + "'s ship #" + shipNumToWipe + " from Distension DB." );
    }


    /**
     * Wipes all traces of player from DB.
     * @param name
     * @param msg
     */
    public void cmdDBWipePlayer( String name, String msg ) {
        DistensionPlayer player = m_players.get( msg );
        if( player != null )
            throw new TWCoreException( msg + " found in arena.  Player must leave arena in order to use this command." );

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
            if( r == null )
                throw new TWCoreException( "DB command not successful." );
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
            Tools.printStackTrace( "Error getting player data from DB for !db-randomarmies.", e );
            throw new TWCoreException( "DB command not successful." );
        }
    }


    // BETA-ONLY COMMANDS

    /**
     * Grants a player credits (only for debug mode).
     * @param name
     * @param msg
     */
    public void cmdGrant( String name, String msg ) {
        if( !DEBUG )
            throw new TWCoreException( "This command disabled during normal operation." );

        String[] args = msg.split(":");
        if( args.length != 2 )
            throw new TWCoreException( "Improper format.  !grant name:points" );

        DistensionPlayer player = m_players.get( args[0] );
        if( player == null )
            throw new TWCoreException( "Can't find player '" + msg + "' in arena." );

        float points = 0;
        if( msg == "max" ) {
            points = player.getPointsToNextRank();
        } else {
            try {
                points = Integer.parseInt( args[1] );
            } catch (NumberFormatException e) {
                throw new TWCoreException( "Improper format.  !grant name:points" );
            }
        }

        points /= DEBUG_MULTIPLIER; // Adjust by multiplier to make amount fairly exact.

        points = player.addRankPoints( (int)points, false );
        m_botAction.sendPrivateMessage( name, "Granted " + (int)points + "RP to " + args[0] + ".", 1 );
        m_botAction.sendPrivateMessage( args[0], "You have been granted " + points + "RP by " + name + ".", 1 );
    }


    /**
     * Sends a message to all beta-testers.
     * @param name
     * @param msg
     */
    public void cmdMsgBeta( String name, String msg ) {
        if( !DEBUG )
            throw new TWCoreException( "This command disabled during normal operation." );
        int players = 0;
        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fcName FROM tblDistensionPlayer WHERE 1" );
            while( r.next() ) {
                m_msgBetaPlayers.add(r.getString("fcName"));
                players++;
            }
            m_botAction.SQLClose(r);
        } catch (SQLException e) {
            m_botAction.sendSmartPrivateMessage( "dugwyler", e.getMessage() );
        }
        m_botAction.sendPrivateMessage( name, players + " players added to notify list." );

        TimerTask msgTask = new TimerTask() {
            public void run() {
                if( !m_msgBetaPlayers.isEmpty() )
                    m_botAction.sendRemotePrivateMessage( m_msgBetaPlayers.remove(), "DISTENSION BETA TEST: ?go #distension if you can participate." );
                else
                    this.cancel();
            }
        };
        m_botAction.scheduleTask(msgTask, 1000, 100);
    }

    // HARDCORE COMMANDS (DEBUG)

    /**
     * Sets a variable.
     * @param name
     * @param msg
     */
    public void cmdSetVar( String name, String msg ) {
        if( !DEBUG ) return;
        String[] args = msg.toLowerCase().split(":");
        if( args.length != 3 )
            throw new TWCoreException( "Improper format.  !debug-setvar player:var:value" );

        DistensionPlayer p = m_players.get( args[0] );
        if( p == null )
            throw new TWCoreException("Player not found.");

        try {
            // Parse first as int
            Integer value = Integer.parseInt( args[2] );
            if( !p.setVar(args[1],value) )
                throw new TWCoreException("Set failed for setting " + args[1] + " to " + value + "." );
            m_botAction.sendSmartPrivateMessage( name, "Set " + args[1] + " to " + args[2] + " on " + args[0] );
        } catch (NumberFormatException e) {
            // Fails?  Try boolean
            Boolean value = null;
            if( args[2].startsWith("t") )
                value = true;
            else if( args[2].startsWith("f") )
                value = false;
            if( value == null )
                throw new TWCoreException("Unable to parse value '" + args[2] + "' as either an int or bool.");
            if( !p.setVar(args[1],value) )
                throw new TWCoreException("Set failed for setting " + args[1] + " to " + value + "." );
            m_botAction.sendSmartPrivateMessage( name, "Set " + args[1] + " to " + args[2] + " on " + args[0] );
        }

    }

    /**
     * Gets an int variable.
     * @param name
     * @param msg
     */
    public void cmdGetInt( String name, String msg ) {
        if( !DEBUG ) return;
        String[] args = msg.split(":");
        if( args.length != 2 )
            throw new TWCoreException( "Improper format.  !debug-getint player:var" );

        DistensionPlayer p = m_players.get( args[0] );
        if( p == null )
            throw new TWCoreException("Player not found.");

        Integer i = p.getInt(args[1]);
        if( i == null )
            throw new TWCoreException( "Var not found." );
        m_botAction.sendSmartPrivateMessage( name, args[1] + "=" + i + " on " + args[0] );
    }

    /**
     * Gets a boolean variable.
     * @param name
     * @param msg
     */
    public void cmdGetBool( String name, String msg ) {
        if( !DEBUG ) return;
        String[] args = msg.split(":");
        if( args.length != 2 )
            throw new TWCoreException( "Improper format.  !debug-getbool player:var" );

        DistensionPlayer p = m_players.get( args[0] );
        if( p == null )
            throw new TWCoreException("Player not found.");

        Boolean b = p.getBoolean(args[1]);
        if( b == null )
            throw new TWCoreException( "Var not found." );
        m_botAction.sendSmartPrivateMessage( name, args[1] + "=" + (b?"true":"false") + " on " + args[0] );
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
     * Makes a String with requested number of asterisks, padded with spaces.
     * If bars > length, number of bars in String == length.
     * @param bars Total number of bars requested
     * @param length Total length of String
     * @return Formatted String
     */
    public String makeBar( int bars, int length ) {
        String bar = "";
        for(int i=0; i<length; i++ )
            if( i < bars )
                bar += "*";
            else
                bar += " ";
        return bar;
    }

    /**
     * Checks if flag time should be started.
     */
    public void checkFlagTimeStart() {
        Iterator <DistensionArmy>i = m_armies.values().iterator();
        boolean foundOne = false;
        while( i.hasNext() ) {
            DistensionArmy army = i.next();
            if( army.getPilotsInGame() >= PILOTS_REQ_EACH_ARMY ) {
                if( !foundOne )
                    foundOne = true;
                else {
                    // Two armies now have enough players; start game, or continue if already started
                    if( !flagTimeStarted ) {
                        m_botAction.sendArenaMessage( "This sector is no longer safe: a war is brewing ...  All pilots, report for duty.  You have " + getTimeString(3 * INTERMISSION_SECS) + " to prepare for the assault.");
                        flagTimer = new FlagCountTask();    // Dummy, for displaying score.
                        intermissionTimer = new IntermissionTask();
                        m_botAction.scheduleTask( intermissionTimer, (2950 * INTERMISSION_SECS) );
                        if( !DEBUG )
                            m_botAction.setTimer(3);
                        m_roundNum = 0;
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
            if( army.getPilotsInGame() >= PILOTS_REQ_EACH_ARMY ) {
                if( !foundOne )
                    foundOne = true;
                else {
                    // Two armies have enough players; do not stop game
                    return;
                }
            }
        }
        stopFlagTime = true;
        if( m_armies.get(0).getPilotsInGame() == 0 && m_armies.get(1).getPilotsInGame() == 0 ) {
            resetAllFlagData();
        }
    }

    /**
     * Based on number of players in the arena and whether the game is using one or
     * two flags, return the time required for a flag win.
     * @return
     */
    public int getActualTimeNeededForFlagWin() {
        int timeNeeded;
        int players = m_botAction.getNumPlaying();
        if( m_singleFlagMode ) {
            timeNeeded = flagSecondsRequiredSingleFlag;
            if( players >= 20 )
                timeNeeded -= 15;
        } else {
            timeNeeded = flagSecondsRequiredDoubleFlag;
            if( players >= 35 )
                timeNeeded -= 10;
            if( players >= 40 )
                timeNeeded -= 5;
        }
        if( m_flagRules == 1 )
            return (int)(timeNeeded * flagSecondsHybridFactor);
        return timeNeeded;
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
        if( !m_singleFlagMode )
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
            desc = "Higher-powered gunning systems";
            break;
        case Tools.Prize.BOMBS:
            desc = "Higher-powered bombing systems";
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
             * -4:  Targeted EMP against all enemies (uses !emp command)
             * -5:  Super chance every 30 seconds, for spiders
             * -6:  Repel chance every 30 seconds, for sharks
             * -7:  Profit sharing
             */
        case ABILITY_PRIORITY_REARM:
            desc = "Always first in line to rearm, plus full energy after rearm";
            break;
        case ABILITY_TERR_REGEN:
            desc = "+7% burst/+9% portal chance every 30 seconds";
            break;
        case ABILITY_ENERGY_TANK:
            desc = "+25% chance of replenishing a reusable energy tank";
            break;
        case ABILITY_TARGETED_EMP:
            desc = "EMP ALL enemies (possible every 20 minutes in Terr)";
            break;
        case ABILITY_SUPER:
            desc = "+9% chance of super every 30 seconds";
            break;
        case ABILITY_SHARK_REGEN:
            desc = "+25% chance of repel every 30 seconds";
            break;
        case ABILITY_PROFIT_SHARING:
            desc = "+1% of team's kill RP earned per level";
            break;
        case ABILITY_VENGEFUL_BASTARD:
            desc = "+15% chance of something awful happening to your killers";
            break;
        case ABILITY_ESCAPE_POD:
            desc = "+10% chance of respawning in the exact spot you died";
            break;
        case ABILITY_LEECHING:
            desc = "+15% chance of full charge after every kill";
            break;
        case ABILITY_JUMPSPACE:
            desc = "Spacial Jump improvements (+regen, -cooldown)";
            break;
        case ABILITY_THOR:
            desc = "Thor recharged every 5 minutes";
            break;
        case ABILITY_MASTER_DRIVE:
            desc = "Chance of Super/Shields after streak of 5";
            break;
        case ABILITY_PRISMATIC_ARRAY:
            desc = "+15% chance to refuel prismatic decoy array";
            break;
        case ABILITY_FIREBLOOM:
            desc = "Expedient rearming of Firebloom burst";
            break;
        case ABILITY_SUMMONING_AUTH:
            desc = "Increased order permissions using !summon";
            break;
        // OPS
        case OPS_INCREASE_MAX_OP:
            desc = "Larger Tactical Ops Point reserve";
            break;
        case OPS_REGEN_RATE:
            desc = "Regenerate +1 OP per cycle";
            break;
        case OPS_WARP:
            desc = "Warp teammates to locations";
            break;
        case OPS_FAST_TEAM_REARM:
            desc = "Priority rearm for all teammates";
            break;
        case OPS_COVER:
            desc = "Drop cover in home base";
            break;
        case OPS_DOOR_CONTROL:
            desc = "Control gates in bases";
            break;
        case OPS_SECLUSION:
            desc = "Seclude enemy in sphere";
            break;
        case OPS_MINEFIELD:
            desc = "Deploy an artificial minefield";
            break;
        case OPS_SHROUD:
            desc = "Cover enemy in cone of darkness";
            break;
        case OPS_FLASH:
            desc = "Totally blind enemy for short time";
            break;
        case OPS_TEAM_SHIELDS:
            desc = "Provide shields for all teammates";
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
     * Spams a player with a LinkedList array based on a default delay.
     * @param arenaID ID of person to spam
     * @param msgs LinkedList containing msgs to spam
     */
    public void spamWithDelay( int arenaID, LinkedList<String> msgs ) {
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
     * warps the player when done prizing.  ***No longer used.***
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

        public void setMsgs( int id, LinkedList<String> list ) {
            arenaIDToSpam = id;
            remainingMsgs = list;
        }

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
                    m_botAction.sendUnfilteredPrivateMessage( arenaIDToSpam, "*prize#" + prize );
            }
        }
    }




    // ***** PLAYER CLASS

    /**
     * Used to keep track of player data retreived from the DB, and update data
     * during play, to be later synch'd with the DB.
     */
    private class DistensionPlayer {
        private String name;    // Playername
        private int arenaPlayerID;    // ID as understood by Arena
        private int dbPlayerID;   // PlayerID as found in DB (not as in Arena); -1 if not logged in
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
        private int battlesWon; // Total number of battles player has won;    -1 if not logged in
        private int successiveKills;            // # successive kills (for unlocking weasel)
        private int[]     purchasedUpgrades;    // Upgrades purchased for current ship
        private boolean[] shipsAvail;           // Marks which ships are available
        private int[]     lastIDsKilled = { -1, -1, -1, -1 };  // ID of last player killed (feeding protection)
        private int       spawnTicks;           // # queue "ticks" until spawn
        private int       idleTicks;            // # ticks player has been idle
        private int       assistArmyID;         // ID of army player is assisting; -1 if not assisting
        private int       recentlyEarnedRP;     // RP earned since changing to this ship
        private int       numLagouts;           // # lagouts this round
        private int       currentOP;            // Current # OP points (for Tactical Ops)
        private int       maxOP;                // Max # OP points (for Tactical Ops)
        private int       currentComms;         // Current # communications saved up (for Tactical Ops)
        private int       lastX;                // Last X position
        private int       lastY;                // Last Y position
        private int       lastXVel;             // Last X velocity
        private int       lastYVel;             // Last Y velocity
        private int       lastRot;              // Last rotation
        private int       idlesInBase;          // # idle checks in which a player has been in base
        private int       bonusPrize;           // # of prize to additionally prize at next spawn
        private boolean   energyTank;           // True if player has an energy tank available
        private boolean   targetedEMP;          // True if player has targeted EMP available
        private boolean   jumpSpace;            // True if player has JumpSpace available
        private boolean   prismatic;            // True if player has Prismatic Array available
        private int       vengefulBastard;      // Levels of Vengeful Bastard ability
        private int       escapePod;            // Levels of Escape Pod ability
        private boolean   escapePodFired;       // Whether or not escape pod has already fired this death
        private int       leeching;             // Levels of Leeching ability
        private int       masterDrive;          // Levels of M.A.S.T.E.R. Drive
        private int       firebloom;            // Levels of Firebloom
        private long      opsAFKNotifyTime;     // Timestamp of Ops being notified of AFK
        private long      lastVengeTime;        // Timestamp of last time Vengeful Bastard fired on player
        private long      lastSummonTime;       // Timestamp of last time summon was used
        private String    lastVenger;           // Name of player that last fired Vengeful Bastard on player
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
        private boolean   ignoreShipChanges;    // True if all ship changes should be ignored until old ship is entered
        private boolean   rankedUpFromLastKill; // True if player ranked up from the last kill
        private boolean   allowSummon;          // True if the player will allow themselves to be summoned
        private TimerTask personalTask = null;  // Personal TimerTask for various uses

        public DistensionPlayer( String name ) {
            this.name = name;
            arenaPlayerID = m_botAction.getPlayerID(name);
            dbPlayerID = -1;
            timePlayed = -1;
            shipNum = -1;
            lastShipNum = -1;
            rank = -1;
            rankPoints = -1;
            nextRank = -1;
            upgPoints = -1;
            armyID = -1;
            progress = -1;
            battlesWon = -1;
            currentOP = 0;
            maxOP = 0;
            currentComms = 0;
            successiveKills = 0;
            spawnTicks = 0;
            idleTicks = 0;
            assistArmyID = -1;
            recentlyEarnedRP = 0;
            numLagouts = 0;
            bonusBuildup = 0.0;
            lastX = 0;
            lastY = 0;
            lastXVel = 0;
            lastYVel = 0;
            lastRot = 0;
            idlesInBase = 0;
            bonusPrize = 0;
            vengefulBastard = 0;
            escapePod = 0;
            escapePodFired = false;
            leeching = 0;
            masterDrive = 0;
            firebloom = 0;
            opsAFKNotifyTime = 0;
            lastVengeTime = 0;
            lastSummonTime = System.currentTimeMillis();
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
            energyTank = false;
            targetedEMP = false;
            jumpSpace = false;
            prismatic = false;
            ignoreShipChanges = false;
            rankedUpFromLastKill = false;
            allowSummon = true;
        }


        // Nasty access

        /**
         * Manually gets the value of an int variable in the class.
         * @param varname Name of variable
         * @return Value of variable
         */
        public Integer getInt( String varname ) {
            try {
                Field f = this.getClass().getDeclaredField(varname);
                if( f == null )
                    return null;
                return f.getInt(this);
            } catch( Exception e ) {
                return null;
            }
        }

        /**
         * Manually gets the value of a boolean variable in the class.
         * @param varname Name of variable
         * @return Value of variable
         */
        public Boolean getBoolean( String varname ) {
            try {
                Field f = this.getClass().getDeclaredField(varname);
                if( f == null )
                    return null;
                return f.getBoolean(this);
            } catch( Exception e ) {
                return null;
            }
        }

        /**
         * Manually sets a variable in the class.
         */
        public boolean setVar( String varname, Object value ) {
            try {
                Field f = this.getClass().getDeclaredField(varname);
                if( f == null )
                    return false;
                if( value instanceof Boolean )
                    f.setBoolean(this, (Boolean)value);
                else if( value instanceof Integer )
                    f.setInt(this, (Integer)value);
            } catch( Exception e ) {
                return false;
            }
            return true;
        }


        // DB METHODS

        /**
         * Creates a player record in the database.
         */
        public void addPlayerToDB() {
            try {
                ResultSet r = m_botAction.SQLQuery( m_database, "INSERT INTO tblDistensionPlayer ( fcName , fnArmyID ) " +
                        "VALUES ('" + Tools.addSlashesToString( name ) + "', '" + armyID + "')" );
                if( r == null ) {
                    Tools.printLog( "Null ResultSet returned for query to add player to DB on connection '" + m_database + "'" );
                    m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
                    return;
                }
                if( r.next() ) {
                    dbPlayerID = r.getInt(1);     // Get index ID returned
                    m_botAction.SQLClose(r);
                }
            } catch (SQLException e ) { m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG ); }
        }

        /**
         * Reads in data about the player from the database.
         * @return True if data is retrieved; false if it wasn't.
         */
        public boolean getPlayerFromDB() {
            try {
                boolean success = false;
                ResultSet r = m_botAction.SQLQuery( m_database, "SELECT * FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
                if( r == null ) {
                    Tools.printLog( "Null ResultSet returned for query: 'SELECT * FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name ) + "'' on connection '" + m_database + "'" );
                    m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
                    return false;
                }
                if( r.next() ) {
                    banned = r.getString( "fcBanned" ).equals( "y" );
                    if( banned == true )
                        return false;
                    dbPlayerID = r.getInt("fnID");
                    armyID = r.getInt( "fnArmyID" );
                    timePlayed = r.getInt( "fnTime" );
                    battlesWon = r.getInt( "fnBattlesWon" );
                    sendKillMessages = r.getString( "fcSendKillMsg" ).equals("y");
                    for( int i = 0; i < 9; i++ )
                        shipsAvail[i] = ( r.getString( "fcShip" + (i + 1) ).equals( "y" ) ? true : false );
                    success = true;
                }
                m_botAction.SQLClose(r);
                return success;
            } catch (SQLException e ) {
                Tools.printStackTrace("Problem fetching returning player: " + name, e);
                m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
                return false;
            }
        }

        /**
         * Saves general player data (time played and number of battles won) to DB.
         */
        public void savePlayerDataToDB() {
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fnTime='" + timePlayed +"', fnBattlesWon='" + battlesWon + "' WHERE fnID='" + dbPlayerID + "'" );
            } catch (SQLException e ) {
                Tools.printStackTrace("Problem saving player: " + name, e);
                m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
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
            if( shipNumToAdd < 1 || shipNumToAdd > 9 )
                return;
            shipsAvail[ shipNumToAdd - 1 ] = true;
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcShip" + shipNumToAdd + "='y' WHERE fnID='" + dbPlayerID + "'" );
                if( startingRankPoints == 0 )
                    m_botAction.SQLQueryAndClose( m_database, "INSERT INTO tblDistensionShip ( fnPlayerID , fnShipNum ) VALUES (" + dbPlayerID + ", " + shipNumToAdd + ")" );
                else
                    m_botAction.SQLQueryAndClose( m_database, "INSERT INTO tblDistensionShip ( fnPlayerID , fnShipNum , fnRankPoints ) VALUES (" + dbPlayerID + ",  " + shipNumToAdd + ", " + startingRankPoints + ")" );
            } catch (SQLException e ) {
                Tools.printStackTrace("Problem adding ship " + shipNumToAdd + " to DB for: " + name, e);
                m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
            }
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
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcShip" + shipNumToRemove + "='n' WHERE fnID='" + dbPlayerID + "'" );
                m_botAction.SQLQueryAndClose( m_database, "DELETE FROM tblDistensionShip WHERE fnPlayerID='" + dbPlayerID + "' AND fnShipNum='" + shipNumToRemove + "'" );
            } catch (SQLException e ) {
                Tools.printStackTrace("Problem removing ship " + shipNumToRemove + " from DB for: " + name, e);
                m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
            }
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

            query +=       " WHERE ship.fnPlayerID='" + dbPlayerID + "' AND ship.fnShipNum='" + shipNum + "'";
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
                "WHERE ship.fnPlayerID='" + dbPlayerID + "' AND ship.fnShipNum='" + shipNum + "'";

                ResultSet r = m_botAction.SQLQuery( m_database, query );
                if( r == null ) {
                    Tools.printLog( "Null ResultSet returned for query: '" + query + "' on connection '" + m_database + "'" );
                    m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
                    return false;
                }
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

                fastRespawn = (shipNum == 5);  // Terrs always have priority rearm
                vengefulBastard = 0;
                escapePod = 0;
                escapePodFired = false;
                leeching = 0;
                masterDrive = 0;
                firebloom = 0;
                bonusPrize = 0;
                energyTank = false;
                targetedEMP = false;
                jumpSpace = false;
                prismatic = false;
                if( shipNum == 9 )
                    maxOP = DEFAULT_MAX_OP;
                else
                    maxOP = 0;
                // Setup special (aka unusual) abilities
                Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum ).getAllUpgrades();
                for( int i = 0; i < NUM_UPGRADES; i++ ) {
                    if( upgrades.get( i ).getPrizeNum() == ABILITY_PRIORITY_REARM && purchasedUpgrades[i] > 0 )
                        fastRespawn = true;
                    else if( upgrades.get( i ).getPrizeNum() == OPS_INCREASE_MAX_OP )
                        maxOP += (purchasedUpgrades[i] * 2);
                    else if( upgrades.get( i ).getPrizeNum() == ABILITY_VENGEFUL_BASTARD )
                        vengefulBastard = purchasedUpgrades[i];
                    else if( upgrades.get( i ).getPrizeNum() == ABILITY_ESCAPE_POD )
                        escapePod = purchasedUpgrades[i];
                    else if( upgrades.get( i ).getPrizeNum() == ABILITY_LEECHING )
                        leeching = purchasedUpgrades[i];
                    else if( upgrades.get( i ).getPrizeNum() == ABILITY_MASTER_DRIVE )
                        masterDrive = purchasedUpgrades[i];
                }

                m_botAction.SQLClose(r);
                return shipDataSaved;
            } catch (SQLException e ) {
                Tools.printStackTrace( "Error getting ship from DB.", e );
                m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
                return false;
            }
        }


        // COMPLEX ACTIONS / SETTERS

        /**
         * Decrements spawn ticker, which must be decremented to 0 before player is
         * allowed back in.  During this time the player will see the familiar countdown.
         * This ensures weapons will continue firing after death, won't clear mines,
         * and provides a decent pause to break up the action.
         */
        public void doSpawnTick() {
            if( !isRespawning )
                return;
            spawnTicks--;
            if( spawnTicks == 3 ) { // 3 ticks (1.5 sec) before spawn end, warp to safe and shipreset

                // Terrs without escape pod, above rank 30 and dying in an FR have chance
                // of activating Last Breath ability.
                if( escapePod <= 0 ) {
                    if( shipNum == 5 && rank >= 30 ) {
                        Player p = m_botAction.getPlayer( arenaPlayerID );
                        if( p==null ) return;
                        int base = -1;
                        if( p.getYTileLocation() > TOP_ROOF && p.getYTileLocation() < TOP_FR )
                            base = 0;
                        if( p.getYTileLocation() > BOT_FR && p.getYTileLocation() < BOT_ROOF )
                            base = 1;
                        if( base != -1 ) {
                            int chance = 50 + (rank - 30); // For every level after 30, +1% chance
                            if( chance >= (int)(Math.random() * 100) ) {
                                if( base == 0 )
                                    for( DistensionPlayer p2 : m_players.values() )
                                        if( p2.getArmyID() == getArmyID() && p.getYTileLocation() > TOP_ROOF && p.getYTileLocation() < TOP_FR )
                                            m_botAction.specificPrize(p2.getArenaPlayerID(), Tools.Prize.FULLCHARGE);
                                if( base == 1 )
                                    for( DistensionPlayer p2 : m_players.values() )
                                        if( p2.getArmyID() == getArmyID() && p.getYTileLocation() > BOT_FR && p.getYTileLocation() < BOT_ROOF )
                                            m_botAction.specificPrize(p2.getArenaPlayerID(), Tools.Prize.FULLCHARGE);
                            }
                        }
                    }
                } else if( !escapePodFired ) {
                    // Check for the escape pod, and if it should fire, respawn instantly.
                    double podChance = Math.random() * 10.0;
                    if( escapePod >= podChance ) {
                        respawnImmediately();
                        return;
                    }
                }
                m_botAction.showObjectForPlayer(arenaPlayerID, LVZ_REARMING);
                doRearmAreaWarp();
                m_botAction.shipReset(arenaPlayerID);
            }
        }

        /**
         * Performs necessary actions to spawn the player, if ready.
         */
        public boolean doSpawn() {
            if( specialRespawn ) {
                specialRespawn = false;
                isRespawning = false;
                m_prizeQueue.resumeSpawningAfterDelay( prizeUpgrades( false ) );
                return true;
            }
            if( !isRespawning ) {
                escapePodFired = false;
                return true;
            }
            if( spawnTicks > 0 )
                return false;
            isRespawning = false;
            escapePodFired = false;
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
            Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum ).getAllUpgrades();
            int prize = -1;
            int totalPrized = 0;
            for( int i = 0; i < NUM_UPGRADES; i++ ) {
                prize = upgrades.get( i ).getPrizeNum();
                if( prize > 0 )
                    for( int j = 0; j < purchasedUpgrades[i]; j++ ) {
                        // Special case: on Lanc, only prize bombs if not attached
                        if( shipNum == 7 && i == 11 && purchasedUpgrades[11] > 0 ) {
                            Player p = m_botAction.getPlayer(arenaPlayerID);
                            if( p != null )
                                if( !p.isAttached() ) {
                                    m_botAction.sendUnfilteredPrivateMessage( arenaPlayerID, "*prize#" + prize );
                                    totalPrized++;
                                }
                        }
                        else {
                            m_botAction.sendUnfilteredPrivateMessage( arenaPlayerID, "*prize#" + prize );
                            totalPrized++;
                        }
                    }
            }
            if( isFastRespawn() )
                m_botAction.sendUnfilteredPrivateMessage( arenaPlayerID, "*prize#" + Tools.Prize.FULLCHARGE );
            if( bonusPrize != 0 ) {
                m_botAction.sendUnfilteredPrivateMessage( arenaPlayerID, "*prize#" + bonusPrize );
                bonusPrize = 0;
            }
            if( firebloom == 3 )
                m_botAction.sendUnfilteredPrivateMessage( arenaPlayerID, "*prize#" + Tools.Prize.BURST );
            if( warp )
                doWarp(false);
            m_botAction.hideObjectForPlayer(arenaPlayerID, LVZ_REARMING);
            return totalPrized * PRIZE_SPAM_DELAY;
        }

        /**
         * Sets up player for respawning.
         */
        public void doSetupRespawn() {
            isRespawning = true;
            if( isFastRespawn() ) {
                if( fastRespawn )   // Standard priority rearmers always get to the head
                    m_prizeQueue.addHighPriorityPlayer( this );
                else                // Other rearmers just get ahead of the other army
                    m_prizeQueue.addPriorityRearmPlayer( this );
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
            if( isFastRespawn() ) {
                m_prizeQueue.addHighPriorityPlayer( this );
            } else {
                m_prizeQueue.addPlayer( this );
            }
            spawnTicks = 0;
        }

        /**
         * Removes player from all respawn queues and respawns them immediately.
         */
        public void respawnImmediately() {
            if( isRespawning || specialRespawn ) {
                m_prizeQueue.removePlayer(this);
                isRespawning = false;
                specialRespawn = false;
                escapePodFired = true;
                spawnTicks = 0;
                m_botAction.shipReset( arenaPlayerID );
                this.prizeUpgradesNow();
            }
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
            // At round start of 2 flag game, warp to special spawn
            if( roundStart ) {
                if( !m_singleFlagMode ) {
                    if( warpInBase ) {
                        int xmod = (int)(Math.random() * 10) - 5;
                        int ymod = (int)(Math.random() * 10) - 5;
                        x = 512 + xmod;
                        if( base == 0 )
                            y = BASE_CENTER_0_Y_COORD;
                        else
                            y = BASE_CENTER_1_Y_COORD;
                        y += ymod;
                    }
                } else {
                    if( base == 0 ) {
                        x = LEFT_EAR_X;
                        y = LEFT_EAR_Y;
                    } else {
                        x = RIGHT_EAR_X;
                        y = RIGHT_EAR_Y;
                    }
                    y += (int)(Math.random() * 4) - 2;
                    x += (int)(Math.random() * 4) - 2;
                }
            } else {
                Random r = new Random();
                x = 512 + (r.nextInt(SPAWN_X_SPREAD) - (SPAWN_X_SPREAD / 2));
                if( !m_singleFlagMode ) {
                    if( base == 0 )
                        y = SPAWN_BASE_0_Y_COORD + (r.nextInt(SPAWN_Y_SPREAD) - (SPAWN_Y_SPREAD / 2));
                    else
                        y = SPAWN_BASE_1_Y_COORD + (r.nextInt(SPAWN_Y_SPREAD) - (SPAWN_Y_SPREAD / 2));
                } else {
                    y = SPAWN_BASE_0_Y_COORD + (r.nextInt(SPAWN_Y_SPREAD) - (SPAWN_Y_SPREAD / 2));
                }
            }
            m_botAction.warpTo(arenaPlayerID, x, y);
        }

        /**
         * Warps player to the rearm area, no strings attached.
         */
        public void doRearmAreaWarp() {
            int base = getArmyID() % 2;
            int xmod = (int)(Math.random() * 4) - 2;
            int ymod = (int)(Math.random() * 4) - 2;
            if( base == 0 )
                m_botAction.warpTo(arenaPlayerID, 512 + xmod, REARM_AREA_TOP_Y + ymod);
            else
                m_botAction.warpTo(arenaPlayerID, 512 + xmod, REARM_AREA_BOTTOM_Y + ymod);
        }

        /**
         * Warps player to the safety part of the rearm area, to reset various
         */
        public void doRearmSafeWarp() {
            int base = getArmyID() % 2;
            if( base == 0 )
                m_botAction.warpTo(arenaPlayerID, 512, REARM_SAFE_TOP_Y );
            else
                m_botAction.warpTo(arenaPlayerID, 512, REARM_SAFE_BOTTOM_Y );
        }

        /**
         * Prizes any special abilities that are reprized every 30 seconds.  This should only
         * be called if a player actually has one of these special abilities.
         *
         * Special prize #s that are prized with this method:
         * -2:  Prizing burst + warp for terriers
         * -3:  Full charge for spiders
         * -4:  Targeted EMP against all enemies -- recharged every 10 min or so, and not lost on death
         * -5:  Super for spiders
         * -6:  Repels for sharks
         *
         * @param tick Number of times this method has been run; used for intermittent abilities
         */
        public void prizeSpecialAbilities( int tick ) {
            boolean prized = false;
            if( shipNum == 5 ) {
                // Regeneration ability; each level worth an additional 8% of prizing either port or burst
                //                       (+8% for each, up to a total of 80%)
                double portChance = Math.random() * 100.0;
                double burstChance = Math.random() * 100.0;
                if( ((double)purchasedUpgrades[11] * 9.0) > portChance && !isRespawning ) {
                    m_botAction.specificPrize( arenaPlayerID, Tools.Prize.PORTAL );
                    prized = true;
                }
                if( ((double)purchasedUpgrades[11] * 7.0) > burstChance && !isRespawning ) {
                    m_botAction.specificPrize( arenaPlayerID, Tools.Prize.BURST );
                    prized = true;
                }
                // EMP ability; re-enable every 30 ticks (15 min)
                if( purchasedUpgrades[13] > 0 && !targetedEMP && tick % 30 == 0 ) {
                    m_botAction.showObjectForPlayer( arenaPlayerID, LVZ_EMP );
                    m_botAction.sendPrivateMessage(arenaPlayerID, "Targeted EMP recharged.  !emp to use.");
                    targetedEMP = true;
                }
            }
            else if( shipNum == 3) {
                // Energy tank ability; each level worth an additional 25%
                if( !energyTank ) {
                    double etChance = Math.random() * 100.0;
                    if( ((double)purchasedUpgrades[10] * 25.0) > etChance ) {
                        m_botAction.showObjectForPlayer( arenaPlayerID, LVZ_ENERGY_TANK );
                        m_botAction.sendPrivateMessage(arenaPlayerID, "Energy Tank replenished.  !! to use.");
                        energyTank = true;
                    }
                }
                // Energy stream ability; each level worth an additional 10%
                double superChance = Math.random() * 100.0;
                if( (double)purchasedUpgrades[11] * 9.0 > superChance && !isRespawning ) {
                    m_botAction.specificPrize( arenaPlayerID, Tools.Prize.SUPER );
                    m_botAction.showObjectForPlayer(arenaPlayerID, LVZ_SUPER );
                    prized = true;
                }
            } else if( shipNum == 8) {
                // Repel regen ability; each level worth an additional 25%
                double repChance = Math.random() * 4.0;
                if( (double)purchasedUpgrades[9] > repChance && !isRespawning ) {
                    m_botAction.specificPrize( arenaPlayerID, Tools.Prize.REPEL );
                    prized = true;
                }
            } else if( shipNum == 1) {
                // Thor ability (every 5 minutes)
                if( purchasedUpgrades[11] > 0 && tick % 10 == 0 ) {
                    m_botAction.sendPrivateMessage(arenaPlayerID, "Thor's Hammer available at next rearm." );
                    bonusPrize = Tools.Prize.THOR;
                    prized = true;
                }
            } else if( shipNum == 7 ) {
                // Firebloom ability.
                // Every 4 minutes with lvl1, every 2 minutes with lvl2, spawn with at lvl3
                if( firebloom > 0 ) {
                    if( (firebloom == 1 && tick % 8 == 0) ||
                        (firebloom == 2 && tick % 4 == 0) ) {
                        m_botAction.sendPrivateMessage(arenaPlayerID, "Firebloom available at next rearm." );
                        bonusPrize = Tools.Prize.BURST;
                        prized = true;
                    }
                }
            } else if( shipNum == 2) {
                // JumpSpace ability (free at rank 15, but doesn't work well)
                int neededTick;
                switch( purchasedUpgrades[9] ) {
                    case 1: neededTick = 15; break;     // 7.5min
                    case 2: neededTick = 10; break;     // 5m
                    case 3: neededTick = 8; break;      // 4m
                    default: neededTick = 20; break;    // 10m
                }
                if( tick % neededTick == 0 ) {
                    if( !jumpSpace ) {
                        m_botAction.showObjectForPlayer( arenaPlayerID, LVZ_JUMPSPACE );
                        m_botAction.sendPrivateMessage( arenaPlayerID, "JumpSpace Drive ready.  PM >>> to use." );
                        jumpSpace = true;
                        prized = true;
                    }
                }
            } else if( shipNum == 6) {
                // Prismatic Array ability; creates decoy array when used
                if( !prismatic ) {
                    double pmChance = Math.random() * 100.0;
                    if( ((double)purchasedUpgrades[14] * 15.0) > pmChance ) {
                        //m_botAction.showObjectForPlayer( arenaPlayerID, LVZ_ENERGY_TANK );  Make LVZ for this.
                        m_botAction.sendPrivateMessage(arenaPlayerID, "Prismatic Array replenished.  --- to use.");
                        prismatic = true;
                    }
                }

            } else if( shipNum == 9 ) {
                // Allow another Comm every minute, up to max allowed
                if( tick % 3 == 0 ) {
                    if( currentComms < DEFAULT_OP_MAX_COMMS ) {
                        m_botAction.sendPrivateMessage( arenaPlayerID, "+1 Comm Auth." );
                        currentComms++;
                    }
                }

                // Regenerate OP
                if( tick % 2 == 0 ) {
                    int increase = purchasedUpgrades[2] + DEFAULT_OP_REGEN;
                    if( currentOP < maxOP ) {
                        if( currentOP + increase > maxOP )
                            increase = maxOP - currentOP;
                        currentOP += increase;
                        m_botAction.sendPrivateMessage( arenaPlayerID, "+" + increase + " OP  ( " + currentOP + " / " + maxOP + " )" );
                    }
                }

                // Give report on Shark/Terr status every 5 min
                if( tick % 10 == 0 ) {
                    int terrs = 0;
                    int sharks = 0;
                    int others = 0;
                    int id = getArmyID();
                    for( DistensionPlayer p : m_players.values() ) {
                        if( p.getArmyID() == id ) {
                            if( p.getShipNum() == Tools.Ship.TERRIER ) {
                                terrs++;
                            } else if( p.getShipNum() == Tools.Ship.SHARK ) {
                                sharks++;
                            } else {
                                others++;
                            }
                        }
                    }
                    m_botAction.sendPrivateMessage(arenaPlayerID, "TEAM REPORT:  " + terrs + " Terrs, " + sharks + " Sharks; " + others + " others."  );
                }
            }
            if( prized )
                m_botAction.showObjectForPlayer( arenaPlayerID, LVZ_PRIZEDUP );
        }


        /**
         * Advances the player to the next rank.  Wrapper.
         */
        public void doAdvanceRank() {
            doAdvanceRank(1);
        }

        /**
         * Advances the player a certain number of ranks, default 1.
         * @param multiRanks Number of ranks to gain.  Default is 1
         */
        public void doAdvanceRank( int numRanks ) {
            if( rank >= 80 ) {
                m_botAction.sendPrivateMessage(arenaPlayerID, "-=(  FINAL RANK ATTAINED  )=-" );
                String shipname = ( shipNum == 9 ? "Tactical Ops" : Tools.shipName(shipNum) );
                m_botAction.sendPrivateMessage(arenaPlayerID, "YOU ARE NOW A MASTER " + shipname.toUpperCase() + ".  ALL WILL FOREVER REMEMBER THE NAME OF " + name.toUpperCase() + " ... CONGRATULATIONS!!!", Tools.Sound.VICTORY_BELL );
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

            // Add JumpSpace ability for Javs at rank 15
            if( shipNum == 2 && rank == 15 ) {
                m_botAction.sendPrivateMessage(arenaPlayerID, "As a rank 15 Javelin, you have unlocked the JumpSpace Drive.  It will be recharged shortly; PM >>> to use." );
                m_specialAbilityPrizer.addPlayer(this);
            }

            if( nextRank - rankPoints > 0 ) {
                String shipname = ( shipNum == 9 ? "Tactical Ops" : Tools.shipName(shipNum) );
                if( numRanks > 1 )
                    m_botAction.sendPrivateMessage(arenaPlayerID, "-=(  " + numRanks + " RANKS UP !!  )=-  You are now a RANK " + rank + " " + shipname + ".  Next rank in " + ( nextRank - rankPoints )+ " RP.", Tools.Sound.VICTORY_BELL );
                else
                    m_botAction.sendPrivateMessage(arenaPlayerID, "-=(  RANK UP!  )=-  You are now a RANK " + rank + " " + shipname + " pilot.  Next rank in " + ( nextRank - rankPoints )+ " RP.", Tools.Sound.VICTORY_BELL );
                m_botAction.showObjectForPlayer(arenaPlayerID, LVZ_RANKUP);
                m_botAction.sendPrivateMessage(arenaPlayerID, "Gained +" + numRanks + "0 UP for any !upgrade available in the !armory." + ((upgPoints > 1) ? ("  (" + upgPoints + " available)") : "") );
            } else {
                // Advanced more than one rank; refire the method
                doAdvanceRank( numRanks + 1 );
                return;
            }
            resetProgressBar();
            initProgressBar();

            if( rank >= RANK_REQ_SHIP2 ) {
                if( shipsAvail[1] == false ) {
                    m_botAction.sendPrivateMessage(arenaPlayerID, "You have proven yourself a capable enough to fly the Javelin.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(arenaPlayerID, "JAVELIN: The Javelin is a difficult ship to pilot, but one of the most potentially dangerous.  Users of the original Javelin model will feel right at home.  Our Javelins are extremely upgradeable, devastating other craft in high ranks.");
                    addShipToDB(2);
                }
            }
            if ( rank >= RANK_REQ_SHIP3 ) {
                if( shipsAvail[2] == false ) {
                    m_botAction.sendPrivateMessage(arenaPlayerID, "You have proven yourself a capable enough to fly the Spider.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(arenaPlayerID, "SPIDER: The Spider is the mainstay support gunner of every army and the most critical element of base defense.  Upgraded spiders receive regular refuelings, wormhole plugging capabilities, and 10-second post-rearmament energy streams.");
                    addShipToDB(3);
                }
            }
            if ( rank >= RANK_REQ_SHIP4 ) {
                if( shipsAvail[3] == false ) {
                    m_botAction.sendPrivateMessage(arenaPlayerID, "You have proven yourself a capable enough to fly the experimental Leviathan.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(arenaPlayerID, "LEVIATHAN: The Leviathan is an experimental craft, as yet untested.  It is unmaneuvarable but capable of great speeds -- in reverse.  Its guns are formidable, its bombs can cripple an entire base, but it is a difficult ship to master.");
                    addShipToDB(4);
                }
            }
            // BETA ONLY
            if ( rank >= RANK_REQ_SHIP6 ) {
                if( shipsAvail[5] == false ) {
                    m_botAction.sendPrivateMessage(arenaPlayerID, "You have proven yourself a capable enough to fly the Weasel.  One has been requisitioned for your use, and is now waiting in your !hangar.  (UNLOCKED BY RANK IN BETA ONLY)");
                    addShipToDB(6);
                }
            }
            if ( rank >= RANK_REQ_SHIP7 ) {
                if( shipsAvail[6] == false ) {
                    m_botAction.sendPrivateMessage(arenaPlayerID, "You have proven yourself a capable enough to fly the Lancaster.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(arenaPlayerID, "LANCASTER: The Lancaster is an unusual ship with a host of surprises onboard.  Pilots can upgrade its most basic components rapidly.  The Firebloom and the Lanc's evasive-bombing capability make this a fantastic choice for advanced pilots.");
                    addShipToDB(7);
                }
            }
            if ( rank >= RANK_REQ_SHIP8 ) {
                if( shipsAvail[7] == false ) {
                    m_botAction.sendPrivateMessage(arenaPlayerID, "You have proven yourself a capable enough to fly the Shark.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(arenaPlayerID, "SHARK: The Shark is piloted by our most clever and resourceful pilots.  Unsung heroes of the army, Sharks are both our main line of defense and leaders of every assault.  Advanced Sharks enjoy light gun capabilities and a cloaking device.");
                    addShipToDB(8);
                }
            }
            if ( rank >= RANK_REQ_SHIP9 ) {
                if( shipsAvail[8] == false ) {
                    boolean allShips = true;
                    for( int i = 1; i<8; i++ )
                        if( shipsAvail[i] == false )
                            allShips = false;

                    if( allShips && battlesWon >= WINS_REQ_RANK_ENSIGN ) {
                        try {
                            String query = "SELECT fnShipNum, fnRank FROM tblDistensionShip ship " +
                            "WHERE ship.fnPlayerID='" + dbPlayerID + "'";
                            ResultSet r = m_botAction.SQLQuery( m_database, query );
                            if( r == null ) {
                                Tools.printLog( "Null ResultSet returned for query: '" + query + "' on connection '" + m_database + "'" );
                                m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
                                return;
                            }
                            boolean allShipsAtRank = true;
                            while( r.next() && allShipsAtRank ) {
                                if( r.getInt("fnShipNum") != shipNum && r.getInt("fnRank") < RANK_REQ_SHIP9 )
                                    allShipsAtRank = false;
                            }
                            if( allShipsAtRank ) {
                                m_botAction.sendPrivateMessage(arenaPlayerID, "You have proven yourself a capable enough to provide battle support in a Tactical Ops position!  A shuttle craft that will take you to your operations terminal is now in your !hangar.  Use !manops to enter it.");
                                addShipToDB(9);
                            }
                            m_botAction.SQLClose(r);
                        } catch(SQLException e) {
                            Tools.printStackTrace( "Error getting ships from DB for adding Tactical Ops.", e );
                            m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
                        }
                    }
                }
            }
        }

        /**
         * Adds # rank points to total rank point amt.
         * @param points Amt to add
         * @return Number of actual points awarded, after debug factor and caps are applied
         */
        public int addRankPoints( int points ) {
            return addRankPoints( points, true );
        }

        /**
         * Adds # rank points to total rank point amt.
         * @param points Amt to add
         * @param limit True if limits should be applied (end of rank and 50% of rank caps)
         * @return Number of actual points awarded, after debug factor and caps are applied
         */
        public int addRankPoints( int points, boolean limit ) {
            if( shipNum < 1 )
                return 0;

            if( points > 0 ) {
                if( DEBUG )
                    points = (int)((float)points * DEBUG_MULTIPLIER);
                if( limit ) {
                    // Allow only 50% of a rank to be earned from any point increase.
                    if( points > (nextRank - rankStart) / 2 )
                        points = (nextRank - rankStart) / 2;
                }
                if( !sendKillMessages ) {
                    bonusBuildup += points / 100;
                    while( bonusBuildup > 1.0 ) {
                        points++;
                        bonusBuildup--;
                    }
                }
            }
            if( limit ) {
                // Max points allowed to be added is number of points needed to rank up.
                if( points > getPointsToNextRank() )
                    points = getPointsToNextRank();
            }

            rankPoints += points;
            recentlyEarnedRP += points;
            checkProgress();

            if( rankPoints >= nextRank ) {
                doAdvanceRank();
                if( limit ) // Limits only generally apply to kills, so is a fairly accurate gauge
                    rankedUpFromLastKill = true;
            }
            shipDataSaved = false;
            return points;
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
         * Adds a battle win, and checks if a player should be given an upgrade in army rank
         * because of this.
         */
        public void addBattleWin() {
            battlesWon++;
            String msg = "";
            boolean printToTeam = false;
            switch( battlesWon ) {
                case WINS_REQ_RANK_CADET_4TH_CLASS:
                case WINS_REQ_RANK_CADET_3RD_CLASS:
                case WINS_REQ_RANK_CADET_2ND_CLASS:
                case WINS_REQ_RANK_CADET_1ST_CLASS:
                    printToTeam = false;
                    msg = getPlayerRankString();
                    break;
                case WINS_REQ_RANK_ENSIGN:
                    m_botAction.sendPrivateMessage( arenaPlayerID, "You are now an Officer in " + getArmyName() + ", now accorded with all due privilege!" );
                case WINS_REQ_RANK_2ND_LIEUTENANT:
                case WINS_REQ_RANK_LIEUTENANT:
                case WINS_REQ_RANK_LIEUTENANT_COMMANDER:
                case WINS_REQ_RANK_COMMANDER:
                case WINS_REQ_RANK_CAPTAIN:
                case WINS_REQ_RANK_FLEET_CAPTAIN:
                    msg = getPlayerRankString();
                    break;
                case WINS_REQ_RANK_COMMODORE:
                    m_botAction.sendPrivateMessage( arenaPlayerID, "You are now a Flag Officer in " + getArmyName() + ", accorded with all due privilege!  Congratulations!" );
                case WINS_REQ_RANK_REAR_ADMIRAL:
                case WINS_REQ_RANK_VICE_ADMIRAL:
                case WINS_REQ_RANK_ADMIRAL:
                    msg = getPlayerRankString();
                    break;
                case WINS_REQ_RANK_FLEET_ADMIRAL:
                    m_botAction.sendPrivateMessage( arenaPlayerID, "You are now the Fleet Admiral of " + getArmyName() + ", accorded with all due privilege!  Congratulations!" );
            }
            if( !msg.equals("") ) {
                if( printToTeam )
                    m_botAction.sendOpposingTeamMessageByFrequency( getArmyID(), name.toUpperCase() + " has been promoted to " + msg.toUpperCase() + " for their commendable efforts!" );
                else
                    if( battlesWon == WINS_REQ_RANK_CADET_1ST_CLASS )
                        m_botAction.sendPrivateMessage( arenaPlayerID, "Congratulations, you have been promoted to " + msg.toUpperCase() + " for your fine efforts in securing the victory.  Rumor has it that soon you'll be promoted to an Officer!" );
                    else
                        m_botAction.sendPrivateMessage( arenaPlayerID, "Congratulations, you have been promoted to " + msg.toUpperCase() + " for your fine efforts in securing the victory.  Keep up the good work, Cadet." );
            }
        }

        /**
         * @return String of player's rank, based on their number of battle wins
         */
        public String getPlayerRankString() {
            if( battlesWon >= WINS_REQ_RANK_FLEET_ADMIRAL )
                return "Fleet Admiral";
            if( battlesWon >= WINS_REQ_RANK_ADMIRAL )
                return "Admiral";
            if( battlesWon >= WINS_REQ_RANK_VICE_ADMIRAL )
                return "Vice Admiral";
            if( battlesWon >= WINS_REQ_RANK_REAR_ADMIRAL )
                return "Rear Admiral";
            if( battlesWon >= WINS_REQ_RANK_COMMODORE )
                return "Commodore";
            if( battlesWon >= WINS_REQ_RANK_FLEET_CAPTAIN )
                return "Fleet Captain";
            if( battlesWon >= WINS_REQ_RANK_CAPTAIN )
                return "Captain";
            if( battlesWon >= WINS_REQ_RANK_COMMANDER )
                return "Commander";
            if( battlesWon >= WINS_REQ_RANK_LIEUTENANT_COMMANDER )
                return "Lieutenant Commander";
            if( battlesWon >= WINS_REQ_RANK_LIEUTENANT )
                return "Lieutenant";
            if( battlesWon >= WINS_REQ_RANK_2ND_LIEUTENANT )
                return "2nd Lieutenant";
            if( battlesWon >= WINS_REQ_RANK_ENSIGN )
                return "Ensign";
            if( battlesWon >= WINS_REQ_RANK_CADET_1ST_CLASS )
                return "Cadet 1st Class";
            if( battlesWon >= WINS_REQ_RANK_CADET_2ND_CLASS )
                return "Cadet 2nd Class";
            if( battlesWon >= WINS_REQ_RANK_CADET_3RD_CLASS )
                return "Cadet 3rd Class";
            if( battlesWon >= WINS_REQ_RANK_CADET_4TH_CLASS )
                return "Cadet 4th Class";
            return "Recruit";
        }


        /**
         * @return Number of wins required for next command rank
         */
        public int getWinsRequiredForNextCommandRank() {
            if( battlesWon >= WINS_REQ_RANK_FLEET_ADMIRAL )
                return -1;
            if( battlesWon >= WINS_REQ_RANK_ADMIRAL )
                return WINS_REQ_RANK_FLEET_ADMIRAL - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_VICE_ADMIRAL )
                return WINS_REQ_RANK_ADMIRAL - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_REAR_ADMIRAL )
                return WINS_REQ_RANK_VICE_ADMIRAL - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_COMMODORE )
                return WINS_REQ_RANK_REAR_ADMIRAL - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_FLEET_CAPTAIN )
                return WINS_REQ_RANK_COMMODORE - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_CAPTAIN )
                return WINS_REQ_RANK_FLEET_CAPTAIN - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_COMMANDER )
                return WINS_REQ_RANK_CAPTAIN - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_LIEUTENANT_COMMANDER )
                return WINS_REQ_RANK_COMMANDER - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_LIEUTENANT )
                return WINS_REQ_RANK_LIEUTENANT_COMMANDER - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_2ND_LIEUTENANT )
                return WINS_REQ_RANK_LIEUTENANT - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_ENSIGN )
                return WINS_REQ_RANK_2ND_LIEUTENANT - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_CADET_1ST_CLASS )
                return WINS_REQ_RANK_ENSIGN - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_CADET_2ND_CLASS )
                return WINS_REQ_RANK_CADET_1ST_CLASS - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_CADET_3RD_CLASS )
                return WINS_REQ_RANK_CADET_2ND_CLASS - battlesWon;
            if( battlesWon >= WINS_REQ_RANK_CADET_4TH_CLASS )
                return WINS_REQ_RANK_CADET_3RD_CLASS - battlesWon;
            return WINS_REQ_RANK_CADET_4TH_CLASS - battlesWon;

        }

        /**
         * Modifies the value of a particular upgrade.
         */
        public boolean modifyUpgrade( int upgrade, int amt ) {
            if( upgrade < 0 || upgrade > NUM_UPGRADES)
                return false;
            if( purchasedUpgrades[upgrade] + amt < 0 )
                return false;
            purchasedUpgrades[upgrade] += amt;
            setupSpecialAbilities();
            if( shipNum == 6 && upgrade == 6 )
                vengefulBastard = purchasedUpgrades[6];
            if( (shipNum == 1 || shipNum == 5) && upgrade == 12 )
                escapePod = purchasedUpgrades[12];
            if( shipNum == 7 && upgrade == 6 )
                leeching = purchasedUpgrades[6];
            if( shipNum == 1 && upgrade == 10 )
                masterDrive = purchasedUpgrades[10];
            if( shipNum == 7 && upgrade == 10 )
                firebloom = purchasedUpgrades[10];
            if( shipNum == 9 && upgrade == 1 )
                maxOP = (purchasedUpgrades[1] * 2) + DEFAULT_MAX_OP;
            shipDataSaved = false;
            return true;
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
                if( this.shipNum > 0 && this.shipNum != 9 ) {
                    m_botAction.specWithoutLock( arenaPlayerID );
                    lastShipNum = this.shipNum;     // Record for lagout
                } else {
                    lastShipNum = -1;               // Ensure lagout is not possible
                    setLagoutAllowed(false);
                }
                turnOffProgressBar();
            } else
                if( this.shipNum == 0 )
                    turnOnProgressBar();

            if( shipNum == 9 ) {
                setLagoutAllowed(false);
                m_botAction.specWithoutLock( getArenaPlayerID() );
                m_botAction.setFreq( arenaPlayerID, getArmyID() );
            }
            this.shipNum = shipNum;
            isRespawning = false;
            successiveKills = 0;
            recentlyEarnedRP = 0;
            currentOP = 0;
            currentComms = 0;
            vengefulBastard = 0;
            escapePod = 0;
            leeching = 0;
            lastX = 0;
            lastY = 0;
        }

        /**
         * Puts player in the currently-set ship and sets them as having respawned.
         */
        public void putInCurrentShip() {
            Player p = m_botAction.getPlayer(getArenaPlayerID());
            if( p.getFrequency() != getArmyID() )
                m_botAction.setFreq( arenaPlayerID, getArmyID() );
            isRespawning = false;
            resetProgressBar();
            initProgressBar();
            setupSpecialAbilities();
        }

        /**
         * Setup special abilities according to what has been upgraded.
         */
        public void setupSpecialAbilities() {
            if( (shipNum == 3 && (purchasedUpgrades[10] > 0 || purchasedUpgrades[11] > 0)) ||
                    (shipNum == 5 && (purchasedUpgrades[11] > 0 || purchasedUpgrades[13] > 0)) ||
                    (shipNum == 8 && (purchasedUpgrades[9] > 0) ) ||
                    (shipNum == 2 && ((purchasedUpgrades[9] > 0) || rank >= 15) ) ||
                    (shipNum == 1 && (purchasedUpgrades[11] > 0) ) ||
                    (shipNum == 6 && (purchasedUpgrades[14] > 0) ) ||
                    (shipNum == 7 && (purchasedUpgrades[10] > 0 && purchasedUpgrades[10] < 3)) ||
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
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcBanned='y' WHERE fnID='" + dbPlayerID + "'" );
                saveCurrentShipToDB();
                m_botAction.sendSmartPrivateMessage(name, "You have been forcefully discharged from your army, and are now considered a civilian.  You may no longer play Distension." );
                m_botAction.sendUnfilteredPrivateMessage( arenaPlayerID, "*kill" );
            } catch (SQLException e ) {
                Tools.printLog( "Error banning player " + name );
            }
        }

        /**
         * Unbans the player.
         */
        public void unban() {
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcBanned='n' WHERE fnID='" + dbPlayerID + "'" );
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
            if( successiveKills % 5 != 0 )
                return false;

            int award = 0;
            boolean isWeasel = (shipNum == Tools.Ship.WEASEL);
            if( successiveKills == 5 ) {
                if( isWeasel )
                    award = 3;
                else
                    award = 2;
                if( rank > 1 )
                    award = rank * 2;

                m_botAction.sendPrivateMessage( arenaPlayerID, "Streak!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.CROWD_OOO );
            } else if( successiveKills == 10 ) {
                if( isWeasel )
                    award = 4;
                else
                    award = 3;
                if( rank > 1 )
                    award = rank * 3;
                m_botAction.sendPrivateMessage( arenaPlayerID, "ON FIRE!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.CROWD_GEE );
            } else if( successiveKills == 15 ) {
                if( isWeasel )
                    award = 6;
                else
                    award = 4;
                if( rank > 1 )
                    award = rank * 4;
                m_botAction.sendPrivateMessage( arenaPlayerID, "UNSTOPPABLE!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.VIOLENT_CONTENT );
            } else if( successiveKills == 20 ) {
                if( shipsAvail[5] == false ) {
                    String query = "SELECT * FROM tblDistensionShip WHERE fnPlayerID='" + dbPlayerID + "' AND ship.fnShipNum='6'";

                    try {
                        ResultSet r = m_botAction.SQLQuery( m_database, query );
                        if( r == null ) {
                            Tools.printLog("Null ResultSet returned for retrieval of ship 6 (for adding) on player ID " + dbPlayerID );
                            return false;
                        }
                        if( r.next() ) {
                            m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcShip6='y' WHERE fnID='" + dbPlayerID + "'" );
                            m_botAction.sendPrivateMessage( arenaPlayerID, "For 20 successive kills, your Weasel has been returned to the hangar!");
                        } else {
                            m_botAction.sendPrivateMessage( arenaPlayerID, "AWARD FOR MASTERFUL DOGFIGHTING.  You are quite the pilot, and have proven yourself capable of joining our stealth operations.  The Weasel is now available in your !hangar." );
                            m_botAction.sendPrivateMessage( arenaPlayerID, "WEASEL: The Weasel heads Covert Operations, providing scout reconnaissance to the rest of the army.  Its small size and cloaking allows it a freedom no others have.  Our newest Weasels now have the ability to cut off pursuit instantly!");
                            addShipToDB(6);
                        }
                        m_botAction.SQLClose(r);
                        return true;
                    } catch (SQLException e ) {
                        Tools.printStackTrace( "Error getting ships from DB for adding Weasel.", e );
                        m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
                        return false;
                    }
                } else {
                    if( isWeasel )
                        award = 8;
                    else
                        award = 5;
                    if( rank > 1 )
                        award = rank * 5;
                    m_botAction.sendPrivateMessage( arenaPlayerID, "INCONCEIVABLE!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.INCONCEIVABLE );
                }
            } else if( successiveKills == 30 ) {
                if( isWeasel )
                    award = 9;
                else
                    award = 6;
                if( rank > 1 )
                    award = rank * 6;
                m_botAction.sendPrivateMessage( arenaPlayerID, "CONTROLLED BY ALIENS!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.REAGAN );
            } else if( successiveKills == 40 ) {
                if( isWeasel )
                    award = 10;
                else
                    award = 7;
                if( rank > 1 )
                    award = rank * 7;
                m_botAction.sendPrivateMessage( arenaPlayerID, "THE BANE OF SMALL CHILDREN!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.CRYING );
            } else if( successiveKills == 50 ) {
                if( isWeasel )
                    award = 15;
                else
                    award = 10;
                if( rank > 1 )
                    award = rank * 10;
                m_botAction.sendPrivateMessage(name, "YOU'RE PROBABLY CHEATING!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.SCREAM );
            } else if( successiveKills == 99 ) {
                if( isWeasel )
                    award = 20;
                else
                    award = 15;
                if( rank > 1 )
                    award = rank * 15;
                m_botAction.sendPrivateMessage(name, "99 KILLS -- ... ORGASMIC !!  (" + (DEBUG ? (int)(award * DEBUG_MULTIPLIER ) : award ) + " RP bonus.)", Tools.Sound.ORGASM_DO_NOT_USE );
            } else if( successiveKills == 100 ) {
                m_botAction.sendPrivateMessage(name, "Well, so you made it to 100 -- you want a cookie or something?..." );
            } else if( successiveKills == 500 ) {
                m_botAction.sendPrivateMessage(name, "Get a job." );
            }
            if( award > 0 ) {
                m_botAction.showObjectForPlayer(arenaPlayerID, LVZ_STREAK);
                addRankPoints(award, false);
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
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fnArmyID=" + armyID + " WHERE fnID='" + dbPlayerID + "'" );
            } catch (SQLException e) {
                m_botAction.sendPrivateMessage( arenaPlayerID, "ERROR CHANGING ARMY!  Report to a mod immediately!" );
            }
            m_botAction.sendPrivateMessage( arenaPlayerID, "So you're defecting to " + getArmyName().toUpperCase() + "?  Can't blame you.  You'll be pilot #" + getArmy().getPilotsTotal() + "." );
            m_botAction.sendOpposingTeamMessageByFrequency(oldarmy.getID(), "TRAITOR!  Villainous dog!  " + name.toUpperCase() + " has betrayed us all for " + getArmyName().toUpperCase() + " !!  Spare not this worm a gruesome death ...");
            m_botAction.sendOpposingTeamMessageByFrequency(armyID, "Glory be to " + getArmyName().toUpperCase() + "!  " + name.toUpperCase() + " has joined our ranks!  Welcome this brave new pilot.");
            if( shipNum > 0 )
                m_botAction.setFreq(arenaPlayerID, armyID);
        }

        /**
         * Checks player for idling, and docks them if they are idle too long.  Players can kill
         * the idle by speaking in pubchat or making kills.
         */
        public void checkIdleStatus() {
            // OPS: Always idle; they must use commands to reset idle counter
            if( shipNum == 9 ) {
                idleTicks++;
                if( idleTicks == OPS_IDLE_TICKS_BEFORE_DOCK - 3) {
                    m_botAction.sendPrivateMessage(arenaPlayerID, "You appear to be idle, and will be docked in " + (IDLE_FREQUENCY_CHECK * 3) + " seconds if you do not use an Ops command or say something in public chat.");
                    opsAFKNotifyTime = System.currentTimeMillis();
                } else if( idleTicks == OPS_IDLE_TICKS_BEFORE_DOCK - 1) {
                    m_botAction.sendPrivateMessage(arenaPlayerID, "You appear to be idle, and will be docked in " + (IDLE_FREQUENCY_CHECK * 1) + " seconds if you do not use an Ops command or say something in public chat.");
                    opsAFKNotifyTime = System.currentTimeMillis();
                } else if( idleTicks >= OPS_IDLE_TICKS_BEFORE_DOCK ) {
                    m_botAction.sendPrivateMessage(arenaPlayerID, "You have been docked for being idle at the Tactical Ops console." );
                    setLagoutAllowed(false);
                    doDock(this);
                    return;
                }
            }
            Player p = m_botAction.getPlayer(arenaPlayerID);
            if( p == null ) return;
            int currenty = p.getYTileLocation();
            int currentx = p.getXTileLocation();
            if( (currenty >= TOP_ROOF && currenty <= TOP_FR ) ||
                (currenty >= BOT_FR && currenty <= BOT_ROOF )) {
                idlesInBase++;
            }
            boolean idleinsafe = (currenty <= TOP_SAFE || currenty >= BOT_SAFE);
            boolean idle = idleinsafe;
            if( !idle ) {
                // Check for people sitting in the same spot, moving in a straight line w/o changing velocity,
                // and bouncing in the rocks without changing their rotation.
                if( lastX >= currentx - 3 && lastX <= currentx + 3 &&
                    lastY >= currenty - 3 && lastY <= currenty + 3 )
                    idle = true;
                else if( lastXVel == p.getXVelocity() && lastYVel == p.getYVelocity() )
                    idle = true;
                else if( lastRot == p.getRotation() )
                    idle = true;
            }
            if( shipNum > 0 && idle ) {
                idleTicks++;
                if( idleTicks == IDLE_TICKS_BEFORE_DOCK - 3)
                    if( idleinsafe )
                        m_botAction.sendPrivateMessage(arenaPlayerID, "You appear to be idle, and will be docked in " + (IDLE_FREQUENCY_CHECK * 3) + " seconds if you do not move out of safe or say something in public chat.");
                    else
                        m_botAction.sendPrivateMessage(arenaPlayerID, "You appear to be idle, and will be docked in " + (IDLE_FREQUENCY_CHECK * 3) + " seconds if you don't move away from your current location or say something in public chat.");
                if( idleTicks == IDLE_TICKS_BEFORE_DOCK - 1)
                    if( idleinsafe )
                        m_botAction.sendPrivateMessage(arenaPlayerID, "You appear to be idle, and will be docked in " + IDLE_FREQUENCY_CHECK + " seconds if you do not move out of safe or say something in public chat.");
                    else
                        m_botAction.sendPrivateMessage(arenaPlayerID, "You appear to be idle, and will be docked in " + IDLE_FREQUENCY_CHECK + " seconds if you don't move away from your current location or say something in public chat.");
                else if( idleTicks >= IDLE_TICKS_BEFORE_DOCK )
                    cmdDock(name, "");
            } else
                idleTicks = 0;
            lastX = currentx;
            lastY = currenty;
            lastXVel = p.getXVelocity();
            lastYVel = p.getYVelocity();
            lastRot = p.getRotation();
        }

        /**
         * Resets the idle counter.  Called whenever a player speaks or makes a kill.
         */
        public void resetIdle() {
            if( shipNum == 9 ) {
                if( System.currentTimeMillis() > opsAFKNotifyTime + 500 ) {
                    // Idle only counts if >500ms since bot notified player (not ?away automated)
                    idleTicks = 0;
                }
                opsAFKNotifyTime = 0;
            } else {
                idleTicks = 0;
            }
        }

        /**
         * Shares a portion of the RP "profits" earned in the last minute with support ships,
         * and to a lesser degree, with weasels.  Levis and especially weasels do not receive
         * as large a share of profit sharing as do sharks, terrs and tactical ops.  Players
         * who are idle for more than 2 minutes do not receive profit-sharing.
         * @param profits RP earned in the last minute by teammates.
         */
        public void shareProfits( int profits ) {
            if( isSupportShip() && idleTicks < 12 ) {
                float sharingPercent;
                float calcRank = Math.max(1.0f, (float)rank);
                // Stronger supports do not receive as much profitsharing
                if( shipNum == 6 || shipNum == 4 )
                    if( rank > 12 )
                        calcRank = 12.0f;
                if( shipNum == 5 || shipNum == 8 || shipNum == 9 )
                    if( rank > 40 )
                        calcRank = 40.0f;

                sharingPercent = calcRank / 10.0f;

                float baseTerrBonus = 0.0f;
                if( shipNum == 5 ) {
                    sharingPercent += purchasedUpgrades[8];
                    // If Terr has been in a base more than half of the time, award an additional bonus
                    if( idlesInBase * IDLE_FREQUENCY_CHECK > PROFIT_SHARING_FREQUENCY / 2 ) {
                        if( rank >= 70 )
                            baseTerrBonus = 5.0f;
                        else if( rank >= 50 )
                            baseTerrBonus = 4.0f;
                        else if( rank >= 40 )
                            baseTerrBonus = 3.0f;
                        else if( rank >= 30 )
                            baseTerrBonus = 2.5f;
                        else if( rank >= 20 )
                            baseTerrBonus = 2.0f;
                        else if( rank >= 15 )
                            baseTerrBonus = 1.5f;
                        else if( rank >= 10 )
                            baseTerrBonus = 1.0f;
                        else if( rank >= 5 )
                            baseTerrBonus = 0.75f;
                        else
                            baseTerrBonus = 0.5f;
                        // If in base 100% of the time, double the bonus
                        if( idlesInBase * IDLE_FREQUENCY_CHECK == PROFIT_SHARING_FREQUENCY )
                            baseTerrBonus *= 2;
                        // 75%, give 1.5x
                        else if( idlesInBase * IDLE_FREQUENCY_CHECK > (float)PROFIT_SHARING_FREQUENCY / 1.5f )
                            baseTerrBonus *= 1.5;
                        sharingPercent += baseTerrBonus;
                    }
                }
                if( shipNum == 9 )
                    sharingPercent += purchasedUpgrades[0];
                int shared = Math.round((float)profits * (sharingPercent / 100.0f ));
                if( shared > 0 ) {
                    shared = addRankPoints(shared); // Get actual points added
                    if( sendKillMessages ) {
                        if( baseTerrBonus > 0.0f ) {
                            m_botAction.sendPrivateMessage(arenaPlayerID, "Profit-sharing: +" + shared + "RP  (" + sharingPercent + "%)" +
                                "  [Awarded " + baseTerrBonus + "% BaseTerr bonus]" );
                        } else {
                            m_botAction.sendPrivateMessage(arenaPlayerID, "Profit-sharing: +" + shared + "RP  (" + sharingPercent + "%)" );
                        }
                    }
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

        /**
         * Checks if the Vengeful Bastard ability should fire, and if so, fires
         * it on the ID of the player provided.
         * @param killerID ID of player who killed the vengeful bastard
         * @return True if vengeful bastard fired
         */
        public boolean checkVengefulBastard( int killerID ) {
            if( vengefulBastard <= 0 )
                return false;
            double vengeChance = Math.random() * 100.0;
            if( (double)(vengefulBastard * 15) > vengeChance ) {
                double vengeType = Math.random() * 100.0;
                if( vengeType >= 99.0 ) {
                    m_botAction.specificPrize( killerID, Tools.Prize.ENGINE_SHUTDOWN_EXTENDED );
                    m_botAction.specificPrize( killerID, -Tools.Prize.GUNS );
                    m_botAction.specificPrize( killerID, -Tools.Prize.RECHARGE );
                    m_botAction.specificPrize( killerID, -Tools.Prize.RECHARGE );
                    m_botAction.specificPrize( killerID, -Tools.Prize.RECHARGE );
                    m_botAction.specificPrize( killerID, -Tools.Prize.FULLCHARGE );
                } else if( vengeType >= 96.0 ) {
                    m_botAction.specificPrize( killerID, Tools.Prize.WARP );
                    m_botAction.specificPrize( killerID, Tools.Prize.ENGINE_SHUTDOWN_EXTENDED );
                } else if( vengeType >= 93.0 )
                    m_botAction.specificPrize( killerID, -Tools.Prize.GUNS );
                else if( vengeType >= 90.0 )
                    m_botAction.specificPrize( killerID, -Tools.Prize.ENGINE_SHUTDOWN_EXTENDED );
                else if( vengeType >= 80.0 )
                    m_botAction.specificPrize( killerID, -Tools.Prize.FULLCHARGE );
                else if( vengeType >= 79.0 ) {
                    m_botAction.specificPrize( killerID, -Tools.Prize.TOPSPEED );
                    m_botAction.specificPrize( killerID, -Tools.Prize.TOPSPEED );
                    m_botAction.specificPrize( killerID, -Tools.Prize.TOPSPEED );
                } else if( vengeType >= 78.0 ) {
                    m_botAction.specificPrize( killerID, -Tools.Prize.THRUST );
                    m_botAction.specificPrize( killerID, -Tools.Prize.THRUST );
                    m_botAction.specificPrize( killerID, -Tools.Prize.THRUST );
                } else if( vengeType >= 77.0 ) {
                    m_botAction.specificPrize( killerID, -Tools.Prize.ROTATION );
                    m_botAction.specificPrize( killerID, -Tools.Prize.ROTATION );
                    m_botAction.specificPrize( killerID, -Tools.Prize.ROTATION );
                } else if( vengeType >= 76.0 ) {
                    m_botAction.specificPrize( killerID, -Tools.Prize.ENERGY );
                    m_botAction.specificPrize( killerID, -Tools.Prize.ENERGY );
                    m_botAction.specificPrize( killerID, -Tools.Prize.ENERGY );
                } else if( vengeType >= 75.0 ) {
                    m_botAction.specificPrize( killerID, -Tools.Prize.RECHARGE );
                    m_botAction.specificPrize( killerID, -Tools.Prize.RECHARGE );
                    m_botAction.specificPrize( killerID, -Tools.Prize.RECHARGE );
                } else if( vengeType >= 70.0 )
                    m_botAction.showObjectForPlayer( killerID, LVZ_OPS_BLIND1 );
                else if( vengeType >= 65.0 )
                    m_botAction.showObjectForPlayer( killerID, LVZ_OPS_SHROUD_SM );
                else if( vengeType >= 50.0 )
                    m_botAction.showObjectForPlayer( killerID, LVZ_OPS_SPHERE );
                else
                    m_botAction.specificPrize( killerID, Tools.Prize.ENGINE_SHUTDOWN );
                return true;
            }
            return false;
        }

        /**
         * Sets the last person to fire vengeful bastard on this player, with timestamp.
         * If the player is killed directly after the VB firing, the vengeful bastard
         * earns RP from the kill.
         */
        public void setVenge( String playerName ) {
            lastVenger = playerName;
            lastVengeTime = System.currentTimeMillis();
        }

        /**
         * Checks if vengeful bastard was fired on the player recently.
         * Only fire once.
         * @return Name of VB if within the time limit; null if not
         */
        public String checkVenge() {
            if( lastVenger != null ) {
                String lastTemp = null;
                if( lastVengeTime + (VENGEFUL_VALID_SECONDS * 1000) > System.currentTimeMillis() )
                    lastTemp = lastVenger;
                lastVenger = null;
                return lastTemp;
            } else {
                return null;
            }
        }

        /**
         * Checks if the Leeching ability should fire, and if so, prizes full charge.
         */
        public void checkLeeching() {
            if( leeching <= 0 )
                return;
            double leechChance = Math.random() * 100.0;
            if( (double)leeching * 15.0 > leechChance )
                m_botAction.specificPrize( arenaPlayerID, Tools.Prize.FULLCHARGE );
        }

        /**
         * Checks if the Master Drive ability should fire; gives super and/or shields.
         * 5 successive kills = MDlvl * 4%  (4%,  8%, 12%, 16%, 20%)
         * 6 successive kills = MDlvl * 5%  (5%, 10%, 15%, 20%, 25%)
         * 7 successive kills = MDlvl * 6%  (6%, 12%, 18%, 24%, 30%)
         */
        public void checkMasterDrive() {
            if( masterDrive <= 0 || successiveKills < 5 )
                return;
            double masterChance = Math.random() * 100.0;
            boolean fired = false;
            int masterMod = Math.min(35, (masterDrive * (successiveKills - 1)));  // Cap at 35%
            if( (double)masterMod > masterChance ) {
                m_botAction.specificPrize( arenaPlayerID, Tools.Prize.SUPER );
                m_botAction.specificPrize( arenaPlayerID, Tools.Prize.SHIELDS );
                fired = true;
            }
            if( fired && sendKillMessages )
                m_botAction.sendPrivateMessage( arenaPlayerID, "--=( ***   M.A.S.T.E.R. Drive ENABLED   *** )=--" );
        }

        /**
         * Checks if the player has an available energy tank, and if so, uses it to restore
         * the player to full energy.
         */
        public void useEnergyTank() {
            if( energyTank == false ) {
                m_botAction.sendPrivateMessage(arenaPlayerID, "You do not presently have an energy tank to use!" );
            } else {
                m_botAction.hideObjectForPlayer( arenaPlayerID, LVZ_ENERGY_TANK );
                m_botAction.specificPrize( arenaPlayerID, Tools.Prize.FULLCHARGE );
                energyTank = false;
            }
        }

        /**
         * Checks if the player has Targeted EMP available; if so, makes it unavailable and returns true.
         * @return True if TargetedEMP is available
         */
        public boolean useTargetedEMP() {
            boolean canemp = targetedEMP;
            targetedEMP = false;
            return canemp;
        }

        /**
         * Checks if the player has JumpSpace available; if so, makes it unavailable and returns true.
         * @return True if JumpSpace is available
         */
        public boolean useJumpSpace() {
            boolean canjump = jumpSpace;
            jumpSpace = false;
            return canjump;
        }

        /**
         * Checks if the player has an available Prismatic Array, and if so, uses it to
         * prize a number of decoys based on their level of upgrade (2 + level of upgrade).
         * The decoys are then deprized shortly afterward.
         */
        public void usePrismaticArray() {
            if( prismatic == false ) {
                m_botAction.sendPrivateMessage(arenaPlayerID, "You do not presently have a Prismatic Array to use!" );
            } else {
                if( personalTask != null ) {
                    try {
                        personalTask.cancel();
                    } catch( Exception e ) {
                        m_botAction.sendPrivateMessage(arenaPlayerID, "Sorry, you must wait a moment before using this ability." );
                        return;
                    }
                }
                int level = purchasedUpgrades[14];
                int delay;
                if( level == 3 )
                    delay = 10000;
                else if( level == 2 )
                    delay = 8000;
                else
                    delay = 6000;
                //m_botAction.hideObjectForPlayer( arenaPlayerID, LVZ_ENERGY_TANK );
                for( int i = 0; i < level + 2; i++ )
                    m_botAction.specificPrize( arenaPlayerID, Tools.Prize.DECOY );
                prismatic = false;
                personalTask = new TimerTask() {
                    public void run() {
                        for( int i = 0; i < purchasedUpgrades[14] + 2; i++ )
                            m_botAction.specificPrize( arenaPlayerID, -Tools.Prize.DECOY );
                    }
                };
                m_botAction.scheduleTask(personalTask, delay);
            }
        }

        /**
         * @return Seconds until Terrier can use summon ability next; 0 if available now.
         */
        public long timeUntilNextSummon( int permissions ) {
            int waitTime = getSummonDelay( permissions );
            long timeToNext = ((lastSummonTime + waitTime) - System.currentTimeMillis());
            if( timeToNext < 0 )
                return 0;
            return (timeToNext / 1000);
        }

        /**
         * Uses Terrier summon ability.
         * @return Seconds remaining until next summon can be used.
         */
        public int useSummon( int permissions ) {
            lastSummonTime = System.currentTimeMillis();
            return (getSummonDelay( permissions ) / 1000);
        }

        /**
         * @param permissions Permissions of Terr
         * @return Time to wait between summons
         */
        public int getSummonDelay( int permissions ) {
            int waitTime = 5 * Tools.TimeInMillis.MINUTE;
            if( permissions == 2 )
                waitTime = 3 * Tools.TimeInMillis.MINUTE;
            else if( permissions == 3 )
                waitTime = 1 * Tools.TimeInMillis.MINUTE;
            return waitTime;
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
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcSendKillMsg='" + (sendKillMessages?'y':'n') +"' WHERE fnID='" + dbPlayerID + "'" );
            } catch (SQLException e ) {
                Tools.printStackTrace( "Error getting ships from DB for adding Tactical Ops.", e );
                m_botAction.sendPrivateMessage( arenaPlayerID, DB_PROB_MSG );
            }
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
            if( shipNum == 9 )
                allowLagout = false;
            else
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

        /**
         * Sets the player to be ignored by the ship changer for changing to any ship; after
         * they change back to their original ship this is returned to false.
         * @oaran value True if ship changes should be ignored
         */
        public void setIgnoreShipChanges( boolean value ) {
            ignoreShipChanges = value;
        }

        /**
         * Increments number of lagouts.
         */
        public void incrementLagouts() {
            numLagouts++;
        }

        /**
         * Toggles between allowing and not allowing summoning.
         * @return Status of summoning allowance.
         */
        public boolean toggleSummon() {
            allowSummon = !allowSummon;
            return allowSummon;
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
        public int getDatabaseID() {
            return dbPlayerID;
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
         * @return True if player ranked up from their last kill (also resets counter)
         */
        public boolean didRankUpFromLastKill() {
            boolean ranked = rankedUpFromLastKill;
            rankedUpFromLastKill = false;
            return ranked;
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
         * @return True if the given ship is available (1 to 9).
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
            if( upgrade < 0 || upgrade >= NUM_UPGRADES)
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
         * @return True if player is a support ship (5, 6, 8, 9, or 4)
         */
        public boolean isSupportShip() {
            return (shipNum == 5 || shipNum == 8 || shipNum == 9 || shipNum == 6 || shipNum == 4);
        }

        /**
         * @return True if player is currently assisting an army and is not on their original one.
         */
        public boolean isAssisting() {
            return assistArmyID != -1;
        }

        /**
         * @return # successive kills player has made.
         */
        public int getSuccessiveKills() {
            return successiveKills;
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
            return allowLagout && lastShipNum > 0;
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

        /**
         * @return Total number of battles won (with 50%+ participation).
         */
        public int getBattlesWon() {
            return battlesWon;
        }

        /**
         * @return Whether or not player should respawn quickly.
         */
        public boolean isFastRespawn() {
            boolean should = fastRespawn;
            if( (getArmyID() == 0 && m_army0_fastRearm) || (getArmyID() == 1 && m_army1_fastRearm) )
                should = true;
            return should;
        }

        /**
         * @return True if all ship changes should be ignored until shipchange event fires with their original ship
         */
        public boolean ignoreShipChanges() {
            return ignoreShipChanges;
        }

        /**
         * @return True if player is over allowed # lagouts
         */
        public boolean isAtMaxLagouts() {
            return numLagouts >= LAGOUTS_ALLOWED;
        }

        /**
         * @return True if player allows Terrs to !summon them
         */
        public boolean doesAllowSummon() {
            return allowSummon;
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



    // ***** ARMY CLASS

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
        int highestNumPilotsThisShare;  // Highest # pilots this profit-sharing cycle
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
                if( r == null ) {
                    Tools.printLog("Null ResultSet returned for army data retrieval, ID: '" + armyID + "'");
                    return;
                }
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
            pilotsInGame = 0;
            highestNumPilotsThisShare = 0;
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
            if( pilotsInGame > highestNumPilotsThisShare )
                highestNumPilotsThisShare = pilotsInGame;
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
         * Gets profits earned this share cycle.  If the highest number of pilots during the cycle
         * was less than 15, the method make up for the difference between the highest number and 15
         * in order to be fair to hard-working support ships with families to feed.
         * @return Profits earned this share cycle, modified as necessary
         */
        public int getProfits() {
            float multiplier = 1.0f;
            if( highestNumPilotsThisShare < 15 )
                multiplier = 15.0f / (float)highestNumPilotsThisShare;
            // Don't be too generous.
            if( multiplier > 2.5f )
                multiplier = 2.5f;
            int share = Math.round((float)profitShareRP * multiplier);
            profitShareRP = 0;
            highestNumPilotsThisShare = 0;
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


    // ***** GENERIC INTERNAL DATA CLASSES

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
            if( cost == -1 ) {
                if( currentLevel >= costDefines.length ) {
                    Tools.printLog( "Attempted to access level " + currentLevel + " for upgrade " + name + "." );
                    m_botAction.sendArenaMessage("ERROR: Invalid level for upgrade " + name + ".  Notify dugwyler or staff immediately.");
                    return -1;
                }
                return costDefines[ currentLevel ];
            } else
                return cost;
        }
    }


    /**
     * Prize queuer, for preventing bot lockups.
     */
    private class PrizeQueue extends TimerTask {
        List <DistensionPlayer>priorityPlayers = Collections.synchronizedList( new LinkedList<DistensionPlayer>() );
        List <DistensionPlayer>players         = Collections.synchronizedList( new LinkedList<DistensionPlayer>() );
        int runs = 0;       // # times queue has run since last spawn tick
        int delayTillNextSpawn = 0;

        /**
         * Adds a player to the end of the prizing queue.
         * @param p Player to add
         */
        public void addPlayer( DistensionPlayer p ) {
            synchronized( players ) {
                if( !players.contains(p) )
                    players.add(p);
            }
        }

        /**
         * Adds a player to the head of the prizing queue.  For terrs and sharks
         * with the ability.
         * @param p Player to add
         */
        public void addHighPriorityPlayer( DistensionPlayer p ) {
            synchronized( priorityPlayers ) {
                if( !priorityPlayers.contains(p) )
                    priorityPlayers.add(p);
            }
        }

        /**
         * Adds a player to the prizing queue when priority rearm is enabled on their
         * army.  Only Terrs are forced to the head of the queue.
         * @param p Player to add
         */
        public void addPriorityRearmPlayer( DistensionPlayer p ) {
            synchronized( priorityPlayers ) {
                if( !priorityPlayers.contains(p) ) {
                    int index = priorityPlayers.size();
                    priorityPlayers.add(index, p);
                }
            }
        }

        /**
         * Removes a player from queue (special use only).
         * @param p Player to remove
         */
        public void removePlayer( DistensionPlayer p ) {
            synchronized( priorityPlayers ) {
                priorityPlayers.remove(p);
            }
            synchronized( priorityPlayers ) {
                players.remove(p);
            }
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
                    synchronized( priorityPlayers ) {
                        for( DistensionPlayer p : priorityPlayers )
                            p.doSpawnTick();
                    }
                // If another player is not in the process of being prized, attempt to spawn
                if( delayTillNextSpawn <= 0 ) {
                    synchronized( priorityPlayers ) {
                        DistensionPlayer currentPlayer = priorityPlayers.get(0);
                        spawned = currentPlayer.doSpawn();
                        if( spawned )
                            priorityPlayers.remove(0);
                    }
                }
            }
            if( players.isEmpty() )
                return;
            if( doTick )
                synchronized( players ) {
                    for( DistensionPlayer p : players )
                        p.doSpawnTick();
                }
            if( spawned )   // If high priority player was spawned, do not try to spawn normal player
                return;
            if( delayTillNextSpawn <= 0 ) {
                synchronized( players ) {
                    DistensionPlayer currentPlayer = players.get(0);
                    if( currentPlayer.doSpawn() )
                        players.remove(0);
                }
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

    private class RearmTask extends TimerTask {
        int id;
        int team;

        public void setID( int id ) { this.id = id; }
        public void setTeam( int team ) { this.team = team; }

        public void run() {
            m_botAction.hideObjectForPlayer( id, LVZ_OPS_FAST_REARM );
            if( team == 0 )
                m_army0_fastRearm = false;
            else
                m_army1_fastRearm = false;
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

        if( m_beginDelayedShutdown ) {
            cmdDie("", "shutdown");
            return;
        }
        String roundTitle = "";

        m_roundNum++;
        if( m_roundNum == 1 )
            roundTitle = "A new conflict";
        else
            roundTitle = "Battle " + m_roundNum;

        String warning = "";
        if( m_freq0Score >= SCORE_REQUIRED_FOR_WIN - 1 || m_freq1Score >= SCORE_REQUIRED_FOR_WIN - 1 )
            warning = "  VICTORY IS IMMINENT!!";
        m_botAction.sendArenaMessage( "FREE PLAY has ended.  " + roundTitle + " begins in " + getTimeString( INTERMISSION_SECS ) + ".  Score:  " + flagTimer.getScoreDisplay() + warning );
        m_botAction.sendChatMessage("The next round of Distension begins in " + getTimeString( INTERMISSION_SECS ) + ".  ?go #distension to play." );

        // Between rounds, switch between one and two flags
        int players = 0;
        for( DistensionPlayer p : m_players.values() )
            if( p.getShipNum() > 0 )
                players++;
        if( m_singleFlagMode ) {
            if( players >= PLAYERS_FOR_2_FLAGS ) {
                m_singleFlagMode = false;
                m_botAction.setDoors( 0 ); // All doors open
            }
        } else {
            if( players < PLAYERS_FOR_2_FLAGS ) {
                m_singleFlagMode = true;
                m_botAction.setDoors( 240 ); // All bottom doors closed
                for( DistensionPlayer p : m_players.values() )
                    if( p.getShipNum() == 9 ) {
                        m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "Only one base is now in contention; the Tactical Ops console will not be required for the next battle." );
                        doDock(p);
                    }
            }
        }

        // Toggle rules every other round.
        // TODO: Decide on which is best, or a better way to switch.  Maybe all "clinching" rounds
        // are done in a certain way?  Or switch off each war?
        if( m_flagRules == 0 )
            m_flagRules = 1;
        else
            m_flagRules = 0;

        if( m_roundNum == 1 )
            if( m_singleFlagMode )
                if( m_flagRules == 0 ) {
                    m_botAction.sendArenaMessage( "OBJECTIVE: Hold the single top flag for an unbroken " + getTimeString( getActualTimeNeededForFlagWin() ) + ".  Winning " + SCORE_REQUIRED_FOR_WIN + " battles more than the enemy will win the war." );
                } else {
                    m_botAction.sendArenaMessage( "OBJECTIVE: Hold the single top flag for a total " + getTimeString( getActualTimeNeededForFlagWin() ) + ".  Winning " + SCORE_REQUIRED_FOR_WIN + " battles more than the enemy will win the war." );
                }
            else
                if( m_flagRules == 0 ) {
                    m_botAction.sendArenaMessage( "OBJECTIVE: Hold both flags for an unbroken " + getTimeString( getActualTimeNeededForFlagWin() ) + ".  Winning " + SCORE_REQUIRED_FOR_WIN + " battles more than the enemy will win the war." );
                } else {
                    m_botAction.sendArenaMessage( "OBJECTIVE: Hold both flags for a total " + getTimeString( getActualTimeNeededForFlagWin() ) + ".  Winning " + SCORE_REQUIRED_FOR_WIN + " battles more than the enemy will win the war." );

                }
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
        int secs             = flagTimer.getTotalSecs();
        int minsToWin        = flagTimer.getTotalSecs() / 60;

        int opposingStrengthAvg = 1;
        int friendlyStrengthAvg = 1;
        float armyDiffWeight;
        HashMap <Integer,Integer>armyStrengths = flagTimer.getArmyStrengthSnapshots();

        if( winningArmyID == 0 || winningArmyID == 1 ) {
            if( winningArmyID == 0 )
                if( m_freq1Score > m_freq0Score )
                    m_freq1Score--;
                else
                    m_freq0Score++;
            else
                if( m_freq0Score > m_freq1Score )
                    m_freq0Score--;
                else
                    m_freq1Score++;

            if( m_freq0Score >= SCORE_REQUIRED_FOR_WIN || m_freq1Score >= SCORE_REQUIRED_FOR_WIN ) {
                m_botAction.sendArenaMessage( "THE CONFLICT IS OVER!!  " + m_armies.get(winningArmyID).getName() + " has laid total claim to the sector after " + m_roundNum + " battles.", Tools.Sound.HALLELUJAH );
                m_botAction.sendArenaMessage( "---(   Double points awarded for winning the war!   )---" );
                gameOver = true;
            } else {
                m_botAction.sendArenaMessage( "BATTLE " + m_roundNum + " ENDED: " + m_armies.get(winningArmyID).getName() + " gains control of the sector after " + getTimeString( flagTimer.getTotalSecs() ) +
                        ".  Score:  " + flagTimer.getScoreDisplay(), Tools.Sound.HALLELUJAH );
            }
        } else {
            m_botAction.sendArenaMessage( "END BATTLE: " + m_armies.get(winningArmyID).getName() + " gains control of the sector after " + getTimeString( flagTimer.getTotalSecs() ), Tools.Sound.HALLELUJAH );
            if( DEBUG )
                m_botAction.sendArenaMessage( "DEBUG: Unexpected Army ID:" + winningArmyID, Tools.Sound.HALLELUJAH );
            return;
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
        // Cap at 50 to keep extreme bonuses down
        if( minsToWin > 50 )
            minsToWin = 50;

        // Points to be divided up by army
        float totalPoints = (float)(minsToWin * 0.5f) * (float)opposingStrengthAvg * armyDiffWeight;
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
        if( percentSupport < 0.5f ) {
            percentSupport = percentSupport * SUPPORT_RATE;
        } else {
            if( percentSupport != 1.0f ) {
                // >=40% support (without everyone being support) means support gets capped max of 40% * support rate
                percentSupport = 0.5f * SUPPORT_RATE;
            }
        }
        supportPoints = Math.round( totalPoints * percentSupport );
        percentAttack = 1.0f - percentSupport;
        attackPoints = Math.round( totalPoints * percentAttack );
        int totalLvls = Math.round(totalLvlSupport + totalLvlAttack);

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
            if( p.getArmyID() == winningArmyID ) {
                if( p.getShipNum() > 0 ) {
                    playerRank = p.getRank();
                    if( playerRank == 0 )
                        playerRank = 1;
                    if( p.isSupportShip() )
                        points = (float)supportPoints * ((float)playerRank / totalLvlSupport);
                    else
                        points = (float)attackPoints * ((float)playerRank / totalLvlAttack);
                    Integer time = m_playerTimes.get( p.getName() );
                    if( time != null ) {
                        float percentOnFreq = (float)(secs - time) / (float)secs;
                        int modPoints = Math.max(1, Math.round(points * percentOnFreq) );
                        String victoryMsg;
                        if( gameOver ) {
                            modPoints *= 2;
                            victoryMsg = "HQ awards you " + (int)(DEBUG ? modPoints * DEBUG_MULTIPLIER : modPoints ) + "RP (double) for the final victory (" + (int)(percentOnFreq * 100) + "% participation)" + ( avarice ? " [-75% for avarice]" : "" ) ;
                        } else {
                            victoryMsg = "HQ awards you " + (int)(DEBUG ? modPoints * DEBUG_MULTIPLIER : modPoints ) + "RP for the victory (" + (int)(percentOnFreq * 100) + "% participation)" + ( avarice ? " [-75% for avarice]" : "" );
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
                        // Need 50% participation or more for the win to count properly.
                        if( percentOnFreq >= .5 )
                            p.addBattleWin();

                        m_botAction.sendPrivateMessage(p.getArenaPlayerID(), victoryMsg );
                        modPoints += bonus;
                        p.addRankPoints(modPoints,false);
                    } else {
                        if( DEBUG )
                            m_botAction.sendSmartPrivateMessage("dugwyler", p.getName() + " had no time data attached to their name at round win." );
                    }
                }
            } else {
                // For long battles, losers receive about 50% of the award of the winners.
                if( minsToWin >= 15 ) {
                    if( p.getShipNum() > 0 ) {
                        playerRank = p.getRank();
                        if( playerRank == 0 )
                            playerRank = 1;
                        points = totalPoints * ((float)playerRank / (float)totalLvls);
                        points /= 2;
                        Integer time = m_playerTimes.get( p.getName() );
                        if( time != null ) {
                            float percentOnFreq = (float)(secs - time) / (float)secs;
                            int modPoints = Math.max(1, Math.round(points * percentOnFreq) );
                            m_botAction.sendPrivateMessage(p.getArenaPlayerID(), "You lost the battle, but fought courageously.  HQ has given you a bonus of " + modPoints + "RP (" + (int)(percentOnFreq * 100) + "% participation).");
                            p.addRankPoints(modPoints,false);
                        }
                    }
                }
            }
        }
        m_botAction.sendArenaMessage( "Lead Defense: " + topBreaker + " [" + topBreaks + " breaks]  ...  Lead Assault: " + topHolder + " [" + topHolds + " holds]" );

        endRoundCleanup( gameOver );
    }


    /**
     * Ends a battle as a stalemate.
     * After, sets up an intermission, followed by a new battle.
     */
    private void doEndRoundAsStalemate( ) {
        if( !flagTimeStarted || flagTimer == null )
            return;

        int secs             = flagTimer.getTotalSecs();
        int minsToWin        = flagTimer.getTotalSecs() / 60;
        int freq0Time        = flagTimer.getFreq0TotalSecs();
        int freq1Time        = flagTimer.getFreq1TotalSecs();
        HashMap <Integer,Integer>armyStrengths = flagTimer.getArmyStrengthSnapshots();

        int strengthAvg0 = 1;
        int strengthAvg1 = 1;
        m_botAction.sendArenaMessage( "END BATTLE: STALEMATE!  The armies have called a truce after " + getTimeString(secs) + ".", Tools.Sound.HALLELUJAH );

        float strCurrent0;
        float strCurrent1;
        strCurrent0 = m_armies.get(0).getTotalStrength();
        strCurrent1 = m_armies.get(1).getTotalStrength();
        if( strCurrent0 <= 0 ) strCurrent0 = 1;
        if( strCurrent1 <= 0 ) strCurrent1 = 1;

        strengthAvg0 = armyStrengths.get( 0 ) / minsToWin;
        strengthAvg1 += armyStrengths.get( 1 ) / minsToWin;

        if( strengthAvg0 == 0 )
            strengthAvg0 = 1;
        if( strengthAvg1 == 0 )
            strengthAvg1 = 1;

        // Points to be divided up by army
        float totalPoints = (float)(minsToWin * 0.5f) * (strengthAvg0 + strengthAvg1);
        float totalLvlSupport = 0;
        float totalLvlAttack = 0;
        float numSupport = 0;
        float numAttack = 0;
        Iterator <DistensionPlayer>i = m_players.values().iterator();

        while( i.hasNext() ) {
            DistensionPlayer p = i.next();
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
        if( totalLvlSupport < 1.0f) totalLvlSupport = 1.0f;
        if( totalLvlAttack < 1.0f) totalLvlAttack = 1.0f;
        float percentSupport = numSupport / ( numSupport + numAttack );
        float percentAttack;
        // If support makes up 40% or less of the team, support cut is percentage * support rate
        int supportPoints;
        int attackPoints;
        if( percentSupport < 0.5f ) {
            percentSupport = percentSupport * SUPPORT_RATE;
        } else {
            if( percentSupport != 1.0f ) {
                // >=40% support (without everyone being support) means support gets capped max of 40% * support rate
                percentSupport = 0.5f * SUPPORT_RATE;
            }
        }
        supportPoints = Math.round( totalPoints * percentSupport );
        percentAttack = 1.0f - percentSupport;
        attackPoints = Math.round( totalPoints * percentAttack );

        // For display purposes only
        float attack, support, combo;
        attack = attackPoints;
        support = supportPoints;
        combo = attackPoints + supportPoints;
        if( DEBUG ) {
            attack *= DEBUG_MULTIPLIER;
            support *= DEBUG_MULTIPLIER;
            combo *= DEBUG_MULTIPLIER;
        }

        // Figure out percentage to award
        float freq0Cut = freq0Time / ( freq0Time + freq1Time );
        float freq1Cut = 1.0f - freq0Cut;

        m_botAction.sendArenaMessage( "Stalemate Award: " + (int)combo + "RP  ...  Split: " + (freq0Cut * 100) + "% to army 0, " + (freq1Cut * 100) + "% to army 1.");

        i = m_players.values().iterator();
        int playerRank = 0;
        String topHolder = "N/A", topBreaker = "N/A";
        int topHolds = 0, topBreaks = 0;
        float points = 0;
        while( i.hasNext() ) {
            DistensionPlayer p = i.next();
            if( p.getShipNum() > 0 ) {
                playerRank = p.getRank();
                if( playerRank == 0 )
                    playerRank = 1;
                if( p.isSupportShip() )
                    points = (float)supportPoints * ((float)playerRank / totalLvlSupport);
                else
                    points = (float)attackPoints * ((float)playerRank / totalLvlAttack);
                if( p.getArmyID() == 0 )
                    points = ((float)points * freq0Cut);
                else
                    points = ((float)points * freq1Cut);
                Integer time = m_playerTimes.get( p.getName() );
                if( time != null ) {
                    float percentOnFreq = (float)(secs - time) / (float)secs;
                    int modPoints = Math.max(1, Math.round(points * percentOnFreq) );
                    String victoryMsg;
                    victoryMsg = "HQ awards you " + (int)(DEBUG ? modPoints * DEBUG_MULTIPLIER : modPoints ) + "RP for your efforts (" + (int)(percentOnFreq * 100) + "% participation)";
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
                    p.addRankPoints(modPoints, false);
                } else {
                    if( DEBUG )
                        m_botAction.sendSmartPrivateMessage("dugwyler", p.getName() + " had no time data attached to their name at round win." );
                }
            }
        }
        m_botAction.sendArenaMessage( "Lead Defense: " + topBreaker + " [" + topBreaks + " breaks]  ...  Lead Assault: " + topHolder + " [" + topHolds + " holds]" );

        endRoundCleanup( false );
    }


    /**
     * Performs tasks necessary to clean up before breaking to intermission.
     * @param gameOver True if the war has ended (someone won)
     */
    public void endRoundCleanup( boolean gameOver ) {
        // Start free play (delaying the intermission)
        // Take away one second so that when we set the timer, it doesn't look odd
        int intermissionTime;
        if( gameOver ) {
            intermissionTime = (INTERMISSION_SECS * 5000) - 1000;
            m_roundNum = 1;
            m_freq0Score = 0;
            m_freq1Score = 0;
        } else {
            intermissionTime = (INTERMISSION_SECS * 3000) - 1000;
        }

        try {
            flagTimer.endBattle();
            m_botAction.cancelTask(flagTimer);
            m_botAction.cancelTask(intermissionTimer);
            m_botAction.cancelTask(freePlayTimer);
        } catch (Exception e ) {
        }

        if( stopFlagTime ) {
            m_botAction.sendArenaMessage( "The war is over ... at least, for now." );
            m_roundNum = 0;
            m_freq0Score = 0;
            m_freq1Score = 0;
            flagTimeStarted = false;
            return;
        }

        if( m_beginDelayedShutdown ) {
            m_botAction.sendArenaMessage( "AUTOMATED SHUTDOWN INITIATED ...  Thank you for testing!" );
            cmdSaveData(m_botAction.getBotName(), "");
            intermissionTime = 5000;
        } else {
            freePlayTimer = new FreePlayTask();
            freePlayTimer.setTime(intermissionTime);
            m_botAction.scheduleTask( freePlayTimer, 15000 );

            doScores(15000);

            // Swap out players waiting to enter
            if( !m_waitingToEnter.isEmpty() ) {
                LinkedList <DistensionPlayer>removals = new LinkedList<DistensionPlayer>();
                for( DistensionPlayer waitingPlayer : m_waitingToEnter ) {
                    if( waitingPlayer.getShipNum() == -1 ) { // Make sure they're still waiting

                        DistensionPlayer highestPlayer = null;

                        for( DistensionPlayer p : m_players.values() ) {
                            if( p.getShipNum() >= 0 ) {
                                if( highestPlayer == null || p.getMinutesPlayed() > highestPlayer.getMinutesPlayed() )
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
                    } else {
                        removals.add(waitingPlayer);
                    }
                }
                for( DistensionPlayer p : removals )
                    m_waitingToEnter.remove(p);
                for( DistensionPlayer p : m_waitingToEnter )
                    m_botAction.sendPrivateMessage( p.getArenaPlayerID(), "No suitable slot could be found for you this battle.  Please continue waiting and you will be placed in as soon as possible." );
            }
        }
        if( intermissionTime >= 60000 && !DEBUG )
            m_botAction.setTimer( (intermissionTime + 1000) / 60000 );
        intermissionTimer = new IntermissionTask();
        m_botAction.scheduleTask( intermissionTimer, intermissionTime );
    }


    /**
     * Adds all players to the hashmap which stores the time, in flagTimer time,
     * when they joined their freq.
     */
    public void setupPlayerTimes() {
        m_playerTimes.clear();

        for( DistensionPlayer p : m_players.values() ) {
            if( p.getShipNum() > 0 )
                m_playerTimes.put( p.getName(), new Integer(0) );
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
     * Formats an integer time as a String.
     * @param time Time in seconds.
     * @return Formatted string in 0:00 format.
     */
    public String getTimeString( long time ) {
        if( time <= 0 ) {
            return "0:00";
        } else {
            long minutes = time / 60;
            long seconds = time % 60;
            return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        }
    }

    /**
     * Warps all players at round start, if they are not presently spawning.
     * Attempts to ensure that at least one Terrier is warped into home base
     * to capture the initial flag and provide an anchor point.
     */
    private void warpPlayers() {
        Iterator <DistensionPlayer>i = m_players.values().iterator();
        DistensionPlayer p;
        boolean terrInTopBase = false, terrInBotBase = false;
        while( i.hasNext() ) {
            p = i.next();
            if( p != null && p.getShipNum() > 0 && !p.isRespawning() ) {

                // Warp a Terrier to home base in dual flag mode
                if( p.getShipNum() == 5 && !m_singleFlagMode ) {
                    if( p.getArmyID() % 2 == 0 ) {
                        if( terrInTopBase == false ) {
                            m_botAction.warpTo(p.getArenaPlayerID(), 512, OPS_TOP_WARP4_Y );
                            terrInTopBase = true;
                        } else
                            p.doWarp( true );
                    } else {
                        if( terrInBotBase == false ) {
                            m_botAction.warpTo(p.getArenaPlayerID(), 512, OPS_BOT_WARP4_Y );
                            terrInBotBase = true;
                        } else
                            p.doWarp( true );
                    }
                } else if( p.getShipNum() != 9 ) {
                    p.doWarp( true );
                }
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
     * are in play before round starts.  Also removes all lagouts from all players.
     */
    private void refreshSupportShips() {
        Iterator <DistensionPlayer>i = m_players.values().iterator();
        DistensionPlayer p;
        while( i.hasNext() ) {
            p = i.next();
            p.numLagouts = 0;
            if( (p.isSupportShip() && p.getShipNum() != 9) ) {
                p.setIgnoreShipChanges(true);
                m_botAction.setShip( p.getArenaPlayerID(), 1 );
                m_botAction.setShip( p.getArenaPlayerID(), p.getShipNum() );
                m_botAction.shipReset( p.getArenaPlayerID() ); // Just in case, as we seem to have problems.
                if( !p.isRespawning() )
                    p.doSetupSpecialRespawn();
            }
        }
    }


    /**
     * Shows and hides scores (used at intermission only).
     * @param time Time after which the score should be removed
     */
    private void doScores( int time ) {
        int[] objs1 = {2000,(m_freq0Score<10 ? 60 + m_freq0Score : 50 + m_freq0Score), (m_freq0Score<10 ? 80 + m_freq1Score : 70 + m_freq1Score)};
        boolean[] objs1Display = {true,true,true};
        scoreDisplay = new AuxLvzTask(objs1, objs1Display);
        int[] objs2 = {2200,2000,(m_freq0Score<10 ? 60 + m_freq0Score : 50 + m_freq0Score), (m_freq0Score<10 ? 80 + m_freq1Score : 70 + m_freq1Score)};
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
            if( !DEBUG )
                m_botAction.setTimer(0);
            doIntermission();
            m_botAction.showObject(LVZ_INTERMISSION); //Shows intermission lvz
        }
    }

    private class FreePlayTask extends TimerTask {
        int time = 0;
        public void setTime( int time ) {
            this.time = time;
        }
        public void run() {
            m_botAction.sendArenaMessage( "FREE PLAY for the next " + getTimeString( (time + 1000) /1000 ) + ".  Flags have no RP bonus during this time.", Tools.Sound.VICTORY_BELL );
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
/*
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
                m_botAction.setShip(player.getArenaPlayerID(), player.getShipNum());
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
*/

    /**
     * This private class counts the consecutive flag time an individual team racks up.
     * Upon reaching the time needed to win, it fires the end of the round.
     */
    private class FlagCountTask extends TimerTask {
        int sectorHoldingArmyID, breakingArmyID, securingArmyID, advantageArmyID;
        int flagSecondsRequired, secondsHeld, totalSecs, breakSeconds, securingSeconds, preTimeCount;
        int freq0TotalSeconds, freq1TotalSeconds;
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
            advantageArmyID = -1;
            flagSecondsRequired = getActualTimeNeededForFlagWin();
            secondsHeld = 0;
            totalSecs = 0;
            breakSeconds = 0;
            securingSeconds = 0;
            freq0TotalSeconds = 0;
            freq1TotalSeconds = 0;
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
         * Called by FlagClaimed when BOTH flags have been claimed by an army
         * in normal 2-flag mode, or when the single flag is claimed in single mode.
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
                    claimBeingEstablished = false;
                    breakingArmyID = -1;
                    breakSeconds = 0;
                    return;
                } else {
                    claimBeingEstablished = false;
                    securingArmyID = -1;
                    securingSeconds = 0;
                    return; // If this army already holds, and no claim is being broken,
                            // either it's an unnecessary 2flag notify, or we're in 1flag, continuing
                }
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
            if( m_flagRules == 0 )
                secondsHeld = 0;
            m_botAction.showObject(LVZ_FLAG_CLAIMED); // Shows flag claimed lvz
            claimBeingBroken = false;
            claimBeingEstablished = false;
            securingSeconds = 0;
            securingArmyID = -1;
            DistensionPlayer p = m_players.get(securerName);
            flagObjs.showObject(LVZ_SECTOR_HOLD);
            m_botAction.manuallySetObjects( flagObjs.getObjects() );
            if( p != null ) {
                addSectorHold( p.getName() );
                m_botAction.sendArenaMessage( "SECTOR HOLD: " + p.getArmyName() + "(" + p.getArmyID() + ") - " + p.getName() );
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
            // it's not a sector break (unless in single flag mode)
            if( claimBeingEstablished ) {
                claimBeingEstablished = false;
                claimBeingBroken = false;
                securingArmyID = -1;
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

            if( m_singleFlagMode && DEBUG )
                m_botAction.sendArenaMessage( "ERROR!  Single flag mode found firing hold break!" );
        }

        /**
         * Called when a hold over the sector has been broken (when an army who was holding the
         * sector no longer holds it).
         *
         * With single-flag mode, this also entails a sector break.
         */
        public void doSectorBreak() {
            int remain = flagSecondsRequired - secondsHeld;
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
            claimBeingEstablished = false;
            breakSeconds = 0;
            breakingArmyID = -1;
            sectorHoldingArmyID = -1;
            if( m_flagRules == 0 )
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
            isRunning = false;
            flagTimerObjs.hideAllObjects();
            flagObjs.hideAllObjects();
            m_botAction.setObjects();
            m_botAction.manuallySetObjects( flagObjs.getObjects() );
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
            if( isRunning == false ) {
                if( m_roundNum == 1 )
                    return "The first battle of a new conflict is just about to begin.";
                else
                    return "We are currently in between battles (battle " + m_roundNum + " starting soon).  Score:  " + getScoreDisplay();
            }

            if( m_flagRules == 0 ) {
                if( sectorHoldingArmyID == -1 )
                    return "BATTLE " + m_roundNum + " Stats: NO-ONE holds the sector.  [Time: " + getTimeString( totalSecs ) + "]  Score:  " + getScoreDisplay();
                return "BATTLE " + m_roundNum + " Stats: " + m_armies.get(sectorHoldingArmyID).getName() + "(" + sectorHoldingArmyID + ") has held the sector for " + getTimeString(secondsHeld) +
                  ", needs " + getTimeString( flagSecondsRequired - secondsHeld ) + " more.  [Time: " + getTimeString( totalSecs ) + "]  Score:  " + getScoreDisplay();
            } else {
                String advantage = ( advantageArmyID == -1 ? "NO-ONE" : m_armies.get(advantageArmyID).getName() );
                String holder = ( sectorHoldingArmyID == -1 ? "NO-ONE" : m_armies.get(sectorHoldingArmyID).getName() );
                if( advantage.equals( holder ) )
                    return "BATTLE " + m_roundNum + " Stats: " + holder + " has advantage of " + getTimeString(secondsHeld) + ", holds the sector, and needs " + getTimeString( flagSecondsRequired - secondsHeld ) + " to win." +
                      "  [Time: " + getTimeString( totalSecs ) + "]  Score:  " + getScoreDisplay();
                return "BATTLE " + m_roundNum + " Stats: " + advantage + " has advantage of " + getTimeString(secondsHeld) + "; " +
                  holder + " holds the sector.  [Time: " + getTimeString( totalSecs ) + "]  Score:  " + getScoreDisplay();
            }
        }

        /**
         * @return Total number of seconds round has been running
         */
        public int getTotalSecs() {
            return totalSecs;
        }

        /**
         * @return Total seconds freq 0 held all necessary flags this round
         */
        public int getFreq0TotalSecs() {
            return freq0TotalSeconds;
        }

        /**
         * @return Total seconds freq 1 held all necessary flags this round
         */
        public int getFreq1TotalSecs() {
            return freq1TotalSeconds;
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
         * @return Formatted display of current scores.
         */
        public String getScoreDisplay() {
            String scoreDisplay = "0 [";
            for(int i=SCORE_REQUIRED_FOR_WIN; i>0; i--)
                if( i > m_freq0Score)
                    scoreDisplay += " ";
                else
                    scoreDisplay += "=";
            scoreDisplay += "|";
            for(int i=1; i<SCORE_REQUIRED_FOR_WIN+1; i++)
                if( i > m_freq1Score)
                    scoreDisplay += " ";
                else
                    scoreDisplay += "=";
            scoreDisplay += "] 1";
            return scoreDisplay;
        }

        public boolean isBeingBroken() {
            return claimBeingBroken;
        }


        /**
         * Timer running once per second that handles the starting of a round,
         * displaying of information updates every 5 minutes, the flag claiming
         * timer, and total flag holding time/round ends.
         */
        public void run() {
            if( isStarted == false ) {
                int roundNum = m_freq0Score + m_freq1Score + 1;
                if( preTimeCount == 0 ) {
                    m_botAction.sendArenaMessage( "The next battle is just beginning . . .", 1 );
                    refreshSupportShips();
                    safeWarp();
                    m_roundGettingStarted = true;
                    m_botAction.getEventRequester().request(EventRequester.PLAYER_POSITION);
                }
                preTimeCount++;

                if( preTimeCount >= 10 ) {
                    isStarted = true;
                    isRunning = true;
                    String battle = "BATTLE " + roundNum;
                    if( m_freq0Score >= SCORE_REQUIRED_FOR_WIN - 1 || m_freq1Score >= SCORE_REQUIRED_FOR_WIN - 1 )
                        battle = "THE DECISIVE " + battle;

                    if( m_flagRules == 0 )
                        m_botAction.sendArenaMessage( battle + " HAS BEGUN!  Capture " + (m_singleFlagMode ? "top flag" : "both flags" ) + " for an unbroken " + getTimeString( flagSecondsRequired ) + " to win the battle.", SOUND_START_ROUND );
                    else
                        m_botAction.sendArenaMessage( battle + " HAS BEGUN!  Capture " + (m_singleFlagMode ? "top flag" : "both flags" ) + " for a total of " + getTimeString( flagSecondsRequired ) + " to win the battle.", SOUND_START_ROUND );
                    resetAllFlagData();
                    setupPlayerTimes();
                    warpPlayers();
                    m_roundGettingStarted = false;
                    m_botAction.getEventRequester().decline(EventRequester.PLAYER_POSITION);
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
            if( (totalSecs % (5 * 60)) == 0 && ( flagSecondsRequired - secondsHeld > 30) ) {
                m_botAction.sendArenaMessage( getTimeInfo() );
                recordMajorStrengthSnapshot();
            }

            if( totalSecs == (SUDDEN_DEATH_MINUTES - 5) * 60 ) {
                m_botAction.sendArenaMessage( "-=(  WARNING!  )=-  Battle will end in STALEMATE in 5 minutes!!", 2 );
            }

            if( totalSecs >= SUDDEN_DEATH_MINUTES * 60 ) {
                endBattle();
                doEndRoundAsStalemate();
            }

            if( claimBeingBroken ) {
                breakSeconds++;
                if( breakSeconds >= SECTOR_CHANGE_SECONDS ) {
                    doSectorBreak();
                    return;
                }
                // For the first second the claim is being broken, add 3 seconds back on to the clock.
                // For every additional second, 1 second is added back on to the clock.
                // (This means an unsuccessful break is not a waste.)
                if( breakingArmyID != advantageArmyID ) {
                    if( breakSeconds == 1 )
                        secondsHeld -= 3;
                    else
                        secondsHeld -= 1;
                }
                if( secondsHeld < 0 )
                    secondsHeld = 0;
                do_updateTimer();
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


            // If the army holding the sector does not have advantage, we SUBTRACT time,
            //  as the time belongs to the holding army, and don't do standard timer checks.
            if( sectorHoldingArmyID != advantageArmyID && m_flagRules == 1 ) {
                secondsHeld -= flagHybridReversionRate;
                // If it reduces the time to 0, now the holding army has advantage
                if( secondsHeld <= 0 ) {
                    secondsHeld = 0;
                    advantageArmyID = sectorHoldingArmyID;
                    m_botAction.sendArenaMessage( m_armies.get(sectorHoldingArmyID).getName() + " now has the sector advantage!", 1 );
                }
                do_updateTimer();
                return;
            } else {
                secondsHeld++;
                if( sectorHoldingArmyID == 0 )
                    freq0TotalSeconds++;
                else if( sectorHoldingArmyID == 1 )
                    freq1TotalSeconds++;
                do_updateTimer();
            }

            if( secondsHeld >= flagSecondsRequired ) {
                endBattle();
                doEndRound();
            } else if( flagSecondsRequired - secondsHeld == 60 && m_flagRules == 1 ) {
                m_botAction.sendArenaMessage( m_armies.get(sectorHoldingArmyID).getName() + " will win the battle in 60 seconds." );
            } else if( flagSecondsRequired - secondsHeld == 30 ) {
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
                    m_botAction.sendOpposingTeamMessageByFrequency(sectorHoldingArmyID, "WARNING: Potential AVARICE; use !assist " + losingArmyID + " (rewarded; participation saved) or pilot lower-ranked ships!" );
                    lastAssistReward = 0;           // Ensure anyone changing gets a reward
                    checkForAssistAdvert = true;    // ... and advert.
                }
            } else if( flagSecondsRequired - secondsHeld == 10 ) {
                m_botAction.sendArenaMessage( m_armies.get(sectorHoldingArmyID).getName() + " will win the battle in 10 seconds . . .", SOUND_END_IS_NIGH );
            }
        }

        /**
         * Runs the LVZ-based timer.
         */
        private void do_updateTimer() {
            // Never wipe timer in hybrid flag mode, even when no army holds both flags
            if( m_flagRules == 1 && sectorHoldingArmyID == -1 )
                return;

            flagTimerObjs.hideAllObjects();
            if( sectorHoldingArmyID == -1 ) {
                m_botAction.setObjects();
                return;
            }
            int secsNeeded = flagSecondsRequired - secondsHeld;
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
     * -4:  Targeted EMP against all enemies (uses !emp command)
     * -5:  Super chance every 30 seconds, for spiders
     * -6:  Repel chance every 30 seconds, for sharks
     * -7:  Profit sharing (+1% of RP of each teammate's kills per level), for terrs / sharks
     *
     * Order of speed of rank upgrades (high speed to low speed, lower # being faster ranks):
     * Terr   - 10.4  (start)
     * Shark  - 11    (unlock @ 2)
     * Ops    - 12    (unlock w/ all ships @ 20 + officer status)
     * Lanc   - 14    (unlock @ 10)
     * Weasel - 14    (unlock by successive kills)
     * WB     - 15    (start)
     * Spider - 15    (unlock @ 5)
     * Jav    - 15.2  (unlock @ 15)
     * Levi   - 16    (unlock @ 20)
     *
     * Prize format: Name, Prize#, Cost([]), Rank([]), # upgrades
     */
    public void setupPrices() {

        // Ship 0 -- dummy (for ease of access)
        ShipProfile ship = new ShipProfile( -1, -1 );
        m_shipGeneralData.add( ship );

        ShipUpgrade upg;

        // WARBIRD -- starting ship
        // Med upg speed; rotation starts +1, energy has smaller spread, smaller max, & starts v. low
        // 4:  L2 Guns
        // 10: M.A.S.T.E.R. Drive 1
        // 17: Multi
        // 20: Escape Pod 1
        // 22: Decoy
        // 25: M.A.S.T.E.R. Drive 2
        // 30: Escape Pod 2
        // 31: L3 Guns
        // 34: M.A.S.T.E.R. Drive 3
        // 36: Mines L1
        // 40: XRadar
        // 42: Escape Pod 3
        // 45: Priority Rearm
        // 47: M.A.S.T.E.R. Drive 4
        // 50: Thor
        // 55: Escape Pod 4
        // 60: Mines L2
        // 65: M.A.S.T.E.R. Drive 5
        // 70: Escape Pod 5
        // 75: Escape Pod 6
        // 80: Mines L3
        ship = new ShipProfile( 0, 15f );
        //                                                    | <--- this mark and beyond is not seen for upg names
        upg = new ShipUpgrade( "Side Thrusters           [ROT]", Tools.Prize.ROTATION, new int[]{7,7,7,8,8,9,10,12}, 0, 8 );           // 20 x8
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Density Reduction Unit   [THR]", Tools.Prize.THRUST, new int[]{4,5,6,6,6,7, 7, 8, 8, 9, 9,10,10,11,11,12}, 0, 16 );            // 1  x16
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Drag Balancer            [SPD]", Tools.Prize.TOPSPEED, new int[]{5,5,5,6,6,6,7,7,7,8,8,9,9,10}, 0, 14 );          // 200 x14
        ship.addUpgrade( upg );
        int costs1[] =          { 10,11, 12,13, 16,  19,25,37, 50,60 };
        int rechargeLevels1[] = { 0,  0, 10, 0, 20,  25, 0, 0, 40, 0 };
        upg = new ShipUpgrade( "Regeneration Drives      [CHG]", Tools.Prize.RECHARGE, costs1, rechargeLevels1, 10 );     // 150 x10
        ship.addUpgrade( upg );
        //                          L2Mult            L3             L3 Multi
        //                      1150    1300    1450     1600    1750
        //         1000      1075   1225    1375    1525     1675     1825
        int costs1b[] =       {10,10,11, 13, 15,  21, 28, 35, 45, 50,  60 };
        int energyLevels1[] = { 0, 3, 5,  7, 15,  20, 25, 30, 35, 40,  45 };
        upg = new ShipUpgrade( "Microfiber Armor         [NRG]", Tools.Prize.ENERGY, costs1b, energyLevels1, 11 );    // 75 x11
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "High-Impact Cannon", Tools.Prize.GUNS, 12, 31, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Energy Concentrator", Tools.Prize.BOMBS, new int[]{10,20,30}, new int[]{36,60,80}, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Beam-Splitter", Tools.Prize.MULTIFIRE, 19, 17, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 35, 40, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Warbird Reiterator", Tools.Prize.DECOY, 14, 22, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "M.A.S.T.E.R. Drive Upgrade", ABILITY_MASTER_DRIVE, new int[]{10,12,14,16,20}, new int[]{10,25,34,47,65}, 5 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Matter-to-Antimatter Converter", ABILITY_THOR, 34, 50, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Escape Pod, +10% Chance", ABILITY_ESCAPE_POD, new int[]{15,20,25,30,35,40}, new int[]{20,30,42,55,70,75}, 6 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Priority Rearmament", ABILITY_PRIORITY_REARM, 20, 45, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Empty slot)", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // JAVELIN -- rank 15
        // Very slow upg speed; standard upgrades but recharge & energy have +2 max
        // 9:  L1 Guns
        // 15: Shrap (2 levels)
        // 20: Rocket 1
        // 24: XRadar
        // 26: Multi
        // 26: L2 Guns
        // 28: JumpSpace 1
        // 30: Shrap (3 more levels)
        // 35: Rocket 2
        // 37: JumpSpace 2
        // 40: L2 Bombs
        // 45: Priority Rearm
        // 50: JumpSpace 3
        // 55: Shrap (7 more levels)
        // 60: L3 Guns
        // 70: Rocket 3
        // 80: L3 Bombs
        ship = new ShipProfile( RANK_REQ_SHIP2, 15.2f );
        upg = new ShipUpgrade( "Balancing Streams        [ROT]", Tools.Prize.ROTATION, new int[]{8,9,9,9,10,10,11,12,13,15}, 0, 10 );       // 20 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Fuel Economizer          [THR]", Tools.Prize.THRUST, new int[]{4,5,6,7,7,8,8,9,9,12,15,20}, 0, 12 );        //  1 x12
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Engine Reoptimization    [SPD]", Tools.Prize.TOPSPEED, new int[]{4,4,5,5,6,6,7,8,9,10}, 0, 10 );       // 200 x10
        ship.addUpgrade( upg );
        int costs2a[] = {10,11,11,12,12,  27,13,14,15,20,  34, 68,100 };
        int p2a2[] =    { 0, 0, 0, 0, 0,  15, 0,25,35,45,  55, 70, 80 };
        upg = new ShipUpgrade( "Tactical Engineering     [CHG]", Tools.Prize.RECHARGE, costs2a, p2a2, 13 );         // 75 x13
        ship.addUpgrade( upg );
        int costs2b[] =       { 9,10, 11, 13, 13, 24, 15, 16, 16, 19, 27,100 };
        int energyLevels2[] = { 2, 5, 10, 15, 20, 25, 30, 35, 40, 45, 55, 70 };
        upg = new ShipUpgrade( "Reinforced Plating       [NRG]", Tools.Prize.ENERGY, costs2b, energyLevels2, 12 );  // 73 x12
        ship.addUpgrade( upg );
        int p2b1[] = { 28, 55 };
        int p2b2[] = { 26, 60 };
        upg = new ShipUpgrade( "Rear Defense System", Tools.Prize.GUNS, p2b1, p2b2, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Mortar Explosive Enhancement", Tools.Prize.BOMBS, 50, 40, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Modified Defense Cannon", Tools.Prize.MULTIFIRE, 18, 23, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Detection System", Tools.Prize.XRADAR, 21, 24, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "JumpSpace Drive Upgrade", ABILITY_JUMPSPACE, new int[]{20,10,10}, new int[]{28,37,50}, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Emergency Fuel Supply", Tools.Prize.ROCKET, new int[]{20,35,70}, new int[]{18,22,30}, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Splintering Mortar 1", Tools.Prize.SHRAPNEL, 9, 15, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Splintering Mortar 2", Tools.Prize.SHRAPNEL, new int[]{11,11,11,12,13,14,15,16}, new int[]{30,40,50,55,0,0,0,0}, 8 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Priority Rearmament", ABILITY_PRIORITY_REARM, 21, 45, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Empty slot)", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // SPIDER -- rank 4
        // Med upg speed; lvl req for thurst & speed; thrust perm+1; speed has wide spread; recharge has 20 lvls
        //  9: +15% Energy Tank 1
        // 15: +5% Super 1
        // 18: Decoy 1
        // 20: +15% Energy Tank 2
        // 25: +5% Super 2
        // 26: Multi (rear)
        // 29: Decoy 2
        // 30: XRadar
        // 33: +15% Energy Tank 3
        // 35: +5% Super 3
        // 38: Anti
        // 41: Priority Rearm
        // 45: +5% Super 4
        // 47: L2 Guns
        // 50: Decoy 3
        // 55: +5% Super 5
        // 60: +15% Energy Tank 4
        // 75: L3 Guns
        ship = new ShipProfile( RANK_REQ_SHIP3, 15f );
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
        int p3c1[] = {10,10,11,13,15,17,19, 20,20,22,  24,27,28,29,34,39,43,50,60,80 };
        int p3c2[] = { 0, 0, 0, 0, 0, 0, 0, 25, 0, 0,  50,50,50,60,60,70,70,80,80,80 };
        upg = new ShipUpgrade( "Recompensator            [CHG]", Tools.Prize.RECHARGE, p3c1, p3c2, 20 );     // 115 x20
        ship.addUpgrade( upg );
        int costs3[] =        {11,11,12,13,14,15,16,17,19,25,29,44 };
        int energyLevels3[] = { 0, 3, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50 };
        upg = new ShipUpgrade( "Molecular Shield         [NRG]", Tools.Prize.ENERGY, costs3, energyLevels3, 12 ); // 70 x12
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Rapid Disintigrator", Tools.Prize.GUNS, new int[]{40,190}, new int[]{47,75}, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombing ability disabled)", Tools.Prize.BOMBS, 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Split Projector", Tools.Prize.MULTIFIRE, 28, 26, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 25, 30, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Spider Reiterator", Tools.Prize.DECOY, new int[]{9,15,23}, new int[]{18,29,47}, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "+25% Energy Tank", ABILITY_ENERGY_TANK, new int[]{12,17,22,30}, new int[]{9,20,33,60}, 4 );
        ship.addUpgrade( upg );
        int p3f1[] = { 13, 15, 17, 19, 20 };
        int p3f2[] = { 15, 25, 35, 45, 55 };
        upg = new ShipUpgrade( "+9% Infinite Energy Stream", ABILITY_SUPER, p3f1, p3f2, 5 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Warp Field Stabilizer", Tools.Prize.ANTIWARP, 23, 38, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Priority Rearmament", ABILITY_PRIORITY_REARM, 21, 41, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Empty slot)", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // LEVIATHAN -- rank 20
        // Slow upg speed; thrust has 5 lvls, all costly; energy starts high, upg's slow, but high max;
        //                 speed has 7 lvls, upgrades fast; rotation has 25 levels, upg's slow;
        //                 energy does not have the rank requirements other ships have
        // 4:  L1 Bombs
        // 8:  L2 Guns
        // 15: L2 Bombs
        // 20: XRadar
        // 25: Stealth
        // 28: Portal
        // 30: Multi (close shotgun)
        // 35: L3 Guns
        // 40: Decoy
        // 48: L3 Bombs
        // 60: Prox
        // 70: Shrapnel
        ship = new ShipProfile( RANK_REQ_SHIP4, 16f );
        upg = new ShipUpgrade( "Gravitational Modifier   [ROT]", Tools.Prize.ROTATION, new int[]{9,5,6,9,10,10,11,12}, 0, 8 );       // 20 x8
        ship.addUpgrade( upg );
        int p4a1[] = {20, 30, 40, 50, 60 };
        int p4a2[] = { 8, 20, 40, 50, 60 };
        upg = new ShipUpgrade( "Force Thrusters          [THR]", Tools.Prize.THRUST, p4a1, p4a2, 5 );     // 4 x5
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Collection Drive         [SPD]", Tools.Prize.TOPSPEED, 15, 0, 7 );        //1000 x7
        ship.addUpgrade( upg );
        int costs4[] = { 9, 10, 12, 14, 15, 16,  18, 20, 22, 27, 30, 35 };
        upg = new ShipUpgrade( "Power Recirculator       [CHG]", Tools.Prize.RECHARGE, costs4, 0, 12 );   // 70 x12
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Carbon-Forced Armor      [NRG]", Tools.Prize.ENERGY, 10, 0, 20 );         // 60 x20
        ship.addUpgrade( upg );
        int p4b1[] = { 40 };
        int p4b2[] = { 35 };
        upg = new ShipUpgrade( "Spill Guns", Tools.Prize.GUNS, p4b1, p4b2, 1 );         // DEFINE
        ship.addUpgrade( upg );
        int p4c1[] = { 7, 20, 55 };
        int p4c2[] = { 4, 13, 48 };
        upg = new ShipUpgrade( "Chronos(TM) Disruptor", Tools.Prize.BOMBS, p4c1, p4c2, 3 );        // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Sidekick Cannons", Tools.Prize.MULTIFIRE, 16, 30, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 15, 20, 1 );
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
        upg = new ShipUpgrade( "(Empty slot)", 0, 0, 0, -1 );
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
        // 16: Portal 2
        // 20: Summoning Upgrade
        // 23: Profit-sharing 2
        // 25: Escape Pod 1
        // 29: Portal 3
        // 30: Burst 2 (HIGH cost)
        // 33: Profit-sharing 3
        // 35: Escape Pod 2
        // 36: L2 Guns
        // 40: Multi (slightly more forward than regular)
        // 40: (Can attach to other terrs)
        // 42: Portal 4
        // 43: Profit-sharing 4
        // 45: Escape Pod 3
        // 50: Targeted EMP (negative full charge to all of the other team)
        // 54: Portal 5
        // 53: Profit-sharing 5
        // 57: Escape Pod 4
        // 70: Portal 6
        // 75: Escape Pod 5
        // 80: Portal 7
        ship = new ShipProfile( 0, 10.4f );
        upg = new ShipUpgrade( "Correction Engine        [ROT]", Tools.Prize.ROTATION, 7, 0, 15 );         // 20 x15
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Interwoven Propulsor     [THR]", Tools.Prize.THRUST, 7, 0, 10 );           // 2 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Microspiral Drive        [SPD]", Tools.Prize.TOPSPEED, 7, 0, 16 );         // 325 x16
        ship.addUpgrade( upg );
        int costs5a[] = { 10, 13, 15, 18, 22, 26, 35, 40, 45, 50 };
        upg = new ShipUpgrade( "Hull Reconstructor       [CHG]", Tools.Prize.RECHARGE, costs5a, 0, 10 );   // 90 x10
        ship.addUpgrade( upg );
        int costs5b[] = { 10, 12, 13, 14, 15,  20, 23, 27, 30 };
        upg = new ShipUpgrade( "Hull Capacity            [NRG]", Tools.Prize.ENERGY, costs5b, 0, 9 );      // 75 x9
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Upgraded Defense Systems", Tools.Prize.GUNS, 28, 36, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Offensive Realignment", Tools.Prize.MULTIFIRE, 60, 40, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 5, 0, 1 );
        ship.addUpgrade( upg );
        int p5d1[] = {  8, 10, 14, 18, 20 };
        int p5d2[] = { 13, 23, 33, 43, 53 };
        upg = new ShipUpgrade( "+1% Profit Sharing", ABILITY_PROFIT_SHARING, p5d1, p5d2, 5 );
        ship.addUpgrade( upg );
        int p5a1[] = {12, 13, 14, 30 };
        int p5a2[] = { 7, 16, 29, 60 };
        upg = new ShipUpgrade( "Wormhole Creation Kit", Tools.Prize.PORTAL, p5a1, p5a2, 4 );        // DEFINE
        ship.addUpgrade( upg );
        int p5b1[] = { 6, 160 };
        int p5b2[] = { 2, 30 };
        upg = new ShipUpgrade( "Rebounding Burst", Tools.Prize.BURST, p5b1, p5b2, 2 );       // DEFINE
        ship.addUpgrade( upg );
        int p5c1[] = { 5, 10, 15, 20, 25, 30, 35, 40, 45, 50 };
        upg = new ShipUpgrade( "+9% Regeneration", ABILITY_TERR_REGEN, 12, p5c1, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Escape Pod, +10% Chance", ABILITY_ESCAPE_POD, new int[]{12,13,14,15,20}, new int[]{25,35,45,57,75}, 5 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Targeted EMP", ABILITY_TARGETED_EMP, 40, 50, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Improved Summoning Authorization", ABILITY_SUMMONING_AUTH, 10, 20, 1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // WEASEL -- Unlocked by 20(tweak?) successive kills w/o dying
        // Slow-medium upg speed; all upg start 2 levels higher than normal calcs, but 1st level (3rd) costs 2pts
        //                        and all have level requirements; special upgrades come somewhat early
        //  5: XRadar
        //  7: Rocket 1
        // 10: 10% Vengeful Bastard 1
        // 13: L2 Guns
        // 18: Multifire
        // 20: 10% Vengeful Bastard 2
        // 23: Cloak
        // 26: Prismatic Array 1
        // 29: Portal
        // 30: 10% Vengeful Bastard 3
        // 33: Stealth
        // 35: Prismatic Array 2
        // 38: L3 Guns
        // 42: Decoy
        // 45: 10% Vengeful Bastard 4
        // 46: Rocket 2
        // 50: Brick
        // 54: Prismatic Array 3
        // 55: 10% Vengeful Bastard 5
        // 60: Rocket 3
        ship = new ShipProfile( RANK_REQ_SHIP6, 14f );
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
        upg = new ShipUpgrade( "Time Distorter           [SPD]", Tools.Prize.TOPSPEED, p6a1c, p6a2c, 10 );      // 150 x10
        ship.addUpgrade( upg );
        int p6a1d[] = { 15,14,13, 12, 14, 18, 24, 29, 33, 40 };
        int p6a2d[] = { 5, 8, 10, 15, 20, 30, 40, 50, 60, 70 };
        upg = new ShipUpgrade( "Influx Recapitulator     [CHG]", Tools.Prize.RECHARGE, p6a1d, p6a2d, 10 );      //  75 x10
        ship.addUpgrade( upg );
        int p6a1e[] = { 15,16,17, 18, 19, 20, 30, 20, 20, 24, 27, 50, 60, 75 };
        int p6a2e[] = {  3, 5, 8, 10, 15, 20, 30, 40, 50, 55, 60, 65, 70, 75 };
        upg = new ShipUpgrade( "Cerebral Shielding       [NRG]", Tools.Prize.ENERGY, p6a1e, p6a2e, 14 );        //  60 x14
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Low Propulsion Cannons", Tools.Prize.GUNS, 32, 38, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "+15% Vengeful B*stard", ABILITY_VENGEFUL_BASTARD, new int[]{9,12,15,20}, new int[]{10,20,30,55}, 4 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Cannon Distributor", Tools.Prize.MULTIFIRE, 19, 18, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 8, 5, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Escape Tunnel", Tools.Prize.PORTAL, 33, 29, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Scrambler", Tools.Prize.STEALTH, 40, 33, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Light-Bending Unit", Tools.Prize.CLOAK, 25, 23, 1 );
        ship.addUpgrade( upg );
        int p6c1[] = {12, 21, 30 };
        int p6c2[] = { 9, 46, 60 };
        upg = new ShipUpgrade( "Assault Boosters", Tools.Prize.ROCKET, p6c1, p6c2, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Movement Inhibitor", Tools.Prize.BRICK, 39, 50, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Prismatic Array", ABILITY_PRISMATIC_ARRAY, new int[]{15,14,13}, new int[]{26,35,54}, 3 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // LANCASTER -- special unlock  (10 for beta)
        // Fast upgrade speed; all upgrades only get lanc to 120% of stock lanc, but energy has few level requirements.
        // 15: +20% Leeching 1
        // 20: Multifire
        // 24: XRadar
        // 26: Bombing special ability
        // 30: +20% Leeching 2
        // 38: L3 Guns
        // 40: +20% Leeching 3
        // 45: Firebloom 1
        // 50: +20% Leeching 4
        // 52: Firebloom 2
        // 55: Prox
        // 60: L2 Bombs
        // 65: Firebloom 3
        // 69: Shrap (10 levels)
        // 70: +20% Leeching 5
        // 80: Decoy
        ship = new ShipProfile( 10, 14f );       // Level 10 unlock: beta only
        upg = new ShipUpgrade( "Directive Realigner      [ROT]", Tools.Prize.ROTATION, new int[]{5,5,5,5,5}, 0, 5 );        //  20 x5
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "InitiaTek Burst Engine   [THR]", Tools.Prize.THRUST, new int[]{4,5,6,6,7, 7,8,9,9,10}, 0, 10 );         //   1 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Streamlining Unit        [SPD]", Tools.Prize.TOPSPEED, new int[]{3,5,5,5,5,6,6,5,7,7}, 0, 10 );        // 150 x10
        ship.addUpgrade( upg );
        int costs7[] = { 11, 12, 15, 16, 21, 22, 29, 35 };
        upg = new ShipUpgrade( "Pneumatic Refiltrator    [CHG]", Tools.Prize.RECHARGE, costs7, 0, 8 );        // 125 x8
        ship.addUpgrade( upg );
        int p7a1[] = { 0, 0, 5, 10, 15, 20, 25, 30 };
        upg = new ShipUpgrade( "Interlocked Deflector    [NRG]", Tools.Prize.ENERGY, costs7, p7a1, 8 );       //  75 x8
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Modernized Projector", Tools.Prize.GUNS, 20, 38, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "+15% Leeching", ABILITY_LEECHING, new int[]{15,18,20,22,30}, new int[]{15,30,40,50,70}, 5 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Magnified Output Force", Tools.Prize.MULTIFIRE, 23, 20, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 16, 24, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Lancaster Reiterator", Tools.Prize.DECOY, 50, 80, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "The Firebloom", ABILITY_FIREBLOOM, new int[]{32,15,50}, new int[]{45,52,65}, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Lancaster Special!", Tools.Prize.BOMBS, new int[]{21,80}, new int[]{26,60}, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Proximity Bomb Detonator", Tools.Prize.PROXIMITY, 42, 55, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Shrapnel", Tools.Prize.SHRAPNEL, 15, 69, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Empty slot)", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // SHARK -- rank 2
        // Very fast upg speed; rotation has small spread (+2start) and few upgrades; thrust starts 1 down
        //                      but has high max; energy starts high, has low level req initially, but
        //                      has high req later on (designed to give bomb capability early)
        // (Starts with 2 repels, so that repel 1 is actually third)
        //  4: Repel upgrade 1 (3 total)
        // 10: Priority Rearmament
        // 12: Guns
        // 17: XRadar
        // 22: Shrap 1
        // 20: +25% Repel Regen 1
        // 28: Multifire
        // 30: +25% Repel Regen 2
        // 34: Repel upgrade 2 (4 total)
        // 38: Cloak
        // 41: Shrap 2
        // 45: L2 Bombs
        // 50: +25% Repel Regen 3
        // 54: L2 Guns
        // 65: Shrap 3
        // 70: Repel upgrade 3 (5 total)
        // 80: +25% Repel Regen 4
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
        upg = new ShipUpgrade( "Light Charging Mechanism [CHG]", Tools.Prize.RECHARGE, costs8, 1, 11 );     //  75 x11
        ship.addUpgrade( upg );
        int p8b1[] = {10,10, 12, 14, 16, 20, 30, 80 };
        int p8b2[] = { 3, 5, 10, 20, 30, 40, 50, 75 };
        upg = new ShipUpgrade( "Projectile Slip Plates   [NRG]", Tools.Prize.ENERGY, p8b1, p8b2, 8 );       //  75 x8
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Emergency Defense Cannon", Tools.Prize.GUNS, new int[]{10,45}, new int[]{12,55}, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Plasma-Infused Weaponry", Tools.Prize.BOMBS, 35, 45, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Spreadshot", Tools.Prize.MULTIFIRE, 24, 28, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 15, 17, 1 );
        ship.addUpgrade( upg );
        int p8ca1[] = { 13, 15, 20, 55 };
        int p8ca2[] = { 20, 30, 50, 80 };
        upg = new ShipUpgrade( "+25% Repulsor Regeneration", ABILITY_SHARK_REGEN, p8ca1, p8ca2, 4 );
        ship.addUpgrade( upg );
        int p8c1[] = { 6, 70, 99 };
        int p8c2[] = { 4, 34, 70 };
        upg = new ShipUpgrade( "Gravitational Repulsor", Tools.Prize.REPEL, p8c1, p8c2, 3 );    // DEFINE
        ship.addUpgrade( upg );
        int p8d2[] = { 22, 41, 65 };
        upg = new ShipUpgrade( "Splintering Unit", Tools.Prize.SHRAPNEL, 11, p8d2, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Nonexistence Device", Tools.Prize.CLOAK, 55, 38, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Priority Rearmament", ABILITY_PRIORITY_REARM, 9, 10, 1 );   // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Empty slot)", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // TACTICAL OPS -- rank 25
        //  1: Comm 1 (normal msg)
        //  2: Fast rearm 1
        //  2: Comm 2 (PM)
        //  4: Warp 1 (lower base)
        //  7: Cover
        //  8: Profit Sharing 1
        //  9: Fast rearm 2
        // 10: Door Control 1 (basic)
        // 12: Orb/Sphere 1 (1 target)
        // 15: Shroud 1 (1 target, sm)
        // 16: Profit Sharing 2
        // 17: Fast rearm 3
        // 18: Comm 3 (Sabotage)
        // 19: Door Control 2 (FR)
        // 20: Orb/Sphere 2 (all in base)
        // 21: Warp 2 (FR entrance, roof)
        // 22: Shroud 2 (all in base, sm)
        // 23: Profit Sharing 3
        // 25: Blind 1
        // 30: Shroud 3 (1 target, LG)
        // 31: Door Control 3 (enemy)
        // 33: Blind 2
        // 35: Warp 3 (FR)
        // 37: Profit Sharing 4
        // 40: Shields (single player)
        // 46: Shroud 4 (all in base, LG)
        // 50: Blind 3
        // 52: Profit Sharing 5
        // 55: EMP Pulse
        // 60: Shields (all on team)
        ship = new ShipProfile( RANK_REQ_SHIP9, 12f );
        upg = new ShipUpgrade( "+1% Profit Sharing", ABILITY_PROFIT_SHARING, new int[]{10,10,12,14,18}, new int[]{8,16,23,37,52}, 5 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "+2 Maximum OP Reserve", OPS_INCREASE_MAX_OP, new int[]{8,10,10,10,11,13,15,20,25}, new int[]{1,5,10,15,20,25,35,45,55}, 9 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "+1 OP Regen", OPS_REGEN_RATE, new int[]{8,10,15,20}, new int[]{9,20,30,40}, 4 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Communications Systems", OPS_COMMUNICATIONS, new int[]{7,7,20}, new int[]{1,3,18}, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Efficient Rearmament", OPS_FAST_TEAM_REARM, new int[]{12,7,10}, new int[]{2,9,17}, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Emergency Base Cover", OPS_COVER, 20, 7, 1 );   // Consider another level offering longer cover
        ship.addUpgrade( upg );                                                 // via diff
        upg = new ShipUpgrade( "Security Door Systems", OPS_DOOR_CONTROL, new int[]{13,15,21}, new int[]{10,19,31}, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Rapid Wormhole Induction", OPS_WARP, new int[]{7,15,25}, new int[]{4,21,31}, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Orb of Seclusion", OPS_SECLUSION, new int[]{13,18}, new int[]{12,20}, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "False Minefield", OPS_MINEFIELD, 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Shroud of Darkness", OPS_SHROUD, new int[]{11,20,12,22}, new int[]{15,22,30,46}, 4 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Full Sensor Disable", OPS_FLASH, new int[]{10,15,18}, new int[]{25,33,50}, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Defensive Shields", OPS_TEAM_SHIELDS, new int[]{24,50}, new int[]{40,60}, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "EMP Pulse", ABILITY_TARGETED_EMP, 40, 55, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Empty slot)", 0, 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );
    }

    /**
     * DC as safely as possible -- if we're dying and haven't saved in the last 10 seconds, do it again.
     */
    public void handleDisconnect() {
        if( m_lastSave + 10000 > System.currentTimeMillis() )
            cmdSaveData("DistensionBot","");
    }

    public boolean isIdle() {
        if( flagTimer != null && flagTimer.isRunning() )
            return false;
        return true;
    }
}