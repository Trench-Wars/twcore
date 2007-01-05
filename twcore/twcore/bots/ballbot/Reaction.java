//////////////////////////////////////////////////////
//
// Filename:
// Reaction.java
//
// Description:
// Reaction to an event
//
// Usage:
//
//
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import twcore.core.*;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.events.BallPosition;
import twcore.core.game.Ship;
import twcore.core.game.Player;
import java.util.*;

public class Reaction
{
	public static void OnPost( Incident post )
	{
	}

	public static void OnShot( Incident shot )
	{
		Position shotPos = new Position( shot.m_x, shot.m_y );
	}

	public static void OnPickup( Incident picker, Incident passer )
	{
		if( picker.m_freq != passer.m_freq )
			OnSteal( picker, passer );
		else
			OnCompletedPass( picker, passer );
	}

	private static void OnSteal( Incident picker, Incident passer)
	{
		if( passer.m_playerName == null )
			Speech.SayIncident( picker.m_playerName + " takes the ball" );
		else
			Speech.SayIncident( picker.m_playerName + " steals from " + passer.m_playerName );
	}

	private static void OnCompletedPass( Incident picker, Incident passer)
	{
		if( picker.m_playerId == passer.m_playerId )
			Speech.SayIncident( passer.m_playerName + " passes to self!" );
		else
			Speech.SayIncident( passer.m_playerName + " passes to " + picker.m_playerName );
	}

	public static void OnGoal( IncidentHistory history )
	{
		Incident shot = history.GetIncident( 1 );

		if( Arena.IsInCrease( shot.GetPosition() ) )
			OnCrease( shot, history );
		else
			OnLegalGoal( shot, history );
	}

	private static void OnCrease( Incident creasedShot, IncidentHistory history )
	{
		Speech.SayGoal("Crease! By " + creasedShot.m_playerName );
	}

	private static void OnLegalGoal( Incident shot, IncidentHistory history )
	{
		// Get shot speed
		double shotSpeed = shot.GetVelocity().GetLength();

		// Get shot distance
		Position shotPos = new Position( shot.m_x, shot.m_y );
		boolean shotDirection = shot.GetVelocity().getX() > 0;
		double shotDistance =
			shotDirection ?
			shotPos.distanceFrom( Arena.CREASE_CENTER_RIGHT ) :
			shotPos.distanceFrom( Arena.CREASE_CENTER_LEFT );

		// Get shot dinkness
		int dinkness = Interpret.GetDinkness( shot.GetPosition(), shot.GetVelocity() );

		// Mention the goal and assists
		Speech.SayGoal(
				"Goal! By " + shot.m_playerName +
				" [" +
				"  Distance: " + Unsorted.GetDistanceString( shotDistance ) +
				"  Speed: " + Unsorted.GetSpeedString( shotSpeed ) +
				" ]" );
		Speech.SayGoal( Interpret.GetAssistantsString( history ) );

		// Extra comments
		if( shotDistance > Unsorted.SNIPE_DISTANCE )
		{
			Unsorted.ChantString( Unsorted.m_snipeMessage, Unsorted.m_snipeSound, true );
		}
	}
}
