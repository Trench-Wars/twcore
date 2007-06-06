/*
 * twbotpoll.java
 *
 * Created on January 28, 2004, 7:52 PM
 */

/**
 *
 * @author  Harvey Yau
 */
package twcore.bots.racingbot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import twcore.core.events.Message;
import twcore.core.util.Tools;

public class Rbpoll extends RBExtender {
    private Poll currentPoll = null;

    /** Creates a new instance of twbotpoll */
    public Rbpoll() {
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

        if(m_botAction.getOperatorList().isER( name ) || m_rbBot.twrcOps.contains(name.toLowerCase()))
        {
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
	        }
	     }
    }

    public String[] getHelpMessages() {
        String[] helps = {
            "------ Commands for the poll module -------",
            "!poll <Topic>:<answer1>:<answer2> (and on and on) - Starts a poll.",
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
        }

        public void handlePollCount( String name, String message ){
            try{
                if( !Tools.isAllDigits( message )){
                    return;
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
            Iterator iterator = votes.values().iterator();
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
