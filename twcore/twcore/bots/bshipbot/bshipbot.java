package twcore.bots.bshipbot;

//Battleship Bot by D1st0rt v2.4

import twcore.core.*;
import java.util.*;

public class bshipbot extends SubspaceBot
{
	private BotSettings config;
	private CommandInterpreter m_commandInterpreter;
	private OperatorList oplist;
	private HashSet teamsLeft;
	private static int MAX_TEAMS, TEAMS;
	private int[] turrets, ships, X, Y;
	private int hour = 0;
	private Objset objects;
	private String objlist;
	private boolean night = true;

	//Game states
	private int state = IDLE;
	private static final int IDLE = 0, PICKING = 1, PLAYING = 2, SETUP = 3;

	//Ships
	private static final int GUN = 1, CANNON = 2, PLANE = 3, MINESWEEPER = 4,
	SUB = 5, FRIGATE = 6, BATTLESHIP = 7, CARRIER = 8;

	//Countries
	private static final int CANADA = 0, EUR_RUS = 1, USA = 2, ASIA_AUS = 3;

	TimerTask timeMode = new TimerTask()
	{
		public void run()
		{
			refresh();
		}

	};

	/********************************/
	/*			  Setup				*/
	/********************************/

	public bshipbot(BotAction botAction)
	{
		super(botAction);
		oplist = m_botAction.getOperatorList();
		config = m_botAction.getBotSettings();
		MAX_TEAMS = config.getInt("MaxTeams");
		TEAMS = 0;
		objects = new Objset();

		//Events
		EventRequester events = m_botAction.getEventRequester();
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.TURRET_EVENT);
		events.request(EventRequester.PLAYER_DEATH);
		events.request(EventRequester.PLAYER_ENTERED);
		events.request(EventRequester.BALL_POSITION); //Backup for Core's Turret handling bug

		//Commands
		m_commandInterpreter = new CommandInterpreter(m_botAction);
		registerCommands();

		//Lists
		teamsLeft = new HashSet();

		//Arrays
		turrets = new int[MAX_TEAMS];
		ships = new int[MAX_TEAMS];
		X = new int[MAX_TEAMS];
		Y = new int[MAX_TEAMS];
		m_botAction.scheduleTaskAtFixedRate(timeMode,1000,60000);
		m_botAction.setPlayerPositionUpdating(500);
	}

	public void handleEvent(LoggedOn event)
	{
		String initialArena = config.getString("InitialArena");
		m_botAction.joinArena(initialArena);
		m_botAction.setReliableKills(1);

	}

	/********************************/
	/*			  Commands			*/
	/********************************/

	public void registerCommands()
	{
		int acceptedMessages = Message.PRIVATE_MESSAGE | Message.PUBLIC_MESSAGE | Message.ARENA_MESSAGE;
		//Base Commands
		m_commandInterpreter.registerCommand("!about", acceptedMessages, this, "about");
		m_commandInterpreter.registerCommand("!help", acceptedMessages, this, "help");
		m_commandInterpreter.registerCommand("!go", acceptedMessages | Message.REMOTE_PRIVATE_MESSAGE, this, "go");
		m_commandInterpreter.registerCommand("!die", acceptedMessages | Message.REMOTE_PRIVATE_MESSAGE, this, "do_die");
		m_commandInterpreter.registerCommand("!say", acceptedMessages |Message.REMOTE_PRIVATE_MESSAGE, this, "say");

		//Extension Commands
		m_commandInterpreter.registerCommand("!rules", acceptedMessages, this, "rules");
		m_commandInterpreter.registerCommand("!allowed", acceptedMessages, this, "allowed");
		m_commandInterpreter.registerCommand("!teams", acceptedMessages, this, "teams");
		//m_commandInterpreter.registerCommand("!list", acceptedMessages, this, "list");
		m_commandInterpreter.registerCommand("!setup",acceptedMessages, this, "setup");
		m_commandInterpreter.registerCommand("!start",acceptedMessages, this, "start");
		m_commandInterpreter.registerCommand("~state",acceptedMessages, this, "setstate");
		m_commandInterpreter.registerCommand("!stop",acceptedMessages, this, "stop");
		//m_commandInterpreter.registerCommand("!pick",acceptedMessages, this, "pick");
		m_commandInterpreter.registerCommand("!quit",acceptedMessages, this, "quit");
		m_commandInterpreter.registerCommand("!remaining",acceptedMessages, this, "remaining");
		m_commandInterpreter.registerCommand("!remove",acceptedMessages, this, "removeP");
		m_commandInterpreter.registerCommand("!sethour",acceptedMessages,this, "setHour");
		m_commandInterpreter.registerCommand("!night",acceptedMessages,this, "night");
		//m_commandInterpreter.registerCommand("!eventeams",acceptedMessages,this,"evenTeams");
	}

	public void about(String name, String message)
		{
			String[] about =
			{"+-Battleship Bot by D1st0rt-------v2.4-+",
			 "| -Make sure you n00bs don't attach to |",
			 "|   the wrong ships.                   |",
			 "| -Night Mode!                         |",
			 "| -Handle Battleship Games             |",
			 "+--------------------------------------+"};

			m_botAction.privateMessageSpam(name,about);
		}

		public void help(String name, String message)
		{
			String[] help =
			{"+------------Battleship Bot------------+",
			 "| -!about         What this bot does   |",
			 "| -!help          This message         |",
			 "| -!allowed       Who can attach to who|",
			 "| -!quit          Leave the game       |",
			 "| -!remaining     Who is left (in game)|",
			 "| -!rules         Rules of the game    |",
			 "+--------------------------------------+"};
			String[] staffhelp =
			{"+-------------Staff Commands-----------+",
			 "| -!go <arena>    Sends bot to <arena> |",
			 "| -!say <msg>     Makes bot say msg    |",
			 "| -!die           Deactivate bot       |",
			 "| -!setup <#>     Sets up game (#teams)|",
			 "| -!start         Starts a set up game |",
			 "| -!stop          Stop any part of game|",
			 "| -!remove <team> Remaining -1 for team|",
			 "| -!teams <#>     Makes # random teams |",
			 "| -!sethour <#>   Set game time to #   |",
			 "| -!night <on/off>Toggle night mode    |",
			// "| -!eventeams     Evens teams          |",
			 "+--------------------------------------+"};

			m_botAction.privateMessageSpam(name,help);
			if(oplist.isER(name))
				m_botAction.privateMessageSpam(name,staffhelp);
	 	}

	public void go(String name, String message)
	{
		if(oplist.isZH(name))
		{
			if(!message.equals(""))
			{
				m_botAction.changeArena(message);
				m_botAction.setReliableKills(1);
			}
		}
	}

	public void say(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(!message.equals(""))
			{
				m_botAction.sendPublicMessage(message);
			}
		}
	}

	public void do_die(String name, String message)
	{
		if(oplist.isZH(name))
		{
			m_botAction.die();
		}
	}

	public void rules(String name, String message)
	{
			String[] game =
			{"Each team gets one of each of the 5 ships, and 6 turrets.",
			 "The rest are planes. When a ship dies it can't reenter,",
			 "but turrets and planes are free to attach. The team wins",
			 "if all enemy ships are killed."};

			String[] normal =
			{"Get your fleet together and try to control the flag",
			 "as much as you can. Remember that turrets (1 and 2)",
			 "can't stay detached out of spawn, and planes (3) use F3",
			 "to move. Attaching is regulated, check !allowed."};

		if(state == PLAYING)
			m_botAction.privateMessageSpam(name,game);
		else
			m_botAction.privateMessageSpam(name,normal);
	}

	public void teams(String name, String message)
	{
		if(oplist.isER(name))
		{
			int i = 1;
			try{
				i = Integer.parseInt(message);
			}
			catch(NumberFormatException e)
			{
				m_botAction.sendPrivateMessage(name,"Invalid number of teams");
				return;
			}
			makeTeams(i);
			m_botAction.sendArenaMessage("Making " + i + " Teams.", 111);
		}
	}

	public void allowed(String name, String message)
	{
		String[] s =
		{"+-------------Allowed Ship Attaches-----------+",
		 "| AA(1)/Cannon(2) -> Frigate(6)/Battleship(7) |",
		 "| Plane(3)        -> Carrier(8)               |",
		 "+---------------------------------------------+"};

		m_botAction.privateMessageSpam(name,s);
	}

	/*public void list(String name, String message)
	{
		if(oplist.isZH(name))
			m_botAction.sendPrivateMessage(name,"" + attached);
	}*/

	public void remaining(String name, String message)
	{
		if(state == IDLE)
			m_botAction.sendPrivateMessage(name, "No game in progress");
		else
		{
			StringBuffer s = new StringBuffer("Remaining:");
			for(int x = 0; x < TEAMS;x++)
				s.append("  Team "+ x +": "+ ships[x]);

			m_botAction.sendPrivateMessage(name,s.toString());
		}
	}

	public void setup(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(state == PICKING || state == PLAYING)
				m_botAction.sendPrivateMessage(name,"A game is already in progress");
			else if(state == IDLE)
				m_botAction.sendPrivateMessage(name,"Please establish teams with !teams <number of teams>");
			else
				setUp();
		}
	}

	/*public void pick(String name, String message)
	{
		if(state == PICKING)
		{
			boolean ok = false;
			int pick = 1;
			int team = m_botAction.getPlayer(name).getFrequency();
			if(!Tools.isAllDigits(message));
			else
			{
				pick = Integer.parseInt(message);
				if(pick == 1)
					ok = true;
				else if(pick == 2)
					ok = true;
			}
			if(m_botAction.getPlayer(name).getShipType() != 3) //Not a plane
			{
				m_botAction.sendPrivateMessage(name, "Sorry, you can't switch to a turret");
			}
			else if(ok) //Valid ship
			{
				if(turrets[team] >= 6)
					m_botAction.sendPrivateMessage(name, "Sorry, your team already has the maximum amount of turrets");
				else
				{
					turrets[team]++;
					m_botAction.setShip(name, pick);
					m_botAction.sendPublicMessage(name + " is now a turret, bringing Team "+ team +"'s turret count to "+ turrets[team]);
				}
			}
			else
				m_botAction.sendPrivateMessage(name, "Invalid ship. (!pick 1 or 2)");
		}
	}*/

	public void start(String name, String message)
	{
		if(oplist.isER(name))
			switch(state)
			{
				case IDLE:
					m_botAction.sendPrivateMessage(name,"You need to set up a game first with !setup");
				break;
				case PICKING:
					m_botAction.sendUnfilteredPublicMessage("*objon 1");
					m_botAction.sendArenaMessage("Game Begin!!!",104);
					for(int x = 0; x < teamsLeft.size();x++)
						m_botAction.warpFreqToLocation(x,X[x],Y[x]);

					m_botAction.showObject(4);
					state = PLAYING;
				break;
				case PLAYING:
					m_botAction.sendPrivateMessage(name,"There is already a game in progress.");
				break;
			}
	}

	public void stop(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(state == PLAYING || state == PICKING)
			{
				m_botAction.toggleLocked();
				m_botAction.sendPrivateMessage(name,"Game Stopped.",1);
				state = IDLE;
				teamsLeft.clear();
				turrets = new int[MAX_TEAMS];
				ships = new int[MAX_TEAMS];

			}
			else
				m_botAction.sendPrivateMessage(name,"No game in progress");
		}
	}

	public void removeP(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(state == PLAYING)
			{
				if(Tools.isAllDigits(message))
				{
					int team = Integer.parseInt(message);
					ships[team]--; //update remaining players
					m_botAction.sendPrivateMessage(name,"1 Ship removed from Team "+team);

					if(ships[team] <= 0) //Check dead players team for remaning, if nobody left
						endGame(team); //End the game, sending team as the losing team
				}
			}
		}
	}

	public void quit(String name, String message)
	{
		m_botAction.spec(name);
		m_botAction.spec(name);
	}

	public void night(String name, String message)
	{
		if(!oplist.isER(name))
			return;
		if(message.equalsIgnoreCase("off") && night)
		{
			setHour("","12");
			m_botAction.cancelTasks();
			night = false;
		}
		else if(message.equalsIgnoreCase("on") && !night)
		{
			m_botAction.scheduleTaskAtFixedRate(timeMode,1000,60000);
			night = true;
		}
	}

	public void setHour(String name, String message)
	{
		if(!oplist.isER(name))
			return;
		int hr;
		try{
			hr = Integer.parseInt(message);
		}catch(NumberFormatException e)
		{
			hr = -1;
		}
		if(hr < 0 || hr > 23)
		{
			m_botAction.sendPrivateMessage(name,"Please specify an hour between 0 and 23");
			return;
		}

		hr--;
		int i = 1000;
		if(hour > 19)
			i = (hour - 24) + 14;
		else if(hour == 4)
			i = 10;
		else if(hour == 3)
			i = 11;
		else if(hour == 2)
			i = 12;
		else if(hour == 1)
			i = 13;
		else if(hour == 0)
			i = 14;
			m_botAction.sendUnfilteredPublicMessage("*objset -"+ (100+hour) +", -"+i+",");
		hour = hr;
		refresh();
	}

	/*public void evenTeams(String name, String message)
	{
		if(oplist.isER(name))
			even();
	}*/

	public void setstate(String name, String message)
	{
		if(oplist.isER(name) || name.equals("D1st0rt"))
			state = Integer.parseInt(message);
	}

	/********************************/
	/*		Battleship Functions	*/
	/********************************/

	public void makeTeams(int howmany)
	{
		TEAMS = howmany;
		int current = 0;
		howmany -= 1;
		Iterator i = m_botAction.getPlayingPlayerIterator();
		while(i.hasNext())
		{
			if(current > howmany)
				current = 0;
			Player p = (Player)i.next();
			m_botAction.setFreq(p.getPlayerID(),current);
			current++;
		}

		state = SETUP;
	}

	public void setUp()
	{
		m_botAction.toggleLocked();
		state = PICKING;

		for(int x = 0; x < TEAMS; x++) //Set up 5 players in ships
		{
			int index = 4;
			Iterator I = m_botAction.getPlayingPlayerIterator();
			while(I.hasNext())
			{
				Player p = (Player)I.next();
				if(p.getFrequency() == x)
				{
					if(index < 9) //first 4
					{
						m_botAction.setShip(p.getPlayerName(),index);
						ships[x]++;
					}
					else if(index < 12)//next 3
					{
						m_botAction.setShip(p.getPlayerName(),1);
					}
					else if(index < 15)//next 3
					{
						m_botAction.setShip(p.getPlayerName(),2);
					}
					else
						m_botAction.setShip(p.getPlayerName(),3);
					index++;
				}

			}
			String coords = config.getString("Spawn"+x);

			String Spawn[] = coords.split(",");
			X[x] = Integer.parseInt(Spawn[0]);
			Y[x] = Integer.parseInt(Spawn[1]);
			turrets[x] = 0;
			teamsLeft.add(new Integer(x));

		}
		//m_botAction.sendArenaMessage("Each team gets 6 turrets, pm me with !pick 1 or !pick 2 -" + m_botAction.getBotName(),2);
	}

	public void endGame(int freq)
	{
		teamsLeft.remove(new Integer(freq));
		m_botAction.sendArenaMessage("All of Team "+ freq +"'s ships have been sunk!",13);
		if(teamsLeft.size() == 1)
		{
			state = IDLE;
			int team = 0;
			Iterator i = teamsLeft.iterator();
			while(i.hasNext())
			{
				team = ((Integer)(i.next())).intValue();
			}
			m_botAction.sendArenaMessage("Team "+ team +" wins!!!!",5);
			m_botAction.toggleLocked();
			m_botAction.sendUnfilteredPublicMessage("*objon 2");
			m_botAction.sendUnfilteredPublicMessage("*prize #7");
		}
	}

	public void refresh()
	{
		if(hour < 0)
			return;
		String objset = "*objset ";
		int i = 1000;
		if(hour > 19)
			i = (hour - 24) + 14;
		else if(hour == 4)
			i = 10;
		else if(hour == 3)
			i = 11;
		else if(hour == 2)
			i = 12;
		else if(hour == 1)
			i = 13;
		else if(hour == 0)
			i = 14;
		objset += "-"+ (100+hour) +", -"+i+",";

		hour++;
		if(hour > 23)
			hour = 0;

		objset += "+"+(100+hour)+",";

		i = -1;
		if(hour > 19)
			i = (hour - 24) + 14;
		else if(hour == 4)
			i = 10;
		else if(hour == 3)
			i = 11;
		else if(hour == 2)
			i = 12;
		else if(hour == 1)
			i = 13;
		else if(hour == 0)
			i = 14;

		if(i != -1)
			objset += " +"+ i + ",";

		m_botAction.sendUnfilteredPublicMessage(objset);
	}

	/********************************/
	/*		   	  Events			*/
	/********************************/

	public void handleEvent(Message event)
	{
    	m_commandInterpreter.handleEvent(event);
	}

	public void handleEvent(PlayerDeath event)
	{
		if(state == PLAYING)
		{
			int ship = m_botAction.getPlayer(event.getKilleeID()).getShipType(); //Get the ship # of the player that died
			String name = m_botAction.getPlayerName(event.getKilleeID()); //Get dead player's name
			String killer = m_botAction.getPlayerName(event.getKillerID()); // Get killer's name
			int team = m_botAction.getPlayer(event.getKilleeID()).getFrequency(); //Get dead player's team #
			switch(ship)
			{
				case MINESWEEPER:
					m_botAction.sendArenaMessage("Team "+ team + "'s Minesweeper ("+ name +") just got blown up by " + killer,19);
					m_botAction.setShip(name, 3);
					ships[team]--; //update remaining players
				break;

				case SUB:
					m_botAction.sendArenaMessage("Team "+ team +"'s Sub ("+ name +") just got destroyed by "+ killer + ". I guess cloak didn't help...",19);
					m_botAction.setShip(name, 3);
					ships[team]--; //update remaining players
				break;

				case FRIGATE:
					m_botAction.sendArenaMessage("Awww snap! Team "+ team+"'s Frigate ("+ name +") just got pwned by "+ killer +"! Where will all of the turrets go?",25);
					m_botAction.setShip(name, 3);
					ships[team]--; //update remaining players
				break;

				case BATTLESHIP:

					m_botAction.sendArenaMessage("Inconceivable! Team "+ team +" just lost their Battleship ("+ name +"). It's not looking good for them now...",7);
					m_botAction.setShip(name, 3);
					ships[team]--; //update remaining players
				break;

				case CARRIER:
					m_botAction.sendArenaMessage("The Carrier for Team " + team + " ("+ name +") was just demolished by "+ killer +" , effectively shutting down their entire Air Force!",10);
					m_botAction.setShip(name, 3);
					ships[team]--; //update remaining players
				break;
			}

			if(ships[team] <= 0) //Check dead players team for remaning, if nobody left
				endGame(team); //End the game, sending team as the losing team
		}
	}

	public void handleEvent(PlayerEntered event)
	{
		int pId = event.getPlayerID();
		m_botAction.sendUnfilteredPrivateMessage(pId,"*objon "+ (100+hour));

			int i = 1000;
			if(hour > 19)
				i = (hour - 24) + 14;
			else if(hour == 4)
				i = 10;
			else if(hour == 3)
				i = 11;
			else if(hour == 2)
				i = 12;
			else if(hour == 1)
				i = 13;
			else if(hour == 0)
				i = 14;

			m_botAction.sendUnfilteredPrivateMessage(pId,"*objon " + i);

	}

	public void handleEvent(TurretEvent event)
	{
		//m_botAction.sendPublicMessage("Attach detected");
		int turret = event.getAttacherID();
		int freq = m_botAction.getPlayer(turret).getFrequency();
		String tname = m_botAction.getPlayerName(turret);
		int tShip = m_botAction.getPlayer(turret).getShipType();
		int bShip = 0;

		int boat = event.getAttacheeID();
		//Tools.printLog("N:"+turret+"->"+boat);
		if(m_botAction.getPlayer(boat) != null)
		{
		//	Tools.printLog("S:"+tShip+"->"+bShip);
			bShip = m_botAction.getPlayer(boat).getShipType();

			switch(tShip)
			{
				case GUN:
				if(bShip == CARRIER) //Tries to attach to Carrier
					{
						m_botAction.setFreq(turret,100);
						m_botAction.setFreq(turret,freq);
						m_botAction.sendPrivateMessage(turret,"You can only attach to Frigates (6) and Battleships (7).");
						m_botAction.sendUnfilteredPrivateMessage(turret,"*prize #-13");
					}
				break;
				case CANNON:
				if(bShip == CARRIER) //Tries to attach to Carrier
					{
						m_botAction.setFreq(turret,100);
						m_botAction.setFreq(turret,freq);
						m_botAction.sendPrivateMessage(turret,"You can only attach to Frigates (6) and Battleships (7).");
						m_botAction.sendUnfilteredPrivateMessage(turret,"*prize #-13");
					}
				break;
				case PLANE:
					if(bShip == BATTLESHIP || bShip == FRIGATE) //Tries to attach to Frigate/Battleship
					{
						m_botAction.setFreq(turret,100);
						m_botAction.setFreq(turret,freq);
						m_botAction.sendPrivateMessage(turret,"You can only attach to Carriers (8)");
						m_botAction.sendUnfilteredPrivateMessage(turret,"*prize #-13");
					}
					//Taken out: too many issues, not that big of a deal
					else if(bShip == CARRIER)
					{
						/*m_botAction.setFreq(turret,100);
						m_botAction.setFreq(turret,freq);
						int x = (m_botAction.getPlayer(boat).getXLocation())/16;
						int y = (m_botAction.getPlayer(boat).getYLocation())/16;*/
						m_botAction.sendPrivateMessage(turret,"Cleared for takeoff!");
						//m_botAction.warpTo(turret,x,y); //Warps to last mine :p
					}//Detach them from carrier so they don't stay as a turret.
				break;
				default:
					m_botAction.setFreq(turret,100);
					m_botAction.setFreq(turret,freq);
					m_botAction.sendPrivateMessage(turret,"You are not allowed to attach.");
					m_botAction.sendUnfilteredPrivateMessage(turret,"*prize #-13");
				break;
			}
		}

		else
		{
			switch(tShip)//Warp turrets back to spawn so they don't sit there w/o ship
			{
				case GUN:
					m_botAction.sendUnfilteredPrivateMessage(turret,"*prize #7");
				break;
				case CANNON:
					m_botAction.sendUnfilteredPrivateMessage(turret,"*prize #7");
				break;
			}
		}
	}
}