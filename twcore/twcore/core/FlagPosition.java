package twcore.core;

public class FlagPosition extends SubspaceEvent
{

    private int m_flagID;
    private int m_xLocation;
    private int m_yLocation;
    private int m_team;

    public FlagPosition(ByteArray array)
    {
        m_byteArray = array;
        m_eventType = EventRequester.FLAG_POSITION; //sets the event type in the superclass
        
        m_flagID = (int) array.readLittleEndianShort(1);
        m_xLocation = (int) array.readLittleEndianShort(3);
        m_yLocation = (int) array.readLittleEndianShort(5);
        m_team = (int) array.readLittleEndianShort(7);
    }

    public int getFlagID()
    {
        return m_flagID;
    }

    public int getXLocation()
    {
        return m_xLocation;
    }

    public int getYLocation()
    {
        return m_yLocation;
    }

    public int getTeam()
    {
        return m_team;
    }
}