package twcore.core.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import twcore.core.util.ByteArray;

/*
47 - Arena list
Field    Length    Description
0        1        Type byte

Repeats until end of packet:
1        ?        Arena name
?        1        \0
?        1        Arena size
?        1        \0
End of repeated section
*/

public class ArenaList extends SubspaceEvent {
    Map<String, Integer>            m_arenaList;

    public ArenaList( ByteArray array ){
        int         i = 1;
        String      name;
        int         size;

        m_arenaList = Collections.synchronizedMap( new HashMap<String, Integer>() );

        while( i < array.size() ){
            name = array.readNullTerminatedString( i );
            i += name.length();
            i += 1; // For the terminating null
            size = array.readByte( i );
            i += 1; // For the size
            i += 1; // For the terminating null
            m_arenaList.put( name.toLowerCase(), new Integer( size ) );
        }
    }

    public String[] getArenaNames(){
        String[] arena = new String[ m_arenaList.size() ];
        Iterator<String> i = m_arenaList.keySet().iterator();
        for( int x = 0; i.hasNext(); x++ ){
            arena[x] = (String)i.next();
        }
        return arena;
    }

    public int getSizeOfArena( String arenaName ){
        return Math.abs( m_arenaList.get( arenaName.toLowerCase() ).intValue() );
    }

    public String getCurrentArenaName(){
        Iterator<String> i = m_arenaList.keySet().iterator();
        while( i.hasNext() ){
            String key = (String)i.next();
            Integer value = m_arenaList.get( key );
            if( value.intValue() < 0 ){
                return key;
            }
        }
        return null;
    }
    public Map<String, Integer> getArenaList(){
        return m_arenaList;
    }
}

