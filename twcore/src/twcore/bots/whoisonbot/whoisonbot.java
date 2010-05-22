package twcore.bots.whoisonbot;

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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaList;
import twcore.core.events.KotHReset;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.game.Player;

/**
 * WhoisOnBot
 * 
 * Go from arena to arena to get the current population.
 * 
 * Author: Arobas+
 */
public class whoisonbot extends SubspaceBot {

	private final static String VERSION = "0.1";
	
	private final static Pattern LOCATE_PATTERN = Pattern.compile("(.+)\\s-\\s([#\\w\\d\\p{Z}]+)");

	private static final int ROAMING_INTERVAL = 60 * 1000;
	private static final int FAST_ROAMING_INTERVAL = 10 * 1000;
	
	private static final int REMOVE_PLAYER_INTERVAL = 7 * 60 * 1000;
	private static final int IDLE_SECOND_TIME = 10 * 60;
	private static final int ENTER_DELAY = 5000;

	private static final int ARENA_SIZE_HIGH_PRIORITY = 20;
	private static final int ARENA_SIZE_LOW_PRIORITY = 2;

	private static final int ARENA_HIGH_PRIORITY_WEIGHT = 3;
	private static final int ARENA_NORMAL_PRIORITY_WEIGHT = 2;
	private static final int ARENA_LOW_PRIORITY_WEIGHT = 1;

	private OperatorList opList;
	private String currentArena;
	private RoamTask roamTask;
	private RemovePlayersTask removePlayersTask;
	private HashSet<String> accessList;
	private String target;

	private final String IPCCHANNEL = "whoisonline";

	private Map<String, PlayerInfo> players;
	private HashMap<String, Integer> arenas; // Arena + Weight

	public whoisonbot(BotAction botAction) {

		super(botAction);

		requestEvents();

		players = Collections.synchronizedMap(new HashMap<String, PlayerInfo>());
		arenas = new HashMap<String, Integer>();

		accessList = new HashSet<String>();
		roamTask = new RoamTask();
		//m_botAction.ipcSubscribe(IPCCHANNEL);
		
		m_botAction.scheduleTaskAtFixedRate(new RemovePlayersTask(), REMOVE_PLAYER_INTERVAL, REMOVE_PLAYER_INTERVAL);
		
	}

	public void handleEvent(LoggedOn event) {

		BotSettings botSettings = m_botAction.getBotSettings();

		opList = m_botAction.getOperatorList();

		m_botAction.sendUnfilteredPublicMessage("?chat="
				+ botSettings.getString("chat"));
		changeArena(botSettings.getString("initialarena"));
		setupAccessList(botSettings.getString("accesslist"));

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

		String message = event.getMessage();
		int messageType = event.getMessageType();

		if (messageType == Message.ARENA_MESSAGE)
			handleArenaMessage(message);
		else if (messageType == Message.REMOTE_PRIVATE_MESSAGE
				|| messageType == Message.PRIVATE_MESSAGE)
			handleCommand(event);
	}

	private void handleCommand(Message event) {

		String command = event.getMessage().toLowerCase();

		if (command.startsWith("!get")) {
			
			String name = command.substring(5);
			
			if (players.containsKey(name.toLowerCase())) {
				int lastUpdate = (int)(System.currentTimeMillis() - players.get(name.toLowerCase()).lastUpdate)/1000;
				m_botAction.sendPrivateMessage(event.getPlayerID(), "Updated " + lastUpdate + " seconds ago.");
			}
			else {
				m_botAction.sendPrivateMessage(event.getPlayerID(), name + " does not exists.");
			}
		}
		else if(command.equals("!die")) {
			handleDisconnect();
		} 
		else if(command.equals("!version")) {
			m_botAction.sendPrivateMessage(event.getPlayerID(), VERSION);
		}

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
					System.out.println("Locate received: " + playerName);
					players.get(playerName).seen();
				}

			}

		}

	}

	private void checkPlayer(String message, String playerName) {

		int idleTime = getIdleTime(message);

		if (idleTime > IDLE_SECOND_TIME) {

			players.get(playerName.toLowerCase()).setIdle(idleTime);

		}
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
		scheduleRoamTask(ROAMING_INTERVAL);
	}

	/**
	 * This method update the idling information of a player
	 */
	private void checkPlayers() {

		Iterator<Player> iterator = m_botAction.getPlayerIterator();

		while (iterator.hasNext()) {

			Player player = iterator.next();
			String playerName = m_botAction.getPlayerName(player.getPlayerID());


			if (playerName == null)
				continue;
			
			PlayerInfo playerInfo;

			if (players.containsKey(playerName.toLowerCase())) {

				playerInfo = players.get(playerName.toLowerCase());
				playerInfo.seen();
				playerInfo.setArena(m_botAction.getArenaName());
				
			} else {
				
				System.out.println("New player: " + playerName);

				playerInfo = new PlayerInfo(playerName, player.getPlayerID(),
						player.getSquadName(), m_botAction.getArenaName());
				players.put(playerName.toLowerCase(), playerInfo);
				m_botAction.sendUnfilteredPrivateMessage(playerName, "*info");
				m_botAction.sendUnfilteredPrivateMessage(playerName, "*einfo");
			}
		}
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
			for (String arena : arenas.keySet()) {
				if (!event.getArenaList().containsKey(arena)) {
					System.out.println("Removing arena : " + arena);
					arenas.remove(arena);
				}
			}
			
			synchronized (arenas) {
				
				Iterator<Entry<String,Integer>> it = arenas.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String,Integer> entry = it.next();
					if (!event.getArenaList().containsKey(entry.getKey())) {
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

			// Now get the heaviest and change the arena with this one
			/*
			LinkedHashMap<String, Integer> arenaWeighted = sortValue(arenas);
			String[] weighted = arenaWeighted.keySet().toArray(
					new String[arenaWeighted.size()]);
					*/
			
			String highest = getHighest(arenas);
			
			System.out.println("Roaming to: " + highest);
			
			changeArena(highest);
			
			if (event.getArenaList().get(highest) <= ARENA_SIZE_LOW_PRIORITY) {
				scheduleRoamTask(FAST_ROAMING_INTERVAL);
			}
			else {
				scheduleRoamTask(ROAMING_INTERVAL);
			}
			
			
			// Reset the heaviest
			arenas.put(highest, 0);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Handles restarting of the KOTH game
	 * 
	 * @param event
	 *            is the event to handle.
	 */
	public void handleEvent(KotHReset event) {
		if (event.isEnabled() && event.getPlayerID() == -1) {
			// Make the bot ignore the KOTH game (send that he's out immediately
			// after restarting the game)
			m_botAction.endKOTH();
		}
	}

	/**
	 * This method requests the events that the bot will use.
	 */

	private void requestEvents() {

		EventRequester eventRequester = m_botAction.getEventRequester();
		eventRequester.request(EventRequester.MESSAGE);
		eventRequester.request(EventRequester.ARENA_LIST);
		eventRequester.request(EventRequester.PLAYER_ENTERED);
		eventRequester.request(EventRequester.KOTH_RESET);
	}

	/**
	 * This method schedules a new roam task
	 */
	private void scheduleRoamTask(int roamTime) {

		m_botAction.cancelTask(roamTask);
		roamTask = new RoamTask();
		m_botAction.scheduleTask(roamTask, roamTime);
	}
	
	/**
	 * This method schedules a new remove player task
	 */
	private void scheduleRemovePlayersTask() {

		int interval = REMOVE_PLAYER_INTERVAL;
		m_botAction.cancelTask(removePlayersTask);
		removePlayersTask = new RemovePlayersTask();
		m_botAction.scheduleTask(removePlayersTask, interval);
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
						System.out.println("Removing player: " + player.getPlayerName());
						it.remove();
					} else if (IDLE_SECOND_TIME < (System.currentTimeMillis() - player.lastSeen)) {
						m_botAction.locatePlayer(player.getPlayerName());
						System.out.println("Locating: " + player.getPlayerName());
						player.updateLocate();
					}
				}
			}
		
		} catch(Exception e) { e.printStackTrace(); }

	}

	private class RoamTask extends TimerTask {
		public void run() {
			System.out.println("Roaming..");
			m_botAction.requestArenaList();
			scheduleRoamTask(ROAMING_INTERVAL);
		}
	}

	private class CheckPlayersTask extends TimerTask {
		public void run() {
			System.out.println("Checking players..");
			checkPlayers();
		}
	}

	private class RemovePlayersTask extends TimerTask {
		public void run() {
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

	private class PlayerInfo {

		private String playerName;
		private short playerID;
		private String playerSquad;
		private String arenaLastSeen;
		private long lastUpdate;
		private long lastLocate;
		private long lastSeen;
		private boolean idle;
		private int idleTime;

		public PlayerInfo(String playerName, short playerID,
				String playerSquad, String arena) {

			this.playerName = playerName;
			this.playerID = playerID;
			this.playerSquad = playerSquad;
			this.lastLocate = System.currentTimeMillis();
			this.lastSeen = System.currentTimeMillis();
			this.lastUpdate = System.currentTimeMillis();
			this.idle = false;
			this.idleTime = 0;
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

		public boolean isIdle() {
			return idle;
		}

		public String getArenaLastSeen() {
			return arenaLastSeen;
		}

		public void seen() {
			this.lastSeen = System.currentTimeMillis();
			update();
		}

		public void update() {
			this.lastUpdate = System.currentTimeMillis();
		}

		public String getPlayerName() {
			return playerName;
		}

		public void setIdle(boolean b) {
			this.idle = b;
			update();
		}

		public void setIdle(int idleTime) {
			this.idle = true;
			this.idleTime = idleTime;
			update();
		}
	}
	
}
