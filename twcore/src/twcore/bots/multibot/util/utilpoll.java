package twcore.bots.multibot.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.LinkedList;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.game.Player;

/**
 * Generates a poll.
 *
 * @author  Harvey Yau
 */
public class utilpoll extends MultiUtil {
    private Poll currentPoll = null;
    private LinkedList<Integer> allowedFreqs = new LinkedList<Integer>();

    /** Creates a new instance of twbotpoll */
    public void init() {
    }

    /**
     * Requests events.
     */
    public void requestEvents( ModuleEventRequester modEventReq ) {
    }

    public void cancel() {

    }

    public void handleEvent( Message event ){
        if( event.getMessageType() != Message.PRIVATE_MESSAGE ) return;
        String name = m_botAction.getPlayerName( event.getPlayerID() );
        String message = event.getMessage();
        int id = event.getPlayerID();

        if( currentPoll != null ){
            currentPoll.handlePollCount( name, event.getMessage() );
        }

        if( !m_opList.isER( name )) return;
        if( message.startsWith( "!poll " )){
            if( currentPoll != null ){
                m_botAction.sendPrivateMessage( name, "A poll is currently in "
                + "session.  End this poll before beginning another one." );
                return;
            }
            StringTokenizer izer = new StringTokenizer( message.substring( 6 ),
            ":" );
            int tokens = izer.countTokens();
            if( tokens < 2 ){
                m_botAction.sendPrivateMessage( id, "Sorry but the poll format "
                + "is wrong." );
                return;
            }

            String[] polls = new String[tokens];
            int i = 0;
            while( izer.hasMoreTokens() ){
                polls[i] = izer.nextToken();
                i++;
            }

            currentPoll = new Poll( polls );
        } else if( message.startsWith( "!endpoll" )){
            if( currentPoll == null ){
                m_botAction.sendPrivateMessage( id,
                "There is no poll running right now." );
            } else {
                currentPoll.endPoll();
                currentPoll = null;
            }
        } else if( message.startsWith( "!allowfreq ") ) {
            try {
                int allowed;
                allowed = Integer.parseInt( message.substring(11) );
                allowedFreqs.add( allowed );
                m_botAction.sendPrivateMessage( id, "Freq " + allowed + " is now allowed to vote.  !clearallowed to re-enable voting for all players." );
            } catch (NumberFormatException e) {
                m_botAction.sendPrivateMessage( id, "Unable to parse '" + message.substring(11) + "' as a freq number!" );
            }
        } else if( message.startsWith( "!clearallowed") ) {
            if( allowedFreqs.size() == 0 ) {
                m_botAction.sendPrivateMessage( id, "All freqs are already allowed to vote!" );
            } else {
                allowedFreqs.clear();
                m_botAction.sendPrivateMessage( id, "All freqs are now allowed to vote." );
            }
        }
    }

    public String[] getHelpMessages() {
        String[] helps = {
            "------ Commands for the poll module -------",
            "!poll <Topic>:<answer1>:<answer2> (and on and on)  - Starts a poll.",
            "!allowfreq <freq>   - Adds <freq> to voting allow list.  Default is all.",
            "!clearallowed       - Removes all freqs from allow list (all can vote).",
            "!endpoll - Ends the poll and tallies the results."
        };
        return helps;
    }

    public class Poll{

        private String[] poll;
        private int range;
        private HashMap<String, Integer> votes;

        public Poll( String[] poll ){
            this.poll = poll;
            votes = new HashMap<String, Integer>();
            range = poll.length - 1;
            m_botAction.sendArenaMessage( "Poll: " + poll[0] );
            for( int i = 1; i < poll.length; i++ ){
                m_botAction.sendArenaMessage( i + ": " + poll[i] );
            }
            m_botAction.sendArenaMessage(
            "Private message your answers to " + m_botAction.getBotName() );
            if( allowedFreqs.size() > 0 ) {
                String allowedMsg = "NOTE, only the following frequencies may vote: ";
                for( Integer i : allowedFreqs )
                    allowedMsg += " " + i + " ";
                m_botAction.sendArenaMessage( allowedMsg );
            }
        }

        public void handlePollCount( String name, String message ){
            try{
                if( !Tools.isAllDigits( message )){
                    return;
                }
                if( allowedFreqs.size() > 0 ) {
                    Player p = m_botAction.getPlayer(name);
                    if( p == null )
                        return;
                    if( !allowedFreqs.contains( p.getFrequency() ) ) {
                        m_botAction.sendPrivateMessage( p.getPlayerID(), "Sorry, players on your frequency are not allowed to vote in this poll!" );
                        return;
                    }
                }
                int vote;
                try{
                    vote = Integer.parseInt( message );
                } catch( NumberFormatException nfe ){
                    m_botAction.sendSmartPrivateMessage( name, "Invalid vote.  "
                    + "Your vote must be a number corresponding to the choices "
                    + "in the poll." );
                    return;
                }

                if( !(vote > 0 && vote <= range )) {
                    return;
                }

                votes.put( name, new Integer( vote ));

                m_botAction.sendSmartPrivateMessage( name, "Your vote has been "
                + "counted." );

            } catch( Exception e ){
                m_botAction.sendArenaMessage( e.getMessage() );
                m_botAction.sendArenaMessage( e.getClass().getName() );
            }
        }

        public void endPoll(){
            m_botAction.sendArenaMessage( "The poll has ended! Question: "
            + poll[0] );

            int[] counters = new int[range+1];
            Iterator<Integer> iterator = votes.values().iterator();
            while( iterator.hasNext() ){
                counters[((Integer)iterator.next()).intValue()]++;
            }
            for( int i = 1; i < counters.length; i++ ){
                m_botAction.sendArenaMessage( i + ". " + poll[i] + ": "
                + counters[i] );
            }
        }

    }

}
