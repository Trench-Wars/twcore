package twcore.bots.achievementbot;

import twcore.core.events.SubspaceEvent;

/**
 * Time class handles the time requirements for achievements in the
 * PubAchievementsModule.
 * 
 * @author spookedone
 */
public class Time extends Requirement {

    private static volatile long current = 0;
    protected long minimum = -1;
    protected long maximum = -1;
    protected long currentMin = -1;
    protected long currentMax = -1;

    public Time() {
        super(Type.time);
    }

    public Time(Time time) {
        super(time);
        this.minimum = time.minimum;
        this.maximum = time.maximum;
        this.currentMin = time.currentMin;
        this.currentMax = time.currentMax;
    }

    public static void increment() {
        current++;
    }

    @Override
    public boolean update(Type type, SubspaceEvent event) {
        boolean valid = false;

        //test minimum
        if (minimum != -1 && current >= currentMin) {
            valid = true;
        }

        //test maximum (resets if over)
        if (maximum != -1) {
            if (current <= currentMax) {
                valid = true;
            } else {
                valid = false;
                reset();
            }
        }

        if (valid) {
            this.completed = updateRequirements(type, event);
        }

        return this.completed;
    }

    @Override
    public void reset() {
        if (minimum != -1) {
            currentMin = current + minimum;
        }
        if (maximum != -1) {
            currentMax = current + maximum;
        }
        this.completed = false;
        resetRequirements();
    }

    /**
     * @return the current
     */
    public static long getCurrent() {
        return current;
    }

    /**
     * @return the minimum
     */
    public long getMinimum() {
        return minimum;
    }

    /**
     * @param minimum the minimum to set
     */
    public void setMinimum(long minimum) {
        this.minimum = minimum;
        currentMin = 0 + minimum;
    }

    /**
     * @return the maximum
     */
    public long getMaximum() {
        return maximum;
    }

    /**
     * @param maximum the maximum to set
     */
    public void setMaximum(long maximum) {
        this.maximum = maximum;
        currentMax = 0 + maximum;
    }

    @Override
    public Requirement deepCopy() {
        return new Time(this);
    }
}
