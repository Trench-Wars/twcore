package twcore.bots.multibot.starcon;

import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.game.Player;
import twcore.core.util.StringBag;

public class starcon extends MultiModule {

    static final int RUNNING_STATE = 1;
    static final int STOPPED_STATE = 0;

    int                 m_state;
    BotSettings         m_settings;
    TimerTask           m_specannounce;
    TimerTask           m_announcements;
    CommandInterpreter  m_commandInterpreter;
    TimerTask           m_scanForCapitalShips;

    public void init() {

        m_state = STOPPED_STATE;

        m_commandInterpreter = new CommandInterpreter( m_botAction );

        setupTimerTasks();
        registerCommands();
        //        m_botAction.sendUnfilteredPublicMessage( "?chat=robodev" );
    }

    public void cancel()
    {
        m_botAction.cancelTasks();
    }

    public void requestEvents(ModuleEventRequester events)
    {
        events.request( this, EventRequester.PLAYER_ENTERED );
        events.request( this, EventRequester.PLAYER_DEATH );
    }

    public boolean isUnloadable()
    {
        return m_state == STOPPED_STATE;
    }

    public String[] getModHelpMessage()
    {
        String[] message =
        {
            "!Start                        -- Starts the game.",
            "!Stop                         -- Stops the game."
        };

        return message;
    }



    void registerCommands() {
        int         acceptedMessages;

        acceptedMessages = Message.PRIVATE_MESSAGE;

        m_commandInterpreter.registerCommand( "!ping", acceptedMessages, this, "handlePing" );
        m_commandInterpreter.registerCommand( "!stop", acceptedMessages, this, "handleStop" );
        m_commandInterpreter.registerCommand( "!start", acceptedMessages, this, "handleStart" );
        m_commandInterpreter.registerCommand( "!trade", acceptedMessages, this, "handleTrade" );
        m_commandInterpreter.registerCommand( "!capital", acceptedMessages, this, "handleCapital" );

        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "registerShip" );
    }

    void setupTimerTasks() {

        final StringBag   bag = new StringBag();
        final StringBag   teamBag = new StringBag();

        teamBag.add( "Want in? Private message me with :" + m_botAction.getBotName() + ":3 (Rapid-Fire Fighter) or your other choice of ships." );
        teamBag.add( "I know you want to play.  You REALLY want to play.  So type :" + m_botAction.getBotName() + ":2 (Bomber)" );
        teamBag.add( "Come on, don't you read these messages?  It's easy to play.  Type :" + m_botAction.getBotName() + ":5 (Stunner ship) to play." );
        teamBag.add( "Fight for your planet's security!  Your enemies are controlling vast systems of space, the only way to prevent this is to join the battle.  Type :" + m_botAction.getBotName() + ":1 (Fighter) to play." );
        teamBag.add( "They're coming, they're coming!  What do we do?  Fight back!  Join the alliance, type :" + m_botAction.getBotName() + ":6 to play." );
        teamBag.add( "No, I'm not a bot.  I just have no life.  Type :" + m_botAction.getBotName() + ":7 to play." );

        bag.add( "Private message the bot with a ship type (as a number) for ship changes." );
        bag.add( "If you want to enter the game, type :" + m_botAction.getBotName() + ":<ship type>.  The <ship type> must be a number between 1 and 8, but not 4." );

        m_scanForCapitalShips = new TimerTask() {
            public void run() {
                if( !m_botAction.freqContainsShip( 0, 4 )) {
                    m_botAction.sendArenaMessage( "Freq 0 is without a capital ship!  The first person to message " + m_botAction.getBotName() + " with !capital will be the new capital ship." );
                }

                if( !m_botAction.freqContainsShip( 1, 4 )) {
                    m_botAction.sendArenaMessage( "Freq 1 is without a capital ship!  The first person to message " + m_botAction.getBotName() + " with !capital will be the new capital ship." );
                }
            }
        };

        m_announcements = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage( bag.toString() );
            }
        };

        m_specannounce = new TimerTask() {
            public void run() {
                m_botAction.sendTeamMessage( teamBag.toString() );
            }
        };
    }

    public void handleEvent( PlayerDeath event ) {
        Player killee = m_botAction.getPlayer( event.getKilleeID() );

        if( killee.getShipType() == 4 ) {
            final int pid = event.getKilleeID();
            m_botAction.scheduleTask( new TimerTask() {
                public void run() {
                    m_botAction.giveBounty( pid, 1000 );
                }
            }, 8000 );
        }
    }

    public void handleEvent( PlayerEntered event ) {
        if( m_state == RUNNING_STATE ) {
            m_botAction.sendPrivateMessage( event.getPlayerID(), "Welcome to " +
                                            "Starcon!  The game is in progress.  To begin, private message " +
                                            "me the number of the ship you want to be on.");
        }
    }

    public void handleEvent( Message event ) {
        m_commandInterpreter.handleEvent( event );
    }

    public void handlePing( String name, String message ) {

        m_botAction.sendRemotePrivateMessage( name, "Pong!" );
    }

    public void handleStart( String name, String message ) {

        if( opList.isER( name ) == true ) {
            m_state = RUNNING_STATE;

            m_botAction.sendArenaMessage( "Starcon bot activated by " + name );
            m_botAction.sendArenaMessage( "Private message the bot with a ship type (as a number) for ship changes." );
            m_botAction.sendTeamMessage( "If you would like to enter the game, message me with the ship type (as a number) and I will put you into the game." );

            setupTimerTasks();
            m_botAction.scheduleTaskAtFixedRate( m_specannounce, 180000, 180000 );
            m_botAction.scheduleTaskAtFixedRate( m_announcements, 120000, 120000 );
            m_botAction.scheduleTaskAtFixedRate( m_scanForCapitalShips, 10000, 10000 );

            m_botAction.toggleLocked();
        }
    }

    //Not for capital ships...
    public void registerShip( String name, String message ) {
        int shipType = 0;

        if( m_state == RUNNING_STATE ) {
            try {
                shipType = Integer.parseInt( message );
            } catch( NumberFormatException e ) {
                m_botAction.sendPrivateMessage( name, "If you would like to change ships, message me with the ship number." );
                return;
            }

            if( shipType < 1 || shipType > 8 ) {
                m_botAction.sendPrivateMessage( name, "Valid ship types are from 1 to 8." );
                return;
            }

            if( m_botAction.getPlayer( name ).getShipType() == 0 ) {
                if( shipType != 4 ) {
                    m_botAction.sendPrivateMessage( name, "Okay, putting you in as " + shipType );
                    m_botAction.setShip( name, shipType );
                } else {
                    m_botAction.sendPrivateMessage( name, "I'm sorry, but you must "
                                                    + "choose a non-capital ship.");
                }
            } else if( m_botAction.getPlayer( name ).getShipType() != 4 && shipType != 4 ) {
                m_botAction.sendPrivateMessage( name, "Okay, changing your ship.");
                m_botAction.setShip( name, shipType );
            } else if( shipType == 4 ) { //requested ship type is 4
                m_botAction.sendPrivateMessage( name, "Sorry, you cannot change to "
                                                + "the capital ship, the player in the capital ship must nominate "
                                                + "you.  If there is no current capital ship, please use !capital" );
            } else { //actual ship type is 4, capital ship
                m_botAction.sendPrivateMessage( name, "You are the capital ship! "
                                                + "In order to leave, you must die, or get full energy and spec." );
            }
        }
    }

    public void handleTrade( String name, String message ) {
        String      otherName;

        if( m_state == RUNNING_STATE ) {
            otherName = message.toLowerCase().trim();

            if( name.toLowerCase().equals( otherName.toLowerCase() )) {
                m_botAction.sendPrivateMessage( name, "Sorry, you cannot trade with yourself!" );
                return;
            }

            Player p = m_botAction.getPlayer( name );
            Player otherP = m_botAction.getPlayer( otherName );

            if( otherP == null ) {
                m_botAction.sendPrivateMessage( name, "Sorry, " + otherName + " is not a recognized name" );
                return;
            }

            if( p.getShipType() == 4 ) {
                if( p.getFrequency() == otherP.getFrequency() ) {
                    m_botAction.setShip( name, otherP.getShipType() );
                    m_botAction.setShip( otherName, 4 );
                    m_botAction.giveBounty( otherP.getPlayerID(), 1000 );
                    m_botAction.sendArenaMessage( otherName + " is now the new capital ship for " + p.getFrequency() );
                } else {
                    m_botAction.sendPrivateMessage( name, "Sorry, but that person is not on your freq" );
                }
            } else {
                m_botAction.sendPrivateMessage( name, "Sorry, but you're not the capital ship" );
            }
        }
    }

    public void handleStop( String name, String message ) {
        if( opList.isER( name ) == true ) {
            if( m_state == RUNNING_STATE ) {
                m_state = STOPPED_STATE;
                m_botAction.sendArenaMessage( "Starcon Bot Disabled by " + name );
                m_botAction.toggleLocked();
                m_botAction.cancelTask(m_specannounce);
                m_botAction.cancelTask(m_announcements);
                m_botAction.cancelTask(m_scanForCapitalShips);
                m_specannounce = null;
                m_announcements = null;
                m_scanForCapitalShips = null;
            }
        }
    }

    public void handleCapital( String name, String message ) {
        Player          p;
        int             frequency;

        if( m_state == RUNNING_STATE ) {
            p = m_botAction.getPlayer( name );
            frequency = p.getFrequency();

            if( p.getShipType() == 0 ) {
                m_botAction.sendPrivateMessage( name, "You need to be in the game before you can be a capital ship" );
            } else if( !m_botAction.freqContainsShip( frequency, 4 ) ) {
                m_botAction.sendArenaMessage( p.getPlayerName() + " is now the capital ship for Freq " + frequency );
                m_botAction.setShip( p.getPlayerID(), 4 );
                m_botAction.giveBounty( p.getPlayerID(), 1000 );
            } else {
                m_botAction.sendPrivateMessage( name, "Your freq already has a capital ship" );
            }
        }
    }
}