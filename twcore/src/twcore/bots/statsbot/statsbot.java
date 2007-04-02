package twcore.bots.statsbot;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.WatchDamage;
import twcore.core.events.WeaponFired;

/**
 * An early form of stats tracking.
 * 
 * Updates:
 *   - Now allows mods (and not just 2d) to use the special commands
 *   - Added !help 
 * 
 * @author 2dragons
 */
public class statsbot extends SubspaceBot {

	private StatTracker m_tracker;

	/**
	 * @param botAction
	 */
	public statsbot( BotAction _botAction ) {

		super( _botAction );

		//Request the events we'd like to handle
		EventRequester eventRequester = _botAction.getEventRequester();
		eventRequester.request( EventRequester.MESSAGE );
		eventRequester.request( EventRequester.PLAYER_DEATH );
		eventRequester.request( EventRequester.FREQUENCY_CHANGE );
		eventRequester.request( EventRequester.FREQUENCY_SHIP_CHANGE );
		eventRequester.request( EventRequester.WATCH_DAMAGE );
		eventRequester.request( EventRequester.WEAPON_FIRED );
		eventRequester.request( EventRequester.ARENA_JOINED );
		eventRequester.request( EventRequester.FLAG_CLAIMED );
		eventRequester.request( EventRequester.PLAYER_LEFT );

		//Create our stat tracker object
		m_tracker = new StatTracker( _botAction, StatTracker.SCOREBOARD_ON );
	}

	public void handleEvent( ArenaJoined _event ) {

		m_botAction.getShip().move( 8192, 300*16 );
	}

	public void handleEvent( Message _event ) {

		//Propogate the event to the stat tracker
		m_tracker.handleEvent( _event );


		if( _event.getMessageType() != Message.PRIVATE_MESSAGE ) {

			if( _event.getMessageType() == Message.ARENA_MESSAGE ) {
				if( _event.getMessage().startsWith( "Go go go!") ) {
					m_tracker.beginTracking();
					m_botAction.sendPublicMessage( "Tracking ON" );
				} else if( _event.getMessage().startsWith( "Result of") ) {
					m_tracker.endTracking();
					m_botAction.sendPublicMessage( "Tracking OFF" );
				}
			}
			return;
		}

        String name = m_botAction.getPlayerName( _event.getPlayerID() );
        String message = _event.getMessage();

                // Nice, 2d.  Nice :P
		// if( !name.equals( "2dragons" ) ) return;
                if( !m_botAction.getOperatorList().isModerator( name ) )
                    return;

		if( message.startsWith( "!go " ) ) {
			m_botAction.joinArena( message.substring( 4 ) );
			m_tracker.reset();
		}
		if( message.startsWith( "!begin" ) )
			m_tracker.beginTracking();
		if( message.startsWith( "!end" ) )
			m_tracker.endTracking();
		if( message.startsWith( "!s " ) ) {
			m_tracker.spamStats( message.substring( 3 ) );
		}
		else if( message.startsWith( "!spam" ) )
			m_tracker.spamStats();
                if( message.startsWith( "!help" ) ) {
                    String[] spam = {
                            "!go <arena>   - You figure it out",
                            "!begin        - Start tracking",
                            "!end          - Stop tracking",
                            "!s <player>   - Sends stats of particular player to spectators",
                            "!spam         - Spams stats of all players to spectators"
                    };
                    m_botAction.privateMessageSpam( name, spam );
                }
	}

	public void handleEvent( PlayerDeath _event ) {

		//Propogate the event to the stat tracker
		m_tracker.handleEvent( _event );
	}

	public void handleEvent( FrequencyChange _event ) {

		//Propogate the event to the stat tracker
		m_tracker.handleEvent( _event );
	}

	public void handleEvent( FrequencyShipChange _event ) {

		//Propogate the event to the stat tracker
		m_tracker.handleEvent( _event );
	}

	public void handleEvent( WatchDamage _event ) {

		//Propogate the event to the stat tracker
		m_tracker.handleEvent( _event );
	}

	public void handleEvent( WeaponFired _event ) {

		//Propogate the event to the stat tracker
		m_tracker.handleEvent( _event );
	}

	public void handleEvent( LoggedOn _event ) {

		m_botAction.joinArena( "#robopark" );
	}

	public void handleEvent( FlagClaimed _event ) {

		//Propogate the event to the stat tracker
		m_tracker.handleEvent( _event );
	}

	public void handleEvent( PlayerLeft _event ) {

		//Propogate the event to the stat tracker
		m_tracker.handleEvent( _event );
	}

}
