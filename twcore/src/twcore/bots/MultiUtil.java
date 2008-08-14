package twcore.bots;

import twcore.core.BotAction;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.*;

/**
 * MultiBot utility module.  (Based on TWBotExtension, but converted to
 * work with MultiBot's more advanced features.)
 */
public abstract class MultiUtil {
    protected BotAction m_botAction;
    protected OperatorList m_opList;
    protected SubspaceBot m_multiBot;
    protected TWScript m_twscript;
    
    /**
     * Creates a new instance of MultiUtil.
     */
    public MultiUtil() {
    }

    /**
     * Gives the util references to BotAction and ModuleEventRequester, runs
     * its init method, and requests appropriate events using ModuleEventRequester.   
     * @param action BotAction in use
     * @param opList OperatorList in use
     */
    public final void initialize( BotAction action, ModuleEventRequester modEventReq ){
        m_botAction = action;
        m_opList = action.getOperatorList();
        init();
        requestEvents(modEventReq);
    }
    
    /**
     * initialize() method for any TWScript module
     * Gives the util references to BotAction and ModuleEventRequester, runs
     * its init method, and requests appropriate events using ModuleEventRequester.
     * @param action BotAction in use
     * @param opList OperatorList in use
     */
    public final void initialize( BotAction action, ModuleEventRequester modEventReq, TWScript tws ){
    	m_botAction = action;
    	m_opList = action.getOperatorList();
    	m_twscript = tws;
    	init();
    	requestEvents(modEventReq);
    }

    /**
     * This method must be overridden to perform any functions that the
     * constructor would normally perform.
     */
    public abstract void init();

    /**
     * This method must be overriden to return the help message for the utility
     * when !help <utilname> is used.
     *
     * @return a string array containing the help message for the util
     */
    public abstract String[] getHelpMessages();

    /**
     * This method must be overridden to request all of the necessary events.
     * Note that this is done through ModuleEventRequester and not the standard
     * EventRequester.
     *
     * @param modEventReq is the ModuleEventRequester of the bot.
     */
    public abstract void requestEvents(ModuleEventRequester modEventReq);

    /**
     * This method should be overridden if any cleanup activites need to be
     * performed so that the module will terminate properly.
     */
    public void cancel()
    {
    }

    public final void handleEvent( SubspaceEvent event ){
        if( event instanceof ScoreReset )
            handleEvent( (ScoreReset)event );
        else if( event instanceof PlayerEntered )
            handleEvent( (PlayerEntered)event );
        else if( event instanceof Message )
            handleEvent( (Message)event );
        else if( event instanceof PlayerLeft )
            handleEvent( (PlayerLeft)event );
        else if( event instanceof WeaponFired )
            handleEvent( (WeaponFired)event );
        else if( event instanceof PlayerPosition )
            handleEvent( (PlayerPosition)event );
        else if( event instanceof PlayerDeath )
            handleEvent( (PlayerDeath)event );
        else if( event instanceof ScoreUpdate )
            handleEvent( (ScoreUpdate)event );
        else if( event instanceof FrequencyChange )
            handleEvent( (FrequencyChange)event );
        else if( event instanceof FrequencyShipChange )
            handleEvent( (FrequencyShipChange)event );
        else if( event instanceof ArenaJoined )
            handleEvent( (ArenaJoined)event );
        else if( event instanceof FileArrived )
            handleEvent( (FileArrived)event );
        else if( event instanceof FlagReward )
            handleEvent( (FlagReward)event );
        else if( event instanceof FlagVictory )
            handleEvent( (FlagVictory)event );
        else if( event instanceof LoggedOn )
            handleEvent( (LoggedOn)event );
        else if( event instanceof KotHReset )
            handleEvent( (KotHReset)event );
        else if( event instanceof Prize )
            handleEvent( (Prize)event );
        else if( event instanceof WatchDamage )
            handleEvent( (WatchDamage)event );
        else if( event instanceof SoccerGoal )
            handleEvent( (SoccerGoal)event );
        else if( event instanceof BallPosition )
            handleEvent( (BallPosition)event );
        else if( event instanceof FlagPosition )
            handleEvent( (FlagPosition)event );
        else if( event instanceof FlagDropped )
            handleEvent( (FlagDropped)event );
        else if( event instanceof FlagClaimed )
            handleEvent( (FlagClaimed)event );
        else if( event instanceof SQLResultEvent )
            handleEvent( (SQLResultEvent)event );
        else if( event instanceof TurretEvent )
        	handleEvent( (TurretEvent)event );
        else if( event instanceof PlayerBanner )
        	handleEvent( (PlayerBanner)event );
        else if( event instanceof ArenaList )
        	handleEvent( (ArenaList)event);
        else if( event instanceof TurfFlagUpdate )
            handleEvent( (TurfFlagUpdate)event);
    }
    public void handleEvent( ScoreReset event ){}
    public void handleEvent( PlayerEntered event ){}
    public void handleEvent( Message event ){}
    public void handleEvent( PlayerLeft event ){}
    public void handleEvent( PlayerPosition event ){}
    public void handleEvent( PlayerDeath event ){}
    public void handleEvent( ScoreUpdate event ){}
    public void handleEvent( WeaponFired event ){}
    public void handleEvent( FrequencyChange event ){}
    public void handleEvent( FrequencyShipChange event ){}
    public void handleEvent( ArenaJoined event ){}
    public void handleEvent( FileArrived event ){}
    public void handleEvent( FlagReward event ){}
    public void handleEvent( FlagVictory event ){}
    public void handleEvent( KotHReset event ) {}
    public void handleEvent( LoggedOn event ){}
    public void handleEvent( WatchDamage event ){}
    public void handleEvent( SoccerGoal event ){}
    public void handleEvent( Prize event ){}
    public void handleEvent( BallPosition event ){}
    public void handleEvent( FlagPosition event ){}
    public void handleEvent( FlagDropped event ){}
    public void handleEvent( FlagClaimed event ){}
    public void handleEvent( SQLResultEvent event ){}
    public void handleEvent( TurretEvent event ){}
    public void handleEvent( PlayerBanner event ){}
    public void handleEvent( ArenaList event ){}
    public void handleEvent( TurfFlagUpdate event ){}
}
