//Updated 6/25/04 by D1st0rt
package twcore.core;

public class TurretEvent extends SubspaceEvent {

    private int         m_attacher;
    private int         m_attachee;
    private boolean     isAttach; //True if attaching, false if detaching

    public TurretEvent( ByteArray array ){
        m_attacher = (int)array.readLittleEndianShort( 1 );
        m_attachee = (int)array.readLittleEndianShort( 3 );

        isAttach = ( m_attachee == -1 ? false : true );
    }

    public int getAttacherID() {
        return m_attacher;
    }

    public int getAttacheeID() {
        return m_attachee;
    }

    public boolean isAttaching() {
        return isAttach;
    }
}
