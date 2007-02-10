package twcore.bots.twbot;

import java.util.*;

import twcore.bots.TWBotExtension;
import twcore.core.*;
import twcore.core.events.Message;

public class twbotplayers extends TWBotExtension {

    public twbotplayers() {
    	m_botAction.sendUnfilteredPublicMessage("?chat=trivia");
    }


    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.CHAT_MESSAGE ){
            String name = event.getMessager();
            if(name.startsWith("RoboBot")) {
            	System.out.println(message);
            }
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


