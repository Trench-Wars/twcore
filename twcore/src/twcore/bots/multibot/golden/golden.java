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
 * Golden Gun module
 *
 * Based on conquer module
 * And I bloody stole a function from zombies.
 * PS: bloody stole function from hunt too.
 *
 * @author  Kyace
 */
public class golden extends MultiModule {

   boolean isRunning = false;
   String hasGun = "";
   //String killMessage = " has got the Golden Gun! Run!";
   int gunShip = 1;
   int gunFreq = 1;
   int humanShip = 1;
   int humanFreq = 0;
   int killerID;
   int killeeID;
   int arenaSize;
   String addPlayerName;
   String playerName;
   //int specPlayers = 25;
   //int resetDelay = 5;
   //TimerTask resetPlayer;
   TimerTask goldenPrizes;
   
   public void init() {
   }

   public void requestEvents(ModuleEventRequester events) {
       events.request(this, EventRequester.PLAYER_DEATH);
   }

   /**
    * This method switchs ships and freqs and specs depending
    * on if guy with gun died and limit and such.
    *
    * @param event is the player death event.
    */
   public void handleEvent( PlayerDeath event ){
       if( !isRunning ) return;
       else if (isRunning) {
       Player killee = m_botAction.getPlayer( event.getKilleeID() );
       Player killer = m_botAction.getPlayer( event.getKillerID() );
       
       if( killer == null || killee == null)
           return;
       if (killee.getPlayerName().equals(hasGun)) {
    	   switchGun(killer,killee);
       }
       }
   }
   public void switchGun(Player killer, Player killee) {
	   // This method switches which player is the gunner...
    	   m_botAction.setShip(killer.getPlayerID(),gunShip);
    	   m_botAction.setFreq(killer.getPlayerID(),gunFreq);
    	   m_botAction.setShip(killee.getPlayerID(),humanShip);
    	   m_botAction.setFreq(killee.getPlayerID(),humanFreq);
    	   hasGun.equals(killer);
    	   m_botAction.specificPrize(hasGun, Tools.Prize.SUPER);
   }
    
   public String randomPlayer() {
	   // Generates a random player String to be used in startGame or if the host just wants to generate a random name 
	   Player p;
       StringBag randomPlayerBag = new StringBag();
       Iterator<Player> i = m_botAction.getPlayerIterator();
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
	   m_botAction.setShip(playerName,gunShip);
	   m_botAction.setFreq(playerName,gunFreq);
	   // Sets Ships
	   final String gunGuy = playerName; // final string for timertask
	   goldenPrizes = new TimerTask() { // timertask that prizes super to golden gunner hopefully
		public void run() {
		   m_botAction.specificPrize(gunGuy, Tools.Prize.SUPER);
		}
	   };
	   m_botAction.scheduleTask(goldenPrizes, Tools.TimeInMillis.SECOND / 10, Tools.TimeInMillis.SECOND * 5);
       // Prizes
   }
	public void setGun (String playerName) {
		// performs a switch without a kill happening. A similar process to what will happen with startGame, except its coded independently
		Player newGun = m_botAction.getFuzzyPlayer(playerName);
		Player oldGun = m_botAction.getFuzzyPlayer(hasGun);
		switchGun(newGun,oldGun);
	}
       
   public void handleEvent(Message event) {
	   	// receieves info from the game, directs to handleCommand
		String message = event.getMessage();
		if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
			String name = m_botAction.getPlayerName(event.getPlayerID());
			handleCommand(name, message);
		}
   }
   /**
    * This method does actions depending what you PM bot with and your powers.
    *
    * @param name is the person that messaged the bot.
    * @param message is text they sent.
    */
   public void handleCommand( String name, String message ){ 
	   // handling of !commands, for now just <ER>+
	   if (opList.isER(name)) {
				if (message.equals("!start")) {  // starts with random player
					if (isRunning == true)
						m_botAction.sendPrivateMessage(name, "Golden Gun already started.");
					else {
						isRunning = true;
						hasGun = randomPlayer();
						startGame(hasGun);
					}
				} else if (message.startsWith("!startgun ")) {  // lets host pick gunner like !startgun WillBy (uses fuzzy name so !startgun will should work too
					if (isRunning == true)
						m_botAction.sendPrivateMessage(name, "Golden Gun already started.");
					else {
					isRunning = true;
					hasGun = (message.substring(10));
					hasGun = m_botAction.getFuzzyPlayerName(hasGun);
					startGame(hasGun);
					}
				} else if (message.startsWith("!setgun ")) {
					hasGun = message.substring(8);
					hasGun = m_botAction.getFuzzyPlayerName(hasGun);
					setGun(hasGun);
				} else if( message.startsWith( "!stop" )){
					if( !isRunning ) {
						m_botAction.sendPrivateMessage(name, "Golden Gun is already stopped, cannot stop.");
						return;
					}
					m_botAction.sendPrivateMessage(name, "Golden Gun deactivated");
					isRunning = false;
						
				} else if (message.equalsIgnoreCase("!randomplayer")) {
					m_botAction.sendPrivateMessage(name, randomPlayer());
				}
	   }  	
	 }
       
   public String[] getModHelpMessage() {
       String[] GoldenHelp = {
               "!start     - starts Golden Gun with random gunner",
               "!start <name>    - starts Golden Gun with name as gunner",
               "!setgun <name>   - sets a new gunner",
               "!randomplayer    - PMs you with name of random player" ,
               "!stop            - stops Golden Gun mode",   
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
       m_botAction.cancelTask(goldenPrizes);
   }

   public boolean isUnloadable()    {
        return true;
    }

}