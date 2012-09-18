package twcore.bots.achievementbot;

import twcore.core.events.FlagClaimed;
import twcore.core.events.SubspaceEvent;

/**
 *
 * @author SpookedOne
 */
public class FlagClaim extends ValueRequirement {
    /**
     * Default Constructor
     */
    public FlagClaim() {
        super(Type.flagclaim);
    }

    public FlagClaim(FlagClaim flagClaim) {
        super(flagClaim);
    }

    @Override
    public boolean update(Type type, SubspaceEvent event) {
        if (event instanceof FlagClaimed) {
            if (this.type == type) {
                current++;
            }
        }

        return super.update(type, event);
    }

    @Override
    public Requirement deepCopy() {
        return new FlagClaim(this);
    }
}
