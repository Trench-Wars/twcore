package twcore.bots.achievementbot;

import static twcore.bots.achievementbot.achievementbot.botAction;

import twcore.core.events.PlayerDeath;
import twcore.core.events.SubspaceEvent;
import twcore.core.game.Player;

/**
 * @author spookedone
 */
public class Range extends ValueRequirement{

    public Range() {
        super(Type.range);
    }

    public Range(Range range) {
        super(range);
    }

    @Override
    public Requirement deepCopy() {
        return new Range(this);
    }

    @Override
    public boolean update(Type type, SubspaceEvent event) {

        if (event instanceof PlayerDeath) {
            PlayerDeath deathEvent = (PlayerDeath) event;
            Player killer = botAction.getPlayer(deathEvent.getKillerID());
            Player killee = botAction.getPlayer(deathEvent.getKilleeID());
            
            int tX = killee.getXLocation();
            int tY = killee.getYLocation();
            int pX = killer.getXLocation();
            int pY = killer.getYLocation();

            int absX = Math.abs(pX - tX);
            int absY = Math.abs(pY - tY);
            this.current = (int) Math.sqrt(Math.pow(absX, 2) + Math.pow(absY, 2));
        }

        return super.update(type, event);
    }

    @Override
    public void setMinimum(int minimum) {
        this.minimum = minimum * 16;
    }

    @Override
    public void setMaximum(int maximum) {
        this.maximum = maximum * 16;
    }
}
