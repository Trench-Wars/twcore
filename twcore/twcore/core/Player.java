package twcore.core;

import java.util.LinkedList;

/**
 * The essential class holding information on a single Subspace player.  It contains
 * information gathered from various events/packets, with all that is considered
 * relevant or useful being stored here.
 * <p>
 * A VERY IMPORTANT consideration is that a player's X & Y location, X & Y velocities & rotation
 * amount may not be accurate / updated.  Use BotAction's setPlayerPositionUpdating() to make
 * this information more reliable.
 */
public class Player {
    
    private String  m_playerName;       // Player's name.  May be shorter than actual
                                        // text of name due to the long name bug.
    private short   m_playerID;         // Player ID (for this arena -- diff. from ?userid) 
    private String  m_squadName;        // Squad name
    private byte    m_shipType;         // Type of ship.  1-8 in game, 0 spectating
    private short   m_xLocation;        // X coordinate of location
    private short   m_yLocation;        // Y coordinate of location
    private short   m_xVelocity;        // Pixels every 10 seconds ship travels on X axis
    private short   m_yVelocity;        // Pixels every 10 seconds ship travels on Y axis
    private byte    m_rotation;         // Direction ship is facing in SS degrees (0-39)
    private short   m_frequency;        // Frequency player belongs to
    private short   m_wins;             // Kills  / Wins
    private short   m_losses;           // Deaths / Losses
    private int     m_score;            // Total points
    private int     m_flagPoints;       // Points from flagging
    private int     m_killPoints;       // Points from kills
    private boolean m_acceptsAudio;     // Whether player accepts remote PMs in priv arenas
    private boolean m_hasKOTH;          // Whether player is King of the Hill
    private byte    m_ping;             // Ping in hundredths of a second
    private short   m_bounty;           // Bounty (not always reliable -- updates infrequent)
    private short   m_energy;           // Current amount of energy
    private short   m_S2CLag;           // Lag from server to computer
    private short   m_timer;            // Timing indicator
    private short   m_flagsCarried;     // Number of flags carried
    private short   m_timeStamp;        // Timestamp of the last position packet
    private short   m_identTurretee;    // Player ID of player this player is attached to
    private LinkedList m_turrets;       // List of player IDs (Integer) that are attached
    
    private boolean m_stealthOn;        // Status of Stealth
    private boolean m_cloakOn;          // Status of Cloaking
    private boolean m_xradarOn;         // Status of X-Radar
    private boolean m_antiOn;           // Status of Antiwarp
    private boolean m_warpedIn;         // True if player is warping/cloaking/uncloaking
    private boolean m_inSafe;           // True if player is in safe
    private boolean m_ufoOn;            // Status of UFO mode
        
    private boolean m_shields;          // True if player has shields
    private boolean m_super;            // True if player has super
    private int m_burst;                // # bursts
    private int m_repel;                // # repels
    private int m_thor;                 // # thor's hammers
    private int m_brick;                // # bricks
    private int m_decoy;                // # decoys
    private int m_rocket;               // # rockets
    private int m_portal;               // # portals
    
    /**
     * Creates a new instance of Player using a PlayerEntered packet.
     * @param playerEntered Event to instantiate with
     */
    public Player( PlayerEntered playerEntered ){
        
        m_ping = 0;
        m_score = 0;
        m_timer = 0;
        m_bounty = 0;
        m_energy = 0;
        m_S2CLag = 0;
        m_rotation = 0;
        m_xLocation = 0;
        m_yLocation = 0;
        m_xVelocity = 0;
        m_yVelocity = 0;
        m_timeStamp = 0;
        
        m_wins = playerEntered.getWins();
        m_losses = playerEntered.getLosses();
        m_frequency = playerEntered.getTeam();
        m_hasKOTH = playerEntered.getHasKOTH();
        m_shipType = playerEntered.getShipType();
        m_flagPoints = playerEntered.getFlagPoints();
        m_killPoints = playerEntered.getKillPoints();
        m_playerID = playerEntered.getPlayerID();
        m_acceptsAudio = playerEntered.getAcceptsAudio();
        m_flagsCarried = playerEntered.getFlagsCarried();
        m_identTurretee = playerEntered.getIdentTurretee();
        m_squadName = new String( playerEntered.getSquadName() );
        m_playerName = new String( playerEntered.getPlayerName() );
        
        m_score = m_flagPoints + m_killPoints;
        
        m_turrets = new LinkedList();
    }
    
    /**
     * Sets data of Player using a PlayerEntered packet.
     * @param playerEntered Event to set data from
     */
    public void setPlayer( PlayerEntered playerEntered ){
        
        m_wins = playerEntered.getWins();
        m_losses = playerEntered.getLosses();
        m_frequency = playerEntered.getTeam();
        m_hasKOTH = playerEntered.getHasKOTH();
        m_shipType = playerEntered.getShipType();
        m_flagPoints = playerEntered.getFlagPoints();
        m_killPoints = playerEntered.getKillPoints();
        m_playerID = playerEntered.getPlayerID();
        m_acceptsAudio = playerEntered.getAcceptsAudio();
        m_flagsCarried = playerEntered.getFlagsCarried();
        m_identTurretee = playerEntered.getIdentTurretee();
        m_squadName = new String( playerEntered.getSquadName() );
        m_playerName = new String( playerEntered.getPlayerName() );
        
        m_score = m_flagPoints + m_killPoints;
        
        m_turrets = new LinkedList();
    }
    
    /**
     * Resets all data to defaults.
     */
    public void clearPlayer(){
        
        m_wins = 0;
        m_ping = 0;
        m_score = 0;
        m_timer = 0;
        m_losses = 0;
        m_bounty = 0;
        m_energy = 0;
        m_S2CLag = 0;
        m_hasKOTH = false;
        m_shipType = 0;
        m_rotation = 0;
        m_xLocation = 0;
        m_yLocation = 0;
        m_xVelocity = 0;
        m_yVelocity = 0;
        m_timeStamp = 0;
        m_frequency = 0;
        m_playerID = -1;
        m_flagPoints = 0;
        m_killPoints = 0;
        m_squadName = "";
        m_playerName = "";
        m_acceptsAudio = false;
        m_flagsCarried = 0;
        m_identTurretee = -1;
        
        m_stealthOn = false;
        m_cloakOn = false;
        m_xradarOn = false;
        m_antiOn = false;
        m_warpedIn = false;
        m_inSafe = false;
        m_ufoOn = false;
                
        m_shields = false;
        m_super = false;
        m_burst = 0;
        m_repel = 0;
        m_thor = 0;
        m_brick = 0;
        m_decoy = 0;
        m_rocket = 0;
        m_portal = 0;
        
        m_turrets = new LinkedList();
    }
        
    /**
     * Resets all score-related data for the player.
     */
    public void scoreReset(){
        m_score = 0;
        m_wins = 0;
        m_losses = 0;
        m_killPoints = 0;
        m_flagPoints = 0;
    }

    /**
     * Updates player data upon receiving a given packet.
     * @param message Class representation of packet/event 
     */
    public void updatePlayer( PlayerDeath message ){
        
        if( message.getKillerID() == m_playerID ){
            m_wins++;
            m_score += message.getScore();
        } else if( message.getKilleeID() == m_playerID ){
            m_losses++;
        }
    }
    
    /**
     * Updates player data upon receiving a given packet.
     * @param message Class representation of packet/event 
     */
    public void updatePlayer( ScoreReset message ){
        scoreReset();
    }
    
    /**
     * Updates player data upon receiving a given packet.
     * @param message Class representation of packet/event 
     */
    public void updatePlayer( ScoreUpdate message ){
        
        m_wins = message.getWins();
        m_losses = message.getLosses();
        m_flagPoints = message.getFlagPoints();
        m_killPoints = message.getKillPoints();
        m_score = m_flagPoints + m_killPoints;
    }
    
    /**
     * Updates player data upon receiving a given packet.
     * @param message Class representation of packet/event 
     */
    public void updatePlayer( FlagVictory message ){
        
        if( message.getFrequency() == m_frequency ){
            m_score += message.getReward();
        }
    }
    
    /**
     * Updates player data upon receiving a given packet.
     * @param message Class representation of packet/event 
     */
    public void updatePlayer( FlagReward message ){
        
        if( message.getFrequency() == m_frequency ){
            m_score += message.getPoints();
        }
    }
    
    /**
     * Updates player data upon receiving a given packet.
     * @param message Class representation of packet/event 
     */
    public void updatePlayer( FrequencyChange message ){
        
        m_frequency = message.getFrequency();
    }
    
    /**
     * Updates player data upon receiving a given packet.
     * @param message Class representation of packet/event 
     */
    public void updatePlayer( FrequencyShipChange message ){
        
        m_frequency = message.getFrequency();
        m_shipType = message.getShipType();
    }
    
    /**
     * Updates player data upon receiving a PlayerPosition packet.  Because there are
     * two different kinds of position packets (long and short), and additional data
     * that may or may not be sent based on server settings and use of the energy
     * password, certain checks must be made to handle each situation.  Note that
     * bounty is only updated from short position packets when the update bounty is
     * larger than the currently recorded bounty.  This is to prevent overflow of
     * the one-byte bounty found in short position packets.
     * @param message Class representation of packet/event
     */
    public void updatePlayer( PlayerPosition message ){
        if( message.getPlayerID() != m_playerID ) return;
        m_rotation = message.getRotation();
        
        // Only trusts bounty count from weapons packet (large position packet).
        // Bounty from the short position packet is stored as only one byte, and
        // so may suffer from overflow problems.  Therefore, we only update bounty
        // from a short position packet when the short position packet bounty data
        // is larger than the current bounty stored.
        if( message.containsWeaponsInfo() ) {
            m_bounty = message.getBounty();
        } else {
            if( m_bounty < message.getBounty() )
                m_bounty = message.getBounty();
        }
        m_xLocation = message.getXLocation();
        m_yLocation = message.getYLocation();
        m_yVelocity = message.getYVelocity();
        m_xVelocity = message.getXVelocity();
        m_ping = message.getPing();
        m_timeStamp = message.getTimeStamp();
        
        // Togglables
        m_stealthOn = message.isStealthed();
        m_cloakOn = message.isCloaked();
        m_xradarOn = message.hasXRadarOn();
        m_antiOn = message.hasAntiwarpOn();
        m_warpedIn = message.isWarpingIn();
        m_inSafe = message.isInSafe();
        m_ufoOn = message.isUFO();
        
        if( message.containsEnergy() ){
            m_energy = message.getEnergy();
        }
        
        if( message.containsItemCount() ){
            m_S2CLag = message.getS2CLag();
            m_timer = message.getTimer();
            
            m_shields = message.hasShields();
            m_super = message.hasSuper();
            m_burst = message.getBurstCount();
            m_repel = message.getRepelCount();
            m_thor = message.getThorCount();
            m_brick = message.getBrickCount();
            m_decoy = message.getDecoyCount();
            m_rocket = message.getRocketCount();
            m_portal = message.getPortalCount();
            
        }
    }
    
    /**
     * Called automatically by Arena (bot does not need to update).
     * 
     * Sets this player as being attached to the player whose ID corresponds to
     * the one given.
     * @param playerID ID of the player this player has attached to
     */
    public void setAttached( short playerID ) {
        m_identTurretee = (short)playerID;        
    }
    
    /**
     * Called automatically by Arena (bot does not need to update).
     * 
     * Sets this player as being unattached to any player, and returns the ID
     * of the last player he/she was attached to.
     * @return ID of the last player this player was attached to 
     */
    public int setUnattached() {
        int lastAttachedTo = m_identTurretee;
        m_identTurretee = -1;
        return lastAttachedTo;
    }

    /**
     * Called automatically by Arena (bot does not need to update).
     * 
     * Adds the specified player ID to the list of players currently attached
     * to this player. 
     * @param playerID
     */
    public void addTurret( short playerID ) {
        Integer pID = new Integer( playerID );
        m_turrets.remove( pID );        
        m_turrets.add( pID );        
    }

    /**
     * Called automatically by Arena (bot does not need to update).
     * 
     * Removes the specified player ID from the list of players currently
     * attached to this player. 
     * @param playerID
     */
    public void removeTurret( short playerID ) {
        m_turrets.remove( new Integer( playerID ) );        
    }
    
    
    /********************************* GETTER METHODS ***********************************
    
    /**
     * @return True if the player is playing in-game as ship 1-8
     */
    public boolean isPlaying(){
        return m_shipType != 0;
    }
    
    /**
     * Return if the player is in a given ship.
     * @param shipType Ship # to test against
     * @return True if the player is in the specified ship
     */
    public boolean isShip( int shipType ){
        return m_shipType == shipType;
    }

    /**
     * @return Name of the player
     */
    public String getPlayerName(){
        
        return new String( m_playerName );
    }
    
    /**
     * @return Name of the player's squad
     */
    public String getSquadName(){
        
        return new String( m_squadName );
    }
    
    /**
     * @return Player's ship type (1-8 if playing, 0 if spectating)
     */
    public byte getShipType(){
        
        return m_shipType;
    }
    
    /**
     * Gets the player's <b>most recently updated</b> X location.
     * <p>
     * A VERY IMPORTANT consideration is that a player's X & Y location and X & Y velocities
     * may not be accurate / updated.  Use BotAction's setPlayerPositionUpdating() to make
     * this information more reliable.
     * @return X coordinate of player's location
     */
    public short getXLocation(){
        
        return m_xLocation;
    }
    
    /**
     * Gets the player's <b>most recently updated</b> Y location.
     * <p>
     * A VERY IMPORTANT consideration is that a player's X & Y location and X & Y velocities
     * may not be accurate / updated.  Use BotAction's setPlayerPositionUpdating() to make
     * this information more reliable.
     * @return Y coordinate of player's location
     */
    public short getYLocation(){
        
        return m_yLocation;
    }
    
    /**
     * Returns how quickly a player is moving along the x plane.  Velocity is
     * measured by number of pixels travelled every 10 seconds.
     * <p>
     * A VERY IMPORTANT consideration is that a player's X & Y location and X & Y velocities
     * may not be accurate / updated.  Use BotAction's setPlayerPositionUpdating() to make
     * this information more reliable.
     * @return Rate at which ship is moving on x plane
     */
    public short getXVelocity(){
        
        return m_xVelocity;
    }
    
    /**
     * Returns how quickly a player is moving along the y plane.  Velocity is
     * measured by number of pixels travelled every 10 seconds.
     * <p>
     * A VERY IMPORTANT consideration is that a player's X & Y location and X & Y velocities
     * may not be accurate / updated.  Use BotAction's setPlayerPositionUpdating() to make
     * this information more reliable.
     * @return Rate at which ship is moving on y plane
     */
    public short getYVelocity(){
        
        return m_yVelocity;
    }
    
    /**
     * Returns the direction the ship is facing in Subspace degrees.
     * SS directions:
     *     0 - Left
     *    10 - Up
     *    20 - Right
     *    30 - Down
     * <p>
     * A VERY IMPORTANT consideration is that a player's rotation may not be accurate / updated.
     * Use BotAction's setPlayerPositionUpdating() to make this information more reliable.
     * @return Direction in which ship is facing, in SS degrees (0-39)
     */
    public byte getRotation(){
        
        return m_rotation;
    }

    /**
     * @return Player's frequency
     */
    public short getFrequency(){
        
        return m_frequency;
    }
    
    /**
     * @return Player's number of kills / wins
     */
    public short getWins(){
        
        return m_wins;
    }
    
    /**
     * @return Player's number of deaths / losses
     */
    public short getLosses(){
        
        return m_losses;
    }
    
    /**
     * @return Points earned from flagging
     */
    public int getFlagPoints(){
        
        return m_flagPoints;
    }
    
    /**
     * @return Points earned from kills
     */
    public int getKillPoints(){
        
        return m_killPoints;
    }
    
    /**
     * @return True if the player accepts remote PMs in private arenas
     */
    public boolean getAcceptsAudio(){
        
        return m_acceptsAudio;
    }
    
    /**
     * @return True if player has King of the Hill flag active
     */
    public boolean getHasKOTH(){
        
        return m_hasKOTH;
    }
    
    /**
     * @return Player's current ping, in hundredths of a second
     */
    public byte getPing(){
        
        return m_ping;
    }
    
    /**
     * Returns player's current bounty.  May be somewhat dated because it can
     * only be reliably updated whenever there is a long position packet received.
     * When a short position packet is received, it only updates if the previously
     * stored bounty is smaller than the one carried in the short position packet.
     * This prevents overflow of a byte that can only store up to 255 without error.
     * @return Player's current bounty
     */
    public short getBounty(){
        
        return m_bounty;
    }
    
    /**
     * @return Player's last recorded amount of energy left 
     */
    public short getEnergy(){
        
        return m_energy;
    }
    
    /**
     * Returns lag from server to computer.  This data will only exist if the
     * ExtraPositionData flag is set to true in the zone settings.
     * @return Lag from server to computer
     */
    public short getS2CLag(){
        
        return m_S2CLag;
    }
    
    /**
     * Returns timing data.  This data will only exist if the ExtraPositionData
     * flag is set to true in the zone settings.
     * @return Lag from server to computer
     */
    public short getTimer(){
        
        return m_timer;
    }
    
    /**
     * @return ID of the player this person is attached to 
     */
    public short getTurretee(){
        
        return m_identTurretee;
    }
    
    /**
     * @return List of Integers containing the IDs of all players attached to this player
     */
    public LinkedList getTurrets() {
        return m_turrets;
    }
    
    /**
     * @return Number of flags the player is carrying
     */
    public short getflagsCarried(){
        
        return m_flagsCarried;
    }
    
    /**
     * @return Last received timestamp
     */
    public short getTimeStamp(){
        
        return m_timeStamp;
    }
    
    /**
     * Returns the unique ID of the player for the specific arena they are currently in.
     * This is different than the universal ID found in ?userid, and is assigned when
     * the player enters the arena.  If storing a player ID to retrieve a Player object
     * later on, BE SURE to check if that Player object is <tt>null</tt> before using it.
     * @return Unique ID of the player
     */
    public short getPlayerID(){
        
        return m_playerID;
    }
    
    /**
     * @return Overall point score of the player
     */
    public int getScore(){
        
        return m_score;
    }
    
    /**
     * @return True if player has shields
     */
    public boolean hasShields(){
        return m_shields;
    }
    
    /**
     * @return True if player has super
     */
    public boolean hasSuper(){
        return m_super;
    }
    
    /**
     * @return Total number of bursts player currently possesses
     */
    public int getBurstCount(){
        return m_burst;
    }
    
    /**
     * @return Total number of bursts player currently possesses
     */
    public int getRepelCount(){
        return m_repel;
    }
    
    /**
     * @return Total number of bursts player currently possesses
     */
    public int getThorCount(){
        return m_thor;
    }
    
    /**
     * @return Total number of bricks player currently possesses
     * @deprecated "Walls" is not a clear term.  Made this a wrapper for getBrickCount
     */
    public int getWallCount(){
        return getBrickCount();
    }
    
    /**
     * @return Total number of bricks player currently posesses
     */
    public int getBrickCount(){
        return m_brick;
    }
    
    /**
     * @return Total number of decoys player currently possesses
     */
    public int getDecoyCount(){
        return m_decoy;
    }
    
    /**
     * @return Total number of rockets player currently possesses
     */
    public int getRocketCount(){
        return m_rocket;
    }
    
    /**
     * @return Total number of portals player currently possesses
     */
    public int getPortalCount(){
        return m_portal;
    }
    
    /**
     * @return True if ship has Stealth on
     */
    public boolean isStealthed(){
        return m_stealthOn;
    }
    
    /**
     * @return True if ship has Cloaking on
     */
    public boolean isCloaked(){
        return m_cloakOn;
    }
    
    /**
     * @return True if ship has X-Radar on
     */
    public boolean hasXRadarOn(){
        return m_xradarOn;
    }
    
    /**
     * @return True if ship has Antiwarp on
     */
    public boolean hasAntiwarpOn(){
        return m_antiOn;
    }
    
    /**
     * @return True if ship is warping in/cloaking/uncloaking (display flash on client)
     */
    public boolean isWarpingIn(){
        return m_warpedIn;
    }
    
    /**
     * @return True if ship is currently in safe
     */
    public boolean isInSafe(){
        return m_inSafe;
    }
    
    /**
     * @return True if ship has UFO on
     */
    public boolean isUFO(){
        return m_ufoOn;
    }
    
    /**
     * @return True if ship is attached to another ship
     */
    public boolean isAttached(){
        return m_identTurretee != -1;
    }
    
    /**
     * @return True if ship has any other ships attached to it
     */
    public boolean hasAttachees(){
        return !(m_turrets.isEmpty());
    }

}
