package twcore.core;

public class ScoreUpdate extends SubspaceEvent
{
    /*
    09 - Score update
    Field    Length    Description
    0        1        Type byte
    1        2        Player ident
    3        4        Flag points
    7        4        Kill points
    11       2        Wins
    13       2        Losses
    */
    int m_playerID;
    int m_flagPoints;
    int m_killPoints;
    int m_wins;
    int m_losses;

    public ScoreUpdate(ByteArray array)
    {
        m_byteArray = array;
        m_eventType = EventRequester.SCORE_UPDATE; //sets the event type in the superclass
        
        m_playerID = (int) array.readLittleEndianShort(1);
        m_flagPoints = array.readLittleEndianInt(3);
        m_killPoints = array.readLittleEndianInt(7);
        m_wins = (int) array.readLittleEndianShort(11);
        m_losses = (int) array.readLittleEndianShort(13);
    }

    public int getPlayerID()
    {
        return m_playerID;
    }
    public int getFlagPoints()
    {
        return m_flagPoints;
    }
    public int getKillPoints()
    {
        return m_killPoints;
    }
    public int getWins()
    {
        return m_wins;
    }
    public int getLosses()
    {
        return m_losses;
    }
}
