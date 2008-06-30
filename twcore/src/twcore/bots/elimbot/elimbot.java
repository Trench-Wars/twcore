package twcore.bots.elimbot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.elimbot.config.configuration;
import twcore.bots.elimbot.tasks.NewGame;
import twcore.bots.elimbot.tasks.PrepareGame;
import twcore.bots.elimbot.tasks.StartGame;
import twcore.bots.elimbot.tasks.deathlimitVote;
import twcore.bots.elimbot.tasks.shipVote;
import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
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

	public long startTime;      // time in ms. when this game started

	// Zone configuration
	private int zoneDelay = 10; // after how many minutes the bot can zone
	private long zoneTime;      // time in ms. when last zoner was done
	private String zoneText = "New elimination game is starting. Type ?go %arena% to play";


	private shipVote shipVote;
	private deathlimitVote deathlimitVote;
	private PrepareGame prepareGame;
	private StartGame startGame;
	private NewGame newGame;

	public HashMap<Integer, String> players = new HashMap<Integer, String>();
	// This HashMap registers the players by playerid and ship nr for use with !lagout command
	// key = playerid
	// value = freq nr. ":" ship nr. ":" timestamp (ms time) ":" lagouts used
	//            0      :     1      :         2             :      3

	/**
	 * @param botAction
	 */
	public elimbot(BotAction botAction) {
		super(botAction);

		// Instantiate and initiliaze the configuration
		config = new configuration(m_botAction, m_botAction.getBotSettings());

		EventRequester events = m_botAction.getEventRequester();
		events.request(EventRequester.LOGGED_ON);
		events.request(EventRequester.ARENA_JOINED);
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
		this.die();
	}

    public boolean isIdle() {
        if( this.state == ElimState.RUNNING )
            return false;
        return true;
    }

	private void removeTimerTasks() {
        m_botAction.cancelTask(this.shipVote);
        m_botAction.cancelTask(this.deathlimitVote);
        m_botAction.cancelTask(this.prepareGame);
        m_botAction.cancelTask(this.startGame);
        m_botAction.cancelTask(this.newGame);
        m_botAction.stopReliablePositionUpdating();
	}

	@Override
	public void handleEvent(LoggedOn event) {
		m_botAction.changeArena(config.getArena());

		// Enter chat
		// 1 = elimbot.cfg configured chat name
		// 2 = "Chat Name" from setup.cfg - botdev chat
		m_botAction.sendUnfilteredPublicMessage("?chat="+config.getChat()+","+ m_botAction.getGeneralSettings().getString( "Chat Name" ));

		// start in 3 seconds
		TimerTask start = new TimerTask() {
			public void run() {
				state = ElimState.IDLE;
				if(isEnoughPlayers()) step();
			}
		};

		m_botAction.scheduleTask(start, 3 * 1000);

		m_botAction.receiveAllPlayerDeaths();
		m_botAction.setPlayerPositionUpdating(300);
	}
	@Override
	public void handleEvent(ArenaJoined event) {
	    this.zoneText = this.zoneText.replaceFirst("%arena%", m_botAction.getArenaName());
	}

	@Override
	public void handleEvent(Message event) {
		if(event.getMessageType() == Message.ARENA_MESSAGE) {
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

		    if(event.getMessage().startsWith("!help")) {
		        String name = m_botAction.getPlayerName(event.getPlayerID());
		        if(name == null) return;

		        String[] help = {       "+---------------------------------------------------------------+",
		                                "|                     E l i m i n a t i o n                     |",
		                                "|---------------------------------------------------------------|",
		                                "| !lagout         - Return to the game if you have lagged out   |" };
		        String[] staffHelp = {  "|----------------------- Staff commands ------------------------|" };
		        String[] modHelp = {    "| !start          - Starts / resumes bot operation              |",
		                                "| !stop           - Aborts current game / halts bot operation   |" };
		        String[] smodHelp = {   "| !die            - Disconnects this bot                        |" };
		        String[] endHelp = {    "+---------------------------------------------------------------+" };

		        m_botAction.privateMessageSpam(event.getPlayerID(), help);

		        if(m_botAction.getOperatorList().isModerator(name)) {
		            m_botAction.privateMessageSpam(event.getPlayerID(), staffHelp);
		            m_botAction.privateMessageSpam(event.getPlayerID(), modHelp);
		        }
		        if(m_botAction.getOperatorList().isSmod(name)) {
		            m_botAction.privateMessageSpam(event.getPlayerID(), smodHelp);
		        }
		        m_botAction.privateMessageSpam(event.getPlayerID(), endHelp);
		    }
			if(event.getMessage().equalsIgnoreCase("!die")) {
				m_botAction.sendChatMessage(2, "ElimBot being removed by "+event.getMessager());
				this.die();
			}
			else if(event.getMessage().equalsIgnoreCase("!stop")) {
			    if(this.state == ElimState.IDLE) {
			        m_botAction.sendPrivateMessage(event.getPlayerID(), "Bot operation halted.");
			        stop();
			    } else if(this.state != ElimState.STOPPED) {
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
						m_botAction.sendPrivateMessage(event.getPlayerID(), "The bot has been started but there are not enough players to start a game.");
				} else {
					m_botAction.sendPrivateMessage(event.getPlayerID(), "The bot isn't stopped. Stop it first by using !stop.");
				}
			}
			else if(event.getMessage().equalsIgnoreCase("!lagout" )) {

			    // Check if Elimination is running
			    if( state != ElimState.RUNNING ) {
			        return;
			    }

			    if( !config.isLagoutsAllowed() ) {
			        m_botAction.sendPrivateMessage(event.getPlayerID(), "No lagouts allowed.");
			        return;
			    }

			    // Other checks
			    if(  ship == Tools.Ship.SPECTATOR &&              // player is a spectator
			         players.containsKey(event.getPlayerID())     // and the player is still in the game
			       ) {

			        String[] playerinfo = players.get(event.getPlayerID()).split(":");
			        int freq = Integer.parseInt(playerinfo[0]);
			        int ship = Integer.parseInt(playerinfo[1]);
			        int time = Integer.parseInt(playerinfo[2]);
			        int lagouts = Integer.parseInt(playerinfo[3]);


			        // Check lagout time
			        if((System.currentTimeMillis() - time) > (config.getLagoutTime() * 60 * 1000)) {
			            // Player has been away longer then the allowed lagout time
			            m_botAction.sendPrivateMessage(event.getPlayerID(), "Your lagout time has expired, you're not allowed to be put back in.");
			            return;
			        }

			        // Check lagout count
			        if(lagouts > config.getLagouts()) {
			            m_botAction.sendPrivateMessage(event.getPlayerID(), "You've used all your available lagouts, you're not allowed to be put back in.");
			            return;
			        }

			        // Put player back in and increment his lagout count
			        m_botAction.setShip(event.getPlayerID(), ship);
			        m_botAction.setFreq(event.getPlayerID(), freq);
			        m_botAction.setShip(event.getPlayerID(), ship);
			        lagouts++;
			        players.put(Integer.valueOf(event.getPlayerID()), freq+":"+ship+":"+time+":"+lagouts);
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
				checkWin();
			}
		}
	}

	@Override
	public void handleEvent(PlayerEntered event) {
		String playername = event.getPlayerName();

		// Welcome message & check if elim can be started if state == IDLE
		if(state == ElimState.DEATHLIMITVOTE) {
			m_botAction.sendPrivateMessage(playername, "Welcome, we are voting on the death limit of this elimination. Enter to play!");
		} else
		if(state == ElimState.IDLE) {
			m_botAction.sendPrivateMessage(playername, "Welcome, we are waiting until enough players enter to start.");
			if(isEnoughPlayers()) step();
		} else
		if(state == ElimState.RUNNING) {
			m_botAction.sendPrivateMessage(playername, "Welcome, we are playing "+config.getCurrentConfig().getFullname()+" to "+this.deathLimit+".");
			m_botAction.sendPrivateMessage(playername, "There are "+this.players.size()+" players remaining.");
		} else
		if(state == ElimState.SHIPVOTE) {
			m_botAction.sendPrivateMessage(playername, "Welcome, we are voting which ships can be played with. Enter to play!");
		} else
		if(state == ElimState.SPAWNING) {
			// Bot will go to state IDLE in 3 seconds, no message necessary
		} else
		if(state == ElimState.STARTING) {
			m_botAction.sendPrivateMessage(playername, "Welcome, we are playing "+config.getCurrentConfig().getFullname()+" to "+this.deathLimit+".");
			m_botAction.sendPrivateMessage(playername, "There are "+this.players.size()+" players remaining.");
		} else
		if(state == ElimState.ENDING) {
		    m_botAction.sendPrivateMessage(playername, "Welcome, we are starting a new game. Enter to play!");
		} else
		if(state == ElimState.STOPPED) {
			// Bot has been !stop'ped, no message
		}

		// If a player enters directly upon entering the arena, check if enough players to start
		if(event.getShipType() != Tools.Ship.SPECTATOR && state == ElimState.IDLE) {
	        if(isEnoughPlayers()) step();
		}

	}

	@Override
	public void handleEvent(PlayerLeft event) {
	    int player = event.getPlayerID();

	    if(    state == ElimState.RUNNING &&           // If Elim is running
	           ship != Tools.Ship.SPECTATOR &&         // and the player is not a spectator (when he changed to spectator, timestamp was already recorded)
	           players.containsKey(player) &&          // and the player is still in the game
	           config.isLagoutsAllowed()               // and lagouts are enabled
	       ) {
	        // Record the timestamp when this player lagged out
	        String playerinfo = players.get(player);
	        String[] pieces = playerinfo.split(":");
	        pieces[2] = String.valueOf(System.currentTimeMillis());
	        players.put(player, pieces[0] +":"+ pieces[1] +":"+ pieces[2] +":"+ pieces[3]);

	        checkWin(); // if 2 players are left and 1 leaves
	    }
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
			config.isLagoutsAllowed()    		    // and lagouts are enabled
			) {

		    // Record the timestamp when this player lagged out
		    String playerinfo = players.get(player);
		    String[] pieces = playerinfo.split(":");
		    pieces[2] = String.valueOf(System.currentTimeMillis());
		    players.put(player, pieces[0] +":"+ pieces[1] +":"+ pieces[2] +":"+ pieces[3]);

		    m_botAction.sendPrivateMessage(player, "Type ::!lagout to get back in.");
		}
	}

	/**
	 * Starts a vote or the elim game
	 */
	public void step() {

	    if(this.state == ElimState.IDLE) {
	        // Zone that elim game is starting
	        zone();
	    }

		if(this.state == ElimState.IDLE && config.getCurrentConfig().isShipsVote()) {
			// Start the vote for the ship
			this.state = ElimState.SHIPVOTE;

			this.showRules();

			// Check if there are still enough players in
			if(!isEnoughPlayers()) {
				m_botAction.sendArenaMessage("Elim aborted: not enough players");
				stop();
				// Set state to IDLE so it restarts automatically
				this.state = ElimState.IDLE;
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
				// Set state to IDLE so it restarts automatically
				this.state = ElimState.IDLE;
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
				// Set state to IDLE so it restarts automatically
				this.state = ElimState.IDLE;
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
			// Preparations has been done, start in 3 seconds

			// Check if there are still enough players in
			if(!isEnoughPlayers()) {
				m_botAction.sendArenaMessage("Elim aborted: not enough players");
				stop();
				// Set state to IDLE so it restarts automatically
				this.state = ElimState.IDLE;
				return;
			}

			startGame = new StartGame(this);
			m_botAction.scheduleTask(startGame, 3 * 1000);
		}

	}

	/**
	 * Checks if there is one frequency left, thus a win situation.
     *
     * Neither of these two win conditions are presently functioning.  Recommend
     * review of other bots to determine their elim end-round conditions.
	 */
	private void checkWin() {
	    //if(this.state == ElimState.RUNNING && countTeams() == 1) {
        if(this.state == ElimState.RUNNING && m_botAction.getNumPlayers() < 2) {
	        m_botAction.sendArenaMessage("GAME OVER",Tools.Sound.HALLELUJAH);

	        Iterator<Player> winners = m_botAction.getPlayingPlayerIterator();

	        while(winners.hasNext()) {
	            Player winner = winners.next();

	            String squad = "";
	            if(winner.getSquadName().length()>0) {
	                squad = "["+winner.getSquadName()+"]";
	            }

	            m_botAction.sendArenaMessage("Winner: "+winner.getPlayerName() + " " + squad + " ("+winner.getWins()+"-"+winner.getLosses()+")");
	        }

            m_botAction.sendArenaMessage("Game length: "+Tools.getTimeDiffString(this.startTime, false));

            // TODO: Save winners to database
            // If successfull:
            //m_botAction.sendArenaMessage("Game saved to database. Visit http://www.trenchwars.org/elim to see statistics.");

            stop();
            this.state = ElimState.ENDING;
            m_botAction.sendPublicMessage("Next elimination game starts in 15 seconds ...");
            newGame = new NewGame(this);
            m_botAction.scheduleTask(newGame, (15 * 1000));
            zone();

	    }
	}

	/**
	 * Counts the number of teams of playing players (non-spectators)
	 * @return
	 */
	private int countTeams() {
	    Iterator<Player> players = m_botAction.getPlayingPlayerIterator();
	    HashSet<Short> teams = new HashSet<Short>();

	    while(players.hasNext()) {
	        teams.add(players.next().getFrequency());
	    }
	    return teams.size();
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
					"-----===[ Welcome to Elimination ]===-----",
					"You can see your statistics on http://www.trenchwars.org/elim . Good luck and have fun!"
				};

			for(String arena:rules) {
				m_botAction.sendArenaMessage(arena);
			}
			config.setRulesShown(true);
		}
	}

	/**
	 * Zones for elimination
	 */
	private void zone() {
	    long difference = System.currentTimeMillis() - this.zoneTime;

	    if((difference / 1000 / 60 ) > this.zoneDelay) {
	        m_botAction.sendZoneMessage(this.zoneText);
	        this.zoneTime = System.currentTimeMillis();
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

	/**
	 * This method is called on !die and in case the bot gets disconnected (handleDisconnect() )
	 */
	private void die() {
		this.state = ElimState.STOPPED;
		this.unlockArena();
		// Remove any running timertasks
		this.removeTimerTasks();
		m_botAction.die();
	}
}