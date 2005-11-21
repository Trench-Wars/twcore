package twcore.core.events;

import twcore.core.util.ByteArray;

public class ScoreReset extends SubspaceEvent {
    /*
1A - Score reset
Field    Length    Description
0        1        Type byte
1        2        Player ident

*/
    short m_playerID;

    public ScoreReset(ByteArray array){
        m_playerID = array.readLittleEndianShort( 1 );
    }

    public short getPlayerID(){
        return m_playerID;
    }
}
