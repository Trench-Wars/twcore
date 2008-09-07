package twcore.core;

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
import twcore.core.events.KotHReset;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerBanner;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.Prize;
import twcore.core.events.SQLResultEvent;
import twcore.core.events.ScoreReset;
import twcore.core.events.ScoreUpdate;
import twcore.core.events.SoccerGoal;
import twcore.core.events.SubspaceEvent;
import twcore.core.events.TurfFlagUpdate;
import twcore.core.events.TurretEvent;
import twcore.core.events.WatchDamage;
import twcore.core.events.WeaponFired;
import twcore.core.util.Tools;

/**
 * Basic abstract class that all Subspace bot classes must extend.  Includes
 * default handling for all events.  If a bot requests an event but doesn't
 * override the handling, the default behavior here will generate an event
 * error on the system console.
 */
public abstract class SubspaceBot {

    public BotAction      m_botAction;              // Held reference to BotAction

    /**
     * Create a new instance of SubspaceBot.
     *
     * Override this constructor so that your bot can hold a reference of BotAction.
     * @param botAction BotAction reference
     */
    public SubspaceBot( BotAction botAction ){
        m_botAction = botAction;
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( SubspaceEvent event ){

        Tools.printLog( m_botAction.getBotName() + ": Unknown event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( WatchDamage event ){

        Tools.printLog( m_botAction.getBotName() + ": WatchDamage event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( ScoreReset event ){

        Tools.printLog( m_botAction.getBotName() + ": ScoreReset event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( PlayerEntered event ){

        Tools.printLog( m_botAction.getBotName() + ": PlayerEntered event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( Message event ){

        Tools.printLog( m_botAction.getBotName() + ": PublicMessage event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( PlayerLeft event ){

        Tools.printLog( m_botAction.getBotName() + ": PlayerLeft event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( PlayerPosition event ){

        Tools.printLog( m_botAction.getBotName() + ": PlayerPosition event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( PlayerDeath event ){

        Tools.printLog( m_botAction.getBotName() + ": PlayerDeath event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( ScoreUpdate event ){

        Tools.printLog( m_botAction.getBotName() + ": ScoreUpdate event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( WeaponFired event ){

        Tools.printLog( m_botAction.getBotName() + ": WeaponFired event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( FrequencyChange event ){

        Tools.printLog( m_botAction.getBotName() + ": ChangeFrequency event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( FrequencyShipChange event ){

        Tools.printLog( m_botAction.getBotName() + ": ChangeFreqAndShip event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( ArenaJoined event ){
        Tools.printLog( m_botAction.getBotName() + ": ArenaJoined event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( FileArrived event ){
        Tools.printLog( m_botAction.getBotName() + ": FileArrived event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( FlagReward event ){
        Tools.printLog( m_botAction.getBotName() + ": FlagReward event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( FlagVictory event ){
        Tools.printLog( m_botAction.getBotName() + ": FlagVictory event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( FlagPosition event ){
        Tools.printLog( m_botAction.getBotName() + ": FlagPosition event ignore" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( FlagClaimed event ) {
        Tools.printLog( m_botAction.getBotName() + ": FlagClaimed event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( FlagDropped event ){
        Tools.printLog( m_botAction.getBotName() + ": FlagDropped event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( SoccerGoal event ) {
        Tools.printLog( m_botAction.getBotName() + ": SoccerGoal event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( KotHReset event ) {
        Tools.printLog( m_botAction.getBotName() + ": KotHReset event not handled; ignored");
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( BallPosition event ) {
        Tools.printLog( m_botAction.getBotName() + ": BallPosition event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( Prize event ) {
        Tools.printLog( m_botAction.getBotName() + ": Prize event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( LoggedOn event ){

        Tools.printLog( m_botAction.getBotName() + ": Logon not handled.  Joining #robopark" );
        
        // Join default arena
        m_botAction.joinArena( "#robopark" );
        
        // Identify as bot on the operatorlist
        m_botAction.getOperatorList().makeBot(m_botAction.getBotName());
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( ArenaList event ){

        Tools.printLog( m_botAction.getBotName() + ": ArenaList event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( InterProcessEvent event ){

        Tools.printLog( m_botAction.getBotName() + ": InterProcess event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( SQLResultEvent event ){
        Tools.printLog( m_botAction.getBotName() + ": SQLResultEvent event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( PlayerBanner event ){
        Tools.printLog( m_botAction.getBotName() + ": PlayerBanner event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( TurretEvent event ){
        Tools.printLog( m_botAction.getBotName() + ": TurretEvent event not handled; ignored" );
    }

    /**
     * Request this event from EventRequester and override this method in your bot to
     * handle this event (packet).  If you request this event and do not handle it,
     * the default behavior defined here will be executed.  If you attempt to handle
     * the event without requesting it from EventRequester, it will not be handled.
     * @param event Event object of handled event (packet)
     */
    public void handleEvent(TurfFlagUpdate event) {
    		Tools.printLog(m_botAction.getBotName() + ": TurfFlagUpdate event not handled; ignored");
    }

    /**
     * Default disconnection behavior (do nothing).  This method is called from
     * Session's disconnect() when the bot has been asked to die.  Override this
     * method to include custom disconnection behavior.  It is recommended if
     * your bot makes any connections to outside sources that may be left open
     * after the bot dies, that they be closed using this method.
     */
    public void handleDisconnect(){
    }

    /**
     * Called regularly on every bot when !smartshutdown is executed to determine whether or
     * not the bot can be disconnected.  If returning true, it is assumed the bot is not in
     * the middle of any critical behavior, such as running a game, and can be safely DC'd.
     * The smart shutdown will use this method to determine whether it can disconnect a bot,
     * and will wait until all bots have returned true before shutting down the core.
     * @return True if the bot is not running a game or function and can be disconnected
     */
    public boolean isIdle() {
        return true;
    }

}
