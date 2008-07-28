package twcore.bots.logbot;

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
import java.text.SimpleDateFormat;
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
import twcore.core.events.ArenaList;
import twcore.core.events.FileArrived;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.util.Tools;
import twcore.core.game.Player;

/**
 *
 *This bot is intended purely for dev zone monitoring.
 *Early Code, extremely messy.
 *
 * @author Ayano, fantus
 */
public class logbot extends SubspaceBot {
    private OperatorList m_opList;
    private HashSet<String> invitedPlayers;
    private HashMap<String, File> incoming;
    private HashMap<String, Watched_Arena> guarded;
    private ArrayList<String> ignorelog, fileNames, op;
    private String bouncemessage, hubBot, entity, home, chat,
    		m_bounceDestination ,m_zoneIP ,m_zonePort ,sendtoCmd;
    private TimerTask logCheck, ArenaCheck, getLog, waitReply, RespawnTask;
    private File subLog, arenaData, adminData, botCfg;
    private int maxBots, auto;
    private boolean logging, realTimeLogging, enslaved, waiting, resume;
    
    /**
     * Initializes.
     * 
     * @param botAction requests botActions.
     */
    
    public logbot( BotAction botAction ){
        super( botAction );
        invitedPlayers = new HashSet<String>();
        incoming = new HashMap<String, File>();
        guarded = new HashMap<String, Watched_Arena>();
        bouncemessage = "Entering a private arena without being invited is against the rules.  Your insolence has been logged!";
        fileNames = new ArrayList<String>();
        ignorelog = new ArrayList<String>();
        op = new ArrayList<String>();
        subLog = m_botAction.getDataFile("subgame.log");
        arenaData = new File (m_botAction.getGeneralSettings().getString("Core Location") + File.separatorChar + "data" + File.separatorChar + "Cfg Archive" + File.separatorChar + "arenaDef.dat");
        adminData = new File (m_botAction.getGeneralSettings().getString("Core Location") + File.separatorChar + "data" + File.separatorChar + "Cfg Archive" + File.separatorChar + "adminDef.dat");
        arenaData.getParentFile().mkdirs();
        logging = false;
        hubBot = m_botAction.getBotSettings().getString("HubBot");
        home = m_botAction.getBotSettings().getString("Home");
        chat = m_botAction.getBotSettings().getString("Chat");
        auto = Integer.parseInt(m_botAction.getBotSettings().getString("Auto Start"));
        maxBots = m_botAction.getBotSettings().getInt("Max Bots");
        m_zoneIP = m_botAction.getGeneralSettings().getString( "Server" );
        m_zonePort = m_botAction.getGeneralSettings().getString( "Port" );
        m_bounceDestination = ":kill:";
        sendtoCmd = m_botAction.getBotSettings().getString("sendtoCmd");
        EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.MESSAGE );
        events.request( EventRequester.PLAYER_ENTERED );
        events.request( EventRequester.FILE_ARRIVED );
        events.request( EventRequester.ARENA_LIST );
        invitedPlayers.add( m_botAction.getBotName().toLowerCase() );
        invitedPlayers.add( hubBot.toLowerCase() );
        //Slaves are our friends! (Edited by fantus)
        for (int i = 2; i <= maxBots; i++)
            invitedPlayers.add( m_botAction.getBotSettings().getString("Name" + i).toLowerCase() );
    }

    /**
     * Gets the argument tokens from a string.  If there are no colons in the
     * string then the delimeter will default to space.
     *
     * @param string is the string to tokenize.
     * @return a tokenizer separating the arguments is returned.
     */
    
    public StringTokenizer getArgTokens(String string, String token)	{
	    if(string.indexOf(token) != -1)
	      return new StringTokenizer(string, token);
	    return new StringTokenizer(string);
	  }
    
    /**
     * Trims the brackets off a string for parsing.
     * 
     * @param list is an arraylist tostring.
     * @return a trimmed string.
     */
    
    public String TrimList (String list)	{
    	try	{
    		return list.substring(list.indexOf("[")+1,list.indexOf("]"));
    	}
    	catch (Exception e)	{return ("");}
    }
    
    /**
     * Re-spawns the slave if it's been d/c .
     */
    
    public void RespawnSlave()	{
    	m_botAction.sendSmartPrivateMessage( hubBot, "!remove " + entity);
    	m_botAction.sendSmartPrivateMessage( hubBot, "!spawn logbot");
    	waitReply.cancel();
    	ArenaCheck.cancel();
    	RespawnTask = new TimerTask()	{
			public void run()	{
				m_botAction.sendChatMessage(1,"Log monitoring deactivated: " +
						"Respawn failed, " + entity + " has been lost. - " + getTimeStamp());
				logCheck.cancel();getLog.cancel();
				logEvent( "Could not replace slave! switching to standalone." );
				waiting = false;resume = false;
				entity = null;
			}
		};
		waiting = true;
		resume = true;
		m_botAction.scheduleTask(RespawnTask, 60000);
		m_botAction.sendChatMessage(1,"Arena monitoring paused: Attempting to " +
				"respawn " + entity + " - " + getTimeStamp());
		logEvent ( "Attempting to respawn "+ entity);
    }
    
    /**
     * Checks wither the slave is still present.
     */
    
    public void CheckSlave()	{
    	m_botAction.sendSmartPrivateMessage( entity, "here?");
    	waitReply = new TimerTask()	{
	  		public void run()	
	  		{RespawnSlave();}
	      };
	     waiting = true;
	     m_botAction.scheduleTask(waitReply, 2000);
    }
    
    /**
     * Returns true is the lvz in question is monitored
     * 
     * @param violator
     * @param stamp
     * @param lvzName
     * @return
     */
    
    public boolean isRestrictedLvz(String violator, String stamp, String lvzName)	{
    	Iterator<Watched_Arena> arenas = guarded.values().iterator();
    	while (arenas.hasNext())	{
    		Watched_Arena temp = arenas.next();
    			if (temp.containsLvz(lvzName))	{
    				if (violator.equals(temp.myOwner)) return false;
    				m_botAction.sendChatMessage(1,violator + " altered " + temp.myArena + "'s files! ( actual owner: " + temp.myOwner + " ) -" + stamp + " >>> " + lvzName);
    				return true;
    			}
    	}
    	return false;
    	
    }
    
    /**
     * Checks wither or not the file is an arena file
     * and thus pull an arena name from it.
     * 
     * @param fileName is the full file name of the file.
     * @return true if the file is indeed an arena file.
     */
    
    public boolean isArenaFile(String fileName)	{
    	if (fileName.endsWith(".cfg"))
    		return true;
    	else if (fileName.endsWith(".lvl"))
    		return true;
    	else return false;
    }
    
    /**
     * Checks wither or not this bot is a slave.
     * 
     * @return false if yes, true if not.
     */
    
    public boolean isSlave()	{
    	try	{
        	if ((entity = m_botAction.getBotSettings().getString("Name1")).equals(m_botAction.getBotName()))	{
        		entity = m_botAction.getBotSettings().getString("Name2");
        		return false;
        	}
        	return true;
    	}
    	catch (Exception e)	{
    		logEvent ( " Could not read bot cfg: " + botCfg.getPath() + " -StandAlone On" );
    		entity = null;
    		return false;
    	}
    }
    
    /**
     * Checks wither or not dever is the arena owner
     * 
     * @param playerName is the submitting dever.
     * @param arenaName is the name of the arena to be checked
     * @return true if the dever is the owner of the arena
     */
    
    public boolean isArenaOwner(String playerName, String arenaName){
    	if (guarded.get(arenaName).myOwner.equalsIgnoreCase(playerName))
    		return true;
    	return false;
    }
    
    /**
     * Checks wither or the the submitted arena was indeed submitted
     * by the arena owner.
     * 
     * @param playerName
     * @param arena
     * @return
     */
    
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
    
    /**
     * Helper method that returns a line from a specified
     * key string on the first occurrence from file.
     * 
     * @param target is the key string.
     * @param arena is the arena file.
     * @return the desired line following the key string.
     */
    
    public String getString(String target,File file)	{
    	try	{
        	String line;
        	BufferedReader  in = new BufferedReader( new FileReader( file));
            while( (line = in.readLine()) != null )	{	
                if (line.startsWith(target + "="))	{
                	in.close();
                	return line.substring(line.indexOf("=")+1);
                }
        	}
            in.close();
            logEvent ( "file is null!!" );
            return null;
    	}
    	catch (Exception e) {
    		logEvent ( file.getPath() + " is non-existant" );
    		return null;
    	}
    	
    }
    
    /**
     * Returns an ArrayList of lvz strings from a cfg file.
     * 
     * @param arena is a cfg file.
     * @return an arraylist of the lvz files
     */
    
    public ArrayList<String> getLvzNames(File arena)	{
    	String argString = getString("LevelFiles",arena);
    	return ParseString(argString);
    }
    
    public ArrayList<String> getLvzNames(String argString)	{
    	return ParseString(argString);
    }
    
    public ArrayList<String> ParseString (String argString)	{
    	try	{
    		StringTokenizer argTokens = getArgTokens(argString,",");
        	ArrayList<String> parts = new ArrayList<String>();
        	while ( argTokens.hasMoreTokens())
        		parts.add(argTokens.nextToken().trim().toLowerCase());
        	return parts;
    	}
    	catch (Exception e){
    		return new ArrayList<String>();
    	}
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
     * Gets a string date as the name for the log file by day/month/year
     * ie: "Aug 25 2008.log"
     * @return
     */
    
    public String getLogName()	{ 	
    	String logName = new SimpleDateFormat("MMM dd yyyy").format
    	(Calendar.getInstance().getTime());
    	return subLog.getParent() + File.separatorChar + 
    	"Log Archives" + File.separatorChar + logName + ".log";
    }
    
    /**
     * Places violations in a log file.
     * 
     * @param event is the violation to be recorded.
     */
    
    public void logEvent( String event )	{
        try{
            PrintWriter out = new PrintWriter( new FileWriter( 
            		m_botAction.getGeneralSettings().getString("Core Location") 
            		+ File.separatorChar + "logs" + File.separatorChar + m_botAction.getBotName() + "(Prema-log).log" ,true));
            event = getTimeStamp() + " - \t" + event;
            out.println( event );
            out.close();
            
        }catch(Exception e){
            Tools.printStackTrace( e );
        }
    } 
    
    /**
     * Invites a player
     * 
     * @param sender is an operator+
     * @param invitee is the player to be invited.
     */
    
    public void doInvite(String sender, String invitee){
    	if( invitee.length() > 0 ){
            invitedPlayers.add( invitee.toLowerCase() );
            m_botAction.sendSmartPrivateMessage( invitee, "You have been invited to " + m_botAction.getArenaName() + "!" );
            m_botAction.sendSmartPrivateMessage( sender, invitee + " has been invited" );
            logEvent( invitee + " has been invited" );
        }
    }
    
    /**
     * Clears all guests and ejects them.
     * 
     * @param sender is an operator+
     */
    
    public void doClearInvites(String sender)	{
    	invitedPlayers.clear();
    	Iterator<Player> it = m_botAction.getPlayerIterator();
    	while (it.hasNext())	{
    		Player temp = it.next();
    		String playerName = temp.getPlayerName();
    		if (!m_opList.isOwner(playerName))	{
    			m_botAction.sendSmartPrivateMessage(playerName, "You are no longer invited!");
    			m_botAction.sendUnfilteredPrivateMessage(playerName,"*kill");
    		}
    	}
    	m_botAction.sendSmartPrivateMessage( sender, "All invitees have been removed, and ejected!");
    }
    
    /**
     * Sets the bounce message.
     * 
     * @param sender is an operator+
     * @param message is the bounce message.
     */
    
    public void doBounceMessage(String sender, String message)	{
    	bouncemessage = message;
        m_botAction.sendSmartPrivateMessage( sender, "Message set to: " + bouncemessage );
    }
    
    /**
     * Sends the bot to an arena.
     * 
     * @param sender is an operator+
     * @param message is the area to be entered.
     */
    
    public void doGo (String sender, String message)	{
    	m_botAction.changeArena( message );
    	m_botAction.sendSmartPrivateMessage( sender,"Going to " + m_botAction.getArenaName());
    }
    
    /**
     * Sets files that are to be restricted and logged for violation.
     * 
     * @param sender is an operator+
     * @param argString is the string containing the file names + extensions
     */
    
    public void doSetFiles(String sender, String argString)	{
    	StringTokenizer argTokens = getArgTokens(argString,",");
	    
	    while (argTokens.hasMoreTokens())	{
	    	String fileName = argTokens.nextToken();
	    	int index = fileName.indexOf(".");
	    	if (index == -1)	{
	    		m_botAction.sendSmartPrivateMessage(sender, "Invalid file name listed, re-input.");
	    		fileNames.clear();
	    		return;
	    	}
	    	fileNames.add(fileName);
	    }
	    String files = fileNames.toString();
	    m_botAction.sendSmartPrivateMessage(sender, "Restricted files are now: " + files);
	    logEvent ( sender + "- Restrictions changed: " + files );
    }
    
    /**
     * Lists the restricted files that are watched
     * 
     * @param sender is an operator+
     */
    
    public void doListFiles(String sender)	{
    	if (fileNames.isEmpty())
    		{m_botAction.sendSmartPrivateMessage(sender,"There are no restricted files.");return;}
    	m_botAction.sendSmartPrivateMessage(sender,"The following files are restricted: " + fileNames.toString());
    }
    
    /**
     * clears the list of restricted files.
     * 
     * @param sender is an operator+
     */
    
    public void doClearFiles(String sender)	{
    	if (fileNames.isEmpty())
    		{m_botAction.sendSmartPrivateMessage(sender,"There are no restricted files.");return;}
    	fileNames.clear();
    	m_botAction.sendSmartPrivateMessage(sender, "Restricted file list cleared");
    	logEvent ( sender + "- Restrictions changed: " + fileNames.toString() );
    }
    
    /**
     * Submits an arena to be monitored, if the dever is not the arena owner
     * of the submitted file, a quite violation is logged, unaware to the
     * dever.
     * 
     * @param sender is the submitting dever.
     * @param arena is the arena name to be monitored.
     */
    
    public void doAddArena (String sender, String arena)	{
    	
	    File arenacfg = new File(arenaData.getParent() + File.separatorChar + arena + ".cfg");
	    if (guarded.containsKey(arena))
	    	m_botAction.sendSmartPrivateMessage(sender, " is already monitored.");
	    else if (!arena.startsWith("#"))
	    		m_botAction.sendSmartPrivateMessage(sender, "Arena must be private to be monitored");
	    else	{
	    	String arenaFileName = arenacfg.getName();
		    incoming.put(arenaFileName, arenacfg);
		    m_botAction.sendUnfilteredPublicMessage("*getfile " + arenaFileName);
		    guarded.put(arena, new Watched_Arena(arena,sender));
		    m_botAction.sendSmartPrivateMessage(sender, arena + " and all related files are now monitored");
		    
		    logEvent ( sender + "- Arena monitoring changed: Arena added -" + arena );
	    }
    }
    
    /**
     * Removes an arena from the monitored list, if the dever
     * is not the arena owner, nothing happens and a quiet violation
     * is logged
     * 
     * @param sender is the submitting dever.
     * @param arena is the arena to be removed from monitoring.
     */
    
    public void doDelArena (String sender, String arena)	{
    	
	    File arenacfg = new File(arenaData.getParent() + File.separatorChar + arena + ".cfg");
	    if (!guarded.containsKey(arena))	{
	    	m_botAction.sendSmartPrivateMessage(sender, arena + " is not monitored.");
	    }
	    else	{
	    	if (!isArenaOwner(sender,arena))	{
	    		m_botAction.sendChatMessage(1,sender + " attempted to remove " + guarded.get(arena).myOwner + "'s arena " + arena + " from the monitored list!");
	    		return;
	    	}
	    	arenacfg.delete();
	    	guarded.remove(arena);
		    m_botAction.sendSmartPrivateMessage(sender, arena + " is no longer monitored");
		    
		    logEvent ( sender + "- Arena monitoring changed: arena removed -" + arena );
	    }
    }
    
    /**
     * Invites an individual to a monitored arena
     * 
     * @param sender is the dever submitting the invitee.
     * @param argString is the string containg the arena name and invitee.
     */
    
    public void doArenaInvite (String sender, String argString)	{
    	StringTokenizer argTokens = getArgTokens(argString,",");
    	int numArgs = argTokens.countTokens();
    	if (numArgs !=2)	{
    		m_botAction.sendSmartPrivateMessage(sender,"Improper syntax, use <arena>,<name>");
    		return;
    	}
    	try	{
    		String arena = argTokens.nextToken();
    		String invitee = argTokens.nextToken();
    		if (!guarded.containsKey(arena))	{
    	    	m_botAction.sendSmartPrivateMessage(sender, arena + " is not monitored.");
    	    	return;
    	    }
    	    else	{
    	    	if (!isArenaOwner(sender,arena))	{
    	    		m_botAction.sendChatMessage(1,sender + " attempted to invite " + invitee + " to " + arena + " ( " + guarded.get(arena).myOwner + "'s arena. )");
    	    		return;
    	    	}
    	    }
    		if (guarded.get(arena).myGuests.contains(invitee))	{
    			m_botAction.sendSmartPrivateMessage(sender, invitee + " is already invited.");
	    		return;
    		}
    		guarded.get(arena).addGuest(invitee);
    		m_botAction.sendSmartPrivateMessage(sender, invitee + " is now invited to " + arena);
    	}
    	catch (Exception e)	{
    		m_botAction.sendSmartPrivateMessage(sender,"Improper syntax, use <arena>,<name>");
    		logEvent (e.getMessage());
    	}
    }
    
    /**
     * Clears all invites to a monitored arena arena.
     * 
     * @param sender is the dever requesting a guest list wipe.
     * @param arena is the arena to be wiped.
     */
    
    public void doClearArenaInvites(String sender, String arena)	{
    	if (!guarded.containsKey(arena))	{
	    	m_botAction.sendSmartPrivateMessage(sender, arena + " is not monitored.");
	    	return;
	    }
	    else	{
	    	if (!isArenaOwner(sender,arena))	{
	    		m_botAction.sendChatMessage(1,sender + " attempted to clear all invites to" + arena + " ( " + guarded.get(arena).myOwner + "'s arena. )");
	    		return;
	    	}
	    }
    	guarded.get(arena).myGuests.clear();
    	m_botAction.sendSmartPrivateMessage(sender, arena + " has no guest list now.");
    }
    
    /**
     * Lists all monitored arenas.
     * 
     * @param sender is an operator+
     */
    
    public void doListArenas(String sender)	{
    	if (!guarded.isEmpty())
    		m_botAction.sendSmartPrivateMessage(sender,"The currently watched arenas are: " + guarded.keySet().toString());
    	else
    		m_botAction.sendSmartPrivateMessage(sender,"No arenas are currently under watch.");
    	
    }
    
    /**
     * Adds an operator.
     * 
     * @param sender is an owner of the bot.
     * @param operator is the player is to be assigned.
     */
    
    public void doAddOp(String sender, String operator)	{
    	if (!op.contains(operator))	{
    		op.add(operator);
    		m_botAction.sendSmartPrivateMessage(sender, operator + " is now an operator");
    	}
    	else
    		m_botAction.sendSmartPrivateMessage(sender, operator + " is already an Operator");
    }
    
    /**
     * Adds an operator.
     * 
     * @param sender is an owner of the bot.
     * @param operator is the operator to be removed.
     */
    
    public void doDelOp(String sender, String operator)	{
    	if (op.contains(operator))	{
    		op.remove(operator);
    		m_botAction.sendSmartPrivateMessage(sender, operator + " was removed from his/her duties");
    	}
    	else
    		m_botAction.sendSmartPrivateMessage(sender, operator + " is not an Operator");
    }
    
   /**
    * Lists all bot operators
    * 
    * @param sender is the owner of the bot.
    */
    
    public void doListOp(String sender)	{
    	if (op.isEmpty())
    		m_botAction.sendSmartPrivateMessage(sender,"No operators listed!");
    	else
    		m_botAction.sendSmartPrivateMessage(sender,"The current operators are: " + op.toString());
    }
    
    /**
     * Turns on/off Real time log violation notification.
     *  
     * @param sender is the user of the bot.
     */
    
    public void doRealTime(String sender)	{
    	if (logging)	{
    		m_botAction.sendSmartPrivateMessage(sender, "Already logging!...!stoplog then try again.");
    		return;
    	}
    	realTimeLogging = !realTimeLogging;
    	m_botAction.sendSmartPrivateMessage(sender, "Real Time logging is " + (realTimeLogging? "on" : "off"));
    	m_botAction.sendChatMessage(1,"Log monitoring set to start with 'Real Time' mode " + (realTimeLogging? "on" : "off"));
    }
    
    /**
     * Starts the logging functionality.
     * 
     * @param sender is the user of the bot.
     */
    
    public void doStartLog(String sender)	{
    	if (logging)	{
    		m_botAction.sendSmartPrivateMessage(sender, "Already logging!");
    		return;
    	}
    		logCheck = new TimerTask()	{
        		public void run()	{
        			m_botAction.sendUnfilteredPublicMessage("*log");
        		}
        		
        	};
        	ArenaCheck = new TimerTask()	{
        		public void run()	{
        			m_botAction.requestArenaList();
        		}
        		
        	};
        	getLog = new TimerTask()	{
        		public void run()	{
        			m_botAction.sendUnfilteredPublicMessage("*getfile subgame.log");
        		}
        		
        	};
        	if(realTimeLogging)	m_botAction.scheduleTask(logCheck, 2000, 30000);
        	m_botAction.scheduleTask(getLog, 2000, 3600000);
        	m_botAction.scheduleTask(ArenaCheck, 2000, 30000);
        	m_botAction.sendSmartPrivateMessage(sender, "Starting to check log");
        	m_botAction.sendChatMessage(1,"Log monitoring activated by "+ sender +
        			" - " + getTimeStamp());
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
    		m_botAction.sendSmartPrivateMessage(sender, "Nothing is being logged!");
    		return;
    	}
    	logCheck.cancel();
    	ArenaCheck.cancel();
    	getLog.cancel();
    	m_botAction.sendSmartPrivateMessage(sender, "Logging stopped!");
    	m_botAction.sendChatMessage(1,"Log monitoring deactivated: " +
    			"Stopped by "+ sender + " - " + getTimeStamp());
    	logEvent ( "Logging stopped by " + sender );
    	logging = false;
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
    		String date = logFile.getName().substring( 0, 6);
    		logFile.getParentFile().mkdirs();
    		String          line;
    		String			myName = m_botAction.getBotName();
            BufferedReader  in = new BufferedReader( new FileReader( subLog ));
            PrintWriter     out = new PrintWriter( new BufferedWriter ( new FileWriter(logName)));
            	

            while( (line = in.readLine()) != null )	{
            	try	{
            	if (line.substring(4,10).startsWith(date) && !line.substring(line.indexOf("Ext:")+5).startsWith(myName))
            		out.println(line);
            	}
            	catch (Exception e)	{}
            }
            m_botAction.sendChatMessage(1,"Log archive updated. - " + logFile.getName());
            in.close();out.close();
    	}
    	catch(Exception e){
            logEvent ( "Failed to write log file: " + logName );
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
            in.close();
            m_botAction.sendChatMessage(1,"Updating definitions...");
    	}
    	catch(Exception e){
            logEvent ( "Failed to check log file: " + logName );
        }
    }
    
    /**
     * Saves monitored arenas.
     */
    
    public void WriteDefinitions()	{
    	File arenaTemp = new File(arenaData.getParent() + File.separatorChar + "arenaDef.tmp");
    	File adminTemp = new File(arenaData.getParent() + File.separatorChar + "adminDef.tmp");
    	
    	try	{
    		DataOutputStream out = new DataOutputStream( new FileOutputStream( arenaTemp ) );
   	     	DataOutputStream out2 = new DataOutputStream( new FileOutputStream( adminTemp ) );
   	     
   	     	Iterator<Watched_Arena> arenas = guarded.values().iterator();
   	     	while (arenas.hasNext())	{
   	    		Watched_Arena temp = arenas.next();
   	    		String line = (temp.myArena + "~" + temp.myOwner + temp.myLvz.toString() + "~" + temp.myGuests.toString());
   	    		out.writeUTF(line);
   	     	}if (!fileNames.isEmpty())
      	    	out2.writeUTF("@" + fileNames.toString());
   	    	if (!op.isEmpty())
   	    		out2.writeUTF("$" + op.toString());
   	    		out2.close();
   	    		out.close();
   	    		
   	    		arenaData.delete();adminData.delete();
   	    		arenaTemp.renameTo(arenaData);
   	    		adminTemp.renameTo(adminData);
    	   }
    	   catch ( Exception e )	{
    		   logEvent ( "Could not save some or any definitions " + arenaData.getParent() );
    	   }
    	
    }
    
    /**
     * Reads saved definitions if any.
     */
    
    public void ReadDefinitions()	{
    	String line;
    	String arena;
    	String owner;
    	ArrayList<String> lvz;
    	ArrayList<String> guests;
    	ArrayList<String> ops;
    	ArrayList<String> files;
    	try {
    		DataInputStream in = new DataInputStream( new FileInputStream( arenaData ) );
    		DataInputStream in2 = new DataInputStream( new FileInputStream( adminData ) );
    		while (in.available() != 0)	{
    			line = in.readUTF();
        		arena = line.substring(0, line.indexOf('~'));
        		owner = line.substring(arena.length()+1, line.indexOf('['));
        		lvz = getLvzNames( TrimList(line.substring(line.indexOf(owner),line.indexOf(owner) + owner.length()-1)) );
        		guests = ParseString( TrimList(line.substring(line.indexOf("]~")+2)) );
        		Watched_Arena temp = new Watched_Arena(arena,owner,lvz,guests);
        		guarded.put(arena, temp);
        	}
    		while (in2.available() != 0)	{
    			line = in2.readUTF();
    			if (line.startsWith("@"))	{
    				files = ParseString(TrimList(line.substring(1)));
    				fileNames = files;
    			}else if (line.startsWith("$"))	{
    				ops = ParseString(TrimList(line.substring(1)));
    				op = ops;
    			}}
    		in2.close();
        	in.close();
    	}
    	catch (Exception e)	{
    		logEvent ( "Could not read some or any definitions, starting new definitions" );
    		fileNames.clear();
    		op.clear();
    		guarded.clear();
    		arenaData.delete();
    		adminData.delete();
    	}
    }
    
    /**
     * Updates the restricted files for each arena to reflect lvz file
     * additions/removals.
     */
    
    public void UpdateArenas()	{
    	if (guarded.isEmpty())
    		return;
    	Iterator<String> it = guarded.keySet().iterator();
    	while (it.hasNext())	{
    		String arenacfg = it.next() + ".cfg";
    		File temp = new File ( arenaData.getParent() + File.separatorChar + arenacfg);
    		m_botAction.sendUnfilteredPublicMessage("*getfile " + arenacfg);
    		incoming.put(arenacfg, temp);
    	}
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
    		String fileName = logMessage.substring(logMessage.indexOf("*"));
    		if (fileName.startsWith("*getfile") || fileName.startsWith("*putfile"))
    			fileName = fileName.substring(9);
    		else
    			return;
    		String violator = logMessage.substring(logMessage.indexOf("Ext:")+5, logMessage.indexOf("(")-1);
    		
        	if (m_opList.isOwner(violator.toLowerCase()) || op.contains(violator.toLowerCase()))
        			return;
        	
        	else if (m_opList.isSysop(violator))	{
        		boolean violation = false;
        		String stamp = (logMessage.substring(0, logMessage.indexOf(": ")));
        		
        		if ( fileName.toLowerCase().startsWith( m_botAction.getArenaName() ))	{
        			m_botAction.sendChatMessage(1,violator + " altered " + m_botAction.getArenaName() + "'s files! -" + stamp + " >>> " + fileName);
        			violation = true;
        		}
        		
        		if (isArenaFile(fileName))	{
        			String arenaName = fileName.substring(0, fileName.indexOf(".")).toLowerCase();
        			if (guarded.containsKey(arenaName))	{
        				if (isArenaOwner(violator,arenaName)) 
        					return;
            			m_botAction.sendChatMessage(1,violator + " altered " + arenaName +"'s files! ( actual owner: " + guarded.get(arenaName).myOwner + " ) -" + stamp + " >>> " + fileName);
            			violation = true;
        			}	
        		}
        		
        		if (fileNames.contains(fileName.toLowerCase()))	{
        			m_botAction.sendChatMessage(1,violator + " altered a restricted file! -" + stamp + " >>> " + fileName);
        			violation = true;
        		}
        		
        		 if (fileName.endsWith(".lvz"))
        			if (isRestrictedLvz(violator,stamp,fileName))	{
        				violation = true;
        			}
        		if(violation)	{
        			logEvent( logMessage );
        			ignorelog.add(logMessage);
        		}
        			
        			
        	}
    	}
    	catch (Exception e)	{}
    }
    
    /**
     * Handles Server warnings
     */
    
    public void handleWarnings (String message)	{
    	m_botAction.sendChatMessage(1,"SERVER_ERROR RECIEVED: " + message);
    	logEvent ("SERVER WARNING: " + message);
    }
    
    /**
     * Handles messages from the hub-bot.
     * 
     * @param message
     */
    
    public void handleHubBotCmd(String message)	{
    	if(message.equals("Maximum number of bots of this type has been reached.") ||
    		       message.equals("Bot failed to log in."))
    	{
    		enslaved = false;
    		entity = null;
    		logEvent ( "Could not acquire slave! (too many bots) moving to standalone." );
    	}
    	else if (message.startsWith("Subspace only"))	{
    		logEvent ("Waiting for " + entity + " to spawn");
    	}
    }
    
    /**
     * Sends a kill bot to the arena.
     * 
     * @param arena is the arena to visit;
     */
    
    public void sendKillBot (String violators)	{
        m_botAction.sendSmartPrivateMessage(entity,"!action " + m_bounceDestination);
    	m_botAction.sendSmartPrivateMessage(entity,"!kill " + violators);
    	waitReply = new TimerTask()	{
	  		public void run()	//UNUSED
	  		{RespawnSlave();}
	      };
	      m_botAction.scheduleTask(waitReply, 10000);
    }
    
    /**
     * Sends a bouncer to the arena.
     * 
     * @param arenas is a command string.
     */
    
    public void sendBounceBot (String arenas)	{
        m_botAction.sendSmartPrivateMessage(entity,"!action " + m_bounceDestination);
    	m_botAction.sendSmartPrivateMessage(entity,"!bounce " + arenas);
    	waitReply = new TimerTask()	{
	  		public void run()	
	  		{RespawnSlave();}
	      };
	  m_botAction.scheduleTask(waitReply, 10000);
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
    	if(entity != null)
    		m_botAction.sendSmartPrivateMessage(entity, "!die");
    	WriteDefinitions();
    	m_botAction.sendChatMessage(1,sender + " has told me to commit suicide - " + getTimeStamp());
    	logEvent( sender + " Has given me a a very sharp razor.");
    	m_botAction.scheduleTask(dieTask, 1000);
    }
    
    /**
     * handles all messages.
     */
    
    public void handleEvent( Message event ){
        String message = event.getMessage();
        String name = "";
        int type = event.getMessageType();
        
        if (type == Message.REMOTE_PRIVATE_MESSAGE )
        	name = event.getMessager();
        else if (type == Message.PRIVATE_MESSAGE)
        	name = m_botAction.getPlayerName(event.getPlayerID());
        else if ( type == Message.SERVER_ERROR && !enslaved && entity != null)
        	handleWarnings(message);
        else if ( type == Message.ARENA_MESSAGE && realTimeLogging)
        	handleLog(message);
        
        if (name == null) 
        	return;
        if (name.equalsIgnoreCase(hubBot))
        	handleHubBotCmd(message);
        else if (entity != null && entity.equalsIgnoreCase(name))
        	handleEntityCommand(message);
        else if (enslaved || waiting) 
        	return;      
        else if ( m_opList.isSysop( name ) ) 
        	handleStandAloneCommand(name, message.toLowerCase() );
    }
    
    /**
     * Handles all entries and determines if they are legal.
     */
    
    public void handleEvent( PlayerEntered event )	{
    	if( enslaved ) return;
        if( m_opList.isOwner(event.getPlayerName())) return;
        if( invitedPlayers.contains( event.getPlayerName().toLowerCase())) return;
        
        m_botAction.sendPrivateMessage( event.getPlayerName(), bouncemessage );
        if ( m_bounceDestination.equals( ":kill:" ) ) { 
            m_botAction.sendUnfilteredPrivateMessage(event.getPlayerName(),"*kill");
            m_botAction.sendChatMessage(1, event.getPlayerName() + " went into my guarded arena without asking permission, and was mysteriously disconnected from the server." );
        } else { 
            m_botAction.sendUnfilteredPrivateMessage(event.getPlayerName(), sendtoCmd + " " + m_bounceDestination);
            m_botAction.sendChatMessage(1, event.getPlayerName() + " went into my guarded arena without asking permission, and was mysteriously relocated on the server." );
        }
        m_botAction.sendPublicMessage( event.getPlayerName() + " entered without permission." );
        logEvent( event.getPlayerName() + " entered "+ m_botAction.getArenaName() +" illegally!" );
    }

    /**
     * The bot's morning ass rub
     */
    
    public void handleEvent( LoggedOn event )	{
    	m_botAction.joinArena(home);
        m_botAction.sendUnfilteredPublicMessage("?chat=" + chat);
        if (!isSlave())	{
    		enslaved = false;
    		ReadDefinitions();
    		TimerTask waitSpawn = new TimerTask()	{
    			public void run()	{
    				m_botAction.sendSmartPrivateMessage(hubBot, "!spawn logbot");
    			}
    		};
    		waitReply = new TimerTask()	{
    			public void run()	{
    				waiting = false;
    				entity = null;
    				logEvent( "Could not acquire slave! switching to standalone." );
    			}
    		};
    		m_botAction.scheduleTask(waitSpawn, 3000);
    		m_botAction.scheduleTask(waitReply, 120000);
    	}
    	else	{
    		enslaved = true;
    		m_botAction.sendSmartPrivateMessage(entity, "slave");
    		waitReply = new TimerTask()	{
    			public void run()	{
    				waiting = false;
    				enslaved = false;
    				entity = null;
    				logEvent( "Could not allocate master! switching to standalone." );
    			}
    		};
    		m_botAction.scheduleTask(waitReply, 5000);
    	}
        waiting = true;
        m_opList = m_botAction.getOperatorList();
        logEvent( "Logged in as " + (enslaved? "Slave" : "Master") );
        //Lets check if it is the master first (edited by fantus)
        if (!enslaved && entity != null && auto == 1)	{
        	TimerTask DelayStart = new TimerTask()	{
			public void run()	{
				doStartLog("Auto-Start");
			}};m_botAction.scheduleTask(DelayStart, 120000);}
        
    }
    
    /**
     * Handles arena list events for processing by
     * the security bot. If there are no arenas to 
     * check, it will check if security bot is still
     * present.
     */
    
    public void handleEvent(ArenaList event)
    {
      String[] arenaNames = event.getArenaNames();
      String arenaName;
      
      String commandstr = "";
      for(int index = 0; index < arenaNames.length; index++)
      {
        arenaName = arenaNames[index].toLowerCase();
        if(guarded.containsKey(arenaName))	{
        	commandstr+=(guarded.get(arenaName).toCheckable());
        }
      }
      if (commandstr != "")
    	  sendBounceBot(commandstr);
      else
    	  CheckSlave();
    }
    
    /**
     * Handles received cfg and log files.
     */
    
    public void handleEvent( FileArrived event )	{
    	String fileName = event.getFileName();
    	String arenaName = fileName.substring(0, fileName.length()-4);
    	
    	if(fileName.endsWith(".log"))	{
    		try	{
    			String logName = getLogName();
    			WriteSubLog(logName);
    			ReadLog(logName);
    			WriteDefinitions();
    			UpdateArenas();
    			}
    			catch (Exception e)	{
    				logEvent ("Failed to write log! (sublog not present?)");
    			}
    	}
    	
    	if(incoming.containsKey(fileName))
    		try	{
    			File file = m_botAction.getDataFile(fileName);
    			File temp = (File)incoming.get(fileName);
    			temp.getParentFile().mkdirs();
    			if (temp.exists())
    				temp.delete();
    			file.renameTo(temp);
    			if (guarded.containsKey(arenaName))	{
    					Watched_Arena arena = guarded.get(arenaName);
    					String playerName = arena.myOwner;
    					if (isArenaFileOwner(playerName,temp))	{
    						Watched_Arena arenax;
    						if (arena.myGuests.isEmpty())
    							arenax = new Watched_Arena(arena.myArena,playerName,getLvzNames(temp));
    						else
    							arenax = new Watched_Arena(arena.myArena,playerName,getLvzNames(temp),arena.myGuests);
    						guarded.put(arenaName, arenax);
    					}
    					else	{
    						m_botAction.sendChatMessage(1, playerName + " denied request for " + arena.myArena + " to be monitored as he/she is not the owner");
    						m_botAction.sendChatMessage(1, playerName + " denied request to add " + arena.myArena + "as a monitored arena without ownership");
    						temp.delete();
    						guarded.remove(arenaName);
    					}
    			}
    			incoming.remove(fileName);
    		}
    		catch (Exception e)	{
    			logEvent ("Fatal Error: could not receive and process " + fileName);
    		}
    }
    
    /**
     * Bot chat, no need for an IPC.
     * 
     * @param message
     */
    
    public void handleEntityCommand(String message)	{
    		waiting = false;
    	if( message.startsWith( "!die" ))
    		m_botAction.die();
    	else if( message.startsWith( "!kill " ))
    		m_botAction.scheduleTask(new killTask(message.substring(6)), 100);
    	else if( message.startsWith( "!bounce " ))
    		m_botAction.scheduleTask(new bounceTask(message.substring(7)), 100);
    	else if (message.startsWith( "!action "))
    	    m_bounceDestination = message.substring(8);
        else if( message.startsWith( "back" ))
    		waitReply.cancel();
    	else if( message.startsWith("here?"))
    		m_botAction.sendSmartPrivateMessage(entity, "back");
    	else if ( message.startsWith( "slave" ))	{
    		m_botAction.sendSmartPrivateMessage(entity, "master");
    		if (resume) 	{
    			RespawnTask.cancel();
    			ArenaCheck = new TimerTask()	{
            		public void run()	{
            			m_botAction.requestArenaList();
            		}};
    			m_botAction.scheduleTask(ArenaCheck, 2000, 30000);
    			m_botAction.sendChatMessage(1, "Respawn of " + entity + " sucessful, " +
    					"resuming arena monitoring operations.");
    		}else
    			waitReply.cancel();
    		logEvent( "Slave " + entity + " acquired!" );
    		}else if ( message.startsWith( "master" ))	{
    		waitReply.cancel();
    		logEvent( "Master " + entity + " allocated!" );
    	}
    }
    
    /**
     * Handles invite commands .
     * 
     * @param sender is an owner. (ops pass through to handleCommand)
     * @param message is the command
     */
    
    public void handleStandAloneCommand( String sender, String message ) {
    	if (m_opList.isOwner( sender ))	{
    		if (entity == null)	{
        		if( message.startsWith( "!invite " )) 
                    doInvite(sender,message.substring(8));
                else if( message.startsWith( "!clearinvites" ))	
                	doClearInvites(sender);
                else if( message.startsWith( "!message " ))
                	doBounceMessage(sender,message.substring(9));
                else if( message.startsWith( "!go " ))
                	doGo(sender, message.substring(4));
        	}
    		if( message.startsWith( "!die" ))	
            	doDie(sender);
    	}
    	if( message.startsWith( "!help" ))	
        	getHelpMessages(sender.toLowerCase());
        else if ( entity != null )
        	handleLesserCommand( sender, message );
    }
    
    /**
     * Handles dever commands. (Sysop)
     * 
     * @param sender is of at least Sysop access.
     * @param message is the command
     */
    
    public void handleLesserCommand( String sender, String message )	{
    	if (entity == null) return;
        if( message.startsWith( "!addarena " ))	
        	doAddArena(sender, message.substring(10));
        else if( message.startsWith( "!delarena " ))	
        	doDelArena(sender, message.substring(10));
        else if( message.startsWith( "!arenainvite " ))	
        	doArenaInvite(sender, message.substring(13));
        else if( message.startsWith( "!cleararenainvites " ))	
        	doClearArenaInvites(sender, message.substring(19));
        else if( m_opList.isOwner( sender.toLowerCase() ) || op.contains( sender.toLowerCase() ) ) 
        	handleCommand( sender, message.toLowerCase() );
    }
    
    /**
     * Handles op commands.
     * 
     * @param name is an operator+
     * @param message is the command.
     */
    
    public void handleCommand( String sender, String message )	{
        if( message.startsWith( "!startlog" ))	
        	doStartLog(sender);
        else if( message.startsWith( "!stoplog" ))	
        	doStopLog(sender);
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
        else if (m_opList.isOwner( sender.toLowerCase() ))
        	handleSecretCommand(sender, message);

    }
    
    /**
     * Handles owner (your) commands.
     * 
     * @param sender
     * @param message
     */
    
    public void handleSecretCommand( String sender, String message )	{
        if( message.startsWith( "!addop " ))	
        	doAddOp(sender, message.substring(7));
        else if( message.startsWith( "!delop " ))	
        	doDelOp(sender, message.substring(7));
        else if( message.startsWith( "!listop" ))	
        	doListOp(sender);
        else if( message.startsWith( "!action move " )){
            if( message.length() > 13 ) {
                String dest = message.substring( 13 );
                if( dest != null && !dest.equals("") ) {
                    m_bounceDestination = m_zoneIP + "," + m_zonePort + "," + dest;
                    m_botAction.sendSmartPrivateMessage( sender, "Bounced players will be sent to " + dest + "."  );
                }
            }
        } else if( message.startsWith( "!action move" )){
            m_bounceDestination = m_zoneIP + "," + m_zonePort;
            m_botAction.sendSmartPrivateMessage( sender, "Bounced players will be sent to pub." );
        } else if( message.startsWith( "!action zonemove " )){
            if( message.length() > 17 ) {
                String dest = message.substring( 17 );
                if( dest != null && !dest.equals("") ) {
                    m_bounceDestination = dest;
                    m_botAction.sendSmartPrivateMessage( sender, "Bounced players will be sent to: " + dest + " (validity of information unverified -- make sure this is correct)"  );
                }
            }
        } else if( message.startsWith( "!action kill" )){
            m_bounceDestination = ":kill:";
            m_botAction.sendSmartPrivateMessage( sender, "Bounced players will be *kill'd off the server." );
        }

    }
    
    /**
     * Lists all help messages.
     * @return returns the help message.
     */
    
    public void getHelpMessages(String sender)
    {
    	if (entity == null)	{
    		if(m_opList.isOwner(sender))	{
    			m_botAction.sendSmartPrivateMessage(sender, "!Invite <name>                            -- Invites <name> to " + m_botAction.getArenaName() + " .");
                m_botAction.sendSmartPrivateMessage(sender, "!ClearInvites                             -- Clears all guests.");
                m_botAction.sendSmartPrivateMessage(sender, "!Message <message>                        -- Sets the bounce message interlopers recieve.");
                m_botAction.sendSmartPrivateMessage(sender, "!Go <arena>                               -- Sends the bot to <arena>.");
    		}else
    			m_botAction.sendSmartPrivateMessage(sender, "Bot has been disabled: Contact a bot dever to restart");
    	}if (entity != null)	{
    		if (!enslaved)	{
        		m_botAction.sendSmartPrivateMessage(sender, "!AddArena <arena>                         -- Monitors an arena and all related files.");
            	m_botAction.sendSmartPrivateMessage(sender, "!DelArena <arena>                         -- Stops monitoring an arena and all related files.");
            	m_botAction.sendSmartPrivateMessage(sender, "!ArenaInvite <arena>,<name>               -- Invites a a player to a monitored arena.");
            	m_botAction.sendSmartPrivateMessage(sender, "!ClearArenaInvites <arena>                -- Clears all Invites to a monitored arena.");
        	}if (m_opList.isOwner(sender) || op.contains(sender))	{
        		m_botAction.sendSmartPrivateMessage(sender, "!ListArenas                               -- Lists all monitored arenas.");
            	m_botAction.sendSmartPrivateMessage(sender, "!SetFiles <file1>,<file2>,ect...          -- Sets restricted files.");
            	m_botAction.sendSmartPrivateMessage(sender, "!ListFiles                                -- Lists curretly restricted files");
            	m_botAction.sendSmartPrivateMessage(sender, "!ClearFiles                               -- Clears all restricted files.");
            	m_botAction.sendSmartPrivateMessage(sender, "!RealTime                                 -- Sets real time logging to be on or off.");
            	m_botAction.sendSmartPrivateMessage(sender, "!StartLog                                 -- Starts the logging functionality.");
            	m_botAction.sendSmartPrivateMessage(sender, "!StopLog                                  -- Stops the logging functionality.");
            	m_botAction.sendSmartPrivateMessage(sender, "!help                                     -- Displays this.");
        	}
    	}if (m_opList.isOwner(sender)) {
    	    m_botAction.sendSmartPrivateMessage(sender, "!action <move|zonemove|kill>              -- Determines bounce action:");
            m_botAction.sendSmartPrivateMessage(sender, "         move                                Bounces to pub on this zone");
            m_botAction.sendSmartPrivateMessage(sender, "         move <arena>                        Bounces to <arena> on this zone");
            m_botAction.sendSmartPrivateMessage(sender, "              zonemove <IP,Port>             Bounces to pub of zone @ <IP,Port>");
            m_botAction.sendSmartPrivateMessage(sender, "              zonemove <IP,Port,Arena>       Bounces to <Arena> of zone @ <IP,Port>");
            m_botAction.sendSmartPrivateMessage(sender, "                       kill                  Sends *kill (not recommended)");
            m_botAction.sendSmartPrivateMessage(sender, "                                             [Default action is: Kill on intrusion]");
    	}
    }
    
    /**
     * Idles a return of the bot, this allows the bot to carry
     * out it's commands in the arena before moving on.
     * @author Ayan
     *
     */
    
    private class idleReturn extends TimerTask	{
    	public void run()	{
    		m_botAction.changeArena(home);
        	m_botAction.sendSmartPrivateMessage( entity, "back");
    	}
    }
    
    /**
     * Recursive task to kill individuals from a string of
     * arguments.
     */
    
    private class killTask extends TimerTask {
    	String myArgs;
    	
    	public killTask(String argString)	{
    		
    	}
    	
    	public void run()	{
    		try	{
    		StringTokenizer argTokens = getArgTokens(myArgs.substring(myArgs.length()-1),",");
        	
        	if (argTokens.hasMoreTokens())	{
        		final String arena = argTokens.nextToken();
            	final String violator = argTokens.nextToken();
            	
            	m_botAction.changeArena(arena);
            	
            	TimerTask microManage = new TimerTask()	{
            		public void run()	{
            			Iterator<Player> it = m_botAction.getPlayerIterator();
                    	while (it.hasNext())	{
                    		String playerName = it.next().getPlayerName();
                    		if (playerName.equals(violator))	{
                    			m_botAction.sendSmartPrivateMessage( playerName, "You are attempting to alter a restricted file!");
                    			if ( m_bounceDestination.equals( ":kill:" ) ) {
                                    m_botAction.sendUnfilteredPrivateMessage(playerName,"*kill");
                                } else {
                                    m_botAction.sendUnfilteredPrivateMessage(playerName, sendtoCmd + " " + m_bounceDestination);
                                }
                    		}
                    	}
            		}
            	};
            	
            	m_botAction.scheduleTask(microManage,500);
            	
            	if (argTokens.hasMoreTokens())
            		m_botAction.scheduleTask(new killTask( myArgs.substring(myArgs.indexOf(argTokens.nextToken())-1) ), 1200);
            	else	{
            		m_botAction.scheduleTask(new idleReturn(),1200);
            		this.cancel();
            	}
            		
        	}
    		}
    		catch (Exception e)	{
        		logEvent( "Could not conduct kill. (bad command) :" + myArgs );
        		m_botAction.changeArena(home);
            	m_botAction.sendSmartPrivateMessage( entity, "back");
        		this.cancel();
        	}	
    	}
    }
    
    /**
     * Recursive task to bounce individuals from an argument string.
     */
    
    private class bounceTask extends TimerTask	{
    	String myArgs;
    	
    	public bounceTask(String argString)	{
    		myArgs = argString;
    	}
    	
    	public void run()	{
    	try	{
    		ArrayList<String> allowed = new ArrayList<String>();
    		String newArg = myArgs.substring(0,myArgs.length()-1);
    		StringTokenizer argTokens = getArgTokens(newArg,":");
        	
        	if (argTokens.hasMoreTokens())	{
        		final String arena = argTokens.nextToken();
            	final String owner = argTokens.nextToken();
            	String list;
            	list = argTokens.nextToken();
            	if ( !list.equals("[]") )
            		allowed = ParseString(TrimList(list));
            	allowed.add(owner.toLowerCase());
            	allowed.add(m_botAction.getBotName().toLowerCase());
            	final ArrayList<String> subAllowed = allowed;
            	m_botAction.changeArena(arena);
            	
            	TimerTask microManage = new TimerTask()	{
            		public void run()	{
            			Iterator<Player> it = m_botAction.getPlayerIterator();
                    	while (it.hasNext())	{
                    		String playerName = ((Player)it.next()).getPlayerName();
                    		if (!subAllowed.contains(playerName.toLowerCase()))	{
                    			m_botAction.sendSmartPrivateMessage( playerName, "You are not allowed in this arena! Ask : " + owner + " and he may invite you.");
                    			if ( m_bounceDestination.equals( ":kill:" ) ) {
                    			    m_botAction.sendUnfilteredPrivateMessage(playerName,"*kill");
                    			    logEvent ( "Intruder " + playerName + " was kicked from " + arena + ". (DC'd)");
                    			} else {
                    			    m_botAction.sendUnfilteredPrivateMessage(playerName, sendtoCmd + " " + m_bounceDestination);
                    			    logEvent ( "Intruder " + playerName + " was kicked from " + arena + ". (relocated)");
                    			}
                    			m_botAction.sendChatMessage(1, playerName + " encroached on " + owner + "'s arena " + arena + " and was removed." );
                    		}
                    	}
            		}
            	};
            	
            	m_botAction.scheduleTask(microManage,500);
            	
            	if (argTokens.hasMoreTokens())	{
            		newArg = myArgs.substring(myArgs.indexOf(argTokens.nextToken())-1);
                	m_botAction.scheduleTask(new bounceTask( newArg ), 1200);
            	}
            	else	{
            		m_botAction.scheduleTask(new idleReturn(), 1200);
            		this.cancel();
            	}
        	}
    	}
    	catch (Exception e)	{
    		logEvent( "Could not conduct bounce. (bad command) :" + myArgs );
    		m_botAction.changeArena(home);
        	m_botAction.sendSmartPrivateMessage( entity, "back");
    		this.cancel();
    	}	
    	}
    }
    
    /**
     * Private class to store arena specific information
     */
    
    private class Watched_Arena	{
    	String myArena;
    	String myOwner;
    	ArrayList<String> myLvz;
    	ArrayList<String> myGuests;
    	
    	public Watched_Arena(String arenaName, String Owner){
    		myArena = arenaName;
    		myOwner = Owner;
    		myLvz = new ArrayList<String>();		//concurrent safety measure
    		myGuests = new ArrayList<String>();		//concurrent safety measure
    	}
    	public Watched_Arena(String arenaName,String arenaOwner,ArrayList<String> arenaLvz){
    		myArena = arenaName;
    		myOwner = arenaOwner;
    		myLvz = arenaLvz;
    		myGuests = new ArrayList<String>();		//concurrent safety measure
    	}
    	public Watched_Arena(String arenaName,String arenaOwner,ArrayList<String> arenaLvz,ArrayList<String> guestlist){
    		myArena = arenaName;
    		myOwner = arenaOwner;
    		myLvz = arenaLvz;
    		myGuests = guestlist;
    	}
    	public void addGuest(String invitee)	{
    		myGuests.add(invitee);
    	}
    	public String toCheckable()	{
    		return myArena + ":" + myOwner + ":" + myGuests + ":";
    	}
    	
    	/*public String getName()
    	{return myArena;}
    	public String getOwner()
    	{return myOwner;}
    	public ArrayList<String> getLvz()
    	{return myLvz;}*/
    	public boolean containsLvz(String lvzName)	
    	{return myLvz.contains(lvzName);}
    }
}
