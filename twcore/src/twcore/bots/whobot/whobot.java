package twcore.bots.whobot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.*;
import twcore.core.game.Player;
import twcore.core.util.Tools;
import twcore.core.util.ipc.IPCEvent;
import twcore.core.util.ipc.IPCMessage;

/**
 * This is a roaming bot that works with TWChat to keep
 * an accurate account of all online players. Its job is 
 * to patrol the private and low population arenas and maintain
 * lists of players found.
 * 
 * @author WingZero
 */
public class whobot extends SubspaceBot {
    
    private static final String IPC = "whoonline";
    private static final long CHECK_TIME = 1 * Tools.TimeInMillis.MINUTE; // ms
    private static final long IDLE_TIME = 4 * Tools.TimeInMillis.MINUTE; // ms
    private static final long LOCATE_WAIT = 3 * Tools.TimeInMillis.SECOND; // ms
    private static final long GO_TIME = 20 * Tools.TimeInMillis.SECOND;
    private static final long GO_DELAY = 60 * Tools.TimeInMillis.SECOND;
    private static final String PUBBOT = "TW-Guard";
    private static final String WHOHUB = "TWChat";
    private static final String HOME = "#robopark";
    private static final int MAX_SPAM = 5;
    
    public BotAction ba;
    public OperatorList ops;
    
    public HashSet<String> nogo;
    
    private HashMap<String, Long> online;
    
    // names waiting to be checked if online
    private Vector<String> locateQueue;
    
    private Vector<String> arenaQueue;
    
    private int arenas;
    private String locating;
    // true if roaming arenas
    private boolean roaming;
    // true if the bot is starting up
    private boolean start;
    // true if the bot is stopped
    private boolean stop;
    private boolean dcWait;
    private TimerTask wait;
    // checks for *locate to return or next 
    private TimerTask locate;
    private TimerTask go;
    
    private boolean DEBUG;
    private String debugger;
    
    
    public whobot(BotAction botAction) {
        super(botAction);
        ba = botAction;
        ops = ba.getOperatorList();
        online = new HashMap<String, Long>();
        nogo = new HashSet<String>();
        arenaQueue = new Vector<String>();
        locateQueue = new Vector<String>();
        
        arenas = 0;
        start = true;
        stop = false;
        roaming = false;
        dcWait = false;
        locating = "";
        debugger = "WingZero";
        DEBUG = true;
        String[] nogos = ba.getBotSettings().getString("NoGoArenas").split(" ");
        for (String n : nogos)
            nogo.add(n);
    }
    
    public void handleEvent(LoggedOn event) {
        ba.ipcSubscribe(IPC);
        ba.joinArena(HOME);
        EventRequester er = ba.getEventRequester();
        er.request(EventRequester.ARENA_JOINED);
        er.request(EventRequester.ARENA_LIST);
        er.request(EventRequester.MESSAGE);
        ba.sendUnfilteredPublicMessage("?chat=robodev");
    }

    public void handleEvent(ArenaJoined event) {
        if (!ba.getArenaName().equalsIgnoreCase(HOME))
            arenas++;
        processPlayers();
        if (ba.getArenaName().equalsIgnoreCase(HOME)) {
            if (start) {
                debug("Starting..");
                start = false;
                ba.requestArenaList();
            } else if (dcWait) {
                dcWait = false;
                arenas = 0;
                go = new TimerTask() {
                    public void run() {
                        if (!arenaQueue.isEmpty()) {
                            ba.changeArena(arenaQueue.remove(0));
                        } else {
                            roaming = false;
                            ba.changeArena(HOME);
                        }
                    }
                };
                ba.scheduleTask(go, GO_DELAY);
            }
        } else if (roaming) {
            debug("Roaming true, processing players..");
            go = new TimerTask() {
                public void run() {
                    String arena = HOME;
                    if (!arenaQueue.isEmpty()) {
                        if (arenas < MAX_SPAM) {
                            // go to next arena
                            arena = arenaQueue.remove(0);
                        } else
                            dcWait = true;
                    } else 
                        roaming = false;
                    ba.changeArena(arena);
                }
            };
            ba.scheduleTask(go, GO_TIME);
        } else {
            wait = new TimerTask() {
                public void run() {
                    debug("Requesting arena list.");
                    ba.requestArenaList();
                }
            };
            ba.scheduleTask(wait, IDLE_TIME);
            
            long recent = System.currentTimeMillis() - CHECK_TIME;
            for (String n : online.keySet()) {
                if (online.get(n) < recent && !locateQueue.contains(n.toLowerCase()))
                    locateQueue.add(n.toLowerCase());
            }
            locateNext();
        }

    }
    
    public void handleEvent(ArenaList event) {
        Map<String, Integer> arenas = event.getArenaList();
        for (String a : arenas.keySet()) {
            if (!a.equalsIgnoreCase(HOME) && !Tools.isAllDigits(a) && ((arenas.get(a) < 3 && !nogo.contains(a.toLowerCase()) || a.contains("#"))))
                arenaQueue.add(a);
        }
        roaming = true;
        String msg = "Arenas: ";
        for (String a : arenaQueue)
            msg += a + " ";
        go = new TimerTask() {
            public void run() {
                ba.changeArena(arenaQueue.remove(0));
            }
        };
        ba.scheduleTask(go, GO_TIME);
        debug("Arena list received and initiating roaming. " + msg);
    }
    
    public void handleEvent(Message event) {
        String msg = event.getMessage();
        
        if (event.getMessageType() == Message.ARENA_MESSAGE && !stop) {
            if (locating.length() > 0 && msg.contains(" - ")) {
                String found = msg.substring(0, msg.lastIndexOf(" - ")).toLowerCase();
                if (locateQueue.remove(found)) {
                    if (online.containsKey(found.toLowerCase()))
                        online.put(found, System.currentTimeMillis());
                }
                locateNext();
            }
        }
        
        String name = "";
        if (event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
            name = event.getMessager();
        else if (event.getMessageType() == Message.PRIVATE_MESSAGE)
            name = ba.getPlayerName(event.getPlayerID());
        else return;
        if (ops.isSmod(name) || ops.isDeveloperExact(name)) {
            if (msg.equals("!stop"))
                stop(name);
            else if (msg.equals("!start"))
                start(name);
            else if (msg.equals("!debug")) 
                debugger(name);
            else if (msg.equals("!die"))
                die(name);
            else if (msg.equals("!pro"))
                processPlayers();
            else
                ba.sendChatMessage(name + " said: " + msg);
        } else {
            ba.sendChatMessage(name + " said: " + msg);
            ba.sendSmartPrivateMessage(name, "Sorry, don't mind me! I'm just passing through. I won't tell anyone about your super secret hideout, trust me!");
        }
    }
    
    public void handleEvent(InterProcessEvent event) {
        // used to talk with TWChat
        if (!event.getChannel().equals(IPC) || stop)
            return;
        
        if (!event.getSenderName().equals(ba.getBotName()) && event.getObject() instanceof IPCEvent) {
            IPCEvent ipc = (IPCEvent) event.getObject();
            if (ipc.getType() == EventRequester.PLAYER_ENTERED || ipc.getType() == EventRequester.PLAYER_LEFT) {
                String name = ipc.getName().toLowerCase();
                online.remove(name);
                locateQueue.remove(name);
            }
        } else if (event.getObject() instanceof IPCMessage && event.getSenderName().equalsIgnoreCase(WHOHUB)) {
            IPCMessage ipc = (IPCMessage) event.getObject();
            if (ipc.getMessage().equals("who:refresh")) {
                ba.ipcTransmit(IPC, new IPCEvent(online, System.currentTimeMillis(), EventRequester.PLAYER_ENTERED));
            }
        }
    }
    
    public void handleDisconnect() {
        die("");
    }
    
    public void die(String name) {
        ba.cancelTasks();
        ba.ipcTransmit(IPC, new IPCEvent(new Vector<String>(), System.currentTimeMillis(), EventRequester.PLAYER_LEFT));
        if (name.length() > 0) {
            ba.sendSmartPrivateMessage(name, "Goodbye!");
            ba.die(name + " the douchebag killed me.");
        } else
            ba.die();
    }
    
    public void processPlayers() {
        if (stop)
            ba.changeArena(HOME);
        debug("Starting player processing in " + ba.getArenaName());
        String test = ba.getFuzzyPlayerName("" + PUBBOT);
        if (test != null && test.startsWith("" + PUBBOT) && !HOME.equalsIgnoreCase(ba.getArenaName())) {
            debug("Found pubbot in arena " + ba.getArenaName());
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Player> i = ba.getPlayerIterator();
        while (i.hasNext()) {
            String name = i.next().getPlayerName();
            if (isNotBot(name)) {
                if (online.containsKey(name.toLowerCase())) {
                    online.put(name.toLowerCase(), now);
                    locateQueue.remove(name.toLowerCase());
                } else {
                    debug("Adding " + name + " as ONLINE");
                    online.put(name.toLowerCase(), now);
                    // send new found player to TWChat
                    ba.ipcTransmit(IPC, new IPCEvent(name, now, EventRequester.PLAYER_ENTERED));
                }
            }
        }
    }
    
    private void locateNext() {
        if (locateQueue.isEmpty() || stop) {
            debug("Locate queue is empty or forced to stop at locateNext.");
            locating = "";
            return;
        }
        final String name = locateQueue.get(0);
        final long time = System.currentTimeMillis();
        locating = name;
        debug("Attempting to locate " + name);
        locate = new TimerTask() {
            public void run() {
                if (stop) return;
                debug("Locate did not return " + name);
                locateQueue.remove(name.toLowerCase());
                online.remove(name.toLowerCase());
                ba.ipcTransmit(IPC, new IPCEvent(name, time, EventRequester.PLAYER_LEFT));
                locateNext();
            }
        };
        ba.scheduleTask(locate, LOCATE_WAIT);
    }
    
    private void stop(String name) {
        ba.cancelTasks();
        stop = true;
        start = false;
        roaming = false;
        ba.sendSmartPrivateMessage(name, "Yes, sir! Haulting processes, resetting and returning home.");
        online.clear();
        locateQueue.clear();
        locating = "";
        ba.ipcTransmit(IPC, new IPCEvent(new Vector<String>(), System.currentTimeMillis(), EventRequester.PLAYER_LEFT));
        ba.changeArena(HOME);
    }
    
    private void start(String name) {
        ba.cancelTasks();
        stop = false;
        ba.sendSmartPrivateMessage(name, "Yes, sir! Setting the wheels in motion...");
        ba.requestArenaList();        
    }
    
    private boolean isNotBot(String name) {
        if (ops.isBotExact(name) || (!ops.isOwner(name) && ops.isSysopExact(name) && !name.equalsIgnoreCase("Pure_Luck") && !name.equalsIgnoreCase("Witness")))
            return false;
        else
            return true;
    }
    
    private void debugger(String name) {
        if (!DEBUG) {
            debugger = name;
            DEBUG = true;
            ba.sendSmartPrivateMessage(name, "Debugging ENABLED. You are now set as the debugger.");
        } else if (debugger.equalsIgnoreCase(name)){
            debugger = "";
            DEBUG = false;
            ba.sendSmartPrivateMessage(name, "Debugging DISABLED and debugger reset.");
        } else {
            ba.sendSmartPrivateMessage(name, "Debugging still ENABLED and you have replaced " + debugger + " as the debugger.");
            debugger = name;
        }
    }
    
    public void debug(String msg) {
        if (DEBUG)
            ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }
}
