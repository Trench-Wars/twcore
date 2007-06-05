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
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * An elimination bot, similar to elim's RoboRef.
 * 
 * @author MMaverick
 */
public class elimbot extends SubspaceBot {

	private configuration config;
	protected ElimState state = ElimState.SPAWNING;
	
	// Temporary/Gameplay variables
	private boolean arenaLock;
	protected HashMap<Integer,Integer> votes = new HashMap<Integer,Integer>();
	// This HashMap contains the votes mapped per playername
	// key = playerID
	// value = vote (always numeric)
	protected int ship = 0;
	protected int deathLimit;
	
	private shipVote shipVote;
	private deathlimitVote deathlimitVote;
	private PrepareGame prepareGame;
	private StartGame startGame;
	
	protected HashMap<Integer, Integer> players = new HashMap<Integer, Integer>();
	// This HashMap registers the players by playerid and ship nr for use with !lagout command
	// key = playerid
	// value = ship nr.
	
	/**
	 * @param botAction
	 */
	public elimbot(BotAction botAction) {
		super(botAction);
		
		// Instantiate and initiliaze the configuration
		config = new configuration(m_botAction);
		
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
		this.removeTimerTasks();
	}
	
	private void removeTimerTasks() {
        m_botAction.cancelTask(this.shipVote);
        m_botAction.cancelTask(this.deathlimitVote);
        m_botAction.cancelTask(this.prepareGame);
        m_botAction.cancelTask(this.startGame);
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
		
		// Handles the votes
		if((this.state == ElimState.SHIPVOTE || this.state == ElimState.DEATHLIMITVOTE) &&
		   (event.getMessageType() == Message.PUBLIC_MESSAGE || event.getMessageType() == Message.OPPOSING_TEAM_MESSAGE || event.getMessageType() == Message.PRIVATE_MESSAGE) &&
			Tools.isAllDigits(event.getMessage().trim()) &&
			m_botAction.getPlayer(event.getPlayerID()).getShipType() != 0
		    ) {
			
			int voteNr = Integer.parseInt(event.getMessage().trim());
			
			// Check if the vote is valid
			if(this.state == ElimState.SHIPVOTE) {
				int[] shipsConf = config.getCurrentConfig().getShips();
				
				if(voteNr == 0 || voteNr >= shipsConf.length) {
					m_botAction.sendPrivateMessage(event.getPlayerID(), "Invalid vote.");
					return;
				} else if(shipsConf[voteNr] == 0) {
					m_botAction.sendPrivateMessage(event.getPlayerID(), "Invalid vote.");
					return;
				}
			} else if(this.state == ElimState.DEATHLIMITVOTE) {
				int[] deathLimitConf = config.getCurrentConfig().getDeathLimit();
				if(voteNr < deathLimitConf[0] || voteNr > deathLimitConf[1]) {
					m_botAction.sendPrivateMessage(event.getPlayerID(), "Invalid vote.");
					return;
				}
			}
			
			if(votes.containsKey(Integer.valueOf(event.getPlayerID()))== false ) {
				votes.put(Integer.valueOf(event.getPlayerID()), voteNr);
				m_botAction.sendPrivateMessage(event.getPlayerID(), "Your vote has been saved.");
			} else {
				m_botAction.sendPrivateMessage(event.getPlayerID(), "You have already voted.");
			}
		}
		
		if(event.getMessageType() == Message.PRIVATE_MESSAGE) {
			if(event.getMessage().equalsIgnoreCase("!die")) {
				// Kill the timertasks
				this.removeTimerTasks();
				m_botAction.die();
			}
			if(event.getMessage().equalsIgnoreCase("!stop")) {
				if(this.state != ElimState.STOPPED) {
					m_botAction.sendArenaMessage("Game aborted by "+event.getMessager()+".");
					this.state = ElimState.STOPPED;
					this.removeTimerTasks();
					votes.clear();
					this.unlockArena();
					this.ship = 0;
					this.deathLimit = 0;
					config.setRulesShown(false);
					players.clear();
				} else {
					m_botAction.sendPrivateMessage(event.getPlayerID(), "The bot is already stopped.");
				}
			}
			if(event.getMessage().equalsIgnoreCase("!start")) {
				if(this.state == ElimState.STOPPED) {
					m_botAction.sendPrivateMessage(event.getPlayerID(), "Starting the bot...");
					this.state = ElimState.IDLE;
					if(isStartable()) start();
				} else {
					m_botAction.sendPrivateMessage(event.getPlayerID(), "The bot isn't stopped. Stop it first by using !stop.");
				}
			}
			
		}
	}

	@Override
	public void handleEvent(PlayerDeath event) {
	
		if(this.state == ElimState.RUNNING) {
			Player killed = m_botAction.getPlayer(event.getKilleeID());
			
			if(killed != null && killed.getLosses() >= this.deathLimit) {
				String squad = "";
				if(killed.getSquadName().length()>0) {
					squad = "["+killed.getSquadName()+"]";
				}
				m_botAction.sendArenaMessage(killed.getPlayerName()+ " " + squad + " ("+killed.getWins()+"-"+killed.getLosses()+") has been eliminated.");
				m_botAction.specWithoutLock(killed.getPlayerID());
				players.remove(Integer.valueOf(killed.getPlayerID())); 		// Remove from the !lagout players list
			}
		}
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
			
			this.showRules();
			
			// Arena's the vote options
			m_botAction.sendArenaMessage("Vote on the ship: ");
			int[] ships = config.getCurrentConfig().getShips(); 
			String shipvote = "";
			
			for(int i = 0 ; i < ships.length; i++) {
				if(ships[i] == 1) {
					shipvote += i + "-" + Tools.shipName(i) + " ";
				}
			}
			m_botAction.sendArenaMessage(shipvote);
			m_botAction.sendArenaMessage("Type the number of the ship to vote.",2);
			
			// Schedule the end of the vote
			shipVote = new shipVote(this);
			m_botAction.scheduleTask(shipVote, 15 * 1000);
			
		} else if( 
				( this.state == ElimState.IDLE && config.getCurrentConfig().isDeathLimitVote()) ||
				( this.state == ElimState.SHIPVOTE && config.getCurrentConfig().isDeathLimitVote() )
				) {
			// Start the vote for the death limit
			this.state = ElimState.DEATHLIMITVOTE;
			this.votes.clear();
			
			this.showRules();
			
			String deathVoteArena = "Vote on the death limit: ";
			deathVoteArena += config.getCurrentConfig().getDeathLimit()[0];
			deathVoteArena += "-";
			deathVoteArena += config.getCurrentConfig().getDeathLimit()[1];
			m_botAction.sendArenaMessage("_");	// Empty line
			m_botAction.sendArenaMessage(deathVoteArena,2);
			
			// Schedule the end of the vote
			deathlimitVote = new deathlimitVote(this);
			m_botAction.scheduleTask(deathlimitVote, 15 * 1000);
			
		} else if(this.state == ElimState.IDLE || 
				  (this.state == ElimState.SHIPVOTE && config.getCurrentConfig().isDeathLimitVote()==false) ||
				  this.state == ElimState.DEATHLIMITVOTE) {
			this.state = ElimState.STARTING;
			
			String ships = "";
			if(this.ship > 0) {
				// there has been a shipvote and a single ship has been chosen
				ships += Tools.shipName(this.ship);
			} else {
				// no shipvote, get the allowed ships from the configuration
				int[] shipsConf = config.getCurrentConfig().getShips(); 
				
				for(int i = 0 ; i < shipsConf.length; i++) {
					if(shipsConf[i] == 1) {
						ships += Tools.shipName(i) + ", ";
					}
				}
			}
			
			this.showRules();
			
			m_botAction.sendArenaMessage("A "+ships+" elimination to "+this.deathLimit+" is starting in 10 seconds",2);
			
			prepareGame = new PrepareGame(this);
			m_botAction.scheduleTask(prepareGame, 7 * 1000);
			
		} else if(this.state == ElimState.STARTING) {
			// Preperations has been done, start in 3 seconds
			startGame = new StartGame(this);
			m_botAction.scheduleTask(startGame, 3 * 1000);
		}
		
	}
	
	protected configuration getConfiguration() {
		return this.config;
	}
	
	private void showRules() {
		if(config.isRulesShown() == false) {
			String[] rules = 
				{ 
					"-----===[ Welcome to Elimination ]===-----"//, 
					//"You can see your statistics on http://www.trenchwars.org/elim . Good luck and have fun!"
				};
			
			for(String arena:rules) {
				m_botAction.sendArenaMessage(arena);
			}
			config.setRulesShown(true);
		}
	}
	
	protected void lockArena() {
		this.arenaLock = true;
		m_botAction.toggleLocked();
	}
	
	protected void unlockArena() {
		this.arenaLock = true;
		m_botAction.toggleLocked();
	}
}

enum ElimState {
	SPAWNING, IDLE, DEATHLIMITVOTE, SHIPVOTE, STARTING, RUNNING, STOPPED
}

class PrepareGame extends TimerTask {
	
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
		
		elimbot.start();
	}
}

class StartGame extends TimerTask {
	
	private elimbot elimbot;

	public StartGame(elimbot elimbot) {
		this.elimbot = elimbot;
	}
	
	public void run() {
		elimbot.state = ElimState.RUNNING;
		elimbot.m_botAction.scoreResetAll();
		elimbot.m_botAction.shipResetAll();
		elimbot.m_botAction.sendArenaMessage("Go Go Go!",104);
	}
}