package twcore.core;

/*OD - Set team
Field    Length    Description
0        1        Type byte
1        2        Player ident
3        2        Team
5        1        ?
*/
public class FrequencyChange extends SubspaceEvent {

    short             m_playerID;
    short             m_frequency;

    public FrequencyChange(ByteArray array){
        m_playerID = (short)array.readLittleEndianShort( 1 );
        m_frequency = (short)array.readLittleEndianShort( 3 );
    }

    public short getPlayerID(){
        return m_playerID;
    }

    public short getFrequency(){
        return m_frequency;
    }
}
