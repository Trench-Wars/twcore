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
import twcore.core.util.Spy;

public class elim extends SubspaceBot {
	
	public OperatorList opList;
	public Spy racismWatcher;
	public String db = "website";
	public Random rand = new Random();
	public static DecimalFormat decForm = new DecimalFormat("0.##");
	public static NumberFormatter format = new NumberFormatter(decForm);
	
	//Class variables
	public int botMode = 1;
	public int gameStyle = 1;
	public int shipType = -1;
	public int deaths = -1;
	public int kills = -1;
	public int shrap = 0;
	public int avg_rating = 0;
	public String lastWinner = "-anonymous-";
	public int winStreak = 0;
	public long lastZoner = 0;
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
	BotSettings cfg;
	public int cfg_minPlayers, cfg_maxDeaths, cfg_maxKills, cfg_defaultShip, cfg_gameType;
	public int cfg_votingLength, cfg_waitLength, cfg_zone, cfg_border, cfg_casualAllowed;
	public int[] cfg_safe, cfg_competitive, cfg_casual, cfg_barY;
	public int[][] cfg_barspots;
	public String cfg_arena, cfg_chats, cfg_gameName;
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
			"God-like!",
			"Cheater!",
			"Juggernaut!",
			"Kill Frenzy!",
			"Running Riot!",
			"Utter Chaos!",
			"Grim Reaper!",
			"Bulletproof!",
			"Invincible!",
			"Certified Veteran!",
			"Trench Wars Most Wanted!",
			"Unforeseeable paradoxes have ripped a hole in the fabric of the universe!"
		};
	
	//Enums
	public static final int ELIM = 1;          //Game type and game style
	public static final int BASEELIM = 2;      //Game type
	public static final int KILLRACE = 2;      //Game style
	public static final int OFF = 0;           //Simple boolean used for int variables
	public static final int ON = 1;            //Simple boolean used for int variables
	public static final int DISCONNECT = 2;    //Bot mode... bot will disconnect after game finishes
	public static final int SPAWN_TIME = 5005; //Time to wait before warping a player after a death in milliseconds
	public static final int SPAWN_NC = 1;      //Time that a spawn kill won't count after spawning in seconds
	public static final int LAGOUT_TIME = 30;  //Time you must wait before returning from a lagout in seconds
	public static final int LAGOUT_RETURN = 60;//Time you must return before when returning from a lagout in seconds
	public static final int MAX_LAGOUT = 2;    //Maximum number of lagouts allowed
	public static final int DOUBLE_KILL = 5;   //Number of seconds after a kill for another kill to be considered duplicate
	public static final int MIN_ZONER = 3;     //The minimum amount of minutes that another zoner can be sent
	public static final int BOUNDARY_TIME = 30;//The maximum amount of time you can be outside of base in seconds
	public static final int STREAK_INIT = 5;   //The number of kills needed for the first streak
	public static final int STREAK_REPEAT = 2; //The number of kills needed for streak messages after the initial streak
	public static final int SAFE_HEIGHT = 14;  //The height of the safety area in tiles
	public static final int ANY_SHIP = 9;      //A ship type... all ships within the cfg_shipTypes are allowed
	public static final int SPEC_FREQ = 9999;  //The spec frequency
	public static final int MAX_FREQ = 99;     //The maximum frequency casual players can be on
	public static final int LIMIT_STREAK = 3;  //The amount of consecutive game wins to limit minimum kill/death voting
	public static final int MIN_FOR_STREAK = 5;//The minimum kills/deaths allowable to vote on during a game winning streak
	public static final int INIT_RATING = 300; //The initial rating that players begin at.
	public static final int INIT_RANKING = 500;//The number of combined kills and deaths needed to be ranked.
	
	//LVZ
	public static final int CASUAL_SPLASH_LVZ = 1;    //Displays temporary image below energy bar that says "This is casual play"
	public static final int CASUAL_LOGO_LVZ = 2;      //Displays casual logo above radar.
	
    public elim(BotAction botAction) {
        super(botAction);
        requestEvents();
        racismWatcher = new Spy(botAction);
        opList = m_botAction.getOperatorList();
        reinstantiateCFG();        
    }
    
    public void reinstantiateCFG(){
    	cfg = m_botAction.getBotSettings();
    	int botnum = getBotNumber(cfg);
        cfg_minPlayers = cfg.getInt("MinPlayers");
        cfg_maxDeaths = cfg.getInt("MaxDeaths");
        cfg_maxKills = cfg.getInt("MaxKills");
        cfg_votingLength = cfg.getInt("VotingLength");
        cfg_waitLength = (cfg.getInt("WaitLength") - 10);
        cfg_zone = cfg.getInt("SendZoner");
        cfg_border = cfg.getInt("Border");
        cfg_arena = cfg.getString("Arena" + botnum);        
        cfg_gameType = cfg.getInt("GameType" + botnum);
        cfg_defaultShip = cfg.getInt("DefaultShip" + cfg_gameType);
        cfg_chats = cfg.getString("Chats" + cfg_gameType);
        cfg_safe = cfg.getIntArray("Safe" + cfg_gameType, ",");
        cfg_competitive = cfg.getIntArray("Competitive" + cfg_gameType, ",");
        cfg_casualAllowed = cfg.getInt("CasualAllowed");
        cfg_casual = cfg.getIntArray("Casual" + cfg_gameType, ",");
        if(cfg_gameType == ELIM) cfg_gameName = "elim";
        else{
        	cfg_gameName = "belim";
        	cfg_barY = cfg.getIntArray("BarwarpY", ",");
        	cfg_barspots = new int[cfg.getInt("Barspots")][4];
        	for(int i=0;i<cfg_barspots.length;i++)
	        	cfg_barspots[i] = cfg.getIntArray("Barspot" + (i+1), ",");
        }
        String[] types = cfg.getString("ShipTypes" + cfg_gameType).split(",");
        cfg_shipTypes.clear();
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
				"| !who          - Shows all remaining players in the game.                   |",
				"| !rank <#>     - Returns the player at rank <#>.                            |",
				"| !stats        - Shows your statistics including your rank.                 |",
				"| !stats <name> - Shows statistics of <name>.                                |",
				"| !rec          - Shows your wins and losses. (!r)                           |",
				"| !rec <name>   - Shows the wins and losses of <name>.                       |",
				"| !classic      - Toggles whether or not you'd like to be spec'd when out.   |",
				"| !warp         - Warps you out of the safe if you're stuck.                 |",
				"| !scorereset   - Resets all of your stats to zero. No going back. (!sr)     |",

		};List<String> reg = Arrays.asList(reghelp);
		String[] modHelp = {
		        "+=============================== MOD MENU ===================================+",
		        "| !remove <name> - Removes <name> from the game                              |",
		};List<String> mod = Arrays.asList(modHelp);
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
		if(opList.isModerator(name))
		    help.addAll(mod);
		if(opList.isSmod(name))
			help.addAll(smod);
		if(opList.isSysop(name)) {
			help.add("| !updatecfg    - Updates the bot to the most current CFG file.              |");
			help.add("| !greetmsg <m> - Sets arena greet message(Sysop only).                      |");
		}
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
				if(gameStyle == ELIM)
					return "We are playing " + Tools.shipName(shipType) + " elim. We are currently voting on number of deaths.";
				else return "We are playing " + Tools.shipName(shipType) + " kill race. We are currently voting on number of kills.";
			case GameStatus.VOTING_ON_SHRAP:
				if(gameStyle == ELIM)
					return "We are playing " + Tools.shipName(shipType) + " elim to " + deaths + ". We are currently voting on shrap.";
				else return "We are playing " + Tools.shipName(shipType) + " kill race to " + kills + ". We are currently voting on shrap.";
			case GameStatus.WAITING_TO_START:
				if(gameStyle == ELIM)
					return "We are playing " + Tools.shipName(shipType) + " elim to " + deaths + ". The game will start soon. Enter to play!";
				else return "We are playing " + Tools.shipName(shipType) + " kill race to " + kills + ". The game will start soon. Enter to play!";
			case GameStatus.TEN_SECONDS:
				return "The game will begin in less than ten seconds. No more entries";
			case GameStatus.GAME_IN_PROGRESS:
				if(gameStyle == ELIM)
					return "We are currently playing " + Tools.shipName(shipType) + " elim to " + deaths + ". " + elimPlayers.size() + " players left.";
				else return "We are currently playing " + Tools.shipName(shipType) + " kill race to " + kills + ". " + elimPlayers.size() + " players playing.";
			case GameStatus.GAME_OVER:
				return "A new elimination match will begin shortly.";
			default: return null;
		}
	}
        
    public void handleEvent(Message event) {
    	String message = event.getMessage();
		String name = event.getMessager() == null ? m_botAction.getPlayerName(event.getPlayerID()) : event.getMessager();
		int messageType = event.getMessageType();
		
		if(messageType == Message.PUBLIC_MESSAGE        ||
				messageType == Message.TEAM_MESSAGE          ||
				messageType == Message.OPPOSING_TEAM_MESSAGE ||
				messageType == Message.PUBLIC_MACRO_MESSAGE)
					racismWatcher.handleEvent(event);
		
		if(messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE){
			handleCommands(name, message);
			if (opList.isModerator(name))
			    handleModeratorCommands(name, message);
			if(opList.isSmod(name))
				handleSmodCommands(name, message);
			if(opList.isSysop(name))
				handleSysopCommands(name, message);
		}
		else if(messageType == Message.ARENA_MESSAGE && message.contains("UserId:"))
			doEInfo(message);
		else if(messageType == Message.PUBLIC_MESSAGE &&
				game.isVoting()                       &&
				Tools.isAllDigits(message)            &&
				message.length() <= 2)
			vote(name, message);
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
    	else if(cmd.equalsIgnoreCase("!who"))
    		cmd_who(name);
    	else if(cmd.equalsIgnoreCase("!rec") || cmd.equalsIgnoreCase("!r"))
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
    
    public void handleModeratorCommands(String name, String cmd) {
        if(cmd.startsWith("!remove "))
            cmd_remove(name, cmd.substring(8));
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
    	else if(cmd.equalsIgnoreCase("!die")){
    		try{
    			m_botAction.scheduleTask(new DieTask(name), 500);
    		}catch(Exception e){}
    	}
    }
    
    public void handleSysopCommands(String name, String cmd){
    	if(cmd.startsWith("!greetmsg "))
			m_botAction.sendUnfilteredPublicMessage( "?set misc:greetmessage:"+cmd.substring(10) );
    	else if(cmd.equalsIgnoreCase("!updatecfg"))
    		reinstantiateCFG();
    }
    
    public void cmd_play(String name){
    	if(name == null)return;
    	if(enabled.remove(name)){
            if( cfg_casualAllowed == 1 )
                m_botAction.sendSmartPrivateMessage( name, "You have disabled !play. Type !play again to compete.");
            else
                m_botAction.sendSmartPrivateMessage( name, "You have disabled !play. Because casual is not enabled, you have been removed from the game. Type !play again to compete.");
    		if(elimPlayers.containsKey(name) && game.state == GameStatus.GAME_IN_PROGRESS){
    			m_botAction.sendArenaMessage(name + " is out. " + elimPlayers.get(name).wins + " wins " + elimPlayers.get(name).losses + " losses (Resigned)");
        		losers.put(name, elimPlayers.remove(name));        		
    		}
    		else if(elimPlayers.containsKey(name))
    			elimPlayers.remove(name);
    		doWarpIntoCasual(name);
    	}else {
    	    if( cfg_casualAllowed == 1 )
    	        m_botAction.sendSmartPrivateMessage( name, "You have enabled !play. Type !play again to play casually.");
    	    else
                m_botAction.sendSmartPrivateMessage( name, "You have enabled !play, and can now play in the next elimination.");
    		enabled.add(name);
    		Player p = m_botAction.getPlayer(name);
    		if(p == null)return;
    		if(p.getShipType() > 0 && !game.isInProgress()){
    			elimPlayers.put(name, new ElimPlayer(name));
    			doWarpIntoElim(name);
    			if(elimPlayers.size() >= cfg_minPlayers && game.state == GameStatus.WAITING_FOR_PLAYERS)
        			game.moveOn();
    		}    		
    	}
    }
    
    public void cmd_mvp(String name){
    	CompareByWinsLossesAccuracy byMVP = new CompareByWinsLossesAccuracy();
    	List<ElimPlayer> l = Arrays.asList(elimPlayers.values().toArray(new ElimPlayer[elimPlayers.values().size()]));
    	Collections.sort(l, Collections.reverseOrder(byMVP));
    	int index = 1;
    	Iterator<ElimPlayer> i = l.iterator();
    	m_botAction.sendSmartPrivateMessage( name, "------------- Best Records ------------");
    	while( i.hasNext() && index <= 3){
    		ElimPlayer p = i.next();
    		m_botAction.sendSmartPrivateMessage( name, index + ") " + p.name + " (" + p.wins + "-" + p.losses + ")");
    		index++;
    	}
    	Collections.sort(l, byMVP);
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
    
    public void cmd_who(String name){
    	if(!game.isInProgress()){
    		m_botAction.sendSmartPrivateMessage( name, "There is no game in progress.");
    		return;
    	}
    	m_botAction.sendSmartPrivateMessage( name, elimPlayers.size() + " players remaining:");
    	ArrayList<String> pNames = new ArrayList<String>();
    	CompareAlphabetical byName = new CompareAlphabetical();
    	List<ElimPlayer> l = Arrays.asList(elimPlayers.values().toArray(new ElimPlayer[elimPlayers.values().size()]));
    	Collections.sort(l, byName);
    	Iterator<ElimPlayer> i = l.iterator();    	
    	while( i.hasNext() ){
    		ElimPlayer ep = i.next();
    		pNames.add(ep.name + "(" + ep.wins + "-" + ep.losses + ")");
    		if(pNames.size() == 5){
    			String message = pNames.toString();
    			m_botAction.sendSmartPrivateMessage( name, message.substring(1, message.indexOf("]")));
    			pNames.clear();
    		}
    	}
    	if(!pNames.isEmpty()){
    		String message = pNames.toString();
			m_botAction.sendSmartPrivateMessage( name, message.substring(1, message.indexOf("]")));
    	}
    }
    
    public void cmd_rec(String name, String target){
    	String clone = m_botAction.getFuzzyPlayerName(target);
    	if(clone == null)clone = target;
    	try{
    		ResultSet rs = m_botAction.SQLQuery(db, "SELECT fnKills, fnDeaths FROM tblElimCasualRecs WHERE fcUserName = '" + Tools.addSlashesToString(clone.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    		if(rs != null && rs.next()){
    			int kills = rs.getInt("fnKills");
    			int deaths = rs.getInt("fnDeaths");
    			m_botAction.SQLClose(rs);
    			if(casualPlayers.containsKey(clone)){
    				kills += casualPlayers.get(clone).wins;
    				deaths += casualPlayers.get(clone).losses;
    			}
    			m_botAction.sendSmartPrivateMessage( name, "'" + clone + "' (" + kills + "-" + deaths + ")");
    		} else {
    			m_botAction.SQLClose(rs);
    			m_botAction.sendSmartPrivateMessage( name, "No record was found for '" + clone + "'.");
    		}
    	}catch(SQLException e){
    		m_botAction.sendSmartPrivateMessage( name, "Error retrieving record. Please try again later. If problem persists contact a staff member.");
    	}
    }
    
    public void cmd_remove(String name, String target) {
        boolean spec = true;
        String clone = m_botAction.getFuzzyPlayerName(target);
        
        if (clone == null) {
            clone = target;
            spec = false;
        }
        
        if(!elimPlayers.containsKey(clone) && !lagouts.containsKey(clone)) {
            m_botAction.sendPrivateMessage(name, clone + " could not be found in the playing players list.");
            return;
        }
        
        if (elimPlayers.containsKey(clone))
            losers.put(clone, elimPlayers.remove(clone));
        else if (lagouts.containsKey(clone))
            losers.put(clone, lagouts.remove(clone));
        
        if (spec)
            m_botAction.specWithoutLock(clone);
        
        m_botAction.sendPrivateMessage(name, clone + " has been removed from the game.");
    }
    
    public void cmd_lagout(String name){
    	if(game.state != GameStatus.GAME_IN_PROGRESS)return;
    	if(lagouts.containsKey(name)){
    		if(System.currentTimeMillis() - lagouts.get(name).lagTime > LAGOUT_TIME * Tools.TimeInMillis.SECOND){
    			if(System.currentTimeMillis() - lagouts.get(name).lagTime < LAGOUT_RETURN * Tools.TimeInMillis.SECOND) {
			    	elimPlayers.put(name, lagouts.remove(name));
			    	if(!enabled.contains(name))
			    		enabled.add(name);
			    	m_botAction.sendSmartPrivateMessage( name, "You have been put back into the game. You have " + (MAX_LAGOUT-elimPlayers.get(name).lagouts) + " lagouts left.");
			    	elimPlayers.get(name).clearBorderInfo();
			    	m_botAction.setShip(name, elimPlayers.get(name).shiptype);
			    	m_botAction.setFreq(name, elimPlayers.get(name).frequency);
			    	doWarpIntoElim(name);
    			} else
    				m_botAction.sendSmartPrivateMessage( name, "You have taken too long to return. Please wait for the next game to begin.");
    		} else
    			m_botAction.sendSmartPrivateMessage( name, "You must wait for " + (LAGOUT_TIME-((System.currentTimeMillis() - lagouts.get(name).lagTime)/Tools.TimeInMillis.SECOND)) + " more seconds.");
    	} else
    		m_botAction.sendSmartPrivateMessage( name, "You aren't in the game!");    		
    }
    
    public void cmd_scorereset(String name){
    	try{
    		m_botAction.SQLQueryAndClose(db, "UPDATE tblElimCasualRecs SET fnKills = 0, fnDeaths = 0 WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    		m_botAction.SQLQueryAndClose(db, "DELETE FROM tblElimPlayer WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    		m_botAction.sendSmartPrivateMessage( name, "Your wins and losses have been reset to zero.");
    	}catch(SQLException e){
    		m_botAction.sendSmartPrivateMessage( name, "Error resetting score. Please try again later. If the problem persists contact a staff member.");
    	}
    }
    
    public void cmd_classic(String name){
        if( cfg_casualAllowed == 1 ) {
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
        } else {
            m_botAction.sendSmartPrivateMessage( name, "Casual play has been disabled.");
        }
    }
    
    public void cmd_warp(String name){
    	Player p = m_botAction.getPlayer(name);
    	if(p == null)return;
    	if((p.getYTileLocation() > (cfg_safe[1]-SAFE_HEIGHT) && p.getYTileLocation() < (cfg_safe[1]+SAFE_HEIGHT) && cfg_gameType == BASEELIM) ||
    	   (p.getYTileLocation() > (cfg_safe[1]-SAFE_HEIGHT) && p.getYTileLocation() < (cfg_safe[1]+SAFE_HEIGHT) && cfg_gameType == ELIM))
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
    			float ave = rs.getFloat("fnAve");
    			double aimNum = rs.getDouble("fnAim");
    			int cks = rs.getInt("fnCKS");
    			int cls = rs.getInt("fnCLS");
    			int bks = rs.getInt("fnBKS");
    			int wls = rs.getInt("fnWLS");
    			int bws = rs.getInt("fnBWS");
    			int pe = rs.getInt("fnPe");
    			int dk = rs.getInt("fnDK");
    			int sb = rs.getInt("fnSB");
    			int gamesWon = rs.getInt("fnGamesWon");
    			int gamesPlayed = rs.getInt("fnGamesPlayed");
    			m_botAction.SQLClose(rs);
    			String aim = format.valueToString(aimNum);
    			String rank;
    			if(ranking == 0)
    				rank = "Not ranked";
    			else
    				rank = "#" + ranking;
    			m_botAction.sendSmartPrivateMessage( name, "=== Player: '" + target + "' Rank: " + rank + " Rating: " + rating + " ===");
    			m_botAction.sendSmartPrivateMessage( name, "Kills: " + kills + " Deaths: " + deaths + " Ave: " + Math.round(ave) + " AIM:" + aim + "% CKS:" + cks + " BKS:" + bks + " CLS:" + cls + " WLS:" + wls + " BWS:" + bws);
    			m_botAction.sendSmartPrivateMessage( name, "Games Won: " + gamesWon + " Games Played: " + gamesPlayed + " PE:" + pe + " DK:" + dk + " SB:" + sb);
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
    		lastWinner = "-anonymous-";
    		winStreak = 0;
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
    			try{
    				m_botAction.scheduleTask(new DieTask(name), 500);
    			}catch(Exception e){}
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
    	if(game.state == GameStatus.OFF_MODE)
    		game.moveOn();
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
    			casualPlayers.get(name).wins += kills;
    			casualPlayers.get(name).losses += deaths;    			
    			m_botAction.sendSmartPrivateMessage( name, "Your record has been updated from ?go javs. PM me with !rec to view your wins and losses.");
    		}
    		else m_botAction.SQLClose(rs);
    	}catch(Exception e){}
    }
    
    public void doWaitingForPlayers(){
    	game.state = GameStatus.WAITING_FOR_PLAYERS;
    	int neededPlayers = cfg_minPlayers - elimPlayers.size();
    	if(neededPlayers <= 0) {
    	    if(cfg_zone == ON)doElimZoner();
    	    for(int i=1;i<=10;i++)
                m_botAction.sendChatMessage(i, "Next " + cfg_gameName + " is starting. Type ?go " + cfg_arena + " to play");
    	    TimerTask wait10Seconds = new TimerTask() {
    	        public void run() {
    	            game.moveOn();
    	        }
    	    };
    	    try{
    	    	m_botAction.scheduleTask(wait10Seconds, 10 * Tools.TimeInMillis.SECOND);
    	    }catch(Exception e){}
    	} else
    		m_botAction.sendArenaMessage("A new elimination match will begin when " + neededPlayers + " more player(s) enter.");
    }
    
    public void doVotingOnShip(){
    	game.state = GameStatus.VOTING_ON_SHIP;
    	shipType = cfg_defaultShip;
    	shrap = OFF;
    	Iterator<String> it = enabled.iterator();
    	while( it.hasNext() )
    		doWarpIntoElim(it.next());
    	String s = "Vote: ";
    	for(int i=0;i<cfg_shipTypes.size();i++){
    		s += Tools.shipName(cfg_shipTypes.get(i)) + " - " + cfg_shipTypes.get(i) + ", ";
    		votes.put(cfg_shipTypes.get(i),0);
    		votes.put(cfg_shipTypes.get(i) * 10, 0);
    	}
    	s = s.substring(0, s.length()-2);
    	m_botAction.sendArenaMessage(s);
    	m_botAction.sendArenaMessage("NOTICE: To vote for a kill race add a 0 on the end of your vote.");
    	game.moveOn(cfg_votingLength * Tools.TimeInMillis.SECOND);
    	
    }
    
    public void doVotingOnDeaths(){
    	game.state = GameStatus.VOTING_ON_DEATHS;
    	deaths = -1;
    	kills = -1;
    	int min = 1;
    	if(winStreak >= LIMIT_STREAK)
    		min = MIN_FOR_STREAK;
    	if(gameStyle == ELIM)
    		for(int i=min;i<=cfg_maxDeaths;i++)
    			votes.put(i, 0);
    	else
    		for(int i=min;i<=cfg_maxKills;i++)
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
    	if(deaths == 1 && gameStyle == ELIM)
    		m_botAction.sendArenaMessage("Rules: All on own freq, no teaming! Die "+ deaths + " time and you are out");
    	else if(gameStyle == ELIM)
    		m_botAction.sendArenaMessage("Rules: All on own freq, no teaming! Die "+ deaths + " times and you are out");
    	else if(kills == 1 && gameStyle == KILLRACE)
    		m_botAction.sendArenaMessage("Rules: All on own freq, no teaming! Get "+ kills + " kill to win");
    	else
    		m_botAction.sendArenaMessage("Rules: All on own freq, no teaming! Get "+ kills + " kills to win");
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
    		if(p == null || p.getShipType() == 0){
    			i.remove();
    			continue;
    		}
    		ep.frequency = freq;
    		if(p.getFrequency() != ep.frequency)
    			m_botAction.setFreq(ep.name, freq);
    		ep.shiptype = p.getShipType();
    		if(shipType == ANY_SHIP){
    			if(!cfg_shipTypes.contains(p.getShipType())){
    				m_botAction.setShip(p.getPlayerName(), cfg_defaultShip);
    				ep.shiptype = cfg_defaultShip;
    			}
    		}
    		else if(p.getShipType() != shipType){
    			ep.shiptype = shipType;
    			m_botAction.setShip(p.getPlayerName(), shipType);
    		}
    		freq++;
    	}
    	if(elimPlayers.size() >= 15)
    		m_botAction.setDoors("00001000");
    	else
    		m_botAction.setDoors(255);
    	game.moveOn(10 * Tools.TimeInMillis.SECOND);
    }
    
    public void doGameInProgress(){
    	game.state = GameStatus.GAME_IN_PROGRESS;
    	lagouts.clear();
    	losers.clear();
    	m_botAction.sendArenaMessage("GO! GO! GO!", Tools.Sound.GOGOGO);
    	Iterator<ElimPlayer> i = elimPlayers.values().iterator();
    	while(i.hasNext()){
    		ElimPlayer ep = i.next();
    		ep.gotChangeWarning = false;
    		if(shrap == ON)
        		m_botAction.specificPrize(ep.name, Tools.Prize.SHRAPNEL);
        	m_botAction.specificPrize(ep.name, Tools.Prize.MULTIFIRE);
    		ep.resetScore();
    		m_botAction.scoreReset(ep.name);
    		try{
    			boolean newPlayer = false;
    			ResultSet rs = m_botAction.SQLQuery(db, "SELECT * FROM tblElimPlayer WHERE fcUserName = '" + Tools.addSlashesToString(ep.name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    			if(rs != null && rs.next()){
    				ep.ave = rs.getFloat("fnAve");
    				ep.BDK = rs.getInt("fnBDK");
    				ep.streak = rs.getInt("fnCKS");
    				ep.BKS = rs.getInt("fnBKS");
    				ep.lstreak = rs.getInt("fnCLS");
    				ep.WLS = rs.getInt("fnWLS");
    				ep.initWins = rs.getInt("fnKills");
    				ep.initLosses = rs.getInt("fnDeaths");
    				ep.initRating = rs.getInt("fnRating");
    			}
    			else newPlayer = true;
    			m_botAction.SQLClose(rs);
    			if(newPlayer){
    				ep.initRating = INIT_RATING;
    				ep.ave = INIT_RATING;
    				m_botAction.SQLQueryAndClose(db, "INSERT INTO tblElimPlayer (fcUserName, fnGameType, fnRating, fnElim, ftUpdated) VALUES ('" + Tools.addSlashesToString(ep.name.toLowerCase()) + "'," + cfg_gameType + "," + INIT_RATING + ",1,NOW())");
    			}
    		}catch(SQLException e){
    			Tools.printStackTrace(e);
    		}
    	}
    }
    
    public void doGameOver(){
    	game.state = GameStatus.GAME_OVER;
    	CompareByWinsLossesAccuracy byMVP = new CompareByWinsLossesAccuracy();
    	Iterator<ElimPlayer> i = lagouts.values().iterator();
    	while( i.hasNext() ){
    		ElimPlayer ep = i.next();
    		if(((((float)ep.getTotalWins()/(float)ep.getTotalLosses())*ep.ave) - ep.initRating) < 0)
    			losers.put(ep.name, ep);
    	}
    	lagouts.clear();
    	losers.put(winner.name, winner);
    	i = losers.values().iterator();
    	while( i.hasNext() ){
    		ElimPlayer ep = i.next();
    		avg_rating += ep.initRating;
    		try{
    			if(ep.name.equalsIgnoreCase(winner.name)){
    				if(lastWinner.equalsIgnoreCase(ep.name)){
    					winStreak++;
    					m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnGamesWon = fnGamesWon + 1, " + 
    							"fnGamesPlayed = fnGamesPlayed + 1, fnKills = fnKills + " + ep.wins + 
    							", fnDeaths = fnDeaths + " + ep.losses + ", fnShots = fnShots + " + ep.shots +
    							", fnPE = fnPE + " + ep.eliminations + 
    							", fnSB = fnSB + " + ep.streakBreaks + 
    							", fnDK = fnDK + " + ep.doublekills +
    							", fnAve = " + ep.ave + ", fnAim = (CASE WHEN (fnAim = 0 OR fnShots = 0) THEN " + ep.hitRatio + 
    							" ELSE ((fnKills/fnShots)*100) END), " + 
    							"fnCKS = " + ep.streak + ", fnCLS = " + ep.lstreak + 
    							", fnCWS = fnCWS + 1, fnBWS = (CASE WHEN (fnCWS > fnBWS) THEN fnCWS ELSE fnBWS END), " + 
    							"fnRating = (CASE WHEN (fnDeaths = 0) THEN 0 ELSE ((fnKills/fnDeaths)*fnAve) END) " + 
    							"WHERE fcUserName = '" + Tools.addSlashesToString(ep.name.toLowerCase()) + 
    							"' AND fnGameType = " + cfg_gameType);
    				} else {
    					winStreak = 1;
    					m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnGamesWon = fnGamesWon + 1, " + 
    							"fnGamesPlayed = fnGamesPlayed + 1, fnKills = fnKills + " + ep.wins + 
    							", fnDeaths = fnDeaths + " + ep.losses + ", fnShots = fnShots + " + ep.shots +
    							", fnPE = fnPE + " + ep.eliminations + 
    							", fnSB = fnSB + " + ep.streakBreaks + 
    							", fnDK = fnDK + " + ep.doublekills +
    							", fnAve = " + ep.ave + ", fnAim = (CASE WHEN (fnAim = 0 OR fnShots = 0) THEN " + ep.hitRatio + 
    							" ELSE ((fnKills/fnShots)*100) END), " + 
    							"fnCKS = " + ep.streak + ", fnCLS = " + ep.lstreak + 
    							", fnCWS = 1, fnRating = (CASE WHEN (fnDeaths = 0) THEN 0 ELSE ((fnKills/fnDeaths)*fnAve) END) " + 
    							"WHERE fcUserName = '" + Tools.addSlashesToString(ep.name.toLowerCase()) + 
    							"' AND fnGameType = " + cfg_gameType);
    				}
    			}else
    				m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnGamesPlayed = fnGamesPlayed + 1, " + 
    						"fnKills = fnKills + " + ep.wins + ", fnDeaths = fnDeaths + " + ep.losses + 
    						", fnShots = fnShots + " + ep.shots +
    						", fnPE = fnPE + " + ep.eliminations + 
    						", fnSB = fnSB + " + ep.streakBreaks +
    						", fnDK = fnDK + " + ep.doublekills +
    						", fnAve = " + ep.ave + ", fnAim = (CASE WHEN (fnAim = 0 OR fnShots = 0) THEN " + ep.hitRatio + 
    						" ELSE ((fnKills/fnShots)*100) END), " + 
    						"fnCKS = " + ep.streak + ", fnCLS = " + ep.lstreak + 
    						", fnCWS = 0, fnRating = (CASE WHEN (fnDeaths = 0) THEN 0 ELSE ((fnKills/fnDeaths)*fnAve) END) " + 
    						"WHERE fcUserName = '" + Tools.addSlashesToString(ep.name.toLowerCase()) + 
    						"' AND fnGameType = " + cfg_gameType);
    		}catch(SQLException e){
    			Tools.printStackTrace(e);
    		}
    	}
    	avg_rating /= losers.size();
	    List<ElimPlayer> l = Arrays.asList(losers.values().toArray(new ElimPlayer[losers.values().size()]));
	    Collections.sort(l, Collections.reverseOrder(byMVP));//Best record
	    new MVPTimer(l.get(0).name);
	    m_botAction.sendArenaMessage("GAME OVER. Winner: " + winner.name + "!", Tools.Sound.HALLELUJAH);
	    lastWinner = winner.name;
    	Iterator<String> s = enabled.iterator();
    	while( s.hasNext() ){
    		String playerName = s.next();
    		Player p = m_botAction.getPlayer(playerName);
    		if(p == null)continue;
    		elimPlayers.put(playerName, new ElimPlayer(playerName));
    		doWarpIntoElim(playerName);
    	}
    	
    	try{
    		m_botAction.SQLQueryAndClose(db, "INSERT INTO tblElimGame (fnGameType, fcWinnerName, fnWinnerKills, fnWinnerDeaths, fnShipType, fnDeaths, fnNumPlayers, fnAvgRating, fdPlayed) VALUES( " + cfg_gameType + ", '" + Tools.addSlashesToString(winner.name.toLowerCase()) + "', " + winner.wins + ", " + winner.losses + ", " + shipType + ", " + deaths + ", " + losers.size() + ", " + avg_rating + ", NOW())");
    		ResultSet rs = m_botAction.SQLQuery(db, "SELECT fcUserName FROM tblElimPlayer WHERE fnGameType = " + cfg_gameType + " AND (fnKills + fnDeaths) > " + INIT_RANKING + " ORDER BY fnRating DESC");
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
    	game.moveOn(20 * Tools.TimeInMillis.SECOND);
    }
    
    public void vote(String name, String message){
    	if(!elimPlayers.containsKey(name)){
    		m_botAction.sendSmartPrivateMessage( name, "You must be playing to vote!");
    		return;
    	}
    	int vote = 0;
    	ElimPlayer player;
    	try{
    		vote = Integer.parseInt(message);
    	} catch(NumberFormatException e){
    		Tools.printStackTrace(e);//This should never happen
    	}
    	player = elimPlayers.get(name);
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
			gameStyle = ELIM;
			if(shipType > 10){
				shipType /= 10;
				gameStyle = KILLRACE;
			}
			int min = 1;
	    	if(winStreak >= LIMIT_STREAK)
	    		min = MIN_FOR_STREAK;
			if(gameStyle == ELIM)
				m_botAction.sendArenaMessage("This will be " + Tools.shipName(shipType) + " elim. VOTE: How many deaths? (" + min + "-" + cfg_maxDeaths + ")");
			else
				m_botAction.sendArenaMessage("This will be " + Tools.shipName(shipType) + " kill race. VOTE: How many kills? (" + min + "-" + cfg_maxKills + ")");
		} else if(game.state == GameStatus.VOTING_ON_DEATHS){//The deaths vote
			if(gameStyle == ELIM){
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
			} else {
				if(winners.isEmpty())
					kills = cfg_maxKills;
				else if(winners.size() > 1)
					kills = winners.get(rand.nextInt(winners.size()-1));
				else
					kills = winners.get(0);
				if(cfg_gameType == ELIM || shipType != 2)
					m_botAction.sendArenaMessage(Tools.shipName(shipType) + " kill race to " + kills);
				else
					m_botAction.sendArenaMessage(Tools.shipName(shipType) + " kill race to " + kills + ". VOTE: Shrap on or off? (1-on, 0-off)");
			}
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
    
    public void doElimZoner(){
    	if((System.currentTimeMillis() - lastZoner) < (MIN_ZONER * Tools.TimeInMillis.MINUTE))return;
    	if(winStreak == 1)
    		m_botAction.sendZoneMessage("Next " + cfg_gameName + " is starting. Last round's winner was " + lastWinner + "! Type ?go " + cfg_arena + " to play");
    	else if(winStreak > 1)
    		switch(winStreak){
	    		case 2:m_botAction.sendZoneMessage("Next " + cfg_gameName + " is starting. " + lastWinner + " has won 2 back to back! Type ?go " + cfg_arena + " to play");
	    			break;
	    		case 3:m_botAction.sendZoneMessage(cfg_gameName.toUpperCase() + ": " + lastWinner + " is on fire with a triple win! Type ?go " + cfg_arena + " to end the streak!", Tools.Sound.CROWD_OOO);
	    			break;
	    		case 4:m_botAction.sendZoneMessage(cfg_gameName.toUpperCase() + ": " + lastWinner + " is on a rampage! 4 wins in a row! Type ?go " + cfg_arena + " to put a stop to the carnage!", Tools.Sound.CROWD_GEE);
	    			break;
	    		case 5:m_botAction.sendZoneMessage(cfg_gameName.toUpperCase() + ": " + lastWinner + " is dominating with a 5 game streak! Type ?go " + cfg_arena + " to end this madness!", Tools.Sound.SCREAM);
	    			break;
	    		default:m_botAction.sendZoneMessage(cfg_gameName.toUpperCase() + ": " + lastWinner + " is bringing the zone to shame with " + winStreak + " consecutive wins! Type ?go " + cfg_arena + " to redeem yourselves!", Tools.Sound.INCONCEIVABLE);
	    			break;
    		}
    	else
    		m_botAction.sendZoneMessage("Next " + cfg_gameName + " is starting. Type ?go " + cfg_arena + " to play");
    	lastZoner = System.currentTimeMillis();
    }
    
    public void doWarpIntoElim(String name){
        if( cfg_casualAllowed == 1 )
            m_botAction.sendUnfilteredPrivateMessage(name, "*objoff " + CASUAL_LOGO_LVZ);
    	if(shipType == 6 && cfg_gameType == BASEELIM){
    		int[] xarena = cfg.getIntArray("XArena" + rand.nextInt(3), ",");
    		m_botAction.warpTo(name, xarena[0], xarena[1], xarena[2]);
    	}
    }
    
    public void doWarpIntoCasual(String name){
        if( cfg_casualAllowed == 1 ) {
            m_botAction.sendUnfilteredPrivateMessage(name, "*objon " + CASUAL_LOGO_LVZ);
            m_botAction.warpTo(name, cfg_casual[0], cfg_casual[1], cfg_casual[2]);
        } else {
            m_botAction.specWithoutLock(name);            
        }
    }
    
    public void doWarpIntoCasualSafe(String name){
    	if(cfg_casualAllowed != 1 || classicMode.contains(name))
    		m_botAction.specWithoutLock(name);
    	else{
	    	for(int i=0;i<9999;i++){
	    		if(m_botAction.getFrequencySize(i) == 0){
	    			m_botAction.setFreq(name, i);
	    			break;
	    		}    			
	    	}
	    	m_botAction.warpTo(name, cfg_safe[0], cfg_safe[1], cfg_safe[2]);
	    	m_botAction.sendUnfilteredPrivateMessage(name, "*objon " + CASUAL_SPLASH_LVZ);
	    	m_botAction.sendUnfilteredPrivateMessage(name, "*objon " + CASUAL_LOGO_LVZ);
    	}
    }
    

private class CasualPlayer{
	private int wins = 0, losses = 0;
	private String name;
	
	private CasualPlayer(String name){
		this.name = name;
		try{
			ResultSet rs = m_botAction.SQLQuery(db, "SELECT fcUserName FROM tblElimCasualRecs WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
			if(rs == null || !rs.next()){
				m_botAction.SQLClose(rs);
				m_botAction.SQLQueryAndClose(db, "INSERT INTO tblElimCasualRecs (fcUserName, fnKills, fnDeaths, fnGameType) VALUES ('" + Tools.addSlashesToString(name.toLowerCase()) + "', 0, 0, " + cfg_gameType + ")");
			}else m_botAction.SQLClose(rs);
		}catch(SQLException e){
			Tools.printStackTrace(e);
		}
	}
	
	private void gotWin(){
		wins += 1;
	}
	
	private void gotLoss(){
		losses += 1;
	}
	
	private void storeStats(){
		try{
			// avoid unnecessary updates to the database
			if (wins != 0 || losses != 0)
				m_botAction.SQLQueryAndClose(db, "UPDATE tblElimCasualRecs SET fnKills = fnKills + " + wins + ", fnDeaths = fnDeaths + " + losses + " WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType );
		}catch(SQLException e){
			Tools.printStackTrace(e);
		}
	}
}
    
private class ElimPlayer{
	private String name;
	//========================================================================
	//Bot variables
	//========================================================================
	private int shiptype = -1, vote = -1, frequency = -1, lagouts = 0;
	private long outOfBounds = 0, lagTime = -1, spawnTime = -1, killTime = -1;
	private boolean gotBorderWarning = false, gotChangeWarning = false;
	//========================================================================
	//Database variables
	//========================================================================
	private int initRating = 0, initWins = 0, initLosses = 0, BKS = 0, WLS = 0, BDK = 0;
	private float ave = 0;
	//========================================================================
	//Game statistic variables
	//========================================================================
	private int wins = 0, losses = 0;	
	private int shots = 0, quickKills = 0, streak = 0, lstreak = 0;
	private int streakBreaks = 0, eliminations = 0, doublekills = 0;
	private double hitRatio = 0;
	
	private ElimPlayer(String name){
		this.name = name;	
	}
	
	private void resetScore(){
		wins = 0;
		losses = 0;
		lagouts = 0;
	}
	
	private void gotWin(int enemyRating){
		if(System.currentTimeMillis() - killTime < (DOUBLE_KILL * Tools.TimeInMillis.SECOND)){
			quickKills++;
			doQuickKill();
		}
		else quickKills = 0;
		lstreak = 0;
		killTime = System.currentTimeMillis();
		wins += 1;
		ave = ((ave * (float)getTotalWins()) + (float)enemyRating)/((float)getTotalWins() + 1f);	
		streak += 1;
		if(streak >= STREAK_INIT && (streak - STREAK_INIT)%STREAK_REPEAT == 0){
			int i = (streak-STREAK_INIT)/STREAK_REPEAT;
			if(i>=sMessages.length)
				i = sMessages.length - 1;
			m_botAction.sendArenaMessage(name + " - " + sMessages[i] + "(" + streak + ":0)");
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
	
	private int getTotalWins(){
		return (wins + initWins);
	}
	
	private void gotLoss(boolean resetStreak){
		losses += 1;
		lstreak += 1;
		if(resetStreak)
			streak = 0;
		if(lstreak > WLS){
			WLS = lstreak;
			try{
				m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnWLS = " + lstreak + " WHERE fnGameType = " + cfg_gameType + " AND fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "'");
			}catch(SQLException e){
				Tools.printStackTrace(e);
			}
		}
	}
	
	private int getTotalLosses(){
		return (losses + initLosses) == 0 ? 1 : (losses + initLosses);
	}
	
	private void doQuickKill(){
		doublekills++;
		switch(quickKills){
			case 1:m_botAction.sendArenaMessage(name + " - Double kill!", Tools.Sound.CROWD_OHH);break;
			case 2:m_botAction.sendArenaMessage(name + " - Triple kill!", Tools.Sound.CROWD_GEE);break;
			case 3:m_botAction.sendArenaMessage(name + " - Quadruple kill!", Tools.Sound.INCONCEIVABLE);break;
			case 4:m_botAction.sendArenaMessage(name + " - Quintuple kill!", Tools.Sound.SCREAM);break;
			case 5:m_botAction.sendArenaMessage(name + " - Sextuple kill!", Tools.Sound.CRYING);break;
			case 6:m_botAction.sendArenaMessage(name + " - Septuple kill!", Tools.Sound.GAME_SUCKS);break;		
		}
		try{
			if(quickKills > BDK){
				BDK = quickKills;
				m_botAction.SQLQueryAndClose(db, "UPDATE tblElimPlayer SET fnBDK = " + BDK + " WHERE fnGameType = " + cfg_gameType + " AND fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "'");
			}
		}catch(SQLException e){
			Tools.printStackTrace(e);
		}
	}
	
	private void calculateRatios(){
		if(shots != 0)
			hitRatio = (((double)wins / shots) * 100);
		else
			hitRatio = (((double)wins / 1) * 100);
		if(hitRatio > 100) 
			hitRatio = 100;
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

public class CompareAlphabetical implements Comparator<ElimPlayer> {
	public int compare(ElimPlayer a, ElimPlayer b){
		return a.name.compareToIgnoreCase(b.name);
	}
}

public class CompareByWinsLossesAccuracy implements Comparator<ElimPlayer> {
	public int compare(ElimPlayer a,ElimPlayer b) {
		// compare by wins first
		if (a.wins > b.wins)return 1;
		else if(a.wins < b.wins)return -1;
		// least losses
		else if(a.losses < b.losses)return 1;
		else if(a.losses > b.losses)return -1;
		// try accuracy
		a.calculateRatios();
		b.calculateRatios();
		if(a.hitRatio > b.hitRatio)return 1;
		else if(a.hitRatio < b.hitRatio)return -1;
		// they did equally well...we leave it to the underlying implementation of Collections.sort for a pseudorandom pick
		return 0;
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
		try{ 
			m_botAction.scheduleTask(task, millis);
		}catch(Exception e){}
	}
	
	private void moveOn(){
		if(elimPlayers.size() < cfg_minPlayers && state < GAME_IN_PROGRESS){
			state = OFF_MODE;
			lastWinner = "-anonymous-";
			winStreak = 0;
		}
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
				else if(botMode == OFF){
					state = OFF_MODE;
					lastWinner = "-anonymous-";
					winStreak = 0;
				}
				else if(botMode == DISCONNECT){
					try{
						m_botAction.scheduleTask(new DieTask(m_botAction.getBotName()), 500);
					}catch( Exception e ){}
				}
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
    private boolean casual;
    private TimerTask runIt = new TimerTask() {
        public void run() {
        	if(!casual){
	        	if(shrap == ON)
	        		m_botAction.specificPrize(name, Tools.Prize.SHRAPNEL);
	        	m_botAction.specificPrize(name, Tools.Prize.MULTIFIRE);
	        	doWarpIntoElim(name);
	        	if(elimPlayers.containsKey(name)){
	        		elimPlayers.get(name).spawnTime = System.currentTimeMillis();
	        		if(elimPlayers.get(name).shiptype == 8){
	        			m_botAction.specificPrize(name, Tools.Prize.SHRAPNEL);
	        			m_botAction.specificPrize(name, Tools.Prize.SHRAPNEL);
	        		}
	        	}
        	}else
        		doWarpIntoCasual(name);
        }
    };
        
    public SpawnTimer(String name, boolean casual) {
        this.name = name;
        if( cfg_casualAllowed == 1 )
            this.casual = casual;
        else
            this.casual = false;
        try {
            m_botAction.scheduleTask(runIt, SPAWN_TIME);
        } catch( Exception e) {}
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
        try{
        	m_botAction.scheduleTask(runIt, 5 * Tools.TimeInMillis.SECOND);
        } catch( Exception e){}
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
    	m_botAction.sendUnfilteredPublicMessage("?chat=" + cfg_chats);
    }
    
    public void handleEvent(ArenaJoined event) {
    	for(int z=0;z<4;z++){
    		m_botAction.sendUnfilteredPublicMessage( "?set spawn:team" + z + "-x:" + cfg_competitive[0]);
    		m_botAction.sendUnfilteredPublicMessage( "?set spawn:team" + z + "-y:" + cfg_competitive[1]);
    		m_botAction.sendUnfilteredPublicMessage( "?set spawn:team" + z + "-radius:" + cfg_competitive[2]);
    	}
    	Iterator<Player> i = m_botAction.getPlayerIterator();
    	while( i.hasNext() ){
    		Player p = i.next();
    		String name = p.getPlayerName();
    		if(!opList.isBotExact(name)){
    		    if( cfg_casualAllowed == 1 ) {
    		        casualPlayers.put(name, new CasualPlayer(name));
    		        if(cfg_gameType == BASEELIM)
    		            m_botAction.sendUnfilteredPrivateMessage(name, "*einfo");
    		        enabled.add(name);
    		    } else {
    	            if(p.getShipType() > 0){
    	                elimPlayers.put(name, new ElimPlayer(name));
    	                doWarpIntoElim(name);
    	            }
    		    }
    		}
    		try{
        		ResultSet rs = m_botAction.SQLQuery(db, "SELECT fnSpecWhenOut, fnElim FROM tblElimPlayer WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
        		if(rs != null && rs.next()){
        			if(rs.getInt("fnElim") == 0)
        				enabled.remove(name);
        			if(cfg_casualAllowed != 1 || rs.getInt("fnSpecWhenOut") == 1)
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
    	if(name == null)return;
    	else if(!game.isInProgress() || !elimPlayers.containsKey(name))return;
    	elimPlayers.get(name).shots += 1;   	
    }
    
    public void handleEvent(PlayerDeath event) {
    	String win = m_botAction.getPlayerName(event.getKillerID());
    	String loss = m_botAction.getPlayerName(event.getKilleeID());
    	if(win == null || loss == null)return;
    	Player winp = m_botAction.getPlayer(win);
    	Player lossp = m_botAction.getPlayer(loss);
    	if(opList.isBotExact(win) || opList.isBotExact(loss) || winp == null || lossp == null)return;
    	casualPlayers.get(win).gotWin();
    	casualPlayers.get(loss).gotLoss();
    	ElimPlayer w = findCollection(win);
    	ElimPlayer l = findCollection(loss);
    	if(!(game.state == GameStatus.GAME_IN_PROGRESS) || w == null || l == null || winp.getYTileLocation() < (cfg_safe[1] + SAFE_HEIGHT)){
    		new SpawnTimer(loss, true);
    		return;
    	}
    	if((System.currentTimeMillis() - l.spawnTime) < (SPAWN_NC * Tools.TimeInMillis.SECOND)||
    	   (System.currentTimeMillis() - w.spawnTime) < (SPAWN_NC * Tools.TimeInMillis.SECOND)){
    		m_botAction.sendSmartPrivateMessage( win, "Spawn kill(No count).");
    		m_botAction.sendSmartPrivateMessage( loss, "Spawn kill(No count).");
    		return;
    	}
    	if(winp.getShipType() == 0 || elimPlayers.containsKey(win))
    		w.gotWin(l.initRating);
    	if(l.streak >= 5){
    		w.streakBreaks++;
    		m_botAction.sendArenaMessage("Streak breaker! " + loss + "(" + l.streak + ":0) broken by " + win + "!", Tools.Sound.INCONCEIVABLE);
    	}
    	if(lossp.getShipType() == 0 || elimPlayers.containsKey(loss))
    		l.gotLoss(true);
    	if(gameStyle == ELIM && l.losses == deaths && elimPlayers.containsKey(loss)){
    		w.eliminations++;
    		m_botAction.sendArenaMessage(loss + " is out. " + l.wins + " wins " + l.losses + " losses");
    		losers.put(loss, elimPlayers.remove(loss));
    		doWarpIntoCasualSafe(loss);
    	}else if(gameStyle == KILLRACE && w.wins == kills && elimPlayers.containsKey(win)){
    		Iterator<ElimPlayer> i = elimPlayers.values().iterator();
    		while( i.hasNext() ){
    			ElimPlayer ep = i.next();
    			if(!ep.name.equalsIgnoreCase(w.name)){
    				losers.put(ep.name, ep);
    				doWarpIntoCasualSafe(ep.name);
    				i.remove();    				
    			}
    		}
    	}else if(elimPlayers.containsKey(loss)){
    		l.clearBorderInfo();
    		new SpawnTimer(loss, false);
    	}else
    		new SpawnTimer(loss, true);
    	if(elimPlayers.size() == 1){
    		winner = elimPlayers.get(elimPlayers.firstKey());
    		game.moveOn();
    	}
    }
    
    public void handleEvent(PlayerEntered event) {
    	String name = m_botAction.getPlayerName(event.getPlayerID());
    	if(name == null || opList.isBotExact(name))return;
    	casualPlayers.put(name, new CasualPlayer(name));
    	if(cfg_gameType == BASEELIM)
    		m_botAction.sendUnfilteredPrivateMessage(name, "*einfo");    	
    	m_botAction.sendSmartPrivateMessage( name, "Welcome to " + cfg_arena + "! " + getStatusMsg());
    	enabled.add(name);
    	if(!game.isInProgress()){
    		elimPlayers.put(name, new ElimPlayer(name));
    		if(elimPlayers.size() >= cfg_minPlayers && game.state == GameStatus.WAITING_FOR_PLAYERS)
    			game.moveOn();
    	}
    	try{
    		ResultSet rs = m_botAction.SQLQuery(db, "SELECT fnSpecWhenOut, fnElim FROM tblElimPlayer WHERE fcUserName = '" + Tools.addSlashesToString(name.toLowerCase()) + "' AND fnGameType = " + cfg_gameType);
    		if(rs != null && rs.next()){
    			if(rs.getInt("fnElim") == 0)
    				enabled.remove(name);
    			if(cfg_casualAllowed != 1 || rs.getInt("fnSpecWhenOut") == 1)
    				classicMode.add(name);
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
    	CasualPlayer c = casualPlayers.get(name);
    	if( c != null )
    	    c.storeStats();
    	casualPlayers.remove(name);
    	if(game.isInProgress() && elimPlayers.containsKey(name)){
    		lagouts.put(name, elimPlayers.remove(name));
    		lagouts.get(name).lagTime = System.currentTimeMillis();
    		lagouts.get(name).lagouts++;
    		if(lagouts.get(name).lagouts >= MAX_LAGOUT){
    			m_botAction.sendArenaMessage(name + " is out. " + lagouts.get(name).wins + " wins " + lagouts.get(name).losses + " losses (Too many lagouts)");
    			losers.put(name, lagouts.remove(name));
        		doWarpIntoCasualSafe(name);
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
    		if(lagouts.get(name).lagouts >= MAX_LAGOUT){
    			m_botAction.sendArenaMessage(name + " is out. " + lagouts.get(name).wins + " wins " + lagouts.get(name).losses + " losses (Too many lagouts)");
    			losers.put(name, lagouts.remove(name));
        		doWarpIntoCasualSafe(name);
    		}   		
    		if(elimPlayers.size() == 1 && game.state == GameStatus.GAME_IN_PROGRESS){
    			winner = elimPlayers.get(elimPlayers.firstKey());
        		game.moveOn();
    		}
    	} else if(!elimPlayers.containsKey(name) && event.getShipType() != cfg_defaultShip && event.getShipType() > 0){
    		m_botAction.setShip(name, cfg_defaultShip);
    		doWarpIntoCasual(name);
    	} else if(!elimPlayers.containsKey(name) && game.isInProgress() && event.getShipType() > 0){
    	    if( cfg_casualAllowed == 1 ) {
    	        m_botAction.sendUnfilteredPrivateMessage(name, "*objon " + CASUAL_LOGO_LVZ);
    	        m_botAction.sendUnfilteredPrivateMessage(name, "*objon " + CASUAL_SPLASH_LVZ);
                if(enabled.contains(name))
                    m_botAction.sendSmartPrivateMessage( name, "You have entered casual play. Please wait for the next game to begin to participate.");
    	    }
    		doWarpIntoCasual(name);
    	} else if(game.isInProgress() && elimPlayers.containsKey(name) && event.getShipType() > 0){
    		ElimPlayer ep = elimPlayers.get(name);
    		if(event.getShipType() != ep.shiptype){
	    		m_botAction.setShip(name, ep.shiptype);
	    		doWarpIntoElim(name);
	    		if(game.state == GameStatus.GAME_IN_PROGRESS){
		    		if(!ep.gotChangeWarning){
		    			ep.gotLoss(false);
		    			m_botAction.sendArenaMessage(name + " has attempted to change ships - +1 death");
		    			if(ep.losses == deaths && gameStyle == ELIM){
		    	    		m_botAction.sendArenaMessage(ep.name + " is out. " + ep.wins + " wins " + ep.losses + " losses");
		    	    		losers.put(ep.name, elimPlayers.remove(ep.name));
		    	    		doWarpIntoCasualSafe(ep.name);
		    	    	}else{
		    	    		m_botAction.sendSmartPrivateMessage( name, "Attempt to change ships or frequencies again and you will be removed from the game. You have been warned.");
		    	    		ep.gotChangeWarning = true;
		    	    	}
		    		}else{
		    			m_botAction.sendArenaMessage(name + " is out. " + ep.wins + " wins " + ep.losses + " losses (Disqualified)");
		    			losers.put(name, elimPlayers.remove(name));
		        		doWarpIntoCasualSafe(name);
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
    		if(event.getFrequency() != elimPlayers.get(name).frequency  && event.getFrequency() != SPEC_FREQ){
    			ElimPlayer ep = elimPlayers.get(name);
    			m_botAction.setFreq(name, ep.frequency);
    			doWarpIntoElim(name);
    			if(!ep.gotChangeWarning){
    				ep.gotLoss(false);
    				m_botAction.sendArenaMessage(name + " has attempted to change frequencies - +1 death");
    				if(ep.losses == deaths && gameStyle == ELIM){
	    	    		m_botAction.sendArenaMessage(ep.name + " is out. " + ep.wins + " wins " + ep.losses + " losses");
	    	    		losers.put(ep.name, elimPlayers.remove(ep.name));
	    	    		doWarpIntoCasualSafe(ep.name);
	    	    	}else{
	    	    		m_botAction.sendSmartPrivateMessage( name, "Attempt to change ships or frequencies again and you will be removed from the game. You have been warned.");
    					ep.gotChangeWarning = true;
	    	    	}
    			} else {		
    				m_botAction.sendArenaMessage(name + " is out. " + ep.wins + " wins " + ep.losses + " losses (Disqualified)");
    				losers.put(name, elimPlayers.remove(name));
    				doWarpIntoCasualSafe(name);
    			}
    		}
    	} else if(!elimPlayers.containsKey(name) && event.getFrequency() > MAX_FREQ){
    		m_botAction.sendSmartPrivateMessage( name, "Please choose a private frequency under " + (MAX_FREQ+1) + ".");
    		doWarpIntoCasual(name);
    	}
    }
    
    public void handleEvent(PlayerPosition event){
    	Player p = m_botAction.getPlayer(event.getPlayerID());
    	if(p == null)return;
    	if(p.getYTileLocation() < (cfg_safe[1]+SAFE_HEIGHT) && elimPlayers.containsKey(p.getPlayerName())){
    		m_botAction.specificPrize(p.getPlayerName(), Tools.Prize.WARP);
    		doWarpIntoElim(p.getPlayerName()); 
    	}
    	else if(p.getYTileLocation() > (cfg_safe[1]+SAFE_HEIGHT) && !elimPlayers.containsKey(p.getPlayerName())){
    		doWarpIntoCasual(p.getPlayerName());
    	}
    	if(cfg_gameType == ELIM)return;
    	ElimPlayer ep = elimPlayers.get(p.getPlayerName());
	    if(ep != null && game.isInProgress()){
	    	if(p.getYTileLocation() < cfg_border)
	    		ep.clearBorderInfo();
		    else if(p.getYTileLocation() > cfg_border){    		
		    	if(ep.outOfBounds == 0)
		    		ep.outOfBounds = System.currentTimeMillis();
		    	else if((System.currentTimeMillis() - ep.outOfBounds) > BOUNDARY_TIME * Tools.TimeInMillis.SECOND){
		    		m_botAction.sendArenaMessage(ep.name + " is out. " + ep.wins + " wins " + ep.losses + " losses (Too long outside base)");
		       		losers.put(ep.name, elimPlayers.remove(ep.name));
		       		doWarpIntoCasualSafe(ep.name);
		    	}
		    	else if((System.currentTimeMillis() - ep.outOfBounds) > (BOUNDARY_TIME/2) * Tools.TimeInMillis.SECOND && !ep.gotBorderWarning){
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
    		if(p.getYTileLocation() > cfg_barY[0] && p.getYTileLocation() < cfg_barY[1]){
    			for(int i=0;i<cfg_barspots.length;i++){
    				if(p.getXTileLocation() > cfg_barspots[i][0] && p.getXTileLocation() < cfg_barspots[i][1])
    					m_botAction.warpTo(p.getPlayerName(), cfg_barspots[i][2], cfg_barspots[i][3]);
    			}
    		}
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

    public ElimPlayer findCollection(String name){
    	if(elimPlayers.containsKey(name))
    		return elimPlayers.get(name);
    	else if(losers.containsKey(name))
    		return losers.get(name);
    	else if(lagouts.containsKey(name))
    		return lagouts.get(name);
    	else return null;
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