/*
 * RoboBot.java
 *
 * Created on December 12, 2001, 7:28 PM
 */

/**
 *
 * @author  Jeremy
 */
package twcore.core;
import java.net.*;
import java.util.*;
import java.io.*;

public class Sender extends Thread {
    private DatagramSocket   m_socket;
    private List             m_packets;
    private int              m_packetsSent;
    private List             m_highPriorityPackets;
    
    public Sender( ThreadGroup group, DatagramSocket socket ){
        super( group, "Sender" );
        m_socket = socket;
        m_packetsSent = 0;
        m_packets = Collections.synchronizedList( new LinkedList() );
        m_highPriorityPackets = Collections.synchronizedList( new LinkedList() );
        start();
    }
    
    public int getNumPacketsSent(){
        
        return m_packetsSent;
    }
    
    public void send( DatagramPacket packet ){
        m_packets.add( packet );
    }
    
    public void highPrioritySend( DatagramPacket packet ){
        
        m_highPriorityPackets.add( packet );
    }
    
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
            } catch( InterruptedException e ){
                return;
            } catch( Exception e ){
                Tools.printStackTrace( e );
            }
        }
    }
}
