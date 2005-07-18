package twcore.core;

/**
 * (S2C 0x0A) Event fired when response to password packet is received. <code><pre>
 * +-----------------------------------------------------+
 * | Offset  Length  Description                         |
 * +-----------------------------------------------------+
 * | 0       1       Type Byte                           |
 * | 1       1       Login Response                      |
 * | 2       4       Server Version                      |
 * | 6       4       ? Unknown                           |
 * | 10      4       Subspace.exe Checksum               |
 * | 14      4       ? Unknown                           |
 * | 18      1       ? Unknown                           |
 * | 19      1       Registration Form Request (Boolean) |
 * | 20      4       ? Unknown                           |
 * | 24      4       News.txt Checksum                   |
 * | 28      8       ? Unknown                           |
 * +-----------------------------------------------------+</code></pre>
 *
 * Server Version returns Major and Minor as single number (1.34.12a returns 134).
 * Checksums are to be compared with local files to determine if an update is required.
 */
public class PasswordPacketResponse {

    //Variable Declarations
    private int m_response;

    /**
     * Creates a new instance of PasswordPacketResponse, this is called by
     * GamePacketInterpreter when it receives the packet
     * @param array the ByteArray containing the packet data
     */
    public PasswordPacketResponse( ByteArray array ) {

        m_response = (int)array.readByte( 1 );
    }

    /**
     * Gets the number value of the response sent by the server
     * @return an integer representing the response
     */
    public int getResponseValue() { return m_response; }

    /**
     * Gets the message associated with the response the server sent
     * @return a String containing the meaning of the message from the server
     */
    public String getResponseMessage() {

        switch( m_response ) {
            case 0:   return "Login successful";
            case 1:   return "Unregistered player"; //Registration required, not sent
            case 2:   return "Bad password";
            case 3:   return "Arena is full";
            case 4:   return "Locked out of zone";
            case 5:   return "Permission only arena";
            case 6:   return "Permission to spectate only";
            case 7:   return "Too many points to play here";
            case 8:   return "Connection is too slow";
            case 9:   return "Server is full";
            case 10:  return "Invalid name";
            case 11:  return "Offensive name";
            case 12:  return "No active biller"; //no scores will be recorded
            case 13:  return "Server busy, try later";
            case 14:  return "Restricted zone"; //Usually based on insufficient usage
            case 15:  return "Demo version detected";
            case 16:  return "Too many demo users";
            case 17:  return "Demo versions not allowed";
            case 255: return "Restricted zone, mod access required";
            default:  return "Unknown response";
        }
    }

    /**
     * Gets whether this response means the client was disconnected from the server
     * @return whether the server disconnected the client or not
     */
    public boolean isFatal() {

        if( m_response == 0 || m_response == 6
            || m_response == 12 || m_response == 14 ) return false;
        else return true;
    }
}