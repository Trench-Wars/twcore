//////////////////////////////////////////////////////
//
// Filename:
// Interpret.java
//
// Description:
// Get the finer details of an Incident
//
// Usage:
//
//
//
//////////////////////////////////////////////////////
package twcore.bots.ballbot;

import java.util.*;
public class Interpret
{
	public static String GetAssistantsString( IncidentHistory history )
	{
		String[] assistants = Interpret.GetAssistants( history );

		if( assistants.length == 0 )
		{
			return "No assist.";
		}
		else
		{
			String string = (assistants.length==1) ? "Assist by: " : "Assists by: ";

			for( int i=0; i<assistants.length; i++ )
			{
				string+= assistants[i];

				if( i != assistants.length-1 )
					string+= ", ";
			}
			return string;
		}
	}

	private static String[] GetAssistants( IncidentHistory history )
	{
		Vector assVec = new Vector();

		Incident goal = history.GetIncident( 1 );

		// Get all INCIDENT_SHOTS
		for( int i=2; i<IncidentHistory.HISTORY_LENGTH; i++ )
		{
			Incident incident = history.GetIncident( i );

			if( assVec.size() == 3 )
				break;

			if( incident == null )
				break;

			if( incident.m_incidentType == Incident.INCIDENT_GOAL )
				break;

			if( incident.m_incidentType == Incident.INCIDENT_SHOT )
			{
				if( incident.m_playerId == goal.m_playerId )
					break;

				if( incident.m_freq == goal.m_freq )
					assVec.addElement( incident.m_playerName );
			}
		}

		String[] assArray = new String[ assVec.size() ];
		for( int i=0; i<assVec.size(); i++ )
		{
			assArray[i] = (String)assVec.elementAt(i);
		}

		return assArray;
	}

	public static int GetDinkness( Position shotPos, Position shotVel )
	{
		boolean shotDirection = shotVel.getX() > 0;
		int intersect =
			shotDirection ?
			Arena.GOAL_VERT_INTERSECT_RIGHT :
			Arena.GOAL_VERT_INTERSECT_LEFT;

		int goalPosY = (int)Math2D.IntersectsXAt( shotPos, shotVel, intersect );
		boolean shotSide = goalPosY < Arena.GOAL_HOR_MIDDLE;
		int distanceFromPost =
			shotSide ?
			(goalPosY - Arena.GOAL_HOR_CEILING) :
			(Arena.GOAL_HOR_FLOOR - goalPosY);

		return distanceFromPost;
	}
}