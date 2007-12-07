package twcore.core.net;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.Session;
import twcore.core.SubspaceBot;
import twcore.core.events.*;
import twcore.core.game.Arena;
import twcore.core.util.ByteArray;
import twcore.core.util.MessageLimiter;
import twcore.core.util.Tools;

/**
 * Interprets packet data sent from a server using the SS protocol.  The packets
 * are then translated into events (which extend SubspaceEvent) and sent to
 * bots and other classes that need to be updated with what is going on in the
 * game.  This class is one of the most essential, as it parses game happenings
 * out to bots in a format they can read.
 */
public class GamePacketInterpreter {

    private Session                 m_session;              // Ref to bot's Session
    private ByteArray               m_chunkArray;           // Holds packet chunks
    private String                  m_playerName;           // Bot's login
    private SubspaceBot             m_subspaceBot;          // Ref to bot class
    private SSEncryption            m_ssEncryption;         // Encryption scheme
    private Arena                   m_arenaTracker;         // Current arena info
    private String                  m_playerPassword;       // Bot's password
    private GamePacketGenerator     m_packetGenerator;      // Packet gen ref
    private int                     m_massiveChunkCount;    // Handles chunk pkt sizes
    private ByteArray               m_massiveChunkArray;    // Lg chunk packet data
    private ReliablePacketHandler   m_reliablePacketHandler;// Reliable receives
    private EventRequester          m_requester;            // Checks if event req.
    private MessageLimiter          m_limiter = null;       // Limits msgs sent
    private int                     m_verboseLogin;         // 1: send verbose login msg

    /**
     * Creates a new instance of GamePacketInterpreter.
     * @param session Session this GPI is attached to
     * @param packetGenerator For creating new packets in response to certain packets interpreted
     * @param ssEncryption Encryption class for decrypting packets
     * @param arenaTracker Arena tracker to update with certain packet info
     * @param name Bot's login name, sent after encryption keys are exchanged
     * @param password Bot's password, sent after encryption keys are exchanged
     */
    public GamePacketInterpreter( Session session,
    GamePacketGenerator packetGenerator, SSEncryption ssEncryption,
    Arena arenaTracker, String name, String password ){
        m_requester = session.getEventRequester();
        m_playerName = name;
        m_playerPassword = password;
        m_session = session;
        m_chunkArray = null;
        m_subspaceBot = null;
        m_massiveChunkCount = 0;
        m_arenaTracker = arenaTracker;
        m_ssEncryption = ssEncryption;
        m_packetGenerator = packetGenerator;
        m_verboseLogin = session.getCoreData().getGeneralSettings().getInt("VerboseLogin");
    }

    /**
     * Sets the bot to receive only a certain number of messages per minute.
     * Does not apply to HighMod+.  Set to 0 to disable a previously active
     * limiter.
     * @param msgsPerMin Number of messages to limit per minute; 0 to disable
     * @param botAction Reference to BotAction class
     */
    public void setMessageLimiter( int msgsPerMin, BotAction botAction ){
        if( msgsPerMin >= 1 ){
            m_limiter = new MessageLimiter( botAction, m_subspaceBot, msgsPerMin );
        } else {
            m_limiter = null;
        }
    }

    /**
     * Sets reference to the bot's main class.
     * @param subspaceBot Bot's main class
     */
    public void setSubspaceBot( SubspaceBot subspaceBot ){
        m_subspaceBot = subspaceBot;
    }

    /**
     * Sets the reliable packet handler.
     * @param reliablePacketHandler Reliable packet handler instantiation
     */
    public void setReliablePacketHandler( ReliablePacketHandler reliablePacketHandler ){
        m_reliablePacketHandler = reliablePacketHandler;
    }

    /**
     * Translates a game packet as either a "special" (bi-drectional) or
     * standard S2C packet.  Bi-directional packets have their first byte,
     * normally the type byte, set to 0, with the second byte acting as the
     * specific type byte.
     * @param array
     * @param alreadyDecrypted
     */
    public void translateGamePacket( ByteArray array, boolean alreadyDecrypted ){
        int index = array.readByte( 0 ) & 0xff;

        if( index == 0 ){
            translateSpecialPacket( array, alreadyDecrypted );
        } else {
            translateNormalPacket( array, alreadyDecrypted );
        }
    }

    /**
     * Translates a "normal" packet, i.e., standard one-way S2C packet from the
     * SS server.  Packets beginning with 0x00 are considered special packets
     * and are handled elsewhere.
     * <p>
     * This method will create and distribute events to bots, the Arena object,
     * etc. as necessary.
     * <p>
     * Note that only a selection of packet types are handled.  TWCore is set
     * up to interpret the packets it currently uses.  Other packet types can
     * be handled fairly easily.  To add a new event type:
     * <p>
     * 1. Create a new event that extends SubspaceEvent and can parse out the
     * data from a ByteArray containing information inside the packet.<br>
     * 2. In EventRequester, add a new index for the event.<br>
     * 3. In SubspaceBot, add a method for default handling of the event, so
     * that a bot wishing to request the event can override that method.<br>
     * 4. Create a new method in this class to handle the specific packet type,
     * being sure to check if it's decrypted, and checking if the bot's event
     * requester has requested the event before sending it out.<br>
     * 5. Add a new case in this switch statement corresponding to the event's
     * type, and have it send the information to your handling method in this class.<br>
     * 6. Start handling the event in your bot!<br>
     *
     * @param array Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void translateNormalPacket( ByteArray array, boolean alreadyDecrypted ){
        int index = array.readByte( 0 ) & 0xff;
        switch( index ){
            // 0x00 - Special packet (see translateSpecialPacket())
            // 0x01 - Bot's player ID has changed: Unhandled
            case 0x02:
                handleArenaJoined( array, alreadyDecrypted );
                break;
            case 0x03:
                handlePlayerEntered( array, alreadyDecrypted );
                break;
            case 0x04:
                handlePlayerLeft( array, alreadyDecrypted );
                break;
            case 0x05:
                handlePlayerPosition( array, alreadyDecrypted );
                break;
            case 0x06:
                handlePlayerDeath( array, alreadyDecrypted );
                break;
            case 0x07:
                handleChatMessage( array, alreadyDecrypted );
                break;
            case 0x08:
                handlePrize( array, alreadyDecrypted );
                break;
            case 0x09:
                handleScoreUpdate( array, alreadyDecrypted );
                break;
            case 0x0A:
                handlePasswordPacketResponse( array, alreadyDecrypted );
                break;
            case 0x0B:
                handleSoccerGoal( array, alreadyDecrypted );
                break;
            // 0x0C - Player Voice: Unhandled
            case 0x0D:
                handleFreqChange( array, alreadyDecrypted );
                break;
            case 0x0E:
                handleTurret( array, alreadyDecrypted );
                break;
            case 0x0F:
                handleArenaSettings( array, alreadyDecrypted );
                break;
            case 0x10:
                handleFileArrived( array, alreadyDecrypted );
                break;
            case 0x12:
                handleFlagPosition( array, alreadyDecrypted );
                break;
            case 0x13:
                handleFlagClaimed( array, alreadyDecrypted );
                break;
            case 0x14:
                handleFlagVictory( array, alreadyDecrypted );
                break;
            // 0x15 - Destroy Turret Link: Unhandled
            case 0x16:
                handleFlagDropped( array, alreadyDecrypted );
                break;
            // 0x17 - Unknown Packet: Unhandled
            // 0x18 - Synchronization Request: Unhandled (This is why bot must be a sysop)
            //        On Sync request, send 0x1A - Security checksum
            case 0x19:
                handleFileRequest( array, alreadyDecrypted );
                break;
            case 0x1A:
                handleScoreReset( array, alreadyDecrypted );
                break;
            // 0x1B - Personal ship reset: Unhandled
            // 0x1C - Put player in spectator mode: Unhandled
            case 0x1D:
                handleShipFreqChange( array, alreadyDecrypted );
                break;
            // 0x1E - Personal banner changed: Unhandled
            case 0x1F:
                handlePlayerBanner( array, alreadyDecrypted );
                break;
            // 0x20 - Bot picked up a prize: Unhandled
            // 0x21 - A player dropped a brick: Unhandled
            case 0x22:
                handleTurfFlagUpdate( array, alreadyDecrypted );
                break;
            case 0x23:
                handleFlagReward( array, alreadyDecrypted );
                break;
            // 0x24 - Speed game over: Unhandled
            // 0x25 - Bot's UFO flag toggled: Unhandled
            // 0x26 - Unknown: Unhandled
            // 0x27 - "Keep-Alive": Unhandled
            case 0x28:
                handlePlayerPosition( array, alreadyDecrypted );
                break;
            // 0x29 - Map information (basic): Unhandled
            // 0x2A - Compressed map file: Unhandled
            // 0x2B - Set bot's KotH timer: Unhandled
            // 0x2C - KotH Game Reset: Unhandled
            // 0x2D - Unknown: Unhandled
            case 0x2E:
                handleBallPosition( array, alreadyDecrypted );
                break;
            case 0x2F:
                handleArenaList( array, alreadyDecrypted );
                break;
            // 0x30 - Received zone banner ads: Unhandled
            // 0x31 - Bot is past the login sequence: Unhandled
            // ** CONTINUUM SPECIFIC PACKETS BELOW **
            // 0x32 - Change bot's ship coords: Unhandled
            // 0x33 - Custom login failure message: Unhandled
            // 0x34 - Continuum version packet: Unhandled
            // 0x35 - Object toggling: Unhandled
            // 0x36 - Unknown: Unhandled
            // 0x37 - Unknown: Unhandled
            case 0x38:
                handleWatchDamage( array, alreadyDecrypted );
                break;
            // 0x3B - *sendto packet: See http://forums.minegoboom.com/viewtopic.php?p=75317#75317
        }
    }

    /**
     * Translates a "special" packet, i.e., a bi-directional packet that can be
     * sent either to or from the server (in this case of course it's being sent
     * from the server).  Special packets have the first byte, normally the type
     * byte, set to 0x00, and use the second byte instead as their type byte.
     *
     * @param array Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void translateSpecialPacket( ByteArray array, boolean alreadyDecrypted ){
        int index = array.readByte( 1 ) & 0xff;

        if( alreadyDecrypted == false && index != 2 ){
            m_ssEncryption.decrypt( array, array.size() - 2, 2 );
        }

        switch( index ){
            // 0x01 - Encryption request: Unhandled (sent to server in GamePacketGenerator)
            case 0x02:              // Encryption response
                m_ssEncryption.setServerKey( array.readLittleEndianInt( 2 ) );
                m_packetGenerator.sendPasswordPacket( false, m_playerName, m_playerPassword );
                Tools.printLog( m_session.getBotName() + " (" + m_subspaceBot.getClass().getSimpleName() + ") is logging in ..." );
                break;
            case 0x03:              // Reliable packet message
                m_reliablePacketHandler.handleReliableMessage( array );
                break;
            case 0x04:              // Reliable ACK
                m_reliablePacketHandler.handleAckMessage( array );
                break;
            case 0x05:              // Sync request
                m_packetGenerator.sendSyncResponse( array.readLittleEndianInt( 2 ) );
                break;
            case 0x06:              // Sync response
                m_packetGenerator.setServerTimeDifference( array.readLittleEndianInt( 6 ) - array.readLittleEndianInt( 2 ) );
                break;
            case 0x07:              // Order to disconnect
                m_session.disconnect( "received bi-directional packet 0x07 from server (ordered to DC)" );
                break;
            case 0x08:              // Small chunk body (store data in buffer until tail is received)
                handleChunk( array );
                break;
            case 0x09:              // Small chunk tail (end of data received; process chunk in buffer)
                handleChunkTail( array );
                break;
            case 0x0A:              // Massive chunk (store data in buffer until total length received)
                handleMassiveChunkMessage( array );
                break;
            // 0x0B-0x0D - Unknown (unused in protocol?)
            case 0x0E:              // Packet cluster (packet lengths and packet data repeated till end)
                int         i=2;
                int         size;
                ByteArray   subMessage;

                while( i<array.size() ){
                    size = array.readByte( i ) & 0xff;
                    subMessage = new ByteArray( size );
                    subMessage.addPartialByteArray( array, 0, i+1, size );
                    translateGamePacket( subMessage, true );
                    i += size + 1;
                }
                break;
        }
    }

    /**
     * Handles the body portion of a small chunk packet.
     * @param message Packet chunk data
     */
    public void handleChunk( ByteArray message ){
        int         oldSize;

        if( m_chunkArray == null ){
            m_chunkArray = new ByteArray( message.size() - 2 );
            m_chunkArray.addPartialByteArray( message, 2, message.size() - 2 );
        } else {
            oldSize = m_chunkArray.size();
            m_chunkArray.growArray( oldSize + message.size() - 2 );
            m_chunkArray.addPartialByteArray( message, 2, message.size() - 2 );
        }
    }

    /**
     * Handles the tail portion of a small chunk packet.
     * @param message Packet chunk data
     */
    public void handleChunkTail( ByteArray message ){
        int         oldSize;

        oldSize = m_chunkArray.size();
        m_chunkArray.growArray( oldSize + message.size() - 2 );
        m_chunkArray.addPartialByteArray( message, 2, message.size() - 2 );
        translateGamePacket( m_chunkArray, true );
        m_chunkArray = null;
    }

    /**
     * Handles a massive chunk packet.
     * @param message Packet chunk data
     */
    public void handleMassiveChunkMessage( ByteArray message ){

        if( m_massiveChunkCount == 0 ){
            m_massiveChunkCount = message.readLittleEndianInt( 2 );
            m_massiveChunkArray = new ByteArray( m_massiveChunkCount );
        }

        m_massiveChunkArray.addPartialByteArray( message, 6, message.size() - 6 );
        m_massiveChunkCount -= (message.size() - 6);

        if( m_massiveChunkCount <= 0 ){
            translateGamePacket( m_massiveChunkArray, true );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * FileArrived is unique in that it creates a new file with the data it has
     * received from the server, and then passes the filename on to the event
     * rather than the data itself.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    public void handleFileArrived( ByteArray message, boolean alreadyDecrypted ){

        String          fileName;

        try {
            fileName = message.readString( 1, 16 ).trim();
            BufferedWriter fileWriter = new BufferedWriter( new FileWriter(
            m_session.getBotAction().getDataFile( fileName )));
            fileWriter.write( message.readString( 17, message.size() - 17 ) );
            fileWriter.close();
            if( m_requester.check( EventRequester.FILE_ARRIVED )){
                ByteArray       fileNameArray = new ByteArray( 16 );

                fileNameArray.addString( fileName );
                m_subspaceBot.handleEvent( new FileArrived( fileNameArray ) );
            }
        } catch( Exception e ){
            Tools.printStackTrace( e );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * If the message limiter is active, Message events are handled by the message
     * limiter before they are sent to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleChatMessage( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() <= 6 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        Message event = new Message(message);

		if(m_session.getChatLog() != null) // Logging is enabled
		{
			synchronized(m_session.getChatLog())
			{
				if(event.getMessageType() == Message.PRIVATE_MESSAGE)
				{
					PrintWriter out = m_session.getChatLog();
					out.print(Tools.getTimeStamp() + " (");
					out.print(m_session.getBotAction().getBotName() + ") : ");
					out.print(m_session.getBotAction().getPlayerName(event.getPlayerID()));
					out.println(" > "+ event.getMessage());
					out.flush();
				}
			}
		}

        if( m_requester.check( EventRequester.MESSAGE )){
            if( m_limiter == null ){
                m_subspaceBot.handleEvent( event );
            } else {
                m_limiter.handleEvent( event );
            }
        }
    }

    /**
     * Translates packet into the appropriate event.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handlePrize( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 13 ){
            return;
        }

        if( !alreadyDecrypted ) {
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        if( m_requester.check( EventRequester.PRIZE ) ) {
            m_subspaceBot.handleEvent( new Prize( message ) );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleSoccerGoal( ByteArray message, boolean alreadyDecrypted ) {
        // Check for valid message
        if( message.size() < 7 ) {
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        if( m_requester.check( EventRequester.SOCCER_GOAL )){
            m_subspaceBot.handleEvent( new SoccerGoal( message ) );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleFlagPosition( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 9 ) {
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        m_arenaTracker.processEvent( new FlagPosition( message ) );

        if( m_requester.check( EventRequester.FLAG_POSITION )) {
            m_subspaceBot.handleEvent( new FlagPosition( message ) );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleFlagClaimed( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 5 ) {
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        m_arenaTracker.processEvent( new FlagClaimed( message ) );

        if( m_requester.check( EventRequester.FLAG_CLAIMED )) {
            m_subspaceBot.handleEvent( new FlagClaimed( message ) );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleFlagDropped( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 3 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        m_arenaTracker.processEvent( new FlagDropped( message ) );

        if( m_requester.check( EventRequester.FLAG_DROPPED ) ){
            m_subspaceBot.handleEvent( new FlagDropped( message ) );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleBallPosition( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 16 ) {
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        if( m_requester.check( EventRequester.BALL_POSITION ) ) {
            m_subspaceBot.handleEvent( new BallPosition( message ) );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleTurret( ByteArray message, boolean alreadyDecrypted ){
        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        m_arenaTracker.processEvent( new TurretEvent( message ) );

        if( m_requester.check( EventRequester.TURRET_EVENT ) ) {
            m_subspaceBot.handleEvent( new TurretEvent( message ) );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handlePlayerEntered( ByteArray message, boolean alreadyDecrypted ){
        ByteArray        subMessage;

        // Check for valid message
        if( message.size() < 64 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        subMessage = new ByteArray( 64 );
        // More than one player entered message can be in each packet
        for( int i=0; i<message.size(); i+=64 ){
            subMessage.addPartialByteArray( message, 0, i, 64 );
            PlayerEntered player = new PlayerEntered( subMessage );

            m_arenaTracker.processEvent( player );

            if( m_requester.check( EventRequester.PLAYER_ENTERED )){
                m_subspaceBot.handleEvent( player );
            }
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handlePlayerLeft( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 3 ){ //CHECK THIS
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        PlayerLeft left = new PlayerLeft( message );

        if( m_requester.check( EventRequester.PLAYER_LEFT )){
            m_subspaceBot.handleEvent( left );
        }

        m_arenaTracker.processEvent( left );
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * Player positions are retrieved from two different packets: a short position
     * packet and a long / weapons position packet.  The short position packet is
     * far more common, with the long position packet generally only being sent
     * when the player uses an item or weapon.  Also, position packets are not
     * sent when a player travels in a straight line, because the player is
     * assumed to be moving at a standard rate.  It is because of Subspace's
     * simplified and constant space physics that it makes it so easy for a client
     * algorithm to predict movement, and thus avoid the sending of unnecessary
     * packets to cut down on network use.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handlePlayerPosition( ByteArray message, boolean alreadyDecrypted ){
        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }
        try{
            PlayerPosition    playerPosition = new PlayerPosition( message );
            m_arenaTracker.processEvent( playerPosition );

            if( m_requester.check( EventRequester.PLAYER_POSITION )){
                m_subspaceBot.handleEvent( playerPosition );
            }

            if( m_requester.check( EventRequester.WEAPON_FIRED )){
                if( playerPosition.containsWeaponsInfo()){
                    m_subspaceBot.handleEvent( new WeaponFired( message ));
                }
            }
        } catch( Exception e ){
            Tools.printStackTrace( e );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleScoreUpdate( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 15 ){ //CHECK THIS
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        ScoreUpdate update = new ScoreUpdate( message );
        m_arenaTracker.processEvent( update );

        if( m_requester.check( EventRequester.SCORE_UPDATE )){
            m_subspaceBot.handleEvent( update );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleFreqChange( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 6 ){ //CHECK THIS
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        FrequencyChange update = new FrequencyChange( message );
        m_arenaTracker.processEvent( update );

        if( m_requester.check( EventRequester.FREQUENCY_CHANGE )){
            m_subspaceBot.handleEvent( update );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleShipFreqChange( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 6 ){ //CHECK THIS
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        FrequencyShipChange update = new FrequencyShipChange( message );
        m_arenaTracker.processEvent( update );

        if( m_requester.check( EventRequester.FREQUENCY_SHIP_CHANGE )){
            m_subspaceBot.handleEvent( update );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handlePlayerBanner( ByteArray message, boolean alreadyDecrypted ) {
        // Check for valid message
        if( message.size() < 99 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        PlayerBanner banner = new PlayerBanner( message );

        if( m_requester.check( EventRequester.PLAYER_BANNER )){
            m_subspaceBot.handleEvent( banner );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleWatchDamage( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 16 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        WatchDamage        watchDamage = new WatchDamage( message );

        if( m_requester.check( EventRequester.WATCH_DAMAGE )){
            m_subspaceBot.handleEvent( watchDamage );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handlePlayerDeath( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 10 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        PlayerDeath         playerDeath = new PlayerDeath( message );

        m_arenaTracker.processEvent( playerDeath );

        if( m_requester.check( EventRequester.PLAYER_DEATH ) ){
            m_subspaceBot.handleEvent( playerDeath );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleScoreReset( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 3 ){ //CHECK THIS
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        ScoreReset reset = new ScoreReset( message );
        m_arenaTracker.processEvent( reset );

        if( m_requester.check( EventRequester.SCORE_RESET ) ){
            m_subspaceBot.handleEvent( new ScoreReset( message ) );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleFlagVictory( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 7 ){ //CHECK THIS
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        FlagVictory         flagVictory = new FlagVictory( message );
        m_arenaTracker.processEvent( flagVictory );

        if( m_requester.check( EventRequester.FLAG_VICTORY ) ){
            m_subspaceBot.handleEvent( flagVictory );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleFlagReward( ByteArray message, boolean alreadyDecrypted ){
        ByteArray        subMessage;

        if( message.size() < 5 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        subMessage = new ByteArray( 5 );

        // More than one flag reward message can be in each packet
        for( int i=1; i<message.size(); i+=4 ){
            subMessage.addPartialByteArray( message, 1, i, 4 );
            FlagReward flagReward = new FlagReward( subMessage );

            m_arenaTracker.processEvent( flagReward );

            if( m_requester.check( EventRequester.FLAG_REWARD ) ){
                m_subspaceBot.handleEvent( flagReward );
            }
        }
    }

    /**
     * Translates packet into the appropriate event.
     * <p>
     * This event is sent to the Arena tracker in addition to the bot.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleTurfFlagUpdate( ByteArray message, boolean alreadyDecrypted ) {

        ByteArray subMessage;

        if( message.size() < 3 ) {
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        subMessage = new ByteArray( 3 );
        // More than one turf flag update message can be in each packet
        for( int i = 1, j = 0; i < message.size(); i+=2, j++) {
            subMessage.addPartialByteArray( message, 1, i, 2 );
            TurfFlagUpdate turfFlagUpdate = new TurfFlagUpdate( subMessage, j );
            m_arenaTracker.processEvent( turfFlagUpdate );

            if( m_requester.check( EventRequester.TURF_FLAG_UPDATE ) ) {
                m_subspaceBot.handleEvent( turfFlagUpdate );
            }
        }

    }

    /**
     * Translates packet into the appropriate event.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleArenaList( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 3 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        // Do we care about arena lists?
        if( m_requester.check( EventRequester.ARENA_LIST )){
            m_subspaceBot.handleEvent( new ArenaList( message ) );
        }
    }

    /**
     * Translates a password packet response, verifying that the connection has
     * indeed logged in correctly, or if not, sends an error and disconnects.
     * Also initializes itself with *energy and ?obscene, and sends a LoggedOn
     * event to the bot to let it know it's now connected (note that this is
     * different than entering an arena).
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handlePasswordPacketResponse( ByteArray message, boolean alreadyDecrypted ){;
         // Check for valid message
        if( message.size() < 36 ){
        	Tools.printLog( m_session.getBotName() + " has received a wrongfully sized password-packet response. (size:"+message.size()+")");
        	return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        PasswordPacketResponse ppResponse = new PasswordPacketResponse( message );
        if( m_verboseLogin == 0 ) {
            if( ppResponse.getResponseValue() != PasswordPacketResponse.response_Continue ) {
                Tools.printLog( m_session.getBotName() + ": " + ppResponse.getResponseMessage() );
            }
        } else {
            Tools.printLog( m_session.getBotName() + ": " + ppResponse.getResponseMessage() );
        }

        if(ppResponse.getRegistrationFormRequest() == true) {
        	String realname = m_session.getCoreData().getGeneralSettings().getString("Real Name");
        	String email = m_session.getCoreData().getGeneralSettings().getString("E-mail");
        	String state = m_session.getCoreData().getGeneralSettings().getString("State");
        	String city = m_session.getCoreData().getGeneralSettings().getString("City");
        	int age = m_session.getCoreData().getGeneralSettings().getInt("Age");

        	// Reset to default values if there is something wrong
        	realname = (realname == null || realname.length() == 0) ? "Maverick":realname;
        	email = (email == null || email.length() == 0) ? "bots@twcore.org":email;
        	state = (state == null || state.length() == 0) ? "The Netherlands":state;
        	city = (city == null || city.length() == 0) ? "Amsterdam":city;
        	age = (age <= 0 ) ? 22 : age;

        	// Send registration information
        	Tools.printLog( m_session.getBotName() + ": Sending registration form");
        	m_packetGenerator.sendRegistrationForm(realname, email, state, city, age);
        }

        if (ppResponse.getSSChecksum() == -1 && ppResponse.getSSChecksumSeed() == -1 && m_verboseLogin == 1) {
    		Tools.printLog( m_session.getBotName() + ": Subspace.exe checksum and (random) server checksum were sent (VIP access)");
    	} else if(ppResponse.getSSChecksum() == 0) {
    		Tools.printLog( m_session.getBotName() + ": Problem found with server: Server doesn't have a copy of subspace.exe so it sent me a zero checksum.");
    	}

        if(ppResponse.getResponseValue() == PasswordPacketResponse.response_NewUser) {
        		Tools.printLog( m_session.getBotName() + ": Creating account");
        		m_packetGenerator.sendPasswordPacket(true, m_playerName, m_playerPassword);
        		return;
        } else if( ppResponse.isFatal() ) {
        	m_session.disconnect( "unsuccessful login" );
        }

        /***** ASSS Compatible Login Sequence Fix (D1st0rt) *****/
        //Login ok, continue (Moved here from handle of packet 0x31)
        else if( ppResponse.getResponseValue() == PasswordPacketResponse.response_Continue ){
            m_session.loggedOn();
            m_subspaceBot.handleEvent( new LoggedOn( null ) );
            m_packetGenerator.sendChatPacket( (byte)2, (byte)0, (short)0,"*energy" );
            m_packetGenerator.sendChatPacket( (byte)2, (byte)0, (short)0,"?obscene" );
        }
    }

    /**
     * Handles a file request from the server (for example, when *putfile is
     * used).  If it's able to find the file, the method generates a request
     * to send the file to the server.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleFileRequest( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() < 273 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        File            file;
        String          fileName;
        int             fileLength;
        ByteArray       fileContents;

        fileName = message.readNullTerminatedString( 1 );

        try {
            file = m_session.getBotAction().getDataFile( fileName );
            if( file.exists() == true ){
                fileLength = (int)file.length();
                fileContents = new ByteArray( fileLength );
                fileContents.addFileContents( file );
            } else {
                fileContents = new ByteArray( 0 );
            }

            m_packetGenerator.sendFile( fileName, fileContents );
        } catch( Exception e ){
            Tools.printLog( "Unable to send file: " + fileName );
        }
    }

    /**
     * Translates packet into the appropriate event.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleArenaJoined( ByteArray message, boolean alreadyDecrypted ){
    	//send position packet to begin recieving position packets, temporary fix
    	m_packetGenerator.sendPositionPacket((byte)0, (short)0, (short)8192, (byte)0, (short)8192, (short)0, (short)0, (short)0, (short)0);
        if( m_requester.check( EventRequester.ARENA_JOINED ) ){
            m_subspaceBot.handleEvent(new ArenaJoined(null));
        }
    }

    /**
     * STUB METHOD: For translating a settings packet.  Unfortunately,
     * settings can take up a fairly large amount of space, and it may be
     * impractical for a bot to hold them in memory.  d1st0rt has a settings
     * generator workaround for this (not yet complete) available at his
     * site: d1st0rt.sscentral.com.
     * @param message Packet data
     * @param alreadyDecrypted True if packet has already been decrypted
     */
    void handleArenaSettings( ByteArray message, boolean alreadyDecrypted ){
        if(message.size() < 1428) return;
        //TODO: add this at some point, 2dragons said he had a settings class
    }
}