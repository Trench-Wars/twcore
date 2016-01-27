package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x16) Event called when a player drops a flag they are carrying. <code>
    +-----------------------------+
    | Offset  Length  Description |
    +-----------------------------+
    | 0       1       Type Byte   |
    | 1       2       Player ID   |
    +-----------------------------+</code>

    This event occurs for warzone flags, but players aren't updated as not carrying
    any warzone flags automatically in the core.
*/
public class FlagDropped extends SubspaceEvent {

    //Variable Declarations
    private short m_playerID;

    /**
        Creates a new instance of FlagDropped, this is called by GamePacketInterpreter
        when it receives the packet
        @param array the ByteArray containing the packet data
    */
    public FlagDropped( ByteArray array ) {
        m_playerID = array.readLittleEndianShort( 1 );
    }

    /**
        This gets the ID of the player that just dropped the flag
        @return the ID of the dropping player
    */
    public short getPlayerID() {
        return m_playerID;
    }
}
