package twcore.core;

public class ArenaJoined extends SubspaceEvent
{
	public ArenaJoined(ByteArray array)
	{
		m_byteArray = array;
		m_eventType = EventRequester.ARENA_JOINED; //sets the event type in the superclass
	}
}
