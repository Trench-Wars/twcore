package twcore.bots.twbot;

import java.sql.*;
import java.util.*;
import twcore.core.*;
import twcore.misc.PlayerProfile;

public class TwlMatch
{

	int matchId;
	int matchTypeId;
	int team1Id;
	int team2Id;
	String team1Name;
	String team2Name;

	int teamLimit = 0;
	int overTime = 1;
	int deathLimit = -1;
	int team1Size = 0;
	int team2Size = 0;
	int team1Subs = 0;
	int team2Subs = 0;
	int team1Swaps = 0;
	int team2Swaps = 0;
	HashMap team1 = new HashMap();
	HashMap team2 = new HashMap();
	int team1Ships[] = { 0, 0, 0, 0, 0, 0, 0, 0 };
	int team2Ships[] = { 0, 0, 0, 0, 0, 0, 0, 0 };
	final int shipLimits[] = { 8, 8, 8, 0, 1, 8, 8, 2 };

	boolean zoned = false;
	boolean stateSaved = false;

	public TwlMatch(ResultSet result)
	{
		try
		{
			matchId = result.getInt("fnMatchID");
			matchTypeId = result.getInt("fnMatchTypeID");
			team1Id = result.getInt("fnTeam1ID");
			team2Id = result.getInt("fnTeam2ID");
			team1Name = result.getString("fcTeam1Name");
			team2Name = result.getString("fcTeam2Name");
			if (matchTypeId < 3)
				teamLimit = 5;
			else
				teamLimit = 8;
			if (matchTypeId < 3)
				deathLimit = 10;

		}
		catch (Exception e)
		{
		}
	}

	public int getMatchId()
	{
		return matchId;
	}

	public String getTeam1Name()
	{
		return team1Name;
	}
	public String getTeam2Name()
	{
		return team2Name;
	}

	public boolean state()
	{
		return stateSaved;
	}
	public void toggleState()
	{
		stateSaved = !stateSaved;
	}

	public String getOverTime()
	{
		if (overTime == 1)
			return "1st";
		else if (overTime == 2)
			return "2nd";
		else if (overTime == 3)
			return "3rd";
		else
			return overTime + "th";

	}
	public int incOverTime()
	{
		return overTime++;
	}

	public String getMatchType()
	{
		if (matchTypeId == 1)
			return "TWLD";
		else if (matchTypeId == 2)
			return "TWLJ";
		else if (matchTypeId == 3)
			return "TWLB";
		else
			return "Game";
	}

	public int getMatchTypeId()
	{
		return matchTypeId;
	}

	public boolean variableShips()
	{
		if (matchTypeId < 3)
			return false;
		else
			return true;
	}

	public int getMatchShipType()
	{
		return matchTypeId;
	}

	public void addSub(int id)
	{
		if (id == team1Id)
			team1Subs++;
		else if (id == team2Id)
			team2Subs++;
	}

	public boolean subLimitReached(int id)
	{
		int subLimit = 5;
		if (matchTypeId < 3)
			subLimit = 2;
		if (id == team1Id && team1Subs >= subLimit)
			return true;
		else if (id == team2Id && team2Subs >= subLimit)
			return true;
		else
			return false;
	}

	public boolean lagoutLimitReached(String player)
	{
		int lagouts = getPlayer(player).getData(1);
		if (matchTypeId < 3 && lagouts >= 3)
			return true;
		if (lagouts >= 5)
			return true;
		return false;
	}

	public int getTeam1Subs()
	{
		return team1Subs;
	}

	public void addSwap(int id)
	{
		if (id == team1Id)
			team1Swaps++;
		else if (id == team2Id)
			team2Swaps++;
	}

	public boolean swapLimitReached(int id)
	{
		int swapLimit = 3;
		if (matchTypeId < 3)
			swapLimit = 0;
		if (id == team1Id && team1Swaps >= swapLimit)
			return true;
		else if (id == team2Id && team2Swaps >= swapLimit)
			return true;
		else
			return false;
	}

	public int getTeam2Subs()
	{
		return team2Subs;
	}

	public boolean isTeamMember(int id)
	{
		if (id == team1Id || id == team2Id)
			return true;
		else
			return false;
	}

	public boolean isTeamOne(int id)
	{
		if (id == team1Id)
			return true;
		else
			return false;
	}

	public boolean isTeamTwo(int id)
	{
		if (id == team2Id)
			return true;
		else
			return false;
	}

	public boolean rosterLimit(int id)
	{
		if (team1Id == id)
			if (team1Size >= teamLimit)
				return true;
			else
				return false;
		else if (team2Id == id)
			if (team2Size >= teamLimit)
				return true;
			else
				return false;
		else
			return true;
	}

	public void addPlayer(int id, String player, int ship)
	{
		if (team1Id == id)
		{
			team1.put(player, new PlayerProfile(player, ship, 0));
			getPlayer(player).setData(0, deathLimit);
			team1Ships[ship - 1]++;
			team1Size++;
		}
		else if (team2Id == id)
		{
			team2.put(player, new PlayerProfile(player, ship, 1));
			getPlayer(player).setData(0, deathLimit);
			team2Ships[ship - 1]++;
			team2Size++;
		}
	}

	public void removePlayer(String player)
	{
		if (team1.containsKey(player))
		{
			team1Ships[getPlayer(player).getShip() - 1]--;
			team1.remove(player);
			team1Size--;
		}
		else if (team2.containsKey(player))
		{
			team2Ships[getPlayer(player).getShip() - 1]--;
			team2.remove(player);
			team1Size--;
		}
	}

	public PlayerProfile getPlayer(String player)
	{
		if (team1.containsKey(player))
			return (PlayerProfile) team1.get(player);
		else if (team2.containsKey(player))
			return (PlayerProfile) team2.get(player);
		else
			return new PlayerProfile();
	}

	public String getTeamName(int id)
	{
		if (team1Id == id)
			return team1Name;
		else if (team2Id == id)
			return team2Name;
		else
			return "";
	}

	public String getTeamName(String player)
	{
		if (team1.containsKey(player))
			return team1Name;
		else if (team2.containsKey(player))
			return team2Name;
		else
			return "";
	}

	public int getTeamId(String player)
	{
		if (team1.containsKey(player))
			return team1Id;
		else if (team2.containsKey(player))
			return team2Id;
		else
			return -1;
	}

	public int getTeam(String player)
	{
		if (team1.containsKey(player))
			return 0;
		else if (team2.containsKey(player))
			return 1;
		else
			return -1;
	}

	public int getTeam1Count()
	{
		return team1Size;
	}

	public int getTeam2Count()
	{
		return team2Size;
	}

	public String getTeam1Players()
	{
		String output = team1Name + "  ";
		Set set = team1.keySet();
		Iterator it = set.iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			output += Tools.formatString(" (" + getPlayer(player).getShip() + ") " + player, 17);
		}
		return output;
	}

	public String getTeam2Players()
	{
		String output = team2Name + "  ";
		Set set = team2.keySet();
		Iterator it = set.iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			output += Tools.formatString(" (" + getPlayer(player).getShip() + ") " + player, 17);
		}
		return output;
	}

	public boolean alreadyHasPlayer(String player)
	{
		if (team1.containsKey(player) || team2.containsKey(player))
			return true;
		else
			return false;
	}

	public void zoned()
	{
		zoned = true;
	}
	public boolean haveZoned()
	{
		return zoned;
	}

	public int getTeam1Score()
	{
		int score = 0;
		Set set = team1.keySet();
		Iterator it = set.iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			score += getPlayer(player).getData(4);
		}
		return score;
	}

	public int getTeam2Score()
	{
		int score = 0;
		Set set = team2.keySet();
		Iterator it = set.iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			score += getPlayer(player).getData(4);
		}
		return score;
	}

	public int getTeam1Deaths()
	{
		int score = 0;
		Set set = team1.keySet();
		Iterator it = set.iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			score += getPlayer(player).getDeaths();
		}
		score += (5 - team1Size) * 10;
		return score;
	}

	public int getTeam2Deaths()
	{
		int score = 0;
		Set set = team2.keySet();
		Iterator it = set.iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			score += getPlayer(player).getDeaths();
		}
		score += (5 - team2Size) * 10;
		return score;
	}

	public int getTeamScore(int id)
	{
		int score = 0;
		Set set = null;
		if (id == team1Id)
			set = team1.keySet();
		else if (id == team2Id)
			set = team2.keySet();
		else
			return 0;
		Iterator it = set.iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			score += getPlayer(player).getData(4);
		}
		return score;
	}

	public int getTeamShipCount(int id, int ship)
	{
		if (id == team1Id)
			return team1Ships[ship - 1];
		else if (id == team2Id)
			return team2Ships[ship - 1];
		else
			return 999;
	}

	public int getShipLimit(int ship)
	{
		return shipLimits[ship - 1];
	}

	public void addFlagReward(BotAction m_botAction, int freq, int pts)
	{
		Set set = team1.keySet();
		Iterator it = set.iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			if (m_botAction.getFuzzyPlayerName(player) != null)
				if (getPlayer(player).getFreq() == freq && m_botAction.getPlayer(player).getShipType() != 0)
					getPlayer(player).setData(4, getPlayer(player).getData(4) + pts);
		}
		set = team2.keySet();
		it = set.iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			if (m_botAction.getFuzzyPlayerName(player) != null)
				if (getPlayer(player).getFreq() == freq && m_botAction.getPlayer(player).getShipType() != 0)
					getPlayer(player).setData(4, getPlayer(player).getData(4) + pts);
		}
	}

	public boolean gameOver(BotAction m_botAction)
	{
		boolean over1 = true, over2 = true;
		Set set = team1.keySet();
		Iterator it = set.iterator();
		while (it.hasNext())
		{
			boolean playerOut = false;
			String player = (String) it.next();
			if (getPlayer(player).getData(9) == -1)
				playerOut = true;
			if (getPlayer(player).getData(1) >= 3)
				playerOut = true; //Checks for 3 lagouts.
			int time = (int) (System.currentTimeMillis() / 1000);
			if (getPlayer(player).getData(-9) == -2)
				playerOut = true; //Checks for lagged out too long.
			if (!playerOut)
				over1 = false;
		}
		set = team2.keySet();
		it = set.iterator();
		while (it.hasNext())
		{
			boolean playerOut = false;
			String player = (String) it.next();
			if (getPlayer(player).getData(9) == -1)
				playerOut = true;
			if (getPlayer(player).getData(1) >= 3)
				playerOut = true; //Checks for 3 lagouts.
			int time = (int) (System.currentTimeMillis() / 1000);
			if (getPlayer(player).getData(-9) == -2)
				playerOut = true; //Checks for lagged out too long.
			if (!playerOut)
				over2 = false;
		}
		if (over1 || over2)
			return true;
		else
			return false;
	}

	public Iterator getTeam1List()
	{
		Set set = team1.keySet();
		return set.iterator();
	}

	public Iterator getTeam2List()
	{
		Set set = team2.keySet();
		return set.iterator();
	}

	public String getMVP()
	{
		int kills = 0;
		int rec = -20;
		String mvp = "";
		Set set = team1.keySet();
		Iterator it = set.iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			int pRec = getPlayer(player).getKills() - getPlayer(player).getDeaths();
			if (rec < pRec)
			{
				rec = pRec;
				kills = getPlayer(player).getKills();
				mvp = player;
			}
			else if (rec == pRec)
			{
				if (kills < getPlayer(player).getKills())
				{
					kills = getPlayer(player).getKills();
					mvp = player;
				}
				else if (kills == getPlayer(player).getKills())
				{
					mvp += " and " + player;
				}
			}
		}
		set = team2.keySet();
		it = set.iterator();
		while (it.hasNext())
		{
			String player = (String) it.next();
			int pRec = getPlayer(player).getKills() - getPlayer(player).getDeaths();
			if (rec < pRec)
			{
				rec = pRec;
				kills = getPlayer(player).getKills();
				mvp = player;
			}
			else if (rec == pRec)
			{
				if (kills < getPlayer(player).getKills())
				{
					kills = getPlayer(player).getKills();
					mvp = player;
				}
				else if (kills == getPlayer(player).getKills())
				{
					mvp += " and " + player;
				}
			}
		}
		return mvp;
	}

}