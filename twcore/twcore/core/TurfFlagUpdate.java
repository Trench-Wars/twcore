package twcore.core;

public class TurfFlagUpdate extends SubspaceEvent {

    int         m_flagId;
    short       m_frequency;
    boolean     m_claimed = false;

    public TurfFlagUpdate( ByteArray array, int flagId ){
        m_flagId = flagId;
        m_frequency = array.readLittleEndianShort( 1 );
        if( m_frequency > -1 ){
            m_claimed = true;
        }
    }

    public int getFlagID(){
        return m_flagId;
    }

    public short getFrequency(){
        return m_frequency;
    }

    public boolean claimed(){
        return m_claimed;
    }
}