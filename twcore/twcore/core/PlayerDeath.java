package twcore.core;

/*
06 - Player death
Field    Length    Description
0        1        Type byte
1        1        ?
2        2        Killer ident
4        2        Killee ident
6        2        Score to be added
8        2        ?
 */

public class PlayerDeath extends SubspaceEvent
{
	int m_random; //Unknown?
	int m_killerID;
	int m_killeeID;
	int m_score;
	int m_unknown; //What's this?

	public PlayerDeath(ByteArray array)
	{
		m_byteArray = array;
		m_eventType = EventRequester.PLAYER_DEATH; //set the eventtype in the super class
		
		m_random = (int) array.readByte(1);
		m_killerID = (int) array.readLittleEndianShort(2);
		m_killeeID = (int) array.readLittleEndianShort(4);
		m_score = (int) array.readLittleEndianShort(6);
		m_unknown = (int) array.readLittleEndianShort(8);
	}
	public int getKillerID()
	{
		return m_killerID;
	}
	public int getKilleeID()
	{
		return m_killeeID;
	}
	public int getScore()
	{
		return m_score;
	}
}
