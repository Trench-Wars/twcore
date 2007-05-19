package twcore.bots.sbbot;
import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
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
import twcore.core.events.WatchDamage;
import twcore.core.events.WeaponFired;

/**
 * This class will interpret all the low-level events and notify all interested listeners.
 */
public abstract class SSEventForwarder extends SubspaceBot {
    protected SSEventOperator operator;

    public static final SSEventMessageType
	SUBSPACEEVENT = new SSEventMessageType(),
	WATCHDAMAGE = new SSEventMessageType(),
	SCORERESET = new SSEventMessageType(),
	PLAYERENTERED = new SSEventMessageType(),
	MESSAGE = new SSEventMessageType(),
	PLAYERLEFT = new SSEventMessageType(),
	PLAYERPOSITION = new SSEventMessageType(),
	PLAYERDEATH = new SSEventMessageType(),
	SCOREUPDATE = new SSEventMessageType(),
	WEAPONFIRED = new SSEventMessageType(),
	FREQUENCYCHANGE = new SSEventMessageType(),
	FREQUENCYSHIPCHANGE = new SSEventMessageType(),
	ARENAJOINED = new SSEventMessageType(),
	FILEARRIVED = new SSEventMessageType(),
	FLAGREWARD = new SSEventMessageType(),
	FLAGVICTORY = new SSEventMessageType(),
	FLAGPOSITION = new SSEventMessageType(),
	FLAGCLAIMED = new SSEventMessageType(),
	FLAGDROPPED = new SSEventMessageType(),
	SOCCERGOAL = new SSEventMessageType(),
	BALLPOSITION = new SSEventMessageType(),
	PRIZE = new SSEventMessageType(),
	LOGGEDON = new SSEventMessageType(),
	ARENALIST = new SSEventMessageType(),
	INTERPROCESSEVENT = new SSEventMessageType(),
	SQLRESULTEVENT = new SSEventMessageType();

    protected EventRequester events;

    public SSEventForwarder( BotAction botAction) {
	super(botAction);
	events = m_botAction.getEventRequester();
	events.requestAll();
	operator = new SSEventOperator();
    }

    protected abstract SSEventOperator getOperator();

    public void handleEvent( SubspaceEvent event ){
	getOperator().notifyEvent(SUBSPACEEVENT,event);
    }

    public void handleEvent( WatchDamage event ) {
	getOperator().notifyEvent(WATCHDAMAGE,event);
    }

    public void handleEvent( ScoreReset event ) {
	getOperator().notifyEvent(SCORERESET, event);
    }

    public void handleEvent( PlayerEntered event ) {
	getOperator().notifyEvent(PLAYERENTERED, event);
    }

    public void handleEvent( twcore.core.events.Message event ) {
	getOperator().notifyEvent(MESSAGE, event);
    }

    public void handleEvent( PlayerLeft event ){
	getOperator().notifyEvent(PLAYERLEFT, event);
    }

    public void handleEvent( PlayerPosition event ){
	getOperator().notifyEvent(PLAYERPOSITION, event);
    }

    public void handleEvent( PlayerDeath event ){
	getOperator().notifyEvent(PLAYERDEATH, event);
    }

    public void handleEvent( ScoreUpdate event ){
	getOperator().notifyEvent(SCOREUPDATE, event);
    }

    public void handleEvent( WeaponFired event ){
	getOperator().notifyEvent(WEAPONFIRED, event);
    }

    public void handleEvent( FrequencyChange event ){
	getOperator().notifyEvent(FREQUENCYCHANGE, event);
    }

    public void handleEvent( FrequencyShipChange event ){
	getOperator().notifyEvent(FREQUENCYSHIPCHANGE, event);
    }

    public void handleEvent( ArenaJoined event ){
	getOperator().notifyEvent(ARENAJOINED, event);
    }

    public void handleEvent( FileArrived event ){
	getOperator().notifyEvent(FILEARRIVED, event);
    }

    public void handleEvent( FlagReward event ){
	getOperator().notifyEvent(FLAGREWARD, event);
    }

    public void handleEvent( FlagVictory event ){
	getOperator().notifyEvent(FLAGVICTORY, event);
    }

    public void handleEvent( FlagPosition event ) {
	getOperator().notifyEvent(FLAGPOSITION, event);
    }

    public void handleEvent( FlagClaimed event ) {
	getOperator().notifyEvent(FLAGCLAIMED, event);
    }

    public void handleEvent( FlagDropped event ){
	getOperator().notifyEvent(FLAGDROPPED, event);
    }

    public void handleEvent( SoccerGoal event ) {
	getOperator().notifyEvent(SOCCERGOAL, event);
    }

    public void handleEvent( BallPosition event ) {
	getOperator().notifyEvent(BALLPOSITION, event);
    }

    public void handleEvent( Prize event ) {
	getOperator().notifyEvent(PRIZE,event);
    }

    public void handleEvent( LoggedOn event ){
        //m_botAction.joinArena( "#robopark" );
	getOperator().notifyEvent(LOGGEDON, event);
    }

    public void handleEvent( ArenaList event ){
	getOperator().notifyEvent(ARENALIST, event);
    }

    public void handleEvent( InterProcessEvent event ){
	getOperator().notifyEvent(INTERPROCESSEVENT, event);
    }

    public void handleEvent( SQLResultEvent event ){
	getOperator().notifyEvent(SQLRESULTEVENT, event);
    }

    public void handleDisconnect(){
    }
}