package twcore.bots.estatsbot;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * Records statistics from elimination matches in a database.
 */
public class estatsbot extends SubspaceBot {

	HashMap<String, ElimPlayer> players;
	HashMap<String, ElimPlayer> lastPlayers;
	boolean gameRunning = false;
	ElimGame thisGame = null;
	ElimGame lastGame = null;
	String ref;
	Iterator<String> playerRatingIt;
	String getRating;
	boolean ratingBefore = false;

	public estatsbot(BotAction botAction) {
		super(botAction);
		players = new HashMap<String, ElimPlayer>();
		lastPlayers = new HashMap<String, ElimPlayer>();
		EventRequester events = m_botAction.getEventRequester();
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.PLAYER_DEATH);
		events.request(EventRequester.LOGGED_ON);
	}

	public void handleEvent(LoggedOn event) {
		m_botAction.joinArena(m_botAction.getBotSettings().getString("Arena"+m_botAction.getBotNumber()));
		ref = m_botAction.getBotSettings().getString("Ref"+m_botAction.getBotNumber());
	}

	public void handleEvent(PlayerDeath event) {
		if(!gameRunning) return;
        Player p1 = m_botAction.getPlayer(event.getKillerID());
        Player p2 = m_botAction.getPlayer(event.getKilleeID());;
        if( p1 == null || p2 == null )
            return;
		String killer = p1.getPlayerName();
		String killee = p2.getPlayerName();
		if(players.containsKey((killer.toLowerCase())))
			players.get(killer.toLowerCase()).addKill();
		if(players.containsKey((killee.toLowerCase())))
			players.get(killee.toLowerCase()).addDeath();
	}

	public void handleEvent(Message event) {
		if(event.getMessageType() == Message.ARENA_MESSAGE) {
			handleMessage(event.getMessage());
		} else if(event.getMessageType() == Message.PRIVATE_MESSAGE) {
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if(name.equalsIgnoreCase(ref)) {
				handlePM(event.getMessage());
			} else if(m_botAction.getOperatorList().isSmod(name) ) {
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
		if(message.startsWith("go! go! go!") && !gameRunning) {
			gameRunning = true;
			startGame();
		} else if(message.startsWith("game over:") && gameRunning) {
			gameRunning = false;
			endGame(message.substring(18));
		}
	}

	public void handlePM(String message) {
		if(gameRunning) {
			if(message.startsWith("Rating")) {
				String pieces[] = message.split(": ", 2);
				String rating = "";
				String pKills = "";
				String pDeaths = "";
				for(int k = 0;k < pieces[1].length();k++) {
					if(pieces[1].charAt(k) == '(') break;
					rating += pieces[1].charAt(k);
				}
				rating = rating.trim();
				String pieces2[] = message.split("Kills:", 2);
				for(int k = 0;k < pieces2[1].length();k++) {
					if(pieces2[1].charAt(k) == ' ') break;
					pKills += pieces2[1].charAt(k);
				}
				pKills = pKills.trim();
				String pieces3[] = message.split("Deaths:", 2);
				for(int k = 0;k < pieces3[1].length();k++) {
					if(pieces3[1].charAt(k) == ' ') break;
					pDeaths += pieces3[1].charAt(k);
				}
				pDeaths = pDeaths.trim();
				if(ratingBefore) {
					ElimPlayer p = players.get(getRating);
					p.ratingBefore(rating);
					if(playerRatingIt.hasNext()) {
						getRating = (String)playerRatingIt.next();
						m_botAction.sendPrivateMessage(ref, "!ranking " + getRating);
					} else {
						ratingBefore = false;
						playerRatingIt = lastPlayers.keySet().iterator();
						if(playerRatingIt.hasNext()) {
							getRating = (String)playerRatingIt.next();
							m_botAction.sendPrivateMessage(ref, "!ranking " + getRating);
						} else {
							updateSQL();
						}
					}
				} else {
					ElimPlayer p = (ElimPlayer)lastPlayers.get(getRating);
					p.ratingAfter(rating, pKills, pDeaths);
					if(playerRatingIt.hasNext()) {
						getRating = (String)playerRatingIt.next();
						m_botAction.sendPrivateMessage(ref, "!ranking " + getRating);
					} else {
						updateSQL();
					}
				}
			} else {
				try {
					String type = message.substring(15, message.indexOf(";"));
					thisGame.setType(type);
					playerRatingIt = players.keySet().iterator();
					ratingBefore = true;
					if(playerRatingIt.hasNext()) {
						getRating = (String)playerRatingIt.next();
						m_botAction.sendPrivateMessage(ref, "!ranking " + getRating);
					} else {
						ratingBefore = false;
						playerRatingIt = lastPlayers.keySet().iterator();
						if(playerRatingIt.hasNext()) {
							getRating = (String)playerRatingIt.next();
							m_botAction.sendPrivateMessage(ref, "!ranking " + getRating);
						} else {
							updateSQL();
						}
					}
				} catch(Exception e) {}
			}
		}
	}

	public void startGame() {
		try {
			lastPlayers = new HashMap<String,ElimPlayer>(players);
			players.clear();
			Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
			while(it.hasNext()) {
				String name = ((Player)it.next()).getPlayerName();
				players.put(name.toLowerCase(), new ElimPlayer(name));
			}
			if(thisGame != null)
				lastGame = new ElimGame(thisGame);
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

		} catch(Exception e) { Tools.printStackTrace(e); }
	}

	public void updateSQL() {
		if(lastGame == null) return;
		try {
            m_botAction.SQLQueryAndClose("local", lastGame.getQuery());
			ResultSet results = m_botAction.SQLQuery("local", "SELECT fnGameID FROM tblElimRound ORDER BY fnGameID DESC");
            if( !results.next() )
                return;
			int gID = results.getInt("fnGameID");
			int isElim = 0;
			if(!lastGame.isElim) isElim = 1;
			Iterator<ElimPlayer> it = lastPlayers.values().iterator();
			while(it.hasNext()) {
				ElimPlayer ep = (ElimPlayer)it.next();
                if( ep != null ) {
                    m_botAction.SQLQueryAndClose("local", ep.getQuery(gID, ep.name.equalsIgnoreCase(lastGame.winner), isElim));
			//	m_botAction.sendPrivateMessage("ikrit", ep.getQuery2(gID, ep.name.equalsIgnoreCase(lastGame.winner), isElim));
                    m_botAction.SQLQueryAndClose("local", ep.getQuery2(gID, ep.name.equalsIgnoreCase(lastGame.winner), isElim));
                }
			}
            m_botAction.SQLClose( results );
		} catch(Exception e) { Tools.printStackTrace(e); }
	}
}

class ElimPlayer {
	String name;
	int kills;
	int deaths;
	String rBefore, rAfter, killsFromBot, deathsFromBot;

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

	public void ratingBefore(String rating) {
		rBefore = rating;
	}

	public void ratingAfter(String rating, String kFB, String dFB) {
		rAfter = rating;
		killsFromBot = kFB;
		deathsFromBot = dFB;
	}

	public String getQuery(int gID, boolean won, int isElim) {
		int w = 0; if(won) w = 1;
		String query = "INSERT INTO tblElimRoundPlayer (fnGameID, fcUserName, fnKill, fnDeath, fnWon, ftDate, fnOldRating, fnNewRating) VALUES ("+gID+", '"+Tools.addSlashesToString(name)+"', "+kills+", "+deaths+", "+w+", NOW(), "+rBefore+", "+rAfter+");";
		return query;
	}

	public String getQuery2(int gID, boolean won, int isElim) {
		//int w = 0; if(won) w = 1;
		String query = "INSERT INTO tblElimPlayer (fcUserName, fnRating, fnKills, fnDeaths, fnElim, ftUpdated) VALUES('"+Tools.addSlashesToString(name)+"', "+rAfter+", "+killsFromBot+", "+deathsFromBot+", "+isElim+", NOW()) ON DUPLICATE KEY UPDATE fnKills = "+killsFromBot+", fnDeaths = "+deathsFromBot+", ftUpdated = NOW(), fnRating = "+rAfter;
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

	public ElimGame(ElimGame eg) {
		gameType = eg.gameType;
		players = eg.players;
		winner = eg.winner;
		isElim = eg.isElim;
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