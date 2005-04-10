/*
 * Created on Jan 3, 2005
 */
package twcore.bots.statsbot;

import twcore.core.ArenaJoined;
import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.FlagClaimed;
import twcore.core.FrequencyChange;
import twcore.core.FrequencyShipChange;
import twcore.core.LoggedOn;
import twcore.core.Message;
import twcore.core.PlayerDeath;
import twcore.core.PlayerLeft;
import twcore.core.SubspaceBot;
import twcore.core.WatchDamage;
import twcore.core.WeaponFired;

/**
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
		
		if( !name.equals( "2dragons" ) ) return;
		
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
