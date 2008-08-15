package twcore.bots.multibot.util;

import java.util.StringTokenizer;

import twcore.bots.MultiUtil;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.game.Player;

/**
 * A rather simple ship changing utility. Why
 * this didn't exist beforehand is beyond me.
 * 
 * @author Ayano
 *
 */

public class utilshipswitch extends MultiUtil {
	
	private static final byte MAX_SHIP = 8;
	private static final byte MIN_SHIP = 1;
	
	private boolean S_witch = false;
	private int[] disabled;
	
	
	
	/**
	 * Initializes global variables;
	 */
	
	public void init()	{
		disabled = new int[MAX_SHIP];
	}
	
	/**
	 * Requests events.
	 */
	
	public void requestEvents( ModuleEventRequester events )	{
	}
	
	/**
	 * Turns on ship switches.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doSwitchOn( String sender)	{
		if (S_witch)
			throw new IllegalArgumentException("Already allowing ship changes");
		S_witch = !S_witch;
		m_botAction.sendPrivateMessage(sender, "ON, Allowing ship changes");
		m_botAction.sendArenaMessage("Ship changes enabled: Pm " + 
				m_botAction.getBotName() + " the ship number to change ships.", 1);
	}
	
	/**
	 * Turns off ship switches.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doSwitchOff(String sender)	{
		if (!S_witch)
			throw new IllegalArgumentException("Ship changes are already off");
		S_witch = !S_witch;
		m_botAction.sendPrivateMessage(sender, "OFF, Discontinuing ship changes");
		m_botAction.sendArenaMessage("Ship changes disabled", 1);
	}
	
	/**
	 * Adds a restriction to ship switches.
	 * 
	 * @param sender is the user of the bot.
	 * @param argString contains the argument to be computed.
	 */
	
	public void doDisableShips(String sender, String argString)	{
		StringTokenizer args = getArgTokens(argString,",");
		
		int[] ships = new int[MAX_SHIP];
		
		while (args.hasMoreTokens())	{
			int type = Integer.parseInt(args.nextToken());
			if ( isValidShip(type) )
				ships[type-1]=1;
			else
				throw new IllegalArgumentException("Please enter only valid ship #s");
		}
		
		disabled = ships.clone();
		
		m_botAction.sendPrivateMessage(sender, "Ships: " 
				+ argString+ " are no longer options");
	}
	
	/**
	 * Clears all restrictions.
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doClearDisabled(String sender)	{
		disabled = new int[MAX_SHIP];
		m_botAction.sendPrivateMessage(sender, "All ships enabled.");
	}
	
	/**
	 * Lists disabled ships
	 * 
	 * @param sender is the user of the bot.
	 */
	
	public void doListDisabled(String sender)	{
		StringBuilder msg = new StringBuilder("");
		
		for (int i = 0 ; i < MAX_SHIP ; i ++)
			if (disabled[i] == 1)
				msg.append(i+1 + ", ");
		m_botAction.sendPrivateMessage(sender, "Ships currenly disabled: " + msg);
	}
	
	/**
	 * Handles ER level commands.
	 * 
	 * @param sender is the user of the bot.
	 * @param message is the command to be issued
	 */
	
	public void handleCommand( String sender, String message ) {
		try {
			if( message.startsWith( "!switchon" ))
	            doSwitchOn(sender);
	        else if( message.startsWith( "!switchoff" ))
	        	doSwitchOff(sender);
	        else if ( message.startsWith("!disableships"))
	        	doDisableShips(sender, message.substring(14));
	        else if ( message.startsWith( "!cleardisabled"))
	        	doClearDisabled(sender);
	        else if ( message.startsWith("!listdisabled"))
				doListDisabled(sender);
	        else
	        	handlePlayerCommand(sender, message);
		}	catch ( NumberFormatException nfe )	{
			m_botAction.sendPrivateMessage(sender, "Syntax must be numerical!");
		}	catch ( IllegalArgumentException iae )	{
			m_botAction.sendPrivateMessage(sender, iae.getMessage());
			}	catch ( Exception e )	{
				m_botAction.sendChatMessage("Invalid syntax.");
				}

    }
	
	/**
	 * Handles player commands
	 */
	
	public void handlePlayerCommand(String sender, String message)	{
		if (!S_witch)
			return;
		
		Player plyr = m_botAction.getPlayer(sender);
		
		if (plyr == null)
			return;
		
		int shiptype = plyr.getShipType();
		
		if (shiptype == 0)
			return;
		
		try	{
			int request = Integer.parseInt(message);
			if ( isValidShip(request) ) {
				if ( disabled[request-1] == 0 )	{
					m_botAction.setShip(sender, request);
					m_botAction.sendPrivateMessage(sender, "Changing your ship" +
							" from " + shiptype + " to " + request);
				}	else
						m_botAction.sendPrivateMessage(sender, "Sorry, but that " +
								"ship has been disabled.");
			}
		}	catch (Exception e)	{}
		
	}
	
	/**
	 * Handles all message events.
	 */
	
	public void handleEvent(Message event)	{
		String playerName = m_botAction.getPlayerName(event.getPlayerID());
		String message = event.getMessage().toLowerCase();
		
		if(event.getMessageType() != Message.PRIVATE_MESSAGE)
			return;
		if(m_opList.isER(playerName))
			handleCommand(playerName, message);
		else
			handlePlayerCommand(playerName, message);
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
	 * Returns help messages
	 */
	
	public String[] getHelpMessages()	{
	      String[] message = {
	    	  "=SHIP-SWITCH============================================================SHIP-SWITCH=",
	    	  "---------This is for use in locked arenas to allow players to change ships ---------",
	    	  "!SwitchOn                         -- Turns on player ship switching through bot.",
	    	  "!SwitchOff                        -- Turns off player ship switching through bot.",
	    	  "!DisableShips <ship1>,<ship2>...  -- Prevents ship switches into these ships.",
	    	  "!ClearDisabled                    -- Allows all ships to switched.",
	    	  "!ListDisabled                     -- Lists the currently disabled ships.",
	          "=SHIP-SWITCH============================================================SHIP-SWITCH="
	      };
	      return message;
	  }

}
