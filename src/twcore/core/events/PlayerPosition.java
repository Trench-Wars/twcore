package twcore.core.events;

import twcore.core.util.ByteArray;

public class PlayerPosition extends SubspaceEvent {
    private byte m_type;
    private int m_size;
    private byte m_rotation;
    private short m_timeStamp;
    private short m_xLocation;
    private byte m_ping;
    private short m_bounty;
    private short m_playerID;
    private short m_yVelocity;
    private short m_yLocation;
    private short m_xVelocity;

    private byte m_togglables;

    // Used in packet containing energy information only
    private short m_energy = 0;
    private short m_s2clag  = 0;
    private short m_timer = 0;
    // DISABLED: Checksum
    // private int m_checksum;

    // RE-ENABLED: Weapon info (used by bfallout/spaceball/etc modules)
    // Used in weapons (long) packet only
    private short m_weaponInfo = 0;

    //Togglables
    private boolean m_stealthOn;
    private boolean m_cloakOn;
    private boolean m_xradarOn;
    private boolean m_antiOn;
    private boolean m_warpedIn;
    private boolean m_inSafe;
    private boolean m_ufoOn;
    // DISABLED: Unknown toggleable
    // private boolean m_unknown;

    // This information may or may not be present
    private boolean m_shields = false;
    private boolean m_super = false;
    private int m_burst = -1;
    private int m_repel = -1;
    private int m_thor = -1;
    private int m_brick = -1;
    private int m_decoy = -1;
    private int m_rocket = -1;
    private int m_portal = -1;

    public PlayerPosition( ByteArray array ) {
        m_type = array.readByte( 0 );
        m_size = array.size();

        if( m_type == 0x28 ) {
            m_rotation = array.readByte(1);
            m_timeStamp = array.readLittleEndianShort(2);
            m_xLocation = array.readLittleEndianShort(4);
            m_ping = array.readByte(6);

            // Bounty is sent as a byte, so >255 bty will lead to errors from short pos packet.
            m_bounty = array.readByte(7);

            // YES, we are only reading the lower byte for the short ver of the position packet.
            // YES, this means player position data may update sporadically because
            // Player checks vs. ID before updating (due to this problem).  Players with the
            // lower 255 ids won't have issues, but those with higher ids will.  -qan
            m_playerID = (short)(array.readByte(8) & 0xff);
            m_togglables = array.readByte( 9 );
            parseTogglables( m_togglables );
            m_yVelocity = array.readLittleEndianShort(10);
            m_yLocation = array.readLittleEndianShort(12);
            m_xVelocity = array.readLittleEndianShort(14);

            if( m_size > 16 ) {
                m_energy = array.readLittleEndianShort(16);
            } else {
                return;
            }

            //parse beginning of packet until 18
            if( m_size == 26 ) {
                m_s2clag = array.readLittleEndianShort(18);
                m_timer = array.readLittleEndianShort(20);
                parseItemCount( array.readLittleEndianInt( 22 ));
            }
        } else if( m_type == 0x05 ) {
            m_rotation = array.readByte(1);
            m_timeStamp = array.readLittleEndianShort(2);
            m_xLocation = array.readLittleEndianShort(4);
            m_yVelocity = array.readLittleEndianShort(6);
            m_playerID = array.readLittleEndianShort(8);
            m_xVelocity = array.readLittleEndianShort(10);

            // Checksum currently unused.  Uncomment and add to Player to use
            // m_checksum = array.readByte(12);

            m_togglables = array.readByte( 13 );
            parseTogglables( m_togglables );
            m_ping = array.readByte(14);
            m_yLocation = array.readLittleEndianShort(15);
            m_bounty = array.readLittleEndianShort(17);

            // Weapon info.  Also added to Player
            m_weaponInfo = array.readLittleEndianShort(19);

            if( m_size > 21 ) {
                //parse beginning until size 23
                m_energy = array.readLittleEndianShort(21);
            } else {
                return;
            }

            if( m_size == 31 ) {
                m_s2clag = array.readLittleEndianShort(23);
                m_timer = array.readLittleEndianShort(25);
                parseItemCount( array.readLittleEndianInt( 27 ));
            }
        }

    }

    public void parseTogglables( byte togglables ) {
        m_stealthOn = ( togglables & 0x01 ) == 1;
        m_cloakOn = ( togglables & 0x02 ) >> 1 == 1;
        m_xradarOn = ( togglables & 0x04 ) >> 2 == 1;
        m_antiOn = ( togglables & 0x08 ) >> 3 == 1 ;
        m_warpedIn = ( togglables & 0x10 ) >> 4 == 1;
        m_inSafe = ( togglables & 0x20 ) >> 5 == 1;
        m_ufoOn = ( togglables & 0x40 ) >> 6 == 1;
        // Unknown toggleable currently unused.  Uncomment and add to Player to use
        // m_unknown = ( togglables & 0x80 ) >> 7 == 1;
    }

    public void parseItemCount( int items ) {
        m_shields = ( items & 0x00000001 ) == 1;
        m_super =   ( items & 0x00000002 ) >> 1 == 1;
        m_burst =   ( items & 0x0000003c ) >> 2;
        m_repel =   ( items & 0x000003c0 ) >> 6;
        m_thor =    ( items & 0x00003c00 ) >> 10;
        m_brick =   ( items & 0x0003c000 ) >> 14;
        m_decoy =   ( items & 0x003c0000 ) >> 18;
        m_rocket =  ( items & 0x03c00000 ) >> 22;
        m_portal =  ( items & 0x3c000000 ) >> 26;
    }

    public byte getRotation() {
        return m_rotation;
    }

    public short getXLocation() {
        return m_xLocation;
    }

    public byte getTogglables() {
        return m_togglables;
    }

    public byte getPing() {
        return m_ping;
    }

    public short getBounty() {
        return m_bounty;
    }

    public short getPlayerID() {
        return m_playerID;
    }

    public short getYVelocity() {
        return m_yVelocity;
    }

    public short getYLocation() {
        return m_yLocation;
    }

    public short getXVelocity() {
        return m_xVelocity;
    }

    public boolean containsEnergy() {
        if( m_type == 0x05 && m_size == 21 ) {
            return false;
        } else if( m_type == 0x28 && m_size == 16 ) {
            return false;
        } else {
            return true;
        }
    }

    public boolean containsWeaponsInfo() {
        return m_type == 0x05;
    }

    public boolean containsItemCount() {
        return( m_type == 0x05 && m_size == 31 )
              || ( m_type == 0x28 && m_size == 0x26 );
    }

    public short getEnergy() {
        return m_energy;
    }

    public short getS2CLag() {
        return m_s2clag;
    }

    public short getTimer() {
        return m_timer;
    }

    public short getTimeStamp() {
        return m_timeStamp;
    }

    public boolean hasShields() {
        return m_shields;
    }

    public boolean hasSuper() {
        return m_super;
    }

    public int getBurstCount() {
        return m_burst;
    }

    public int getRepelCount() {
        return m_repel;
    }

    public int getThorCount() {
        return m_thor;
    }

    /**
        @deprecated Who calls them walls?
    */
    @Deprecated
    public int getWallCount() {
        return getBrickCount();
    }

    public int getBrickCount() {
        return m_brick;
    }

    public int getDecoyCount() {
        return m_decoy;
    }

    public int getRocketCount() {
        return m_rocket;
    }

    public int getPortalCount() {
        return m_portal;
    }

    public boolean isStealthed() {
        return m_stealthOn;
    }

    public boolean isCloaked() {
        return m_cloakOn;
    }

    public boolean hasXRadarOn() {
        return m_xradarOn;
    }

    public boolean hasAntiwarpOn() {
        return m_antiOn;
    }

    public boolean isWarpingIn() {
        return m_warpedIn;
    }

    public boolean isInSafe() {
        return m_inSafe;
    }

    public boolean isUFO() {
        return m_ufoOn;
    }

    /**
        DISABLED.
        @return 0
    */
    public int getChecksum() {
        return 0;
        // return m_checksum;
    }

    public short getWeaponInfo() {
        return m_weaponInfo;
    }

    /**
        DISABLED.
        @return false
    */
    public boolean Unknown() {
        return false;
        //return m_unknown;
    }

}