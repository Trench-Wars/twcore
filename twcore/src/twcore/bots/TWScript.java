package twcore.bots;

import java.sql.ResultSet;
import java.util.TreeMap;
import java.util.Iterator;

import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.OperatorList;

/**
 * @author milosh
 */
public class TWScript extends MultiUtil {
    
    public OperatorList opList;
    public String database = "website";
    public static final int LOWER_STAFF = 0;
    public static final int SMOD_LEVEL = 1;
    public static final int SYSOP_LEVEL = 2;
    public int ACCESS_LEVEL = 0;
    
    public TreeMap<String, String> variables = new TreeMap<String, String>();
    
    /**
     * Initializes.
     */
    public void init() {
        opList = m_botAction.getOperatorList();
    }
    
    /**
     * Required method that returns this help menu.
     */
    public String[] getHelpMessages() {
        String[] message = {
        		"+========================== TWSCRIPT ==========================+",
        		"| !setup              - Loads the default setup for this arena.|",
        		"| !mysetup            - Loads your personal setup.             |",
        		"| !setup <name>       - Loads <name>'s personal setup.         |",
        		"| !addvar <n>:<v>     - Adds a variable named <n> of value <v>.|",
        		"| !setvar <n>:<v>     - Sets variable <n> to value <v>.        |",
        		"| !removevar <n>      - Removes variable <n>.                  |",
        		"| !listvar            - Lists the current variables.           |",
        		"| !listkeys           - Lists private TWScript escape keys.    |",
        		"| !listpubkeys        - Lists public TWScript escape keys.     |",
        		"| !smodlogin          - Log in for Smods.                      |",
        		"| !sysoplogin         - Log in for Sysops.                     |",
        		"+==============================================================+"
        };        
        return message;
    }
    
    /**
     * Gets a help message of all replacement keys
     * @see twcore.core.util.CodeCompiler.replaceKeys()
     * @return - A help message displaying key types.
     */
    public static String[] getPrivateKeysMessage(){
        String msg[] = {
                "+================ Private Escape Keys ================+",
                "| @name             - The player's name.              |",
                "| @wins             - The player's wins.              |",
                "| @losses           - The player's losses.            |",
                "| @frequency        - The player's frequency.         |",
                "| @id               - The player's id(not userid)     |",
                "| @botname          - The bot's name.                 |",
                "| @shipnum          - The player's ship number.       |",
                "| @shipname         - The player's ship.              |",
                "| @shipslang        - Player's ship in vernacular.    |",
                "| @arenaname        - The arena's name.               |",
                "| @arenasize        - Number of players in arena.     |",
                "| @playingplayers   - Number of players in a ship.    |",
                "| @freqsize(#)      - Number of players on freq       |",
                "| @pfreqsize(#)     - Num. of players playing on freq |",
                "| @shipsonfreq(#,#) - Num of players in a certain ship|",
                "|                       on freq. (ship type, freq #)  |",
                "| @squad            - The player's squad.             |",
                "| @bounty           - The player's bounty.            |",
                "| @kpoints          - Points earned by kills.         |",
                "| @fpoints          - Points earned by flags.         |",
                "| @points           - The sum of kpoints and fpoints. |",
                "| @x                - X Location(Tiles)               |",
                "| @y                - Y Location(Tiles)               |",
                "| @randomfreq       - A random number(0 - 9998)       |",        
                "| @randomship       - A random number(1-8)            |",            
                "| @randomtile       - A random number(1-1022)         |",
                "| @randomsound      - A random ALLOWED sound number.  |",
                "| @randomplayer     - A random player in the arena.   |",
                "| @ping             - The player's ping in ms.        |",
                "| @date             - The current date.               |",
                "| @time             - The current time.               |",
                "| @!command@@       - Issues a command to the bot, but|",
                "|                      the player receives no message.|",
                "+=====================================================+",
            };
            return msg;
    }
    
    public static String[] getPublicKeysMessage(){
        String msg[] = {
                "+================= Public Escape Keys ================+",
                "| @botname          - The bot's name.                 |",
                "| @arenaname        - The arena's name.               |",
                "| @arenasize        - Number of players in arena.     |",
                "| @playingplayers   - Number of players in a ship.    |",
                "| @freqsize(#)      - Number of players on freq #.    |",
                "| @pfreqsize(#)     - Num. of players in ship. Freq # |",
                "| @shipsonfreq(#,#) - Num of players in a certain ship|",
                "|                       on freq. (ship type, freq #)  |",
                "| @randomfreq       - A random number(0 - 9998)       |",        
                "| @randomship       - A random number(1-8)            |",            
                "| @randomtile       - A random number(1-1022)         |",
                "| @randomsound      - A random ALLOWED sound number.  |",
                "| @randomplayer     - A random player in the arena.   |",
                "| @date             - The current date.               |",
                "| @time             - The current time.               |",
                "+=====================================================+",
            };
            return msg;
    }
    
    /**
     * Handles messaging.
     */
    public void handleEvent(Message event) {
        String message = event.getMessage();
        String name = m_botAction.getPlayerName(event.getPlayerID());
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if (name == null || p == null)
            return;
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && opList.isER(name))
            handleCommand(name, message);
    }
    
    /**
     * Handles commands.
     */
    public void handleCommand(String name, String cmd) {
    	if (cmd.equalsIgnoreCase("!setup"))
    		doArenaSetup(name, "default");
    	if (cmd.startsWith("!setup "))
    		doArenaSetup(name, cmd.substring(7));
    	if (cmd.equalsIgnoreCase("!mysetup"))
    		doArenaSetup(name, name);
    	if (cmd.startsWith("!addvar "))
    		doAddVar(name, cmd.substring(8));
    	if (cmd.startsWith("!setvar "))
    		doSetVar(name, cmd.substring(8));
    	if (cmd.startsWith("!removevar "))
    		doRemoveVar(name, cmd.substring(11));
    	if (cmd.equalsIgnoreCase("!listvar"))
    		doListVar(name);
        if (cmd.equalsIgnoreCase("!listkeys"))
        	m_botAction.smartPrivateMessageSpam( name, getPrivateKeysMessage());
        if (cmd.equalsIgnoreCase("!listpubkeys"))
        	m_botAction.smartPrivateMessageSpam(name, getPublicKeysMessage());
        if (cmd.equalsIgnoreCase("!sysoplogin"))
            doSysopOverride(name);
        if (cmd.equalsIgnoreCase("!smodlogin"))
            doSmodOverride(name);
        if (cmd.equals("Sysop locking has been disabled."))
        	m_botAction.sendSmartPrivateMessage( m_botAction.getBotName(), "!sysoplock");
        if (cmd.equals("Smod locking has been disabled."))
        	m_botAction.sendSmartPrivateMessage( m_botAction.getBotName(), "!smodlock");
    }
    
    /**
     * Handles arena setups by querying the database and having the bot PM itself with the commands.
     */
    public void doArenaSetup(String name, String message){
        try {
            ResultSet resultSet = m_botAction.SQLQuery(database, 
                    "SELECT S.fcMessage " + "FROM `tblArena` A, `tblArenaSetup` S "
                    + "WHERE A.fnArenaID = S.fnArenaID "
                    + "AND S.fcName = '" + message + "' "
                    + "AND A.fcArenaName = '"
                    + m_botAction.getArenaName() + "'");
            if(resultSet.next()) {
                String msg = resultSet.getString("fcMessage");
                String[] msgs = msg.split("\r\n|\r|\n");
                m_botAction.smartPrivateMessageSpam(m_botAction.getBotName(),msgs);
                m_botAction.sendSmartPrivateMessage( name, "Setup complete.");
            }
            else
            	m_botAction.sendSmartPrivateMessage( name, "Setup failed; Could not locate setup.");
            m_botAction.SQLClose(resultSet);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }
    
    /**
	 * Adds a variable.
	 */
    public void doAddVar(String name, String message){
    	String[] msgs = message.split(":");
    	if(msgs.length != 2){
    		m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !addvar x:1");
    		return;
    	}
    	if(this.variables.containsKey(msgs[0])){
    		m_botAction.sendSmartPrivateMessage( name, "The variable '" + msgs[0] + "' already exists. Use !setvar <name>:<value> to change its value.");
    		return;
    	}
    	this.variables.put(msgs[0], msgs[1]);
    	m_botAction.sendSmartPrivateMessage( name, "Variable '" + msgs[0] + "' has been added.");
    }
    
    /**
	 * Sets a variable to a certain value.
	 */
    public void doSetVar(String name, String message){
    	String[] msgs = message.split(":");
    	if(msgs.length != 2){
    		m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !setvar x:1");
    		return;
    	}
    	if(!this.variables.containsKey(msgs[0])){
    		m_botAction.sendSmartPrivateMessage( name, "The variable '" + msgs[0] + "' does not exist. Use !addvar <name>:<type> to create it.");
    		return;
    	}
    	this.variables.remove(msgs[0]);
    	this.variables.put(msgs[0], msgs[1]);
    	m_botAction.sendSmartPrivateMessage( name, "Variable '" + msgs[0] + "' has been set to '" + msgs[1] + "'.");
    }
    
    /**
	 * Removes a variable.
	 */
	public void doRemoveVar(String name, String message){
		if(!this.variables.containsKey(message)){
    		m_botAction.sendSmartPrivateMessage( name, "The variable '" + message + "' does not exist. Use !addvar <name>:<type> to create it.");
    		return;
    	}
    	this.variables.remove(message);
    	m_botAction.sendSmartPrivateMessage( name, "Variable '" + message + "' has been removed.");
	}
	
	/**
	 * Lists the current variables.
	 */
	public void doListVar(String name){
		if(this.variables.isEmpty()){
			m_botAction.sendSmartPrivateMessage( name, "There are no variables to list!");
			return;
		}
		m_botAction.sendSmartPrivateMessage( name, "=========== TWScript variables ===========");
		Iterator<String> i = this.variables.keySet().iterator();
		while( i.hasNext() ){
			String n = i.next();
			String v = this.variables.get(n);
			m_botAction.sendSmartPrivateMessage( name, "| " + n + " : " + v);
		}
	}
    
    /**
     * Toggles Sysop override.
     */
    public void doSysopOverride(String name){
        if(opList.isSysop(name) && !name.equalsIgnoreCase(m_botAction.getBotName())){
            if(ACCESS_LEVEL == SYSOP_LEVEL){
                ACCESS_LEVEL = 0;
                m_botAction.sendSmartPrivateMessage( name, "Sysop override deactivated.");
            } else {
                ACCESS_LEVEL = SYSOP_LEVEL;
                m_botAction.sendSmartPrivateMessage( name, "Sysop override activated.");
                m_botAction.sendSmartPrivateMessage( m_botAction.getBotName(), "!sysoplock");
            }
        }else
            m_botAction.sendSmartPrivateMessage( name, "Only System Operators can use this command.");
    }
    
    /**
     * Toggles Smod override.
     */
    public void doSmodOverride(String name){
        if(opList.isSmod(name) && !name.equalsIgnoreCase(m_botAction.getBotName())){
            if(ACCESS_LEVEL == SMOD_LEVEL){
                ACCESS_LEVEL = 0;
                m_botAction.sendSmartPrivateMessage( name, "Smod override deactivated.");
            } else {
                ACCESS_LEVEL = SMOD_LEVEL;
                m_botAction.sendSmartPrivateMessage( name, "Smod override activated.");
                m_botAction.sendSmartPrivateMessage( m_botAction.getBotName(), "!smodlock");
            }
        }else
            m_botAction.sendSmartPrivateMessage( name, "Only Super Moderators can use this command.");
    }
    
    public void cancel() {}
    public boolean isUnloadable(){ return true; }
    public void requestEvents(ModuleEventRequester modEventReq) {}
    
}

