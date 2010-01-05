package twcore.bots.multibot.betrayal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import java.util.StringTokenizer;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;

import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/*Author: Dexter,
	thanks to everyone who helped on the tests/final tests.
*/

//bot to betrayal's event
public class betrayal extends MultiModule {

	private	boolean		betrayalGame = false;
	private		int 		death;
	private		int 		allowedTK;
	private		int 		circleNumber;

	//Var of players, those will change to each player when they get warped into middle of the circle
	private		int			oldX, oldY; 
	private		String	killerName; 
	private		int			freqKiller; 
	
	//because of those vars, there are 3 queues and 1 hashmap.
	private Map<String, Integer> playingPlayers = new HashMap<String,Integer>();
	
	private		Vector<String>		bannedPlayerName = 	new Vector<String>(); //will save the names of players that went to middle of circle(like a ban in the game) 
	private		Vector<Integer>		previousfreq 		 = 	new Vector<Integer>();	//will save the previous freqs of each player
	private		Vector<Integer>		oldCoords 			 =	new Vector<Integer>();	//will save the old coords of each player that went to middle of circle, so they can get back to tubes with those oldcoords
	
	public void handleEvent(Message event){
		String message = event.getMessage();
		if(event.getMessageType() == Message.PRIVATE_MESSAGE){
			String nome = m_botAction.getPlayerName(event.getPlayerID());
			if(opList.isER(nome)) handleCommand(nome, message);
	
		}
	}
	
	public void handleEvent(PlayerDeath event){
		
	if(betrayalGame){
		
		//--------------------------------------------------------------------------//
		//this will add the tk number of each player
		//check if player has 10 deaths then check if there is a winner already
		//will see if the tk number > allowed tk(given by host), if yes, he gets as betrayal.
		//--------------------------------------------------------------------------//

		Player pkiller = m_botAction.getPlayer(event.getKillerID());
		Player pkillee = m_botAction.getPlayer(event.getKilleeID()); //player who got killed
		
		killerName = pkiller.getPlayerName(); //gets the name of tker
		freqKiller = pkiller.getFrequency();	//gets the name of the killed guy
		
		if(pkillee.getLosses() == death){ //to spec player on that death limit
			m_botAction.specWithoutLock(event.getKilleeID());
			m_botAction.sendArenaMessage(pkillee.getPlayerName()+" is out! "+death+" deaths!");
			TimerTask checkWin = new TimerTask(){ //it'll check the winner.
				public void run(){
				checkWinner(killerName);
							}
						}; m_botAction.scheduleTask(checkWin, 500);
		}
		
		if(pkiller == null && pkillee == null)
			return ;
		
		/*This is a "tk watcher", it'll use a tree with the map<player,numberofTKz>
		if the guy has passed the TK Limit, he'll get in the middle of circle for 1 minute.
		*/if(pkiller.getFrequency() == pkillee.getFrequency()){	
			
			if(playingPlayers.get(killerName) == null)
				playingPlayers.put(killerName, 1); //first TK
			
			else
				playingPlayers.put(killerName, playingPlayers.get(killerName)+1); //adds to number of tk, which isnt in limit yet.
			
			if(playingPlayers.get(killerName) > allowedTK){ //there, we'll warp the player to middle of circle for 1 minute
				
				int f = freqKiller;
				previousfreq.add(f); /*we use 3 queues for the 3 player's var...
				 													which they are: frequence, coords and name of each player
				 													during the game on "tubes->middle of circle". Because it changes the coords to each player, there are different names for each players and different freqs.	*/
				warpMiddle(killerName);

				bannedPlayerName.add(killerName); //using it like a queue
				
				//Why use a queue? Because, first player who gets in middle of circle, is the first of leave it too. so each player will have his 1 minute on circle
				TimerTask betrayalTime = new TimerTask(){
					
					String 	kn; //the name will be saved on a queue
					int 	freq; //this will be the old frequence of each player, which's goin to be saved on a queue.
					int 	x,y; //those will be coords before the warp, we'll save on a queue.
					
					public void run(){
						try{
							//like dequeue a queue
							kn = bannedPlayerName.firstElement();
							bannedPlayerName.remove(bannedPlayerName.firstElement());
							
							freq =  (Integer) previousfreq.firstElement(); //old freq
							previousfreq.remove(previousfreq.firstElement());//like dequeue
							
							//like a queue
							x = (Integer) oldCoords.firstElement(); //x is the first which "entered" in queue, its the first who'll leave too(its a queue, thats why :P)
							oldCoords.remove(oldCoords.firstElement());
							
							//like a queue
							y = (Integer) oldCoords.firstElement(); //second leaves...its the y
							oldCoords.remove(oldCoords.firstElement());
						}catch(Exception e){}
						m_botAction.setFreq(kn, freq);  //Setting player back to old frequence
						m_botAction.warpTo(kn, x, y);  //Warping player back to a tube of his frequence(2 tubes each freq)
						m_botAction.sendArenaMessage("Hope "+kn+" has learned his lesson! Back to some tube!");
							}
						}; m_botAction.scheduleTask(betrayalTime, 60000); //1 minute of punishment
					}//if > allowed		
				}//if tk'd
		 	
		}//if betrayalgame
			
	}	
	public void handleCommand(String nome, String message){
		try{
			if(message.startsWith("!start")) startBetrayal(nome, message);
			else if(message.startsWith("!warp")) 	changeCircle(nome, message);
			else if(message.startsWith("!stop")) 	stopBetrayal(nome, message); 
			else if(message.startsWith("!rules")) sendRules();
		}catch(Exception e){}
		
	}
	
	//this method is to warp ppl into other circles during the game
	public void changeCircle(String name, String message){
		
		StringTokenizer			st = new StringTokenizer(message);
		String 							cmd, circleN;
		int 								numbercircle;
		
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
	}		
	
	//It will be like the punishment of players, warping them into middle of circle.
	public void warpMiddle(String killerName){
		//relacionar nome oldx old y e frequencia...esses 3 mudam pra cada tker. fazer algo, arvore, que seja!
		if(this.playingPlayers.get(killerName) == allowedTK + 1)
			m_botAction.sendArenaMessage(killerName+" will be punished for tking! 1 minute in the middle of circle!", 13);
		
		/*Each case of them, had to do it and not in general by .getYTileLocation and .GetXTileLocation because of the map's structure,
		it has the tube's walls and so, when someone hit the walls, they get warped into spawn instead of right Coords. 
		So, I did default coords to warp players to each case. */
		
		if(freqKiller == 1){
			if(circleNumber == 5){
				oldX = 632; oldY = 741; //depends on freq
				m_botAction.setFreq(killerName, 222);
				m_botAction.warpTo(killerName, 621, 748); //will be the same
				}
			else if(circleNumber == 4){
				oldX = 295; oldY = 739;
				m_botAction.setFreq(killerName, 222);
				m_botAction.warpTo(killerName, 337, 712);
				
			}
			else if(circleNumber == 3){
				oldX = 220; oldY = 375;
				m_botAction.setFreq(killerName, 222);
				m_botAction.warpTo(killerName, 185, 418);
			}
			else if(circleNumber == 2){
				oldX = 507; oldY = 106;
				m_botAction.setFreq(killerName, 222);
				m_botAction.warpTo(killerName, 446, 167);
			}
			else if(circleNumber == 1){
				oldX = 574; oldY = 371;
				m_botAction.setFreq(killerName, 222);
				m_botAction.warpTo(killerName, 609, 503);	
			}
		}
		else if(freqKiller == 0){
			if(circleNumber == 5){
				oldX = 611; oldY = 740;
				m_botAction.setFreq(killerName, 221);
				m_botAction.warpTo(killerName, 621, 748);
			}
			else if(circleNumber == 4){
				oldX = 346; oldY = 744;
				m_botAction.setFreq(killerName, 221);
				m_botAction.warpTo(killerName, 337, 712);
				
			}
			else if(circleNumber == 3){
				oldX = 228; oldY = 474;
				m_botAction.setFreq(killerName, 221);
				m_botAction.warpTo(killerName, 185, 418);
			}
			else if(circleNumber == 2){
				oldX = 539; oldY = 220;
				m_botAction.setFreq(killerName, 221);
				m_botAction.warpTo(killerName, 446, 167);
			}
			else if(circleNumber == 1){
				oldX = 474; oldY = 362;
				m_botAction.setFreq(killerName, 221);
				m_botAction.warpTo(killerName, 609, 503);	
			}
		}
	
		//enqueue the old coords of each player
		oldCoords.add(oldX); 
		oldCoords.add(oldY);
}
	
public void startBetrayal(String nome, String message){
	
	/*As each player who gets in middle will leave after 1 minute, well, 
	we dont need any "clear" on the enqueues. just on the Tree cause of players - tknumber */
	
		playingPlayers.clear();
		int playingplayers = countPlayers(); 

		if(playingplayers > 1){ //the game will start if there's more than 1 person in game
		
			String commandStart,	circle,	TK, DEATH; //it'll analyze what the mod typed. if it is a standard start or customizable one
			StringTokenizer st = new StringTokenizer(message);
			commandStart = st.nextToken();
		
			if(!st.hasMoreTokens()){
				circleNumber = 1;
				allowedTK = 2;
				death = 10;
			}
			
			else{
				circle = st.nextToken();
				circleNumber = Integer.parseInt(circle);
				if(!st.hasMoreTokens()){
					allowedTK = 2;
					death = 10;
					}
				else{
					TK = st.nextToken();
					allowedTK = Integer.parseInt(TK);
					if(!st.hasMoreTokens()) death = 10;
					else{
						DEATH = st.nextToken();
						death = Integer.parseInt(DEATH);
					}
				}
			}
			
			this.betrayalGame = true; //game begins
			
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
					m_botAction.sendArenaMessage("GOGOGOGOGO!", 104);
				}
			};m_botAction.scheduleTask(startGame, 10000); //timerset to do after "game begins in 10 secs..."
	
	}else m_botAction.sendPrivateMessage(nome, "Not enough players, get more than 1 player playing to start betrayal please.");
}
	public void checkWinner(String killer){
		if(betrayalGame){
		if(countPlayers() == 1){
			m_botAction.sendArenaMessage(killer+" has won this game of betrayal!", 7);
			m_botAction.toggleLocked();
			betrayalGame = false;
		}
	}
}
	public void splitTeam(int freq, int X1, int Y1, int X2, int Y2){
		
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
	
	public int countPlayers(){
		Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
		int numplayers = 0;
		for( ; i.hasNext(); i.next()) numplayers++;
		return numplayers;
	}

	public void cancel() {
	m_botAction.cancelTasks();
	}
	public void stopBetrayal(String name, String message){
		if(betrayalGame){
		m_botAction.cancelTasks();
		m_botAction.shipResetAll();
		m_botAction.toggleLocked();
		m_botAction.sendPrivateMessage(name, "Game stop'd.");
		betrayalGame = false;
		}
		else m_botAction.sendPrivateMessage(name, "I'll just stop the game if it is running!");
		
	}

	public String[] getModHelpMessage() {

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

	public void sendRules(){
		m_botAction.sendArenaMessage("Rules:", 12);
		m_botAction.sendArenaMessage("It's a javelin game!");
		m_botAction.sendArenaMessage("There are two freqs, each freq will have 2 teams, each team will be in a tube of the circle.");
		m_botAction.sendArenaMessage("If you tk, you are the betrayal, so be careful to don't get owned if you are it!");
	}
	public void init() {}

	public boolean isUnloadable() {
		return !betrayalGame;
	}

	public void requestEvents(ModuleEventRequester eventRequester) {
	   eventRequester.request(this, EventRequester.PLAYER_DEATH);     
	}

}