/*
 * Created on Jan 3, 2005
 *
 * A streak is the number of kills in a row without dieing.
 * A multikill is the number of kills in a row without dieing within a certain time limit.
 */
package twcore.bots.statsbot;

import java.text.DecimalFormat;

/**
 * @author Austin
 */
public class PlayerStatistics {

	public static final int MULTIKILL_TIME = 5000; // 5 seconds
	public static final int HIT_DELAY = 200; //0.10 seconds

	private String 	m_name;
	private int		m_freq;
	private int		m_ship;
	private boolean	m_watchDamage;

	//Basic stats
	private int		m_kills		 	= 0;
	private int		m_teamKills	 	= 0;
	private int		m_deaths	 	= 0;
	private int		m_teamDeaths 	= 0;
	private int 	m_bounty	 	= 0;	//Current bounty
	private int 	m_runningBounty	= 0;	//Running bounty
	private int		m_damageDealt	= 0;
	private int		m_damageTaken	= 0;
	private int		m_hitsDealt		= 0;
	private int		m_hitsTaken		= 0;
	private int		m_shots			= 0;
	private int	    m_flagTouch		= 0;

	//More advanced stats
	private int		m_streak		= 0;
	private int		m_multi			= 0;

	//Top stats
	private int		m_topStreak 	= 0;	//Top streak player has had
	private int		m_topMulti  	= 0;	//Top multikill player has had
	private int		m_topBounty 	= 0;	//Top bounty player has had
	private int		m_topBountyKill = 0;	//Top bounty from kill

	//Stat helpers
	private long	m_lastHit		= 0;	//Time of last hit
	private long	m_lastKill 		= 0;	//Time of last kill

	public PlayerStatistics( String _name, int _ship, int _freq ) {

		m_name = _name;
		m_ship = _ship;
		m_freq = _freq;

		m_watchDamage = true;
	}

	/** Handles a normal kill
	 * @param _bountyGained The bounty of the killed player
	 */
	public void processKill( int _bountyGained ) {

		//increment kills
		m_kills++;

		//increment current bounty & total running
		m_bounty += _bountyGained;
		m_runningBounty += _bountyGained;

		//increment current streak
		m_streak++;

		//check for top bounty kill
		if( _bountyGained > m_topBountyKill ) m_topBountyKill = _bountyGained;

		//If this kill was within 5 seconds of the last it is a multikill
		if( (System.currentTimeMillis() - m_lastKill) < MULTIKILL_TIME ) {

			m_multi++;
		} else {

			//Check for top multi
			if( m_multi > m_topMulti ) m_topMulti = m_multi;
			m_multi = 1;
		}

		//Store time of kill
		m_lastKill = System.currentTimeMillis();

	}

	/** Handles a normal death
	 * @param _bountyLost The bounty the player had before dieing
	 */
	public void processDeath( int _bountyLost ) {

		//increment deaths
		m_deaths++;

		//check for high bounty
		if( _bountyLost > m_topBounty ) m_topBounty = _bountyLost;

		resetCheckMultiStreak();
	}

	/** Handles a kill on a teammate
	 *
	 */
	public void processTeamKill() {

		//increment teamkills
		m_teamKills++;

		resetCheckMultiStreak();
	}

	/** Handles a death by a teammate
	 *
	 */
	public void processTeamDeath() {

		//increment teamdeaths and deaths
		m_deaths++;
		m_teamDeaths++;

		resetCheckMultiStreak();
	}

	/** Handles damage dealt upon a player
	 * @param _damage The amount of damage dealt
	 */
	public void processDamageDealt( int _damage ) {

		m_damageDealt += _damage;

		if( (System.currentTimeMillis() - m_lastHit) > HIT_DELAY ) {
			m_hitsDealt++;
			m_lastHit = System.currentTimeMillis();
		}
	}

	/** Handles damage taken from a player
	 * @param _damage The amount of damage taken
	 */
	public void processDamageTaken( int _damage ) {

		m_damageTaken += _damage;
		m_hitsTaken++;
	}

	public void processShot() {

		m_shots++;
	}

	public void processFlagTouch() {

		m_flagTouch++;
	}

	/** Resets the current running multikill/streak counts and checks for top of each.
	 * They should be reset upon any death or kill of a teammate.
	 */
	private void resetCheckMultiStreak() {

		//check for high streak
		if( m_streak > m_topStreak ) m_topStreak = m_streak;

		//check for high multi
		if( m_multi > m_topMulti ) m_topMulti = m_multi;

		//reset streak
		m_streak = 0;

		//reset multi
		m_multi = 0;
	}

	public void setWatchDamage( boolean _watching ) {

		m_watchDamage = _watching;
	}

	public boolean isWatchingDamage() {
		return m_watchDamage;
	}

	public String toString() {

		DecimalFormat dm = new DecimalFormat( "0.##" );
		double acc = (double)m_hitsDealt / (double)m_shots;

		String ret = "("+m_name+":"+m_ship+") K/D: " + m_kills + "/" + m_deaths + "  Tk/Td: " + m_teamKills + "/" + m_teamDeaths;
		ret += "  S/M: " + m_streak + "/" + m_multi + " St/Mt: " + m_topStreak + "/" + m_topMulti;
		ret += "  Tb/Tbk: " + m_topBounty + "/" + m_topBountyKill + "  Dd/Dt: " + m_damageDealt + "/" + m_damageTaken;
		ret += "  Hd/Ht: " + m_hitsDealt + "/" + m_hitsTaken + "  Ft: " + m_flagTouch + " ("+dm.format( acc )+")";
		return ret;
	}

}
