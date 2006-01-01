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
	int ship = 1;
	int freq = 0;
	int sFreq = 5239;
	int deaths = 10;
	boolean staffVsWorld = true;
	
    public twbotplayers() {
    }


    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isZH( name ))
                handleCommand( name, message, m_opList.isZH( name ) );
        }
    }



    public void handleCommand( String name, String message, boolean staff ){
        if(message.startsWith("!late")) {
        	if(!staff || !staffVsWorld) {
        		Iterator it = m_botAction.getPlayingPlayerIterator();
        		int highestDeaths = 0;
	        	while(it.hasNext()) {
	        		Player p = (Player)it.next();
	        		int deaths = p.getLosses();
	        		if(!m_botAction.getOperatorList().isZH(p.getPlayerName()) && !staffVsWorld)
	        			if(deaths > highestDeaths) highestDeaths = deaths;
	        	}
	        	m_botAction.sendPrivateMessage(m_botAction.getBotName(), "!specplayer "+name+":"+(deaths - highestDeaths));
	        	m_botAction.setShip(name, ship);
	        	m_botAction.setFreq(name, freq);
	        } else if(staffVsWorld) {
	        	m_botAction.setShip(name, ship);
	        	m_botAction.setFreq(name, sFreq);	        	
	        }
        } else if(message.startsWith("!ship ") && staff) {
        	try {
        		ship = Integer.parseInt(message.substring(6));
        	} catch(Exception e) {}
        } else if(message.startsWith("!freq ") && staff) {
        	try {
        		freq = Integer.parseInt(message.substring(6));
        	} catch(Exception e) {}
        } else if(message.startsWith("!sfreq ") && staff) {
        	try {
        		sFreq = Integer.parseInt(message.substring(7));
        	} catch(Exception e) {}
        } else if(message.startsWith("!deaths ") && staff) {
        	try {
        		deaths = Integer.parseInt(message.substring(8));
        	} catch(Exception e) {}
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


