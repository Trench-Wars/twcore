package twcore.bots.twdt;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author WingZero
 */
public enum StatType {
    KILLS("fnKills"), 
    DEATHS("fnDeaths"), 
    TERR_KILLS("fnTerrKills"),
    TEAM_KILLS("fnTeamKills"),
    DOAS("fnDOAs"),
    MULTI_KILLS("fnMultiKills"), 
    KILL_STREAK("fnKillStreak"), 
    DEATH_STREAK("fnDeathStreak"),
    SHOTS("fnShots"), 
    BOMBS("fnBombs"),
    BURSTS("fnBursts"),
    REPELS("fnRepels"),
    FLAG_CLAIMS("fnFlagClaims"),
    KILL_JOYS("fnKillJoys"), 
    KNOCK_OUTS("fnKnockOuts"), 
    BEST_MULTI_KILL("fnTopMultiKill"), 
    BEST_KILL_STREAK("fnTopKillStreak"), 
    WORST_DEATH_STREAK("fnTopDeathStreak"),
    AIM("fnAim"),
    LAGOUTS("fnLagouts");
    
    private String dbName;
    
    private static final Map<String, StatType> lookup = new HashMap<String, StatType>();
    
    static {
        for (StatType s : EnumSet.allOf(StatType.class))
            lookup.put(s.db(), s);
    }
    
    private StatType(String db) {
        dbName = db;
    }
    
    public String db() {
        return dbName;
    }
}
