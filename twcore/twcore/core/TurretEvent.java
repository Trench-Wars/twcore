package twcore.core;

public class TurretEvent extends SubspaceEvent
{

	private int m_attacher;
	private int m_attachee;

	public TurretEvent(ByteArray array)
	{
		m_eventType = EventRequester.TURRET_EVENT; //sets the event type in the superclass
		
		m_attacher = (int) array.readLittleEndianShort(1);
		m_attachee = (int) array.readLittleEndianShort(3);
	}

	public int getAttacherID()
	{
		return m_attacher;
	}

	public int getAttacheeID()
	{
		return m_attachee;
	}
}