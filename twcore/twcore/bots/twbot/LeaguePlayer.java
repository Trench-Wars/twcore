package twcore.bots.twbot;

import twcore.core.*;

public class LeaguePlayer {
	
	String playerName;
	int    shipType;
	int	   frequency;
	int    deathLimit;
	
	int	   deaths = 0;
	int    kills = 0;
	int	   teamkills = 0;
	int	   terrierKills = 0;
	int    score = 0;
	int	   lagouts = 0;
	
	boolean outOfGame = false;
	boolean laggedOut = false;
	boolean warned = false;
	boolean inBase = false;
	boolean saveState = false;
	int timeOfLagout = 0;
	int timer = 0;
	String sub = "  -";

	public LeaguePlayer( String name, int ship, int freq, int deaths ) {
		playerName = name;
		shipType = ship;
		frequency = freq;
		deathLimit = deaths;
		timer = (int)(System.currentTimeMillis()/1000);
	}
	
	public String getName() { return playerName; }
	
	public int getShip() { return shipType; }
	public void setShip( int s ) { shipType = s; }
	
	public int getFreq() { return frequency; }
	public int getDeathLimit() { return deathLimit; }
	public void setDeathLimit( int d ) { deathLimit = d; }
	
	public int getKills() { return kills; }
	public void addKill() { kills++; }
	public void setKills( int k ) { kills = k; }
	
	public int getTeamKills() { return teamkills; }
	public void addTeamKill() { teamkills++; }
	public void setTeamKills( int k ) { teamkills = k; }
	
	public int getTerrierKills() { return terrierKills; }
	public void addTerrierKill() { terrierKills++; }
	public void setTerrierKills( int k ) { terrierKills = k; }
	
	public int getDeaths() { return deaths; }
	public void addDeath() { 
		deaths++; 
		timer = (int)(System.currentTimeMillis()/1000);
		warned = false;
		inBase = false;
	}
	public void setDeaths( int d ) { deaths = d; }
	
	public int getScore() { return score; }
	public void addToScore( int s ) { score += s; }
	public void setScore( int s ) { score = s; }
	
	public int getLagouts() { return lagouts; }
	public void addLagout() { lagouts++; }
	public void setLagouts( int l ) { lagouts = l; }
	
	public void isOut() { outOfGame = true; }
	public boolean isOutOfGame() { return outOfGame; }
	
	public void laggedOut() { laggedOut = true; }
	
	public void notLaggedOut() { 
		laggedOut = false; 
		timer = (int)(System.currentTimeMillis()/1000);
	}
	
	public boolean isLagged() { return laggedOut; }
	
	public int getTimeBetweenLagouts() {
		int time = (int)(System.currentTimeMillis()/1000);
		time -= timeOfLagout;
		return time;
	}
	
	public void subbedBy( String name ) { sub = name; }
	public String getSub() { return sub; }
	
	public void updateTimer() { 
		timer = (int)(System.currentTimeMillis()/1000); 
		warned = false;
		inBase = false;
	}
	
	public boolean hasBeenInBase() { return inBase; }
	public void inBase() { inBase = true; }
	public void notInBase() { inBase = false; }
	
	public int timeOutOfBounds() {
		int time = (int)(System.currentTimeMillis()/1000);
		time -= timer;
		return time;
	}
	
	public boolean warned() { return warned; }
	public void haveWarned() { 
		warned = true; 
		//timer = (int)(System.currentTimeMillis()/1000);
	}
	
	public boolean state() { return saveState; }
	public void toggleState() { saveState = true; }
	
}