package twcore.bots.multibot.twscript;

import java.sql.ResultSet;

import twcore.bots.MultiModule;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.AdaptiveClassLoader;
import twcore.core.OperatorList;

/**
 * @author milosh
 */
public class twscript extends MultiModule {
    
    public OperatorList opList;
    public String database = "website";
    public static boolean isSmod = false;
    
    /**
     * Initializes.
     */
    public void init() {
        opList = m_botAction.getOperatorList();
    }
    
    /**
     * Required method that returns this utilities help menu.
     */
    public String[] getModHelpMessage() {
        String[] message = {
        		"--- variable commands ---",
        		"!setup        -- sets up the arena",
                "!listkeys     -- lists keys",
                "!smodlogin    -- logs in smod"
        };        
        return message;
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
        	m_botAction.smartPrivateMessageSpam( name, CodeCompiler.getPrivateKeysMessage());
        if (cmd.equalsIgnoreCase("!smodlogin"))
            do_smodOverride(name);
    }
    
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
    
    public void do_smodOverride(String name){
        if(opList.isSmod(name)){
            if(isSmod){
                isSmod = false;
                m_botAction.sendSmartPrivateMessage( name, "Smod override deactivated.");
            } else {
                isSmod = true;
                m_botAction.sendSmartPrivateMessage( name, "Smod override activated.");
            }
        }else
            m_botAction.sendSmartPrivateMessage( name, "Only Super-Moderators can use this command.");
    }
    
    public void cancel() {
    	
    }
    public boolean isUnloadable(){ return true; }
    public void requestEvents(ModuleEventRequester modEventReq) {}
    
}
