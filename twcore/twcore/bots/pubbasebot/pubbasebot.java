package twcore.bots.pubbasebot;

import java.util.*;
import twcore.core.*;
import twcore.core.events.Message;
import twcore.core.events.FlagClaimed;
import twcore.core.events.ScoreUpdate;
import twcore.core.events.PlayerDeath;
import twcore.core.events.LoggedOn;
import twcore.core.events.ArenaList;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.FrequencyChange;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.*;


public class pubbasebot extends SubspaceBot {
	
	OperatorList m_opList;
	
	HashMap<Integer,FrequencyStats> freqs;
	HashMap<String,String> lowerToUpper;
	HashMap<String,PlayerStats> playerStats;
	
	HashSet<String> baseWarp;
	
	boolean isRunning = false;
	boolean levisAllowed = true;
	boolean privateFreqs = true;
	int secondsNeeded = 0;
	int secondsBetweenRounds = 0;
	
	TimerTask updateAnnounce;
	TimerTask checkTimes;
	
	int secondsLeftLast = 0;
	int minutesLeftLast = 0;
	
	int currentHolder = -1;
	
	int updates = 1;
	
	BotSettings m_botSettings;
	
	public pubbasebot(BotAction botAction) {
		super(botAction);
		requestEvents();
		m_opList = m_botAction.getOperatorList();
		m_botSettings = m_botAction.getBotSettings();
		freqs = new HashMap<Integer,FrequencyStats>();
		lowerToUpper = new HashMap<String,String>();
		playerStats = new HashMap<String,PlayerStats>();
		baseWarp = new HashSet<String>();
	}
	
	public void requestEvents() {
		EventRequester er = m_botAction.getEventRequester();
		er.request(EventRequester.MESSAGE);
		er.request(EventRequester.FLAG_CLAIMED);
		er.request(EventRequester.SCORE_UPDATE);
		er.request(EventRequester.PLAYER_DEATH);
		er.request(EventRequester.LOGGED_ON);
		er.request(EventRequester.ARENA_LIST);
		er.request(EventRequester.FREQUENCY_SHIP_CHANGE);
		er.request(EventRequester.FREQUENCY_CHANGE);
		er.request(EventRequester.PLAYER_ENTERED);
		er.request(EventRequester.PLAYER_LEFT);
	}
	
	public void handleEvent(Message event) {
		String message = event.getMessage();
		if(event.getMessageType() == Message.PRIVATE_MESSAGE) {
			String sender = m_botAction.getPlayerName(event.getPlayerID());
			if(m_opList.isSmod(sender))
				handleCommand(sender, message, true);
			else
				handleCommand(sender, message, false);
		}
	}
	
	public void handleEvent(FlagClaimed event) {
		if(!isRunning) return;
		Player p = m_botAction.getPlayer(event.getPlayerID());
		String name = p.getPlayerName();
		try {
			playerStats.get(name.toLowerCase()).addFlagClaim();
    	} catch(Exception e) {
    		playerStats.put(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase(), new PlayerStats(m_botAction.getPlayer(event.getPlayerID()).getScore(),m_botAction.getPlayer(event.getPlayerID()).getFrequency()));
    		lowerToUpper.put(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase(), m_botAction.getPlayerName(event.getPlayerID()));
    		playerStats.get(name.toLowerCase()).addFlagClaim();
    	}
    	try {
			freqs.get(currentHolder).endHold();
    	} catch(Exception e) {}
		int secondsLeft = (secondsNeeded - freqs.get(currentHolder).getHoldSeconds());
		currentHolder = p.getFrequency();
		try {
			freqs.get(currentHolder).startHold();
		} catch(Exception e) {
			freqs.put(currentHolder, new FrequencyStats());
			freqs.get(currentHolder).startHold();
		}
	}
	
	public void handleEvent(ScoreUpdate event) {
		if(!isRunning) return;
		String name = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
		playerStats.get(name).addPoints(event.getFlagPoints() + event.getKillPoints());
	}
	
	public void handleEvent(PlayerDeath event) {
		if(!isRunning) return;
		String killer = m_botAction.getPlayerName(event.getKillerID()).toLowerCase();
		String killee = m_botAction.getPlayerName(event.getKilleeID()).toLowerCase();
		playerStats.get(killer).addKill();
		playerStats.get(killee).addDeath();
	}
	
	public void handleEvent(LoggedOn event) {
		m_botAction.requestArenaList();
	}
	
	public void handleEvent(ArenaList event) {
		String[] arenaNames = event.getArenaNames();
		int initialPub = m_botSettings.getInt("InitialArena"+m_botAction.getBotNumber());
        Comparator a = new Comparator()
        {
            public int compare(Object oa, Object ob)
            {
                String a = (String)oa;
                String b = (String)ob;
                if (Tools.isAllDigits(a) && !a.equals("") ) {
                    if (Tools.isAllDigits(b) && !b.equals("") ) {
                        if (Integer.parseInt(a) < Integer.parseInt(b)) {
                            return -1;
                        } else {
                            return 1;
                        }
                    } else {
                        return -1;
                    }
                } else if (Tools.isAllDigits(b)) {
                    return 1;
                } else {
                    return a.compareToIgnoreCase(b);
				}
            };
        };

        Arrays.sort(arenaNames, a);

    	String arenaToJoin = arenaNames[initialPub];
    	if(Tools.isAllDigits(arenaToJoin))
    	{
    		m_botAction.changeArena(arenaToJoin);
    		startBot();
    	}
    }
    
    public void handleEvent(FrequencyChange event) {
    	if(privateFreqs) return;
    	
    	if(event.getFrequency() > 1) {
    		if(getFreqSize(0) > getFreqSize(1)) {
    			m_botAction.setFreq(event.getPlayerID(), 1);
    		} else {
    			m_botAction.setFreq(event.getPlayerID(), 0);
    		}
    		m_botAction.sendPrivateMessage(event.getPlayerID(), "Private frequencies are currently disabled.");
    	}
    	
    	if(!isRunning) return;
    	try {
    		if(m_botAction.getPlayer(event.getPlayerID()).getShipType() != 0)
    			playerStats.get(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase()).changeFreq(event.getFrequency());
    	} catch(Exception e) {
    		playerStats.put(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase(), new PlayerStats(m_botAction.getPlayer(event.getPlayerID()).getScore(),m_botAction.getPlayer(event.getPlayerID()).getFrequency()));
    		lowerToUpper.put(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase(), m_botAction.getPlayerName(event.getPlayerID()));
    	}
    }
    
    
    public void handleEvent(FrequencyShipChange event) {
    	if(!privateFreqs && event.getFrequency() > 1 && event.getShipType() != 0) {
    		if(getFreqSize(0) > getFreqSize(1)) {
    			m_botAction.setFreq(event.getPlayerID(), 1);
    		} else {
    			m_botAction.setFreq(event.getPlayerID(), 0);
    		}
    		m_botAction.sendPrivateMessage(event.getPlayerID(), "Private frequencies are currently disabled.");
    	}
    	
    	if(!levisAllowed && event.getShipType() == 4) {
    		m_botAction.setShip(event.getPlayerID(), 1);
    		m_botAction.sendPrivateMessage(event.getPlayerID(), "Leviathans are currently disabled.");
    	}
    	
    	if(!isRunning) return;
    	try {
    		if(event.getShipType() == 0) playerStats.get(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase()).stopTimeLog();
    		else playerStats.get(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase()).changeFreq(event.getFrequency());
    	} catch(Exception e) {
    		playerStats.put(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase(), new PlayerStats(m_botAction.getPlayer(event.getPlayerID()).getScore(),m_botAction.getPlayer(event.getPlayerID()).getFrequency()));
    		lowerToUpper.put(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase(), m_botAction.getPlayerName(event.getPlayerID()));
    	}
    }
    
    public void handleEvent(PlayerEntered event) {
    	String name = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
    	m_botAction.sendPrivateMessage(name, "Welcome to Pub Basing. The current game mode is: ");
    	if(privateFreqs)
    		m_botAction.sendPrivateMessage(name, "Private frequencies: disabled");
    	else 
    		m_botAction.sendPrivateMessage(name, "Private frequencies: enabled");
    	if(!levisAllowed)
    		m_botAction.sendPrivateMessage(name, "Leviathans: disabled");
    	else
    		m_botAction.sendPrivateMessage(name, "Leviathans: enabled");
    	m_botAction.sendPrivateMessage(name, "Time Race to: " + (secondsNeeded / 60) + " minutes " + (secondsNeeded % 60) + " seconds.");
    	m_botAction.sendPrivateMessage(name, "PM me with !warp to be warped into base at start.");
    	if(!isRunning) return;
    	if(playerStats.get(name) == null) {
    		playerStats.put(name, new PlayerStats(m_botAction.getPlayer(name).getScore(),m_botAction.getPlayer(name).getFrequency()));
    		lowerToUpper.put(name, m_botAction.getPlayerName(event.getPlayerID()));
    	}
    }
    
    public void handleEvent(PlayerLeft event) {
    	if(!isRunning) return;
    	try {
    		playerStats.get(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase()).stopTimeLog();
    	} catch(Exception e) {}
    }
    
    public void startBot() {
    	secondsNeeded = m_botSettings.getInt("SecondsToWin"+m_botAction.getBotNumber());
    	levisAllowed = m_botSettings.getInt("LeviAllowed"+m_botAction.getBotNumber()) == 1;
    	privateFreqs = m_botSettings.getInt("PrivateFreqs"+m_botAction.getBotNumber()) == 1;
    	secondsBetweenRounds = m_botSettings.getInt("BetweenTime"+m_botAction.getBotNumber());
    	TimerTask startRound = new TimerTask() {
    		public void run() {
    			startRound();
    		}
    	};
    	m_botAction.scheduleTask(startRound, secondsBetweenRounds * 1000);
    	m_botAction.sendArenaMessage("Welcome to Pub Basing. The current game mode is: ");
    	if(privateFreqs)
    		m_botAction.sendArenaMessage("Private frequencies: disabled");
    	else 
    		m_botAction.sendArenaMessage("Private frequencies: enabled");
    	if(!levisAllowed)
    		m_botAction.sendArenaMessage("Leviathans: disabled");
    	else
    		m_botAction.sendArenaMessage("Leviathans: enabled");
    	m_botAction.sendArenaMessage("Time Race to: " + (secondsNeeded / 60) + " minutes " + (secondsNeeded % 60) + " seconds.");
    	m_botAction.sendArenaMessage("PM me with !warp to be warped into base at start.", 2);
    	m_botAction.sendArenaMessage("The next game will begin in: " + (secondsBetweenRounds / 60) + " minutes " + (secondsBetweenRounds % 60) + " seconds.");
    }
    
    public String addSpacesToString(String str, int totalLength) {
    	for(int k = str.length();k <= totalLength;k++) {
    		str += " ";
    	}
    	if(str.length() > totalLength) str = str.substring(0, totalLength);
    	return str;
    }
    
    public String addSpacesToString(int num, int totalLength) {
    	String str = "" + num;
    	for(int k = str.length();k <= totalLength;k++) {
    		str += " ";
    	}
    	if(str.length() > totalLength) str = str.substring(0, totalLength);
    	return str;
    }
    
    public void startRound() {
    	int updates = 1;
    	try {
    		if(updateAnnounce != null) updateAnnounce.cancel();
	    	if(checkTimes != null) checkTimes.cancel();
	    } catch(Exception e) {}
    	updateAnnounce = new TimerTask() {
    		public void run() {
    			inTheLead();
    		}
    	};
    	checkTimes = new TimerTask() {
    		public void run() {
    			checkForWin();
    		}
    	};
    	secondsNeeded = m_botSettings.getInt("SecondsToWin"+m_botAction.getBotNumber());
    	levisAllowed = m_botSettings.getInt("LeviAllowed"+m_botAction.getBotNumber()) == 1;
    	privateFreqs = m_botSettings.getInt("PrivateFreqs"+m_botAction.getBotNumber()) == 1;
    	secondsBetweenRounds = m_botSettings.getInt("BetweenTime"+m_botAction.getBotNumber());
    	m_botAction.scheduleTaskAtFixedRate(checkTimes, 2 * 1000, 2 * 1000);
    	m_botAction.scheduleTaskAtFixedRate(updateAnnounce, 5 * 60 * 1000, 5 * 60 * 1000);
    	Iterator it = m_botAction.getPlayerIterator();
    	while(it.hasNext()) {
    		Player itP = (Player)it.next();
    		String name = itP.getPlayerName();
    		String lowerName = name.toLowerCase();
    		int freq = itP.getFrequency();
    		int ship = itP.getShipType();
    		if(ship == 4 && !levisAllowed)
    			m_botAction.setShip(name, 1);
    		if(freq > 1 && !privateFreqs) {
    			if(getFreqSize(0) > getFreqSize(1)) {
	    			m_botAction.setFreq(name, 1);
	    			freq = 1;
	    		} else {
	    			m_botAction.setFreq(name, 0);
	    			freq = 0;
	    		}
    		}
    		playerStats.put(lowerName, new PlayerStats(itP.getScore(), freq));
    		lowerToUpper.put(lowerName, name);
    	}
    	doBaseWarp();
    }
    
    public void inTheLead() {
    	int[] leadStats = getMax();
    	m_botAction.sendArenaMessage("Current time in game: " + (updates*5) + ":00");
    	m_botAction.sendArenaMessage("Frequency #" + leadStats[0] + " with " + (leadStats[1] / 60) + " minutes "+ (leadStats[1] % 60) + " seconds.");
    	updates++;
    }
    
    public void checkForWin() {
    	int[] maxStats = getMax();
    	if(maxStats[1] >= secondsNeeded) {
    		checkTimes.cancel();
    		updateAnnounce.cancel();
    		m_botAction.sendArenaMessage("Frequency #" + maxStats[0] + " has won the game!", 5);
    		handleGameOver(maxStats[0]);
    		m_botAction.sendUnfilteredPublicMessage("*objoff 1"+(minutesLeftLast/10));
    		m_botAction.sendUnfilteredPublicMessage("*objoff 2"+(minutesLeftLast%10));
    		m_botAction.sendUnfilteredPublicMessage("*objoff 3"+(secondsLeftLast/10));
    		m_botAction.sendUnfilteredPublicMessage("*objoff 4"+(secondsLeftLast%10));
    	} else {
    		int timeLeft = secondsNeeded - maxStats[1];
    		int minutesLeft = timeLeft / 60;
    		int secondsLeft = timeLeft % 60;
    		m_botAction.sendUnfilteredPublicMessage("*objoff 1"+(minutesLeftLast/10));
    		m_botAction.sendUnfilteredPublicMessage("*objoff 2"+(minutesLeftLast%10));
    		m_botAction.sendUnfilteredPublicMessage("*objoff 3"+(secondsLeftLast/10));
    		m_botAction.sendUnfilteredPublicMessage("*objoff 4"+(secondsLeftLast%10));
    		m_botAction.sendUnfilteredPublicMessage("*objon 1"+(minutesLeft/10));
    		m_botAction.sendUnfilteredPublicMessage("*objon 2"+(minutesLeft%10));
    		m_botAction.sendUnfilteredPublicMessage("*objon 3"+(secondsLeft/10));
    		m_botAction.sendUnfilteredPublicMessage("*objon 4"+(secondsLeft%10));
    		minutesLeftLast = minutesLeft;
    		secondsLeftLast = secondsLeft;
    		if(secondsLeft <= 30 && secondsLeft > 10) {
    			m_botAction.sendArenaMessage(maxStats[0] + " has 30 seconds remaining!",2);
    		} else if(secondsLeft <= 10) {
    			m_botAction.sendArenaMessage(maxStats[0] + " will win in 10 seconds!",2);
    		}
    	}
    }
    
    public int getFreqSize(int freq) {
    	Iterator it = m_botAction.getFreqPlayerIterator(freq);
    	int num = 0;
    	while(it.hasNext()) {
    		it.next(); num++;
    	}
    	return num;
    }
    
    public void handleGameOver(int freq) {
    	TimerTask startRound = new TimerTask() {
    		public void run() {
    			startRound();
    		}
    	};
    	m_botAction.scheduleTask(startRound, secondsBetweenRounds * 1000);
    	ArrayList orderedRating = new ArrayList();
    	Iterator it = playerStats.keySet().iterator();
    	while(it.hasNext()) {
    		String name = (String)it.next();
    		int rating = playerStats.get(name).getRating();
    		for(int k = 0;k < orderedRating.size();k++) {
    			if(rating > playerStats.get((String)orderedRating.get(k)).getRating())
    				orderedRating.add(k, name);
    		}
    		if(orderedRating.indexOf(name) < 0) orderedRating.add(name);
       	}
       	m_botAction.sendArenaMessage("Top 5 Players: ", 2);
       	for(int k = 0;k < 5 && k < orderedRating.size();k++) {
       		String name = (String)orderedRating.get(k);
       		PlayerStats p = playerStats.get(name);
       		int[] stats = p.getStats();
       		m_botAction.sendArenaMessage("Name: " + addSpacesToString(lowerToUpper.get(name),20) + " Rating: " + addSpacesToString(""+p.getRating(),5)
       			+ " Kills: " + addSpacesToString(stats[0],3) + " Deaths: " + addSpacesToString(stats[1],3)
       			+ " Points: " + addSpacesToString(stats[3],5) + " Flag Claims: " +stats[2]);
       	}
       	for(int k = 0;k < orderedRating.size();k++) {
       		String name = (String)orderedRating.get(k);
       		PlayerStats p = playerStats.get(name);
       		int points = (int)(p.getRating() * freqHoldPercent(p.currentFreq));
       		m_botAction.sendUnfilteredPrivateMessage(name, "*points " + points);
       		m_botAction.sendPrivateMessage(name, "You have received " + points + " points for having a rating of " + p.getRating() + " and your team holding the base " + (freqHoldPercent(p.currentFreq)*100) + "% of the game.");
       	}
       	playerStats.clear();
       	lowerToUpper.clear();
       	freqs.clear();
       	isRunning = false;
    }
    
    public double freqHoldPercent(int f) {
    	try {
    		int freqHold = freqs.get(f).getHoldSeconds();
    		return ((double)freqHold/secondsNeeded);
    	} catch(Exception e) {}
    	return 0;
    }
    
    public int[] getMax() {
    	if(!isRunning) {
    		int negs[] = {-1,-1};
    		return negs;
    	}
    	int highest[] = {-1,-1};
    	Iterator it = freqs.keySet().iterator();
    	while(it.hasNext()) {
    		int freq = (Integer)it.next();
    		FrequencyStats fstats = freqs.get(freq);
    		if(fstats.getHoldSeconds() > highest[1])  {
    			highest[0] = freq;
    			highest[1] = fstats.getHoldSeconds();
    		}
    	}
    	return highest;
    }
	
	public void handleCommand(String name, String message, boolean smod) {
		if(smod) {
			if(message.toLowerCase().startsWith("!levion")) {
				m_botSettings.put("LeviAllowed"+m_botAction.getBotNumber(),1);
				m_botAction.sendPrivateMessage(name, "Levis enabled, settings will take affect next round.");
			} else if(message.toLowerCase().startsWith("!levioff")) {
				m_botAction.sendPrivateMessage(name, "Levis disabled, settings will take affect next round.");
				m_botSettings.put("LeviAllowed"+m_botAction.getBotNumber(),0);
			} else if(message.toLowerCase().startsWith("!privon")) {
				m_botAction.sendPrivateMessage(name, "Private freqs enabled, settings will take affect next round.");
				m_botSettings.put("PrivFreqs"+m_botAction.getBotNumber(),1);
			} else if(message.toLowerCase().startsWith("!privoff")) {
				m_botAction.sendPrivateMessage(name, "Private freqs disabled, settings will take affect next round.");
				m_botSettings.put("PrivFreqs"+m_botAction.getBotNumber(),0);
			} else if(message.toLowerCase().startsWith("!secinround ")) {
				String pieces[] = message.split(" ");
				try {
					m_botSettings.put("SecondsToWin"+m_botAction.getBotNumber(),new Integer(pieces[1]));
					m_botAction.sendPrivateMessage(name, "Seconds to win set to " + pieces[1]);
				} catch(Exception e) {m_botAction.sendPrivateMessage(name, "Invalid input.");}
			} else if(message.toLowerCase().startsWith("!secbetween ")) {
				String pieces[] = message.split(" ");
				try {
					m_botSettings.put("BetweenTime"+m_botAction.getBotNumber(),new Integer(pieces[1]));
					m_botAction.sendPrivateMessage(name, "Between time set to " + pieces[1]);
				} catch(Exception e) {m_botAction.sendPrivateMessage(name, "Invalid input.");}
			}
		}
		
		if(message.toLowerCase().startsWith("!stats ")) {
			String pieces[] = message.split(" ",2);
			getStats(name, pieces[1]);
		} else if(message.toLowerCase().startsWith("!stats")) {
			String pieces[] = message.split(" ",2);
			getStats(name, pieces[1]);
		} else if(message.toLowerCase().startsWith("!fstats ")) {
			String pieces[] = message.split(" ");
			try {
				getFreqStats(name, new Integer(pieces[1]));
			} catch(Exception e) {m_botAction.sendPrivateMessage(name, "Invalid input.");}
		} else if(message.toLowerCase().startsWith("!warp")) {
			if(baseWarp.remove(name.toLowerCase())) {
				m_botAction.sendPrivateMessage(name, "Basewarp disabled.");
			} else {
				baseWarp.add(name.toLowerCase());
				m_botAction.sendPrivateMessage(name, "Basewarp enabled.");
			}
		} else if(message.toLowerCase().startsWith("!help")) {
			handleHelp(name, smod);
		}
	}
	
	public void getFreqStats(String name, int freq) {
		try {
			FrequencyStats fs = freqs.get(freq);
			m_botAction.sendPrivateMessage(name, "Freq #" + freq + " has held the flag for " + fs.getHoldSeconds() + " seconds.");
		} catch(Exception e) {
			m_botAction.sendPrivateMessage(name, "Could not find frequency.");
		}
	}
	
	public void getStats(String name, String player) {
		PlayerStats p = playerStats.get(player.toLowerCase());
		if(p == null) {
			m_botAction.sendPrivateMessage(name, "No stats on record.");
			return;
		}
       	int[] stats = p.getStats();
       	m_botAction.sendPrivateMessage(name, "Name: " + addSpacesToString(lowerToUpper.get(player),20) + " Rating: " + addSpacesToString(""+p.getRating(),5)
       		+ " Kills: " + addSpacesToString(stats[0],3) + " Deaths: " + addSpacesToString(stats[1],3)
       		+ " Points: " + addSpacesToString(stats[3],5) + " Flag Claims: " +stats[2]);
	}
	
	public void doBaseWarp() {
		Random rand = new Random();
		int minX = m_botSettings.getInt("BaseWarpMinX");
		int maxX = m_botSettings.getInt("BaseWarpMaxX");
		int minY = m_botSettings.getInt("BaseWarpMinY");
		int maxY = m_botSettings.getInt("BaseWarpMaxY");
		Iterator it = baseWarp.iterator();
		while(it.hasNext()) {
			String name = (String)it.next();
			int xLoc = rand.nextInt((maxX-minX)) + minX;
			int yLoc = rand.nextInt((maxY-minY)) + minY;
			m_botAction.warpTo(name, xLoc, yLoc);
		}
	}
	
	
	public void handleHelp(String name, boolean smod) {
		if(smod) {
			m_botAction.sendPrivateMessage(name, "!levion                -Enables levis next round");
			m_botAction.sendPrivateMessage(name, "!levioff               -Disables levis next round");
			m_botAction.sendPrivateMessage(name, "!privon                -Enables private frequencies");
			m_botAction.sendPrivateMessage(name, "!privoff               -Disables private frequencies");
			m_botAction.sendPrivateMessage(name, "!secinround <secs>     -Sets seconds to win");
			m_botAction.sendPrivateMessage(name, "!secbetween <secs>     -Sets seconds between rounds");
		}
			m_botAction.sendPrivateMessage(name, "!fstats <#>            -Returns stats for Freq <#>");
			m_botAction.sendPrivateMessage(name, "!stats                 -Returns your stats");
			m_botAction.sendPrivateMessage(name, "!stats <name>          -Returns <name>'s stats");
			m_botAction.sendPrivateMessage(name, "!warp                  -Toggles basewarp");
			m_botAction.sendPrivateMessage(name, "!help                  -Sends this");
	}
}

class FrequencyStats {
	
	long holdTime = 0;
	long startHold = 0;
	boolean timing = true;
	public FrequencyStats() {
	}
	
	public void startHold() {
		startHold = System.currentTimeMillis();
		timing = true;
	}
	
	public void endHold() {
		holdTime += (System.currentTimeMillis() - startHold);
		timing = false;
	}
	
	public int getHoldSeconds() {
		if(timing) {
			endHold();
			startHold();
		}
		return (int)(holdTime / 1000);
	}
}

class PlayerStats {
	int kills;
	int deaths;
	int flagClaims;
	int points;
	int lastPoints;
	int currentFreq = -1;
	HashMap<Integer,FreqTime> timeOnFreq = new HashMap<Integer,FreqTime>();
	
	public PlayerStats(int startP, int currentF) {
		lastPoints = startP;
		kills = 0;
		deaths = 0;
		flagClaims = 0;
		points = 0;
		changeFreq(currentF);
	}
	
	public void addKill() {
		kills++;
	}
	
	public void addDeath() {
		deaths++;
	}
	
	public void addFlagClaim() {
		flagClaims++;
	}
	
	public void addPoints(int ps) {
		points += (ps - lastPoints);
		lastPoints = points;
	}
	
	public int getRating() {
		int rating = (int)(((points / (deaths+1)) * (kills + flagClaims)) * getPercentOnFreq());
		return rating;
	}
	
	public int[] getStats() {
		int array[] = { kills, deaths, flagClaims, points };
		return array;
	}
	
	public double getPercentOnFreq() {
		int totalTime = 0;
		int freqTime = timeOnFreq.get(currentFreq).getTime();
		Iterator it = timeOnFreq.values().iterator();
		while(it.hasNext()) {
			FreqTime ft = (FreqTime)it.next();
			totalTime += ft.getTime();
		}
		double percent = ((double)freqTime/totalTime);
		return percent;
	}
	
	public void changeFreq(int newFreq) {
		try {
			timeOnFreq.get(currentFreq).endTime();
		} catch(Exception e) {}
		
		if(timeOnFreq.get(newFreq) != null) {
			timeOnFreq.get(newFreq).startTime();
		} else {
			timeOnFreq.put(newFreq, new FreqTime());
		}
		currentFreq = newFreq;
	}
	
	public void stopTimeLog() {
		try {
			timeOnFreq.get(currentFreq).endTime();
		} catch(Exception e) {}
		currentFreq = -1;
	}
	
	private class FreqTime {
		int totalTime;
		long startTime;
		boolean timing = true;
		public FreqTime() {
			startTime = System.currentTimeMillis();
			totalTime = 0;
		}
		
		public void endTime() {
			totalTime += ((System.currentTimeMillis() - startTime) / 1000);
			timing = false;
		}
		
		public void startTime() {
			startTime = System.currentTimeMillis();
			timing = true;
		}
		
		public int getTime() {
			if(timing){
				endTime();
				startTime();
			}
			return totalTime;
		}
	}
}