package twcore.core.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import twcore.core.util.ByteArray;
import twcore.core.util.Tools;

/*
47 - Arena list
  
Field   Length  Description
0       1       Type byte
The following are repeated until the end of the message
1       ...\0   Arena name
?       2       Arena population
*/

public class ArenaList extends SubspaceEvent {
    Map<String, Integer>            m_arenaList;

    public ArenaList( ByteArray array ){
        int         i = 1;
        String      name;
        int         size;

        m_arenaList = Collections.synchronizedMap( new HashMap<String, Integer>() );

        try {
            while( i < array.size() ){
                name = array.readNullTerminatedString( i );
                i += name.length();
                i += 1; // For the terminating null
                size = array.readLittleEndianShort( i );
                size = Math.abs(size);  // negative to positive
                i += 2; // For the size
                m_arenaList.put( name.toLowerCase(), size );
            }
        } catch(ArrayIndexOutOfBoundsException aioobe) {
            Tools.printLog("ArrayIndexOutOfBoundsException occurred while interpreting ArenaList packet ("+aioobe.getMessage()+"):");
            Tools.printLog("Hex format:");
            array.show();
            Tools.printLog("String format:" + array.toString());
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
        return m_arenaList.get( arenaName.toLowerCase() );
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

