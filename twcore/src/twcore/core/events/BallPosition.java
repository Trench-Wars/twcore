package twcore.core.events;

import twcore.core.util.ByteArray;

/**
 * (S2C 0x2E) Event fired when a player picks up a ball and periodically as
 * the ball moves/changes position.<code><pre>
 *
 * +-------------------------+
 * |Offset Length Description|
 * +-------------------------+
 * |0        1    Type Byte  |
 * |1        1    Ball ID    |
 * |2        2    X location |
 * |4        2    Y location |
 * |6        2    X Velocity |
 * |8        2    Y Velocity |
 * |10       2    Player ID  |
 * |12       4    Timestamp  |
 * +-------------------------+</code></pre>
 */
public class BallPosition extends SubspaceEvent {

    private byte m_ballID;     // ID of the ball picked up
    private short m_xLocation; // X location of the ball
    private short m_yLocation; // Y location of the ball
    private short m_xVelocity; // X velocity of the ball
    private short m_yVelocity; // Y velocity of the ball
    private short m_playerID;  // ID of the player who picked up/has the ball
    private int m_timeStamp;   // Time stamp of the ball
    private short m_carrier;      // Person carrying the ball

    /**
	 * Creates a new instance of BallPosition; this is called by
	 * GamePacketInterpreter when it receives the packet.
	 * @param array the ByteArray containing the packet data
	 */
    public BallPosition( ByteArray array ) {
        m_ballID = array.readByte( 1 );
        m_xLocation = array.readLittleEndianShort( 2 );
        m_yLocation = array.readLittleEndianShort( 4 );
        m_xVelocity = array.readLittleEndianShort( 6 );
        m_yVelocity = array.readLittleEndianShort( 8 );
        m_playerID = array.readLittleEndianShort( 10 );
        m_timeStamp = array.readInt( 12 );
        if(m_timeStamp == 0)
        	m_carrier = m_playerID;
        else
        	m_carrier = -1;
    }

    /**
     * Gets the ID of the ball.
     * @return BallID
     */
    public byte getBallID() {
        return m_ballID;
    }

    /**
     * Gets the X location of the ball.
     * @return Xlocation
     */
    public short getXLocation() {
        return m_xLocation;
    }

    /**
     * Gets the Y location of the ball.
     * @return Ylocation
     */
    public short getYLocation() {
        return m_yLocation;
    }

    /**
     * Gets the X velocity of the ball.
     * @return Xvelocity
     */
    public short getXVelocity() {
        return m_xVelocity;
    }

    /**
     * Gets the Y velocity of the ball.
     * @return Yvelocity
     */
    public short getYVelocity() {
        return m_yVelocity;
    }

    /**
     * Gets the ID of the last player that touched the ball.
     * @return PlayerID
     */
    public short getPlayerID() {
        return m_playerID;
    }

    /**
     * Gets the Time Stamp of the ball.
     * @return TimeStamp
     */
    public int getTimeStamp() {
        return m_timeStamp;
    }
    
    /**
     * Gets the ID of the person carrying the ball.
     * @return -1 if the ball is not being carried.
     */
    public short getCarrier() {
    	return m_carrier;
    }
}