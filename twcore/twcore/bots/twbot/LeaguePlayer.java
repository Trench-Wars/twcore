package twcore.bots.twbot;

import twcore.core.*;

public class LeaguePlayer
{

	String m_playerName;
	int m_shipType;
	int m_frequency;
	int m_deathLimit;

	int m_deaths = 0;
	int m_kills = 0;
	int m_teamkills = 0;
	int m_terrierKills = 0;
	int m_score = 0;
	int m_lagouts = 0;

	boolean m_outOfGame = false;
	boolean m_laggedOut = false;
	boolean m_warned = false;
	boolean m_inBase = false;
	boolean m_saveState = false;
	int m_timeOfLagout = 0;
	int m_timer = 0;
	String m_sub = "  -";

	public LeaguePlayer(String name, int ship, int freq, int deaths)
	{
		m_playerName = name;
		m_shipType = ship;
		m_frequency = freq;
		m_deathLimit = deaths;
		m_timer = (int) (System.currentTimeMillis() / 1000);
	}

	public String getName()
	{
		return m_playerName;
	}

	public int getShip()
	{
		return m_shipType;
	}
	public void setShip(int s)
	{
		m_shipType = s;
	}

	public int getFreq()
	{
		return m_frequency;
	}
	public int getDeathLimit()
	{
		return m_deathLimit;
	}
	public void setDeathLimit(int d)
	{
		m_deathLimit = d;
	}

	public int getKills()
	{
		return m_kills;
	}
	public void addKill()
	{
		m_kills++;
	}
	public void setKills(int k)
	{
		m_kills = k;
	}

	public int getTeamKills()
	{
		return m_teamkills;
	}
	public void addTeamKill()
	{
		m_teamkills++;
	}
	public void setTeamKills(int k)
	{
		m_teamkills = k;
	}

	public int getTerrierKills()
	{
		return m_terrierKills;
	}
	public void addTerrierKill()
	{
		m_terrierKills++;
	}
	public void setTerrierKills(int k)
	{
		m_terrierKills = k;
	}

	public int getDeaths()
	{
		return m_deaths;
	}
	public void addDeath()
	{
		m_deaths++;
		m_timer = (int) (System.currentTimeMillis() / 1000);
		m_warned = false;
		m_inBase = false;
	}
	public void setDeaths(int d)
	{
		m_deaths = d;
	}

	public int getScore()
	{
		return m_score;
	}
	public void addToScore(int s)
	{
		m_score += s;
	}
	public void setScore(int s)
	{
		m_score = s;
	}

	public int getLagouts()
	{
		return m_lagouts;
	}
	public void addLagout()
	{
		m_lagouts++;
	}
	public void setLagouts(int l)
	{
		m_lagouts = l;
	}

	public void isOut()
	{
		m_outOfGame = true;
	}
	public boolean isOutOfGame()
	{
		return m_outOfGame;
	}

	public void laggedOut()
	{
		m_laggedOut = true;
	}

	public void notLaggedOut()
	{
		m_laggedOut = false;
		m_timer = (int) (System.currentTimeMillis() / 1000);
	}

	public boolean isLagged()
	{
		return m_laggedOut;
	}

	public int getTimeBetweenLagouts()
	{
		int time = (int) (System.currentTimeMillis() / 1000);
		time -= m_timeOfLagout;
		return time;
	}

	public void subbedBy(String name)
	{
		m_sub = name;
	}
	public String getSub()
	{
		return m_sub;
	}

	public void updateTimer()
	{
		m_timer = (int) (System.currentTimeMillis() / 1000);
		m_warned = false;
		m_inBase = false;
	}

	public boolean hasBeenInBase()
	{
		return m_inBase;
	}
	public void inBase()
	{
		m_inBase = true;
	}
	public void notInBase()
	{
		m_inBase = false;
	}

	public int timeOutOfBounds()
	{
		int time = (int) (System.currentTimeMillis() / 1000);
		time -= m_timer;
		return time;
	}

	public boolean warned()
	{
		return m_warned;
	}
	public void haveWarned()
	{
		m_warned = true;
		//timer = (int)(System.currentTimeMillis()/1000);
	}

	public boolean state()
	{
		return m_saveState;
	}
	public void toggleState()
	{
		m_saveState = true;
	}

}