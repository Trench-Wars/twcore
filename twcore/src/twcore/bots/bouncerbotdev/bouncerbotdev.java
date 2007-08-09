package twcore.bots.bouncerbotdev;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.util.Tools;
import java.util.TimerTask;

/**
 * A bot designed to kick players off the server who enter into a particular arena
 * without being invited It-- a personal bouncer.
 * 
 * Modifications include the ability to kick all but those on the owners.cfg, 
 * including it's own core and siblings if you're not careful. This bot is
 * intended for dev use. It also has a log checker to ensure nobody's touching
 * files they arn't supposed to, especially the files of the arena the bot
 * is in; unless you're the owner of the bot.
 *
 * @author  harvey - modifications Ice-demon / Ayano
 */
public class bouncerbotdev extends SubspaceBot {
    OperatorList m_opList;
    HashSet<String> invitedPlayers;
    ArrayList<String> log;
    ArrayList<String> ignorelog;
    ArrayList<String> fileNames;
    String bouncemessage;
    TimerTask logcheck;
    TimerTask getlogs;
    boolean logging;
    
    /**
     * Initializes.
     * 
     * @param botAction requests botActions.
     */
    
    public bouncerbotdev( BotAction botAction ){
        super( botAction );
        invitedPlayers = new HashSet<String>();
        bouncemessage = "Entering a private arena without being invited is against the rules.  Goodbye!";
        log = new ArrayList<String>();
        fileNames = new ArrayList<String>();
        ignorelog = new ArrayList<String>();
        logging = false;
        EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.MESSAGE );
        events.request( EventRequester.PLAYER_ENTERED );
        invitedPlayers.add( m_botAction.getBotName().toLowerCase() );
    }

    /**
     * Places violations a a log file.
     * 
     * @param event is the violation to be recorded.
     */
    
    public void logEvent( String event ){
        Calendar c = Calendar.getInstance();
        String timestamp = c.get( Calendar.MONTH ) + "/" + c.get( Calendar.DAY_OF_MONTH )
        + "/" + c.get( Calendar.YEAR ) + ": " + c.get( Calendar.HOUR ) + ":"
        + c.get( Calendar.MINUTE ) + ":" + c.get( Calendar.SECOND ) + " - ";
        try{
            PrintWriter out = new PrintWriter( new FileWriter( "bouncerbot.log" ));
            log.add( timestamp + event );
            for( Iterator<String> i = log.iterator(); i.hasNext(); ){
                out.println( (String)i.next() );
            }
            out.close();
            
        }catch(Exception e){
            Tools.printStackTrace( e );
        }
    }
    
    /**
     * Handles all entries and determines if they are legal.
     */
    
    public void handleEvent( PlayerEntered event ){
        if( m_opList.isOwner(event.getPlayerName())) return;
        if( invitedPlayers.contains( event.getPlayerName().toLowerCase())) return;
        m_botAction.sendPrivateMessage( event.getPlayerName(), bouncemessage );
        m_botAction.sendUnfilteredPrivateMessage( event.getPlayerName(), "*kill" );
        m_botAction.sendPublicMessage( "Whoops, " + event.getPlayerName() + " entered without permission." );
        m_botAction.sendChatMessage( event.getPlayerName() + " went into a private arena without asking permission, and was mysteriously disconnected from the server." );
        logEvent( event.getPlayerName() + " entered the arena illegally!" );
    }

    /**
     * The bot's morning ass rub
     */
    
    public void handleEvent( LoggedOn event ){
        m_botAction.joinArena( "#noseeum" );
        m_botAction.sendUnfilteredPublicMessage("?chat=L0gch4t");
        m_opList = m_botAction.getOperatorList();
        logEvent( "Logged in!" );
    }

    /**
     * Handles commands, there is no !help but since you're reading
     * this, get in the 'know' as this isn't meant to be used
     * by clueless ERs.
     * 
     * @param name is the user of the bot.
     * @param message is the command.
     */
    
    public void handleCommand( String name, String message ){
        if( message.startsWith( "!invite " ))	{
            if( message.length() > 0 ){
                String invitee = message.substring(8);
                invitedPlayers.add( invitee.toLowerCase() );
                m_botAction.sendRemotePrivateMessage( invitee, "You have been invited to " + m_botAction.getArenaName() + "!" );
                m_botAction.sendPublicMessage( invitee + " has been invited" );
                logEvent( invitee + " has been invited" );
            }
        }
        else if( message.startsWith( "!message " ))	{
            bouncemessage = message.substring( 9 );
            m_botAction.sendRemotePrivateMessage( name, "Message set to: " + bouncemessage );
        }
        else if( message.startsWith( "!go " ))	{
        	m_botAction.changeArena( message.substring( 4 ));
        	m_botAction.sendRemotePrivateMessage( name,"Going to " + m_botAction.getArenaName());
        }
        else if( message.startsWith( "!startlog" ))	
        {doStartLog(name);}
        else if( message.startsWith( "!stoplog" ))	
        {doStopLog(name);}
        else if( message.startsWith( "!setfiles " ))	
        {doSetFiles(name,message.substring(10));}
        else if( message.startsWith( "!clearfiles" ))	
        {doClearFiles(name);}

    }
    
    /**
     * Gets the argument tokens from a string.  If there are no colons in the
     * string then the delimeter will default to space.
     *
     * @param string is the string to tokenize.
     * @return a tokenizer separating the arguments is returned.
     */
    
    public StringTokenizer getArgTokens(String string)	{
	    if(string.indexOf((int) ',') != -1)
	      return new StringTokenizer(string, ",");
	    return new StringTokenizer(string);
	  }
    
    /**
     * Sets files that are to be restricted and logged for violation.
     * Note: there is no list functionality.
     * 
     * @param sender is the user of the bot
     * @param argString is the string containing the file names + extensions
     */
    
    public void doSetFiles(String sender, String argString)	{
    	StringTokenizer argTokens = getArgTokens(argString);
	    
	    while (argTokens.hasMoreTokens())	{
	    	String fileName = argTokens.nextToken();
	    	int index = fileName.indexOf(".");
	    	if (index == -1)	{
	    		m_botAction.sendPrivateMessage(sender, "Invalid file name listed, re-input");
	    		fileNames.clear();
	    		return;
	    	}
	    	fileNames.add(fileName);
	    }
	    m_botAction.sendPrivateMessage(sender, "Restricted files are now: " + fileNames.toString());
    }
    
    /**
     * clears the list of restricted files
     * @param sender
     */
    
    public void doClearFiles(String sender)	{
    	fileNames.clear();
    	m_botAction.sendPrivateMessage(sender, "Restricted file list cleared");
    }
    
    /**
     * Starts the logging functionality, reads the log every 30 seconds.
     * 
     * @param sender is thr user of the bot.
     */
    
    public void doStartLog(String sender)	{
    	if (logging)	{
    		m_botAction.sendPrivateMessage(sender, "Already logging!");
    		return;
    	}
    		logcheck = new TimerTask()	{
        		public void run()	{
        			m_botAction.sendUnfilteredPublicMessage("*log");
        		}
        		
        	};
        	getlogs = new TimerTask()	{
        		public void run()	{
        			m_botAction.sendUnfilteredPublicMessage("*getfile subgame.log");
        		}
        		
        	};
        	m_botAction.scheduleTask(logcheck, 2000, 30000);
        	//m_botAction.scheduleTask(getlogs, 2000, 3600000);
        	m_botAction.sendPrivateMessage(sender, "Starting to check log");
        	logging = true;
    }
    
    /**
     * Stops the logging
     * 
     * @param sender is the user of the bot.
     */
    
    public void doStopLog (String sender)	{
    	if (!logging)	{
    		m_botAction.sendPrivateMessage(sender, "Nothing is being logged!");
    		return;
    	}
    	logcheck.cancel();
    	m_botAction.sendPrivateMessage(sender, "Logging stopped!");
    	logging = false;
    }
    
    /**
     * Handles the log messages and occasionally the arena message
     * that passes through it's filters.
     * 
     * @param arenamessage
     */
    
    public void handleArenas( String arenamessage )	{ 	
    	if (!logging) return;
    	if (ignorelog.contains(arenamessage)) return;
    	
    	try	{
    		String command = arenamessage.substring(arenamessage.indexOf("*getfile"));
        	String violator = arenamessage.substring(arenamessage.indexOf("Ext:")+5, arenamessage.indexOf("(")-1);
        	if (m_opList.isOwner(violator))
        			return;
        	else if (m_opList.isSysop(violator))	{
        		if ( command.startsWith("*getfile " + m_botAction.getArenaName()))	{
        			logEvent( arenamessage );
        			m_botAction.sendChatMessage(1,violator + " getfile'd " + m_botAction.getArenaName() + "'s files! >>> " + command);
        			ignorelog.add(arenamessage);
        		}
        		
        		for ( int i= 0; i<fileNames.size(); i++ )
        		if ( command.equalsIgnoreCase("*getfile " + (String)fileNames.get(i)))	{
        			logEvent( arenamessage );
        			m_botAction.sendChatMessage(1,violator + " getfile'd a restricted file! >>> " + command);
        			ignorelog.add(arenamessage);
        		}
        	}
    	}
    	catch (Exception e)	{}
    }
    
    /**
     * handles all of your commands.
     */
    
    public void handleEvent( Message event ){

        String message = event.getMessage();
        String name = "";
        int type = event.getMessageType();
        
        if (type == Message.REMOTE_PRIVATE_MESSAGE )
        	name = event.getMessager();
        else if (type == Message.PRIVATE_MESSAGE)
        	name = m_botAction.getPlayerName(event.getPlayerID());
        
        if( m_opList.isOwner( name )) handleCommand( name, message );
        if( type == Message.ARENA_MESSAGE)
        	handleArenas(message);
    }
}
