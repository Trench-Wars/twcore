package twcore.bots.twbot;

//Attach Regulation TWBotExtension by D1st0rt v1.0

import twcore.core.*;

public class twbotturret extends TWBotExtension
{
	private boolean warpBack;
	private boolean[][] rules;

	public twbotturret()
	{
		warpBack = false;
		rules = new boolean[8][8];
	}

	//Enforcement
	public void handleEvent(TurretEvent event)
	{
		int tID = event.getAttacherID(); //turret ID
		int aID = event.getAttacheeID(); //anchor ID
		Player attacher = m_botAction.getPlayer(tID);
		Player attachee = m_botAction.getPlayer(aID);
		int tShip = attacher.getShipType() - 1;

		int freq = attacher.getFrequency();

		if(attachee != null) //Attaching
		{
			int aShip = attachee.getShipType() - 1;
			boolean allowed = rules[aShip][tShip];
			if(!allowed) //not allowed
			{
				m_botAction.setFreq(tID,Math.abs(1-freq)); //Safe for arenas with only 2 freqs
				m_botAction.setFreq(tID,freq);
			}

		}
		else //Detaching
		{
			if(warpBack)
			{
				m_botAction.setFreq(tID,Math.abs(1-freq)); //Safe for arenas with only 2 freqs
				m_botAction.setFreq(tID,freq);
			}
		}
	}

	//The easy stuff, rule manipulation

	public void setAll(boolean allowed)
	{
		for(int x = 0; x < 8;x++)
			for(int y = 0; y < 8;y++)
				rules[x][y] = allowed;
	}

	public void setIndiv(int anchor, int turret, boolean allowed)
	{
		rules[anchor][turret] = allowed;
	}

	public void setAttachable(int anchor, boolean allowed)
	{
		for(int x = 0; x < 8; x++)
			rules[anchor][x] = allowed;
	}

	public void setCanAttach(int turret, boolean allowed)
	{
		for(int x = 0; x < 8; x++)
			rules[x][turret] = allowed;
	}

	public String[] getHelpMessages()
	{
		String help[] = {
		"Note: Use ship numbers 1-8 for ships (anchor, turret)",
		"!check <anchor>:<turret> - checks if turret can attach to anchor",
		"!checka <anchor> - shows all ships allowed to attach to it",
		"!checkt <turret> - shows all ships it can attach to",
		"!set <t/f>:<anchor>:<turret> - sets a rule of attaching",
		"!seta <t/f>:<anchor> - sets rule for all ships attaching to anchor",
		"!sett <t/f>:<turret> - sets rule for turret attaching to all ships",
		"!setall <t/f> - sets rule for all attaching",
		"!warpback <on/off> - warp players back to spawn on detach",
		"!rules - announces attaching rules in an arena message"};
		return help;
	}

	//The fun stuff: Command Interpretation :D

	public void handleEvent(Message event)
	{
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if(m_opList.isER(name))
				delegateCommand(name,event.getMessage());
		}
	}

	public void delegateCommand(String name, String message)
	{
		message = message.toLowerCase(); //case insensitive

		//pass along to proper function
		try{
		if(message.startsWith("!check "))
			interpretCheck(name,message.substring(7));
		else if(message.startsWith("!checka"))
			interpretCheckA(name,message.substring(8));
		else if(message.startsWith("!checkt"))
			interpretCheckT(name,message.substring(8));
		else if(message.startsWith("!set "))
			interpretSet(name,message.substring(5));
		else if(message.startsWith("!seta "))
			interpretSetA(name,message.substring(6));
		else if(message.startsWith("!sett"))
			interpretSetT(name,message.substring(6));
		else if(message.startsWith("!setall"))
			interpretSetAll(name,message.substring(8));
		else if(message.startsWith("!warpback"))
			interpretWarpBack(name,message.substring(10));
		else if(message.startsWith("!rules"))
			showRules();
		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name,"Please check the syntax of your command with !help turret");
		}
	}

	public void interpretCheck(String name, String message)
	{
		int anchor, turret;
		try{
			String[] pieces = message.split(":");
			anchor = Integer.parseInt(pieces[0]) - 1;
			turret = Integer.parseInt(pieces[1]) - 1;
			if(anchor > 7 || anchor < 0 || turret > 7 || turret < 0)
				throw new Exception();
		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !check 5:2 (wb attach to terr)");
			return;
		}

		if(rules[anchor][turret])
			m_botAction.sendPrivateMessage(name, ""+ Tools.shipName(turret + 1) +"->"+ Tools.shipName(anchor + 1) +" Allowed");
		else
			m_botAction.sendPrivateMessage(name, ""+ Tools.shipName(turret + 1) +"->"+ Tools.shipName(anchor + 1) +" Not Allowed");
	}

	public void interpretCheckA(String name, String message)
	{
		int anchor;
		try{
			anchor = Integer.parseInt(message) - 1;
			if(anchor > 7 || anchor < 0)
				throw new Exception();
		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !checka 5 (all attach to terr)");
			return;
		}

		StringBuffer buf = new StringBuffer();
		buf.append(Tools.shipName(anchor + 1)+ ": ");
		for(int x = 0; x < 8; x++)
			if(rules[anchor][x])
				buf.append(Tools.shipName(x + 1) + " ");

		if(buf.toString().endsWith(": "));
			buf.append("none");

		m_botAction.sendPrivateMessage(name, buf.toString());
	}

	public void interpretCheckT(String name, String message)
	{
		int turret;
		try{
			turret = Integer.parseInt(message) - 1;
			if(turret > 7 || turret < 0)
				throw new Exception();
		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !checkt 1 (wb attach to all)");
			return;
		}

		StringBuffer buf = new StringBuffer();
		buf.append(Tools.shipName(turret + 1)+ ": ");
		for(int x = 0; x < 8; x++)
			if(rules[x][turret])
				buf.append(Tools.shipName(x + 1) + " ");

		if(buf.toString().endsWith(": "));
			buf.append("none");

		m_botAction.sendPrivateMessage(name, buf.toString());
	}

	public void interpretSet(String name, String message)
	{
		boolean allowed;
		int anchor, turret;
		try{
			String[] pieces = message.split(":");
			anchor = Integer.parseInt(pieces[1]) - 1;
			turret = Integer.parseInt(pieces[2]) - 1;

			if(pieces[0].equals("t"))
				allowed = true;
			else if(pieces[0].equals("f"))
				allowed = false;
			else
				throw new Exception();

			if(anchor > 7 || anchor < 0 || turret > 7 || turret < 0)
				throw new Exception();

		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !set t:5:2 (wb can attach to terr)");
			return;
		}

		setIndiv(anchor,turret,allowed);
		String s = Tools.shipName(turret + 1) +"->"+ Tools.shipName(anchor + 1) +" is now ";
		if(allowed)
			s += "allowed";
		else
			s += "not allowed";

		m_botAction.sendPrivateMessage(name,s);
	}

	public void interpretSetA(String name, String message)
	{
		boolean allowed;
		int anchor;
		try{
			String[] pieces = message.split(":");
			anchor = Integer.parseInt(pieces[1])- 1;

			if(pieces[0].equals("t"))
				allowed = true;
			else if(pieces[0].equals("f"))
				allowed = false;
			else
				throw new Exception();

			if(anchor > 7 || anchor < 0)
				throw new Exception();
		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !seta f:5 (none can attach to terr)");
			return;
		}

		setAttachable(anchor,allowed);

		String s;
		if(allowed)
			s = "All ";
		else
			s = "None ";
		s += "can attach to "+ Tools.shipName(anchor + 1);

		m_botAction.sendPrivateMessage(name,s);

	}

	public void interpretSetT(String name, String message)
	{
		boolean allowed;
		int turret;
		try{
			String[] pieces = message.split(":");
			turret = Integer.parseInt(pieces[1])- 1;

			if(pieces[0].equals("t"))
				allowed = true;
			else if(pieces[0].equals("f"))
				allowed = false;
			else
				throw new Exception();

			if(turret > 7 || turret < 0)
				throw new Exception();
		}catch(Exception e)
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !seta t:1 (wb can attach to all)");
			return;
		}

		setCanAttach(turret,allowed);

		String s = Tools.shipName(turret + 1);
		if(allowed)
			s += " can attach to all";
		else
			s += " cannot attach to any";

		m_botAction.sendPrivateMessage(name, s);

	}

	public void interpretSetAll(String name, String message)
	{
		boolean allowed;
		if(message.startsWith("t"))
			allowed = true;
		else if(message.startsWith("f"))
			allowed = false;
		else
		{
			m_botAction.sendPrivateMessage(name, "Example syntax: !setall f (no attaching allowed)");
			return;
		}

		setAll(allowed);

		if(allowed)
			m_botAction.sendPrivateMessage(name, "Everyone can attach to everyone");
		else
			m_botAction.sendPrivateMessage(name, "Nobody can attach");

	}

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

		warpBack = active;
		String s = "WarpBack is ";
		if(warpBack)
			s += "on.";
		else
			s += "off.";

		m_botAction.sendPrivateMessage(name, s);
	}

	//Oooh! Sparkly! :D
	public void showRules()
	{
		String disp[] = new String[9];
		disp[0] = "=========ALLOWED ATTACHING=========";
		for(int x = 1; x < 9; x++)
		{
			disp[x] = Tools.shipName(x).toUpperCase() + " can attach to: ";
			for(int y = 0; y < 8; y++)
			{
				if(rules[y][x-1])
					disp[x] += Tools.shipName(y+1) + " ";
			}
			if(disp[x].endsWith("to: "))
				disp[x] += "none";
		}

		for(int x = 0; x < 9; x++)
			m_botAction.sendArenaMessage(disp[x]);
	}

	public void cancel()
	{
	}
}
