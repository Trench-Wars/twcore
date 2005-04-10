/**
 *
 * Based on alertbot by Dom
 * @author Rob
 *
 */
package twcore.bots.alertbot;

import twcore.core.*;
import java.util.*;
import java.util.regex.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class alertbot extends SubspaceBot {


    BotSettings config;
    boolean needsNotPlaying;
    String arena;
    int alertBotTypeID;
    String botType;
    Pattern endPattern;
    Pattern startPattern;
    String startMessage;
    String endMessage;

    private static Pattern failPattern = Pattern.compile("(?!)"); // will never match anything
    private static Pattern acceptable = Pattern.compile("^!(?:on|off|help)$",Pattern.CASE_INSENSITIVE);
    private static Pattern replaceMessage = Pattern.compile("(?<!\\\\)(\\\\\\\\)*\\$arena"); // setup in case someone wants \$arena to show up

    public alertbot( BotAction botAction ){

        super( botAction );
        config = m_botAction.getBotSettings();

        // let's look for some magic here
        String botName = m_botAction.getBotName();
        alertBotTypeID = config.getInt( botName + "ID" );
        botType = config.getString( "type" + alertBotTypeID );
        needsNotPlaying = config.getInt( "matchbot" + alertBotTypeID ) == 1;

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

        String botName = m_botAction.getBotName();
        arena = config.getString( botName + "Arena" );
        m_botAction.sendUnfilteredPublicMessage( "?chat="+ config.getString( botName + "Chat" )+",alerts,uberalerts" );
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
                if ( needsNotPlaying ) {
                    // find our mysterious matchbot
                    Iterator iter = m_botAction.getPlayerIterator();
                    if (iter != null) {
                        while ( iter.hasNext() ) {
                            String player = ((Player)iter.next()).getPlayerName();
                            if (player.toLowerCase().startsWith("matchbot")) m_botAction.sendSmartPrivateMessage(player,"!notplaying");
                        }
                    }
                }
                m_botAction.sendChatMessage(1, startMessage);
                m_botAction.sendChatMessage(2, startMessage);
                m_botAction.sendChatMessage(3, startMessage);
                try {
                    ResultSet set = m_botAction.SQLQuery("local","select name from alerts where id="+alertBotTypeID+" and date > NOW() order by date desc limit 100");
                    if (set == null) return;
                    while ( set.next() ) {
                        m_botAction.sendSmartPrivateMessage( set.getString("name"), startMessage );
                    }
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

        // hack and a half, ty...ty
        else if (event.getMessageType() == Message.PRIVATE_MESSAGE && message.equalsIgnoreCase("notplaying mode turned off, captains will be able to pick you") && m_botAction.getOperatorList().isSysop(m_botAction.getPlayerName(event.getPlayerID())) && m_botAction.getPlayerName(event.getPlayerID()).toLowerCase().startsWith("matchbot")) {
            m_botAction.sendSmartPrivateMessage(m_botAction.getPlayerName(event.getPlayerID()),"!notplaying");
        }
    }

    public void handleCommand(String name, String message){
        if(message.equalsIgnoreCase("!on")){
            try {
                m_botAction.SQLQuery("local","REPLACE INTO alerts (id,name,date) values("+alertBotTypeID+",\""+Tools.addSlashesToString(name)+"\",ADDDATE(NOW(), INTERVAL 6 HOUR))");
                m_botAction.sendSmartPrivateMessage(name,"Alerts activated for " + botType + "." );
            } catch (SQLException e) {
                Tools.printStackTrace(e);
                m_botAction.sendSmartPrivateMessage(name,"Unable to enable alerts at this time.");
                return;
            }
        }
        else if ( message.equalsIgnoreCase("!off") ) {
            try {
                m_botAction.SQLQuery("local","delete from alerts where id="+alertBotTypeID+" and name=\""+Tools.addSlashesToString(name)+"\"");
                m_botAction.sendSmartPrivateMessage(name,"Alerts deactivated for " + botType + "." );
            } catch (SQLException e) {
                Tools.printStackTrace(e);
                m_botAction.sendSmartPrivateMessage(name,"Unable to disable alerts at this time.");
                return;
            }
        }
        else if(message.equalsIgnoreCase("!help")){
             m_botAction.sendSmartPrivateMessage(name, "!on - Activate a private alert for when the next "+ arena +" starts. The alert lasts 6 hours.");
             m_botAction.sendSmartPrivateMessage(name, "!off - Turn the alerts off.");
             m_botAction.sendSmartPrivateMessage(name, "!help - Bring up this message.");
        }
    }


}



