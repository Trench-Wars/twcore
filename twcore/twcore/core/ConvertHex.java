package twcore.core;

/**
 * General utility class for byte to hex conversions.  
 * All methods are referenced in a static context. 
 */
public class ConvertHex {
    
    /**
     * Given a byte, return a String containing the hexadecimal equivalent.
     * @param b byte to process
     * @return String containing the hexadecimal equivalent of the provided byte
     */
    public static String byteToHex( byte b ){
        if( (b & 0xf0) == 0 ){
            return 0 + Integer.toHexString( b & 0xFF );
        } else {
            return Integer.toHexString( b & 0xFF );
        }
    }
    
    /**
     * Given an integer representation of a byte, return a String containing the
     * hexadecimal equivalent.
     * @param theByte byte to process
     * @return String containing the hexadecimal equivalent of the provided byte
     */
    public static String byteToHex( int theByte ){
        if( (theByte & 0x00F0) == 0 ){
            return 0 + Integer.toHexString( theByte & 0xFF );
        } else {
            return Integer.toHexString( theByte & 0xFF );
        }
    }
}