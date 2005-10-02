package twcore.bots.multibot.bship;

import java.util.BitSet;

public class BSPlayer
{
	private String _name;	
	
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
	public char[] ships;
	/** Aggregate rating */
	public int rating;
	
	public BSPlayer(String name)	
	{
		_name = name;
		ship = 0;
		deaths = 0;
		cskills = 0;
		tkills = 0;
		pkills = 0;
		takeoffs = 0;
		tacount = 0;
		rating = 0;
		ships = new char[8];
	}
	
	public String toString()
	{
		return _name;
	}	
}
