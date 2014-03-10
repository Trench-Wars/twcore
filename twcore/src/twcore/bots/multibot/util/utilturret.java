package twcore.bots.multibot.util;

//

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.TurretEvent;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

/**
 * Attach Regulation.
 * 
 * @author D1st0rt
 * @version 1.5
 */
public class utilturret extends MultiUtil
{
	private boolean warpBack;
	private boolean[][] rules;

	public void init()
	{
		warpBack = false;
		rules = new boolean[8][8];

	//Set all rules initially to allow attaching as default
		for(int i = 0; i < 8; i++)
			for(int j = 0; j < 8; j++)
				rules[i][j] = true;
	}

    /**
     * Requests events.
     */
    public void requestEvents( ModuleEventRequester modEventReq ) {
        modEventReq.request(this, EventRequester.TURRET_EVENT );
    }    
    
	//Enforcement
	/**
	 * What to do when the bot receives notification of an attach/detach
	 */
	public void handleEvent(TurretEvent event)
	{
		int tID = event.getAttacherID(); //turret ID
		int aID = event.getAttacheeID(); //anchor ID
		Player attacher = m_botAction.getPlayer(tID);
		Player attachee = m_botAction.getPlayer(aID);
		int tShip = attacher.getShipType() - 1;
		int freq = attacher.getFrequency();

		if(event.isAttaching()) //Attaching
		{
		    if(attachee == null)
		        return;
			int aShip = attachee.getShipType() - 1; //convert to 0-7
			boolean allowed = rules[aShip][tShip]; //check rule for attaching
			if(!allowed) //not allowed
			{
				//change their freq to make them get off, then put them back
				m_botAction.setFreq(tID,Math.abs(1-freq)); //Safe for arenas with only 2 freqs
				m_botAction.setFreq(tID,freq);
			}

		}
		else //Detaching
			if(warpBack)
			{
				m_botAction.sendUnfilteredPrivateMessage(tID,"*prize #7"); //prizes "warp!"
				m_botAction.sendUnfilteredPrivateMessage(tID,"*prize #-13"); //takes away their energy
			}
	}

	//The easy stuff, rule manipulation

	/**
	 * Turns all restrictions on or off
	 * @param allowed value to assign rule as
	 */
	public void setAll(boolean allowed)
	{
		for(int x = 0; x < 8;x++) //anchor loop
			for(int y = 0; y < 8;y++) //turret loop
				rules[x][y] = allowed;
	}

	/**
	 * Sets an individual restriction
	 * @param anchor number of ship being attached to
	 * @param turret number of ship attaching
	 * @param allowed value to assign rule as
	 */
	public void setIndiv(int anchor, int turret, boolean allowed)
	{
		rules[anchor][turret] = allowed;
	}

	/**
	 * Sets restrictions for all ships attaching to a ship
	 * @param anchor number of ship being attached to
	 * @param allowed value to assign rule as
	 */
	public void setAttachable(int anchor, boolean allowed)
	{
		for(int x = 0; x < 8; x++) //turret loop
			rules[anchor][x] = allowed;
	}

	/**
	 * Sets restrictions for a ship attaching to all ships
	 * @param turret number of ship attaching
	 * @param allowed value to assign rule as
	 */
	public void setCanAttach(int turret, boolean allowed)
	{
		for(int x = 0; x < 8; x++) //anchor loop
			rules[x][turret] = allowed;
	}

	/**
	 * Returns a list of commands specific to this extension
	 */
	public String[] getHelpMessages()
	{
		String help[] = {
		"Note: Use ship numbers 1-8 for ships (anchor, turret)",
		"!check <anchor>:<turret> - checks if turret can attach to anchor",
		"!checkattach <anchor> - shows all ships allowed to attach to it",
		"!checkturret <turret> - shows all ships it can attach to",
		"!checkall - displays all rules privately",
		"!set <y/n>:<anchor>:<turret> - sets a rule of attaching",
		"!setattach <y/n>:<anchor> - sets rule for all ships attaching to anchor",
		"!setturret <y/n>:<turret> - sets rule for turret attaching to all ships",
		"!setall <y/n> - sets rule for all attaching",
		"!warpback <on/off> - warp players back to spawn on detach",
		"!rules - announces attaching rules in an arena message"};
		return help;
	}

	//The fun stuff: Command Interpretation :D

	/**
	 * What to do when the bot receives a chat message
	 */
	public void handleEvent(Message event)
	{
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if(m_opList.isER(name)) //has credentials
				delegateCommand(name,event.getMessage());
		}
	}

	/**
	 * Takes a command and passes it to the proper command function
	 * @param name the name of the sender
	 * @param message the text of the chat message
	 */
	public void delegateCommand(String name, String message)
	{
		message = message.toLowerCase(); //case insensitive

		//pass along to proper function
		try{
		if(message.startsWith("!check "))
			interpretCheck(name,message.substring(7));
		else if(message.startsWith("!checkattach "))
			interpretCheckA(name,message.substring(8));
		else if(message.startsWith("!checkturret "))
			interpretCheckT(name,message.substring(8));
		else if(message.startsWith("!checkall"))
			interpretCheckAll(name,message);
		else if(message.startsWith("!set "))
			interpretSet(name,message.substring(5));
		else if(message.startsWith("!setattach "))
			interpretSetA(name,message.substring(6));
		else if(message.startsWith("!setturret "))
			interpretSetT(name,message.substring(6));
		else if(message.startsWith("!setall"))
			interpretSetAll(name,message.substring(8));
		else if(message.startsWith("!warpback"))
			interpretWarpBack(name,message.substring(10));
		else if(message.startsWith("!rules"))
			showRules();
		}catch(Exception e) //catches index exceptions for messages being too short
		{
			m_botAction.sendPrivateMessage(name,"Please check the syntax of your command with !help turret");
		}
	}

	/**
	 * Interprets a !check command from the user
	 * @param name the name of the sender
	 * @param message the parameters of the command
	 */
	public void interpretCheck(String name, String message)
	{
		int anchor, turret;
		try{
			String[] pieces = message.split(":");
			anchor = Integer.parseInt(pieces[0]) - 1; //convert to 0-7
			turret = Integer.parseInt(pieces[1]) - 1;
			if(anchor > 7 || anchor < 0 || turret > 7 || turret < 0) //invalid ship #
				throw new Exception();
		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !check 5:2 (wb attach to terr)");
			return;
		}

		String msg = Tools.shipName(turret + 1) +"->"+ Tools.shipName(anchor + 1);
		msg += (rules[anchor][turret] ? " Allowed" : " Not Allowed");

		//grab rule and inform
		m_botAction.sendPrivateMessage(name, msg);
	}

	/**
	 * Interprets a !checka command from the user
	 * @param name the name of the sender
	 * @param message the parameters of the command
	 */
	public void interpretCheckA(String name, String message)
	{
		int anchor;
		try{
			anchor = Integer.parseInt(message) - 1;
			if(anchor > 7 || anchor < 0) //invalid ship #
				throw new Exception();
		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !checka 5 (all attach to terr)");
			return;
		}

		StringBuffer buf = new StringBuffer();
		buf.append(Tools.shipName(anchor + 1)+ ": ");
		for(int x = 0; x < 8; x++) //turret loop
			if(rules[anchor][x])
				buf.append(Tools.shipName(x + 1) + " ");

		if(buf.toString().endsWith(": ")) //no ships appended, buf is unaltered
			buf.append("none");

		//grab rule and inform
		m_botAction.sendPrivateMessage(name, buf.toString());
	}

	/**
	 * Interprets a !checkt command from the user
	 * @param name the name of the sender
	 * @param message the parameters of the command
	 */
	public void interpretCheckT(String name, String message)
	{
		int turret;
		try{
			turret = Integer.parseInt(message) - 1;
			if(turret > 7 || turret < 0) //invalid ship #
				throw new Exception();
		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !checkt 1 (wb attach to all)");
			return;
		}

		StringBuffer buf = new StringBuffer();
		buf.append(Tools.shipName(turret + 1)+ ": ");
		for(int x = 0; x < 8; x++) //anchor loop
			if(rules[x][turret])
				buf.append(Tools.shipName(x + 1) + " ");

		if(buf.toString().endsWith(": ")) //no ships appended, buf is unaltered
			buf.append("none");

		//grab rule and inform
		m_botAction.sendPrivateMessage(name, buf.toString());
	}

	/**
	 * Interprets a !checkall command from the user
	 * @param name the name of the sender
	 * @param message the parameters of the command
	 */
	public void interpretCheckAll(String name, String message)
	{
		String disp[] = new String[9];
		disp[0] = "=========ALLOWED ATTACHING=========";
		for(int x = 1; x < 9; x++) //anchor loop
		{
			//Tools.shipName(x) requires 1-8 numbering
			disp[x] = Tools.shipName(x).toUpperCase() + " can attach to: ";
			for(int y = 0; y < 8; y++) //turret loop
			{
				if(rules[y][x-1]) //x-1 to access correct array
					disp[x] += Tools.shipName(y+1) + " ";
			}
			if(disp[x].endsWith("to: ")) //no ships appended, disp[x] is unaltered
				disp[x] += "none";
		}

		//grab rules and inform
		m_botAction.privateMessageSpam(name, disp);
	}

	/**
	 * Interprets a !set command from the user
	 * @param name the name of the sender
	 * @param message the parameters of the command
	 */
	public void interpretSet(String name, String message)
	{
		boolean allowed;
		int anchor, turret;
		try{
			String[] pieces = message.split(":");
			anchor = Integer.parseInt(pieces[1]) - 1;
			turret = Integer.parseInt(pieces[2]) - 1;

			if(pieces[0].equals("y")) //allowed
				allowed = true;
			else if(pieces[0].equals("n")) //not allowed
				allowed = false;
			else
				throw new Exception(); //didn't enter "t" or "f"

			if(anchor > 7 || anchor < 0 || turret > 7 || turret < 0) //invalid ship #
				throw new Exception();

		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !set t:5:2 (wb can attach to terr)");
			return;
		}

		setIndiv(anchor,turret,allowed); //update rule
		String s = Tools.shipName(turret + 1) +"->"+ Tools.shipName(anchor + 1) +" is now ";
		s += (allowed ? "allowed" : "not allowed");

		//inform of change
		m_botAction.sendPrivateMessage(name,s);
	}

	/**
	 * Interprets a !seta command from the user
	 * @param name the name of the sender
	 * @param message the parameters of the command
	 */
	public void interpretSetA(String name, String message)
	{
		boolean allowed;
		int anchor;
		try{
			String[] pieces = message.split(":");
			anchor = Integer.parseInt(pieces[1])- 1;

			if(pieces[0].equals("y")) //allowed
				allowed = true;
			else if(pieces[0].equals("n")) //not allowed
				allowed = false;
			else
				throw new Exception(); //didn't enter "t" or "f"

			if(anchor > 7 || anchor < 0) //invalid ship #
				throw new Exception();
		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !seta f:5 (none can attach to terr)");
			return;
		}

		setAttachable(anchor,allowed); //update rule

		String s = (allowed ? "All " : "None ");
		s += "can attach to "+ Tools.shipName(anchor + 1);

		//inform of change
		m_botAction.sendPrivateMessage(name,s);

	}

	/**
	 * Interprets a !sett command from the user
	 * @param name the name of the sender
	 * @param message the parameters of the command
	 */
	public void interpretSetT(String name, String message)
	{
		boolean allowed;
		int turret;
		try{
			String[] pieces = message.split(":");
			turret = Integer.parseInt(pieces[1])- 1;

			if(pieces[0].equals("y")) //allowed
				allowed = true;
			else if(pieces[0].equals("n")) //not allowed
				allowed = false;
			else
				throw new Exception(); //didn't enter "t" or "f"

			if(turret > 7 || turret < 0) //invalid ship #
				throw new Exception();
		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !seta t:1 (wb can attach to all)");
			return;
		}

		setCanAttach(turret,allowed); //update rule

		String s = Tools.shipName(turret + 1) + (allowed ? " can attach to all" : " cannot attach to any");

		//inform of change
		m_botAction.sendPrivateMessage(name, s);
	}

	/**
	 * Interprets a !setall command from the user
	 * @param name the name of the sender
	 * @param message the parameters of the command
	 */
	public void interpretSetAll(String name, String message)
	{
		boolean allowed;
		if(message.startsWith("y")) //no attach restrictions
			allowed = true;
		else if(message.startsWith("n")) //no attaching
			allowed = false;
		else
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !setall f (no attaching allowed)");
			return;
		}

		setAll(allowed); //update rule

		//inform of change
		m_botAction.sendPrivateMessage(name, (allowed ? "No attach restrictions" : "No attaching"));
	}

	/**
	 * Interprets a !warpback command from the user
	 * @param name the name of the sender
	 * @param message the parameters of the command
	 */
	public void interpretWarpBack(String name, String message)
	{
		boolean active;
		if(message.startsWith("on"))
			active = true;
		else if(message.startsWith("off"))
			active = false;
		else
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !warpback on (turns WarpBack on)");
			return;
		}

		warpBack = active; //update setting
		String s = "WarpBack is "+ (warpBack ? "on.":"off.");

		//inform of change
		m_botAction.sendPrivateMessage(name, s);
	}

	/**
	 * Displays all of the attaching rules as an arena message
	 */
	public void showRules()
	{
		String disp[] = new String[9];
		disp[0] = "=========ALLOWED ATTACHING=========";
		for(int x = 1; x < 9; x++) //anchor loop
		{
			//Tools.shipName(x) requires 1-8 numbering
			disp[x] = Tools.shipName(x).toUpperCase() + " can attach to: ";
			for(int y = 0; y < 8; y++) //turret loop
			{
				if(rules[y][x-1]) //x-1 to access correct array
					disp[x] += Tools.shipName(y+1) + " ";
			}
			if(disp[x].endsWith("to: ")) //no ships appended, disp[x] is unaltered
				disp[x] += "none";
		}

		//grab rules and inform
		for(int x = 0; x < 9; x++)
			m_botAction.sendArenaMessage(disp[x]);
	}

	/**
	 * Not used except that it's an abstract method needed to compile
	 */
	public void cancel()
	{
	}
}