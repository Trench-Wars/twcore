//Updated 6/25/04 by D1st0rt
package twcore.core;

public class TurretEvent extends SubspaceEvent {

    private short         m_attacher;
    private short         m_attachee;
    private boolean     isAttach; //True if attaching, false if detaching

    public TurretEvent( ByteArray array ){
        m_attacher = array.readLittleEndianShort( 1 );
        m_attachee = array.readLittleEndianShort( 3 );

        isAttach = ( m_attachee == -1 ? false : true );
    }

    public short getAttacherID() {
        return m_attacher;
    }

    public short getAttacheeID() {
        return m_attachee;
    }

    public boolean isAttaching() {
        return isAttach;
    }
}
