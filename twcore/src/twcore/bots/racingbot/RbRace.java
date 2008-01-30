package twcore.bots.racingbot;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.core.events.FlagClaimed;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.Tools;

public class RbRace extends RacingBotExtension {

	HashMap<Integer, Track> trackIDList;
	HashMap<String, Track> trackNameList;
	HashSet<Integer> pitFlags;

	int racing = 0;
	int laps = 0;
	int currentTrack = -1;
	int pitExit[] = new int[2];
	long lastReset = 0;

	boolean settingPits = false;
	boolean updated = true;

	public RbRace() {
		trackIDList = new HashMap<Integer, Track>();
		trackNameList = new HashMap<String, Track>();
		pitFlags = new HashSet<Integer>();
	}

	public void handleEvent( Message event ) {
		try {
			String name = m_botAction.getPlayerName( event.getPlayerID() );
			String message = event.getMessage().toLowerCase();

			if(m_botAction.getOperatorList().isModerator(name) || m_bot.twrcOps.contains(name.toLowerCase()))
			{
				if( message.equals( "yes" ) )
					handleAnswer( true );
				else if( message.equals( "no" ) )
					handleAnswer( false );
				else if( message.startsWith( "!help race" ) )
					displayHelp( name );
				else if( message.equals( "!help" ) )
					m_botAction.sendPrivateMessage( name, "race        - Runs races, host free" );
				else if( message.startsWith( "!loadarena" ) )
					loadArena();
				else if( message.startsWith( "!startrace " ) )
					startRace( message, name );
				else if( message.startsWith( "!tracklist" ) )
					listTracks( name );
				else if(message.startsWith("!setpits"))
				{
					m_botAction.sendPrivateMessage(name, "Grab all the pit flags now.");
					settingPits = true;
				}
				else if(message.startsWith("!pitsset"))
				{
					m_botAction.sendPrivateMessage(name, "Pits are now set.");
					settingPits = false;
				}
				else if(message.startsWith("!clearpits"))
				{
					pitFlags = new HashSet<Integer>();
					m_botAction.sendPrivateMessage(name, "Pit flags cleared.");
				}
				else if( message.startsWith("!raceover"))
					handleDone();
				else if( message.startsWith("!fakeover"))
					handleFake();
			}
			else if(m_botAction.getOperatorList().isER(name))
			{
				if( message.startsWith( "!loadarena" ) )
					loadArena();
				else if( message.startsWith( "!startrace " ) )
					startRace( message, name );
				else if( message.startsWith( "!tracklist" ) )
					listTracks( name );
				else if(message.startsWith("!setpits"))
				{
					m_botAction.sendPrivateMessage(name, "Grab all the pit flags now.");
					settingPits = true;
				}
				else if(message.startsWith("!pitsset"))
				{
					m_botAction.sendPrivateMessage(name, "Pits are now set.");
					settingPits = false;
				}
				else if(message.startsWith("!clearpits"))
				{
					pitFlags = new HashSet<Integer>();
					m_botAction.sendPrivateMessage(name, "Pit flags cleared.");
				}
				else if( message.startsWith("!raceover"))
					handleDone();
			}
			if(message.startsWith("!position "))
			{
				String pieces[] = message.split(" ", 2);
				int pos = getPosition(pieces[1]);
				if(pos == -1)
					m_botAction.sendPrivateMessage(name, "That player was not found.");
				else
				{
					String ending = "th";
					if(pos == 1)
						ending = "st";
					if(pos == 2)
						ending = "nd";
					if(pos == 3)
						ending = "rd";
					m_botAction.sendPrivateMessage(name, pieces[1] + " is currently in " + pos + ending + ".");
				}
			}
			else if(message.startsWith("!position"))
			{
				int pos = getPosition(name);
				if(pos == -1)
					m_botAction.sendPrivateMessage(name, "You were not found.");
				else
				{
					String ending = "th";
					if(pos == 1)
						ending = "st";
					if(pos == 2)
						ending = "nd";
					if(pos == 3)
						ending = "rd";
					m_botAction.sendPrivateMessage(name,  "You are currently in " + pos + ending + ".");
				}
			}
		} catch(Exception e) {}
	}

	public void displayHelp( String name ) {
		if(m_bot.twrcOps.contains(name.toLowerCase()) || m_botAction.getOperatorList().isModerator(name))
		{
			String help[] = {
				"------------- Race Help Menu ------------------------------------",
				"| !loadarena     - loads arena information                      |",
				"| !startrace <#> - starts a race on the track identified by <#> |",
				"| !tracklist     - displays available tracks for this map       |",
				"| !setpits       - starts the process of setting pit flags      |",
				"| !pitsset       - ends pit setting process                     |",
				"| !clearpits     - clears flag ids from pit list                |",
				"| !raceover      - ends the race                                |",
				"----------------------------------------------------------------|"
			};
			m_botAction.privateMessageSpam( name, help );
		}
		else
		{
			String help[] = {
				"------------- Race Help Menu ------------------------------------",
				"| !loadarena     - loads arena information                      |",
				"| !startrace <#> - starts a race on the track identified by <#> |",
				"| !tracklist     - displays available tracks for this map       |",
				"| !setpits       - starts the process of setting pit flags      |",
				"| !pitsset       - ends pit setting process                     |",
				"| !clearpits     - clears flag ids from pit list                |",
				"| !raceover      - ends the race                                |",
				"----------------------------------------------------------------|"
			};
			m_botAction.privateMessageSpam( name, help );
		}
	}

	public void handleAnswer( boolean answer ) {
	}

	public void loadArena() {

		trackIDList.clear();
		trackNameList.clear();

		int arenaId = getArenaID();
		if( arenaId < 0 ) {
			m_botAction.sendPublicMessage( "This arena has not been setup for racing." );
			return;
		}
		try {
			//Get track summary
			ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT * FROM tblRaceTrack WHERE fnRaceID = "+arenaId );
			while( result.next() ) {
				loadTrack( result );

			}
                        m_botAction.SQLClose( result );
		} catch (Exception e) { Tools.printStackTrace(e); }
		m_botAction.sendPublicMessage("Bot ready for use.");
	}

	public void startRace( String message, String name ) {

		if( racing != 0 ) {
			m_botAction.sendPrivateMessage( name, "Unable to start race, a race is already running." );
			return;
		}

		int trackId = -1;
		try { trackId = Integer.parseInt( message.split(" ")[1] ); } catch (Exception e) {}
		if( trackId == -1 ) {
			m_botAction.sendPrivateMessage( name, "Unable to start race, please use !startrace <#>" );
			return;
		}

		if( !trackIDList.containsKey( new Integer( trackId ) ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to start race, race does not exist." );
			return;
		}

		laps = 1;
		try { laps = Integer.parseInt( message.split( " " )[2] ); } catch (Exception e) {}
		if( laps < 1 ) laps = 1;
		if( laps > 25 ) laps = 25;

		racing = 1;
		currentTrack = trackId;
		getTrack( trackId ).reset();

		m_botAction.sendUnfilteredPublicMessage("*prize #5");
		m_botAction.sendArenaMessage( "A "+laps+" lap race is starting at: " + getTrack(trackId).getName() + " in 30 seconds!", 2 );
		m_botAction.sendArenaMessage( "Ships will be locked in 15 seconds." );

		TimerTask lockArena = new TimerTask() {
			public void run() {
				m_botAction.toggleLocked();
				Track thisTrack = getTrack( currentTrack );
				ArrayList<String> zeroLap = new ArrayList<String>();
				Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
				while( it.hasNext() ) {
					Player p = (Player)it.next();
					if( thisTrack.checkShip( p.getShipType() ) )
						m_botAction.setShip( p.getPlayerID(), thisTrack.getShip() );
					zeroLap.add(m_botAction.getPlayerName(p.getPlayerID()).toLowerCase());
				}
				thisTrack.playerPositions.add(zeroLap);
				m_botAction.createRandomTeams( 1 );
				thisTrack.warpPlayers();
				m_botAction.sendArenaMessage("You have 10 seconds to get into a safe!!!", 1);
			}
		};
		m_botAction.scheduleTask( lockArena, 15000 );

		TimerTask doors = new TimerTask() {
			public void run() {
				m_botAction.sendArenaMessage("Safes are locked, race begins in 15 seconds!");
				m_botAction.setDoors(255);
			}
		};

		m_botAction.scheduleTask(doors, 25000);

		TimerTask announceLine = new TimerTask() {
			public void run() {
				m_botAction.sendArenaMessage( "Get ready, the race begins in 10 seconds.", 3 );
				m_botAction.sendUnfilteredPublicMessage("*objon 1");
			}
		};
		m_botAction.scheduleTask( announceLine, 30000 );

		TimerTask fiveSecs = new TimerTask() {
			public void run() {
				m_botAction.sendUnfilteredPublicMessage("*objon 2");
			}
		};

		m_botAction.scheduleTask( fiveSecs, 35000);

		TimerTask three = new TimerTask() {
			public void run() {
				m_botAction.sendArenaMessage( "3" );
				m_botAction.resetFlagGame();
			}
		};
		m_botAction.scheduleTask( three, 37000 );

		TimerTask two = new TimerTask() {
			public void run() {
				m_botAction.sendArenaMessage( "2" );
				m_botAction.resetFlagGame();
			}
		};
		m_botAction.scheduleTask( two, 38000 );

		TimerTask one = new TimerTask() {
			public void run() {
				m_botAction.sendArenaMessage( "1" );
				m_botAction.resetFlagGame();
			}
		};
		m_botAction.scheduleTask( one, 39000 );

		TimerTask beginRace = new TimerTask() {
			public void run() {
				m_botAction.sendArenaMessage( "GOOOO GOOOO GOOOO!!!", 104 );
				m_botAction.sendUnfilteredPublicMessage("*objon 3");
				racing = 2;
				m_botAction.resetFlagGame();
				m_botAction.setDoors(0);
			}
		};
		m_botAction.scheduleTask( beginRace, 40000 );
	}

	public void loadTrack( ResultSet result ) {

		try {
			int uId = result.getInt( "fnTrackID" );
			int trackId = result.getInt( "fnArenaTrackID" );
			String trackName = result.getString( "fcTrackName" );

			Track thisTrack = new Track( result, m_botAction );

			trackIDList.put( new Integer( trackId ), thisTrack );
			trackNameList.put( trackName, thisTrack );

			ResultSet tList = m_botAction.SQLQuery( m_sqlHost, "SELECT * FROM tblRaceCheckPoint WHERE fnTrackID = "+uId );
			thisTrack.loadCheckPoints( tList );
                        m_botAction.SQLClose( tList );

		} catch (Exception e) { Tools.printStackTrace(e); }
	}

	public void listTracks( String name ) {
		Iterator<Integer> it = trackIDList.keySet().iterator();
		while( it.hasNext() ) {
			int i = ((Integer)it.next()).intValue();
			Track n = trackIDList.get( new Integer( i ) );
			m_botAction.sendPublicMessage( n.getName()+"  "+i);
		}
	}

	public int getArenaID() {

		try {
			ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT * FROM tblRace WHERE fcArena = '"+m_botAction.getArenaName()+"'" );
                        int id = -1;
			if( result.next() )
                            id = result.getInt( "fnRaceID" );
                        m_botAction.SQLClose( result );
                        return id;
		} catch (Exception e) {
			Tools.printStackTrace(e);
			return -1;
		}
	}

	public Track getTrack( int id ) {
		return trackIDList.get( new Integer(id) );
	}


	public void handleEvent( FlagClaimed event ) {

		if(settingPits)
			pitFlags.add(new Integer(event.getFlagID()));

		if( racing == 0 ) return;

		if( System.currentTimeMillis() - lastReset > 2000 ) {
			m_botAction.resetFlagGame();
			lastReset = System.currentTimeMillis();
		}

		if( racing == 1 ) return;

		if(pitFlags.contains(new Integer(event.getFlagID())))
			handlePit(m_botAction.getPlayerName(event.getPlayerID()));

		try {
		Track thisTrack = getTrack( currentTrack );
		if( !thisTrack.check( event, laps ) )
			racing = 0;
		} catch (Exception e) { System.out.println(e); }
	}

	public void handlePit(String name)
	{
		m_botAction.sendUnfilteredPrivateMessage(name, "*prize #13");
	}

	public void handleDone()
	{
		m_botAction.sendArenaMessage("Race over! Winner is " + getTrack(currentTrack).winner + "!",5);
		String winnee = getTrack(currentTrack).winner;
		updated = false;
		racing = 0;
		m_botAction.sendUnfilteredPublicMessage("*objon 4");
		try {
			String query = "INSERT INTO tblRaceWinners (arena, trackWon, name ) VALUES("+getArenaID() +", "+currentTrack+", '"+winnee+"')";
			m_botAction.SQLQueryAndClose(m_sqlHost, query);
		} catch(Exception e) {}
		m_botAction.toggleLocked();
	}

	public void handleFake()
	{
		Track thisTrack = getTrack(currentTrack);
		m_botAction.sendUnfilteredPublicMessage("*objon 4");
		m_botAction.sendArenaMessage("Race over! Winner is " + getTrack(currentTrack).winner + "!",5);
		racing = 0;
		m_botAction.toggleLocked();
		thisTrack.lapLeaders = new HashMap<String, Integer>();
		thisTrack.positions = new HashMap<Integer, String>();
		loadArena();
		currentTrack = -1;
	}

	public int getPosition(String name)
	{
		ArrayList<ArrayList<String>> positions = getTrack(currentTrack).playerPositions;
		int position = 1;

		for(int k = positions.size() - 1;k >= 0;k--)
		{
			ArrayList<String> currentLap = positions.get(k);
			if(currentLap.indexOf(name.toLowerCase()) == -1)
				position += currentLap.size();
			else
			{
				position += currentLap.indexOf(name.toLowerCase());
				return position;
			}
		}

		return -1;
	}
}