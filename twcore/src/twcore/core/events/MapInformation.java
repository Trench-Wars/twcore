package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x29) Event fired when Map Information is received. <code><pre>
    +-----------------------------------------------------+
    | Offset  Length  Description                         |
    +-----------------------------------------------------+
    | 0       1       Type Byte                           |
    | 1       16      Map name                            |
    | 17      4       Map checksum                        |
    +-----------------------------------------------------+
    | The map size is optional. It's sent in subgame if   |
    | the client version is not 134, and ASSS may         |
    | potentially send it anyway.                         |
    +-----------------------------------------------------+
    | 21      4       Map size                            |
    +-----------------------------------------------------+
    | The following are optionally repeated until the end |
    | of the message.                                     |
    +-----------------------------------------------------+
    | 25      16      LVZ name                            |
    | 41      4       LVZ checksum                        |
    | 45      4       LVZ size                            |
    +-----------------------------------------------------+</code></pre>

    Original field research is credited to the folks from MervBot.
    Implementation idea is taken from the MervBot.
    @author Trancid
*/
public class MapInformation {
    private String m_mapName;       // Name of the map, including extension.
    private long m_mapChecksum;     // Checksum of the LVL file. Needs to be long due to signdness.
    private int m_mapSize;          // Size of the map.
    private String[] m_lvzNames;    // Names of the LVZ files, including extension.
    private int[] m_lvzChecksums;   // Checksums of each of the LVZ files.
    private int[] m_lvzSizes;       // Size of each LVZ file.
    private int m_lvzCount;         // Amount of LVZs.

    /** MapInformation constructor */
    public MapInformation( ByteArray array ) {
        m_mapName = array.readString(1, 16).toLowerCase();
        // Have to take the annoying route to ensure the checksum is an unsigned integer.
        m_mapChecksum = (((long)(array.readByte(20) & 0xff) << 24) | ((array.readByte(19) & 0xff) << 16)
                         | ((array.readByte(18) & 0xff) << 8) | (array.readByte(17) & 0xff));

        if(array.size() >= 25) {
            m_mapSize = array.readLittleEndianInt(21);
        } else {
            m_mapSize = -1;
        }

        m_lvzCount = (array.size() - 25) / 24;

        // LVZs aren't used in checksums, so ignoring the fancy stuff to make the checksum unsigned.
        if(m_lvzCount > 0) {
            m_lvzNames = new String[m_lvzCount];
            m_lvzChecksums = new int[m_lvzCount];
            m_lvzSizes = new int[m_lvzCount];

            for(int i = 0; i < m_lvzCount; i ++) {
                m_lvzNames[i] = array.readString(25 + i * 24, 16).toLowerCase();
                m_lvzChecksums[i] = array.readLittleEndianInt(41 + i * 24);
                m_lvzSizes[i] = array.readLittleEndianInt(45 + i * 24);
            }
        } else {
            m_lvzCount = 0;
            m_lvzNames = null;
            m_lvzChecksums = null;
            m_lvzSizes = null;
        }
    }

    /*
        Getters
    */
    public String getMapName() {
        return m_mapName;
    }


    public long getMapChecksum() {
        return m_mapChecksum;
    }


    public int getMapSize() {
        return m_mapSize;
    }

    public String getLvzName(int index) {
        if(index < 0 || index > m_lvzCount)
            return null;

        return m_lvzNames[index];
    }

    public String[] getLvzNames() {
        return m_lvzNames;
    }

    public int getLvzChecksum(int index) {
        if(index < 0 || index > m_lvzCount)
            return -1;

        return m_lvzChecksums[index];
    }

    public int[] getLvzChecksums() {
        return m_lvzChecksums;
    }

    public int getLvzSize(int index) {
        if(index < 0 || index > m_lvzCount)
            return -1;

        return m_lvzSizes[index];
    }

    public int[] getLvzSizes() {
        return m_lvzSizes;
    }



    public int getLvzCount() {
        return m_lvzCount;
    }

}
