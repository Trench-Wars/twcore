package twcore.core.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import twcore.core.util.ByteArray;
import twcore.core.util.Tools;

/**
 * A packet receiving queue.  Waits for a packet to be received from the socket
 * it was initialized from, and once received, adds it to the end of the queue.
 * Periodically the Session looks for available packets and requests any
 * @author Jeremy
 */
public class Receiver extends Thread {
    private DatagramSocket   	 m_socket;          // Connection to SS server
    private List<DatagramPacket> m_packets;         // Received packet queue
    private int              	 m_packetsReceived; // # packets received

    /**
     * Creates a new instance of Receiver, a packet receiving queue.
     * @param group ThreadGroup of this Session
     * @param socket Connection to the outside world
     */
    public Receiver( ThreadGroup group, DatagramSocket socket ){
        super( group, group.getName()+"-Receiver" );
        m_socket = socket;
        m_packetsReceived = 0;
        m_packets = Collections.synchronizedList( new LinkedList<DatagramPacket>() );
        start();
    }

    /**
     * @return Total number of packets received
     */
    public int getNumPacketsReceived(){

        return m_packetsReceived;
    }

    /**
     * @return True if the connection to the SS server is still active
     */
    public boolean isConnected(){

        return m_socket.isConnected();
    }

    /**
     * @return Next packet in the queue in the form of a ByteArray
     */
    public ByteArray get(){
        if( m_packets.isEmpty() == false ){
            return new ByteArray( m_packets.remove( 0 ) );
        } else {
            return null;
        }
    }

    /**
     * @return True if there are packets in the queue still waiting to be processed
     */
    public boolean containsMoreElements(){

        return !m_packets.isEmpty();
    }
    
    /**
     * @return The number of packets in the queue
     */
    public int getNumPacketsWaiting(){
    	
    	return m_packets.size();
    }

    /**
     * As long as the connection is maintained, attempts to receive a new
     * packet.  The DatagramSocket's receive(DatagramPacket) method will
     * block until a packet is received, at which point Receiver will add
     * the packet to the queue, increment the received packet counter, and
     * then attempt to receive the next packet.
     */
    public void run(){
        DatagramPacket      packet;

        try {
            m_socket.setSoTimeout( 30000 );
        } catch( IOException e ){
            Tools.printStackTrace( e );
            m_socket.disconnect();
        }

        while( m_socket.isConnected() == true && !interrupted() ){
            try {
                packet = new DatagramPacket( new byte[520], 520 );
                m_socket.receive( packet );
                m_packets.add( packet );
                m_packetsReceived++;
            } catch( IOException e ){
                m_socket.disconnect();
            }
        }
    }
}

