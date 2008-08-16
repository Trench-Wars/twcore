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
 * References: zombies, pubbot, prodem
 * 
 * @author Ayano / Ice-demon
 *
 */
public class utilshipc extends MultiUtil
{


	ArrayList <Integer>Aclass;
	ArrayList <Integer>Bclass;
	ArrayList <String>Exmpt;
	ArrayList <String>Spec;
	int mainlives=2;
	int reservelives=2;
	int latelives=1;
	int numfreqs=2;
	String deathmsg = "is out with";
	String bclassmsg = null;
	boolean validstart = false;
	boolean Late = true;
	Random rand = new Random();
	private HashMap<String, PlayerProfile> playerMap;
	TimerTask notify;

	/**
	 * initialize variables
	 */
	
	public void init()	{
	Integer x = new Integer(1);
	Integer y = new Integer(3);
	Aclass = new ArrayList<Integer>();
	Bclass = new ArrayList<Integer>();
	Exmpt = new ArrayList<String>();
	Spec = new ArrayList<String>();
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
		events.request(this, EventRequester.PLAYER_LEFT );
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
		        handleCommand(playerName, event.getMessage());
			else
				handlePlayerCommand(playerName, event.getMessage().toLowerCase());
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
	 * Creates the arena message for when a ship is shunted to reserve.
	 * the message can be erased to not display at all if '~' is
	 * sent.
	 * 
	 * @param sender is the user of the bot.
	 * @param message is the reserve message.
	 */
	
	public void doBclassMsg(String sender, String message)	{    
    	if(message.equals("~")) {
    		bclassmsg = null;
    		m_botAction.sendPrivateMessage(sender, "Reserve message removed");
    	}
    	else	{
    		bclassmsg=message;	
        	m_botAction.sendPrivateMessage(sender, "Your current reserve message is: (player) " + message);
    	}
    }
	
	/**
	 * Assigns a death message.
	 * 
	 * @param sender is the user of the bot.
	 * @param message is the sent death message.
	 */
	
	public void doDedMsg(String sender, String message)	{    
		    	deathmsg=message;	
		    	m_botAction.sendPrivateMessage(sender, "Your current spec message is: (player) " + message + " Kills (#) Deaths (#)");
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
	
	/**
	 * Generates a list of players who are exempt from ship 
	 * classification.
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is the String containing the players to be exempt.
	 */
	
	public void doSetExcepts(String sender, String argString)	{
		StringTokenizer argTokens = getArgTokens(argString);
	    int numArgs = argTokens.countTokens();
	    try	{
	    	StringBuilder strmsg = new StringBuilder("The following players are now exempt : ");
	    	for (int i=0;i<numArgs;i++)	{
	    		String name = argTokens.nextToken();
	    		if (IsValidPlayer(name))
	    			if (!IsExempt(name))	{
	    				Exmpt.add(name);
	    				strmsg.append(name + ", ");
	    			}
	    	}
	    	m_botAction.sendSmartPrivateMessage(sender, strmsg.toString());
	    }
	    catch(Exception e)	{
	        m_botAction.sendSmartPrivateMessage(sender, "Please use vaild player names in the following syntax: !setExcepts <name1>:<name2>..ect");
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
	
	public void doRemove(String sender,String name)	{
		try	{
			if(IsValidPlayer(name))	{
				PlayerProfile plyrP = playerMap.get(m_botAction.getFuzzyPlayerName(name));
			if (plyrP == null)  throw new IllegalArgumentException(name + " is not playing");
				for(int i=0 ;i< mainlives + reservelives ; i++)
					plyrP.addDeath();
				m_botAction.specWithoutLock(plyrP.getName()); //case sensitive I assume, inserted precaution
				m_botAction.sendSmartPrivateMessage(plyrP.getName(),"You have been removed by the host!");
				m_botAction.sendSmartPrivateMessage(sender,name + " has been removed/shunted ");
			}
			else
				m_botAction.sendSmartPrivateMessage(sender,name + " is not a valid name ");
		}
		catch(Exception e)	{
	        m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
	      }
	}
	
	/**
	 * Resets the definitions, clears the ArrayLists, and stops the game
	 * if in progress.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doStop(String sender)	{
		if(!validstart)	{ 
			m_botAction.sendSmartPrivateMessage(sender,"No game in progress");
			return;
		}
		
		Integer x = new Integer(1);
		Integer y = new Integer(3);
		validstart=false;
		
		Aclass.clear();		Aclass.add(x);
		Bclass.clear();		Bclass.add(y);
		mainlives=2;		reservelives=2;
		latelives=1;		numfreqs=2;
		Late=true;			Spec.clear();
		Exmpt.clear();		if(!playerMap.isEmpty()) playerMap.clear();
		deathmsg = "is out with";	bclassmsg = null;	notify.cancel();
		
		m_botAction.sendSmartPrivateMessage(sender, "All settings returned to default");
		m_botAction.sendArenaMessage("Game stopped", 13);
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
		if(bclassmsg != null)
			m_botAction.sendSmartPrivateMessage(sender,"Your current reserve message is: (player) " + bclassmsg);
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
			else if(Bclass.size() == 0 || Aclass.size() == 0)	{
				m_botAction.sendSmartPrivateMessage(sender, "Double check your definitions, a parameter is empty");
				return;	
			}		
		
		String reservestr;						//purely aesthetic
		if(reservelives == 0) 
			reservestr = "infinite backup lives";
		else
			reservestr = reservelives + " backup lives";
		
		m_botAction.toggleLocked();
		m_botAction.createNumberOfTeams(numfreqs);
        
        TimerTask rdy = new TimerTask() {
            public void run() {
            	m_botAction.sendArenaMessage("Game Started!",2);
            	m_botAction.scoreResetAll();
        		Iterator<Player> i = m_botAction.getPlayerIterator();
                if( i == null ) return;
                while( i.hasNext() )	{
                    Player plyr = (Player)i.next();
                    String name = plyr.getPlayerName();
                    int ship = randomship(0);
                    if (plyr.isPlaying())	{
                    	playerMap.put( name, new PlayerProfile( name, ship , plyr.getFrequency() ) );
                    	m_botAction.setShip(name, ship);
                    }
                    else
                    	AddSpec(name);
                }
                m_botAction.sendPublicMacro("*prize #7");
                m_botAction.toggleLocked();validstart=true;
                m_botAction.sendTeamMessage("Late? PM me !lemmein to get in -" + m_botAction.getBotName(), 10);
                startNotify();
             }

        };
        
        m_botAction.scheduleTask(rdy, 5000);
        m_botAction.sendArenaMessage("Game set to " + mainlives + " main lives-(Ships: " + Aclass.toString()+ ") and " + reservestr + "-(Ships: " + Bclass.toString() + ")");
        m_botAction.sendArenaMessage("Game starts in 5 seconds",1);
	}
	
    /**
     * Locks new players in spec to allow the participating players the ability to
     * ship change in this pseudo-locked game. 
     */
    
	public void handleEvent( PlayerEntered event ) {
        if(validstart)	{
        	String plyrName=event.getPlayerName();
        	AddSpec(plyrName);
        	m_botAction.specWithoutLock(plyrName);
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
        	if(Spec.contains(plyrName)) {m_botAction.specWithoutLock(plyrName);return;}
        	
        	Integer shipnumber = new Integer(event.getShipType());
        	PlayerProfile plyrP;
            plyrP = playerMap.get( plyrName );
            if(plyrP == null) {m_botAction.specWithoutLock(plyrName);AddSpec(plyrName);return;} //if the most manually adds, spec, for they arn't on the hash
            if(IsExempt(plyrName.toLowerCase())) return;
            int deaths = plyrP.getDeaths();
            
            if(shipnumber.intValue() == 0)	{ //don't confuse lag-outs/specs with game outs.
        		AddSpec(plyrName);
        		m_botAction.sendSmartPrivateMessage(plyrName, "lagged out? or want in? PM me !lemmein -" + m_botAction.getBotName());
        		return;
        	}
            
            if (deaths < mainlives)	{
        		if(Aclass.contains(shipnumber))
        			return;											//Still have primary lives and in correct ships? ok
        		else	{
        			m_botAction.setShip(plyrName, randomship(0));	//If not in right ships, set them
        			m_botAction.sendPrivateMessage(plyrName, "You may only use ships: " + Aclass.toString());
        		}
        	}
        	else	{
        		if(Bclass.contains(shipnumber))	{
        			if(reservelives == 0 || deaths < reservelives + mainlives)	// Infinite reserve is on or do you still have lives and are in the correct ship? ok
        				return;
        			else				//note: odd how it wouldn't lock reserve ships until I added this else and bracketed in the Bclass.contains
        				{m_botAction.specWithoutLock(plyrName);AddSpec(plyrName);}
        		}
        		else if(deaths < reservelives + mainlives)	{
        				m_botAction.setShip(plyrName, randomship(1));// If not in right ships, set them
        				m_botAction.sendPrivateMessage(plyrName, "You may only use ships: " + Bclass.toString());
        		}
        		else	{
        				m_botAction.specWithoutLock(plyrName);
        				AddSpec(plyrName);
        				m_botAction.warnPlayer(plyrName, "Stay in spec u hobo"); //Abnormality or host places in player who's past the death limits
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
	        if(plyrP == null) {m_botAction.specWithoutLock(plyrName);AddSpec(plyrName);return;} //if the most manually adds, spec, for they arn't on the hash
	        int deaths = plyrP.getDeaths();
	        //Used for debug m_botAction.sendPrivateMessage(plyrName, "You got killed and have " + (deaths+1) + " deaths");
	        if(deaths+1 == mainlives)	{
	        	if(bclassmsg != null)	{
	        		if(bclassmsg.contains("%"))	{
	        			int soundPos = bclassmsg.indexOf('%');
	        			m_botAction.sendArenaMessage( plyrName + " " + bclassmsg.substring(0,soundPos),GetSound(bclassmsg,soundPos) );
	        		}
	        		else
	        			m_botAction.sendArenaMessage( plyrName + " " + bclassmsg );
	        	}
	        		
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
	        	m_botAction.specWithoutLock(plyrName);
	        	AddSpec(plyrName);
	        	if(deathmsg.contains("%"))	{
	        		int soundPos = deathmsg.indexOf('%');
    	        	m_botAction.sendArenaMessage( plyrName + " " + deathmsg.substring(0,soundPos)  + " (" + plyr.getWins()+ ") Kills ("  + plyr.getLosses() + ") Deaths",GetSound(deathmsg,soundPos) );
	        	}
	        	else
	        		m_botAction.sendArenaMessage( plyrName + " " + deathmsg  + " (" + plyr.getWins()+ ") Kills ("  + plyr.getLosses() + ") Deaths");
	        }
		}
    	
        	
        	
    }
	
	/**
	 * periodically sends a team message notifying players how to
	 * enter the game if they arrive late
	 */
	
	private void startNotify(){
        //Timer setup.
        notify = new TimerTask() {
            public void run() {
                    m_botAction.sendTeamMessage("Late and want in? pm me !lemmein to get in -" + m_botAction.getBotName() );
            }
        };
        m_botAction.scheduleTaskAtFixedRate( notify, 30000, 30000 );

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
	
	private void IsValidShip(Integer shipnum, ArrayList<Integer> array)	{
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
		if (m_botAction.getFuzzyPlayerName(name).equalsIgnoreCase(name))
			return true;
		return false;
	}
	
	/**
	 * Simple helper method that adds players to the spec list.
	 * 
	 * @param name is the player.
	 */
	
	private void AddSpec(String name)	{
		if (Spec.contains(name))
			return;
		Spec.add(name);
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
	 * Recursive helper method used to return the freq with the
	 * lowest players to help with balance when adding new players. 
	 * If there's less freqs than numfreqs, it recurses numfreqs-1. 
	 * 
	 * @return the int value of the freq with the lowest players or freq 1 if they're all
	 * the same amount.
	 */
	
	private int GetLowest(int freqs)	{
		int least = 900,count = 0,lowfreq = 0;

		if(freqs < 0)return 1;
		try	{
			for(int i=freqs-1 ; i>=0 ; i--)
			{
				count = m_botAction.getFrequencySize(i);
				if(count<least){least=count;lowfreq=i;}
			}
			return lowfreq;
		}
	     catch(Exception e)	{return GetLowest(freqs-1);}
	}
	
	/**
	 * Helps with sounds..
	 * 
	 * @param message is the death or reserve message.
	 * @param soundPos is the position of the '%'
	 * @return the sound code;
	 */
	
	private int GetSound(String message, int soundPos)	{
		int soundCode;
		try{
            soundCode = Integer.parseInt(message.substring(soundPos + 1));
        } catch( Exception e ){
            soundCode = 1;
        }
        return soundCode;
	}
    
    /**
     * Helper method used by doShps that eliminates conflictions between
     * Aclass and Bclass.
     * 
     * @param shipnum is the ship in question between the two.
     * @param array is the array where similar values are removed.
     * @return the fixed array.
     */
	
	private ArrayList<Integer> FixConflicts(Integer shipnum, ArrayList <Integer>array)
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
        if(command.startsWith("!setresermsg "))
            doBclassMsg(sender, message.substring(12).trim());
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
        if(command.startsWith("!remove "))
            doRemove(sender, command.substring(7).trim());
        if(command.startsWith("!stop"))
            doStop(sender);
        if(command.startsWith("!confirm"))
            doConfirm(sender);
        if(command.startsWith("!start"))
            doStart(sender);
        if(message.startsWith("!lemmein") && validstart)
    		doAddPlayer(sender);
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
    	/*if(message.startsWith("!unfrez") && !validstart)
    		doUnfrez(sender);*/
    }
	/**
	 * Adds a new player to the hashset and chooses a random freq and ship
	 * for the player. If the player is returning, it looks up the player
	 * using his/her name as a key.
	 * @param name is the player's name.
	 */
	
	public void doAddPlayer(String name)	{
		
		if(IsValidShip(m_botAction.getPlayer(name).getShipType())) //already playing
				return;
		
		if(!playerMap.containsKey( name )) {
			if (!Late)	{
				m_botAction.sendPrivateMessage(name, "Sorry but Lates have been disabled");
				return;
			}
            int newship = randomship(0);
            int freq = GetLowest(numfreqs);
            Spec.remove(name);
            m_botAction.setShip( name, newship );
            m_botAction.setFreq( name, freq );
            PlayerProfile plyrP = new PlayerProfile( name, newship, freq );
            playerMap.put( name, plyrP );
            m_botAction.sendPrivateMessage(name, "You've been added with only " + latelives + " lives for being late");
            for(int i=0;i<mainlives - latelives;i++)
            	plyrP.addDeath();          	
        }
        else {
            PlayerProfile tempP;
            tempP = playerMap.get( name );
            int deaths= tempP.getDeaths();
            Spec.remove(name);
            if(reservelives == 0)
            	m_botAction.setShip( name, randomship(1) );
            else if(deaths+1 >= reservelives + mainlives)	{
            	m_botAction.sendPrivateMessage(name, "Sorry, but you're already out");
            	Spec.add(name);
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
	/*
	public void doUnfrez(String name)	{
		m_botAction.spec(name);
		m_botAction.sendPrivateMessage(name, "Ok, attempting to unfrezze you. if you're still frozen pm me !unfrez again. -" + m_botAction.getBotName());
	}
    */
	/**
	 * Returns this module's help listing.
	 */
	
	public String[] getHelpMessages()	{
	      String[] message = {
	    	  "=SHIPC===================================================================================================SHIPC=",
	          "!SetMainShps <ship#1>:<Ship#2>..ect         -- List of main ships.",
	          "!SetReserShps <ship#1>:<Ship#2>..ect        -- List of reserve ships.",
	          "!SetMainDths <#ofDeaths>                    -- Number of deaths before shunted to reserve.",
	          "!SetReserDths <#ofDeaths>                   -- Number of deaths on reserve before spec'd. (0 = infinite)",
	          "!SetReserMsg <message>                      -- Arena Message to be displayed when a player is put on reserve;",
	          "                                               format: (name)(your message) use %% for sounds, send '~' to remove.",
	          "!SetDedMsg <message>                        -- Arena Message to be displayed when a player is out of lives.;",
	          "                                               format: (name)(your message)(kills)(deaths) use %% for sounds.",
	          "!SetFreqs <positive#>                       -- Set the number freqs.",
	          "!SetExcepts <name1>:<name2>..ect            -- Set players to be exempt from ship changes but are still",
	          "                                               freq locked upon start. (exact name ignoring case))",
	          "!SetLate <Lives>                            -- Set the lives late comers get while lates are on.",
	          "!Late                                       -- Turns lates on/off. (default is on)",
	          "!Remove <name>                              -- Removes a player into spec (exact name ignoring case)",
	          "                                            -- if Infinte lives is on they will be shunted", 
	          "!Stop                                       -- Resets to default values and stops the game",
	          "!Confirm                                    -- Lists the curret definitions.",
	          "!Start                                      -- Loads up definitions and starts.",
	          "===============================================================================================================",
	          "NOTE: Ensure that the arena has nobody *spec locked",
	          "NOTE: If you wish to remove someone use the !remove, don't *spec",
	          "NOTE: Don't *setfreq as when the player attemps to freq switch they'll snap back."
	      };
	      return message;
	  }
	
	public void cancel()	{
		if (validstart)
			notify.cancel();
	}

}
