package twcore.bots.multibot.spaceball;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;
import twcore.core.game.Ship;
import twcore.core.util.Tools;

/**
 * Hosts a game of SpaceBall. Objectives:
 * Prevent the SpaceBall (bot) from reaching your planet's
 * athmosphere by hitting it with bullets and cannonballs.
 * Don't cross your planet's boundaries or your engines
 * will malfunction!
 */

public class spaceball extends MultiModule {

	private Ship oShip;

	int PLANET_1_BORDER = 7776;
	int PLANET_2_BORDER = 8608;
	int Y_BORDER_1 = 6864;
	int Y_BORDER_2 = 9536;

	double BULLET_SPEED = 5000.0;
	double BOMB_SPEED = 8000.0;

	LinkedList<Projectile> fired_projectiles = new LinkedList<Projectile>();
	LinkedList<Cannon> team1_cannons = new LinkedList<Cannon>();
	LinkedList<Cannon> team2_cannons = new LinkedList<Cannon>();
	HashMap<String, SBPlayer> players = new HashMap<String, SBPlayer>();

	TimerTask updateState;
	TimerTask killLosers;
	TimerTask tenSeconds;
	TimerTask fiveSeconds;

	int eventState = 0;		// 0 = nothing, 1 = starting, 2 = playing, 3 = cleaning up

	int botX = 8200;
	int botY = 8200;

	int lBotX = 8200;
	int lBotY = 8200;

	int lastMove = 0;
	int botVX = 0;
	int botVY = 0;

	int botMass = 2200;

	int winner;

	public void init() {
		oShip = m_botAction.getShip();
		String thisArena = m_botAction.getArenaName();
		try {
			m_botAction.joinArena(thisArena, (short)6000, (short)6000);
		} catch(Exception e) { m_botAction.joinArena(thisArena); }
	}

	/** Request events that this bot requires to receive.  */
	public void requestEvents(ModuleEventRequester req) {
		req.request(this, EventRequester.ARENA_JOINED);
		req.request(this, EventRequester.PLAYER_ENTERED);
		req.request(this, EventRequester.PLAYER_POSITION);
		req.request(this, EventRequester.PLAYER_LEFT);
		req.request(this, EventRequester.PLAYER_DEATH);
		req.request(this, EventRequester.WEAPON_FIRED);
		req.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
	}

	public boolean isUnloadable() {
		return eventState == 0;
	}
	
	/**
	 * This method is called when the module is unloaded
	 */
	public void cancel() {
		m_botAction.cancelTasks();
	}

	public String[] getModHelpMessage() {
		String[] bleh = {
				"+------------------------------------------------------------+",
				"| SpaceBallBot v.0.96                         - author Sika  |",
				"+------------------------------------------------------------+",
				"| SpaceBall objectives:                                      |",
				"|   Prevent the SpaceBall (bot) from reaching your planet's  |",
				"|   athmosphere by hitting it with bullets and cannonballs.  |",
				"|   Don't cross your planet's boundaries or your engines     |",
				"|   will malfunction!!                                       |",
				"+------------------------------------------------------------+",
				"| Commands:                                                  |",
				"|   !help OR !about OR !rules     - Brings up this message   |",
				"|   !return OR !lagout OR !play   - Get in the game          |",
				"+------------------------------------------------------------+",
				"| Host Commands:                                             |",
				"|   !start                        - Starts/stops the event   |",
				"|   !die                          - Kills the bot            |",
				"+------------------------------------------------------------+"
				
		};
		return bleh;
	}


	public void handleEvent(Message event) {

		String name = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());
		if (name == null) name = "-anonymous-";

		String message = event.getMessage();

		if (message.equalsIgnoreCase("!play") || message.equalsIgnoreCase("!return") || message.equalsIgnoreCase("!lagout")) {
			if (eventState != 0) {
				Player p = m_botAction.getPlayer(name);
				if (p != null && p.isShip(0)) {
					SBPlayer sbP = players.get(name);
					if (sbP != null && sbP.isLagged()) {
						m_botAction.setShip(name, 1);
						m_botAction.setFreq(name, sbP.getTeam());
						sbP.setLagged(false);
					} else {
						m_botAction.setShip(name, 1);
					}
				}
			}
		}

		if (message.equalsIgnoreCase("!help") || message.equalsIgnoreCase("!about") || message.equalsIgnoreCase("!rules")) {
			if (!name.equals("-anonymous-")) {
				handleHelp(name);
			}
		}

		if (!m_botAction.getOperatorList().isER(name)) {
			return;
		}

		if (event.getMessageType() == Message.PRIVATE_MESSAGE && message.equalsIgnoreCase("!die")) {
			try { Thread.sleep(50); } catch (Exception e) {};
			m_botAction.die();
		}

		if (message.startsWith("!start")) {
			if (eventState == 2) {
				finishGame();
			} else if (eventState == 0) {
				setupGame();
			}
		}

		if (message.startsWith("!speed")) {
			BULLET_SPEED = Integer.parseInt(event.getMessage().substring(7));
		}
		if (message.startsWith("!mass")) {
			botMass = Integer.parseInt(event.getMessage().substring(6));
		}
	}

	/**
	 * This method handles the PlayerEntered event and welcomes the players
	 * if there is a game in progress.

	 * @param event is the PlayerEntered event to handle
	 */

	public void handleEvent(PlayerEntered event) {
		if (eventState != 0) {
			String status = "Welcome to Spaceball! Use !help for instructions";
			if (eventState == 2) {
				status += " and !play to get in";
			}
			m_botAction.sendPrivateMessage(event.getPlayerID(), status);
		}
	}


	/**
	 * This method handles the PlayerLeft event and updates players lagstatus
	 * if they are in the event.
	 *
	 * @param event is the PlayerLeft event to handle
	 */

	public void handleEvent(PlayerLeft event) {
		if (eventState == 0) {
			return;
		}

		SBPlayer p = players.get(m_botAction.getPlayerName(event.getPlayerID()));
		if (p != null && !p.isLagged()) {
			p.setLagged(true);
		}
	}


	/**
	 * This method handles the PlayerDeath event and updates players kill/death stats.
	 *
	 * @param event is the PlayerDeath event to handle
	 */

	public void handleEvent(PlayerDeath event) {
		if (eventState == 0) {
			return;
		}

		SBPlayer killer = players.get(m_botAction.getPlayerName(event.getKillerID()));
		SBPlayer killee = players.get(m_botAction.getPlayerName(event.getKilleeID()));

		if (killer != null) {
			killer.incrementKills();
		}
		if (killee != null) {
			killee.incrementDeaths();
		}
	}


	/**
	 * This method handles the FrequencyShipChange event and if players change from a ship
	 * to spectator mode, it instructs them how to get back in.
	 *
	 * @param event is the FrequencyShipChange event to handle
	 */

	public void handleEvent(FrequencyShipChange event) {
		if (eventState == 0) {
			return;
		}

		Player p2 = m_botAction.getPlayer(event.getPlayerID());
		SBPlayer p = players.get(p2.getPlayerName());
		if (p != null && event.getShipType() == 0) {
			p.setLagged(true);
			m_botAction.sendPrivateMessage(p2.getPlayerName(), "Use !return to get back in");
		}
	}


	/**
	 * This method handles the PlayerPosition event. If the player isn't in the
	 * players HashMap, it registers him.
	 *
	 * It also checks if the player has crossed the planetary borders and handles
	 * cannons.
	 *
	 * @param event is the PlayerPosition event to handle
	 */

	public void handleEvent(PlayerPosition event) {
		if (eventState != 2 && eventState != 3) {
			return;
		}
		SBPlayer p = players.get(m_botAction.getPlayerName(event.getPlayerID()));
		if (p == null) {
			Player p2 = m_botAction.getPlayer(event.getPlayerID());
			if (p2 == null) {
				return;
			}
			players.put(p2.getPlayerName(), new SBPlayer(p2.getPlayerName(), p2.getFrequency()));
			return;
		}

		if (p.isCannon()) {
			if (!p.getCannon().isInArea(event.getXLocation(), event.getYLocation())) {
				p.getCannon().deAttach();
			}
		} else {
			if (((p.getTeam() == 0 && event.getXLocation() > PLANET_1_BORDER) || (p.getTeam() == 1 && event.getXLocation() < PLANET_2_BORDER)) && p.timeFromEShutDown() > 10) {
				p.setLastEShutDown();
				m_botAction.sendUnfilteredPrivateMessage(event.getPlayerID(), "*prize #14");
				m_botAction.sendPrivateMessage(event.getPlayerID(), "Do not cross the planetary boundaries!");
			} else {
				ListIterator<Cannon> it;
				if (p.getTeam() == 0) {
					it = team1_cannons.listIterator();
				} else {
					it = team2_cannons.listIterator();
				}
				while (it.hasNext()) {
					Cannon c = (Cannon)it.next();
					if (!c.inUse() && c.isInArea(event.getXLocation(), event.getYLocation())) {
						c.attach(p);
					}
				}
			}
		}
	}


	/**
	 * This method handles the WeaponFired event. If the projectile is a bomb or bullet
	 * it calculates the velocities and adds the projectile to fired_projectiles list.

	 * @param event is the WeaponFired event to handle
	 */

	public void handleEvent(WeaponFired event) {
		if (eventState != 2) {
			return;
		}
		SBPlayer p = players.get(m_botAction.getPlayerName(event.getPlayerID()));
		if (p == null) {
//			m_botAction.spec(m_botAction.getPlayerName(event.getPlayerID()));
//			m_botAction.spec(m_botAction.getPlayerName(event.getPlayerID()));
			return;
		}

		double pSpeed;
		if (event.isType(1)) {
			p.incrementBulletsFired();
			pSpeed = BULLET_SPEED;
		} else if (event.isType(3)) {
			p.incrementBombsFired();
			pSpeed = BOMB_SPEED;
		} else { return; }

		double bearing = Math.PI * 2 * (double)event.getRotation() / 40.0;

		/*double bVX = event.getXVelocity() + (short)(pSpeed * Math.sin(bearing));
		double bVY = event.getYVelocity() - (short)(pSpeed * Math.cos(bearing));*/

//		m_botAction.sendArenaMessage("Weapon phyred! " + p + " X:" + event.getXLocation() + " Y:" + event.getYLocation() + " VX:" + event.getXVelocity() + " BVX:" + bVX + " VY:" + event.getYVelocity() + " BVY:" + bVY);

		fired_projectiles.add(new Projectile(p, event.getXLocation() + (short)(10.0 * Math.sin(bearing)), event.getYLocation() - (short)(10.0 * Math.cos(bearing)), event.getXVelocity() + (short)(pSpeed * Math.sin(bearing)), event.getYVelocity() - (short)(pSpeed * Math.cos(bearing)), event.getWeaponType(), event.getWeaponLevel()));
	}


	/* set ReliableKills 1 (*relkills 1) to make sure your bot receives every packet */
	public void handleEvent(ArenaJoined event) {
		m_botAction.setReliableKills(1);
		oShip = m_botAction.getShip();
	}


	/**
	 * This method handles spamming players with the help/rules.

	 * @param name is the messager
	 */

	public void handleHelp(String name) {
		String[] out = {
			"+------------------------------------------------------------+",
			"| SpaceBallBot v.0.96                         - author Sika  |",
			"+------------------------------------------------------------+",
			"| SpaceBall objectives:                                      |",
			"|   Prevent the SpaceBall (bot) from reaching your planet's  |",
			"|   athmosphere by hitting it with bullets and cannonballs.  |",
			"|   Don't cross your planet's boundaries or your engines     |",
			"|   will malfunction!!                                       |",
			"+------------------------------------------------------------+",
			"| Commands:                                                  |",
			"|   !help OR !about OR !rules     - Brings up this message   |",
			"|   !return OR !lagout OR !play   - Get in the game          |",
			"+------------------------------------------------------------+"
		};
		m_botAction.privateMessageSpam(name, out);

		if (m_botAction.getOperatorList().isER(name)) {
			String[] out2 = {
				"| Host Commands:                                             |",
				"|   !start                        - Starts/stops the event   |",
				"|   !die                          - Kills the bot            |",
				"+------------------------------------------------------------+"
			};
			m_botAction.privateMessageSpam(name, out2);
		}
	}


	/**
	 * This method setups the game.
	 *
	 */

	public void setupGame() {
		eventState = 1;
		m_botAction.toggleLocked();
		m_botAction.setDoors(255);
		m_botAction.createNumberOfTeams(2);
		m_botAction.warpAllRandomly();

		m_botAction.sendArenaMessage("Get ready! Starting in 10 seconds..", 2);

		team1_cannons.add(new Cannon(7216, 7840, m_botAction));
		team1_cannons.add(new Cannon(7200, 8016, m_botAction));
		team1_cannons.add(new Cannon(7200, 8368, m_botAction));
		team1_cannons.add(new Cannon(7216, 8544, m_botAction));
		team2_cannons.add(new Cannon(9168, 7840, m_botAction));
		team2_cannons.add(new Cannon(9184, 8016, m_botAction));
		team2_cannons.add(new Cannon(9184, 8368, m_botAction));
		team2_cannons.add(new Cannon(9168, 8544, m_botAction));

		oShip.setShip(7);
		oShip.setFreq(1337);
		oShip.move(botX, botY);

		tenSeconds = new TimerTask() {
			public void run() {
				startGame();
			}
		};
		m_botAction.scheduleTask(tenSeconds, 10000);
	}


	/**
	 * This method starts the game.
	 *
	 */

	public void startGame() {
		Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
		while (i.hasNext())	{
			Player p = (Player) i.next();
			if (!p.getPlayerName().equalsIgnoreCase(m_botAction.getBotName())) {
				m_botAction.setShip(p.getPlayerName(), 1);
				players.put(p.getPlayerName(), new SBPlayer(p.getPlayerName(), p.getFrequency()));
			}
		}

		eventState = 2;
		m_botAction.setDoors(0);
		m_botAction.sendArenaMessage("Go go go! Aim for your lives!", 104);
		updateState = new TimerTask() {
			public void run() {
				updateState();
			}
		};
	    m_botAction.scheduleTaskAtFixedRate(updateState, 1000, 100);
	}


	/**
	 * This method finishes the game.
	 *
	 */

	public void finishGame() {
		if (updateState != null)
            m_botAction.cancelTask(updateState);

		botX = 8200;
		botY = 8200;

		lBotX = 8200;
		lBotY = 8200;

		lastMove = 0;
		botVX = 0;
		botVY = 0;
		oShip.move(botX, botY);
		oShip.setShip(8);

		displayScores();

		players.clear();
		team1_cannons.clear();
		team1_cannons.clear();

		m_botAction.toggleLocked();
		eventState = 0;
	}


	/**
	 * This method displays the end game stats .. quite messy :O
	 *
	 */

	public void displayScores() {

		Comparator<String> a = new Comparator<String>() {
			public int compare(String oa, String ob) {
				SBPlayer pa = players.get(oa), pb = players.get(ob);
				if (pb.getTotalHits() > pa.getTotalHits()) {
					return 1;
				} else {
					return 0;
				}
			};
		};

		String[] sorted_p = players.keySet().toArray(new String[players.size()]);
		Arrays.sort(sorted_p, a);

		m_botAction.sendArenaMessage(",----------------+ Planet "+winner+" has survived +----------------.");
		m_botAction.sendArenaMessage("|  PLANET 1                                     PLANET 2  |");
		m_botAction.sendArenaMessage("+---------------+-----------+ +---------------+-----------+");
		m_botAction.sendArenaMessage("| Name          | Hits Acc% | | Name          | Hits Acc% |");
		m_botAction.sendArenaMessage("+---------------+-----------+ +---------------+-----------+");

		ArrayList<String> p_1out = new ArrayList<String>();
		ArrayList<String> p_2out = new ArrayList<String>();

		SBPlayer mostHits = null;
		SBPlayer bestAcc = null;
		SBPlayer mostKills = null;
		SBPlayer mostDeaths = null;

		for (int i = 0; i < sorted_p.length; i++) {
			SBPlayer p = players.get(sorted_p[i]);
			if (mostHits == null || p.getTotalHits() > mostHits.getTotalHits()) {
				mostHits = p;
			}
			if (bestAcc == null || p.getAccuracy() > bestAcc.getAccuracy()) {
				bestAcc = p;
			}
			if (mostKills == null || p.getKills() > mostKills.getKills()) {
				mostKills = p;
			}
			if (mostDeaths == null || p.getDeaths() > mostDeaths.getDeaths()) {
				mostDeaths = p;
			}
			if (p.getTeam() == 0) {
				p_1out.add("| "+Tools.formatString(p.getName(), 14)+"  "+rightenString(Integer.toString(p.getTotalHits()), 4)+" "+rightenString(p.getAccuracy() + "%", 4));
			} else {
				p_2out.add(Tools.formatString(p.getName(), 14)+"  "+rightenString(Integer.toString(p.getTotalHits()), 4)+" "+rightenString(p.getAccuracy() + "%", 4)+" |");
			}
		}
		String out1[] = p_1out.toArray(new String[p_1out.size()]);
		String out2[] = p_2out.toArray(new String[p_2out.size()]);

		int s;
		if (out1.length > 5 || out2.length > 5) {
			s = 5;
		} else if (out1.length > out2.length) {
			s = out1.length;
		} else {
			s = out2.length;
		}

		for (int i = 0; i < s; i++) {
			String out;
			if (i > (out1.length - 1)) {
				out = "|                          ";
			} else {
				out = out1[i];
			}
			out += "     ";
			if (i > (out2.length - 1)) {
				out += "                          |";
			} else {
				out += out2[i];
			}
			m_botAction.sendArenaMessage(out);
		}
		m_botAction.sendArenaMessage("+---------------------------+ +---------------------------+");
		if (mostHits != null) { m_botAction.sendArenaMessage("- Most Hits: " + mostHits.getName() + " (" + mostHits.getTotalHits() + ")"); }
		if (bestAcc != null) {m_botAction.sendArenaMessage("- Best Accuracy: " + bestAcc.getName() + " (" + bestAcc.getAccuracy() + "%)"); }
		if (mostKills != null) {m_botAction.sendArenaMessage("- Most Kills: " + mostKills.getName() + " (" + mostKills.getKills() + ")"); }
		if (mostDeaths != null) {m_botAction.sendArenaMessage("- Most Deaths: " + mostDeaths.getName() + " (" + mostDeaths.getDeaths() + ")"); }
	}


	public String rightenString(String fragment, int length) {
		int curLength = fragment.length();
		int startPos = length - curLength;
		String result = "";

		for (int i=0; i < startPos; i++) result = result + " ";
			result = result + fragment;
		for (int j=result.length(); j < length; j++) result = result + " ";

		return result;
	}


	/**
	 * This method is called when the bot has crossed either of the planet's border.
	 * It does the gay bursting..
	 */

	public void gameOver() {
		eventState = 3;
        m_botAction.cancelTask(updateState);
		killLosers = new TimerTask() {

			int c = 0;
			int c2 = 0;
			int[] x = { 763,   496,  496, 1152, 1152, 1152, 1488, 1488 };
			int[] y = { 8192, 6944, 9480, 7728, 8192, 8656, 8000, 8300 };

			public void run() {
				int pX;
				if (winner == 1) {
					pX = 8192 + x[c];
				} else {
					pX = 8192 - x[c];
				}
				oShip.move(pX, y[c]);
				oShip.fire(7);
//				oShip.fire(7);
				if (c == 7) {
					c = 0;
					if (c2 == 0) {
						c = 0;
						c2 = 0;
						announceWinner();
					} else {
						c2++;
					}
				} else {
					c++;
				}
			}
		};
	    m_botAction.scheduleTaskAtFixedRate(killLosers, 1000, 500);
	}


	/**
	 * This method announces the winner and stalls the bot for 5 seconds
	 * so the bursts wont disappear instantly.
	 */

	public void announceWinner() {
        m_botAction.cancelTask(killLosers);
		if (winner == 1) {
			m_botAction.sendArenaMessage("Planet 1 has survived!!", 5);
		} else {
			m_botAction.sendArenaMessage("Planet 2 has survived!!", 5);
		}
		fiveSeconds = new TimerTask() {
			public void run() {
				finishGame();
			}
		};
		m_botAction.scheduleTask(fiveSeconds, 5000);
	}


	/**
	 * This method updates the bot location and detects collisions with
	 * walls/projectiles.
	 */

	public void updateState() {
//		m_botAction.sendArenaMessage("BOTLOC: " + botX + " . " + botY + " VX: " + botVX + " VY: " + botVY + " AGE: " + getAge());

		checkCollision();
		int botX = (int)getBotX();
		int botY = (int)getBotY();

		if (botVX == 0 && botVY == 0) {
			oShip.move(botX, botY);
		} else {
			int dir = (int) (Math.random() * 40);
			oShip.move(dir, botX, botY, botVX, botVY, 0, 1500, 1337);
		}

		ListIterator<Projectile> it = fired_projectiles.listIterator();
		while (it.hasNext()) {
			Projectile b = (Projectile) it.next();

//			m_botAction.sendArenaMessage("TRACKING: " + b.getXLocation() + " . " + b.getYLocation() + " fired by: " + b.getOwner() + " age:" + b.getAge() + " d:" + b.getDistance(botX, botY));

			if (b.isHitting(botX, botY)) {
				if (b.getType() == 1) {
					b.getOwner().incrementBulletsHit();
				} else {
					b.getOwner().incrementBombsHit();
				}
				m_botAction.sendUnfilteredPrivateMessage(b.getOwner().getName(), "*objon " + b.getOwner().getTeam());
//				m_botAction.sendArenaMessage("HIT!!! " + b.getOwner().getName() + " ns!");
				noteHit(b, botX, botY);
				it.remove();
			} else if (b.getAge() > 5000) {
				it.remove();
			}
		}
	}


	/**
	 * This method is called to detect collisions with walls/planet borders.
	 * If there is a collision with a wall, the Y velocity is converted to the
	 * opposite number and multiplied by 0.9 to slow down the bot a bit ..
	 */

	public void checkCollision() {
		if (getBotX() < PLANET_1_BORDER) {
			winner = 2;
			gameOver();
		} else if (getBotX() > PLANET_2_BORDER) {
			winner = 1;
			gameOver();
		}
		if ((getBotY() > Y_BORDER_2 && botVY > 0) || (getBotY() < Y_BORDER_1 && botVY < 0)) {
			lBotX = (int)getBotX();
			lBotY = (int)getBotY();
			lastMove = (int)(System.currentTimeMillis());
			botVY = (int)(botVY * (-1) * 0.9);
		}
	}


	/**
	 * This method calculates the new velocities after a collision with a projectile.

	 * @param b is the Projectile
	 * @param bX is the current bot x location
	 * @param bY is the current bot y location
	 */

	public void noteHit(Projectile b, int bX, int bY) {
		lastMove = (int)(System.currentTimeMillis());
		lBotX = bX;
		lBotY = bY;

		double nXV = (botVX * botMass + b.getXVelocity() * b.getMass()) / (botMass + b.getMass());
		double nYV = (botVY * botMass + b.getYVelocity() * b.getMass()) / (botMass + b.getMass());
		botVX = (int)nXV;
		botVY = (int)nYV;
	}


	/**
	 * This method calculates the bot x location.

	 * @return bX is the new bot x location
	 */

	public double getBotX() { double bX = lBotX + (botVX * getAge() / 10000.0); return bX; }


	/**
	 * This method calculates the bot y location.

	 * @return bY is the new bot y location
	 */

	public double getBotY() { double bY = lBotY + (botVY * getAge() / 10000.0); return bY; }


	/**
	 * This method calculates the time from last collision with a wall/projectile.

	 * @return is the age
	 */

	public double getAge() { return (int)(System.currentTimeMillis()) - lastMove; }
}