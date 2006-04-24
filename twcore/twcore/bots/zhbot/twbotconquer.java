/*
 * portabotTestModule.java
 *
 * Created on March 21, 2002, 4:14 PM
 */

/**
 *
 * @author  harvey
 */
package twcore.bots.zhbot;

import twcore.core.*;

public class twbotconquer extends TWBotExtension {
    /** Creates a new instance of portabotTestModule */
    public twbotconquer() {
    }    
    
    boolean isRunning = false;        
    
    public void handleEvent( PlayerDeath event ){
        if( !isRunning ) return;
        
        Player killer = m_botAction.getPlayer( event.getKillerID() );
        if( killer == null )
            return;
        
        m_botAction.setFreq( event.getKilleeID(), killer.getFrequency() );
        String killeename = m_botAction.getPlayerName( event.getKilleeID() );
        String killername = m_botAction.getPlayerName( event.getKillerID() );
        m_botAction.sendArenaMessage( killeename + " has been conquered by "
        + killername + " and now joins freq " + killer.getFrequency() );
    }
        
    public void handleEvent( Message event ){
        
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name )) handleCommand( name, message );
        }
    }
    
    public void handleCommand( String name, String message ){
        if( message.startsWith( "!start" )){
            m_botAction.sendArenaMessage( "Conquer mode activated by " + name );
            isRunning = true;
        } else if( message.startsWith( "!stop" )){
            m_botAction.sendArenaMessage( "Conquer mode deactivated by " + name );
            isRunning = false;
        }
    }
    
    public String[] getHelpMessages() {
        String[] help = {
            "!start - starts conquer mode",
            "!stop - stops conquer mode"
        };
        return help;
    }
    
    public void cancel() {
    }    
    
}
