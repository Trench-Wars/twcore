/*
 * Arena.java
 * 
 * Arena is used to keep track of player information in the Arena a bot is in.  
 * It is also used to spectate players in order to receive their position packets,
 * as like any other client, a TWCore bot only receives position packets from
 * other players when they are close.
 * 
 * NOTE ABOUT PLAYER IDS:
 * "Player ID" refers to the internal packet ID used by the SS protocol rather than
 * the player's UserID found in ?userid.  IDs are assigned sequentially arena-wide
 * rather than zone-wide.  In almost all cases, an ID is sent as 2 bytes; however,
 * short position packets, used to update frequent info on a player, send ID as 1 byte.
 * This doesn't become a problem until we have >255 people in an arena, after which a
 * client with a movement prediction algorithm has no real trouble distinguishing between
 * two players with the same 1-byte ID, but a bot core does.  Fair warning!  -qan
 */

package twcore.core;

import java.util.*;

public class Arena {
    
    //Player Integer wrapped ID# mapped to Player objects;
    Map         m_playerList;
    
    //String player names mapped to player ID#'s
    Map         m_playerIDList;
    
    //Integer wrapped freq numbers mapped to maps
    //Inner maps have playerID's mapped to Player objects.
    Map         m_frequencyList;
    
    //Flag Integer wrapped ID# mapped to Flag objects;
    Map         m_flagIDList;
    
    //Arena Tracker variables
    private List m_tracker;
    private int  m_currentTimer = 0;
    private int  m_updateTimer = 0;
    
    /**
     * Creates a new instance of an Arena object.
     *
     */
    public Arena() {
        
        m_playerList = Collections.synchronizedMap( new HashMap() );
        m_playerIDList = Collections.synchronizedMap( new HashMap() );
        m_frequencyList = Collections.synchronizedMap( new HashMap() );
        m_flagIDList = Collections.synchronizedMap( new HashMap() );
        
        m_tracker = Collections.synchronizedList( new LinkedList() );
    }
    
    /**
     * Clears all data.  Used when the bot changes Arenas.  Note that it
     * is not foolproof, as there may be a delay between when the arena change
     * is made and when the data is cleared, resulting in unexpected packets
     * received.  This is particularly common if two arena changes are made in
     * a very short amount of time.
     */
    public void clear(){
        
        m_playerList.clear();
        m_playerIDList.clear();
        m_frequencyList.clear();
        m_flagIDList.clear();
        m_tracker.clear();
    }
    
    /**
     * Gets an Iterator over the players in the arena.
     * @return Iterator over players in arena
     */
    public Iterator getPlayerIterator(){
        
        return m_playerList.values().iterator();
    }
    
    /**
     * Gets a mapping of IDs to players in the arena.
     * @return Map (ID -> Player)
     */
    public Map getPlayerMap() {
    	return m_playerList;
    }
    
    /**
     * Gets an Iterator over IDs of players in the arena.
     * @return Iterator over IDs of players in arena
     */
    public Iterator getPlayerIDIterator(){
        
        return m_playerList.keySet().iterator();
    }
    
    /**
     * Gets an Iterator over players in ships other than 0/spec in the arena.
     * @return Iterator over players in ships other than 0/spec in arena
     */    
    public Iterator getPlayingPlayerIterator(){
        LinkedList list = new LinkedList();
        for( Iterator i = getPlayerIterator(); i.hasNext(); ){
            Player player = (Player)i.next();
            if( player.getShipType() != 0 ){
                list.add(player);
            }            
        }
        return list.iterator();
    }

    /**
     * Gets an Iterator over IDs of players in ships other than 0/spec in the arena.
     * @return Iterator over IDs of players in ships other than 0/spec in arena
     */    
    public Iterator getPlayingIDIterator(){
        LinkedList list = new LinkedList();
        for( Iterator i = getPlayerIterator(); i.hasNext(); ){
            Player player = (Player)i.next();
            if( player.getShipType() != 0 ){
                list.add( new Integer( player.getPlayerID() ));
            }            
        }
        return list.iterator();
    }
    
    /**
     * Gets an Iterator over IDs of players on a given freq in the arena.
     * @param freq Frequency of particular interest
     * @return Iterator over IDs of players on a given freq in arena
     */    
    public Iterator getFreqIDIterator( int freq ){
        
        Map freqMap = (Map)m_frequencyList.get( new Integer( freq ));        
        if( freqMap == null ){
            return null;
        } else {
            return freqMap.keySet().iterator();
        }
    }

    /**
     * Gets an Iterator over players on a given freq in the arena.
     * @param freq Frequency of particular interest
     * @return Iterator over players on a given freq in arena
     */    
    public Iterator getFreqPlayerIterator( int freq ){
        
        Map freqMap = (Map)m_playerList.get( new Integer( freq ));
        if( freqMap == null ) return null;
        return freqMap.values().iterator();        
    }
    
    /**
     * Gets an Iterator over all flags in the arena.
     * @return Iterator over all flags in arena
     */
    public Iterator getFlagIDIterator() {
        
        return m_flagIDList.values().iterator();
    }

    /**
     * Gets a Flag based on a flag ID provided.
     * @param flagID Flag ID of interest
     * @return Flag requested
     */
    public Flag getFlag( int flagID ) {
        
        return (Flag)m_flagIDList.get( new Integer( flagID ) );
    }
    
    /**
     * Gets a Player based on player ID provided.
     * 
     * NOTE: It's important to check the returned Player object for a null value
     * or catch the resulting NullPointerException if you make reference to the
     * object without checking for null.  This is the single most common error
     * made by new TWCore botmakers.  Don't trust that the playerID provided from
     * an event packet will correspond to an existing player.  It's possible that
     * the event will fire simultaneously as the player leaves, resulting in a null
     * value for the Player object. 
     * 
     * @param playerID Player ID of interest
     * @return Player requested
     */
    public Player getPlayer( int playerID ){
        
        return (Player)m_playerList.get( new Integer( playerID ) );
    }
    
    /**
     * Gets a Player based on player name provided.
     * 
     * NOTE: It's important to check the returned Player object for a null value
     * before using it, or at least catch the possible NullPointerException. 
     * 
     * @param playerName Name of the player of interest
     * @return Player requested
     */
    public Player getPlayer( String playerName ){
        Player p = (Player)m_playerList.get( m_playerIDList.get( playerName.toLowerCase() ) );
        if( p == null ){
            //hack to support really long names
            Iterator i = getPlayerIterator();
            while( i.hasNext() ){
                Player q = (Player)i.next();
                if( playerName.startsWith( q.getPlayerName() )){
                    return q;
                }
            }
            return null;
        } else {
            return p;
        }
    }

    
    /**
     * Gets the name of a player based on ID. 
     * @param playerID Player ID of interest
     * @return Player requested
     */
    public String getPlayerName( int playerID ){
        
        try {
            return ((Player)(m_playerList.get( new Integer( playerID ) ))).getPlayerName();
        } catch( Exception e ){
            return null;
        }
    }
    
    /**
     * Gets the ID of a player based on name.
     * @param playerName Player name of interest
     * @return Player requested
     */
    public int getPlayerID( String playerName ){
        try {
            return ((Integer)m_playerIDList.get( playerName.toLowerCase() )).intValue();
        } catch( Exception e ){
            return -1;
        }
    }
    
    /**
     * Gets the size of the list of players (total in arena)
     * @return number of players in the arena
     */
    public int size(){
        return m_playerList.size();
    }
    
    /**
     * PlayerEntered event processing.  Fires every time a player enters,
     * and also when the bot enters an arena.
     * @param message Event object to be processed
     */
    public void processEvent( PlayerEntered message ){
        int             frequency;
        Map             frequencyList;
        
        frequency = message.getTeam();
        Player player = new Player( message );
        Integer playerID = new Integer( message.getPlayerID() );
        
        m_playerList.put( playerID, player );
        m_playerIDList.put( player.getPlayerName().toLowerCase(), playerID );
        
        frequencyList = (Map)m_frequencyList.get( new Integer( frequency ) );
        if( frequencyList == null ){
            frequencyList = Collections.synchronizedMap( new HashMap() );
            m_frequencyList.put( new Integer( frequency ), frequencyList );
        }
        
        frequencyList.put( playerID, player );
        if( message.getShipType() != 0 )
            addPlayerToTracker( new Integer( message.getPlayerID() ) );            
    }
    
    /**
     * ScoreUpdate event processing
     * @param message Event object to be processed
     */
    public void processEvent( ScoreUpdate message ){
        Player          player = (Player)m_playerList.get( new Integer( message.getPlayerID() ) );
        
        if( player != null ){
            player.updatePlayer( message );
        }
    }
    
    /**
     * PlayerPosition event processing
     * @param message Event object to be processed
     */
    public void processEvent( PlayerPosition message ){
        Player          player = (Player)m_playerList.get( new Integer( message.getPlayerID() ) );

        if( player != null ){
            player.updatePlayer( message );
        }
    }
    
    /**
     * FrequencyChange event processing
     * @param message Event object to be processed
     */
    public void processEvent( FrequencyChange message ){
        int             oldFrequency;
        int             newFrequency;
        Map             frequencyList;
        Player          player;
        
        player = (Player)m_playerList.get( new Integer( message.getPlayerID() ) );
        if( player == null ){
            return;
        }

        oldFrequency = player.getFrequency();
        
        player.updatePlayer( message );
        newFrequency = message.getFrequency();
        
        frequencyList = (Map)m_frequencyList.get( new Integer( oldFrequency ) );
        frequencyList.remove( new Integer( message.getPlayerID() ) );
        
        frequencyList = (Map)m_frequencyList.get( new Integer( newFrequency ) );
        if( frequencyList == null ){
            frequencyList = Collections.synchronizedMap( new HashMap() );
            m_frequencyList.put( new Integer( newFrequency ), frequencyList );
        }
        
        frequencyList.put( new Integer( message.getPlayerID() ), player );
        
        if( player.getShipType() != 0 )
            addPlayerToTracker( new Integer( message.getPlayerID() ) );
        else
            removePlayerFromTracker( new Integer( message.getPlayerID() ) );
    }
    
    /**
     * FrequencyShipChange event processing
     * @param message Event object to be processed
     */
    public void processEvent( FrequencyShipChange message ){
        int             oldFrequency;
        int             newFrequency;
        Map             frequencyList;
        Player          player;
        
        player = (Player)m_playerList.get( new Integer( message.getPlayerID() ) );
        if( player == null ){
            return;
        }

        oldFrequency = player.getFrequency();
        
        player.updatePlayer( message );
        newFrequency = message.getFrequency();
        
        frequencyList = (Map)m_frequencyList.get( new Integer( oldFrequency ) );
        frequencyList.remove( new Integer( message.getPlayerID() ) );
        
        frequencyList = (Map)m_frequencyList.get( new Integer( newFrequency ) );
        if( frequencyList == null ){
            frequencyList = Collections.synchronizedMap( new HashMap() );
            m_frequencyList.put( new Integer( newFrequency ), frequencyList );
        }
        
        frequencyList.put( new Integer( message.getPlayerID() ), player );
        
        if( player.getShipType() != 0 ) addPlayerToTracker( new Integer( message.getPlayerID() ) );
        else removePlayerFromTracker( new Integer( message.getPlayerID() ) );
    }
    
    /**
     * PlayerLeft event processing
     * @param message Event object to be processed
     */
    public void processEvent( PlayerLeft message ){
        int             oldFrequency;
        Map             frequencyList;
        Player          player;
        
        player = (Player)m_playerList.get( new Integer( message.getPlayerID() ) );
        if( player == null ){
            return;
        }

        oldFrequency = player.getFrequency();
        
        frequencyList = (Map)m_frequencyList.get( new Integer( oldFrequency ) );
        frequencyList.remove( new Integer( message.getPlayerID() ) );
        
        m_playerIDList.remove( player.getPlayerName().toLowerCase() );
        m_playerList.remove( new Integer( message.getPlayerID() ) );
        
        removePlayerFromTracker( new Integer( message.getPlayerID() ) );
    }
    
    /**
     * PlayerDeath event processing.  We move the player killed to the back
     * of the queue because we should receive a "free" position packet when
     * they return to life, regardless of position, and because during the
     * spawn delay it's not as worthwhile to spectate their position when we
     * could be spectating living players instead.
     * @param message Event object to be processed
     */
    public void processEvent( PlayerDeath message ){
        Player          killer = (Player)m_playerList.get( new Integer( message.getKillerID() ) );
        Player          killee = (Player)m_playerList.get( new Integer( message.getKilleeID() ) );

        if( killer != null ){
            killer.updatePlayer( message );
        }

        if( killee != null ){
            killee.updatePlayer( message );
        }
        
        addPlayerToTracker( new Integer( message.getKilleeID() ) );
    }
    
    /**
     * ScoreReset event processing
     * @param message Event object to be processed
     */
    public void processEvent( ScoreReset message ){
        if( message.getPlayerID() != -1 ){
            Player          player = (Player)m_playerList.get( new Integer( message.getPlayerID() ) );
            
            if( player != null ){
                player.updatePlayer( message );
            }
        } else {
            for( Iterator i = getPlayerIterator(); i.hasNext(); ){
                Player player = (Player)i.next();
                player.scoreReset();
            }
        }
        
    }

    /**
     * FlagVictory event processing
     * @param message Event object to be processed
     */
    public void processEvent( FlagVictory message ){
        Player          player;
        Map             frequencyList;
        
        frequencyList = (Map)m_frequencyList.get( new Integer( message.getFrequency() ) );
        
        if( frequencyList != null ){
            for( Iterator i = frequencyList.values().iterator(); i.hasNext(); ){
                player = (Player)i.next();
                player.updatePlayer( message );
            }
        }
        m_flagIDList.clear();
    }

    /**
     * FlagReward event processing
     * @param message Event object to be processed
     */
    public void processEvent( FlagReward message ){
        Player          player;
        Map             frequencyList;
        
        frequencyList = (Map)m_frequencyList.get( new Integer( message.getFrequency() ) );
        
        if( frequencyList != null ){
            for( Iterator i = frequencyList.values().iterator(); i.hasNext(); ){
                player = (Player)i.next();
                player.updatePlayer( message );
            }
        }
    }
    
    /**
     * FlagPosition event processing
     * @param message Event object to be processed
     */
    public void processEvent( FlagPosition message ) {
        Flag flag;
        if( !m_flagIDList.containsKey( new Integer( message.getFlagID() ) ) ) {
            flag = new Flag( message );
            m_flagIDList.put( new Integer( message.getFlagID() ), flag );
        } else
            flag = (Flag)m_flagIDList.get( new Integer( message.getFlagID() ) );
            
        flag.processEvent( message );
    }
    
    /**
     * FlagClaimed event processing
     * @param message Event object to be processed
     */
    public void processEvent( FlagClaimed message ) {
        Flag flag;
        if( !m_flagIDList.containsKey( new Integer( message.getFlagID() ) ) ) {
            flag = new Flag( message );
            m_flagIDList.put( new Integer( message.getFlagID() ), flag );
        } else
            flag = (Flag)m_flagIDList.get( new Integer( message.getFlagID() ) );
            
        try {            
            flag.processEvent( message, getPlayer( message.getPlayerID() ).getFrequency() );
        } catch (Exception e) {
            // If given playerID returns a null Player, set team to -1 
            flag.processEvent( message, -1 );            
        }
    }
    
    /**
     * FlagDropped event processing
     * @param message Event object to be processed
     */
    public void processEvent( FlagDropped message ) {
        Iterator it = getFlagIDIterator();
        while( it.hasNext() ) {
            Flag flag = (Flag)it.next();
            if( flag.getPlayerID() == message.getPlayerID() )
                flag.dropped();
        }
    }
    
    /**
     * TurfFlagUpdate event processing
     * @param message Event object to be processed
     */
    public void processEvent( TurfFlagUpdate message ) {
        Flag flag;
        if( !m_flagIDList.containsKey( new Integer( message.getFlagID() ) ) ) {
            flag = new Flag( message );
            m_flagIDList.put( new Integer( message.getFlagID() ), flag );
        } else
            flag = (Flag)m_flagIDList.get( new Integer( message.getFlagID() ) );
            
        flag.processEvent( message );
    }
    
     /** Adds a playing player into the tracker Queue
     * (PlayerEntered, FrequencyShipChange, PlayerDeath)
     * @param playerID - unique ID of player to add to tracking system
     */
    public void addPlayerToTracker( Integer playerID ) {
        
        m_tracker.remove( playerID );
        m_tracker.add( playerID );
    }
    
    /** Removes a playing player from the tracker Queue
     * (PlayerLeft)
     * @param playerID - unique ID of player to remove from tracking system
     */
    public void removePlayerFromTracker( Integer playerID ) {
        
        m_tracker.remove( playerID );
    }
      
    /** Called every .100 from Session and used to maintain updating player positions
     * on regular intervals.
     * @param m_gen - GamePacketGenerator
     */
    public void checkPositionChange( GamePacketGenerator m_gen ) {
        
        m_currentTimer += 100;
        
        if( m_currentTimer > m_updateTimer && m_updateTimer != -1 ) {
            m_currentTimer = 0;

            if( m_updateTimer > 0 )
                m_gen.sendSpectatePacket( getNextPlayer().shortValue() );
            else if( m_updateTimer == 0 ) {
                m_gen.sendSpectatePacket( (short)-1 );
                m_updateTimer = -1;
            }    
        }
    }
    
    /** Returns the next player in the queue - also updates the player to keep the 
     * system moving. +)
     * @return - The next player ID in the queue
     */
    public Integer getNextPlayer() {
        
        if( m_tracker.size() > 0 ) {
            Integer i = (Integer)m_tracker.get( 0 );
            m_tracker.remove( 0 );
            m_tracker.add( i );
            return i;
        } else
            return new Integer( -1 );
        
    }
    
    /** Turns on/off the position updating system with specified 
     * timeframe for switching to the next player in the queue.
     * @param ms - time in ms to update the queue
     * < 0 : off, < 200 : on w/200 delay, anything else is specified speed
     */
    public void setPlayerPositionUpdateDelay( int ms ) {
        
        if( ms <= 0 ) 
            m_updateTimer = 0;
        else if( ms < 200 )
            m_updateTimer = 200;
        else 
            m_updateTimer = ms;
    }
}
