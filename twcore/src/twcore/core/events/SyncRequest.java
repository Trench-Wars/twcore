package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x18) Event fired when a synchronization request is received. <code>
    +-----------------------------------------------------+
    | Offset  Length  Description                         |
    +-----------------------------------------------------+
    | 0       1       Type Byte                           |
    | 1       4       Green Seed                          |
    | 5       4       Door Seed                           |
    | 9       4       Timestamp                           |
    | 13      4       Checksum Generator Key              |
    +-----------------------------------------------------+</code>

    Original field research is credited to Kavar!.
    Implementation idea is taken from the MervBot.
    @author Trancid
*/
public class SyncRequest {
    private int m_greenSeed;        // Seed for greens.
    private int m_doorSeed;         // Seed for doors.
    private int m_timestamp;        // Timestamp when this was sent.
    private int m_checksumkey;      // The key used to generate the checksums.

    /** SyncRequest constructor */
    public SyncRequest( ByteArray array ) {
        m_greenSeed = array.readLittleEndianInt(1);
        m_doorSeed = array.readLittleEndianInt(5);
        m_timestamp = array.readLittleEndianInt(9);
        m_checksumkey = array.readLittleEndianInt(13);
    }

    /*
        Getters
    */
    public int getGreenSeed() {
        return m_greenSeed;
    }

    public int getDoorSeed() {
        return m_doorSeed;
    }

    public int getTimeStamp() {
        return m_timestamp;
    }

    public int getCheckSumKey() {
        return m_checksumkey;
    }

}
