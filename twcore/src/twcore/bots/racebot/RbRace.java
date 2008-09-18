package twcore.bots.racebot;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.core.events.FlagClaimed;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.Tools;

public class RbRace extends RaceBotExtension {

	HashMap<Integer, Track> trackIDList;
	HashMap<String, Track> trackNameList;

	int racing = 0;
	int laps = 0;
	int currentTrack = -1;
	int pitExit[] = new int[2];
	long lastReset = 0;

	boolean updated = true;
	
	boolean arenaLocked = false;

	public RbRace() {
		trackIDList = new HashMap<Integer, Track>();
		trackNameList = new HashMap<String, Track>();
	}

	public void handleEvent( Message event ) {
		String name = m_botAction.getPlayerName( event.getPlayerID() );
		String message = event.getMessage().toLowerCase();
		
		// Check for reliable arena locking
		if(event.getMessageType() == Message.ARENA_MESSAGE) {
            if(event.getMessage().equals("Arena LOCKED")) {
                if(this.arenaLocked == false) {
                    m_botAction.toggleLocked();
                }
            }
            if(event.getMessage().equals("Arena UNLOCKED")) {
                if(this.arenaLocked == true) {
                    m_botAction.toggleLocked();
                }
            }
		}
		
		if(message.startsWith("!position ")) {
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
        } else if(message.startsWith("!help")) {
            String help[] = {
                "------------- Race Help Menu (Player+) --------------------------",
                "| !position          - Shows your current race position         |",
                "| !position <player> - Shows the position of <player>           |",
                "----------------------------------------------------------------|"
            };
            m_botAction.privateMessageSpam( name, help );
        }
		
		if(m_botAction.getOperatorList().isSmod(name) || m_bot.isOperator(name))
		{
			if( message.startsWith( "!help" ) ) {
	            String help[] = {
	                "------------- Race Help Menu (ER+) ----------------------------------",
	                "| !loadarena               - loads arena information                |",
	                "| !startrace <id#> <laps#> - starts a race on the track <id#>       |",
	                "|                            for <laps#>                            |",
	                "| !tracklist               - displays available tracks for this map |",
	                "| !raceover                - ends the race                          |",
	                "| !fakeover                - ends the race without saving results   |",
	                "--------------------------------------------------------------------|"
	            };
	            m_botAction.privateMessageSpam( name, help );
			}
			else if( message.startsWith( "!loadarena" ) )
				loadArena( name, true);
			else if( message.startsWith( "!startrace " ) )
				startRace( message, name );
			else if( message.startsWith( "!tracklist" ) )
				listTracks( name );
			else if( message.startsWith("!raceover"))
				handleDone();
			else if( message.startsWith("!fakeover"))
				handleFake();
		}
	}

	public void loadArena(String name, boolean respond) {

		trackIDList.clear();
		trackNameList.clear();

		int arenaId = getArenaID();
		if( arenaId < 0 ) {
		    if(respond)
		        m_botAction.sendPrivateMessage( name, "This arena has not been setup for racing." );
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
		
		if(respond)
		    m_botAction.sendPrivateMessage(name, "Bot ready for use.");
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
			m_botAction.sendPrivateMessage( name, "Unable to start race, track does not exist." );
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
				arenaLocked = true;
				
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
			    m_botAction.resetFlagGame();
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
			}
		};
		m_botAction.scheduleTask( three, 37000 );

		TimerTask two = new TimerTask() {
			public void run() {
				m_botAction.sendArenaMessage( "2" );
				m_botAction.getShip().setShip(0);
			}
		};
		m_botAction.scheduleTask( two, 38000 );

		TimerTask one = new TimerTask() {
			public void run() {
				m_botAction.sendArenaMessage( "1" );
			}
		};
		m_botAction.scheduleTask( one, 39000 );

		TimerTask beginRace = new TimerTask() {
			public void run() {
				m_botAction.sendArenaMessage( "GOOOO GOOOO GOOOO!!!", 104 );
				m_botAction.sendUnfilteredPublicMessage("*objon 3");
				racing = 2;
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
			
			ResultSet pList = m_botAction.SQLQuery( m_sqlHost, "SELECT * FROM tblRacePits WHERE fnTrackID = "+uId );
			thisTrack.loadPits( pList );
			m_botAction.SQLClose( pList );
            

		} catch (Exception e) { Tools.printStackTrace(e); }
	}

	public void listTracks( String name ) {
		Iterator<Integer> it = trackIDList.keySet().iterator();
		while( it.hasNext() ) {
			int i = ((Integer)it.next()).intValue();
			Track n = trackIDList.get( new Integer( i ) );
			m_botAction.sendPrivateMessage(name, "ID  NAME");
			m_botAction.sendPrivateMessage(name, "--------------------");
			
			m_botAction.sendPrivateMessage(name, Tools.formatString(String.valueOf(i), 4) + 
			                                     n.getName());
		}
	}

	public int getArenaID() {

		try {
			ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT * FROM tblRace WHERE fcArena = '"+m_botAction.getArenaName()+"'" );
                        int id = -1;
			if( result != null && result.next() )
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
	    
		if( racing < 2 ) return;

		// Claim flag by bot (instead of flagreset)
		short botid = m_botAction.getPlayer(m_botAction.getBotName()).getPlayerID();

		if(botid != event.getPlayerID()) {
		    m_botAction.grabFlag(event.getFlagID());
		    
    		Track thisTrack = getTrack( currentTrack );
    		if( !thisTrack.check( event, laps ) )
    		    racing = 0;
		}
	}

	public void handleDone()
	{
	    Track thisTrack = getTrack(currentTrack);
	    if(thisTrack == null) return;
	    
	    // Game Over LVZ
	    m_botAction.sendUnfilteredPublicMessage("*objon 4");
	    racing = 0;
	    
	    if(thisTrack.winner == null || thisTrack.winner.trim().length() == 0) {
	        m_botAction.sendArenaMessage("Race over! No winner!",5);
	        m_botAction.sendArenaMessage("Results not saved to database (no winner).");
	    } else {
    		m_botAction.sendArenaMessage("Race over! Winner is " + thisTrack.winner + "!",5);
    		updated = false;
    		
    		try {
    			String query = "INSERT INTO tblRaceWinners (arena, trackWon, name ) VALUES("+getArenaID() +", "+currentTrack+", '"+thisTrack.winner+"')";
    			m_botAction.SQLQueryAndClose(m_sqlHost, query);
    		} catch(Exception e) {}
	    }
	    
		// Unlock arena
		m_botAction.toggleLocked();
		arenaLocked = false;
		m_botAction.cancelTasks();
		m_botAction.setDoors(0);
		m_botAction.getShip().setShip(8);
	}

	public void handleFake()
	{
		Track thisTrack = getTrack(currentTrack);
		if(thisTrack == null) return;
		m_botAction.sendUnfilteredPublicMessage("*objon 4");
		
		if(thisTrack.winner == null || thisTrack.winner.trim().length() == 0) {
            m_botAction.sendArenaMessage("Race over! No winner!",5);
		} else {
		    m_botAction.sendArenaMessage("Race over! Winner is " + getTrack(currentTrack).winner + "!",5);
		}
		racing = 0;
		
		// Unlock arena
		m_botAction.toggleLocked();
		arenaLocked = false;
		m_botAction.cancelTasks();
		m_botAction.setDoors(0);
		m_botAction.getShip().setShip(8);
		
		thisTrack.lapLeaders = new HashMap<String, Integer>();
		thisTrack.positions = new HashMap<Integer, String>();
		loadArena(null, false);     // reload arena but no response
		currentTrack = -1;
		
	}

	public int getPosition(String name)
	{
	    Track track = getTrack(currentTrack);
	    if(track == null)
	        return -1;
	    
		ArrayList<ArrayList<String>> positions = track.playerPositions;
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