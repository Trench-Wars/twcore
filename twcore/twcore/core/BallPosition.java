package twcore.core;


public class BallPosition extends SubspaceEvent {
	
	private int m_ballID;
	private int m_xLocation;
	private int m_yLocation;
	private int m_xVelocity;
	private int m_yVelocity;
	private int m_playerID;
	private int m_timeStamp;
	
	public BallPosition( ByteArray array ) {
		m_ballID = (int)array.readByte( 1 );
		m_xLocation = (int)array.readLittleEndianShort( 2 );
		m_yLocation = (int)array.readLittleEndianShort( 4 );
		m_xVelocity = (int)array.readLittleEndianShort( 6 );
		m_yVelocity = (int)array.readLittleEndianShort( 8 );
		m_playerID = (int)array.readLittleEndianShort( 10 );
		m_timeStamp = (int)array.readInt( 12 );
	}
	
	public int getBallID() {
		return m_ballID;
	}
	
	public int getXLocation() {
		return m_xLocation;
	}
	
	public int getYLocation() {
		return m_yLocation;
	}
	
	public int getXVelocity() {
		return m_xVelocity;
	}
	
	public int getYVelocity() {
		return m_yVelocity;
	}
	
	public int getPlayerID() {
		return m_playerID;
	}
	
	public int getTimeStamp() {
		return m_timeStamp;
	}
}