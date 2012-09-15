package twcore.bots.achievement;

import twcore.core.events.SubspaceEvent;

/**
 * Abstract class ValueRequirement helps handle value based requirements for
 * achievements in the PubAchievementsModule.
 * 
 * @author spookedone
 */
public abstract class ValueRequirement extends Requirement {

    protected int current = 0;                //current value
    protected int minimum = -1;               //minimum value
    protected int maximum = -1;               //maximum value

    /**
     * Default Constructor
     * @param type type of requirement
     */
    public ValueRequirement(Type type) {
        super(type);
    }

    /**
     * Copy Constructor
     * @param valueRequirement the ValueRequirement class to copy
     */
    public ValueRequirement(ValueRequirement valueRequirement) {
        super(valueRequirement);
        this.exclusive = valueRequirement.exclusive;
        this.current = valueRequirement.current;
        this.minimum = valueRequirement.minimum;
        this.maximum = valueRequirement.maximum;
    }

    @Override
    public boolean update(Type type, SubspaceEvent event) {
        //test minimum
        if (minimum != -1 && current >= minimum) {
            this.completed = true;
        }

        //test maximum (flags calling method if over)
        if (maximum != -1) {
            if (current <= maximum) {
                this.completed = true;
            } else {
                this.reset();
            }
        }

        return completed;
    }

    @Override
    public void reset() {
        this.current = 0;
        this.completed = false;
        resetRequirements();
    }

    /**
     * Increases the current value
     */
    protected void increment() {
        this.current++;
    }

    /**
     * Decreases the current value
     */
    protected void decrement() {
        this.current--;
    }

    /**
     * @return the current
     */
    public int getCurrent() {
        return current;
    }

    /**
     * @param current the current to set
     */
    protected void setCurrent(int current) {
        this.current = current;
    }

    /**
     * @return the minimum
     */
    public int getMinimum() {
        return minimum;
    }

    /**
     * @param minimum the minimum to set
     */
    public void setMinimum(int minimum) {
        this.minimum = minimum;
    }

    /**
     * @return the maximum
     */
    public int getMaximum() {
        return maximum;
    }

    /**
     * @param maximum the maximum to set
     */
    public void setMaximum(int maximum) {
        this.maximum = maximum;
    }
}
