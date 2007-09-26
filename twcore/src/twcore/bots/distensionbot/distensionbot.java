package twcore.bots.distensionbot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
 * - if player earns ship 6, it removes the ship from player who they earned it off  
 * 
 * Lower priority (in order):
 * - !intro, !intro2, !intro3, etc.
 * - 60-second timer that does full charge for spiders, burst/warp for terrs, restores !emp every 20th run, etc.
 *   Consider what can be negative-prized in order to combat rising bty problem.
 * - !emp for terr "targetted EMP" ability, and appropriate player data.
 *   This involves negative full charge, and later prizing FC back if this decrements bty.
 * - F1 Help
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

    private final int AUTOSAVE_DELAY = 15;                 // How frequently autosave occurs, in minutes 
    private final int UPGRADE_DELAY = 500;                 // Delay between fully prizing players, in ms  

    private final int NUM_UPGRADES = 14;                   // Number of upgrade slots allotted per ship
    private final double EARLY_RANK_FACTOR = 1.6;          // Factor for rank increases (lvl 1-10)
    private final double NORMAL_RANK_FACTOR = 1.15;        // Factor for rank increases (lvl 11+)
    private final int RANK_REQ_SHIP2 = 3;    // 15
    private final int RANK_REQ_SHIP3 = 1;    //  4
    private final int RANK_REQ_SHIP4 = 4;    // 20
    private final int RANK_REQ_SHIP5 = 2;    //  7
    private final int RANK_REQ_SHIP7 = 778;  // 10
    private final int RANK_REQ_SHIP8 = 779;  //  2

    private final int SPAWN_BASE_0_Y_COORD = 466;               // Y coord around which base 0 owners (top) spawn
    private final int SPAWN_BASE_1_Y_COORD = 556;               // Y coord around which base 1 owners (bottom) spawn
    private final int SPAWN_Y_SPREAD = 75;                      // # tiles * 2 from above coords to spawn players
    private final int SPAWN_X_SPREAD = 150;                     // # tiles * 2 from x coord 512 to spawn players  
    private final int SAFE_TOP_Y = 249;                         // Y coords of safes, for warping in
    private final int SAFE_BOTTOM_Y = 773;

    private final String DB_PROB_MSG = "That last one didn't go through.  Database problem, it looks like.  Please send a ?help message ASAP.";
    // Msg displayed for DB error
    private String m_database;                              // DB to connect to

    private BotSettings m_botSettings;
    private CommandInterpreter m_commandInterpreter;
    private PrizeQueue m_prizeQueue;

    private Vector <ShipProfile>m_shipGeneralData;          // Generic (nonspecific) purchasing data for ships.  Uses 1-8 for #
    private HashMap <String,DistensionPlayer>m_players;     // In-game data on players (Name -> Player)
    private HashMap <Integer,DistensionArmy>m_armies;       // In-game data on armies  (ID -> Army)

    private TimerTask entranceWaitTask;
    private TimerTask autoSaveTask;
    private boolean readyForPlay = false;
    private int[] flagOwner = {-1, -1};

    // DATA FOR FLAG TIMER
    private static final int MAX_FLAGTIME_ROUNDS = 7;   // Max # rounds (odd numbers only)
    private static final int FLAG_CLAIM_SECS = 3;       // Seconds it takes to fully claim a flag
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

    private Objset objs;                                // For keeping track of counter





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
        objs = m_botAction.getObjectSet();
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
    }


    /**
     * Registers all commands necessary.
     *
     */
    private void registerCommands() {
        int acceptedMessages = Message.PRIVATE_MESSAGE;        
        int additionalAcceptedMessages = Message.PRIVATE_MESSAGE | Message.PUBLIC_MESSAGE;

        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "cmdHelp" );
        m_commandInterpreter.registerCommand( "!enlist", acceptedMessages, this, "cmdEnlist" );
        m_commandInterpreter.registerCommand( "!defect", acceptedMessages, this, "cmdDefect" );
        m_commandInterpreter.registerCommand( "!return", additionalAcceptedMessages, this, "cmdReturn" );
        m_commandInterpreter.registerCommand( "!pilot", additionalAcceptedMessages, this, "cmdPilot" );
        m_commandInterpreter.registerCommand( "!ship", additionalAcceptedMessages, this, "cmdPilot" );    // For the confused, such as me
        m_commandInterpreter.registerCommand( "!dock", additionalAcceptedMessages, this, "cmdDock" );
        m_commandInterpreter.registerCommand( "!armies", additionalAcceptedMessages, this, "cmdArmies" );
        m_commandInterpreter.registerCommand( "!hangar", additionalAcceptedMessages, this, "cmdHangar" );
        m_commandInterpreter.registerCommand( "!status", additionalAcceptedMessages, this, "cmdStatus" );
        m_commandInterpreter.registerCommand( "!progress", additionalAcceptedMessages, this, "cmdProgress" );
        m_commandInterpreter.registerCommand( ".", additionalAcceptedMessages, this, "cmdProgress" );
        m_commandInterpreter.registerCommand( "!armory", additionalAcceptedMessages, this, "cmdArmory" );
        m_commandInterpreter.registerCommand( "!upgrade", acceptedMessages, this, "cmdUpgrade" );
        m_commandInterpreter.registerCommand( "!scrap", acceptedMessages, this, "cmdScrap" );
        m_commandInterpreter.registerCommand( "!intro", additionalAcceptedMessages, this, "cmdIntro" );
        m_commandInterpreter.registerCommand( "!info", acceptedMessages, this, "cmdInfo", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!grant", acceptedMessages, this, "cmdGrant", OperatorList.HIGHMOD_LEVEL );
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
    }


    /**
     * Display appropriate message for invalid commands.
     * @param name
     * @param msg
     */
    public void handleInvalidMessage( String name, String msg ) {
        try {
            Integer i = Integer.parseInt(msg);
            if( i >= 1 && i <= 8 )      // For lazy pilots
                cmdPilot( name, msg );
        } catch (NumberFormatException e) {
            m_botAction.sendPrivateMessage( name, "I don't get what you mean... maybe I can !help you with something?"  );
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

        if( p.getShipNum() == -1 ) {
            String[] helps = {
                    "!return               - Return to your current position in the war",
                    "!enlist <army#>       - Enlist in <army#>",
                    "!armies               - View all public armies and their IDs",
                    "!intro                - Gives an introduction to Distension"                    
            };
            m_botAction.privateMessageSpam(name, helps);
        } else if( p.getShipNum() == 0 ) {
            String[] helps = {
                    "!pilot <ship>         - Pilot <ship> if available in hangar",
                    "<shipnum>             - Shortcut for !pilot <shipnum>",
                    "!hangar               - View your ships & those available for purchase",
                    "!progress (or .)      - See your progress toward next advancement",
                    "!status               - View current ship's level and upgrades",
                    "!upgrade <upg>        - Upgrade your ship with <upg> from the armory",
                    "!armory               - View ship upgrades available in the armory",
                    "!defect <army>        - Defect to <army>.  Restarts all ships at current rank",
                    "!scrap <upg>          - Trade in <upg>.  Restarts that ship at current rank"
            };
            m_botAction.privateMessageSpam(name, helps);
        } else {
            String[] helps = {
                    "!progress (or .)      - See your progress toward next advancement",
                    "!status               - View current ship's level and upgrades",
                    "!upgrade <upg>        - Upgrade your ship with <upg> from the armory",
                    "!armory               - View ship upgrades available in the armory",
                    "!hangar               - View your ships & those available for purchase",
                    "!dock                 - Dock your ship, recording status to headquarters",
                    "!pilot <ship>         - Change to <ship> if available in hangar",
                    "<shipnum>             - Shortcut for !pilot <shipnum>",
                    "!defect <army>        - Defect to <army>.  Restarts all ships at current rank",
                    "!scrap <upg>          - Trade in <upg>.  Restarts that ship at current rank"
            };
            m_botAction.privateMessageSpam(name, helps);
        }

        if( m_botAction.getOperatorList().isHighmod(name) ) {
            String[] helps = {
                    "!info <name>          - Gets info on <name> from their !status screen",
                    "!ban <name>           - Bans a player from playing Distension",
                    "!unban <name>         - Unbans banned player",
                    "!savedata             - Saves all player data to database",
                    "!die                  - Kills DistensionBot -- use !savedata first"
            };
            m_botAction.privateMessageSpam(name, helps);
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
        m_botAction.sendPrivateMessage( name, "Welcome to Distension BETA - the TW RPG.  You are welcome to play, but player data will not carry over to the public release, & bugs WILL be present.  This is a work in progress." );
        m_botAction.sendPrivateMessage( name, "NOTE: At public release, bug reporters (?message dugwyler) will receive bonus points, as will the 3 players w/ highest combined rank in all ships." );
        m_botAction.sendPrivateMessage( name, "--- HOW TO START ---   1: Type !armies to see available armies.  2: !enlist id# to enlist in a specific army.  3: !pilot 1 to pilot your first ship, the WB.  Use !return to come back later on.");
    }


    /**
     * Save player data if unexpectedly leaving Distension.
     * @param event Event to handle.
     */
    public void handleEvent(PlayerLeft event) {
        String name = m_botAction.getPlayerName(event.getPlayerID());
        if( name == null )
            return;
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;
        DistensionArmy army = m_armies.get( player.getArmyID() );
        if( army != null ) {
            checkFlagTimeStop();
            army.adjustPilotsInGame( -1 );
            army.adjustStrength( -player.getUpgradeLevel() );
        }
        player.saveCurrentShipToDBNow();
        playerTimes.remove( name );
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

        // If flag is already claimed, take it away from old freq
        if( flagOwner[flagID] != -1 ) { 
            DistensionArmy army = m_armies.get( new Integer( flagOwner[flagID] ) );
            if( army != null ) {
                if( army.getNumFlagsOwned() == 2 && flagTimer != null && flagTimer.isRunning())
                    holdBreaking = true;
                army.adjustFlags( -1 );

                if( DEBUG )
                    m_botAction.sendPrivateMessage( p.getPlayerName(), "Flag #" + flagID + " taken from army #" + flagOwner[flagID] );
            }            
        }
        DistensionArmy army = m_armies.get( new Integer( p.getFrequency() ) );
        if( army != null ) {
            army.adjustFlags( 1 );
            if( army.getNumFlagsOwned() == 2 && flagTimer != null && flagTimer.isRunning())
                holdSecured = true;
            if( DEBUG )
                m_botAction.sendPrivateMessage( p.getPlayerName(), "Flag #" + flagID + " added to your army; " + army.getNumFlagsOwned() + " flags now owned");
        }
        if( holdBreaking )
            flagTimer.holdBreaking( army.getID(), p.getPlayerName() );
        if( holdSecured )
            flagTimer.sectorClaimed( army.getID(), p.getPlayerName() );
        flagOwner[flagID] = p.getFrequency();                
    }


    /**
     * Dock player if they spec.
     * @param event Event to handle.
     */
    public void handleEvent(FrequencyShipChange event) {
        if( !readyForPlay )         // If bot has not fully started up,
            return;                 // don't operate normally when speccing players.
        if( event.getShipType() == 0 ) {
            DistensionPlayer p = m_players.get( m_botAction.getPlayerName( event.getPlayerID() ) );
            doDock( p );
        }
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
                int div;
                // Sharks get off a little easier for TKs.  But not that easy.
                if( killer.getShipType() == Tools.Ship.SHARK )
                    div = 5;
                else
                    div = 2;
                int loss = -(victor.getUpgradeLevel() / div);
                victor.addRankPoints( loss );
                victor.clearSuccessiveKills();
                if( !victor.wasWarnedForTK() ) {
                    m_botAction.sendPrivateMessage( killer.getPlayerName(), "Lost " + loss + " rank points for TKing " + killed.getPlayerName() + ".  (You will not be notified for subsequent TKs)" );
                    victor.setWarnedForTK();
                }
                // Otherwise: Add points via level scheme
            } else {
                DistensionArmy killerarmy = m_armies.get( new Integer(killer.getFrequency()) );
                DistensionArmy killedarmy = m_armies.get( new Integer(killed.getFrequency()) );
                if( killerarmy == null || killedarmy == null )
                    return;

                if( killerarmy.getNumFlagsOwned() == 0 ) {
                    if( DEBUG )
                        m_botAction.sendPrivateMessage( killer.getPlayerName(), "DEBUG: 1 RP for kill.  (0 flags owned)" );
                    victor.addRankPoints( 1 );
                    // Loser does not lose any points for dying if killer's army controls no flags
                    return;
                }

                int points;

                // Loser is 10 or more levels above victor:
                //   Victor earns his level + 10, and loser loses half of that amount from due shame
                if( loser.getUpgradeLevel() - victor.getUpgradeLevel() >= 10 ) {
                    points = victor.getUpgradeLevel() + 10;
                    loser.addRankPoints( -(points / 2) ); 

                    // Loser is 15 or more levels below victor:
                    //   Victor only gets 1 point, and loser loses nothing
                } else if( loser.getUpgradeLevel() - victor.getUpgradeLevel() <= -15 ) {
                    points = 1;

                    // Loser is between 14 levels below and 9 levels above victor:
                    //   Victor earns the level of the loser in points, and loser loses
                    //   a point if over level 5.  Level 0 players are worth 1 point. 
                } else {
                    if( loser.getUpgradeLevel() == 0 )
                        points = 1;
                    else {
                        points = loser.getUpgradeLevel();
                        if( loser.getUpgradeLevel() > 5 )
                            loser.addRankPoints( -1 );
                    }
                }

                // Points adjusted based on size of victor's army v. loser's
                float armySizeWeight;
                try {
                    armySizeWeight = ((float)killedarmy.getTotalStrength() / (float)killerarmy.getTotalStrength());
                } catch (Exception e ) {
                    armySizeWeight = 1;
                }

                points = (int)(points * armySizeWeight);
                if( points < 1 )
                    points = 1;
                points *= killerarmy.getNumFlagsOwned();

                victor.addRankPoints( points );
                // Track successive kills for weasel unlock & streaks
                boolean earnedWeasel = victor.addSuccessiveKill();
                loser.clearSuccessiveKills();
                if( DEBUG ) {
                    m_botAction.sendPrivateMessage( killer.getPlayerName(), "DEBUG: " + points + " RP earned; weight=" + armySizeWeight + "; flags=" + killerarmy.getNumFlagsOwned() );
                    //m_botAction.sendPrivateMessage( killed.getPlayerName(), "DEBUG: " + loss + " RP lost; weight=" + armySizeWeight + "; flags=" + killerarmy.getNumFlagsOwned() );
                }
            }
        }
    }



    // ***** COMMAND PROCESSING

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
        m_botAction.sendArenaMessage( "Distension going down for maintenance ...", 1 );
        try { Thread.sleep(500); } catch (Exception e) {};
        m_botAction.die();
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

        // Easy fix: Disallow names >19 chars to avoid name hacking 
        if( name.length() > 19 ) {
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
            m_botAction.sendPrivateMessage( name, "Hmm... which army do you want to enlist in?  You sure that's one of the !armies here?" );
            return;
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
            bonus = calcEnlistmentBonus( armyNum );
        }

        m_botAction.sendPrivateMessage( name, "Ah, joining " + army.getName().toUpperCase() + "?  Excellent.  You are pilot #" + (army.getPilotsTotal() + 1) + "." );

        p.setArmy( armyNum );
        p.addPlayerToDB();
        p.setShipNum( 0 );
        army.adjustPilotsTotal(1);
        m_botAction.sendPrivateMessage( name, "Welcome aboard." );
        m_botAction.sendPrivateMessage( name, "If you need an !intro to how things work, I'd be glad to !help out.  Or if you just want to get some action, jump in your new Warbird.  (!pilot 1)" );
        if( bonus > 0 ) {
            m_botAction.sendPrivateMessage( name, "Your enlistment bonus also entitles you to " + bonus + " free upgrade" + (bonus==1?"":"s")+ ".  Congratulations." );
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
            if( player.isBanned() )
                m_botAction.sendPrivateMessage( name, "Sorry ... I don't think I can let you back in.  You've caused enough trouble around here.  If you think I'm wrong, you might want to ask someone for ?help ..." );
            else                
                m_botAction.sendPrivateMessage( name, player.getName().toUpperCase() + " not authorized as pilot of any army.  You must first !enlist." );
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

        if( player.shipIsAvailable( shipNum ) ) {
            DistensionArmy army = m_armies.get( new Integer(player.getArmyID()) );
            if( player.getShipNum() > 0 ) {
                player.saveCurrentShipToDBNow();
                playerTimes.remove( player.getName() );
                if( army != null ) {
                    army.adjustStrength( -player.getUpgradeLevel() );
                    army.adjustPilotsInGame( -1 );
                    if( DEBUG )
                        m_botAction.sendPrivateMessage( player.getName(), "DEBUG: Shipchange found army; adjusting strength and # pilots in game" );            
                }
            }

            player.setShipNum( shipNum );
            if( player.getCurrentShipFromDB() ) {
                if( army == null ) {
                    army = new DistensionArmy( player.getArmyID() );
                    m_armies.put( new Integer(player.getArmyID()), army );
                    if( DEBUG )
                        m_botAction.sendPrivateMessage( player.getName(), "DEBUG: Army not found; creating new record" );            
                }
                army.adjustStrength( player.getUpgradeLevel() );
                army.adjustPilotsInGame( 1 );
                player.putInCurrentShip();
                player.prizeUpgrades();
                if( flagTimer != null && flagTimer.isRunning() )
                    playerTimes.put( player.getName(), new Integer( flagTimer.getTotalSecs() ) );
                if( !flagTimeStarted ) {
                    checkFlagTimeStart();
                }                    
                cmdProgress( name, null );                
            } else {
                m_botAction.sendPrivateMessage( name, "Having trouble getting that ship for you.  Please contact a mod." );
                army.adjustPilotsInGame( -1 );
                player.setShipNum( 0 );
            }
        } else {
            m_botAction.sendPrivateMessage( name, "You don't own that ship.  Check your !hangar before you try flying something you don't have." );
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
        m_botAction.specWithoutLock( name );
    }

    /**
     * Workhorse of docking, used by the FrequencyShipChange message.
     * @param player Player to dock
     */
    public void doDock( DistensionPlayer player ) {
        if( player == null )
            return;
        DistensionArmy army = m_armies.get( new Integer(player.getArmyID()) );
        if( army != null ) {
            army.adjustPilotsInGame(-1);
            army.adjustStrength(-player.getUpgradeLevel());
            playerTimes.remove( player.getName() );
            checkFlagTimeStop();
            if( DEBUG )
                m_botAction.sendPrivateMessage( player.getName(), "DEBUG: Docking reduced army strength by " + player.getUpgradeLevel() );            
        }
        if( player.saveCurrentShipToDBNow() ) {
            m_botAction.sendPrivateMessage( player.getName(), "Ship status confirmed and logged to our records.  You are now docked.");
            player.setShipNum( 0 );
        } else {
            m_botAction.sendPrivateMessage( player.getName(), "Ship status was NOT logged.  Please notify a member of staff immediately.");
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
            m_botAction.sendPrivateMessage( name, "#   " + Tools.formatString("Army Name", 35 ) + "Playing    Total    Strength    Flags" );
        else
            m_botAction.sendPrivateMessage( name, "#   " + Tools.formatString("Army Name", 35 ) + "Playing    Total    Bonus" );
        //                                                                                     #234567890#23456789#23456789012#

        if( inGame ) {
            for( DistensionArmy a : m_armies.values() ) {
                m_botAction.sendPrivateMessage( name, Tools.formatString( ""+a.getID(), 4 ) +
                        Tools.formatString( a.getName(), 38 ) +
                        Tools.formatString( ""+a.getPilotsInGame(), 10 ) +
                        Tools.formatString( ""+a.getPilotsTotal(), 9 ) +
                        Tools.formatString( ""+a.getTotalStrength(), 12 ) +
                        a.getNumFlagsOwned() );
            }
        } else {
            for( DistensionArmy a : m_armies.values() ) {
                int bonus = 0;
                //if( a.isDefault() )
                //    bonus = calcEnlistmentBonus( a.getID(), defaultcount );  // defaultcount = hashmap of all armies (id->totalpilots) 
                //else
                //    bonus = 0;
                m_botAction.sendPrivateMessage( name, Tools.formatString( ""+a.getID(), 4 ) +
                        Tools.formatString( a.getName(), 38 ) +
                        Tools.formatString( ""+a.getPilotsInGame(), 10 ) +
                        Tools.formatString( ""+a.getPilotsTotal(), 9 ) +
                        bonus + " upgrade" + (bonus == 1?"":"s") ); 
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
                    m_botAction.sendPrivateMessage( name, "  " + i + "   " + Tools.formatString( Tools.shipName( i ), 20 ) + "IN FLIGHT: Rank " + player.getUpgradeLevel() );
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
        m_botAction.sendPrivateMessage( name, player.getUpgradePoints() + " Upgrade Points available." );
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
        else
            m_botAction.specificPrize( name, upgrade.getPrizeNum() );
        if( upgrade.getMaxLevel() == 1 )
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " has been installed on the " + Tools.shipName( shipNum ) + "." );
        else
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " on the " + Tools.shipName( shipNum ) + " upgraded to level " + (currentUpgradeLevel + 1) + "." );
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
     * Provides a brief introduction to the game for a player.  This should support
     * the A1 LVZ help, and the F1 help.
     * @param name
     * @param msg
     */
    public void cmdIntro( String name, String msg ) {
        String[] intro1 = {
                "DISTENSION - The Trench Wars RPG - G. Dugwyler",
                ""
        };
        m_botAction.privateMessageSpam( name, intro1 );
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


    /**
     * Grants a player credits (only for debug mode).
     * @param name 
     * @param msg
     */
    public void cmdGrant( String name, String msg ) {
        if( !DEBUG ) { 
            m_botAction.sendPrivateMessage( name, "This command disabled for live testing." );
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



    // COMMAND ASSISTANCE METHODS

    /**
     * Checks if flag time should be started.
     */
    public void checkFlagTimeStart() {
        if( flagTimeStarted )
            return;
        Iterator <DistensionArmy>i = m_armies.values().iterator();
        boolean foundOne = false;
        while( i.hasNext() ) {
            DistensionArmy army = i.next();
            if( army.getPilotsInGame() > 0 ) {
                if( !foundOne )
                    foundOne = true;
                else {
                    // Two armies now have players; start game
                    m_botAction.sendArenaMessage( "A war is brewing ... " );
                    m_botAction.sendArenaMessage( "To win the battle, hold both flags for " + flagMinutesRequired + " minute" + (flagMinutesRequired == 1 ? "" : "s") + ".  Winning " + ( MAX_FLAGTIME_ROUNDS + 1) / 2 + " battles will win the sector conflict." );
                    m_botAction.scheduleTask( new StartRoundTask(), 60000 );
                    stopFlagTime = false;
                    flagTimeStarted = true;
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
    }


    /**
     * @return Enlistment bonus for a given default army, based on the size of other default armies.
     */
    public int calcEnlistmentBonus( int armyID ) {
        int pilots = 0;
        int numOtherArmies = 0;
        DistensionArmy army = m_armies.get( armyID );
        if( army == null || !army.isDefault() )
            return 0;
        int ourcount = army.getPilotsTotal();

        for( DistensionArmy a : m_armies.values() ) {
            if( a.getID() != armyID && a.isDefault() )
                pilots += a.getPilotsTotal();
            numOtherArmies++;
        }
        
        if( pilots == 0 )
            return 0;
        pilots /= numOtherArmies;     // Figure average size of other armies
        pilots = pilots - ourcount;   // Figure how much under, if any, ours is

        // If avg # pilots is close enough, no bonus
        if( pilots < 3 )
            return 0;
        // Starting at a difference of 3, we give a 1 point bonus 
        pilots -= 2;
        // Points to give greater than 5?  Stick to 5
        if( pilots > 5 )
            pilots = 5;
        return pilots;
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
        private boolean   warnedForTK;          // True if they TKd / notified of penalty this match
        private boolean   banned;               // True if banned from playing
        private boolean   shipDataSaved;        // True if ship data on record equals ship data in DB
        private boolean   fastRespawn;          // True if player respawns at the head of the queue
        private int[]     purchasedUpgrades;    // Upgrades purchased for current ship
        private boolean[] shipsAvail;           // Marks which ships are available
        private boolean   isRespawning;         // True if player is currently in respawn process

        public DistensionPlayer( String name ) {
            this.name = name;
            shipNum = -1;
            rank = -1;
            rankPoints = -1;
            nextRank = -1;
            upgPoints = -1;
            armyID = -1;
            warnedForTK = false;
            banned = false;
            shipDataSaved = false;
            fastRespawn = false;
            purchasedUpgrades = new int[NUM_UPGRADES];
            shipsAvail = new boolean[8];
            for( int i = 0; i < 8; i++ )
                shipsAvail[i] = false;
            successiveKills = 0;
            isRespawning = false;
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
        public void addShipToDB( int shipNumToAdd, int startingUpgradePoints ) {
            if( shipNumToAdd < 1 || shipNumToAdd > 8 )
                return;
            shipsAvail[ shipNumToAdd - 1 ] = true;
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcShip" + shipNumToAdd + "='y' WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
                if( startingUpgradePoints == 0 )
                    m_botAction.SQLQueryAndClose( m_database, "INSERT INTO tblDistensionShip ( fnPlayerID , fnShipNum ) VALUES ((SELECT fnID FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name ) + "'), " + shipNumToAdd + ")" );
                else
                    m_botAction.SQLQueryAndClose( m_database, "INSERT INTO tblDistensionShip ( fnPlayerID , fnShipNum , fnUpgradePoints ) VALUES ((SELECT fnID FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name ) + "'), " + shipNumToAdd + ", " + startingUpgradePoints + ")" );
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
                    if( upgrades.get( i ).getPrizeNum() == -1 && purchasedUpgrades[i] > 0 );
                fastRespawn = true;

                m_botAction.SQLClose(r);
                return shipDataSaved;
            } catch (SQLException e ) {
                return false;
            }
        }


        // COMPLEX ACTIONS

        /**
         * Prizes upgrades to player based on what has been purchased.
         */
        public void prizeUpgrades() {
            Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum ).getAllUpgrades();
            int prize = -1;
            for( int i = 0; i < NUM_UPGRADES; i++ ) {
                prize = upgrades.get( i ).getPrizeNum();
                if( prize > 0 )
                    for( int j = 0; j < purchasedUpgrades[i]; j++ )
                        m_botAction.specificPrize( name, prize );
            }
            doWarp();
        }        

        /**
         * Warps player to the appropriate spawning location (near a specific base).
         * Used after prizing. 
         */
        public void doWarp() {
            int base = armyID % 2;
            Random r = new Random();
            int x = 512 + (r.nextInt(SPAWN_X_SPREAD) - (SPAWN_X_SPREAD / 2));
            int y;
            if( base == 0 )            
                y = SPAWN_BASE_0_Y_COORD + (r.nextInt(SPAWN_Y_SPREAD) - (SPAWN_Y_SPREAD / 2));
            else
                y = SPAWN_BASE_1_Y_COORD + (r.nextInt(SPAWN_Y_SPREAD) - (SPAWN_Y_SPREAD / 2));
            m_botAction.warpTo(name, x, y);
            isRespawning = false;
        }

        /**
         * Sets up player for respawning.
         */
        public void doSetupRespawn() {
            isRespawning = true;
            doSafeWarp();
            m_botAction.shipReset(name);
            if( hasFastRespawn() ) {
                m_prizeQueue.addHighPriorityPlayer( this );                
            } else {
                m_prizeQueue.addPlayer( this );
            }            
        }

        /**
         * Warps player to the safe, no strings attached.
         */
        public void doSafeWarp() {
            int base = armyID % 2;
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
            m_botAction.sendPrivateMessage(name, "Congratulations, you have advanced to rank " + rank + " in the " + Tools.shipName(shipNum) + ".  You will reach the next rank in " + ( nextRank - rankPoints )+ " rank points (total " + nextRank + ").");
            m_botAction.sendPrivateMessage(name, "You have earned 1 upgrade point to spend in the !armory." + ((upgPoints > 1) ? ("  (" + upgPoints + " total available).") : "") );

            switch (rank) {
                case RANK_REQ_SHIP2:
                    if( shipsAvail[1] == false ) {
                        m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Javelin.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                        m_botAction.sendPrivateMessage(name, "JAVELIN: The Javelin is a difficult ship to pilot, but one of the most potentially dangerous.  Users of the original Javelin model will feel right at home.  Our Javelins are extremely upgradeable, devastating other craft in high ranks.");
                        addShipToDB(2);
                    }
                    break;
                case RANK_REQ_SHIP3:
                    if( shipsAvail[2] == false ) {
                        m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Spider.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                        m_botAction.sendPrivateMessage(name, "SPIDER: The Spider is the mainstay support gunner of every army and the most critical element of base defense.  Upgraded spiders receive regular refuelings, wormhole plugging capabilities, and 10-second post-rearmament energy streams.");
                        addShipToDB(3);
                    }
                    break;
                case RANK_REQ_SHIP4:
                    if( shipsAvail[3] == false ) {
                        m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the experimental Leviathan.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                        m_botAction.sendPrivateMessage(name, "LEVIATHAN: The Leviathan is an experimental craft, as yet untested.  It is unmaneuvarable but capable of great speeds -- in reverse.  Its guns are formidable, its bombs can cripple an entire base, but it is a difficult ship to master.");
                        addShipToDB(4);
                    }
                    break;
                case RANK_REQ_SHIP5:
                    if( shipsAvail[4] == false ) {
                        m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Terrier.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                        m_botAction.sendPrivateMessage(name, "TERRIER: The Terrier is our most important ship, providing a point of support into the fray.  Also the most rapidly-advancing craft, advanced Terriers enjoy rearmament preference and resupply of weapons and wormhole kits.");
                        addShipToDB(5);
                    }
                    break;
                case RANK_REQ_SHIP7:
                    if( shipsAvail[6] == false ) {
                        m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Lancaster.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                        m_botAction.sendPrivateMessage(name, "LANCASTER: The Lancaster is an unusual ship with a host of surprises onboard.  Pilots can upgrade its most basic components rapidly.  The Firebloom and the Lanc's evasive-bombing capability make this a fantastic choice for advanced pilots.");
                        addShipToDB(7);
                    }
                    break;
                case RANK_REQ_SHIP8:
                    if( shipsAvail[7] == false ) {
                        m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Shark.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                        m_botAction.sendPrivateMessage(name, "SHARK: The Shark is piloted by our most clever and resourceful pilots.  Unsung heroes of the army, Sharks are both our main line of defense and leaders of every assault.  Advanced sharks enjoy light gun capabilities and a cloaking device.");
                        addShipToDB(8);
                    }
                    break;
            }
        }


        // SETTERS

        /**
         * Adds # rank points to total rank point amt.
         * @param points Amt to add
         */
        public void addRankPoints( int points ) {
            if( shipNum < 1 )
                return;
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
            m_armies.get(armyID).adjustStrength(amt);
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
        }

        public void putInCurrentShip() {
            m_botAction.setShip( name, shipNum );
            m_botAction.setFreq( name, getArmyID() );            
        }

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
         * Increments successive kills.
         */
        public boolean addSuccessiveKill() {
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
                if( false ) { // shipsAvail[5] == false ) {                                        
                    String query = "SELECT * FROM tblDistensionShip ship, tblDistensionPlayer player " +
                    "WHERE player.fcName = '" + Tools.addSlashesToString( name ) + "' AND " +
                    "ship.fnPlayerID = player.fnID AND " +
                    "ship.fnShipNum = '6'";
                    boolean weaselPreviouslyStolen = false;
                    
                    try {
                        ResultSet r = m_botAction.SQLQuery( m_database, query );
                        if( r.next() ) {
                            m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcShip6='y' WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
                        } else {
                            m_botAction.sendPrivateMessage(name, "DESC");
                            m_botAction.sendPrivateMessage(name, "WEASEL: DESC.");
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
                    award = rank * 20;
                m_botAction.sendPrivateMessage(name, "99 KILLS --  ORGASMIC !!  (" + award + " RP bonus.)", Tools.Sound.ORGASM_DO_NOT_USE );                
            }
            return false;
        }

        /**
         * Sets successive kills to 0.
         */
        public void clearSuccessiveKills() {
            successiveKills = 0;
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
            m_botAction.sendSmartPrivateMessage(name, "You have been indefinitely banned from playing Distension." );
            setShipNum( -1 );
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
            return armyID;
        }

        /**
         * @return Returns army name.
         */
        public String getArmyName() {
            DistensionArmy army = m_armies.get( new Integer( armyID ) );
            if( army == null ) {
                army = new DistensionArmy( new Integer( armyID ) );
                m_armies.put( new Integer( armyID ), army );
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
                ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fcArmyName, fcDefaultArmy, fcPrivateArmy, fnNumPilots FROM tblDistensionArmy WHERE fnArmyID = '"+ armyID + "'" );
                if( r.next() ) {
                    armyName = r.getString( "fcArmyName" );
                    pilotsTotal = r.getInt( "fnNumPilots" ); 
                    isDefault = r.getString( "fcDefaultArmy" ).equals("y");
                    isPrivate = r.getString( "fcPrivateArmy" ).equals("y");
                    password = r.getString( "fcPassword" );
                }
            } catch( Exception e ) {
            }
        }

        // SETTERS

        public void adjustPilotsTotal( int value ) {
            pilotsTotal += value;
            if( pilotsTotal < 0 )
                pilotsTotal = 0;
            m_botAction.SQLBackgroundQuery( m_database, null, "UPDATE tblDistensionArmy SET fnNumPilots=" + pilotsTotal + " WHERE fnArmyID=" + armyID );
        }

        public void adjustPilotsInGame( int value ) {
            pilotsInGame += value;
            if( pilotsInGame < 0 )
                pilotsInGame = 0;
        }

        public void adjustStrength( int value ) {
            totalStrength += value;
            if( totalStrength < 0 )
                totalStrength = 0;
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
                return costDefines[ currentLevel + 1 ];
            else
                return cost;
        }
    }


    /**
     * Prize queuer, for preventing bot lockups.
     */
    private class PrizeQueue extends TimerTask {
        DistensionPlayer currentPlayer;

        LinkedList <DistensionPlayer>players = new LinkedList<DistensionPlayer>();

        /**
         * Adds a player to the end of the prizing queue.
         * @param p Player to add 
         */
        public void addPlayer( DistensionPlayer p ) {
            players.add(p);
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
            currentPlayer = players.remove();
            if( currentPlayer != null )
                currentPlayer.prizeUpgrades();
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

        flagTimer = new FlagCountTask();
        m_botAction.showObject(2300); // Turns on countdown lvz
        m_botAction.hideObject(1000); // Turns off intermission lvz
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
                roundTitle = "A new conflict ";
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
                        "  Battles won: " + freq0Score + " to " + freq1Score, 1 );
            }
        } else {
            m_botAction.sendArenaMessage( "END BATTLE: " + m_armies.get(winningArmyID).getName() + " gains control of the sector after " + getTimeString( flagTimer.getTotalSecs() ), 1 );
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
        
        // Points to be divided up by army
        int totalPoints = (int)(minsToWin * opposingStrengthAvg * armyDiffWeight);
        if( DEBUG )
            m_botAction.sendArenaMessage( "DEBUG: " + minsToWin + "min battle * " + opposingStrengthAvg + " avg total enemy strength * " + armyDiffWeight + " strength diff weight = " + totalPoints + "RP to be divided among winners" );

        // Point formula: (min played * avg opposing strength * avg opposing strength / avg team strength) * your upgrade level / avg team strength        
        Iterator <DistensionPlayer>i = m_players.values().iterator();
        int upgLevel = 0;
        int points = 0;
        while( i.hasNext() ) {
            DistensionPlayer p = i.next();
            if( p.getArmyID() == winningArmyID ) {
                upgLevel = p.getUpgradeLevel();
                if( upgLevel == 0 ) 
                    upgLevel = 1;
                points = (int)(totalPoints * ((float)upgLevel / (float)friendlyStrengthAvg));
                Integer time = playerTimes.get( p.getName() );
                if( time != null ) {
                    int secs = flagTimer.getTotalSecs();
                    int percentOnFreq = (int)( ( (float)(secs - time) / (float)secs ) * 100 );
                    int modPoints = (int)(points * ((float)percentOnFreq / 100));

                    if( DEBUG )
                        m_botAction.sendPrivateMessage(p.getName(), "DEBUG: " + modPoints + " RP for victory = lvl:" + (upgLevel) + " / avg team str:" + friendlyStrengthAvg + "(or " + upgLevel / friendlyStrengthAvg + ") * total points:" + totalPoints + " * " + percentOnFreq + "% participation");
                    else
                        m_botAction.sendPrivateMessage(p.getName(), "You receive " + modPoints + " RP for your role in the victory." );
                    int holds = flagTimer.getSectorHolds( p.getName() );
                    int breaks = flagTimer.getSectorBreaks( p.getName() );                    
                    int bonus = 0;
                    if( holds != 0 && breaks != 0 ) {
                        bonus = (int)( modPoints * (((float)holds / 10.0) + ((float)breaks / 20.0)) );
                        m_botAction.sendPrivateMessage( p.getName(), "For " + holds + " sector holds and " + breaks +" sector breaks, you also receive an additional " + bonus + " RP." );
                    } else if( holds != 0 ) {
                        bonus = (int)( modPoints * ((float)holds / 10.0) );                        
                        m_botAction.sendPrivateMessage( p.getName(), "For " + holds + " sector holds, you also receive an additional " + bonus + " RP." );
                    } else if( breaks != 0 ) {
                        bonus = (int)( modPoints * ((float)breaks / 20.0) );                        
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
            m_botAction.sendArenaMessage( "THE CONFLICT IS OVER!  " + m_armies.get(winningArmyID).getName() + " has laid total claim to the sector, after " + (freq0Score + freq1Score) + " total battles.", 2 );
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
                p.doWarp();
            }
        }
    }


    /**
     * Warp all players to a safe 10 seconds before starting round.
     * Clears mines and builds tension.
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
                    flagTimer.endBattle();
                    m_botAction.cancelTask(flagTimer);
                    flagTimeStarted = false;
                } catch (Exception e ) {
                }
                m_botAction.hideObject(1000); // Turns off intermission lvz
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
            m_botAction.showObject(1000); //Shows intermission lvz
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
                    objs.showObject(objNums[i]);
                else
                    objs.hideObject(objNums[i]);
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
            m_botAction.showObject(2400); // Shows flag claimed lvz
            claimBeingBroken = false;
            breakingArmyID = -1;
            secondsHeld = 0;            
            DistensionPlayer p = m_players.get(pName);
            if( p == null )
                return;
            addSectorHold( p.getName() );
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
         * Ends the battle for the timer's internal purposes.
         */
        public void endBattle() {
            objs.hideAllObjects();
            m_botAction.setObjects();
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
         * @return Total number of seconds round has been running.
         */
        public int getTotalSecs() {
            return totalSecs;
        }

        /**
         * @return Frequency that currently holds the flag
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
                    m_botAction.sendArenaMessage( "The next battle is just beginning . . ." );
                    safeWarp();
                }
                preTimeCount++;

                if( preTimeCount >= 10 ) {
                    isStarted = true;
                    isRunning = true;
                    m_botAction.sendArenaMessage( ( roundNum == MAX_FLAGTIME_ROUNDS ? "THE FINAL BATTLE" : "BATTLE " + roundNum) + " HAS BEGUN!  Capture both flags for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win the battle.", 1 );
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
                if( breakSeconds >= FLAG_CLAIM_SECS ) {
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
            objs.hideAllObjects();
            int minutes = secsNeeded / 60;
            int seconds = secsNeeded % 60;
            if( minutes < 1 ) objs.showObject( 1100 );
            if( minutes > 10 )
                objs.showObject( 10 + ((minutes - minutes % 10)/10) );
            objs.showObject( 20 + (minutes % 10) );
            objs.showObject( 30 + ((seconds - seconds % 10)/10) );
            objs.showObject( 40 + (seconds % 10) );
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
     * -3:  Full charge every 60 seconds, for spiders
     * -4:  Targetted EMP against all enemies (uses !emp command)
     * 
     * 
     * Order of speed of rank upgrades (high speed to low speed, lower # being faster ranks):
     * Terr   - 10  (unlock @ 7)
     * Shark  - 11  (unlock @ 2)
     * Lanc   - 12  (unlock @ 10)
     * Spider - 14  (unlock @ 4)
     * WB     - 15  (start)
     * Weasel - 16  (unlock by successive kills)
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
        ShipUpgrade upg;
        m_shipGeneralData.add( ship );

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
        upg = new ShipUpgrade( "Matter-to-Antimatter Emulsifier", Tools.Prize.THOR, 1, 42, 1 );
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
        upg = new ShipUpgrade( "Rear Defense System", Tools.Prize.GUNS, p2b1, p2b2, 4 );
        ship.addUpgrade( upg );
        int p2c1[] = {  3,  8 };
        int p2c2[] = { 15, 80 };
        upg = new ShipUpgrade( "Mortar Explosive Enhancement", Tools.Prize.BOMBS, p2c1, p2c2, 3 );
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
        // 0:  Super (but costs 5)
        // 15: Decoy
        // 26: Multi (rear)
        // 30: Decoy
        // 31: Refueler
        // 38: Anti
        // 40: L2 Guns
        // 42: XRadar
        // 45: Decoy
        ship = new ShipProfile( RANK_REQ_SHIP3, 14 );
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
        int p3d1[] = { 15, 30, 45 };
        upg = new ShipUpgrade( "Spider Reiterator", Tools.Prize.DECOY, 1, p3d1, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "60-second Refeuler", -3, 1, 31, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Infinite Energy Stream", Tools.Prize.SUPER, 5, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Field Stabilizer", Tools.Prize.ANTIWARP, 1, 38, 1 );
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
        upg = new ShipUpgrade( "Force Thrusters", Tools.Prize.THRUST, 2, 0, 5 );                // -4 x5
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Collection Drive", Tools.Prize.TOPSPEED, 1, 0, 7 );             // 1000 x7
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Power Recirculator", Tools.Prize.RECHARGE, 1, 0, 10 );          // 140 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Carbon-Forced Armor", Tools.Prize.ENERGY, 1, 0, 20 );           // 100 x20
        ship.addUpgrade( upg );
        int p4a1[] = { 1,  4 };
        int p4a2[] = { 8, 35 };
        upg = new ShipUpgrade( "Spill Guns", Tools.Prize.GUNS, p4a1, p4a2, 2 );         // DEFINE
        ship.addUpgrade( upg );
        int p4b1[] = { 1,  1,  3 };
        int p4b2[] = { 4, 15, 48 };
        upg = new ShipUpgrade( "Chronos(TM) Disruptor", Tools.Prize.BOMBS, p4b1, p4b2, 3 );        // DEFINE
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
        // 7:  Portal 1
        // 10: Priority respawn
        // 13: XRadar
        // 16: Multi (regular)
        // 21: Burst 2 
        // 25: Decoy
        // 29: Portal 2
        // 30: (Can attach to other terrs)
        // 36: Guns
        // 40: Regeneration 
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
        upg = new ShipUpgrade( "Full Reconstructor", Tools.Prize.RECHARGE, 1, 0, 5 );           // 180 x5
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
        upg = new ShipUpgrade( "Terrier Reiterator", Tools.Prize.DECOY, 1, 25, 1 );
        ship.addUpgrade( upg );
        int p5a1[] = { 1,  1,  2,  3,  5 };
        int p5a2[] = { 7, 29, 48, 60, 70 };
        upg = new ShipUpgrade( "Wormhole Creation Kit", Tools.Prize.PORTAL, p5a1, p5a2, 5 );        // DEFINE
        ship.addUpgrade( upg );
        int p5b1[] = { 1,  1,  2,  3,  5,  8 };
        int p5b2[] = { 2, 21, 44, 55, 65, 80 };
        upg = new ShipUpgrade( "Rebounding Burst", Tools.Prize.BURST, p5b1, p5b2, 6 );       // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "60-second Regeneration", -2, 3, 40, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Priority Respawning", -1, 1, 10, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Targetted EMP", -4, 5, 50, 1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        ship = new ShipProfile( -1, 0 );
        m_shipGeneralData.add( ship );
        ship = new ShipProfile( -1, 1 );
        m_shipGeneralData.add( ship );
        ship = new ShipProfile( -1, 2 );
        m_shipGeneralData.add( ship );

        /*
        // WEASEL -- Unlocked by 20(tweak?) successive kills w/o dying
        // Slow-medium upg speed; all upg start 2 levels higher than normal calcs, but 1st level (3rd) costs 2pts
        //                        and all have level requirements
        ship = new ShipProfile( -1, 16 );
        int p6a1[] = { 2, 1, 1, 1,  1,  1,  1,  1,  1 };
        int p6a2[] = { 5, 8, 10, 15, 20, 30, 40, 50, 50 };
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
        upg = new ShipUpgrade( "Low-Propulsion Cannons", Tools.Prize.GUNS, 0, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombing ability disabled)", Tools.Prize.BOMBS, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Cannon Distributor", Tools.Prize.MULTIFIRE, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Weasel Reiterator", Tools.Prize.DECOY, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Scrambler", Tools.Prize.STEALTH, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Light-Bending Unit", Tools.Prize.CLOAK, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Assault Boosters", Tools.Prize.ROCKET, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Movement Inhibitor", Tools.Prize.BRICK, 0, 1 );
        ship.addUpgrade( upg );
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

        // SHARK -- rank 2
        ship = new ShipProfile( RANK_REQ_SHIP8, 11 );
        upg = new ShipUpgrade( "Runningside Correctors", Tools.Prize.ROTATION, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Spitfire Thrusters", Tools.Prize.THRUST, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Space-Force Emulsifier", Tools.Prize.TOPSPEED, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Light Charging Mechanism", Tools.Prize.RECHARGE, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Projectile Slip Plates", Tools.Prize.ENERGY, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Emergency Defense Cannon", Tools.Prize.GUNS, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Plasma Drop Upgrade", Tools.Prize.BOMBS, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Spreadshot", Tools.Prize.MULTIFIRE, 0, 0 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Unit", Tools.Prize.XRADAR, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Shark Reiterator", Tools.Prize.DECOY, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Gravitational Repulsor", Tools.Prize.REPEL, 0, 5 );    // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Splintering Unit", Tools.Prize.SHRAPNEL, 0, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Nonexistence Device", Tools.Prize.CLOAK, 20700, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Fast Respawn", -1, 1200, 1 );   // DEFINE
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );
         */
    }

}