//////////////////////////////////////////////////////
//
// Filename:
// BotTask.java
//
// Description:
// Bot-dependent functions
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;

import twcore.core.*;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import java.util.*;

public class BotTask
{	
	public static SubspaceBot m_bot;
	
	public static void Warp( Player player, int x, int y )
	{
		m_bot.m_botAction.warpTo( player.getPlayerID(), x/16, y/16 );
	}
	
	public static void Warp( Player player, Position pos )
	{
		m_bot.m_botAction.warpTo( player.getPlayerID(), pos.getX()/16, pos.getY()/16 );
	}
	
	public static Player GetAnyPlayer()
	{
		Iterator it = m_bot.m_botAction.getPlayerIterator();
		
		while( it.hasNext() )
			return (Player)it.next();
		
		return null;
	}

	public static int GetNumFoOccupants( int freq )
	{
		int acc = 0;
		Iterator it = m_bot.m_botAction.getPlayerIterator();
		
		while( it.hasNext() )
		{
			Player player = (Player)it.next();
			
			if( player.getFrequency() == freq )
				if( Arena.IsInFoCircle( new Position( player.getXLocation(), player.getYLocation() ) ) )
					acc++;
		}

		return acc;
	}
	
	public static void PlacePuckInCenter( Player victim, Position grabPos )
	{		
		int oldShipType = victim.getShipType();
		int newShipType = oldShipType - 1;
		if( newShipType == 0 )
			newShipType = 5;		
		
		Prize( victim, Prize.ENGINE_SHUTDOWN );
		Warp( victim, Arena.CENTER-4, Arena.CENTER-4 );		
		try { Thread.sleep(500); } catch( Exception e ) { }
		m_bot.m_botAction.setShip( victim.getPlayerID(), newShipType );
		m_bot.m_botAction.setShip( victim.getPlayerID(), oldShipType );
		Warp( victim, grabPos );
	}
	
	public static Player GetPlayer( int playerId)
	{
		return m_bot.m_botAction.getPlayer( playerId );
	}
	
	public static void Prize( Player player, int prize )
	{
		m_bot.m_botAction.sendUnfilteredPrivateMessage( player.getPlayerName(), "*prize #-" + prize );
		m_bot.m_botAction.prize( player.getPlayerID(), prize );
	}
}

class Prize
{
	public static final int ENGINE_SHUTDOWN = 14;
}
