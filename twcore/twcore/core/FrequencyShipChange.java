package twcore.core;

/*1D - Set team and ship
Field    Length    Description
0        1        Type byte
1        1        Ship type
2        2        Player ident
4        2        Team*/
public class FrequencyShipChange extends SubspaceEvent {

    int             m_playerID;
    int             m_shipType;
    int             m_freq;

    public FrequencyShipChange(ByteArray array){
        m_shipType = ((int)array.readByte( 1 ) + 1) % 9;
        m_playerID = (int)array.readLittleEndianShort( 2 );
        m_freq = (int)array.readLittleEndianShort( 4 );
    }

    public int getPlayerID(){
        return m_playerID;
    }    
    public int getShipType(){
        return m_shipType;
    }
    public int getFrequency(){
        return m_freq;
    }
}
