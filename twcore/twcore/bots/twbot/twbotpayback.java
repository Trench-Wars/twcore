package twcore.bots.twbot;

import twcore.core.*;
import java.util.*;

/** Author: Ikrit
 *  
 *  Version: 1.0
 */

public class twbotpayback extends TWBotExtension
{
	HashMap payback;
	HashMap beingTracked;
	
	int time = 30;
	
	boolean started = false;
	
	public twbotpayback() {
		payback = new HashMap();
		beingTracked = new HashMap();
	}
	
	public void handleEvent(PlayerDeath event) {
		if(!started) return;
		
		String killee = m_botAction.getPlayerName(event.getKilleeID()).toLowerCase();
		String killer = m_botAction.getPlayerName(event.getKillerID()).toLowerCase();
		
		if(payback.containsKey(killee + killer)) {
			Payback pb = (Payback)payback.get(killee + killer);
			pb.cancel();
			payback.remove(killee + killer);
			m_botAction.sendPrivateMessage(killer, "You have avenged your death and may continue to play.");
		} else {
			Payback pb = new Payback(killee, m_botAction);
			payback.put(killer + killee, pb);
			if(beingTracked.containsKey(killer)) {
				HashSet killed = (HashSet)beingTracked.get(killer);
				killed.add(killee);
				beingTracked.put(killer, killed);
			} else {
				HashSet killed = new HashSet();
				killed.add(killee);
				beingTracked.put(killer, killed);
			}
			m_botAction.scheduleTask(pb, time * 1000);
			m_botAction.sendPrivateMessage(killee, "How can you continue to play after that? Avenge your death or be spec'd!");
		}
	}
	
	public void handleEvent(PlayerLeft event) {
		if(!started) return;
		
		String player = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
		if(beingTracked.containsKey(player)) {
			Iterator it = ((HashSet)beingTracked.get(player)).iterator();
			while(it.hasNext()) {
				String avenger = (String)it.next();
				if(payback.containsKey(player + avenger)) {
					Payback pb = (Payback)payback.get(player + avenger);
					pb.cancel();
					payback.remove(player + avenger);
					m_botAction.sendPrivateMessage(avenger, player + " has run away like a coward, consider your death avenged.");
				}
			}
		}
	}
	
	public void handleEvent(FrequencyShipChange event) {
		if(!started) return;
		
		if(event.getShipType() == 0) {
			String player = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
			if(beingTracked.containsKey(player)) {
				Iterator it = ((HashSet)beingTracked.get(player)).iterator();
				while(it.hasNext()) {
					String avenger = (String)it.next();
					if(payback.containsKey(player + avenger)) {
						Payback pb = (Payback)payback.get(player + avenger);
						pb.cancel();
						payback.remove(player + avenger);
						m_botAction.sendPrivateMessage(avenger, player + " has run away like a coward, consider your death avenged.");
					}
				}
			}
		}
	}
	
	public void handleEvent(Message event) {
		String message = event.getMessage();
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
            String name = m_botAction.getPlayerName(event.getPlayerID());
            if(m_opList.isER(name))
            	handleCommand(name, message);
        }
    }
    
    public void handleCommand(String name, String message) {
    	if(message.toLowerCase().startsWith("!start")) {
    		m_botAction.sendArenaMessage("Payback started, GO!", 104);
    		started = true;
    		cancel();
    	} else if(message.toLowerCase().startsWith("!stop")) {
    		m_botAction.sendArenaMessage("Payback stopped!", 13);
    		cancel();
    	} else if(message.toLowerCase().startsWith("!time ")) {
    		time = 30;
    		try {
    			time = Integer.parseInt(message.split(" ", 2)[1]);
    		} catch(Exception e) {}
    	}
    }
    
    public String[] getHelpMessages() {
    	String helps[] = {
    		"!start        -Starts payback.",
    		"!stop         -Stops payback.",
    		"!time #       -Sets time they have to do their payback thing, default: 30 sec's."
    	};
    	
    	return helps;
    }
    
    public void cancel() {
    	Iterator it = payback.values().iterator();
    	while(it.hasNext()) {
    		Payback pb = (Payback)it.next();
    		if(!pb.isCancelled())
    			pb.cancel();
    	}
    	
    	payback.clear();
    	beingTracked.clear();
    }
}

class Payback extends TimerTask {
	
	String player;
	BotAction m_botAction;
	boolean cancelled = false;
	
	public Payback(String name, BotAction ba) {
		player = name;
		m_botAction = ba;
	}
	
	public void run() {
		m_botAction.spec(player);
		m_botAction.spec(player);
		cancelled = true;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
}