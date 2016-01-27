package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x38) Event called when a ship is damaged and has *watchdamage on.<code>
    +--------------------------------+
    | Offset  Length  Description    |
    +--------------------------------+
    |  0      1       Type Byte      |
    |  1      1       Victim ID      |
    |  2      5       ?              |
    |  7      1       Attacker ID    |
    |  8      1       ?              |
    |  9      2       Weapons info   |
    | 11      2       Old energy     |
    | 13      2       Energy lost    |
    | 15      1       ?              |
    +--------------------------------+</code>

    To fire this event, send *watchdamage to players on which you wish to watch damage.
    Preferably this should be done when PlayerEntered is fired.  Remember that you must
    also *watchdamage players upon exit.

    TODO: A way to have *watchdamage automatically sent when players enter and leave,
    based on either requesting this event, or requesting this event and setting a flag.
*/
public class WatchDamage extends SubspaceEvent {

    public static int WEAPON_BULLET = 1;
    public static int WEAPON_BULLET_BOUNCING = 2;
    public static int WEAPON_BOMB = 3;
    public static int WEAPON_EMP_BOMB = 4;
    public static int WEAPON_REPEL = 5;

    public static int WEAPON_DECOY = 6;
    public static int WEAPON_BURST = 7;
    public static int WEAPON_THOR = 8;

    public static int WEAPON_MULTIFIRE = 9;
    public static int WEAPON_SINGLEFIRE = 10;
    public static int WEAPON_MINE = 11;
    public static int WEAPON_NOTMINE = 12;

    short               m_victim;
    short               m_attacker;
    short               m_oldEnergy;
    short               m_energyLost;

    private short       m_weaponInfo;
    private int         m_weaponType;
    private int         m_weaponLevel;
    private int         m_weaponShrapLevel;
    private int         m_weaponShrapCount;
    private boolean     m_weaponAlternative;
    private boolean     m_weaponBouncyShrap;

    public WatchDamage( ByteArray array ) {

        m_victim = array.readLittleEndianShort( 1 );
        m_attacker = array.readLittleEndianShort( 7 );
        m_weaponInfo = array.readLittleEndianShort( 9 );
        m_oldEnergy = array.readLittleEndianShort( 11 );
        m_energyLost = array.readLittleEndianShort( 13 );

        if( m_energyLost < 0 ) {
            m_energyLost += 65535;
        }

        if( m_oldEnergy < 0 ) {
            m_oldEnergy += 65535;
        }

        m_weaponType = m_weaponInfo & 0x001f;
        m_weaponLevel = ((m_weaponInfo & 0x0060) >> 5) + 1;
        m_weaponBouncyShrap = ((m_weaponInfo & 0x0080) >> 7) == 1;
        m_weaponShrapLevel = ((m_weaponInfo & 0x0300) >> 8) + 1;
        m_weaponShrapCount = (m_weaponInfo & 0x7c00) >> 10;
        m_weaponAlternative = (m_weaponInfo & 0x8000) >> 15 == 1;
    }

    /** Gets the PlayerID of the Victim
        @return Victim PlayerID
    */
    public short getVictim() {
        return m_victim;
    }

    /** Gets the PlayerID of the Attacker
        @return Attacker PlayerID
    */
    public short getAttacker() {
        return m_attacker;
    }

    /** Gets the energy the player had before.
        @return Old Energy Level
    */
    public short getOldEnergy() {
        return m_oldEnergy;
    }

    /** Gets the energy the player lost.
        @return Lost energy
    */
    public short getEnergyLost() {
        return m_energyLost;
    }

    public int getWeaponType() {
        return m_weaponType;
    }

    public int getWeaponLevel() {
        return m_weaponLevel;
    }

    public boolean isWeaponBouncyShrap() {
        return m_weaponBouncyShrap;
    }

    public int getWeaponShrapLevel() {
        return m_weaponShrapLevel;
    }

    public int getWeaponShrapCount() {
        return m_weaponShrapCount;
    }

    public boolean isType( int type ) {
        if( m_weaponType == type ) {
            return true;
        } else if( m_weaponType == WEAPON_EMP_BOMB && type == WEAPON_BOMB ) {
            return true;
        } else if( m_weaponType == WEAPON_BULLET_BOUNCING && type == WEAPON_BULLET ) {
            return true;
        }

        return false;
    }

    public boolean isMultifire() {
        if( m_weaponType != WEAPON_BULLET && m_weaponType != WEAPON_BULLET_BOUNCING ) {
            return false;
        } else {
            return m_weaponAlternative;
        }
    }

    public boolean isMine() {
        if( m_weaponType != WEAPON_BOMB && m_weaponType != WEAPON_EMP_BOMB ) {
            return false;
        } else {
            return m_weaponAlternative;
        }
    }
}
