package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x13) Event called when a player picks up or runs over a flag. <code>
    +-----------------------------+
    | Offset  Length  Description |
    +-----------------------------+
    | 0       1       Type Byte   |
    | 1       2       Flag ID     |
    | 3       2       Player ID   |
    +-----------------------------+</code>

    This event occurs for both turf and warzone flags, but players aren't updated as carrying
    any warzone flags automatically in the core.
*/
public class FlagClaimed extends SubspaceEvent {

    //Variable Declarations
    private short m_flagID;
    private short m_playerID;

    /**
        Creates a new instance of FlagClaimed, this is called by GamePacketInterpreter
        when it recieves the packet.
        @param array the ByteArray containing the packet data
    */
    public FlagClaimed( ByteArray array ) {
        m_flagID = array.readLittleEndianShort( 1 );
        m_playerID = array.readLittleEndianShort( 3 );
    }

    /**
        This gets the ID of the flag that was just claimed
        @return the ID of the claimed flag
    */
    public short getFlagID() {
        return m_flagID;
    }

    /**
        This gets the ID of the player that just claimed the flag
        @return the ID of the claiming player
    */
    public short getPlayerID() {
        return m_playerID;
    }
}
