//////////////////////////////////////////////////////
//
// Filename:
// Command.java
// 
// Description: 
// Describes a Command available to the bot.
//
// Usage:
// Create a subclass of BotCommand and implement DoCommand().
// Overwrite the member variables if the defaults do not suffice.
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import twcore.core.*;
import twcore.core.events.Message;
import java.util.*;

// Description of a command suppliable to the bot
abstract class Command
{
	protected String 			m_description 	= "No description";			// Description of command shown in !help
	protected String 			m_command 		= null;						// Command (whole/prefix)
	protected String			m_commandArgs	= "";						// Command arguments (shown in !help)
	protected int 				m_accessLevel 	= OperatorList.ER_LEVEL;	// Command authority level
	protected MessageTypeFilter m_msgTypeFilter = new MessageTypeFilter();	// What message types a responded to... e.g. priv msgs only etc

	abstract void Execute( ballbot bot, Message message );					// Command execution goes here - implement the command by subclassing BotCommand
	
	public void DoCommand( ballbot bot, Message message )
	{		
		Execute( bot, message );
	}	
	
	public boolean AllowCommand( String message )
	{
		return m_command.equals( message );
	}
	
	public String GetDescription()
	{
		return m_description;
	}
	
	public String GetCommand()
	{
		return m_command;
	}
	
	public String GetCommandArgs()
	{
		return m_commandArgs;
	}
	
	public int GetAccessLevel()
	{
		return m_accessLevel;
	}
	
	public MessageTypeFilter GetMessageTypeFilter()
	{
		return m_msgTypeFilter;
	}
}
