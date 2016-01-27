package twcore.bots.multibot.killrace;

import java.util.HashMap;
import java.util.Iterator;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
    Frequency-based kill race.

    @author affirmative

*/
public class killrace extends MultiModule {

    private boolean isRunning = false;
    private int KillsNeeded = 50;
    private HashMap <Integer, Integer>freqScores = new HashMap<Integer, Integer>();

    public void init() {
    }

    public void requestEvents(ModuleEventRequester events) {
        events.request(this, EventRequester.PLAYER_DEATH);
    }

    public void handleEvent( PlayerDeath event ) {
        if( !isRunning ) return;

        Player killer = m_botAction.getPlayer( event.getKillerID() );

        if (killer == null)
            return;

        int team = killer.getFrequency();

        try {
            int score = freqScores.remove(team);
            score++;
            freqScores.put(team, score);

            if( score >= KillsNeeded )
                goWin(team);
        } catch (Exception e) {}
    }

    public void handleEvent( Message event ) {

        String message = event.getMessage();

        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );

            if( opList.isER( name )) handleCommand( name, message );

            if(message.startsWith ("!score")) {
                m_botAction.sendPrivateMessage( name, "Current scores:" );
                printScores( name );
            }
        }
    }

    public void handleCommand( String name, String message ) {
        if( message.startsWith( "!start" )) {
            m_botAction.sendArenaMessage( "Kill race mode activated!  First frequency with " + KillsNeeded + " combined kills wins.  -" + name );
            isRunning = true;
            freqScores.clear();
            m_botAction.scoreResetAll();
        } else if( message.startsWith( "!stop" )) {
            m_botAction.sendArenaMessage( "Kill race mode deactivated by " + name );
            isRunning = false;
        } else if( message.startsWith( "!target " )) {
            try {
                KillsNeeded = getInteger( message.substring( 8 ));
                m_botAction.sendSmartPrivateMessage( name, "Kills needed to win set to " + KillsNeeded );
            } catch (Exception e) {
                KillsNeeded = 50;
                m_botAction.sendSmartPrivateMessage( name, "Bad input.  Try again." );
            }
        }
    }

    private void goWin(int winningfreq) {
        //Displays the end of game info and sets isRunning back to false
        m_botAction.sendArenaMessage( "FREQ " + winningfreq + " wins!", 5 );
        printScores( null );
        isRunning = false;
    }

    private void printScores( String name ) {
        Iterator<Integer> i = freqScores.keySet().iterator();

        while( i.hasNext() ) {
            Integer team = (Integer)i.next();
            int score = freqScores.get(team);

            if( name == null )
                m_botAction.sendArenaMessage( "Freq " + Tools.formatString(team.toString(), 4) + " ... " + score );
            else
                m_botAction.sendPrivateMessage( name, "Freq " + Tools.formatString(team.toString(), 4) + " ... " + score );
        }
    }

    public String[] getModHelpMessage() {
        String[] help = {
            "[ Kill Race ]   by Kyace (multifreqs by dugwyler)",
            "Rules: First freq to a certain number of kills wins (default: 50).",
            "!target <kills>   - Sets target number of kills to <kills>.",
            "!start            - Starts kill race mode (will not *lock).",
            "!stop             - Stops kill race mode."
        };
        return help;
    }

    public void cancel() {
    }

    public boolean isUnloadable() {
        return true;
    }

    public int getInteger( String input ) {
        try {
            return Integer.parseInt( input.trim() );
        } catch( Exception e ) {
            return 1;
        }
    }
}