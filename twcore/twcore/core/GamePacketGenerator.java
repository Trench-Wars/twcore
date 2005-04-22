package twcore.core;

import java.net.*;
import java.util.*;

public class GamePacketGenerator {

    private Timer                 m_timer;
    private TimerTask             m_timerTask;
    private List                  m_messageList;
    private SSEncryption          m_ssEncryption;
    private Sender                m_outboundQueue;
    private int                   m_serverTimeDifference;
    private ReliablePacketHandler m_reliablePacketHandler;

    private long                  m_sendDelay = 75;

    public GamePacketGenerator( Sender outboundQueue, SSEncryption ssEncryption, Timer timer ){
        m_timer = timer;
        m_serverTimeDifference = 0;
        m_ssEncryption = ssEncryption;
        m_outboundQueue = outboundQueue;
        m_messageList = Collections.synchronizedList(new LinkedList());

        m_timerTask = new TimerTask(){
            public void run(){
                int         size;

                synchronized (m_messageList) {
                    size = m_messageList.size();
                    if( size == 1 ){
                        sendReliableMessage( (ByteArray)m_messageList.remove(0) );
                    } else if( size > 1 ){
                        sendClusteredPacket();
                    }
                }
            }
        };

        m_timer.scheduleAtFixedRate( m_timerTask, 0, m_sendDelay );
    }

    public void setReliablePacketHandler( ReliablePacketHandler packetHandler ){

        m_reliablePacketHandler = packetHandler;
    }

    public void setSendDelay( int delay ){
        m_sendDelay = (long)delay;
    }

    public void composePacket( int[] array ){

        m_messageList.add( new ByteArray( array ) );
    }

    public void composePacket( byte[] array ){

        m_messageList.add( new ByteArray( array ) );
    }

    public void composePacket( ByteArray bytearray ){

        m_messageList.add( bytearray );
    }

    public void composeImmediatePacket( int[] array, int size ){

        m_outboundQueue.send( new DatagramPacket( ByteArray.toByteArray( array ), size ) );
    }

    public void composeImmediatePacket( byte[] array, int size ){

        m_outboundQueue.send( new DatagramPacket( array, size ) );
    }

    public void composeImmediatePacket( ByteArray bytearray, int size ){

        m_outboundQueue.send( new DatagramPacket( bytearray.getByteArray(), size ) );
    }

    public void composeHighPriorityPacket( int[] array, int size ){

        m_outboundQueue.highPrioritySend( new DatagramPacket( ByteArray.toByteArray( array ), size ) );
    }

    public void composeHighPriorityPacket( byte[] array, int size ){

        m_outboundQueue.highPrioritySend( new DatagramPacket( array, size ) );
    }

    public void composeHighPriorityPacket( ByteArray bytearray, int size ){

        m_outboundQueue.highPrioritySend( new DatagramPacket( bytearray.getByteArray(), size ) );
    }

    public void setServerTimeDifference( int newTimeDifference ){

        m_serverTimeDifference = newTimeDifference;
    }

    public void sendReliableMessage( ByteArray message ){

        m_reliablePacketHandler.sendReliableMessage( message );
    }

    public void sendSyncRequest(){

        int[] intArray = { 0x00, 0x05, 0x6D, 0xE1,
        0x1E, 0x00, 0x06, 0x00,
        0x00, 0x00, 0x04, 0x00,
        0x00, 0x00 };

        composeImmediatePacket( intArray, intArray.length );
    }

    public void sendClientKey( int clientKey ){

        ByteArray      bytearray = new ByteArray( 8 );

        bytearray.addByte( 0x00 );
        bytearray.addByte( 0x01 );
        bytearray.addLittleEndianInt( clientKey );
        bytearray.addByte( 0x01 );
        bytearray.addByte( 0x00 );

        composeImmediatePacket( bytearray, 8 );
    }

    public void sendSyncPacket( int numPacketsSent, int numPacketsReceived ){

        ByteArray      bytearray = new ByteArray( 14 );

        bytearray.addByte( 0x00 );
        bytearray.addByte( 0x05 );
        bytearray.addLittleEndianInt( (int)((System.currentTimeMillis()/10) & 0xffffffff) );
        bytearray.addLittleEndianInt( numPacketsSent );
        bytearray.addLittleEndianInt( numPacketsReceived );

        m_ssEncryption.encrypt( bytearray, 12, 2 );

        composeImmediatePacket( bytearray, 14 );
    }

    public void sendSyncResponse( int syncTime ){

        ByteArray      bytearray = new ByteArray( 10 );

        bytearray.addByte( 0x00 );
        bytearray.addByte( 0x06 );
        bytearray.addLittleEndianInt( syncTime );
        bytearray.addLittleEndianInt( (int)((System.currentTimeMillis()/10) & 0xffffffff) );

        composeImmediatePacket( bytearray, 10 );
    }

    //Must NOT be a new user...
    public void sendPasswordPacket( String name, String password ){

        ByteArray      bytearray = new ByteArray( 101 );
        int[]          arr = { 0x86, 0x00, 0xBC, 0x01,
        0x00, 0x00, 0x2B, 0x02,
        0x00, 0x00 };

        bytearray.addByte( 0x09 );
        bytearray.addByte( 0x00 );
        bytearray.addPaddedString( name, 32 );       //Name
        bytearray.addPaddedString( password, 32 );   //Password

        bytearray.addByte( 0xd8 ); // Machine ID
        bytearray.addByte( 0x67 );
        bytearray.addByte( 0xeb );
        bytearray.addByte( 0x64 );

        bytearray.addByte( 0x04 ); // Machine ID
        bytearray.addByte( 0xE0 );
        bytearray.addByte( 0x01 );
        bytearray.addByte( 0x57 );
        bytearray.addByte( 0xFC );

        bytearray.addLittleEndianShort( (short)134 );
        bytearray.addLittleEndianInt( 444 );
        bytearray.addLittleEndianInt( 555 );
        bytearray.addLittleEndianInt( 0x92f88614 );
        bytearray.repeatAdd( 0x0, 12 );       //Last

        sendReliableMessage( bytearray );
    }

    //00,04,XX,XX,XX,XX
    public void sendAck( int ackID ){
        ByteArray      bytearray = new ByteArray( 6 );

        bytearray.addByte( 0x00 );
        bytearray.addByte( 0x04 );
        bytearray.addLittleEndianInt( ackID );

        if( m_ssEncryption != null ){
            composePacket( bytearray );
        } else {
            composeImmediatePacket( bytearray, 6 );
        }
    }

    public void sendArenaLoginPacket( byte shipType, short xResolution, short yResolution, short arenaType, String arenaName ){
        ByteArray      bytearray = new ByteArray( 26 );

        bytearray.addByte( 0x01 );
        bytearray.addByte( shipType );
        bytearray.addByte( 0x00 );
        bytearray.addByte( 0x00 );
        bytearray.addLittleEndianShort( xResolution );
        bytearray.addLittleEndianShort( yResolution );
        bytearray.addLittleEndianShort( arenaType );
        bytearray.addPaddedString( arenaName, 16 );

        sendReliableMessage( bytearray );
    }

    public void sendPositionPacket( byte direction, short xVelocity, short yPosition, byte toggle,
    short xPosition, short yVelocity, short bounty, short energy, short weapon ){

        byte           checksum = 0;
        ByteArray      bytearray = new ByteArray( 22 );

        bytearray.addByte( 0x03 );
        bytearray.addByte( direction );
        bytearray.addLittleEndianInt( (int)((System.currentTimeMillis()/10) & 0xffffffff) + m_serverTimeDifference );
        bytearray.addLittleEndianShort( xVelocity );
        bytearray.addLittleEndianShort( yPosition );
        bytearray.addByte( 0 );  // Placeholder for the checksum
        bytearray.addByte( toggle );
        bytearray.addLittleEndianShort( xPosition );
        bytearray.addLittleEndianShort( yVelocity );
        bytearray.addLittleEndianShort( bounty );
        bytearray.addLittleEndianShort( energy );
        bytearray.addLittleEndianShort( weapon );

        for( int i=0; i<22; i++ ){
            checksum ^= bytearray.readByte( i );
        }

        bytearray.addByte( checksum, 10 );

        m_ssEncryption.encrypt( bytearray, 21, 1 );

        composeImmediatePacket( bytearray, 22 );
    }

    public void sendChatPacket( byte messageType, byte soundCode, short userID, String message ){
        int            size = message.length() + 6;
        ByteArray      bytearray = new ByteArray( size );

        bytearray.addByte( 0x06 );
        bytearray.addByte( messageType );
        bytearray.addByte( soundCode );
        bytearray.addLittleEndianShort( userID );
        bytearray.addString( message );
        bytearray.addByte( 0x00 );

        composePacket( bytearray );
    }

    public void sendClusteredPacket(){
        int                 nextSize;
        int                 sizeLeft;
        ByteArray           bytearray;
        ByteArray           tempMessage;
        boolean             done = false;

        bytearray = new ByteArray( 500 );
        bytearray.addByte( 0x00 );
        bytearray.addByte( 0x0e );
        sizeLeft = 498;

        synchronized (m_messageList) {
            // We might need to send more than one packet if the total of all messages
            // is longer than 500 bytes.
            while( done == false ){

                // We now need to add as many as we can before we fill up the 500 bytes.
                if( m_messageList.size() > 0 ){
                    tempMessage = (ByteArray)m_messageList.remove(0);
                    nextSize = tempMessage.size();
                } else {
                    done = true;
                    nextSize = 0;
                    tempMessage = null;
                }

                while( ((sizeLeft - (nextSize + 1)) > 0) && done == false ){
                    bytearray.addByte( nextSize );
                    bytearray.addByteArray( tempMessage );

                    if( m_messageList.size() > 0 ){
                        tempMessage = (ByteArray)m_messageList.remove(0);
                        sizeLeft -= nextSize + 1;
                        nextSize = tempMessage.size();
                    } else {
                        done = true;
                    }
                }

                bytearray.shrinkArray( bytearray.getPointerIndex() );
                sendReliableMessage( bytearray );

                // Prepare next packet if there is more to send.
                if( !done ) {
                    bytearray = new ByteArray( 500 );
                    bytearray.addByte( 0x00 );
                    bytearray.addByte( 0x0e );
                    sizeLeft = 498 - (nextSize + 1);
                    bytearray.addByte( nextSize );
                    bytearray.addByteArray( tempMessage );
                }
            }
        }
    }

    public void sendArenaLeft(){
        ByteArray ba = new ByteArray( 1 );
        ba.addByte( 0x02 );

        sendReliableMessage( ba );
    }

    public void sendDisconnect(){
        ByteArray      bytearray = new ByteArray( 2 );

        bytearray.addByte( 0x00 );
        bytearray.addByte( 0x07 );

        composeImmediatePacket( bytearray, 2 );
    }

    public void sendSpecPacket( int playerID ){
        ByteArray      bytearray = new ByteArray( 2 );

        bytearray.addLittleEndianShort( (short)playerID );
        composeImmediatePacket( bytearray, 2 );
    }

    public void sendSpectatePacket( short playerID ) {
        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x08 );
        if( playerID == -1 ) {
            bytearray.addByte( 0xFF );
            bytearray.addByte( 0xFF );
        } else
            bytearray.addLittleEndianShort( playerID );

        sendReliableMessage( bytearray );
    }

    public void sendFlagRequestPacket( short flagID ) {
        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x13 );
        bytearray.addLittleEndianShort( flagID );

        sendReliableMessage( bytearray );
    }

    public void sendFlagDropPacket( ) {
        ByteArray   bytearray = new ByteArray( 1 );
        bytearray.addByte( 0x15 );

        sendReliableMessage( bytearray );
    }

    public void sendBannerPacket( byte[] banner ) {

        ByteArray bytearray = new ByteArray( 97 );
        bytearray.addByte( 0x19 );
        bytearray.addByteArray( banner );

        sendReliableMessage( bytearray );
    }

    public void sendShipChangePacket( short ship ) {
        ByteArray   bytearray = new ByteArray( 2 );
        bytearray.addByte( 0x18 );
        bytearray.addByte( ship );

        sendReliableMessage( bytearray );
    }

    public void sendFreqChangePacket( short freq ) {
        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x0F );
        bytearray.addLittleEndianShort( freq );

        sendReliableMessage( bytearray );
    }

    public void sendAttachRequestPacket( short playerId ) {
        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x10 );
        if( playerId == -1 ) {
            bytearray.addByte( 0xFF );
            bytearray.addByte( 0xFF );
        } else
            bytearray.addLittleEndianShort( playerId );

        sendReliableMessage( bytearray );
    }

    public void sendMassiveChunkPacket( ByteArray data ){
        int            i, totalSize, size;
        ByteArray      bytearray;

        totalSize = data.size();
        i = totalSize;

        while( i > 0 ){
            size = 480;

            if( i < size ){
                size = i;
            }

            bytearray = new ByteArray( size + 6 );
            bytearray.addByte( 0x00 );
            bytearray.addByte( 0x0a );
            bytearray.addLittleEndianInt( totalSize );
            bytearray.addPartialByteArray( data, totalSize - i, size );
            sendReliableMessage( bytearray );
            i -= size;
        }
    }

    public void sendFile( String fileName, ByteArray fileData ){
        ByteArray      data;

        data = new ByteArray( fileData.size() + 17 );
        data.addByte( 0x16 );
        data.addPaddedString( fileName, 16 );
        data.addByteArray( fileData );
        sendMassiveChunkPacket( data );
    }

    public void sendPlayerDeath( int playerID, int bounty ){
        ByteArray      data;

        data = new ByteArray( 5 );
        data.addByte( 0x06 );
        data.addLittleEndianShort( (short)playerID );
        data.addLittleEndianShort( (short)bounty );

        composePacket( data );
    }
}
