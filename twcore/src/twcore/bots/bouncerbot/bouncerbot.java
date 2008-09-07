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
 * A bot designed to handle players who enter into a particular arena
 * without being invited -- a personal bouncer.
 *
 * @author  harvey / modified by dugwyler
 */
public class bouncerbot extends SubspaceBot {
    OperatorList m_opList;                    // List of valid operators
    HashSet<String> m_invitedPlayers;         // Players allowed to enter
    ArrayList<String> m_log;                  // Log of messages
    String m_bounceMessage;                   // Msg sent to players before being bounced
    String m_bounceDestination;               // "Destination" of a bounce for *sendto (:kill: to send a *kill)
    String m_zoneIP;                          // IP of the default zone to use in *sendto
    String m_zonePort;                        // Port of the default zone to use in *sendto
    boolean m_isBouncing = false;             // True if bot should bounce players
    boolean m_sendToChat = false;             // True to send reports to SMod chat
    boolean m_sentInfoOnce = false;           // True if SMod chat has been informed of how to turn off reports
    boolean m_logInfo = true;                 // True if information should be logged to bouncerbot.log
    boolean m_bounceAll = false;              // True if all should be bounced, regardless of operator status (except sysop)


    /**
     * Initializes bouncer.
     * @param botAction
     */
    public bouncerbot( BotAction botAction ){
        super( botAction );
        m_invitedPlayers = new HashSet<String>();
        m_bounceMessage = "Sorry, access to this private arena is currently restricted.  You may not enter.";
        m_log = new ArrayList<String>();
        EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.MESSAGE );
        events.request( EventRequester.PLAYER_ENTERED );
    }

    /**
     * Sends bot to default arena, sets up the chat, and readies the default bounce location: pub of this zone.
     */
    public void handleEvent( LoggedOn event ){
        m_botAction.joinArena( m_botAction.getGeneralSettings().getString( "Arena" ) );
        m_opList = m_botAction.getOperatorList();
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Smod Chat" ) );
        logEvent( "Logged in!" );
        m_zoneIP = m_botAction.getGeneralSettings().getString( "Server" );
        m_zonePort = m_botAction.getGeneralSettings().getString( "Port" );
        m_bounceDestination = m_zoneIP + "," + m_zonePort;
    }

    /**
     * Runs command handling for SMods.
     */
    public void handleEvent( Message event ) {
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( name == null )
                name = event.getMessager();
            if( m_opList.isSmod( name ))
                handleCommand( name, message );
        }
    }

    /**
     * Handles bouncing, and possible exceptions to being bounced.
     */
    public void handleEvent( PlayerEntered event ){
        // Determine if player is allowed in ...
        if( !m_isBouncing )
            return;
        if( m_bounceAll ) {
            if( m_opList.isSysop( event.getPlayerName() ) )
                return;
        } else {
            if( m_opList.isBot( event.getPlayerName() ) )
                return;
        }
        if( m_invitedPlayers.contains( event.getPlayerName().toLowerCase()))
            return;

        // Not?  Bounce.
        if( !m_bounceMessage.equals("none") )
            m_botAction.sendPrivateMessage( event.getPlayerName(), m_bounceMessage );
        if( m_bounceDestination.equals(":kill:") ) {
            m_botAction.sendUnfilteredPrivateMessage( event.getPlayerName(), "*kill" );
            if( m_logInfo )
                m_botAction.sendPublicMessage( event.getPlayerName() + " bounced (DC'd)" );
        } else {
            m_botAction.sendUnfilteredPrivateMessage( event.getPlayerName(), "*sendto " + m_bounceDestination );
            if( m_logInfo )
                m_botAction.sendPublicMessage( event.getPlayerName() + " bounced (relocated)" );
        }

        if( m_sendToChat ) {
            if( m_sentInfoOnce )
                m_botAction.sendChatMessage( event.getPlayerName() + " bounced." );
            else {
                m_botAction.sendChatMessage( event.getPlayerName() + " bounced.  (PM me with !chat to turn off this msg)" );
                m_sentInfoOnce = true;
            }
        }
        logEvent( event.getPlayerName() + " bounced." );
    }


    /**
     * Send event string to log.
     * @param event
     */
    public void logEvent( String event ){
        if( !m_logInfo )
            return;
        Calendar c = Calendar.getInstance();
        String timestamp = c.get( Calendar.MONTH ) + "/" + c.get( Calendar.DAY_OF_MONTH )
        + "/" + c.get( Calendar.YEAR ) + ": " + c.get( Calendar.HOUR ) + ":"
        + c.get( Calendar.MINUTE ) + ":" + c.get( Calendar.SECOND ) + " - ";
        try{
            PrintWriter out = new PrintWriter( new FileWriter( "bouncerbot.log" ));
            m_log.add( timestamp + event );
            for( Iterator<String> i = m_log.iterator(); i.hasNext(); ){
                out.println( i.next() );
            }
            out.close();
        }catch(Exception e){
            Tools.printStackTrace( e );
        }
    }


    /**
     * Handle SMod commands.
     * @param name
     * @param message
     */
    public void handleCommand( String name, String message ){
        if( message.startsWith( "!invite " )){
            if( message.length() > 8 ){
                String invitee = message.substring(8);
                m_invitedPlayers.add( invitee.toLowerCase() );
                m_botAction.sendRemotePrivateMessage( invitee, "You have been invited to " + m_botAction.getArenaName() + "." );
                m_botAction.sendPublicMessage( invitee + " has been invited." );
                logEvent( invitee + " has been invited." );
            }
        } else if( message.startsWith( "!go " )){
            m_botAction.changeArena( message.substring( 4 ));
        } else if( message.startsWith( "!die" )) {
            m_botAction.die( "!die ordered by " + name );
        } else if( message.startsWith( "!message " )){
            m_bounceMessage = message.substring( 9 );
            if( m_bounceMessage.equals("none") )
                m_botAction.sendSmartPrivateMessage( name, "No message will be sent before a player is bounced." );
            else
                m_botAction.sendSmartPrivateMessage( name, "Bounce message set to: " + m_bounceMessage );
        } else if( message.startsWith( "!action move " )){
            if( message.length() > 13 ) {
                String dest = message.substring( 13 );
                if( dest != null && !dest.equals("") ) {
                    m_bounceDestination = m_zoneIP + "," + m_zonePort + "," + dest;
                    m_botAction.sendSmartPrivateMessage( name, "Bounced players will be sent to " + dest + "."  );
                }
            }
        } else if( message.startsWith( "!action move" )){
            m_bounceDestination = m_zoneIP + "," + m_zonePort;
            m_botAction.sendSmartPrivateMessage( name, "Bounced players will be sent to pub." );
        } else if( message.startsWith( "!action zonemove " )){
            if( message.length() > 17 ) {
                String dest = message.substring( 17 );
                if( dest != null && !dest.equals("") ) {
                    m_bounceDestination = dest;
                    m_botAction.sendSmartPrivateMessage( name, "Bounced players will be sent to: " + dest + " (validity of information unverified -- make sure this is correct)"  );
                }
            }
        } else if( message.startsWith( "!action kill" )){
            m_bounceDestination = ":kill:";
            m_botAction.sendSmartPrivateMessage( name, "Bounced players will be *kill'd off the server." );
        } else if( message.startsWith( "!bounce" )){
            m_isBouncing = !m_isBouncing;
            m_botAction.sendSmartPrivateMessage( name, "Bouncer " + (m_isBouncing?"ON":"OFF") );
        } else if( message.startsWith( "!chat" )){
            m_sendToChat = !m_sendToChat;
            m_botAction.sendSmartPrivateMessage( name, "Sending msgs to SMod chat: " + (m_sendToChat?"ON":"OFF") );
        } else if( message.startsWith( "!log" )){
            m_logInfo = !m_logInfo;
            m_botAction.sendSmartPrivateMessage( name, "Sending info to log: " + (m_logInfo?"ON":"OFF") );
        } else if( message.startsWith( "!all" )){
            m_bounceAll = !m_bounceAll;
            m_botAction.sendSmartPrivateMessage( name, "Bouncing all players except sysops: " + (m_bounceAll?"ON":"OFF") );
        } else if( message.startsWith( "!help" )) {
            String[] help = {
                    "Command           Desc                                       Status",
                    "!bounce           Toggles whether to bounce players           "+(m_isBouncing?"ON":"OFF"),
                    "!chat             Toggles sending bounce msgs to SMod chat    "+(m_sendToChat?"ON":"OFF"),
                    "!log              Toggles logging messages to chat            "+(m_logInfo?"ON":"OFF"),
                    "!all              Toggles bouncing everyone except for sysop+ "+(m_bounceAll?"ON":"OFF"),
                    "!action <move|zonemove|kill>     Determines bounce action:",
                    "         move                    Bounces to pub on this zone",
                    "         move <arena>            Bounces to <arena> on this zone",
                    "     zonemove <IP,Port>          Bounces to pub of zone @ <IP,Port>",
                    "     zonemove <IP,Port,Arena>    Bounces to <Arena> of zone @ <IP,Port>",
                    "         kill                    Sends *kill (not recommended)",
                    "     [Default action is: move to pub on this zone]",
                    "!go <arena>       Sends bot to <arena>",
                    "!invite <name>    Adds <name> to the authorized entry list",
                    "!message <msg>    Sets message sent to unauthorized players to <msg>",
                    "!message none     Sets bot to not send a message before bouncing players",
                    "!die              Kills bot"
            };
            m_botAction.smartPrivateMessageSpam(name, help);
        }

    }
}
