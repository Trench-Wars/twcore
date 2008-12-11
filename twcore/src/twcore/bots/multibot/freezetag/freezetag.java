/**
 * twbotfreezetag.java
 *
 * Created: 07/05/2004
 *
 * Last Updated: 7/11/2004
 * - implemented !leave command
 * - implemented timed games
 * - cleaned things up in general
 */

package twcore.bots.multibot.freezetag;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;

/**
 * This class is sort of a variant of the zombies module.  It is to be used
 * when hosting a game of ?go freezetag.
 *
 * @author  Jason
 */

public class freezetag extends MultiModule {

    /*
     * Here are some constants.
     */

     //private static final int SPEC_FREQ   = 9999; //spec freq #
     private static int WARBIRD     = 1;    //wb ship #
     private static int JAVELIN     = 2;    //jav ship #
     private static int SPIDER      = 3;    //spid ship #
     private static int LEVIATHAN   = 4;    //lev ship #
     private static int TEAM1_FREQ  = 0;    //wb starting freq
     private static int TEAM2_FREQ  = 1;    //jav starting freq
     private static int TEAM1_WARPX = 294;
     private static int TEAM1_WARPY = 356;
     private static int TEAM2_WARPX = 742;
     private static int TEAM2_WARPY = 664;
     
     private static int SECS_IN_MIN = 60;   //num seconds in minute
     private static int MS_IN_SEC   = 1000; //num milliseconds in second
     private static int MIN_TIME_LIMIT = 5; //min time limit in minutes

    /*
     * Here are the class-wide variables.
     */

    HashSet<String> freq0WarbirdSet;     //set to keep track of freq 0 wbs
    HashSet<String> freq0LevSet;         //set to keep track of freq 0 levs
    HashSet<String> freq1JavSet;         //set to keep track of freq 1 javs
    HashSet<String> freq1SpidSet;        //set to keep track of freq 1 spids
    int timeLimit;               //defaults to 0 if game is not timed
    boolean isRunning;           //whether or not the game is currently running
    TimerTask m_runOutTheClock;

    /**
     * Initialize the module.
     */

    public void init() {
            freq0WarbirdSet  = new HashSet<String>();
            freq0LevSet      = new HashSet<String>();
            freq1JavSet      = new HashSet<String>();
            freq1SpidSet     = new HashSet<String>();
            timeLimit        = 0; //default if game is not timed
        	isRunning        = false;
    }

    /**
     * This method checks to see if the module can be unloaded or not.
     *
     * @return true is returned if the module is allowed to be unloaded.
     */
    public boolean isUnloadable()
    {
      return !isRunning;
    }

    /**
     * This method requests the events used by this module.
     */
    public void requestEvents(ModuleEventRequester eventRequester)
    {
      eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
      eventRequester.request(this, EventRequester.PLAYER_DEATH);
      eventRequester.request(this, EventRequester.PLAYER_LEFT);
    }

    /**
     * This method checks to see what permissions a player has before allowing
     * them to execute certain commands.
     *
     * @param event  the Message being sent to the bot
     */

    public void handleEvent( Message event ) {
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( opList.isER( name ) ) {
                handleCommand( name, message );
            } else {
                handlePlayerCommand( name, message );
            }
        }
    }

    /**
     * This method checks to see what command was sent, and then acts
     * accordingly.
     *
     * @param name     the name of the player who issued the command
     * @param message  the command the player issued
     */

    public void handleCommand( String name, String message ) {
        if( message.toLowerCase().equals( "!rules" ) ) {
            doRules();
        } else if( message.toLowerCase().equals( "!start" ) ) {
        	timeLimit = 0;
            startGame( name );
        } else if( message.toLowerCase().startsWith( "!start " ) ) {
            m_botAction.sendPrivateMessage( name, message.substring( 7 ) );
            if( message.substring( 7 ).length() > 2 ) {
                m_botAction.sendPrivateMessage( name, "Invalid time limit "
                           + "specified.  Please enter a whole number and/or "
                           + "ensure that you're not trying to start an "
                           + "insanely long game! :P" );
            } else {
                timeLimit = Integer.parseInt( message.substring( 7 ) );
                if( timeLimit > MIN_TIME_LIMIT ) {
                    startGame( name );
                } else {
                    m_botAction.sendPrivateMessage( name, "You must specify a "
                    + "time limit of at least " + MIN_TIME_LIMIT + " minutes." );
                }
            }
        } else if( message.toLowerCase().equals( "!stop" ) ) {
            stopGame( name );
        } else if( message.toLowerCase().equals( "!leave" ) ) {
            doLeave( name );
        }
    }

    /**
     * This method is used to handle the !leave command for non-staff players.
     * If a player is frozen and needs to leave mid-game, this'll spec 'em so
     * they can go do their thing elsewhere.
     *
     * @param name     the name of the player issuing the command
     * @param message  the command being issued
     */

     public void handlePlayerCommand( String name, String message ) {
        if( message.toLowerCase().equals( "!leave" ) ) {
            doLeave( name );
        }
     }

    /**
     * This method is called when a PlayerDeath event fires.  Every time a
     * player dies, we check to see if they're frozen/unfrozen, then we do
     * the opposite.
     *
     * @param event  the PlayerDeath event that has fired
     */

    public void handleEvent( PlayerDeath event ) {
        if (isRunning) {
            Player p = m_botAction.getPlayer(event.getKilleeID());
            int freq = p.getFrequency();
            int ship = p.getShipType();

            if (ship == WARBIRD) {
                m_botAction.setShip(event.getKilleeID(),SPIDER);
                m_botAction.setFreq(event.getKilleeID(),TEAM2_FREQ);
                m_botAction.sendUnfilteredPrivateMessage(p.getPlayerName(),"*prize #-13");
            } else if (ship == JAVELIN) {
                m_botAction.setShip(event.getKilleeID(),LEVIATHAN);
                m_botAction.setFreq(event.getKilleeID(),TEAM1_FREQ);
                m_botAction.sendUnfilteredPrivateMessage(p.getPlayerName(),"*prize #-13");
            } else if (ship == SPIDER || ship == LEVIATHAN) {
                if (freq == TEAM1_FREQ) {
                    m_botAction.setShip(event.getKilleeID(),JAVELIN);
                    m_botAction.setFreq(event.getKilleeID(),TEAM2_FREQ);
                } else if (freq == TEAM2_FREQ) {
                    m_botAction.setShip(event.getKilleeID(),WARBIRD);
                    m_botAction.setFreq(event.getKilleeID(),TEAM1_FREQ);
                }
            }
        }
    }

    /**
     * This method is called when a FrequencyShipChange event fires.  Every time
     * a player switches freq/ship, we need to update the HashSets so we can
     * can then check for a winner.  (This will also update the HashSets in the
     * event that a player lags out.)
     *
     * @param event  the FrequencyShipChange event that fired
     */

    public void handleEvent( FrequencyShipChange event ) {
        if( isRunning ) {
            Player p = m_botAction.getPlayer( event.getPlayerID() );
            String name = p.getPlayerName().toLowerCase();
            int freq = p.getFrequency();
            int ship = p.getShipType();
            if (ship == WARBIRD) {
                freq1SpidSet.remove( name );
                freq0WarbirdSet.add( name );
            } else if (ship == JAVELIN) {
                freq0LevSet.remove( name );
                freq1JavSet.add( name );
            } else if (ship == SPIDER || ship == LEVIATHAN) {
                if (freq == TEAM1_FREQ) {
                    freq1JavSet.remove( name );
                    freq0LevSet.add( name );
                } else if (freq == TEAM2_FREQ) {
                    freq0WarbirdSet.remove( name );
                    freq1SpidSet.add( name );
                }
            }

            /*
             * Here we're checking to see one of the sets has emptied out, in
             * which case a winner must be determined, regardless of whether or
             * not the game is timed and/or time is up yet.
             */

            if( freq0WarbirdSet.isEmpty() || freq1JavSet.isEmpty() ) {
                determineWinner();
            }
        }
    }

    /**
     * This method is called when a PlayerLeft event is fired.  If a player
     * flat out leaves the arena (via ?go or esc+q) without speccing first,
     * the HashSets need to be updated.
     *
     * @param event  the PlayerLeft event that fired
     */

    public void handleEvent( PlayerLeft event ) {
        Player p = m_botAction.getPlayer( event.getPlayerID() );
        String name = p.getPlayerName().toLowerCase();
        freq0WarbirdSet.remove( name );
        freq0LevSet.remove( name );
        freq1JavSet.remove( name );
        freq1SpidSet.remove( name );
    }

    /**
     * This method displays the rules of freeze tag in a superfluously perty
     * set of arena messages.
     */

    public void doRules() {
        m_botAction.sendArenaMessage( "Here come the rules... "
                                      + "get ready for a bit of spam!" );
        m_botAction.sendArenaMessage( "Use ESC to display all of the rules "
                                      +"at once.", 2 );

        /*
         * Here's a TimerTask to delay the display of the rules just
         * a wee bit.
         */

        TimerTask displayRules = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage(
"---------------------------- FREEZE TAG RULES ----------------------------" );
            m_botAction.sendArenaMessage(
"|                                                                        |" );
            m_botAction.sendArenaMessage(
"| 1.) There will be two teams, a team of warbirds versus a team of javs. |" );
            m_botAction.sendArenaMessage(
"|                                                                        |" );
            m_botAction.sendArenaMessage(
"| 2.) If you are shot (tagged) by an enemy you will become frozen, which |" );
            m_botAction.sendArenaMessage(
"|     will render you unable to move or fire.                            |" );
            m_botAction.sendArenaMessage(
"|                                                                        |" );
            m_botAction.sendArenaMessage(
"| 3.) To become unfrozen a teammate must shoot (tag) you.                |" );
            m_botAction.sendArenaMessage(
"|                                                                        |" );
            m_botAction.sendArenaMessage(
"| 4.) If no time limit is specified, the first team to entirely freeze   |" );
            m_botAction.sendArenaMessage(
"|     the other team wins.  If a time limit is specified, the team with  |" );
            m_botAction.sendArenaMessage(
"|     the least number of frozen players when time is up wins.           |" );
            m_botAction.sendArenaMessage(
"|                                                                        |" );
            m_botAction.sendArenaMessage(
"--------------------------------------------------------------------------" );
            m_botAction.sendArenaMessage(
"|                                                                        |" );
            m_botAction.sendArenaMessage (
"| NOTE: If you're frozen and you need to leave, you can PM the bot with  |" );
            m_botAction.sendArenaMessage(
"| with !leave to get back in spec.                                       |" );
            m_botAction.sendArenaMessage(
"|                                                                        |" );
            m_botAction.sendArenaMessage(
"--------------------------------------------------------------------------" );
            }
        };
        m_botAction.scheduleTask( displayRules, 5000 );
    }

    /**
     * This method starts a game of freeze tag.
     *
     * @param name  the name of the player who issued the !start command
     */

    public void startGame( String name ) {
        if( !isRunning ) {
            m_botAction.toggleLocked();
            m_botAction.sendPrivateMessage( name, "Freeze Tag mode started." );
            if (m_botAction.getArenaName().equalsIgnoreCase("tortugaft") || m_botAction.getArenaName().equalsIgnoreCase("#tortugaft")) {
                // special settings for playing freezetag in ?go tortugaft
                SPIDER = 4;
            }
            
            m_botAction.changeAllShipsOnFreq(TEAM1_FREQ, WARBIRD);
            m_botAction.changeAllShipsOnFreq(TEAM2_FREQ, JAVELIN);
            freq0WarbirdSet.clear();
            freq0LevSet.clear();
            freq1JavSet.clear();
            freq1SpidSet.clear();
            fillFreqSets();
            if( timeLimit == 0 ) {
                m_botAction.sendArenaMessage( "This game of freeze tag has no "
                                            + "time limit." );
                m_botAction.sendArenaMessage( "The freezing will begin in "
                                            + "about 10 seconds!", 1 );
            } else {
                m_botAction.sendArenaMessage( "This game of freeze tag has a "
                                            + "time limit of " + timeLimit
                                            + " minutes." );
                m_botAction.sendArenaMessage( "The freezing will begin in "
                                            + "about 10 seconds!", 1 );
            }
            doPreGame();
        } else {
            m_botAction.sendPrivateMessage( name, "You already started "
            + "Freeze Tag mode, ya moron!" );
        }
    }

    /**
     * This method stops a game of freeze tag.  If no game is currently in
     * progress, it will yell at the dipshit who tried to stop a non-existent
     * game.
     *
     * @param name  the name of the player who issued the !stop command
     */

    public void stopGame( String name ) {
        if( isRunning ) {
                isRunning = false;
                cancel();
                m_botAction.sendPrivateMessage( name, "Freeze Tag mode "
                + "stopped" );
                m_botAction.specAll();
                m_botAction.sendArenaMessage( "This game of freeze tag has "
                + "been cancelled by " + name + ".", 13 );
                m_botAction.sendArenaMessage( "If you are still frozen, PM "
                + "!leave to " + m_botAction.getBotName() + " to be put to spectator.");
                m_botAction.toggleLocked();
        } else {
                m_botAction.sendPrivateMessage( name, "Freeze Tag mode is not "
                + "currently running, ya moron!" );
        }
    }

    /**
     * This method fills up the freqs sets at the start of the game.
     */

    public void fillFreqSets() {
        Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
        while( it.hasNext() ) {
            Player p = (Player)it.next();
            if( p.getFrequency() == TEAM1_FREQ) {
                freq0WarbirdSet.add( p.getPlayerName().toLowerCase() );
            } else if( p.getFrequency() == TEAM2_FREQ) {
                freq1JavSet.add( p.getPlayerName().toLowerCase() );
            }
        }
    }

    /**
     * This method handles all of the pre-game stuff in a TimerTask.  The
     * TimerTask is used simply to delay the start of the game by 10 seconds.
     */

    public void doPreGame() {
        TimerTask preGameStuff = new TimerTask() {
            public void run() {
                m_botAction.warpFreqToLocation(TEAM1_FREQ, TEAM1_WARPX, TEAM1_WARPY);
                m_botAction.warpFreqToLocation(TEAM2_FREQ, TEAM2_WARPX, TEAM2_WARPY);
                m_botAction.sendArenaMessage( "GO GO GO !!!", 104 );
                m_botAction.scoreResetAll();
                m_botAction.shipResetAll();
                if( timeLimit > 0 ) {
                    m_botAction.setTimer( timeLimit );
                    m_runOutTheClock = new TimerTask() {
                        public void run() {
                            determineWinner();
                        }
                    };
                    m_botAction.scheduleTask( m_runOutTheClock,
                                timeLimit * SECS_IN_MIN * MS_IN_SEC );
                }
                isRunning = true;
            }
        };
        m_botAction.scheduleTask( preGameStuff, 10000 );
    }

    /**
     * This method does the actual determining of a winner based on the sizes
     * of the HashSets.  Should one of the HashSets be empty, I do realize that
     * it's a bit redundant to be comparing their sizes here, but it's not that
     * big of a deal so shush. :P
     */

    public void determineWinner() {
    	m_botAction.cancelTask(m_runOutTheClock);
        if( freq1JavSet.size() < freq0WarbirdSet.size() ) {
            isRunning = false;
            m_botAction.sendArenaMessage(
            "The warbirds have tagged their way to victory!", 5 );
            m_botAction.changeAllShipsOnFreq(TEAM1_FREQ,WARBIRD);
            if( !freq1SpidSet.isEmpty() ) {
                m_botAction.changeAllShipsOnFreq(TEAM2_FREQ,WARBIRD);
                m_botAction.setFreqtoFreq(TEAM2_FREQ,TEAM1_FREQ);
            }
            m_botAction.toggleLocked();
        } else if( freq0WarbirdSet.size() < freq1JavSet.size() ) {
            isRunning = false;
            m_botAction.sendArenaMessage(
            "The javelins have tagged their way to victory!", 5 );
            m_botAction.changeAllShipsOnFreq(TEAM2_FREQ,JAVELIN);
            if( !freq0LevSet.isEmpty() ) {
                m_botAction.changeAllShipsOnFreq(TEAM1_FREQ,JAVELIN);
                m_botAction.setFreqtoFreq(TEAM1_FREQ,TEAM2_FREQ);
            }
            m_botAction.toggleLocked();
        }
        m_botAction.setTimer(99999);
    }

    /**
     * This method performs the actual *spec commands when a player issues the
     * !leave command to the bot.
     *
     * @param name  the name of the player who issued the !leave command
     */

    public void doLeave( String name ) {
        m_botAction.spec( name );
        m_botAction.spec( name );
        m_botAction.sendSmartPrivateMessage( name, "You are free to go!" );
     }

    /**
     * This method displays the list of commands when the !help command is
     * issued for this module.
     *
     * @return FreezeTagHelp  the list of commands used by the module
     */

    public String[] getModHelpMessage() {
        String[] freezeTagHelp = {
            "!rules                        -- Displays the rules of freeze tag via arena "
                                            +"messages.",
            "!start                        -- Starts a game of freeze tag with no time "
                                            +"limit.",
            "!start <time limit>           -- Starts a game of freeze tag with the "
                                           + "specified time limit.  (15 minute minimum required)",
            "!stop                         -- Stops a game of freeze tag.",
            "!leave                        -- (Public Command) Allows frozen players to "
                                            +"get into spec."
        };
        return freezeTagHelp;
    }

    /**
     * This method is used to cancel any pending TimerTasks should the game be
     * !stop'd prematurely.
     */

    public void cancel() {
        m_botAction.cancelTasks();
    }
} //twbotfreezetag.java
