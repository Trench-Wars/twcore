/*
 * twbotdangerous - Most Dangerous Game module - qan
 *
 * 2/29/05
 *
 *
 * DESC: Elimination match based on time.
 *
 *       - Each person starts with 60 seconds.
 *       - Every kill gives you 20 seconds more to live (or another amount of time,
 *         to be decided).
 *       - When their clock reaches 0, players are spec'd.
 */



package twcore.bots.multibot.dangerous;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.util.ModuleEventRequester;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.Tools;



/**
 * The Most Dangerous Game: Time elimination.
 *
 * @author  qan
 * @version 1.2
 */
public class dangerous extends MultiModule {

    public void init() {
    }

    public void requestEvents(ModuleEventRequester events) {
		events.request(this, EventRequester.PLAYER_DEATH);
		events.request(this, EventRequester.PLAYER_LEFT);
		events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
	}

    private TimerTask startGame;
    private TimerTask giveStartWarning;
    private TimerTask timeUpdate;

    private boolean isRunning = false;

    private HashMap<String, PlayerInfo> m_players;

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
        // Let's get reliable.
        m_botAction.setReliableKills( 1 );

        m_totalTime = 0;
        m_stolenTime = 0;
        m_numStolen = 0;

        m_starttime = starttime;
        m_killtime = killtime;
        m_deathtime = deathtime;

        m_botAction.sendArenaMessage( "--- Welcome to the MOST DANGEROUS GAME. ---" );
        m_botAction.sendArenaMessage( "...You are about to die.  In " + getTimeString( m_starttime ) + ", your life is mine." );
        m_botAction.sendArenaMessage( "Every kill extends your meager life " + m_killtime + " seconds, while every death reduces it by " + m_deathtime + ".");
        m_botAction.sendArenaMessage( "It does not matter how much you 'die.'  When your timer reaches 0, you will cease to exist.", 2);

        giveStartWarning = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage( "10 seconds until we begin ...", 2);
            }
        };
        m_botAction.scheduleTask( giveStartWarning, 8000 );

        startGame = new TimerTask() {
            public void run() {
                if( isRunning == false ) {
                    isRunning = true;

                    m_botAction.scoreResetAll();
                    m_botAction.shipResetAll();

                    m_botAction.prizeAll( 7 );

                    createPlayerRecords();

                    m_botAction.sendArenaMessage( "--- Let the MOST DANGEROUS GAME begin.. ---", 104);
                    m_botAction.sendArenaMessage( "Granted " + getTimeString( m_starttime ) + " until death.  My offer: +" + m_killtime + " per kill, -" + m_deathtime + " per death.");
                    m_botAction.sendArenaMessage( "PM " + m_botAction.getBotName() + " with:  !help  !time  !lagout  !invest x  !info" );
                }

            }
        };
        m_botAction.scheduleTask( startGame, 18000 );

        timeUpdate = new TimerTask() {
            int minutes, seconds;

            public void run() {
                m_totalTime++;
                seconds = m_totalTime % 60;
                if( seconds == 0 ) {
                    minutes = m_totalTime / 60;
                    m_botAction.sendArenaMessage( "Time elapsed: " + minutes + ":00.  Time leader: " + getTimeLeaderString() );
                }
            }
        };
        m_botAction.scheduleTaskAtFixedRate( timeUpdate, 19000, 1000 );

    }



    /**
     * Creates a record on each player to keep track of their time,
     * and starts their clocks running.
     */
    public void createPlayerRecords() {
        m_players = new HashMap<String, PlayerInfo>();

        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

        while( i.hasNext() ) {
            Player p = i.next();

            PlayerInfo player = new PlayerInfo( p.getPlayerName(), p.getShipType(), m_starttime );
            m_botAction.scheduleTaskAtFixedRate( player, 1000, 1000 );

            m_players.put( p.getPlayerName(), player );
        }
    }



    /**
     * Clears all player records, and cancels all timer tasks.
     */
    public void clearRecords() {
        m_botAction.cancelTask(timeUpdate);

        if( m_players == null )
            return;
        if( m_players.values() == null )
            return;

        Iterator<PlayerInfo> i = m_players.values().iterator();

        while( i.hasNext() ) {
            PlayerInfo p = (PlayerInfo)i.next();
            p.setNotPlaying();
            m_botAction.cancelTask(p);
        }

        m_players = new HashMap<String, PlayerInfo>();
    }



    /**
     * Declares a winner to the game when there is only one person left.
     *
     */
    public void declareWinner() {
        isRunning = false;

        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
        Player winner = i.next();

        PlayerInfo player = m_players.get( winner.getPlayerName() );
        m_botAction.sendArenaMessage( "It is " + getTimeString( m_totalTime ) + "!  Someone has survived the Game.", 13 );

        if( player != null )
            m_botAction.sendArenaMessage( winner.getPlayerName() + " still lives with " + player.getTime() + " to spare, and a high of " + player.getMaxTime() + "." );
        m_botAction.sendArenaMessage( "This game's Time MVP is: " + getMaxTimeLeaderString() + ""  );
        if( m_numStolen > 0 )
            m_botAction.sendArenaMessage( "Today I've cheated " + m_numStolen + " of you out of " + m_stolenTime + " seconds of your lives!" );
        m_botAction.sendArenaMessage( "Thank you for playing the MOST DANGEROUS GAME.", 102 );

        clearRecords();
    }



    /**
     * Gets info on highest current time.
     * @return Name and time of person with highest current time.
     */
    public String getTimeLeaderString() {
        Iterator<PlayerInfo> i = m_players.values().iterator();
        PlayerInfo highPlayer;

        if( i.hasNext() ) {
            highPlayer = i.next();
        } else {
            return "";
        }

        while( i.hasNext() ) {
            PlayerInfo p = i.next();
            // Doesn't retain tied high times.  Cry -> river
            if( p.getTimeInt() > highPlayer.getTimeInt() ) {
                highPlayer = p;
            }
        }

        return highPlayer + " (" + highPlayer.getTime() + ")";
    }


    /**
     * Gets info on highest max time.
     * @return Name and time of person with highest max time.
     */
    public String getMaxTimeLeaderString() {
        Iterator<PlayerInfo> i = m_players.values().iterator();
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

        return highPlayer + " (" + highPlayer.getMaxTime() + ")";
    }


    /**
     * Format an integer time as a String.
     * @param time Time in seconds.
     * @return Formatted string in 0:00 format.
     */
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
                PlayerInfo player = m_players.get( name );
                if( player != null ) {
                    m_botAction.sendPrivateMessage( name, "So far, you have survived for " + getTimeString( m_totalTime ) + "." );
                    m_botAction.sendPrivateMessage( name, "You have " + player.getTime() + " remaining to live, dead one.");
                }
            } else {
                m_botAction.sendPrivateMessage( name, "Game is not currently running." );
            }

        } else if( message.startsWith( "!lagout" )) {
            if(isRunning == true) {
                PlayerInfo player = m_players.get( name );
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
                PlayerInfo player = m_players.get( name );

                if( player != null ) {
                    String[] parameters = Tools.stringChopper( message.substring( 8 ), ' ' );
                    try {
                        int amt = Integer.parseInt(parameters[0]);
                        if( amt < player.getTimeInt() ) {
                            if( amt >= 60 ) {
                                Investment i = new Investment( name, amt );
                                m_botAction.scheduleTask( i, amt * 1000);
                                player.invest( amt );
                                int total = (int) ( amt + (amt * .15) );
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


        if( opList.isER( name )) {
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
                    doInit( 120, 20, 8 );
                } else {
                    m_botAction.sendPrivateMessage( name, "The Most Dangerous Game has already begun." );
                }

            } else if( message.startsWith( "!addlate " )){
            	if(isRunning == true && m_players.values().size() != 0) {
            		Iterator<PlayerInfo> i = m_players.values().iterator();
            		int size = m_players.values().size();
            		double average = 0, sum = 0;
            		while( i.hasNext() ){
            			PlayerInfo p = i.next();
            			sum += p.time;
            		}
            		average = sum / size;//TODO:
            	    String[] msgs = message.substring(9).split(":");
            	    if(msgs.length != 2){
                		m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !addlate <name>:<ship>");
                		return;
                	}
            		try {
            			Integer shipType = Integer.parseInt(msgs[1]);
            			m_players.put(msgs[0], new PlayerInfo(msgs[0], shipType, (int) Math.round(average)));
            			m_botAction.setShip(msgs[0], shipType);
            		} catch(NumberFormatException e){
            			m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !addlate <name>:<ship>");
            			return;
            		}
            	} else {
            		m_botAction.sendPrivateMessage( name, "Game is not currently running." );
            	}
            } else if( message.startsWith( "!rules" )) {

                m_botAction.sendArenaMessage( "RULES of the MOST DANGEROUS GAME: Everyone starts with a certain number of seconds left to live." );
                m_botAction.sendArenaMessage( "Every kill extends your life, while every death reduces it.");
                m_botAction.sendArenaMessage( "It does not matter how much you die.  When your timer reaches 0, you will cease to exist.", 2);

            } else if ( message.startsWith( "!remove " )) {
                if(isRunning == true) {
                    PlayerInfo player = m_players.get( message.substring(8) );
                    if( player != null ) {
                        player.spec();
                    } else {
                        m_botAction.sendPrivateMessage( name, "Player not found." );
                    }
                } else {
                    m_botAction.sendPrivateMessage( name, "Game is not currently running." );
                }
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
                PlayerInfo player = m_players.get( killed.getPlayerName() );
                if( player != null )
                    player.hadDeath();
            }

            if( killed != null ) {
                PlayerInfo player = m_players.get( killer.getPlayerName() );
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
                    PlayerInfo player = m_players.get( p.getPlayerName() );
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
                        PlayerInfo player = m_players.get( p.getPlayerName() );
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
    public String[] getModHelpMessage() {
        String[] DangerousHelp = {
            "!start                 - Starts normal game (same as !start 120 20 8)",
            "!start <starttime> <killtime> <deathtime>    where",
            "                         starttime = time players begin with",
            "                         killtime  = time gained for killing someone",
            "                         deathtime = time lost for being killed",
            "!addlate <name>:<ship> - Adds with average remaining time",
            "!stop                  - Stops the Most Dangerous Game.",
            "!rules                 - Displays basic rules to the arena.",
            "!remove <name>         - Removes player from the game." };
        return DangerousHelp;
    }



    /**
     * Cleanup.
     */
    public void cancel() {
        clearRecords();
    }


    /**
     * Essentially a TimerTask that stores info about each player.
     *
     */
    private class PlayerInfo extends TimerTask {

        private String name;
        private int time;
        private int maxTime;
        private int shipType;
        private boolean isPlaying = true;
        private boolean laggedOut = false;


        public PlayerInfo( String name, int shipType, int startTime ) {
            this.name = name;
            this.shipType = shipType;
            time = startTime;
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
            	} else {                                      //                 !!! 10 SECONDS LEFT !!!
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
                String name = p.getPlayerName();
                m_botAction.setShip( name, shipType );
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
            time -= amt;
        }


        public boolean addInvestment( int amt ) {
            if( !isPlaying )
                return false;

            int amtReturn = (int)(amt * .15);

            m_botAction.sendPrivateMessage( name, "INVESTMENT RETURN!  (" + getTimeString( amt ) + " + " + getTimeString( amtReturn ) + ") + " + getTimeString( time ) + " = " + getTimeString( amt + amtReturn + time ));
            time += amt + amtReturn;
            return true;

        }

    }


    /**
     * A TimerTask extended class that calculates "!invest"'s made.
     */
    public class Investment extends TimerTask {
        private String investor;
        private int time;

        public Investment( String investor, int amt ) {
            this.investor = investor;
            time = amt;
        }

        public void run() {
            PlayerInfo p = m_players.get(investor);
            if( p != null ) {
                boolean investSucceed = p.addInvestment( time );
                if( !investSucceed ) {
                    m_stolenTime += time;
                    m_numStolen++;
                }
            }
        }
    }
    public boolean isUnloadable()	{
    	clearRecords();
        isRunning = false;
		return true;
	}
}
