/*
 * bouncerbot.java
 *
 * Created on January 21, 2002, 12:57 AM
 */

/**
 *
 * @author  harvey
 */
package twcore.bots.bouncerbot;

import twcore.core.*;
import java.util.*;
import java.io.*;
public class bouncerbot extends SubspaceBot {
    OperatorList m_opList;
    HashSet invitedPlayers;
    ArrayList log;
    String bouncemessage;
    public bouncerbot( BotAction botAction ){
        super( botAction );
        invitedPlayers = new HashSet();
        bouncemessage = "Entering a private arena without being invited is against the rules.  Goodbye!";
        log = new ArrayList();
        EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.MESSAGE );
        events.request( EventRequester.PLAYER_ENTERED );
    }
    
    public void logEvent( String event ){
        Calendar c = Calendar.getInstance();
        String timestamp = c.get( c.MONTH ) + "/" + c.get( c.DAY_OF_MONTH )
        + "/" + c.get( c.YEAR ) + ": " + c.get( c.HOUR ) + ":"
        + c.get( c.MINUTE ) + ":" + c.get( c.SECOND ) + " - ";
        try{
            PrintWriter out = new PrintWriter( new FileWriter( "bouncerbot.log" ));
            log.add( timestamp + event );
            for( Iterator i = log.iterator(); i.hasNext(); ){
                out.println( (String)i.next() );
            }
            out.close();
        }catch(Exception e){
            Tools.printStackTrace( e );
        }
    }
    
    public void handleEvent( PlayerEntered event ){
        if( m_opList.isSmod( event.getPlayerName() )) return;
        if( invitedPlayers.contains( event.getPlayerName().toLowerCase())) return;
        if( !m_opList.isModerator( event.getPlayerName() )) return;
        m_botAction.sendPrivateMessage( event.getPlayerName(), bouncemessage );
        m_botAction.sendUnfilteredPrivateMessage( event.getPlayerName(), "*kill" );
        m_botAction.sendPublicMessage( "Whoops, " + event.getPlayerName() + " entered without permission." );
        m_botAction.sendChatMessage( event.getPlayerName() + " went into a private arena without asking permission, and was mysteriously disconnected from the server." );
        logEvent( event.getPlayerName() + " entered the arena illegally!" );
    }
    
    public void handleEvent( LoggedOn event ){
        m_botAction.joinArena( "#noseeum" );
        m_opList = m_botAction.getOperatorList();
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Smod Chat" ) );
        logEvent( "Logged in!" );
    }
    
    public void handleCommand( String name, String message ){
        if( message.startsWith( "!invite " )){
            if( message.length() > 0 ){
                String invitee = message.substring(8);
                invitedPlayers.add( invitee.toLowerCase() );
                m_botAction.sendRemotePrivateMessage( invitee, "You have been invited to " + m_botAction.getArenaName() + "!" );
                m_botAction.sendPublicMessage( invitee + " has been invited" );
                logEvent( invitee + " has been invited" );
            }
        } else if( message.startsWith( "!message " )){
            bouncemessage = message.substring( 9 );
            m_botAction.sendPrivateMessage( name, "Message set to: " + bouncemessage );
        } else if( message.startsWith( "!go " )){
            m_botAction.changeArena( message.substring( 4 ));
        }
        
    }
    public void handleEvent( Message event ){
        
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isSmod( name )) handleCommand( name, message );
        }
    }
}
