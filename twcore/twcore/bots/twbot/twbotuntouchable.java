/*
 * twbotuntouchable - The Untouchable module - qan (gdugwyler@hotmail.com)
 *
 * Created 5/30/04 - Last modified 7/14/04
 * 
 *
 *
 * DESC: Normal elimination match, except:
 *
 *       - One person privately designated at the start of match as "the Untouchable"
 *       - Anyone who kills the Untouchable is spec'd!
 *       - Untouchable has unlimited lives, but of course can't be the winner of the game.
 *       - Players MUST NOT give away identity of the Untouchable (as it's cheating).
 */  



package twcore.bots.twbot;

import java.util.*;
import twcore.core.*;



/** TWBot Extension for use in any arena, particularly deathmatch.  One person
 * is the untouchable, and anyone who kills them is spec'd w/o message.
 * When there are two people left (the UT and one other), the non-UT
 * is declared the winner.  Other than that, a normal elim.
 *
 * @author  qan
 * @version 1.5
 */
public class twbotuntouchable extends TWBotExtension {

    public twbotuntouchable() {
    }

    TimerTask startGame;
    TimerTask giveStartWarning;

    int m_untouchableID;
    int m_lives;

    boolean isRunning = false;



    /**This handleEvent accepts msgs from players as well as ERs.
     * ER commands are passed to handleCommand
     * @event The Message event in question.
     */
    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );

            // Let everyone check who they are, not just mods
            if( message.startsWith( "!whoami" )) {
                if( m_botAction.getPlayerID( name ) == m_untouchableID ) {
                    m_botAction.sendPrivateMessage( name, "You are the Untouchable.  Be careful not to give yourself away." );
                } else {
                    m_botAction.sendPrivateMessage( name, "You are not the Untouchable." );
                }
            }

            if( m_opList.isER( name ))
                handleCommand( name, message );
        }
    }



    /** Initializes number of lives & starting Untouchable, gives rules, 10
     * second warning, and starts game.
     * @param lives Number of lives to spec players at.
     * @param UTName Name of player to be the starting Untouchable.  If
     *                   left blank, random starting UT.
     */    
    public void doInit( int lives, String UTName ){
        m_lives = lives;
        final int theLives = lives;

        m_botAction.sendArenaMessage( "RULES OF UNTOUCHABLE: Elim, except one person is privately selected to be THE UNTOUCHABLE.  His or her identity should remain as secret as possible." );
        m_botAction.sendArenaMessage( "The Untouchable can never die.  A person who shoots the Untouchable will be in for a painful surprise -- they're spec'd without warning." );
        m_botAction.sendArenaMessage( "WINNING: The last person left alive, not including the Untouchable, is declared the winner." );

        if ( UTName == "" ) {
            makeNewRandomUntouchable();
        } else {
            Player p = m_botAction.getFuzzyPlayer( UTName );
            if( p != null) {
                m_untouchableID = p.getPlayerID();
                String name = m_botAction.getPlayerName( m_untouchableID );
                m_botAction.sendPrivateMessage( name, "You have been selected to be the Untouchable!  You have no death limit, and anyone who kills you will be spec'd.  Be careful not to reveal your identity.");
            }
        }

        // Wait 5 seconds before giving 10 sec warning so players can read rules
        giveStartWarning = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage( "10 seconds until game begins...", 2);
            }
        };
        m_botAction.scheduleTask( giveStartWarning, 5000 );

        startGame = new TimerTask() {
            public void run() {
                if( isRunning == false ) {
                    isRunning = true;

                    m_botAction.scoreResetAll();
                    m_botAction.shipResetAll();

                    m_botAction.sendArenaMessage( "Removing players with " + theLives + " deaths (except the Untouchable)" );
                    m_botAction.sendArenaMessage( "GO GO GO!!", 104);

                    m_botAction.sendUnfilteredPublicMessage( "*lockpublic" );
                    m_botAction.sendArenaMessage( "Blueout enabled to protect the Untouchable.  Staff, please refrain from speaking in public chat." );
                }

            }
        };
        m_botAction.scheduleTask( startGame, 15000 );
    }



    /** Assign a new UT (not actually the slightest bit random)
     * FIX: Feed all eligible players into a list and use a random number generator to choose one.
     */
    public void makeNewRandomUntouchable() {
        m_untouchableID = -1;

        Iterator i = m_botAction.getPlayingPlayerIterator();

        while ( m_untouchableID == -1 && i.hasNext() ) {
            if (i != null) {
                m_untouchableID = ((Player) i.next()).getPlayerID();
                String name = m_botAction.getPlayerName( m_untouchableID );
                m_botAction.sendPrivateMessage( name, "You have been selected to be the Untouchable!  You can not die, and anyone who kills you will be spec'd.  Be careful not to reveal your identity.");
            }
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

            // Default: 10 lives, random starting Untouchable
            case 0: 
                doInit( 10, "" );
                m_botAction.sendPrivateMessage( name, "Untouchable mode started." );
                break;

            // User-defined number of lives, random starting Untouchable
            case 1: 
                int lives = Integer.parseInt(params[0]);
                doInit( lives, "" );
                m_botAction.sendPrivateMessage( name, "Untouchable mode started." );
                break;

            // User-defined number of lives and starting Untouchable
            case 2:
                int theLives = Integer.parseInt(params[0]);
                String UTName = params[1];
                Player p = m_botAction.getFuzzyPlayer( UTName );

                if (p != null) {
                    doInit( theLives, UTName );
                    m_botAction.sendPrivateMessage( name, "Untouchable mode started." );
                } else {
                    m_botAction.sendPrivateMessage( name, "Unrecognized user name.  Please check the spelling and try again." );
                }

                break;
            } 

        }catch( Exception e ){
            m_botAction.sendPrivateMessage( name, "Silly " + name + ".  You've made a mistake -- please try again." );
            isRunning = false;
        }
    }



    /** Handles all commands given to the bot.
     * @param name Name of ER or above who sent the command.
     * @param message Message sent
     */
    public void handleCommand( String name, String message ){

        if( message.startsWith( "!stop" )){
            if(isRunning == true) {
                m_botAction.sendPrivateMessage( name, "Untouchable mode stopped." );
                isRunning = false;
            } else {
                m_botAction.sendPrivateMessage( name, "Untouchable mode is not currently enabled." );
            }
              
        } else if( message.startsWith( "!start " )){
            if(isRunning == false) {
                String[] parameters = Tools.stringChopper( message.substring( 7 ), ' ' );
                start( name, parameters );
            } else {
                m_botAction.sendPrivateMessage( name, "Untouchable mode is already enabled." );
            }

        } else if( message.startsWith( "!start" )){
            if(isRunning == false) {
                doInit( 10, "" );
                m_botAction.sendPrivateMessage( name, "Untouchable mode started." );
            } else {
                m_botAction.sendPrivateMessage( name, "Untouchable mode is already enabled." );
            }

        } else if( message.startsWith( "!rules" )) {
            m_botAction.sendArenaMessage( "RULES OF UNTOUCHABLE: Elim, except one person is privately selected to be THE UNTOUCHABLE.  His or her identity should remain as secret as possible." );
            m_botAction.sendArenaMessage( "The Untouchable can never die.  A person who shoots the Untouchable will be in for a painful surprise -- they're spec'd without warning." );
            m_botAction.sendArenaMessage( "WINNING: The last person left alive, not including the Untouchable, is declared the winner." );
        } 
    }


    
    /** Handles player leaving events.  If player who left is UT, reassign UT.
     * @param event Contains event information on player who left.
     */
    public void handleEvent( PlayerLeft event ) {
        if( event.getPlayerID() == m_untouchableID ) {
            makeNewRandomUntouchable();
        }
    }



    /** Handles player changing ship/freq events.  If the UT specs, get a new one.
     * If we are down to the last 2 players, we have a winner: the one that isn't the UT.
     * @param event Contains event information on player who changed ship or freq.
     */
    public void handleEvent( FrequencyShipChange event ) {
       
        if( event.getShipType() == 0 && isRunning ) {

            if( event.getPlayerID() == m_untouchableID ) {

                int numPs = 0;
                Iterator i3 = m_botAction.getPlayingPlayerIterator();

                while ( i3.hasNext() ) {
                    numPs++;
                    i3.next();
                }

                if( numPs > 1 )
                    makeNewRandomUntouchable();
            
            } else {

                int numPlayers = 0;
                Iterator i = m_botAction.getPlayingPlayerIterator();

                while ( i.hasNext() ) {
                    numPlayers++;
                    i.next();
                }

                if( numPlayers <= 2) {
                    Iterator i2 = m_botAction.getPlayingPlayerIterator();

                    try {
                        Player winner = (Player) i2.next();

                        // If the "winner" we're getting is actually the UT, get next
                        if( winner.getPlayerID() == m_untouchableID )
                            winner = (Player) i2.next();

                        m_botAction.sendArenaMessage( "GAME OVER!  Winner:  " + winner.getPlayerName() + "!",5);
                        isRunning = false;

                    } catch (Exception e) {
                    }
                }
            }
        }
    }



    /** Handles player death events.  Spec killed at appropriate number of deaths, 
     * and spec killer if they've kill the Untouchable.
     * @param event Contains event information on player who died.
     */
    public void handleEvent( PlayerDeath event ){

        if( isRunning ){

            int pID = event.getKilleeID();
            Player p = m_botAction.getPlayer( pID );

            // If someone's shot the Untouchable . . .
            if( pID == m_untouchableID ) { 
                Player killer = m_botAction.getPlayer( event.getKillerID() );
                String killerName = killer.getPlayerName();

                m_botAction.sendPrivateMessage(killerName, "You've made the permanent mistake of trying to kill the Untouchable ... now you're sleeping with the fishes!", 13);
                m_botAction.sendPrivateMessage(killerName, "REMEMBER: Revealing the identity of the Untouchable is strictly forbidden!");
                m_botAction.spec(killerName);
                m_botAction.spec(killerName);

            // Spec non-UTs over certain amount of lives
            } else if( p.getLosses() >= m_lives ) {

                String playerName = p.getPlayerName();
                int wins = p.getWins();
                int losses = p.getLosses();

                m_botAction.sendArenaMessage(playerName + " is out.  " + wins + " wins, " + losses + " losses.");
                m_botAction.spec(playerName);
                m_botAction.spec(playerName);
            
            }
        }
    }
    


    /** Returns help message.
     * @return A string array containing help msgs for this bot.
     */
    public String[] getHelpMessages() {
        String[] untouchableHelp = {
            "!start              - Starts Untouchable mode w/ defaults (10 lives, random UT)",
            "!start <lives>      - Starts Untouchable mode w/ specific # of lives and random UT.",
            "!start <lives> <UTname> - Specify number of lives and starting Untouchable.",
            "!stop               - Stops Untouchable mode.",
            "!rules              - Displays basic rules of the Untouchable to the arena.",
            "!whoami             - PUBLIC COMMAND.  Tells you if you are the Untouchable."
        };
        return untouchableHelp;
    }



    /** (blank method)
     */
    public void cancel() {
    }

}
