package twcore.core;

public class FlagDropped extends SubspaceEvent
{

    private int m_playerID;

    public FlagDropped(ByteArray array)
    {
        m_byteArray = array;
        m_eventType = EventRequester.FLAG_DROPPED; //sets the event type in the superclass
        
        m_playerID = (int) array.readLittleEndianShort(1);
    }

    public int getPlayerID()
    {
        return m_playerID;
    }
}