package twcore.bots.multibot.util;

import java.util.Iterator;
import java.util.Random;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Flag;
import twcore.core.util.ModuleEventRequester;

/**
 * For tracking flags, and (for some reason) controlling the bot in-game. 
 *
 */
public class utilflags extends MultiUtil {

	boolean turret = true;
	boolean fire = false;
	double speed = 4000;
	int weapon = 1;
	int		id = -1;
	long time = 0;
	int ourX = 0, ourY = 0;
	Random generator = new Random();
    
	public void init() {
	}

      /**
       * Requests events.
       */
      public void requestEvents( ModuleEventRequester modEventReq ) {
          modEventReq.request(this, EventRequester.PLAYER_POSITION );
      }

    
	public void handleCommand( String name, String message ) {
        if( message.toLowerCase().startsWith( "!flags" )) showFlagDetails();
        if( message.toLowerCase().startsWith( "!move" )) moveFlags( message );
        if( message.toLowerCase().startsWith( "!botattach" )) turretPlayer( message.substring( 11, message.length() ) );
        if( message.toLowerCase().startsWith( "!botunattach" )) unAttach();
        if( message.toLowerCase().startsWith( "!setbotship" )) setShip( message );
        if( message.toLowerCase().startsWith( "!setbotfreq" )) setFreq( message );
        if( message.toLowerCase().startsWith( "!fire" ) ) fire = !fire;
        if( message.toLowerCase().startsWith( "!shoot" ) ) shoot( message );
        if( message.toLowerCase().startsWith( "!speed" ) ) {
        	try { speed = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e ){}
        }
        if( message.toLowerCase().startsWith( "!weapon" ) ) {
        	try { weapon = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e ){}
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

    	if( id == event.getPlayerID() ) {
			ourX = event.getXLocation();
    		ourY = event.getYLocation();
    		m_botAction.getShip().move( event.getXLocation(), event.getYLocation(), event.getXVelocity(), event.getYVelocity() );
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
    		m_botAction.getShip().rotateDegrees( degrees+adjust );
    		m_botAction.getShip().fire( weapon );
    	}
    }

    public int getDoppler( PlayerPosition event, double d ) {
    	int x = (int)(Math.sin( Math.toRadians(d) ) * event.getXVelocity() );
    	int y = (int)(Math.cos( Math.toRadians(d) ) * event.getYVelocity() );
    	int dop = (int)Math.sqrt( Math.pow( x, 2 ) + Math.pow( y, 2 ) );
    	return dop;
    }


	public void cancel() {
	}

	public String[] getHelpMessages() {
		String help[] = {
            "Flags Utility",
			"!flags    - shows who holds flags",
			"!move x y - moves all flags to coord x:y",
            "Secret commands:",
            "!setbotship <ship>     - Place the bot in a ship",
            "!setbotfreq <freq>     - Set the bot to a specific freq",
            "!botattach <player>    - Attach bot to player, if in game",
            "!botunattach           - Detach the bot",
            "!fire                  - Toggles firing at close players",
            "!shoot <degree>        - Fires in direction of degree (0-359)",
            "!speed <speed>         - Sets how fast bot tracks using !fire",
            "!weapon <weapon>       - Changes to weapon # (16 bit vector)"
		};
		return help;
	}

}