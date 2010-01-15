package twcore.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import twcore.core.util.Tools;

/**
 * TODO: This needs updating
 *
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
 * 1 - Bot              used for determining if player is a bot          [moderate.txt]
 * 2 - Outsider         limited privileges; for non-staff coders         [outsider.cfg]
 * 3 - Event Ref        regular privileges; first stage of real access   [moderate.txt]
 * 4 - Moderator        expanded privileges for administrative duties    [moderate.txt]
 * 5 - High Moderator   additional privileges normally only for smods    [highmod.cfg ]
 * 6 - Developer        additional privileges; for staff coders only     [develop.cfg ]
 * 7 - Super Moderator  nearly all privileges                            [smod.txt    ]
 * 8 - Sysop            nearly all privileges (used to test if a bot)    [sysop.txt   ]
 * 9 - Owner            all privileges                                   [owner.cfg   ]
 * </pre></code>
 */
public class OperatorList {

	public static final int PLAYER_LEVEL = 0;
    public static final int BOT_LEVEL = 1;
    public static final int OUTSIDER_LEVEL = 2;
    public static final int ER_LEVEL = 3;
    public static final int MODERATOR_LEVEL = 4;
    public static final int HIGHMOD_LEVEL = 5;
    public static final int DEV_LEVEL = 6;
    public static final int SMOD_LEVEL = 7;
    public static final int SYSOP_LEVEL = 8;
    public static final int OWNER_LEVEL = 9;

    /**
     * This Hashmap contains all the operators
     * Key:   Name of the operator [lowercase]
     * Value: level id (0-9)
     */
    private static Map<String,Integer> operators;

    /**
     * autoAssign Hashmap contains the automatic assignment rules specified in operators.cfg
     * Key:   level id (0-9)
     * Value: Exact value from operators.cfg
     */
    private static Map<Integer,String> autoAssign;
    
    private static OperatorList operatorList;


    /**
     * Private constructor. Use OperatorList.getInstance() instead.
     */
    private OperatorList(){
        operators = Collections.synchronizedMap( new LinkedHashMap<String,Integer>() );
        autoAssign = Collections.synchronizedMap( new LinkedHashMap<Integer,String>() );
    }
    
    /**
     * Singleton pattern; always only creates one instance of OperatorList.
     * This method is synchronized so multiple threads can't both get a different OperatorList instance if they call this method at the same time.
     * 
     * @return OperatorList instance
     */
    public static synchronized OperatorList getInstance() {
    	if(operatorList == null) {
    		operatorList = new OperatorList();
    	}
    	return operatorList;
    }

    /**
     * Initializes this OperatorList by loading the operators.cfg configuration file
     */
    public void init(File operatorsCfg) throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        FileInputStream fileInput = new FileInputStream(operatorsCfg);
        prop.load(fileInput);

        // temporary map for reading out the configuration
        String[] operators_keys = {
                 "level_player", "level_bot", "level_outsider", "level_er", "level_moderator",
                 "level_highmod", "level_dev", "level_smod", "level_sysop", "level_owner"
        };
        String[] auto_assign_keys = {
                "assign_player", "assign_bot", "assign_outsider", "assign_er", "assign_moderator",
                "assign_highmod", "assign_dev", "assign_smod", "assign_sysop", "assign_owner"
        };


        // Operators
        for(int i = operators_keys.length-1 ; i >= 0 ; i--) {
            String key = operators_keys[i];

            if(prop.containsKey(key)) {
                String value = prop.getProperty(key);

                if(value.trim().length() > 0) {
                    StringTokenizer tokens = new StringTokenizer(value,",");
                    while(tokens.hasMoreTokens()) {
                        operators.put( tokens.nextToken().trim().toLowerCase(), i );
                    }
                }
            }
        }

        // Auto-assignment
        for(int i = auto_assign_keys.length-1 ; i >= 0 ; i--) {
            String key = auto_assign_keys[i];

            if(prop.containsKey(key)) {
                String value = prop.getProperty(key).trim();

                if(value.trim().length() > 0) {
                    autoAssign.put(i, value);
                }
            }
        }
        
        fileInput.close();
    }

    /**
     * Carries out auto-assignment of operators using the operators on the specified file
     *
     * @param data one of moderate.txt, smod.txt or sysop.txt
     */
    public void autoAssignFile(File data) {
        // 1. Cycle the autoAssign hashmap
        //

        for(int level:autoAssign.keySet()) {
            String autoAssignSetting = autoAssign.get(level);

            // If not defined, do nothing for this level
            if(autoAssignSetting == null) {
                continue;
            }

            // If the auto assign setting starts with "moderate.txt" / "smod.txt" / "sysop.txt"
            if(autoAssignSetting.startsWith(data.getName()) ) {
                // Load operators from this file into this level
                String autoAssignSetting2 = null;
                if(autoAssignSetting.contains(":")) {
                    autoAssignSetting2 = autoAssignSetting.substring(autoAssignSetting.indexOf(':'));
                }

                // Read through the file and add operators
                try {
                	FileReader reader = new FileReader(data);
                    BufferedReader buffer = new BufferedReader(reader);
                    String line = null, name = null;
                    boolean in_area = false;

                    while (( line = buffer.readLine()) != null) {

                        if( line.startsWith(" ") ||
                            line.startsWith("-") ||
                            line.startsWith("+") ||
                            line.startsWith("/") ||
                            line.startsWith("*") ||
                            line.trim().length() == 0)
                            continue;

                        name = line.trim().toLowerCase();

                        // Ignore bot names that are already on the operator list as bot level
                        if(operators.containsKey(name) && operators.get(name) == OperatorList.BOT_LEVEL) {
                            continue;
                        }

                        // if we're not using auto-assign options "tag" or "line"
                        if(autoAssignSetting2 == null || autoAssignSetting2.replace(":", "").length() == 0) {
                            if(operators.containsKey(name) && operators.get(name) >= level) {
                                continue;
                            } else {
                                operators.put(name, level);
                            }
                        }

                        // If an operator is added below and he is already known in the operators map, he will
                        // be overwritten. Because operators are read in from highest (owner) to lowest (ZH),
                        // everyone will get the correct level
                        if(autoAssignSetting2 != null && autoAssignSetting2.startsWith(":tag ") && autoAssignSetting2.length() > 6) {
                            if(name.contains(autoAssignSetting2.toLowerCase().substring(5))) {
                                operators.put(name, level);
                            }
                        }
                        if(autoAssignSetting2 != null && autoAssignSetting2.startsWith(":line")) {
                            String[] delimiters = autoAssignSetting2.substring(7).split("\" - \""); // cut off :line " and split by " - "

                            if(line.trim().toLowerCase().startsWith(delimiters[0].toLowerCase())) { // start area
                                in_area = true;
                                continue;
                            }
                            if(line.trim().toLowerCase().startsWith(delimiters[1].toLowerCase())) { // end area
                                in_area = false;
                                continue;
                            }
                            if(in_area) {
                                operators.put(name,level);
                            } else {
                                continue;
                            }
                        }
                    }
                    
                    buffer.close();
                    reader.close();

                } catch(FileNotFoundException fnfe) {

                } catch(IOException ioe) {

                }


                // Get a quick count of added operators
                int count = 0;
                for(Integer l:operators.values()) {
                    if(l == level)
                        count++;
                }

                Tools.printLog( "Added "+count+" operators to level "+Tools.staffNameShort(level)+" from file "+data.getName());
            }
        }
    }



    /**
     * @return The entire access mapping of player names to access levels
     */
    public Map<String, Integer> getList() {
        return operators;
    }

    /**
     * Given a name, return the access level associated.  If none is found,
     * return 0 (normal player).
     * @param name Name in question
     * @return Access level of the name provided
     */
    public int getAccessLevel( String name ){
        if( name == null ){
            return 0;
        }

        Integer accessLevel = operators.get( name.trim().toLowerCase() );
        if( accessLevel == null ){
            return PLAYER_LEVEL;
        } else {
            return accessLevel.intValue();
        }
    }

    /**
     * Checks if a given name has at least bot operator level status.
     * Bots automatically make themselves the bot operator level after logging in.
     * @param name Name in question
     * @return True if player has at least the bot operator level status
     */
    public boolean isBot( String name ){

        if( getAccessLevel( name ) >= BOT_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if a given name has bot operator level status.
     * Bots automatically make themselves the bot operator level after logging in.
     *
     * @param name Name in question
     * @return True if player has the bot operator level status
     */
    public boolean isBotExact( String name ){

        if( getAccessLevel( name ) == BOT_LEVEL ){
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
     * Check if a given name is at least of Developer status.
     * @param name Name in question
     * @return True if player is at least an ER
     */
    public boolean isDeveloper( String name ){

        if( getAccessLevel( name ) >= DEV_LEVEL ){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a given name is a Developer.
     * @param name Name in question
     * @return True if player is a Developer
     */
    public boolean isDeveloperExact( String name ){

        if( getAccessLevel( name ) == DEV_LEVEL ){
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
    @Deprecated
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
        if( accessLevel < PLAYER_LEVEL || accessLevel > OWNER_LEVEL )
            return null;


        HashSet<String> gathered = new HashSet<String>();

        for(Entry<String,Integer> operator:operators.entrySet()) {
            if(operator.getValue().intValue() == accessLevel) {
                gathered.add(operator.getKey());
            }
        }

        return gathered;
    }

    /**
     * Manually adds an operator to the access list.  For special use only.
     * (Not needed in any normal procedure.)
     * @param name Name to add
     * @param accessLevel Access level at which to add the name
     */
    public void addOperator( String name, int accessLevel ) {
        if( accessLevel < PLAYER_LEVEL || accessLevel > OWNER_LEVEL )
            return;
        operators.put(name.toLowerCase(), accessLevel);
    }

    /**
     * Clears the access list.
     */
    public void clear(){

        // Custom clean method of operators
        // Leave the bot operator list entries intact
        Set<Entry<String, Integer>> operatorNames = operators.entrySet();

        synchronized(operators) {
            Iterator<Entry<String, Integer>> it = operatorNames.iterator(); // Must be in synchronized block
            while (it.hasNext()) {
                Entry<String, Integer> operator = it.next();

                if(operator.getValue() != OperatorList.BOT_LEVEL) {
                    it.remove();
                }
            }
        }

        autoAssign.clear();
    }

    /**
     * Retrieve names based on numeric access level.
     * @param accessLevel
     * @return
     */
    public String getAccessLevelName( int accessLevel ) {
        switch( accessLevel ) {
            case 0: return "Player [lvl " + accessLevel + "]";
            case 1: return "Bot [lvl " + accessLevel + "]";
            case 2: return "Outsider [lvl " + accessLevel + "]";
            case 3: return "ER [lvl " + accessLevel + "]";
            case 4: return "Mod [lvl " + accessLevel + "]";
            case 5: return "HighMod [lvl " + accessLevel + "]";
            case 6: return "Developer [lvl " + accessLevel + "]";
            case 7: return "SMod [lvl " + accessLevel + "]";
            case 8: return "Sysop [lvl " + accessLevel + "]";
            case 9: return "Owner [lvl " + accessLevel + "]";
            default: return "Unknown access level! [lvl " + accessLevel + "]";
        }
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException  {
    	// Prevent cloning of this object as it's a Singleton.
    	throw new CloneNotSupportedException(); 
    }
}
