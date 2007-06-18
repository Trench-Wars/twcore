package twcore.bots.multibot.util;

import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

public class utilhotspots extends MultiUtil {

    Vector <HotSpot>hotSpots;
    HotSpot watch;

    private int switchTime = 500;

    public void init() {
        hotSpots = new Vector<HotSpot>();
        // Turn off updating -- we will do it ourselves
        m_botAction.setPlayerPositionUpdating(0);
    }
    
    public void requestEvents( ModuleEventRequester modEventReq ) {
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
        } else if( message.toLowerCase().startsWith( "!switchtime " ) ) {
            do_switchTime( name, message.substring( 12, message.length() ) );
        } else if( message.toLowerCase().startsWith( "!watch" ) ) {
            TimerTask change = new TimerTask() {
                public void run() {
                    try {
                        watch = hotSpots.elementAt(0);
                        hotSpots.removeElementAt(0);
                        hotSpots.addElement( watch );
                    } catch (Exception e) {}
                    m_botAction.moveToTile(watch.getX(), watch.getY());
                }
            };
            m_botAction.scheduleTaskAtFixedRate( change, 2000, switchTime );
        }
    }

    public void handleEvent( PlayerPosition event ) {

        Player p = m_botAction.getPlayer( event.getPlayerID() );
        String name = m_botAction.getPlayerName( event.getPlayerID() );

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

    public void do_switchTime( String name, String message ) {
        int time = switchTime;
        try {
            time = Integer.parseInt( message );
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( name, "Input error.  Need a number!" );
            return;
        }
        if( time < 200 ) {
            m_botAction.sendPrivateMessage( name, "Time can not be less than 200ms." );
            return;
        }
        
        switchTime = time;
        m_botAction.sendPrivateMessage( name, "Switch time set to " + time );
    }

    public String[] getHelpMessages() {
        String help[] = {
                "HotSpot Module - 'Enter one coordinate, warp to another; maybe get a prize.'",
                "!addspot <warpx> <warpy> <radius> <destx> <desty>     - Adds a new hotspot.",
                "            Players will warp to the coord <destx>,<desty>",
                "            when they enter within <radius> of <warpx>,<warpy>.",
                "!prizespot <warpx> <warpy> <radius> <destx> <desty> <prize>" +
                "            - Same as !addspot, but players are given <prize> after warping.",
                "!switchtime <ms> - How long to watch each spot before moving to the next",
                "!watch      - Begin watching all hotspots.",
                "!clearspots - Remove all hotspots.",
                "(Use !warp module's !where command to locate your hotspots!)"
        };
        return help;
    }

    public void cancel() {
        m_botAction.setPlayerPositionUpdating(5000);
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