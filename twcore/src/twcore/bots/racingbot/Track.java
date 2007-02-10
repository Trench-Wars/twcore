package twcore.bots.racingbot;

import twcore.core.*;
import twcore.core.events.FlagClaimed;
import twcore.core.util.Tools;

import java.sql.*;
import java.util.*;

public class Track {

	BotAction m_botAction;
	HashMap checkPoints;
	HashMap playerMap;
	HashMap positions;
	HashMap lapLeaders;
	ArrayList playerPositions;

	String trackName;
	String winner = "";
	String second = "";
	String third = "";
	int lastPoint = 0;
	int ships[] = {0,0,0,0,0,0,0,0};
	int lastLap = 0;
	int place = 1;
	int x_pos = 512, y_pos = 512;

	boolean won = false;
	boolean newRecord = false;

	public Track( ResultSet result, BotAction botAction ) {
		m_botAction = botAction;
		checkPoints = new HashMap();
		playerMap   = new HashMap();
		positions = new HashMap();
		lapLeaders = new HashMap();
		playerPositions = new ArrayList();
		try {
			trackName = result.getString( "fcTrackName" );
			x_pos = result.getInt( "fnXWarp" );
			y_pos = result.getInt( "fnYWarp" );
			String shipList[] = result.getString( "fcAllowedShips" ).split(" ");
			for( int i = 0; i < shipList.length; i++) {
				int num = Integer.parseInt( shipList[i] );
				ships[num-1] = 1;
			}
		} catch (Exception e) {}
	}

	public void loadCheckPoints( ResultSet result ) {

		try {
			while( result.next() ) {
				int checkPoint = result.getInt( "fnCheckPoint" );
				int flagId = result.getInt( "fnFlagID" );
				checkPoints.put( new Integer( flagId ), new Integer( checkPoint ) );
				if( checkPoint > lastPoint ) lastPoint = checkPoint;
			}
		} catch (Exception e) { Tools.printStackTrace(e); }
	}

	public boolean check( FlagClaimed event, int lapsToWin ) {

		int checkPoint = -1;
		int playerId = event.getPlayerID();
		try {
			checkPoint = ((Integer)checkPoints.get( new Integer( event.getFlagID() ) ) ).intValue();
		} catch (Exception e) {}

		if( checkPoint == -1 ) return true;



		if( !playerMap.containsKey( new Integer( playerId ) ) )
			playerMap.put( new Integer( playerId ), new RacePlayer( lastPoint ) );

		RacePlayer thisP = (RacePlayer)playerMap.get( new Integer( playerId ) );
		int laps = thisP.hitPoint( checkPoint , m_botAction.getPlayerName(playerId), this);

		if( laps == lapsToWin ) {
			if(!won)
			{
				won = true;
				m_botAction.sendArenaMessage( m_botAction.getPlayerName( playerId ) +" Wins the race!", 5 );
				winner = m_botAction.getPlayerName(playerId);
				m_botAction.spec(m_botAction.getPlayerName(playerId));
				m_botAction.spec(m_botAction.getPlayerName(playerId));
				positions.put(new Integer(1), winner);
				if(lapLeaders.containsKey(winner))
				{
					int lapz = ((Integer)lapLeaders.get(winner)).intValue();
					lapz++;
					lapLeaders.put(winner, new Integer(lapz));
				}
				else
					lapLeaders.put(winner, new Integer(1));
			}
			else
			{
				if(place == 2)
					second = m_botAction.getPlayerName(playerId);
				if(place == 3)
					third = m_botAction.getPlayerName(playerId);
				m_botAction.sendArenaMessage(place + ". " + m_botAction.getPlayerName(playerId));
				m_botAction.spec(m_botAction.getPlayerName(playerId));
				m_botAction.spec(m_botAction.getPlayerName(playerId));
				positions.put(new Integer(place), m_botAction.getPlayerName(playerId));
			}
			place++;
		} else if( laps > 0 ) {
			String out = "(1 lap)";
			if( laps > 1 )
				out = "("+laps+" laps)";

			if(lastLap < laps) {
				lastLap = laps;
				m_botAction.sendArenaMessage( m_botAction.getPlayerName( playerId ) +" is in the lead. "+out );
				if(lapLeaders.containsKey(m_botAction.getPlayerName(playerId)))
				{
					int lapz = ((Integer)lapLeaders.get(m_botAction.getPlayerName(playerId))).intValue();
					lapz++;
					lapLeaders.put(m_botAction.getPlayerName(playerId), new Integer(lapz));
				}
				else
					lapLeaders.put(m_botAction.getPlayerName(playerId), new Integer(1));
			}
		}


		return true;
	}

	public boolean checkShip( int ship ) {
		if( ships[ship-1] == 0 ) return true;
		else return false;
	}

	public int getShip() {
		for( int i=0; i < 8; i++ )
			if(ships[i] == 1 ) return i+1;

		return 1;
	}

	public void warpPlayers() {
		m_botAction.warpAllToLocation( x_pos, y_pos );
	}

	public String getName() { return trackName; }

	public void reset() {
		playerMap.clear();
		winner = "";
		lastLap = 0;
		won = false;
		place = 1;
	}
}