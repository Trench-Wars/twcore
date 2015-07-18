package twcore.bots.whobot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
    private static final long GO_DELAY = 120 * Tools.TimeInMillis.SECOND;
    private static final String PUBBOT = "TW-Guard";
    private static final String WHOHUB = "TW-Chat";
    private static final String HOME = "#robopark";
    private static final int MAX_SPAM = 5;

    private static final int OFF = -1;
    private static final int IDLE = 0;
    private static final int STARTING = 1;
    private static final int ROAMING = 2;
    private static final int PAUSED = 3;
    private static final int LOCATING = 4;
    
    private int status = -1;
    
    public BotAction ba;
    public OperatorList ops;
    
    public HashSet<String> nogo;
    
    private HashMap<String, Long> online;
    
    // names waiting to be checked if online
    private Vector<String> locateQueue;
    
    private Vector<String> arenaQueue;
    
    private int arenaCount;
    private String locating;
    private TimerTask wait;
    // checks for *locate to return or next 
    private TimerTask locate;
    private TimerTask go;
    
    private boolean alert;
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
        
        arenaCount = 0;
        status = STARTING;
        locating = "";
        debugger = "";
        DEBUG = false;
        alert = false;
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
        boolean home = ba.getArenaName().equalsIgnoreCase(HOME);
        if (home && status == OFF)
            return;
        processPlayers();
        if (status == STARTING && home) {
            debug("Starting..");
            status = IDLE;
            ba.requestArenaList();
        } else if (status == PAUSED && home) {
            // if PAUSED then reached arenaCount so reset and roam after delay
            debug("Returning home to prevent a potential DC...");
            arenaCount = 0;
            go = new TimerTask() {
                public void run() {
                    if (!arenaQueue.isEmpty()) {
                        status = ROAMING;
                        arenaCount++;
                        ba.changeArena(arenaQueue.remove(0));
                    } else {
                        status = IDLE;
                        ba.changeArena(HOME);
                    }
                }
            };
            ba.scheduleTask(go, GO_DELAY);
            startLocate();
        } else if (status == ROAMING) {
            // arena to arena method used when ROAMING
            go = new TimerTask() {
                public void run() {
                    String arena = HOME;
                    if (!arenaQueue.isEmpty()) {
                        if (arenaCount < MAX_SPAM) {
                            // go to next arena and stay roaming
                            arenaCount++;
                            arena = arenaQueue.remove(0);
                        } else
                            status = PAUSED; // reached max consecutive arenas so return home and PAUSE
                    } else 
                        status = IDLE; // out of arenas to visit so IDLE at home
                    ba.changeArena(arena);
                }
            };
            ba.scheduleTask(go, GO_TIME);
        } else if (status == IDLE){
            wait = new TimerTask() {
                public void run() {
                    debug("Requesting arena list.");
                    ba.requestArenaList();
                }
            };
            ba.scheduleTask(wait, IDLE_TIME);
            startLocate();
        }

    }
    
    public void handleEvent(ArenaList event) {
        arenaQueue.clear();
        Map<String, Integer> arenas = event.getArenaList();
        for (String a : arenas.keySet()) {
            if (!a.equalsIgnoreCase(HOME) && !Tools.isAllDigits(a) && ((arenas.get(a) < 3 && !nogo.contains(a.toLowerCase()) || a.contains("#"))))
                arenaQueue.add(a);
        }
        go = new TimerTask() {
            public void run() {
                status = ROAMING;
                if (!arenaQueue.isEmpty()) {
                    arenaCount++;
                    ba.changeArena(arenaQueue.remove(0));
                } else {
                    status = IDLE;
                    ba.changeArena(HOME);
                }
            }
        };
        ba.scheduleTask(go, GO_TIME);
        debug("Arena list received and initiating roaming. ");
    }
    
    public void handleEvent(Message event) {
        String msg = event.getMessage();
        
        if (event.getMessageType() == Message.ARENA_MESSAGE && status != OFF) {
            if (locating.length() > 0 && msg.contains(" - ")) {
                String found = msg.substring(0, msg.lastIndexOf(" - ")).toLowerCase();
                if (locateQueue.remove(found)) {
                    if (online.containsKey(found.toLowerCase()))
                        online.put(found, System.currentTimeMillis());
                    ba.cancelTask(locate);
                    debug("Located " + found);
                }
                locateNext();
            }
        }

        if (event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE || event.getMessageType() == Message.PRIVATE_MESSAGE) {
            String name = event.getMessager();
            if (name == null || name.length() < 1)
                name = m_botAction.getPlayerName(event.getPlayerID());

            if (msg.equals("!about")) {
                about(name);
                ba.ipcTransmit(IPC, new IPCEvent(name + ":" + msg, System.currentTimeMillis(), EventRequester.MESSAGE));
                return;
            } else if (msg.toLowerCase().startsWith("!whohas ") || msg.toLowerCase().startsWith("!s ") || msg.toLowerCase().startsWith("!squad ")) {
                if (alert)
                    ba.sendChatMessage(name + " said: " + msg);
                ba.ipcTransmit(IPC, new IPCEvent(name + ":" + msg, System.currentTimeMillis(), EventRequester.MESSAGE));
                return;
            }
            
            if (ops.isSmod(name) && msg.startsWith("!say ")) {
                say(name, msg);
                return;
            }
            
            if (ops.isHighmod(name) || ops.isDeveloperExact(name)) {
                if (msg.equals("!stop"))
                    stop(name);
                else if (msg.equals("!start"))
                    start(name);
                else if (msg.equals("!debug"))
                    debugger(name);
                else if (msg.equals("!alert"))
                    alert(name);
                else if (msg.equals("!die"))
                    die(name);
                else if (msg.equals("!pro"))
                    processPlayers();
                else if (msg.equals("!help"))
                    sendHelp(name);
                else if (msg.startsWith("!stat")) {
                    status(name);
                    ba.ipcTransmit(IPC, new IPCEvent(name + ":!stats", System.currentTimeMillis(), EventRequester.MESSAGE));
                } else if (msg.equals("!online"))
                    online(name);
                else if (msg.equals("!arenas"))
                    arenas(name);
                else if (!ops.isBotExact(name)) {
                    ba.sendChatMessage(name + " said: " + msg);
                    ba.ipcTransmit(IPC, new IPCEvent(name + ":" + msg, System.currentTimeMillis(), EventRequester.MESSAGE));
                }
            } else if (!ops.isBotExact(name)) {
                ba.sendChatMessage(name + " said: " + msg);
                if (msg.startsWith("!")) {
                    about(name);
                    ba.ipcTransmit(IPC, new IPCEvent(name + ":" + msg, System.currentTimeMillis(), EventRequester.MESSAGE));
                } else if (ba.getArenaName().contains("#"))
                    ba.sendSmartPrivateMessage(name, "Sorry, don't mind me! I'm just passing through. I won't tell anyone about your super secret hideout, trust me!");
            }
        }
    }
    
    private void alert(String name) {
        alert = !alert;
        if (alert)
            ba.sendChatMessage("Command relay alerts: ENABLED");
        else
            ba.sendChatMessage("Command relay alerts: DISABLED");
    }
    
    private void say(String name, String cmd) {
        if (!cmd.contains(":")) {
            ba.sendSmartPrivateMessage(name, "I can only do pms.");
        } else {
            String[] msg = cmd.substring(cmd.indexOf(" ")+1).split(":");
            ba.sendSmartPrivateMessage(msg[0], msg[1]);
            ba.sendChatMessage(name + " says to " + msg[0] + ": " + msg[1]);
            ba.sendSmartPrivateMessage(name, "Message sent.");
        }
    }
    
    public void handleEvent(InterProcessEvent event) {
        // used to talk with TWChat
        if (!event.getChannel().equals(IPC) || status == OFF)
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
            if (ipc.getMessage().equals("who:refresh"))
                ba.ipcTransmit(IPC, new IPCEvent(online, System.currentTimeMillis(), EventRequester.PLAYER_ENTERED));
            else if (ipc.getMessage().equals("who:deviates")) {
                Set<String> dev = (Set<String>) online.keySet();
                ba.ipcTransmit(IPC, new IPCEvent(dev, System.currentTimeMillis(), EventRequester.PLAYER_POSITION));
            }
        }
    }
    
    private void online(String name) {
        String msg = "Online: ";
        for (String p : online.keySet())
            msg += p + ", ";
        ba.sendSmartPrivateMessage(name, msg.substring(0, msg.length()-2));
    }
    
    private void arenas(String name) {
        String msg = "Arenas: ";
        for (String a : arenaQueue)
            msg += a + " ";
        ba.sendSmartPrivateMessage(name, msg);
    }
    
    private void about(String name) {
        String[] msg = {
                ",---------------------------| WhoBot |---------------------------+",
                "| WhoBot is a roaming bot that keeps track of online players in  |",
                "| private and low population arenas. WhoBot synchronizes with    |",
                "| TW-Chat bot so that the online player information remains as   |",
                "| accurate as possible. After roaming, WhoBot also uses *locate  |",
                "| to reduce update delays caused by SS arena change restrictions.|",
                "`----------------------------------------------------------------+"
        };
        ba.smartPrivateMessageSpam(name, msg);
    }
    
    private void sendHelp(String name) {
        String[] msg = {
                ",---------------------------| WhoBot SMOD Commands |----------------------------+",
                "| !about     - What the hell is this bot and why is it in this arena?           |",
                "| !status    - Displays current process and statistical information             |",
                "| !online    - Displays local list of online players (outsiders)                |",
                "| !arenas    - Displays all the arenas currently enqueued                       |",
                "| !stop      - Haults all processes, retracts player records and returns home   |",
                "| !start     - Starts all processes and begins roaming low population arenas    |",
                "| !debug     - Debug toggle sets/removes/hijacks the requester as the debugger  |",
                "| !die       - Retracts player information and logs off                         |",
                "`-------------------------------------------------------------------------------+"
        };
        ba.smartPrivateMessageSpam(name, msg);
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
            ba.die("!die from unknown source/Session disconnect");
    }
    
    public void processPlayers() {
        if (status == OFF)
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
            if (!ops.isBotExact(name)) {
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
    
    private void startLocate() {
        status = LOCATING;
        debug("Locate processesing engaged...");
        long recent = System.currentTimeMillis() - CHECK_TIME;
        for (String n : online.keySet()) {
            if (online.get(n) < recent && !locateQueue.contains(n.toLowerCase()))
                locateQueue.add(n.toLowerCase());
        }
        locateNext();
    }
    
    private void locateNext() {
        if (locateQueue.isEmpty()) {
            locating = "";
            if (status != OFF)
                status = IDLE;
            return;
        }
        final String name = locateQueue.get(0);
        final long time = System.currentTimeMillis();
        locating = name;
        debug("Attempting to locate " + name);
        locate = new TimerTask() {
            public void run() {
                if (status == OFF) return;
                debug("Locate did not return " + name);
                locateQueue.remove(name.toLowerCase());
                online.remove(name.toLowerCase());
                ba.ipcTransmit(IPC, new IPCEvent(name, time, EventRequester.PLAYER_LEFT));
                locateNext();
            }
        };
        ba.sendUnfilteredPublicMessage("*locate " + locating);
        try {
            ba.scheduleTask(locate, LOCATE_WAIT);
        } catch( IllegalStateException e) {}
    }
    
    private void stop(String name) {
        if (status == OFF) {
            ba.sendSmartPrivateMessage(name, "I am stopped already.");
            return;
        }
        ba.cancelTasks();
        status = OFF;
        ba.sendSmartPrivateMessage(name, "Yes, sir! Haulting processes, resetting and returning home.");
        online.clear();
        locateQueue.clear();
        locating = "";
        ba.ipcTransmit(IPC, new IPCEvent(new Vector<String>(), System.currentTimeMillis(), EventRequester.PLAYER_LEFT));
        ba.sendChatMessage("STOPPING all functions as requested by " + name);
        ba.changeArena(HOME);
    }
    
    private void start(String name) {
        ba.cancelTasks();
        status = STARTING;
        ba.sendChatMessage("STARTING all functions as requested by " + name);
        ba.sendSmartPrivateMessage(name, "Yes, sir! Setting the wheels in motion...");
        ba.requestArenaList();        
    }
    
    private void status(String name) {
        String msg = "Currently " + getStatus() + " in " + ba.getArenaName() + " | Outsiders=" + online.size() + " | Queues: Locates=" + locateQueue.size() + " Arenas=" + arenaQueue.size() + " with " + (MAX_SPAM-arenaCount) + " arenas until IDLE";
        ba.sendSmartPrivateMessage(name, msg);
    }
    
    private String getStatus() {
        switch (status) {
        case OFF:
            return "OFF";
        case IDLE:
            return "IDLING";
        case STARTING:
            return "STARTING";
        case ROAMING:
            return "ROAMING";
        case PAUSED:
            return "PAUSING";
        case LOCATING:
            return "LOCATING";
        default:
            return "";
        }
        
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
            ba.sendChatMessage(name + " has overriden " + debugger + " as the target of debug messages.");
            ba.sendSmartPrivateMessage(name, "Debugging still ENABLED and you have replaced " + debugger + " as the debugger.");
            debugger = name;
        }
    }
    
    public void debug(String msg) {
        if (DEBUG)
            ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }
}
