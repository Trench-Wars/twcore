package twcore.bots.chatgui;

import twcore.core.BotAction;
import twcore.core.events.Message;

public class PrivateChat extends Chat {
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    public PrivateChat(String name, Client client, BotAction botAction) {
		super(name, client, botAction);
	}
	
	public void handleEvent(Message event) {
		if (event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE) {
			short sender = event.getPlayerID();
			String name = event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE ? event.getMessager() :  m_botAction.getPlayerName(sender);
			//String name = m_botAction.getPlayerName(event.getPlayerID());
			if (this.name.equalsIgnoreCase(name)) {
				appendText(name + "> " + event.getMessage(), Message.PRIVATE_MESSAGE);
				setVisible(true);
			}
		}
	}
	
	@Override
	public String getTitle() {
		return "PM: " + name;
	}

	@Override
	public void sendMessage(String message) {
		if (message.isEmpty())
			return;
		m_botAction.sendSmartPrivateMessage(name, message);
		appendText(m_botAction.getBotName() + "> " + message, Message.PRIVATE_MESSAGE);
	}

	
}
