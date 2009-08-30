/**
 *
 * Based on alertbot by Dom
 * @author Rob
 *
 */
package twcore.bots.alertbot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**
 * Sends alerts to players when specific games begin.
 */
public class alertbot extends SubspaceBot {


    BotSettings config;
    String arena;
    int alertBotTypeID;
    String botType;
    Pattern endPattern;
    Pattern startPattern;
    String startMessage;
    String endMessage;
    String chat;

	boolean alertToChat = true;
	
	String sqlHost = "website";
	
    private static Pattern failPattern = Pattern.compile("(?!)"); // will never match anything
    private static Pattern acceptable = Pattern.compile("^!(?:on|off|help|die)$",Pattern.CASE_INSENSITIVE);
    private static Pattern replaceMessage = Pattern.compile("(?<!\\\\)(\\\\\\\\)*\\$arena"); // setup in case someone wants \$arena to show up

    public alertbot( BotAction botAction ){

        super( botAction );
        config = m_botAction.getBotSettings();

        // let's look for some magic here
        String botName = m_botAction.getBotName();
        alertBotTypeID = config.getInt( botName + "ID" );
        botType = config.getString( "type" + alertBotTypeID );
        chat = config.getString("AlertChat"+m_botAction.getBotNumber());
        arena = config.getString( botName + "Arena" );
        
        // Is alerting to chat not something like "false"?
        if(chat != null && (chat.equals("false") || chat.equals("f") || chat.equals("no") || chat.equals("n") || chat.equals("0"))) {
            alertToChat = false;
        }

        String startRegex = config.getString( "startregex" + alertBotTypeID );
        if ( startRegex == null ) {
            startPattern = failPattern;
            startMessage = null;
        } else {
            try {
                startPattern = Pattern.compile( startRegex );
                String tempStart = config.getString( "startmessage" + alertBotTypeID );
                if ( tempStart == null ) {
                    startPattern = failPattern;
                    startMessage = null;
                } else {
                    startMessage = replaceMessage.matcher(tempStart).replaceAll( "$1" + config.getString( botName + "Arena" ) );
                }
            } catch ( Exception e ) {
                startPattern = failPattern;
                startMessage = null;
            }
        }

        String endRegex = config.getString( "endregex" + alertBotTypeID );
        if ( endRegex == null ) {
            endPattern = failPattern;
            endMessage = null;
        } else {
            try {
                endPattern = Pattern.compile( endRegex );
                String tempEnd = config.getString( "endmessage" + alertBotTypeID );
                if ( tempEnd == null ) {
                    endPattern = failPattern;
                    endMessage = null;
                } else {
                    endMessage = replaceMessage.matcher(tempEnd).replaceAll( "$1" + config.getString( botName + "Arena" ) );
                }
            } catch ( Exception e ) {
                endPattern = failPattern;
                endMessage = null;
            }
        }
        EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.MESSAGE );
        events.request( EventRequester.LOGGED_ON );
    }


    public void handleEvent( LoggedOn event ){
        
        m_botAction.sendUnfilteredPublicMessage( "?chat="+chat+",alerts" );
        m_botAction.joinArena( arena );
    }

    public void handleEvent( Message event){

        String message = event.getMessage();

        if ( acceptable.matcher(message).matches() && ( event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE ) ) {
            String messager = event.getMessager();
            if ( messager == null) messager = m_botAction.getPlayerName(event.getPlayerID());
            if ( messager == null) return;
            handleCommand( messager , message);
        }

        else if(event.getMessageType() == Message.ARENA_MESSAGE){
            if(startPattern.matcher(message).matches()){
                if(alertToChat) {
	                m_botAction.sendChatMessage(1, startMessage);
	                m_botAction.sendChatMessage(2, startMessage);
                }
                try {
                    ResultSet set = m_botAction.SQLQuery(sqlHost,"SELECT name FROM tblAlerts WHERE id = "+alertBotTypeID+"");
                    if (set == null) return;
                    while ( set.next() ) {
                        m_botAction.sendSmartPrivateMessage( set.getString("name"), startMessage );
                    }
                    m_botAction.SQLClose( set );
                } catch (SQLException e) {
                  return;
                }
            } else if (endPattern.matcher(message).matches()) {
                String returnMessage = endPattern.matcher(message).replaceAll(endMessage);
                m_botAction.sendChatMessage(1, returnMessage);
                m_botAction.sendChatMessage(2, returnMessage);
            }
            else{ return;}
        }
    }

    public void handleCommand(String name, String message){
        if(message.equalsIgnoreCase("!on")){
            try {
                // let's NOT be paranoid
                ResultSet set = m_botAction.SQLQuery(sqlHost,"REPLACE INTO tblAlerts (id,name,date) VALUES("+alertBotTypeID+",\""+Tools.addSlashesToString(name)+"\",NOW())");
                m_botAction.sendSmartPrivateMessage(name,"Alerts activated for " + botType + "." );
                if (set != null) m_botAction.SQLClose( set );
            } catch (SQLException e) {
                Tools.printStackTrace(e);
                m_botAction.sendSmartPrivateMessage(name,"Unable to enable alerts at this time.");
                return;
            }
        }
        else if ( message.equalsIgnoreCase("!off") ) {
            try {
                ResultSet set = m_botAction.SQLQuery(sqlHost,"DELETE FROM tblAlerts WHERE id="+alertBotTypeID+" AND name=\""+Tools.addSlashesToString(name)+"\"");
                m_botAction.sendSmartPrivateMessage(name,"Alerts deactivated for " + botType + "." );
                if (set != null) m_botAction.SQLClose( set );
            } catch (SQLException e) {
                Tools.printStackTrace(e);
                m_botAction.sendSmartPrivateMessage(name,"Unable to disable alerts at this time.");
                return;
            }
        }
        else if ( message.equalsIgnoreCase("!die") && m_botAction.getOperatorList().isHighmod( name ) ) {
            m_botAction.sendSmartPrivateMessage(name, "See you.");
            m_botAction.die();
        }
        else if(message.equalsIgnoreCase("!help")){
            String[] helpmenu = {
                    "Hi, I'm a bot that alerts you when the next "+arena+" starts.",
                    " !on   - turns on alerts in a private message to you",
                    " !off  - turns off alerts",
                    " !help - this message",
                    " ",
                    "Players on the ?chat="+chat+" or ?chat=alerts will also be",
                    "alerted through the chat when the next game begins."
            };
            m_botAction.smartPrivateMessageSpam(name, helpmenu);
        }
    }


}



