package twcore.bots.messagebot;

import twcore.core.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.net.*;
import java.io.*;
import com.wilko.jaim.*;

/** Bot to host "channels" that allow a player to ?message or pm
 *  everyone on the channel so that information can be spread easily.
 *
 *  @author Ikrit
 *  @version 1.8
 *  
 *  Added database support of messages because I forgot SSC messaging only
 *  allows one message from a person at a time.
 *  
 *  Added pubbot support so the bot can PM a player that has just logged
 *  in to tell them if they have messages.
 *  
 *  Fixed possible SQL injection attacks (cough FINALLY cough).
 *  
 *  Added support so the bot will sync with the website.
 *
 *  Added a thing for default channels so you don't have to do !command channel:blahblahblah all the time
 *
 *  Added a buddy list thing for my planned AIM stuff.
 *
 *  Added AOL Instant Messanger support.
 *
 *  Added a news feature for the lobby thing that qan is designing. Highmod+ can add news messages that get arena'd every 90 sec's.
 *
 *  Added all new commands to !help thus making !help a 41 PM long help message.
 */
public class messagebot extends SubspaceBot implements JaimEventListener
{
	HashMap channels;
	HashMap defaultChannel;
	HashMap buddyLists;
	HashMap aimLogins;
	HashMap aimToTW;
	HashSet ops;
	HashMap news;
	ArrayList newsIDs;
	int newsID;
	CommandInterpreter m_CI;
	TimerTask messageDeleteTask, messageBotSync, aimReconnect, newsTask;
	public static final String IPCCHANNEL = "messages";
	JaimConnection aimConnection;
	boolean aimOn = false;
	boolean bug = false;
	
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
		defaultChannel = new HashMap();
		buddyLists = new HashMap();
		aimLogins = new HashMap();
		aimToTW = new HashMap();
		ops = new HashSet();
		news = new HashMap();
		newsIDs = new ArrayList();
		newsID = 0;
		m_CI = new CommandInterpreter(m_botAction);
		registerCommands();
		createTasks();
		m_botAction.scheduleTaskAtFixedRate(aimReconnect, 1, 60 * 1000);
		m_botAction.scheduleTaskAtFixedRate(messageDeleteTask, 30 * 60 * 1000, 30 * 60 * 1000);
		m_botAction.scheduleTaskAtFixedRate(messageBotSync, 2 * 60 * 1000, 2 * 60 * 1000);
		m_botAction.scheduleTaskAtFixedRate(newsTask, 90 * 1000, 90 * 1000);
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
		String query = "SELECT * FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(name)+"' and fnRead = 0";
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
			results.close();
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
        m_CI.registerCommand( "!join",       acceptedMessages, this, "joinChannel" ); 
        m_CI.registerCommand( "!quit",       acceptedMessages, this, "quitChannel" ); 
        m_CI.registerCommand( "!help",       acceptedMessages, this, "doHelp" ); 
        m_CI.registerCommand( "!accept",     acceptedMessages, this, "acceptPlayer" ); 
        m_CI.registerCommand( "!decline",    acceptedMessages, this, "declinePlayer" ); 
        m_CI.registerCommand( "!announce",   acceptedMessages, this, "announceToChannel" ); 
        m_CI.registerCommand( "!message",    acceptedMessages, this, "messageChannel" ); 
        m_CI.registerCommand( "!requests",   acceptedMessages, this, "listRequests"); 
        m_CI.registerCommand( "!ban",        acceptedMessages, this, "banPlayer"); 
        m_CI.registerCommand( "!unban",		 acceptedMessages, this, "unbanPlayer"); 
        m_CI.registerCommand( "!makeop",	 acceptedMessages, this, "makeOp"); 
        m_CI.registerCommand( "!deop", 	     acceptedMessages, this, "deOp"); 
        m_CI.registerCommand( "!owner",		 acceptedMessages, this, "sayOwner"); 
        m_CI.registerCommand( "!grant",		 acceptedMessages, this, "grantChannel"); 
        m_CI.registerCommand( "!private",	 acceptedMessages, this, "makePrivate"); 
        m_CI.registerCommand( "!public",	 acceptedMessages, this, "makePublic"); 
        m_CI.registerCommand( "!unread",	 acceptedMessages, this, "setAsNew");
        m_CI.registerCommand( "!read",		 acceptedMessages, this, "readMessage");
        m_CI.registerCommand( "!delete",	 acceptedMessages, this, "deleteMessage");
        m_CI.registerCommand( "!messages",	 acceptedMessages, this, "myMessages");
        m_CI.registerCommand( "!go",		 acceptedMessages, this, "handleGo");
        m_CI.registerCommand( "!members",	 acceptedMessages, this, "listMembers");
        m_CI.registerCommand( "!banned",	 acceptedMessages, this, "listBanned");
        m_CI.registerCommand( "!me",		 acceptedMessages, this, "myChannels");
        m_CI.registerCommand( "!mtfbwy",	 acceptedMessages, this, "handleDie");
        m_CI.registerCommand( "!login",		 acceptedMessages, this, "playerLogin");
        m_CI.registerCommand( "!setdefault", acceptedMessages, this, "setPlayerDefaultChannel");
        m_CI.registerCommand( "!default",	 acceptedMessages, this, "tellPlayerDefaultChannel");
        m_CI.registerCommand( "!register",	 acceptedMessages, this, "registerName");
        m_CI.registerCommand( "!aim",		 acceptedMessages, this, "aimMessage");
        m_CI.registerCommand( "!add",		 acceptedMessages, this, "addBuddy");
        m_CI.registerCommand( "!remove",	 acceptedMessages, this, "removeBuddy");
        m_CI.registerCommand( "!buddylist",	 acceptedMessages, this, "buddyList");
        m_CI.registerCommand( "!addnews",	 acceptedMessages, this, "addNewsItem");
        m_CI.registerCommand( "!delnews",	 acceptedMessages, this, "deleteNewsItem");
        m_CI.registerCommand( "!setaimname", acceptedMessages, this, "setAIMName");
        m_CI.registerCommand( "!bug",		 acceptedMessages, this, "bugMe");
        m_CI.registerCommand( "!debug",		 acceptedMessages, this, "stopBuggingMe");
        
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
    	
    	String query = "INSERT INTO tblChannel (fcChannelName, fcOwner, fnPrivate) VALUES('"+Tools.addSlashesToString(message.toLowerCase())+"', '"+Tools.addSlashesToString(name.toLowerCase())+"', 0)";
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
    	String channel = getChannel(name, message, true).toLowerCase();
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOwner(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
    	{
    		String query = "DELETE FROM tblChannel WHERE fcChannelName = '" + Tools.addSlashesToString(channel) + "'";
    		String query2 = "DELETE FROM tblChannelUser WHERE fcChannel = '" + Tools.addSlashesToString(channel) + "'";
    		try {
    			m_botAction.SQLQuery("local", query);
    			m_botAction.SQLQuery("local", query2);
    		} catch(SQLException sqle) { Tools.printStackTrace( sqle ); }
    		m_botAction.sendSmartPrivateMessage(name, "Channel deleted.");
    		c.messageChannel(name, "Channel " + channel + " deleted.");
    		channels.remove(channel);
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
    	String channel = getChannel(name, message, true).toLowerCase();
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	c.joinRequest(name);    	
    }
    
    /** Allows a person to quit a channel.
     *  @param Name of player.
     *  @param Name of channel being quit.
     */
    public void quitChannel(String name, String message)
    {
    	String channel = getChannel(name, message, true).toLowerCase();
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
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
    	String channel = getChannel(name, message, false).toLowerCase();
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, false).toLowerCase();
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, false).toLowerCase();
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, false).toLowerCase();
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, true).toLowerCase();
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, false).toLowerCase();
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, false).toLowerCase();
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, true).toLowerCase();
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, true).toLowerCase();
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, false).toLowerCase();
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOwner(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, false).toLowerCase();
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(!c.isOp(name))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That player is not an operator.");
    		return;
    	}
    	
    	if(c.isOwner(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, false).toLowerCase();
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(pieces[0].toLowerCase());
    	if(c.isOwner(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, true).toLowerCase();
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	m_botAction.sendSmartPrivateMessage(name, "Owner: " + c.owner);
    }
    
    /** Makes a channel public.
     *  @param Name of operator
     *  @param Name of channel
     */
    public void makePublic(String name, String message)
    {
    	String channel = getChannel(name, message, true).toLowerCase();
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String channel = getChannel(name, message, true).toLowerCase();
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = (Channel)channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
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
    	String query = "SELECT * FROM tblChannelUser WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"'";
    	try {
    		ResultSet results = m_botAction.SQLQuery("local", query);
    		while(results.next())
    		{
    			String channel = results.getString("fcChannel");
    			channel = channel.toLowerCase();
    			Channel c = (Channel)channels.get(channel);
    			if( c == null)
    			    return;
    			if(c.isOwner(name.toLowerCase()))
    				channel += ": Owner.";
    			else if(c.isOp(name.toLowerCase()))
    				channel += ": Operator.";
    			else
    				channel += ": Member.";
    			m_botAction.sendSmartPrivateMessage(name, channel);
    		}
    		results.close();
    	} catch(Exception e) { Tools.printStackTrace(e); }
    }
    			
    
    /** Sends help messages
     *  @param Name of player.
     */
    public void doHelp(String name, String message)
    {
    	m_botAction.sendSmartPrivateMessage(name, "Messaging system commands:");
        m_botAction.sendSmartPrivateMessage(name, "    !join <channel>                -Puts in request to join <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !quit <channel>                -Removes you from <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !owner <channel>               -Tells you who owns <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !unread <num>                  -Sets message <num> as unread.");
        m_botAction.sendSmartPrivateMessage(name, "    !read <num>                    -PM's you message <num>.");
        m_botAction.sendSmartPrivateMessage(name, "    !delete <num>                  -Deletes message <num>.");
        m_botAction.sendSmartPrivateMessage(name, "    !messages                      -PM's you all your message numbers.");
        m_botAction.sendSmartPrivateMessage(name, "    !me                            -Tells you what channels you have joined.");
        m_botAction.sendSmartPrivateMessage(name, "    !setdefault <channel>          -Sets your default channel.");
        m_botAction.sendSmartPrivateMessage(name, "    !default                       -Tells you what your default channel is.");
        m_botAction.sendSmartPrivateMessage(name, "    !announce <channel>:<message>  -Sends everyone on <channel> a pm of <message>.");
        m_botAction.sendSmartPrivateMessage(name, "    !message <channel>:<message>   -Leaves a message for everyone on <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !requests <channel>            -PM's you with all the requests to join <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !banned <channel>              -Lists players banned on <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !members <channel>             -Lists all members on <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !ban <channel>:<name>          -Bans <name> from <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !unban <channel>:<name>        -Lifts <name>'s ban from <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !makeop <channel>:<name>       -Makes <name> an operator in <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !deop <channel>:<name>         -Revokes <name>'s operator status in <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !grant <channel>:<name>        -Grants ownership of <channel> to <name>.");
        m_botAction.sendSmartPrivateMessage(name, "    !private <channel>             -Makes <channel> a request based channel.");
        m_botAction.sendSmartPrivateMessage(name, "    !public <channel>              -Makes <channel> open to everyone.");
        m_botAction.sendSmartPrivateMessage(name, "    !destroy <channel>             -Destroys <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !accept <channel>:<name>       -Accepts <name>'s request to join <channel>.");
        m_botAction.sendSmartPrivateMessage(name, "    !decline <channel>:<name>      -Declines <name>'s request to join <channel>.");
	    if(m_botAction.getOperatorList().isZH(name))
	       	m_botAction.sendSmartPrivateMessage(name, "    !create <channel>              -Creates a channel with the name <channel>.");
		m_botAction.sendSmartPrivateMessage(name, " ");
    	m_botAction.sendSmartPrivateMessage(name, "AIM interface commands:");
        m_botAction.sendSmartPrivateMessage(name, "    !register <name>               -Registers you for a buddy list and sets your AIM name as <name>.");
        m_botAction.sendSmartPrivateMessage(name, "    !aim <player>:<message>        -Sends <message> to <player>'s AIM screen name.");
        m_botAction.sendSmartPrivateMessage(name, "    !add <name>                    -Adds <name> to your buddy list.");
        m_botAction.sendSmartPrivateMessage(name, "    !remove <name>                 -Removes <name> from your buddy list.");
        m_botAction.sendSmartPrivateMessage(name, "    !buddylist                     -PM's you your buddy list.");
        m_botAction.sendSmartPrivateMessage(name, "    !setaimname <name>             -Sets your AIM name to <name>.");
        m_botAction.sendSmartPrivateMessage(name, " ");
        m_botAction.sendSmartPrivateMessage(name, "!help                          -Sends you this help message.");
	    if(m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase())) {
	    	m_botAction.sendSmartPrivateMessage(name, "Highmod+ commands:");
	        m_botAction.sendSmartPrivateMessage(name, "    !addnews <news>:<url>          -Adds a news article with <news> as the content and <url> for more info.");
	        m_botAction.sendSmartPrivateMessage(name, "    !delnews <#>                   -Deletes news id number <#>."); 
	        m_botAction.sendSmartPrivateMessage(name, "    !go <arena>                    -Sends messagebot to <arena>.");
        }       
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
		try {
			m_botAction.joinArena(m_botAction.getBotSettings().getString("Default arena"));
			m_botAction.ipcSubscribe(IPCCHANNEL);
			
			String opList[] = (m_botAction.getBotSettings().getString("Ops")).split(":");
			for(int k = 0;k < opList.length;k++)
				ops.add(opList[k].toLowerCase());
			
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
				results.close();
			} catch(Exception e) { Tools.printStackTrace( e ); }
			
			query = "SELECT * FROM tblAIMUser";
			
			try {
				ResultSet results = m_botAction.SQLQuery("local", query);
				
				HashMap nameID = new HashMap();
				
				while(results.next())
				{
					nameID.put(results.getString("fcName"), results.getInt("fnID"));
				}
				
				HashMap idBuddyList = new HashMap();		
				
				query = "SELECT * FROM tblBuddyList";
				results = m_botAction.SQLQuery("local", query);
				
				while(results.next())
				{
					String buddyName = results.getString("fcBuddyName");
					int pID = results.getInt("fnID");
					if(idBuddyList.containsKey(pID)) {
						HashSet buddies = (HashSet)idBuddyList.get(pID);
						buddies.add(buddyName);
						idBuddyList.put(pID, buddies);
					} else {
						HashSet buddies = new HashSet();
						buddies.add(buddyName);
						idBuddyList.put(pID, buddies);
					}
				}
				
				Iterator it = nameID.keySet().iterator();
				while(it.hasNext())
				{
					String name = (String)it.next();
					int id = (Integer)nameID.get(name);
					HashSet buddies = (HashSet)idBuddyList.get(id);
					if(buddies == null) buddies = new HashSet();
					buddyLists.put(name, new BuddyList(name, id, buddies, m_botAction));
				}
			} catch(SQLException e) { Tools.printStackTrace(e); }
			
			query = "SELECT * FROM tblBotNews ORDER BY fnID DESC";
			try {
				ResultSet results = m_botAction.SQLQuery("local", query);
				while(results.next()) {
					String name = results.getString("fcName");
					String content = results.getString("fcNews");
					String date = results.getString("fdTime");
					String url = results.getString("fcURL");
					int id = results.getInt("fnID");
					NewsArticle na = new NewsArticle(content, name, date, id, url);
					news.put(id, na);
					newsIDs.add(id);
				}
				results.close();
			} catch(SQLException e) { Tools.printStackTrace(e); }
					
		} catch(Exception e) {}
		
//		aimLogins.put("olos necaj", "ikrit <er>");
//		HashSet h = new HashSet(); h.add("ikrit <er>"); buddyLists.put("ikrit <er>", new BuddyList("ikrit <er>", 1, h, m_botAction));
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
		if(!ownsMessage(name.toLowerCase(), messageNumber)) {
			m_botAction.sendSmartPrivateMessage(name, "That is not your message!");
			return;
		}
		String query = "SELECT * FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"' AND fnID = " + messageNumber;
		try{
			ResultSet results = m_botAction.SQLQuery("local", query);
			if(results.next())
			{
				message = results.getString("fcMessage");
				String timestamp = results.getString("fdTimeStamp");
				
				m_botAction.sendSmartPrivateMessage(name, timestamp + " " + message);
				
				query = "UPDATE tblMessageSystem SET fnRead = 1 WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"' AND fnID = " + messageNumber;
				m_botAction.SQLQuery("local", query);
			}
			else
			{
				m_botAction.sendSmartPrivateMessage(name, "Could not find that message.");
			}
			results.close();
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
			m_botAction.sendSmartPrivateMessage(name, "Invalid message number");
			return;
		}
		if(!ownsMessage(name.toLowerCase(), messageNumber)) {
			m_botAction.sendSmartPrivateMessage(name, "That is not your message!");
			return;
		}
		String query = "UPDATE tblMessageSystem SET fnRead = 0 WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"' AND fnID = " + messageNumber;
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
		if(!ownsMessage(name.toLowerCase(), messageNumber)) {
			m_botAction.sendSmartPrivateMessage(name, "That is not your message!");
			return;
		}
		String query = "DELETE FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"' AND fnID = " + messageNumber;
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
		String query = "SELECT * FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"'";
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
			results.close();
		} catch(Exception e) {
			m_botAction.sendSmartPrivateMessage(name, "Error while reading message database.");
			Tools.printStackTrace( e );
		}
		m_botAction.sendSmartPrivateMessage(name, "PM me with !read <num> to read a message.");
	}
	
	/** Checks the database to make sure a player owns the
	 *  message that he's trying to do stuff with.
	 *  @param name Name of the player
	 *  @param messageNumber Number of the message he/she is
	 *         trying to do stuff to
	 */
	 public boolean ownsMessage(String name, int messageNumber)
	 {
	 	String query = "SELECT * FROM tblMessageSystem WHERE fnID = " + messageNumber;
	 	try {
	 		ResultSet results = m_botAction.SQLQuery("local", query);
	 		
	 		if(!results.next()) { results.close(); return false; }
	 		
	 		if(results.getString("fcName").toLowerCase().equals(name)) {
	 			results.close();
	 			return true;
	 		} else {
	 			results.close();
	 			return false;
	 		}
	 	} catch(Exception e) { Tools.printStackTrace(e); }
	 	return false;
	 }
	 
	/** Sets the default channel for a player.
	 *  @param name Name of player
	 *  @param channel Channel to be set as default.
	 */
	 public void setPlayerDefaultChannel(String name, String channel) {
	 	defaultChannel.put(name.toLowerCase(), channel.toLowerCase());
	 	m_botAction.sendSmartPrivateMessage(name, "Default channel set to: " + channel.toLowerCase());
	 }
	 
	/** Tells the player what their default is set to.
	 *  @param name Name of the player
	 *  @param empty ""
	 */
	 public void tellPlayerDefaultChannel(String name, String empty) {
	 	if(defaultChannel.containsKey(name.toLowerCase()))
	 		m_botAction.sendSmartPrivateMessage(name, "Default channel: " + defaultChannel.get(name.toLowerCase()));
	 	else
	 		m_botAction.sendSmartPrivateMessage(name, "You have not set your default channel.");
	 }
	 
	 /** Registers a name for the AIM stuff.
	  *  @param name Name of player
	  *  @param empty ""
	  */
	  public void registerName(String name, String aimName) {
	  	if(buddyLists.containsKey(name.toLowerCase())) {
	  		m_botAction.sendSmartPrivateMessage(name, "You have already registered for this.");
	  		return;
	  	} else {
	  		try {
		  		m_botAction.SQLQuery("local", "INSERT INTO tblAIMUser (fnID, fcName, fcAIMName) VALUES (0, '"+Tools.addSlashesToString(name.toLowerCase())+"', '"+Tools.addSlashesToString(aimName.toLowerCase())+"')");
		  		int id = m_botAction.SQLQuery("local", "SELECT fnID FROM tblAIMUser WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"'").getInt("fnID");
		  		m_botAction.sendSmartPrivateMessage(name, "Registration complete, you may use the AIM name " + aimName + " to send PMs with me.");
		  		buddyLists.put(name.toLowerCase(), new BuddyList(name.toLowerCase(), id, new HashSet(), m_botAction));
		  		aimLogins.put(name.toLowerCase(), aimName.toLowerCase());
		  		aimToTW.put(aimName.toLowerCase(), name.toLowerCase());
		  	} catch(Exception e) { }
		}
	}
	
	/** Sets AIM name.
	 *  @param name Name of player
	 *  @param AIM name.
	 */
	 public void setAIMName(String name, String aimName) {
	 	String query = "UPDATE tblAIMUser SET fcAIMName = '"+Tools.addSlashesToString(aimName.toLowerCase())+"' WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"'";
	 	try {
	 		m_botAction.SQLQuery("local", query);
	 		aimLogins.put(name.toLowerCase(), aimName.toLowerCase());
	 		aimToTW.put(aimName.toLowerCase(), name.toLowerCase());
	 		m_botAction.sendSmartPrivateMessage(name, "AIM name set to: " + aimName);
	 	} catch(Exception e) { m_botAction.sendSmartPrivateMessage(name, "Update failed."); }
	 }
	
	/** Adds a name to buddy list.
	 *  @param name Name of player
	 *  @param buddy Name of buddy
	 */
	 public void addBuddy(String name, String buddy) {
	 	if(!buddyLists.containsKey(name.toLowerCase())) {
	 		m_botAction.sendSmartPrivateMessage(name, "You have not registered for a buddy list yet.");
	 		return;
	 	}
	 	
	 	BuddyList bList = (BuddyList)buddyLists.get(name.toLowerCase());
	 	bList.addBuddy(buddy);
	 }
	 
	/** Removes a name from buddy list.
	 *  @param name Name of player
	 *  @param buddy Name of buddy
	 */
	 public void removeBuddy(String name, String buddy) {
	 	if(!buddyLists.containsKey(name.toLowerCase())) {
	 		m_botAction.sendSmartPrivateMessage(name, "You have not registered for a buddy list yet.");
	 		return;
	 	}
	 	
	 	BuddyList bList = (BuddyList)buddyLists.get(name.toLowerCase());
	 	bList.removeBuddy(buddy);
	 }
	 
	/** Lists a player's buddy list.
	 *  @param name Name of player
	 *  @param empty ""
	 */
	 public void buddyList(String name, String empty) {
	 	if(!buddyLists.containsKey(name.toLowerCase())) {
	 		m_botAction.sendSmartPrivateMessage(name, "You have not registered for a buddy list yet.");
	 		return;
	 	}
	 	
	 	BuddyList bList = (BuddyList)buddyLists.get(name.toLowerCase());
	 	bList.listBuddies();
	 }
	 
	/** Sends aim message.
	 *  @param name Name of player
	 *  @param message Player that's getting message and message
	 */
	 public void aimMessage(String name, String message) {
	 	if(!buddyLists.containsKey(name.toLowerCase())) {
	 		m_botAction.sendSmartPrivateMessage(name, "You have not registered for a buddy list yet.");
	 		return;
	 	}
	 	if(!aimOn) {
	 		m_botAction.sendSmartPrivateMessage(name, "The bot cannot sign on to AIM right now, please try again later.");
	 		return;
	 	}
	 	
	 	String pieces[] = message.split(":", 2);
	 	String aimName = (String)aimLogins.get(pieces[0].toLowerCase());
	 	if(aimName == null) return;
	 	
	 	if(bothBuddies(name, pieces[0]))
	 		try {
	 			aimConnection.sendIM(aimName, addSpecialChars(name + "> " + pieces[1]));
	 		} catch(Exception e) { }
	 	else
	 		m_botAction.sendSmartPrivateMessage(name, "You must be buddies with the player before you can send a message.");
	 }
	 			
	 
	/** Retrieves the channel name out of the message.
	 *  @param name Name of player.
	 *  @param message Message sent
	 */
	 public String getChannel(String name, String message, boolean noParams) {
	 	if(noParams) {
	 		if(message.length() == 0 && defaultChannel.containsKey(name.toLowerCase()))
	 			return (String)defaultChannel.get(name.toLowerCase());
	 		else
	 			return message;
	 	} else {
		 	if(message.indexOf(":") == -1) {
		 		if(defaultChannel.containsKey(name.toLowerCase()))
		 			return (String)defaultChannel.get(name.toLowerCase());
		 		else
		 			return "";
		 	} else {
		 		String pieces[] = message.split(":");
		 		return pieces[0];
		 	}
		}
	 }
	 
	/** Handles a message sent by pubbots to tell a player if they have new messages
	 *  @param Name of player PM'ing
	 *  @param Name of player logging in
	 */
	 public void playerLogin(String name, String player)
	 {
	 	if(m_botAction.getOperatorList().isSysop(name))
	 		checkNewMessages(player.toLowerCase());
	 }
	
	/** Sends the bot to a new arena.
	 *  @param Name of player.
	 *  @param Arena to go to.
	 */
	 public void handleGo(String name, String message)
	 {
	 	if(m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
	 		m_botAction.changeArena(message);
	 }
	 
	/** Kills the bot
	 *  @param Name of player
	 *  @param should be blank
	 */
	 public void handleDie(String name, String message)
	 {
	 	if(m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase())) {
	 		try {
		 		aimConnection.logOut();
		 		aimConnection.disconnect();
			 	m_botAction.die();
			} catch(Exception e) { }
		}
	 }
	 
	 /** Announces to a channel that they have recieved a new message.
	  *  @param Name of channel
	  */
	 public Set messageSentFromWebsite(String channel)
	 {
	 	Set s = null;
	 	
	 	try {
		 	Channel c = (Channel)channels.get(channel.toLowerCase());
		 	s = c.members.keySet();
		 	s.removeAll(c.banned);
		} catch(Exception e) {  Tools.printStackTrace( e ); }
		
		return s;
	 }
	 
	 /** Syncs the website and MessageBot's access levels
	  *  @param Name of channel
	  *  @param Name of player
	  *  @param Access level
	  */
	 public void accessUpdateFromWebsite(String channel, String name, int level)
	 {
	 	try {
	 		Channel c = (Channel)channels.get(channel.toLowerCase());
	 		c.updateAccess(name, level);
	 	} catch(Exception e) { Tools.printStackTrace( e ); }
	 }
	
	/** Sets up the task that will delete messages that have expired.
	 */
	void createTasks()
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
		
		messageBotSync = new TimerTask()
		{
			public void run()
			{
				HashSet peopleToTell = new HashSet();
				
				String query = "SELECT * FROM tblMessageToBot ORDER BY fnID ASC";
				String query2 = "DELETE FROM tblMessageToBot";
				try {
					ResultSet results = m_botAction.SQLQuery("local", query);
					while(results.next()) {
						String event = results.getString("fcSyncData");
						String pieces[] = event.split(":");
						if(pieces.length == 2)
							peopleToTell.addAll(messageSentFromWebsite(pieces[1]));
						else
							accessUpdateFromWebsite(pieces[2], pieces[1], Integer.parseInt(pieces[3]));
					}
					m_botAction.SQLQuery("local", query2);
				} catch(Exception e) { Tools.printStackTrace( e ); }
				
				Iterator it = peopleToTell.iterator();
				while(it.hasNext()) {
					String player = (String)it.next();
					m_botAction.sendSmartPrivateMessage(player, "You have received a new message. PM me with !messages to read it.");
				}
			}
		};
		
		aimReconnect = new TimerTask() {
        	public void run() {
        		setupAIM();
        	}
        };
        
        newsTask = new TimerTask() {
        	public void run() {
        		nextNews();
        	}
        };
	}
		
	/** Sets up all the stuff for AIM.
	 */
	public void setupAIM()
	{
		System.out.println("Trying to connect...");
		try {
			aimConnection = new JaimConnection("toc.oscar.aol.com",9898);
			aimConnection.setDebug(false);               // Send debugging to standard output
	        aimConnection.connect();
	            
	        aimConnection.addEventListener(this);
	        aimConnection.watchBuddy("unknownbuddy1212");         // Must watch at least one buddy or you will not appear on buddy listings
	            
	        aimConnection.logIn("TW MessageBot","hogwarts",50000);
	        aimConnection.addBlock("");     // Set Deny None
	            
	        aimConnection.setInfo("This buddy is using <a href=\"http://jaimlib.sourceforge.net\">Jaim</a>.");
	     } catch(Exception e) { e.printStackTrace(); }
	}
		
	/** Receive an event and process it according to its content
     *@param event - The JaimEvent to be processed
     */
    public void receiveEvent(JaimEvent event) {
        TocResponse tr=event.getTocResponse();
        String responseType=tr.getResponseType();
        if (responseType.equalsIgnoreCase(IMTocResponse.RESPONSE_TYPE)) {
        	receiveIM((IMTocResponse)tr);
        } else if (responseType.equalsIgnoreCase(ConfigTocResponse.RESPONSE_TYPE)) {
        	receiveConfig();
    	} else if (responseType.equalsIgnoreCase(LoginCompleteTocResponse.RESPONSE_TYPE)) {
    		aimReconnect.cancel();
    		aimOn = true;
        } else if (responseType.equalsIgnoreCase(ConnectionLostTocResponse.RESPONSE_TYPE)) {
        	aimReconnect = new TimerTask() {
        		public void run() {
        			setupAIM();
        		}
        	};
        	m_botAction.scheduleTaskAtFixedRate(aimReconnect, 60 * 1000, 60 * 1000);
        	aimOn = false;
        } else {
        	System.out.println(tr.toString());
        }
    }
    
    /** AIM stuff...
     */
    public void receiveConfig() {
        System.out.println("Config is now valid.");
        
        try {
            Iterator it= aimConnection.getGroups().iterator();
            while (it.hasNext()) {
                Group g=(Group)it.next();
                System.out.println("Group: "+g.getName());
                Enumeration e=g.enumerateBuddies();
                while (e.hasMoreElements()) {
                    Buddy b =(Buddy)e.nextElement();
                    b.setDeny(false);
                    b.setPermit(false);
                    aimConnection.watchBuddy(b.getName());
                    if (b.getDeny()) {
                        aimConnection.addBlock(b.getName());
                    }
                    if (b.getPermit()) {
                        aimConnection.addPermit(b.getName());
                    }
                }
            }
            aimConnection.saveConfig();
        }
        catch (Exception je) {
            je.printStackTrace();
        }
    }
    
    /** Receives an IM.
     *  @param im Response thing.
     */
     public void receiveIM(IMTocResponse im) {
     	String player = im.getFrom();
     	String message = removeSpecialChars(Utils.stripHTML(im.getMsg()));
     	System.out.println(im.getMsg());
     	try {
	     	if(aimToTW.containsKey(player.toLowerCase())) {
	     		String name = (String)aimToTW.get(player.toLowerCase());
	     		if(message.toLowerCase().startsWith("!add ")) {
	     			BuddyList bList = (BuddyList)buddyLists.get(name);
	     			String pieces[] = message.split(" ", 2);
	     			if(bList.addBuddy(pieces[1]))
	     				aimConnection.sendIM(player, "Buddy added.");
	     			else
	     				aimConnection.sendIM(player, "Already buddy.");
	     		} else if(message.toLowerCase().startsWith("!remove ")) {
	     			BuddyList bList = (BuddyList)buddyLists.get(name);
	     			String pieces[] = message.split(" ", 2);
	     			if(bList.removeBuddy(pieces[1]))
	     				aimConnection.sendIM(player, "Buddy removed.");
	     			else
	     				aimConnection.sendIM(player, "Not a buddy.");
	     		} else if(message.toLowerCase().startsWith("!help")) {
	     			aimConnection.sendIM(player, "!add <name>   -Adds a player to your buddy list. <br> !remove <name>   -Removes a player from your buddy list. <br> Name:Message  -Sends Name 'message' through pm.");
	     		} else {
	     			String pieces[] = message.split(":", 2);
	     			if(bothBuddies(name, pieces[0]))
	     				m_botAction.sendSmartPrivateMessage(pieces[0], name + "> " + pieces[1]);
	     			else
	     				aimConnection.sendIM(player, "You need to be on that player's buddy list to send them pm's.");
	     		}
	     	}
	     } catch(Exception e) { }
	     
	     try {
	     	Integer.parseInt("haha");
	     } catch(Exception e) {
	     	StackTraceElement[] els = e.getStackTrace();
	     	String msgs[] = new String[els.length];
	     	for(int k = 0;k < els.length;k++)
	     		msgs[k] = els[k].toString();
	     	sendIMArray("olos necaj", msgs);
	     	e.printStackTrace();
	     }
     }
     
     public void sendIMArray(String name, String messages[]) {    	
    	for(int k = 0;k < messages.length;k++)
    		messages[k] = addSpecialChars(messages[k]);
		
    	String message = "<html><body bgcolor=\"#ffffff\"><font LANG=\"0\">";
    	for(int k = 0;k < messages.length - 1;k--) {
    		message += messages[k] + "<BR>";
    	}
    	
    	message += messages[0] + "</font></body></html>";
    	try {
    		aimConnection.sendIM(name, message);
    	} catch(Exception e) {}
    }
     
     /** Checks if allowed to PM.
      *  @param Player1 One player
      *  @param Player2 Two player
      */
      public boolean bothBuddies(String p1, String p2) {
      	BuddyList b1 = (BuddyList)buddyLists.get(p1.toLowerCase());
      	BuddyList b2 = (BuddyList)buddyLists.get(p2.toLowerCase());
      	if(b1 == null || b2 == null) return false;
      	if(b1.isBuddy(p2) && b2.isBuddy(p1)) return true;
      	else return false;
      }
      
     /** Takes all the stupid things out of the message.
      *  @param message Message thing
      */
      public String removeSpecialChars(String message) {
      	for(int k = 0;k < message.length();k++) {
      		if(message.charAt(k) == '&') {
      			String type = "";
      			k++;
      			while(message.charAt(k) != ';') {
      				type += message.charAt(k);
      				k++;
      			}
      			String pieces[] = message.split("&" + type + ";", 2);
      			message = pieces[0] + getChar(type) + pieces[1];
      			k -= (2 + type.length());
      		}
      	}
      	return message;
      }
      
    /** Returns the right char for type
     *  @param type The type
     */
     public char getChar(String type) {
       	type = type.toLowerCase();
       	if(type.equals("lt")) return '<';
       	else if(type.equals("gt")) return '>';
       	else if(type.equals("amp")) return '&';
       	else if(type.equals("quot")) return '"';
       	else return ' ';
     }
     
    /** Adds the &abc; things.
     *  @param message Message
     */
     public String addSpecialChars(String message) {
     	for(int k = 0;k < message.length();k++) {
     		char atK = message.charAt(k);
     		if(atK == '<') {
     			String pieces[] = message.split("<", 2);
     			message = pieces[0] + "&lt;" + pieces[1];
     		} else if(atK == '>') {
     			String pieces[] = message.split(">", 2);
     			message = pieces[0] + "&gt;" + pieces[1];
     		}
     	}
     	return message;
     }
     
    /** Adds a news item.
     *  @param name Name of player adding.
     *  @param message Stuff...
     */
     public void addNewsItem(String name, String message) {
     	if(!m_botAction.getOperatorList().isHighmod(name) && !ops.contains(name.toLowerCase()))
     		return;
     	String contents;
     	String writer = name;
     	String url;
     	
     	String pieces[] = message.split(":", 2);
     	if(pieces.length == 2) {
     		contents = pieces[0];
     		url = pieces[1];
     	} else {
     		contents = message;
     		url = "";
     	}
     	int id;
     	String date;
     	String query = "INSERT INTO tblBotNews (fnID, fcName, fcNews, fdTime, fcURL) VALUES (0, '"+Tools.addSlashesToString(writer)+"', ";
     	query += "'"+Tools.addSlashesToString(contents)+"', NOW(), '"+Tools.addSlashesToString(url) +"')";
     	String query2 = "SELECT fnID, fdTime FROM tblBotNews WHERE fcName = '"+Tools.addSlashesToString(writer)+"' ORDER BY fnID DESC";
     	try {
     		m_botAction.SQLQuery("local", query);
     		ResultSet results = m_botAction.SQLQuery("local", query2);
     		if(results.next()) {
	     		date = results.getString("fdTime");
	     		id = results.getInt("fnID");
	     	} else { return; }
     	} catch(Exception e) { if(bug) m_botAction.sendSmartPrivateMessage("ikrit <er>", "Error while adding news item :/."); return; }
     	NewsArticle na = new NewsArticle(writer, contents, date, id, url);
     	news.put(id, na);
     	newsIDs.add(id);
     	m_botAction.sendSmartPrivateMessage(name, "News item added.");
     }
    
    /** Deletes a news item
     *  @param name Name of player
     *  @param id News id
     */
     public void deleteNewsItem(String name, String id2) {
     	if(!m_botAction.getOperatorList().isHighmod(name) && !ops.contains(name.toLowerCase()))
     		return;
     	int id = 9283749;
     	try {
     		id = Integer.parseInt(id2);
     	} catch(Exception e) { m_botAction.sendSmartPrivateMessage(name, "Someone needs to go back to 1st grade to learn what a number is."); }
     	if(id == 9283749) return;
     	
     	if(news.remove(id) != null) {
     		String query = "DELETE FROM tblBotNews WHERE fnID = " + id;
     		try {
     			m_botAction.SQLQuery("local", query);
     		} catch(Exception e) { m_botAction.sendSmartPrivateMessage(name, "Delete failed."); return; }
     		m_botAction.sendSmartPrivateMessage(name, "News article deleted.");
     		newsIDs.remove(newsIDs.indexOf(id));
     	} else {
     		m_botAction.sendSmartPrivateMessage(name, "Invalid news id.");
     	}
     }
    
    /** PM's a player with a news article.
     *  @param name Name of player
     *  @param id ID of article.
     */
     public void readNewsItem(String name, String id2) {
     	int id = 9283749;
     	try {
     		id = Integer.parseInt(id2);
     	} catch(Exception e) { m_botAction.sendSmartPrivateMessage(name, "Someone needs to go back to 1st grade to learn what a number is."); }
     	if(id == 9283749) return;
     	
     	NewsArticle na = (NewsArticle)news.get(id2);
     	if(na == null) {
     		m_botAction.sendSmartPrivateMessage(name, "Invalid news id.");
     		return;
     	}
     	
     	m_botAction.sendSmartPrivateMessage(name, na.toString());
     	if(!na.url.equals(""))
     		m_botAction.sendSmartPrivateMessage(name, "For more information, visit: " + na.url);
     }
    
    /** Gets next news article in queue.
     */
     public void nextNews() {
     	if(bug) {
     		m_botAction.sendSmartPrivateMessage("ikrit <er>", "I should have just sent a news thing... I have " + newsIDs.size() + " news articles.");
     	}
     	if(newsID > newsIDs.size()) newsID = 0;
     	if(newsIDs.isEmpty()) return;
     	
     	NewsArticle na = (NewsArticle)news.get(newsID);
     	m_botAction.sendArenaMessage(na.toString());
     	if(!na.url.equals(""))
     		m_botAction.sendArenaMessage("For more information, click on this link: " + na.url);
     	
     	newsID++;
     	
     	
     }
     
    /** Bugs Ikrit
     */
     public void bugMe(String name, String bleh) {
     	if(name.toLowerCase().startsWith("ikrit"))
     		bug = true;
     }
    
    /** Stops bugging Ikrit
     */
     public void stopBuggingMe(String name, String bleh) {
     	if(name.toLowerCase().startsWith("ikrit"))
     		bug = false;
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
		if(level >= 2) return true;
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
			updateSQL(name.toLowerCase(), 1);
			updateSQL(player.toLowerCase(), 3);
			try {
				m_bA.SQLQuery("local", "UPDATE tblChannel SET fcOwner = '"+Tools.addSlashesToString(player)+"' WHERE fcChannelName = '" + Tools.addSlashesToString(channelName.toLowerCase()) + "'");
			} catch(Exception e) { Tools.printStackTrace( e ); }
			owner = player;
			m_bA.sendSmartPrivateMessage(player, "I have just left you an important message. PM me with !messages receive it.");
			leaveMessage(name, player, "You have been made the owner of " + channelName + " channel.");
			m_bA.sendSmartPrivateMessage(name, player + " has been granted ownership of " + channelName);
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
			updateSQL(player.toLowerCase(), 2);
			m_bA.sendSmartPrivateMessage(player, "I have just left you an important message. PM me with !messages receive it.");
			leaveMessage(name, player, "You have been made an operator in " + channelName + " channel.");
			m_bA.sendSmartPrivateMessage(name, player + " has been granted op powers in " + channelName);
		}
		else
			m_bA.sendSmartPrivateMessage(name, "This player is not in the channel.");
	}
	
	/** Removes an operator.
	 *  @param Name of owner.
	 *  @param Name of op being deleted.
	 */
	public void deOp(String name, String player)
	{
		if(members.containsKey(player.toLowerCase()))
		{
			if(isOwner(player)) {
				m_bA.sendSmartPrivateMessage(name, "You can't take away your owner access!");
				return;
			}
			updateSQL(player.toLowerCase(), 1);
			m_bA.sendSmartPrivateMessage(player, "I have just left you an important message. PM me with !messages receive it.");
			leaveMessage(name, player, "Your operator priveleges in " + channelName + " channel have been revoked.");
			m_bA.sendSmartPrivateMessage(name, player + "'s op powers in " + channelName + " have been revoked.");
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
			m_bA.SQLQuery("local", "UPDATE tblChannel SET fnPrivate = 1 WHERE fcChannelName = '" + Tools.addSlashesToString(channelName.toLowerCase()) + "'");
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
			m_bA.SQLQuery("local", "UPDATE tblChannel SET fnPrivate = 0 WHERE fcChannelName = '" + Tools.addSlashesToString(channelName.toLowerCase()) + "'");
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
			updateSQL(name.toLowerCase(), 1);
			m_bA.sendSmartPrivateMessage(name, "You have been accepted to " + channelName + " announcement channel.");
		}
		else
		{
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
			updateSQL(name.toLowerCase(), -5);
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
		{
			banned.add(player.toLowerCase());
			m_bA.sendSmartPrivateMessage(name, player + " has been banned.");
		}
		if(members.containsKey(player.toLowerCase()))
		{
			int level = ((Integer)members.get(player.toLowerCase())).intValue();
			level *= -1;
			updateSQL(player, level);
		}
	}
	
	/** Unbans a player.
	 *  @param Name of operator.
	 *  @param Name of player being unbanned.
	 */
	public void unbanPlayer(String name, String player)
	{
		if(banned.contains(player.toLowerCase()))
		{
			banned.remove(player.toLowerCase());
			m_bA.sendSmartPrivateMessage(name, player + " has been unbanned.");
		}
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
		}
	}			
	
	/** Updates the database when a player's level of access changes.
	 *  @param Player being changed.
	 *  @param New level of access.
	 */
	public void updateSQL(String player, int level)
	{
		String query = "SELECT * FROM tblChannelUser WHERE fcName = '" + Tools.addSlashesToString(player.toLowerCase())+"' AND fcChannel = '"+Tools.addSlashesToString(channelName)+"'";
		
		
		try {
			ResultSet results = m_bA.SQLQuery("local", query);
			if(results.next())
			{
				if(level != -5) {
					query = "UPDATE tblChannelUser SET fnLevel = " + level + " WHERE fcName = '" + Tools.addSlashesToString(player.toLowerCase()) + "' AND fcChannel = '" + Tools.addSlashesToString(channelName) +"'";
					members.put(player.toLowerCase(), new Integer(level));
				} else {
					query = "DELETE FROM tblChannelUser WHERE fcName = '" + Tools.addSlashesToString(player.toLowerCase())+"'";
					members.remove(player.toLowerCase());
				}
				m_bA.SQLQuery("local", query);
			}
			else
			{
				query = "INSERT INTO tblChannelUser (fcChannel, fcName, fnLevel) VALUES ('" + Tools.addSlashesToString(channelName) + "', '" + Tools.addSlashesToString(player.toLowerCase()) + "', " + level + ")";
				members.put(player.toLowerCase(), new Integer(level));
				m_bA.SQLQuery("local", query);
			}
			results.close();
		} catch(SQLException sqle) { Tools.printStackTrace( sqle ); }
		  catch(NullPointerException npe) { System.out.println("Silly debugging...."); }
	}
	
	/** Leaves a message from for a player in the database.
	 *  @param Name of player leaving the message.
	 *  @param Name of player recieving the message.
	 *  @param Message being left.
	 */
	public void leaveMessage(String name, String player, String message)
	{
		String query = "INSERT INTO tblMessageSystem (fnID, fcName, fcMessage, fnRead, fdTimeStamp) VALUES (0, '"+Tools.addSlashesToString(player.toLowerCase())+"', '"+Tools.addSlashesToString(channelName)+":"+Tools.addSlashesToString(name) + ": " + Tools.addSlashesToString(message)+"', 0, NOW())";
		try {
			m_bA.SQLQuery("local", query);
		} catch(SQLException sqle) { Tools.printStackTrace( sqle ); }
	}
	
	/** Reload is called when the bot respawns. Allows channel to resume from where it was before bot went offline.
	 */
	public void reload()
	{
		String query = "SELECT * FROM tblChannelUser WHERE fcChannel = '" + Tools.addSlashesToString(channelName.toLowerCase())+"'";
		
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
			results.close();
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
			
			int level = (Integer)members.get(pName.toLowerCase());
			
			if(isOwner(pName))
				message += pName + " (Owner)";
			else if(isOp(pName))
				message += pName + " (Op)";
			else if(level > 0)
				message += pName;
			k++;
			if(k % 10 == 0 || !it.hasNext())
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
		Iterator it = banned.iterator();
		String message = "";
		m_bA.sendSmartPrivateMessage(name, "List of players banned from " + channelName + ": ");
		for(int k = 0;it.hasNext();)
		{
			String pName = (String)it.next();
			if(k % 10 != 0)
				message += ", ";
			
			message += pName;
			k++;
			if(k % 10 == 0 || !it.hasNext())
			{
				m_bA.sendSmartPrivateMessage(name, message);
				message = "";
			}
		}
	}
	
	/** Updates access after website refresh
	 *  @param Name of player
	 *  @param New access level
	 */
	 public void updateAccess(String name, int level) {
	 	members.put(name.toLowerCase(), new Integer(level));
	 	if(level < 0) {
	 		banned.add(name.toLowerCase());
	 	} else if(banned.contains(name.toLowerCase())) {
	 		banned.remove(name.toLowerCase());
	 	}
	 }
	
}


class BuddyList
{
	String name;
	int id;
	HashSet buddies;
	BotAction m_bA;
	
	public BuddyList(String n, int i, HashSet buds, BotAction ba)
	{
		name = n;
		id = i;
		buddies = buds;
		m_bA = ba;
	}
	
	public boolean addBuddy(String n)
	{
		try {
			if(buddies.add(n.toLowerCase())) {
				m_bA.SQLQuery("local", "INSERT INTO tblBuddyList (fnID, fcBuddyName) VALUES ("+id+", '"+Tools.addSlashesToString(n.toLowerCase())+"'");
				m_bA.sendSmartPrivateMessage(name, n + " added to buddy list.");
				return true;
			} else {
				m_bA.sendSmartPrivateMessage(name, n + " is already on your buddy list.");
			}
		} catch(Exception e) { }
		return false;
	}
	
	public boolean removeBuddy(String n)
	{
		try {
			if(buddies.remove(n.toLowerCase())) {
				m_bA.SQLQuery("local", "DELETE FROM tblBuddyList WHERE fcBuddyName = '"+Tools.addSlashesToString(n.toLowerCase())+"'");
				m_bA.sendSmartPrivateMessage(name, n + " removed from buddy list.");
				return true;
			} else {
				m_bA.sendSmartPrivateMessage(name, n + " is not on your buddy list.");
			}
		} catch(Exception e) { }
		return false;
	}
	
	public void listBuddies()
	{
		Iterator it = buddies.iterator();
		String message = "";
		m_bA.sendSmartPrivateMessage(name, "Buddy list:");
		for(int k = 0;it.hasNext();)
		{
			String pName = (String)it.next();
			if(k % 10 != 0)
				message += ", ";
			
			message += pName;
			k++;
			if(k % 10 == 0 || !it.hasNext())
			{
				m_bA.sendSmartPrivateMessage(name, message);
				message = "";
			}
		}
	}
	
	public boolean isBuddy(String name) {
		if(buddies.contains(name.toLowerCase())) return true;
		else return false;
	}
}

class NewsArticle 
{
	String writer;
	String contents;
	String d;
	int id;
	String url;
	
	public NewsArticle(String writer, String contents, String d, int id, String url)
	{
		this.writer = writer;
		this.contents = contents;
		this.d = d;
		this.id = id;
		this.url = url;
	}
	
	public String toString()
	{
		String news = "Article #" + id + ": " + d + "-" + contents + " -" + writer;
		return news;
	}
}