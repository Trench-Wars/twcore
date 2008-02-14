package twcore.bots.racingbot;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;

import twcore.core.events.FlagClaimed;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.Tools;

public class RbTrackManager extends RacingBotExtension {


	HashMap<Integer, Integer> checkPointList;
	String action = "";

	int    checkPoint;
	int    state;

	public RbTrackManager() {
		checkPointList = new HashMap<Integer, Integer>();
	}

	public void handleEvent( Message event ) {
		try {
			String name = m_botAction.getPlayerName( event.getPlayerID() );

			if(m_botAction.getOperatorList().isModerator(name) || m_bot.twrcOps.contains(name.toLowerCase()))
			{
				String message = event.getMessage().toLowerCase();
				if( message.equals( "yes" ) )
					handleAnswer( true );
				else if( message.equals( "no" ) )
					handleAnswer( false );
				else if( message.equals( "!help" ) ) {
				    String help[] = {
			            "------------- Track Manager Help Menu ---------------------------",
			            "| !newtrack      - starts the process of setting up a new track |",
			            "| !done          - used when done setting current checkpoint    |",
			            "| !name <name>   - sets arena name to <name>                    |",
			            "| !nametrack <id>:<name> - sets track <id> to <name>            |",
			            "| !setships <id>:<ships> - sets ships for track <id>            |",
			            "|                          seperate multiple ships by a space   |",
			            "| !setwarp <id>  - sets the warp point for track <id> from      |",
			            "|                  your current position                        |",
			            "| !testwarp <id> - tests a warp point already setup             |",
			            "----------------------------------------------------------------|"
			        };
			        m_botAction.privateMessageSpam( name, help );
				}
				else if( message.startsWith( "!newtrack" ) )
					createTrack( message );
				else if( message.startsWith( "!done" ) ) {
					endCheckPoint();
				} else if( message.startsWith( "!name " ) )
					setArenaName( event.getMessage().substring( 6, message.length() ) );
				else if( message.startsWith( "!nametrack " ) )
					setTrackName( event.getMessage().substring( 11, message.length() ), name );
				else if( message.startsWith( "!setships " ) )
					setShipList( message.substring( 10, message.length() ), name );
				else if( message.startsWith( "!setwarp " ) )
					setWarpPoint( message.substring( 9, message.length() ), name );
				else if( message.startsWith( "!testwarp " ) )
					testWarp( message.substring( 10, message.length() ), name );
			}
		} catch(Exception e) {}

	}

	public void handleAnswer( boolean answer ) {

		if( action.equals( "arena" ) )
			if( answer ) createArena();
			else m_botAction.sendPublicMessage( "k" );
		else if( action.equals( "setpoint" ) )
			if( answer ) newCheckPoint();
			else resetLastPoint();
		else if( action.equals( "endtrack" ) )
			if( answer ) endTrack();
			else resetLastPoint();
		action = "";

	}

	public void createTrack( String message ) {

		//Must create an arena before you can add tracks to it.
		if( !sql_arenaExists() ) {
			setupNewArena();
			return;
		}

		int id = sql_getNextArenaTrackID();

		if( id == -1 ) {
			m_botAction.sendPublicMessage( "Unable to create new track" );
			return;
		}

		checkPoint = 1;
		state = 2;
		m_botAction.sendPublicMessage( "Please mark the start/finish checkpoint." );
		m_botAction.sendPublicMessage( "When done marking points use !done" );
	}

	public void endCheckPoint() {

		if( state == 0 ) return;

		Iterator<Integer> it = checkPointList.keySet().iterator();
		int count = 0;
		while( it.hasNext() ) {
			Integer point = it.next();
			if( checkPointList.get(point).intValue() == checkPoint ) count++;
		}

		if( count == 0 ) {
			m_botAction.sendPublicMessage( "No points were marked. End track? yes/no" );
			action = "endtrack";
			state = 1;
			return;
		}

		m_botAction.sendPublicMessage( "Points: " + count + " marked." );
		m_botAction.sendPublicMessage( "Is this correct? yes/no" );
		action = "setpoint";
		state = 1;
	}

	public void newCheckPoint() {
		m_botAction.sendPublicMessage( "If you are done use !done to end the track." );
		state = 2;
		m_botAction.resetFlagGame();
		checkPoint++;
		m_botAction.sendPublicMessage( "Please mark checkpoint " + checkPoint );
	}

	public void resetLastPoint() {
		m_botAction.resetFlagGame();

		Iterator<Integer> it = checkPointList.keySet().iterator();
		while( it.hasNext() ) {
			Integer point = it.next();
			if( checkPointList.get(point).intValue() == checkPoint ) it.remove();
		}
		m_botAction.sendPublicMessage( "Last checkpoint was not correct, please mark that checkpoint again." );
		state = 2;
	}

	public void endTrack() {
		state = 0;

		sql_insertNewTrack();
		int id = sql_getMaxTrackID();


		Iterator<Integer> it = checkPointList.keySet().iterator();
		while( it.hasNext() ) {
			int flagId = it.next().intValue();
			int checkPointId = checkPointList.get( new Integer( flagId ) ).intValue();
			try {
			    m_botAction.SQLQueryAndClose( m_sqlHost, "INSERT INTO tblRaceCheckPoint (fnTrackID, fnCheckPoint, fnFlagID) VALUES ("+id+", "+checkPointId+", "+flagId+")" );
			} catch (Exception e) { Tools.printStackTrace(e); }
		}

		m_botAction.sendPublicMessage( "Track stored #" + (sql_getNextArenaTrackID()-1) );
		m_botAction.sendPublicMessage( "To name this track use: !nametrack <id>:<name>" );
		checkPointList.clear();

	}

	public void setupNewArena() {
		m_botAction.sendPublicMessage( "Arena has not been setup for racing. Setup now? yes/no" );
		action = "arena";
	}

	public void createArena() {

		try {
			String a = m_botAction.getArenaName();
			ResultSet result = m_botAction.SQLQuery( m_sqlHost, "INSERT INTO tblRace (fcArena, fcName) VALUES ('"+a+"', '"+a+"')" );
                        m_botAction.SQLClose( result );
			m_botAction.sendPublicMessage( "Arena setup. To change the name of this arena later please use !name" );
			m_botAction.sendPublicMessage( "Please do !newtrack again to setup tracks for this arena." );
		} catch (Exception e) {
			Tools.printStackTrace(e);
			m_botAction.sendPublicMessage( "Unable to setup arena." );
		}
	}

	public void setArenaName( String name ) {

		//Must create an arena before you can change its name
		if( !sql_arenaExists() ) {
			setupNewArena();
			return;
		}

		try {
		    m_botAction.SQLQueryAndClose( m_sqlHost, "UPDATE tblRace SET fcName = '"+Tools.addSlashesToString(name)+"' WHERE fcArena = '"+m_botAction.getArenaName()+"'" );
			m_botAction.sendPublicMessage( "Arena Name Changed To: " + name );
		} catch (Exception e) {
			Tools.printStackTrace(e);
			m_botAction.sendPublicMessage( "Unable to change arena name." );
		}
	}

	public void setTrackName( String message, String name ) {

		String pieces[] = message.split(":");
		int id = -1;
		try { id = Integer.parseInt( pieces[0] ); } catch (Exception e) {}

		if( id < 0 ) {
			m_botAction.sendPrivateMessage( name, "Unable to name track, track # out of bounds." );
			return;
		}

		try {
		    m_botAction.SQLQueryAndClose( m_sqlHost, "UPDATE tblRaceTrack SET fcTrackName = '"+Tools.addSlashesToString(pieces[1])+"' WHERE fnArenaTrackID = "+id+" AND fnRaceID = "+sql_getArenaID());
			m_botAction.sendPublicMessage( "Track #"+id+" name to "+pieces[1] );
		} catch (Exception e) {
			m_botAction.sendPrivateMessage( name, "Unable to name track." );
			Tools.printStackTrace( e );
		}

	}

	public void setShipList( String message, String name ) {

		String pieces[] = message.split(":");
		int id = -1;
		try { id = Integer.parseInt( pieces[0] ); } catch (Exception e) {}

		if( id < 0 ) {
			m_botAction.sendPrivateMessage( name, "Unable to name track, track # out of bounds." );
			return;
		}

		try {
			String chopped[] = pieces[1].split( " " );
			for( int i = 0; i < chopped.length; i++ ) {
				int d = Integer.parseInt( chopped[i] );
				if( d < 1 || d > 8 ) {
					m_botAction.sendPrivateMessage( name, "Unable to set ships for track, please only use ships 1-8." );
					return;
				}
			}

			m_botAction.SQLQueryAndClose( m_sqlHost, "UPDATE tblRaceTrack SET fcAllowedShips = '"+pieces[1]+"' WHERE fnArenaTrackID = "+id+" AND fnRaceID = "+sql_getArenaID());
			m_botAction.sendPrivateMessage( name, "Ships set to: (" + pieces[1] +  ") :on track #"+id);
		} catch (Exception e) {
			m_botAction.sendPrivateMessage( name, "Unable to set ships for track, please use numerical digits 1-8." );
			return;
		}
	}

	public void setWarpPoint( String message, String name ) {
		int id = -1;
		try { id = Integer.parseInt( message ); } catch (Exception e) {}

		if( id < 0 ) {
			m_botAction.sendPrivateMessage( name, "Unable to name track, track # out of bounds." );
			return;
		}

		try {
			Player p = m_botAction.getPlayer( name );
			m_botAction.SQLQueryAndClose( m_sqlHost, "UPDATE tblRaceTrack SET fnXWarp = "+(p.getXLocation()/16)+", fnYWarp = "+(p.getYLocation()/16)+" WHERE fnArenaTrackID = "+id+" AND fnRaceID = "+sql_getArenaID());
			m_botAction.sendPrivateMessage( name, "Warppoint set for track #"+id );
		} catch (Exception e) {
			m_botAction.sendPrivateMessage( name, "Unable to set warppoint for track");
		}
	}

	public void testWarp( String message, String name ) {
		int id = -1;
		try { id = Integer.parseInt( message ); } catch (Exception e) {}

		if( id < 0 ) {
			m_botAction.sendPrivateMessage( name, "Unable to name track, track # out of bounds." );
			return;
		}

		try {
			ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT fnXWarp, fnYWarp FROM tblRaceTrack WHERE fnArenaTrackID = "+id+" AND fnRaceID = "+sql_getArenaID());
			if( result.next() )
				m_botAction.warpTo( name, result.getInt("fnXWarp"), result.getInt("fnYWarp") );
                        m_botAction.SQLClose( result );
		} catch (Exception e) {}

	}

	public void handleEvent( FlagClaimed event ) {

		if( state < 2 ) return;
		checkPointList.put( new Integer( event.getFlagID() ), new Integer( checkPoint ) );
	}

	/*****************************
	**** SQL RELATED METHODS  ****
	*****************************/

	public boolean sql_insertNewTrack() {

		try {
		    m_botAction.SQLQueryAndClose( m_sqlHost, "INSERT INTO tblRaceTrack (fnRaceID, fnArenaTrackID ) VALUES ("+sql_getArenaID()+", "+sql_getNextArenaTrackID()+")" );
			return true;
		} catch (Exception e) {
			Tools.printStackTrace(e);
			return false;
		}
	}

	public boolean sql_arenaExists() {

		try {
			ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT * FROM tblRace WHERE fcArena = '"+m_botAction.getArenaName()+"'" );
			boolean exists = true;
			if( !result.next() )
			    exists = false;
			m_botAction.SQLClose( result );
			return exists;
		} catch (Exception e) { return false; }
	}

	public int sql_getArenaID() {

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

	public int sql_getMaxTrackID() {

		try {
			ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT MAX(fnTrackID) AS id FROM `tblRaceTrack`" );
            int id = -1;
            if( result != null && result.next() )
				id = result.getInt("id");
                
            m_botAction.SQLClose( result );
			return id;
		} catch (Exception e) {
			Tools.printStackTrace(e);
			return -1;
		}
	}

	public int sql_getNextArenaTrackID() {

		try {
			ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT MAX(fnArenaTrackID) AS id FROM `tblRaceTrack` WHERE fnRaceID = "+sql_getArenaID() );
                        int id = 1;
			if( result.next() )
				id = result.getInt("id")+1;
                        m_botAction.SQLClose( result );
			return id;
		} catch (Exception e) {
			Tools.printStackTrace(e);
			return -1;
		}

	}

}