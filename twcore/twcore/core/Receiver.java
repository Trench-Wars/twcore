/*
 * Receiver.java
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

class Receiver extends Thread {
    private DatagramSocket   m_socket;
    private List             m_packets;
    private int              m_packetsReceived;
    
    public Receiver( ThreadGroup group, DatagramSocket socket ){
        super( group, "Receiver" );
        m_socket = socket;
        m_packetsReceived = 0;
        m_packets = Collections.synchronizedList( new LinkedList() );
        start();
    }
    
    public int getNumPacketsReceived(){
        
        return m_packetsReceived;
    }
        
    public ByteArray get(){
        if( m_packets.isEmpty() == false ){
            return new ByteArray( (DatagramPacket)m_packets.remove( 0 ) );
        } else {
            return null;
        }
    }
    
    public boolean containsMoreElements(){
        
        return !m_packets.isEmpty();
    }
    
    public void run(){
        DatagramPacket      packet;
        try{
            m_socket.setSoTimeout( 20000 );
        } catch( SocketException e ){
            return;
        }
        
        try {
            while( m_socket.isConnected() == true && !interrupted() ){
                
                packet = new DatagramPacket( new byte[520], 520 );
                m_socket.receive( packet );
                m_packets.add( packet );
                m_packetsReceived++;
            }
        } catch( IOException e ){
            //Tools.printStackTrace( e );
            Tools.printLog( "A bot has disconnected." );
            return;
        }
    }
}