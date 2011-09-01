package twcore.bots.twdt;

import java.util.EnumMap;
import java.util.HashMap;

import twcore.bots.twdt.StatType;

/**
 *
 * @author WingZero
 */
public class DraftStats {

    private HashMap<Integer, EnumMap<StatType, DraftStat>> ships;
    private EnumMap<StatType, DraftStat> stats;
    private int ship;
    
    public DraftStats(int ship) {
        setShip(ship);
    }
    
    public void setShip(int toShip) {
        stats = ships.get(toShip);
        if (stats == null) {
            stats = new EnumMap<StatType, DraftStat>(StatType.class);
            for (StatType stat : StatType.values())
                stats.put(stat, new DraftStat(stat));
            ships.put(ship, stats);
        }
        ship = toShip;
    }
    
    public int getShip() {
        return ship;
    }
    
    public DraftStat getStat(StatType stat) {
        return stats.get(stat);
    }
    
    public void setStat(StatType stat, int value) {
        stats.get(stat).setValue(value);
    }
    
    
    
}
