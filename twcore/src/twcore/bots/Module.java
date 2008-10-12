package twcore.bots;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.events.ArenaJoined;
import twcore.core.events.ArenaList;
import twcore.core.events.BallPosition;
import twcore.core.events.FileArrived;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FlagDropped;
import twcore.core.events.FlagPosition;
import twcore.core.events.FlagReward;
import twcore.core.events.FlagVictory;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerBanner;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.Prize;
import twcore.core.events.ScoreReset;
import twcore.core.events.ScoreUpdate;
import twcore.core.events.SoccerGoal;
import twcore.core.events.SubspaceEvent;
import twcore.core.events.WatchDamage;
import twcore.core.events.WeaponFired;

public abstract class Module
{
    protected BotAction m_botAction;
    protected OperatorList opList;

    public Module()
    {
    }

    /**
     * This method initializes the module to default values.
     */

    public void initializeModule(BotAction botAction)
    {
        m_botAction = botAction;
        opList = m_botAction.getOperatorList();
        requestEvents(m_botAction.getEventRequester());
        initializeModule();
    }

    /**
     * This method distributes the events to the appropriate event handlers.
     *
     * @param event is the event to distribute.
     */

    public void handleEvent(SubspaceEvent event)
    {
        if(event instanceof Message)
            handleEvent((Message) event);
        else if(event instanceof InterProcessEvent)
            handleEvent((InterProcessEvent) event);
        else if(event instanceof PlayerLeft)
            handleEvent((PlayerLeft) event);
        else if(event instanceof PlayerDeath)
            handleEvent((PlayerDeath) event);
        else if(event instanceof PlayerEntered)
            handleEvent((PlayerEntered) event);
        else if(event instanceof FrequencyChange)
            handleEvent((FrequencyChange) event);
        else if(event instanceof FrequencyShipChange)
            handleEvent((FrequencyShipChange) event);
        else if(event instanceof ArenaList)
            handleEvent((ArenaList) event);
        else if(event instanceof PlayerPosition)
            handleEvent((PlayerPosition) event);
        else if(event instanceof Prize)
            handleEvent((Prize) event);
        else if(event instanceof ScoreUpdate)
            handleEvent((ScoreUpdate) event);
        else if(event instanceof WeaponFired)
            handleEvent((WeaponFired) event);
        else if(event instanceof LoggedOn)
            handleEvent((LoggedOn) event);
        else if(event instanceof FileArrived)
            handleEvent((FileArrived) event);
        else if(event instanceof ArenaJoined)
            handleEvent((ArenaJoined) event);
        else if(event instanceof FlagVictory)
            handleEvent((FlagVictory) event);
        else if(event instanceof FlagReward)
            handleEvent((FlagReward) event);
        else if(event instanceof ScoreReset)
            handleEvent((ScoreReset) event);
        else if(event instanceof WatchDamage)
            handleEvent((WatchDamage) event);
        else if(event instanceof SoccerGoal)
            handleEvent((SoccerGoal) event);
        else if(event instanceof BallPosition)
            handleEvent((BallPosition) event);
        else if(event instanceof FlagPosition)
            handleEvent((FlagPosition) event);
        else if(event instanceof FlagDropped)
            handleEvent((FlagDropped) event);
        else if(event instanceof FlagClaimed)
            handleEvent((FlagClaimed) event);
        else if(event instanceof PlayerBanner)
            handleEvent((PlayerBanner) event);
    }

    /**
     * All of these stub functions handle the various events.
     */

    public void handleEvent(Message event){}

    public void handleEvent(PlayerEntered event){}

    public void handleEvent(ArenaList event){}

    public void handleEvent(PlayerPosition event){}

    public void handleEvent(PlayerLeft event){}

    public void handleEvent(PlayerDeath event){}

    public void handleEvent(Prize event){}

    public void handleEvent(ScoreUpdate event){}

    public void handleEvent(WeaponFired event){}

    public void handleEvent(FrequencyChange event){}

    public void handleEvent(FrequencyShipChange event){}

    public void handleEvent(LoggedOn event){}

    public void handleEvent(FileArrived event){}

    public void handleEvent(ArenaJoined event){}

    public void handleEvent(FlagVictory event){}

    public void handleEvent(FlagReward event){}

    public void handleEvent(ScoreReset event){}

    public void handleEvent(WatchDamage event){}

    public void handleEvent(SoccerGoal event){}

    public void handleEvent(BallPosition event){}

    public void handleEvent(FlagPosition event){}

    public void handleEvent(FlagDropped event){}

    public void handleEvent(FlagClaimed event){}
    
    public void handleEvent(PlayerBanner event){}

    public void handleEvent(InterProcessEvent event){}

    public abstract void initializeModule();

    public abstract void cancel();

    public abstract void requestEvents(EventRequester eventRequester);
}