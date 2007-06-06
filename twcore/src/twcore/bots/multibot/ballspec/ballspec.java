package twcore.bots.multibot.ballspec;

import static twcore.core.EventRequester.BALL_POSITION;
import static twcore.core.EventRequester.FREQUENCY_SHIP_CHANGE;
import static twcore.core.EventRequester.MESSAGE;

import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.BallPosition;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * A MultiBot Extension for ?go elimination.  ER passes bot someone to be the
 * eliminator, who is the only person allowed to touch the ball -- everyone
 * else is spec'd when they do so.  Last remaining besides eliminator wins.
 *
 * @version 1.8
 * @author qan
 */
public class ballspec extends MultiModule {

    public void init() {
    }

    public void requestEvents(EventRequester events)
    {
        events.request(MESSAGE);
        events.request(BALL_POSITION);
        events.request(FREQUENCY_SHIP_CHANGE);
    }


    // Bot stats
    final static String f_version = "1.8";         // Version of bot
    final static String f_modified = "7/25/04";    // Date of last modification


    // Final declarations
    final static int f_5seconds = 5000;      // 5000ms / 5 seconds
    final static int f_specship = 0;         // the ship number representing spec
    final static int f_startcmdlength = 7;   // number of chars before param in a !start cmd
    final static int f_elimmsgcount = 9;     // total # elimination msgs

    // Misc data
    TimerTask startGame;
    TimerTask giveStartWarning;

    // Not using IDs as of 1.5 because they can change in the course of a game
    String m_eliminator = "";  // Name of THE ELIMINATOR (dun dun dun!)
    String m_lastelim = "";    // Name of the person last eliminated
    boolean isRunning = false;
    boolean manual = false;



    /** Handles event received message, and if from an ER or above,
     * tries to parse it as an event mod command.  Otherwise, parses
     * as a general command.
     * @param event Passed event.
     */
    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( opList.isER( name ))
                handleCommand( name, message );
            else
                handleGeneralCommand( name, message );
        }
    }



    /** Returns a Player when given a command string and a place to start
     * looking for the player's name.
     * @param msg Command string, e.g. "!start qan"
     * @param index Index at which to start searching for the player name
     */
    public Player getPlayerFromParam( String msg, int index ) {

        try {

            String[] params = Tools.stringChopper( msg.substring( index ), ' ' );

            String pname = params[0];
            return m_botAction.getFuzzyPlayer( pname );

        } catch (Exception e) {
            return null;
        }

    }


    /** Initialize game by displaying rules, giving 10 second message, and
     * resetting scores.
     */
    public void doInit() {

        startGame = new TimerTask() {
            public void run() {
                if( isRunning == false ) {
                    isRunning = true;

                    m_botAction.shipResetAll();
                    m_botAction.scoreResetAll();

                    m_botAction.sendArenaMessage( "GO GO GO !!", 104 );
                    m_botAction.sendArenaMessage( m_eliminator + " is the Eliminator.  Run for your life!", 104 );

                }

            }
        };

        if( manual ) {
            isRunning = true;
            m_botAction.shipResetAll();
            m_botAction.scoreResetAll();

        } else {
            displayRules();    // show formatted rules

            // Wait 5 seconds before giving 10 seconds so players can read rules
            giveStartWarning = new TimerTask() {
                public void run() {
                    m_botAction.sendArenaMessage( "10 seconds until game begins...", 2);
                }
            };

            m_botAction.scheduleTask( giveStartWarning, f_5seconds );
            m_botAction.scheduleTask( startGame, 3 * f_5seconds );
        }

    }



    /** Handles all commands given to the bot.
     * @param name Name of ER or above who sent the command.
     * @param message Message sent
     */
    public void handleCommand( String name, String message ){
        /*
        if( message.startsWith( "!balls " )) {
            if( !( m_botAction.getOperatorList().isModerator(name) ) ) {
                m_botAction.sendPrivateMessage( name, "You must be a moderator to use this command." );
                return;
            }

            String[] parameters = Tools.stringChopper( message.substring( 7 ), ' ' );
            try {
                int numballs = Integer.parseInt(parameters[0]);
                if(numballs > 8) numballs = 8;
                if(numballs < 0) numballs = 0;
                m_botAction.sendUnfilteredPublicMessage( "?set soccer:ballcount=" + numballs );
                m_botAction.sendPrivateMessage( name, "Ball count set to " + numballs + "." );
            } catch ( Exception e ) {
                m_botAction.sendPrivateMessage( name, "Invalid input.  Please give a number." );
            }

        } else
         */
        if( message.startsWith( "!stop" )){
            if( isRunning == true ) {
                m_botAction.sendPrivateMessage( name, "BallSpec mode stopped." );
                m_eliminator = "";
                m_lastelim = "";
                isRunning = false;
            } else
                m_botAction.sendPrivateMessage( name, "BallSpec mode is not currently running." );

        } else if( message.startsWith( "!start " )){
            if( isRunning == false ) {
                Player p = getPlayerFromParam( message, f_startcmdlength );

                if( p != null ) {
                    m_eliminator = p.getPlayerName();
                    m_botAction.sendPrivateMessage( name, "BallSpec mode started." );
                    doInit();
                } else {
                    m_botAction.sendPrivateMessage( name, "Player not found.  Please try again." );
                }

            } else
                m_botAction.sendPrivateMessage( name, "BallSpec mode is already running." );

        } else if( message.startsWith( "!rules" )) {
            displayRules();

        } else if( message.startsWith( "!manual" )) {
            if( manual ) {
                manual = false;
                m_botAction.sendPrivateMessage( name, "Manual OFF.  !start will now do most of the work for you." );
            } else {
                manual = true;
                m_botAction.sendPrivateMessage( name, "Manual OFF.  !start won't display rules, give 10 seconds, say GO, etc." );
            }

        } else
            handleGeneralCommand( name, message );    // pass the buck


    }



    /** Handles all general commands given to the bot.
     * @param name Name of player who sent the command.
     * @param message Message sent
     */
    public void handleGeneralCommand( String name, String message ) {
        // Prevent double !help spam (don't be TOO helpful)
        if( message.startsWith( "!bothelp" ) )
            sendHelp( name );

        else if( message.equals( "!whoiselim" ) ) {
            if( isRunning )
                m_botAction.sendPrivateMessage( name, "The Eliminator is " + m_eliminator + "." );
            else
                m_botAction.sendPrivateMessage( name, "Game not currently running." );

        } else if( message.startsWith( "!botrules" ) )
            sendRules( name );

        else if( message.startsWith( "!about" ) )
            sendAbout( name );

        else if( message.equals( "!help" ) )
            m_botAction.sendPrivateMessage( name, "Send !bothelp for help on the loaded bot module." );

    }



    /** Handles player changing ship/freq events.  If we are down to the last 3 players,
     * we have a winner: the one that's not the eliminator, and the one that's not
     * just being specced.
     * @param event Contains event information on player who changed ship or freq.
     */
    public void handleEvent( FrequencyShipChange event ) {

        if( event.getShipType() == f_specship && isRunning ) {

            int numPlayers = 0;
            Iterator i = m_botAction.getPlayingPlayerIterator();

            while ( i.hasNext() ) {
                numPlayers++;
                i.next();
            }

            // check for two players: the elim'r, and our winner
            if( numPlayers <= 2) {
                Iterator i2 = m_botAction.getPlayingPlayerIterator();

                try {
                    Player winner = (Player) i2.next();

                    // If the "winner" we're getting is the one who's being spec'd
                    // or the eliminator, get next

                    if( m_eliminator.equals( winner.getPlayerName() ) )
                        winner = (Player) i2.next();

                    m_botAction.sendArenaMessage( "GAME OVER!  " + winner.getPlayerName() + " has escaped the wrath of the Eliminator and won the game!",5);
                    m_eliminator = "";
                    m_lastelim = "";
                    isRunning = false;

                } catch (Exception e) {
                }
            }
        }
    }



    /** Handles ball movement, and specs those who touch ball and are not
     * the eliminator.  Also ends the game when there's just the Eliminator
     * and one other person left.
     * @param event Passed message event.
     */
    public void handleEvent( BallPosition event ){

        Player p;
        String pname;

        if( isRunning ) {
            p = m_botAction.getPlayer( event.getPlayerID() );
            pname = m_botAction.getPlayerName( event.getPlayerID() );

            // Mode 1 check (w/ Eliminator)
            if( m_eliminator != "" ) {

                // If you have the ball and are not the eliminator, you are specced
                // (Also check if someone was just elim'd so there's no spam)
                if ( p != null && !( m_eliminator.equals( pname ) )
                        && !( m_lastelim.equals( pname ) ) ) {

                    switch( p.getPlayerID() % f_elimmsgcount ) {
                    case 0:
                        m_botAction.sendArenaMessage( pname + " has been obliterated by the Eliminator!" );
                        break;
                    case 1:
                        m_botAction.sendArenaMessage( pname + " has been massacred by the Eliminator!" );
                        break;
                    case 2:
                        m_botAction.sendArenaMessage( pname + " has been destroyed by the Eliminator!" );
                        break;
                    case 3:
                        m_botAction.sendArenaMessage( pname + " has been eviscerated by the Eliminator!" );
                        break;
                    case 4:
                        m_botAction.sendArenaMessage( pname + " has been decimated by the Eliminator!" );
                        break;
                    case 5:
                        m_botAction.sendArenaMessage( pname + " has been ...eliminated... by the Eliminator!" );
                        break;
                    case 6:
                        m_botAction.sendArenaMessage( pname + " has been violated by the Eliminator!" );
                        break;
                    case 7:
                        m_botAction.sendArenaMessage( pname + " has been skewered by the Eliminator!" );
                        break;
                    case 8:
                        m_botAction.sendArenaMessage( pname + " has been ravaged by the Eliminator!" );
                        break;
                    }

                    m_botAction.spec( pname );
                    m_botAction.spec( pname  );
                    m_lastelim = pname;
                }
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
     * @param name Player to send msg to.
     */
    public void sendRules( String name ) {
        m_botAction.privateMessageSpam( name, getRules() );
    }



    /** Sends about message to a player.
     * @param name Player to send msg to.
     */
    public void sendAbout(String name) {
        String about = "Ball Elimination module, v" + f_version + ".  Created by qan.  Last modified " + f_modified;
        m_botAction.sendPrivateMessage( name, about );
    }



    /** Sends general help to a player.
     * @param name Player to send msg to.
     */
    public void sendHelp( String name ) {
        String[] help = {
                "General Help for Ball Elimination Module",
                "!whoiselim   - Tells you the name of the Eliminator.",
                "!botrules    - Display built-in rules via private message.",
                "!about       - Gives basic information about the bot module.",
                "!bothelp     - This message."
        };
        m_botAction.privateMessageSpam( name, help );
    }



    /** Returns rules.
     * @return A string array containing rules for this bot.
     */
    public String[] getRules() {
        String[] rules = {
                // | Max line length for rules to display correctly on 800x600.....................|
                ".....              - RULES of BALL ELIMINATION -              .....",
                "...  Only the Eliminator, chosen at start, can touch the ball.  ...",
                "..   Anyone else is specced instantly if they touch it!          ..",
                ".                          - OBJECTIVE -                          .",
                ".    Escape from the Eliminator!  Last person standing wins.      ."
        };
        return rules;
    }



    /** Returns help message.
     * @return A string array containing help msgs for this bot.
     */
    public String[] getModHelpMessage() {
        String[] ballspecHelp = {
                "!start <name>       - Starts BallSpec mode w/ <name> as Eliminator.",
                "!stop               - Ends BallSpec mode.",
                "!rules              - Displays basic rules of BallSpec mode to the arena.",
                "!manual             - Manual toggle.  If on, !start will start game instantly. (Default OFF)",
//              "!balls #            - (Mod+) Sets # balls in the arena.",
                "!bothelp            - Displays help on commands for players."
        };
        return ballspecHelp;
    }


    public boolean isUnloadable() {
        return true;
    }


    /** (blank method)
     */
    public void cancel() {
    }
}


