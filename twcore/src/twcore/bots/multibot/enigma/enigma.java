
package twcore.bots.multibot.enigma;

import java.util.Random;
import java.util.TimerTask;
//import java.util.TreeMap;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.OperatorList;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;

/**
 * Enigma.
 * 
 *  08.01.02 
 * Completely recoded. - Austin Barton (2d)
*/
public class enigma extends MultiModule {
    //Standard variables
    OperatorList        opList;
    //TreeMap             rawData;
    CommandInterpreter  m_commandInterpreter;
    BotSettings         m_botSettings;

    //EnigmaBot specific variables
    Random 				generator = new Random();

    TimerTask			CheckTime, CheckAnimation;
    TimerTask			start1, start2, start3;
    TimerTask			stop1, stop2, stop3;

    int					m_timeON;
    int					m_timeOFF;
    int					m_delay;
    String				m_arena;

    int					checkDoors = 0, checkEvents = 0, checkMessages = 0, lastEvent;
    int					curTime = 0;
    boolean 			gameProgress = false, eventProgress = false, startProgress = false;
    boolean				allowLagout = true;



    /****************************************************************/
    /*** Constructor                                              ***/
    /****************************************************************/
    public void init() {

        m_commandInterpreter = new CommandInterpreter( m_botAction );
        m_botSettings = moduleSettings;

        registerCommands();

        //Timer setup
        setupTimerTasks();
        m_botAction.scheduleTaskAtFixedRate(CheckTime,0,1000);
        //Gets variables from .cfg
        m_timeON 	= m_botSettings.getInt("EventOffDelay");
        m_timeOFF 	= m_botSettings.getInt("EventOnDelay");
        m_delay		= m_botSettings.getInt("MessageDelay");
        m_arena 	= m_botSettings.getString("Arena");
    }
    
    /**
     * This method is called when the module is unloaded
     */
    public void cancel() {
    	m_botAction.cancelTasks();
    }

    public void requestEvents(ModuleEventRequester events)	{
        events.request( this, EventRequester.PLAYER_ENTERED );
	}

    /****************************************************************/
    /*** Registers the bot commands.                              ***/
    /****************************************************************/
    void registerCommands() {
        int acceptedMessages;

        acceptedMessages = Message.PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand( "!start", 	acceptedMessages, this, "doStartGame" );
        m_commandInterpreter.registerCommand( "!help",  	acceptedMessages, this, "doShowHelp" );
        m_commandInterpreter.registerCommand( "!cancel", 	acceptedMessages, this, "doCancelGame" );
        m_commandInterpreter.registerCommand( "!stop", 		acceptedMessages, this, "doStopGame" );
        m_commandInterpreter.registerCommand( "!reset", 	acceptedMessages, this, "doResetGame" );
        m_commandInterpreter.registerCommand( "!lagouton",  acceptedMessages, this, "doLagoutOn");
        m_commandInterpreter.registerCommand( "!lagoutoff", acceptedMessages, this, "doLagoutOff");
        m_commandInterpreter.registerCommand( "!lagout", 	acceptedMessages, this, "doLagout" );
        m_commandInterpreter.registerCommand( "!whatis", 	acceptedMessages, this, "doWhatIs" );
        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "doShowHelp" );
    }


    /****************************************************************/
    /*** Starts a game of Enigma.                                 ***/
    /****************************************************************/
    public void doStartGame( String name, String message ) {
        if( m_botAction.getOperatorList().isER( name ) && gameProgress == false ) {
            //Sets up players.
            gameProgress = true;
            startProgress = true;
            checkDoors = curTime + 100;
            checkMessages = curTime + 2;
            m_botAction.showObject(10);
            m_botAction.sendArenaMessage("Enigma is starting!", 21);
            m_botAction.toggleLocked();
            m_botAction.setAlltoFreq(0);
            m_botAction.changeAllShips(1);
            //This is confusing but easiest way to do it in my opinion without sleeping.
            start1 = new TimerTask() {
                public void run() {
                    if(gameProgress) {
                        m_botAction.sendArenaMessage("Enigma Rules/Tips", 103);
                        m_botAction.showObject(11);
                        //2
                        start2 = new TimerTask() {
                            public void run() {
                                if(gameProgress) {
                                    m_botAction.sendArenaMessage("Game Starts Shortly", 2);
                                    m_botAction.showObject(12);
                                    //3
                                    start3 = new TimerTask() {
                                        public void run() {
                                            if(gameProgress) {
                                                startProgress = false;
                                                m_botAction.sendArenaMessage("Enigma has begun! GO GO GO!", 104);
                                                m_botAction.showObject(13);
                                                checkDoors = curTime + 1;
                                                m_botAction.sendUnfilteredPublicMessage("*shipreset");

                                                checkEvents = curTime + getRandom( 10, 15 );
                                                if(checkEvents == checkDoors)
                                                    checkEvents++;
                                            }
                                        }
                                    };
                                    m_botAction.scheduleTask(start3,5500);
                                }
                            }
                        };
                        m_botAction.scheduleTask(start2,20500);
                    }

                }
            };
            m_botAction.scheduleTask(start1,4000);
            //doSleep(3500);

        }
    }

    /****************************************************************/
    /*** Cancels the game.                                        ***/
    /****************************************************************/
    public void doCancelGame(String name, String message) {
        if( m_botAction.getOperatorList().isER( name ) && gameProgress == true) {
            m_botAction.sendArenaMessage("Enigma has been cancelled.", 103);
            m_botAction.toggleLocked();
            m_botAction.setDoors(255);
            m_botAction.prizeAll(7);
            if(eventProgress) {
                eventProgress = false;
                switch(lastEvent) {
                    case 0:
                        eventZero();
                        break;
                    case 1:
                        eventOne();
                        break;
                    case 2:
                        eventTwo();
                        break;
                    case 3:
                        eventThree();
                        break;
                }
            }
            gameProgress = false;
        }
    }

    /****************************************************************/
    /*** Stops the game.                                          ***/
    /****************************************************************/
    public void doStopGame(String name, String message) {
        final String winner = message;
        if( m_botAction.getOperatorList().isER( name ) && (gameProgress == true)  && (message.length() > 1)) {
            if(!startProgress) {
                if(eventProgress) {
                    eventProgress = false;
                    switch(lastEvent) {
                        case 0:
                            eventZero();
                            break;
                        case 1:
                            eventOne();
                            break;
                        case 2:
                            eventTwo();
                            break;
                        case 3:
                            eventThree();
                            break;
                        case 4:
                            eventFour();
                            break;
                    }
                }
                gameProgress = false;
                m_botAction.sendArenaMessage("Enigma has been conquered!", 5);
                m_botAction.showObject(14);
                //1
                stop1 = new TimerTask() {
                    public void run() {
                        m_botAction.sendArenaMessage("Congratulations to the winner: " + winner, 147);
                        m_botAction.showObject(15);
                        //2
                        stop2 = new TimerTask() {
                            public void run() {
                                m_botAction.sendArenaMessage("And pity to the losers", 148);
                                m_botAction.showObject(16);
                                //3
                                stop3 = new TimerTask() {
                                    public void run() {
                                        m_botAction.sendUnfilteredPublicMessage("*lock");
                                        m_botAction.setDoors(255);
                                        m_botAction.prizeAll(7);
                                    }
                                };
                                m_botAction.scheduleTask(stop3,5000);
                            }
                        };
                        m_botAction.scheduleTask(stop2,5000);
                    }
                };
                m_botAction.scheduleTask(stop1,5000);
            }
            else
                m_botAction.sendPrivateMessage(name, "Cannot stop a game while it is starting. Please use !cancel or wait until the game has started.");

        }
    }

    /****************************************************************/
    /*** Resets arena settings in case bot lagged during a game.  ***/
    /****************************************************************/
    public void doResetGame(String name, String message) {
        if( m_botAction.getOperatorList().isER( name ) ) {
            if(!gameProgress) {
                m_botAction.hideObject(2);
                m_botAction.hideObject(4);
                m_botAction.sendUnfilteredPublicMessage("?set warbird:gravity:0");
                m_botAction.sendUnfilteredPublicMessage("?set warbird:gravitytopspeed:0");
                m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:2000");  //max 6000
                m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:16");   //max 100
                m_botAction.sendUnfilteredPublicMessage("?set warbird:initialrotation:200");//max 800
                m_botAction.sendUnfilteredPublicMessage("?set misc:bouncefactor:28");
                m_botAction.setDoors(255);
                m_botAction.prizeAll(7);
                m_botAction.sendPrivateMessage(name,"Settings reset");
            }
            else
                m_botAction.sendPrivateMessage(name, "I will not reset the arena settings while a game is in progress");
        }
    }

    /****************************************************************/
    /*** Shows help menu for players/staff                        ***/
    /****************************************************************/
    public void doShowHelp( String playerName, String message ) {
        if (m_botAction.getOperatorList().isER(playerName)) {
            m_botAction.remotePrivateMessageSpam(playerName,getModHelpMessage());
        } else {
            m_botAction.remotePrivateMessageSpam(playerName,getPlayerHelpMessage());
        }
    }

    public  String[] getModHelpMessage() {
    	String[] message = {
    		"|---------------------------------------------------------------|",
    		"|  Commands directed privately:                                 |",
    		"|  !start          - Begins a game of enigma.                   |",
    		"|  !stop <name>    - Stops a game of enigma w/<name> as winner. |",
    		"|  !cancel         - Cancels a game of enigma.                  |",
    		"|  !reset          - Resets the arena in case of bot lagout/etc.|",
    		"|  !come           - Moves the bot in case you'd need this ???  |",
    		"|  !whatis         - Answers: what is Enigma?                   |",
    		"|  !lagout         - Want in? Type this to me to play!!!        |",
    		"|  !lagouton       - Toggles the !lagout feature ON             |",
    		"|  !lagoutoff      - Toggles the !lagout feature OFF            |",
    		"|---------------------------------------------------------------|"
	    };
        return message;
    }

    public String[] getPlayerHelpMessage() {
    	String[] help = {
   	        "|---------------------------------------------------------------|",
   	        "|  Commands directed privately:                                 |",
   	        "|  !whatis         - Answers: what is Enigma?                   |",
   	        "|  !lagout         - Want in? Type this to me to play!!!        |",
   	        "|---------------------------------------------------------------|"
	    };
        return help;
    }

    public boolean isUnloadable() {
    	return !gameProgress;
    }

    /****************************************************************/
    /*** Tells what is Enigma. $$$                                ***/
    /****************************************************************/
    public void doWhatIs( String name, String message) {
        m_botAction.sendUnfilteredPrivateMessage(name, "*objon 52");
    }

    /****************************************************************/
    /*** Places a player in the game.                             ***/
    /****************************************************************/
    public void doLagout( String name, String message ) {
    	if (allowLagout) {
    		if(gameProgress) {
    			m_botAction.setShip(name, 1);
    			m_botAction.setFreq(name, 0);
    		} else {
    			m_botAction.sendPrivateMessage( name, "The game hasn't started, just jump in, I'm very busy preparing.");
    		}
    	} else {
    		m_botAction.sendPrivateMessage(name, "The !lagout feature has been disabled by the host.");
    	}
    }

    /****************************************************************/
    /*** Toggles the !lagout feature ON                           ***/
    /****************************************************************/
    public void doLagoutOn(String name, String message) {
        if (m_botAction.getOperatorList().isER(name)) {
        	allowLagout = true;
        	m_botAction.sendPrivateMessage(name, "The !lagout feature is now ON.");
        }
    }

    /****************************************************************/
    /*** Toggles the !lagout feature OFF                          ***/
    /****************************************************************/
    public void doLagoutOff(String name, String message) {
        if (m_botAction.getOperatorList().isER(name)) {
        	allowLagout = false;
        	m_botAction.sendPrivateMessage(name, "The !lagout feature is now OFF.");
        }
    }

    /****************************************************************/
    /*** Runs the timer tasks every second                        ***/
    /****************************************************************/
    void setupTimerTasks() {

        CheckTime = new TimerTask() {
            public void run() {
                curTime += 1;
                //Checks to give door warning.
                if( ( ( curTime + 1 ) == checkDoors ) && gameProgress )
                    m_botAction.showObject(50);

                //Checks for door action.
                if(curTime == checkDoors)
                    doCheckDoors();

                //Checks for event action.
                if( ( curTime == checkEvents ) && gameProgress)
                    doCheckEvents();

                //Checks messages.
                if( curTime == checkMessages ) {
                    if( gameProgress )
                        m_botAction.sendTeamMessage( "If you'd like to play personal message (PM) me with !lagout" );
                    checkMessages += m_delay;
                }

            }
        };
    }

    /****************************************************************/
    /*** Handles checking doors                                   ***/
    /****************************************************************/
    public void doCheckDoors() {
        checkDoors += getRandom( 3, 2 );
        if( gameProgress ) {
            int temp = 0;
            for(int i=0; i < 8;i++) {
                int curValue = getRandom( 2, 0 );
                if(curValue == 0)
                    curValue = getRandom( 2, 0 );
                temp += Math.pow(2 * curValue,i);
            }
            m_botAction.setDoors(temp);
            //Decides if it should flash screen.
            temp = getRandom( 15, 0 );
            if(temp == 1)
                doFlashScreen();

        }
    }

    /****************************************************************/
    /*** Handles checking events                                  ***/
    /****************************************************************/
    public void doCheckEvents() {
        if(eventProgress) {
            //Turns off the last event.
            eventProgress = false;
            switch(lastEvent) {
                case 0:
                    eventZero();
                    break;
                case 1:
                    eventOne();
                    break;
                case 2:
                    eventTwo();
                    break;
                case 3:
                    eventThree();
                    break;
                case 4:
                    eventFour();
                    break;
            }
            checkEvents = curTime + getRandom( 10, m_timeOFF );
        }
        else {
            //Turns on an event.
            eventProgress = true;
            int nextEvent = getRandom( 5, 0 );
            if(nextEvent == lastEvent)
                nextEvent++;

            switch(nextEvent) {
                case 0:
                    eventZero();
                    lastEvent = 0;
                    break;
                case 1:
                    eventOne();
                    lastEvent = 1;
                    break;
                case 2:
                    eventTwo();
                    lastEvent = 2;
                    break;
                case 3:
                    eventThree();
                    lastEvent = 3;
                    break;
                case 4:
                    eventFour();
                    lastEvent = 4;
                    break;
                default:
                    eventZero();
                    lastEvent = 0;
            }

            checkEvents = curTime + getRandom( 10, m_timeON );
        }
    }

    /****************************************************************/
    /*** Flashes Screen Randomly                                  ***/
    /****************************************************************/
    public void doFlashScreen() {
        m_botAction.showObject(3);
        Random generator = new Random();
        int i = Math.abs(generator.nextInt()) % 4;
        switch(i) {
            case(0):
                m_botAction.sendArenaMessage("Madness!", 149);
                break;
            case(1):
                m_botAction.sendArenaMessage("Crazy!", 149);
                break;
            case(2):
                m_botAction.sendArenaMessage("Insane!", 149);
                break;
            case(3):
                m_botAction.sendArenaMessage("Enigmatic!", 149);
                break;
        }
    }

    /****************************************************************/
    /*** Sphere of Seclusion                                      ***/
    /****************************************************************/
    public void eventZero() {
        if(eventProgress) {
            m_botAction.showObject(1);
            m_botAction.sendArenaMessage("Sphere of Seclusion", 153);
            m_botAction.showObject(17);
            CheckAnimation = new TimerTask() {
                public void run() {
                    m_botAction.showObject(2);
                }
            };
            m_botAction.scheduleTask(CheckAnimation,800);
            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:-16");
            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:-2000");
        }
        else {
            m_botAction.hideObject(2);
            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:16");
            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:2000");
        }
    }

    /****************************************************************/
    /*** Shroud of Darkness                                       ***/
    /****************************************************************/
    public void eventOne() {
        if(eventProgress) {
            m_botAction.showObject(4);
            m_botAction.sendArenaMessage("Shroud of Darkness", 151);
            m_botAction.showObject(18);
        }
        else {
            m_botAction.hideObject(4);
        }
    }

    /****************************************************************/
    /*** Unnatural Attraction                                     ***/
    /****************************************************************/
    public void eventTwo() {
        if(eventProgress) {
            m_botAction.sendArenaMessage("Unnatural Attraction",152);
            m_botAction.showObject(19);
            m_botAction.sendUnfilteredPublicMessage("?set warbird:gravity:2000");
            m_botAction.sendUnfilteredPublicMessage("?set warbird:gravitytopspeed:200");
        }
        else {
            m_botAction.sendUnfilteredPublicMessage("?set warbird:gravity:0");
            m_botAction.sendUnfilteredPublicMessage("?set warbird:gravitytopspeed:0");
        }
    }

    /****************************************************************/
    /*** Out of Control                                           ***/
    /****************************************************************/
    public void eventThree() {
        if(eventProgress) {
            m_botAction.sendArenaMessage("Out of Control",150);
            m_botAction.showObject(20);
            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:5999");  //max 6000
            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:99");   //max 100
            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialrotation:799");//max 800
            m_botAction.sendUnfilteredPublicMessage("?set misc:bouncefactor:20");
        }
        else {
            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:2000");  //max 6000
            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:16");   //max 100
            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialrotation:200");//max 800
            m_botAction.sendUnfilteredPublicMessage("?set misc:bouncefactor:28");
        }
    }

    /****************************************************************/
    /*** Imperceptible Walls                                      ***/
    /****************************************************************/
    public void eventFour() {
        if(eventProgress) {
            m_botAction.sendArenaMessage("Out of Control", 154);
            m_botAction.showObject(21);
            m_botAction.showObject(5);
        }
        else
            m_botAction.hideObject(5);
    }

    /****************************************************************/
    /*** Generates Random Integer                                 ***/
    /****************************************************************/
    public int getRandom( int number, int modifier ) {
        int temp = Math.abs( generator.nextInt() ) % number + modifier;
        return temp;
    }

    /****************************************************************/
    /*** Handles Messages                                         ***/
    /****************************************************************/
    public void handleEvent( Message event ) {
        m_commandInterpreter.handleEvent( event );
    }

    public void handleEvent( PlayerEntered event ) {
        if(gameProgress)
            m_botAction.sendPrivateMessage(event.getPlayerName(), "Enigma has started, if you wish to play please pm me with :  !lagout");
    }

}