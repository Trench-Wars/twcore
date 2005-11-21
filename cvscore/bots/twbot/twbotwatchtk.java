/**
 * twbotwatchtk.java - Watch Teamkill module - Jacen Solo
 *
 * Created 7/15/2004 - Last modified 7/22/2004.
 * JavaDoc by Rodge_Rabbit
 */


package twcore.bots.twbot;

import java.util.HashMap;
import java.util.TimerTask;

import twcore.bots.shared.TWBotExtension;
import twcore.core.Player;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;

/**
 *  This module lets the bot monitor the teamkills, and
 *  take action of spec for a x amount of minutes
 *  if, depending on the variables, a certain amount of
 *  teamkills are made.
 */

public class twbotwatchtk extends TWBotExtension
{
	private HashMap players;
	private HashMap cantPlay;
	int banTime = 5;       //Ban time default in minutes
	int allowedTKs = 3;    //How many tk's are  allowed by default
        int decayTime = 5;     //The time it default takes to make decay one teamkill
	boolean tkwatching = false;   // If true, monitors and take action on teamkills
	
	public twbotwatchtk()
	{
		players = new HashMap();
        cantPlay = new HashMap();
	}
	
	
	/** Handles event received message, and if from an ER or above, 
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

    /** Handles all commands given to the bot.
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
    		decayTime = dT;
    		banTime = bT;
    		allowedTKs = aT;
    		m_botAction.sendArenaMessage(name + " has activated TK watcher. You will be spec'd for " + banTime + " minute(s) after " + allowedTKs + " TKs.");
    		m_botAction.sendArenaMessage("Each TK will decay after " + decayTime + " minute(s).");
    		tkwatching = true;   //enables watch for teamkills and undertakes action.
    	}
    	if(message.toLowerCase().startsWith("!stoptkwatch"))
    	{
    		//Resets all variables to default
    		allowedTKs = 3;
    		banTime = 5;
    		decayTime = 5;
    		tkwatching = false;
    		m_botAction.sendArenaMessage(name + " has turned off TK watcher.");
    	}
    }
    
    /** Handles all actions when a player enters the arena.
     * @param name Name of player who entered the arena.
     */
    public void handleEvent(PlayerEntered event)
    {
    	String name = event.getPlayerName();
    	if(tkwatching)
    	{
    		m_botAction.sendPrivateMessage(name, "TK watcher is activated. You will be spec'd for " + banTime + " minute(s) after " + allowedTKs + " TKs. TKs decay after " + decayTime + " minute(s).");
    		m_botAction.spec(name);
    		m_botAction.spec(name);   //double spec to free the person for moving, and dont let him enter if he comes from another arena.
    	}
    }
    
    /** Handles all actions when someone kills another person .
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
			
			if(killer.getFrequency() == killee.getFrequency())
			{			
				if(players.containsKey(killer))
				{
					int i = ((Integer)players.get(killer)).intValue();
					i++;
					m_botAction.scheduleTask(new decay(killer.getPlayerName()), decayTime * 60 * 1000);
					if(i > allowedTKs)
					{
						m_botAction.spec(killer.getPlayerName());
						m_botAction.spec(killer.getPlayerName());   //double spec to free the person for moving
						m_botAction.sendPrivateMessage(killer.getPlayerName(), "You have been specced for excessive teamkilling, you can reenter in " + banTime + " minute(s).");
						m_botAction.sendArenaMessage(killer.getPlayerName() + " has been specced for " + banTime + " minute(s) for excessive teamkilling.", 2);
						cantPlay.put(killer.getPlayerName(), killer);   //adds killer to the spec list
						players.remove(killer);
						m_botAction.scheduleTask(new EndIt(killer.getPlayerName()), banTime * 60 * 1000); //Starts the task that prevents the player to enter
					}
					else
						players.put(killer, new Integer(i));
				}
				else
				{
					players.put(killer, new Integer(1));
					m_botAction.scheduleTask(new decay(killer.getPlayerName()), decayTime * 60 * 1000); //Starts task for decay time, converted for minutes
					m_botAction.sendPrivateMessage( killer.getPlayerName(), "Teamkilling is illegal, if you teamkill " + allowedTKs + " times you will be banned for " + banTime + " minute(s) from playing." );
				}
			}
		}
	}
    
    /** Handles all actions when a player tryd to enter the game.
     * @param getPlayerID Id of Person who tries to enter.
     * @param getPlayerName Name of person who tries to enter.
     */
    public void handleEvent(FrequencyShipChange event)
	{
		if(event.getShipType() == 0) return;
		if(cantPlay.containsKey(m_botAction.getPlayerName(event.getPlayerID())))
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
			"!watchtk <tklimit> <bantime> -starts watching tk's with specified stuff with decay time of 5 minute(s).",
			"!watchtk <tklimit> -starts watching tk's with specified limit, decay time of 5 minute(s), and bantime of 5 minute(s).",
			"!watchtk -starts watching tk's with 3 tk limit, decay time of 5 minute(s), and bantime of 5 minute(s).",
			"!stoptkwatch -stops watching tk's."
        };
        return TKHelp;
    }

    /** Ends the time of a player who was specced for teamkill.
     * @param player Persons name whos spec is lifted.
     * @param name Persons name whos spec is lifted.
     */
    public void cancel()
    {
    }
    
	class EndIt extends TimerTask
	{	
		String player;
	
		public EndIt(String name)
		{
			player = name;
		}
		
		public void run()
		{
			m_botAction.sendSmartPrivateMessage(player, "You may now play.");
			cantPlay.remove(player);
		}
	}

     /** Removes one teamkill after the decay time     .
     * @param player Persons name whos teamkill needs to be decayed.
     * @param name Persons name whos teamkill needs to be decayed.
     */


	class decay extends TimerTask
	{
		String player;
		
		public decay(String name)
		{
			player = name;
		}
		
		public void run()
		{
			if(players.containsKey(player))
			{
				m_botAction.sendSmartPrivateMessage(player, "1 TK decayed.");
				int i = ((Integer)players.get(player)).intValue();
				if(i > 0)
					i--;
				players.put(player, new Integer(i));
			}
		}
	}
}
