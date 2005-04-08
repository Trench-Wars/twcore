package twcore.bots.multibot.bfallout;

import java.util.*;
import twcore.core.*;
import twcore.bots.multibot.*;
import twcore.misc.spaceship.*;

public class bfallout extends MultiModule {

	SpaceShip m_spaceShip;
	CommandInterpreter m_commandInterpreter;

	HashMap players;

	TimerTask fallOutCheck;

	int m_eventStartTime;
	int m_eventState = 0;		// 0 = nothing, 1 = starting, 2 = playing, 3 = stalling
	int m_shipType = 1;

	BFPlayer bestGreener;

	public void init() {
		m_spaceShip = new SpaceShip(m_botAction, moduleSettings, this, "handleShipEvent", 6, 0);
		m_spaceShip.init();
		m_commandInterpreter = new CommandInterpreter(m_botAction);
		registerCommands();

		players = new HashMap();
    }

	public boolean isUnloadable() {
		return m_eventState == 0;
	}

	public String[] getModHelpMessage() {
		String[] message = {
			""
		};
		return message;
	}

	public void requestEvents(EventRequester eventRequester) {

		eventRequester.request(EventRequester.MESSAGE);
		eventRequester.request(EventRequester.ARENA_JOINED);
		eventRequester.request(EventRequester.PLAYER_ENTERED);
		eventRequester.request(EventRequester.PLAYER_LEFT);
		eventRequester.request(EventRequester.LOGGED_ON);
		eventRequester.request(EventRequester.PLAYER_POSITION);
		eventRequester.request(EventRequester.FREQUENCY_SHIP_CHANGE);
		eventRequester.request(EventRequester.PRIZE);
	}

	public void registerCommands() {
        int acceptedMessages = Message.PRIVATE_MESSAGE | Message.PUBLIC_MESSAGE | Message.TEAM_MESSAGE | Message.OPPOSING_TEAM_MESSAGE;

		m_commandInterpreter.registerCommand("!help", acceptedMessages, this, "do_help");
		m_commandInterpreter.registerCommand("!start", acceptedMessages, this, "do_start");
		m_commandInterpreter.registerCommand("!stop", acceptedMessages, this, "do_stop");
		m_commandInterpreter.registerCommand("!spamrules", acceptedMessages, this, "do_spamRules");
        
//		m_commandInterpreter.registerDefaultCommand(acceptedMessages, this, "do_nothing");
	}

	public void do_help(String name, String message) {
		String[] out = {
			"+------------------------------------------------------------+",
			"| BalanceFalloutBot v.1.0                     - author Sika  |",
			"+------------------------------------------------------------+",
			"| BalanceFallout objectives:                                 |",
			"|   Try to stay inside the moving circle (bot graphic). Last |",
			"|   survivor wins!                                           |",
			"+------------------------------------------------------------+",
			"| Commands:                                                  |",
			"|   !help                      - Brings up this message      |",
			"+------------------------------------------------------------+"
		};
		m_botAction.privateMessageSpam(name, out);

		if (opList.isER(name)) {
			String[] out2 = {
				"| Host Commands:                                             |",
				"|   !start                     - Starts the event            |",
				"|   !start <#>                 - Starts the event and forces |",
				"|                                shiptype # for players      |",
				"|   !stop                      - Stops the event             |",
				"|   !spamrules                 - *arena messages the rules   |",
				"+------------------------------------------------------------+"
			};
			m_botAction.privateMessageSpam(name, out2);	
		}
	}

	public void do_start(String name, String message) {

		if (!opList.isER(name))
			return;

		if (m_eventState == 0) {
			m_shipType = 1;
			if (message != null) {
				if (Tools.isAllDigits(message)) {
					int ship;
					try {
						ship = Integer.parseInt(message);
						if (ship <= 8 && ship >= 1) {
							m_shipType = ship;
						}
					} catch (NumberFormatException nfe) { }
				}
			}
			startEvent();
		} else {
			m_botAction.sendPrivateMessage(name, "The event is already in progress!  (!stop it first..?)");
		}
	}

	public void do_stop(String name, String message) {

		if (!opList.isER(name))
			return;
		
		if (m_eventState == 2) {
			stopEvent();
		} else {
			m_botAction.sendPrivateMessage(name, "The event has not been started yet.");
		}
	}

	public void do_spamRules(String name, String message) {

		if (!opList.isER(name))
			return;

		m_botAction.sendArenaMessage("BalanceFallout rules:  Stay inside the moving circle as long as possible, last player in wins!", 1);
		m_botAction.sendArenaMessage("This event also features a green game:  Pick up the most greens to win!");
	}

	public void startEvent() {
		m_eventState = 1;
		m_botAction.toggleLocked();
		m_botAction.sendArenaMessage("Get ready!  Starting in 10 seconds ..", 2);

		TimerTask fiveSeconds = new TimerTask() {
			public void run() {
		
				Iterator i = m_botAction.getPlayingPlayerIterator();
				while (i.hasNext()) {
					Player p = (Player)i.next();

					if (!p.getPlayerName().equals(m_botAction.getBotName())) {
						m_botAction.setShip(p.getPlayerID(), m_shipType);
						m_botAction.setFreq(p.getPlayerID(), 1);
					}
				}
			}
		};

		TimerTask eightSeconds = new TimerTask() {
			public void run() {
				m_botAction.warpAllRandomly();
			}
		};

		TimerTask tenSeconds = new TimerTask() {
			public void run() {
		
				Iterator i = m_botAction.getPlayingPlayerIterator();
				while (i.hasNext()) {
					Player p = (Player)i.next();

					if (!p.getPlayerName().equals(m_botAction.getBotName())) {
						players.put(p.getPlayerName(), new BFPlayer(p.getPlayerName()));
					}
				}

				if (players.size() <= 1) {
					m_botAction.sendArenaMessage("Not enough players.");
					stopEvent();
				} else {
					m_eventState = 2;
					m_eventStartTime = (int)(System.currentTimeMillis());
					m_botAction.sendArenaMessage("GO GO GO!", 104);

					changeTarget();

					fallOutCheck = new TimerTask() {
						public void run() {
							Iterator i = m_botAction.getPlayingPlayerIterator();
							while (i.hasNext()) {
								Player p = (Player)i.next();

								if (players.containsKey(p.getPlayerName()) && !playerInsideRing(p, 2000)) {
									m_botAction.sendPrivateMessage("Sika", p.getPlayerName() + " cheats!!!1");
								}
							}
						}
					};
					m_botAction.scheduleTaskAtFixedRate(fallOutCheck, 5000, 5000);
				}
			}
		};
		m_botAction.scheduleTask(fiveSeconds, 5000);
		m_botAction.scheduleTask(eightSeconds, 8000);
		m_botAction.scheduleTask(tenSeconds, 10000);
	}

	public void stopEvent() {
		m_eventState = 0;
		m_botAction.cancelTasks();
		m_botAction.toggleLocked();
		m_spaceShip.reset();
		players.clear();
		m_botAction.sendArenaMessage("Event has been stopped.");
	}

	public void handleEvent(Message event) {
		m_commandInterpreter.handleEvent(event);

		if (event.getMessageType() == Message.ARENA_MESSAGE) {
			if (event.getMessage().equals("Arena LOCKED")) {
				handleArenaLock(true);
			} else if (event.getMessage().equals("Arena UNLOCKED")) {
				handleArenaLock(false);
			}
		}
	}

	public void handleEvent(ArenaJoined event) {
		m_botAction.setReliableKills(1);
	}

	public void handleEvent(PlayerPosition event) {
		if (m_eventState == 2) {
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if (players.containsKey(name) && !playerInsideRing(m_botAction.getPlayer(event.getPlayerID()), 330)) {
				handleFallOut(name);
			}
		}
	}

	public void handleEvent(PlayerEntered event) {
		String out = "Welcome to BalanceFallout!";
		if (m_eventState != 0) {
			out += " There is a game in progress with "+players.size()+" players in.  [Duration: "+getTimeString()+"]";
		}
		m_botAction.sendPrivateMessage(event.getPlayerID(), out);
	}

	public void handleEvent(PlayerLeft event) {
		String name = m_botAction.getPlayerName(event.getPlayerID());
		handleLagOut(name);
	}

	public void handleEvent(FrequencyShipChange event) {
		String name = m_botAction.getPlayerName(event.getPlayerID());
		if (event.getShipType() == 0) {
			handleLagOut(name);
		}
	}

	public void handleEvent(Prize event) {
		if (m_eventState == 2) {
			String name = m_botAction.getPlayerName(event.getPlayerID());

			if (players.containsKey(name)) {
				BFPlayer p = (BFPlayer)players.get(name);
				p.incGreens();
			}
		}
	}

	public boolean playerInsideRing(Player p, int dist) {
		return m_spaceShip.getDistance(p.getXLocation(), p.getYLocation()) < dist;
	}

	public void handleFallOut(String name) {

		BFPlayer p = (BFPlayer)players.get(name);

		m_botAction.sendArenaMessage(name + " fell out!  Time: "+getTimeString()+"  Greens: "+p.getGreens());

		if (bestGreener == null || bestGreener.getGreens() < p.getGreens()) {
			bestGreener = p;
		}
		players.remove(name);

		m_botAction.spec(name);
		m_botAction.spec(name);

		if (players.size() <= 1) {
			declareWinner();
		}
	}

	public void handleLagOut(String name) {
		if (m_eventState != 0 && players.containsKey(name)) {
			handleFallOut(name);
		}
	}

	public void handleArenaLock(boolean locked) {
		if (locked && m_eventState == 0) {
			m_botAction.toggleLocked();
		} else if (!locked && m_eventState != 0) {
			m_botAction.toggleLocked();
		}
	}

	public void handleShipEvent(Projectile p) {
	}

	public void handleShipEvent(Integer time) {
		changeTarget();
	}

	public void changeTarget() {
		int newX = 1 + (int)(Math.random() * 2000);
		int newY = 1 + (int)(Math.random() * 2000);

		if (Math.random() > 0.5) {
			newX = m_spaceShip.getX() + newX;
			newY = m_spaceShip.getY() + newY;
		} else {
			newX = m_spaceShip.getX() - newX;
			newY = m_spaceShip.getY() - newY;
		}

		if (!m_spaceShip.changeTarget(newX, newY)) {
			changeTarget();		
		}
	}

	public void declareWinner() {
		BFPlayer p;
		String winner = null;
		int g = 0;
		Iterator i = players.keySet().iterator();
		while (i.hasNext()) {
			winner = (String)i.next();
			p = (BFPlayer)players.get(winner);
			g = p.getGreens();

			if (bestGreener == null || bestGreener.getGreens() < p.getGreens()) {
				bestGreener = p;
			}
		}

		if (winner == null) {
			winner = "-No one (?)-";
		}

		m_botAction.sendArenaMessage("GAME OVER: Winner "+winner+"!  Time: "+getTimeString()+"  Greens: "+g, 5);
		m_botAction.sendArenaMessage("---------  Most Greens: "+bestGreener.getName()+" with "+bestGreener.getGreens()+" green(s)");
		stopEvent();
	}

	public String getTimeString() {
		int time = ((int)(System.currentTimeMillis()) - m_eventStartTime) / 1000;
		if (time <= 0) {
			return "0:00";            
		} else {
			int minutes = time / 60;
			int seconds = time % 60;
			return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
		}
	}

	public void cancel() {
		m_spaceShip.setShip(8);
		m_spaceShip.interrupt();
		m_botAction.cancelTasks();
	}

	class BFPlayer {

		int greens = 0;
		String name;

		public BFPlayer(String name) {
			this.name = name;
		}

		public String getName() { return name; }

		public void incGreens() { greens++; }

		public int getGreens() { return greens; }
	};
}
