package twcore.bots.messagebot;

import twcore.core.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/** Bot to host "channels" that allow a player to ?message or pm
 *  everyone on the channel so that information can be spread easily.
 *
 *  @author Ikrit
 *  @version 1.4
 *  
 *  Added database support of messages because I forgot SSC messaging only
 *  allows one message from a person at a time.
 *  
 *  Added pubbot support so the bot can PM a player that has just logged
 *  in to tell them if they have messages.
 */
public class messagebot extends SubspaceBot
{
	HashMap channels;
	CommandInterpreter m_CI;
	TimerTask messageDeleteTask;
	public static final String IPCCHANNEL = "messages";
	
	/** Constructor, requests Message and Login events.
	 *  Also prepares bot for use.
	 */
	public messagebot(BotAction botAction)
	{
		super(botAction);
		EventRequester events = m_botAction.getEventRequester();
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.LOGGED_ON);
		channels = new HashMap();
		m_CI = new CommandInterpreter(m_botAction);
		registerCommands();
		deleteTask();
		m_botAction.scheduleTaskAtFixedRate(messageDeleteTask, 30 * 60 * 1000, 30 * 60 * 1000);
	}
	
	/** This method handles an InterProcessEvent
	 *  @param event is the InterProcessEvent to handle.
	 */
	public void handleEvent(InterProcessEvent event)
	{
		System.out.println("IPCEVENT");
		IPCMessage ipcMessage = (IPCMessage) event.getObject();
		String message = ipcMessage.getMessage();
		String recipient = ipcMessage.getRecipient();
		String sender = ipcMessage.getSender();
		String botSender = event.getSenderName();
		checkNewMessages(message.toLowerCase());
	}
	
	/** Checks to see if the player has new messages.
	 *  @param Name of player to check.
	 */
	public void checkNewMessages(String name)
	{
		String query = "SELECT * FROM tblMessageSystem WHERE fcName = \""+name+"\" and fnRead = 0";
		try {
			ResultSet results = m_botAction.SQLQuery("local", query);
			boolean found = false;
			while(results.next() && !found)
			{
				int read = results.getInt("fnRead");
				if(read == 0)
				{
					m_botAction.sendSmartPrivateMessage(name, "You have new messages. PM me with !messages to receive them.");
					found = true;
				}
			}
		} catch(Exception e) { Tools.printStackTrace(e); }
	}
	
	/** Sets up the CommandInterpreter to respond to
	 *  all of the commands.
	 *
	 */
	public void registerCommands() {
        int acceptedMessages;
        
        acceptedMessages = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
        m_CI.registerCommand( "!create",     acceptedMessages, this, "createChannel" );
        m_CI.registerCommand( "!destroy",    acceptedMessages, this, "destroyChannel" );
        m_CI.registerCommand( "!join",  acceptedMessages, this, "joinChannel" ); 
        m_CI.registerCommand( "!quit",  acceptedMessages, this, "quitChannel" ); 
        m_CI.registerCommand( "!help",  acceptedMessages, this, "doHelp" ); 
        m_CI.registerCommand( "!accept",  acceptedMessages, this, "acceptPlayer" ); 
        m_CI.registerCommand( "!decline",  acceptedMessages, this, "declinePlayer" ); 
        m_CI.registerCommand( "!announce",  acceptedMessages, this, "announceToChannel" ); 
        m_CI.registerCommand( "!message",  acceptedMessages, this, "messageChannel" ); 
        m_CI.registerCommand( "!requests", acceptedMessages, this, "listRequests"); 
        m_CI.registerCommand( "!ban", acceptedMessages, this, "banPlayer"); 
        m_CI.registerCommand( "!unban", acceptedMessages, this, "unbanPlayer"); 
        m_CI.registerCommand( "!makeop", acceptedMessages, this, "makeOp"); 
        m_CI.registerCommand( "!deop", acceptedMessages, this, "deOp"); 
        m_CI.registerCommand( "!owner", acceptedMessages, this, "sayOwner"); 
        m_CI.registerCommand( "!grant", acceptedMessages, this, "grantChannel"); 
        m_CI.registerCommand( "!private", acceptedMessages, this, "makePrivate"); 
        m_CI.registerCommand( "!public", acceptedMessages, this, "makePublic"); 
        m_CI.registerCommand( "!unread", acceptedMessages, this, "setAsNew");
        m_CI.registerCommand( "!read", acceptedMessages, this, "readMessage");
        m_CI.registerCommand( "!delete", acceptedMessages, this, "deleteMessage");
        m_CI.registerCommand( "!messages", acceptedMessages, this, "myMessages");
        m_CI.registerCommand( "!go", acceptedMessages, this, "handleGo");
        m_CI.registerCommand( "!members", acceptedMessages, this, "listMembers");
        m_CI.registerCommand( "!banned", acceptedMessages, this, "listBanned");
        m_CI.registerCommand( "!me", acceptedMessages, this, "myChannels");
        
        m_CI.registerDefaultCommand( Message.REMOTE_PRIVATE_MESSAGE, this, "doNothing"); 
    }
    
    /** Creates a channel
     *  @param Name of the player creating the channel.
     *  @param Name of the channel they are trying to create.
     */
    public void createChannel(String name, String message)
    {
    	if(channels.containsKey(message.toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "Sorry, this channel is already owned.");
    		return;
    	}
    	if(!m_botAction.getOperatorList().isZH(name))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "Sorry, you need to be a ZH+ to create a channel.");
    		return;
    	}
    	
    	Channel c = new Channel(name.toLowerCase(), message.toLowerCase(), m_botAction, true, false);
    	channels.put(message.toLowerCase(), c);
    	
    	String query = "INSERT INTO tblChannel (fcChannelName, fcOwner, fnPrivate) VALUES(\""+message.toLowerCase()+"\", \""+name.toLowerCase()+"\", 0)";
    	try {
    		m_botAction.SQLQuery("local", query);
    	} catch(SQLException sqle) { Tools.printStackTrace( sqle ); }
    }
    
    /** Allows a channel owner or Highmod to delete a channel.
     *  @param Name of player.
     *  @param Name of channel being deleted.
     */
    public void destroyChannel(String name, String message)
    {
    	if(!channels.containsKey(message.toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(message.toLowerCase());
    	if(c.isOwner(name) || m_botAction.getOperatorList().isHighmod(name))
    	{
    		String query = "DELETE FROM tblChannel WHERE fcChannelName = " + message.toLowerCase();
    		String query2 = "DELETE FROM tblChannelUser WHERE fcChannel = " + message.toLowerCase();
    		try {
    			m_botAction.SQLQuery("local", query);
    			m_botAction.SQLQuery("local", query2);
    		} catch(SQLException sqle) { Tools.printStackTrace( sqle ); }
    		m_botAction.sendSmartPrivateMessage(name, "Channel deleted.");
    		c.messageChannel(name, "Channel " + message + " deleted.");
    		channels.remove(message.toLowerCase());
    	}
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Puts in a request to join a channel.
     *  @param Name of player.
     *  @param Name of channel they want to join.
     */
    public void joinChannel(String name, String message)
    {
    	if(!channels.containsKey(message.toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(message.toLowerCase());
    	c.joinRequest(name);    	
    }
    
    /** Allows a person to quit a channel.
     *  @param Name of player.
     *  @param Name of channel being quit.
     */
    public void quitChannel(String name, String message)
    {
    	if(!channels.containsKey(message.toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(message.toLowerCase());
    	if(c.isOwner(name))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "You cannot leave while you are owner.");
    		return;
    	}
    	c.leaveChannel(name);
    }
    
    /** Accepts a player into the channel.
     *  @param Name of operator.
     *  @param Name of channel and player beinc accepted.
     */
    public void acceptPlayer(String name, String message)
    {
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(pieces[0].toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(pieces[0].toLowerCase());
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name))
  		  	c.acceptPlayer(name, pieces[1]);
  		else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Declines a player's request to join a channel.
     *  @param Name of operator.
     *  @param Name of channel and player being rejected.
     */
    public void declinePlayer(String name, String message)
    {
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(pieces[0].toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(pieces[0].toLowerCase());
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.rejectPlayer(name, pieces[1]);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Sends a pm to all players on the channel.
     *  @param Name of operator.
     *  @param Name of channel and message to be sent.
     */
    public void announceToChannel(String name, String message)
    {
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(pieces[0].toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(pieces[0].toLowerCase());
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.announceToChannel(name, pieces[1]);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
	
	/** Sends ?message's to every player on a channel.
	 *  @param Name of operator.
	 *  @param Name of channel and message to be sent.
	 */
	public void messageChannel(String name, String message)
    {
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(pieces[0].toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(pieces[0].toLowerCase());
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.messageChannel(name, pieces[1]);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Lists all requests to join a channel.
     *  @param Name of operator
     *  @param Name of channel.
     */
    public void listRequests(String name, String message)
    {
    	if(!channels.containsKey(message.toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(message.toLowerCase());
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.listRequests(name);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Bans a player from a channel
     *  @param Name of operator
     *  @param Name of channel and player being banned.
     */
    public void banPlayer(String name, String message)
    {
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(pieces[0].toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(pieces[0].toLowerCase());
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.banPlayer(name, pieces[1]);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Removes a player's ban.
     *  @param Name of operator.
     *  @param Name of channel and player being unbanned.
     */
    public void unbanPlayer(String name, String message)
    {
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(pieces[0].toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(pieces[0].toLowerCase());
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.unbanPlayer(name, pieces[1]);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Lists all requests to join a channel.
     *  @param Name of operator
     *  @param Name of channel.
     */
    public void listBanned(String name, String message)
    {
    	if(!channels.containsKey(message.toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(message.toLowerCase());
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.listBanned(name);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Lists all requests to join a channel.
     *  @param Name of operator
     *  @param Name of channel.
     */
    public void listMembers(String name, String message)
    {
    	if(!channels.containsKey(message.toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(message.toLowerCase());
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.listMembers(name);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Makes a player a channel operator.
     *  @param Name of owner
     *  @param Name of channel and player being op'd.
     */
    public void makeOp(String name, String message)
    {
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(pieces[0].toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(pieces[0].toLowerCase());
    	if(c.isOwner(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.makeOp(name, pieces[1]);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Revokes a player's operator status
     *  @param Name of owner
     *  @param Name of channel and player.
     */
    public void deOp(String name, String message)
    {
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(pieces[0].toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(pieces[0].toLowerCase());
    	if(!c.isOp(name))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That player is not an operator.");
    		return;
    	}
    	
    	if(c.isOwner(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.deOp(name, pieces[1]);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Grants ownership of a channel to a new player.
     *  @param Name of owner/Highmod
     *  @param Name of channel and player beign given operator.
     */
    public void grantChannel(String name, String message)
    {
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(pieces[0].toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(pieces[0].toLowerCase());
    	if(c.isOwner(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.newOwner(name, pieces[1]);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Tells who the owner of the requested channel is.
     *  @param Name of player
     *  @param Name of channel
     */
    public void sayOwner(String name, String message)
    {
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(pieces[0].toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(pieces[0].toLowerCase());
    	m_botAction.sendSmartPrivateMessage(name, "Owner: " + c.owner);
    }
    
    /** Makes a channel public.
     *  @param Name of operator
     *  @param Name of channel
     */
    public void makePublic(String name, String message)
    {
    	if(!channels.containsKey(message.toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(message.toLowerCase());
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.makePublic(name);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Makes a channel private
     *  @param Name of operator
     *  @param Name of channel.
     */
    public void makePrivate(String name, String message)
    {
    	if(!channels.containsKey(message.toLowerCase()))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(message.toLowerCase());
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name))
    		c.makePrivate(name);
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Tells the player all the channels he/she is on.
     *  @param Name of player
     *  @param does nothing
     */
     
    public void myChannels(String name, String message)
    {
    	String query = "SELECT * FROM tblChannelUser WHERE fcName = \""+name.toLowerCase()+"\"";
    	try {
    		ResultSet results = m_botAction.SQLQuery("local", query);
    		while(results.next())
    		{
    			String channel = results.getString("fcChannel");
    			channel = channel.toLowerCase();
    			Channel c = (Channel)channels.get(channel);
    			if(c.isOp(name.toLowerCase()))
    				channel += ": Operator.";
    			else if(c.isOwner(name.toLowerCase()))
    				channel += ": Owner.";
    			else
    				channel += ": Member.";
    			m_botAction.sendSmartPrivateMessage(name, channel);
    		}
    	} catch(Exception e) { Tools.printStackTrace(e); }
    }
    			
    
    /** Sends help messages
     *  @param Name of player.
     */
    public void doHelp(String name, String message)
    {
    	m_botAction.sendSmartPrivateMessage(name, "Commands for any player:");
        m_botAction.sendSmartPrivateMessage(name, "!join <channel>                -Puts in request to join <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!quit <channel>                -Removes you from <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!owner <channel>               -Tells you who owns <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!unread <num>                  -Sets message <num> as unread.");
        m_botAction.sendSmartPrivateMessage(name, "!read <num>                    -PM's you message <num>.");
        m_botAction.sendSmartPrivateMessage(name, "!delete <num>                  -Deletes message <num>.");
        m_botAction.sendSmartPrivateMessage(name, "!messages                      -PM's you all your message numbers.");
        m_botAction.sendSmartPrivateMessage(name, "!help                          -Sends you this help message.");
        m_botAction.sendSmartPrivateMessage(name, "");
        m_botAction.sendSmartPrivateMessage(name, "Any channel owner commands:");
        m_botAction.sendSmartPrivateMessage(name, "!announce <channel>:<message>  -Sends everyone on <channel> a pm of <message>.");
        m_botAction.sendSmartPrivateMessage(name, "!message <channel>:<message>   -Leaves a message for everyone on <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!requests <channel>            -PM's you with all the requests to join <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!banned <channel>              -Lists players banned on <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!members <channel>             -Lists all members on <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!ban <channel>:<name>          -Bans <name> from <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!unban <channel>:<name>        -Lifts <name>'s ban from <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!makeop <channel>:<name>       -Makes <name> an operator in <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!deop <channel>:<name>         -Revokes <name>'s operator status in <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!grant <channel>:<name>        -Grants ownership of <channel> to <name>.");
        m_botAction.sendSmartPrivateMessage(name, "!private <channel>             -Makes <channel> a request based channel.");
        m_botAction.sendSmartPrivateMessage(name, "!public <channel>              -Makes <channel> open to everyone.");
        m_botAction.sendSmartPrivateMessage(name, "!destroy <channel>             -Destroys <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!accept <channel>:<name>       -Accepts <name>'s request to join <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!decline <channel>:<name>      -Declines <name>'s request to join <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "!me                            -Tells you what chennels you have joined.");
        
        if(m_botAction.getOperatorList().isZH(name))
       		m_botAction.sendSmartPrivateMessage(name, "!create <channel>              -Creates a channel with the name <channel>.");
       	if(m_botAction.getOperatorList().isHighmod(name))
       		m_botAction.sendSmartPrivateMessage(name, "You can do any command for any channel.");
    }
    
    /** Does nothing
     */
    public void doNothing(String name, String message) {}
	
	/** Passes a message event to the command interpreter
	 *  @param The message event being passed.
	 */
	public void handleEvent(Message event)
	{
		m_CI.handleEvent(event);
	}
	
	/** Sends the bot to the default arena and reloads all channels.
	 */
	public void handleEvent(LoggedOn event)
	{
		m_botAction.joinArena(m_botAction.getBotSettings().getString("Default arena"));
		m_botAction.ipcSubscribe(IPCCHANNEL);
		
		String query = "SELECT * FROM tblChannel";
		try {
			ResultSet results = m_botAction.SQLQuery("local", query);
			
			while(results.next())
			{
				String channelName = results.getString("fcChannelName");
				String owner = results.getString("fcOwner");
				int pub = results.getInt("fnPrivate");
				boolean priv;
				if(pub == 1)
					priv = false;
				else
					priv = true;
				Channel c = new Channel(owner, channelName, m_botAction, priv, true);
				c.reload();
				channels.put(channelName.toLowerCase(), c);
			}
		} catch(Exception e) { Tools.printStackTrace( e ); }
	}
	
	/** Reads a message from the database.
	 *  @param Name of player reading the message.
	 *  @param Message number being read.
	 */
	public void readMessage(String name, String message)
	{
		int messageNumber = -1;
		try{
			messageNumber = Integer.parseInt(message);
		} catch(Exception e) {
			e.printStackTrace();
			m_botAction.sendSmartPrivateMessage(name, "Invalid message number");
			return;
		}
		String query = "SELECT * FROM tblMessageSystem WHERE fcName = \""+name+"\" AND fnID = " + messageNumber;
		try{
			ResultSet results = m_botAction.SQLQuery("local", query);
			if(results.next())
			{
				message = results.getString("fcMessage");
				String timestamp = results.getString("fdTimeStamp");
				
				m_botAction.sendSmartPrivateMessage(name, timestamp + " " + message);
				
				query = "UPDATE tblMessageSystem SET fnRead = 1 WHERE fcName = \""+name.toLowerCase()+"\" AND fnID = " + messageNumber;
				m_botAction.SQLQuery("local", query);
			}
			else
			{
				m_botAction.sendSmartPrivateMessage(name, "Could not find that message.");
			}
		} catch(Exception e) { Tools.printStackTrace( e ); }
	}
	
	/** Marks a message's status as unread.
	 *  @param Name of player.
	 *  @param Message number being reset.
	 */
	public void setAsNew(String name, String message)
	{
		int messageNumber = -1;
		try{
			messageNumber = Integer.parseInt(message);
		} catch(Exception e) {
			Tools.printStackTrace( e );
			m_botAction.sendSmartPrivateMessage(name, "Invalid message number");
			return;
		}
		String query = "UPDATE tblMessageSystem SET fnRead = 0 WHERE fcName = \""+name.toLowerCase()+"\" AND fnID = " + messageNumber;
		try {
			m_botAction.SQLQuery("local", query);
			m_botAction.sendSmartPrivateMessage(name, "Message marked as unread.");
		} catch(SQLException e) {
			Tools.printStackTrace( e );
			m_botAction.sendSmartPrivateMessage(name, "Unable to mark as read.");
		}
	}
	
	/** Deletes a message from the database.
	 *  @param Name of player
	 *  @param Message being deleted.
	 */
	public void deleteMessage(String name, String message)
	{
		int messageNumber = -1;
		try{
			messageNumber = Integer.parseInt(message);
		} catch(Exception e) {
			e.printStackTrace();
			m_botAction.sendSmartPrivateMessage(name, "Invalid message number");
			return;
		}
		String query = "DELETE FROM tblMessageSystem WHERE fcName = \""+name.toLowerCase()+"\" AND fnID = " + messageNumber;
		try {
			m_botAction.SQLQuery("local", query);
			m_botAction.sendSmartPrivateMessage(name, "Message deleted.");
		} catch(Exception e) {
			m_botAction.sendSmartPrivateMessage(name, "Message unable to be deleted.");
			Tools.printStackTrace( e );
		}
	}
	
	/** PM's a player with all of his/her message numbers.
	 *  @param Name of player.
	 *  @param (does nothing).
	 */
	public void myMessages(String name, String message)
	{
		String query = "SELECT * FROM tblMessageSystem WHERE fcName = \""+name.toLowerCase()+"\"";
		String messageNumbers = "";
		m_botAction.sendSmartPrivateMessage(name, "You have the following messages: ");
		try {
			ResultSet results = m_botAction.SQLQuery("local", query);
			while(results.next())
			{
				String thisMessage = "Message #" + String.valueOf(results.getInt("fnID")) + ". Status: ";
				if(results.getInt("fnRead") == 1)
					thisMessage += " Read";
				else
					thisMessage += " Unread";

				m_botAction.sendSmartPrivateMessage(name, thisMessage);
			}
		} catch(Exception e) {
			m_botAction.sendSmartPrivateMessage(name, "Error while reading message database.");
			Tools.printStackTrace( e );
		}
		m_botAction.sendSmartPrivateMessage(name, "PM me with !read <num> to read a message.");
	}
	
	/** Sends the bot to a new arena.
	 *  @param Name of player.
	 *  @param Arena to go to.
	 */
	 public void handleGo(String name, String message)
	 {
	 	if(!m_botAction.getOperatorList().isHighmod(name))
	 		return;
	 	
	 	m_botAction.changeArena(message);
	 }
	
	/** Sets up the task that will delete messages that have expired.
	 */
	void deleteTask()
	{
		messageDeleteTask = new TimerTask()
		{
			public void run()
			{
			//	String query = "DELETE FROM tblMessageSystem WHERE (fdTimeStamp < DATE_SUB(NOW(), INTERVAL 31 DAY)) AND fnRead = 0";
			//	String query2 = "DELETE FROM tblMessageSystem WHERE (fdTimeStamp < DATE_SUB(NOW(), INTERVAL 15 DAY)) AND fnRead = 1";
			//	try {
			//		m_botAction.SQLQuery("local", query);
			//		m_botAction.SQLQuery("local", query2);
			//		System.out.println("Deleting messages.");
			//	} catch(SQLException e) { Tools.printStackTrace( e ); }
				System.out.println("fun....");
			}
		};
	}
}

class Channel
{
	String owner;
	String channelName;
	BotAction m_bA;
	HashMap members;
	HashSet banned;
	boolean isOpen;
	
	/** Constructs the new channel.
	 */
	public Channel(String creator, String cName, BotAction botAction, boolean pub, boolean auto)
	{
		m_bA = botAction;
		owner = creator;
		channelName = cName.toLowerCase();
		isOpen = pub;
		members = new HashMap();
		banned = new HashSet();
		members.put(owner.toLowerCase(), new Integer(3));
		updateSQL(owner.toLowerCase(), 3);
		
		if(!auto)
			m_bA.sendSmartPrivateMessage(owner, "Channel created.");
	}
	
	/** Returns wether the person is owner or not.
	 *  @param Name of player being checked.
	 */
	public boolean isOwner(String name)
	{
		if(!members.containsKey(name.toLowerCase()))
			return false;
		
		int level = ((Integer)members.get(name.toLowerCase())).intValue();
		if(level == 3) return true;
		else return false;
	}
	
	/** Returns whether the person is an operator or not.
	 *  @param Name of player being checked.
	 */
	public boolean isOp(String name)
	{
		if(!members.containsKey(name.toLowerCase()))
			return false;
		
		int level = ((Integer)members.get(name.toLowerCase())).intValue();
		if(level == 2) return true;
		else return false;
	}
	
	/** Returns wheter the channel is public or private.
	 *  @return
	 */
	public boolean isOpen()
	{
		return isOpen;
	}
	
	/** Gives channel ownership away
	 *  @param Name of owner.
	 *  @param Name of new owner.
	 */
	public void newOwner(String name, String player)
	{
		if(members.containsKey(player.toLowerCase()))
		{
			members.put(player.toLowerCase(), new Integer(3));
			updateSQL(name.toLowerCase(), 1);
			updateSQL(player.toLowerCase(), 3);
			try {
				m_bA.SQLQuery("local", "UPDATE tblChannel SET fcOwner = \""+player+"\" WHERE fcChannelName = \"" + channelName.toLowerCase() + "\"");
			} catch(Exception e) { Tools.printStackTrace( e ); }
			owner = player;
			m_bA.sendSmartPrivateMessage(player, "I have just left you an important message. PM me with !messages receive it.");
			leaveMessage(name, player, "You have been made the owner of " + channelName + " channel.");
		}
		else
			m_bA.sendSmartPrivateMessage(name, "This player is not in the channel.");
	}
	
	/** Adds an operator.
	 *  @param Name of owner.
	 *  @param Name of new operator.
	 */
	public void makeOp(String name, String player)
	{
		if(members.containsKey(player.toLowerCase()))
		{
			members.put(player.toLowerCase(), new Integer(2));
			updateSQL(player.toLowerCase(), 2);
			m_bA.sendSmartPrivateMessage(player, "I have just left you an important message. PM me with !messages receive it.");
			leaveMessage(name, player, "You have been made an operator in " + channelName + " channel.");
		}
		else
			m_bA.sendSmartPrivateMessage(name, "This player is not in the channel.");
	}
	
	/** Removes an operator.
	 *  @param Name of owner.
	 *  @param Name of new operator.
	 */
	public void deOp(String name, String player)
	{
		if(members.containsKey(player.toLowerCase()))
		{
			members.put(player.toLowerCase(), new Integer(1));
			updateSQL(player.toLowerCase(), 1);
			m_bA.sendSmartPrivateMessage(player, "I have just left you an important message. PM me with !messages receive it.");
			leaveMessage(name, player, "Your operator priveleges in " + channelName + " channel have been revoked.");
		}
		else
			m_bA.sendSmartPrivateMessage(name, "This player is not in the channel.");
	}
	
	/** Makes the channel private.
	 *  @param Name of owner.
	 */
	public void makePrivate(String name)
	{
		try {
			m_bA.SQLQuery("local", "UPDATE tblChannel SET fnPrivate = 1 WHERE fcChannelName = \"" + channelName.toLowerCase() + "\"");
		} catch(Exception e) { Tools.printStackTrace( e ); }
		m_bA.sendSmartPrivateMessage(name, "Now private channel.");
		isOpen = false;
	}
	
	/** Makes the channel public.
	 *  @param Name of owner.
	 */
	public void makePublic(String name)
	{
		try {
			m_bA.SQLQuery("local", "UPDATE tblChannel SET fnPrivate = 0 WHERE fcChannelName = \"" + channelName.toLowerCase() + "\"");
		} catch(Exception e) { Tools.printStackTrace( e ); }
		m_bA.sendSmartPrivateMessage(name, "Now public channel.");
		isOpen = true;
	}
	
	/** Sends a ?message to everyone on channel and pm's the players that
	 *  are online to tell them they got a message.
	 *  @param Name of operator.
	 *  @param Message being sent.
	 */
	public void messageChannel(String name, String message)
	{
		Iterator it = members.keySet().iterator();
		
		while(it.hasNext())
		{
			String player = (String)it.next();
			int level = ((Integer)members.get(player.toLowerCase())).intValue();
			if(level > 0)
			{
				leaveMessage(name, player, message);
				m_bA.sendSmartPrivateMessage(player, "I have just left you an important message. PM me with !messages receive it.");
			}
		}
	}
	
	/** PM's everyone on channel.
	 *  @param Name of operator.
	 *  @param Message being sent.
	 */
	public void announceToChannel(String name, String message)
	{
		Iterator it = members.keySet().iterator();
		
		while(it.hasNext())
		{
			String player = (String)it.next();
			int level = ((Integer)members.get(player.toLowerCase())).intValue();
			if(level > 0)
			{
				m_bA.sendSmartPrivateMessage(player, channelName + ": " + name + ">" + message);
			}
		}
	}
	
	/** Puts in request to join channel.
	 *  @param Name of player wanting to join.
	 */
	public void joinRequest(String name)
	{
		if(members.containsKey(name.toLowerCase()))
		{
			m_bA.sendSmartPrivateMessage(name, "You are already on this channel.");
			return;
		}
		if(banned.contains(name.toLowerCase()))
		{
			m_bA.sendSmartPrivateMessage(name, "You have been banned from this channel.");
			return;
		}
		if(isOpen)
		{
			members.put(name.toLowerCase(), new Integer(1));
			updateSQL(name.toLowerCase(), 1);
			m_bA.sendSmartPrivateMessage(name, "You have been accepted to " + channelName + " announcement channel.");
		}
		else
		{
			members.put(name.toLowerCase(), new Integer(0));
			updateSQL(name.toLowerCase(), 0);
			m_bA.sendSmartPrivateMessage(name, "You have been placed into the channel request list. The channel owner will make the decision.");
		}
	}
	
	/** Removes a player from the channel
	 *  @param Name of player leaving.
	 */
	public void leaveChannel(String name)
	{
		if(members.containsKey(name.toLowerCase()))
		{
			int level = ((Integer)members.get(name.toLowerCase())).intValue();
			if(level < 0)
			{
				m_bA.sendSmartPrivateMessage(name, "You are not on this channel.");
				return;
			}
			members.remove(name.toLowerCase());
			try {
				m_bA.SQLQuery("local", "DELETE FROM tblChannelUser WHERE fcName = \""+name.toLowerCase()+"\"");
			} catch(Exception e) { Tools.printStackTrace( e ); }
			m_bA.sendSmartPrivateMessage(name, "You have been removed from the channel.");
		}
		else
			m_bA.sendSmartPrivateMessage(name, "You are not on this channel.");
	}
	
	/** Lists all requests to join channel.
	 *  @param Name of operator.
	 */
	public void listRequests(String name)
	{
		Iterator it = members.keySet().iterator();
		
		m_bA.sendSmartPrivateMessage(name, "People requesting to join: ");
		
		while(it.hasNext())
		{
			String p = (String)it.next();
			int level = ((Integer)members.get(p)).intValue();
			if(level == 0)
				m_bA.sendSmartPrivateMessage(name, p);
		}
	}
	
	/** Accepts a player onto channel.
	 *  @param Name of operator.
	 *  @param Name of player being accepted.
	 */
	public void acceptPlayer(String name, String player)
	{
		if(members.containsKey(player.toLowerCase()))
		{
			members.put(player.toLowerCase(), new Integer(1));
			m_bA.sendSmartPrivateMessage(player, "I have just left you an important message. PM me with !messages receive it.");
			leaveMessage(name, player, "You have been accepted into " + channelName + " channel.");
			m_bA.sendSmartPrivateMessage(name, player + " accepted.");
		}
		else
			m_bA.sendSmartPrivateMessage(name, "This player has not requested to join.");
	}
	
	/** Rejects a player's request.
	 *  @param Name of operator.
	 *  @param Name of player being rejected.
	 */
	public void rejectPlayer(String name, String player)
	{
		if(members.containsKey(player.toLowerCase()))
		{
			members.remove(player.toLowerCase());
			updateSQL(player, -5);
			m_bA.sendSmartPrivateMessage(player, "I have just left you an important message. PM me with !messages receive it.");
			leaveMessage(name, player, "You have been rejected from " + channelName + " channel.");
			m_bA.sendSmartPrivateMessage(name, player + " rejected.");
		}
		else
			m_bA.sendSmartPrivateMessage(name, "This player is not on the channel.");
	}
	
	/** Bans a player from the channel.
	 *  @param Name of operator
	 *  @param Name of player getting banned.
	 */
	public void banPlayer(String name, String player)
	{
		if(banned.contains(player.toLowerCase()))
		{
			m_bA.sendSmartPrivateMessage(name, "That player is already banned.");
			return;
		}
		else
			banned.add(player.toLowerCase());
		if(members.containsKey(player.toLowerCase()))
		{
			int level = ((Integer)members.get(player.toLowerCase())).intValue();
			level *= -1;
			updateSQL(player, level);
			members.put(player.toLowerCase(), new Integer(level));
		}
	}
	
	/** Unbans a player.
	 *  @param Name of operator.
	 *  @param Name of player being unbanned.
	 */
	public void unbanPlayer(String name, String player)
	{
		if(banned.contains(player.toLowerCase()))
			banned.remove(player.toLowerCase());
		else
		{
			m_bA.sendSmartPrivateMessage(name, "That player is not banned.");
			return;
		}
		if(members.containsKey(player.toLowerCase()))
		{
			int level = ((Integer)members.get(player.toLowerCase())).intValue();
			level *= -1;
			updateSQL(player, level);
			members.put(player.toLowerCase(), new Integer(level));
		}
	}			
	
	/** Updates the database when a player's level of access changes.
	 *  @param Player being changed.
	 *  @param New level of access.
	 */
	public void updateSQL(String player, int level)
	{
		String query = "SELECT * FROM tblChannelUser WHERE fcName = \"" + player.toLowerCase()+"\" AND fcChannel = \""+channelName+"\"";
		
		
		try {
			ResultSet results = m_bA.SQLQuery("local", query);
			if(results.next())
			{
				if(level != -5)
					query = "UPDATE tblChannelUser SET fnLevel = " + level + " WHERE fcName = \"" + player.toLowerCase() + "\" AND fcChannel = \"" + channelName +"\"";
				else
					query = "DELETE FROM tblChannelUser WHERE fcName = \"" + player.toLowerCase()+"\"";
				m_bA.SQLQuery("local", query);
			}
			else
			{
				query = "INSERT INTO tblChannelUser (fcChannel, fcName, fnLevel) VALUES (\"" + channelName + "\", \"" + player.toLowerCase() + "\", " + level + ")";
				m_bA.SQLQuery("local", query);
			}
		} catch(SQLException sqle) { Tools.printStackTrace( sqle ); }
	}
	
	/** Leaves a message from for a player in the database.
	 *  @param Name of player leaving the message.
	 *  @param Name of player recieving the message.
	 *  @param Message being left.
	 */
	public void leaveMessage(String name, String player, String message)
	{
		String query = "INSERT INTO tblMessageSystem (fnID, fcName, fcMessage, fnRead, fdTimeStamp) VALUES (0, \""+player.toLowerCase()+"\", \""+name + ": " + message+"\", 0, NOW())";
		try {
			m_bA.SQLQuery("local", query);
		} catch(SQLException sqle) { Tools.printStackTrace( sqle ); }
	}
	
	/** Reload is called when the bot respawns. Allows channel to resume from where it was before bot went offline.
	 */
	public void reload()
	{
		String query = "SELECT * FROM tblChannelUser WHERE fcChannel = \"" + channelName.toLowerCase()+"\"";
		
		try {
			ResultSet results = m_bA.SQLQuery("local", query);
			while(results.next())
			{
				String name = results.getString("fcName");
				int level = results.getInt("fnLevel");
				members.put(name.toLowerCase(), new Integer(level));
				if(level < 0)
					banned.add(name.toLowerCase());
			}
		} catch(Exception e) { Tools.printStackTrace( e ); }
	}
	
	/** Lists all players on channel.
	 *  @param Name of player.
	 */
	public void listMembers(String name)
	{
		Iterator it = members.keySet().iterator();
		String message = "";
		m_bA.sendSmartPrivateMessage(name, "List of players on " + channelName + ": ");
		for(int k = 0;it.hasNext();)
		{
			String pName = (String)it.next();
			if(k % 10 != 0)
				message += ", ";
			
			if(isOwner(pName))
				message += pName + " (Owner)";
			else if(isOp(pName))
				message += pName + " (Op)";
			else
				message += pName;
			k++;
			if(k % 10 == 0)
			{
				m_bA.sendSmartPrivateMessage(name, message);
				message = "";
			}
		}
	}
	
	/** Lists all players banned from channel.
	 *  @param Name of player.
	 */
	public void listBanned(String name)
	{
		Iterator it = members.keySet().iterator();
		String message = "";
		m_bA.sendSmartPrivateMessage(name, "List of players banned from " + channelName + ": ");
		for(int k = 0;it.hasNext();)
		{
			String pName = (String)it.next();
			if(k % 10 != 0)
				message += ", ";
			
			message += pName;
			k++;
			if(k % 10 == 0)
			{
				m_bA.sendSmartPrivateMessage(name, message);
				message = "";
			}
		}
	}
	
}