package twcore.core;

public class FileArrived extends SubspaceEvent
{

	String fileName;

	public FileArrived(ByteArray array)
	{
		m_byteArray = array;
		m_eventType = EventRequester.FILE_ARRIVED; //sets the event type in the superclass

		fileName = array.readString(0, array.size());
	}

	public String getFileName()
	{

		return new String(fileName);
	}
}
