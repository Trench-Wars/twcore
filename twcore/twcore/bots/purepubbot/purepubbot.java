package twcore.bots.purepubbot;

import java.util.*;
import twcore.core.*;

public class purepubbot extends SubspaceBot
{
    
    public static final int SPEC = 0;
    public static final int FREQ_0 = 0;
    public static final int FREQ_1 = 1;
    private static final int FLAG_CLAIM_SECS = 4;		// Seconds it takes to fully claim a flag
    private static final int INTERMISSION_SECS = 90;	// Seconds between end of round and start of next
    private static final int NUM_WARP_POINTS_PER_SIDE = 6;		// Total number of warp points per side of FR
    
    private OperatorList opList;
    private HashSet freq0List;
    private HashSet freq1List;
    private HashMap playerTimes;
    private HashSet MVPs;
    private boolean started;
    private boolean privFreqs;
    private boolean flagTimeStarted;
    private boolean strictFlagTime;
    private FlagCountTask flagTimer;
    private StartRoundTask startTimer;
    private IntermissionTask intermissionTimer;
    private int flagMinutesRequired;
    private int freq0Score, freq1Score;
    
    boolean initLogin = true;
    private int initialPub;
    private String initialSpawn;    
    private Vector shipWeights;
    
    private int warpPtsLeftX[] = { 505, 502, 499, 491, 495, 487 }; 
    private int warpPtsLeftY[] = { 260, 267, 274, 279, 263, 255 };

    private int warpPtsRightX[] = { 519, 522, 525, 529, 533, 537 }; 
    private int warpPtsRightY[] = { 260, 267, 274, 263, 279, 255 };
    private static final int SAFE_LEFT_X = 306;
    private static final int SAFE_LEFT_Y = 482;
    private static final int SAFE_RIGHT_X = 717;
    private static final int SAFE_RIGHT_Y = 482;
    
    private LinkedList warpPlayers;
    
    
    /**
     * This method initializes the bot.
     *
     * @param botAction is the BotAction method for the bot.
     */   
    public purepubbot(BotAction botAction)
    {
        super(botAction);
        
        requestEvents();
        opList = m_botAction.getOperatorList();
        freq0List = new HashSet();
        freq1List = new HashSet();
        playerTimes = new HashMap();
        MVPs = new HashSet();
        started = false;
        privFreqs = true;
        flagTimeStarted = false;
        strictFlagTime = false;
        warpPlayers = new LinkedList();
        shipWeights = new Vector();
    }
    
    
    /**
     * This method requests all of the appropriate events.
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
     * Retreive all necessary settings for the bot to operate.
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
     * Requests arena list to move to appropriate pub automatically, if this is the first arena joined.
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

        Comparator a = new Comparator()
        {
            public int compare(Object oa, Object ob)
            {
                String a = (String)oa;
                String b = (String)ob;
                if (Tools.isAllDigits(a) || a == "" ) {
                    if (Tools.isAllDigits(b) || b == "" ) {
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
     * This method handles the FrequencyShipChange event.
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
     * Remove player from all tracking lists when they leave the arena.
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
     * When a player enters, display necessary information, and check their ship & freq.
     *
     * @param event is the event to process.
     */    
    public void handleEvent(PlayerEntered event)
    {
        try {
            if(started)
            {
                int playerID = event.getPlayerID();
                Player player = m_botAction.getPlayer(playerID);
                String playerName = m_botAction.getPlayerName(playerID);
                
                m_botAction.sendSmartPrivateMessage(playerName, "This arena has pure pub settings enabled.  Certain ships are restricted.");
                checkPlayer(playerID);
                if(!privFreqs)
                {
                    m_botAction.sendSmartPrivateMessage(playerName, "Private Frequencies are currently disabled.");
                    checkFreq(playerID, player.getFrequency(), false);
                }
            }
            if(flagTimeStarted) {
                int playerID = event.getPlayerID();
                Player player = m_botAction.getPlayer(playerID);
                String playerName = m_botAction.getPlayerName(playerID);
                
                m_botAction.sendSmartPrivateMessage(playerName, "Flag Time mode is currently running.  Send !help to me for more information.");
                if( flagTimer != null)
                    m_botAction.sendSmartPrivateMessage(playerName, flagTimer.getTimeInfo() );      
            }
        } catch (Exception e) {      
        }
        
    }
    
    
    /**
     * If flag time mode is running, register with the flag time game that the flag has been claimed. 
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
     * This method handles a Message event.
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
     * This method handles a command sent to the bot.
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
            else if(command.startsWith("!startstricttime "))
                doStartStrictTimeCmd(sender, message.substring(17));
            else if(command.equals("!stoptime"))
                doStopTimeCmd(sender);
            else if(command.equals("!die"))
                doDieCmd(sender);
        }
        catch(RuntimeException e)
        {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }

    
    /**
     * This method moves a bot from one arena to another.  The bot must not be
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
     * This method starts the pure pub settings.
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
     * This method stops the pure pub settings.
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
     * This method toggles if private frequencies are allowed or not.
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
     * Starts a "flag time" mode in which a team must hold the flag for a certain consecutive
     * number of minutes in order to win the round.
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
        
        m_botAction.sendArenaMessage( "OBJECT: Hold flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win." );
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
     * Starts a "flag time" mode in which a team must hold the flag for a certain consecutive
     * number of minutes in order to win the round.
     * 
     * Differs from normal time in that all players are warped automatically, and shipreset.
     * 
     * @param sender is the person issuing the command.
     * @param argString is the number of minutes to hold the game to.
     */
    public void doStartStrictTimeCmd(String sender, String argString ) {
        if(flagTimeStarted)
            throw new RuntimeException( "Flag Time mode has already been started.  Disable and re-enable if you want to run strict flag time." );
        
        strictFlagTime = true;
        doStartTimeCmd(sender, argString );
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
            flagTimer.cancel();
            intermissionTimer.cancel();
            startTimer.cancel();
        } catch (Exception e ) {
        }
        
        flagTimeStarted = false;
        strictFlagTime = false;
    }
    
    
    /**
     * Displays info about time remaining, if applicable.
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
            throw new RuntimeException( "You do not need to !warp in Strict Flag Time mode." );

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
     * Displays a help message depending on access level.
     *
     * @param sender is the person issuing the command.
     */    
    public void doHelpCmd(String sender)
    {
        String[] helpMessage =
        {
                "!Go <ArenaName>                  -- Moves the bot to <ArenaName>.",
                "!Start                           -- Starts pure pub settings.",
                "!Stop                            -- Stops pure pub settings.",
                "!Privfreqs                       -- Toggles Private Frequencies.",
                "!StartTime #                     -- Starts Flag Time mode (a team wins",
                "                                    with # consecutive min of flagtime).",
                "!StopTime                        -- Ends Flag Time mode.",
                "!StartStrictTime #               -- Starts a 'Strict' Flag Time mode",                
                "!Time                            -- Provides time remaining in Flag Time mode.",
                "!Die                             -- Logs the bot off of the server.",
                "!Help                            -- Displays this help message."
        };
        
        String[] playerHelpMessage =
        {
                "Hello!  I am a bot designed to enforce 'pure pub' rules.",
                "When enabled, I may restrict certain ships, prevent private frequencies, or run Flag Time mode.",
                "Flag Time mode commands (a freq must hold flag for an amount of consecutive minutes to win):",
                "!Time                            -- Provides time remaining in Flag Time mode.",
                "!Warp                            -- Warps you into flagroom at start of next round.",
                "!Help                            -- Displays this help message."
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
     * This method checks to see if a player is a restricted ship.  If they are then
     * they are specced.
     *
     * @param playerName is the player to be checked.
     * @param specMessage enables the spec message.
     */    
    private void checkPlayer(int playerID)
    {
        Player player = m_botAction.getPlayer(playerID);
        if( player == null )
            return;

        int weight = ((Integer)shipWeights.get(player.getShipType())).intValue();
        
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
        
        // For all other weights, we must decide whether they can play based on the number of people on freq
        // who are also using the ship.
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
            if( ((Integer)shipWeights.get(3)).intValue() == 1 ) {
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
     * This method removes a playerName from the freq lists.
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
     * This method removes a playerName from the warp list.
     */
    private void removeFromWarpList(String playerName)
    {       
        warpPlayers.remove( playerName );
    }

    
    /**
     * This method sets a player to a freq and updates the freq lists.
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
     * This method checks to see if a player is on a private freq.  If they are
     * then they are changed to the pub freq with the fewest number of players.
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
     * This method fills the freq lists for freqs 1 and 0.
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
     * This method fixes the freq of each player.
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
            flagTimer.cancel();
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
        
        m_botAction.sendArenaMessage( "OBJECT: Hold flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win a round.  Best 3 of 5." );
        int roundNum = freq0Score + freq1Score + 1;
        
        m_botAction.sendArenaMessage( ( roundNum == 5 ? "FINAL ROUND" : "Round " + roundNum) + " begins in " + getTimeString( INTERMISSION_SECS ) + ".  PM me with !warp to warp into flagroom at round start. -" + m_botAction.getBotName() );

        try {
            startTimer.cancel();
        } catch (Exception e ) {
        }       
        
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
       
        try {
            int secs = flagTimer.getTotalSecs();
            int mins = secs / 60;
            int weight = ((int)(secs * 3 ) / 60);
            
            // Incremental bounty bonuses
            if( mins >= 90 )
                weight += 150;
            else if( mins >= 60 )
                weight += 100;
            else if( mins >= 30 )
                weight += 45;
            else if( mins >= 15 )
                weight += 20;
            
            
            int flagholdingFreq = flagTimer.getHoldingFreq();
            
            if( flagholdingFreq == 0 || flagholdingFreq == 1 ) {
                if( flagholdingFreq == 0 )
                    freq0Score++;
                else
                    freq1Score++;
                
                if( freq0Score > 2 || freq1Score > 2) {
                    m_botAction.sendArenaMessage( "END GAME!  Freq " + flagholdingFreq + " has won after " + getTimeString( flagTimer.getTotalSecs() ) +
                            " (" + weight + " bounty bonus)  Final score: " + freq0Score + " - " + freq1Score, 2 );                                
                    freq0Score = 0;
                    freq1Score = 0;
                } else {
                    int roundNum = freq0Score + freq1Score;
                    m_botAction.sendArenaMessage( "END OF ROUND " + roundNum + ": Freq " + flagholdingFreq + " wins after " + getTimeString( flagTimer.getTotalSecs() ) +
                            " (" + weight + " bounty bonus)  Score: " + freq0Score + " - " + freq1Score, 1 );                                                
                }
                
            } else {
                m_botAction.sendArenaMessage( "END ROUND: Freq " + flagholdingFreq + " wins the round after " + getTimeString( flagTimer.getTotalSecs() ) + "(" + weight + " bounty bonus)", 1 );
                
            }
            
            int special = 0;
            // Special prizes for long battles (add more if you think of any!)
            if( mins > 12 ) {
                Random r = new Random();
                int chance = r.nextInt(100);
                
                if( chance == 99 ) {
                    special = 8;
                } else if( chance >= 97 ) {
                    special = 7;
                } else if( chance >= 94 ) {
                    special = 6;
                } else if( chance >= 90 ) {
                    special = 5;
                } else if( chance >= 80 ) {
                    special = 4;
                } else if( chance >= 70 ) {
                    special = 3;
                } else if( chance >= 60 ) {
                    special = 2;        	    
                } else if( chance > 20 ) {
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
                        
                        Integer i = (Integer)playerTimes.get( playerName );
                        
                        if( i != null ) {
                            // Calculate amount of time actually spent on freq 
                            
                            int timeOnFreq = secs - i.intValue();
                            int percentOnFreq = (int)( ( (float)timeOnFreq / (float)secs ) * 100 );
                            int modbounty = (int)(weight * ((float)percentOnFreq / 100));
                            
                            if( percentOnFreq == 100 ) {
                                MVPs.add( playerName );
                                m_botAction.sendPrivateMessage( playerName, "For staying with the same freq and ship the entire match, you are an MVP and receive the full bonus: " + modbounty );
                                if( special == 4 ) {
                                    m_botAction.sendPrivateMessage( playerName, "You also receive an additional " + weight + " bounty as a special prize!" );
                                    modbounty *= 2;
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
            
            String leader = flagTimer.getTeamLeader( MVPs ); 
            String name, MVplayers = "";
            MVPs.remove( leader );
            Iterator i = MVPs.iterator();
            
            if( i.hasNext() ) {
                switch( special ) {
                case 1:  // "Refreshments" -- replenishes all essentials + gives anti
                    m_botAction.sendArenaMessage( "PRIZE for MVPs: Refreshments!" );
                    break;
                case 2:  // "Full shrap"
                    m_botAction.sendArenaMessage( "PRIZE for MVPs: Full shrap!" );
                    break;
                case 3:  // "Trophy" -- decoy given
                    m_botAction.sendArenaMessage( "PRIZE for MVPs: Life-size Trophies of Themselves!" );
                    break;
                case 4:  // "Double bounty reward"
                    m_botAction.sendArenaMessage( "PRIZE for MVPs: Double Bounty Bonus!  (MVP bounty: " + weight * 2 + ")" );
                    break;
                case 5:  // "Triple trophy" -- 3 decoys
                    m_botAction.sendArenaMessage( "PRIZE for MVPs: The Triple Platinum Trophy!" );
                    break;                
                case 6:  // "Techno Dance Party" -- plays victory music :P
                    m_botAction.sendArenaMessage( "PRIZE for MVPs: Ultimate Techno Dance Party!", 102);
                    break;                
                case 7:  // "Sore Loser's Revenge" -- engine shutdown!
                    m_botAction.sendArenaMessage( "PRIZE for MVPs: Sore Loser's REVENGE!" );
                    break;                
                case 8:  // "Bodyguard" -- shields
                    m_botAction.sendArenaMessage( "PRIZE for MVPs: Personal Body-Guard!" );
                    break;                
                }
                
                MVplayers = (String)i.next();
            }        
            while( i.hasNext() ) {
                name = (String)i.next();
                MVplayers = MVplayers + ", " + name;
            }
            
            if( leader != "" )
                m_botAction.sendArenaMessage( "The Team Leader was " + leader + "!");
            if( MVplayers != "" )
                m_botAction.sendArenaMessage( "MVPs: " + MVplayers );

        } catch(Exception e) {
            Tools.printStackTrace( e );
        }        
        
        MVPs = new HashSet();
        
        try {
            flagTimer.endGame();
            flagTimer.cancel();
            intermissionTimer.cancel();
        } catch (Exception e ) {
        }       
               
        intermissionTimer = new IntermissionTask();
        m_botAction.scheduleTask( intermissionTimer, 10000 );
    }
    

    /**
     * Adds all players to the hashmap which stores the time, in flagTimer time,
     * when they joined their freq. 
     */
    public void setupPlayerTimes() {
        playerTimes = new HashMap();

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
                rand = r.nextInt( NUM_WARP_POINTS_PER_SIDE ); 
                if( p.getFrequency() % 2 == randomside )
                    doRandomWarp( pname, warpPtsLeftX[rand], warpPtsLeftY[rand] );
                else
                    doRandomWarp( pname, warpPtsRightX[rand], warpPtsRightY[rand] );
            } else {
                if( !strictFlagTime )
                    warpPlayers.remove( pname );
            }
        }                    
    }
    
    
    /**
     * In Strict Flag Time mode, warp all players to a safe 10 seconds before starting.
     *
     */
    private void safeWarp() {
        Iterator i = m_botAction.getPlayingPlayerIterator();
        Player p;
        
        while( i.hasNext() ) {
            p = (Player)i.next();
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
    private void doRandomWarp(String playerName, int xCoord, int yCoord ) {
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
            m_botAction.die();
        }
    }
    
    
    /**
     * This private class starts the round.
     */
    private class StartRoundTask extends TimerTask {
        public void run() {
            doStartRound();
        }
    }
    
    
    /**
     * This private class provides a pause before starting the round.
     */
    private class IntermissionTask extends TimerTask {
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
        HashMap flagClaims;
        
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
            flagClaims = new HashMap();
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
                        m_botAction.sendArenaMessage( "INCONCIEVABLE!!: " + p.getPlayerName() + " claims the flag for Freq " + flagHoldingFreq + " with just " + remain + " second" + (remain == 1 ? "" : "s") + " left!", 7 );
                    else if( remain < 11 )
                        m_botAction.sendArenaMessage( "AMAZING!: " + p.getPlayerName() + " claims the flag for Freq " + flagHoldingFreq + " with just " + remain + " seconds left!" );
                    else if( remain < 25 )
                        m_botAction.sendArenaMessage( "SAVE!: " + p.getPlayerName() + " claims the flag for Freq " + flagHoldingFreq + " with " + remain + " seconds left!" );
                    else
                        m_botAction.sendArenaMessage( "SAVE: " + p.getPlayerName() + " claims the flag for Freq " + flagHoldingFreq + " with " + remain + " seconds left." );
                }
            }
            
            m_botAction.setTimer( flagMinutesRequired );
            
            isBeingClaimed = false;
            flagClaimingFreq = -1;
            secondsHeld = 0;
            
        }
        
        /**
         * Increments a count for player claiming the flag.
         * @param name Name of player.
         */
        public void addFlagClaim( String name ) {
            Integer count = (Integer)flagClaims.get( name );
            if( count == null ) {
                flagClaims.put( name, new Integer(1) );
            } else {
                flagClaims.remove( name );
                flagClaims.put( name, new Integer( count.intValue() + 1) );
            }
        }
        
        /**
         * Gives the name of the top flag claimers out of the MVPs.  If there is a tie, does not
         * care because it doesn't matter all that much and it's only bragging rights anyway. :P
         * @return A HashSet containing all players who claimed the flag the largest number of times.  
         */
        public String getTeamLeader( HashSet MVPs ) {
            if( MVPs == null )
                return ""; 
            try {
                Iterator i = MVPs.iterator();
                Integer dummyClaim, highClaim = new Integer(0);
                String leader = "", dummyPlayer;
                
                while( i.hasNext() ) {
                    dummyPlayer = (String)i.next();
                    dummyClaim = (Integer)flagClaims.get( dummyPlayer );
                    if( dummyClaim != null ) {
                        if( dummyClaim.intValue() > highClaim.intValue() ) {
                            leader = dummyPlayer;
                            highClaim = dummyClaim;
                        }
                    }
                }                
                return leader;

            } catch (Exception e ) {
                Tools.printStackTrace( e );
                return "";
            }
            
        }

        /**
         * Ends the game for the timer's internal purposes.
         */
        public void endGame() {
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
         * @return Time-based status of game
         */
        public String getTimeInfo() {
            int roundNum = freq0Score + freq1Score + 1;

            if( isRunning == false ) {
                return "We are currently in between games (round " + roundNum + " starting soon).  Score: " + freq0Score + " - " + freq1Score;
            }
            return "ROUND " + roundNum + ": " + (flagHoldingFreq == -1 ? "Nobody" : "Freq " + flagHoldingFreq ) + " has held for " + getTimeString(secondsHeld) + " & needs " + getTimeString( (flagMinutesRequired * 60) - secondsHeld ) + " more.  [Total time: " + getTimeString( totalSecs ) + "]  Score: " + freq0Score + " - " + freq1Score;        
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
               
        public void run() {        
            if( isStarted == false ) {
                int roundNum = freq0Score + freq1Score + 1;
                if( preTimeCount == 0 ) {
                    m_botAction.sendArenaMessage( "Round " + roundNum + " begins in 10 seconds . . ." );
                    if( strictFlagTime )
                        safeWarp();
                }
                preTimeCount++;
                
                if( preTimeCount >= 10 ) {
                    isStarted = true;
                    isRunning = true;
                    m_botAction.sendArenaMessage( ( roundNum == 5 ? "FINAL ROUND" : "ROUND " + roundNum) + " START!  Hold flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win.", 1 );
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
                m_botAction.sendArenaMessage( "Flag Time commands (PM to " + m_botAction.getBotName() + "): !warp !time !help" );       
                m_botAction.sendArenaMessage( getTimeInfo() );
            }
            
            if( isBeingClaimed ) {
                claimSecs++;
                if( claimSecs >= 3 ) {
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
                m_botAction.sendArenaMessage( "Freq " + flagHoldingFreq + " will win in 60 seconds." );
            } else if( flagSecsReq - secondsHeld == 10 ) {
                m_botAction.sendArenaMessage( "Freq " + flagHoldingFreq + " will win in 10 seconds . . ." );
            }
        }
    }  
    
}
