/*
 * twbotuntouchable - The Untouchable module - qan (gdugwyler@hotmail.com)
 *
 * Created 5/30/04 - Last modified 7/24/04
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
 * @version 1.7
 */
public class twbotuntouchable extends TWBotExtension {


    // Bot stats
    final static String f_version = "1.7";         // Version of bot
    final static String f_modified = "7/24/04";    // Date of last modification

    // Final declarations
    final static int f_5seconds = 5000;      // 5000ms / 5 seconds
    final static int f_specship = 0;         // the ship number representing spec
    final static int f_deflives = 10;        // the default number of deaths allowed
    final static int f_teamsize = 1;         // Teams of 1 (standard DM style)
    final static int f_startmsglength = 7;   // # chars in a !start msg until arg(s)


    public twbotuntouchable() {
    }

    TimerTask startGame;
    TimerTask giveStartWarning;

    String m_untouchable = "";
    int m_lives;

    boolean isRunning = false;
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
            if( m_opList.isER( name ))
                handleCommand( name, message );
            else
                handleGeneralCommand( name, message );
        }
    }



    /** Initializes number of lives & starting Untouchable, gives rules, 10
     * second warning, and starts game.
     * @param lives Number of lives to spec players at.
     * @param UTName Name of player to be the starting Untouchable.  If
     *                   left blank, random starting UT.
     */    
    public void doInit( final int lives, String UTName ){
        m_lives = lives;

        if ( UTName == "" ) {
            makeNewRandomUntouchable();
        } else {
            Player p = m_botAction.getFuzzyPlayer( UTName );
            if( p != null) {
                m_untouchable = p.getPlayerName();
                m_botAction.sendPrivateMessage( m_untouchable, "You have been selected to be the Untouchable!  You have no death limit, and anyone who kills you will be spec'd.  Be careful not to reveal your identity.");
            }
        }


        startGame = new TimerTask() {
            public void run() {
                if( isRunning == false ) {
                    isRunning = true;

                    m_botAction.scoreResetAll();
                    m_botAction.shipResetAll();

                    m_botAction.sendArenaMessage( "Removing players with " + lives + " deaths (except the Untouchable)" );
                    m_botAction.sendArenaMessage( "GO GO GO!!", 104);

                    m_botAction.sendUnfilteredPublicMessage( "*lockpublic" );
                    m_botAction.sendUnfilteredPublicMessage( "*lockprivate" );
                    m_botAction.sendArenaMessage( "Blueout enabled for secrecy.  Staff, please refrain from speaking in public chat." );
                }

            }
        };

        if( manual ) {
            isRunning = true;
            m_botAction.scoreResetAll();
            m_botAction.shipResetAll();
            m_botAction.sendArenaMessage( "Removing players with " + lives + " deaths (except the Untouchable)" );
            m_botAction.sendUnfilteredPublicMessage( "*lockpublic" );
            m_botAction.sendUnfilteredPublicMessage( "*lockprivate" );

        } else {
            m_botAction.createRandomTeams( f_teamsize );

            displayRules();

            // Wait 5 seconds before giving 10 sec warning so players can read rules
            giveStartWarning = new TimerTask() {
                public void run() {
                    m_botAction.sendArenaMessage( "10 seconds until game begins...", 2);
                }
            };
            m_botAction.scheduleTask( giveStartWarning, f_5seconds );
            m_botAction.scheduleTask( startGame, f_5seconds * 3 );
        }
    }



    /** Assign a new UT (not actually the slightest bit random)
     * FIX: Feed all eligible players into a list and use a random number generator to choose one.
     */
    public void makeNewRandomUntouchable() {
        m_untouchable = "";

        Iterator i = m_botAction.getPlayingPlayerIterator();

        while ( m_untouchable == "" && i.hasNext() ) {
            if (i != null) {
                m_untouchable = ((Player) i.next()).getPlayerName();
                m_botAction.sendPrivateMessage( m_untouchable, "You have been selected to be the Untouchable!  You can not die, and anyone who kills you will be spec'd.  Be careful not to reveal your identity.");
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
                m_untouchable = "";
                doInit( 10, "" );
                m_botAction.sendPrivateMessage( name, "Untouchable mode started." );
                break;

            // User-defined number of lives, random starting Untouchable
            case 1: 
                int lives = Integer.parseInt(params[0]);
                m_untouchable = "";
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
                m_untouchable = "";
                isRunning = false;
                if( ! manual )
                    m_botAction.sendUnfilteredPublicMessage( "*lockpublic" );
            } else {
                m_botAction.sendPrivateMessage( name, "Untouchable mode is not currently enabled." );
            }
              
        } else if( message.startsWith( "!start " )){
            if(isRunning == false) {
                String[] parameters = Tools.stringChopper( message.substring( f_startmsglength ), ' ' );
                start( name, parameters );
            } else {
                m_botAction.sendPrivateMessage( name, "Untouchable mode is already enabled." );
            }

        } else if( message.startsWith( "!start" )){
            if(isRunning == false) {
                m_untouchable = "";
                doInit( 10, "" );
                m_botAction.sendPrivateMessage( name, "Untouchable mode started." );
            } else {
                m_botAction.sendPrivateMessage( name, "Untouchable mode is already enabled." );
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
            if( m_untouchable.equals( name ) ) {
                m_botAction.sendPrivateMessage( name, "You are the Untouchable.  Be careful not to give yourself away." );
            } else {
                m_botAction.sendPrivateMessage( name, "You are not the Untouchable.  Watch who you shoot at!" );
            }
        }

   }

    
    /** Handles player leaving events.  If player who left is UT, reassign UT.
     * @param event Contains event information on player who left.
     */
    public void handleEvent( PlayerLeft event ) {
        if( m_untouchable.equals( m_botAction.getPlayerName( event.getPlayerID() ) ) ) {
            makeNewRandomUntouchable();
        }
    }



    /** Handles player changing ship/freq events.  If the UT specs, get a new one.
     * If we are down to the last 2 players, we have a winner: the one that isn't the UT.
     * @param event Contains event information on player who changed ship or freq.
     */
    public void handleEvent( FrequencyShipChange event ) {
       
        if( event.getShipType() == f_specship && isRunning ) {

            if( m_untouchable.equals( m_botAction.getPlayerName( event.getPlayerID() ) ) ) {

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
                        if( m_untouchable.equals( winner.getPlayerName() ) )
                            winner = (Player) i2.next();

                        m_botAction.sendArenaMessage( "GAME OVER!  " + winner.getPlayerName() + " stayed on the Untouchable " + m_untouchable + "'s good side and triumphed!",5);

                    } catch (Exception e) {
                    }
                }
            }
        }
    }


    
    /** Performs all necessary operations to stop bot.
     */
    public void doStop() {
        m_untouchable = "";
        isRunning = false;
        m_botAction.sendUnfilteredPublicMessage( "*lockpublic" );
        m_botAction.sendUnfilteredPublicMessage( "*lockprivate" );

        if( ! manual ) {
            m_botAction.toggleLocked();	// Note: the bot DOES NOT LOCK to start.
                                          // This is to give host some freedom.
            m_botAction.sendArenaMessage( "Arena and chats unlocked, free to enter." );
        }

    }



    /** Handles player death events.  Spec killed at appropriate number of deaths, 
     * and spec killer if they've kill the Untouchable.
     * @param event Contains event information on player who died.
     */
    public void handleEvent( PlayerDeath event ){

        if( isRunning ){

            Player p = m_botAction.getPlayer( event.getKilleeID() );

            // If someone's shot the Untouchable . . .
            if( m_untouchable.equals( p.getPlayerName() ) ) { 
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
        String about = "Untouchable module, v" + f_version + ".  Created by qan.  Last modified " + f_modified;
        m_botAction.sendPrivateMessage( name, about );
    }



    /** Sends general help to a player.
     * @param name Player to send msg to.
     */
    public void sendHelp( String name ) {
        String[] help = {
            "General Help for Untouchable Module",
            "!whoami       - Shows whether or not you are the Untouchable.",
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
            ".....                       - RULES of UNTOUCHABLE -                        .....",
            "...   One person is, in secret, the Untouchable.  If anyone kills the         ...",
            "..    Untouchable, they are specced.  Revealing the UT's identity = CHEATING.  ..",
            ".     While normal elim rules apply, the UT can't be eliminated!                .",
            ".     Note: send !whoami to the bot to check on your status.                    .",
            ".                                 - OBJECTIVE -                                 .",
            ".         - Be the last one standing, no matter the cost.                       .",
        };
        return rules;
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
            "!whoami             - PUBLIC COMMAND.  Tells you if you are the Untouchable.",
            "!manual             - Manual toggle.  If on, !start will start game instantly. (Default OFF)"
        };
        return untouchableHelp;
    }



    /** (blank method)
     */
    public void cancel() {
    }

}
