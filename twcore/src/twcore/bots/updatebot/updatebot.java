package twcore.bots.updatebot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;


/**
 * This bot cycles through every arena in arenas.txt file and can be 
 * used to execute multiple commands in the arena.
 *
 * @author  CRe
 */
public class updatebot extends SubspaceBot {

    private BotSettings m_botSettings; 
    
    private Vector<String> arenas;
    private Vector<String> commandsToExecute;
    
    private boolean ready;
    private boolean completedTask;
    private String coreLocation;
    private String executor;
    
    private final int ARENA_SWITCH_DELAY = 30000;
    
    private ArenaSwitch arenaSwitch;
    
    public updatebot(BotAction botAction) {
        super(botAction);
        requestEvents();

        m_botSettings = m_botAction.getBotSettings();
        coreLocation = ba.getCoreData().getGeneralSettings().getString("Core Location");
        
        commandsToExecute = new Vector<String>();
        arenas = new Vector<String>();
        
        ready = false;
        completedTask = false;
        
        retrieveArenaList();        
        
        arenaSwitch = new ArenaSwitch();
    }

    private void retrieveArenaList() {
    	try (BufferedReader br = new BufferedReader(new FileReader(coreLocation + "/twcore/bots/updatebot/arenas.txt"))) { 
			
    		String currentLine;
 
			while ((currentLine = br.readLine()) != null) {
				arenas.add(currentLine);
				Tools.printLog("UpdateBot: Added " + currentLine);
			} 
		} catch (IOException e) {
			Tools.printStackTrace(e);
		} 
    }

    public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.ARENA_JOINED);
        req.request(EventRequester.LOGGED_ON);
    }

    public void handleEvent(Message event) {
    	
    	String msg = null; 
    	
        if (event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE || event.getMessageType() == Message.PRIVATE_MESSAGE)
        {
            msg = event.getMessage().toLowerCase();
        }
            
        String name = event.getMessager();
        if (name == null) {
        	name = m_botAction.getPlayerName(event.getPlayerID());
        }
        
        if (name == null) return;
    	
    	String[] helpmsg = {
    			"!help - Derp",
    			"!addcommand <command> - Adds command to command list",
    			"!removecommand <#> - Removes command from command list. Find # in !commandlist.",
    			"!listcommands - Lists added commands.",
    			"!cycle - Cycles through every arena and executes command(s). Bot will notify & shutdown when done.",
    			"!die - Murders this bot.",
    			"This bot cycles through arenas in a provided file and executes the inputed commands."};
    	
        if(m_botAction.getOperatorList().isSysop(name) && !ready) {        	
        	if(msg.startsWith("!help")) {
        		m_botAction.privateMessageSpam(name, helpmsg);
        	} else if (msg.startsWith("!addcommand ")) {
        		do_addCommand(name, msg.substring(12));
        	} else if (msg.startsWith("!removecommand ")) {
        		do_removeCommand(name, msg.substring(15));
        	} else if (msg.startsWith("!listcommands")) {
        		do_listCommand(name);
        	} else if (msg.startsWith("!cycle")) {
        		do_cycle(name);
        	} else if (msg.startsWith("!die")) {
        		m_botAction.die();
        	}  	
        }
    }
        
    private void do_addCommand(String name, String command) {
    	if(command == null)
    		return;
 	
    	commandsToExecute.addElement(command);
    	m_botAction.sendPrivateMessage(name, "Command Added: " + command);
    }
        
    private void do_removeCommand(String name, String number) {
    	int i = 0;
    	
    	try {
    		i = Integer.parseInt(number);
    	} catch (NumberFormatException e) {
    		m_botAction.sendPrivateMessage(name, "Error: Invalid input.");
    	}
    	
    	if(commandsToExecute.size() > 0 && !(i > commandsToExecute.size()))
    	{
    		commandsToExecute.remove(i);
    		m_botAction.sendPrivateMessage(name, "Removed command.");
    	}
    	else
    		m_botAction.sendPrivateMessage(name, "Error: Invalid input.");
    }
    
    private void do_listCommand(String name) {
    	for(int i = 0; i < commandsToExecute.size(); i++) {
    		m_botAction.sendPrivateMessage(name, "#"+i+"  "+commandsToExecute.get(i));
    	}
    	
    	if(commandsToExecute.size() == 0)
    		m_botAction.sendPrivateMessage(name,"Nothing to list.");
    }

    private void do_cycle(String name)   {
    	m_botAction.scheduleTask(arenaSwitch, 1000, ARENA_SWITCH_DELAY);
    	ready = true;
    	executor = name;
    	m_botAction.sendPrivateMessage(name, executor);
    	m_botAction.sendPrivateMessage(name, "Cycle has begun. Will ignore all commands now.");
    	m_botAction.sendPrivateMessage(name, "Estimated time for completion is about: " + (((double)ARENA_SWITCH_DELAY+1000.0)/60000.0)*(double)arenas.size() + " minutes.");
    	Tools.printLog("UpdateBot: Cycle started by "+name);
    }

    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botSettings.getString("arena"));
    }

    public void handleEvent(ArenaJoined event) {
    	if(ready) {
    		for(int i = 0; i < commandsToExecute.size(); i++)
    			m_botAction.sendUnfilteredPublicMessage(commandsToExecute.get(i));
    	}
    }
    
    public void handleDisconnect() {
    	arenaSwitch.cancel();
    	String status = completedTask ? "completed. " : "not completed. ";
    	Tools.printLog("UpdateBot: Shutting down. Cycle " + status + "Arena: "
    		+ m_botAction.getArenaName());
    }
    
    private class ArenaSwitch extends TimerTask
    {    	
    	public ArenaSwitch() {}
    	    	
    	public void run() {
    		if (arenas.size() > 0) {
    			m_botAction.changeArena(arenas.remove(0));
    		}
    		else {
    			m_botAction.sendRemotePrivateMessage(executor, "Task completed. Shutting down.");
    			ready = false;
    			completedTask = true;
    			m_botAction.die();
    		}    		
    	}
    }
}
