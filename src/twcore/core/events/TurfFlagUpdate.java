package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x22) Event fired for each individual flag when the status of all turfs flags is updated.

    NOTE: This event does not match the packet, but is actually the
    packet broken up into one event per flag.
*/
public class TurfFlagUpdate extends SubspaceEvent {
    int         m_flagId;          // ID of the flag
    short       m_frequency;       // Frequency Frequency that holds the flag; -1 if not held
    boolean     m_claimed = false; // True if Flag is claimed

    /**
        Creates a new instance of TurfFlagUpdate; this is called by
        GamePacketInterpreter when it receives the packet.
        @param array the ByteArray containing the packet data
    */
    public TurfFlagUpdate( ByteArray array, int flagId ) {
        m_flagId = flagId;
        m_frequency = array.readLittleEndianShort( 1 );

        if( m_frequency > -1 ) {
            m_claimed = true;
        }
    }

    /**
        Gets the ID of the flag.
        @return Flag ID
    */
    public int getFlagID() {
        return m_flagId;
    }

    /**
        Gets the frequency that holds the flag; -1 if not held.
        @return Frequency
    */
    public short getFrequency() {
        return m_frequency;
    }

    /**
        True if flag is claimed.
        @return Claimed
    */
    public boolean claimed() {
        return m_claimed;
    }
}