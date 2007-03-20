package twcore.core.util;

import java.util.StringTokenizer;

import twcore.core.*;
import twcore.core.events.Message;

/**
 * Racism spy.  Extremely similar to PubBot's spy module.  When instantiated, a bot
 * can send a copy of all message events received to the Spy, which then checks
 * the message for racism, and sends a ?cheater call if necessary.
 *  
 * This could easily be altered to check for other words or perform another action. 
 */
public class Spy {
    public static final String keywords = "j3w jew chink nig nigger n1g n1gg3r nigg3r paki gook nigg@ n1gg@ nigga";
    
    private BotAction   m_botAction;        // BotAction reference

    public Spy( BotAction botAction ) {
		m_botAction = botAction;
	}
    
    public void handleEvent( Message event ){
        int messageType = event.getMessageType();
        String sender = getSender(event);
        String message = event.getMessage();
        String messageTypeString = getMessageTypeString(messageType);

        if(sender != null && messageType != Message.CHAT_MESSAGE && messageType != Message.PRIVATE_MESSAGE &&
                             messageType != Message.REMOTE_PRIVATE_MESSAGE)
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
    
    private boolean containsWord(String message, String word)
    {
      StringBuffer stringBuffer = new StringBuffer();
      String formattedMessage;
      String lowerWord = word.toLowerCase();
      char character;

      for(int index = 0; index < message.length(); index++)
      {
        character = Character.toLowerCase(message.charAt(index));
        if(Character.isLetterOrDigit(character) || character == ' ')
          stringBuffer.append(character);
      }

      formattedMessage = " " + stringBuffer.toString() + " ";
      return formattedMessage.indexOf(" " + lowerWord + " ") != -1 ||
             formattedMessage.indexOf(" " + lowerWord + "s ") != -1;
    }

    public boolean isRacist(String message)
    {
      StringTokenizer keywordTokens = new StringTokenizer(keywords);

      while(keywordTokens.hasMoreTokens())
        if(containsWord(message, keywordTokens.nextToken()))
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