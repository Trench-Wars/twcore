/*
 * twbotkiller - Killer module - qan (gdugwyler@hotmail.com)
 *
 * Created 5/29/04 - Last modified 7/14/04
 * 
 *
 *
 * DESC: Normal elimination match, except:
 *
 *       - One person privately designated at the start of match as "the killer"
 *       - Anyone dying by the killer's hand is spec'd w/o arena msg.
 *       - Players still in the game can PM the bot with the killer's name.
 *           ... if they're correct, they become the new killer, and a msg
 *               is displayed saying the killer's identity has changed.               
 *           ... if they're not correct, they are spec'd.
 *       - Players MUST NOT give away identity of the Killer (as it's cheating).
 */  



package twcore.bots.twbot;

import java.util.*;
import twcore.core.*;



/** TWBot Extension for use in any arena, particularly deathmatch.  One person
 * is the killer, and all of their shots spec without message.  If a person
 * guesses who they are, they become the killer, but guess wrong and they are
 * spec'd.  When there are two people left, it becomes a showdown, with the
 * next hit spec'ing.  Other than that, a normal elim.  Last person standing wins.
 *
 * @author  qan
 * @version 1.7
 */
public class twbotkiller extends TWBotExtension {

    public twbotkiller() {
    }

    TimerTask startGame;
    TimerTask giveStartWarning;

    int m_killerID;
    int m_lives;

    boolean isRunning = false;
    boolean suddenDeath = false;



    /**This handleEvent accepts msgs from players as well as mods.
     * Mod-only commands are filtered out in handleCommand
     * @event The Message event in question.
     */
    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            handleCommand( name, message );
        }
    }



    /** Initializes number of lives & starting killer, gives rules, 10
     * second warning, and starts game.
     * @param lives Number of lives to spec players at.
     * @param killerName Name of player to be the starting Killer.  If
     *                   left blank, random starting killer.
     */    
    public void doInit( final int lives, String killerName ){
        m_lives = lives;


        m_botAction.sendArenaMessage( "RULES OF KILLER: Basic elim, except that one person is THE KILLER.  This should remain as secret as possible.  Anyone murdered by the Killer is instantly specced.",4 );
        m_botAction.sendArenaMessage( "Send !guess <name> to the bot to guess who the killer is.  If you're right, you become the new Killer -- but guess wrong and you'll be specced." );
        m_botAction.sendArenaMessage( "Use !whoami to see if you're the Killer.  NOTE: Revealing the identity of the Killer to anyone else is considered *cheating* and will be dealt with harshly!" );


        if ( killerName == "" ) {
            makeNewRandomKiller();
        } else {
            Player p = m_botAction.getFuzzyPlayer( killerName );
            if( p != null) {
                m_killerID = p.getPlayerID();
                String name = m_botAction.getPlayerName( m_killerID );
                m_botAction.sendPrivateMessage( name, "You have been selected to be the Killer!  All your shots force others into spec.  Be careful not to reveal your identity.",1);
            }
        }


        // Wait 5 seconds before giving 10 sec warning so players can read rules
        giveStartWarning = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage( "10 seconds until the slaying begins...", 2);
            }
        };
        m_botAction.scheduleTask( giveStartWarning, 5000 );


        startGame = new TimerTask() {
            public void run() {
                if( isRunning == false ) {
                    isRunning = true;

                    m_botAction.scoreResetAll();
                    m_botAction.shipResetAll();

                    m_botAction.sendArenaMessage( "Removing players with " + lives + " deaths." );
                    m_botAction.sendArenaMessage( "THE HUNT IS ON!  Beware the Killer...", 104);
                    m_botAction.sendUnfilteredPublicMessage( "*lockpublic" );
                    m_botAction.sendArenaMessage( "Blueout enabled to protect the Killer.  Staff, please refrain from speaking in public chat." );
                }

            }
        };
        m_botAction.scheduleTask( startGame, 15000 );
    }



    /** Checks if killer is still playing in the arena.
     * @return true if player associated with current killer's ID is
     *         in the arena, false if not.
     */
    public boolean killerStillPlaying() {
        Player p = m_botAction.getPlayer( m_killerID );

        if( p != null && p.getShipType() != 0)
            return true;
        else
            return false;
    }



    /** Assign a new killer (not actually the slightest bit random)
     * FIX: Feed all eligible players into a list and use a random number generator to choose one.
     */
    public void makeNewRandomKiller() {
        m_killerID = -1;

        Iterator i = m_botAction.getPlayingPlayerIterator();

        while (m_killerID == -1 && i.hasNext() ) {
            if (i != null) {
                m_killerID = ((Player) i.next()).getPlayerID();

                String name = m_botAction.getPlayerName( m_killerID );
                m_botAction.sendPrivateMessage( name, "You have been selected to be the Killer!  All your shots force others into spec.  Be careful not to reveal your identity by keeping your kills secretive.",1);
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

            // Default: 10 lives, random starting killer
            case 0: 
                doInit( 10, "" );
                m_botAction.sendPrivateMessage( name, "Killer mode started." );
                break;

            // User-defined number of lives, random starting killer
            case 1: 
                int theLives = Integer.parseInt(params[0]);
                doInit( theLives, "" );
                m_botAction.sendPrivateMessage( name, "Killer mode started." );
                break;

            // User defined number of lives and starting killer
            case 2:
                int lives = Integer.parseInt(params[0]);
                String killerName = params[1];
                Player p = m_botAction.getFuzzyPlayer( killerName );

                if (p != null) {
                    doInit( lives, killerName );
                    m_botAction.sendPrivateMessage( name, "Killer mode started." );
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
     * @param name Name of person who sent the command (not necessarily an ER+)
     * @param message Message sent
     */
    public void handleCommand( String name, String message ){

        if( message.startsWith( "!whoami" )) {
            if(isRunning == true) {

                if( m_botAction.getPlayerID( name ) == m_killerID ) {
                    m_botAction.sendPrivateMessage( name, "You are the Killer.  Be careful not to give yourself away." );
                } else {
                    m_botAction.sendPrivateMessage( name, "You are not the Killer.  Run, run for your life!" );
                }

            } else {
                m_botAction.sendPrivateMessage( name, "Killer is not currently running." );
            }

        }

        if( message.startsWith( "!guess " )){

            if(isRunning == true) {

                if( killerStillPlaying()) {

                    if( m_botAction.getPlayerID( name ) == m_killerID ) {
                        m_botAction.sendPrivateMessage( name, "You ARE the Killer...  you don't need to make any guesses as to who you are." );

                    } else {    
                
                        String[] parameters = Tools.stringChopper( message.substring( 7 ), ' ' );

                        Player guessK = m_botAction.getFuzzyPlayer( parameters[0] );
                        Player p = m_botAction.getPlayer( name );

                        if (p.getShipType() != 0 && guessK != null && suddenDeath == false ) {
                            if( m_killerID == guessK.getPlayerID() ) {
                                m_botAction.sendPrivateMessage( name, "CORRECT!  You are now the new killer!  Try to keep your identity secret from others and make your kills as stealthy as possible.", 103 );
                                m_botAction.sendArenaMessage( "Beware, a new killer stalks the night...!");
                                m_killerID = p.getPlayerID();
                            } else {
                                m_botAction.sendPrivateMessage( name, "You have been deceived by the killer!  Now you have become one of the victims...", 13 );
                                m_botAction.sendArenaMessage( name + " falsely presumed the killer's identity and ended up one of the victims.");
                                m_botAction.spec(name);
                                m_botAction.spec(name);
                            }
                        }
                    }

                } else {
                    makeNewRandomKiller();
                }

            } else {
                m_botAction.sendPrivateMessage( name, "Killer is not currently running." );
            }

	}        



        if( m_opList.isER( name )) {
            if( message.startsWith( "!stop" )){
                if(isRunning == true) {
                    m_botAction.sendPrivateMessage( name, "Killer mode stopped." );
                    isRunning = false;
                    suddenDeath = false;
                } else {
                    m_botAction.sendPrivateMessage( name, "Killer mode is not currently enabled." );
                }
              
            } else if( message.startsWith( "!start " )){
                if(isRunning == false) {
                    String[] parameters = Tools.stringChopper( message.substring( 7 ), ' ' );
                    start( name, parameters );
                } else {
                    m_botAction.sendPrivateMessage( name, "Killer mode has already been started." );
                }

            } else if( message.startsWith( "!start" )){
                if(isRunning == false) {
                    doInit( 10, "" );
                    m_botAction.sendArenaMessage( "Removing players with 10 deaths." );
                    isRunning = true;
                    m_botAction.sendPrivateMessage( name, "Killer mode started." );
                } else {
                    m_botAction.sendPrivateMessage( name, "Killer mode has already been started." );
                }

            } else if( message.startsWith( "!rules" )) {
                m_botAction.sendArenaMessage( "RULES OF KILLER: Basic elim, except that one person is THE KILLER.  This should remain as secret as possible.  Anyone murdered by the Killer is instantly specced.",4 );
                m_botAction.sendArenaMessage( "Send !guess <name> to the bot to guess who the killer is.  If you're right, you become the new Killer -- but guess wrong and you'll be specced." );
                m_botAction.sendArenaMessage( "Use !whoami to see if you're the Killer.  NOTE: Revealing the identity of the Killer to anyone else is considered *cheating* and will be dealt with harshly!" );

            } 
        }
    }



    /** Handles player leaving events.  If player who left is killer, reassign killer.
     * @param event Contains event information on player who left.
     */
    public void handleEvent( PlayerLeft event ) {
        if( event.getPlayerID() == m_killerID ) {
            makeNewRandomKiller();
        }
    }



    /** Handles player changing ship/freq events.  If killer specs, get a new killer.
     * If we are down to the last 1 or 2 players, make special arrangements: sudden
     * death, or declare winner.
     * @param event Contains event information on player who changed ship or freq.
     */
    public void handleEvent( FrequencyShipChange event ) {

        if( event.getShipType() == 0 && isRunning ) {

            if( event.getPlayerID() == m_killerID ) {

                int numPs = 0;
                Iterator i3 = m_botAction.getPlayingPlayerIterator();

                while ( i3.hasNext() ) {
                    numPs++;
                    i3.next();
                }

                if( numPs > 1 )
                    makeNewRandomKiller();

            } else {

                int numPlayers = 0;
                Iterator i = m_botAction.getPlayingPlayerIterator();

                while ( i.hasNext() ) {
                    numPlayers++;
                    i.next();
                }

                if( numPlayers == 2 ) {

                    Player p = m_botAction.getPlayer( m_killerID );

                    try {
                       if( p != null && p.getShipType() != 0) {
                           suddenDeath = true;                
                           m_botAction.sendArenaMessage( p.getPlayerName() + " has been uncovered as the Killer!  The final showdown begins... and the next to die will perish.",103);
                       }
                    } catch (Exception e) {
                    }

                } else if( numPlayers == 1) {
                    Iterator i2 = m_botAction.getPlayingPlayerIterator();

                    try {
                        Player winner = (Player) i2.next();

                        if( winner.getPlayerID() == m_killerID ) {
                            m_botAction.sendArenaMessage( "GAME OVER!  " + winner.getPlayerName() + ", the notorious Killer, has emerged victorious, and lives to kill another day...",13);
                            isRunning = false;
                            suddenDeath = false;
                        } else {
                            m_botAction.sendArenaMessage( "GAME OVER!  " + winner.getPlayerName() + " has triumphed over the Killer and returned peace to this once-quiet city.",5);
                            isRunning = false;
                            suddenDeath = false;
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
    }



    /** Handles player death events.  Spec at appropriate number of deaths, spec
     * if killed by the killer, and spec if in sudden death.
     * @param event Contains event information on player who died.
     */
    public void handleEvent( PlayerDeath event ){

        if( isRunning ){

            Player p = m_botAction.getPlayer( event.getKilleeID() );

            if( suddenDeath ) {

                String playerName = p.getPlayerName();
                m_botAction.spec(playerName);
                m_botAction.spec(playerName);
                suddenDeath = false;
            
            } else {

                int kID = event.getKillerID();

                if( kID == m_killerID ) {

                    String playerName = p.getPlayerName();
                    m_botAction.sendPrivateMessage(playerName, "You have been slain by the Killer -- you are dead.", 13);
                    m_botAction.sendPrivateMessage(playerName, "REMEMBER: It is illegal to reveal the identity of the Killer!");
                    m_botAction.spec(playerName);
                    m_botAction.spec(playerName);
                
            
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
    }
    


    /** Returns help message.
     * @return A string array containing help msgs for this bot.
     */
    public String[] getHelpMessages() {
        String[] KillerHelp = {
            "!start              - Starts Killer mode with standard config (10 lives, random killer)",
            "!start <lives>      - Starts Killer mode with specific number of lives and random killer.",
            "!start <lives> <killername> - Specify number of lives and starting Killer.",
            "!stop               - Stops Killer mode.",
            "!rules              - Displays basic rules of Killer to the arena.",
            "!guess <name>       - PUBLIC COMMAND.  Makes a guess as to who the Killer is.",
            "!whoami             - PUBLIC COMMAND.  Tells you if you are the Killer." };
        return KillerHelp;
    }



    /** (blank method)
     */
    public void cancel() {
    }
}
