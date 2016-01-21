package twcore.core.lag;

import twcore.core.events.*;

public abstract class EventListener {

    /**
        The constructor is protected to make it so that this class cannot be
        instantiated.
    */
    protected EventListener() {
    }

    /**
        This method distributes the events to the appropriate event handlers.

        @param event is the event to distribute.
    */
    public void handleEvent(SubspaceEvent event) {
        if (event instanceof Message)
            handleEvent((Message) event);
        else if (event instanceof InterProcessEvent)
            handleEvent((InterProcessEvent) event);
        else if (event instanceof PlayerLeft)
            handleEvent((PlayerLeft) event);
        else if (event instanceof PlayerDeath)
            handleEvent((PlayerDeath) event);
        else if (event instanceof PlayerEntered)
            handleEvent((PlayerEntered) event);
        else if (event instanceof FrequencyChange)
            handleEvent((FrequencyChange) event);
        else if (event instanceof FrequencyShipChange)
            handleEvent((FrequencyShipChange) event);
        else if (event instanceof ArenaList)
            handleEvent((ArenaList) event);
        else if (event instanceof PlayerPosition)
            handleEvent((PlayerPosition) event);
        else if (event instanceof Prize)
            handleEvent((Prize) event);
        else if (event instanceof ScoreUpdate)
            handleEvent((ScoreUpdate) event);
        else if (event instanceof WeaponFired)
            handleEvent((WeaponFired) event);
        else if (event instanceof LoggedOn)
            handleEvent((LoggedOn) event);
        else if (event instanceof FileArrived)
            handleEvent((FileArrived) event);
        else if (event instanceof ArenaJoined)
            handleEvent((ArenaJoined) event);
        else if (event instanceof FlagVictory)
            handleEvent((FlagVictory) event);
        else if (event instanceof FlagReward)
            handleEvent((FlagReward) event);
        else if (event instanceof ScoreReset)
            handleEvent((ScoreReset) event);
        else if (event instanceof WatchDamage)
            handleEvent((WatchDamage) event);
        else if (event instanceof SoccerGoal)
            handleEvent((SoccerGoal) event);
        else if (event instanceof BallPosition)
            handleEvent((BallPosition) event);
        else if (event instanceof FlagPosition)
            handleEvent((FlagPosition) event);
        else if (event instanceof FlagDropped)
            handleEvent((FlagDropped) event);
        else if (event instanceof FlagClaimed)
            handleEvent((FlagClaimed) event);
    }

    /*
        All of these stub functions handle the various events.  When this class
        is extended, these methods will be overridden to add functionality.
    */
    public void handleEvent(Message event) {
    }

    public void handleEvent(PlayerEntered event) {
    }

    public void handleEvent(ArenaList event) {
    }

    public void handleEvent(PlayerPosition event) {
    }

    public void handleEvent(PlayerLeft event) {
    }

    public void handleEvent(PlayerDeath event) {
    }

    public void handleEvent(Prize event) {
    }

    public void handleEvent(ScoreUpdate event) {
    }

    public void handleEvent(WeaponFired event) {
    }

    public void handleEvent(FrequencyChange event) {
    }

    public void handleEvent(FrequencyShipChange event) {
    }

    public void handleEvent(LoggedOn event) {
    }

    public void handleEvent(FileArrived event) {
    }

    public void handleEvent(ArenaJoined event) {
    }

    public void handleEvent(FlagVictory event) {
    }

    public void handleEvent(FlagReward event) {
    }

    public void handleEvent(ScoreReset event) {
    }

    public void handleEvent(WatchDamage event) {
    }

    public void handleEvent(SoccerGoal event) {
    }

    public void handleEvent(BallPosition event) {
    }

    public void handleEvent(FlagPosition event) {
    }

    public void handleEvent(FlagDropped event) {
    }

    public void handleEvent(FlagClaimed event) {
    }

    public void handleEvent(InterProcessEvent event) {
    }
}
