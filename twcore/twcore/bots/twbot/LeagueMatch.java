package twcore.bots.twbot;

import java.sql.*;
import java.util.*;
import twcore.core.*;
import twcore.misc.statistics.*;

public class LeagueMatch
{
	//match stuff
	private int m_matchId;
	private int m_matchTypeId;

	//team stuff
	private int m_team1Id;
	private int m_team2Id;
	private String m_team1Name;
	private String m_team2Name;
	private int m_team1Size = 0;
	private int m_team2Size = 0;
	private HashMap m_team1List;
	private HashMap m_team2List;
	private int m_team1Subs = 0;
	private int m_team2Subs = 0;
	private int m_team1Switches = 0;
	private int m_team2Switches = 0;
	private int m_team1Score = 0;
	private int m_team2Score = 0;
	private int m_team1Ships[] = { 0, 0, 0, 0, 0, 0, 0, 0 };
	private int m_team2Ships[] = { 0, 0, 0, 0, 0, 0, 0, 0 };
	private final int m_shipLimits[] = { 8, 8, 8, 0, 1, 8, 8, 2 };
	private boolean m_team1Flag = false;
	private boolean m_team2Flag = false;

	//ref stuff
	private String m_ref = "";
	private boolean m_zoned = false;
	private boolean m_blueOut = false;
	private boolean m_saveState = false;
	private int m_overTime = 1;
	private String m_restartTime = "00:00";
	private BotAction m_botAction;
	private Vector m_playerList;
	private HashMap m_justSaw;

	//limits	
	private int m_rosterLimit;
	private int m_deathLimit = 0;
	private int m_subLimit = 0;
	private int m_switchLimit = 3;
	private int m_lagoutLimit;

	//constants
	private final int TEAM_ONE_FREQ = 0;
	private final int TEAM_TWO_FREQ = 1;
	
	public LeagueMatch(ResultSet result, BotAction botAction)
	{

		try
		{
			m_matchId = result.getInt("fnMatchID");
			m_matchTypeId = result.getInt("fnMatchTypeID");
			m_team1Id = result.getInt("fnTeam1ID");
			m_team2Id = result.getInt("fnTeam2ID");
			m_team1Name = result.getString("fcTeam1Name");
			m_team2Name = result.getString("fcTeam2Name");
			if (m_matchTypeId < 3)
			{
				m_rosterLimit = 5;
				m_deathLimit = 10;
				m_subLimit = 2;
				m_lagoutLimit = 3;
			}
			else
			{
				m_rosterLimit = 8;
				m_deathLimit = 0;
				m_subLimit = 3;
				m_lagoutLimit = 5;
			}
		}
		catch (Exception e)
		{
			Tools.printStackTrace(e);
			//m_botAction.sendPrivateMessage
		}
		m_team1List = new HashMap();
		m_team2List = new HashMap();
		m_justSaw = new HashMap();
		m_playerList = new Vector();
		m_botAction = botAction;
	}

	public void loadMatchState(ResultSet result)
	{

		try
		{
			m_team1Subs = result.getInt("fnTeam1Subs");
			m_team2Subs = result.getInt("fnTeam2Subs");
			m_team1Switches = result.getInt("fnTeam1Switches");
			m_team2Switches = result.getInt("fnTeam2Switches");
			m_restartTime = result.getString("fcTimeLeft");
		}
		catch (Exception e)
		{
		}
	}

	public boolean reloadPlayer(ResultSet result)
	{
		try
		{
			boolean gone = false;
			String name = result.getString("fcPlayerName");
			if (m_botAction.getFuzzyPlayerName(name) == null)
				gone = true;
			int ship = result.getInt("fnShipType");
			int freq = result.getInt("fnFreq");
			int teamId = result.getInt("fnTeamId");
			addPlayer(teamId, name, ship);
			getPlayer(name).changeStatistic(Statistics.TOTAL_KILLS, result.getInt("fnKills"));
			getPlayer(name).changeStatistic(Statistics.DEATHS, result.getInt("fnDeaths"));
			getPlayer(name).setDeathLimit(result.getInt("fnLives"));
			getPlayer(name).subbedBy(result.getString("fcSubbedBy"));
			getPlayer(name).changeStatistic(Statistics.TOTAL_TEAMKILLS, result.getInt("fnTeamKills"));
			getPlayer(name).changeStatistic(Statistics.TERRIER_KILL, result.getInt("fnTerrierKills"));
			getPlayer(name).setLagouts(result.getInt("fnLagouts"));
			getPlayer(name).changeStatistic(Statistics.SCORE, result.getInt("fnScore"));
			if (result.getInt("fnState") == 1)
			{
				getPlayer(name).isOut();
				if (m_team1List.containsKey(name))
					m_team1Size--;
				else if (m_team2List.containsKey(name))
					m_team2Size--;
				return true;
			}
			if (gone)
			{
				return false;
			}
			m_botAction.setShip(name, ship);
			m_botAction.setFreq(name, freq);
			m_botAction.sendArenaMessage(name + " in for " + getTeamName(name));
			return true;
		}
		catch (Exception e)
		{
			return true;
		}
	}

	public String getRestartTime()
	{
		return m_restartTime;
	}

	/*** MATCH TYPE STUFF ***/
	public int getMatchId()
	{
		return m_matchId;
	}

	public void toggleBlueOut()
	{
		m_blueOut = !m_blueOut;
	}
	public boolean blueOut()
	{
		return m_blueOut;
	}

	public boolean variableShips()
	{

		if (m_matchTypeId < 3)
			return false;
		else
			return true;
	}

	public String getMatchType()
	{

		if (m_matchTypeId == 1)
			return "TWLD";
		else if (m_matchTypeId == 2)
			return "TWLJ";
		else if (m_matchTypeId == 3)
			return "TWLB";
		else
			return "Game";
	}

	public int getMatchTypeId()
	{
		return m_matchTypeId;
	}

	public boolean hasPlayer(String name)
	{
		if (m_team1List.containsKey(name) || m_team2List.containsKey(name))
			return true;
		else
			return false;
	}

	public String getOverTime()
	{
		if (m_overTime == 1)
			return "1st";
		else if (m_overTime == 2)
			return "2nd";
		else if (m_overTime == 3)
			return "3rd";
		else
			return m_overTime + "th";

	}

	public int incOverTime()
	{
		return m_overTime++;
	}

	/*** TEAM TYPE STUFF ***/

	public String getTeam1Name()
	{
		return m_team1Name;
	}
	public String getTeam2Name()
	{
		return m_team2Name;
	}

	public String getTeamName(String player)
	{
		if (m_team1List.containsKey(player))
			return m_team1Name;
		else if (m_team2List.containsKey(player))
			return m_team2Name;
		else
			return "Unknown team";
	}

	public boolean isTeamMember(int id)
	{
		if (id == m_team1Id || id == m_team2Id)
			return true;
		else
			return false;
	}

	public boolean rosterLimitMet(int id)
	{
		if (m_team1Id == id)
			if (m_team1Size >= m_rosterLimit)
				return true;
			else
				return false;
		else if (m_team2Id == id)
			if (m_team2Size >= m_rosterLimit)
				return true;
			else
				return false;
		else
			return true;
	}

	public boolean shipLimitMet(int shipType, int teamId)
	{
		if (teamId == m_team1Id)
		{
			if (m_team1Ships[shipType - 1] >= m_shipLimits[shipType - 1])
				return true;
			else
				return false;
		}
		else if (teamId == m_team2Id)
		{
			if (m_team2Ships[shipType - 1] >= m_shipLimits[shipType - 1])
				return true;
			else
				return false;
		}
		else
			return true;
	}

	public Iterator getTeam1List()
	{
		return m_team1List.keySet().iterator();
	}
	public Iterator getTeam2List()
	{
		return m_team2List.keySet().iterator();
	}

	public boolean subLimitReached(int teamId)
	{
		if (teamId == m_team1Id && m_team1Subs >= m_subLimit)
			return true;
		else if (teamId == m_team2Id && m_team2Subs >= m_subLimit)
			return true;
		else
			return false;
	}

	public boolean switchLimitReached(int teamId)
	{
		if (teamId == m_team1Id && m_team1Switches >= m_switchLimit)
			return true;
		else if (teamId == m_team2Id && m_team2Switches >= m_switchLimit)
			return true;
		else
			return false;
	}

	/**
	 * @author FoN
	 * 
	 * This adds a sec point to team with flag
	 * Also gives out time warnings.
	 */
	public void addTimePoint()
	{
		if (m_team1Flag)
		{
			m_team1Score++;
			giveTimeWarning(3, m_team1Id);
			giveTimeWarning(1, m_team1Id);
		}
		else if (m_team2Flag)
		{
			m_team2Score++;
			giveTimeWarning(3, m_team2Id);
			giveTimeWarning(1, m_team2Id);
		}
	}

	/**
	 * Displays warning depending on time remaining
	 */
	private void giveTimeWarning(int time, int team)
	{

		if (time <= twbottwl.TIME_RACE_TARGET)
		{

			int teamId = 0;
			boolean warning = true;
			String name = "";
			int teamScore = 0;

			if (team == m_team1Id)
			{
				teamId = m_team1Id;
				name = getTeam1Name();
				teamScore = getTeam1Score();
			}
			else
			{
				teamId = m_team2Id;
				name = getTeam2Name();
				teamScore = getTeam2Score();
			}

			if (twbottwl.TIME_RACE_TARGET - teamScore == time * 60) //3 mins * 60 secs
			{
				if (teamId == getFlagOwner()) //no multiple warning
					m_botAction.sendArenaMessage(name + " needs " + time + " min of flag time to win");
			}
		}
	}

	/**
	 * @author FoN
	 * 
	 * Sets the flag owner depending on the freq and disowns the other freq
	 * @param freq The freq of the player who claimed the flag
	 */
	public void setFlagOwner(int freq)
	{
		if (freq == TEAM_ONE_FREQ)
		{
			m_team1Flag = true;
			m_team2Flag = false;
		}
		else if (freq == TEAM_TWO_FREQ)
		{
			m_team2Flag = true;
			m_team1Flag = false;
		}
	}
	
	/**
	 * @author FoN
	 * 
	 * @return the Id of the team (freq) who owns the flag, -1 if no one owns flag.
	 */
	public int getFlagOwner()
	{
		if(m_team1Flag)
			return m_team1Id;
		else if (m_team2Flag)
			return m_team2Id; 
		else
			return -1;
	}

	public int getTeam1Score()
	{
		return m_team1Score;
	}

	public int getTeam2Score()
	{
		return m_team2Score;
	}

	public int getTeam1PlayerCount()
	{
		return m_team1Size;
	}
	public int getTeam2PlayerCount()
	{
		return m_team2Size;
	}

	public int getTeam1Deaths()
	{
		int score = 0;
		Iterator it = m_team1List.keySet().iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			score += getPlayer(player).getStatistic(Statistics.DEATHS);
		}
		score += (5 - m_team1Size) * 10;
		return score;
	}

	public int getTeam2Deaths()
	{
		int score = 0;
		Iterator it = m_team2List.keySet().iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			score += getPlayer(player).getStatistic(Statistics.DEATHS);
		}
		score += (5 - m_team2Size) * 10;
		return score;
	}

	public boolean gameOver()
	{
		boolean over1 = true, over2 = true;
		Iterator it = getTeam1List();
		while (it.hasNext())
		{
			boolean playerOut = false;
			String player = (String) it.next();
			if (getPlayer(player).isOutOfGame())
				playerOut = true;
			if (!playerOut)
				over1 = false;
		}
		it = getTeam2List();
		while (it.hasNext())
		{
			boolean playerOut = false;
			String player = (String) it.next();
			if (getPlayer(player).isOutOfGame())
				playerOut = true;
			if (!playerOut)
				over2 = false;
		}
		if (over1 || over2)
			return true;
		else
			return false;
	}

	/*** PLAYER TYPE STUFF ***/

	public String getClosestMatch(String name)
	{
		Iterator it = getTeam1List();
		while (it.hasNext())
		{
			String player = (String) it.next();
			if (player.toLowerCase().startsWith(name))
				return player;
		}
		it = getTeam2List();
		while (it.hasNext())
		{
			String player = (String) it.next();
			if (player.toLowerCase().startsWith(name))
				return player;
		}
		return name;
	}

	public void addPlayer(int teamId, String name, int shipType)
	{
		if (m_team1Id == teamId)
		{
			m_team1List.put(name, new LeaguePlayer(name, shipType, TEAM_ONE_FREQ, m_deathLimit));
			m_team1Ships[shipType - 1]++;
			m_team1Size++;
		}
		else if (m_team2Id == teamId)
		{
			m_team2List.put(name, new LeaguePlayer(name, shipType, TEAM_TWO_FREQ, m_deathLimit));
			m_team2Ships[shipType - 1]++;
			m_team2Size++;
		}
		m_playerList.addElement(name);
	}

	public void removePlayer(String name)
	{
		if (m_team1List.containsKey(name))
		{
			m_team1Ships[getPlayer(name).getShip() - 1]--;
			m_team1List.remove(name);
			m_team1Size--;
		}
		else if (m_team2List.containsKey(name))
		{
			m_team2Ships[getPlayer(name).getShip() - 1]--;
			m_team2List.remove(name);
			m_team2Size--;
		}
		for (int i = 0; i < m_playerList.size(); i++)
			if (((String) m_playerList.elementAt(i)).equals(name))
				m_playerList.removeElementAt(i);
	}

	public int subPlayer(String playerOut, String playerIn)
	{
		int shipType = getPlayer(playerOut).getShip();
		int deathLimit = 0;
		if (getMatchTypeId() < 3)
			deathLimit = getPlayer(playerOut).getDeathLimit() - getPlayer(playerOut).getStatistic(Statistics.DEATHS);
			
		//remove old player
		getPlayer(playerOut).isOut();
		m_botAction.spec(playerOut);
		m_botAction.spec(playerOut);
		m_botAction.setFreq(playerOut, getPlayer(playerOut).getFreq());
		
		//add new player
		if (getTeamId(playerOut) == m_team1Id)
		{
			m_team1List.put(playerIn, new LeaguePlayer(playerIn, shipType, 0, deathLimit));
			m_team1Subs++;
		}
		else
		{
			m_team2List.put(playerIn, new LeaguePlayer(playerIn, shipType, 1, deathLimit));
			m_team2Subs++;
		}

		m_botAction.setShip(playerIn, getPlayer(playerIn).getShip());
		m_botAction.setFreq(playerIn, getPlayer(playerIn).getFreq());
		m_botAction.scoreReset(playerIn);
		getPlayer(playerOut).subbedBy(playerIn);

		//Adds/removes players to 'watch' queue
		for (int i = 0; i < m_playerList.size(); i++)
			if (((String) m_playerList.elementAt(i)).equals(playerOut))
				m_playerList.removeElementAt(i);
		m_playerList.addElement(playerIn);

		return deathLimit;
	}

	public int getTeamSubs(int team)
	{
		if (team == 1)
			return m_team1Subs;
		else if (team == 2)
			return m_team2Subs;
		else
			return 0;
	}

	public int getTeamSwitches(int team)
	{
		if (team == 1)
			return m_team1Switches;
		else if (team == 2)
			return m_team2Switches;
		else
			return 0;
	}

	public void switchPlayers(String player1, String player2)
	{
		int ship1 = getPlayer(player1).getShip();
		int ship2 = getPlayer(player2).getShip();
		getPlayer(player1).setShip(ship2);
		getPlayer(player2).setShip(ship1);
		m_botAction.setShip(player1, ship2);
		m_botAction.setShip(player2, ship1);

		if (getTeamId(player1) == m_team1Id)
			m_team1Switches++;
		else
			m_team2Switches++;
	}

	public LeaguePlayer getPlayer(String name)
	{
		if (m_team1List.containsKey(name))
			return ((LeaguePlayer) m_team1List.get(name));
		else if (m_team2List.containsKey(name))
			return ((LeaguePlayer) m_team2List.get(name));
		else
			return null;
	}

	public void placePlayersInGame()
	{
		boolean first = true;
		String team1 = "In for " + getTeam1Name() + ": ";
		String team2 = "In for " + getTeam2Name() + ": ";
		Iterator i = getTeam1List();
		while (i.hasNext())
		{
			String player = (String) i.next();
			m_botAction.setShip(player, getPlayer(player).getShip());
			m_botAction.setFreq(player, getPlayer(player).getFreq());
			if (first)
			{
				team1 += player;
				first = false;
			}
			else
				team1 += ", " + player;
		}
		first = true;
		i = getTeam2List();
		while (i.hasNext())
		{
			String player = (String) i.next();
			m_botAction.setShip(player, getPlayer(player).getShip());
			m_botAction.setFreq(player, getPlayer(player).getFreq());
			if (first)
			{
				team2 += player;
				first = false;
			}
			else
				team2 += ", " + player;
		}
		m_botAction.sendArenaMessage(team1);
		m_botAction.sendArenaMessage(team2);
	}

	public void warpAllPlayers()
	{
		int t = getMatchTypeId();
		if (t == 1)
		{
			m_botAction.warpFreqToLocation(TEAM_ONE_FREQ, 312, 362);
			m_botAction.warpFreqToLocation(TEAM_TWO_FREQ, 713, 662);
		}
		else if (t == 2)
		{
			m_botAction.warpFreqToLocation(TEAM_ONE_FREQ, 446, 453);
			m_botAction.warpFreqToLocation(TEAM_TWO_FREQ, 576, 453);
		}
		else if (t == 3)
		{
			m_botAction.warpFreqToLocation(TEAM_ONE_FREQ, 310, 482);
			m_botAction.warpFreqToLocation(TEAM_TWO_FREQ, 714, 482);
		}
	}

	public void warpPlayer(String name, int x, int y)
	{
		if (playerInBounds(name, x, y))
			return;
		try
		{
			int t = getMatchTypeId();
			int f = getPlayer(name).getFreq();
			if (t == 1)
			{
				if (f == TEAM_ONE_FREQ)
					m_botAction.warpTo(name, 312, 362);
				else if (f == TEAM_TWO_FREQ)
					m_botAction.warpTo(name, 713, 662);
			}
			else if (t == 2)
			{
				if (f == TEAM_ONE_FREQ)
					m_botAction.warpTo(name, 446, 453);
				else if (f == TEAM_TWO_FREQ)
					m_botAction.warpTo(name, 576, 453);
			}
			else if (t == 3)
			{
				if (f == TEAM_ONE_FREQ)
					m_botAction.warpTo(name, 310, 482);
				else if (f == TEAM_TWO_FREQ)
					m_botAction.warpTo(name, 714, 482);
			}
		}
		catch (Exception e)
		{
		}
	}

	public boolean playerInBounds(String name, int x, int y)
	{
		try
		{
			int t = getMatchTypeId();
			int f = getPlayer(name).getFreq();
			int x2 = 0, y2 = 0;
			if (t == 1)
			{
				if (f == TEAM_ONE_FREQ)
				{
					x2 = 312 * 16;
					y2 = 362 * 16;
				}
				else if (f == TEAM_TWO_FREQ)
				{
					x2 = 713 * 16;
					y2 = 662 * 16;
				}
			}
			else if (t == 2)
			{
				if (f == TEAM_ONE_FREQ)
				{
					x2 = 446 * 16;
					y2 = 453 * 16;
				}
				else if (f == TEAM_TWO_FREQ)
				{
					x2 = 576 * 16;
					y2 = 453 * 16;
				}
			}
			else if (t == 3)
			{
				if (f == TEAM_ONE_FREQ)
				{
					x2 = 310 * 16;
					y2 = 482 * 16;
				}
				else if (f == TEAM_TWO_FREQ)
				{
					x2 = 714 * 16;
					y2 = 482 * 16;
				}
			}
			double dist = Math.sqrt(Math.pow((x - x2), 2) + Math.pow((y - y2), 2));
			if (dist < 160)
				return true;
			else
				return false;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public int getTeamId(String name)
	{
		if (m_team1List.containsKey(name))
			return m_team1Id;
		else if (m_team2List.containsKey(name))
			return m_team2Id;
		else
			return -1;
	}

	public String getScoreMVP(int team)
	{
		String mvp = "";
		int score = 0;
		Iterator it;
		if (team == 1)
			it = m_team1List.keySet().iterator();
		else
			it = m_team2List.keySet().iterator();
		while (it.hasNext())
		{
			String name = (String) it.next();
			if (score < getPlayer(name).getStatistic(Statistics.RATING))
			{
				mvp = name;
				score = getPlayer(name).getStatistic(Statistics.RATING);
			}
		}
		return mvp;
	}

	public String getMVP(int team)
	{
		int kills = 0;
		int rec = -20;
		String mvp = "";
		Iterator it;
		if (team == 1)
			it = m_team1List.keySet().iterator();
		else
			it = m_team2List.keySet().iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			int pRec = getPlayer(player).getStatistic(Statistics.TOTAL_KILLS) - getPlayer(player).getStatistic(Statistics.TOTAL_KILLS) - getPlayer(player).getStatistic(Statistics.TOTAL_TEAMKILLS);
			if (rec < pRec)
			{
				rec = pRec;
				kills = getPlayer(player).getStatistic(Statistics.TOTAL_KILLS);
				mvp = player;
			}
			else if (rec == pRec)
			{
				if (kills < getPlayer(player).getStatistic(Statistics.TOTAL_KILLS))
				{
					kills = getPlayer(player).getStatistic(Statistics.TOTAL_KILLS);
					mvp = player;
				}
				else if (kills == getPlayer(player).getStatistic(Statistics.TOTAL_KILLS))
				{
					mvp += " and " + player;
				}
			}
		}
		return mvp;
	}

	public void addFlagReward(int freq, int pts)
	{
		Iterator it;
		if (freq == TEAM_ONE_FREQ)
			it = m_team1List.keySet().iterator();
		else
			it = m_team2List.keySet().iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			if (m_botAction.getFuzzyPlayerName(player) != null)
				if (m_botAction.getPlayer(player).getShipType() != 0)
					getPlayer(player).reportStatistic(Statistics.SCORE, pts);
		}
	}

	public String getTeam1Players()
	{
		String output = m_team1Name + "  ";
		Iterator it = getTeam1List();
		while (it.hasNext())
		{
			String player = (String) it.next();
			output += Tools.formatString(" (" + getPlayer(player).getShip() + ") " + player, 17);
		}
		return output;
	}

	public String getTeam2Players()
	{
		String output = m_team2Name + "  ";
		Iterator it = getTeam2List();
		while (it.hasNext())
		{
			String player = (String) it.next();
			output += Tools.formatString(" (" + getPlayer(player).getShip() + ") " + player, 17);
		}
		return output;
	}

	public boolean isTeamOne(int id)
	{
		if (id == m_team1Id)
			return true;
		else
			return false;
	}

	public boolean isTeamTwo(int id)
	{
		if (id == m_team2Id)
			return true;
		else
			return false;
	}

	public boolean lagoutLimitReached(String player)
	{
		int lagouts = getPlayer(player).getLagouts();
		if (lagouts >= m_lagoutLimit)
			return true;
		else
			return false;
	}

	/*** ZONE TYPE STUFF ***/

	public void zoned()
	{
		m_zoned = true;
	};
	public boolean haveZoned()
	{
		return m_zoned;
	}

	/*** REF TYPE STUFF ***/

	public void setRef(String ref)
	{
		m_ref = ref;
	}
	public String getRef()
	{
		return m_ref;
	}

	/*** SCORING ***/

	public void displayScores(int team1Score, int team2Score, String mvp, boolean endGame)
	{
		m_botAction.sendArenaMessage(Tools.formatString("", 67, "-"));
		m_botAction.sendArenaMessage(Tools.formatString("_  " + getTeam1Name(), 29) + "Kills   Deaths   TKs   LOs   SubbedBy");
		m_botAction.sendArenaMessage(Tools.formatString("", 67, "-"));
		Iterator i = getTeam1List();
		while (i.hasNext())
		{
			String player = (String) i.next();
			String out = Tools.formatString(player, 29);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.TOTAL_KILLS), 8);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.DEATHS), 9);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.TOTAL_TEAMKILLS), 6);
			out += Tools.formatString("" + getPlayer(player).getLagouts(), 6);
			out += getPlayer(player).getSub();
			m_botAction.sendArenaMessage(out);
			try
			{
				Thread.sleep(75);
			}
			catch (Exception e)
			{
			}
		}
		m_botAction.sendArenaMessage("_");
		m_botAction.sendArenaMessage(Tools.formatString("", 67, "-"));
		m_botAction.sendArenaMessage(Tools.formatString("_  " + getTeam2Name(), 29) + "Kills   Deaths   TKs   LOs   SubbedBy");
		m_botAction.sendArenaMessage(Tools.formatString("", 67, "-"));
		i = getTeam2List();
		while (i.hasNext())
		{
			String player = (String) i.next();
			String out = Tools.formatString(player, 29);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.TOTAL_KILLS), 8);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.DEATHS), 9);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.TOTAL_TEAMKILLS), 6);
			out += Tools.formatString("" + getPlayer(player).getLagouts(), 6);
			out += getPlayer(player).getSub();
			m_botAction.sendArenaMessage(out);
			try
			{
				Thread.sleep(75);
			}
			catch (Exception e)
			{
			}
		}
		if (!endGame)
			return;
		m_botAction.showObject(50);
		m_botAction.sendArenaMessage("_", 5);
		if (team1Score > team2Score)
			m_botAction.sendArenaMessage(getTeam1Name() + " defeats " + getTeam2Name() + " in this match  (" + team1Score + " - " + team2Score + ")");
		else
			m_botAction.sendArenaMessage(getTeam2Name() + " defeats " + getTeam1Name() + " in this match  (" + team2Score + " - " + team1Score + ")");
		m_botAction.sendArenaMessage("MVP: " + mvp);
	}

	public void displayBaseScores(int team1Score, int team2Score, String mvp, boolean endGame)
	{
		m_botAction.sendArenaMessage(Tools.formatString("", 79, "-"));
		m_botAction.sendArenaMessage(Tools.formatString("_  " + getTeam1Name(), 29) + "Kills   Deaths   TKs   TeK   LOs   Score   SubbedBy");
		m_botAction.sendArenaMessage(Tools.formatString("", 79, "-"));
		Iterator i = getTeam1List();
		while (i.hasNext())
		{
			String player = (String) i.next();
			String out = "_" + Tools.formatString(player, 28);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.TOTAL_KILLS), 8);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.DEATHS), 9);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.TOTAL_TEAMKILLS), 6);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.TERRIER_KILL), 6);
			out += Tools.formatString("" + getPlayer(player).getLagouts(), 6);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.SCORE), 8);
			out += getPlayer(player).getSub();
			m_botAction.sendArenaMessage(out);
			//try {
			//	Thread.sleep( 75 );
			//} catch (Exception e){}
		}
		m_botAction.sendArenaMessage("_");
		m_botAction.sendArenaMessage(Tools.formatString("", 79, "-"));
		m_botAction.sendArenaMessage(Tools.formatString("_  " + getTeam2Name(), 29) + "Kills   Deaths   TKs   TeK   LOs   Score   SubbedBy");
		m_botAction.sendArenaMessage(Tools.formatString("", 79, "-"));
		i = getTeam2List();
		while (i.hasNext())
		{
			String player = (String) i.next();
			String out = "_" + Tools.formatString(player, 28);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.TOTAL_KILLS), 8);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.DEATHS), 9);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.TOTAL_TEAMKILLS), 6);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.TERRIER_KILL), 6);
			out += Tools.formatString("" + getPlayer(player).getLagouts(), 6);
			out += Tools.formatString("" + getPlayer(player).getStatistic(Statistics.SCORE), 8);
			out += getPlayer(player).getSub();
			m_botAction.sendArenaMessage(out);
			//try {
			//	Thread.sleep( 75 );
			//} catch (Exception e){}
		}
		if (!endGame)
			return;
		m_botAction.showObject(50);
		m_botAction.sendArenaMessage("_", 5);
		if (team1Score > team2Score)
			m_botAction.sendArenaMessage(getTeam1Name() + " defeats " + getTeam2Name() + " in this match  (" + team1Score + " - " + team2Score + ")");
		else
			m_botAction.sendArenaMessage(getTeam2Name() + " defeats " + getTeam1Name() + " in this match  (" + team2Score + " - " + team1Score + ")");
		m_botAction.sendArenaMessage("MVP: " + mvp);
	}

	///PLAYER QUEUE FOR WATCHING///
	public String getNextPlayer()
	{
		String name = "";
		try
		{
			name = (String) m_playerList.elementAt(0);
			m_playerList.removeElementAt(0);
			m_playerList.addElement(name);
		}
		catch (Exception e)
		{
		}
		if (m_justSaw.containsKey(name))
		{
			int time = Integer.parseInt((String) m_justSaw.get(name));
			m_justSaw.remove(name);
			if ((int) (System.currentTimeMillis() / 1000) - time > 5)
				return name;
			else
				return getNextPlayer();
		}
		else
			return name;
	}

	public void removeFromWatch(String name)
	{
		for (int i = 0; i < m_playerList.size(); i++)
			if (((String) m_playerList.elementAt(i)).equals(name))
				m_playerList.removeElementAt(i);
	}

	public boolean handlePlayerOutOfBounds(String name)
	{
		LeaguePlayer lp = getPlayer(name);
		if (lp.hasBeenInBase())
			return true;
		else if (lp.timeOutOfBounds() > 30)
			return true;
		else if (lp.timeOutOfBounds() > 15 && !lp.warned())
		{
			m_botAction.sendPrivateMessage(name, "You have 15 seconds to return to base.");
			lp.haveWarned();
			return false;
		}
		else
			return false;
	}

	public void justSaw(String name)
	{
		if (!m_justSaw.containsKey(name))
			m_justSaw.put(name, "" + (int) (System.currentTimeMillis() / 1000));
	}

	///STORE GAME STATE///
	public boolean state()
	{
		return m_saveState;
	}
	public void toggleState()
	{
		m_saveState = true;
	}
}