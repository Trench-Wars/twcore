//////////////////////////////////////////////////////
//
// Filename:
// BallMonitor.java
// 
// Description: 
// 
//
// 
//
// 
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import twcore.core.*;
import twcore.core.events.BallPosition;
import java.util.*;

public class BallMonitor
{
	private IncidentHistory m_history = new IncidentHistory();

	private void UpdateIncidentType( Incident thisIncident )
	{
		Incident prevIncident = m_history.GetLastIncident();
		
		if( prevIncident == null )
			thisIncident.m_incidentType = Incident.INCIDENT_INIT;		
		else if( ( prevIncident.m_playerId != -1 ) && ( thisIncident.m_playerId == -1 ) )
			thisIncident.m_incidentType =  Incident.INCIDENT_GOAL;		
		else if( ( prevIncident.m_timeStamp != 0 ) && ( thisIncident.m_timeStamp == 0 ) )
			thisIncident.m_incidentType =  Incident.INCIDENT_PICKUP;		
		else if( ( prevIncident.m_timeStamp == 0 ) && ( thisIncident.m_timeStamp != 0 ) )
			thisIncident.m_incidentType =  Incident.INCIDENT_SHOT;		
		else 
			thisIncident.m_incidentType =  Incident.INCIDENT_NONE;
	}
	
	public void SubmitEvent( BallPosition event, ballbot bot )
	{
		Incident latestEvent = new Incident( event, bot, Incident.INCIDENT_NONE );
		UpdateIncidentType( latestEvent );
		
		if( latestEvent.m_incidentType == Incident.INCIDENT_NONE )
			return;

		m_history.AppendIncident( latestEvent );
		
		switch( latestEvent.m_incidentType )
		{
		case Incident.INCIDENT_GOAL:
			Reaction.OnGoal( m_history );
			break;
		case Incident.INCIDENT_PICKUP:
			Reaction.OnPickup( m_history.GetIncident( 0 ), m_history.GetIncident( 1 ) );
			break;
		case Incident.INCIDENT_SHOT:
			Reaction.OnShot( m_history.GetIncident( 0 ) );
			break;
		case Incident.INCIDENT_POST:
			Reaction.OnPost( m_history.GetIncident( 0 ) );
			break;
		}
	}
}