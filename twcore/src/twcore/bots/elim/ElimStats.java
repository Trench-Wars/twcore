package twcore.bots.elim;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.EnumMap;


import twcore.bots.elim.elim.ShipType;
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
    
    DecimalFormat decimal = new DecimalFormat("#0.00");
    
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
            loadStat(StatType.AIM, aim);
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
            setStat(StatType.AIM, aim);
            loadStat(StatType.AIM, aim);
            return aim;
        }
    }
    
    /** Computes and sets the ave stat given the rating of the player killed */
    public void crunchAve(int rating) {
        setStat(StatType.AVE, (Float) (((getAve(StatType.AVE)) * (float) getTotal(StatType.KILLS)) + (float) rating) / ((float) getTotal(StatType.KILLS) + 1f));
    }
    
    /** Computes and sets the player's rating after a game */
    public void crunchRating() {
        if (getTotal(StatType.DEATHS) > 0) 
            setStat(StatType.RATING, (int)((getTotal(StatType.KILLS) / getTotal(StatType.DEATHS) * getAve(StatType.AVE))));
        else
            setStat(StatType.RATING, (int)(getTotal(StatType.KILLS) * getAve(StatType.AVE)));
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
    
    public int getRating() {
        return stats.get(StatType.RATING).getInt();
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
    
    /** Checks to see if database stats have been loaded */
    public boolean isLoaded() {
        return loaded;
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
     * Get stat list for the current ship from the database records.
     * @param name Supposed name of this stat owner
     * @return Array of Strings containing all available stat info
     */
    public String[] getStats(String name) {
        /*
        JAVELIN stats for WingZero: Rank #0 Rating: 1234
         K:123 D:43123 Ave:321 Aim:12.22% BestKillStreak:12 WorstDeathStreak:4
         KOs:32 KillJoys:55 MultiKills:21 BestMultiKill:4 
         Games:55112 Wins:1234 BestWinStreak:5 CurrentStreak:0
        
        old:
                "Kills(" + getTotal(StatType.KILLS) + ") Deaths(" + getTotal(StatType.DEATHS) + ") Rating(" + getDB(StatType.RATING) + ") Shots(" + getTotal(StatType.SHOTS) + ")",
                "TopKillStreak(" + getDB(StatType.BEST_KILL_STREAK) + ") TopDeathStreak(" + getDB(StatType.WORST_DEATH_STREAK) + ") KillStreak(" + getDB(StatType.KILL_STREAK) + ") DeathStreak(" + getDB(StatType.DEATH_STREAK) + ")",
                "TopMultiKill(" + getDB(StatType.BEST_MULTI_KILL) + ") MultiKills(" + getDB(StatType.MULTI_KILLS) + ") KOs(" + getDB(StatType.KNOCK_OUTS) + ") KillJoys(" + getDB(StatType.KILL_JOYS) + ")",
                "WinStreak(" + getDB(StatType.WIN_STREAK) + ") TopWinStreak(" + getDB(StatType.BEST_WIN_STREAK) + ") AVE(" + getAveDB(StatType.AVE) + ") AIM(" + decimal.format(getAimDB(StatType.AIM)) + ")"
        */
        String rank = "not ranked ";
        if (getDB(StatType.RANK) > 0)
            rank = "#" + getDB(StatType.RANK); 
        String[] msg = {
                "" + ShipType.type(ship).toString() + " stats for " + name + ": Rank " + rank + " Rating: " + getDB(StatType.RATING),
                " K:" + getTotal(StatType.KILLS) + " D:" + getTotal(StatType.DEATHS) + " Ave:" + getAve(StatType.AVE) + " Aim:" + decimal.format(getAimDB(StatType.AIM)) + "% BestKillStreak:" + getDB(StatType.BEST_KILL_STREAK) + " WorstDeathStreak:" + getDB(StatType.WORST_DEATH_STREAK),
                " KOs:" + getDB(StatType.KNOCK_OUTS) + " KillJoys:" + getDB(StatType.KILL_JOYS) + " MultiKills:" + getDB(StatType.MULTI_KILLS) + " BestMultiKill:" + getDB(StatType.BEST_MULTI_KILL),
                " Games:" + getDB(StatType.GAMES) + " Wins:" + getDB(StatType.WINS) + " BestWinStreak:" + getDB(StatType.BEST_WIN_STREAK) + " CurrentStreak:" + getDB(StatType.WIN_STREAK),
        };
        return msg;
    }
    
    /**
     * Get stat list for the all ships from the database records.
     * @param name Supposed name of this stat owner
     * @return Array of Strings containing all available stat info
     */
    public String[] getAll(String name) {
        String rank = "not ranked ";
        if (getDB(StatType.RANK) > 0)
            rank = "#" + getDB(StatType.RANK); 
        String[] msg = {
                "Total ship stats for " + name + ": BestRank " + rank + " BestRating: " + getDB(StatType.RATING),
                " K:" + getTotal(StatType.KILLS) + " D:" + getTotal(StatType.DEATHS) + " BestAve:" + getAveDB(StatType.AVE) + " Aim:" + decimal.format(crunchAim(true)) + "% BestKillStreak:" + getDB(StatType.BEST_KILL_STREAK) + " WorstDeathStreak:" + getDB(StatType.WORST_DEATH_STREAK),
                " KOs:" + getDB(StatType.KNOCK_OUTS) + " KillJoys:" + getDB(StatType.KILL_JOYS) + " MultiKills:" + getDB(StatType.MULTI_KILLS) + " BestMultiKill:" + getDB(StatType.BEST_MULTI_KILL),
                " Games:" + getDB(StatType.GAMES) + " Wins:" + getDB(StatType.WINS) + " BestWinStreak:" + getDB(StatType.BEST_WIN_STREAK),
        };
        return msg;
    }
    
    /**
     * Get streak stat list for the current ship.
     * @param name Supposed name of this stat owner
     * @return string containing all available streak info
     */
    public String getStreak(String name) {
        return "Current streak stats for " + name + ": KillStreak:" + getStat(StatType.KILL_STREAK) + " DeathStreak:" + getStat(StatType.DEATH_STREAK) + " BestKillStreak:" + getStat(StatType.BEST_KILL_STREAK) + " WorstDeathStreak:" + getStat(StatType.WORST_DEATH_STREAK);
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
        unload();
    }
    
    public void handleLoss() {
        incrementStat(StatType.GAMES);
        setStat(StatType.WIN_STREAK, 0);
        unload();
    }
    
    public void handleMultiKill(int kills) {
        incrementStat(StatType.MULTI_KILLS);
        if (kills > getStat(StatType.BEST_MULTI_KILL))
            setStat(StatType.BEST_MULTI_KILL, kills);
    }
    
    /** Updates the local database stats with the recent game stats to prepare for saving */
    private void unload() {
        crunchAim(true);
        crunchRating();
        total.get(StatType.KILLS).add(getStat(StatType.KILLS));
        total.get(StatType.DEATHS).add(getStat(StatType.DEATHS));
        total.get(StatType.WINS).add(getStat(StatType.WINS));
        total.get(StatType.GAMES).add(getStat(StatType.GAMES));
        total.get(StatType.SHOTS).add(getStat(StatType.SHOTS));
        total.get(StatType.KILL_JOYS).add(getStat(StatType.KILL_JOYS));
        total.get(StatType.KNOCK_OUTS).add(getStat(StatType.KNOCK_OUTS));
        total.get(StatType.MULTI_KILLS).add(getStat(StatType.MULTI_KILLS));
        loadStat(StatType.RATING, getStat(StatType.RATING));
        loadStat(StatType.AVE, getAve(StatType.AVE));
        loadStat(StatType.AIM, getAim(StatType.AIM));
        loadStat(StatType.WIN_STREAK, getStat(StatType.WIN_STREAK));
        loadStat(StatType.KILL_STREAK, getStat(StatType.KILL_STREAK));
        loadStat(StatType.DEATH_STREAK, getStat(StatType.DEATH_STREAK));
        if (getStat(StatType.BEST_KILL_STREAK) > getDB(StatType.BEST_KILL_STREAK))
            loadStat(StatType.BEST_KILL_STREAK, getStat(StatType.BEST_KILL_STREAK)); 
        if (getStat(StatType.WORST_DEATH_STREAK) > getDB(StatType.WORST_DEATH_STREAK))
            loadStat(StatType.WORST_DEATH_STREAK, getStat(StatType.WORST_DEATH_STREAK)); 
        if (getStat(StatType.BEST_WIN_STREAK) > getDB(StatType.BEST_WIN_STREAK))
            loadStat(StatType.BEST_WIN_STREAK, getStat(StatType.BEST_WIN_STREAK)); 
        if (getStat(StatType.BEST_KILL_STREAK) > getDB(StatType.BEST_KILL_STREAK))
            loadStat(StatType.BEST_MULTI_KILL, getStat(StatType.BEST_MULTI_KILL));
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
        loaded = true;
    }
    
    public void loadAllShips(ResultSet rs) throws SQLException {
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
        while (rs.next()) {
            total.get(StatType.KILLS).add(rs.getInt("fnKills"));
            total.get(StatType.DEATHS).add(rs.getInt("fnDeaths"));
            total.get(StatType.WINS).add(rs.getInt(StatType.WINS.db()));
            total.get(StatType.GAMES).add(rs.getInt(StatType.GAMES.db()));
            total.get(StatType.KILL_JOYS).add(rs.getInt(StatType.KILL_JOYS.db()));
            total.get(StatType.KNOCK_OUTS).add(rs.getInt(StatType.KNOCK_OUTS.db()));
            total.get(StatType.MULTI_KILLS).add(rs.getInt(StatType.MULTI_KILLS.db()));
            int value = rs.getInt("fnRating");
            if (getDB(StatType.RATING) < value)
                loadStat(StatType.RATING, value);
            value = rs.getInt("fnRank");
            if (getDB(StatType.RANK) < value)
                loadStat(StatType.RANK, value);
            value = rs.getInt("fnAve");
            if (getDB(StatType.AVE) < value)
                loadStat(StatType.AVE, value);
            value = rs.getInt("fnTopKillStreak");
            if (value > getDB(StatType.BEST_KILL_STREAK))
                loadStat(StatType.BEST_KILL_STREAK, value);
            value = rs.getInt("fnTopDeathStreak");
            if (value > getDB(StatType.WORST_DEATH_STREAK))
                loadStat(StatType.WORST_DEATH_STREAK, value);
            value = rs.getInt("fnTopWinStreak");
            if (value > getDB(StatType.BEST_WIN_STREAK))
                loadStat(StatType.BEST_WIN_STREAK, value);
            value = rs.getInt("fnTopMultiKill");
            if (value > getDB(StatType.BEST_MULTI_KILL))
                loadStat(StatType.BEST_MULTI_KILL, value);
        }
        loaded = true;
    }
}
