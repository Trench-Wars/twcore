package twcore.core;

/*
56 - Watch damage
Field    Length    Description
0        1        Type byte
1        1        Victim
2        5        ?
7        1        Attacker
8        1        ?
9         2          Weapons Info
11       2        Old energy
13       2        Energy lost
15       1        ?
*/

/** This class represents watchdamage packets.  Remember that you must
 * *watchdamage players that enter and also *watchdamage players upon exit.
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

    int                 m_victim;
    int                 m_attacker;
    int                 m_oldEnergy;
    int                 m_energyLost;

    private short       m_weaponInfo;
    private int         m_weaponType;
    private int         m_weaponLevel;
    private int         m_weaponShrapLevel;
    private int         m_weaponShrapCount;
    private boolean     m_weaponAlternative;
    private boolean     m_weaponBouncyShrap;

    public WatchDamage( ByteArray array ){

        m_victim = (int)array.readLittleEndianShort( 1 ); //(int)array.readByte( 1 );
        m_attacker = (int)array.readLittleEndianShort( 7 ); //(int)array.readByte( 7 );
        m_weaponInfo = array.readLittleEndianShort( 9 );
        m_oldEnergy = (int)array.readLittleEndianShort( 11 );
        m_energyLost = (int)array.readLittleEndianShort( 13 );

        if( m_energyLost < 0 ){
            m_energyLost += 65535;
        }

        if( m_oldEnergy < 0 ){
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
     * @return Victim PlayerID
     */
    public int getVictim(){
        return m_victim;
    }

    /** Gets the PlayerID of the Attacker
     * @return Attacker PlayerID
     */
    public int getAttacker(){
        return m_attacker;
    }

    /** Gets the energy the player had before.
     * @return Old Energy Level
     */
    public int getOldEnergy(){
        return m_oldEnergy;
    }

    /** Gets the energy the player lost.
     * @return Lost energy
     */
    public int getEnergyLost(){
        return m_energyLost;
    }

    public int getWeaponType(){
        return m_weaponType;
    }

    public int getWeaponLevel(){
        return m_weaponLevel;
    }

    public boolean isWeaponBouncyShrap(){
        return m_weaponBouncyShrap;
    }

    public int getWeaponShrapLevel(){
        return m_weaponShrapLevel;
    }

    public int getWeaponShrapCount(){
        return m_weaponShrapCount;
    }

    public boolean isType( int type ){
        if( m_weaponType == type ){
            return true;
        } else if( m_weaponType == WEAPON_EMP_BOMB && type == WEAPON_BOMB ){
            return true;
        } else if( m_weaponType == WEAPON_BULLET_BOUNCING && type == WEAPON_BULLET ){
            return true;
        }
        return false;
    }

    public boolean isMultifire(){
        if( m_weaponType != WEAPON_BULLET && m_weaponType != WEAPON_BULLET_BOUNCING ){
            return false;
        } else {
            return m_weaponAlternative;
        }
    }

    public boolean isMine(){
        if( m_weaponType != WEAPON_BOMB && m_weaponType != WEAPON_EMP_BOMB ){
            return false;
        } else {
            return m_weaponAlternative;
        }
    }
}
