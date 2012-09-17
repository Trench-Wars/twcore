package twcore.bots.achievementbot;

import twcore.core.events.PlayerDeath;
import twcore.core.events.SubspaceEvent;

/**
 * KillDeath Class handles storing and validating required kills or deaths for
 * achievements in the pubsystem's pubachievementmodule.
 * 
 * @author spookedone
 */
public class KillDeath extends ValueRequirement {

    /**
     * Default Constructor
     * @param type type of requirement this is (kill or death)
     */
    public KillDeath(Type type) {
        super(type);
    }

    public KillDeath(KillDeath killDeath) {
        super(killDeath);
    }

    @Override
    public boolean update(Type type, SubspaceEvent event) {
        if (event instanceof PlayerDeath) {
            //boolean valid = updateRequirements(type, event);

            if (this.type == type) {
                current++;
            }
        }

        return super.update(type, event);
    }

    @Override
    public Requirement deepCopy() {
        return new KillDeath(this);
    }
}
