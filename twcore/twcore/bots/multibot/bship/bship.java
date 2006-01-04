package twcore.bots.multibot.bship;

import twcore.core.*;
import twcore.misc.multibot.*;
import twcore.misc.tempset.*;
import java.util.*;
import static twcore.core.EventRequester.*;

/**
 * Battleship Bot
 *
 * This bot is the first I ever made, and I have been constantly
 * making additions and improvements to it to sharpen my skills as a
 * bot developer. I have this available for public download even though
 * I realize that it can really only be used in the battleship arena in
 * Trench because it is here that I have tried to employ my best bot
 * making practices. There are several different things this bot has
 * pioneered, and aspects of this bot can be applied to many other bots.
 * I hope that it can be used as a guide of sorts to new coders as to
 * how they can go about getting things done in their own bots.
 *
 * Check http://d1st0rt.sscentral.com for latest releases
 *
 * @Author D1st0rt
 * @version 06.01.04
 */

public class bship extends MultiModule implements TSChangeListener
{

	//////////////////////////////////
	/*		   Declarations			*/
	//////////////////////////////////

	// Core Objects //
	private CommandInterpreter m_cmd;
	private TempSettingsManager m_tsm;

	// Night Mode //
	private boolean night;
	private NightUpdate timeMode;

	// Game Data //
	private byte state;
	private static final byte IDLE = 0, ACTIVE = 1;
	private int[][] points;
	private BSTeam[] _teams;

	// TimerTasks //
	private StartWarp startWarp;
	private CapshipNotify notify;
	private CapshipRespawn respawn;

	// Ships //
	public static final byte SPEC = 0, GUN = 1, CANNON = 2, PLANE = 3, MINESWEEPER = 4,
	SUB = 5, FRIGATE = 6, BATTLESHIP = 7, CARRIER = 8, ALL = 9, PLAYING = 10;

	// Geometry //
	private static final byte X = 0, Y = 1, HEIGHT = 2, WIDTH = 3;

	//////////////////////////////////
	/*       Message Strings        */
	//////////////////////////////////

	/** rGame: Rules for playing in a special (hosted) game */
	private final String[] rGame = {
		"+-------------------Rules: Special Game-------------------+",
		"|Each team gets one of each of the 5 ships, and 6 turrets.|",
		"|The rest are planes. When a ship dies it can't reenter,  |",
		"|but turrets and planes are free to attach. A team wins   |",
		"|when all enemy ships are destroyed.                      |",
		"+---------------------------------------------------------+"};

	/** rNormal: Rules for playing when not in a special (hosted) game */
	private final String[] rNormal = {
		"+------------------Rules: Normal Game---------------------+",
		"|Get your fleet together and try to control the flag      |",
		"|as much as you can. Remember that turrets (1 and 2)      |",
		"|can't stay detached out of spawn, and planes (3) use F3  |",
		"|to move.                                                 |",
		"+---------------------------------------------------------+"};

	/** rAttach: Restrictions on attaching */
	private final String[] rAttach = {
		"+-----------------Allowed Ship Attaches-------------------+",
		"| AA(1)/Cannon(2) -------------> Frigate(6)/Battleship(7) |",
		"| Plane(3)        -------------> Carrier(8)               |",
		"+---------------------------------------------------------+"};

	/** aboutMsg: Tells the player information about the bot, used by !about */
	private final String[] aboutMsg = {
		"+-Battleship Bot by D1st0rt--------v3.0-+",
		"| -Attach Regulation                    |",
		"| -Night Mode LVZ Automation            |",
		"| -Battleship Games                     |",
		"+---------------------------------------+"};

	/** helpMsg: Tells the player all available commands, used by !help */
	private final String[] helpMsg = {
		"+------------Battleship Bot-------------+",
		"| -!about          What this bot does   |",
		"| -!help           This message         |",
		"| -!quit           Enter spectator mode |",
		"| -!rules          Rules of the game    |",
		"| -!status         What is happening    |",
		"| -!stathelp       Explain stats        |",
		"+---------------------------------------+"};

	/** statHelp: Explains the statistics used in hosted games, used by !stathelp */
	private final String[] statHelp = {
		"+--------------------------Statistic Abbreviations-------------------------+",
		"|ShpsPlyd: All ships the player has been in this game                      |",
		"|Kls     : Total kills by the player during the game                       |",
		"|Dths    : Total deaths by the player during the game                      |",
		"|SKls    : Kills the player got on capital ships                           |",
		"|TKls    : Kills the player got on ship turrets                            |",
		"|PKls    : Kills the player got on planes                                  |",
		"|Atts    : Times the player attached to a capital ship                     |",
		"|TaT     : Times the player was attached to                                |",
		"|Rating  : (5*SKls) + (2*TKls) + PKls + TaT - (1*Dths or 3*Dths if capship)|",
		"+--------------------------------------------------------------------------+"};

	/** modHelp: Displays commands for staff use, used by !help */
	private final String[] modHelp = {
		"+-------------Staff Commands------------+",
		"| -!start          Starts a game        |",
		"| -!stop           Stop a game          |",
		"| -!assign         See <!assign help>   |",
		"| -!set            See <!set help>      |",
		"| -!scheck         Manual game update   |",
		"| -!night <on/off> Toggle night mode    |",
		"+---------------------------------------+"};

	//////////////////////////////////
	/*			  Setup				*/
	//////////////////////////////////

	/**
	 * Initializes all of the bot objects (acts as constructor)
	 */
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

		int teams = (Integer)m_tsm.getSetting("teams");
		_teams = new BSTeam[teams];
		for(int x = 0; x < _teams.length; x++)
			_teams[x] = new BSTeam(x);

		short[] dims = boardDimensions((byte)((Integer)m_tsm.getSetting("board")).intValue());
		points = standardWarp(dims);

		//Start Night Mode
		timeMode = new NightUpdate();
		m_botAction.scheduleTaskAtFixedRate(timeMode,1000,60000);
		m_botAction.sendPublicMessage("Module changed.");
	}

	/**
	 * Determines unloadability of the bot based on game status
	 * @return whether the bot can be unloaded or not
	 */
	public boolean isUnloadable()
	{
		return (state != ACTIVE);
	}

	/**
	 * Gets the help message for moderators
	 * @return the moderator help message
	 */
	public String[] getModHelpMessage()
	{
		return this.modHelp;
	}

	//////////////////////////////////
	/*			  Settings			*/
	//////////////////////////////////

	/**
	 * Establishes all of the settings with the TempSettings Manager
	 */
	private void registerSettings()
	{
		m_tsm.addSetting(SType.INT, "board", "2");
		m_tsm.restrictSetting("board", 0, 5);
		m_tsm.addSetting(SType.INT, "teams", "2");
		m_tsm.restrictSetting("teams", 0, 4);
		m_tsm.addSetting(SType.BOOLEAN, "cslock", "false");
		m_tsm.addSetting(SType.INT, "hour", "0");
		m_tsm.restrictSetting("hour", 0, 23);
		m_tsm.addSetting(SType.INT, "lives", "3");
		m_tsm.restrictSetting("lives", 0, 10);

		m_tsm.addTSChangeListener(this);
	}

	/**
	 * Called when a setting is altered in-game through the command interface
	 * @param name the name of the setting that was changed
	 * @param value the value the setting was changed to
	 */
	public void settingChanged(String name, String value)
	{
		if(name.equals("hour"))
			refresh();
		else if(name.equals("board"))
		{
			int board = Integer.parseInt(value);
			points = standardWarp(boardDimensions((byte)board));
		}
	}


	//////////////////////////////////
	/*			  Commands			*/
	//////////////////////////////////

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
		m_cmd.registerCommand("!say", priv | rem, this, "C_say");

		//Night Mode Commands
		m_cmd.registerCommand("!night", priv, this, "C_night");

		//Battleship Game Commands
		m_cmd.registerCommand("!rules", priv | pub, this, "C_rules");
		m_cmd.registerCommand("!status", priv | pub, this, "C_status");
		m_cmd.registerCommand("!assign", priv, this, "C_assign");
		m_cmd.registerCommand("!start", priv, this, "C_start");
		m_cmd.registerCommand("!stop", priv, this, "C_stop");
		m_cmd.registerCommand("!quit", priv | pub, this, "C_quit");
		m_cmd.registerCommand("!scheck", priv, this, "C_scheck");
		m_cmd.registerCommand("!stathelp", priv | pub, this, "C_stathelp");
	}


	/**
	 * Command: !about
	 * Parameters:
	 * What this bot does
	 */
	public void C_about(String name, String message)
	{
		m_botAction.privateMessageSpam(name, this.aboutMsg);
	}

	/**
	 * Command: !help
	 * Parameters:
	 * Displays list of commands available
	 */
	public void C_help(String name, String message)
	{
		m_botAction.privateMessageSpam(name, this.helpMsg);
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
	 * Command: !rules
	 * Parameters:
	 * Displays the rules for the current game mode
	 */
	public void C_rules(String name, String message)
	{
		m_botAction.privateMessageSpam(name, (state == ACTIVE ? this.rGame : this.rNormal));
		m_botAction.privateMessageSpam(name, this.rAttach);
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
			//only allow op to assign ships/teams when a game is not in progress
			else if(state == IDLE)
			{
				try{
					StringBuffer buf = new StringBuffer();
					message = message.toLowerCase();
					boolean changed = false;

					//check to see if teams are to be assigned
					if(message.indexOf("team") != -1)
					{
						makeTeams((Integer)m_tsm.getSetting("teams"));
						buf.append("Teams ");
						changed = true;
					}

					//check to see if ships are to be assigned
					if(message.indexOf("ship") != -1)
					{
						randomShips();
						buf.append("Ships ");
						changed = true;
					}

					//if they didn't specify anything
					if(buf.length() < 1 || !changed)
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

			//For staff, display current main game configuration
			if(opList.isZH(name))
				m_botAction.sendPrivateMessage(name, "Setup:  Teams="+ teams +" Board="+ board
												+" Cap Ship Locking="+ lockCapShips);
		}
		else
		{
			//Report the number of ships remaining per team
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
		m_botAction.privateMessageSpam(name, this.statHelp);
	}

	//////////////////////////////////
	/*		Battleship Functions	*/
	//////////////////////////////////

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
		try{

		//Prevent configuration changes to be made during a game
		m_tsm.setLocked("teams", true);
		m_tsm.setLocked("board", true);
		m_tsm.setLocked("lives", true);

		//Retrieve values of required settings
		int teams = (Integer)m_tsm.getSetting("teams");
		int board = (Integer)m_tsm.getSetting("board");
		int lives = (Integer)m_tsm.getSetting("lives");

		//Initialize main game data storage
		_teams = new BSTeam[teams];
		for(int x = 0; x < _teams.length; x++)
			_teams[x] = new BSTeam(x);

		//Report game configuration
		StringBuffer buf = new StringBuffer("Initializing Battleship Game: ");

		buf.append(teams);
		buf.append(" Team");
		if(teams > 1)
			buf.append("s");
		buf.append(" in Board #");
		buf.append(board);

		m_botAction.sendArenaMessage(buf.toString());

		//Change spawn points to safety zones
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X=495");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y=752");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius=1");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-X=528");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Y=752");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Radius=1");

		//Update main game data with current ships and initial lives
		Iterator i = m_botAction.getPlayingPlayerIterator();
		while(i.hasNext())
		{
			Player p = (Player)i.next();
			_teams[p.getFrequency()].setShip(p.getPlayerName(), p.getShipType());
			_teams[p.getFrequency()].getPlayer(p.getPlayerName()).lives = (short)lives;
		}

		//Display current breakdown of ships by frequency
		m_botAction.sendArenaMessage("Spawn Points established. Ship Distribution: ");

		String[] teamStatus = getTeamShipCount();
		for(int x = 0; x < teamStatus.length; x++)
			m_botAction.sendArenaMessage(teamStatus[x]);

		//Warp everyone to holding area in preparation for game
		for(int x = 0; x < teams; x++)
		{
			if(x % 2 == 0) 	//even freq
				m_botAction.warpFreqToLocation(x, 495, 752);
			else 			//odd freq
				m_botAction.warpFreqToLocation(x, 528, 752);
		}

		//Update internal game state
		m_botAction.sendArenaMessage("Game will begin in 10 seconds.");
		state = ACTIVE;

		//Reset tasks
		notify = new CapshipNotify();
		startWarp = new StartWarp();

		//Begin tasks
		m_botAction.scheduleTask(startWarp,10000);
		m_botAction.scheduleTaskAtFixedRate(notify,2000,300000);
		}catch(Exception e)
		{
			Tools.printStackTrace(e);
		}
	}

	/**
	 * Cleans up everything after a main game ends
	 */
	private void postgame()
	{
		//Destroy tasks
		startWarp.cancel();
		notify.cancel();

		//Change spawn points and warp everyone back
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X=357");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y=199");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius=10");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-X=667");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Y=199");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Radius=10");
		m_botAction.shipResetAll();
		m_botAction.warpAllRandomly();

		//Show end-game statistics
		displayStats();

		//Update internal game state
		state = IDLE;

		//Allow changes to be made to main game configuration
		m_tsm.setLocked("teams", false);
		m_tsm.setLocked("board", false);
		m_tsm.setLocked("lives", false);
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

	/**
	 * Shows end game statistics for a hosted game
	 */
	private void displayStats()
	{
		for(int x = 0; x < _teams.length; x++)
		{
			BSPlayer[] players = _teams[x].getPlayers();
			String[] msg = new String[6 + players.length];
			msg[0] = "+-----------------------------+";
			msg[1] = "| END GAME STATS: TEAM "+ x +"      |";
			msg[2] = "+--------------------+--------+------+------+------+------+------+------+------+------+";
			msg[3] = "|Name                |ShpsPlyd|Rating| Kls  | Dths | SKls | TKls | PKls | Atts | TaT  |";
			msg[4] = "+--------------------+--------+------+------+------+------+------+------+------+------+";
			for(int y = 0; y < players.length; y++)
			{
				BSPlayer p = players[y];
				StringBuffer buf = new StringBuffer("|");
				buf.append(Tools.formatString(p.toString(),20));
				buf.append("|");
				buf.append(p.shipsPlayed());
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
				msg[5 + y] = buf.toString();
			}
			msg[msg.length - 1] = msg[2];

			for(int y = 0; y < msg.length; y++)
				m_botAction.sendArenaMessage(msg[y]);
		}
	}

	/**
	 * Takes a string and fills the left end of it with spaces up to a specified length
	 * @param fragment the string to pad
	 * @param length the desired length of the final string
	 */
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
	 * Checks and removes eliminated teams
	 */
	private void checkForLosers()
	{
		for(int x = 0; x < _teams.length; x++)
			if(_teams[x].isOut())
				removeTeam(x);
	}

	/**
	 * Put all players on a given frequency into spectator mode
	 * @param freq the frequency to spec
	 */
	private void specFreq(int freq)
	{
		Iterator it = m_botAction.getFreqPlayerIterator(freq);
		while(it.hasNext())
		{
			Player p = (Player)it.next();
			int id = p.getPlayerID();
			m_botAction.spec(id);
			m_botAction.spec(id);
			//Put them back on their team's frequency since they will be on spec freq
			m_botAction.setFreq(id, freq);
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
	 * @deprecated Its not really deprecated but you shouldn't use it
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
					m_botAction.setShip(p.getPlayerName(), CANNON);
				else if(index < 15)//next 3
					m_botAction.setShip(p.getPlayerName(), GUN);
				else
					m_botAction.setShip(p.getPlayerName(), PLANE);
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


	//////////////////////////////////
	/*     Night Mode Functions     */
	//////////////////////////////////

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


	//////////////////////////////////
	/*		   	  Events			*/
	//////////////////////////////////

	/**
	 * Registers all of the events to be used by the bot with the core
	 */
	 public void requestEvents(EventRequester events)
	 {
		events.request(MESSAGE);
		events.request(TURRET_EVENT);
		events.request(PLAYER_DEATH);
		events.request(PLAYER_ENTERED);
		events.request(PLAYER_LEFT);
		events.request(FREQUENCY_SHIP_CHANGE);
		events.request(FREQUENCY_CHANGE);
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
			short kteam = m_botAction.getPlayer(event.getKillerID()).getFrequency(); //Get killer's team #
			short dteam = m_botAction.getPlayer(event.getKilleeID()).getFrequency(); //Get dead player's team #

			_teams[kteam].playerKill(name, ship);

			//change capital ships into planes when they run out of lives
			boolean hasLivesLeft = _teams[dteam].playerDeath(name);
			if(!hasLivesLeft)
				m_botAction.setShip(name, PLANE);

			//no death message or respawn for ships that can attach
			if(ship > PLANE)
			{

				StringBuffer buf = new StringBuffer("Team ");
				buf.append(dteam);
				buf.append(" just lost ");

				switch(ship)
				{
					case MINESWEEPER:
						buf.append("a Minesweeper! (");
					break;

					case SUB:
						buf.append("a Submarine! (");
					break;

					case FRIGATE:
						buf.append("a Frigate! (");
					break;

					case BATTLESHIP:
						buf.append("a BATTLESHIP! (");
					break;

					case CARRIER:
						buf.append("an AIRCRAFT CARRIER! (");
					break;
				}

				buf.append(name);
				buf.append(", killed by ");
				buf.append(killer);
				buf.append(")");

				//respawn them if they have lives left
				if(!hasLivesLeft)
					buf.append(" SHIP ELIMINATED!!!");
				else
					m_botAction.scheduleTask(new CapshipRespawn(name, dteam), 3500);

				m_botAction.sendArenaMessage(buf.toString(), 19);
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
		//activate night mode for the entering player
		if(night)
			showObjects(event.getPlayerID());

		//if they don't come in as a spectator, put them in the game
		if(state == ACTIVE)
		{
			if(event.getShipType() != SPEC && event.getTeam() < _teams.length)
				_teams[event.getTeam()].setShip(event.getPlayerName(), event.getShipType());

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
			checkForLosers();
	}

	/**
	 * Event: FrequencyChange
	 * If playing a game, will update the ship count
	 */
	public void handleEvent(FrequencyChange event)
	{
		if(state == ACTIVE)
		{
			Player p = m_botAction.getPlayer(event.getPlayerID());
			String name = p.getPlayerName();
			short freq = event.getFrequency();
			byte ship = p.getShipType();

			if(p.getShipType() != SPEC)
			{
				//Get the old BSPlayer object for this player if it exists
				BSPlayer bp = null;
				for(int x = 0; x < _teams.length; x++)
					if((bp = _teams[x].getPlayer(name)) != null)
						break;

				//The player exists and is not locked (normal condition)
				if(bp != null && !bp.locked)
				{
					_teams[freq].setShip(name, ship);
				}
				//The player doesn't exist
				else if(bp == null)
				{
					_teams[freq].setShip(name, ship); //add player to the game
				}
				//The player is locked
				else
				{
					//unlock them and stick them back in their old ship
					bp.locked = false;
					m_botAction.setFreq(name, bp.getFreq());
				}
			}

			checkForLosers();
		}
	}

	/**
	 * Event: FrequencyShipChange
	 * If playing a game, updates the ship count
	 * If cap ship locking is on, will prevent players from switching to a capital ship
	 */
	public void handleEvent(FrequencyShipChange event)
	{
		if(state == ACTIVE)
		{
			Player p = m_botAction.getPlayer(event.getPlayerID());
			short freq = p.getFrequency();
			byte ship = event.getShipType();
			String name = p.getPlayerName();

			//I don't care what spectators are doing
			if(p.getShipType() != SPEC)
			{

				//Get the old BSPlayer object for this player if it exists
				BSPlayer bp = null;
				for(int x = 0; x < _teams.length; x++)
					if((bp = _teams[x].getPlayer(name)) != null)
						break;

				//The player exists and is not locked (normal condition)
				if(bp != null && !bp.locked)
				{
					//player switching to capital ship when not allowed
					if(((Boolean)m_tsm.getSetting("cslock") || bp.lives < 1) && event.getShipType() > 3)
						m_botAction.setShip(name, bp.ship); //set them to their old ship
					else
						_teams[freq].setShip(name, ship);
				}
				//The player doesn't exist
				else if(bp == null)
				{
					//They are coming in as a capital ship
					if((Boolean)m_tsm.getSetting("cslock") && event.getShipType() > 3)
						m_botAction.setShip(name, PLANE);
					else
						_teams[freq].setShip(name, ship); //add player to the game
				}
				//The player is locked
				else
				{
					//unlock them and stick them back in their old ship
					bp.locked = false;
					m_botAction.setShip(name, bp.ship);
					m_botAction.setFreq(name, bp.getFreq());
				}
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


		//Keep ships from attaching to the wrong other ship
		if(event.isAttaching())
		{
			bShip = m_botAction.getPlayer(boat).getShipType();
			_teams[freq].lockPlayer(tname, true);

			switch(tShip)
			{
				case GUN: //fall through
				case CANNON:
				if(bShip == CARRIER) //Tries to attach to Carrier
					{
						m_botAction.sendPrivateMessage(turret,"Only Planes (3) can attach to Carriers (8).");
						m_botAction.setShip(turret, PLANE);
					}
				break;
				case PLANE:
					if(bShip != CARRIER) //Tries to attach to Frigate/Battleship
					{
						m_botAction.setShip(turret, GUN);
						m_botAction.sendPrivateMessage(turret,"Only Guns (1) and Cannons (2) can attach to Frigates (6) and Battleships (7).");
					}
				break;
				default:
					m_botAction.setShip(turret, GUN);
					m_botAction.sendPrivateMessage(turret,"You are not allowed to attach.");
				break;
			}
		}
		else //on detach
		{
			switch(tShip)//Warp turrets back to spawn so they don't sit there w/o ship
			{
				case GUN: //fall through
				case CANNON:
					m_botAction.sendUnfilteredPrivateMessage(turret,"*prize #7");
				break;
			}
		}
	}

	//////////////////////////////////
	/*		   	 TimerTasks			*/
	//////////////////////////////////

	/**
	 * TimerTask: NightUpdate
	 * If night mode is on, will progress to the next hour
	 */
	private class NightUpdate extends TimerTask
	{
		/**
	     * Executes this task, changes the night mode graphics
	     */
		public void run()
		{
			if(night)
				refresh(); //proceed to next hour and change graphics
		}
	}

	/**
	 * TimerTask: StartWarp
	 * Warps all players to their starting locations at beginning of game
	 */
	private class StartWarp extends TimerTask
	{
		/**
	     * Executes this task, warps all players to their team's starting location
	     * on the game board.
	     */
		public void run()
		{
			int teams = (Integer)m_tsm.getSetting("teams");

			//Warp everyone to their calculated starting points
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
	 * TimerTask: CapshipRespawn
	 * Respawns a capital ship to the arena after they die
	 */
	private class CapshipRespawn extends TimerTask
	{
		private String _name;
		private int _freq;

		/**
		 * Creates a new CapshipRespawn task to send a capital ship back to
		 * its team's starting location on the game board after they die.
		 * @param name the player's name
		 * @param freq the player's frequency
		 */
		public CapshipRespawn(String name, int freq)
		{
			_name = name;
			_freq = freq;
		}

		/**
	     * Executes this task, warps ship back into game board
	     */
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
		/**
	     * Executes this task, notifies team of attachable ships
	     */
		public void run()
		{
			for(int x = 0; x < _teams.length; x++)
			{
				StringBuffer bships = new StringBuffer("Your Team's Battleships:");
				StringBuffer carriers = new StringBuffer("Your Team's Carriers:");

				//Find attachable ships
				BSPlayer[] players = _teams[x].getPlayers();
				for(int y = 0; y < players.length; y++)
				{
					//turret attachable ships
					if(players[x].ship == FRIGATE || players[x].ship == BATTLESHIP)
						bships.append(" "+ players[x] +",");
					//plane attachable ships
					else if(players[x].ship == CARRIER)
						carriers.append(" "+ players[x] +",");
				}

				//remove trailing commas
				if(bships.toString().endsWith(","))
					bships.deleteCharAt(bships.length() - 1);
				if(carriers.toString().endsWith(","))
					carriers.deleteCharAt(carriers.length() - 1);

				//broadcast to team members
				m_botAction.sendOpposingTeamMessageByFrequency(x, bships.toString());
				m_botAction.sendOpposingTeamMessageByFrequency(x, carriers.toString());
			}
		}
	}
}