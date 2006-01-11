package twcore.bots.distensionbot;

import twcore.core.*;
import java.util.*;
import java.sql.*;


/**
 * DistensionBot -- for the progressive war-game, 'Distension'.
 * 
 * 
 * !ban   dude
 * !unban dude
 * 
 * 
 * Pre-login cmds
 * 
 * `!return       - Checks your enlistment status, and loads profile if it exists.
 * `!enlist #:password - Enlists with a given army.
 * `!armies       - Shows list of armies, their totals, and respective enlistment bonuses. 
 * (armies is also post-login)
 * 
 * Post-login cmds
 * 
 * `!pilot #        - Enter as a given ship.
 * `!dock           - Save and enter spectator mode.
 *
 * `!hangar         - Show available ships, and costs for unavailables.
 * `!buyship #      - Purchase rights to given ship, if possible.
 * 
 * `!armory         - Show contents of the armory, and associated prices.
 * `!upgrade #      - Upgrade a given aspect of current ship.
 * !scrap #        - Scrap (sell) a given upgrade as defined on !report screen.
 * 
 * `!status         - Report of current ship status (upgrades, level, etc.).
 * `!credit         - check cred
 *   
 * !bonus # person - Give an amt of points to comrade, up to max allowed (10?).
 * 
 * !defect #:password - Defect to another army, scrapping all upgrades for all ships.
 * !newarmy name:freq:private?:password    - Creates a new army and transfers you to the army.  No penalty, but costs $
 * 
 * !myfreq  - to their freq when in spec
 * !watch # - inform player when they reach a given point amt
 * 
 * !intro, !intro2, !intro3, etc.
 *
 * 
 * When a player is killed (0 second spawn time):
 *   1. Add pts to killer, as determined.
 *   2. .2 second spawn time, so *shipreset, prize warp, and prize all necessary prizes
 *   (if a TK, the killer loses pts equal to their level)   
 * 
 * Adding up points:
 *   Flag multiplier - No flags=all rewards are 1; 1 flag=regular; 2 flags=x2
 *   
 *   15 levels lower or more           = 1 pt
 *   14 levels below to 9 levels above = level of ship
 *   10 levels above or more           = killer's level + 10
 * 
 * @author qan
 */
public class distensionbot extends SubspaceBot {
    
    private static int SCRAP_PERCENTAGE = 60;               // % of $ returned for scrapping upgrades
    private static int NUM_UPGRADES = 14;                   // Number of upgrade slots allotted per ship
    private static String DB_PROB_MSG = "DAMN!  We've got a database problem!  That last one didn't go through.  Please tell somebody!";
                                                            // Msg displayed for DB error
    private String m_database;                              // DB to connect to
    
    private BotSettings m_botSettings;
    private CommandInterpreter m_commandInterpreter;

    private Vector <ShipProfile>m_shipGeneralData;          // Generic (nonspecific) purchasing data for ships
    private HashMap <String,DistensionPlayer>m_players;     // In-game data on players (Name -> Player)
    private HashMap <Integer,DistensionArmy>m_armies;       // In-game data on armies  (ID -> Army)
    private HashMap <Integer,Integer>m_flags;               // In-game data on flags   (FlagID -> ArmyID that owns)
    
    
    
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
        m_flags = new HashMap<Integer,Integer>();
        setupPrices();
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
        req.request(EventRequester.FLAG_REWARD);
        req.request(EventRequester.FLAG_CLAIMED);
    }

    
    
    // ****** COMMAND PROCESSING ******
    
    /**
     * Registers all commands necessary.
     *
     */
    private void registerCommands() {
        int acceptedMessages = Message.PRIVATE_MESSAGE;

        m_commandInterpreter.registerCommand( "!die", acceptedMessages, this, "cmdDie",         "!die       - Kills DistensionBot.", OperatorList.SMOD_LEVEL );
        
        m_commandInterpreter.registerCommand( "!enlist", acceptedMessages, this, "cmdEnlist",   "!enlist #  - Enlist with army #.  Use !enlist #:pw for a priv. army" );
        m_commandInterpreter.registerCommand( "!return", acceptedMessages, this, "cmdReturn",   "!return    - Return to your current position in the war" );
        m_commandInterpreter.registerCommand( "!pilot", acceptedMessages, this, "cmdPilot",     "!pilot #   - Pilot ship # available in your hangar." );        
        m_commandInterpreter.registerCommand( "!dock", acceptedMessages, this, "cmdDock",       "!dock      - View public armies, soldier counts & enlistment bonuses" );
        m_commandInterpreter.registerCommand( "!armies", acceptedMessages, this, "cmdArmies",   "!armies    - View public armies, soldier counts & enlistment bonuses" );
        m_commandInterpreter.registerCommand( "!hangar", acceptedMessages, this, "cmdHangar",   "!hangar    - View your ships & those available for purchase" );
        m_commandInterpreter.registerCommand( "!buyship", acceptedMessages, this, "cmdBuyship", "!buyship # - Buys a given ship, if it is available" );
        m_commandInterpreter.registerCommand( "!status", acceptedMessages, this, "cmdStatus",   "!status    - View current ship's level and upgrades" );
        m_commandInterpreter.registerCommand( "!credit", acceptedMessages, this, "cmdCredit",   "!credit    - Quick view of your army credit" );
        m_commandInterpreter.registerCommand( "!armory", acceptedMessages, this, "cmdArmory",   "!armory    - View ship upgrades available in your army's armory" );        
        m_commandInterpreter.registerCommand( "!upgrade", acceptedMessages, this, "cmdUpgrade",  "!upgrade # - Upgrades your ship with upgrade # from the armory" );        
        m_commandInterpreter.registerHelpCommand( acceptedMessages, this );

        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "handleInvalidMessage" );
        m_commandInterpreter.registerDefaultCommand( Message.REMOTE_PRIVATE_MESSAGE, this, "handleRemoteMessage" );
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
        m_botAction.sendSmartPrivateMessage( name, "Can't hear ya.  C'mere, and maybe I'll !help you."  );
    }
    
    
    /**
     * Display help based on access level.
     * @param name
     * @param msg
     */
    public void cmdHelp( String name, String msg ) {
        Collection helps = m_commandInterpreter.getCommandHelpsForAccessLevel( m_botAction.getOperatorList().getAccessLevel( name ) );
        m_botAction.privateMessageSpam( name, (String[])helps.toArray() );
    }
    
    
    /**
     * Kills the bot.
     * @param name
     * @param msg
     */
    public void cmdDie( String name, String msg ) {
        try { Thread.sleep(50); } catch (Exception e) {};
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
            m_botAction.sendPrivateMessage( name, "Hmm.  Try again, hot shot.");
            return;
        }
        
        name = name.toLowerCase();
        try {
            if( m_players.get( name ).getShipNum() != -1 ) {
                m_botAction.sendPrivateMessage( name, "Ah, trying to enlist?...  Maybe you'd like to !defect to an army more worthy of your skills.");
                return;
            }
        } catch (NullPointerException e) {
            m_players.put( name, new DistensionPlayer(name) );
        }
        
        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fnArmyID FROM tblDistensionPlayer WHERE fcName = '" + Tools.addSlashesToString( name  ) +"'" );
            if( r.next() ) {
                m_botAction.sendPrivateMessage( name, "Ah, trying to enlist?... No, I know you.  You can !return any time.  After that, maybe you'd like to !defect to an army more worthy of your skills?");
                return;
            }            
        } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); }
        
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

        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT * FROM tblDistensionArmy WHERE fnArmyID = '"+ armyNum +"'" );
            if( r.next() ) {
                int bonus = 0;
                if( r.getString( "fcPrivateArmy" ) == "y" ) {
                    if ( r.getString( "fnPassword" ) != pwd ) {
                        m_botAction.sendPrivateMessage( name, "That's a private army there.  And the password doesn't seem to match up.  Best watch yourself now, or you won't get in ANYWHERE." );
                        return;
                    }
                } else {
                    if( r.getString( "fcDefaultArmy" ) == "y" )
                        bonus = calcEnlistmentBonus( armyNum, getDefaultArmyCounts() );
                }
                m_botAction.sendPrivateMessage( name, "Ah, joining " + r.getString( "fcArmyName" ) + "?  Excellent.  You'll be pilot #" + (r.getString( "fnNumPilots" ) + 1) + "." );
                if( bonus > 0 )
                    m_botAction.sendPrivateMessage( name, "You're also entitled to an enlistment bonus of " + bonus + ".  Congratulations." );
                m_botAction.SQLBackgroundQuery( m_database, null, "UPDATE tblDistensionArmy SET fnNumPilots='" + (r.getInt( "fnNumPilots" ) + 1) + "' WHERE fnArmyID='" + armyNum + "'" );
                
                DistensionPlayer p = m_players.get( name );
                p.addPoints( bonus );
                p.setArmy( armyNum );
                p.addPlayerToDB();
                p.setShipNum( 0 );
                p.addShipToDB( 1 );
                m_botAction.sendPrivateMessage( name, "Welcome aboard.  If you need an !intro to how things work, I'd be glad to !help out.  Or if you just want to get some action, jump in your warbird.  (!pilot 1)" );
            } else {
                m_botAction.sendPrivateMessage( name, "You making stuff up now?  Maybe you should join one of those !armies that ain't just make believe..." );
                return;
            }
        } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); }
    }
    
    
    /**
     * Logs a player in / allows them to return to the game.
     * @param name
     * @param msg
     */
    public void cmdReturn( String name, String msg ) {
        name = name.toLowerCase();
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;
            
        int ship = player.getShipNum();
        if( ship != -1 ) {
            if( ship == 0 ) 
                m_botAction.sendPrivateMessage( name, "Having some trouble?  You need me to, haha, !help you?  Might want to !pilot a ship now." );
            else
                m_botAction.sendPrivateMessage( name, "Yeah, very funny -- that's hilarious actually.  Now go kill someone, jackass." );
            return;
        }
        
        if( player.getPlayerFromDB() == true ) {
            m_botAction.sendPrivateMessage( name, "Returning to the hangars of " + player.getArmyName() + " ..." );
            m_botAction.sendPrivateMessage( name, "Welcome back." );
            player.setShipNum( 0 );
        } else {
            m_botAction.sendPrivateMessage( name, "I don't have you in my records... you need to !enlist before I can !help you." );
        }
    }

    
    /**
     * Enters a player as a given ship.
     * @param name
     * @param msg
     */
    public void cmdPilot( String name, String msg ) {
        name = name.toLowerCase();
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
        }

        if( player.shipIsAvailable( shipNum ) ) {
            if( player.getShipNum() > 0 )
                player.saveCurrentShipToDB();
            DistensionArmy army = m_armies.get( new Integer(player.getArmyID()) );
            if( army != null )
                army.adjustStrength( -player.getLevel() );

            player.setShipNum( shipNum );
            if( player.getCurrentShipFromDB() ) {
                if( army == null ) {
                    army = new DistensionArmy( player.getArmyID() );
                    m_armies.put( new Integer(player.getArmyID()), army );
                }
                army.adjustStrength( player.getLevel() );
                
                m_botAction.setShip( name, shipNum );
                m_botAction.setFreq( name, player.getArmyID() );
                player.prizeUpgrades();
            } else {
                m_botAction.sendPrivateMessage( name, "I'm sorry, I'm having trouble getting that ship for you.  Contact a mod immediately." );                
            }
        } else {
            m_botAction.sendPrivateMessage( name, "You don't own that ship.  Check your !hangar before you try flying something you don't have." );            
        }
    }
    
    
    /**
     * Docks player (that is, sends them to spectator mode -- not the sex act, or the TW sysop).
     * @param name
     * @param msg
     */
    public void cmdDock( String name, String msg ) {
        name = name.toLowerCase();
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;       

        if( player.getShipNum() < 1) {
            m_botAction.sendPrivateMessage( name, "Hmm.  Pretty sure you're already docked there.  Want a look at the !hangar while you're here?" );
            return;
        }

        DistensionArmy army = m_armies.get( new Integer(player.getArmyID()) );
        if( army != null )
            army.adjustStrength( -player.getLevel() );
        player.saveCurrentShipToDB();
        player.setShipNum( 0 );
        m_botAction.specWithoutLock( name );
        m_botAction.sendPrivateMessage( name, "Ship status confirmed and logged.  You are now docked.");
    }

    
    /**
     * Shows list of public armies with their current counts and enlistment bonuses.
     * @param name
     * @param msg
     */
    public void cmdArmies( String name, String msg ) {        
        try {
            ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fnArmyID, fcArmyName, fnNumPilots, fcDefaultArmy FROM tblDistensionArmy WHERE fcPrivateArmy = 'n' ORDER BY fnArmyID ASC" );
            HashMap<Integer,Integer> allcount     = new HashMap<Integer,Integer>();
            HashMap<Integer,Integer> defaultcount = new HashMap<Integer,Integer>();
            HashMap<Integer,String>  names        = new HashMap<Integer,String>();
            while( r.next() ) {
                allcount.put( new Integer( r.getInt( "fnArmyID" )), new Integer( r.getInt( "fnNumPilots" )) );
                names.put( new Integer( r.getInt( "fnArmyID" )), new String( r.getString( "fcArmyName" )) );
                if( r.getString("fcDefaultArmy") == "y" ) {
                    defaultcount.put( new Integer( r.getInt( "fnArmyID" )), new Integer( r.getInt( "fnNumPilots" )) );
                }
            }

            m_botAction.sendPrivateMessage( name, Tools.formatString("#", 4 ) + Tools.formatString("Army Name", 40 ) + "Pilots" + "   Enlistment Bonus" );
            
            Iterator i = allcount.keySet().iterator();
            Integer armyNum, armyCount;
            int bonus;
            String armyName;
            while( i.hasNext() ) {
                armyNum = (Integer)i.next();
                armyCount = allcount.get( armyNum );
                armyName = names.get( armyNum );
                if( defaultcount.containsKey(armyNum) )
                    bonus = calcEnlistmentBonus( armyNum, defaultcount );
                else
                    bonus = 0;
                m_botAction.sendPrivateMessage( name, Tools.formatString( armyNum.toString(), 4 )   + Tools.formatString( armyName, 43 ) +
                                                      Tools.formatString( armyCount.toString(), 6 ) + bonus + "c" );
            }
        } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); }
        
    }
    

    /**
     * Shows the status of all ships available in the hangar, and costs for those
     * not yet purchased, if they can be purchased.
     * @param name
     * @param msg
     */
    public void cmdHangar( String name, String msg ) {
        name = name.toLowerCase();
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
                    m_botAction.sendPrivateMessage( name, "  " + (i + 1) + "   " + Tools.formatString( Tools.shipName( i+1 ), 20 ) + "IN FLIGHT: Level " + player.getLevel() );                    
                } else {
                    m_botAction.sendPrivateMessage( name, "  " + (i + 1) + "   " + Tools.formatString( Tools.shipName( i+1 ), 20 ) + "AVAILABLE" );                    
                }
            } else {
                pf = m_shipGeneralData.get(i);
                if( pf.getCost() == -1 )
                    m_botAction.sendPrivateMessage( name, "  " + (i + 1) + "   " + Tools.formatString( Tools.shipName( i+1 ), 20 ) + "LOCKED" );
                else
                    m_botAction.sendPrivateMessage( name, "  " + (i + 1) + "   " + Tools.formatString( Tools.shipName( i+1 ), 20 ) + pf.getCost() + "c" );
            }
        }
    }


    /**
     * Adds a given ship to a player's hangar if purchasable and the player has sufficient funds.
     * @param name
     * @param msg
     */
    public void cmdBuyship( String name, String msg ) {
        name = name.toLowerCase();
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;       
        
        if( player.getShipNum() == -1 ) {
            m_botAction.sendPrivateMessage( name, "You'll need to !return to your army or !enlist in a new one before purchasing a ship." );
            return;            
        }
        
        int shipNum = 0;
        try {
            shipNum = Integer.parseInt( msg );
        } catch ( Exception e ) {
            m_botAction.sendPrivateMessage( name, "Exactly which ship do you mean there?  Give me a number.  Maybe check the !hangar first." );
        }

        if( player.shipIsAvailable( shipNum ) ) {
            m_botAction.sendPrivateMessage( name, "You already own that ship.  Take a look at the !hangar if you're unsure." );
        } else {
            ShipProfile pf = m_shipGeneralData.get(shipNum - 1);
            int cost = pf.getCost();
            if( cost == -1 ) {
                m_botAction.sendPrivateMessage( name, "Not available for purchase.  Not sure how you might go about getting it, either..." );
            } else {
            	if( cost > player.getPoints() ) {
                    m_botAction.sendPrivateMessage( name, "You don't have enough !credit to purchase that.  Work a little harder." );            		
            	} else {
            		player.addPoints( -cost );
                    player.addShipToDB( shipNum );
                    m_botAction.sendPrivateMessage( name, "You're now the owner of a new " + Tools.shipName( shipNum) + "." );
            	}
            }
        }
    }
    

    /**
     * Shows current status of ship -- its level and upgrades.
     * @param name
     * @param msg
     */
    public void cmdStatus( String name, String msg ) {
        name = name.toLowerCase();
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;       
        int shipNum = player.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( name, "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );
            return;
        } else if( shipNum == 0 ){
            m_botAction.sendPrivateMessage( name, name.toUpperCase() + ":  DOCKED.  [Your data is saved; safe to leave arena.]" );
            m_botAction.sendPrivateMessage( name, "CREDIT:  " + player.getPoints() + "c");
            return;
        }
        m_botAction.sendPrivateMessage( name, name.toUpperCase() + " of " + player.getArmyName().toUpperCase() + ":  " + Tools.shipName( shipNum ).toUpperCase() +
                                              " - LEVEL " + player.getLevel() );        	
        m_botAction.sendPrivateMessage( name, "CREDIT:  " + player.getPoints() + "c" );
        Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum - 1 ).getAllUpgrades();
        ShipUpgrade currentUpgrade;
        int[] purchasedUpgrades = player.getPurchasedUpgrades();
        String printmsg;
        for( int i = 0; i < NUM_UPGRADES; i++ ) {
            currentUpgrade = upgrades.get( i );
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
            if( currentUpgrade.getMaxLevel() != -1 )
                m_botAction.sendPrivateMessage( name, printmsg );
        }
    }
    

    /**
     * Quick view of credits.
     * @param name
     * @param msg
     */
    public void cmdCredit( String name, String msg ) {
        name = name.toLowerCase();
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;       
        int shipNum = player.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( name, "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );
            return;
        }
        m_botAction.sendPrivateMessage( name, "CREDIT:  " + player.getPoints() + "c");        
    }

        
    /**
     * Shows upgrades available for purchase.
     * @param name
     * @param msg
     */
    public void cmdArmory( String name, String msg ) {
        name = name.toLowerCase();
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;       
        int shipNum = player.getShipNum();
        if( shipNum == -1 ) {
            m_botAction.sendPrivateMessage( name, "I don't know who you are yet.  Please !return to your army, or !enlist if you have none." );
            return;
        } else if( shipNum == 0 ){
            m_botAction.sendPrivateMessage( name, "If you want to see the armory's selection, you'll need to !pilot a ship first." );
            return;
        }
        m_botAction.sendPrivateMessage( name, "Available Upgrades: " + player.getArmyName() + " Armory - " + Tools.shipName( shipNum ).toUpperCase() ); 
        m_botAction.sendPrivateMessage( name, "#  Name                          Curr /  Max      Credit" ); 
        Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum - 1 ).getAllUpgrades();
        ShipUpgrade currentUpgrade;
        int[] purchasedUpgrades = player.getPurchasedUpgrades();
        String printmsg;
        boolean printCost;
        for( int i = 0; i < NUM_UPGRADES; i++ ) {
            printCost = true;
            currentUpgrade = upgrades.get( i );
            printmsg = (i+1 < 10 ? " " : "") + (i + 1) + ": " + Tools.formatString( currentUpgrade.getName(), 30);
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
                                 currentUpgrade.getMaxLevel() + (currentUpgrade.getMaxLevel() < 10 ? " " : "") + " )", 18);
                }
            }            
            if( printCost )
                printmsg += currentUpgrade.getCostDefine( purchasedUpgrades[i] ) + "c";            
            if( currentUpgrade.getMaxLevel() != -1 )
                m_botAction.sendPrivateMessage( name, printmsg );
        }        
    }
    

    /**
     * Upgrades a particular aspect of the current ship.
     * @param name
     * @param msg
     */
    public void cmdUpgrade( String name, String msg ) {
        name = name.toLowerCase();
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
        ShipUpgrade upgrade = m_shipGeneralData.get( shipNum - 1 ).getUpgrade( upgradeNum - 1 );
        int current = player.getPurchasedUpgrade( upgradeNum - 1 );
        if( upgrade == null || current == -1 ) {
            m_botAction.sendPrivateMessage( name, "Exactly which do you mean there?  Maybe check the !armory first.  #" + upgradeNum + " doesn't work for me." );
            return;
        }
        if( current >= upgrade.getMaxLevel() ) {
            m_botAction.sendPrivateMessage( name, "You've upgraded that one as much as you can, I think." );
            return;
        }
        int cost = upgrade.getCostDefine( current );
        if( cost > player.getPoints() ) {
            m_botAction.sendPrivateMessage( name, "Need more !credit to cover that one.  Go kill some people, and let's see you enjoy it, eh?" );
            return;
        }
        player.addPoints( -cost );
        player.modifyUpgrade( upgradeNum - 1, 1 );
        if( upgrade.getMaxLevel() == 1 )
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " has been installed on the " + Tools.shipName( shipNum ) + "." );
        else 
            m_botAction.sendPrivateMessage( name, upgrade.getName() + " on the " + Tools.shipName( shipNum ) + " upgraded to level " + current + 1 + "." );
    }
    
    
    
    // ****** EVENT PROCESSING ******

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
        m_botAction.setMessageLimit( 10 );
    }
    
    
    /**
     * Enable reliable tracking of kills on joining of arena.
     * @param event Event to handle.
     */
    public void handleEvent(ArenaJoined event) {
        m_botAction.setReliableKills(1);
    }
       
    
    /**
     * Track players as they enter, and provide basic help on playing Distension.
     * @param event Event to handle.
     */
    public void handleEvent(PlayerEntered event) {
        String name = event.getPlayerName();
        if( name == null )
            return;
        if( name == "Mr. Arrogant 2")
            return;
        name = name.toLowerCase();
        m_players.remove( name );
        m_players.put( name, new DistensionPlayer( name ) );
        m_botAction.sendPrivateMessage( name, "Welcome to Distension.  You a !return pilot?  Or will you !enlist in one of our !armies?" );
    }
    
    
    /**
     * Save player data if unexpectedly leaving Distension.
     * @param event Event to handle.
     */
    public void handleEvent(PlayerLeft event) {
        String name = m_botAction.getPlayerName(event.getPlayerID());
        if( name == null )
            return;
        name = name.toLowerCase();
        DistensionPlayer player = m_players.get( name );
        if( player == null )
            return;
        player.savePlayerToDB();
        player.saveCurrentShipToDB();
        
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
        
        // Subtract from old holders; increment for new holders
        Integer ownerID = m_flags.get( new Integer(flagID) );
        if( ownerID != null ) {
            DistensionArmy army = m_armies.get( ownerID );
            if( army != null )
                army.adjustFlags( -1 );
        }
        DistensionArmy army = m_armies.get( p.getFrequency() );
        if( army != null )
            army.adjustFlags( 1 );

        // Replace old army with the new
        m_flags.put( new Integer(flagID), new Integer(p.getFrequency()) );    
    }
    

    /**
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
            loser.prizeUpgradesAfterDeath();
        else
            return;
        
        if( killer != null ) {
            DistensionPlayer victor = m_players.get( killer.getPlayerName() );
            if( victor == null )
                return;

            // IF TK: TKer loses points equal to their level, and they are notified
            // of it if they have not yet been notified this match.
            if( killed.getFrequency() == killer.getFrequency() ) {
                victor.addPoints( -victor.getLevel() );
                if( !victor.wasWarnedForTK() ) {
                    m_botAction.sendPrivateMessage( killer.getPlayerName(), "You have lost " + victor.getLevel() + " army credits for team-killing " + killed.getPlayerName() + "." );
                    victor.setWarnedForTK();
                }
            // Otherwise: Add points via level scheme
            } else {
                DistensionArmy killerarmy = m_armies.get( new Integer(killer.getFrequency()) );
                DistensionArmy killedarmy = m_armies.get( new Integer(killed.getFrequency()) );
                if( killerarmy == null || killedarmy == null )
                    return;                

                if( killerarmy.getNumFlagsOwned() == 0 ) {
                    victor.addPoints( 1 );
                    return;
                }
                                
                int points;
                if( loser.getLevel() - victor.getLevel() >= 10 )
                    points = victor.getLevel() + 10;
                else if( loser.getLevel() - victor.getLevel() <= -15 )
                    points = 1;
                else
                    points = loser.getLevel();
                double armySizeWeight = (killedarmy.getTotalStrength() - killerarmy.getTotalStrength());
                if( armySizeWeight != 0 )
                    armySizeWeight /= 2.0;
                points = (int)((points * killerarmy.getNumFlagsOwned()) * armySizeWeight);
                victor.addPoints( points );
            }
        }
    }

    
    
    // ****** ASSISTANCE METHODS ******
    
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
        
        // If avg # pilots on other armies is less than # on this one, no bonus for you!
        if( pilots <= 0 )
            return 0;      
        // Diff is greater than 25?  Stick to 25
        if( pilots > 25 )
            pilots = 25;
        return pilots * 10;
    }
    
    
    /**
     * Add ships and their appropriate upgrades to m_shipGeneralData
     */
    public void setupPrices() {
        /*
15000~    10
7500~      9
3600~      8
1920       7
960        6
480        5
240        4
120        3
60         2
30         1
           0
         */
        
        // WARBIRD
        ShipProfile ship = new ShipProfile( 0 );
        ShipUpgrade upg = new ShipUpgrade( "Microfiber Armor", 2, 33, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Regeneration Drives", 1, 28, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Drag Balancer", 12, 29, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Density Reduction Unit", 11, 29, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Side Thrusters", 3, 31, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "High-Impact Cannon", 8, 520, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombing ability disabled)", 9, 0, 0 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Wave-Splitter", 15, 3000, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Detection System", 6, 17000, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Holograph Unit", 23, 2000, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Outer-Dimensional Expulsor", 24, 11000, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S2", 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S3", 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S4", 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // JAVELIN
        ship = new ShipProfile( 3500 );
        upg = new ShipUpgrade( "Reinforced Plating", 2, 27, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Tactical Engineering Crew", 1, 35, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Engine Reoptimization", 12, 31, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Fuel Economizer", 11, 28, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Balancing Streams", 3, 29, 10 );
        ship.addUpgrade( upg );
        int p1[] = { 200, 7800 };
        upg = new ShipUpgrade( "Auxillary Defense System", 8, p1, 3 );        // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Refractive Mortaring", 9, 6000, 1 );       // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Modified Defense Cannon", 15, 2600, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Detection System", 6, 3800, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Mock Javelin", 23, 2100, 1 );
        ship.addUpgrade( upg );
        int p3[] = { 250, 1050, 2900, 6700, 12000 };
        upg = new ShipUpgrade( "Splintering Mortar", 19, p3, 5 );        // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Emergency Fuel Supply", 27, 1500, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S3", 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S4", 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // SPIDER
        ship = new ShipProfile( 700 );
        upg = new ShipUpgrade( "Molecular Shield", 2, 33, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Recompensator", 1, 27, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Spacial Filtering", 12, 35, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Sling Drive", 11, 29, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Central Realigner", 3, 26, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Rapid Disintigrator", 8, 13000, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombing ability disabled)", 9, 0, 0 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Rear Projector", 15, 4200, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Detection System", 6, 12600, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Light Emitter", 23, 2050, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Quantum Stabilizer", 20, 17350, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Inbound Arsenal Enhancer", 17, 8900, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S3", 0, 0, -1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S4", 0, 0, -1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // LEVIATHAN
        ship = new ShipProfile( 9000 );
        upg = new ShipUpgrade( "Carbon-Forced Armor", 2, 23, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Power Recirculator", 1, 32, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Collection Drive", 12, 38, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Force Thrusters", 11, 29, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Gravitational Modifier", 3, 34, 10 );
        ship.addUpgrade( upg );
        int p4[] = { 360, 6200, 14700 };
        upg = new ShipUpgrade( "Spill Guns", 8, p4, 3 );         // DEFINE
        ship.addUpgrade( upg );
        int p5[] = { 1350, 9500 };
        upg = new ShipUpgrade( "Planet Recalibrator", 9, p5, 2 );        // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Optional Radial Spill", 15, 1800, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Detection System", 6, 16000, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Clone Device", 23, 2900, 1 );
        ship.addUpgrade( upg );
        int p6[] = { 150, 600, 8000 };
        upg = new ShipUpgrade( "Deflective Shields", 21, p6, 3 );        // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Wormhole Creation Kit", 28, 750, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Single-Use Propellant", 27, 1700, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Scrambler", 4, 1350, 1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // TERRIER
        ship = new ShipProfile( 1100 );
        upg = new ShipUpgrade( "Hull Capacity", 2, 37, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Full Reconstructor", 1, 27, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Microspiral Drive", 12, 24, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Interwoven Propulsor", 11, 28, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Correction Engine", 3, 27, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Plasma Defense Unit", 8, 9000, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombing ability disabled)", 9, 0, 0 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Multiple Cannons", 15, 1400, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Detection System", 6, 400, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Image Projector", 23, 2400, 1 );
        ship.addUpgrade( upg );
        int p7[] = { 280, 8150 };
        upg = new ShipUpgrade( "Wormhole Creation Kit", 28, p7, 2 );        // DEFINE
        ship.addUpgrade( upg );
        int p8[] = { 150, 5200, 18650 };
        upg = new ShipUpgrade( "Rebounding Distractive", 22, p8, 3 );       // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Light-Bending Unit", 5, 12000, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S4", 0, 0, 0 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );
        
        // WEASEL
        ship = new ShipProfile( -1 );
        upg = new ShipUpgrade( "Cerebral Shielding", 2, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Influx Recapitulator", 1, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Time Distortion Mechanism", 12, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Gravity Shifter", 11, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Orbital Force Unit", 3, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Low-Propulsion Cannons", 8, 0, 2 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Bombing ability disabled)", 9, 0, 0 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Ionic Distributor", 15, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Detection System", 6, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Reiteration Unit", 23, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Light-Bending Unit", 5, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Scrambler", 4, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Movement Inhibitor", 26, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Assault Boosters", 27, 0, 1 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // LANCASTER
        ship = new ShipProfile( -1 );
        upg = new ShipUpgrade( "Interlocked Deflector", 2, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Pneumatic Refiltrator", 1, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Streamlining Unit", 12, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "InitiaTek Burst Engine", 11, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Directive Realigner", 3, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Modernized Projector", 8, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Hole-Sniffer", 9, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Magnified Output Force", 15, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Detection System", 6, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Hired Imitator", 23, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Katarina's Flower", 22, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S2", 0, 0, 0 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S3", 0, 0, 0 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "S4", 0, 0, 0 );
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );

        // SHARK
        ship = new ShipProfile( -1 );
        upg = new ShipUpgrade( "Projectile Slip Plates", 2, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Light Charging Mechanism", 1, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Space-Force Emulsifier", 12, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Spitfire Thrusters", 11, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Runningside Correctors", 3, 0, 10 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Emergency Defense Cannon", 8, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Plasma Drop Upgrade", 9, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "(Multifire not available)", 15, 0, 0 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Detection System", 6, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Cloning Chamber", 23, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Gravitational Repulsor", 21, 0, 5 );    // DEFINE
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Splintering Unit", 19, 0, 3 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Radar Scrambler", 4, 0, 1 );
        ship.addUpgrade( upg );
        upg = new ShipUpgrade( "Inbound Protection Field", 18, 0, 1 );   // DEFINE
        ship.addUpgrade( upg );
        m_shipGeneralData.add( ship );
        
    }
    
    
    
    // ****** INTERNAL CLASSES ******

    /**
     * Used to keep track of player data retreived from the DB, and update data
     * during play, to be later synch'd with the DB.
     */
    private class DistensionPlayer {
        private String name;    // Playername
        private int shipNum;    // Current ship: 1-8, 0 if docked/spectating; -1 if not logged in
        private int shipLevel;  // Combination of levels of all upgrades;     -1 if docked/not logged
        private int points;     // Current points;                            -1 if not logged in
        private int armyID;     // 0-9998;                                    -1 if not logged in
        private boolean   warnedForTK;          // True if they TKd / notified of penalty this match
        private boolean   banned;               // True if banned from playing
        private boolean   pointsSaved;          // True if points on record equal points in DB
        private boolean   upgradesSaved;        // True if upgrades on record equal upgrades in DB
        private int[]     purchasedUpgrades;    // Upgrades purchased for current ship 
        private boolean[] shipsAvail;           // Marks which ships are available
        
        public DistensionPlayer( String name ) {
            this.name = name;
            shipNum = -1;
            shipLevel = -1;
            points = 0;
            armyID = -1;
            warnedForTK = false;
            banned = false;
            pointsSaved = false;
            upgradesSaved = false;
            purchasedUpgrades = new int[NUM_UPGRADES];
            shipsAvail = new boolean[8];
            for( int i = 0; i < 8; i++ )
                shipsAvail[i] = false;
        }
        
        /**
         * Shipresets & prizes warp, then prizes upgrades to player based on
         * what has been purchased.
         */
        public void prizeUpgradesAfterDeath() {
            m_botAction.shipReset( name );
            m_botAction.specificPrize( name, 7 );
            prizeUpgrades();
        }
        
        /**
         * Prizes upgrades to player based on what has been purchased.
         */
        public void prizeUpgrades() {
            Vector<ShipUpgrade> upgrades = m_shipGeneralData.get( shipNum - 1).getAllUpgrades();
            for( int i = 0; i < NUM_UPGRADES; i++ )
                for( int j = 0; j < purchasedUpgrades[i]; j++ )
                    m_botAction.specificPrize( name, upgrades.get( i ).getPrizeNum() );
        }

        /**
         * Creates a player record in the database.
         */
        public void addPlayerToDB() {
            try {
                m_botAction.SQLQuery( m_database, "INSERT INTO tblDistensionPlayer ( fcName , fnArmyID, fnPoints ) " +
                                                  "VALUES ('" + Tools.addSlashesToString( name ) + "', '" + armyID + "', '" + points + "')" );
            } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); }
        }
        
        /**
         * Saves necessary player data to the database.
         */
        public void savePlayerToDB() {
            if( !pointsSaved ) {
                m_botAction.SQLBackgroundQuery( m_database, null, "UPDATE tblDistensionPlayer SET fnPoints='" + points + "' WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
                pointsSaved = true;
            }
        }
        
        /**
         * Reads in data about the player from the database.
         * @return True if data is retrieved; false if it wasn't.
         */
        public boolean getPlayerFromDB() {
            try {
                ResultSet r = m_botAction.SQLQuery( m_database, "SELECT * FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
                if( r.next() ) {
                    banned = (r.getString( "fcBanned" ) == "y" ? true : false);
                    if( banned == true )
                        return false;
                    
                    armyID = r.getInt( "fnArmyID" );
                    points = r.getInt( "fnPoints" );                    
                    for( int i = 0; i < 8; i++ )
                        shipsAvail[i] = ( r.getString( "fcShip" + (i + 1) ) == "y" ? true : false );
                    pointsSaved = true;
                } else {
                    pointsSaved = false;
                }
                return pointsSaved;
            } catch (SQLException e ) {
                Tools.printStackTrace("Problem fetching returning player", e);
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
                m_botAction.SQLQuery( m_database, "UPDATE tblDistensionPlayer SET fcShip" + shipNumToAdd + "='y' WHERE fcName='" + Tools.addSlashesToString( name ) + "'" );
                m_botAction.SQLQuery( m_database, "INSERT INTO tblDistensionShip ( fnPlayerID , fnShipNum ) VALUES ((SELECT fnID FROM tblDistensionPlayer WHERE fcName='" + Tools.addSlashesToString( name ) + "'), '" + shipNumToAdd + "')" );
            } catch (SQLException e ) { m_botAction.sendPrivateMessage( name, DB_PROB_MSG ); }
        }
        
        /**
         * Saves the current ship to the DB, handling as a background query so as not to interrupt the process.
         */
        public void saveCurrentShipToDB() {
            if( shipNum < 1 || shipNum > 9 || upgradesSaved )
                return;
            
            String query = "UPDATE tblDistensionShip ship, tblDistensionPlayer player SET ";
 
            query +=       "ship.fnStat1='" + purchasedUpgrades[0];
            for( int i = 1; i < NUM_UPGRADES; i++ )
                query +=   ", ship.fnStat" + (i + 1) + "='" + purchasedUpgrades[i] + "'";
            
            query +=       " WHERE player.fcName = '" + Tools.addSlashesToString( name ) + "' AND " +
                           "ship.fnPlayerID = player.fnID AND " +
                           "ship.fnShipNum = '" + shipNum;
            // Background it, but don't worry about results for obvious reasons
            m_botAction.SQLBackgroundQuery( m_database, null, query );
            upgradesSaved = true;
        }
        
        public boolean getCurrentShipFromDB() {
            try {
                String query = "SELECT * FROM tblDistensionShip ship, tblDistensionPlayer player " +
                               "WHERE player.fcName = '" + Tools.addSlashesToString( name ) + "' AND " +
                               "ship.fnPlayerID = player.fnID AND " +
                               "ship.fnShipNum = '" + shipNum + "'";
                
                ResultSet r = m_botAction.SQLQuery( m_database, query );
                if( r == null )
                    return false;
                if( r.next() ) {
                    shipLevel = 0;
                    for( int i = 0; i < NUM_UPGRADES; i++ ) {
                        purchasedUpgrades[i] = r.getInt( "fnStat" + (i + 1) );
                        shipLevel += purchasedUpgrades[i];
                    }
                    upgradesSaved = true;
                } else {
                    upgradesSaved = false;
                }
                return upgradesSaved;
            } catch (SQLException e ) {
                return false;
            }
        }
       
        /**
         * Adds # pts to total point amt.
         * @param points Amt to add
         */
        public void addPoints( int points ) {
            this.points += points;
            pointsSaved = false;
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
        }
        
        /**
         * Sets the current ship player is using.
         * @param shipNum # of ship (1-8)
         */
        public void setShipNum( int shipNum ) {
            this.shipNum = shipNum;
            if( shipNum == 0 )
                shipLevel = 0;
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
         * @return Returns points.
         */
        public int getPoints() {
            return points;
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
            DistensionArmy army = m_armies.get( armyID );
            if( army == null ) {
                army = new DistensionArmy( armyID );
                m_armies.put( armyID, army );
            }
            return army.getName();
        }

        /**
         * @return Returns level of ship (combined levels of all upgrades).
         */
        public int getLevel() {
            return shipLevel;
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
    }
        
    
    /**
     * Used to keep track of number of players on a given army.
     */
    private class DistensionArmy {
        int armyID;
        int totalStrength;              // Combined levels of all pilots of the army
        int flagsOwned;                 // # flags currently owned
        String armyName;                // Name of army
        
        /**
         * Creates record for an army, given an ID.
         * @param armyID ID of the army
         */
        public DistensionArmy( int armyID ) {
            this.armyID = armyID;
            totalStrength = 0;
            flagsOwned = 0;
            try {
                ResultSet r = m_botAction.SQLQuery( m_database, "SELECT fcArmyName FROM tblDistensionArmy WHERE fnArmyID = '"+ armyID + "'" );
                if( r.next() )
                    armyName = r.getString( "fcArmyName" );
            } catch( Exception e ) {
            }
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
    }
    
    
    // GENERIC INTERNAL DATA CLASSES (for static info defined in price setup)
    
    /**
     * Generic (not player specific) class used for holding basic info on a ship --
     * essentially just its number, cost (if applicable), and upgrade info.
     */
    private class ShipProfile {
        int shipCost;					  // Cost in points; -1 if can not be purchased
        Vector <ShipUpgrade>upgrades;  // List containing upgrade data for this ship        
        
        public ShipProfile( int shipCost ) {
            this.shipCost = shipCost;
            upgrades = new Vector<ShipUpgrade>(NUM_UPGRADES);
        }
        
        public void addUpgrade( ShipUpgrade upgrade ) {
            upgrades.add( upgrade );
        }
        
        public ShipUpgrade getUpgrade( int upgradeNum ) {
        	if( upgradeNum < 1 || upgradeNum > NUM_UPGRADES )
        		return null;
        	else
        		return upgrades.get( upgradeNum );
        }
        
        public Vector<ShipUpgrade> getAllUpgrades() {
        	return upgrades;
        }
        
        public int getCost() {
            return shipCost;
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
        String name;				// Upgrade name
        int prizeNum;				// Prize associated with the upgrade
        int baseCost;				// Cost associated with each level of upgrade.  -1 if array-defined.
        int maxLevel;				// Highest level the upgrade can reach; 1=one-time upgrade; 0=not available; -1=NA and don't number
        int[] costDefines;          // (For precise cost definitions not based on standard formula)
                
        public ShipUpgrade( String name, int prizeNum, int baseCost, int maxLevel ) {
            this.name = name;
            this.prizeNum = prizeNum;
            this.baseCost = baseCost;
            this.maxLevel = maxLevel;
            costDefines = null;
        }
        
        public ShipUpgrade( String name, int prizeNum, int[] costDefines, int maxLevel ) {
            this.name = name;
            this.prizeNum = prizeNum;
            this.costDefines = costDefines;
            this.maxLevel = maxLevel;
            baseCost = -1;
        }

        /**
         * @return Returns the baseCost.  -1 if array-defined.
         */
        public int getBaseCost() {
            return baseCost;
        }
        
        /**
         * @return Returns max level of the upgrade; 1=one time upgrade; 0=not available
         */
        public int getMaxLevel() {
            return maxLevel;
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
         * @return Returns the cost of upgrading to the next level of this upgrade.
         */
        public int getCostDefine( int currentLevel ) {
            if( costDefines == null )
                return ((int)Math.pow(2, currentLevel)) * baseCost;
            else            
                return costDefines[ currentLevel ];
        }
    }
}