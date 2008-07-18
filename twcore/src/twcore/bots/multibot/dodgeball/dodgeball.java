package twcore.bots.multibot.dodgeball;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.events.BallPosition;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

public class dodgeball extends MultiModule {
	
	// Dodgeball game parameters
	private int dodgetime = 3; //secs
	private int ballCount = 1;
	private int orgBallCount = -1;
	protected boolean running = false;
	
	private short previousCarrier = -1;
	private long previousCarrierTime = -1;
	private int ballMode = 0;
	//				0  =>  ball not carried
	//				1  =>  ball carried
	
	private HashMap<Short,Integer> players = new HashMap<Short,Integer>();
	// List with PlayerIDs of players in the game
	// <PlayerID,number of eliminations>
	
	private TimerTask checkWinner;
    
    private List<String> publicHelp = Arrays.asList(new String[]{
            "+-----------------------------------------------------------------+",
            "| Dodgeball v"+super.getVersion()+"                               - author MMaverick |",
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
            "|   !usage                        - How to host with this module  |",
            "|----------------- Player control (in-game only) -----------------|",
            "|   !add <fuzzy-playername>       - Adds player to the game       |",
            "|   !remove <fuzzy-playername>    - Removes player from the game  |",
            "|   !remove-id <playerID>         - Removes player id from game   |",
            "|   !list                         - Lists players in game         |",
            "|------------------------- Game Settings -------------------------|",
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
        eventRequester.request(this, EventRequester.PLAYER_LEFT);
        eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
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
    		if(!opList.isER(name)) {
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
    					
    					// Fill list of players
    					for(Player p:m_botAction.getPlayingPlayers()) {
    						players.put(p.getPlayerID(), 0);
    					}
    					
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
    			
    			if(message.startsWith("!usage")) {
    				List<String> usage = Arrays.asList(new String[]{
    			            "+------------------------------------------------------------------+",
    			            "| Dodgeball v"+super.getVersion()+"                                - author MMaverick |",
    			            "+------------------------------------------------------------------+",
    			            "| Dodgeball objectives:                                            |",
    			            "|   Eliminate opponents by hitting them with the ball less then    |",
    			            "|   "+dodgetime+" seconds after firing the ball. The last one standing wins!   |",
    			            "+------------------------------------------------------------------+",
    			            "| When issuing the command !start, the dodgeball module stores all |",
    			            "| players in a ship into memory. Dodgeball functionality is        |",
    			            "| enabled aswell: When a player touches a ball that has been shot  |",
    			            "| less then x seconds ago (default;3, set by !dodgetime) he is     |",
    			            "| eliminated from the game. The player is put to spectator to make |",
    			            "| him release the ball.                                            |",
    			            "| The arena can be left unlocked during play as players are        |",
    			            "| removed from the in-memory list when eliminated. Players must    |",
    			            "| not be able to reach the play-area when getting back in though.  |",
    			            "| When one or no player is left, the winner or the end of the game |",
    			            "| is announced. The module stops itself automatically.             |",
    			            "+------------------------------------------------------------------+"
    				});
    				m_botAction.privateMessageSpam(playerID, usage);
    			}
    			
    			if(message.startsWith("!add")) {
    				String arg1 = message.substring(4).trim();
    				
    				if(!running) {
    					m_botAction.sendPrivateMessage(playerID, "The command !add can only be used when the game is started. Command aborted.");
    					return;
    				}
    				
    				if(arg1 == null || arg1.length() == 0) {
    					m_botAction.sendPrivateMessage(playerID, "Syntax error. Please specify part of the playername to add to the game. Type ::!help for more information.");
    					return;
    				}
    				
    				Player selectedPlayer = m_botAction.getFuzzyPlayer(arg1);
    				
    				if(selectedPlayer == null) {
    					m_botAction.sendPrivateMessage(playerID, "The specified player can't be found in this arena. Please specify part of the playername to add to the game. Type ::!help for more information.");
    					return;
    				}
    				if(selectedPlayer.getShipType() == Tools.Ship.SPECTATOR) {
    					m_botAction.sendPrivateMessage(playerID, "The player '"+selectedPlayer.getPlayerName()+"' is a spectator. Please specify a player that isn't a spectator. Type ::!help for more information.");
    					return;
    				}
    				
    				players.put(selectedPlayer.getPlayerID(), 0);
    				m_botAction.sendPrivateMessage(playerID, "Player '"+selectedPlayer.getPlayerName()+"' added to the game.");
    				checkWinner();
    			}
    			
    			if(message.startsWith("!remove")) {
    				String arg1 = message.substring(7).trim();
    				
    				if(!running) {
    					m_botAction.sendPrivateMessage(playerID, "The command !remove can only be used when the game is started. Command aborted.");
    					return;
    				}
    				
    				if(arg1 == null || arg1.length() == 0) {
    					m_botAction.sendPrivateMessage(playerID, "Syntax error. Please specify part of the playername to remove from the game. Type ::!help for more information.");
    					return;
    				}
    				
    				Player selectedPlayer = m_botAction.getFuzzyPlayer(arg1);
    				
    				if(selectedPlayer == null) {
    					m_botAction.sendPrivateMessage(playerID, "The specified player can't be found in this arena. Use the !remove-id and the !list command to remove a specific player using a player ID.");
    					return;
    				}
    				if(!players.containsKey(selectedPlayer.getPlayerID())) {
    					m_botAction.sendPrivateMessage(playerID, "The specified player can't be found in the list of players playing the game. Use the !remove-id and the !list command to remove a specific player using a player ID.");
    					return;
    				}
    				players.remove(selectedPlayer.getPlayerID());
    				m_botAction.sendPrivateMessage(playerID, "Player '"+selectedPlayer.getPlayerName()+"' removed from the list of playing players.");
    				checkWinner();
    			}
    			
    			if(message.startsWith("!list")) {
    				if(!running) {
    					m_botAction.sendPrivateMessage(playerID, "The command !list can only be used when the game is started. Command aborted.");
    					return;
    				}
    				
    				List<String> list = Arrays.asList(new String[]{
    						"ID   Name                       Ship      ",
    						"---- -------------------------- ----------"
    				});
    				
    				for(short id:players.keySet()) {
    					Player p = m_botAction.getPlayer(id);
    					list.add(
    						Tools.formatString(String.valueOf(id), 4)+" "+
    						Tools.formatString(p.getPlayerName(), 26)+" "+
    						Tools.shipName(p.getShipType())
    						);
    				}
    				
    				m_botAction.privateMessageSpam(playerID, list);
    				
    			}
    			
    			if(message.startsWith("!dodgetime")) {
    				String arg1 = message.substring(7).trim();
    				int argument = 3;
    				
    				if(arg1 == null || arg1.length() == 0) {
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
    				// !fixball <x>,<y>              - Fixes all balls on <x>,<y> pos
    	            // !fixball <id>,<x>,<y>         - Fixes ball <id> on <x>,<y> pos
    				String arg1 = message.substring(8).trim();
    				
    				if(arg1 == null || arg1.length() == 0 || arg1.indexOf(',') == -1) {
    					m_botAction.sendPrivateMessage(playerID, "Syntax error. Please specify the <x> and <y> coordinates where to fix all the balls. Type ::!help for more information.");
    					return;
    				}
    				
    				String[] args = arg1.split(",");
    				int id = -1;
    				int x  = -1;
    				int y  = -1;
    				
    				if(args.length < 2 || args.length > 3 || Tools.isAllDigits(arg1.replace(",", "")) == false) {
    					m_botAction.sendPrivateMessage(playerID, "Syntax error. Please specify the <x> and <y> coordinates where to fix all the balls. Type ::!help for more information.");
    					return;
    				}
    				
    				if(args.length == 2) {
    					x = Integer.parseInt(args[0]);
    					y = Integer.parseInt(args[1]);
    					
    					for(int i = 0 ; i < ballCount; i++) {
    						m_botAction.sendPrivateMessage(playerID, "Moving ball #"+(i+1)+" to "+x+","+y);
    						this.moveBall(i, x, y);
    					}
    					
    				} else if(args.length == 3) {
    					id = Integer.parseInt(args[0]);
    					x = Integer.parseInt(args[1]);
    					y = Integer.parseInt(args[2]);
    					
    					if(id > 0) id--;
    					m_botAction.sendPrivateMessage(playerID, "Moving ball #"+(id+1)+" to "+x+","+y);
    					this.moveBall(id, x, y);
    				}
    			}
    			
    			if(message.startsWith("!setballcount ")) {
    				String arg1 = message.substring(13).trim();
    				int argument = 1;
    				
    				if(arg1 == null || arg1.length() == 0) {
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
    					m_botAction.sendArenaMessage(p.getPlayerName()+" ("+players.get(ball.getCarrier())+" kills) has been eliminated by "+prev.getPlayerName()+"!");
    					players.remove(ball.getCarrier());
    					players.put(previousCarrier, (players.get(previousCarrier) + 1));
    				}
    				
    				checkWinner();
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
    
    public void handleEvent(PlayerLeft event) {
    	if(running)
    		players.remove(event.getPlayerID());
    }
    
    public void handleEvent(FrequencyShipChange event) {
    	// if game has started and the new ship type is 0 (Spectator)
    	if(running && event.getShipType() == Tools.Ship.SPECTATOR) {
    		// remove player from game
    		players.remove(event.getPlayerID());
    	}
    }
    
    private void checkWinner() {
    	m_botAction.scheduleTask(checkWinner, 1000);
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
				Player winner = m_botAction.getPlayer(players.keySet().iterator().next());
				m_botAction.sendArenaMessage(winner.getPlayerName()+" WINS!  ("+players.get(winner.getPlayerID())+" kills)", Tools.Sound.HALLELUJAH);
				clear();
				
			} else if(players.size() == 0) {
				running = false;
				m_botAction.sendArenaMessage("There is no winner - dodgeball game ended", Tools.Sound.HALLELUJAH);
				clear();
			}
		}
    }

}
