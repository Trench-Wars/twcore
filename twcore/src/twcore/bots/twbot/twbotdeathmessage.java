/*
 *Death Message Module
 *
 *Created By: Jacen Solo
 *
 * Created on May 24, 2004, 4:33 PM
 */


package twcore.bots.twbot;

import java.util.List;

import twcore.bots.TWBotExtension;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.util.StringBag;
import twcore.core.util.Tools;

public class twbotdeathmessage extends TWBotExtension
{
    public twbotdeathmessage()
    {
        killmsgs = new StringBag();
        killmsgs.add( "has died." );
    }

    StringBag killmsgs;
    boolean isRunning = false;
    boolean modeSet = false;

    public void deleteKillMessage( String name, int index )
    {

        List list = killmsgs.getList();

        if( !( 1 <= index && index <= list.size() )){
            m_botAction.sendPrivateMessage( name, "Error: Can't find the index" );
            return;
        }

        index--;

        if( list.size() > 1 )
            m_botAction.sendPrivateMessage( name, "Removed: " + (String)list.remove( index ));
        else
        {
            m_botAction.sendPrivateMessage( name, "Sorry, but there must be at least one kill message loaded at all times." );
        }
    }

    public void listKillMessages( String name ){
        m_botAction.sendPrivateMessage( name, "The following messages are in my posession: " );
        List list = killmsgs.getList();
        for( int i = 0; i < list.size(); i++ ){
            if( ((String)list.get( i )).startsWith( "'" )){
                m_botAction.sendPrivateMessage( name, i + 1 + ". " + "<name>" + (String)list.get( i ));
            } else {
                m_botAction.sendPrivateMessage( name, i + 1 + ". " + "<name> " + (String)list.get( i ));
            }
        }
    }

    public void handleEvent(Message event)
    {
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name )) handleCommand( name, message );
        }
    }

    public int getInteger( String input )
    {
        try{
            return Integer.parseInt( input.trim() );
        } catch( Exception e ){
            return 1;
        }
    }

    public void start( String name, String[] params )
    {
            isRunning = true;
            modeSet = true;
    }

    public void handleCommand(String name, String message)
    {
        if( message.startsWith( "!list" )){
            listKillMessages( name );
        } else if( message.startsWith( "!add " )){
            addKillMessage( name, message.substring( 5 ));
        } else if( message.startsWith( "!stop" )){
            m_botAction.sendPrivateMessage( name, "Death Message mode stopped" );
            isRunning = false;
        } else if( message.startsWith( "!start " )){
            String[] parameters = Tools.stringChopper( message.substring( 7 ), ' ' );
            start( name, parameters );
            m_botAction.sendPrivateMessage( name, "Death Message mode started" );
        } else if( message.startsWith( "!start" )){
            isRunning = true;
            modeSet = true;
            m_botAction.sendPrivateMessage( name, "Death Message mode started" );
        } else if( message.startsWith( "!del " )){
            deleteKillMessage( name, getInteger( message.substring( 5 )));
        }
    }


    public void addKillMessage( String name, String killMessage )
    {
        killmsgs.add( killMessage );
        m_botAction.sendPrivateMessage( name, "<name> " + killMessage + " Added" );
    }

    public void handleEvent( PlayerDeath event )
    {
        if( modeSet && isRunning )
        {
            String killmsg = killmsgs.toString();
            int soundPos = killmsg.indexOf('%');
            int soundCode = 0;

            if( soundPos != -1){
                try
                {
                    soundCode = Integer.parseInt(killmsg.substring(soundPos + 1));
                }
                catch( Exception e ){
                    	soundCode = 0;
                    }
                    if(soundCode == 12) {soundCode = 1;} //no naughty sounds
                }

                if( killmsg.startsWith( "'" ) == false){
                   killmsg = " " + killmsg;
                }

                if( soundCode > 0 )
                {
                    killmsg = killmsg.substring(0, soundPos + 1);
                    m_botAction.sendArenaMessage( m_botAction.getPlayerName( event.getKilleeID() ) + killmsg, soundCode );
                }
                else
                {
                    m_botAction.sendArenaMessage( m_botAction.getPlayerName( event.getKilleeID() ) + killmsg );
                }
        }
    }

    public String[] getHelpMessages()
    {
        String[] DeathMessageHelp = {
            "!list               - Lists the currently loaded kill messages",
            "!add <Kill Message> - Adds a kill message. Use %%<num> at the end to add a sound.",
            "!del <index>        - Deletes a kill message.  The number for the index is taken from !list",
            "!stop               - Shuts down death message mode",
            "!start              - Starts standard death message mode"
        };
        return DeathMessageHelp;
    }
    public void cancel()
    {
    }
}