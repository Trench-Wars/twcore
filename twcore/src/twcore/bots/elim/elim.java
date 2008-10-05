package twcore.bots.elim;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.TimerTask;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.text.NumberFormatter;

import twcore.core.BotAction;
import twcore.core.SubspaceBot;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.events.Message;
import twcore.core.events.ArenaJoined;
import twcore.core.events.LoggedOn;
import twcore.core.events.WeaponFired;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerPosition;
import twcore.core.events.PlayerLeft;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.FrequencyChange;
import twcore.core.game.Player;
import twcore.core.util.Tools;

public class elim extends SubspaceBot {
	
	public OperatorList opList;
	public String db = "website";
	public Random rand = new Random();
	public static DecimalFormat decForm = new DecimalFormat("0.##");
	public static NumberFormatter format = new NumberFormatter(decForm);
	
	//Class variables
	public int botMode = 1;
	public int shipType = -1;
	public int deaths = -1;
	public int shrap = 0;
	public double avg_rating = -1;
	public ElimPlayer winner;
	
	//Game collections
	public GameStatus game = new GameStatus();
	public TreeMap<String, CasualPlayer> casualPlayers = new TreeMap<String, CasualPlayer>();
	public TreeMap<String, ElimPlayer> elimPlayers = new TreeMap<String, ElimPlayer>();
	public TreeMap<String, ElimPlayer> lagouts = new TreeMap<String, ElimPlayer>();
	public TreeMap<String, ElimPlayer> losers = new TreeMap<String, ElimPlayer>();
	public TreeMap<Integer, Integer> votes = new TreeMap<Integer, Integer>();
	public ArrayList<String> enabled = new ArrayList<String>();
	public ArrayList<String> classicMode = new ArrayList<String>();

	
	//BotSettings variables
	public int cfg_minPlayers, cfg_maxDeaths, cfg_defaultShip, cfg_gameType, cfg_votingLength, cfg_waitLength, cfg_zone;
	public String cfg_arena;
	public ArrayList<Integer> cfg_shipTypes = new ArrayList<Integer>();
	
	//Defaults
	public int[] defaultElimTypes = {1,3,4,7,9};
	public int[] defaultBelimTypes = {2,5,6,8,9};
	public String[]  sMessages = {
			"On Fire!",
			"Killing Spree!",
			"Rampage!",
			"Dominating!",
			"Unstoppable!",
			"God Like!",
			"Cheater!",
		};
	
	//Enums
	public static final int ELIM = 1;
	public static final int BASEELIM = 2;
	public static final int OFF = 0;
	public static final int ON = 1;
	public static final int DISCONNECT = 2;
	public static final int SPAWN_TIME = 5005;
	public static final int LAGOUT_TIME = 30;
	
    public elim(BotAction botAction) {
        super(botAction);
        requestEvents();
        BotSettings cfg = m_botAction.getBotSettings();
        opList = m_botAction.getOperatorList();
        int botnum = getBotNumber(cfg);
        cfg_minPlayers = cfg.getInt("MinPlayers");
        cfg_maxDeaths = cfg.getInt("MaxDeaths");
        cfg_votingLength = cfg.getInt("VotingLength");
        cfg_waitLength = (cfg.getInt("WaitLength") - 10);
        cfg_zone = cfg.getInt("SendZoner");
        cfg_defaultShip = cfg.getInt("DefaultShip" + botnum);
        cfg_arena = cfg.getString("Arena" + botnum);
        cfg_gameType = cfg.getInt("GameType" + botnum);
        String[] types = cfg.getString("ShipTypes" + botnum).split(",");
        try{
	        for(int i=0;i<types.length;i++)
	        	cfg_shipTypes.add(Integer.parseInt(types[i]));        	
        }catch(NumberFormatException e){
        	if(cfg_gameType == ELIM){
        		for(int i=0;i<defaultElimTypes.length;i++)
    	        	cfg_shipTypes.add(defaultElimTypes[i]);    
        	} else {
        		for(int i=0;i<defaultBelimTypes.length;i++)
    	        	cfg_shipTypes.add(defaultBelimTypes[i]); 
        	}   	
        }       
    }
	
	public String[] getHelpMsg(String name){
		ArrayList<String> help = new ArrayList<String>();
		String[] reghelp = {
				"+=============================== HELP MENU ==================================+",
				"| !help         - Displays this menu. (?)                                    |",
				"| !play         - Toggles whether or not you wish to play. (!p)              |",
				"| !lagout       - Puts you back into the game if you've lagged out. (!l)     |",
				"| !status       - Displays the game status. (!s)                             |",
				"| !mvp          - Shows the three players with the best and worst recs.      |",
				"| !wl           - Shows the three players with the most and least losses.    |",
				"| !rank <#>     - Returns the player at rank <#>.                            |",
				"| !stats        - Shows your statistics including your rank.                 |",
				"| !stats <name> - Shows statistics of <name>.                                |",
				"| !rec          - Shows your wins and losses. (!r)                           |",
				"| !rec <name>   - Shows the wins and losses of <name>.                       |",
				"| !classic      - Toggles whether or not you'd like to be spec'd when out.   |",
				"| !warp         - Warps you out of the safe if you're stuck.                 |",
				"| !scorereset   - Resets your arena score card to zero. No going back. (!sr) |",

		};List<String> reg = Arrays.asList(reghelp);
		String[] smodhelp = {
				"+=============================== SMOD MENU ==================================+",
				"| !zone         - Toggles whether or not the bot should zone for games.      |",
				"| !stop         - Ends the current game and prevents new games from starting.|",
				"| !off          - Prevents new games from starting once the current one ends.|",
				"| !on           - Turns off !off mode and allows new games to begin          |",
				"| !shutdown     - Causes the bot to disconnect once the current game ends.   |",
				"| !die          - Causes the bot to disconnect.                              |",
		};List<String> smod = Arrays.asList(smodhelp);				
		help.addAll(reg);
		if(opList.isSmod(name))
			help.addAll(smod);
		if(opList.isSysop(name))
			help.add("| !greetmsg <m> - Sets arena greet message(Sysop only).                      |");
		help.add("+============================================================================+");
		return help.toArray(new String[help.size()]);

	}
	
	public String getStatusMsg(){
		switch(game.state){
			case GameStatus.OFF_MODE:
				return "Elimination is temporarily disabled.";
			case GameStatus.WAITING_FOR_PLAYERS:
				return "A new elimination match will begin when " + (cfg_minPlayers - elimPlayers.size()) + " more player(s) enter.";
			case GameStatus.VOTING_ON_SHIP:
				return "We are currently voting on ship type.";
			case GameStatus.VOTING_ON_DEATHS:
				return "We are currently voting on number of deaths.";
			case GameStatus.VOTING_ON_SHRAP:
				return "We are currently voting on shrap.";
			case GameStatus.WAITING_TO_START:
				return "The game will start soon. Enter to play!";
			case GameStatus.TEN_SECONDS:
				return "The game will begin in less than ten seconds. No more entries";
			case GameStatus.GAME_IN_PROGRESS:
				return "We are currently playing " + Tools.shipName(shipType) + " elim to " + deaths + ". " + elimPlayers.size() + " player(s) left.";
			case GameStatus.GAME_OVER:
				return "A new elimination match will begin shortly.";
			default: return null;
		}
	}
        
    public void handleEvent(Message event) {
    	String message = event.getMessage();
		String name = event.getMessager() == null ? m_botAction.getPlayerName(event.getPlayerID()) : event.getMessager();
		int messageType = event.getMessageType();
		
		if(messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE){
			handleCommands(name, message);
			if(opList.isSmod(name))
				handleSmodCommands(name, message);
			if(opList.isSysop(name) && message.startsWith("!greetmsg "))
				m_botAction.sendUnfilteredPublicMessage( "?set misc:greetmessage:"+message.substring(10) );
		}				
		else if(messageType == Message.ARENA_MESSAGE && message.equals("Arena LOCKED"))
			m_botAction.toggleLocked();
		else if(messageType == Message.ARENA_MESSAGE && message.startsWith("Name:") && message.contains("UserID:"))
			doEInfo(message);
		else if(messageType == Message.PUBLIC_MESSAGE &&
				game.isVoting()                       &&
				Tools.isAllDigits(message)            &&
				message.length() <= 2                 &&
				elimPlayers.containsKey(name))
			vote(name, message);
		else if(messageType == Message.PUBLIC_MESSAGE &&
				game.isVoting()                       &&
				Tools.isAllDigits(message)            &&
				message.length() <= 2                 &&
				!elimPlayers.containsKey(name))
			m_botAction.sendSmartPrivateMessage( name, "You must be playing to vote!");
    }
    
    public void handleCommands(String name, String cmd){
    	if(cmd.equalsIgnoreCase("!help") || cmd.equals("?"))
    		m_botAction.smartPrivateMessageSpam(name, getHelpMsg(name));
    	else if(cmd.equalsIgnoreCase("!play") || cmd.equalsIgnoreCase("!p") || cmd.equals(""))
    		cmd_play(name);
    	else if(cmd.equalsIgnoreCase("!status") || cmd.equalsIgnoreCase("!s"))
    		m_botAction.sendSmartPrivateMessage( name, getStatusMsg());
    	else if(cmd.equalsIgnoreCase("!mvp"))
    		cmd_mvp(name);
    	else if(cmd.equalsIgnoreCase("!wl"))
    		cmd_wl(name);
    	else if(cmd.equalsIgnoreCase("!rec"))
    		cmd_rec(name, name);
    	else if(cmd.startsWith("!rec "))
    		cmd_rec(name, cmd.substring(5));
    	else if(cmd.equalsIgnoreCase("!classic") || cmd.equalsIgnoreCase("!c"))
    		cmd_classic(name);
    	else if(cmd.equalsIgnoreCase("!warp") || cmd.equalsIgnoreCase("!w"))
    		cmd_warp(name);
    	else if(cmd.equalsIgnoreCase("!lagout") || cmd.equalsIgnoreCase("!l"))
    		cmd_lagout(name);
    	else if(cmd.equalsIgnoreCase("!scorereset") || cmd.equalsIgnoreCase("!sr"))
    		cmd_scorereset(name);
    	else if(cmd.equalsIgnoreCase("!stats"))
    		cmd_stats(name, name);
    	else if(cmd.startsWith("!stats "))
    		cmd_stats(name, cmd.substring(7));
    	else if(cmd.startsWith("!rank "))
    		cmd_rank(name, cmd.substring(6));
    }
    
    public void handleSmodCommands(String name, String cmd){
    	if(cmd.equalsIgnoreCase("!zone"))
    		cmd_zone(name);
    	else if(cmd.equalsIgnoreCase("!stop"))
    		cmd_stop(name);
    	else if(cmd.equalsIgnoreCase("!off"))
    		cmd_off(name, false);
    	else if(cmd.equalsIgnoreCase("!on"))
    		cmd_on(name);
    	else if(cmd.equalsIgnoreCase("!shutdown"))
    		cmd_off(name, true);
    	else if(cmd.equalsIgnoreCase("!die"))
    		m_botAction.scheduleTask(new DieTask(name), 500);    		
    }
    
    public void cmd_play(String name){
    	if(enabled.remove(name)){
    		m_botAction.sendSmartPrivateMessage( name, "You have disabled !play. Type !play again to compete.");
    		if(elimPlayers.containsKey(name) && game.state == GameStatus.GAME_IN_PROGRESS){
    			m_botAction.sendArenaMessage(name + " is out. " + elimPlayers.get(name).wins + " wins " + elimPlayers.get(name).losses + " losses (Resigned)");
        		losers.put(name, elimPlayers.remove(name));        		
    		}
    		else if(elimPlayers.containsKey(name))
    			elimPlayers.remove(name);
    		doWarpIntoCasual(name);
    	}else {
    		m_botAction.sendSmartPrivateMessage( name, "You have enabled !play. Type !play again to play casually.");
    		enabled.add(name);
    		Player p = m_botAction.getPlayer(name);
    		if(p.getShipType() > 0 && !game.isInProgress()){
    			elimPlayers.put(name, new ElimPlayer(name));
    			doWarpIntoElim(name);
    			if(elimPlayers.size() >= cfg_minPlayers && game.state == GameStatus.WAITING_FOR_PLAYERS)
        			game.moveOn();
    		}    		
    	}
    }
    
    public void cmd_mvp(String name){
    	CompareByWinRatio byRatio = new CompareByWinRatio();
    	List<ElimPlayer> l = Arrays.asList(elimPlayers.values().toArray(new ElimPlayer[elimPlayers.values().size()]));
    	Collections.sort(l, Collections.reverseOrder(byRatio));
    	int index = 1;
    	Iterator<ElimPlayer> i = l.iterator();
    	m_botAction.sendSmartPrivateMessage( name, "------------- Best Records ------------");
    	while( i.hasNext() && index <= 3){
    		ElimPlayer p = i.next();
    		m_botAction.sendSmartPrivateMessage( name, index + ") " + p.name + " (" + p.wins + "-" + p.losses + ")");
    		index++;
    	}
    	Collections.sort(l, byRatio);
    	i = l.iterator();
    	index = 1;
    	m_botAction.sendSmartPrivateMessage( name, "------------ Worst Records ------------");
    	while( i.hasNext() && index <= 3){
    		ElimPlayer p = i.next();
    		m_botAction.sendSmartPrivateMessage( name, index + ") " + p.name + " (" + p.wins + "-" + p.losses + ")");
    		index++;
    	}
    }
    
    public void cmd_wl(String name){
    	CompareByLosses byLosses = new CompareByLosses();
    	List<ElimPlayer> l = Arrays.asList(elimPlayers.values().toArray(new ElimPlayer[elimPlayers.values().size()]));
    	Collections.sort(l, Collections.reverseOrder(byLosses));
    	int index = 1;
    	Iterator<ElimPlayer> i = l.iterator();
    	m_botAction.sendSmartPrivateMessage( name, "------- Most Deaths ---- Limit: " + deaths + "-------");
    	while( i.hasNext() && index <= 3){
    		ElimPlayer p = i.next();
    		m_botAction.sendSmartPrivateMessage( name, index + ") " + p.name + " (" + p.wins + "-" + p.losses + ")");
    		index++;
    	}
    	Collections.sort(l, byLosses);
    	i = l.iterator();
    	index = 1;
    	m_botAction.sendSmartPrivateMessage( name, "------ Least Deaths --- Players: " + elimPlayers.size() + "------");
    	while( i.hasNext() && index <= 3){
    		ElimPlayer p = i.next();
    		m_botAction.sendSmartPrivateMessage( name, index + ") " + p.name + " (" + p.wins + "-" + p.losses + ")");
    		index++;
    	}
    }
    
    public void cmd_rec(String name, String target){
    	String clone = m_botAction.getFuzzyPlayerName(target);
    	if(clone == null)clone = target;
    	try{
    		ResultSet rs = m_botAction.SQLQuery(db, "SELECT fnKills, fnDeaths FROM tblElimCasualRecs WHERE fcUserName = '" + Tools.addSlashesToString(clone.toLowerCase()) + "'");
    		if(rs != null && rs.next()){
    			int kills = rs.getInt("fnKills");
    			int deaths = rs.getInt("fnDeaths");
    			m_botAction.SQLClose(rs);
    			m_botAction.sendSmartPrivateMessage( name, "'" + clone + "' (" + kills + "-" + deaths + ")");
    		} else {
    			m_botAction.SQLClose(rs);
    			m_botAction.sendSmartPrivateMessage( name, "No record was found for '" + clone + "'.");
    		}
    	}catch(SQLException e){
    		m_botAction.sendSmartPrivateMessage( name, "Error retrieving record. Please try again later. If problem persists contact a staff member.");
    	}
    }
    
    public void cmd_lagout(String name){
    	if(game.state != GameStatus.GAME_IN_PROGRESS)return;
    	if(lagouts.containsKey(name)){
    		if(System.currentTimeMillis() - lagouts.get(name).lagTime > LAGOUT_TIME * Tools.TimeInMillis.SECOND){
		    	elimPlayers.put(name, lagouts.remove(name));
		    	if(!enabled.contains(name))
		    		enabled.add(name);
		    	m_botAction.sendSmartPrivateMessage( name, "You have been put back into the game. You have " + (3-elimPlayers.get(name).lagouts) + " lagouts left.");
		    	m_botAction.setShip(name, elimPlayers.get(name).shiptype);
		    	m_botAction.setFreq(name, elimPlayers.get(name).frequency);
		    	doWarpIntoElim(name);
    		} else
    			m_botAction.sendSmartPrivateMessage( name, "You must wait for " + (LAGOUT_TIME-((System.currentTimeMillis() - lagouts.get(name).lagTime)/Tools.TimeInMillis.SECOND)) + " more seconds.");
    	} else
    		m_botAction.sendSmartPrivateMessage( name, "You aren't in the game!");    		
    }
    
    public void cmd_scorereset(String name){
    	try{
    		m_botAction.SQLQueryAndClose(db, "UPDATE tblElimCasualRecs SET fnKills = 0, fnDeaths = 0 WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "'");
    		m_botAction.sendSmartPrivateMessage( name, "Your wins and losses have been reset to zero.");
    	}catch(SQLException e){
    		m_botAction.sendSmartPrivateMessage( name, "Error resetting score. Please try again later. If the problem persists contact a staff member.");
    	}
    }
    
    public void cmd_classic(String name){
    	int wantsClassic = 0;
    	if(classicMode.remove(name))
    		m_botAction.sendSmartPrivateMessage( name, "You will be moved to the casual arena when you are out.");
    	else {
    		wantsClassic = 1;
    		classicMode.add(name);
    		m_botAction.sendSmartPrivateMessage( name, "You will be specced when you are out.");
    	}
    	try{
    		m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnSpecWhenOut = " + wantsClassic + " WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    	}catch(Exception e){
    		Tools.printStackTrace(e);
    	}
    }
    
    public void cmd_warp(String name){
    	Player p = m_botAction.getPlayer(name);
    	if(p == null)return;
    	if((p.getYTileLocation() > 368 && p.getYTileLocation() < 394 && cfg_gameType == BASEELIM) ||
    	   (p.getYTileLocation() > 411 && p.getYTileLocation() < 437 && cfg_gameType == ELIM))
    		m_botAction.specificPrize(name, Tools.Prize.WARP);
    	else
    		m_botAction.sendSmartPrivateMessage( name, "This command is for players in the safe.");
    }
    
    public void cmd_stats(String name, String target){
    	String t = m_botAction.getFuzzyPlayerName(target);
    	if(t != null)
    		target = t;
    	try{
    		ResultSet rs = m_botAction.SQLQuery(db, "SELECT * FROM tblElimPlayer WHERE fcUserName = '" + Tools.addSlashesToString(target.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    		if( rs != null && rs.next() ){
    			int ranking = rs.getInt("fnRank");
    			int rating = rs.getInt("fnRating");
    			int kills = rs.getInt("fnKills");
    			int deaths = rs.getInt("fnDeaths");
    			int shots = rs.getInt("fnShots");
    			int cks = rs.getInt("fnCKS");
    			int cws = rs.getInt("fnCWS");
    			int bks = rs.getInt("fnBKS");
    			int bws = rs.getInt("fnBWS");
    			int gamesWon = rs.getInt("fnGamesWon");
    			int gamesPlayed = rs.getInt("fnGamesPlayed");
    			m_botAction.SQLClose(rs);
    			double w = kills, l = deaths, s = shots, won = gamesWon, plyd = gamesPlayed;
    			double winRatioNum = (w/l) * 100;
    			double aimNum = (w/s) * 100;
    			double victorRatioNum = (won/plyd) * 100;
    			String winRatio = format.valueToString(winRatioNum);
    			String aim = format.valueToString(aimNum);
    			String victorRatio = format.valueToString(victorRatioNum);
    			String rank;
    			if(ranking == 0)
    				rank = "Not ranked";
    			else
    				rank = "#" + ranking;
    			m_botAction.sendSmartPrivateMessage( name, "=== Player: '" + target + "' Rank: " + rank + " Rating: " + rating + " ===");
    			m_botAction.sendSmartPrivateMessage( name, "Kills: " + kills + " Deaths: " + deaths + " Ratio: (" + winRatio + "%)" + " AIM:" + aim + "% CKS:" + cks + " BKS:" + bks + " CWS:" + cws + " BWS:" + bws);
    			m_botAction.sendSmartPrivateMessage( name, "Games Won: " + gamesWon + " Games Played: " + gamesPlayed + " Win Ratio: " + victorRatio);
    		} else {
    			m_botAction.SQLClose(rs);
    			m_botAction.sendSmartPrivateMessage( name, "User '" + target + "' not found.");	
    		}
    	} catch(Exception e){
    		m_botAction.sendSmartPrivateMessage( name, "There was a problem handling your request. Please try again in a minute.");
    	}
    }
    
    public void cmd_rank(String name, String message){
    	int rank;
    	try{
    		rank = Integer.parseInt(message);
    	}catch(NumberFormatException e){
    		m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !rank 1");
    		return;
    	}
    	try{
    		if(rank < 3)rank = 3;
    		ResultSet rs = m_botAction.SQLQuery(db, "SELECT fnRank, fnRating, fcUserName FROM tblElimPlayer WHERE fnGameType = " + cfg_gameType + " AND fnRank BETWEEN " + (rank - 2) + " AND " + (rank + 2) + " ORDER BY fnRank ASC");
    		while(rs != null && rs.next()){
    			int rsRank = rs.getInt("fnRank");
    			int rsRating = rs.getInt("fnRating");
    			String rsName = rs.getString("fcUserName");
    			m_botAction.sendSmartPrivateMessage( name, "#" + rsRank + ") " + rsName + " - " + rsRating);
    		}
    		m_botAction.SQLClose(rs);
    	}catch(Exception e){
    		Tools.printStackTrace(e);
    	}
    }
    
    public void cmd_zone(String name){
    	if(cfg_zone == ON){
    		cfg_zone = OFF;
    		m_botAction.sendSmartPrivateMessage( name, "The bot will no longer zone at the start of elimination matches.");
    	}else{
    		cfg_zone = ON;
    		m_botAction.sendSmartPrivateMessage( name, "The bot will now zone at the start of elimination matches.");
    	}
    }
    
    public void cmd_stop(String name){
    	if(game.state != GameStatus.OFF_MODE){
    		m_botAction.sendArenaMessage("This game has been brutally killed by " + name);
    		game.state = GameStatus.OFF_MODE;
    		Iterator<String> i = elimPlayers.keySet().iterator();
    		while( i.hasNext() ){
    			doWarpIntoCasual(i.next());
    			i.remove();
    		}
    		lagouts.clear();
    		losers.clear();
    		votes.clear();
    		m_botAction.cancelTasks();
    	}
    	else
    		m_botAction.sendSmartPrivateMessage( name, "There is currently no game in progress.");
    }
    
    public void cmd_off(String name, boolean shouldDisconnect){
    	if((botMode == OFF && !shouldDisconnect) || (botMode == DISCONNECT && shouldDisconnect)){
    		m_botAction.sendSmartPrivateMessage( name, "The bot is already set for that task.");
    		return;
    	}
    	if(!shouldDisconnect){
    		botMode = OFF;
    		m_botAction.sendSmartPrivateMessage( name, "New games will be prevented from starting once the current game ends.");
    	}else{
    		botMode = DISCONNECT;
    		if(game.state == GameStatus.OFF_MODE || game.state == GameStatus.WAITING_FOR_PLAYERS)
    			m_botAction.scheduleTask(new DieTask(name), 500);
    		else
    			m_botAction.sendSmartPrivateMessage( name, "The bot will disconnect once the current game ends.");
    	}
    }
    
    public void cmd_on(String name){
    	if(botMode == ON){
    		m_botAction.sendSmartPrivateMessage( name, "The bot is already on.");
    		return;
    	}
    	if(botMode == OFF)
    		m_botAction.sendSmartPrivateMessage( name, "Off mode disabled. New games will be allowed to start.");
    	else
    		m_botAction.sendSmartPrivateMessage( name, "Shutdown mode disabled. New games will be allowed to start.");
    	botMode = ON;
    }
    
    public void doEInfo(String message){
    	String name, userid;
    	int IDindex, ID;
    	IDindex = message.indexOf("UserId:");
    	name = message.substring(0,IDindex-2);
    	userid = message.substring(IDindex + 8, message.indexOf("  Res:"));
    	try{
    		ID = Integer.parseInt(userid);
    		ResultSet rs = m_botAction.SQLQuery(db, "SELECT * FROM tblElimJavsRecs WHERE fnUserID = " + ID);
    		if(rs != null && rs.next()){
    			int kills = rs.getInt("fnKills"), deaths = rs.getInt("fnDeaths");
    			m_botAction.SQLClose(rs);
    			m_botAction.SQLQueryAndClose(db, "DELETE FROM tblElimJavsRecs WHERE fnUserID = " + ID);
    			m_botAction.SQLQueryAndClose(db, "INSERT INTO tblElimCasualRecs (fcUserName, fnKills, fnDeaths) VALUES (" + Tools.addSlashesToString(name.toLowerCase()) + ", " + kills + ", " + deaths + ")");
    			m_botAction.sendSmartPrivateMessage( name, "Your record has been updated from ?go javs. PM me with !rec to view your wins and losses.");
    		}
    		else m_botAction.SQLClose(rs);
    	}catch(Exception e){}
    	casualPlayers.put(name, new CasualPlayer(name));
    }
    
    public void doWaitingForPlayers(){
    	game.state = GameStatus.WAITING_FOR_PLAYERS;
    	int neededPlayers = cfg_minPlayers - elimPlayers.size();
    	if(neededPlayers <= 0)
    		game.moveOn();
    	else
    		m_botAction.sendArenaMessage("A new elimination match will begin when " + neededPlayers + " more player(s) enter.");
    }
    
    public void doVotingOnShip(){
    	game.state = GameStatus.VOTING_ON_SHIP;
    	shipType = cfg_defaultShip;
    	if(cfg_zone == ON)
    		m_botAction.sendZoneMessage("Next elimination match is starting. Type ?go " + cfg_arena + " to play");
    	Iterator<String> it = enabled.iterator();
    	while( it.hasNext() )
    		doWarpIntoElim(it.next());
    	String s = "Vote: ";
    	for(int i=0;i<cfg_shipTypes.size();i++){
    		s += Tools.shipName(cfg_shipTypes.get(i)) + " - " + cfg_shipTypes.get(i) + ", ";
    		votes.put(cfg_shipTypes.get(i),0);
    	}
    	s = s.substring(0, s.length()-2);
    	m_botAction.sendArenaMessage(s);
    	game.moveOn(cfg_votingLength * Tools.TimeInMillis.SECOND);
    	
    }
    
    public void doVotingOnDeaths(){
    	game.state = GameStatus.VOTING_ON_DEATHS;
    	for(int i=1;i<=cfg_maxDeaths;i++)
    		votes.put(i, 0);
    	game.moveOn(cfg_votingLength * Tools.TimeInMillis.SECOND);
    }
    
    public void doVotingOnShrap(){
    	game.state = GameStatus.VOTING_ON_SHRAP;
    	votes.put(0,0);
    	votes.put(1,0);
    	game.moveOn(cfg_votingLength * Tools.TimeInMillis.SECOND);
    }
    
    public void doWaitingToStart(){
    	game.state = GameStatus.WAITING_TO_START;
    	m_botAction.sendArenaMessage("Enter to play. Game will begin in " + (cfg_waitLength + 10) + " seconds");
    	if(deaths == 1)
    		m_botAction.sendArenaMessage("Rules: All on own freq, no teaming! Die "+ deaths + " time and you are out");
    	else
    		m_botAction.sendArenaMessage("Rules: All on own freq, no teaming! Die "+ deaths + " times and you are out");
    	game.moveOn(cfg_waitLength * Tools.TimeInMillis.SECOND);
    }
    
    public void doTenSeconds(){
    	game.state = GameStatus.TEN_SECONDS;
    	m_botAction.sendArenaMessage("Get ready. Game will start in 10 seconds");//No new entries
    	int freq = 600;
    	Iterator<ElimPlayer> i = elimPlayers.values().iterator();
    	while(i.hasNext()){
    		ElimPlayer ep = i.next();
    		Player p = m_botAction.getPlayer(ep.name);
    		if(p == null){
    			i.remove();
    			continue;
    		}
    		ep.frequency = freq;
    		m_botAction.setFreq(ep.name, freq);
    		ep.shiptype = p.getShipType();
    		if(shipType == 9){
    			if(!cfg_shipTypes.contains(p.getShipType()))
    				m_botAction.setShip(p.getPlayerName(), cfg_defaultShip);	
    		}else if(p.getShipType() != shipType){
    			ep.shiptype = shipType;
    			m_botAction.setShip(p.getPlayerName(), shipType);
    		}
    		
    		doWarpIntoElim(ep.name);
    		freq++;
    	}
    	if(elimPlayers.size() >= 30)
    		m_botAction.setDoors("00001000");
    	else
    		m_botAction.setDoors(255);
    	game.moveOn(10 * Tools.TimeInMillis.SECOND);
    }
    
    public void doGameInProgress(){
    	game.state = GameStatus.GAME_IN_PROGRESS;
    	lagouts.clear();
    	losers.clear();
    	avg_rating = 0;
    	m_botAction.sendArenaMessage("GO! GO! GO!", Tools.Sound.GOGOGO);
    	Iterator<ElimPlayer> i = elimPlayers.values().iterator();
    	while(i.hasNext()){
    		ElimPlayer ep = i.next();
    		ep.gotChangeWarning = false;
    		if(shrap == OFF)
    			m_botAction.specificPrize(ep.name, -Tools.Prize.SHRAPNEL);
    		ep.resetScore();
    		m_botAction.scoreReset(ep.name);
    		try{
    			boolean newPlayer = false;
    			ResultSet rs = m_botAction.SQLQuery(db, "SELECT * FROM tblElimPlayer WHERE fcUserName = '" + Tools.addSlashesToString(ep.name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    			if(rs != null && rs.next()){
    				ep.initRating = rs.getInt("fnRating");
    				ep.streak = rs.getInt("fnCKS");
    			}
    			else {
    				newPlayer = true;
    				ep.initRating = 1000;
    			}
    			m_botAction.SQLClose(rs);
    			if(newPlayer)
    				m_botAction.SQLQueryAndClose(db, "INSERT INTO tblElimPlayer (fcUserName, fnGameType, fnRating, fnElim, ftUpdated) VALUES ('" + Tools.addSlashesToString(ep.name.toLowerCase()) + "'," + cfg_gameType + ",1000,1,NOW())");
    			avg_rating += ep.initRating;
    		}catch(SQLException e){
    			Tools.printStackTrace(e);
    		}
    	}
    	avg_rating /= elimPlayers.size();
    }
    
    public void doGameOver(){
    	game.state = GameStatus.GAME_OVER;
    	CompareByWinRatio byWinRatio = new CompareByWinRatio();
    	Iterator<ElimPlayer> i = lagouts.values().iterator();
    	while( i.hasNext() ){
    		ElimPlayer ep = i.next();
    		losers.put(ep.name, ep);
    	}
    	lagouts.clear();
    	losers.put(winner.name, winner);
    	i = losers.values().iterator();
    	while( i.hasNext() ){
    		ElimPlayer ep = i.next();
    		ep.calculateStats();
    		try{
    			String lastWinner = "null";
    			ResultSet rs = m_botAction.SQLQuery(db, "SELECT fcWinnerName FROM tblElimGame WHERE fnGameType = " + cfg_gameType + " ORDER BY fnGameID DESC LIMIT 1");
    			if( rs != null && rs.next())
    				lastWinner = rs.getString("fcWinnerName");
    			m_botAction.SQLClose(rs);
    			if(ep.name.equalsIgnoreCase(winner.name)){
    				if(lastWinner.equalsIgnoreCase(ep.name))
    					m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnRating = fnRating + " + ep.ratingChange + ", fnGamesWon = fnGamesWon + 1, fnGamesPlayed = fnGamesPlayed + 1, fnKills = fnKills + " + ep.wins + ", fnDeaths = fnDeaths + " + ep.losses + ", fnShots = fnShots + " + ep.shots + ", fnCKS = " + ep.streak + ", fnCWS = fnCWS + 1, fnBWS = (CASE WHEN (fnCWS > fnBWS) THEN fnCWS ELSE fnBWS END) WHERE fcUserName = '" + Tools.addSlashesToString(ep.name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    				else
    					m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnRating = fnRating + " + ep.ratingChange + ", fnGamesWon = fnGamesWon + 1, fnGamesPlayed = fnGamesPlayed + 1, fnKills = fnKills + " + ep.wins + ", fnDeaths = fnDeaths + " + ep.losses + ", fnShots = fnShots + " + ep.shots + ", fnCKS = " + ep.streak + ", fnCWS = 1 WHERE fcUserName = '" + Tools.addSlashesToString(ep.name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    			}else
    				m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnRating = fnRating + " + ep.ratingChange + ", fnGamesPlayed = fnGamesPlayed + 1, fnKills = fnKills + " + ep.wins + ", fnDeaths = fnDeaths + " + ep.losses + ", fnShots = fnShots + " + ep.shots + ", fnCKS = " + ep.streak + ", fnCWS = 0 WHERE fcUserName = '" + Tools.addSlashesToString(ep.name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    		}catch(SQLException e){
    			Tools.printStackTrace(e);
    		}
    	}
	    List<ElimPlayer> l = Arrays.asList(losers.values().toArray(new ElimPlayer[losers.values().size()]));
	    Collections.sort(l, byWinRatio);//Best record
	    new MVPTimer(l.get(0).name);
	    m_botAction.sendArenaMessage("GAME OVER. Winner: " + winner.name + "!", Tools.Sound.HALLELUJAH);
    	Iterator<String> s = enabled.iterator();
    	while( s.hasNext() ){
    		String playerName = s.next();
    		Player p = m_botAction.getPlayer(playerName);
    		if(p == null || p.getShipType() == 0)continue;
    		elimPlayers.put(playerName, new ElimPlayer(playerName));
    		doWarpIntoElim(playerName);
    	}
    	try{
    		m_botAction.SQLQueryAndClose(db, "INSERT INTO tblElimGame (fnGameType, fcWinnerName, fnShipType, fnDeaths, fnNumPlayers, fnAvgRating, fdPlayed) VALUES( " + cfg_gameType + ", '" + Tools.addSlashesToString(winner.name.toLowerCase()) + "', " + shipType + ", " + deaths + ", " + losers.size() + ", " + avg_rating + ", NOW())");
    		ResultSet rs = m_botAction.SQLQuery(db, "SELECT fcUserName FROM tblElimPlayer WHERE fnGameType = " + cfg_gameType + " ORDER BY fnRating DESC");
    		TreeMap<String, Integer> rankings = new TreeMap<String, Integer>();
    		int rank = 1;
    		while( rs != null && rs.next() ){
    			rankings.put(rs.getString("fcUserName"), rank);
    			rank++;
    		}
    		m_botAction.SQLClose(rs);
    		Iterator<String> r = rankings.keySet().iterator();
    		while( r.hasNext() ){
    			String name = r.next();
    			m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnRank = " + rankings.get(name) + " WHERE fnGameType = " + cfg_gameType + " AND fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "'");
    		}    		
    	}catch(SQLException e){
    		Tools.printStackTrace(e);
    	}
    	game.moveOn(30 * Tools.TimeInMillis.SECOND);
    }
    
    public void vote(String name, String message){
    	int vote = 0;
    	ElimPlayer player;
    	try{
    		vote = Integer.parseInt(message);
    	} catch(NumberFormatException e){
    		Tools.printStackTrace(e);//This should never happen
    	}
    	if(elimPlayers.containsKey(name))
    		player = elimPlayers.get(name);
    	else return;
    	if(votes.containsKey(vote)){
    		if(player.vote != -1)
        		votes.put(player.vote, votes.get(player.vote) - 1);
    		player.vote = vote;
			votes.put(vote, votes.get(vote) + 1);
    	}    	
    }
    
    public void countVotes(){
    	int max = 0;
		ArrayList<Integer> winners = new ArrayList<Integer>();		
		Iterator<Integer> i = votes.keySet().iterator();
		while( i.hasNext() ){
			int x = i.next();
			if( votes.get(x) > max ){
				max = votes.get(x);
				winners.clear();
				winners.add(x);
			}
			else if( votes.get(x) == max && max != 0) winners.add(x);
		}
		if(game.state == GameStatus.VOTING_ON_SHIP){//The ship vote
			if(winners.isEmpty()){
				shipType = cfg_defaultShip;
			}else if(winners.size() > 1){
				shipType = winners.get(rand.nextInt(winners.size()-1));
			}else
				shipType = winners.get(0);
			m_botAction.sendArenaMessage("This will be " + Tools.shipName(shipType) + " elim. VOTE: How many deaths? (1-" + cfg_maxDeaths + ")");
		} else if(game.state == GameStatus.VOTING_ON_DEATHS){//The deaths vote
			if(winners.isEmpty())
				deaths = cfg_maxDeaths;
			else if(winners.size() > 1)
				deaths = winners.get(rand.nextInt(winners.size()-1));
			else
				deaths = winners.get(0);
			if(cfg_gameType == ELIM || (shipType != 2 && shipType != 8))
				m_botAction.sendArenaMessage(Tools.shipName(shipType) + " elim to " + deaths);
			else
				m_botAction.sendArenaMessage(Tools.shipName(shipType) + " elim to " + deaths + ". VOTE: Shrap on or off? (1-on, 0-off)");
		} else if(game.state == GameStatus.VOTING_ON_SHRAP){//Shrap vote(Only for baseelim)
			if(winners.isEmpty() || winners.size() > 1)
				shrap = OFF;
			else
				shrap = winners.get(0);
			if(shrap == OFF)
				m_botAction.sendArenaMessage(Tools.shipName(shipType) + " elim to " + deaths + ". Shrap: [OFF]");
			else
				m_botAction.sendArenaMessage(Tools.shipName(shipType) + " elim to " + deaths + ". Shrap: [ON]");
		}
		//Clear votes
		votes = new TreeMap<Integer,Integer>();
		Iterator<ElimPlayer> it = elimPlayers.values().iterator();
		while( it.hasNext() )
			it.next().vote = -1;
    }
    
    public void doWarpIntoElim(String name){
    	if(cfg_gameType == BASEELIM){
    		if(shipType == 6){
    			if(rand.nextInt(1) == 0)
    			m_botAction.warpTo(name, 497, 448, 6);
    			else
    				m_botAction.warpTo(name, 527, 448, 6);
    		}
    		else
    			m_botAction.warpTo(name, 512, 600, 40);
    	} else
    		m_botAction.warpTo(name, 512, 734, 175);
    }
    
    public void doWarpIntoCasual(String name){
    	if(classicMode.contains(name))
    		m_botAction.specWithoutLock(name);
    	else{
	    	for(int i=0;i<9999;i++){
	    		if(m_botAction.getFrequencySize(i) == 0){
	    			m_botAction.setFreq(name, i);
	    			break;
	    		}    			
	    	}
	    	if(cfg_gameType == BASEELIM)
	    		m_botAction.warpTo(name, 512, 381, 7);
	    	else
	    		m_botAction.warpTo(name, 512, 424, 7);
	    	m_botAction.sendSmartPrivateMessage( name, "You're out! PM me with !warp to play casually or hang out in here until the next game.");
    	}
    }
    

private class CasualPlayer{
	private int wins = -1, losses = -1;
	private String name;
	
	private CasualPlayer(String name){
		this.name = name;
		try{
			ResultSet rs = m_botAction.SQLQuery(db, "SELECT fnKills, fnDeaths FROM tblElimCasualRecs WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "'");
			if(rs != null && rs.next()){
				wins = rs.getInt("fnKills");
				losses = rs.getInt("fnDeaths");
				m_botAction.SQLClose(rs);
			} else {
				m_botAction.SQLClose(rs);
				m_botAction.SQLQueryAndClose(db, "INSERT INTO tblElimCasualRecs (fcUserName, fnKills, fnDeaths) VALUES ('" + Tools.addSlashesToString(name.toLowerCase()) + "',0,0)");
				wins = 0;
				losses = 0;
			}
		}catch(SQLException e){
			Tools.printStackTrace(e);
		}
	}
	
	private void gotWin(){
		wins++;
	}
	
	private void gotLoss(){
		losses++;
	}
	
	private void storeStats(){
		try{
			m_botAction.SQLQueryAndClose(db, "UPDATE tblElimCasualRecs SET fnKills = fnKills + " + wins + ", fnDeaths = fnDeaths + " + losses + " WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "'" );
		}catch(SQLException e){
			Tools.printStackTrace(e);
		}
	}
}
    
private class ElimPlayer{
	private String name;
	private int shiptype = -1, vote = -1, frequency = -1;
	private int wins = 0, losses = 0, shots = 0, initRating = 0, streak = 0, BKS = 0, lagouts = 0;
	private double hitRatio = 0, winRatio = 100, ratingChange = 0, w, l, s;
	private long outOfBounds = 0, lagTime = -1;
	private boolean gotBorderWarning = false, gotChangeWarning = false;
	private static final int MAX_BONUS = 25;
	
	private ElimPlayer(String name){
		this.name = name;
		
	}
	
	private void resetScore(){
		wins = 0;
		losses = 0;
		lagouts = 0;
	}
	
	private void gotWin(){
		wins += 1;
		streak += 1;
		switch(streak){
		case 5:
			m_botAction.sendArenaMessage(name + " - " + sMessages[0] + "(" + streak + ":0)");
			break;
		case 7:
			m_botAction.sendArenaMessage(name + " - " + sMessages[1] + "(" + streak + ":0)");
			break;
		case 9:
			m_botAction.sendArenaMessage(name + " - " + sMessages[2] + "(" + streak + ":0)");
			break;
		case 11:
			m_botAction.sendArenaMessage(name + " - " + sMessages[3] + "(" + streak + ":0)");
			break;
		case 13:
			m_botAction.sendArenaMessage(name + " - " + sMessages[4] + "(" + streak + ":0)");
			break;
		case 15:
			m_botAction.sendArenaMessage(name + " - " + sMessages[5] + "(" + wins + ":0)");
			break;
		case 17:
			m_botAction.sendArenaMessage(name + " - " + sMessages[6] + "(" + wins + ":0)");
			break;
		}
		if(streak > BKS){
			BKS = streak;
			try{
				m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnBKS = " + streak + " WHERE fnGameType = " + cfg_gameType + " AND fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "'");
			}catch(SQLException e){
				Tools.printStackTrace(e);
			}
		}
	}
	
	private void gotLoss(){
		losses += 1;
		streak = 0;
	}
	
	private void calculateStats(){
		calculateRatios();
		double x = avg_rating - initRating;//Average Player Rating - Player Rating
		double p = 1/(1 + Math.pow(10, (x/400)));//Probability
		double z = (((winRatio-70) * 3) / losers.size());
		if(z > 0)
			ratingChange =  z * (1-p);
		else if(z < 0)
			ratingChange =  z * (p);
		else
			ratingChange = z;
		if(winner.name.equalsIgnoreCase(name))	
			ratingChange += MAX_BONUS * (losers.size()/30) * (1 - p);
	}
	
	private void calculateRatios(){
		w = wins;
		l = losses;
		s = shots;
		if(shots != 0)
			hitRatio = ((w / s) * 100);
		if(hitRatio > 100) 
			hitRatio = 100;
		if(losses != 0)
			winRatio = ((w / l) * 100);
		else{
			winRatio = ((w / 1) * 100);
		}
	}
	
	private void clearBorderInfo(){
		outOfBounds = 0;
		gotBorderWarning = false;
	}

}

public class CompareByLosses implements Comparator<ElimPlayer> {
	public int compare(ElimPlayer a, ElimPlayer b){
		if(a.losses > b.losses)return 1;
		else if(a.losses == b.losses)return 0;
		else return -1;
	}
}

public class CompareByWinRatio implements Comparator<ElimPlayer> {
	public int compare(ElimPlayer a, ElimPlayer b){
		a.calculateRatios();
		b.calculateRatios();
		if(a.winRatio > b.winRatio)return 1;
		else if(a.winRatio == b.winRatio)return 0;
		else return -1;
	}
}

private class GameStatus{
	private static final int OFF_MODE = -1;
	private static final int WAITING_FOR_PLAYERS = 0;
	private static final int VOTING_ON_SHIP = 1;
	private static final int VOTING_ON_DEATHS = 2;
	private static final int VOTING_ON_SHRAP = 3;
	private static final int WAITING_TO_START = 4;
	private static final int TEN_SECONDS = 5;
	private static final int GAME_IN_PROGRESS = 6;
	private static final int GAME_OVER = 7;
	private int state = 0;
	private TimerTask task;
	
	private GameStatus(){}
	
	private void moveOn(long millis){
		m_botAction.cancelTask(task);
		task = new TimerTask(){
			public void run(){
				moveOn();
				this.cancel();
			}
		};
		m_botAction.scheduleTask(task, millis);
	}
	
	private void moveOn(){
		if(elimPlayers.size() < cfg_minPlayers && state < GAME_IN_PROGRESS)
			state = OFF_MODE;
		switch(state){
			case OFF_MODE:
				doWaitingForPlayers();
				break;
			case WAITING_FOR_PLAYERS:
				doVotingOnShip();
				break;
			case VOTING_ON_SHIP:
				countVotes();
				doVotingOnDeaths();
				break;
			case VOTING_ON_DEATHS:
				countVotes();
				if(cfg_gameType == ELIM || (shipType != 2 && shipType != 8)){
					doWaitingToStart();
				} else {
					doVotingOnShrap();
				}
				break;
			case VOTING_ON_SHRAP:
				countVotes();
				doWaitingToStart();
				break;
			case WAITING_TO_START:
				doTenSeconds();
				break;
			case TEN_SECONDS:
				doGameInProgress();
				break;
			case GAME_IN_PROGRESS:
				doGameOver();
				break;
			case GAME_OVER:
				if(botMode == ON)
					doWaitingForPlayers();
				else if(botMode == OFF)
					state = OFF_MODE;
				else if(botMode == DISCONNECT)
					m_botAction.scheduleTask(new DieTask(m_botAction.getBotName()), 500);
				break;
		}	
	}
	
	public boolean isVoting(){
		if(state == VOTING_ON_SHIP || state == VOTING_ON_DEATHS || state == VOTING_ON_SHRAP)
			return true;
		else return false;
	}
	
	public boolean isInProgress(){
		if(state == TEN_SECONDS || state == GAME_IN_PROGRESS)
			return true;
		else return false;
	}
	
}

private class SpawnTimer {
    private String name;
    private TimerTask runIt = new TimerTask() {
        public void run() {
        	if(shrap == ON)
        		m_botAction.specificPrize(name, Tools.Prize.SHRAPNEL);
        	m_botAction.specificPrize(name, Tools.Prize.MULTIFIRE);
        	doWarpIntoElim(name);
        }
    };
        
    public SpawnTimer(String name) {
        this.name = name;
        m_botAction.scheduleTask(runIt, SPAWN_TIME);
    }
}

private class MVPTimer {
    private String name;
    private TimerTask runIt = new TimerTask() {
        public void run() {
        	m_botAction.sendArenaMessage("MVP: " + name, Tools.Sound.INCONCEIVABLE);
        }
    };
        
    public MVPTimer(String name) {
        this.name = name;
        m_botAction.scheduleTask(runIt, 5 * Tools.TimeInMillis.SECOND);
    }
}

////////////////////////////////////////////////////
//                    EVENTS                      //
////////////////////////////////////////////////////

    public void handleEvent(LoggedOn event) {    	       
    	m_botAction.joinArena(cfg_arena);
    	m_botAction.specAll();
    	m_botAction.receiveAllPlayerDeaths();
    	m_botAction.setPlayerPositionUpdating( 400 );
    }
    
    public void handleEvent(ArenaJoined event) {
    	Iterator<Player> i = m_botAction.getPlayerIterator();
    	while( i.hasNext() ){
    		Player p = i.next();
    		String name = p.getPlayerName();
    		try{
        		ResultSet rs = m_botAction.SQLQuery(db, "SELECT fnSpecWhenOut, fnElim FROM tblElimPlayer WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
        		if(rs != null && rs.next()){
        			if(rs.getInt("fnElim") == 1)
        				enabled.add(name);
        			if(rs.getInt("fnSpecWhenOut") == 1)
        				classicMode.add(name);
        		}
        		m_botAction.SQLClose(rs);
        	}catch(SQLException e){
        		Tools.printStackTrace(e);
        	}
    	}
    	game.moveOn();
    }
    
    public void handleEvent(WeaponFired event) {
    	String name = m_botAction.getPlayerName(event.getPlayerID());
    	if(!game.isInProgress() || !elimPlayers.containsKey(name))return;
    	elimPlayers.get(name).shots += 1;   	
    }
    
    public void handleEvent(PlayerDeath event) {
    	String win = m_botAction.getPlayerName(event.getKillerID());
    	String loss = m_botAction.getPlayerName(event.getKilleeID());
    	Player p = m_botAction.getPlayer(win);
    	if(opList.isBotExact(win) || opList.isBotExact(loss))return;
    	casualPlayers.get(win).gotWin();
    	casualPlayers.get(loss).gotLoss();
    	if(!(game.state == GameStatus.GAME_IN_PROGRESS) || !elimPlayers.containsKey(win) || !elimPlayers.containsKey(loss))return;
    	if(p != null && p.getYTileLocation() > 550 && cfg_gameType == BASEELIM){
    		m_botAction.sendSmartPrivateMessage( win, "Kill from outside base(No count).");
    		m_botAction.sendSmartPrivateMessage( loss, "Kill from outside base(No count).");
    		return;
    	}
    	elimPlayers.get(win).gotWin();
    	if(elimPlayers.get(loss).streak >= 7)
    		m_botAction.sendArenaMessage("Streak breaker! " + loss + "(" + elimPlayers.get(loss).streak + ":0) broken by " + win + "!", Tools.Sound.INCONCEIVABLE);
    	elimPlayers.get(loss).gotLoss();
    	if(elimPlayers.get(loss).losses == deaths){
    		m_botAction.sendArenaMessage(loss + " is out. " + elimPlayers.get(loss).wins + " wins " + elimPlayers.get(loss).losses + " losses");
    		losers.put(loss, elimPlayers.remove(loss));
    		doWarpIntoCasual(loss);
    	}else{
    		elimPlayers.get(loss).clearBorderInfo();
    		new SpawnTimer(loss);
    	}
    	if(elimPlayers.size() == 1){
    		winner = elimPlayers.get(elimPlayers.firstKey());
    		game.moveOn();
    	}
    }
    
    public void handleEvent(PlayerEntered event) {
    	String name = m_botAction.getPlayerName(event.getPlayerID());
    	if(name == null || opList.isBotExact(name))return;
    	m_botAction.sendUnfilteredPrivateMessage(name, "*einfo");
    	m_botAction.sendSmartPrivateMessage( name, "Welcome to " + cfg_arena + "! " + getStatusMsg());
    	enabled.add(name);
    	classicMode.add(name);
    	try{
    		ResultSet rs = m_botAction.SQLQuery(db, "SELECT fnSpecWhenOut, fnElim FROM tblElimPlayer WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    		if(rs != null && rs.next()){
    			if(rs.getInt("fnElim") == 0)
    				enabled.remove(name);
    			if(rs.getInt("fnSpecWhenOut") == 0)
    				classicMode.remove(name);
    		}
    		m_botAction.SQLClose(rs);
    	}catch(SQLException e){
    		Tools.printStackTrace(e);
    	}
    }
    
    public void handleEvent(PlayerLeft event) {
    	String name = m_botAction.getPlayerName(event.getPlayerID());
    	if(name == null || opList.isBotExact(name))return;
    	enabled.remove(name);
    	classicMode.remove(name);
    	casualPlayers.get(name).storeStats();
    	casualPlayers.remove(name);
    	if(game.isInProgress() && elimPlayers.containsKey(name)){
    		lagouts.put(name, elimPlayers.remove(name));
    		lagouts.get(name).lagTime = System.currentTimeMillis();
    		lagouts.get(name).lagouts++;
    		if(lagouts.get(name).lagouts > 2){
    			m_botAction.sendArenaMessage(name + " is out. " + lagouts.get(name).wins + " wins " + lagouts.get(name).losses + " losses (Too many lagouts)");
    			losers.put(name, lagouts.remove(name));
        		doWarpIntoCasual(name);
    		}
    	}
    	else if(!game.isInProgress() && elimPlayers.containsKey(name))
    		elimPlayers.remove(name);
    	if(elimPlayers.size() == 1 && game.state == GameStatus.GAME_IN_PROGRESS){
			winner = elimPlayers.get(elimPlayers.firstKey());
    		game.moveOn();
		}
    }
    
    public void handleEvent(FrequencyShipChange event) {
    	String name = m_botAction.getPlayerName(event.getPlayerID());
    	if(game.isInProgress() && elimPlayers.containsKey(name) && event.getShipType() == 0){
    		lagouts.put(name, elimPlayers.remove(name));
    		lagouts.get(name).lagTime = System.currentTimeMillis();
    		lagouts.get(name).lagouts++;
    		if(lagouts.get(name).lagouts > 3){
    			m_botAction.sendArenaMessage(name + " is out. " + lagouts.get(name).wins + " wins " + lagouts.get(name).losses + " losses (Too many lagouts)");
    			losers.put(name, lagouts.remove(name));
        		doWarpIntoCasual(name);
    		}   		
    		if(elimPlayers.size() == 1 && game.state == GameStatus.GAME_IN_PROGRESS){
    			winner = elimPlayers.get(elimPlayers.firstKey());
        		game.moveOn();
    		}
    	} else if(elimPlayers.containsKey(name) && !game.isInProgress() && event.getShipType() == 0){
    		elimPlayers.remove(name);
    	} else if(!elimPlayers.containsKey(name) && event.getShipType() != cfg_defaultShip && event.getShipType() > 0){
    		m_botAction.setShip(name, cfg_defaultShip);
    	} else if(game.isInProgress() && elimPlayers.containsKey(name) && event.getShipType() > 0){
    		ElimPlayer ep = elimPlayers.get(name);
    		if(event.getShipType() != ep.shiptype){
	    		m_botAction.setShip(name, ep.shiptype);
	    		doWarpIntoElim(name);
	    		if(game.state == GameStatus.GAME_IN_PROGRESS){
		    		if(!ep.gotChangeWarning){
		    			ep.losses += 1;
		    			m_botAction.sendArenaMessage(name + " has attempted to change ships - +1 death");
		    			m_botAction.sendSmartPrivateMessage( name, "Attempt to change ships or frequencies again and you will be removed from the game. You have been warned.");
		    			ep.gotChangeWarning = true;
		    		}else{
		    			m_botAction.sendArenaMessage(name + " is out. " + ep.wins + " wins " + ep.losses + " losses (Disqualified)");
		    			losers.put(name, elimPlayers.remove(name));
		        		doWarpIntoCasual(name);
		    		}
		    		if(elimPlayers.size() == 1 && game.state == GameStatus.GAME_IN_PROGRESS){
		    			winner = elimPlayers.get(elimPlayers.firstKey());
		        		game.moveOn();
		    		}
	    		}
    		}
    	} else if(enabled.contains(name) && !elimPlayers.containsKey(name) && !game.isInProgress() && event.getShipType() > 0){
    		doWarpIntoElim(name);
    		elimPlayers.put(name, new ElimPlayer(name));
    		if(elimPlayers.size() >= cfg_minPlayers && game.state == GameStatus.WAITING_FOR_PLAYERS)
    			game.moveOn();
    	}
    }
    
    public void handleEvent(FrequencyChange event){
    	String name = m_botAction.getPlayerName(event.getPlayerID());
    	if(game.isInProgress() && elimPlayers.containsKey(name)){
    		if(event.getFrequency() != elimPlayers.get(name).frequency  && event.getFrequency() != 9999){
    			ElimPlayer ep = elimPlayers.get(name);
    			m_botAction.setFreq(name, ep.frequency);
    			doWarpIntoElim(name);
    			if(!ep.gotChangeWarning){
    				ep.losses += 1;
    				m_botAction.sendArenaMessage(name + " has attempted to change frequencies - +1 death");
    				m_botAction.sendSmartPrivateMessage( name, "Attempt to change ships or frequencies again and you will be removed from the game. You have been warned.");
    				ep.gotChangeWarning = true;
    			} else {		
    				m_botAction.sendArenaMessage(name + " is out. " + ep.wins + " wins " + ep.losses + " losses (Disqualified)");
    				losers.put(name, elimPlayers.remove(name));
    				doWarpIntoCasual(name);
    			}
    		}
    	} else if(!elimPlayers.containsKey(name) && event.getFrequency() >= 600){
    		m_botAction.sendSmartPrivateMessage( name, "Please choose a private frequency under 600.");
    		doWarpIntoCasual(name);
    	}
    }
    
    public void handleEvent(PlayerPosition event){
    	Player p = m_botAction.getPlayer(event.getPlayerID());
    	if(p == null)return;
    	if(p.getYTileLocation() < 400 && elimPlayers.containsKey(p.getPlayerName()))
    		doWarpIntoElim(p.getPlayerName()); 
    	if(cfg_gameType == BASEELIM){
    		ElimPlayer ep = elimPlayers.get(p.getPlayerName());
	    	if(ep != null && game.isInProgress()){
		    	if(p.getYTileLocation() < 550)
		    		ep.clearBorderInfo();
		    	else if(p.getYTileLocation() > 550){    		
		    		if(ep.outOfBounds == 0)
		    			ep.outOfBounds = System.currentTimeMillis();
		    		else if((System.currentTimeMillis() - ep.outOfBounds) > 20 * Tools.TimeInMillis.SECOND){
		    			m_botAction.sendArenaMessage(ep.name + " is out. " + ep.wins + " wins " + ep.losses + " losses (Too long outside base)");
		        		losers.put(ep.name, elimPlayers.remove(ep.name));
		        		doWarpIntoCasual(ep.name);
		    		}
		    		else if((System.currentTimeMillis() - ep.outOfBounds) > 10 * Tools.TimeInMillis.SECOND && !ep.gotBorderWarning){
		    			m_botAction.sendSmartPrivateMessage( ep.name, "Get in the base!");
		    			ep.gotBorderWarning = true;
		    		}    		
		    	}
		    	if(elimPlayers.size() == 1 && game.state == GameStatus.GAME_IN_PROGRESS){
		    		winner = elimPlayers.get(elimPlayers.firstKey());
		    		game.moveOn();
		    	}
    		} else {
    			//Barwarp
    			if(p.getYTileLocation() >= 258 && p.getYTileLocation() <= 263){
    				if(p.getXTileLocation() >= 499 && p.getXTileLocation() <= 507){
    					m_botAction.warpTo(p.getPlayerName(), 474, 96);
    				} else if(p.getXTileLocation() >= 508 && p.getXTileLocation() <= 516){
    					m_botAction.warpTo(p.getPlayerName(), 512, 97);
    				} else if(p.getXTileLocation() >= 517 && p.getXTileLocation() <= 525){
    					m_botAction.warpTo(p.getPlayerName(), 549, 96);
    				}
    			}
    		}
    	} else {
    		//Hiders
    	}
    }
    
    public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.LOGGED_ON);
        req.request(EventRequester.PLAYER_ENTERED);
        req.request(EventRequester.PLAYER_LEFT);
        req.request(EventRequester.WEAPON_FIRED);
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(EventRequester.FREQUENCY_CHANGE);
        req.request(EventRequester.PLAYER_DEATH);
        req.request(EventRequester.PLAYER_POSITION);
        req.request(EventRequester.ARENA_JOINED);
    }

    public String padString(String s, int length){
    	if(s.length() == length)return s;
    	while(s.length() < length)
    		s += " ";
    	return s;
    }
    
    public int getBotNumber(BotSettings cfg){ 
        for (int i = 1; i <= cfg.getInt("Max Bots"); i++){
            if (cfg.getString("Name" + i).equalsIgnoreCase(m_botAction.getBotName()))
                return i;
        }
        return 1;
    }
    
    private class DieTask extends TimerTask {
        String m_initiator;

        public DieTask( String name ) {
            super();
            m_initiator = name;
        }

        public void run() {
            m_botAction.die( "!die initiated by " + m_initiator );
        }
    }
    
    public void handleDisconnect(){
    	m_botAction.cancelTasks();
    }
}