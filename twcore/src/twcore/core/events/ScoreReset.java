package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x1A) Event fired when a player's score is reset. <code><pre>

    +--------------------------+
    |Offset Length Description |
    +--------------------------+
    |0        1    Type byte   |
    |1        2    Player ident|
    +--------------------------+</code></pre>

    If the PlayerID is 0xFFFF all players' scores were reset.
*/
public class ScoreReset extends SubspaceEvent {
    short m_playerID; // ID of player whose score was reset

    /**
        Creates a new instance of ScoreReset; this is called by
        GamePacketInterpreter when it receives the packet.
        @param array the ByteArray containing the packet data
    */
    public ScoreReset(ByteArray array) {
        m_playerID = array.readLittleEndianShort( 1 );
    }

    /**
        Gets the ID of the player whose score was reset.
        @return PlayerID
    */
    public short getPlayerID() {
        return m_playerID;
    }
}

