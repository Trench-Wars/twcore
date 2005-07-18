package twcore.core;

/**
 * (S2C 0x06) Event fired when a player dies. <code><pre>
 * +------------------------------+
 * | Offset  Length  Description  |
 * +------------------------------+
 * | 0       1       Type Byte    |
 * | 1       1       Death Green  |
 * | 2       2       Killer ident |
 * | 4       2       Killee ident |
 * | 6       2       Bounty       |
 * | 8       2       Flags        |
 * +------------------------------+</code></pre>
 *
 * The death green is the ID of the green that is left by this players death.
 * The bounty, along with the other kill score modifiers in settings are added
 * to the players kill points. The flags are how many flags were transferred
 * as a result of the kill.
 */
public class PlayerDeath extends SubspaceEvent {
    short     m_score;
    short     m_flags;
    short     m_killerID;
    short     m_killeeID;
    // byte     m_deathGreen;

    /**
     * Creates a new instance of PlayerDeath, this is called by
     * GamePacketInterpreter when it receives the packet
     * @param array the ByteArray containing the packet data
     */
    public PlayerDeath( ByteArray array ){
        // m_deathGreen = (byte)array.readByte( 1 );
        m_killerID = (short)array.readLittleEndianShort( 2 );
        m_killeeID = (short)array.readLittleEndianShort( 4 );
        m_score = (short)array.readLittleEndianShort( 6 );
        m_flags = (short)array.readLittleEndianShort( 8 );
    }

    /**
     * Gets the ID of the player that did the killing
     * @return killer's player ID
     */
    public short getKillerID(){
        return m_killerID;
    }

    /**
     * Gets the ID of the player that got killed
     * @return dead player's player ID
     */
    public short getKilleeID(){
        return m_killeeID;
    }

    /**
     * Gets the score resulting from the kill. NOTE: this currently
     * just returns the bounty and not the entire score.
     * @return the points gotten from the kill
     */
    public short getScore(){
        return m_score;
    }

    /**
     * Gets the number of flags transferred from the kill
     * @return number of additional flags the killer now has
     */
    public short getFlagCount(){
        return m_flags;
    }
}
