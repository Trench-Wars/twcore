package twcore.core;

/*04 - Player leaving
Field    Length    Description
0        1        Type byte
1        2        Player ident
 */
public class PlayerLeft extends SubspaceEvent {

    short             m_playerID;

    public PlayerLeft( ByteArray array ){

        m_playerID = array.readLittleEndianShort( 1 );
    }

    public short getPlayerID(){
        return m_playerID;
    }
}
