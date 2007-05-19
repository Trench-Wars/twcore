package twcore.bots.powerballbot;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.BallPosition;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.SoccerGoal;

/**
 * Used for hockey or other powerball-based games. 
 */
public class powerballbot extends SubspaceBot {

	PowerBallManager m_manager;

	public powerballbot( BotAction botAction ) {

		super( botAction );
		EventRequester events = m_botAction.getEventRequester();
        //events.request( EventRequester.MESSAGE );
        //events.request( EventRequester.FREQUENCY_SHIP_CHANGE );
        //events.request( EventRequester.PLAYER_LEFT );
        //events.request( EventRequester.PLAYER_DEATH );
        //events.request( EventRequester.WATCH_DAMAGE );

        m_manager = new PowerBallManager( m_botAction );
	}

	public void handleEvent( BallPosition event ) {
		PowerBallEvent pbEvent = m_manager.handleEvent( event );
		//Handle event now.

		if( pbEvent.saved() )
			m_botAction.sendArenaMessage( "Saved by " + pbEvent.getSaver() );
		if( pbEvent.shotOnGoal() )
			m_botAction.sendArenaMessage( "Shot On Goal by " + pbEvent.getShooter() );

	}

	public void handleEvent( FrequencyChange event ) {
		m_manager.handleEvent( event );
		//Handle event now.
	}

	public void handleEvent( FrequencyShipChange event ) {
		m_manager.handleEvent( event );
		//Handle event now.
	}

	public void handleEvent( PlayerDeath event ) {
		m_manager.handleEvent( event );
		//Handle event now.
	}

	public void handleEvent( PlayerLeft event ) {
		m_manager.handleEvent( event );
		//Handle event now.
	}

	//player position?

	public void handleEvent( SoccerGoal event ) {
		PowerBallEvent pbEvent = m_manager.handleEvent( event );
		//Handle event now.

		if( pbEvent.getScorer() != null )
			m_botAction.sendArenaMessage( "Goal By: " + pbEvent.getScorer() );
		if( pbEvent.getAssist() != null )
			m_botAction.sendArenaMessage( "Assist By: " + pbEvent.getAssist() );
		if( pbEvent.getSecondAssist() != null )
			m_botAction.sendArenaMessage( "2nd Assist By: " + pbEvent.getSecondAssist() );
	}

	//weapon fired?



	public void handleEvent( LoggedOn event ) {
		m_botAction.joinArena( "hockey" );
	}
}