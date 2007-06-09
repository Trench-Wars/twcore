/**
 * twbotwatchtk.java - Watch Teamkill module - Jacen Solo
 *
 * Created 7/15/2004 - Last modified 7/22/2004.
 * JavaDoc by Rodge_Rabbit
 */


package twcore.bots.twbot;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimerTask;

import twcore.bots.TWBotExtension;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.game.Player;

/**
 *  This module lets the bot monitor the teamkills, and
 *  take action of spec for a x amount of minutes
 *  if, depending on the variables, a certain amount of
 *  teamkills are made.
 */

public class twbotwatchtk extends TWBotExtension
{
	private Map<String, Integer> players;
	private Set<String> cantPlay;
	int banTime = 5;       //Ban time default in minutes
	int allowedTKs = 3;    //How many tk's are  allowed by default
    int decayTime = 5;     //The time it default takes to make decay one teamkill in minutes
	boolean tkwatching = false;   // If true, monitors and take action on teamkills

	public twbotwatchtk()
	{
		players = Collections.synchronizedMap(new HashMap<String, Integer>());
        cantPlay = Collections.synchronizedSet(new HashSet<String>());
	}


	/**
	 * Handles event received message, and if from an ER or above,
     * tries to parse it as a command.
     * @param event Passed event.
     */
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

    /**
     * Handles all commands given to the bot.
     * @param name Name of ER or above who sent the command.
     * @param message Message sent
     */
    public void handleCommand(String name, String message)
    {
    	if(message.toLowerCase().startsWith("!watchtk"))
    	{
    		String pieces[] = message.split(" ");
    		int aT = 3;   //Allowed Teamkills integer set to standard
    		int bT = 5;   //Ban time integer set to standard
    		int dT = 5;   //Decay time integer set to standard/
    		try{
    			aT = Integer.parseInt(pieces[1]);
    		} catch(Exception e) {}
    		try{
    			bT = Integer.parseInt(pieces[2]);
    		} catch(Exception e) {}
    		try{
    			dT = Integer.parseInt(pieces[3]);
    		} catch(Exception e) {}

    		aT = Math.max(1, Math.min(aT, 100));
    		bT = Math.max(1, Math.min(bT, 60));
    		dT = Math.max(1, Math.min(dT, 30));
    		allowedTKs = aT;
    		banTime = bT;
    		decayTime = dT;
    		m_botAction.sendArenaMessage(name + " has activated TK watcher. You will be spec'd for " + banTime + " minute(s) after " + allowedTKs + " TKs.");
    		m_botAction.sendArenaMessage("Each TK will decay after " + decayTime + " minute(s).");
    		tkwatching = true;   //enables watch for teamkills and undertakes action.
    	}
    	else if(message.toLowerCase().startsWith("!stoptkwatch"))
    	{
    		//Resets all variables to default
    		allowedTKs = 3;
    		banTime = 5;
    		decayTime = 5;
    		tkwatching = false;
    		m_botAction.cancelTasks();
    		players.clear();
    		cantPlay.clear();
    		m_botAction.sendArenaMessage(name + " has turned off TK watcher.");
    	}
    }

    /**
     * Handles all actions when a player enters the arena.
     * @param name Name of player who entered the arena.
     */
    public void handleEvent(PlayerEntered event)
    {
    	if(tkwatching)
    	{
	    	String name = event.getPlayerName();
    		m_botAction.sendPrivateMessage(name, "TK watcher is activated. You will be spec'd for " + banTime + " minute(s) after " + allowedTKs + " TKs. TKs decay after " + decayTime + " minute(s).");
    		m_botAction.spec(name);
    		m_botAction.spec(name);   //double spec to free the person for moving, and dont let him enter if he comes from another arena.
    	}
    }

    /**
     * Handles all actions when someone kills another person .
     * @param killer Person who did the killing.
     * @param killee Person who got killed.
     */
    public void handleEvent(PlayerDeath event)
	{
		if(tkwatching)
		{
			Player killer = m_botAction.getPlayer(event.getKillerID());
			Player killee = m_botAction.getPlayer(event.getKilleeID());

			if( killer == null || killee == null )
			    return;

			String killerName = killer.getPlayerName();

			if(killer.getFrequency() == killee.getFrequency())
			{
				if(players.containsKey(killerName))
				{
					int i = players.get(killerName).intValue() + 1;
					//m_botAction.scheduleTask(new decay(killerName), decayTime * 60 * 1000);
					if(i > allowedTKs)
					{
						m_botAction.spec(killerName);
						m_botAction.spec(killerName);   //double spec to free the person for moving
						m_botAction.sendPrivateMessage(killerName, "You have been specced for excessive teamkilling, you can reenter in " + banTime + " minute(s).");
						m_botAction.sendArenaMessage(killerName + " has been specced for " + banTime + " minute(s) for excessive teamkilling.", 2);
						cantPlay.add(killerName);   //adds killer to the spec list
						players.remove(killerName);
						m_botAction.scheduleTask(new EndItTask(killerName), banTime * 60 * 1000); //Starts the task that prevents the player to enter
					}
					else
					{
						players.put(killerName, new Integer(i));
					}
				}
				else
				{
					players.put(killerName, new Integer(1));
					long decayTimems = decayTime * 60 * 1000;
					m_botAction.scheduleTaskAtFixedRate(new DecayTask(killerName), decayTimems, decayTimems); //Starts task for decay time, converted for minutes
					m_botAction.sendPrivateMessage( killerName, "Teamkilling is illegal, if you teamkill " + allowedTKs + " times you will be banned for " + banTime + " minute(s) from playing." );
				}
			}
		}
	}

    /**
     * Handles all actions when a player tryd to enter the game.
     * @param getPlayerID Id of Person who tries to enter.
     * @param getPlayerName Name of person who tries to enter.
     */
    public void handleEvent(FrequencyShipChange event)
	{
		if(event.getShipType() == 0) return;
		if(cantPlay.contains(m_botAction.getPlayerName(event.getPlayerID())))
		{
			m_botAction.spec(event.getPlayerID());
			m_botAction.spec(event.getPlayerID());  //double spec to free the person for moving
			m_botAction.sendPrivateMessage( event.getPlayerID(), "You cannot enter, you were banned for " + banTime + " minute(s) for teamkilling." );
		}
	}

	public String[] getHelpMessages()
	{
    	String[] TKHelp = {
			"!watchtk <tklimit> <bantime> <decaytime> -starts watching tk's with specified stuff.",
			"!watchtk <tklimit> <bantime> -starts watching tk's with specified stuff with decay time of 5 minutes.",
			"!watchtk <tklimit> -starts watching tk's with specified limit, decay time of 5 minutes, and bantime of 5 minutes.",
			"!watchtk -starts watching tk's with 3 tk limit, decay time of 5 minutes, and bantime of 5 minutes.",
			"!stoptkwatch -stops watching tk's."
        };
        return TKHelp;
    }

    public void cancel()
    {
    }

    /**
     * Ends the time of a player who was specced for teamkill.
     * @param player Persons name whos spec is lifted.
     * @param name Persons name whos spec is lifted.
     */
	class EndItTask extends TimerTask
	{
		String player;

		public EndItTask(String name)
		{
			player = name;
		}

		public void run()
		{
			m_botAction.sendSmartPrivateMessage(player, "You may now play in ?go " + m_botAction.getArenaName());
			cantPlay.remove(player);
		}
	}

    /**
     * Removes one teamkill after the decay time.
     * @param player Persons name whos teamkill needs to be decayed.
     * @param name Persons name whos teamkill needs to be decayed.
     */
	class DecayTask extends TimerTask
	{
		String player;

		public DecayTask(String name)
		{
			player = name;
		}

		public void run()
		{
			if(players.containsKey(player))
			{
				int i = players.get(player).intValue();
				if(i > 0) {
					i--;
					players.put(player, new Integer(i));
					//m_botAction.sendSmartPrivateMessage(player, "1 TK decayed.");
				} else {
					players.remove(player);
					m_botAction.cancelTask(this);
				}
			} else {
				m_botAction.cancelTask(this);
			}
		}
	}
}