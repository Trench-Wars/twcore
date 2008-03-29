package twcore.bots;

import java.util.Date;

/**
 * Domain object for storing the (public) statistics
 * This object is used by the pubbotstats and pubhubstats modules
 * 
 * @author Maverick
 */
public class PubStatsScore {
	private PubStatsPlayer player;
	
	/** Ship number (1-8, 0 = spectator) */
	private int ship;
	
	private int flagPoints;
	private int killPoints;
	private int wins;
	private int losses;
	private int rate;
	private float average;
	
	private Date lastUpdate;
	private boolean scorereset = false;
	
	// Variables to check if this score has been updated since last updatre
	private Date lastSave;
	
	public PubStatsScore(PubStatsPlayer player, int ship, int flagPoints, int killPoints, int wins, int losses, int rate, float average) {
		this.player = player;
		this.ship = ship;
		this.flagPoints = flagPoints;
		this.killPoints = killPoints;
		this.wins = wins;
		this.losses = losses;
		this.rate = rate;
		this.average = average;
		
		this.lastUpdate = new Date();
	}
	
	//******************************* Getters & Setters *************************************//
	
	
	/**
	 * @return the player
	 */
	public PubStatsPlayer getPlayer() {
		return player;
	}

	/**
	 * @param player the player to set
	 */
	public void setPlayer(PubStatsPlayer player) {
		this.player = player;
	}

	/**
	 * @return the ship
	 */
	public int getShip() {
		return ship;
	}

	/**
	 * @param ship the ship to set
	 */
	public void setShip(int ship) {
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
	 * @return the rate
	 */
	public int getRate() {
		return rate;
	}

	/**
	 * @param rate the rate to set
	 */
	public void setRate(int rate) {
		this.rate = rate;
	}

	/**
	 * @return the average
	 */
	public float getAverage() {
		return average;
	}

	/**
	 * @param average the average to set
	 */
	public void setAverage(float average) {
		this.average = average;
	}

	/**
	 * @return the date
	 */
	public Date getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * @param date the date to set
	 */
	public void setLastUpdate(Date date) {
		this.lastUpdate = date;
	}
	
	public void resetLastUpdate() {
	    this.lastUpdate = new Date();
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
     * @return the lastSave
     */
    public Date getLastSave() {
        return lastSave;
    }

    /**
     * @param lastSave the lastSave to set
     */
    public void setLastSave(Date lastSave) {
        this.lastSave = lastSave;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(average);
        result = prime * result + ((lastUpdate == null) ? 0 : lastUpdate.hashCode());
        result = prime * result + flagPoints;
        result = prime * result + killPoints;
        result = prime * result + losses;
        result = prime * result + ((player == null) ? 0 : player.hashCode());
        result = prime * result + rate;
        result = prime * result + (scorereset ? 1231 : 1237);
        result = prime * result + ship;
        result = prime * result + wins;
        return result;
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
        
        final PubStatsScore other = (PubStatsScore) obj;
        if( !player.equals(other.player) ||
            this.ship != other.ship ||
            !this.lastUpdate.equals(lastUpdate) ||
            this.wins != other.wins || 
            this.losses != other.losses || 
            this.killPoints != other.killPoints || 
            this.flagPoints != other.flagPoints)
            return false;
        return true;
    }
	
}
