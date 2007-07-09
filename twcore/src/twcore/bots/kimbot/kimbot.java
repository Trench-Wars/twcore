package twcore.bots.kimbot;

import java.util.Collections;
import java.util.Collection;
import java.util.TimerTask;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.lang.StringBuilder;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerPosition;
import twcore.core.events.PlayerLeft;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.PlayerEntered;
import twcore.core.OperatorList;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * Bot for hosting Kim's new Jav event (<a href=http://www.twcore.org/ticket/74>ticket #74</a>)
 * @author  flibb
 */
public final class kimbot extends SubspaceBot implements LagoutMan.ExpiredLagoutHandler<KimPlayer> {

    private BotSettings m_botSettings;
    private BotAction m_botAction;

	private State m_state = new State(State.STOPPED);

    private TimerTask m_prizeTask;
    private String m_startedBy = "";
    private int m_deathsToSpec = 10;
    private boolean m_ensureLock = false;
    private List<Map<String, KimPlayer>> m_groups;
    private int[] m_playerCount = { 0, 0, 0, 0 }; //for tracking how many players left in each group
    private KimPlayer[] m_survivors = { null, null, null, null };
    private LagoutMan<KimPlayer> m_lagoutMan = new LagoutMan<KimPlayer>(this);
    private List<String> m_startingLagouts = new LinkedList<String>();
    private List<KimPlayer> m_startingReturns = new LinkedList<KimPlayer>();

    final static int MAX_NAMELENGTH = 18;

    final static String[] help_player = {
    	"-- Help --------",
    	" !lagout  Return to game.",
    	" !spec    Leave game.",
    	" !status  Displays current status."
    };

	final static String[] help_staff = {
		"-- Staff Help --",
		" !start          Starts a game.",
		" !stop           Cancels a game.",
		" !die            Shuts down bot.",
		" !startinfo      Tells who started a game.",
		" !reset          Resets arena.",
		" !remove <name>  Removes player from a game. Must be exact name."
	};

    final static int[] m_safeCoords = {
    	78 | (344 << 16),
		806 | (344 << 16),
		260 | (344 << 16),
		624 | (344 << 16),
		77 | (452 << 16),
		805 | (452 << 16),
		259 | (452 << 16),
		623 | (452 << 16),
		218 | (344 << 16),
		946 | (344 << 16),
		400 | (344 << 16),
		764 | (344 << 16),
		219 | (452 << 16),
		947 | (452 << 16),
		401 | (452 << 16),
		765 | (452 << 16),
		109 | (330 << 16),
		837 | (330 << 16),
		291 | (330 << 16),
		655 | (330 << 16),
		122 | (437 << 16),
		850 | (437 << 16),
		304 | (437 << 16),
		668 | (437 << 16),
		187 | (330 << 16),
		915 | (330 << 16),
		369 | (330 << 16),
		733 | (330 << 16),
		173 | (437 << 16),
		901 | (437 << 16),
		355 | (437 << 16),
		719 | (437 << 16),
		148 | (486 << 16),
		876 | (486 << 16),
		330 | (486 << 16),
		694 | (486 << 16),
		148 | (331 << 16),
		876 | (331 << 16),
		330 | (331 << 16),
		694 | (331 << 16),
		67 | (385 << 16),
		795 | (385 << 16),
		249 | (385 << 16),
		613 | (385 << 16),
		229 | (385 << 16),
		957 | (385 << 16),
		411 | (385 << 16),
		775 | (385 << 16),
		89 | (416 << 16),
		817 | (416 << 16),
		271 | (416 << 16),
		635 | (416 << 16),
		207 | (416 << 16),
		935 | (416 << 16),
		389 | (416 << 16),
		753 | (416 << 16),
		148 | (493 << 16),
		876 | (493 << 16),
		330 | (493 << 16),
		694 | (493 << 16),
		148 | (450 << 16),
		876 | (450 << 16),
		330 | (450 << 16),
		694 | (450 << 16)
    };

    final static int[] m_goCoords = {
    	78 | (348 << 16),
		806 | (348 << 16),
		260 | (348 << 16),
		624 | (348 << 16),
		77 | (448 << 16),
		805 | (448 << 16),
		259 | (448 << 16),
		623 | (448 << 16),
		218 | (348 << 16),
		946 | (348 << 16),
		400 | (348 << 16),
		764 | (348 << 16),
		219 | (448 << 16),
		947 | (448 << 16),
		401 | (448 << 16),
		765 | (448 << 16),
		109 | (334 << 16),
		837 | (334 << 16),
		291 | (334 << 16),
		655 | (334 << 16),
		122 | (433 << 16),
		450 | (433 << 16),
		304 | (433 << 16),
		668 | (433 << 16),
		187 | (334 << 16),
		915 | (334 << 16),
		369 | (334 << 16),
		733 | (334 << 16),
		173 | (433 << 16),
		901 | (433 << 16),
		355 | (433 << 16),
		719 | (433 << 16),
		148 | (482 << 16),
		876 | (482 << 16),
		330 | (482 << 16),
		694 | (482 << 16),
		148 | (335 << 16),
		876 | (335 << 16),
		330 | (335 << 16),
		694 | (335 << 16),
		71 | (385 << 16),
		799 | (385 << 16),
		253 | (385 << 16),
		617 | (385 << 16),
		225 | (385 << 16),
		953 | (385 << 16),
		407 | (385 << 16),
		771 | (385 << 16),
		93 | (416 << 16),
		821 | (416 << 16),
		275 | (416 << 16),
		639 | (416 << 16),
		203 | (416 << 16),
		931 | (416 << 16),
		385 | (416 << 16),
		749 | (416 << 16),
		148 | (497 << 16),
		876 | (497 << 16),
		330 | (497 << 16),
		694 | (497 << 16),
		148 | (446 << 16),
		876 | (446 << 16),
		330 | (446 << 16),
		694 | (446 << 16)
    };

    final static int[] m_finalSafeCoords = {
    	431 | (385 << 16),
    	593 | (385 << 16),
    	876 | (450 << 16),
    	876 | (331 << 16)
    };

    final static int[] m_finalGoCoords = {
    	435 | (385 << 16),
    	589 | (385 << 16),
    	876 | (446 << 16),
    	876 | (335 << 16)
    };


    /** Creates a new instance of kimbot */
    public kimbot(BotAction botAction) {
        super(botAction);
        m_botAction = botAction;
        requestEvents();

        // m_botSettings contains the data specified in file <botname>.cfg
        m_botSettings = m_botAction.getBotSettings();

        m_groups = new ArrayList<Map<String, KimPlayer>>(4);
        m_groups.add(Collections.synchronizedMap(new HashMap<String, KimPlayer>(20, 1.0f)));
        m_groups.add(Collections.synchronizedMap(new HashMap<String, KimPlayer>(20, 1.0f)));
        m_groups.add(Collections.synchronizedMap(new HashMap<String, KimPlayer>(20, 1.0f)));
        m_groups.add(Collections.synchronizedMap(new HashMap<String, KimPlayer>(20, 1.0f)));

    }


    /** Request events that this bot requires to receive.  */
    public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.ARENA_JOINED);
        // req.request(EventRequester.PLAYER_ENTERED);
        req.request(EventRequester.PLAYER_POSITION);
        req.request(EventRequester.PLAYER_LEFT);
        req.request(EventRequester.PLAYER_DEATH);
        // req.request(EventRequester.FREQUENCY_CHANGE);
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(EventRequester.LOGGED_ON);
    }


    /* when the bot logs on, you have to manually send it to an arena */
    public void handleEvent(LoggedOn event) {
    	m_botAction.setMessageLimit(10);
        m_botAction.sendUnfilteredPublicMessage("?chat=" + m_botSettings.getString("chat"));
        m_botAction.joinArena(m_botSettings.getString("arena"));
    }


    /* set ReliableKills 1 (*relkills 1) to make sure your bot receives every packet */
    public void handleEvent(ArenaJoined event) {
        m_botAction.setReliableKills(1);
    }

    public void handleEvent(PlayerEntered event) {
		if(event.getPlayerName().length() > MAX_NAMELENGTH) {
			m_botAction.sendPrivateMessage(event.getPlayerID(), "NOTICE: Name too long. Shorten your name to 18 or less characters to be able to play.");
			if(event.getShipTypeRaw() != 8) { //!spectator
				m_botAction.specWithoutLock(event.getPlayerID());
			}
    	}
    }


    public void handleEvent(Message event) {
    	//System.out.println("(" + event.getPlayerID() + ":" + event.getMessager() + ")(" + Integer.toHexString(event.getMessageType()) + ":" + event.getSoundCode() + "!) " + event.getMessage());

    	if(m_ensureLock && event.getMessageType() == Message.ARENA_MESSAGE && event.getMessage().equals("Arena UNLOCKED")) {
    		m_botAction.toggleLocked();
    		return;
    	}

    	if(event.getMessageType() != Message.PRIVATE_MESSAGE)
    		return;

		int id = event.getPlayerID();
    	String name = m_botAction.getPlayerName(id);
    	String msg = event.getMessage().trim().toLowerCase();
    	if(name == null || msg == null || msg.length() == 0)
    		return;

    	boolean hasAccess = m_botAction.getOperatorList().isSmod(name);

    	if(msg.equals("!help")) {
    		m_botAction.privateMessageSpam(id, help_player);
    		if(hasAccess)
        		m_botAction.privateMessageSpam(id, help_staff);

        } else if(msg.equals("!status")) {
        	cmdStatus(id);

        } else if(msg.equals("!spec")) {
        	if(m_state.isStarting())
				m_botAction.specWithoutLock(id);

        } else if(msg.equals("!about")) {
        	m_botAction.sendPrivateMessage(id, "KimBot! by flibb <ER>", 7);

        } else if(msg.equals("!lagout")) {
			KimPlayer kp = getKimPlayer(name);
			if(kp == null)
				return;

			if(!kp.m_isOut) {
				synchronized(m_state) {
					if((m_state.isMidGame() || m_state.isMidGameFinal()) && m_lagoutMan.remove(kp) == true) {
						m_botAction.setShip(id, Tools.Ship.JAVELIN);
						m_botAction.setFreq(id, kp.m_freq);
					} else if((m_state.isStarting() || m_state.isStartingFinal()) && m_startingLagouts.remove(name)) {
						m_startingReturns.add(kp);
						m_botAction.sendPrivateMessage(id, "You will be placed in the game at the start.");
					}
				}
			} else {
				m_botAction.sendPrivateMessage(id, "Cannot return to game.");
			}

        } else if(hasAccess) {
        	if(msg.equals("!start")) {
        		cmdStart(id);

        	} else if(msg.equals("!stop")) {
        		cmdStop(id);

        	} else if(msg.equals("!die")) {
        		if(m_state.isStopped()) {
	            	m_botAction.die();
        		} else {
        			m_botAction.sendPrivateMessage(id, "A game is in progress. Use !stop first.");
	        	}

        	} else if(msg.equals("!startinfo")) {
        		if(m_state.isStopped() || m_startedBy == null) {
        			m_botAction.sendPrivateMessage(id, "No one started a game.");
        		} else {
        			m_botAction.sendPrivateMessage(id, "Game started by " + m_startedBy);
        		}

        	} else if(msg.equals("!reset")) {
        		if(m_state.isStopped()) {
        			resetArena();
        			m_botAction.sendPrivateMessage(id, "Resetted.");
        		} else {
        			m_botAction.sendPrivateMessage(id, "A game is in progress. Use !stop first.");
        		}
        	} else if(msg.startsWith("!remove ")) {
        		if(m_state.isStopped())
        			m_botAction.sendPrivateMessage(id, "The game is not running.");
        		else
        			cmdRemove(id, msg.substring(8));
        	}
        }
    }


	public void handleEvent(PlayerDeath event) {
		if(m_state.isMidGame() || m_state.isMidGameFinal()) {
			int victimID = event.getKilleeID();
			int killerID = event.getKillerID();
			Player victim = m_botAction.getPlayer(victimID);
			Player killer = m_botAction.getPlayer(killerID);
			int victimGroup, killerGroup;
			if(killer != null) {
				String killerName = m_botAction.getPlayerName(killerID);
				killerGroup = killer.getFrequency() % 4;
				KimPlayer kimKiller = m_groups.get(killerGroup).get(killerName);
				if(kimKiller != null)
					kimKiller.m_kills++;
			}
			if(victim != null) {
				String victimName = m_botAction.getPlayerName(victimID);
				victimGroup = victim.getFrequency() % 4;
				KimPlayer kimVictim = m_groups.get(victimGroup).get(victimName);
				if(kimVictim != null && ++kimVictim.m_deaths >= m_deathsToSpec) {
					removePlayerAndCheck(kimVictim);
					m_botAction.sendArenaMessage(victimName + " is out. "
						+ kimVictim.m_kills + " wins, " + kimVictim.m_deaths + " losses.");
				}
			}
		}
	}


	public void handleEvent(PlayerPosition event) {
		//event.
	}


	//check for lagout
	public void handleEvent(FrequencyShipChange event) {
		if(event.getShipTypeRaw() != 8)
			return;
		lagoutHelper(m_botAction.getPlayerName(event.getPlayerID()));
	}

	//check for lagout
	public void handleEvent(PlayerLeft event) {
		Player player = m_botAction.getPlayer(event.getPlayerID());
		if(player == null || player.getShipType() == Tools.Ship.SPECTATOR) //wasn't in the game
			return;
		lagoutHelper(player.getPlayerName());
	}

	private void lagoutHelper(String name) {
		if(name == null || name.length() == 0) return;
		synchronized(m_state) {
			if(m_state.isMidGame() || m_state.isMidGameFinal()) {
				KimPlayer kimPlayer = getKimPlayer(name);
				if(kimPlayer != null && !kimPlayer.m_isOut) {
					if(m_state.isMidGame() && m_survivors[kimPlayer.m_freq % 4] == kimPlayer) {
						m_startingLagouts.add(kimPlayer.m_name);
					} else {
						registerLagout(kimPlayer);
					}
				}
			} else if(m_state.isStarting() || m_state.isStartingFinal()) {
				m_startingLagouts.add(name);
			}
		}
	}


	//call when a player lags out
	private void registerLagout(KimPlayer kimPlayer) {
		if(kimPlayer.m_lagoutsLeft > 0) {
			m_lagoutMan.add(kimPlayer);
			kimPlayer.m_lagoutsLeft--;
		} else {
			m_botAction.sendArenaMessage(kimPlayer.m_name + " is out (disqualified for too many lagouts). "
				+ kimPlayer.m_kills + " wins, " + kimPlayer.m_deaths + " losses.");
			removePlayerAndCheck(kimPlayer);
		}
	}


	//helper for getting KimPlayer when freq is unknown (time for space)
	private KimPlayer getKimPlayer(int id) {
		return getKimPlayer(m_botAction.getPlayerName(id));
	}
	//helper for getting KimPlayer when freq is unknown
	private KimPlayer getKimPlayer(String name) {
		if(name == null || name.length() == 0) {
			return null;
		}
		KimPlayer result = null;
		for(int i = 0; i < 4; i++) {
			if((result = m_groups.get(i).get(name)) != null)
				break;
		}
		return result;
	}


	public void handleExpiredLagout(KimPlayer kimPlayer) {
		if(m_state.isStopped()) return;
		m_botAction.sendArenaMessage(kimPlayer.m_name + " is out (failed to return from lagout). "
			+ kimPlayer.m_kills + " wins, " + kimPlayer.m_deaths + " losses.");
		removePlayerAndCheck(kimPlayer);
	}

	private void cmdStart(int id) {
		if(!m_state.isStopped()) {
			m_botAction.sendPrivateMessage(id, "A game is in progress. Use !stop first.");
			return;
		}

		m_state.setState(State.PREGAME);
		cleanUp();
		m_startedBy = m_botAction.getPlayerName(id);

		m_botAction.sendArenaMessage("_._.:._.::._.:|:.:|:.    HOW TO PLAY    .:|:.:|:._.::._.:._.", 2);
		m_botAction.sendArenaMessage("(        .    Four sub arenas will each have a     .        )");
		m_botAction.sendArenaMessage("_)  ..  : seperate elimination match. The survivors :  ..  (");
		m_botAction.sendArenaMessage("( ....: . of each sub arena will then battle each-  . :.... )");
		m_botAction.sendArenaMessage("_)..... |     other carrying over their deaths.     | .....(");

		m_botAction.scheduleTask(new TimerTask() {
			public void run() {
				m_state.setState(State.STARTING);
				m_botAction.sendArenaMessage("The game will begin in 30 seconds.", 1);
				m_ensureLock = true;
				m_botAction.toggleLocked();
			}
		}, 5000);

		//prize negative full charge to prevent warping
		m_botAction.scheduleTask(new TimerTask() {
			public void run() {
				m_botAction.scheduleTask(m_prizeTask = new PrizeNegativeFullCharge(), 0, 3000);
			}
		}, 15000);

		m_botAction.scheduleTask(new TimerTask() {
			public void run() {
				List<Player> players = m_botAction.getPlayingPlayers();

				if(players.size() < 8) {
					m_botAction.sendArenaMessage("Not enough players.");
					cmdStop(-1);
					return;
				}

				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X:148");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y:517");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius:20");

				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-X:330");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Y:517");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Radius:20");

				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-X:694");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Y:517");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Radius:20");

				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-X:876");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Y:517");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Radius:20");

				Collections.shuffle(players);
				int freq = 0;
				for(Player player : players) {
					String name = player.getPlayerName();
					int id = player.getPlayerID();

					if(freq >= 64) {
						m_botAction.specWithoutLock(id);
						m_botAction.sendPrivateMessage(id, "Sorry, too many players.");
					} else if(name.length() > 18) {
						m_botAction.sendPrivateMessage(id, "Name too long. Max 18 characters.");
						m_botAction.specWithoutLock(id);
					} else {
						m_botAction.setShip(id, Tools.Ship.JAVELIN);
						m_botAction.setFreq(id, freq);
						int coord = m_safeCoords[freq];
						m_botAction.warpTo(id, coord & 0xffff, coord >> 16);
						m_groups.get(freq % 4).put(name, new KimPlayer(name, freq));
						m_playerCount[freq % 4]++;
						freq++;
					}
				}
			}
		}, 25000);

		//stop prizing negative full charge
		m_botAction.scheduleTask(new TimerTask() {
			public void run() {
				m_botAction.cancelTask(m_prizeTask);
			}
		}, 27000);

		m_botAction.scheduleTask(new TimerTask() {
			public void run() {
				m_state.setState(State.MIDGAME);
				m_botAction.sendArenaMessage("GO! GO! GO!", 104);
				m_botAction.scoreResetAll();
				m_botAction.shipResetAll();
				for(Map<String, KimPlayer> group : m_groups) {
					for(KimPlayer kp : group.values()) {
						if(m_startingLagouts.remove(kp.m_name)) {
							registerLagout(kp);
							m_botAction.sendSmartPrivateMessage(kp.m_name, "You may return to the game with !lagout");
						} else {
							if(m_startingReturns.remove(kp)) {
								m_botAction.setShip(kp.m_name, Tools.Ship.JAVELIN);
								m_botAction.setFreq(kp.m_name, kp.m_freq);
							}
							int coord = m_goCoords[kp.m_freq];
							m_botAction.warpTo(kp.m_name, coord & 0xffff, coord >> 16);
						}
					}
				}
				m_startingLagouts.clear();
				m_startingReturns.clear();
			}
		}, 30000);

	}


	private void cmdStop(int id) {
		if(m_state.isStopped()) {
			m_botAction.sendPrivateMessage(id, "I'm already stopped.");
			return;
		}
		m_state.setState(State.STOPPED);
		cleanUp();
		resetArena();
		String name = id < 0 ? m_botAction.getBotName() : m_botAction.getPlayerName(id);
		m_botAction.sendArenaMessage("This game was brutally killed by " + name, 13);
	}


	private void cmdStatus(int id) {
       	switch(m_state.getState()) {
       		case State.STOPPED:
       			m_botAction.sendPrivateMessage(id, "Stopped.");
       			break;
       		case State.PREGAME:
       			m_botAction.sendPrivateMessage(id, "Preparing for a new game.");
       			break;
       		case State.STARTING:
       			m_botAction.sendPrivateMessage(id, "Starting a game.");
       			break;
    		case State.MIDGAME:
       			m_botAction.sendPrivateMessage(id, "Game in progress. Current lagouts:");
       			for(KimPlayer kp : m_lagoutMan.getLaggers(new KimPlayer[m_lagoutMan.size()])) {
       				if(kp != null)
       					m_botAction.sendPrivateMessage(id, kp.m_name);
       			}
    			break;
    		case State.STARTING_FINAL:
    			m_botAction.sendPrivateMessage(id, "Starting final round.");
    			break;
    		case State.MIDGAME_FINAL:
    			m_botAction.sendPrivateMessage(id, "Final round in progress.");
    			StringBuilder sb = new StringBuilder(120);
    			for(KimPlayer kp : m_survivors) {
    				if(kp != null) {
    					if(sb.length() > 0) sb.append(", ");
    					sb.append(kp.m_name);
    					if(m_lagoutMan.contains(kp)) sb.append("(lagged out)");
    				}
    			}
    			m_botAction.sendPrivateMessage(id, sb.toString());
    			break;
    		case State.ENDING_GAME:
       			m_botAction.sendPrivateMessage(id, "Ending game.");
    			break;
    		default:
       	}
	}


    private void cmdRemove(int id, String nameToRemove) {
    	KimPlayer kp = getKimPlayer(nameToRemove);
    	if(kp != null) {
	    	m_startingLagouts.remove(nameToRemove);
    		m_startingReturns.remove(kp);
    		m_lagoutMan.remove(kp);
    		removePlayerAndCheck(kp);
    		m_botAction.sendPrivateMessage(id, "Player removed.");
    	} else {
	    	m_botAction.sendPrivateMessage(id, "Player not found.");
    	}
    }

	private void cleanUp() {
		m_botAction.cancelTasks();
		for(int i = 0; i < 4; i++) {
			m_groups.get(i).clear();
			m_survivors[i] = null;
			m_playerCount[i] = 0;
		}
		m_lagoutMan.clear();
		m_startingLagouts.clear();
		m_startingReturns.clear();
	}

	private void resetArena() {
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X:490");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y:286");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius:1");

		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-X:534");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Y:286");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Radius:1");

		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-X:490");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Y:286");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Radius:1");

		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-X:534");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Y:286");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Radius:1");

		if(m_ensureLock) {
			m_ensureLock = false;
			m_botAction.toggleLocked();
		}
	}


	private void removePlayerAndCheck(KimPlayer kimPlayer) {
		kimPlayer.m_isOut = true;
		m_botAction.specWithoutLock(kimPlayer.m_name);
		final int groupIndex = kimPlayer.m_freq % 4;
		if(--m_playerCount[groupIndex] == 1) {
			m_botAction.scheduleTask(new TimerTask() {
				//a local copy of groupIndex is automatically provided here
				public void run() {
					KimPlayer survivor = null;
					Map<String, KimPlayer> group = m_groups.get(groupIndex);
					synchronized(group) {
						for(KimPlayer kp : group.values()) {
							if(!kp.m_isOut) {
								survivor = kp;
								//kp.remove
								break;
							}
						}
					}
					if(survivor != null) {
						m_botAction.sendArenaMessage(survivor.m_name + " wins for base " + (groupIndex + 1) + ". "
							+ survivor.m_kills + " wins, " + survivor.m_deaths + " losses.");
					} else {
						m_botAction.sendArenaMessage("No one won for base " + (groupIndex + 1));
					}
					//check if we need to start final round
					if(m_playerCount[0] <= 1 && m_playerCount[1] <= 1
					&& m_playerCount[2] <= 1 && m_playerCount[3] <= 1) {
						startFinalRound();
					}
				}
			}, 4000);
		} else if(m_state.isMidGameFinal()) {
			//check if at most 1 player left
			if(m_playerCount[0] + m_playerCount[1] + m_playerCount[2] + m_playerCount[3] <= 1) {
				m_botAction.scheduleTask(new TimerTask() {
					public void run() {
						endGame();
					}
				}, 4000);
			}
		}
	}


	//only call if 1 or less players in each group
	private void startFinalRound() {
		if(!m_state.isMidGame())
			throw new RuntimeException("startFinalRound() called, but game not running");

		m_state.setState(State.STARTING_FINAL);
		StringBuilder survivorStr = new StringBuilder(120);
		int survivorCount = 0;
		for(KimPlayer kp : m_survivors) {
			if(kp != null) {
				if(survivorStr.length() > 0) survivorStr.append(", ");
				survivorStr.append(kp.m_name).append(" (").append(kp.m_kills).append('-').append(kp.m_deaths).append(')');
				survivorCount++;
			}
		}
		if(survivorCount == 0) {
			m_botAction.sendArenaMessage("No survivors?! o_O", 24);
			endGame();
			return;
		} else if(survivorCount == 1) {
			m_botAction.sendArenaMessage(survivorStr + " wins by default.");
			endGame();
			return;
		}

		m_botAction.sendArenaMessage("Welcome to the Final Round! The survivors are:", 2);
		m_botAction.sendArenaMessage(survivorStr.toString());

		//pm leavers
		for(String name : m_startingLagouts) {
			if(name != null) {
				m_botAction.sendSmartPrivateMessage(name, "Final round is starting. You have 15 seconds to return.");
			}
		}

		//disable warping
		m_botAction.scheduleTask(m_prizeTask = new PrizeNegativeFullCharge(), 0, 3000);

		//warp to final safes and ?set spawns
		m_botAction.scheduleTask(new TimerTask() {
			public void run() {
				for(int i = 0; i < 4; i++) {
					KimPlayer kp = m_survivors[i];
					if(kp != null) {
						if(m_startingReturns.remove(kp)) {
							m_botAction.setShip(kp.m_name, Tools.Ship.JAVELIN);
							m_botAction.setFreq(kp.m_name, kp.m_freq);
						}
						m_botAction.warpTo(m_survivors[i].m_name, m_finalSafeCoords[i] & 0xffff, m_finalSafeCoords[i] >> 16);
					}
				}

				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X:512");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y:517");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius:20");

				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-X:512");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Y:517");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Radius:20");

				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-X:512");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Y:517");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Radius:20");

				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-X:512");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Y:517");
				m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Radius:20");
			}
		}, 15000);

		//stop prizing
		m_botAction.scheduleTask(new TimerTask() {
			public void run() {
				m_botAction.cancelTask(m_prizeTask);
			}
		}, 12000);

		//shipreset and warpto starting coords
		m_botAction.scheduleTask(new TimerTask() {
			public void run() {
				m_state.setState(State.MIDGAME_FINAL);
				m_botAction.sendArenaMessage("GO! GO! GO!", 104);
				m_botAction.shipResetAll();
				for(int i = 0; i < 4; i++) {
					KimPlayer kp = m_survivors[i];
					if(kp != null) {
						if(m_startingLagouts.remove(kp)) {
							registerLagout(kp);
							continue;
						}
						if(m_startingReturns.remove(kp)) {
							m_botAction.setShip(kp.m_name, Tools.Ship.JAVELIN);
							m_botAction.setFreq(kp.m_name, kp.m_freq);
						}
						m_botAction.warpTo(kp.m_name, m_finalGoCoords[i] & 0xffff, m_finalGoCoords[i] >> 16);
					}
				}
			}
		}, 25000);
	}


	private void endGame() {
		/*
		"105 max chars                                                                                           ."
		"--Base1--------------W--L --Base2--------------W--L --Base3--------------W--L --Base4--------------W--L"
		"123456789012345678 123 12 123456789012345678 123 12 123456789012345678 123 12 123456789012345678 123 12"
		"------------------------- ------------------------- ------------------------- -------------------------"
		"123456789012345678 123 12 123456789012345678 123 12 123456789012345678 123 12 123456789012345678 123 12"
		"Winner: winnername! 123 wins, 12 losses."
		*/

		m_state.setState(State.ENDING_GAME);

		KimPlayer winner = null;
		for(int i = 0; i < 4; i++) {
			if(m_survivors[i] != null) {
				m_groups.get(i).remove(m_survivors[i].m_name);
				if(!m_survivors[i].m_isOut)
					winner = m_survivors[i];
			}
		}

		List<KimPlayer> lista = new ArrayList<KimPlayer>(m_groups.get(0).values());
		List<KimPlayer> listb = new ArrayList<KimPlayer>(m_groups.get(1).values());
		List<KimPlayer> listc = new ArrayList<KimPlayer>(m_groups.get(2).values());
		List<KimPlayer> listd = new ArrayList<KimPlayer>(m_groups.get(3).values());

		Comparator<KimPlayer> kpc = new Comparator<KimPlayer>() {
			public int compare(KimPlayer kp1, KimPlayer kp2) {
				return kp1.m_kills - kp2.m_kills;
			}
		};

		Collections.sort(lista, kpc);
		Collections.sort(listb, kpc);
		Collections.sort(listc, kpc);
		Collections.sort(listd, kpc);

		StringBuilder sb = new StringBuilder(105);
		KimPlayer kp;
		int max = Math.max(Math.max(lista.size(), listb.size()), Math.max(listc.size(), listd.size()));
		m_botAction.sendArenaMessage("--Base1--------------W--L --Base2--------------W--L --Base3--------------W--L --Base4--------------W--L");
		for(int i = 0; i < max; i++) {
			if(i < lista.size()) {
				 kp = lista.get(i);
				 padHelper(sb, kp.m_name, 18);
				 padHelper(sb, kp.m_kills, 4);
				 padHelper(sb, kp.m_deaths, 3);
			} else sb.append("                         ");
			if(i < listb.size()) {
				 kp = listb.get(i);
				 padHelper(sb, kp.m_name, 19);
				 padHelper(sb, kp.m_kills, 4);
				 padHelper(sb, kp.m_deaths, 3);
			} else sb.append("                          ");
			if(i < listc.size()) {
				 kp = listc.get(i);
				 padHelper(sb, kp.m_name, 19);
				 padHelper(sb, kp.m_kills, 4);
				 padHelper(sb, kp.m_deaths, 3);
			} else sb.append("                          ");
			if(i < listd.size()) {
				 kp = listc.get(i);
				 padHelper(sb, kp.m_name, 19);
				 padHelper(sb, kp.m_kills, 4);
				 padHelper(sb, kp.m_deaths, 3);
			}
			m_botAction.sendArenaMessage(sb.toString());
			sb.delete(0, sb.length());
		}
		m_botAction.sendArenaMessage("------------------------- ------------------------- ------------------------- -------------------------");

		if((kp = m_survivors[0]) != null) {
			padHelper(sb, kp.m_name, 18);
			padHelper(sb, kp.m_kills, 4);
			padHelper(sb, kp.m_deaths, 3);
		} else sb.append("                         ");
		if((kp = m_survivors[1]) != null) {
			padHelper(sb, kp.m_name, 19);
			padHelper(sb, kp.m_kills, 4);
			padHelper(sb, kp.m_deaths, 3);
		} else sb.append("                          ");
		if((kp = m_survivors[2]) != null) {
			padHelper(sb, kp.m_name, 19);
			padHelper(sb, kp.m_kills, 4);
			padHelper(sb, kp.m_deaths, 3);
		} else sb.append("                          ");
		if((kp = m_survivors[3]) != null) {
			padHelper(sb, kp.m_name, 19);
			padHelper(sb, kp.m_kills, 4);
			padHelper(sb, kp.m_deaths, 3);
		} else sb.append("                          ");

		m_botAction.sendArenaMessage(sb.toString());
		sb.delete(0, sb.length());

		if(winner != null) {
			sb.append("Winner: ").append(winner.m_name).append("! ").append(winner.m_kills).append(" wins, ").append(winner.m_deaths).append(" losses.");
			m_botAction.sendArenaMessage(sb.toString(), 5);
		} else {
			m_botAction.sendArenaMessage("No winner.");
		}

		resetArena();
		cleanUp();
		m_state.setState(State.STOPPED);
	}

	private StringBuilder padHelper(StringBuilder sb, String str, int width) {
		int pad = width - str.length();
		for(int i = 0; i < pad; i++) {
			sb.append(' ');
		}
		return sb.append(str);
	}
	private StringBuilder padHelper(StringBuilder sb, int num, int width) {
		int pad = width - 1;
		for(int i = num / 10; i != 0; i /= 10) { pad--; }
		for(int i = 0; i < pad; i++) {
			sb.append(' ');
		}
		return sb.append(num);
	}


	private class PrizeNegativeFullCharge extends TimerTask {
		public void run() {
			m_botAction.prizeAll(-Tools.Prize.FULLCHARGE);
		}
	}


	private final class State {
	    private int m_state;
	    final static int UNDEFINED = -1;
	    final static int STOPPED = 0;
	    final static int PREGAME = 1;
	    final static int STARTING = 2;
	    final static int MIDGAME = 3;
	    final static int STARTING_FINAL = 4;
	    final static int MIDGAME_FINAL = 5;
	    final static int ENDING_GAME = 6;

		State() {
			m_state = UNDEFINED;
		}

		State(int initialState) {
			m_state = initialState;
		}

		public synchronized boolean isStopped() { return m_state == STOPPED; }
		public synchronized boolean isPreGame() { return m_state == PREGAME; }
		public synchronized boolean isStarting() { return m_state == STARTING; }
		public synchronized boolean isMidGame() { return m_state == MIDGAME; }
		public synchronized boolean isStartingFinal() { return m_state == STARTING_FINAL; }
		public synchronized boolean isMidGameFinal() { return m_state == MIDGAME_FINAL; }
		public synchronized boolean isEndingGame() { return m_state == ENDING_GAME; }

		synchronized void setState(int newState) { m_state = newState; }
		synchronized int getState() { return m_state; }
	}
}


final class KimPlayer {
	String m_name;
	int m_kills = 0;
	int m_deaths = 0;
	int m_freq;
	int m_timeOutside = 0;
	int m_lagoutsLeft = 3;
	boolean m_isOut = false;

	KimPlayer(String name, int freq) {
		m_name = name;
		m_freq = freq;
	}
}
