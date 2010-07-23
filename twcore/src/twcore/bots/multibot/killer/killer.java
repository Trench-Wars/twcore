/*
 * twbotkiller - Killer module - qan (gdugwyler@hotmail.com)
 *
 * Created 5/29/04 - Last modified 8/5/04
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



package twcore.bots.multibot.killer;

import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.Tools;



/** TWBot Extension for use in any arena, particularly deathmatch.  One person
 * is the killer, and all of their shots spec without message.  If a person
 * guesses who they are, they become the killer, but guess wrong and they are
 * spec'd.  When there are two people left, it becomes a showdown, with the
 * next hit spec'ing.  Other than that, a normal elim.  Last person standing wins.
 *
 * @author  qan
 * @version 1.9
 */
public class killer extends MultiModule {

    public void init() {
    }

	public void requestEvents(ModuleEventRequester events)	{
		events.request(this, EventRequester.PLAYER_DEATH);
		events.request(this, EventRequester.PLAYER_LEFT);
		events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
	}

    // Bot stats
    final static String f_version = "1.9";         // Version of bot
    final static String f_modified = "7/24/04";    // Date of last modification

    // Final declarations
    final static int f_5seconds = 5000;      // 5000ms / 5 seconds
    final static int f_specship = 0;         // the ship number representing spec
    final static int f_deflives = 10;        // the default number of deaths allowed
    final static int f_teamsize = 1;         // Teams of 1 (standard DM style)
    final static int f_guesscmdlength = 7;   // length of !guess command till args


    TimerTask startGame;
    TimerTask giveStartWarning;

    String m_killer = "";
    int m_lives;

    boolean isRunning = false;
    boolean suddenDeath = false;
    boolean manual = false;        // Manual toggle for more personal hosting style



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



    /** Initializes number of lives & starting killer, gives rules, 10
     * second warning, and starts game.
     * @param lives Number of lives to spec players at.
     * @param killerName Name of player to be the starting Killer.  If
     *                   left blank, random starting killer.
     */
    public void doInit( final int lives, String killerName ){
        m_lives = lives;

        if ( killerName == "" ) {
            makeNewRandomKiller();
        } else {
            Player p = m_botAction.getFuzzyPlayer( killerName );
            if( p != null) {
                m_killer = p.getPlayerName();
                m_botAction.sendPrivateMessage( m_killer, "You have been selected to be the Killer!  All your shots force others into spec.  Be careful not to reveal your identity.",1);
            }
        }


        startGame = new TimerTask() {
            public void run() {
                if( isRunning == false ) {
                    isRunning = true;
                    m_botAction.scoreResetAll();
                    m_botAction.shipResetAll();

                    m_botAction.sendArenaMessage( "Removing players with " + lives + " deaths." );
                    m_botAction.sendArenaMessage( "Blueout enabled for secrecy.  Staff, please refrain from speaking in public chat." );
                    m_botAction.sendArenaMessage( "THE HUNT IS ON!  Beware the Killer...", 104);

                    m_botAction.sendUnfilteredPublicMessage( "*lockpublic" );
                }
            }
        };

        if( manual ) {
            if( isRunning == false ) {
                isRunning = true;
                m_botAction.scoreResetAll();
                m_botAction.shipResetAll();
                m_botAction.sendArenaMessage( "Removing players with " + lives + " deaths." );
                m_botAction.sendUnfilteredPublicMessage( "*lockpublic" );
            }
        } else {
            m_botAction.createRandomTeams( f_teamsize );

            displayRules();

            // Wait 5 seconds before giving 10 sec warning so players can read rules
            giveStartWarning = new TimerTask() {
                public void run() {
                    m_botAction.sendArenaMessage( "10 seconds until the slaying begins...", 2);
                }
            };
            m_botAction.scheduleTask( giveStartWarning, f_5seconds );
            m_botAction.scheduleTask( startGame, f_5seconds * 3 );
        }
    }



    /** Checks if killer is still playing in the arena.
     * @return true if player associated with current killer's ID is
     *         in the arena, false if not.
     */
    public boolean killerStillPlaying() {
        Player p = m_botAction.getPlayer( m_killer );

        if( p != null && p.getShipType() != f_specship)
            return true;
        else
            return false;
    }



    /** Assign a new killer (not actually the slightest bit random)
     * FIX: Feed all eligible players into a list and use a random number generator to choose one.
     */
    public void makeNewRandomKiller() {
        m_killer = "";

        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

        while (m_killer == "" && i.hasNext() ) {
            if (i != null) {
                m_killer = ((Player) i.next()).getPlayerName();

                m_botAction.sendPrivateMessage( m_killer, "You have been selected to be the Killer!  All your shots force others into spec.  Be careful not to reveal your identity by keeping your kills secretive.",1);
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

            // Default # lives, random starting killer
            case 0:
                m_killer = "";
                doInit( f_deflives, "" );
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

        if( message.startsWith( "!stop" )){
            if(isRunning == true) {
                doStop();
                m_botAction.sendPrivateMessage( name, "Killer mode stopped." );
            } else {
                m_botAction.sendPrivateMessage( name, "Killer mode is not currently enabled." );
            }

        } else if( message.startsWith( "!start " )){
            if(isRunning == false) {
                String[] parameters = Tools.stringChopper( message.substring( 7 ), ' ' );
                m_killer = "";
                start( name, parameters );
            } else {
                m_botAction.sendPrivateMessage( name, "Killer mode has already been started." );
            }

        } else if( message.startsWith( "!start" )){
            if(isRunning == false) {
                m_killer = "";
                doInit( f_deflives, "" );
                m_botAction.sendPrivateMessage( name, "Killer mode started." );
            } else {
                m_botAction.sendPrivateMessage( name, "Killer mode has already been started." );
            }

        } else if( message.startsWith( "!rules" )) {
            displayRules();

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
     * @param name Name of player who sent the command.
     * @param message Message sent
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

        else if( message.startsWith( "!whoami" )) {
            if(isRunning == true) {

                if( m_killer.equals( name ) ) {
                    m_botAction.sendPrivateMessage( name, "You are the Killer.  Be careful not to give yourself away." );
                } else {
                    m_botAction.sendPrivateMessage( name, "You are not the Killer.  Run, run for your life!" );
                }

            } else {
                m_botAction.sendPrivateMessage( name, "Killer is not currently running." );
            }

        } else if( message.startsWith( "!guess " )){

            if(isRunning == true) {

                if( killerStillPlaying()) {

                    if( m_killer.equals( name ) ) {
                        m_botAction.sendPrivateMessage( name, "You ARE the Killer...  you don't need to make any guesses as to who you are." );

                    } else {

                        String[] parameters = Tools.stringChopper( message.substring( f_guesscmdlength ), ' ' );

                        Player guessK = m_botAction.getFuzzyPlayer( parameters[0] );
                        Player p = m_botAction.getPlayer( name );

                        if ( p.getShipType() != f_specship && guessK != null
                                                           && suddenDeath == false ) {
                            if( m_killer.equals( guessK.getPlayerName() ) ) {
                                m_botAction.sendPrivateMessage( name, "CORRECT!  You are now the new killer!  Try to keep your identity secret from others and make your kills as stealthy as possible.", 103 );
                                m_botAction.sendArenaMessage( "Beware, a new killer stalks the night...!");
                                m_killer = p.getPlayerName();
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
    }



    /** Handles player leaving events.  If player who left is killer, reassign killer.
     * @param event Contains event information on player who left.
     */
    public void handleEvent( PlayerLeft event ) {
        if( m_killer.equals( m_botAction.getPlayerName(event.getPlayerID()) ) ) {
            makeNewRandomKiller();
        }
    }



    /** Handles player changing ship/freq events.  If killer specs, get a new killer.
     * If we are down to the last 1 or 2 players, make special arrangements: sudden
     * death, or declare winner.
     * @param event Contains event information on player who changed ship or freq.
     */
    public void handleEvent( FrequencyShipChange event ) {

        if( event.getShipType() == f_specship && isRunning ) {

            if( m_killer.equals( m_botAction.getPlayerName( event.getPlayerID() ) ) ) {

                int numPs = 0;
                Iterator<Player> i3 = m_botAction.getPlayingPlayerIterator();

                while ( i3.hasNext() ) {
                    numPs++;
                    i3.next();
                }

                if( numPs > 1 )
                    makeNewRandomKiller();
                if( numPs == 1 ) {
                    announceWinner();
                }

            } else {

                int numPlayers = 0;
                Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

                while ( i.hasNext() ) {
                    numPlayers++;
                    i.next();
                }

                if( numPlayers == 2 && !suddenDeath ) {

                    Player p = m_botAction.getPlayer( m_killer );

                    try {
                       if( p != null && p.getShipType() != f_specship) {
                           suddenDeath = true;
                           m_botAction.sendArenaMessage( p.getPlayerName() + " has been uncovered as the Killer!  The final showdown begins... and the next to die will perish.",103);
                       }
                    } catch (Exception e) {
                    }

                } else if( numPlayers == 1) {
                    announceWinner();
                }
            }
        }
    }

    public void announceWinner(){
        Iterator<Player> i2 = m_botAction.getPlayingPlayerIterator();

        try {
            Player winner = (Player) i2.next();

            if( m_killer.equals( winner.getPlayerName() ) ) {
                m_botAction.sendArenaMessage( "GAME OVER!  " + winner.getPlayerName() + ", the notorious Killer, has emerged victorious, and lives to kill another day...",13);
                doStop();
            } else {
                m_botAction.sendArenaMessage( "GAME OVER!  " + winner.getPlayerName() + " has triumphed over the Killer and returned peace to this once-quiet city.",5);
                doStop();
            }
        } catch (Exception e) {
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

                if( m_killer.equals( m_botAction.getPlayerName( event.getKillerID() ) ) ) {

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



    /** Performs all necessary operations to stop bot.
     */
    public void doStop() {
        isRunning = false;
        suddenDeath = false;
        m_killer = "";
        m_botAction.sendUnfilteredPublicMessage( "*lockpublic" );

        if( ! manual ) {
            m_botAction.toggleLocked();	// Note: the bot DOES NOT LOCK to start.
                                          // This is to give host some freedom.
            m_botAction.sendArenaMessage( "Arena and chats unlocked, free to enter." );
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
        String about = "Killer module, v" + f_version + ".  Created by qan.  Last modified " + f_modified;
        m_botAction.sendPrivateMessage( name, about );
    }



    /** Sends general help to a player.
     * @param name Player to send msg to.
     */
    public void sendHelp( String name ) {
        String[] help = {
            "General Help for Killer Module",
            "!guess <name> - Guess <name> as the Killer.  If you are wrong,",
            "                you are specced.  If right, you become the Killer.",
            "!whoami       - Shows whether or not you are the Killer.",
            "!botrules     - Display built-in rules via private message.",
            "!about        - Gives basic information about the bot module.",
            "!bothelp      - This message."
        };
        m_botAction.privateMessageSpam( name, help );
    }



    /** Returns the rules of the event in readable form.
     * @return String array containing rules.
     */
    public String[] getRules() {
        String[] rules = {
          // | Max line length for rules to display correctly on 800x600.....................|
            ".....                          - RULES of KILLER -                          .....",
            "...   One person is, in secret, the Killer.  If anyone is killed by the       ...",
            "..    Killer, they are specced.  Revealing the Killer's identity = CHEATING.   ..",
            ".     To guess who the Killer is, PM !guess <name> to the bot.   If you guess   .",
            ".     wrong, you'll be specced, but if you're correct, you become the Killer!   .",
            ".     Send !whoami to the bot to check on your status.  Otherwise, normal elim. .",
            ".                                 - OBJECTIVE -                                 .",
            ".         - Be the last one standing, no matter the cost.                       .",
        };
        return rules;
    }



    /** Returns help message.
     * @return A string array containing help msgs for this bot.
     */
    public String[] getModHelpMessage() {
        String[] KillerHelp = {
            "!start              - Starts Killer mode with standard config (10 lives, random killer)",
            "!start <lives>      - Starts Killer mode with specific number of lives and random killer.",
            "!start <lives> <killername> - Specify number of lives and starting Killer.",
            "!stop               - Stops Killer mode.",
            "!rules              - Displays basic rules of Killer to the arena.",
            "!manual             - Manual toggle.  If on, !start will start game instantly. (Default OFF)",
            "!guess <name>       - PUBLIC COMMAND.  Makes a guess as to who the Killer is.",
            "!whoami             - PUBLIC COMMAND.  Tells you if you are the Killer."
        };
        return KillerHelp;
    }



    /** (blank method)
     */
    public void cancel() {
    }

    public boolean isUnloadable()	{
		return true;
	}
}
