package twcore.core;

public class FlagPosition extends SubspaceEvent {
    
    private short m_flagID;
    private short m_xLocation;
    private short m_yLocation;
    private short m_team;
    
    public FlagPosition( ByteArray array ) {
        m_flagID = array.readLittleEndianShort( 1 );
        m_xLocation = array.readLittleEndianShort( 3 );
        m_yLocation = array.readLittleEndianShort( 5 );
        m_team = array.readLittleEndianShort( 7 );
    }
    
    public short getFlagID() {
        return m_flagID;
    }
    
    public short getXLocation() {
        return m_xLocation;
    }
    
    public short getYLocation() {
        return m_yLocation;
    }
    
    public short getTeam() {
        return m_team;
    }
}