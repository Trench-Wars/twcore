package twcore.core;

public class FlagDropped extends SubspaceEvent {
	
	private int m_playerID;
	
	public FlagDropped( ByteArray array ) {
		m_playerID = (int)array.readLittleEndianShort( 1 );
	}
	
	public int getPlayerID() {
		return m_playerID;
	}
}