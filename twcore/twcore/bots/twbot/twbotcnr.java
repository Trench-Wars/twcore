/*
 * twbotcnr.java - Cops and Robbers module - qan (gdugwyler@hotmail.com)
 *
 * Created 5/27/2004 - Last modified 7/14/04.
 *
 */  



package twcore.bots.twbot;

import java.util.*;
import twcore.core.*;


/** TWBot Extension for use in ?go cnr.
 * Cops have a limited amount of lives, robbers do not.  Robbers are jailed on death.
 * A cop can close the jail by touching the flag, and a robber can open it the same
 * way.  Cops win by jailing all robbers, and robbers win by jailing all cops.
 *
 *
 * DEFAULT LAYOUT
 *
 *   - Robbers on freq 0 as default ship 3 with infinite lives.
 *   - Cops on freq 1 as default ship 1 with default 2 lives.
 *     (note: changing the default freqs means you must set up spawn points in cfg
 *      a bit differently)
 *
 *
 * DEFAULT POSITIONS
 *   
 *   - Robbers start game at 512 600
 *   - Cops start game at 512 290
 *   - Robbers respawn at 512 202 -- SET IN MAP'S CFG
 *   - Cops respawn near 512 500 -- SET IN MAP'S CFG
 * 
 *     IMPORTANT NOTE - Cops should not spawn near the flag!  This makes it too
 *                      easy for the swine.
 *
 *
 * DOORS
 *
 *   - Before start: set doors to 0 (all open)
 *   - After start: set doors to 1 (all open except prison)
 *   - If robbers get flag, set doors to 0 (open)
 *   - If cops get flag, set doors to 1 (closed)
 * 
 * @version 1.6
 * @author qan
 *
 */


public class twbotcnr extends TWBotExtension {

    public twbotcnr() {
    }


    TimerTask startGame;
    TimerTask giveStartWarning;

    // Data on frequencies, ships, number of lives, and start locations
    int m_robberfreq = 0;
    int m_robbership = 1;
    int m_copfreq = 1;
    int m_copship = 3;
    int m_lives = 2;

    // Default start locations (for use in CnR arena)
    int m_robstart_x = 512;
    int m_robstart_y = 600;
    int m_copstart_x = 512;
    int m_copstart_y = 290;

    boolean isRunning = false;
    boolean modeSet = false;


    /** Initializes frequencies, ships, and number of cop lives
      * @param robberfreq Robber freq
      * @param robberfreq Robber ship
      * @param robberfreq Cop freq
      * @param robberfreq Cop ship
      * @param robberfreq Cop lives before spec
      */
    public void setMode( int robberfreq, int robbership, int copfreq, int copship, int lives ){
        m_robberfreq = robberfreq;
        m_robbership = robbership;
        m_copfreq = copfreq;
        m_copship = copship;
        m_lives = lives;
        modeSet = true;
    }



    /** Set start locations for arenas other than standard CNR.
      * @param name Name of mod executing command (for reporting out of range errors)
      * @param team Team number.  0 robbers, 1 cops.
      * @param x X location to start at.
      * @param y Y location to start at.
      */
    public void setStart( String name, int team, int x, int y ) {
        if ( x >= 0 && x <= 1024 && y >= 0 && y <= 1024) {
            if (team == 0) {
                m_robstart_x = x;
                m_robstart_y = y;
            } else {
                m_copstart_x = x;
                m_copstart_y = y;
            }
        } else {
            m_botAction.sendPrivateMessage( name, "Value out of range.  Must be between 0 and 1024." );
        }
    }


    /** Initializes number of lives, doors, & correct ships, gives rules, then a 10
     * second warning and starts game.
     */
    public void doInit() {

        startGame = new TimerTask() {
            public void run() {
                if( isRunning == false ) {
                    isRunning = true;

                    m_botAction.setDoors(1);  // close up the prison

                    m_botAction.changeAllShipsOnFreq( m_robberfreq, m_robbership);
                    m_botAction.changeAllShipsOnFreq( m_copfreq, m_copship);

                    m_botAction.warpFreqToLocation( m_robberfreq, m_robstart_x, m_robstart_y );
                    m_botAction.warpFreqToLocation( m_copfreq, m_copstart_x, m_copstart_y );
            
                    m_botAction.shipResetAll();
                    m_botAction.scoreResetAll();
                    m_botAction.resetFlagGame();

                    m_botAction.sendArenaMessage( "The robbers are loose!  Cops and Robbers has begun!", 104 );
                    m_botAction.sendArenaMessage( "Removing Cops with " + m_lives + " deaths." );
                }
                
            }
        };
        m_botAction.scheduleTask( startGame, 15000 );


        // Wait 5 seconds before giving 10 seconds so players can read rules
        giveStartWarning = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage( "10 seconds until the crime spree begins...", 2);
            }
        };
        m_botAction.scheduleTask( giveStartWarning, 5000 );


        m_botAction.sendArenaMessage( "RULES OF COPS AND ROBBERS: Robbers (usually WBs) have unlimited lives, cops (usually spiders) don't.  If a cop shoots a robber, the robber goes to jail.  However, if another robber touches the flag, allowing an escape." );
        m_botAction.sendArenaMessage( "OBJECTIVE: COPS win if all the robbers are jailed.  ROBBERS win only when every last stinkin' cop is dead." );

    }



    /** Handles event received message, and if from an ER or above, 
     * tries to parse it as a command.
     * @param event Passed event.
     */
    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name )) handleCommand( name, message );
        }
    }


 
    /** Intermediary method that passes information from handleCommand to doInit
     * based on the parameters of the !start command.
     * @param name Name of mod executing !start command (for reporting purposes)
     * @param params String array containing each parameter
     */
    public void start( String name, String[] params ){
        try{
            
            switch( params.length){
            // All default
            case 0: 
                setMode(0, 1, 1, 3, 2);
                doInit();

                m_botAction.sendPrivateMessage( name, "Cops and Robbers started." );
                break;

            // Specifying number of cop deaths before spec
            case 1: 
                int lives = Integer.parseInt(params[0]);

                setMode(0, 1, 1, 3, lives);
                doInit();

                m_botAction.sendPrivateMessage( name, "Cops and Robbers started." );
                break;

            // All specified
            case 3:
                int robbership = Integer.parseInt(params[0]);
                int copship = Integer.parseInt(params[1]);
                int theLives = Integer.parseInt(params[2]);

                setMode( 0, robbership, 1, copship, theLives );
                doInit();

                m_botAction.sendPrivateMessage( name, "Cops and Robbers started." );
                break;
            } 

        }catch( Exception e ){
            m_botAction.sendPrivateMessage( name, "Invalid argument type, or invalid number of arguments.  Please try again." );
            isRunning = false;
            modeSet = false;
        }
    }



    /** Handles all commands given to the bot.
     * @param name Name of ER or above who sent the command.
     * @param message Message sent
     */
    public void handleCommand( String name, String message ){

        if( message.startsWith( "!stop" )){
            if(isRunning == true) {
              m_botAction.sendPrivateMessage( name, "Cops and Robbers stopped." );
              m_botAction.setDoors(0);
              isRunning = false;
            } else {
              m_botAction.sendPrivateMessage( name, "I can't do that, Dave.  Cops and Robbers is not currently running." );
            }
              
        } else if( message.startsWith( "!start " )){
            if(isRunning == false) {
                String[] parameters = Tools.stringChopper( message.substring( 7 ), ' ' );
                start( name, parameters );
            } else {
                m_botAction.sendPrivateMessage( name, "Cops and Robbers already running." );
            }

        } else if( message.startsWith( "!start" )){
            if(isRunning == false) {
                setMode(0, 1, 1, 3, 2);
                doInit();
                m_botAction.sendPrivateMessage( name, "Cops and Robbers started." );
            } else {
                m_botAction.sendPrivateMessage( name, "Cops and Robbers already running." );
            }

        } else if( message.startsWith( "!rules" )) {
            m_botAction.sendArenaMessage( "RULES OF COPS AND ROBBERS: Robbers (usually WBs) have unlimited lives, cops (usually spiders) don't.  If a cop shoots a robber, the robber goes to jail.  However, if another robber touches the flag, allowing an escape." );
            m_botAction.sendArenaMessage( "OBJECTIVE: COPS win if all the robbers are jailed.  ROBBERS win only when every last stinkin' cop is dead." );

        } else if( message.startsWith( "!setrobstart ")) {
            String[] parameters = Tools.stringChopper( message.substring( 12 ), ' ' );
            try {
                if( parameters.length == 2) {
                    int x = Integer.parseInt(parameters[0]);
                    int y = Integer.parseInt(parameters[1]);
                    setStart(name, 0, x, y);
                } else {
                    m_botAction.sendPrivateMessage( name, "Invalid number of parameters.  Format: !setrobstart <x-coord> <y-coord>" ); 
                }
            } catch (Exception e) {
                m_botAction.sendPrivateMessage( name, "Format: !setrobstart <x-coord> <y-coord>" ); 
            }

        } else if( message.startsWith( "!setcopstart ")) {
            String[] parameters = Tools.stringChopper( message.substring( 12 ), ' ' );
            try {

                if( parameters.length == 2) {
                    int x = Integer.parseInt(parameters[0]);
                    int y = Integer.parseInt(parameters[1]);
                    setStart(name, 1, x, y);
                } else {
                    m_botAction.sendPrivateMessage( name, "Invalid number of parameters.  Format: !setcopstart <x-coord> <y-coord>" ); 
                }

            } catch (Exception e) {
                m_botAction.sendPrivateMessage( name, "Format: !setrobstart <x-coord> <y-coord>" ); 
            }

        } else if( message.startsWith( "!openjail")) {
            m_botAction.setDoors(0);  // open all doors
            m_botAction.sendPrivateMessage( name, "Jail opened." ); 

        } else if( message.startsWith( "!closejail")) {
            m_botAction.setDoors(1);  // close jail
            m_botAction.sendPrivateMessage( name, "Jail closed." ); 
        }

    }



    /** Handles player death events.  Spec cops at appropriate number of deaths.
     * @param event Contains event information on player who died.
     */
    public void handleEvent( PlayerDeath event ){
        if( modeSet && isRunning ){
            Player p = m_botAction.getPlayer( event.getKilleeID() );

            if( p.getShipType() == m_copship && p.getFrequency() == m_copfreq) {

                if( p.getLosses() >= m_lives ) {

                    String playerName = p.getPlayerName();
                    int wins = p.getWins();
                    int losses = p.getLosses();

                    m_botAction.sendArenaMessage(playerName + " has been killed in the line of duty.  " + wins + " wins, " + losses + " losses.");
                    m_botAction.spec(playerName);
                    m_botAction.spec(playerName);
                }
            }
        }
    }



    /** Handles flag claiming events.  Opens and closes the prison door based on
     * who touches it.
     * @param event Contains event information on player who claimed the flag.
     */
    public void handleEvent( FlagClaimed event ) {
        if( modeSet && isRunning ){

            Flag f = m_botAction.getFlag( event.getFlagID() );
            Player p = m_botAction.getPlayer( event.getPlayerID() );
            String playerName = p.getPlayerName();
        
            if( p.getShipType() == m_copship && p.getFrequency() == m_copfreq) {
                m_botAction.setDoors(1); // close only jail door
                m_botAction.sendArenaMessage("The cops have restored order to the jail and closed the gates.");

            } else if ( p.getShipType() == m_robbership && p.getFrequency() == m_robberfreq) {
                m_botAction.setDoors(0); // open all doors
                m_botAction.sendArenaMessage(playerName + " has broken open the jail!  The robbers are free!");
                final int warpx = f.getXLocation();
                final int warpy = f.getYLocation();           
            
            }
        }
    }



    /** Returns help message.
     * @return A string array containing help msgs for this bot.
     */
    public String[] getHelpMessages() {
        String[] cnrHelp = {
            "!start              - Starts Cops and Robbers with default mediocre settings. TRY OTHERS! :)",
            "!start <lives>      - Starts Cops and Robbers with specific number of cop lives.",
            "!start <robship> <copship> <lives> - Starts a user-defined CnR game.",
            "!stop               - Ends Cops and Robbers game.",
            "!rules              - Displays basic rules of Cops and Robbers to the arena.",
            "!openjail           - Manually opens jail doors (and all other doors).",
            "!closejail          - Manually closes jail doors.",
            "   MANUAL SETUP (when not in ?go cnr.  Jail is door 1, set spawns in cfg)",
            "!setcopstart <x-coord> <y-coord> - Sets cop start location.",
            "!setrobstart <x-coord> <y-coord> - Sets robber start location."
        };
        return cnrHelp;
    }



    /** (blank method)
     */
    public void cancel() {
    }
}
