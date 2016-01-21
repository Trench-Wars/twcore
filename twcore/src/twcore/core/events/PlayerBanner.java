package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x1F) Event fired when a player changes their banner. <code><pre>
    +------------------------------+
    | Offset  Length  Description  |
    +------------------------------+
    | 0       1       Type Byte    |
    | 1       2       Player ID    |
    | 2       96      Banner Data  |
    +------------------------------+</code></pre>

    Banner is sent as a 12x8 bitmap with no palette. Data arranged in left to
    right rows, but bottom row is the first in the read order
*/
public class PlayerBanner extends SubspaceEvent {

    //Variable Declarations
    private short m_playerID;
    private byte[] m_banner;

    /**
        Creates a new instance of PlayerBanner, this is called by
        GamePacketInterpreter when it receives the packet
        @param array the ByteArray containing the packet data
    */
    public PlayerBanner( ByteArray array ) {

        m_playerID = array.readLittleEndianShort( 1 );
        m_banner = new byte[96];

        for( int i = 3; i < 99; i++ )
            m_banner[i - 3] = array.readByte( i );
    }

    /**
        Gets the ID of the player that just changed their banner
        @return the changer's player ID
    */
    public short getPlayerID() {
        return m_playerID;
    }

    /**
        Gets the bitmap data of the banner
        @return 96 bytes of bitmappy goodness
    */
    public byte[] getBanner() {
        return m_banner;
    }
}