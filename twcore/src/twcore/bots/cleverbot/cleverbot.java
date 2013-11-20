package twcore.bots.cleverbot;

import java.util.HashMap;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;

import chatterbot.*;

/**
 * The best way to waste my time. Simple cleverbot.
 * @author JabJabJab
 *
 */
public class cleverbot extends SubspaceBot 
{
	//HashMap to store session objects created by our cleverbot.
	private HashMap<String,ChatterBotSession> mapSessions;
	
	//HashMap to store session time to end inactive sessions.
	private HashMap<String,Long> mapSessionTime;
	
	//Requests all of the events from the core
    private EventRequester events;
    
    //Object factory for Cleverbot.
    ChatterBotFactory factory;

    //Our Cleverbot instance.
    ChatterBot cleverBot;
    
	public cleverbot(BotAction botAction) 
	{
		//TWCore stuff.
		super(botAction);
        events = m_botAction.getEventRequester();
        events.request(EventRequester.PLAYER_ENTERED);
        events.request(EventRequester.MESSAGE);
        
        //Create our hashmaps.
        mapSessions = new HashMap<String, ChatterBotSession>();
        mapSessionTime = new HashMap<String, Long>();
        
	}
	
    public void handleEvent(LoggedOn event)
    {
        //Get the data from mybot.cfg
        BotSettings config = m_botAction.getBotSettings();
        //Get the initial arena from config and enter it
        String initial = config.getString("InitialArena");
        m_botAction.joinArena(initial);
        
        //Initializes the factory.
        factory = new ChatterBotFactory();

        //Creates the bot.
		try 
		{
			cleverBot = factory.create(ChatterBotType.CLEVERBOT);
		}
		catch (Exception e) 
		{
			Tools.printStackTrace(e);
		}
		//Sets the bot to the chat channel for chat use.
        m_botAction.sendUnfilteredPublicMessage("?chat=CleverBot");
    }
    
    public void handleEvent(Message event)
    {
    	if(event.getMessageType() != Message.CHAT_MESSAGE && event.getMessageType() != Message.PRIVATE_MESSAGE)
    	{
    		return;
    	}
    	
    	if(event.getMessage() == null)
    	{
    		return;
    	}
    	if(event.getMessage().isEmpty())
    	{
    		return;
    	}
    	
    	long currentTime = System.currentTimeMillis();
    	for(String player : mapSessions.keySet())
    	{
    		long recordedTime = mapSessionTime.get(player);
    		if(recordedTime != 0L)
    		{
    			if((currentTime - recordedTime) > Tools.TimeInMillis.MINUTE * 10)
    			{
    				mapSessions.remove(player);
    			}
    		}
    	}
    	String messenger = event.getMessager();
    	short playerID = event.getPlayerID();
    	if(messenger == null)
    	{
    		messenger = m_botAction.getPlayerName(playerID);
    	}
		ChatterBotSession session = mapSessions.get(messenger);
		if(session == null)
		{
			session = cleverBot.createSession();
			mapSessions.put(messenger, session);
		}
		mapSessionTime.put(messenger, currentTime);
        String cleverBotResponse = null;
		try 
		{
			cleverBotResponse = session.think(event.getMessage());
		}
		catch (Exception e) 
		{
		    Tools.printStackTrace(e);
		}
		if(cleverBotResponse != null)
		{
			String chatMessageResponse = "(To " + messenger + ") ";
			if(event.getMessageType() == Message.CHAT_MESSAGE)
			{					
				m_botAction.sendChatMessage(chatMessageResponse + cleverBotResponse);
			}
			else if(event.getMessageType() == Message.PRIVATE_MESSAGE)
			{
				m_botAction.sendSmartPrivateMessage(messenger, cleverBotResponse);
			}
		}
    }
}
