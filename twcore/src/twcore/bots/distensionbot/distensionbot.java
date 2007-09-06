package twcore.bots.distensionbot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
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
import twcore.core.util.Tools;


/**
 * DistensionBot -- for the progressive war-game, 'Distension'.
 *
 *
 * *** Development notes ***
 * 
 * Add:
 * - flag time for two flags
 * - last few ships
 * - reset all flags when nobody's in game
 * - fix enlistment bonus
 * - fix weight
 * 
 * Lower priority (in order):
 * - !scrap #         - Scrap (sell) a given upgrade as defined on !status screen (lose back to start of level)
 * - !defect #(:password) - Defect to another army
 * - !intro, !intro2, !intro3, etc.
 * - 60-second timer that does full charge for spiders, burst/warp for terrs, restores !emp every 20th run, etc.
 * - !emp for terr "targetted EMP" ability, and appropriate player data
 * - !myfreq  - to their freq when in spec
 * - F1 Help
 *
 *
 * Adding up points:
 *   Flag multiplier - No flags=all rewards are 1; 1 flag=regular; 2 flags=x2
 *
 *   15 levels lower or more           = 1 pt
 *   14 levels below to 9 levels above = level of ship
 *   10 levels above or more           = killer's level + 10
 *
 * @author dugwyler
 */
public class distensionbot extends SubspaceBot {
    
    private final boolean DEBUG = true;                    // Debug mode.  Displays various info that would
                                                           // normally be annoying in a public release.

    private final int NUM_UPGRADES = 14;                   // Number of upgrade slots allotted per ship
    private final int UPGRADE_DELAY = 500;                 // Delay between fully prizing players, in ms  
    private final double EARLY_RANK_FACTOR = 1.6;          // Factor for rank increases (lvl 1-10)
    private final double NORMAL_RANK_FACTOR = 1.15;        // Factor for rank increases (lvl 11+)
    private final int RANK_REQ_SHIP2 = 15;
    private final int RANK_REQ_SHIP3 = 4;
    private final int RANK_REQ_SHIP4 = 20;
    private final int RANK_REQ_SHIP5 = 7;
    private final int RANK_REQ_SHIP7 = 10;
    private final int RANK_REQ_SHIP8 = 2;
    
    private final int SPAWN_BASE_0_Y_COORD = 466;               // Y coord around which base 0 owners (top) spawn
    private final int SPAWN_BASE_1_Y_COORD = 556;               // Y coord around which base 1 owners (bottom) spawn
    private final int SPAWN_Y_SPREAD = 75;                      // # tiles * 2 from above coords to spawn players
    private final int SPAWN_X_SPREAD = 150;                     // # tiles * 2 from x coord 512 to spawn players  
    
    private final String DB_PROB_MSG = "That last one didn't go through.  Database problem, it looks like.  Please send a ?help message ASAP.";
                                                            // Msg displayed for DB error
    private String m_database;                              // DB to connect to

    private BotSettings m_botSettings;
    private CommandInterpreter m_commandInterpreter;
    private PrizeQueue m_prizeQueue;

    private Vector <ShipProfile>m_shipGeneralData;          // Generic (nonspecific) purchasing data for ships
    private HashMap <String,DistensionPlayer>m_players;     // In-game data on players (Name -> Player)
    private HashMap <Integer,DistensionArmy>m_armies;       // In-game data on armies  (ID -> Army)
    //private HashMap <Integer,Integer>m_flags;               // In-game data on flags   (FlagID -> ArmyID that owns)

    private TimerTask entranceWaitTask;
    private boolean readyForPlay = false;
    private int[] flagOwner = {-1, -1};


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
        //m_flags = new HashMap<Integer,Integer>();
        setupPrices();
        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fnArmyID FROM tblDistensionArmy" );
            while( r.next() ) {
                Integer id = r.getInt( "fnArmyID");
                m_armies.put(id, new DistensionArmy(id));                
            }
            m_botAction.SQLClose(r);
        } catch (SQLException e) {
            Tools.printLog("Error retrieving army data on startup.");
        }
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
        m_commandInterpreter.registerCommand( "!return", acceptedMessages, this, "cmdReturn" );
        m_commandInterpreter.registerCommand( "!pilot", acceptedMessages, this, "cmdPilot" );
        m_commandInterpreter.registerCommand( "!dock", acceptedMessages, this, "cmdDock" );
        m_commandInterpreter.registerCommand( "!armies", acceptedMessages, this, "cmdArmies" );
        m_commandInterpreter.registerCommand( "!hangar", acceptedMessages, this, "cmdHangar" );
        m_commandInterpreter.registerCommand( "!status", acceptedMessages, this, "cmdStatus" );
        m_commandInterpreter.registerCommand( "!progress", acceptedMessages, this, "cmdProgress" );
        m_commandInterpreter.registerCommand( ".", additionalAcceptedMessages, this, "cmdProgress" );
        m_commandInterpreter.registerCommand( "!armory", acceptedMessages, this, "cmdArmory" );
        m_commandInterpreter.registerCommand( "!upgrade", acceptedMessages, this, "cmdUpgrade" );
        m_commandInterpreter.registerCommand( "!intro", acceptedMessages, this, "cmdIntro" );
        m_commandInterpreter.registerCommand( "!info", acceptedMessages, this, "cmdInfo", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!grant", acceptedMessages, this, "cmdGrant", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!ban", acceptedMessages, this, "cmdBan", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!unban", acceptedMessages, this, "cmdUnban", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!savedata", acceptedMessages, this, "cmdSaveData", OperatorList.HIGHMOD_LEVEL );
        m_commandInterpreter.registerCommand( "!die", acceptedMessages, this, "cmdDie", OperatorList.HIGHMOD_LEVEL );
        //m_commandInterpreter.registerHelpCommand( acceptedMessages, this );

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
       m_botAction.sendPrivateMessage( name, "I don't get what you mean... maybe I can !help you with something?"  );
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
       if( p == null )
           p = m_players.put( name, new DistensionPlayer(name) );

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
                   "!pilot <ship>         - Pilot <ship> if available in your hangar",
                   "!hangar               - View your ships & those available for purchase",
                   "!progress (or .)      - See your progress toward next advancement",
                   "!status               - View current ship's level and upgrades",
                   "!upgrade <upg>        - Upgrade your ship with <upg> from the armory",
                   "!armory               - View ship upgrades available in the armory",
                   "!defect <army>        - (not yet implemented) Defect to <army>",
                   "!scrap <upg>          - (not yet implemented) Sell <upg>"
           };
           m_botAction.privateMessageSpam(name, helps);
       } else {
           String[] helps = {
                   "!progress (or .)      - See your progress toward next advancement",
                   "!status               - View current ship's level and upgrades",
                   "!upgrade <upg>        - Upgrade your ship with <upg> from the armory",
                   "!armory               - View ship upgrades available in the armory",
                   "!pilot <ship>         - Change into <ship> if available in your hangar",
                   "!hangar               - View your ships & those available for purchase",
                   "!dock                 - Dock your ship, recording status to headquarters",
                   "!defect <army>        - (not yet implemented) Defect to <army>",
                   "!scrap <upg>          - (not yet implemented) Sell <upg>"
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


    
    // ***** EVENT PROCESSING *****

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
        if( name == null || name == "Mr. Arrogant 2" || name == m_botAction.getBotName() )
            return;
        m_players.remove( name );
        m_players.put( name, new DistensionPlayer( name ) );
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
        army.adjustStrength( -player.getUpgradeLevel() );
        army.adjustPilotsInGame( -1 );
        
        player.saveCurrentShipToDBNow();

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
        
        // If flag is already claimed, take it away from old freq
        if( flagOwner[flagID] != -1 ) { 
            DistensionArmy army = m_armies.get( new Integer( flagOwner[flagID] ) );
            if( army != null ) {
                army.adjustFlags( -1 );
                if( DEBUG )
                    m_botAction.sendPrivateMessage( p.getPlayerName(), "Flag #" + flagID + " taken from army #" + flagOwner[flagID] );
            }            
        }
        DistensionArmy army = m_armies.get( new Integer( p.getFrequency() ) );
        if( army != null ) {
            army.adjustFlags( 1 );
            if( DEBUG )
                m_botAction.sendPrivateMessage( p.getPlayerName(), "Flag #" + flagID + " added to your army; " + army.getNumFlagsOwned() + " flags now owned");
        }
        
        flagOwner[flagID] = p.getFrequency();
                
        /*
        // Subtract from old holders; increment for new holders
        Integer ownerID = m_flags.get( new Integer(flagID) );
        if( ownerID != null ) {
            DistensionArmy army = m_armies.get( ownerID );
            if( army != null ) {
                army.adjustFlags( -1 );
                if( DEBUG )
                    m_botAction.sendPrivateMessage( p.getPlayerName(), "Flag #" + flagID + " taken from army #" + ownerID );
            }
        }
        DistensionArmy army = m_armies.get( new Integer( p.getFrequency() ) );
        if( army != null ) {
            army.adjustFlags( 1 );
            if( DEBUG )
                m_botAction.sendPrivateMessage( p.getPlayerName(), "Flag #" + flagID + " added to your army; " + army.getNumFlagsOwned() + " flags now owned");
        }

        // Replace old army with the new
        m_flags.put( new Integer(flagID), new Integer(p.getFrequency()) );
        */
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
        if( loser != null ) {
            loser.doSafeWarp();
            if( loser.hasFastRespawn() ) {
                m_prizeQueue.addHighPriorityPlayer( loser );                
            } else {
                m_prizeQueue.addPlayer( loser );
            }            
        } else
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
                    // Loser does not lose any points for dying if controlling both flags
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
                double armySizeWeight = (killedarmy.getTotalStrength() - killerarmy.getTotalStrength());
                if( armySizeWeight != 0 )
                    armySizeWeight /= 2.0;
                else
                    armySizeWeight = 1;

                points = (int)((points * killerarmy.getNumFlagsOwned()) * armySizeWeight);
                if( points < 1 )
                    points = 1;

                victor.addRankPoints( points );
                // Track successive kills for weasel unlock
                victor.addSuccessiveKill();
                loser.clearSuccessiveKills();
                if( DEBUG ) {
                    m_botAction.sendPrivateMessage( killer.getPlayerName(), "DEBUG: " + points + " RP earned; weight=" + armySizeWeight + "; flags=" + killerarmy.getNumFlagsOwned() );
                    //m_botAction.sendPrivateMessage( killed.getPlayerName(), "DEBUG: " + loss + " RP lost; weight=" + armySizeWeight + "; flags=" + killerarmy.getNumFlagsOwned() );
                }
            }
        }
    }


    // ***** COMMAND PROCESSING *****
        

    /**
     * Save all player data.  Sends arena msgs.
     * @param name
     * @param msg
     */
    public void cmdSaveData( String name, String msg ) {
        m_botAction.sendArenaMessage( "Saving player data ..." ,1 );
        int players = 0;
        int playersunsaved = 0;
        for( DistensionPlayer p : m_players.values() ) {
            if( p.saveCurrentShipToDBNow() == false )
                playersunsaved++;
            else
                players++;
        }
        if( playersunsaved == 0 )
            m_botAction.sendArenaMessage( "All " + players + " players saved to the database. -" + name, 1 );
        else
            m_botAction.sendArenaMessage( players + " players saved.  " + playersunsaved + " players could not be saved.  All players please use !dock to save data IMMEDIATELY.", 1 );
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
        if( p == null )
            p = m_players.put( name, new DistensionPlayer(name) );
        else            
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

        // int bonus = 0;
        if( army.isPrivate() ) {
            if( pwd != null && !pwd.equals(army.getPassword()) ) {
                m_botAction.sendPrivateMessage( name, "That's a private army there.  And the password doesn't seem to match up.  Duff off." );
                return;
            }
        } else {
            //if( r.getString( "fcDefaultArmy" ).equals("y") )
            //bonus = calcEnlistmentBonus( armyNum, getDefaultArmyCounts() );
        }

        m_botAction.sendPrivateMessage( name, "Ah, joining " + army.getName().toUpperCase() + "?  Excellent.  You are pilot #" + army.getPilotsTotal() + "." );
        army.adjustPilotsTotal(1);

        p.setArmy( armyNum );
        p.addPlayerToDB();
        p.setShipNum( 0 );

        p.addShipToDB( 1 );
        m_botAction.sendPrivateMessage( name, "Welcome aboard." );
        m_botAction.sendPrivateMessage( name, "If you need an !intro to how things work, I'd be glad to !help out.  Or if you just want to get some action, jump in your new Warbird.  (!pilot 1)" );
        /* ENLISTMENT BONUS -- FIX
                if( bonus > 0 ) {
                    m_botAction.sendPrivateMessage( name, "Your enlistment bonus also entitles you to " + bonus + " free upgrade" + (bonus==1?"":"s")+ ".  Congratulations." );
                    m_botAction.SQLBackgroundQuery( m_database, null, "UPDATE tblDistensionShip SET fnRankPoints='" + bonus + "' WHERE fnPlayerID='" + p.get + "'" );                    
                }
         */
    }


    /**
     * Logs a player in / allows them to return to the game.
     * @param name
     * @param msg
     */
    public void cmdReturn( String name, String msg ) {
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;

        int ship = player.getShipNum();
        if( ship != -1 ) {
            if( ship == 0 )
                m_botAction.sendPrivateMessage( name, "Having some trouble?  What, you need me to !help you?  Might want to !pilot a ship now." );
            else
                m_botAction.sendPrivateMessage( name, "Yeah, very funny -- that's hilarious actually.  Now go kill someone." );
            return;
        }

        if( player.getPlayerFromDB() == true ) {
            m_botAction.sendPrivateMessage( name, "Returning to the hangars of " + player.getArmyName().toUpperCase() + " ..." );
            m_botAction.sendPrivateMessage( name, "Welcome back." );
            player.setShipNum( 0 );
        } else {
            if( player.isBanned() )
                m_botAction.sendPrivateMessage( name, "Sorry ... I don't think I can let you back in.  You've caused enough trouble around here.  If you think I'm wrong, you might want to ask someone for ?help ..." );
            else                
                m_botAction.sendPrivateMessage( name, "I don't have you in my records... you need to !enlist before I can !help you." );
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
            m_botAction.sendPrivateMessage( name, "Exactly which ship do you mean there?  Give me a number.  Maybe check the !hangar first." );
            return;            
        }

        if( player.shipIsAvailable( shipNum ) ) {
            DistensionArmy army = m_armies.get( new Integer(player.getArmyID()) );
            if( player.getShipNum() > 0 ) {
                player.saveCurrentShipToDBNow();
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
                m_botAction.sendPrivateMessage( name, Tools.shipName( player.getShipNum() ).toUpperCase() + ", rank " + player.getRank() + ".  " + player.getPointsToNextRank() + "RP to next rank ( " + player.getRankPoints() + " / " + player.getNextRankPoints() + " )");
                
            } else {
                m_botAction.sendPrivateMessage( name, "Having trouble getting that ship for you.  Please contact a mod." );
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
            army.adjustStrength( -player.getUpgradeLevel() );
            army.adjustPilotsInGame( -1 );
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
        if( player == null )
            return;
        boolean inGame = !(player.getShipNum() == -1);

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
                        Tools.formatString( a.getName(), 43 ) +
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
        for( int i = 0; i < 8; i++ ) {
            if( player.shipIsAvailable(i+1) ) {
                if( currentShip == (i+1) ) {
                    m_botAction.sendPrivateMessage( name, "  " + (i + 1) + "   " + Tools.formatString( Tools.shipName( i+1 ), 20 ) + "IN FLIGHT: Rank " + player.getUpgradeLevel() );
                } else {
                    m_botAction.sendPrivateMessage( name, "  " + (i + 1) + "   " + Tools.formatString( Tools.shipName( i+1 ), 20 ) + "AVAILABLE" );
                }
            } else {
                pf = m_shipGeneralData.get(i);
                if( pf.getRankUnlockedAt() == -1 )
                    m_botAction.sendPrivateMessage( name, "  " + (i + 1) + "   " + Tools.formatString( Tools.shipName( i+1 ), 20 ) + "LOCKED" );
                else
                    m_botAction.sendPrivateMessage( name, "  " + (i + 1) + "   " + Tools.formatString( Tools.shipName( i+1 ), 20 ) + "RANK " + pf.getRankUnlockedAt() + " NEEDED"  );
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
        Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum - 1 ).getAllUpgrades();
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
                
        m_botAction.sendPrivateMessage( name, "You are a rank " + player.getRank() + " " + Tools.shipName(shipNum) + " pilot.  " + player.getUpgradeLevel() + " upgrades installed." );
        m_botAction.sendPrivateMessage( name, "Rank points:  " + player.getRankPoints() + " / " + player.getNextRankPoints() + "  (" + player.getPointsToNextRank() + " needed for rank advancement)" );
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
        Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum - 1 ).getAllUpgrades();
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
                        printmsg += "(NOT INSTALLED)    ";
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
            m_botAction.sendPrivateMessage( name, "You'll need to !return to your army or !enlist in a new one before purchasing a ship." );
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
        ShipUpgrade upgrade = m_shipGeneralData.get( shipNum - 1 ).getUpgrade( upgradeNum );
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
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " on the " + Tools.shipName( shipNum ) + " upgraded to level " + currentUpgradeLevel + 1 + "." );
    }

    
    /**
     * Provides a brief introduction to the game for a player.  (Maybe do an F1 help?)
     * @param name
     * @param msg
     */
    public void cmdIntro( String name, String msg ) {

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
     * @return HashMap containing army# -> num players on army of all default armies.
     */
    public HashMap<Integer,Integer> getDefaultArmyCounts() {
        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fnArmyID, fnNumPilots, fcDefaultArmy FROM tblDistensionArmy WHERE fcDefaultArmy = 'y' ORDER BY fnArmyID ASC" );
            HashMap<Integer,Integer> count = new HashMap<Integer,Integer>();
            while( r.next() ) {
                count.put( new Integer( r.getInt( "fnArmyID" )), new Integer( r.getInt( "fnNumPilots" )) );
            }
            return count;
        } catch (SQLException e ) { return null; }
    }


    /**
     * @return Enlistment bonus for a given default army, based on the size of other default armies.
     */
    public int calcEnlistmentBonus( int armyID, HashMap<Integer,Integer> counts ) {
        if( counts.get( new Integer(armyID)) == null )
            return 0;   // No bonus if this for some reason is not a default army

        int ourcount = counts.remove( new Integer(armyID) ).intValue();

        Iterator<Integer> i = counts.values().iterator();
        int pilots = 0;
        while( i.hasNext() )
            pilots += i.next().intValue();
        if( pilots == 0 || counts.size() == 0 )
            return 0;
        pilots = pilots / counts.size();
        pilots = pilots - ourcount;

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





    // ***** INTERNAL CLASSES *****
    // Players, armies, prizing queue

    /**
     * Used to keep track of player data retreived from the DB, and update data
     * during play, to be later synch'd with the DB.
     */
    private class DistensionPlayer {
        private String name;    // Playername
        private int shipNum;    // Current ship: 1-8, 0 if docked/spectating; -1 if not logged in
        private int upgLevel;   // Number of upgrades on ship;                -1 if docked/not logged in
        private int rank;       // Current rank (# upgrade points awarded);   -1 if docked/not logged in
        private int rankPoints; // Current rank points for ship;              -1 if docked/not logged in
        private int nextRank;   // Rank points needed to earn next rank;      -1 if docked/not logged in
        private int upgPoints;  // Current upgrade points available for ship; -1 if docked/not logged in
        private int armyID;     // 0-9998;                                    -1 if not logged in
        private int successiveKills;            // # successive kills (for unlocking weasel)
        private boolean   warnedForTK;          // True if they TKd / notified of penalty this match
        private boolean   banned;               // True if banned from playing
        private boolean   shipDataSaved;        // True if ship data on record equals ship data in DB
        private boolean   fastRespawn;          // True if player respawns at the head of the queue
        private int[]     purchasedUpgrades;    // Upgrades purchased for current ship
        private boolean[] shipsAvail;           // Marks which ships are available
        //private int       nextUpgrade;          // Which prize is set to be prized next

        public DistensionPlayer( String name ) {
            this.name = name;
            shipNum = -1;
            upgLevel = -1;
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
            //nextUpgrade = 0;
            successiveKills = 0;
        }
        
        /*
        public boolean prizeNext() {
            // Prize all of a specific upgrade (all levels) at once
            int prize = m_shipGeneralData.get(shipNum - 1).getUpgrade( nextUpgrade ).getPrizeNum();
            for( int j = 0; j < purchasedUpgrades[nextUpgrade]; j++ )
                m_botAction.specificPrize( name, prize );

            // Find out which upgrade will be next
            while( purchasedUpgrades[nextUpgrade] == 0 && nextUpgrade < NUM_UPGRADES )
                nextUpgrade++;

            if( nextUpgrade == NUM_UPGRADES ) {
                // All done
                return true;
            }
            return false;
        }
        
        public int setupPrizingAfterDeath() {
            nextUpgrade = 0;
            while( purchasedUpgrades[nextUpgrade] == 0 && nextUpgrade < NUM_UPGRADES )
                nextUpgrade++;
            // For players that have not yet purchased any upgrades
            if( nextUpgrade == NUM_UPGRADES )
                return 0;
            if( fastRespawn )
                return 2;
            else
                return 1;
        }
        */
        
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
         * Adds a ship as available on a player's record, sending to the database
         * as a background query.
         * @param shipNum Ship # to make available
         */
        public void addShipToDB( int shipNumToAdd ) {
            if( shipNumToAdd < 1 || shipNumToAdd > 8 )
                return;
            shipsAvail[ shipNumToAdd - 1 ] = true;
            try {
                m_botAction.SQLQueryAndClose( m_database, "UPDATE tblDistensionPlayer SET fcShip" + shipNumToAdd + "='y' WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
                m_botAction.SQLQueryAndClose( m_database, "INSERT INTO tblDistensionShip ( fnPlayerID , fnShipNum ) VALUES ((SELECT fnID FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name ) + "'), " + shipNumToAdd + ")" );
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
                    
                    // Init all upgrades
                    upgLevel = 0;
                    for( int i = 0; i < NUM_UPGRADES; i++ ) {
                        purchasedUpgrades[i] = r.getInt( "fnStat" + (i + 1) );
                        upgLevel += purchasedUpgrades[i];
                    }
                    shipDataSaved = true;
                } else {
                    shipDataSaved = false;
                }

                // Setup special (aka unusual) abilities
                Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum - 1).getAllUpgrades();
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
            Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum - 1).getAllUpgrades();
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
        }
        
        /**
         * Warps player to the safe after death.
         */
        public void doSafeWarp() {
            int base = armyID % 2;
            if( base == 0 )
                m_botAction.warpTo(name, 512, 249);
            else
                m_botAction.warpTo(name, 512, 773);
            m_botAction.shipReset(name);
        }
        
        /**
         * Advances the player to the next rank.
         */
        public void doAdvanceRank() {
            rank++;
            upgPoints++;
            nextRank = m_shipGeneralData.get( shipNum ).getNextRankCost(rank);
            m_botAction.sendPrivateMessage(name, "Congratulations, you have advanced to rank " + rank + " in the " + Tools.shipName(shipNum) + ".  You will reach the next rank in " + ( nextRank - rankPoints )+ " rank points (total " + nextRank + ").");
            m_botAction.sendPrivateMessage(name, "You have earned 1 upgrade point to spend in the !armory." + ((upgPoints > 1) ? ("  (" + upgPoints + " total available).") : "") );
                        
            switch (rank) {
            case RANK_REQ_SHIP2:
                if( shipsAvail[1] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Javelin.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "JAVELIN: The Javelin is a difficult ship to pilot, but one of the most dangerous in the right hands.  Users of the original Javelin model will feel right at home here.  Javelins are extremely upgradeable ships, devastating any other craft in their highest ranks.");
                    // character limit:                                                                                                                                                                                                                                                                                                                                     //
                    addShipToDB(2);
                }
                break;
            case RANK_REQ_SHIP3:
                if( shipsAvail[2] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Spider.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "SPIDER: The Spider is the mainstay support gunner of every army.  It is an absolutely critical element for securing our hold on this sector.  Upgraded spiders receive regular refuelings, can prevent other ships from using wormholes, and after repairs can receive a 10-second Infinite Energy Stream.");
                    addShipToDB(3);
                }
                break;
            case RANK_REQ_SHIP4:
                if( shipsAvail[3] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the experimental Leviathan.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "LEVIATHAN:");
                    addShipToDB(4);
                }
                break;
            case RANK_REQ_SHIP5:
                if( shipsAvail[4] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Terrier.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "TERRIER: The Terrier is the most important ship in the army, providing a point of support into the fray.  The Terrier is our most rapidly-advancing craft, and advanced Terriers enjoy rearmament preference over other ships.  We resupply weapons and wormhole kits to our most trusted Terriers.");
                    addShipToDB(5);
                }
            case RANK_REQ_SHIP7:
                if( shipsAvail[6] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Lancaster.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "LANCASTER: The Lancaster is an unusual ship with a host of surprises onboard.  Pilots are able to upgrade its most basic components very rapidly.  The Firebloom and its stunning new evasive-bombing capability make this a fantastic choice for advanced pilots.");
                    addShipToDB(7);
                }
            case RANK_REQ_SHIP8:
                if( shipsAvail[7] == false ) {
                    m_botAction.sendPrivateMessage(name, "You have proven yourself a capable enough to fly the Shark.  One has been requisitioned for your use, and is now waiting in your !hangar.");
                    m_botAction.sendPrivateMessage(name, "SHARK: The Shark is piloted by our most clever and resourceful pilots.  Unsung heroes of the army, Sharks are both our main line of defense and leaders of every assault.  Advanced sharks now enjoy light gun capabilities, rearmament preference, and a cloaking device.");
                    addShipToDB(8);
                }
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
            m_armies.get(armyID).adjustStrength( amt );
            shipDataSaved = false;
        }

        /**
         * Sets the current ship player is using.
         * @param shipNum # of ship (1-8)
         */
        public void setShipNum( int shipNum ) {
            if( shipNum < 1 ) {
                upgLevel = -1;
                rank = -1;
                rankPoints = -1;
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
        public void addSuccessiveKill() {
            successiveKills++;            
            
            if( successiveKills == 5 ) {
                int award = 3;
                if( rank > 1 )
                    award = rank * 3;
                m_botAction.sendPrivateMessage(name, "Streak!  (" + award + " RP bonus.)", 19 );                
                addRankPoints(award);
            }
            
            if( successiveKills == 10 ) {
                int award = 5;
                if( rank > 1 )
                    award = rank * 5;
                m_botAction.sendPrivateMessage(name, "ON FIRE!  (" + award + " RP bonus.)", 20 );
                addRankPoints(award);
            }
            
            if( successiveKills == 15 ) {
                int award = 8;
                if( rank > 1 )
                    award = rank * 7;
                m_botAction.sendPrivateMessage(name, "UNSTOPPABLE!  (" + award + " RP bonus.)", 21 );
                addRankPoints(award);
            }
            
            /*
            if( successiveKills >= SUCCESSIVE_KILLS_TO_UNLOCK_WEASEL ) {
                if( shipsAvail[5] == false ) {
                    m_botAction.sendPrivateMessage(name, "DESC");
                    m_botAction.sendPrivateMessage(name, "WEASEL: DESC.");
                    addShipToDB(6);                    
                }                
            }
            */
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
         * @return Returns points needed to the next rank
         */
        public int getPointsToNextRank() {
            return nextRank - rankPoints;
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
         * @return True if the given ship is available.
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


    // ***** GENERIC INTERNAL DATA CLASSES (for static info defined in price setup) *****

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
        
        public int getNextRankCost( int currentRank ) {
            if( currentRank < 10 )
                return (int)(Math.pow(2, currentRank) * baseRankCost * EARLY_RANK_FACTOR);
            else
                return (int)(Math.pow(2, currentRank) * baseRankCost * NORMAL_RANK_FACTOR);
        }
    }


    /**
     * Generic (not player specific) class used for holding info on upgrades for
     * a specific ship -- name of the upgrade, prize number associated with it,
     * base cost, and max level.  The starting level of all upgrades is 0.
     * The base upgrades range from level 0-10.
     *    2^(current level of upgrade) * base cost = cost of next level of upgrade
     *
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
            this.name = name;
            this.prizeNum = prizeNum;
            this.cost = -1;
            this.costDefines = costDefines;
            this.rankRequired = rankRequired;
            this.numUpgrades = numUpgrades;
        }
        
        public ShipUpgrade( String name, int prizeNum, int cost, int[] rankDefines, int numUpgrades ) {
            this.name = name;
            this.prizeNum = prizeNum;
            this.cost = cost;
            this.rankRequired = -1;
            this.rankDefines = rankDefines;
            this.numUpgrades = numUpgrades;
        }

        public ShipUpgrade( String name, int prizeNum, int[] costDefines, int[] rankDefines, int numUpgrades ) {
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

    
    
    
    
    
    // *** SHIP DATA ***
    
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

        // WARBIRD -- starting ship
        // Med upg speed; rotation starts +1, energy has smaller spread, smaller max, & starts v. low
        // 4:  L2 Guns
        // 17: Multi
        // 21: Decoy
        // 27: L3 Guns
        // 36: Mines
        // 42: Thor
        // 48: XRadar
        ShipProfile ship = new ShipProfile( 0, 15 );
        ShipUpgrade upg;
        upg = new ShipUpgrade( "Side Thrusters", Tools.Prize.ROTATION, 1, 0, 9 );           // 20 x9
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Density Reduction Unit", Tools.Prize.THRUST, 1, 0, 10 );    // 1 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Drag Balancer", Tools.Prize.TOPSPEED, 1, 0, 10 );           // 200 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Regeneration Drives", Tools.Prize.RECHARGE, 1, 0, 10 );     // 400 x10
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Microfiber Armor", Tools.Prize.ENERGY, 1, 0, 10 );          // 150 x10
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
        upg = new ShipUpgrade( "Reinforced Plating", Tools.Prize.ENERGY, p2a1, p2a2, 12 );  // 146 x12
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
        upg = new ShipUpgrade( "Molecular Shield", Tools.Prize.ENERGY, 1, 0, 10 );          // 140 x10
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
        //                 speed has 7 lvls, upgrades fast; rotation has 25 levels, upg's slow
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
        int p4a1[] = { 1,  4, 10 };
        int p4a2[] = { 8, 35, 75 };
        upg = new ShipUpgrade( "Spill Guns", Tools.Prize.GUNS, p4a1, p4a2, 3 );         // DEFINE
        ship.addUpgrade( upg );
        int p4b1[] = { 1,  1,  3 };
        int p4b2[] = { 4, 15, 48 };
        upg = new ShipUpgrade( "Chronos(TM) Disruptor", Tools.Prize.BOMBS, p4b1, p4b2, 4 );        // DEFINE
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
        upg = new ShipUpgrade( "Hull Capacity", Tools.Prize.ENERGY, 1, 0, 9 );                  // 150 x9
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
        ship = new ShipProfile( -1, 0 );
        m_shipGeneralData.add( ship );
        ship = new ShipProfile( -1, 0 );
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