package twcore.core;

/*1D - Set team and ship
Field    Length    Description
0        1        Type byte
1        1        Ship type
2        2        Player ident
4        2        Team*/
public class FrequencyShipChange extends SubspaceEvent {

    short           m_playerID;
    byte            m_shipType;
    short           m_freq;

    public FrequencyShipChange(ByteArray array){
        m_shipType = (byte)((array.readByte( 1 ) + 1) % 9);
        m_playerID = (short)array.readLittleEndianShort( 2 );
        m_freq = (short)array.readLittleEndianShort( 4 );
    }

    public short getPlayerID(){
        return m_playerID;
    }    
    public byte getShipType(){
        return m_shipType;
    }
    public short getFrequency(){
        return m_freq;
    }
}
