package twcore.core;

/*04 - Player leaving
Field    Length    Description
0        1        Type byte
1        2        Player ident
 */
public class PlayerLeft extends SubspaceEvent
{
	private int m_playerID;

	public PlayerLeft(ByteArray array)
	{
		m_byteArray = array;
		m_eventType = EventRequester.PLAYER_LEFT; //sets the event type in the superclass

		m_playerID = (int) array.readLittleEndianShort(1);
	}


	public int getPlayerID()
	{
		return m_playerID;
	}
}
