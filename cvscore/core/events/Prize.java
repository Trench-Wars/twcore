package twcore.core.events;

import twcore.core.util.ByteArray;

public class Prize extends SubspaceEvent {
    
    static final int RECHARGE_PRIZE = 1;
    static final int ENERGY_PRIZE = 2;
    static final int ROTATION_PRIZE = 3;
    static final int STEALTH_PRIZE = 4;
    static final int CLOAK_PRIZE = 5;
    static final int XRADAR_PRIZE = 6;
    static final int WARP_PRIZE = 7;
    static final int GUNS_PRIZE = 8;
    static final int BOMBS_PRIZE = 9;
    static final int BOUNCING_BULLETS_PRIZE = 10;
    static final int THRUSTER_PRIZE = 11;
    static final int TOP_SPEED_PRIZE = 12;
    static final int FULL_CHARGE_PRIZE = 13;
    static final int ENGINE_SHUTDOWN_PRIZE = 14;
    static final int MULTIFIRE_PRIZE = 15;
    static final int PROXIMITY_PRIZE = 16;
    static final int SUPER_PRIZE = 17;
    static final int SHIELDS_PRIZE = 18;
    static final int SHRAPNEL_PRIZE = 19;
    static final int ANTIWARP_PRIZE = 20;
    static final int REPEL_PRIZE = 21;
    static final int BURST_PRIZE = 22;
    static final int DECOY_PRIZE = 23;
    static final int THOR_PRIZE = 24;
    static final int MULTIPRIZE_PRIZE = 25;
    static final int BRICK_PRIZE = 26;
    static final int ROCKET_PRIZE = 27;
    static final int PORTAL_PRIZE =28;
    
    int m_timeStamp;
    short m_xTiles;
    short m_yTiles;
    short m_prizeType;
    short m_playerID;
    
    public Prize(ByteArray array){
        m_timeStamp = (int)array.readLittleEndianInt( 1 );
        m_xTiles = (short)array.readLittleEndianShort( 5 );
        m_yTiles = (short)array.readLittleEndianShort( 7 );
        m_prizeType = (short)array.readLittleEndianShort( 9 );
        m_playerID = (short)array.readLittleEndianShort( 11 );
    }
    
    public int getTimeStamp(){
        return m_timeStamp;
    }
    public short getXTiles(){
        return m_xTiles;
    }
    public short getYTiles(){
        return m_yTiles;
    }
    public short getPrizeType(){
        return m_prizeType;
    }
    public short getPlayerID(){
        return m_playerID;
    }
}
