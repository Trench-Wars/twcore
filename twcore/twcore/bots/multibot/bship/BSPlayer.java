package twcore.bots.multibot.bship;

/**
 * Represents a player in the main battleship game and stores statistics
 * related to the game in progress.
 *
 * @author D1st0rt
 * @version 06.01.04
 */
public class BSPlayer
{
	/** Player's name */
	private String _name;

	/** Player's frequency */
	private short _freq;

	/** Player's current ship */
	public byte ship;

	/** Capital ship kills */
	public short cskills;

	/** Turret kills */
	public short tkills;

	/** Plane kills */
	public short pkills;

	/** Capital ship kills */
	public short deaths;

	/** Lives left (capships) */
	public short lives;

	/** Times attached to a capital ship (turret/plane) */
	public short takeoffs;

	/** Times attached to (capships) */
	public short tacount;

	/** Ships played by this player */
	public boolean[] ships;

	/** Aggregate rating */
	public int rating;

	/** Used to prevent being added to a team when inappropriate */
	public boolean locked;

	/**
     * Creates a new instance of BSPlayer
     * @param name the Player's name
     */
	public BSPlayer(String name, int freq)
	{
		_name = name;
		resetStats();
	}

	/**
     * Returns the player's name as a string
     * @return a string containing the player's name
     */
	public String toString()
	{
		return _name;
	}

	public short getFreq()
	{
		return _freq;
	}

	/**
	 * Sets all statistics for this player back to 0
	 */
	public void resetStats()
	{
		ship = 0;
		deaths = 0;
		lives = 0;
		cskills = 0;
		tkills = 0;
		pkills = 0;
		takeoffs = 0;
		tacount = 0;
		rating = 0;
		ships = new boolean[8];
	}

	/**
     * Gets a string of all ships played by this player. Displays the ship
     * number in it's 0-7 index if it has been used.
     */
	public String shipsPlayed()
	{
		String s = "";
		for(int x = 0; x < ships.length; x++)
		{
			if(ships[x])
				s += (x+1);
			else
				s += " ";
		}
		return s;
	}
}
