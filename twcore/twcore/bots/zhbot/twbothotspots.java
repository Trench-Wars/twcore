package twcore.bots.zhbot;

import java.util.*;
import twcore.core.*;

public class twbothotspots extends TWBotExtension {

	HashMap hotSpots;

	HashMap playerList;
	Vector  queueList;
	String watch = "";

	final int SWITCH_TIME = 2000;

	public twbothotspots() {

		hotSpots = new HashMap();

		playerList = new HashMap();
		queueList = new Vector();
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

		Iterator it = hotSpots.keySet().iterator();
		while( it.hasNext() ) {
			String key = (String)it.next();
			HotSpot spot = (HotSpot)hotSpots.get( key );
			if( spot != null )
                if( p != null )
                    if( spot.inside( p.getXLocation(), p.getYLocation() ) )
                        m_botAction.warpTo( name, spot.getX(), spot.getY() );
		}
	}

	public Integer getTime() {
		return new Integer( (int)(System.currentTimeMillis()/100) );
	}

	public void resetPlayerQueue( String name ) {
		for( int i = 0; i < queueList.size(); i++ ) {
			String thisName = (String)queueList.elementAt( i );
			if( thisName.equals( name ) && thisName != null ) {
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
		} catch (Exception e) { return; }

		hotSpots.put( message, new HotSpot( values ) );
		m_botAction.sendPrivateMessage( name, "Hotspot added." );
	}

	public String[] getHelpMessages() {
		String help[] = {
		        "HotSpot Module - the neglected, forgotten module",
		        "!addspot <warpx> <warpy> <radius> <destx> <desty>     - Adds a new hotspot.",
		        "            Players will warp to the coord <destx>,<desty>",
		        "            when they enter within <radius> of <warpx>,<warpy>.",
		        "!watch    - Begin watching all hotspots.",
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

	public HotSpot( int values[] ) {
		x = values[0];
		y = values[1];
		r = values[2];
		x2 = values[3];
		y2 = values[4];
	}

	public boolean inside( int playerX, int playerY ) {

		double dist = Math.sqrt( Math.pow( x*16 - playerX , 2 ) + Math.pow( y*16 - playerY , 2 ) );
		if( dist < r*16 ) return true;
		else return false;
	}

	public int getX() { return x2; }
	public int getY() { return y2; }

}