package twcore.bots.cleverbot;

import java.io.IOException;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;

import com.google.code.chatterbotapi.*;

/**
 * Cleverbot is a bot designed to link player messaging to the CleverBot API to work
 * as a medium to the cleverbot service.
 * @author JabJabJab
 *
 */
public class cleverbot extends SubspaceBot 
{
	//Requests all of the events from the core
    private EventRequester events;
    
    //Main chatterBotSession instance.
    private ChatterBotSession mainSession;
    
    //Object factory for Cleverbot.
    ChatterBotFactory factory;

    //Our Cleverbot instance.
    ChatterBot cleverBot;
    
	public cleverbot(BotAction botAction) 
	{
		//TWCore stuff.
		super(botAction);
        events = m_botAction.getEventRequester();
        events.request(EventRequester.MESSAGE);
        
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
			e.printStackTrace();
		}
		//Sets the bot to the chat channel for chat use.
        m_botAction.sendUnfilteredPublicMessage("?chat=CleverBot");
        
        //Creates our main session.
        mainSession = cleverBot.createSession();
    }
    
    public void handleEvent(Message event)
    {
    	//If it's either private or chat-recieved message.
    	if(event.getMessageType() != Message.CHAT_MESSAGE && event.getMessageType() != Message.PRIVATE_MESSAGE)
    	{
    		return;
    	}
    	
    	//if the message is null.
    	if(event.getMessage() == null)
    	{
    		return;
    	}

    	//Grabs the user's name.
    	String messenger = event.getMessager();
    	
    	if(messenger == null)
    	{
    		short playerID = event.getPlayerID();
    		messenger = m_botAction.getPlayerName(playerID);
    	}
    	
    	//if someone sends a blank message
    	if(event.getMessage().isEmpty()) 
    	{
    		if(event.getMessage().contains("!help") && event.getMessageType() == Message.PRIVATE_MESSAGE)
	    	{
	    		m_botAction.sendSmartPrivateMessage(messenger,"Hi, I'm Cleverbot! If you want to use me, either PM me a message, or use ?chat=Cleverbot and type a message there. I'll surely enjoy your company there. :)");
	    	}
    	 return;
    	}
    	
    	//String to pass back to player.
        String cleverBotResponse = null;
		
        //Chat message response.
		String chatMessageResponse = "(To " + messenger + ") ";
		
        //executes the cleverbot api to respond to statement.
        try 
		{
			cleverBotResponse = mainSession.think(event.getMessage());
		}
		catch (Exception e) 
		{
			if(e instanceof IOException)
			{
				cleverBotResponse = "lol";
			}
			else
			{				
				Tools.printStackTrace(e);
			}
		}
		if(cleverBotResponse != null)
		{
			//Returns message through chat system.
			if(event.getMessageType() == Message.CHAT_MESSAGE)
			{					
				m_botAction.sendChatMessage(chatMessageResponse + cleverBotResponse);
			}
			//Returns message through privsate messaging.
			else if(event.getMessageType() == Message.PRIVATE_MESSAGE)
			{
				m_botAction.sendSmartPrivateMessage(messenger, cleverBotResponse);
			}
		}
    }
}
