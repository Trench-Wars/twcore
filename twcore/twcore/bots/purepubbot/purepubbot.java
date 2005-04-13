package twcore.bots.purepubbot;

import java.util.*;
import twcore.core.*;

public class purepubbot extends SubspaceBot
{
    
    public static final int SPEC = 0;
    public static final int FREQ_0 = 0;
    public static final int FREQ_1 = 1;
    public static final int LEVIATHAN = 4;
    private static final int FLAG_CLAIM_SECS = 3;		// Seconds it takes to fully claim a flag
    private static final int INTERMISSION_SECS = 120;	// Seconds between end of round and start of next
    private static final int NUM_WARP_POINTS = 13;		// Total number of warp points for !warp cmd
    
    private OperatorList opList;
    private HashSet freq0List;
    private HashSet freq1List;
    private HashMap playerTimes;
    private LinkedList MVPs;
    private boolean started;
    private boolean privFreqs;
    private boolean flagTimeStarted;
    private FlagCountTask flagTimer;
    private StartRoundTask startTimer;
    private IntermissionTask intermissionTimer;
    private int flagMinutesRequired; 
    
    private int warpPtsX[] = { 512, 505, 502, 499, 491, 495, 487, 519, 522, 525, 529, 533, 537 }; 
    private int warpPtsY[] = { 264, 260, 267, 274, 279, 263, 255, 260, 267, 274, 263, 279, 255 };
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
        MVPs = new LinkedList();
        started = false;
        privFreqs = true;
        flagTimeStarted = false;
        warpPlayers = new LinkedList();
    }
    
    /**
     * This method handles the FrequencyShipChange event.
     *
     * @param event is the event to process.
     */
    
    public void handleEvent(FrequencyShipChange event)
    {
        int playerID = event.getPlayerID();
        int freq = event.getFrequency();
        
        if(started)
        {
            checkPlayer(playerID, true);
            if(!privFreqs)
                checkFreq(playerID, freq, true);
        }

        try {
            if( flagTimeStarted && flagTimer.isRunning() ) {
                String pname = m_botAction.getPlayer( playerID ).getPlayerName();
                playerTimes.remove( pname );
                playerTimes.put( pname, new Integer( flagTimer.getTotalSecs() ) );
            }
        } catch (Exception e) {
        }    
    }
    
    /**
     * This method handles a PlayerLeft event.
     *
     * @param event is the event to handle.
     */
    public void handleEvent(PlayerLeft event)
    {
        int playerID = event.getPlayerID();
        String playerName = m_botAction.getPlayerName(playerID);
        
        removeFromLists(playerName);
        removeFromWarpList(playerName);
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
        
        if(started && !privFreqs)
            checkFreq(playerID, freq, true);
        
        try {
            if( flagTimeStarted && flagTimer.isRunning() ) {
                String pname = m_botAction.getPlayer( playerID ).getPlayerName();
                playerTimes.remove( pname );
                playerTimes.put( pname, new Integer( flagTimer.getTotalSecs() ) );
            }
        } catch (Exception e) {
        }
    }
    
    /**
     * This method handles the PlayerEntered event.
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
                
                m_botAction.sendSmartPrivateMessage(playerName, "This arena has pure pub settings enabled.  Leviathans (Ship 4) are no longer allowed in this arena.");
                checkPlayer(playerID, false);
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
     * This method handles a PlayerLeft event.
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
            if( p != null ) {
                flagTimer.flagClaimed( p.getFrequency(), playerID );
            }
        } catch (Exception e) {
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
        specLevis();
        m_botAction.sendArenaMessage("Pure pub settings enabled.  Leviathans (Ship 4) are no longer allowed in this arena.", 2);
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
        m_botAction.sendArenaMessage("Pure pub settings disabled.  Leviathans (Ship 4) are allowed in this arena.", 2);
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
        
        m_botAction.sendSmartPrivateMessage(sender, "Flag Time mode enabled." );
        m_botAction.sendArenaMessage( "Flag Time mode has been enabled." );
        
        m_botAction.sendArenaMessage( "RULES: Hold the flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win the round." );
        m_botAction.sendArenaMessage( "Next round will begin in 60 seconds.  PM me with !warp to warp into flagroom at round start. -" + m_botAction.getBotName() );
        
        flagTimeStarted = true;
        m_botAction.scheduleTask( new StartRoundTask(), 60000 );
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
    }
      
    /**
     * Displays info about time remaining, if applicable.
     * 
     * @param sender is the person issuing the command.
     */
    public void doTimeCmd( String sender )
    {   
        if( flagTimer != null && flagTimeStarted )
            flagTimer.sendTimeRemaining( sender );
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

        if( warpPlayers.contains( sender ) ) {
            warpPlayers.remove( sender );
            m_botAction.sendSmartPrivateMessage( sender, "You will no longer be warped inside FR at the start of every round.  !warp again to reactivate." );
        } else {
            warpPlayers.add( sender );
            m_botAction.sendSmartPrivateMessage( sender, "You will now be warped inside the flag room at start of every round.  !warp again to deactivate." );            
        }
    }
        
    /**
     * This method logs the bot off.
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
     * This method displays a help message.
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
                "!Time                            -- Provides time remaining in Flag Time mode.",
                "!Die                             -- Logs the bot off of the server.",
                "!Help                            -- Displays this help message."
        };
        
        String[] playerHelpMessage =
        {
                "Hello!  I am a bot designed to enforce 'pure pub' rules.",
                "When enabled, I may restrict levis from playing, prevent private frequencies, or run Flag Time mode.",
                "Flag Time mode commands (a freq must hold flag for an amount of consecutive minutes to win):",
                "!Help                            -- Displays this help message.",
                "!Time                            -- Provides time remaining in Flag Time mode.",
                "!Warp                            -- Warps you into flagroom at start of next round."
        };
        
        if( opList.isHighmod( sender ) )
            m_botAction.smartPrivateMessageSpam(sender, helpMessage);
        else
            m_botAction.smartPrivateMessageSpam(sender, playerHelpMessage);
    }
    
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
            
            if ( !opList.isHighmod(sender) )
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
    
    /**
     * This method handles a LoggedOn event.
     *
     * @param event is the message event to handle.
     */
    
    public void handleEvent(LoggedOn event)
    {
        BotSettings botSettings = m_botAction.getBotSettings();
        String initialArena = botSettings.getString("InitialArena");
        
        m_botAction.changeArena(initialArena);
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
    }
    
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
     * This method checks to see if a player is a leviathan.  If they are then
     * they are specced.
     *
     * @param playerName is the player to be checked.
     * @param specMessage enables the spec message.
     */
    
    private void checkPlayer(int playerID, boolean specMessage)
    {
        Player player = m_botAction.getPlayer(playerID);
        
        if(player != null && player.getShipType() == LEVIATHAN)
        {
            m_botAction.spec(playerID);
            m_botAction.spec(playerID);
            if(specMessage)
                m_botAction.sendSmartPrivateMessage(m_botAction.getPlayerName(playerID), "Leviathans are not allowed in this pub.  Please change pubs if you wish to be a levi.");
        }
    }
    
    /**
     * This method removes a playerName from the freq lists.
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
        int ship = player.getShipType();
        int newFreq = freq;
        
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
     * This method specs all of the levis in the arena.
     */
    
    private void specLevis()
    {
        Iterator iterator = m_botAction.getPlayingPlayerIterator();
        Player player;
        
        while(iterator.hasNext())
        {
            player = (Player) iterator.next();
            checkPlayer(player.getPlayerID(), false);
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
        
        m_botAction.sendArenaMessage( "RULES: Hold the flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win the round." );
        m_botAction.sendArenaMessage( "Next round will begin in " + getTimeString( INTERMISSION_SECS ) + ".  PM me with !warp to warp into flagroom at round start. -" + m_botAction.getBotName() );

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
        if( !flagTimeStarted )
            return;
       
        int secs = flagTimer.getTotalSecs();
        int mins = secs / 60;
        // int points = secs * 5;
        int weight = ((int)(secs * 2.5 ) / 60);
        
        /*
        // Incremental point bonuses - REMOVED because using *points resets "Ave" stat
        if( mins >= 120 ) 
            points += 100000;
        if( mins >= 90 ) 
            points += 50000;
        if( mins >= 60 )
            points += 20000;
        if( mins >= 45 )
            points += 10000;
        if( mins >= 30 )
            points += 5000;
        if( mins >= 20 )
            points += 2000;
        if( mins >= 15 )
            points += 1000;
        */

        // Incremental bounty bonuses
        if( mins >= 90 )
            weight += 100;
        else if( mins >= 60 )
            weight += 50;
        else if( mins >= 30 )
            weight += 25;
        else if( mins >= 15 )
            weight += 10;
        

        int flagholdingFreq = flagTimer.getHoldingFreq();
        m_botAction.sendArenaMessage( "END ROUND:  Freq " + flagholdingFreq + " has won!  Bounty bonus possible: " + weight + "   [Total time: " + getTimeString( flagTimer.getTotalSecs() ) + "]", 1 );
        
    	int special = 0;
    	// Special prizes for long battles (add more if you think of any!)
        if( mins > 15 ) {
            Random r = new Random();
        	int chance = r.nextInt(100);
        
        	if( chance == 99 ) {
        	    m_botAction.sendArenaMessage( "SPECIAL PRIZE for Freq " + flagholdingFreq + " MVPs: Personal Body-Guard!" );
        	    special = 8;
        	} else if( chance >= 97 ) {
                m_botAction.sendArenaMessage( "SPECIAL PRIZE for Freq " + flagholdingFreq + " MVPs: Sore Loser's REVENGE!" );
        	    special = 7;
        	} else if( chance >= 94 ) {
                m_botAction.sendArenaMessage( "SPECIAL PRIZE for Freq " + flagholdingFreq + " MVPs: Ultimate Techno Dance Party!", 102);
        	    special = 6;
        	} else if( chance >= 90 ) {
                m_botAction.sendArenaMessage( "SPECIAL PRIZE for Freq " + flagholdingFreq + " MVPs: The Triple Platinum Trophy!" );
        	    special = 5;
        	} else if( chance >= 80 ) {
        	    weight *= 2;
                m_botAction.sendArenaMessage( "SPECIAL PRIZE for Freq " + flagholdingFreq + " MVPs: Double Bounty Bonus!  (Total possible bounty: " + weight + ")" );
        		special = 4;
        	} else if( chance >= 70 ) {
                m_botAction.sendArenaMessage( "SPECIAL PRIZE for Freq " + flagholdingFreq + " MVPs: Life-size Trophies of Themselves!" );
        	    special = 3;
        	} else if( chance >= 60 ) {
                m_botAction.sendArenaMessage( "SPECIAL PRIZE for Freq " + flagholdingFreq + " MVPs: Full shrap!" );
        	    special = 2;        	    
        	} else if( chance > 20 ) {
                m_botAction.sendArenaMessage( "SPECIAL PRIZE for Freq " + flagholdingFreq + " MVPs: Refreshments!" );
        	    special = 1;
        	}
        }
        
        try
        {
            Iterator iterator = m_botAction.getPlayerIterator();
            while(iterator.hasNext())
            {
                Player player;
                player = (Player) iterator.next();
                if( player != null ) {
                    if(player.getFrequency() == flagholdingFreq ) {
                        String playerName = player.getPlayerName();

                        // Calculate amount of time actually spent on freq 
                        if( playerTimes.containsKey( playerName ) ) {
                            Integer i = (Integer)playerTimes.get( playerName );
                                                       
                            int timeOnFreq = secs - i.intValue();
                            int percentOnFreq = (int)( ( (float)timeOnFreq / (float)secs ) * 100 );
                            //int modpoints = points * (percentOnFreq / 100);
                            int modbounty = (int)(weight * ((float)percentOnFreq / 100));
                            
                            if( percentOnFreq == 100 ) {
                                MVPs.add( playerName );
                                m_botAction.sendPrivateMessage( playerName, "For staying on the freq the entire match, you are an MVP and receive the full bonus: " + modbounty );
                            } else {
                                m_botAction.sendPrivateMessage( playerName, "You were on the team for the last " + getTimeString(timeOnFreq) + ", and receive " + percentOnFreq  + "% of the bounty reward: " + modbounty );                                
                            }
                                                                
                            m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*prize " + modbounty);
                            //m_botAction.sendUnfilteredPrivateMessage(player.getPlayerID(), "*points " + modpoints);
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
        } catch(Exception e) {
        }
        
        Iterator i = MVPs.iterator();
        String MVplayers = ""; 
        while( i.hasNext() ) {
            String name = (String)i.next();
            MVplayers = MVplayers + name + " ";
        }        
        if( MVplayers != "" )
            m_botAction.sendArenaMessage( "Freq " + flagholdingFreq + " MVPs: " + MVplayers );
        
        MVPs = new LinkedList();
        
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

        Iterator i = m_botAction.getPlayerIterator();
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
     *
     */
    private void warpPlayers() {
        Iterator i = warpPlayers.iterator();
        Random r = new Random();
        int rand;
        
        while( i.hasNext() ) {
            String pname = (String)i.next();
            if( pname != null ) {
                rand = r.nextInt( NUM_WARP_POINTS ); 
                doRandomWarp( pname, warpPtsX[rand], warpPtsY[rand] );
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
            
            if( remain < 60 ) {
                Player p = m_botAction.getPlayer( claimerID );

                if( p != null ) {
                    if( remain < 4 )
                        m_botAction.sendArenaMessage( "INCONCIEVABLE!!: " + p.getPlayerName() + " claims the flag for Freq " + flagHoldingFreq + " with just " + remain + " second" + (remain == 1 ? "" : "s") + " left!", 7 );
                    else if( remain < 11 )
                        m_botAction.sendArenaMessage( "AMAZING!: " + p.getPlayerName() + " claims the flag for Freq " + flagHoldingFreq + " with just " + remain + " second" + (remain == 1 ? "" : "s") + " left!" );
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
            if( isRunning == false )
                return "We are currently between games.";
            return (flagHoldingFreq == -1 ? "Nobody" : "Freq " + flagHoldingFreq ) + " has held for " + getTimeString(secondsHeld) + " and needs " + getTimeString( (flagMinutesRequired * 60) - secondsHeld ) + " more to win.  [Total time: " + getTimeString( totalSecs ) + "]";        
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
                if( preTimeCount == 0 )
                    m_botAction.sendArenaMessage( "Round begins in 10 seconds . . ." );
                preTimeCount++;
                
                if( preTimeCount >= 10 ) {
                    isStarted = true;
                    isRunning = true;
                    m_botAction.sendArenaMessage( "ROUND START!", 1 );
                    m_botAction.sendArenaMessage( "Hold the flag for " + flagMinutesRequired + " consecutive minute" + (flagMinutesRequired == 1 ? "" : "s") + " to win." );
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
                m_botAction.sendArenaMessage( "Flag Time mode commands (PM to " + m_botAction.getBotName() + "): !help !time !warp" );       
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
