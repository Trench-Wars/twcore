package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x0D) Event fired when a player changes frequencies.<code>

    +-------------------------+
    |Field Length Description |
    +-------------------------+
    |0       1    Type byte   |
    |1       2    Player ident|
    |3       2    Team        |
    |5       1    ?           |
    +-------------------------+</code>
*/
public class FrequencyChange extends SubspaceEvent {
    short             m_playerID;  // ID of player who changed freqs
    short             m_frequency; // Frequency the player changed to

    /**
        Creates a new instance of FrequencyChange; this is called by
        GamePacketInterpreter when it receives the packet.
        @param array the ByteArray containing the packet data
    */
    public FrequencyChange(ByteArray array) {
        m_playerID = array.readLittleEndianShort( 1 );
        m_frequency = array.readLittleEndianShort( 3 );
    }

    /**
        Gets the ID of the player who changed frequencies.
        @return PlayerID
    */
    public short getPlayerID() {
        return m_playerID;
    }

    /**
        Gets the Frequency that the player changed to.
        @return Frequency
    */
    public short getFrequency() {
        return m_frequency;
    }
}
