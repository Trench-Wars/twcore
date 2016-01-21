package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x23) Event called when a team gets periodic flag points from holding them. <code><pre>
    +--------------------------------+
    | Offset  Length  Description    |
    +--------------------------------+
    | 0       1       Type Byte      |
    | 1       2       Freq           |
    | 3       2       Points Awarded |
    +--------------------------------+</code></pre>

    It is important to note that the actual packet can contain more information
    by repeating the last two fields until all awards have been announced.
    The GamePacketInterpreter has already split it up into separate events for us though :D
*/
public class FlagReward extends SubspaceEvent {

    //Variable Declarations
    short         m_points;
    short         m_frequency;

    /**
        Creates a new instance of FlagReward, this is called by GamePacketInterpreter
        when it receives the packet
        @param array the ByteArray containing the packet data
    */
    public FlagReward( ByteArray array ) {

        m_frequency = array.readLittleEndianShort( 1 );
        m_points = array.readLittleEndianShort( 3 );
    }

    /**
        This gets the frequency that won the points
        @return the freq that won the points
    */
    public short getFrequency() {
        return m_frequency;
    }

    /**
        This gets the number of points won
        @return the number of points won
    */
    public short getPoints() {
        return m_points;
    }

}
