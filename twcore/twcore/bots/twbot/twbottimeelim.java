/*
 * twbottimeelim - TimeElim module - qan (dugwyler@gmail.com)
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
 * Time elimination.
 * 
 * @author  qan
 * @version 1.1
 */
public class twbottimeelim extends TWBotExtension {

    public twbottimeelim() {
    }

    private TimerTask startGame;
    private TimerTask giveStartWarning;

    private boolean isRunning = false;
    
    // Defaults (for reader clarity only)
    private int m_starttime = 120; 
    private int m_killtime = 20; 
    private int m_deathtime = 5; 
    
    private HashMap m_players;



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
     * Initializes game with appropriate settings
     * @param starttime Time each player starts with.
     * @param killtime Time you get for making a kill.
     * @param deathtime Time subtracted for a death.
     */
    public void doInit( final int starttime, final int killtime, final int deathtime ){

        m_starttime = starttime;
        m_killtime = killtime;
        m_deathtime = deathtime;
        
        m_botAction.sendArenaMessage( "TIME ELIMINATION RULES: Everyone starts with a certain number of seconds left to live." );
        m_botAction.sendArenaMessage( "Every kill extends your life, while every death reduces it.");
        m_botAction.sendArenaMessage( "You can't be eliminated for deaths, but when your timer reaches 0, you lose!", 2);
        
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

                    m_botAction.sendArenaMessage( "GO GO GO!!!", 104);
                    m_botAction.sendArenaMessage( "Time Elim has begun.  Start time: " + m_starttime + " sec, +" + m_killtime + "/kill, -" + m_deathtime + "/death.");
                }

            }
        };
        m_botAction.scheduleTask( startGame, 15000 );
    }

    
    
    /**
     * Creates a record on each player to keep track of their time,
     * and starts the clock running.
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
                    m_botAction.sendPrivateMessage( name, "Lifetime remaining: " + player.getTime() );                    
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
                m_botAction.sendPrivateMessage( name, "Game is not currently running." );
            }

        }

        
        if( m_opList.isER( name )) {
            if( message.startsWith( "!stop" )){
                if(isRunning == true) {
                    m_botAction.sendPrivateMessage( name, "TimeElim stopped." );
                    clearRecords();
                    isRunning = false;
                } else {
                    m_botAction.sendPrivateMessage( name, "TimeElim is not currently enabled." );
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
                    m_botAction.sendPrivateMessage( name, "TimeElim has already been started." );
                }

            } else if( message.startsWith( "!start" )){
                if(isRunning == false) {
                    doInit( 120, 20, 5 );
                    m_botAction.sendPrivateMessage( name, "TimeElim started." );
                } else {
                    m_botAction.sendPrivateMessage( name, "TimeElim has already been started." );
                }

            } else if( message.startsWith( "!rules" )) {
                m_botAction.sendArenaMessage( "TIME ELIMINATION RULES: Everyone starts with a certain number of seconds left to live." );
                m_botAction.sendArenaMessage( "Every kill extends your life, while every death reduces it.");
                m_botAction.sendArenaMessage( "You can't be eliminated for deaths, but when your timer reaches 0, you lose!", 2);

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
                PlayerInfo player = (PlayerInfo) m_players.get( p.getPlayerName() );
                if( player != null ) {
                    if( player.isPlaying() )
                        player.laggedOut();
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
                    PlayerInfo player = (PlayerInfo) m_players.get( p.getPlayerName() );
                    if( player != null ) {
                        if( player.isPlaying() )
                            player.laggedOut();
                    }                
                }
            }
        }
    }
    

    /** Returns help message.
     * @return A string array containing help msgs for this bot.
     */
    public String[] getHelpMessages() {
        String[] KillerHelp = {
            "!start              - Starts normal TimeElim (same as !start 120 20 5)",
            "!start <starttime> <killtime> <deathtime>    where",
            "                      starttime = time players begin with",
            "                      killtime  = time gained for killing someone",
            "                      deathtime = time lost for being killed",
            "!stop               - Stops TimeElim.",
            "!rules              - Displays basic rules of TimeElim to the arena.",
            "!time               - (PUBLIC COMMAND) Shows remaining time.",
            "!lagout             - (PUBLIC COMMAND) Gets you back in the game." }; 
        return KillerHelp;
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
        		m_botAction.sendPrivateMessage( name, "KILL:  +" + m_killtime + " secs.  (" + getTime() + " remaining)" );
            }
        }

        
        public void hadDeath() {
            if( isPlaying ) {
                time -= m_deathtime;
                if( time > 0 )
                    m_botAction.sendPrivateMessage( name, "DEATH: -" + m_deathtime + " secs. (" + getTime() + " remaining)" );
                else
                    m_botAction.sendPrivateMessage( name, "DEATH: -" + m_deathtime + " secs. (TIME UP!  You're dead!)" );
                    
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
                m_botAction.sendArenaMessage( name + " is out.  " +
                                              p.getWins() + " wins, " +
                                              p.getLosses() + " losses " +
                                              "(best time: " + getMaxTime() + ")" );                                        
        }
        
        
        public void run() {
            if( isRunning && isPlaying ) {
                time--;

            	if( time > 0 ) {
            	    if( !laggedOut ) {
            	        if( time == 10 )
            	            m_botAction.sendPrivateMessage( name, "10 seconds left to live!" );
                		else if( time <= 5 )
                		    m_botAction.sendPrivateMessage( name, String.valueOf(time) );
            	    }
            	} else {
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
            int minutes = time / 60;
            int seconds = time % 60;
            String timeReading = minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
            return timeReading;
        }
        

        public String getMaxTime() {
            int minutes = maxTime / 60;
            int seconds = maxTime % 60;
            String timeReading = minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
            return timeReading;
        }
        
        
    }
}
