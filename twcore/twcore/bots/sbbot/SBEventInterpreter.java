package twcore.bots.strikeballbot;
import twcore.core.*;
import java.util.*;
import java.io.*;


/**
 * This class interprets all of the raw subspace events into SB events and notifies all
 * interested listeners when SB events occur.  E.g., soccer goal packets don't contain
 * info identifying the scorer, but an arena message sent just prior to the soccer goal
 * packet does.  The SBEventInterpreter combines the two into a single GOAL event which
 * contains the scorer's name, as well as the freq he scored for.  Eventually this class
 * will interpret raw BallPosition packets into passes, steals, etc., as the precise rules
 * defining those statistics are finalized.
 */
public class SBEventInterpreter extends SSEventListener {
    private SSEventOperator eventOp;
    private SBMatchOperator matchOp;
    private SBMatchCoordinator match;
    private BotAction m_botAction;
    private final int MINX = 0, MINY = 1, MAXX = 2, MAXY = 3, STARTX = 4, STARTY = 5;
    private int[] TEAM0BOX = { 469, 597, 480, 603, 474, 600 };
    private int[] TEAM1BOX = { 544, 597, 555, 603, 550, 600 };
    
    public SBEventInterpreter(SSEventOperator op,
			      SBMatchOperator m,
			      SBMatchCoordinator mc,
			      BotAction botAction) {
	eventOp = op;
	matchOp = m;
	match = mc;
	m_botAction = botAction;
	eventOp.addListener(SSEventForwarder.SOCCERGOAL, this);
	eventOp.addListener(SSEventForwarder.MESSAGE, this);
	eventOp.addListener(SSEventForwarder.PLAYERPOSITION, this);
	eventOp.addListener(SSEventForwarder.BALLPOSITION, this);
	eventOp.addListener(SSEventForwarder.PLAYERLEFT, this);
    }

    public void notify(SSEventMessageType type, PlayerLeft event) {
	Player p;
	if((p = m_botAction.getPlayer(event.getPlayerID())) != null)
	    matchOp.notifyEvent(SBMatchOperator.PLAYERLEFT, new SBEvent(p));
    }
    
    public void notify(SSEventMessageType type, SoccerGoal event) {
	if(match.getState() != SBMatchCoordinator.PLAYING) return;
	matchOp.notifyEvent(SBMatchOperator.GOAL,
			    new SBEvent(m_botAction.getPlayer(lastScorer)));
    }

    String lastScorer = null;
    public void notify(SSEventMessageType type, twcore.core.Message event) {
	if(event.getMessageType() != twcore.core.Message.ARENA_MESSAGE) return;
	if(match.getState() == SBMatchCoordinator.PLAYING &&
	   event.getMessage().startsWith("Enemy Goal!")) {

	    lastScorer = null;
	    String[] words = Tools.cleanStringChopper(event.getMessage(), ' ');
	    for(int i = 3; i < words.length - 2; i++) {
		if( lastScorer == null ) lastScorer = words[i];
		else lastScorer = lastScorer + " " + words[i];
	    }
	} else if(event.getMessage().startsWith("Soccer game over")) {
	    if(match.getState() != SBMatchCoordinator.PLAYING) return;
	    TimerTask t = new TimerTask() {
		    public void run() {
			matchOp.notifyEvent(SBMatchOperator.GAMEOVER, new SBEvent());
			m_botAction.sendSmartPrivateMessage("arceo","gameover!");
		    }
		};
	    // Necessary to avoid race condition that prevents recording of last goal
	    m_botAction.scheduleTask( t, 1000 );
	} else if(event.getMessage().startsWith("Arena UNLOCKED")) {
	    if(match.getState() != SBMatchCoordinator.NONE) {
		m_botAction.sendSmartPrivateMessage("arceo","Game in progress.  Arena re-locked.");
		m_botAction.toggleLocked();
	    }
	} else if(event.getMessage().startsWith("Arena LOCKED")) {
	    if(match.getState() == SBMatchCoordinator.NONE) {
		m_botAction.sendSmartPrivateMessage("arceo","Game over.  Arena re-unlocked.");
		m_botAction.toggleLocked();
	    }
	}
    }

    public void notify(SSEventMessageType type, PlayerPosition event) {
	if(match.getState() == SBMatchCoordinator.WAITING) {
	    Player p = m_botAction.getPlayer(event.getPlayerID());;
	    int[] bBox;
	    int xTile, yTile;
	    try {
		if(p.getFrequency() % 2 == 0) bBox = TEAM0BOX;
		else bBox = TEAM1BOX;
		xTile = event.getXLocation() / 16;
		yTile = event.getYLocation() / 16;
		if( xTile < bBox[MINX] || xTile > bBox[MAXX] || yTile < bBox[MINY] || yTile > bBox[MAXY] )
		    m_botAction.warpTo( p.getPlayerName(), bBox[STARTX], bBox[STARTY] );
	    } catch( Exception e ) {}
	}
	if(match.getState() != SBMatchCoordinator.PLAYING) return;
	
    }

    public void notify(SSEventMessageType type, BallPosition event) {
	if(match.getState() == SBMatchCoordinator.PREDROP) {
	    matchOp.notifyEvent(SBMatchOperator.THEDROP,new SBEvent(null));
	    m_botAction.sendSmartPrivateMessage("arceo","Ball Dropped!");
	}
	if(match.getState() != SBMatchCoordinator.PLAYING) return;
	
    }
}