package twcore.core;

/* 13 - Flag Victory
Field    Length    Description
0        1        Type byte
1        2        Frequency
3        4       Points
 */
public class FlagVictory extends SubspaceEvent {

    int         m_reward;
    short       m_frequency;

    public FlagVictory( ByteArray array ){

        m_frequency = array.readLittleEndianShort( 1 );
        m_reward = array.readLittleEndianInt( 3 );
    }

    public short getFrequency(){
        return m_frequency;
    }

    public int getReward(){
        return m_reward;
    }

}
