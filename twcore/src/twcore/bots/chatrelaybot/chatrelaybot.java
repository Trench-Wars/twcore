package twcore.bots.chatrelaybot;

import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.ipc.IPCMessage;

public class chatrelaybot extends SubspaceBot {

	private static final String CHAT_COMMAND = "!active";
	private String requester;    
    private BotSettings m_botSettings;
    
    /** Creates a new instance of ultrabot */
    public chatrelaybot(BotAction botAction) {
        super(botAction);
        requestEvents();
        m_botSettings = m_botAction.getBotSettings();
    }
    
    
    /** Request events that this bot requires to receive.  */
    public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.LOGGED_ON);
    }
    
    
    public void handleEvent(Message event) {
    	String message = event.getMessage();
    	String sender = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());
    	if(event.getMessageType() == Message.CHAT_MESSAGE) {    		
    		int chatNumber = event.getChatNumber();
    		String sendMessage = sender + ":" + message;
    		if(chatNumber == 2) {
    			if(m_botAction.getOperatorList().isBot(sender)) {
    				IPCMessage sendIPCMessage = new IPCMessage(sendMessage, "staff", m_botAction.getBotName());
    				m_botAction.ipcTransmit("crosszones", sendIPCMessage);
    			}
    		} else if(chatNumber == 1) {
    			IPCMessage sendIPCMessage = new IPCMessage(sendMessage, "twdev", m_botAction.getBotName());
    			m_botAction.ipcTransmit("crosszones", sendIPCMessage);
    		}
    	}
    	else if(event.getMessageType() == Message.ARENA_MESSAGE){
    		if(message.startsWith("(local) twdev: ")){
    			String msg = message.substring(15);
    			m_botAction.sendSmartPrivateMessage( requester, msg);
    		}    		
    	}
    	else if(event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE){
    		if(message.equalsIgnoreCase(CHAT_COMMAND)){
    			requester = event.getMessager();
    			m_botAction.sendUnfilteredPublicMessage("?chat");
    		}
    		else if(message.equalsIgnoreCase("!die")){
    			m_botAction.sendSmartPrivateMessage(sender, "Logging off.");
    			m_botAction.scheduleTask(new DieTask(m_botAction.getBotName()), 500);
    		}
    	}
    }
    
    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botSettings.getString("arena"));
        m_botAction.sendUnfilteredPublicMessage("?chat=twdev");
        m_botAction.sendUnfilteredPublicMessage("?chat=twdev");
        m_botAction.ipcSubscribe("crosszones");
    }
    
    public void handleEvent(InterProcessEvent event) {
    	IPCMessage message = (IPCMessage)event.getObject();
    	if(message.getSender().equals(m_botAction.getBotName())) return;
    	if(message.getRecipient().equals("staff")) m_botAction.sendChatMessage(2, message.getMessage());
    	else if(message.getRecipient().equals("twdev")) {
    		String temp = message.getMessage();
    		int index = temp.indexOf(":");    		
    		String msg = temp.substring(index+1);
    		if(msg.equalsIgnoreCase(CHAT_COMMAND)){
    			requester = temp.substring(0,index);
    			m_botAction.sendUnfilteredPublicMessage("?chat");
    		}    			
    		else {
    			m_botAction.sendChatMessage(1, temp.substring(0,index) + "> " + msg);
    		}
    	}
    }
    
    private class DieTask extends TimerTask {
        String m_initiator;

        public DieTask( String name ) {
            super();
            m_initiator = name;
        }

        public void run() {
            m_botAction.die( "!die initiated by " + m_initiator );
        }
    }
}