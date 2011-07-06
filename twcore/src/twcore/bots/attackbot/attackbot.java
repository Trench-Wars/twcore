package twcore.bots.attackbot;

import java.util.Stack;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.BallPosition;
import twcore.core.events.LoggedOn;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.SoccerGoal;
import twcore.core.game.Ship;

/**
 * This class runs the BallGame event. Two teams trying to score on their enemy's
 * goal. Players may use ships 1, 2, 3 or 7 only. If a team captures all four flags
 * in the middle, the doors at the enemy goal will open. First team to 3 wins.
 * 
 * Made by fLaReD. 
 */
public class attackbot extends SubspaceBot {
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
	public EventRequester        		events;					//event requester
	public OperatorList          		oplist;					//operator list
	public Ball                         ball;
	public int[]                        attack;                 //attack arena coords
    public int[]                        attack2;                //attack2 arena coords

	/**
	 * Requests events, sets up bot. 
	 */
	public attackbot(BotAction botAction) {
		super(botAction);
		oplist = m_botAction.getOperatorList();
		events = m_botAction.getEventRequester();
		events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
		events.request(EventRequester.LOGGED_ON);
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.SOCCER_GOAL);
        events.request(EventRequester.BALL_POSITION);
        events.request(EventRequester.ARENA_JOINED);
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
        m_botAction.joinArena(m_botAction.getBotSettings().getString("InitialArena"));
        attack = new int[] {478, 511, 544, 511};
        attack2 = new int[] {475, 512, 549, 512};
        //475 512, 549 512 attack2
        //478 511, 544 511 attack
	}
	
	public void handleEvent(ArenaJoined event) {
        ball = new Ball();	    
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
	
	public void handleEvent(BallPosition event) {
	    ball.update(event);
	}
    
    /**
     * Command handler.
     */
    public void handleEvent(Message event) {
        String name = event.getMessager();
        if (name == null || name.length() < 1)
            name = m_botAction.getPlayerName(event.getPlayerID());
        String msg = event.getMessage();
        if (event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE) {
            
            if (msg.equalsIgnoreCase("!help")) 
                help(name);
            else if (msg.equalsIgnoreCase("!status"))
                getStatus(name);
            
            if (oplist.isER(name)) {
                if (msg.equalsIgnoreCase("!drop"))
                    dropBall();
                else if (msg.equalsIgnoreCase("!start"))
                    startGame(name);
                else if (msg.equalsIgnoreCase("!stop"))
                    stopGame(name);
                else if (msg.equalsIgnoreCase("!die"))
                    die(name);
                else if (msg.startsWith("!go "))
                    go(name, msg);
            }
        }
    }
    
    public void go(String name, String cmd) {
        String arena = cmd.substring(cmd.indexOf(" ") + 1);
        if (arena.length() > 0) {
            m_botAction.sendSmartPrivateMessage(name, "Moving to " + arena);
            m_botAction.changeArena(arena);
        }
    }
	
	/**
	 * !help
	 * Displays help message.
	 */
	public void help(String name) {
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
	public void die(String name) {
	    m_botAction.sendSmartPrivateMessage(name, "I'm melting! I'm melting...");
	    m_botAction.cancelTasks();
	    m_botAction.die();
	}
	
	/**
	 * !stop
	 * Stops a game if one is running.
	 */
	public void stopGame(String name) {
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

	/**
	 * !status
	 * Displays the score to player if a game is running.
	 */
	public void getStatus(String name) {
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
	public void startGame(String name) {
	    m_botAction.sendArenaMessage("Get ready, game will start in 10 seconds.",1);
	    TimerTask t = new TimerTask() {
	        public void run() {
	            runGame();
	        }
	    };
	    m_botAction.scheduleTask(t, 10000);
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
		if (m_botAction.getArenaName().equalsIgnoreCase("attack")) {
	        m_botAction.warpFreqToLocation(0,attack[0], attack[1]);
	        m_botAction.warpFreqToLocation(1,attack[2], attack[3]);
		} else {
            m_botAction.warpFreqToLocation(0,attack2[0], attack2[1]);
            m_botAction.warpFreqToLocation(1,attack2[2], attack2[3]);		    
		}
		m_botAction.sendArenaMessage("GOGOGO!!!",104);		
		m_botAction.scoreResetAll();
		m_botAction.resetFlagGame();
		dropBall();
		//m_botAction.setDoors(255);
		//475 512, 549 512 attack2
		//478 511, 544 511 attack
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
            m_botAction.shipResetAll();
            m_botAction.resetFlagGame();
			m_botAction.warpFreqToLocation(0,478,512);
			m_botAction.warpFreqToLocation(1,543,511);
			dropBall();
			/*
			m_botAction.setDoors(255);			 
			flag0owner = -1;
			flag1owner = -1;
			flag2owner = -1;
			flag3owner = -1;
			*/
		}
	}
	
	public void dropBall() {
        Ship s = m_botAction.getShip();
        s.setShip(0);
        s.setFreq(1234);
        final TimerTask drop = new TimerTask() {
            public void run() {
                m_botAction.getShip().move(512*16, 600*16);
                m_botAction.getShip().sendPositionPacket();
                try { Thread.sleep(75); } catch (InterruptedException e) {}
                m_botAction.getBall(ball.getBallID(), (int)ball.getTimeStamp());
                m_botAction.getShip().move(512*16, 512*16);
                m_botAction.getShip().sendPositionPacket();
                try { Thread.sleep(75); } catch (InterruptedException e) {}
            }
        };
        drop.run();
        m_botAction.specWithoutLock(m_botAction.getBotName());
	}
	
    private class Ball {

        private byte ballID;
        private long timestamp;
        private short ballX;
        private short ballY;
        private boolean carried;
        private String carrier;
        private final Stack<String> carriers;
        private boolean holding;

        public Ball() {
            carrier = null;
            carriers = new Stack<String>();
            carried = false;
            holding = false;
        }

        /**
         * Called by handleEvent(BallPosition event)
         * @param event the ball position
         */
        public void update(BallPosition event) {
            ballID = event.getBallID();
            this.timestamp = event.getTimeStamp();
            ballX = event.getXLocation();
            ballY = event.getYLocation();
            short carrierID = event.getCarrier();
            if (carrierID != -1) {
                carrier = m_botAction.getPlayerName(carrierID);
            } else {
                carrier = null;
            }

            if (carrier != null && !carrier.equals(m_botAction.getBotName())) {
                if (!carried && isRunning) {
                    carriers.push(carrier);
                }
                carried = true;
            } else if (carrier == null && carried) {
                carried = false;
            } else if (carrier != null && carrier.equals(m_botAction.getBotName())) {
                holding = true;
            } else if (carrier == null && holding) {
                holding = false;
            }
        }

        /**
         * clears local data for puck
         */
        public void clear() {
            carrier = null;
            try {
                carriers.clear();
            } catch (Exception e) {
            }
        }

        public byte getBallID() {
            return ballID;
        }

        public long getTimeStamp() {
            return timestamp;
        }

        public short getBallX() {
            return ballX;
        }

        public short getBallY() {
            return ballY;
        }

        public boolean isCarried() {
            return carried;
        }
    }
}







