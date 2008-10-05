package twcore.core.helper.pubstats;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Domain object for storing the (public) statistics
 * This object is used by the pubbotstats and pubhubstats modules
 * 
 * @author Maverick
 */
public class PubStatsPlayer {
	private String name;
	private String squad;
	private short ship;
	
	// Information from *info
	private String IP;
	private String machineID;
	private int timezone;
	private String usage;
	private String dateCreated;
	
	// Information from *einfo
    private int userID = -1;
    private String resolution;
    private String client;
	
    // Score variables (overall)
    private int flagPoints;
    private int killPoints;
    private int wins;
    private int losses;
    
    // Extra score variables for pubstats
    private Map<Short,PubStatsScore> shipScores = Collections.synchronizedMap(new HashMap<Short, PubStatsScore>());
    // < shipnr (1-8), number of kills >
    
    private HashMap<Short,Long> shipTime = new HashMap<Short, Long>();
    // < shipnr (1-8), amount of seconds in ship >
    
    private long lastUpdate;
    private long lastSeen;
    private long lastShipchange;
    private long lastSave;
    private boolean scorereset = false;
    
    
    
	public PubStatsPlayer(String name, String squad, short ship) {
		this.name = name;
		this.squad = squad;
		this.ship = ship;
		this.lastUpdate = System.currentTimeMillis();
		this.lastSeen = System.currentTimeMillis();
		this.lastShipchange = System.currentTimeMillis();
	}
	
	private int convertTimezone(String timezone) {
	    int tz;
	    try {
	        tz = Integer.parseInt(timezone);
	    } catch(NumberFormatException nfe) {
	        tz = 0;    // Too bad, something went wrong
	    }
	    return tz;
	}
	
	public void scorereset() {
	    this.scorereset = true;
	    
	    shipScores.clear();
	    this.flagPoints = 0;
	    this.killPoints = 0;
	    this.wins = 0;
	    this.losses = 0;
	}
	
	public void updated() {
	    this.lastUpdate = System.currentTimeMillis();
	}
	public void seen() {
	    this.lastSeen = System.currentTimeMillis();
	}
	
	public void shipchange(short newShip) {
	    long time = 0;
	    
	    if(shipTime.containsKey(ship)) {
	        time = shipTime.get(ship);
	    }
	    
	    // calculate difference
	    long diff = (System.currentTimeMillis() - this.lastShipchange)/1000;
	    time = time + diff;
	    
	    // Save total time (sec)
	    shipTime.put(ship, time);
	    
	    // Perform ship change
	    this.ship = newShip;
	    this.lastShipchange = System.currentTimeMillis();
	}
	
	/**
	 * Adds scores for the given ship 
	 * (note; cumulative numbers must be given as they are added to the current known ship scores)
	 * 
	 * @param ship
	 */
	public void updateShipScore(short ship, int flagPoints, int killPoints, int wins, int losses) {
	    if(shipScores.containsKey(ship)) {
	        PubStatsScore score = shipScores.get(ship);
	        score.setFlagPoints(score.getFlagPoints()+flagPoints);
	        score.setKillPoints(score.getKillPoints()+killPoints);
	        score.setWins(score.getWins()+wins);
	        score.setLosses(score.getLosses()+losses);
	    } else {
	        PubStatsScore score = new PubStatsScore();
	        score.setFlagPoints(flagPoints);
	        score.setKillPoints(killPoints);
	        score.setWins(wins);
	        score.setLosses(losses);
	        shipScores.put(ship, score);
	    }
	}
	
	/** 
	 * Adds a teamkill count to the specified ship-score of this player
	 * @param ship
	 */
	public void addTeamkill(short ship) {
	    if(shipScores.containsKey(ship)) {
	        PubStatsScore score = shipScores.get(ship);
	        score.setTeamkills(score.getTeamkills()+1);
	    }
	}
	
	public PubStatsScore getShipScore(short ship) {
	    if(shipScores.containsKey(ship)) {
	        return shipScores.get(ship);
	    }
	    return null;
	}
	
	public PubStatsScore getCurrentShipScore() {
	    return this.getShipScore(this.ship);
	}
	
	public void removeShipScore(short ship) {
	    shipScores.remove(ship);
	}
	
	/**
	 * Returns whether the information from *info is filled 
	 * @return
	 */
	public boolean isExtraInfoFilled() {
	    return 
	        userID != -1 &&
	        resolution != null && resolution.trim().length() > 0 &&
	        IP != null && IP.trim().length() > 0 &&
	        machineID != null && machineID.trim().length() > 0 &&
	        usage != null && usage.trim().length() > 0 &&
	        dateCreated != null && dateCreated.trim().length() > 0;
	    
	}
	
	//******************************* Getters & Setters *************************************//
	
	

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the squad
	 */
	public String getSquad() {
		return squad;
	}

	/**
	 * @param squad the squad to set
	 */
	public void setSquad(String squad) {
		this.squad = squad;
	}

	/**
	 * @return the iP
	 */
	public String getIP() {
		return IP;
	}

	/**
	 * @param ip the iP to set
	 */
	public void setIP(String ip) {
		IP = ip;
	}
	
	/**
     * @return the machineID
     */
    public String getMachineID() {
        return machineID;
    }

    /**
     * @param machineID the machineID to set
     */
    public void setMachineID(String machineID) {
        this.machineID = machineID;
    }

    /**
	 * @return the timezone
	 */
	public int getTimezone() {
		return timezone;
	}

	/**
	 * @param timezone the timezone to set
	 */
	public void setTimezone(String timezone) {
		this.timezone = convertTimezone(timezone);
	}

	/**
	 * @return the usage
	 */
	public String getUsage() {
		return usage;
	}

	/**
	 * @param usage the usage to set
	 */
	public void setUsage(String usage) {
		this.usage = usage;
	}
	
    /**
     * @return the dateCreated
     */
    public String getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }
    
    /**
     * @return the userID
     */
    public int getUserID() {
        return userID;
    }

    /**
     * @param userID the userID to set
     */
    public void setUserID(int userID) {
        this.userID = userID;
    }

    /**
     * @return the resolution
     */
    public String getResolution() {
        return resolution;
    }

    /**
     * @param resolution the resolution to set
     */
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
    
    /**
     * @return the client
     */
    public String getClient() {
        return client;
    }

    /**
     * @param client the client to set
     */
    public void setClient(String client) {
        this.client = client;
    }

    /**
     * @return the ship
     */
    public short getShip() {
        return ship;
    }

    /**
     * @param ship the ship to set
     */
    public void setShip(short ship) {
        this.ship = ship;
    }

    /**
     * @return the flagPoints
     */
    public int getFlagPoints() {
        return flagPoints;
    }

    /**
     * @param flagPoints the flagPoints to set
     */
    public void setFlagPoints(int flagPoints) {
        this.flagPoints = flagPoints;
    }

    /**
     * @return the killPoints
     */
    public int getKillPoints() {
        return killPoints;
    }

    /**
     * @param killPoints the killPoints to set
     */
    public void setKillPoints(int killPoints) {
        this.killPoints = killPoints;
    }

    /**
     * @return the wins
     */
    public int getWins() {
        return wins;
    }

    /**
     * @param wins the wins to set
     */
    public void setWins(int wins) {
        this.wins = wins;
    }

    /**
     * @return the losses
     */
    public int getLosses() {
        return losses;
    }

    /**
     * @param losses the losses to set
     */
    public void setLosses(int losses) {
        this.losses = losses;
    }

    /**
     * @return the lastUpdate
     */
    public long getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param lastUpdate the lastUpdate to set
     */
    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * @return the lastSave
     */
    public long getLastSave() {
        return lastSave;
    }

    /**
     * @param lastSave the lastSave to set
     */
    public void setLastSave(long lastSave) {
        this.lastSave = lastSave;
    }

    /**
     * @return the scorereset
     */
    public boolean isScorereset() {
        return scorereset;
    }

    /**
     * @param scorereset the scorereset to set
     */
    public void setScorereset(boolean scorereset) {
        this.scorereset = scorereset;
    }

    /**
     * @param timezone the timezone to set
     */
    public void setTimezone(int timezone) {
        this.timezone = timezone;
    }
    
    
    
    /**
     * @return the lastSeen
     */
    public long getLastSeen() {
        return lastSeen;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        
        
        final PubStatsPlayer other = (PubStatsPlayer) obj;
        if( !this.name.equals(other.name) ||
            !this.squad.equals(other.squad) ||
            !this.IP.equals(other.IP) ||
            this.timezone != other.timezone ||
            !this.usage.equals(other.usage))
            return false;
        return true;
    }
	
	
	
}
