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

import twcore.core.game.Player;

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
		if( State.Is( State.FACE_OFF ) || State.Is( State.SPOT_PUCK_BEFORE_FACE_OFF ) )
		{
			Player spotter = BotTask.GetPlayer( picker.m_playerId );
			Position grabPos = new Position( picker.m_x, picker.m_y );
			BotTask.PlacePuckInCenter( spotter, grabPos );
			State.Override( State.FACE_OFF, 1 );
		}
		
		if( State.Is( State.REG_PLAY ) || State.Is( State.FREE_PLAY ) )
		{			
			int dcCall = Interpret.GetDcCall( picker, passer );
			
			if( dcCall == Interpret.DC_CALL_DC )
			{
				OnDefensiveCrease( picker, passer );
			}
			
			if( dcCall == Interpret.DC_CALL_BDC )
			{
				OnBadDefensiveCrease( picker, passer );
			}

			if( picker.m_freq != passer.m_freq )
			{
				OnSteal( picker, passer );
			}
			else
			{
				OnCompletedPass( picker, passer );
			}
		}
	}
	
	private static void OnDefensiveCrease( Incident picker, Incident passer )
	{
		Speech.SayGoal( "DC against " + picker.m_playerName );
	}
	
	private static void OnBadDefensiveCrease( Incident picker, Incident passer )
	{
		Speech.SayGoal( "BDC against " + picker.m_playerName );		
	}

	private static void OnSteal( Incident picker, Incident passer)
	{
		return;
		/*
		if( passer.m_playerName == null )
			Speech.SayIncident( picker.m_playerName + " takes the ball" );
		else
			Speech.SayIncident( picker.m_playerName + " steals from " + passer.m_playerName );*/
	}

	private static void OnCompletedPass( Incident picker, Incident passer)
	{
		return;/*
		if( picker.m_playerId == passer.m_playerId )
			Speech.SayIncident( passer.m_playerName + " passes to self!" );
		else
			Speech.SayIncident( passer.m_playerName + " passes to " + picker.m_playerName );*/
	}

	public static void OnGoal( IncidentHistory history )
	{
		if( State.Is( State.REG_PLAY ) )
		{
			State.Override( State.SPOT_PUCK_BEFORE_FACE_OFF, 3 );
		}
		
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
				"  Perfection: " + Unsorted.GetPerfectionString( dinkness ) +
				" ]" );
		Speech.SayGoal( Interpret.GetAssistantsString( history ) );

		// Extra comments
		if( shotDistance > Unsorted.SNIPE_DISTANCE )
		{
			Unsorted.ChantString( Unsorted.m_snipeMessage, Unsorted.m_snipeSound, true );
		}
	}
}
