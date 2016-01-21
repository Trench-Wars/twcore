package twcore.bots.multibot.cnr;

import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.FlagClaimed;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.Tools;


/**
    MultiBot Module for use in ?go cnr.

    Cops have a limited amount of lives, robbers do not.  Robbers are jailed on death.
    A cop can close the jail by touching the flag, and a robber can open it the same
    way.  Cops win by jailing all robbers, and robbers win by jailing all cops.


    DEFAULT LAYOUT

     - Robbers on freq 0 as default ship 3 with infinite lives.
     - Cops on freq 1 as default ship 1 with default 2 lives.
       (note: changing the default freqs means you must set up spawn points in cfg
        a bit differently)


    DEFAULT POSITIONS

     - Robbers start game at 512 600
     - Cops start game at 512 290
     - Robbers respawn at 512 202 -- SET IN MAP'S CFG
     - Cops respawn near 512 500 -- SET IN MAP'S CFG

       IMPORTANT NOTE - Cops should not spawn near the flag!  This makes it too
                        easy for the swine.


    DOORS

     - Before start: set doors to 0 (all open)
     - After start: set doors to 1 (all open except prison)
     - If robbers get flag, set doors to 0 (open)
     - If cops get flag, set doors to 1 (closed)

    Created 5/27/2004 - Last modified 7/24/04.
    @version 1.8
    @author qan



*/


public class cnr extends MultiModule {

    public void init() {
    }

    public void requestEvents(ModuleEventRequester events) {
        events.request(this, EventRequester.PLAYER_DEATH);
        events.request(this, EventRequester.FLAG_CLAIMED);
    }


    // Bot stats
    final static String f_version = "1.8";         // Version of bot
    final static String f_modified = "7/24/04";    // Date of last modification


    // Final declarations
    final static int f_5seconds = 5000;      // 5000ms / 5 seconds
    final static int f_minmapcoord = 0;      // Min coord
    final static int f_maxmapcoord = 1024;   // Max coord
    final static int f_defrobfreq = 0;       // Default Robber freq
    final static int f_defcopfreq = 1;       // "       Cop    "
    final static int f_defrobship = 1;       // "       Robber ship
    final static int f_defcopship = 3;       // "       Cop    "
    final static int f_deflives = 2;         // "       Lives
    final static int f_alldoorsopen = 0;     // Door open for setdoors
    final static int f_prisondoorclosed = 1; // Jail door closed for setdoors


    // Timers
    TimerTask startGame;
    TimerTask giveStartWarning;

    // Data on frequencies, ships, number of lives, and start locations
    int m_robberfreq = f_defrobfreq;
    int m_copfreq = f_defcopfreq;
    int m_robbership = f_defrobship;
    int m_copship = f_defcopship;
    int m_lives = f_deflives;

    // Default start locations (for use in CnR arena)
    int m_robstart_x = 512;
    int m_robstart_y = 600;
    int m_copstart_x = 512;
    int m_copstart_y = 290;

    boolean isRunning = false;
    boolean manual = false;


    /** Handles event received message, and if from an ER or above,
        tries to parse it as an event mod command.  Otherwise, parses
        as a general command.
        @param event Passed event.
    */
    public void handleEvent( Message event ) {

        String message = event.getMessage();

        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );

            if( opList.isER( name ))
                handleCommand( name, message );
            else
                handleGeneralCommand( name, message );
        }
    }



    /** Initializes ships and number of cop lives
        @param robberfreq Robber ship
        @param robberfreq Cop ship
        @param robberfreq Cop lives before spec
    */
    public void setMode( int robbership, int copship, int lives ) {
        m_robberfreq = f_defrobfreq;
        m_copfreq = f_defcopfreq;
        m_robbership = robbership;
        m_copship = copship;
        m_lives = lives;
    }



    /** Set start locations for arenas other than standard CNR.
        @param name Name of mod executing command (for reporting out of range errors)
        @param team Team number.  0 robbers, 1 cops.
        @param x X location to start at.
        @param y Y location to start at.
    */
    public void setStart( String name, int team, int x, int y ) {
        if ( x >= f_minmapcoord && x <= f_maxmapcoord &&
                y >= f_minmapcoord && y <= f_maxmapcoord) {
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
        second warning and starts game.
    */
    public void doInit() {
        if( !manual ) {

        }

        startGame = new TimerTask() {
            public void run() {
                if( isRunning == false ) {
                    isRunning = true;

                    m_botAction.setDoors( f_prisondoorclosed );  // close up the prison

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

        if( manual ) {
            m_botAction.setDoors( f_prisondoorclosed );  // close up the prison
            m_botAction.changeAllShipsOnFreq( m_robberfreq, m_robbership);
            m_botAction.changeAllShipsOnFreq( m_copfreq, m_copship);
            m_botAction.warpFreqToLocation( m_robberfreq, m_robstart_x, m_robstart_y );
            m_botAction.warpFreqToLocation( m_copfreq, m_copstart_x, m_copstart_y );
            m_botAction.shipResetAll();
            m_botAction.scoreResetAll();
            m_botAction.resetFlagGame();

        } else {
            displayRules();    // show formatted rules

            // Wait 5 seconds before giving 10 seconds so players can read rules
            giveStartWarning = new TimerTask() {
                public void run() {
                    m_botAction.sendArenaMessage( "10 seconds until the crime spree begins...", 2);
                }
            };
            m_botAction.scheduleTask( giveStartWarning, f_5seconds );
            m_botAction.scheduleTask( startGame, f_5seconds * 3 );
        }

    }



    /** Intermediary method that passes information from handleCommand to doInit
        based on the parameters of the !start command.
        @param name Name of mod executing !start command (for reporting purposes)
        @param params String array containing each parameter
    */
    public void start( String name, String[] params ) {
        try {

            switch( params.length) {
            // All default
            case 0:
                setMode( f_defrobship, f_defcopship, f_deflives );
                doInit();

                m_botAction.sendPrivateMessage( name, "Cops and Robbers started." );
                break;

            // Specifying number of cop deaths before spec
            case 1:
                int lives = Integer.parseInt(params[0]);

                setMode( f_defrobship, f_defcopship, lives );
                doInit();

                m_botAction.sendPrivateMessage( name, "Cops and Robbers started." );
                break;

            // All specified
            case 3:
                int robbership = Integer.parseInt(params[0]);
                int copship = Integer.parseInt(params[1]);
                int theLives = Integer.parseInt(params[2]);

                setMode( robbership, copship, theLives );
                doInit();

                m_botAction.sendPrivateMessage( name, "Cops and Robbers started." );
                break;
            }

        } catch( Exception e ) {
            m_botAction.sendPrivateMessage( name, "Invalid argument type, or invalid number of arguments.  Please try again." );
            isRunning = false;
        }
    }



    /** Handles all event mod commands given to the bot.
        @param name Name of ER or above who sent the command.
        @param message Message sent
    */
    public void handleCommand( String name, String message ) {

        if( message.startsWith( "!stop" )) {
            if(isRunning == true) {
                m_botAction.sendPrivateMessage( name, "Cops and Robbers stopped." );
                m_botAction.setDoors( f_alldoorsopen );
                isRunning = false;
            } else {
                m_botAction.sendPrivateMessage( name, "I can't do that, Dave.  Cops and Robbers is not currently running." );
            }

        } else if( message.startsWith( "!start " )) {
            if(isRunning == false) {
                String[] parameters = Tools.stringChopper( message.substring( 7 ), ' ' );
                start( name, parameters );
            } else {
                m_botAction.sendPrivateMessage( name, "Cops and Robbers already running." );
            }

        } else if( message.startsWith( "!start" )) {
            if(isRunning == false) {
                setMode( f_defrobship, f_defcopship, f_deflives );
                doInit();
                m_botAction.sendPrivateMessage( name, "Cops and Robbers started." );
            } else {
                m_botAction.sendPrivateMessage( name, "Cops and Robbers already running." );
            }

        } else if( message.startsWith( "!rules" )) {
            displayRules();

        } else if( message.startsWith( "!setrobstart ")) {
            String[] parameters = Tools.stringChopper( message.substring( 12 ), ' ' );

            try {
                if( parameters.length == 2) {
                    int x = Integer.parseInt(parameters[0]);
                    int y = Integer.parseInt(parameters[1]);
                    setStart(name, f_defrobfreq, x, y);
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
                    setStart(name, f_defcopfreq, x, y);
                } else {
                    m_botAction.sendPrivateMessage( name, "Invalid number of parameters.  Format: !setcopstart <x-coord> <y-coord>" );
                }

            } catch (Exception e) {
                m_botAction.sendPrivateMessage( name, "Format: !setrobstart <x-coord> <y-coord>" );
            }

        } else if( message.startsWith( "!openjail")) {
            m_botAction.setDoors( f_alldoorsopen );  // open all doors
            m_botAction.sendPrivateMessage( name, "Jail opened." );

        } else if( message.startsWith( "!closejail")) {
            m_botAction.setDoors( f_prisondoorclosed );  // close jail
            m_botAction.sendPrivateMessage( name, "Jail closed." );

        } else if( message.startsWith( "!manual" )) {
            if( manual ) {
                manual = false;
                m_botAction.sendPrivateMessage( name, "Manual OFF.  !start will now do most of the work for you." );
            } else {
                manual = true;
                m_botAction.sendPrivateMessage( name, "Manual ON.  !start won't display rules, give 10 seconds, say GO, etc." );
            }
        } else
            handleGeneralCommand( name, message );    // pass the buck
    }



    /** Handles all general commands given to the bot.
        @param name Name of player who sent the command.
        @param message Message sent
    */
    public void handleGeneralCommand( String name, String message ) {
        // Prevent double !help spam (don't be TOO helpful)
        if( message.startsWith( "!bothelp" ) )
            sendHelp( name );

        else if( message.startsWith( "!botrules" ) )
            sendRules( name );

        else if( message.startsWith( "!about" ) )
            sendAbout( name );

        else if( message.equals( "!help" ) )
            m_botAction.sendPrivateMessage( name, "Send !bothelp for help on the loaded bot module." );

    }



    /** Handles player death events.  Spec cops at appropriate number of deaths.
        @param event Contains event information on player who died.
    */
    public void handleEvent( PlayerDeath event ) {
        if( isRunning ) {
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
        who touches it.
        @param event Contains event information on player who claimed the flag.
    */
    public void handleEvent( FlagClaimed event ) {
        if( isRunning ) {

            //Flag f = m_botAction.getFlag( event.getFlagID() );
            Player p = m_botAction.getPlayer( event.getPlayerID() );
            String playerName = p.getPlayerName();

            if( p.getShipType() == m_copship && p.getFrequency() == m_copfreq) {
                m_botAction.setDoors( f_prisondoorclosed ); // close only jail door
                m_botAction.sendArenaMessage("The cops have restored order to the jail and closed the gates.");

            } else if ( p.getShipType() == m_robbership && p.getFrequency() == m_robberfreq) {
                m_botAction.setDoors( f_alldoorsopen ); // open all doors
                m_botAction.sendArenaMessage(playerName + " has broken open the jail!  The robbers are free!");
                /*  final int warpx = f.getXLocation();
                    final int warpy = f.getYLocation();*/

            }
        }
    }




    // DISPLAY/INFO SECTION -- a handy layout.  To use, replace your module's
    // handleEvent for Message events with the one in here, create f_version
    // and f_lastmodified final static variables as seen above, copy/paste
    // the handleGeneralCommand method, and copy/paste/edit the following
    // methods below.  Makes a module much more user-friendly.

    /** Displays the rules of the event module in readable form.
    */
    public void displayRules() {
        String[] rules = getRules();

        for( int i = 0; i < rules.length; i++ )
            m_botAction.sendArenaMessage( rules[i] );
    }



    /** Sends rules privately to a player.
        @param name Player to send msg to.
    */
    public void sendRules( String name ) {
        m_botAction.privateMessageSpam( name, getRules() );
    }



    /** Sends about message to a player.
        @param name Player to send msg to.
    */
    public void sendAbout( String name ) {
        String about = "Cops and Robbers module, v" + f_version + ".  Created by qan.  Last modified " + f_modified;
        m_botAction.sendPrivateMessage( name, about );
    }



    /** Sends general help to a player.
        @param name Player to send msg to.
    */
    public void sendHelp( String name ) {
        String[] help = {
            "General Help for Cops and Robbers Module",
            "!botrules   - Display built-in rules via private message.",
            "!about      - Gives basic information about the bot module.",
            "!bothelp    - This message."
        };
        m_botAction.privateMessageSpam( name, help );
    }



    /** Returns a copy of the rules.
        @return A string array containing the rules for this module.
    */
    public String[] getRules() {
        String[] rules = {
            // | Max line length for rules to display correctly on 800x600.....................|
            ".....               - RULES of COPS AND ROBBERS -               .....",
            "...    Every time a Robber dies, he goes to jail.  The jail can   ...",
            "..     be opened/closed by touching the flag in base.              ..",
            ".      Robbers have unlimited lives, but Cops have a death limit.   .",
            ".                           - OBJECTIVE -                           .",
            ".         - Cops win by jailing all Robbers.                        .",
            ".         - Robbers win only by killing off all Cops (!)            ."
        };
        return rules;
    }



    /** Returns help message.
        @return A string array containing help msgs for this bot.
    */
    public String[] getModHelpMessage() {
        String[] cnrHelp = {
            "!start              - Starts Cops and Robbers with default mediocre settings. TRY OTHERS! :)",
            "!start <lives>      - Starts Cops and Robbers with specific number of cop lives.",
            "!start <robship> <copship> <lives> - Starts a user-defined CnR game.",
            "!stop               - Ends Cops and Robbers game.",
            "!rules              - Displays basic rules of Cops and Robbers to the arena.",
            "!openjail           - Manually opens jail doors (and all other doors).",
            "!closejail          - Manually closes jail doors.",
            "!manual             - Manual toggle.  If on, !start will start game instantly. (Default OFF)",
            "!bothelp            - Displays help on commands for players.",
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

    public boolean isUnloadable()   {
        return true;
    }
}
