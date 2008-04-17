package twcore.bots.multibot.tugawar;

import static twcore.core.EventRequester.FREQUENCY_CHANGE;
import static twcore.core.EventRequester.FREQUENCY_SHIP_CHANGE;
import static twcore.core.EventRequester.PLAYER_DEATH;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;

/**
 * Hosts "Tugawar" or "Boomball" events.
 *
 * Austin Barton - 12.15.02
 */
public class tugawar extends MultiModule {

    boolean 	event = false, tugAWar = false, boomBall = false;
    TimerTask   startEvent;
    /* Used for respawn timers */
    // A HashMap set up to do the task that a List can handle ... yikes, 2d :P  -dugwyler
    HashMap     <String,String>players = new HashMap<String,String>();
    /* Tugawar variables */
    String		capZero, capOne;
    int			capZeroD = 0, capOneD = 0;
    final int   tug_TimeDelay = 10000;
    int			deathLimit = 1;
    /* Boomball variables */
    int[] 		areas = { 0, 0, 0, 0, 0, 0 };
    
    String database = "website";

    public void init() {
    }

    public void requestEvents(ModuleEventRequester events) {
        events.request(this, PLAYER_DEATH);
        events.request(this, FREQUENCY_SHIP_CHANGE);
        events.request(this, FREQUENCY_CHANGE);
    }

    public void handleCommand( String name, String message ) {
        try {
            if( message.toLowerCase().startsWith( "!start tugawar " ) ) {
                String[] pieces = message.split( " " );
                int deaths = Integer.parseInt( pieces[2] );
                if( deaths > 0 && deaths < 11 )
                    deathLimit = deaths;
                startTugAWar( name, message );
            } else if( message.toLowerCase().startsWith( "!start tugawar" ) ) {
                startTugAWar( name, message );
            } else if( message.toLowerCase().startsWith( "!stop tugawar" ) ) {
                stopTugAWar( name, message);
            } else if( message.toLowerCase().startsWith( "!start boomball" ) ) {
                startBoomBall( name, message );
            } else if( message.toLowerCase().startsWith( "!stop boomball" ) ) {
                stopBoomBall( name, message );
            } else if (message.toLowerCase().startsWith("!sql")) {
                doSQLQuery();
            }
        } catch ( Exception e ) {}
    }

    public void doSQLQuery() {
        try {
            int k = 0;
            ResultSet results = m_botAction.SQLQuery(database, "SELECT * FROM tblCall");
            while(results.next()) {
                String name = results.getString("fcUserName");
                int id = results.getInt("fnCallID");
                if(!opList.isZH(name)) {
                    m_botAction.SQLQueryAndClose(database, "DELETE FROM tblCall WHERE fnCallID = "+id);
                    k++;
                }
            }
            m_botAction.SQLClose( results );
        } catch(Exception e) {e.printStackTrace();}
    }

    public void startTugAWar( String name, String message ) {
        if( !event ) {
            try {
                m_botAction.sendUnfilteredPublicMessage( "?set kill:enterdelay:0" );
                startEvent = new TimerTask() {
                    public void run() {
                        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
                        if( i == null ) return;
                        Vector <String>zeroTeam = new Vector<String>();
                        Vector <String>oneTeam = new Vector<String>();
                        while( i.hasNext() ){
                            Player player = (Player)i.next();
                            String curName   = player.getPlayerName();
                            int    freq	  = player.getFrequency();
                            if( freq == 0 ) {
                                m_botAction.warpTo( curName, 419, 422 );
                                zeroTeam.addElement( curName );
                            } else {
                                m_botAction.warpTo( curName, 603, 422 );
                                oneTeam.addElement( curName );
                            }
                        }
                        Random generator = new Random();
                        int randZero = Math.abs( generator.nextInt() ) % zeroTeam.size();
                        int randOne  = Math.abs( generator.nextInt() ) % oneTeam.size();
                        capZero = zeroTeam.elementAt( randZero);
                        capOne = oneTeam.elementAt( randOne );
                        m_botAction.sendArenaMessage( "GO GO GO!!!", 104);
                        m_botAction.sendArenaMessage( "Team 0 Cap: " + capZero );
                        m_botAction.sendArenaMessage( "Team 1 Cap: " + capOne );
                        m_botAction.sendPrivateMessage( capZero, "You are the captain for your team, should you die " + deathLimit + " time(s) your team loses - so play it safe! Good luck." );
                        m_botAction.sendPrivateMessage( capOne, "You are the captain for your team, should you die " + deathLimit + " time(s) your team loses - so play it safe! Good luck." );
                        tugAWar = true;
                        m_botAction.sendUnfilteredPublicMessage( "?set kill:enterdelay:900" );
                    }
                };
                event = true;
                m_botAction.sendArenaMessage( "Rules of TugAWar: DO NOT TEAM KILL! Also warping is illegal (use antiwarp.)" );
                m_botAction.sendArenaMessage( "Kill the enemy cap " + deathLimit + " time(s) to win! Should a cap be eliminated, his team loses." );
                m_botAction.sendArenaMessage( "TugAWar beings in 10 seconds!", 2 );
                m_botAction.scheduleTask( startEvent, 10000 );
            } catch ( Exception e ) {}
        } else m_botAction.sendPrivateMessage( name, "Event already in progress." );
    }

    public void stopTugAWar( String name, String message ) {
        if( tugAWar ) {
            deathLimit = 1;
            capZeroD = 0;
            capOneD = 0;
            event = false;
            tugAWar = false;
            players.clear();
            m_botAction.sendArenaMessage( "Game Over." );
            m_botAction.sendUnfilteredPublicMessage( "?set kill:enterdelay:0" );
            m_botAction.prizeAll( 7 );
        } else m_botAction.sendPrivateMessage( name, "TugAWar is not in progress." );
    }

    public void startBoomBall( String name, String message ) {
        if( !event ) {
            MiscTask thisCheck = new MiscTask( "Check__Players", -1 );
            m_botAction.scheduleTaskAtFixedRate( thisCheck, 0, 1000 );
            Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
            while( i.hasNext() ){
                Player player = (Player)i.next();
                String curName   = player.getPlayerName();
                int    freq	  = player.getFrequency();
                if( freq == 0 ) m_botAction.warpTo( curName, 511, 549 );
                else m_botAction.warpTo( curName, 511, 474 );
            }
            m_botAction.sendArenaMessage( "Boomball begins shortly! Get to your positions, when the ball appears the game beings!", 5 );
            m_botAction.sendUnfilteredPublicMessage( "*restart" );
            event = true;
            boomBall = true;
        } else  m_botAction.sendPrivateMessage( name, "Event already in progress." );
    }

    public void stopBoomBall( String name, String message ) {
        if( boomBall ) {
            m_botAction.cancelTasks();
            players.clear();
            event = false;
            boomBall = false;
            m_botAction.sendPrivateMessage( name, "Boomball handling has been stopped." );
        } else m_botAction.sendPrivateMessage( name, "Boomball is not in progress." );
    }

    public void handleEvent( Message event ){
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( opList.isER( name ))
                handleCommand( name, message );
        }
    }

    public void handleEvent( FrequencyChange event ) {
        if( boomBall ) {
            Player theKilled = m_botAction.getPlayer( event.getPlayerID() );
            if( theKilled == null ) return;
            String killed    = theKilled.getPlayerName();
            int    freq      = theKilled.getFrequency();
            if( !players.containsKey( killed ) ) {
                MiscTask thisDeath = new MiscTask( killed, freq );
                m_botAction.scheduleTask( thisDeath, 1500 );
                players.put( killed, killed );
            }
        }
    }

    public void handleEvent( FrequencyShipChange event ) {
        Player theKilled = m_botAction.getPlayer( event.getPlayerID() );
        if( theKilled == null ) return;
        String killed    = theKilled.getPlayerName();
        int    freq      = theKilled.getFrequency();
        if( tugAWar && !players.containsKey( killed ) ) {
            MiscTask thisDeath = new MiscTask( killed, freq );
            m_botAction.scheduleTask( thisDeath, tug_TimeDelay / 5 );
            players.put( killed, killed );
        }
        else if( boomBall ) {
            if( !players.containsKey( killed ) ) {
                MiscTask thisDeath = new MiscTask( killed, freq );
                m_botAction.scheduleTask( thisDeath, 1500 );
                players.put( killed, killed );
            }
        }
    }

    public void handleEvent( PlayerDeath event ) {
        Player theKilled  = m_botAction.getPlayer( event.getKilleeID() );
        String killed 	  = theKilled.getPlayerName();
        int    freq 	  = theKilled.getFrequency();
        if( tugAWar ) {
            if( killed.equals( capZero ) ) {
                capZeroD++;
                if( capZeroD >= deathLimit ) {
                    m_botAction.sendArenaMessage( capZero + " the cap of freq 0 has died! Congrats to " + capOne + " and his team!", 5 );
                    stopTugAWar( "-  -", "-  -" );
                } else {
                    m_botAction.sendArenaMessage( capZero + " died and has " + ( deathLimit - capZeroD ) + " deaths left.", 22 );
                    registerDeath( killed, freq );
                }
            } else if( killed.equals( capOne ) ) {
                capOneD++;
                if( capOneD >= deathLimit ) {
                    m_botAction.sendArenaMessage( capOne + " the cap of freq 1 has died! Congrats to " + capZero + " and his team!", 5 );
                    stopTugAWar( "-  -", "-  -" );
                } else {
                    m_botAction.sendArenaMessage( capOne + " died and has " + ( deathLimit - capOneD ) + " deaths left.", 22 );
                    registerDeath( killed, freq );
                }
            } else registerDeath( killed, freq );
        }
        else if( boomBall ) {
            if( !players.containsKey( killed ) ) {
                MiscTask thisDeath = new MiscTask( killed, freq );
                m_botAction.scheduleTask( thisDeath, 1500 );
                players.put( killed, killed );
            }
            //if( !players.containsKey( "Check__Players" ) ) {
            //	MiscTask thisCheck = new MiscTask( "Check__Players", -1 );
            //	m_botAction.scheduleTask( thisCheck, 5000 );
            //	players.put( "Check__Players", "checking" );
            //}
        }
    }

    public void registerDeath( String name, int freq ) {
        if( !players.containsKey( name ) ) {
            MiscTask thisDeath = new MiscTask( name, freq );
            m_botAction.scheduleTask( thisDeath, tug_TimeDelay );
            players.put( name, name );
        }
    }

    public String[] getModHelpMessage() {
        String[] helps = {
                "------ Commands for the misc module -------",
                "!start tugawar    - Starts game for ?go tugawar",
                "!start tugawar <deaths>",
                "!stop tugawar     - Stops game for ?go tugawar",
                "!start boomball   - Starts game for ?go boomball",
                "!stop boomball    - Stops game for ?go boomball"
        };
        return helps;
    }

    public void cancel() {
        m_botAction.cancelTasks();
    }

    public boolean isUnloadable() {
        return true;
    }


    public class MiscTask extends TimerTask {

        String thisName;
        int	   thisFreq;

        public MiscTask( String name, int freq ) {
            thisName = name;
            thisFreq = freq;
        }

        public void run() {
            if( tugAWar ) {
                if( thisFreq == 0 )
                    m_botAction.warpTo( thisName, 419, 422 );
                else {
                    m_botAction.warpTo( thisName, 603, 422 );
                }
                players.remove( thisName );
            } else if( boomBall ) {
                if( thisFreq == -1 ) {
                    int doormode = 0;
                    for( int i=0; i < 6; i++ )
                        areas[i] = 0;
                    //Checks player positions to toggle doors
                    Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
                    if( i == null ) return;
                    while( i.hasNext() ){
                        Player player = (Player)i.next();
                        int    xpos	  = player.getXLocation();
                        int    ypos   = player.getYLocation();
                        //m_botAction.sendArenaMessage( curName + "   " + xpos + "," + ypos );
                        if( xpos > 6960 && xpos < 9392 && ypos < 8712 && ypos > 7690 ) {
                            if( xpos > 8824 ) areas[0]++;
                            else if( xpos > 8504 ) areas[1]++;
                            else if( xpos > 8184 ) areas[2]++;
                            else if( xpos > 7864 ) areas[3]++;
                            else if( xpos > 7544 ) areas[4]++;
                            else areas[5]++;
                            //m_botAction.sendArenaMessage( "Test: " + areas[0]+areas[1]+areas[2]+areas[3]+areas[4]+areas[5] );
                        }
                    }
                    if( areas[0] > 2 ) doormode += 128;
                    if( areas[1] > 2 ) doormode += 64;
                    if( areas[2] > 2 ) doormode += 32;
                    if( areas[3] > 2 ) doormode += 16;
                    if( areas[4] > 2 ) doormode += 8;
                    if( areas[5] > 2 ) doormode += 4;
                    m_botAction.setDoors( doormode );
                }
                if( thisFreq == 0 )
                    m_botAction.warpTo( thisName, 511, 549 );
                else {
                    m_botAction.warpTo( thisName, 511, 474 );
                }
                players.remove( thisName );
            }
        }
    }

}