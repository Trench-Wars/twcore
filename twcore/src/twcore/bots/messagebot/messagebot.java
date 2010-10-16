package twcore.bots.messagebot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TimerTask;
import java.util.Date;
import java.text.SimpleDateFormat;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;

import twcore.core.util.Tools;
import twcore.core.util.ipc.IPCMessage;

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
 *  Added a news feature for the lobby thing that qan is designing. Highmod+ can add news messages that get arena'd every 90 sec's.
 *
 *  Added all new commands to !help thus making !help a 41 PM long help message.
 *
 *  Added debugging stuff...
 *
 *  Added an alerts chat type thing for in lobby.
 *
 *  Fixed help so it doesn't spam 41 lines.
 *
 *  Deleted AIM stuff because I dislike it
 *
 *  Added ability to leave messages to people instead of channels
 *
 *	Editted !help
 *
 *	Added ability to message yourself
 *
 *	Fixed !help order
 *
 *
 * TODO:
 *
 *	Multi-line messages
 *
 */
public class messagebot extends SubspaceBot
{
	HashMap <String,Channel>channels;
	//HashMap defaultChannel;
	HashSet <String>ops;
	CommandInterpreter m_CI;
	TimerTask messageDeleteTask, messageBotSync;
	public static final String IPCCHANNEL = "messages";
	boolean bug = false;
	public String database = "website";//If you change this you must also change line ~1426

	private LinkedList<String> playersOnline = new LinkedList();
	/** Constructor, requests Message and Login events.
	 *  Also prepares bot for use.
	 */
	public messagebot(BotAction botAction)
	{
		super(botAction);
		EventRequester events = m_botAction.getEventRequester();
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.LOGGED_ON);
		channels = new HashMap<String,Channel>();
		//defaultChannel = new HashMap();
		ops = new HashSet<String>();
		m_CI = new CommandInterpreter(m_botAction);
		registerCommands();
		createTasks();
		m_botAction.scheduleTaskAtFixedRate(messageDeleteTask, 30 * 60 * 1000, 30 * 60 * 1000);
		m_botAction.scheduleTaskAtFixedRate(messageBotSync, 2 * 60 * 1000, 2 * 60 * 1000);
	}

	/** This method handles an InterProcessEvent
	 *  @param event is the InterProcessEvent to handle.
	 */
	public void handleEvent(InterProcessEvent event)
	{
		IPCMessage ipcMessage = (IPCMessage) event.getObject();
		String message = ipcMessage.getMessage();
		checkNewMessages(message.toLowerCase());
	}

	/** Checks to see if the player has new messages.
	 *  @param Name of player to check.
	 */
	public void checkNewMessages(String name)
	{
		String query = "SELECT * FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(name)+"' and fnRead = 0";
		try {
			ResultSet results = m_botAction.SQLQuery(database, query);
            int unreadMsgs = 0;
			while(results.next()) {
                unreadMsgs++;
			}
            m_botAction.SQLClose(results);
            if(unreadMsgs > 0) 
            {
                //checkPlayerOnline(name);
                //if( playersOnline.contains(name) ){
                    m_botAction.sendSmartPrivateMessage(name, "You have "
                            + unreadMsgs + " new message" + (unreadMsgs==1?"":"s") +
                        ".  PM me with !read to read " + (unreadMsgs==1?"it":"them") +
                        ", or !messages for a list.");
                    playersOnline.remove(name);
                //}
                   
                /*else{
                        sendSSMessage(name);
                    }*/
            }
		} catch(Exception e) { Tools.printStackTrace(e); }
	}
	
	public void checkPlayerOnline(String name)
	{
	    m_botAction.locatePlayer(name);
	    
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
        m_CI.registerCommand( "!add",        acceptedMessages, this, "addPlayer" );
        m_CI.registerCommand( "!remove",     acceptedMessages, this, "removePlayer" );
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
        m_CI.registerCommand( "!readnew",    acceptedMessages, this, "readNewMessages");
        m_CI.registerCommand( "!delete",	 acceptedMessages, this, "deleteMessage");
        m_CI.registerCommand( "!messages",	 acceptedMessages, this, "myMessages");
        m_CI.registerCommand( "!msgs",       acceptedMessages, this, "myMessages");
        m_CI.registerCommand( "!go",		 acceptedMessages, this, "handleGo");
        m_CI.registerCommand( "!members",	 acceptedMessages, this, "listMembers");
        m_CI.registerCommand( "!banned",	 acceptedMessages, this, "listBanned");
        m_CI.registerCommand( "!me",		 acceptedMessages, this, "myChannels");
        m_CI.registerCommand( "!die",		 acceptedMessages, this, "handleDie");
        m_CI.registerCommand( "!check",		 acceptedMessages, this, "playerLogin");
        m_CI.registerCommand( "!ignore",	 acceptedMessages, this, "ignorePlayer");
        m_CI.registerCommand( "!unignore",	 acceptedMessages, this, "unignorePlayer");
        m_CI.registerCommand( "!ignored",	 acceptedMessages, this, "whoIsIgnored");
        m_CI.registerCommand( "!lmessage",	 acceptedMessages, this, "leaveMessage");
        m_CI.registerCommand( "!regall",	 acceptedMessages, this, "registerAll");

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
    	if(!m_botAction.getOperatorList().isBot(name))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "Sorry, you need to be a ZH+ to create a channel.");
    		return;
    	}

    	Channel c = new Channel(name.toLowerCase(), message.toLowerCase(), m_botAction, true, false);
    	channels.put(message.toLowerCase(), c);

    	String query = "INSERT INTO tblChannel (fcChannelName, fcOwner, fnPrivate) VALUES('"+Tools.addSlashesToString(message.toLowerCase())+"', '"+Tools.addSlashesToString(name.toLowerCase())+"', 0)";
    	try {
    		m_botAction.SQLClose(m_botAction.SQLQuery(database, query));
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

    	Channel c = channels.get(channel);
    	if(c.isOwner(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
    	{
    		String query = "DELETE FROM tblChannel WHERE fcChannelName = '" + Tools.addSlashesToString(channel) + "'";
    		String query2 = "DELETE FROM tblChannelUser WHERE fcChannel = '" + Tools.addSlashesToString(channel) + "'";
    		try {
    			m_botAction.SQLClose(m_botAction.SQLQuery(database, query));
                        m_botAction.SQLClose(m_botAction.SQLQuery(database, query2));
    		} catch(SQLException sqle) { Tools.printStackTrace( sqle ); }
    		m_botAction.sendSmartPrivateMessage(name, "Channel deleted.");
    		c.messageChannel(name, "Channel " + channel + " deleted.");
    		channels.remove(channel);
    	}
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }

    /** Remove a player to a channel
     *  @param Name of player.
     *  @param Name of channel they want to join.
     */
    public void removePlayer(String name, String message)
    {
    	String channel = getChannel(name, message, false).toLowerCase();
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = channels.get(channel);
    	if(c.isOwner(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
    	{
	    	if(c.leaveChannel(pieces[1])) 
	    	{
	    		m_botAction.sendSmartPrivateMessage(name, pieces[1] + " removed.");
	    	} 
	    	else {
	    		m_botAction.sendSmartPrivateMessage(name, pieces[1] + " cannot be removed (owner? not a member?).");
	    	}
    	}
    	else
    		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
    }
    
    /** Add a player to a channel
     *  @param Name of player.
     *  @param Name of channel they want to join.
     */
    public void addPlayer(String name, String message)
    {
    	String channel = getChannel(name, message, false).toLowerCase();
    	String pieces[] = message.split(":", 2);
    	if(!channels.containsKey(channel))
    	{
    		m_botAction.sendSmartPrivateMessage(name, "That channel does not exist.");
    		return;
    	}
    	
    	Channel c = channels.get(channel);
    	if(c.isOwner(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
    	{
	    	if(c.joinRequest(pieces[1], true)) 
	    	{
	    		if (c.acceptPlayer(name, pieces[1],true)) {
	    			m_botAction.sendSmartPrivateMessage(name, pieces[1] + " is now a member of " + c.channelName + ".");
	    		}
	    		else {
	    			m_botAction.sendSmartPrivateMessage(name, "An error has occured.");
	    		}
	    	} 
	    	else {
	    		m_botAction.sendSmartPrivateMessage(name, pieces[1] + " cannot join this channel for some reason (already member? banned?).");
	    	}
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

    	Channel c = channels.get(channel);
    	c.joinRequest(name, false);
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

    	Channel c = channels.get(channel);
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

    	Channel c = channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
  		  	c.acceptPlayer(name, pieces[1], false);
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

    	Channel c = channels.get(channel);
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

    	Channel c = channels.get(channel);
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

    	Channel c = channels.get(channel);
    	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()) || m_botAction.getOperatorList().isER(name)){
    	    if(pieces[1].length() > 200){
    	        m_botAction.sendSmartPrivateMessage( name, "That message is too long to send. Please create a message of 200 characters or less.");
    	        return;
    	    }
    	    else {
    	        int numMsgd = c.messageChannel(name, pieces[1]);
                m_botAction.sendSmartPrivateMessage(name, "Message sent to " + numMsgd + " members of channel '" + channel + "'.");
    	    }
    	}
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

    	Channel c = channels.get(channel);
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

    	Channel c = channels.get(channel);
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

    	Channel c = channels.get(channel);
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

    	Channel c = channels.get(channel);
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

    	Channel c = channels.get(channel);
    //	if(c.isOp(name) || m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
    		c.listMembers(name);
    //	else
    //		m_botAction.sendSmartPrivateMessage(name, "You do not have permission to do that on this channel.");
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

    	Channel c = channels.get(channel);
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

    	Channel c = channels.get(channel);
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

    	Channel c = channels.get(pieces[0].toLowerCase());
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

    	Channel c = channels.get(channel);
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

    	Channel c = channels.get(channel);
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

    	Channel c = channels.get(channel);
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
    		ResultSet results = m_botAction.SQLQuery(database, query);
    		while(results.next())
    		{
    			String channel = results.getString("fcChannel");
    			channel = channel.toLowerCase();
    			Channel c = channels.get(channel);
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
    		m_botAction.SQLClose(results);
    	} catch(Exception e) { Tools.printStackTrace(e); }
    }


    /** Sends help messages
     *  @param Name of player.
     */
    public void doHelp(String name, String message)
    {
    	if(message.toLowerCase().startsWith("m")) {
	    	m_botAction.sendSmartPrivateMessage(name, "Messaging system commands:");
            m_botAction.sendSmartPrivateMessage(name, "    !messages   (Shortcut: !msgs) - PM's you all your message numbers.");
            m_botAction.sendSmartPrivateMessage(name, "    !messages all                 - PM's you all your message numbers.");
            m_botAction.sendSmartPrivateMessage(name, "    !messages #<channel>          - PM's you alll message numbers on <channel>.");
            m_botAction.sendSmartPrivateMessage(name, "    !read                         - Reads all unread messages.");
	        m_botAction.sendSmartPrivateMessage(name, "    !read <num>                   - Reads you message <num>.");
            m_botAction.sendSmartPrivateMessage(name, "    !read r                       - Reads all old/read messages.");
            m_botAction.sendSmartPrivateMessage(name, "    !read a                       - Reads all read & unread messages.");
            m_botAction.sendSmartPrivateMessage(name, "    !read #<channel>              - Reads all unread messages on <channel>.");
            m_botAction.sendSmartPrivateMessage(name, "    !read #<channel>:r            - Reads all old/read messages on <channel>.");
            m_botAction.sendSmartPrivateMessage(name, "    !read #<channel>:a            - Reads all messages on <channel>.");
            m_botAction.sendSmartPrivateMessage(name, "    !unread <num>                 - Sets message <num> as unread.");
	        m_botAction.sendSmartPrivateMessage(name, "    !delete <num>                 - Deletes message <num>.");
            m_botAction.sendSmartPrivateMessage(name, "    !delete read                  - Deletes messages already read.");
            m_botAction.sendSmartPrivateMessage(name, "    !delete all                   - Deletes all messages.");
	        m_botAction.sendSmartPrivateMessage(name, "    !lmessage <name>:<message>    - Leaves <message> for <name>.");
	    } else if(message.toLowerCase().startsWith("c")) {
	        m_botAction.sendSmartPrivateMessage(name, "    !me                           - Tells you what channels you have joined.");
	        m_botAction.sendSmartPrivateMessage(name, "    !join <channel>               - Puts in request to join <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !quit <channel>               - Removes you from <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !owner <channel>              - Tells you who owns <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !announce <channel>:<message> - Sends everyone on <channel> a pm of <message>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !message <channel>:<message>  - Leaves a message for everyone on <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !requests <channel>           - PM's you with all the requests to join <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !banned <channel>             - Lists players banned on <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !members <channel>            - Lists all members on <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !ban <channel>:<name>         - Bans <name> from <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !unban <channel>:<name>       - Lifts <name>'s ban from <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !makeop <channel>:<name>      - Makes <name> an operator in <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !deop <channel>:<name>        - Revokes <name>'s operator status in <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !grant <channel>:<name>       - Grants ownership of <channel> to <name>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !private <channel>            - Makes <channel> a request based channel.");
	        m_botAction.sendSmartPrivateMessage(name, "    !public <channel>             - Makes <channel> open to everyone.");
	        m_botAction.sendSmartPrivateMessage(name, "    !destroy <channel>            - Destroys <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !accept <channel>:<name>      - Accepts <name>'s request to join <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !decline <channel>:<name>     - Declines <name>'s request to join <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !add <channel>:<name>         - Add <name> to <channel>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !remove <channel>:<name>      - Remove <name> from <channel>.");
		    if(m_botAction.getOperatorList().isBot(name))   m_botAction.sendSmartPrivateMessage(name, "    !create <channel>             - Creates a channel with the name <channel>.");
	    } else if((m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase())) && message.toLowerCase().startsWith("smod")) {
	    	m_botAction.sendSmartPrivateMessage(name, "Smod+ commands:");
	        m_botAction.sendSmartPrivateMessage(name, "    !go <arena>                   - Sends messagebot to <arena>.");
	        m_botAction.sendSmartPrivateMessage(name, "    !die                          - Kills messagebot.");
        } else {
	    	String defaultHelp = "PM me with !help channel for channel system help, !help message for message system help";
	    	if(m_botAction.getOperatorList().isHighmod(name) || ops.contains(name.toLowerCase()))
	    		defaultHelp += ", !help smod for SMod command help.";
	    	else
	    		defaultHelp += ".";
        	m_botAction.sendSmartPrivateMessage(name, defaultHelp);
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
	    if(event.getMessageType() == Message.ARENA_MESSAGE){
	        if(event.getMessage().contains(" - ")){
	            playersOnline.add(event.getMessage().substring(0, event.getMessage().indexOf(" -")));
	        }
	    }
		m_CI.handleEvent(event);
	}

	/** Sends the bot to the default arena and reloads all channels.
	 */
	public void handleEvent(LoggedOn event)
	{
		try {
			m_botAction.joinArena(m_botAction.getBotSettings().getString("Default arena"));
			m_botAction.ipcSubscribe(IPCCHANNEL);
			//long before = Runtime.getRuntime().freeMemory();
			String opList[] = (m_botAction.getBotSettings().getString("Ops")).split(":");
			for(int k = 0;k < opList.length;k++)
				ops.add(opList[k].toLowerCase());

			String query = "SELECT * FROM tblChannel";
			try {
				ResultSet results = m_botAction.SQLQuery(database, query);

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
                m_botAction.SQLClose(results);
			} catch(SQLException e) { Tools.printStackTrace( e ); }

			//long after = Runtime.getRuntime().freeMemory();
			//long memUsed = before - after;
		} catch(Exception e) {}
		m_botAction.setMessageLimit(5);
        m_botAction.ipcSubscribe(IPCCHANNEL);
	}

	/** Reads a message from the database.
	 *  @param Name of player reading the message.
	 *  @param Message number being read.
	 */
	public void readMessage(String name, String message) {
	    /*
        if( message == "" ) {
            try {
                LinkedList <String>messages = new LinkedList<String>();
                LinkedList <Integer>ids = new LinkedList<Integer>();
                ResultSet results = m_botAction.SQLQuery(database, "SELECT * FROM tblMessageSystem WHERE fcName='" +
                    Tools.addSlashesToString(name) + "' AND fnRead='0'" );
                if( !results.next() ) {
                    m_botAction.sendSmartPrivateMessage(name, "You have no new messages.");
                    return;                    
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MMM d h:mma");
                
                while(results.next()) {
                    String msg = results.getString("fcMessage");
                    //String timestamp = results.getString("fdTimeStamp");

                    String time = dateFormat.format( new Date( results.getTimestamp("fdTimeStamp").getTime() ) );
                    //m_botAction.sendSmartPrivateMessage(name, timestamp + " " + message);
                    String channel = results.getString("fcSender");
                    messages.add( time + " " + (!channel.equals("") ? " (" + channel + ") " : " " ) + msg );
                    ids.add( results.getInt("fnID") );
                }                
                String resetResults = "";
                int idNum = 0;
                for( Integer currentID : ids ) {
                    if( idNum == 0 )
                        resetResults += "fnID='" + currentID + "'";
                    else
                        resetResults += " OR fnID='" + currentID + "'";
                    idNum++;
                }
                messages.add( "Total " + idNum + " message" + (idNum==1?"":"s") + " displayed." );
                
                messages.add( "Now attempting to set messages to read..." );
                if( !resetResults.equals("") ) {
                    String query = "UPDATE tblMessageSystem SET fnRead='1' WHERE " + resetResults + "";
                    Tools.printLog( query );
                    m_botAction.SQLQueryAndClose(database, query);
                    messages.add( "Set messages as read." );
                } else {
                    messages.add( "Did not set messages as read." );
                }
                
                /*
                    messageIDs.add(results.getInt("fnID"));
                Iterator<Integer> it = messageIDs.iterator();
                if( !it.hasNext() ) {
                    m_botAction.sendSmartPrivateMessage(name, "You have no new messages.");
                    return;
                }
                while(it.hasNext()) {
                    // SUCH an idiotic way to handle this... a query for each message?  Jesus Christ.
                    // You already HAVE the query you need... work with it...
                    // readMessage(name, "" + (Integer)it.next());                    
                }
                */
	    /*
                SpamTask spamTask = new SpamTask();
                spamTask.setMsgs( name, messages );
                m_botAction.scheduleTask(spamTask, 75, 75 );
                m_botAction.SQLClose(results);
            } catch(SQLException e) {
                Tools.printLog("Exception while trying to run SQL query in readMessage." );
            }
            return;
        }
        */
		if(!isAllDigits(message)) {
			try {
				String addAnd = " AND fnRead='0'";
                ResultSet results;
                if( message.startsWith("#") && message.length() > 1 ) {
                    String pieces[] = message.split(":", 2);
                    if(pieces.length == 2) {
                        message = pieces[0].substring(1);
                        if(pieces[1].toLowerCase().startsWith("a"))
                            addAnd = "";
                        else if(pieces[1].toLowerCase().startsWith("r"))
                            addAnd = " AND fnRead='1'";
                    } else {
                        message = message.substring(1);
                    }
                    results = m_botAction.SQLQuery(database, "SELECT * FROM tblMessageSystem WHERE fcSender = '"
                            + Tools.addSlashesToString(message) + "' AND fcName = '" + Tools.addSlashesToString(name) + "'"+addAnd);
                } else {
                    // Not a channel request; either a standard !read, or reading all messages or only read messages
                    if( message.toLowerCase().startsWith("a"))
                        addAnd = "";
                    else if( message.toLowerCase().startsWith("r"))
                        addAnd = " AND fnRead='1'";
                    results = m_botAction.SQLQuery(database, "SELECT * FROM tblMessageSystem WHERE fcName = '" +
                            Tools.addSlashesToString(name) + "'"+addAnd);
                }

                LinkedList <String>messages = new LinkedList<String>();
                LinkedList <Integer>ids = new LinkedList<Integer>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MMM d h:mma");

                if( !results.next() ) {
                    if( addAnd.equals( " AND fnRead='0'" ) )
                        m_botAction.sendSmartPrivateMessage(name, "You have no new messages.");
                    else
                        m_botAction.sendSmartPrivateMessage(name, "You have no messages fitting those criteria.");
                    return;                    
                }

                int numMsgs = 0;
                do {
                    String msg = results.getString("fcMessage");
                    //String timestamp = results.getString("fdTimeStamp");

                    String time = dateFormat.format( new Date( results.getTimestamp("fdTimeStamp").getTime() ) );
                    //m_botAction.sendSmartPrivateMessage(name, timestamp + " " + message);
                    String channel = results.getString("fcSender");
                    messages.add( time + " " + (!channel.equals("") ? " (" + channel + ") " : " " ) + msg );
                    if( results.getInt("fnRead") == 0 )
                        ids.add( results.getInt("fnID") );
                    numMsgs++;
                } while( results.next() );
                
                String resetResults = "";
                int idResetNum = 0;
                for( Integer currentID : ids ) {
                    if( idResetNum == 0 )
                        resetResults += "fnID='" + currentID + "'";
                    else
                        resetResults += " OR fnID='" + currentID + "'";
                    idResetNum++;
                }
                
                if( idResetNum == numMsgs ) {
                    messages.add( "Total " + numMsgs + " new message" + (numMsgs==1?"":"s") + " displayed." );
                } else {
                    messages.add( "Total " + numMsgs + " message" + (numMsgs==1?"":"s") + " displayed." + (idResetNum>0 ? (" (" + idResetNum + " new)") : "") );                    
                }
                
                if( !resetResults.equals("") ) {
                    String query = "UPDATE tblMessageSystem SET fnRead='1' WHERE " + resetResults + "";
                    m_botAction.SQLQueryAndClose(database, query);
                }
                
                SpamTask spamTask = new SpamTask();
                spamTask.setMsgs( name, messages );
                m_botAction.scheduleTask(spamTask, 75, 75 );

                /*
				while(results.next()) {
					messageIDs.add(results.getInt("fnID"));
				}
				Iterator<Integer> it = messageIDs.iterator();
				while(it.hasNext()) {
					int id = (Integer)it.next();
					readMessage(name, ""+id);
				}
				*/
                m_botAction.SQLClose(results);
			} catch(Exception e) {
                Tools.printLog("Exception while trying to run SQL query in readMessage." );			    
			}
			return;
		}
		
		// Single message handling ONLY.  This method is NO LONGER recursively (and inefficiently) called.		
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
		String query = "SELECT * FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"' AND fnID='" + messageNumber + "'";
		try{
			ResultSet results = m_botAction.SQLQuery(database, query);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MMM d h:mma");
			if(results.next()) {
                message = results.getString("fcMessage");
                String time = dateFormat.format( new Date( results.getTimestamp("fdTimeStamp").getTime() ) );
                String channel = results.getString("fcSender");
                m_botAction.sendSmartPrivateMessage(name, time + " " + (!channel.equals("") ? " (" + channel + ") " : " " ) + message);
                if( results.getInt("fnRead") == 0 ) {
                    query = "UPDATE tblMessageSystem SET fnRead='1' WHERE fnID='" + messageNumber +"'";
                    m_botAction.SQLQueryAndClose(database, query);
                }
			} else {
				m_botAction.sendSmartPrivateMessage(name, "Could not find that message.  PM !messages to see new messages, or !messages all to see all messages.");
			}
            m_botAction.SQLClose(results);
		} catch(Exception e) { Tools.printStackTrace( e ); }
	}
	
	/**
	 * Support method to get the text of a given message when reading multiple message
	 * numbers.  The way this should have been written initially.  It took 20 minutes.
	 * (Passing a ResultSet seemed to result in erratic behavior for keeping track of
	 * the row.)
	 * @param rs
	 * @param dateFormat
	 * @return
	public String getMessageText( ResultSet rs, SimpleDateFormat dateFormat ) {
        try {
            String message = rs.getString("fcMessage");
            //String timestamp = results.getString("fdTimeStamp");

            String time = dateFormat.format( new Date( rs.getTimestamp("fdTimeStamp").getTime() ) );
            //m_botAction.sendSmartPrivateMessage(name, timestamp + " " + message);
            String channel = rs.getString("fcSender");
            return time + " " + (!channel.equals("") ? " (" + channel + ") " : " " ) + message;
        } catch(Exception e) {
            Tools.printStackTrace( e );
            return "";
        }
	}
     */

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
                        m_botAction.SQLClose(m_botAction.SQLQuery(database, query));
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
		String query;
		if(message.equalsIgnoreCase("all")) {
			query = "DELETE FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"'";
        } else if(message.equalsIgnoreCase("read")) {
            query = "DELETE FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"' AND fnRead = 1";
        } else {
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
			query = "DELETE FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"' AND fnID = " + messageNumber;
		}
		try {
                        m_botAction.SQLClose(m_botAction.SQLQuery(database, query));
			m_botAction.sendSmartPrivateMessage(name, "Message(s) deleted.");
		} catch(Exception e) {
			m_botAction.sendSmartPrivateMessage(name, "Message(s) unable to be deleted.");
			Tools.printStackTrace( e );
		}
	}

	/** PM's a player with all of his/her message numbers.
	 *  @param Name of player.
	 *  @param (does nothing).
	 */
	public void myMessages(String name, String message)
	{
        boolean allMsgs = message.startsWith("all") || message.startsWith("*");
        String query;
        if( allMsgs ) {
            m_botAction.sendSmartPrivateMessage(name, "You have the following messages:");
            query = "SELECT * FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"' ORDER BY fnRead DESC";
        } else {
            if( message.startsWith("#") && message.length() > 1 ) {
                message = message.substring(1);
                m_botAction.sendSmartPrivateMessage(name, "You have the following messages on channel '" + message + "':");
                query = "SELECT * FROM tblMessageSystem WHERE fcSender = '" + Tools.addSlashesToString(message) + "' AND fcName = '" + Tools.addSlashesToString(name) + "'";
            } else { 
                m_botAction.sendSmartPrivateMessage(name, "You have the following unread messages (use '!msgs all' for a list of read and unread msgs):");
                query = "SELECT * FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"' AND fnRead=0";
            }
        }
		try {
			ResultSet results = m_botAction.SQLQuery(database, query);
			LinkedList<String> msgs = new LinkedList<String>();
			while(results.next())
			{
			    String thisMessage = "Message #" + String.valueOf(results.getInt("fnID")) +
			    "  From: " + Tools.formatString(results.getString("fcSender"), 20 ) +
			    "  Read: [";
			    if(results.getInt("fnRead") == 1)
			        thisMessage += "X]";
			    else
			        thisMessage += " ]";

			    msgs.add( thisMessage );
			}
            SpamTask spamTask = new SpamTask();
            spamTask.setMsgs(name, msgs);
            m_botAction.scheduleTask(spamTask, 75, 75 );
            
            m_botAction.SQLClose(results);
		} catch(Exception e) {
			m_botAction.sendSmartPrivateMessage(name, "Error while reading message database during listing of msgs.");
			Tools.printStackTrace( e );
		}
		m_botAction.sendSmartPrivateMessage(name, "PM me with !read to read all unread messages, or use !read <num>.");
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
	 		ResultSet results = m_botAction.SQLQuery(database, query);

            if( results == null || !results.next() ) {
                m_botAction.SQLClose(results);
                return false;
             }

	 		if(results.getString("fcName").toLowerCase().equals(name)) {
                m_botAction.SQLClose(results);
	 			return true;
	 		} else {
                m_botAction.SQLClose(results);
	 			return false;
	 		}
	 	} catch(Exception e) {
        }
	 	return false;
	 }

	/** Retrieves the channel name out of the message.
	 *  @param name Name of player.
	 *  @param message Message sent
	 */
	 public String getChannel(String name, String message, boolean noParams) {
	 	if(noParams) {
	 		return message;
	 	} else {
		 	if(message.indexOf(":") == -1) {
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
	 	else checkNewMessages(name.toLowerCase());
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
			 	m_botAction.die();
			} catch(Exception e) { }
		}
	 }

	 /** Announces to a channel that they have recieved a new message.
	  *  @param Name of channel
	  */
	 public Set<String> messageSentFromWebsite(String channel)
	 {
	 	Set <String>s = null;

	 	try {
		 	Channel c = channels.get(channel.toLowerCase());
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
	 		Channel c = channels.get(channel.toLowerCase());
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
			//		m_botAction.SQLQuery(database, query);
			//		m_botAction.SQLQuery(database, query2);
			//		System.out.println("Deleting messages.");
			//	} catch(SQLException e) { Tools.printStackTrace( e ); }
			}
		};

		messageBotSync = new TimerTask()
		{
			public void run()
			{
				HashSet <String>peopleToTell = new HashSet<String>();

				String query = "SELECT * FROM tblMessageToBot ORDER BY fnID ASC";
				String query2 = "DELETE FROM tblMessageToBot";
				try {
					ResultSet results = m_botAction.SQLQuery(database, query);
					if(results == null) return;
					while(results.next()) {
						String event = results.getString("fcSyncData");
						String pieces[] = event.split(":");
						if(pieces.length == 2)
							peopleToTell.addAll(messageSentFromWebsite(pieces[1]));
						else
							accessUpdateFromWebsite(pieces[2], pieces[1], Integer.parseInt(pieces[3]));
					}
                                        m_botAction.SQLClose(results);
                                        m_botAction.SQLClose(m_botAction.SQLQuery(database, query2));
				} catch(Exception e) { Tools.printStackTrace( e ); }

				Iterator<String> it = peopleToTell.iterator();
				while(it.hasNext()) {
					String player = (String)it.next();
					m_botAction.sendSmartPrivateMessage(player, "You have received a new message. PM me with !messages to read it.");
				}
			}
		};

       
	}


     public void ignorePlayer(String name, String player) {
     	try {
     		if(!isIgnored(name, player)) {
     		        m_botAction.SQLClose(m_botAction.SQLQuery(database, "INSERT INTO tblMessageBotIgnore (fcIgnorer, fcIgnoree) VALUES('"+Tools.addSlashesToString(name)+"', '"+Tools.addSlashesToString(player)+"');"));
	     		m_botAction.sendSmartPrivateMessage(name, player + " ignored.");
	     	} else {
	     		m_botAction.sendSmartPrivateMessage(name, player + " is already ignored.");
	     	}
     	} catch(Exception e) {}
     }

     public void unignorePlayer(String name, String player) {
     	try {
     		if(isIgnored(name, player)) {
     		        m_botAction.SQLClose(m_botAction.SQLQuery(database, "DELETE FROM tblMessageBotIgnore WHERE fcIgnorer = '"+Tools.addSlashesToString(name)+"' AND fcIgnoree = '"+Tools.addSlashesToString(player)+"';"));
     			m_botAction.sendSmartPrivateMessage(name, player + " unignored.");
     		} else {
     			m_botAction.sendSmartPrivateMessage(name, player + " is not currently ignored.");
     		}
     	} catch(Exception e) {}
     }

     public void whoIsIgnored(String name, String blank) {
     	try {
     		ResultSet results = m_botAction.SQLQuery(database, "SELECT * FROM tblMessageBotIgnore WHERE fcIgnorer = '"+Tools.addSlashesToString(name)+"'");
     		String ignored = "";
     		while(results.next()) {
     			ignored += results.getString("fcIgnoree");
     			if(ignored.length() > 150) {
     				m_botAction.sendSmartPrivateMessage(name, ignored);
     				ignored = "";
     			} else {
     				ignored += ", ";
     			}
     		}
                m_botAction.SQLClose(results);
     		m_botAction.sendSmartPrivateMessage(name, ignored.substring(0, ignored.length() - 2));
     	} catch(Exception e) {}
     }

     public boolean isIgnored(String name, String player) {
     	try {
     		ResultSet results = m_botAction.SQLQuery(database, "SELECT * FROM tblMessageBotIgnore WHERE fcIgnorer = '"+Tools.addSlashesToString(name)+"' AND fcIgnoree = '"+Tools.addSlashesToString(player)+"'");
     		if(results.next()) {
     		        m_botAction.SQLClose(results);
     			return true;
     		} else {
     		        m_botAction.SQLClose(results);
     			return false;
     		}
     	} catch(Exception e) {}
     	return false;
     }

     public void leaveMessage(String name, String message) {
     	if(message.indexOf(":") == -1) {
     		m_botAction.sendSmartPrivateMessage(name, "Correct usage: !lmessage <name>:<message>");
     		return;
     	}
     	String pieces[] = message.split(":", 2);
     	String player = pieces[0];
     	message = pieces[1];
     	try {
     		String query1 = "SELECT count(*) AS msgs FROM tblMessageSystem WHERE fcSender = '"+Tools.addSlashesToString(name)+"' AND fdTimeStamp > SUBDATE(NOW(), INTERVAL 7 DAY)";
     		String query2 = "SELECT count(*) AS msgs FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(player)+"' AND fcSender = '"+Tools.addSlashesToString(name)+"' AND fdTimeStamp > SUBDATE(NOW(), INTERVAL 1 DAY)";
     		//String query3 = "SELECT count(*) AS msgs FROM tblMessageSystem WHERE fcName = '"+Tools.addSlashesToString(player)+"'";
     		ResultSet results = m_botAction.SQLQuery(database, query1);
     		int msgsSent = 0;
     		if(results.next()) {
     			msgsSent = results.getInt("msgs");
     		}
     		int plrMsgsRcvdFrmName = 0;
                m_botAction.SQLClose(results);
     		results = m_botAction.SQLQuery(database, query2);
     		if(results.next()) {
     			plrMsgsRcvdFrmName = results.getInt("msgs");
     		}
                m_botAction.SQLClose(results);
            /*
     		int plrMsgsRcvd = 0;
     		results = m_botAction.SQLQuery(database, query3);
     		if(results.next()) {
     			plrMsgsRcvd = results.getInt("msgs");
     		}
                m_botAction.SQLClose(results);
            */
     		if(msgsSent > 100) {
     			m_botAction.sendSmartPrivateMessage(name, "Sorry, you have reached your weekly quota of 100 messages. Please wait until some of your messages reset their stats before trying to send more.");
     			return;
     		} else if(plrMsgsRcvdFrmName > 3) {
     			m_botAction.sendSmartPrivateMessage(name, "Sorry, you have reached your daily limit on messages to this player. Please wait before sending more messages.");
     			return;
     	    /*
     		} else if(plrMsgsRcvd > 24) {
     			m_botAction.sendSmartPrivateMessage(name, "Sorry, the player's inbox is currently full. Please try to message him/her later.");
     			return;
     	    */
     		} else if(isIgnored(player, name)) {
     			return;
     		} else {
     			m_botAction.SQLClose(m_botAction.SQLQuery(database, "INSERT INTO tblMessageSystem (fnID, fcName, fcMessage, fcSender, fnRead, fdTimeStamp) VALUES(0, '"+Tools.addSlashesToString(player)+"', '"+Tools.addSlashesToString(message)+"', '"+Tools.addSlashesToString(name)+"', 0, NOW())"));
     			m_botAction.sendSmartPrivateMessage(name, "Message sent.");
     			checkPlayerOnline(player);
     			if(playersOnline.contains(player))
     			    m_botAction.sendSmartPrivateMessage(name, "You have a new message, " +
     			    		"type :MessageBot:!read to check");
     			else
     			    m_botAction.sendUnfilteredPublicMacro("?message "+player+":You have a new message, " +
     			    		"type :MessageBot:!read to check");
     		}
     	} catch(Exception e) {}
     }

     public void registerAll(String name, String message) {
     	if(!m_botAction.getOperatorList().isHighmod(name) && !ops.contains(name.toLowerCase()))
     		return;
     	try {
    		String pieces[] = message.split(":",2);
    		URL site = new URL(pieces[1]);
    		HashSet <String>nameList = new HashSet<String>();
    		if(site.getFile().toLowerCase().endsWith("txt"))
    		{
    			URLConnection file = site.openConnection();
    			file.connect();
    			BufferedReader getnames = new BufferedReader(new InputStreamReader(file.getInputStream()));
    			String nextName;
    			while((nextName = getnames.readLine()) != null)
    				nameList.add(nextName);
    			file.getInputStream().close();
    			Iterator<String> it = nameList.iterator();
    			while(it.hasNext()) {
    				String addName = (String)it.next();
					ResultSet results = m_botAction.SQLQuery(database, "SELECT * FROM tblChannelUser WHERE fcName = '"
						+ Tools.addSlashesToString(addName)+"' AND fcChannel = '"
						+ Tools.addSlashesToString(pieces[0])+"'");
					if(!results.next()) {
                                                m_botAction.SQLClose(m_botAction.SQLQuery(database, "INSERT INTO tblChannelUser (fcName, fcChannel, fnLevel) VALUES ('"
							+ Tools.addSlashesToString(addName)+"', '"+Tools.addSlashesToString(pieces[0])+"', 1)"));
					}
                                        m_botAction.SQLClose(results);
    			}
    			m_botAction.sendSmartPrivateMessage(name, "Done");
    		} else {
    			m_botAction.sendSmartPrivateMessage(name, "Please end the file name with .txt");
    		}
    	} catch(Exception e) {}
     }

     public boolean isAllDigits(String test) {
		boolean allDigits = true;
		for(int k = 0;k < test.length() && allDigits;k++) {
			if(!Character.isDigit(test.charAt(k))) allDigits = false;
		}
		return allDigits && test.length() != 0;
	}
     
     
     /**
      * Task used to send a spam of messages at a slower rate than normal.
      */
     private class SpamTask extends TimerTask {
         LinkedList <String>remainingMsgs = new LinkedList<String>();
         String nameToSpam = "";

         public void setMsgs( String name, LinkedList<String> list ) {
             nameToSpam = name;
             remainingMsgs = list;
         }

         public void run() {
             if( remainingMsgs == null || remainingMsgs.isEmpty() ) {
                 this.cancel();
             } else {
                 String msg = remainingMsgs.remove();
                 if( msg != null )
                     m_botAction.sendRemotePrivateMessage( nameToSpam, msg );
             }
         }
     }
}

class Channel
{
	String owner;
	String channelName;
	BotAction m_bA;
	HashMap <String,Integer>members;
	HashSet <String>banned;
	boolean isOpen;
	String database = "website";

	/** Constructs the new channel.
	 */
	public Channel(String creator, String cName, BotAction botAction, boolean pub, boolean auto)
	{
		m_bA = botAction;
		owner = creator;
		channelName = cName.toLowerCase();
		isOpen = pub;
		members = new HashMap<String,Integer>();
		banned = new HashSet<String>();
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

		int level = members.get(name.toLowerCase()).intValue();
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

		int level = members.get(name.toLowerCase()).intValue();
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
			updateSQL(owner.toLowerCase(), 1);
			updateSQL(player.toLowerCase(), 3);
			try {
                                m_bA.SQLClose(m_bA.SQLQuery(database, "UPDATE tblChannel SET fcOwner = '"+Tools.addSlashesToString(player)+"' WHERE fcChannelName = '" + Tools.addSlashesToString(channelName.toLowerCase()) + "'"));
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
		if(isOwner(player)) {
				m_bA.sendSmartPrivateMessage(name, "You can't take away your owner access!");
				return;
		}
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
                        m_bA.SQLClose(m_bA.SQLQuery(database, "UPDATE tblChannel SET fnPrivate = 1 WHERE fcChannelName = '" + Tools.addSlashesToString(channelName.toLowerCase()) + "'"));
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
                        m_bA.SQLClose(m_bA.SQLQuery(database, "UPDATE tblChannel SET fnPrivate = 0 WHERE fcChannelName = '" + Tools.addSlashesToString(channelName.toLowerCase()) + "'"));
		} catch(Exception e) { Tools.printStackTrace( e ); }
		m_bA.sendSmartPrivateMessage(name, "Now public channel.");
		isOpen = true;
	}

	/** Sends a ?message to everyone on channel and pm's the players that
	 *  are online to tell them they got a message.
	 *  @param Name of operator.
	 *  @param Message being sent.
	 *  @return Number of players messaged
	 */
	public int messageChannel(String name, String message)
	{
	    int numMsgd = 0;
		Iterator<String> it = members.keySet().iterator();

		while(it.hasNext())
		{
			String player = (String)it.next();
			int level = members.get(player.toLowerCase()).intValue();
			if(level > 0)
			{
			    // sends a ?message to all players in channel
			    m_bA.sendUnfilteredPublicMessage("?message " + player +
                ":You have new messages, type :MessageBot:!read to check them!");
			    
			    numMsgd++;
				leaveMessage(name, player, message);
				m_bA.sendSmartPrivateMessage(player, "I have just left you an important message. PM me with !messages receive it.");
			}
		}
		return numMsgd;
	}

	/** PM's everyone on channel.
	 *  @param Name of operator.
	 *  @param Message being sent.
	 */
	public void announceToChannel(String name, String message)
	{
		Iterator<String> it = members.keySet().iterator();

		while(it.hasNext())
		{
			String player = it.next();
			int level = members.get(player.toLowerCase()).intValue();
			if(level > 0)
			{
				m_bA.sendSmartPrivateMessage(player, channelName + ": " + name + "> " + message);
			}
		}
	}

	/** Puts in request to join channel.
	 *  @param Name of player wanting to join.
	 */
	public boolean joinRequest(String name, boolean silent)
	{
		if(members.containsKey(name.toLowerCase()))
		{
			if (!silent)
				m_bA.sendSmartPrivateMessage(name, "You are already on this channel.");
			return false;
		}
		if(banned.contains(name.toLowerCase()))
		{
			if (!silent)
				m_bA.sendSmartPrivateMessage(name, "You have been banned from this channel.");
			return false;
		}
		if(isOpen)
		{
			if (!silent)
				updateSQL(name.toLowerCase(), 1);
			m_bA.sendSmartPrivateMessage(name, "You have been accepted to " + channelName + " announcement channel.");
		}
		else
		{
			if (!silent)
				updateSQL(name.toLowerCase(), 0);
			m_bA.sendSmartPrivateMessage(name, "You have been placed into the channel request list. The channel owner will make the decision.");
		}
		return true;
	}

	/** Removes a player from the channel
	 *  @param Name of player leaving.
	 */
	public boolean leaveChannel(String name)
	{
		if(members.containsKey(name.toLowerCase()))
		{
			int level = members.get(name.toLowerCase()).intValue();
			if(level < 0)
			{
				m_bA.sendSmartPrivateMessage(name, "You are not on this channel.");
				return false;
			}
			if(isOwner(name)) {
				m_bA.sendSmartPrivateMessage(name, "You have to make a new owner before you leave.");
				return false;
			}
			updateSQL(name.toLowerCase(), -5);
			m_bA.sendSmartPrivateMessage(name, "You have been removed from the channel.");
			return true;
		}
		else
			m_bA.sendSmartPrivateMessage(name, "You are not on this channel.");
		return false;
	}

	/** Lists all requests to join channel.
	 *  @param Name of operator.
	 */
	public void listRequests(String name)
	{
		Iterator<String> it = members.keySet().iterator();

		m_bA.sendSmartPrivateMessage(name, "People requesting to join: ");

		while(it.hasNext())
		{
			String p = it.next();
			int level = members.get(p).intValue();
			if(level == 0)
				m_bA.sendSmartPrivateMessage(name, p);
		}
	}

	/** Accepts a player onto channel.
	 *  @param Name of operator.
	 *  @param Name of player being accepted.
	 */
	public boolean acceptPlayer(String name, String player, boolean silent)
	{
		if(members.containsKey(player.toLowerCase()))
		{
			updateSQL(player.toLowerCase(), new Integer(1));
			if (!silent)
				m_bA.sendSmartPrivateMessage(player, "I have just left you an important message. PM me with !messages receive it.");
			leaveMessage(name, player, "You have been accepted into " + channelName + " channel.");
			if (!silent)
				m_bA.sendSmartPrivateMessage(name, player + " accepted.");
			return true;
		}
		else
			m_bA.sendSmartPrivateMessage(name, "This player has not requested to join.");
		return false;
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
		if(isOwner(player)) {
			m_bA.sendSmartPrivateMessage(name, "You cannot ban the owner!");
			return;
		}
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
			int level = members.get(player.toLowerCase()).intValue();
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
			int level = members.get(player.toLowerCase()).intValue();
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
			ResultSet results = m_bA.SQLQuery(database, query);
			if(results.next())
			{
				if(level != -5) {
					query = "UPDATE tblChannelUser SET fnLevel = " + level + " WHERE fcName = '" + Tools.addSlashesToString(player.toLowerCase()) + "' AND fcChannel = '" + Tools.addSlashesToString(channelName) +"'";
					members.put(player.toLowerCase(), new Integer(level));
				} else {
					query = "DELETE FROM tblChannelUser WHERE fcName = '" + Tools.addSlashesToString(player.toLowerCase())+"'";
					members.remove(player.toLowerCase());
				}
                                m_bA.SQLClose(m_bA.SQLQuery(database, query));
			}
			else
			{
				query = "INSERT INTO tblChannelUser (fcChannel, fcName, fnLevel) VALUES ('" + Tools.addSlashesToString(channelName) + "', '" + Tools.addSlashesToString(player.toLowerCase()) + "', " + level + ")";
				members.put(player.toLowerCase(), new Integer(level));
                                m_bA.SQLClose(m_bA.SQLQuery(database, query));
			}
                        m_bA.SQLClose(results);
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
		String query = "INSERT INTO tblMessageSystem (fnID, fcName, fcMessage, fcSender, fnRead, fdTimeStamp) VALUES (0, '"+Tools.addSlashesToString(player.toLowerCase())+"', '"+Tools.addSlashesToString(name) + ": " + Tools.addSlashesToString(message)+"', '"+Tools.addSlashesToString(channelName)+"', 0, NOW())";
		try {
                        m_bA.SQLClose(m_bA.SQLQuery(database, query));
		} catch(SQLException sqle) { Tools.printStackTrace( sqle ); }
	}

	/** Reload is called when the bot respawns. Allows channel to resume from where it was before bot went offline.
	 */
	public void reload()
	{
		String query = "SELECT * FROM tblChannelUser WHERE fcChannel = '" + Tools.addSlashesToString(channelName.toLowerCase())+"'";

		try {
			ResultSet results = m_bA.SQLQuery(database, query);
			while(results.next())
			{
				String name = results.getString("fcName");
				int level = results.getInt("fnLevel");
				members.put(name.toLowerCase(), new Integer(level));
				if(level < 0)
					banned.add(name.toLowerCase());
			}
                        m_bA.SQLClose(results);
		} catch(Exception e) { Tools.printStackTrace( e ); }
	}

	/** Lists all players on channel.
	 *  @param Name of player.
	 */
	public void listMembers(String name)
	{
		Iterator<String> it = members.keySet().iterator();
		String message = "";
		m_bA.sendSmartPrivateMessage(name, "List of players on " + channelName + ": ");
        if( !it.hasNext() ) {
            m_bA.sendSmartPrivateMessage(name, "Error: no members found.  Please use ?help to report this problem." );
            return;
        }

		for(int k = 0;it.hasNext();)
		{
			String pName = (String)it.next();

			int level = members.get(pName.toLowerCase());

			if(isOwner(pName))
				message += pName + " (Owner), ";
			else if(isOp(pName))
				message += pName + " (Op), ";
			else if(level > 0)
				message += pName + ", ";
			k++;
			if(k % 10 == 0 || !it.hasNext())
			{
                if( message.length() > 2 ) {
                    m_bA.sendSmartPrivateMessage(name, message.substring(0, message.length() - 2));
                    message = "";
                }
			}
		}
	}

	/** Lists all players banned from channel.
	 *  @param Name of player.
	 */
	public void listBanned(String name)
	{
		Iterator<String> it = banned.iterator();
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

