package twcore.core;

public class ScoreReset extends SubspaceEvent
{
    /*
    1A - Score reset
    Field    Length    Description
    0        1        Type byte
    1        2        Player ident
    
    */
    int m_playerID;

    public ScoreReset(ByteArray array)
    {
        m_byteArray = array;
        m_eventType = EventRequester.SCORE_RESET; //sets the event type in the superclass
        
        m_playerID = (int) array.readLittleEndianShort(1);
    }

    public int getPlayerID()
    {
        return m_playerID;
    }
}
