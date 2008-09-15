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

import java.text.ParseException;

import twcore.core.BotAction;
import twcore.core.SubspaceBot;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.events.Message;
import twcore.core.events.LoggedOn;
import twcore.core.events.WeaponFired;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.FrequencyShipChange;
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
	public TreeMap<String, ElimPlayer> players = new TreeMap<String, ElimPlayer>();
	public TreeMap<String, ElimPlayer> lagouts = new TreeMap<String, ElimPlayer>();
	public TreeMap<String, ElimPlayer> losers = new TreeMap<String, ElimPlayer>();
	public TreeMap<Integer, Integer> votes = new TreeMap<Integer, Integer>();
	public ArrayList<String> enabled = new ArrayList<String>();

	
	//BotSettings variables
	public int cfg_minPlayers, cfg_maxDeaths, cfg_defaultShip, cfg_gameType, cfg_votingLength, cfg_waitLength, cfg_zone;
	public String cfg_arena;
	public ArrayList<Integer> cfg_shipTypes = new ArrayList<Integer>();
	public int[] defaultElimTypes = {1,3,4,7,9};
	public int[] defaultBelimTypes = {2,5,8,9};
	
	//Constants
	public static final int ELIM = 1;
	public static final int BASEELIM = 2;
	public static final int OFF = 0;
	public static final int ON = 1;
	public static final int DISCONNECT = 2;
	public static final int SPAWN_TIME = 5010;
	
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
				"+=============================== HELP MENU =================================+",
				"| !help        - Displays this menu. (?)                                    |",
				"| !play        - Toggles whether or not you wish to play. (!p)              |",
				"| !status      - Displays the game status. (!s)                             |",
				"| ",
		};List<String> reg = Arrays.asList(reghelp);
		String[] elimhelp = {
				"+=============================== ELIM MENU =================================+",
				"| !lagout      - Puts you back into the game if you've lagged out. (!l)     |",
				"| !wl          - Shows the five players with the most deaths.               |",
				"| !wl <player> - Shows the wins and losses of <player>.                     |",
				"| ",
		};List<String> elim = Arrays.asList(elimhelp);
		String[] smodhelp = {
				"+=============================== SMOD MENU =================================+",
				"| !zone        - Toggles whether or not the bot should zone for games.      |",
				"| !off         - Prevents new games from starting once the current one ends.|",
				"| !on          - Turns off !off mode and allows new games to begin          |",
				"| !shutdown    - Causes the bot to disconnect once the current game ends.   |",
				"| !die         - Causes the bot to disconnect.                              |",
				"| ",
		};List<String> smod = Arrays.asList(smodhelp);				
		help.addAll(reg);
		if(players.containsKey(name))
			help.addAll(elim);
		if(opList.isSmod(name))
			help.addAll(smod);
		help.add("+===========================================================================+");
		return help.toArray(new String[help.size()]);

	}
	
	public String getStatusMsg(){
		switch(game.state){
			case GameStatus.OFF_MODE:
				return "Elimination is temporarily disabled.";
			case GameStatus.WAITING_FOR_PLAYERS:
				return "A new elimination match will begin when " + (cfg_minPlayers - players.size()) + " more players enter.";
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
				return "There is currently a game in progress. Feel free to play casually until the next game begins.";
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
			if(players.containsKey(name))
				handleElimCommands(name, message);
			if(opList.isSmod(name))
				handleSmodCommands(name, message);
		}				
		else if(messageType == Message.ARENA_MESSAGE && message.equals("Arena LOCKED"))
			m_botAction.toggleLocked();
		else if(messageType == Message.PUBLIC_MESSAGE &&
				game.isVoting()                       &&
				Tools.isAllDigits(message)            &&
				players.containsKey(name))
			vote(name, message);
		else if(messageType == Message.PUBLIC_MESSAGE && (message.equalsIgnoreCase("!play") || message.equalsIgnoreCase("!p")))
			cmd_play(name);
    }
    
    public void handleCommands(String name, String cmd){
    	if(cmd.equalsIgnoreCase("!help") || cmd.equals("?"))
    		m_botAction.smartPrivateMessageSpam(name, getHelpMsg(name));
    	else if(cmd.equalsIgnoreCase("!play") || cmd.equalsIgnoreCase("!p") || cmd.equals(""))
    		cmd_play(name);
    	else if(cmd.equalsIgnoreCase("!status") || cmd.equalsIgnoreCase("!s"))
    		m_botAction.sendSmartPrivateMessage( name, getStatusMsg());
    }
    
    public void handleElimCommands(String name, String cmd){
    	if(cmd.equalsIgnoreCase("!lagout") || cmd.equalsIgnoreCase("!l"))
    		cmd_lagout(name);
    	else if(cmd.equalsIgnoreCase("!wl"))
    		cmd_wl(name, null);
    	else if(cmd.startsWith("!wl "))
    		cmd_wl(name, cmd.substring(4));
    }
    
    public void handleSmodCommands(String name, String cmd){
    	if(cmd.equalsIgnoreCase("!zone"))
    		cmd_zone(name);
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
    		if(players.containsKey(name)){
    			players.remove(name);
    			m_botAction.specificPrize(name, Tools.Prize.WARP);
    		}
    	}else {
    		m_botAction.sendSmartPrivateMessage( name, "You have enabled !play. Type !play again to play casually.");
    		enabled.add(name);
    		Player p = m_botAction.getPlayer(name);
    		if(p.getShipType() > 0 && !game.isInProgress()){
    			players.put(name, new ElimPlayer(name));
    			doWarpIntoElim(name);
    			if(players.size() >= cfg_minPlayers && game.state == GameStatus.WAITING_FOR_PLAYERS)
        			game.moveOn();
    		}    		
    	}
    }
    
    public void cmd_lagout(String name){
    	if(lagouts.containsKey(name) && game.state == GameStatus.GAME_IN_PROGRESS){
    		players.put(name, lagouts.remove(name));
    		if(!enabled.contains(name))
    			enabled.add(name);
    		m_botAction.setShip(name, players.get(name).shiptype);
    		m_botAction.setFreq(name, players.get(name).frequency);
    		doWarpIntoElim(name);
    	} else
    		m_botAction.sendSmartPrivateMessage( name, "You aren't in the game!");    		
    }
    
    public void cmd_wl(String name, String target){
    	if(target != null){
    		target = m_botAction.getFuzzyPlayerName(target);
    		if(target != null && players.containsKey(target)){
	    		ElimPlayer t = players.get(target);
	    		m_botAction.sendSmartPrivateMessage( name, t.name + " (" + t.wins + "-" + t.losses + ")");
	    		return;
    		}
    	}
    	CompareByLosses byLosses = new CompareByLosses();
    	List<ElimPlayer> l = Arrays.asList(players.values().toArray(new ElimPlayer[players.values().size()]));
    	Collections.sort(l, Collections.reverseOrder(byLosses));
    	int index = 1;
    	Iterator<ElimPlayer> i = l.iterator();
    	m_botAction.sendSmartPrivateMessage( name, "------------- Most Deaths -------------");
    	while( i.hasNext() && index < 6){
    		ElimPlayer p = i.next();
    		m_botAction.sendSmartPrivateMessage( name, index + ") " + p.name + " (" + p.wins + "-" + p.losses + ")");
    		index++;
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
    			new DieTask(m_botAction.getBotName());
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
    
    public void doWaitingForPlayers(){
    	game.state = GameStatus.WAITING_FOR_PLAYERS;
    	int neededPlayers = cfg_minPlayers - players.size();
    	if(neededPlayers <= 0)
    		game.moveOn();
    	else
    		m_botAction.sendArenaMessage("A new elimination match will begin when " + neededPlayers + " more players enter.");
    }
    
    public void doVotingOnShip(){
    	game.state = GameStatus.VOTING_ON_SHIP;
    	if(cfg_zone == ON)
    		m_botAction.sendZoneMessage("Next elimination match is starting. Type ?go " + cfg_arena + " to play");
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
    	Iterator<ElimPlayer> i = players.values().iterator();
    	while(i.hasNext()){
    		ElimPlayer ep = i.next();
    		ep.frequency = freq;
    		m_botAction.setFreq(ep.name, freq);
    		if(shipType != 9)
    			m_botAction.setShip(ep.name, shipType);
    		doWarpIntoElim(ep.name);
    		freq++;
    	}
    	game.moveOn(10 * Tools.TimeInMillis.SECOND);
    }
    
    public void doGameInProgress(){
    	game.state = GameStatus.GAME_IN_PROGRESS;
    	lagouts.clear();
    	m_botAction.sendArenaMessage("GO! GO! GO!", Tools.Sound.GOGOGO);
    	int freq = 600;
    	Iterator<ElimPlayer> i = players.values().iterator();
    	while(i.hasNext()){
    		ElimPlayer ep = i.next();
    		Player p = m_botAction.getPlayer(ep.name);
    		if(p == null)continue;
    		if(shipType == 9){
    			if(!cfg_shipTypes.contains(p.getShipType())){
    				m_botAction.setShip(p.getPlayerName(), cfg_defaultShip);
    				doWarpIntoElim(p.getPlayerName());
    			}	
    		}else if(p.getShipType() != shipType){
    			m_botAction.setShip(p.getPlayerName(), shipType);
    			doWarpIntoElim(p.getPlayerName());
    		}
    		if(shrap == OFF)
    			m_botAction.specificPrize(p.getPlayerName(), -Tools.Prize.SHRAPNEL);
    		ep.resetScore();
    		try{
    			ResultSet rs = m_botAction.SQLQuery(db, "SELECT * FROM tblElimPlayer WHERE fcUserName = '" + Tools.addSlashesToString(ep.name.toLowerCase()) + "'");
    			if(rs != null && rs.next())
    				ep.initRating = rs.getInt("fnRating");
    			else {
    				m_botAction.SQLQueryAndClose(db, "INSERT INTO tblElimPlayer (fnRank, fcUserName, fnRating, fnKills, fnDeaths, fnElim, ftUpdate) VALUES (0,'" + Tools.addSlashesToString(ep.name.toLowerCase()) + "',1000,0,0,1,NOW())");//TODO:query
    				ep.initRating = 1000;
    			}
    			avg_rating += ep.initRating;
    			m_botAction.SQLClose(rs);
    		}catch(SQLException e){
    			Tools.printStackTrace(e);
    		}
    		freq++;
    	}
    	avg_rating /= players.size();
    }
    
    public void doGameOver(){
    	game.state = GameStatus.GAME_OVER;
    	CompareByWinRatio byWinRatio = new CompareByWinRatio();
    	CompareByHitRatio byHitRatio = new CompareByHitRatio();
    	CompareByRatingChange byRatingChange = new CompareByRatingChange();
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
    			m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnRating = fnRating + " + ep.ratingChange + " WHERE fcUserName = '" + Tools.addSlashesToString(ep.name.toLowerCase()) + "'");
    		}catch(SQLException e){
    			Tools.printStackTrace(e);
    		}
    	}
    	try{
	    	ArrayList<String> stats = new ArrayList<String>();
	    	List<ElimPlayer> l = Arrays.asList(losers.values().toArray(new ElimPlayer[losers.values().size()]));
	    	stats.add("Winner: " + winner.name + "!");
	    	stats.add(",-------------------------------------------------------------.");
	    	Collections.sort(l, byWinRatio);//Best record
	    	stats.add("| MVP:          " + l.get(l.size() - 1).name + " (" + format.valueToString(l.get(l.size() - 1).winRatio) + "% win ratio)");
	    	stats.add("| LVP:          " + l.get(0).name + " (" + format.valueToString(l.get(0).winRatio) + "% win ratio)");
	    	Collections.sort(l, byHitRatio);//Best aim
	    	stats.add("| Best aim:     " + l.get(l.size() - 1).name + " (" + format.valueToString(l.get(l.size() - 1).hitRatio) + "% hit ratio)");
	    	stats.add("| Worst aim:    " + l.get(0).name + " (" + format.valueToString(l.get(0).hitRatio) + "% hit ratio)");
	    	Collections.sort(l, byRatingChange);//Best rating change
	    	stats.add("| Best effort:  " + l.get(l.size() - 1).name + " (" + l.get(l.size() - 1).wins + "-" + l.get(l.size() - 1).losses + ")");
	    	stats.add("| Worst effort: " + l.get(0).name + " (" + l.get(0).wins + "-" + l.get(0).losses + ")");
	    	stats.add("`-------------------------------------------------------------'");
	    	m_botAction.arenaMessageSpam(stats.toArray(new String[stats.size()]));
    	}catch(ParseException e){
    		Tools.printStackTrace(e);
    	}
    	Iterator<String> s = enabled.iterator();
    	while( s.hasNext() ){
    		String playerName = s.next();
    		Player p = m_botAction.getPlayer(playerName);
    		if(p == null || p.getShipType() == 0)continue;
    		players.put(playerName, new ElimPlayer(playerName));
    		doWarpIntoElim(playerName);
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
    	if(players.containsKey(name))
    		player = players.get(name);
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
			if(cfg_gameType == ELIM || shipType != 2)
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
		Iterator<ElimPlayer> it = players.values().iterator();
		while( it.hasNext() )
			it.next().vote = -1;
    }
    
    public void doWarpIntoElim(String name){
    	m_botAction.warpTo(name, 512, 512);//TODO:
    }
    
    
private class ElimPlayer{
	private String name;
	private byte shiptype = -1;
	private int vote = -1, frequency = -1, wins = 0, losses = 0, shots = 0, initRating = 0;
	private double hitRatio = 0, winRatio = 100, ratingChange = 0, w, l, s;
	private static final int MAX_POINTS = 50;
	
	private ElimPlayer(String name){
		this.name = name;
		
	}
	
	private void resetScore(){
		wins = 0;
		losses = 0;
	}
	
	private void calculateStats(){
		w = wins;
		l = losses;
		s = shots;
		if(shots != 0)
			hitRatio = ((w / s) * 100);
		if(hitRatio > 100) 
			hitRatio = 100;
		if(losses != 0)
			winRatio = ((w / l) * 100);
		double x = avg_rating - initRating;//Average Player Rating - Player Rating
		double p = 1/(1 + Math.pow(10, (x/400)));//Probability of winning
		if(winner.name.equalsIgnoreCase(name))
			ratingChange = MAX_POINTS * (1 - p);
		else
			ratingChange = MAX_POINTS * (0 - p);
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
		if(a.winRatio > b.winRatio)return 1;
		else if(a.winRatio == b.winRatio)return 0;
		else return -1;
	}
}

public class CompareByHitRatio implements Comparator<ElimPlayer> {
	public int compare(ElimPlayer a, ElimPlayer b){
		if(a.hitRatio > b.hitRatio)return 1;
		else if(a.hitRatio == b.hitRatio)return 0;
		else return -1;
	}
}

public class CompareByRatingChange implements Comparator<ElimPlayer> {
	public int compare(ElimPlayer a, ElimPlayer b){
		if(a.name.equals(winner.name))return -1;
		if(a.ratingChange > b.ratingChange)return 1;
		else if(a.ratingChange == b.ratingChange)return 0;
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
	private int state = -1;
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
		if(players.size() < cfg_minPlayers && state < GAME_IN_PROGRESS)
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
				if(cfg_gameType == ELIM || shipType != 2){
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
				else
					new DieTask(m_botAction.getBotName());
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
            doWarpIntoElim(name);
            if(shrap == OFF)
    			m_botAction.specificPrize(name, -Tools.Prize.SHRAPNEL);                
        }
    };
        
    public SpawnTimer(String name) {
        this.name = name;
        m_botAction.scheduleTask(runIt, SPAWN_TIME);
    }
}

    public void handleEvent(LoggedOn event) {    	       
    	m_botAction.joinArena(cfg_arena);
    	m_botAction.specAll();
    	game.moveOn();
    }
    
    public void handleEvent(WeaponFired event) {
    	String name = m_botAction.getPlayerName(event.getPlayerID());
    	if(!game.isInProgress() || !players.containsKey(name))return;
    	players.get(name).shots += 1;   	
    }
    
    public void handleEvent(PlayerDeath event) {
    	String win = m_botAction.getPlayerName(event.getKillerID());
    	String loss = m_botAction.getPlayerName(event.getKilleeID());
    	if(!game.isInProgress() || !players.containsKey(win) || !players.containsKey(loss))return;
    	players.get(win).wins += 1;
    	players.get(loss).losses += 1;
    	if(players.get(loss).losses == deaths){
    		m_botAction.sendArenaMessage(loss + " is out. " + players.get(loss).wins + " wins " + players.get(loss).losses + " losses");
    		losers.put(loss, players.remove(loss));
    	}else
    		new SpawnTimer(loss);
    	if(players.size() == 1 && game.state == GameStatus.GAME_IN_PROGRESS){
    		winner = players.get(players.firstKey());
    		game.moveOn();
    	}
    }
    
    public void handleEvent(PlayerEntered event) {
    	String name = m_botAction.getPlayerName(event.getPlayerID());
    	if(name == null || opList.isBot(name))return;
    	m_botAction.sendSmartPrivateMessage( name, "Welcome to " + cfg_arena + "! " + getStatusMsg());
    	try{
    		ResultSet rs = m_botAction.SQLQuery(db, "SELECT fnElim FROM tblElimPlayer WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "'");
    		if(rs != null && rs.next())
    			if(rs.getInt("fnElim") == 1)
    				enabled.add(name);
    		m_botAction.SQLClose(rs);
    	}catch(SQLException e){
    		Tools.printStackTrace(e);
    	}
    }
    
    public void handleEvent(PlayerLeft event) {
    	String name = m_botAction.getPlayerName(event.getPlayerID());
    	enabled.remove(name);
    	if(game.isInProgress() && players.containsKey(name)){
    		lagouts.put(name, players.remove(name));
    		if(players.size() == 1 && game.state == GameStatus.GAME_IN_PROGRESS){
    			winner = players.get(players.firstKey());
        		game.moveOn();
    		}
    	}
    }
    
    public void handleEvent(FrequencyShipChange event) {
    	String name = m_botAction.getPlayerName(event.getPlayerID());
    	if(game.isInProgress() && players.containsKey(name) && event.getShipType() == 0){
    		lagouts.put(name, players.remove(name));
    		if(players.size() == 1 && game.state == GameStatus.GAME_IN_PROGRESS){
    			winner = players.get(players.firstKey());
        		game.moveOn();
    		}
    	} else if(game.isInProgress() && players.containsKey(name)){
    		if(event.getShipType() != players.get(name).shiptype)
	    		m_botAction.setShip(name, players.get(name).shiptype);
    		else if(event.getFrequency() != players.get(name).frequency)
    			m_botAction.setFreq(name, players.get(name).frequency);
    		doWarpIntoElim(name);
    	} else if(!enabled.contains(name) && event.getFrequency() >= 600 && event.getShipType() > 0){
    		m_botAction.sendSmartPrivateMessage( name, "Please choose a private frequency under 600.");
    		m_botAction.setFreq(name, 0);
    	} else if(enabled.contains(name) && !players.containsKey(name) && !game.isInProgress() && event.getShipType() > 0){
    		doWarpIntoElim(name);
    		players.put(name, new ElimPlayer(name));
    		if(players.size() >= cfg_minPlayers && game.state == GameStatus.WAITING_FOR_PLAYERS)
    			game.moveOn();
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
        req.request(EventRequester.PLAYER_DEATH);
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