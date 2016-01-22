package twcore.bots.multibot.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/**
    Lagout util for MultiBot.

    This module is designed specifically for my spec module.  In addition to
    handling lagouts, it also monitors who lags out of the game.  The user is
    able to be notified in the following cases:

    1) anyone lags out.
    2) anyone on a certain freq lags out.
    3) anyone in a certain ship lags out.
    4) a specific player lags out.

    The syntax for the lagout module is as follows:

    In order to use the notify feature, the bot must be currently monitoring
    lagouts.  The !lagon command can take up to 2 arguments.  If one argument is
    not supplied, then the default value will be used.  The default values are
    3 lagouts and 30 seconds to return.  You are now able to turn off the
    messages that the bot gives out when a player is added to the game (yay!).
    The !remove command will spec and remove a player from the game.

    NOTE: There is no more need to specify the number of deaths.  The bot will
    figure it out from the spec module.

    Players that are registered with the lagout handler will be able to see their
    status by typing !status to the bot.  !help works for them as well.

    The notify portion of the lagout module works as follows:
    You can add notify tasks to the bot and it will inform you if someone meeting
    those critera are specced or leave the arena.

    Here is the help menu:
    !Lagon                                    -- Enables the lagout handler.
    !Lagon [Lagouts]                          -- Enables the lagout handler with [Lagouts] Lagouts.
    !Lagon [Lagouts] [Time]                   -- Enables the lagout handler with [Lagouts] Lagouts and [Time] Seconds.
    !EnterMsg [On / Off]                      -- Toggles the enter messages sent by the bot.
    !Remove [Player]                          -- Specs and removes a player from the game.
    !LagOff                                   -- Disables the lagout handler.
    !Notify                                   -- Notifies the host if someone lags out.
    !NotifyFreq [Freq]                        -- Notifies the host if someone from freq [Freq] lags out.
    !NotifyShip [Ship]                        -- Notifies the host if someone in ship [Ship] lags out.
    !NotifyPlayer [Player]                    -- Notifies the host if player [Player] lags out.
    !NotifyList                               -- Displays a list of notify tasks.
    !NotifyDel [Notify Number]                -- Removes a notify task.
    !NotifyOff                                -- Clears all notify tasks.

    Due to the fact that this help menu is SUPER long, some of the messages might
    get lost.

    Author: Cpt.Guano!
    June 21, 2003

    Upadted:
    July 06, 2003 - Fixed the bug where if the player leaves the zone the lagout
                   handler wouldnt recognize that they were lagged out.
*/
public class utillagout extends MultiUtil
{
    public static final int DEFAULT_LAGOUTS_ALLOWED = 3;
    public static final double DEFAULT_TIME_ALLOWED = 90;

    private HashMap<String, LagoutTask> lagoutList;
    private String host;
    private Vector<NotifyTask> notifyList;
    private int lagoutsAllowed;
    private double timeAllowed;
    private boolean enterMsg;
    private boolean lagoutOn;

    /**
        This method initializes the lagout module.
    */
    public void init()
    {
        lagoutList = new HashMap<String, LagoutTask>();
        notifyList = new Vector<NotifyTask>();
        enterMsg = true;
        lagoutOn = false;
    }

    /**
        Requests events.
    */
    public void requestEvents( ModuleEventRequester modEventReq ) {
        modEventReq.request(this, EventRequester.PLAYER_ENTERED );
        modEventReq.request(this, EventRequester.PLAYER_LEFT );
        modEventReq.request(this, EventRequester.FREQUENCY_CHANGE );
        modEventReq.request(this, EventRequester.FREQUENCY_SHIP_CHANGE );
    }

    /**
        This method returns an array of strings that contains the help menu.  If
        the lagout handler is not activated then the notify menu will not be
        displayed.
    */
    public String[] getHelpMessages()
    {
        String[] lagonMessage =
        {
            "!Lagon                                    -- Enables the lagout handler.",
            "!Lagon <Lagouts>                          -- Enables the lagout handler with <Lagouts> Lagouts.",
            "!Lagon <Lagouts> <Time>                   -- Enables the lagout handler with <Lagouts> Lagouts and <Time> Seconds.",
            "!EnterMsg <On / Off>                      -- Toggles the enter messages sent by the bot."
        };

        String[] notifyMessage =
        {
            "!Remove <Player>                          -- Specs and removes a player from the game.",
            "!LagOff                                   -- Disables the lagout handler.",
            "!Notify                                   -- Notifies the host if someone lags out.",
            "!NotifyFreq <Freq>                        -- Notifies the host if someone from freq <Freq> lags out.",
            "!NotifyShip <Ship>                        -- Notifies the host if someone in ship <Ship> lags out.",
            "!NotifyPlayer <Player>                    -- Notifies the host if player <Player> lags out.",
            "!NotifyList                               -- Displays a list of notify tasks.",
            "!NotifyDel <Notify Number>                -- Removes a notify task.",
            "!NotifyOff                                -- Clears all notify tasks."
        };

        if(!lagoutOn)
            return lagonMessage;

        String[] message = new String[lagonMessage.length + notifyMessage.length];
        System.arraycopy(lagonMessage, 0, message, 0, lagonMessage.length);
        System.arraycopy(notifyMessage, 0, message, lagonMessage.length, notifyMessage.length);
        return message;
    }

    /**
        This method displays the player help menu.

        @param sender is the player that is requesting help.
    */
    public void doPlayerHelpCmd(String sender)
    {
        String[] message =
        {
            "!Status                                   -- Displays your current lagout status.",
            "!Lagout                                   -- Places you back in the game if you lag out.",
            "!Help                                     -- Displays this help message."
        };
        m_botAction.smartPrivateMessageSpam(sender, message);
    }

    /**
        This method turns on the lagout handler.

        @param sender is the person using the bot.
        @param argString are the arguments for the command.
    */
    public void doModLagonCmd(String sender, String argString)
    {
        StringTokenizer argTokens = new StringTokenizer(argString);
        int numTokens = argTokens.countTokens();

        if(lagoutOn)
            throw new RuntimeException("Lagout handler already enabled.");

        if(numTokens > 2)
            throw new IllegalArgumentException("Please use the following format: !Lagon <Lagouts> <Time>");

        try
        {
            int newLagoutsAllowed = DEFAULT_LAGOUTS_ALLOWED;
            double newTimeAllowed = DEFAULT_TIME_ALLOWED;

            if(numTokens > 0)
                newLagoutsAllowed = Integer.parseInt(argTokens.nextToken());

            if(numTokens > 1)
                newTimeAllowed = Double.parseDouble(argTokens.nextToken());

            if(newLagoutsAllowed < 0)
                throw new IllegalArgumentException("Invalid number of lagouts.");

            if(newTimeAllowed < 0)
                throw new IllegalArgumentException("Invalid time allowed to re-enter");

            lagoutsAllowed = newLagoutsAllowed;
            timeAllowed = newTimeAllowed;
            updatePlayers();
            m_botAction.sendArenaMessage("Lagout handler enabled.  Please private message " + m_botAction.getBotName() + " with !lagout if you lag out.");
            m_botAction.sendArenaMessage(getSettingString(), 2);
            lagoutOn = true;
            host = sender;
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException("Please use the following format: !Lagon <Lagouts> <Time>");
        }
    }

    /**
        This method turns the enter game messages on or off.

        @param sender is the person using the bot.
        @param argString are the arguments for the command.
    */
    public void doModEnterMsgCmd(String sender, String argString)
    {
        if(argString.equalsIgnoreCase("on"))
        {
            if(enterMsg)
                throw new IllegalArgumentException("Enter messages are already enabled.");

            enterMsg = true;
            m_botAction.sendSmartPrivateMessage(sender, "Enter message enabled.");
        }
        else if(argString.equalsIgnoreCase("off"))
        {
            if(!enterMsg)
                throw new IllegalArgumentException("Enter messages are already disabled.");

            enterMsg = false;
            m_botAction.sendSmartPrivateMessage(sender, "Enter message disabled.");
        }
        else
            throw new IllegalArgumentException("Please use the following format: !EnterMsg <On / Off>");
    }

    /**
        This method specs and removes a player from the game.

        @param sender is the person using the bot.
        @param argString are the arguments for the command.
    */
    public void doModRemoveCmd(String sender, String argString)
    {
        String playerName = getFuzzyLaggot(argString);

        if(playerName == null)
            throw new IllegalArgumentException("Player is not registered with the lagout handler.");

        lagoutList.remove(playerName);
        m_botAction.spec(playerName);
        m_botAction.spec(playerName);
        m_botAction.sendSmartPrivateMessage(sender, playerName + " has been removed from the game.");
    }

    /**
        This method turns the lagout handler off.

        @param sender is the person using the bot.
    */
    public void doModLagoffCmd(String sender)
    {
        lagoutOn = false;
        lagoutList.clear();
        m_botAction.sendArenaMessage("Lagout handler disabled.");
    }

    /**
        This method notifies the host if any player lags out of the game.

        @param sender is the person using the bot.
    */
    public void doNotifyCmd(String sender)
    {
        NotifyTask notifyTask = new NotifyTask();
        notifyList.add(notifyTask);
        m_botAction.sendSmartPrivateMessage(sender, notifyTask.toString());
    }

    /**
        This notifies the host if a player on a certain freq lags out of the game.

        @param sender is the person using the bot.
        @param argString are the arguments for the command.
    */
    public void doNotifyFreqCmd(String sender, String argString)
    {
        try
        {
            int freq = Integer.parseInt(argString);
            NotifyTask notifyTask = new NotifyTask(freq, NotifyTask.NOTIFY_FREQ);
            addTask(notifyTask);
            m_botAction.sendSmartPrivateMessage(sender, notifyTask.toString());
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException("Please use the following format: !NotifyFreq <Freq>");
        }
    }

    /**
        This notifies the host if a player in a certain ship lags out of the game.

        @param sender is the person using the bot.
        @param argString are the arguments for the command.
    */
    public void doNotifyShipCmd(String sender, String argString)
    {
        try
        {
            int ship = Integer.parseInt(argString);
            NotifyTask notifyTask = new NotifyTask(ship, NotifyTask.NOTIFY_SHIP);
            addTask(notifyTask);
            m_botAction.sendSmartPrivateMessage(sender, notifyTask.toString());
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException("Please use the following format: !NotifyShip <Ship>");
        }
    }

    /**
        This notifies the host if a player on a certain player lags out of the game.

        @param sender is the person using the bot.
        @param argString are the arguments for the command.
    */
    public void doNotifyPlayerCmd(String sender, String argString)
    {
        String playerName = m_botAction.getFuzzyPlayerName(argString);

        if(playerName == null)
            throw new IllegalArgumentException("Player not found in the arena.");

        int playerID = m_botAction.getPlayerID(playerName);
        NotifyTask notifyTask = new NotifyTask(playerID, NotifyTask.NOTIFY_PLAYER);
        addTask(notifyTask);
        m_botAction.sendSmartPrivateMessage(sender, notifyTask.toString());
    }

    /**
        This method displays the list of notify tasks.

        @param sender is the person using the bot.
    */
    public void doNotifyListCmd(String sender)
    {
        NotifyTask notifyTask;

        if(notifyList.isEmpty())
            m_botAction.sendSmartPrivateMessage(sender, "Bot not notifying of any lagouts.");

        for(int index = 0; index < notifyList.size(); index++)
        {
            notifyTask = notifyList.get(index);
            m_botAction.sendSmartPrivateMessage(sender, "Task " + index + ") " + notifyTask.toString());
        }
    }

    /**
        This method deletes a notify task.

        @param sender is the person using the bot.
        @param argString are the arguments for the command.
    */
    public void doNotifyDelCmd(String sender, String argString)
    {
        if(notifyList.isEmpty())
            throw new RuntimeException("Bot not notifying of any lagouts.");

        try
        {
            int delIndex = Integer.parseInt(argString);

            if(delIndex < 0 || delIndex >= notifyList.size())
                throw new IllegalArgumentException("Invalid notify number.");

            NotifyTask notifyTask = notifyList.get(delIndex);
            notifyList.remove(delIndex);
            m_botAction.sendSmartPrivateMessage(sender, "Removing Task: " + notifyTask.toString());
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException("Please use the following format: !NotifyDel <Notify Number>");
        }
    }

    /**
        This method removes all notify tasks.

        @param sender is the person using the bot.
    */
    public void doNotifyOffCmd(String sender)
    {
        if(notifyList.isEmpty())
            throw new RuntimeException("Bot not notifying of any lagouts.");

        notifyList.clear();
        m_botAction.sendSmartPrivateMessage(sender, "Removing all notify tasks.");
    }

    /**
        This method handles the mod commands.

        @param sender is the person using the bot.
        @param command is the command
    */
    public void handleModCommand(String sender, String command)
    {
        try
        {
            if(command.equalsIgnoreCase("!lagon"))
                doModLagonCmd(sender, "");

            if(command.startsWith("!lagon "))
                doModLagonCmd(sender, command.substring(7).trim());

            if(command.startsWith("!entermsg "))
                doModEnterMsgCmd(sender, command.substring(10).trim());

            if(lagoutOn)
            {
                if(command.startsWith("!remove "))
                    doModRemoveCmd(sender, command.substring(8).trim());

                if(command.equalsIgnoreCase("!lagoff"))
                    doModLagoffCmd(sender);

                if(command.equalsIgnoreCase("!notify"))
                    doNotifyCmd(sender);

                if(command.startsWith("!notifyfreq "))
                    doNotifyFreqCmd(sender, command.substring(12));

                if(command.startsWith("!notifyship "))
                    doNotifyShipCmd(sender, command.substring(12));

                if(command.startsWith("!notifyplayer "))
                    doNotifyPlayerCmd(sender, command.substring(14));

                if(command.equalsIgnoreCase("!notifylist"))
                    doNotifyListCmd(sender);

                if(command.startsWith("!notifydel "))
                    doNotifyDelCmd(sender, command.substring(11));

                if(command.equalsIgnoreCase("!notifyoff"))
                    doNotifyOffCmd(sender);
            }
        }
        catch(RuntimeException e)
        {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }

    /**
        This method displays the players status.

        @param sender is the person using the bot.
    */
    public void doPlayerStatusCmd(String sender)
    {
        LagoutTask lagoutTask = lagoutList.get(sender);

        m_botAction.sendSmartPrivateMessage(sender, getSettingString());
        m_botAction.sendSmartPrivateMessage(sender, lagoutTask.getStatusString());
    }

    /**
        This method places a player back in the game when he is lagged out.

        @param sender is the person using the bot.
    */
    public void doPlayerLagoutCmd(String sender)
    {
        LagoutTask lagoutTask = lagoutList.get(sender);

        lagoutTask.lagIn();
    }

    public void handlePlayerCommand(String sender, String command)
    {
        try
        {
            if(command.equalsIgnoreCase("!status"))
                doPlayerStatusCmd(sender);

            if(command.equalsIgnoreCase("!lagout"))
                doPlayerLagoutCmd(sender);

            if(command.equalsIgnoreCase("!help"))
                doPlayerHelpCmd(sender);
        }
        catch(RuntimeException e)
        {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }

    public void handleArenaMessage(String message)
    {
        LagoutTask lagoutTask;

        if(lagoutOn)
        {
            String playerName = getSpeccedPlayerName(message);

            if(playerName != null && lagoutList.containsKey(playerName))
            {
                lagoutTask = lagoutList.get(playerName);
                lagoutTask.specOut();
            }
        }
    }

    public void handleEvent(Message event)
    {
        int senderID = event.getPlayerID();
        String sender = m_botAction.getPlayerName(senderID);
        String message = event.getMessage();
        int messageType = event.getMessageType();

        if(messageType == Message.PRIVATE_MESSAGE)
        {
            if(m_opList.isER(sender))
                handleModCommand(sender, message);

            if(lagoutList.containsKey(sender))
                handlePlayerCommand(sender, message);
        }

        if(messageType == Message.ARENA_MESSAGE)
            handleArenaMessage(message);
    }

    public void handleEvent(PlayerEntered event)
    {
        String playerName = event.getPlayerName();
        LagoutTask lagoutTask;

        if(lagoutList.containsKey(playerName))
        {
            lagoutTask = lagoutList.get(playerName);
            lagoutTask.updatePlayerID();

            if(lagoutOn)
                lagoutTask.displayLagoutMessage();
        }
    }

    public boolean isNotifyValid(LagoutTask lagoutTask)
    {
        NotifyTask notifyTask;
        int freq = lagoutTask.getFreq();
        int ship = lagoutTask.getShip();
        int playerID = lagoutTask.getPlayerID();
        Iterator<NotifyTask> iterator = notifyList.iterator();

        while(iterator.hasNext())
        {
            notifyTask = (NotifyTask) iterator.next();

            if(notifyTask.isSameType(freq, ship, playerID))
                return true;
        }

        return false;
    }

    public void handleEvent(PlayerLeft event)
    {
        int playerID = event.getPlayerID();
        LagoutTask lagoutTask;
        Collection<LagoutTask> collection = lagoutList.values();
        Iterator<LagoutTask> iterator = collection.iterator();

        if(lagoutOn)
        {
            while(iterator.hasNext())
            {
                lagoutTask = (LagoutTask) iterator.next();

                if(lagoutTask.getPlayerID() == playerID)
                {
                    lagoutTask.lagOut();

                    if(isNotifyValid(lagoutTask))
                        m_botAction.sendSmartPrivateMessage(host, lagoutTask.getPlayerName() + " (Freq: " + lagoutTask.freq + ", Ship: " + lagoutTask.ship + ") has left the arena.");
                }
            }
        }
    }

    public void handleEvent(FrequencyChange event)
    {
        int playerID = event.getPlayerID();
        String playerName = m_botAction.getPlayerName(playerID);
        LagoutTask lagoutTask;

        if(lagoutOn && lagoutList.containsKey(playerName))
        {
            lagoutTask = lagoutList.get(playerName);
            lagoutTask.update(m_botAction.getPlayer(playerID));
        }
    }

    public void handleEvent(FrequencyShipChange event)
    {
        int playerID = event.getPlayerID();
        String playerName = m_botAction.getPlayerName(playerID);
        Player player = m_botAction.getPlayer(playerID);
        LagoutTask lagoutTask;
        int ship = event.getShipType();

        if(lagoutOn)
        {
            if(lagoutList.containsKey(playerName))
            {
                lagoutTask = lagoutList.get(playerName);

                if(ship == 0)
                {
                    lagoutTask.lagOut();
                    lagoutTask.displayLagoutMessage();

                    if(isNotifyValid(lagoutTask))
                        m_botAction.sendSmartPrivateMessage(host, playerName + " (Freq: " + lagoutTask.freq + ", Ship: " + lagoutTask.ship + ") has been specced.");
                }
                else
                    lagoutTask.update(player);
            }
            else if(ship != 0)
            {
                lagoutList.put(playerName, new LagoutTask(player));

                if(enterMsg)
                    m_botAction.sendSmartPrivateMessage(host, playerName + " has entered in ship " + ship + ".");
            }
        }
    }



    public void cancel()
    {
        lagoutList.clear();
        notifyList.clear();
    }

    private void updatePlayers()
    {
        Iterator<Player> iterator = m_botAction.getPlayingPlayerIterator();
        Player player;
        String playerName;
        LagoutTask lagoutTask;

        while(iterator.hasNext())
        {
            player = (Player) iterator.next();
            playerName = player.getPlayerName();

            if(!lagoutList.containsKey(playerName))
                lagoutList.put(playerName, new LagoutTask(player));
            else
            {
                lagoutTask = lagoutList.get(playerName);
                lagoutTask.update(player);
            }
        }
    }

    private String getFuzzyLaggot(String argString)
    {
        Set<String> set = lagoutList.keySet();
        Iterator<String> iterator = set.iterator();
        String playerName = null;
        String checkName;

        while(iterator.hasNext())
        {
            checkName = (String) iterator.next();

            if(playerName == null && checkName.toLowerCase().startsWith(argString))
                playerName = checkName;

            if(checkName.equalsIgnoreCase(argString))
                return checkName;
        }

        return playerName;
    }

    private String getSettingString()
    {
        StringBuffer returnString = new StringBuffer("Lagouts Allowed: ");

        if(lagoutsAllowed == 0)
            returnString.append(" Unlimited");
        else
            returnString.append(lagoutsAllowed);

        returnString.append(".  Time allowed to re-enter: ");

        if(timeAllowed == 0)
            returnString.append(" Unlimited");
        else
            returnString.append(timeAllowed + " seconds");

        return returnString.toString() + ".";
    }


    public String getSpeccedPlayerName(String s)
    {
        int index = s.indexOf(" is out.  ");

        if(index == -1)
            return null;

        return s.substring(0, index);
    }

    /**
        This private method adds a spec task to the task list.  It will spec any
        players that are over the specified number of deaths.  In addition, it will
        replace any spec tasks that it might conflict with.

        @param specTask is the Spec Task to be added.
    */

    private void addTask(NotifyTask notifyTask)
    {
        int replaceIndex = notifyList.indexOf(notifyTask);

        if(replaceIndex != -1)
            notifyList.remove(replaceIndex);

        notifyList.add(notifyTask);
        Collections.sort(notifyList, new NotifyTaskComparator());
    }

    private class LagoutTask
    {
        private int lagouts;
        private long lagoutTime;
        private boolean laggedOut;
        private boolean speccedOut;
        private int playerID;
        private int ship;
        private int freq;
        private String playerName;

        public LagoutTask(Player player)
        {
            lagouts = 0;
            lagoutTime = 0;
            speccedOut = false;
            playerID = player.getPlayerID();
            freq = player.getFrequency();
            ship = player.getShipType();
            playerName = player.getPlayerName();

            if(ship == 0)
                laggedOut = true;
            else
                laggedOut = false;
        }

        public String getPlayerName()
        {
            return playerName;
        }

        public int getPlayerID()
        {
            return playerID;
        }

        public int getShip()
        {
            return ship;
        }

        public int getFreq()
        {
            return freq;
        }

        @SuppressWarnings("unused")
        public boolean isLaggedOut()
        {
            return laggedOut;
        }

        public void lagOut()
        {
            lagoutTime = System.currentTimeMillis();
            laggedOut = true;
        }

        public String getStatusString()
        {
            StringBuffer returnString = new StringBuffer("Lagouts: " + lagouts);

            if(!laggedOut)
                returnString.append(".  Currently not lagged out.");
            else
            {
                double timeElapsed = (System.currentTimeMillis() - lagoutTime) / 1000.;
                returnString.append(".  Time lagged out for: " + timeElapsed + " seconds.");
            }

            return returnString.toString();
        }

        public void updatePlayerID()
        {
            NotifyTask notifyTask;
            int newPlayerID = m_botAction.getPlayerID(playerName);

            for(int index = 0; index < notifyList.size(); index++)
            {
                notifyTask = notifyList.get(index);
                notifyTask.updatePlayerID(playerID, newPlayerID);
            }

            playerID = newPlayerID;
        }

        public void displayLagoutMessage()
        {
            String playerName = m_botAction.getPlayerName(playerID);

            if(laggedOut && !speccedOut && isValidLagouts() && isValidTime())
                m_botAction.sendSmartPrivateMessage(playerName, "Please private message me with !lagout to get back in the game.");
        }

        public void lagIn()
        {
            if(!laggedOut)
                throw new RuntimeException("You are already in the game.");

            if(speccedOut)
                throw new RuntimeException("You have reached the death limit for this game.");

            if(!isValidLagouts())
                throw new RuntimeException("You have reached the lagout limit for this game.");

            if(!isValidTime())
                throw new RuntimeException("You have reached the lagout time limit for this game.");

            laggedOut = false;
            lagouts++;
            m_botAction.setShip(playerID, ship);
            m_botAction.setFreq(playerID, freq);
        }

        public void update(Player player)
        {
            if(!player.isPlaying())
                laggedOut = true;
            else
            {
                laggedOut = false;
                speccedOut = false;
                ship = player.getShipType();
                freq = player.getFrequency();
            }
        }

        public void specOut()
        {
            speccedOut = true;
        }

        private boolean isValidLagouts()
        {
            return (lagouts < lagoutsAllowed) || (lagoutsAllowed == 0);
        }

        private boolean isValidTime()
        {
            return (System.currentTimeMillis() - lagoutTime) < (timeAllowed * 1000) || (timeAllowed == 0);
        }
    }

    private class NotifyTask
    {
        public static final int NOTIFY_ALL = 0;
        public static final int NOTIFY_FREQ = 1;
        public static final int NOTIFY_SHIP = 2;
        public static final int NOTIFY_PLAYER = 3;

        public static final int MAX_FREQ = 9999;
        public static final int MIN_FREQ = 0;
        public static final int MAX_SHIP = 8;
        public static final int MIN_SHIP = 1;

        private int notifyID;
        private int notifyType;

        public NotifyTask()
        {
            notifyType = NOTIFY_ALL;
        }

        public NotifyTask(int notifyID, int notifyType)
        {
            if(notifyType < 0 || notifyType > 3)
                throw new IllegalArgumentException("ERROR: Unknown Notify Type.");

            if((notifyID < MIN_FREQ || notifyID > MAX_FREQ) && notifyType == NOTIFY_FREQ)
                throw new IllegalArgumentException("Invalid freq number.");

            if((notifyID < MIN_SHIP || notifyID > MAX_SHIP) && notifyType == NOTIFY_SHIP)
                throw new IllegalArgumentException("Invalid ship type.");

            this.notifyID = notifyID;
            this.notifyType = notifyType;
        }
        public int getNotifyType()
        {
            return notifyType;
        }

        public void updatePlayerID(int oldID, int newID)
        {
            if(notifyType == NOTIFY_PLAYER && notifyID == oldID)
                notifyID = newID;
        }

        /**
            This method checks to see if a player with the given attributs will
            be specced by this particular spec task.

            @param freq is the freq of the player.
            @param ship is the ship of the player.
            @playerID is the ID of the player.
            @return True if the player is affected, false if not.
        */
        public boolean isSameType(int freq, int ship, int playerID)
        {
            switch(notifyType)
            {
            case NOTIFY_ALL:
                return true;

            case NOTIFY_FREQ:
                return freq == notifyID;

            case NOTIFY_SHIP:
                return ship == notifyID;

            case NOTIFY_PLAYER:
                return playerID == notifyID;
            }

            return false;
        }

        /**
            This method returns a string representation of the spec task.

            @return a string representation of the spec task is returned.
        */
        public String toString()
        {
            String notifyTypeName;

            switch(notifyType)
            {
            case NOTIFY_ALL:
                notifyTypeName = "all players";
                break;

            case NOTIFY_FREQ:
                notifyTypeName = "freq " + notifyID;
                break;

            case NOTIFY_SHIP:
                notifyTypeName = "ship " + notifyID;
                break;

            case NOTIFY_PLAYER:
                notifyTypeName = "player " + m_botAction.getPlayerName(notifyID);
                break;

            default:
                notifyTypeName = "ERROR: Unknown Spec Type";
            }

            return "Monitoring " + notifyTypeName + " for lagouts.";
        }

        /**
            This overrides the equals method so as to check the spec types and the
            spec IDs.

            @param obj is the other spec task to check.
            @return True if the spec tasks have the same specID and the same
            specType.  False if not.
        */
        public boolean equals(Object obj)
        {
            NotifyTask notifyTask = (NotifyTask) obj;
            return notifyTask.notifyID == notifyID && notifyTask.notifyType == notifyType;
        }
    }

    /**
        This private class compares two spec tasks and orders them in the following
        manner:
        From highest priority to lowest: SPEC_PLAYER, SPEC_SHIP, SPEC_FREQ,
        SPEC_ALL.  In each subcategory they are ordered in order of descending
        deaths.
    */
    private class NotifyTaskComparator implements Comparator<NotifyTask>
    {
        /**
            This method provides a compare function for two specTasks.  This is used
            for ordering the tasks in the list.

            @param obj1 is the first spec task to compare.
            @param obj2 is the second spec task to compare.
            @return a numerical representation of the difference of the two spec
            tasks.
        */

        public int compare(NotifyTask task1, NotifyTask task2)
        {
            int value1 = task1.getNotifyType() * NotifyTask.MAX_FREQ + task1.notifyID;
            int value2 = task2.getNotifyType() * NotifyTask.MAX_FREQ + task2.notifyID;

            return value2 - value1;
        }
    }
}
