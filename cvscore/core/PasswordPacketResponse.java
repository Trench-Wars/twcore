package twcore.core;

/*
		Offset	Length	Description
		0		1		Type Byte 0x0A
		1		1		Login Response *1
		2		4		Server Version *2
		6		4		? Unknown
		10		4		Subspace.exe Checksum *3
		14		4		? Unknown
		18		1		? Unknown
		19		1		Registration Form Request (Boolean)
		20		4		? Unknown
		24		4		News.txt Checksum *4
		28		8		? Unknown
*/
public class PasswordPacketResponse {
	
	private int m_response;
	
	public PasswordPacketResponse( ByteArray array ) {
		
		m_response = (int)array.readByte( 1 );
	}
	
	public int getResponseValue() { return m_response; }
	
	public String getResponseMessage() {
		
		switch( m_response ) {
			case 0:	return "Login successful";
			case 1:	return "Unregistered player";
			case 2: return "Bad password";
			case 3: return "Arena is full";
			case 4: return "Locked out of zone";
			case 5: return "Permission only arena";
			case 6: return "Permission to spectate only";
			case 7: return "Too many points to play here";
			case 8: return "Connection is too slow";
			case 9: return "Server is full";
			case 10: return "Invalid name";
			case 11: return "Offensive name";
			case 12: return "No active biller";
			case 13: return "Server busy, try later";
			case 14: return "Restricted zone";
			case 15: return "Demo version detected";
			case 16: return "Too many demo users";
			case 17: return	"Demo versions not allowed";
			case 255: return "Restricted zone, mod access required";
			default: return "Unknown response";
		}
	}
	
	public boolean isFatal() {
		
		if( m_response == 0 || m_response == 6 
			|| m_response == 12 || m_response == 14 ) return false;
		else return true;
	}
}