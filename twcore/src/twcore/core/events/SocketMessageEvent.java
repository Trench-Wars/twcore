package twcore.core.events;

/**
 * Event class of the Socket Communication protocol of TWCore.  Bots may
 * request this event if they want to receive requests from a socket.
 *
 * @author  arobas+ (code from IPC)
 */
public class SocketMessageEvent extends SubspaceEvent implements Runnable {
	
	private final static long TIMEOUT = 5000;
	
    String m_senderName;        // Name of the sender of the message
    String m_channelName;       // Channel the message is being sent under
    String m_request;			// Request
    String m_response;            // Answer
    
    boolean running = true;
    
    /** Creates a new instance of SocketMessageEvent */
    public SocketMessageEvent( String senderName, String channelName, String request ) {
        m_request = request;
        m_response = "";
        m_senderName = senderName;
        m_channelName = channelName;
    }
    
    /**
     * Returns the name of the channel this event is intended to broadcast over.
     * 
     * @return Name of the channel
     */
    public String getChannel(){
        return m_channelName;
    }

    public String getRequest(){
        return m_request;
    }
    
    public String getResponse(){
        return m_response;
    }
    
    public void setResponse(String response){
        this.m_response = response;
        this.running = false;
    }
 
    public String getSenderName(){
        return m_senderName;
    }

	@Override
	public void run() {
		long start = System.currentTimeMillis();
		while(running) {
			if (System.currentTimeMillis()-start > TIMEOUT)
				setResponse("TIMEOUT");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) { }
		}
		synchronized (this) {
			notify();
		}
		
	}
    
}
