package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x14) Event called when a team wins a flag jackpot. <code>
    +------------------------------+
    | Offset  Length  Description  |
    +------------------------------+
    | 0       1       Type Byte    |
    | 2       2       Frequency    |
    | 3       4       Jackpot size |
    +------------------------------+</code>

    This event also resets ships of the winning frequency and warzone flag locations
*/
public class FlagVictory extends SubspaceEvent {

    //Variable Declarations
    int         m_reward;
    short       m_frequency;

    /**
        Creates a new instance of FlagVictory, this is called by GamePacketInterpreter
        when it receives the packet
        @param array the ByteArray containing the packet data
    */
    public FlagVictory( ByteArray array ) {

        m_frequency = array.readLittleEndianShort( 1 );
        m_reward = array.readLittleEndianInt( 3 );
    }

    /**
        This gets the frequency that won the points
        @return the freq that won the points
    */
    public short getFrequency() {
        return m_frequency;
    }

    /**
        This gets the size of the jackpot (number of points won)
        @return the jackpot size
    */
    public int getReward() {
        return m_reward;
    }

}
