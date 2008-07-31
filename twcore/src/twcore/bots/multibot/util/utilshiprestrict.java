package twcore.bots.multibot.util;

import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Iterator;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/**
 * 
 * A utility that sets ship restrictions for a given freq,
 * I'm surprised this functionally wasn't present before hand.
 * 
 * Ship restrictions can be applied for any number of freqs, it's
 * built to run in an unlocked arena and can be used in combination
 * with other utils easily such as safes, shipc, hotspots, ect...
 * 
 * @author Ayano / Ice-demon
 *
 */

public class utilshiprestrict extends MultiUtil {
	
	HashMap<Integer, FreqRestriction> restrictions;
	ArrayList<FreqRestriction> list;
	ArrayList<Integer> universalRestrict;
	ArrayList <String> Exmpt;
	boolean universal;
	Random rand = new Random();
	
	/**
	 * Initializes variables.
	 */
	
	public void init()	{
		restrictions = new HashMap<Integer, FreqRestriction>();
		list = new ArrayList<FreqRestriction>();
		universalRestrict = new ArrayList<Integer>();
		Exmpt = new ArrayList<String>();
		universal = false;
		}
	
	/**
	 * Requests events.
	 */
	
	public void requestEvents(ModuleEventRequester events)	{
		events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
		events.request(this, EventRequester.FREQUENCY_CHANGE );
		events.request(this, EventRequester.PLAYER_ENTERED);
	}
	
	/**
	 * Handles messages sent to bot.
	 */
	
	public void handleEvent(Message event)	{
		String playerName = m_botAction.getPlayerName(event.getPlayerID());
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)	{
			if(m_opList.isER(playerName))
		        handleCommand(playerName, (event.getMessage()).toLowerCase());
		}
	  }
	
	/**
	   * Gets the argument tokens from a string.  If there are no colons in the
	   * string then the delimeter will default to space.
	   *
	   * @param string is the string to tokenize.
	   * @return a tokenizer separating the arguments is returned.
	   */
	
	public StringTokenizer getArgTokens(String string,String token)	{
	    if(string.indexOf(token) != -1)
	      return new StringTokenizer(string, token);
	    return new StringTokenizer(string);
	  }
	
	/**
	 * Fixes list based conflicts removing any duplicates.
	 * 
	 * @param freq is the freq to be checked for duplicates.
	 */
	
	public void FixListConflicts(Integer freq)	{
		for (int i=0 ; i<list.size(); i++)	{
			FreqRestriction current = (FreqRestriction)list.get(i);
			if(((Integer)current.getFreq()).equals(freq))
				{list.remove(i);return;}
		}	
	}
	
	/**
	 * Sets restrictions for a given freq and stores them into 
	 * FreqRestricition object which is in turn stored into a vector of
	 * said objects. If new restrictions arrive for an existing restricted
	 * freq, that freq's restrictions are replaced with the new ones.
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is a string containing the ship numbers and freq.
	 */
	
	public void doRestricts(String sender, String argString)	{
		if (universal)
			{m_botAction.sendSmartPrivateMessage(sender, "Universal settings in effect, clear then then try again");return;}
		try	{
			StringTokenizer argTokens = getArgTokens(argString,":");
			StringTokenizer shipTokens = getArgTokens(argTokens.nextToken(),",");
			int numArgs = argTokens.countTokens(),numShips = shipTokens.countTokens();
			Integer freq = new Integer(Integer.parseInt(argTokens.nextToken()));
			
	    	if (numArgs != 1 || numShips < 1)
		    	throw new IllegalArgumentException("Please use the following syntax: !SetRestricts <ship1>,<ship2>,ect..:<freq>");
	    	ArrayList<Integer> rules = new ArrayList<Integer>();
	    	for( int i=0 ; i < numShips ; i++ )	{
	    		Integer shipnum = new Integer (Integer.parseInt(shipTokens.nextToken()));
				ValidCheck(shipnum,rules);
	    		rules.add(shipnum);
	    	}
	    	if (freq < 0) throw new IllegalArgumentException("Freqs can't be negative!");
	    	FreqRestriction restriction = new FreqRestriction(rules,freq);
	    	FixListConflicts(freq);
	    	restrictions.put(freq, restriction);
	    	list.add(restriction);
	    	UpdateArena();
	    	m_botAction.sendSmartPrivateMessage(sender, "Freq " + freq + " has the following ship restrictions: " + rules.toString());
	    }
		catch(NumberFormatException e)	{
		      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !SetRestricts <ship1>,<ship2>,ect..:<freq>");
		    }
		catch(Exception e)	{
			  if (e.getMessage() == null)
				  m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !SetRestricts <ship1>,<ship2>,ect..:<freq>");
			  else
				  m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
		    }	
	}
	
	/**
	 * Sets universal restrictions.
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is the string containing the ship numbers.
	 */
	
	public void doRestrictAll(String sender, String argString)	{
		StringTokenizer shipTokens = getArgTokens(argString,",");
		int numShips = shipTokens.countTokens();
		try	{
			if(!restrictions.isEmpty())
				throw new IllegalArgumentException("Freq specific restrictions are present, clear them then try again");
			ArrayList<Integer> temp = new ArrayList<Integer>();
			for( int i=0 ; i < numShips ; i++ )	{
				Integer shipnum = new Integer (Integer.parseInt(shipTokens.nextToken()));
				ValidCheck(shipnum,temp);
				temp.add(shipnum);
			}
			universal=true;
			universalRestrict = temp;
			UpdateArena();
	    	m_botAction.sendSmartPrivateMessage(sender, "Universal ship restrictions are : " + universalRestrict.toString());
		}
		catch(NumberFormatException e)	{
		      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !SetRestrictsUni <ship1>,<ship2>,ect..");
		    }
		catch(Exception e)	{
		      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
		}
	}
	
	/**
	 * Generates a list of players who are exempt from ship 
	 * restrictions.
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is the String containing the players to be exempt.
	 */
	
	public void doSetExcepts(String sender, String argString)	{
		StringTokenizer argTokens = getArgTokens(argString,",");
	    int numArgs = argTokens.countTokens();
	    try	{
	    	StringBuilder strmsg = new StringBuilder("The following players have been added to the exempt list : ");
	    	for (int i=0;i<numArgs;i++)	{
	    		String name = argTokens.nextToken();
	    		if (IsValidPlayer(name))	{
	    			if (!IsExempt(name))
	    				Exmpt.add(name);
	    			strmsg.append(name + ", ");
	    		}
	    	}
	    	m_botAction.sendSmartPrivateMessage(sender, strmsg.toString());
	    }	catch(Exception e)	{
	        m_botAction.sendSmartPrivateMessage(sender, "Please use vaild player names in the following syntax: !setExcepts <name1>,<name2>..ect");
	      }
	    }
	
	/**
	 * Removes a player from the exempt list.
	 * @param sender is the user of the bot.
	 * @param playerName is the player to be removed.
	 */
	
	public void doRemoveExcept(String sender, String playerName)	{
		if (IsValidPlayer(playerName))
			if(IsExempt(playerName))	{
				Exmpt.remove(playerName);
				m_botAction.sendSmartPrivateMessage(sender, playerName + " was removed from the exempt list");
				UpdateArena();
				return;
				}
		m_botAction.sendSmartPrivateMessage(sender, playerName + " is not present in the arena or is not Exempt!");
	}

	/**
	 * Spams the list of restrictions to the sender.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doList(String sender)	{
		
		if (universal || !restrictions.isEmpty())	{
			if(universal)
				m_botAction.sendPrivateMessage(sender, "Universal ship restrictions: " + universalRestrict.toString());
			else	{
				String str;
				for (int i=0 ; i<list.size(); i++)	{
					FreqRestriction current = (FreqRestriction)list.get(i);
					str = "Freq " + current.getFreq() + " has the current ship restrictions: " + (current.getRules()).toString();
					m_botAction.sendPrivateMessage(sender, str);
				}
			}
		}
		else
			m_botAction.sendPrivateMessage(sender, "No restrictions have been set.");
	}
	
	/**
	 * Lists exempt players.
	 * @param sender is the user of the bot.
	 */
	
	public void doListExempt(String sender)	{
		if (Exmpt.isEmpty())
			{m_botAction.sendPrivateMessage(sender, "Nobody is Exempt!");return;}
		m_botAction.sendPrivateMessage(sender,"The following players are exempt from restrictions " + Exmpt.toString());
	}
	
	/**
	 * Clears a specific freq's restrictions
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is a string containing the freq.
	 */
	
	public void doClear(String sender, String argString)	{
		if (universal)
		{m_botAction.sendPrivateMessage(sender, "Universal restrictions in effect, there are no freq-specific restrictions.");return;}
		else if (restrictions.isEmpty())
		{m_botAction.sendPrivateMessage(sender, "No restrictions have been set");return;}
		
		try {
			Integer freq = new Integer (Integer.parseInt(argString));
			if (!restrictions.containsKey(freq))
				{m_botAction.sendSmartPrivateMessage(sender, "Requested freq is not restricted");return;}
			FreqRestriction temp = restrictions.get(freq);
			restrictions.remove(freq);
			list.remove(temp);
			m_botAction.sendPrivateMessage(sender, "Freq " + freq + "'s restrictions have been lifted"); 
		}
		catch(Exception e)	{
		      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !ClearRestricts <freq>");
		}
		
	}
	
	/**
	 * Clears all restrictions.
	 * 
	 * @param sender is the user of the bot
	 */
	
	public void doClearAll(String sender)	{		
		if (!universal && restrictions.isEmpty())
			m_botAction.sendPrivateMessage(sender, "No restrictions have been set.");
		else	{
			restrictions.clear();list.clear();universalRestrict.clear();universal=false;
			m_botAction.sendPrivateMessage(sender, "All restrictions cleared");
		}
	}
	
	/**
	 * Clears the list of exempt players.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doClearExcepts(String sender)	{
		if (Exmpt.isEmpty())
			{m_botAction.sendPrivateMessage(sender, "Nobody is Exempt!");return;}
		Exmpt.clear();
		UpdateArena();
		m_botAction.sendPrivateMessage(sender, "All exempt players normalized");
	}
	
	/**
	 * Handles ship change events in accordance with set 
	 * freq restrictions.
	 */
	
	public void handleEvent( FrequencyShipChange event )	{
		Player player = m_botAction.getPlayer(event.getPlayerID());
		CheckPlayer(player);
	}
	
	/**
	 * Handles freq change events in accordance with set freq 
	 * ship restrictions.
	 */
	public void handleEvent( PlayerEntered event ) {
		Player player = m_botAction.getPlayer(event.getPlayerID());
		CheckPlayer(player);
	}
	
	/**
	 * Handles freq change events in accordance with set freq 
	 * ship restrictions.
	 */
	
	public void handleEvent( FrequencyChange event )	{
		Player player = m_botAction.getPlayer(event.getPlayerID());
		CheckPlayer(player);
	}
	
	/**
	 * Helper method that checks if the given ship is valid and
	 * isn't duplicate
	 * 
	 * @param shipnum is the ship number
	 */
	
	private void ValidCheck(Integer shipnum,ArrayList<Integer> errand)	{
		if (shipnum > 0 && shipnum < 9)
			if (!errand.contains(shipnum))
				return;
		throw new IllegalArgumentException ("Invalid ship number or duplicate listed!");
	}
	
	/**
	 * Helper method to check if the player is in the arena.
	 * -might be some discrepancies with similar named players.
	 * 
	 * @param name is the player.
	 * @return true if the player is present.
	 */
	
	private boolean IsValidPlayer(String name)	{
		if (m_botAction.getFuzzyPlayerName(name).equalsIgnoreCase(name))
			return true;
		return false;
	}
	
	/**
	 * Helper method to check if players are on the exempt list.
	 * 
	 * @param name is the name of the player.
	 * @return true if on the list.
	 */
	
	private boolean IsExempt(String name)	{
		if(Exmpt.size() == 0) return false;
		if (Exmpt.contains(name.toLowerCase()))
			return true;
		return false;
	}
	
	/**
	 * Updates the arena with the new restrictions
	 */
	
	private void UpdateArena()	{
		Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
		while (it.hasNext())	{
			CheckPlayer((Player)it.next());
		}
	}
	
	/**
	 * Checks the given player for ship restrictions.
	 * 
	 * @param player is the player being checked
	 */
	
	private void CheckPlayer(Player player)	{
		if (player == null) return;
		String playerName = player.getPlayerName();
		if (IsExempt(playerName)) return;
		Integer freq = new Integer (player.getFrequency());
		Integer shipnum = new Integer (player.getShipType());
		
		
		if (universalRestrict.contains(shipnum))
			SwitchShip(playerName,universalRestrict);		
		else if(restrictions.containsKey(freq))	{
			ArrayList<Integer> badships = ((FreqRestriction)restrictions.get(freq)).getRules();
			if (badships.contains(shipnum))
				SwitchShip(playerName,badships);
		}
		else
			return; //useless? yes but it makes the bot somehow 'remember'.
	}
	
	/**
	 * Helper method that ship changes a player who has entered
	 * an invalid ship.
	 *  
	 * @param playerName is the Player's name.
	 * @param invaildships is a vector containing invalid ships.
	 */
	
	private void SwitchShip(String playerName, ArrayList<Integer> invaildships)	{
		ArrayList<Integer> goodships = new ArrayList<Integer>();
		for( Integer i = new Integer(1) ; i<9 ; i++ )
			if (!invaildships.contains(i))	
			{goodships.add(i);}
		if (!goodships.isEmpty())	{
			m_botAction.setShip(playerName, goodships.get(rand.nextInt(goodships.size())));
			m_botAction.sendPrivateMessage(playerName, "You may only use ships: " + goodships.toString() + " on this freq");
			return;
		}
		m_botAction.sendPrivateMessage(playerName, "All ships are restricted for this freq apparently!");
		m_botAction.specWithoutLock(playerName);
	}
	
	/**
	 * Handles commands sent by ER+.
	 * 
	 * @param sender is the user of the bot.
	 * @param command is the issued command.
	 */
	
	public void handleCommand(String sender,String command)	{
    	if(command.startsWith("!setrestricts "))
        	doRestricts(sender, command.substring(14).trim());
    	if(command.startsWith("!setrestrictsuni "))
            doRestrictAll(sender, command.substring(17).trim());
    	if(command.startsWith("!setexcepts "))
            doSetExcepts(sender, command.substring(12).trim());
    	if(command.startsWith("!removeexcept "))
            doRemoveExcept(sender, command.substring(14).trim());
    	if(command.startsWith("!listrestricts"))
            doList(sender);
        if(command.startsWith("!listexcepts"))
            doListExempt(sender);
        if(command.startsWith("!clearrestricts "))
            doClear(sender, command.substring(16).trim());
        if(command.startsWith("!clearallrestricts"))
            doClearAll(sender);
        if(command.startsWith("!clearexcepts"))
            doClearExcepts(sender);
	}
	
	/**
	 * An Object that holds a freq's restrictions.
	 */
	
	private class FreqRestriction	{
		ArrayList<Integer> myRules;
		int myFreq;
		
		public FreqRestriction(ArrayList<Integer> rules,int freq)	{
			myRules = rules;
			myFreq = freq;
		}
		
		public ArrayList<Integer> getRules()	
		{return myRules;}
		public int getFreq()	
		{return myFreq;}
	}
	
	/**
	 * Returns help messages.
	 */
	
	public String[] getHelpMessages()	{
	      String[] message = {
	    	  "=SHIP-RESTRICT=====================================================================SHIP-RESTRICT=",
	          "!SetRestricts [<ship1>,<ship2>,ect..]:<freq>    -- List of restricted ships. for <freq>",
	          "                                                -- Example format: :bot:!setrestricts 1,2,3:0",
	          "!SetRestrictsUni <ship1>,<ship2>,ect..          -- List of restrictions for all freqs",
	          "                                                -- Example format: :bot:!setrestrictsuni 1,2,3",
	          "!SetExcepts <name>,<name>,ect...                -- Specifies specific players to be exempt from", 
	          "                                                -- Restrictions",
	          "!RemoveExcept <name>                            -- Removes player from Exempt status",
	          "!ListRestricts                                  -- Lists all restrictions fors freqs or universal",
	          "!ListExcepts                                    -- Lists all Exempt players",
	          "!ClearRestricts <freq>                          -- Clears restrictions for <freq>",
	          "!ClearAllRestricts                              -- Clears all restrictions for <freq>",
	          "!ClearExcepts                                   -- Deprives all Exempt players their priviligaes",
	          "================================================================================================="
	      };
	      return message;
	  }

}
