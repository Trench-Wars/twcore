package twcore.core.events;

import twcore.core.util.ByteArray;

/**
 * (S2C 0x03) Event fired when a player (or the bot if non-empty) enters the arena. <code><pre>
 * +------------------------------+
 * | Offset  Length  Description  |
 * +--------------------------------+
 * | 0       1       Type Byte      |
 * | 1       1       Ship Type      |
 * | 2       1       Accepts Audio  |
 * | 3       20      Player Name    |
 * | 23      20      Squad Name     |
 * | 43      4       Kill Points    |
 * | 47      4       Flag Points    |
 * | 51      2       Player ID      |
 * | 53      2       Frequency      |
 * | 55      2       Wins           |
 * | 57      2       Losses         |
 * | 59      2       Attachee ID    |
 * | 61      2       Flags Held     |
 * | 63      1       Has KOTH Timer |
 * +--------------------------------+</code></pre>
 *
 * Upon entering an arena (or possibly some other instances) several
 * of these packets (including the Type Byte) are stacked on top
 * of each other and sent as one packet. Be sure to process this
 * packet in 64 byte chunks until there are no chunks left.
 */
public class PlayerEntered extends SubspaceEvent {

    short           m_team;
    short           m_wins;
    short           m_losses;
    boolean         m_hasKOTH;
    byte            m_shipType;
    String          m_squadName;
    String          m_playerName;
    int             m_flagPoints;
    int             m_killPoints;
    short           m_playerID;
    boolean         m_acceptsAudio;
    short           m_flagsCarried;
    short           m_identTurretee;

   /**
     * Creates a new instance of PlayerEntered, this is called by
     * GamePacketInterpreter when it receives the packet
     * @param array the ByteArray containing the packet data
     */
    public PlayerEntered( ByteArray array ){

        m_shipType = array.readByte( 1 );
        m_acceptsAudio = array.readByte( 2 ) == 1;
        m_playerName = array.readString( 3, 20 );
        m_squadName = array.readString( 23,20 );
        m_killPoints = array.readLittleEndianInt( 43 );
        m_flagPoints = array.readLittleEndianInt( 47 );
        m_playerID = array.readLittleEndianShort( 51 );
        m_team = array.readLittleEndianShort( 53 );
        m_wins = array.readLittleEndianShort( 55 );
        m_losses = array.readLittleEndianShort( 57 );
        m_identTurretee = array.readLittleEndianShort( 59 );
        m_flagsCarried = array.readLittleEndianShort( 61 );
        m_hasKOTH = array.readByte( 63 ) == 1;
    }

    /**
     * Gets the team of the entering player
     * @return player's team
     */
    public short getTeam(){

        return m_team;
    }

    /**
     * Gets how many kills the entering player has
     * @return player's wins
     */
    public short getWins(){

        return m_wins;
    }

    /**
     * Gets how many deaths the entering player has
     * @return player's losses
     */
    public short getLosses(){

        return m_losses;
    }

    /**
     * Gets whether the entering player is currently in the running for King of the Hill (KOTH)
     * @return player's KOTH status
     */
    public boolean getHasKOTH(){

        return m_hasKOTH;
    }

    /**
     * Gets entering player's ship type (warbird == 1)
     * @return player's ship number
     */
    public byte getShipType(){

        return (byte)((m_shipType + 1) % 9);
    }

    /**
     * Gets entering player's ship type raw form (warbird == 0)
     * @return player's ship number raw form
     */
    public byte getShipTypeRaw() {
    	return m_shipType;
    }

    /**
     * Gets the name of the entering player's squad
     * @return player's squad name
     */
    public String getSquadName(){

        return m_squadName;
    }

    /**
     * Gets the entering player's name
     * @return player's name
     */
    public String getPlayerName(){

        return m_playerName;
    }

    /**
     * Gets the number of points the entering player has from flags
     * @return player's flag points
     */
    public int getFlagPoints(){

        return m_flagPoints;
    }

    /**
     * Gets the number of points the entering player has from kills
     * @return player's kill points
     */
    public int getKillPoints(){

        return m_killPoints;
    }

    /**
     * Gets the ID of the entering player
     * @return player's ID
     */
    public short getPlayerID(){

        return m_playerID;
    }

    /**
     * Gets whether the entering player accepts audio (voice) messages
     * @return if player accepts audio
     */
    public boolean getAcceptsAudio(){

        return m_acceptsAudio;
    }

    /**
     * Gets the number of flags carried by the entering player
     * @return player's flag carry count
     */
    public short getFlagsCarried(){

        return m_flagsCarried;
    }

    /**
     * Gets the ID of the player the entering player is attached to
     * @return player's turret's player ID
     */
    public short getIdentTurretee(){

        return m_identTurretee;
    }
}
