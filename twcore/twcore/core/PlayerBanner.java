package twcore.core;


public class PlayerBanner extends SubspaceEvent {
    
    private short m_playerID;
    private byte[] m_banner;
    
    public PlayerBanner( ByteArray array ) {

        m_playerID = (short)array.readLittleEndianShort( 1 );
        m_banner = new byte[96];
        for( int i = 3; i < 99; i++ )
            m_banner[i-3] = array.readByte( i );
    }
    
    public short getPlayerID() {
        return m_playerID;
    }
    
    public byte[] getBanner() {
        return m_banner;
    }
}