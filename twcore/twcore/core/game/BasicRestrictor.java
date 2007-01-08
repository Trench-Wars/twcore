package twcore.core.game;

import twcore.core.BotAction;
import java.util.*;

/**
 * Basic implementation of the ShipRestrictor interface. Uses per-ship maximum
 * amounts based on the number of those ships already present on the player's
 * team to determine whether a player is allowed to switch or not.
 *
 * @author D1st0rt
 * @version 06.12.27
 */
public class BasicRestrictor implements ShipRestrictor
{
	/** The restrictions by ship */
	private int[] restrictions;

	/** The fallback ship */
	private byte fallback;

	/** The BotAction object to send messages with */
	private BotAction m_botAction;

	/**
	 * Creates a new instance of BasicRestrictor
	 */
	public BasicRestrictor()
	{
		restrictions = new int[8];
		Arrays.fill(restrictions, UNRESTRICTED);
		fallback = Ship.SPEC;
		m_botAction = BotAction.getBotAction();
	}

	/**
	 * Creates a new instance of BasicRestrictor with pre-defined limits
	 * @param limits the ship limits to use
	 */
	public BasicRestrictor(int[] limits)
	{
		this();
		for(int i = 0; i < limits.length && i < 8; i++)
		{
			restrictions[i] = limits[i];
		}
	}

	/**
	 * Gets the maximum number of players allowed in a particular ship
	 * @param ship the ship type to check
	 * @return the ship count limit, or ShipRestrictor.UNRESTRICTED
	 */
	public int getMaximum(byte ship)
	{
		int max = UNRESTRICTED;

		if(ship >= 0 && ship < 8)
		{
			max = restrictions[ship];
		}

		return max;
	}

	/**
	 * Sets the maximum number of players allowed in a particular ship
	 * @param ship the ship type to set the limit for
	 * @param max the maximum number of players allowed in that ship
	 */
	public void setMaximum(byte ship, int max)
	{
		if(ship >= 0 && ship < 8)
		{
			restrictions[ship] = max;
		}
	}

	/**
	 * Determines whether a player can switch to a particular ship or not
	 * @param p the player in question
	 * @param ship the ship the player wishes to switch to
	 * @param team the player's team
	 * @return true if the player is allowed to switch, false otherwise
	 */
	public boolean canSwitch(Player p, byte ship, Team team)
	{
		boolean allowed = true;

		if(ship >= 0 && ship < 8 && restrictions[ship] > UNRESTRICTED)
		{
			allowed = team.getShipCount(ship) < restrictions[ship];
		}

		if(!allowed)
		{
			m_botAction.sendPrivateMessage(p.getPlayerID(), "You are not permitted in that ship.");
		}
		return allowed;
	}

	/**
	 * Sets the fallback ship for when a player fails a canSwitch check
	 * @param ship the new ship type to set players to
	 */
	public void setFallback(byte ship)
	{
		if(ship >= Ship.WARBIRD && ship <= Ship.SPEC)
		{
			fallback = ship;
		}
	}

	/**
	 * Gets the ship to set the player in when they are not allowed to switch
	 * @return a ship value from 0-7, or 8 for spectator
	 */
	public byte fallbackShip()
	{
		return fallback;
	}
}
