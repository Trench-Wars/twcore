package twcore.bots.multibot.util;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.util.ModuleEventRequester;

/**
 * @author milosh
 */
public class utilideabox extends MultiUtil {
	
	@Override
	public String[] getHelpMessages() {
		String[] message =
	    {
	      "+=============== HELP ===============+",
	      "| !idea <message> - submits an idea  |",
	      "+====================================+"
		
	    };
	    return message;
	}
	
	@Override
	public void init() {}

	@Override
	public void requestEvents(ModuleEventRequester modEventReq) {}
	
	public void handleEvent(Message event)
	{
		String message = event.getMessage();
        String name = event.getMessager() == null ? m_botAction.getPlayerName(event.getPlayerID()) : event.getMessager();
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
			handlePlayerCommands(name, message);
	}
	
	public void handlePlayerCommands(String name, String cmd){
		if(cmd.startsWith("!idea ") && cmd.substring(6).length() > 0){
			if(m_botAction.sendEmailMessage("twsuggestions@googlegroups.com", name, cmd.substring(6)))
				m_botAction.sendSmartPrivateMessage(name, "Idea submitted. Thank you for your suggestion.");
			else m_botAction.sendSmartPrivateMessage(name, "An error occurred while submitting your idea.");
		}
	}
	
	public void cancel() {}
}