package twcore.bots.twdt;

/**
 * DraftStat represents and manipulates a single statistic for a player.
 * 
 * @author WingZero
 */
public class DraftStat {
    
    protected StatType type;
    protected int stat;
    
    public DraftStat(StatType statType) {
        type = statType;
        stat = 0;
    }
    
    /** Increment integer value stat. NOTE: Only works for integer types */
    public synchronized void increment() {
        stat++;
    }

    /** Decrement integer value stat. NOTE: Only works for integer types */
    public synchronized void decrement() {
        stat--;
    }
    
    /** Adds value to the current stat value. NOTE: Only works for integer types */
    public synchronized void add(int value) {
        stat += value;
    }
    
    /** Set the integer value stat to value specified */
    public synchronized void setValue(int value) {
        stat = value;
    }
    
    public int getValue() {
        return stat;
    }
}
