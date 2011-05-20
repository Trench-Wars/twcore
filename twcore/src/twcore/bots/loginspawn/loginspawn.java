/**
 * 
 */
package twcore.bots.loginspawn;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.*;

/**
 * @author WingZero
 *
 */
public class loginspawn extends SubspaceBot {

    BotAction ba;
    public loginspawn(BotAction botAction) {
        super(botAction);
        ba = m_botAction;
        EventRequester er = ba.getEventRequester();
        er.request(EventRequester.MESSAGE);
        er.request(EventRequester.LOGGED_ON);
        er.request(EventRequester.ARENA_JOINED);
    }
    
    
    public void handleEvent(Message event) {        
        String msg = event.getMessage();
        if (event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE) {
            if (msg.equals("!die")) {
                ba.die();
            }
        }
    }
    
    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena("#robopark");
    }
    
    public void handleEvent(ArenaJoined event) {
        ba.die();
    }
}
