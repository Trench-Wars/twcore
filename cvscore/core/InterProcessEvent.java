package twcore.core;

/*
 * InterProcessEvent.java
 *
 * Created on October 30, 2002, 7:56 AM
 */

/**
 *
 * @author  harvey
 */
public class InterProcessEvent extends SubspaceEvent {
    String m_senderName;
    String m_channelName;
    Object m_o;
    /** Creates a new instance of InterProcessEvent */
    public InterProcessEvent( String senderName, String channelName, Object o ) {
        m_o = o;
        m_senderName = senderName;
        m_channelName = channelName;
    }
    
    public String getChannel(){
        return m_channelName;
    }
    
    public Object getObject(){
        return m_o;
    }
    
    public String getType(){
        return m_o.getClass().getName();
    }
    
    public String getSenderName(){
        return m_senderName;
    }
    
}
