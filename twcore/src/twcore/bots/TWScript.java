package twcore.bots;

import java.sql.ResultSet;

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
                "| @x                - X Location(Tiles)               |",
                "| @y                - Y Location(Tiles)               |",
                "| @randomfreq       - A random number(0 - 9998)       |",        
                "| @randomship       - A random number(1-8)            |",            
                "| @randomtile       - A random number(1-1022)         |",
                "| @randomsound      - A random ALLOWED sound number.  |",
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
                "                        on freq. (ship type, freq #)  |",
                "| @randomfreq       - A random number(0 - 9998)       |",        
                "| @randomship       - A random number(1-8)            |",            
                "| @randomtile       - A random number(1-1022)         |",
                "| @randomsound      - A random ALLOWED sound number.  |",
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
        if (cmd.equalsIgnoreCase("!listkeys"))
        	m_botAction.smartPrivateMessageSpam( name, getPrivateKeysMessage());
        if (cmd.equalsIgnoreCase("!listpubkeys"))
        	m_botAction.smartPrivateMessageSpam(name, getPublicKeysMessage());
        if (cmd.equalsIgnoreCase("!sysoplogin"))
            do_sysopOverride(name);
        if (cmd.equalsIgnoreCase("!smodlogin"))
            do_smodOverride(name);
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
     * Toggles Sysop override.
     */
    public void do_sysopOverride(String name){
        if(opList.isSysop(name) && !name.equalsIgnoreCase(m_botAction.getBotName())){
            if(ACCESS_LEVEL == SYSOP_LEVEL){
                ACCESS_LEVEL = 0;
                m_botAction.sendSmartPrivateMessage( name, "Sysop override deactivated.");
            } else {
                ACCESS_LEVEL = SYSOP_LEVEL;
                m_botAction.sendSmartPrivateMessage( name, "Sysop override activated.");
            }
        }else
            m_botAction.sendSmartPrivateMessage( name, "Only System Operators can use this command.");
    }
    
    /**
     * Toggles Smod override.
     */
    public void do_smodOverride(String name){
        if(opList.isSmod(name) && !name.equalsIgnoreCase(m_botAction.getBotName())){
            if(ACCESS_LEVEL == SMOD_LEVEL){
                ACCESS_LEVEL = 0;
                m_botAction.sendSmartPrivateMessage( name, "Smod override deactivated.");
            } else {
                ACCESS_LEVEL = SMOD_LEVEL;
                m_botAction.sendSmartPrivateMessage( name, "Smod override activated.");
            }
        }else
            m_botAction.sendSmartPrivateMessage( name, "Only Super Moderators can use this command.");
    }
    
    public void cancel() {
    	
    }
    public boolean isUnloadable(){ return true; }
    public void requestEvents(ModuleEventRequester modEventReq) {}
    
}
