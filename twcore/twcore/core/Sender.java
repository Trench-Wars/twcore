package twcore.core;

import java.net.*;
import java.util.*;

/**
 * A packet sending queue.  Packets are formatted properly from inside
 * GamePacketGenerator, and then added to Sender's queue to be sent out as
 * soon as possible.  Packets added as high priority are sent first.
 * 
 * @author Jeremy
 */
public class Sender extends Thread {
    private DatagramSocket   m_socket;              // Connection to server
    private List             m_packets;             // Regular send packet queue
    private int              m_packetsSent;         // # packets sent
    private List             m_highPriorityPackets; // High priority send packet queue

    /**
     * Creates a new instance of Sender, a packet sending queue.
     * @param group ThreadGroup of this Session
     * @param socket Connection to the outside world
     */
    public Sender( ThreadGroup group, DatagramSocket socket ){
        super( group, "Sender" );
        m_socket = socket;
        m_packetsSent = 0;
        m_packets = Collections.synchronizedList( new LinkedList() );
        m_highPriorityPackets = Collections.synchronizedList( new LinkedList() );
        start();
    }
    
    /**
     * @return Total number of packets that have been sent
     */
    public int getNumPacketsSent(){
        
        return m_packetsSent;
    }

    /**
     * @return True if the connection to the SS server is still active
     */
    public boolean isConnected(){

        return m_socket.isConnected();
    }

    /**
     * Initiates sending of a created packet by adding it to the send queue. 
     * @param packet Packet to send 
     */
    public void send( DatagramPacket packet ){
        m_packets.add( packet );
    }
    
    /**
     * Initiates sending of a high priority packet by adding it to the high
     * priority send queue. 
     * @param packet Packet to send 
     */
    public void highPrioritySend( DatagramPacket packet ){
        
        m_highPriorityPackets.add( packet );
    }
    
    /**
     * Checks the queue for packets waiting to be sent, and sends any that are
     * in waiting.
     */
    public void run(){
        while( m_socket.isConnected() == true && !interrupted()){
            try{
                if( m_highPriorityPackets.isEmpty() == false ){
                    m_socket.send( (DatagramPacket)m_highPriorityPackets.remove( 0 ));
                    m_packetsSent++;
                } else if( m_packets.isEmpty() == false ){
                    m_socket.send( (DatagramPacket)m_packets.remove( 0 ));
                    m_packetsSent++;
                }
 
                Thread.sleep( 5 );
            } catch( Exception e ){
                m_socket.disconnect();
            }
        }
    }
}

