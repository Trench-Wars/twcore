package twcore.core;

import java.io.*;
import java.util.*;

public class OperatorList {

    Map             m_accessList;

    public static final int ZH_LEVEL = 1;
    public static final int OUTSIDER_LEVEL = 2;
    public static final int ER_LEVEL = 3;
    public static final int MODERATOR_LEVEL = 4;
    public static final int HIGHMOD_LEVEL = 5;
    public static final int SMOD_LEVEL = 6;
    public static final int SYSOP_LEVEL = 7;
    public static final int OWNER_LEVEL = 8;

    public OperatorList(){

        m_accessList = Collections.synchronizedMap( new HashMap() );
    }

    public Map getList() {
        return m_accessList;
    }

    int getAccessLevel( String name ){
        Integer      accessLevel;
        
        if( name == null ){
            return 0;
        }
        
        accessLevel = (Integer)m_accessList.get( name.trim().toLowerCase() );
        if( accessLevel == null ){
            return 0;
        } else {
            return accessLevel.intValue();
        }
    }

    public boolean isZH( String name ){

        if( getAccessLevel( name ) >= ZH_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    public boolean isZHExact( String name ){

        if( getAccessLevel( name ) == ZH_LEVEL ){
            return true;
        } else {
            return false;
        }
    }
    
    public boolean isOutsider( String name ){

        if( getAccessLevel( name ) >= OUTSIDER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    public boolean isOutsiderExact( String name ){

        if( getAccessLevel( name ) == OUTSIDER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }


    public boolean isER( String name ){

        if( getAccessLevel( name ) >= ER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    public boolean isERExact( String name ){

        if( getAccessLevel( name ) == ER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }
    
    public boolean isModerator( String name ){

        if( getAccessLevel( name ) >= MODERATOR_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    public boolean isModeratorExact( String name ){

        if( getAccessLevel( name ) == MODERATOR_LEVEL ){
            return true;
        } else {
            return false;
        }
    }
    
    public boolean isHighmod( String name ){

        if( getAccessLevel( name ) >= HIGHMOD_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    public boolean isHighmodExact( String name ){

        if( getAccessLevel( name ) == HIGHMOD_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    public boolean isSmod( String name ){

        if( getAccessLevel( name ) >= SMOD_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    public boolean isSmodExact( String name ){

        if( getAccessLevel( name ) == SMOD_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    public boolean isSysop( String name ){

        if( getAccessLevel( name ) >= SYSOP_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    public boolean isSysopExact( String name ){

        if( getAccessLevel( name ) == SYSOP_LEVEL ){
            return true;
        } else {
            return false;
        }
    }
    
    public boolean isOwner( String name ){

        if( getAccessLevel( name ) >= OWNER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    public boolean isOwnerExact( String name ){

        if( getAccessLevel( name ) == OWNER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    public void changeAllMatches( String pattern, int accessLevel ){
        Iterator        i;
        String          player;
        String          tempPattern;

        tempPattern = pattern.trim().toLowerCase();        
        for( i = m_accessList.keySet().iterator(); i.hasNext(); ){
            player = (String)i.next();
            if( player.indexOf( tempPattern ) != -1 ){
                m_accessList.put( player, new Integer( accessLevel ) );
            }            
        }
    }        

    public void parseFile( String filename, int accessLevel ){
        parseFile( new File( filename ), accessLevel );
    }
    public void parseFile( File file, int accessLevel ){

        String             name;
        String             inBuffer; 
        char               firstCharacter;
        LineNumberReader   lineReader;
        Integer            oldAccessLevel;

        try {
            lineReader = new LineNumberReader( new InputStreamReader( new FileInputStream( file ) ) );
            while( (inBuffer = lineReader.readLine()) != null ){
                if( inBuffer.trim().length() > 0 ){
                    firstCharacter = inBuffer.charAt( 0 );
                    if( firstCharacter != ' ' && firstCharacter != '-' && firstCharacter != '+' && firstCharacter != '/' ){
                        name = inBuffer.trim().toLowerCase();
                        oldAccessLevel = (Integer)m_accessList.get( name );
                        if( oldAccessLevel != null ){
                            if( oldAccessLevel.intValue() >= accessLevel ){
                                continue;
                            }
                        }
                        m_accessList.put( inBuffer.trim().toLowerCase(), new Integer( accessLevel ) );
                    }
                }
            }
            lineReader.close();
        } catch( Exception e ){
            Tools.printStackTrace( e );
        }  
    }

    void clearList(){

        m_accessList.clear();
    }
}
