package twcore.core.events;

import twcore.core.util.ByteArray;


public class BallPosition extends SubspaceEvent {
    
    private byte m_ballID;
    private short m_xLocation;
    private short m_yLocation;
    private short m_xVelocity;
    private short m_yVelocity;
    private short m_playerID;
    private int m_timeStamp;
    
    public BallPosition( ByteArray array ) {
        m_ballID = array.readByte( 1 );
        m_xLocation = array.readLittleEndianShort( 2 );
        m_yLocation = array.readLittleEndianShort( 4 );
        m_xVelocity = array.readLittleEndianShort( 6 );
        m_yVelocity = array.readLittleEndianShort( 8 );
        m_playerID = array.readLittleEndianShort( 10 );
        m_timeStamp = array.readInt( 12 );
    }
    
    public byte getBallID() {
        return m_ballID;
    }
    
    public short getXLocation() {
        return m_xLocation;
    }
    
    public short getYLocation() {
        return m_yLocation;
    }
    
    public short getXVelocity() {
        return m_xVelocity;
    }
    
    public short getYVelocity() {
        return m_yVelocity;
    }
    
    public short getPlayerID() {
        return m_playerID;
    }
    
    public int getTimeStamp() {
        return m_timeStamp;
    }
}