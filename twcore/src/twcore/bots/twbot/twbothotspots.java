package twcore.bots.twbot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.TWBotExtension;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;

public class twbothotspots extends TWBotExtension {

    HashSet <HotSpot>hotSpots;

    HashMap <String,Integer>playerList;
    Vector  <String>queueList;
    String watch = "";

    final int SWITCH_TIME = 2000;

    public twbothotspots() {

        hotSpots = new HashSet<HotSpot>();

        playerList = new HashMap<String,Integer>();
        queueList = new Vector<String>();
    }

    public void handleEvent( Message event ){

        String message = event.getMessage();

        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name ))
                handleCommand( name, message );
        }
    }

    public void handleCommand( String name, String message ) {

        if( message.toLowerCase().startsWith( "!addspot " ) ) {
            do_addHotSpot( name, message.substring( 9, message.length() ) );
        } else if( message.toLowerCase().startsWith( "!prizespot " ) ) {
            do_addPrizeHotSpot( name, message.substring( 11, message.length() ) );
        } else if( message.toLowerCase().startsWith( "!clearspots" ) ) {
            do_clearHotSpots( name );
        } else if( message.toLowerCase().startsWith( "!watch" ) ) {
            TimerTask change = new TimerTask() {
                public void run() {
                    try {
                        watch = (String)queueList.elementAt(0);
                        queueList.removeElementAt(0);
                        queueList.addElement( watch );
                    } catch (Exception e) {}
                    m_botAction.spectatePlayer( watch );
                }
            };
            m_botAction.scheduleTaskAtFixedRate( change, 2000, SWITCH_TIME );
        }
    }

    public void handleEvent( PlayerEntered event ) {

        Player p = m_botAction.getPlayer( event.getPlayerID() );
        String name = m_botAction.getPlayerName( event.getPlayerID() );

        if( p.getShipType() == 0 ) return;
        if( !playerList.containsKey( name ) ) {
            playerList.put( name, getTime() );
            queueList.addElement( name );
        }
    }

    public void handleEvent( PlayerLeft event ) {

        String name = m_botAction.getPlayerName( event.getPlayerID() );

        if( playerList.containsKey( name ) )
            playerList.remove( name );
    }

    public void handleEvent( FrequencyChange event ) {

        Player p = m_botAction.getPlayer( event.getPlayerID() );
        String name = m_botAction.getPlayerName( event.getPlayerID() );

        if( p.getShipType() == 0 ) return;
        if( !playerList.containsKey( name ) ) {
            playerList.put( name, getTime() );
            queueList.addElement( name );
        }
    }

    public void handleEvent( FrequencyShipChange event ) {

        Player p = m_botAction.getPlayer( event.getPlayerID() );
        String name = m_botAction.getPlayerName( event.getPlayerID() );

        if( p.getShipType() == 0 ) {
            if( playerList.containsKey( name ) )
                playerList.remove( name );
        } else {
            if( !playerList.containsKey( name ) ) {
                playerList.put( name, getTime() );
                queueList.addElement( name );
            }
        }
    }

    public void handleEvent( PlayerPosition event ) {

        Player p = m_botAction.getPlayer( event.getPlayerID() );
        String name = m_botAction.getPlayerName( event.getPlayerID() );

        if( !playerList.containsKey( name ) ) {
            playerList.put( name, getTime() );
            queueList.addElement( name );
        }

        //Need to reset players it sees
        if( name != null)
            resetPlayerQueue( name );

        Iterator it = hotSpots.iterator();
        while( it.hasNext() ) {
            HotSpot spot = (HotSpot)it.next();
            if( spot != null )
                if( p != null )
                    if( spot.inside( p.getXLocation(), p.getYLocation() ) ) {
                        m_botAction.warpTo( name, spot.getX(), spot.getY() );
                        if( spot.getPrize() != -1 )
                            m_botAction.specificPrize( name, spot.getPrize() );
                    }
        }
    }

    public Integer getTime() {
        return new Integer( (int)(System.currentTimeMillis()/100) );
    }

    public void resetPlayerQueue( String name ) {
        for( int i = 0; i < queueList.size(); i++ ) {
            String thisName = (String)queueList.elementAt( i );
            if( thisName != null && thisName.equals( name ) ) {
                queueList.removeElementAt( i );
                queueList.addElement( name );
                i = queueList.size();
            }
        }
    }

    public void do_addHotSpot( String name, String message ) {

        String pieces[] = message.split( " " );
        if( pieces.length != 5 ) return;

        int values[] = new int[5];
        try {
            for( int i = 0; i < 5; i++ )
                values[i] = Integer.parseInt( pieces[i] );
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( name, "Input error.  Check and try again." );
            return;
        }

        hotSpots.add( new HotSpot( values ) );
        m_botAction.sendPrivateMessage( name, "Hotspot added." );
    }

    public void do_addPrizeHotSpot( String name, String message ) {

        String pieces[] = message.split( " " );
        if( pieces.length != 6 ) return;

        int values[] = new int[6];
        try {
            for( int i = 0; i < 6; i++ )
                values[i] = Integer.parseInt( pieces[i] );
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( name, "Input error.  Check and try again." );
            return;
        }

        hotSpots.add( new HotSpot( values ) );
        m_botAction.sendPrivateMessage( name, "Prize hotspot added." );
    }
    
    /**
     * Clears all hotspots.
     * @param name
     */
    public void do_clearHotSpots( String name ) {
        hotSpots.clear();
        m_botAction.sendPrivateMessage( name, "All hotspots cleared." );
    }
    
    public String[] getHelpMessages() {
        String help[] = {
                "HotSpot Module - 'Enter one coordinate, warp to another; maybe get a prize.'",
                "!addspot <warpx> <warpy> <radius> <destx> <desty>     - Adds a new hotspot.",
                "            Players will warp to the coord <destx>,<desty>",
                "            when they enter within <radius> of <warpx>,<warpy>.",
                "!prizespot <warpx> <warpy> <radius> <destx> <desty> <prize>" +
                "            - Same as !addspot, but players are given <prize> after warping.",
                "!watch      - Begin watching all hotspots.",
                "!clearspots - Remove all hotspots.",
                "(Use !warp module's !where command to locate your hotspots!)"
        };
        return help;
    }

    public void cancel() {
    }
}

class HotSpot {

    int x;
    int y;
    int r;
    int x2;
    int y2;
    int prize;

    public HotSpot( int values[] ) {
        if(values.length == 5) {
            x = values[0];
            y = values[1];
            r = values[2];
            x2 = values[3];
            y2 = values[4];
            prize = -1;
        } else {
            x = values[0];
            y = values[1];
            r = values[2];
            x2 = values[3];
            y2 = values[4];
            prize = values[5];            
        }
    }

    public boolean inside( int playerX, int playerY ) {

        double dist = Math.sqrt( Math.pow( x*16 - playerX , 2 ) + Math.pow( y*16 - playerY , 2 ) );
        if( dist < r*16 ) return true;
        else return false;
    }

    public int getX() { return x2; }
    public int getY() { return y2; }
    public int getPrize() { return prize; }
}