package twcore.core.net;

import java.net.DatagramPacket;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import twcore.core.util.ByteArray;
import twcore.core.util.Tools;

/**
 * Generates packets that correspond to the Subspace / Continuum protocol, and
 * requests to encrypt and send them out to the SS server.
 */
public class GamePacketGenerator {

    private Timer                  m_timer;          // Schedules clustered packets
    private TimerTask              m_timerTask;      // Clustered packet send task
    private DelayedPacketList<ByteArray> m_messageList;  // Packets waiting to be sent in clusters
    private SSEncryption           m_ssEncryption;   // Encryption class
    private Sender                 m_outboundQueue;  // Outgoing packet queue
    private int                    m_serverTimeDifference;   // Diff (*tinfo)
    private ReliablePacketHandler  m_reliablePacketHandler;  // Handles reliable sends
    private LinkedList<ByteArray>  m_lvzToggleCluster;       // LVZ obj toggle group
    private Map<Integer,LinkedList<ByteArray>> m_lvzPlayerToggleCluster;   // Toggle clusters for
                                                                           // individual playerIDs
    private long                   m_sendDelay = 75;         // Delay between packet sends
    private final static int       DEFAULT_PACKET_CAP = 45;  // Default # low-priority packets allowed
                                                             // per clustered send

    /**
     * Creates a new instance of GamePacketGenerator.
     * @param outboundQueue Packet sending queue
     * @param ssEncryption Encryption algorithm class
     * @param timer Bot's universal timer
     * @param isLocalConnection True if we are connecting locally to the server
     */
    public GamePacketGenerator( Sender outboundQueue, SSEncryption ssEncryption, Timer timer ){
        m_timer = timer;
        m_serverTimeDifference = 0;
        m_ssEncryption = ssEncryption;
        m_outboundQueue = outboundQueue;
        m_messageList = new DelayedPacketList<ByteArray>( DEFAULT_PACKET_CAP );
        m_lvzToggleCluster = new LinkedList<ByteArray>();
        m_lvzPlayerToggleCluster = Collections.synchronizedMap( new HashMap<Integer,LinkedList<ByteArray>>() );

        m_timerTask = new TimerTask(){
            int size;
            public void run(){

                synchronized (m_messageList) {
                    size = m_messageList.size();
                    if( size == 1 ) {
                        ByteArray packet = m_messageList.getNextPacket();
                        if( packet.size() == 6 && packet.readLittleEndianShort(0) == ((short)0x0400) ) {
                            // Do not send single ACKs reliably
                            composeImmediatePacket( packet, 6 );
                        } else {
                            sendReliableMessage( packet );
                        }
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
     * Sets the max number of lower-priority (aka chat) packets that can
     * be sent per clustered send.
     * @param cap Max # lower-priority packets allowed to be sent per clustered send
     */
    public void setPacketCap( int cap ){
        m_messageList.setPacketCap( cap );
    }

    /**
     * Adds a packet to the standard outgoing queue.
     * @param array Packet to add
     */
    public void composePacket( int[] array ){

        m_messageList.addNormalPacket( new ByteArray( array ) );
    }

    /**
     * Adds a packet to the standard outgoing queue.
     * @param array Packet to add
     */
    public void composePacket( byte[] array ){

        m_messageList.addNormalPacket( new ByteArray( array ) );
    }

    /**
     * Adds a packet to the standard outgoing queue.
     * @param bytearray Packet to add
     */
    public void composePacket( ByteArray bytearray ){

        m_messageList.addNormalPacket( bytearray );
    }

    /**
     * Adds a packet to the outgoing queue with low priority.  The number of
     * such packets allowed to be transmitted out per clustered send is capped,
     * and can be set with setPacketCap(int).
     * @param bytearray Packet to add
     * @see #setPacketCap(int)
     */
    public void composeLowPriorityPacket( ByteArray bytearray ){

        m_messageList.addCappedPacket( bytearray );
    }

    /**
     * Adds a packet to the outgoing queue with low priority.  The number of
     * such packets allowed to be transmitted out per clustered send is capped,
     * and can be set with setPacketCap(int).
     * @param array Packet to add
     * @see #setPacketCap(int)
     */
    public void composeLowPriorityPacket( int[] array ){

        m_messageList.addCappedPacket( new ByteArray( array ) );
    }

    /**
     * Adds a packet to the outgoing queue with low priority.  The number of
     * such packets allowed to be transmitted out per clustered send is capped,
     * and can be set with setPacketCap(int).
     * @param array Packet to add
     * @see #setPacketCap(int)
     */
    public void composeLowPriorityPacket( byte[] array ){

        m_messageList.addCappedPacket( new ByteArray( array ) );
    }

    /**
     * Adds a packet to the immediate outgoing queue, ignoring any other packets
     * in the standard queue it could be clustered with.  Fast but not as efficient.
     * Packets sent in this manner are not sent reliably.
     * @param array Packet to add
     */
    public void composeImmediatePacket( int[] array, int size ){

        m_outboundQueue.send( new DatagramPacket( ByteArray.toByteArray( array ), size ) );
    }

    /**
     * Adds a packet to the immediate outgoing queue, ignoring any other packets
     * in the standard queue it could be clustered with.  Fast but not as efficient.
     * Packets sent in this manner are not sent reliably.
     * @param array Packet to add
     */
    public void composeImmediatePacket( byte[] array, int size ){

        m_outboundQueue.send( new DatagramPacket( array, size ) );
    }

    /**
     * Adds a packet to the immediate outgoing queue, ignoring any other packets
     * in the standard queue it could be clustered with.  Fast but not as efficient.
     * Packets sent in this manner are not sent reliably.
     * @param bytearray Packet to add
     * @param size Size of packet
     */
    public void composeImmediatePacket( ByteArray bytearray, int size ){

        m_outboundQueue.send( new DatagramPacket( bytearray.getByteArray(), size ) );
    }

    /**
     * Adds a packet to the immediate high-priority outgoing queue, ignoring any
     * other packets in the standard queue it could be clustered with and placing
     * it at the head of the immediate queue.  Packets sent in this manner are not
     * sent reliably.
     * @param array Packet to add
     */
    public void composeHighPriorityPacket( int[] array, int size ){

        m_outboundQueue.highPrioritySend( new DatagramPacket( ByteArray.toByteArray( array ), size ) );
    }

    /**
     * Adds a packet to the immediate high-priority outgoing queue, ignoring any
     * other packets in the standard queue it could be clustered with and placing
     * it at the head of the immediate queue.  Packets sent in this manner are not
     * sent reliably.
     * @param array Packet to add
     */
    public void composeHighPriorityPacket( byte[] array, int size ){

        m_outboundQueue.highPrioritySend( new DatagramPacket( array, size ) );
    }

    /**
     * Adds a packet to the immediate high-priority outgoing queue, ignoring any
     * other packets in the standard queue it could be clustered with and placing
     * it at the head of the immediate queue.  Packets sent in this manner are not
     * sent reliably.
     * @param bytearray Packet to add
     * @param size Size of packet
     */
    public void composeHighPriorityPacket( ByteArray bytearray, int size ){

        m_outboundQueue.highPrioritySend( new DatagramPacket( bytearray.getByteArray(), size ) );
    }

    /**
     * Sends a reliable message that must receive an ACKnowledged packet in
     * response.  The ReliablePacketHandler will continue to resend the packet
     * until the ACK message is received.  Note that sending a message this
     * way will ensure it is not clustered with any other.  If at all possible,
     * composePacket methods should be used instead to minimize bandwidth use.
     * @param message
     */
    public void sendReliableMessage( ByteArray message ){

        m_reliablePacketHandler.sendReliableMessage( message );
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
     * Sends the client key / encryption request packet (bi-directional packet 0x01)
     * using the provided key.
     * @param clientKey Key to send
     */
    public void sendClientKey( int clientKey ){
        
        Tools.printConnectionLog("SEND BI : (0x01) Encryption Request");

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
        
        Tools.printConnectionLog("SEND BI : (0x05) Sync Request");

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
        
        Tools.printConnectionLog("SEND BI : (0x06) Sync Response");

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
     * @param newUser boolean that determines if user needs to be registered (true) or if it's an existing user (false)
     * @param name Bot's pre-existing login name
     * @param password Bot's account password
     */
    public void sendPasswordPacket( boolean newUser, String name, String password ){
        
        Tools.printConnectionLog("SEND    : (0x09) Password packet");

        ByteArray      bytearray = new ByteArray( 101 );

        bytearray.addByte( 0x09 ); // Type byte
        bytearray.addByte( newUser ? 1 : 0 ); // 0 for returning user (1 for new)
        bytearray.addPaddedString( name, 32 );       //Name
        bytearray.addPaddedString( password, 32 );   //Password

        bytearray.addByte( 0xd8 ); // Machine ID - 4 bytes
        bytearray.addByte( 0x67 );
        bytearray.addByte( 0xeb );
        bytearray.addByte( 0x64 );

        bytearray.addByte( 0x04 ); // Connection type (UnknownNotRAS)
        bytearray.addByte( 0xE0 ); // Timezone bias (224; 240=EST) - 2 bytes
        bytearray.addByte( 0x01 );
        bytearray.addByte( 0x57 ); // Unknown - 2bytes
        bytearray.addByte( 0xFC );

        bytearray.addLittleEndianShort( (short)134 ); // Client version (SS 1.34)
        bytearray.addLittleEndianInt( 444 );          // Memory checksum
        bytearray.addLittleEndianInt( 555 );          // Memory checksum
        bytearray.addLittleEndianInt( 0x92f88614 );   // Permission ID
        bytearray.repeatAdd( 0x0, 12 );               // Last / Unknown

        sendReliableMessage( bytearray );
    }

    /**
     * Sends the registration data if the server asks for it. This information is mostly build up
     * from the properties of setup.cfg ([registration] tag). The registry variables are replaced by "TWCore"
     * since we want to keep TWCore cross-platform.
     *
     * @param realname Real name
     * @param email E-mail address
     * @param state State
     * @param city City
     * @param age Age
     */
    public void sendRegistrationForm(String realname, String email, String state, String city, int age) {
        
        Tools.printConnectionLog("SEND    : (0x17) Registration Form Response");
        
    	/*
		Field	Length	Description
			0		1		Type byte
			1		32		Real name
			33		64		Email
			97		32		City
			129		24		State
			153		1		Sex('M'/'F')
			154		1		Age
		Connecting from...
			155		1		Home
			156		1		Work
			157		1		School
		System information
			158		4		Processor type (586)
			162		2		?
			164		2		?
		Windows registration information (SSC RegName ban)
			166		40		Real name
			206		40		Organization
		Windows NT-based OS's do not send any hardware information (DreamSpec HardwareID ban)
			246		40		System\CurrentControlSet\Services\Class\Display\0000
			286		40		System\CurrentControlSet\Services\Class\Monitor\0000
			326		40		System\CurrentControlSet\Services\Class\Modem\0000
			366		40		System\CurrentControlSet\Services\Class\Modem\0001
			406		40		System\CurrentControlSet\Services\Class\Mouse\0000
			446		40		System\CurrentControlSet\Services\Class\Net\0000
			486		40		System\CurrentControlSet\Services\Class\Net\0001
			526		40		System\CurrentControlSet\Services\Class\Printer\0000
			566		40		System\CurrentControlSet\Services\Class\MEDIA\0000
			606		40		System\CurrentControlSet\Services\Class\MEDIA\0001
			646		40		System\CurrentControlSet\Services\Class\MEDIA\0002
			686		40		System\CurrentControlSet\Services\Class\MEDIA\0003
			726		40		System\CurrentControlSet\Services\Class\MEDIA\0004
		*/

    	ByteArray bytearray = new ByteArray(766);

    	bytearray.addByte( 0x17 );						// Type
    	bytearray.addPaddedString(realname, 32); 		// Real name
    	bytearray.addPaddedString(email, 64);			// E-mail
    	bytearray.addPaddedString(city, 32);			// City
    	bytearray.addPaddedString(state, 24);			// State
    	bytearray.addByte(0);							// Sex/gender (Male)
    	bytearray.addByte(age);							// Age
    	bytearray.addByte(1);							// Connecting from Home
    	bytearray.addByte(1);							//                 Work
    	bytearray.addByte(1);							//                 School
    	bytearray.addLittleEndianInt(586);				// System information: Processor type (586)
    	bytearray.addLittleEndianShort((short)0xC000);	//                     ? Magic number 1
    	bytearray.addLittleEndianShort((short)2036);	//                     ? Magic number 2
    	bytearray.addPaddedString(realname, 40);		// Real name
    	bytearray.addPaddedString("TWCore", 40);		// Organization
    	bytearray.addPaddedString("TWCore", 40);		// Registry Sys. info.:	...\Display\0000
    	bytearray.addPaddedString("TWCore", 40);		//           			...\Monitor\0000
    	bytearray.addPaddedString("TWCore", 40);		//           			...\Modem\0000
    	bytearray.addPaddedString("TWCore", 40);		//        	   			...\Modem\0001
    	bytearray.addPaddedString("TWCore", 40);		//         			  	...\Mouse\0000
    	bytearray.addPaddedString("TWCore", 40);		//           			...\Net\0000
    	bytearray.addPaddedString("TWCore", 40);		//           			...\Net\0001
    	bytearray.addPaddedString("TWCore", 40);		//           			...\Printer\0000
    	bytearray.addPaddedString("TWCore", 40);		//           			...\MEDIA\0000
    	bytearray.addPaddedString("TWCore", 40);		//           			...\MEDIA\0001
    	bytearray.addPaddedString("TWCore", 40);		//           			...\MEDIA\0002
    	bytearray.addPaddedString("TWCore", 40);		//           			...\MEDIA\0003
    	bytearray.addPaddedString("TWCore", 40);		//           			...\MEDIA\0004

    	this.sendMassiveChunkPacket( bytearray );
    }

    /**
     * Sends an acknolwedgement packet in response to a reliable packet sent
     * by the server.
     * @param ackID ACK ID of the reliable packet sent that must be returned
     */
    public void sendAck( int ackID ){
        
        Tools.printConnectionLog("SEND BI : (0x04) Reliable ACK");
        
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
        
        Tools.printConnectionLog("SEND    : (0x01) Arena login");
        
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
        
        Tools.printConnectionLog("SEND    : (0x03) Position packet");

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
        
        Tools.printConnectionLog("SEND    : (0x06) Chat message");
        
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

        composeLowPriorityPacket( bytearray );
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
        
        Tools.printConnectionLog("SEND BI : (0x0E) Cluster");

        bytearray = new ByteArray( 500 );
        bytearray.addByte( 0x00 ); // Type byte (specifies bidirectional)
        bytearray.addByte( 0x0e ); // Bidirectional: Cluster packet
        sizeLeft = 498;

        synchronized (m_messageList) {
            // We might need to send more than one packet if the total of all messages
            // is longer than 500 bytes.
            m_messageList.resetCap();

            while( done == false ){

                // We now need to add as many as we can before we fill up the 500 bytes.
                if( m_messageList.cappedSize() > 0 ){
                    tempMessage = m_messageList.getNextPacket();
                    nextSize = tempMessage.size();
                } else {
                    done = true;
                    nextSize = 0;
                    tempMessage = null;
                }

                while( ((sizeLeft - (nextSize + 1)) > 0) && done == false ){
                    bytearray.addByte( nextSize );
                    bytearray.addByteArray( tempMessage );

                    if( m_messageList.cappedSize() > 0 ){
                        tempMessage = m_messageList.getNextPacket();
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
        
        Tools.printConnectionLog("SEND    : (0x02) Leave arena");
        
        ByteArray ba = new ByteArray( 1 );
        ba.addByte( 0x02 );  // Type byte (nothing else needed)

        sendReliableMessage( ba );
    }

    /**
     * Sends disconnect packet, informing the server that the client has
     * disconnected.
     */
    public void sendDisconnect(){
        
        Tools.printConnectionLog("SEND BI : (0x07) Disconnect");
        
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
        
        Tools.printConnectionLog("SEND    : (0x08) Spectate player");
        
        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x08 );  // Type byte
        if( playerID == -1 ) {
            bytearray.addByte( 0xFF );	// All high bits = stop spectating
            bytearray.addByte( 0xFF );
        } else
            bytearray.addLittleEndianShort( playerID );

        composePacket( bytearray );
    }

    /**
     * Sends instant spectate packet, for when fresh data on a specific player
     * is absolutely required.
     * @param playerID ID of player to immediately spectate
     */
    public void sendImmediateSpectatePacket( short playerID ) {
        
        Tools.printConnectionLog("SEND IMM: (0x08) Spectate player");
        
        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x08 );  // Type byte
        if( playerID == -1 ) {
            bytearray.addByte( 0xFF );  // All high bits = stop spectating
            bytearray.addByte( 0xFF );
        } else
            bytearray.addLittleEndianShort( playerID );

        m_outboundQueue.forceInstantSend( new DatagramPacket( bytearray.getByteArray(), 3 ) );
    }

    /**
     * Sends a request to pick up the flag of the specified ID.
     * @param flagID ID of flag to pick up
     */
    public void sendFlagRequestPacket( short flagID ) {
        
        Tools.printConnectionLog("SEND    : (0x13) Flag request");
        
        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x13 ); // Type byte
        bytearray.addLittleEndianShort( flagID );

        sendReliableMessage( bytearray );
    }

    /**
     * Sends the signal to drop all flags carried.
     */
    public void sendFlagDropPacket( ) {
        
        Tools.printConnectionLog("SEND    : (0x15) Drop Flags");
        
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
        
        Tools.printConnectionLog("SEND    : (0x19) Set personal banner");

        ByteArray bytearray = new ByteArray( 97 );
        bytearray.addByte( 0x19 );			// Type byte
        bytearray.addByteArray( banner );	// Banner without palette info

        sendReliableMessage( bytearray );
    }

/*
    public void sendBallPacket( short playerID, short ballId){
        
        ByteArray bytearray = new ByteArray( 3 );
        if( playerID == -1 )
        { 
            bytearray.addByte(0x2E);
        }
        else
            bytearray.addLittleEndianShort( ballId );
        
        sendReliableMessage( bytearray );
        m_outboundQueue.forceInstantSend( new DatagramPacket( bytearray.getByteArray(), 3 ) );
    }
  */
    /**
    * Sends a request to pick up a ball.
    * @param ballID ID of ball to pick up
    * @param timestamp Timestamp of last known ball position
    * @author Cheese
    */
    public void sendBallPickupPacket(byte ballID, int timestamp)
    {
        Tools.printConnectionLog("SEND BP: (0x20) Ball Pickup");
        
      
        ByteArray bytearray = new ByteArray(6);
        bytearray.addByte(0x20); //Type Byte
        bytearray.addByte(ballID);
/*
This is pointless, the server will just ignore the packet unless the Timestamp is the same as the last Ball update.
        bytearray.addLittleEndianInt((int)(System.currentTimeMillis()/10) + m_serverTimeDifference); 
 */
        bytearray.addLittleEndianInt(timestamp);
        
        sendReliableMessage(bytearray);
    }

    /**
    * Sends a ball fire packet.
    * @param ballID ID of ball to fire
    * @param xPosition Starting X position
    * @param yPosition Starting Y position
    * @param xVelocity Ball X velocity
    * @param yVelocity Ball Y velocity
    * @param playerID ID of player firing ball
    * @author Cheese
    */
    public void sendBallFirePacket(byte ballID, short xPosition, short yPosition, short xVelocity, short yVelocity, short playerID)
    {
        Tools.printConnectionLog("SEND BF: (0x1F) Ball Fire");
        
        ByteArray bytearray=new ByteArray(16);
        bytearray.addByte(0x1F); //Type Byte
        bytearray.addByte(ballID);
        bytearray.addLittleEndianShort(xPosition);
        bytearray.addLittleEndianShort(yPosition);
        bytearray.addLittleEndianShort(xVelocity);
        bytearray.addLittleEndianShort(yVelocity);
        bytearray.addLittleEndianShort(playerID);
        bytearray.addLittleEndianInt((int)(System.currentTimeMillis()/10) + m_serverTimeDifference);
        
        sendReliableMessage(bytearray);
    }
  
    /**
     * Sets the bot as a given ship number.  Unlike TWCore ship numbering
     * standards, the SS protocol uses ship 0 as warbird and 8 as spectator.
     * @param ship Number of ship to change to; 0 is WB, 8 is spec
     */
    public void sendShipChangePacket( short ship ) {
        
        Tools.printConnectionLog("SEND    : (0x18) Set ship type");
        
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
        
        Tools.printConnectionLog("SEND    : (0x0F) Frequency Change");
        
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
        
        Tools.printConnectionLog("SEND    : (0x10) Attach request");
        
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
        
        Tools.printConnectionLog("SEND BI : (0x0A) HUGE Chunk");
        
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
        
        Tools.printConnectionLog("SEND    : (0x16) File transfer");
        
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
       
        Tools.printConnectionLog("SEND    : (0x05) Death message");
        
        ByteArray      data;

        data = new ByteArray( 5 );
        data.addByte( 0x05 );  // Type byte
        data.addLittleEndianShort( (short)playerID );
        data.addLittleEndianShort( (short)bounty );

        composePacket( data );
    }

    /**
     * Allows bot to be able to drop bricks.
     * @param xLocation
     * @param yLocation
     */
    public void sendDropBrick( int xLocation, int yLocation ){
        
        Tools.printConnectionLog("SEND    : (0x1C) Drop Brick");
        
        ByteArray      data;

        data = new ByteArray( 5 );
        data.addByte( 0x1C );  // Type byte
        data.addLittleEndianShort( (short)xLocation );
        data.addLittleEndianShort( (short)yLocation );

        composePacket( data );
    }

    /**
     * Adds an LVZ object ID state (whether to turn it on or off) to the object queue.
     * Use {@link #sendLVZObjectCluster(int)} to send out a packet containing the
     * toggling instructions.
     * @param playerID ID of player to send to; use -1 for all players
     * @param objID Object ID to toggle
     * @param objVisible True if object is to be visible after being set; false if invisible
     * @see #sendLVZObjectCluster(int)
     */
    public void setupLVZObjectToggle( int playerID, int objID, boolean objVisible ) {
        ByteArray objData = new ByteArray( 2 );
        objData.addLittleEndianShort( (short) (objVisible ? (objID & 0x7FFF) : (objID | 0x8000)) );
        if( playerID == -1 )
            m_lvzToggleCluster.addLast( objData );
        else {
            LinkedList <ByteArray>playerCluster = m_lvzPlayerToggleCluster.remove(playerID);
            if( playerCluster == null )
                playerCluster = new LinkedList<ByteArray>();
            playerCluster.addLast( objData );
            m_lvzPlayerToggleCluster.put( playerID, playerCluster );
        }
    }

    /**
     * Adds one or more LVZ object ID states (whether to turn it on or off) to an object
     * toggle queue at once, using a HashMap with (Integer)ObjectID->(Boolean)Visible
     * mappings.  Use {@link #sendLVZObjectCluster(int)} to send out a packet containing the
     * toggling instructions.
     * @param playerID ID of player to send to; use -1 for all players
     * @param toggles Mapping of object ID to visibility state to set
     * @see #sendLVZObjectCluster(int)
     */
    public void setupMultipleLVZObjectToggles( int playerID, HashMap<Integer,Boolean> toggles ) {
        if( toggles == null )
            return;
        if( playerID == -1 ) {
            for( Integer objID : toggles.keySet() ) {
                ByteArray objData = new ByteArray( 2 );
                objData.addLittleEndianShort( (short) (toggles.get(objID) ? (objID & 0x7FFF) : (objID | 0x8000)) );
                m_lvzToggleCluster.addLast( objData );
            }
        } else {
            LinkedList <ByteArray>playerCluster = m_lvzPlayerToggleCluster.remove(playerID);
            if( playerCluster == null )
                playerCluster = new LinkedList<ByteArray>();
            for( Integer objID : toggles.keySet() ) {
                ByteArray objData = new ByteArray( 2 );
                objData.addLittleEndianShort( (short) (toggles.get(objID) ? (objID & 0x7FFF) : (objID | 0x8000)) );
                playerCluster.addLast( objData );
            }
            m_lvzPlayerToggleCluster.put( playerID, playerCluster );
        }
    }

    /**
     * Sends the current LVZ object toggle cluster to the player specified by the ID;
     * -1 for all players.  Use {@link #addLVZObjectToggle(int, int, boolean)} to add objects
     * before sending out the packet.
     * @param playerID ID of player to send to; use -1 for all players
     * @see #addLVZObjectToggle(int, int, boolean)
     */
    public void sendLVZObjectCluster( int playerID ) {
        
        Tools.printConnectionLog("SEND    : (0x0A) LVZ Object Cluster");
        
        if( playerID == -1 ) {
            if( m_lvzToggleCluster.size() > 0 ) {
                ByteArray objPacket = new ByteArray( m_lvzToggleCluster.size() * 2 + 4 );
                objPacket.addByte( 0x0A );  // Type byte
                objPacket.addLittleEndianShort( ((short) 0xFFFF ) );
                objPacket.addByte( 0x35 );  // LVZ packet type byte
                while( m_lvzToggleCluster.size() > 0 )
                    objPacket.addLittleEndianShort( m_lvzToggleCluster.removeFirst().readLittleEndianShort(0) );
                composeLowPriorityPacket( objPacket );
            }
        } else {
            LinkedList <ByteArray>playerCluster = m_lvzPlayerToggleCluster.remove(playerID);
            if( playerCluster != null && playerCluster.size() > 0 ) {
                ByteArray objPacket = new ByteArray( playerCluster.size() * 2 + 4 );
                objPacket.addByte( 0x0A );  // Type byte
                objPacket.addLittleEndianShort( ((short) (playerID >= 0 ? (playerID & 0xFFFF) : 0xFFFF) ) );
                objPacket.addByte( 0x35 );  // LVZ packet type byte
                while( playerCluster.size() > 0 )
                    objPacket.addLittleEndianShort( playerCluster.removeFirst().readLittleEndianShort(0) );
                composeLowPriorityPacket( objPacket );
            }
        }
    }

    /**
     * Sends a single LVZ object toggle to the player specified by the ID;
     * -1 for all players.  Using this method does not interfere with or check
     * against the object toggling queues in any way.
     * @param objID Object ID to toggle
     * @param playerID ID of player to send to; use -1 for all players
     * @param objVisible True if object is to be visible after being set; false if invisible
     */
    public void sendSingleLVZObjectToggle( int playerID, int objID, boolean objVisible ) {
        
        Tools.printConnectionLog("SEND    : (0x0A) LVZ Object Toggle");
        
        ByteArray objPacket = new ByteArray( 6 );
        objPacket.addByte( 0x0A );  // Type byte
        objPacket.addLittleEndianShort( ((short) (playerID >= 0 ? (playerID & 0xFFFF) : 0xFFFF) ) );
        objPacket.addByte( 0x35 );  // LVZ packet type byte
        objPacket.addLittleEndianShort( (short) (objVisible ? (objID & 0x7FFF) : (objID | 0x8000)) );
        composeLowPriorityPacket( objPacket );
    }

    /**
     * Sends a signal to the server that this client is no longer in the King of the Hill game.
     * (In Continuum this only happens when you die or the timer runs out.)
     */
    public void sendEndKoTH() {
        
        Tools.printConnectionLog("SEND    : (0x1E) End Personal KoTH Timer");
        
        ByteArray objPacket = new ByteArray( 1 );
        objPacket.addByte( 0x1E );
        composePacket(objPacket);
    }

    /**
     * Implementation to send out most packets as normal, while placing a cap
     * on the number of less-important packets (generally messages) that are
     * sent out per run of the cluster packet composition timer.
     * @author dugwyler
     * @param <E>
     */
    private class DelayedPacketList<E> {
        private List<E> m_normalPacketList;
        private List<E> m_cappedPacketList;
        private int m_packetCap;
        private int m_remainingCap;

        public DelayedPacketList( int packetCap ) {
            m_normalPacketList = Collections.synchronizedList(new LinkedList<E>());
            m_cappedPacketList = Collections.synchronizedList(new LinkedList<E>());
            m_packetCap = packetCap;
            m_remainingCap = packetCap;
        }

        public void setPacketCap( int packetCap ) {
            m_packetCap = packetCap;
        }

        public void resetCap() {
            m_remainingCap = m_packetCap;
        }

        public void addNormalPacket( E packet ) {
            m_normalPacketList.add( packet );
        }

        public void addCappedPacket( E packet ) {
            m_cappedPacketList.add( packet );
        }

        public E getNextPacket( ) {
            if( m_normalPacketList.isEmpty() == false )
                return m_normalPacketList.remove( 0 );
            else {
                m_remainingCap--;
                return m_cappedPacketList.remove( 0 );
            }
        }

        public int size() {
            return m_normalPacketList.size() + m_cappedPacketList.size();
        }

        public int cappedSize() {
            return m_normalPacketList.size() + Math.min( m_cappedPacketList.size(), m_remainingCap );
        }
    }
}