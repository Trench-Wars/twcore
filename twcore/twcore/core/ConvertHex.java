package twcore.core;

public class ConvertHex {
    
    public static String byteToHex( byte b ){
        if( (b & 0xf0) == 0 ){
            return 0 + Integer.toHexString( b & 0xFF );
        } else {
            return Integer.toHexString( b & 0xFF );
        }
    }
    
    public static String byteToHex( int theByte ){
        if( (theByte & 0x00F0) == 0 ){
            return 0 + Integer.toHexString( theByte & 0xFF );
        } else {
            return Integer.toHexString( theByte & 0xFF );
        }
    }
}