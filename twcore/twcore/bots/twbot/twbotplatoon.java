/*
 * Created on 27/02/2005
 *
 */
package twcore.bots.twbot;

import java.util.*;
import twcore.core.*;

/**
 *  Module for the platoon type games SuperDAVE(postal) created.
 *  basically an inverted prodem type game where you want to stay alive,
 *  every time you die you get put down 1 ship until you get to ship 1.
 *  If you die as ship 1, you're out.
 *  @author - Jacen Solo
 *  @version 1.1
 */
public class twbotplatoon extends TWBotExtension
{
	HashSet exemptShips = new HashSet();
	boolean isRunning = false;
	
	public twbotplatoon() {}
	
	public void handleEvent(Message event)
	{
		String name = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());
        if (name == null) return;


        String message = event.getMessage();
        
        if(m_botAction.getOperatorList().isER(name))
        	handleCommand(name, message);
    }
    
    public void handleCommand(String name, String message)
    {
    	if(message.toLowerCase().startsWith("!start ") && !isRunning)
    	{
    		updateExempt(name, message);
    		startGame(name);
    	}
    	else if(message.toLowerCase().startsWith("!start") && !isRunning)
    		startGame(name);
    	else if(message.toLowerCase().startsWith("!cancel") && isRunning)
    		cancelGame(name);
    	else if(message.toLowerCase().startsWith("!exempt "))
    		updateExempt(name, message);
    }
    
    public void updateExempt(String name, String message)
    {
    	exemptShips = new HashSet();
    	
    	String pieces[] = message.split(" ", 2);
    	String pieces2[] = pieces[1].split(":");
    	for(int k = 0;k < pieces2.length;k++)
    	{
    		try {
    			if(Integer.parseInt(pieces2[k]) < 9 && Integer.parseInt(pieces2[k]) > 0)
    				exemptShips.add(new Integer(Integer.parseInt(pieces2[k])));
    		} catch(Exception e) {}
    	}
    	
    	String exemptShipList = "Exempt ships are: ";
    	Iterator it = exemptShips.iterator();
    	while(it.hasNext())
    	{
    		exemptShipList += " " + ((Integer)it.next()).intValue();
    	}
    	m_botAction.sendPrivateMessage(name, exemptShipList);
    }
    
    public void handleEvent(PlayerDeath event)
    {
    	if(!isRunning) return;
    	
    	int ship = m_botAction.getPlayer(event.getKilleeID()).getShipType();
    	
    	if(!exemptShips.contains(new Integer(ship)))
    	{
    		ship--;
    		
    		while(exemptShips.contains(new Integer(ship)))
    			ship--;
    		
    		if(ship == 0)
    		{
    			m_botAction.spec(event.getKilleeID());
    			m_botAction.spec(event.getKilleeID());
    			m_botAction.sendArenaMessage(m_botAction.getPlayerName(event.getKilleeID()) + " is out!");
    			Iterator it = m_botAction.getPlayingPlayerIterator();
		    	int players = 0;
		    	int freqs = 0;
		    	HashSet freqList = new HashSet();
		    	
		    	while(it.hasNext()) { it.next(); players++; Player p = (Player)it.next(); if(!freqList.contains(new Integer(p.getFrequency()))) freqList.add(new Integer(p.getFrequency()));}
		    	
		    	if(players == 1)
		    		gameOver(true);
		    	else if(freqList.size() == 1)
		    		gameOver(false);
		    }
		    else
		    {
		    	final int kID = event.getKilleeID();
		    	final int ship2 = ship;
		    	TimerTask shipChange = new TimerTask()
		    	{
		    		public void run()
		    		{
		    			m_botAction.setShip(kID, ship2);
		    		}
		    	};
		    	m_botAction.scheduleTask(shipChange, 5  * 1000);
		    }
		}
    }
    
    public void handleEvent(PlayerLeft event)
    {
    	if(!isRunning)
    		return;
    	Iterator it = m_botAction.getPlayingPlayerIterator();
		int players = 0;
		int freqs = 0;
		HashSet freqList = new HashSet();
		    	
		while(it.hasNext()) { it.next(); players++; Player p = (Player)it.next(); if(!freqList.contains(new Integer(p.getFrequency()))) freqList.add(new Integer(p.getFrequency()));}
		    	
		if(players == 1)
			gameOver(true);
		else if(freqList.size() == 1)
			gameOver(false);
    }
    
    public void handleEvent(FrequencyShipChange event)
    {
    	if(!isRunning)
    		return;
    	if(event.getShipType() == 0)
    	{
    		Iterator it = m_botAction.getPlayingPlayerIterator();
		    int players = 0;
		    int freqs = 0;
		    HashSet freqList = new HashSet();
		    
		    while(it.hasNext()) { it.next(); players++; Player p = (Player)it.next(); if(!freqList.contains(new Integer(p.getFrequency()))) freqList.add(new Integer(p.getFrequency()));}
		    
		    if(players == 1)
		    	gameOver(true);
		    else if(freqList.size() == 1)
		    	gameOver(false);
	    }
	}
	
	public void cancelGame(String name)
	{
		m_botAction.sendArenaMessage("This game has been killed by: " + name, 13);
		isRunning = false;
	}
	
	public void gameOver(boolean onePlayer)
	{
		Iterator it = m_botAction.getPlayingPlayerIterator();
    	String winner = "";
    	while(it.hasNext())
    	{
    		Player p = (Player)it.next();
    		if(onePlayer)
    			winner = p.getPlayerName();
    		else
    			winner = "Freq " + p.getFrequency();
    	}
    	
    	m_botAction.sendArenaMessage(winner + " has won the game!", 5);
    	isRunning = false;
    }
    
    public void startGame(String name)
    {
    	m_botAction.sendArenaMessage(name + " has started platoon mode!");
    	m_botAction.sendArenaMessage("Game begins in 10 seconds!", 2);
    	
    	TimerTask five = new TimerTask()
    	{
    		public void run()
    		{
    			m_botAction.sendArenaMessage("5");
    		}
    	};
    	TimerTask three = new TimerTask()
    	{
    		public void run()
    		{
    			m_botAction.sendArenaMessage("3");
    		}
    	};
    	TimerTask two = new TimerTask()
    	{
    		public void run()
    		{
    			m_botAction.sendArenaMessage("2");
    		}
    	};
    	TimerTask one = new TimerTask()
    	{
    		public void run()
    		{
    			m_botAction.sendArenaMessage("1");
    		}
    	};
    	TimerTask go = new TimerTask()
    	{
    		public void run()
    		{
    			m_botAction.sendArenaMessage("GOOO GO GO GO GOOOOO!", 104);
    			isRunning = true;
    		}
    	};
    	
    	m_botAction.scheduleTask(five, 5000);
    	m_botAction.scheduleTask(three, 7000);
    	m_botAction.scheduleTask(two, 8000);		
    	m_botAction.scheduleTask(one, 9000);
    	m_botAction.scheduleTask(go, 10000);
    }
    
    public void cancel() {}
    
    public String[] getHelpMessages()
    {
    	String helps[] = {
    		"!start a:b:c:etc       -Starts a game with ships a, b, and c exempt from ship changes.",
    		"!start                 -Starts a game with no ships exempt.",
    		"!cancel                -Cancels current game.",
    		"!exempt a:b:c:d:etc    -Changes ships exempt to a, b, c, and d. You can add as many as you want, they just need to be seperated by :'s"
    	};
    	
    	return helps;
    }
}
