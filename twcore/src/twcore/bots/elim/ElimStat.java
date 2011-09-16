package twcore.bots.elim;

import twcore.bots.elim.StatType;

/**
 * Single statistic for one elimination player.
 * @author WingZero
 */
public class ElimStat {

    protected StatType stat;    // Name of stat
    protected int iValue;       // Integer value if INT type
    protected double dValue;    // Double value if DOUBLE type
    protected float fValue;     // Float value if FLOAT type
    
    /**
     * Construct generic ElimStat with no value
     * @param name StatType of stat
     * @param data StatData type
     */
    public ElimStat(StatType name) {
        stat = name;
        iValue = 0;
        dValue = 0;
        fValue = 0;
    }
    
    /** Increment integer value stat. NOTE: Only works for integer types */
    public synchronized void increment() {
        if (stat.isInt())
            iValue++;
    }

    /** Decrement integer value stat. NOTE: Only works for integer types */
    public synchronized void decrement() {
        if (stat.isInt())
            iValue--;
    }
    
    /** Adds value to the current stat value. NOTE: Only works for integer types */
    public synchronized void add(int value) {
        iValue += value;
    }
    
    /** Set the integer value stat to value specified */
    public synchronized void setValue(int value) {
        iValue = value;
    }

    /** Set the double value stat to value specified */
    public synchronized void setValue(double value) {
        dValue = value;
    }

    /** Set the float value stat to value specified */
    public synchronized void setValue(float value) {
        fValue = value;
    }
    
    /** Returns the StatType for this stat */
    public StatType getStat() {
        return stat;
    }
    
    public Object get() {
        if (stat.isInt())
            return getInt();
        else if (stat.isDouble())
            return getDouble();
        else
            return getFloat();
    }
    
    /** Returns the integer stat value */
    public int getInt() {
        if (stat.isInt())
            return iValue;
        else if (stat.isDouble())
            return (int) dValue;
        else
            return (int) fValue;
    }
    
    /** Returns the double stat value */
    public double getDouble() {
        if (stat.isDouble())
            return dValue;
        else if (stat.isInt())
            return (double) iValue;
        else
            return (double) fValue;
    }
    
    /** Returns the float stat value */
    public float getFloat() {
        if (stat.isFloat())
            return fValue;
        else if (stat.isInt())
            return (float) iValue;
        else
            return (float) dValue;
    }
}
