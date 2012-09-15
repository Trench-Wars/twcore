package twcore.bots.achievement;

import java.util.LinkedList;
import java.util.List;
import twcore.bots.achievement.Requirement.Type;
import twcore.core.events.SubspaceEvent;

/**
 * Achievement Class handles storing and validating player achievements in the
 * PubAchievementsModule.
 *
 * @author spookedone
 */
public final class Achievement {

    private final int id;                       //id used by database
    private String name;
    private String description;
    private int typeMask = 0;
    private boolean completed = false;
    private List<Requirement> requirements =
            new LinkedList<Requirement>();

    ;

    /*
     * Hidden Default Class Constructor
     */
    private Achievement() {
        this.id = -1;
    }

    /**
     * Constructor for creating achievement with id.
     * @param id id for achievement
     */
    public Achievement(int id) {
        this.id = id;
    }

    /**
     * Copy Constructor
     * @param achievement
     */
    public Achievement(Achievement achievement) {
        this.id = achievement.id;
        this.name = achievement.name;
        this.description = achievement.description;
        this.typeMask = achievement.typeMask;
        for (Requirement r : achievement.requirements) {
            requirements.add(r.deepCopy());
        }
    }

    /**
     * Updates the requirements for achievement of this type and verifies in any achievements completed
     * @param type
     * @param event
     */
    public boolean update(Type type, SubspaceEvent event) {
        boolean valid = false;
        if ((typeMask & type.value()) == type.value() && !completed) {
            valid = updateRequirements(type, event);
            completed = valid;
        }
        return valid;
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
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public int getTypeMask() {
        return typeMask;
    }

    public void setTypeMask(int typeMask) {
        this.typeMask = typeMask;
    }

    public void reset() {
        this.completed = false;
        for (Requirement r : requirements) {
            r.reset();
        }
    }

    public void setComplete(boolean completed) {
        this.completed = completed;
    }

    public boolean isComplete() {
        return completed;
    }

    public void addRequirement(Requirement requirement) {
        requirements.add(requirement);
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }
}
