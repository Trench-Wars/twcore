//////////////////////////////////////////////////////
//
// Filename:
// DebugOut.java
//
// Description:
// 
//
// Usage:
// 
// 
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

public class DebugOut
{
	public static final boolean DEBUG = false;
	
	public static void Print( String message )
	{
		if( DEBUG )
		{
			System.out.println( message );
		}
	}
}