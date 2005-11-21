/*
 *
 * StrikeballPlayer.java
 *
 */


package twcore.bots.strikeballbot;

import java.util.TimerTask;

import twcore.core.BotAction;

public class StrikeballPlayer {

    private String m_name;
    private String m_teamName;
    private int m_team;

    private BotAction m_botAction;
    private TimerTask m_lagoutTimer;

    // Warnings
    private int m_warning1 = -1;
    private int m_warning2 = -1;
    private int m_warning3 = -1;
    private boolean m_isWarnedOut = false;    // Has this player been warned out of the game?
    private boolean m_isLaggedOut = false;    // Has this player lagged out of the game?
    private boolean m_canDoLagout = false;    // Set true when player can !lagout back in

    // Stats
    private int m_stat_kills = 0;
    private int m_stat_deaths = 0;
    private int m_stat_goals = 0;
    private int m_stat_assists = 0;
    private int m_stat_carries = 0;
    private int m_stat_steals = 0;
    private int m_stat_turnovers = 0;



    public StrikeballPlayer( String playerName, int team, String teamName, BotAction m_botAction ) {
        m_name = playerName;
        m_team = team;
        m_teamName = teamName;
        this.m_botAction = m_botAction;
    }



    public void setTeam( int team ) {
        m_team = team;
    }



    public void setTeamName( String teamName ) {
        m_teamName = teamName;
    }



    /** Adds a warning to player.
     * @param warnType 0-Generic, 1-Lag/Phase, 2-Cherrypicking
     * @return true if this is their final warning.
     */
    public boolean addWarning( int warnType ) {

        if( m_warning1 == -1 ) {
            m_warning1 = warnType;
            m_isWarnedOut = false;
            return false;
        }

        if( m_warning2 == -1 ) {
            m_warning2 = warnType;
            m_isWarnedOut = false;
            return false;
        }

        if( m_warning3 == -1 ) {
            m_warning3 = warnType;
            m_isWarnedOut = true;
            return true;
        }

        m_isWarnedOut = true;
        return true;
    }



    public void setLaggedOut( boolean lagged ) {
        m_isLaggedOut = lagged;
        m_canDoLagout = false;
        if( m_isLaggedOut == true ) {

            if( m_lagoutTimer != null )
                m_lagoutTimer.cancel();
            m_lagoutTimer = new TimerTask() {
                public void run() {
                    allowLagout();
                };
            };
            m_botAction.scheduleTask(m_lagoutTimer, 30000 ); // Wait 30 seconds before !lagout

            m_botAction.sendPrivateMessage( m_name, "You have lagged out, and will be PMed in 30 seconds to allow you to get back into the game." );
        }

    }



    public void allowLagout() {
        m_canDoLagout = true;
        m_botAction.sendPrivateMessage( m_name, "You may now re-enter the game by PMing me with !lagout." );
    }



    public void cancelLagoutTask() {
        if( m_lagoutTimer != null )
            m_lagoutTimer.cancel();
    }



    public void resetPlayer() {
        m_warning1 = -1;
        m_warning2 = -1;
        m_warning3 = -1;
        m_isWarnedOut = false;
        m_isLaggedOut = false;
        m_canDoLagout = false;
        m_stat_kills = 0;
        m_stat_deaths = 0;
        m_stat_goals = 0;
        m_stat_assists = 0;
        m_stat_carries = 0;
        m_stat_steals = 0;
        m_stat_turnovers = 0;

        cancelLagoutTask();
    }



    public void incKills() {
        m_stat_kills++;
    }



    public void incDeaths() {
        m_stat_deaths++;
    }



    public void incGoals() {
        m_stat_goals++;
    }



    public void incAssists() {
        m_stat_assists++;
    }



    public void incCarries() {
        m_stat_carries++;
    }



    public void incSteals() {
        m_stat_steals++;
    }



    public void incTurnovers() {
        m_stat_turnovers++;
    }




// ********************************** ACCESSORS ************************************


    public String getName() {
        return m_name;
    }



    public String getTeamName() {
        return m_teamName;
    }



    public int getTeam() {
        return m_team;
    }



    public boolean isWarnedOut() {
        return m_isWarnedOut;
    }



    public boolean isLaggedOut() {
        return m_isLaggedOut;
    }



    public boolean canDoLagout() {
        return m_canDoLagout;
    }



    /** Returns all stats in order.
     * @return int array containing stats: kills, deaths, goals, assists, carries, steals, turnovers
     */
    public int[] getAllStats() {
        int[] stats = { 0, 0, 0, 0, 0, 0, 0, 0 };
        stats[0] = getKills();
        stats[1] = getDeaths();
        stats[2] = getGoals();
        stats[3] = getAssists();
        stats[4] = getCarries();
        stats[5] = getSteals();
        stats[6] = getTurnovers();
        stats[7] = getWarnings();
        return stats;
    }



    public int getKills() {
        return m_stat_kills;
    }



    public int getDeaths() {
        return m_stat_deaths;
    }



    public int getGoals() {
        return m_stat_goals;
    }



    public int getAssists() {
        return m_stat_assists;
    }



    public int getCarries() {
        return m_stat_carries;
    }



    public int getSteals() {
        return m_stat_steals;
    }



    public int getTurnovers() {
        return m_stat_kills;
    }



    public int getWarnings() {
        int warnings = 0;
        if( m_warning1 != -1 )
            warnings++;
        if( m_warning2 != -1 )
            warnings++;
        if( m_warning3 != -1 )
            warnings++;
        return warnings;
    }



    public String[] getWarningInfo( ) {
        String[] warnings = { "", "", "" };

        warnings[0] = getWarningDesc( m_warning1 );
        warnings[1] = getWarningDesc( m_warning2 );
        warnings[2] = getWarningDesc( m_warning3 );

        return warnings;
    }



    public String getWarningDesc( int warnType ) {

        switch( warnType ) {
        case 0:
            return "Generic warning";
        case 1:
            return "Lagging/causing a phase";
        case 2:
            return "Cherrypicking";
        default:
            return "(no warning)";
        }

    }

}