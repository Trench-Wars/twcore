package twcore.bots.twbot;

import java.util.Iterator;
import java.util.SortedSet;

import twcore.bots.TWBotExtension;
import twcore.core.events.ArenaList;
import twcore.core.events.Message;

/**
 * Module used for listing all the arena names in the current server.
 * Should be ported into a more general purpose module some day.
 * 
 * @author Maverick
 */
public class twbotlist extends TWBotExtension {

	private String commander;
	
	public twbotlist() { }

	public void handleEvent(ArenaList event)
	{
		SortedSet<String> arenaSet = null;
		String[] arenas = event.getArenaNames();
		for(int k = 0;k < arenas.length;k++) {
			arenaSet.add(arenas[k]);
		}
		arenas = null;
		
		if(commander != null && arenaSet != null && arenaSet.isEmpty()==false) {
			Iterator<String> it = arenaSet.iterator();
			while(it.hasNext()) {
				m_botAction.sendSmartPrivateMessage(commander, it.next());
			}
			m_botAction.sendSmartPrivateMessage(commander, "Arenas listed.");
			commander = null;
		}
	}

	public void handleCommand(String name, String message)
	{
		if(message.toLowerCase().startsWith("!list")) {
			commander = name;
			m_botAction.requestArenaList();
		}
			
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
		String[] help = { "!list    - Lists all the arena names" };
		return help;
	}

	public void cancel() { }
}