package twcore.core.game;

import java.util.BitSet;

import twcore.core.net.GamePacketGenerator;
import twcore.core.game.Arena;

/**
 * Representation of the bot as as a Subspace ship for in-game playing.
 *
 * @author  harvey
 */
public final class Ship extends Thread {

    public static final double VELOCITY_TIME = 10000.0;     // # ms to divide velocity by
                                                            //   to determine distance

    // Internal ship number enum; should be used for packet construction but not normal ship #'s
	public static final byte INTERNAL_WARBIRD = 0, INTERNAL_JAVELIN = 1, INTERNAL_SPIDER = 2, INTERNAL_LEVIATHAN = 3,
							 INTERNAL_TERRIER = 4, INTERNAL_WEASEL = 5, INTERNAL_LANCASTER = 6, INTERNAL_SHARK = 7,
							 INTERNAL_SPECTATOR = 8, INTERNAL_PLAYINGSHIP = 9, INTERNAL_ALL = 10;

    private volatile long	m_movingUpdateTime  = 100;      // How often a moving ship's
                                                            //   position is updated
    private volatile long	m_unmovingUpdateTime = 1000;    // How often an unmoving ship's
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

    private short       shipType = INTERNAL_SPECTATOR;       // 0-7 in-game ship; 8 spectating

    private long m_mAge;   // Last time position updated
    private long m_pAge;   // Last time packet sent

    private GamePacketGenerator m_gen;			// Packet generator
    private Arena				m_arenaTracker;	// for getting next id to spectate on
    private int 				m_lastId = -1;	// previous id spectated on
    private volatile long		m_spectatorUpdateTime = 0;	// how often to switch which player
    														// is being spectated by bot

	/**
	 * Converts a ship type from the 1-8, 0 is spec format used in Player's
	 * getShipType() method to the 0-7, 8 is spec format of the constants.
	 * @param shipType the old style ship type to convert
	 * @return the adjusted ship type value
	 */
    public static byte typeToConstant(byte shipType)
    {
    	shipType--;
    	if(shipType == -1)
    		shipType = INTERNAL_SPECTATOR;

    	return shipType;
    }

    /**
     * Create a new instance of the Ship class (a separate program Thread).
     * @param group Thread grouping to add this new thread to
     * @param gen For generating appropriate position packets
     */
    public Ship(ThreadGroup group, GamePacketGenerator gen, Arena arenaTracker) {
        super(group, group.getName()+"-Ship");
        m_gen = gen;
        m_arenaTracker = arenaTracker;
        m_mAge = m_pAge = System.currentTimeMillis();
    }

    /**
     * Regularly checks to make updates to position by way of sending a new
     * position packet.  If the ship isn't moving, it doesn't need to be updated
     * quite as often as a moving ship.  Default regularity of updates is
     * 100ms and 1000ms for moving and unmoving ships, respectively.  Set update
     * times either hard in the code, or with setUpdateTime()
     */
    public void run() {
        try {
            while(!interrupted()) {
            	long sleepTime;
                if(shipType == INTERNAL_SPECTATOR) {
                	sleepTime = getSpectatorUpdateTime();
                	if(sleepTime > 0) {
                		int id = m_arenaTracker.getNextPlayerToWatch();
                		if(id != m_lastId) {
                			//System.out.println("Spectating: " + id);
	                		m_gen.sendSpectatePacket((short)id);
	                		m_lastId = id;
                		}
                	} else {
		                updatePosition();
	                	sleepTime = getUnmovingUpdateTime();
                	}
                } else if(xVel == 0 && yVel == 0) {
	                updatePosition();
                	sleepTime = getUnmovingUpdateTime();
                } else {
	                updatePosition();
                	sleepTime = getMovingUpdateTime();
                }
                sleep(sleepTime);
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
        return xVel != lastXV || yVel != lastYV || lastD != direction || System.currentTimeMillis() - m_pAge >= m_unmovingUpdateTime;
    }

    /**
     * Generates a position packet for the ship, without weapon data.
     */
    public void sendPositionPacket()
    {
        m_gen.sendPositionPacket( direction, xVel, y, togglables, x, yVel, bounty, energy, (short)0 );
        m_pAge = (int)System.currentTimeMillis();
    }

    /**
     * Generates a position packet for the ship, and fires the weapon specified.
     * Refer to the getWeaponNumber method for proper setup of a weapon.
     * @param weapon A 16-bit bitvector containing weapon information
     * @see #getWeaponNumber(byte, byte, boolean, boolean, boolean, byte, boolean)
     */
    public void fire( int weapon ){
        m_gen.sendPositionPacket( direction, xVel, y, togglables, x, yVel, bounty, energy, (short)weapon );
        m_pAge = (int)System.currentTimeMillis();
    }
    
    /**
     * Drops a brick at the bots current location.
     */
    public void dropBrick(){
    	m_gen.sendDropBrick(this.x/16, this.y/16);
    	m_pAge = (int)System.currentTimeMillis();
    }
    
    /**
     * Drops a brick at a specified location.
     * @param xLocation - X Location in tiles.
     * @param yLocation - Y Location in tiles.
     */
    public void dropBrick( int xLocation, int yLocation){
    	m_gen.sendDropBrick(xLocation, yLocation);
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
        if( shipType != INTERNAL_SPECTATOR )
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
        if( shipType != INTERNAL_SPECTATOR )
            sendPositionPacket();
    }

    /**
     * Increases or decreases velocity.  Using this method prevents having to
     * look up the old velocity in order to set it, acting as more an accelerator
     * or decelerator.
     * @param xVelChange
     * @param yVelChange
     */
    public void alterVelocity(int xVelChange, int yVelChange ) {
        this.lastXV = this.xVel;
        this.lastYV = this.yVel;
        this.xVel = (short)(xVel + xVelChange);
        this.yVel = (short)(yVel + yVelChange);
        if( shipType != INTERNAL_SPECTATOR )
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
        if( rotation < 0 )
            rotation = 0;
        else if( rotation > 39 )
            rotation = 39;
        lastD = (byte)rotation;
        direction = (byte)rotation;
        if( shipType != INTERNAL_SPECTATOR )
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
     * Rotates the ship left by the number of Subspace rotation points given.
     * This prevents having to look up or store which direction the ship is
     * currently facing.
     * @param rotationAmount Number of SS rotation points by which to rotate left
     */
    public void rotateLeft( int rotationAmount ) {
        int newrot = this.direction - rotationAmount;
        if( newrot < 0 )
            newrot = 40 + newrot;
        setRotation( newrot );
    }

    /**
     * Rotates the ship right by the number of Subspace rotation points given.
     * This prevents having to look up or store which direction the ship is
     * currently facing.
     * @param rotationAmount Number of SS rotation points by which to rotate right
     */
    public void rotateRight( int rotationAmount ) {
        int newrot = this.direction + rotationAmount;
        if( newrot > 39 )
            newrot = newrot - 40;
        setRotation( newrot );
    }

    /**
     * Sets ship type from 0-8.  0 is warbird.  If set to 8, the bot will
     * become a spectator, and cease sending any kind of position packet.
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
     * Sets the time between player switching updates for when the bot is spectating.
     * @param updateTime Time in ms between player switching, 0 to disable
     */
    public void setSpectatorUpdateTime(int updateTime) {
    	m_spectatorUpdateTime = updateTime;
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
    public long getAge() {
        return (int)(System.currentTimeMillis() - m_mAge);
    }

    /**
     * @return Interval in ms between position packet sendings while ship is moving
     */
    public int getMovingUpdateTime() {
        return (int)m_movingUpdateTime;
    }

    /**
     * @return Interval in ms between position packet sendings while ship is moving
     */
    public int getUnmovingUpdateTime() {
        return (int)m_unmovingUpdateTime;
    }

    /**
     * @return Interval in ms between player switching while ship is spectator
     */
    public synchronized int getSpectatorUpdateTime() {
    	return (int)m_spectatorUpdateTime;
    }
}
