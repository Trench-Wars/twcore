package twcore.core;

/* 13 - Flag Victory
Field    Length    Description
0        1        Type byte
1        2        Frequency
3        4       Points
 */
public class FlagReward extends SubspaceEvent
{
    int m_points;
    int m_frequency;

    public FlagReward(ByteArray array)
    {
        m_byteArray = array;
        m_eventType = EventRequester.FLAG_REWARD; //sets the event type in the superclass

        m_frequency = (int) array.readLittleEndianShort(1);
        m_points = (int) array.readLittleEndianShort(3);
    }

    public int getFrequency()
    {
        return m_frequency;
    }

    public int getPoints()
    {
        return m_points;
    }

}
