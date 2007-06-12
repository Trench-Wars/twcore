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

import java.util.Vector;

import twcore.core.SubspaceBot;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;

public class PosMonitor
{
	private Vector<PosReaction> m_reactions = new Vector<PosReaction>();

	public PosMonitor()
	{
		m_reactions.addElement( new PrGoalieBlueLine() );
		m_reactions.addElement( new PrFaceOff() );
	}

	public void SubmitEvent( PlayerPosition event, SubspaceBot bot )
	{
		for( int i=0; i<m_reactions.size(); i++ )
			( m_reactions.elementAt( i ) ).React( event, bot );
	}
}

abstract class PosReaction
{
	abstract void React( PlayerPosition event, SubspaceBot bot );
}

class PrFaceOff extends PosReaction
{

	void React( PlayerPosition event, SubspaceBot bot )
	{
		if( !State.Is( State.FACE_OFF ) )
			return;

		Position playerPos = new Position( event.getXLocation(), event.getYLocation() );
		Player player = bot.m_botAction.getPlayer( event.getPlayerID() );
		int freq = player.getFrequency();

		// Max 1 person from each team in the FO circle
		if( Arena.IsInFoCircle( playerPos ) )
		{
			if( ( freq == 0 ) && ( BotTask.GetNumFoOccupants( 0 ) ) > 1)
				BotTask.Warp( player, Arena.SINSPOT_LEFT );

			if( ( freq == 1 ) && ( BotTask.GetNumFoOccupants( 1 ) ) > 1)
				BotTask.Warp( player, Arena.SINSPOT_RIGHT );
		}

		if( ( freq == 0 ) && ( event.getXLocation() > Arena.CENTER ) )
			BotTask.Warp( player, Arena.SINSPOT_LEFT );

		if( ( freq == 1 ) && ( event.getXLocation() < Arena.CENTER ) )
			BotTask.Warp( player, Arena.SINSPOT_RIGHT );
	}
}

class PrGoalieBlueLine extends PosReaction
{
	boolean IsGoalie( Player player )
	{
		int ship = player.getShipType();
		return ship==7 || ship==8;
	}

	void React( PlayerPosition event, SubspaceBot bot )
	{
		Player player = bot.m_botAction.getPlayer( event.getPlayerID() );

		if( !IsGoalie( player ) )
			return;

		int freq = player.getFrequency();
		int x = event.getXLocation();

		if( freq == 0 && x > Arena.BLUE_LINE_LEFT_X )
			BotTask.Warp( player, Arena.BLUE_LINE_WARP_LEFT_X, Arena.CENTER );
		else if( freq == 1 && x < Arena.BLUE_LINE_RIGHT_X )
			BotTask.Warp( player, Arena.BLUE_LINE_WARP_RIGHT_X, Arena.CENTER );
	}
}