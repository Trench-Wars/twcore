package twcore.bots.purepubbot;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.Vector;
import java.util.ArrayList;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.ArenaList;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.lvz.Objset;
import twcore.core.util.Tools;

/**
 * "Pure" pub bot that can enforce ship restrictions, freq restrictions, and run
 * a timed pub game using a flag. (Note that for non-TW zones, the warp points for
 * flag time games be set up by hand.)
 *
 * Restrictions for any ship can be easily enforced using this bot.  Each restriction
 * should be marked in this format in the CFG: (BotName)Ship(#)=(Value), e.g., if
 * the bot's name is MyPurePub, to completely restrict ship 1, one would use
 * MyPurePubShip1=0, and to allow ship 3, one would use MyPurePubShip3=1.  All
 * playable ships 1-8 must be defined for each bot.  Ship 0 is autodefined as 1.
 *
 *   Values:
 *   0  - No ships of this type allowed
 *   1  - Unlimited number of ships of this type are allowed
 *   #  - If the number of current ships of the type on this frequency is
 *        greater than the total number of people on the frequency divided
 *        by this number (ships of this type > total ships / weight), then the
 *        ship is not allowed.  The exception to this rule is if the player is
 *        the only one on the freq currently in the ship.
 *
 * For example, to say that only half the ships on a freq are allowed to be javs:
 * MyPurePub2=2, and for only a fifth of the ships allowed to be terrs, MyPurePub=5.
 * See JavaDocs of the checkPlayer(int) method for more information.
 *
 *
 * (NOTE: purepubbot is different than the pub bot module and hub system.  Pubhub /
 * Pubbot answers queries about player aliases, spies for certain words, monitors
 * TKs, informs people of messages received, and can perform any other task necessary
 * in a pub -- particularly ones that require a way to verify when a person logs on.)
 *
 * @author Cpt.Guano!  (Timed pub & specific restrictions: qan)
 * @see pubbot; pubhub
 */
public class purepubbot extends SubspaceBot
{
    public static final int SPEC = 0;                   // Number of the spec ship
    public static final int FREQ_0 = 0;                 // Frequency 0
    public static final int FREQ_1 = 1;                 // Frequency 1
    private static final int FLAG_CLAIM_SECS = 3;		// Seconds it takes to fully
                                                        // claim a flag
    private static final int INTERMISSION_SECS = 90;	// Seconds between end of round
                                                        // and start of next
    private static final int NUM_WARP_POINTS_PER_SIDE = 6; // Total number of warp points
                                                           // per side of FR
    private static final int MAX_FLAGTIME_ROUNDS = 5;   // Max # rounds (odd numbers only)

    private OperatorList opList;                        // Admin rights info obj
    private HashSet <String>freq0List;                  // Players on freq 0
    private HashSet <String>freq1List;                  // Players on freq 1
    private HashMap <String,Integer>playerTimes;        // Roundtime of player on freq
    private boolean started;                            // True if pure pub is enabled
    private boolean privFreqs;                          // True if priv freqs are allowed
    private boolean flagTimeStarted;                    // True if flag time is enabled
    private boolean strictFlagTime;                     // True for autowarp in flag time
    private FlagCountTask flagTimer;                    // Flag time main class
    private StartRoundTask startTimer;                  // TimerTask to start round
    private IntermissionTask intermissionTimer;         // TimerTask for round intermission
    private int flagMinutesRequired;                    // Flag minutes required to win
    private int freq0Score, freq1Score;                 // # rounds won
    boolean initLogin = true;                           // True if first arena login
    private int initialPub;                             // Order of pub arena to defaultjoin
    private String initialSpawn;                        // Arena initially spawned in
    private Vector <Integer>shipWeights;                // "Weight" restriction per ship
    private List <String>warpPlayers;                   // Players that wish to be warped
    private Objset objs;                                // For keeping track of counter

    // X and Y coords for warp points.  Note that the first X and Y should be
    // the "standard" warp; in TW this is the earwarp.  These coords are used in
    // strict flag time mode.
    private int warpPtsLeftX[]  = { 487, 505, 502, 499, 491, 495 };
    private int warpPtsLeftY[]  = { 255, 260, 267, 274, 279, 263 };
    private int warpPtsRightX[] = { 537, 519, 522, 525, 529, 533 };
    private int warpPtsRightY[] = { 255, 260, 267, 274, 263, 279 };

    // Warp coords for safes (for use in strict flag time mode)
    private static final int SAFE_LEFT_X = 306;
    private static final int SAFE_LEFT_Y = 482;
    private static final int SAFE_RIGHT_X = 717;
    private static final int SAFE_RIGHT_Y = 482;


    /**
     * Creates a new instance of purepub bot and initializes necessary data.
     *
     * @param Reference to bot utility class
     */
    public purepubbot(BotAction botAction)
    {
        super(botAction);
        requestEvents();
        opList = m_botAction.getOperatorList();
        freq0List = new HashSet<String>();
        freq1List = new HashSet<String>();
        playerTimes = new HashMap<String,Integer>();
        started = false;
        privFreqs = true;
        flagTimeStarted = false;
        strictFlagTime = false;
        warpPlayers = Collections.synchronizedList( new LinkedList<String>() );
        shipWeights = new Vector<Integer>();
        objs = m_botAction.getObjectSet();
    }


    /**
     * Requests all of the appropriate events.
     */
    private void requestEvents()
    {
        EventRequester eventRequester = m_botAction.getEventRequester();
        eventRequester.request(EventRequester.MESSAGE);
        eventRequester.request(EventRequester.PLAYER_LEFT);
        eventRequester.request(EventRequester.PLAYER_ENTERED);
        eventRequester.request(EventRequester.FLAG_CLAIMED);
        eventRequester.request(EventRequester.FREQUENCY_CHANGE);
        eventRequester.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        eventRequester.request(EventRequester.LOGGED_ON);
        eventRequester.request(EventRequester.ARENA_LIST);
        eventRequester.request(EventRequester.ARENA_JOINED);
    }


    /* **********************************  EVENTS  ************************************ */

    /**
     * Retreives all necessary settings for the bot to operate.
     *
     * @param event is the event to process.
     */
    public void handleEvent(LoggedOn event)
    {
        BotSettings botSettings = m_botAction.getBotSettings();
        initialSpawn = botSettings.getString("InitialArena");
        initialPub = (botSettings.getInt(m_botAction.getBotName() + "Pub") - 1);
        m_botAction.joinArena(initialSpawn);
        shipWeights.add( new Integer(1) );		// Allow unlimited number of spec players
        for( int i = 1; i < 9; i++ )
            shipWeights.add( new Integer( botSettings.getInt(m_botAction.getBotName() + "Ship" + i) ) );
    }


    /**
     * Requests arena list to move to appropriate pub automatically, if the arena
     * is the first arena joined.
     *
     * @param event is the event to process.
     */
    public void handleEvent(ArenaJoined event)
    {
    	if(!initLogin)
    		return;

    	initLogin = false;
    	m_botAction.requestArenaList();
    }


    /**
     * Sends bot to public arena specified in CFG.
     *
     * @param event is the event to process.
     */
    public void handleEvent(ArenaList event)
    {
    	String[] arenaNames = event.getArenaNames();

        Comparator <String>a = new Comparator<String>()
        {
            public int compare(String a, String b)
            {
                if (Tools.isAllDigits(a) && !a.equals("") ) {
                    if (Tools.isAllDigits(b) && !b.equals("") ) {
                        if (Integer.parseInt(a) < Integer.parseInt(b)) {
                            return -1;
                        } else {
                            return 1;
                        }
                    } else {
                        return -1;
                    }
                } else if (Tools.isAllDigits(b)) {
                    return 1;
                } else {
                    return a.compareToIgnoreCase(b);
				}
            };
        };

        Arrays.sort(arenaNames, a);

    	String arenaToJoin = arenaNames[initialPub];
    	if(Tools.isAllDigits(arenaToJoin))
    	{
    		m_botAction.changeArena(arenaToJoin);
    		startBot();
    	}
    }


    /**
     * Handles the FrequencyShipChange event.
     * Checks players for appropriate ships/freqs.
     * Resets their MVP timer if they spec or change ships (new rule).
     *
     * @param event is the event to process.
     */
    public void handleEvent(FrequencyShipChange event)
    {
        int playerID = event.getPlayerID();
        int freq = event.getFrequency();

        if(started) {
            checkPlayer(playerID);
            if(!privFreqs)
                checkFreq(playerID, freq, true);
        }

        Player p = m_botAction.getPlayer( playerID );
        if( p == null )
            return;

        try {
            if( flagTimeStarted && flagTimer.isRunning() ) {
                // Remove player if spec'ing
                if( p.getShipType() == 0 ) {
                    String pname = p.getPlayerName();
                	playerTimes.remove( pname );
                // Reset player if shipchanging
                } else {
                    String pname = p.getPlayerName();
                	playerTimes.remove( pname );
                    playerTimes.put( pname, new Integer( flagTimer.getTotalSecs() ) );
                }
            }
        } catch (Exception e) {
        }
    }


    /**
     * Removes a player from all tracking lists when they leave the arena.
     *
     * @param event is the event to handle.
     */
    public void handleEvent(PlayerLeft event)
    {
        int playerID = event.getPlayerID();
        String playerName = m_botAction.getPlayerName(playerID);

        removeFromLists(playerName);
        removeFromWarpList(playerName);
    	playerTimes.remove( playerName );
    }


    /**
     * Checks if freq is valid (if private frequencies are disabled), and prevents
     * freq-hoppers from switching freqs for end round prizes.
     *
     * @param event is the event to handle.
     */
    public void handleEvent(FrequencyChange event)
    {
        int playerID = event.getPlayerID();
        int freq = event.getFrequency();

        if(started) {
            checkPlayer(playerID);
            if(!privFreqs)
                checkFreq(playerID, freq, true);
        }

        Player p = m_botAction.getPlayer( playerID );
        if( p == null )
            return;

        try {
            if( flagTimeStarted && flagTimer.isRunning() ) {
                String pname = p.getPlayerName();
                playerTimes.remove( pname );
                playerTimes.put( pname, new Integer( flagTimer.getTotalSecs() ) );
            }
        } catch (Exception e) {
        }
    }


    /**
     * When a player enters, displays necessary information, and checks
     * their ship & freq.
     *
     * @param event is the event to process.
     */
    public void handleEvent(PlayerEntered event)
    {
        try {
            int playerID = event.getPlayerID();
            Player player = m_botAction.getPlayer(playerID);
            String playerName = m_botAction.getPlayerName(playerID);

            if(started)
            {

                m_botAction.sendPrivateMessage(playerName, "Pure Pub enabled.  Private Freqs: [" + (privFreqs ? "OK" : "NO") + "]" + "  Timed pub: [" + (flagTimeStarted ? "ON" : "OFF") + "]" );

                String restrictions = "";
                int weight;

                for( int i = 1; i < 9; i++ ) {
                    weight = shipWeights.get( i ).intValue();
                    if( weight == 0 )
                        restrictions += Tools.shipName( i ) + "s disabled.  ";
                    if( weight > 1 )
                        restrictions += Tools.shipName( i ) + "s limited.  ";
                }

                if( restrictions != "" )
                    m_botAction.sendPrivateMessage(playerName, "Ship restrictions: " + restrictions );

                checkPlayer(playerID);
                if(!privFreqs)
                    checkFreq(playerID, player.getFrequency(), false);
                m_botAction.sendPrivateMessage(playerName, "Commands available: !help, !team, !listships, !time, !warp");
            }
            if(flagTimeStarted)
                if( flagTimer != null)
                    m_botAction.sendPrivateMessage(playerName, flagTimer.getTimeInfo() );
        } catch (Exception e) {
        }

    }


    /**
     * If flag time mode is running, register with the flag time game that the
     * flag has been claimed.
     *
     * @param event is the event to handle.
     */
    public void handleEvent(FlagClaimed event)
    {
        if(!flagTimeStarted)
            return;

        int playerID = event.getPlayerID();
        Player p = m_botAction.getPlayer(playerID);

        try {
            if( p != null && flagTimer != null ) {
                flagTimer.flagClaimed( p.getFrequency(), playerID );
            }
        } catch (Exception e) {
        }
    }


    /**
     * Handles all messages received.
     *
     * @param event is the message event to handle.
     */
    public void handleEvent(Message event)
    {
        String sender = getSender(event);
        int messageType = event.getMessageType();
        String message = event.getMessage().trim();

        if((messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE) )
            handleCommand(sender, message);
    }


    /* **********************************  COMMANDS  ************************************ */

    /**
     * Handles commands sent to the bot.
     *
     * @param sender is the person issuing the command.
     * @param message is the command that is being sent.
     */
    public void handleCommand(String sender, String message)
    {
        String command = message.toLowerCase();

        try
        {
            if(message.equals("!time"))
                doTimeCmd(sender);
            else if(command.equals("!help"))
                doHelpCmd(sender);
            else if(command.equals("!warp"))
                doWarpCmd(sender);
            else if(command.equals("!listships"))
                doListShipsCmd(sender);
            else if(command.equals("!team"))
                doShowTeamCmd(sender);

            if ( !opList.isHighmod(sender) && !sender.equals(m_botAction.getBotName()) )
                return;

            if(command.startsWith("!go "))
                doGoCmd(sender, message.substring(4));
            else if(command.equals("!start"))
                doStartCmd(sender);
            else if(command.equals("!stop"))
                doStopCmd(sender);
            else if(command.equals("!privfreqs"))
                doPrivFreqsCmd(sender);
            else if(command.startsWith("!starttime "))
                doStartTimeCmd(sender, message.substring(11));
            else if(command.startsWith("!stricttime"))
                doStrictTimeCmd(sender);
            else if(command.equals("!stoptime"))
                doStopTimeCmd(sender);
            else if(command.startsWith("!set "))
                doSetCmd(sender, message.substring(5));
            else if(command.equals("!die"))
                doDieCmd(sender);
        }
        catch(RuntimeException e)
        {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }


    /**
     * Moves the bot from one arena to another.  The bot must not be
     * started for it to move.
     *
     * @param sender is the person issuing the command.
     * @param argString is the new arena to go to.
     * @throws RuntimeException if the bot is currently running.
     * @throws IllegalArgumentException if the bot is already in that arena.
     */
    public void doGoCmd(String sender, String argString)
    {
        String currentArena = m_botAction.getArenaName();

        if(started || flagTimeStarted)
            throw new RuntimeException("Bot is currently running pure pub settings in " + currentArena + ".  Please !Stop and/or !Endtime before trying to move.");
        if(currentArena.equalsIgnoreCase(argString))
            throw new IllegalArgumentException("Bot is already in that arena.");

        m_botAction.changeArena(argString);
        m_botAction.sendSmartPrivateMessage(sender, "Bot going to: " + argString);
    }


    /**
     * Starts the pure pub settings.
     *
     * @param sender is the person issuing the command.
     * @throws RuntimeException if the bot is already running pure pub settings.
     */
    public void doStartCmd(String sender)
    {
        if(started)
            throw new RuntimeException("Bot is already running pure pub settings.");

        started = true;
        specRestrictedShips();
        m_botAction.sendArenaMessage("Pure pub settings enabled.  Ship restrictions are now in effect.", 2);
        m_botAction.sendSmartPrivateMessage(sender, "Pure pub succesfully enabled.");
    }


    /**
     * Stops the pure pub settings.
     *
     * @param sender is the person issuing the command.
     * @throws RuntimeException if the bot is not currently running pure pub
     * settings.
     */
    public void doStopCmd(String sender)
    {
        if(!started)
            throw new RuntimeException("Bot is not currently running pure pub settings.");

        started = false;
        m_botAction.sendArenaMessage("Pure pub settings disabled.  Ship restrictions are no longer in effect.", 2);
        m_botAction.sendSmartPrivateMessage(sender, "Pure pub succesfully disabled.");
    }


    /**
     * Toggles if private frequencies are allowed or not.
     *
     * @param sender is the sender of the command.
     */
    public void doPrivFreqsCmd(String sender)
    {
        if(!started)
            throw new RuntimeException("Bot is not currently running pure pub settings.");
        if(!privFreqs)
        {
            m_botAction.sendArenaMessage("Private Frequencies enabled.", 2);
            m_botAction.sendSmartPrivateMessage(sender, "Private frequencies succesfully enabled.");
        }
        else
        {
            fixFreqs();
            m_botAction.sendArenaMessage("Private Frequencies disabled.", 2);
            m_botAction.sendSmartPrivateMessage(sender, "Private frequencies succesfully disabled.");
        }
        privFreqs = !privFreqs;
    }


    /**
     * Starts a "flag time" mode in which a team must hold the flag for a certain
     * consecutive number of minutes in order to win the round.
     *
     * @param sender is the person issuing the command.
     * @param argString is the number of minutes to hold the game to.
     */
    public void doStartTimeCmd(String sender, String argString )
    {
        if(flagTimeStarted)
            throw new RuntimeException( "Flag Time mode has already been started." );

        int min = 0;

        try {
            min = (Integer.valueOf( argString )).intValue();
        } catch (Exception e) {
            throw new RuntimeException( "Bad input.  Please supply a number." );
        }

        if( min < 1 || min > 120 )
            throw new RuntimeException( "The number of minutes required must be between 1 and 120." );

        flagMinutesRequired = min;

        m_botAction.sendArenaMessage( "Flag Time mode has been enabled." );

        m_botAction.sendArenaMessage( "Object: Hold flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win a round.  Best " + ( MAX_FLAGTIME_ROUNDS + 1) / 2 + " of "+ MAX_FLAGTIME_ROUNDS + " wins the game." );
        if( strictFlagTime )
            m_botAction.sendArenaMessage( "Round 1 begins in 60 seconds.  All players will be warped at round start." );
        else
            m_botAction.sendArenaMessage( "Round 1 begins in 60 seconds.  PM me with !warp to warp into flagroom at round start. -" + m_botAction.getBotName() );

        flagTimeStarted = true;
        freq0Score = 0;
        freq1Score = 0;
        m_botAction.scheduleTask( new StartRoundTask(), 60000 );
    }


    /**
     * Toggles "strict" flag time mode in which all players are first warped
     * automatically into safe (must be set), and then warped into base.
     *
     * @param sender is the person issuing the command.
     */
    public void doStrictTimeCmd(String sender ) {
        if( strictFlagTime ) {
            strictFlagTime = false;
            if( flagTimeStarted )
                m_botAction.sendSmartPrivateMessage(sender, "Strict flag time mode disabled.  Changes will go into effect next round.");
            else
                m_botAction.sendSmartPrivateMessage(sender, "Strict flag time mode disabled.  !startflagtime <minutes> to begin a normal flag time game.");
        } else {
            strictFlagTime = true;
            if(flagTimeStarted) {
                m_botAction.sendSmartPrivateMessage(sender, "Strict flag time mode enabled.  All players will be warped into base next round.");
            } else {
                m_botAction.sendSmartPrivateMessage(sender, "Strict flag time mode enabled.  !startflagtime <minutes> to begin a strict flag time game.");
            }
        }
    }


    /**
     * Ends "flag time" mode.
     *
     * @param sender is the person issuing the command.
     */
    public void doStopTimeCmd(String sender )
    {
        if(!flagTimeStarted)
            throw new RuntimeException( "Flag Time mode is not currently running." );

        m_botAction.sendSmartPrivateMessage( sender, "Flag Time mode disabled." );
        m_botAction.sendArenaMessage( "Flag Time mode has been disabled." );

        try {
            flagTimer.endGame();
            m_botAction.cancelTask(flagTimer);
            m_botAction.cancelTask(intermissionTimer);
            m_botAction.cancelTask(startTimer);
        } catch (Exception e ) {
        }

        flagTimeStarted = false;
        strictFlagTime = false;
    }


    /**
     * Displays info about time remaining in flag time round, if applicable.
     *
     * @param sender is the person issuing the command.
     */
    public void doTimeCmd( String sender )
    {
        if( flagTimeStarted )
            if( flagTimer != null )
                flagTimer.sendTimeRemaining( sender );
            else
                throw new RuntimeException( "Flag time mode is just about to start." );
        else
            throw new RuntimeException( "Flag time mode is not currently running." );
    }


    /**
     * Adds player to next round's warp list.
     *
     * @param sender is the person issuing the command.
     */
    public void doWarpCmd( String sender )
    {
        if(!flagTimeStarted)
            throw new RuntimeException( "Flag Time mode is not currently running." );
        if( strictFlagTime )
            throw new RuntimeException( "You do not need to !warp in Strict Flag Time mode.  You will automatically be warped." );

        if( warpPlayers.contains( sender ) ) {
            warpPlayers.remove( sender );
            m_botAction.sendSmartPrivateMessage( sender, "You will NOT be warped inside FR at every round start.  !warp again to turn back on." );
        } else {
            warpPlayers.add( sender );
            m_botAction.sendSmartPrivateMessage( sender, "You will be warped inside FR at every round start.  !warp again to turn off." );
        }
    }


    /**
     * Logs the bot off if not enabled.
     *
     * @param sender is the person issuing the command.
     * @throws RuntimeException if the bot is running pure pub settings.
     */
    public void doDieCmd(String sender)
    {
        String currentArena = m_botAction.getArenaName();

        if(started)
            throw new RuntimeException("Bot is currently running pure pub settings in " + currentArena + ".  Please !Stop before trying to die.");

        m_botAction.sendSmartPrivateMessage(sender, "Bot logging off.");
        m_botAction.scheduleTask(new DieTask(), 100);
    }


    /**
     * Lists any ship restrictions in effect.
     *
     * @param sender is the person issuing the command.
     */
    public void doListShipsCmd(String sender) {
        int weight;
        m_botAction.sendSmartPrivateMessage(sender, "Ship limitations/restrictions (if any):" );
        for( int i = 1; i < 9; i++ ) {
            weight = shipWeights.get( i ).intValue();
            if( weight == 0 )
                m_botAction.sendSmartPrivateMessage(sender, Tools.shipName( i ) + "s disabled." );
            else if( weight > 1 )
                m_botAction.sendSmartPrivateMessage(sender, Tools.shipName( i ) + "s limited to 1/" + weight + " of the size of a frequency (but 1 always allowed).");
        }
    }


    /**
     * Sets a given ship to a particular restriction.
     *
     * @param sender is the person issuing the command.
     */
    public void doSetCmd(String sender, String argString) {
        String[] args = argString.split(" ");
        if( args.length != 2 )
            throw new RuntimeException("Usage: !set <ship#> <weight#>");

        try {
            Integer ship = Integer.valueOf(args[0]);
            ship = ship.intValue();
            Integer weight = Integer.valueOf(args[1]);
            if( ship > 0 && ship < 9 ) {
                if( weight >= 0 ) {
                    shipWeights.set( ship.intValue(), weight );
                    if( weight == 0 )
                        m_botAction.sendSmartPrivateMessage(sender, Tools.shipName( ship ) + "s: disabled." );
                    if( weight == 1 )
                        m_botAction.sendSmartPrivateMessage(sender, Tools.shipName( ship ) + "s: unrestricted." );
                    if( weight > 1 )
                        m_botAction.sendSmartPrivateMessage(sender, Tools.shipName( ship ) + "s: limited to 1/" + weight + " of the size of a frequency (but 1 always allowed).");
                    specRestrictedShips();
                } else
                    throw new RuntimeException("Weight must be >= 0.");
            } else
                throw new RuntimeException("Invalid ship number.");
        } catch (Exception e) {
            throw new RuntimeException("Usage: !set <ship#> <weight#>");
        }
    }


    /**
     * Shows who on the team is in which ship.
     *
     * @param sender is the person issuing the command.
     */
    public void doShowTeamCmd(String sender) {
        Player p = m_botAction.getPlayer(sender);
        if( p == null )
            throw new RuntimeException("Can't find you.  Please report this to staff.");
        if( p.getShipType() == 0 )
            throw new RuntimeException("You must be in a ship for this command to work.");
        ArrayList<Vector<String>>  team = getTeamData( p.getFrequency() );
        for(int i = 0; i < 9; i++ ) {
            if( team.get(i).size() > 0) {
                String text = Tools.formatString(Tools.shipName(i) + "s", 11);
                text += "(" + team.get(i).size() + "):  ";
                for( int j = 0; j < team.get(i).size(); j++) {
                   text += team.get(i).get(j) + "  ";
                }
                m_botAction.sendSmartPrivateMessage(sender, text);
            }
        }
    }


    /**
     * Collects names of players on a freq into a Vector array by ship.
     * @param freq Frequency to collect info on
     * @return Vector array containing player names on given freq
     */
    public ArrayList<Vector<String>> getTeamData( int freq ) {
        ArrayList<Vector<String>> team = new ArrayList<Vector<String>>();
        // 8 ships plus potential spectators
        for( int i = 0; i < 9; i++ ) {
            team.add( new Vector<String>() );
        }
        Iterator i = m_botAction.getFreqPlayerIterator(freq);
        while( i.hasNext() ) {
            Player p = (Player)i.next();
            team.get(p.getShipType()).add(p.getPlayerName());
        }
        return team;
    }


    /**
     * Displays a help message depending on access level.
     *
     * @param sender is the person issuing the command.
     */
    public void doHelpCmd(String sender)
    {
        String[] helpMessage =
        {
                "!go <ArenaName>         -- Moves the bot to <ArenaName>.",
                "!start                  -- Starts pure pub settings.",
                "!stop                   -- Stops pure pub settings.",
                "!privfreqs              -- Toggles Private Frequencies.",
                "!starttime #            -- Starts Flag Time mode (a team wins",
                "                           with # consecutive min of flagtime).",
                "!stoptime               -- Ends Flag Time mode.",
                "!stricttime             -- Toggles strict mode (all players warped)",
                "!listships              -- Lists all current ship restrictions.",
                "!set <ship> <#>         -- Sets <ship> to restriction <#>.",
                "                           0=disabled; 1=any amount; other=weighted:",
                "                           2 = 1/2 of freq can be this ship, 5 = 1/5, ...",
                "!time                   -- Provides time remaining in Flag Time mode.",
                "!team                   -- Shows which ships team members are in.",
                "!die                    -- Logs the bot off of the server.",
        };

        String[] playerHelpMessage =
        {
                "Hi.  I'm a bot designed to enforce pure pub rules.",
                "I restrict ships, manage private frequencies, and run Flag Time mode.",
                "Commands:",
                "!team                   -- Tells you which ships your team members are in.",
                "!listships              -- Lists all current ship restrictions.",
                "!time                   -- Provides time remaining when Flag Time mode.",
                "!warp                   -- Warps you into flagroom at start of next round (flag time)",
        };

        if( opList.isHighmod( sender ) )
            m_botAction.smartPrivateMessageSpam(sender, helpMessage);
        else
            m_botAction.smartPrivateMessageSpam(sender, playerHelpMessage);
    }



    /* **********************************  SUPPORT METHODS  ************************************ */

    /**
     * This method returns the name of the player that sent the message regardless
     * of whether or not the message is a remote private message or a private
     * message.
     *
     * @param event is the message event.
     * @return the name of the sender is returned.  If the name of the sender
     * cannot be determined then null is returned.
     */
    private String getSender(Message event)
    {
        if(event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
            return event.getMessager();

        int senderID = event.getPlayerID();
        return m_botAction.getPlayerName(senderID);
    }


    /**
     * This method checks to see if a player is in a restricted ship, or the
     * weight for the given ship has been reached.  If either is true, then the
     * player is specced.
     *
     * Weights can be thought of as a denominator (bottom number) of a fraction,
     * the fraction saying how much of the freq can be made up of ships of this
     * type.  If the weight is 0, no ships of a type are allowed.  Weight of 1
     * gives a fraction of 1/1, or a whole -- the entire freq can be made up of
     * this ship.  Following that, 2 is half, 3 is a third, 4 is a fourth, etc.
     * Play with what weight seems right to you.
     *
     * Note that even with a very small freq, if a weight is 1 or greater, 1 ship
     * of this type is ALWAYS allowed.
     *
     * Value for ship "weights":
     *
     * 0  - No ships of this type allowed
     * 1  - Unlimited number of ships of this type are allowed
     * #  - If the number of current ships of the type on this frequency is
     *      greater than the total number of people on the frequency divided
     *      by this number (ships of this type > total ships / weight), then the
     *      ship is not allowed.  Exception to this rule is if the player is the
     *      only one on the freq currently in the ship.
     *
     * @param playerName is the player to be checked.
     * @param specMessage enables the spec message.
     */
    private void checkPlayer(int playerID)
    {
        Player player = m_botAction.getPlayer(playerID);
        if( player == null )
            return;

        int weight = shipWeights.get(player.getShipType()).intValue();

        // If weight is 1, unlimited number of that shiptype is allowed.  (Spec is also set to 1.)
        if( weight == 1 )
            return;

        // If weight is 0, ship is completely restricted.
        if( weight == 0 ) {
            m_botAction.spec(playerID);
        	m_botAction.spec(playerID);
       	    m_botAction.sendSmartPrivateMessage(m_botAction.getPlayerName(playerID), "That ship has been restricted in this arena.  Please choose another, or type ?arena to select another arena.");
       	    return;
        }

        // For all other weights, we must decide whether they can play based on the
        // number of people on freq who are also using the ship.
        Iterator i = m_botAction.getPlayingPlayerIterator();
        if( i == null)
            return;

        int freqTotal = 0;
        int numShipsOfType = 0;

        Player dummy;
        while( i.hasNext() ) {
            dummy = (Player)i.next();
            if( dummy != null) {
                if( dummy.getFrequency() == player.getFrequency() ) {
                    freqTotal++;
                    if( dummy.getShipType() == player.getShipType() )
                        numShipsOfType++;
                }
            }
        }

    	// Free pass if you're the only one on the freq, regardless of weight.
    	if( numShipsOfType <= 1 )
    	    return;

    	if( freqTotal == 0 ) {
            m_botAction.sendSmartPrivateMessage(m_botAction.getPlayerName(playerID), "Problem locating your freq!  Please contact a mod with ?help.");
            return;
    	}

        if( numShipsOfType > freqTotal / weight ) {
            // If unlimited spiders are allowed, set them to spider; else spec
            if( shipWeights.get(3).intValue() == 1 ) {
                m_botAction.setShip(playerID, 3);
                m_botAction.sendSmartPrivateMessage(m_botAction.getPlayerName(playerID), "There are too many ships of that kind (" + (numShipsOfType - 1) + "), or not enough people on the freq to allow you to play that ship.");
            } else {
                m_botAction.spec(playerID);
                m_botAction.spec(playerID);
                m_botAction.sendSmartPrivateMessage(m_botAction.getPlayerName(playerID), "There are too many ships of that kind (" + (numShipsOfType - 1) + "), or not enough people on the freq to allow you to play that ship.  Please choose another.");
            }
        }
    }


    /**
     * Removes a playerName from the freq tracking lists.
     *
     * @param playerName is the name of the player to remove.
     */
    private void removeFromLists(String playerName)
    {
        String lowerName = playerName.toLowerCase();

        freq0List.remove(lowerName);
        freq1List.remove(lowerName);
    }


    /**
     * Removes a playerName from the warp list.
     */
    private void removeFromWarpList(String playerName)
    {
        warpPlayers.remove( playerName );
    }


    /**
     * Sets a player to a freq and updates the freq lists.
     *
     * @param playerName is the name of the player to add.
     * @param freq is the new freq.
     */
    private void addToLists(String playerName, int freq)
    {
        String lowerName = playerName.toLowerCase();

        if(freq == FREQ_0)
            freq0List.add(lowerName);
        if(freq == FREQ_1)
            freq1List.add(lowerName);
    }


    /**
     * Checks to see if a player is on a private freq.  If they are then
     * they are changed to the pub freq with the fewest number of players.
     *
     * @param Player player is the player to check.
     * @param changeMessage is true if a changeMessage will be displayed.
     */
    private void checkFreq(int playerID, int freq, boolean changeMessage)
    {
        Player player = m_botAction.getPlayer(playerID);
        String playerName = player.getPlayerName();
        if( player == null )
            return;

        int ship = player.getShipType();
        int newFreq = freq;

        if( playerName == null )
            return;

        removeFromLists(playerName);

        if(ship != SPEC)
        {
            if(player != null && freq != FREQ_0 && freq != FREQ_1)
            {
                if(freq0List.size() <= freq1List.size())
                    newFreq = FREQ_0;
                else
                    newFreq = FREQ_1;
                if(changeMessage)
                    m_botAction.sendSmartPrivateMessage(playerName, "Private Frequencies are currently disabled.  You have been placed on a public Frequency.");
                m_botAction.setFreq(playerName, newFreq);
            }
            addToLists(playerName, newFreq);
        }
    }


    /**
     * Specs all ships in the arena that are over the weighted restriction limit.
     */
    private void specRestrictedShips()
    {
        Iterator iterator = m_botAction.getPlayingPlayerIterator();
        Player player;

        while(iterator.hasNext())
        {
            player = (Player) iterator.next();
            checkPlayer(player.getPlayerID());
        }
    }


    /**
     * Fills the freq lists for freqs 1 and 0.
     */
    private void fillFreqLists()
    {
        Iterator iterator = m_botAction.getPlayingPlayerIterator();
        Player player;
        String lowerName;

        freq0List.clear();
        freq1List.clear();
        while(iterator.hasNext())
        {
            player = (Player) iterator.next();
            lowerName = player.getPlayerName().toLowerCase();
            if(player.getFrequency() == FREQ_0)
                freq0List.add(lowerName);
            if(player.getFrequency() == FREQ_1)
                freq1List.add(lowerName);
        }
    }


    /**
     * Fixes the freq of each player.
     */
    private void fixFreqs()
    {
        Iterator iterator = m_botAction.getPlayingPlayerIterator();
        Player player;

        fillFreqLists();
        while(iterator.hasNext())
        {
            player = (Player) iterator.next();
            checkFreq(player.getPlayerID(), player.getFrequency(), false);
        }
    }


    /**
     * Starts the bot with CFG-specified setup commands.
     */
    public void startBot()
    {
    	String commands[] = m_botAction.getBotSettings().getString(m_botAction.getBotName() + "Setup").split(",");
    	for(int k = 0; k < commands.length; k++) {
    		handleCommand(m_botAction.getBotName(), commands[k]);
		}
    }


    /* **********************************  FLAGTIME METHODS  ************************************ */
    /**
     * Starts a game of flag time mode.
     */
    private void doStartRound() {
        if(!flagTimeStarted)
            return;

        try {
            flagTimer.endGame();
            m_botAction.cancelTask(flagTimer);
        } catch (Exception e ) {
        }

        flagTimer = new FlagCountTask();
        m_botAction.scheduleTaskAtFixedRate( flagTimer, 100, 1000);
    }


    /**
     * Displays rules and pauses for intermission.
     */
    private void doIntermission() {
        if(!flagTimeStarted)
            return;

        int roundNum = freq0Score + freq1Score + 1;

        String roundTitle = "";
        switch( roundNum ) {
        case 1:
            m_botAction.sendArenaMessage( "Object: Hold flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win a round.  Best " + ( MAX_FLAGTIME_ROUNDS + 1) / 2 + " of "+ MAX_FLAGTIME_ROUNDS + " wins the game." );
            roundTitle = "The next game";
            break;
        case MAX_FLAGTIME_ROUNDS:
            roundTitle = "Final Round";
            break;
        default:
            roundTitle = "Round " + roundNum;
        }

        m_botAction.sendArenaMessage( roundTitle + " begins in " + getTimeString( INTERMISSION_SECS ) + ".  (Score: " + freq0Score + " - " + freq1Score + ")" + (strictFlagTime?"":(" Type :" + m_botAction.getBotName() +":!warp to warp into FR.")) );

        m_botAction.cancelTask(startTimer);

        startTimer = new StartRoundTask();
        m_botAction.scheduleTask( startTimer, INTERMISSION_SECS * 1000 );
    }


    /**
     * Ends a round of Flag Time mode & awards prizes.
     * After, sets up an intermission, followed by a new round.
     */
    private void doEndRound( ) {
        if( !flagTimeStarted || flagTimer == null )
            return;

        HashSet <String>MVPs = new HashSet<String>();
        boolean gameOver     = false;       // Game over, man.. game over!
        int flagholdingFreq  = flagTimer.getHoldingFreq();
        int maxScore         = (MAX_FLAGTIME_ROUNDS + 1) / 2;  // Score needed to win
        int secs = flagTimer.getTotalSecs();
        int mins = secs / 60;
        int weight = (secs * 3 ) / 60;

        try {

            // Incremental bounty bonuses
            if( mins >= 90 )
                weight += 150;
            else if( mins >= 60 )
                weight += 100;
            else if( mins >= 30 )
                weight += 45;
            else if( mins >= 15 )
                weight += 20;


            if( flagholdingFreq == 0 || flagholdingFreq == 1 ) {
                if( flagholdingFreq == 0 )
                    freq0Score++;
                else
                    freq1Score++;

                if( freq0Score >= maxScore || freq1Score >= maxScore ) {
                    gameOver = true;
                } else {
                    int roundNum = freq0Score + freq1Score;
                    m_botAction.sendArenaMessage( "END OF ROUND " + roundNum + ": Freq " + flagholdingFreq + " wins after " + getTimeString( flagTimer.getTotalSecs() ) +
                            " (" + weight + " bounty bonus)  Score: " + freq0Score + " - " + freq1Score, 1 );
                }

            } else {
                if( flagholdingFreq < 100 )
                    m_botAction.sendArenaMessage( "END ROUND: Freq " + flagholdingFreq + " wins the round after " + getTimeString( flagTimer.getTotalSecs() ) + " (" + weight + " bounty bonus)", 1 );
                else
                    m_botAction.sendArenaMessage( "END ROUND: A private freq wins the round after " + getTimeString( flagTimer.getTotalSecs() ) + " (" + weight + " bounty bonus)", 1 );
            }

            int special = 0;
            // Special prizes for longer battles (add more if you think of any!)
            if( mins > 15 ) {
                Random r = new Random();
                int chance = r.nextInt(100);

                if( chance == 99 ) {
                    special = 8;
                } else if( chance == 98 ) {
                    special = 7;
                } else if( chance >= 94 ) {
                    special = 6;
                } else if( chance >= 90 ) {
                    special = 5;
                } else if( chance >= 75 ) {
                    special = 4;
                } else if( chance >= 60 ) {
                    special = 3;
                } else if( chance >= 35 ) {
                    special = 2;
                } else {
                    special = 1;
                }
            }

            Iterator iterator = m_botAction.getPlayingPlayerIterator();
            Player player;
            while(iterator.hasNext()) {
                player = (Player) iterator.next();
                if( player != null ) {
                    if(player.getFrequency() == flagholdingFreq ) {
                        String playerName = player.getPlayerName();

                        Integer i = playerTimes.get( playerName );

                        if( i != null ) {
                            // Calculate amount of time actually spent on freq

                            int timeOnFreq = secs - i.intValue();
                            int percentOnFreq = (int)( ( (float)timeOnFreq / (float)secs ) * 100 );
                            int modbounty = (int)(weight * ((float)percentOnFreq / 100));

                            if( percentOnFreq == 100 ) {
                                MVPs.add( playerName );
                                m_botAction.sendPrivateMessage( playerName, "For staying with the same freq and ship the entire match, you are an MVP and receive the full bonus: " + modbounty );
                                int grabs = flagTimer.getFlagGrabs( playerName );
                                if( special == 4 ) {
                                    m_botAction.sendPrivateMessage( playerName, "You also receive an additional " + weight + " bounty as a special prize!" );
                                    modbounty *= 2;
                                }
                                if( grabs != 0 ) {
                                    modbounty += modbounty * (grabs / 10);
                                    m_botAction.sendPrivateMessage( playerName, "For your " + grabs + " flag grabs, you also receive an additional " + grabs + "0% bounty, for a total of " + modbounty );
                                }

                            } else {
                                m_botAction.sendPrivateMessage( playerName, "You were with the same freq and ship for the last " + getTimeString(timeOnFreq) + ", and receive " + percentOnFreq  + "% of the bounty reward: " + modbounty );
                            }

                            m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize " + modbounty);
                        }

                        if( MVPs.contains( playerName ) ) {
                            switch( special ) {
                            case 1:  // "Refreshments" -- replenishes all essentials + gives anti
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #6");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #15");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #20");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #21");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #21");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #21");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #22");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #27");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #28");
                                break;
                            case 2:  // "Full shrap"
                                for(int j = 0; j < 5; j++ )
                                    m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #19");
                                break;
                            case 3:  // "Trophy" -- decoy given
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #23");
                                break;
                            case 4:  // "Double bounty reward"
                                break;
                            case 5:  // "Triple trophy" -- 3 decoys
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #23");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #23");
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #23");
                                break;
                            case 6:  // "Techno Dance Party" -- plays victory music :P
                                break;
                            case 7:  // "Sore Loser's Revenge" -- engine shutdown!
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #14");
                                break;
                            case 8:  // "Bodyguard" -- shields
                                m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize #18");
                                break;
                            }
                        }
                    }
                }
            }

            String[] leaderInfo = flagTimer.getTeamLeader( MVPs );
            if( leaderInfo.length != 3 )
                return;
            String name, MVplayers = "";
            MVPs.remove( leaderInfo[0] );
            if( !leaderInfo[2].equals("") ) {
                String otherleaders[] = leaderInfo[2].split(", ");
                for( int j = 0; j<otherleaders.length; j++ )
                    MVPs.remove( otherleaders[j] );
            }
            Iterator i = MVPs.iterator();

            if( i.hasNext() ) {
                switch( special ) {
                case 1:  // "Refreshments" -- replenishes all essentials + gives anti
                    m_botAction.sendArenaMessage( "Prize for MVPs: Refreshments! (+ AntiWarp for all loyal spiders)" );
                    break;
                case 2:  // "Full shrap"
                    m_botAction.sendArenaMessage( "Prize for MVPs: Full shrap!" );
                    break;
                case 3:  // "Trophy" -- decoy given
                    m_botAction.sendArenaMessage( "Prize for MVPs: Life-size Trophies of Themselves!" );
                    break;
                case 4:  // "Double bounty reward"
                    m_botAction.sendArenaMessage( "Prize for MVPs: Double Bounty Bonus!  (MVP bounty: " + weight * 2 + ")" );
                    break;
                case 5:  // "Triple trophy" -- 3 decoys
                    m_botAction.sendArenaMessage( "Prize for MVPs: The Triple Platinum Trophy!" );
                    break;
                case 6:  // "Techno Dance Party" -- plays victory music :P
                    m_botAction.sendArenaMessage( "Prize for MVPs: Ultimate Techno Dance Party!", 102);
                    break;
                case 7:  // "Sore Loser's Revenge" -- engine shutdown!
                    m_botAction.sendArenaMessage( "Prize for MVPs: Sore Loser's REVENGE!" );
                    break;
                case 8:  // "Bodyguard" -- shields
                    m_botAction.sendArenaMessage( "Prize for MVPs: Personal Body-Guard!" );
                    break;
                }

                MVplayers = (String)i.next();
                int grabs = flagTimer.getFlagGrabs(MVplayers);
                if( grabs > 0 )
                    MVplayers += "(" + grabs + ")";
            }
            int grabs = 0;
            while( i.hasNext() ) {
                name = (String)i.next();
                grabs = flagTimer.getFlagGrabs(name);
                if( grabs > 0 )
                    MVplayers = MVplayers + ", " + name + "(" + grabs + ")";
                else
                    MVplayers = MVplayers + ", " + name;
            }

            if( leaderInfo[0] != "" ) {
                if( leaderInfo[2] == "" )
                    m_botAction.sendArenaMessage( "Team Leader was " + leaderInfo[0] + "!  (" + leaderInfo[1] + " flag claim(s) + MVP)" );
                else
                    m_botAction.sendArenaMessage( "Team Leaders were " + leaderInfo[2] + "and " + leaderInfo[0] + "!  (" + leaderInfo[1] + " flag claim(s) + MVP)" );
            }
            if( MVplayers != "" )
                m_botAction.sendArenaMessage( "MVPs (+ claims): " + MVplayers );

        } catch(Exception e) {
            Tools.printStackTrace( e );
        }

        int intermissionTime = 10000;

        if( gameOver ) {
            intermissionTime = 20000;

            int diff = 0;
            String winMsg = "";
            if( freq0Score >= maxScore ) {
                if( freq1Score == 0 )
                    diff = -1;
                else
                    diff = freq0Score - freq1Score;
            } else if( freq1Score >= maxScore ) {
                if( freq0Score == 0 )
                    diff = -1;
                else
                    diff = freq1Score - freq0Score;
            }
            switch( diff ) {
            case -1:
                winMsg = " for their masterful victory!";
                break;
            case 1:
                winMsg = " for their close win!";
                break;
            case 2:
                winMsg = " for a well-executed victory!";
                break;
            default:
                winMsg = " for their win!";
                break;
            }
            m_botAction.sendArenaMessage( "GAME OVER!  Freq " + flagholdingFreq + " has won the game after " + getTimeString( flagTimer.getTotalSecs() ) +
                    " (" + weight + " bounty bonus)  Final score: " + freq0Score + " - " + freq1Score, 2 );
            m_botAction.sendArenaMessage( "Give congratulations to FREQ " + flagholdingFreq + winMsg );

            freq0Score = 0;
            freq1Score = 0;
        }


        try {
            flagTimer.endGame();
            m_botAction.cancelTask(flagTimer);
            m_botAction.cancelTask(intermissionTimer);
        } catch (Exception e ) {
        }

        intermissionTimer = new IntermissionTask();
        m_botAction.scheduleTask( intermissionTimer, intermissionTime );
    }


    /**
     * Adds all players to the hashmap which stores the time, in flagTimer time,
     * when they joined their freq.
     */
    public void setupPlayerTimes() {
        playerTimes = new HashMap<String,Integer>();

        Iterator i = m_botAction.getPlayingPlayerIterator();
        Player player;

        try {
            while( i.hasNext() ) {
                player = (Player)i.next();
                playerTimes.put( player.getPlayerName(), new Integer(0) );
            }
        } catch (Exception e) {
        }
    }


    /**
     * Formats an integer time as a String.
     * @param time Time in seconds.
     * @return Formatted string in 0:00 format.
     */
    public String getTimeString( int time ) {
        if( time <= 0 ) {
            return "0:00";
        } else {
            int minutes = time / 60;
            int seconds = time % 60;
            return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        }
    }


    /**
     * Warps all players who have PMed with !warp into FR at start.
     * Ensures !warpers on freqs are warped all to 'their' side, but not predictably.
     */
    private void warpPlayers() {
        Iterator i;

        if( strictFlagTime )
            i = m_botAction.getPlayingPlayerIterator();
        else
            i = warpPlayers.iterator();

        Random r = new Random();
        int rand;
        Player p;
        String pname;
        LinkedList <String>nullPlayers = new LinkedList<String>();

        int randomside = r.nextInt( 2 );

        while( i.hasNext() ) {
            if( strictFlagTime ) {
                p = (Player)i.next();
                pname = p.getPlayerName();
            } else {
                pname = (String)i.next();
                p = m_botAction.getPlayer( pname );
            }

            if( p != null ) {
                if( strictFlagTime )
                    rand = 0;           // Warp freqmates to same spot in strict mode.
                                        // The warppoints @ index 0 must be set up
                                        // to default/earwarps for this to work properly.
                else
                    rand = r.nextInt( NUM_WARP_POINTS_PER_SIDE );
                if( p.getFrequency() % 2 == randomside )
                    doPlayerWarp( pname, warpPtsLeftX[rand], warpPtsLeftY[rand] );
                else
                    doPlayerWarp( pname, warpPtsRightX[rand], warpPtsRightY[rand] );
            } else {
                if( !strictFlagTime ) {
                    nullPlayers.add( pname );
                }
            }
        }

        if( ! nullPlayers.isEmpty() ) {
            i = nullPlayers.iterator();
            while( i.hasNext() ) {
                warpPlayers.remove( (String)i.next() );
            }
        }
    }


    /**
     * In Strict Flag Time mode, warp all players to a safe 10 seconds before
     * starting.  This gives a semi-official feeling to the game, and resets
     * all mines, etc.
     */
    private void safeWarp() {
        // Prevent pre-laid mines and portals in strict flag time by setting to WB and back again (slightly hacky)
        HashMap<String,Integer> players = new HashMap<String,Integer>();
        HashMap<String,Integer> bounties = new HashMap<String,Integer>();
        Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
        Player p;
        while( it.hasNext() ) {
            p = it.next();
            if( p != null ) {
                if( p.getShipType() == Tools.Ship.SHARK || p.getShipType() == Tools.Ship.TERRIER || p.getShipType() == Tools.Ship.LEVIATHAN ) {
                    players.put( p.getPlayerName(), new Integer(p.getShipType()) );
                    bounties.put( p.getPlayerName(), new Integer(p.getBounty()) );
                    m_botAction.setShip(p.getPlayerName(), 1);
                }
            }
        }
        Iterator<String> it2 = players.keySet().iterator();
        String name;
        Integer ship, bounty;
        while( it2.hasNext() ) {
            name = it2.next();
            ship = players.get(name);
            bounty = bounties.get(name);
            if( ship != null )
                m_botAction.setShip( name, ship.intValue() );
            if( bounty != null )
                m_botAction.giveBounty( name, bounty.intValue() - 3 );
        }

        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
        while( i.hasNext() ) {
            p = i.next();
            if( p != null ) {
                if( p.getFrequency() % 2 == 0 )
                    m_botAction.warpTo( p.getPlayerID(), SAFE_LEFT_X, SAFE_LEFT_Y );
                else
                    m_botAction.warpTo( p.getPlayerID(), SAFE_RIGHT_X, SAFE_RIGHT_Y );
            }
        }
    }


    /**
     * Warps a player within a radius of 2 tiles to provided coord.
     *
     * @param playerName
     * @param xCoord
     * @param yCoord
     * @param radius
     * @author Cpt.Guano!
     */
    private void doPlayerWarp(String playerName, int xCoord, int yCoord ) {
        int radius = 2;
        double randRadians;
        double randRadius;
        int xWarp = -1;
        int yWarp = -1;

        randRadians = Math.random() * 2 * Math.PI;
        randRadius = Math.random() * radius;
        xWarp = calcXCoord(xCoord, randRadians, randRadius);
        yWarp = calcYCoord(yCoord, randRadians, randRadius);

        m_botAction.warpTo(playerName, xWarp, yWarp);
    }


    private int calcXCoord(int xCoord, double randRadians, double randRadius)
    {
        return xCoord + (int) Math.round(randRadius * Math.sin(randRadians));
    }


    private int calcYCoord(int yCoord, double randRadians, double randRadius)
    {
        return yCoord + (int) Math.round(randRadius * Math.cos(randRadians));
    }


    /* **********************************  TIMERTASK CLASSES  ************************************ */

    /**
     * This private class logs the bot off.  It is used to give a slight delay
     * to the log off process.
     */
    private class DieTask extends TimerTask
    {

        /**
         * This method logs the bot off.
         */
        public void run()
        {
            objs.hideAllObjects();
            m_botAction.setObjects();
            m_botAction.die();
        }
    }


    /**
     * This private class starts the round.
     */
    private class StartRoundTask extends TimerTask {

        /**
         * Starts the round when scheduled.
         */
        public void run() {
            doStartRound();
        }
    }


    /**
     * This private class provides a pause before starting the round.
     */
    private class IntermissionTask extends TimerTask {

        /**
         * Starts the intermission/rule display when scheduled.
         */
        public void run() {
            doIntermission();
        }
    }


    /**
     * This private class counts the consecutive flag time an individual team racks up.
     * Upon reaching the time needed to win, it fires the end of the round.
     */
    private class FlagCountTask extends TimerTask {
        int flagHoldingFreq, flagClaimingFreq;
        int secondsHeld, totalSecs, claimSecs, preTimeCount;
        int claimerID;
        boolean isStarted, isRunning, isBeingClaimed;
        HashMap <String,Integer>flagClaims;

        /**
         * FlagCountTask Constructor
         */
        public FlagCountTask() {
            flagHoldingFreq = -1;
            secondsHeld = 0;
            totalSecs = 0;
            claimSecs = 0;
            isStarted = false;
            isRunning = false;
            isBeingClaimed = false;
            flagClaims = new HashMap<String,Integer>();
        }

        /**
         * This method is called by the FlagClaimed event, and tracks who currently
         * has or is in the process of claiming the flag.  While the flag can physically
         * be claimed in the game, 3 seconds are needed to claim it for the purpose of
         * the game.
         * @param freq Frequency of flag claimer
         * @param pid PlayerID of flag claimer
         */
        public void flagClaimed( int freq, int pid ) {
            if( isRunning == false || freq == -1 )
                return;

            // Return the flag back to the team that had it if the claim attempt
            // is unsuccessful (countered by the holding team)
            if( freq == flagHoldingFreq ) {
                isBeingClaimed = false;
                claimSecs = 0;
                return;
            }

            if( freq != flagHoldingFreq ) {
                if( (!isBeingClaimed) || (isBeingClaimed && freq != flagClaimingFreq) ) {
                    claimerID = pid;
                    flagClaimingFreq = freq;
                    isBeingClaimed = true;
                    claimSecs = 0;
                }
            }
        }

        /**
         * Assigns flag (internally) to the claiming frequency.
         *
         */
        public void assignFlag() {
            flagHoldingFreq = flagClaimingFreq;

            int remain = (flagMinutesRequired * 60) - secondsHeld;


            Player p = m_botAction.getPlayer( claimerID );
            if( p != null ) {

                addFlagClaim( p.getPlayerName() );

                if( remain < 60 ) {
                    if( remain < 4 )
                        m_botAction.sendArenaMessage( "INCONCIEVABLE!!: " + p.getPlayerName() + " claims flag for " + (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "priv. freq" ) + " with just " + remain + " second" + (remain == 1 ? "" : "s") + " left!", 7 );
                    else if( remain < 11 )
                        m_botAction.sendArenaMessage( "AMAZING!: " + p.getPlayerName() + " claims flag for " + (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "priv. freq" ) + " with just " + remain + " sec. left!" );
                    else if( remain < 25 )
                        m_botAction.sendArenaMessage( "SAVE!: " + p.getPlayerName() + " claims flag for " + (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "priv. freq" ) + " with " + remain + " sec. left!" );
                    else
                        m_botAction.sendArenaMessage( "Save: " + p.getPlayerName() + " claims flag for " + (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "priv. freq" ) + " with " + remain + " sec. left." );
                }
            }

            isBeingClaimed = false;
            flagClaimingFreq = -1;
            secondsHeld = 0;

        }

        /**
         * Increments a count for player claiming the flag.
         * @param name Name of player.
         */
        public void addFlagClaim( String name ) {
            Integer count = flagClaims.get( name );
            if( count == null ) {
                flagClaims.put( name, new Integer(1) );
            } else {
                flagClaims.remove( name );
                flagClaims.put( name, new Integer( count.intValue() + 1) );
            }
        }

        /**
         * Ends the game for the timer's internal purposes.
         */
        public void endGame() {
            objs.hideAllObjects();
            m_botAction.setObjects();
            isRunning = false;
        }

        /**
         * Sends time info to requested player.
         * @param name Person to send info to
         */
        public void sendTimeRemaining( String name ) {
            m_botAction.sendSmartPrivateMessage( name, getTimeInfo() );
        }

        /**
         * @return True if a game is currently running; false if not
         */
        public boolean isRunning() {
            return isRunning;
        }

        /**
         * Gives the name of the top flag claimers out of the MVPs.  If there is
         * a tie, does not care because it's only bragging rights anyway. :P
         * @return Array of size 2, index 0 being the team leader and 1 being # flaggrabs
         */
        public String[] getTeamLeader( HashSet<String> MVPs ) {
            String[] leaderInfo = {"", "", ""};
            HashSet <String>ties = new HashSet<String>();

            if( MVPs == null )
                return leaderInfo;
            try {
                Iterator<String> i = MVPs.iterator();
                Integer dummyClaim, highClaim = new Integer(0);
                String leader = "", dummyPlayer;

                while( i.hasNext() ) {
                    dummyPlayer = i.next();
                    dummyClaim = flagClaims.get( dummyPlayer );
                    if( dummyClaim != null ) {
                        if( dummyClaim.intValue() > highClaim.intValue() ) {
                            leader = dummyPlayer;
                            highClaim = dummyClaim;
                            ties.clear();
                        } else if ( dummyClaim.intValue() == highClaim.intValue() ) {
                            ties.add(dummyPlayer);
                        }
                    }
                }
                leaderInfo[0] = leader;
                leaderInfo[1] = highClaim.toString();
                i = ties.iterator();
                while( i.hasNext() )
                    leaderInfo[2] += i.next() + ", ";
                return leaderInfo;

            } catch (Exception e ) {
                Tools.printStackTrace( e );
                return leaderInfo;
            }

        }

        /**
         * Returns number of flag grabs for given player.
         * @param name Name of player
         * @return Flag grabs
         */
        public int getFlagGrabs( String name ) {
            Integer grabs = flagClaims.get( name );
            if( grabs == null )
                return 0;
            else
                return grabs;
        }

        /**
         * @return Time-based status of game
         */
        public String getTimeInfo() {
            int roundNum = freq0Score + freq1Score + 1;

            if( isRunning == false ) {
                if( roundNum == 1 )
                    return "Round 1 of a new game is just about to start.";
                else
                    return "We are currently in between rounds (round " + roundNum + " starting soon).  Score: " + freq0Score + " - " + freq1Score;
            }
            return "ROUND " + roundNum + " Stats: " + (flagHoldingFreq == -1 || flagHoldingFreq > 99 ? "?" : "Freq " + flagHoldingFreq ) + " holding for " + getTimeString(secondsHeld) + ", needs " + getTimeString( (flagMinutesRequired * 60) - secondsHeld ) + " more.  [Time: " + getTimeString( totalSecs ) + "]  Score: " + freq0Score + " - " + freq1Score;
        }

        /**
         * @return Total number of seconds round has been running.
         */
        public int getTotalSecs() {
            return totalSecs;
        }

        /**
         * @return Frequency that currently holds the flag
         */
        public int getHoldingFreq() {
            return flagHoldingFreq;
        }

        /**
         * Timer running once per second that handles the starting of a round,
         * displaying of information updates every 5 minutes, the flag claiming
         * timer, and total flag holding time/round ends.
         */
        public void run() {
            if( isStarted == false ) {
                int roundNum = freq0Score + freq1Score + 1;
                if( preTimeCount == 0 ) {
                    m_botAction.sendArenaMessage( "Next round begins in 10 seconds . . ." );
                    if( strictFlagTime )
                        safeWarp();
                }
                preTimeCount++;

                if( preTimeCount >= 10 ) {
                    isStarted = true;
                    isRunning = true;
                    m_botAction.sendArenaMessage( ( roundNum == MAX_FLAGTIME_ROUNDS ? "FINAL ROUND" : "ROUND " + roundNum) + " START!  Hold flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win the round.", 1 );
                    m_botAction.resetFlagGame();
                    setupPlayerTimes();
                    warpPlayers();
                    return;
                }
            }

            if( isRunning == false )
                return;

            totalSecs++;

            // Display mode info at 5 min increments, unless we are near the end of a game
            if( (totalSecs % (5 * 60)) == 0 && ( (flagMinutesRequired * 60) - secondsHeld > 30) ) {
                m_botAction.sendArenaMessage( getTimeInfo() );
            }

            do_updateTimer();

            if( isBeingClaimed ) {
                claimSecs++;
                if( claimSecs >= FLAG_CLAIM_SECS ) {
                    claimSecs = 0;
                    assignFlag();
                }
                return;
            }

            if( flagHoldingFreq == -1 )
                return;

            secondsHeld++;

            int flagSecsReq = flagMinutesRequired * 60;
            if( secondsHeld >= flagSecsReq ) {
                endGame();
                doEndRound();
            } else if( flagSecsReq - secondsHeld == 60 ) {
                m_botAction.sendArenaMessage( (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "Private freq" ) + " will win in 60 seconds." );
            } else if( flagSecsReq - secondsHeld == 10 ) {
                m_botAction.sendArenaMessage( (flagHoldingFreq < 100 ? "Freq " + flagHoldingFreq : "Private freq" ) + " will win in 10 seconds . . ." );
            }
        }

        /**
         * Runs the LVZ-based timer.
         */
        private void do_updateTimer() {
            int secsNeeded = flagMinutesRequired * 60 - secondsHeld;
            objs.hideAllObjects();
            int minutes = secsNeeded / 60;
            int seconds = secsNeeded % 60;
            if( minutes > 10 )
                objs.showObject( 10 + ((minutes - minutes % 10)/10) );
            objs.showObject( 20 + (minutes % 10) );
            objs.showObject( 30 + ((seconds - seconds % 10)/10) );
            objs.showObject( 40 + (seconds % 10) );
            m_botAction.setObjects();
        }
    }
}
