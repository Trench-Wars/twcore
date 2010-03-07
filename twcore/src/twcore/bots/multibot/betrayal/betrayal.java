package twcore.bots.multibot.betrayal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/*Author: dexter
	
	Thanks to D1st0rt who gave me an idea to fix a problem I had with the bot.
	Thanks to Dezmond who gave me the coords to all circles and tubes of map(5 circles, each circle has 4 tubes)
	Thanks to everyone who helped me on tests.

*/

public class betrayal extends MultiModule
{
	
	private boolean betrayalGame = false;
	private LinkedList<bannedinfo> bannedPlayer = new LinkedList<bannedinfo>();
	private Map<String, Integer> nameTK = new HashMap<String, Integer>(); //Map to Player and his tk number
	
	private int circleNumber;
	private int deathPlayer;
	private int tkPlayer;

	public void handleEvent(Message event){
		String message = event.getMessage();
		if(event.getMessageType() == Message.PRIVATE_MESSAGE){
			String nome = m_botAction.getPlayerName(event.getPlayerID());
			if(opList.isER(nome)) handleCommand(nome, message);
		}
	}
	
	public void handleEvent(PlayerDeath event)
	{
		if(betrayalGame){ 
			
			Player pkiller = m_botAction.getPlayer(event.getKillerID());
			Player pkillee = m_botAction.getPlayer(event.getKilleeID()); //killed
			
			// --------- checks the winner of game ------------
			if(pkillee.getLosses() == deathPlayer){ //to spec player on that death limit
				m_botAction.specWithoutLock(event.getKilleeID());
				m_botAction.sendArenaMessage(pkillee.getPlayerName()+" is out! "+deathPlayer+" deaths!");
				
				if(pkillee.getFrequency() != pkiller.getFrequency()){
					TimerTask checkWin = new TimerTask(){ //it'll check the winner.
						public void run(){
							checkWinner();
								}
							}; m_botAction.scheduleTask(checkWin, 500);
				}// -------- checks the winner of game ---------
			}
			if(pkiller == null && pkillee == null)
				return ;
			
			// ------------ checks if it is a tk ----------
			if(pkiller.getFrequency() == pkillee.getFrequency()){
			
					if(nameTK.get(pkiller.getPlayerName()) == null)
						nameTK.put(pkiller.getPlayerName(), 1); //first TK
					else
						nameTK.put(pkiller.getPlayerName(), nameTK.get(pkiller.getPlayerName())+1); //adds to number of tk, which isnt in limit yet.

				// --------- checks if tked more than allowed --------
					if(nameTK.get(pkiller.getPlayerName()) > tkPlayer){
						bannedinfo player = new bannedinfo();
						player.setFreq(pkiller.getFrequency());
						player.setPlayerName(pkiller.getPlayerName());
						warpMiddle(player);
						
						//depletes NRG to whoever is in middle of circle
						TimerTask prizeNegNRG = new TimerTask(){
							
							public void run(){
								for(bannedinfo b: bannedPlayer)
									m_botAction.specificPrize(b.getPlayerName(), -13);
							}
						};//It'll go on all the bannedplayers list & deplete energy. like a punishment.
						m_botAction.scheduleTaskAtFixedRate(prizeNegNRG, 50, 300);
				
						// ------- warps player back to old frequence -------
						TimerTask betrayalTime = new TimerTask(){
							public void run(){
								try{
								
									bannedinfo playerBack = bannedPlayer.getFirst(); //works like a QUEUE, first entered, first who leaves from circle
									if(nameTK.containsKey(playerBack.getPlayerName())){
										m_botAction.setFreq(playerBack.getPlayerName(), playerBack.getFreq()); //sets back to old freq
										m_botAction.warpTo(playerBack.getPlayerName(), playerBack.getOldX(), playerBack.getOldY()); //warps to some old tube's freq
										nameTK.remove(playerBack.getPlayerName()); //resets tk number of player
										bannedPlayer.remove(bannedPlayer.getFirst()); //removes him fom banned list(to stop depleting energy)
										m_botAction.sendArenaMessage(playerBack.getPlayerName()+", get back to old frequence, playing or in spec!" );
									}
								}catch(Exception e){}
							}
						};// ------- warps player back to old frequence -------
						m_botAction.scheduleTask(betrayalTime, 15000);
					
					} // --------- checks if tked more than allowed --------
				
			}// ------------ checks if it is a tk ----------
			
			
		}//betrayalgame
	}
	public void handleCommand(String nome, String message)
	{
		try{
			if(message.startsWith("!start")) startBetrayal(nome, message);
			else if(message.startsWith("!warp")) 	changeCircle(nome, message);
			else if(message.startsWith("!stop")) 	stopBetrayal(nome, message); 
			else if(message.startsWith("!rules")) sendRules();
		}catch(Exception e){}
		
	}
	public void warpMiddle(bannedinfo player)
	{
		int oldX = -1, oldY = -1;

		/*Each case of them, had to do it and not in general by .getYTileLocation and .GetXTileLocation because of the map's structure,
		it has the tube's walls and so, when someone hit the walls, they get warped into spawn instead of right Coords. 
		So, I did default coords to warp players to each case. */
		
		if(nameTK.get(player.getPlayerName()) == tkPlayer+1)
			m_botAction.sendArenaMessage(player.getPlayerName()+" will be punished for tking! 1 minute in the middle of circle!", 13);
		
		if(player.getFreq() == 1){
			if(circleNumber == 5){
				oldX = 632; oldY = 741; //depends on freq
				m_botAction.setFreq(player.getPlayerName(), 222);
				m_botAction.warpTo(player.getPlayerName(), 621, 748); //will be the same
				}
			else if(circleNumber == 4){
				oldX = 295; oldY = 739;
				m_botAction.setFreq(player.getPlayerName(), 222);
				m_botAction.warpTo(player.getPlayerName(), 337, 712);
				
			}
			else if(circleNumber == 3){
				oldX = 220; oldY = 375;
				m_botAction.setFreq(player.getPlayerName(), 222);
				m_botAction.warpTo(player.getPlayerName(), 185, 418);
			}
			else if(circleNumber == 2){
				oldX = 507; oldY = 106;
				m_botAction.setFreq(player.getPlayerName(), 222);
				m_botAction.warpTo(player.getPlayerName(), 446, 167);
			}
			else if(circleNumber == 1){
				oldX = 574; oldY = 371;
				m_botAction.setFreq(player.getPlayerName(), 222);
				m_botAction.warpTo(player.getPlayerName(), 609, 503);	
			}
		}
		else if(player.getFreq() == 0){
			if(circleNumber == 5){
				oldX = 611; oldY = 740;
				m_botAction.setFreq(player.getPlayerName(), 221);
				m_botAction.warpTo(player.getPlayerName(), 621, 748);
			}
			else if(circleNumber == 4){
				oldX = 346; oldY = 744;
				m_botAction.setFreq(player.getPlayerName(), 221);
				m_botAction.warpTo(player.getPlayerName(), 337, 712);
				
			}
			else if(circleNumber == 3){
				oldX = 228; oldY = 474;
				m_botAction.setFreq(player.getPlayerName(), 221);
				m_botAction.warpTo(player.getPlayerName(), 185, 418);
			}
			else if(circleNumber == 2){
				oldX = 539; oldY = 220;
				m_botAction.setFreq(player.getPlayerName(), 221);
				m_botAction.warpTo(player.getPlayerName(), 446, 167);
			}
			else if(circleNumber == 1){
				oldX = 474; oldY = 362;
				m_botAction.setFreq(player.getPlayerName(), 221);
				m_botAction.warpTo(player.getPlayerName(), 609, 503);	
			}
		}
		player.setOldX(oldX);
		player.setOldY(oldY);
		bannedPlayer.add(player);
	
	}
	//deixar start menor
	public void startBetrayal(String nome, String message)
	{
	
		String commandStart, circle,	TK, DEATH; //it'll analyze what the mod typed. if it is a standard start or customizable one
		StringTokenizer st = new StringTokenizer(message);
		commandStart = st.nextToken();
		
		int playingplayers = countPlayers(); 
		
		bannedPlayer.clear();
		nameTK.clear();
		
		if(playingplayers > 1){ //the game will start if there's more than 1 person in game
	
			if(!st.hasMoreTokens()){
				circleNumber = 1;
				tkPlayer = 2;
				deathPlayer = 10;
			}
			
			else{
				circle = st.nextToken();
				circleNumber = Integer.parseInt(circle);
				if(!st.hasMoreTokens()){
					tkPlayer = 2;
					deathPlayer = 10;
					}
				else{
					TK = st.nextToken();
					tkPlayer = Integer.parseInt(TK);
					if(!st.hasMoreTokens()) deathPlayer = 10;
					else{
						DEATH = st.nextToken();
						deathPlayer = Integer.parseInt(DEATH);
					}
				}
			}
			
			betrayalGame = true; //game begins
			
			m_botAction.changeAllShips(2);
			m_botAction.prizeAll(20);
			m_botAction.shipResetAll();
			m_botAction.sendUnfilteredPublicMessage("*scorereset");
			m_botAction.toggleLocked();
			m_botAction.createNumberOfTeams(2); //it'll make freq 1 and freq 0
			m_botAction.sendArenaMessage("Game begins in 10 seconds!", 13);
					
			TimerTask startGame = new TimerTask(){
			//Splits each frequence in 2 teams, each team(4 teams) has differents coords.	
				public void run(){
					if(circleNumber == 1){//circle 1
							splitTeam(0, 474, 362, 652, 562);
							splitTeam(1, 574, 371, 364, 565);
						}
					else if(circleNumber == 2){ //circle 2
							splitTeam(0, 391, 105, 539, 220);
							splitTeam(1, 374, 244, 507, 106);
						}
					else if(circleNumber == 3 ) { //circle 3
							splitTeam(0, 138, 388, 228, 474);
							splitTeam(1, 220, 375, 137, 464);
							}
					else if(circleNumber == 4){
							splitTeam(0, 295, 699, 346, 744);
							splitTeam(1, 345, 693, 295, 739);
							}
					else if(circleNumber == 5){
							splitTeam(1, 632, 741, 608, 762);
							splitTeam(0, 611, 740, 630, 765); 
					}
					m_botAction.sendArenaMessage("GO GO GO!", 104);
				}
			};m_botAction.scheduleTask(startGame, 10000); //timerset to do after "game begins in 10 secs..."
	
		}else m_botAction.sendPrivateMessage(nome, "Not enough players, get more than 1 player playing to start betrayal please.");
	}
	
	public void splitTeam(int freq, int X1, int Y1, int X2, int Y2)
	{
		
			Iterator<Player> freqIterator = m_botAction.getFreqPlayerIterator(freq); //we need an iterator to freq
			int freqSize = m_botAction.getFrequencySize(freq) ; //we need to know the freq size
			
			/*now, it'll see if the number of size is even or odd. if even, we split it well. 
			otherwise we put the (size-1)/2...(like the number 5, 5-1 = 4. 4/2 = 2.
			then we put 2 members on a location and the other 3(the rest) will go to the other location
			*/
			if(freqSize % 2 == 0 && freqSize != 0){ 
				int i;
				for( i = 1; i <= (freqSize/2) ; i++){//half to a location and...
					Player p = (Player) freqIterator.next();
					m_botAction.warpTo(p.getPlayerName(), X1 , Y1 );
				}
				for(int u = i; u<= freqSize ; u++){ 
					Player p = (Player) freqIterator.next();//and the other half to other
					m_botAction.warpTo(p.getPlayerName(), X2, Y2 );
				}
			}
			
			//odd number of freqsize
			else if(freqSize != 0){
				int i;
				for( i = 1; i <= ((freqSize-1)/2) ; i++){ //the little amount goin to a location
					Player p = (Player) freqIterator.next();
					m_botAction.warpTo(p.getPlayerName(), X1 , Y1 );
							}
				for(int u = i; u<= freqSize ; u++){ //then, the rest goes to other location
					Player p = (Player) freqIterator.next();
					m_botAction.warpTo(p.getPlayerName(), X2, Y2 );
					}
				}
		}

	public void changeCircle(String name, String message)
	{
		
		if(betrayalGame){
			if(m_botAction.getFrequencySize(222) == 0 && m_botAction.getFrequencySize(221) == 0){
			
				StringTokenizer st = new StringTokenizer(message);
				String cmd, circleN;
				int numbercircle;
				
				cmd = st.nextToken();
				circleN = st.nextToken();
				
				numbercircle = Integer.parseInt(circleN); //getting number of circle to warp players into it
				
				m_botAction.sendArenaMessage("Surprise! Warp!", 22);
				//analyzing what circle it is to warp it right
				if(numbercircle == 1){//circle 1
						splitTeam(0, 474, 362, 652, 562);
						splitTeam(1, 574, 371, 364, 565);
					}
				else if(numbercircle == 2){ //circle 2
						splitTeam(0, 391, 105, 539, 220);
						splitTeam(1, 374, 244, 507, 106);
					}
				else if(numbercircle == 3 ) { //circle 3
						splitTeam(0, 138, 388, 228, 474);
						splitTeam(1, 220, 375, 137, 464);
						}
				else if(numbercircle == 4){
						splitTeam(0, 295, 699, 346, 744);
						splitTeam(1, 345, 693, 295, 739);
						}
				else if(numbercircle == 5){
						splitTeam(0, 611, 740, 632, 763);
						splitTeam(1, 632, 741, 608, 762);
						}
				
				bannedPlayer.clear();
				nameTK.clear();
				
				}else m_botAction.sendPrivateMessage(name, "Can't change circle while someone is a betrayal!");	
			}else m_botAction.sendPrivateMessage(name, "Can't warp if the game hasn't been started!");
		}

	public void checkWinner()
	{
		if(betrayalGame){
			
			if(m_botAction.getFrequencySize(0) == 0 || m_botAction.getFrequencySize(1) == 0){
				
				if(m_botAction.getFrequencySize(0) == 0)
					m_botAction.sendArenaMessage("Freq 1 has won this game of betrayal!", 7);
				
				else if(m_botAction.getFrequencySize(1) == 0)
					m_botAction.sendArenaMessage("Freq 0 has won this game of betrayal!", 7);
				
				m_botAction.toggleLocked();
				bannedPlayer.clear();
				nameTK.clear();
				m_botAction.cancelTasks();
				betrayalGame = false;
			}
		}
	}

	public void stopBetrayal(String name, String message)
	{
		if(betrayalGame){
			m_botAction.cancelTasks();
			m_botAction.shipResetAll();
			m_botAction.toggleLocked();
			m_botAction.sendPrivateMessage(name, "Game stop'd.");
			bannedPlayer.clear();
			nameTK.clear();
			
			betrayalGame = false;
			}else m_botAction.sendPrivateMessage(name, "I'll just stop the game if it is running!");
	}
	
	public int countPlayers()
	{
		Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
		int numplayers = 0;
		for( ; i.hasNext(); i.next()) numplayers++;
		return numplayers;
	}
	
	public void sendRules()
	{
		m_botAction.sendArenaMessage("Rules: It's a javelin game!",2);
		m_botAction.sendArenaMessage("There will be two teams, each spawned within a tube of a circle.");
		m_botAction.sendArenaMessage("Teamkilling results in the player being warped into the middle of the circle to be bombed by all!");
	}
	
	public void cancel() 
	{
		m_botAction.cancelTasks();
	}
	
	public String[] getModHelpMessage() 
	{
		String opm []= {
				"!start 												 												- starts betrayal in circle 1 and teamkill limit 2, 10 deaths",
				"!start <circle> 															 - starts betrayal in a circle and teamkill limit 2, 10 deaths",
				"!start <circle> <teamkilllimit> - starts betrayal in a circle and a custom limit of teamkill, 10 deaths",
				"!start <circle> <tklim> <death> - starts betrayal in a circle, a custom limit of tk and death of <death>",
				"!warp  <circle>	  														- warps to other circle during the game",	
				"!stop													 													- stops betrayal's game while it is running.",
				"!rules																										- sends the rules"																									
		};
										
			return opm;
	}
	
	public void init() {}
	public boolean isUnloadable() 
	{
		return !betrayalGame;
	}
	public void requestEvents(ModuleEventRequester eventRequester)
	{
	   eventRequester.request(this, EventRequester.PLAYER_DEATH);   
	}
	
}