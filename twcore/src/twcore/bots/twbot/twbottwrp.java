package twcore.bots.twbot;

import twcore.bots.TWBotExtension;
import twcore.core.*;
import twcore.core.events.Message;
import java.util.*;
import twcore.core.util.*;
import java.sql.*;

public class twbottwrp extends TWBotExtension {
	
	String database;
	int lastPopRecord = 0;
	boolean initialized = false;
	
	public twbottwrp() {
	}
	
	public void init() {
		if(m_botAction.getBotName().toLowerCase().startsWith("twbot") || m_botAction.getBotNumber() > 10) {
			database = "website";
		} else {
			database = "local";
		}
	}
	
	public void handleEvent(Message event) {
		if(!initialized) init();
		String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name ) ) {
            	handleCommand( name, message );
            }
        } else if(event.getMessageType() == Message.ARENA_MESSAGE) {
        	if(message.toLowerCase().indexOf("?go "+m_botAction.getArenaName()) != -1) {
        		TimerTask popRecord = new TimerTask() {
        			public void run() {
        				recordPop();
        			}
        		};
        		m_botAction.scheduleTask(popRecord, 60 * 1000);
        	}
        }
	}
	
	public void handleCommand(String name, String message) {
		if(message.toLowerCase().startsWith("!mvp ")) {
			handleMvp(name, message.substring(5));
		} else if(message.toLowerCase().startsWith("!last")) {
			getLast(name);
		}
	}
	
	public void getLast(String name) {
		try {
			ResultSet results = m_botAction.SQLQuery(database, "SELECT * FROM tblRewardPending ORDER BY fdDate DESC LIMIT 0,1");
			if(results.next()) {
				String mvp = results.getString("fcMvpName");
				String arena = results.getString("fcArenaName");
				String host = results.getString("fcHostName");
				String modules = results.getString("fcModulesLoaded");
				int initPop = results.getInt("fnArenaInitialPopulation");
				int finalPop = results.getInt("fnArenaFinalPopulation");
				String date = results.getString("fdDate");
				m_botAction.sendPrivateMessage(name, "MVP:        " + mvp);
				m_botAction.sendPrivateMessage(name, "Arena:      " + arena);
				m_botAction.sendPrivateMessage(name, "Host:       " + host);
				m_botAction.sendPrivateMessage(name, "Modules:    " + modules);
				m_botAction.sendPrivateMessage(name, "Init Pop:   " + initPop);
				m_botAction.sendPrivateMessage(name, "Final Pop:  " + finalPop);
				m_botAction.sendPrivateMessage(name, "Date:       " + date);
			}
                        m_botAction.SQLClose( results );
		} catch(Exception e) {}
	}
	
	public void handleMvp(String name, String mvp) {
		String modulesLoaded = ((twbot)m_twBot).modulesToStringList();
		String arena = m_botAction.getArenaName();
		int atStartPop = lastPopRecord;
		int atEndPop = m_botAction.getArenaSize();
		String host = ((twbot)m_twBot).getHostName();
		try {
		    m_botAction.SQLQueryAndClose(database, "INSERT INTO tblRewardPending "
				+ "(fcMvpName, fcArenaName, fcHostName, fcModulesLoaded, fnArenaInitialPopulation, fnArenaFinalPopulation, fdDate) "
				+ "VALUES ('"+Tools.addSlashesToString(mvp)+"', '"+arena+"', '"+Tools.addSlashesToString(host)+"', "
				+ "'"+Tools.addSlashesToString(modulesLoaded)+"', "+atStartPop+", "+atEndPop+", NOW())");
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	public void recordPop() {
		lastPopRecord = m_botAction.getArenaSize();
	}
	
	public void cancel() {
	}
	
	public String[] getHelpMessages() {
		String[] str = { "!mvp <name>            -Gives <name> mvp for the game." };
		return str;
	}
}