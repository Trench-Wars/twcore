package twcore.core;

import java.util.*;

public class Objset {
	
	HashMap m_objects;
	HashMap m_unsetObjects;
	HashMap m_privateObjects;
	HashMap m_privateUnsetObjects;
	
	public Objset() {
		m_objects = new HashMap();
		m_unsetObjects = new HashMap();
		m_privateObjects = new HashMap();
		m_privateUnsetObjects = new HashMap();
	}
	
	public void showObject( int object ) {
		
		m_unsetObjects.put( new Integer( object ), "+" );
	}
	
	//Private objset
	public void showObject( int playerId, int object ) {
		
		if( playerId < 0 ) return;
		if( !m_privateUnsetObjects.containsKey( new Integer( playerId ) ) )
			m_privateUnsetObjects.put( new Integer( playerId ), new HashMap() );
		if( !m_privateObjects.containsKey( new Integer( playerId ) ) )
			m_privateObjects.put( new Integer( playerId ), new HashMap () );
			
		HashMap playerMap = (HashMap)m_privateUnsetObjects.get( new Integer( playerId ) );
		playerMap.put( new Integer( object ), "+" );
	}
	
	public void hideObject( int object ) {
		
		if( m_objects.containsKey( new Integer( object ) ) ) {
			String status = (String)m_objects.get( new Integer( object ) );
			if( status.equals( "+" ) ) 
				m_unsetObjects.put( new Integer( object ), "-" );
		} else m_unsetObjects.put( new Integer( object ), "-" );
	}
	
	//Private objset
	public void hideObject( int playerId, int object ) {
		
		if( playerId < 0 ) return;
		if( !m_privateUnsetObjects.containsKey( new Integer( playerId ) ) )
			m_privateUnsetObjects.put( new Integer( playerId ), new HashMap() );
		if( !m_privateObjects.containsKey( new Integer( playerId ) ) )
			m_privateObjects.put( new Integer( playerId ), new HashMap () );
			
		HashMap playerMap = (HashMap)m_privateUnsetObjects.get( new Integer( playerId ) );
		HashMap playerObj = (HashMap)m_privateObjects.get( new Integer( playerId ) );
		
		if( playerObj.containsKey( new Integer( object ) ) ) {
			String status = (String)playerObj.get( new Integer( object ) );
			if( status.equals( "+" ) )
				playerMap.put( new Integer( object ), "-" );
		} else playerMap.put( new Integer( object ), "-" );
	}
	
	public void hideAllObjects() {
		m_unsetObjects.clear();
		Iterator it = m_objects.keySet().iterator();
		while( it.hasNext() ) {
			int x = ((Integer)it.next()).intValue();
			String status = (String)m_objects.get( new Integer( x ) );
			if( status.equals( "+" ) )
				m_unsetObjects.put( new Integer( x ), "-" );			
		}
	}
	
	//Private objset
	public void hideAllObjects( int playerId ) {
		if( playerId < 0 ) return;
		if( !m_privateObjects.containsKey( new Integer( playerId ) ) ) return;
		
		HashMap playerUnset = (HashMap)m_privateUnsetObjects.get( new Integer( playerId ) );
		HashMap playerObj = (HashMap)m_privateObjects.get( new Integer( playerId ) );
		
		playerUnset.clear();
		Iterator it = playerObj.keySet().iterator();
		while( it.hasNext() ) {
			int x = ((Integer)it.next()).intValue();
			String status = (String)playerObj.get( new Integer( x ) );
			if( status.equals( "+" ) )
				playerUnset.put( new Integer( x ), "-" );			
		}
	}
	
	public boolean objectShown( int object ) {
		if( !m_objects.containsKey( new Integer( object ) ) )
			return false;
		String status = (String)m_objects.get( new Integer( object ) );
		if( status.equals("+") ) return true;
		else return false;
	}
	
	public boolean objectShown( int playerId, int object ) {
		if( playerId < 0 ) return false;
		if( !m_privateObjects.containsKey( new Integer( playerId ) ) ) return false;
		HashMap playerObj = (HashMap)m_privateObjects.get( new Integer( playerId ) );
		String status = (String)playerObj.get( new Integer( object ) );
		if( status.equals("+") ) return true;
		else return false;
	}
	
	public boolean toSet() { return !m_unsetObjects.isEmpty(); }
	
	public boolean toSet( int playerId ) {
		if( playerId < 0 ) return false;
		if( !m_privateUnsetObjects.containsKey( new Integer( playerId ) ) ) return false;
		HashMap playerObj = (HashMap)m_privateUnsetObjects.get( new Integer( playerId ) );
		return !playerObj.isEmpty();
	}
	
	public String getObjects() {
		String theseObjects = " ";
		Iterator it = m_unsetObjects.keySet().iterator();
		while( it.hasNext() ) {
			//Gets object and on/off value
			int x = ((Integer)it.next()).intValue();
			String s = (String)m_unsetObjects.get( new Integer( x ) );
			theseObjects += s + x + ",";
			m_objects.put( new Integer( x ), s );
		}
		m_unsetObjects.clear();
		return theseObjects;
	}
	
	public String getObjects( int playerId ) {
		if( playerId < 0 ) return "";
		if( !m_privateObjects.containsKey( new Integer( playerId ) ) ) return "";
		
		HashMap playerMap = (HashMap)m_privateUnsetObjects.get( new Integer( playerId ) );
		HashMap playerObj = (HashMap)m_privateObjects.get( new Integer( playerId ) );
		String theseObjects = " ";
		Iterator it = playerMap.keySet().iterator();
		while( it.hasNext() ) {
			//Gets object and on/off value
			int x = ((Integer)it.next()).intValue();
			String s = (String)playerMap.get( new Integer( x ) );
			theseObjects += s + x + ",";
			playerObj.put( new Integer( x ), s );
		}
		playerMap.clear();
		return theseObjects;
	}
}