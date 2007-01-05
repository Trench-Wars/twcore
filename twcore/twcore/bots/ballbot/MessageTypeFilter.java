//////////////////////////////////////////////////////
//
//Filename:
//MessageTypeFilter.java
//
//Description:
//Contains a true/false value for every message type. True = allowed, False = denied.
//
//Usage:
//Create an instance of this and use the "set" functions to define the filter.
//Invoke PassesFilter( Message ) to filter Message objects by using the returned boolean. 
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import twcore.core.*;
import twcore.core.events.Message;
import java.util.*;

class Speech
{
	public static SubspaceBot m_bot;
	
	public static void Say( int msgType, String msg, int sound )
	{
		if( msgType == Message.ARENA_MESSAGE ) 			m_bot.m_botAction.sendArenaMessage( msg, sound );
		if( msgType == Message.CHAT_MESSAGE ) 			;
		if( msgType == Message.OPPOSING_TEAM_MESSAGE ) 	;
		if( msgType == Message.PRIVATE_MESSAGE ) 		;
		if( msgType == Message.PUBLIC_MACRO_MESSAGE ) 	;
		if( msgType == Message.PUBLIC_MESSAGE ) 		m_bot.m_botAction.sendPublicMessage( msg, sound );
		if( msgType == Message.REMOTE_PRIVATE_MESSAGE ) ;
		if( msgType == Message.SERVER_ERROR ) 			;
		if( msgType == Message.TEAM_MESSAGE ) 			;
		if( msgType == Message.WARNING_MESSAGE ) 		;
	}
	
	public static void Say( int msgType, String msg  )
	{
		Say( msgType, msg, 300 );
	}
	
	public static void SayIncident( String msg )
	{
		Say( Message.PUBLIC_MESSAGE, msg );
	}
	
	public static void SayGoal( String msg, int sound )
	{
		Say( Message.ARENA_MESSAGE, msg, sound );
	}
	
	public static void SayGoal( String msg )
	{
		Say( Message.ARENA_MESSAGE, msg );
	}
}

public class MessageTypeFilter
{
	private boolean m_alert 			= false;
	private boolean m_arena 			= false;
	private boolean m_chat 				= false;
	private boolean m_opposing_team 	= false;
	private boolean m_private 			= false;
	private boolean m_public_macro 		= false;
	private boolean m_public 			= false;
	private boolean m_remote_private 	= false;
	private boolean m_server_error 		= false;
	private boolean m_team 				= false;
	private boolean m_warning 			= false;
	
	public MessageTypeFilter()
	{
	}

	public MessageTypeFilter(
			boolean alert, boolean arena, boolean chat,
			boolean opposing_team, boolean _private, boolean public_macro, 
			boolean _public, boolean remote_private, boolean server_error, 
			boolean team, boolean warning)
	{
		m_alert 			= alert;
		m_arena 			= arena;
		m_chat 				= chat;
		m_opposing_team 	= opposing_team;
		m_private 			= _private;
		m_public_macro 		= public_macro;
		m_public 			= _public;
		m_remote_private 	= remote_private;
		m_server_error 		= server_error;
		m_team 				= team;
		m_warning 			= warning;
	}

	public void SetPrivate( boolean set )
	{
		m_private = set;
	}

	public boolean PassesFilter( Message msg )
	{
		int msgType = msg.getMessageType();

		if( msgType == Message.ALERT_MESSAGE ) 			return m_alert;
		if( msgType == Message.ARENA_MESSAGE ) 			return m_arena;
		if( msgType == Message.CHAT_MESSAGE ) 			return m_chat;
		if( msgType == Message.OPPOSING_TEAM_MESSAGE ) 	return m_opposing_team;
		if( msgType == Message.PRIVATE_MESSAGE ) 		return m_private;
		if( msgType == Message.PUBLIC_MACRO_MESSAGE ) 	return m_public_macro;
		if( msgType == Message.PUBLIC_MESSAGE ) 		return m_public;
		if( msgType == Message.REMOTE_PRIVATE_MESSAGE ) return m_remote_private;
		if( msgType == Message.SERVER_ERROR ) 			return m_server_error;
		if( msgType == Message.TEAM_MESSAGE ) 			return m_team;
		if( msgType == Message.WARNING_MESSAGE ) 		return m_warning;

		return false;
	}
}