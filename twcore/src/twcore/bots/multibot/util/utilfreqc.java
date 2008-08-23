package twcore.bots.multibot.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.stats.PlayerProfile;
import twcore.core.util.ModuleEventRequester;

/**
 * Adds a pseudo-lock to the arena allowing those who entered
 * before the 'lock' to change ships but not freqs freely while
 * those in spectator before, or after 'lock', and entering players
 * are kept in spec. Spec x 4 ensures compatibility with most utils.
 * 
 * Note: This is NOT compatible with lagout or shipc.
 * 
 * @author Ayano
 *
 */

public class utilfreqc extends MultiUtil {
	
	private boolean active  = false,
					lates   = false,
					lagouts= false;
					
	private int numfreqs = 2;
	
	private HashSet<Integer> specSet;
	private HashMap<String, PlayerProfile> playerMap;
	private TimerTask notifySpec;
	
	
	/**
	 * Initializes global variables.
	 */
	
	public void init()	{
		specSet = new HashSet<Integer>();
		playerMap = new HashMap<String, PlayerProfile>();
	}
	
	/**
	 * Requests all events used by this module.
	 */
	
	public void requestEvents(ModuleEventRequester events)	{
		events.request(this, EventRequester.PLAYER_ENTERED);
		events.request(this, EventRequester.PLAYER_LEFT );
		events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
		events.request(this, EventRequester.FREQUENCY_CHANGE );
	}
	
	/**
	 * Sets the number of freqs in the pseudo-lock'd game.
	 * Syntax <#>
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is the argument to be computed.
	 */
	
	public void doSetFreqs(String sender, String argString)	{
		int freqs;
		try {
			freqs = Integer.parseInt(argString);
			if (freqs <= 0)
				throw new IllegalArgumentException("Freqs cannot be negative.");
			numfreqs = freqs;
			m_botAction.sendPrivateMessage(sender, "Settings will start " +
					"with "+ numfreqs + " freqs.");
		}	catch (IllegalArgumentException iae )	{
			iae.getMessage();
			}	catch (Exception e)	{
				m_botAction.sendPrivateMessage(sender, "Numbers only.");
		}
		
	}
	
	/**
	 * Turns lates on/off.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doLates(String sender)	{
		if(!active)	{
			m_botAction.sendPrivateMessage(sender, "Settings must be active prior " +
					"to setting lates on/off.");
        	return;
		}
		lates = !lates;
		
		if (lates)	{
			m_botAction.sendArenaMessage("Lates have been enabled, pm " +
					m_botAction.getBotName() + " to get in.  - " + sender, 2);
			notifySpec = new TimerTask()	{
				public void run()	{
					m_botAction.sendTeamMessage("Late? want in? " +
							"Pm me !lemmein to play!", 25);
				}};
			m_botAction.scheduleTask(notifySpec, 10000, 60000);
		}
		else	{
			m_botAction.sendArenaMessage("Lates have been disabled. - " + 
					sender,10);
			notifySpec.cancel();
		}
		
	}
	
	/**
	 * Turns on/off lagouts.
	 * Used for games that DO NOT rely on deaths.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doLagouts(String sender)	{
		lagouts = !lagouts;
		
		if(lagouts)
			m_botAction.sendArenaMessage("Lagouts have been enabled." +
					" - " + sender,2);
		else
			m_botAction.sendArenaMessage("Lagouts have been disabled." +
					" - " + sender,13);
	}
	
	/**
	 *Starts the pseudo-lock.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doStart(String sender)	{
		if (!active)	{
			Iterator<Player> iter = m_botAction.getPlayerIterator();
            if( iter == null ) return;
            m_botAction.toggleLocked();
            m_botAction.createNumberOfTeams(numfreqs);
            while (iter.hasNext())	{
            	Player player = iter.next();
            	putPlayer(player);
            } m_botAction.toggleLocked();
            m_botAction.sendPrivateMessage(sender, "Settings activated.");
            m_botAction.sendArenaMessage("Arena Locked, " +
            		"those playing can freely switch ships.   - " + sender,1);
            active = true;
		} else
			m_botAction.sendPrivateMessage(sender, "Settings already active.");
		
	}
	
	/**
	 * Stops the pseudo-lock, resets all tasks.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doStop(String sender)	{
		if (active)	{
			if (lates)
				notifySpec.cancel();
			
			active = false;lates = false;
			specSet.clear();
			playerMap.clear();
			numfreqs = 2;
			m_botAction.sendArenaMessage("Arena unlocked   - " + sender,1);
		} else
			m_botAction.sendPrivateMessage(sender, "No settings to stop.");
	}
	
	/**
	 * Adds a late player if they haven't left the game or were removed.
	 * 
	 * @param playerName is the name of the player.
	 */
	
	public void doCheckPlayer(String playerName)	{
		Integer playerID = new Integer(m_botAction.getPlayerID(playerName));
		int freq = getLowest();
		
		if (lates && !playerMap.containsKey(playerName))	{
			PlayerProfile profile = new PlayerProfile(playerName,1,freq);
			playerMap.put(playerName, profile);
		} else if (lagouts && playerMap.containsKey(playerName))
			freq = playerMap.get(playerName).getFreq();
		
		specSet.remove(playerID);
		m_botAction.setShip(playerName, 1);
		m_botAction.setFreq(playerID, freq);
	}
	
	
	/**
	 * Helper method, adds a player to the spec-tasks and
	 * specs them.
	 * 
	 * @param playerID is the ID of the player.
	 */
	
	private void addSpec(Integer playerID)	{
		specSet.add(playerID);
		m_botAction.specWithoutLock(playerID);
	}
	
	/**
	 * Helper method, places 'playing' players in the game and
	 * keeps spectators out as if the arena were *locked.
	 * @param player is a PlayerProfile object.
	 */
	
	private void putPlayer(Player player)	{
		String name = player.getPlayerName();
        if (player.isPlaying())	{
        	playerMap.put( name, new PlayerProfile
        			(name, 1 , player.getFrequency()) );
        	m_botAction.setShip(name, 1);
        }
        else
        	specSet.add( new Integer(player.getPlayerID()) );
	}
	
	/**
	 * Helper method, gets the freq with the lowest players.
	 * 
	 * @return the freq with the lowest players.
	 */
	
	
	private int getLowest()	{
		int least = 900,count = 0,lowfreq = 0;

		try	{
			for (int i=numfreqs ; i>0 ; i--)	{
				count = m_botAction.getFrequencySize(i-1);
				if(count<least){least=count;lowfreq=i-1;}
			}	return lowfreq;
		}	catch(Exception e)	{return 0;}
	}
	
	/**
	 * handles player entries, adds spec-tasks.
	 */
	
	public void handleEvent( PlayerEntered event ) {
        if(!active)
        	return;
        
        Integer playerID = new Integer(event.getPlayerID());
        //String playerName = event.getPlayerName();
        addSpec(playerID);
        
        /*if(playerMap.containsKey(playerName))
        	playerMap.get(playerName).setData(1, 1); //Player left before, remember this
        */
        	
    }
	
	/**
	 * Handles player exit events, removes spec-tasks.
	 */
	
	public void handleEvent( PlayerLeft event )	{
		if(!active)
        	return;
		
		
		int playerID = event.getPlayerID();
		
		if (specSet.contains(playerID))
			specSet.remove(playerID);
		
	}
	
	/**
	 * Handles all freq switching, enforces freq-lock.
	 */
	
	public void handleEvent( FrequencyChange event )	{
		if(!active)
        	return;
		
		Integer playerID = new Integer(event.getPlayerID());
		String playerName = m_botAction.getPlayerName(playerID);
		if (!playerMap.containsKey(playerName))
			addSpec(playerID);
		else
			m_botAction.setFreq(playerID, playerMap.get(playerName).getFreq());
		
	}
	
	/**
	 * Handles all ship change events, enforces the pseudo-lock.
	 */
	
	public void handleEvent( FrequencyShipChange event )	{
		if(!active)
        	return;
		
		//Ensures compliance with others utils, *spec x 4
		Integer playerID = new Integer (event.getPlayerID());
		String playerName = m_botAction.getPlayerName(playerID);
		if ( specSet.contains(playerID) )
			m_botAction.specWithoutLock(playerID.intValue());
		else if (!playerMap.containsKey(playerName))	{ //FIXME may be of issue.
			addSpec(playerID);
		} else if (event.getShipType() == 0)	{
			specSet.add(playerID);
			//playerMap.get(playerName).setData(1, 1); //Player was speced, remember this.
		}
	}
	
	/**
	 * Handles all message events.
	 */
	
	public void handleEvent(Message event)	{
		String playerName = m_botAction.getPlayerName(event.getPlayerID());
		String message = event.getMessage().toLowerCase();
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)	{
			if(m_opList.isER(playerName))
		        handleCommand(playerName, message);
			else
				handlePlayerCommand(playerName, message);
		}
	  }
	
	/**
	 * Handles ER commands.
	 * 
	 * @param sender is the user of the bot.
	 * @param message is the command.
	 */
	
	public void handleCommand(String sender, String message)	{
		if (message.startsWith("!setfreqs "))
			doSetFreqs( sender, message.substring(10) );
		else if (message.startsWith("!lates"))
			doLates( sender );
		else if (message.startsWith("!lagouts"))
			doLagouts( sender );
		else if (message.startsWith("!start"))
			doStart(sender);
		else if (message.startsWith("!stop"))
			doStop(sender);
		else
			handlePlayerCommand(sender, message);
	}
	
	/**
	 * Handles player commands.
	 * 
	 * @param sender is the command sender.
	 * @param message is the command.
	 */
	
	public void handlePlayerCommand(String sender, String message)	{
		if (active && message.startsWith("!lemmein"))
			if (lates || lagouts)
				doCheckPlayer(sender);
	}
	
	public String[] getHelpMessages()	{
	      String[] message = {
	    	  "=FREQ-C======================================================FREQ-C=",
	    	  "++This modules enforces a 'pseudo-lock' allowing players to change++",
	    	  "++ships while keeping players in spec. The game can be hosted as  ++",
	    	  "++         if it were locked, with all utils except lagout        ++",
	    	  "!SetFreqs <#>            -- Sets the number of freqs. Default is 2",
	    	  "!lates                   -- Toggles lates on/off. [Default is off]",
	    	  "!lagouts                 -- Toggles lagouts on/off. [Default is off]",
	    	  "                         ^^ Do not use if deaths are important.",
	    	  "!start                   -- Activates the pseudo lock.",
	    	  "!stop                    -- Deactivates the pseudo lock.",
	          "=FREQ-C======================================================FREQ-C=",
	      };
	      return message;
	  }

	public void cancel()	{
		if (lates)
			notifySpec.cancel();
	}
}
