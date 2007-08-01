/**
 * @(#)Poll.java
 * Poll class copied from radiobot
 * for polling spectators on who will win final round
 *
 * @author ?
 * @version 1.00 2007/7/27
 */
package twcore.bots.javelim;

import twcore.core.BotAction;
import java.util.HashMap;

class Poll {

    private String[] m_poll;
    private int m_lastIndex;
    private HashMap<String, Integer> m_votes;
    private BotAction m_botAction;

    public Poll(String[] poll, BotAction botAction) {
    	m_botAction = botAction;
        m_poll = poll;
        m_votes = new HashMap<String, Integer>();
        m_lastIndex = 0;
        m_botAction.sendTeamMessage("Poll: " + poll[0]);
        for(int i = 1; i < poll.length; i++) {
        	if(poll[i] != null) {
        		m_lastIndex++;
	            m_botAction.sendTeamMessage(i + ": " + poll[i]);
        	}
        }
        m_botAction.sendTeamMessage("Private message your answers to " + m_botAction.getBotName());
    }

    public void handlePollCount(int id, String name, String message) {
        int vote;
        try {
            vote = Integer.parseInt(message);
        } catch(NumberFormatException nfe) {
            m_botAction.sendPrivateMessage(id, "Invalid vote. "
            + "Your vote must be a number corresponding to the choices "
            + "in the poll.");
            return;
        }

        if(vote < 1 || vote > m_lastIndex) {
            return;
        }

		if(m_votes.containsKey(name)) {
            m_botAction.sendPrivateMessage(id, "Your vote has been changed.");
		} else {
            m_botAction.sendPrivateMessage(id, "Your vote has been counted.");
		}

        m_votes.put(name, new Integer(vote));
    }

    public void endPoll() {
        m_botAction.sendArenaMessage("The poll has ended! Question: " + m_poll[0]);

        int[] counters = new int[m_lastIndex + 1];
        for(Integer i : m_votes.values()) {
        	counters[i.intValue()]++;
        }
        for(int i = 1; i <= m_lastIndex; i++) {
            m_botAction.sendArenaMessage(i + ". " + m_poll[i] + ": " + counters[i]);
        }
    }

}