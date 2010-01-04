package twcore.bots.gravitronbot;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimerTask;

import twcore.core.*;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
//import java.util.TimerTask;
//import java.util.*;

/*
 * Authors: Andre (Dex) and Derek (Dez)
 * Gravitronbot, bot to JabJabJab's event.
 * */
public class gravitronbot extends SubspaceBot{

	private boolean gravitronGame = false; //Game hasn't started.
	private int timer1;	//Time to stage 1
	private int timer2; //Time to stage 2
	private int timer3; //Time to stage 3
	
	private EventRequester events;
	private OperatorList operatorList;
	private CommandInterpreter commandInterpreter;
	
	public gravitronbot(BotAction gravitronAction){ //bot's constructor
		super(gravitronAction);
		setEvents();
		setCommandInterpreter();
		setOperatorList();
		addCommands(); //add the commands...like help, start, die, cancel, reset..
	}

	public void addCommands(){ //adding commands

		int okay = Message.PRIVATE_MESSAGE;
		this.commandInterpreter.registerCommand("!help", okay, this, "doShowHelp");

		this.commandInterpreter.registerCommand("!die", okay, this, "die");

		this.commandInterpreter.registerCommand("!go", okay, this, "go");
		
		this.commandInterpreter.registerCommand("!start", okay, this, "startGravitron");

		//this.commandInterpreter.registerCommand("!startcustom int int", okay, this, "start");
		this.commandInterpreter.registerCommand("!reset", okay, this, "doResetGame");
		
		this.commandInterpreter.registerCommand("!cancel", okay, this, "doCancelGame");
		this.commandInterpreter.registerCommand("!sendrules", okay, this, "doShowRules");
		
	}
	
	
	public void setOperatorList(){
		this.operatorList = m_botAction.getOperatorList();
	}
	
	public void setCommandInterpreter(){
		this.commandInterpreter = new CommandInterpreter(m_botAction);
	}
	
	public void setEvents(){
		this.events = m_botAction.getEventRequester();
		events.request(EventRequester.PLAYER_ENTERED);
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.PLAYER_POSITION); //this will be used to spec players when get into safe while game running
	}
	
	public void handleEvent(LoggedOn event){
		BotSettings configuration = m_botAction.getBotSettings();
		m_botAction.joinArena(configuration.getString("arena"));
	
	}
	
	public void handleEvent(PlayerEntered event){
	
		m_botAction.sendPrivateMessage(event.getPlayerName(), "Welcome to Gravitron's arena! Enjoy!!"); 
		
	}
	
	public void handleEvent(Message event){
//		this.commandInterpreter.handleEvent(event);

		String okay = event.getMessage();
	
		if(event.getMessageType() == Message.PRIVATE_MESSAGE){
		
			String nome = m_botAction.getPlayerName(event.getPlayerID());
		
			if(this.operatorList.isER(nome)){ //this will see which commands the staff typed to bot.
				
				try{
				
				verifyWhichCommandIs(nome, okay);
		
				}catch(Exception e){}}
	}																					
	
	}
	public void beforeEnds(){
		while(gravitronGame){
			if(countPlayers() == 1){
				botWins();
				m_botAction.sendArenaMessage("Playing players: "+countPlayers());
			}
		}
	}
	//This is the method to spec players when goes to safe during the game
	public void handleEvent(PlayerPosition event){
		
		if(gravitronGame == true){ //specs when game is running, otherwise it would spec before game starts and after game gets over..
			
			Iterator<Player> iterator = m_botAction.getPlayingPlayerIterator();
			for(; iterator.hasNext() ; iterator.next()){
				
				Player p = m_botAction.getPlayer( event.getPlayerID() );
				if(p.isInSafe()){
					m_botAction.specWithoutLock(p.getPlayerName());
				TimerTask tk = new TimerTask(){	
					public void run(){
					if(countPlayers() == 1){
						botWins();
						m_botAction.sendArenaMessage("Playing players: "+countPlayers());
					}	}
				};
				m_botAction.scheduleTask(tk, 1000);
				}
				
			}
			/*
			if (countPlayers() <= 2	){
				botWins();	//to last player in
			}
			//else if(countPlayers() <=2) botWins();
		*/}
	}
	public void botWins(){
		m_botAction.sendArenaMessage("Bot wins!");
		m_botAction.cancelTasks();
		this.gravitronGame = false;
		this.doResetGame(m_botAction.getBotName(),"");
		m_botAction.hideObject(1);
		m_botAction.hideObject(2);
		m_botAction.hideObject(3);
		m_botAction.hideObject(4);
		
	}
	

	
	public void verifyWhichCommandIs(String name, String mensagem){ //method to verify which command staff typed to bot
		if(mensagem.startsWith("!start")) startGravitron(name,mensagem); //starts game
		else if(mensagem.startsWith("!cancel")) this.doCancelGame(name, mensagem);//cancels game
		else if(mensagem.startsWith("!reset")) this.doResetGame(name, mensagem); //reset ships
		else if(mensagem.startsWith("!help")) this.doShowHelp(name, mensagem); //show help to staff
		else if(mensagem.startsWith("!sendrules")) this.doShowRules(name, mensagem); //show rules to players
		else if(mensagem.startsWith("!die")) this.die(name, mensagem); //bot dies
		else if(mensagem.startsWith("!go")) this.go(name, mensagem); //bot goes to an arena
	}
	
	//method to game && his logic
	public void startGravitron(String nome, String mensagem){
		//int timer1;
		//int timer2, timer3;
		
		//mod typed !start 
		//will analyze if the command has <time1> <time2> <time3> ...like !start 30 40 50 (seconds each stage of game)
	if(m_botAction.getArenaName().equalsIgnoreCase("gravitron")){
		if(countPlayers() > 0){
			StringTokenizer st = new StringTokenizer(mensagem);
			String t1, t2, t3, startcmd;
			
			startcmd = st.nextToken(); //this will be the first token: !start
			
			if(!st.hasMoreTokens()){ //if doesnt have any more command, host wants to be a standard game, then sets time to 60 30 20
				timer1 = 60;
				timer2 = 30;
				timer3 = 20;
			}
			
			else{ //else, mod has typed a specific time to each stage, we'll analyze it and start game with those timers.
			
				t1 = st.nextToken(); //time to stage 1
				t2 = st.nextToken(); //time to stage 2
				t3 = st.nextToken(); //time to stage 3
				this.timer1 = Integer.parseInt(t1); //converting each timer
				this.timer2 = Integer.parseInt(t2);
				this.timer3 = Integer.parseInt(t3);
				if(timer1 > 120) timer1 = 120;
				if(timer2 > 120) timer2 = 120;
				if(timer3 > 120) timer3 = 120;
				if(timer1 < 15) timer1 = 15;
				if(timer2 < 15) timer2 = 15;
				if(timer3 < 15) timer3 = 15;
			}
			
			preGameStarting(); //this is to do the "Starting in 10 seconds..." and set the configs to objon 1(first stage)
			TimerTask afterTenSeconds = new TimerTask(){
				public void run() {
					startObjon1();
					m_botAction.sendArenaMessage("Starting stage 1! " +timer1+ " seconds ... GOGOGOGOGO!!!! ", 104);
					gravitronGame = true; //Game has started, 10 seconds has passed, so, gravitronGame is true and it will enable the spec watch.
					
								}
			};
			
			TimerTask toStage2 = new TimerTask(){
				public void run(){
					/*if(countPlayers() == 1){
						m_botAction.cancelTasks();
						m_botAction.hideObject(1);
						m_botAction.hideObject(5);
						gravitronGame = false;
						m_botAction.spec(m_botAction.getBotName());
						m_botAction.sendArenaMessage("The Machine has Conquered all!", 22);
						checkWinner();
					}
					else{*/
					m_botAction.sendArenaMessage("Starting stage 2! " + timer2 + " seconds!", 22); //after the objon1, starts objon 2
					startObjon2();
				}
			};
			TimerTask toStage3 = new TimerTask(){
				public void run(){
				/*	if(countPlayers() == 1){
						m_botAction.hideObject(2);
						m_botAction.hideObject(5);
						gravitronGame = false;
						m_botAction.spec(m_botAction.getBotName());
						m_botAction.cancelTasks();
						m_botAction.sendArenaMessage("The Machine has Conquered all!", 22);
						checkWinner();
					}
					else{*/
					m_botAction.sendArenaMessage("Starting stage 3! " + timer3 + " seconds!", 22); //and after objon 2, objon 3 starts..
					startObjon3();
				}
			};
			TimerTask afterStage3 = new TimerTask(){
				public void run(){
					/*if(countPlayers() == 1){
						m_botAction.hideObject(5);
						m_botAction.hideObject(3);
						m_botAction.sendArenaMessage("The Machine has Conquered all!", 22);
						gravitronGame = false;
						m_botAction.spec(m_botAction.getBotName());
						m_botAction.cancelTasks();
						checkWinner();
					}
					else*/
						finishingGame(); //game over!
				}
			};
			
			m_botAction.scheduleTask(afterTenSeconds, 10000);
			m_botAction.scheduleTask(toStage2, 10000+timer1*1000);
			m_botAction.scheduleTask(toStage3, 10000+timer1*1000+timer2*1000);
			m_botAction.scheduleTask(afterStage3, 10000+timer1*1000+timer2*1000+timer3*1000);
	
			}
			else m_botAction.sendPrivateMessage(nome, "Can't start the game without players in!");
		}
	
	else m_botAction.sendPrivateMessage(nome, "Can't start, I'm not in gravitron's arena!");
}

	public void doShowHelp( String playerName, String message ) {
        m_botAction.remotePrivateMessageSpam(playerName,getModHelpMessage());
   
}
	

public  String[] getModHelpMessage() {
		String[] message = {
			"|--------------------------------------------------------------------------------|",
			"|  Commands for <ER>'s and Above                                                 |",
			"| !start                                        - Standard game of Gravitron     |",
			"| !start <timestage1> <timestage2> <timestage3> - Customizable game of Gravitron |",
			"| !cancel                                       - to Cancel Gravitron's game.    |",
			"| !reset                                        - Resets the arena..ships, etc.  |",
			"| !die                                          - Makes me die! :(               |",
			"| !go                                           - Sends me to an arena.          |",
			"| !sendrules                                    - Displays rules in arena message|",
	        "|----------------------- Authors: Dexter and Dezmond ----------------------------|"
	  };
	    return message;
	}

//LOGIC OF GAME	
public void preGameStarting(){
		m_botAction.toggleLocked();
		m_botAction.changeAllShips(1);
		m_botAction.setShip(m_botAction.getBotName(), 8);
		m_botAction.setAlltoFreq(0);
		m_botAction.setFreq(m_botAction.getBotName(), 322);	
		m_botAction.warpAllToLocation(512, 462);
		m_botAction.warpFreqToLocation(322, 512, 513);
		m_botAction.sendArenaMessage("Game begins in 10 seconds", 4);
	}
		
	public void startObjon1(){
		
		m_botAction.showObject(5);
		
		m_botAction.showObject(1);
		
		
		settingsObjon1();
	}

	public void startObjon2(){
		
		m_botAction.hideObject(1);
		m_botAction.showObject(2);
	
		settingsObjon2();
	}
	
	public void startObjon3(){
		
		m_botAction.hideObject(2);
		m_botAction.showObject(3);

		settingsObjon3();
		
    }

	public void finishingGame(){

		m_botAction.getShip().rotateRight(10);
		m_botAction.getShip().setVelocitiesAndDir(300, 100, 30);	
		m_botAction.getShip().fire(3); //bot bombs ...
	
		TimerTask toRep = new TimerTask(){

			@Override
			public void run() {

				if(countPlayers() > 1){
				
				m_botAction.getShip().fire(5); //after timertask, 2 seconds, bot reps and games is over!
				m_botAction.showObject(4);
				m_botAction.sendArenaMessage("KBOOM! Game over!", 22);
	
				}
				
				else{
					m_botAction.hideObject(4);
		//			m_botAction.sendArenaMessage("The Machine has Conquered all!", 22);
				}
			
				m_botAction.hideObject(3);
				m_botAction.hideObject(5);
				checkWinner();
				
				gravitronGame = false; //game over, so value false to disable spec watch.
				m_botAction.spec(m_botAction.getBotName());
				m_botAction.toggleLocked();
				
				settingsFinishingGame();
				m_botAction.shipResetAll();
				m_botAction.warpAllRandomly();
			}
			
		};
		
		m_botAction.scheduleTask(toRep, 2000);
		
	
	}
	
	public void die(String modname, String ymsg){
		
		if(this.operatorList.isER(modname)) m_botAction.die();
		
	}
	
	public void go(String modname, String ymsg){
		StringTokenizer st = new StringTokenizer(ymsg);
		String arena, cmd;
		cmd = st.nextToken();
		arena = st.nextToken();
		m_botAction.changeArena(arena);
	}
	
	
	/**
	 * Settings to each stages!!!
	*
	*/
	public void settingsObjon1(){

		m_botAction.sendUnfilteredPublicMessage("*ufo");
		m_botAction.sendUnfilteredPublicMessage("?set warbird:gravity:2000");
		m_botAction.sendUnfilteredPublicMessage("?set warbird:gravitytopspeed:200");
		//m_botAction.sendUnfilteredPublicMessage("?set warbird:gravity:0");
    //m_botAction.sendUnfilteredPublicMessage("?set warbird:gravitytopspeed:0");
	}
	
	public void settingsObjon2(){
		m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:-40");
    m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:2000");
  //  m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:16");
   // m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:2000");
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
    
    //m_botAction.setDoors(255);
	}
	
	public void doResetGame(String name, String message) {
	    if( m_botAction.getOperatorList().isER( name ) && gravitronGame == false ) {
	            m_botAction.hideObject(1);
	            m_botAction.hideObject(2);
	            m_botAction.hideObject(3);
	            m_botAction.sendUnfilteredPublicMessage("?set warbird:gravity:0");
	            m_botAction.sendUnfilteredPublicMessage("?set warbird:gravitytopspeed:0");
	            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:2000");  //max 6000
	            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:16");   //max 100
	            m_botAction.sendUnfilteredPublicMessage("?set warbird:initialrotation:200");//max 800
	            m_botAction.sendUnfilteredPublicMessage("?set Warbird:MaximumThrust=23");
	            m_botAction.sendUnfilteredPublicMessage("?set misc:bouncefactor:28");
	            m_botAction.sendUnfilteredPublicMessage("?set Door:DoorMode=0");
	            m_botAction.prizeAll(7);
	            m_botAction.sendPrivateMessage(name,"Settings reset");
	            m_botAction.toggleLocked();
	            gravitronGame = false;
	        }
	        else
	            m_botAction.sendPrivateMessage(name, "I will not reset the arena settings while a game is in progress");
	}

	    

	public void doCancelGame(String name, String message) {
	    if( gravitronGame == true) {
	    		
	    		m_botAction.hideObject(1);
	    		m_botAction.hideObject(2);
	    		m_botAction.hideObject(3);
	    		m_botAction.hideObject(5);
	    		m_botAction.sendUnfilteredPublicMessage("?set Door:DoorMode=0");
	    		m_botAction.sendUnfilteredPublicMessage("?set warbird:gravity:0");
          m_botAction.sendUnfilteredPublicMessage("?set warbird:gravitytopspeed:0");
          m_botAction.sendUnfilteredPublicMessage("?set warbird:initialspeed:2000");  //max 6000
          m_botAction.sendUnfilteredPublicMessage("?set warbird:initialthrust:16");   //max 100
          m_botAction.sendUnfilteredPublicMessage("?set Warbird:MaximumThrust=23");
    
          m_botAction.sendUnfilteredPublicMessage("?set warbird:initialrotation:200");//max 800
          m_botAction.sendUnfilteredPublicMessage("?set misc:bouncefactor:28");
        
	    		//m_botAction.setDoors(255);
	        m_botAction.prizeAll(7);
	        m_botAction.cancelTasks();
	        m_botAction.sendPrivateMessage(name,"Game Cancelled.");
	        m_botAction.shipResetAll();
	        m_botAction.toggleLocked();
	        gravitronGame = false;
	        m_botAction.sendArenaMessage("Gravitron has been cancelled.", 103);
	        
	    }
	    else m_botAction.sendPrivateMessage(name, "Game isn't running, there is no game to cancel!");
	}
  public void doShowRules(String name, String message) {
     m_botAction.sendArenaMessage("Rules: The player objective is to defeat the machine (Be last standing).");
     
     m_botAction.sendArenaMessage("Do not fall easy with his power! It will try hard to push you into the safes and get you out.");
     
     m_botAction.sendArenaMessage("Warning: Watch for walls turning on. If you are on a wall when turned on, you will be specced.");
 
     m_botAction.sendArenaMessage("Beware of the obstacles that come at you. It will require speed and tacticts. Good luck!", 9);      
       
  }


  //This will check the winners
  public void checkWinner(){
  	
  	
  	Iterator<Player> playerwinner = m_botAction.getPlayingPlayerIterator();
  	
  	int numberwinners = 0;
  	String winner = "The Machine has won!";
  	
  	if(playerwinner.hasNext()){ //if theres someone except bot
  		winner = "";
  		for( ; playerwinner.hasNext(); /*playeron = playerwinner.next()*/){ //if more than 1 winner
  			Player playeron = playerwinner.next(); //first player
  			String nameplayer = playeron.getPlayerName();
  			if(!nameplayer.equals(m_botAction.getBotName())){
  				winner += nameplayer+" and ";
  				numberwinners++; //more than 1 winner
  				
  			}
  		}
  		
  		try{
  			winner = winner.substring(0, winner.length()-5); //removes " and "
  		}catch(Exception e){ winner = "the machine has won!";}
  		
  		if(numberwinners == 1) m_botAction.sendArenaMessage("The winner is "+winner+ "!");
  		else if(numberwinners > 1) m_botAction.sendArenaMessage("The winners are " + 	winner +"!");
  		else m_botAction.sendArenaMessage("The Machine has Conquered all!", 22);
		 //none wins
    	
  	}
  	m_botAction.cancelTasks();
  	m_botAction.hideObject(1);
  	m_botAction.hideObject(2);
  	m_botAction.hideObject(3);
  	m_botAction.hideObject(4);
  	doResetGame(m_botAction.getBotName(), "reseted");
  	
  }

	public int countPlayers(){
		Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
		int numplayers = 0;
		for( ; i.hasNext(); i.next()) numplayers++;
		return numplayers;
	}
	}	
