//Updated 6/25/04 by D1st0rt
package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x0E) Event called when a player attaches or detaches from another.
    <code><pre>
    +------------------------------------------------+
    | Offset  Length  Description                    |
    +------------------------------------------------+
    | 0       1       Type Byte (0x0E)               |
    | 1       2       Turret Requester Player ID     |
    | 3       2       Turret Destination Player ID * |
    +------------------------------------------------+
    </code></pre>
 *   * Destination player ID will be set to -1 if this is a detach; otherwise
    it will be considered an attach.  As a result of this, some special
    handling must be done in order to properly tell a Player object that
    someone has detached from it, because the ID of the person detaching from
    is not provided.
*/

public class TurretEvent extends SubspaceEvent {

    private short         m_attacher;       // ID of the player attaching
    private short         m_attachee;       // ID of the player being attached to.
    // (-1 if this event is a detach.)

    /**
        Create a new instance of TurretEvent, given S2C 0x0E packet data
        stored in the form of a ByteArray.
    */
    public TurretEvent( ByteArray array ) {
        m_attacher = array.readLittleEndianShort( 1 );
        m_attachee = array.readLittleEndianShort( 3 );
    }

    /**
        @return ID of the person making the attach / detach
    */
    public short getAttacherID() {
        return m_attacher;
    }

    /**
        @return ID of the person being attached to; -1 if this is a detach
    */
    public short getAttacheeID() {
        return m_attachee;
    }

    /**
        @return True if this event is an attach; false if it is a detach
    */
    public boolean isAttaching() {
        return m_attachee != -1;
    }
}
