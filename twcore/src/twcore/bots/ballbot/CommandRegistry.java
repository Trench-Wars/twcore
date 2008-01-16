//////////////////////////////////////////////////////
//
// Filename:
// CommandRegistry.java
//
// Description:
// Holds a list of commands available to the bot.
//
// Usage:
// Create an instance of CommandRegistry.
// Pass "Message" events captured by to the bot to CommandRegistry.ProcessMessage().
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import java.util.Vector;

import twcore.core.OperatorList;
import twcore.core.events.Message;

// Collection of commands that the bot recognises
public class CommandRegistry
{
	public static int OPERATORLIST_ANYONE = 313354; // Magic number. ;)
	private Vector<Command> m_rgdCommands = new Vector<Command>();	// Registered commands

	public CommandRegistry()
	{
		m_rgdCommands.addElement( new BotCommand_Die() );
		m_rgdCommands.addElement( new BotCommand_WhereAmI() );
		m_rgdCommands.addElement( new BotCommand_SetSnipeMessage() );
		m_rgdCommands.addElement( new BotCommand_Help() );
		m_rgdCommands.addElement( new BotCommand_Enter() );
		m_rgdCommands.addElement( new BotCommand_StartAutoHost() );
		m_rgdCommands.addElement( new BotCommand_EndAutoHost() );
	}

	public void ProcessMessage( Message msg, ballbot bot )
	{
		for( int i=0; i< m_rgdCommands.size(); i++ )
			ProcessCommand( m_rgdCommands.elementAt(i), msg, bot );
	}

	private void ProcessCommand( Command command, Message msg, ballbot bot )
	{
		if( !command.AllowCommand( msg.getMessage() ) )
			return;

		if( !command.GetMessageTypeFilter().PassesFilter( msg ))
			return;

		String playerName = bot.m_botAction.getPlayerName( msg.getPlayerID() );

		if(   command.GetAccessLevel() == CommandRegistry.OPERATORLIST_ANYONE )  {  command.DoCommand( bot, msg );  	return; }
		if( ( command.GetAccessLevel() == OperatorList.ZH_LEVEL  			&&  bot.m_botAction.getOperatorList().isZH( playerName ) ) ) 			{ command.DoCommand( bot, msg );  	return; }
		if( ( command.GetAccessLevel() == OperatorList.ER_LEVEL  			&&  bot.m_botAction.getOperatorList().isER( playerName ) ) ) 			{ command.DoCommand( bot, msg );  	return; }
		if( ( command.GetAccessLevel() == OperatorList.MODERATOR_LEVEL  	&&  bot.m_botAction.getOperatorList().isModerator( playerName ) ) ) 	{ command.DoCommand( bot, msg );  	return; }
		if( ( command.GetAccessLevel() == OperatorList.SYSOP_LEVEL  		&&  bot.m_botAction.getOperatorList().isSysop( playerName ) ) ) 		{ command.DoCommand( bot, msg );  	return; }
		if( ( command.GetAccessLevel() == OperatorList.OWNER_LEVEL  		&&  bot.m_botAction.getOperatorList().isOwner( playerName ) ) ) 		{ command.DoCommand( bot, msg );  	return; }
	}

	public Vector<String> GetHelpText()
	{
		return Help.GetHelpText( m_rgdCommands );
	}
}