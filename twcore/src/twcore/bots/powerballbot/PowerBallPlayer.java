package twcore.bots.powerballbot;

import java.util.*;
import twcore.core.*;
import twcore.core.game.Player;

public class PowerBallPlayer {

	String 	name;
	HashMap stats;
	int     m_freq;

	public PowerBallPlayer( Player player ) {

		name = player.getPlayerName();
		m_freq = player.getFrequency();

		stats = new HashMap();
		stats.put( new Integer( m_freq ), new PlayerStats() );
	}

	public boolean sameFrequency( int freq ) {

		if( freq == m_freq ) return true;
		else return false;
	}

	//Creates a new record and switches to store stats to that.
	public void switchRecord( int freq ) {

		if( !stats.containsKey( new Integer( freq ) ) )
			stats.put( new Integer( freq ), new PlayerStats() );
		m_freq = freq;
	}

	//Takes current record and modifies the frequency which it is stored under
	public void changeRecordFreq( int freq ) {

		PlayerStats thisStat = (PlayerStats)stats.get( new Integer( m_freq ) );
		stats.remove( new Integer( m_freq ) );
		stats.put( new Integer( freq ), thisStat );
	}

	//Resets current records and start with fresh one
	public void newRecord( int freq ) {

		stats.clear();
		stats.put( new Integer( freq ), new PlayerStats() );
	}

	public PlayerStats getPlayer() {

		return (PlayerStats)stats.get( new Integer( m_freq ) );
	}

}

class PlayerStats {

	HashMap		pickUp;
	int kills = 0, teamkills = 0, deaths = 0;
	int goals = 0, assists = 0;
	int steals = 0, turnOvers = 0;
	int saves = 0, shotsOnGoal = 0;
	int timeCarried = 0, lastCarry = 0;

	public PlayerStats() {
		pickUp = new HashMap();
	}

	public void addKill() { kills++; }
	public void setKills( int k ) { kills = k; }
	public int  getKills() { return kills; }

	public void addTeamKill() { teamkills++; }
	public void setTeamKills( int t ) { teamkills = t; }
	public int  getTeamKills() { return teamkills; }

	public void addDeath() { deaths++; }
	public void setDeaths( int d ) { deaths = d; }
	public int  getDeaths() { return deaths; }

	public void addGoal() { goals++; }
	public void setGoals( int g ) { goals = g; }
	public int getGoals() { return goals; }

	public void addAssist() { assists++; }
	public void setAssists( int a ) { assists = a; }
	public int getAssists() { return assists; }

	public void addSave() { saves++; }
	public void setSaves( int s ) { saves = s; }
	public int getSaves() { return saves; }

	public void addShotOnGoal() { shotsOnGoal++; }
	public void setShotsOnGoal( int s ) { shotsOnGoal = s; }
	public int getShotsOnGoal() { return shotsOnGoal; }

	public void addSteal() { steals++; }

	public void addTurnOver() { turnOvers++; }

	public void setPickUp( int ballId ) {
		pickUp.put( new Integer( ballId ), new Integer( (int)(System.currentTimeMillis()/1000)) );
	}

	public void endPickUp( int ballId ) {
		try {
			int time = ((Integer)pickUp.get( new Integer( ballId ) )).intValue();
			lastCarry = (int)(System.currentTimeMillis()/1000)-time;
			timeCarried += lastCarry;
		} catch (Exception e) {}
	}

	public int getLastCarry() { return lastCarry; }

}