package twcore.core;

public class FlagClaimed extends SubspaceEvent
{

	private int m_flagID;
	private int m_playerID;

	public FlagClaimed(ByteArray array)
	{
		m_byteArray = array;
		m_eventType = EventRequester.FLAG_CLAIMED; //sets the event type in the superclass
		
		m_flagID = (int) array.readLittleEndianShort(1);
		m_playerID = (int) array.readLittleEndianShort(3);
	}

	public int getFlagID()
	{
		return m_flagID;
	}

	public int getPlayerID()
	{
		return m_playerID;
	}
}