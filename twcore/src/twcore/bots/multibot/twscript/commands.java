package twcore.bots.multibot.twscript;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.StringBuilder;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.OperatorList;

/**
 * @author milosh
 */
public class commands extends MultiUtil {
    
    public OperatorList opList;
    public TreeMap<String, CustomCommand> commands;
    
    /**
     * Initializes.
     */
    public void init() {
        opList = m_botAction.getOperatorList();
        commands = new TreeMap<String, CustomCommand>();
    }
    
    /**
     * Required method that returns this utilities help menu.
     */
    public String[] getHelpMessages() {
        String[] message = {
        "+------------------------------------ Commands ------------------------------------+",
        "| !addaction <!cmd> <action>  - Adds an <action> to command <!command>             |",
        "|                             - If <!command> does not exist it is created.        |",
        "| !removecmd <!cmd>           - Removes a custom command and all its actions.      |",
        "| !removecmd <!cmd> <index>   - Removes action at <index> from <!cmd>              |",
        "| !describe <!cmd> <text>     - Submits a description of <!cmd> that shows in help.|",
        "| !listcmd                    - Shows a list of custom commands and their actions. |",
        "+----------------------------------------------------------------------------------+"
        };        
        return message;
    }
    
    /**
     * Displays a customized help menu for commands created by this utility
     * 
     * @param name -
     *            The person requesting the help menu.
     */
    public void do_customHelp(String name) {
        if (commands.size() == 0)
            return;
        m_botAction.sendSmartPrivateMessage(name, "+================== Help Menu ===================");
        Iterator<CustomCommand> it = commands.values().iterator();
        while (it.hasNext()) {
            CustomCommand c = it.next();
            m_botAction.sendSmartPrivateMessage(name, "|  " + padString(14, c.command) + "- " + c.description);
        }
        m_botAction.sendSmartPrivateMessage(name, "+================================================");
    }
    
    /**
     * Adds space padding to the end of a string
     * 
     * @param length
     *            The length you want the string to be after padding
     * @param s
     *            The string to pad
     * @return A new padded string
     */
    public String padString(int length, String s) {
        StringBuilder builder = new StringBuilder();
        builder.append(s);
        for (int i = s.length(); i < length; i++) {
            builder.append(" ");
        }
        return builder.toString();
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
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && commands.containsKey(message))
            commands.get(message).message(p);
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && message.equalsIgnoreCase("!help"))
            do_customHelp(name);     
    }
    
    /**
     * Handles commands.
     */
    public void handleCommand(String name, String cmd) {
        if (cmd.startsWith("!addaction "))
            do_addAction(name, cmd.substring(11));
        if (cmd.startsWith("!removecmd "))
            do_removeCmd(name, cmd.substring(11));
        if (cmd.equalsIgnoreCase("!listcmd"))
            do_listCmd(name);
        if (cmd.startsWith("!describe "))
            do_describe(name, cmd.substring(10));
    }
    
    /**
     * Allows the bot user to create a description for a custom command that
     * users will see in the customized help menu.
     */
    public void do_describe(String name, String message) {
        int index = message.indexOf(" ");
        if (index == -1) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect usage. Example: !describe <Command> <Description>");
            return;
        }
        String command = message.substring(0, index);
        String description = message.substring(index + 1);
        if (!commands.containsKey(command)) {
            m_botAction.sendSmartPrivateMessage(name, "Command '" + command + "' not found.");
            return;
        }
        commands.get(command).describe(description);
        m_botAction.sendSmartPrivateMessage(name, "Description changed.");
    }
    
    /**
     * Allows the bot user to create a custom action for a specified custom
     * command. If the command does not exist it is created.
     */
    public void do_addAction(String name, String message) {
        int index = message.indexOf(" ");
        if (index == -1) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect usage. Example: !addaction !warbird *setship 1");
            return;
        }
        String command = message.substring(0, index);
        String msg = message.substring(index + 1);
        if (!commands.containsKey(command))
            commands.put(command, new CustomCommand(command));
        commands.get(command).addMessage(msg);
        m_botAction.sendSmartPrivateMessage(name, "Action added.");
    }
    
    /**
     * Removes a custom action from the specified command.
     */
    public void do_removeCmd(String name, String message) {
        int index = message.indexOf(" ");
        if (index == -1) {
        	if (commands.containsKey(message)) {
                commands.remove(message);
                m_botAction.sendSmartPrivateMessage(name, "Command '" + message + "' removed.");
            } else
                m_botAction.sendSmartPrivateMessage(name, "Specified command not found. Use !listcmd to see a list of registered commands.");
        }
        String command = message.substring(0, index);
        int actionIndex;
        try {
            actionIndex = Integer.parseInt(message.substring(index + 1));
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect usage. Example: !removeaction <Command> <Index of Action>");
            return;
        }
        if (!commands.containsKey(command)) {
            m_botAction.sendSmartPrivateMessage(name, "Command '" + command + "' not found.");
            return;
        }
        if (commands.get(command).hasIndex(actionIndex)) {
            commands.get(command).removeMessage(actionIndex);
            m_botAction.sendSmartPrivateMessage(name, "Action of " + command + " at index " + actionIndex + " removed.");
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Action of " + command + " at index " + actionIndex + " not found.");
        }
    }
    
    /**
     * Displays a list of all custom commands and their actions(including
     * indices)
     */
    public void do_listCmd(String name) {
        if (commands.size() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "There are no custom commands to list.");
            return;
        }
        Iterator<CustomCommand> it = commands.values().iterator();
        m_botAction.sendSmartPrivateMessage(name, "======== Custom Commands ========");
        while (it.hasNext()) {
            CustomCommand c = it.next();
            m_botAction.sendSmartPrivateMessage(name, "| Command: " + c.command);
            Iterator<String> i = c.getMessages().iterator();
            while (i.hasNext()) {
                String msg = i.next();
                m_botAction.sendSmartPrivateMessage(name, "|  " + c.getMessages().indexOf(msg) + ") " + msg);
            }
        }
    }
    
    /**
     * A Custom Command object.
     */
    private class CustomCommand {
        
        public ArrayList<String> messages;
        public String command;
        public String description = "No description available.";
     
        public CustomCommand(String cmd) {
            command = cmd;
        }
        
        /**
         * Changes description for the customized help menu.
         */
        public void describe(String message) {
            description = message;
        }
        
        /**
         * Adds an action to this command.
         */
        public void addMessage(String message) {
            if (messages == null)
                messages = new ArrayList<String>();
            messages.add(message);
        }
        
        /**
         * Removes an action from this command
         * 
         * @param index -
         *            the action's index as shown by !listcmd
         */
        public void removeMessage(int index) {
            if (messages != null)
                messages.remove(index);
            if (messages.size() == 0)
                messages = null;
        }
        
        /**
         * Checks to see if the given index exists
         * 
         * @return true if it does. else false.
         */
        public boolean hasIndex(int index) {
            return messages.get(index) != null;
        }
        
        /**
         * Get the entire ArrayList of actions for this command.
         * 
         * @return the ArrayList
         */
        public ArrayList<String> getMessages() {
            return messages;
        }
        
        /**
         * Sends all messages/actions listed in this command to the user.
         * 
         * @param name -
         *            the user.
         */
        public void message(Player p) {
            Iterator<String> it = messages.iterator();
            while (it.hasNext())
                CodeCompiler.handlePrivateTWScript(m_botAction, it.next(), p, twscript.isSysop);
        }
    }
    
    /**
     * Required methods.
     */
    public void cancel() {}    
    public void requestEvents(ModuleEventRequester modEventReq) {}
    
}
