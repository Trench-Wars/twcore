package twcore.bots.twbot;

import twcore.core.events.SoccerGoal
import twcore.core.*;
import twcore.core.events.BallPosition;
import twcore.bots.*;
import java.util.*;

public class twbotikrit extends TWBotExtension {
	
	int status = 0;
	int ship = 1;
	int death = 10;
	
	HashMap<String,Integer> deaths = new HashMap<String,Integer>();
	HashMap<String,Integer> ships = new HashMap<String,Integer>();
	
	public twbotikrit() {
		
	}
	
/*	public void handleEvent(Message event) {
		if(event.getMessageType() == Message.ARENA_MESSAGE) {
			String message = event.getMessage();
			if(message.indexOf("Vote: 1-WB Elim") != -1) {
				status = 1;
			} else if(message.indexOf("Vote: How many") != -1) {
				status = 2;
			} else if(message.indexOf("Rules:") != -1) {
				status = 0;
				calc();
				deaths.clear();
				ships.clear();
				m_botAction.sendPrivateMessage("Robo Ref", "!override "+ship+" "+death);
			}
		} else if(event.getMessageType() == Message.PUBLIC_MESSAGE) {
			if(status == 1) {
				try {
					int val = Integer.parseInt(event.getMessage());
					if(val > 0 && val < 8)
						ships.put(event.getMessager().toLowerCase(),val);
				} catch(Exception e) {}
			} else if(status == 2) {
				try {
					int val = Integer.parseInt(event.getMessage());
					if(val > 0)
						deaths.put(event.getMessager().toLowerCase(),val);
				} catch(Exception e) {}
			}
		}
	}
	
	public void calc() {
		Iterator it = deaths.values().iterator();
		ArrayList<Integer> deathVotes = new ArrayList<Integer>();
		ArrayList<Integer> shipVotes = new ArrayList<Integer>();
		while(it.hasNext()) {
			int ds = (Integer)it.next();
			if(deathVotes.get(ds) == null) {
				deathVotes.add(ds, 1);
			} else {
				deathVotes.add(ds, (((Integer)deathVotes.get(ds)) + 1));
			}
		}
		while(it.hasNext()) {
			int ss = (Integer)it.next();
			if(shipVotes.get(ss) == null) {
				shipVotes.add(ss, 1);
			} else {
				shipVotes.add(ss, (((Integer)shipVotes.get(ss)) + 1));
			}
		}
		int maxDeathVotes = 1;
		int maxDeath = 10;
		for(int k = 0;k < deathVotes.size();k++) {
			if(deathVotes.get(k) != null) {
				if(deathVotes.get(k) > maxDeathVotes) {
					maxDeathVotes = deathVotes.get(k);
					maxDeath = k;
				}
			}
		}
		int maxShipVotes = 1;
		int maxShip = 1;
		for(int k = 0;k < shipVotes.size();k++) {
			if(shipVotes.get(k) != null) {
				if(shipVotes.get(k) > maxShipVotes) {
					maxShipVotes = shipVotes.get(k);
					maxShip = k;
				}
			}
		}
		ship = maxShip;
		death = maxDeath;
	}
	*/
	
	public void handleEvent(BallPosition event ) {
		m_botAction.sendPublicMessage("ID: " + event.getBallID() + "   TimeStamp: " + event.getTimeStamp() + "   PlayerID: + " event.getPlayerID());
	}
	
	public String[] getHelpMessages() {
		String[] str = { "" };
		return str;
	}
	
	public void cancel() {
		
	}
}