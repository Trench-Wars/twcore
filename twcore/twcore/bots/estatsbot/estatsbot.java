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
		} else if(event.getMessageType() == Message.PRIVATE_MESSAGE && gameRunning) {
			if(event.getMessager().equalsIgnoreCase(ref)) {
				handlePM(event.getMessage());
			}
		}
	}
	
	public void handleMessage(String message) {
		message = message.toLowerCase();
		if(message.startsWith("go! go! go!")) {
			gameRunning = true;
			startGame();
		} else if(message.startsWith("game over:")) {
			gameRunning = false;
			endGame();
		}
	}
	
	public void handlePM(String message) {
		String type = message.substring(15, message.indexOf(";"));
		String pieces[] = type.split(" to ");
		thisGame.setType(pieces[0]);
		thisGame.setKills(Integer.parseInt(pieces[1]));
	}
	
	public void startGame() {
		players.clear();
		Iterator it = m_botAction.getPlayingPlayerIterator();
		while(it.hasNext()) {
			String name = ((Player)it.next()).getPlayerName();
			players.put(name.toLowerCase(), new ElimPlayer(name));
		}
		thisGame = new ElimGame(players.size(), m_botAction.getArenaName().equalsIgnoreCase("elim"));
		m_botAction.sendPrivateMessage(ref, "!status");
	}
	
	public void endGame() {
		try {
			String winner = "";
			Iterator it = m_botAction.getPlayingPlayerIterator();
			if(it.hasNext()) {
				String name = ((Player)it.next()).getPlayerName();
				thisGame.setWinner(name);
				winner = name;
			} else {
				thisGame.setWinner("No winner");
			}
			m_botAction.SQLQuery("local", thisGame.getQuery());
			ResultSet results = m_botAction.SQLQuery("local", "SELECT fnGameID FROM tblElimRound ORDER BY fnGameID DESC");
			results.next();
			int gID = results.getInt("fnGameID");
			it = players.values().iterator();
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
	int kills;
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
	
	public void setKills(int k) {
		kills = k;
	}
	
	public void setWinner(String w) {
		winner = w;
	}
	
	public String getQuery() {
		int e = 0; if(isElim) e = 1;
		String query = "INSERT INTO tblElimRound (fnGameID, fnPlayers, fcGameType, fnToWin, fcWinner, fnElim, ftDate) VALUES(0, "+players+", '"+gameType+"', "+kills+", '"+winner+"', "+e+", NOW());";
		return query;
	}
}