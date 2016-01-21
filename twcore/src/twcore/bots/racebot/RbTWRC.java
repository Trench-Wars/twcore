package twcore.bots.racebot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import twcore.core.events.Message;

/** Bot for TWRC updates
    Also allows ops to update points after races.
    @author Jacen Solo
    @version 2.3
*/
public class RbTWRC extends RaceBotExtension
{
    String sqlHost = "website";
    File log = new File("log.txt");
    int racePlayers;
    Calendar calendar;

    /** Constructs a new instance of RbTWRC and sets up stuff for timestamp/logfile
    */
    public RbTWRC()
    {
        String[] ids = TimeZone.getAvailableIDs(-6 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(-6 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);

        calendar = new GregorianCalendar(pdt);

        try {
            if(!log.exists())
                log.createNewFile();
        } catch(Exception e) {}
    }

    /** Recieves a message event and handles it based on user permissions.
    */
    public void handleEvent(Message event)
    {
        if(event.getMessageType() == Message.PRIVATE_MESSAGE)
        {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            String message = event.getMessage();

            if(m_bot.isOperator(name) || m_botAction.getOperatorList().isSmod(name))
                handleCommand(name, message);
        }
    }

    /** Handles the commands for updating a race or sending an arena message.
    */
    public void handleCommand(String name, String message)
    {
        RbRace race = (RbRace)modules.get("Race");

        if(message.toLowerCase().startsWith("!help")) {
            m_botAction.sendPrivateMessage(name, "!normal           -Updates player standings after a normal race.");
            m_botAction.sendPrivateMessage(name, "!big              -Updates player standings after a major race.");
            m_botAction.sendPrivateMessage(name, "!marathon         -Updates player standings after a marathon.");
            m_botAction.sendPrivateMessage(name, "!help             -Sends you this message...");
        }
        else if(!(race.updated))
        {
            if(message.toLowerCase().startsWith("!normal"))
            {
                handleNormal(name);
                race.updated = true;
            }

            if(m_bot.isOperator(name) && m_botAction.getOperatorList().isSmod(name))
            {
                if(message.toLowerCase().startsWith("!big"))
                {
                    handleBig(name);
                    race.updated = true;
                }
                else if(message.toLowerCase().startsWith("!marathon"))
                {
                    handleMarathon(name);
                    race.updated = true;
                }
            }
        }
    }

    /** Updates all the databases for the last normal race.
        2 points for leading a lap. 2 points for leading the most laps. top 10 get points
        1st = 10p, 2nd = 9p, 3rd = 8p......10th = 1p
    */
    public void handleNormal(String opName)
    {
        raceStatsUpdate("Normal", opName);
        writeLog(opName + " started a normal race update, points changes follow.");
        RbRace race = (RbRace)modules.get("Race");
        Track track = race.getTrack(race.currentTrack);
        HashMap<Integer, String> positions = track.positions;
        HashMap<String, Integer> leaders = track.lapLeaders;
        Set<String> set = leaders.keySet();
        Collection<Integer> col = leaders.values();
        Iterator<Integer> it = positions.keySet().iterator();
        Iterator<String> it2 = set.iterator();
        Iterator<Integer> it3 = col.iterator();
        String leadName = "";
        int mostLaps = 0;

        while(it2.hasNext())
        {
            String name = (String)it2.next();

            try {
                m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + 2 WHERE fldName = \'" + name + "\'");
                writeLog(name + " given 2 points.");
                m_botAction.sendSmartPrivateMessage(name, "You just recieved 2 points for leading a lap and now have " + getPoints(name) + " points.");
                pointChange(name, 2, "Lead a lap.");
            } catch(Exception e) {
                m_botAction.sendPrivateMessage(opName, "Error while giving bonus points.");
            }

            int leadLaps = ((Integer)it3.next()).intValue();

            if(leadLaps > mostLaps)
            {
                mostLaps = leadLaps;
                leadName = name;
            }
        }

        try {
            m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + 2 WHERE fldName = \'" + leadName + "\'");
            writeLog(leadName + " given 2 points.");
            m_botAction.sendSmartPrivateMessage(leadName, "You just recieved 2 points for leading the most laps and now have " + getPoints(leadName) + " points.");
            pointChange(leadName, 2, "Lead most laps.");
        } catch(Exception e) {
            m_botAction.sendPrivateMessage(opName, "Error while giving person with most lead laps points.");
        }

        int k = 0;

        while(it.hasNext() && k < 10)
        {
            it.next();
            String name = (String)positions.get(new Integer(k + 1));
            int points = 10 - k;

            try {
                m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + " + points + " WHERE fldName = \'" + name + "\'");
                writeLog(name + " given " + points + " points.");
                m_botAction.sendSmartPrivateMessage(name, "You just recieved " + points + " points and now have " + getPoints(name) + " points.");
                pointChange(name, points, "Top ten in normal race.");
            } catch(Exception e) {
                m_botAction.sendPrivateMessage(opName, "Error while giving points.");
            }

            k++;
        }

        while(it.hasNext())
        {
            it.next();
            String name = (String)positions.get(new Integer(k + 1));

            try {
                m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + 1 WHERE fldName = \'" + name + "\'");
                writeLog(name + " given 1 point.");
                m_botAction.sendSmartPrivateMessage(name, "You just recieved 1 point and now have " + getPoints(name) + " points.");
                pointChange(name, 1, "Finished normal race.");
            } catch(Exception e) {
                m_botAction.sendPrivateMessage(opName, "Error while giving points.");
            }

            k++;
        }

        m_botAction.sendPrivateMessage(opName, "Done.");
        track.lapLeaders = new HashMap<String, Integer>();
        track.positions = new HashMap<Integer, String>();
        writeLog("Normal race log done.");
        writeLog("");
        writeLog("");
        race.currentTrack = -1;
        race.loadArena(null, false);
    }

    /** Updates all the databases for the last race if it was a big/major race.
        2 points for leading a lap. 2 points for leading the most. Points - Position + 1 for finishing.
    */
    public void handleBig(String opName)
    {
        raceStatsUpdate("Major", opName);
        writeLog(opName + " started a major race update, point changes follow.");
        RbRace race = (RbRace)modules.get("Race");
        Track track = race.getTrack(race.currentTrack);
        HashMap<Integer, String> positions = track.positions;
        HashMap<String, Integer> leaders = track.lapLeaders;
        Set<String> set = leaders.keySet();
        Collection<Integer> col = leaders.values();
        Iterator<Integer> it = positions.keySet().iterator();
        Iterator<String> it2 = set.iterator();
        Iterator<Integer> it3 = col.iterator();
        String leadName = "";
        int mostLaps = 0;

        while(it2.hasNext())
        {
            String name = (String)it2.next();

            try {
                m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + 3 WHERE fldName = \'" + name + "\'");
                writeLog(name + " given 3 points.");
                m_botAction.sendSmartPrivateMessage(name, "You just recieved 3 points for leading a lap and now have " + getPoints(name) + " points.");
                pointChange(name, 3, "Lead a lap.");
            } catch(Exception e) {
                m_botAction.sendPrivateMessage(opName, "Error while giving bonus points.");
            }

            int leadLaps = ((Integer)it3.next()).intValue();

            if(leadLaps > mostLaps)
            {
                mostLaps = leadLaps;
                leadName = name;
            }
        }

        try {
            m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + 5 WHERE fldName = \'" + leadName + "\'");
            writeLog(leadName + " given 2 points.");
            m_botAction.sendSmartPrivateMessage(leadName, "You just recieved 5 points for leading the most laps and now have " + getPoints(leadName) + " points.");
            pointChange(leadName, 5, "Lead most laps.");
        } catch(Exception e) {
            m_botAction.sendPrivateMessage(opName, "Error while giving person with most laps lead points.");
        }

        int k = 0;

        while(it.hasNext() && k < 10)
        {
            it.next();
            String name = (String)positions.get(new Integer(k + 1));
            int points = 20 - k;

            try {
                m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + " + points + " WHERE fldName = \'" + name + "\'");
                writeLog(name + " given " + points + " points.");
                m_botAction.sendSmartPrivateMessage(name, "You just recieved " + points + " points and now have " + getPoints(name) + " points.");
                pointChange(name, points, "Finished a major race.");
            } catch(Exception e) {
                m_botAction.sendPrivateMessage(opName, "Error while giving people points.");
            }

            k++;
        }

        while(it.hasNext())
        {
            it.next();
            String name = (String)positions.get(new Integer(k + 1));
            int points = 2;

            try {
                m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + " + points + " WHERE fldName = \'" + name + "\'");
                writeLog(name + " given " + points + " points.");
                m_botAction.sendSmartPrivateMessage(name, "You just recieved " + points + " points and now have " + getPoints(name) + " points.");
                pointChange(name, points, "Finished a major race.");
            } catch(Exception e) {
                m_botAction.sendPrivateMessage(opName, "Error while giving people points.");
            }

            k++;
        }

        m_botAction.sendPrivateMessage(opName, "Done.");
        track.lapLeaders = new HashMap<String, Integer>();
        track.positions = new HashMap<Integer, String>();
        writeLog("Major race log done.");
        writeLog("");
        writeLog("");
        race.currentTrack = -1;
        race.loadArena(null, false);
    }

    /** Updates all of the databases for the last race if it was a marathon
        2 points for leading a lap. 10 points for leading the most. 30 points for winning.
    */
    public void handleMarathon(String opName)
    {
        raceStatsUpdate("Marathon", opName);
        writeLog(opName + " started a marathon race update, point changes follow.");
        RbRace race = (RbRace)modules.get("Race");
        Track track = race.getTrack(race.currentTrack);
        HashMap<Integer, String> positions = track.positions;
        HashMap<String, Integer> leaders = track.lapLeaders;
        Set<String> set = leaders.keySet();
        Collection<Integer> col = leaders.values();
        Iterator<Integer> it = positions.keySet().iterator();
        Iterator<String> it2 = set.iterator();
        Iterator<Integer> it3 = col.iterator();
        String leadName = "";
        int mostLaps = 0;

        while(it2.hasNext())
        {
            String name = (String)it2.next();

            try {
                m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + 5 WHERE fldName = \'" + name + "\'");
                writeLog(name + " given 5 points.");
                m_botAction.sendSmartPrivateMessage(name, "You just recieved 5 points for leading a lap and now have " + getPoints(name) + " points.");
                pointChange(name, 5, "Lead a lap.");
            } catch(Exception e) {
                m_botAction.sendPrivateMessage(opName, "Error while giving people points for leading a lap.");
            }

            int leadLaps = ((Integer)it3.next()).intValue();

            if(leadLaps > mostLaps)
            {
                mostLaps = leadLaps;
                leadName = name;
            }
        }

        try {
            m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + 10 WHERE fldName = \'" + leadName + "\'");
            writeLog(leadName + " given 10 points.");
            m_botAction.sendSmartPrivateMessage(leadName, "You just recieved 10 points for leading the most laps and now have " + getPoints(leadName) + " points.");
            pointChange(leadName, 10, "Lead most laps.");
        } catch(Exception e) {
            m_botAction.sendPrivateMessage(opName, "Error while updating person with the most laps lead.");
        }

        int k = 0;

        while(it.hasNext() && k < 10)
        {
            it.next();
            String name = (String)positions.get(new Integer(k + 1));
            int points = 30 - k;

            try {
                m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + " + points + " WHERE fldName = \'" + name + "\'");
                writeLog(name + " given " + points + " points.");
                m_botAction.sendSmartPrivateMessage(name, "You just recieved " + points + " points and now have " + getPoints(name) + " points.");
                pointChange(name, points, "Finished a major race.");
            } catch(Exception e) {
                m_botAction.sendPrivateMessage(opName, "Error while giving people points.");
            }

            k++;
        }

        while(it.hasNext())
        {
            it.next();
            String name = (String)positions.get(new Integer(k + 1));
            int points = 10;

            try {
                m_botAction.SQLQueryAndClose(sqlHost, "UPDATE tblRacers Set fldPoints = fldPoints + " + points + " WHERE fldName = \'" + name + "\'");
                writeLog(name + " given " + points + " points.");
                m_botAction.sendSmartPrivateMessage(name, "You just recieved " + points + " points and now have " + getPoints(name) + " points.");
                pointChange(name, points, "Finished a major race.");
            } catch(Exception e) {
                m_botAction.sendPrivateMessage(opName, "Error while giving people points.");
            }

            k++;
        }

        track.lapLeaders = new HashMap<String, Integer>();
        track.positions = new HashMap<Integer, String>();
        writeLog("Marathon race log done.");
        writeLog("");
        writeLog("");
        race.currentTrack = -1;
        race.loadArena(null, false);
    }

    /** Writes point changes to log.txt
        @param message - message to be written to the log.txt file
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

    /** Returns the points of the requested player.
        @param name - name of player.
        @return points that player has.
    */
    public int getPoints(String name)
    {
        try {
            ResultSet results = m_botAction.SQLQuery(m_sqlHost, "SELECT fldPoints FROM tblRacers WHERE fldName = \'" + name + "\'");
            int points = 0;

            if(results.next())
                points = results.getInt("fldPoints");

            m_botAction.SQLClose( results );
            return points;
        } catch(Exception e) {
            return 0;
        }
    }

    /** Sets the racePlayers variable to the # of starters from the last race.
    */
    public void getPlayers()
    {
        RbRace race = (RbRace)modules.get("Race");
        Track track = race.getTrack(race.currentTrack);
        int players = 0;
        ArrayList<ArrayList<String>> trackPositions = track.playerPositions;
        ArrayList<String> currentLap;

        for(int k = 0; k < trackPositions.size(); k++)
        {
            currentLap = trackPositions.get(k);
            players += currentLap.size();
        }

        racePlayers = players;
        track.playerPositions = new ArrayList<ArrayList<String>>();
    }

    /** Inserts the last race's data into tblRaceData
        @param type - Type of race
        @param host - host of the race.
    */
    public void raceStatsUpdate(String type, String host)
    {
        getPlayers();
        RbRace race = (RbRace)modules.get("Race");
        Track track = race.getTrack(race.currentTrack);
        String trackName = track.trackName;
        int laps = race.laps;

        try {
            m_botAction.SQLQueryAndClose(m_sqlHost, "INSERT INTO tblRaceData (fldID, fldDate, fldTrack, fldLaps, fldStarters, fldFinishers, fldType, fldFirst, fldSecond, fldThird, fldHost) VALUES (0, \"" + getTimeStamp() + "\", \"" + trackName + "\", " + laps + ", " + racePlayers + ", " + track.positions.size() + ", \"" + type + "\", \"" + track.winner + "\", \"" + track.second + "\", \"" + track.third + "\", \"" + host + "\") ");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /** Inserts point change data into the tblPointsData sql table.
        @param player - Name of player whose points were changed.
        @param points - Change in points.
        @param reason - reason for change.
    */
    public void pointChange(String player, int points, String reason)
    {
        try {
            m_botAction.SQLQueryAndClose(m_sqlHost, "INSERT INTO tblPointsData (fldID, fldName, fldPoints, fldReason, fldTime) VALUES (0,  " + getID(player) + ", " + points + ", \"" + reason + "\", \"" + getTimeStamp() + "\")");
        } catch(Exception e) {}
    }

    /** Returns the player's tblRacers fldID.
        @param name - Name of player.
        @return ID
    */
    public int getID(String name)
    {
        try {
            ResultSet results = m_botAction.SQLQuery(m_sqlHost, "SELECT fldID FROM tblRacers WHERE fldName = \'" + name + "\'");
            results.next();
            int id = results.getInt("fldID");
            m_botAction.SQLClose( results );
            return id;
        } catch(Exception e) {}

        return 0;
    }

    /** Returns a timestamp for the database.
        @return Returns a timestamp in the format MM/DD/YYYY HH:MM
    */
    public String getTimeStamp()
    {
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        String date = "";

        if(calendar.get(Calendar.MONTH) < 9)
            date += "0" + (calendar.get(Calendar.MONTH) + 1);
        else
            date += (calendar.get(Calendar.MONTH) + 1);

        if(calendar.get(Calendar.DAY_OF_MONTH) < 10)
            date += "/" + "0" + (calendar.get(Calendar.DAY_OF_MONTH));
        else
            date += "/" + calendar.get(Calendar.DAY_OF_MONTH);

        date += "/" + calendar.get(Calendar.YEAR);
        date += " ";

        if(calendar.get(Calendar.HOUR_OF_DAY) < 10)
            date += "0" + calendar.get(Calendar.HOUR_OF_DAY);
        else
            date += calendar.get(Calendar.HOUR_OF_DAY);

        if(calendar.get(Calendar.MINUTE) < 10)
            date += ":0" + calendar.get(Calendar.MINUTE);
        else
            date += ":" + calendar.get(Calendar.MINUTE);

        return date;
    }

}