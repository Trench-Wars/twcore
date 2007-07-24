package twcore.core.events;

import twcore.core.util.ByteArray;

/**
 * (S2C 0x0A) Event fired when response to password packet is received. <code><pre>
 * +-----------------------------------------------------+
 * | Offset  Length  Description                         |
 * +-----------------------------------------------------+
 * | 0       1       Type Byte                           |
 * | 1       1       Login Response                      |
 * | 2       4       Server Version                      |
 * | 6       4       <unused>                            |
 * | 10      4       Subspace.exe Checksum               |
 * | 14      4       <unused>                            |
 * | 18      1       <unused>                            |
 * | 19      1       Registration Form Request (Boolean) |
 * | 20      4       SSEXE cksum with seed of zero (1)   |
 * | 24      4       News.txt checksum (0 = no news file)|
 * | 28      8       <unused>                            |
 * | 32      4       <unused>                            |
 * +-----------------------------------------------------+</code></pre>
 *
 * (1) if this and EXE checksum are -1, bestows supervisor privs to the client
 *
 * Server Version returns Major and Minor as single number (1.34.12a returns 134).
 * Checksums are to be compared with local files to determine if an update is required.
 */
public class PasswordPacketResponse {

    //Variable Declarations
    private int m_response;				// Login response
    private int m_serverVersion;		// Server Version
    private int m_ssChecksum;			// Subspace.exe Checksum
    private boolean m_regFormRequest;	// Registration Form Request
    private int m_ssChecksumSeed;		// Subspace.exe Checksum with seed of zero
    private int m_newsChecksum;			// News.txt checksum (0 = no news file)

    /**
     * Creates a new instance of PasswordPacketResponse, this is called by
     * GamePacketInterpreter when it receives the packet
     * @param array the ByteArray containing the packet data
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

        if(		m_response == 0 || 		// Login successful
        		m_response == 6 || 		// Permission to spectate only
        		m_response == 12 || 	// No active biller
        		m_response == 14 ) 		// Restricted zone
        	return false;
        else 
        	return true;
    }

	/**
	 * @return the news.txt checksum (0 = no news file)
	 */
	public int getNewsChecksum() {
		return m_newsChecksum;
	}

	/**
	 * @return the Registration Form Request
	 */
	public boolean getRegistrationFormRequest() {
		return m_regFormRequest;
	}

	/**
	 * @return the Server Version
	 */
	public int getServerVersion() {
		return m_serverVersion;
	}

	/**
	 * @return the Subspace.exe Checksum
	 */
	public int getSSChecksum() {
		return m_ssChecksum;
	}

	/**
	 * @return the Subspace.exe Checksum with seed of zero
	 */
	public int getSSChecksumSeed() {
		return m_ssChecksumSeed;
	}
    
    
}