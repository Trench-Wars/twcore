package twcore.core;

import java.util.BitSet;

/**
 * Representation of the bot as as a Subspace ship for in-game playing.
 * 
 * @author  harvey
 */
public class Ship extends Thread {

    public static final double VELOCITY_TIME = 10000.0;     // # ms to divide velocity by
                                                            //   to determine distance

    private int         m_movingUpdateTime  = 100;          // How often a moving ship's
                                                            //   position is updated
    private int         m_unmovingUpdateTime = 1000;        // How often an unmoving ship's
                                                            //   position is updated
    
    private short       x = 8192;           // X coord (0-16384)
    private short       y = 8192;           // Y coord (0-16384)
    private short       xVel = 0;           // X velocity (# pixels traveled in 10 secs)
    private short       yVel = 0;           // Y velocity (# pixels traveled in 10 secs)
    private short       bounty = 3;         // Bot's bounty
    private short       energy = 1500;      // Bot's current energy
    private byte        togglables = 0;     // Bit vector of bools for cloak, UFO, etc.
    private byte        direction = 0x0;    // Direction facing in Subspace degrees (0-39)

    private short       lastXV = 0;         // Previous x velocity
    private short       lastYV = 0;         // Previous y velocity
    private byte        lastD = 0x0;        // Previous direction faced in SS degrees (0-39)

    private short       shipType = 8;       // 0-7 in-game ship; 8 spectating

    private int m_mAge = (int)System.currentTimeMillis();   // Last time position updated
    private int m_pAge = (int)System.currentTimeMillis();   // Last time packet sent

    private GamePacketGenerator     m_gen;  // Packet generator
    
    private final short SPEC_SHIP = 8;

    /**
     * Create a new instance of the Ship class (a separate program Thread).
     * @param group Thread grouping to add this new thread to
     * @param gen For generating appropriate position packets
     */
    Ship( ThreadGroup group, GamePacketGenerator gen ){
        super( group, "Ship" );
        m_gen = gen;
    }

    /**
     * Regularly checks to make updates to position by way of sending a new
     * position packet.  If the ship is in spectator mode, it doesn't need to
     * be updated, and if it isn't moving, it doesn't need to be updated
     * quite as often as a moving ship.  Default regularity of updates is
     * 100ms and 1000ms for moving and unmoving ships, respectively.  Set update
     * times either hard in the code, or with setUpdateTime()
     */
    public void run(){
        try {
            while( !interrupted() ){
                if( shipType == SPEC_SHIP ) {
                    Thread.sleep( getUnmovingUpdateTime() );            
                    return;
                }
                updatePosition();
                if ( xVel == 0 && yVel == 0 ){
                    Thread.sleep( getUnmovingUpdateTime() );
                } else {
                    Thread.sleep( getMovingUpdateTime() );
                }
            }
        } catch( InterruptedException e ){
            return;
        }
    }

    /**
     * Updates the ship's position based on current location and velocity.  If
     * a position packet should be sent, sends one.
     */
    public void updatePosition()
    {
        if (xVel != 0 && yVel != 0)
        {
            x += (short)(xVel * getAge() / VELOCITY_TIME );
            y += (short)(yVel * getAge() / VELOCITY_TIME );
        }
        m_mAge = (int)System.currentTimeMillis();

        if (needsToBeSent())
        {
            sendPositionPacket();
            lastXV = xVel;
            lastYV = yVel;
            lastD = direction;
        }
    }

    /**
     * Returns whether or not a position packet needs to be sent.  If the velocity
     * has changed, the direction the ship is facing has changed, or the time since
     * the last packet update is greater than or equal to the update interval for
     * an unmoving ship, this method will return true. 
     * @return True if a position packet needs to be sent
     */
    public boolean needsToBeSent()
    {
        return xVel != lastXV || yVel != lastYV || lastD != direction || (int)System.currentTimeMillis() - m_pAge >= m_unmovingUpdateTime;
    }

    /**
     * Generates a position packet for the ship, without weapon data.
     */
    public void sendPositionPacket()
    {
        m_gen.sendPositionPacket( direction, (short)xVel, (short)y, (byte)togglables, (short)x, (short)yVel, (short)bounty, (short)energy, (short)0 );
        m_pAge = (int)System.currentTimeMillis();
    }

    /**
     * Generates a position packet for the ship, and fires the weapon specified.
     * Refer to the getWeaponNumber method for proper setup of a weapon.
     * @param weapon A 16-bit bitvector containing weapon information
     * @see #getWeaponNumber(byte, byte, boolean, boolean, boolean, byte, boolean)
     */
    public void fire( int weapon ){
        m_gen.sendPositionPacket( direction, (short)xVel, (short)y, (byte)togglables, (short)x, (short)yVel, (short)bounty, (short)energy, (short)weapon );
        m_pAge = (int)System.currentTimeMillis();
    }
    
    /**
     * Sets various ship-related fields (not all are movement-related), and then sends
     * a position packet.
     * @param direction Direction to face ship, in Subspace degrees (0-39)
     * @param x X location on the map (0-16384)
     * @param y Y location on the map (0-16384)
     * @param xVel Velocity along the x axis (# pixels across x every 10 seconds)
     * @param yVel Velocity along the y axis (# pixels across y every 10 seconds)
     * @param togglables Size 8 bitvector containing stealth, cloak, x, anti, safety, ufo data
     * @param energy Player's current energy
     * @param bounty Player's current bounty
     */
    public void move( int direction, int x, int y, int xVel, int yVel, int togglables, int energy, int bounty ){
        this.lastXV = this.xVel;
        this.lastYV = this.yVel;
        this.lastD = this.direction;
        this.x = (short)x;
        this.y = (short)y;
        this.xVel = (short)xVel;
        this.yVel = (short)yVel;
        this.direction = (byte)direction;
        this.togglables = (byte)togglables;
        this.energy = (short)energy;
        this.bounty = (short)bounty;
        if( shipType != SPEC_SHIP )
            sendPositionPacket();
    }

    /**
     * Sets some movement-related fields for the bot, and then sends a position packet.
     * @param x X location on the map (0-16384)
     * @param y Y location on the map (0-16384)
     * @param xVel Velocity along the x axis (# pixels across x every 10 seconds)
     * @param yVel Velocity along the y axis (# pixels across y every 10 seconds)
     */
    public void move( int x, int y, int xVel, int yVel ){
        this.x = (short)x;
        this.y = (short)y;
        this.lastXV = this.xVel;
        this.lastYV = this.yVel;
        this.xVel = (short)xVel;
        this.yVel = (short)yVel;
        if( shipType != SPEC_SHIP )
            sendPositionPacket();
    }

    /**
     * Essentially a low-level warp.  Sets x and y coords of the bot, sets the
     * x and y velocities to 0 (stopped), and then sends a position packet.
     * Note that because this stops the bot dead in its tracks, it will revert
     * position data updates to the time specified in m_unmovingUpdateTime.
     * When this occurs, it may result in the appearance of the bot 'shifting'
     * back and forth badly between a predicted position and a sent position.
     * @param x X location on the map (0-16384)
     * @param y Y location on the map (0-16384)
     */
    public void move( int x, int y ){
        this.x = (short)x;
        this.y = (short)y;
        this.lastXV = this.xVel;
        this.lastYV = this.yVel;
        this.xVel = 0;
        this.yVel = 0;
        if( shipType != SPEC_SHIP )
            sendPositionPacket();
    }
    
    /**
     * Similar to move(int, int) except that it also causes the ship to fire the
     * specified weapon.
     * @param x X location on the map (0-16384)
     * @param y Y location on the map (0-16384)
     * @param weapon 16-bit bitvector containing weapon information
     * @see #getWeaponNumber(byte, byte, boolean, boolean, boolean, byte, boolean)
     */
    public void moveAndFire(int x, int y, int weapon ){
    	this.x = (short)x;
        this.y = (short)y;
        this.xVel = 0;
        this.yVel = 0;
        if( shipType != SPEC_SHIP )
            fire( weapon );
    }

    /**
     * Sets velocity of x and y, and the direction the ship is facing, and then
     * sends a position packet.
     * @param xVel Velocity along the x axis (# pixels across x every 10 seconds)
     * @param yVel Velocity along the y axis (# pixels across y every 10 seconds)
     * @param direction Direction to face ship in Subspace degrees (0-39)
     */
    public void setVelocitiesAndDir(int xVel, int yVel, int direction)
    {
        this.lastXV = this.xVel;
        this.lastYV = this.yVel;
        this.lastD = this.direction; 
        this.xVel = (short)xVel;
        this.yVel = (short)yVel;
        this.direction = (byte)direction;
        if( shipType != SPEC_SHIP )
            sendPositionPacket();
    }

    /**
     * Sets direction the ship is facing, in Subspace degrees.
     * SS directions:
     *     0 - Left
     *    10 - Up
     *    20 - Right
     *    30 - Down
     * @param rotation Direction to face ship in Subspace degrees (0-39)
     */
    public void setRotation( int rotation ){
        lastD = (byte)rotation;
        direction = (byte)rotation;
        if( shipType != SPEC_SHIP )
            sendPositionPacket();
    }
    
    /**
     * Sets direction the ship is facing, in radians.
     * @param rads Radian direction to face ship
     */
    public void rotateRadians( double rads ){
        rotateDegrees( Math.round( (int)Math.toDegrees( rads )) );
    }

    /**
     * Sets direction the ship is facing, in degrees (1-360)
     * @param degrees Direction the ship is facing, in degrees
     */
    public void rotateDegrees( int degrees ){
        if( degrees == 0 ) 
            degrees = 360;
        int rotate = (Math.round( (float)degrees / (float)9 ) + 10) % 40;
        setRotation( rotate );
    }

    /**
     * Sets ship type from 0-8.  0 is warbird.  If set to 8, the bot will
     * become a spectator, and cease sending any kind of position packet.
     * (Yes, TWCore's spec ship is normally 0.  This is not the way the SS
     * protocol normally handles it, however.)
     * @param shipType Type of ship to set to (1-8 in-game, 0 spectator)
     */
    public void setShip( int shipType ){
        this.shipType = (short)shipType;
        m_gen.sendShipChangePacket( (short)shipType );
    }

    /**
     * Sets the freq of the bot.
     * @param freq Freq to set to
     */
    public void setFreq( int freq ){
        m_gen.sendFreqChangePacket( (short)freq );
    }

    /**
     * Sets the bot to attach to a specific player.
     * @param playerId ID to attach to
     */
    public void attach( int playerId ){
        m_gen.sendAttachRequestPacket( (short)playerId );
    }

    /**
     * Sets the bot to detach from whomever it is currently attached to.
     */
    public void unattach(){
        m_gen.sendAttachRequestPacket( (short)-1 );
    }
    
    /**
     * Sets the time between position packet updates for when the bot is moving.
     * @param updateTime Time in ms between position packets while in movement
     */
    public void setMovingUpdateTime( int updateTime ) {
        m_movingUpdateTime = updateTime;        
    }

    /**
     * Sets the time between position packet updates for when the bot is not moving.
     * @param updateTime Time in ms between position packets while stopped
     */
    public void setUnmovingUpdateTime( int updateTime ) {
        m_unmovingUpdateTime = updateTime;
    }


    /**
     * @return Current x coordinate
     */
    public short getX(){
        return x;
    }
    
    /**
     * @return Current y coordinate
     */
    public short getY(){
        return y;
    }
    
    /**
     * @return Ship type (0-7 in-game, 8 spec'd)
     */
    public short getShip() {
        return shipType;
    }
    
    /**
     * @return Direction ship is facing, in SS degrees (0-39)
     */
    public short getDirection() {
        return direction;
    }
    
    /**
     * Returns a weapon number as used by the SS protocol based on information provided.
     * @param weaponType Type of weapon (0-15)
     * @param weaponLevel Level of weapon (0-15)
     * @param bouncing True for a bouncing effect
     * @param isEMP True if weapon causes EMP damage
     * @param isBomb True if weapon is a bomb
     * @param shrap Amount of shrap (0-15)
     * @param alternate Unknown
     * @return An int bitvector representing the weapon specified
     */
    public int getWeaponNumber(byte weaponType, byte weaponLevel, boolean bouncing, boolean isEMP, boolean isBomb, byte shrap, boolean alternate)
    {
    	BitSet bits = new BitSet(16);
    	bits.set(11, false);
    	
    	if(weaponType >= 8) { bits.set(12, true); weaponType -= 8; }
    	if(weaponType >= 4) { bits.set(13, true); weaponType -= 4; }
    	if(weaponType >= 2) { bits.set(14, true); weaponType -= 2; }
    	if(weaponType >= 1) { bits.set(15, true); weaponType -= 1; }
    	
    	if(weaponLevel >= 2) { bits.set(9, true); weaponLevel -= 2; }
    	if(weaponLevel >= 1) { bits.set(10, true); weaponLevel -= 1; }
    	
    	if(bouncing) bits.set(8, true);
    	
    	if(isEMP) bits.set(7, true);
    	
    	if(isBomb) bits.set(6, true);
    	
    	if(shrap >= 16) { bits.set(1, true); shrap -= 16; }
    	if(shrap >= 8) { bits.set(2, true); shrap -= 8; }
    	if(shrap >= 4) { bits.set(3, true); shrap -= 4; }
    	if(shrap >= 2) { bits.set(4, true); shrap -= 2; }
    	if(shrap >= 1) { bits.set(5, true); shrap -= 1; }
    	
    	if(alternate) bits.set(0, true);
    	
    	int total = 0;
    	int factor = 1;
    	
    	for(int k = 15;k >= 0;k--) {
    		if(bits.get(k)) total += factor;
    		factor *= 2;
    	}
    	
    	return total;
    }

    /**
     * @return Time in ms since last position update (regardless of packet sent or not)
     */
    public int getAge() {
        return (int)System.currentTimeMillis() - m_mAge;
    }
    
    /**
     * @return Interval in ms between position packet sendings while ship is moving
     */
    public int getMovingUpdateTime() {
        return m_movingUpdateTime;
    }

    /**
     * @return Interval in ms between position packet sendings while ship is moving
     */
    public int getUnmovingUpdateTime() {
        return m_unmovingUpdateTime;
    }    
}
