package twcore.bots.sbbot;
import twcore.core.*;
import java.util.*;

/**
 * Synopsis of game state sequence:
 *
 *    NONE - No game ongoing.
 *    |
 *    SETUP - Host is setting up the parameters of the game. Valid operations are: specifying
 *    |       the number of goals, notplaying, game, killgame.
 *    |
 *    PICKING - Setup complete, caps pick their initial rosters.  Valid operations are: adding,
 *    |         Subbing, removing, lagout, notplaying, cap, list, ready, killgame.
 *    |
 *    WAITING - Initial rosters complete.  Teams warped to their respective safes.  Valid
 *    |         operations are: adding(enqueue), subbing, lagout, notplaying, cap, list,
 *    |         killgame.
 *    |
 *    PREDROP - Teams random warped out of their safes, waiting for the ball to drop.  Valid
 *    |         operations are: adding(enqueue), subbing, lagout, notplaying, cap, list,
 *    |         killgame.
 *    |
 *    PLAYING - Ball has dropped, begin recording stats, teams play to specified number of
 *    |         goals (default 10).  Valid operations are: adding(enqueue), subbing, lagout,
 *    |         notplaying, cap, list.
 *    |
 *    WRAPUP - Game is over, crunch stats, print stats and announce MVP.  Valid operations are:
 *    |        killgame.
 *    |
 *    NONE - Stats have been printed, game state cleaned up.
 */


public class SBMatchCoordinator {
    private BotCommandOperator botOp;
    private SBMatchOperator matchOp;
    private BotAction m_botAction;
    private SBRosterManager roster;
    private SBEventInterpreter interpreter;
    private SBStatKeeper statKeeper;
    private HashSet<TimerTask> tasks;


    public static final int
	NONE = 0,
	SETUP = 1,
	PICKING = 2,
	WAITING = 3,
	PREDROP = 4,
	PLAYING = 5,
	WRAPUP = 6;
    private int gameState;

    private final int MINX = 0, MINY = 1, MAXX = 2, MAXY = 3, STARTX = 4, STARTY = 5;
    private int[] TEAM0BOX = { 469, 597, 480, 603, 474, 600 };
    private int[] TEAM1BOX = { 544, 597, 555, 603, 550, 600 };
    
    public SBMatchCoordinator(BotCommandOperator op, BotAction botAction) {
	botOp = op;
	m_botAction = botAction;
	matchOp = new SBMatchOperator();
	roster = new SBRosterManager(matchOp, botAction);
	interpreter = new SBEventInterpreter(botOp, matchOp, this, botAction);
	statKeeper = new SBStatKeeper(botAction, matchOp);
	tasks = new HashSet<TimerTask>();
	botOp.addListener(BotCommandOperator.ADD, new AddCommandHandler());
	botOp.addListener(BotCommandOperator.SUB, new SubCommandHandler());
	botOp.addListener(BotCommandOperator.DIE, new DieCommandHandler());
	botOp.addListener(BotCommandOperator.STARTGAME, new StartGameCommandHandler());
	botOp.addListener(BotCommandOperator.T1SETCAP, new SetCapCommandHandler());
	botOp.addListener(BotCommandOperator.T2SETCAP, new SetCapCommandHandler());
	botOp.addListener(BotCommandOperator.LAGOUT, new LagoutCommandHandler());
	botOp.addListener(BotCommandOperator.NOTPLAYING, new NotPlayingCommandHandler());
	botOp.addListener(BotCommandOperator.CAP, new CapCommandHandler());
	botOp.addListener(BotCommandOperator.LIST, new ListCommandHandler());
	botOp.addListener(BotCommandOperator.STARTPICK, new StartPickCommandHandler());
	botOp.addListener(BotCommandOperator.READY, new ReadyCommandHandler());
	botOp.addListener(BotCommandOperator.REMOVE, new RemoveCommandHandler());
	botOp.addListener(BotCommandOperator.KILLGAME, new KillGameCommandHandler());
	matchOp.addListener(SBRosterManager.ALLREADY, new AllReadyMessageListener());
	matchOp.addListener(SBRosterManager.STARTPICK, new StartPickMessageListener());
	matchOp.addListener(SBMatchOperator.THEDROP, new DropEventListener());
	matchOp.addListener(SBMatchOperator.GAMEOVER, new GameOverEventListener());
	initialize();
    }

    private void initialize() {
	gameState = NONE;
    }

    // --- State change signal handlers
    private class StartPickMessageListener extends Listener {
	public void notify(MessageType type, Message message) {
	    gameState = PICKING;
	}
    }

    
    private class AllReadyMessageListener extends Listener {
	public void notify(MessageType type, Message message) {
	    m_botAction.sendUnfilteredPublicMessage("*restart");
	    arena("Both teams ready!  Game starts shortly.", 2);
	    m_botAction.warpFreqToLocation( 0, TEAM0BOX[STARTX], TEAM0BOX[STARTY] );
	    m_botAction.warpFreqToLocation( 1, TEAM1BOX[STARTX], TEAM0BOX[STARTY] );
	    gameState = WAITING;
	    
	    TimerTask t = new TimerTask() {
		    public void run() {
			gameState = PREDROP;
			m_botAction.scoreResetAll();
			m_botAction.warpAllRandomly();
			m_botAction.sendArenaMessage( "Get ready for the drop!", 26 );
		    }
		};
	    tasks.add(t);
	    m_botAction.scheduleTask( t, 10000 );
	}
    }

    private class DropEventListener extends SBEventListener {
	public void notify(SBEventType type, SBEvent event) {
	    //this can happen due to race conditions
	    pm("arceo", "Entering drop listener!  gameState: " + gameState);
	    if(gameState != PREDROP && gameState != WAITING) return;
	    gameState = PLAYING;
	    m_botAction.sendArenaMessage( "Go go go!", 104 );
	    m_botAction.scoreResetAll();
	}
    }

    private class GameOverEventListener extends SBEventListener {
	public void notify(SBEventType type, SBEvent event) {
	    if(gameState != PLAYING) return;
	    gameState = WRAPUP;
	    arena("And that's the game!", 5);
	    TimerTask t = new TimerTask() {
		    public void run() {
			matchOp.notifyEvent(SBMatchOperator.POSTGAME, new SBEvent());
			gameState = NONE;
			m_botAction.toggleLocked();
		    }
		};
	    tasks.add(t);
	    m_botAction.scheduleTask(t,3000);
	}
    }
    
    // --- Command handlers
    private class AddCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    switch(gameState) {
	    case PICKING:
		roster.pick(event.getSender(), event.getArgs());
		break;
	    case WAITING:
	    case PREDROP:
	    case PLAYING:
		roster.enqueue(event.getSender(), event.getArgs());
		break;
	    default:
		pm(event.getSender().getPlayerName(), "You can only !add during during the game.");
	    }
	}
    }

    private class RemoveCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    switch(gameState) {
	    case PICKING:
		roster.remove(event.getSender(), event.getArgs());
		break;
	    default:
		pm(event.getSender().getPlayerName(), "You can only !remove during picking.");
	    }
	}
    }

    private class SubCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    String[] names = Tools.cleanStringChopper(event.getArgs(), ':');
	    if(names.length != 2 && names.length != 1) {
		pm(event.getSender().getPlayerName(), "The correct usage is: !sub name1:name2 or !sub name");
	    }
	    switch(gameState) {
	    case PICKING:
	    case WAITING:
	    case PREDROP:
	    case PLAYING:
		if(names.length == 2)
		    roster.sub(event.getSender(), names[0], names[1]);
		else
		    roster.sub(event.getSender(),null,names[0]);
		break;
	    default:
		pm(event.getSender().getPlayerName(), "You can only !sub during the game.");
	    }
	}
    }

    private class DieCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    for(TimerTask t : tasks) {
		t.cancel();
	    }
	}
    }

    private class StartGameCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    switch(gameState) {
	    case NONE:
		arena("A game of strikeball is about to begin!", 2);
		gameState = SETUP;
		String[] names = Tools.cleanStringChopper(event.getArgs(), ':');
		if(names == null || names.length != 2) {
		    String[] defaultNames =  {
			"Freq 0",
			"Freq 1"
		    };
		    names = defaultNames;
		}
		roster.initialize(2, names);
		statKeeper.newGame(names);
		arena(names[0] + " vs " + names[1]);
		m_botAction.specAll();
		m_botAction.toggleLocked();
		break;
	    default:
		pm(event.getSender().getPlayerName(), "You must !kill the current game first.");
	    }
	}
    }

    private class SetCapCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    switch(gameState) {
	    case SETUP:
	    case PICKING:
	    case WAITING:
	    case PLAYING:
	    case PREDROP:
		if(type == BotCommandOperator.T1SETCAP) {
		    roster.setCap(event.getSender(), 0, event.getArgs());
		} else if(type == BotCommandOperator.T2SETCAP) {
		    roster.setCap(event.getSender(), 1, event.getArgs());
		}
		break;
	    default:
		pm(event.getSender().getPlayerName(), "You can only set a team's cap during the game.");
		break;
	    }
	}
    }

    private class LagoutCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    switch(gameState) {
	    case PICKING:
	    case WAITING:
	    case PREDROP:
	    case PLAYING:
		roster.lagout(event.getSender());
		break;
	    default:
		pm(event.getSender().getPlayerName(), "The game is already over.");
	    }
	}
    }

    private class NotPlayingCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    roster.notPlaying(event.getSender());
	}
    }

    private class ReadyCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    switch(gameState) {
	    case PICKING:
		roster.ready(event.getSender());
		break;
	    default:
		pm(event.getSender().getPlayerName(), "You can only !ready during picking.");
	    }
	}
    }
    
    private class CapCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type,  BotCommandEvent event) {
	    switch(gameState) {
	    case NONE:
		pm(event.getSender().getPlayerName(), "There is no game currently being run.");
		break;
	    default:
		roster.showCaps(event.getSender());
	    }
	}
    }

    private class ListCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    switch(gameState) {
	    case NONE:
	    case SETUP:
		pm(event.getSender().getPlayerName(), "You can only check the roster(s) when a game is in progress.");
	    default:
		roster.listPlayers(event.getSender());
	    }
	}
    }

    private class StartPickCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    switch(gameState) {
	    case SETUP:
		roster.startPick(event.getSender());
		break;
	    default:
		pm(event.getSender().getPlayerName(),"!startpick is only a valid command during game setup.");
	    }
	}
    }

    private class KillGameCommandHandler extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    switch(gameState) {
	    case NONE:
		pm(event.getSender().getPlayerName(),"There is no game currently taking place.");
		break;
	    default:
		arena("The game has been brutally killed by " + event.getSender().getPlayerName(), 13);
		gameState = NONE;
	    }
	}
    }
    private void pm(String pName, String message) {
	m_botAction.sendSmartPrivateMessage(pName, message);
    }

    private void arena(String message, int sound) {
	m_botAction.sendArenaMessage(message, sound);
    }

    private void arena(String message) {
	arena(message, 0);
    }

    public int getState() { return gameState; }
}