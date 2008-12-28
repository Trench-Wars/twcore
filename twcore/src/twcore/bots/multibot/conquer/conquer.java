package twcore.bots.multibot.conquer;

import static twcore.core.EventRequester.PLAYER_DEATH;
import twcore.bots.MultiModule;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

/**
 * Conquer module: the legend.
 */
public class conquer extends MultiModule {
    
    HashMap <String,Integer>m_originals = new HashMap<String,Integer>();
    TimerTask m_checkForWinnersTask;

    public void init() {
    }

    public void requestEvents(ModuleEventRequester events)
    {
        events.request(this, PLAYER_DEATH);
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
        m_botAction.sendArenaMessage( killeename + " conquered by "
                + killername + "; joins freq " + killer.getFrequency() );
    }

    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( opList.isER( name )) handleCommand( name, message );
        }
    }

    public void handleCommand( String name, String message ){
        if( message.startsWith( "!start" )) {
            m_originals.clear();
            Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
            while( i.hasNext() ) {
                Player p = i.next();
                if( p != null ) {
                    m_originals.put( p.getPlayerName(), new Integer(p.getFrequency()) );
                }
            }
                
            try {
                if( m_checkForWinnersTask != null ) {
                    m_botAction.cancelTask( m_checkForWinnersTask );
                }
            } catch(Exception e) {
                try {
                    m_checkForWinnersTask.cancel();
                } catch( Exception e2 ) {
                    m_botAction.sendPrivateMessage(name, "Unable to check for win condition.  You'll have to monitor it yourself.  Please notify bot coders or make a new task on twcore.org." );
                }
            }
            
            m_checkForWinnersTask = new TimerTask() {
                public void run() {
                    if( !isRunning ) {
                        m_botAction.cancelTask( this );
                        return;
                    }
                    
                    Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
                    boolean multiFreqs = false;
                    int firstFreq = -1;
                    while( i.hasNext() && multiFreqs == false ) {
                        Player p = i.next();
                        if( p != null ) {
                            if( firstFreq == -1 )
                                firstFreq = p.getFrequency();
                            else {
                                if( p.getFrequency() != firstFreq )
                                    multiFreqs = true;
                            }
                        }
                    }
                    
                    // WINRAR!
                    if( multiFreqs == false && firstFreq != -1 ) {
                        String originals = "";
                        
                        for( String origName : m_originals.keySet() ) {
                            int freq = m_originals.get(origName);
                            if( freq == firstFreq )
                                originals += "  " + origName;
                        }
                        m_botAction.sendArenaMessage( Tools.formatString( "Freq " + firstFreq + " has conquered all!  Victory to:" + originals + "!", 200 ), Tools.Sound.HALLELUJAH );
                        isRunning = false;
                    }
                }
            };
            m_botAction.scheduleTask(m_checkForWinnersTask, 5000, 5000 );
            
            m_botAction.sendArenaMessage( "Conquer mode activated by " + name + "." );
            isRunning = true;            
        } else if( message.startsWith( "!stop" )){
            m_botAction.sendArenaMessage( "Conquer mode deactivated by " + name + "." );
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
