package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x06) Event fired when a player dies. <code><pre>
    +------------------------------+
    | Offset  Length  Description  |
    +------------------------------+
    | 0       1       Type Byte    |
    | 1       1       Death Green  |
    | 2       2       Killer ident |
    | 4       2       Killee ident |
    | 6       2       Bounty       |
    | 8       2       Flags        |
    +------------------------------+</code></pre>

    The death green is the ID of the green that is left by this players death.
    The bounty, along with the other kill score modifiers in settings are added
    to the players kill points. The flags are how many flags were transferred
    as a result of the kill.

    NOTE: When getting IDs of the killer or victim, they very occasionally may
    not match to an ID stored in Arena.  This is because one of the players may
    leave the arena or zone only a moment after the death event is fired, clearing
    their information from the Arena object, while still leaving what appears to be
    a valid ID in the PlayerDeath event.  It's therefore extremely important to
    check for a null value when attempting to reference by ID anything related to
    the Player object stored in Arena.  (This includes any getPlayer, getPlayerName,
    etc. checks from BotAction.)
*/
public class PlayerDeath extends SubspaceEvent {
    short     m_score;
    short     m_flags;
    short     m_killerID;
    short     m_killeeID;
    // byte     m_deathGreen;

    /**
        Creates a new instance of PlayerDeath, this is called by
        GamePacketInterpreter when it receives the packet
        @param array the ByteArray containing the packet data
    */
    public PlayerDeath( ByteArray array ) {
        // m_deathGreen = (byte)array.readByte( 1 );
        m_killerID = array.readLittleEndianShort( 2 );
        m_killeeID = array.readLittleEndianShort( 4 );
        m_score = array.readLittleEndianShort( 6 );
        m_flags = array.readLittleEndianShort( 8 );
    }

    /**
        Gets the ID of the player that did the killing.
        NOTE: When retrieving a Player based on the ID, check if the Player is null
        before using it.  The person in question may have left the arena, and if that
        is the case, their record will no longer exist inside Arena.
        @return killer's player ID
    */
    public short getKillerID() {
        return m_killerID;
    }

    /**
        Gets the ID of the player that got killed
        NOTE: When retrieving a Player based on the ID, check if the Player is null
        before using it.  The person in question may have left the arena, and if that
        is the case, their record will no longer exist inside Arena.
        @return dead player's player ID
    */
    public short getKilleeID() {
        return m_killeeID;
    }

    /**
        Gets the bounty of the player who was killed.  This is not the amount of bounty
        added to the player who made the kill, or the amount of points to add.
        @return Bounty of the player who was killed.
    */
    public short getKilledPlayerBounty() {
        return m_score;
    }

    /**
        Gets the number of flags transferred from the kill
        @return number of additional flags the killer now has
    */
    public short getFlagCount() {
        return m_flags;
    }
}
