
//////////////////////////////////////////////////////
//
// Filename:
// Unsorted.java
//
// Description:
// Miscellaneous stuff
//
// Usage:
// Various stuff that needs a place
//
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import java.util.StringTokenizer;
import java.util.Vector;

public class Unsorted
{
	public static String m_snipeMessage = "SNIPER";
	public static int m_snipeSound = 5;
	public static int SNIPE_DISTANCE = 1200;
	public static String m_dinkMessage = "DINK";
	public static int m_dinkSound = 1;

	
	public static String GetRow( String[] columns, int[] widths, String delim )
	{
		String output = "";
		
		for( int i=0; i<columns.length; i++ )
		{
			output+= GetPaddedString( columns[i], widths[i], ' ' ) + delim + "\n";
		}		
		
		return output;
	}
	
	public static String GetPaddedString( String string, int length, char c )
	{
		int paddingNeeded = length - string.length();
		for( int i=0; i<paddingNeeded; i++ )
			string+=c;
		
		return string.substring(0,length);
	}
	
	public static void ChantString( String chant, int sound, boolean stagger )
	{
		for( int i=0; i<chant.length(); i++ )
		{
			String character = "| ";

			for(int p=0; p<i; p++)
				character+= " ";

			character+= chant.charAt(i);
			Speech.SayGoal( character, sound );

			try
			{
				Thread.sleep(300);
			}
			catch( Exception e )
			{

			}
		}
	}
	
	public static String GetDistanceString( double distance )
	{
		return ((int)distance/100) + "." + ((int)distance)%100 + "m";
	}
	
	public static String GetSpeedString( double speed )
	{
		return ((int)speed/100) + "." + ((int)speed)%100 + "mph";
	}
	
	public static String GetPerfectionString( int dinkness )
	{
		int perfection = 10 - dinkness;
		if( perfection < 0 )
			perfection = 0;
		return perfection + "/10";
	}
	
	protected int[] GetTokens( String message )
	{
		StringTokenizer tokenizer = new StringTokenizer( message, " " );
		Vector vector = new Vector();
				
		while( tokenizer.hasMoreTokens() )
		{
			vector.addElement( tokenizer.nextToken() );
		}
		
		if( vector.size() > 0 )
		{
			vector.removeElementAt( 0 );
		}
		
		int[] intTokens = new int[ vector.size() ];
		
		for( int i=0; i<vector.size(); i++ )
		{
			try
			{
				intTokens[i] = Integer.parseInt( (String)vector.elementAt(i) ) ;
			}
			catch( Exception e )
			{
				DebugOut.Print( (String)vector.elementAt(i) );
			}
		}
		
		return intTokens;
	}
}
