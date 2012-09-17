package twcore.bots.achievementbot;

import java.util.LinkedList;
import java.util.List;
import twcore.core.events.SubspaceEvent;

/**
 * Abstract requirement for achievement in PubAchievementsModule.
 * @author spookedone
 */
public abstract class Requirement {

    protected boolean completed = false;    //flag for completed requirement
    protected boolean exclusive = false;    //flag for exclusive requirement
    protected Type type;                    //type of requirement
    protected List<Requirement> requirements;
    
    /**
     * Requirement Types
     */
    public enum Type {

        kill, death, location, time, flagclaim, flagtime, prize, ship, range;

        /**
         * Returns bit masked value of enumeration
         * @return value bit mask
         */
        public int value() {
            return 1 << ordinal();
        }
    };

    /**
     * Hidden default class constructor
     */
    private Requirement() {
    }

    /**
     * Type class constructor
     * @param type type of requirement
     */
    protected Requirement(Type type) {
        this.type = type;
        requirements = new LinkedList<Requirement>(); 
    }

    /**
     * Copy class constructor
     * @param requirement requirement to copy
     */
    protected Requirement(Requirement requirement) {
        this.completed = requirement.completed;
        this.exclusive = requirement.exclusive;
        this.type = requirement.type;
        this.requirements = new LinkedList<Requirement>();
        for (Requirement r : requirement.requirements) {
            this.requirements.add(r.deepCopy());
        }
    }

    public abstract Requirement deepCopy();

    /**
     * Update the Requirement with the subspace event, returns true if
     * requirement is completed
     * @param event the subspace event
     * @return completed
     */
    public abstract boolean update(Type type, SubspaceEvent event);

    /**
     * Resets the requirement state to default.
     */
    public abstract void reset();

    /**
     * Return the type of Requirement this is.
     * @return requirement type
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Returns if requirement is completed.
     * @return completed
     */
    public boolean isComplete() {
        return completed;
    }

    /**
     * Sets the requirement in exclusive validation state
     * @param exclusive
     */
    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    /**
     * Add addition sub requirements
     * @param requirement
     */
    public void addRequirement(Requirement requirement) {
        requirements.add(requirement);
    }

    /**
     * Updates sub requirements and returns all complete
     * @param event Subspace event
     * @return all completed
     */
    protected boolean updateRequirements(Type type, SubspaceEvent event) {
        boolean valid = true;
        for (Requirement r : requirements) {
            if (!r.isComplete()) {
                if (!r.update(type, event)) {
                    valid = false;
                }
            }
        }
        return valid;
    }

    /**
     * Resets all sub requirements of this requirement
     */
    protected void resetRequirements() {
        for (Requirement r : requirements) {
            r.reset();
        }
    }
}
