package twcore.bots.whoisonbot;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaList;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.KotHReset;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.SQLResultEvent;
import twcore.core.game.Player;
import twcore.core.util.Tools;
import twcore.core.util.ipc.IPCMessage;

/**
 * TODO
 * - Anti-spam function
 */



/**
 * WhoisOnBot
 * 
 * Go from arena to arena to get the current population.
 * 
 * @author Arobas+
 */
public class whoisonbot extends SubspaceBot {

	private final static boolean DEBUG = false;

	/* INTERVALS */
	
	private static final int ROAMING_INTERVAL = 60 * 1000; // 
	private static final int ROAMING_INTERVAL_MULTIPLICATOR = 1750; // 1.75 sec. X population of the arena
	private static final int REMOVE_PLAYER_INTERVAL = 7 * 60 * 1000; // check every 7 minutes
	private static final int REMOVE_PLAYER_LOCATE_INTERVAL = 2 * 60 * 1000; //
	private static final int LAST_SEEN_TTL = 10 * 60 * 1000;
	private static final int IDLE_CHECK_INTERVAL = 10 * 60 * 1000;
	private static final int ENTER_DELAY = 5000;
	private static final int GROUP_PLAYER_LIST_INTERVAL = 60 * 60 * 1000; // every hour
	
	/* PRIORITY QUEUING */
	
	private static final int ARENA_SIZE_HIGH_PRIORITY = 20;
	private static final int ARENA_SIZE_LOW_PRIORITY = 2;

	private static final int ARENA_HIGH_PRIORITY_WEIGHT = 3;
	private static final int ARENA_NORMAL_PRIORITY_WEIGHT = 2;
	private static final int ARENA_LOW_PRIORITY_WEIGHT = 1;

	/* MASTER-SLAVE */
	
	private static enum Status { MASTER, SLAVE };
	private static final String REQUEST_MASTER = "REQUEST_MASTER";
	private static final String MASTER = "MASTER";
	private static final String SLAVE = "SLAVE";
	private static final String PLAYER_SEEN = "SEEN:";
	private static final String ROAMING_ARENA = "ROAMARENA:";
	
	/* ... */
	
	private final static Pattern LOCATE_PATTERN = Pattern.compile("(.+)\\s-\\s([#\\w\\d\\p{Z}]+)");
	
	/* VARIABLES */
	
	private OperatorList opList;
	private String currentArena;
	private RoamTask roamTask;
	private HashSet<String> accessList;
	private String target;

	private final String SOCKETCHANNEL = "whoisonline";
	private final String IPCCHANNEL = "whoisonline";

	private Map<String, PlayerInfo> players;
	private HashMap<String, Integer> arenas; // Arena + Weight
	
	private static enum GroupCategory { PUB, TWD, TWHT, STAFF }
	private Map<String, PlayerGroup> groups;
	
	private Status status;
	private String masterName;
	
    private static final String HELP_MESSAGE [] = {
    	
        "--------- WHO'S ONLINE COMMANDS -----------------------------------",
        "!here              - tell the bot that you are connected",
        "                     in case the bot had'nt picked up your name yet",
        "!me                - check your current status on the bot",
        "!pub <squadname>   - check who's online on this squad (pub)",
        "!twd <squadname>   - check who's online on this squad (twd)",
        //"!twht <squadname>  - check who's online on this squad (twht)"
    };
    
    private static final String HELP_SMOD_MESSAGE [] = { 
    	"--------- SMOD COMMANDS--------------------------------------------",
    	"!die               - kill this bot",
    };

	public whoisonbot(BotAction botAction) {

		super(botAction);

		requestEvents();

		players = Collections.synchronizedMap(new HashMap<String, PlayerInfo>());
		groups = Collections.synchronizedMap(new HashMap<String, PlayerGroup>());
		arenas = new HashMap<String, Integer>();
		
		accessList = new HashSet<String>();
				
	}
	
	public void handleEvent(LoggedOn event) {

		BotSettings botSettings = m_botAction.getBotSettings();
		
		opList = m_botAction.getOperatorList();
		
		if (botSettings.getString("Type"+m_botAction.getBotNumber()).equals("master")) {
			status = Status.MASTER;
		} else {
			status = Status.SLAVE;
		}

		m_botAction.sendUnfilteredPublicMessage("?chat=" + botSettings.getString("chat"));
		
		setupAccessList(botSettings.getString("accesslist"));
		
		m_botAction.ipcSubscribe(IPCCHANNEL);
		//m_botAction.socketSubscribe(SOCKETCHANNEL);

		if (status.equals(Status.SLAVE)) {
			
			m_botAction.ipcSendMessage(IPCCHANNEL, REQUEST_MASTER, null, m_botAction.getBotName());
			if (DEBUG)
				System.out.println("Requesting master..");
			m_botAction.scheduleTask(new CheckRequestMasterAnswerTask(), (long)Math.random()*60000);
		}
		else {
			start();
		}

	}
	
	/**
	 * Starting only when we know if we are MASTER or SLAVE.
	 */
	public void start() {
		
	
		changeArena(m_botAction.getBotSettings().getString("initialarena"));
		scheduleRoamTask(ROAMING_INTERVAL);
		roamTask = new RoamTask();
		if (status.equals(Status.MASTER)) {
			m_botAction.scheduleTaskAtFixedRate(new RemovePlayersTask(), REMOVE_PLAYER_INTERVAL, REMOVE_PLAYER_INTERVAL);
		}
	}
	
	/*
	public void handleEvent(SocketMessageEvent event) {
		
		System.out.println("RESPONSE!");
		//event.setResponse("TES\nTdfsdf");
		
	}
	*/
	
	public void handleEvent(InterProcessEvent event) {
		
		if (event.getObject()==null)
			return;

		if (event.getSenderName().equals(m_botAction.getBotName()))
			return;
		
		if (event.getObject() instanceof Message) {
			
			handleEvent((Message)event.getObject());
			
		}

		else if (event.getObject() instanceof IPCMessage) {
		
			IPCMessage ipcMessage = (IPCMessage)event.getObject();
			String message = ipcMessage.getMessage();
			
			System.out.println("IPC received from (" + event.getSenderName() + "): " + message);
			
			if (REQUEST_MASTER.equals(message)) {
				
				if (status.equals(status.MASTER)) {
					m_botAction.ipcSendMessage(IPCCHANNEL, MASTER, event.getSenderName(), m_botAction.getBotName());
				}
			}
			else if (MASTER.equals(message)) {
				masterName = event.getSenderName();
			}
			else if (message.startsWith(PLAYER_SEEN) && status.equals(Status.MASTER)) {
				
				String[] split = message.substring(PLAYER_SEEN.length()).split(":::");
				updatePlayer(split[0], Boolean.getBoolean(split[1]));
				
			}
			else if(message.startsWith(ROAMING_ARENA) && status.equals(Status.MASTER)) {
				
				String arena = message.substring(ROAMING_ARENA.length());
				synchronized (arenas) {
					arenas.put(arena, 0);
				}
				
			}
		
		}
		
	}
	
	public void handleEvent(PlayerEntered event) {

		Player player = m_botAction.getPlayer(event.getPlayerID());
		if (players.containsKey(player.getPlayerName()))
			players.get(player.getPlayerName()).seen();

	}

	public void handleDisconnect() {

		m_botAction.cancelTasks();
		m_botAction.die();
	}

	public void handleEvent(Message event) {

		if (status.equals(Status.SLAVE)) {
			m_botAction.ipcTransmit(IPCCHANNEL, event);
			return;
		}
		
		String message = event.getMessage();
		int messageType = event.getMessageType();
		
		String sender = m_botAction.getPlayerName(event.getPlayerID());
		String command = event.getMessage();

		if (messageType == Message.ARENA_MESSAGE) {
			handleArenaMessage(message);
		}
		else if (messageType == Message.REMOTE_PRIVATE_MESSAGE || messageType == Message.PRIVATE_MESSAGE) {
			if (event.getMessage().startsWith("!"))
				handlePrivateCommand(command, sender);
		}
		else if (messageType == Message.CHAT_MESSAGE) {
			if (event.getMessage().startsWith("!"))
				handleChatCommand(command, sender);
		}
	}

	private void handleChatCommand(String command, String sender) {

		

	}

	private void handlePrivateCommand(String command, String sender) {

		if(command.startsWith("!help")) {

			m_botAction.remotePrivateMessageSpam(sender, HELP_MESSAGE);
			if(opList.isSmod(sender) || accessList.contains(sender.toLowerCase())) {
				 m_botAction.remotePrivateMessageSpam(sender, HELP_SMOD_MESSAGE);
			} 
			if (status.equals(Status.MASTER))
				m_botAction.sendRemotePrivateMessage(sender, "------------------------------------------------------------MASTER-");
			else	
				m_botAction.sendRemotePrivateMessage(sender, "-------------------------------------------------------------SLAVE-");

		}
		else if(command.equals("!die") && (opList.isSmod(sender) || accessList.contains(sender.toLowerCase()))) {
			handleDisconnect();
		} 
		else if(command.equals("!here")) {
			
			if (!players.containsKey(sender.toLowerCase())) {
				PlayerInfo player = new PlayerInfo(sender, "Unknown");
				players.put(sender.toLowerCase(), player);
				m_botAction.sendRemotePrivateMessage(sender, "You have been added.");

			}
			else {
				m_botAction.sendRemotePrivateMessage(sender, "You are already monitored.");
				
			}
		}
		else if(command.equals("!me")) {
			
			if (players.containsKey(sender.toLowerCase())) {
				lookupPlayer(sender);
			}
			else {
				m_botAction.sendRemotePrivateMessage(sender, "You are not monitored yet.");

			}
		}
		else if(command.startsWith("!twd")) {

			if (command.length() > 5) {
				
				String squadname = command.substring(5).trim();
				String identifier = "twd_"+squadname;

				if (groups.containsKey(identifier) && !groups.get(identifier).needUpdate()) {
					lookupGroup(identifier, sender);	
				}
				else {
					m_botAction.sendRemotePrivateMessage(sender, "Please wait while retrieving the player list..");

					String query = "SELECT fcUserName AS name " +
						"FROM tblTeam t " +
						"JOIN tblTeamUser tu ON t.fnTeamID = tu.fnTeamID " +
						"JOIN tblUser u ON u.fnUserID = tu.fnUserID " +
						"WHERE t.fcTeamName = '" + squadname + "' " +
						"AND tu.fnCurrentTeam = 1";
					
					m_botAction.SQLBackgroundQuery("website", "updategroup:::"+identifier+":::"+sender, query);
				}
			}
			else {
				
			}

		}
		else if(command.startsWith("!pub")) {

			if (command.length() > 5) {
				
				String squadname = command.substring(5).trim();
				String identifier = "pub_"+squadname;

				if (groups.containsKey(identifier) && !groups.get(identifier).needUpdate()) {
					lookupGroup(identifier, sender);	
				}
				else {
					m_botAction.sendRemotePrivateMessage(sender, "Please wait while retrieving the player list..");

					String query = "SELECT fcName AS name " +
						"FROM tblPlayer " +
						"WHERE fcSquad = '" + squadname + "'";
											
					m_botAction.SQLBackgroundQuery("pubstats", "updategroup:::"+identifier+":::"+sender, query);
				}
			}
			else {
				
			}

		}

	}
	
	private void lookupGroup(String identifier, String sender) {
		
		ArrayList<String> lines = new ArrayList<String>();
		
		if (groups.containsKey(identifier)) {
			PlayerGroup group = groups.get(identifier);
			
			StringBuffer buffer = new StringBuffer();
			buffer.append(Tools.formatString("Player Name", 24));
			buffer.append(Tools.formatString("Status", 20));
			buffer.append(Tools.formatString("Arena", 20));
			buffer.append(Tools.formatString("Last Updated", 18));
			lines.add(buffer.toString());
			
			int playing = 0;
			int inspec = 0;
			int idle = 0;
			int offline = 0;
			
			for(String playerName: group.getPlayersName()) {
				
				if (players.containsKey(playerName.toLowerCase())) {
					PlayerInfo player = players.get(playerName.toLowerCase());
					buffer = new StringBuffer();
					buffer.append(Tools.formatString(player.getPlayerName(), 24));
					String arena = player.getArenaLastSeen();
					if (arena.startsWith("("))
						arena = arena.substring(1,arena.length()-1);
					if (player.getIdleTime()==0) {
						if (player.isPlaying()) {
							buffer.append(Tools.formatString("Playing", 20));
							playing++;
						} else {
							buffer.append(Tools.formatString("In spec", 20));
							inspec++;
						}
					} else {
						buffer.append(Tools.formatString("Idle ("+player.getIdleTime()+" sec)", 20));
						idle++;
					}
					buffer.append(Tools.formatString(arena, 20));
					buffer.append(Tools.formatString(player.getLastUpdate() + " sec. ago", 18));
					lines.add(buffer.toString());
				}
				else {
					offline++;
				}
			}
			
			lines.add(" ");
			lines.add("ONLINE: "+(playing+inspec+idle)+"    OFFLINE: "+offline);
			
			m_botAction.remotePrivateMessageSpam(sender, lines.toArray(new String[lines.size()]));
			
		
		}
		else {
			System.out.println("No group found for identifier: " + identifier);
		}

	}
	
	private void lookupPlayer(String playerName) {
		
		ArrayList<String> lines = new ArrayList<String>();

		if (players.containsKey(playerName.toLowerCase())) {
			
			StringBuffer buffer = new StringBuffer();
			buffer.append(Tools.formatString("Player Name", 24));
			buffer.append(Tools.formatString("Status", 20));
			buffer.append(Tools.formatString("Arena", 20));
			buffer.append(Tools.formatString("Last Updated", 18));
			lines.add(buffer.toString());
			
			PlayerInfo player = players.get(playerName.toLowerCase());
			buffer = new StringBuffer();
			buffer.append(Tools.formatString(player.getPlayerName(), 24));
			String arena = player.getArenaLastSeen();
			if (arena.startsWith("("))
				arena = arena.substring(1,arena.length()-1);
			if (player.getIdleTime()==0) {
				if (player.isPlaying()) {
					buffer.append(Tools.formatString("Playing", 20));
				} else {
					buffer.append(Tools.formatString("In spec", 20));
				}
			} else {
				buffer.append(Tools.formatString("Idle ("+player.getIdleTime()+" sec)", 20));
			}
			buffer.append(Tools.formatString(arena, 20));
			buffer.append(Tools.formatString(player.getLastUpdate() + " sec. ago", 18));
			lines.add(buffer.toString());
		}
			
		m_botAction.remotePrivateMessageSpam(playerName, lines.toArray(new String[lines.size()]));

	}

	private void handleArenaMessage(String message) {

		if (message.startsWith("IP:")) {
			updateTarget(message);
		} else if (message.startsWith(target + ": " + "UserId: ")) {
			checkPlayer(message, target);
		} else {

			Matcher m = LOCATE_PATTERN.matcher(message);

			if (m.matches()) {

				String playerName = m.group(1).toLowerCase();
				String arenaName = m.group(2);

				if (players.containsKey(playerName)) {
					if (DEBUG)
						System.out.println("Locate received: " + playerName);
					players.get(playerName).seen();
				}

			}

		}

	}

	private void checkPlayer(String message, String playerName) {

		int idleTime = getIdleTime(message);
		try {
			players.get(playerName.toLowerCase()).setIdle(idleTime);
		} catch(Exception e) { }
	}

	/**
	 * This method updates the target based on the *info message that is
	 * received.
	 * 
	 * @param message
	 *            is the info message that was received.
	 */

	private void updateTarget(String message) {

		int beginIndex = message.indexOf("TypedName:") + 10;
		int endIndex = message.indexOf("  ", beginIndex);
		target = message.substring(beginIndex, endIndex);
	}

	/**
	 * This method gets the idle time of a player from an *einfo message.
	 * 
	 * @param message
	 *            is the einfo message to parse.
	 * @return the idle time in seconds is returned.
	 */
	private int getIdleTime(String message) {

		int beginIndex = message.indexOf("Idle: ") + 6;
		int endIndex = message.indexOf(" s", beginIndex);

		if (beginIndex == -1 || endIndex == -1)
			throw new RuntimeException("Cannot get idle time.");
		return Integer.parseInt(message.substring(beginIndex, endIndex));
	}

	/**
	 * This method changes the arena and updates the currentArena. It also
	 * schedules a new roam task and kills all of the idlers.
	 */
	private void changeArena(String arenaName) {

		int beginIndex = 0;
		int endIndex;
		
		beginIndex = arenaName.indexOf("(Public ");
		endIndex = arenaName.indexOf(")");

		if (beginIndex != -1 && endIndex != -1 && beginIndex <= endIndex)
			arenaName = arenaName.substring(beginIndex, endIndex);

		m_botAction.changeArena(arenaName);
		currentArena = arenaName;
		m_botAction.scheduleTask(new CheckPlayersTask(), ENTER_DELAY);
	}
	
	/**
	 * This method update the idling information of a player
	 */
	private void checkRequestMasterAnswer() {

		if (masterName != null) {
			if (DEBUG)
				System.out.println(masterName + " is my new master.");
			start();
		}
	}

	/**
	 * This method update the idling information of a player
	 */
	private void checkPlayers() {

		Iterator<Player> iterator = m_botAction.getPlayerIterator();

		ArrayList<String> players = new ArrayList<String>();
		
		while (iterator.hasNext()) {

			Player player = iterator.next();
			String playerName = m_botAction.getPlayerName(player.getPlayerID());

			if (playerName == null)
				continue;
			
			if (status.equals(Status.MASTER)) {
				updatePlayer(playerName, player.isPlaying());
			}
			else {
				String message = PLAYER_SEEN+playerName+":::"+(player.isPlaying()?1:0);
				m_botAction.ipcSendMessage(IPCCHANNEL, message, masterName, m_botAction.getBotName());
			}
			
		}

	}
	
	public void updatePlayer(String playerName, boolean isPlaying) {
		
		PlayerInfo playerInfo;

		if (players.containsKey(playerName.toLowerCase())) {

			playerInfo = players.get(playerName.toLowerCase());
			playerInfo.seen();
			playerInfo.setArena(m_botAction.getArenaName());
			playerInfo.setIsPlaying(isPlaying);
			if (IDLE_CHECK_INTERVAL < playerInfo.getLastIdleCheck()) {
				sendIdleTimeInfo(playerName);
			}
			
		} else {
			
			if (DEBUG)
				System.out.println("New player: " + playerName);

			playerInfo = new PlayerInfo(playerName, m_botAction.getArenaName());
			playerInfo.setIsPlaying(isPlaying);
			players.put(playerName.toLowerCase(), playerInfo);
			sendIdleTimeInfo(playerName);
		}
	}
	
	public void sendIdleTimeInfo(String playerName) {
		if (DEBUG)
			System.out.println("Sending *info/*einfo to " + playerName);
		m_botAction.sendUnfilteredPrivateMessage(playerName, "*info");
		m_botAction.sendUnfilteredPrivateMessage(playerName, "*einfo");
	}

	/**
	 * This method handles the ArenaList event and takes the bot to an arena.
	 * It used a priority queue algorithm to get an arena using a weight.
	 * The weight will grow until the algorithm choose it.
	 * Because we don't want to waste our time to check arena with 1-2 players on it.
	 */
	public void handleEvent(ArenaList event) {

		try {
			
			// Remove non-existing arena
			synchronized (arenas) {
				
				Iterator<Entry<String,Integer>> it = arenas.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String,Integer> entry = it.next();
					if (!event.getArenaList().containsKey(entry.getKey())) {
						if (DEBUG)
							System.out.println("Removing arena : " + entry.getKey());
						it.remove();
					}
				}
			}

			int weight = 0;

			// Increment the weight of an arena
			for (Entry<String, Integer> entry : event.getArenaList().entrySet()) {

				if (entry.getValue() >= ARENA_SIZE_HIGH_PRIORITY) {
					weight = ARENA_HIGH_PRIORITY_WEIGHT;
				} else if (entry.getValue() > ARENA_SIZE_LOW_PRIORITY) {
					weight = ARENA_NORMAL_PRIORITY_WEIGHT;
				} else {
					weight = ARENA_LOW_PRIORITY_WEIGHT;
				}
				
				int currentWeight = 0;
				if (arenas.containsKey(entry.getKey()))
						currentWeight =  arenas.get(entry.getKey());

				arenas.put(entry.getKey(), currentWeight + weight);
			}

			String highest = getHighest(arenas);
			
			if (DEBUG)
				System.out.println("Roaming to: " + highest);
			
			changeArena(highest);
			
			int customRoamingInterval = ENTER_DELAY+(ROAMING_INTERVAL_MULTIPLICATOR*event.getArenaList().get(highest));

			scheduleRoamTask(customRoamingInterval);
			
			m_botAction.ipcSendMessage(IPCCHANNEL, ROAMING_ARENA+highest, null, m_botAction.getBotName());
			
			// Reset the heaviest
			synchronized (arenas) {
				arenas.put(highest, 0);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Handles restarting of the KOTH game */
	public void handleEvent(KotHReset event) {
		if (event.isEnabled() && event.getPlayerID() == -1) {
			// Make the bot ignore the KOTH game (send that he's out immediately
			// after restarting the game)
			m_botAction.endKOTH();
		}
	}
	
	public void handleEvent(SQLResultEvent event) {
		
		String command = event.getIdentifier();

		String[] split = command.split(":::");
		if (split.length == 3) {
			
			String type = split[0];
			String identifier = split[1];
			String[] idSplit = identifier.split("_");
			String sender = split[2];
			
			ResultSet set = event.getResultSet();
			try {
				
				TreeSet<String> playersName = new TreeSet<String>();
				while(set.next()) {
					String name = set.getString("name");
					playersName.add(name);
				}
				if (!groups.containsKey(identifier)) {
					PlayerGroup group = new PlayerGroup(GroupCategory.valueOf(idSplit[0].toUpperCase()), idSplit[1]);
					group.updatePlayersList(playersName);
					groups.put(identifier, group);
				}
				else {
					groups.get(identifier).updatePlayersList(playersName);
				}

				lookupGroup(identifier, sender);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

	/**
	 * This method requests the events that the bot will use. */
	private void requestEvents() {

		EventRequester eventRequester = m_botAction.getEventRequester();
		eventRequester.request(EventRequester.MESSAGE);
		eventRequester.request(EventRequester.ARENA_LIST);
		eventRequester.request(EventRequester.PLAYER_ENTERED);
		eventRequester.request(EventRequester.KOTH_RESET);
	}

	/**
	 * This method schedules a new roam task */
	private void scheduleRoamTask(int roamTime) {

		m_botAction.cancelTask(roamTask);
		roamTask = new RoamTask();
		m_botAction.scheduleTask(roamTask, roamTime);
	}

	private void setupAccessList(String accessString) {

		StringTokenizer accessTokens = new StringTokenizer(accessString, ":");
		accessList.clear();

		while (accessTokens.hasMoreTokens()) {
			accessList.add(accessTokens.nextToken().toLowerCase());
		}
	}

	private void removePlayers() {

		Collection<PlayerInfo> playersInfo = players.values();

		try {
		
			synchronized (players) {
	
				Iterator<PlayerInfo> it = playersInfo.iterator();
				while (it.hasNext()) {
	
					PlayerInfo player = it.next();
	
					if (player.lastLocate > player.lastSeen) {
						if (DEBUG)
							System.out.println("Removing player: " + player.getPlayerName());
						it.remove();
					} else if (LAST_SEEN_TTL < (System.currentTimeMillis() - player.lastSeen)) {
						System.out.println((System.currentTimeMillis() - player.lastSeen));
						m_botAction.locatePlayer(player.getPlayerName());
						if (DEBUG)
							System.out.println("Locating: " + player.getPlayerName());
						player.updateLocate();
					}
				}
			}
		
		} catch(Exception e) { e.printStackTrace(); }

	}

	private class RoamTask extends TimerTask {
		public void run() {
			if (DEBUG)
				System.out.println("Roaming..");
			m_botAction.requestArenaList();
			scheduleRoamTask(ROAMING_INTERVAL);
		}
	}

	private class CheckRequestMasterAnswerTask extends TimerTask {
		public void run() {
			checkRequestMasterAnswer();
		}
	}
	
	private class CheckPlayersTask extends TimerTask {
		public void run() {
			if (DEBUG)
				System.out.println("Checking players..");
			checkPlayers();
		}
	}

	private class RemovePlayersTask extends TimerTask {
		public void run() {
			if (DEBUG)
				System.out.println("Removing players..");
			removePlayers();
		}
	}
	
	private String getHighest(HashMap<String, Integer> arenas) {
		
		int weight = 0;
		String arena = "";
		
		for (Entry<String, Integer> entry :arenas.entrySet()) {
			if (entry.getValue() > weight) {
				weight = entry.getValue();
				arena = entry.getKey();
			}
		}
		return arena;		
	}

	public LinkedHashMap<String, Integer> sortValue(HashMap<String, Integer> map) {

		List<String> mapKeys = new ArrayList<String>(map.keySet());
		List<Integer> mapValues = new ArrayList<Integer>(map.values());
		Collections.sort(mapValues);
		Collections.sort(mapKeys);

		LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();

		Iterator<Integer> valueIt = mapValues.iterator();
		while (valueIt.hasNext()) {
			Object val = valueIt.next();
			Iterator<String> keyIt = mapKeys.iterator();

			while (keyIt.hasNext()) {
				Object key = keyIt.next();
				String comp1 = map.get(key).toString();
				String comp2 = val.toString();

				if (comp1.equals(comp2)) {
					map.remove(key);
					mapKeys.remove(key);
					sortedMap.put((String) key, (Integer) val);
					break;
				}

			}

		}
		return sortedMap;
	}
	
	private class PlayerGroup {
		
		private String name; //ie: squad name
		private GroupCategory category;
		
		// All players of this group (populated from an SQL query or something else)
		private TreeSet<String> playerNames; 
		
		// Last update of the list
		// We don't want to update this list too often for performance reasons
		private long lastUpdate;
		
		public PlayerGroup(GroupCategory category, String name) {
			
			this.category = category;
			this.name = name;
		}
		
		public boolean needUpdate() {
			return GROUP_PLAYER_LIST_INTERVAL < (System.currentTimeMillis()-lastUpdate);
		}
		
		public void updatePlayersList(TreeSet<String> playersName) {
			
			this.playerNames = playersName;
			this.lastUpdate = System.currentTimeMillis();
		}
		
		public Set<String> getPlayersName() {
			return playerNames;
		}
		
		public int getLongestName() {
			int dim = 0;
			for(String p: playerNames) {
				if (p.length() > dim)
					dim = p.length();
			}
			return dim;
		}
	}

	/**
	 * This class contains inf
	 */
	private class PlayerInfo {

		private String playerName;
		private String arenaLastSeen;
		private long lastUpdate;
		private long lastIdleCheck;
		private long lastLocate;
		private long lastSeen;
		private int idleTime;
		private boolean isPlaying;

		public PlayerInfo(String playerName, String arena) {

			this.playerName = playerName;
			this.lastLocate = System.currentTimeMillis();
			this.lastSeen = System.currentTimeMillis();
			this.lastUpdate = System.currentTimeMillis();
			this.idleTime = 0;
			this.isPlaying = true;
			this.arenaLastSeen = arena;
		}

		public void updateLocate() {
			this.lastLocate = System.currentTimeMillis();
			update();
		}

		public void setArena(String arena) {
			this.arenaLastSeen = arena;
			update();
		}
		
		public void setIsPlaying(boolean b) {
			this.isPlaying = b;
		}

		public String getArenaLastSeen() {
			if (arenaLastSeen.startsWith("#"))
				return "((PRIVATE))";
			else
				return arenaLastSeen;
		}

		public void seen() {
			this.lastSeen = System.currentTimeMillis();
			update();
		}

		public void update() {
			this.lastUpdate = System.currentTimeMillis();
		}

		public void setIdle(int idleTime) {
			this.idleTime = idleTime;
			this.lastIdleCheck = System.currentTimeMillis();
			update();
		}
		
		public String getPlayerName() {
			return playerName;
		}
				
		public boolean isPlaying() {
			return isPlaying;
		}
		
		/**
		 * @return idle time (in second) since the last check */
		public int getIdleTime() {
			return idleTime;
		}
		
		/**
		 * @return last idle info update (in second) since the last check */
		public int getLastIdleCheck() {
			return (int)(System.currentTimeMillis()-lastIdleCheck)/1000;
		}
		
		/**
		 * @return last player info update (in second) since the last update */
		public int getLastUpdate() {
			return (int)(System.currentTimeMillis()-lastUpdate)/1000;
		}
	}
	
}
