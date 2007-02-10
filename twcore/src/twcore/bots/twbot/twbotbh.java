/*TWBot Wipeout Module
 *
 *Built by: Jacen Solo
 */


package twcore.bots.twbot;

import java.util.*;

import twcore.bots.TWBotExtension;
import twcore.core.*;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;

public class twbotbh extends TWBotExtension
{
	boolean isRunning = false;
	int bounty = 50;

	public twbotbh()
	{
	}

	public void handleEvent(Message event)
	{
		String message = event.getMessage();
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if(m_opList.isER(name))
				handleCommand(name, message);
		}
	}

	public void handleEvent(PlayerDeath event)
	{
		if(!isRunning) return;
		
		Player killer = m_botAction.getPlayer(event.getKillerID());
		Player killee = m_botAction.getPlayer(event.getKilleeID());
		int killerBounty = killer.getBounty();
		int killeeBounty = killee.getBounty();
		if(killerBounty >= bounty && killeeBounty < bounty) {
			m_botAction.sendUnfilteredPrivateMessage(killee.getPlayerName(), "*spec");
			m_botAction.sendUnfilteredPrivateMessage(killee.getPlayerName(), "*spec");
			m_botAction.sendArenaMessage(killee.getPlayerName() + " has been hunted by " + killer.getPlayerName());
		}
	}

	public void handleCommand(String name, String message)
	{
		if(message.toLowerCase().startsWith("!start") && !isRunning)
			handleStart(name, message);
		else if(message.toLowerCase().startsWith("!stop") && isRunning)
			handleStop(name);
	}
	
	public void handleStart(String name, String message) {
		String pieces[] = message.split(" ", 2);
		if(pieces.length == 2) {
			try {
				bounty = Integer.parseInt(pieces[1]);
			} catch(Exception e) {}
		}
		m_botAction.sendArenaMessage("Bounty Hunter mode enabled, bounty required: " + bounty, 2);
		isRunning = true;
	}
	
	public void handleStop(String name) {
		m_botAction.sendArenaMessage("Bounty Hunter mode disabled.", 13);
		isRunning = false;
	}

	public String[] getHelpMessages()
	{
		String[] help = {
			"!start <#>                     - Starts bounty hunter mode",
			"!stop                          - Stops bounty hunter mode"
		};
		return help;
	}

	public void cancel()
	{
	}
}