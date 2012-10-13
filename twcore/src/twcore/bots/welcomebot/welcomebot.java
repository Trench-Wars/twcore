package twcore.bots.welcomebot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.PlayerDeath;
import twcore.core.events.SQLResultEvent;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.Tools;
import twcore.core.util.ipc.IPCMessage;

/**
 * WelcomeBot is a player tracking bot designed specifically for identifying and monitoring
 * players new to the game. Stores tracking data in the database each time an identified new player
 * logs on creating a new record for each login. Upon detection of a new player an alert is issued
 * to staff which allows them to handle the call accordingly. Prepared statements are used for most
 * queries for ease of use and efficiency. Identification of new players relies on information
 * previously recorded by the stats module of pubhub. The last function of welcome bot is regulating
 * the new player tutorial objons for automatic use upon detection and staff. 
 *
 * @author WingZero
 */
public class welcomebot extends SubspaceBot {

    private static final String IPC_CHANNEL = "Zone Channel";
    private static final String web = "website";
    private static final String db = "welcome";
    
    private BotSettings cfg;
    private OperatorList ops;
    
    private TreeSet<String> vets;                           // Veterans ignore list of previously checked players found not to be new
    private TreeSet<String> trusted;                        // Trusted players and staff for new player login notifications
    private TreeSet<String> trainers;                       // Trainer aliases to waiting to be triggered
    private TreeMap<String, Session> sessions;              // All currently tracked playing new players
    
    private TreeMap<String, AliasCheck> aliases;
    private TreeMap<String, Integer[]> loopCatcher;
    private TreeMap<String, ObjonTimer> objonTimers;
    private TreeMap<String, Stack<String[]>> tutorials;

    private PreparedStatement psAddTrusted;                 // Add trusted player
    private PreparedStatement psRemTrusted;                 // Remove trusted player
    private PreparedStatement psUpdatePlayer;               // Creates or updates a NewPlayer record
    private PreparedStatement psUpdateSession;              // Used to create a session record
    private PreparedStatement psCheckAlerts;                // Checks for prior newb call listing
    private PreparedStatement psGetCountryCode;             // Gets country code using IP
    private PreparedStatement psGetSessionCount;            // Gets current session count
    private PreparedStatement psInsertNewb;                 // Creates a new newbcall alert entry
    
    private String infoee;
    private String debugger;
    private boolean DEBUG;
    private boolean ready;                                  // Prevents bot from operating outside of pub
    
    public welcomebot(BotAction botAction) {
        super(botAction);
        
        cfg = ba.getBotSettings();
        ops = ba.getOperatorList();
        
        EventRequester er = ba.getEventRequester();
        er.request(EventRequester.MESSAGE);
        er.request(EventRequester.LOGGED_ON);
        er.request(EventRequester.PLAYER_DEATH);
        er.request(EventRequester.PLAYER_ENTERED);
        er.request(EventRequester.PLAYER_LEFT);
    }
    
    private void init() {
        ready = false;
        infoee = null;
        debugger = "WingZero";
        DEBUG = true;
        
        vets = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        trusted = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        trainers = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        sessions = new TreeMap<String, Session>(String.CASE_INSENSITIVE_ORDER);
        aliases = new TreeMap<String, AliasCheck>(String.CASE_INSENSITIVE_ORDER);
        loopCatcher = new TreeMap<String, Integer[]>(String.CASE_INSENSITIVE_ORDER);
        objonTimers = new TreeMap<String, ObjonTimer>(String.CASE_INSENSITIVE_ORDER);
        tutorials = new TreeMap<String, Stack<String[]>>(String.CASE_INSENSITIVE_ORDER);
        
        psInsertNewb = ba.createPreparedStatement(web, db, "INSERT INTO tblCallNewb (fcUserName, fdCreated) VALUES (?, NOW())");
        psCheckAlerts = ba.createPreparedStatement(web, db, "SELECT * FROM tblCallNewb WHERE fcUserName = ?");
       
        psAddTrusted = ba.createPreparedStatement(db, db, "INSERT INTO tblTrustedPlayers (fcPlayerName, fcAddedBy) VALUES(?,?)");
        psRemTrusted = ba.createPreparedStatement(db, db, "DELETE FROM tblTrustedPlayers WHERE fcPlayerName = ?");
        psGetSessionCount = ba.createPreparedStatement(db, db, "SELECT COUNT(*) FROM tblNewPlayerSession WHERE fcName = ?");
        psGetCountryCode = ba.createPreparedStatement(db, db, "SELECT country_code3 FROM tblCountryIPs WHERE INET_ATON(?) >= ip_from AND INET_ATON(?) <= ip_to");
        psUpdateSession = ba.createPreparedStatement(db, db, "INSERT INTO tblNewPlayerSession (fcName, ffSessionUsage, ffTotalUsage, fnKills, fnDeaths) VALUES(?,?,?,?,?)");
        psUpdatePlayer = ba.createPreparedStatement(db, db, "INSERT INTO tblNewPlayer " 
                + "(fcName, fcSquad, ffTotalUsage, fcIP, fnMID, fcCountryCode, fdNameCreated) " 
                + "VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE fcSquad=VALUES(fcSquad), ffTotalUsage=VALUES(ffTotalUsage), fcIP=VALUES(fcIP), fnMID=VALUES(fnMID), fcCountryCode=VALUES(fcCountryCode)");

        if (psInsertNewb == null || psGetSessionCount == null || psGetCountryCode == null || psCheckAlerts == null || psUpdateSession == null || psUpdatePlayer == null) {
            ba.sendChatMessage("Disconnecting due to null prepared statement(s)...");
            Tools.printLog("WelcomeBot: One or more PreparedStatements are null! Bot disconnecting.");
            ba.die("null prepared statement");
            return;
        }
        loadTrusted();
    }
    
    private void loadTrusted() {
        trusted.clear();
        ResultSet rs = null;
        try {
            rs = ba.SQLQuery(db, "SELECT fcPlayerName FROM tblTrustedPlayers");
            while (rs.next())
                trusted.add(rs.getString(1));
            if (DEBUG) {
                String msg = "Trusted(" + trusted.size() + "): ";
                for (String n : trusted)
                    msg += n + ", ";
                msg = msg.substring(0, msg.length() - 2);
                debug(msg);
            }
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        } finally {
            ba.SQLClose(rs);
        }
    }
    
    public void handleEvent(LoggedOn event) {
        
        init();
        ba.joinArena(cfg.getString("InitialArena"));
        ba.ipcSubscribe(IPC_CHANNEL);
        ba.sendUnfilteredPublicMessage("?chat=robodev,staff");
        ready = ba.getArenaName().startsWith("(Public");
    }
    
    /**
     * Takes entering players and checks for new player call record. If there is no
     * found record then new player detection commences. Otherwise, if the alert was genuine
     * then player tracking is started.
     */
    public void handleEvent(PlayerEntered event) {
        if (!ready) return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null || vets.contains(name))
            return;
        if (trainers.remove(name))
            ba.sendAlertMessage("newplayer", name);
        else {
            try {
                int taken = -1;
                psCheckAlerts.clearParameters();
                psCheckAlerts.setString(1, name);
                ResultSet rs = psCheckAlerts.executeQuery();
                if (rs != null && rs.next())
                    taken = rs.getInt(1);
                // 0 is missed, 1 is taken
                if (taken < 0) {
                    // no record so move on to detection
                    ba.sendUnfilteredPrivateMessage(name, "*info");
                } else if (taken < 3) {
                    debug("Taken for " + name + ": " + taken);
                    if (taken == 0) {
                        ba.SQLBackgroundQuery(web, null, "UPDATE tblCallNewb SET fdCreated = NOW() WHERE fnAlertID = " + rs.getInt("fnAlertID"));
                        debug("Sending newplayer alert: " + name);
                        ba.sendAlertMessage("newplayer", name);
                    }
                    if (!sessions.containsKey(name))
                        sessions.put(name, new Session(ba.getPlayer(name)));
                } else {
                    vets.add(name);
                    debug("Added vet: " + name);
                }
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
        }
    }
    
    /**
     * Session is ended if the player tracked leaves the arena.
     */
    public void handleEvent(PlayerLeft event) {
        if (!ready) return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (sessions.containsKey(name))
            sessions.get(name).endSession();
        tutorials.remove(name);
    }
    
    public void handleEvent(PlayerDeath event) {
        if (!ready) return;
        String killer = ba.getPlayerName(event.getKillerID());
        if (sessions.containsKey(killer))
            sessions.get(killer).addKill();
        String killed = ba.getPlayerName(event.getKilleeID());
        if (sessions.containsKey(killed))
            sessions.get(killed).addDeath();
    }
    
    public void handleEvent(Message event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null)
            name = event.getMessager();
        String msg = event.getMessage();
        String cmd = msg.toLowerCase();
        int type = event.getMessageType();
        
        if (type == Message.ARENA_MESSAGE)
            handleArenaMessage(msg);
        else if (type == Message.PUBLIC_MESSAGE) {
            // Player Commands
            if (cmd.startsWith("!help") && cmd.contains("tutorial"))
                cmd_helpTutorial(name);
            else if (cmd.equals("!tutorial")) 
                cmd_tutorial(name);
            else if (cmd.equals("!next")) 
                cmd_next(name, true);
            else if (cmd.equals("!end")) 
                cmd_end(name);
            else if (cmd.equals("!quickhelp")) 
                cmd_quickHelp(name);
        } else if (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) { 
            // Player Commands
            if (cmd.equals("!help"))
                cmd_help(name);
            else if (cmd.startsWith("!help") && cmd.contains("tutorial"))
                cmd_helpTutorial(name);
            else if (cmd.equals("!tutorial")) 
                cmd_tutorial(name);
            else if (cmd.equals("!next")) 
                cmd_next(name, false);
            else if (cmd.equals("!end")) 
                cmd_end(name);
            else if (cmd.equals("!quickhelp")) 
                cmd_quickHelp(name);
            
            // Staff Commands
            if (ops.isZH(name)) {
                if (cmd.startsWith("!newplayer "))
                    cmd_newplayer(name, msg);
                else if (cmd.startsWith("!next "))
                    cmd_next(name, msg);
                else if (cmd.startsWith("!end "))
                    cmd_end(name, msg);
            }
            if (ops.isSmod(name)) {
                if (cmd.equals("!die"))
                    cmd_die(name, true);
                else if (cmd.equals("!kill"))
                    cmd_die(name, false);
                else if (cmd.equals("!debug"))
                    cmd_debug(name);
                else if (cmd.equals("!dome"))
                    cmd_doMe(name);
                else if (cmd.startsWith("!trust "))
                    cmd_addTrusted(name, msg);
                else if (cmd.startsWith("!untrust "))
                    cmd_remTrusted(name, msg);
                else if (cmd.startsWith("!go "))
                    cmd_go(name, msg);
            }
            
        }
    }
    
    private void cmd_go(String name, String msg) {
        msg = msg.substring(msg.indexOf(" ") + 1);
        ba.sendSmartPrivateMessage(name, "Going to " + msg);
        ba.changeArena(msg);
        ready = ba.getArenaName().startsWith("(Public");
    }
    
    private void cmd_doMe(String name) {
        Player p = ba.getPlayer(name);
        if (p != null) {
            ba.sendSmartPrivateMessage(name, "Creating a new session for you!");
            sessions.put(name, new Session(p));
        } else
            ba.sendSmartPrivateMessage(name, "You must be in my arena.");
    }
    
    private void cmd_help(String name) {
        
        ArrayList<String> msgs = new ArrayList<String>();
        if (ops.isZH(name)) {
            msgs.add("!newplayer <name>     -- Sends new player helper objon to <name>.");
            msgs.add("!next <name>          -- Sends the next helper objon to <name>.");
            msgs.add("!end <name>           -- Removes all objons for <name>.");
        }
    }
    
    /**
     * Breaks down the lines of text received after a command has been sent.
     * Lines are identified and then distributed accordingly.
     * 
     * @param message
     */
    private void handleArenaMessage(String message) {
            if (message.contains("TypedName:")) {
                infoee = message.substring(message.indexOf("TypedName:") + 10);
                infoee = infoee.substring(0, infoee.indexOf("Demo:")).trim();
                if (sessions.containsKey(infoee))
                    sessions.get(infoee).setInfo(message);
            } else if (message.startsWith("TIME: Session:")) {
                if (infoee != null && sessions.containsKey(infoee))
                    sessions.get(infoee).setUsage(message);
                else {
                    String time = message.substring(message.indexOf("Total:") + 6);
                    time = time.substring(0, time.indexOf("Created")).trim();
                    String[] pieces = time.split(":");
                    if (pieces.length == 3) {
                        int hour = Integer.valueOf(pieces[0]);
                        int min = Integer.valueOf(pieces[1]);
                        if (pieces[0].equals("0")) { // if usage less than 1 hour
                            if (aliases.containsKey(infoee)) {
                                AliasCheck alias = aliases.get(infoee);
                                alias.setUsage(hour * 60 + min);
                                System.out.println("[ALIAS] " + alias.getName() + " in array already.");
                                debug("[ALIAS] " + alias.getName() + " in array already.");
                                if (alias.getTime() > 900000) {
                                    alias.resetTime();
                                    doAliasCheck(alias);
                                }
                            } else {
                                AliasCheck alias = new AliasCheck(infoee, hour * 60 + min);
                                doAliasCheck(alias);
                            }
                        }
                    }
                }
            } else if ((message.startsWith("PING Current") || message.startsWith("Ping:")) && infoee != null && sessions.containsKey(infoee)) {
                // either from *lag or *info, either way send it to session for possible welcome objon and message
                sessions.get(infoee).checkPing(message);
            }
    }
    
    /**
     * IPC messages are exchanged with RoboHelp for falsed/un-falsed newb calls
     * and trainer alert triggers.
     */
    public void handleEvent(InterProcessEvent event) {
        if (!ready) return;
        if (event.getChannel().equals(IPC_CHANNEL) && event.getObject() instanceof IPCMessage) {
            IPCMessage ipc = (IPCMessage) event.getObject();
            debug("IPCMessage: " + ipc.getMessage());
            if (ipc.getSender().equalsIgnoreCase("RoboHelp")) {
                String msg = ((IPCMessage) event.getObject()).getMessage();
                if (msg.startsWith("false:")) {
                    String name = msg.substring(msg.indexOf(" ") + 1);
                    if (sessions.containsKey(name))
                        sessions.get(name).falseSession();
                    vets.add(name);
                } else if (msg.startsWith("undo:")) {
                    String name = msg.substring(msg.indexOf(" ") + 1);
                    vets.remove(name);
                    Player p = ba.getPlayer(name);
                    if (p != null)
                        sessions.put(name, new Session(p));
                } else if (msg.startsWith("newb:")) {
                    String[] args = msg.substring(5).split(",");
                    if (trainers.contains(args[1]))
                        ba.ipcSendMessage(IPC_CHANNEL, args[0] + ":" + args[1] + " was already set as a trainer newb alert alias.", "RoboHelp", ba.getBotName());
                    else
                        ba.ipcSendMessage(IPC_CHANNEL, args[0] + ":" + args[1] + " will trigger a newb alert upon next visit." + ":" + args[1], "RoboHelp", ba.getBotName());
                    trainers.add(args[1]);                
                }
            }
        }
    }

    /**
     * SQL events are used for alias checking functions done using background
     * queries in order to keep down load size.
     */
    public void handleEvent(SQLResultEvent event) {
        ResultSet resultSet = event.getResultSet();
        if (resultSet == null)
            return;

        if (event.getIdentifier().startsWith("alias:")) {

            String name = event.getIdentifier().substring(event.getIdentifier().lastIndexOf(":") + 1);
            AliasCheck alias = aliases.get(name);
            if (alias == null)
                return;

            debug("SQL: " + event.getIdentifier());
            // GET IP + MID
            if (event.getIdentifier().startsWith("alias:ip:")) {
                StringBuffer buffer = new StringBuffer();
                try {
                    resultSet.beforeFirst();
                    while (resultSet.next()) {
                        buffer.append(", ");
                        buffer.append(resultSet.getString("fnIP"));
                    }
                } catch (Exception e) {
                    Tools.printStackTrace(e);
                }

                if (buffer.length() > 2)
                    alias.setIpResults("(" + buffer.toString().substring(2) + ") ");
                else {
                    System.out.println("[ALIAS] " + buffer.toString());
                    final String aliasIP = alias.getName();
                    Integer count = 0;
                    if (loopCatcher.containsKey(aliasIP)) {
                        Integer[] tasks = loopCatcher.get(aliasIP);
                        if (tasks == null)
                            tasks = new Integer[] { 1, 0 };
                        else {
                            tasks[0]++;
                            count = tasks[0];
                        }
                        loopCatcher.put(aliasIP, tasks);
                    }
                    if (count > 5)
                        alias.setIpResults("");
                    if (alias.getIpResults() == null && web != null) {
                        TimerTask delayIP = new TimerTask() {
                            @Override
                            public void run() {
                                System.out.println("[ALIAS] Blank IP: " + aliasIP + " Task Scheduled.");
                                ba.SQLBackgroundQuery(web, "alias:ip:" + aliasIP, "SELECT DISTINCT(fnIP) "
                                        + "FROM `tblAlias` INNER JOIN `tblUser` ON `tblAlias`.fnUserID = `tblUser`.fnUserID " + "WHERE fcUserName = '" + Tools.addSlashes(aliasIP) + "'");
                            }
                        };
                        ba.scheduleTask(delayIP, 10000);
                    }
                }
            } else if (event.getIdentifier().startsWith("alias:mid:")) {
                StringBuffer buffer = new StringBuffer();
                try {
                    resultSet.beforeFirst();
                    while (resultSet.next()) {
                        buffer.append(", ");
                        buffer.append(resultSet.getString("fnMachineID"));
                    }
                } catch (Exception e) {
                    Tools.printStackTrace(e);
                }

                if (buffer.length() > 2)
                    alias.setMidResults("(" + buffer.toString().substring(2) + ") ");
                else {
                    final String aliasMID = alias.getName();
                    Integer count = 0;
                    if (loopCatcher.containsKey(aliasMID)) {
                        Integer[] tasks = loopCatcher.get(aliasMID);
                        if (tasks == null)
                            tasks = new Integer[] { 0, 1 };
                        else {
                            tasks[1]++;
                            count = tasks[1];
                        }
                        loopCatcher.put(aliasMID, tasks);
                    }
                    if (count > 5)
                        alias.setMidResults("");
                    if (alias.getMidResults() == null && web != null) {
                        TimerTask delayMID = new TimerTask() {
                            @Override
                            public void run() {
                                System.out.println("[ALIAS] Blank MID: " + aliasMID + " Task Scheduled.");
                                ba.SQLBackgroundQuery(web, "alias:mid:" + aliasMID, "SELECT DISTINCT(fnMachineID) "
                                        + "FROM `tblAlias` INNER JOIN `tblUser` ON `tblAlias`.fnUserID = `tblUser`.fnUserID " + "WHERE fcUserName = '" + Tools.addSlashes(aliasMID) + "'");
                            }
                        };
                        ba.scheduleTask(delayMID, 13000);
                    }
                }

            }
            // Retrieve the final query using IP+MID
            if (event.getIdentifier().startsWith("alias:final:")) {
                HashSet<String> prevResults = new HashSet<String>();
                int numResults = 0;

                try {
                    while (resultSet.next()) {
                        String username = resultSet.getString("fcUserName");
                        if (!prevResults.contains(username)) {
                            prevResults.add(username);
                            numResults++;
                        }
                    }
                } catch (Exception e) {}

                alias.setAliasCount(numResults);
                alias.checkAndSend();

            }
            // Send final query if we have IP+MID
            else if (alias.getIpResults() != null && alias.getMidResults() != null) {
                if (alias.getIpResults().equals("") || alias.getMidResults().equals("")) {
                    alias.setAliasCount(0);
                    String reason = alias.getIpResults().equals("") ? "ip" : "mid";
                    if (alias.getIpResults().equals("") && alias.getMidResults().equals(""))
                        reason = "ip&mid";
                    System.out.println("[ALIAS] " + alias.getName() + " (empty:" + reason + ")");
                    alias.checkAndSend();
                } else {
                    ba.SQLBackgroundQuery(web, "alias:final:" + name, 
                            "SELECT * " + "FROM `tblAlias` INNER JOIN `tblUser` ON `tblAlias`.fnUserID = `tblUser`.fnUserID "
                            + "WHERE fnIP IN " + alias.getIpResults() + " " + "AND fnMachineID IN " + alias.getMidResults() + " ORDER BY fdUpdated DESC");
                }
            }
            ba.SQLClose(event.getResultSet());
        }
    }

    /**
     * Alias check using background queries.
     * 
     * @param alias
     */
    private void doAliasCheck(AliasCheck alias) {
        debug("AliasCheck begun for " + alias.getName());
        aliases.put(alias.getName(), alias);
        loopCatcher.put(alias.getName(), new Integer[] { 0, 0 });
        ba.SQLBackgroundQuery(web, "alias:ip:" + alias.getName(), "SELECT DISTINCT(fnIP) " + "FROM `tblAlias` INNER JOIN `tblUser` ON `tblAlias`.fnUserID = `tblUser`.fnUserID "
                + "WHERE fcUserName = '" + Tools.addSlashes(alias.getName()) + "'");
        ba.SQLBackgroundQuery(web, "alias:mid:" + alias.getName(), "SELECT DISTINCT(fnMachineID) " + "FROM `tblAlias` INNER JOIN `tblUser` ON `tblAlias`.fnUserID = `tblUser`.fnUserID "
                + "WHERE fcUserName = '" + Tools.addSlashes(alias.getName()) + "'");
    }
    
    private void cmd_addTrusted(String name, String msg) {
        if (msg.length() < 8) return;
        String p = msg.substring(msg.indexOf(" ") + 1);
        if (trusted.contains(p))
            ba.sendSmartPrivateMessage(name, "Player already trusted.");
        else {
            ba.sendSmartPrivateMessage(name, "Trusted player added: " + p);
            trusted.add(p);
            try {
                psAddTrusted.setString(1, name);
                psAddTrusted.executeQuery();
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
        }
    }
    
    private void cmd_remTrusted(String name, String msg) {
        if (msg.length() < 10) return;
        String p = msg.substring(msg.indexOf(" ") + 1);
        if (!trusted.contains(p))
            ba.sendSmartPrivateMessage(name, "Player not found in trusted list.");
        else {
            ba.sendSmartPrivateMessage(name, "Trusted player removed: " + p);
            trusted.remove(p);
            try {
                psRemTrusted.setString(1, name);
                psRemTrusted.executeQuery();
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
        }
        
    }
    
    private void cmd_helpTutorial(String sender){
        List<String> list = new ArrayList<String>();
        list.add("This is your guide to use our tutorial.");
        list.add("Use !tutorial to start it");
        list.add("Use !next to see step by step");
        list.add("Use !quickhelp to see the whole tutorial");
        list.add("If you're done, try !end");
        list.add("Thanks to Flared and WingZero for creating the tutorial in Trench Wars!");
        m_botAction.privateMessageSpam(sender, list.toArray(new String[list.size()]));
    }

    private void cmd_tutorial(String player) {
        if (!tutorials.containsKey(player)) {
            if (ba.getPlayer(player).getShipType() == 0)
                ba.setShip(player, 1);
            ba.sendUnfilteredPrivateMessage(player, "*objon 2011");
            ba.sendUnfilteredPrivateMessage(player, "*objoff 2010");
            ba.sendUnfilteredPrivateMessage(player, "*objoff 2020");
            Stack<String[]> objons = new Stack<String[]>();
            objons.push(new String[]{"*objoff 2017", "*objon 2018", "*objoff 2019"});
            objons.push(new String[]{"*objoff 2016", "*objon 2017"});
            objons.push(new String[]{"*objoff 2015", "*objon 2016"});
            objons.push(new String[]{"*objoff 2014", "*objon 2015"});
            objons.push(new String[]{"*objoff 2013", "*objon 2014"});
            objons.push(new String[]{"*objoff 2012", "*objon 2013"});
            objons.push(new String[]{"*objoff 2011", "*objon 2012", "*objon 2019"});
            tutorials.put(player, objons);
        } else
            ba.sendPrivateMessage(player, "Use !next");
    }

    private void cmd_next(String player, boolean pub) {
        if (tutorials.containsKey(player)) {
            Stack<String[]> objects = tutorials.get(player);
            String[] objs = objects.pop();
            ba.sendUnfilteredPrivateMessage(player, objs[0]);
            ba.sendUnfilteredPrivateMessage(player, objs[1]);
            if (objs.length > 2)
                ba.sendUnfilteredPrivateMessage(player, objs[2]);
            if (objs.length > 3)
                ba.sendPrivateMessage(player, objs[3]);
            if (pub && !objs[0].equals("*objoff 2017"))
                ba.sendPrivateMessage(player, "" + player + ", to continue the tutorial, please type ::!next");
            tutorials.put(player, objects);
            if (objects.empty())
                tutorials.remove(player);
        } else
            ba.sendPrivateMessage(player, "You must first type !tutorial");
    }

    private void cmd_end(String player) {
        tutorials.remove(player);
        
        if (objonTimers.containsKey(player))
            ba.cancelTask(objonTimers.remove(player));
        
        ba.sendUnfilteredPrivateMessage(player, "*objoff 2010");
        ba.sendUnfilteredPrivateMessage(player, "*objoff 2011");
        ba.sendUnfilteredPrivateMessage(player, "*objoff 2012");
        ba.sendUnfilteredPrivateMessage(player, "*objoff 2013");
        ba.sendUnfilteredPrivateMessage(player, "*objoff 2014");
        ba.sendUnfilteredPrivateMessage(player, "*objoff 2015");
        ba.sendUnfilteredPrivateMessage(player, "*objoff 2016");
        ba.sendUnfilteredPrivateMessage(player, "*objoff 2017");
        ba.sendUnfilteredPrivateMessage(player, "*objoff 2018");
        ba.sendUnfilteredPrivateMessage(player, "*objoff 2019");
        ba.sendUnfilteredPrivateMessage(player, "*objoff 2020");
    }

    private void cmd_quickHelp(String player) {
        cmd_end(player);
        ba.sendUnfilteredPrivateMessage(player, "*objon 2020");
    }

    /**
     * Command initiates the new player objon for the specified player.
     * 
     * @param mod
     *          Staff member
     * @param name
     *          Player
     */
    private void cmd_newplayer(String mod, String name) {
        if (name.length() <= "!newplayer ".length())
            return;
        else
            name = name.substring(name.indexOf(" ") + 1);
        name = ba.getFuzzyPlayerName(name);
        if (name != null) {
            ba.sendUnfilteredPrivateMessage(name, "*objon 2025");
            ba.sendSmartPrivateMessage(mod, "New player objon sent to: " + name);
        } else {
            ba.sendSmartPrivateMessage(mod, "Player not found!");
        }
    }

    /**
     * Command triggers the next new player objon for the specified player.
     * 
     * @param mod
     *          Staff member
     * @param name
     *          Player
     */
    private void cmd_next(String mod, String name) {
        if (name.length() <= "!next ".length())
            return;
        else
            name = name.substring(name.indexOf(" ") + 1);
        name = ba.getFuzzyPlayerName(name);
        if (name != null) {
            ba.sendUnfilteredPrivateMessage(name, "*objoff 2025");
            ba.sendUnfilteredPrivateMessage(name, "*objon 2026");
            ba.sendSmartPrivateMessage(mod, "Next objon sent to: " + name);
        } else {
            ba.sendSmartPrivateMessage(mod, "Player not found!");
        }
    }

    /**
     * Command ends the tutorial objons for the specified player.
     * 
     * @param mod
     *          Staff member
     * @param name
     *          Player
     */
    private void cmd_end(String mod, String name) {
        if (name.length() <= "!end ".length())
            return;
        else
            name = name.substring(name.indexOf(" ") + 1);
        name = ba.getFuzzyPlayerName(name);
        if (name != null) {
            ba.sendUnfilteredPrivateMessage(name, "*objoff 2025");
            ba.sendUnfilteredPrivateMessage(name, "*objoff 2026");
            ba.sendSmartPrivateMessage(mod, "All objons removed for: " + name);
        } else {
            ba.sendSmartPrivateMessage(mod, "Player not found!");
        }

    }
    
    /**
     * The die and kill commands disconnect the bot with or without first saving
     * all the currently active sessions.
     * 
     * @param name
     * @param save
     */
    private void cmd_die(String name, boolean save) {
        if (save) {
            ba.sendSmartPrivateMessage(name, "Saving sessions and diconnecting...");
            ba.sendChatMessage("Disconnect with save request: " + name);
            // its weird but avoids concurrent modifications
            Set<String> names = sessions.keySet();
            for (String ses : names)
                sessions.get(ses).endSession();
            ba.scheduleTask(new TimerTask() { public void run() { ba.die(); }}, 3000);
        } else {
            ba.sendSmartPrivateMessage(name, "Diconnecting without saving...");
            ba.sendChatMessage("Disconnect without save request: " + name);
            ba.die();
        }
    }
    
    /**
     * Sends notifications to all appropriate players regarding new player logins.
     * 
     * @param session
     */
    private void doTrustedAlerts(Session session) {
        int c = session.getSessionCount();
        for (String n : trusted)
            ba.sendSmartPrivateMessage(n, "New player '" + session.getName() + "' has logged in now " + c + " times.");
    }
    
    private void cmd_debug(String name) {
        if (DEBUG) {
            if (debugger.equalsIgnoreCase(name)) {
                DEBUG = false;
                debugger = "";
                ba.sendSmartPrivateMessage(name, "Debugger: DISABLED");
            } else {
                debugger = name;
                ba.sendSmartPrivateMessage(name, "You have replaced the previous debugger.");
            }
        } else {
            DEBUG = true;
            debugger = name;
            ba.sendSmartPrivateMessage(name, "Debugger: ENABLED");
        }
    }
    
    private void debug(String msg) {
        if (DEBUG)
            ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }
    
    public void handleDisconnect() {
        ba.ipcUnSubscribe(IPC_CHANNEL);
        ba.closePreparedStatement(web, db, psInsertNewb);
        ba.closePreparedStatement(web, db, psCheckAlerts);
        ba.closePreparedStatement(db, db, psAddTrusted);
        ba.closePreparedStatement(db, db, psRemTrusted);
        ba.closePreparedStatement(db, db, psUpdatePlayer);
        ba.closePreparedStatement(db, db, psUpdateSession);
        ba.closePreparedStatement(db, db, psGetCountryCode);
        ba.closePreparedStatement(db, db, psGetSessionCount);
        ba.cancelTasks();
    }
    
    /**
     * Session holds all the information required for tracking and updates database.
     * Takes message lines from *info commands that extract usage and player information.
     *
     * @author WingZero
     */
    private class Session {
        
        DateFormat fromInfo = new SimpleDateFormat("M-d-yyyy HH:mm:ss");
        java.sql.Date created;
        String name;
        String squad;
        String countryCode;
        //String res;
        String ip;
        int mid;
        double totalUsage;
        double sessionUsage;
        long startTime;
        int kills, deaths;
        int sessionCount;
        
        public Session(Player p) {
            startTime = System.currentTimeMillis();
            name = p.getPlayerName();
            squad = p.getSquadName();
            debug("Session created for: " + name);
            kills = 0;
            deaths = 0;
            sessionUsage = 0;
            totalUsage = 0;
            sessionCount = 0;
            try {
                psGetSessionCount.clearParameters();
                psGetSessionCount.setString(1, name);
                ResultSet rs = psGetSessionCount.executeQuery();
                if (rs != null && rs.next())
                    sessionCount = rs.getInt(1) + 1;
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
            doTrustedAlerts(this);
            ba.sendUnfilteredPrivateMessage(name, "*info");
        }

        public String getName() {
            return name;
        }

        public void setUsage(String msg) {
            String[] args = new String[3];
            args[0] = msg.substring(14, msg.indexOf("Total")).trim();
            args[1] = msg.substring(msg.indexOf("Total") + 6, msg.indexOf("Created")).trim();
            args[2] = msg.substring(msg.indexOf("Created") + 8).trim();
            String[] split = args[0].split(":");
            sessionUsage = Math.round((Double.valueOf(split[0]) + (Double.valueOf(split[1]) / 60)) * 100) / 100.0;
            split = args[1].split(":");
            totalUsage = Math.round((Double.valueOf(split[0]) + (Double.valueOf(split[1]) / 60)) * 100) / 100.0;
            try {
                created = new java.sql.Date(fromInfo.parse(args[2]).getTime());
                
            } catch (ParseException e) {
                Tools.printStackTrace(e);
            }
            debug("Usage setting for " + name + ": Total:" + totalUsage + " Session:" + sessionUsage + " Created: " + created.toString());
            doUpdatePlayer();
        }
        
        public void checkPing(String message) {
            if (sessionCount != 1)
                return;
            debug("Ping check for " + name + ": " + message);
            String pingString = "";
            int currentPing = -1;
            if (message.startsWith("Ping:")) {
                pingString = message.substring(message.indexOf(':') + 1, message.indexOf('m'));
                currentPing = Integer.valueOf(pingString);

                ba.sendUnfilteredPrivateMessage(name, "*objon 2010");
                ba.sendPrivateMessage(name, "Welcome to Trench Wars! If you'd like to see a brief tutorial, please type !tutorial");
                if (currentPing == 0) {
                    ObjonTimer lagCheck = new ObjonTimer(name);
                    ba.scheduleTask(lagCheck, 45000, 15000);
                    objonTimers.put(name, lagCheck);
                }
            } else if (objonTimers.containsKey(name) && message.startsWith("PING Current")) {
                int pingCheck = Integer.valueOf(message.substring(message.indexOf(':') + 1, message.indexOf(" m")));
                if (pingCheck > 0) {
                    ba.sendUnfilteredPrivateMessage(name, "*objon 2010");
                    ba.cancelTask(objonTimers.remove(name));
                }
            }
        }
        
        public void doUpdatePlayer() {
            try {
                psUpdatePlayer.clearParameters();
                psUpdatePlayer.setString(1, name);
                psUpdatePlayer.setString(2, squad);
                psUpdatePlayer.setDouble(3, totalUsage);
                psUpdatePlayer.setString(4, ip);
                psUpdatePlayer.setInt(5, mid);
                psUpdatePlayer.setString(6, countryCode);
                psUpdatePlayer.setDate(7, created);
                psUpdatePlayer.executeQuery();
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
        }
        
        public void setInfo(String msg) {
            ip = msg.substring(3, msg.indexOf(" "));
            mid = Integer.valueOf(msg.substring(msg.lastIndexOf(":") + 1));
            try {
                psGetCountryCode.clearParameters();
                psGetCountryCode.setString(1, ip);
                psGetCountryCode.setString(2, ip);
                ResultSet rs = psGetCountryCode.executeQuery();
                if (rs != null && rs.next()) 
                    countryCode = rs.getString(1);
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
            debug("Info setting for " + name + ": IP:" + ip + " MID:" + mid + " CC: " + countryCode);
        }
        
        public void addKill() {
            kills++;
        }
        
        public void addDeath() {
            deaths++;
        }
        
        public void endSession() {
            // send updates and clear session
            long now = System.currentTimeMillis();
            int mins = (int) ((now - startTime) / Tools.TimeInMillis.MINUTE);
            int hours = (int) (mins / 60);
            mins -= hours * 60;
            sessionUsage = Math.round((hours + mins/60.0) * 100) / 100.0 + sessionUsage;
            debug("Session ending for " + name + ": SessionUsage:" + sessionUsage + " Kills:" + kills + " Deaths:" + deaths);
            try {
                psUpdateSession.clearParameters();
                psUpdateSession.setString(1, name);
                psUpdateSession.setDouble(2, sessionUsage);
                psUpdateSession.setDouble(3, totalUsage);
                psUpdateSession.setInt(4, kills);
                psUpdateSession.setInt(5, deaths);
                psUpdateSession.executeQuery();
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
            sessions.remove(name);            
        }
        
        public int getSessionCount() {
            return sessionCount;
        }
        
        public void falseSession() {
            sessions.remove(name);
        }
    }

    private class AliasCheck {
        private String name;
        private String ipResults;
        private String midResults;
        private int usage; // in minutes
        private int aliasCount = -1;
        private long time;

        public AliasCheck(String name, int usage) {
            this.name = name;
            this.usage = usage;
            this.time = System.currentTimeMillis();
        }

        public long getTime() {
            return System.currentTimeMillis() - time;
        }

        public void resetTime() {
            time = System.currentTimeMillis();
        }

        public String getName() {
            return name;
        }

        public void setUsage(int usage) {
            this.usage = usage;
        }

        public void setAliasCount(int count) {
            this.aliasCount = count;
        }

        public String getIpResults() {
            return ipResults;
        }

        public void setIpResults(String ipResults) {
            this.ipResults = ipResults;
        }

        public String getMidResults() {
            return midResults;
        }

        public void setMidResults(String midResults) {
            this.midResults = midResults;
        }
        
        public void checkAndSend() {
            System.out.print("[ALIAS] " + name + ":" + usage + ":" + aliasCount);
            if (usage < 15 && aliasCount < 2 && aliasCount >= 0) {
                try {
                    psInsertNewb.setString(1, name);
                    psInsertNewb.executeQuery();
                    ba.sendAlertMessage("newplayer", name);
                    System.out.println(":YES");
                } catch (SQLException e) {
                    Tools.printStackTrace(e);
                }
            } else {
                System.out.println(":NO");
                vets.add(name);
            }
        }
    }


    /**
     * This class is used for checking the lag status of newbs in order
     * to determine whether or not they will be able to see an objon
     */
    class ObjonTimer extends TimerTask {

        String name;

        public ObjonTimer(String p) {
            name = p;
        }

        public void run() {
            if (ba.getFuzzyPlayerName(name) != null) {
                infoee = name;
                ba.sendUnfilteredPrivateMessage(name, "*lag");
            }
        }
    }

}
