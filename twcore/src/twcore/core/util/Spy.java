package twcore.core.util;

import java.util.StringTokenizer;

import twcore.core.BotAction;
import twcore.core.events.Message;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Racism spy.  Extremely similar to PubBot's spy module.  When instantiated, a bot
 * can send a copy of all message events received to the Spy, which then checks
 * the message for racism, and sends a ?cheater call if necessary.
 *  
 * This could easily be altered to check for other words or perform another action. 
 * 
 * [added by Pio]
 * Added ability to update via racism.cfg within the corecfg folder.
 * Example usage:
 * import twcore.core.util.Spy;
 * 
 * 
 *     public multibot(BotAction botAction) {
 *       super(botAction);
 *       watcher = new Spy(botAction); // implement spy.java
 *       m_eventModule = null;  
 *  }
 *   
 *   On your bots handleEvent for Messages do the folowing:
 *       public void handleEvent(Message event) {
 *   	   watcher.handleEvent(event);
 *		 }
 *[/added by Pio]
 */

public class Spy {
    public ArrayList<String> keywords = new ArrayList<String>(); // our banned words
    public ArrayList<String> fragments = new ArrayList<String>(); // our banned fragments
    private BotAction   m_botAction;   // BotAction reference

    public Spy( BotAction botAction ) {
		m_botAction = botAction;
		loadConfig();
	}
    
    /*** 
     * Loads the banned keywords and fragments via corecfg/racism.cfg
     */
    public void loadConfig () { 
    	BufferedReader sr;
    	String line;
    	boolean loadWords = true;
    	try {
    		sr = new BufferedReader(new FileReader(m_botAction.getCoreCfg("racism.cfg")));
    		while((line = sr.readLine()) != null)
    		{
    		   if(line.contains("[Words]")) { loadWords = true; }
    		   if(line.contains("[Fragments]")) { loadWords = false; }
    		   
    		   if(line.startsWith("[") == false) { 
    			   if (loadWords) {
    				   keywords.add(line.trim());
    			   }
    			   else {
    				   fragments.add(line.trim());
    			   }
    		   }
    		}
    		sr.close();
    		sr = null;
    	}
    		catch (Exception e) {
    		sr = null;
    	}
    	
    }

    /***
     * Handles the Message event and checks for Banned words
     * @param event event from a Message Event
     */
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
    
    /***
     * Matches messageType to a human representation.
     * @param messageType Type of message to represent.
     * @return Human representation of the messageType.
     */
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
    
    /***
     * Searches words/fragments for banned words in corecfg/racism.cfg
     * @param message Text to check
     * @return True: If a word/fragment is detected. False: If nothing is found.
     */
    public boolean isRacist(String message)
    {
      // remove all special characters
      StringTokenizer words = new StringTokenizer(message.toLowerCase(), " !?:;.,@#$%^&*()[]{}-_=+~`\"\'|\\<>/");
      String word = "";
      
      while(words.hasMoreTokens()) {
    	  word = words.nextToken().trim();
    	  
    	  if(word.length() == 0)
    	      continue;
    	  
    	  for (String i : keywords){ 
    		  if (word.trim().equals(i.trim())) {
    			  return true;
    		  }
    	  }
    	  
    	  for (String i : fragments){ 
    		  if (word.contains(i)) {
    			  return true;
    		  }
    	  }
      }

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