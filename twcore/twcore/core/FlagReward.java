package twcore.core;

/* 13 - Flag Victory
Field    Length    Description
0        1        Type byte
1        2        Frequency
3        4       Points
 */
public class FlagReward extends SubspaceEvent {

    short         m_points;
    short         m_frequency;

    public FlagReward( ByteArray array ){

        m_frequency = array.readLittleEndianShort( 1 );
        m_points = array.readLittleEndianShort( 3 );
    }

    public short getFrequency(){
        return m_frequency;
    }

    public short getPoints(){
        return m_points;
    }

}
