package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x04) Event is fired when a player leaves an arena. <code><pre>.

    +--------------------------+
    |Field Length Description  |
    +--------------------------+
    |0        1    Type byte   |
    |1        2    Player ident|
    +--------------------------+>/code></pre>
*/
public class PlayerLeft extends SubspaceEvent {
    short             m_playerID; // ID of the player who left an arena

    /**
        Creates a new instance of PlayerLeft; this is called by
        GamePacketInterpreter when it receives the packet.
        @param array the ByteArray containing the packet data
    */
    public PlayerLeft( ByteArray array ) {
        m_playerID = array.readLittleEndianShort( 1 );
    }

    /**
        Gets the ID of the player who left an arena.
        @return PlayerID
    */
    public short getPlayerID() {
        return m_playerID;
    }
}
