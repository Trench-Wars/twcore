package twcore.bots.bshipbot;

import twcore.core.*;
import java.util.*;

/**
 * Battleship Bot
 *
 * @Author D1st0rt
 * @Version 3.0
 */

public class bshipbot extends SubspaceBot
{
	//Class Objects
	private CommandInterpreter m_cmd;
	private OperatorList oplist;
	private StartWarp startWarp;
	private CapshipNotify notify;

	//Night Mode
	private int hour;
	private boolean night;

	//Game settings
	private byte state, board, teams, ships[][];
	private static final byte IDLE = 0, ACTIVE = 1;
	private boolean lockCapShips;

	//Ships
	private static final byte GUN = 1, CANNON = 2, PLANE = 3, MINESWEEPER = 4,
	SUB = 5, FRIGATE = 6, BATTLESHIP = 7, CARRIER = 8;

	//Geometry Constants
	private static final byte X = 0, Y = 1, HEIGHT = 2, WIDTH = 3;

	private NightUpdate timeMode = new NightUpdate();

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

		//Events
		EventRequester events = m_botAction.getEventRequester();
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.TURRET_EVENT);
		events.request(EventRequester.PLAYER_DEATH);
		events.request(EventRequester.PLAYER_ENTERED);
		events.request(EventRequester.PLAYER_LEFT);
		events.request(EventRequester.FREQUENCY_SHIP_CHANGE);

		//Commands
		m_cmd = new CommandInterpreter(m_botAction);
		registerCommands();

		//Default Values for Settings
		hour = 0;
		night = false;
		state = IDLE;
		teams = 2;
		lockCapShips = false;
	}

	/**
	 * Event: LoggedOn
	 * Joins the Battleship arena, sets reliable kills
	 */
	public void handleEvent(LoggedOn event)
	{
		String initialArena = m_botAction.getBotSettings().getString("InitialArena");
		m_botAction.joinArena(initialArena);
		m_botAction.setReliableKills(1);

		//Start Night Mode
		m_botAction.scheduleTaskAtFixedRate(timeMode,1000,60000);
	}

	/********************************/
	/*			  Commands			*/
	/********************************/

	/**
	 * Sets up all of the commands with the CommandInterpreter
	 */
	private void registerCommands()
	{
		int priv = Message.PRIVATE_MESSAGE;
		int pub = Message.PUBLIC_MESSAGE;
		int rem = Message.REMOTE_PRIVATE_MESSAGE;

		//Base Commands
		m_cmd.registerCommand("!about", priv | pub, this, "C_about");
		m_cmd.registerCommand("!help", priv | pub, this, "C_help");
		m_cmd.registerCommand("!go", priv | rem, this, "C_go");
		m_cmd.registerCommand("!die", priv | rem, this, "C_die");
		m_cmd.registerCommand("!say", priv | rem, this, "C_say");

		//Night Mode Commands
		m_cmd.registerCommand("!sethour", priv, this, "C_setHour");
		m_cmd.registerCommand("!night", priv, this, "C_night");

		//Battleship Game Commands
		m_cmd.registerCommand("!rules", priv | pub, this, "C_rules");
		m_cmd.registerCommand("!status", priv | pub, this, "C_status");
		m_cmd.registerCommand("!assign", priv, this, "C_assign");
		m_cmd.registerCommand("!set", priv, this, "C_set");
		m_cmd.registerCommand("!start", priv, this, "C_start");
		m_cmd.registerCommand("!stop", priv, this, "C_stop");
		m_cmd.registerCommand("!quit", priv | pub, this, "C_quit");
		m_cmd.registerCommand("!scheck", priv, this, "C_scheck");

	}

	/**
	 * Command: !about
	 * Parameters:
	 * What this bot does
	 */
	public void C_about(String name, String message)
	{
		String[] about =
		{"+-Battleship Bot by D1st0rt-------v3.0-+",
		 "| -Attach Regulation                   |",
		 "| -Night Mode LVZ Automation           |",
		 "| -Battleship Games                    |",
		 "+--------------------------------------+"};

		m_botAction.privateMessageSpam(name,about);
	}

	/**
	 * Command: !help
	 * Parameters:
	 * Displays list of commands available
	 */
	public void C_help(String name, String message)
	{
		String[] help =
		{"+------------Battleship Bot------------+",
		 "| -!about         What this bot does   |",
		 "| -!help          This message         |",
		 "| -!quit          Enter spectator mode |",
		 "| -!rules         Rules of the game    |",
		 "| -!status        What is happening    |",
		 "+--------------------------------------+"};
		String[] staffhelp =
		{"+-------------Staff Commands-----------+",
		 "| -!go <arena>    Sends bot to <arena> |",
		 "| -!die           Deactivate bot       |",
		 "| -!start         Starts a game        |",
		 "| -!stop          Stop a game          |",
		 "| -!assign        See <!assign help>   |",
		 "| -!set           See <!set help>      |",
		 "| -!scheck        Manual game update   |",
		 "| -!sethour <#>   Set game time to #   |",
		 "| -!night <on/off>Toggle night mode    |",
		 "+--------------------------------------+"};

		m_botAction.privateMessageSpam(name,help);
		if(oplist.isER(name))
			m_botAction.privateMessageSpam(name,staffhelp);
	}

	/**
	 * Command: !go
	 * Parameters: <arena>
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
	 * Command: !say
	 * Parameters: <text>
	 * Makes the bot say <text>
	 */
	public void C_say(String name, String message)
	{
		if(oplist.isER(name) && !message.equals(""))
			m_botAction.sendPublicMessage(message);
	}

	/**
	 * Command: !die
	 * Parameters
	 * Terminates the bot
	 */
	public void C_die(String name, String message)
	{
		if(oplist.isZH(name))
			m_botAction.die();
	}

	/**
	 * Command: !rules
	 * Parameters:
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
		 "to move."};

		if(state == ACTIVE)
			m_botAction.privateMessageSpam(name,game);
		else
			m_botAction.privateMessageSpam(name,normal);

		String[] s =
		{"+-------------Allowed Ship Attaches-----------+",
		 "| AA(1)/Cannon(2) -> Frigate(6)/Battleship(7) |",
		 "| Plane(3)        -> Carrier(8)               |",
		 "+---------------------------------------------+"};

		m_botAction.privateMessageSpam(name,s);
	}

	/**
	 * Command: !assign
	 * Parameters: [team] [ship]
	 * Assigns players to random teams and ships based on game settings
	 */
	public void C_assign(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(message.equalsIgnoreCase("help"))
			{
				String[] s = new String[]{
					"You can use this to automatically assign",
					"Random players to teams and ships. Just put",
					"\"team\" and/or \"ship\" after !assign."};

					m_botAction.privateMessageSpam(name, s);
			}
			else if(state == IDLE)
			{
				try{
					StringBuffer buf = new StringBuffer();
					message = message.toLowerCase();
					if(message.indexOf("team") != -1)
					{
						makeTeams(teams);
						buf.append("Teams ");
					}

					if(message.indexOf("ship") != -1)
					{
						randomShips();
						buf.append("Ships ");
					}

					if(buf.length() < 1)
						buf.append("Nothing ");

					buf.append("assigned");
					m_botAction.sendPrivateMessage(name, buf.toString());
				}catch(Exception e)
				{
					m_botAction.sendPrivateMessage(name, "Error: Bad syntax. use !assign help.");
				}
			}
			else
				m_botAction.sendPrivateMessage(name, "Can't assign while game in progress.");

		}
	}

	/**
	 * Command: !set
	 * Parameters: [teams=<#>] [board=<#>] [cslock=<on/off>]
	 * Modifies current game settings
	 */
	public void C_set(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(message.equalsIgnoreCase("help"))
			{
				String[] s = new String[]{
					"Change game settings - number of teams, board or cap ship locking.",
					"Use \"teams=\" \"cslock=\" and/or \"board=\" after !set"};

				m_botAction.privateMessageSpam(name, s);
			}
			else if(state == IDLE)
			{
				message = message.toLowerCase() + " ";

				try{
					if(message.indexOf("teams=") != -1)
					{
						int index = message.indexOf("teams=");
						String part = message.substring(index + 6, index + 7);
						teams = Byte.parseByte(part);
						m_botAction.sendPrivateMessage(name, "Teams set to "+ teams);
					}

					if(message.indexOf("board=") != -1)
					{
						int index = message.indexOf("board=");
						String part = message.substring(index + 6, index + 7);
						board = Byte.parseByte(part);
						m_botAction.sendPrivateMessage(name, "Board set to "+ board);
					}
					if(message.indexOf("cslock=") != -1)
					{
						int index = message.indexOf("cslock=");
						String part = message.substring(index + 7, index + 10);
						if(part.startsWith("on"))
							lockCapShips = true;
						else if(part.startsWith("off"))
							lockCapShips = false;
						else
							throw new Exception();

						m_botAction.sendPrivateMessage(name, "Cap ship locking "+ part);
					}
				}catch(Exception e)
				{
					m_botAction.sendPrivateMessage(name, "Error: Bad syntax. use !set help.");
					Tools.printStackTrace(e);
				}
			}
			else
				m_botAction.sendPrivateMessage(name, "Can't change settings while game in progress.");
		}
	}

	/**
	 * Command: !status
	 * Parameters:
	 * Displays current status
	 * Staff Only - displays current game settings
	 */
	public void C_status(String name, String message)
	{
		if(state == IDLE)
		{
			m_botAction.sendPrivateMessage(name, "Nothing special going on.");
			if(oplist.isZH(name))
				m_botAction.sendPrivateMessage(name, "Setup:  Teams="+ teams +" Board="+ board
												+" Cap Ship Locking="+ lockCapShips);
		}
		else
		{
			StringBuffer buf = new StringBuffer("Playing game: ");
			buf.append(teams);
			buf.append(" Team");
			if(teams > 1)
				buf.append("s");
			buf.append(" in Board #");
			buf.append(board);
			buf.append(". Status: ");
			m_botAction.sendPrivateMessage(name, buf.toString());

			String[] msg = getTeamShipCount();
			for(int x = 0; x < msg.length; x++)
				m_botAction.sendPrivateMessage(name, msg[x]);
		}
	}

	/**
	 * Command: !start
	 * Parameters:
	 * Once teams have been created and ships assigned, this will begin the game
	 */
	public void C_start(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(state == IDLE)
			{
				m_botAction.sendPrivateMessage(name, "Starting.");
				pregame();
			}
			else
				m_botAction.sendPrivateMessage(name, "Chill dude, there's already one going!");
		}
	}

	/**
	 * Command: !stop
	 * Parameters:
	 * Stops a currently running game.
	 */
	public void C_stop(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(state != IDLE)
			{
				m_botAction.sendArenaMessage("Game Stopped.");
				postgame();
			}
			else
				m_botAction.sendPrivateMessage(name, "No game in progress, dude!");
		}
	}

	/**
	 * Command: !quit
	 * Parameters:
	 * If a player wants to spec, but doesn't have enough energy, they can use this
	 */
	public void C_quit(String name, String message)
	{
		m_botAction.spec(name);
		m_botAction.spec(name);
	}

	/**
	 * Command: !night
	 * Parameters: <on/off>
	 * Turns night mode on or off
	 */
	public void C_night(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(message.equalsIgnoreCase("off"))
			{
				m_botAction.sendUnfilteredPublicMessage("*objset"+nightObjectsOff());
				night = false;
			}
			else if(message.equalsIgnoreCase("on"))
			{
				night = true;
				m_botAction.sendUnfilteredPublicMessage("*objset"+nightObjectsOn(hour));
			}

			m_botAction.sendPrivateMessage(name,"Night mode: "+night);
		}
	}

	/**
	 * Command: !sethour
	 * Parameters: <hour (0-23)>
	 * Sets the night mode hour to <hour>, updates lvz
	 */
	public void C_setHour(String name, String message)
	{
		if(!oplist.isER(name))
		{
			byte hr;
			try{
				hr = Byte.parseByte(message);
			}catch(NumberFormatException e)
			{
				hr = -1;
			}
			if(hr < 0 || hr > 23)
				m_botAction.sendPrivateMessage(name,"Please specify an hour between 0 and 23");
			else
				m_botAction.sendUnfilteredPublicMessage("*objset"+nightObjectsOff() + nightObjectsOn(hour = hr));
		}
	}

	/**
	 * Command: !scheck
	 * Parameters:
	 * Manual check of all ships in game, shouldn't be needed most of the time
	 */
	public void C_scheck(String name, String message)
	{
		if(oplist.isER(name))
		{
			if(state == ACTIVE)
			{
				m_botAction.sendPrivateMessage(name, "Re-checking teams still in game...");
				try{
					updateShipCount();
				}catch(Exception e)
				{
					m_botAction.sendPrivateMessage(name, "Uh oh, this shouldn't happen :p");
				}
				checkForLosers();
			}
		}
	}

	/********************************/
	/*		Battleship Functions	*/
	/********************************/

	/**
	 * Makes random teams, and warps them to safety areas
	 * @param howmany how many teams to make
	 */
	private void makeTeams(int howmany)
	{
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
			m_botAction.setFreq(name, current);
			current++;
		}
	}

	/**
	 * Sets up everything before a main game starts
	 */
	private void pregame()
	{
		StringBuffer buf = new StringBuffer("Initializing Battleship Game: ");

		buf.append(teams);
		buf.append(" Team");
		if(teams > 1)
			buf.append("s");
		buf.append(" in Board #");
		buf.append(board);

		m_botAction.sendArenaMessage(buf.toString());

		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X=495");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y=752");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius=1");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-X=528");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Y=752");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Radius=1");

		m_botAction.sendArenaMessage("Spawn Points established. Ship Distribution: ");

		notify = new CapshipNotify();

		String[] teamStatus = getTeamShipCount();
		for(int x = 0; x < teamStatus.length; x++)
			m_botAction.sendArenaMessage(teamStatus[x]);

		for(int x = 0; x <= teams; x++)
		{
			if(x % 2 == 0) 	//even freq
				m_botAction.warpFreqToLocation(x, 495, 752);
			else 			//odd freq
				m_botAction.warpFreqToLocation(x, 528, 752);
		}

		m_botAction.sendArenaMessage("Game will begin in 10 seconds.");
		state = ACTIVE;
		startWarp = new StartWarp();

		m_botAction.scheduleTask(startWarp,10000);
		m_botAction.scheduleTaskAtFixedRate(notify,2000,300000);
	}

	/**
	 * Cleans up everything after a main game ends
	 */
	private void postgame()
	{
		startWarp.cancel();
		notify.cancel();
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X=357");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y=199");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius=10");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-X=667");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Y=199");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Radius=10");
		m_botAction.shipResetAll();
		m_botAction.warpAllRandomly();

		state = IDLE;
		ships = null;
	}

	/**
	 * Gets all of the ships currently playing ordered by team and ship #
	 * @return a String[] of ships in game
	 */
	private String[] getTeamShipCount()
	{
		String[] s;
		try{
		 	s = new String[teams];

			if(ships == null) //only in pregame
				updateShipCount();

			for(int x = 0; x < s.length; x++)
			{
				StringBuffer buf = new StringBuffer("Team "+ x +": ");
				buf.append("#1: ");
				buf.append(ships[x][0]);
				buf.append(", #2: ");
				buf.append(ships[x][1]);
				buf.append(", #3: ");
				buf.append(ships[x][2]);
				buf.append(", #4: ");
				buf.append(ships[x][3]);
				buf.append(", #5: ");
				buf.append(ships[x][4]);
				buf.append(", #6: ");
				buf.append(ships[x][5]);
				buf.append(", #7: ");
				buf.append(ships[x][6]);
				buf.append(", #8: ");
				buf.append(ships[x][7]);
				s[x] = buf.toString();
			}
		}catch(Exception e)
		{
			s = new String[]{"Error - Teams not properly configured."};
		}

		return s;
	}

	/**
	 * Checks ships of all playing players and updates the recorded numbers
	 * Also will add capital ships to the notifier
	 */
	private void updateShipCount() throws Exception
	{
		ships = new byte[teams][8];
		Iterator it = m_botAction.getPlayingPlayerIterator();
		while(it.hasNext())
		{
			Player p = (Player)it.next();
			ships[p.getFrequency()][p.getShipType() -1]++;
			if(p.getShipType() >= FRIGATE)
				notify.add(p.getPlayerName(), p.getFrequency());
		}
	}

	/**
	 * Checks all teams for being eliminated and
	 *
	 */
	private void checkForLosers()
	{
		for(int x = 0; x < teams; x++)
			if(!checkTeam(x))
				endGame(x);
	}

	/**
	 * Checks a team to see if all of its capital ships have been destroyed
	 * @param team the team to check
	 * @return whether the team has any capital ships remaining
	 */
	private boolean checkTeam(int team)
	{
		try{
			int left = ships[team][3] + ships[team][4] + ships[team][5] +
					   ships[team][6] + ships[team][7];

			return (left > 0);
		}catch(Exception e)
		{
			return false;
		}
	}

	/**
	 * Spectate all players on a given frequency
	 * @param freq the frequency to spec
	 */
	private void specFreq(int freq)
	{
		Iterator it = m_botAction.getPlayingPlayerIterator();
		while(it.hasNext())
		{
			Player p = (Player)it.next();
			if(p.getFrequency() == freq)
			{
				int id = p.getPlayerID();
				m_botAction.spec(id);
				m_botAction.spec(id);
				m_botAction.setFreq(id, freq);
			}
		}
	}

	/**
	 * Retrieve the dimensions of the given board
	 * @param board the board to get dimensions for
	 * @return a short[] containing the board's dimensions
	 */
	private final short[] boardDimensions(byte board)
	{
		short[] dims = new short[4];
		switch(board)
		{
			case 1: //(189, 357), (834, 676)
				dims[X] = 189;
				dims[Y] = 357;
				dims[HEIGHT] = 319;
				dims[WIDTH] = 645;
			break;

			case 2: //(287, 769), (738, 995)
				dims[X] = 287;
				dims[Y] = 769;
				dims[HEIGHT] = 226;
				dims[WIDTH] = 451;
			break;

			case 3: //(10, 815), (243, 949)
				dims[X] = 10;
				dims[Y] = 815;
				dims[HEIGHT] = 101;
				dims[WIDTH] = 233;
			break;

			case 4: //(804, 804), (961, 882)
				dims[X] = 804;
				dims[Y] = 804;
				dims[HEIGHT] = 78;
				dims[WIDTH] = 157;
			break;

			case 5: //(832, 936), (933, 984)
				dims[X] = 832;
				dims[Y] = 936;
				dims[HEIGHT] = 48;
				dims[WIDTH] = 101;
			break;

			default: //default to board 2
				dims[X] = 287;
				dims[Y] = 769;
				dims[HEIGHT] = 226;
				dims[WIDTH] = 451;
		}

		return dims;
	}

	/**
	 * Calculates starting warp points for up to 4 teams in a given board
	 * @param dims the dimensions of the board to calculate on
	 * @return an int[][] containing the warp points
	 */
	private int[][] standardWarp(short[] dims)
	{
		int hCenter = (dims[WIDTH] / 2) + dims[X];
		int vCenter = (dims[HEIGHT] / 2) + dims[Y];

		int[][] points = new int[4][2];

		//changed x offset from 9 to 4
		points[0][X] = dims[X] + 4;
		points[0][Y] = vCenter;

		//changed y offset from 8 to 4
		points[1][X] = hCenter;
		points[1][Y] = dims[Y] + 4;

		points[2][X] = dims[X] + (dims[WIDTH] - 4);
		points[2][Y] = vCenter;

		points[3][X] = hCenter;
		points[3][Y] = dims[Y] + (dims[HEIGHT] - 4);

		return points;
	}

	/**
	 * WARNING: Does not yet produce the desired results, use at your own risk
	 * Calculates starting warp points for more than 4 teams in a given board
	 * @param dims the dimensions of the board to calculate on
	 * @return an int[][] containing the warp points
	 */
	private int[][] specialWarp(short[] dims)
	{
		//Distribution:
		//Left, Right, Top, Bottom
		byte[] distrib = new byte[4];
		byte cur = 0;
		int space;

		for(int i = 0; i < teams; i++)
		{
			if(i > 3)
				i = 0;
			distrib[i]++;
		}

		int[][] points = new int[teams][2];

		//Left
		space = dims[HEIGHT] / distrib[0];
		for(int i = 0; i < distrib[0]; i++)
		{
			points[i][X] = dims[X] + 9;
			points[i][Y] = dims[Y] + (i * space);
		}
		cur = distrib[0];

		//Right
		space = dims[HEIGHT] / distrib[1];
		for(int i = 0; i < distrib[1]; i++)
		{
			points[i + cur][X] = dims[X] + (dims[WIDTH] - 9);
			points[i + cur][Y] = dims[Y] + (i * space);
		}
		cur += distrib[1];

		//Top
		space = dims[WIDTH] / distrib[2];
		for(int i = 0; i < distrib[2]; i++)
		{
			points[i + cur][X] = dims[X] + (i * space);
			points[i + cur][Y] = dims[Y] + 8;
		}
		cur += distrib[2];

		//Bottom
		space = dims[WIDTH] / distrib[2];
		for(int i = 0; i < distrib[2]; i++)
		{
			points[i + cur][X] = dims[X] + (i * space);
			points[i + cur][Y] = dims[Y] + (dims[HEIGHT] - 8);
		}

		return points;
	}

	/**
	 * Assigns all playing players to ships. Done randomly through each team, will
	 * assign 1 of each ship 4-8, the next 3 as ship 2, next 3 as ship 1, and the rest
	 * as ship 3.
	 */
	private void randomShips()
	{
		StringBag[] plist = new StringBag[teams];
		for(int x = 0; x < plist.length; x++)
			plist[x] = new StringBag();

		//stick all of the players in randomizer
		Iterator i = m_botAction.getPlayingPlayerIterator();
		while(i.hasNext())
		{
			Player p = (Player)i.next();
			plist[p.getFrequency()].add(p.getPlayerName());
		}

		for(int x = 0; x < teams; x++) //Set up 5 players in ships
		{
			int index = 4;
			for(int z = 0, s = plist[x].size(); z < s; z++)
			{
				//get a random player
				Player p = m_botAction.getPlayer(plist[x].grabAndRemove());

				if(index < 9) //first 4
					m_botAction.setShip(p.getPlayerName(),index);

				else if(index < 12)//next 3
					m_botAction.setShip(p.getPlayerName(),2);
				else if(index < 15)//next 3
					m_botAction.setShip(p.getPlayerName(),1);
				else
					m_botAction.setShip(p.getPlayerName(),3);
				index++;
			}
		}
	}

	/**
	 * Determines the number of teams left in the game
	 * @return how many teams still playing
	 */
	private byte getTeamsLeft()
	{
		byte count = 0;
		for(int x = 0; x < teams; x++)
			if(checkTeam(x))
				count++;
		return count;
	}

	/**
	 * Removes a frequency from the game, checks for a winning frequency
	 * @param freq the team to remove from the game
	 */
	private void endGame(int freq)
	{
		specFreq(freq);
		m_botAction.sendArenaMessage("All of Team "+ freq +"'s ships have been sunk!",13);
		if(getTeamsLeft() <= 1)
		{
			int team = -1;
			for(int x = 0; x < teams; x++)
				if(checkTeam(x))
				{
					team = x;
					break;
				}

			m_botAction.sendArenaMessage("Team "+ team +" wins!!!!", 5);
			postgame();
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
    	m_cmd.handleEvent(event);
	}

	/**
	 * Event: PlayerDeath
	 * (In-Game) Checks for death of capital ships, announces, adjusts team data, checks for win
	 */
	public void handleEvent(PlayerDeath event)
	{
		if(state == ACTIVE)
		{
			int ship = m_botAction.getPlayer(event.getKilleeID()).getShipType(); //Get the ship # of the player that died
			String name = m_botAction.getPlayerName(event.getKilleeID()); //Get dead player's name
			String killer = m_botAction.getPlayerName(event.getKillerID()); // Get killer's name
			int team = m_botAction.getPlayer(event.getKilleeID()).getFrequency(); //Get dead player's team #
			switch(ship)
			{
				case MINESWEEPER:
					m_botAction.sendArenaMessage("Team "+ team + " just lost a Minesweeper! ("+ name +", killed by "+ killer +")",19);
					m_botAction.setShip(name, 3);
					notify.remove(name);
				break;

				case SUB:
					m_botAction.sendArenaMessage("Team "+ team + " just lost a Submarine! ("+ name +", killed by "+ killer +")",19);
					m_botAction.setShip(name, 3);
					notify.remove(name);
				break;

				case FRIGATE:
					m_botAction.sendArenaMessage("Team "+ team + " just lost a Frigate! ("+ name +", killed by "+ killer +")",19);
					m_botAction.setShip(name, 3);
					notify.remove(name);
				break;

				case BATTLESHIP:
					m_botAction.sendArenaMessage("Team "+ team + " just lost a BATTLESHIP! ("+ name +", killed by "+ killer +")",19);
					m_botAction.setShip(name, 3);
					notify.remove(name);
				break;

				case CARRIER:
					m_botAction.sendArenaMessage("Team "+ team + " just lost an AIRCRAFT CARRIER! ("+ name +", killed by "+ killer +")",19);
					m_botAction.setShip(name, 3);
					notify.remove(name);
				break;
			}

			try{
				updateShipCount(); //update remaining players
			}catch(Exception e)
			{
				m_botAction.sendArenaMessage("Error - Teams not properly configured.");
			}
			checkForLosers();
		}
	}

	/**
	 * Event: PlayerEntered
	 * Displays current night mode lvz to player
	 */
	public void handleEvent(PlayerEntered event)
	{
		if(night)
			showObjects(event.getPlayerID());

		if(state == ACTIVE)
				{
					try{
						updateShipCount(); //update remaining players
					}catch(Exception e)
					{
						m_botAction.sendArenaMessage("Error - Teams not properly configured.");
					}
					checkForLosers();
		}
	}

	/**
	 * Event: PlayerLeft
	 * If playing a game, will update the ship count
	 */
	public void handleEvent(PlayerLeft event)
	{
		if(state == ACTIVE)
		{
			try{
				updateShipCount(); //update remaining players
			}catch(Exception e)
			{
				m_botAction.sendArenaMessage("Error - Teams not properly configured.");
			}
			checkForLosers();
		}
	}

	/**
	 * Event: FrequencyShipChange
	 * If playing, updates the ship count
	 * If cap ship locking is on, will prevent players from switching to a capital ship
	 */
	public void handleEvent(FrequencyShipChange event)
	{
		if(state == ACTIVE)
		{
			if(lockCapShips)
				if(event.getShipType() > 3)
					m_botAction.setShip(event.getPlayerID(), 3);

			try{
				updateShipCount(); //update remaining players
			}catch(Exception e)
			{
				m_botAction.sendArenaMessage("Error - Teams not properly configured.");
			}
			checkForLosers();
		}
	}

	/**
	 * Event: TurretEvent
	 * (Attach): If not allowed, warps back
	 * (Detach): warps back
	 */
	public void handleEvent(TurretEvent event)
	{
		int turret = event.getAttacherID();
		int freq = m_botAction.getPlayer(turret).getFrequency();
		String tname = m_botAction.getPlayerName(turret);
		int tShip = m_botAction.getPlayer(turret).getShipType();
		int bShip = 0;

		int boat = event.getAttacheeID();
		if(m_botAction.getPlayer(boat) != null)
		{
			bShip = m_botAction.getPlayer(boat).getShipType();

			switch(tShip)
			{
				case GUN:
				if(bShip == CARRIER) //Tries to attach to Carrier
					{
						m_botAction.sendPrivateMessage(turret,"Only Planes (3) can attach to Carriers (8).");
						m_botAction.setShip(turret,3);
					}
				break;
				case CANNON:
				if(bShip == CARRIER) //Tries to attach to Carrier
					{
						m_botAction.sendPrivateMessage(turret,"Only Planes (3) can attach to Carriers (8).");
						m_botAction.setShip(turret,3);
					}
				break;
				case PLANE:
					if(bShip != CARRIER) //Tries to attach to Frigate/Battleship
					{
						m_botAction.setShip(turret,1);
						m_botAction.sendPrivateMessage(turret,"Only Guns (1) and Cannons (2) can attach to Frigates (6) and Battleships (7).");
					}
				break;
				default:
					m_botAction.setFreq(turret,100);
					m_botAction.setFreq(turret,freq);
					m_botAction.sendPrivateMessage(turret,"You are not allowed to attach.");
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

	/********************************/
	/*		   	 TimerTasks			*/
	/********************************/

	/**
	 * TimerTask: NightUpdate
	 * If night mode is on, will progress to the next hour
	 */
	private class NightUpdate extends TimerTask
	{
		public void run()
		{
			if(night)
				refresh();
		}
	}

	/**
	 * TimerTask: StartWarp
	 * Warps all players to their starting locations at beginning of game
	 */
	private class StartWarp extends TimerTask
	{
		public void run()
		{
			short[] dims = boardDimensions(board);
			int[][] points;

			//Special can handle more than 4, but standard is faster
			if(teams <= 4)
				points = standardWarp(dims);
			else
				points = specialWarp(dims); //NOTE: DOESN'T WORK YET

			for(int i = 0; i < teams; i++)
			{
				int x = points[i][X];
				int y = points[i][Y];
				m_botAction.warpFreqToLocation(i,x,y);
			}
			m_botAction.sendArenaMessage("Game Begin!!!",104);
		}
	}

	/**
	 * TimerTask: CapshipNotify
	 * Notifies players which ships they can attach to on their team
	 */
	private class CapshipNotify extends TimerTask
	{
		private Vector[] capships = new Vector[teams];
		private boolean ready = false;
		public void run()
		{
			if(!ready)
				init();

			for(int x = 0; x < capships.length; x++)
			{
				StringBuffer bships = new StringBuffer("Your Team's Battleships:");
				StringBuffer carriers = new StringBuffer(" Carriers:");
				Player p = null;
				for(int y = 0; y < capships[x].size(); y++)
				{
					p = m_botAction.getPlayer((String)capships[x].get(y));
					if(p.getShipType() == FRIGATE || p.getShipType() == BATTLESHIP)
						bships.append(" "+ p.getPlayerName() +",");
					else if(p.getShipType() == CARRIER)
						carriers.append(" "+ p.getPlayerName() +",");
				}
				if(bships.toString().endsWith(","))
					bships.deleteCharAt(bships.length() - 1);
				if(carriers.toString().endsWith(","))
					carriers.deleteCharAt(carriers.length() - 1);
				if(p != null)
					m_botAction.sendOpposingTeamMessage((int)p.getPlayerID(), bships.toString() + carriers.toString(),0);
			}
		}

		public void remove(String name)
		{
			if(!ready)
				init();
			for(int x = 0; x < capships.length; x++)
				capships[x].remove(name);
		}

		public void add(String name, int freq)
		{
			if(!ready)
				init();
			if(!capships[freq].contains(name))
				capships[freq].add(name);
		}

		public void init()
		{
			for(int x = 0; x < capships.length; x++)
				capships[x] = new Vector();
			ready = true;
		}
	}
}