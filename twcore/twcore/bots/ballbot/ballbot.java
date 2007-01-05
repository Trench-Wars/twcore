package twcore.bots.ballbot;

import twcore.core.*;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.events.BallPosition;

public class ballbot extends SubspaceBot
{	
	public CommandRegistry m_commandRegistry;
	private BallMonitor m_ballMonitor;
	private PosMonitor m_posMonitor;
	
	public ballbot( BotAction botAction )
	{
		// Mandatory
		super( botAction );                                         // Every bot has to do this.  It calls the constructor of the superclass.
		
		// Events
		EventRequester events = m_botAction.getEventRequester();    // Request some events from the core:
		events.request( EventRequester.MESSAGE );                   // This is the syntax for requesting messages.  Almost all bots
		events.request( EventRequester.PLAYER_POSITION );           // must have at least this much to start.  Look below, there is a method	
		events.request( EventRequester.BALL_POSITION );
		// Command registry
		
		DebugOut.Print( "Creating CommandRegistry" );
		m_commandRegistry = new CommandRegistry();
		m_ballMonitor = new BallMonitor();
		m_posMonitor = new PosMonitor();
		
		// Static speech class needs a bot to use to talk
		Speech.m_bot = this;
	}


	// This method defines precisely how a bot behaves while it is attempting
	// to log into the zone.  This method is mostly the same on most bots, but
	// many times other such things need to be initialized when the bot
	// connects, not just in the bot's constructor.
	public void handleEvent( LoggedOn event )
	{		
		BotSettings config = m_botAction.getBotSettings();			// Get the .cfg information from the core		
		String initialArena = config.getString( "InitialArena" );	// Get the initial arena from the .cfg file		
		m_botAction.joinArena( "hockey" );						    // Join the arena
	}

	public void handleEvent( Message event )
	{	
		m_commandRegistry.ProcessMessage( event, this );
	}
	
	public void handleEvent( PlayerPosition event )
	{
		m_posMonitor.SubmitEvent( event, this );
	}
	
	public void handleEvent( BallPosition event )
	{
		m_ballMonitor.SubmitEvent( event, this );
	}
	

}