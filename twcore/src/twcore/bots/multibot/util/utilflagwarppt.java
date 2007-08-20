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

public class utilflagwarppt extends MultiUtil {
	ArrayList<Integer> harvested;
	Integer gdex;
	HashMap<Integer,ArrayList<Integer>> groups;
	HashMap<Integer,Holding_Freq> freqs;
	HashMap<String,Warp_Point> points;
	int explorer;
	boolean exploring;
	Random generator = new Random();
	
	public utilflagwarppt ()	{
		
	}
	
	public void init()	{
		harvested = new ArrayList<Integer>();
		groups = new HashMap<Integer,ArrayList<Integer>>();
		freqs = new HashMap<Integer,Holding_Freq>();
		points = new HashMap<String,Warp_Point>();
		gdex = 1;
	}
	
	public void requestEvents( ModuleEventRequester modEventReq ) {
		modEventReq.request(this, EventRequester.FLAG_CLAIMED );
    }
	
	public StringTokenizer getArgTokens(String string, String token)	{
	    if(string.indexOf(token) != -1)
	      return new StringTokenizer(string, token);
	    return new StringTokenizer(string);
	  }
	
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
	
	public void doSetGroup(String sender)	{
		if (harvested.isEmpty())	{
			m_botAction.sendPrivateMessage(sender, 
					"No flags have been harvested");
			return;
		}
		groups.put(gdex, (ArrayList<Integer>)harvested.clone());
		
		m_botAction.sendPrivateMessage(sender, "Flags " 
				+ harvested.toString() + " are now group " + gdex);
		
		harvested.clear();
		gdex++;
	}
	
	public void doListGroups (String sender)	{
		if (groups.isEmpty())
			m_botAction.sendPrivateMessage(sender,"No groups are left, " +
												"or there are no groups to list");
		else	{
		m_botAction.sendPrivateMessage(sender, "Current groups:");
		ListSort(groups,sender);
		}
	}
	
	public void doClearGroups (String sender)	{
		if (groups.isEmpty())
			m_botAction.sendPrivateMessage(sender,"No groups to, clear.");
		else	{
			m_botAction.sendPrivateMessage(sender, "Groups cleared.");
			groups.clear();
		}
	}
	
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
	
	public void doHeld(String sender)	{
		if (points.isEmpty())
			m_botAction.sendPrivateMessage(sender, "There are no points to be held.");
		else	{
			Iterator<Warp_Point> wppts = points.values().iterator();
			while (wppts.hasNext())	{
				Warp_Point currentwp = wppts.next();
				m_botAction.sendPrivateMessage(sender,
						currentwp.m_Name + " is currently held by " + currentwp.m_Holder);
			}
		}
	}
	
	public void doReleaseFlags (String sender)	{
		Iterator<Integer> flags = m_botAction.getFlagIDIterator();
		int freq = new Random().nextInt( 9998 );
		if (flags == null)	{
			m_botAction.sendPrivateMessage(sender, "There are no flags on this map.");
			return;
		}
		
		m_botAction.getShip().setShip(0);
		m_botAction.getShip().setFreq(freq);
		while (flags.hasNext())	{
			Integer flag = flags.next();
			m_botAction.grabFlag(flag);
		}
		
		m_botAction.getShip().setShip(8);
		m_botAction.sendPrivateMessage(sender, "Flags released from all ownership.");
	}
	
	private void ListSort(HashMap list, String sender)	{
		Iterator key = list.keySet().iterator();
		Iterator value = list.values().iterator();
		
		while (key.hasNext())	{
			m_botAction.sendPrivateMessage(sender,key.next().toString() + " -[|   " + value.next().toString() + "   |]");
		}
	}
	
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
	
	private void CheckWppt(Warp_Point wppt, Holding_Freq freq)	{
			if (wppt.hasFlags(freq.m_Flags) 
					&& !freq.containsWrpp(wppt.m_Name))	{
				freq.addWrpp(wppt.m_Name);
				wppt.setHolder(freq.m_Freq);
				notifyFreq(freq.m_Freq, wppt.m_Name,true);
			}
			else if (!wppt.hasFlags(freq.m_Flags) 
					&& freq.containsWrpp(wppt.m_Name))	{
				freq.removeWrpp(wppt.m_Name);
				notifyFreq(freq.m_Freq, wppt.m_Name,false);
			}
	}
	
	private void notifyFreq (Integer freq, String wppt, boolean obtained)	{
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
							+ wppt + " pm me !goto " + wppt + " to go there.");
				else
					m_botAction.sendPrivateMessage(
							currentp.getPlayerName(),
							"Your freq has lost control of the flags for " 
							+ wppt + "!");
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
    
    public void handleEvent(FlagClaimed event)	{
		Integer flagID = new Integer ( event.getFlagID() );
		int playerID = event.getPlayerID();
		Integer rawFreq = new Integer ( m_botAction.getPlayer(playerID).getFrequency() );
		
		if (exploring && playerID == explorer)	{
			harvested.add(flagID);
			m_botAction.sendPrivateMessage(explorer, "Flag " + flagID + 
					" has been harvested.");
			m_botAction.sendPublicMessage(harvested.toString());
		}
		else {
			if ( points.isEmpty() )
				return;
			if ( !freqs.containsKey(rawFreq) )	{
				Holding_Freq newFreq = new Holding_Freq(rawFreq,flagID);
				freqs.put(rawFreq, newFreq);
			}
			CheckFlag(flagID,rawFreq.intValue());
		}
    }
    
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
    
    public void handlePlayerCommand( String sender, String message )	{
		try 	{
		Player player = m_botAction.getPlayer(sender);
		if (player == null)
			return;
		Integer freq = new Integer (player.getFrequency());
		if (freqs.containsKey(freq))	{
			if (freqs.get(freq).containsWrpp(message.substring(6)))	{
				WarpPlayer(sender, message.substring(6));
			}
		}
		}
		catch (Exception e) {m_botAction.sendPublicMessage("bad!");}
    }
	
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
			"!Held               - Lists who's currently holding each point; -1 if no freq holds it.",
			"!ReleaseFlags       - Releases flag ownership from all freqs.",
			"=FLAGWARPPT=======================================================================FLAGWPPT="
		};
		return help;
	}
	
	private class Holding_Freq	{
		Integer m_Freq;
		ArrayList<Integer> m_Flags;
		ArrayList<String> m_Wrpp;
		
		public Holding_Freq (Integer freq)	{
			m_Freq = freq;
			m_Wrpp = new ArrayList<String>();
			m_Flags = new ArrayList<Integer>();
		}
		
		public Holding_Freq (Integer freq, Integer flag)	{
			m_Freq = freq;
			m_Wrpp = new ArrayList<String>();
			m_Flags = new ArrayList<Integer>();
			addFlag(flag);
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
	
	private class Warp_Point {
		Integer m_Holder = -1;
		ArrayList<Integer> m_Ids;
		String m_Name;
		int m_X,m_Y,m_R;
		public Warp_Point(ArrayList<Integer> ids, String name, int xcoord, int ycoord, int radius)	{
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
