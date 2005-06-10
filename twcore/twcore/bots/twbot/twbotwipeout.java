/*TWBot Wipeout Module
 *
 *Built by: Jacen Solo
 */
 

package twcore.bots.twbot;

import java.util.*;
import twcore.core.*;

public class twbotwipeout extends TWBotExtension
{
	boolean isRunning = false;
	int speed = 30;
	HashSet players = new HashSet();
	HashSet playersKicked = new HashSet();
	HashSet gotKill = new HashSet();
	TimerTask starter;
	TimerTask specer;
	TimerTask tenSecWarn;
	
	public twbotwipeout()
	{
	}
	
	public void handleEvent(Message event)
	{
		String message = event.getMessage();
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if(m_opList.isER(name))
				handleCommand(name, message);
		}
	}
	
	public void handleEvent(PlayerDeath event)
	{
		String name = m_botAction.getPlayerName(event.getKillerID());
		if(!gotKill.contains(name))
			gotKill.add(name);
	}
	
	public void handleEvent(PlayerLeft event)
	{
		String name = m_botAction.getPlayerName(event.getPlayerID());
		if(players.contains(name))
			players.remove(name);
		if(playersKicked.contains(name))
			playersKicked.remove(name);
		if(gotKill.contains(name))
			gotKill.remove(name);
	}
	
	public void handleEvent(FrequencyShipChange event)
	{
		if(event.getShipType() == 0)
		{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if(players.contains(name))
				players.remove(name);
			if(playersKicked.contains(name))
				playersKicked.remove(name);
			if(gotKill.contains(name))
				gotKill.remove(name);
		}
	}

	public void handleCommand(String name, String message)
	{
		if(message.toLowerCase().startsWith("!start") && !isRunning)
			handleStart(name, message);
		else if(message.toLowerCase().startsWith("!stop") && isRunning)
			handleStop(name);
	}
	
	public void handleStart(String name, String message)
	{
		String pieces[] = message.split(" ");
		int time = 30;
		try
		{
				time = Integer.parseInt(pieces[1]);
				if(time < 10)
					time = 10;
				if(time > 180)
					time = 180;
		}
		catch (Exception e ) {}
		speed = time;
		m_botAction.sendArenaMessage("Wipeout started by: " + name);
		m_botAction.sendArenaMessage("You must get a kill every " + speed + " seconds or you will be spec'd!");
		m_botAction.sendArenaMessage("Game begins in 10 seconds.",1);
		setupTimerTasks();
		m_botAction.scheduleTask(starter, 10 * 1000);
		m_botAction.scheduleTaskAtFixedRate(specer, speed * 1000 + 10 * 1000, speed * 1000);
		m_botAction.scheduleTaskAtFixedRate(tenSecWarn, speed * 1000, speed * 1000);
	}
	
	public void handleStop(String name)
	{
		isRunning = false;
		m_botAction.sendArenaMessage("Wipeout stopped by: " + name, 13);
		end();
		
	}
	
	public void handleTie(HashSet winnerz)
	{
		Iterator it = winnerz.iterator();
		m_botAction.sendArenaMessage("We have a tie!!!", 5);
		m_botAction.sendArenaMessage("Winners are: ");
		while(it.hasNext())
		{
			m_botAction.sendArenaMessage(String.valueOf(it.next()));
		}
		end();
	}
	
	public void handleWin()
	{
		Iterator it = players.iterator();
		m_botAction.sendArenaMessage("Game over!", 5);
		m_botAction.sendArenaMessage(String.valueOf(it.next()) + " has won this round. Congratulations!");
		end();
	}
	
	public void spec()
	{
		Iterator it = players.iterator();
		
		while(it.hasNext())
		{
			String name = (String)it.next();
			if(!gotKill.contains(name))
			{
				m_botAction.spec(name);
				m_botAction.spec(name);
				playersKicked.add(name);
				players.remove(name);
			}
		}
		if(players.size() == 1)
			handleWin();
		else if(players.isEmpty())
			handleTie(playersKicked);
		playersKicked.clear();
		gotKill.clear();
	}
			
	public void end()
	{
		m_botAction.toggleLocked();
		isRunning = false;
		speed = 60;
		players = new HashSet();
		playersKicked = new HashSet();
		gotKill = new HashSet();
		specer.cancel();
		tenSecWarn.cancel();	
	}	
	
	void setupTimerTasks()
	{
		specer = new TimerTask()
		{
			public void run()
			{
				m_botAction.sendArenaMessage("WIPEOUT!!!", 13);
				spec();
			}
		};
		
		starter = new TimerTask()
		{
			public void run()
			{
				m_botAction.scoreResetAll();
				m_botAction.toggleLocked();
				m_botAction.sendArenaMessage(" Goooooo go go go goooooooo",104);
				isRunning = true;
				
				Iterator it = m_botAction.getPlayingPlayerIterator();
				while(it.hasNext())
				{
					Player p = (Player) it.next();
					players.add(p.getPlayerName());
				}
			}
		};
		
		tenSecWarn = new TimerTask()
		{
			public void run()
			{
				m_botAction.sendArenaMessage("10 seconds until wipeout!", 7);
			}
		};
	}
	
	public String[] getHelpMessages()
	{
		String[] help = {
			"!start                         - Starts a game of wipeout with speed of 60.",
			"!start <time>                  - Starts a game of wipeout with set speed.",
			"!stop                          - Stop a game of wipeout.",
		};
		return help;
	}
	
	public void cancel()
	{
	}
}
