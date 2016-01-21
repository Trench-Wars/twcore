package twcore.bots.multibot.util;

import java.util.Iterator;
import java.util.Random;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.game.Flag;
import twcore.core.util.ModuleEventRequester;

/**
    For tracking flags.  Bot-controlling commands moved to utiletc.

*/
public class utilflags extends MultiUtil {

    boolean turret = true;
    boolean fire = false;
    double speed = 4000;
    int weapon = 1;
    int     id = -1;
    long time = 0;
    int ourX = 0, ourY = 0;
    Random generator = new Random();

    public void init() {
    }

    /**
        Requests events.
    */
    public void requestEvents( ModuleEventRequester modEventReq ) {
    }


    public void handleCommand( String name, String message ) {
        if( message.toLowerCase().startsWith( "!flags" )) showFlagDetails();

        if( message.toLowerCase().startsWith( "!move" )) moveFlags( message );
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
            Iterator<Integer> it = m_botAction.getFlagIDIterator();

            while( it.hasNext() ) {
                Flag f = m_botAction.getFlag(it.next());
                int flagId = f.getFlagID();
                boolean carried = f.carried();
                String player = m_botAction.getPlayerName( f.getPlayerID() );

                if( carried )
                    m_botAction.sendPublicMessage( "Flag #" + flagId + " carried by " + player );
                else
                    m_botAction.sendPublicMessage( "Flag #" + flagId + " at: " + f.getXLocation() + ":" + f.getYLocation() );
            }
        } catch( Exception e ) {
            m_botAction.sendPublicMessage( "" + e );
        }
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
            Iterator<Integer> it = m_botAction.getFlagIDIterator();

            while( it.hasNext() ) {
                m_botAction.getShip().setFreq( new Random().nextInt( 9998 ) );
                Flag f = m_botAction.getFlag(it.next());

                if( !f.carried() ) {
                    m_botAction.grabFlag( f.getFlagID() );
                    m_botAction.moveToTile( x, y );
                    m_botAction.dropFlags();
                }
            }

            m_botAction.getShip().setShip( 8 );

        } catch (Exception e) {
            m_botAction.sendPublicMessage( "" + e );
        }
    }

    public void cancel() {
    }

    public String[] getHelpMessages() {
        String help[] = {
            "Flags Utility",
            "!flags    - shows who holds flags",
            "!move x y - moves all flags to coord x:y",
        };
        return help;
    }

}