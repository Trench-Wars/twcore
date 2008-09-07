package twcore.bots.multibot.bship;

import static twcore.core.EventRequester.FREQUENCY_CHANGE;
import static twcore.core.EventRequester.FREQUENCY_SHIP_CHANGE;
import static twcore.core.EventRequester.PLAYER_DEATH;
import static twcore.core.EventRequester.PLAYER_ENTERED;
import static twcore.core.EventRequester.PLAYER_LEFT;
import static twcore.core.EventRequester.PLAYER_POSITION;
import static twcore.core.EventRequester.TURRET_EVENT;

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.util.ModuleEventRequester;
import twcore.core.OperatorList;
import twcore.core.command.CommandInterpreter;
import twcore.core.command.TSChangeListener;
import twcore.core.command.TempSettingsManager;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.TurretEvent;
import twcore.core.game.Player;
import twcore.core.util.StringBag;
import twcore.core.util.Tools;

/**
 * Battleship Bot
 *
 * This bot regulates the specific attach requirements needed in the battleship
 * arena, manipulates the night mode lvz objects, and hosts battleship main
 * game events. Extremely flexible in the way games can be set up on multiple
 * boards and with practically any team-ship arrangement.
 *
 * Update 12/23/07 10:39am - Fixed all raw type references. -Pio
 *
 * Check http://d1st0rt.sscentral.com for latest releases
 *
 * @Author D1st0rt
 * @version 07.01.14
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
	private int hour;
	private NightUpdate timeMode;

	// Game Data //
	private byte state;
	private boolean cslock, usedb;
	private static final byte IDLE = 0, PREGAME = 1, ACTIVE = 2;
	private int[][] points;
	private BSTeam[] m_teams;
	private int cslimit, maxlives, gameId;

	// TimerTasks //
	private StartWarp startWarp;
	private CapshipNotify notify;
//	private CapshipRespawn respawn;

	// Ships //
	static final byte SPEC = 0, GUN = 1, CANNON = 2, PLANE = 3, MINESWEEPER = 4,
	SUB = 5, FRIGATE = 6, BATTLESHIP = 7, CARRIER = 8, ALL = 9, PLAYING = 10;

	// Geometry //
	private static final byte X = 0, Y = 1, HEIGHT = 2, WIDTH = 3;

	// SQL //
	public static final String dbConn = "bship";

	//////////////////////////////////
	/*       Message Strings        */
	//////////////////////////////////

	/** rGame: Rules for playing in a special (hosted) game */
	private final String[] rGame = {
		"+-------------------Rules: Special Game-------------------+",
		"|Each team gets a set number of capital ships, each with  |",
		"|a set number of lives. When a ship dies it loses a life  |",
		"|and subtracts one from the total remaining team lives.   |",
		"|Planes and turrets can enter the game by attaching to a  |",
		"|capital ship. A team is out when they either have no     |",
		"|team lives left or no capital ships in play.             |",
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
		"| Gun(1)/Cannon(2) ------------> Frigate(6)/Battleship(7) |",
		"| Plane(3)         ------------> Carrier(8)               |",
		"+---------------------------------------------------------+"};

	/** aboutMsg: Tells the player information about the bot, used by !about */
	private final String[] aboutMsg = {
		"+-Battleship Bot by D1st0rt--------v3.2-+",
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
		"| -!myfreq         Get back on your team|",
		"+---------------------------------------+"};

	/** assignHelp: Explains how to use !assign */
	private final String[] assignHelp = {
		"You can use this to automatically assign",
		"Random players to teams and ships. Just put",
		"\"team\" and/or \"ship\" after !assign."
	};

	/** statHelp: Explains the statistics used in hosted games, used by !stathelp */
	private final String[] statHelp = {
		"+--------------------------Statistic Abbreviations-------------------------------+",
		"|ShpsPlyd: All ships the player has been in this game                            |",
		"|Kls     : Total kills by the player during the game                             |",
		"|Dths    : Total deaths by the player during the game                            |",
		"|SKls    : Kills the player got on capital ships                                 |",
		"|TKls    : Kills the player got on ship turrets                                  |",
		"|PKls    : Kills the player got on planes                                        |",
		"|Atts    : Times the player attached to a capital ship                           |",
		"|TaT     : Times the player was attached to                                      |",
		"|Rating  : (5*SKls) + (2*TKls) + PKls + (TaT/3) - ((Dths/2) or 4*Dths if capship)|",
		"+--------------------------------------------------------------------------------+"};

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

	/** setHelp: Custom !set help override for the TSM */
	private final String[] setHelp = {
		"Use !set to change game settings",
		"Syntax: !set name1=value name2=value ...",
		"+----------Modifiable Settings----------+",
		"| - hour    <1-23>                      |",
		"|   The simulated game hour (night mode)|",
		"| - board   <1-5>                       |",
		"|   Which box to play the game in       |",
		"| - teams   <1-4>                       |",
		"|   How many teams to play in game      |",
		"| - lives   <1-10>                      |",
		"|   How many lives capital ships get    |",
		"| - cslock  <on/off>                    |",
		"|   Prevents switching to capital ship  |",
		"| - cslimit <1-10>                      |",
		"|   How many capital ships a team gets  |",
		"+---------------------------------------+"
	};

	/** Fields for sql insert of a player's postgame score */
	private static final String[] scoreFields = {
		"gameId", "playerId", "freq", "rating", "shipsPlayed", "sKills",
		"tKills", "pKills", "deaths", "attaches", "attached"
	};

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
		m_tsm = m_botAction.getTSM();
		m_tsm.setOperatorLevel(OperatorList.ER_LEVEL);
		m_tsm.registerCommands(m_cmd);
		registerSettings();

		//Initial Game Settings
		night = false;
		state = IDLE;
		cslock = false;
		cslimit = 5;
		maxlives = 3;
		usedb = true;
		gameId = -1;

		int teams = (Integer)m_tsm.getSetting("teams");
		m_teams = new BSTeam[teams];
		for(int x = 0; x < m_teams.length; x++)
			m_teams[x] = new BSTeam(x, maxlives);

		short[] dims = boardDimensions((byte)((Integer)m_tsm.getSetting("board")).intValue());
		points = standardWarp(dims);

		//Start Night Mode
		timeMode = new NightUpdate();
		m_botAction.scheduleTaskAtFixedRate(timeMode, 1000, 60000);
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
	 * This method is called when the module is unloaded
	 */
	public void cancel() 
	{
		timeMode.cancel();
		m_tsm.removeTSChangeListener(this);
		m_tsm.removeSetting("board");
		m_tsm.removeSetting("teams");
		m_tsm.removeSetting("cslock");
		m_tsm.removeSetting("hour");
		m_tsm.removeSetting("lives");
		m_tsm.removeSetting("cslimit");
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
		m_tsm.addSetting("board", 2);
		m_tsm.restrictSetting("board", 1, 5);
		m_tsm.addSetting("teams", 2);
		m_tsm.restrictSetting("teams", 1, 4);
		m_tsm.addSetting("cslock", false);
		m_tsm.addSetting("hour", 0);
		m_tsm.restrictSetting("hour", 0, 23);
		m_tsm.addSetting("lives", 3);
		m_tsm.restrictSetting("lives", 1, 10);
		m_tsm.addSetting("cslimit", 5);
		m_tsm.restrictSetting("cslimit", 1, 10);
		m_tsm.addSetting("usedb", true);

		m_tsm.addTSChangeListener(this);
		m_tsm.setCustomHelp(setHelp);
	}

	/**
	 * Called when a setting is altered in-game through the command interface
	 * @param name the name of the setting that was changed
	 * @param value the value the setting was changed to
	 */
	public void settingChanged(String name, Object value)
	{
		if(name.equals("hour"))
		{
			m_botAction.sendUnfilteredPublicMessage("*objset "+ nightObjectsOff());
			hour = (Integer)value;
			m_botAction.sendUnfilteredPublicMessage("*objset "+ nightObjectsOn(hour));
		}
		else if(name.equals("board"))
		{
			int board = (Integer)value;
			points = standardWarp(boardDimensions((byte)board));
		}
		else if(name.equals("cslock"))
			cslock = (Boolean)value;
		else if(name.equals("cslimit"))
			cslimit = (Integer)value;
		else if(name.equals("lives"))
			maxlives = (Integer)value;
		else if(name.equals("usedb"))
			usedb = (Boolean)value;
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
		m_cmd.registerCommand("!about", priv | pub, this, "c_About");
		m_cmd.registerCommand("!help", priv | pub, this, "c_Help");
		m_cmd.registerCommand("!say", priv | rem, this, "c_Say");

		//Night Mode Commands
		m_cmd.registerCommand("!night", priv, this, "c_Night");

		//Battleship Game Commands
		m_cmd.registerCommand("!rules", priv | pub, this, "c_Rules");
		m_cmd.registerCommand("!status", priv | pub, this, "c_Status");
		m_cmd.registerCommand("!assign", priv, this, "c_Assign");
		m_cmd.registerCommand("!start", priv, this, "c_Start");
		m_cmd.registerCommand("!stop", priv, this, "c_Stop");
		m_cmd.registerCommand("!quit", priv | pub, this, "c_Quit");
		m_cmd.registerCommand("!scheck", priv, this, "c_Scheck");
		m_cmd.registerCommand("!stathelp", priv | pub, this, "c_Stathelp");
		m_cmd.registerCommand("!myfreq", priv, this, "c_MyFreq");
	}


	/**
	 * Command: !about
	 * Parameters:
	 * What this bot does
	 */
	public void c_About(String name, String message)
	{
		m_botAction.privateMessageSpam(name, this.aboutMsg);
	}

	/**
	 * Command: !help
	 * Parameters:
	 * Displays list of commands available
	 */
	public void c_Help(String name, String message)
	{
		m_botAction.privateMessageSpam(name, this.helpMsg);
	}

	/**
	 * Command: !say
	 * Parameters: <text>
	 * Makes the bot say <text>
	 */
	public void c_Say(String name, String message)
	{
		if(opList.isER(name) && !message.equals(""))
			m_botAction.sendPublicMessage(message);
	}

	/**
	 * Command: !rules
	 * Parameters:
	 * Displays the rules for the current game mode
	 */
	public void c_Rules(String name, String message)
	{
		m_botAction.privateMessageSpam(name, (state == ACTIVE ? this.rGame : this.rNormal));
		m_botAction.privateMessageSpam(name, this.rAttach);
	}

	/**
	 * Command: !assign
	 * Parameters: [team] [ship]
	 * Assigns players to random teams and ships based on game settings
	 */
	public void c_Assign(String name, String message)
	{
		if(opList.isER(name))
		{
			if(message.equalsIgnoreCase("help"))
					m_botAction.privateMessageSpam(name, assignHelp);
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
	public void c_Status(String name, String message)
	{
		int teams = (Integer)m_tsm.getSetting("teams");
		int board = (Integer)m_tsm.getSetting("board");

		if(state == IDLE)
		{
			m_botAction.sendPrivateMessage(name, "Nothing special going on.");

			//For staff, display current main game configuration
			if(opList.isBot(name))
				m_botAction.sendPrivateMessage(name, "Setup:  Teams="+ teams +" Board="+ board
						+" Lives="+ maxlives +" Cap Ship Locking="+ cslock + " Limit="+ cslimit);
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
	public void c_Start(String name, String message)
	{
		if(opList.isER(name))
		{
			if(state == IDLE)
			{
				m_botAction.sendPrivateMessage(name, "Starting.");
				pregame(name);
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
	public void c_Stop(String name, String message)
	{
		if(opList.isER(name))
		{
			if(state == ACTIVE)
			{
				m_botAction.sendArenaMessage("Game Stopped.");
				if(usedb)
				{
					StringBuffer query = new StringBuffer("UPDATE games SET winner=-1");
					query.append(" WHERE id=");
					query.append(gameId);
					m_botAction.SQLHighPriorityBackgroundQuery(dbConn, null, query.toString());

					query = new StringBuffer("INSERT INTO elims");
					query.append("(gameId,team,reason,time) VALUES (");
					query.append(gameId); query.append(",");
					query.append(-1); query.append(",\"");
					query.append("Game stopped by "+ name); query.append("\",");
					query.append("now())");
					m_botAction.SQLBackgroundQuery(dbConn, null, query.toString());
				}
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
	public void c_Quit(String name, String message)
	{
		m_botAction.spec(name);
		m_botAction.spec(name);
	}

	/**
	 * Command: !night
	 * Parameters: <on/off>
	 * Turns night mode on or off
	 */
	public void c_Night(String name, String message)
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

			m_botAction.sendPrivateMessage(name,"Night mode is "+ (night ? "on":"off"));
		}
	}

	/**
	 * Command: !scheck
	 * Parameters:
	 * Manual check of all ships in game, shouldn't be needed most of the time
	 */
	public void c_Scheck(String name, String message)
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
	public void c_Stathelp(String name, String message)
	{
		m_botAction.privateMessageSpam(name, this.statHelp);
	}

	/**
	 * Command: !myfreq
	 * Parameters: <#>
	 * Allows a player to rejoin a team they have been on earlier in a game.
	 */
	public void c_MyFreq(String name, String message)
	{
		if(state == ACTIVE)
		{
			try{
				int team = Integer.parseInt(message);
				BSPlayer player = m_teams[team].getPlayer(name);
				if(player != null)
				{
					if(m_botAction.getPlayer(name).getFrequency() != team)
					{
						m_botAction.spectatePlayer(name);
						m_botAction.spectatePlayer(name);
						m_botAction.setFreq(name, team);
						m_botAction.sendPrivateMessage(name, "Setting you back on team "+ team);
						m_botAction.setShip(name, PLANE);
					}
					else
					{
						m_botAction.sendPrivateMessage(name, "You are already on that team.");
					}
				}
				else
				{
					m_botAction.sendPrivateMessage(name, "You have not been on that team.");
				}
			}
			catch(Exception e)
			{
				for(int x = 0; x < m_teams.length; x++)
				{
					if(m_teams[x].getPlayer(name) != null)
					{
						String msg = "You have been recorded on team ";
						msg += x;
						msg += ". Use !myfreq ";
						msg += x;
						msg += " to get back.";
						m_botAction.sendPrivateMessage(name, msg);
					}
				}
			}
		}
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
		Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
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
	private void pregame(String host)
	{
		try{

		//Reset turf flag to keep one team from getting way more points than the other.
		m_botAction.resetFlagGame();

		//Prevent configuration changes to be made during a game
		m_tsm.setLocked("teams", true);
		m_tsm.setLocked("board", true);
		m_tsm.setLocked("lives", true);
		m_tsm.setLocked("usedb", true);

		//Retrieve values of required settings
		int teams = (Integer)m_tsm.getSetting("teams");
		int board = (Integer)m_tsm.getSetting("board");

		//Initialize main game data storage
		m_teams = new BSTeam[teams];
		for(int x = 0; x < m_teams.length; x++)
			m_teams[x] = new BSTeam(x, maxlives);

		//Report game configuration
		StringBuffer buf = new StringBuffer("Initializing Battleship Game: ");

		buf.append(teams);
		buf.append(" Team");
		if(teams > 1)
			buf.append("s");
		buf.append(" in Board #");
		buf.append(board);
		buf.append(" with ");
		buf.append(cslimit);
		buf.append(" Capital Ships allowed each.");


		m_botAction.sendArenaMessage(buf.toString());

		//Change spawn points to safety zones
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X=495");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y=752");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius=1");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-X=528");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Y=752");
		m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Radius=1");

		//Update main game data with current ships and initial lives
		Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
		while(i.hasNext())
		{
			Player p = (Player)i.next();
			int ship = p.getShipType();
			if(ship > 3 && m_teams[p.getFrequency()].getCapShipCount() >= cslimit)
			{
				String msg = "Sorry, your team already has the maximum number of capital ships.";
				m_botAction.sendPrivateMessage(p.getPlayerID(), msg);
				m_botAction.setShip(p.getPlayerID(), PLANE);
				m_teams[p.getFrequency()].setShip(p.getPlayerName(), PLANE);
			}
			else
			{
				m_teams[p.getFrequency()].setShip(p.getPlayerName(), p.getShipType());
			}
		}

		//Display current breakdown of ships by frequency
		m_botAction.sendArenaMessage("Spawn Points established. Ship Distribution: ");

		String[] teamStatus = getTeamShipCount();
		for(int x = 0; x < teamStatus.length; x++)
		{
			m_botAction.sendArenaMessage(teamStatus[x]);
			m_botAction.sendArenaMessage("Cap Ship Count: "+ m_teams[x].getCapShipCount());
		}

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
		state = PREGAME;

		if(usedb && m_botAction.SQLisOperational())
		{
			StringBuffer query = new StringBuffer(
				"INSERT INTO games(host,startTime,board,teams,cslimit,lives,cslock) VALUES (");
			query.append("\"");
			query.append(host);
			query.append("\"");
			query.append(",");
			query.append("now()");
			query.append(",");
			query.append(board);
			query.append(",");
			query.append(teams);
			query.append(",");
			query.append(cslimit);
			query.append(",");
			query.append(maxlives);
			query.append(",");
			query.append(cslock);
			query.append(")");

			try {
				m_botAction.SQLQuery(dbConn, query.toString());
				ResultSet s = m_botAction.SQLQuery(dbConn, "SELECT MAX(id) AS gameId FROM games");
				if (s.next())
				{
					gameId = s.getInt("gameId");
				}
				else
					gameId = -1;

				m_botAction.SQLClose(s);

			} catch (Exception e) {
				m_tsm.setLocked("usedb", false);
				m_tsm.setValue("usedb", "false");
				m_tsm.setLocked("usedb", true);
				gameId = -1;
			};
		}

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
		if(usedb)
		{
			m_botAction.SQLBackgroundQuery(dbConn, null, "update games set endTime=now() where id="+gameId);
		}

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
		if(usedb)
			m_botAction.sendArenaMessage("Complete Game Summary at http://bship.slopeout.com/games/"+ gameId);

		//Update internal game state
		state = IDLE;
		gameId = -1;

		//Allow changes to be made to main game configuration
		m_tsm.setLocked("teams", false);
		m_tsm.setLocked("board", false);
		m_tsm.setLocked("lives", false);
		if(m_botAction.SQLisOperational())
			m_tsm.setLocked("usedb", false);
	}

	/**
	 * Gets all of the ships currently playing ordered by team and ship #
	 * @return a String[] of ships in game
	 */
	private String[] getTeamShipCount()
	{
		String[] s;
		try{
		 	s = new String[m_teams.length];
			for(int x = 0; x < s.length; x++)
			{
				StringBuffer buf = new StringBuffer("Team "+ x +": ");
				byte[] ships = m_teams[x].getShipCount();
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
				buf.append(", Team Lives Left: ");
				buf.append((maxlives * cslimit) - m_teams[x].getCapShipDeaths());
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

		for(int x = 0; x < m_teams.length; x++)
		{
			BSPlayer[] players = m_teams[x].getPlayers();
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
				buf.append(rightAlign(""+ p.attaches, 6));
				buf.append("|");
				msg[5 + y] = buf.toString();

				if(usedb)
				{
					String[] vals = new String[]{
						String.valueOf(gameId), String.valueOf(p.sqlId),
						String.valueOf(x), String.valueOf(p.rating),
						p.shipsPlayed(), String.valueOf(p.cskills),
						String.valueOf(p.tkills), String.valueOf(p.pkills),
						String.valueOf(p.deaths), String.valueOf(p.takeoffs),
						String.valueOf(p.attaches)
					};
					m_botAction.SQLBackgroundInsertInto(dbConn, null, scoreFields, vals);
				}
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
        else
        {
        	StringBuffer buf = new StringBuffer(length);
            for(int i=fragment.length();i<length;i++)
                buf.append(" ");
            buf.append(fragment);
            fragment = buf.toString();
        }
        return fragment;
    }

	/**
	 * Checks and removes eliminated teams
	 */
	private void checkForLosers()
	{
		String reason = "";
		int totalLives = cslimit * maxlives;
		for(int x = 0; x < m_teams.length; x++)
		{
			int code = m_teams[x].isOut(totalLives);
			if(code > 0)
			{
				if(code == 1)
					reason = "(No team lives left)";
				else if(code == 2)
					reason = "(No ships left)";

				m_botAction.sendArenaMessage("Team "+ x +" is out! "+ reason);

				if(usedb)
				{
					StringBuffer query = new StringBuffer("UPDATE games SET winner=-1");
					query.append(" WHERE id=");
					query.append(gameId);
					m_botAction.SQLHighPriorityBackgroundQuery(dbConn, null, query.toString());

					query = new StringBuffer("INSERT INTO elims");
					query.append("(gameId,team,reason,time) VALUES (");
					query.append(gameId); query.append(",");
					query.append(x); query.append(",\"");
					query.append(reason); query.append("\",");
					query.append("now())");
					m_botAction.SQLBackgroundQuery(dbConn, null, query.toString());
				}
				removeTeam(x);
			}
		}

		if(getTeamsLeft() <= 0)
		{
			m_botAction.sendArenaMessage("All teams appear to have been eliminated. Game ends in a draw.");
			if(usedb)
			{
				StringBuffer query = new StringBuffer("INSERT INTO elims");
				query.append("(gameId,team,reason,time) VALUES (");
				query.append(gameId); query.append(",");
				query.append(-1); query.append(",\"");
				query.append("All teams eliminated. Draw."); query.append("\",");
				query.append("now())");
				m_botAction.SQLBackgroundQuery(dbConn, null, query.toString());
			}
		}

		if(getTeamsLeft() <= 1)
			postgame();

	}

	/**
	 * Put all players on a given frequency into spectator mode
	 * @param freq the frequency to spec
	 */
	private void specFreq(int freq)
	{
		Iterator<Player> it = m_botAction.getFreqPlayerIterator(freq);
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

			default: //default to board 2 (287, 769), (738, 995)
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
	 * Assigns all playing players to ships. Done randomly through each team,
	 * will assign players to each capital ship in reverse order from carrier to
	 * minesweeper until the capital ship limit is reached. The remaining
	 * players will be assigned as turrets and planes.
	 */
	private void randomShips()
	{
		int teams = (Integer)m_tsm.getSetting("teams");
		int cslimit = (Integer)m_tsm.getSetting("cslimit");
		StringBag[] plist = new StringBag[teams];
		for(int x = 0; x < plist.length; x++)
			plist[x] = new StringBag();

		//stick all of the players in randomizer
		Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
		while(i.hasNext())
		{
			Player p = (Player)i.next();
			plist[p.getFrequency()].add(p.getPlayerName());
		}

		for(int x = 0; x < teams; x++)
		{
			int count = 0;

			while(!plist[x].isEmpty())
			{
				String name = plist[x].grabAndRemove();
				if(name != null)
				{
					if(count < cslimit)
					{
						//put players in capital ships up to the limit
						m_botAction.setShip(name, 8 - (count % 5));
					}
					else
					{
						//put everyone else in turrets and planes
						m_botAction.setShip(name, count % 3);
					}
					count++;
				}
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
		for(int x = 0; x < m_teams.length; x++)
			if(m_teams[x].isOut(maxlives * cslimit) == 0)
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

		if(getTeamsLeft() <= 1)
		{
			int team = -1;
			for(int x = 0; x < m_teams.length; x++)
				if(m_teams[x].isOut(maxlives * cslimit) == 0)
				{
					team = x;
					break;
				}

			if(team != -1)
			{
				m_botAction.sendArenaMessage("Team "+ team +" wins!!!!", 5);
				StringBuffer query = new StringBuffer("UPDATE games SET winner=");
				query.append(team);
				query.append(" WHERE id=");
				query.append(gameId);

				m_botAction.SQLHighPriorityBackgroundQuery(dbConn, null, query.toString());
			}
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
		if(night)
		{
			m_botAction.sendUnfilteredPublicMessage("*objset"+ nightObjectsOff());

			hour++;
			if(hour >= 24)
				hour = 0;

			m_botAction.sendUnfilteredPublicMessage("*objset"+ nightObjectsOn(hour));
			m_tsm.setValue("hour",""+ hour);
		}
	}

	/**
	 * Used to display the night mode lvzs to players upon entering
	 */
	private void showObjects(int playerID)
	{
		int hour = (Integer)m_tsm.getSetting("hour");
		if(night)
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


	//////////////////////////////////
	/*		   	  Events			*/
	//////////////////////////////////

	/**
	 * Registers all of the events to be used by the bot with the core
	 */
	 public void requestEvents(ModuleEventRequester events)
	 {
		events.request(this, TURRET_EVENT);
		events.request(this, PLAYER_DEATH);
		events.request(this, PLAYER_ENTERED);
		events.request(this, PLAYER_LEFT);
		events.request(this, FREQUENCY_SHIP_CHANGE);
		events.request(this, FREQUENCY_CHANGE);
		events.request(this, PLAYER_POSITION);
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

			if(kteam >= 0 && kteam < m_teams.length)
			{
				m_teams[kteam].playerKill(killer, ship);

				//change capital ships into planes when they run out of lives
				int livesLeft = m_teams[dteam].playerDeath(name);

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
					if(livesLeft < 1)
					{
						buf.append(" SHIP ELIMINATED!!!");
						m_botAction.setShip(name, 3);
					}
					else
					{
						buf.append(" "+ livesLeft);
						int teamMax = cslimit * maxlives;
						int teamLeft = teamMax - m_teams[dteam].getCapShipDeaths();
						buf.append(" ("+ teamLeft);
						buf.append(" Team)");
						if(livesLeft > 1)
							buf.append(" lives remaining.");
						else
							buf.append(" life remaining.");
					}


					m_botAction.sendArenaMessage(buf.toString(), 19);

					if(usedb)
					{
						BSPlayer k = m_teams[kteam].getPlayer(killer);
						BSPlayer d = m_teams[dteam].getPlayer(name);

						StringBuffer query = new StringBuffer("INSERT INTO kills");
						query.append("(gameId,time,killerId,killedId,killerShip,killedShip,killerTeam,killedTeam,eliminated) VALUES (");
						query.append(gameId); query.append(",");
						query.append("now()"); query.append(",");
						query.append(k.sqlId); query.append(",");
						query.append(d.sqlId); query.append(",");
						query.append(k.ship); query.append(",");
						query.append(d.ship); query.append(",");
						query.append(kteam); query.append(",");
						query.append(dteam); query.append(",");
						query.append(livesLeft < 1 ? 1 : 0);
						query.append(")");

						m_botAction.SQLBackgroundQuery(dbConn, null, query.toString());
					}
				}
			}
			else
			{
				m_botAction.sendPrivateMessage(name, "Please get on one of the playing teams");
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
			if(event.getShipType() != SPEC && event.getTeam() < m_teams.length)
				m_teams[event.getTeam()].setShip(event.getPlayerName(), event.getShipType());

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
			Player leaving = m_botAction.getPlayer(event.getPlayerID());
			if(leaving.getFrequency() < m_teams.length)
			{
				m_teams[leaving.getFrequency()].setShip(leaving.getPlayerName(), SPEC);
				checkForLosers();
			}
		}
	}

	/**
	 * Event: FrequencyChange
	 * If playing a game, will update the ship count
	 */
	public void handleEvent(FrequencyChange event)
	{
		if(state >= PREGAME)
		{
			Player p = m_botAction.getPlayer(event.getPlayerID());
			String name = p.getPlayerName();
			short freq = event.getFrequency();
			byte ship = p.getShipType();

			if(p.getShipType() != SPEC)
			{
				//Get the old BSPlayer object for this player if it exists
				BSPlayer bp = null;
				for(BSTeam team : m_teams)
				{
					if(team.getPlayer(name) != null)
					{
						bp = team.getPlayer(name);
						break;
					}
				}

				//The player exists and is not locked (normal condition)
				if(bp != null && !bp.locked)
				{
					m_teams[freq].setShip(name, ship);
				}
				//The player doesn't exist
				else if(bp == null)
				{
					m_teams[freq].setShip(name, ship); //add player to the game
				}
				//The player is locked
				else
				{
					//unlock them and stick them back in their old ship
					bp.locked = false;
				}
			}
			else
			{
				//Make sure teams know when a player is no longer in a ship
				for(BSTeam team : m_teams)
				{
					if(team.getPlayer(name) != null)
						team.setShip(name, SPEC);
				}

			}

			if(state == ACTIVE)
			{
				checkForLosers();
			}
		}
	}

	/**
	 * Event: FrequencyShipChange
	 * If playing a game, updates the ship count
	 * If cap ship locking is on, will prevent players from switching to a capital ship
	 */
	public void handleEvent(FrequencyShipChange event)
	{
		if(state >= PREGAME)
		{
			Player p = m_botAction.getPlayer(event.getPlayerID());
			short freq = p.getFrequency();
			byte ship = event.getShipType();
			String name = p.getPlayerName();

			int capShipsLeft = 0;
			if(freq < m_teams.length)
				capShipsLeft = (maxlives * cslimit) - m_teams[freq].getCapShipDeaths();

			//I don't care what spectators are doing
			if(p.getShipType() != SPEC)
			{

				//Get the old BSPlayer object for this player if it exists
				BSPlayer bp = null;
				for(int x = 0; x < m_teams.length; x++)
					if((bp = m_teams[x].getPlayer(name)) != null)
						break;

				byte oldship = (bp == null ? PLANE : bp.ship);

				// The player exists and is locked
				if(bp != null && bp.locked)
				{
					//unlock them and stick them back in their old ship
					bp.locked = false;
					m_botAction.setShip(name, oldship);
				}
				// The player is in a capital ship and is not locked
				else if(event.getShipType() > 3)
				{
					// cap ships are locked
					if(cslock)
					{
						m_botAction.setShip(name, oldship);
						m_botAction.sendPrivateMessage(name, "Capital ships are currently locked.");
					}
					// cap ships not locked
					else
					{
						// check team limit
						if(m_teams[freq].getCapShipCount() >= capShipsLeft)
						{
							m_botAction.sendPrivateMessage(name, "Your team has reached its capital ship limit.");
							m_botAction.setShip(name, oldship);
						}
						// check player lives
						else if(bp != null && bp.lives < 1)
						{
							m_botAction.sendPrivateMessage(name, "You have no more lives as a capital ship.");
							m_botAction.setShip(name, oldship);
						}
						else
						{
							m_teams[freq].setShip(name, ship); //add player to the game
							int x = points[p.getFrequency()][X];
							int y = points[p.getFrequency()][Y];
							if(state == ACTIVE)
								m_botAction.warpTo(event.getPlayerID(), x, y); //warp them
						}
					}
				}
				//The player is in a non-capital ship and is not locked (normal condition)
				else
					m_teams[freq].setShip(name, ship); //add player to the game
			}
			else //Now I care about what spectators are doing
			{
				//Make sure teams know when a player is no longer in a ship
				for(BSTeam team : m_teams)
				{
					if(team.getPlayer(name) != null)
						team.setShip(name, SPEC);
				}
			}

			if(state == ACTIVE)
			{
				checkForLosers();
			}
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
			String bname = m_botAction.getPlayerName(boat);
			bShip = m_botAction.getPlayer(boat).getShipType();
			if(state == ACTIVE)
				m_teams[freq].lockPlayer(tname, true);

			switch(tShip)
			{
				case GUN: //fall through
				case CANNON:
					if(bShip == CARRIER) //Tries to attach to Carrier
					{
						m_botAction.sendPrivateMessage(turret,"Only Planes (3) can attach to Carriers (8).");
						m_botAction.setShip(turret, PLANE);
					}
					else if(state == ACTIVE)
					{
						//Updates statistics for valid attach
						m_teams[freq].attach(bname, tname);
					}
				break;
				case PLANE:
					if(bShip != CARRIER) //Tries to attach to Frigate/Battleship
					{
						m_botAction.setShip(turret, GUN);
						m_botAction.sendPrivateMessage(turret,"Only Guns (1) and Cannons (2) can attach to Frigates (6) and Battleships (7).");
					}
					else if(state == ACTIVE)
					{
						//Updates statistics for valid attach
						m_teams[freq].attach(bname, tname);
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

	/**
	 * Event: PlayerPosition
	 * If active, warp any capital ship currently on a safety tile to the board
	 * where the game is going on.
	 */
	public void handleEvent(PlayerPosition event)
	{
		if(state == ACTIVE)
		{
			Player p = m_botAction.getPlayer(event.getPlayerID());
			if(p.isInSafe())
			{
				BSPlayer bp = m_teams[p.getFrequency()].getPlayer(p.getPlayerName());
				if(bp != null)
				{
					if(bp.ship > 3 && bp.lives > 0)
					{
						int x = points[p.getFrequency()][X];
						int y = points[p.getFrequency()][Y];
						m_botAction.warpTo(event.getPlayerID(), x, y);
					}
				}
				else if(p.getShipType() > 3)
				{
					int x = points[p.getFrequency()][X];
					int y = points[p.getFrequency()][Y];
					m_botAction.warpTo(event.getPlayerID(), x, y);
				}
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
			state = ACTIVE;
		}
	}

	// CapshipRespawn should be unnecessary with the new safety warping
	/**
	 * TimerTask: CapshipRespawn
	 * Respawns a capital ship to the arena after they die
	 *
	private class CapshipRespawn extends TimerTask
	{
		private String _name;
		private int _freq;

		/**
		 * Creates a new CapshipRespawn task to send a capital ship back to
		 * its team's starting location on the game board after they die.
		 * @param name the player's name
		 * @param freq the player's frequency
		 *
		public CapshipRespawn(String name, int freq)
		{
			_name = name;
			_freq = freq;
		}

		/**
	     * Executes this task, warps ship back into game board
	     *
		public void run()
		{
			int x = points[_freq][X];
			int y = points[_freq][Y];
			m_botAction.warpTo(_name, x, y);
		}
	}*/

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
			for(int x = 0; x < m_teams.length; x++)
			{
				StringBuffer bships = new StringBuffer("Your Team's Battleships:");
				StringBuffer carriers = new StringBuffer("Your Team's Carriers:");

				Player targetPlayer = null;

				//Find attachable ships
				Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
				while(i.hasNext())
				{
					Player p = (Player)i.next();
					if(p.getFrequency() == x)
					{
						targetPlayer = p;
						//turret attachable ships
						if(p.getShipType() == FRIGATE || p.getShipType() == BATTLESHIP)
							bships.append(" "+ p +",");
						//plane attachable ships
						else if(p.getShipType() == CARRIER)
							carriers.append(" "+ p +",");
					}
				}

				//remove trailing commas
				if(bships.toString().endsWith(","))
					bships.deleteCharAt(bships.length() - 1);
				if(carriers.toString().endsWith(","))
					carriers.deleteCharAt(carriers.length() - 1);

				//broadcast to team members
				if(targetPlayer != null)
				{
					m_botAction.sendOpposingTeamMessage(targetPlayer.getPlayerID(), bships.toString(), 0);
					m_botAction.sendOpposingTeamMessage(targetPlayer.getPlayerID(), carriers.toString(), 0);
				}
			}
		}
	}
}

