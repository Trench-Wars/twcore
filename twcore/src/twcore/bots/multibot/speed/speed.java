package twcore.bots.multibot.speed;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.Tools;

public class speed extends MultiModule {

	final static int MAX_SPEED = 2000;
	final static int GOALS_REQUIRED = 3;

	CommandInterpreter m_commandInterpreter;

	HashMap<String, SpeedPlayer> players;

	int m_eventStartTime;
	int m_eventState = 0;		// 0 = nothing, 1 = starting, 2 = playing, 3 = stalling

	int[] m_speedLimits = { 500, 750, 1000, 1250, 1500 };
	int m_speedLimit = 0;
	int m_speedLimitInterval = 3;

	boolean m_fixedSpeedLimit = false;
	boolean m_bombDisarmable = true;
	boolean m_teamsOfOne = true;
	boolean m_speedLimitReversed = false;
	boolean m_doorsActivated = true;

	public void init() {
		m_commandInterpreter = new CommandInterpreter(m_botAction);
		registerCommands();

		players = new HashMap<String, SpeedPlayer>();
    }

	public boolean isUnloadable() {
		return m_eventState == 0;
	}

	public String[] getModHelpMessage() {
        String opmsg[] = {
                "| Host Commands:                                             |",
                "|   !start                     - Starts the event            |",
                "|   !stop                      - Stops the event             |",
                "|   !settings                  - Displays game settings and  |",
                "|                                how to modify them          |",
                "|   !spamrules                 - *arena messages the rules   |",
                "+------------------------------------------------------------+"
        };
		return opmsg;
	}

	public void requestEvents(ModuleEventRequester eventRequester) {
		eventRequester.request(this, EventRequester.PLAYER_ENTERED);
		eventRequester.request(this, EventRequester.PLAYER_LEFT);
		eventRequester.request(this, EventRequester.PLAYER_DEATH);
		eventRequester.request(this, EventRequester.PLAYER_POSITION);
		eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
	}

	public void registerCommands() {
        int acceptedMessages = Message.PRIVATE_MESSAGE | Message.PUBLIC_MESSAGE | Message.TEAM_MESSAGE | Message.OPPOSING_TEAM_MESSAGE;

		m_commandInterpreter.registerCommand("!help", acceptedMessages, this, "do_help");
		m_commandInterpreter.registerCommand("!start", acceptedMessages, this, "do_start");
		m_commandInterpreter.registerCommand("!stop", acceptedMessages, this, "do_stop");
		m_commandInterpreter.registerCommand("!settings", acceptedMessages, this, "do_settings");
		m_commandInterpreter.registerCommand("!setfixedspeedlimit", acceptedMessages, this, "do_setFixedSpeedLimit");
		m_commandInterpreter.registerCommand("!setinterval", acceptedMessages, this, "do_setInterval");
		m_commandInterpreter.registerCommand("!toggledisarmable", acceptedMessages, this, "do_toggleDisarmable");
		m_commandInterpreter.registerCommand("!toggle1team", acceptedMessages, this, "do_toggleOneTeam");
		m_commandInterpreter.registerCommand("!togglereversed", acceptedMessages, this, "do_toggleReversed");
		m_commandInterpreter.registerCommand("!toggledoors", acceptedMessages, this, "do_toggleDoors");
		m_commandInterpreter.registerCommand("!spamrules", acceptedMessages, this, "do_spamRules");

//		m_commandInterpreter.registerDefaultCommand(acceptedMessages, this, "do_nothing");
	}

	public void do_help(String name, String message) {
		String[] out = {
			"+------------------------------------------------------------+",
			"| SpeedBot v.0.9                              - author Sika  |",
			"+------------------------------------------------------------+",
			"| Speed objectives:                                          |",
			"|   There is a bomb planted in your spaceship. It explodes   |",
			"|   if you fly under the required speedlimit! Last player in |",
			"|   wins!                                                    |",
			"+------------------------------------------------------------+",
			"| Commands:                                                  |",
			"|   !help                      - Brings up this message      |",
			"+------------------------------------------------------------+"
		};
		m_botAction.privateMessageSpam(name, out);
	}

	public void do_start(String name, String message) {

		if (!opList.isER(name))
			return;

		if (m_eventState == 0) {
			m_eventState = 1;

			TimerTask tenSeconds = new TimerTask() {
				public void run() {
					startGame();
				}
			};

			m_botAction.sendArenaMessage("Event starting in 10 seconds!!", 2);
			spamSettings();

			m_botAction.scheduleTask(tenSeconds, 10000);
		} else {
			m_botAction.sendPrivateMessage(name, "The event is already in progress!  (!stop it first)");
		}
	}

	public void do_stop(String name, String message) {

		if (!opList.isER(name))
			return;

		if (m_eventState == 2) {
			endGame(true);
		} else {
			m_botAction.sendPrivateMessage(name, "The event has not been started yet.");
		}
	}

	public void do_settings(String name, String message) {

		if (!opList.isER(name))
			return;

		if (m_fixedSpeedLimit) {
			m_botAction.sendPrivateMessage(name, "Fixed SpeedLimit:           "+m_speedLimits[m_speedLimit]+"  (!setfixedspeedlimit off = off, 1 = 500, 2 = 750, 3 = 1000, 4 = 1250, 5 = 1500)");
		} else {
			m_botAction.sendPrivateMessage(name, "Fixed SpeedLimit:           OFF  (!setfixedspeedlimit off = off, 1 = 500, 2 = 750, 3 = 1000, 4 = 1250, 5 = 1500)");
			m_botAction.sendPrivateMessage(name, "SpeedLimit change interval: "+m_speedLimitInterval+" mins  (!setinterval #)");
		}
		m_botAction.sendPrivateMessage(name, "Bomb disarmable:            "+Boolean.toString(m_bombDisarmable).toUpperCase()+"  (!toggledisarmable)");
		m_botAction.sendPrivateMessage(name, "Force teams of 1:           "+Boolean.toString(m_teamsOfOne).toUpperCase()+"  (!toggleteam)");
		m_botAction.sendPrivateMessage(name, "Reversed SpeedLimit:        "+Boolean.toString(m_speedLimitReversed).toUpperCase()+"  (!togglereversed)");
		m_botAction.sendPrivateMessage(name, "Doors activated:            "+Boolean.toString(m_doorsActivated).toUpperCase()+"  (!toggledoors)");
		m_botAction.sendPrivateMessage(name, "NOTE: You can't alter settings when there is a game in progress!  You can also mix 'normal' events (hunt, dm, etc.) with this and if you do so you should make the bomb not disarmable  (!toggledisarmable)");
	}

	public void do_setFixedSpeedLimit(String name, String message) {

		if (!opList.isER(name) && m_eventState == 0)
			return;

		if (Tools.isAllDigits(message)) {
			int i = Integer.parseInt(message) - 1;

			if (i >= 0 && i <= 4) {
				m_fixedSpeedLimit = true;
				m_speedLimit = i;
				m_botAction.sendPrivateMessage(name, "Fixed SpeedLimit set to "+m_speedLimits[i]+".");
			} else {
				m_botAction.sendPrivateMessage(name, "Correct format: !setfixedspeedlimit off = off, 1 = 500, 2 = 750, 3 = 1000, 4 = 1250, 5 = 1500.");
			}
		} else if (message.equals("off")) {
			m_fixedSpeedLimit = false;
			m_botAction.sendPrivateMessage(name, "Fixed SpeedLimit disabled.");
		} else {
			m_botAction.sendPrivateMessage(name, "Correct format: !setfixedspeedlimit off = off, 1 = 500, 2 = 750, 3 = 1000, 4 = 1250, 5 = 1500.");
		}
	}

	public void do_setInterval(String name, String message) {

		if (!opList.isER(name) && m_eventState == 0)
			return;

		if (m_fixedSpeedLimit) {
			m_botAction.sendPrivateMessage(name, "You need to disable fixed SpeedLimit first.  !setfixedspeedlimit off.");
		} else {
			if (Tools.isAllDigits(message)) {
				m_speedLimitInterval = Integer.parseInt(message);
				m_botAction.sendPrivateMessage(name, "SpeedLimit change interval set to "+message+" mins.");
			} else {
				m_botAction.sendPrivateMessage(name, "Correct format: !setinterval <#> mins.");
			}
		}
	}

	public void do_toggleDisarmable(String name, String message) {

		if (!opList.isER(name) && m_eventState == 0)
			return;

		if (m_bombDisarmable) {
			m_bombDisarmable = false;
			m_botAction.sendPrivateMessage(name, "Bomb cannot be disarmed.");
		} else {
			m_bombDisarmable = true;
			m_botAction.sendPrivateMessage(name, "Bomb can be disarmed.");
		}
	}

	public void do_toggleOneTeam(String name, String message) {

		if (!opList.isER(name) && m_eventState == 0)
			return;

		if (m_teamsOfOne) {
			m_teamsOfOne = false;
			m_botAction.sendPrivateMessage(name, "I won't arrange teams, hopefully you or another bot will!");
		} else {
			m_teamsOfOne = true;
			m_botAction.sendPrivateMessage(name, "Players will be will be forced on teams of 1.");
		}
	}

	public void do_toggleReversed(String name, String message) {

		if (!opList.isER(name) && m_eventState == 0)
			return;

		if (m_speedLimitReversed) {
			m_speedLimitReversed = false;
			m_botAction.sendPrivateMessage(name, "SpeedLimit set to normal.");
		} else {
			m_speedLimitReversed = true;
			m_botAction.sendPrivateMessage(name, "SpeedLimit set to reversed.");
		}
	}

	public void do_toggleDoors(String name, String message) {

		if (!opList.isER(name) && m_eventState == 0)
			return;

		if (m_doorsActivated) {
			m_doorsActivated = false;
			m_botAction.sendPrivateMessage(name, "Doors will stay open.");
		} else {
			m_doorsActivated = true;
			m_botAction.sendPrivateMessage(name, "Doors will randomly open/close.");
		}
	}

	public void do_spamRules(String name, String message) {

		if (!opList.isER(name))
			return;

		m_botAction.sendArenaMessage("Speed rules:  To stay in the game you must obey the speedlimit!  Last one in wins!", 1);
		spamSettings();
	}

	public void spamSettings() {
		if (m_speedLimitReversed) {
			m_botAction.sendArenaMessage("SpeedLimit is set to REVERSED mode, which means you must fly UNDER the speedlimit or you are out!");
		} else {
			m_botAction.sendArenaMessage("SpeedLimit is set to NORMAL mode, which means you must fly OVER the speedlimit or you are out!");
		}

		if (m_bombDisarmable) {
			m_botAction.sendArenaMessage("This round you can disarm the bomb, to disarm it you must score 3 goals!  First one to disarm the bomb OR last one in wins!");
		} else {
			m_botAction.sendArenaMessage("This round you cannot disarm the bomb.  Last player in wins!");
		}
	}

	public void startGame() {

		m_botAction.toggleLocked();
		registerPlayers();

//		if (players.size() >= 2) {
			m_eventStartTime = (int)System.currentTimeMillis();
			if (m_teamsOfOne) {
				m_botAction.createRandomTeams(1);
			}
			m_botAction.changeAllShips(1);

			/*TimerTask positionCheck = new TimerTask() {
				public void run() {
					updatePositions();
				}
			};*/
//			m_botAction.scheduleTaskAtFixedRate(positionCheck, 100, 100);

			if (m_fixedSpeedLimit) {
				m_botAction.sendArenaMessage("NOTICE: SpeedLimit ["+getFigure()+"] [Fixed "+m_speedLimits[m_speedLimit]+"] comes into effect in 10 seconds!!", 2);
				TimerTask fixed = new TimerTask() {
					public void run() {
						m_eventState = 2;
						m_botAction.showObject(50 + m_speedLimit);
						m_botAction.sendArenaMessage("WARNING: SpeedLimit ["+getFigure()+"] is now "+m_speedLimits[m_speedLimit]+"!!!", 19);
					}
				};
				m_botAction.scheduleTask(fixed, 10000);
			} else {
				if (m_speedLimitReversed) {
					m_speedLimit = 5;
				} else {
					m_speedLimit = -1;
				}
				changeSpeedLimit();
			}

			if (m_doorsActivated) {
				setupDoors();
			} else {
				m_botAction.setDoors(0);
			}
//		} else {
//			endGame(true);
//		}
	}

	public void updatePositions() {
		Iterator<String> it = players.keySet().iterator();

		while (it.hasNext()) {
			String name = (String)it.next();
			m_botAction.spectatePlayer(name);
		}
	}

	public void registerPlayers() {
		players.clear();

		Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
		while (it.hasNext()) {
			Player p = (Player)it.next();
			players.put(p.getPlayerName(), new SpeedPlayer(p.getPlayerName()));
		}
	}

	public void changeSpeedLimit() {

		int newLimit;

		if (m_speedLimitReversed) {
			newLimit = m_speedLimits[m_speedLimit - 1];
		} else {
			newLimit = m_speedLimits[m_speedLimit + 1];
		}

		m_botAction.sendArenaMessage("NOTICE: SpeedLimit ["+getFigure()+"] will change to "+newLimit+" in 10 seconds!!", 2);
		TimerTask fixed = new TimerTask() {
			public void run() {
				m_eventState = 2;
				m_botAction.hideObject(50 + m_speedLimit);


				if (m_speedLimitReversed) {
					m_speedLimit--;
				} else {
					m_speedLimit++;
				}

				m_botAction.showObject(50 + m_speedLimit);
				m_botAction.sendArenaMessage("WARNING: SpeedLimit ["+getFigure()+"] is now "+m_speedLimits[m_speedLimit]+"!!!", 19);

				if (m_speedLimit != 4) {
					TimerTask changeSpeedL = new TimerTask() {
						public void run() {
							changeSpeedLimit();
						}
					};
					m_botAction.scheduleTask(changeSpeedL, m_speedLimitInterval * 1000 * 60 - 10000);
				}
			}
		};
		m_botAction.scheduleTask(fixed, 10000);
	}

	public void setupDoors() {
		TimerTask doorToggle = new TimerTask() {
			public void run() {
				int rand = 1 + (int)(Math.random() * 255);
				m_botAction.setDoors(rand);
			}
		};
		m_botAction.scheduleTaskAtFixedRate(doorToggle, 15000, 15000);
	}

	public String getFigure() {
		if (m_speedLimitReversed) {
			return "MAXIMUM";
		} else {
			return "MINIMUM";
		}
	}

	public void endGame(boolean forced) {

		m_eventState = 0;

		if (forced) {
			m_botAction.sendArenaMessage("Event has been stopped.");
		} else {
			declareWinner();
		}

		m_botAction.cancelTasks();

		m_botAction.hideObject(50 + m_speedLimit);
		String obj = "";
		for (int i = 0; i <= 40; i++) {
			obj += "-"+i+",";
		}
		m_botAction.sendUnfilteredPublicMessage("*objset "+obj);
		m_botAction.toggleLocked();
	}

	public void declareWinner() {
		String winner = "- Nobody (?) -";

		Iterator<String> it = players.keySet().iterator();
		while (it.hasNext()) {
			winner = (String)it.next();
		}

		m_botAction.sendArenaMessage(winner+" wins!!", 5);
	}

	public void handleEvent(Message event) {
		m_commandInterpreter.handleEvent(event);

		if (event.getMessageType() == Message.ARENA_MESSAGE) {
			if (event.getMessage().equals("Arena LOCKED")) {
				handleArenaLock(true);
			} else if (event.getMessage().equals("Arena UNLOCKED")) {
				handleArenaLock(false);
			} else if (event.getMessage().startsWith("Team Goal! by ")) {
				handleSoccerGoal(event.getMessage().substring(14));
			} else if (event.getMessage().startsWith("Enemy Goal! by ")) {
				handleSoccerGoal(event.getMessage().substring(15));
			}
		}
	}

	public void handleEvent(PlayerPosition event) {

		String name = m_botAction.getPlayerName(event.getPlayerID());
		if (m_eventState > 0 && players.containsKey(name)) {

			SpeedPlayer p = players.get(name);

			int speed = getSpeed(event.getXVelocity(), event.getYVelocity());
			double speedID = (speed / (double)MAX_SPEED) * 40;

			if (speedID > 40) {
				speedID = 40;
			}

			if ((int)speedID != p.getSpeed() && p.timeFromUpdate() > 300) {
				String objSet = "";

				if (p.getSpeed() < speedID) {
					for (int i = p.getSpeed() + 1; i <= speedID; i++) {
						objSet += "+"+i+",";
					}
				} else {
					for (int i = p.getSpeed(); i > speedID; i--) {
						objSet += "-"+i+",";
					}
				}
				m_botAction.sendUnfilteredPrivateMessage(event.getPlayerID(), "*objset "+objSet);

				p.setSpeed((int)speedID);
			}

		if (m_eventState == 2 && p.timeFromDeath() > 6000 && ((speed < m_speedLimits[m_speedLimit] && !m_speedLimitReversed) || (speed > m_speedLimits[m_speedLimit] && m_speedLimitReversed))) {
				handleExplosion(m_botAction.getPlayerName(event.getPlayerID()), speed);
			}
		}
	}

	public void handleEvent(PlayerEntered event) {
		String out = "Welcome to Speed!";
		if (m_eventState == 2) {
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

	public void handleEvent(PlayerDeath event) {
		if (m_eventState == 2) {
			String name = m_botAction.getPlayerName(event.getKilleeID());

			if (players.containsKey(name)) {
				SpeedPlayer p = players.get(name);
				p.died();
			}
		}
	}

	public void handleExplosion(String name, int speed) {
		m_botAction.spec(name);
		m_botAction.spec(name);
		players.remove(name);
		m_botAction.sendArenaMessage("Oh no!!  There goes "+name+"'s ship!!  [Time: "+getTimeString()+"]  [Speed: "+speed+"km/h  Limit: "+m_speedLimits[m_speedLimit]+"km/h]");

		if (players.size() <= 1) {
			endGame(false);
		}
	}

	public void handleLagOut(String name) {
		if (m_eventState != 0) {
			players.remove(name);

			if (players.size() <= 1) {
				endGame(false);
			}
		}
	}

	public void handleArenaLock(boolean locked) {
		if (locked && m_eventState == 0) {
			m_botAction.toggleLocked();
		} else if (!locked && m_eventState != 0) {
			m_botAction.toggleLocked();
		}
	}

	public void handleSoccerGoal(String name) {
		if (m_eventState == 2 && m_bombDisarmable && players.containsKey(name)) {
			SpeedPlayer p = players.get(name);

			p.incGoals();

			if (p.getGoals() >= GOALS_REQUIRED) {
				m_botAction.sendArenaMessage(name+" has disarmed the bomb and wins the game!!", 5);
				endGame(true);
			} else {
				int i = GOALS_REQUIRED - p.getGoals();

				if (i == 1) {
					m_botAction.sendPrivateMessage(name, "You need to score one more goal to disarm the bomb!");
				} else {
					m_botAction.sendPrivateMessage(name, "You need to score "+i+" more goals to disarm the bomb.");
				}
			}
		}
	}

	public int getSpeed(int vX, int vY) {
		double speed = Math.sqrt(Math.pow(vX, 2) + Math.pow(vY, 2));

		return (int)speed;
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
		m_botAction.cancelTasks();
	}

	class SpeedPlayer {

		String name;
		int speed;
		int lastUpdate = (int)System.currentTimeMillis();

		int lastDeath = (int)System.currentTimeMillis();

		int goals;

		public SpeedPlayer(String name) {
			this.name = name;
		}

		public String getPlayerName() {
			return name;
		}

		public int getSpeed() {
			return speed;
		}

		public void setSpeed(int speed) {
			this.speed = speed;
			lastUpdate = (int)System.currentTimeMillis();
		}

		public int timeFromUpdate() {
			return (int)System.currentTimeMillis() - lastUpdate;
		}

		public int timeFromDeath() {
			return (int)System.currentTimeMillis() - lastDeath;
		}

		public void died() {
			lastDeath = (int)System.currentTimeMillis();
		}

		public int getGoals() {
			return goals;
		}

		public void incGoals() {
			goals++;
		}
	};
}

