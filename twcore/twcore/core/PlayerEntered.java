package twcore.core;

public class PlayerEntered extends SubspaceEvent {

    short           m_team;
    short           m_wins;
    short           m_losses;
    byte            m_hasKOTH;
    byte            m_shipType;
    String          m_squadName;
    String          m_playerName;
    int             m_flagPoints;
    int             m_killPoints;
    short           m_playerID;
    byte            m_acceptsAudio;
    short           m_flagsCarried;
    short           m_identTurretee;

    public PlayerEntered( ByteArray array ){
        
        m_shipType = (byte)((array.readByte( 1 ) + 1) % 9);
        m_acceptsAudio = array.readByte( 2 );
        m_playerName = array.readString( 3, 20 );
        m_squadName = array.readString( 23,20 );
        m_flagPoints = array.readLittleEndianInt( 43 );
        m_killPoints = array.readLittleEndianInt( 47 );
        m_playerID = array.readLittleEndianShort( 51 );
        m_team = array.readLittleEndianShort( 53 );
        m_wins = array.readLittleEndianShort( 55 );
        m_losses = array.readLittleEndianShort( 57 );
        m_identTurretee = array.readLittleEndianShort( 59 );
        m_flagsCarried = array.readLittleEndianShort( 61 );
        m_hasKOTH = array.readByte( 63 );
    }

    public short getTeam(){

        return m_team;
    }

    public short getWins(){

        return m_wins;
    }

    public short getLosses(){
        
        return m_losses;
    }

    public byte getHasKOTH(){

        return m_hasKOTH;
    }

    public byte getShipType(){

        return m_shipType;
    }

    public String getSquadName(){
        
        return m_squadName;
    }

    public String getPlayerName(){

        return m_playerName;
    }

    public int getFlagPoints(){

        return m_flagPoints;
    }

    public int getKillPoints(){

        return m_killPoints;
    }

    public short getPlayerID(){

        return m_playerID;
    }

    public byte getAcceptsAudio(){

        return m_acceptsAudio;
    }

    public short getFlagsCarried(){

        return m_flagsCarried;
    }

    public short getIdentTurretee(){

        return m_identTurretee;
    }
}
