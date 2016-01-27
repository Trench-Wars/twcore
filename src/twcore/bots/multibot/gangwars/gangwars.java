package twcore.bots.multibot.gangwars;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.StringBag;
import twcore.core.util.Tools;


public class gangwars extends MultiModule {

    int m_srcship;
    int m_destship;
    int m_leadership;
    int m_lives;
    StringBag killmsgs;
    boolean isRunning = false;
    boolean modeSet = false;
    HashMap<String, Integer> playerList;
    Vector<String>  queueList;
    String watch = "";
    final int SWITCH_TIME = 2000;
    int lancCops;

    public void init() {
        killmsgs = new StringBag();
        killmsgs.add( "has come to the good side and is now a Cop!" );

        playerList = new HashMap<String, Integer>();
        queueList = new Vector<String>();
    }

    public void requestEvents(ModuleEventRequester events)  {
        events.request(this, EventRequester.PLAYER_POSITION);
        events.request(this, EventRequester.PLAYER_DEATH);
    }

    public void setMode( int srcship, int destship, int leadership, int lives ) {
        m_leadership = leadership;
        m_srcship = srcship;
        m_destship = destship;
        m_lives = lives;
        modeSet = true;
    }

    public void deleteKillMessage( String name, int index ) {

        ArrayList<String> list = killmsgs.getList();

        if( !( 1 <= index && index <= list.size() )) {
            m_botAction.sendPrivateMessage( name, "Error: Can't find the index" );
            return;
        }

        index--;

        if( list.size() > 1 )
            m_botAction.sendPrivateMessage( name, "Removed: " + (String)list.remove( index ));
        else {
            m_botAction.sendPrivateMessage( name, "Sorry, but there must be at least one kill message loaded at all times." );
        }
    }

    public void listKillMessages( String name ) {
        m_botAction.sendPrivateMessage( name, "The following messages are in my posession: " );
        ArrayList<String> list = killmsgs.getList();

        for( int i = 0; i < list.size(); i++ ) {
            if( ((String)list.get( i )).startsWith( "'" )) {
                m_botAction.sendPrivateMessage( name, i + 1 + ". " + "<name>" + (String)list.get( i ));
            } else {
                m_botAction.sendPrivateMessage( name, i + 1 + ". " + "<name> " + (String)list.get( i ));
            }
        }
    }

    public void handleEvent( Message event ) {

        String message = event.getMessage();

        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );

            if( opList.isER( name )) handleCommand( name, message );
        }
    }

    public Integer getTime() {
        return new Integer( (int)(System.currentTimeMillis() / 100) );
    }

    public int getInteger( String input ) {
        try {
            return Integer.parseInt( input.trim() );
        } catch( Exception e ) {
            return 1;
        }
    }

    public void start( String name, String[] params ) {
        try {
            if( params.length == 4 ) {
                int srcship = Integer.parseInt(params[0]);
                int destship = Integer.parseInt(params[1]);
                int leadership = Integer.parseInt(params[2]);
                int lives = Integer.parseInt(params[3]);

                if( lives <= 10 ) {
                    m_botAction.sendPrivateMessage( name, "Lives must be greater than 10.");
                    return;
                }

                setMode( srcship, destship, leadership, lives );
                isRunning = true;
                modeSet = true;
            } else if( params.length == 1 ) {
                int lives = Integer.parseInt(params[0]);
                setMode( 1, 7, 5, lives );
                isRunning = true;
                modeSet = true;
            }
        } catch( Exception e ) {
            m_botAction.sendPrivateMessage( name, "Sorry, you made a mistake, please try again." );
            isRunning = false;
            modeSet = false;
        }
    }

    public void handleCommand( String name, String message ) {
        if( message.startsWith( "!list" )) {
            listKillMessages( name );
        } else if( message.startsWith( "!add " )) {
            addKillMessage( name, message.substring( 5 ));
        } else if( message.startsWith( "!stop" )) {
            m_botAction.sendPrivateMessage( name, "Gangwars mode stopped" );
            isRunning = false;
        } else if( message.startsWith( "!start " )) {
            String[] parameters = Tools.stringChopper( message.substring( 4 ), ' ' );
            start( name, parameters );
            m_botAction.sendPrivateMessage( name, "Gangwars mode started" );
        } else if( message.equals( "!start" )) {
            setMode( 1, 7, 5, 15 );
            isRunning = true;
            modeSet = true;
            m_botAction.sendPrivateMessage( name, "Gangwars mode started" );
        } else if( message.startsWith( "!del " )) {
            deleteKillMessage( name, getInteger( message.substring( 5 )));
        } else if( message.startsWith( "!startwarp" )) {
            m_botAction.warpFreqToLocation( 0, 53, 89 );
            m_botAction.warpFreqToLocation( 1, 48, 1012 );
            m_botAction.warpFreqToLocation( 2, 988, 1012 );
            m_botAction.warpFreqToLocation( 3, 959, 87 );
        }
    }

    public void handleEvent( PlayerPosition event ) {
        String name = m_botAction.getPlayerName( event.getPlayerID() );

        if( !playerList.containsKey( name ) ) {
            playerList.put( name, getTime() );
            queueList.addElement( name );
        }

        //Need to reset players it sees
        resetPlayerQueue( name );
    }

    public void resetPlayerQueue( String name ) {
        for( int i = 0; i < queueList.size(); i++ ) {
            String thisName = queueList.elementAt( i );

            if( thisName.equals( name ) ) {
                queueList.removeElementAt( i );
                queueList.addElement( name );
                i = queueList.size();
            }
        }
    }

    public void addKillMessage( String name, String killMessage ) {
        killmsgs.add( killMessage );
        m_botAction.sendPrivateMessage( name, "<name> " + killMessage + " Added" );
    }

    public void handleEvent( PlayerDeath event ) {
        if( modeSet && isRunning ) {
            Player p = m_botAction.getPlayer( event.getKilleeID() );

            if( p.getLosses() >= 10 && p.getShipType() == m_srcship ) {
                if( lancCops < 3 ) {
                    m_botAction.setShip( event.getKilleeID(), 7 );
                    lancCops++;
                }
                else {
                    m_botAction.setShip( event.getKilleeID(), 3 );
                    lancCops = 0;
                }

                m_botAction.setFreq( event.getKilleeID(), 6739 );
                String killmsg = killmsgs.toString();
                int soundPos = killmsg.indexOf('%');
                int soundCode = 0;

                if( soundPos != -1) {
                    try {
                        soundCode = Integer.parseInt(killmsg.substring(soundPos + 1));
                    } catch( Exception e ) {
                        soundCode = 0;
                    }

                    if(soundCode == 12) {
                        soundCode = 1;   //no naughty sounds
                    }
                }

                if( killmsg.startsWith( "'" ) == false) {
                    killmsg = " " + killmsg;
                }

                if( soundCode > 0 ) {
                    killmsg = killmsg.substring(0, soundPos + 1);
                    m_botAction.sendArenaMessage( m_botAction.getPlayerName( event.getKilleeID() ) + killmsg, soundCode );
                } else {
                    m_botAction.sendArenaMessage( m_botAction.getPlayerName( event.getKilleeID() ) + killmsg );
                }

                //}
            }

            if( p.getLosses() >= m_lives ) {
                m_botAction.sendArenaMessage( m_botAction.getPlayerName( event.getKilleeID() ) + " has died for good after being revived 14 times.");
                m_botAction.spec( event.getKilleeID() );
                m_botAction.spec( event.getKilleeID() );
            }
        }
    }

    public String[] getModHelpMessage() {
        String[] GangwarsHelp = {
            "!list               - Lists the currently loaded kill messages",
            "!add <Kill Message> - Adds a kill message. Use %%<num> at the end to add a sound.",
            "!startwarp          - Warps everyone to their proper start locations.",
            "!del <index>        - Deletes a kill message.  The number for the index is taken from !list",
            "!stop               - Shuts down gangwars mode",
            "!start              - Starts a standard gangwars mode",
            " ",
            " ",
            " ",
            "!start <srcship> <destship> <leadership> <lives> - Starts a special gangwars mode"
        };
        return GangwarsHelp;
    }
    public void cancel() {
    }

    public boolean isUnloadable()   {
        return true;
    }
}
