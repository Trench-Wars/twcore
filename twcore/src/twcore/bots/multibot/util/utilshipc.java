package twcore.bots.multibot.util;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.stats.PlayerProfile;
import twcore.core.util.ModuleEventRequester;

import java.util.Iterator;
import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;

import twcore.core.game.Player;
import java.util.StringTokenizer;

/**
 * 
 * This is the 'simple' ship changing module I tasked myself to do.
 * As you can see, it's not so simple as I choose to do ship 'groups'
 * where after x amount of deaths, you were down-graded to another class
 * of ships. Players start off with ships of competitive fire=power
 * but after x amount of deaths they are shunted over to their
 * reserve ships which after y deaths (or not if set to infinite) would
 * be spec'd. These are ship groups meaning the players can change ships
 * of their current class if they feel a change in play style while still
 * playing in a pseudo-locked game.
 * 
 * This is a Util, and there for is highly customizable to add spice
 * to the library of TW events; provided the hosts have the intellect
 * and creativity to use it.
 * 
 * 
 * @author Ayano / Ice-demon
 *
 */
public class utilshipc extends MultiUtil
{


	ArrayList Aclass;
	ArrayList Bclass;
	ArrayList Exmpt;
	int mainlives=2;
	int reservelives=2;
	int latelives=1;
	int numfreqs=2;
	String deathmsg = "is out with";
	boolean validstart = false;
	boolean Late = true;
	Random rand = new Random();
	private HashMap<String, PlayerProfile> playerMap;

	/**
	 * initialize variables
	 */
	
	public void init()	{
	Integer x = new Integer(1);
	Integer y = new Integer(3);
	ArrayList prim = new ArrayList();
	ArrayList reser = new ArrayList();
	ArrayList Exmpts = new ArrayList();
	Aclass = prim;
	Bclass = reser;
	Exmpt = Exmpts;
	Aclass.add(x);
	Bclass.add(y);
	playerMap = new HashMap<String, PlayerProfile>();
	}
	
	/**
	 * Requests events
	 */
	
	public void requestEvents(ModuleEventRequester events)	{
		events.request(this, EventRequester.PLAYER_DEATH);
		events.request(this, EventRequester.PLAYER_ENTERED);
		events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
		events.request(this, EventRequester.FREQUENCY_CHANGE );
	}
	
	/**
	   * Gets the argument tokens from a string.  If there are no colons in the
	   * string then the delimeter will default to space.
	   *
	   * @param string is the string to tokenize.
	   * @return a tokenizer separating the arguments is returned.
	   */
	
	public StringTokenizer getArgTokens(String string)	{
	    if(string.indexOf((int) ':') != -1)
	      return new StringTokenizer(string, ":");
	    return new StringTokenizer(string);
	  }
	
	/**
	 * Handle private messages sent to the bot. 
	 */
	
	public void handleEvent(Message event)	{
		String playerName = m_botAction.getPlayerName(event.getPlayerID());
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)	{
			if(m_opList.isER(playerName))
		        handleCommand(playerName, event.getMessage().toLowerCase());
			else
				handlePlayerCommand(playerName, event.getMessage());
		}
	  }

	/**
	 * Sets up both primary and reserve ships pending on the status of
	 * the sent boolean 'main'. When creating either set of 
	 * ship list, the opposing Array is cleared of conflicts; 
	 * overwriting it in short terms. If the game is in progress,
	 * the values cannot be changed unless !stop is issued.
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is a string containing the ship numbers seperated
	 * by colons ':'
	 * @param main is the accompanying boolean the decides wither the 
	 * method is compiling the primary or reserve ships.
	 */
	
	public void doShps(String sender, String argString, boolean main)	{
		if(validstart)	{ 
			m_botAction.sendSmartPrivateMessage(sender,"Game in progress, !Stop then change definitions");
			return;
		}
		
		StringTokenizer argTokens = getArgTokens(argString);
	    int numArgs = argTokens.countTokens();
	    String mainstr = ("primary");
	    if(!main)
	    	{mainstr = ("reserve");Bclass.clear();}
	    else
	    	Aclass.clear();
	    
	    try	{
	    	StringBuilder strmsg = new StringBuilder("The following ships are considered "  + mainstr + ": ");
	    	for (int i=1;i<=numArgs;i++)	{
	    	  Integer shipnum = Integer.parseInt(argTokens.nextToken());
	    	  if(main)	{
	    		  IsValidShip(shipnum,Aclass);
	    		  Aclass.add(shipnum);
	    		  Bclass = FixConflicts(shipnum,Bclass);
	    	  }
	    	  else	{
	    		  IsValidShip(shipnum,Bclass);
	    		  Bclass.add(shipnum);
	    		  Aclass = FixConflicts(shipnum,Aclass);
	    	  }
	    	  strmsg.append(shipnum +",");
	    	}
	    	m_botAction.sendSmartPrivateMessage(sender,strmsg.toString());
	    }
	    catch(NumberFormatException e)	{
	      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !SetMainShps/!SetReserShps <ship1#>:<ship2#>..ect");
	    }
	    catch(Exception e)	{
	      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
	    }	
	}
	
	/**
	 * Sets up the death limits for both primary and reserve ships pending
	 * the status of the sent boolean main. If the game is in progress,
	 * the values cannot be changed unless !stop is issued.
	 * 
	 * @param sender is the user of the bot.
	 * @param deathlimit is a string containing the death limit.
	 * @param main is the accompanying boolean the decides wither the 
	 * method is compiling the primary or reserve ships.
	 */
	
	public void doDths(String sender, String deathlimit, boolean main)	{	
		if(validstart)	{ 
			m_botAction.sendSmartPrivateMessage(sender,"Game in progress, !Stop then change definitions");
			return;
		}
		
		String mainstr = ("primary");
	    if(!main)
	    	mainstr = ("reserve");
		try	{
			int deaths = Integer.parseInt(deathlimit);
			if (deaths < 0)
				throw new IllegalArgumentException("Deaths can't be negative");
			if(main)	{
				if(deaths == 0) 
					throw new IllegalArgumentException("Deaths cannot be set to 0 on main ships");
				mainlives = deaths;
			}
			else	{
				reservelives = deaths;
				if(deaths == 0)	{
					m_botAction.sendSmartPrivateMessage(sender,"Infinite deaths set for reserve ships");
					return;
				}
			}
			m_botAction.sendSmartPrivateMessage(sender, mainstr + " deaths have been set at: " + deaths);
	    }
	    catch(NumberFormatException e)	{
	      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !SetMainDths/!SetReserDths <#ofDeaths>..ect");
	    }
	    catch(Exception e)	{
	      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
	    }	
	}

	/**
	 * Assigns a death message.
	 * 
	 * @param sender is the user of the bot.
	 * @param message is the sent death message.
	 */
	
	public void doDedMsg(String sender, String argString)	{
		StringTokenizer argTokens = getArgTokens(argString);
	    int numArgs = argTokens.countTokens();
	    
	    try	{
	    	switch(numArgs){
		    case 1:
		    	deathmsg=argString;	
				break;
		    case 2:
		    	deathmsg=argTokens.nextToken();
		    	int sound = Integer.parseInt(argTokens.nextToken());
		    	deathmsg+= "%" + sound;
		    }
		    m_botAction.sendSmartPrivateMessage(sender, "The current spec message is now: (player) " + argString + " (#)Kills (#)Deaths");
	    }
	    catch(NumberFormatException e)	{
		      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !Setdedmsg <message>:<sound>");
		    }
		catch(Exception e)	{
		      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
		}
	}
	
	/**
	 * Sets the desired number of frequencies where the incoming string
	 * contains the desired number of frequencies which is a non-zero, 
	 * non-negative value.If a game is in progress it cannot be changed
	 * unless !stop is issued.
	 * 
	 * @param sender is the user of the bot.
	 * @param value is the number of frequencies to be used.
	 */
	
	public void doSetFreqs(String sender, String value)	{
		if(validstart)	{ 
			m_botAction.sendSmartPrivateMessage(sender,"Game in progress, !Stop then change definitions");
			return;
		}
		
		try	{
			int freqs = Integer.parseInt(value);
			if(freqs <= 0)
				throw new IllegalArgumentException("0 or negative freqs is impossible");
			numfreqs = freqs;
			m_botAction.sendSmartPrivateMessage(sender, "There will be " + numfreqs + " freqs");
		}
		catch(NumberFormatException e)	{
	        m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !setFreqs <positive#>");
	      }
	    catch(Exception e)	{
	        m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
	      }
	}
	
	public void doSetExcepts(String sender, String argString)	{
		StringTokenizer argTokens = getArgTokens(argString);
	    int numArgs = argTokens.countTokens();
	    try	{
	    	StringBuilder strmsg = new StringBuilder("The following players are now exempt : ");
	    	for (int i=0;i<numArgs;i++)	{
	    		String name = argTokens.nextToken();
	    		if (IsValidPlayer(name));	{
	    			Exmpt.add(name);
	    			strmsg.append(name + ", ");
	    		}
	    		m_botAction.sendSmartPrivateMessage(sender, strmsg.toString());
	    	}
	    }
	    catch(Exception e)	{
	        m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !setExcepts <name1>:<name2>..ect");
	      }
	    }
	
	/**
	 * Sets the number of deaths late comers receive if the option
	 * is enabled. 
	 * 
	 * @param sender is the user of the bot.
	 * @param argString contains a value less than primary lives and
	 * is greater than zero.
	 */
	
	public void doLates(String sender, String argString)	{
	      try	{
	    	  int var = Integer.parseInt(argString);
	    	  if(var <= 0 || var > mainlives)
	    		 throw new IllegalArgumentException("Cannot give negative, 0 lives, or more lives than Primary lives");
	    	  latelives = var;
	    	  m_botAction.sendSmartPrivateMessage(sender, "Late lives are now set at: " + latelives);
	      }
	      catch(NumberFormatException e)	{
	        m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !setLate <lives>");
	      }
	      catch(Exception e)	{
	        m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
	      }
	}
	
	/**
	 * A switch on wither or not to allow late comers.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doLateSwitch(String sender)	{
		Late = !Late;
		if(Late == true)
			m_botAction.sendArenaMessage("Lates turned on -" + sender,1);
		else
			m_botAction.sendArenaMessage("Lates turned off -" + sender,25);
	}
	
	/**
	 * Resets the definitions, clears the ArrayLists, and stops the game
	 * if in progress.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doReset(String sender)	{
		Integer x = new Integer(1);
		Integer y = new Integer(3);
		validstart=false;
		
		Aclass.clear();		Aclass.add(x);
		Bclass.clear();		Bclass.add(y);
		mainlives=2;		reservelives=2;
		latelives=1;		numfreqs=2;
		deathmsg = ("is out with");	Late=true;
		Exmpt.clear();		if(!playerMap.isEmpty()) playerMap.clear();
		
		Iterator i = m_botAction.getPlayerIterator();
        if( i == null ) return;
        while( i.hasNext() )	{
            Player plyr = (Player)i.next();
            if (!IsValidShip(plyr.getShipType()))
            		m_botAction.spec(plyr.getPlayerID());
        }
		
		m_botAction.sendSmartPrivateMessage(sender, "All settings returned to default");
		m_botAction.sendArenaMessage("Game  Stopped, if you're still frozen in spec, PM me !unfrez -" + m_botAction.getBotName(), 1);
	}
	
	/**
	 * Relays a confirm message to the user on the current definitions
	 * to double check.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doConfirm(String sender)	{
		m_botAction.sendSmartPrivateMessage(sender, "You have the following definitions:");
		StringBuilder strshp = new StringBuilder("<Primary ships: " + Aclass.toString());
		strshp.append("> <Reserve Ships: " + Bclass.toString());
		strshp.append("> || Primary deaths: " + mainlives + " Reserve Deaths: " + reservelives + " Late lives: " + latelives + " Freqs: " + numfreqs + " ||");
		m_botAction.sendSmartPrivateMessage(sender,strshp.toString());
		m_botAction.sendSmartPrivateMessage(sender,"Players currently exempt are: " + Exmpt.toString());
		m_botAction.sendSmartPrivateMessage(sender,"Your current spec message is: (player) " + deathmsg + " Kills (#) Deaths (#)");		
	}
	
	/**
	 * Starts the bot by activating the event handlers. It also creates a
	 * snapshot of the arena's players organizing those not spectating
	 * into the desired frequencies while the spectators are locked.
	 * Those playing are added into a hashset to access their deaths and
	 * allow lagouts.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doStart(String sender)	{
			if(validstart)	{
				m_botAction.sendSmartPrivateMessage(sender, "Game already in progress");
				return;		
			}
			else if(Bclass.size() != 0 && Aclass.size() != 0)
				validstart=true;
			else	{
				m_botAction.sendSmartPrivateMessage(sender, "Double check your definitions, a parameter is empty");
				return;	
			}		
		
		String reservestr;						//purely aesthetic
		if(reservelives == 0) 
			reservestr = "infinite backup lives";
		else
			reservestr = reservelives + " backup lives";
		
		m_botAction.toggleLocked();
        m_botAction.scoreResetAll();
        m_botAction.createNumberOfTeams(numfreqs);
		Iterator i = m_botAction.getPlayerIterator();
        if( i == null ) return;
        while( i.hasNext() )	{
            Player plyr = (Player)i.next();
            String name = plyr.getPlayerName();
            if (IsValidShip(plyr.getShipType()))
            	playerMap.put( name, new PlayerProfile( name, randomship(0), plyr.getFrequency() ) );
            else
            	m_botAction.spec(name);
        }
        m_botAction.toggleLocked();
        m_botAction.sendSmartPrivateMessage(sender,"Game Started");
        m_botAction.sendArenaMessage("Game set to " + mainlives + " main lives-(Ships: " + Aclass.toString()+ ") and " + reservestr + "-(Ships: " + Bclass.toString() + ")", 1);
        m_botAction.sendTeamMessage("Late? PM me !lemmein to get in -" + m_botAction.getBotName(), 10);
	}
	
    /**
     * Locks new players in spec to allow the participating players the ability to
     * ship change in this pseudo-locked game. 
     */
    
	public void handleEvent( PlayerEntered event ) {
        if(validstart)	{
        	String plyrName=event.getPlayerName();
        	m_botAction.spec(plyrName);
        	m_botAction.sendSmartPrivateMessage(plyrName, "lagged out? or want in? PM me !lemmein -" + m_botAction.getBotName());
        }
    }
    
    /**
     * Maintains the separation between primary and reserve ships in this
     * pseudo-locked game. In a player is spec'd, send them the lag-out message.
     */
	
	public void handleEvent( FrequencyShipChange event )	{
        if(validstart)	{
        	String plyrName = m_botAction.getPlayerName(event.getPlayerID());
        	if(IsExempt(plyrName.toLowerCase())) return;
        	Integer shipnumber = new Integer(event.getShipType());
        	if(shipnumber.intValue() == 0)	{
        		m_botAction.spec(plyrName);
        		m_botAction.sendSmartPrivateMessage(plyrName, "lagged out? or want in? PM me !lemmein -" + m_botAction.getBotName());
        		return;
        	}
        	PlayerProfile plyrP;
            plyrP = playerMap.get( plyrName );
            int deaths = plyrP.getDeaths();
            if (deaths < mainlives)	{
        		if(Aclass.contains(shipnumber))
        			return;											//Still have primary lives and in correct ships? ok
        		else	{
        			m_botAction.setShip(plyrName, randomship(0));	//If not in right ships, set them
        			m_botAction.sendPrivateMessage(plyrName, "You may only use ships: " + Aclass.toString());
        		}
        	}
        	else	{
        		if(Bclass.contains(shipnumber))
        			if(reservelives == 0 || deaths < reservelives + mainlives)	// Infinite reserve is on or do you still have lives and are in the correct ship? ok
        				return;
        		else	{
        			if(deaths < reservelives + mainlives)	{
        				m_botAction.setShip(plyrName, randomship(1));// If not in right ships, set them
        				m_botAction.sendPrivateMessage(plyrName, "You may only use ships: " + Bclass.toString());
        			}
        			else	{
        				m_botAction.spec(plyrName);
        				m_botAction.warnPlayer(plyrName, "What're you trying to pull?"); //Abnormality or host places in player who's past the death limits
        			}
        		}
        			
        	}
        }      
    }
	
	/**
	 * Handles Freq change events to 'lock' the arena, if a player tries to change freqs, 
	 * they're changed back.
	 */
	
	public void handleEvent( FrequencyChange event )	{
		if(validstart)	{
			String plyrName = m_botAction.getPlayerName(event.getPlayerID());
			PlayerProfile plyrP;
	        plyrP = playerMap.get( plyrName );
	        int ofreq = plyrP.getFreq();
	        int nfreq = event.getFrequency();
	        if(nfreq != ofreq)
	        	m_botAction.setFreq(plyrName, ofreq);
		}
    }
    
    /**
     * Forces ship changes in the transition death from primary ship
     * to reserve ship and specs player when out of lives.
     */
	
	public void handleEvent( PlayerDeath event ) {
		if(validstart)	{
			Player plyr  = m_botAction.getPlayer( event.getKilleeID() );
	    	if (plyr == null) return;
	    	
	    	String plyrName = plyr.getPlayerName();
	    	PlayerProfile plyrP;
	        plyrP = playerMap.get( plyrName );
	        int deaths = plyrP.getDeaths();
	        //Used for debug m_botAction.sendPrivateMessage(plyrName, "You got killed and have " + (deaths+1) + " deaths");
	        if(deaths+1 == mainlives)	{
	        	plyrP.addDeath();
	        	m_botAction.sendPrivateMessage(plyrName, "You've been downgraded and may only now use the following ship(s): " + Bclass.toString());
	        	m_botAction.setShip(plyrName, randomship(1));
	        	m_botAction.sendPrivateMessage(plyrName, "*prize 7");
	        }
	        else if (deaths+1 < mainlives || deaths+1 < reservelives + mainlives)
	        	plyrP.addDeath();
	        else if(reservelives == 0)
	        		return;
	        else	{
	        	m_botAction.sendPrivateMessage(plyrName, "Out of lives!");
	        	m_botAction.spec(plyrName);
	        	m_botAction.sendArenaMessage( plyrName + " " + deathmsg  + " (" + plyr.getWins()+ ") Kills ("  + plyr.getLosses() + ") Deaths");
	        }
		}
    	
        	
        	
    }
    
    /**
     * Helper method that returns a random ship that is either primary or
     * reserve.
     * 
     * @param var is either 0 or 1 where 0 calculates a primary ship and
     * 1 calculates a reserve ship.
     * @return a valid ship number in the requested class.
     */
	
	private int randomship(int var)	{
    	if (var==0)
    		return ((Integer)Aclass.get(rand.nextInt(Aclass.size()))).intValue();
    	else
    		return ((Integer)Bclass.get(rand.nextInt(Bclass.size()))).intValue();
    			
    }
    
    /**
     * Helper method that filters out the unmentionables which are
     * invalid ship types or array duplicates.
     * 
     * @param shipnum is the ship number in question.
     * @param array is the array checked for duplicates of shipnum.
     */
	
	private void IsValidShip(Integer shipnum, ArrayList array)	{
    	if (shipnum.intValue()<1 || shipnum.intValue()>=9)
    		throw new IllegalArgumentException("Invalid ship number listed");
    	for (int i = 0;i<array.size();i++)
    		if(((Integer)array.get(i)).equals(shipnum))
    				throw new IllegalArgumentException("Duplicate ships detected in list");
    }
    
    /**
     * overloaded method for simple ship checks
     * 
     * @param shipnum is the ship number in question.
     * @return true if valid, false if not.
     */
	
	private boolean IsValidShip(int shipnum)	{
    	if (shipnum<1 || shipnum>=9)
    		return false;
    	return true;  	
    }
	
	/**
	 * Helper method to check if the player is in the arena.
	 * -might be some discrepancies with similar named players.
	 * 
	 * @param name is the player.
	 * @return true if the player is present.
	 */
	
	private boolean IsValidPlayer(String name)	{
		if (m_botAction.getFuzzyPlayerName(name).equals(name))
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
		if (Exmpt.contains(name))
			return true;
		return false;
	}
	
	/**
	 * Helper method used to return the freq with the lowest players
	 * to help with balance when adding new players.
	 * @return the int value of the freq with the lowest players or freq 0 is they're all
	 * the same amount.
	 */
	
	private int GetLowest()	{
		int least = 900;
		int count = 0;
		int lowfreq = 0;
		try	{
			for(int i=0;i<numfreqs-1;i++)
			{
				Iterator iterator = m_botAction.getFreqIDIterator(i);
				while(iterator.hasNext()){count++; iterator.next();}
				if(count<least){least=count;lowfreq=i;}
				count=0;
			}
			return lowfreq;
		}
	     catch(Exception e)	{return 0;}
	}
    
    /**
     * Helper method used by doShps that eliminates conflictions between
     * Aclass and Bclass.
     * 
     * @param shipnum is the ship in question between the two.
     * @param array is the array where similar values are removed.
     * @return the fixed array.
     */
	
	private ArrayList FixConflicts(Integer shipnum, ArrayList array)
    {
    		for (int i = 0;i<array.size();i++)
    			if (((Integer)array.get(i)).equals(shipnum))
    				{array.remove(i);i--;}
    		return array;
    }
	
    /**
     * Handles commands from ERs+
     * 
     * @param sender is the user of the bot.
     * @param message is the command.
     */
	
	public void handleCommand(String sender,String message)	{
    	boolean main = true;//Saves code
    	String command = message.toLowerCase(); // for death message
    	if(command.startsWith("!setmainshps "))
        	doShps(sender, command.substring(13).trim(),main);
        if(command.startsWith("!setresershps"))
            doShps(sender, command.substring(14).trim(),!main);
        if(command.startsWith("!setmaindths "))
            doDths(sender, command.substring(13).trim(),main);
        if(command.startsWith("!setreserdths "))
            doDths(sender, command.substring(14).trim(),!main);
        if(command.startsWith("!setdedmsg "))
            doDedMsg(sender, message.substring(11).trim());
        if(command.startsWith("!setfreqs "))
            doSetFreqs(sender, command.substring(10).trim());
        if(command.startsWith("!setexcepts "))
            doSetExcepts(sender, command.substring(12).trim());
        if(command.startsWith("!setlate "))
            doLates(sender, command.substring(9).trim());
        if(command.startsWith("!late"))
            doLateSwitch(sender);
        if(command.startsWith("!stop"))
            doReset(sender);
        if(command.startsWith("!confirm"))
            doConfirm(sender);
        if(command.startsWith("!start"))
            doStart(sender);
      }
    
	/**
	 * Handles player commands.
	 * 
	 * @param sender is the player.
	 * @param message is the command.
	 */
	
	public void handlePlayerCommand(String sender, String message)	{ //left so more commands could be added
    	if(message.startsWith("!lemmein") && validstart)
    		doAddPlayer(sender);
    	if(message.startsWith("!unfrez") && !validstart)
    		doUnfrez(sender);
    }
    
	/**
	 * Adds a new player to the hashset and chooses a random freq and ship
	 * for the player. If the player is returning, it looks up the player
	 * using his/her name as a key.
	 * @param name is the player's name.
	 */
	
	public void doAddPlayer(String name)	{
		if(!playerMap.containsKey( name )) {
			if (!Late)	{
				m_botAction.sendPrivateMessage(name, "Sorry but Lates have been disabled");
				return;
			}
            int newship = randomship(0);
            int freq = GetLowest();
            m_botAction.spec(name);
            m_botAction.setShip( name, newship );
            m_botAction.setFreq( name, freq );
            PlayerProfile plyrP = new PlayerProfile( name, newship, freq );
            playerMap.put( name, plyrP );
            for(int i=0;i<mainlives - latelives;i++)
            	plyrP.addDeath();          	
        }
        else {
            PlayerProfile tempP;
            tempP = playerMap.get( name );
            int deaths= tempP.getDeaths();
            m_botAction.spec(name);
            if(reservelives == 0)
            	m_botAction.setShip( name, randomship(1) );
            else if(deaths+1 >= reservelives + mainlives)	{
            	m_botAction.sendPrivateMessage(name, "Sorry, but you're already out");
            	m_botAction.spec(name);
            	return;
            }
            else if(deaths+1 < mainlives )
            	m_botAction.setShip( name, randomship(0) );
            else
            	m_botAction.setShip( name, randomship(1) );
            	
            m_botAction.setFreq( name, tempP.getFreq() );
        }
	}
	
	/**
	 * PMs the player with *spec to attempt to get them unlocked.
	 * @param name
	 */
	
	public void doUnfrez(String name)	{
		m_botAction.spec(name);
		m_botAction.sendPrivateMessage(name, "Ok, attempting to unfrezze you. if you're still frozen pm me !unfrez again. -" + m_botAction.getBotName());
	}
    
	/**
	 * Returns this module's help listing.
	 */
	
	public String[] getHelpMessages()	{
	      String[] message = {
	          "!SetMainShps <ship#1>:<Ship#2>..ect         -- List of main ships.",
	          "!SetReserShps <ship#1>:<Ship#2>..ect        -- List of reserve ships.",
	          "!SetMainDths <#ofDeaths>                    -- Number of deaths before shunted to reserve.",
	          "!SetReserDths <#ofDeaths>                   -- Number of deaths on reserve before spec'd. (0 = infinite)",
	          "!SetDedMsg <message>:<sound#>               -- Arena Message to be displayed when a player is out of lives;",
	          "                                               formatting is: (name)(your message)(kills)(deaths)",
	          "!SetFreqs <positive#>                       -- Set the number freqs.",
	          "!SetExcepts <name1>:<name2>..ect            -- Set players to be exempt from ship changes but are still",
	          "                                               freq locked upon start.",
	          "!SetLate <Lives>                            -- Set the lives late comers get while lates are on.",
	          "!Late                                       -- Turns lates on/off. (default is on)",
	          "!Stop                                       -- Resets to default values and stops the game if in progess.",
	          "!Confirm                                    -- Lists the curret definitions.",
	          "!Start                                      -- Loads up definitions and starts.",
	      };
	      return message;
	  }

}
