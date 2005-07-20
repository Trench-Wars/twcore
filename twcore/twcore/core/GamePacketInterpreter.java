package twcore.core;

/*
 * GamePacketInterpreter.java
 *
 * Created on December 8, 2001, 2:39 AM
 */
import java.io.*;

public class GamePacketInterpreter {

    private Session                 m_session;
    private ByteArray               m_chunkArray;
    private String                  m_playerName;
    private int                     m_expectedAck;
    private SubspaceBot             m_subspaceBot;
    private SSEncryption            m_ssEncryption;
    private Arena                   m_arenaTracker;
    private String                  m_playerPassword;
    private GamePacketGenerator     m_packetGenerator;
    private int                     m_massiveChunkCount;
    private ByteArray               m_massiveChunkArray;
    private ReliablePacketHandler   m_reliablePacketHandler;
    private EventRequester          m_requester;
    private MessageLimiter          m_limiter = null;

    public GamePacketInterpreter( Session session,
    GamePacketGenerator packetGenerator, SSEncryption ssEncryption,
    Arena arenaTracker, String name, String password ){
        m_requester = session.getEventRequester();
        m_playerName = name;
        m_playerPassword = password;
        m_expectedAck = 0;
        m_session = session;
        m_chunkArray = null;
        m_subspaceBot = null;
        m_massiveChunkCount = 0;
        m_arenaTracker = arenaTracker;
        m_ssEncryption = ssEncryption;
        m_packetGenerator = packetGenerator;
    }

    public void setMessageLimiter( int msgsPerMin, BotAction botAction ){
        if( msgsPerMin >= 1 ){
            m_limiter = new MessageLimiter( botAction, m_subspaceBot, msgsPerMin );
        } else {
            m_limiter = null;
        }
    }

    public void setSubspaceBot( SubspaceBot subspaceBot ){
        m_subspaceBot = subspaceBot;
    }

    public void setReliablePacketHandler( ReliablePacketHandler reliablePacketHandler ){
        m_reliablePacketHandler = reliablePacketHandler;
    }

    public void translateGamePacket( ByteArray array, boolean alreadyDecrypted ){
        int index = array.readByte( 0 ) & 0xff;

        if( index == 0 ){
            translateSpecialPacket( array, alreadyDecrypted );
        } else {
            translateNormalPacket( array, alreadyDecrypted );
        }
    }

    void translateNormalPacket( ByteArray array, boolean alreadyDecrypted ){
        int index = array.readByte( 0 ) & 0xff;
        //System.out.println( Integer.toHexString( index ) + " " );
        switch( index ){
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
            case 0x16:
                handleFlagDropped( array, alreadyDecrypted );
                break;
            case 0x2E:
                handleBallPosition( array, alreadyDecrypted );
                break;
            case 0x1a:
                handleScoreReset( array, alreadyDecrypted );
                break;
            case 0x14:
                handleFlagVictory( array, alreadyDecrypted );
                break;
            case 0x19:
                handleFileRequest( array, alreadyDecrypted );
                break;
            case 0x1d:
                handleShipFreqChange( array, alreadyDecrypted );
                break;
            case 0x1F:
                handlePlayerBanner( array, alreadyDecrypted );
                break;
            case 0x22:
                handleTurfFlagUpdate( array, alreadyDecrypted );
                break;
            case 0x23:
                handleFlagReward( array, alreadyDecrypted );
                break;
            case 0x28:
                handlePlayerPosition( array, alreadyDecrypted );
                break;
            case 0x2F:
                handleArenaList( array, alreadyDecrypted );
                break;
            case 0x38:
                handleWatchDamage( array, alreadyDecrypted );
                break;
        }
    }

    void translateSpecialPacket( ByteArray array, boolean alreadyDecrypted ){
        int index = array.readByte( 1 ) & 0xff;

        if( alreadyDecrypted == false && index != 2 ){
            m_ssEncryption.decrypt( array, array.size() - 2, 2 );
        }

        switch( index ){
            case 0x02:
                m_ssEncryption.setServerKey( array.readLittleEndianInt( 2 ) );
                m_packetGenerator.sendPasswordPacket( m_playerName, m_playerPassword );
                Tools.printLog( m_session.getBotName() + " is logging in." );
                break;
            case 0x03:
                m_reliablePacketHandler.handleReliableMessage( array );
                break;
            case 0x04:
                m_reliablePacketHandler.handleAckMessage( array );
                break;
            case 0x05:
                m_packetGenerator.sendSyncResponse( array.readLittleEndianInt( 2 ) );
                break;
            case 0x06:
                m_packetGenerator.setServerTimeDifference( array.readLittleEndianInt( 2 ) - array.readLittleEndianInt( 6 ) );
                break;
            case 0x07:
                m_session.disconnect();
                break;
            case 0x08:
                handleChunk( array );
                break;
            case 0x09:
                handleChunkTail( array );
                break;
            case 0x0A:
                handleMassiveChunkMessage( array );
                break;
            case 0x0E:
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

    public void handleChunkTail( ByteArray message ){
        int         oldSize;

        oldSize = m_chunkArray.size();
        m_chunkArray.growArray( oldSize + message.size() - 2 );
        m_chunkArray.addPartialByteArray( message, 2, message.size() - 2 );
        translateGamePacket( m_chunkArray, true );
        m_chunkArray = null;
    }

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

    void handleChatMessage( ByteArray message, boolean alreadyDecrypted ){
        // Check for valid message
        if( message.size() <= 6 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        if( m_requester.check( EventRequester.MESSAGE )){
            if( m_limiter == null ){
                m_subspaceBot.handleEvent( new Message( message ));
            } else {
                m_limiter.handleEvent( new Message( message ));
            }
        }
    }

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

    void handleTurret( ByteArray message, boolean alreadyDecrypted ){
        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }


        if( m_requester.check( EventRequester.TURRET_EVENT ) ) {
            m_subspaceBot.handleEvent( new TurretEvent( message ) );
        }
    }

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

    void handleFlagReward( ByteArray message, boolean alreadyDecrypted ){
        ByteArray        subMessage;

        if( message.size() < 5 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        subMessage = new ByteArray( 5 );

        // More than one player entered message can be in each packet
        for( int i=1; i<message.size(); i+=4 ){
            subMessage.addPartialByteArray( message, 1, i, 4 );
            FlagReward flagReward = new FlagReward( subMessage );

            m_arenaTracker.processEvent( flagReward );

            if( m_requester.check( EventRequester.FLAG_REWARD ) ){
                m_subspaceBot.handleEvent( flagReward );
            }
        }
    }

    void handleTurfFlagUpdate( ByteArray message, boolean alreadyDecrypted ) {

        ByteArray subMessage;

        if( message.size() < 3 ) {
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        subMessage = new ByteArray( 3 );
        for( int i = 1, j = 0; i < message.size(); i+=2, j++) {
            subMessage.addPartialByteArray( message, 1, i, 2 );
            TurfFlagUpdate turfFlagUpdate = new TurfFlagUpdate( subMessage, j );
            m_arenaTracker.processEvent( turfFlagUpdate );

            if( m_requester.check( EventRequester.TURF_FLAG_UPDATE ) ) {
                m_subspaceBot.handleEvent( turfFlagUpdate );
            }
        }

    }

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

    void handlePasswordPacketResponse( ByteArray message, boolean alreadyDecrypted ){;
         // Check for valid message
        if( message.size() < 36 ){
            return;
        }

        if( alreadyDecrypted == false ){
            m_ssEncryption.decrypt( message, message.size()-1, 1 );
        }

        PasswordPacketResponse ppResponse = new PasswordPacketResponse( message );

        if( ppResponse.getResponseValue() > 0 )
            Tools.printLog( m_session.getBotName() + " log in response: " + ppResponse.getResponseMessage() );

        if( ppResponse.isFatal() ) m_session.disconnect();

        /***** ASSS Compatible Login Sequence Fix (D1st0rt) *****/
        //Login ok, continue (Moved here from handle of packet 0x31)
        else if( ppResponse.getResponseValue() == 0 ){
            m_session.loggedOn();
            m_subspaceBot.handleEvent( new LoggedOn( null ) );
            m_packetGenerator.sendChatPacket( (byte)2, (byte)0, (short)0,"*energy" );
            m_packetGenerator.sendChatPacket( (byte)2, (byte)0, (short)0,"?obscene" );
        }
    }

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

    void handleArenaJoined( ByteArray message, boolean alreadyDecrypted ){
        if( m_requester.check( EventRequester.ARENA_JOINED ) ){
			//Joining new arena, so the old players aren't there (good eye qan :D)
			m_arenaTracker.clear();
            m_subspaceBot.handleEvent( new ArenaJoined( null ) );
        }
    }

    void handleArenaSettings( ByteArray message, boolean alreadyDecrypted ){
        if(message.size() < 1428) return;
        //TODO: add this at some point, 2dragons said he had a settings class
    }
}

