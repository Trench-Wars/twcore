package twcore.bots.multibot.wipeout;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.ArrayList;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.FrequencyShipChange;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/**
 * @author Jacen Solo modded by milosh
 */
public class wipeout extends MultiModule{
	boolean isRunning = false, isSuddenDeath = false;;
	int speed = 30;
	double initialPlayers;
	OperatorList opList;
	HashSet<String> players = new HashSet<String>();
	HashSet<String> gotKill = new HashSet<String>();
	TimerTask starter;
	
	String[] opmsg={
			"!start                         - Starts a game of wipeout with speed of 30.",
			"!start <time>                  - Starts a game of wipeout with set speed.",
			"!stop                          - Stop a game of wipeout.",	
	};

	public void init(){
		opList = m_botAction.getOperatorList();
	}
	public void requestEvents(ModuleEventRequester events) {
		events.request(this, EventRequester.PLAYER_DEATH);
		events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
		events.request(this, EventRequester.PLAYER_LEFT);
	}
	public  String[] getModHelpMessage(){return opmsg;}
	public boolean isUnloadable() {return true;}

	public void handleEvent(Message event)
	{
		String message = event.getMessage();
		String name = m_botAction.getPlayerName(event.getPlayerID());
		if(message.equalsIgnoreCase("!lagout") && players.contains(name) && isRunning){
			Player p = m_botAction.getPlayer(name);
			if( p == null )return;
			if(p.getShipType() == 0)m_botAction.setShip(name, 1);
		}			
		if(event.getMessageType() == Message.PRIVATE_MESSAGE && opList.isER(name))
				handleCommand(name, message);
	}

	public void handleEvent(PlayerDeath event){
		if(isSuddenDeath)
			m_botAction.specWithoutLock(m_botAction.getPlayerName(event.getKilleeID()));
		String name = m_botAction.getPlayerName(event.getKillerID());
		if(!gotKill.contains(name))
			gotKill.add(name);

	}

	public void handleEvent(PlayerLeft event){
	    if(!isRunning)return;
	    String name = m_botAction.getPlayerName(event.getPlayerID());
	    if(name == null)return;
	    players.remove(name);
	    if(players.size() == 1)
            handleWin(players.toString().replace("]", "").trim());
	}
	
	public void handleEvent(FrequencyShipChange event){
	    if(!isRunning)return;
	    String name = m_botAction.getPlayerName(event.getPlayerID());
        if(name == null)return;
        if(event.getShipType() == 0)
            players.remove(name);
        if(players.size() == 1)
            handleWin(players.toString().replace("]", "").trim());
	}
	
	public void handleCommand(String name, String message)
	{
		if(message.toLowerCase().startsWith("!start ") && !isRunning)
			handleStart(name, message.substring(7));
		else if(message.equalsIgnoreCase("!start") && !isRunning)
			handleStart(name, "30");
		else if(message.equalsIgnoreCase("!stop"))
			handleStop(name);
	}

	public void handleStart(String name, String message)
	{
		int time;
		try
		{
				time = Integer.parseInt(message);
				if(time < 10)
					time = 10;
				if(time > 180)
					time = 180;
		}
		catch (Exception e ) {time = 30;}
		speed = time;
		m_botAction.sendArenaMessage("Wipeout started by: " + name);
		m_botAction.sendArenaMessage("You must get a kill every " + speed + " seconds or you're out! Time will decrease as the game progresses!");
		m_botAction.sendArenaMessage("Game begins in 10 seconds.",1);
		cancel();
		setupTimerTasks();
		m_botAction.scheduleTask(starter, 10 * 1000);
	}

	public void handleStop(String name)
	{
		m_botAction.sendArenaMessage("Wipeout stopped by: " + name, 13);
		cancel();

	}

	public void handleWin(String name)
	{
		name = name.replace("[", "").trim();
		m_botAction.sendArenaMessage("Game over!", 5);
		m_botAction.sendArenaMessage(name + " has won this round. Congratulations!");
		cancel();
	}

	public void spec()
	{		
		if(gotKill.isEmpty()){
		    if(isRunning)
		        m_botAction.sendArenaMessage("No one got a kill, time extended.");
			return;
		}
		Iterator<String> it = players.iterator();
		ArrayList<String> wipedOut = new ArrayList<String>();
		while(it.hasNext())
		{
			String name = it.next();
			if(!gotKill.contains(name))
			{
				m_botAction.specWithoutLock(name);
				m_botAction.sendSmartPrivateMessage(name, "You've been wiped out!");
				wipedOut.add(name);
			}			
		}
		Iterator<String> i = wipedOut.iterator();
		while(i.hasNext())
			players.remove(i.next());
		if(players.size() == 1)
			handleWin(players.toString().replace("]", "").trim());
		gotKill.clear();
	}

	public void cancel()
	{
		isRunning = false;
		isSuddenDeath = false;
		players.clear();
		gotKill.clear();
        m_botAction.cancelTasks();
	}

	void setupTimerTasks()
	{
		starter = new TimerTask()
		{
			public void run()
			{
				if(m_botAction.getPlayingPlayers().size() == 1){
					m_botAction.sendArenaMessage("To play wipeout you must have more than one player!");
					cancel();
					return;
				}
				m_botAction.scoreResetAll();
				gotKill.clear();
				m_botAction.sendArenaMessage("GO GO GO!!!",104);
				isRunning = true;

				Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
				while(it.hasNext())
				{
					Player p = it.next();
					players.add(p.getPlayerName());
				}
				initialPlayers = players.size();
				scheduleTasks();				
			}
		};

		
	}
	
	public void scheduleTasks(){
	    TimerTask specer = new TimerTask()
        {
            public void run()
            {                                
                spec();
                double plyrs = players.size();
                double speedReduction = Math.round((1 - (plyrs / initialPlayers)) * speed);
                speed -= speedReduction;
                if(speed > 5 && isRunning)
                    m_botAction.sendArenaMessage("WIPEOUT!!! You have " + speed + " seconds until the next wipeout!", 2);
                else if (speed <= 5 && !isSuddenDeath && isRunning){
                    m_botAction.sendArenaMessage("Sudden death! Be the first to get a kill!", 13);
                    isSuddenDeath = true;
                }
                if(!isSuddenDeath && isRunning)
                    scheduleTasks();
            }
        };
	    TimerTask tenSecWarn = new TimerTask()
        {
            public void run()
            {
                m_botAction.sendArenaMessage("10 seconds until wipeout!", 1);
                Iterator<String> ite = players.iterator();
                while(ite.hasNext()){
                    String name = ite.next();
                    if(!gotKill.contains(name))
                        m_botAction.sendSmartPrivateMessage(name, "You only have 10 seconds left to get a kill!");              
                }
            }
        };
        m_botAction.scheduleTask(specer, speed * 1000);
        if(speed > 10)
            m_botAction.scheduleTask(tenSecWarn, (speed * 1000) - 10000);
	}
}