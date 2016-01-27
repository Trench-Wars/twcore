package twcore.bots;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.util.ModuleEventRequester;
import twcore.core.OperatorList;
import twcore.core.events.*;

public abstract class MultiModule
{
    public static final int FIRST_CHAT = 1;
    private static final String TRUE_STRING = "true";

    public BotAction m_botAction;
    public OperatorList opList;
    public BotSettings moduleSettings;
    public boolean autoStart = false;

    /**
        This method initializes the bot module.  This must be called before the
        module may be used.

        @param botAction is the botAction of the bot that the module is loaded into.
        @param moduleSettings BotSettings object, this holds the .cfg info of the bot
        @param modEventReq the event requester object ModuleEventRequester
    */
    public void initialize(BotAction botAction, BotSettings moduleSettings, ModuleEventRequester modEventReq)
    {
        if(botAction == null)
            throw new IllegalArgumentException("Invalid BotAction instance.");

        if(moduleSettings == null)
            throw new IllegalArgumentException("Invalid Module Settings.");

        m_botAction = botAction;
        this.moduleSettings = moduleSettings;
        opList = m_botAction.getOperatorList();
        init();
        requestEvents(modEventReq);
    }

    /**
        This method gets the name of the module from the configuration file.

        @return the name of the module is returned.
    */
    public String getModuleName()
    {
        try
        {
            return moduleSettings.getString("modulename");
        }
        catch(Exception e)
        {
            return "Module Name Not Specified";
        }
    }

    /**
        This method gets the version of the module from the configuration file.

        @return the version of the module is returned.
    */
    public String getVersion()
    {
        try
        {
            return moduleSettings.getString("version");
        }
        catch(Exception e)
        {
            return "Version Not Specified";
        }
    }

    /**
        This method cheks to see if exceptions by events are to be rethrown back to
        the multibot.

        @return true is returned if the exception is to be rethrown.
    */
    public boolean isExceptionRethrown()
    {
        try
        {
            String boolString = moduleSettings.getString("rethrowexception");
            return boolString.equalsIgnoreCase(TRUE_STRING);
        }
        catch(Exception e)
        {
            return true;
        }
    }

    /**
        This method should be overridden if any cleanup activites need to be
        performed so that the module will terminate properly.
    */
    public abstract void cancel();

    /**
        This method should be overridden to perform any functions that the
        constructor would normally perform.
    */
    public abstract void init();

    /**
        This method should be overridden to request all of the necessary events.

        @param eventRequester is the EventRequester of the bot.
    */
    public abstract void requestEvents(ModuleEventRequester eventRequester);

    /**
        This method gets the help message for the module.

        @return a string array containing the help message for the module is
        returned.
    */
    public abstract String[] getModHelpMessage();

    /**
        This method lets the module know whether to automatically start or wait for
        more commands. It defaults as false.
        @param auto
        true - automatically start, false - await commands
    */
    public void autoStart(boolean auto) {
        autoStart = auto;
    }

    /**
        This method returns if the module can be unloaded.

        @return true is returned if the module can be safely unloaded.
    */
    public abstract boolean isUnloadable();

    /**
        This method distributes the events to the appropriate event handlers.  If
        any of the events throw an exception, the exception is handled by
        displaying it in the first chat.  A stack trace is also outputted.

        @param event is the event to distribute.
        @throws Exception if isExceptionRethrown is set to true then the module
        will rethrow the exception to be handled elsewhere.
    */

    public void handleEvent(SubspaceEvent event) throws Exception
    {
        try
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
            else if(event instanceof WeaponFired)
                handleEvent((WeaponFired) event);
            else if(event instanceof PlayerPosition)
                handleEvent((PlayerPosition) event);
            else if(event instanceof Prize)
                handleEvent((Prize) event);
            else if(event instanceof ScoreUpdate)
                handleEvent((ScoreUpdate) event);
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
            else if(event instanceof TurretEvent)
                handleEvent((TurretEvent) event);
            else if(event instanceof SQLResultEvent)
                handleEvent((SQLResultEvent) event);
            else if(event instanceof PlayerBanner)
                handleEvent((PlayerBanner) event);
        }
        catch(Exception e)
        {
            StackTraceElement[] stackTrace = e.getStackTrace();
            String eventName = event.getClass().getName();

            m_botAction.sendChatMessage(FIRST_CHAT, "Error in module: " + getModuleName() + " when handling a " + eventName + " event:" + e.getMessage() );
            m_botAction.sendChatMessage(FIRST_CHAT, "Stack Trace:");

            for(int index = 0; index < stackTrace.length; index++)
                m_botAction.sendChatMessage(FIRST_CHAT, stackTrace[index].toString());

            if(isExceptionRethrown())
                throw e;
        }
    }

    /*
        All of these stub methods handle the various events (default being doing nothing).
    */

    public void handleEvent(Message event) {}

    public void handleEvent(PlayerEntered event) {}

    public void handleEvent(ArenaList event) {}

    public void handleEvent(PlayerPosition event) {}

    public void handleEvent(PlayerLeft event) {}

    public void handleEvent(PlayerDeath event) {}

    public void handleEvent(Prize event) {}

    public void handleEvent(ScoreUpdate event) {}

    public void handleEvent(WeaponFired event) {}

    public void handleEvent(FrequencyChange event) {}

    public void handleEvent(FrequencyShipChange event) {}

    public void handleEvent(LoggedOn event) {}

    public void handleEvent(FileArrived event) {}

    public void handleEvent(ArenaJoined event) {}

    public void handleEvent(FlagVictory event) {}

    public void handleEvent(FlagReward event) {}

    public void handleEvent(ScoreReset event) {}

    public void handleEvent(WatchDamage event) {}

    public void handleEvent(SoccerGoal event) {}

    public void handleEvent(BallPosition event) {}

    public void handleEvent(FlagPosition event) {}

    public void handleEvent(FlagDropped event) {}

    public void handleEvent(FlagClaimed event) {}

    public void handleEvent(InterProcessEvent event) {}

    public void handleEvent(TurretEvent event) {}

    public void handleEvent(SQLResultEvent event) {}

    public void handleEvent(PlayerBanner event) {}

}
