package twcore.bots.multibot.bship;

import twcore.core.*;
import twcore.misc.multibot.*;
import twcore.misc.tempset.*;
import java.util.*;

/**
 * Battleship Bot
 *
 * @Author D1st0rt
 * @Version 3.0
 */

public class bship extends MultiModule implements TSChangeListener
{
	//Class Objects
	private CommandInterpreter m_cmd;
	private StartWarp startWarp;
	private CapshipNotify notify;
	private TempSettingsManager m_tsm;
	private BSTeam[] _teams;

	//Night Mode
	private boolean night;

	//Game settings
	private byte state;
	private static final byte IDLE = 0, ACTIVE = 1;
	//private boolean lockCapShips;
	private int[][] points;

	//Ships
	public static final byte SPEC = 0, GUN = 1, CANNON = 2, PLANE = 3, MINESWEEPER = 4,
	SUB = 5, FRIGATE = 6, BATTLESHIP = 7, CARRIER = 8, ALL = 9, PLAYING = 10;

	//Geometry
	private static final byte X = 0, Y = 1, HEIGHT = 2, WIDTH = 3;

	private NightUpdate timeMode = new NightUpdate();

	/********************************/
	/*			  Setup				*/
	/********************************/

	public void init()
	{
		m_botAction.setReliableKills(1);

		//Commands
		m_cmd = new CommandInterpreter(m_botAction);
		registerCommands();
		m_tsm = new TempSettingsManager(m_botAction, m_cmd, OperatorList.ER_LEVEL);
		registerSettings();

		//Initial Game Settings		
		night = false;
		state = IDLE;		

		//Start Night Mode
		m_botAction.scheduleTaskAtFixedRate(timeMode,1000,60000);
	}


	public boolean isUnloadable()
	{
		return (state != ACTIVE);
	}

	public String[] getModHelpMessage()
	{
		return new String[]
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
		//m_cmd.registerCommand("!set", priv, this, "C_set");
		m_cmd.registerCommand("!start", priv, this, "C_start");
		m_cmd.registerCommand("!stop", priv, this, "C_stop");
		m_cmd.registerCommand("!quit", priv | pub, this, "C_quit");
		m_cmd.registerCommand("!scheck", priv, this, "C_scheck");
		m_cmd.registerCommand("!stathelp", priv | pub, this, "C_stathelp");

	}
	
	private void registerSettings()
	{		
		m_tsm.addSetting(SType.INT, "board", "2");
		m_tsm.addSetting(SType.INT, "teams", "2");
		m_tsm.addSetting(SType.BOOLEAN, "cslock", "false");
		m_tsm.addSetting(SType.INT, "hour", "0");		
		m_tsm.restrictSetting("hour", 0, 23);
		m_tsm.addSetting(SType.INT, "lives", "3");
		
		m_tsm.addTSChangeListener(this);
	}
	
	public void settingChanged(String name, String value)
	{
		if(name.equals("hour"))
			refresh();
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
		 "| -!stathelp      Explain stats        |",
		 "+--------------------------------------+"};
		m_botAction.privateMessageSpam(name,help);
	}

	/**
	 * Command: !go
	 * Parameters: <arena>
	 * Makes the bot change arena to <arena>
	 */
	public void C_go(String name, String message)
	{
		if(opList.isZH(name) && !message.equals(""))
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
		if(opList.isER(name) && !message.equals(""))
			m_botAction.sendPublicMessage(message);
	}

	/**
	 * Command: !die
	 * Parameters
	 * Terminates the bot
	 */
	public void C_die(String name, String message)
	{
		if(opList.isZH(name))
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
		if(opList.isER(name))
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
						makeTeams((Integer)m_tsm.getSetting("teams"));
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
					Tools.printStackTrace(e);
					m_botAction.sendPrivateMessage(name, "Error: Bad syntax. use !assign help.");
				}
			}
			else
				m_botAction.sendPrivateMessage(name, "Can't assign while game in progress.");

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
		int teams = (Integer)m_tsm.getSetting("teams");
		int board = (Integer)m_tsm.getSetting("board");
		boolean lockCapShips = (Boolean)m_tsm.getSetting("cslock");
		
		if(state == IDLE)
		{
			m_botAction.sendPrivateMessage(name, "Nothing special going on.");
			if(opList.isZH(name))
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
		if(opList.isER(name))
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
		if(opList.isER(name))
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
		if(opList.isER(name))
		{
			if(message.equalsIgnoreCase("off"))
			{
				m_botAction.sendUnfilteredPublicMessage("*objset"+nightObjectsOff());
				night = false;
			}
			else if(message.equalsIgnoreCase("on"))
			{
				night = true;
				m_botAction.sendUnfilteredPublicMessage("*objset"+nightObjectsOn((Integer)m_tsm.getSetting("hour")));
			}

			m_botAction.sendPrivateMessage(name,"Night mode: "+night);
		}
	}	

	/**
	 * Command: !scheck
	 * Parameters:
	 * Manual check of all ships in game, shouldn't be needed most of the time
	 */
	public void C_scheck(String name, String message)
	{
		if(opList.isER(name))
		{
			if(state == ACTIVE)
			{
				m_botAction.sendPrivateMessage(name, "Re-checking teams still in game...");				
				checkForLosers();
			}
		}
	}
	
	/**
	 * Command: !stathelp
	 * Parameters:
	 * Explains statistic abbreviations
	 */
	public void C_stathelp(String name, String message)
	{
		String[] msg = new String[]{
				"ShpsPlyd: All ships the player has been in this game",
				"Kls     : Total kills by the player during the game",
				"Dths    : Total deaths by the player during the game",
				"SKls    : Kills the player got on capital ships",
				"TKls    : Kills the player got on ship turrets",
				"PKls    : Kills the player got on planes",
				"Atts    : Times the player attached to a capital ship",
				"TaT     : Times the player was attached to",
				"Rating  : (5*SKls) + (2*TKls) + PKls + TaT - (1*Dths or 3*Dths if capship)"};
		m_botAction.privateMessageSpam(name, msg);
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
		int teams = (Integer)m_tsm.getSetting("teams");
		int board = (Integer)m_tsm.getSetting("board");
		
		_teams = new BSTeam[teams];
		for(int x = 0; x < _teams.length; x++)
			_teams[x] = new BSTeam(x);
		
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
		m_tsm.setLocked(true);
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
		
		displayStats();

		state = IDLE;
		m_tsm.setLocked(false);		
	}

	/**
	 * Gets all of the ships currently playing ordered by team and ship #
	 * @return a String[] of ships in game
	 */
	private String[] getTeamShipCount()
	{
		String[] s;
		try{
		 	s = new String[_teams.length];
			for(int x = 0; x < s.length; x++)
			{
				StringBuffer buf = new StringBuffer("Team "+ x +": ");
				byte[] ships = _teams[x].getShipCount();
				buf.append("#1: ");
				buf.append(ships[0]);
				buf.append(", #2: ");
				buf.append(ships[1]);
				buf.append(", #3: ");
				buf.append(ships[2]);
				buf.append(", #4: ");
				buf.append(ships[3]);
				buf.append(", #5: ");
				buf.append(ships[4]);
				buf.append(", #6: ");
				buf.append(ships[5]);
				buf.append(", #7: ");
				buf.append(ships[6]);
				buf.append(", #8: ");
				buf.append(ships[7]);
				s[x] = buf.toString();
			}
		}catch(Exception e)
		{
			s = new String[]{"Error - Teams not properly configured."};
		}

		return s;
	}
	
	private void displayStats()
	{
		for(int x = 0; x < _teams.length; x++)
		{
			BSPlayer[] players = _teams[x].getPlayers();
			String[] msg = new String[5 + players.length];
			msg[0] = "+-----------------------------+";
			msg[1] = "| END GAME STATS: TEAM "+ x +"      |";
			msg[2] = "+--------------------+--------+------+------+------+------+------+------+------+------+";
			msg[3] = "|Name                |ShpsPlyd|Rating| Kls  | Dths | SKls | TKls | PKls | Atts | TaT  |";
			for(int y = 0; y < players.length; y++)
			{
				BSPlayer p = players[y];
				StringBuffer buf = new StringBuffer("|");
				buf.append(Tools.formatString(p.toString(),20));
				buf.append("|");
				buf.append(p.ships);
				buf.append("|");
				buf.append(rightAlign(""+ p.rating, 6));
				buf.append("|");
				buf.append(rightAlign(""+ (p.cskills + p.tkills + p.pkills), 6));
				buf.append("|");
				buf.append(rightAlign("" + p.deaths, 6));
				buf.append("|");
				buf.append(rightAlign(""+ p.cskills, 6));
				buf.append("|");
				buf.append(rightAlign(""+ p.tkills, 6));
				buf.append("|");
				buf.append(rightAlign(""+ p.pkills, 6));
				buf.append("|");
				buf.append(rightAlign(""+ p.takeoffs, 6));
				buf.append("|");
				buf.append(rightAlign(""+ p.tacount, 6));
				buf.append("|");
				msg[4 + y] = buf.toString();
			}
			msg[msg.length - 1] = msg[2];
			
			for(int y = 0; y < msg.length; y++)
				m_botAction.sendArenaMessage(msg[y]);
		}		    		    
	}
	
	private String rightAlign( String fragment, int length)
	{        
        if(fragment.length() > length)
            fragment = fragment.substring(0,length-1);
        else {
            for(int i=fragment.length();i<length;i++)
                fragment = " "+ fragment ;
        }
        return fragment;
    }	

	/**
	 * Checks all teams for being eliminated and
	 *
	 */
	private void checkForLosers()
	{
		for(int x = 0; x < _teams.length; x++)
			if(_teams[x].isOut())
				removeTeam(x);
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
		int teams = (Integer)m_tsm.getSetting("teams");
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
		int teams = (Integer)m_tsm.getSetting("teams");
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
		for(int x = 0; x < _teams.length; x++)
			if(!_teams[x].isOut())
				count++;
		return count;
	}

	/**
	 * Removes a frequency from the game, checks for a winning frequency
	 * @param freq the team to remove from the game
	 */
	private void removeTeam(int freq)
	{
		specFreq(freq);
		m_botAction.sendArenaMessage("All of Team "+ freq +"'s ships have been sunk!",13);
		if(getTeamsLeft() <= 1)
		{
			int team = -1;
			for(int x = 0; x < _teams.length; x++)
				if(!_teams[x].isOut())
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
		int hour = (Integer)m_tsm.getSetting("hour");
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
		int hour = (Integer)m_tsm.getSetting("hour");
		if(hour > -1)
		m_botAction.sendUnfilteredPrivateMessage(playerID,"*objset"+ nightObjectsOff() + nightObjectsOn(hour));
	}

	/**
	 * Produces a string to turn off the night mode objects
	 * @return an objset formatted string to turn off lvz
	 */
	private String nightObjectsOff()
	{
		String objset = " -"+ (100 + (Integer)m_tsm.getSetting("hour")) +", ";

		int id = objectID((Integer)m_tsm.getSetting("hour"));
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
	 * Registers all of the events to be used by the bot with the core
	 */
	 public void requestEvents(EventRequester events)
	 {
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.TURRET_EVENT);
		events.request(EventRequester.PLAYER_DEATH);
		events.request(EventRequester.PLAYER_ENTERED);
		events.request(EventRequester.PLAYER_LEFT);
		events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
		events.request(EventRequester.FREQUENCY_CHANGE);
	 }

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
			byte ship = m_botAction.getPlayer(event.getKilleeID()).getShipType(); //Get the ship # of the player that died
			String name = m_botAction.getPlayerName(event.getKilleeID()); //Get dead player's name
			String killer = m_botAction.getPlayerName(event.getKillerID()); // Get killer's name
			short kteam = m_botAction.getPlayer(event.getKilleeID()).getFrequency(); //Get dead player's team #
			short dteam = m_botAction.getPlayer(event.getKilleeID()).getFrequency(); //Get killer's team #
			if(_teams[dteam].playerDeath(name))
				m_botAction.setShip(name, 3);
			_teams[kteam].playerKill(name, ship);
			switch(ship)
			{
				case MINESWEEPER:
					m_botAction.sendArenaMessage("Team "+ dteam + " just lost a Minesweeper! ("+ name +", killed by "+ killer +")",19);
					m_botAction.scheduleTask(new CapshipRespawn(name, dteam), 3500);
				break;

				case SUB:
					m_botAction.sendArenaMessage("Team "+ dteam + " just lost a Submarine! ("+ name +", killed by "+ killer +")",19);
					m_botAction.scheduleTask(new CapshipRespawn(name, dteam), 3500);
				break;

				case FRIGATE:
					m_botAction.sendArenaMessage("Team "+ dteam + " just lost a Frigate! ("+ name +", killed by "+ killer +")",19);
					m_botAction.scheduleTask(new CapshipRespawn(name, dteam), 3500);
				break;

				case BATTLESHIP:
					m_botAction.sendArenaMessage("Team "+ dteam + " just lost a BATTLESHIP! ("+ name +", killed by "+ killer +")",19);
					m_botAction.scheduleTask(new CapshipRespawn(name, dteam), 3500);
				break;

				case CARRIER:
					m_botAction.sendArenaMessage("Team "+ dteam + " just lost an AIRCRAFT CARRIER! ("+ name +", killed by "+ killer +")",19);
					m_botAction.scheduleTask(new CapshipRespawn(name, dteam), 3500);
				break;
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
			checkForLosers();		
	}

	/**
	 * Event: PlayerLeft
	 * If playing a game, will update the ship count
	 */
	public void handleEvent(PlayerLeft event)
	{
		if(state == ACTIVE)			
			checkForLosers();		
	}
	
	public void handleEvent(FrequencyChange event)
	{
		Player p = m_botAction.getPlayer(event.getPlayerID());
		String name = p.getPlayerName();
		short freq = event.getFrequency();
		
		for(int x = 0; x < _teams.length; x++)			
			if(x != freq)
				_teams[x].removePlayer(name);		
		
		_teams[freq].setShip(name, p.getShipType());
	}

	/**
	 * Event: FrequencyShipChange
	 * If playing, updates the ship count
	 * If cap ship locking is on, will prevent players from switching to a capital ship
	 */
	public void handleEvent(FrequencyShipChange event)
	{	
		Player p = m_botAction.getPlayer(event.getPlayerID());
		short freq = p.getFrequency();
		byte ship = event.getShipType();
		String name = p.getPlayerName();
		BSPlayer bp = _teams[freq].getPlayer(name);		
		if(state == ACTIVE)
		{			
			if((Boolean)m_tsm.getSetting("cslock"))
			{
				if(event.getShipType() > 3 && bp.ship < 3)
					m_botAction.setShip(event.getPlayerID(), bp.ship);			
				else
					_teams[freq].setShip(name, ship);
			}
			else
				_teams[freq].setShip(name, ship);
				
			checkForLosers();
		}
		else		
			_teams[freq].setShip(name, ship);			
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
			short[] dims = boardDimensions((byte)((Integer)m_tsm.getSetting("board")).intValue());
			

			int teams = _teams.length;
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
	
	private class CapshipRespawn extends TimerTask
	{
		private String _name;
		private int _freq;
		public CapshipRespawn(String name, int freq)
		{
			_name = name;
			_freq = freq;
		}
		
		public void run()
		{
			int x = points[_freq][X];
			int y = points[_freq][Y];
			m_botAction.warpTo(_name, x, y);
		}
	}

	/**
	 * TimerTask: CapshipNotify
	 * Notifies players which ships they can attach to on their team
	 */
	private class CapshipNotify extends TimerTask
	{		
		public void run()
		{
			for(int x = 0; x < _teams.length; x++)
			{
				StringBuffer bships = new StringBuffer("Your Team's Battleships:");
				StringBuffer carriers = new StringBuffer(" Carriers:");				
				BSPlayer[] players = _teams[x].getPlayers(); 
				for(int y = 0; y < players.length; y++)
				{
					if(players[x].ship == FRIGATE || players[x].ship == BATTLESHIP)					
						bships.append(" "+ players[x] +",");
					else if(players[x].ship == CARRIER)
						carriers.append(" "+ players[x] +",");
				}
				if(bships.toString().endsWith(","))
					bships.deleteCharAt(bships.length() - 1);
				if(carriers.toString().endsWith(","))
					carriers.deleteCharAt(carriers.length() - 1);				
				m_botAction.sendOpposingTeamMessageByFrequency(x, bships.toString() + carriers.toString(),0);
			}
		}		
	}
}