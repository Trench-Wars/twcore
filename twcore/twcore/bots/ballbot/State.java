package twcore.bots.ballbot;

import twcore.core.*;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.events.BallPosition;

import java.util.*;

public class State
{
	public static final int INVALID = 0;
	public static final int FREE_PLAY = 1;
	public static final int REG_PLAY = 2;
	public static final int FACE_OFF = 3;	
	public static final int SPOT_PUCK_BEFORE_FACE_OFF = 4;
	public static final String[] ms_stateNames = new String[]{ "Invalid", "Free play", "Regular play", "Face-off" };	
	
	private static int ms_currentState = FREE_PLAY;
	private static int ms_nextState = 0;
	private static Date ms_changeoverAt = null;
	private static boolean ms_acknowledgedStateChange = true;
	
	public static void Tick()
	{
		Update();
	}	
	
	private static void Update()
	{
		if( !ms_acknowledgedStateChange )
		{
			ms_acknowledgedStateChange = true;
			OnStateChange();
		}
		
		if( ms_changeoverAt == null )
		{
			return;
		}
		
		if( new Date().after( ms_changeoverAt ) )
		{
			ms_changeoverAt = null;
			ms_currentState = ms_nextState;	
			ms_acknowledgedStateChange = false;			
		}
	}

	private static void OnStateChange()
	{
		switch( ms_currentState )
		{
		case INVALID:
			Speech.SayGoal( "" );
			break;
		case FREE_PLAY:
			Speech.SayGoal( "Free play" );
			break;
		case REG_PLAY:
			Speech.SayGoal( "GO!!!!!!!!!!" );
			break;
		case SPOT_PUCK_BEFORE_FACE_OFF:
			Speech.SayGoal( "Grab the puck begin face-off" );
			break;
		case FACE_OFF:
			Speech.SayGoal( "Face off begins in 10 seconds... " );
			State.Override( State.REG_PLAY, 10 );
			break;
		default:
			break;
		}
	}

	public static void Override( int nextState, int secondsTillExpiry )
	{
		ms_nextState = nextState;
		
		if( secondsTillExpiry == INVALID )
			ms_changeoverAt = null;
		else
			ms_changeoverAt = GetFutureDate( secondsTillExpiry );
	}
	
	private static Date GetFutureDate( int secondsAfterNow )
	{
		long now_ms = new Date().getTime();
		long future_ms = now_ms + secondsAfterNow * 1000; 
		return new Date( future_ms );
	}
	
	public static String GetStateName( int state )
	{		
		return ms_stateNames[ state ];
	}
	
	public static boolean Is( int state )
	{
		return state == ms_currentState;
	}
}