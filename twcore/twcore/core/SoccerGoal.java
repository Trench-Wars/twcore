package twcore.core;

public class SoccerGoal extends SubspaceEvent
{

    private int m_frequency;
    private int m_reward;

    public SoccerGoal(ByteArray array)
    {
        m_eventType = EventRequester.SOCCER_GOAL; //sets the event type in the superclass
        
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