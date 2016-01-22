package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x12) Event called when a flag is dropped, reset, or otherwise put somewhere else on the map. <code>
    +-----------------------------+
    | Offset  Length  Description |
    +-----------------------------+
    | 0       1       Type Byte   |
    | 1       2       Flag ID     |
    | 3       2       X Location  |
    | 5       2       Y Location  |
    | 7       2       Owning Freq |
    +-----------------------------+</code>

    The Owning Frequency is -1 if unowned.
*/
public class FlagPosition extends SubspaceEvent {

    //Variable Declarations
    private short m_flagID;
    private short m_xLocation;
    private short m_yLocation;
    private short m_team;

    /**
        Creates a new instance of FlagPosition, this is called by GamePacketInterpreter
        when it receives the packet
        @param array the ByteArray containing the packet data
    */
    public FlagPosition( ByteArray array ) {
        m_flagID = array.readLittleEndianShort( 1 );
        m_xLocation = array.readLittleEndianShort( 3 );
        m_yLocation = array.readLittleEndianShort( 5 );
        m_team = array.readLittleEndianShort( 7 );
    }

    /**
        This gets the ID of the flag
        @return the ID of the flag
    */
    public short getFlagID() {
        return m_flagID;
    }

    /**
        This gets the X coordinate of the tile that the flag is located on
        @return the X coordinate of the flag
    */
    public short getXLocation() {
        return m_xLocation;
    }

    /**
        This gets the Y coordinate of the tile that the flag is located on
        @return the Y coordinate of the flag
    */
    public short getYLocation() {
        return m_yLocation;
    }

    /**
        This gets the frequency of the team that owns this flag
        @return the frequency that owns this flag, or -1 if unowned
    */
    public short getTeam() {
        return m_team;
    }
}
