package twcore.bots.elimbot.tasks;

import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.elimbot.elimbot;
import twcore.core.game.Player;

public class PrepareGame extends TimerTask {
	
	private elimbot elimbot;

	public PrepareGame(elimbot elimbot) {
		this.elimbot = elimbot;
	}
	
	public void run() {
		// Lock the arena
		elimbot.lockArena();		
		
		// Set everybody to the right ship
		if(elimbot.ship != 0) {	// after a vote, only one ship is allowed
			Iterator playerIterator = elimbot.m_botAction.getPlayingPlayerIterator();
			
			while( playerIterator.hasNext()) {
				Player p = (Player)playerIterator.next();
				if(p.getShipType() != elimbot.ship) {
					elimbot.m_botAction.setShip(p.getPlayerID(), elimbot.ship);
				}
			}
			
		} else {				// Without a vote, several ships are allowed
			Iterator playerIterator = elimbot.m_botAction.getPlayingPlayerIterator();
			int[] allowedShips = elimbot.getConfiguration().getCurrentConfig().getShips();
			int allowedShip = 1;
			
			for(int i = 0 ; i < allowedShips.length ; i++) {
				if(allowedShips[i] == 1) {
					allowedShip = i;
				}
			}
			
			while( playerIterator.hasNext() ){
				Player p = (Player)playerIterator.next();
				
				if(allowedShips[p.getShipType()] == 0) {
					elimbot.m_botAction.setShip(p.getPlayerID(),allowedShip);
				}
			 }
		}
		
		// Registers the players for !lagout feature
		Iterator playerIt = elimbot.m_botAction.getPlayingPlayerIterator();
		
		while(playerIt.hasNext()) {
			Player p = (Player)playerIt.next();
			elimbot.players.put(Integer.valueOf(p.getPlayerID()), Integer.valueOf(p.getShipType()));
		}

		// Get ready arena message
		elimbot.m_botAction.sendArenaMessage("Get ready!");
		
		elimbot.step();
	}
}