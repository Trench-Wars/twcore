package twcore.core;

/*OD - Set team
Field    Length    Description
0        1        Type byte
1        2        Player ident
3        2        Team
5        1        ?
*/
public class FrequencyChange extends SubspaceEvent {

    int             m_playerID;
    int             m_frequency;

    public FrequencyChange(ByteArray array){
        m_playerID = (int)array.readLittleEndianShort( 1 );
        m_frequency = (int)array.readLittleEndianShort( 3 );
    }

    public int getPlayerID(){
        return m_playerID;
    }

    public int getFrequency(){
        return m_frequency;
    }
}
