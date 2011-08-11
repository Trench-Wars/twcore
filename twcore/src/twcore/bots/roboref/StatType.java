package twcore.bots.roboref;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import twcore.bots.roboref.ElimStats.StatData;

public enum StatType { 
    KILLS("fnKills", StatData.INT, false), DEATHS("fnDeaths", StatData.INT, false), MULTI_KILLS("fnMultiKills", StatData.INT, false), 
    KILL_STREAK("fnKillStreak", StatData.INT, true), DEATH_STREAK("fnDeathStreak", StatData.INT, true), WIN_STREAK("fnWinStreak", StatData.INT, true), 
    SHOTS("fnShots", StatData.INT, false), KILL_JOYS("fnKillJoys", StatData.INT, false), KNOCK_OUTS("fnKnockOuts", StatData.INT, false), 
    BEST_MULTI_KILL("fnTopMultiKill", StatData.INT, false), BEST_KILL_STREAK("fnTopKillStreak", StatData.INT, false), 
    WORST_DEATH_STREAK("fnTopDeathStreak", StatData.INT, false), BEST_WIN_STREAK("fnTopWinStreak", StatData.INT, false),
    AVE("fnAve", StatData.FLOAT, true), RATING("fnRating", StatData.INT, true), AIM("fnAim", StatData.DOUBLE, false), 
    WINS("fnWins", StatData.INT, false), GAMES("fnGames", StatData.INT, false), RANK("fnRank", StatData.INT, true);
    
    private String dbName;
    private boolean dbOnly;
    private StatData dataType;
    
    private static final Map<String, StatType> lookup = new HashMap<String, StatType>();
    
    static {
        for (StatType s : EnumSet.allOf(StatType.class))
            lookup.put(s.db(), s);
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