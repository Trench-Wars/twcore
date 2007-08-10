package twcore.bots.bouncerbotdev;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.util.Tools;
import twcore.core.game.Player;

/**
 * A bot designed to kick players off the server who enter into a particular arena
 * without being invited It-- a personal bouncer.
 * 
 * Modifications include the ability to kick all but those on the owners.cfg, 
 * including it's own core and siblings if you're not careful or haven't added.
 * them to the owners.cfg . This bot is intended for dev use. It also has a
 * log checker to ensure nobody's touching files they arn't supposed to, 
 * especially the files of the arena the bot is in; unless you're the 
 * owner of the bot.
 *
 * @author  harvey - major modifications Ice-demon / Ayano
 */
public class bouncerbotdev extends SubspaceBot {
    OperatorList m_opList;
    HashSet<String> invitedPlayers;
    ArrayList<String> log;
    ArrayList<String> ignorelog;
    ArrayList<String> fileNames;
    String bouncemessage;
    TimerTask logCheck;
    TimerTask getLog;
    TimerTask archiveLog;
    File subLog;
    boolean logging;
    boolean realTimeLogging;
    
    /**
     * Initializes.
     * 
     * @param botAction requests botActions.
     */
    
    public bouncerbotdev( BotAction botAction ){
        super( botAction );
        invitedPlayers = new HashSet<String>();
        bouncemessage = "Entering a private arena without being invited is against the rules.  Your insolence has been logged!";
        log = new ArrayList<String>();
        fileNames = new ArrayList<String>();
        ignorelog = new ArrayList<String>();
        logging = false;
        realTimeLogging = false;
        EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.MESSAGE );
        events.request( EventRequester.PLAYER_ENTERED );
        invitedPlayers.add( m_botAction.getBotName().toLowerCase() );
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
     * Returns a time stamp.
     * 
     * @return a time stamp
     */
    
    public String getTimeStamp()	{
    	Calendar c = Calendar.getInstance();
        String timestamp = c.get( Calendar.MONTH ) + "/" + c.get( Calendar.DAY_OF_MONTH )
        + "/" + c.get( Calendar.YEAR ) + ": " + c.get( Calendar.HOUR ) + ":"
        + c.get( Calendar.MINUTE ) + ":" + c.get( Calendar.SECOND );
        return timestamp;
    }
    
    /**
     * Places violations in a log file.
     * 
     * @param event is the violation to be recorded.
     */
    
    public void logEvent( String event )	{
        try{
            PrintWriter out = new PrintWriter( new FileWriter( "bouncerbot.log" ));
            log.add( getTimeStamp() + " - \t" + event );
            for( Iterator<String> i = log.iterator(); i.hasNext(); ){
                out.println( (String)i.next() );
            }
            out.close();
            
        }catch(Exception e){
            Tools.printStackTrace( e );
        }
    }
    
    /**
     * Invites a player
     * 
     * @param sender is the user of the bot.
     * @param invitee is the player to be invited.
     */
    
    public void doInvite(String sender, String invitee){
    	if( invitee.length() > 0 ){
            invitedPlayers.add( invitee.toLowerCase() );
            m_botAction.sendRemotePrivateMessage( invitee, "You have been invited to " + m_botAction.getArenaName() + "!" );
            m_botAction.sendRemotePrivateMessage( sender, invitee + " has been invited" );
            logEvent( invitee + " has been invited" );
        }
    }
    
    /**
     * Clears all guests and ejects them.
     * 
     * @param sender is the user of the bot.
     */
    
    public void doClearInvites(String sender)	{
    	invitedPlayers.clear();
    	Iterator<Player> it = m_botAction.getPlayerIterator();
    	while (it.hasNext())	{
    		Player temp = it.next();
    		String playerName = temp.getPlayerName();
    		if (m_opList.isOwner(playerName))	{
    			m_botAction.sendPrivateMessage(playerName, "You are no longer invited!");
    			m_botAction.sendUnfilteredPrivateMessage(playerName,"*kill");
    		}
    	}
    	m_botAction.sendPrivateMessage( sender, "All invitees have been removed, and ejected!");
    }
    
    /**
     * Sets the bounce message.
     * 
     * @param sender is the user of the bot.
     * @param message is the bounce message.
     */
    
    public void doBounceMessage(String sender, String message)	{
    	bouncemessage = message;
        m_botAction.sendRemotePrivateMessage( sender, "Message set to: " + bouncemessage );
    }
    
    /**
     * Sends the bot to an arena.
     * 
     * @param sender is the user of the bot.
     * @param message is the area to be entered.
     */
    
    public void doGo (String sender, String message)	{
    	m_botAction.changeArena( message );
    	m_botAction.sendRemotePrivateMessage( sender,"Going to " + m_botAction.getArenaName());
    }
    
    /**
     * Sets files that are to be restricted and logged for violation.
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
	    		m_botAction.sendPrivateMessage(sender, "Invalid file name listed, re-input.");
	    		fileNames.clear();
	    		return;
	    	}
	    	fileNames.add(fileName);
	    }
	    String files = fileNames.toString();
	    m_botAction.sendPrivateMessage(sender, "Restricted files are now: " + files);
	    logEvent ( sender + "- Restrictions changed: " + files );
    }
    
    /**
     * Lists the restricted files that are watched
     * 
     * @param sender
     */
    
    public void doListFiles(String sender)	{
    	if (fileNames.isEmpty())
    		m_botAction.sendPrivateMessage(sender,"There are no restricted files.");
    	m_botAction.sendPrivateMessage(sender,"The following files are restricted: " + fileNames.toString());
    }
    
    /**
     * clears the list of restricted files.
     * 
     * @param sender
     */
    
    public void doClearFiles(String sender)	{
    	fileNames.clear();
    	m_botAction.sendPrivateMessage(sender, "Restricted file list cleared");
    	logEvent ( sender + "- Restrictions changed: " + fileNames.toString() );
    }
    
    /**
     * Turns on/off Real time log violation notification.
     *  
     * @param sender is the user of the bot.
     */
    
    public void doRealTime(String sender)	{
    	if (logging)	{
    		m_botAction.sendPrivateMessage(sender, "Already logging!...!stoplog then try again.");
    		return;
    	}
    	realTimeLogging = !realTimeLogging;
    	m_botAction.sendPrivateMessage(sender, "Real Time logging is " + (realTimeLogging? "on" : "off"));
    }
    
    /**
     * Starts the logging functionality.
     * 
     * @param sender is the user of the bot.
     */
    
    public void doStartLog(String sender)	{
    	if (logging)	{
    		m_botAction.sendPrivateMessage(sender, "Already logging!");
    		return;
    	}
    		logCheck = new TimerTask()	{
        		public void run()	{
        			m_botAction.sendUnfilteredPublicMessage("*log");
        		}
        		
        	};
        	getLog = new TimerTask()	{
        		public void run()	{
        			try	{
        				subLog = m_botAction.getDataFile("subgame.log");
            			if (subLog.exists())
            				subLog.delete();
        			}
        			catch (Exception e)	{}
        			m_botAction.sendUnfilteredPublicMessage("*getfile subgame.log");
        		}
        		
        	};
        	archiveLog = new TimerTask()	{
        		public void run()	{
        			try	{
        			subLog = m_botAction.getDataFile("subgame.log");
        			String logName = getLogName();
        			WriteSubLog(logName);
        			ReadLog(logName);
        			}
        			catch (Exception e)	{
        				m_botAction.sendChatMessage(1,"Failed to write log! (sublog not present?)");
        			}
        		}
        		
        	};
        	if(realTimeLogging) m_botAction.scheduleTask(logCheck, 2000, 30000);
        	m_botAction.scheduleTask(getLog, 2000, 3600000);
        	m_botAction.scheduleTask(archiveLog, 300000, 3600000);
        	m_botAction.sendPrivateMessage(sender, "Starting to check log");
        	m_botAction.sendChatMessage(1,"Log monitoring activated - " + getTimeStamp());
        	logEvent ( "Logging started by " + sender );
        	logging = true;
    }
    
    /**
     * Stops the logging.
     * 
     * @param sender is the user of the bot.
     */
    
    public void doStopLog (String sender)	{
    	if (!logging)	{
    		m_botAction.sendPrivateMessage(sender, "Nothing is being logged!");
    		return;
    	}
    	logCheck.cancel();
    	getLog.cancel();
    	archiveLog.cancel();
    	m_botAction.sendPrivateMessage(sender, "Logging stopped!");
    	m_botAction.sendChatMessage(1,"Log monitoring deactivated - " + getTimeStamp());
    	logEvent ( "Logging stopped by " + sender );
    	logging = false;
    }
    
    /**
     * Gets the name of the log from the earliest date on the log.
     * 
     * @returns the dated named for the log or null if an exception.
     */
    
    public String getLogName()	{
    	try	{
    		BufferedReader  in = new BufferedReader( new FileReader( subLog ));
        	String line = "";
        	String target = "";
        	while( (line = in.readLine()) != null )	{
        		target = line;
        	}
        	in.close();
        	return subLog.getParent() + File.separatorChar + "Log Archives" + File.separatorChar + target.substring(0, 10) + ".log";
    	}
    	catch(Exception e)	{
        logEvent ( "Failed to read log file " + subLog.getName() );
        return null;
    	}
    }
    
    /**
     * Writes the log into the archive which is 
     * basically a folder filled with logs.
     * 
     * @param logName is the pathname of the log file.
     */
    
    public void WriteSubLog(String logName) {
    	try	{
    		File logFile = new File ( logName );
    		logFile.getParentFile().mkdirs();
    		String          line;
            BufferedReader  in = new BufferedReader( new FileReader( subLog ));
            PrintWriter     out = new PrintWriter( new BufferedWriter ( new FileWriter(logName)));
            	

            while( (line = in.readLine()) != null )	{
                out.println(line);
            }
            m_botAction.sendChatMessage(1,"Log archive updated. -" + logFile.getName());
            in.close();out.close();
    	}
    	catch(Exception e){
            logEvent ("Failed to write log file: " + logName );
        }
    }
    
    /**
     * Reads the log for any violations.
     * 
     * @param logName is the pathname of the log file.
     */
    
    public void ReadLog(String logName)	{
    	try	{
    		String line;
            BufferedReader  in = new BufferedReader( new FileReader( logName ));

            m_botAction.sendChatMessage(1,"Checking updated log file for violations...");
            while( (line = in.readLine()) != null )	{
                handleLog(line);
            }
            m_botAction.sendChatMessage(1,"Done!");
    	}
    	catch(Exception e){
            logEvent ("Failed to check log file: " + logName );
        }
    }
    
    /**
     * Handles the log messages and occasionally the arena message
     * that passes through it's filters.
     * 
     * @param logmessage is the log message to be checked.
     */
    
    public void handleLog( String logmessage )	{ 	
    	if (!logging) return;
    	if (ignorelog.contains(logmessage)) return;
    	
    	try	{
    		String command = logmessage.substring(logmessage.indexOf("*getfile"));
        	String violator = logmessage.substring(logmessage.indexOf("Ext:")+5, logmessage.indexOf("(")-1);
        	if (m_opList.isOwner(violator))
        			return;
        	else if (m_opList.isSysop(violator))	{
        		if ( command.startsWith("*getfile " + m_botAction.getArenaName()))	{
        			logEvent( logmessage );
        			m_botAction.sendChatMessage(1,violator + " getfile'd " + m_botAction.getArenaName() + "'s files! -" + getTimeStamp() + " >>> " + command);
        			ignorelog.add(logmessage);
        		}
        		
        		for ( int i= 0; i<fileNames.size(); i++ )
        		if ( command.equalsIgnoreCase("*getfile " + (String)fileNames.get(i)))	{
        			logEvent( logmessage );
        			m_botAction.sendChatMessage(1,violator + " getfile'd a restricted file! -" + getTimeStamp() + " >>> " + command);
        			ignorelog.add(logmessage);
        		}
        	}
    	}
    	catch (Exception e)	{}
    }
    
    /**
     * Tell the bot to *CUT*
     * 
     * @param sender
     */
    
    public void doDie(String sender)	{
    	
    	TimerTask dieTask = new TimerTask()	{
    		public void run()	{
    		m_botAction.die();	
    		}
    	};
    	
    	if(logging)
    		doStopLog(sender);
    	m_botAction.sendChatMessage(1,sender + " has told me to commit suicide -" + getTimeStamp());
    	logEvent( sender + " Has given me a a very sharp razor.");
    	m_botAction.scheduleTask(dieTask, 1000);
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
        if( type == Message.ARENA_MESSAGE && realTimeLogging)
        	handleLog(message);
    }
    
    /**
     * Handles all entries and determines if they are legal.
     */
    
    public void handleEvent( PlayerEntered event )	{
        if( m_opList.isOwner(event.getPlayerName())) return;
        if( invitedPlayers.contains( event.getPlayerName().toLowerCase())) return;
        
        m_botAction.sendPrivateMessage( event.getPlayerName(), bouncemessage );
        m_botAction.sendUnfilteredPrivateMessage( event.getPlayerName(), "*kill" );
        m_botAction.sendPublicMessage( event.getPlayerName() + " entered without permission." );
        m_botAction.sendChatMessage( event.getPlayerName() + " went into my guarded arena without asking permission, and was mysteriously disconnected from the server." );
        logEvent( event.getPlayerName() + " entered "+ m_botAction.getArenaName() +" illegally!" );
    }

    /**
     * The bot's morning ass rub
     */
    
    public void handleEvent( LoggedOn event )	{
        m_botAction.joinArena( "#noseeum" );
        m_botAction.sendUnfilteredPublicMessage("?chat=L0gch4t");
        m_opList = m_botAction.getOperatorList();
        logEvent( "Logged in!" );
    }
    
    /**
     * Handles commands.
     * 
     * @param name is the user of the bot.
     * @param message is the command.
     */
    
    public void handleCommand( String sender, String message )	{
        if( message.startsWith( "!invite " ))
            doInvite(sender,message.substring(8));
        else if( message.startsWith( "!message " ))
        	doBounceMessage(sender,message.substring(9));
        else if( message.startsWith( "!go " ))
        	doGo(sender, message.substring(4));
        else if( message.startsWith( "!help" ))	
        	m_botAction.privateMessageSpam(sender, getHelpMessages());
        else if( message.startsWith( "!startlog" ))	
        	doStartLog(sender);
        else if( message.startsWith( "!stoplog" ))	
        	doStopLog(sender);
        else if( message.startsWith( "!setfiles " ))	
        	doSetFiles(sender,message.substring(10));
        else if( message.startsWith( "!listfiles" ))	
        	doListFiles(sender);
        else if( message.startsWith( "!clearfiles" ))	
        	doClearFiles(sender);
        else if( message.startsWith( "!realtime" ))	
        	doRealTime(sender);
        else if( message.startsWith( "!die" ))	
        	doDie(sender);

    }
    
    /**
     * Lists all help messages.
     * @return returns the help message.
     */
    
    public String[] getHelpMessages()
    {
      String[] message =
      {
          "!Invite <name>                            -- Invites <name> to " + m_botAction.getArenaName() + " .",
          "!Message <message>                        -- Sets the bounce message interlopers recieve.",
          "!Go <arena>                               -- Sends the bot to <arena>.",
          "!SetFiles <file1>,<file2>,ect...          -- Sets restricted files.",
          "!ListFiles                                -- Lists curretly restricted files",
          "!ClearInvites                             -- Clears all guests.",
          "!ClearFiles                               -- Clears all restricted files",
          "!RealTime                                 -- Sets real time logging to be on or off.",
          "!StartLog                                 -- Starts the logging functionality.",
          "!StopLog                                  -- Stops the logging functionality.",
          "!help                                     -- Displays this."
      };
      return message;
    }
}
