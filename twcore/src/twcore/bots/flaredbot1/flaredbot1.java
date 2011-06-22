package twcore.bots.flaredbot1;

import java.util.TimerTask;
import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.LoggedOn;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.SoccerGoal;

/**
 * This class runs the BallGame event. Two teams trying to score on their enemy's
 * goal. Players may use ships 1, 2, 3 or 7 only. If a team captures all four flags
 * in the middle, the doors at the enemy goal will open. First team to 3 wins.
 * 
 * Made by fLaReD. 
 */
public class flaredbot1 extends SubspaceBot {
	public boolean 						isRunning = false;  	//whether or not a game is running
	/*
	 * These variables are to be used only if the flag/door module is enabled. 
	public int 							flag0owner;         	//owner of flag id #0
	public int 							flag1owner;         	//owner of flag id #1
	public int 							flag2owner;         	//owner of flag id #2
	public int 							flag3owner;         	//owner of flag id #3
	*/
	public int 							freq0Score = 0;			//total goals scored by freq 0
	public int 							freq1Score = 0;			//total goals scored by freq 1
	public CommandInterpreter    		cmds;					//command interpreter
	public EventRequester        		events;					//event requester
	public OperatorList          		oplist;					//operator list

	/**
	 * Requests events, sets up bot. 
	 */
	public flaredbot1(BotAction botAction) {
		super(botAction);
		cmds = new CommandInterpreter(m_botAction);
		oplist = m_botAction.getOperatorList();
		events = m_botAction.getEventRequester();
		events.request(EventRequester.FLAG_CLAIMED);
		events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
		events.request(EventRequester.LOGGED_ON);
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.SOCCER_GOAL);
		addCommands();
	}
	
	/**
	 * Locks certain ships from play. Players can use ships 1, 2, 3 or 7.
	 * If they attempt to switch to an illegal ship they will be placed in ship 1.
	 */
	public void handleEvent(FrequencyShipChange event) {
		if (isRunning) {
			byte shipType = event.getShipType();
			int playerName = event.getPlayerID();		
			if (shipType == 4 || shipType == 6) {
				m_botAction.sendPrivateMessage(playerName, "This ship type is not allowed. You may use any ships besides the leviathan and the weasel.");
				m_botAction.setShip(playerName,1);
			}
		}
	}
	
	/**
	 * Joins #newtwfd arena.
	 */
	public void handleEvent(LoggedOn event) {
        m_botAction.joinArena("attack");
	}
	
	/**
	 * Command handler.
	 */
	public void handleEvent(Message event) {
		cmds.handleEvent(event);
	}
	
	/*
	 * Monitors flag claims. If a team captures all four flags, the doors at the enemy goal will open.
	 *
	public void handleEvent(FlagClaimed event) {
		if (isRunning) {
			short flagID = event.getFlagID();
			short flagCapturer = event.getPlayerID();
			int capturerFreq = m_botAction.getPlayer(flagCapturer).getFrequency();
		
			if (capturerFreq == 0) {
				if (flagID == 0)
					flag0owner = 0;
				else if (flagID == 1)
					flag1owner = 0;
				else if (flagID == 2)
					flag2owner = 0;
				else if (flagID == 3)
					flag3owner = 0;
			}
			else if (capturerFreq == 1) {
				if (flagID == 0)
					flag0owner = 1;
				else if (flagID == 1)
					flag1owner = 1;
				else if (flagID == 2)
					flag2owner = 1;
				else if (flagID == 3)
					flag3owner = 1;				
			}
		
			if ( flag0owner == 0 && flag1owner == 0 && flag2owner == 0 && flag3owner == 0 ) 
				m_botAction.setDoors(1);
			else if ( flag0owner == 1 && flag1owner == 1 && flag2owner == 1 && flag3owner == 1 ) 
				m_botAction.setDoors(2);
			else
				m_botAction.setDoors(255);
		}
	}
	*/
	
	/**
	 * Monitors goals scored.
	 */
	public void handleEvent(SoccerGoal event) {		
		if (isRunning) {
			short scoringFreq = event.getFrequency();
			if (scoringFreq == 0) {
				freq0Score++;
			}
			else if (scoringFreq == 1) {
				freq1Score++;
			}	
		handleGoal();
		}
	}
	
	/**
	 * Help commands.
	 */
	public void addCommands() {
		int ok = Message.PRIVATE_MESSAGE;
		cmds.registerCommand("!help",ok,this,"help");
		cmds.registerCommand("!die",ok,this,"die");
		cmds.registerCommand("!start",ok,this,"startGame");
		cmds.registerCommand("!stop",ok,this,"stopGame");
		cmds.registerCommand("!status",ok,this,"getStatus");
	}
	
	/**
	 * !help
	 * Displays help message.
	 */
	public void help(String name, String msg) {
		if (oplist.isER(name)) {
			String[] helpMod =
			{"!help     - this message",
			"!die      - kills the bot",
			"!status   - status of the current game",
			"!start    - starts the game",
			"!stop     - kills the game",
			"NOTE: Lock the arena and add the lineups before using the !start command."};
			m_botAction.privateMessageSpam(name,helpMod);
		}
		else {
			String[] help =
			{"!help    - this message",
			"!status  - shows the current score"};
			m_botAction.privateMessageSpam(name,help);
		}
		
	}
	/**
	 * !die
	 * Kills bot.
	 */
	public void die(String name, String msg) {
		if (oplist.isER(name))
			m_botAction.die();
	}
	
	/**
	 * !stop
	 * Stops a game if one is running.
	 */
	public void stopGame(String name, String msg) {
		if (oplist.isER(name)) {
			if (isRunning) {
				m_botAction.sendArenaMessage("This game has been killed by " + name);
				isRunning = false;
				freq0Score = 0;
				freq1Score = 0;
			}
			else if (isRunning == false) {
				m_botAction.sendPrivateMessage(name, "There is no game currently running.");
			}
		}
	}

	/**
	 * !status
	 * Displays the score to player if a game is running.
	 */
	public void getStatus(String name, String msg) {
		if (isRunning) {
			m_botAction.sendPrivateMessage(name, "[---  SCORE  ---]");
			m_botAction.sendPrivateMessage(name, "[--Freq 0: " + freq0Score + " --]");
			m_botAction.sendPrivateMessage(name, "[--Freq 1: " + freq1Score + " --]");
		}
		else if (isRunning == false)
			m_botAction.sendPrivateMessage(name, "There is no game currently running.");		
	}
	
	/**
	 * !start
	 * Starts a game. Warps players to safe for 30 seconds and calls the runGame() method.
	 */
	public void startGame(String name, String msg) {
		if(oplist.isER(name)) {
			m_botAction.sendArenaMessage("Get ready, game will start in 30 seconds.",1);
			m_botAction.warpFreqToLocation(0,306,512);
			m_botAction.warpFreqToLocation(1,684,512);
			TimerTask t = new TimerTask() {
				public void run() {
					runGame();
				}
			};
			m_botAction.scheduleTask(t, 30000);
		}
	}
	
	/**
	 * Runs the game. Resets all variables, warps players to the arena and begins game.
	 */
	public void runGame() {
		isRunning = true;
		freq0Score = 0;
		freq1Score = 0;
		/*used if flag/door mode is enabled
		flag0owner = -1;
		flag1owner = -1;
		flag2owner = -1;
		flag3owner = -1;
		*/
		m_botAction.shipResetAll();
		m_botAction.warpFreqToLocation(0,477,511);
		m_botAction.warpFreqToLocation(1,543,511);
		m_botAction.sendArenaMessage("GOGOGO!!!",104);		
		m_botAction.scoreResetAll();
		m_botAction.resetFlagGame();
		//m_botAction.setDoors(255);

	}
	
	/**
	 * Handles goals. Determines total score of each team and if there is a winner.
	 */
	public void handleGoal() {
		if (freq0Score == 5) {
			m_botAction.sendArenaMessage("GAME OVER: Freq 0 wins!",5); 
			m_botAction.sendArenaMessage("Final score: " + freq0Score + " - " + freq1Score);
			isRunning = false;
		}
		else if (freq1Score == 5) {
			m_botAction.sendArenaMessage("GAME OVER: Freq 1 wins!",5); 
			m_botAction.sendArenaMessage("Final score: " + freq0Score + " - " + freq1Score);	
			isRunning = false;
		}
		else {
			m_botAction.sendArenaMessage("Score: " + freq0Score + " - " + freq1Score);
			m_botAction.warpFreqToLocation(0,477,511);
			m_botAction.warpFreqToLocation(1,543,511);
			m_botAction.shipResetAll();
			m_botAction.resetFlagGame();
			/*
			m_botAction.setDoors(255);			 
			flag0owner = -1;
			flag1owner = -1;
			flag2owner = -1;
			flag3owner = -1;
			*/
		}
	}
}







