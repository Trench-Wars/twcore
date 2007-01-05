

//////////////////////////////////////////////////////
//
// Filename:
// Arena.java
//
// Description:
// Physical layout of an arena
//
// Usage:
// Public data + methods
//
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import twcore.core.*;
import twcore.core.events.Message;
import java.util.*;

class Arena
{
	public static final Position CREASE_CENTER_LEFT = new Position( 6030, 8192 );
	public static final Position CREASE_CENTER_RIGHT = new Position( 10354, 8192 );
	public static final int CREASE_RADIUS = 242;
	public static final int CREASE_CEILING = 8001;
	public static final int CREASE_FLOOR = 8382;

	public static final int BLUE_LINE_LEFT_X = 7408;
	public static final int BLUE_LINE_RIGHT_X = 8977;
	public static final int BLUE_LINE_TO_WARP_INDENT = 20;
	public static final int BLUE_LINE_WARP_LEFT_X = BLUE_LINE_LEFT_X - BLUE_LINE_TO_WARP_INDENT;
	public static final int BLUE_LINE_WARP_RIGHT_X = BLUE_LINE_RIGHT_X + BLUE_LINE_TO_WARP_INDENT;
	public static final int CENTER = 8192;

	public static final int GOAL_VERT_INTERSECT_LEFT = 6081;
	public static final int GOAL_VERT_INTERSECT_RIGHT = 10304;
	public static final int GOAL_HOR_CEILING = 8032;
	public static final int GOAL_HOR_FLOOR = 8350;
	public static final int GOAL_HOR_MIDDLE = 8192;

	public static final Position SINBIN_LEFT = new Position( 0, 0 );
	public static final Position SINBIN_RIGHT = new Position( 0, 0 );

	public static final Position FACEOFF_SAFE_LEFT = new Position( 0, 0 );
	public static final Position FACEOFF_SAFE_RIGHT = new Position( 0, 0 );
	public static final Position FACEOFF_CIRCLE_RADIUS = new Position( 0, 0 );
	public static final Position FACEOFF_CIRCLE_CENTER = new Position( 0, 0 );

	public static boolean IsInCrease( Position pos )
	{
		if( IsInCrease( pos, Arena.CREASE_CENTER_LEFT ) )
			return true;

		if( IsInCrease( pos, Arena.CREASE_CENTER_RIGHT ) )
			return true;

		return false;
	}

	public static boolean IsInCrease( Position pos, Position creaseCenter  )
	{
		boolean outOfCircle = !pos.IsInsideCircle( creaseCenter, Arena.CREASE_RADIUS );
		boolean aboveCeiling = pos.getY() < Arena.CREASE_CEILING;
		boolean belowFloor = pos.getY() > Arena.CREASE_FLOOR;

		if( outOfCircle )
			return false;

		if( aboveCeiling )
			return false;

		if( belowFloor )
			return false;

		return true;
	}
}