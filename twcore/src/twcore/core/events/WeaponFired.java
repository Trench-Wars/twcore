package twcore.core.events;

import twcore.core.util.ByteArray;

public class WeaponFired extends PlayerPosition {

    public static final int WEAPON_BULLET = 1;
    public static final int WEAPON_BULLET_BOUNCING = 2;
    public static final int WEAPON_BOMB = 3;
    public static final int WEAPON_EMP_BOMB = 4;
    public static final int WEAPON_REPEL = 5;

    public static final int WEAPON_DECOY = 6;
    public static final int WEAPON_BURST = 7;
    public static final int WEAPON_THOR = 8;

    public static final int WEAPON_MULTIFIRE = 9;
    public static final int WEAPON_SINGLEFIRE = 10;
    public static final int WEAPON_MINE = 11;
    public static final int WEAPON_NOTMINE = 12;
    
    // Used internally, not in the packet:
    public static final int WEAPON_SHRAP = 13;

    private int         m_weaponType;
    private int         m_weaponLevel;
    private int         m_weaponShrapLevel;
    private int         m_weaponShrapCount;
    private boolean     m_weaponAlternative;
    private boolean     m_weaponBouncyShrap;

    public WeaponFired( ByteArray array ){
        super( array );
        
        short wepinfo = getWeaponInfo();
        m_weaponType = wepinfo & 0x001f;
        m_weaponLevel = ((wepinfo & 0x0060) >> 5) + 1;
        m_weaponBouncyShrap = ((wepinfo & 0x0080) >> 7) == 1;
        m_weaponShrapLevel = ((wepinfo & 0x0300) >> 8) + 1;
        m_weaponShrapCount = (wepinfo & 0x7c00) >> 10;
        m_weaponAlternative = (wepinfo & 0x8000) >> 15 == 1;
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

    public boolean isMultifire() {
        return (isType(WEAPON_BULLET) && m_weaponAlternative);
    }
    
    public boolean isMine() {
        return (isType(WEAPON_BOMB) && m_weaponAlternative);
    }
}
