package twcore.bots.estatsbot;

import twcore.core.*;
import java.util.*;
import java.sql.*;

public class estatsbot extends SubspaceBot {
	
	HashMap players;
	boolean gameRunning = false;
	ElimGame thisGame;
	String ref;
	
	public estatsbot(BotAction botAction) {
		super(botAction);
		players = new HashMap();
		EventRequester events = m_botAction.getEventRequester();
		events.request(events.MESSAGE);
		events.request(events.PLAYER_DEATH);
		events.request(events.LOGGED_ON);
	}
	
	public void handleEvent(LoggedOn event) {
		m_botAction.joinArena(m_botAction.getBotSettings().getString("Arena"+m_botAction.getBotNumber()));
		ref = m_botAction.getBotSettings().getString("Ref"+m_botAction.getBotNumber());
	}
	
	public void handleEvent(PlayerDeath event) {
		if(!gameRunning) return;
		
		String killer = m_botAction.getPlayerName(event.getKillerID());
		String killee = m_botAction.getPlayerName(event.getKilleeID());
		((ElimPlayer)players.get(killer.toLowerCase())).addKill();
		((ElimPlayer)players.get(killee.toLowerCase())).addDeath();
	}
	
	public void handleEvent(Message event) {
		if(event.getMessageType() == Message.ARENA_MESSAGE) {
			handleMessage(event.getMessage());
		} else if(event.getMessageType() == Message.PRIVATE_MESSAGE) {
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if(name.equalsIgnoreCase(ref) && gameRunning) {
				handlePM(event.getMessage());
			} else if(m_botAction.getOperatorList().isSmod(name) || name.equalsIgnoreCase("ikrit <er>")) {
				if(event.getMessage().toLowerCase().startsWith("!die")) {
					m_botAction.die();
				} else {
					tellAbout(name);
				}
			} else {
				tellAbout(name);
			}
		}
	}
	
	public void tellAbout(String name) {
		if(name == null) return;
		m_botAction.sendPrivateMessage(name, "Hey, just your friendly StatsBot here watching everything you do >.>. You can check my stuff out at http://www.trenchwars.org/Elim");
	}
	
	public void handleMessage(String message) {
		message = message.toLowerCase();
		if(message.startsWith("go! go! go!")) {
			gameRunning = true;
			startGame();
		} else if(message.startsWith("game over:")) {
			gameRunning = false;
			endGame(message.substring(18));
		}
	}
	
	public void handlePM(String message) {
		try {
			String type = message.substring(15, message.indexOf(";"));
			thisGame.setType(type);
		} catch(Exception e) { Tools.printStackTrace(message, e); }
	}
	
	public void startGame() {
		try {
			players.clear();
			Iterator it = m_botAction.getPlayingPlayerIterator();
			while(it.hasNext()) {
				String name = ((Player)it.next()).getPlayerName();
				players.put(name.toLowerCase(), new ElimPlayer(name));
			}
			thisGame = new ElimGame(players.size(), m_botAction.getArenaName().equalsIgnoreCase("elim"));
			m_botAction.sendPrivateMessage(ref, "!status");
		} catch(Exception e) {}
	}
	
	public void endGame(String winner) {
		try {
			if(winner != null && !winner.equalsIgnoreCase("ner")) {
				thisGame.setWinner(winner);
			} else {
				thisGame.setWinner("No winner");
			}
			m_botAction.SQLQuery("local", thisGame.getQuery());
			ResultSet results = m_botAction.SQLQuery("local", "SELECT fnGameID FROM tblElimRound ORDER BY fnGameID DESC");
			results.next();
			int gID = results.getInt("fnGameID");
			Iterator it = players.values().iterator();
			while(it.hasNext()) {
				ElimPlayer ep = (ElimPlayer)it.next();
				m_botAction.SQLQuery("local", ep.getQuery(gID, ep.name.equalsIgnoreCase(winner)));
			}
		} catch(Exception e) { Tools.printStackTrace(e); }
	}
}

class ElimPlayer {
	String name;
	int kills;
	int deaths;
	
	public ElimPlayer(String n) {
		name = n;
		kills = 0;
		deaths = 0;
	}
	
	public void addDeath() {
		deaths++;
	}
	
	public void addKill() {
		kills++;
	}
	
	public String getQuery(int gID, boolean won) {
		int w = 0; if(won) w = 1;
		String query = "INSERT INTO tblElimRoundPlayer (fnGameID, fcUserName, fnKill, fnDeath, fnWon, ftDate) VALUES ("+gID+", '"+Tools.addSlashesToString(name)+"', "+kills+", "+deaths+", "+w+", NOW());";
		return query;
	}
}

class ElimGame {
	String gameType;
	int players;
	String winner;
	boolean isElim;
	
	public ElimGame(int p, boolean elim) {
		players = p;
		isElim = elim;
	}
	
	public void setType(String type) {
		gameType = type;
	}
		
	public void setWinner(String w) {
		winner = w;
	}
	
	public String getQuery() {
		int e = 0; if(isElim) e = 1;
		String query = "INSERT INTO tblElimRound (fnGameID, fnPlayers, fcGameType, fcWinner, fnElim, ftDate) VALUES(0, "+players+", '"+gameType+"', '"+winner+"', "+e+", NOW());";
		return query;
	}
}