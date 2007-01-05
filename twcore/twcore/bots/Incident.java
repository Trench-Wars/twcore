//////////////////////////////////////////////////////
//
// Filename:
// N/A
//
// Description:
// Contains BallPosition event data and any other useful metadata (e.g. Player's name)
//
// Usage:
// Public member access
//
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import twcore.core.*;
import twcore.core.events.Message;
import twcore.core.events.BallPosition;
import twcore.core.game.Ship;
import twcore.core.game.Player;
import java.util.*;

public class Incident
{
	public static final int INCIDENT_NONE = 0;
	public static final int INCIDENT_INIT = 1;
	public static final int INCIDENT_GOAL = 2;
	public static final int INCIDENT_SHOT = 3;
	public static final int INCIDENT_PICKUP = 4;
	public static final int INCIDENT_POST = 5;
	
	public int m_incidentType;

	public int m_ballId;
	public int m_playerId;
	public int m_x;
	public int m_y;
	public int m_vx;
	public int m_vy;
	public int m_timeStamp;

	public String m_playerName;
	public int m_freq = -1;

	Incident( BallPosition event, SubspaceBot bot, int incidentType )
	{
		// Copy data from event
		m_ballId = event.getBallID();
		m_playerId = event.getPlayerID();
		m_x = event.getXLocation();
		m_y = event.getYLocation();
		m_vx = event.getXVelocity();
		m_vy = event.getYVelocity();
		m_timeStamp = event.getTimeStamp();
		
		// Copy incident type
		m_incidentType = incidentType;

		// Use m_botAction to store convenience data for later use
		m_playerName = bot.m_botAction.getPlayerName( m_playerId );
		Player player = bot.m_botAction.getPlayer( m_playerId );

		if( player != null )
			m_freq = player.getFrequency();
	}
	
	public Position GetPosition()
	{
		return new Position( m_x, m_y );
	}
	
	public Position GetVelocity()
	{
		return new Position( m_vx, m_vy );
	}

	public String toString()
	{
		String ids = "BID("+m_ballId+") PID("+m_playerId+")";
		String pos = "P("+GetPosition()+")";
		String vel = "V("+GetVelocity()+")";
		String type = getTypeString();
		String time = "T("+m_timeStamp+")";
		String freq = m_playerName+"("+m_freq+")";

		return ids + " " + pos + " " + vel + " " + type + " " + time + " " + freq;
	}

	public String getTypeString()
	{
		switch(m_incidentType)
		{
		case Incident.INCIDENT_NONE:
			return "NONE";
		case Incident.INCIDENT_GOAL:
			return "GOAL";
		case Incident.INCIDENT_SHOT:
			return "SHOT";
		case Incident.INCIDENT_PICKUP:
			return "PICK";
		case Incident.INCIDENT_POST:
			return "POST";
			default:
				return "";
		}
	}
}