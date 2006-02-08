package twcore.core;

import java.util.*;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
/**
 * The main bot utility class, your bot's best and easiest method of performing
 * actions in the arena, getting information on players, scheduling tasks to be
 * performed, and much more.  Being familiar with the features of BotAction will
 * allow you to do just about anything you require.  Every bot has a reference
 * to BotAction available as m_botAction, thanks to the bot superclass, SubspaceBot.
 * <p>
 * <u>Some of BotAction's abilities include:</u><br>
 *  - Sending public, private, arena, zone and warning messages.<br>
 *  - Setting ships, freqs, locking/unlocking arenas, spec'ing, warping, and setting doors.<br>
 *  - Scheduling tasks to run at a future time, or run repeatedly.<br>
 *  - Creating teams by team size or number of teams, and setshipping or warping all on a freq.<br>
 *  - Controlling the bot as a playing ship, changing arenas, and getting an arena list.<br>
 *  - Setting how reliable position data for players is (important!).<br>
 *  - Showing and hiding LVZ objects from one or all players using the Objset class to keep track.<br>
 *  - Accessing SQL databases simply and effectively.<br>
 *  - Sending messages between bots.<br>
 *  - Getting data about players, flags, the bot itself, CFG files, player iterators, and more.<br>
 * <p>
 * Note that this is just SOME of what BotAction can do; it can do much, much more!
 * <p>
 * If you are currently viewing the JavaDoc, and wish to see a more logical and in-depth
 * organization of all BotAction has to offer, <b><u>look at the BotAction source</b></u>.
 */
public class BotAction
{
    private Timer               m_timer;            // Timer used to schedule TimerTasks
    private String              m_arenaName;        // Name of arena bot is currently in
    private Session             m_botSession;       // Reference to bot's Session
    private LinkedList          m_timerTasks;       // List of TimerTasks being run
    private Arena               m_arenaTracker;     // Arena tracker holding player/flag data
    private GamePacketGenerator m_packetGenerator;  // Packet creator
    private Objset              m_objectSet;        // For automation of LVZ object setting
    private int                 m_botNumber;        // Bot's internal ID number


    /** Constructor for BotAction.  Don't worry about this, the object has already
     * been constructed for you.
     * @param botSession Bot thread this object is attached to
     * @param packetGenerator Packet generator which is called when packets are sent out
     * @param arena Arena Tracker object used to represent an arena full of players
     */
    public BotAction(GamePacketGenerator packetGenerator, Arena arena, Timer timer, int botNum, Session botSession)
    {
        m_timer = timer;
        m_arenaTracker = arena;
        m_botSession = botSession;
        m_timerTasks = new LinkedList();
        m_packetGenerator = packetGenerator;
        m_botNumber = botNum;
        m_objectSet = new Objset();
    }




    // **********************************************************************************
    //
    //                                TASK SCHEDULING
    //
    // **********************************************************************************
    /*
     * Task scheduling is not difficult in TWCore.  It allows you to create events
     * that will run at a later time, or run repeatedly at regular intervals.
     *
     * To schedule a task, create a new internal class for your bot that extends the
     * TimerTask class, and include a run() method in the class containing the code
     * you want to run when the TimerTask executes.  Then call either scheduleTask
     * (for running once) or scheduleTaskAtFixedRate (for running repeatedly) to
     * schedule the task, and you're done.
     *
     * If you ever need to cancel the task, hold a reference to it and call the cancel
     * method.  Make sure to catch any exceptions that can possibly arise from the
     * task already being cancelled.
     */

    /**
     * Schedules a TimerTask to occur once at a future time.  TimerTask is part of
     * the package java.util.  The only method that a subclass of TimerTask must
     * override is public void run().
     * <p>See the Task Scheduling heading in BotAction source to learn about task scheduling.
     * @param task TimerTask to be executed
     * @param delayms Length of time before execution, in milliseconds
     */
    public void scheduleTask(TimerTask task, long delayms)
    {
        m_timerTasks.add(task);
        m_timer.schedule(task, delayms);
    }

    /**
     * Schedules a TimerTask to occur repeatedly at an interval.  TimerTask is part of
     * the package java.util.  The only method that a TimerTask must override is
     * public void run().
     * <p>See the Task Scheduling heading in BotAction source to learn about task scheduling.
     * @param task TimerTask to be executed
     * @param delayms Length of time before execution, in milliseconds
     * @param periodms Delay between executions after the initial execution, in milliseconds
     */
    public void scheduleTaskAtFixedRate(TimerTask task, long delayms, long periodms)
    {
        m_timerTasks.add(task);
        m_timer.scheduleAtFixedRate(task, delayms, periodms);
    }

    /**
     * Cancels all pending TimerTasks.  You can cancel an individual TimerTask by using
     * task.cancel().  Note that if you cancel a TimerTask and it has already been cancelled,
     * an exception will be generated, which should be caught.
     * <p>See the Task Scheduling heading in BotAction source to learn about task scheduling.
     */
    public void cancelTasks()
    {
        TimerTask temp;

        while (m_timerTasks.size() > 0)
        {
            temp = (TimerTask) m_timerTasks.removeFirst();
            if (temp != null)
            {
                temp.cancel();
            }
        }
    }




    // **********************************************************************************
    //
    //                                    MESSAGING
    //
    // **********************************************************************************
    /*
     * Messages generally consist of three components: a way to identify the target,
     * which is either the player ID or name; the message itself; and optionally, a
     * sound code to play along with the message.
     *
     * Messages come in several different types: public, private, team-wide, arena-wide,
     * zone-wide, to an opposing team, on a chat, or as a public macro.
     *
     * Private messages also come in two flavors.  A normal private message is to
     * someone in the arena (/), whereas a remote is to anywhere on the billing
     * server, and requires the server to look up their location in order to deliver
     * it.  Therefore, normal private messages cause less server load, but also
     * will not be received if the player is outside the arena.  Use Smart private
     * messages if you're unsure of where the player is, and want to guarantee that
     * they receive the message.  TWCore will try to find the person in the arena,
     * and if they can't be found, will send it remotely.
     *
     * Use private message spams to send many messages at once to a player, such as
     * for a help display.
     *
     * [For the most part, this section should be self-explanatory.]
     */

    /**
     * Displays a green arena message to the arena the bot is in.
     * @param message The message to be displayed.
     */
    public void sendArenaMessage(String message)
    {
        sendArenaMessage(message, (byte) 0);
    }

    /**
     * Displays a green arena message to the arena the bot is in, with a sound code.
     * @param message The message to be displayed.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendArenaMessage(String message, int soundCode)
    {
        sendUnfilteredPublicMessage("*arena " + message, soundCode);
    }

    /**
     * Sends a zone wide advertisement.  Do not use unless absolutely necessary.
     * @param message The message to be sent
     */
    public void sendZoneMessage(String message)
    {
        sendZoneMessage(message, (byte) 0);
    }

    /**
     * Sends a zone wide advertisement.  Do not use unless absolutely necessary.
     * Includes a sound code.
     * @param message The away message to be sent
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendZoneMessage(String message, int soundCode)
    {
        sendUnfilteredPublicMessage("*zone " + message, soundCode);
    }

    /**
     * Sends a normal (blue) message to the public chat.
     * @param message The message to be displayed.
     */
    public void sendPublicMessage(String message)
    {
        sendPublicMessage(message, 0);
    }

    /**
     * Sends a normal (blue) message to the public chat with a sound code.
     * @param message The message to be displayed.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendPublicMessage(String message, int soundCode)
    {
        String temp = message.trim();
        char firstChar;

        if (temp.length() > 0)
        {
            firstChar = message.charAt(0);
            if (firstChar != '/' && firstChar != '*' && firstChar != '?' && firstChar != ';')
            {
                sendUnfilteredPublicMessage(message, soundCode);
            }
        }
    }

    /**
     * Sends a message to the public chat as a macro message.  Macro messages may
     * be ignored by players if they choose.  A suggested use for this command would
     * be to print regular rules displays for new players, allowing experienced players
     * to ignore them if they choose.
     * @param message The message to be displayed.
     */
    public void sendPublicMacro(String message)
    {
        sendPublicMacro(message, 0);
    }

    /**
     * Sends a message to the public chat as a macro message.  Macro messages may
     * be ignored by players if they choose.  A suggested use for this command would
     * be to print regular rules displays for new players, allowing experienced players
     * to ignore them if they choose.  This method can also play a sound w/ the macro.
     * @param message The message to be displayed.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendPublicMacro(String message, int soundCode)
    {
        String temp = message.trim();
        char firstChar;

        if (temp.length() > 0)
        {
            firstChar = message.charAt(0);
            if (firstChar != '/' && firstChar != '*' && firstChar != '?' && firstChar != ';')
            {
                sendUnfilteredPublicMacro(message, soundCode);
            }
        }
    }

    /**
     * Sends a private message to someone in the same arena.  Do not attempt to use
     * this method if you are not absolutely certain that the player is in the arena.
     * If you are at all unsure, use sendSmartPrivateMessage instead.
     * @param name The name of the player.
     * @param message The message to be displayed.
     */
    public void sendPrivateMessage(String name, String message)
    {
        int playerID = m_arenaTracker.getPlayerID(name);
        sendPrivateMessage(playerID, message, (byte) 0);
    }

    /**
     * Sends a private message to someone in the same arena.  Do not attempt to use
     * this method if you are not absolutely certain that the player is in the arena.
     * If you are at all unsure, use sendSmartPrivateMessage instead.
     * @param playerID Player ID of the player you wish to send the message to.
     * @param message The message to be displayed.
     */
    public void sendPrivateMessage(int playerID, String message)
    {
        sendPrivateMessage(playerID, message, (byte) 0);
    }

    /**
     * Sends a private message to someone in the same arena.  Do not attempt to use
     * this method if you are not absolutely certain that the player is in the arena.
     * If you are at all unsure, use sendSmartPrivateMessage instead.
     * @param name The name of the player.
     * @param message The message to be displayed.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendPrivateMessage(String name, String message, int soundCode)
    {
        int playerID = m_arenaTracker.getPlayerID(name);
        sendPrivateMessage(playerID, message, soundCode);
    }

    /**
     * Sends a private message to someone in the same arena.  Do not attempt to use
     * this method if you are not absolutely certain that the player is in the arena.
     * If you are at all unsure, use sendSmartPrivateMessage instead.
     * @param playerID Player ID of the player you wish to send the message to.
     * @param message The message to be displayed.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendPrivateMessage(int playerID, String message, int soundCode)
    {
        String temp = message.trim();
        char firstChar;

        if (temp.length() > 0)
        {
            firstChar = message.charAt(0);
            if (firstChar != '/' && firstChar != '*' && firstChar != '?' && firstChar != ';')
            {
                sendUnfilteredPrivateMessage(playerID, message, soundCode);
            }
        }
    }

    /**
     * Sends a smart private message.  A smart private message is a private message
     * sent much like the Continuum client does it.  If a player is not present in the
     * arena, the message will be sent as a remote private message.  Otherwise, the
     * message will be sent as a private message.  Using this will help save server
     * time over remote private messages, and appear more natural to players.
     * @param name The name of the player.
     * @param message The message to be displayed.
     */
    public void sendSmartPrivateMessage(String name, String message)
    {
        sendSmartPrivateMessage(name, message, 0);
    }

    /**
     * Sends a smart private message.  A smart private message is a private message
     * sent much like the Continuum client does it.  If a player is not present in the
     * arena, the message will be sent as a remote private message.  Otherwise, the
     * message will be sent as a private message.  Using this will help save server
     * time over remote private messages, and appear more natural to players.
     * @param name The name of the player.
     * @param message The message to be displayed.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendSmartPrivateMessage(String name, String message, int soundCode)
    {
        int playerID = m_arenaTracker.getPlayerID(name);
        if (playerID == -1)
        {
            sendRemotePrivateMessage(name, message, soundCode);
        }
        else
        {
            sendPrivateMessage(playerID, message, soundCode);
        }
    }

    /**
     * Sends a remote private message.  Remote private messages look like (Name)> to
     * the player, even if the player is in the same arena as you.  Try to use smart
     * private messages instead.
     * @param name The name of the player.
     * @param message The message to be displayed.
     */
    public void sendRemotePrivateMessage(String name, String message)
    {
        sendRemotePrivateMessage( name, message, 0 );
    }

    /**
     * Sends a remote private message.  Remote private messages look like (Name)> to
     * the player, even if the player is in the same arena as you.  Try to use smart
     * private messages instead.
     * @param name The name of the player.
     * @param message The message to be displayed.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendRemotePrivateMessage(String name, String message, int soundCode)
    {
        if( message == null )
            return;
        String temp = message.trim();
        char firstChar;

        if (temp.length() > 0)
        {
            firstChar = message.charAt(0);
            if (firstChar != '/' && firstChar != '*' && firstChar != '?' && firstChar != ';')
            {
                m_packetGenerator.sendChatPacket((byte) 7, (byte) soundCode, (short) 0, ":" + name + ":" + message);
            }
        }
    }

    /**
     * Sends a message to bot's teammates.  If the bot is in spectator mode, it will
     * speak with the players in spectator mode.
     * @param message The message to be displayed.
     */
    public void sendTeamMessage(String message)
    {
        sendTeamMessage(message, (byte) 0);
    }

    /**
     * Sends a message to bot's teammates.  If the bot is in spectator mode, it will
     * speak with the players in spectator mode.  Includes a sound code.
     * @param message The message to be displayed.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendTeamMessage(String message, int soundCode)
    {
        String temp = message.trim();
        char firstChar;

        if (temp.length() > 0)
        {
            firstChar = message.charAt(0);
            if (firstChar != '/' && firstChar != '*' && firstChar != '?' && firstChar != ';')
            {
                m_packetGenerator.sendChatPacket((byte) 3, (byte) soundCode, (short) 0, message);
            }
        }
    }

    /**
     * Sends a message to a whole frequency of players. (")
     * @param frequency The frequency this message is to be sent to.
     * @param message The message to be displayed.
     */
    public void sendOpposingTeamMessageByFrequency(int frequency, String message)
    {
        sendOpposingTeamMessage(frequency, message, (byte) 0);
    }

    /**
     * Sends a message to a whole frequency of players ("), with sound code.
     * @param frequency The frequency this message is to be sent to.
     * @param message The message to be sent
     * @param soundCode Sound code to be sent along with the message (0 if none).
     */
    public void sendOpposingTeamMessageByFrequency(int frequency, String message, int soundCode)
    {
        Iterator i;
        int playerID;
        char firstChar;
        String temp = message.trim();

        if (temp.length() > 0)
        {
            firstChar = message.charAt(0);
            if (firstChar != '/' && firstChar != '*' && firstChar != '?' && firstChar != ';')
            {
                i = m_arenaTracker.getFreqIDIterator(frequency);
                if (i != null)
                {
                    if( i.hasNext() ) {
                        playerID = ((Integer) i.next()).intValue();
                        m_packetGenerator.sendChatPacket((byte) 4, (byte) soundCode, (short) playerID, message);
                    }
                }
            }
        }
    }

    /**
     * Sends a message to a whole frequency of players based on the ID of one player
     * on the frequency.
     * @param playerID The id of the player whose frequency this message is to be sent to.
     * @param message The message to be sent
     * @param soundCode Sound code to be sent along with the message (0 if none).
     */
    public void sendOpposingTeamMessage( int playerID, String message, int soundCode ) {

        char firstChar;
        String temp = message.trim();

        if (temp.length() > 0)
        {
            firstChar = message.charAt(0);
            if (firstChar != '/' && firstChar != '*' && firstChar != '?' && firstChar != ';')
            {
                m_packetGenerator.sendChatPacket((byte) 4, (byte) soundCode, (short)playerID, message);
            }
        }
    }

    /**
     * Sends a message to a whole frequency of players based on the name of a player
     * on that frequency.
     * @param playerName The name of the player whose frequency this message is to be sent to.
     * @param message The message to be sent
     * @param soundCode Sound code to be sent along with the message (0 if none).
     */
    public void sendOpposingTeamMessage( String playerName, String message, int soundCode ){
        int         playerID = m_arenaTracker.getPlayerID( playerName );

        sendOpposingTeamMessage( playerID, message, soundCode );
    }

    /**
     * Sends a chat message to the first chat this bot has joined.
     * @param message The message to be displayed.
     */
    public void sendChatMessage(String message)
    {
        sendChatMessage(1, message, (byte) 0);
    }

    /**
     * Sends a chat message to a specific chat number out of the chats the bot
     * has joined.
     * @param chatNumber Number of the chat to send information to.
     * @param message The message to be displayed.
     */
    public void sendChatMessage(int chatNumber, String message)
    {
        sendChatMessage(chatNumber, message, (byte) 0);
    }

    /**
     * Sends a chat message to a specific chat number, with a sound code.
     * @param chatNumber Number of the chat to send information to.
     * @param message The message to be displayed.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendChatMessage(int chatNumber, String message, int soundCode)
    {
        if( message == null )
            return;
        String temp = message.trim();
        char firstChar;

        if (temp.length() > 0)
        {
            firstChar = message.charAt(0);
            if (firstChar != '/' && firstChar != '*' && firstChar != '?' && firstChar != ';')
            {
                m_packetGenerator.sendChatPacket((byte) 9, (byte) soundCode, (short) 0, ";" + chatNumber + ";" + message);
            }
        }
    }

    /**
     * Sends a squad message to a specific squad.
     * @param squadName Name of the squad
     * @param message Message to send to that squad
     */
    public void sendSquadMessage(String squadName, String message)
    {
        sendSquadMessage(squadName, message, 0);
    }

    /**
     * Sends a squad message to a specific squad with a sound code.
     * @param squadName Name of the squad
     * @param message Message to send to that squad
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendSquadMessage(String squadName, String message, int soundCode)
    {
        m_packetGenerator.sendChatPacket((byte) 7, (byte) soundCode, (short) 0, ":#" + squadName + ":" + message);
    }

    /**
     * Sends a "?help" alert command.
     * @param message The message to be sent along with the alert
     */
    public void sendHelpMessage(String message)
    {
        sendUnfilteredPublicMessage("?help " + message);
    }

    /**
     * Sends a "?cheater" alert command.
     * @param message The message to be sent along with the alert
     */
    public void sendCheaterMessage(String message)
    {
        sendUnfilteredPublicMessage("?cheater " + message);
    }

    /**
     * Sends an alert command as specified.
     * @param type The type of alert command to be sent
     * @param message The message to be sent along with the alert
     */
    public void sendAlertMessage(String type, String message)
    {
        sendUnfilteredPublicMessage("?" + type + " " + message);
    }


    // ***** ULFILTERED MESSAGING *****

    /**
     * Sends a private message without any filtration.  Use this ONLY in situations
     * where you are hard coding in commands to be sent.  For the sake of security, use
     * the filtered methods for *everything* else.
     * @param name Name of the person the message is to be sent to.
     * @param message Message to be sent.
     */
    public void sendUnfilteredPrivateMessage(String name, String message)
    {
        int playerID = m_arenaTracker.getPlayerID(name);
        sendUnfilteredPrivateMessage(playerID, message, (byte) 0);
    }

    /**
     * Sends a private message without any filtration.  Use this ONLY in situations
     * where you are hard coding in commands to be sent.  For the sake of security, use
     * the filtered methods for *everything* else.
     * @param playerID Player ID of the player you wish to send the message to.
     * @param message Message to be sent.
     */
    public void sendUnfilteredPrivateMessage(int playerID, String message)
    {
        sendUnfilteredPrivateMessage(playerID, message, (byte) 0);
    }

    /**
     * Sends a private message without any filtration.  Use this ONLY in situations
     * where you are hard coding in commands to be sent.  For the sake of security, use
     * the filtered methods for *everything* else.
     * @param name Name of the person the message is to be sent to.
     * @param message Message to be sent.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendUnfilteredPrivateMessage(String name, String message, int soundCode)
    {
        int playerID = m_arenaTracker.getPlayerID(name);
        sendUnfilteredPrivateMessage(playerID, message, soundCode);
    }

    /**
     * Sends a private message without any filtration.  Use this ONLY in situations
     * where you are hard coding in commands to be sent.  For the sake of security, use
     * the filtered methods for *everything* else.
     * @param playerID Player ID of the player you wish to send the message to.
     * @param message Message to be sent.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendUnfilteredPrivateMessage(int playerID, String message, int soundCode)
    {
        m_packetGenerator.sendChatPacket((byte) 5, (byte) soundCode, (short) playerID, message);
    }

    /**
     * Sends a public message without any filtration.  Use this ONLY in situations
     * where you are hard coding in commands to be sent.  For the sake of security, use
     * the filtered methods for *everything* else.
     * @param message Message to be sent.
     */
    public void sendUnfilteredPublicMessage(String message)
    {
        sendUnfilteredPublicMessage(message, (byte) 0);
    }

    /**
     * Sends a public message without any filtration.  Use this ONLY in situations
     * where you are hard coding in commands to be sent.  For the sake of security, use
     * the filtered methods for *everything* else.
     * @param message Message to be sent.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendUnfilteredPublicMessage(String message, int soundCode)
    {
        m_packetGenerator.sendChatPacket((byte) 2, (byte) soundCode, (short) 0, message);
    }

   /**
     * Sends a public macro without any filtration.  For the sake of security, use
     * the filtered macro methods unless you know what you're doing.
     * @param message Message to be sent.
     */
    public void sendUnfilteredPublicMacro(String message)
    {
        sendUnfilteredPublicMacro(message, (byte) 0);
    }

    /**
     * Sends a public macro without any filtration.  For the sake of security, use
     * the filtered macro methods unless you know what you're doing.
     * @param message Message to be sent.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendUnfilteredPublicMacro(String message, int soundCode)
    {
        m_packetGenerator.sendChatPacket((byte) 1, (byte) soundCode, (short) 0, message);
    }


    // ***** MULTI-LINE MESSAGING *****

    /**
     * Sends the contents of the String array to the player in private messages.
     * Note that the player should be in the arena or this may fail.
     * @param playerID PlayerID to send the messages to.
     * @param spam The array of Strings to send to the player in private messages.
     */
    public void privateMessageSpam(int playerID, String[] spam)
    {
        for (int i = 0; i < spam.length; i++)
        {
            sendPrivateMessage(playerID, spam[i]);
        }
    }

    /**
     * Sends the contents of the String array to the player in private messages.
     * Note that the player should be in the arena or this may fail.
     * @param playerName Player name to send the messages to.
     * @param spam The array of Strings to send to the player in private messages.
     */
    public void privateMessageSpam(String playerName, String[] spam)
    {
        for (int i = 0; i < spam.length; i++)
        {
            sendPrivateMessage(playerName, spam[i]);
        }
    }

    /**
     * Private message spam but with a generalized collection to allow for dynamic
     * help statements, such as those generated by CommandInterpreter.
     * <p>This will not work remotely as in REMOTE_PRIVATE_MESSAGE type, because
     * playerIDs have no relevance outside of an Arena.  For smart private messages,
     * get the player's name and use privateMessageSpam(String, Collection) instead.
     * @param playerID ID of the player to be spammed
     * @param messages The collection of messages (Need to be String Objects or typed to string objects)
     */
    public void privateMessageSpam(int playerID, Collection messages)
    {
        Iterator i = messages.iterator();
        while (i.hasNext())
        {
            sendPrivateMessage(playerID, (String)i.next());
        }
    }

    /**
     * Private message spam but with a generalized collection to allow for dynamic
     * help statements, such as those generated by CommandInterpreter.
     * <p>Will work if player is in or outside of the arena.
     * @param playerName Name of the player to be spammed
     * @param messages The collection of messages (Need to be String Objects or typed to string objects)
     */
    public void privateMessageSpam(String playerName, Collection messages)
    {
        Iterator i = messages.iterator();
        while (i.hasNext())
        {
            sendSmartPrivateMessage(playerName, (String)i.next());
        }
    }

    /**
     * Sends the contents of the String array to the player in remote private messages.
     * Careful with this one, sending too many across the billing server can cause
     * trouble.
     * @param playerName Name of the player to send the messages to.
     * @param spam The array of Strings to send to the player in private messages.
     */
    public void remotePrivateMessageSpam(final String playerName, final String[] spam)
    {
        for (int i = 0; i < spam.length; i++)
        {
            sendRemotePrivateMessage(playerName, spam[i]);
        }
    }

    /**
     * This sends the contents of the String array to the player in smart private
     * messages.  Smart private messages are private messages that are sent as private
     * messages if the player is in the same arena, and remote private messages if the
     * player is not in the arena.
     * @param playerName Name of the player to send the messages to.
     * @param spam The array of Strings to send to the player in private messages.
     */
    public void smartPrivateMessageSpam(final String playerName, final String[] spam)
    {
        int playerID = m_arenaTracker.getPlayerID(playerName);
        if (playerID == -1)
        {
            remotePrivateMessageSpam(playerName, spam);
        }
        else
        {
            for (int i = 0; i < spam.length; i++)
            {
                sendPrivateMessage(playerID, spam[i]);
            }
        }
    }




    // **********************************************************************************
    //
    //                                  MISC OPERATIONS
    //
    // **********************************************************************************
    /*
     * This is the main section of BotAction, covering the general actions you can use:
     *
     *  - BASIC PLAYER OPERATIONS: Set ships & freqs, warp, send to spec, reset scores,
     *    give prizes, add bounty, send warns, record damage to/from a player.
     *  - ARENA OPERATIONS: Lock/unlock arena, reset flag game, set timer, set doors,
     *    toggle public & private chat on/off, set power of thors in arena.
     *  - COMPLEX OPERATIONS: Team creation by team size or # teams, mass warping,
     *    mass shipsetting, mass setfreq'ing, old spec@death method, arena data clear.
     *  - BOT OPERATIONS: Join & change arenas, move ship, spectate on players, pick up
     *    and drop flags, set a banner, force an in-game death, force complete bot
     *    termination (logoff), force receiving of all death packets, get list of arenas,
     *    add spam protection, increase or decrease reliability of player position data,
     *    adjust rate of packets being sent, send and receive files to server.
     *  - LVZ OBJECT OPERATIONS: Show & hide one LVZ obj, show or hide a set of objs,
     *    and show & hide a set of objects based on objects managed by Objset.
     *  - SQL DATABASE OPERATIONS: Run regular, background and high-priority background
     *    queries, and automatically-formed table insertions for new records.
     *  - INTER-PROCESS COMMUNICATIONS: (For sending messages between bots -- very easy).
     *    Send a standard IPC message, transmit a generic object, subscribe bot to an
     *    IPC channel, unsubscribe from an IPC channel, and destroy an IPC channel.
     *
     */

    // ***** BASIC PLAYER OPERATIONS *****
    /*
     * Simple operations that either have a target, or apply to all players.
     */

    /**
     * Changes the ship type of a player to the type specified.  Ship should be
     * 1 to 8 (0 does not set a player to spectator, though when using getShipType()
     * inside player, 0 will correspond to the spectator ship).  Note that unlike
     * the internal packet protocol, this command uses the more accepted public
     * numbering system instead of using ship 0 for warbird and ship 8 for spectator.
     * @param playerID Player ID of the player you want to change.
     * @param shipType Ship type that you wish to set.  Must be a value between 1 and 8.
     */
    public void setShip(int playerID, int shipType)
    {
        sendUnfilteredPrivateMessage(playerID, "*setship " + shipType);
    }

    /**
     * Changes the ship type of a player to the type specified.  Ship should be
     * 1 to 8 (0 does not set a player to spectator, though when using getShipType()
     * inside player, 0 will correspond to the spectator ship).  Note that unlike
     * the internal packet protocol, this command uses the more accepted public
     * numbering system instead of using ship 0 for warbird and ship 8 for spectator.
     * @param playerName The name of the player.
     * @param shipType Ship type that you wish to set.  Must be a value between 1 and 8.
     */
    public void setShip(String playerName, int shipType)
    {
        sendUnfilteredPrivateMessage(playerName, "*setship " + shipType);
    }

    /**
     * Changes the frequency a player is on.
     * <p>You may also set spectators to a freq with this command so that they
     * can use teamchat there, but if they then enter the game or have their
     * ship set, they will not automatically enter in with that freq.
     * @param playerID Player ID of the player you want to change.
     * @param freqNum Frequency you want this player to be on.
     */
    public void setFreq(int playerID, int freqNum)
    {
        sendUnfilteredPrivateMessage(playerID, "*setfreq " + freqNum);
    }

    /**
     * Changes the frequency a player is on.
     * <p>You may also set spectators to a freq with this command so that they
     * can use teamchat there, but if they then enter the game or have their
     * ship set, they will not automatically enter in with that freq.
     * @param playerName The name of the player.
     * @param freqNum Frequency you want this player to be on.
     */
    public void setFreq(String playerName, int freqNum)
    {
        sendUnfilteredPrivateMessage(playerName, "*setfreq " + freqNum);
    }

    /**
     * Warps a player to the given tile coordinates.  Exact positioning is not
     * offered by this command, operating on the 1 to 1024 scheme rather than
     * the more accurate 1 to 16384 (scale of 16).  512,512 is nearly center.
     * @param playerID PlayerID of the player to be warped
     * @param xTiles X coordinate (1 to 1024)
     * @param yTiles Y coordinate (1 to 1024)
     */
    public void warpTo(int playerID, int xTiles, int yTiles)
    {
        sendUnfilteredPrivateMessage(playerID, "*warpto " + xTiles + " " + yTiles);
    }

    /**
     * Warps a player to the given tile coordinates.  Exact positioning is not
     * offered by this command, operating on the 1 to 1024 scheme rather than
     * the more accurate 1 to 16384 (scale of 16).  512,512 is nearly center.
     * @param playerName The name of the player.
     * @param xTiles X coordinate (1 to 1024)
     * @param yTiles Y coordinate (1 to 1024)
     */
    public void warpTo(String playerName, int xTiles, int yTiles)
    {
        sendUnfilteredPrivateMessage(playerName, "*warpto " + xTiles + " " + yTiles);
    }

    /**
     * Issues a /*spec command to the given player, sending them immediately to
     * spectator mode.  Note that if this command is issued just once, it will
     * lock the player in spectator mode, making it impossible for them to enter
     * an arena even if it's unlocked.  The command needs to be issued twice in
     * order to let them re-enter when the arena is unlocked -- once to spec and
     * lock, and once to unlock.  Alternately you can use specWithoutLock() to
     * automatically issue two /*spec commands.
     * @param playerName The name of the player to be specced.
     */
    public void spec(String playerName)
    {
        sendUnfilteredPrivateMessage(playerName, "*spec");
    }

    /**
     * Issues a /*spec command to the given player, sending them immediately to
     * spectator mode.  Note that if this command is issued just once, it will
     * lock the player in spectator mode, making it impossible for them to enter
     * an arena even if it's unlocked.  The command needs to be issued twice in
     * order to let them re-enter when the arena is unlocked -- once to spec and
     * lock, and once to unlock.  Alternately you can use specWithoutLock() to
     * automatically issue two /*spec commands.
     * @param playerID Player ID of the player you want to spec.
     */
    public void spec(int playerID)
    {
        sendUnfilteredPrivateMessage(playerID, "*spec");
    }

    /**
     * Sends a player immediately to spectator mode, and does not lock them there.
     * If the arena is unlocked, they will be able to re-enter.  This is the
     * standard way to spec someone, and is equivalent to issuing two /*spec's.
     * @param playerName The name of the player to be specced.
     */
    public void specWithoutLock(String playerName) {
        sendUnfilteredPrivateMessage(playerName, "*spec");
        sendUnfilteredPrivateMessage(playerName, "*spec");
    }

    /**
     * Sends a player immediately to spectator mode, and does not lock them there.
     * If the arena is unlocked, they will be able to re-enter.  This is the
     * standard way to spec someone, and is equivalent to issuing two /*spec's.
     * @param playerID Player ID of the player you want to spec.
     */
    public void specWithoutLock(int playerID) {
        sendUnfilteredPrivateMessage(playerID, "*spec");
        sendUnfilteredPrivateMessage(playerID, "*spec");
    }

    /**
     * Places all players in the arena into spectator mode.
     */
    public void specAll()
    {
        sendUnfilteredPublicMessage("*specall");
    }

    /**
     * Resets the scores of an individual player.
     * @param playerName Name of player to be reset.
     */
    public void scoreReset(String playerName)
    {
        sendUnfilteredPrivateMessage(playerName, "*scorereset");
    }

    /**
     * Resets the scores of an individual player.
     * @param playerID Player ID of player to be reset.
     */
    public void scoreReset(int playerID)
    {
        sendUnfilteredPrivateMessage(playerID, "*scorereset");
    }

    /**
     * Resets the scores of all the players in an arena.
     */
    public void scoreResetAll()
    {
        sendUnfilteredPublicMessage("*scorereset");
    }

    /**
     * Issues a *shipreset to a specific player, returning their status, energy,
     * etc. to the way it was at spawn.  If issued on a player that has just died,
     * the player will come back to life at the position of their death.  Note
     * that a shipreset does not remove mines (warping to a safe will, however).
     * @param playerName Name of the player to be *shipreset
     */
    public void shipReset(String playerName)
    {
        sendUnfilteredPrivateMessage(playerName, "*shipreset");
    }

    /**
     * Issues a *shipreset to a specific player, returning their status, energy,
     * etc. to the way it was at spawn.  If issued on a player that has just died,
     * the player will come back to life at the position of their death.  Note
     * that a shipreset does not remove mines (warping to a safe will, however).
     * @param playerID Integer ID reference to the player specified.
     */
    public void shipReset(int playerID)
    {
        sendUnfilteredPrivateMessage(playerID, "*shipreset");
    }

    /**
     * Issues a *shipreset to all in-game players, returning their status, energy,
     * etc. to the way it was at spawn.  Anyone who has just died will come back
     * to life at the position of their death.  Note that a shipreset does not
     * remove mines (warping to a safe will, however).
     */
    public void shipResetAll()
    {
        sendUnfilteredPublicMessage("*shipreset");
    }

    /**
     * Issues one specific prize to a player, using *prize #.  This is the
     * standard method of prizing.<p><code><pre><u>Prize numbers</u>:
     * 1 = Recharge     8 = Guns              15 = MultiFire    22 = Burst
     * 2 = Energy       9 = Bombs             16 = Proximity    23 = Decoy
     * 3 = Rotation    10 = Bouncing Bullets  17 = Super!       24 = Thor
     * 4 = Stealth     11 = Thruster          18 = Shields      25 = Multiprize
     * 5 = Cloak       12 = Top Speed         19 = Shrapnel     26 = Brick
     * 6 = XRadar      13 = Full Charge       20 = AntiWarp     27 = Rocket
     * 7 = Warp        14 = Engine Shutdown   21 = Repel        28 = Portal</pre></code>
     * To take away a prize, or reduce the prize's result by one, use a negative number.
     * @param playerName Name of the player.
     * @param prizeNum Number of the prize to issue.
     */
    public void specificPrize(String playerName, int prizeNum)
    {
        sendUnfilteredPrivateMessage(playerName, "*prize #" + prizeNum);
    }

    /**
     * Issues one specific prize to a player, using *prize #.  This is the
     * standard method of prizing.<p><code><pre><u>Prize numbers</u>:
     * 1 = Recharge     8 = Guns              15 = MultiFire    22 = Burst
     * 2 = Energy       9 = Bombs             16 = Proximity    23 = Decoy
     * 3 = Rotation    10 = Bouncing Bullets  17 = Super!       24 = Thor
     * 4 = Stealth     11 = Thruster          18 = Shields      25 = Multiprize
     * 5 = Cloak       12 = Top Speed         19 = Shrapnel     26 = Brick
     * 6 = XRadar      13 = Full Charge       20 = AntiWarp     27 = Rocket
     * 7 = Warp        14 = Engine Shutdown   21 = Repel        28 = Portal</pre></code>
     * To take away a prize, or reduce the prize's result by one, use a negative number.
     * @param playerID Player ID
     * @param prizeNum Number of the prize
     */
    public void specificPrize(int playerID, int prizeNum)
    {
          sendUnfilteredPrivateMessage(playerID, "*prize #" + prizeNum);
    }

    /**
     * Issues one specific prize to every player in the arena.
     * <p><code><pre><u>Prize numbers</u>:
     * 1 = Recharge     8 = Guns              15 = MultiFire    22 = Burst
     * 2 = Energy       9 = Bombs             16 = Proximity    23 = Decoy
     * 3 = Rotation    10 = Bouncing Bullets  17 = Super!       24 = Thor
     * 4 = Stealth     11 = Thruster          18 = Shields      25 = Multiprize
     * 5 = Cloak       12 = Top Speed         19 = Shrapnel     26 = Brick
     * 6 = XRadar      13 = Full Charge       20 = AntiWarp     27 = Rocket
     * 7 = Warp        14 = Engine Shutdown   21 = Repel        28 = Portal</pre></code>
     * To take away a prize, or reduce the prize's result by one, use a negative number.
     * @param prizeNum Number of the prize.
     */
    public void prizeAll(int prizeNum)
    {
        sendUnfilteredPublicMessage("*prize #" + prizeNum);
    }

    /**
     * Issues one specific prize to an entire frequency of players.
     * <p><code><pre><u>Prize numbers</u>:
     * 1 = Recharge     8 = Guns              15 = MultiFire    22 = Burst
     * 2 = Energy       9 = Bombs             16 = Proximity    23 = Decoy
     * 3 = Rotation    10 = Bouncing Bullets  17 = Super!       24 = Thor
     * 4 = Stealth     11 = Thruster          18 = Shields      25 = Multiprize
     * 5 = Cloak       12 = Top Speed         19 = Shrapnel     26 = Brick
     * 6 = XRadar      13 = Full Charge       20 = AntiWarp     27 = Rocket
     * 7 = Warp        14 = Engine Shutdown   21 = Repel        28 = Portal</pre></code>
     * To take away a prize, or reduce the prize's result by one, use a negative number.
     * @param freqID The frequency of players you wish to issue the prizes to.
     * @param prizeNum Number of the prize.
     */
    public void prizeFreq(int freqID, int prizeNum)
    {
        try
        {
            for (Iterator i = m_arenaTracker.getFreqIDIterator(freqID); i.hasNext();)
            {
                specificPrize(((Integer) i.next()).intValue(), prizeNum);
            }
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Adds a specific amount of bounty to everyone in the arena by giving them
     * random prizes (each equivalent to 1 bounty).  The prizes are determined
     * by the availability and frequency specified in the arena's config file.
     * @param number Amount of bounty / random prizes to give.
     */
    public void giveBounty(int number)
    {
        sendUnfilteredPublicMessage("*prize " + number);
    }

    /**
     * Adds a specific amount of bounty to a player in the arena by giving them
     * random prizes (each equivalent to 1 bounty).  The prizes are determined
     * by the availability and frequency specified in the arena's config file.
     * @param name Name of the player to give the bounty to.
     * @param number Amount of bounty / random prizes to give.
     */
    public void giveBounty(String name, int number)
    {
        sendUnfilteredPrivateMessage(name, "*prize " + number);
    }

    /**
     * Adds a specific amount of bounty to a player in the arena by giving them
     * random prizes (each equivalent to 1 bounty).  The prizes are determined
     * by the availability and frequency specified in the arena's config file.
     * @param playerID PlayerID of the player you want to give the bounty to.
     * @param number Amount of bounty / random prizes to give.
     */
    public void giveBounty(int playerID, int number)
    {
        sendUnfilteredPrivateMessage(playerID, "*prize " + number);
    }

    /**
     * Issues the number of random prizes specified to a specific player.  The
     * prizes are chosen based on the probability in the arena settings.  This
     * method is equivalent to giveBounty, and should not be used because it can
     * be confused with specificPrize, which issues one specific prize to a player.
     * @param playerName Name of the player.
     * @param numPrizes Number of random prizes to issue.
     * @see #specificPrize(String, int)
     * @see #giveBounty(String, int)
     * @deprecated Duplicate functionality of giveBounty.  Often confused with specificPrize
     */
    @Deprecated
    public void prize(String playerName, int numPrizes)
    {
        sendUnfilteredPrivateMessage(playerName, "*prize " + numPrizes);
    }

    /**
     * Issues the number of random prizes specified to a specific player.  The
     * prizes are chosen based on the probability in the arena settings.  This
     * method is equivalent to giveBounty, and should not be used because it can
     * be confused with specificPrize, which issues one specific prize to a player.
     * @param playerID Player ID of the player.
     * @param numPrizes Number of random prizes to issue.
     * @see #specificPrize(int, int)
     * @see #giveBounty(int, int)
     * @deprecated Duplicate functionality of giveBounty.  Often confused with specificPrize
     */
    @Deprecated
    public void prize(int playerID, int numPrizes)
    {
        sendUnfilteredPrivateMessage(playerID, "*prize " + numPrizes);
    }

    /**
     * Warns the player with a moderator warning (red).  The message will be
     * appended with MODERATOR WARNING:, and will be signed with the bot's name.
     * @param playername The name of the player.
     * @param message The message to be sent.
     */
    public void warnPlayer(String playername, String message)
    {
        sendUnfilteredPrivateMessage(playername, "*warn " + message);
    }

    /**
     * Warns the player with a moderator warning (red).  The message will be
     * appended with MODERATOR WARNING:, and will be signed with the bot's name.
     * @param playerID The player to be warned
     * @param message The message to be sent
     */
    public void warnPlayer(int playerID, String message)
    {
        sendUnfilteredPrivateMessage(playerID, "*warn " + message);
    }

    /**
     * Toggles the WatchDamage attribute for this player, which will send
     * detailed information about weapon damage to/from them, and can be picked
     * up by handling WatchDamage events.
     * @param playerID PlayerID of the player to receive damage info about.
     */
    public void toggleWatchDamage(int playerID)
    {
        sendUnfilteredPrivateMessage(playerID, "*watchdamage");
    }

    /**
     * Toggles the WatchDamage attribute for this player, which will send
     * detailed information about weapon damage to/from them, and can be picked
     * up by handling WatchDamage events.
     * @param playerName PlayerName of the player to receive damage info about.
     */
    public void toggleWatchDamage(String playerName)
    {
        sendUnfilteredPrivateMessage(playerName, "*watchdamage");
    }


    // ***** ARENA OPERATIONS *****
    /*
     * Pertaining to anything done that is done to the arena or is arena-specific.
     */

    /**
     * Locks or unlocks the arena, depending on previous lock status.  A locked
     * arena can't be entered by anyone in the standard method, but only by *setship.
     */
    public void toggleLocked()
    {
        sendUnfilteredPublicMessage("*lock");
    }

    /**
     * Resets the flag game so that all stationary flags become neutral, and
     * all grabbable flags are respawned.
     */
    public void resetFlagGame()
    {
        sendUnfilteredPublicMessage("*flagreset");
    }

    /**
     * Sets the *timer game timer to a specified time.  0 to turn off.
     * @param minutes Time until end of game, in minutes.
     */
    public void setTimer(int minutes)
    {
        sendUnfilteredPublicMessage("*timer" + minutes);
    }

    /**
     * Resets the timer on a timed game to its starting value.
     * @see #setTimer(int)
     */
    public void resetTimer()
    {
        sendUnfilteredPublicMessage("*timereset");
    }

    /**
     * Sets the doors in the arena to the specified door value.  The integer
     * provided is a length 8 bit vector.  If you wish to explicitly declare
     * the doors in a String, such as "01010111", use setDoors( String ).
     * @param doors Value of the doors to be set (bitvector).
     */
    public void setDoors(int doors)
    {
        if (doors < -2 || doors > 255)
            return;
        sendUnfilteredPublicMessage("?set door:doormode:" + doors);
    }

    /**
     * Uses a binary string such as "11010110" to turn specific doors on or off.
     * @param eightBinaryDigits Door state as a String
     */
    public void setDoors(String eightBinaryDigits)
    {
        try
        {
            setDoors((int) Byte.parseByte(eightBinaryDigits, 2));
        }
        catch (NumberFormatException e)
        {
            Tools.printStackTrace(e);
        }
    }

    /**
     * Starts or ends 'blue message out,' depending on state (locks public
     * messages being sent inside the arena).
     * <p>The name of this method is confusing, and is incompatible with non-TW
     * standards.  It has been replaced by toggleLockPublicChat.
     * @see #toggleLockPublicChat()
     */
    public void toggleBlueOut()
    {
        toggleLockPublicChat();
    }

    /**
     * Starts or stops the locking of public (blue) messages in the arena.  When
     * this is enabled, players will see their text printed to the chat window,
     * but it will not actually be displayed.
     * <p>All lock options default to off.  After calling this method to switch
     * on, call it to turn off again, or else the arena will keep the lock.
     * <p>Note that lock options do not apply to staff on moderate.txt.
     */
    public void toggleLockPublicChat()
    {
        sendUnfilteredPublicMessage("*lockpublic");
    }

    /**
     * Starts or stops the locking of private messages being sent by anyone
     * in the arena.  (Blocks / and :: msgs)
     * <p>All lock options default to off.  After calling this method to switch
     * on, call it to turn off again, or else the arena will keep the lock.
     * <p>Note that lock options do not apply to staff on moderate.txt.
     */
    public void toggleLockPrivateChat()
    {
        sendUnfilteredPublicMessage("*lockprivate");
    }

    /**
     * Toggles between applying chat locks to spectators or all players.  By
     * default the locks affect all players.  This should be used in conjunction
     * with toggleLockPublicChat() and toggleLockPrivateChat().
     * @see #toggleLockPublicChat()
     * @see #toggleLockPrivateChat()
     */
    public void toggleLocksToSpectators()
    {
        sendUnfilteredPublicMessage("*lockspec");
    }

    /**
     * Sets the power of the thor weapon.  0 is normal, 1 is instant death on contact,
     * and >1 is instant death with increasing proximity as 1 -> infinity.
     * @param thorAdjust Amount to adjust thor power by (0: norm, 1:instant death, >1:instant death w/ prox)
     */
    public void setThorAdjust(int thorAdjust)
    {
        sendUnfilteredPublicMessage("*thor " + thorAdjust);
    }


    // ***** COMPLEX OPERATIONS *****
    /*
     * Operations made up of other operations to form more complex behaviors.
     */

    /**
     * Creates a set of teams of a particular size randomly. Starts at freq 0
     * and continues filling freqs completely until there are no players left.
     * @param teamSize The size of team desired.
     */
    public void createRandomTeams(int teamSize)
    {
        StringBag plist = new StringBag();
        int freq = 0;
        String name;

        //stick all of the players in randomizer
        Iterator i = m_arenaTracker.getPlayingPlayerIterator();
        while(i.hasNext())
            plist.add(((Player)i.next()).getPlayerName());

        while(!plist.isEmpty() && freq > -1)
        {
            for(int x = 0; x < teamSize; x++)
            {
                name = plist.grabAndRemove();
                if(name != null)
                    setFreq(name, freq);
                else
                {
                    freq = -2;
                    break;
                }
            }
            freq++; //that freq is done, move on to the next
        }
    }

    /**
     * Creates a certain number of random teams from non-specced players.  Starts
     * with freq 0 and goes up to number specified - 1, in an even distribution.
     * @param howMany Number of random teams to create.
     */
    public void createNumberOfTeams(int howMany)
    {
        StringBag plist = new StringBag();
        int current = 0;
        howMany -= 1;
        String name;

        // stick all of the players in randomizer
        Iterator i = m_arenaTracker.getPlayingPlayerIterator();
        while(i.hasNext())
            plist.add(((Player)i.next()).getPlayerName());

        // assign players to teams
        while(!plist.isEmpty())
        {
            if(current > howMany)
                current = 0;
            name = plist.grabAndRemove();
            setFreq(name,current);
            current++;
        }
    }

    /**
     * Warps all the players in the arena to location X, Y.
     * @param x X coordinate to warp the players to (1-1024)
     * @param y X coordinate to warp the players to (1-1024)
     */
    public void warpAllToLocation(int x, int y)
    {
        Iterator i = m_arenaTracker.getPlayerIDIterator();
        if (i == null)
            return;
        while (i.hasNext())
        {
            warpTo(((Integer) i.next()).intValue(), x, y);
        }
    }

    /**
     * Warps all the players on a frequency to location X, Y.
     * @param freq Frequency to warp
     * @param x X coordinate to warp the players to (1-1024)
     * @param y Y coordinate to warp the players to (1-1024)
     */
    public void warpFreqToLocation(int freq, int x, int y)
    {
        Iterator i = m_arenaTracker.getFreqIDIterator(freq);
        if (i == null)
        {
            Tools.printLog("Arena: Freq " + freq + " does not exist.");
            return;
        }

        while (i.hasNext())
        {
            int next = ((Integer) i.next()).intValue();
            warpTo(next, x, y);
        }
    }

    /**
     * Sends the warp prize to all players that are currently in a ship, creating
     * the same effect as if they had just hit the warp key.
     */
    public void warpAllRandomly()
    {
        prizeAll(7);
    }

    /**
     * Issues a *setship to all the players in the arena who are not in spec.
     * @param shipType Ship type to switch the players into.
     */
    public void changeAllShips(int shipType)
    {
        if (!(shipType >= 1 && shipType <= 8))
            return;
        for (Iterator i = m_arenaTracker.getPlayingIDIterator(); i.hasNext();)
        {
            setShip(((Integer) i.next()).intValue(), shipType);
        }
    }

    /**
     * Changes all the ships on a freq to a particular ship type.
     * @param freq The frequency of the players you wish to change
     * @param shipType The ship type that you wish to change the players to
     */
    public void changeAllShipsOnFreq(int freq, int shipType)
    {
        if (!(shipType >= 1 && shipType <= 8))
            return;
        Iterator i = m_arenaTracker.getFreqIDIterator(freq);
        if (i == null)
            return;
        while (i.hasNext())
        {
            setShip(((Integer) i.next()).intValue(), shipType);
        }
    }

    /**
     * Changes all the players on one freq to another freq.  Great for
     * consolidating teams.
     * @param initialFreq Frequency of the players you wish to change
     * @param destFreq The frequency you wish to change the players to
     */
    public void setFreqtoFreq(int initialFreq, int destFreq)
    {
        Iterator i = m_arenaTracker.getFreqIDIterator(initialFreq);
        if (i == null)
            return;
        while (i.hasNext())
        {
            setFreq(((Integer) i.next()).intValue(), destFreq);
        }
    }

    /**
     * Changes the frequency of all the playing players in the game to a
     * specified frequency.
     * @param destFreq Frequency to change the players to.
     */
    public void setAlltoFreq(int destFreq)
    {
        Iterator i = m_arenaTracker.getPlayingIDIterator();
        if (i == null)
            return;
        while (i.hasNext())
        {
            setFreq(((Integer) i.next()).intValue(), destFreq);
        }
    }

    /**
     * Checks every player in the game for X number of deaths, and places them
     * in spectator mode.  Normally not used (spec module used instead).  Note
     * that if you use this method, you still have to monitor PlayerDeath packets
     * to check the remaining deaths.  It is not advisable to use checkAndSpec.
     * @param deaths Number of deaths that, if over, will result in a speccing.
     */
    public void checkAndSpec(int deaths)
    {

        Iterator i = m_arenaTracker.getPlayerIterator();
        while (i.hasNext())
        {
            Player p = (Player) i.next();
            if (p.getShipType() != 0 && p.getLosses() >= deaths)
            {
                spec(p.getPlayerID());
                spec(p.getPlayerID());
                sendArenaMessage(p.getPlayerName() + " is out with " + p.getWins() + " wins, " + p.getLosses() + " losses.");
            }
        }
    }

    /**
     * Wipes all data from the arena tracker (player lists, etc)
     */
    public void clearArenaData(){
        m_arenaTracker.clear();
    }


    // ***** BOT OPERATIONS *****
    /*
     * Covers operations that involve the bot performing some kind of action
     * relating directly to itself.
     */

    /**
     * Causes the bot to join an arena at initial login.  If the bot has logged
     * on and is already in an arena, use changeArena( int ) instead.
     * <p>If arenaname contains only a number, then it's assumed to be a pub
     * that is being joined.
     * @param arenaName The name of arena (or number of pub) to join.
     * @author DoCk> (modified by FoN)
     * @see #changeArena(String)
     */
    public void joinArena(String arenaName)
    {
        if (Tools.isAllDigits(arenaName))
        {
            joinArena(Short.parseShort(arenaName));
        }
        else
        {
            m_arenaName = arenaName;
            try
            {
                m_packetGenerator.sendArenaLoginPacket((byte) 8, (short) 1024, (short) 768, (short) 0xFFFD, arenaName);
            }
            catch (Exception e)
            {
                //don't do anything cause hardcoded res is correct
            }

            m_packetGenerator.sendSpectatePacket((short) - 1);
        }
    }

    /**
     * Causes the bot to join an arena at initial login.  If the bot has logged
     * on and is already in an arena, use changeArena( int ) instead.  You can
     * also specify the resolution for the bot to enter in at.
     * <p>If arenaname contains only a number, then it's assumed to be a pub
     * that is being joined.
     * @param arenaName The arena string to join.
     * @param xResolution The X coordinate resolution for the screen.
     * @param yResolution The Y coordinate resolution for the screen.
     * @author Kirthi Sugnanam - FoN
     * @see #changeArena(String, short, short)
     */
    public void joinArena(String arenaName, short xResolution, short yResolution) throws Exception
    {

        if (Tools.isAllDigits(arenaName))
        {
            joinArena(Short.parseShort(arenaName), xResolution, yResolution);
        }
        else
        {
            try
            {
                m_arenaName = arenaName;
                m_packetGenerator.sendArenaLoginPacket((byte) 8, xResolution, yResolution, (short) 0xFFFD, arenaName);
                m_packetGenerator.sendSpectatePacket((short) - 1);
            }
            catch (Exception e)
            {
                throw new Exception("The resolution isnt an allowed specification: Specified X: " + xResolution + "Specified Y: " + yResolution);
            }
        }
    }

    /**
     * Causes the bot to join the specified public arena.  Should be used only
     * at initial login.
     * @param arena The arena number to join.
     * @author DoCk> (modified by FoN)
     * @see #changeArena(short)
     */
    public void joinArena(short arena)
    {
        m_arenaName = "(Public " + arena + ")";
        try
        {
            m_packetGenerator.sendArenaLoginPacket((byte) 8, (short) 1024, (short) 768, arena, "");
        }
        catch (Exception e)
        {
            //don't do anything cause hardcoded res is correct
        }
        m_packetGenerator.sendSpectatePacket((short) - 1);
    }

    /**
     * Causes the bot to join the specified public arena at a given resolution.
     * Should be used only at initial login.
     * @param arena The arena number to join.
     * @param xResolution The X coordinate resolution for the screen.
     * @param yResolution The Y coordinate resolution for the screen.
     * @author Kirthi Sugnanam - FoN
     * @see #changeArena(short, short, short)
     */
    public void joinArena(short arena, short xResolution, short yResolution) throws Exception
    {
        try
        {
            m_arenaName = "(Public " + arena + ")";
            m_packetGenerator.sendArenaLoginPacket((byte) 8, xResolution, yResolution, arena, "");
            m_packetGenerator.sendSpectatePacket((short) - 1);
        }
        catch (Exception e)
        {
            throw new Exception("The resolution isnt an allowed specification: Specified X: " + xResolution + "Specified Y: " + yResolution);
        }
    }

    /**
     * Joins the bot to a random public arena.
     * @author DoCk> (modified by FoN)
     */
    public void joinRandomPublicArena()
    {
        try
        {
            m_packetGenerator.sendArenaLoginPacket((byte) 8, (short) 1024, (short) 768, (short) 0xFFFF, "");
        }
        catch (Exception e)
        {
            //don't do anything because hardcoded res is correct
        }
        m_packetGenerator.sendSpectatePacket((short) - 1);
    }

    /**
     * This method tells the bot to leave the current arena, and join another
     * arena specified by a String.  If the String contains an integer value, the
     * bot will go to a public arena of that value.
     * @param newArenaName Name or number of the arena to change to.
     */
    public void changeArena(String newArenaName)
    {
        m_packetGenerator.sendArenaLeft();
        m_arenaTracker.clear();
        joinArena(newArenaName);
    }

    /**
     * This is an overloaded change function to allow variations in resolution.
     * @param newArenaName Name or number of the arena to change to.
     * @param xResolution The X Max for the screen size.
     * @param yResolution The Y Max for the screen size.
     * @exception Catches the resolution mistake for specifying a bad max X and Y
     * @author Kirthi Sugnanam - FoN
     */
    public void changeArena(String newArenaName, short xResolution, short yResolution) throws Exception
    {
        m_packetGenerator.sendArenaLeft();
        m_arenaTracker.clear();
        try
        {
            joinArena(newArenaName, xResolution, yResolution);
        }
        catch (Exception e)
        {
            throw new Exception("The resolution isnt an allowed specification: Specified X: " + xResolution + "Specified Y: " + yResolution);
        }
    }

    /**
     * Forces the bot to leave the current arena and join a public arena specified
     * by an integer.
     * @param arenaNumber The number of the public arena to change to.
     */
    public void changeArena(short arenaNumber)
    {
        m_packetGenerator.sendArenaLeft();
        m_arenaTracker.clear();
        joinArena(arenaNumber);
    }

    /**
     * Forces the bot to leave the current arena and join a public arena specified
     * by an integer.  Allows resolution to be specified.
     * @param arenaNumber The number of the public arena to change to.
     * @param xResolution The X Max for the screen size.
     * @param yResolution The Y Max for the screen size.
     * @exception catches the resolution mistake for specifying X max and Y
     * @author Kirthi Sugnanam - FoN
     */
    public void changeArena(short arenaNumber, short xResolution, short yResolution) throws Exception
    {
        m_packetGenerator.sendArenaLeft();
        m_arenaTracker.clear();
        try
        {
            joinArena(arenaNumber, xResolution, yResolution);
        }
        catch (Exception e)
        {
            throw new Exception ("The resolution isnt an allowed specification: Specified X: " + xResolution + "Specified Y: " + yResolution);
        }
    }

    /**
     * Moves the bot to the tile (x,y).  512, 512 is the center of the map.
     * For more complex operations, get a copy of the Ship object with getShip().
     * @param x X value to move the bot to
     * @param y Y value to move the bot to
     * @see #getShip()
     */
    public void moveToTile(int x, int y)
    {
        m_botSession.getShip().move(x * 16, y * 16);
    }

    /**
     * Moves the bot to position X, Y in exact map coordinates as opposed to
     * tile coordinates.  This means that the center of the arena is (8192, 8192)
     * as opposed to the less accurate (512, 512) of moveToTile( X, Y ).
     * For more complex operations, get a copy of the Ship object with getShip().
     * @param x Position of the bot along the X axis.
     * @param y Position of the bot along the Y axis.
     * @see #getShip()
     */
    public void move(int x, int y)
    {
        m_botSession.getShip().move(x, y);
    }

    /**
     * Moves the bot to position X, Y in exact map coordinates and applies the
     * x and y plane velocities as specified.  Center of the arena is (8192, 8192).
     * For more complex operations, get a copy of the Ship object with getShip().
     * @param x Position of the bot along the X axis.
     * @param y Position of the bot along the Y axis.
     * @param vx Velocity along the X axis (# pixels per 10 seconds).
     * @param vy Velocity along the Y axis (# pixels per 10 seconds).
     * @see #getShip()
     */
    public void move(int x, int y, int vx, int vy)
    {
        m_botSession.getShip().move(x, y, vx, vy);
    }

    /**
     * Sets the bot to spectate the specified player and surrounding area.
     * PlayerPosition events for any players in radar range will be created,
     * but any outside will not.  If you send -1, the bot will cease spectating
     * anyone, but will still receive position packets from the area.  You
     * may also call stopSpectatingPlayer() to do this.
     * @param playerID Ident of the player to spectate. (-1 to stop spectating)
     * @see #stopSpectatingPlayer()
     */
    public void spectatePlayer(int playerID)
    {
        m_packetGenerator.sendSpectatePacket((short) playerID);
    }

    /**
     * Sets the bot to spectate the specified player and surrounding area.
     * PlayerPosition events for any players in radar range will be created,
     * but any outside will not.
     * @param playerName Name of the player to spectate.
     */
    public void spectatePlayer(String playerName)
    {
        m_packetGenerator.sendSpectatePacket((short) m_arenaTracker.getPlayerID(playerName));
    }

    /**
     * Ceases spectating the player the bot is currently spectating on, if any.
     */
    public void stopSpectatingPlayer()
    {
        m_packetGenerator.sendSpectatePacket((short) -1);
    }

    /**
     * Sent a request for the bot to pick up flag specified by the given ID.
     * @param flagID ID of flag to pickup.
     */
    public void grabFlag(int flagID)
    {
        m_packetGenerator.sendFlagRequestPacket((short) flagID);
    }

    /**
     * Drops all flags the bot is carrying.
     */
    public void dropFlags()
    {
        m_packetGenerator.sendFlagDropPacket();
    }

    /**
     * Sets the bot's personal banner.
     * @param _banner A byte array containing the banner data (BMP format, no palette).
     */
    public void setBanner( byte[] _banner ) {

        m_packetGenerator.sendBannerPacket( _banner );
    }

    /**
     * Sends a death packet, causing the bot to die. (Fixed by D1st0rt 3-26-05)
     * If you send an invalid playerID, hypothetically nothing should happen.
     * @param playerID ID of the player who killed the bot.
     * @param bounty Bot's bounty at time of death.
     */
    public void sendDeath(int playerID, int bounty)
    {
        m_packetGenerator.sendPlayerDeath(playerID, bounty);
    }

    /**
     * Tells the bot to go parachuting (without the parachute).
     */
    public void die()
    {
        m_botSession.disconnect();
    }

    /**
     * Sets the minimum bounty needed for a player's kills/deaths to be sent
     * reliably to the bot.  If it's essential that you receive all player death
     * events / packets, it's advisable to set this to 1, or to use the more
     * explicit receiveAllPlayerDeaths() method.
     * @param minBounty Amount of bounty for kill messages to become reliable.
     * @see #receiveAllPlayerDeaths()
     */
    public void setReliableKills(int minBounty)
    {
        sendUnfilteredPublicMessage("*relkills " + minBounty);
    }

    /**
     * Sets the bot to receive all death events / packets, regardless of the
     * amount of bounty a player has.  Useful if you must account for every death.
     */
    public void receiveAllPlayerDeaths()
    {
        sendUnfilteredPublicMessage("*relkills 1");
    }

    /**
     * Sends out a request for an ArenaList packet to be sent back.  It can
     * then be received by handling the ArenaList event (if it has been requested
     * by using EventRequester).
     */
    public void requestArenaList()
    {
        sendUnfilteredPublicMessage("?arena");
    }

    /**
     * Sets up spam protection for the bot, allowing only the specified number of
     * messages to be sent to the bot before it begins to ignore them.
     * <p>This protection does not apply to HighMod+ (who should know what they
     * are doing!).
     * @param msgsPerMin Number of messages per minute the bot will allow.
     */
    public void setMessageLimit(int msgsPerMin)
    {
        m_botSession.getGamePacketInterpreter().setMessageLimiter(msgsPerMin, this);
    }

    /**
     * Adjusts the packet send delay.  The more packets that are sent out, the greater
     * the possibility you'll overflow the 2500 packet/minute limit.  The default
     * setting is 75 ms.  If your bot doesn't require a quick response, set it higher.
     * If your bot requires a near-real time response, set it lower.  The lower you set
     * it the more packets will be sent from the bot.
     * @param milliseconds Number of milliseconds between each packet.
     */
    public void setPacketSendDelay(int milliseconds)
    {
        m_botSession.getGamePacketGenerator().setSendDelay(milliseconds);
    }

    /**
     * Turns automatic player position updating on and off.  By default it is ON,
     * and set to change between the players it spectates for packets every 5000ms.
     * The lower the number, the more reliable any position information stored in
     * Player will be, and the more frequently and reliably will the bot receive
     * PlayerPosition events.  It is strongly advised that if you do not use
     * position packets that you set the time to 0 (completely off).
     * <p>Note that because TWCore is a client emulator (does not operate on the
     * server side but logs in as a bot) it only receives position packets from
     * the server about players within radar range.  This is why it must switch
     * who is being spectated in order to get as many packets as possible.  This
     * process may be CPU-intensive when done rapidly.
     * <p>If the arena the bot operates in is small, you may want to manually
     * adjust its position using the move() methods for efficiency.
     * @param milliseconds - specified time to update player positions at
     * 0     : turns tracking off and has the bot spectate its current area (change with move())
     * <200  : turns tracking on with 200 ms change rate
     * >=200 : turns tracking on with specified rate
     */
    public void setPlayerPositionUpdating( int milliseconds ) {
        m_arenaTracker.setPlayerPositionUpdateDelay( milliseconds );
    }

    /**
     * Starts the automatic player position updating system with the most reliable
     * positioning TWCore offers (switching between players every 200ms).  This
     * will cause the bot to switch which players it is spectating once every 5
     * seconds.  On slower systems and older network connections this may be
     * quite taxing.
     * <p>If you wish to switch faster than every 200ms, you must manually edit
     * Arena.java and hardcore a new value.
     */
    public void startReliablePositionUpdating() {
        m_arenaTracker.setPlayerPositionUpdateDelay( 200 );
    }

    /**
     * Stops the automatic player position updating system, causing the bot to
     * stop spectating the player it is currently spectating on, and cease
     * switching between players to update position packets.  Use this if your
     * bot will not be receiving position packets.
     */
    public void stopReliablePositionUpdating() {
        m_arenaTracker.setPlayerPositionUpdateDelay( 0 );
    }

    /**
     * Retreives a file from the server.  File arrives in a "FileArrived" packet /
     * event, and can be received by handling that event.
     * @param fileName Filename of the file requested.
     */
    public void getServerFile(String fileName)
    {
        sendUnfilteredPublicMessage("*getfile " + fileName);
    }

    /**
     * Sends a file to the server.  This could be used to back up logs, upload
     * alternate maps and configuration files.
     * @param fileName Name of the file to send.
     */
    public void putFile(String fileName)
    {
        sendUnfilteredPublicMessage("*putfile " + fileName);
    }


    // ***** LVZ OBJECT OPERATIONS *****
    /*
     * For use with LVZ objects.  If you're not familiar with the specification:
     *
     * http://www.kolumbus.fi/sakari.aura/contmapdevguide.html
     */

    /**
     * Issues an LVZ object on command, displaying it for all players.  The ID
     * of the object is configured from inside the LVZ.
     * <p>For more information on this format, please download the LVZToolkit from
     * your favorite Subspace download site (subspacedownloads.com ?), and see
     * http://www.kolumbus.fi/sakari.aura/contmapdevguide.html for a solid spec.
     * @param objID The ID of the object as denoted in the LVZ.
     */
    public void showObject(int objID)
    {
        sendUnfilteredPublicMessage("*objon " + objID);
    }

    /**
     * Issues an LVZ object off command, hiding it for all players.  The ID of
     * the object is configured from inside the LVZ.
     * <p>For more information on this format, please download the LVZToolkit from
     * your favorite Subspace download site (subspacedownloads.com ?), and see
     * http://www.kolumbus.fi/sakari.aura/contmapdevguide.html for a solid spec.
     * @param objID The ID of the object as denoted in the LVZ.
     */
    public void hideObject(int objID)
    {
        sendUnfilteredPublicMessage("*objoff " + objID);
    }

    /**
     * Shows the specified LVZ object for a specific player.
     * @param playerID ID of player.
     * @param objID ID of object.
     */
    public void showObjectForPlayer(int playerID, int objID)
    {
        sendUnfilteredPrivateMessage(playerID, "*objon " + objID);
    }

    /**
     * Hides the specified LVZ object for a specific player.
     * @param playerID ID of player.
     * @param objID ID of object.
     */
    public void hideObjectForPlayer(int playerID, int objID)
    {
        sendUnfilteredPrivateMessage(playerID, "*objoff " + objID);
    }

    /**
     * Manually sets multiple objects to either be shown or hidden using the
     * syntax of the *objset command.  objString should contain comma-separated
     * object IDs marked either with a + or - for show or hide.  For example:
     * <p>  "+2,+5,-7,+12,-14,+15"
     * @param objString Comma-separated list of object IDs marked with + for show or - for hide.
     */
    public void manuallySetObjects( String objString ) {
        sendUnfilteredPublicMessage("*objset " + objString );
    }

    /**
     * Manually sets multiple objects to either be shown or hidden using the
     * syntax of the *objset command for a single player.  objString should
     * contain comma-separated object IDs marked either with a + or - for show
     * or hide.  For example:
     * <p>  "+2,+5,-7,+12,-14,+15"
     * @param playerID ID of the player to send to.
     * @param objString Comma-separated list of object IDs marked with + for show or - for hide.
     */
    public void manuallySetObjects( String objString, int playerID ) {
        sendUnfilteredPrivateMessage( playerID, "*objset " + objString );
    }

    /**
     * Sets objects using the current objects set under BotAction's copy of Objset.
     * In order for this command to work, first get the Objset with getObjectSet(),
     * add the objects you would like set all at once, and then run the command.
     */
    public void setObjects() {
        if( m_objectSet.toSet() ) {
            sendUnfilteredPublicMessage( "*objset" + m_objectSet.getObjects() );
        }
    }

    /**
     * Sets objects using the current objects set under BotAction's copy of Objset
     * for a specific player, as specified by ID.  In order for this command to
     * work, first get the Objset with getObjectSet(), add the objects you would
     * like set all at once, and then run the command.
     * @param playerId ID of the player to send to.
     */
    public void setObjects( int playerId ) {
        if( m_objectSet.toSet( playerId ) ) {
            sendUnfilteredPrivateMessage( playerId, "*objset" + m_objectSet.getObjects( playerId ) );
        }
    }


    // ***** SQL DATABASE OPERATIONS *****
    /*
     * SQL in TWCore is easy [and fun :P]!  If you set up a connection to a
     * SQL server inside sql.cfg, in order to run a query, just supply the
     * name of the connection and the SQL query you'd like to run.  Then the
     * results will be returned to you in a ResultSet.  Search for ResultSet
     * inside various bots to see examples of usage -- it's very simple to do,
     * and adds a tremendous amount of functionality to a bot.
     */

    /**
     * Runs a direct SQL Query.  This method does block up the bot from doing anything
     * else, so be careful while using it.  Queries should be quick.  Only use it for
     * queries where the performance of the bot depends primarily upon the value
     * returned by this query.
     * @param connectName The connection name as specified in sql.cfg
     * @param query The SQL query to be executed
     * @throws SQLException SQLException
     * @return ResultSet from the SQL Query.
     */
    public ResultSet SQLQuery(String connectName, String query) throws SQLException
    {
        return getCoreData().getSQLManager().query(connectName, query);
    }

    /**
     * Runs a regular backround SQL Query, which is placed in a queue and waits
     * behind any other background queries ahead of it.  Background queries are
     * generally used when getting the data isn't time critical, or when a bot's
     * program thread should not be blocked (for example if the query is large).
     * <p>Background queries are returned to the bot by handling an SQLResultEvent
     * and checking for a specific identifier to determine if it's the right query.
     * @param connectName The connection name as specified in sql.cfg
     * @param identifier A unique identifier that describes what the query is.
     * This identifier will be found in the SQLResultEvent when it is returned.
     * The ID allows you to handle different sorts of background queries differently
     * when they come out the other end.  If the identifier is null, the result
     * of the query will not be delivered.
     * @param query The SQL query to be executed.
     */
    public void SQLBackgroundQuery(String connectName, String identifier, String query)
    {
        getCoreData().getSQLManager().queryBackground(connectName, identifier, query, m_botSession.getSubspaceBot());
    }

    /**
     * Runs a high priority background query.  This query will be executed before all
     * other background queries.  This is useful for when you know the queue will be
     * rather large, and you need a query done very quickly, but still wish to run it
     * in the background without blocking the bot's thread.
     * @param connectName The connection name as specified in sql.cfg
     * @param identifier A unique identifier that describes what the query is.
     * This identifier will be found in the SQLResultEvent when it is returned.
     * The ID allows you to handle different sorts of background queries differently
     * when they come out the other end.  If the identifier is null, the result
     * of the query will not be delivered.
     * @param query The SQL query to be executed
     */
    public void SQLHighPriorityBackgroundQuery(String connectName, String identifier, String query)
    {
        getCoreData().getSQLManager().queryBackgroundHighPriority(connectName, identifier, query, m_botSession.getSubspaceBot());
    }

    /**
     * Runs an insert query into the specified table, given a set of fields and
     * values that correspond to those fields.  This is a helper method that
     * forms the query for you.  Experienced users of SQL may wish to form the
     * query themselves using the SQLQuery() method.
     * @param connectName The connection name as specified in sql.cfg
     * @param tableName The name of the table you wish to insert the values into.
     * @param fields The field names you want to enter data into.
     * @param values The corresponding values for the field names.
     * @see #SQLQuery(String, String)
     */
    public void SQLInsertInto(String connectName, String tableName, String[] fields, String[] values)
    {
        if (fields.length != values.length)
        {
            Tools.printLog("SQLInsertInfo error: mismatch in the number of " + "fields/values");
            return;
        }
        StringBuffer beginning = new StringBuffer();
        beginning.append("INSERT INTO " + tableName + "(");
        StringBuffer end = new StringBuffer();
        end.append(")VALUES(");
        for (int i = 0; i < fields.length; i++)
        {
            beginning.append(fields[i]);
            end.append("\"" + values[i] + "\"");
            if (i < fields.length - 1)
            {
                beginning.append(",");
                end.append(",");
            }
        }

        String query = beginning.toString() + end.toString() + ")";
        try
        {
            SQLQuery(connectName, query);
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Runs an insert query into the specified table, given a set of fields and
     * values that correspond to those fields.  This is a helper method that
     * forms the query for you.  Experienced users of SQL may wish to form the
     * query themselves using the SQLBackgroundQuery() method.
     * @param connectName The connection name as specified in sql.cfg.
     * @param tableName The name of the table you wish to insert the values into.
     * @param fields The field names you want to enter data into.
     * @param values The corresponding values for the field names.
     * @see #SQLBackgroundQuery(String, String, String)
     */
    public void SQLBackgroundInsertInto(String connectName, String tableName, String[] fields, String[] values)
    {
        if (fields.length != values.length)
        {
            Tools.printLog("SQLInsertInfo error: mismatch in the number of " + "fields/values");
            return;
        }
        StringBuffer beginning = new StringBuffer();
        beginning.append("INSERT INTO " + tableName + "(");
        StringBuffer end = new StringBuffer();
        end.append(")VALUES(");
        for (int i = 0; i < fields.length; i++)
        {
            beginning.append(fields[i]);
            end.append("\"" + values[i] + "\"");
            if (i < fields.length - 1)
            {
                beginning.append(",");
                end.append(",");
            }
        }

        String query = beginning.toString() + end.toString() + ")";
        try
        {
            SQLBackgroundQuery(connectName, null, query);
        }
        catch (Exception e)
        {
        }
    }


    // ***** INTER-PROCESS COMMUNICATION OPERATIONS *****
    /*
     * IPC messages are a very simple and easy way to send information from one
     * bot to another.  This can be used to coordinate activities between them.
     * To use Inter-Process Communications:
     *
     *  - All bots that will communicate with one another need to be subscribed
     *    to a particular channel (something like tuning into the same frequency
     *    on a radio).  The channel is identified by a unique String.  Use
     *    the ipcSubscribe(String) to do this.
     *  - Any bot that wishes to receive messages needs to request and handle
     *    InterProcessEvent in their code.  From there the bot can verify the
     *    sender, receiver, message itself, and handle how it wishes.
     *  - Send messages using ipcSendMessage, or the more generic ipcTransmit.
     */

    /**
     * Constructs a basic IPC message across a given channel.  This is the simplest
     * way to transmit a message.  To transmit a more generic object, use the
     * ipcTransmit method.  Note that in this method, sender and receiver may be null.
     * @param channelName Name of the channel to broadcast the message over.
     * @param message Message to send.
     * @param recipient Unique name of a specific receiver, if any.  Null for all on channel.
     * @param sender Unique name of the 'sender' -- usually name of the bot.  May be null.
     * @see #ipcTransmit(String, Object)
     */
    public void ipcSendMessage( String channelName, String message, String recipient, String sender ) {
        IPCMessage msg = new IPCMessage( message, recipient, sender );
        getCoreData().getInterProcessCommunicator().broadcast(channelName, m_botSession.getBotName(), m_botSession.getSubspaceBot(), msg);
    }

    /**
     * Transmits a generic object (usually an IPCMessage) to the specified channel
     * using inter-process communication.  The object will arrive as an InterProcessEvent,
     * which must be requested and handled by any bot that will receive the message.
     * If a bot is not yet subscribed to the channel, the bot will automatically be
     * subscribed.
     * @param channelName Channel name to send the object to.
     * @param o Object to be transmitted.
     * @see #ipcSendMessage(String, String, String, String)
     */
    public void ipcTransmit(String channelName, Object o)
    {
        getCoreData().getInterProcessCommunicator().broadcast(channelName, m_botSession.getBotName(), m_botSession.getSubspaceBot(), o);
    }

    /**
     * Subscribes the current bot to the specified IPC Channel.  Do not attempt to
     * use this method in the constructor of a bot.  Use it during or after handling
     * the LoggedOn event, or at a later time.
     * @param channelName Name of the IPC channel you wish to subscribe to.
     */
    public void ipcSubscribe(String channelName)
    {
        getCoreData().getInterProcessCommunicator().subscribe(channelName, m_botSession.getSubspaceBot());
    }

    /**
     * Unsubscribes this bot from the provided channelName.  If the bot is the last
     * one on the channel, the channel is destroyed.
     * @param channelName Name of the IPC channel you wish to unsubscribe from.
     */
    public void ipcUnSubscribe(String channelName)
    {
        getCoreData().getInterProcessCommunicator().unSubscribe(channelName, m_botSession.getSubspaceBot());
    }

    /**
     * Explicitly destroys a channel, regardless of if any bots are subscribed.
     * @param channelName The name of the channel to be destroyed.
     */
    public void ipcDestroyChannel(String channelName)
    {
        getCoreData().getInterProcessCommunicator().destroy(channelName);
    }




    // **********************************************************************************
    //
    //                                     GETTERS
    //
    // **********************************************************************************
    /*
     * The following methods are labelled getters because their main intent is to
     * return data.  They are organized into the following categories:
     *
     *  - SIMPLE GETTERS: Get bot name, ID #, current arena name, number of players
     *    in current arena, server address, server port, current state, & SQL status.
     *  - SIMPLE CLASS GETTERS: Gets for the following classes - BotAction, CoreData,
     *    OperatorList, EventRequester, BotSettings (for the general setup.cfg and
     *    for specific bots), Ship, InterProcessCommunicator, and Objset.
     *  - PLAYER/FLAG GETTERS: Get player by ID or name, get player ID, get player by
     *    fuzzy (incomplete) name, get player name by fuzzy name, get flag by flag ID.
     *  - COMPLEX GETTERS: Get number of players in the arena playing, if a frequency
     *    has a particular ship currently on it, total combined scores of all players
     *    on a freq, File corresponding to setup.cfg, File to file in /data directory,
     *    File to TWCore root directory, and File to file in TWCore root directory.
     *  - ITERATORS: Iterators over - all players currently in a ship in the arena,
     *    all players on a frequency, all players (spec'd and playing), all players
     *    by ID rather than Player objects, and all flags by ID.
     */


    // ***** SIMPLE GETTERS *****

    /**
     * @return The login name of the bot as displayed to players.
     */
    public String getBotName() {
        return m_botSession.getBotName();
    }

    /**
     * @return m_botNumber Bot's TWCore ID number, generally only used internally.
     */
    public int getBotNumber() {
        return m_botNumber;
    }

    /**
     * @return Name of the arena the bot is currently in or travelling to.
     */
    public String getArenaName() {
        if (m_arenaName != null)
            return m_arenaName;
        else
            return "Unknown";
    }

    /**
     * @return The number of players currently in the arena (in-game + spectating).
     */
    public int getArenaSize() {
        return m_arenaTracker.size();
    }

    /**
     * @return The host name of the server the bot is connected to.
     */
    public String getServerName() {
        return getCoreData().getServerName();
    }

    /**
     * @return The port number on the server that the bot is connected to.
     */
    public int getServerPort() {
        return getCoreData().getServerPort();
    }

    /**
     * Gets the state of the bot.  This is only important from an internal
     * perspective, and unless you plan to add new bot states, can be ignored.
     * @return An integer representing the state of the bot as reflected in Session.
     */
    public int getBotState() {
        return m_botSession.getBotState();
    }

    /**
     * True if the SQL connection pools were initialized properly, a general
     * indicator that the SQL system appears to be running properly.  The
     * reliability of this method is somewhat questionable.
     * @return True if the SQL connection pools were initialized properly.
     */
    public boolean SQLisOperational() {
        return getCoreData().getSQLManager().isOperational();
    }


    // ***** SIMPLE CLASS GETTERS *****

    /**
     * Return the correct BotAction for the running Thread.  Useful for subclasses
     * of bots, so you don't need to pass BotAction down the entire hierarchy.
     * @return The BotAction object of the currently running Thread / bot.
     */
    static public BotAction getBotAction() {
        return ((Session)Thread.currentThread()).getBotAction();
    }

    /**
     * @return A reference to the CoreData storage class for the bot core.
     */
    public CoreData getCoreData()
    {
        return m_botSession.getCoreData();
    }

    /**
     * Gets the OperatorList object shared between bots that is used to determine
     * access levels.
     * @return An instance containing methods and information related to access control.
     */
    public OperatorList getOperatorList() {
        return getCoreData().getOperatorList();
    }

    /**
     * Gets the EventRequester object.  This object controls what events are being sent
     * to your bot.  EventRequester can turn on or off requested events at any time.
     * Use its requestEvent(int) method to request an event.  See source for more info.
     * @return The EventRequester object for this bot.
     */
    public EventRequester getEventRequester()
    {
        return m_botSession.getEventRequester();
    }

    /**
     * Gets a copy of the general settings object, which stores information found
     * in setup.cfg.
     * @return An instance of a class that provides the data contained in setup.cfg
     */
    public BotSettings getGeneralSettings() {
        return getCoreData().getGeneralSettings();
    }

    /**
     * Gets a BotSettings object for this bot (from botname.cfg, where botname
     * is the main class name of the bot).
     * @return A BotSettings object containing data from the bot's .cfg
     */
    public BotSettings getBotSettings() {
        String botName = m_botSession.getBotClass().getName();
        if (botName.indexOf(".") != -1 ) {
            botName = botName.substring(botName.lastIndexOf(".") + 1);
        }
        return m_botSession.getCoreData().getBotConfig(botName);
    }

    /**
     * Gets the Ship object for this bot, which allows you to control the bot
     * as an in-game ship, including movement, firing, attaching, etc.
     * @return This bot's Ship object, which controls the in-game flight of the bot.
     */
    public Ship getShip() {
        return m_botSession.getShip();
    }

    /**
     * Gets a copy of the InterProcessCommunicator for use with sending messages
     * between bots.  See the source for BotAction, under MISC OPERATIONS ->
     * INTER-PROCESS COMMUNICATOR OPERATIONS, for a guide on using IPC.
     * @return The main class of IPC messaging, the InterProcessCommunicator.
     * @see #ipcSendMessage(String, String, String, String)
     * @see #ipcSubscribe(String)
     */
    public InterProcessCommunicator getIPC() {
        return getCoreData().getInterProcessCommunicator();
    }

    /**
     * Returns the Objset associated with this bot.  Objset can be used to
     * maintain a list of LVZ objects to shown or hide without the hassle
     * of handling them manually.
     */
    public Objset getObjectSet() {
        return m_objectSet;
    }



    // ***** PLAYER/FLAG GETTERS *****
    /*
     * One word of warning: when looking up Players, be sure to check them for
     * null before using them.  Even if an event passes you a name or ID, it's
     * not a 100% chance you can use that information to find the player, and
     * an attempt to do so can from time to time return a null value.
     */

    /**
     * Gets the Player object associated with the PlayerID provided.  The Player
     * object describes all the pertinent details about a player in the arena the
     * bot is in.  PlayerID is not the same as ?userid or a network adapter's MAC
     * address, and is assigned arena-wide, not zone-wide.  As such it is not
     * considered an extremely reliable way of tracking players.
     * <p>
     * <b>!!NOTE!!</b>  It's important to check the returned Player object for a null
     * value or catch the resulting NullPointerException if you make reference to the
     * object without checking for null.  This is the single most common error
     * made by new TWCore botmakers.  Don't trust that the playerID provided from
     * an event packet will correspond to an existing player.  It's possible that
     * the event will fire simultaneously as the player leaves, resulting in a null
     * value for the Player object.
     *
     * @param playerID The PlayerID of the player you wish to retrieve info for.
     * @return If a matching ID is found, returns that Player object; otherwise, ** NULL **.
     */
    public Player getPlayer(int playerID)
    {
        return m_arenaTracker.getPlayer(playerID);
    }

    /**
     * Gets the Player object associated with the PlayerID provided.  The Player
     * object describes all the pertinent details about a player in the arena the
     * bot is in.  PlayerID is not the same as ?userid or a network adapter's MAC
     * address, and is assigned arena-wide, not zone-wide.  As such it is not
     * considered an extremely reliable way of tracking players.
     * <p>
     * <b>!!NOTE!!</b>  It's important to check the returned Player object for a null
     * value or catch the resulting NullPointerException if you make reference to the
     * object without checking for null.  This is the single most common error
     * made by new TWCore botmakers.  Don't trust that the playerID provided from
     * an event packet will correspond to an existing player.  It's possible that
     * the event will fire simultaneously as the player leaves, resulting in a null
     * value for the Player object.
     *
     * @param playerName The name of the player you wish to retreive info for.
     * @return If a matching name is found, returns that Player object; otherwise, ** NULL **.
     */
    public Player getPlayer(String playerName)
    {
        return m_arenaTracker.getPlayer(playerName);
    }

    /**
     * Looks up a player's name using a given player ID.  If the name can't be
     * found based on that ID, null may be returned.
     * @param playerID The PlayerID to look up.
     * @return The player's name.  ** MAY BE NULL **
     */
    public String getPlayerName(int playerID)
    {
        return m_arenaTracker.getPlayerName(playerID);
    }

    /**
     * Looks up a player's name using a playerID.  If the player is not in the
     * arena, they will not have an ID associated with their name, and -1 will
     * be returned.
     * @param playerName The name of the player.
     * @return If found, the ID of the player; -1 if not found.
     */
    public int getPlayerID(String playerName)
    {
        return m_arenaTracker.getPlayerID(playerName);
    }

    /**
     * Gets the Player whose name most accurately matches the supplied search
     * name.  If the name matches exactly, this is returned; else, all names
     * are checked to see if they begin with the search term, and of those that
     * do, the one that comes latest in the dictionary is returned.  For
     * example, if the search name is "Oli", and both the players "Oliver" and
     * "Oliver Claushauf" are in the arena, the latter will be returned.  If no
     * name in the arena begins with the provided name, null is returned.
     * @param playerName The partial name of a player.
     * @return A Player with name matching or starting with playerName; null if no match.
     */
    public Player getFuzzyPlayer(String playerName)
    {
        String fuzzyResult = getFuzzyPlayerName(playerName);
        if (fuzzyResult != null)
            return getPlayer(fuzzyResult);
         else
            return null;
    }

    /**
     * Gets the name of the player that most accurately matches the supplied
     * search name.  If the name matches exactly, this is returned; else, all
     * names are checked to see if they begin with the search term, and of those
     * that do, the one that comes latest in the dictionary is returned.  For
     * example, if the search name is "Oli", and both the players "Oliver" and
     * "Oliver Claushauf" are in the arena, the latter will be returned.  If no
     * name in the arena begins with the provided name, null is returned.
     * @param playerName The partial name of a player.
     * @return Name of the player matching or starting with playerName; null if no match.
     */
    public String getFuzzyPlayerName(String playerName)
    {
        Map m_playerMap = m_arenaTracker.getPlayerMap();
        Iterator i = m_playerMap.values().iterator();
        String answ, best = null;
        synchronized(m_playerMap) {
            while (i.hasNext())
            {
                answ = ((Player) i.next()).getPlayerName();
                if (answ.toLowerCase().startsWith(playerName.toLowerCase()))
                    if (best == null)
                        best = answ;
                    else if (best.toLowerCase().compareTo(answ.toLowerCase()) > 0)
                        best = answ;

                if (answ.equalsIgnoreCase(playerName))
                    return answ;
            }
         }

        return best;
    }

    /**
     * Gets the Flag object associated with the FlagID provided.  The Flag object
     * describes all the pertinent details about a flag in the arena the bot is in.
     * @param flagID The FlagID of the player you wish to retrieve info for.
     * @return The corresponding Flag object.
     */
    public Flag getFlag(int flagID)
    {
        return m_arenaTracker.getFlag(flagID);
    }


    // ***** COMPLEX GETTERS *****

    /**
     * Gets the total number of players currently playing in the arena (does
     * NOT include those who are spectating).
     * @return Number of arena players currently in a ship.
     */
    public int getNumPlayers() {
        int numPlayers = 0;
        Iterator i = m_arenaTracker.getPlayingPlayerIterator();

        while ( i.hasNext() ) {
            numPlayers++;
            i.next();
        }
        return numPlayers;
    }

    /**
     * Returns true if a freq in the arena contains a specific type of ship.
     * @param freq Frequency to check for the ship.
     * @param ship Ship type to look for.
     * @return True if the ship does exist in the freq.
     */
    public boolean freqContainsShip(int freq, int ship)
    {
        Iterator i = m_arenaTracker.getPlayerIterator();
        while (i.hasNext())
        {
            Player p = (Player) i.next();
            if (p.getShipType() == ship && p.getFrequency() == freq)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the combined total score for all players on a particular frequency.
     * @param freq Frequency of the players to get the score for.
     * @return The total combined score for the players on the specified frequency.
     */
    public int getScoreForFreq(int freq)
    {
        int result = 0;
        Iterator i = m_arenaTracker.getPlayerIterator(); //if( i == null ) return 0;
        while (i.hasNext())
        {
            Player p = (Player) i.next();
            if (p.isPlaying() && p.getFrequency() == freq)
                result += p.getScore();
        }
        return result;
    }

    /**
     * Returns the core CFG (setup.cfg) as a File.
     * @param filename Temporary filename to use for this File.
     * @return A File containing the core configuration (setup.cfg).
     */
    public File getCoreCfg(String filename)
    {
        String location = getCoreData().getGeneralSettings().getString("Core Location");
        return new File(location + File.separatorChar + "corecfg", filename);
    }

    /**
     * Gets a file from the /data directory, given the filename as a String.
     * The filename can also be a path relative to the data directory.
     * @param filename Filename or pathname to the file in question.
     * @return File object for the specified file.
     */
    public File getDataFile(String filename)
    {
        String location = getCoreData().getGeneralSettings().getString("Core Location");
        return new File(location + File.separatorChar + "data", filename);
    }

    /**
     * Gets the root directory of TWCore, returning as a File object.  This
     * directory is specified in setup.cfg.
     * @return File object representing the directory the core is in.
     */
    public File getCoreDirectory()
    {
        return new File(getGeneralSettings().getString("Core Location"));
    }

    /**
     * Gets a File in the root directory of TWCore.  May be a pathname relative
     * to this directory as well.
     * @param filename Filename or pathname of the file or directory.
     * @return File representation of the object.
     */
    public File getCoreDirectoryFile(String filename)
    {
        return new File(getGeneralSettings().getString("Core Location") + File.separatorChar + filename);
    }


    // ***** ITERATORS *****
    /*
     * Iterators return the entire contents of records stored in Arena, and are
     * very useful for managing players in the arena in ways not already covered
     * by BotAction.  See getPlayingPlayerIterator() for an example of usage.
     */

    /**
     * Returns an iterator of all non-specced Players in the arena.  Example usage:
     * <code><pre>
     * Iterator i = m_botAction.getPlayingPlayerIterator();
     * while( i.hasNext() ){
     *    Player p = (Player)i.next();
     *    if( p.getPlayerName().equals( "DoCk>" )){
     *        m_botAction.sendPrivateMessage( p.getPlayerID(), "Hi DoCk>!" );
     *    } else if( p.getFrequency() == 223 && p.getSquadName().equals( "LAME" )){
     *        m_botAction.sendPrivateMessage( p.getPlayerID(), "L!" );
     *    }
     * }
     * </pre></code>
     * @return An Iterator of all Players who are currently playing in the arena.
     */
    public Iterator getPlayingPlayerIterator()
    {
        return m_arenaTracker.getPlayingPlayerIterator();
    }

    /**
     * @param freq Frequency to fetch
     * @return An iterator of all players on a frequency.  (Should now be working)
     */
    public Iterator getFreqPlayerIterator(int freq)
    {
        return m_arenaTracker.getFreqPlayerIterator(freq);
    }

    /**
     * @return An Iterator of all Players in the arena, both spec'd and playing.
     */
    public Iterator getPlayerIterator()
    {
        return m_arenaTracker.getPlayerIterator();
    }

    /**
     * @return An Iterator of the IDs of all players in the arena, both spec'd and playing.
     */
    public Iterator getPlayerIDIterator()
    {
        return m_arenaTracker.getPlayerIDIterator();
    }

    /**
     * @return An Iterator of all Flags in the arena.
     */
    public Iterator getFlagIDIterator()
    {
        return m_arenaTracker.getFlagIDIterator();
    }
}