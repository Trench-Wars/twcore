package twcore.core;

import java.util.*;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
/** Defines all the actions your bot may do in the arena.
 * Combinations of these actions define more complex actions.
 */
public class BotAction
{
    private Timer m_timer;
    private String m_arenaName;
    private Session m_botSession;
    private LinkedList m_timerTasks;
    private List m_messageQueue;
    private Arena m_arenaTracker;
    private GamePacketGenerator m_packetGenerator;
    private Objset m_objectSet;

    /** Constructor for BotAction.  Don't worry about this, the object has already
     * been constructed for you.
     * @param botSession Session object describing the session
     * @param packetGenerator Packet generator which is called when packets are sent out.
     * @param arena Arena Tracker object used to represent an arena full of players.
     */
    public BotAction(GamePacketGenerator packetGenerator, Arena arena, Timer timer, Session botSession)
    {
        m_timer = timer;
        m_arenaTracker = arena;
        m_botSession = botSession;
        m_timerTasks = new LinkedList();
        m_packetGenerator = packetGenerator;
        m_objectSet = new Objset();
    }


        /** Return the correct BotAction for the running Thread. Useful for subclasses of bots, so you don't need
         * to pass BotAction down the entire hierarchy.
         * @return BotAction the botAction object of the currently running Thread(Bot)
         */
        static public BotAction getBotAction() {
            return ((Session)Thread.currentThread()).getBotAction();
        }


    /** Schedules a TimerTask to occur once at a future time.  TimerTask is part of
     * the package java.util.  The only method that a TimerTask must override is
     * public void run().
     * @param task TimerTask to be executed.
     * @param delayms Length of time before execution, in milliseconds
     */
    public void scheduleTask(TimerTask task, long delayms)
    {

        m_timerTasks.add(task);
        m_timer.schedule(task, delayms);
    }

    /** Schedules a TimerTask to occur repeatedly at an interval.  TimerTask is part of
     * the package java.util.  The only method that a TimerTask must override is
     * public void run().
     * @param task TimerTask to be executed.
     * @param delayms Length of time before execution, in milliseconds
     * @param periodms Delay between executions after the initial execution, in milliseconds.
     */
    public void scheduleTaskAtFixedRate(TimerTask task, long delayms, long periodms)
    {
        m_timerTasks.add(task);
        m_timer.scheduleAtFixedRate(task, delayms, periodms);
    }

    /** Cancels all pending TimerTasks.  You can cancel an individual TimerTask by using
     * task.cancel().
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

    /** Sends a private message without any filtration.  Use this ONLY in situations
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

    /** Sends a private message without any filtration.  Use this ONLY in situations
     * where you are hard coding in commands to be sent.  For the sake of security, use
     * the filtered methods for *everything* else.
     * @param playerID Player ID of the player you wish to send the message to.
     * @param message Message to be sent.
     */
    public void sendUnfilteredPrivateMessage(int playerID, String message)
    {
        sendUnfilteredPrivateMessage(playerID, message, (byte) 0);
    }

    /** Sends a private message without any filtration.  Use this ONLY in situations
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

    /** Sends a private message without any filtration.  Use this ONLY in situations
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

    /** Sends a private message without any filtration.  Use this ONLY in situations
     * where you are hard coding in commands to be sent.  For the sake of security, use
     * the filtered methods for *everything* else.
     * @param message Message to be sent.
     */
    public void sendUnfilteredPublicMessage(String message)
    {
        sendUnfilteredPublicMessage(message, (byte) 0);
    }

    /** Sends a private message without any filtration.  Use this ONLY in situations
     * where you are hard coding in commands to be sent.  For the sake of security, use
     * the filtered methods for *everything* else.
     * @param message Message to be sent.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendUnfilteredPublicMessage(String message, int soundCode)
    {
        m_packetGenerator.sendChatPacket((byte) 2, (byte) soundCode, (short) 0, message);
    }

    /** Sends a zone wide advertisement.  Do not use unless absolutely necessary.
     * @param message The message to be sent
     */
    public void sendZoneMessage(String message)
    {
        sendZoneMessage(message, (byte) 0);
    }

    /** Sends a zone wide advertisement.  Do not use unless absolutely necessary.
     * Includes a sound code.
     * @param message The away message to be sent
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendZoneMessage(String message, int soundCode)
    {
        sendUnfilteredPublicMessage("*zone " + message, soundCode);
    }

    /** Retrieves the name of the server the bot is connected to.
     * @return The host name of the server the bot is connected to.
     */
    public String getServerName()
    {
        return getCoreData().getServerName();
    }

    /** Retrieves the port of the server the bot is connected to.
     * @return The port number on the server that the bot is connected to.
     */
    public int getServerPort()
    {
        return getCoreData().getServerPort();
    }

    /** Gets an OperatorList.  Operator list is our means of access control.  Please see
     * the class OperatorList for more information.  This one instance is shared
     * between every bot running on the system.
     * @return An instance containing methods and information related to access control.
     */
    public OperatorList getOperatorList()
    {
        return getCoreData().getOperatorList();
    }

    /** Retreives the information from setup.cfg
     * @return An instance of a class that provides the data contained in setup.cfg
     */
    public BotSettings getGeneralSettings()
    {
        return getCoreData().getGeneralSettings();
    }

    /** Gets the state of the bot.  This is important only internally.  This can safely
     * be ignored by most people.
     * @return An integer representing the state of the bot as reflected in Session
     */
    public int getBotState()
    {
        return m_botSession.getBotState();
    }

    /**Sends a "?help" alert command.
     * @param message The message to be sent along with the alert
     */
    public void sendHelpMessage(String message)
    {
        sendUnfilteredPublicMessage("?help " + message);
    }

    /**Sends a "?cheater" alert command.
     * @param message The message to be sent along with the alert
     */
    public void sendCheaterMessage(String message)
    {
        sendUnfilteredPublicMessage("?cheater " + message);
    }

    /**Sends a alert command as specified.
     * @param type The type of alert command to be sent
     * @param message The message to be sent along with the alert
     */
    public void sendAlertMessage(String type, String message)
    {
        sendUnfilteredPublicMessage("?" + type + " " + message);
    }

    /** Displays a green arena message to the arena the bot is in.
     * @param message The message to be displayed.
     */
    public void sendArenaMessage(String message)
    {
        sendArenaMessage(message, (byte) 0);
    }

    /** Displays a green arena message to the arena the bot is in, with a sound code.
     * @param message The message to be displayed.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendArenaMessage(String message, int soundCode)
    {
        sendUnfilteredPublicMessage("*arena " + message, soundCode);
    }

    /** Sends a normal (blue) message to the public chat.
     * @param message The message to be displayed.
     */
    public void sendPublicMessage(String message)
    {

        sendPublicMessage(message, 0);
    }

    /** Sends a normal (blue) message to the public chat with a sound code.
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

    /** Send a message to your teammates.  If the bot is in spectator mode, it will
     * speak with the players in spectator mode.
     * @param message The message to be displayed.
     */
    public void sendTeamMessage(String message)
    {
        sendTeamMessage(message, (byte) 0);
    }

    /** Send a message to your teammates.  If the bot is in spectator mode, it will
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

    /** Send a message to a whole frequency of players.
     * @param frequency The frequency this message is to be sent to.
     * @param message The message to be displayed.
     */
    public void sendOpposingTeamMessageByFrequency(int frequency, String message)
    {

        sendOpposingTeamMessage(frequency, message, (byte) 0);
    }

    /** Send a message to a whole frequency of players.
     * @param frequency The frequency this message is to be sent to.
     * @param message The message to be sent
     * @param soundCode Sound code to be sent along with the message.
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
                i = m_arenaTracker.getFreqPlayerIterator(frequency);
                if (i != null)
                {
                    playerID = ((Integer) i.next()).intValue();
                    m_packetGenerator.sendChatPacket((byte) 4, (byte) soundCode, (short) playerID, message);
                }
            }
        }
    }

    /** Send a message to a whole frequency of players.
     * @param playerID The id of the player whose frequency this message is to be sent to.
     * @param message The message to be sent
     * @param soundCode Sound code to be sent along with the message.
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

    /** Send a message to a whole frequency of players.
     * @param playerName The name of the player whose frequency this message is to be sent to.
     * @param message The message to be sent
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendOpposingTeamMessage( String playerName, String message, int soundCode ){
        int         playerID = m_arenaTracker.getPlayerID( playerName );
        
        sendOpposingTeamMessage( playerID, message, soundCode );
    }

    /** Retreives a file from the server.  File arrives in a "FileArrived" packet.
     * @param fileName Filename of the file requested.
     */
    public void getServerFile(String fileName)
    {
        sendUnfilteredPublicMessage("*getfile " + fileName);
    }

    /** Issues the command *putfile.  Sphonk, please fill in what one might do with
     * this.
     * @param fileName Name of the file to send.
     */
    public void putFile(String fileName)
    {
        sendUnfilteredPublicMessage("*putfile " + fileName);
    }

    /** Sends a chat message to the first chat.
     * @param message The message to be displayed.
     */
    public void sendChatMessage(String message)
    {
        sendChatMessage(1, message, (byte) 0);
    }

    /** Sends a chat message to a specific chat number.
     * @param chatNumber Number of the chat to send information to.
     * @param message The message to be displayed.
     */
    public void sendChatMessage(int chatNumber, String message)
    {
        sendChatMessage(chatNumber, message, (byte) 0);
    }

    /** Sends a chat message to a specific chat number, with a sound code.
     * @param chatNumber Number of the chat to send information to.
     * @param message The message to be displayed.
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendChatMessage(int chatNumber, String message, int soundCode)
    {
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

    /** Sends a squad message to a specific squad.
     * @param squadName Name of the squad
     * @param message Message to send to that squad
     */
    public void sendSquadMessage(String squadName, String message)
    {
        sendSquadMessage(squadName, message, 0);
    };

    /** Sends a squad message to a specific squad with a sound code.
     * @param squadName Name of the squad
     * @param message Message to send to that squad
     * @param soundCode Sound code to be sent along with the message.
     */
    public void sendSquadMessage(String squadName, String message, int soundCode)
    {
        m_packetGenerator.sendChatPacket((byte) 7, (byte) soundCode, (short) 0, ":#" + squadName + ":" + message);
    };

    /** Sends a private message to someone in the same arena.  Do not attempt to use
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

    /** Sends a private message to someone in the same arena.  Do not attempt to use
     * this method if you are not absolutely certain that the player is in the arena.
     * If you are at all unsure, use sendSmartPrivateMessage instead.
     * @param playerID Player ID of the player you wish to send the message to.
     * @param message The message to be displayed.
     */
    public void sendPrivateMessage(int playerID, String message)
    {
        sendPrivateMessage(playerID, message, (byte) 0);
    }

    /** Sends a private message to someone in the same arena.  Do not attempt to use
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

    /** Sends a private message to someone in the same arena.  Do not attempt to use
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

    /** Sends a smart private message.  A smart private message is a private message
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

    /** Sends a smart private message.  A smart private message is a private message
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

    /** Sends a remote private message.  Remote private messages look like (Name)> to
     * the player, even if the player is in the same arena as you.  Try to use smart
     * private messages instead.
     * @param name The name of the player.
     * @param message The message to be displayed.
     */
    public void sendRemotePrivateMessage(String name, String message)
    {
        sendRemotePrivateMessage( name, message, 0 );
    }

    /** Sends a remote private message.  Remote private messages look like (Name)> to
     * the player, even if the player is in the same arena as you.  Try to use smart
     * private messages instead.
     * @param name The name of the player.
     * @param message The message to be displayed.
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

    /** Creates a set of teams of a particular size randomly. Starts at freq 0
     * and continues filling freqs completely until there are no players left.
     * @param teamSize The size of the team desired.
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

    /** Creates a certain number of random teams from non-specced players
     * Starts with freq 0, goes up to number specified - 1, in an even distribution
     * @param howMany how many random teams to create
     */
    public void createNumberOfTeams(int howMany)
    {
        StringBag plist = new StringBag();
        int current = 0;
        howMany -= 1;
        String name;

        //stick all of the players in randomizer
        Iterator i = m_arenaTracker.getPlayingPlayerIterator();
        while(i.hasNext())
            plist.add(((Player)i.next()).getPlayerName());

        //assign players to teams
        while(!plist.isEmpty())
        {
            if(current > howMany)
                current = 0;
            name = plist.grabAndRemove();
            setFreq(name,current);
            current++;
        }
    }

    /** Issues a /*spec command to the player supplied by the parameter.  Note that in
     * most cases the command must be issued twice, just as in the game.
     * @param playerName The name of the player.
     */
    public void spec(String playerName)
    {
        sendUnfilteredPrivateMessage(playerName, "*spec");
    }

    /** Issues a /*spec command to the player supplied by the parameter.  Note that in
     * most cases the command must be issued twice, just as in the game.
     * @param playerID Player ID of the player you want to spec.
     */
    public void spec(int playerID)
    {
        sendUnfilteredPrivateMessage(playerID, "*spec");
    }

    /** Places all players in the arena into spectator mode. */
    public void specAll()
    {
        sendUnfilteredPublicMessage("*specall");
    }

    /** Changes the ship type of a player.
     * @param playerID Player ID of the player you want to change.
     * @param shipType Ship type that you wish to set.  Must be a value between 1 and 8.
     */
    public void setShip(int playerID, int shipType)
    {
        sendUnfilteredPrivateMessage(playerID, "*setship " + shipType);
    }

    /** Changes the ship type of a player.
     * @param playerName The name of the player.
     * @param shipType Ship type that you wish to set.  Must be a value between 1 and 8.
     */
    public void setShip(String playerName, int shipType)
    {
        sendUnfilteredPrivateMessage(playerName, "*setship " + shipType);
    }

    /** Changes the frequency a player is on.
     * @param playerID Player ID of the player you want to change.
     * @param freqNum Frequency you want this player to be on.
     */
    public void setFreq(int playerID, int freqNum)
    {
        sendUnfilteredPrivateMessage(playerID, "*setfreq " + freqNum);
    }

    /** Changes the frequency a player is on.
     * @param playerName The name of the player.
     * @param freqNum Frequency you want this player to be on.
     */
    public void setFreq(String playerName, int freqNum)
    {
        sendUnfilteredPrivateMessage(playerName, "*setfreq " + freqNum);
    }

    /** Warps a player to the given coordinates
     * @param playerID PlayerID of the player to be warped
     * @param xTiles X coordinate
     * @param yTiles Y coordinate
     */
    public void warpTo(int playerID, int xTiles, int yTiles)
    {
        sendUnfilteredPrivateMessage(playerID, "*warpto " + xTiles + " " + yTiles);
    }

    /** Warps a player to the given coordinates
     * @param playerName The name of the player.
     * @param xTiles X coordinate
     * @param yTiles Y coordinate
     */
    public void warpTo(String playerName, int xTiles, int yTiles)
    {
        sendUnfilteredPrivateMessage(playerName, "*warpto " + xTiles + " " + yTiles);
    }

    /** Issues an LVZ object on command.
     * @param objID The ID of the object as denoted in the LVZ.
     */
    public void showObject(int objID)
    {
        sendUnfilteredPublicMessage("*objon " + objID);
    }

    /** Issues an LVZ object off command.
     * @param objID The ID of the object as denoted in the LVZ.
     */
    public void hideObject(int objID)
    {
        sendUnfilteredPublicMessage("*objoff " + objID);
    }

    /** Returns the Objset object associated with this bot
     */
    public Objset getObjectSet() {
        return m_objectSet;
    }

    /** Sets private objects of an individual player
     */
    public void setObjects() {
        if( m_objectSet.toSet() ) {
            sendUnfilteredPublicMessage( "*objset" + m_objectSet.getObjects() );
        }
    }

    /** Sets private objects of an individual player
     * @param playerId of the player
     */
    public void setObjects( int playerId ) {
        if( m_objectSet.toSet( playerId ) ) {
            sendUnfilteredPrivateMessage( playerId, "*objset" + m_objectSet.getObjects( playerId ) );
        }
    }

    /** Resets the scores of an individual player
     * @param playerName Name of player to be reset
     */
    public void scoreReset(String playerName)
    {
        sendUnfilteredPrivateMessage(playerName, "*scorereset");
    }

    /** Resets the scores of an individual player
     * @param playerID Player ID of player to be reset
     */
    public void scoreReset(int playerID)
    {
        sendUnfilteredPrivateMessage(playerID, "*scorereset");
    }

    /** Resets the scores of the players in an arena
     */
    public void scoreResetAll()
    {
        sendUnfilteredPublicMessage("*scorereset");
    }

    /** Issues a *shipreset to a specific player
     * @param playerName Name of the player to be *shipresetted
     */
    public void shipReset(String playerName)
    {
        sendUnfilteredPrivateMessage(playerName, "*shipreset");
    }

    /** Issues a *shipreset to a specific player
     * @param playerID Integer reference to the player specified
     */
    public void shipReset(int playerID)
    {
        sendUnfilteredPrivateMessage(playerID, "*shipreset");
    }

    /** Issues a *shipreset to all players in the arena
     */
    public void shipResetAll()
    {
        sendUnfilteredPublicMessage("*shipreset");
    }

    /** Sets the power of the thor weapons
     * @param thorAdjust Amount to adjust thor power by
     */
    public void setThorAdjust(int thorAdjust)
    {
        sendUnfilteredPublicMessage("*thor " + thorAdjust);
    }

    /** Issue a prize to a specific player
     * @param playerName Name of the player
     * @param prizeNum Number of the prize
     */
    public void prize(String playerName, int prizeNum)
    {
        sendUnfilteredPrivateMessage(playerName, "*prize " + prizeNum);
    }

    /** Issues a prize to a specified player
     * @param playerID Player ID
     * @param prizeNum Number of the prize
     */
    public void prize(int playerID, int prizeNum)
    {
        sendUnfilteredPrivateMessage(playerID, "*prize " + prizeNum);
    }


    /** Issues a specific prize to a player, using *prize #'s
     * @param playerName Name of the player
     * @param prizeNum number of the prize
     */
    public void specificPrize(String playerName, int prizeNum)
    {
        sendUnfilteredPrivateMessage(playerName, "*prize #" + prizeNum);
    }


    /** Issues a specific prize to a player, using *prize #'s
     * @param playerID Player ID
     * @param prizeNum Number of the prize
     */
    public void specificPrize(int playerID, int prizeNum)
    {
          sendUnfilteredPrivateMessage(playerID, "*prize #" + prizeNum);
    }


    /** Issues a prize to a frequency of players.  Possibly buggy?
     * @param freqID The frequency of players you wish to issue the prizes to.
     * @param prizeNum Number of the prize
     */
    public void prizeFreq(int freqID, int prizeNum)
    {
        try
        {
            for (Iterator i = m_arenaTracker.getFreqIDIterator(freqID); i.hasNext();)
            {
                prize(((Integer) i.next()).intValue(), prizeNum);
            }
        }
        catch (Exception e)
        {
        }
    }

    /** Issues a prize to every player in the arena
     * @param prizeNum Number of the prize
     */
    public void prizeAll(int prizeNum)
    {
        sendUnfilteredPublicMessage("*prize #" + prizeNum);
    }

    /** Gives an amount of bounty to everyone in the arena
     * @param number Amount of bounty to give
     */
    public void giveBounty(int number)
    {
        sendUnfilteredPublicMessage("*prize " + number);
    }

    /** Gives an amount of bounty to everyone in the arena.
     * @param name Name of the player to give the bounty to.
     * @param number Amount of bounty you want to give.
     */
    public void giveBounty(String name, int number)
    {
        sendUnfilteredPrivateMessage(name, "*prize " + number);
    }

    /** Gives an amount of bounty to everyone in the arena.
     * @param playerID PlayerID of the player you want to give the bounty to.
     * @param number Amount of bounty you want to give.
     */
    public void giveBounty(int playerID, int number)
    {
        sendUnfilteredPrivateMessage(playerID, "*prize " + number);
    }

    /** Resets the flag game
     */
    public void resetFlagGame()
    {
        sendUnfilteredPublicMessage("*flagreset");
    }

    /** Locks or unlocks the arena, depending on game state.
     */
    public void toggleLocked()
    {
        sendUnfilteredPublicMessage("*lock");
    }

    /** Starts or ends blue message out, depending on state.
     */
    public void toggleBlueOut()
    {
        sendUnfilteredPublicMessage("*lockpublic");
    }

    /** Sets the *timer game timer
     * @param seconds Time to game end in minutes
     */
    public void setTimer(int minutes)
    {
        sendUnfilteredPublicMessage("*timer" + minutes);
    }

    /** Warns the player with a moderator warning
     * @param playername The name of the player
     * @param message The message to be sent
     */
    public void warnPlayer(String playername, String message)
    {
        sendUnfilteredPrivateMessage(playername, "*warn " + message);
    }

    /** Warns the player with a moderator warning
     * @param playerID The player to be warned
     * @param message The message to be sent
     */
    public void warnPlayer(int playerID, String message)
    {
        sendUnfilteredPrivateMessage(playerID, "*warn " + message);
    }

    /** Gets the Flag object associated with the FlagID provided.  The Flag object
     * describes all the pertinent details about a flag in the arena the bot is in.
     * @param flagID The FlagID of the player you wish to retrieve info for.
     * @return Returns a Flag object.
     */
    public Flag getFlag(int flagID)
    {
        return m_arenaTracker.getFlag(flagID);
    }

    /** Gets the Player object associated with the PlayerID provided.  The Player object
     * describes all the pertinent details about a player in the arena the bot is in.
     * @param playerID The PlayerID of the player you wish to retrieve info for.
     * @return Returns a Player object.
     */
    public Player getPlayer(int playerID)
    {
        return m_arenaTracker.getPlayer(playerID);
    }

    /** Gets the Player object associated with the PlayerID provided.  The Player object
     * describes all the pertinent details about a player in the arena the bot is in.
     * @param playerName The name of the player you wish to retreive info for.
     * @return Returns a Player object.
     */
    public Player getPlayer(String playerName)
    {
        return m_arenaTracker.getPlayer(playerName);
    }

    /** Translates a playerID into a name, as a String.
     * @param playerID The PlayerID you want to translate.
     * @return The player's name.
     */
    public String getPlayerName(int playerID)
    {
        return m_arenaTracker.getPlayerName(playerID);
    }

    /** Translates a player's name into a playerID.  Be careful while using this,
     * however, as players may or may not be in a given arena.
     * @param playerName The name of the player.
     * @return The PlayerID of the player given in playerName.
     */
    public int getPlayerID(String playerName)
    {
        return m_arenaTracker.getPlayerID(playerName);
    }

    /** Searches for players in the arena of which the first part of the name
     * matches with 'playerName'. When there are multiple player matches it will
     * return the first found match OR the player which exactly matches (case
     * insensitive) 'playerName' if there is one.
     * @param playerName The partial name of a player
     * @return The PlayerID of the first found player matching or starting with playerName
     */
    public Player getFuzzyPlayer(String playerName)
    {
        String fuzzyResult = getFuzzyPlayerName(playerName);
        if (fuzzyResult != null)
            return getPlayer(fuzzyResult);
         else
            return null;
    };

    /** Searches for players in the arena of which the first part of the name
     * matches with 'playerName'. When there are multiple player matches it will
     * return the first found match OR the player which exactly matches (case
     * insensitive) 'playerName' if there is one.
     * @param playerName The partial name of a player
     * @return The PlayerName of the first found player matching or starting with playerName
     */
    public String getFuzzyPlayerName(String playerName)
    {
        Iterator i = m_arenaTracker.getPlayerIterator();
        String answ, best = null;

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
        };

        return best;
    };

    /** Retreives the name of the bot.
     * @return The name of the bot.
     */
    public String getBotName()
    {
        return m_botSession.getBotName();
    }

    /** Causes the bot to join an arena.  If the bot is already in an arena, use
     * changeArena( int ) instead.  This method is only to be used before the bot is in
     * an arena.  String encoded Integer values denote public arenas.
     * @author Dock>, Modified by FoN
     *
     * @param arenaName The name or number of the arena to join.
     *
     * @version 1.1 - added catch to handle exception thrown by sendArenaLoginPacket
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

    /**Overloaded function to joinArena which allows to specify the resolution of the arena
     * @see joinArena(String arenaName)
     *
     * @author Kirthi Sugnanam - FoN
     *
     * @param arenaName The arena string to join
     * @param xResolution The X - coordinate resolution for the screen
     * @param yResolution The Y - coordinate resolution for the screen
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

    /** Causes the bot to join a public arena.
     * @author Dock>, Modified by FoN
     *
     * @param arena The arena number to join
     *
     * @version 1.1 - added catch to handle exception thrown at sendArenaLoginPacket
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

    /**Overloaded function to joinArena which allows to specify the resolution of the arena
     * @see joinArena(short arena)
     *
     * @author Kirthi Sugnanam - FoN
     *
     * @param arena The arena number to join
     * @param xResolution The X - coordinate resolution for the screen
     * @param yResolution The Y - coordinate resolution for the screen
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

    /** Causes the bot to join a random public arena
     * @author Dock>, Modfied by FoN
     * @version 1.1 - added catch for exception thrown by sendArenaLoginPacket
     *
     * */
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

    /** This method tells the bot to leave the current arena, and join another
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

    /** This is an overloaded change function to allow variations in resolution
     * @see changeArena(String newArenaName)
     * @author Kirthi Sugnanam - FoN
     *
     * @param newArenaName Name or number of the arena to change to.
     * @param xResolution The X Max for the screen size
     * @param yResolution The Y Max for the screen size
     * @exception catches the resolution mistake for specifying X max and Y
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

    /** This method tells the bot to leave the current arena, and join another
     * arena specified by an integer.
     * @param arenaNumber The number of the public arena to change to.
     */
    public void changeArena(short arenaNumber)
    {
        m_packetGenerator.sendArenaLeft();
        m_arenaTracker.clear();
        joinArena(arenaNumber);
    }

    /** This is an overloaded function to allow resolution changes
     * @see changeArena(short arenaNumber)
     * @author Kirthi Sugnanam - FoN
     *
     * @param arenaNumber The number of the public arena to change to.
     * @param xResolution The X Max for the screen size
     * @param yResolution The Y Max for the screen size
     * @exception catches the resolution mistake for specifying X max and Y
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

    /** Gets the name of the arena the bot is in.
     * @return the arena the bot currently is in or is going to
     */
    public String getArenaName()
    {
        if (m_arenaName != null)
            return m_arenaName;
        else
            return "Unknown";
    };

    /** Sets the kill message reliability to an integer bounty value.
     * @param var Amount of bounty for kill messages to become reliable.
     */
    public void setReliableKills(int var)
    {
        sendUnfilteredPublicMessage("*relkills " + var);
    }

    /** Sets the bot to spectate the specified player and surrounding area
     * @param playerID Ident of the player to spectate
     */
    public void spectatePlayer(int playerID)
    {
        m_packetGenerator.sendSpectatePacket((short) playerID);
    }

    /** Sets the bot to spectate the specified player and surrounding area
     * @param playerName Name of the player to spectate
     */
    public void spectatePlayer(String playerName)
    {
        m_packetGenerator.sendSpectatePacket((short) m_arenaTracker.getPlayerID(playerName));
    }

    /** Sets the bot to pickup the specified flag
     * @param flagID ID of flag to pickup.
     */
    public void grabFlag(int flagID)
    {
        m_packetGenerator.sendFlagRequestPacket((short) flagID);
    }

    /** Drops all flags the bot is carrying
                         */
    public void dropFlags()
    {
        m_packetGenerator.sendFlagDropPacket();
    }

    /** Warps all the players in the arena to location X, Y.
     * @param x X coordinate to warp the players to
     * @param y X coordinate to warp the players to
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

    /** Warps all the players on a frequency to location X, Y.
     * @param freq Frequency to warp
     * @param x X coordinate to warp the players to
     * @param y Y coordinate to warp the players to
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

    /** Issues a *setship to all the players in the arena (who are not in spec).
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

    /** Warps the warp prize to all playing players in the arena.
     */
    public void warpAllRandomly()
    {
        prizeAll(7);
    }

    /** Changes all the ships on a freq to a particular ship type.
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

    /** Changes all the players on one freq to another freq.  Great for consolidating
     * teams.
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

    /** Changes the frequency of all the playing players in the game to a specified
     * frequency.
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

    /** Gets the total score for a particular frequency.
     * @param freq Frequency of the players to get the score for.
     * @return The total score for the players on the specified frequency.
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
     * Gets the total number of players currently playing.
     * @return # players
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


    /** Sets the doors in the arena to the specified value.  If you wish, you can use
     * setDoors( String ) and enter binary string such as "11010110" to turn doors
     * on or off.
     * @param doors The value of the doors to be set.
     */
    public void setDoors(int doors)
    {
        if (doors < -2 || doors > 255)
            return;
        sendUnfilteredPublicMessage("?set door:doormode:" + doors);
    }

    /** Sends the contents of the String array to the player in private messages.
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

    /** Sends the contents of the String array to the player in private messages.
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
     * Private message spam but with a gerneralized collection to allow for dynamic help statements
     * This will not work remotely as in REMOTE.MESSAGE type
     * @param playerID ID of the player to be spammed
     * @param helpMessages The collection of messages (Need to be String Objects or typed to string objects)
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
     * Private message spam but with a gerneralized collection to allow for dynamic help statements
     * @param playerName Name of the player to be spammed
     * @param helpMessages The collection of messages (Need to be String Objects or typed to string objects)
     */
    public void privateMessageSpam(String playerName, Collection messages)
    {
        Iterator i = messages.iterator();
        while (i.hasNext())
        {
            sendSmartPrivateMessage(playerName, (String)i.next());
        }
    }

    /** Sends the contents of the String array to the player in remote private messages.
     * Careful with this one, sending too many across the billing server can cause
     * trouble.
     * @param playerName Name of the player to send the messages to.
     * @param spam The array of Strings to send to the player in private messages.
     */
    public void remotePrivateMessageSpam(final String playerName, final String[] spam)
    {
        for (int i = 0; i < spam.length; i++)
        {
            sendSmartPrivateMessage(playerName, spam[i]);
        }
    }

    /** This Sends the contents of the String array to the player in smart private
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

    /** Tells if a freq in the arena contains a specific type of ship.
     * @param freq Frequency to check
     * @param ship Ship type to look for
     * @return Returns whether the ship exists in the freq
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

    /** Gets the number of players in the arena.
     * @return The number of players in the arena.
     */
    public int getArenaSize()
    {

        return m_arenaTracker.size();
    }

    /** Checks every player in the game for X number of deaths, and places them in
     * spectator mode.  Use this command once, then use the PlayerDeath packets to
     * count the rest.
     * @param deaths Number of deaths to spec at.
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

    /** Tells the bot to go parachuting (without the parachute). */
    public void die()
    {
        m_botSession.disconnect();
    }

    /** Sends a death packet. (Fixed by D1st0rt 3-26-05)
     * I'm pretty sure nothing will happen if you send an invalid playerID
     * @param playerID the id of the player that killed the bot.
     * @param bounty Amount of bounty the death is related to.
     */
    public void sendDeath(int playerID, int bounty)
    {
        m_packetGenerator.sendPlayerDeath(playerID, bounty);
    }


    /** Unimplemented
     * @param size Maximum size of the freqs
     */
    public void fillInTeams(int size)
    {

    }

    /** Returns an iterator of all non-specced Players.  Example usage:
     * Iterator i = m_botAction.getPlayingPlayerIterator();
     * while( i.hasNext() ){
     *    Player p = (Player)i.next();
     *    if( p.getPlayerName().equals( "DoCk>" )){
     *        m_botAction.sendPrivateMessage( p.getPlayerID(), "Hi DoCk>!" );
     *    } else if( p.getFrequency() == 223 && p.getSquadName().equals( "LAME" )){
     *        m_botAction.sendPrivateMessage( p.getPlayerID(), "L!" );
     *    }
     * }
     * @return An Iterator of players who are playing
     */
    public Iterator getPlayingPlayerIterator()
    {
        return m_arenaTracker.getPlayingPlayerIterator();
    }

    /**
     * @return an iterator of all players in a frequency (currently not working)
     * @param freq
     */
    public Iterator getFreqPlayerIterator(int freq)
    {
        return m_arenaTracker.getFreqPlayerIterator(freq);
    }

    /** Gets an Iterator of all the Players playing in the game.
     * @return An Iterator of all the Players playing in the game.
     */
    public Iterator getPlayerIterator()
    {
        return m_arenaTracker.getPlayerIterator();
    }

    /** Gets the PlayerIDs of all the players in the Iterator.
     * @return Returns an Iterator containing the player ID value wrapped in an Integer
     */
    public Iterator getPlayerIDIterator()
    {
        return m_arenaTracker.getPlayerIDIterator();
    }

    /** Gets an Iterator of all the Flags in the game.
     * @return An Iterator of all the Flags in the game.
     */
    public Iterator getFlagIDIterator()
    {
        return m_arenaTracker.getFlagIDIterator();
    }

    /** Gets a BotSettings for this bot.  Reads the .cfg file.
     * @return A BotSettings object containing data from the bot's .cfg
     */
    public BotSettings getBotSettings()
    {
        String botName = m_botSession.getBotClass().getName();
        if (botName.indexOf(".") != -1 ) {
            botName = botName.substring(botName.lastIndexOf(".") + 1);
        }
        return m_botSession.getCoreData().getBotConfig(botName);
    }

    /** Moves the bot to the tile (x,y).  512, 512 is the center.
     * @param x X value to move the bot to
     * @param y Y value to move the bot to
     */
    public void moveToTile(int x, int y)
    {
        m_botSession.getShip().move(x * 16, y * 16);
    }

    /** Moves the bot to position X, Y.  Center of the arena is (8192, 8192) as opposed
     * to the less accurate (512, 512) of moveToTile( x, y ).
     * @param x Position of the bot along the X axis.
     * @param y Position of the bot along the Y axis.
     */
    public void move(int x, int y)
    {
        m_botSession.getShip().move(x, y);
    }

    public void move(int x, int y, int vx, int vy)
    {
        m_botSession.getShip().move(x, y, vx, vy);
    }

    /** Gets the Ship class, where ship movements, firing, and flying can be controlled.
     * @return The Ship class, which controls the in-game flight of the bot.
     */
    public Ship getShip()
    {
        return m_botSession.getShip();
    }

    /** Runs a direct SQL Query.  This method does block up the bot from doing anything
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

    /** Runs a <B>high priority</B> backround SQL Query.  This is the same as a normal
     * background query, except these queries are executed first, if there are other
     * queries in line to be executed.  Use this only for time-important queries.
     * Otherwise, let them flow along with the rest.
     * @param connectName The connection name as specified in sql.cfg
     * @param identifier A unique identifier that describes what this Query is.  This identifier will be
     * found in the SQLEvent when it is returned.  This identifier helps you handle
     * different sorts of background queries differently when they come out the other
     * end.  If the identifier is null, the result of the query will not be delivered.
     * @param query The SQL query to be executed
     */
    public void SQLBackgroundQuery(String connectName, String identifier, String query)
    {
        getCoreData().getSQLManager().queryBackground(connectName, identifier, query, m_botSession.getSubspaceBot());
    }

    /** Runs a high priority background query.  This query will be executed before all
     * the others.  This is useful for when you know the queue will be rather large,
     * and you need a query done immediately.
     * @param connectName The connection name as specified in sql.cfg
     * @param identifier A unique identifier that describes what this Query is.  This identifier will be
     * found in the SQLEvent when it is returned.  This identifier helps you handle
     * different sorts of background queries differently when they come out the other
     * end.  If the identifier is null, the result of the query will not be delivered.
     * @param query The SQL query to be executed
     */
    public void SQLHighPriorityBackgroundQuery(String connectName, String identifier, String query)
    {
        getCoreData().getSQLManager().queryBackgroundHighPriority(connectName, identifier, query, m_botSession.getSubspaceBot());
    }

    /** Insert Into helper method.
     * @param connectName The connection name as specified in sql.cfg
     * @param tableName The name of the table you wish to insert the values into.
     * @param fields The field names you want to enter data into.
     * @param values The corresponding values for the field names.
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
        };
    }

    /** Insert Into helper method.
     * @param connectName The connection name as specified in sql.cfg
     * @param tableName The name of the table you wish to insert the values into.
     * @param fields The field names you want to enter data into.
     * @param values The corresponding values for the field names.
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
        };
    }

    /** Sends out a request for an ArenaList packet to be sent back. */
    public void requestArenaList()
    {
        sendUnfilteredPublicMessage("?arena");
    }

    /** Gets the EventRequester object.  This object controls what events are being sent
     * to the bot.  EventRequester can turn on or off any events at any time.
     * @return The EventRequester object for this bot.
     */
    public EventRequester getEventRequester()
    {
        return m_botSession.getEventRequester();
    }

    /** Transmits an object to the specified channel over inter-process communication.
     * The object will arrive as an InterProcessEvent, which must be handled by the
     * bot.  If a bot is not yet subscribed to the channel, the bot will automatically
     * be subscribed.
     * @param channelName Channel name to send the object to.
     * @param o Object to be transmitted
     */
    public void ipcTransmit(String channelName, Object o)
    {
        getCoreData().getInterProcessCommunicator().broadcast(channelName, m_botSession.getBotName(), m_botSession.getSubspaceBot(), o);
    }

    /** Subscribes the current bot to the specified IPC Channel.  Please remember you
     * <B>must not</B> attempt to use this method in the constructor of a bot.  This
     * method must be used in the LoggedOn packet handler.
     * @param channelName Name of the IPC channel you wish to subscribe to.
     */
    public void ipcSubscribe(String channelName)
    {
        getCoreData().getInterProcessCommunicator().subscribe(channelName, m_botSession.getSubspaceBot());
    }

    /** Unsubscribes this bot from the provided channelName.  If the bot was the last
     * one on the channel, the channel is destroyed.
     * @param channelName Name of the IPC channel you wish to unsubscribe from.
     */
    public void ipcUnSubscribe(String channelName)
    {
        getCoreData().getInterProcessCommunicator().unSubscribe(channelName, m_botSession.getSubspaceBot());
    }

    /** Explicitly destroys a channel
     * @param channelName The name of the channel to be destroyed
     */
    public void ipcDestroyChannel(String channelName)
    {
        getCoreData().getInterProcessCommunicator().destroy(channelName);
    }

    public InterProcessCommunicator getIPC()
    {
        return getCoreData().getInterProcessCommunicator();
    }

    /** Uses a binary string such as "11010110" to turn specific doors on or off.
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

    /** Toggles WatchDamage for this player.
     * @param playerID PlayerID of the player
     */
    public void toggleWatchDamage(int playerID)
    {
        sendUnfilteredPrivateMessage(playerID, "*watchdamage");
    }

    /** Toggles WatchDamage for this player.
     * @param playerName PlayerName of the player
     */
    public void toggleWatchDamage(String playerName)
    {
        sendUnfilteredPrivateMessage(playerName, "*watchdamage");
    }

    /** Gets a file from the Data directory, given the filename as a String.  Filename
     * can a path relative to the data directory.
     * @param filename Filename or pathname to the file in question.
     * @return File object for the specified file.
     */
    public File getDataFile(String filename)
    {
        String location = getCoreData().getGeneralSettings().getString("Core Location");
        return new File(location + "/data", filename);
    }

    /**
     * @return a file containing the core configuration
     * @param filename
     */
    public File getCoreCfg(String filename)
    {
        String location = getCoreData().getGeneralSettings().getString("Core Location");
        return new File(location + "/corecfg", filename);
    }

    public CoreData getCoreData()
    {
        return m_botSession.getCoreData();
    }

    /** Gets the directory the core is in as a File object.  Specified by setup.cfg
     * @return File object representing the directory the core is in
     */
    public File getCoreDirectory()
    {
        return new File(getGeneralSettings().getString("Core Location"));
    }

    /** Gets a file from the core directory. May be a pathname relative to the core
     * directory as well.
     * @param filename Filename or pathname of the file or directory
     * @return File representation of the object
     */
    public File getCoreDirectoryFile(String filename)
    {
        return new File(getGeneralSettings().getString("Core Location") + File.separatorChar + filename);
    }

    /** Sets up spam protection for the bot.  Indicate the number of messages per minute
     * the player is allowed.
     * @param msgsPerMin Number of messages per minute you want the bot to allow.
     */
    public void setMessageLimit(int msgsPerMin)
    {
        m_botSession.getGamePacketInterpreter().setMessageLimiter(msgsPerMin, this);
    }

    /** If the SQL connection pools weren't initialized properly for some reason, this
     * method will return false.  Good for disabling portions of bots that use SQL.
     * @return Boolean value representing the operational status of the SQL Connection pools.
     */
    public boolean SQLisOperational()
    {
        return getCoreData().getSQLManager().isOperational();
    }

    /** Adjusts the packet send delay.  The more packets that are sent out, the greater
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

     /** Turns automatic player position updating on and off. By default it is off.
     * @param milliseconds - specified time to update player positions at
     * 0      : turns tracking off and has the bot spectate its current area
     * <200  : turns tracking on with 200 ms change rate
     * >=200 : turns tracking on with specified rate
     */
    public void setPlayerPositionUpdating( int milliseconds ) {
        m_arenaTracker.setPlayerPositionUpdateDelay( milliseconds );
    }

    /** Sets the bots personal banner
     * @param _banner A byte array containing the banner data
     */
    public void setBanner( byte[] _banner ) {

        m_packetGenerator.sendBannerPacket( _banner );
    }
}