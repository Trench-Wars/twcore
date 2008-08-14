package twcore.bots.multibot.twscript;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.util.ModuleEventRequester;
import twcore.core.OperatorList;
import twcore.core.util.Tools;

/**
 * @author milosh
 */
public class timers extends MultiUtil {
	
	public OperatorList opList;
	private TreeMap<String, CustomTimer> timers;
	
	public void init(){
		opList = m_botAction.getOperatorList();
		timers = new TreeMap<String, CustomTimer>();
	}
	
	public String[] getHelpMessages(){
		String[] help = {
		"+------------------------------------ Timers ------------------------------------+",
		"| !addtimer <timer> <message>   - Adds a TWScript <message> to a timer, <name>.  |",
		"|                               - If the timer does not exist it is created.     |",
		"| !removetimer <timer>          - Removes the timer, <name>.                     |",
		"| !removetimer <timer> <index>  - Removes message at <index> from <timer>        |",
		"| !runonce <timer> <s>          - Schedules the specified timer to run one time  |",
		"|                               - <s> seconds from the time you hit enter.       |",
		"|",
		"|",
		"|",
		"| !listtimer                  - Displays a list of timers and their messages.    |",
		"+--------------------------------------------------------------------------------+"
		};
		return help;
	}
	
	public void handleEvent(Message event){
		String message = event.getMessage();
		String name = event.getMessager() == null ? m_botAction.getPlayerName(event.getPlayerID()) : event.getMessager();
		int messageType = event.getMessageType();
		
		if(messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE)
			if(opList.isER(name))
				handleCommands(name, message);
	}
	
	public void handleCommands(String name, String cmd){
		if (cmd.startsWith("!addtimer "))
            do_addTimer(name, cmd.substring(10));
        if (cmd.startsWith("!removetimer "))
            do_removeTimer(name, cmd.substring(13));
        if (cmd.equalsIgnoreCase("!listtimer"))
            do_listTimer(name);
        if (cmd.startsWith("!runonce "))
        	do_runOnce(name, cmd.substring(9));
	}
	
	public void do_addTimer(String name, String message){
		int index = message.indexOf(" ");
        if (index == -1) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect usage. Example: !addtimer birthday I was born on this day!");
            return;
        }
        String timer = message.substring(0, index);
        String msg = message.substring(index + 1);
        if (!timers.containsKey(timer))
            timers.put(timer, new CustomTimer(timer));
        timers.get(timer).addMessage(msg);
        m_botAction.sendSmartPrivateMessage(name, "Timer added.");
	}
	
	public void do_removeTimer(String name, String message){
		int index = message.indexOf(" ");
        if (index == -1) {
        	if (timers.containsKey(message)) {
                timers.remove(message);
                m_botAction.sendSmartPrivateMessage(name, "Timer '" + message + "' removed.");
            } else
                m_botAction.sendSmartPrivateMessage(name, "Specified timer not found. Use !listtimer to see a list of registered timers.");
        }
        String timer = message.substring(0, index);
        int actionIndex;
        try {
            actionIndex = Integer.parseInt(message.substring(index + 1));
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect usage. Example: !removetimer <Timer> <Index of Message>");
            return;
        }
        if (!timers.containsKey(timer)) {
            m_botAction.sendSmartPrivateMessage(name, "Timer '" + timer + "' not found.");
            return;
        }
        if (timers.get(timer).hasIndex(actionIndex)) {
            timers.get(timer).removeMessage(actionIndex);
            m_botAction.sendSmartPrivateMessage(name, "Message of " + timer + " at index " + actionIndex + " removed.");
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Message of " + timer + " at index " + actionIndex + " not found.");
        }
	}
	
	public void do_listTimer(String name){
		if (timers.size() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "There are no timers to list.");
            return;
        }
        Iterator<CustomTimer> it = timers.values().iterator();
        m_botAction.sendSmartPrivateMessage(name, "========== Custom Timers ==========");
        while (it.hasNext()) {
            CustomTimer t = it.next();
            m_botAction.sendSmartPrivateMessage(name, "| Timer: " + t.name);
            Iterator<String> i = t.getMessages().iterator();
            while (i.hasNext()) {
                String msg = i.next();
                m_botAction.sendSmartPrivateMessage(name, "|  " + t.getMessages().indexOf(msg) + ") " + msg);
            }
        }
	}
	
	public void do_runOnce(String name, String message){
		int index = message.indexOf(" ");
        if (index == -1) {
        	m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !runonce <Timer> <Seconds>");
        	return;
        }
        String timer = message.substring(0, index);
        int actionIndex;
        try {
            actionIndex = Integer.parseInt(message.substring(index + 1));
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect usage. Example: !runonce <Timer> <Seconds>");
            return;
        }
        //TODO:
	}
	
private class CustomTimer{
	private String name;
	private ArrayList<String> messages;
	
	private CustomTimer(String name){
		this.name = name;
		messages = new ArrayList<String>();
	}
	
	private void addMessage(String message){
		messages.add(message);
	}
	
	private void removeMessage(int index){
		messages.remove(index);
	}
	
	private boolean hasIndex(int index){
		return messages.get(index) != null;
	}
	
	private ArrayList<String> getMessages(){
		return messages;
	}
	
	
}
	public void requestEvents(ModuleEventRequester req){}
	public void cancel(){}
}