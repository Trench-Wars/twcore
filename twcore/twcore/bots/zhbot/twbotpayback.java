package twcore.bots.zhbot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.TWBotExtension;
import twcore.core.BotAction;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;

/**
 * @author Ikrit, MMaverick
 * @version 1.0
 */

public class twbotpayback extends TWBotExtension
{
	HashMap<String, Payback> payback;		// Contains [killee+killer][TimerTask]
	HashMap<String, HashSet> beingTracked;	// Contains Killer's History of kills: [name][HashSet containing names of killed players] 

	final int defaultTime = 60;
	int time = defaultTime;

	boolean started = false;

	public twbotpayback() {
		payback = new HashMap<String, Payback>();
		beingTracked = new HashMap<String, HashSet>();
	}

	public void handleEvent(PlayerDeath event) {
		// Only handle deaths if bot has started
		if(!started) return;
		
		String killee, killeeOriginal;
		String killer, killerOriginal;
		
		// Check if Killee is still in the arena
		if(event.getKilleeID() != 0) {
			killee = m_botAction.getPlayerName(event.getKilleeID()).toLowerCase();
			killeeOriginal = m_botAction.getPlayerName(event.getKilleeID());
		} else {
			return; // killee has left / doesn't exist - ignore the kill
		}
		
		
		// Check if Killer is still in the arena
		if(event.getKillerID() != 0) {
			killer = m_botAction.getPlayerName(event.getKillerID()).toLowerCase();
			killerOriginal = m_botAction.getPlayerName(event.getKillerID());
		} else {
			return; // killer has left / doesn't exist - ignore the kill
		}
		
		
		if(payback.containsKey(killee + killer)) {
			// Killee has killed killer and has avenged his (previous?) death
			// TODO: What happens to the one that got killed (got payback)? That one should also deliver payback.
			Payback pb = (Payback)payback.get(killee + killer);
			pb.cancel(); 						// Remove the TimerTask
			payback.remove(killee + killer);	// Remove from HashSet
			// Notify player that he has completed his payback
			m_botAction.sendPrivateMessage(killer, "You have avenged your death on "+killee+" and may continue to play.");
		} else {
			// Killee has been killed by killer and must now payback his death
			Payback pb;
			
			//TODO: If Killee got killed by the same person again (payback.containsKey(killer+killee)), reset timertask to time
			if(payback.containsKey(killer+killee) == false) {
				pb = new Payback(killeeOriginal, killerOriginal, m_botAction);			// new TimerTask
				payback.put(killer + killee, pb);								// add to hashset
				m_botAction.scheduleTask(pb, time * 1000); 						// Start TimerTask with set time
			} else {
				pb = payback.get(killer+killee);
			}
			
			// Add killee to killer's history of kills
			if(beingTracked.containsKey(killer)) {
				HashSet<String> killed = (HashSet<String>)beingTracked.get(killer);
				killed.add(killee);
				beingTracked.put(killer, killed);
			} else {
				HashSet<String> killed = new HashSet<String>();
				killed.add(killee);
				beingTracked.put(killer, killed);
			}
			
			if(pb != null) {
				m_botAction.sendPrivateMessage(killee, "You need to kill "+m_botAction.getPlayerName(event.getKillerID())+" within "+pb.getSecondsLeft()+" seconds to stay in the game.");
			} else {
				m_botAction.sendPrivateMessage(killee, "You need to kill "+m_botAction.getPlayerName(event.getKillerID())+" within "+time+" seconds to stay in the game.");
			}
		}
	}

	public void handleEvent(PlayerLeft event) {
		if(!started) return;

		// Player has left, tell player who needed to do payback to this player that his task is completed.
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
			
			checkWin();
		}
	}

	public void handleEvent(FrequencyShipChange event) {
		if(!started) return;

		// Player has specced, tell player who needed to do payback to this player that his task is completed.
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
			
			checkWin();
		}
	}

	public void handleEvent(Message event) {
		// Handles commands when operator is ER or higher
		String message = event.getMessage();
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
            String name = m_botAction.getPlayerName(event.getPlayerID());
            if(m_opList.isER(name))
            	handleCommand(name, message);
        }
    }

    public void handleCommand(String name, String message) {
    	// Handles the commands
    	
    	if(message.toLowerCase().startsWith("!start")) {
    		
    		if(started == false) {
				TimerTask startEvent = new TimerTask() {
					public void run() {
						m_botAction.shipResetAll(); // Ship reset all players
						m_botAction.sendArenaMessage("Payback time! Kill or get killed!", 104);
			    		cancel(); // Clears everything
			    		started = true;
					}
				};
				
				TimerTask fiveSeconds = new TimerTask() {
					public void run() {
						m_botAction.sendArenaMessage("Payback starting in 5 seconds...", 1);
					}
				};
	
				m_botAction.sendArenaMessage("Payback starting in 10 seconds...", 1);
	
				m_botAction.scheduleTask(startEvent, 10000);
				m_botAction.scheduleTask(fiveSeconds, 5000);
			} else {
				m_botAction.sendPrivateMessage(name, "Payback is already in progress.  (!stop it first)");
			}
    	} else if(message.toLowerCase().startsWith("!stop")) {
    		if(started) {
    			m_botAction.sendArenaMessage("Payback cancelled by "+name, 23);
    			cancel(); // Clears everything
    		} else {
    			m_botAction.sendPrivateMessage(name, "Payback isn't started.");
    		}
    	} else if(message.toLowerCase().startsWith("!time ")) {
    		try {
    			time = Integer.parseInt(message.split(" ", 2)[1]);
    		} catch(NumberFormatException nfe) {
    			System.err.println("NumberFormatException on !time command (TWBot - Payback module): "+nfe.getMessage());
    			time = defaultTime;
    		}
    		m_botAction.sendPrivateMessage(name, "Time set: "+time+" seconds");
    	} else if(message.toLowerCase().equals("!time")) {
    		m_botAction.sendPrivateMessage(name, "Current time: "+time+" seconds");
    	} else if(message.toLowerCase().equals("!rules")) {
    		
    		// Provides the spacing on the line that displays the time
    		String spacing = "";

    		for(int i = 0 ; i < (12-String.valueOf(time).length()); i++) spacing += " ";
    		
    		// Shows the rules in *arena messages
    		String rules[] = {
    				"+------------------------------------------------------+",
    				"|                   PAYBACK RULES                      |",
    				"|------------------------------------------------------|",
    				"| > When you get killed by a player you get the chance |",
    				"|   to deliver some payback.                           |",
    				"| > You have "+time+" seconds to give this payback."+spacing+"|",
    				"| > If your time runs out, you are put into spectator  |",
    				"|   and you are OUT.                                   |",
    				"| > Last remaining player WINS.                        |",
    				"|                                                      |",
    				"|------------------------------------------------------|",
    				"|           It's payback time! Good luck.              |",
    				"\\------------------------------------------------------/"
    		};
    		
    		for(String line: rules) {
    			m_botAction.sendArenaMessage(line);
    		}
    	} else if(message.toLowerCase().equals("!about")) {
    		String about[] = {
    				"Payback rules:",
    				"- A player can be killed by any other player, the killed player must now 'payback'" +
    				" his death.",
    				"- The player has x seconds to do this (set this amount with !time) (default = 60)",
    				"- If the player runs out of time, he is out and put into spec.",
    				"- Last remaining player wins. Bot handles the winner and" +
    				" deactivates itself.",
    				" ",
    				"Start of game:",
    				"- Make sure the arena is *locked.",
    				"- Get all the people on the right frequency (alone or team) and in a ship of your choice",
    				"- On !start the bot will do a 10 seconds countdown following a *shipreset." +
    				" Then the game will begin.",
    				" ",
    				"End of game:",    				
    				"- The bot will announce the winner or team and deactivate itself once one" +
    				" player or team is left.",
    				"- You can always cancel the current game with !stop.",
    				" ",
    				"Lagouts, misc.:",
    				"- Lagouts shouldn't be put back in",
    				"- The bot can handle new players be put in after !start but since this event is" +
    				" elim-style it isn't really fair."
    		};
    		
    		m_botAction.privateMessageSpam(name, about);
    	}
    }

    // Returns the help menu on !help
    public String[] getHelpMessages() {
    	String helps[] = {
    		"!start        - Starts payback with 10 seconds countdown",
    		"!stop         - Stops payback",
    		"!time <#>     - Sets/Shows time they have to do their payback thing, default: 60 sec's",
    		"!rules        - Displays Payback rules in *arena messages",
    		"!about        - Displays the rules (PM) and other info about this plugin",
    	};

    	return helps;
    }

    // Overridden method from TWBotExtension, clears the game
    public void cancel() {
    	started = false;
    	
    	Iterator it = payback.values().iterator();
    	while(it.hasNext()) {
    		Payback pb = (Payback)it.next();
    		if(pb != null && !pb.isCancelled())
    			pb.cancel();
    	}

    	payback.clear();
    	beingTracked.clear();
    }
    
    // Checks if there is one player left OR only one team and announce winner
    private void checkWin() {
    	
    	if(m_botAction.getNumPlayers()==1) {
    		// Only one player is in play - declare winner
    		Player p = (Player)m_botAction.getPlayingPlayerIterator().next();
    		int kills = (beingTracked.get(p.getPlayerName().toLowerCase())).size();
    		
    		m_botAction.sendArenaMessage("GAME OVER",5);
    		m_botAction.sendArenaMessage("Winner: "+p.getPlayerName()+"  Kills: "+kills);
    		this.cancel();
    		
    	} else if(this.getNumTeams() == 1) {
    		// Only one team is in play - declare winning team
    		Player p = (Player)m_botAction.getPlayingPlayerIterator().next();
    		
    		m_botAction.sendArenaMessage("GAME OVER",5);
    		m_botAction.sendArenaMessage("Winning team: Freq "+p.getFrequency()+" :");
    		
    		for(String arena : getNamesPlayers()) {
    			if(arena != null && arena.length()>0)
    				m_botAction.sendArenaMessage(arena);
    		}
    		
    		this.cancel();
    	}
    }
    
    /**
     * Determines and returns the number of teams still in play (so no spectators counted)
     * @return the number of teams
     */
    private int getNumTeams() {
    	HashSet<String> freqs = new HashSet<String>();
    	Iterator<Player> i = (Iterator<Player>)m_botAction.getPlayerIterator();
    	
    	while(i.hasNext()) {
    		Player p = i.next();
    		
    		if(p.getShipType() != 0) {
    			if(freqs.contains(String.valueOf(p.getFrequency()))== false) {
    				freqs.add(String.valueOf(p.getFrequency()));
    			}
    		}
    	}
    	
    	return freqs.size();
    }
    
    private String[] getNamesPlayers() {
    	Iterator<Player> iterator = (Iterator<Player>)m_botAction.getPlayingPlayerIterator();
    	int playerCount = m_botAction.getNumPlayers();
    	String[] names = new String[playerCount];
    	int i = 0, kills = 0;
    	
    	//244 max length arena msg - 28 playername+kills msg = 216
    	while(iterator.hasNext()) {
    		Player p = iterator.next();
    		
    		if(beingTracked.get(p.getPlayerName().toLowerCase()) == null)
    			kills = 0;
    		else 
    			kills = (beingTracked.get(p.getPlayerName().toLowerCase())).size();
    		
    		if(names[i] != null && names[i].length() > 215) {
    			i++;
    		}
    		if(names[i]==null)
    			names[i] = new String();
    		    		
    		if(names[i].length() == 0) {
    			names[i] = p.getPlayerName() + " ("+kills+" kills)";
    		} else {
    			names[i] = names[i] + ", " + p.getPlayerName() + " ("+kills+" kills)";
    		}
    	}
    	
    	return names;
    }
}

class Payback extends TimerTask {

	String player;
	String tobeKilled;
	BotAction m_botAction;
	boolean cancelled = false;

	public Payback(String player, String tobeKilled, BotAction ba) {
		this.player = player;
		this.tobeKilled = tobeKilled;
		m_botAction = ba;
	}

	public void run() {
		//FIXME: From payback hashset, remove killer + killee
		cancelled = true;
		m_botAction.spec(player);
		m_botAction.spec(player);
		m_botAction.sendArenaMessage(player+" couldn't deliver payback to "+tobeKilled+" in time and is OUT!");
	}

	public boolean isCancelled() {
		return cancelled;
	}
	
	public int getSecondsLeft() {
		return Math.round((this.scheduledExecutionTime() - System.currentTimeMillis())/1000);
	}
}
