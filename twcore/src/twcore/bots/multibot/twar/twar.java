package twcore.bots.multibot.twar;

/*
 * Author: Dexter
 * Thanks to turban who gave the idea of ships promotion to turretwars event*/
import java.util.StringTokenizer;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.TurretEvent;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/*--------------- Bot to any TURRETWARs event ------
	. it has !start command which uses *scorereset, *shipreset, !random2 and *lock 's commands before start.
	. each ER+ has to *arena GO GO GO after the timertask(10 seconds)
	. command to set terr is !terr name is before the !start command
	. command to switch terr is !terr name1 newTerr is after !start command
	. command to warpback is !warp, so anyone who detaches terr will get warped and energy depleted.
	. command to ships promotion is !prom, the line is: spider->lancaster->javelin->warbird (30 kills lanc, 45 kills jav and 60 kills warbird)
	. command to stop game is !stop, so after game ends, it stops the game with 'turretWar = false;'
	. command to ER+ guide to use this Bot to host is !guide
	. commands of others utilities/bots are OTHERS: !setupwarplist and !setupwarp to set safes warping before game starts and !setship #ship 
	if not using !prom, because you'll set #ship by your own.
	--------------- Turret War ----------------------
*/
public class twar extends MultiModule
{
	private boolean promotion = false;
	private boolean turretWar = false;
	private boolean warpOn = false;
	
	//guide to ER+s
	private String guideER[] = 
	{
			"This is a guide to any turretwar hosting",
			"1.!start command WILL: *SCORERESET, *SHIPRESET, *LOCK and !random 2(CREATES 2 TEAMS)",
			"2.You can host it with SHIP promotions or Not - use '!prom' to enable it",
			"3.Use !setship #ship if you're not using promotion, it will set EVERYONE into a ship.",
			"4.You should set two terrs BEFORE game starts - use '!terr <name>' to pick each terr",
			"5.You can switch terrs while the game is RUNNING - use '!terr <player1> <newTERR> <#ShipPlayer1>' to switch",
			"6.If you're using promotion, then just !terr <player1> <newTERR> to switch",
			"7.You can set warp on, so when someone DETACHES a terr, he gets warped - use !warp to enable warpback",
			"8.Use !setupwarplist and check what command warps freqs into safes.",
			"9.Use !stop to stop the game - it will UNLOCK arena and stop the game."
	};
	public void handleEvent(TurretEvent event)
	{
		if(!event.isAttaching()){
			if(warpOn){ //if any player detaches, he'll get warped.
				int idDetacher = event.getAttacherID();
				m_botAction.specificPrize(idDetacher, 7);
				m_botAction.specificPrize(idDetacher, -13);
			}
		}
	}
	public void handleEvent(PlayerDeath event)
	{
			if(promotion)
			{
			Player killer = m_botAction.getPlayer(event.getKillerID());
			handlePromotion(killer.getPlayerName(), (int) killer.getWins());
			}
	}
	public void handleEvent(Message event)
	{
		
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
			String message = event.getMessage();
			String name = m_botAction.getPlayerName(event.getPlayerID());
			
			if(opList.isER(name))
				handleCommand(name, message);
		
		}
	}

	public void handleCommand(String name, String message)
	{
		try
		{
			if(message.toLowerCase().startsWith("!start")) startTurretWar(name, message);
			else if(message.toLowerCase().startsWith("!terr")) setTerr(name, message);
			else if(message.toLowerCase().startsWith("!prom")) promShip(name, message);
			else if(message.toLowerCase().startsWith("!stop")) stopTurretWar(name, message);
			else if(message.toLowerCase().startsWith("!warp")) warpDetachedPlayer(name, message);
			else if(message.toLowerCase().startsWith("!guide")) showGuide(name, message);
		}catch(Exception e){}
	}
	public void handlePromotion(String name, Integer kills)
	{
		if(kills == 30)
			{
			m_botAction.setShip(name, 7);
			m_botAction.sendArenaMessage(name+" got promoted to lancaster, get 45 kills to next promotion!", 2);
			}
		else if(kills == 45)
		{
			 m_botAction.setShip(name, 2);
			 m_botAction.sendArenaMessage(name+" got promoted to javelin, get 60 kills to next promotion!", 2);
		}
		else if(kills == 60) 
		{
			m_botAction.setShip(name, 1);
			m_botAction.sendArenaMessage(name+" is owning! He just got promoted to warbird!", 2);
		}
	}
	public void startTurretWar(String name, String message)
	{
		m_botAction.sendArenaMessage("Locking in 10 seconds, get ready!", 2);
		m_botAction.sendPrivateMessage(name, "locking arena in 10 seconds and it'll start.");
		m_botAction.sendPrivateMessage(name,"Please *arena GO after it");
		m_botAction.sendPrivateMessage(name ,"and type *timer <number> to SET TIMER.");
		
		TimerTask starting = new TimerTask(){
			public void run(){
					m_botAction.toggleLocked();
					m_botAction.createNumberOfTeams(2);
					m_botAction.scoreResetAll();
					m_botAction.shipResetAll();
					turretWar = true;
					}
		};m_botAction.scheduleTask(starting, 10000);
	}
	
	public void setTerr(String name, String message)
	{
		String terr1Name = null;
		String terr2Name = null;
		String ship = null;
		StringTokenizer terrNames = new StringTokenizer(message);
		
		int shipn;
		boolean ok = false; //the ok to switch terrs
		
		terrNames.nextToken(); //command !terr is first token
		
		terr1Name = terrNames.nextToken(); //then it'll have the first name
		
		if(terrNames.hasMoreTokens())//if there is another token, it should be the second name(then it is to SWITCH)
		{
			if(turretWar)//will switch just if game is ON
			{ //switch...
				terr2Name = terrNames.nextToken(); //second name
				if(promotion)//If promotion is on, then the old terr will be SPIDER
					{
					m_botAction.setShip(terr1Name, 3);
					ok = true; //ok switch
					}
					
				else //else, the host will need to choose a #ship to old terr
				{
					if(terrNames.hasMoreTokens()) //#ship
					{
						ship = terrNames.nextToken();
						shipn = Integer.parseInt(ship); //getting #ship
						m_botAction.setShip(terr1Name, shipn); //putting player to ship the host wanted
						ok = true; //ok switch
					}
					else //otherwise, he havent typed a #ship and its promotion is off. then he has to type a #ship
						m_botAction.sendPrivateMessage(name, "Type a ship number to "+terr1Name+" be!" );
				}
				
				if(ok)//if switch ok, we'll arena it
				{
				m_botAction.sendArenaMessage( terr1Name+", get back!");
				m_botAction.sendArenaMessage(terr2Name + " is your new terr!", 24);
				m_botAction.setShip(terr2Name, 5);
				}
				
			//resetting scores	
			m_botAction.scoreReset(terr1Name); //score reseting old terr
			m_botAction.scoreReset(terr2Name); //score reseting new terr
		
		}else m_botAction.sendPrivateMessage(name, "Just possible to switch while game is RUNNING!"); //if game isnt running, can't switch.
			
	}else //if it doesnt have any more turret, it is the pick before start !terr playername
		{
			if(!turretWar)//and it will just pick a terr before game starts
			{
			m_botAction.sendArenaMessage(terr1Name + " is your terr!", 5);
			m_botAction.setShip(terr1Name, 5);
			}
			else //can't use the command !terr player after game started. it should be the switch command !terr name1 newTerr
				m_botAction.sendPrivateMessage(name, "type the complete command to switch. !terr player1 newterr #shipPlayer1");
		}
	}
	
	//setter of !warp
	public void warpDetachedPlayer(String name, String message)
	{
		if(warpOn) warpOn = false;
		else{
			warpOn = true;
			m_botAction.sendPrivateMessage(name, "Allright! any players dettaching will be warped!");
		}
	}
	
	//setter of !prom
	public void promShip(String name, String message)
	{
		if(promotion && turretWar) //can cancel promotion while game running
			promotion = false;
		else if(!promotion && !turretWar)
		{ //setting promotion on before game starts
			promotion = true;
			m_botAction.sendArenaMessage("Promotion is on! First you'll be spiders. Get 30 kills to be promoted to lancaster!", 21 );
			m_botAction.changeAllShips(3);
		}
		else if(!promotion && turretWar) //if game has started, can't start promotion after it.
			m_botAction.sendPrivateMessage(name, "Can't start promotion while game is running. Do it before game starts please.");
	}
	public void cancel() {}

	public String[] getModHelpMessage() 
	{
		String erHelp [] = {
			"|	!guide 																					 - A GUIDE TO HOST TURRETWAR",
			"|	!terr NAME																			- To pick a terr",
			"|	!terr name1 Terr	  										- To switch terrs if promotion on",
			"|	!terr name1 Terr #ship1		 			- To switch terrs promotion if off",
			"|	!prom																								- to enable promotion",	
			"|	!start																							- to start game (random 2, *scorereset, *shipreset and *lock commands)",
			"|	!warp																								- to enable warpback",
			"|	 OTHERS:",
			"|| !setship #ship														- to set everyone in a ship",
			"|| !setupwarplist														- to see warp safes list",
			
		};
		return erHelp;
	}
	
	//showing Guide to ER+s
	public void showGuide(String name, String message){
		m_botAction.sendPrivateMessage(name, guideER[0]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[1]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[2]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[3]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[4]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[5]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[6]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[7]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[8]);
		m_botAction.sendPrivateMessage(name, "");
		m_botAction.sendPrivateMessage(name, guideER[9]);
	}
	//stopping game.
	public void stopTurretWar(String name, String message){
		turretWar = false;
		m_botAction.cancelTasks();
		m_botAction.toggleLocked();
		m_botAction.sendPrivateMessage(name, "game stopped.");
	}
	public void init() {
	}

	public boolean isUnloadable() {
		return !turretWar;
	}

	public void requestEvents(ModuleEventRequester eventRequester) {
		eventRequester.request(this, EventRequester.PLAYER_DEATH);
	  eventRequester.request(this, EventRequester.MESSAGE);
	}
	
}
