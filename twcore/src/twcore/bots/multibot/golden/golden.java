package twcore.bots.multibot.golden;

import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.StringBag;
import twcore.core.util.Tools;

/**
 * Golden Gun
 * Rewrote this silly module
 * By WillBy
 */
public class golden extends MultiModule {

   boolean isRunning = false;
   boolean coordsOn = false;
   int gunShip = 1;
   int gunFreq = 1;
   int humanShip = 1;
   int humanFreq = 0;
   int killerID;
   int killeeID;
   int arenaSize;
   int x;
   int y;
   char xCoord;
   int yCoord;
   String hasGun = "";
   String oldGun;
   String newGun;
   String addPlayerName;
   String playerName;
   TimerTask coords;
   TimerTask goldenPrizes;
   
   public void init() {
   }

   public void requestEvents(ModuleEventRequester events) {
       events.request(this, EventRequester.PLAYER_DEATH);
   }

   public void handleEvent( PlayerDeath event ){
       if( !isRunning ) return;
       else if (isRunning) {
       Player killee = m_botAction.getPlayer( event.getKilleeID() );
       Player killer = m_botAction.getPlayer( event.getKillerID() );
       
       if( killer == null || killee == null)
           return;
       if (killee.getPlayerName().equals(hasGun)) {
    	   switchGun(killer.getPlayerName(),killee.getPlayerName());
       }
       }
   }
   
   public void handleEvent(Message event) {
	   	// receieves info from the game, directs to handleCommand
		String message = event.getMessage();
		if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
			String name = m_botAction.getPlayerName(event.getPlayerID());
			handleCommand(name, message);
		}
  }
   
   public void handleCommand( String name, String message ){ 
	   // handling of !commands, for now just <ER>+
	   if (opList.isER(name)) {
				if (message.equals("!start")) {  // starts with random player
					if (isRunning)
						m_botAction.sendPrivateMessage(name, "Golden Gun already started.");
					else {
						hasGun = randomPlayer();
						startGame(hasGun);
						isRunning = true;
					}
				} else if (message.startsWith("!start ")) {  // lets host pick gunner like !startgun WillBy (uses fuzzy name so !startgun will should work too)
					if (isRunning)
						m_botAction.sendPrivateMessage(name, "Golden Gun already started.");
					else {
					hasGun = (message.substring(7));
					if (opList.isBot(hasGun)) {
						m_botAction.sendPrivateMessage(name, "Invalid player. Please try again.");
						return;
					}
    					if (!hasGun.isEmpty()) { 
    				        hasGun = m_botAction.getFuzzyPlayerName(hasGun);
    					    if (hasGun != null){ 
    					        startGame(hasGun);
    					    	isRunning = true;
    					    }
    					}
    					
					}
				} else if (message.startsWith("!setgun ")) {
					if (!isRunning)
						m_botAction.sendPrivateMessage(name, "Golden Gun needs to be running first!");
					else {
						oldGun = hasGun;
						hasGun = message.substring(8);
						
						if (!hasGun.isEmpty()) { 
							if (opList.isBot(hasGun)) {
							m_botAction.sendPrivateMessage(name, "Invalid player. Please try again.");
							return;
							}
							newGun = m_botAction.getFuzzyPlayerName(hasGun);
							if (newGun != null)
								switchGun(newGun,oldGun);
					}
					}
					
				} else if (message.startsWith("!guncoords")) {
					if (!isRunning) {
						m_botAction.sendPrivateMessage(name, "Golden Gun isn't running yet. You have to !start it first!");
					} else {
					m_botAction.sendPrivateMessage(name,hasGun + " is located at: " + getCoords(hasGun));
					}
					
				} else if (message.startsWith("!coordson")) { // command to start periodic *Arena messages of gunners coordinates using a TimerTask
					if (!isRunning) {
						m_botAction.sendPrivateMessage(name, "Golden Gun isn't running yet. You have to !start it first!");
					} else {
						coordsOn = true;
						m_botAction.sendPrivateMessage(name, "Coordinate mode: ON");
					}
				} else if (message.startsWith("!coordsoff")) { // cancels timertask for coordinates
					m_botAction.cancelTask(coords);
					coordsOn = false;
					m_botAction.sendPrivateMessage(name, "Coordinate mode: OFF");
					
				} else if (message.startsWith("!setmode ")) {
					String mode = message.substring(9);
					if (!isRunning) {
						if (mode != null)
							setMode(mode);
					} else {
						m_botAction.sendPrivateMessage(name, "Golden Gun already running, can't change modes now.");
					}
					
				} else if( message.startsWith( "!stop" )) {
					if( !isRunning ) {
						m_botAction.sendPrivateMessage(name, "Golden Gun is already stopped, cannot stop.");
						return;
					}
					m_botAction.sendPrivateMessage(name, "Golden Gun deactivated");
					isRunning = false;
					cancel();
						
				} else if (message.equalsIgnoreCase("!randomplayer")) {
					m_botAction.sendPrivateMessage(name, randomPlayer());
				}
	   }  	
   }
	 
       
   public void switchGun(final String killer, String killee) {
	   // This method switches which player is the gunner...
       if (killee != null) {
           m_botAction.setShip(killee,humanShip);
           m_botAction.setFreq(killee,humanFreq);
           m_botAction.cancelTasks();
           m_botAction.shipReset(killee);
           m_botAction.sendArenaMessage(killer + " has captured the Golden Gun!",2);

       }    	   hasGun = killer;
       				m_botAction.setShip(killer,gunShip);
    	           m_botAction.setFreq(killer,gunFreq);
    	           goldenPrizes = new TimerTask() { // timertask that prizes super to golden gunner hopefully
    	               @Override
    	               public void run() {
    	                   m_botAction.specificPrize(killer, Tools.Prize.SUPER);
    	               }
    	          };
    	          m_botAction.scheduleTask(goldenPrizes, 100, Tools.TimeInMillis.SECOND * 5);
    	          coords = new TimerTask() {
    	        	  @Override
						public void run() {
							getCoords(killer);
							m_botAction.sendArenaMessage(killer + " is located at " + xCoord + yCoord,2);
						}
					};
					m_botAction.scheduleTaskAtFixedRate(coords, 5 * Tools.TimeInMillis.SECOND, 20 * Tools.TimeInMillis.SECOND);
         
   }
    
   public String randomPlayer() {
	   // Generates a random player String to be used in startGame or if the host just wants to generate a random name 
	   Player p;
       StringBag randomPlayerBag = new StringBag();
      Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
       if (i == null)
           return null;
       while (i.hasNext()) {
           p = (Player) i.next();
           addPlayerName = p.getPlayerName();
           randomPlayerBag.add(addPlayerName);
       }
       addPlayerName = randomPlayerBag.grabAndRemove();
       return addPlayerName;
   }
   
   
   public void startGame(String playerName) {
       // pretty obvious what it does here... 
	   m_botAction.setAlltoFreq(humanFreq);
	   m_botAction.changeAllShips(humanShip);
	   m_botAction.scoreResetAll();
	   m_botAction.shipResetAll();
	   switchGun(playerName,null);
	   m_botAction.sendArenaMessage("Golden Gun has started! " + playerName + " has the Golden Gun!",104);
   }
	
	public String getCoords(String playerName) {
		// method that will generate in-game coordinates (A1, T20, etc) 
		m_botAction.spectatePlayer(playerName);
		Player p = m_botAction.getPlayer(playerName);
		if (p == null)
			return null;
		x = p.getXTileLocation();
		y = p.getYTileLocation();
		int tempX = x/52 + 65; 
		xCoord = ((char) tempX);
		yCoord = y/52 + 1;
		return "" + xCoord + yCoord;
	} 
	
	public void setMode(String parameters) { 
		// by default, parameters would equal "0 1 1 1"
		parameters.trim();
		humanFreq = parameters.charAt(0);
		humanShip = parameters.charAt(2);
		gunFreq = parameters.charAt(4);
		gunShip = parameters.charAt(6);
		
	}
	
   public String[] getModHelpMessage() {
       String[] GoldenHelp = {
               "!start            - starts Golden Gun with random gunner",
               "!start <name>     - starts Golden Gun with name as gunner",
               "!setgun <name>    - sets a new gunner",
               "!randomplayer     - PMs you with name of random player" ,
               "!stop             - stops Golden Gun mode",   
               "!guncoords        - PMs you with the location of the Golden Gunner",
               "!coordson         - sends periodic arena messages of the Gunner's coordinates",
               "!coordsoff        - turns off periodic coordinate arena messages",
               "!setmode <params> - changes human and gunner freqs and ships",
               " params: <humanFreq> <humanShip> <gunFreq> <gunShip>  (default 0 1 1 1)"
       };
       return GoldenHelp;
   }
   				// "!status          - returns the status",
               // "!game <human freq> <human ship> <gunner freq> <gunner ship>",
               // "                 - sets freqs and ships for game.",
               // "                 - default !game 0 1 1 1",
               // "!setmessage <message>",
               // "                 - Changes the arena message when new golden gun.",
               // "!goldspec <#>    - sets death limit.",
               // "!resetdelay <#)  - changes deley between when goldengun dies and *shipreset"

   public void cancel() {
       m_botAction.cancelTasks();
   }

   public boolean isUnloadable()    {
        return true;
    }

}