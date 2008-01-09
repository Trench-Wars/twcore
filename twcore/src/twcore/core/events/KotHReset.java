package twcore.core.events;

import twcore.core.util.ByteArray;

/**
 * (S2C 0x2C) Event fired when the King of the Hill game resets
 * 
 * <p>
 * This packet is send by the server when;
 * <ul>
 *  <li> the King of the Hill game (re)starts (player id=-1, timer value=?,timer=on)</li>
 *  <li> the King of the Hill game ends       (player id=-1, timer value=?,timer=off)</li>
 *  <li> a player dies and looses his crown   (player id=killed player, timer value=0, timer=off)</li>
 *  <li> a player depletes his timer          (player id=player, timer value=0, timer=off)</li>
 * </ul>
 * </p>
 * 
 * <code><pre>
 * +-------------------------------+
 * |Offset   Length Description    |
 * +-------------------------------+
 * |0        1    Type Byte  (0x2c)|
 * |1        1    KotH enabled     |
 * |2        4    KotH Timer Value |
 * |6        2    Player ID        |
 * +-------------------------------+</code></pre>
 */
public class KotHReset extends SubspaceEvent {
    
    private boolean enabled;    // whether King of the Hill is running for the specified player ID
    private int timer;          // King of the Hill timer ?? >0 when running, 0 when koth game is over for specified player
    private short playerID;     // specified player ID or -1 for all
    
    /**
	 * Creates a new instance of BallPosition; this is called by
	 * GamePacketInterpreter when it receives the packet.
	 * @param array the ByteArray containing the packet data
	 */
    public KotHReset( ByteArray array ) {
        if(array.readByte( 1 ) == 1) {
            enabled = true;
        } else {
            enabled = false;
        }
        
        timer = array.readInt( 2 );
        playerID = array.readLittleEndianShort( 6 );
    }

    /**
     * @return whether King of the Hill game is running or not
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * @return
     */
    public int getTimer() {
        return timer;
    }
    
    /**
     * The specified player id
     * @return
     */
    public short getPlayerID() {
        return playerID;
    }    
}