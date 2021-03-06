package twcore.core.net;

import java.net.DatagramPacket;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.lvz.LvzObject;
import twcore.core.util.ByteArray;
import twcore.core.util.Tools;

/**
    Generates packets that correspond to the Subspace / Continuum protocol, and
    requests to encrypt and send them out to the SS server.


    XXX: Connection logging of frequently-sent packets commented out rather than
    disabled in Tools, because each packet created requires a new String object
    to be constructed, and a method called. Normally not a big issue, this
    becomes costly when done often, which is the case.

    Uncomment to use the connection logging feature. If you're editing GPG you
    shouldn't need to be told this anyhow. :)
*/
public class GamePacketGenerator {

    private Timer                  m_timer;                     // Schedules clustered packets
    private TimerTask              m_timerTask;                 // Clustered packet send task
    private DelayedPacketList<ByteArray> m_messageList;         // Packets waiting to be sent in clusters
    private SSEncryption           m_ssEncryption;              // Encryption class
    private Sender                 m_outboundQueue;             // Outgoing packet queue
    private int                    m_serverTimeDifference;      // Diff (*tinfo)
    private ReliablePacketHandler  m_reliablePacketHandler;     // Handles reliable sends
    private LinkedList<ByteArray>  m_lvzToggleCluster;          // LVZ obj toggle group
    private Map<Integer, LinkedList<ByteArray>> m_lvzPlayerToggleCluster;  // Toggle clusters for
    // individual playerIDs
    private LinkedList<LvzObject>  m_lvzModCluster;             // LVZ obj modification group
    private Map<Integer, LinkedList<LvzObject>> m_lvzPlayerModCluster;     // LVZ modification cluster
    // for individual playerIDs
    private long                   m_sendDelay = 75;            // Delay between packet sends
    private final static int       DEFAULT_PACKET_CAP = 45;     // Default # low-priority packets allowed
    // per clustered send

    private long m_lastSyncRecv;                   // Used by subspace to invalidate slow or fast time syncs
    private long m_timeDiff;                       // Delta T between server and client - changes over time
    private int m_syncPing;                        // Average host response time to sync requests
    private int m_accuPing;                        // Ping time accumulator for average ping time
    private int m_countPing;                       // Ping count accumulator for average ping time
    private int m_avgPing;                         // Average ping time
    private int m_highPing;                        // Highest ping outlier
    private int m_lowPing;                         // Lowest ping outlier

    /**
        Creates a new instance of GamePacketGenerator.
        @param outboundQueue Packet sending queue
        @param ssEncryption Encryption algorithm class
        @param timer Bot's universal timer
    */
    public GamePacketGenerator( Sender outboundQueue, SSEncryption ssEncryption, Timer timer ) {
        m_timer = timer;
        m_serverTimeDifference = 0;
        m_ssEncryption = ssEncryption;
        m_outboundQueue = outboundQueue;
        m_messageList = new DelayedPacketList<ByteArray>( DEFAULT_PACKET_CAP );

        m_lvzToggleCluster = new LinkedList<ByteArray>();
        m_lvzPlayerToggleCluster = Collections.synchronizedMap( new HashMap<Integer, LinkedList<ByteArray>>() );

        m_lvzModCluster = new LinkedList<LvzObject>();
        m_lvzPlayerModCluster = Collections.synchronizedMap( new HashMap<Integer, LinkedList<LvzObject>>() );

        m_timerTask = new TimerTask() {
            int size;
            public void run() {

                synchronized (m_messageList) {
                    size = m_messageList.size();

                    if( size == 1 ) {
                        ByteArray packet = m_messageList.getNextPacket();
                        sendReliableMessage( packet );
                    } else if( size > 1 ) {
                        sendClusteredPacket();
                    }
                }
            }
        };

        m_timer.scheduleAtFixedRate( m_timerTask, 0, m_sendDelay );
    }

    /**
        Assigns the bot a reference to the reliable packet handler to use.
        @param packetHandler Handler of sent and received reliable packets and ACKs
    */
    public void setReliablePacketHandler( ReliablePacketHandler packetHandler ) {

        m_reliablePacketHandler = packetHandler;
    }

    /**
        Sets the delay between attempted packet sends.
        @param delay Delay, in milliseconds
    */
    public void setSendDelay( int delay ) {
        m_sendDelay = (long)delay;
    }

    /**
        Sets the max number of lower-priority (aka chat) packets that can
        be sent per clustered send.
        @param cap Max # lower-priority packets allowed to be sent per clustered send
    */
    public void setPacketCap( int cap ) {
        m_messageList.setPacketCap( cap );
    }

    /**
        Adds a packet to the standard outgoing queue.
        @param array Packet to add
    */
    public void composePacket( int[] array ) {

        m_messageList.addNormalPacket( new ByteArray( array ) );
    }

    /**
        Adds a packet to the standard outgoing queue.
        @param array Packet to add
    */
    public void composePacket( byte[] array ) {

        m_messageList.addNormalPacket( new ByteArray( array ) );
    }

    /**
        Adds a packet to the standard outgoing queue.
        @param bytearray Packet to add
    */
    public void composePacket( ByteArray bytearray ) {

        m_messageList.addNormalPacket( bytearray );
    }

    /**
        Adds a packet to the outgoing queue with low priority.  The number of
        such packets allowed to be transmitted out per clustered send is capped,
        and can be set with setPacketCap(int).
        @param bytearray Packet to add
        @see #setPacketCap(int)
    */
    public void composeLowPriorityPacket( ByteArray bytearray ) {

        m_messageList.addCappedPacket( bytearray );
    }

    /**
        Adds a packet to the outgoing queue with low priority.  The number of
        such packets allowed to be transmitted out per clustered send is capped,
        and can be set with setPacketCap(int).
        @param array Packet to add
        @see #setPacketCap(int)
    */
    public void composeLowPriorityPacket( int[] array ) {

        m_messageList.addCappedPacket( new ByteArray( array ) );
    }

    /**
        Adds a packet to the outgoing queue with low priority.  The number of
        such packets allowed to be transmitted out per clustered send is capped,
        and can be set with setPacketCap(int).
        @param array Packet to add
        @see #setPacketCap(int)
    */
    public void composeLowPriorityPacket( byte[] array ) {

        m_messageList.addCappedPacket( new ByteArray( array ) );
    }

    /**
        Adds a packet to the immediate outgoing queue, ignoring any other packets
        in the standard queue it could be clustered with.  Fast but not as efficient.
        Packets sent in this manner are not sent reliably.
        @param array Packet to add
    */
    public void composeImmediatePacket( int[] array, int size ) {

        m_outboundQueue.send( new DatagramPacket( ByteArray.toByteArray( array ), size ) );
    }

    /**
        Adds a packet to the immediate outgoing queue, ignoring any other packets
        in the standard queue it could be clustered with.  Fast but not as efficient.
        Packets sent in this manner are not sent reliably.
        @param array Packet to add
    */
    public void composeImmediatePacket( byte[] array, int size ) {

        m_outboundQueue.send( new DatagramPacket( array, size ) );
    }

    /**
        Adds a packet to the immediate outgoing queue, ignoring any other packets
        in the standard queue it could be clustered with.  Fast but not as efficient.
        Packets sent in this manner are not sent reliably.
        @param bytearray Packet to add
        @param size Size of packet
    */
    public void composeImmediatePacket( ByteArray bytearray, int size ) {

        m_outboundQueue.send( new DatagramPacket( bytearray.getByteArray(), size ) );
    }

    /**
        Adds a packet to the immediate high-priority outgoing queue, ignoring any
        other packets in the standard queue it could be clustered with and placing
        it at the head of the immediate queue.  Packets sent in this manner are not
        sent reliably.
        @param array Packet to add
    */
    public void composeHighPriorityPacket( int[] array, int size ) {

        m_outboundQueue.highPrioritySend( new DatagramPacket( ByteArray.toByteArray( array ), size ) );
    }

    /**
        Adds a packet to the immediate high-priority outgoing queue, ignoring any
        other packets in the standard queue it could be clustered with and placing
        it at the head of the immediate queue.  Packets sent in this manner are not
        sent reliably.
        @param array Packet to add
    */
    public void composeHighPriorityPacket( byte[] array, int size ) {

        m_outboundQueue.highPrioritySend( new DatagramPacket( array, size ) );
    }

    /**
        Adds a packet to the immediate high-priority outgoing queue, ignoring any
        other packets in the standard queue it could be clustered with and placing
        it at the head of the immediate queue.  Packets sent in this manner are not
        sent reliably.
        @param bytearray Packet to add
        @param size Size of packet
    */
    public void composeHighPriorityPacket( ByteArray bytearray, int size ) {

        m_outboundQueue.highPrioritySend( new DatagramPacket( bytearray.getByteArray(), size ) );
    }

    /**
        Sends a reliable message that must receive an ACKnowledged packet in
        response.  The ReliablePacketHandler will continue to resend the packet
        until the ACK message is received.  Note that sending a message this
        way will ensure it is not clustered with any other.  If at all possible,
        composePacket methods should be used instead to minimize bandwidth use.
        @param message
    */
    public void sendReliableMessage( ByteArray message ) {

        m_reliablePacketHandler.sendReliableMessage( message );
    }

    /**
        Manually sets server time difference to a new value.
        @param newTimeDifference Value to set to
    */
    public void setServerTimeDifference( int newTimeDifference ) {

        m_serverTimeDifference = newTimeDifference;
    }

    /**
        Get server time difference
    */
    public int getServerTimeDifference() {
        return m_serverTimeDifference;
    }

    /**
        Updates the last time a normal synchronization request was sent to the client.
        @param syncTime The new synchronization time.
    */
    public void updateSyncTime(int syncTime) {
        m_lastSyncRecv = syncTime;
    }

    /**
        Updates the various ping times based on the synchronization response packet.
        Used to accurately display the statistics for ?lag and *lag.
        @param pingTime
        @param pongTime
    */
    public void updatePingTimes(int pingTime, int pongTime) {
        long ticks = System.currentTimeMillis() / 10;   // One tick is 10 ms.
        int roundTrip = (int) (ticks - pingTime);

        // Update the running average.
        m_accuPing += roundTrip;
        ++m_countPing;
        m_avgPing = m_accuPing / m_countPing;

        // High ping
        if (roundTrip > m_highPing) {
            m_highPing = roundTrip;
        }

        // Low ping
        if ((m_lowPing == 0) || (roundTrip < m_lowPing)) {
            m_lowPing = roundTrip;
        }

        // Slow pings get ignored until the next security check.
        if (roundTrip > m_syncPing + 1) {
            if (ticks - m_lastSyncRecv <= 12000)
                return;
        }

        // Ping spikes get ignored
        if (roundTrip >= m_syncPing * 2) {
            if (ticks - m_lastSyncRecv <= 60000)
                return;
        }

        // Calculate the relative time difference.
        m_timeDiff = ((roundTrip * 3) / 5) + pongTime - ticks;

        if (m_timeDiff >= -10 && m_timeDiff <= 10) m_timeDiff = 0;

        m_lastSyncRecv = ticks;
        m_syncPing = roundTrip;

    }

    /**
        Sends the client key / encryption request packet (bi-directional packet 0x01)
        using the provided key.
        @param clientKey Key to send
    */
    public void sendClientKey( int clientKey ) {

        //Tools.printConnectionLog("SEND BI : (0x01) Encryption Request");

        ByteArray      bytearray = new ByteArray( 8 );

        bytearray.addByte( 0x00 );  // Type byte (specifies bidirectional)
        bytearray.addByte( 0x01 );  // Bidirectional: Encryption request
        bytearray.addLittleEndianInt( clientKey );
        bytearray.addByte( 0x01 );  // Using SS protocol rather than Continuum
        bytearray.addByte( 0x00 );

        composeImmediatePacket( bytearray, 8 );
    }

    /**
        Sends a synchronization request packet (bi-directional packet 0x05)
        to the server to verify they aren't working on different time.
        @param numPacketsSent Number of packets that have been sent so far
        @param numPacketsReceived Number of packets that have been received so far
    */
    public void sendSyncPacket( int numPacketsSent, int numPacketsReceived ) {

        //Tools.printConnectionLog("SEND BI : (0x05) Sync Request");

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
        Sends a synchrozination response packet (bi-directional packet 0x06) after
        receiving a sync request packet.
        @param syncTime Timestamp found in the sync request packet this packet is responding to
    */
    public void sendSyncResponse( int syncTime ) {

        //Tools.printConnectionLog("SEND BI : (0x06) Sync Response");

        ByteArray      bytearray = new ByteArray( 10 );

        bytearray.addByte( 0x00 );  // Type byte (specifies bidirectional)
        bytearray.addByte( 0x06 );  // Bidirectional: Synch response
        bytearray.addLittleEndianInt( syncTime );
        bytearray.addLittleEndianInt( (int)(System.currentTimeMillis() / 10) );

        composeImmediatePacket( bytearray, 10 );
    }

    /**
        Sends a password packet (C2S 0x09), including all necessary login data,
        after receiving the server encryption key for this session.

        @param newUser boolean that determines if user needs to be registered (true) or if it's an existing user (false)
        @param name Bot's pre-existing login name
        @param password Bot's account password
    */
    public void sendPasswordPacket( boolean newUser, String name, String password ) {

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
        bytearray.addLittleEndianShort((short) (Calendar.getInstance().getTimeZone().getOffset(System.currentTimeMillis()) / 60000));
        //bytearray.addByte( 0xE0 ); // Timezone bias (224; 240=EST) - 2 bytes
        //bytearray.addByte( 0x01 );
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
        Sends the registration data if the server asks for it. This information is mostly build up
        from the properties of setup.cfg ([registration] tag). The registry variables are replaced by "TWCore"
        since we want to keep TWCore cross-platform.

        @param realname Real name
        @param email E-mail address
        @param state State
        @param city City
        @param age Age
    */
    public void sendRegistrationForm(String realname, String email, String state, String city, int age) {

        Tools.printConnectionLog("SEND    : (0x17) Registration Form Response");

        /*
            Field   Length  Description
            0       1       Type byte
            1       32      Real name
            33      64      Email
            97      32      City
            129     24      State
            153     1       Sex('M'/'F')
            154     1       Age
            Connecting from...
            155     1       Home
            156     1       Work
            157     1       School
            System information
            158     4       Processor type (586)
            162     2       ?
            164     2       ?
            Windows registration information (SSC RegName ban)
            166     40      Real name
            206     40      Organization
            Windows NT-based OS's do not send any hardware information (DreamSpec HardwareID ban)
            246     40      System\CurrentControlSet\Services\Class\Display\0000
            286     40      System\CurrentControlSet\Services\Class\Monitor\0000
            326     40      System\CurrentControlSet\Services\Class\Modem\0000
            366     40      System\CurrentControlSet\Services\Class\Modem\0001
            406     40      System\CurrentControlSet\Services\Class\Mouse\0000
            446     40      System\CurrentControlSet\Services\Class\Net\0000
            486     40      System\CurrentControlSet\Services\Class\Net\0001
            526     40      System\CurrentControlSet\Services\Class\Printer\0000
            566     40      System\CurrentControlSet\Services\Class\MEDIA\0000
            606     40      System\CurrentControlSet\Services\Class\MEDIA\0001
            646     40      System\CurrentControlSet\Services\Class\MEDIA\0002
            686     40      System\CurrentControlSet\Services\Class\MEDIA\0003
            726     40      System\CurrentControlSet\Services\Class\MEDIA\0004
        */

        ByteArray bytearray = new ByteArray(766);

        bytearray.addByte( 0x17 );                      // Type
        bytearray.addPaddedString(realname, 32);        // Real name
        bytearray.addPaddedString(email, 64);           // E-mail
        bytearray.addPaddedString(city, 32);            // City
        bytearray.addPaddedString(state, 24);           // State
        bytearray.addByte(0);                           // Sex/gender (Male)
        bytearray.addByte(age);                         // Age
        bytearray.addByte(1);                           // Connecting from Home
        bytearray.addByte(1);                           //                 Work
        bytearray.addByte(1);                           //                 School
        bytearray.addLittleEndianInt(586);              // System information: Processor type (586)
        bytearray.addLittleEndianShort((short)0xC000);  //                     ? Magic number 1
        bytearray.addLittleEndianShort((short)2036);    //                     ? Magic number 2
        bytearray.addPaddedString(realname, 40);        // Real name
        bytearray.addPaddedString("TWCore", 40);        // Organization
        bytearray.addPaddedString("TWCore", 40);        // Registry Sys. info.: ...\Display\0000
        bytearray.addPaddedString("TWCore", 40);        //                      ...\Monitor\0000
        bytearray.addPaddedString("TWCore", 40);        //                      ...\Modem\0000
        bytearray.addPaddedString("TWCore", 40);        //                      ...\Modem\0001
        bytearray.addPaddedString("TWCore", 40);        //                      ...\Mouse\0000
        bytearray.addPaddedString("TWCore", 40);        //                      ...\Net\0000
        bytearray.addPaddedString("TWCore", 40);        //                      ...\Net\0001
        bytearray.addPaddedString("TWCore", 40);        //                      ...\Printer\0000
        bytearray.addPaddedString("TWCore", 40);        //                      ...\MEDIA\0000
        bytearray.addPaddedString("TWCore", 40);        //                      ...\MEDIA\0001
        bytearray.addPaddedString("TWCore", 40);        //                      ...\MEDIA\0002
        bytearray.addPaddedString("TWCore", 40);        //                      ...\MEDIA\0003
        bytearray.addPaddedString("TWCore", 40);        //                      ...\MEDIA\0004

        this.sendMassiveChunkPacket( bytearray );
    }

    /**
        Sends an acknolwedgement packet in response to a reliable packet sent
        by the server.
        @param ackID ACK ID of the reliable packet sent that must be returned
    */
    public void sendAck( int ackID ) {

        //Tools.printConnectionLog("SEND BI : (0x04) Reliable ACK");

        ByteArray      bytearray = new ByteArray( 6 );

        bytearray.addByte( 0x00 );  // Type byte (specifies bidirectional)
        bytearray.addByte( 0x04 );  // Bidrectional: Reliable ACK
        bytearray.addLittleEndianInt( ackID );  // ID

        if( m_ssEncryption != null ) {
            m_ssEncryption.encrypt( bytearray, 4, 2 );
            composeImmediatePacket( bytearray, 6 );
        } else {
            composeImmediatePacket( bytearray, 6 );
        }
    }

    /**
        Sends the arena login packet, changing the bot to the specified arena.
        @param shipType 0-7 in-game ships, 8 spectator
        @param xResolution X resolution to enter with
        @param yResolution Y resolution to enter with
        @param arenaType Pub # for specific pub; 0xFFFF random pub; 0xFFFD for priv arena
        @param arenaName Arena name if arena type is 0xFFFD
    */
    public void sendArenaLoginPacket( byte shipType, short xResolution, short yResolution, short arenaType, String arenaName ) {

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
        Sends a position packet based on position information provided.
        @param direction Value from 0 to 39 representing direciton ship is facing in SS degrees
        @param xVelocity Velocity ship on the x axis
        @param yPosition Current y position on the map (0-16384)
        @param toggle Bitvector for cloak, stealth, UFO status
        @param xPosition Current x position on the map (0-16384)
        @param yVelocity Velocity of ship on the y axis
        @param bounty Current bounty
        @param energy Current ship energy
        @param weapon Weapon number if firing a weapon at this position; 0 if none
    */
    public void sendPositionPacket( byte direction, short xVelocity, short yPosition, byte toggle,
                                    short xPosition, short yVelocity, short bounty, short energy, short weapon ) {

        //Tools.printConnectionLog("SEND    : (0x03) Position packet");

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

        // Add the checksum
        bytearray.addByte( ChecksumGenerator.simpleChecksum(bytearray, bytearray.size()) , 10 );

        m_ssEncryption.encrypt( bytearray, 21, 1 );

        composeImmediatePacket( bytearray, 22 );
    }

    /**
        Sends a chat message.
        @param messageType Type of message being sent -- public, error, etc. (0x00 to 0x09)
        @param soundCode Number of the sound attached to this message, if any
        @param userID For message types 0x04 and 0x05, player ID of target
        @param message Text to send
    */
    public void sendChatPacket( byte messageType, byte soundCode, short userID, String message ) {
        sendChatPacket( messageType, soundCode, userID, "", message );
    }

    /**
        Sends a chat message.
        This function may look overly complicated and slow, but as long as the chat message isn't
        exceeding the maximum length, almost all of the code is skipped. This scenario will happen at least 95~99%
        of the time, so sending chat messages should still remain fast.
        @param messageType Type of message being sent -- public, error, etc. (0x00 to 0x09)
        @param soundCode Number of the sound attached to this message, if any
        @param userID For message types 0x04 and 0x05, player ID of target
        @param prefix Any prefix that needs to be repeated for chopped up messages. (This comes down to ";#;", ":[name]:" and ":#[squadname]:")
        @param message Text to send
    */
    public void sendChatPacket( byte messageType, byte soundCode, short userID, String prefix, String message ) {

        //Tools.printConnectionLog("SEND    : (0x06) Chat message");

        Charset targetSet;
        String fullMessage = prefix + message;

        try {
            targetSet = Charset.forName("ISO-8859-1");
        } catch (UnsupportedCharsetException uce) {
            targetSet = Charset.defaultCharset();
            Tools.printLog("Unsupported charset used for encoding string: " + uce.getMessage());
        }

        // Due to character encoding methods, there is a chance that strings can expand in size.
        // The following lines are an attempt to prevent this, by using the converted length as parameter
        // and chopping up the message when it's too long. (This last part may have unwanted side-effects.)
        byte[] fullMsg = targetSet.encode(fullMessage).array();

        // If the length doesn't exceed the maximum length, send the packet immediately.
        if(fullMsg.length <= 243) {
            sendChatPacketCore( messageType, soundCode, userID, fullMsg);
            return;
        }

        // Otherwise, do some hacking to make it all work.
        byte[] msg;                 // Original message
        byte[] pref = null;         // Original prefix, if any
        byte[] splitMsg;            // Split up message part.

        int sectionLength = 243;    // Maximum length of a section
        int currOffset = 0;         // Current offset within the message.
        int len;                    // Current length of a chopped up section.

        int counter = 5;            // Safety counter to prevent server recycling due to flooding if stuff goes haywire.

        msg = targetSet.encode( message ).array();

        if( !prefix.isEmpty() ) {
            pref = targetSet.encode( prefix ).array();
            sectionLength -= pref.length;
        }

        while( currOffset + sectionLength < msg.length ) {
            int i = currOffset + sectionLength;

            if(--counter <= 0) {
                // Anti recycle protection.
                Tools.printLog("[ERROR] Too many iterations in a split up chat packet. Original message:");
                Tools.printLog(prefix + message);
                return;
            }

            // Find a suitable space for chopping the string.
            for(; i > currOffset + 2; i--) {
                if(msg[i] == 0x20) { // && (msg[i - 1] & 0x80) == 0 && (msg[i - 2] & 0x80) == 0) {
                    break;
                }
            }

            // No suitable spot found.
            if(i <= currOffset + 2) {
                len = sectionLength;
            } else {
                len = i - currOffset + 1;
            }

            // Copy over and send the partial message.
            if(pref == null) {
                splitMsg = new byte[len];
                System.arraycopy(msg, currOffset, splitMsg, 0, len);
            } else {
                splitMsg = new byte[len + pref.length];
                System.arraycopy(pref, 0, splitMsg, 0, pref.length);
                System.arraycopy(msg, currOffset, splitMsg, pref.length, len);
            }

            currOffset += len;

            sendChatPacketCore( messageType, soundCode, userID, splitMsg );
        }

        // Do the final block of the chopped up message.
        len = msg.length - currOffset;

        if(pref == null) {
            splitMsg = new byte[len];
            System.arraycopy( msg, currOffset, splitMsg, 0, len );
        } else {
            splitMsg = new byte[len + pref.length];
            System.arraycopy( pref, 0, splitMsg, 0, pref.length );
            System.arraycopy( msg, currOffset, splitMsg, pref.length, len );
        }

        sendChatPacketCore( messageType, soundCode, userID, splitMsg );
    }

    /**
        Original core code of the sendChatPacket function, to keep it all nice and tidy.
        @param messageType Type of message being sent -- public, error, etc. (0x00 to 0x09)
        @param soundCode Number of the sound attached to this message, if any
        @param userID For message types 0x04 and 0x05, player ID of target
        @param msg Encoded chat message to be sent.
    */
    private void sendChatPacketCore( byte messageType, byte soundCode, short userID, byte[] msg ) {
        int            size = msg.length + 6;
        ByteArray      bytearray = new ByteArray( size );

        bytearray.addByte( 0x06 );          // Type byte
        bytearray.addByte( messageType );
        bytearray.addByte( soundCode );
        bytearray.addLittleEndianShort( userID );
        bytearray.addByteArray( msg );
        bytearray.addByte( 0x00 );          // Terminator

        composeLowPriorityPacket( bytearray );

    }

    /**
        Sends numerous packets in one cluster (all packets queued by
        composePacket methods).
    */
    public void sendClusteredPacket() {
        int                 nextSize;
        int                 sizeLeft;
        ByteArray           bytearray;
        ByteArray           tempMessage;
        boolean             done = false;

        //Tools.printConnectionLog("SEND BI : (0x0E) Cluster");

        bytearray = new ByteArray( 500 );
        bytearray.addByte( 0x00 ); // Type byte (specifies bidirectional)
        bytearray.addByte( 0x0e ); // Bidirectional: Cluster packet
        sizeLeft = 498;

        synchronized (m_messageList) {
            // We might need to send more than one packet if the total of all messages
            // is longer than 500 bytes.
            m_messageList.resetCap();

            while( done == false ) {

                // We now need to add as many as we can before we fill up the 500 bytes.
                if( m_messageList.cappedSize() > 0 ) {
                    tempMessage = m_messageList.getNextPacket();
                    nextSize = tempMessage.size();
                } else {
                    done = true;
                    nextSize = 0;
                    tempMessage = null;
                }

                while( ((sizeLeft - (nextSize + 1)) > 0) && done == false ) {
                    bytearray.addByte( nextSize );
                    bytearray.addByteArray( tempMessage );

                    if( m_messageList.cappedSize() > 0 ) {
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
        Sends an Arena Left packet, notifying all that the bot has left the arena.
    */
    public void sendArenaLeft() {

        Tools.printConnectionLog("SEND    : (0x02) Leave arena");

        ByteArray ba = new ByteArray( 1 );
        ba.addByte( 0x02 );  // Type byte (nothing else needed)

        sendReliableMessage( ba );
    }

    /**
        Sends disconnect packet, informing the server that the client has
        disconnected.
    */
    public void sendDisconnect() {

        Tools.printConnectionLog("SEND BI : (0x07) Disconnect");

        ByteArray      bytearray = new ByteArray( 2 );

        bytearray.addByte( 0x00 ); // Type byte (specifies bidirectional)
        bytearray.addByte( 0x07 ); // Bidirectional: Disconnect

        composeImmediatePacket( bytearray, 2 );
    }

    /**
        Sends a request to spectate on a given player ID.
        @param playerID ID of player to spectate
    */
    public void sendSpectatePacket( short playerID ) {

        //Tools.printConnectionLog("SEND    : (0x08) Spectate player");

        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x08 );  // Type byte

        if( playerID == -1 ) {
            bytearray.addByte( 0xFF );  // All high bits = stop spectating
            bytearray.addByte( 0xFF );
        } else
            bytearray.addLittleEndianShort( playerID );

        composePacket( bytearray );
    }

    /**
        Sends instant spectate packet, for when fresh data on a specific player
        is absolutely required.
        @param playerID ID of player to immediately spectate
    */
    public void sendImmediateSpectatePacket( short playerID ) {

        //Tools.printConnectionLog("SEND IMM: (0x08) Spectate player");

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
        Sends a request to pick up the flag of the specified ID.
        @param flagID ID of flag to pick up
    */
    public void sendFlagRequestPacket( short flagID ) {

        Tools.printConnectionLog("SEND    : (0x13) Flag request");

        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x13 ); // Type byte
        bytearray.addLittleEndianShort( flagID );

        sendReliableMessage( bytearray );
    }

    /**
        Sends the signal to drop all flags carried.
    */
    public void sendFlagDropPacket( ) {

        Tools.printConnectionLog("SEND    : (0x15) Drop Flags");

        ByteArray   bytearray = new ByteArray( 1 );
        bytearray.addByte( 0x15 );  // Type byte

        sendReliableMessage( bytearray );
    }

    /**
        Sets a personal banner for the bot.  The banner is in BMP format
        without any palette information, size 12x8, for a total of 96 bytes.
        @param banner Banner data to set as bot's banner (96 bytes, 12x8 BMP, no palette)
    */
    public void sendBannerPacket( byte[] banner ) {

        Tools.printConnectionLog("SEND    : (0x19) Set personal banner");

        ByteArray bytearray = new ByteArray( 97 );
        bytearray.addByte( 0x19 );          // Type byte
        bytearray.addByteArray( banner );   // Banner without palette info

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
        Sends a request to pick up a ball.
        Added/written by Cheese
        @param ballID ID of ball to pick up
        @param timestamp Timestamp of last known ball position
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
        Sends a ball fire packet.
        Added/written by Cheese
        @param ballID ID of ball to fire
        @param xPosition Starting X position
        @param yPosition Starting Y position
        @param xVelocity Ball X velocity
        @param yVelocity Ball Y velocity
        @param playerID ID of player firing ball
    */
    public void sendBallFirePacket(byte ballID, short xPosition, short yPosition, short xVelocity, short yVelocity, short playerID)
    {
        Tools.printConnectionLog("SEND BF: (0x1F) Ball Fire");

        ByteArray bytearray = new ByteArray(16);
        bytearray.addByte(0x1F); //Type Byte
        bytearray.addByte(ballID);
        bytearray.addLittleEndianShort(xPosition);
        bytearray.addLittleEndianShort(yPosition);
        bytearray.addLittleEndianShort(xVelocity);
        bytearray.addLittleEndianShort(yVelocity);
        bytearray.addLittleEndianShort(playerID);
        bytearray.addLittleEndianInt((int)(System.currentTimeMillis() / 10) + m_serverTimeDifference);

        sendReliableMessage(bytearray);
    }

    /**
        Sets the bot as a given ship number.  Unlike TWCore ship numbering
        standards, the SS protocol uses ship 0 as warbird and 8 as spectator.
        @param ship Number of ship to change to; 0 is WB, 8 is spec
    */
    public void sendShipChangePacket( short ship ) {

        Tools.printConnectionLog("SEND    : (0x18) Set ship type");

        ByteArray   bytearray = new ByteArray( 2 );
        bytearray.addByte( 0x18 );  // Type byte
        bytearray.addByte( ship );

        sendReliableMessage( bytearray );
    }

    /**
        Sets the bot to a given frequency.
        @param freq Frequency to set to
    */
    public void sendFreqChangePacket( short freq ) {

        Tools.printConnectionLog("SEND    : (0x0F) Frequency Change");

        ByteArray   bytearray = new ByteArray( 3 );
        bytearray.addByte( 0x0F ); // Type byte
        bytearray.addLittleEndianShort( freq );

        sendReliableMessage( bytearray );
    }

    /**
        Requests an attachment to a specific player ID, or a detachment.
        If ID is -1, the operation is a detach.
        @param playerId PlayerID to attach to
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
        Sends a massive chunk packet to the server.  Used for file transfers,
        mostly only in the case of when the bot executes a *putfile.  (Else
        the server will probably reject it.)
        @param data Data to send
    */
    public void sendMassiveChunkPacket( ByteArray data ) {

        //Tools.printConnectionLog("SEND BI : (0x0A) HUGE Chunk");

        int            i, totalSize, size;
        ByteArray      bytearray;

        totalSize = data.size();
        i = totalSize;

        while( i > 0 ) {
            size = 480;             // Sent in chunks of 480 bytes at max

            if( i < size ) {
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
        Sends a file to the server.  Normally only used in conjunction with
        the *putfile command and the 0x19 S2C packet (see GamePacketInterpreter).
        @param fileName Name to associate with the file
        @param fileData File data, compressed using ZLib compression
    */
    public void sendFile( String fileName, ByteArray fileData ) {

        Tools.printConnectionLog("SEND    : (0x16) File transfer");

        ByteArray      data;

        data = new ByteArray( fileData.size() + 17 );
        data.addByte( 0x16 );  // Type byte
        data.addPaddedString( fileName, 16 );
        data.addByteArray( fileData );
        sendMassiveChunkPacket( data );
    }

    /**
        Send a death packet for the bot.  This is not done automatically
        after receiving terminal damage, but is instead the option of the
        bot controller.
        @param playerID ID of the player who killed the bot
        @param bounty Bot's bounty at the time of death
    */
    public void sendPlayerDeath( int playerID, int bounty ) {

        Tools.printConnectionLog("SEND    : (0x05) Death message");

        ByteArray      data;

        data = new ByteArray( 5 );
        data.addByte( 0x05 );  // Type byte
        data.addLittleEndianShort( (short)playerID );
        data.addLittleEndianShort( (short)bounty );

        // Sending reliably so that it's sent ASAP, and also confirmed, as
        // bot death is triggered only by code, and is important.
        sendReliableMessage( data );
    }

    /**
        Allows bot to be able to drop bricks.
        @param xLocation
        @param yLocation
    */
    public void sendDropBrick( int xLocation, int yLocation ) {

        Tools.printConnectionLog("SEND    : (0x1C) Drop Brick");

        ByteArray      data;

        data = new ByteArray( 5 );
        data.addByte( 0x1C );  // Type byte
        data.addLittleEndianShort( (short)xLocation );
        data.addLittleEndianShort( (short)yLocation );

        composePacket( data );
    }

    /**
        Adds an LVZ object ID state (whether to turn it on or off) to the object queue.
        Use {@link #sendLVZObjectCluster(int)} to send out a packet containing the
        toggling instructions.
        @param playerID ID of player to send to; use -1 for all players
        @param objID Object ID to toggle
        @param objVisible True if object is to be visible after being set; false if invisible
        @see #sendLVZObjectCluster(int)
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
        Adds multiple LVZ object IDs with the same state (whether to turn it on or off) to the object queue.
        Use {@link #sendLVZObjectCluster(int)} to send out a packet containing the
        toggling instructions.
        @param playerID ID of player to send to; use -1 for all players
        @param objID Object IDs to toggle
        @param objVisible True if object is to be visible after being set; false if invisible
        @see #sendLVZObjectCluster(int)
    */
    public void setupMultipleLVZObjectToggles( int playerID, Vector<Short> objID, boolean objVisible ) {
        if(objID == null)
            return;

        if( playerID == -1 ) {
            for(Short s : objID) {
                ByteArray objData = new ByteArray( 2 );
                objData.addLittleEndianShort( (short) (objVisible ? (s & 0x7FFF) : (s | 0x8000)) );
                m_lvzToggleCluster.addLast( objData );
            }
        } else {
            LinkedList <ByteArray>playerCluster = m_lvzPlayerToggleCluster.remove(playerID);

            if( playerCluster == null )
                playerCluster = new LinkedList<ByteArray>();

            for(Short s : objID) {
                ByteArray objData = new ByteArray( 2 );
                objData.addLittleEndianShort( (short) (objVisible ? (s & 0x7FFF) : (s | 0x8000)) );
                playerCluster.addLast( objData );
            }

            m_lvzPlayerToggleCluster.put( playerID, playerCluster );
        }
    }

    /**
        Adds one or more LVZ object ID states (whether to turn it on or off) to an object
        toggle queue at once, using a HashMap with (Integer)ObjectID-&gt;(Boolean)Visible
        mappings.  Use {@link #sendLVZObjectCluster(int)} to send out a packet containing the
        toggling instructions.
        @param playerID ID of player to send to; use -1 for all players
        @param toggles Mapping of object ID to visibility state to set
        @see #sendLVZObjectCluster(int)
    */
    public void setupMultipleLVZObjectToggles( int playerID, HashMap<Integer, Boolean> toggles ) {
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
        Sends the current LVZ object toggle cluster to the player specified by the ID;
        -1 for all players.  Use {@link #setupLVZObjectToggle(int, int, boolean)} to add objects
        before sending out the packet.
        @param playerID ID of player to send to; use -1 for all players
        @see #setupLVZObjectToggle(int, int, boolean)
        @see #setupMultipleLVZObjectToggles(int, HashMap)
    */
    public void sendLVZObjectCluster( int playerID ) {

        //Tools.printConnectionLog("SEND    : (0x0A) LVZ Object Cluster");

        ByteArray data = null;

        // First convert toggle data into a byte array. If no valid data is found, the bytearray will remain null.
        if(playerID == -1) {
            if ( m_lvzToggleCluster.size() > 0 ) {
                data = new ByteArray( m_lvzToggleCluster.size() * 2 );

                while( m_lvzToggleCluster.size() > 0 )
                    data.addLittleEndianShort( m_lvzToggleCluster.removeFirst().readLittleEndianShort(0) );
            }

        } else {
            LinkedList <ByteArray>playerCluster = m_lvzPlayerToggleCluster.remove( playerID );

            if ( playerCluster != null && playerCluster.size() > 0 ) {
                data = new ByteArray( playerCluster.size() * 2 );

                while( playerCluster.size() > 0 )
                    data.addLittleEndianShort( playerCluster.removeFirst().readLittleEndianShort(0) );
            }
        }

        // Determine whether the data is sufficiently small enough to be send by means of a low priority packet.
        // Previous testing indicated that 39 combined object toggles were fine, but the used bot started acting
        // erratically or even disconnected when 77 combined object toggles were sent through a low priority packet.
        // Further research will have to point out what the exact safe limit is, but going for a wild guess here at 50 for now.
        if( data == null ) {
            // No data to be sent.
            return;
        } else if( data.size() <= 100 ) {
            //TODO Test proper limit for this.
            // Fifty objects or less, send unreliably. ( 100 == 50 objects * 2 bytes )
            ByteArray objPacket = new ByteArray( data.size() + 4 );
            objPacket.addByte( 0x0A );  // Type byte
            objPacket.addLittleEndianShort( ((short) (playerID >= 0 ? (playerID & 0xFFFF) : 0xFFFF) ) ); // Player id, -1 for all.
            objPacket.addByte( 0x35 );  // LVZ Object toggle packet type byte
            objPacket.addByteArray( data );
            composeLowPriorityPacket( objPacket );
        } else {
            // More than 50 objects. Send reliably and split up if needed.
            int totalSize = data.size();
            int i = totalSize;
            int size;

            while( i > 0 ) {
                size = 440;             // Sent in chunks of 440 bytes at max (220 objects)

                if( i < size ) {
                    size = i;
                }

                ByteArray objPacket = new ByteArray( size + 4 );
                objPacket.addByte( 0x0A ); // Encapsulating packet type byte.
                objPacket.addLittleEndianShort( ((short) (playerID >= 0 ? (playerID & 0xFFFF) : 0xFFFF) ) ); // Player id, -1 for all.
                objPacket.addByte( 0x35 ); // LVZ Object toggle packet type byte.
                objPacket.addPartialByteArray( data, totalSize - i, size );
                sendReliableMessage( objPacket );

                i -= size;
            }
        }
    }

    /**
        Sends a single LVZ object toggle to the player specified by the ID;
        -1 for all players.  Using this method does not interfere with or check
        against the object toggling queues in any way.
        @param objID Object ID to toggle
        @param playerID ID of player to send to; use -1 for all players
        @param objVisible True if object is to be visible after being set; false if invisible
    */
    public void sendSingleLVZObjectToggle( int playerID, int objID, boolean objVisible ) {

        //Tools.printConnectionLog("SEND    : (0x0A) LVZ Object Toggle");

        ByteArray objPacket = new ByteArray( 6 );
        objPacket.addByte( 0x0A );  // Type byte
        objPacket.addLittleEndianShort( ((short) (playerID >= 0 ? (playerID & 0xFFFF) : 0xFFFF) ) );
        objPacket.addByte( 0x35 );  // LVZ packet type byte
        objPacket.addLittleEndianShort( (short) (objVisible ? (objID & 0x7FFF) : (objID | 0x8000)) );
        composeLowPriorityPacket( objPacket );
    }

    /**
        Adds an modified LVZ object to the object queue.
        Use {@link #sendLVZObjectModCluster(int)} to send out a packet containing the
        modification instructions.
        @param playerID ID of player to send to; use -1 for all players.
        @param obj LVZ Object containing the modifications.
        @see #sendLVZObjectModCluster(int)
        @see LvzObject
    */
    public void setupLVZObjectMod( int playerID, LvzObject obj ) {
        if( playerID == -1 )
            m_lvzModCluster.addLast( obj );
        else {
            LinkedList <LvzObject>playerCluster = m_lvzPlayerModCluster.remove(playerID);

            if( playerCluster == null )
                playerCluster = new LinkedList<LvzObject>();

            playerCluster.addLast( obj );
            m_lvzPlayerModCluster.put( playerID, playerCluster );
        }
    }

    /**
        Adds one or more modified LVZ object to an object modification queue at once,
        using a LinkedList[LvzObject] as an array.
        Use {@link #sendLVZObjectModCluster(int)} to send out a packet containing the
        modification instructions.
        @param playerID ID of player to send to; use -1 for all players.
        @param objs Linked list of all the modifications of all the objects.
        @see #sendLVZObjectModCluster(int)
        @see LvzObject
    */
    public void setupMultipleLVZObjectMod(int playerID, LinkedList<LvzObject> objs ) {
        if( objs == null )
            return;

        if( playerID == -1 ) {
            m_lvzModCluster.addAll( objs );
        } else {
            LinkedList <LvzObject> playerCluster = m_lvzPlayerModCluster.remove(playerID);

            if( playerCluster == null )
                playerCluster = new LinkedList<LvzObject>();

            playerCluster.addAll( objs );

            m_lvzPlayerModCluster.put( playerID, playerCluster );
        }
    }

    /**
        Sends the current LVZ object modification cluster to the player specified by the ID;
        -1 for all players.  Use {@link #setupLVZObjectMod(int, LvzObject)} or
        {@link #setupMultipleLVZObjectMod(int, LinkedList)} to add objects
        before sending out the packet.
        @param playerID ID of player to send to; use -1 for all players.
        @see #setupLVZObjectMod(int, LvzObject)
        @see #setupMultipleLVZObjectMod(int, LinkedList)
        @see LvzObject
    */
    public void sendLVZObjectModCluster( int playerID ) {

        //Tools.printConnectionLog("SEND    : (0x0A) Containing 0x36 LVZ Object Modification Cluster");

        ByteArray objData = null;

        // Construct the data.
        if( playerID == -1 ) {
            if( m_lvzModCluster.size() > 0 ) {
                objData = new ByteArray( m_lvzModCluster.size() * 11);

                while( m_lvzModCluster.size() > 0 )
                    objData.addByteArray( m_lvzModCluster.removeFirst().objUpdateInfo );
            }
        } else {
            LinkedList<LvzObject> playerCluster = m_lvzPlayerModCluster.remove(playerID);

            if( playerCluster != null && playerCluster.size() > 0 ) {
                objData = new ByteArray( playerCluster.size() * 11 );

                while( playerCluster.size() > 0 )
                    objData.addByteArray( playerCluster.removeFirst().objUpdateInfo );
            }
        }

        // Check if anything needs to be sent.
        if(objData == null)
            return;

        // Automatically break up the data if it's too big.
        int totalSize = objData.size();
        int i = totalSize;
        int size;

        while( i > 0 ) {
            size = 440;             // Sent in chunks of 440 bytes at max (40 objects)

            if( i < size ) {
                size = i;
            }

            ByteArray objPacket = new ByteArray( size + 4 );
            objPacket.addByte( 0x0A ); // Encapsulating packet type byte.
            objPacket.addLittleEndianShort( ((short) (playerID >= 0 ? (playerID & 0xFFFF) : 0xFFFF) ) );
            objPacket.addByte( 0x36 );
            objPacket.addPartialByteArray( objData, totalSize - i, size );
            sendReliableMessage( objPacket );

            i -= size;
        }
    }

    /**
        Sends a single LVZ object modification to the player specified by the ID;
        -1 for all players.  Using this method does not interfere with or check
        against the object toggling or modification queues in any way.
        @param playerID ID of player to send to; use -1 for all players.
        @param obj LVZ Object containing the modifications.
        @see LvzObject
    */
    public void sendSingleLVZObjectMod( int playerID, LvzObject obj ) {

        //Tools.printConnectionLog("SEND    : (0x0A) Containing 0x36 LVZ Object Modification");

        ByteArray objPacket = new ByteArray( 15 );
        objPacket.addByte( 0x0A );  // Encapsulating packet type byte.
        objPacket.addLittleEndianShort( ((short) (playerID >= 0 ? (playerID & 0xFFFF) : 0xFFFF) ) );
        objPacket.addByte( 0x36 );  // Core packet type byte. (LVZ Object modification.)
        objPacket.addByteArray(obj.objUpdateInfo);
        composeLowPriorityPacket( objPacket );
    }

    /**
        Sends a signal to the server that this client is no longer in the King of the Hill game.
        (In Continuum this only happens when you die or the timer runs out.)
    */
    public void sendEndKoTH() {

        Tools.printConnectionLog("SEND    : (0x1E) End Personal KoTH Timer");

        ByteArray objPacket = new ByteArray( 1 );
        objPacket.addByte( 0x1E );
        composePacket(objPacket);
    }

    /**
        Sends a packet to the server to request the transfer of a level file.
        The level that will be sent is the one that is linked to the arena the bot is in.
    */
    public void sendLevelRequest() {
        Tools.printConnectionLog("SEND    : (0x0C) Request level file.");

        ByteArray message = new ByteArray( 1 );
        message.addByte( 0x0C );
        composeImmediatePacket(message, 1);
    }

    /**
        Sends a security checksum packet as a response to a security synchronization request. This is generally
        received every two minutes. If this packet is not answered or answered incorrectly, the server will
        generate an error, unless the bot is on the sysop list.
        @param parameterChecksum Checksum based on the arena settings.
        @param EXEChecksum Checksum based on the continuum/subspace executable.
        @param levelChecksum Checksum based on the current arena the bot is in.
        @param S2CRelOut Amount of reliable packets received by the client?
        @param weaponCount Amount of position packets received that contained weapon data.
        @param S2CSlowCurrent Amount of slow packets received since the last check?
        @param S2CFastCurrent Amount of fast packets received since the last check?
        @param S2CSlowTotal Amount of slow packets received in total.
        @param S2CFastTotal Amount of fast packets received in total.
        @param slowFrame Is slow frame detected?
    */
    public void sendSecurityChecksum(int parameterChecksum, int EXEChecksum, int levelChecksum, short S2CRelOut, int weaponCount,
                                     short S2CSlowCurrent, short S2CFastCurrent, short S2CSlowTotal, short S2CFastTotal, boolean slowFrame) {
        Tools.printConnectionLog("SEND    : (0x1A) Security Checksum");

        /*  Field   Length  Description
            0       1       Type byte
            1       4       Weapon count
            5       4       Parameter checksum
            9       4       EXE checksum
            13      4       Level checksum
            17      4       S2CSlowTotal
            21      4       S2CFastTotal
            25      2       S2CSlowCurrent
            27      2       S2CFastCurrent
            29      2       ? S2CRelOut
            31      2       Ping
            33      2       Average ping
            35      2       Low ping
            37      2       High ping
            39      1       Boolean: Slow frame detected
        */

        ByteArray      data;

        data = new ByteArray( 40 );
        data.addByte( 0x1A );  // Type byte
        data.addLittleEndianInt( weaponCount );
        data.addLittleEndianInt( parameterChecksum );
        data.addLittleEndianInt( EXEChecksum );
        data.addLittleEndianInt( levelChecksum );
        data.addLittleEndianInt( S2CSlowTotal );
        data.addLittleEndianInt( S2CFastTotal );
        data.addLittleEndianShort( S2CSlowCurrent );
        data.addLittleEndianShort( S2CFastCurrent );
        data.addLittleEndianShort( S2CRelOut );
        data.addLittleEndianShort( (short) m_syncPing );
        data.addLittleEndianShort( (short) m_avgPing );
        data.addLittleEndianShort( (short) m_lowPing );
        data.addLittleEndianShort( (short) m_highPing );
        data.addByte( (slowFrame ? 0x01 : 0x00) );

        sendReliableMessage( data );
    }
    /**
        Implementation to send out most packets as normal, while placing a cap
        on the number of less-important packets (generally messages) that are
        sent out per run of the cluster packet composition timer.
        @author dugwyler
        @param <E>
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
