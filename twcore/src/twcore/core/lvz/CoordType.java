package twcore.core.lvz;


/**
 * Lvz Screen Object relative positioning Reference Points
 * 
 * @author D1st0rt
 * @version 06.01.21
 */
public enum CoordType
{
    N, // Top left corner
	C, // Screen center
	B, // Bottom right corner
	S, // Stats box, lower right corner
	G, // Top right corner of specials
	F, // Bottom right corner of specials
	E, // Below energy bar & spec data
	T, // Top left corner of chat
	R, // Top left corner of radar
	O, // Top left corner of radar's text (clock/location)
	W, // Top left corner of weapons
	V; // Bottom left corner of weapons

	/**
	 * Gets a CoordType object from an ordinal (index) value
	 * @param ordinal the ordinal of the desired coord type
	 * @return the coord type for that ordinal, or null
	 */
	public CoordType fromOrdinal(short ordinal)
	{
		CoordType t = null;

		if(ordinal == 0)
		    t = CoordType.N;
		else if(ordinal == 1)
			t = CoordType.C;
		else if(ordinal == 2)
			t = CoordType.B;
		else if(ordinal == 3)
			t = CoordType.S;
		else if(ordinal == 4)
			t = CoordType.G;
		else if(ordinal == 5)
			t = CoordType.F;
		else if(ordinal == 6)
			t = CoordType.E;
		else if(ordinal == 7)
			t = CoordType.T;
		else if(ordinal == 8)
			t = CoordType.R;
		else if(ordinal == 9)
			t = CoordType.O;
		else if(ordinal == 10)
			t = CoordType.W;
		else if(ordinal == 11)
			t = CoordType.V;

		return t;
	}
};
