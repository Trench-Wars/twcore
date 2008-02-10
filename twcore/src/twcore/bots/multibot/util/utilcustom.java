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
	
	public void do_listKeys(String name){
		String msg[] = {
				"+=============== Escape Keys ===============+",
				"| &player         - The player's name.      |",
				"| &freq           - The player's frequency. |",
				"| &ship           - The player's ship.      |",
				"| &squad          - The player's squad.     |",
				"| &bounty         - The player's bounty.    |",
				"| &x              - X Location(Tiles)       |",
				"| &y              - Y Location(Tiles)       |",
				"| &wins           - The player's wins.      |",
				"| &losses         - The player's losses.    |",
				"+===========================================+",
		};
		m_botAction.smartPrivateMessageSpam(name, msg);
	}
	
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
	
	public void do_addAction(String name, String message) {
		int index = message.indexOf(" ");
		if(index == -1){
			m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !addaction !warbird *setship 1");
			return;
		}
		String command = message.substring(0, index);
		String msg = message.substring(index + 1);
		if((msg.startsWith("*warn") ||
	    		msg.startsWith("*shutup") ||
	    	    msg.startsWith("*kill") ||
	    	    msg.startsWith("*mirror") ||
	    	    msg.startsWith("*sendto") ||
	    	    msg.startsWith("*ufo") ||
	    	    msg.startsWith("*putfile") ||
	    	    msg.startsWith("*getfile") ||
	    	    msg.startsWith("*sysop") ||
	    	    msg.startsWith("*smoderator") ||
	    	    msg.startsWith("*moderator") ||
	    	    msg.startsWith("*permit") ||
	    	    msg.startsWith("*revoke") ||
	    	    msg.startsWith("*points") ||
	    	    msg.startsWith("*bandwidth") ||
	    	    msg.startsWith("*lowbandwidth") ||
	    	    msg.startsWith("*thor") ||
	    	    msg.startsWith("*super")) &&
	    	    !opList.isSmod(name)){
	    		m_botAction.sendSmartPrivateMessage( name, "That command is restricted.");
	    		return;
	    	}
		if(!commands.containsKey(command))
			commands.put(command, new CustomCommand(command));
		commands.get(command).addMessage(msg);
		m_botAction.sendSmartPrivateMessage( name, "Action added.");
	}
	
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
	
	public void do_removeCmd(String name, String message){
		if(commands.containsKey(message)){
			commands.remove(message);
			m_botAction.sendSmartPrivateMessage( name, "Command '" + message + "' removed.");
		}
		else {
			m_botAction.sendSmartPrivateMessage( name, "Specified command not found. Use !listcmd to see a list of registered commands.");
		}
	}
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
	
	
	private class CustomCommand {
		
		public ArrayList<String> messages;
		public String command, description = "No description available.";
		
		public CustomCommand(String cmd){
			command = cmd;
		}
		
		public void describe(String message){
			description = message;
		}
		
		public void addMessage(String message){
			if(messages == null)
				messages = new ArrayList<String>();
			messages.add(message);
		}
		
		public void removeMessage(int index){
			if(messages != null)
				messages.remove(index);
			if(messages.size() == 0)
				messages = null;
		}
		
		public boolean hasIndex(int index){
			return messages.get(index) != null;
		}
		
		public ArrayList<String> getMessages(){
			return messages;
		}
		
		public String replaceKeys(String name, String message){
			Player p = m_botAction.getPlayer(name);
			if(message.contains("&player"))
				message = message.replace("&player", name);
			if(p == null)return message;
			if(message.contains("&freq")){
				message = message.replace("&freq", p.getFrequency() + "");			
			}
			if(message.contains("&ship")){
				message = message.replace("&ship", getShipNames(p.getShipType()));
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
			//TODO: Feel free to add more escape keys.
			return message;
		}
		
		public String getShipNames(int ship){
			String message = "-Unknown-";
			switch(ship){
			case 0: message =  "Spectator"; break;
			case 1: message =  "Warbird"; break;
			case 2: message =  "Javelin"; break;
			case 3: message =  "Spider"; break;
			case 4: message =  "Leviathan"; break;
			case 5: message =  "Terrier"; break;
			case 6: message =  "Weasel"; break;
			case 7: message =  "Lancaster"; break;
			case 8: message =  "Shark"; break;
			}
			return message;
		}
		
		public void message(String name){
			Iterator<String> it = messages.iterator();
			while( it.hasNext() ) {
				String message = it.next();
				message = replaceKeys(name, message);
				m_botAction.sendUnfilteredPrivateMessage(name, message);
			}
			
		}
	}
	
	public void cancel() {}
	public void requestEvents(ModuleEventRequester modEventReq) {}

}
