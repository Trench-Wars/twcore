package twcore.core;
public class TurfFlagUpdate extends SubspaceEvent
{

	int m_frequency;
	int m_flagId;
	boolean m_claimed = false;

	public TurfFlagUpdate(ByteArray array, int flagId)
	{
		m_eventType = EventRequester.TURF_FLAG_UPDATE; //sets the event type in the superclass

		m_flagId = flagId;
		m_frequency = (int) array.readLittleEndianShort(1);
		if (m_frequency > -1)
			m_claimed = true;
	}

	public int getFlagID()
	{
		return m_flagId;
	}
	public int getFrequency()
	{
		return m_frequency;
	}
	public boolean claimed()
	{
		return m_claimed;
	}
}