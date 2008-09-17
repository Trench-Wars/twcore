package twcore.core.game;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import twcore.core.CoreData;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FlagDropped;
import twcore.core.events.FlagPosition;
import twcore.core.events.FlagReward;
import twcore.core.events.FlagVictory;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.Prize;
import twcore.core.events.ScoreReset;
import twcore.core.events.ScoreUpdate;
import twcore.core.events.TurfFlagUpdate;
import twcore.core.events.TurretEvent;

/**
 * Arena is used to keep track of player information in the Arena a bot is in.
 * It is also used to spectate players in order to receive their position packets,
 * as like any other client, a TWCore bot only receives position packets from
 * other players when they are close.
 * <p>
 * <u>NOTE ABOUT PLAYER IDS:</u><br>
 * "Player ID" refers to the internal packet ID used by the SS protocol rather than
 * the player's UserID found in ?userid.  IDs are assigned sequentially arena-wide
 * rather than zone-wide.  Large position packets send 2 bytes for ID, and small
 * position packets send only 1; on the offchance that more than 256 players are in
 * the arena at the same time, the server will begin sending only large position
 * packets to alleviate potential problems with the same lower byte in two IDs.<br>
 * However, if a player leaves the arena, their ID is freed up to be used by another
 * player.  This can cause confusion in bots attempting to remember players primarily
 * by their ID -- and for the reason that only 19 of a possible 23 characters of a
 * player's name is accessible (aside from using *info) name is also not reliable.
 * A combination of the two may be used, or just fairly meticulous planning.<br>
 * A last word: using Arena's getPlayer(ID) method is more reliable than getPlayer(name),
 * for the reasons described above.  If you need to get a Player object, try to do
 * so by player ID rather than name.  For utmost security also compare the name in
 * the Player object with the expected one.
 */
public class Arena {
    Map <Integer,Player>m_playerList;   // (Integer)PlayerID -> Player
    Map <String,Integer>m_playerIDList; // (String)Player Name -> (Integer)PlayerID
    Map <Integer,Map<Integer,Player>>m_frequencyList; // (Integer)Freq -> ((Integer)PlayerID -> Player))
    Map <Integer,Flag>m_flagIDList;     // (Integer)FlagID -> Flag

    private List <Integer>m_tracker;    // Queue list for spectating (gathers position data)

    /**
     * Creates a new instance of an Arena object.
     */
    public Arena(CoreData coredata) {

        m_playerList = Collections.synchronizedMap( new HashMap<Integer,Player>() );
        m_playerIDList = Collections.synchronizedMap( new HashMap<String,Integer>() );
        m_frequencyList = Collections.synchronizedMap( new HashMap<Integer,Map<Integer,Player>>() );
        m_flagIDList = Collections.synchronizedMap( new HashMap<Integer,Flag>() );

        m_tracker = Collections.synchronizedList( new LinkedList<Integer>() );

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
    public Iterator<Player> getPlayerIterator(){

        return m_playerList.values().iterator();
    }

    /**
     * Gets a mapping of IDs to players in the arena.
     * @return Map (ID -> Player)
     */
    public Map<Integer, Player> getPlayerMap() {
    	return m_playerList;
    }

    /**
     * Gets an Iterator over IDs of players in the arena.
     * @return Iterator over IDs of players in arena
     */
    public Iterator<Integer> getPlayerIDIterator(){

        return m_playerList.keySet().iterator();
    }

    /**
     * Gets an Iterator over players in ships other than 0/spec in the arena.
     * @return Iterator over players in ships other than 0/spec in arena
     */
    public Iterator<Player> getPlayingPlayerIterator(){
        LinkedList <Player>list = new LinkedList<Player>();
        for( Iterator<Player> i = getPlayerIterator(); i.hasNext(); ){
            Player player = i.next();
            if( player.getShipType() != 0 ){
                list.add(player);
            }
        }
        return list.iterator();
    }

    /**
     * Gets a List of players in ships other than spec in the arena.
     * The returned List may be modified, but do not modify the Player objects.
     * @return List of players in ships
     */
    public List<Player> getPlayingPlayers() {
    	List<Player> list = new ArrayList<Player>(m_playerList.size());
    	for(Player player : m_playerList.values()) {
    		if(player.getShipType() != 0) {
    			list.add(player);
    		}
    	}
    	return list;
    }

    /**
     * Gets an Iterator over IDs of players in ships other than 0/spec in the arena.
     * @return Iterator over IDs of players in ships other than 0/spec in arena
     */
    public Iterator<Integer> getPlayingIDIterator(){
        LinkedList <Integer>list = new LinkedList<Integer>();
        for( Iterator<Player> i = getPlayerIterator(); i.hasNext(); ){
            Player player = i.next();
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
    public Iterator<Integer> getFreqIDIterator( int freq ){

        Map<Integer, Player> freqMap = m_frequencyList.get( new Integer( freq ));
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
    public Iterator<Player> getFreqPlayerIterator( int freq ){

        Map<Integer, Player> freqMap = m_frequencyList.get( new Integer( freq ));
        if( freqMap == null ) return null;
        return freqMap.values().iterator();
    }

    /**
     * Gets an Iterator over all flags IDs in the arena.
     * @return Iterator over all flags in arena
     */
    public Iterator<Integer> getFlagIDIterator() {

        return m_flagIDList.keySet().iterator();
    }

    /**
     * Gets an Iterator over all flag objects in the arena.
     * @return Iterator over all flags in arena
     */
    public Iterator<Flag> getFlagIterator() {

    	return m_flagIDList.values().iterator();
    }

    /**
     * Gets a Flag based on a flag ID provided.
     * @param flagID Flag ID of interest
     * @return Flag requested
     */
    public Flag getFlag( int flagID ) {

        return m_flagIDList.get( new Integer( flagID ) );
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

        return m_playerList.get( new Integer( playerID ) );
    }

    /**
     * Gets a Player based on player name provided.  The name must match exactly
     * in order for this method to work properly -- or in the case of 20+ character
     * names, the first 19 characters must match.
     *
     * NOTE: It's important to check the returned Player object for a null value
     * before using it, or at least catch the possible NullPointerException.
     *
     * @param searchName Name of the player of interest
     * @return Player requested
     */
    public Player getPlayer( String searchName ){
        Player p = m_playerList.get( m_playerIDList.get( searchName.toLowerCase() ) );
        if( p == null && searchName.length() == 19 ){
            //hack to support really long names
            Iterator<Player> i = getPlayerIterator();
            while( i.hasNext() ){
                Player q = i.next();
                if( q.getPlayerName().startsWith( searchName ) ) {
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
            return ((m_playerList.get( new Integer( playerID ) ))).getPlayerName();
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
            return (m_playerIDList.get( playerName.toLowerCase() )).intValue();
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
     * Gets the number of players currently in game.
     * @return Number of players in game
     */
    public int getNumPlaying() {
        LinkedList <Player>list = new LinkedList<Player>();
        for( Iterator<Player> i = getPlayerIterator(); i.hasNext(); ){
            Player player = i.next();
            if( player.getShipType() != 0 )
                list.add(player);
        }
        return list.size();
    }

    /**
     * Gets the number of players currently spectating.
     * @return Number of players spectating
     */
    public int getNumSpectating() {
        LinkedList <Player>list = new LinkedList<Player>();
        for( Iterator<Player> i = getPlayerIterator(); i.hasNext(); ){
            Player player = i.next();
            if( player.getShipType() == 0 )
                list.add(player);
        }
        return list.size();
    }

    /**
     * Gets the number of players on a particular frequency.
     * @param freq Freq to request
     * @return Number of players on freq
     */
    public int getFreqSize( int freq ) {
        Map<Integer,Player> m = m_frequencyList.get((Integer)freq);
        if (m != null)
            return m.size();
        else
            return 0;
    }

    /**
     * Gets the number of in-game players on a particular frequency.
     * @param freq Freq to request
     * @return Number of players in-game on freq
     */
    public int getPlayingFreqSize( int freq ) {
        Map <Integer,Player>m = m_frequencyList.get((Integer)freq);
        if( m == null )
            return 0;

        LinkedList <Player>list = new LinkedList<Player>();
        for( Iterator<Player> i = m.values().iterator(); i.hasNext(); ){
            Player player = i.next();
            if( player.getShipType() != 0 ){
                list.add(player);
            }
        }
        return list.size();
    }

    // *** EVENT PROCESSING ***

    /**
     * PlayerEntered event processing.  Fires every time a player enters,
     * and also when the bot enters an arena.
     * @param message Event object to be processed
     */
    public void processEvent( PlayerEntered message ){
        int                 frequency;
        Map <Integer,Player>frequencyList;

        frequency = message.getTeam();
        Player player = new Player( message );
        Integer playerID = new Integer( message.getPlayerID() );

        m_playerList.put( playerID, player );
        m_playerIDList.put( player.getPlayerName().toLowerCase(), playerID );

        frequencyList = m_frequencyList.get( new Integer( frequency ) );
        if( frequencyList == null ){
            frequencyList = Collections.synchronizedMap( new HashMap<Integer,Player>() );
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
        Player          player = m_playerList.get( new Integer( message.getPlayerID() ) );

        if( player != null ){
            player.updatePlayer( message );
        }
    }

    /**
     * PlayerPosition event processing
     * @param message Event object to be processed
     */
    public void processEvent( PlayerPosition message ){
        Player          player = m_playerList.get( new Integer( message.getPlayerID() ) );

        if( player != null ){
            player.updatePlayer( message );
        }
    }
    
    /**
     * Prize event processing
     * @param message Event object to be processed
     */
    public void processEvent( Prize message ){
        Player          player = m_playerList.get( new Integer( message.getPlayerID() ) );

        if( player != null ){
            player.updatePlayer( message );
        }
    }

    /**
     * FrequencyChange event processing
     * @param message Event object to be processed
     */
    public void processEvent( FrequencyChange message ){
        int                 oldFrequency;
        int                 newFrequency;
        Map <Integer,Player>frequencyList;
        Player              player;

        player = m_playerList.get( new Integer( message.getPlayerID() ) );
        if( player == null ){
            return;
        }

        oldFrequency = player.getFrequency();

        player.updatePlayer( message );
        newFrequency = message.getFrequency();

        frequencyList = m_frequencyList.get( new Integer( oldFrequency ) );
        frequencyList.remove( new Integer( message.getPlayerID() ) );

        frequencyList = m_frequencyList.get( new Integer( newFrequency ) );
        if( frequencyList == null ){
            frequencyList = Collections.synchronizedMap( new HashMap<Integer,Player>() );
            m_frequencyList.put( new Integer( newFrequency ), frequencyList );
        }

        frequencyList.put( new Integer( message.getPlayerID() ), player );

        if( player.getShipType() != 0 )
            addPlayerToFrontOfTracker( new Integer( message.getPlayerID() ) );
        else
            removePlayerFromTracker( new Integer( message.getPlayerID() ) );
    }

    /**
     * FrequencyShipChange event processing
     * @param message Event object to be processed
     */
    public void processEvent( FrequencyShipChange message ){
        int                 oldFrequency;
        int                 newFrequency;
        Map <Integer,Player>frequencyList;
        Player              player;

        player = m_playerList.get( new Integer( message.getPlayerID() ) );
        if( player == null ){
            return;
        }

        oldFrequency = player.getFrequency();

        player.updatePlayer( message );
        newFrequency = message.getFrequency();

        frequencyList = m_frequencyList.get( new Integer( oldFrequency ) );
        frequencyList.remove( new Integer( message.getPlayerID() ) );

        frequencyList = m_frequencyList.get( new Integer( newFrequency ) );
        if( frequencyList == null ){
            frequencyList = Collections.synchronizedMap( new HashMap<Integer,Player>() );
            m_frequencyList.put( new Integer( newFrequency ), frequencyList );
        }

        frequencyList.put( new Integer( message.getPlayerID() ), player );

        if( player.getShipType() != 0 )
            addPlayerToFrontOfTracker( new Integer( message.getPlayerID() ) );
        else
            removePlayerFromTracker( new Integer( message.getPlayerID() ) );
    }

    /**
     * PlayerLeft event processing
     * @param message Event object to be processed
     */
    public void processEvent( PlayerLeft message ){
        int             oldFrequency;
        Map<Integer,Player> frequencyList;
        Player          player;

        player = m_playerList.get( new Integer( message.getPlayerID() ) );
        if( player == null ){
            return;
        }

        oldFrequency = player.getFrequency();

        frequencyList = (Map<Integer,Player>)m_frequencyList.get( new Integer( oldFrequency ) );
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
        Player          killer = m_playerList.get( new Integer( message.getKillerID() ) );
        Player          killee = m_playerList.get( new Integer( message.getKilleeID() ) );

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
            Player          player = m_playerList.get( new Integer( message.getPlayerID() ) );

            if( player != null ){
                player.updatePlayer( message );
            }
        } else {
            for( Iterator<Player> i = getPlayerIterator(); i.hasNext(); ){
                Player player = i.next();
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
        Map<Integer, Player> frequencyList;

        frequencyList = m_frequencyList.get( new Integer( message.getFrequency() ) );

        if( frequencyList != null ){
            for( Iterator<Player> i = frequencyList.values().iterator(); i.hasNext(); ){
                player = i.next();
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
        Map<Integer, Player> frequencyList;

        frequencyList = m_frequencyList.get( new Integer( message.getFrequency() ) );

        if( frequencyList != null ){
            for( Iterator<Player> i = frequencyList.values().iterator(); i.hasNext(); ){
                player = i.next();
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
            flag = m_flagIDList.get( new Integer( message.getFlagID() ) );

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
            flag = m_flagIDList.get( new Integer( message.getFlagID() ) );

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
        Iterator<Flag> it = getFlagIterator();
        while( it.hasNext() ) {
            Flag flag = it.next();
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
            flag = m_flagIDList.get( new Integer( message.getFlagID() ) );

        flag.processEvent( message );
    }

    /**
     * Handles players attaching and unattaching from one another.  Rather than
     * handling the event separately inside Player, it's parsed beforehand.
     * @param message Event object to be processed
     */
    public void processEvent( TurretEvent message ){
        short attacheeID = message.getAttacheeID();
        short attacherID = message.getAttacherID();
        Player attacher = m_playerList.get( new Integer( attacherID ) );
        if( attacher == null )
            return;

        if( message.isAttaching() ) {
            // Attaching
            Player attachee = m_playerList.get( new Integer( attacheeID ) );
            if( attachee != null ) {
                attachee.addTurret( attacherID );
                attacher.setAttached( attacheeID );
            }
        } else {
            // Unattaching
            int lastAttachedTo = -1;
            lastAttachedTo = attacher.setUnattached();
            if( lastAttachedTo != -1 ) {
                Player detachedFrom = m_playerList.get( new Integer( lastAttachedTo ) );
                if( detachedFrom != null )
                    detachedFrom.removeTurret( attacherID );
            }
        }
    }

    // *** PLAYER OBJECT UPDATING ***

    /**
     * Adds a playing player into the tracker queue to be spectated by the bot
     * and receive position packets from.  Can be used to force the player to
     * the back of the queue.
     * (Called by PlayerEntered and PlayerDeath)
     * @param playerID Unique ID of player to add to the tracking system
     */
    public void addPlayerToTracker( Integer playerID ) {

		synchronized(m_tracker) {
	        m_tracker.remove( playerID );
	        m_tracker.add( playerID );
		}
    }

    /**
     * Adds a playing player to the front of the tracker queue to be spectated
     * by the bot and receive position packets from.  Used after a player's position
     * needs an immediate update.
     * (Called by FrequencyChange and FrequencyShipChange)
     * @param playerID Unique ID of player to add to the tracking system
     */
    public void addPlayerToFrontOfTracker( Integer playerID ) {

        synchronized(m_tracker) {
            m_tracker.remove( playerID );
            m_tracker.add( 0, playerID );
        }
    }

    /**
     * Removes a playing player from the tracker queue.
     * (Called by PlayerLeft, FrequencyChange, FrequencyShipChange)
     * @param playerID Unique ID of player to remove from the tracking system
     */
    public void removePlayerFromTracker( Integer playerID ) {

        m_tracker.remove( playerID );
    }

    /**
     * Looks at the head of the queue of players to be spectated, adds the
     * next player's ID to the tail of the queue, and returns their ID.
     * @return The ID of the next player in the queue to be spectated on
     */
    public int getNextPlayerToWatch() {
        synchronized(m_tracker) {
	        if(m_tracker.isEmpty()) {
	            return -1;
	        } else {
	            Integer i = m_tracker.remove(0);
	            m_tracker.add(i);
	            return i.intValue();
	        }
        }
    }

}
