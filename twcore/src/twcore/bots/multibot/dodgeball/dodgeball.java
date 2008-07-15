package twcore.bots.multibot.dodgeball;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.events.BallPosition;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

public class dodgeball extends MultiModule {
	
	// Dodgeball game parameters
	private int dodgetime = 3; //secs
	private int ballCount = 1;
	private int orgBallCount = -1;
	protected boolean running = false;
	
	private int previousCarrier = -1;
	private long previousCarrierTime = -1;
	private int ballMode = 0;
	//				0  =>  ball not carried
	//				1  =>  ball carried
	
	private ArrayList<Integer> players;
	// List with PlayerIDs of players in the game
	
	private TimerTask checkWinner;
    
    private List<String> publicHelp = Arrays.asList(new String[]{
            "+-----------------------------------------------------------------+",
            "| Dodgeball v0.1                               - author MMaverick |",
            "+-----------------------------------------------------------------+",
            "| Dodgeball objectives:                                           |",
            "|   Eliminate opponents by hitting them with the ball less then   |",
            "|   "+dodgetime+" seconds after firing the ball. The last one standing wins!  |",
            "+-----------------------------------------------------------------+",
            "| Commands:                                                       |",
            "|   !help                         - Brings up this message        |",
            "+-----------------------------------------------------------------+"
        });
    
    private List<String> staffHelp = Arrays.asList(new String[]{
            "|   !start                        - Starts a new game             |",
            "|   !stop                         - Stops the current game        |",
            "|   !dodgetime [secs]             - Set [secs] of ball \"hot\" time |",
            "|                                   (default: 3 seconds)          |",
            "|   !fixball <x>,<y>              - Fixes all balls on <x>,<y> pos|",
            "|   !fixball <id>,<x>,<y>         - Fixes ball <id> on <x>,<y> pos|",
            "|                                   The first ball id is 1        |",
            "|   !setballcount #               - Sets number of balls in arena |",
            "+-----------------------------------------------------------------+"
        });
    
    @Override
    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request(this, EventRequester.BALL_POSITION);
    }

    @Override
    public void cancel() {
    	
    	// When unloading module, set ball count back to what it was when loading module
        if(orgBallCount != -1 && orgBallCount != ballCount) {
        	m_botAction.sendUnfilteredPublicMessage("?set Soccer:BallCount:"+orgBallCount);
        }
    }

    @Override
    public String[] getModHelpMessage() {
    	List<String> help = new ArrayList<String>(publicHelp);
    	help.addAll(staffHelp);
    	
    	// Add status line at bottom of staff's !help menu
    	help.addAll( Arrays.asList( new String[] {
    			"| Status: "+(running?"STARTED":"STOPPED")+" | Dodge time: "+dodgetime+" seconds | Ball count: "+ballCount+" ball(s) |",
    			"+-----------------------------------------------------------------+"
    	}));
    	
        return help.toArray(new String[]{});
    }

    @Override
    public void init() {
    	m_botAction.sendUnfilteredPublicMessage("?get Soccer:BallCount");
    	checkWinner = new CheckWinner();
    }

    @Override
    public boolean isUnloadable() {
        return !running;
    }
    
    /* (non-Javadoc)
     * @see twcore.bots.MultiModule#handleEvent(twcore.core.events.Message)
     */
    @Override
    public void handleEvent(Message event) {
    	
    	if(ballCount == 0) 
    		return;
    	
    	if(event.getMessageType() == Message.PRIVATE_MESSAGE) {
    		String message = event.getMessage();
    		short playerID = event.getPlayerID();
    		String name = event.getMessager();
    		if(name == null) {
    			name = m_botAction.getPlayerName(playerID);
    		}
    		OperatorList opList = m_botAction.getOperatorList();
    		
    		// Public commands
    		if(!opList.isZH(name)) {
	    		m_botAction.privateMessageSpam(playerID, publicHelp);
    		}
    		// Staff commands
    		if(opList.isER(name)) {
    			if(message.startsWith("!help")) {
    				// This is already handled by multibot
    			}
    			if(message.startsWith("!start")) {
    				if(!running) {
    					m_botAction.sendPrivateMessage(playerID, "Dodgeball started. Players who are hit by a ball less than "+dodgetime+" seconds after firing will be eliminated.");
    					running = true;
    					m_botAction.shipResetAll();
    				} else {
    					m_botAction.sendPrivateMessage(playerID, "Dodgeball is already started. PM !stop to stop dodgeball.");
    				}
    			}
    			if(message.startsWith("!stop")) {
    				if(running) {
    					m_botAction.sendPrivateMessage(playerID, "Dodgeball stopped.");
    					running = false;
    					clear();
    				} else {
    					m_botAction.sendPrivateMessage(playerID, "Dodgeball hasn't started. PM !start to start dodgeball.");
    				}
    			}
    			
    			if(message.startsWith("!dodgetime")) {
    				String arg1 = message.substring(7).trim();
    				int argument = 3;
    				
    				if(arg1 == null || arg1.trim().length() == 0) {
    					m_botAction.sendPrivateMessage(playerID, "Current dodgetime: "+dodgetime+" seconds.");
    					m_botAction.sendPrivateMessage(playerID, "Please specify number of seconds to change the dodge time. Type ::!help for more information.");
    					return;
    				}
    				if(!Tools.isAllDigits(arg1)) {
    					m_botAction.sendPrivateMessage(playerID, "Syntax error. Only numbers allowed as argument for !dodgetime. Type ::!help for more information.");
    					return;
    				}
    				
    				argument = Integer.parseInt(arg1);
    				
    				if(argument > 60) {
    					m_botAction.sendPrivateMessage(playerID, "You can't set a dodge time larger then 60 seconds.");
    					return;
    				}
    				
    				dodgetime = argument;
    				m_botAction.sendPrivateMessage(playerID, "Dodge time set to: "+dodgetime+" seconds.");
    				m_botAction.sendPrivateMessage(playerID, "If game is already started, will take effect immediatly.");
    			}
    			
    			if(message.startsWith("!fixball")) {
    				String arg1 = message.substring(8).trim();
    				
    				if(arg1 == null || arg1.length() == 0) {
    					m_botAction.sendPrivateMessage(playerID, "Syntax error. Please specify the <x> and <y> coordinates where to fix all the balls. Type ::!help for more information.");
    					return;
    				}
    				
    				// FIXME
    				
    			}
    			
    			if(message.startsWith("!setballcount ")) {
    				String arg1 = message.substring(13).trim();
    				int argument = 1;
    				
    				if(arg1 == null || arg1.trim().length() == 0) {
    					m_botAction.sendPrivateMessage(playerID, "Syntax error. Please specify number of balls to set. Type ::!help for more information");
    					return;
    				}
    				if(!Tools.isAllDigits(arg1)) {
    					m_botAction.sendPrivateMessage(playerID, "Syntax error. Only numbers allowed as argument for !setballcount. Type ::!help for more information.");
    					return;
    				}
    				
    				argument = Integer.parseInt(arg1);
    				
    				if(argument < 1 || argument > 8) {
    					m_botAction.sendPrivateMessage(playerID, "You can't set the ball count higher then 8 or lower then 1.");
    					return;
    				}
    				
    				m_botAction.sendUnfilteredPublicMessage("?set Soccer:BallCount:"+argument);
    				m_botAction.sendPrivateMessage(playerID, "Ball count set to "+argument+" balls.");
    				m_botAction.sendPrivateMessage(playerID, "The arena settings are already changed and the balls will (dis)appear in a few seconds.");
    				
    			}
    		}
    	}
    	
    	// Gets the result from ?get Soccer:BallCount or ?set Soccer:BallCount:
    	// Soccer:BallCount=1
    	if(event.getMessageType() == Message.ARENA_MESSAGE) {
    		if(event.getMessage().startsWith("Soccer:BallCount=")) {
    			String count = event.getMessage().substring(17);
    			if(Tools.isAllDigits(count)) {
    				ballCount = Integer.parseInt(count);
    				
    				if(orgBallCount == -1)	// Store original ball count so it can be set back when unloading 
    					orgBallCount = ballCount;
    				
    				if(ballCount == 0) {
    					m_botAction.sendPublicMessage("No balls are set in this arena. Module disabled.");
    				}
    			} else {
    				m_botAction.sendPublicMessage("Unable to get the number of balls in this arena (Soccer:BallCount). Internal ballcount set to 1.");
    				m_botAction.sendPublicMessage("Please do not use !setballcount or it will result in arena configuration errors.");
    			}
    		}
    	}
    }

    /* (non-Javadoc)
     * @see twcore.bots.MultiModule#handleEvent(twcore.core.events.BallPosition)
     */
    @Override
    public void handleEvent(BallPosition ball) {
    	if(ballCount == 0)
    		return;
    	
    	if(running) {
    		
    		if(ball.getCarrier() != -1) { // A player has picked up the ball or is carrying it
    			ballMode = 1;
    			
    			if((System.currentTimeMillis() - previousCarrierTime) > dodgetime) {
    				// the player is out
    				ballMode = 0;
    				
    				Player p = m_botAction.getPlayer(ball.getCarrier());
    				Player prev = m_botAction.getPlayer(previousCarrier);
    				if(p.getFrequency() != prev.getFrequency() && p.getPlayerID() != m_botAction.getPlayerID(m_botAction.getBotName())) {
    					m_botAction.specWithoutLock(ball.getCarrier());
    					m_botAction.sendArenaMessage(p.getPlayerName()+" has been eliminated by "+prev.getPlayerName()+"!");
    				}
    				
    				m_botAction.scheduleTask(checkWinner, 1000);
    			} else {
    				previousCarrier = ball.getCarrier();
    			}
    		} else { // Ball has just been shot or hasn't got a carrier
    			if(ballMode == 1) {	// ball has just been shot (it had a carrier)
    				ballMode = 0;
    				previousCarrierTime = System.currentTimeMillis();
    			}
    			
    		}
    	}
    }
    
    private void clear() {
    	players.clear();
    	previousCarrier = -1;
    	previousCarrierTime = -1;
    	ballMode = 0;
    }
    
    private void moveBall(int ballID, int x, int y) {
    	m_botAction.stopReliablePositionUpdating();			// makes the bot stop following people
    	m_botAction.getShip().setShip(Tools.Ship.WARBIRD-1);// shipchange to ship 1
    	// TODO: m_botAction.grabBall(ballID);						// grab the ball
    	m_botAction.getShip().move(x*16, y*16);				// move to the coordinates
    	m_botAction.getShip().sendPositionPacket();			// makes the bot actually go there
    														// (action done)
    	m_botAction.getShip().setShip(8);					// shipchange to spectator
    	m_botAction.resetReliablePositionUpdating();		// follow players
    }

    
    
    /**
     * Essentially a TimerTask that stores info about each player.
     *
     */
    private class CheckWinner extends TimerTask {

        public CheckWinner() {}

		@Override
		public void run() {
			if(players.size() == 1) {
				running = false;
				Player winner = m_botAction.getPlayer(players.get(0));
				m_botAction.sendArenaMessage(winner.getPlayerName()+" WINS!", Tools.Sound.HALLELUJAH);
				clear();
				
			} else if(players.size() == 0) {
				running = false;
				m_botAction.sendArenaMessage("There is no winner - dodgeball game ended", Tools.Sound.HALLELUJAH);
				clear();
			}
		}
    }

}
