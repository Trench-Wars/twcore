package twcore.bots.achievementbot;

import static twcore.bots.achievementbot.achievementbot.botAction;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.PlayerDeath;
import twcore.core.events.SubspaceEvent;

/**
 * @author spookedone
 */
public class Ship extends Requirement {

    private int required = -1;
    private int current = -1;

    /**
     * Default Constructor
     * @param type type that defines ship (ship, kill, or death)
     */
    public Ship(Type type) {
        super(type);
    }

    /**
     * Copy Constructor
     * @param ship ship requirement to copy
     */
    public Ship(Ship ship) {
        super(ship);
        this.required = ship.required;
    }

    @Override
    public boolean update(Type type, SubspaceEvent event) {
        
        if (type == Type.ship && this.type == Type.ship) {
            FrequencyShipChange shipChange = (FrequencyShipChange) event;
            current = shipChange.getShipType();
        } else if (type == Type.kill && this.type == Type.kill) {
            PlayerDeath playerDeath = (PlayerDeath) event;
            current = botAction.getPlayer(playerDeath.getKilleeID()).getShipType();
        } else if (type == Type.death && this.type == Type.death) {
            PlayerDeath playerDeath = (PlayerDeath) event;
            current = botAction.getPlayer(playerDeath.getKillerID()).getShipType();
        }

        if (current == required) {
            completed = updateRequirements(type, event);
        } else {
            reset();
        }

        return completed;
    }

    @Override
    public void reset() {
        completed = false;
        resetRequirements();
    }

    /**
     * Sets required type of ship.
     * @param requiredType
     */
    public void setType(int requiredType) {
        this.required = requiredType;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    @Override
    public Requirement deepCopy() {
        return new Ship(this);
    }
}
