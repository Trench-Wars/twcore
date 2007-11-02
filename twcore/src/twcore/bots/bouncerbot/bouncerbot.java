package twcore.bots.bouncerbot;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.util.Tools;

/**
 * A bot designed to kick players off the server who enter into a particular arena
 * without being invited -- a personal bouncer.
 *
 * @author  harvey
 */
public class bouncerbot extends SubspaceBot {
    OperatorList m_opList;
    HashSet<String> invitedPlayers;
    ArrayList<String> log;
    String bouncemessage;
    boolean sendToChat = false;
    boolean sentInfoOnce = false;

    public bouncerbot( BotAction botAction ){
        super( botAction );
        invitedPlayers = new HashSet<String>();
        bouncemessage = "Sorry, access to this private arena is currently restricted.  Goodbye!";
        log = new ArrayList<String>();
        EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.MESSAGE );
        events.request( EventRequester.PLAYER_ENTERED );
    }

    public void logEvent( String event ){
        Calendar c = Calendar.getInstance();
        String timestamp = c.get( Calendar.MONTH ) + "/" + c.get( Calendar.DAY_OF_MONTH )
        + "/" + c.get( Calendar.YEAR ) + ": " + c.get( Calendar.HOUR ) + ":"
        + c.get( Calendar.MINUTE ) + ":" + c.get( Calendar.SECOND ) + " - ";
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
        if( m_opList.isModerator( event.getPlayerName() )) return;
        if( invitedPlayers.contains( event.getPlayerName().toLowerCase())) return;
        m_botAction.sendPrivateMessage( event.getPlayerName(), bouncemessage );
        m_botAction.sendUnfilteredPrivateMessage( event.getPlayerName(), "*kill" );
        m_botAction.sendPublicMessage( event.getPlayerName() + " entered w/o permission; DC'd." );
        if( sendToChat ) {
            if( sentInfoOnce )
                m_botAction.sendChatMessage( event.getPlayerName() + " entered protected arena; DC'd." );
            else {
                m_botAction.sendChatMessage( event.getPlayerName() + " entered protected arena; DC'd.  (PM me with !chat to turn off this msg)" );
                sentInfoOnce = true;
            }
        }
        logEvent( event.getPlayerName() + " entered a protected arena and was DC'd." );
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
                m_botAction.sendPublicMessage( invitee + " has been invited." );
                logEvent( invitee + " has been invited." );
            }
        } else if( message.startsWith( "!message " )){
            bouncemessage = message.substring( 9 );
            m_botAction.sendPrivateMessage( name, "Pre-kill message set to: " + bouncemessage );
        } else if( message.startsWith( "!go " )){
            m_botAction.changeArena( message.substring( 4 ));
        } else if( message.startsWith( "!chat" )){
            sendToChat = !sendToChat;
            m_botAction.sendPrivateMessage( name, "Sending msgs to SMod chat: " + (sendToChat?"ON":"OFF") );
        } else if( message.startsWith( "!help" )) {
            String[] help = {
                    "BouncerBot .... 'Removes unwanted visitors.'(tm)",
                    "!go <arena>     - Send bot to <arena>",
                    "!invite <name>  - Adds <name> to the authorized list",
                    "!message <msg>  - Set message sent to unauthorized players to <msg>",
                    "!chat           - Toggle sending kill msgs to SMod chat (default off)",
                    "!die            - Kills me"
            };
            m_botAction.privateMessageSpam(name, help);
        } else if( message.startsWith( "!die" )) {
            m_botAction.die();
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
