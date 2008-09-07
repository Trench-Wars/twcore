package twcore.bots.twrcbot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;


/** 
 * Bot for TWRC signups
 * 
 * @author Jacen Solo, Maverick
 * @version 2.0
 */
public class twrcbot extends SubspaceBot {
	HashSet<String> twrcOps = new HashSet<String>();
	boolean isSignupsOpen = true;
	BotSettings m_botSettings;
	Calendar calendar;
	
	private final String sqlHost = "twrc"; 

	/** Create the twrcbot, requests LoggedOn and Message events, and makes the 2 logs if necessary
	 *  @param botAction - necessary thing, passed by BotQueue to give bot functionality
	 */
	public twrcbot(BotAction botAction)
	{
		super(botAction);
		String[] ids = TimeZone.getAvailableIDs(-6 * 60 * 60 * 1000);
		SimpleTimeZone pdt = new SimpleTimeZone(-6 * 60 * 60 * 1000, ids[0]);
		pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
		pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
		calendar = new GregorianCalendar(pdt);

		EventRequester events = m_botAction.getEventRequester();

		events.request(EventRequester.LOGGED_ON);
		events.request(EventRequester.MESSAGE);
	}

	/** Called when the bot gets logged in
	 *  @param event - LoggedOn event doesn't do much, it's just there so the bot knows when it gets logged in
	 */
	public void handleEvent(LoggedOn event) {
		m_botAction.joinArena("twrc");
		loadOperators();
	}
	
	/**
	 * Loads the operators from the twrcbot.cfg configuration file
	 */
	private void loadOperators() {
	    // Get the operators from the configuration file
	    m_botSettings = m_botAction.getBotSettings();
        String allOps = m_botSettings.getString("TWRC Ops");
        String ops[] = allOps.split(":");
        twrcOps.clear();
        
        for(int k = 0;k < ops.length;k++) {
            twrcOps.add(ops[k]);
        }
	}
	
	private void saveOperators() {
	    // Save the operators to the configuration file
	    m_botSettings = m_botAction.getBotSettings();
	    
	    String allOps = "";
	    for(String op:twrcOps) {
	        if(allOps.length() == 0) {
	            allOps += op;
	        } else {
	            allOps += ":" + op;
	        }
	    }
	    
	    m_botSettings.put("TWRC Ops", allOps);
	    m_botSettings.save();
	}

	/** Gets the person's name and message then passes info to handleCommand
	 *  @param event - Message event containing all kinds of info about messages sent to bot
	 */
	public void handleEvent(Message event)
	{
		String name;
		String message = event.getMessage();

		int messageType = event.getMessageType();
		if(messageType == Message.PRIVATE_MESSAGE)
		{
			int senderID = event.getPlayerID();
			name = m_botAction.getPlayerName(senderID);

			handleCommand(name, message);
		}
	}

	/** Inserts the player's name into the Racers table of the Racing database
	 *  @param name - Name of the person signing up.
	 */
	public void sqlSignup(String name)
	{
		try {
			m_botAction.SQLQueryAndClose(sqlHost, "INSERT INTO tblRacers (fldName, fldPoints) VALUES ( '"+Tools.addSlashes(name)+"', 0 ) ");
		} catch(SQLException sqle) {
		    m_botAction.sendSmartPrivateMessage(name, "Error encountered while saving signup to database. Please contact a TWRC Operator.");
		    Tools.printStackTrace("SQLException encountered while performing sqlSignup() in TWRCBot", sqle);
		}
	}

	/** Handles the different commands that can be sent to the bot
	 *  @param name - Name of the person
	 *  @param message - Message sent to bot
	 */
	public void handleCommand(String name, String message)
	{
	    // Operator commands
		if(m_botAction.getOperatorList().isSmod(name) || twrcOps.contains(name))
		{
			if(message.toLowerCase().startsWith("!open"))
			{
			    if(isSignupsOpen) {
			        m_botAction.sendSmartPrivateMessage(name, "Signups are already open.");
			    } else {
			        isSignupsOpen = true;
			        m_botAction.sendSmartPrivateMessage(name, "Signups opened");
			    }
			}
			else if(message.toLowerCase().startsWith("!close"))
			{
			    if(!isSignupsOpen) {
			        m_botAction.sendSmartPrivateMessage(name, "Signups are already closed");
			    } else {
			        isSignupsOpen = false;
			        m_botAction.sendSmartPrivateMessage(name, "Signups closed");
			    }
			}
			else if(message.toLowerCase().startsWith("!player "))
			{
				String pieces[] = message.split(" ", 2);
				int points = getPoints(pieces[1]);
				if(points > -1) {
				    m_botAction.sendPrivateMessage(name, "Current points of " + pieces[1] + " : " + getPoints(pieces[1]));
				} else if(points == -1) {
				    m_botAction.sendPrivateMessage(name, "Player name not found : "+ pieces[1]);
				} else if(points == -2) {
				    m_botAction.sendPrivateMessage(name, "Unexpected error encountered while checking points of player : "+pieces[1]+". Please contact a member of TW Bot Development.");
				}
			}
			else if(message.toLowerCase().startsWith("!update "))
			{
				String pieces[] = message.split(" ", 2);
				String params[] = pieces[1].split(":");
				if(params.length == 3 && Tools.isAllDigits(params[1]))
					updatePlayer(name, params);
				else
					m_botAction.sendPrivateMessage(name, "Incorrect command syntax");
			}
			else if(message.toLowerCase().startsWith("!reload")) {
			    loadOperators();
			    m_botAction.sendPrivateMessage(name, "Reload completed.");
			}
			else if(message.toLowerCase().startsWith("!addop ")) {
			    String newoperator = message.split(" ",2)[1];
			    
			    if(twrcOps.contains(newoperator)) {
			        m_botAction.sendPrivateMessage(name, "Operator '"+newoperator+"' already exists.");
			        return;
			    }
			    if(newoperator != null && newoperator.length() > 0) {
			        twrcOps.add(newoperator);
			        saveOperators();
			        m_botAction.sendPrivateMessage(name, "Operator '"+newoperator+"' added.");
			    } else {
			        m_botAction.sendPrivateMessage(name, "Command syntax error. Please specify a correct name.");
			    }
			}
			else if(message.toLowerCase().startsWith("!removeop ")) {
			    String deloperator = message.split(" ",2)[1];
			    
			    if(deloperator == null || deloperator.length() == 0) {
			        m_botAction.sendPrivateMessage(name, "Command syntax error. Please specify a correct name.");
			        return;
			    }
			    
		        if(deloperator.equalsIgnoreCase(name)) {
		            m_botAction.sendPrivateMessage(name, "You can't remove yourself.");
		            return;
		        }
		        if(!m_botAction.getOperatorList().isBot(name) && m_botAction.getOperatorList().isBot(deloperator)) {
		            m_botAction.sendPrivateMessage(name, "You can't remove staff members from Operator status.");
		            return;
		        }
		        
		        boolean removed = twrcOps.remove(deloperator);
		        
		        if(removed) {
		            saveOperators();
		            m_botAction.sendPrivateMessage(name, "Operator '"+deloperator+"' removed.");
		        } else {
		            m_botAction.sendPrivateMessage(name, "Operator '"+deloperator+"' couldn't be found. Removal cancelled.");
		        }
			}
			else if(message.toLowerCase().startsWith("!die")) {
			    m_botAction.cancelTasks();
			    m_botAction.die();
			}
		}

		// Public commands
		if(message.toLowerCase().startsWith("!signup")) {
		    
		    int playerid = getID(name);
		    
			if(playerid == -1 && isSignupsOpen) {
				m_botAction.sendPrivateMessage(name, "Sign up successfull! You may now participate in races.");
				sqlSignup(name);
			} else if(!isSignupsOpen) {
                m_botAction.sendPrivateMessage(name, "Signups are currently closed. Please contact a TWRC Operator if you need assistance.");
            } else if(playerid > -1) {
			    m_botAction.sendPrivateMessage(name, "You have already signed up for TWRC. Please contact a TWRC Operator if you need assistance.");
			} 
		}		
		else if(message.toLowerCase().startsWith("!rank")) {
		    String params = message.substring(5).trim();
		    try {
		        doRank(name, params);
		    } catch(SQLException sqle) {
		        m_botAction.sendPrivateMessage(name, "Unexpected error encountered. Contact a TWRC Operator for further assistance.");
		        Tools.printStackTrace("SQLException encountered on !rank of twrcbot",sqle);
		    }
		}
		else if(message.toLowerCase().startsWith("!operators")) {
		    m_botAction.sendPrivateMessage(name, "TWRC Operators: ");
		    
		    String ops = "";
		    for(String op:twrcOps) {
		        if(ops.length() > 0)
		            ops += ", ";
		        ops += op;
		    }
		    
		    m_botAction.sendPrivateMessage(name, ops);
		}
		else if(message.toLowerCase().startsWith("!help"))
			handleHelp(name);
	}

	/** Updates a players points.
	 *  @param name - Name of the op that is updating points.
	 *  @param params - Player's name and point change
	 */
	public void updatePlayer(String name, String params[])
	{
	    String pName = params[0];
		int points = Integer.parseInt(params[1]);
		int currentPoints = getPoints(pName);
		
		try {
			if(currentPoints < 0) {
			    m_botAction.sendPrivateMessage(name, "Player not found.");
			} else {
			    currentPoints += points;
			    
			    m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers SET fldPoints = "+currentPoints+" WHERE fldName = '"+Tools.addSlashes(pName)+"'");
			    m_botAction.sendPrivateMessage(name, "Updated.");
			}
		} catch(Exception e) {
		    e.printStackTrace();
		}

		// Save into tblPointsData table
		if(currentPoints > -1) {
    		try {
                m_botAction.SQLQueryAndClose(sqlHost, "INSERT INTO tblPointsData (fldID, fldName, fldPoints, fldReason, fldTime) VALUES (0, "+getID(pName)+", "+points+", '"+Tools.addSlashes(params[2])+"', '"+getTimeStamp()+"')");
            } catch(Exception e) {
                e.printStackTrace();
            }
		}
	}

	/** 
	 * Returns the points that name has.
	 * @param name - name of the player that we are getting points of.
	 * @return the points of the player, -1 if not found, -2 if error encountered
	 */
	public int getPoints(String name)
	{
		try {
			ResultSet results = m_botAction.SQLQuery(sqlHost, "SELECT fldPoints FROM tblRacers WHERE fldName = '"+Tools.addSlashes(name)+"'");
            int points = 0;
			if(results != null && results.next())
				points = results.getInt("fldPoints");
			else if(results == null || results.next() == false) {
			    points = -1;
			}
            m_botAction.SQLClose(results);
            return points;
		} catch(SQLException sqle) {
		    return -2;
		}
	}

	/** PM's the person with the Help message
	 *  @param name - Player that asked for the message.
	 */
	public void handleHelp(String name)
	{
		if(m_botAction.getOperatorList().isSmod(name) || twrcOps.contains(name))
		{
		    m_botAction.sendPrivateMessage(name, "--- Operator commands ---");
		    m_botAction.sendPrivateMessage(name, "Signups currently: "+(isSignupsOpen?"OPEN":"CLOSED"));
			m_botAction.sendPrivateMessage(name, "!open                   -Open signups.");
			m_botAction.sendPrivateMessage(name, "!close                  -Close signups.");
			m_botAction.sendPrivateMessage(name, "!update <name>:#:reason -Adds # points to <name>'s record.");
			m_botAction.sendPrivateMessage(name, "!die                    -Kills the bot.");
			m_botAction.sendPrivateMessage(name, "--- Operator management commands ---");
			m_botAction.sendPrivateMessage(name, "!reload                 -Reloads the operators from the cfg file.");
			m_botAction.sendPrivateMessage(name, "!addop <name>           -Adds <name> as TWRC operator");
			m_botAction.sendPrivateMessage(name, "!removeop <name>        -Removes <name> as TWRC operator");
			m_botAction.sendPrivateMessage(name, "---  Public commands  ---");
		}

		m_botAction.sendPrivateMessage(name, "!rank                   -Shows top 5 ranking by points");
		m_botAction.sendPrivateMessage(name, "!rank #                 -Shows top # ranking by points");
		m_botAction.sendPrivateMessage(name, "!rank <name>            -Gets <name>'s rank.");
		if(isSignupsOpen)
		m_botAction.sendPrivateMessage(name, "!signup                 -Signs you up for TWRC.");
		m_botAction.sendPrivateMessage(name, "!operators              -Displays the TWRC Operators.");
		m_botAction.sendPrivateMessage(name, "!help                   -Displays this message.");
	}

	/** 
	 * Returns the rank the specified player.
	 * @param name - Name of the player to get rank of.
	 * @return Player's rank.
	 */
	public void doRank(String name, String parameters) throws SQLException {
	    int count = 5;
	    
	    if(Tools.isAllDigits(parameters)) {
	        try {
	            count = Integer.parseInt(parameters);
	        } catch(NumberFormatException nfe) { }
	        
	        if( count < 5)
	            count = 5;
	        if( count > 50)
	            count = 50;
	    } else {
	        
	        // !rank <playername> 
	        
	        ResultSet result = m_botAction.SQLQuery(sqlHost, "SELECT fldName, fldPoints FROM tblRacers ORDER BY fldPoints DESC");
	        int rank = 1;
	        boolean found = false;
	        
	        while(result != null && result.next()) {
	            if(result.getString(1).trim().equalsIgnoreCase(parameters)) {
	                m_botAction.sendPrivateMessage(name,   Tools.formatString(" #"+rank, 6) + " " +
	                                                       Tools.formatString(result.getString(1),23) + " " +
	                                                       result.getString(2)+" pts.");
	                found = true;
	                break;
	            }
	            rank++;
	        }
	        
	        m_botAction.SQLClose(result);
	        
	        if(!found) {
	            m_botAction.sendPrivateMessage(name, "No record of " + parameters + " found.");
	        }
	        
	        return;
	    }
	    
	    // !rank [#]
		ResultSet result = m_botAction.SQLQuery(sqlHost, "SELECT fldName, fldPoints FROM tblRacers ORDER BY fldPoints DESC LIMIT 0,"+count);
		int rank = 1;
		
		while(result != null && result.next()) {
		    m_botAction.sendPrivateMessage(name,   Tools.formatString(" #"+rank, 6) + " " +
		                                           Tools.formatString(result.getString(1),23) + " " +
		                                           result.getString(2)+" pts.");
		    rank++;
		}
		m_botAction.SQLClose(result);
	}

	/* * Finds the player with the requested rank and pm's the person w/ that name.
	 *  @param name - Name of person requesting rank.
	 *  @param rank - Rank they want to know.
	 * /
	public void who(String name, int rank)
	{
		int k = 0;
		boolean found = false;
		try {
			ResultSet result = m_botAction.SQLQuery(sqlHost, "SELECT fldName, fldPoints FROM tblRacers ORDER BY fldPoints DESC");
			while(result.next())
			{
				k++;
				if(k == rank)
				{
					String ending = "th";
					found = true;
					if(rank == 1)
						ending = "st";
					if(rank == 2)
						ending = "nd";
					if(rank == 3)
						ending = "rd";
					m_botAction.sendPrivateMessage(name, result.getString("fldName") + " is currently in " + rank + ending + " place with " + result.getInt("fldPoints") + " points.");
				}
			}
            m_botAction.SQLClose(result);
		} catch(Exception e) {}
		if(!found)
			m_botAction.sendPrivateMessage(name, "Cannot find anyone with that rank.");
	}*/

	/** 
	 * Returns the player's tblRacers fldID.
	 * @param name - Name of player.
	 * @return the fldID of the name or -1 if not found
	 */
	public int getID(String name)
	{
	    int id = -1;
		try {
			ResultSet results = m_botAction.SQLQuery(sqlHost, "SELECT fldID FROM tblRacers WHERE fldName = '"+Tools.addSlashes(name)+"'");
			
			if(results != null && results.next()) {
			    id = results.getInt("fldID");
			}
			m_botAction.SQLClose(results);
		
		} catch(SQLException sqle) {
		    Tools.printStackTrace("Unexpected SQLException encountered during twrcbot.getID().", sqle);
		}
        return id;
	}

	/** Returns a timestamp for the database.
	 *  @return Returns a timestamp in the format MM/DD/YYYY HH:MM
	 */
	public String getTimeStamp()
	{
		Date trialTime = new Date();
		calendar.setTime(trialTime);
		String date = "";
		if((calendar.get(Calendar.MONTH) < 9))
			date += "0" + (calendar.get(Calendar.MONTH) + 1);
		else
			date += (calendar.get(Calendar.MONTH) + 1);
		if((calendar.get(Calendar.DAY_OF_MONTH) < 10))
			date += "/" + "0" + (calendar.get(Calendar.DAY_OF_MONTH));
		else
			date += "/" + calendar.get(Calendar.DAY_OF_MONTH);
		date += "/" + calendar.get(Calendar.YEAR);
		date += " ";
		if((calendar.get(Calendar.HOUR_OF_DAY) < 10))
			date += "0" + calendar.get(Calendar.HOUR_OF_DAY);
		else
			date += calendar.get(Calendar.HOUR_OF_DAY);
		if((calendar.get(Calendar.MINUTE) < 10))
			date += ":0" + calendar.get(Calendar.MINUTE);
		else
			date += ":" + calendar.get(Calendar.MINUTE);
		return date;
	}
}