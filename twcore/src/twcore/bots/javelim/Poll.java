/**
 * @(#)Poll.java
 * Modified version of Harvey Yau's Poll class found in MultiBot
 * for polling spectators on who will win final round
 *
 */
package twcore.bots.javelim;

import twcore.core.BotAction;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

final class Poll {

    private String m_question = "Who do you predict will win?";
    private KimTeam[] m_teams;
    private int m_lastIndex;
    private HashMap<String, Integer> m_votes;
    private BotAction m_botAction;
    private boolean m_isOpen = false;
    private TimerTask m_closePollTask;
    private final static int DELAY_CLOSE_POLL = 60000;

    public Poll(KimTeam[] teams, BotAction botAction) {
    	m_botAction = botAction;
        m_votes = new HashMap<String, Integer>();

		int i = 0;
		for(KimTeam team : teams) {
			if(team != null) {
				i++;
			}
		}
		m_teams = new KimTeam[i];
		i = 0;
		for(KimTeam team : teams) {
			if(team != null) {
				m_teams[i++] = team;
			}
		}

        m_lastIndex = i;
        m_botAction.sendTeamMessage("Poll: " + m_question);
        for(i = 0; i < m_teams.length; i++) {
	            m_botAction.sendTeamMessage((i + 1) + ": " + m_teams[i].toString());
        }
        m_botAction.sendTeamMessage("Private message your answers to me.", 21);
        m_isOpen = true;

        m_botAction.scheduleTask(m_closePollTask = new TimerTask() {
        	public void run() {
	        	m_isOpen = false;
	        	m_botAction.sendTeamMessage("The poll is now closed! Results will be shown when the game ends.");
        	}
        }, DELAY_CLOSE_POLL);
    }

    public void handlePollCount(int id, String name, String message) {
        int vote;
        try {
            vote = Integer.parseInt(message);
        } catch(NumberFormatException e) {
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

    public void endPoll(KimTeam winner) {
    	m_botAction.cancelTask(m_closePollTask);
        m_botAction.sendArenaMessage("Poll results! Question: " + m_question);

        int[] counters = new int[m_lastIndex];
        StringBuilder names = new StringBuilder(64);

        for(Map.Entry<String, Integer> ent : m_votes.entrySet()) {
        	int vote = ent.getValue().intValue();
        	int idx = vote - 1;
        	counters[idx]++;

        	if(m_teams[idx] == winner) {
        		names.append(ent.getKey()).append(", ");
        	}
        }

        for(int i = 0; i < m_teams.length; i++) {
            m_botAction.sendArenaMessage((i + 1) + ". " + m_teams[i].toString() + " : " + counters[i]);
        }

        if(names.length() > 2) {
        	names.setLength(names.length() - 2);
        	m_botAction.sendArenaMessage("Voted for winner: " + names.toString());
        }
    }

    public boolean isOpen() {
    	return m_isOpen;
    }

}