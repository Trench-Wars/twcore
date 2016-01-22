package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x0A) Event fired when response to password packet is received. <code>
    +-----------------------------------------------------+
    | Offset  Length  Description                         |
    +-----------------------------------------------------+
    | 0       1       Type Byte                           |
    | 1       1       Login Response                      |
    | 2       4       Server Version                      |
    | 6       4       &lt;unused&gt;                            |
    | 10      4       Subspace.exe Checksum               |
    | 14      4       &lt;unused&gt;                            |
    | 18      1       &lt;unused&gt;                            |
    | 19      1       Registration Form Request (Boolean) |
    | 20      4       SSEXE cksum with seed of zero (1)   |
    | 24      4       News.txt checksum (0 = no news file)|
    | 28      8       &lt;unused&gt;                            |
    | 32      4       &lt;unused&gt;                            |
    +-----------------------------------------------------+</code>

    (1) if this and EXE checksum are -1, bestows supervisor privs to the client

    Server Version returns Major and Minor as single number (1.34.12a returns 134).
    Checksums are to be compared with local files to determine if an update is required.
*/
public class PasswordPacketResponse {

    //Variable Declarations
    private int m_response;             // Login response
    private int m_serverVersion;        // Server Version
    private int m_ssChecksum;           // Subspace.exe Checksum
    private boolean m_regFormRequest;   // Registration Form Request
    private int m_ssChecksumSeed;       // Subspace.exe Checksum with seed of zero
    private int m_newsChecksum;         // News.txt checksum (0 = no news file)

    // Response messages
    public static final int response_Continue = 0;          // 0   - Move along.
    public static final int response_NewUser = 1;           // 1   - Unknown player, continue as new user?
    public static final int response_InvalidPassword = 2;   // 2   - Invalid password for specified user.  The name you have chosen is probably in use by another player, try picking a different name.
    public static final int response_FullArena = 3;         // 3   - This arena is currently full, try again later.
    public static final int response_LockedOut = 4;         // 4   - You have been locked out of SubSpace, for more information inquire on Web BBS.
    public static final int response_NoPermission = 5;      // 5   - You do not have permission to play in this arena, see Web Site for more information.
    public static final int response_SpectateOnly = 6;      // 6   - You only have permission to spectate in this arena.
    public static final int response_TooManyPoints = 7;     // 7   - You have too many points to play in this arena, please choose another arena.
    public static final int response_SlowConnection = 8;    // 8   - Your connection appears to be too slow to play in this arena.
    public static final int response_NoPermission2 = 9;     // 9   - You do not have permission to play in this arena, see Web Site for more information.
    public static final int response_NoNewConnections = 10; // 10  - The server is currently not accepting new connections.
    public static final int response_InvalidName = 11;      // 11  - Invalid user name entered, please pick a different name.
    public static final int response_ObsceneName = 12;      // 12  - Possibly offensive user name entered, please pick a different name.
    public static final int response_BillerDown = 13;       // 13  - NOTICE: Server difficulties; this zone is currently not keeping track of scores.  Your original score will be available later.  However, you are free to play in the zone until we resolve this problem.
    public static final int response_BusyProcessing = 14;   // 14  - The server is currently busy processing other login requests, please try again in a few moments.
    public static final int response_ExperiencedOnly = 15;  // 15  - This zone is restricted to experienced players only (ie. certain number of game-hours logged).
    public static final int response_UsingDemoVersion = 16; // 16  - You are currently using the demo version.  Your name and score will not be kept track of.
    public static final int response_TooManyDemos = 17;     // 17  - This arena is currently has(sic) the maximum Demo players allowed, try again later.
    public static final int response_ClosedToDemos = 18;    // 18  - This arena is closed to Demo players.
    public static final int response_UnknownResponse = 19;  // ... - Unknown response type, please go to Web site for more information and to obtain latest version of the program.
    public static final int response_NeedModerator = 255;   // 255 - Moderator access required for this zone (MGB addition)


    /**
        Creates a new instance of PasswordPacketResponse, this is called by
        GamePacketInterpreter when it receives the packet
        @param array the ByteArray containing the packet data
    */
    public PasswordPacketResponse( ByteArray array ) {
        m_response = (int)array.readByte( 1 );
        m_serverVersion = array.readLittleEndianInt(2);
        m_ssChecksum = array.readLittleEndianInt(10);
        m_regFormRequest = array.readByte( 19 ) == 1;
        m_ssChecksumSeed = array.readLittleEndianInt(20);
        m_newsChecksum = array.readLittleEndianInt(24);
    }

    /**
        Gets the number value of the response sent by the server
        @return an integer representing the response
    */
    public int getResponseValue() {
        return m_response;
    }

    /**
        Gets the message associated with the response the server sent
        @return a String containing the meaning of the message from the server
    */
    public String getResponseMessage() {

        switch( m_response ) {
        case response_Continue:
            return "Successful password packet response";

        case response_NewUser:
            return "Unregistered player"; //Registration required, not sent

        case response_InvalidPassword:
            return "Bad password";

        case response_FullArena:
            return "Arena is full";

        case response_LockedOut:
            return "Locked out of zone";

        case response_NoPermission:
            return "Permission only arena";

        case response_SpectateOnly:
            return "Permission to spectate only";

        case response_TooManyPoints:
            return "Too many points to play here";

        case response_SlowConnection:
            return "Connection is too slow";

        case response_NoPermission2:
            return "You do not have permission to play in this arena";

        case response_NoNewConnections:
            return "The server is currently not accepting new connections.";

        case response_InvalidName:
            return "Invalid name";

        case response_ObsceneName:
            return "Obscene name";

        case response_BillerDown:
            return "No active biller"; //no scores will be recorded

        case response_BusyProcessing:
            return "Server busy, try later";

        case response_ExperiencedOnly:
            return "Restricted zone"; //Usually based on insufficient usage

        case response_UsingDemoVersion:
            return "Demo version detected";

        case response_TooManyDemos:
            return "Too many demo users";

        case response_ClosedToDemos:
            return "Demo versions not allowed";

        case response_NeedModerator:
            return "Restricted zone, mod access required";

        default:
            return "Unknown response";
        }
    }

    /**
        Gets whether this response means the client was disconnected from the server
        @return whether the server disconnected the client or not
    */
    public boolean isFatal() {

        if(     m_response == response_Continue ||          // Login successful
                m_response == response_SpectateOnly ||      // Permission to spectate only
                m_response == response_BillerDown ||        // No active biller
                m_response == response_ExperiencedOnly )    // Restricted zone
            return false;
        else
            return true;
    }

    /**
        @return the news.txt checksum (0 = no news file)
    */
    public int getNewsChecksum() {
        return m_newsChecksum;
    }

    /**
        @return the Registration Form Request
    */
    public boolean getRegistrationFormRequest() {
        return m_regFormRequest;
    }

    /**
        @return the Server Version
    */
    public int getServerVersion() {
        return m_serverVersion;
    }

    /**
        @return the Subspace.exe Checksum
    */
    public int getSSChecksum() {
        return m_ssChecksum;
    }

    /**
        @return the Subspace.exe Checksum with seed of zero
    */
    public int getSSChecksumSeed() {
        return m_ssChecksumSeed;
    }


}
