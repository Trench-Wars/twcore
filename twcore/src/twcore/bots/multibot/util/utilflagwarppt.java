package twcore.bots.multibot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.FlagClaimed;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/**
 * An advanced utility that creates warp points from
 * groups of flags. Use of this utility is intended for
 * specialist events or trained/seasoned ERs.
 *
 * @author Ayano
 *
 */

public class utilflagwarppt extends MultiUtil {
	
	public ArrayList<Integer> harvested;
	public HashMap<Integer,ArrayList<Integer>> groups;
	public HashMap<Integer,Holding_Freq> freqs;
	public HashMap<String,Warp_Point> points;
	public Integer gdex;
	public int explorer;
	public boolean exploring;
	
	public String claimedmsg = null;
	public String contestedmsg = null;

	public utilflagwarppt ()	{

	}

	/**
	 * Initializes variables.
	 */

	public void init()	{
		harvested = new ArrayList<Integer>();
		groups = new HashMap<Integer,ArrayList<Integer>>();
		freqs = new HashMap<Integer,Holding_Freq>();
		points = new HashMap<String,Warp_Point>();
		gdex = 1;
	}

	/**
	 * Requests events.
	 */

	public void requestEvents( ModuleEventRequester modEventReq ) {
		modEventReq.request(this, EventRequester.FLAG_CLAIMED );
    }

	/**
     * Gets the argument tokens from a string.  If there are no colons in the
     * string then the delimeter will default to space.
     *
     * @param string is the string to tokenize.
     * @param token is the token to use.
     * @return a tokenizer separating the arguments is returned.
     */

	public StringTokenizer getArgTokens(String string, String token)	{
	    if(string.indexOf(token) != -1)
	      return new StringTokenizer(string, token);
	    return new StringTokenizer(string);
	  }

	/**
	 * Helper method that returns true if a flag exists in the arena.
	 *
	 * @param flag is the flag id.
	 * @return true if the flag exists.
	 */

	public boolean isValidFlag (int flag)	{
		Iterator<Integer> flags = m_botAction.getFlagIDIterator();
		if (!flags.hasNext())
			throw new IllegalArgumentException ("There are no flags to make points from.");
		while (flags.hasNext()){
			if (flags.next().intValue() == flag)
				return true;
		}
		return false;
	}

	/**
	 * Turns on/off exploring for flags which enables harvesting.
	 *
	 * @param sender is the user of the bot.
	 */

	public void doExplore(String sender)	{
		exploring=!exploring;
		if (exploring)	{
			doReleaseFlags(sender);
			m_botAction.sendPrivateMessage(sender,
					"Exploring initiated, " +
					"start claiming flags for the first group.");
			explorer = m_botAction.getPlayerID(sender);
		}
		else	{
			m_botAction.sendPrivateMessage(sender,
					"Exploring stopped");
			harvested.clear();
			doReleaseFlags(sender);
			doListGroups(sender);
		}
	}

	/**
	 * Creates a group out of the harvested flags.
	 *
	 * @param sender is the user of the bot.
	 */

	public void doSetGroup(String sender)	{
		if (harvested.isEmpty())	{
			m_botAction.sendPrivateMessage(sender,
					"No flags have been harvested");
			return;
		}
		//groups.put(gdex, (ArrayList<Integer>)harvested.clone());
		groups.put(gdex, new ArrayList<Integer>(harvested));

		m_botAction.sendPrivateMessage(sender, "Flags "
				+ harvested.toString() + " are now group " + gdex);

		harvested.clear();
		gdex++;
	}



	/**
	 * Lists all harvested groups
	 *
	 * @param sender
	 */

	public void doListGroups (String sender)	{
		if (groups.isEmpty())
			m_botAction.sendPrivateMessage(sender,"No groups are left, " +
												"or there are no groups to list");
		else	{
		m_botAction.sendPrivateMessage(sender, "Current groups:");
		ListSort(groups,sender);
		}
	}

	/**
	 * Clears all harvested groups
	 *
	 * @param sender
	 */

	public void doClearGroups (String sender)	{
		if (groups.isEmpty())
			m_botAction.sendPrivateMessage(sender,"No groups to, clear.");
		else	{
			m_botAction.sendPrivateMessage(sender, "Groups cleared.");
			groups.clear();
		}
	}

	/**
	 * Adds a point using a group number for the source of IDs.
	 * This group is erased from the list of groups after a
	 * successful point  is added.
	 *
	 * @param sender is the user of th bot.
	 * @param argString is the argument string to be tokenized and
	 * translated.
	 */

	public void doAddGroupPoint(String sender, String argString)	{
		if (exploring)	{
			m_botAction.sendPrivateMessage(sender,
					"cannot conduct while explore is currently in progess");
			return;
		}

		try	{
		StringTokenizer argTokens = getArgTokens(argString, ",");
		Integer groupNum = Integer.parseInt(argTokens.nextToken());
		String wpptName = argTokens.nextToken();
		StringTokenizer wpptTokens = getArgTokens(argTokens.nextToken(), ":");
		ArrayList<Integer> ids = new ArrayList<Integer>();

		if (groups.containsKey(groupNum))
			ids = groups.get(groupNum);
		else
			throw new IllegalArgumentException (
			"Invalid group number listed");

		int xcoord = Integer.parseInt(wpptTokens.nextToken()),
		ycoord = Integer.parseInt(wpptTokens.nextToken()),
		radius = Integer.parseInt(wpptTokens.nextToken());

		Warp_Point newPoint = new Warp_Point(ids,wpptName,xcoord,ycoord,radius);
		points.put(wpptName, newPoint);
		groups.remove(groupNum);
		m_botAction.sendPrivateMessage(sender, "Point added: " + newPoint.toString());

		}
		catch (Exception e)	{
			if (e.getMessage() != null)
				m_botAction.sendPrivateMessage(sender,e.getMessage());
			else
			m_botAction.sendPrivateMessage(
					sender, "Improper syntax, please use !AddGroupPoint" +
							" <group#>,<name>,<destx>:<desty>:<radius>");
		}

	}

	/**
	 * Manually adds a point for known flag IDs
	 *
	 * @param sender is the user of the bot.
	 * @param argString is the argument string to be tokenized
	 * and translated
	 */

	public void doAddPoint(String sender, String argString)	{
		if (exploring)	{
			m_botAction.sendPrivateMessage(sender,
					"cannot conduct while explore is currently in progess");
			return;
		}

		try	{
		StringTokenizer argTokens = getArgTokens(argString, ",");
		StringTokenizer idTokens = getArgTokens(argTokens.nextToken(), ":");
		String wpptName = argTokens.nextToken();
		StringTokenizer wpptTokens = getArgTokens(argTokens.nextToken(), ":");
		ArrayList<Integer> ids = new ArrayList<Integer>();

		while (idTokens.hasMoreTokens())	{
			int subject = Integer.parseInt(idTokens.nextToken());
			if ( isValidFlag(subject) )
				ids.add(new Integer (subject));
			else
				throw new IllegalArgumentException (
						"Invalid flag Id listed.");
		}
		int xcoord = Integer.parseInt(wpptTokens.nextToken()),
		ycoord = Integer.parseInt(wpptTokens.nextToken()),
		radius = Integer.parseInt(wpptTokens.nextToken());

		Warp_Point newPoint = new Warp_Point(ids,wpptName,xcoord,ycoord,radius);
		points.put(wpptName, newPoint);
		m_botAction.sendPrivateMessage(sender, "Point added: " + newPoint.toString());

		}
		catch (Exception e)	{
			if (e.getMessage() != null)
				m_botAction.sendPrivateMessage(sender,e.getMessage());
			else
			m_botAction.sendPrivateMessage(
					sender, "Improper syntax, please use !AddGroupPoint" +
							" <id1>:<id2>:<ect..>,<name>,<destx>:<desty>:<radius>");
		}

	}

	/**
	 * Lists all points.
	 *
	 * @param sender is the user of the bot.
	 */

	public void doListPoints (String sender)	{
		if (!points.isEmpty())	{
		m_botAction.sendPrivateMessage(sender, "Current points:");
		ListSort(points,sender);
		}
		else
			m_botAction.sendPrivateMessage(sender, "There are no points to list.");
	}

	public void doClearPoints (String sender)	{
		if (points.isEmpty())
			m_botAction.sendPrivateMessage(sender,"No points to remove.");
		else	{
			points.clear();
			Iterator<Holding_Freq> freqList = freqs.values().iterator();
			while (freqList.hasNext())
				freqList.next().clearWrpp();
			m_botAction.sendPrivateMessage(sender, "Points cleared.");
		}
	}

	/**
	 * Removes a point.
	 *
	 * @param sender is the user of the bot.
	 * @param pointName is the name of the point to be removed.
	 */

	public void doRemovePoint (String sender, String pointName)	{
		if (points.isEmpty())
			m_botAction.sendPrivateMessage(sender,"No points to remove.");
		else if (!points.containsKey(pointName))
			m_botAction.sendPrivateMessage(sender, "Point does not exist.");
		else	{
			points.remove(pointName);
			Iterator<Holding_Freq> freqList = freqs.values().iterator();
			while (freqList.hasNext())	{
				Holding_Freq currentf = freqList.next();
				if (currentf.containsWrpp(pointName))
					currentf.removeWrpp(pointName);
			}
			m_botAction.sendPrivateMessage(sender,
					"Point: " + pointName + " was removed.");
		}
	}

	/**
	 * Displays the frequencies that hold the set points if any exist.
	 * There may be discrepancies as the held status will not change
	 * until the point is completely owned by a frequency.
	 *
	 * @param sender is the user of the bot.
	 */

	public void doHeld(String sender)	{
		if (points.isEmpty())
			m_botAction.sendPrivateMessage(sender, "There are no points to be held.");
		else	{
			Iterator<Warp_Point> wppts = points.values().iterator();
			while (wppts.hasNext())	{
				Warp_Point currentwp = wppts.next();
				m_botAction.sendPrivateMessage(sender,
						currentwp.m_Name + " is currently held by " 
						+ (currentwp.getHolder() ==-1 ?
								"none" : currentwp.getHolder()) );
			}
		}
	}

	/**
	 * Releases all flags from ownership.
	 *
	 * @param sender is the user of the bot.
	 */

	public void doReleaseFlags (String sender)	{
		m_botAction.resetFlagGame();
		freqs.clear();
		Iterator<Warp_Point>wrppts = points.values().iterator();
		while (wrppts.hasNext())	{
			Warp_Point tmpwppt = wrppts.next();
			tmpwppt.m_Holder = -1;
		}
		
		m_botAction.getShip().setShip(8);
		m_botAction.sendPrivateMessage(sender, "Flags released from all ownership.");
	}
	
	/**
	 * Adds a 'claimed' message for a warp point
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is the message to be implemented.
	 */
	
	public void doClaimedMsg( String sender, String argString )	{
		if (argString.equals("~*"))	{
			claimedmsg = null;
			m_botAction.sendPrivateMessage(sender, "Warp point claimed message " +
					"has been erased.");
		}
		else	{
			claimedmsg = argString;
			m_botAction.sendPrivateMessage(sender, 
					"Warp point claimed message is now : " + claimedmsg);
		}
			
	}
	
	/**
	 * Adds a 'contested' message for a warp point under duress
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is the message to be implemented.
	 */
	
	public void doContestedMsg( String sender, String argString )	{
		if (argString.equals("~*"))	{
			contestedmsg = null;
			m_botAction.sendPrivateMessage(sender, "Warp point contested message " +
			"has been erased.");
		}
		else	{
			contestedmsg = argString;
			m_botAction.sendPrivateMessage(sender, 
					"Warp point contested message is now : " + contestedmsg);
		}
		
	}
	
	public void doListMsgs( String sender )	{
		m_botAction.sendPrivateMessage(sender, "Warp point gained message is: " 
				+ (claimedmsg == null ? "none" : claimedmsg));
		m_botAction.sendPrivateMessage(sender, "Warp point contested message is: " 
				+ (contestedmsg == null ? "none" : contestedmsg));
	}

	/**
	 * Sorts a hash map into a readable format to the user.
	 *
	 * @param list is the hash map.
	 * @param sender is the user of the bot.
	 */

	private void ListSort(HashMap list, String sender)	{
		Iterator key = list.keySet().iterator();
		Iterator value = list.values().iterator();

		while (key.hasNext())	{
			m_botAction.sendPrivateMessage(sender,key.next().toString() 
					+ " -[|   " + value.next().toString() + "   |]");
		}
	}

	/**
	 * Checks the flags and transfers them internally determining
	 * wither or not they constitute for a warp point if any.
	 *
	 * @param flag is the flag id that was gained.
	 * @param gainingfreq is the freq that picked up the flag.
	 */

	private void CheckFlag(Integer flag, int gainingfreq)	{
		Iterator<Holding_Freq> freqList = freqs.values().iterator();
		if (freqList == null)
			return;

		while (freqList.hasNext())	{
			Holding_Freq currentf = freqList.next();
			if (currentf.m_Freq.intValue() == gainingfreq
					&& !currentf.containsFlag(flag))	{
				currentf.addFlag(flag);
				if (points.isEmpty())
					return;
				Iterator<Warp_Point> warppts = points.values().iterator();
				while (warppts.hasNext())	{
					Warp_Point currentwp = warppts.next();
					CheckWppt(currentwp,currentf);
					}
			}
			else if (currentf.containsFlag(flag)
					&& currentf.m_Freq.intValue() != gainingfreq)	{
				currentf.removeFlag(flag);
				if (points.isEmpty())
					return;
				Iterator<Warp_Point> warppts = points.values().iterator();
				while (warppts.hasNext())	{
					Warp_Point currentwp = warppts.next();
					CheckWppt(currentwp,currentf);
				}
			}

		}
	}

	/**
	 * Helper method that sorts out warp point gains and
	 * losses.
	 *
	 * @param wppt is the warp point in question.
	 * @param freq is the freq holding the flag.
	 */

	private void CheckWppt(Warp_Point wppt, Holding_Freq freq)	{
			if (wppt.hasFlags(freq.m_Flags)
					&& !freq.containsWrpp(wppt.m_Name))	{
				freq.addWrpp(wppt.m_Name);
				wppt.setHolder(freq.m_Freq);
				WarpptMsg(freq.m_Freq,wppt.m_Name,claimedmsg);
				notifyFreq(freq.m_Freq, wppt.m_Name,true);
			}
			else if (!wppt.hasFlags(freq.m_Flags)
					&& freq.containsWrpp(wppt.m_Name))	{
				wppt.setHolder(-1);
				freq.removeWrpp(wppt.m_Name);
				if ( wppt.m_Ids.size() > 1)
					WarpptMsg(freq.m_Freq,wppt.m_Name,contestedmsg);
				notifyFreq(freq.m_Freq, wppt.m_Name,false);
			}
	}
	
	private void WarpptMsg(int freq, String wpptName,String message)	{
		int soundpos;
		
		if (message == null)
			return;
		
		try {
			if ((soundpos = message.indexOf('%')) != -1)
				m_botAction.sendArenaMessage(
						"Freq " + freq + " " 
						+ message.substring(0, soundpos) + wpptName
						,Integer.parseInt(message.substring(soundpos+1)));
			else
				m_botAction.sendArenaMessage("Freq " + freq + " " 
						+ message + " " + wpptName);
		} catch (Exception e) 	{
			m_botAction.sendArenaMessage("EX Freq " + freq + " " 
					+ message + " " + wpptName);
		}
	}

	/**
	 * Notifies the gaining and losing freq wither or not they have
	 * a contested point.
	 *
	 * @param freq is the freq to be notified.
	 * @param wpptName is the name of the warp point gained/lost.
	 * @param obtained determines wither the the notification is
	 * a loss or a gain,
	 */

	private void notifyFreq (Integer freq, String wpptName, boolean obtained)	{
		m_botAction.sendPublicMessage("notifying " + freq);
		Iterator<Player> freqPlayers = m_botAction.getFreqPlayerIterator(freq.intValue());
		if (freqPlayers == null)
			return;

		while (freqPlayers.hasNext())	{
			Player currentp = freqPlayers.next();
			if (currentp != null)
				if (obtained)
					m_botAction.sendPrivateMessage(
							currentp.getPlayerName(),
							"Your freq has control of the flags for "
							+ wpptName + " pm me !warpto " + wpptName + " to go there.");
				else
					m_botAction.sendPrivateMessage(
							currentp.getPlayerName(),
							"Your freq has lost control of the flags for "
							+ wpptName + "!");
		}

	}

	/**
     * Warps a player within a radius of the warp point coord.
     *
     * @param playerName is the player to be warped.
     * @param wppt is the warp point name.
     * @author Cpt.Guano!
     * @modifications Ayano
     */
    private void WarpPlayer(String playerName, String wppt) {
    	Warp_Point dest = points.get(wppt);
        int radius = dest.m_R;
        int xWarp = -1;
        int yWarp = -1;


        double randRadians = Math.random() * 2 * Math.PI;
        double randRadius = Math.random() * radius;
        xWarp = calcXCoord(dest.m_X, randRadians, randRadius);
        yWarp = calcYCoord(dest.m_Y, randRadians, randRadius);

        m_botAction.warpTo(playerName, xWarp, yWarp);
        m_botAction.sendPrivateMessage(playerName,"You have been sent to " + wppt);
    }


    private int calcXCoord(int xCoord, double randRadians, double randRadius)
    {
        return xCoord + (int) Math.round(randRadius * Math.sin(randRadians));
    }


    private int calcYCoord(int yCoord, double randRadians, double randRadius)
    {
        return yCoord + (int) Math.round(randRadius * Math.cos(randRadians));
    }

    /**
     * Handles all flag captures.
     */

    public void handleEvent(FlagClaimed event)	{
		Integer flagID = new Integer ( event.getFlagID() );
		m_botAction.sendPublicMessage("someone grabed flag: " + flagID);
		int playerID = event.getPlayerID();
		Integer rawFreq = new Integer ( m_botAction.getPlayer(playerID).getFrequency() );

		if (exploring && playerID == explorer)	{
			harvested.add(flagID);
			m_botAction.sendPrivateMessage(explorer,
					"Flag " + flagID + " has been harvested.");
			m_botAction.sendPrivateMessage(explorer,
					"Current group list: " + harvested.toString());
		}
		else {
			if ( points.isEmpty() )
				return;
			if ( !freqs.containsKey(rawFreq) )	{
				m_botAction.sendPublicMessage("adding " + rawFreq + " with " + flagID);
				Holding_Freq newFreq = new Holding_Freq(rawFreq);
				freqs.put(rawFreq, newFreq);
			}
			m_botAction.sendPublicMessage("checking " + rawFreq + " for " + flagID);
			CheckFlag(flagID,rawFreq.intValue());
		}
    }

    /**
     * handles all messages.
     */

    public void handleEvent( Message event ) {
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name ) ) {
                handleCommand( name, message.toLowerCase() );
            }
            else
            	handlePlayerCommand( name, message.toLowerCase() );
        }
    }

    /**
     * Handles all ER+ commands.
     *
     * @param sender is the user of the bot.
     * @param message is the command.
     */

    public void handleCommand( String sender, String message ) {
		if( message.startsWith( "!explore" ))
            doExplore(sender);
        else if( message.startsWith( "!setgroup" ))
        	doSetGroup(sender);
        else if( message.startsWith( "!listgroups" ))
        	doListGroups(sender);
        else if( message.startsWith( "!cleargroups" ))
        	doClearGroups(sender);
        else if( message.startsWith( "!addgrouppoint " ))
        	doAddGroupPoint(sender,message.substring(15));
        else if( message.startsWith( "!addpoint " ))
        	doAddPoint(sender,message.substring(10));
        else if( message.startsWith( "!claimedmsg " ))
        	doClaimedMsg(sender,message.substring(12));
        else if( message.startsWith( "!contestedmsg " ))
        	doContestedMsg(sender,message.substring(14));
        else if( message.startsWith( "!listmsg" ))
        	doListMsgs(sender);
        else if( message.startsWith( "!listpoints" ))
        	doListPoints(sender);
        else if( message.startsWith( "!removepoint " ))
        	doRemovePoint(sender,message.substring(13));
        else if( message.startsWith( "!clearpoints" ))
        	doClearPoints(sender);
        else if( message.startsWith( "!held" ))
        	doHeld(sender);
        else if( message.startsWith( "!releaseflags" ))
        	doReleaseFlags(sender);
        else
        	handlePlayerCommand(sender, message);
    }

    /**
     * Handles player level commands.
     *
     * @param sender
     * @param message
     */

    public void handlePlayerCommand( String sender, String message )	{
		try 	{
			Player player = m_botAction.getPlayer(sender);
			if (player == null)
				return;
			if (message.startsWith("!warpto "));	{
				Integer freq = new Integer (player.getFrequency());
				if (freqs.containsKey(freq))	{
					String arg = message.substring(8);
					if (freqs.get(freq).containsWrpp(arg))	{
						WarpPlayer(sender, arg);
					}
				}
			}
		}catch (Exception e) {}
    }

	/**
	 * Returns messages.
	 */

    public String[] getHelpMessages() {
		String help[] = {
            "=FLAGWARPPT=======================================================================FLAGWPPT=",
			"!Explore            - Turns on/off 'explorer' mode where you can manually set flag",
			"                      groups by claiming them, then using !setgroup .",
			"!SetGroup           - Used for manual flag setting, if a flag is claimed in explore",
			"                      mode, it will be added to a list which is then grouped when this",
			"                      command is used; it will then print a number for group reference.",
			"                      The list will then be cleared for a new group list; note that a flag",
			"                      can be registered on multiple groups for various combinations.",
			"!ListGroups         - Lists all set group numbers and tied IDs, it's usless if you don't.",
			"                      remember where the IDs were.",
			"!ClearGroups        - Clears all saved groups.",
			"!AddGroupPoint <group#>,<name>,<destx>:<desty>:<radius>",
			"                    - For manual setting of flag groups-to-points.",
			"!AddPoint <id1>:<id2>:<ect..>,<name>,<destx>:<desty>:<radius>",
			"                    - For already known flagIDs, sets a warppoint to be activated.",
			"!ListPoints         - Lists all set warppts.",
			"!RemovePoint        - Removes a point.",
			"!ClearPoints        - Clears all set points.",
			"!ClaimedMsg <msg>   - Adds an arena message for claimed warp points. \"~*\" Erases message",
			"!ContestedMsg <msg> - Adds an arena message for contested warp points. \"~*\" Erases message",
			"!ListMsg            - Lists current claimed and contested messages if any.",
			"!Held               - Lists who's currently holding each point; -1 if no freq holds it.",
			"!ReleaseFlags       - Releases flag ownership from all freqs.",
			"=FLAGWARPPT=======================================================================FLAGWPPT="
		};
		return help;
	}

    /**
     * Holds freq specific data.
     * @author Ayano
     */

	private class Holding_Freq	{
		public Integer m_Freq;
		public ArrayList<Integer> m_Flags;
		public ArrayList<String> m_Wrpp;

		public Holding_Freq (Integer freq)	{
			m_Freq = freq;
			m_Wrpp = new ArrayList<String>();
			m_Flags = new ArrayList<Integer>();
		}

		public void addFlag (Integer flag)
		{m_Flags.add(flag);}
		public void removeFlag (Integer flag)
		{m_Flags.remove(flag);}
		public void clearFlags ()
		{m_Flags.clear();}
		public void addWrpp (String wrpp)
		{m_Wrpp.add(wrpp);}
		public void removeWrpp (String wrpp)
		{m_Wrpp.remove(wrpp);}
		public void clearWrpp ()
		{m_Wrpp.clear();}
		public boolean containsFlag (Integer flag)
		{return m_Flags.contains(flag);}
		public boolean containsWrpp (String wrpp)
		{return m_Wrpp.contains(wrpp);}
	}

	/**
	 * Handles all warp point specific data
	 * @author Ayano
	 */

	private class Warp_Point {
		public Integer m_Holder = -1;
		public ArrayList<Integer> m_Ids;
		public String m_Name;
		public int m_X,m_Y,m_R;
		
		public Warp_Point(ArrayList<Integer> ids, 
				String name, int xcoord, int ycoord, int radius)	{
			m_Ids = ids;
			m_Name = name;
			m_X = xcoord;
			m_Y = ycoord;
			m_R = radius;
		}

		public void setHolder(Integer holder)
		{m_Holder = holder;}
		public Integer getHolder()
		{return m_Holder;}
		public boolean containsFlag (Integer flag)
		{return m_Ids.contains(flag);}

		public boolean hasFlags(ArrayList<Integer> flags)	{
			Iterator<Integer> required = m_Ids.iterator();

			while (required.hasNext())	{
				if (!flags.contains((Integer)required.next()))
					return false;
			}
			return true;
		}
		public String toString ()	{
			return (m_Name + " Flags: " + m_Ids.toString()
					+ " X:" + m_X + " Y:" + m_Y + " Radius:" + m_R);
		}
	}

}
