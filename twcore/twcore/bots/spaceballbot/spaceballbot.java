package twcore.bots.spaceballbot;

import twcore.core.*;
import java.util.*;
import java.math.*;

public class spaceballbot extends SubspaceBot {
	
	private BotSettings m_botSettings;
	private Ship oShip;

	int PLANET_1_BORDER = 7776;
	int PLANET_2_BORDER = 8608;
	int Y_BORDER_1 = 6864;
	int Y_BORDER_2 = 9536;

	double BULLET_SPEED = 5000.0;
	double BOMB_SPEED = 8000.0;

	LinkedList fired_projectiles = new LinkedList();
	LinkedList team1_cannons = new LinkedList();
	LinkedList team2_cannons = new LinkedList();
	HashMap players = new HashMap();

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

	int botMass = 300;

	int winner;

	public spaceballbot(BotAction botAction) {
		super(botAction);
		requestEvents();

		m_botSettings = m_botAction.getBotSettings();
		oShip = m_botAction.getShip();
	}
	
	/** Request events that this bot requires to receive.  */
	public void requestEvents() {
		EventRequester req = m_botAction.getEventRequester();
		req.request(EventRequester.MESSAGE);
		req.request(EventRequester.ARENA_JOINED);
		req.request(EventRequester.PLAYER_ENTERED);
		req.request(EventRequester.PLAYER_POSITION);
		req.request(EventRequester.PLAYER_LEFT);
		req.request(EventRequester.PLAYER_DEATH);
		req.request(EventRequester.WEAPON_FIRED);
		req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
		req.request(EventRequester.LOGGED_ON);
	}
	

	public void handleEvent(Message event) {

		String name = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());
		if (name == null) name = "-anonymous-";

		String message = event.getMessage();

		if (message.equalsIgnoreCase("!play") || message.equalsIgnoreCase("!return") || message.equalsIgnoreCase("!lagout")) {
			if (eventState != 0) {
				Player p = m_botAction.getPlayer(name);
				if (p != null && p.isShip(0)) {
					SBPlayer sbP = (SBPlayer) players.get(name);
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
		
		if (!m_botAction.getOperatorList().isER(name) && !name.equalsIgnoreCase("Sika")) {
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

	public void handleEvent(PlayerEntered event) {
		if (eventState != 0) {
			String status = "Welcome to Spaceball!";
			if (eventState == 2) {
				status += " Use !play to get in and !help for instructions";
			}
			m_botAction.sendPrivateMessage(event.getPlayerID(), status);
		}
	}	

	public void handleEvent(PlayerLeft event) {
		if (eventState == 0) {
			return;
		}

		SBPlayer p = (SBPlayer) players.get(m_botAction.getPlayerName(event.getPlayerID()));
		if (p != null && !p.isLagged()) {
			p.setLagged(true);
		}
	}

	public void handleEvent(PlayerDeath event) {
		if (eventState == 0) {
			return;
		}

		SBPlayer killer = (SBPlayer) players.get(m_botAction.getPlayerName(event.getKillerID()));
		SBPlayer killee = (SBPlayer) players.get(m_botAction.getPlayerName(event.getKilleeID()));

		if (killer != null) {
			killer.incrementKills();
		}
		if (killee != null) {
			killee.incrementDeaths();
		}
	}

	public void handleEvent(FrequencyShipChange event) {
		if (eventState == 0) {
			return;
		}

		Player p2 = m_botAction.getPlayer(event.getPlayerID());
		SBPlayer p = (SBPlayer) players.get(p2.getPlayerName());
		if (p != null && event.getShipType() == 0) {
			p.setLagged(true);
			m_botAction.sendPrivateMessage(p2.getPlayerName(), "Use !return to get back in");
		}
	}

	public void handleEvent(PlayerPosition event) {
		if (eventState != 2 && eventState != 3) {
			return;
		}
		SBPlayer p = (SBPlayer)players.get(m_botAction.getPlayerName(event.getPlayerID()));
		if (p == null) {
			Player p2 = m_botAction.getPlayer(event.getPlayerID());
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
				ListIterator it;
				if (p.getTeam() == 0) {
					it = team1_cannons.listIterator();
				} else {
					it = team2_cannons.listIterator();
				}
				while (it.hasNext()) {
					Cannon c = (Cannon)it.next();
					if (c.isInArea(event.getXLocation(), event.getYLocation()) && !c.inUse()) {
						c.attach(p);
					}
				}
			}
		}
	}


	public void handleEvent(WeaponFired event) {
		if (eventState != 2) {
			return;
		}
		SBPlayer p = (SBPlayer)players.get(m_botAction.getPlayerName(event.getPlayerID()));
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

		double bearing = (double)Math.PI * 2 * (double)event.getRotation() / (double)40.0;

		double bVX = event.getXVelocity() + (short)(pSpeed * Math.sin(bearing));
		double bVY = event.getYVelocity() - (short)(pSpeed * Math.cos(bearing));

//		m_botAction.sendArenaMessage("Weapon phyred! " + p + " X:" + event.getXLocation() + " Y:" + event.getYLocation() + " VX:" + event.getXVelocity() + " BVX:" + bVX + " VY:" + event.getYVelocity() + " BVY:" + bVY);

		fired_projectiles.add(new Projectile(p, event.getXLocation() + (short)(10.0 * Math.sin(bearing)), event.getYLocation() - (short)(10.0 * Math.cos(bearing)), event.getXVelocity() + (short)(pSpeed * Math.sin(bearing)), event.getYVelocity() - (short)(pSpeed * Math.cos(bearing)), event.getWeaponType()));
	}

	public void handleEvent(LoggedOn event) {
		try
		{
			m_botAction.joinArena(m_botSettings.getString("arena"), (short)6000, (short)6000);
		}
		catch (Exception e)
		{
			System.out.println("!??!?!");
			m_botAction.joinArena(m_botSettings.getString("arena"));
		}
	}
	
	
	/* set ReliableKills 1 (*relkills 1) to make sure your bot receives every packet */
	public void handleEvent(ArenaJoined event) {
		m_botAction.setReliableKills(1);
		oShip = m_botAction.getShip();
	}

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

	public void startGame() {
		Iterator i = m_botAction.getPlayingPlayerIterator();
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

	public void finishGame() {
		if (updateState != null)
			updateState.cancel();

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

	public void displayScores() {

		Comparator a = new Comparator() {
			public int compare(Object oa, Object ob) {
				SBPlayer pa = (SBPlayer) players.get(oa), pb = (SBPlayer) players.get(ob);
				if (pb.getTotalHits() > pa.getTotalHits()) {
					return 1;
				} else {
					return 0;
				}
			};
		};
		
		Object[] sorted_p = (Object[]) players.keySet().toArray(new Object[players.size()]);
		Arrays.sort(sorted_p, a);

		m_botAction.sendArenaMessage(",----------------+ Planet 1 has survived +----------------.");
		m_botAction.sendArenaMessage("|  PLANET 1                                     PLANET 2  |");
		m_botAction.sendArenaMessage("+---------------+-----------+ +---------------+-----------+");
		m_botAction.sendArenaMessage("| Name          | Hits Acc% | | Name          | Hits Acc% |");
		m_botAction.sendArenaMessage("+---------------+-----------+ +---------------+-----------+");

		ArrayList p_1out = new ArrayList();
		ArrayList p_2out = new ArrayList();

		SBPlayer mostHits = null;
		SBPlayer bestAcc = null;
		SBPlayer mostKills = null;
		SBPlayer mostDeaths = null;

		for (int i = 0; i < sorted_p.length; i++) {
			SBPlayer p = (SBPlayer) players.get(sorted_p[i]);
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
		String out1[] = (String[]) p_1out.toArray(new String[p_1out.size()]);
		String out2[] = (String[]) p_2out.toArray(new String[p_2out.size()]);

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

	public void gameOver() {
		eventState = 3;
		updateState.cancel();
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
					if (c2 == 2) {
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

	public void announceWinner() {
		killLosers.cancel();
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

	public void updateState() {
//		m_botAction.sendArenaMessage("BOTLOC: " + botX + " . " + botY + " VX: " + botVX + " VY: " + botVY + " AGE: " + getAge());

		checkCollision();
		int botX = (int)getBotX();
		int botY = (int)getBotY();

		if (botVX == 0 && botVY == 0) {
			oShip.move(botX, botY);
		} else {
			int dir = (int) (Math.random() * 360);
			oShip.move(dir, botX, botY, botVX, botVY, 0, 1500, 1337);
		}

		ListIterator it = fired_projectiles.listIterator();
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
			botVY = (int)(botVY * (-1) / (double)1.2);
		}
	}

	public void noteHit(Projectile b, int bX, int bY) {
		lastMove = (int)(System.currentTimeMillis());
		lBotX = bX;
		lBotY = bY;

		double nXV = (botVX * botMass + b.getXVelocity() * b.getMass()) / (botMass + b.getMass());
		double nYV = (botVY * botMass + b.getYVelocity() * b.getMass()) / (botMass + b.getMass());
		botVX = (int)nXV;
		botVY = (int)nYV;
	}

	public double getBotX() { double bX = lBotX + (botVX * getAge() / (double) 10000.0); return bX; }

	public double getBotY() { double bY = lBotY + (botVY * getAge() / (double) 10000.0); return bY; }

	public double getAge() { return (int)(System.currentTimeMillis()) - lastMove; }
}