package twcore.bots.multibot.gravitron;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/**
 * Authors: Andre Vinicius (Dexter)
 * 					Derek (Dezmond)
 * Gravitron's bot, JabJabJab's event
 * */

public class gravitron extends MultiModule {
	
	private boolean gravitronGame = false; //Game hasn't started, we'll use it on spec watch and a lot of methods, its the game, running game.
	private int timer1;	//Time to stage 1
	private int timer2; //Time to stage 2
	private int timer3; //Time to stage 3
	private int teamSize;//size of team to start game
	
	public void handleEvent(PlayerPosition event){
		
		if(gravitronGame == true){ //specs when game is running, otherwise it would spec before game starts and after game gets over..
			Player p = m_botAction.getPlayer( event.getPlayerID() ); 
			String nameplayer = p.getPlayerName();
			if(p.isInSafe()){
					m_botAction.specWithoutLock(nameplayer);
					m_botAction.sendArenaMessage(nameplayer+" got owned by the Machine!", 13);
					TimerTask tk = new TimerTask(){	
						public void run(){
							if(countPlayers() == 1) checkWinner(); //looks if theres a winner already.
						}
					};m_botAction.scheduleTask(tk, 500);
			}
		}
	}
	
	public void handleEvent(Message event){
		
		String message = event.getMessage();
  
		if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
        String name = m_botAction.getPlayerName(event.getPlayerID());
    
        if( opList.isER( name ) ) handleCommand( name, message ); //if person is ER, analyze what command he typed and do it.
    }
	}
	
	public void handleCommand(String name, String message){
		
		try{
		
			if(message.startsWith("!start")) startGravitron(name, message); //analyzin the command and doin it
			else if(message.startsWith("!stop")) stopGravitron(name, message);
			else if(message.startsWith("!rules")) showRules(name, message);
			
		} catch(Exception e){}
	}
	
	//logic of the game
	public void startGravitron(String name, String message){
		if(m_botAction.getArenaName().equalsIgnoreCase("gravitron")){//if it is in arena GRAVITRON, it may start. otherwise, in other arena, doesn't.
			if(countPlayers() > 0){ //then if someone is in. it starts.
				StringTokenizer st = new StringTokenizer(message);
				String t1, t2, t3, tsize, startcmd;			
				startcmd = st.nextToken(); //this will be the first token: !start
				
				if(!st.hasMoreTokens()){ //if doesnt have any more command, host wants to be a standard game, then sets time to 60 30 20 and teamsize 1
					timer1 = 60;
					timer2 = 30;
					timer3 = 20;
					teamSize = 1;
					}
				
				else{ //else, mod has typed a specific time to each stage, we'll analyze it and start game with those timers.
				
					t1 = st.nextToken(); //time to stage 1
					t2 = st.nextToken(); //time to stage 2
					t3 = st.nextToken(); //time to stage 3
					if(!st.hasMoreTokens()) teamSize = 1; //second !start, which doesnt set any teamsize. then it is set to 1.
					else{
						//else, he customized a teamsize and we'll use it.
						tsize = st.nextToken();
						this.teamSize = Integer.parseInt(tsize);
					
					}
					
					this.timer1 = Integer.parseInt(t1); //converting each timer
					this.timer2 = Integer.parseInt(t2);
					this.timer3 = Integer.parseInt(t3);
					
					//limits of timers and team
					if(timer1 > 120) timer1 = 120;
					if(timer2 > 120) timer2 = 120;
					if(timer3 > 120) timer3 = 120;
					if(timer1 < 15) timer1 = 15;
					if(timer2 < 15) timer2 = 15;
					if(timer3 < 15) timer3 = 15;
					if( teamSize < 1 ) teamSize = 1;
					if( teamSize > 15 ) teamSize = 15;
				
				}
			//starts the game
				m_botAction.createRandomTeams(teamSize);
				preGameStarting(); //pre-starting...
				
				TimerTask afterTenSeconds = new TimerTask(){ //this is after "Game begins in 10 seconds...(stage 1)
					public void run() {
						startObjon1();
						m_botAction.sendArenaMessage("Starting stage 1! " +timer1+ " seconds ... GOGOGOGOGO!!!! ", 104);
						gravitronGame = true; //Game has started, 10 seconds has passed, so, gravitronGame is true and it will enable the spec watch.
					}
					};

				TimerTask toStage2 = new TimerTask(){ //startin stage 2
						public void run(){
							m_botAction.sendArenaMessage("Starting stage 2! "
							+ timer2 + " seconds!", 22); //after the objon1, starts objon 2
							startObjon2();
						}};
				
				TimerTask toStage3 = new TimerTask(){ //starting stage 3
							public void run(){
								m_botAction.sendArenaMessage("Starting stage 3! " + timer3 + " seconds!", 22); //and after objon 2, objon 3 starts..
								startObjon3();
								}};
				
				TimerTask afterStage3 = new TimerTask(){ //after the stages, finishes the game.
									public void run(){
										finishingGame(); //game over!
									}
								};
								
				m_botAction.scheduleTask(afterTenSeconds, 10000);
				m_botAction.scheduleTask(toStage2, 10000+timer1*1000);
				m_botAction.scheduleTask(toStage3, 10000+timer1*1000+timer2*1000);
				m_botAction.scheduleTask(afterStage3, 10000+timer1*1000+timer2*1000+timer3*1000);
			}	else m_botAction.sendPrivateMessage(name, "Can't start the game without players in!");

		}else m_botAction.sendPrivateMessage(name, "Can't start, I'm not in gravitron's arena!");

	}

	//all the settings and what happens in each stage n pre-game
	public void preGameStarting(){//before starts
			m_botAction.toggleLocked(); //looks every1 in
			m_botAction.changeAllShips(1); //change every1 to wb
			m_botAction.setShip(m_botAction.getBotName(), 8); //set bot a shark
			m_botAction.setFreq(m_botAction.getBotName(), 322);	//set him to a priv freq
			m_botAction.warpAllToLocation(512, 462); //warp all to the game's circle
			m_botAction.warpFreqToLocation(322, 512, 513); //warps bot to the middle of ring
			m_botAction.sendArenaMessage("Game begins in 10 seconds", 4);
		}

	public void startObjon1(){		//on stage 1
		m_botAction.showObject(5); //shows background of ring
		m_botAction.showObject(1); //ring of stage 1
		settingsObjon1(); //settings of the game(warbird speed...etc)
	}
	
	public void startObjon2(){	//on stage 2..
		m_botAction.hideObject(1); //hides the objon 1 to show objon 2(second ring)
		m_botAction.showObject(2);
		settingsObjon2(); //settings to objon 2
	}	
	public void startObjon3(){ //on stage3..
		m_botAction.hideObject(2);
		m_botAction.showObject(3);
		settingsObjon3();
    }
	
	public void finishingGame(){//game ending..

		m_botAction.getShip().rotateRight(10);
		m_botAction.getShip().setVelocitiesAndDir(300, 100, 30);	
		m_botAction.getShip().fire(3); //bot bombs ...
		m_botAction.hideObject(3);
		m_botAction.showObject(4);//objon of explosion
		
		TimerTask toRep = new TimerTask(){

			@Override
			public void run() {

				if(countPlayers() > 1){ //if someone wins
				
				m_botAction.getShip().fire(5); //after timertask, 2 seconds, bot reps and games is over!
				m_botAction.sendArenaMessage("KBOOM! Game over!", 22);
				
				}
				
				else //bot won and wont slow explosion
					m_botAction.hideObject(4);
		//			m_botAction.sendArenaMessage("The Machine has Conquered all!", 22);
				
			
				
				checkWinner(); //check winners
			}
			};m_botAction.scheduleTask(toRep, 1000);

	}
	//this is the method to check winner(s)
public void checkWinner(){
  	  	
  	Iterator<Player> playerwinner = m_botAction.getPlayingPlayerIterator();
  	int numberwinners = 0;
  	String winner = "The Machine has won!";
  	
  	if(playerwinner.hasNext()){ //if theres someone except bot
  		winner = "";
  		for( ; playerwinner.hasNext(); /*playeron = playerwinner.next()*/){ //if more than 1 winner
  			Player playeron = playerwinner.next(); //first player..others players...till last one in
  			String nameplayer = playeron.getPlayerName(); //gets nicknames
  			
  			if(!nameplayer.equals(m_botAction.getBotName())){ //if it isnt the bot(which is in)
  				winner += nameplayer+" and "; //will set the winners to string
  				numberwinners++; //more than 1 winner or just 1 winner.
  			}
  		}
  		
  		try{
  			winner = winner.substring(0, winner.length()-5); //removes " and "
  		}catch(Exception e){ winner = "the machine has won!";}
  		
  		if(numberwinners == 1) m_botAction.sendArenaMessage("The winner is "+winner+ "!"); //if it is just 1 winner
  		else if(numberwinners > 1) m_botAction.sendArenaMessage("The winners are " + 	winner +"!"); //else..if it is more than 1 winner
  		else{ //else its just the bot who won the game of gravitron!
  			m_botAction.sendArenaMessage("The Machine has Conquered all!", 22);
  			m_botAction.hideObject(5); //hides background of ring just if bot won or the command !stop will do it too
  		}
  	}
		gravitronGame = false; //game over, so value false to disable spec watch.
		m_botAction.cancelTasks();
		m_botAction.spec(m_botAction.getBotName());
		m_botAction.hideObject(1);
		m_botAction.hideObject(2);
		m_botAction.hideObject(3);
		m_botAction.toggleLocked();
		settingsFinishingGame();
		m_botAction.shipResetAll();
		
}
	//this counts number of playing players.
	public int countPlayers(){
		Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
		int numplayers = 0;
	
		for( ; i.hasNext(); i.next()) numplayers++;
	
		return numplayers;
	}
	
//this mefod stop/reset the game of gravitron
	public void stopGravitron(String name, String message){
			
		if(gravitronGame == true){
				m_botAction.hideObject(1);
		    m_botAction.hideObject(2);
		    m_botAction.hideObject(3);
		    m_botAction.hideObject(4);
		    m_botAction.hideObject(5);
		    m_botAction.cancelTasks();
		    m_botAction.sendUnfilteredPublicMessage("?set warbird:gravity:0");
		    m_botAction.sendUnfilteredPublicMessage("?set warbird:gravitytopspeed:0");
		    m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:2000");  //max 6000
		    m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:16");   //max 100
		    m_botAction.sendUnfilteredPublicMessage("?set warbird:initialrotation:200");//max 800
		    m_botAction.sendUnfilteredPublicMessage("?set Warbird:MaximumThrust=23");
		    m_botAction.sendUnfilteredPublicMessage("?set misc:bouncefactor:28");
		    m_botAction.sendUnfilteredPublicMessage("?set Door:DoorMode=0");
		    m_botAction.prizeAll(7);
		    m_botAction.shipResetAll();
		    m_botAction.sendPrivateMessage(name,"Game stop'd");
		    m_botAction.toggleLocked();
		    gravitronGame = false;
	    }
			else m_botAction.sendPrivateMessage(name,"Not stopping game because it hasn't started.");
	}
	public String[] getModHelpMessage() {
		String[] message = {
				  "	Commands for <ER>'s And above                                                                          ",
			    " !start 																												- Starts Gravitron with times of 60 30 20 and teamsize of 1",
	    		" !start time1 time2 time3											- Starts Gravitron with each times and teamsize of 1       ",
	    		" !start time1 time2 time3  teamsize - Starts Gravitron with each times and a teamsize           ",
	    		" !stop 																													- Stop the game of Gravitron while it is running           ",
	    		"Authors: Dexter and Dezmond"};
			return message;
	}

  public void showRules(String name, String message) {
     m_botAction.sendArenaMessage("Rules: The player objective is to defeat the machine (Be last standing)."); 
     m_botAction.sendArenaMessage("Do not fall easy with his power! It will try hard to push you into the safes and get you out.");
     m_botAction.sendArenaMessage("Warning: Watch for walls turning on. If you are on a wall when turned on, you will be specced.");
     m_botAction.sendArenaMessage("Beware of the obstacles that come at you. It will require speed and tacticts. Good luck!", 9);      
  }

	public boolean isUnloadable() {
		
		return false;
	}


	public void requestEvents(ModuleEventRequester eventRequester) {
    eventRequester.request( this, EventRequester.PLAYER_POSITION );

	}
	
	public void cancel() {
	
	}

	public void init() {}

	//settings stage1, 2, 3 and end of game.
	public void settingsObjon1(){
		m_botAction.sendUnfilteredPublicMessage("*ufo");
		m_botAction.sendUnfilteredPublicMessage("?set warbird:gravity:2000");
		m_botAction.sendUnfilteredPublicMessage("?set warbird:gravitytopspeed:200");
	}
	
	public void settingsObjon2(){
		m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:-40");
    m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:2000");
	}
	
	public void settingsObjon3(){
		m_botAction.setDoors(255);
		m_botAction.sendUnfilteredPublicMessage("?set Door:DoorMode=128");
		m_botAction.sendUnfilteredPublicMessage("?set Warbird:MaximumSpeed=6000");
    m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:4000");
		m_botAction.sendUnfilteredPublicMessage("?set warbird:gravity:2500");
    m_botAction.sendUnfilteredPublicMessage("?set warbird:gravitytopspeed:50");
    m_botAction.sendUnfilteredPublicMessage("?set Warbird:MaximumThrust=200");
    m_botAction.sendUnfilteredPublicMessage("?set Warbird:InitialThrust=100");
    m_botAction.sendUnfilteredPublicMessage("?set Misc:BounceFactor=4");
    m_botAction.sendUnfilteredPublicMessage("?set Door:DoorMode=255");
	}
	public void settingsFinishingGame(){
		m_botAction.sendUnfilteredPublicMessage("?set warbird:gravity:0");
    m_botAction.sendUnfilteredPublicMessage("?set warbird:gravitytopspeed:200");
    m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:2000");  //max 6000
    m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:40");   //max 10
    m_botAction.sendUnfilteredPublicMessage("?set warbird:initialrotation:200");//max 800
    m_botAction.sendUnfilteredPublicMessage("?set misc:bouncefactor:28");
    m_botAction.sendUnfilteredPublicMessage("?set Door:DoorMode=0");
    m_botAction.sendUnfilteredPublicMessage("?set Warbird:MaximumThrust=23");
    m_botAction.sendUnfilteredPublicMessage("?set Door:DoorMode=0");
 	 }
	}
