package twcore.bots.twrcbot;

import twcore.core.*;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;

import java.util.*;
import java.sql.ResultSet;
import java.io.*;


/** Bot for TWRC signups
 *  Also allows ops to update points
 *  New version shows people points data.
 *  @author Jacen Solo
 *  @version 1.7
 */
public class twrcbot extends SubspaceBot
{
	HashSet signups = new HashSet();
	HashSet twrcOps = new HashSet();
	File people = new File("people.txt");
	File log = new File("log.txt");
	boolean isRunning = true;
	BotSettings m_botSettings;
	Calendar calendar;

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
		try {
			if(!people.exists())
				people.createNewFile();
			if(!log.exists())
				log.createNewFile();
		} catch(Exception e) {}
		m_botSettings = m_botAction.getBotSettings();
	}

	/** Called when the bot gets logged in
	 *  @param event - LoggedOn event doesn't do much, it's just there so the bot knows when it gets logged in
	 */
	public void handleEvent(LoggedOn event)
	{
		m_botAction.sendUnfilteredPublicMessage("?blogin bangme");
		TimerTask t = new TimerTask() {
			public void run() {
				m_botAction.sendUnfilteredPublicMessage("?liftban #14791");
			}
		};
		m_botAction.scheduleTask(t, 5000);
		m_botAction.joinArena("twrc");
		String allOps = m_botSettings.getString("TWRC Ops");
		String ops[] = allOps.split(":");
		for(int k = 0;k < ops.length;k++)
			twrcOps.add(ops[k].toLowerCase());
		try {
			BufferedReader signedup = new BufferedReader(new FileReader(people));
			String inLine;
			while((inLine = signedup.readLine()) != null)
			{
				signups.add(inLine);
			}
		} catch(IOException e) {}
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
	public void sql(String name)
	{
		try {
			m_botAction.SQLQueryAndClose("website", "INSERT INTO tblRacers (fldID, fldName, fldPoints) VALUES ( 0, \""+name+"\", 0 ) ");
		} catch(Exception e) {}
	}

	/** Handles the different commands that can be sent to the bot
	 *  @param name - Name of the person
	 *  @param message - Message sent to bot
	 */
	public void handleCommand(String name, String message)
	{
		if(m_botAction.getOperatorList().isSmod(name) || twrcOps.contains(name.toLowerCase()))
		{
			if(message.toLowerCase().startsWith("!on"))
			{
				isRunning = true;
				m_botAction.sendSmartPrivateMessage(name, "Bot Activated");
			}
			else if(message.toLowerCase().startsWith("!off"))
			{
				isRunning = false;
				m_botAction.sendSmartPrivateMessage(name, "Bot Deactivated");
			}
			else if(message.toLowerCase().startsWith("!arena "))
			{
				String pieces[] = message.split(" ", 2);
				if(pieces[1].indexOf("%") == -1)
					m_botAction.sendArenaMessage(pieces[1] + " -" + name);
				else
				{
					String soundPieces[] = pieces[1].split("%", 2);
					String soundPiece[] = soundPieces[1].split(" ", 2);
					int soundCode = 2;
					try {
						soundCode = Integer.parseInt(soundPieces[0]);
						if(soundCode == 12)
							soundCode = 2;
					} catch(Exception e) {}
					m_botAction.sendArenaMessage(soundPieces[0] + soundPiece[1] + " -" + name);
				}
			}
			else if(message.toLowerCase().startsWith("!player "))
			{
				String pieces[] = message.split(" ", 2);
				m_botAction.sendPrivateMessage(name, "Current points of " + pieces[1] + " - " + getPoints(pieces[1]));
			}
			else if(message.toLowerCase().startsWith("!update "))
			{
				String pieces[] = message.split(" ", 2);
				String params[] = pieces[1].split(":");
				if(params.length == 3)
					updatePlayer(name, params);
				else
					m_botAction.sendPrivateMessage(name, "Comon...you know the right way to do this");
			}
			else if(message.toLowerCase().startsWith("!clear"))
			{
				signups = new HashSet();
				updatePeopleFile();
			}
			else if(message.toLowerCase().startsWith("!die"))
				m_botAction.die();
		}

		//reg people commands
		if(message.toLowerCase().startsWith("!signup"))
		{
			if(!signups.contains(name.toLowerCase()) && isRunning)
			{
				m_botAction.sendPrivateMessage(name, "Sign up successfull! You may now participate in races.");
				sql(name);
				signups.add(name.toLowerCase());
				updatePeopleFile();
			}
			else
				m_botAction.sendPrivateMessage(name, "The bot is either deactivated or you have already signed up for TWRC, please contact Jacen Solo, Ice Storm, or SuperDAVE(postal) if you need assistance.");
		}
		else if(message.toLowerCase().startsWith("!who "))
		{
			String pieces[] = message.split(" ");
			try {
				int place = Integer.parseInt(pieces[1]);
				who(name, place);
			} catch(Exception e) {}
		}
		else if(message.toLowerCase().startsWith("!rank "))
		{
			String pieces[] = message.split(" ", 2);
			if(getRank(pieces[1]) != 0)
				m_botAction.sendPrivateMessage(name, pieces[1] + " is currently ranked #" + getRank(pieces[1]) + " with " + getPoints(pieces[1]) + " points.");
			else
				m_botAction.sendPrivateMessage(name, "No record of " + pieces[1] + " in the database.");
		}
		else if(message.toLowerCase().startsWith("!rank"))
			m_botAction.sendPrivateMessage(name, "You are currently ranked #" + getRank(name) + " with " + getPoints(name) + " points.");
		else if(message.toLowerCase().startsWith("!help"))
			handleHelp(name);
	}

	/** Updates the people.txt file every time a person signs up to keep track of the people
	 *  that have signed up.
	 */
	public void updatePeopleFile()
	{
		Iterator it = signups.iterator();
		try {
			FileWriter out = new FileWriter(people, true);
			while(it.hasNext())
			{
				String name = (String)it.next();
				out.write(name + "\n");
				out.flush();
			}
			out.close();
		} catch(IOException e) {}
	}

	/** Updates a players points.
	 *  @param name - Name of the op that is updating points.
	 *  @param params - Player's name and point change
	 */
	public void updatePlayer(String name, String params[])
	{
		int points = Integer.parseInt(params[1]);
		String pName = params[0];
		try {
			int currentPoints = getPoints(pName);
			writeLog(name + " changed " + params[0] + "'s points from " + currentPoints + " to " + (currentPoints + points) + ". Reason: " + params[2]);
			currentPoints += points;

			m_botAction.SQLQueryAndClose("website", "UPDATE tblRacers SET fldPoints = "+currentPoints+" WHERE fldName = \'"+pName+"\'");
			m_botAction.sendPrivateMessage(name, "Updated.");
		} catch(Exception e) {e.printStackTrace();}
		pointChange(pName, points, params[2]);
	}

	/** Returns the points that name has.
	 *  @param name - name of the player that we are getting points of.
	 */
	public int getPoints(String name)
	{
		try {
			ResultSet results = m_botAction.SQLQuery("website", "SELECT fldPoints FROM tblRacers WHERE fldName = \'"+name+"\'");
            int points = 0;
			if(results.next())
				points = results.getInt("fldPoints");
            m_botAction.SQLClose(results);
            return points;
		} catch(Exception e) {return 0;}
	}

	/** Writes point changes to log.txt
	 *  @param message - message to be written to the log.txt file
	 */
	public void writeLog(String message)
	{
		try {
			FileWriter out = new FileWriter(log, true);
			String outLine = message;
			out.write(outLine + "\n");
			out.flush();
			out.close();
		} catch(IOException e) {}
	}

	/** PM's the person with the Help message
	 *  @param name - Player that asked for the message.
	 */
	public void handleHelp(String name)
	{
		if(m_botAction.getOperatorList().isSmod(name) || twrcOps.contains(name.toLowerCase()))
		{
			m_botAction.sendPrivateMessage(name, "!on                     -Activates the bot.");
			m_botAction.sendPrivateMessage(name, "!off                    -Deactivates the bot.");
			m_botAction.sendPrivateMessage(name, "!delname <name>         -Removes the person from the people.txt file.");
			m_botAction.sendPrivateMessage(name, "!update <name>:#:reason -Adds # points to <name>'s record.");
			m_botAction.sendPrivateMessage(name, "!die                    -Kills the bot.");
		}

		m_botAction.sendPrivateMessage(name, "!who <#>                -Returns the player with rank of <#>.");
		m_botAction.sendPrivateMessage(name, "!rank <name>            -Gets <name>'s rank.");
		m_botAction.sendPrivateMessage(name, "!signup                 -Signs you up for TWRC,");
		m_botAction.sendPrivateMessage(name, "!help                   -Displays this message.");
	}

	/** Returns the rank the specified player.
	 *  @param name - Name of the player to get rank of.
	 *  @return Player's rank.
	 */
	public int getRank(String name)
	{
		int k = 0;
		HashMap ranks = new HashMap();
		try {
			ResultSet result = m_botAction.SQLQuery("website", "SELECT fldName, fldPoints FROM tblRacers ORDER BY fldPoints DESC");
			while(result.next())
			{
				k++;
				ranks.put(result.getString("fldName").toLowerCase(), new Integer(k));
			}
            m_botAction.SQLClose(result);
		} catch(Exception e) {}
		try {
			return ((Integer)ranks.get(name.toLowerCase())).intValue();
		} catch(Exception e) {return 0;}
	}

	/** Finds the player with the requested rank and pm's the person w/ that name.
	 *  @param name - Name of person requesting rank.
	 *  @param rank - Rank they want to know.
	 */
	public void who(String name, int rank)
	{
		int k = 0;
		boolean found = false;
		try {
			ResultSet result = m_botAction.SQLQuery("website", "SELECT fldName, fldPoints FROM tblRacers ORDER BY fldPoints DESC");
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
	}

	/** Inserts point change data into the tblPointsData sql table.
	 *  @param player - Name of player whose points were changed.
	 *  @param points - Change in points.
	 *  @param reason - reason for change.
	 */
	public void pointChange(String player, int points, String reason)
	{
		try {
			m_botAction.SQLQueryAndClose("website", "INSERT INTO tblPointsData (fldID, fldName, fldPoints, fldReason, fldTime) VALUES (0, "+getID(player)+", "+points+", \""+reason+"\", \""+getTimeStamp()+"\")");
		} catch(Exception e) {e.printStackTrace();}
	}

	/** Returns the player's tblRacers fldID.
	 *  @param name - Name of player.
	 *  @return ID
	 */
	public int getID(String name)
	{
		try {
			ResultSet results = m_botAction.SQLQuery("website", "SELECT fldID FROM tblRacers WHERE fldName = \'"+name+"\'");
			results.next();
            int id = results.getInt("fldID");
            m_botAction.SQLClose(results);
            return id;
		} catch(Exception e) {e.printStackTrace();}
		return 0;
	}

	/** Returns a timestamp for the database.
	 *  @return Returns a timestamp in the format MM/DD/YYYY HH:MM
	 */
	public String getTimeStamp()
	{
		Date trialTime = new Date();
		calendar.setTime(trialTime);
		String date = "";
		if(((int)calendar.get(Calendar.MONTH) < 9))
			date += "0" + (calendar.get(Calendar.MONTH) + 1);
		else
			date += (calendar.get(Calendar.MONTH) + 1);
		if(((int)calendar.get(Calendar.DAY_OF_MONTH) < 10))
			date += "/" + "0" + (calendar.get(Calendar.DAY_OF_MONTH));
		else
			date += "/" + calendar.get(Calendar.DAY_OF_MONTH);
		date += "/" + calendar.get(Calendar.YEAR);
		date += " ";
		if(((int)calendar.get(Calendar.HOUR_OF_DAY) < 10))
			date += "0" + calendar.get(Calendar.HOUR_OF_DAY);
		else
			date += calendar.get(Calendar.HOUR_OF_DAY);
		if(((int)calendar.get(Calendar.MINUTE) < 10))
			date += ":0" + calendar.get(Calendar.MINUTE);
		else
			date += ":" + calendar.get(Calendar.MINUTE);
		return date;
	}
}