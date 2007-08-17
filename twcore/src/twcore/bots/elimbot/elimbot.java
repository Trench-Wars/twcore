package twcore.bots.elimbot;

import java.util.HashMap;
import java.util.TimerTask;

import twcore.bots.elimbot.configuration.configuration;
import twcore.bots.elimbot.tasks.PrepareGame;
import twcore.bots.elimbot.tasks.StartGame;
import twcore.bots.elimbot.tasks.deathlimitVote;
import twcore.bots.elimbot.tasks.shipVote;
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
	public ElimState state = ElimState.SPAWNING;
	
	// Temporary/Gameplay variables
	private boolean arenaLock;
	public HashMap<Integer,Integer> votes = new HashMap<Integer,Integer>();
	// This HashMap contains the votes mapped per playername
	// key = playerID
	// value = vote (always numeric)
	public int ship = 0;
	public int deathLimit;
	
	private shipVote shipVote;
	private deathlimitVote deathlimitVote;
	private PrepareGame prepareGame;
	private StartGame startGame;
	
	public HashMap<Integer, Integer> players = new HashMap<Integer, Integer>();
	// This HashMap registers the players by playerid and ship nr for use with !lagout command
	// key = playerid
	// value = ship nr.
	
	/**
	 * @param botAction
	 */
	public elimbot(BotAction botAction) {
		super(botAction);
		
		// Instantiate and initiliaze the configuration
		config = new configuration(m_botAction, m_botAction.getBotSettings());
		
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
				if(isEnoughPlayers()) step();
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
		
		// Handle !commands
		if(event.getMessageType() == Message.PRIVATE_MESSAGE) {
			// TODO: Check if it's staff
			// TODO: Make a !help
			if(event.getMessage().equalsIgnoreCase("!die")) {
				// Kill the timertasks
				this.removeTimerTasks();
				m_botAction.die();
			}
			else if(event.getMessage().equalsIgnoreCase("!stop")) {
				if(this.state != ElimState.STOPPED) {
					m_botAction.sendArenaMessage("Game aborted by "+event.getMessager()+".");
					stop();
				} else {
					m_botAction.sendPrivateMessage(event.getPlayerID(), "The bot is already stopped.");
				}
			}
			else if(event.getMessage().equalsIgnoreCase("!start")) {
				if(this.state == ElimState.STOPPED) {
					m_botAction.sendPrivateMessage(event.getPlayerID(), "Starting the bot...");
					this.state = ElimState.IDLE;
					
					if(isEnoughPlayers()) 
						step();
					else
						m_botAction.sendPrivateMessage(event.getPlayerID(), "Not enough players to start.");
				} else {
					m_botAction.sendPrivateMessage(event.getPlayerID(), "The bot isn't stopped. Stop it first by using !stop.");
				}
			}
			else if(event.getMessage().equalsIgnoreCase("!lagout" )) {
				//if(config.isAllowLagouts() && player = spectator) {
					// TODO: put back in
				//}
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
			if(isEnoughPlayers()) step();
		}
	}

	@Override
	public void handleEvent(PlayerLeft event) {

	}
	
	@Override
	public void handleEvent(FrequencyShipChange event) {
		int ship = event.getShipType();
		int player = event.getPlayerID();
		
		if(state == ElimState.IDLE) {
			if(isEnoughPlayers()) step();
		}
		
		if(	state == ElimState.RUNNING && 			// If Elim is running
			ship == Tools.Ship.SPECTATOR && 		// and a player changed to spectator
			players.containsKey(player) &&			// and the player is still in the game
			config.isAllowLagouts() 				// and lagouts are enabled
			) {
			m_botAction.sendPrivateMessage(player, "Type ::!lagout to get back in.");
		}
	}
	
	/**
	 * Starts a vote or the elim game
	 */
	public void step() {
		//zoneElim(); // Zone that elim game is starting

		if(this.state == ElimState.IDLE && config.getCurrentConfig().isShipsVote()) {
			// Start the vote for the ship
			this.state = ElimState.SHIPVOTE;
			
			this.showRules();
			
			// Check if there are still enough players in
			if(!isEnoughPlayers()) {
				m_botAction.sendArenaMessage("Elim aborted: not enough players");
				stop();
				return;
			}
			
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
			
			// Check if there are still enough players in
			if(!isEnoughPlayers()) {
				m_botAction.sendArenaMessage("Elim aborted: not enough players");
				stop();
				return;
			}
			
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
			
			// Check if there are still enough players in
			if(!isEnoughPlayers()) {
				m_botAction.sendArenaMessage("Elim aborted: not enough players");
				stop();
				return;
			}
			
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
			
			// Check if there are still enough players in
			if(!isEnoughPlayers()) {
				m_botAction.sendArenaMessage("Elim aborted: not enough players");
				stop();
				return;
			}
			
			startGame = new StartGame(this);
			m_botAction.scheduleTask(startGame, 3 * 1000);
		}
		
	}
	
	/**
	 * Checks if enough players are in to start the game
	 */
	private boolean isEnoughPlayers() {
		int playing = m_botAction.getPlayingPlayers().size();
		
		if(playing >= config.getPlayerMin()) {
			return true;
		} else {
			return false;
		}
	}
	
	public configuration getConfiguration() {
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
	
	public void lockArena() {
		this.arenaLock = true;
		m_botAction.toggleLocked();
	}
	
	public void unlockArena() {
		this.arenaLock = true;
		m_botAction.toggleLocked();
	}
	
	/** 
	 * Aborts the current going elim
	 */
	public void stop() {
		this.state = ElimState.STOPPED;
		this.removeTimerTasks();
		votes.clear();
		this.unlockArena();
		this.ship = 0;
		this.deathLimit = 0;
		config.setRulesShown(false);
		players.clear();
	}
	
}