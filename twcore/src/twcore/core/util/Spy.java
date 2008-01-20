package twcore.core.util;

import java.util.StringTokenizer;

import twcore.core.BotAction;
import twcore.core.events.Message;
import twcore.core.BotSettings;

/**
 * Racism spy.  Extremely similar to PubBot's spy module.  When instantiated, a bot
 * can send a copy of all message events received to the Spy, which then checks
 * the message for racism, and sends a ?cheater call if necessary.
 *  
 * This could easily be altered to check for other words or perform another action. 
 * 
 * <added by Pio>
 * Added ability to update via racism.cfg within the data folder.
 * Example usage:
 * import twcore.core.util.Spy;
 * 
 * 
 *     public multibot(BotAction botAction) {
 *       super(botAction);
 *       watcher = new Spy(botAction); // implement spy.java
 *       m_eventModule = null;  
 *  }
    
    On your bots handleEvent for Messages do the folowing:
        public void handleEvent(Message event) {
    	   watcher.handleEvent(event);
    	}
 *</added by Pio>
 */
public class Spy {
    public String keywords = "";    
    private BotAction   m_botAction;        // BotAction reference

    public Spy( BotAction botAction ) {
		m_botAction = botAction;
		keywords = new BotSettings(m_botAction.getCoreCfg("racism.cfg")).getString("words"); 
		
		if(keywords == null) {
		    keywords = "nig n1g jew gook chink"; // just in case if racism.cfg isn't found
		}
	}
    
    public void handleEvent( Message event ){
        int messageType = event.getMessageType();
        String sender = getSender(event);
        String message = event.getMessage();
        String messageTypeString = getMessageTypeString(messageType);

        if(sender != null)
        {
          if(isRacist(message))
              m_botAction.sendUnfilteredPublicMessage("?cheater " + messageTypeString + ": (" + sender + "): " + message);
        }
    }
    
    private String getMessageTypeString(int messageType)
    {
      switch(messageType)
      {
        case Message.PUBLIC_MESSAGE:
          return "Public";
        case Message.PRIVATE_MESSAGE:
          return "Private";
        case Message.TEAM_MESSAGE:
          return "Team";
        case Message.OPPOSING_TEAM_MESSAGE:
          return "Opp. Team";
        case Message.ARENA_MESSAGE:
          return "Arena";
        case Message.PUBLIC_MACRO_MESSAGE:
          return "Pub. Macro";
        case Message.REMOTE_PRIVATE_MESSAGE:
          return "Private";
        case Message.WARNING_MESSAGE:
          return "Warning";
        case Message.SERVER_ERROR:
          return "Serv. Error";
        case Message.ALERT_MESSAGE:
          return "Alert";
      }
      return "Other";
    }    
    
    public boolean isRacist(String message)
    {
      StringTokenizer keywordTokens = new StringTokenizer(keywords);

      while(keywordTokens.hasMoreTokens())
    	if(message.toLowerCase().contains(keywordTokens.nextToken()))
          return true;
      return false;
    }

    /**
     * This method gets the sender from a message Event.
     *
     * @param event is the message event to analyze.
     * @return the name of the sender is returned.  If the sender cannot be
     * determined then null is returned.
     */

    private String getSender(Message event)
    {
      int messageType = event.getMessageType();

      if(messageType == Message.REMOTE_PRIVATE_MESSAGE || messageType == Message.CHAT_MESSAGE)
        return event.getMessager();
      int senderID = event.getPlayerID();
      return m_botAction.getPlayerName(senderID);
    }
    
}