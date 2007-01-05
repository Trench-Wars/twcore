//////////////////////////////////////////////////////
//
// Filename:
// PosMonitor.java
//
// Description:
// 
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import twcore.core.*;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import java.util.*;

public class PosMonitor
{
	private Vector m_reactions = new Vector();
	
	public PosMonitor()
	{
		m_reactions.addElement( new PrGoalieBlueLine() );
	}
	
	public void SubmitEvent( PlayerPosition event, SubspaceBot bot )
	{
		for( int i=0; i<m_reactions.size(); i++ )
			( (IPosReaction) m_reactions.elementAt( i ) ).React( event, bot );
	}
}

abstract class IPosReaction
{
	abstract void React( PlayerPosition event, SubspaceBot bot );
}

class PrGoalieBlueLine extends IPosReaction
{
	boolean IsGoalie( Player player )
	{
		int ship = player.getShipType();		
		return ship==7 || ship==8;
	}
	
	static void Warp( SubspaceBot bot, Player player, int x, int y )
	{
		bot.m_botAction.warpTo( player.getPlayerID(), x/16, y/16 );
	}
	
	void React( PlayerPosition event, SubspaceBot bot )
	{
		Player player = bot.m_botAction.getPlayer( event.getPlayerID() );
		
		if( !IsGoalie( player ) )
			return;
		
		int freq = player.getFrequency();
		int x = event.getXLocation();
		
		if( freq == 0 && x > Arena.BLUE_LINE_LEFT_X )
			Warp( bot, player, Arena.BLUE_LINE_WARP_LEFT_X, Arena.CENTER );
		else if( freq == 1 && x < Arena.BLUE_LINE_RIGHT_X )
			Warp( bot, player, Arena.BLUE_LINE_WARP_RIGHT_X, Arena.CENTER );
	}
}