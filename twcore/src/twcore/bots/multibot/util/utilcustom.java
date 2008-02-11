package twcore.bots.multibot.util;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.StringBuilder;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.OperatorList;
import twcore.core.util.Tools;

/**
 * @author milosh
 */
public class utilcustom extends MultiUtil{
	
	OperatorList opList;
	TreeMap<String, CustomCommand> commands;
	
	public void init() {
		opList = m_botAction.getOperatorList();
		commands = new TreeMap<String, CustomCommand>();
	}
	
	public String[] getHelpMessages() {
		String[] message =
	    {
		  "!addaction <!cmd> <action>  - Adds another <action> to command <!command>",
		  "                            - If <!command> does not exist it is created.",
		  "!removeact <!cmd> <index>   - Removes action at <index> from <!cmd>",
	      "!removecmd <!cmd>           - Removes a custom command and all its actions",
	      "!listcmd                    - Shows a list of custom commands and their actions",
	      "!describe <!cmd>            - Submits a description of <!cmd> that shows in help",
	      "!listkeys                   - Shows a list of available escape phrases"
	      
	    };

	    return message;
	}
	
    /**
     * Displays a customized help menu for commands created by this utility
     * @param name - The person requesting the help menu.
     */
	public void do_customHelp(String name) {
		if(commands.size() == 0)return;
		m_botAction.sendSmartPrivateMessage( name, "+================== Help Menu ===================");
		Iterator<CustomCommand> it = commands.values().iterator();
		while( it.hasNext() ){
			CustomCommand c = it.next();
			m_botAction.sendSmartPrivateMessage( name, "|  " + padString(14,c.command) + "- " + c.description);
		}
		m_botAction.sendSmartPrivateMessage( name, "+================================================");
	}
	
	/**
	 * Adds space padding to the end of a string
	 * @param length The length you want the string to be after padding
	 * @param s The string to pad
	 * @return A new padded string
	 */
	public String padString(int length, String s){
		StringBuilder builder = new StringBuilder();
		builder.append(s);
		for(int i = s.length(); i < length; i++ ){
			builder.append(" ");
		}
		return builder.toString();
	}
	
	public void handleEvent(Message event){
		String message = event.getMessage();
		String name = m_botAction.getPlayerName(event.getPlayerID());		
		if(event.getMessageType() == Message.PRIVATE_MESSAGE && opList.isER(name))
				handleCommand(name, message);
		if(commands.containsKey(message))
			commands.get(message).message(name);
		if(event.getMessageType() == Message.PRIVATE_MESSAGE && message.equalsIgnoreCase("!help"))
			do_customHelp(name);
		
	}
	public void handleCommand(String name, String cmd){
		if(cmd.startsWith("!addaction "))
			do_addAction(name, cmd.substring(11));
		if(cmd.startsWith("!removeact "))
			do_removeAction(name, cmd.substring(11));
		if(cmd.startsWith("!removecmd "))
			do_removeCmd(name, cmd.substring(11));
		if(cmd.equalsIgnoreCase("!listcmd"))
			do_listCmd(name);
		if(cmd.startsWith("!describe "))
			do_describe(name, cmd.substring(10));
		if(cmd.equalsIgnoreCase("!listkeys"))
			do_listKeys(name);
	}
	
	/**
	 * Displays a list of escape keys for use in custom commands and/or messages
	 * @param name - The person requesting this list
	 */
	public void do_listKeys(String name){
		String msg[] = {
				"+=================== Escape Keys ===================+",
				"| &player         - The player's name.              |",
				"| &freq           - The player's frequency.         |",
				"| &ship           - The player's ship.              |",
				"| &squad          - The player's squad.             |",
				"| &bounty         - The player's bounty.            |",
				"| &x              - X Location(Tiles)               |",
				"| &y              - Y Location(Tiles)               |",
				"| &wins           - The player's wins.              |",
				"| &losses         - The player's losses.            |",
				"| &!command&&   - Issues a command to the bot, but  |",
				"|                    the player receives no message.|",
				"+===================================================+",
		};
		m_botAction.smartPrivateMessageSpam(name, msg);
	}
	
	/**
	 * A white-list of allowed custom commands.
	 * @param s - The string
	 * @return true if the string is allowed. else false.
	 */
	public boolean isAllowed(String s){
		if(s.startsWith("*setship")   ||
		   s.startsWith("*setfreq")   ||
		   s.startsWith("*warpto")    ||
		   s.equals("*scorereset")    ||
		   s.equals("*spec")          ||
		   s.equals("*prize #4")      ||//Stealth
		   s.equals("*prize #5")      ||//Cloak
		   s.equals("*prize #6")      ||//X-radar
		   s.equals("*prize #7")      ||//Warp
		   s.equals("*prize #13")     ||//Full charge
		   s.equals("*prize #14")     ||//Engine shutdown
		   s.equals("*prize #15")     ||//Multi-fire
		   s.equals("*prize #17")     ||//Super
		   s.equals("*prize #18")     ||//Shields
		   s.equals("*prize #19")     ||//Shrapnel
		   s.equals("*prize #20")     ||//Anti-warp
		   s.equals("*prize #21")     ||//Repel
		   s.equals("*prize #22")     ||//Burst
		   s.equals("*prize #23")     ||//Decoy
		   s.equals("*prize #24")     ||//Thor
		   s.equals("*prize #25")     ||//Multi-prize
		   s.equals("*prize #26")     ||//Brick
		   s.equals("*prize #27")     ||//Rocket
		   s.equals("*prize #28")     ||//Portal
		   s.equals("*prize #-4")     ||//Negative Stealth
		   s.equals("*prize #-5")     ||//Negative Cloak
		   s.equals("*prize #-6")     ||//Negative X-radar
		   s.equals("*prize #-7")     ||//Negative Warp
		   s.equals("*prize #-13")    ||//Negative Full charge
		   s.equals("*prize #-14")    ||//Negative Engine shutdown
		   s.equals("*prize #-15")    ||//Negative Multi-fire
		   s.equals("*prize #-17")    ||//Negative Super
		   s.equals("*prize #-18")    ||//Negative Shields
		   s.equals("*prize #-19")    ||//Negative Shrapnel
		   s.equals("*prize #-20")    ||//Negative Anti-warp
		   s.equals("*prize #-21")    ||//Negative Repel
		   s.equals("*prize #-22")    ||//Negative Burst
		   s.equals("*prize #-23")    ||//Negative Decoy
		   s.equals("*prize #-24")    ||//Negative Thor
		   s.equals("*prize #-25")    ||//Negative Multi-prize
		   s.equals("*prize #-26")    ||//Negative Brick
		   s.equals("*prize #-27")    ||//Negative Rocket
		   s.equals("*prize #-28"))     //Negative Portal
		return true;
		else return false;
	}
	
	/**
	 * Allows the bot user to create a description for a custom command that users will see in
	 * the customized help menu.
	 */
	public void do_describe(String name, String message) {
		int index = message.indexOf(" ");
		if(index == -1){
			m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !describe <Command> <Description>");
			return;
		}
		String command = message.substring(0, index);
		String description = message.substring(index + 1);
		if(!commands.containsKey(command)){
			m_botAction.sendSmartPrivateMessage( name, "Command '" + command + "' not found.");
			return;
		}
		commands.get(command).describe(description);
		m_botAction.sendSmartPrivateMessage( name, "Description changed.");
	}
	
	/**
	 * Allows the bot user to create a custom action for a specified custom command. If the command does
	 * not exist it is created.
	 */
	public void do_addAction(String name, String message) {
		int index = message.indexOf(" ");
		if(index == -1){
			m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !addaction !warbird *setship 1");
			return;
		}
		String command = message.substring(0, index);
		String msg = message.substring(index + 1);
		if(msg.startsWith("*")){
			if(!isAllowed(msg) && !opList.isSmod(name)){
				m_botAction.sendSmartPrivateMessage( name, "Command not added; Restricted or unknown.");
				return;
			}
		}		
		if(!commands.containsKey(command))
			commands.put(command, new CustomCommand(command));
		commands.get(command).addMessage(msg);
		m_botAction.sendSmartPrivateMessage( name, "Action added.");
	}
	
	/**
	 * Removes a custom action from the specified command.
	 */
	public void do_removeAction(String name, String message) {
		int index = message.indexOf(" ");
		if(index == -1){
			m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !removeaction <Command> <Index of Action>");
			return;
		}
		String command = message.substring(0, index);
		int actionIndex;
		try{ 
			actionIndex = Integer.parseInt(message.substring(index + 1));
		}catch(Exception e){
			m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !removeaction <Command> <Index of Action>");
			return;
		}
		if(!commands.containsKey(command)){
			m_botAction.sendSmartPrivateMessage( name, "Command '" + command + "' not found.");
			return;
		}
		if(commands.get(command).hasIndex(actionIndex)){		
			commands.get(command).removeMessage(actionIndex);
			m_botAction.sendSmartPrivateMessage( name, "Action of " + command + " at index " + actionIndex + " removed.");
		}
		else {
			m_botAction.sendSmartPrivateMessage( name, "Action of " + command + " at index " + actionIndex + " not found.");
		}
	}
	
	/**
	 * Removes an entire custom command and all of its actions.
	 */
	public void do_removeCmd(String name, String message){
		if(commands.containsKey(message)){
			commands.remove(message);
			m_botAction.sendSmartPrivateMessage( name, "Command '" + message + "' removed.");
		}
		else {
			m_botAction.sendSmartPrivateMessage( name, "Specified command not found. Use !listcmd to see a list of registered commands.");
		}
	}
	
	/**
	 * Displays a list of all custom commands and their actions(including indices)
	 */
	public void do_listCmd(String name){
		if(commands.size() == 0){
			m_botAction.sendSmartPrivateMessage( name, "There are no custom commands.");
			return;
		}
		Iterator<CustomCommand> it = commands.values().iterator();
		m_botAction.sendSmartPrivateMessage( name, "======== Custom Commands ========");
		while( it.hasNext() ){
			CustomCommand c = it.next();
			m_botAction.sendSmartPrivateMessage( name, "| Command: " + c.command );
			Iterator<String> i = c.getMessages().iterator();
			while( i.hasNext() ){
				String msg = i.next();
				m_botAction.sendSmartPrivateMessage( name, "|  " + c.getMessages().indexOf(msg) + ") "+ msg );
			}
		}
	}
	
	/**
	 * A Custom Command object.
	 */
	private class CustomCommand {
		
		public ArrayList<String> messages; //All of the actions for this command.
		public String command; //The name of this command.
		public String description = "No description available."; //A description of this command for the customized help menu.
		
		public CustomCommand(String cmd){
			command = cmd;
		}
		
		/**
		 * Changes description for the customized help menu.
		 */
		public void describe(String message){
			description = message;
		}
		
		/**
		 * Adds an action to this command.
		 */
		public void addMessage(String message){
			if(messages == null)
				messages = new ArrayList<String>();
			messages.add(message);
		}
		
		/**
		 * Removes an action from this command
		 * @param index - the action's index as shown by !listcmd
		 */
		public void removeMessage(int index){
			if(messages != null)
				messages.remove(index);
			if(messages.size() == 0)
				messages = null;
		}
		
		/**
		 * Checks to see if the given index exists
		 * @return true if it does. else false.
		 */
		public boolean hasIndex(int index){
			return messages.get(index) != null;
		}
		
		/**
		 * Get the entire ArrayList of actions for this command.
		 * @return the ArrayList
		 */
		public ArrayList<String> getMessages(){
			return messages;
		}
		
		/**
		 * Replaces escape keys when messaging a user who has used this command
		 * @param name - The user accessing this command
		 * @param message - The message/action to be sent to the user
		 * @return A string with objects instead of escape keys
		 */
		public String replaceKeys(String name, String message){
			Player p = m_botAction.getPlayer(name);
			if(message.contains("&player"))
				message = message.replace("&player", name);
			if(p == null)return message;
			if(message.contains("&freq")){
				message = message.replace("&freq", p.getFrequency() + "");			
			}
			if(message.contains("&ship")){
				message = message.replace("&ship", Tools.shipName(p.getShipType()));
			}
			if(message.contains("&shipslang")){
				message = message.replace("&shipslang", Tools.shipNameSlang(p.getShipType()));
			}
			if(message.contains("&wins")){
				message = message.replace("&wins", p.getWins() + "");
			}
			if(message.contains("&losses")){
				message = message.replace("&losses", p.getLosses() + "");
			}
			if(message.contains("&bounty")){
				message = message.replace("&bounty", p.getBounty() + "");
			}
			if(message.contains("&squad")){
				message = message.replace("&squad", p.getSquadName());
			}
			if(message.contains("&x")){
				message = message.replace("&x", p.getXLocation()/16 + "");
			}
			if(message.contains("&y")){
				message = message.replace("&y", p.getYLocation()/16 + "");
			}
			if(message.contains("&!")){
				while(true){
					int beginIndex = message.indexOf("&!");
					int endIndex = message.indexOf("&&");
					if(endIndex != -1 && endIndex > beginIndex){
						m_botAction.sendPrivateMessage(m_botAction.getBotName(), message.substring(beginIndex + 1, endIndex));
						message = message.replaceFirst("&!", " ");
						message = message.replaceFirst("&&", " ");
					}
					else break;
				}
				message = null;
			}
			//TODO: Feel free to add more escape keys.
			return message;
		}
		
		/**
		 * Sends all messages/actions listed in this command to the user.
		 * @param name - the user.
		 */
		public void message(String name){
			Iterator<String> it = messages.iterator();
			while( it.hasNext() ) {
				String message = it.next();
				message = replaceKeys(name, message);
				if(message != null)
					m_botAction.sendUnfilteredPrivateMessage(name, message);
			}
			
		}
	}
	
	public void cancel() {}
	public void requestEvents(ModuleEventRequester modEventReq) {}

}
