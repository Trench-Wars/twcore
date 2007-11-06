package twcore.core.game;

import static twcore.core.game.Ship.INTERNAL_SPECTATOR;
import static twcore.core.game.Ship.INTERNAL_WARBIRD;

import java.util.ArrayList;

import twcore.core.BotAction;

/**
 * A more flexible implementation of the ShipRestrictor interface, allows you
 * to define ship groups and assign a maximum value to each group.
 *
 * @author D1st0rt
 * @version 07.01.10
 */
public class CompoundRestrictor implements ShipRestrictor
{
	/** The fallback ship */
	private byte fallback;

	/** A list containing all of the defined groupings of ship types */
	private ArrayList<ShipGroup> groups;

	/** The BotAction object to send messages with */
	private BotAction m_botAction;

	/** Classes signed up for denied ship change notifications */
	private ArrayList<InvalidShipListener> listeners;

	/**
	 * Creates a new instance of CompoundRestrictor
	 */
	public CompoundRestrictor()
	{
		groups = new ArrayList<ShipGroup>();
		fallback = INTERNAL_SPECTATOR;
		m_botAction = BotAction.getBotAction();
		listeners = new ArrayList<InvalidShipListener>();
	}

	/**
	 * Adds a new group to the restriction set, replaces group already in the
	 * list if one with the same contained ships exists
	 * @param ships a list of ships in the group
	 * @param max the limit of players allowed in ships this group contains
	 */
	public void addGroup(byte[] ships, int max)
	{
		ShipGroup group = new ShipGroup(ships, max);
		int index = groups.indexOf(group);
		if(index != -1)
		{
			groups.set(index, group);
		}
		else
		{
			groups.add(group);
		}
	}

	/**
	 * Removes a group from the restriction set
	 * @param ships a list of ships in the group to remove
	 */
	public void removeGroup(byte[] ships)
	{
		int mask = 0;
		ShipGroup group = null;

		for(byte b : ships)
		{
			if(b >= INTERNAL_WARBIRD && b <= INTERNAL_SPECTATOR)
			{
				mask |= 1 << b;
			}
		}

		for(ShipGroup g : groups)
		{
			if(g.getMask() == mask)
			{
				group = g;
				break;
			}
		}

		groups.remove(group);
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

		for(ShipGroup group : groups)
		{
			int pMask = 1 << ship;
			if((group.getMask() & pMask) != 0)
			{
				short total = 0;
				for(byte i = INTERNAL_WARBIRD; i < INTERNAL_SPECTATOR; i++)
				{
					int mask = 1 << i;
					if((group.getMask() & mask) != 0)
					{
						total += team.getShipCount(i);
					}
				}

				if(total >= group.getMax())
				{
					allowed = false;
					break;
				}
			}
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

	/**
	 * Represents a grouping of ships and an associated limit.
	 *
	 * @author D1st0rt
	 * @version 06/12/27
	 */
	private class ShipGroup
	{
		/** The ships this group contains */
		private int mask;

		/** The maximum number of players allowed in this group */
		private int max;

		/**
		 * Creates a new instance of ShipGroup
		 * @param ships the ships in this group
		 * @param max the maximum number of players allowed in this group
		 */
		ShipGroup(byte[] ships, int max)
		{
			for(byte b : ships)
			{
				if(b >= INTERNAL_WARBIRD && b < INTERNAL_SPECTATOR)
				{
					mask |= 1 << b;
				}
			}

			this.max = max;
		}

		/**
		 * Gets the contained ship mask for this group
		 * @return an integer mask where the first 8 bits represent a ship
		 */
		int getMask()
		{
			return mask;
		}

		/**
		 * Gets the maximum player count for this group
		 * @return the maximum number of players allowed in this group
		 */
		int getMax()
		{
			return max;
		}

		/**
		 * Compares two ShipGroup objects for equality
		 * @return true if the two objects have the same mask, false otherwise
		 */
		public boolean equals(Object obj)
		{
			if(obj instanceof ShipGroup)
			{
				ShipGroup g = (ShipGroup)obj;
				return mask == g.getMask();
			}
			else
			{
				return false;
			}
		}
	}
}
