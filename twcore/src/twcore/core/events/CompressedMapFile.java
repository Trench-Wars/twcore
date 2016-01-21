package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x2A) Event fired when a compressed map file is received. <code><pre>
    +-----------------------------------------------------+
    | Offset  Length  Description                         |
    +-----------------------------------------------------+
    | 0       1       Type Byte                           |
    | 1       16      Map name                            |
    | 17      ...     Compressed map                      |
    +-----------------------------------------------------+</code></pre>

    Original field research is credited to the folks from MervBot.
    Implementation idea is taken from the MervBot.
    @author Trancid
*/
public class CompressedMapFile {
    private String m_mapName;                   // Map name.
    private ByteArray m_compressedMapData;      // The raw compressed data.
    private int m_mapSize;                      // Size of the compressed data.

    /** CompressedMapFile constructor */
    public CompressedMapFile( ByteArray array ) {
        m_mapSize = array.size() - 17;
        m_mapName = array.readString(1, 16).toLowerCase();
        m_compressedMapData = array.readByteArray(17, array.size() - 1);
    }

    /*
        Getters
    */
    public String getMapName() {
        return m_mapName;
    }

    public ByteArray getCompressedMapData() {
        return m_compressedMapData;
    }

    public int getMapSize() {
        return m_mapSize;
    }

}
