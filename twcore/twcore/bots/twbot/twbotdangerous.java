/*
 * twbotdangerous - Most Dangerous Game module - qan (dugwyler@gmail.com)
 *
 * 2/19/05
 * 
 *
 * DESC: Elimination match based on time.
 *
 *       - Each person starts with 60 seconds.
 *       - Every kill gives you 20 seconds more to live (or another amount of time,
 *         to be decided).
 *       - When their clock reaches 0, players are spec'd.
 */  



package twcore.bots.twbot;

import java.util.*;

import twcore.core.*;



/**
 * The Most Dangerous Game: Time elimination.
 * 
 * @author  qan
 * @version 1.2
 */
public class twbotdangerous extends TWBotExtension {

    public twbotdangerous() {
    }

    private TimerTask startGame;
    private TimerTask giveStartWarning;
    private TimerTask timeUpdate;

    private boolean isRunning = false;

    private HashMap m_players;

    private int m_totalTime = 0;
    private int m_stolenTime = 0;
    private int m_numStolen = 0; 
    
    // Defaults (for reader clarity only)
    private int m_starttime = 120; 
    private int m_killtime = 20; 
    private int m_deathtime = 8;
    
    



    /**
     * Initializes game with appropriate settings
     * @param starttime Time each player starts with.
     * @param killtime Time you get for making a kill.
     * @param deathtime Time subtracted for a death.
     */
    public void doInit( final int starttime, final int killtime, final int deathtime ){

        m_totalTime = 0;
        
        m_starttime = starttime;
        m_killtime = killtime;
        m_deathtime = deathtime;
                
        m_botAction.sendArenaMessage( "--- Welcome to the MOST DANGEROUS GAME. ---" );
        m_botAction.sendArenaMessage( "...You are about to die.  You have " + m_starttime + " left to live." );
        m_botAction.sendArenaMessage( "Every kill extends your life " + m_killtime + " seconds, while every death reduces it by " + m_deathtime + ".");
        m_botAction.sendArenaMessage( "It does not matter how much you 'die.'  When your timer reaches 0, you will cease to exist.", 2);
        
        giveStartWarning = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage( "10 seconds until the game begins ...", 2);
            }
        };
        m_botAction.scheduleTask( giveStartWarning, 5000 );

        startGame = new TimerTask() {
            public void run() {
                if( isRunning == false ) {
                    isRunning = true;

                    m_botAction.scoreResetAll();
                    m_botAction.shipResetAll();
                    
                    m_botAction.prizeAll( 7 );
                    
                    createPlayerRecords();

                    m_botAction.sendArenaMessage( "Let the MOST DANGEROUS GAME begin.", 104);
                    m_botAction.sendArenaMessage( "Granted " + getTimeString( m_starttime ) + " until you die.  My offer to you: +" + m_killtime + " per kill, -" + m_deathtime + " per death.");
                    m_botAction.sendArenaMessage( "You may speak to me with:  !time  !lagout  !invest x  !help  !info  -" + m_botAction.getBotName());
                }

            }
        };
        m_botAction.scheduleTask( startGame, 15000 );

        timeUpdate = new TimerTask() {
            int minutes, seconds;
            
            public void run() {
                m_totalTime++;
                seconds = m_totalTime % 60;
                if( seconds == 0 ) {
                    minutes = m_totalTime / 60;
                    m_botAction.sendArenaMessage( "Time elapsed: " + minutes + ":00.  Time leader:  " + getTimeLeaderString() );
                }
            }
        };
        m_botAction.scheduleTaskAtFixedRate( giveStartWarning, 1000, 1000 );
    
    }

    
    
    /**
     * Creates a record on each player to keep track of their time,
     * and starts their clocks running.
     */
    public void createPlayerRecords() {
        m_players = new HashMap();
        
        Iterator i = m_botAction.getPlayingPlayerIterator();
        
        while( i.hasNext() ) {
            Player p = (Player)i.next();
           
            PlayerInfo player = new PlayerInfo( p.getPlayerName(), p.getShipType() );           
            m_botAction.scheduleTaskAtFixedRate( player, 1000, 1000 );
           
            m_players.put( p.getPlayerName(), player );            
        }
    }
    
    
    
    /**
     * Clears all player records, and cancels all timer tasks. 
     */
    public void clearRecords() {
        Iterator i = m_players.values().iterator();
        
        while( i.hasNext() ) {
            PlayerInfo p = (PlayerInfo)i.next();
            p.setNotPlaying();
            p.cancel();
        }
        
        m_players = new HashMap();
    }
    
    
    
    public void declareWinner() {
        isRunning = false;

        Iterator i = m_botAction.getPlayingPlayerIterator();
        Player winner = (Player) i.next();        
        
        PlayerInfo player = (PlayerInfo) m_players.get( winner.getPlayerName() );
        m_botAction.sendArenaMessage( "It is " + getTimeString( m_totalTime ) + "!  Someone has survived the Game.", 13 );

        if( player != null )
            m_botAction.sendArenaMessage( winner.getPlayerName() + " still lives with " + player.getTime() + " to spare, and a high of " + player.getMaxTime() + "." );
        m_botAction.sendArenaMessage( "This game's Time MVP is " + getMaxTimeLeaderString() + ", with " + getHighKiller() + " being quite the butcher..."  );
        if( m_numStolen > 0 )
            m_botAction.sendArenaMessage( "Today I've managed to cheat " + m_numStolen + " of you out of more than " + m_stolenTime + " seconds of your lives." );
        m_botAction.sendArenaMessage( "Thank you for playing the MOST DANGEROUS GAME.", 102 );
    }
    
    
    
    public String getTimeLeaderString() {
        Iterator i = m_players.values().iterator();
        PlayerInfo highPlayer;
        
        if( i.hasNext() ) {
            highPlayer = (PlayerInfo)i.next();
        } else {
            return "";
        }
                
        while( i.hasNext() ) {
            PlayerInfo p = (PlayerInfo)i.next();
            // Doesn't retain tied high times.  Cry -> river
            if( p.getTimeInt() > highPlayer.getTimeInt() ) {
                highPlayer = p;
            }            
        }
        
        return highPlayer + " (" + highPlayer.getTime() + ")";        
    }
    
    
    public String getMaxTimeLeaderString() {
        Iterator i = m_players.values().iterator();
        PlayerInfo highPlayer;
        
        if( i.hasNext() ) {
            highPlayer = (PlayerInfo)i.next();
        } else {
            return "";
        }
                
        while( i.hasNext() ) {
            PlayerInfo p = (PlayerInfo)i.next();
            // Doesn't retain tied high times.  Cry -> river
            if( p.getMaxTimeInt() > highPlayer.getMaxTimeInt() ) {
                highPlayer = p;
            }            
        }
        
        return highPlayer + " (" + highPlayer.getTime() + ")";        
    }

    
    
    public String getHighKiller() {
        Iterator i = m_players.values().iterator();
        String highPlayer = "";
        int kills = 0;
        
        if( i.hasNext() ) {
            PlayerInfo pinfo = (PlayerInfo)i.next();
            Player player = m_botAction.getPlayer( pinfo.toString() );
            if( player != null ) {
                highPlayer = pinfo.toString();
                kills = player.getWins();
            }
        } else {
            return "no-one";
        }
                
        while( i.hasNext() ) {
            PlayerInfo pinfo = (PlayerInfo)i.next();
            Player player = m_botAction.getPlayer( pinfo.toString() );
            if( player != null ) {            
                int pkills = player.getWins();           
                // Doesn't retain tied highs.  Cry -> river
                if( pkills > kills ) {
                	highPlayer = pinfo.toString();
            	}
            }
        }
        
        if( highPlayer == "" )
            return "no-one";
        else
            return highPlayer;        
    }
    
    
    
    public String getTimeString( int time ) {
        if( time <= 0 ) {
            return "0:00";            
    	}else {
            int minutes = time / 60;
            int seconds = time % 60;
            return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        }
    }
    

    /**
     * This handleEvent accepts msgs from players as well as mods.
     * @event The Message event in question.
     */
    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            handleCommand( name, message );
        }
    }

    
    
    /**
     * Handles all commands given to the bot.
     * @param name Name of person who sent the command (not necessarily an ER+)
     * @param message Message sent
     */
    public void handleCommand( String name, String message ){

        if( message.startsWith( "!time" )) {
            if(isRunning == true) {
                PlayerInfo player = (PlayerInfo) m_players.get( name );
                if( player != null ) {
                    m_botAction.sendPrivateMessage( name, "So far, you have survived for " + getTimeString( m_totalTime ) + "." );                    
                    m_botAction.sendPrivateMessage( name, "You have " + player.getTime() + " remaining to live, dead one.");                    
                }
            } else {
                m_botAction.sendPrivateMessage( name, "Game is not currently running." );
            }

        } else if( message.startsWith( "!lagout" )) {
            if(isRunning == true) {
                PlayerInfo player = (PlayerInfo) m_players.get( name );
                if( player != null ) {
                    if( player.isLagged() ) {
                        player.returnedFromLagout();
                    } else {
                        m_botAction.sendPrivateMessage( name, "You aren't lagged out!" );
                    }
                } else {                    
                    m_botAction.sendPrivateMessage( name, "Your name was not found in the record." );
                }
            } else {
                m_botAction.sendPrivateMessage( name, "The Game is not currently started." );
            }

        } else if( message.startsWith( "!invest " )) {
            if(isRunning == true) {
                PlayerInfo player = (PlayerInfo) m_players.get( name );
                                                
                if( player != null ) {
                    String[] parameters = Tools.stringChopper( message.substring( 8 ), ' ' );
                    try {
                        int amt = Integer.parseInt(parameters[0]);
                        if( amt > player.getTimeInt() ) {
                            if( amt >= 60 ) {
                                Investment i = new Investment( name, amt );
                                player.invest( amt );
                                int total = amt + (amt / 15); 
                                m_botAction.sendPrivateMessage( name, "Done.  You will receive back " + getTimeString( total ) + " when my clock reaches " + getTimeString( m_totalTime + amt ) + "." );
                                
                            } else {
                                m_botAction.sendPrivateMessage( name, "I demand at least one minute of your life, dead one." );                                
                            }
                            
                        } else {
                            m_botAction.sendPrivateMessage( name, "You don't have that kind of time, dead one." );                            
                        }
                            
                    } catch (Exception e) {
                        m_botAction.sendPrivateMessage( name, "Give me a number or I can't bargain with you, dead one." );                        
                    }
                } else {                    
                    m_botAction.sendPrivateMessage( name, "Your name was not found in the record." );
                }
            } else {
                m_botAction.sendPrivateMessage( name, "The Game is not currently started." );
            }

        } else if( message.startsWith( "!invest" )) {
            m_botAction.sendPrivateMessage( name, "How much would you like to invest?  I'll... borrow the time, and give you more back later, if you survive." );           

        } else if( message.startsWith( "!help" )) {
            String[] playerHelp = {
                    "!time               - (PUBLIC) Shows remaining time.",
            		"!invest x           - (PUBLIC) Invests x secs, returned x later + 15%",
                    "!lagout             - (PUBLIC) Gets you back in the game.", 
                    "!info               - (PUBLIC) Shows bot info." };
			m_botAction.privateMessageSpam( name, playerHelp );
            
    	} else if( message.startsWith( "!info" )) {
            m_botAction.sendPrivateMessage( name, "The Most Dangerous Game, v1.2 by qan.  Send !help for a list of commands." );
    	}

        
        if( m_opList.isER( name )) {
            if( message.startsWith( "!stop" )){
                if(isRunning == true) {
                    m_botAction.sendPrivateMessage( name, "The Most Dangerous Game has stopped." );
                    clearRecords();
                    isRunning = false;
                } else {
                    m_botAction.sendPrivateMessage( name, "The Most Dangerous Game is not currently enabled." );
                }
              
            } else if( message.startsWith( "!start " )){
                if(isRunning == false) {
                    String[] parameters = Tools.stringChopper( message.substring( 7 ), ' ' );
                    try {
                        int p1 = Integer.parseInt( parameters[0] );
                        int p2 = Integer.parseInt( parameters[1] );
                        int p3 = Integer.parseInt( parameters[2] );                        
                        doInit( p1, p2, p3 );
                    } catch (Exception e) {
                        m_botAction.sendPrivateMessage( name, "Error in formatting your command.  Please try again." );                        
                    }
                                        
                } else {
                    m_botAction.sendPrivateMessage( name, "The Most Dangerous Game has already begun." );
                }

            } else if( message.startsWith( "!start" )){
                if(isRunning == false) {
                    doInit( 120, 20, 5 );
                    m_botAction.sendPrivateMessage( name, "Beginning the story..." );
                } else {
                    m_botAction.sendPrivateMessage( name, "The Most Dangerous Game has already begun." );
                }

            } else if( message.startsWith( "!rules" )) {

                m_botAction.sendArenaMessage( "RULES of the MOST DANGEROUS GAME: Everyone starts with a certain number of seconds left to live." );
                m_botAction.sendArenaMessage( "Every kill extends your life, while every death reduces it.");
                m_botAction.sendArenaMessage( "It does not matter how much you die.  When your timer reaches 0, you will cease to exist.", 2);

            } 
        }
    }



    /**
     * Adds and subtracts time when players die.
     * @param event Contains event information on player who died.
     */
    public void handleEvent( PlayerDeath event ){
        if( isRunning ){
            Player killed = m_botAction.getPlayer( event.getKilleeID() );
            Player killer = m_botAction.getPlayer( event.getKillerID() );
            
            if( killed != null ) {
                PlayerInfo player = (PlayerInfo) m_players.get( killed.getPlayerName() );
                if( player != null )
                    player.hadDeath();
            }

            if( killed != null ) {
                PlayerInfo player = (PlayerInfo) m_players.get( killer.getPlayerName() );
                if( player != null )
                    player.hadKill();
            }
        }
    }
    

    /**
     * Counts arena leaves as DCs to be safe.
     * @param event Contains event information on player.
     */
    public void handleEvent( PlayerLeft event ){
        if( isRunning ){
            Player p = m_botAction.getPlayer( event.getPlayerID() );
            
            if( p != null ) {
                if( m_botAction.getNumPlayers() == 1 ) {
                    declareWinner();
                } else {
                    PlayerInfo player = (PlayerInfo) m_players.get( p.getPlayerName() );
                    if( player != null ) {
                        if( player.isPlaying() )
                            player.laggedOut();
                    }
                }
            }
        }
    }

    
    /**
     * Handles lagouts.
     * @param event Contains event information on player.
     */
    public void handleEvent( FrequencyShipChange event ) {
        if( isRunning ) {
            Player p = m_botAction.getPlayer( event.getPlayerID() );
        
            if( p != null ) {
                if( p.getShipType() == 0 ) {

                    if( m_botAction.getNumPlayers() == 1 ) {
                        declareWinner();
                    } else {
                        PlayerInfo player = (PlayerInfo) m_players.get( p.getPlayerName() );
                    	if( player != null ) {
                        	if( player.isPlaying() )
                        	    player.laggedOut();
                    	}
                    }
                }
            }
        }
    }
      
    

    /** Returns help message.
     * @return A string array containing help msgs for this bot.
     */
    public String[] getHelpMessages() {
        String[] DangerousHelp = {
            "!start              - Starts normal game (same as !start 120 20 5)",
            "!start <starttime> <killtime> <deathtime>    where",
            "                      starttime = time players begin with",
            "                      killtime  = time gained for killing someone",
            "                      deathtime = time lost for being killed",
            "!stop               - Stops the Most Dangerous Game.",
            "!rules              - Displays basic rules to the arena." };
        return DangerousHelp;
    }



    /**
     * Cleanup.
     */
    public void cancel() {
        clearRecords();
    }
    
    
    
    private class PlayerInfo extends TimerTask {
        
        private String name;
        private int time;
        private int maxTime;
        private int shipType;
        private boolean isPlaying = true;
        private boolean laggedOut = false;

        
        public PlayerInfo( String name, int shipType ) {
            this.name = name;
            this.shipType = shipType;
            time = m_starttime;
            maxTime = m_starttime;
        }
        
        
        public void hadKill() {
            if( isPlaying ) {
                time += m_killtime;
            	if( time > maxTime )
                	maxTime = time;
        		m_botAction.sendPrivateMessage( name, "KILL:  +" + m_killtime + " sec life. (" + getTime() + " total)" );
            }
        }

        
        public void hadDeath() {
            if( isPlaying ) {
                time -= m_deathtime;
                if( time > 0 )
                    m_botAction.sendPrivateMessage( name, "DEATH: -" + m_deathtime + " sec life.  (" + getTime() + " total)" );
                else
                    m_botAction.sendPrivateMessage( name, "DEATH: -" + m_deathtime + " sec life.  (0 TOTAL -- YOU ARE DEAD!!!)" );
                    
            }
        }
        
        
        public void spec() {
            setNotPlaying();
            
            if( laggedOut )
                return;
            
            m_botAction.spec( name );
            m_botAction.spec( name );

            Player p = m_botAction.getPlayer( name );
            
            
            if( p != null )                      
                m_botAction.sendArenaMessage( getTimeString( m_totalTime ) + ": " + name + " is out.  " +
                                              p.getWins() + " wins, " +
                                              p.getLosses() + " losses " +
                                              "(highest time: " + getMaxTime() + ")" );                                        
        }
        
        
        public void run() {
            if( isRunning && isPlaying ) {
                time--;

            	if( time > 0 ) {
            	    if( !laggedOut ) {
            	        if( time == 10 )
            	            m_botAction.sendPrivateMessage( name, "                !!! 10 SECONDS LEFT !!!", 103 );
                		else if( time <= 5 )     
                		    m_botAction.sendPrivateMessage( name, "                       --- " + String.valueOf(time) + " ---" );
            	    }
            	} else {                                        //                 !!! 10 SECONDS LEFT !!!                
            	                                                //                        --- 1 ---
    	            m_botAction.sendPrivateMessage( name,         "~ R.I.P ~  Life cut short, you have expired.  ~ R.I.P ~", 8 );            	    
                	spec();
            	}
            }
        }
        

        public void setNotPlaying() {
            isPlaying = false;
            laggedOut = false;
        }
        

        public void laggedOut() {
            laggedOut = true;
        	m_botAction.sendPrivateMessage( name, "PM me with !lagout to get back in the game." );
        }
        
        
        public void returnedFromLagout() {
            Player p = m_botAction.getPlayer( name );
            if( p != null ) {
                int id = p.getPlayerID();
                m_botAction.setShip( id, shipType );
                m_botAction.sendPrivateMessage( name, "Welcome back.  Time remaining: " + getTime() );
                laggedOut = false;
            } else {
                m_botAction.sendPrivateMessage( name, "Error!  Please ask the host to put you back in manually." );                
            }
        }
        
        
        public boolean isLagged() {
            return laggedOut;
        }

        
        public boolean isPlaying() {
            return isPlaying;
        }
        
        
        public String getTime() {
            return getTimeString( time );
        }
        

        public String getMaxTime() {
            return getTimeString( maxTime );
        }
        

        public int getTimeInt() {
            return time;
        }

        
        public int getMaxTimeInt() {
            return time;
        }
        
        
        public String toString() {
            return name;
        }
        
        
        public void invest( int amt ) {
            time =- amt;
        }

        
        public boolean addInvestment( int amt ) {
            if( !isPlaying )
                return false;
            
            int amtReturn = amt / 15;
            
            m_botAction.sendPrivateMessage( name, "INVESTMENT RETURN!  (" + getTimeString( amt ) + " + " + getTimeString( amtReturn ) + ") + " + getTimeString( time ) + " = " + getTimeString( amt + amtReturn + time ));
            time += amt + amtReturn;
            return true;
            
        }
        
    }


    public class Investment extends TimerTask {
        private String investor;
        private int time;
        
        public Investment( String investor, int amt ) {
            this.investor = investor; 
            time = amt;
        }
        
        public void run() {
            PlayerInfo p = (PlayerInfo)m_players.get(investor);
            if( p != null ) {
                boolean investSucceed = p.addInvestment( time );
                if( !investSucceed ) {
                    m_stolenTime += time;
                    m_numStolen++;
                }
            }
        }
    }
}
