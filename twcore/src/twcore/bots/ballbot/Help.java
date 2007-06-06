//////////////////////////////////////////////////////
//
// Filename:
// Help.java
//
// Description:
// Extracts and formats the !help display from a Command array
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import java.util.Vector;

public class Help
{
	// For help
	static String[] m_titles = new String[]{ "Command", "Description" };
	static int[] m_widths = new int[]{20,30};
	static String m_delim = "|";


	public static Vector GetHelpText( Vector botCommands )
	{
		int totalWidth = 0;
		String totalWidthLine = "";
		for( int i=0; i<m_widths.length; i++ )
			totalWidth+= m_widths[i] + 1;
		totalWidthLine = Unsorted.GetPaddedString( totalWidthLine, totalWidth, '-' );



		Vector<String> help = new Vector<String>();

		help.addElement( totalWidthLine );
		help.addElement( Unsorted.GetRow( m_titles, m_widths, m_delim ) );
		help.addElement( totalWidthLine );

		for( int i=0; i< botCommands.size(); i++ )
		{
			help.addElement( GetCommandListAt( (Command)botCommands.elementAt( i ) ) );
		}
		help.addElement( totalWidthLine );

		return help;
	}

	public static String GetCommandListAt( Command cmd )
	{
		String[] columns = new String[]{
			cmd.GetCommand() + " " + cmd.GetCommandArgs(),
			cmd.GetDescription()
			};

		return Unsorted.GetRow( columns, m_widths, m_delim );
	}
}