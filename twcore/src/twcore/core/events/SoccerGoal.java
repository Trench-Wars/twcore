package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x0B) Event fired when a Soccer Goal is made.<code><pre>

    +-----------------------------+
    |Offset Length Description    |
    +-----------------------------+
    |0        1    Type Byte      |
    |1        2    Score Frequency|
    |3        4    Team Points    |
    +-----------------------------+</code></pre>
*/
public class SoccerGoal extends SubspaceEvent {
    private short m_frequency; // Frequency of the team who scored a goal
    private int   m_reward;    // Reward won by scoring a goal

    /**
        Creates a new instance of SoccerGoal; this is called by
        GamePacketInterpreter when it receives the packet.
        @param array the ByteArray containing the packet data
    */
    public SoccerGoal( ByteArray array ) {
        m_frequency = array.readLittleEndianShort( 1 );
        m_reward    = array.readLittleEndianInt( 3 );
    }

    /**
        Gets the Frequency of the team who scored a Soccer Goal.
        @return Frequency
    */
    public short getFrequency() {
        return m_frequency;
    }

    /**
        This gets the Reward won.
        @return Reward
    */
    public int getReward() {
        return m_reward;
    }
}