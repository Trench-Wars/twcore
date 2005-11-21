package twcore.bots.racingbot;

import twcore.core.events.Message;

public class Rbopstuff extends RBExtender
{
	public Rbopstuff()
	{
	}
	
	public void handleEvent(Message event)
	{
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			String message = event.getMessage();
			if(m_rbBot.twrcOps.contains(name.toLowerCase()) || m_botAction.getOperatorList().isSmod(name))
				handleCommand(name, message);
		}
	}
	
	public void handleCommand(String name, String message)
	{
		if(message.toLowerCase().startsWith("!specall"))
			m_botAction.specAll();
		else if(message.toLowerCase().startsWith("!setship "))
		{
			try {
				String pieces[] = message.split(" ", 2);
				if(pieces[1].indexOf(":") != -1)
				{
					String params[] = pieces[1].split(":");
					m_botAction.setShip(params[0], Integer.parseInt(params[1]));
				}
				else
					m_botAction.setShip(name, Integer.parseInt(pieces[1]));
			} catch(Exception e) {}
		}
		else if(message.toLowerCase().startsWith("!setfreq "))
		{
			try {
				String pieces[] = message.split(" ", 2);
				if(pieces[1].indexOf(":") != -1)
				{
					String params[] = pieces[1].split(":");
					m_botAction.setFreq(params[0], Integer.parseInt(params[1]));
				}
				else
					m_botAction.setFreq(name, Integer.parseInt(pieces[1]));
			} catch(Exception e) {}
		}
		else if(message.toLowerCase().startsWith("!flagreset"))
			m_botAction.resetFlagGame();
		else if(message.toLowerCase().startsWith("!lock"))
			m_botAction.toggleLocked();
	}
	
	public String[] getHelpMessages()
	{
		String[] help = {
			"!specall             -Specs everyone.",
			"!setship <name>:<#>  -Puts <name> in ship <#>",
			"!setship <#>         -Puts you in ship <#>",
			"!setfreq <name>:<#>  -Puts <name> on freq <#>",
			"!setfreq <#>         -Puts you on freq <#>",
			"!flagreset           -Resets flags.",
			"!lock                -Toggles the arena from locked-unlocked or unlocked-locked"
		};
		
		return help;
	}
	
	public void cancel()
	{
	}
			
}