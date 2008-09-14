package twcore.core.net;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import twcore.core.util.ByteArray;
import twcore.core.util.Tools;

/**
 * Handles sending and receipt of reliable packets.  Reliable packets are packets
 * that require the receiver to return an ACKnowledgement packet.  The ACK packet
 * contains an ID specified in the head of the reliable packet that lets the sender
 * know the packet went through okay.  If an ACK is not received, the reliable
 * packet must be resent.
 */
public class ReliablePacketHandler {

    Map<Integer, ReliablePacket> m_sentPackets;        // (Integer)ACK ID -> ReliablePacket
    Map<Integer, ByteArray> m_receivedPackets;    // (Integer)ACK ID -> ByteArray
    int                     m_nextOutboundAck;    // ACK ID of next outbound packet
    int                     m_expectedReliable;   // ACK ID of the next expected packet

    SSEncryption            m_ssEncryption;       // Encryption class
    GamePacketGenerator     m_packetGenerator;    // Packet generator
    GamePacketInterpreter   m_packetInterpreter;  // Packet interpreter

    /**
     * Creates a new instance of ReliablePacketHandler.
     * @param packetGenerator Packet generator
     * @param packetInterpreter Packet interpreter
     * @param ssEncryption Encryption alg
     */
    public ReliablePacketHandler( GamePacketGenerator packetGenerator, GamePacketInterpreter packetInterpreter, SSEncryption ssEncryption ){

        m_nextOutboundAck = 0;
        m_expectedReliable = 0;
        m_ssEncryption = ssEncryption;
        m_packetGenerator = packetGenerator;
        m_packetInterpreter = packetInterpreter;
        m_sentPackets = Collections.synchronizedMap( new HashMap<Integer, ReliablePacket>() );
        m_receivedPackets = Collections.synchronizedMap( new HashMap<Integer, ByteArray>() );
    }

    /**
     * Constructs and encrypts an outgoing message, and assigns it the next
     * available ACK ID.
     * @param message Reliable packet to be handled
     */
    public void sendReliableMessage( ByteArray message ){

        ByteArray           outgoingPacket = new ByteArray( message.size() + 6 );

        outgoingPacket.addByte( 0x00 );
        outgoingPacket.addByte( 0x03 );
        outgoingPacket.addLittleEndianInt( m_nextOutboundAck );
        outgoingPacket.addByteArray( message );
        m_ssEncryption.encrypt( outgoingPacket, outgoingPacket.size() - 2, 2 );

        m_packetGenerator.composeImmediatePacket( outgoingPacket, outgoingPacket.size() );
        m_sentPackets.put( new Integer( m_nextOutboundAck ), new ReliablePacket( outgoingPacket ) );
        m_nextOutboundAck++;
    }

    /**
     * Handles received packets.  First sends an ACK in response to the packet
     * with the specified ACK ID, and then checks to see if that same ID
     * corresponds to the next packet it expects to receive.  If not, the packet
     * has arrived out of order, and is placed in a queue to be processed after
     * earlier packets have arrived.  If on the other hand the ID matches the
     * expected ID, the packet is sent off to be interpreted, as are any other
     * packets that were received out of order before this one.
     * @param message Reliable packet to be handled
     */
    public void handleReliableMessage( ByteArray message ){

        ByteArray  subMessage = null;
        int        receivedAck;

        receivedAck = message.readLittleEndianInt( 2 );
        m_packetGenerator.sendAck( receivedAck );

        if( receivedAck == m_expectedReliable ){
            m_expectedReliable++;

            subMessage = new ByteArray( message.size() - 6 );
            subMessage.addPartialByteArray( message, 0, 6, message.size() - 6 );
            m_packetInterpreter.translateGamePacket( subMessage, true );
            
            
            int index = subMessage.readByte( 0 );
            Tools.printConnectionLog("RECV BI : (0x03) Containing packet 0x" + (index<10?"0":"") + index);
            

            while((subMessage = m_receivedPackets.remove(new Integer(m_expectedReliable)))
            		!= null) {
                m_expectedReliable++;
                m_packetInterpreter.translateGamePacket( subMessage, true );
            }
        } else if( receivedAck > m_expectedReliable ){
            subMessage = new ByteArray( message.size() - 6 );
            subMessage.addPartialByteArray( message, 0, 6, message.size() - 6 );
            m_receivedPackets.put( new Integer( receivedAck ), subMessage );
        }
    }

    /**
     * Handles a received ACK message in response to a packet the bot sent.
     * When the ACK is received, the reliable packet has gone through without
     * a problem, and the sent packet is removed from the potential resend list.
     * @param message ACK message received
     */
    public void handleAckMessage( ByteArray message ){
        int        receivedAck;

        receivedAck = message.readLittleEndianInt( 2 );
        m_sentPackets.remove( new Integer( receivedAck ) );
    }

    /**
     * Resends all packets that have not been ACKnowledged within the amount of
     * time specified in the method's internal final RESEND_DELAY. (default 3000ms)
     */
    public void resendUnackedPacket(){
        Integer         i;
        ByteArray       packet;
        ReliablePacket  message;
        Iterator<Integer>        iterator;
        long            currentTime = System.currentTimeMillis();

        final int       RESEND_DELAY = 3000;

        if( m_sentPackets.isEmpty() == false ){
            synchronized (m_sentPackets) {
                iterator = m_sentPackets.keySet().iterator();

                while( iterator.hasNext() == true ){
                    i = (Integer)iterator.next();
                    message = m_sentPackets.get( i );
                    if( message != null ){
                        if( message.getTimeSent() + RESEND_DELAY < currentTime ){
                            packet = message.getPacket();
                            m_packetGenerator.composeHighPriorityPacket( packet, packet.size() );
                            message.setTimeSent( currentTime );
                        }
                    }
                }
            }
        }
    }

    /**
     * ReliablePacket internal data class.
     */
    class ReliablePacket {

        long            m_timeSent;
        ByteArray       m_packet;

        /**
         * Creates a new instance of ReliablePacket.
         * @param packet Packet data
         */
        ReliablePacket( ByteArray packet ){

            m_packet = packet;
            m_timeSent = System.currentTimeMillis();
        }

        /**
         * @return Time when the packet was sent, in milliseconds since Jan 1, 1970
         */
        long getTimeSent(){

            return m_timeSent;
        }

        /**
         * @return Contents of the packet
         */
        ByteArray getPacket(){

            return m_packet;
        }

        /**
         * Set when the packet was sent.
         * @param newTimeSent Send time in milliseconds since Jan 1, 1970
         */
        void setTimeSent( long newTimeSent ){

            m_timeSent = newTimeSent;
        }
    }
}
