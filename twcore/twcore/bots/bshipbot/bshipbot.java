package twcore.bots.bshipbot;

//Battleship Bot by D1st0rt v2.5.3

import twcore.core.*;
import java.util.*;

public class bshipbot extends SubspaceBot
{
	private BotSettings config;
	private CommandInterpreter m_commandInterpreter;
	private OperatorList oplist;
	private HashSet teamsLeft;
	private static int MAX_TEAMS, TEAMS;
	private int[] ships, X, Y;
	private int hour = 0;
	private Objset objects;
	private String objlist;
	private boolean night = true;

	//Game states
	private int state = IDLE; //haha this wouldn't work in c :D
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
			if(night)
				refresh();
		}

	};

	/********************************/
	/*			  Setup				*/
	/********************************/

	/**
	 * Constructor: Creates a new bshipbot
	 */
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

		//Commands
		m_commandInterpreter = new CommandInterpreter(m_botAction);
		registerCommands();

		//Lists
		teamsLeft = new HashSet();

		//Arrays
		ships = new int[MAX_TEAMS];
		X = new int[MAX_TEAMS];
		Y = new int[MAX_TEAMS];
		m_botAction.scheduleTaskAtFixedRate(timeMode,1000,60000);

		//Spawn points
		for(int x = 0; x < MAX_TEAMS; x++)
		{
			String coords = config.getString("Spawn"+x);
			String[] spawn = coords.split(",");
			X[x] = Integer.parseInt(spawn[0]);
			Y[x] = Integer.parseInt(spawn[1]);
		}

	}

	/**
	 * Event: LoggedOn
	 * Joins the Battleship arena, sets reliable kills
	 */
	public void handleEvent(LoggedOn event)
	{
		String initialArena = config.getString("InitialArena");
		m_botAction.joinArena(initialArena);
		m_botAction.setReliableKills(1);

	}

	/********************************/
	/*			  Commands			*/
	/********************************/

	/**
	 * Sets up all of the commands with the CommandInterpreter
	 */
	public void registerCommands()
	{
		int acceptedMessages = Message.PRIVATE_MESSAGE | Message.PUBLIC_MESSAGE | Message.ARENA_MESSAGE;
		//Base Commands
		m_commandInterpreter.registerCommand("!about", acceptedMessages, this, "C_about");
		m_commandInterpreter.registerCommand("!help", acceptedMessages, this, "C_help");
		m_commandInterpreter.registerCommand("!go", acceptedMessages | Message.REMOTE_PRIVATE_MESSAGE, this, "C_go");
		m_commandInterpreter.registerCommand("!die", acceptedMessages | Message.REMOTE_PRIVATE_MESSAGE, this, "C_die");
		m_commandInterpreter.registerCommand("!say", acceptedMessages |Message.REMOTE_PRIVATE_MESSAGE, this, "C_say");

		//Battleship Commands
		m_commandInterpreter.registerCommand("!rules", acceptedMessages, this, "C_rules");
		m_commandInterpreter.registerCommand("!allowed", acceptedMessages, this, "C_allowed");
		m_commandInterpreter.registerCommand("!teams", acceptedMessages, this, "C_teams");
		m_commandInterpreter.registerCommand("!setup",acceptedMessages, this, "C_setup");
		m_commandInterpreter.registerCommand("!start",acceptedMessages, this, "C_start");
		m_commandInterpreter.registerCommand("~state",acceptedMessages, this, "C_state");
		m_commandInterpreter.registerCommand("!stop",acceptedMessages, this, "C_stop");
		m_commandInterpreter.registerCommand("!quit",acceptedMessages, this, "C_quit");
		m_commandInterpreter.registerCommand("!remaining",acceptedMessages, this, "C_remaining");
		m_commandInterpreter.registerCommand("!remove",acceptedMessages, this, "C_remove");
		m_commandInterpreter.registerCommand("!sethour",acceptedMessages,this, "C_setHour");
		m_commandInterpreter.registerCommand("!night",acceptedMessages,this, "C_night");
	}

	/**
	 * Command: !about
	 * What this bot does
	 */
	public void C_about(String name, String message)
	{
		String[] about =
		{"+-Battleship Bot by D1st0rt-------v2.5-+",
		 "| -Make sure you n00bs don't attach to |",
		 "|   the wrong ships.                   |",
		 "| -Night Mode LVZ!                     |",
		 "| -Handle Battleship Games             |",
		 "+--------------------------------------+"};

		m_botAction.privateMessageSpam(name,about);
	}

	/**
	 * Command: !help
	 * Displays list of commands available
	 */
	public void C_help(String name, String message)
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
		 "+--------------------------------------+"};

		m_botAction.privateMessageSpam(name,help);
		if(oplist.isER(name))
			m_botAction.privateMessageSpam(name,staffhelp);
	}

	/**
	 * Command: !go <arena>
	 * Makes the bot change arena to <arena>
	 */
	public void C_go(String name, String message)
	{
		if(oplist.isZH(name) && !message.equals(""))
		{
			m_botAction.changeArena(message);
			m_botAction.setReliableKills(1);
		}
	}

	/**
	 * Command: !say <text>
	 * Makes the bot say <text>
	 */
	public void C_say(String name, String message)
	{
		if(oplist.isER(name) && !message.equals(""))
			m_botAction.sendPublicMessage(message);
	}

	/**
	 * Command: !die
	 * Terminates the bot
	 */
	public void C_die(String name, String message)
	{
		if(oplist.isZH(name))
			m_botAction.die();
	}

	/**
	 * Command: !rules
	 * Displays the rules for the current game mode
	 */
	public void C_rules(String name, String message)
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

	/**
	 * Command: !teams <number>
	 * Use this before !setup to start a game, organizes players into
	 * <number> teams to play the game with.
	 */
	public void C_teams(String name, String message)
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

	/**
	 * Command: !allowed
	 * Displays who is allowed to attach to who
	 */
	public void C_allowed(String name, String message)
	{
		String[] s =
		{"+-------------Allowed Ship Attaches-----------+",
		 "| AA(1)/Cannon(2) -> Frigate(6)/Battleship(7) |",
		 "| Plane(3)        -> Carrier(8)               |",
		 "+---------------------------------------------+"};

		m_botAction.privateMessageSpam(name,s);
	}

	/**
	 * Command: !remaining
	 * (In-Game) Displays how many capital ships remain for each team
	 */
	public void C_remaining(String name, String message)
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

	/**
	 * Command: !setup
	 * Once teams have been established
	 ****************
	 * NOTE TO HOSTS: Make all manual team arrangements before this point
	 */
	public void C_setup(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(state == PICKING || state == PLAYING)
				m_botAction.sendPrivateMessage(name,"A game is already in progress");
			else
				setUp();
		}
	}

	/**
	 * Command: !start
	 * Once teams have been created and ships assigned, this will begin the game
	 */
	public void C_start(String name, String message)
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

	/**
	 * Command: !stop
	 * Stops a currently running game.
	 */
	public void C_stop(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(state == PLAYING || state == PICKING)
			{
				m_botAction.toggleLocked();
				m_botAction.sendPrivateMessage(name,"Game Stopped.",1);
				state = IDLE;
				teamsLeft.clear();
				ships = new int[MAX_TEAMS];
			}
			else
				m_botAction.sendPrivateMessage(name,"No game in progress");
		}
	}

	/**
	 * Command: !remove <team #>
	 * If one of the team's capital ships specs or otherwise leaves, this decreases the
	 * ship count appropriately
	 */
	public void C_remove(String name, String message)
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

	/**
	 * Command: !quit
	 * If a player wants to spec, but doesn't have enough energy, they can use this
	 */
	public void C_quit(String name, String message)
	{
		m_botAction.spec(name);
		m_botAction.spec(name);
	}

	/**
	 * Command: !night <on/off>
	 * Turns night mode on or off
	 */
	public void C_night(String name, String message)
	{
		if(!oplist.isER(name))
			return;
		if(message.equalsIgnoreCase("off") && night)
		{
			m_botAction.sendUnfilteredPublicMessage("*objset"+nightObjectsOff());
			night = false;
		}
		else if(message.equalsIgnoreCase("on") && !night)
		{
			night = true;
			m_botAction.sendUnfilteredPublicMessage("*objset"+nightObjectsOn(hour));
		}
		else
			m_botAction.sendPrivateMessage(name,"Night mode: "+night);
	}

	/**
	 * Command !sethour <hour>
	 * Sets the night mode hour to <hour>, updates lvz
	 */
	public void C_setHour(String name, String message)
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
			m_botAction.sendPrivateMessage(name,"Please specify an hour between 0 and 23");
		else
			m_botAction.sendUnfilteredPublicMessage("*objset"+nightObjectsOff() + nightObjectsOn(hour = hr));
	}

	/**
	 * Unlisted control Command: ~state
	 * Use this only as a last resort to fix something (ie ~state 0 kills a game)
	 */
	public void C_state(String name, String message)
	{
		if(oplist.isER(name) || name.equals("D1st0rt"))
			state = Integer.parseInt(message);
	}

	/********************************/
	/*		Battleship Functions	*/
	/********************************/

	/**
	 * Makes random teams, and warps them to safety areas
	 * @param howmany how many teams to make
	 */
	public void makeTeams(int howmany)
	{
		TEAMS = howmany;
		StringBag plist = new StringBag();
		int current = 0;
		howmany -= 1;

		//stick all of the players in randomizer
		Iterator i = m_botAction.getPlayingPlayerIterator();
		while(i.hasNext())
			plist.add(((Player)i.next()).getPlayerName());

		//assign players to teams
		for(int z = 0, s = plist.size(); z < s; z++)
		{
			if(current > howmany)
				current = 0;
			String name = plist.grabAndRemove();
			m_botAction.setFreq(name,current);
			current++;
		}

		for(int x = 0; x <= TEAMS; x++)
		{
			if(x % 2 == 0) 	//even freq
				m_botAction.warpFreqToLocation(x, 495, 752);
			else 			//odd freq
				m_botAction.warpFreqToLocation(x, 528, 752);
		}

		state = SETUP;
	}

	/**
	 * Sets up the game by assigning players to appropriate ships
	 */
	public void setUp()
	{
		m_botAction.toggleLocked();
		state = PICKING;
		StringBag[] plist = new StringBag[TEAMS];
		for(int x = 0; x < plist.length; x++)
		{
			plist[x] = new StringBag();
		}


		//stick all of the players in randomizer
		Iterator i = m_botAction.getPlayingPlayerIterator();
		while(i.hasNext())
		{
			Player p = (Player)i.next();
			plist[p.getFrequency()].add(p.getPlayerName());
		}

		for(int x = 0; x < TEAMS; x++) //Set up 5 players in ships
		{
			int index = 4;
			for(int z = 0, s = plist[x].size(); z < s; z++)
			{
				//get a random player
				Player p = m_botAction.getPlayer(plist[x].grabAndRemove());

				if(index < 9) //first 4
				{
					m_botAction.setShip(p.getPlayerName(),index);
					ships[x]++;
				}
				else if(index < 12)//next 3
					m_botAction.setShip(p.getPlayerName(),2);
				else if(index < 15)//next 3
					m_botAction.setShip(p.getPlayerName(),1);
				else
					m_botAction.setShip(p.getPlayerName(),3);
				index++;
			}
			teamsLeft.add(new Integer(x));

		}
		//m_botAction.sendArenaMessage("Each team gets 6 turrets, pm me with !pick 1 or !pick 2 -" + m_botAction.getBotName(),2);
	}

	/**
	 * Removes a frequency from the game, checks for a winning frequency
	 * @param freq the team to remove from the game
	 */
	public void endGame(int freq)
	{
		teamsLeft.remove(new Integer(freq));
		m_botAction.sendArenaMessage("All of Team "+ freq +"'s ships have been sunk!",13);
		if(teamsLeft.size() == 1)
		{
			state = IDLE;
			int team = 0;
			Iterator i = teamsLeft.iterator();
			team = ((Integer)(i.next())).intValue();

			m_botAction.sendArenaMessage("Team "+ team +" wins!!!!",5);
			m_botAction.toggleLocked();
			m_botAction.sendUnfilteredPublicMessage("*objon 2");
			m_botAction.sendUnfilteredPublicMessage("*prize #7");
		}
	}


	/********************************/
	/*     Night Mode Functions     */
	/********************************/

	/**
	 * Called by the TimerTask, updates the night mode lvz to the next hour
	 */
	private void refresh()
	{
		if(hour > -1)
		{
			m_botAction.sendUnfilteredPublicMessage("*objset"+ nightObjectsOff() + nightObjectsOn(hour+1));
			hour++;
			if(hour >= 24)
				hour = 0;
		}
	}

	/**
	 * Used to display the night mode lvzs to players upon entering
	 */
	private void showObjects(int playerID)
	{
		if(hour > -1)
		m_botAction.sendUnfilteredPrivateMessage(playerID,"*objset"+ nightObjectsOff() + nightObjectsOn(hour));
	}

	/**
	 * Produces a string to turn off the night mode objects
	 * @return an objset formatted string to turn off lvz
	 */
	private String nightObjectsOff()
	{
		String objset = " -"+ (100 + hour) +", ";

		int id = objectID(hour);
		if(id != -1)
			objset += "-"+ id +",";

		return objset;
	}

	/**
	 * Produces a string to turn on the night mode objects
	 * @param hr the hour to turn on
	 * @return an objset formatted string to turn on lvz
	 */
	private String nightObjectsOn(int hr)
	{
		if(hr >= 24)
			hr = 0;
		String objset = " +"+ (100 + hr) +",";

		int id = objectID(hr);
		if(id != -1)
			objset += " +"+ id +",";

		return objset;
	}

	/**
	 * Calculates the darkness level needed for a particular hour
	 * @param hr the hour to find the darkness object id for
	 * @returns the objon id number of the proper lvz object
	 */
	private int objectID(int hr)
	{
		int id = -1;
		if(hr > 19)
			id = (hr - 24) + 14;
		else if(hr < 5 && hr > -1)
			id = (13 - hr);

		return id;
	}


	/********************************/
	/*		   	  Events			*/
	/********************************/

	/**
	 * Event: Message
	 * Sends to command interpreter
	 */
	public void handleEvent(Message event)
	{
    	m_commandInterpreter.handleEvent(event);
	}

	/**
	 * Event: PlayerDeath
	 * (In-Game) Checks for death of capital ships, announces, adjusts team data, checks for win
	 */
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

	/**
	 * Event: PlayerEntered
	 * Displays current night mode lvz to player
	 */
	public void handleEvent(PlayerEntered event)
	{
		showObjects(event.getPlayerID());
	}

	/**
	 * Event: TurretEvent
	 * (Attach): If not allowed, warps back
	 * (Detach): warps back
	 */
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
					if(bShip != CARRIER) //Tries to attach to Frigate/Battleship
					{
						m_botAction.setFreq(turret,100);
						m_botAction.setFreq(turret,freq);
						m_botAction.sendPrivateMessage(turret,"You can only attach to Carriers (8)");
						m_botAction.sendUnfilteredPrivateMessage(turret,"*prize #-13");
					}
					//Taken out: too many issues, not that big of a deal
					/*else if(bShip == CARRIER)
					{
						m_botAction.setFreq(turret,100);
						m_botAction.setFreq(turret,freq);

						//Note: This section won't work until position updates improve
						int x = (m_botAction.getPlayer(boat).getXLocation())/16;
						int y = (m_botAction.getPlayer(boat).getYLocation())/16;
						m_botAction.sendPrivateMessage(turret,"Cleared for takeoff!");
						m_botAction.warpTo(turret,x,y); //Warps to last mine :p
						//Detach them from carrier so they don't stay as a turret.
					}*/
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