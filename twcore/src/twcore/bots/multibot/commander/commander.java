/************************************************************************************
 *                                                                                  *
    twbotcommander.java - Commander Module - qan
 *                                                                                  *
    Created 6/04/2004 - Last modified 6/14/04
 *                                                                                  *
 ************************************************************************************

      Commander is an attempt to bring a splash of real-time strategy into
    Trench Wars via a fairly elaborate point system, ship hierarchy, and command set.


      GAMEPLAY:

      - Two armies compete for points.  The one with the most points at the end of
    a given amount of time is declared the winner.
      - Each army starts with one Commander (who remains in spec the entire time),
    one Lead Terr, and one Lead Levi.  The rest of the players start as warbirds.
      - Levis (and javelins) are the main point-earners.  Every kill by a levi (or a
    jav) earns the team 1 point.  1 point is also earned for every 3 kills by an
    upgraded support ship -- spider, terrier, lanc, or shark.
      - Every time a warbird (the basic foot soldier) kills a levi, that warbird
    becomes a levi, capable of earning points for the team.  The levi is the basic
    ship that can evolve into any other ship.  Of course, changing a levi into
    a support ship has consequences: that ship can no longer earn points, but that ship
    also isn't a target for the enemy to turn their WBs into levis.
      - If there are enough team points, the Commander can issue an upgrade for a
    ship through the bot.  The problem is, those same points are what will win the
    game -- so it's a careful balance between getting the ships you need to match
    the enemy, and conserving your points for your victory.  Each game will be unique
    as the Commanders match their strategies against one another.
      - Other Commander abilities include opening and closing doors on the map at
    key moments, and purchasing bot-based warps and repels for their lead terr and
    levis.  BotWarps allow the lead terr to set a warp location with the bot and
    then warp there, completely independent of their normal warping capabilities.
    BotRepels are simply prized to levis.



      COMMANDER EVOLUTION CHART
      -------------------------

                                                    ----->  SLOWJAV (-20)
                                                  /
                                                 /------->  SPIDER  (-1)
                                                /
      WB  --KillsLevi-->  LEVI  ---UpgradedTo------------>  TERRIER (-10)
                                                \
       ^                   |                     \------->  LANC    (-1)
       |                   |                      \
        \    (+2PTS)      /                         ----->  SHARK   (-8)
          --RevertsTo----




    SHIP LAYOUT

    Warbird      - Standard.  Killing a levi turns it into a levi.
    Javelin      - A little more than half normal speed.  Has levi (L3) bombs.
                 Earns 1 point for every kill.
    Spider       - Increased recharge, always has antiwarp.
                 Every 3 levis killed by a support ship earns 1 point.
    Leviathan    - 1 repel, no warping, stealth.
                 Earns 1 point for every kill.
    Terrier      - Can transport 4 ships max.
                 Every 3 levis killed by a support ship earns 1 point.
    Weasel       - Not used.
    Lancaster    - Warbird-level recharge, always has multi.
                 Every 3 levis killed by a support ship earns 1 point.
    Shark        - 4 repels, cloaking.
                 Every 3 levis killed by a support ship earns 1 point.





    COMMANDER COMMANDS

    CMD     COST      COMMENTS
    makewb     +2   Reverts any ship except lead terr or levi back to a WB
    makejav   -20   Creates a slowjav, much like a levi but w/ stealth & bounce.
                  Every kill adds +1, also like a levi.  However, players
                  killing a slowjav don't turn into levis, rack up pts, etc.
    makespid   -1   Has anti and slightly increased recharge.  3 levi kills add +1.
    maketerr  -10   4-person transport.  3 levi kills add +1.
    makelanc   -1   Multi, & recharge faster than WB.  3 levi kills add +1.
    makeshark  -8   Has 4 reps & cloak.  3 levi kills add +1.
    swapdoor   -2   Closes a door between 2 and 8.
    buywarps   -8   Buys 5 bot-controlled warp points for use by the lead terr.
    buyrepels  -5   Buys 5 repels that levis can get from the bot.



    LEAD TERR COMMANDS

    CMD     COST      COMMENTS
    setwarp     0   Sets a warp location.
    dowarp      0   Warps to last warp location set.
    numwarps    0   Shows current number of warps.


    LEVI COMMANDS

    CMD
    numrepels   0   Shows current number of repels.
    getrepel    0   Prizes 1 repel to levi from the stock.


    ANY PLAYER

    CMD
    score       0   Shows the game's current score.



*/


package twcore.bots.multibot.commander;

import static twcore.core.EventRequester.PLAYER_DEATH;

import java.util.*;
import twcore.core.game.Player;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.bots.MultiModule;

/**
    Commander -- strange RTS + SS style game.  90% complete; needs map.
    @author  qan
*/
public class commander extends MultiModule {

    public void init() {
    }

    public void requestEvents(ModuleEventRequester events) {
        events.request(this, PLAYER_DEATH);
    }

    TimerTask startGame;
    TimerTask giveStartWarning;
    TimerTask scoreTimer;


    // Keeps ID of Commanders, Lead Terrs and Lead Levis
    int m_commander0_ID = -1;
    int m_commander1_ID = -1;

    int m_leadterr0_ID = -1;
    int m_leadterr1_ID = -1;

    int m_leadlevi0_ID = -1;
    int m_leadlevi1_ID = -1;


    // Default army start locations
    int m_start0_x = 512;
    int m_start0_y = 600;
    int m_start1_x = 512;
    int m_start1_y = 290;


    // Misc data

    boolean isRunning = false;
    boolean botWarps = true;    // allow/disallow bot-based warping w/ !setwarp and !dowarp
    boolean botRepels = true;   // allow/disallow bot-based repel-buying w/ !buyrepels and !getrepel
    String modName = "";        // for sending PMs.  Prevents having to pass name to methods

    String m_name0 = "Left Army";
    String m_name1 = "Right Army";

    int m_pts0 = 0;
    int m_pts1 = 0;
    int m_levictr0 = 0;
    int m_levictr1 = 0;
    int m_botWarps0 = 0;
    int m_botWarps1 = 0;
    int m_botRepels0 = 0;
    int m_botRepels1 = 0;

    int m_warp0_x = -1;
    int m_warp0_y = -1;
    int m_warp1_x = -1;
    int m_warp1_y = -1;

    int[] m_doors = { 0, 0, 0, 0, 0, 0, 0, 0 };


    // Modified handleEvent -- accepts mod and player commands
    public void handleEvent( Message event ) {

        String message = event.getMessage();

        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );

            if( opList.isER( name )) {
                if( modName == "") {
                    modName = name;
                }

                handleCommand( name, message );
            } else {
                handlePlayerCommand( name, message );
            }
        }
    }


    public int getInteger( String input ) {
        try {
            return Integer.parseInt( input.trim() );
        } catch( Exception e ) {
            return 1;
        }
    }


    // Resets all positions and game data except start locations

    public void resetAll() {
        m_commander0_ID = -1;
        m_commander1_ID = -1;
        m_leadterr0_ID = -1;
        m_leadterr1_ID = -1;
        m_leadlevi0_ID = -1;
        m_leadlevi1_ID = -1;
        botWarps = true;
        botRepels = true;
        modName = "";
        m_name0 = "Left Army";
        m_name1 = "Right Army";
        m_pts0 = 0;
        m_pts1 = 0;
        m_levictr0 = 0;
        m_levictr1 = 0;
        m_botWarps0 = 0;
        m_botWarps1 = 0;
        m_botRepels0 = 0;
        m_botRepels1 = 0;
        m_warp0_x = -1;
        m_warp0_y = -1;
        m_warp1_x = -1;
        m_warp1_y = -1;

        for( int i = 0; i < 8; i++ )
            m_doors[i] = 0;

        m_botAction.setDoors(0);
    }


    // Set army start locations
    public void setStart( int team, String[] parameters ) {
        int x = 0;
        int y = 0;

        try {
            if( parameters.length == 2) {
                x = Integer.parseInt(parameters[0]);
                y = Integer.parseInt(parameters[1]);

                if ( x >= 0 && x <= 1024 && y >= 0 && y <= 1024) {
                    if( team == 0) {
                        m_start0_x = x;
                        m_start0_y = y;
                    } else {
                        m_start1_x = x;
                        m_start1_y = y;
                    }

                    m_botAction.sendPrivateMessage( modName, "Start location set for team " + team + "." );

                } else {
                    m_botAction.sendPrivateMessage( modName, "Value out of range.  Must be between 0 and 1024." );
                }
            } else {
                m_botAction.sendPrivateMessage( modName, "Invalid number of parameters.  Use <x-coord> <y-coord>" );
            }
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( modName, "Invalid format.  Use numbers for <x-coord> <y-coord>" );
        }
    }


    // Check if all positions have been set
    public boolean allPositionsSet() {
        return ( isPositionSet(1) && isPositionSet(2) && isPositionSet(3) && isPositionSet(4) && isPositionSet(5) && isPositionSet(6) );
    }


    // Check if commander, lead terr and lead levi are set
    public boolean isPositionSet(int positionNum) {

        int pID = -1;

        switch ( positionNum ) {
        case 1:
            pID = m_commander0_ID;
            break;

        case 2:
            pID = m_commander1_ID;
            break;

        case 3:
            pID = m_leadterr0_ID;
            break;

        case 4:
            pID = m_leadterr1_ID;
            break;

        case 5:
            pID = m_leadlevi0_ID;
            break;

        case 6:
            pID = m_leadlevi1_ID;
            break;

        default:
            m_botAction.sendPrivateMessage( modName, "Positions range from 1 to 6.");
        }

        if( m_botAction.getPlayer(pID) != null )
            return true;
        else
            return false;
    }


    // Set a player position.  If game has already started, set their ship & loc
    // properly (or spec them if they're a commander), and if the position is
    // currently filled, return player in old position to proper ship & loc
    public void setPosition(int positionNum, String name) {

        Player p = m_botAction.getFuzzyPlayer( name );

        if( p != null) {

            switch ( positionNum ) {
            case 1:
                if( isRunning == true) {
                    m_botAction.spec( p.getPlayerID() );
                    m_botAction.spec( p.getPlayerID() );

                    if( isPositionSet( 1 ) ) {
                        m_botAction.setShip( m_commander0_ID, 1 );
                    }
                }

                m_commander0_ID = p.getPlayerID();
                m_botAction.sendPrivateMessage( name, "You are a Commander -- the single most important position in the game.  Send !commhelp to the bot for more info." );
                m_botAction.sendArenaMessage( name + " has been selected to be Commander of " + m_name0 + "." );
                break;

            case 2:
                if( isRunning == true) {
                    m_botAction.spec( p.getPlayerID() );
                    m_botAction.spec( p.getPlayerID() );

                    if( isPositionSet( 2 ) ) {
                        m_botAction.setShip( m_commander1_ID, 1 );
                    }
                }

                m_commander1_ID = p.getPlayerID();
                m_botAction.sendPrivateMessage( name, "You are a Commander -- the single most important position in the game.  Send !commhelp to the bot for more info." );
                m_botAction.sendArenaMessage( name + " has been selected to be Commander of " + m_name1 + "." );
                break;

            case 3:
                if( isRunning == true) {
                    m_botAction.setShip( p.getPlayerID(), 5 );
                    m_botAction.warpTo( p.getPlayerID(), m_start0_x, m_start0_y );

                    if( isPositionSet( 3 ) ) {
                        m_botAction.setShip( m_leadterr0_ID, 1 );
                    }
                }

                m_leadterr0_ID = p.getPlayerID();
                m_botAction.sendPrivateMessage( name, "You are a Lead Terr -- a very important position.  Send !terrhelp to the bot for more info." );
                m_botAction.sendArenaMessage( name + " has been selected to be Lead Terr of " + m_name0 + "." );
                break;

            case 4:
                if( isRunning == true) {
                    m_botAction.setShip( p.getPlayerID(), 5 );
                    m_botAction.warpTo( p.getPlayerID(), m_start1_x, m_start1_y );

                    if( isPositionSet( 4 ) ) {
                        m_botAction.setShip( m_leadterr1_ID, 1 );
                    }
                }

                m_leadterr1_ID = p.getPlayerID();
                m_botAction.sendPrivateMessage( name, "You are a Lead Terr -- a very important position!  Send !terrhelp to the bot for more info." );
                m_botAction.sendArenaMessage( name + " has been selected to be Lead Terr of " + m_name1 + "." );
                break;

            case 5:
                if( isRunning == true) {
                    m_botAction.setShip( p.getPlayerID(), 4 );
                    m_botAction.warpTo( p.getPlayerID(), m_start0_x, m_start0_y );

                    if( isPositionSet( 4 ) ) {
                        m_botAction.setShip( m_leadlevi0_ID, 1 );
                    }
                }

                m_leadlevi0_ID = p.getPlayerID();
                m_botAction.sendPrivateMessage( name, "You are a Lead Levi -- a very important position!  Send !levihelp to the bot for more info." );
                m_botAction.sendArenaMessage( name + " has been selected to be Lead Levi of " + m_name0 + ".");
                break;

            case 6:
                if( isRunning == true) {
                    m_botAction.setShip( p.getPlayerID(), 4 );
                    m_botAction.warpTo( p.getPlayerID(), m_start1_x, m_start1_y );

                    if( isPositionSet( 4 ) ) {
                        m_botAction.setShip( m_leadlevi1_ID, 1 );
                    }
                }

                m_leadlevi1_ID = p.getPlayerID();
                m_botAction.sendPrivateMessage( name, "You are a Lead Levi -- a very important position!  Send !levihelp to the bot for more info." );
                m_botAction.sendArenaMessage( name + " has been selected to be Lead Levi of " + m_name1 + "." );
                break;

            default:
                m_botAction.sendPrivateMessage( modName, "Positions range from 1 to 6.");

            }
        }
    }


    // Show a list of set positions to the mod
    public void showPositions() {
        String l0, l1, l2, l3, l4, l5;

        // Set it up ...
        if( isPositionSet( 1 ) )
            l0 = "Commander 0: " + m_botAction.getPlayerName( m_commander0_ID );
        else
            l0 = "Commander 0: NOT SET";

        if( isPositionSet( 2 ) )
            l1 = "Commander 1: " + m_botAction.getPlayerName( m_commander1_ID );
        else
            l1 = "Commander 1: NOT SET";

        if( isPositionSet( 3 ) )
            l2 = "Lead Terr 0: " + m_botAction.getPlayerName( m_leadterr0_ID );
        else
            l2 = "Lead Terr 0: NOT SET";

        if( isPositionSet( 4 ) )
            l3 = "Lead Terr 1: " + m_botAction.getPlayerName( m_leadterr1_ID );
        else
            l3 = "Lead Terr 1: NOT SET";

        if( isPositionSet( 5 ) )
            l4 = "Lead Levi 0: " + m_botAction.getPlayerName( m_leadlevi0_ID );
        else
            l4 = "Lead Levi 0: NOT SET";

        if( isPositionSet( 6 ) )
            l5 = "Lead Levi 1: " + m_botAction.getPlayerName( m_leadlevi1_ID );
        else
            l5 = "Lead Levi 1: NOT SET";

        // ... and spit it out.  Could be done better w/ a for loop & String[],
        // but who cares?  We'll never be using anything but 6 strings :p
        m_botAction.sendPrivateMessage( modName, l0 );
        m_botAction.sendPrivateMessage( modName, l1 );
        m_botAction.sendPrivateMessage( modName, l2 );
        m_botAction.sendPrivateMessage( modName, l3 );
        m_botAction.sendPrivateMessage( modName, l4 );
        m_botAction.sendPrivateMessage( modName, l5 );

    }


    // Displays score to the arena or an individual
    public void showScore( String name ) {
        String score = "CURRENT SCORE:  " + m_name0 + " -  " + m_pts0 + "  to  " + m_pts1 + " -  " + m_name1;

        if( name == "all" ) {
            m_botAction.sendArenaMessage( score );
        } else {
            m_botAction.sendPrivateMessage( name, score );
        }
    }


    // Modify freq's points by amt.  Return true if it's successful (pts - amt >= 0)
    public boolean modifyPts(int freq, int amt) {
        if( freq == 0 ) {
            if( m_pts0 + amt >= 0 ) {
                m_pts0 += amt;
                return true;
            } else
                return false;
        } else {
            if( m_pts1 + amt >= 0 ) {
                m_pts1 += amt;
                return true;
            } else
                return false;
        }
    }


    // Open a closed door or close an open door (and record it in m_doors)
    // Returns: doorState after flip.  0 is open, 1 is closed.
    public int flipDoor( int doorNum ) {

        if( doorNum > 0 && doorNum < 9) {

            // Transfer number to our binary-like index representation.
            // So if doornum = 3, we access array index 5 (position 3 from right)
            int indexnum = 8 - doorNum;

            if( m_doors[indexnum] > 0 ) {
                m_doors[indexnum] = 0;
                smartSetDoors();
                return 0;
            } else {
                m_doors[indexnum] = 1;
                smartSetDoors();
                return 1;
            }
        } else {
            return -1;
        }
    }


    // Set doors based on int array m_doors (treating it as a binary
    // representation of an integer, our doorState)
    public void smartSetDoors() {
        int weight = 1;
        int doorState = 0;

        for( int i = 7; i >= 0; i-- ) {
            doorState = weight * m_doors[i];
            weight *= 2;
        }

        m_botAction.setDoors( doorState );
    }


    // Returns a Player when given a command list and a place to start
    // looking for the name
    public Player getPlayerFromParam( String msg, int index ) {

        try {

            String[] params = Tools.stringChopper( msg.substring( index ), ' ' );

            String pname = params[0];
            return (Player)m_botAction.getFuzzyPlayer( pname );

        } catch (Exception e) {
            return null;
        }

    }


    // Makes certain a given playerID is not "restricted" (commander, leadterr or leadlevi)
    public boolean isNotRestricted( int pID ) {

        return ( pID != m_commander0_ID && pID != m_commander1_ID &&
                 pID != m_leadterr0_ID  && pID != m_leadterr1_ID &&
                 pID != m_leadlevi0_ID  && pID != m_leadlevi1_ID );
    }


    // Perform a ship upgrade from a normal levi into shipNum
    public void doShipUpgrade( String commName, int freq, int shipNum, String shipName,
                               int cost, String message, int charsTillName ) {

        Player target = getPlayerFromParam( message, charsTillName );

        if( target != null ) {
            if( target.getFrequency() == freq ) {
                if( target.getShipType() == 4 ) {
                    if( isNotRestricted( target.getPlayerID() )) {

                        // Check if we have the points and modify appropriately
                        if ( modifyPts( freq, cost )) {
                            m_botAction.setShip( target.getPlayerID(), shipNum );
                            m_botAction.sendPrivateMessage( commName, target.getPlayerName() + " reassigned to " + shipName + ".  [" + cost + " pt(s)]" );
                            m_botAction.sendPrivateMessage( target.getPlayerName(), "ATTN: You have reassigned to " + shipName + "!" );
                        } else
                            m_botAction.sendPrivateMessage( commName, "Not enough points to perform upgrade.  (Costs " + -cost + " pts)" );
                    } else
                        m_botAction.sendPrivateMessage( commName, target.getPlayerName() + " can't become a " + shipName + " -- already Lead Levi (or other set position)." );
                } else
                    m_botAction.sendPrivateMessage( commName, target.getPlayerName() + " must be a Levi to upgrade to a " + shipName + "." );
            } else
                m_botAction.sendPrivateMessage( commName, target.getPlayerName() + " isn't under your command!" );
        } else
            m_botAction.sendPrivateMessage( commName, "Player not found.  Please try again." );

    }


    // Warp ships, give 10 second wait, then open the doors & go
    public void start() {

        m_botAction.setDoors(1);  // Lock everyone in until go-time
        m_doors[7] = flipDoor( 7 ); // Keep track of doorstates

        m_botAction.changeAllShips( 1 ); // WBs for everyone!

        m_botAction.setShip( m_leadterr0_ID, 5 ); // Oh, except these guys
        m_botAction.setShip( m_leadterr1_ID, 5 );
        m_botAction.setShip( m_leadlevi0_ID, 4 );
        m_botAction.setShip( m_leadlevi1_ID, 4 );

        m_botAction.warpFreqToLocation( 0, m_start0_x, m_start0_y );
        m_botAction.warpFreqToLocation( 1, m_start1_x, m_start1_y );

        // In case Commanders aren't yet spec'd, make them.
        m_botAction.spec( m_commander0_ID );
        m_botAction.spec( m_commander0_ID );
        m_botAction.spec( m_commander1_ID );
        m_botAction.spec( m_commander1_ID );


        // Wait 5 seconds before giving 10 sec warning so players can read rules
        giveStartWarning = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage( "10 seconds until all hell breaks loose...", 2);
            }
        };
        m_botAction.scheduleTask( giveStartWarning, 5000 );


        startGame = new TimerTask() {
            public void run() {
                if( isRunning == false ) {
                    isRunning = true;

                    m_botAction.setDoors(0);  // open the doors

                    for( int i = 0; i < 8; i++ )  // Reset just in case.
                        m_doors[i] = 0;

                    m_botAction.scoreResetAll();
                    m_botAction.shipResetAll();

                    m_botAction.sendArenaMessage( "THE WAR HAS BEGUN!!  CHAAAAARRRRGE!!!", 104 );
                }

            }
        };
        m_botAction.scheduleTask( startGame, 15000 );


        // Display score every 60 seconds
        scoreTimer = new TimerTask() {
            public void run() {
                if( isRunning ) showScore( "all" );
            }
        };
        m_botAction.scheduleTaskAtFixedRate( scoreTimer, 60000, 60000 );

    }





    // *** PLAYER COMMAND HANDLER ***

    public void handlePlayerCommand( String name, String message ) {

        Player p = m_botAction.getPlayer( name );
        Player target;

        try {

            // ** General command set **
            if( message.startsWith( "!score" )) {
                showScore( name );

            } else if( message.startsWith( "!mainhelp" )) {
                m_botAction.privateMessageSpam( name, getHelp() );

            } else if( message.startsWith( "!commhelp" )) {
                m_botAction.privateMessageSpam( name, getCommHelp() );

            } else if( message.startsWith( "!terrhelp" )) {
                m_botAction.privateMessageSpam( name, getLeadTerrHelp() );

            } else if( message.startsWith( "!levihelp" )) {
                m_botAction.privateMessageSpam( name, getLeviHelp() );

                // EDIT
            } else if ( true ) {

                // ** Commander's command set **
                if( p.getPlayerID() == m_commander0_ID || p.getPlayerID() == m_commander1_ID ) {

                    int commFreq;

                    if( p.getPlayerID() == m_commander0_ID )
                        commFreq = 0;
                    else
                        commFreq = 1;

                    // Displays points, door & army status, and score to Commander
                    if( message.startsWith( "!stats")) {

                        String[] doorStats = new String[8];
                        String doorsParsed, shipsParsed1, shipsParsed2;
                        Iterator<Player> fi;

                        for( int i = 0; i < 8; i++ ) {
                            if( m_doors[i] == 0 )
                                doorStats[i] = " ";
                            else
                                doorStats[i] = "x";
                        }

                        doorsParsed = "DOORS (x=Closed):  " +
                                      "1[" + doorStats[7] + "] " +
                                      "2[" + doorStats[6] + "] " +
                                      "3[" + doorStats[5] + "] " +
                                      "4[" + doorStats[4] + "] " +
                                      "5[" + doorStats[3] + "] " +
                                      "6[" + doorStats[2] + "] " +
                                      "7[" + doorStats[1] + "] " +
                                      "8[" + doorStats[0] + "] ";

                        fi = m_botAction.getPlayingPlayerIterator();

                        int[] shipCnt = new int[9];

                        for(int i = 1; i < 9; i++)
                            shipCnt[i] = 0;

                        if( fi != null ) {
                            while( fi.hasNext() ) {
                                target = fi.next();

                                if( target.getFrequency() == commFreq )
                                    shipCnt[ target.getShipType() ]++;
                            }
                        }

                        shipsParsed1 = "YOUR ARMY: " +
                                       "  WBs: " + shipCnt[1] +
                                       "  Javs: " + shipCnt[2] +
                                       "  Spids: " + shipCnt[3] +
                                       "  Levis: " + shipCnt[4];

                        shipsParsed2 =
                            "             Terrs: " + shipCnt[5] +
                            "  Lancs: " + shipCnt[7] +
                            "  Sharks: " + shipCnt[8];

                        double ltRatio = -1.0;

                        if( shipCnt[4] != 0 && shipCnt[5] != 0 ) {
                            ltRatio = shipCnt[4] / shipCnt[5];
                        }

                        if( commFreq == 0) {
                            m_botAction.sendPrivateMessage( name, "Status for " + m_name0 + " -  POINTS AVAILABLE: " + m_pts0);
                            m_botAction.sendPrivateMessage( name, "BotWarps: " + m_botWarps0 + "    BotRepels: " + m_botRepels0 );
                        } else {
                            m_botAction.sendPrivateMessage( name, "Status for " + m_name1 + " -  POINTS AVAILABLE: " + m_pts1);
                            m_botAction.sendPrivateMessage( name, "BotWarps: " + m_botWarps1 + "    BotRepels: " + m_botRepels1 );
                        }

                        m_botAction.sendPrivateMessage( name, doorsParsed );
                        m_botAction.sendPrivateMessage( name, shipsParsed1 );
                        m_botAction.sendPrivateMessage( name, shipsParsed2 );

                        if( ltRatio != -1.0 ) {
                            m_botAction.sendPrivateMessage( name, "Approximate number of Levis per Terr:  " + ltRatio );

                            if( ltRatio > 3.5 )
                                m_botAction.sendPrivateMessage( name, "ADVISORY:  You should create more Terrs to properly support your army!" );
                        }

                        showScore( name );


                        // Revert any ship but commander, lead terr or lead levi to WB
                    } else if( message.startsWith( "!makewb " )) {
                        target = getPlayerFromParam( message, 8 );

                        if( target != null ) {
                            if( target.getFrequency() == commFreq ) {
                                if( target.getShipType() >= 2 && target.getShipType() <= 8 ) {
                                    if( isNotRestricted( target.getPlayerID() )) {

                                        // Add 2 pts for reverting back / sacrifice
                                        if (modifyPts( target.getFrequency(), 2 )) {
                                            m_botAction.setShip( target.getPlayerID(), 1 );
                                            m_botAction.sendPrivateMessage( name, target.getPlayerName() + " reverted to a Warbird.  (+2 pts)" );
                                        }

                                    } else
                                        m_botAction.sendPrivateMessage( name, target.getPlayerName() + " can't become a Warbird -- already Commander, Lead Terr, or Lead Levi." );
                                } else
                                    m_botAction.sendPrivateMessage( name, target.getPlayerName() + " is already a Warbird." );
                            } else
                                m_botAction.sendPrivateMessage( name, target.getPlayerName() + " isn't under your command!" );
                        } else
                            m_botAction.sendPrivateMessage( name, "Player not found.  Please try again." );

                        // Turn a levi (except lead levi) to a Slowjav
                    } else if( message.startsWith( "!makejav " )) {
                        doShipUpgrade( name, commFreq, 2, "Elite Javelin Assassin Squadron", -20, message, 9 );

                        // Turn a levi (except lead levi) to a Spider
                    } else if( message.startsWith( "!makespid " )) {
                        doShipUpgrade( name, commFreq, 3, "Spider Special Ops Division", -1, message, 10 );

                        // Turn a levi (except lead levi) to a Terrier
                    } else if( message.startsWith( "!maketerr " )) {
                        doShipUpgrade( name, commFreq, 5, "Terrier Transport Corps", -10, message, 10 );

                        // Turn a levi (except lead levi) to a Lancaster
                    } else if( message.startsWith( "!makelanc " )) {
                        doShipUpgrade( name, commFreq, 7, "Lancaster Guerilla Forces", -1, message, 10 );

                        // Turn a levi (except lead levi) to a Terrier
                    } else if( message.startsWith( "!makeshark " )) {
                        doShipUpgrade( name, commFreq, 8, "Covert Shark Mining Legion", -8, message, 11 );

                        // Add 5 BotWarps to army stockpile
                    } else if( message.startsWith( "!buywarps" )) {
                        if ( botWarps == true ) {
                            if ( modifyPts( commFreq, -8 )) {
                                if( p.getFrequency() == 0)
                                    m_botWarps0 += 5;
                                else
                                    m_botWarps1 += 5;

                                m_botAction.sendPrivateMessage( name, "5 BotWarps purchased for 8 pts." );
                            } else
                                m_botAction.sendPrivateMessage( name, "Not enough points to purchase warps (requires 8)." );
                        } else {
                            m_botAction.sendPrivateMessage( name, "BotWarps are currently DISABLED." );
                        }

                        // Add 5 BotRepels to army stockpile
                    } else if( message.startsWith( "!buyrepels" )) {
                        if ( modifyPts( commFreq, -5 )) {
                            if( commFreq == 0)
                                m_botRepels0 += 5;
                            else
                                m_botRepels1 += 5;

                            m_botAction.sendPrivateMessage( name, "5 BotRepels purchased for 5 pts." );
                        } else
                            m_botAction.sendPrivateMessage( name, "Not enough points to purchase repels (requires 8)." );

                        // Opens a closed door, or closes an open door
                    } else if( message.startsWith( "!swapdoor ")) {
                        try {
                            String[] parameters = Tools.stringChopper( message.substring( 10 ), ' ' );
                            int doornum = Integer.parseInt(parameters[0]);

                            if( doornum > 1 && doornum < 9 ) {
                                if ( modifyPts( commFreq, -2 )) {

                                    int doorReturn = flipDoor( doornum );

                                    if( doorReturn == 0 )
                                        m_botAction.sendPrivateMessage( name, "Door #" + doornum + " is now open.");
                                    else
                                        m_botAction.sendPrivateMessage( name, "Door #" + doornum + " is now closed.");

                                } else
                                    m_botAction.sendPrivateMessage( name, "Not enough points to change door state (requires 2)." );

                            } else
                                m_botAction.sendPrivateMessage( name, "Door # must be between 2 and 8." );

                        } catch (Exception e) {
                            m_botAction.sendPrivateMessage( name, "Invalid parameter type.  Usage: !swapdoor <door#>" );
                        }
                    }


                    // ** Lead Terr's command set **
                } else if( p.getPlayerID() == m_leadterr0_ID || p.getPlayerID() == m_leadterr1_ID ) {

                    // Display current amount of BotWarps
                    if( message.startsWith( "!numwarps" )) {
                        if( botWarps == true ) {
                            if( p.getFrequency() == 0 )
                                m_botAction.sendPrivateMessage( name, "BotWarps currently remaining: " + m_botWarps0 );
                            else
                                m_botAction.sendPrivateMessage( name, "BotWarps currently remaining: " + m_botWarps1 );
                        } else
                            m_botAction.sendPrivateMessage( name, "BotWarps are currently DISABLED." );

                        // Place a BotWarp point at current location
                    } else if( message.startsWith( "!setwarp" )) {
                        if( botWarps == true ) {
                            if( p.getFrequency() == 0 ) {
                                m_warp0_x = p.getXLocation();
                                m_warp0_y = p.getYLocation();
                            } else {
                                m_warp1_x = p.getXLocation();
                                m_warp1_y = p.getYLocation();
                            }

                            m_botAction.sendPrivateMessage( name, "BotWarp set at present location.  Use !dowarp to warp there." );

                        } else
                            m_botAction.sendPrivateMessage( name, "BotWarps are currently DISABLED." );

                        // Warp to last BotWarp point, if there are any in the stockpile
                    } else if( message.startsWith( "!dowarp" )) {
                        if( botWarps == true ) {
                            if( p.getFrequency() == 0 ) {
                                if( m_botWarps0 > 0 ) {

                                    if( m_warp0_x > 0 && m_warp0_x < 1024 && m_warp0_y > 0 && m_warp0_y < 1024 ) {
                                        m_botAction.warpTo( p.getPlayerID(), m_warp0_x, m_warp0_y );
                                        m_botWarps0--;
                                    } else
                                        m_botAction.sendPrivateMessage( name, "Warp not set.  Use !setwarp to position it at current location." );
                                } else
                                    m_botAction.sendPrivateMessage( name, "Out of warps.  Ask your Commander to buy more." );

                            } else {
                                if( m_botWarps1 > 0 ) {

                                    if( m_warp1_x > 0 && m_warp1_x < 1024 && m_warp1_y > 0 && m_warp1_y < 1024 ) {
                                        m_botAction.warpTo( p.getPlayerID(), m_warp1_x, m_warp1_y );
                                        m_botWarps1--;
                                    } else
                                        m_botAction.sendPrivateMessage( name, "Warp not set.  Use !setwarp to place it." );
                                } else
                                    m_botAction.sendPrivateMessage( name, "Out of warps.  Ask your Commander to buy more." );

                            }
                        } else
                            m_botAction.sendPrivateMessage( name, "BotWarps are currently DISABLED." );
                    }


                    // ** Levi command set **
                } else if( p.getShipType() == 4 ) {

                    // Display current amount of BotRepels
                    if( message.startsWith( "!numrepels" )) {
                        if( botRepels == true ) {
                            if( p.getFrequency() == 0 )
                                m_botAction.sendPrivateMessage( name, "BotRepels currently remaining: " + m_botRepels0 );
                            else
                                m_botAction.sendPrivateMessage( name, "BotRepels currently remaining: " + m_botRepels1 );
                        } else
                            m_botAction.sendPrivateMessage( name, "BotRepels are currently DISABLED." );

                        // Prize levi with repel, if there are any in the stockpile
                    } else if( message.startsWith( "!getrepel" )) {
                        if( botRepels == true ) {
                            if( p.getFrequency() == 0 ) {
                                if( m_botRepels0 > 0 ) {
                                    m_botRepels0--;
                                    m_botAction.specificPrize( name, 21 ); // Prize repel
                                    m_botAction.sendPrivateMessage( name, "Repel granted from army stockpile.  Remaining:  " + m_botRepels0 );
                                } else
                                    m_botAction.sendPrivateMessage( name, "Out of Repels.  Ask your Commander to buy more." );

                            } else {
                                if( m_botRepels1 > 0 ) {
                                    m_botRepels1--;
                                    m_botAction.specificPrize( name, 21 ); // Prize repel
                                    m_botAction.sendPrivateMessage( name, "Repel granted from army stockpile.  Remaining:  " + m_botRepels1 );
                                } else
                                    m_botAction.sendPrivateMessage( name, "Out of Repels.  Ask your Commander to buy more." );
                            }
                        } else
                            m_botAction.sendPrivateMessage( name, "BotRepels are currently DISABLED." );
                    }

                }
            }

        } catch (Exception e) {
            m_botAction.sendPrivateMessage( modName, "Exception thrown by " + name + "'s command: " + e.getMessage() );
            m_botAction.sendPrivateMessage( name, "Error found in command.  Please try again, or ask a mod if you have trouble." );
        }
    }





    // *** MOD COMMAND HANDLER ***

    public void handleCommand( String name, String message ) {

        if( message.startsWith( "!setpos " )) {
            String[] parameters = Tools.stringChopper( message.substring( 8 ), ' ' );

            try {
                if( parameters.length == 2) {
                    int position = Integer.parseInt(parameters[0]);
                    String pname = parameters[1];
                    setPosition(position, pname);

                } else {
                    m_botAction.sendPrivateMessage( modName, "Invalid number of parameters.  Usage: !setpos pos# playername" );
                }
            } catch (Exception e) {
                m_botAction.sendPrivateMessage( modName, "Invalid parameters.  Usage: !setpos pos# playername" );
            }

        } else if ( message.startsWith( "!showpos" )) {
            showPositions();

        } else if( message.startsWith( "!stop" )) {
            if( isRunning == true ) {
                m_botAction.sendPrivateMessage( modName, "Commander stopped." );
                resetAll();
                isRunning = false;
            } else {
                m_botAction.sendPrivateMessage( modName, "Commander is not currently running." );
            }

        } else if( message.startsWith( "!start" )) {
            if( allPositionsSet() == true) {
                if( isRunning == false ) {
                    start();
                    m_botAction.sendPrivateMessage( modName, "Commander started." );
                } else {
                    m_botAction.sendPrivateMessage( modName, "Commander is already running." );
                }
            } else {
                m_botAction.sendPrivateMessage( modName, "One or more positions not set.  Use !showpos to display positions, and !setpos <#> <name> to set." );
            }

        } else if( message.startsWith( "!setname0 " )) {
            try {
                m_name0 = message.substring( 10 );
                m_botAction.sendPrivateMessage( modName, "Freq 0 Army Name set to: " + m_name0 );
            } catch (Exception e) {
            }

        } else if( message.startsWith( "!setname1 " )) {
            try {
                m_name1 = message.substring( 10 );
                m_botAction.sendPrivateMessage( modName, "Freq 1 Army Name set to: " + m_name1 );
            } catch (Exception e) {
            }

        } else if( message.startsWith( "!rules" )) {
            m_botAction.sendArenaMessage( "RULES OF COMMANDER: Two armies fight to earn the most points before time is up.  WBs can not earn points, but can become point-earning Levis if they kill a Lev." );
            m_botAction.sendArenaMessage( "One person from each army is the Commander, and will control and upgrade the army.  Listen to your Commander's orders!  Send !help to the bot for more detailed info." );
            m_botAction.sendArenaMessage( "Basic Commands:  !score  !mainhelp  !commhelp  !terrhelp  !levihelp  !stats (for Commanders)" );

        } else if( message.startsWith( "!setmod")) {
            m_botAction.sendPrivateMessage( modName, name + " is now the event mod for Commander." );
            m_botAction.sendPrivateMessage( name, "You have taken control of Commander from " + modName + ".");
            modName = name;

        } else if( message.startsWith( "!botwarps" )) {
            if( botWarps == true ) {
                m_botAction.sendPrivateMessage( modName, "Bot-based warping has been DISABLED." );
                botWarps = false;
            } else {
                m_botAction.sendPrivateMessage( modName, "Bot-based warping has been ENABLED." );
                botWarps = true;
            }

        } else if( message.startsWith( "!botrepels" )) {
            if( botRepels == true ) {
                m_botAction.sendPrivateMessage( modName, "Bot-based repel-buying has been DISABLED." );
                botRepels = false;
            } else {
                m_botAction.sendPrivateMessage( modName, "Bot-based repel-buying has been ENABLED." );
                botRepels = true;
            }

        } else if( message.startsWith( "!setstart0 ")) {
            String[] parameters = Tools.stringChopper( message.substring( 11 ), ' ' );
            setStart(0, parameters);

        } else if( message.startsWith( "!setstart1 ")) {
            String[] parameters = Tools.stringChopper( message.substring( 11 ), ' ' );
            setStart(1, parameters);


        } else {
            // Pass it on the player commands if we don't find anything
            handlePlayerCommand( name, message );
        }

    }


    // Check various death/point conditions
    // CHECK IF PLAYERS ARE ON TEAM OR NOT!!
    public void handleEvent( PlayerDeath event ) {
        if( isRunning ) {
            Player killer = m_botAction.getPlayer( event.getKillerID() );
            Player killed = m_botAction.getPlayer( event.getKilleeID() );

            // Add 1 pt for every kill made by a levi or jav
            if( killer.getShipType() == 4 || killer.getShipType() == 2 ) {
                if( killer.getFrequency() == 0 )
                    m_pts0++;
                else
                    m_pts1++;

                // Change WBs to levis if they kill a levi
            } else if ( killer.getShipType() == 1 ) {
                if( killed.getShipType() == 4 )
                    m_botAction.setShip( killer.getPlayerID(), 1 );

                // Award 1 pt for every 3 kills by an upgraded support ship
            } else if ( (killer.getShipType() == 3 || killer.getShipType() == 5 ||
                         killer.getShipType() == 7 || killer.getShipType() == 8 )
                        && killed.getShipType() == 4 ) {
                if( killer.getFrequency() == 0) {
                    m_levictr0++;

                    if( m_levictr0 >= 3 ) {
                        m_pts0++;
                        m_levictr0 -= 3;
                    }
                } else {
                    m_levictr1++;

                    if( m_levictr1 >= 3 ) {
                        m_pts1++;
                        m_levictr1 -= 3;
                    }
                }
            }
        }
    }


    public String[] getHelp() {
        String[] help = {
            "  COMMANDER is a mix of real time strategy and Trench Wars action.",
            "  - The COMMANDER leads the army from spec and uses the bot to perform",
            "    ship upgrades, open and close doors, and buy supplies. (see !commhelp)",
            "  - The LEAD TERR is the army's main transport, and has access to BotWarps,",
            "    which allow the terr to warp without setting a portal. (see !terrhelp)",
            "  - LEVIS earn 1 point for every kill they make.  If a WB shoots a Levi,",
            "    the WB becomes a Levi.  (see !levihelp)  Once you are a Levi, a Commander",
            "    can reassign you to any one of 5 specialized strike forces:",
            "       SLOWJAV:   Levi-level bombs, stealth, +1pt per kill",
            "       SPIDER:    AntiWarp, faster recharge",
            "       TERRIER:   Holds up to 4 troops (no BotWarps)",
            "       LANCASTER: Warbird-level recharge",
            "       SHARK:     Cloak, 4 repels",
            "  - Spiders, Terrs, Lancs and Sharks earn +1pt for every 3 levis killed.",
            "  - !score will tell you the current score, !mainhelp to show this screen again."
        };
        return help;
    }

    public String[] getCommHelp() {
        String[] help = {
            "COMMAND    COST   DESC",
            "!commhelp    -    Bring up this reference.",
            "!stats       -    Shows score, BotWarp & BotRepel counts, doors & ship counts.",
            "!makewb     +2    Turn any ship (except leads) back to a WB.  Gives +2pts.",
            "!makejav    20    Turn a Levi into a Slowjav.  Each kill adds +1pt.  Stealth.",
            "!makespid    1    Turn a Levi into a Spider.  Has anti & fast recharge.",
            "!maketerr   10    Turn a Levi into a Terrier.  Each terr can carry 4 troops.",
            "!makelanc    1    Turn a Levi into a Lanc.  Has fast recharge & multi.",
            "!makeshark   8    Turn a Levi into a Shark.  Has 4 repels and cloak.",
            "!swapdoor #  2    Closes an open door, or opens a closed door (# can be 2-8)",
            "!buywarps    8    Buy 5 BotWarps for your Lead Terr to use (if enabled).",
            "!buyrepels   5    Buy 5 BotRepels for any Levi to use."
        };
        return help;
    }

    public String[] getLeadTerrHelp() {
        String[] help = {
            "COMMAND     DESC",
            "!terrhelp   Bring up this reference.",
            "!setwarp    Sets current location as your BotWarp location.",
            "!dowarp     Warps to last location set with !setwarp, if available.",
            "!numwarps   Displays number of BotWarps remaining.",
            "NOTE: For best results, macro !setwarp and !dowarp."
        };
        return help;
    }

    public String[] getLeviHelp() {
        String[] help = {
            "COMMAND     DESC",
            "!levihelp   Bring up this reference.",
            "!getrepel   Prizes you 1 repel, if available.",
            "!numrepels  Displays number of BotRepels remaining."
        };
        return help;
    }

    public String[] getModHelpMessage() {
        String[] cnrHelp = {
            "!setpos <#> <name>  - Sets game position # with name given.  Position #s:",
            "                      (1) Commander of Team 0     (2) Commander of Team 1",
            "                      (3) Lead Terr of Team 0     (4) Lead Terr of Team 1",
            "                      (5) Lead Levi of Team 0     (6) Lead Levi of Team 1",
            "!showpos            - Shows who has been set to which position.",
            "!setname0           - Set the name of freq 0.  Default: Left Army",
            "!setname1           - Set the name of freq 1.  Default: Right Army",
            "!start              - Starts Commander if all positions have been set.",
            "!stop               - Ends Commander.",
            "!rules              - Displays basic rules of Commander to the arena.",
            "!botwarps           - Disables/enables bot-based warping. Default: ON",
            "!botrepels          - Disables/enables bot-based repel-buying.  Default: ON",
            "!setmod             - Makes you the bot's mod contact (for passing control)",
            "   MANUAL SETUP (for arenas other than ?go Commander)",
            "!setstart0 <x-coord> <y-coord>  - Sets Team 0 start location.",
            "!setstart1 <x-coord> <y-coord>  - Sets Team 1 start location."
        };
        return cnrHelp;
    }

    public boolean isUnloadable() {
        return true;
    }

    public void cancel() {
    }
}