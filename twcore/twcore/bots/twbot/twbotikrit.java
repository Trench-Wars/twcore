package twcore.bots.twbot;

import twcore.core.*;
import twcore.core.game.Player;
import twcore.core.events.PlayerDeath;
import twcore.core.events.ScoreUpdate;
import twcore.core.events.PlayerEntered;
import twcore.core.events.ScoreReset;
import twcore.bots.*;

public class twbotikrit extends TWBotExtension {
	
	public twbotikrit() {
		
	}
	
	public void handleEvent(PlayerDeath event) {
		Player killer = m_botAction.getPlayer(event.getKillerID());
		if(killer != null) {
			m_botAction.sendPublicMessage("PlayerDeath event: " + killer.getScore());
		}
	}
	
	public void handleEvent(ScoreUpdate event) {
		Player p = m_botAction.getPlayer(event.getPlayerID());
		m_botAction.sendPublicMessage("ScoreUpdate event: " + p.getScore());
	}
	
	public void handleEvent(PlayerEntered event) {
		Player p = m_botAction.getPlayer(event.getPlayerID());
		m_botAction.sendPublicMessage("PlayerEntered event: " + p.getScore());
	}
	
	public void handleEvent(ScoreReset event) {
		Player p = m_botAction.getPlayer(event.getPlayerID());
		m_botAction.sendPublicMessage("ScoreReset event: " + p.getScore() + " -" + p.getPlayerName());
	}
	
	public String[] getHelpMessages() {
		String[] str = { "" };
		return str;
	}
	
	public void cancel() {
	}
}