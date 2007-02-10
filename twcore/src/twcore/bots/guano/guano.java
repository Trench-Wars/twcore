package twcore.bots.guano;
import twcore.core.*;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;

import java.sql.*;
import java.util.*;

public class guano extends SubspaceBot
{
    public static final String TWSITES_DATABASE = "server";
    public static final String FIND_DELIMETER = " - ";
    public static final int CHECK_LENGTH = 180;
    public static final int CHECK_DURATION = 1;
    public static final int DIE_DELAY = 100;

    private OperatorList opList;
    private AltCheck currentCheck;

    /**
     *
     * @param botAction
     */
    public guano(BotAction botAction)
    {
        super(botAction);
        currentCheck = new AltCheck();
    }

    /**
     * This method initializes the bot when it logs on.
     *
     * @param event is the event to handle.
     */
    public void handleEvent(LoggedOn event)
    {
        BotSettings botSettings = m_botAction.getBotSettings();
        String initialArena = botSettings.getString("InitialArena");

        opList = m_botAction.getOperatorList();
        m_botAction.changeArena(initialArena);
        requestEvents();
    }

    /**
     * This method handles a message sent to the bot.
     *
     * @param event is the message to handle.
     */
    public void handleEvent(Message event)
    {
        String sender = getSender(event);
        String message = event.getMessage().trim();
        int messageType = event.getMessageType();

        if(messageType == Message.ARENA_MESSAGE)
            handleArenaMessage(message);
        if(sender != null && opList.isHighmod(sender) &&
                (messageType == Message.REMOTE_PRIVATE_MESSAGE || messageType == Message.PRIVATE_MESSAGE))
            handleCommand(sender, message);
    }

    /**
     * This method handles an arena message.  If it is the result of a *locate
     * command, and a check is active, it will send the results of the find to the
     * person.
     *
     * @param message is the arena message to handle.
     */
    private void handleArenaMessage(String message)
    {
        String name;
        int endOfNameIndex;

        if(currentCheck.isActive())
        {
            endOfNameIndex = message.indexOf(FIND_DELIMETER);
            if(endOfNameIndex != -1) {
                name = message.substring(0, endOfNameIndex).trim();
                currentCheck.addResult(name, message);
            }
        }
    }

    /**
     * This private message handles a command sent to the bot.
     *
     * @param sender is the sender of the command.
     * @param message is the message that was sent.
     */
    private void handleCommand(String sender, String message)
    {
        try
        {
            String command = message.toLowerCase();

            if(command.startsWith("!find "))
                doFindCmd(sender, message.substring(6).trim());
            if(command.startsWith("!warning "))
                doWarningCmd(sender, message.substring(9).trim());
            if(command.equals("!die"))
                doDieCmd(sender);
            if(command.equals("!help"))
                doHelpCmd(sender);
        }
        catch(Exception e)
        {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }

    private void doDieCmd(String sender)
    {
        m_botAction.sendSmartPrivateMessage(sender, "Logging off.");
        m_botAction.scheduleTask(new DieTask(), DIE_DELAY);
    }

    private void doHelpCmd(String sender)
    {
        String[] message =
        {
                "!Find <PlayerName>        -- Find a player on all their altnicks.",
                "!Warning <PlayerName>     -- Checks warnings of a player on all their altnicks.",
                "!Die                      -- Logs the bot off.",
                "!Help                     -- Displays this help message."
        };
        m_botAction.smartPrivateMessageSpam(sender, message);
    }

    /**
     * This method finds a player if they are in the zone, using any of their
     * known altnicks.
     *
     * @param argString
     */
    private void doFindCmd(String sender, String argString)
    {
        if(currentCheck.isActive())
            throw new RuntimeException("Already performing a check.  Please try again momentarily.");

        Vector altNicks = getAltNicks(argString);
        if(altNicks.isEmpty())
            throw new RuntimeException("Player not found in database.");
        currentCheck.startCheck(sender, altNicks);
        locateAll(altNicks);
        m_botAction.scheduleTask(new EndCheckTask(), CHECK_DURATION * 1000);
    }

    /**
     *
     * @param sender
     * @param argString
     */
    private void doWarningCmd(String sender, String argString)
    {
        if(currentCheck.isActive())
            throw new RuntimeException("Already performing a check.  Please try again momentarily.");

        Vector altNicks = getAltNicks(argString);
        if(altNicks.isEmpty())
            throw new RuntimeException("Player not found in database.");
        findWarnings(sender, argString, altNicks);
    }

    private void queryWarnings(String sender, String playerName)
    {
        try
        {
            ResultSet resultSet = m_botAction.SQLQuery(TWSITES_DATABASE,
                    "SELECT * " +
                    "FROM tblWarnings " +
                    "WHERE name = \"" + playerName.toLowerCase() + "\"");

            while (resultSet.next())
                m_botAction.sendRemotePrivateMessage(sender, resultSet.getString("warning"));
        }
        catch(SQLException e)
        {
            throw new RuntimeException("Unable to connect to database.");
        }
    }

    /**
     * This private method performs a locate command on a Vector of names.
     *
     * @param altNicks are the names to perform the altnick command on.
     */
    private void locateAll(Vector altNicks)
    {
        for(int index = 0; index < altNicks.size(); index++)
            m_botAction.sendUnfilteredPublicMessage("*locate " + (String) altNicks.get(index));
    }

    /**
     * This private method finds the warnings for a Vector of names.
     *
     * @param altNicks are the names to perform the altNick command on.
     */
    private void findWarnings(String sender, String name, Vector altNicks)
    {
        if( altNicks.size() <= 0 )
            throw new RuntimeException( "Unable to retreive any altnicks for player." );

        m_botAction.sendRemotePrivateMessage(sender, "Warnings for " + name + ":");

        try {
            Iterator i = altNicks.iterator();
            String altnick;
            while( i.hasNext() ) {
                altnick = (String)i.next();
                queryWarnings(sender, altnick);
            }
            m_botAction.sendRemotePrivateMessage(sender, "End of list.");
        } catch (Exception e) {
            throw new RuntimeException( "Unexpected error while querying altnicks." );
        }
    }

    /**
     * This private method performs an altnick query on a players name and returns
     * the results in the form of a ResultSet.
     * @param playerName is the player to check.
     * @return the results of the query are returned.
     */
    private Vector getAltNicks(String playerName)
    {
        try
        {
            Vector altNicks = new Vector();
            ResultSet resultSet = m_botAction.SQLQuery(TWSITES_DATABASE,
                    "SELECT * " +
                    "FROM tblAlias A1, tblAlias A2, tblUser U1, tblUser U2 " +
                    "WHERE U1.fcUserName = '" + Tools.addSlashesToString(playerName) + "' " +
                    "AND U1.fnUserID = A1.fnUserID " +
                    "AND A1.fcIP = A2.fcIP " +
                    "AND A1.fnMachineID = A2.fnMachineID " +
                    "AND A2.fnUserID = U2.fnUserID " +
            "ORDER BY U2.fcUserName, A2.fdUpdated");
            String lastName = "";
            String currName;
            if(resultSet == null)
                throw new RuntimeException("ERROR: Cannot connect to database.");

            while(resultSet.next())
            {
                currName = resultSet.getString("U2.fcUserName");
                if(!currName.equalsIgnoreCase(lastName))
                    altNicks.add(currName);
                lastName = currName;
            }
            return altNicks;
        }
        catch(SQLException e)
        {
            throw new RuntimeException("ERROR: Cannot connect to database.");
        }
    }

    /**
     * This private method requests the events that the bot will use.
     */
    private void requestEvents()
    {
        EventRequester eventRequester = m_botAction.getEventRequester();

        eventRequester.request(EventRequester.MESSAGE);
    }

    /**
     * This method gets the sender of a message from a Message event.  If there
     * was no sender, then null is returned.
     *
     * @param event is the event to get.
     * @return the sender of the message is returned.  If there is no sender then
     * null is returned.
     */
    private String getSender(Message event)
    {
        int messageType = event.getMessageType();
        int playerID;

        if(messageType == Message.CHAT_MESSAGE ||
                messageType == Message.REMOTE_PRIVATE_MESSAGE ||
                messageType == Message.ALERT_MESSAGE)
            return event.getMessager();

        playerID = event.getPlayerID();
        return m_botAction.getPlayerName(playerID);
    }

    private class AltCheck
    {
        public static final int FIND_CHECK = 0;

        private TreeMap checkResults;
        private String checkSender;
        private boolean isActive;

        public AltCheck()
        {
            checkResults = new TreeMap();
            isActive = false;
        }

        public void startCheck(String checkSender, Vector altNicks)
        {
            if(isActive)
                throw new RuntimeException("Already performing a check.  Please try again momentarily.");
            this.checkSender = checkSender;
            checkResults.clear();
            populateResults(altNicks);
            isActive = true;
        }

        public String getCheckSender()
        {
            return checkSender;
        }

        /**
         * This method stops the current check.
         */
        public void stopCheck()
        {
            isActive = false;
        }

        /**
         * This method gets the number of checks that were made.
         *
         * @return the number of checks that were made is returned.
         */
        public int getNumNicks()
        {
            return checkResults.size();
        }

        /**
         * This method gets the results of a check.
         *
         * @return a Vector containing the results of a check is returned.
         */
        public Vector getResults()
        {
            Vector results = new Vector();
            Collection allResults = checkResults.values();
            Iterator iterator = allResults.iterator();
            String result;

            while(iterator.hasNext())
            {
                result = (String) iterator.next();
                if(result != null)
                    results.add(result);
            }

            return results;
        }

        /**
         * This method adds a result to the result map.
         *
         * @param name is the name of the nick that got the result.
         * @param result is the result of the check.
         */
        public void addResult(String name, String result)
        {
            String lowerName = name.toLowerCase();
            if(checkResults.containsKey(lowerName))
                checkResults.put(lowerName, result);
        }

        /**
         * This method checks to see if a check is currently active.
         *
         * @return true is returned if the check is active.
         */
        public boolean isActive()
        {
            return isActive;
        }

        /**
         * This private method populates the keys of the result map with the names
         * from the altNick check.
         *
         * @param altNicks are the names that will be checked.
         */
        private void populateResults(Vector altNicks)
        {
            String altNick;

            for(int index = 0; index < altNicks.size(); index++)
            {
                altNick = (String) altNicks.get(index);
                checkResults.put(altNick.toLowerCase(), null);
            }
        }
    }

    /**
     * This private method displays the results of a check.
     *
     * @param sender
     * @param numNicks
     * @param checkType
     * @param results
     */
    private void displayResults(String sender, int numNicks, Vector results)
    {
        if(results.isEmpty())
            throw new RuntimeException("Player not online.");
        for(int index = 0; index < results.size(); index++)
            m_botAction.sendSmartPrivateMessage(sender, (String) results.get(index));
        m_botAction.sendSmartPrivateMessage(sender, "Checked " + numNicks + " names.");
    }

    /**
     * <p>Title: </p>
     * <p>Description: </p>
     * <p>Copyright: Copyright (c) 2004</p>
     * <p>Company: </p>
     * @author not attributable
     * @version 1.0
     */
    private class EndCheckTask extends TimerTask
    {
        public void run()
        {
            Vector results = currentCheck.getResults();
            String checkSender = currentCheck.getCheckSender();
            int numNicks = currentCheck.getNumNicks();

            try
            {
                currentCheck.stopCheck();
                displayResults(checkSender, numNicks, results);
            }
            catch(RuntimeException e)
            {
                m_botAction.sendSmartPrivateMessage(checkSender, e.getMessage());
            }
        }
    }

    private class DieTask extends TimerTask
    {
        public void run()
        {
            m_botAction.die();
        }
    }
}
