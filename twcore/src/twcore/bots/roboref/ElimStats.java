package twcore.bots.roboref;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.EnumMap;


import twcore.core.util.Tools;

/**
 * Holds stats for one game of elimination for a particular player.
 * 
 * @author WingZero
 */
public class ElimStats {

    enum StatData { INT, DOUBLE, FLOAT }
    
    EnumMap<StatType, ElimStat> stats;   // stats from this game alone (aside from ave)
    EnumMap<StatType, ElimStat> total;   // stats loaded from the database
    
    boolean loaded;                      // true if stats have been loaded into total from database   
    
    protected int ship;
    
    DecimalFormat decimal = new DecimalFormat("##.00");
    
    /**
     * Constructs a new set of statistics for a given ship.
     * Maintains the current game stats as well as stats loaded from the database.
     * @param shipNum
     */
    public ElimStats(int shipNum) {
        ship = shipNum;
        stats = new EnumMap<StatType, ElimStat>(StatType.class);
        total = new EnumMap<StatType, ElimStat>(StatType.class);
        for (StatType stat : StatType.values()) {
            stats.put(stat, new ElimStat(stat));
            total.put(stat, new ElimStat(stat));
        }        
        loaded = false;
    }
    
    /** Returns the current ship number these stats represent */
    public int getShip() {
        return ship;
    }
    
    /** Increments the stat of type StatType as long as it is an integer data stat */
    public void incrementStat(StatType stat) {
        stats.get(stat).increment();
        if (stat == StatType.SHOTS)
            crunchAim(false);
    }
    
    /** Computes, sets and returns the aim stat for the current game or all games */
    public double crunchAim(boolean total) {
        double aim;
        if (!total) {
            if (getStat(StatType.SHOTS) > 0)
                aim = (((double) getStat(StatType.KILLS) / getStat(StatType.SHOTS)) * 100);
            else
                aim = (((double) getStat(StatType.KILLS) / 1) * 100);
            if (aim > 100)
                aim = 100;
            setStat(StatType.AIM, aim);
            return aim;
        } else {
            int shots = getTotal(StatType.SHOTS);
            int kills = getTotal(StatType.KILLS);
            if (shots > 0)
                aim = (((double) kills / shots) * 100);
            else
                aim = (((double) kills / 1) * 100);
            if (aim > 100)
                aim = 100;
            loadStat(StatType.AIM, aim);
            return aim;
        }
    }
    
    /** Computes and sets the ave stat given the rating of the player killed */
    public void crunchAve(int rating) {
        setStat(StatType.AVE, (((getAve(StatType.AVE)) * (float) getTotal(StatType.KILLS)) + (float) rating) / ((float) getTotal(StatType.KILLS) + 1f));
    }
    
    /** Computes and sets the player's rating after a game */
    public void crunchRating() {
        if (getTotal(StatType.DEATHS) > 0) 
            setStat(StatType.RATING, (getTotal(StatType.KILLS) / getTotal(StatType.DEATHS) * getStat(StatType.AVE)));
    }
    
    /** Decrements the stat of type StatType as long as it is an integer data stat */
    public void decrementStat(StatType stat) {
        stats.get(stat).decrement();
    }
    
    /** Sets the stat to the value specified as integer double or float */
    public void setStat(StatType stat, Object value) {
        if (value instanceof Integer)
            stats.get(stat).setValue((Integer) value);
        else if (value instanceof Double)
            stats.get(stat).setValue((Double) value);
        else if (value instanceof Float)
            stats.get(stat).setValue((Float) value);
    }
    
    /** Returns the total of stat by adding current game stat with database stat */
    public int getTotal(StatType stat) {
        if (stat.isInt())
            return getStat(stat) + total.get(stat).getInt();
        else return -1;
    }
    
    /** Returns the integer value of stat */
    public int getStat(StatType stat) {
        return stats.get(stat).getInt();
    }

    /** Returns the double value of stat */
    public double getAim(StatType stat) {
        return stats.get(stat).getDouble();
    }

    /** Returns the float value of stat */
    public float getAve(StatType stat) {
        return stats.get(stat).getFloat();
    }
    
    /** Returns the integer stat value found in the database stats */
    public int getDB(StatType stat) {
        return total.get(stat).getInt();
    }

    /** Returns the aim double stat from the database stats */
    public double getAimDB(StatType stat) {
        return total.get(stat).getDouble();
    }

    /** Returns the ave stat from the database stats */
    public float getAveDB(StatType stat) {
        return total.get(stat).getFloat();
    }
    
    /** Loads the stat from the database into the total stats array */
    public void loadStat(StatType stat, Object value) {
        if (value instanceof Integer)
            total.get(stat).setValue((Integer) value);
        else if (value instanceof Double)
            total.get(stat).setValue((Double) value);
        else if (value instanceof Float)
            total.get(stat).setValue((Float) value);
        
        if (stat.dependent())
            setStat(stat, value);
    }
    
    /** Sets loaded to true */
    public void loaded() {
        loaded = !loaded;
    }
    
    /** Checks to see if database stats have been loaded */
    public boolean isLoaded() {
        return loaded;
    }
    
    /** Updates the local database stats with the recent game stats to prepare for saving */
    public void unload() {
        crunchAim(true);
        crunchRating();
        for (StatType stat : stats.keySet()) {
            if (!stat.dependent() && stat.isInt())
                total.get(stat).add(stats.get(stat).getInt());
            else {
                if (stat.isInt())
                    total.get(stat).setValue(stats.get(stat).getInt());
                else if (stat.isDouble())
                    total.get(stat).setValue(stats.get(stat).getDouble());
                else if (stat.isFloat())
                    total.get(stat).setValue(stats.get(stat).getFloat());
            }
        }
    }
    
    /**
     * Get the list of stats for this set as were loaded from the database.
     * @param name This stat owner name
     * @param status Status from ElimPlayer
     * @return Array of Strings containing all available stat info
     */
    public String[] getStats(String name, String status) {
        String[] msg = {
                "" + Tools.shipName(ship) + " stats for " + name + ":  Rank(" + getDB(StatType.RANK) + ")",
                "Kills(" + getStat(StatType.KILLS) + ") Deaths(" + getStat(StatType.DEATHS) + ") Shots(" + getTotal(StatType.SHOTS) + ")  Status(" + status + ")",
                "Rating(" + getStat(StatType.RATING) + ") TotalKills(" + getTotal(StatType.KILLS) + ") TotalDeaths(" + getTotal(StatType.DEATHS) + ")",
                "TopKillStreak(" + getStat(StatType.BEST_KILL_STREAK) + ") TopDeathStreak(" + getStat(StatType.WORST_DEATH_STREAK) + ") KillStreak(" + getStat(StatType.KILL_STREAK) + ") DeathStreak(" + getStat(StatType.DEATH_STREAK) + ")",
                "TopMultiKill(" + getStat(StatType.BEST_MULTI_KILL) + ") MultiKills(" + getStat(StatType.MULTI_KILLS) + ") KOs(" + getStat(StatType.KNOCK_OUTS) + ") KillJoys(" + getStat(StatType.KILL_JOYS) + ")",
                "Shots(" + getStat(StatType.SHOTS) + ") WinStreak(" + getStat(StatType.WIN_STREAK) + ") TopWinStreak(" + getStat(StatType.BEST_WIN_STREAK) + ") AVE(" + getStat(StatType.AVE) + ") AIM(" + decimal.format(crunchAim(false)) + ") AIM(" + crunchAim(true) + ")"
        };
        return msg;
    }
    
    /**
     * Get the list of stats for this set as were loaded from the database.
     * @param name Supposed name of this stat owner
     * @return Array of Strings containing all available stat info
     */
    public String[] getStats(String name) {
        String[] msg = {
                "" + Tools.shipName(ship) + " stats for " + name + ": ",
                "Kills(" + getTotal(StatType.KILLS) + ") Deaths(" + getTotal(StatType.DEATHS) + ") Rating(" + getDB(StatType.RATING) + ") Shots(" + getTotal(StatType.SHOTS) + ")",
                "TopKillStreak(" + getDB(StatType.BEST_KILL_STREAK) + ") TopDeathStreak(" + getDB(StatType.WORST_DEATH_STREAK) + ") KillStreak(" + getDB(StatType.KILL_STREAK) + ") DeathStreak(" + getDB(StatType.DEATH_STREAK) + ")",
                "TopMultiKill(" + getDB(StatType.BEST_MULTI_KILL) + ") MultiKills(" + getDB(StatType.MULTI_KILLS) + ") KOs(" + getDB(StatType.KNOCK_OUTS) + ") KillJoys(" + getDB(StatType.KILL_JOYS) + ")",
                "WinStreak(" + getDB(StatType.WIN_STREAK) + ") TopWinStreak(" + getDB(StatType.BEST_WIN_STREAK) + ") AVE(" + getAveDB(StatType.AVE) + ") AIM(" + decimal.format(getAimDB(StatType.AIM)) + ")"
        };
        return msg;
    }
    
    /** 
     * GAME STAT HANDLERS 
     */
    public void handleKill() {
        incrementStat(StatType.KILLS);
        incrementStat(StatType.KILL_STREAK);
        setStat(StatType.DEATH_STREAK, 0);
        if (getStat(StatType.KILL_STREAK) > getStat(StatType.BEST_KILL_STREAK))
            setStat(StatType.BEST_KILL_STREAK, getStat(StatType.KILL_STREAK));
    }
    
    public void handleDeath() {
        incrementStat(StatType.DEATHS);
        incrementStat(StatType.DEATH_STREAK);
        setStat(StatType.KILL_STREAK, 0);
        if (getStat(StatType.DEATH_STREAK) > getStat(StatType.WORST_DEATH_STREAK))
            setStat(StatType.WORST_DEATH_STREAK, getStat(StatType.DEATH_STREAK));
    }
    
    public void handleKO() {
        incrementStat(StatType.KNOCK_OUTS);
    }
    
    public void handleKillJoy() {
        incrementStat(StatType.KILL_JOYS);
    }
    
    public void handleWin() {
        incrementStat(StatType.GAMES);
        incrementStat(StatType.WINS);
        incrementStat(StatType.WIN_STREAK);
        if (getStat(StatType.WIN_STREAK) > getStat(StatType.BEST_WIN_STREAK))
            setStat(StatType.BEST_WIN_STREAK, getStat(StatType.WIN_STREAK));
    }
    
    public void handleLoss() {
        incrementStat(StatType.GAMES);
        setStat(StatType.WIN_STREAK, 0);
    }
    
    public void handleMultiKill(int kills) {
        incrementStat(StatType.MULTI_KILLS);
        if (kills > getStat(StatType.BEST_MULTI_KILL))
            setStat(StatType.BEST_MULTI_KILL, kills);
    }
    
    /** Loads stats from the database into the total stat array */
    public void loadStats(ResultSet rs) throws SQLException {
        loadStat(StatType.KILLS, rs.getInt("fnKills"));
        loadStat(StatType.DEATHS, rs.getInt("fnDeaths"));
        loadStat(StatType.RATING, rs.getInt("fnRating"));
        loadStat(StatType.RANK, rs.getInt("fnRank"));
        loadStat(StatType.WINS, rs.getInt("fnWins"));
        loadStat(StatType.GAMES, rs.getInt("fnGames"));
        loadStat(StatType.AVE, rs.getFloat("fnAve"));
        loadStat(StatType.SHOTS, rs.getInt("fnShots"));
        loadStat(StatType.AIM, rs.getDouble("fnAim"));
        loadStat(StatType.KILL_JOYS, rs.getInt("fnKillJoys"));
        loadStat(StatType.KNOCK_OUTS, rs.getInt("fnKnockOuts"));
        loadStat(StatType.MULTI_KILLS, rs.getInt("fnMultiKills"));
        loadStat(StatType.WIN_STREAK, rs.getInt("fnWinStreak"));
        loadStat(StatType.KILL_STREAK, rs.getInt("fnKillStreak"));
        loadStat(StatType.DEATH_STREAK, rs.getInt("fnDeathStreak"));
        loadStat(StatType.BEST_KILL_STREAK, rs.getInt("fnTopKillStreak"));
        loadStat(StatType.WORST_DEATH_STREAK, rs.getInt("fnTopDeathStreak"));
        loadStat(StatType.BEST_WIN_STREAK, rs.getInt("fnTopWinStreak"));
        loadStat(StatType.BEST_MULTI_KILL, rs.getInt("fnTopMultiKill"));
        loaded();
    }
}
