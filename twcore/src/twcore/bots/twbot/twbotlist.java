package twcore.bots.twbot;

import twcore.bots.TWBotExtension;
import twcore.core.events.ArenaList;
import twcore.core.events.Message;

public class twbotlist extends TWBotExtension {

	public twbotlist() { }

	public void handleEvent(ArenaList event)
	{
		String[] arenas = event.getArenaNames();
		for(int k = 0;k < arenas.length;k++)
			m_botAction.sendUnfilteredPublicMessage(arenas[k]);

		m_botAction.sendUnfilteredPublicMessage("Arenas listed.");
	}

	public void handleCommand(String name, String message)
	{
		if(message.toLowerCase().startsWith("!list"))
			m_botAction.requestArenaList();
	}

	public void handleEvent(Message event)
	{
		String message = event.getMessage();
		String name = "";

		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
			name = m_botAction.getPlayerName(event.getPlayerID());
		else if(event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
			name = event.getMessager();

		if(m_opList.isZH(name))
			handleCommand(name, message);
	}

	public String[] getHelpMessages() {
		String[] hi = { "" };
		return hi;
	}

	public void cancel() { }
}