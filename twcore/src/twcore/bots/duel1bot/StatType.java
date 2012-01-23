package twcore.bots.duel1bot;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Taken from elim bot.
 */
public enum StatType { 
    SPAWNS("fnSpawnKills", false),
    WARPS("fnWarps", false),
    LAGOUTS("fnLagouts", false),
    PLAYTIME("fnTimePlayed", false),
    
    KILLS("fnKills", false), 
    DEATHS("fnDeaths", false), 
    MULTI_KILLS("fnMultiKills", false), 
    KILL_STREAK("fnKillStreak", true), 
    DEATH_STREAK("fnDeathStreak", true), 
    WIN_STREAK("fnWinStreak", true), 
    SHOTS("fnShots", false), 
    KILL_JOYS("fnKillJoys", false), 
    KNOCK_OUTS("fnKnockOuts", false), 
    BEST_MULTI_KILL("fnTopMultiKill", false), 
    BEST_KILL_STREAK("fnTopKillStreak", false), 
    WORST_DEATH_STREAK("fnTopDeathStreak", false), 
    BEST_WIN_STREAK("fnTopWinStreak", false),
    AVE("fnAve", StatData.FLOAT, true), 
    RATING("fnRating", true), 
    AIM("fnAim", StatData.DOUBLE, false), 
    WINS("fnWins", false), 
    GAMES("fnGames", false), 
    RANK("fnRank", true);
    
    private String dbName;
    private boolean dbOnly;
    private StatData dataType;
    
    private static final Map<String, StatType> lookup = new HashMap<String, StatType>();
    
    static {
        for (StatType s : EnumSet.allOf(StatType.class))
            lookup.put(s.db(), s);
    }
    
    private StatType(String dbname, boolean dbonly) {
        dbName = dbname;
        dbOnly = dbonly;
        dataType = StatData.INT;
    }
    
    private StatType(String dbname, StatData type, boolean dbonly) {
        dbName = dbname;
        dbOnly = dbonly;
        dataType = type;
    }
    
    public String db() {
        return dbName;
    }
    
    public static StatType sql(String name) {
        return lookup.get(name);
    }
    
    public boolean dependent() {
        return dbOnly;
    }
    
    public StatData type() {
        return dataType;
    }
    
    public boolean isInt() {
        return (dataType == StatData.INT);
    }
    
    public boolean isDouble() {
        return (dataType == StatData.DOUBLE);
    }
    
    public boolean isFloat() {
        return (dataType == StatData.FLOAT);
    }
}
