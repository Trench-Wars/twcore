package twcore.core.net;

import java.net.DatagramPacket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import twcore.core.util.ByteArray;

/**
 * Generates packets that correspond to the Subspace / Continuum protocol, and
 * requests to encrypt and send them out to the SS server.
 */
public class GamePacketGenerator {

    private Timer                 m_timer;          // Schedules clustered packets
    private TimerTask             m_timerTask;      // Clustered packet send task
    private List<ByteArray>       m_messageList;    // Msgs waiting for clustered send
    private SSEncryption          m_ssEncryption;   // Encryption class
    private Sender                m_outboundQueue;  // Outgoing packet queue
    private int                   m_serverTimeDifference;   // Diff (*tinfo)
    private ReliablePacketHandler m_reliablePacketHandler;  // Handles reliable sends

    private long                  m_sendDelay = 75; // Delay between cluster sends

    /**
     * Creates a new instance of GamePacketGenerator.
     * @param outboundQueue Packet sending queue
     * @param ssEncryption Encryption algorithm class
     * @param timer Bot's universal timer
     */
    public GamePacketGenerator( Sender outboundQueue, SSEncryption ssEncryption, Timer timer ){
        m_timer = timer;
        m_serverTimeDifference = 0;
        m_ssEncryption = ssEncryption;
        m_outboundQueue = outboundQueue;
        m_messageList = Collections.synchronizedList(new LinkedList<ByteArray>());

        m_timerTask = new TimerTask(){
            public void run(){
                int         size;

                synchronized (m_messageList) {
                    size = m_messageList.size();
                    if( size == 1 ){
                        sendReliableMessage( m_messageList.remove(0) );
                    } else if( size > 1 ){
                        sendClusteredPacket();
                    }
                }
            }
        };

        m_timer.scheduleAtFixedRate( m_timerTask, 0, m_sendDelay );
    }

    /**
     * Assigns the bot a reference to the reliable packet handler to use.
     * @param packetHandler Handler of sent and received reliable packets and ACKs
     */
    public void setReliablePacketHandler( ReliablePacketHandler packetHandler ){

        m_reliablePacketHandler = packetHandler;
    }

    /**
     * Sets the delay between attempted packet sends.
     * @param delay Delay, in milliseconds
     */
    public void setSendDelay( int delay ){
        m_sendDelay = (long)delay;
    }

    /**
     * Adds a packet to the standard outgoing queue.
     * @param array Packet to add
     */
    public void composePacket( int[] array ){

        m_messageList.add( new ByteArray( array ) );
    }

    /**
     * Adds a packet to the standard outgoing queue.
     * @param array Packet to add
     */
    public void composePacket( byte[] array ){

        m_messageList.add( new ByteArray( array ) );
    }

    /**
     * Adds a packet to the standard outgoing queue.
     * @param bytearray Packet to add
     */
    public void composePacket( ByteArray bytearray ){

        m_messageList.add( bytearray );
    }

    /**
     * Adds a packet to the immediate outgoing queue, ignoring any other packets
     * in the standard queue it could be clustered with.  Fast but not as efficient.
     * @param array Packet to add
     */
    public void composeImmediatePacket( int[] array, int size ){

        m_outboundQueue.send( new DatagramPacket( ByteArray.toByteArray( array ), size ) );
    }

    /**
     * Adds a packet to the immediate outgoing queue, ignoring any other packets
     * in the standard queue it could be clustered with.  Fast but not as efficient.
     * @param array Packet to add
     */
    public void composeImmediatePacket( byte[] array, int size ){

        m_outboundQueue.send( new DatagramPacket( array, size ) );
    }

    /**
     * Adds a packet to the immediate outgoing queue, ignoring any other packets
     * in the standard queue it could be clustered with.  Fast but not as efficient.
     * @param bytearray Packet to add
     * @param size Size of packet
     */
    public void composeImmediatePacket( ByteArray bytearray, int size ){

        m_outboundQueue.send( new DatagramPacket( bytearray.getByteArray(), size ) );
    }

    /**
     * Adds a packet to the immediate high-priority outgoing queue, ignoring any
     * other packets in the standard queue it could be clustered with and placing
     * it at the head of the immediate queue.
     * @param array Packet to add
     */
    public void composeHighPriorityPacket( int[] array, int size ){

        m_outboundQueue.highPrioritySend( new DatagramPacket( ByteArray.toByteArray( array ), size ) );
    }

    /**
     * Adds a packet to the immediate high-priority outgoing queue, ignoring any
     * other packets in the standard queue it could be clustered with and placing
     * it at the head of the immediate queue.
     * @param array Packet to add
     */
    public void composeHighPriorityPacket( byte[] array, int size ){

        m_outboundQueue.highPrioritySend( new DatagramPacket( array, size ) );
    }

    /**
     * Adds a packet to the immediate high-priority outgoing queue, ignoring any
     * other packets in the standard queue it could be clustered with and placing
     * it at the head of the immediate queue.
     * @param bytearray Packet to add
     * @param size Size of packet
     */
    public void composeHighPriorityPacket( ByteArray bytearray, int size ){

        m_outboundQueue.highPrioritySend( new DatagramPacket( bytearray.getByteArray(), size ) );
    }

    /**
     * Manually sets server time difference to a new value.
     * @param newTimeDifference Value to set to
     */
    public void setServerTimeDifference( int newTimeDifference ){

        m_serverTimeDifference = newTimeDifference;
    }

    /**
     * Get server time difference
     */
    public int getServerTimeDifference() {
    	return m_serverTimeDifference;
    }

    /**
     * Sends a reliable message that must receive an ACKnowledged packet in
     * response.  The ReliablePacketHandler will continue to resend the packet
     * until the ACK message is received.
     * @param message
     */
    public void sendReliableMessage( ByteArray message ){

        m_reliablePacketHandler.sendReliableMessage( message );
    }

    /**
     * Sends the client key / encryption request packet (bi-directional packet 0x01)
     * using the provided key.
     * @param clientKey Key to send
     */
    public void sendClientKey( int clientKey ){

        ByteArray      bytearray = new ByteArray( 8 );

        bytearray.addByte( 0x00 );  // Type byte (specifies bidirectional)
        bytearray.addByte( 0x01 );  // Bidirectional: Encryption request
        bytearray.addLittleEndianInt( clientKey );
        bytearray.addByte( 0x01 );  // Using SS protocol rather than Continuum
        bytearray.addByte( 0x00 );

        composeImmediatePacket( bytearray, 8 );
    }

    /**
     * Sends a synchronization request packet (bi-directional packet 0x05)
     * to the server to verify they aren't working on different time.
     * @param numPacketsSent Number of packets that have been sent so far
     * @param numPacketsReceived Number of packets that have been received so far
     */
    public void sendSyncPacket( int numPacketsSent, int numPacketsReceived ){

        ByteArray      bytearray = new ByteArray( 14 );

        bytearray.addByte( 0x00 );  // Type byte (specifies bidirectional)
        bytearray.addByte( 0x05 );  // Bidirectional: Synch request
        bytearray.addLittleEndianInt( (int)(System.currentTimeMillis() / 10) );
        bytearray.addLittleEndianInt( numPacketsSent );
        bytearray.addLittleEndianInt( numPacketsReceived );

        m_ssEncryption.encrypt( bytearray, 12, 2 );

        composeImmediatePacket( bytearray, 14 );
    }

    /**
     * Sends a synchrozination response packet (bi-directional packet 0x06) after
     * receiving a sync request packet.
     * @param syncTime Timestamp found in the sync request packet this packet is responding to
     */
    public void sendSyncResponse( int syncTime ){

        ByteArray      bytearray = new ByteArray( 10 );

        bytearray.addByte( 0x00 );  // Type byte (specifies bidirectional)
        bytearray.addByte( 0x06 );  // Bidirectional: Synch response
        bytearray.addLittleEndianInt( syncTime );
        bytearray.addLittleEndianInt( (int)(System.currentTimeMillis() / 10) );

        composeImmediatePacket( bytearray, 10 );
    }

    /**
     * Sends a password packet (C2S 0x09), including all necessary login data,
     * after receiving the server encryption key for this session.
     *
     * NOTE: User MUST exist before attempting to log in.  Core does not create
     * user accounts.
     * @param name Bot's pre-existing login name
     * @param password Bot's account password
     */
    public void sendPasswordPacket( String name, String password ){

        ByteArray      bytearray = new ByteArray( 101 );

        bytearray.addByte( 0x09 ); // Type byte
        bytearray.addByte( 0x00 ); // 0 for returning user (1 for new)
        bytearray.addPaddedString( name, 32 );       //Name
        bytearray.addPaddedString( password, 32 );   //Password

        bytearray.addByte( 0xd8 ); // Machine ID - 4 bytes
        bytearray.addByte( 0x67 );
        bytearray.addByte( 0xeb );
        bytearray.addByte( 0x64 );

        bytearray.addByte( 0x04 ); // Connection type (UnknownNotRAS)
        bytearray.addByte( 0xE0 ); // Timezone bias (224; 240=EST) - 2 bytes
        bytearray.addByte( 0x01 );
        bytearray.addByte( 0x57 ); // ? - 2bytes
        bytearray.addByte( 0xFC );

        bytearray.addLittleEndianShort( (short)134 ); // Client version (SS 1.34)
        bytearray.addLittleEndianInt( 444 );          // Memory checksum
        bytearray.addLittleEndianInt( 555 );          // Memory checksum
        bytearray.addLittleEndianInt( 0x92f88614 );   // Permission ID
        bytearray.repeatAdd( 0x0, 12 );               // Last

        sendReliableMessage( bytearray );
    }

    /**
     * Sends an acknolwedgement packet in response to a reliable packet sent
     * by the server.
     * @param ackID ACK ID of the reliable packet sent that must be returned
     */
    public void sendAck( int ackID ){
        ByteArray      bytearray = new ByteArray( 6 );

        bytearray.addByte( 0x00 );  // Type byte (specifies bidirectional)
        bytearray.addByte( 0x04 );  // Bidrectional: Reliable ACK
        bytearray.addLittleEndianInt( ackID );  // ID

        if( m_ssEncryption != null ){
            composePacket( bytearray );
        } else {
            composeImmediatePacket( bytearray, 6 );
        }
    }

    /**
     * Sends the arena login packet, changing the bot to the specified arena.
     * @param shipType 0-7 in-game ships, 8 spectator
     * @param xResolution X resolution to enter with
     * @param yResolution Y resolution to enter with
     * @param arenaType Pub # for specific pub; 0xFFFF random pub; 0xFFFD for priv arena
     * @param arenaName Arena name if arena type is 0xFFFD
     */
    public void sendArenaLoginPacket( byte shipType, short xResolution, short yResolution, short arenaType, String arenaName ){
        ByteArray      bytearray = new ByteArray( 26 );

        bytearray.addByte( 0x01 );  // Type byte
        bytearray.addByte( shipType );
        bytearray.addByte( 0x00 );  // Allow audio (2 bytes)
        bytearray.addByte( 0x00 );
        bytearray.addLittleEndianShort( xResolution );
        bytearray.addLittleEndianShort( yResolution );
        bytearray.addLittleEndianShort( arenaType );
        bytearray.addPaddedString( arenaName, 16 );

        sendReliableMessage( bytearray );
    }

    /**
     * Sends a position packet based on position information provided.
     * @param direction Value from 0 to 39 representing direciton ship is facing in SS degrees
     * @param xVelocity Velocity ship on the x axis
     * @param yPosition Current y position on the map (0-16384)
     * @param toggle Bitvector for cloak, stealth, UFO status
     * @param xPosition Current x position on the map (0-16384)
     * @param yVelocity Velocity of ship on the y axis
     * @param bounty Current bounty
     * @param energy Current ship energy
     * @param weapon Weapon number if firing a weapon at this position; 0 if none
     */
    public void sendPositionPacket( byte direction, short xVelocity, short yPosition, byte toggle,
    short xPosition, short yVelocity, short bounty, short energy, short weapon ){

        byte           checksum = 0;
        ByteArray      bytearray = new ByteArray( 22 );

        bytearray.addByte( 0x03 );      // Type byte
        bytearray.addByte( direction );
        bytearray.addLittleEndianInt( (int)(System.currentTimeMillis() / 10) + m_serverTimeDifference );
        bytearray.addLittleEndianShort( xVelocity );
        bytearray.addLittleEndianShort( yPosition );
        bytearray.addByte( 0 );         // Placeholder for the checksum
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

    /**
     * Sends a chat message.
     * @param messageType Type of message being sent -- public, error, etc. (0x00 to 0x09)
     * @param soundCode Number of the sound attached to this message, if any
     * @param userID For message types 0x04 and 0x05, player ID of target
     * @param message Text to send
     */
    public void sendChatPacket( byte messageType, byte soundCode, short userID, String message ){
    	if( message.length() > 243 )
    		message = message.substring(0, 242);		// (hack) Don't send more than SS can handle

        int            size = message.length() + 6;
        ByteArray      bytearray = new ByteArray( size );

        bytearray.addByte( 0x06 );          // Type byte
        bytearray.addByte( messageType );
        bytearray.addByte( soundCode );
        bytearray.addLittleEndianShort( userID );
        bytearray.addString( message );
        bytearray.addByte( 0x00 );          // Terminator

        composePacket( bytearray );
    }

    /**
     * Sends numerous packets in one cluster (all packets queued by
     * composePacket methods).
     */
    public void sendClusteredPacket(){
        int                 nextSize;
        int                 sizeLeft;
        ByteArray           bytearray;
        ByteArray           tempMessage;
        boolean             done = false;

        bytearray = new ByteArray( 500 );
        bytearray.addByte( 0x00 ); // Type byte (specifies bidirectional)
        bytearray.addByte( 0x0e ); // Bidirectional: Cluster packet
        sizeLeft = 498;

        synchronized (m_messageList) {
            // We might need to send more than one packet if the total of all messages
            // is longer than 500 bytes.
            while( done == false ){

                // We now need to add as many as we can before we fill up the 500 bytes.
                if( m_messageList.size() > 0 ){
                    tempMessage = m_messageList.remove(0);
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
                        tempMessage = m_messageList.remove(0);
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

    /**
     * Sends an Arena Left packet, notifying all that the bot has left the arena.
     */
    public void sendArenaLeft(){
        ByteArray ba = new ByteArray( 1 );
        ba.addByte( 0x02 );  // Type byte (nothing else needed)

        sendReliableMessage( ba );
    }

    /**
     * Sends disconnect packet, informing the server that the client has
     * disconnected.
     */
    public void sendDisconnect(){
        ByteArray      bytearray = new ByteArray( 2 );

        bytearray.addByte( 0x00 ); // Type byte (specifies bidirectional)
        bytearray.addByte( 0x07 ); // Bidirectional: Disconnect

        composeImmediatePacket( bytearray, 2 );
    }

    /**
     * Sends a request to spectate on a given player ID.
     * @param playerID ID of player to spectate
     */
    public void sendSpectatePacket( short playerID ) {
        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x08 );  // Type byte
        if( playerID == -1 ) {
            bytearray.addByte( 0xFF );	// All high bits = stop spectating
            bytearray.addByte( 0xFF );
        } else
            bytearray.addLittleEndianShort( playerID );

        sendReliableMessage( bytearray );
    }

    /**
     * Sends a request to pick up the flag of the specified ID.
     * @param flagID ID of flag to pick up
     */
    public void sendFlagRequestPacket( short flagID ) {
        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x13 ); // Type byte
        bytearray.addLittleEndianShort( flagID );

        sendReliableMessage( bytearray );
    }

    /**
     * Sends the signal to drop all flags carried.
     */
    public void sendFlagDropPacket( ) {
        ByteArray   bytearray = new ByteArray( 1 );
        bytearray.addByte( 0x15 );	// Type byte

        sendReliableMessage( bytearray );
    }

    /**
     * Sets a personal banner for the bot.  The banner is in BMP format
     * without any palette information, size 12x8, for a total of 96 bytes.
     * @param banner Banner data to set as bot's banner (96 bytes, 12x8 BMP, no palette)
     */
    public void sendBannerPacket( byte[] banner ) {

        ByteArray bytearray = new ByteArray( 97 );
        bytearray.addByte( 0x19 );			// Type byte
        bytearray.addByteArray( banner );	// Banner without palette info

        sendReliableMessage( bytearray );
    }

    /**
     * Sets the bot as a given ship number.  Unlike TWCore ship numbering
     * standards, the SS protocol uses ship 0 as warbird and 8 as spectator.
     * @param ship Number of ship to change to; 0 is WB, 8 is spec
     */
    public void sendShipChangePacket( short ship ) {
        ByteArray   bytearray = new ByteArray( 2 );
        bytearray.addByte( 0x18 );	// Type byte
        bytearray.addByte( ship );

        sendReliableMessage( bytearray );
    }

    /**
     * Sets the bot to a given frequency.
     * @param freq Frequency to set to
     */
    public void sendFreqChangePacket( short freq ) {
        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x0F ); // Type byte
        bytearray.addLittleEndianShort( freq );

        sendReliableMessage( bytearray );
    }

    /**
     * Requests an attachment to a specific player ID, or a detachment.
     * If ID is -1, the operation is a detach.
     * @param playerId PlayerID to attach to
     */
    public void sendAttachRequestPacket( short playerId ) {
        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x10 );  // Type byte
        if( playerId == -1 ) {
            bytearray.addByte( 0xFF );  // ID bytes set high for detach
            bytearray.addByte( 0xFF );
        } else
            bytearray.addLittleEndianShort( playerId );

        sendReliableMessage( bytearray );
    }

    /**
     * Sends a massive chunk packet to the server.  Used for file transfers,
     * mostly only in the case of when the bot executes a *putfile.  (Else
     * the server will probably reject it.)
     * @param data Data to send
     */
    public void sendMassiveChunkPacket( ByteArray data ){
        int            i, totalSize, size;
        ByteArray      bytearray;

        totalSize = data.size();
        i = totalSize;

        while( i > 0 ){
            size = 480;				// Sent in chunks of 480 bytes at max

            if( i < size ){
                size = i;
            }

            bytearray = new ByteArray( size + 6 );
            bytearray.addByte( 0x00 ); // Type byte (specifies bidirectional)
            bytearray.addByte( 0x0a ); // Bidrectional: huge chunk packet
            bytearray.addLittleEndianInt( totalSize );
            bytearray.addPartialByteArray( data, totalSize - i, size );
            sendReliableMessage( bytearray );
            i -= size;
        }
    }

    /**
     * Sends a file to the server.  Normally only used in conjunction with
     * the *putfile command and the 0x19 S2C packet (see GamePacketInterpreter).
     * @param fileName Name to associate with the file
     * @param fileData File data, compressed using ZLib compression
     */
    public void sendFile( String fileName, ByteArray fileData ){
        ByteArray      data;

        data = new ByteArray( fileData.size() + 17 );
        data.addByte( 0x16 );  // Type byte
        data.addPaddedString( fileName, 16 );
        data.addByteArray( fileData );
        sendMassiveChunkPacket( data );
    }

    /**
     * Send a death packet for the bot.  This is not done automatically
     * after receiving terminal damage, but is instead the option of the
     * bot controller.
     * @param playerID ID of the player who killed the bot
     * @param bounty Bot's bounty at the time of death
     */
    public void sendPlayerDeath( int playerID, int bounty ){
        ByteArray      data;

        data = new ByteArray( 5 );
        data.addByte( 0x05 );  // Type byte
        data.addLittleEndianShort( (short)playerID );
        data.addLittleEndianShort( (short)bounty );

        composePacket( data );
    }

}
