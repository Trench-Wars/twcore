package twcore.core;

import java.util.*;

public class ReliablePacketHandler {

    Map                     m_sentPackets;
    Map                     m_receivedPackets;
    int                     m_nextOutboundAck;
    int                     m_expectedReliable;

    SSEncryption            m_ssEncryption;
    GamePacketGenerator     m_packetGenerator;
    GamePacketInterpreter   m_packetInterpreter;

    public ReliablePacketHandler( GamePacketGenerator packetGenerator, GamePacketInterpreter packetInterpreter, SSEncryption ssEncryption ){

        m_nextOutboundAck = 0;
        m_expectedReliable = 0;
        m_ssEncryption = ssEncryption;
        m_packetGenerator = packetGenerator;
        m_packetInterpreter = packetInterpreter;
        m_sentPackets = Collections.synchronizedMap( new HashMap() );
        m_receivedPackets = Collections.synchronizedMap( new HashMap() );
    }

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

    public void handleReliableMessage( ByteArray message ){
        
        ByteArray  subMessage;
        int        receivedAck;
        
        receivedAck = message.readLittleEndianInt( 2 );
        m_packetGenerator.sendAck( receivedAck );
                
        if( receivedAck == m_expectedReliable ){
            m_expectedReliable++;

            subMessage = new ByteArray( message.size() - 6 );
            subMessage.addPartialByteArray( message, 0, 6, message.size() - 6 );
            m_packetInterpreter.translateGamePacket( subMessage, true );

            while( (subMessage = (ByteArray)m_receivedPackets.get( new Integer( m_expectedReliable ) )) != null ){
                m_expectedReliable++;
                m_packetInterpreter.translateGamePacket( subMessage, true );
            }
        } else if( receivedAck > m_expectedReliable ){
            subMessage = new ByteArray( message.size() - 6 );
            subMessage.addPartialByteArray( message, 0, 6, message.size() - 6 );
            m_receivedPackets.put( new Integer( receivedAck ), subMessage );
        }
    }

    public void handleAckMessage( ByteArray message ){
        int        receivedAck;

        receivedAck = message.readLittleEndianInt( 2 );
        m_sentPackets.remove( new Integer( receivedAck ) );
    }

    public void resendUnackedPacket(){
        Integer         i;
        ByteArray       packet;
        ReliablePacket  message;
        Iterator        iterator;
        long            currentTime = System.currentTimeMillis();

        final int       RESEND_DELAY = 3000;

        if( m_sentPackets.isEmpty() == false ){
            iterator = m_sentPackets.keySet().iterator();

            while( iterator.hasNext() == true ){
                i = (Integer)iterator.next();
                message = (ReliablePacket)m_sentPackets.get( i );
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

    class ReliablePacket {

        long            m_timeSent;
        ByteArray       m_packet;

        ReliablePacket( ByteArray packet ){

            m_packet = packet;
            m_timeSent = System.currentTimeMillis();
        }

        long getTimeSent(){

            return m_timeSent;
        }

        ByteArray getPacket(){

            return m_packet;
        }

        void setTimeSent( long newTimeSent ){

            m_timeSent = newTimeSent;
        }
    }
}
