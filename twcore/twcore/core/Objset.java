package twcore.core;

import java.util.*;

/**
 * A useful class for keeping track of and formatting *objset commands.  Stores which
 * objects have been set and to which state (shown "+" or hidden "-").  Can also store
 * and process *objset strings for private sets.
 * 
 * Note that the *objset command and the Objset class are referring to LVZ objects,
 * and not Objects in the Java sense.  The objects will be defined inside the LVZ
 * file itself.  For the purposes of this class, all the programmer needs to know
 * is which index corresponds to which LVZ object.  For more information on the LVZ
 * format, download the LVZ Toolkit, or try the link below:
 * 
 *    http://www.kolumbus.fi/sakari.aura/contmapdevguide.html
 */
public class Objset {
    Map m_objects;              // Objects that have already had their show/hide *objset.  
                                // (Integer)Obj# -> (String) +/- [shown or hidden]
    Map m_unsetObjects;         // Objects that have not yet had their show/hide *objset.
                                // (Integer)Obj# -> (String) +/- [shown or hidden]
    Map m_privateObjects;       // Objects for a specific player already *objset
                                // (Integer)PlayerID -> ((Integer)Obj# -> (String) +/-)
    Map m_privateUnsetObjects;  // Objects for a specific player not yet *objset
                                // (Integer)PlayerID -> ((Integer)Obj# -> (String) +/-)

    /**
     * Creates a new instantiation of Objset.  For use with LVZ object defines.
     */
    public Objset() {
        m_objects = Collections.synchronizedMap(new HashMap());
        m_unsetObjects = Collections.synchronizedMap(new HashMap());
        m_privateObjects = Collections.synchronizedMap(new HashMap());
        m_privateUnsetObjects = Collections.synchronizedMap(new HashMap());
    }

    /**
     * Make a given object visible for all players.
     * @param object Index of object to make visible 
     */
    public void showObject( int object ) {

        m_unsetObjects.put( new Integer( object ), "+" );
    }

    /**
     * Makes a given object visible for a specific player.
     * @param playerId ID of player to show object to
     * @param object Index of object to make visible
     */
    public void showObject( int playerId, int object ) {

        if( playerId < 0 ) return;
        if( !m_privateUnsetObjects.containsKey( new Integer( playerId ) ) )
            m_privateUnsetObjects.put( new Integer( playerId ), Collections.synchronizedMap(new HashMap()) );
        if( !m_privateObjects.containsKey( new Integer( playerId ) ) )
            m_privateObjects.put( new Integer( playerId ), Collections.synchronizedMap(new HashMap ()) );

        Map playerMap = (Map)m_privateUnsetObjects.get( new Integer( playerId ) );
        playerMap.put( new Integer( object ), "+" );
    }

    /**
     * Makes a given object invisible to all players.
     * @param object Index of object to make invisible 
     */
    public void hideObject( int object ) {

        if( m_objects.containsKey( new Integer( object ) ) ) {
            String status = (String)m_objects.get( new Integer( object ) );
            if( status.equals( "+" ) )
                m_unsetObjects.put( new Integer( object ), "-" );
        } else m_unsetObjects.put( new Integer( object ), "-" );
    }

    /**
     * Makes a given object invisible to a specific player.
     * @param playerId ID of player to hide object from
     * @param object Index of object to make invisible
     */
    public void hideObject( int playerId, int object ) {

        if( playerId < 0 ) return;
        if( !m_privateUnsetObjects.containsKey( new Integer( playerId ) ) )
            m_privateUnsetObjects.put( new Integer( playerId ), Collections.synchronizedMap(new HashMap()) );
        if( !m_privateObjects.containsKey( new Integer( playerId ) ) )
            m_privateObjects.put( new Integer( playerId ), Collections.synchronizedMap(new HashMap ()) );

        Map playerMap = (Map)m_privateUnsetObjects.get( new Integer( playerId ) );
        Map playerObj = (Map)m_privateObjects.get( new Integer( playerId ) );

        if( playerObj.containsKey( new Integer( object ) ) ) {
            String status = (String)playerObj.get( new Integer( object ) );
            if( status != null && status.equals( "+" ) )
                playerMap.put( new Integer( object ), "-" );
        } else playerMap.put( new Integer( object ), "-" );
    }

    /**
     * Hides all objects that have previously been set in this Objset.
     */
    public void hideAllObjects() {
        m_unsetObjects.clear();
        synchronized (m_objects) {
            Iterator it = m_objects.keySet().iterator();
            while( it.hasNext() ) {
                int x = ((Integer)it.next()).intValue();
                String status = (String)m_objects.get( new Integer( x ) );
                if( status != null && status.equals( "+" ) )
                    m_unsetObjects.put( new Integer( x ), "-" );
            }
        }
    }

    /**
     * Hides all objects that have previously been set in this Objset for the
     * specified player.
     * @param playerId ID of player to hide all objects from
     */
    public void hideAllObjects( int playerId ) {
        if( playerId < 0 ) return;
        if( !m_privateObjects.containsKey( new Integer( playerId ) ) ) return;

        Map playerUnset = (Map)m_privateUnsetObjects.get( new Integer( playerId ) );
        Map playerObj = (Map)m_privateObjects.get( new Integer( playerId ) );

        playerUnset.clear();
        synchronized (playerObj) {
            Iterator it = playerObj.keySet().iterator();
            while( it.hasNext() ) {
                int x = ((Integer)it.next()).intValue();
                String status = (String)playerObj.get( new Integer( x ) );
                if( status != null && status.equals( "+" ) )
                    playerUnset.put( new Integer( x ), "-" );
            }
        }
    }

    /**
     * Checks if a given object is currently visible to everyone.
     * @param object Index of object to check
     * @return True if the specified object is currently shown
     */
    public boolean objectShown( int object ) {
        if( !m_objects.containsKey( new Integer( object ) ) )
            return false;
        String status = (String)m_objects.get( new Integer( object ) );
        if( status != null && status.equals("+") ) return true;
        else return false;
    }

    /**
     * Checks if a given object is currently visible to a specific player.
     * @param playerId ID of the player to check
     * @param object Index of object to check
     * @return True if the specified object is currently visible
     */
    public boolean objectShown( int playerId, int object ) {
        if( playerId < 0 ) return false;
        if( !m_privateObjects.containsKey( new Integer( playerId ) ) ) return false;
        Map playerObj = (Map)m_privateObjects.get( new Integer( playerId ) );
        String status = (String)playerObj.get( new Integer( object ) );
        if( status != null && status.equals("+") ) return true;
        else return false;
    }

    /**
     * @return True if there are objects currently waiting to be set for all players.
     */
    public boolean toSet() {
        return !m_unsetObjects.isEmpty();
    }

    /**
     * Returns true if there are objects currently waiting to be set for a specific
     * player.
     * @param playerId ID of the player to check
     * @return True if there are objects waiting to be set for the specified player
     */
    public boolean toSet( int playerId ) {
        if( playerId < 0 ) return false;
        if( !m_privateUnsetObjects.containsKey( new Integer( playerId ) ) ) return false;
        Map playerObj = (Map)m_privateUnsetObjects.get( new Integer( playerId ) );
        return !playerObj.isEmpty();
    }

    /**
     * Returns a String of all public objects entered into Objset, parsed to work
     * with the *objset command, and then moves them from the "unset" objects list
     * to the "set" objects list. 
     * @return String of objects in the objset format
     */
    public String getObjects() {
        String theseObjects = " ";
        synchronized (m_unsetObjects) {
            Iterator it = m_unsetObjects.keySet().iterator();
            while( it.hasNext() ) {
                //Gets object and on/off value
                int x = ((Integer)it.next()).intValue();
                String s = (String)m_unsetObjects.get( new Integer( x ) );
                theseObjects += s + x + ",";
                m_objects.put( new Integer( x ), s );
            }
        }
        m_unsetObjects.clear();
        return theseObjects;
    }

    /**
     * Returns a String of all private unset objects for a specific player, parsed to
     * work with the *objset command, and then moves them from the "unset" objects list
     * to the "set" objects list. 
     * @param playerId
     * @return A String containing a list of objects and their show or hide values for use with *objset.
     */
    public String getObjects( int playerId ) {
        if( playerId < 0 ) return "";
        if( !m_privateObjects.containsKey( new Integer( playerId ) ) ) return "";

        Map playerMap = (Map)m_privateUnsetObjects.get( new Integer( playerId ) );
        Map playerObj = (Map)m_privateObjects.get( new Integer( playerId ) );
        String theseObjects = " ";
        synchronized (playerMap) {
            Iterator it = playerMap.keySet().iterator();
            while( it.hasNext() ) {
                //Gets object and on/off value
                int x = ((Integer)it.next()).intValue();
                String s = (String)playerMap.get( new Integer( x ) );
                theseObjects += s + x + ",";
                playerObj.put( new Integer( x ), s );
            }
        }
        playerMap.clear();
        return theseObjects;
    }
}