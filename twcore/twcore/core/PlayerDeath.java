//Updated 8/3/04 by D1st0rt
package twcore.core;

/*
06 - Player death
Field    Length    Description
0        1        Type byte
1        1        Death Green (afaik, not used anywhere) -D1st0rt
2        2        Killer ident
4        2        Killee ident
6        2        Score to be added
8        2        Number of flags transferred through kill -D1st0rt
 */

public class PlayerDeath extends SubspaceEvent {
    short     m_score;
    short     m_flags;
    short     m_killerID;
    short     m_killeeID;
    // byte     m_deathGreen;
    // Not recording it until it's actually used.

    public PlayerDeath( ByteArray array ){
        // m_deathGreen = (byte)array.readByte( 1 );
        m_killerID = (short)array.readLittleEndianShort( 2 );
        m_killeeID = (short)array.readLittleEndianShort( 4 );
        m_score = (short)array.readLittleEndianShort( 6 );
        m_flags = (short)array.readLittleEndianShort( 8 );
    }

    public short getKillerID(){
        return m_killerID;
    }

    public short getKilleeID(){
        return m_killeeID;
    }

    public short getScore(){
        return m_score;
    }

    public short getFlagCount(){
        return m_flags;
    }
}
