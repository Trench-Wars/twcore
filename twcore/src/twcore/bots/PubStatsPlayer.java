package twcore.bots;

import java.util.Date;

/**
 * Domain object for storing the (public) statistics
 * This object is used by the pubbotstats and pubhubstats modules
 * 
 * @author Maverick
 */
public class PubStatsPlayer {
	private String name;
	private String squad;
	private String IP;
	private int timezone;
	private String usage;
	
	private Date date; // when this record was made
	
	public PubStatsPlayer(String name, String squad, String IP, String timezone, String usage) {
		this.name = name;
		this.squad = squad;
		this.IP = IP;
		this.timezone = convertTimezone(timezone);
		this.usage = usage;
		this.date = new Date();
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
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((IP == null) ? 0 : IP.hashCode());
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((squad == null) ? 0 : squad.hashCode());
        result = prime * result + timezone;
        result = prime * result + ((usage == null) ? 0 : usage.hashCode());
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
