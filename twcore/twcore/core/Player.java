package twcore.core;

public class Player {
    
    private String  m_playerName;
    private String  m_squadName;
    private byte    m_shipType;
    private short   m_xLocation;
    private short   m_yLocation;
    private short   m_xVelocity;
    private short   m_yVelocity;
    private short   m_frequency;
    private short   m_wins;
    private short   m_losses;
    private int     m_flagPoints;
    private int     m_killPoints;
    private byte    m_acceptsAudio;
    private byte    m_hasKOTH;
    private byte    m_rotation;
    private byte    m_ping;
    private short   m_bounty;
    private short   m_energy;
    private short   m_s2CLag;
    private short   m_timer;
    private short   m_identTurretee;
    private short   m_flagsCarried;
    private short   m_timeStamp;
    private short   m_playerID;
    private int     m_score;
    
    private boolean m_stealthOn;
    private boolean m_cloakOn;
    private boolean m_xradarOn;
    private boolean m_antiOn;
    private boolean m_warpedIn;
    private boolean m_inSafe;
    private boolean m_ufoOn;
    
    private int m_s2clag;
    
    private boolean m_shields;
    private boolean m_super;
    private int m_burst;
    private int m_repel;
    private int m_thor;
    private int m_wall;
    private int m_decoy;
    private int m_rocket;
    private int m_portal;
    
    public Player( PlayerEntered playerEntered ){
        
        m_ping = 0;
        m_score = 0;
        m_timer = 0;
        m_bounty = 0;
        m_energy = 0;
        m_s2CLag = 0;
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
    }
    
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
    }
    
    public void clearPlayer(){
        
        m_wins = 0;
        m_ping = 0;
        m_score = 0;
        m_timer = 0;
        m_losses = 0;
        m_bounty = 0;
        m_energy = 0;
        m_s2CLag = 0;
        m_hasKOTH = 0;
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
        m_acceptsAudio = 0;
        m_flagsCarried = 0;
        m_identTurretee = 0;
        
        m_stealthOn = false;
        m_cloakOn = false;
        m_xradarOn = false;
        m_antiOn = false;
        m_warpedIn = false;
        m_inSafe = false;
        m_ufoOn = false;
        
        m_s2clag = 0;
        
        m_shields = false;
        m_super = false;
        m_burst = 0;
        m_repel = 0;
        m_thor = 0;
        m_wall = 0;
        m_decoy = 0;
        m_rocket = 0;
        m_portal = 0;
        
    }
    
    public boolean isPlaying(){
        return m_shipType != 0;
    }
    
    public boolean isShip( int shipType ){
        return m_shipType == shipType;
    }
    
    public void scoreReset(){
        m_score = 0;
        m_wins = 0;
        m_losses = 0;
        m_killPoints = 0;
        m_flagPoints = 0;
    }
    
    public void updatePlayer( PlayerDeath message ){
        
        if( message.getKillerID() == m_playerID ){
            m_wins++;
            m_score += message.getScore();
        } else if( message.getKilleeID() == m_playerID ){
            m_losses++;
        }
    }
    
    public void updatePlayer( ScoreReset message ){
        scoreReset();
    }
    
    public void updatePlayer( ScoreUpdate message ){
        
        m_wins = message.getWins();
        m_losses = message.getLosses();
        m_flagPoints = message.getFlagPoints();
        m_killPoints = message.getKillPoints();
        m_score = m_flagPoints + m_killPoints;
    }
    
    public void updatePlayer( FlagVictory message ){
        
        if( message.getFrequency() == m_frequency ){
            m_score += message.getReward();
        }
    }
    
    public void updatePlayer( FlagReward message ){
        
        if( message.getFrequency() == m_frequency ){
            m_score += message.getPoints();
        }
    }
    
    public void updatePlayer( FrequencyChange message ){
        
        m_frequency = message.getFrequency();
    }
    
    public void updatePlayer( FrequencyShipChange message ){
        
        m_frequency = message.getFrequency();
        m_shipType = message.getShipType();
    }
    
    public void updatePlayer( PlayerPosition message ){
        if( message.getPlayerID() != m_playerID ) return;
        m_rotation = message.getRotation();
        m_bounty = message.getBounty();
        m_xLocation = message.getXLocation();
        m_yLocation = message.getYLocation();
        m_yVelocity = message.getYVelocity();
        m_xVelocity = message.getXVelocity();
        m_ping = message.getPing();
        m_timeStamp = message.getTimeStamp();
        
        //Togglables
        
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
            m_s2clag = message.getS2CLag();
            m_timer = message.getTimer();
            
            m_shields = message.hasShields();
            m_super = message.hasSuper();
            m_burst = message.getBurstCount();
            m_repel = message.getRepelCount();
            m_thor = message.getThorCount();
            m_wall = message.getWallCount();
            m_decoy = message.getDecoyCount();
            m_rocket = message.getRocketCount();
            m_portal = message.getPortalCount();
            
        }
        
        
    }
    
    public String getPlayerName(){
        
        return new String( m_playerName );
    }
    
    public String getSquadName(){
        
        return new String( m_squadName );
    }
    
    public byte getShipType(){
        
        return m_shipType;
    }
    
    public short getXLocation(){
        
        return m_xLocation;
    }
    
    public short getYLocation(){
        
        return m_yLocation;
    }
    
    public short getXVelocity(){
        
        return m_xVelocity;
    }
    
    public short getYVelocity(){
        
        return m_yVelocity;
    }
    
    public short getFrequency(){
        
        return m_frequency;
    }
    
    public short getWins(){
        
        return m_wins;
    }
    
    public short getLosses(){
        
        return m_losses;
    }
    
    public int getFlagPoints(){
        
        return m_flagPoints;
    }
    
    public int getKillPoints(){
        
        return m_killPoints;
    }
    
    public byte getAcceptsAudio(){
        
        return m_acceptsAudio;
    }
    
    public byte getHasKOTH(){
        
        return m_hasKOTH;
    }
    
    public byte getRotation(){
        
        return m_rotation;
    }
    
    public byte getPing(){
        
        return m_ping;
    }
    
    public short getBounty(){
        
        return m_bounty;
    }
        
    public short getEnergy(){
        
        return m_energy;
    }
    
    public short getS2CLag(){
        
        return m_s2CLag;
    }
    
    public short getTimer(){
        
        return m_timer;
    }
        
    public short getTurretee(){
        
        return m_identTurretee;
    }
    
    public short getflagsCarried(){
        
        return m_flagsCarried;
    }
    
    public short getTimeStamp(){
        
        return m_timeStamp;
    }
    
    public short getPlayerID(){
        
        return m_playerID;
    }
    
    public int getScore(){
        
        return m_score;
    }
    
    public boolean hasShields(){
        return m_shields;
    }
    
    public boolean hasSuper(){
        return m_super;
    }
    
    public int getBurstCount(){
        return m_burst;
    }
    
    public int getRepelCount(){
        return m_repel;
    }
    
    public int getThorCount(){
        return m_thor;
    }
    
    public int getWallCount(){
        return m_wall;
    }
    
    public int getDecoyCount(){
        return m_decoy;
    }
    
    public int getRocketCount(){
        return m_rocket;
    }
    
    public int getPortalCount(){
        return m_portal;
    }
    
    public boolean isStealthed(){
        return m_stealthOn;
    }
    
    public boolean isCloaked(){
        return m_cloakOn;
    }
    
    public boolean hasXRadarOn(){
        return m_xradarOn;
    }
    
    public boolean hasAntiwarpOn(){
        return m_antiOn;
    }
    
    public boolean isWarpingIn(){
        return m_warpedIn;
    }
    
    public boolean isInSafe(){
        return m_inSafe;
    }
    
    public boolean isUFO(){
        return m_ufoOn;
    }
}
