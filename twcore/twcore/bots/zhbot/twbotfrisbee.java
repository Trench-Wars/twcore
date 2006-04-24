package twcore.bots.zhbot;

import java.util.*;
import twcore.core.*;


public class twbotfrisbee extends TWBotExtension {
	
	String lastHolder = "";
	boolean isRunning = false;
	int oldX = 0;
	int oldY = 0;
	int oldTimeStamp = -1;
	
    public twbotfrisbee() {
    }


    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name ))
                handleCommand( name, message );
        }
    }

    /** Handles all commands given to the bot.
     * @param name Name of ER or above who sent the command.
     * @param message Message sent
     */
    public void handleCommand( String name, String message ){
    	if(message.toLowerCase().startsWith("!start")) {
    		if(!isRunning) {
    			lastHolder = "";
    			m_botAction.sendPrivateMessage(name, "Ultimate Frisbee started, ball being refreshed.");
    			isRunning = true;
    			m_botAction.sendUnfilteredPublicMessage("*restart");
    		} else {
    			m_botAction.sendPrivateMessage(name, "Ultimate Frisbee is already running.");
    		}
    	} else if(message.toLowerCase().startsWith("!stop")) {
    		if(isRunning) {
    			m_botAction.sendPrivateMessage(name, "Ultimate Frisbee stopped.");
    			isRunning = false;
    		} else {
    			m_botAction.sendPrivateMessage(name, "Ultimate Frisbee is not running.");
    		}
    	}
    }
    
    public void handleEvent(FrequencyShipChange event) {
    	m_botAction.specificPrize(event.getPlayerID(), 12);
    }

    public void handleEvent( BallPosition event ){
		try {
        Player p;
        int numPlayers = 0;
        Iterator i;
        String pname;
        System.out.println(event.getTimeStamp());
		System.out.println(event.getXLocation());
		System.out.println(event.getYLocation());
        if( isRunning ) {
            p = m_botAction.getPlayer( event.getPlayerID() );
            pname = m_botAction.getPlayerName( event.getPlayerID() );
            if(pname.equals(lastHolder) && (event.getXLocation() != oldX || event.getYLocation() != oldY)) {
            	m_botAction.specificPrize(pname, 12);
            	lastHolder = "";
            	oldTimeStamp = event.getTimeStamp();
            } else if(event.getTimeStamp() != oldTimeStamp) {
				m_botAction.specificPrize(pname, -12);
				oldX = event.getXLocation();
				oldY = event.getYLocation();
				lastHolder = pname;
			}
        }
        } catch(Exception e) { e.printStackTrace();
        }
    }

    /** Returns help message.
     * @return A string array containing help msgs for this bot.
     */
    public String[] getHelpMessages() {
        String[] ballspecHelp = {
            "!start              - Starts Ultimate Frisbee mode.",
            "!stop               - Ends Ultimate Frisbee mode."          
        };
        return ballspecHelp;
    }


    
    /** (blank method)
     */
    public void cancel() {
    }
}


