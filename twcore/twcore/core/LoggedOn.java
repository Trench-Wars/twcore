package twcore.core;

public class LoggedOn extends SubspaceEvent
{

	public LoggedOn(ByteArray array)
	{
		m_eventType = EventRequester.LOGGED_ON; //sets the event type in the superclass
	}
}
