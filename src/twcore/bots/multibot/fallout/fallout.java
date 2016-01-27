package twcore.bots.multibot.fallout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;

public class fallout extends MultiModule {


    /* Fallout variables */
    HashMap<String, String> players = new HashMap<String, String>();
    final int   arenaRad = 1216;
    int         startTime = 0;
    int         arenaCur = 0;
    int         falloutTime = 30;
    int         teamSize = 1;
    boolean     fallOut = false;

    public void init() {
    }

    public boolean isUnloadable() {
        return !fallOut;
    }
    String[] opmsg = {
        "!start                         - starts a game of fallout with speed of 30 and teams of 1",
        "!start <speed>                 - starts a game of fallout with <speed> and teams of 1",
        "!start <speed> <teamsize>      - starts a game of fallout with <speed> and teams of size <teamsize>",
        "!stop                          - stop/resets a game of fallout in case of error"
    };
    public String[] getModHelpMessage() {
        return opmsg;
    }

    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request( this, EventRequester.PLAYER_POSITION );
    }

    public void handleEvent( Message event ) {
        String message = event.getMessage();

        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );

            if( opList.isER( name ) )
                handleCommand( name, message );
        }
    }

    public void handleCommand( String name, String message ) {
        try {
            if( message.toLowerCase().startsWith( "!start" ) ) {
                int t = 30;
                String pieces[] = message.split( " " );

                try {
                    t = Integer.parseInt( pieces[1] );

                    if( t < 10 ) t = 2;

                    if( t > 60 ) t = 60;

                    teamSize = Integer.parseInt( pieces[2] );

                    if( teamSize < 1 ) teamSize = 1;

                    if( teamSize > 15 ) teamSize = 15;
                } catch (Exception e ) {}

                falloutTime = t;
                startFallout( name, message );
            } else if( message.toLowerCase().startsWith( "!stop" ) ) {
                stopFallout( name, message );
            } else if( message.toLowerCase().startsWith( "!help" ) ) {
                displayHelp( name, message );
            }

        } catch ( Exception e ) {}
    }

    public void displayHelp( String name, String message ) {
        String help[] = {
            "!start                         - starts a game of fallout with speed of 30 and teams of 1",
            "!start <speed>                 - starts a game of fallout with <speed> and teams of 1",
            "!start <speed> <teamsize>      - starts a game of fallout with <speed> and teams of size <teamsize>",
            "!stop                          - stop/resets a game of fallout in case of error",
        };
        m_botAction.privateMessageSpam( name, help );
    }

    public void startFallout( String name, String message ) {
        if( !fallOut ) {
            if( getPlayerCount() > 1 ) {
                m_botAction.toggleLocked();
                m_botAction.changeAllShips( 1 );

                for(int j = 0; j < 7; j++)
                    m_botAction.showObject( j * 10 );

                m_botAction.showObject( 1 );
                m_botAction.sendArenaMessage( "Game begins in 10 seconds!", 22 );

                TimerTask preStartGame = new TimerTask() {
                    public void run() {
                        m_botAction.createRandomTeams( teamSize );
                    }
                };
                m_botAction.scheduleTask( preStartGame, 9000 );

                TimerTask startGame = new TimerTask() {
                    public void run() {
                        for(int i = 1; i < 7; i++)
                            m_botAction.hideObject( i * 10 );

                        fallOut = true;
                        m_botAction.showObject( 2 );
                        m_botAction.sendUnfilteredPublicMessage( "*prize 255" );
                        m_botAction.sendArenaMessage( "Fallout begins now! Beware falling out of the parameter", 104 );
                        m_botAction.sendArenaMessage( " Perimeter reduction every " + falloutTime + " seconds." );
                        startTime = (int)(new java.util.Date().getTime() / 1000);
                    }
                };
                m_botAction.scheduleTask( startGame, 10000 );

                TimerTask prizeEm = new TimerTask() {
                    public void run() {
                        m_botAction.sendUnfilteredPublicMessage( "*prize 20" );
                    }
                };
                m_botAction.scheduleTaskAtFixedRate( prizeEm, 11000, 3000 );

            } else m_botAction.sendPrivateMessage( name, "Need 2 or more players to start fallout." );
        } else m_botAction.sendPrivateMessage( name, "Event already in progress." );
    }

    public void stopFallout( String name, String message ) {
        if( fallOut ) {
            m_botAction.sendArenaMessage( "This round of Fallout has been canceled." );
            m_botAction.shipResetAll();
            m_botAction.toggleLocked();
            m_botAction.hideObject( arenaCur * 10 );
            arenaCur = 0;
            teamSize = 1;
            fallOut = false;
            m_botAction.cancelTasks();
            players.clear();
        } else m_botAction.sendPrivateMessage( name, "Fallout is not in progress." );
    }

    public void handleEvent( PlayerPosition event ) {
        //m_botAction.sendArenaMessage( m_botAction.getPlayer( event.getPlayerID() ).getPlayerName() + "   " + event.getXLocation() +":" + event.getYLocation());
        //System.out.println( m_botAction.getPlayer( event.getPlayerID() ).getPlayerName() + "   " + event.getXLocation() +":" + event.getYLocation());
        if( fallOut ) {
            int currentTime = (int)(new java.util.Date().getTime() / 1000);

            if( currentTime - startTime > falloutTime ) {
                m_botAction.sendUnfilteredPublicMessage( "*prize 40" );

                if(arenaCur != 6) {
                    m_botAction.showObject( 4 );
                    m_botAction.showObject( (arenaCur + 1) * 10 );
                    m_botAction.hideObject( arenaCur * 10 );
                    arenaCur++;
                    m_botAction.sendArenaMessage("Fall Out!");
                    startTime = currentTime;
                }
            }

            Player p = m_botAction.getPlayer( event.getPlayerID() );
            double dist = getCenterDistance( p.getXLocation(), p.getYLocation() );

            if( dist > (arenaRad - arenaCur * 160) && !players.containsKey( p.getPlayerName() ) ) {
                m_botAction.sendArenaMessage( p.getPlayerName() + " has passed the perimeter and is out!" );
                players.put( p.getPlayerName(), p.getPlayerName() );
                m_botAction.spec( p.getPlayerName() );
                m_botAction.spec( p.getPlayerName() );
            }

            if( getPlayerCount() < 2 ) doHandleWin();

        }
    }

    public double getCenterDistance( int x, int y ) {
        double dist = Math.sqrt( Math.pow(( 8192 - x ), 2) + Math.pow(( 8192 - y ), 2) );
        return dist;
    }

    public void doHandleWin() {
        String winner = "I (the best bot around town)";
        Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
        int i = 0;

        if( it != null ) {
            if( it.hasNext() ) {
                winner = "";

                while( it.hasNext() ) {
                    Player player = (Player)it.next();
                    winner += player.getPlayerName() + " and ";
                    i++;
                }

                try {
                    winner = winner.substring( 0, winner.length() - 5 );  // Remove the tailing " and "
                } catch (Exception e) {
                    winner = "I (the best bot around town)";
                }
            }
        }

        m_botAction.showObject( 3 );

        if( i == 1 )
            m_botAction.sendArenaMessage( winner + " has won this round of Fallout!", 5 );
        else
            m_botAction.sendArenaMessage( winner + " have won this round of Fallout!", 5 );

        m_botAction.cancelTasks();
        m_botAction.shipResetAll();
        m_botAction.toggleLocked();
        m_botAction.hideObject( arenaCur * 10 );
        players.clear();
        arenaCur = 0;
        teamSize = 1;
        fallOut = false;
    }

    public int getPlayerCount() {
        int freq = -1;
        int i = 1;
        Iterator<Player> it = m_botAction.getPlayingPlayerIterator();

        if( it == null ) return 0;

        while( it.hasNext() ) {
            Player p = (Player)it.next();

            if( freq == -1 )
                freq = p.getFrequency();

            if( p.getFrequency() != freq )
                i++;
        }

        return i;
    }

    public void cancel() {
        m_botAction.cancelTasks();
    }
}