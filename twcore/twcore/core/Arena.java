/*
 * Arena.java
 *
 * Created on December 12, 2001, 9:10 AM
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
    
    public Arena() {
        
        m_playerList = Collections.synchronizedMap( new HashMap() );
        m_playerIDList = Collections.synchronizedMap( new HashMap() );
        m_frequencyList = Collections.synchronizedMap( new HashMap() );
        m_flagIDList = Collections.synchronizedMap( new HashMap() );
    }
    
    public void clear(){
        
        m_playerList.clear();
        m_playerIDList.clear();
        m_frequencyList.clear();
        m_flagIDList.clear();
    }
    
    public Iterator getPlayerIterator(){
        
        return m_playerList.values().iterator();
    }
    
    public Iterator getPlayerIDIterator(){
        
        return m_playerList.keySet().iterator();
    }
    
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
    
    public Iterator getFreqIDIterator( int freq ){
        
        Map freqMap = (Map)m_frequencyList.get( new Integer( freq ));        
        if( freqMap == null ){
            return null;
        } else {
            return freqMap.keySet().iterator();
        }
    }

    public Iterator getFreqPlayerIterator( int freq ){
        
        Map freqMap = (Map)m_playerList.get( new Integer( freq ));
        if( freqMap == null ) return null;
        return freqMap.values().iterator();        
    }
    
    public Iterator getFlagIDIterator() {
    	
    	return m_flagIDList.values().iterator();
    }
    
    public Flag getFlag( int flagID ) {
    	
    	return (Flag)m_flagIDList.get( new Integer( flagID ) );
    }
    
    public Player getPlayer( int playerID ){
        
        return (Player)m_playerList.get( new Integer( playerID ) );
    }
    
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
    
    public String getPlayerName( int playerID ){
        
        try {
            return ((Player)(m_playerList.get( new Integer( playerID ) ))).getPlayerName();
        } catch( Exception e ){
            return null;
        }
    }
    
    public int getPlayerID( String playerName ){
        try {
            return ((Integer)m_playerIDList.get( playerName.toLowerCase() )).intValue();
        } catch( Exception e ){
            return -1;
        }
    }
    
    public int size(){
        return m_playerList.size();
    }
    
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
    }
    
    public void processEvent( ScoreUpdate message ){
        Player          player = (Player)m_playerList.get( new Integer( message.getPlayerID() ) );
        
        if( player != null ){
            player.updatePlayer( message );
        }
    }
    
    public void processEvent( PlayerPosition message ){
        Player          player = (Player)m_playerList.get( new Integer( message.getPlayerID() ) );

        if( player != null ){
            player.updatePlayer( message );
        }
    }
/*
    public void processEvent( WeaponUpdate message ){
        Player          player = (Player)m_playerList.get( new Integer( message.getPlayerID() ) );

        if( player != null ){
            player.updatePlayer( message );
        }
    }*/
    
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
    }
    
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
    }
    
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
    }
    
    public void processEvent( PlayerDeath message ){
        Player          killer = (Player)m_playerList.get( new Integer( message.getKillerID() ) );
        Player          killee = (Player)m_playerList.get( new Integer( message.getKilleeID() ) );

        if( killer != null ){
            killer.updatePlayer( message );
        }

        if( killee != null ){
            killee.updatePlayer( message );
        }
    }
    
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
    
    public void processEvent( FlagPosition message ) {
    	Flag flag;
    	if( !m_flagIDList.containsKey( new Integer( message.getFlagID() ) ) ) {
    		flag = new Flag( message );
    		m_flagIDList.put( new Integer( message.getFlagID() ), flag );
    	} else
    		flag = (Flag)m_flagIDList.get( new Integer( message.getFlagID() ) );
    		
    	flag.processEvent( message );
    }
    
    public void processEvent( FlagClaimed message ) {
    	Flag flag;
    	if( !m_flagIDList.containsKey( new Integer( message.getFlagID() ) ) ) {
    		flag = new Flag( message );
    		m_flagIDList.put( new Integer( message.getFlagID() ), flag );
    	} else
    		flag = (Flag)m_flagIDList.get( new Integer( message.getFlagID() ) );
    		
    	flag.processEvent( message );
    }
    
    public void processEvent( FlagDropped message ) {
    	Iterator it = getFlagIDIterator();
    	while( it.hasNext() ) {
    		Flag flag = (Flag)it.next();
    		if( flag.getPlayerID() == message.getPlayerID() )
    			flag.dropped();
    	}
    }
}
