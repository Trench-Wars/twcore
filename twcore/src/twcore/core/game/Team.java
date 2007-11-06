package twcore.core.game;

import static twcore.core.game.Ship.INTERNAL_ALL;
import static twcore.core.game.Ship.INTERNAL_PLAYINGSHIP;
import static twcore.core.game.Ship.INTERNAL_SPECTATOR;
import static twcore.core.game.Ship.INTERNAL_WARBIRD;

import java.util.ArrayList;

import twcore.core.BotAction;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;

/**
 * Represents a team of players. Provides basic ship tracking by team
 * functionality that can be integrated with a ShipRestrictor to automatically
 * control ship access. Also allows team captains to be defined for use
 * elsewhere.
 *
 * @author D1st0rt
 * @version 06.12.27
 */
public class Team
{
	/** The players on this team */
	private ArrayList<Player> players;

	/** Cached array of Player objects for this team */
	private Player[] plist;

	/** This team's frequency */
	private short freq;

	/** A 2d array of players in rows by ship for this team */
	private Player[][] ships;

	/** The number of allowed players in each ship on this team */
	private ShipRestrictor restrict;

	/** Whether the cache is up to date or not */
	private boolean changed;

	/** BotAction used to resolve player id's */
	private BotAction m_botAction;

	/** The names of the team captains */
	private ArrayList<String> captains;

	/**
     * Creates a new instance of Team
     * @param freq the team's frequency
     */
	public Team(short freq)
	{
		if(freq < 0)
			this.freq = 0;
		else if(freq > 9999)
			this.freq = 999;
		else
			this.freq = freq;

		players = new ArrayList<Player>();
		changed = true;
		ships = new Player[8][0];
		plist = null;
		captains = new ArrayList<String>();
		m_botAction = BotAction.getBotAction();

		restrict = new ShipRestrictor()
		{
			public boolean canSwitch(Player p, byte ship, Team t)
			{
				return true;
			}

			public boolean canSwap(Player p1, Player p2, Team t)
			{
				return true;
			}

			public byte fallbackShip()
			{
				return Ship.INTERNAL_SPECTATOR;
			}
		};
	}

	/**
     * Resets this team to completely empty, just as when it was instantiated
     * This does not change the ShipRestrictor, however
     */
	public void reset()
	{
		players.clear();
		changed = true;
		plist = null;
		ships = new Player[8][0];
	}

	public short getFrequency()
	{
		return freq;
	}

	/**
     * Gets all players on this in a specific ship
     * @param the desired ship, can also be ALL or PLAYING
     * @return an array of all Players currently in that ship
     */
	public Player[] getPlayers(byte ship)
	{
		if(ship > -1 && ship < INTERNAL_PLAYINGSHIP)
		{
			return ships[ship];
		}
		else if(ship == INTERNAL_PLAYINGSHIP)
		{
			ArrayList<Player> list = new ArrayList<Player>();
			for(int i = INTERNAL_WARBIRD; i < INTERNAL_SPECTATOR; i++)
			{
				for(Player p : ships[i])
				{
					list.add(p);
				}
			}
			return list.toArray(new Player[list.size()]);
		}
		else if(ship == INTERNAL_ALL)
		{
			return getPlayers();
		}

		return null;
	}

	/**
     * Removes a player from this team
     * @param name the name of the player
     */
	public void removePlayer(String name)
	{
		Player p = getPlayer(name);
		if(p != null)
		{
			players.remove(p);
			changed = true;
			plist = null;
		}
	}

	/**
     * Gets all of the players on this team
     * @return an array of all Players currently on the team
     */
	public Player[] getPlayers()
	{
		if(plist == null || changed)
			plist = players.toArray(new Player[players.size()]);

		return plist;
	}

	/**
     * Gets a player by name
     * @param name the name of the desired player
     * @return the Player object for the player with that name, or null if not found
     */
	public Player getPlayer(String name)
	{
		Player[] list = getPlayers();
		for(int x = 0; x < list.length; x++)
		{
			if(list[x].toString().equals(name))
				return list[x];
		}
		return null;
	}

	/**
     * Gets the number of players on this team in a particular ship
     * @param the desired ship, can also be ALL or PLAYING
     */
	public int getShipCount(byte ship)
	{
		return getPlayers(ship).length;
	}

	/**
     * Gets a cached array of the ship distribution of this team. If the ship
     * distribution has changed since the last calculation, it recalculates.
     * @return a byte[] containing the number of players in each ship by array index
     */
	public Player[][] getShipDistribution()
	{
		return ships;
	}

	/**
	 * Makes a specified player a captain of this team
	 * @param name the name of the player to make captain
	 */
	public void addCaptain(String name)
	{
		if(!captains.contains(name))
		{
			captains.add(name);
		}
	}

	/**
	 * Removes a specified player from being a team captain
	 * @param name the name of the player to demote from captain
	 */
	public void removeCaptain(String name)
	{
		captains.remove(name);
	}

	/**
     * Determines whether a player is a team captain or not
     * @param name the name of the player to check
     * @return true if the player is a team captain, false otherwise
     */
	public boolean isCaptain(String name)
	{
		return captains.contains(name);
	}

	/**
	 * Sets the ShipRestrictor this team uses to restrict ship access. If
	 * restrictor is null, the team will be set to use a Restrictor that allows
	 * full access to all ships.
	 * @param restrictor the new ShipRestrictor to use
	 */
	public void setRestrictor(ShipRestrictor restrict)
	{
		if(restrict == null)
		{
			restrict = new ShipRestrictor()
			{
				public boolean canSwitch(Player p, byte ship, Team t)
				{
					return true;
				}

				public boolean canSwap(Player p1, Player p2, Team t)
				{
					return true;
				}

				public byte fallbackShip()
				{
					return Ship.INTERNAL_SPECTATOR;
				}
			};
		}
		else
		{
			this.restrict = restrict;
		}
	}

	/**
	 * Gets the ShipRestrictor used to restrict ships for this team
	 * @return ShipRestrictor object used by this team
	 */
	public ShipRestrictor getRestrictor()
	{
		return restrict;
	}

	/**
	 * Takes a player out of the ship distribution for a specified ship.
	 * @param ship the ship the player was previously in
	 * @param p the player to remove
	 */
	private void removeFromShip(byte ship, Player p)
	{
		if(ships[ship].length > 0)
		{
			Player[] temp = new Player[ships[ship].length - 1];
			int i = 0;
			try
			{
				for(Player pl : ships[ship])
				{
					if(pl != p)
					{
						temp[i] = p;
						i++;
					}
				}
				ships[ship] = temp;
			}
			catch(ArrayIndexOutOfBoundsException e)
			{
				//player wasn't actually on that ship list, ignore
			}
		}
	}

	/**
	 * Adds a player to the ship distribution list for a specified ship.
	 * @param ship the ship the player is in
	 * @param p the player to add
	 */
	private void addToShip(byte ship, Player p)
	{
		Player[] temp = ships[ship];
		ships[ship] = new Player[temp.length + 1];
		ships[ship][0] = p;
		System.arraycopy(temp, 0, ships[ship], 1, temp.length);
	}

	/**
	 * Gets the last ship the player is recorded as being in. This is done
	 * by checking each ship array to see which one contains the player.
	 * @param p the player to search for
	 * @return the ship the player was last in, or -1 if not found
	 */
	private byte getOldShip(Player p)
	{
		for(byte i = INTERNAL_WARBIRD; i < INTERNAL_SPECTATOR; i++)
		{
			for(Player pl : ships[i])
			{
				if(pl == p)
				{
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Updates the player list and ship distribution as necessary when a player
	 * enters.
	 */
	public void handleEvent(PlayerEntered event)
	{
		if(event.getTeam() == freq)
		{
			int id = event.getPlayerID();
			Player p = m_botAction.getPlayer(id);
			players.add(p);
			changed = true;

			if(restrict.canSwitch(p, Ship.typeToConstant(p.getShipType()), this))
			{
				addToShip(Ship.typeToConstant(p.getShipType()), p);
			}
			else
			{
				if(restrict.fallbackShip() == Ship.INTERNAL_SPECTATOR)
				{
					m_botAction.spec(id);
					m_botAction.spec(id);
				}
				else
				{
					m_botAction.setShip(id, restrict.fallbackShip());
				}
			}

		}
	}

	/**
	 * Updates the player list and ship distribution as necessary when a player
	 * leaves.
	 */
	public void handleEvent(PlayerLeft event)
	{
		Player p = m_botAction.getPlayer(event.getPlayerID());
		if(p.getFrequency() == freq)
		{
			players.remove(p);
			byte ship = getOldShip(p);
			if(ship != -1)
			{
				removeFromShip(ship, p);
				changed = true;
			}
		}
	}

	/**
	 * Updates the player list and ship distribution as necessary when a player
	 * changes frequencies.
	 */
	public void handleEvent(FrequencyChange event)
	{
		short id = event.getPlayerID();
		Player p = m_botAction.getPlayer(id);
		if(players.contains(p) && event.getFrequency() != freq)
		{
			players.remove(p);
			byte ship = getOldShip(p);
			if(ship != -1)
			{
				removeFromShip(ship, p);
				changed = true;
			}
		}
		else if(event.getFrequency() == freq)
		{
			if(!players.contains(p))
			{
				players.add(p);
				changed = true;
			}

			if(restrict.canSwitch(p, Ship.typeToConstant(p.getShipType()), this))
			{
				byte ship = getOldShip(p);
				if(ship != -1)
				{
					removeFromShip(ship, p);
				}

				addToShip(Ship.typeToConstant(p.getShipType()), p);
			}
			else
			{
				if(restrict.fallbackShip() == Ship.INTERNAL_SPECTATOR)
				{
					m_botAction.spec(id);
					m_botAction.spec(id);
				}
				else
				{
					m_botAction.setShip(id, restrict.fallbackShip());
				}
			}
		}
	}

	/**
	 * Updates the player list and ship distribution as necessary when a player
	 * changes ships/frequencies.
	 */
	public void handleEvent(FrequencyShipChange event)
	{
		short id = event.getPlayerID();
		Player p = m_botAction.getPlayer(id);
		if(players.contains(p) && event.getFrequency() != freq)
		{
			players.remove(p);
			byte ship = getOldShip(p);
			if(ship != -1)
			{
				removeFromShip(ship, p);
				changed = true;
			}
		}
		else if(event.getFrequency() == freq)
		{
			if(!players.contains(p))
			{
				players.add(p);
				changed = true;
			}

			byte ship = getOldShip(p);

			if(ship != -1)
			{
				removeFromShip(ship, p);
			}

			if(restrict.canSwitch(p, Ship.typeToConstant(p.getShipType()), this))
			{
				addToShip(Ship.typeToConstant(p.getShipType()), p);
			}
			else
			{
				if(restrict.fallbackShip() == Ship.INTERNAL_SPECTATOR)
				{
					m_botAction.spec(id);
					m_botAction.spec(id);
				}
				else
				{
					m_botAction.setShip(id, restrict.fallbackShip());
				}
			}
		}
	}
}
