package twcore.core;

public abstract class SubspaceBot {

    public BotAction      m_botAction;
    public int            m_requestedEvents = 0;

    public SubspaceBot( BotAction botAction ){
        m_botAction = botAction;
    }

    public void handleEvent( SubspaceEvent event ){

        Tools.printLog( m_botAction.getBotName() + ": Unknown event not handled; ignored" );
    }

    public void handleEvent( WatchDamage event ){

        Tools.printLog( m_botAction.getBotName() + ": WatchDamage event not handled; ignored" );
    }

    public void handleEvent( ScoreReset event ){

        Tools.printLog( m_botAction.getBotName() + ": ScoreReset event not handled; ignored" );
    }

    public void handleEvent( PlayerEntered event ){

        Tools.printLog( m_botAction.getBotName() + ": PlayerEntered event not handled; ignored" );
    }

    public void handleEvent( Message event ){

        Tools.printLog( m_botAction.getBotName() + ": PublicMessage event not handled; ignored" );
    }

    public void handleEvent( PlayerLeft event ){

        Tools.printLog( m_botAction.getBotName() + ": PlayerLeft event not handled; ignored" );
    }

    public void handleEvent( PlayerPosition event ){

        Tools.printLog( m_botAction.getBotName() + ": PlayerPosition event not handled; ignored" );
    }

    public void handleEvent( PlayerDeath event ){

        Tools.printLog( m_botAction.getBotName() + ": PlayerDeath event not handled; ignored" );
    }

    public void handleEvent( ScoreUpdate event ){

        Tools.printLog( m_botAction.getBotName() + ": ScoreUpdate event not handled; ignored" );
    }

    public void handleEvent( WeaponFired event ){

        Tools.printLog( m_botAction.getBotName() + ": WeaponFired event not handled; ignored" );
    }

    public void handleEvent( FrequencyChange event ){

        Tools.printLog( m_botAction.getBotName() + ": ChangeFrequency event not handled; ignored" );
    }

    public void handleEvent( FrequencyShipChange event ){

        Tools.printLog( m_botAction.getBotName() + ": ChangeFreqAndShip event not handled; ignored" );
    }

    public void handleEvent( ArenaJoined event ){
        Tools.printLog( m_botAction.getBotName() + ": ArenaJoined event not handled; ignored" );
    }

    public void handleEvent( FileArrived event ){
        Tools.printLog( m_botAction.getBotName() + ": FileArrived event not handled; ignored" );
    }

    public void handleEvent( FlagReward event ){
        Tools.printLog( m_botAction.getBotName() + ": FlagReward event not handled; ignored" );
    }

    public void handleEvent( FlagVictory event ){
        Tools.printLog( m_botAction.getBotName() + ": FlagVictory event not handled; ignored" );
    }
    
    public void handleEvent( FlagPosition event ){
        Tools.printLog( m_botAction.getBotName() + ": FlagPosition event ignore" );
    }

    public void handleEvent( FlagClaimed event ) {
        Tools.printLog( m_botAction.getBotName() + ": FlagClaimed event not handled; ignored" );
    }

    public void handleEvent( FlagDropped event ){
        Tools.printLog( m_botAction.getBotName() + ": FlagDropped event not handled; ignored" );
    }  
    
    public void handleEvent( SoccerGoal event ) {
        Tools.printLog( m_botAction.getBotName() + ": SoccerGoal event not handled; ignored" );
    }

    public void handleEvent( BallPosition event ) {
        Tools.printLog( m_botAction.getBotName() + ": BallPosition event not handled; ignored" );
    }
    
    public void handleEvent( Prize event ) {
        Tools.printLog( m_botAction.getBotName() + ": Prize event not handled; ignored" );
    }

    public void handleEvent( LoggedOn event ){

        Tools.printLog( m_botAction.getBotName() + ": Logon not handled.  Joining #robopark" );
        m_botAction.joinArena( "#robopark" );
    }
    
    public void handleEvent( ArenaList event ){

        Tools.printLog( m_botAction.getBotName() + ": ArenaList event not handled; ignored" );
    }

    public void handleEvent( InterProcessEvent event ){    

        Tools.printLog( m_botAction.getBotName() + ": InterProcess event not handled; ignored" );
    }
    
    public void handleEvent( SQLResultEvent event ){
        Tools.printLog( m_botAction.getBotName() + ": SQLResultEvent event not handled; ignored" );
    }
    
    public void handleEvent( PlayerBanner event ){
        Tools.printLog( m_botAction.getBotName() + ": PlayerBanner event not handled; ignored" );
    }

    public void handleEvent( TurretEvent event ){
        Tools.printLog( m_botAction.getBotName() + ": TurretEvent event not handled; ignored" );
    }
    
    public void handleDisconnect(){
    }

}

