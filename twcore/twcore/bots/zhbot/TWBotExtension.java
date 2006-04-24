package twcore.bots.zhbot;

import twcore.core.*;

/*
 * PortabotExtension.java
 *
 * Created on March 21, 2002, 2:46 PM
 */

/**
 *
 * @author  harvey
 */
public abstract class TWBotExtension {
    BotAction m_botAction;
    OperatorList m_opList;
    SubspaceBot m_twBot;
    /** Creates a new instance of PortabotExtension */
    public TWBotExtension() {
    }
    
    public final void set( BotAction action, OperatorList opList, SubspaceBot twBot ){
        m_botAction = action;
        m_opList = opList;
        m_twBot = twBot;
    }
    
    public abstract String[] getHelpMessages();
    public abstract void cancel();
    
    private final void sendBotCommand( String name, String message ){
  //      m_twBot.handleCommand( name, message );
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
}
