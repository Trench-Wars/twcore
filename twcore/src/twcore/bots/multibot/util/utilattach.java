package twcore.bots.multibot.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.StringTokenizer;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.TurretEvent;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/**
 * A utility intended for turret sensitive events where only certain
 * ships can attach, and those could be limited or unlimited.
 * eg: a terrier has a limit of 1 lance, 2 javelins, 1 warbird, and
 * spiders are unlimited.
 * 
 * @author Ayano
 *
 */

public class utilattach extends MultiUtil {
	
	private static final byte MAX_SHIP = 8;
	private static final byte MIN_SHIP = 1; 
	
	private Random randgen = new Random();
	private HashMap<Integer,HashMap<Integer,Integer>> atchRules;
	
	/**
	 * Initializes global variables.
	 */
	
	public void init(){
		atchRules= new HashMap<Integer,HashMap<Integer,Integer>>();
	}
	
	/**
	 * Requests events.
	 */
	
	public void requestEvents( ModuleEventRequester events )	{
		events.request(this, EventRequester.TURRET_EVENT);
	}
	
	/**
	 * Adds an ship to the limited attach list.
	 * Syntax <Anchor>:<ship1>,<ship2>,<ship3>, ...
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is the argument string to be spliced.
	 */
	
	public void doAddAttachLimit( String sender, String argString )	{
		StringTokenizer args = getArgTokens(argString,":");
		int anchor;
		
		if (args.countTokens() < 2)
			throw new IllegalArgumentException("Invalid syntax, use " +
					"<Anchor>:<ship1>,<ship2>,<ship3>, ...");
		
		anchor = Integer.parseInt(args.nextToken());
		if (!isValidShip(anchor))
			throw new IllegalArgumentException("Invalid ship listed; " +
			"Valid ships are 1-8");
		
		StringTokenizer shpArgs = getArgTokens(args.nextToken(),",");
		int[] allowedShps = new int[shpArgs.countTokens()];
		
		for (int i = 0 ; i < allowedShps.length ; i++)	{
			int current = Integer.parseInt(shpArgs.nextToken());
			if (isValidShip(current))
				allowedShps[i] = current;
			else
				throw new IllegalArgumentException("Invalid ship listed; " +
						"Valid ships are 1-8");
		}
		
		HashMap<Integer,Integer> newRule = new HashMap<Integer,Integer>();
		for (int i = 0 ; i < allowedShps.length ; i++)	{
			newRule.put(new Integer (allowedShps[i]), new Integer(-1));
		}
		
		Integer anchorI = new Integer(anchor);
		if (atchRules.keySet().contains(anchorI))	{
			atchRules.remove(anchorI);
		}
		
		atchRules.put(anchorI, newRule);
		m_botAction.sendPrivateMessage(sender, "Only ship types: " 
				+ newRule.keySet().toString() + " can attach to ship type: " + anchor
				+ " with unlimted turrets ");
		
		
	}
	
	/**
	 * Adds a turret limit allowing only a certain number of ships
	 * of specific type to be turrets on specific ship type.
	 * Syntax <Anchor>:<Turret>:<number>
	 * 
	 * @param sender is the user of the bot.
	 * @param argString is the argument string to be spliced.
	 */
	
	public void doAddTurretLimit( String sender, String argString) {
		StringTokenizer args = getArgTokens(argString,":");
		if (args.countTokens() != 3)
			throw new IllegalArgumentException("Invalid syntax, use " +
					"<Anchor>:<ship>:<limit>");
		
		Integer anchor = new Integer (Integer.parseInt(args.nextToken()));
		Integer ship = new Integer (Integer.parseInt(args.nextToken()));
		Integer limit = new Integer (Integer.parseInt(args.nextToken()));
		
		if (!atchRules.keySet().contains(anchor))
			throw new IllegalArgumentException("Anchor has no attach limits!");
		else if (!atchRules.get(anchor).keySet().contains(ship))
			throw new IllegalArgumentException("Ship is not allowed to attach!");
		else if ( limit.intValue() == 0 || limit.intValue() < -1)
			throw new IllegalArgumentException("only Values -1 (unlimited)," 
					+ " and 1-8 are permited");
		
		atchRules.get(anchor).remove(ship);
		atchRules.get(anchor).put(ship, limit);
		m_botAction.sendPrivateMessage(sender, "A limit of " + 
				(limit ==-1 ? "unlimited" : limit) + " turrets of ship type " 
				+ ship + " is now permitted on anchor of ship type " + anchor);
		
	}
	
	public void doClearAttachLimits(String sender)	{
		if (atchRules.isEmpty())	{
			m_botAction.sendPrivateMessage(sender, "There are no limits to clear!");
			return;
		}
		atchRules.clear();
		m_botAction.sendPrivateMessage(sender, "Attach limits have been cleared.");
	}
	
	/**
	 * Shows the all attach limits.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doShowLimits ( String sender )	{
		
		if (atchRules.isEmpty())	{
			m_botAction.sendPrivateMessage(sender, "There are no attach limits.");
			return;
		}
		Iterator<Integer> anchors = atchRules.keySet().iterator();
		Iterator<HashMap<Integer,Integer>> hashShps = atchRules.values().iterator();
		
		while (anchors.hasNext())	{
			HashMap<Integer,Integer> currentHash = hashShps.next();
			Iterator<Integer> ships = currentHash.keySet().iterator();
			Iterator<Integer> limits = currentHash.values().iterator();
			
			StringBuilder hashMsg = new StringBuilder("");
			
			while (ships.hasNext())	{
				int tmpLim = limits.next().intValue();
				hashMsg.append("(" + ships.next() + ": limit:" 
						+ (tmpLim ==-1 ? "unlimited" : tmpLim ) + ")");
			}
			
			m_botAction.sendPrivateMessage(sender, "Anchor: " 
					+ anchors.next().toString() 
					+ "  [|  Allowed ships:  " + hashMsg.toString() + "  |]  ");
		}
		
	}
	
	/**
	 * Handle's all message events.
	 */
	
	public void handleEvent(Message event)	{
		String playerName = m_botAction.getPlayerName(event.getPlayerID());
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
			if(m_opList.isER(playerName))
		        handleCommand(playerName, (event.getMessage()).toLowerCase());
	  }
	
	/**
	 * Handle's all ER level commands.
	 * 
	 * @param sender is the user of the bot.
	 * @param message is the command being issued.
	 */
	
	public void handleCommand( String sender, String message ) {
		try {
			if( message.startsWith( "!addattachlim" ))
	            doAddAttachLimit(sender, message.substring(14));
	        else if( message.startsWith( "!addturretlim" ))
	        	doAddTurretLimit(sender, message.substring(14));
	        else if ( message.startsWith("!attachclearlim"))
	        	doClearAttachLimits(sender);
	        else if( message.startsWith( "!attachlim" ))
	        	doShowLimits(sender);
		}	catch ( NumberFormatException nfe )	{
			m_botAction.sendPrivateMessage(sender, "Syntax must be numerical!");
		}	catch ( IllegalArgumentException iae )	{
			m_botAction.sendPrivateMessage(sender, iae.getMessage());
			}	catch ( Exception e )	{
				m_botAction.sendChatMessage("Uh-oh, error! contact bot dev " +
						"for the 'attach' util.");
				}

    }
	
	/**
	 * Handles incoming turret events. If the anchor is monitored, check it for
	 * further details.
	 */
	
	public void handleEvent(TurretEvent event)	{
		if (!event.isAttaching())
			return;
		if (atchRules.isEmpty())
			return;
		Player attacher = m_botAction.getPlayer(event.getAttacherID());
		Player attachee = m_botAction.getPlayer(event.getAttacheeID());
		if (attacher == null || attachee == null)
			return;
		
		if (atchRules.containsKey(new Integer(attachee.getShipType())))
			checkShip(attachee,attacher);
	}
	
	/**
	 * Checks if the ship is valid 1-8
	 * 
	 * @param ship is the ship in question.
	 * @return true if valid, false otherwise.
	 */
	
	private boolean isValidShip(int ship)	{
		if (ship >= MIN_SHIP && ship <= MAX_SHIP)
			return true;
		return false;
	}
	
	/**
	 * Checks the attacher and the attachee to see if they have  an infinite
	 * limit, where it passes.If it has a limit, check it, else kick the
	 * player off the turret.
	 * 
	 * @param attachee is the Anchor in question.
	 * @param attacher is the Turret in question.
	 */
	
	private void checkShip(Player attachee, Player attacher)	{
		Integer anchor = new Integer(attachee.getShipType());
		Integer turret = new Integer(attacher.getShipType());
		String msg = "Your ship : " + turret + " cannot attach to ship : " + anchor;
		
		if (atchRules.get(anchor).containsKey(turret)
				&& atchRules.get(anchor).get(turret) == -1)
			return;
		else if (atchRules.get(anchor).containsKey(turret))	{
			int lim = atchRules.get(anchor).get(turret).intValue();
			
			if (!hitLimit(attachee.getTurrets(),attacher.getShipType(),lim))
				return;
			else
				msg = "The turret limit for your ship type : " + turret 
				+ " has been reached for the ship you're tring to attach to "
				+ "(ship : " + anchor + " )";
		}
		
		short freq = attacher.getFrequency();
		short atchID = attacher.getPlayerID();
		m_botAction.sendPrivateMessage(attacher.getPlayerID(), msg);
		m_botAction.setFreq(atchID, randgen.nextInt(998));
		m_botAction.setFreq(atchID, freq);
	}
	
	/**
	 * Checks if the limit for the given ship type requesting to turret on
	 * anchor ship of type x has been reached.
	 * 
	 * @param list is a linked list of integers (playerID)s.
	 * @param ship is the attacher's ship type.
	 * @param limit is the limit the anchor ship can hold.
	 * @return true if limit is reached, false otherwise.
	 */
	
	private boolean hitLimit(LinkedList<Integer> list, byte ship, int limit)	{
		Iterator<Integer> iter = list.iterator();
		int attached = 0;
		
		while(iter.hasNext())	{
			if (m_botAction.getPlayer(iter.next().intValue()).getShipType() == ship)
				attached++;
		}
		if (attached-1 >= limit)
			return true;
		return false;
		
	}
	
	/**
     * Gets the argument tokens from a string.  If there are no colons in the
     * string then the delimeter will default to space.
     *
     * @param string is the string to tokenize.
     * @param token is the token to use.
     * @return a tokenizer separating the arguments is returned.
     */

	private StringTokenizer getArgTokens(String string, String token)	{
	    if(string.indexOf(token) != -1)
	      return new StringTokenizer(string, token);
	    return new StringTokenizer(string);
	  }
	
	/**
	 * Return's the bot's help messages.
	 */
	
	public String[] getHelpMessages()	{
	      String[] message = {
	    	  "=ATTACH========================================================================ATTACH=",
	    	  "!AddAttachlim <anchor>:<ship1>,<ship2>.. -- Adds 'permitted' ships to attach to the",
	    	  "                                         -- 'anchor' ship.",
	    	  "!AddTurretlim <anchor>:<ship>:<limit>    -- Sets a limit for attached ships of a",
	    	  "                                         -- certian type to attach to the 'anchor' ship",
	    	  "!AttachClearlim                          -- Removes all attach limits.",
	    	  "!Attachlim                               -- Shows current attach limits.",
	          "=ATTACH========================================================================ATTACH="
	      };
	      return message;
	  }

}
