package twcore.core;

public abstract class SubspaceEvent {

    protected int m_eventType;
   	protected ByteArray m_byteArray;

    public int getEventType(){

        return m_eventType;
    }
    
	/**
	 * @return ByteArray the bytearray to form metapackets or store packets
	 * @author FoN
	 */
	public ByteArray getByteArray()
	{
		return m_byteArray;	
	}
}
