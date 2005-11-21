package twcore.core.events;

import twcore.core.util.ByteArray;

public class SoccerGoal extends SubspaceEvent {
    
    private short m_frequency;
    private int   m_reward;
    
    public SoccerGoal( ByteArray array ) {
        m_frequency = array.readLittleEndianShort( 1 );
        m_reward    = array.readLittleEndianInt( 3 );
    }
    
    public short getFrequency() {
        return m_frequency;
    }
    
    public int getReward() {
        return m_reward;
    }
}