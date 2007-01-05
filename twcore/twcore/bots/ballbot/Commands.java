//////////////////////////////////////////////////////
//
// Filename:
// Commands.java
// 
// Description: 
// Encapsulates the behaviour of commands and when they should be executed.
//
// Usage:
// Each new command is a subclass of BotCommand.
// Implement DoCommand() to define its functionality.
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import twcore.core.*;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;
import twcore.core.game.Ship;
import twcore.core.util.Tools;

import java.util.*;
import java.lang.Integer;

class BotCommand_Enter extends Command
{
	BotCommand_Enter()
	{
		super();
		
		m_description = "Makes bot enter arena";
		m_command = "!enter";
		m_commandArgs = "";
		m_accessLevel = OperatorList.ER_LEVEL;
		m_msgTypeFilter.SetPrivate( true );
	}

	void Execute( ballbot bot, Message message )
	{	
		Ship s = bot.m_botAction.getShip();
		s.setShip(7);
		s.move(10183,8186);
		
	}
}

class BotCommand_Help extends Command
{
	BotCommand_Help()
	{
		super();
		
		m_description = "Show this help";
		m_command = "!help";
		m_commandArgs = "";
		m_accessLevel = OperatorList.ER_LEVEL;
		m_msgTypeFilter.SetPrivate( true );
	}

	void Execute( ballbot bot, Message message )
	{	
		Vector helpText = bot.m_commandRegistry.GetHelpText();
		
		for( int i=0; i<helpText.size(); i++ )
		{
			bot.m_botAction.sendPrivateMessage( message.getPlayerID(), (String)helpText.elementAt( i ) );
		}
	}
}

class BotCommand_SetSnipeMessage extends Command
{
	BotCommand_SetSnipeMessage()
	{
		super();
		
		m_description = "Set the snipe message";
		m_command = "!snipe";
		m_commandArgs = "<msg>";
		m_accessLevel = OperatorList.ER_LEVEL;
		m_msgTypeFilter.SetPrivate( true );
	}
	
	public boolean AllowCommand( String message )
	{
		return message.startsWith( m_command + " " );
	}	

	void Execute( ballbot bot, Message message )
	{	
		int maxLen = 10;
		String snipeMessage = message.getMessage().substring( m_command.length() + 1 );
		
		if( snipeMessage.length() > maxLen )
			snipeMessage = snipeMessage.substring( 0, maxLen );
		
		Unsorted.m_snipeMessage = snipeMessage;
		bot.m_botAction.sendPrivateMessage( message.getPlayerID(), "Snipe message set to: " + snipeMessage );
	}
}

class BotCommand_WhereAmI extends Command
{
	BotCommand_WhereAmI()
	{
		super();
		
		m_description = "Get your current coordinates";
		m_command = "!whereami";
		m_accessLevel = OperatorList.ER_LEVEL;
		m_msgTypeFilter.SetPrivate( true );
	}

	void Execute( ballbot bot, Message message )
	{	
		int pid = message.getPlayerID();
		Player player = bot.m_botAction.getPlayer( pid );
		Position playerPos = new Position( player.getXLocation(), player.getYLocation() );
		double distance = playerPos.distanceFrom( Arena.CREASE_CENTER_RIGHT );
		bot.m_botAction.sendPrivateMessage( pid, "Your location is: " + playerPos.toString() + " Distance="+(int)(distance)+"cm");
		
		if( Arena.IsInCrease( playerPos ) )
		{
			bot.m_botAction.sendPrivateMessage( pid, "IN CREASE" );
		}	
	}
}

class BotCommand_Die extends Command
{
	BotCommand_Die()
	{
		super();
		
		m_description = "Send me to bot heaven";
		m_command = "!die";
		m_accessLevel = OperatorList.ER_LEVEL;
		m_msgTypeFilter.SetPrivate( true );
	}

	void Execute( ballbot bot, Message message )
	{
		bot.m_botAction.sendArenaMessage( "Removing bot from arena" );
		bot.m_botAction.die();
	}
}