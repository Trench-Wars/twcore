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
    int     m_score;
    int     m_flags;
    int     m_killerID;
    int     m_killeeID;
    int     m_deathGreen;

    public PlayerDeath( ByteArray array ){
        m_deathGreen = (int)array.readByte( 1 );
        m_killerID = (int)array.readLittleEndianShort( 2 );
        m_killeeID = (int)array.readLittleEndianShort( 4 );
        m_score = (int)array.readLittleEndianShort( 6 );
        m_flags = (int)array.readLittleEndianShort( 8 );
    }

    public int getKillerID(){
        return m_killerID;
    }

    public int getKilleeID(){
        return m_killeeID;
    }

    public int getScore(){
        return m_score;
    }

    public int getFlagCount(){
        return m_flags;
    }
}
