/*
* twbotballspec.java - BallSpec Module - qan (gdugwyler@hotmail.com)
*
* Created 6/09/2004 - Last modified 7/14/04
*
*
*
* (SHOULD HAVE) two game modes:
*
*   1) Only 1 player, the Eliminator, can touch the ball.  Anyone else who
*      touches it is specced.  Last man standing wins.
*
*   ** NOT WORKING ** (MERVBot might do it, but currently TWBot can't)
*   2) Ball can be touched only when not moving.  If you grab it while it's
*      still moving, you'll be specced.  Last man standing wins.
*
*/



package twcore.bots.twbot;

import java.util.*;
import twcore.core.*;

/** A TWBot Extension for ?go elimination.  ER passes bot someone to be the
 * eliminator, who is the only person allowed to touch the ball -- everyone
 * else is spec'd when they do so.  Last remaining besides eliminator wins.
 *
 * @version 1.41
 * @author qan
 */
public class twbotballspec extends TWBotExtension {

    public twbotballspec() {
    }


    // Misc data

    TimerTask startGame;
    TimerTask giveStartWarning;

    int m_eliminator_ID = -1;    // ID of THE ELIMINATOR (dun dun dun!)
    int m_lastelim_ID = -1;      // ID of the person last eliminated
    boolean isRunning = false;



    /** Handles event received message, and if from an ER or above, 
     * tries to parse it as a command.
     * @param event Passed event.
     */
    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );

            if( m_opList.isER( name ))
                handleCommand( name, message );
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
            return (Player)m_botAction.getFuzzyPlayer( pname );

        } catch (Exception e) {
            return null;
        }

    }


   /** Initialize game by displaying rules, giving 10 second message, and
    * resetting scores.
    */
   public void doInit() {


        m_botAction.sendArenaMessage( "RULES: One person is designated to be the Eliminator, the only person who can touch the ball.  Anyone else touching it will be specced!" );
        m_botAction.sendArenaMessage( "OBJECTIVE: Last person alive (excluding Eliminator) is declared the winner, and becomes the Eliminator in the next round." );


        startGame = new TimerTask() {
            public void run() {
                if( isRunning == false ) {
                    isRunning = true;

                    m_botAction.shipResetAll();
                    m_botAction.scoreResetAll();

                    m_botAction.sendUnfilteredPublicMessage( "?set soccer:ballcount=1" );
                    m_botAction.sendArenaMessage( "GO GO GO !!", 104 );
                }
                
            }
        };
        m_botAction.scheduleTask( startGame, 15000 );


        // Wait 5 seconds before giving 10 seconds so players can read rules
        giveStartWarning = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage( "10 seconds until game begins...", 2);
            }
        };
        m_botAction.scheduleTask( giveStartWarning, 5000 );


    }


    /** Handles all commands given to the bot.
     * @param name Name of ER or above who sent the command.
     * @param message Message sent
     */
    public void handleCommand( String name, String message ){

        if( message.startsWith( "!stop" )){
            if( isRunning == true ) {
                m_botAction.sendPrivateMessage( name, "BallSpec mode stopped." );
                isRunning = false;
            } else
                m_botAction.sendPrivateMessage( name, "BallSpec mode is not currently running." );

        } else if( message.startsWith( "!start " )){
            if( isRunning == false ) {
                Player p = getPlayerFromParam( message, 7 );

                if( p != null ) {
                    m_eliminator_ID = p.getPlayerID();
                    m_botAction.sendPrivateMessage( name, "BallSpec mode started." );
                    m_botAction.sendUnfilteredPublicMessage( "?set soccer:ballcount=0" );
                    doInit();
                } else {
                    m_botAction.sendPrivateMessage( name, "Player not found.  Please try again." );
                }

            } else
                m_botAction.sendPrivateMessage( name, "BallSpec mode is already running." );

        } else if( message.startsWith( "!rules" )) {
            m_botAction.sendArenaMessage( "RULES: One person is designated to be the Eliminator, the only person who can touch the ball.  Anyone else touching it will be specced!" );
            m_botAction.sendArenaMessage( "OBJECTIVE: Last person alive (excluding Eliminator) is declared the winner, and becomes the Eliminator in the next round." );

        }

    }


    /** Handles player changing ship/freq events.  If we are down to the last 3 players,
     * we have a winner: the one that's not the eliminator, and the one that's not
     * just being specced.
     * @param event Contains event information on player who changed ship or freq.
     */
    public void handleEvent( FrequencyShipChange event ) {
       
        if( event.getShipType() == 0 && isRunning ) {

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

                    if( winner.getPlayerID() == m_eliminator_ID )
                        winner = (Player) i2.next();

                    m_botAction.sendArenaMessage( "GAME OVER!  " + winner.getPlayerName() + " has escaped the wrath of the Eliminator and won the game!",5);
                    m_botAction.sendUnfilteredPublicMessage( "?set soccer:ballcount=0" );
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

        int pID; 
        Player p;
        int numPlayers = 0;
        Iterator i;

        if( isRunning ) {
            pID = event.getPlayerID();
            p = m_botAction.getPlayer( pID );

            // Mode 1 check (w/ Eliminator)
            if( m_eliminator_ID != -1) {

                // If you have the ball and are not the eliminator, you are specced
                // (Also check if someone was just elim'd so there's no spam)
                if ( p != null && m_eliminator_ID != pID && m_lastelim_ID != pID) {
                    
                    switch( p.getPlayerID() % 9 ) {
                    case 0:
                        m_botAction.sendArenaMessage( m_botAction.getPlayerName( pID) + " has been obliterated by the Eliminator!" );
                        break;
                    case 1:
                        m_botAction.sendArenaMessage( m_botAction.getPlayerName( pID) + " has been massacred by the Eliminator!" );
                        break;
                    case 2:
                        m_botAction.sendArenaMessage( m_botAction.getPlayerName( pID) + " has been destroyed by the Eliminator!" );
                        break;
                    case 3:
                        m_botAction.sendArenaMessage( m_botAction.getPlayerName( pID) + " has been eviscerated by the Eliminator!" );
                        break;
                    case 4:
                        m_botAction.sendArenaMessage( m_botAction.getPlayerName( pID) + " has been decimated by the Eliminator!" );
                        break;
                    case 5:
                        m_botAction.sendArenaMessage( m_botAction.getPlayerName( pID) + " has been ...eliminated... by the Eliminator!" );
                        break;
                    case 6:
                        m_botAction.sendArenaMessage( m_botAction.getPlayerName( pID) + " has been violated by the Eliminator!" );
                        break;
                    case 7:
                        m_botAction.sendArenaMessage( m_botAction.getPlayerName( pID) + " has been skewered by the Eliminator!" );
                        break;
                    case 8:
                        m_botAction.sendArenaMessage( m_botAction.getPlayerName( pID) + " has been ravaged by the Eliminator!" );
                        break;
                    }

                    m_botAction.spec( pID );
                    m_botAction.spec( pID );
                    m_lastelim_ID = pID;
                }             
            }



            // Mode 2 (w/o Eliminator)
            // NOT YET POSSIBLE W/ TWCORE (BALLPOSITION IS NOT ADEQUATE)

            // Conclusion: since BallPosition fires only when the ball is moved,
            //             you have to cleverly check position regularly and
            //             record it until there is no movement.  However, you
            //             will never get two BallPosition events firing w/ the
            //             same coord (stopped) b/c it requires movement.  Thus
            //             it won't currently work.

        }
    }



    /** Returns help message.
     * @return A string array containing help msgs for this bot.
     */
    public String[] getHelpMessages() {
        String[] ballspecHelp = {
            "!start <name>       - Starts BallSpec mode w/ <name> as Eliminator.",
            "!stop               - Ends BallSpec mode.",
            "!rules              - Displays basic rules of BallSpec mode to the arena."
        };
        return ballspecHelp;
    }



    /** (blank method)
     */
    public void cancel() {
    }
}


