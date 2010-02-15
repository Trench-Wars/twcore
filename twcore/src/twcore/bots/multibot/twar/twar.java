package twcore.bots.multibot.twar;

/*
 * Author: Dexter
 * Thanks to turban who gave the idea of ships promotion to turretwars event*/
import java.util.ArrayList;
import java.util.Collections;

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
	. it has !start command which uses *scorereset, *shipreset and *lock 's commands before start.
	. each ER+ has to MANUALLY *arena GO GO GO after the timertask(10 seconds)
	. command to set everyone as a ship is !ship (old !setship)
	. command to set terr is !terr name
	. command to switch terr is !switch name:newTerr
	. command to warpback is !warp, so anyone who detaches terr will get warped and energy depleted.
	. command to ships promotion is !prom ship:kills
	. command to stop game is !stop, so after game ends, it stops the game with 'turretWar = false;' and others booleans too.
	. command to ER+ guide to use this Bot to host is !guide
	. commands of others utilities/bots are OTHERS: !setupwarplist and !setupwarp to set safes warping before game starts and !setship #ship 
	if not using !prom, because you'll set #ship by your own with the old !setship command
	
	. any player can type !lprom to bot so he'll get back the ship promotions list
	--------------- Turret War ----------------------
*/
public class twar extends MultiModule
{
	
	private boolean promotion = false;
	private boolean turretWar = false;
	private boolean warpOn = false;
	private boolean freq1Terr = false;
	private boolean freq0Terr = false;
	
	private int firstShip = 0;
	
	private ArrayList<shipSettings> listShipSettings = new ArrayList() ;
	
	//guide to ER+s
	private String guideER[] = 
	{
			"1.  Use !terr name to pick terr",
			"2.  Use !switch name:newTerr to switch terrs",
			"3.  Use !prom ship:kills to set a promotion",
			"4.  Use !ship number to set everyone in this ship",
			"5.  Use !start to: scorereset, shipreset and lock",
			"6.  Use !warp to make detachers get warped to spawn area - old !warpon command ",
			"7.  do manually the *arena GO GO GO and *timer <numberMinutes>",
			"8.  Use !setship #ship to set the starter ship (using promotions or not)",
			"9.  Use !setupwarplist to check the warp command",
			"10.  When you use !terr name at first time, it'll automatically !random 2, so don't worry about the old !random 2",
			"11. !prom ship:kills is AUTO-SORTED by KILLS in the LIST !lprom"
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
			handleCustomPromotion(killer.getPlayerName(), (int) killer.getWins());
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
			else
			{
				if(event.getMessage().toLowerCase().startsWith("!help"))
				{
					if(promotion)
						m_botAction.sendPrivateMessage(name,"!lprom           - to check ship promotions");
				}
				else if(event.getMessage().toLowerCase().startsWith("!lprom"))
					listPromotion(name, message);
			}
		}
	}

	public void handleCommand(String name, String message)
	{
		try
		{
			if(message.toLowerCase().startsWith("!start")) startTurretWar(name, message);
			else if(message.toLowerCase().startsWith("!terr")) setTerr(name, message);
			else if(message.toLowerCase().startsWith("!ship")) setFirstShip(name, message);
			else if(message.toLowerCase().startsWith("!prom")) setPromotion(name, message);
			else if(message.toLowerCase().startsWith("!lprom")) listPromotion(name, message);
			else if(message.toLowerCase().startsWith("!stop")) stopTurretWar(name, message);
			else if(message.toLowerCase().startsWith("!warp")) warpDetachedPlayer(name, message);
			else if(message.toLowerCase().startsWith("!guide")) showGuide(name, message);
			else if(message.toLowerCase().startsWith("!switch")) switchTerr(name, message);
		}catch(Exception e){}
	}
	
	public void handleCustomPromotion(String name, Integer kills)
	{
		for(int i = 0; i < listShipSettings.size() ; i++ )//loop to check promotions in the sorted list by kills
		{
			shipSettings e = listShipSettings.get(i);
			
			if(i == listShipSettings.size() - 1)
			{
				if(kills >= e.getKill()) //this is to the last ship promotion(the last limit of kills)
					if(!m_botAction.getPlayer(name).isShip(e.getShip()))
					{
						m_botAction.setShip(name, e.getShip());
						m_botAction.sendArenaMessage(name+" is owning! He just got promoted to "+e.getShipName()+"!", 7);
					}
			}
			
		 else if(kills >= e.getKill() && kills < listShipSettings.get(i+1).getKill())//to any other promotion(needs 2 limits of kills)
				if(!m_botAction.getPlayer(name).isShip(e.getShip()))
				{
					m_botAction.setShip(name, e.getShip());
					m_botAction.sendArenaMessage(name+" got promoted to "+e.getShipName()+"!", 21);
				}
		}
	}
	
	public void setFirstShip(String name, String message)
	{
		if(!freq1Terr && !freq0Terr)
		{//!ship 3
			this.firstShip = Integer.parseInt( message.substring(6) );
			m_botAction.changeAllShips(firstShip);
		}
		else m_botAction.sendPrivateMessage(name, "Use !setship before the command !terr please. Now !stop and start all again.");
	}
	
	public void startTurretWar(String name, String message)
	{
		m_botAction.sendArenaMessage("Locking in 10 seconds, get ready!", 13);
		m_botAction.sendPrivateMessage(name, "locking arena in 10 seconds and it'll start.");
		m_botAction.sendPrivateMessage(name,"Please *arena GO after it");
		m_botAction.sendPrivateMessage(name ,"and type *timer <number> to SET TIMER.");
		
		TimerTask starting = new TimerTask(){
			public void run(){
					m_botAction.toggleLocked();
					m_botAction.scoreResetAll();
					m_botAction.shipResetAll();
					turretWar = true;
					}
		};m_botAction.scheduleTask(starting, 10000);
	}
	
	public int getFreq( String player )
	{
		return m_botAction.getPlayer(player).getFrequency();
	}
	
	public void setTerr(String name, String message)
	{ //setting terrs
		StringTokenizer st = new StringTokenizer(message);
		String terrName;
		st.nextToken();
		terrName = st.nextToken();
		
		if(!freq1Terr && !freq0Terr)
			m_botAction.createNumberOfTeams(2);
		
		if(getFreq(terrName) == 1)
			if(!freq1Terr)
				{
					m_botAction.setShip(terrName, 5);
					m_botAction.sendArenaMessage(terrName+" is your the terr!",1);
					freq1Terr = true;
				}else m_botAction.sendPrivateMessage(name, "Freq 1 has a terr already, try to use !switch please");
		
		else if(getFreq(terrName) == 0)
			if(!freq0Terr)
			{
				m_botAction.setShip(terrName, 5);
				m_botAction.sendArenaMessage(terrName+ " is your the terr!", 1);
				freq0Terr = true;
			}else m_botAction.sendPrivateMessage(name, "Freq 0 has a terr already, try to use !switch please.");
	}
	
	public void switchTerr( String name, String message )
	{ //switching terrs
		if(turretWar){	
			String oldTerr;
			String newTerr;
			String [] terr;
			terr = message.split(":", 2);
			oldTerr = terr[0].substring(8).toLowerCase();
			newTerr = terr[1].toLowerCase();
			
			if(getFreq(oldTerr) == getFreq(newTerr))
				{
					m_botAction.setShip(newTerr, 5);
					m_botAction.scoreReset(newTerr);
					m_botAction.sendArenaMessage(newTerr+ " is the new terr to the freq "+getFreq(newTerr)+"!", 21);
					m_botAction.setShip(oldTerr, firstShip); //set back to the first ship of !setship command
					m_botAction.scoreReset(oldTerr);
				}else m_botAction.sendPrivateMessage(name, "Choose a new terr from the same frequence please.");
			}else m_botAction.sendPrivateMessage(name, "Game hasn't started. Start it first please.");
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
	
	public void setPromotion( String name, String message )
	{
		if(!promotion)
		{
			promotion = true;
			m_botAction.sendArenaMessage("Yes! Ship Promotions ON!", 24);
		}
		
		String messageSplit [] = message.split(":");
		
		shipSettings object = new shipSettings();
		
		object.setShip( Integer.parseInt(messageSplit[0].substring(6)) );
		object.setKill( Integer.parseInt(messageSplit[1]) );
		
		listShipSettings.add(object);
		Collections.sort(listShipSettings);
		
	}	
	public void cancel() {}

	public void listPromotion(String name, String message)
	{
		m_botAction.sendPrivateMessage(name,"------------------------------------------");
		for(shipSettings e:listShipSettings)
			m_botAction.sendPrivateMessage(name, "| be a "+e.getShipName()+ " with "+e.getKill()+" kills");
		
		m_botAction.sendPrivateMessage(name,"------------------------------------------");	
	}
	public String[] getModHelpMessage() 
	{
		String erHelp [] = {
			"|	!guide 																					 																														- A GUIDE TO HOST TURRETWAR",
			"| !ship number																																															- to set everyone in this ship",																				
			"|	!terr name																																																	- to pick a terr",
			"|	!switch name:terr																																										- to switch terrs",
			"|	!prom	ship:kill																																												- to enable promotion",	
			"| !lprom																																																					- to check the list of promotions",
			"|	!start																																																					- to start game (*scorereset, *shipreset and *lock)",
			"|	!warp																																																						- to enable warpback",
			"|	 OTHERS:",
			"|| !setship #ship																																												- to set everyone in a ship",
			"|| !setupwarplist																																												- to see warp safes list",
			
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
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[9]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[10]);
	
	}
	//stopping game.
	public void stopTurretWar(String name, String message){
		turretWar = false;
		promotion = false;
		warpOn = false;
		freq1Terr = false;
		freq0Terr = false;
		listShipSettings.clear();
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
	}
	
}
