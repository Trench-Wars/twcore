package twcore.bots.multibot.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.FlagClaimed;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.lvz.Objset;
import twcore.core.util.ModuleEventRequester;

/**
 * An advanced utility that creates warp points from
 * groups of flags. Use of this utility is intended for
 * specialist events or trained/seasoned ERs.
 *
 * @author Ayano
 *
 */

public class utilflagconquest extends MultiUtil {
	
	
	private static final int 					SWITCH_TIME = 200;
	private static final int 					REPEAT_TIME = SWITCH_TIME*5;
	private static final int					MINUTE = 60000;
	private static final int 					STIMULATION_TIME = MINUTE/2;
	
	private ArrayList<Integer> 					harvested;
	private Vector<Hot_Spot> 					spotList;
	private HashMap<Integer,ArrayList<Integer>> groups;
	private HashMap<Integer,Holding_Freq> 		freqs;
	private HashMap<String,Warp_Point> 			points;
	private HashMap<Integer,Long> 				recentContacts;
	
	private Objset								m_Objset;
	private Hot_Spot 							spot;
	private TimerTask 							checkSpot,
												stimulant;
	private GameTask							gameTimer;
	private Integer 							gdex;
	
	
	private int 								explorer,
												gameTime = -1;
	private boolean 							exploring,
												spotsOnly,
												lvzQuiet,
												watching = false;
	
	private String 								claimedmsg = null,
												contestedmsg = null;


	/**
	 * Initializes variables.
	 */

	public void init()	{
		harvested = new ArrayList<Integer>();
		spotList = new Vector<Hot_Spot>();
		groups = new HashMap<Integer,ArrayList<Integer>>();
		freqs = new HashMap<Integer,Holding_Freq>();
		points = new HashMap<String,Warp_Point>();
		recentContacts = new HashMap<Integer,Long>();
		m_Objset = m_botAction.getObjectSet();
		gdex = 1;
		m_botAction.setPlayerPositionUpdating(0);
		m_botAction.stopSpectatingPlayer();
	}

	/**
	 * Requests events.
	 */

	public void requestEvents( ModuleEventRequester modEventReq ) {
		modEventReq.request(this, EventRequester.FLAG_CLAIMED );
		modEventReq.request(this, EventRequester.PLAYER_POSITION);
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
		sortList(groups,sender);
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
		} else if (watching)	{
			m_botAction.sendPrivateMessage(sender,
			"cannot conduct while watching");
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
		if (argTokens.hasMoreTokens())
			while (argTokens.hasMoreTokens())
				addHotSpot(newPoint,argTokens.nextToken());
		
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
							" <group#>,<name>,<destx>:<desty>:<radius>" +
							",<x1>:<y1>:<r1>,<x2>:<y2>:<r2>,(ect..)");
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
		} else if (watching)	{
			m_botAction.sendPrivateMessage(sender,
			"cannot conduct while watching");
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
		if (argTokens.hasMoreTokens())
			while (argTokens.hasMoreTokens())
				addHotSpot(newPoint,argTokens.nextToken());
		
		points.put(wpptName, newPoint);
		m_botAction.sendPrivateMessage(sender, "Point added: " + newPoint.toString());

		}
		catch (Exception e)	{
			if (e.getMessage() != null)
				m_botAction.sendPrivateMessage(sender,e.getMessage());
			else
			m_botAction.sendPrivateMessage(
					sender, "Improper syntax, please use !AddPoint" +
							" <id1>:<id2>:<ect..>,<name>,<destx>:<desty>:<radius>" +
							",<x1>:<y1>:<r1>,<x2>:<y2>:<r2>,(ect..)");
		}

	}
	
	public void doAddLvz (String sender, String argString)	{
		StringTokenizer args = getArgTokens(argString,",");
		
		try	{
			String pointName = args.nextToken();
			int claim = Integer.parseInt(args.nextToken()),
				lost = Integer.parseInt(args.nextToken()),
				contest = Integer.parseInt(args.nextToken());
			
			points.get(pointName).addObj(claim, lost, contest);
			m_botAction.sendPrivateMessage(sender, "For point: " + pointName +
					" claimed lvz is: " + claim + " lost lvz is: " + lost + 
					" contested lvz is: " + contest);
		} catch (Exception e)	{
			m_botAction.sendPrivateMessage(sender, "Improper syntax, pleast use: " +
					"!AddLvz <PointName>,<claimedLVZ#>,<lostLVZ#>,<contestedLVZ#>");
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
		sortList(points,sender);
		}
		else
			m_botAction.sendPrivateMessage(sender, "There are no points to list.");
	}
	
	/**
	 * Clears all points and cleans the freqs of the point if any possess it.
	 * 
	 * @param sender is the user of the bot.
	 */

	public void doClearPoints (String sender)	{
		if (points.isEmpty())
			m_botAction.sendPrivateMessage(sender,"No points to remove.");
		else if (watching)	{
			m_botAction.sendPrivateMessage(sender,
			"cannot conduct while watching");
		} else	{
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
		else if (watching)	{
			m_botAction.sendPrivateMessage(sender,
			"cannot conduct while watching");
		} else	{
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
		Iterator<Integer>holdings = freqs.keySet().iterator();
		while (holdings.hasNext())
			m_Objset.hideAllFreqObjects(holdings.next().intValue());
		freqs.clear();
		
		Iterator<Warp_Point>wrppts = points.values().iterator();
		while (wrppts.hasNext())	{
			Warp_Point tmpwppt = wrppts.next();
			tmpwppt.m_Holder = -1;
			if (tmpwppt.hasLvz)	{
				m_botAction.hideObject(tmpwppt.m_ObjClaim);
				m_botAction.hideObject(tmpwppt.m_ObjLost);
				m_botAction.hideObject(tmpwppt.m_ObjContest);
			}
		}
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
	
	/**
	 * Lists the claimed and contested messages if any.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doListMsgs( String sender )	{
		m_botAction.sendPrivateMessage(sender, "Warp point gained message is: " 
				+ (claimedmsg == null ? "none" : claimedmsg));
		m_botAction.sendPrivateMessage(sender, "Warp point contested message is: " 
				+ (contestedmsg == null ? "none" : contestedmsg));
	}
	
	/**
	 * initiates and deactivates the watching of hotspots.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	
	public void doWatch( String sender )	{
		if (watching) {
            m_botAction.sendPrivateMessage(sender, "Watching deactivated");
            checkSpot.cancel();
            stimulant.cancel();
            watching = !watching;
        }else if (points.isEmpty())	{
        	m_botAction.sendPrivateMessage(sender, "No warp points to watch");
        }else	{
        	spotList.clear();
        	for (Warp_Point warppt : points.values())
        		if (warppt.hasSpot)
        			for (Hot_Spot hs : warppt.m_Spots)
        				spotList.addElement(hs);
        	
        	if (spotList.isEmpty())	{
        		m_botAction.sendPrivateMessage(sender, "No hotspots to watch");
        		return;
        	}
        		
        	checkSpot = new TimerTask()	{
        		public void run()	{
        			spot = spotList.elementAt(0);
        			spotList.removeElementAt(0);
        			spotList.addElement(spot);
        			m_botAction.moveToTile(spot.m_X, spot.m_Y);
        		}
        	}; stimulant = new TimerTask()	{
        		public void run()	{
        			m_botAction.getShip().sendPositionPacket();
        		}
        	};
        	
        	m_botAction.scheduleTaskAtFixedRate
        	(stimulant, STIMULATION_TIME, STIMULATION_TIME);
            m_botAction.scheduleTaskAtFixedRate
            (checkSpot, SWITCH_TIME, SWITCH_TIME);
            m_botAction.sendPrivateMessage(sender, "Watching activated");
            watching = !watching;
        }
        	
		
	}
	
	/**
	 * Toggles on/off team claim/lost/contested notification messages.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doLvzOnly( String sender )	{
		lvzQuiet = !lvzQuiet;
		
		if (lvzQuiet)
			m_botAction.sendPrivateMessage(sender, 
					"Team claim/lost/contested messages have been disabled.");
		else
			m_botAction.sendPrivateMessage(sender, 
				"Team claim/lost/contested messages have been enabled.");
	}
	
	/**
	 * Toggles on/off manual !warpto commands.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doSpotsOnly( String sender )	{
		spotsOnly = !spotsOnly;
		
		if (spotsOnly)
			m_botAction.sendPrivateMessage(sender, 
					"Manual !warpto has been disabled");
		else
			m_botAction.sendPrivateMessage(sender, 
				"Manual !warpto has been enabled.");
	}
	
	/**
	 * Toggles on/off a timed game for a specifed time
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is the time to be set.
	 */
	
	public void doTimedGame(String sender, String argString)	{
		if (gameTime != -1)	{
			m_botAction.sendArenaMessage("Timed game stopped ", 2);
			gameTime = -1;
			gameTimer.cancel();
			m_botAction.setTimer(0);
			return;
		} else if (argString == null)
			return;
		
		try	{
			int time = Integer.parseInt(argString);
			if (time > 0 && !points.isEmpty())	{
				gameTime = time;
				m_botAction.sendArenaMessage("Timed game set for: " + time + " minutes", 1);
				m_botAction.setTimer(time);
				m_botAction.prizeAll(7);
				doReleaseFlags(sender);
				gameTimer = new GameTask(time);
				m_botAction.scheduleTaskAtFixedRate(gameTimer, SWITCH_TIME, MINUTE);
			} else
				m_botAction.sendPrivateMessage(sender, "Time must be greater than " +
						"zero and or there no points to capture");
			
		} catch (NumberFormatException nfe)	{
			m_botAction.sendPrivateMessage(sender, "Invalid syntax");
		}
		
	}
	
	/**
	 * Helper method that adds a hotspot to a warppoint.
	 * 
	 * @param warppt is the warp point to add a hotspot to.
	 * @param argString is the arguments for a spot.
	 */
	
	private boolean addHotSpot( Warp_Point warppt, String argString )
		throws NumberFormatException 									{
		
		StringTokenizer args = getArgTokens(argString,":");
		int numArgs = args.countTokens();
		
		if (numArgs < 3 || numArgs > 6)
			return false;
		
		int x,y,r;
		x = Integer.parseInt(args.nextToken());
		y = Integer.parseInt(args.nextToken());
		r = Integer.parseInt(args.nextToken());
		if (args.hasMoreTokens())	{
			int dx,dy,dr;
			dx = Integer.parseInt(args.nextToken());
			dy = Integer.parseInt(args.nextToken());
			dr = Integer.parseInt(args.nextToken());
			warppt.addSpot(x, y, r,dx,dy,dr);
		} else
			warppt.addSpot(x, y, r);
			
		return true;
	}

	/**
	 * Sorts a hash map into a readable format to the user.
	 *
	 * @param list is the hash map.
	 * @param sender is the user of the bot.
	 */

	private void sortList(HashMap list, String sender)	{
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

	private void checkFlag(Integer flag, int gainingfreq)	{
		Iterator<Holding_Freq> freqList = freqs.values().iterator();
		if (freqList == null)
			return;

		while (freqList.hasNext())	{
			boolean win = true;
			Holding_Freq currentf = freqList.next();
			if (currentf.m_Freq.intValue() == gainingfreq
					&& !currentf.containsFlag(flag))	{
				currentf.addFlag(flag);
				if (points.isEmpty())
					return;
				Iterator<Warp_Point> warppts = points.values().iterator();
				while (warppts.hasNext())	{
					Warp_Point currentwp = warppts.next();
					checkWppt(currentwp,currentf);
					if (gameTime != -1 && currentwp.m_Holder != currentf.m_Freq)
						win = false;
					}
				if (gameTime != -1 && win)
					declareWinner(currentf.m_Freq,false);
			}
			else if (currentf.containsFlag(flag)
					&& currentf.m_Freq.intValue() != gainingfreq)	{
				currentf.removeFlag(flag);
				if (points.isEmpty())
					return;
				Iterator<Warp_Point> warppts = points.values().iterator();
				while (warppts.hasNext())	{
					Warp_Point currentwp = warppts.next();
					checkWppt(currentwp,currentf);
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

	private void checkWppt(Warp_Point wppt, Holding_Freq freq)	{
			if (wppt.hasFlags(freq.m_Flags)
					&& !freq.containsWrpp(wppt.m_Name))	{
				freq.addWrpp(wppt.m_Name);
				wppt.setHolder(freq.m_Freq);
				warpptMsg(freq.m_Freq,wppt.m_Name,claimedmsg);
				if (wppt.hasLvz)	{
					m_Objset.showFreqObject(freq.m_Freq, wppt.m_ObjClaim);
					m_Objset.hideFreqObject(freq.m_Freq, wppt.m_ObjLost);
				}
				notifyFreq(freq.m_Freq, wppt.m_Name,true);
			}
			else if (!wppt.hasFlags(freq.m_Flags)
					&& freq.containsWrpp(wppt.m_Name))	{
				wppt.setHolder(-1);
				freq.removeWrpp(wppt.m_Name);
				if (wppt.hasLvz)	{
					m_botAction.showObject(wppt.m_ObjContest);
					m_Objset.showFreqObject(freq.m_Freq, wppt.m_ObjLost);
					m_Objset.hideFreqObject(freq.m_Freq, wppt.m_ObjClaim);
				} if ( wppt.m_Ids.size() > 1)	{
						warpptMsg(freq.m_Freq,wppt.m_Name,contestedmsg);
						m_botAction.showObject(wppt.m_ObjContest);
				}
				notifyFreq(freq.m_Freq, wppt.m_Name,false);
			}
	}
	
	/**
	 * Controls arena wide claimed or contested messages and Lvz.
	 * 
	 * @param freq is the freq gaining or contested at a point.
	 * @param wpptName is the point that is contested.
	 * @param message is the message to be issued.
	 */
	
	private void warpptMsg(int freq, String wpptName,String message)	{
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
		} catch (Exception e) 	{}
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
		Iterator<Player> freqPlayers = m_botAction.getFreqPlayerIterator(freq.intValue());
		if (freqPlayers == null)
			return;
		
		Warp_Point warppt = points.get(wpptName);
		if (warppt.hasLvz)	{
			m_botAction.setFreqObjects(freq);
		}

		while (freqPlayers.hasNext())	{
			Player currentp = freqPlayers.next();
			if (currentp != null)	{
				if (obtained)	{
					if (!lvzQuiet)
						m_botAction.sendPrivateMessage(
								currentp.getPlayerName(),
								"Your freq has control of the flags for "
								+ wpptName + (spotsOnly ? "" :
									" pm me !warpto " + wpptName + " to go there."));
					else
					if (!lvzQuiet)
						m_botAction.sendPrivateMessage(
								currentp.getPlayerName(),
								"Your freq has lost control of the flags for "
								+ wpptName + "!");
				}
			}
		}
	}
	
	/**
	 * Declares a winner
	 * **NOTE: There is an issue with draws / tied games.
	 * 
	 * @param freq is the victorious freq.
	 * @param rtime is a wither or not there is remaining time.
	 */
	
	private void declareWinner(int freq, boolean rtime)	{
		if (!rtime)	{
			m_botAction.sendArenaMessage("Freq " + freq + 
					" Has Captured all the flags and won this game!",5);
			gameTimer.cancel();
			m_botAction.setTimer(0);
		} else
			m_botAction.sendArenaMessage("Freq " + freq + 
					" Has Captured the most flags!");
		
		m_botAction.sendArenaMessage("",101);	//Stops music if any
		m_botAction.prizeAll(7);
		gameTime = -1;
		
		try	{
			Thread.sleep(500); 					//Wait for flag checking
		} catch (InterruptedException ie)	{} //to complete
		
		doReleaseFlags("0");
		
		
	}

	/**
     * Warps a player within a radius of the warp point coord.
     * 
     * @param playerName is the name of the player.
     * @param wppt is the name of the warp point.
     */
    private void warpPlayer(String playerName, String wppt) {
    	Warp_Point dest = points.get(wppt);

        m_botAction.warpTo(playerName, dest.m_DX, dest.m_DY, dest.m_DR);
        m_botAction.sendPrivateMessage(playerName,"You have been sent to " + wppt);
    }
    
    /**
     * Warps a player within a radius of the warp point coord.
     * 
     * @param playerName is the name of the player
     * @param wppt is the
     */
    
    private void warpPlayer(String playerName, Hot_Spot hs) {

        m_botAction.warpTo(playerName, hs.m_DX, hs.m_DY, hs.m_DR);
        m_botAction.sendPrivateMessage(playerName,"You have been sent to " + hs.m_Warppt);
    }

    /**
     * Handles all flag captures.
     */

    public void handleEvent(FlagClaimed event)	{
		Integer flagID = new Integer ( event.getFlagID() );
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
				Holding_Freq newFreq = new Holding_Freq(rawFreq);
				freqs.put(rawFreq, newFreq);
			}
			checkFlag(flagID,rawFreq.intValue());
		}
    }
    
    /**
     * Handles position events.
     */
    
    public void handleEvent ( PlayerPosition event )	{
    	if (points.isEmpty())
    		return;
    	int playerID = event.getPlayerID(),
    		xcoord = event.getXLocation(),
    		ycoord = event.getYLocation();
    	
    	if (watching && spot != null && spot.inside(xcoord, ycoord))	{
    		if (recentContacts.containsKey(playerID))
                if ((System.currentTimeMillis() - 
                		recentContacts.get(playerID)) < REPEAT_TIME)
                    		return;
    		
    		Player player = m_botAction.getPlayer(playerID);
    		if (player == null)
    			return;
    		
    		int freq = player.getFrequency();
    		recentContacts.put(playerID, System.currentTimeMillis());
    		if (freqs.containsKey(freq) 
    			&& freqs.get(freq).containsWrpp(spot.m_Warppt))
    				warpPlayer(m_botAction.getPlayerName(playerID), spot);
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
        else if( message.startsWith( "!addlvz " ))
    		doAddLvz(sender,message.substring(8));
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
        else if( message.startsWith( "!watch" ))
        	doWatch(sender);
        else if( message.equals( "!time"))			// Toggle
        	doTimedGame(sender,null);
        else if( message.startsWith( "!time "))		// Toggle
        	doTimedGame(sender, message.substring(6));
        else if( message.startsWith( "!lvzonly" ))
        	doLvzOnly(sender);
        else if( message.startsWith( "!spotsonly" ))
        	doSpotsOnly(sender);
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
     * @param sender is the player's name
     * @param message is the issued command.
     */

    public void handlePlayerCommand( String sender, String message )	{
    	if(spotsOnly)
    		return;
    	Player player = m_botAction.getPlayer(sender);
		if (player == null)
			return;
		
		if (message.startsWith("!warpto "))	{
			Integer freq = new Integer (player.getFrequency());
			if (freqs.containsKey(freq))	{
				String arg = message.substring(8);
				if (freqs.get(freq).containsWrpp(arg))	{
					warpPlayer(sender, arg);
				}
			}
		}
    }

	/**
	 * Returns messages.
	 */

    public String[] getHelpMessages() {
		String help[] = {
            "=FLAG-CONQUEST=======================================================================FLAG-CONQUEST=",
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
			"!AddGroupPoint <group#>,<name>,<destX>:<destY>:<destR>",
			" **OR <group#>,<name>,<destX>:<destY>:<destR>,<X>:<Y>:<R>,<X..>:<Y..>:<R..> [ect..]",
			" **For variable hotspots ,<X>:<Y>:<R>:<dX>:<dY>:<dR>,<X..>:<Y..>:<R..>:<dX..>:<dY..>:<dR..> [ect..]",
			"                    - For manual setting of flag groups-to-points; optional hotspots",
			"!AddPoint <id1>:<id2>:<ect..>,<name>,<destX>:<destY>:<destR>",
			" **OR <id1>:<id2>:<ect..>,<name>,<destX>:<destY>:<destR>,<X>:<Y>:<R>,<X..>:<Y..>:<R..> [ect..]",
			" **For variable hotspots ,<X>:<Y>:<R>:<dX>:<dY>:<dR>,<X..>:<Y..>:<R..>:<dX..>:<dY..>:<dR..> [ect..]",
			"                    - For already known flagIDs; optional hotspots",
			"!AddLvz <PointName>,<claimedLVZ>,<lostLVZ>,<ContestedLVZ>",
			"                    - Adds lvz objects to claimed/lost/contested points.",
			"!ListPoints         - Lists all set points.",
			"!RemovePoint        - Removes a point.",
			"!ClearPoints        - Clears all set points.",
			"!ClaimedMsg <msg>   - Adds an arena message for claimed warp points. \"~*\" Erases message",
			"!ContestedMsg <msg> - Adds an arena message for contested warp points. \"~*\" Erases message",
			"!ListMsg            - Lists current claimed and contested messages if any.",
			"!watch              - Activates/Deactivates watching if any hotspots were defined for a point.",
			"!time <minutes>     - Starts a Timed game, declares winner at limit, or if a team holds all points.",
			"                    - Sending nothing turns off the timer, if it's on.",
			"!LvzOnly            - Toggles team message notification messages for claims/contests/losses.",
			"!SpotsOnly          - Toggles manual !warpto on or off.",
			"!Held               - Lists who's currently holding each point; -1 if no freq holds it.",
			"!ReleaseFlags       - Releases flag ownership from all freqs.",
			"=FLAG-CONQUEST=======================================================================FLAG-CONQUEST="
		};
		return help;
	}
    
    /**
     * Cancels timer tasks
     */
    
    public void cancel()	{
    	m_Objset = new Objset();
    	if (watching)	{
    		checkSpot.cancel();
    		stimulant.cancel();
    	}
    	if (gameTime != -1)
    		gameTimer.cancel();
    }
    
    /**
     * Game timer task used for timed games.
     * 
     * @author Ayano
     *
     */
    
    private class GameTask extends TimerTask	{
    	public int m_Time;
    	
    	public GameTask(int time)	{
    		m_Time = time;
    	}
    	
    	public void run()	{
    		if (m_Time > 3)
    			m_Time--;
    		else if (m_Time == 3)	{
    			m_botAction.sendArenaMessage("3 minutes Remaining!!",100);
    			m_Time--;
    		} else if (m_Time > 0)	{
    			m_botAction.sendArenaMessage("",100);
    			m_Time--;
    		} else	{
    			declareWinner(getWinner(),true);
    			this.cancel();
    		}
    	}
    	
    	public int getWinner()	{		//FIXME: Adjust this with related methods to
    		int freq = -1,			   // handle tied games.
    			numPoints=0;
    		
    		for (Holding_Freq cfreq : freqs.values())	{
    			if (cfreq.m_Wrpp.size() > numPoints)	{
    				numPoints = cfreq.m_Wrpp.size();
    				freq = cfreq.m_Freq;
    			}
    		}
    		return freq;
    	}
    	
    }
    

    /**
     * Holds freq specific data.
     * 
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
	 * Handles all warp point specific data.
	 * 
	 * @author Ayano
	 */

	private class Warp_Point {
		public Integer m_Holder = -1;
		public ArrayList<Integer> m_Ids;
		public ArrayList<Hot_Spot> m_Spots;
		public String m_Name;
		public boolean 	hasSpot,
						hasLvz;
		public int	m_DX,
					m_DY,
					m_DR,
					m_ObjClaim,
					m_ObjLost,
					m_ObjContest;
		
		public Warp_Point(ArrayList<Integer> ids, 
				String name, int xcoord, int ycoord, int radius)	{
			m_Ids = ids;
			m_Name = name;
			m_DX = xcoord;
			m_DY = ycoord;
			m_DR = radius;
			m_Spots = new ArrayList<Hot_Spot>();
		}
		
		public void addSpot(int xcoord, int ycoord, int radius)	{
			hasSpot = true;
			m_Spots.add(new Hot_Spot(xcoord,ycoord,radius,m_DX,m_DY,m_DR,m_Name));
		}
		
		public void addSpot(int xcoord, int ycoord, int radius, 
				int destx, int desty, int destr)	{
			hasSpot = true;
			m_Spots.add(new Hot_Spot(xcoord,ycoord,radius,destx,desty,destr,m_Name));
		}
		
		public void addObj(int claim, int lost, int contest)	{
			hasLvz = true;
			m_ObjClaim = claim;
			m_ObjLost = lost;
			m_ObjContest = contest;
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
			String msg = m_Name + " Flags: " + m_Ids.toString() + 
			" DX:" + m_DX + " DY:" + m_DY + " DR:" + m_DR;
			if (hasSpot)
				for (int i = 0 ; i < m_Spots.size() ; i++)	{
					Hot_Spot curr = m_Spots.get(i);
					msg +=  " <Hotspot X:" + curr.m_X + " Y:" + curr.m_Y + " R:" + 
					curr.m_R + (curr.m_DR != m_DR ? " DX:" + curr.m_DX + " DY:" + 
								curr.m_DY + " DR:" + curr.m_DR : "") + ">";	
				} 
			
			if (hasLvz)
				msg += " Claim:" + m_ObjClaim + " Lost:" + m_ObjLost + 
				" Contest:" + m_ObjContest;
			return (msg);
		}
	}
	
	/**
	 * Mini Hot spot class, holds hot spot specific data.
	 * Added hot spot 'inside'/distance formula from Milosh.
	 * 
	 * @author Ayano, Milosh
	 *
	 */
	
	private class Hot_Spot	{
		public int	m_X,
					m_Y,
					m_R,
					m_DX,
					m_DY,
					m_DR;
		
		public String m_Warppt;
		
		public Hot_Spot(int x, int y, int r, int dx, int dy, int dr, String wrppt)	{
			m_X = x;
			m_Y = y;
			m_R = r;
			m_DX = dx;
			m_DY = dy;
			m_DR = dr;
			m_Warppt = wrppt;
		}
		
		/**
	     * Return true if the given X,Y is touching this spot. Else return false.
	     */
		
		public boolean inside(int playerX, int playerY) {
	        double dist = Math.sqrt(Math.pow(m_X * 16 - playerX, 2) + 
	        		Math.pow(m_Y * 16 - playerY, 2));
	        if (Math.round(dist) <= m_R * 16)
	            return true;
	        else
	            return false;
	    }
	}

}
