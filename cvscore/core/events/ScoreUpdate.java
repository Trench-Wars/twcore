package twcore.core.events;

import twcore.core.util.ByteArray;

public class ScoreUpdate extends SubspaceEvent {
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
    short m_playerID;
    int   m_flagPoints;
    int   m_killPoints;
    short m_wins;
    short m_losses;

    public ScoreUpdate(ByteArray array){
        m_playerID = array.readLittleEndianShort( 1 );
        m_flagPoints = array.readLittleEndianInt( 3 );
        m_killPoints = array.readLittleEndianInt( 7 );
        m_wins = array.readLittleEndianShort( 11 );
        m_losses = array.readLittleEndianShort( 13 );
    }

    public short getPlayerID(){
        return m_playerID;
    }
    public int getFlagPoints(){
        return m_flagPoints;
    }
    public int getKillPoints(){
        return m_killPoints;
    }
    public short getWins(){
        return m_wins;
    }
    public short getLosses(){
        return m_losses;
    }
}
