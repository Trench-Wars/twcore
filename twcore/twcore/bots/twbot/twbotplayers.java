package twcore.bots.twbot;

import java.util.*;
import twcore.core.*;

/** A TWBot Extension for ?go elimination.  ER passes bot someone to be the
 * eliminator, who is the only person allowed to touch the ball -- everyone
 * else is spec'd when they do so.  Last remaining besides eliminator wins.
 *
 * @version 1.8
 * @author qan
 */
public class twbotplayers extends TWBotExtension {

    public twbotplayers() {
    }


    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name ))
                handleCommand( name, message );
            else
                handleGeneralCommand( name, message );
        }
    }



    public void handleCommand( String name, String message ){
        if(message.startsWith("!players")) {
        	Iterator it = m_botAction.getPlayingPlayerIterator();
        	while(it.hasNext()) {
        		m_botAction.sendPublicMessage(((Player)it.next()).getPlayerName());
        	}
        } else if(message.startWith("!")) {
        	m_botAction.sendPublicMessage(m_botAction.getFuzzyPlayerName(message.substring(1)));
        }


    }


    public String[] getHelpMessages() {
        String[] ballspecHelp = {
            ""         
        };
        return ballspecHelp;
    }


    
    /** (blank method)
     */
    public void cancel() {
    }
}


