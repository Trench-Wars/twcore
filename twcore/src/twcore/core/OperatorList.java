package twcore.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import twcore.core.util.Tools;

/**
 * Stores the access list as read from the server-based files moderate.txt, smod.txt,
 * and sysop.txt, and the bot core config files owner.cfg, outsider.cfg, and
 * highmod.cfg.  Is able to answer access-specific queries based on the information
 * gathered.
 * <p>
 * Access levels
 * <p><code><pre>
 * #   Title            Description                                      Read from
 *
 * 0 - Normal player    no special privileges
 * 1 - Zone Helper      extremely limited privileges                     [moderate.txt]
 * 2 - Outsider         limited privileges; for non-staff coders         [outsider.cfg]
 * 3 - Event Ref        regular privileges; first stage of real access   [moderate.txt]
 * 4 - Developer		regular privileges; for more trusted coders      [develop.cfg ]
 * 4 - Moderator        expanded privileges for administrative duties    [moderate.txt]
 * 5 - High Moderator   additional privileges normally only for smods    [highmod.cfg ]
 * 6 - Super Moderator  nearly all privileges                            [smod.txt    ]
 * 7 - Sysop            nearly all privileges (used to test if a bot)    [sysop.txt   ]
 * 8 - Owner            all privileges                                   [owner.cfg   ]
 * </pre></code>
 */
public class OperatorList {

    Map             m_accessList;

    public static final int PLAYER_LEVEL = 0;
    public static final int ZH_LEVEL = 1;
    public static final int OUTSIDER_LEVEL = 2;
    public static final int ER_LEVEL = 3;
    public static final int DEV_LEVEL = 4;
    public static final int MODERATOR_LEVEL = 5;
    public static final int HIGHMOD_LEVEL = 6;
    public static final int SMOD_LEVEL = 7;
    public static final int SYSOP_LEVEL = 8;
    public static final int OWNER_LEVEL = 9;

    /**
     * Creates a new instance of OperatorList.
     */
    public OperatorList(){

        m_accessList = Collections.synchronizedMap( new HashMap() );
    }

    public OperatorList(OperatorList o) {
    	m_accessList = Collections.synchronizedMap( new HashMap() );
    	m_accessList.putAll(o.m_accessList);
    }

    /**
     * @return The entire access mapping of player names to access levels
     */
    public Map getList() {
        return m_accessList;
    }

    /**
     * Given a name, return the access level associated.  If none is found,
     * return 0 (normal player).
     * @param name Name in question
     * @return Access level of the name provided
     */
    public int getAccessLevel( String name ){
        Integer      accessLevel;

        if( name == null ){
            return PLAYER_LEVEL;
        }

        accessLevel = (Integer)m_accessList.get( name.trim().toLowerCase() );
        if( accessLevel == null ){
            return PLAYER_LEVEL;
        } else {
            return accessLevel.intValue();
        }
    }

    /**
     * Check if a given name is at least of ZH status.
     * @param name Name in question
     * @return True if player is at least a ZH
     */
    public boolean isZH( String name ){

        if( getAccessLevel( name ) >= ZH_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is a ZH.
     * @param name Name in question
     * @return True if player is a ZH
     */
    public boolean isZHExact( String name ){

        if( getAccessLevel( name ) == ZH_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is at least of Outsider status.
     * NOTE: Outsider is a special status provided to coders who are not members
     * of staff.  They are able to use some bot powers that ZHs can't, but can't
     * generally use event bots.
     * FIXME: If an Outsider also is on moderate.txt with arena permissions, they
     * will be considered a full moderator.
     * @param name Name in question
     * @return True if player is at least an Outsider
     */
    public boolean isOutsider( String name ){

        if( getAccessLevel( name ) >= OUTSIDER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is an Outsider.
     * @param name Name in question
     * @return True if player is a Outsider
     */
    public boolean isOutsiderExact( String name ){

        if( getAccessLevel( name ) == OUTSIDER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }


    /**
     * Check if a given name is at least of ER status.
     * @param name Name in question
     * @return True if player is at least an ER
     */
    public boolean isER( String name ){

        if( getAccessLevel( name ) >= ER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is an ER.
     * @param name Name in question
     * @return True if player is an ER
     */
    public boolean isERExact( String name ){

        if( getAccessLevel( name ) == ER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is at least of Mod status.
     * @param name Name in question
     * @return True if player is at least a Mod
     */
    public boolean isModerator( String name ){

        if( getAccessLevel( name ) >= MODERATOR_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is a Mod.
     * @param name Name in question
     * @return True if player is a Mod
     */
    public boolean isModeratorExact( String name ){

        if( getAccessLevel( name ) == MODERATOR_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is at least of HighMod status.
     * NOTE: HighMod is a special status given to experienced mods, allowing them
     * access to certain features that are normally only allowed to SMod+.  Usually
     * they are league ops or hold another important position requiring this status.
     * @param name Name in question
     * @return True if player is at least a HighMod
     */
    public boolean isHighmod( String name ){

        if( getAccessLevel( name ) >= HIGHMOD_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is a HighMod.
     * @param name Name in question
     * @return True if player is a HighMod
     */
    public boolean isHighmodExact( String name ){

        if( getAccessLevel( name ) == HIGHMOD_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is at least of SMod status.
     * @param name Name in question
     * @return True if player is at least a SMod
     */
    public boolean isSmod( String name ){

        if( getAccessLevel( name ) >= SMOD_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is a SMod.
     * @param name Name in question
     * @return True if player is a SMod.
     */
    public boolean isSmodExact( String name ){

        if( getAccessLevel( name ) == SMOD_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is at least of Sysop status.
     * @param name Name in question
     * @return True if player is at least a Sysop
     */
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

    /**
     * Check if a given name is an owner.
     * @param name Name in question
     * @return True if player is an owner
     */
    public boolean isOwner( String name ){

        if( getAccessLevel( name ) >= OWNER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * (REDUNDANT) Check if a given name is an owner.
     * @param name Name in question
     * @return True if player is an owner
     * @deprecated Exactly the same functionality as isOwner, as no higher access level exists.
     */
    public boolean isOwnerExact( String name ){

        if( getAccessLevel( name ) == OWNER_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Given an access level, returns all players who match that access level.
     * @param accessLevel A number corresponding to the OperatorList access standard
     * @return HashSet of all players of that access level.
     */
    public HashSet<String> getAllOfAccessLevel( int accessLevel ) {
        if( accessLevel < ZH_LEVEL || accessLevel > OWNER_LEVEL )
            return null;

        HashSet<String> gathered = new HashSet<String>();
        Iterator i = m_accessList.keySet().iterator();
        String player;

        while( i.hasNext() ) {
            player = (String)i.next();
            if( player != null )
                if( getAccessLevel( player ) == accessLevel )
                    gathered.add( player );
        }

        return gathered;
    }

    /**
     * Changes all players with a supplied strange pattern inside their name to
     * a given access level.
     * @param pattern The pattern that must be contained in the player name
     * @param accessLevel A number corresponding to the OperatorList access standard to change to
     */
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

    /**
     * Wrapper method for parseFile(File, int).
     *
     * Parses an access list and sets all members on the list to a given access
     * level.  Used in conjunction with the changeAllMatches method (using ER
     * and ZH tags), it can successfully assign access levels to all individuals.
     * NOTE: If someone in outsider.cfg is also on moderate.txt, and they don't
     * have a tag of some kind, they will be set to moderator level.
     *
     * @param filename Filename, in String form, to parse
     * @param accessLevel Access level to assign to
     */
    public void parseFile( String filename, int accessLevel ){
        parseFile( new File( filename ), accessLevel );
    }

    /**
     * Parses an access list and sets all members on the list to a given access
     * level.  Used in conjunction with the changeAllMatches method (using ER
     * and ZH tags), it can successfully assign access levels to all individuals.
     * NOTE: If someone in outsider.cfg is also on moderate.txt, and they don't
     * have a tag of some kind, they will be set to moderator level.
     *
     * @param file Filename, in String form, to parse
     * @param accessLevel Access level to assign to
     */
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

    /**
     * Clears the access list.
     */
    void clearList(){

        m_accessList.clear();
    }
}
