package twcore.bots.elimbot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;

/**
 * @author MMaverick
 *
 */
public class elimbot extends SubspaceBot {

	private elimbotConfiguration config;
	protected ElimState state = ElimState.SPAWNING;
	
	// Temporary/Gameplay variables
	private boolean arenaLock;
	protected HashMap<String,Integer> votes = new HashMap<String,Integer>();
	// This HashMap contains the votes mapped per playername
	// key = playername.toLowerCase()
	// value = vote (always numeric)
	protected int ship;
	protected int deathLimit;
	
	/**
	 * @param botAction
	 */
	public elimbot(BotAction botAction) {
		super(botAction);
		
		// Instantiate and initiliaze the configuration
		config = new elimbotConfiguration(m_botAction);
		
		EventRequester events = m_botAction.getEventRequester();
		events.request(EventRequester.LOGGED_ON);
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.PLAYER_DEATH);
		events.request(EventRequester.PLAYER_ENTERED);
		events.request(EventRequester.PLAYER_LEFT);
		events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
	}

	/**
	 * Called when bot (is going to) disconnect
	 */
	@Override
	public void handleDisconnect() {

	}

	@Override
	public void handleEvent(LoggedOn event) {
		m_botAction.changeArena(config.getArena());
		
		// start in 3 seconds
		TimerTask start = new TimerTask() {
			public void run() {
				state = ElimState.IDLE;
				if(isStartable()) start();
			}
		};

		m_botAction.scheduleTask(start, 3 * 1000);
	}

	@Override
	public void handleEvent(Message event) {
		if(event.getMessageType() == Message.ALERT_MESSAGE) {
			// Check if the arena is really locked by the alert message
			if(event.getMessage().equals("Arena LOCKED")) {
				if(this.arenaLock == false) {
					m_botAction.toggleLocked();
				}
			}
			if(event.getMessage().equals("Arena UNLOCKED")) {
				if(this.arenaLock == true) {
					m_botAction.toggleLocked();
				}
			}
		}
	}

	@Override
	public void handleEvent(PlayerDeath event) {
	
	}

	@Override
	public void handleEvent(PlayerEntered event) {
		if(state == ElimState.IDLE) {
			if(isStartable()) start();
		}
	}

	@Override
	public void handleEvent(PlayerLeft event) {

	}
	
	@Override
	public void handleEvent(FrequencyShipChange event) {
		if(state == ElimState.IDLE) {
			if(isStartable()) start();
		}
	}
	
	/**
	 * Checks if enough players are in to start the game
	 */
	private boolean isStartable() {
		int playing = 0;
		Iterator i = m_botAction.getPlayingPlayerIterator();
		while( i.hasNext() ){
			playing++;
			i.next();
		 }
		
		if(playing >= config.getPlayerMin()) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Starts a vote or the elim game
	 */
	protected void start() {
		//zoneElim(); // Zone that elim game is starting

		if(this.state == ElimState.IDLE && config.getCurrentConfig().isShipsVote()) {
			// Start the vote for the ship
			this.state = ElimState.SHIPVOTE;
			
			// Arena's the vote options
			m_botAction.sendArenaMessage("Vote on the ship: ");
			int[] ships = config.getCurrentConfig().getShips(); 
			
			for(int i = 0 ; i < ships.length; i++) {
				if(ships[i] == 1) {
					m_botAction.sendArenaMessage("- " + i + " (" + elimbotConfiguration.shipNames[i] + ") ");
				}
			}
			m_botAction.sendArenaMessage("Type the number of the ship to vote.",2);
			
			// Schedule the end of the vote
			m_botAction.scheduleTask(new shipVote(this), 15 * 1000);
			
		} else if( 
				( this.state == ElimState.IDLE && config.getCurrentConfig().isDeathLimitVote()) ||
				( this.state == ElimState.SHIPVOTE && config.getCurrentConfig().isDeathLimitVote() )
				) {
			// Start the vote for the death limit
			this.state = ElimState.DEATHLIMITVOTE;
			
			String deathVoteArena = "Vote on the death limit: ";
			deathVoteArena += config.getCurrentConfig().getDeathLimit()[0];
			deathVoteArena += "-";
			deathVoteArena += config.getCurrentConfig().getDeathLimit()[1];
			m_botAction.sendArenaMessage("_");	// Empty line
			m_botAction.sendArenaMessage(deathVoteArena,2);
			
			// Schedule the end of the vote
			m_botAction.scheduleTask(new deathlimitVote(this), 15	 * 1000);
			
		} else if(this.state == ElimState.IDLE || 
				  (this.state == ElimState.SHIPVOTE && config.getCurrentConfig().isDeathLimitVote()==false) ||
				  this.state == ElimState.DEATHLIMITVOTE) {
			this.state = ElimState.RUNNING;
			
		} 
		
	}

}

enum ElimState {
	SPAWNING, IDLE, DEATHLIMITVOTE, SHIPVOTE, STARTING, RUNNING, STOPPED
}