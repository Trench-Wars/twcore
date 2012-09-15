package twcore.bots.achievementbot;

import static twcore.bots.achievementbot.achievementbot.botAction;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerPosition;
import twcore.core.events.SubspaceEvent;
import twcore.core.game.Player;

/**
 * Class Location handles storing and validating required locations for
 * achievements in the PubAchievementModule.
 * 
 * @author spookedone
 */
public class Location extends Requirement {

    private int x = -1;                     //x coordinate
    private int y = -1;                     //y coordinate
    private int width = -1;                 //positive distance from x coord
    private int height = -1;                //positive distance from y coord
    private int minRange = -1;              //minimum distance from any two coords
    private int maxRange = -1;              //maximum distance from any two coords
    private int pX = -1;
    private int pY = -1;
    private boolean lastKnown = false;
    
    //private int debugCount = 0;

    /**
     * Default Constructor
     */
    public Location() {
        super(Type.location);
    }

    /**
     * Type constructor (for kill/death locations)
     * @param type
     */
    public Location(Type type) {
        super(type);
    }

    /**
     * Copy Constructor
     * @param location location to copy
     */
    public Location(Location location) {
        super(location);
        this.x = location.x;
        this.y = location.y;
        this.width = location.width;
        this.height = location.height;
        this.minRange = location.minRange;
        this.maxRange = location.maxRange;
    }

    /**
     * Validates this location.
     *
     * @return is valid position
     */
    private boolean validLocation() {
        boolean valid = false;

        if (debug) {
            System.out.println("[DEBUG] Location validation. " + "[pX:] " + pX +
                    "\t[pY:] " + pY); 
        }
        
        //player is on coordinate
        if (x != -1 && y != -1) {
            if (x == pX && y == pY) {
                valid = true;
            }
        }

        //player is within coordinate range
        if (minRange != -1 || maxRange != -1) {
            int absX = Math.abs(pX - x);
            int absY = Math.abs(pY - y);
            double distance = Math.sqrt(Math.pow(absX, 2) + Math.pow(absY, 2));


            //test minimum range
            if (minRange != -1) {
                if (distance >= minRange) {
                    valid = true;
                } else {
                    return false;
                }
            }

            //test maximum range
            if (maxRange != -1) {
                if (distance <= maxRange) {
                    valid = true;
                } else {
                    return false;
                }
            }
        }

        //player is within coordinate box
        if (width != -1 && height != -1) {
            if ((pX >= x && pX <= x + width) && (pY >= y && pY <= y + height)) {
                valid = true;
            } else {
                valid = false;
            }
        }

        return valid;
    }

    @Override
    public boolean update(Type type, SubspaceEvent event) {

        boolean valid = false;

        if (type == Type.location && this.type == Type.location) {
            PlayerPosition positionEvent = (PlayerPosition) event;
            pX = positionEvent.getXLocation();
            pY = positionEvent.getYLocation();
            valid = true;
        } else if ((type == Type.kill && this.type == Type.kill)
                || (type == Type.death && this.type == Type.death)) {
            PlayerDeath deathEvent = (PlayerDeath) event;
            Player killer = botAction.getPlayer(deathEvent.getKillerID());
            Player killee = botAction.getPlayer(deathEvent.getKilleeID());
            if (this.type == Type.kill) {
                pX = killee.getXLocation();
                pY = killee.getYLocation();
            } else if (this.type == Type.death) {
                pX = killer.getXLocation();
                pY = killer.getYLocation();
            }
            valid = true;
        }

        if (valid) {
            if (validLocation()) {
                completed = updateRequirements(type, event);
            } else {
                reset();
            }
        }

        return completed;
    }

    @Override
    public void reset() {
        pX = -1;
        pY = -1;
        this.completed = false;
        resetRequirements();
    }

    /**
     * Sets the x coordinate and isCoordinate to true
     * @param x the x coordinate to set
     */
    public void setX(int x) {
        this.x = x * 16;
    }

    /**
     * Sets the y coordinate and isCoordinate to true
     * @param y the y coordinate to set
     */
    public void setY(int y) {
        this.y = y * 16;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width * 16;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height * 16;
    }

    /**
     * Sets minimum range and isRange to true
     * @param minRange the minRange to set
     */
    public void setMinRange(int minRange) {
        this.minRange = minRange * 16;
    }

    /**
     * Sets maximum range and isRange to true
     * @param maxRange the maxRange to set
     */
    public void setMaxRange(int maxRange) {
        this.maxRange = maxRange * 16;
    }

    @Override
    public Requirement deepCopy() {
        return new Location(this);
    }
}
