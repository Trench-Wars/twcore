package twcore.core;

/* 13 - Flag Victory
Field    Length    Description
0        1        Type byte
1        2        Frequency
3        4       Points
 */
public class FlagVictory extends SubspaceEvent
{

	int m_reward;
	int m_frequency;

	public FlagVictory(ByteArray array)
	{
		m_byteArray = array;
		m_eventType = EventRequester.FLAG_VICTORY; //sets the event type in the superclass

		m_frequency = (int) array.readLittleEndianShort(1);
		m_reward = (int) array.readLittleEndianInt(3);
	}

	public int getFrequency()
	{
		return m_frequency;
	}

	public int getReward()
	{
		return m_reward;
	}

}
