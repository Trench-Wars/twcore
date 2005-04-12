/*
 * twbotrevenge.java
 *
 * Created on April 6, 2005, 17:40
 *
 */

package twcore.bots.twbot;

import twcore.core.*;
import java.util.*;

public class twbotrevenge extends TWBotExtension
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

	private boolean inProgress = false;
	private int normalReward = 5;
	private int alreadyKilledReward = 2;
	private int revengeReward = 7;
	private int normalPenalty = 0;
	private int revengePenalty = 7;
	private int timeLimit = 10;
	private int shipType = ANY_SHIP;
	private HashMap playerMap;

	public twbotrevenge () {}

	public void handleEvent(Message event)
		{
		String message = event.getMessage().toLowerCase();
		if (event.getMessageType() == Message.PRIVATE_MESSAGE)
			{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			handleCommand(name, message);
			}
		}

	public void handleEvent(PlayerDeath event)
		{
		if (inProgress)
			{
			String killerName = m_botAction.getPlayerName(event.getKillerID());
			String killedName = m_botAction.getPlayerName(event.getKilleeID());
			RevengePlayer killer = (RevengePlayer)playerMap.get(killerName);
			RevengePlayer killed = (RevengePlayer)playerMap.get(killedName);

			if (killed.hasKilled(killerName))
				{
				killer.addPoints(revengeReward);
				m_botAction.sendPrivateMessage(killerName, "You have gained "
				+ revengeReward + " for killing " + killedName + ". (Total: "
				+ killer.getScore() + ")");

				killed.removePoints(revengePenalty);
				m_botAction.sendPrivateMessage(killedName, "You have lost "
				+ revengePenalty + " points for being killed by " + killerName
				+ ". (Total: " + killed.getScore() + ")");
				}
			else
				{
				if (!killer.hasKilled(killedName))
					{
					killer.addPoints(normalReward);
					m_botAction.sendPrivateMessage(killerName, "You have "
					+ "gained " + normalReward + " points for killing "
					+ killedName + ". (Total: " + killer.getScore() + ")");
					killer.addKilled(killedName);
					}
				else
					{
					killer.addPoints(alreadyKilledReward);
					m_botAction.sendPrivateMessage(killerName, "You have "
					+ "gained " + alreadyKilledReward + " points for killing "
					+ killedName + ". (Total: " + killer.getScore() + ")");
					}
				killed.removePoints(normalPenalty);
				m_botAction.sendPrivateMessage(killedName, "You have lost "
				+ normalPenalty + " points for being killed by " + killerName
				+ ". (Total: " + killed.getScore() + ")");
				}
			playerMap.remove(killerName);
			playerMap.remove(killedName);
			playerMap.put(killerName, killer);
			playerMap.put(killedName, killed);
			}
		}

	public void handleCommand(String name, String message)
		{
		if (m_opList.isER(name))
			{
			if (message.startsWith("!nreward"))
				setNormalReward(name, message);
			else if (message.startsWith("!rreward"))
				setRevengeReward(name, message);
			else if (message.startsWith("!areward"))
				setAlreadyKilledReward(name, message);
			else if (message.startsWith("!npenalty"))
				setNormalPenalty(name, message);
			else if (message.startsWith("!rpenalty"))
				setRevengePenalty(name, message);
			else if (message.startsWith("!setshiptype"))
				setShipTypeLimit(name, message);
			else if (message.startsWith("!startrevenge"))
				startRevenge(name, message);
			else if (message.equals("!stoprevenge"))
				stopRevenge(name);
			else if (message.equals("!settings"))
				showSettings(name);
			}
		if (message.equals("!score"))
			{
			tellScore(name);
			}
		}

	public void startRevenge(String name, String message)
		{
		if (!inProgress)
			{
			timeLimit = explodeToInt(message, 1, " ");
			if (timeLimit == -1)
				{
				timeLimit = 10;
				m_botAction.sendPrivateMessage(name, "That is not properly "
				+ "formatted. Defaulting to 10 minutes.");
				}

			m_botAction.toggleLocked();
			m_botAction.sendArenaMessage("Get ready, game starts in 10 "
			+ "seconds.", 2);

			TimerTask prepare = new TimerTask()
				{
				public void run()
					{
					if (shipType != ANY_SHIP)
						{
						m_botAction.changeAllShips(shipType);
						}
					m_botAction.createRandomTeams(1);
					int playerCount = getPlayerCount();
					Iterator it = m_botAction.getPlayingPlayerIterator();
					while (it.hasNext())
						{
						Player p = (Player)it.next();
						playerMap.put(p.getPlayerName(), new RevengePlayer(
p.getPlayerName(), playerCount));
						}
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
					endRevenge();
					}
				};

			m_botAction.scheduleTask(prepare, 9000);
			m_botAction.scheduleTask(startIt, 10000);
			m_botAction.scheduleTask(endIt, (timeLimit * 60 * 1000) + 11000);
			}
		else
			{
			m_botAction.sendPrivateMessage(name, "There is already a game "
			+ "in progress.");
			}
		}

	public void endRevenge()
		{
		inProgress = false;
		m_botAction.sendArenaMessage("MVP: " + getMVP() + "!", 7);
		m_botAction.toggleLocked();
		playerMap.clear();
		}

	public void stopRevenge(String name)
		{
		if (inProgress)
			{
			inProgress = false;
			playerMap.clear();
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

	public void setNormalReward(String name, String message)
		{
		normalReward = explodeToInt(message, 1, " ");
		if (normalReward == -1)
			{
			normalReward = 7;
			m_botAction.sendPrivateMessage(name, "That is not properly "
			+ "formatted. Defaulting to 7 points.");
			}
		else
			{
			m_botAction.sendPrivateMessage(name, "Normal reward set.");
			}
		}

	public void setRevengeReward(String name, String message)
		{
		revengeReward = explodeToInt(message, 1, " ");
		if (revengeReward == -1)
			{
			revengeReward = 7;
			m_botAction.sendPrivateMessage(name, "That is not properly "
			+ "formatted. Defaulting to 7 points.");
			}
		else
			{
			m_botAction.sendPrivateMessage(name, "Revenge kill reward set.");
			}
		}

	public void setAlreadyKilledReward(String name, String message)
		{
		alreadyKilledReward = explodeToInt(message, 1, " ");
		if (alreadyKilledReward == -1)
			{
			alreadyKilledReward = 2;
			m_botAction.sendPrivateMessage(name, "That is not properly "
			+ "formatted. Defaulting to 2 points.");
			}
		else
			{
			m_botAction.sendPrivateMessage(name, "Already-killed reward set.");
			}
		}

	public void setNormalPenalty(String name, String message)
		{
		normalPenalty = explodeToInt(message, 1, " ");
		if (normalPenalty == -1)
			{
			normalPenalty = 0;
			m_botAction.sendPrivateMessage(name, "That is not properly "
			+ "formatted. Defaulting to 0 points.");
			}
		else
			{
			m_botAction.sendPrivateMessage(name, "Normal penalty set.");
			}
		}

	public void setRevengePenalty(String name, String message)
		{
		revengePenalty = explodeToInt(message, 1, " ");
		if (revengePenalty == -1)
			{
			revengePenalty = 7;
			m_botAction.sendPrivateMessage(name, "That is not properly "
			+ " formatted. Defaulting to 7 points.");
			}
		else
			{
			m_botAction.sendPrivateMessage(name, "Revenge death penalty set.");
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
	
	public void showSettings(String name)
		{
		String[] settings =
			{
			"Time limit:             " + timeLimit + "minutes",
			"Normal kill reward:     " + normalReward + "pts",
			"Revenge kill reward:    " + revengeReward + "pts",
			"Already-killed reward:  " + alreadyKilledReward + "pts",
			"Normal penalty:         " + normalPenalty + "pts",
			"Revenge penalty:        " + revengePenalty + "pts",
			"Ship type limit:        " + SHIP_LIST[shipType]
			};
		m_botAction.privateMessageSpam(name, settings);
		}

	public void tellScore(String name)
		{
		if (playerMap.containsKey(name))
			{
			RevengePlayer tempPlayer = (RevengePlayer)playerMap.get(name);
			m_botAction.sendPrivateMessage(name, "You currently have "
			+ tempPlayer.getScore() + " points.");
			}
		else
			{
			m_botAction.sendPrivateMessage(name, "You are not in the game!");
			}
		}

	public String[] getHelpMessages()
		{
		String[] help =
			{
			"!nreward <#>                - Sets the point value of normal kills.",
			"!rreward <#>                - Sets the point value of 'revenge' kills.",
			"!areward <#>                - Sets the point value of 'rekills'.",
			"!npenalty <#>               - Sets the penalty value for normal deaths.",
			"!rpenalty <#>               - Sets the penalty value for 'revenge' deaths.",
			"!setshiptype <#>            - Allows players to use only this ship.",
			"!startrevenge <minutes>     - Starts a game of revenge.",
			"!stoprevenge                - Kills a game in progress.",
			"!settings                   - Shows current settings."
			};
		return help;
		}

	public void cancel() {}

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
			RevengePlayer p = (RevengePlayer)it.next();
			if (p.getScore() > bestScore)
				{
				bestScore = p.getScore();
				playerName = p.getName();
				}
			}
		return playerName;
		}
	}

class RevengePlayer
	{
	private String playerName;
	private int score = 0;
	private ArrayList killedList;

	public RevengePlayer(String name, int totalPlayers)
		{
		playerName = name;
		killedList = new ArrayList(totalPlayers - 1);
		}

	public void addPoints(int points)
		{
		score += points;
		}

	public void removePoints(int points)
		{
		score -= points;
		}

	public int getScore()
		{
		return score;
		}

	public void addKilled(String name)
		{
		killedList.add(name);
		}

	public boolean hasKilled(String name)
		{
		if (killedList.contains(name)) return true;
		return false;
		}

	public String getName()
		{
		return playerName;
		}
	}
