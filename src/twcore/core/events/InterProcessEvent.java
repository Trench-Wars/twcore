package twcore.core.events;

/**
    Event class of the Inter-process Communication protocol of TWCore.  Bots may
    request this event if they intend to send and receive IPC messages.

    @author  harvey
*/
public class InterProcessEvent extends SubspaceEvent {
    String m_senderName;        // Name of the sender of the message
    String m_channelName;       // Channel the message is being sent under
    Object m_o;                 // Object being sent (almost always IPCMessage)

    /** Creates a new instance of InterProcessEvent */
    public InterProcessEvent( String senderName, String channelName, Object o ) {
        m_o = o;
        m_senderName = senderName;
        m_channelName = channelName;
    }

    /**
        Returns the name of the channel this event is intended to broadcast over.

        @return Name of the channel
    */
    public String getChannel() {
        return m_channelName;
    }

    /**
        Returns the object being broadcast.  Note that while this is general
        enough to transmit any object, an IPCMessage is usually anticipated.
        A new class could be created to transmit more complex messages.

        @return Object being broadcast.
    */
    public Object getObject() {
        return m_o;
    }

    /**
        Returns the class type of the message (generally IPCMessage).
        @return Name of the class transmitted
    */
    public String getType() {
        return m_o.getClass().getName();
    }

    /**
        Returns the name of the message sender.
        @return Name of sender (potentially null)
    */
    public String getSenderName() {
        return m_senderName;
    }

}
