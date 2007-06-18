package twcore.bots.multibot.rabbit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.FlagClaimed;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.StringBag;

/**
 * For hosting rabbit.
 *
 * Created on March 26, 2005, 4:30 AM
 *
 * @author  Stultus
 */
public class rabbit extends MultiModule
	{
	private static final int ANY_SHIP = 0;
	private static final String[] SHIP_LIST =
		{
		"None",
		"Warbirds",
		"Javelins",
		"Spiders",
		"Leviathans",
		"Terriers",
		"Weasels",
		"Lancasters",
		"Sharks"
		};

	private boolean	inProgress = false;
	private int timeLimit = 15;
	private int shipType = ANY_SHIP;
	private ArrayList<String> rabbitList = new ArrayList<String>(4);
	private StringBag fcMsgs = new StringBag();
	private StringBag killMsgs = new StringBag();

	public void init() { }

	public void requestEvents(ModuleEventRequester events)	{
		events.request(this, EventRequester.FLAG_CLAIMED);
		events.request(this, EventRequester.PLAYER_DEATH);
	}

	public void handleEvent(Message event)
		{
		String message = event.getMessage();
		if (event.getMessageType() == Message.PRIVATE_MESSAGE)
			{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if (opList.isER(name))
				{
				handleCommand(name, message);
				}
			}
		}

	public void handleEvent(FlagClaimed event)
		{
		if (inProgress)
			{
			String p = m_botAction.getPlayerName(event.getPlayerID());
			rabbitList.add(p);
			if (!fcMsgs.isEmpty())
				{
				m_botAction.sendArenaMessage(p + fcMsgs.grab());
				}
			}
		}

	public void handleEvent(PlayerDeath event)
		{
		if (inProgress && !rabbitList.isEmpty())
			{
			String pKilled = m_botAction.getPlayerName(event.getKilleeID());
			String pKiller = m_botAction.getPlayerName(event.getKillerID());
			if (rabbitList.contains(pKilled))
				{
				rabbitList.remove(pKilled);
				rabbitList.add(pKiller);
				if (!fcMsgs.isEmpty())
					{
					m_botAction.sendArenaMessage(pKiller + fcMsgs.grab());
					}
				}
			if (rabbitList.contains(pKiller) && !killMsgs.isEmpty())
				{
				m_botAction.sendArenaMessage(pKiller + killMsgs.grab());
				}
			}
		}

	public void handleCommand(String name, String message)
		{
		if (message.toLowerCase().startsWith("!startrabbit"))
			startRabbit(name, message);
		else if (message.toLowerCase().equals("!stoprabbit"))
			stopRabbit(name);
		else if (message.toLowerCase().startsWith("!setshiptype"))
			setShipTypeLimit(name, message);
		else if (message.toLowerCase().startsWith("!killmsg"))
			addKillMessage(name, message);
		else if (message.toLowerCase().startsWith("!fcmsg"))
			addFCMessage(name, message);
		else if (message.toLowerCase().equals("!remkill"))
			removeKillMessages(name);
		else if (message.toLowerCase().equals("!remfc"))
			removeFCMessages(name);
		else if (message.toLowerCase().equals("!rabbitsettings"))
			showSettings(name);
		}

	public void startRabbit(String name, String message)
		{
		if (!inProgress)
			{
			timeLimit = explodeToInt(message, 1, " ");
			if (timeLimit == -1)
				{
				timeLimit = 15;
				m_botAction.sendPrivateMessage(name, "That is not properly "
				+ "formatted. Defaulting to 15 minutes.");
				}
			if (getPlayerCount() >= 3)
				{
				m_botAction.toggleLocked();
				m_botAction.sendArenaMessage("Rabbit mode activated by "
				+ name + ".");
				m_botAction.sendArenaMessage("Get ready, game starts in 10 "
				+ "seconds.", 2);

				TimerTask makeTeams = new TimerTask()
					{
					public void run()
						{
						if (shipType != ANY_SHIP)
							{
							m_botAction.changeAllShips(shipType);
							}
						m_botAction.createRandomTeams(1);
						}
					};

				TimerTask startIt = new TimerTask()
					{
					public void run()
						{
						inProgress = true;
						m_botAction.scoreResetAll();
						m_botAction.setTimer(timeLimit);
						m_botAction.sendArenaMessage("Go go go!", 104);
						}
					};

				TimerTask endIt = new TimerTask()
					{
					public void run()
						{
						endRabbit();
						}
					};

				m_botAction.scheduleTask(makeTeams, 9000);
				m_botAction.scheduleTask(startIt, 10000);
				m_botAction.scheduleTask(endIt, (timeLimit * 60 * 1000)
				+ 11000);
				}
			else
				{
				m_botAction.sendPrivateMessage(name, "3 or more players are "
				+ "required for a game of Rabbit.");
				}
			}
		else
			{
			m_botAction.sendPrivateMessage(name, "There is already a game in "
			+ "progress.");
			}
		}

	public void endRabbit()
		{
		inProgress = false;
		m_botAction.sendArenaMessage("MVP: " + getMVP() + "!", 7);
		m_botAction.toggleLocked();
		rabbitList.clear();
		}

	public void stopRabbit(String name)
		{
		if (inProgress)
			{
			inProgress = false;
			rabbitList.clear();
			m_botAction.cancelTasks();
			m_botAction.sendArenaMessage("This game has been brutally killed "
			+ "by " + name + ".", 13);
			m_botAction.toggleLocked();
			}
		else
			{
			m_botAction.sendPrivateMessage(name, "There was no game in "
			+ "progress.");
			}
		}

	public void setShipTypeLimit(String name, String message)
		{
		shipType = explodeToInt(message, 1, " ");
		if (shipType == -1)
			{
			shipType = ANY_SHIP;
			m_botAction.sendPrivateMessage(name, "That is not properly "
			+ "formatted. Defaulting to any ship.");
			}
		else if (shipType < 0 || shipType > 8)
			{
			m_botAction.sendPrivateMessage(name, "That is not a valid ship "
			+ "number. Defaulting to 'any' (0).");
			}
		else
			{
			m_botAction.sendPrivateMessage(name, "Ship type limit set.");
			}
		}

	public void addKillMessage(String name, String message)
		{
		if (message.length() > 8)
			{
			try
				{
				String[] args = message.substring(8).split(":");
				int sound = Integer.parseInt(args[1]);
				if (sound < 0 || sound > 104)
					{
					sound = 0;
					m_botAction.sendPrivateMessage(name, "Sound not within "
					+ "valid range (1-104). Defaulting to none.");
					}
				killMsgs.add(args[0] + "%" + args[1]);
				m_botAction.sendPrivateMessage(name, "Flag kill message added.");
				}
			catch (Exception e)
				{
				m_botAction.sendPrivateMessage(name, "That is not properly "
				+ "formatted.");
				}
			}
		else
			{
			List killMsgsOut = killMsgs.getList();
			Iterator it = killMsgsOut.iterator();
			int i = 1;
			while (it.hasNext())
				{
				m_botAction.sendPrivateMessage(name, "[" + i + "]" + it.next());
				i++;
				}
			}
		}

	public void addFCMessage(String name, String message)
		{
		if (message.length() > 6)
			{
			try
				{
				String[] args = message.substring(6).split(":");
				int sound = Integer.parseInt(args[1]);
				if (sound < 0 || sound > 104)
					{
					sound = 0;
					m_botAction.sendPrivateMessage(name, "Sound not within "
					+ "valid range (1-104). Defaulting to none.");
					}
				fcMsgs.add(args[0] + "%" + args[1]);
				m_botAction.sendPrivateMessage(name, "Flag claim message added.");
				}
			catch (Exception e)
				{
				m_botAction.sendPrivateMessage(name, "That is not properly "
				+ "formatted.");
				}
			}
		else
			{
			List fcMsgsOut = fcMsgs.getList();
			Iterator it = fcMsgsOut.iterator();
			int i = 1;
			while (it.hasNext())
				{
				m_botAction.sendPrivateMessage(name, "[" + i + "]" + it.next());
				i++;
				}
			}
		}

	public void removeKillMessages(String name)
		{
		killMsgs.clear();
		m_botAction.sendPrivateMessage(name, "Kill messages cleared.");
		}

	public void removeFCMessages(String name)
		{
		fcMsgs.clear();
		m_botAction.sendPrivateMessage(name, "Flag claim messages cleared.");
		}

	public void showSettings(String name)
		{
		String shipTypeOut = SHIP_LIST[shipType];
		String[] settings =
			{
			"     Time limit: " + timeLimit + " minutes",
			"Ship type limit: " + shipTypeOut
			};

		m_botAction.privateMessageSpam(name, settings);
		}

	public String[] getModHelpMessage()
		{
		String[] help =
			{
			"!startrabbit <time>             - Starts a game of Rabbit (default = 15 minutes).",
			"!stoprabbit                     - Kills a game in progress.",
			"!setshiptype <ship #>           - Restricts players to this ship. (0 = any)",
			"!killmsg <message>:<sound>      - Adds a rabbit kill message and sound (1-104; 0 = none);",
			"                                  With no arguments, lists current messages.",
			"!fcmsg <message>:<sound>        - Adds a flag claim message and sound.",
			"!remkill                        - Clears list of kill messages.",
			"!remfc                          - Clears list of flag claim messages.",
			"!rabbitsettings                 - Shows current time limit and ship type limit."
			};
		return help;
		}

	public void cancel() {}

	public boolean isUnloadable()	{
		return true;
	}

	private int explodeToInt(String message, int index, String separator)
		{
		try
			{
			return Integer.parseInt(message.split(separator)[index]);
			}
		catch (Exception e)
			{
			return -1;
			}
		}

	private int getPlayerCount()
		{
		int freq = -1;
		int playerCount = 1;
		Iterator it = m_botAction.getPlayingPlayerIterator();
		if (it == null)
			return 0;
		while (it.hasNext())
			{
			Player p = (Player)it.next();
			if (freq == -1)
				freq = p.getFrequency();
			if (p.getFrequency() != freq)
				playerCount++;
			}
		return playerCount;
		}

	private String getMVP()
		{
		int bestScore = 0;
		String playerName = "";
		Iterator it = m_botAction.getPlayerIterator();
		while (it.hasNext())
			{
			Player p = (Player)it.next();
			if (p.getScore() > bestScore)
				{
				bestScore = p.getScore();
				playerName = p.getPlayerName();
				}
			}
		return playerName;
		}
	}
