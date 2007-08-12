package twcore.bots.bouncerbotdev;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.FileArrived;
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
    HashMap<String, File> incoming;
    HashMap<String, Watched_Arena> guarded;
    ArrayList<String> log;
    ArrayList<String> ignorelog;
    ArrayList<String> fileNames;
    String bouncemessage;
    TimerTask logCheck;
    TimerTask getLog;
    File subLog;
    File data;
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
        incoming = new HashMap<String, File>();
        guarded = new HashMap<String, Watched_Arena>();
        bouncemessage = "Entering a private arena without being invited is against the rules.  Your insolence has been logged!";
        log = new ArrayList<String>();
        fileNames = new ArrayList<String>();
        ignorelog = new ArrayList<String>();
        data = new File (m_botAction.getGeneralSettings().getString("Core Location") + File.separatorChar + "Data" + File.separatorChar + "Cfg Archive" + File.separatorChar + "definitions.dat");
        data.getParentFile().mkdirs();
        logging = false;
        realTimeLogging = false;
        EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.MESSAGE );
        events.request( EventRequester.PLAYER_ENTERED );
        events.request( EventRequester.FILE_ARRIVED );
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
    
    public boolean isRestrictedLvz(String violator, String stamp, String lvzName)	{
    	Iterator<Watched_Arena> arenas = guarded.values().iterator();
    	while (arenas.hasNext())	{
    		Watched_Arena temp = arenas.next();
    		Iterator<String> current = temp.myLvz.iterator();
    		while (current.hasNext())	{
    			if (current.next().equals(lvzName))	{
    				m_botAction.sendChatMessage(1,violator + " getfile'd " + temp.myArena + "'s file! ( actual owner: " + temp.myOwner + " ) -" + stamp + " >>> " + lvzName);
    				return true;
    			}
    		}
    	}
    	return false;
    	
    }
    
    public boolean isArenaFile(String fileName)	{
    	if (fileName.endsWith(".cfg"))
    		return true;
    	else if (fileName.endsWith(".lvl"))
    		return true;
    	else return false;
    }
    
    public boolean isArenaOwner(String playerName, String arenaName){
    	if (guarded.get(arenaName).myOwner.equalsIgnoreCase(playerName))
    		return true;
    	return false;
    }
    
    public boolean isArenaFileOwner(String playerName, File arena)	{
    	try	{
        	if (getString("Name",arena).equalsIgnoreCase(playerName))
        		return true;
        	return false;
    	}
    	catch (Exception e)	{
    		return false;
    	}
    }
    
    public String getString(String target,File arena)	{
    	try	{
        	String line;
        	BufferedReader  in = new BufferedReader( new FileReader( arena ));
            while( (line = in.readLine()) != null )	{	
                if (line.startsWith(target + "="))
                	return line.substring(line.indexOf("=")+1);
        	}
            m_botAction.sendPublicMessage("file is null!!");
            return null;
    	}
    	catch (Exception e) {
    		m_botAction.sendChatMessage(1, arena.getPath() + " is non-existant");
    		return null;
    	}
    	
    }
    
    public ArrayList<String> getLvzNames(File arena)	{
    	String argString = getString("LevelFiles",arena);
    	
    	StringTokenizer argTokens = getArgTokens(argString);
    	ArrayList<String> lvzFiles = new ArrayList<String>();
    	while ( argTokens.hasMoreTokens())
    		lvzFiles.add(argTokens.nextToken());
    	return lvzFiles;
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
    		{m_botAction.sendPrivateMessage(sender,"There are no restricted files.");return;}
    	m_botAction.sendPrivateMessage(sender,"The following files are restricted: " + fileNames.toString());
    }
    
    /**
     * clears the list of restricted files.
     * 
     * @param sender
     */
    
    public void doClearFiles(String sender)	{
    	if (fileNames.isEmpty())
    		{m_botAction.sendPrivateMessage(sender,"There are no restricted files.");return;}
    	fileNames.clear();
    	m_botAction.sendPrivateMessage(sender, "Restricted file list cleared");
    	logEvent ( sender + "- Restrictions changed: " + fileNames.toString() );
    }
    
    public void doAddArena (String sender, String arena)	{
    	
	    File arenacfg = new File(data.getParent() + File.separatorChar + arena + ".cfg");
	    if (guarded.containsKey(arena))
	    	m_botAction.sendPrivateMessage(sender, " is already monitored.");
	    else	{
	    	String arenaFileName = arenacfg.getName();
		    incoming.put(arenaFileName, arenacfg);
		    m_botAction.sendUnfilteredPublicMessage("*getfile " + arenaFileName);
		    guarded.put(arena, new Watched_Arena(arena,sender));
		    m_botAction.sendPrivateMessage(sender, arena + " is now monitored");
		    
		    logEvent ( sender + "- Arena monitoring changed: " + arena );
	    }
    }
    
public void doDelArena (String sender, String arena)	{
    	
	    File arenacfg = new File(data.getParent() + File.separatorChar + arena + ".cfg");
	    if (!guarded.containsKey(arena))	{
	    	m_botAction.sendPrivateMessage(sender, arena + " is not watched.");
	    }
	    else	{
	    	if (!isArenaOwner(sender,arena))	{
	    		m_botAction.sendChatMessage(1,"Attempted to remove " + guarded.get(arena).myOwner + "'s arena " + arena + " from the monitored list!");
	    		return;
	    	}
	    	arenacfg.delete();
	    	guarded.remove(arena);
		    m_botAction.sendPrivateMessage(sender, arena + " is no longer monitored");
		    
		    logEvent ( sender + "- Arena monitoring changed: " + arena );
	    }
    }
    
    public void doListArenas(String sender)	{
    	if (!guarded.isEmpty())
    		m_botAction.sendPrivateMessage(sender,"The currently watched arenas are: " + guarded.keySet().toString());
    	else
    		m_botAction.sendPrivateMessage(sender,"No arenas are currently under watch.");
    	
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
        	if(realTimeLogging) m_botAction.scheduleTask(logCheck, 2000, 30000);
        	m_botAction.scheduleTask(getLog, 2000, 3600000);
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
    		String date = logFile.getName().substring( 0, logFile.getName().indexOf(".") );
    		logFile.getParentFile().mkdirs();
    		String          line;
            BufferedReader  in = new BufferedReader( new FileReader( subLog ));
            PrintWriter     out = new PrintWriter( new BufferedWriter ( new FileWriter(logName)));
            	

            while( (line = in.readLine()) != null )	{
            	if (line.startsWith(date))
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
    
    public void WriteDefinitions()	{	
    	try	{
    		 if (data.exists())
    			 data.delete();
    	     DataOutputStream out = new DataOutputStream( new FileOutputStream( data ) );
    	     
    	     Iterator<Watched_Arena> arenas = guarded.values().iterator();
    	    	while (arenas.hasNext())	{
    	    		Watched_Arena temp = arenas.next();
    	    		String line = "#" + temp.myArena + "~" + temp.myOwner + "~" + temp.myLvz.toString() + "#";
    	    		m_botAction.sendPublicMessage(line + " was read in");
    	    		out.writeChars(line);
    	    	}
    	     
    	     out.close(); //TODO
    	   }
    	   catch ( Exception e )	{
    		   m_botAction.sendChatMessage(1,"Could not save definitions " + data.getPath() );
    	   }
    	
    }
    
    public boolean ReadDefinitions()	{
    	return false; //TODO
    }
    
    /**
     * Handles the log messages and occasionally the arena message
     * that passes through it's filters if real time logging is on.
     * 
     * @param logMessage is the log message to be checked.
     */
    
    public void handleLog( String logMessage )	{ 	
    	if (!logging) return;
    	if (ignorelog.contains(logMessage)) return;
    	
    	try	{
    		String fileName = logMessage.substring(logMessage.indexOf("*getfile")+10);
        	String violator = logMessage.substring(logMessage.indexOf("Ext:")+5, logMessage.indexOf("(")-1);
        	if (m_opList.isOwner(violator))
        			return;
        	else if (m_opList.isSysop(violator))	{
        		String stamp = (realTimeLogging? getTimeStamp() : logMessage.substring(0, logMessage.indexOf(": ")));
        		if ( fileName.toLowerCase().startsWith( m_botAction.getArenaName() ))	{
        			logEvent( logMessage );
        			m_botAction.sendChatMessage(1,violator + " getfile'd " + m_botAction.getArenaName() + "'s files! -" + stamp + " >>> " + fileName);
        			ignorelog.add(logMessage);
        			return;
        		}
        		
        		for ( int i= 0; i<fileNames.size(); i++ )
        		if ( fileName.equalsIgnoreCase( (String)fileNames.get(i) ))	{
        			logEvent( logMessage );
        			m_botAction.sendChatMessage(1,violator + " getfile'd a restricted file! -" + stamp + " >>> " + fileName);
        			ignorelog.add(logMessage);
        			return;
        		}
        		if (isArenaFile(fileName))	{
        			String arenaName = logMessage.substring( logMessage.indexOf("(")+1, logMessage.indexOf(")") );
        			if (guarded.containsKey(arenaName))	{
        				if (isArenaOwner(violator,arenaName)) 
        					return;
        				logEvent( logMessage );
            			m_botAction.sendChatMessage(1,violator + " getfile'd " + arenaName +"'s file! ( actual owner: " + guarded.get(arenaName).myOwner + " ) -" + stamp + " >>> " + fileName);
            			ignorelog.add(logMessage);
            			return;
        			}	
        		}
        		 if (fileName.endsWith(".lvz"))
        			if (isRestrictedLvz(violator,stamp,fileName))	{
        				logEvent( logMessage );
        				ignorelog.add(logMessage);
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
        
        if( m_opList.isOwner( name )) handleCommand( name, message.toLowerCase() );
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
        m_botAction.sendChatMessage(1, event.getPlayerName() + " went into my guarded arena without asking permission, and was mysteriously disconnected from the server." );
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
    
    public void handleEvent( FileArrived event )	{
    	String fileName = event.getFileName();
    	String arenaName = fileName.substring(0, fileName.length()-4);
    	m_botAction.sendPublicMessage( fileName + " arrived");
    	
    	if(fileName.endsWith(".log"))	{
    		try	{
    			subLog = m_botAction.getDataFile("subgame.log");
    			String logName = getLogName();
    			WriteSubLog(logName);
    			ReadLog(logName);
    			WriteDefinitions();
    			}
    			catch (Exception e)	{
    				m_botAction.sendChatMessage(1,"Failed to write log! (sublog not present?)");
    			}
    	}
    	
    	if(incoming.containsKey(fileName))
    		try	{
    			File file = m_botAction.getDataFile(fileName);
    			m_botAction.sendPublicMessage("Tring to process " + fileName);
    			File temp = (File)incoming.get(fileName);
    			temp.getParentFile().mkdirs();
    			file.renameTo(temp);
    			if (guarded.containsKey(arenaName))	{
    					Watched_Arena arena = guarded.get(arenaName);
    					String playerName = arena.myOwner;
    					if (isArenaFileOwner(playerName,temp))
    						arena = new Watched_Arena(arena.myArena,playerName,getLvzNames(temp));
    					else	{
    						m_botAction.sendChatMessage(1, playerName + " tried to request for " + arena.myArena + "to be monitored when he/she is not the owner or the arena DNE.");
    						if (temp.delete()) //TODO
    							m_botAction.sendPublicMessage( fileName + " was deleted");
    						else	{
    							m_botAction.sendPublicMessage( temp.getPath() + " could not deleted: attempting force delete.." );
    						}
    						guarded.remove(arenaName);
    					}
    			}
    			incoming.remove(fileName);
    		}
    		catch (Exception e)	{
    			m_botAction.sendChatMessage(1,"Fatal Error: could not receive and process" + fileName);
    		}
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
        else if( message.startsWith( "!clearinvites" ))	
        	doClearInvites(sender);
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
        else if( message.startsWith( "!addarena " ))	
        	doAddArena(sender, message.substring(10));
        else if( message.startsWith( "!delarena " ))	
        	doDelArena(sender, message.substring(10));
        else if( message.startsWith( "!listarenas" ))	
        	doListArenas(sender);
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
          "!AddArena                                 -- Monitors an arena and all related files.",
          "!DelArena                                 -- Stops monitoring an arena and all related files.",
          "!ListArenas                               -- Lists all monitored arenas.",
          "!SetFiles <file1>,<file2>,ect...          -- Sets restricted files.",
          "!ListFiles                                -- Lists curretly restricted files",
          "!ClearInvites                             -- Clears all guests.",
          "!ClearFiles                               -- Clears all restricted files.",
          "!RealTime                                 -- Sets real time logging to be on or off.",
          "!StartLog                                 -- Starts the logging functionality.",
          "!StopLog                                  -- Stops the logging functionality.",
          "!help                                     -- Displays this."
      };
      return message;
    }
    
    private class Watched_Arena	{
    	String myArena;
    	String myOwner;
    	ArrayList<String> myLvz;
    	
    	public Watched_Arena(String arenaName, String Owner){
    		myArena = arenaName;
    		myOwner = Owner;
    	}
    	public Watched_Arena(String arenaName,String arenaOwner,ArrayList<String> arenaLvz){
    		myArena = arenaName;
    		myOwner = arenaOwner;
    		myLvz = arenaLvz;   		
    	}
    	
    	/*public String getName()
    	{return myArena;}
    	public String getOwner()
    	{return myOwner;}
    	public ArrayList<String> getLvz()
    	{return myLvz;}
    	public boolean containsLvz(String lvzName)	
    	{return myLvz.contains(lvzName);}*/
    }
}
