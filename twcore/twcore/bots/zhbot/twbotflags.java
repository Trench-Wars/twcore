package twcore.bots.zhbot;

import java.util.*;
import twcore.core.*;

public class twbotflags extends TWBotExtension {
	
	boolean turret = true;
	boolean fire = false;
	double speed = 4000;
	int weapon = 1;
	int		id = -1;
	long time = 0;
	int ourX = 0, ourY = 0;
	Random generator = new Random();	
	public twbotflags() {
	}
	
	public void handleCommand( String name, String message ) {
        if( message.toLowerCase().startsWith( "!flags" )) showFlagDetails();
        if( message.toLowerCase().startsWith( "!move" )) moveFlags( message );
        if( message.toLowerCase().startsWith( "!attach" )) turretPlayer( message.substring( 8, message.length() ) );
        if( message.toLowerCase().startsWith( "!unattach" )) unAttach();
        if( message.toLowerCase().startsWith( "!setship" )) setShip( message );
        if( message.toLowerCase().startsWith( "!setfreq" )) setFreq( message );
        if( message.toLowerCase().startsWith( "!fire" ) ) fire = !fire;
        if( message.toLowerCase().startsWith( "!shoot" ) ) shoot( message );
        if( message.toLowerCase().startsWith( "!speed" ) ) {
        	try { speed = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e ){}
        }
        if( message.toLowerCase().startsWith( "!weapon" ) ) {
        	try { weapon = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e ){}
        }
        if( message.toLowerCase().startsWith( "!lvl" ) ) {
        	m_botAction.sendPrivateMessage( name, "getting lvl" );
        	//m_botAction.getLvl();
        }
    }
    
    public void handleEvent( Message event ) {
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name ) ) {
                handleCommand( name, message );
            }
        }
    }
    
    public void showFlagDetails() {
    try {
		Iterator it = m_botAction.getFlagIDIterator();
		while( it.hasNext() ) {
			Flag f = (Flag)it.next();
			int flagId = f.getFlagID();
			boolean carried = f.carried();
			String player = m_botAction.getPlayerName( f.getPlayerID() );
			if( carried )
				m_botAction.sendPublicMessage( "Flag #" + flagId + " carried by " + player );
			else
				m_botAction.sendPublicMessage( "Flag #" + flagId + " at: " + f.getXLocation() + ":" + f.getYLocation() );
		}
	} catch( Exception e ) { m_botAction.sendPublicMessage( ""+e ); }
    }
    
    public void moveFlags( String message ) {
    	try {
	    	String pieces[] = message.split( " " );
	    	int x = 512;
			int y = 512;
			try { 
				x = Integer.parseInt( pieces[1] );
				y = Integer.parseInt( pieces[2] );
			} catch (Exception e) {}
			m_botAction.getShip().setShip( 0 );
	    	Iterator it = m_botAction.getFlagIDIterator();
			while( it.hasNext() ) {
				m_botAction.getShip().setFreq( new Random().nextInt( 9998 ) );
				Flag f = (Flag)it.next();
				if( !f.carried() ) {
					m_botAction.grabFlag( f.getFlagID() );
					m_botAction.moveToTile( x, y );
					m_botAction.dropFlags();
				}
			}
			m_botAction.getShip().setShip( 8 );

		} catch (Exception e) { m_botAction.sendPublicMessage( ""+e ); }
    }
    
    public void shoot( String message ) {
    	int direction = 0;
    	try { direction = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e) {}
    	m_botAction.getShip().rotateDegrees( direction );
    	m_botAction.getShip().fire( weapon );
    	
    }
    
    public void turretPlayer( String name ) {
    	turret = true;
    	m_botAction.getShip().attach( m_botAction.getPlayerID( name ) );
    	id = m_botAction.getPlayerID( name );
    }
    
    public void unAttach() {
    	turret = false;
    	m_botAction.getShip().unattach();
    }
    
    public void setShip( String message ) {
    	int x = 0;
    	try { x = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e) {}
    	m_botAction.getShip().setShip( x );
    }
    
    public void setFreq( String message ) {
    	int x = 0;
    	try { x = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e) {}
    	m_botAction.getShip().setFreq( x );
    }
    
    public void handleEvent( PlayerPosition event ) {
    	if( !fire ) return;
    	//if( !turret ) return;
    	
    	if( id == event.getPlayerID() ) {
    		//m_botAction.sendPublicMessage( "XChange: " +(ourX - event.getXLocation()) + "   XVel: " +event.getXVelocity() +"   TimeChange: " + (System.currentTimeMillis()-time) );
			//time = (System.currentTimeMillis());
			ourX = event.getXLocation();
    		ourY = event.getYLocation();
    		m_botAction.getShip().move( event.getXLocation(), event.getYLocation(), event.getXVelocity(), event.getYVelocity() );
    	} else if( id ==  -1 ){
    		//ourX = 512*16;
    		//ourY = 512*16;
    		//m_botAction.getShip().move( ourX, ourY );
    		
    	}
    	
    	
    	if( (int)(System.currentTimeMillis()/100) - time > 2 ) {
    		time = (int)(System.currentTimeMillis()/100);
    		if( !fire || id == event.getPlayerID() ) return;
    		int degrees = 0;
    		degrees += (int)Math.toDegrees((Math.atan( (event.getYLocation() - ourY + 0.0)/(event.getXLocation()-ourX+0.0) )));
    		int newDegree = (int)Math.toDegrees((Math.atan( (event.getYLocation()+event.getYVelocity() - ourY + 0.0)/(event.getXLocation()+event.getXVelocity()-ourX+0.0) )));
    		int doppler = getDoppler( event, degrees );
    		if( ourX > event.getXLocation() ) { degrees += 180; newDegree += 180; }
    		int adjust = (int)Math.toDegrees( Math.atan( doppler / speed ) );
    		//if( degrees > newDegree ) 	
    		//m_botAction.sendPublicMessage( "Less Than" );
    		//else m_botAction.sendPublicMessage( "More Than" );
    		//m_botAction.sendPublicMessage( "Angle: " + degrees + "  Doppler: " +doppler + "  Adjustment: " + adjust);  		
    		m_botAction.getShip().rotateDegrees( degrees+adjust );
    		m_botAction.getShip().fire( weapon );
    	} 
    }
    
    public int getDoppler( PlayerPosition event, double d ) {
    	int x = (int)(Math.sin( Math.toRadians(d) ) * event.getXVelocity() );
    	int y = (int)(Math.cos( Math.toRadians(d) ) * event.getYVelocity() );
    	int dop = (int)Math.sqrt( Math.pow( x, 2 ) + Math.pow( y, 2 ) ); 
    	//if( Math.sin( Math.toRadians(d) ) * event.getXVelocity() < 0 ) dop *= -1;
    	//if( Math.cos( Math.toRadians(d) ) * event.getYVelocity() < 0 ) dop *= -1;
    	return dop;
    }
	
	
	public void cancel() {
	}
	
	public String[] getHelpMessages() {
		String help[] = {
			"!flags - shows who holds flags",
			"!move x y - moves flags"
		};
		return help;
	}	
	
}