package twcore.core.game;

import java.util.ArrayList;
import java.util.Arrays;

import twcore.core.BotAction;

/**
 * Basic implementation of the ShipRestrictor interface. Uses per-ship maximum
 * amounts based on the number of those ships already present on the player's
 * team to determine whether a player is allowed to switch or not.
 *
 * @author D1st0rt
 * @version 07.01.10
 */
public class BasicRestrictor implements ShipRestrictor
{
	/** The restrictions by ship */
	private int[] restrictions;

	/** The fallback ship */
	private byte fallback;

	/** The BotAction object to send messages with */
	private BotAction m_botAction;

	/** Classes signed up for denied ship change notifications */
	private ArrayList<InvalidShipListener> listeners;

	/**
	 * Creates a new instance of BasicRestrictor
	 */
	public BasicRestrictor()
	{
		restrictions = new int[8];
		Arrays.fill(restrictions, UNRESTRICTED);
		fallback = Ship.INTERNAL_SPECTATOR;
		m_botAction = BotAction.getBotAction();
		listeners = new ArrayList<InvalidShipListener>();
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
			for(InvalidShipListener l : listeners)
			{
				l.changeDenied(p, ship, team);
			}
		}
		return allowed;
	}

	public boolean canSwap(Player p1, Player p2, Team team)
	{
		boolean allowed = true;

		return allowed;
	}

	/**
	 * Sets the fallback ship for when a player fails a canSwitch check
	 * @param ship the new ship type to set players to
	 */
	public void setFallback(byte ship)
	{
		if(ship >= Ship.INTERNAL_WARBIRD && ship <= Ship.INTERNAL_SPECTATOR)
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

	/**
	 * Adds the specified InvalidShipListener to the event queue. It will now
	 * receive notification of denied ship change attempts.
	 * @param l the listener to add
	 */
	public void addListener(InvalidShipListener l)
	{
		if(l != null && !listeners.contains(l))
		{
			listeners.add(l);
		}
	}

	/**
	 * Adds the specified InvalidShipListener to the event queue. It will no
	 * longer recieve notification of denied ship change attempts.
	 * @param l the listener to remove
	 */
	public void removeListener(InvalidShipListener l)
	{
		if(l != null && listeners.contains(l))
		{
			listeners.remove(l);
		}
	}
}
