package twcore.bots.twbot;

import twcore.bots.TWBotExtension;
import twcore.core.*;
import twcore.core.events.Message;
import java.util.*;
import twcore.core.util.*;

public class twbottwrp extends TWBotExtension {
	
	String database;
	int lastPopRecord = 0;
	
	public twbottwrp() {
		if(m_botAction.getBotName().toLowerCase().startsWith("twbot") || m_botAction.getBotNumber() > 10) {
			database = "website";
		} else {
			database = "local";
		}
	}
	
	public void handleEvent(Message event) {
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
		}
	}
	
	public void handleMvp(String name, String mvp) {
		String modulesLoaded = m_twBot.modulesToStringList();
		String arena = m_botAction.getArenaName();
		int atStartPop = lastPopRecord;
		int atEndPop = m_botAction.getArenaSize();
		String host = m_twBot.getHostName();
		m_botAction.SQLQuery(database, "INSERT INTO tblRewardPending "
		+ "(fcMvpName, fcArenaName, fcHostName, fcModulesLoaded, fnArenaInitialPopulation, fnArenaFinalPopulation, fdDate) "
		+ "VALUES ('"+Tools.addSlashesToString(mvp)+"', '"+arena+"', '"+Tools.addSlashesToString(host)+"', "
		+ "'"+Tools.addSlashesToString(modulesLoaded)+"', "+atStartPop+", "+atEndPop+", NOW())");
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