package twcore.bots.multibot.conquer;

import static twcore.core.EventRequester.MESSAGE;
import static twcore.core.EventRequester.PLAYER_DEATH;
import twcore.bots.MultiModule;
import twcore.core.*;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;

/**
 * Conquer module: the legend.
 */
public class conquer extends MultiModule {

    public void init() {
    }

    public void requestEvents(EventRequester events)
    {
        events.request(MESSAGE);
        events.request(PLAYER_DEATH);
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
            if( opList.isER( name )) handleCommand( name, message );
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

    public String[] getModHelpMessage() {
        String[] help = {
                "!start - starts conquer mode",
                "!stop - stops conquer mode"
        };
        return help;
    }

    public boolean isUnloadable() {
        return true;
    }

    public void cancel() {
    }

}
