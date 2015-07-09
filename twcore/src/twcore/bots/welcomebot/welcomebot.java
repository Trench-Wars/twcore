package twcore.bots.welcomebot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
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
import twcore.core.events.ArenaJoined;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.SQLResultEvent;
import twcore.core.game.Player;
import twcore.core.util.Tools;
import twcore.core.util.ipc.IPCMessage;

/**
 * WelcomeBot is a player tracking bot designed specifically for identifying and monitoring
 * players new to the game. Stores tracking data in the database each time an identified new player
 * logs on creating a new record for each login. Upon detection of a new player an alert is issued
 * to staff which allows them to handle the call accordingly. Prepared statements are used for most
 * queries for ease of use and efficiency. Identification of new players relies on information
 * previously recorded by the stats module of guardhub. The last function of welcome bot is regulating
 * the new player tutorial objons for automatic use upon detection and staff. 
 *
 * @author WingZero
 */
public class welcomebot extends SubspaceBot {

    private static final int MAX_STRING = 220;
    private static final String IPC_CHANNEL = "Zone Channel";
    private static final String web = "website";
    private static final String db = "welcome";

    private BotSettings cfg;
    private OperatorList ops;

    private TreeSet<String> vets; // Veterans ignore list of previously checked players found not to be new
    private TreeSet<String> trainers; // Trainer aliases to waiting to be triggered
    private TreeSet<String> grantedOps; // List of people allowed to control bot when not smod
    private TreeMap<String, Session> sessions; // All currently tracked playing new players
    private TreeMap<String, Boolean> trusted; // Trusted players and staff for new player login notifications

    private InfoQueue infoer;

    private TreeMap<String, AliasCheck> aliases;
    private TreeMap<String, Integer[]> loopCatcher;
    private TreeMap<String, ObjonTimer> objonTimers;
    private TreeMap<String, Stack<String[]>> tutorials;

    // PS's called to welcome db
    private PreparedStatement psGetTrusted;
    private PreparedStatement psAddTrusted; // Add trusted player
    private PreparedStatement psRemTrusted; // Remove trusted player
    private PreparedStatement psSetAlerting; // Set alerting status
    private PreparedStatement psUpdatePlayer; // Creates or updates a NewPlayer record
    private PreparedStatement psUpdateSession; // Used to create a session record
    private PreparedStatement psGetCountryCode; // Gets country code using IP
    private PreparedStatement psGetSessionCount; // Gets current session count
    private PreparedStatement psGetReferral; // Retrieves the referral ID for a given person.
    private PreparedStatement psAddReferral; // Adds a player to the referral database.

    // PS's called to website db
    private PreparedStatement psInsertNewb; // Creates a new newbcall alert entry
    private PreparedStatement psCheckAlerts; // Checks for prior newb call listing

    private String infoee;
    private String einfoee;
    private TreeSet<String> debuggers;
    private boolean DEBUG;
    private boolean ready; // Prevents bot from operating outside of pub

    public welcomebot(BotAction botAction) {
        super(botAction);

        cfg = ba.getBotSettings();
        ops = ba.getOperatorList();

        EventRequester er = ba.getEventRequester();
        er.request(EventRequester.MESSAGE);
        er.request(EventRequester.LOGGED_ON);
        er.request(EventRequester.ARENA_JOINED);
        er.request(EventRequester.PLAYER_DEATH);
        er.request(EventRequester.PLAYER_ENTERED);
        er.request(EventRequester.PLAYER_LEFT);
    }

    private void init() {
        ready = false;
        infoee = null;
        DEBUG = false;

        debuggers = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        vets = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        trusted = new TreeMap<String, Boolean>(String.CASE_INSENSITIVE_ORDER);
        trainers = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        grantedOps = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        sessions = new TreeMap<String, Session>(String.CASE_INSENSITIVE_ORDER);
        aliases = new TreeMap<String, AliasCheck>(String.CASE_INSENSITIVE_ORDER);
        loopCatcher = new TreeMap<String, Integer[]>(String.CASE_INSENSITIVE_ORDER);
        objonTimers = new TreeMap<String, ObjonTimer>(String.CASE_INSENSITIVE_ORDER);
        tutorials = new TreeMap<String, Stack<String[]>>(String.CASE_INSENSITIVE_ORDER);

        infoer = new InfoQueue(3);

        createAllPrepareds();

        if (psSetAlerting == null || psGetTrusted == null || psAddTrusted == null || psRemTrusted == null || psInsertNewb == null
                || psGetSessionCount == null || psGetCountryCode == null || psCheckAlerts == null || psUpdateSession == null
                || psUpdatePlayer == null || psGetReferral == null || psAddReferral == null) {
            ba.sendChatMessage("Disconnecting due to null prepared statement(s)...");
            Tools.printLog("WelcomeBot: One or more PreparedStatements are null! Bot disconnecting.");
            ba.die("null prepared statement");
            return;
        }
        loadTrusted();
    }

    private void createAllPrepareds() {
        psInsertNewb = ba.createPreparedStatement(web, db, "INSERT INTO tblCallNewb (fcUserName, fdCreated) VALUES (?, NOW())");
        psCheckAlerts = ba.createPreparedStatement(web, db, "SELECT * FROM tblCallNewb WHERE fcUserName = ?");

        psGetTrusted = ba.createPreparedStatement(db, db, "SELECT fcPlayerName, fbAlerting FROM tblTrustedPlayers");
        psAddTrusted = ba.createPreparedStatement(db, db, "INSERT INTO tblTrustedPlayers (fcPlayerName, fcAddedBy) VALUES(?,?)");
        psRemTrusted = ba.createPreparedStatement(db, db, "DELETE FROM tblTrustedPlayers WHERE fcPlayerName = ?");
        psSetAlerting = ba.createPreparedStatement(db, db, "UPDATE tblTrustedPlayers SET fbAlerting = ? WHERE fcPlayerName = ?");
        psGetSessionCount = ba.createPreparedStatement(db, db, "SELECT COUNT(*) FROM tblNewPlayerSession WHERE fcName = ?");
        psGetCountryCode = ba.createPreparedStatement(db, db, "SELECT country_code3 FROM tblCountryIPs WHERE INET_ATON(?) >= ip_from AND INET_ATON(?) <= ip_to");
        psUpdateSession = ba.createPreparedStatement(db, db, "INSERT INTO tblNewPlayerSession (fcName, ffSessionUsage, fnKills, fnDeaths, fcResolution) VALUES(?,?,?,?,?)");
        psUpdatePlayer = ba.createPreparedStatement(db, db, "INSERT INTO tblNewPlayer "
                + "(fcName, fcSquad, ffTotalUsage, fcIP, fnMID, fcCountryCode, fdNameCreated, fcResolution) "
                + "VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE fcSquad=VALUES(fcSquad), ffTotalUsage=VALUES(ffTotalUsage), fcIP=VALUES(fcIP), fnMID=VALUES(fnMID), fcCountryCode=VALUES(fcCountryCode), fcResolution=VALUES(fcResolution)");

        psGetReferral = ba.createPreparedStatement(db, db, "SELECT fnReferralid FROM tblReferral WHERE fcReferralName = ?");
        psAddReferral = ba.createPreparedStatement(db, db, "INSERT INTO tblReferral (fcReferralName) VALUES (?)");
    }

    private void closeAllPrepareds() {
        ba.closePreparedStatement(web, db, psInsertNewb);
        ba.closePreparedStatement(web, db, psCheckAlerts);

        ba.closePreparedStatement(db, db, psGetTrusted);
        ba.closePreparedStatement(db, db, psAddTrusted);
        ba.closePreparedStatement(db, db, psRemTrusted);
        ba.closePreparedStatement(db, db, psSetAlerting);
        ba.closePreparedStatement(db, db, psUpdatePlayer);
        ba.closePreparedStatement(db, db, psUpdateSession);
        ba.closePreparedStatement(db, db, psGetCountryCode);
        ba.closePreparedStatement(db, db, psGetSessionCount);
        ba.closePreparedStatement(db, db, psAddReferral);
        ba.closePreparedStatement(db, db, psGetReferral);
    }

    private void loadTrusted() {
        trusted.clear();
        grantedOps.clear();
        String[] gops = cfg.getString("GrantedOps").trim().split(",");
        for (String op : gops)
            grantedOps.add(op);
        try {
            ResultSet rs = psGetTrusted.executeQuery();
            while (rs != null && rs.next())
                trusted.put(rs.getString(1), rs.getBoolean(2));
            if (DEBUG) {
                String msg = "Trusted(" + trusted.size() + "): ";
                for (String n : trusted.keySet())
                    msg += n + ", ";
                msg = msg.substring(0, msg.length() - 2);
                debug(msg);
                msg = "GrantedOps(" + grantedOps.size() + "): ";
                for (String n : grantedOps)
                    msg += n + ", ";
                msg = msg.substring(0, msg.length() - 2);
                debug(msg);
            }
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }

    public void handleEvent(LoggedOn event) {
        init();
        ba.joinArena(cfg.getString("InitialArena"));
        ready = true;
        ba.ipcSubscribe(IPC_CHANNEL);
        ba.sendUnfilteredPublicMessage("?chat=newplayer,robodev,staff,training");
        ba.changeArena("0");
    }

    public void handleEvent(ArenaJoined event) {
        debug("ArenaJoined: " + ba.getArenaName());
        ba.receiveAllPlayerDeaths();
    }

    /**
     * Takes entering players and checks for new player call record. If there is no
     * found record then new player detection commences. Otherwise, if the alert was genuine
     * then player tracking is started.
     */
    public void handleEvent(PlayerEntered event) {
        if (!ready)
            return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (trainers.remove(name))
            ba.sendAlertMessage("newplayer", name);
        else if (name == null || vets.contains(name) || ops.isBotExact(name))
            return;
        else {
            try {
                int taken = -1;
                if (psCheckAlerts.isClosed()) { // For some reason, this PS was getting closed, causing exceptions.
                    Tools.printLog("PreparedStatement attempting to be accessed in PlayerEntered event that is already closed. ?message WingZero or qan");
                    return;
                }
                psCheckAlerts.clearParameters();
                psCheckAlerts.setString(1, name);
                ResultSet rs = psCheckAlerts.executeQuery();
                if (rs != null && rs.next())
                    taken = rs.getInt("fnTaken");
                // 0 is missed, 1 is taken
                if (taken < 0) {
                    // no record so move on to detection
                    infoer.add(name);
                } else if (taken < 3) {
                    debug("Taken for " + name + ": " + taken);
                    if (taken == 0) {
                        ba.SQLBackgroundQuery(web, null, "UPDATE tblCallNewb SET ftUpdated = NOW() WHERE fnAlertID = " + rs.getInt("fnAlertID"));
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
        if (!ready)
            return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (sessions.containsKey(name))
            sessions.get(name).endSession();
        tutorials.remove(name);
    }

    public void handleEvent(PlayerDeath event) {
        if (!ready)
            return;
        String killer = ba.getPlayerName(event.getKillerID());
        if (killer != null && sessions.containsKey(killer))
            sessions.get(killer).addKill();
        String killed = ba.getPlayerName(event.getKilleeID());
        if (killed != null && sessions.containsKey(killed))
            sessions.get(killed).addDeath();
    }

    /**
     * IPC messages are exchanged with RoboHelp for falsed/un-falsed newb calls
     * and trainer alert triggers.
     */
    public void handleEvent(InterProcessEvent event) {
        if (!ready)
            return;
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
                                        + "FROM `tblAlias` INNER JOIN `tblUser` ON `tblAlias`.fnUserID = `tblUser`.fnUserID "
                                        + "WHERE fcUserName = '" + Tools.addSlashes(aliasIP) + "'");
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
                                        + "FROM `tblAlias` INNER JOIN `tblUser` ON `tblAlias`.fnUserID = `tblUser`.fnUserID "
                                        + "WHERE fcUserName = '" + Tools.addSlashes(aliasMID) + "'");
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
                    ba.SQLBackgroundQuery(web, "alias:final:" + name, "SELECT * "
                            + "FROM `tblAlias` INNER JOIN `tblUser` ON `tblAlias`.fnUserID = `tblUser`.fnUserID " + "WHERE fnIP IN "
                            + alias.getIpResults() + " " + "AND fnMachineID IN " + alias.getMidResults() + " ORDER BY fdUpdated DESC");
                }
            }
            ba.SQLClose(event.getResultSet());
        }
    }

    public void handleEvent(Message event) {
        String name = event.getMessager();
        ;
        if (name == null)
            name = ba.getPlayerName(event.getPlayerID());
        String msg = event.getMessage();
        String cmd = msg.toLowerCase();
        int type = event.getMessageType();

        if (type == Message.ARENA_MESSAGE)
            handleArenaMessage(msg);
        if (type == Message.PUBLIC_MESSAGE) {
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
        }
        if (type == Message.REMOTE_PRIVATE_MESSAGE || type == Message.PRIVATE_MESSAGE) {
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
            else if (cmd.equals("!link"))
                cmd_displayLink(name);
            else if (cmd.startsWith("!createrefid"))
                cmd_createReferral(name, msg);

            if (trusted.containsKey(name)) {
                if (cmd.equals("!alert"))
                    cmd_alert(name);
            }

            if (trusted.containsKey(name) || ops.isZH(name)) {
                if (cmd.startsWith("!newplayer "))
                    cmd_newplayer(name, msg);
                else if (cmd.startsWith("!next "))
                    cmd_next(name, msg);
                else if (cmd.equals("!list"))
                    cmd_list(name);
            }
            // Staff Commands
            if (ops.isZH(name)) {
                if (cmd.startsWith("!where "))
                    cmd_where(name, msg);
                else if (cmd.startsWith("!end "))
                    cmd_end(name, msg);
            }

            if (ops.isSmod(name)) {
                if (cmd.equals("!die"))
                    cmd_die(name, true);
                else if (cmd.equals("!kill"))
                    cmd_die(name, false);
                else if (cmd.startsWith("!addop "))
                    cmd_addOp(name, msg);
                else if (cmd.startsWith("!remop "))
                    cmd_remOp(name, msg);
                else if (cmd.startsWith("!go "))
                    cmd_go(name, msg);
                else if (cmd.startsWith("!listo"))
                    cmd_listOps(name);
            }
            if (ops.isSmod(name) || grantedOps.contains(name)) {
                if (cmd.equals("!debug"))
                    cmd_debug(name);
                else if (cmd.equals("!dome"))
                    cmd_doMe(name);
                else if (cmd.startsWith("!trust "))
                    cmd_addTrusted(name, msg);
                else if (cmd.startsWith("!untrust "))
                    cmd_remTrusted(name, msg);
                else if (cmd.equals("!trusted"))
                    cmd_trusted(name);
                else if (cmd.startsWith("!trusted "))
                    cmd_trusted(name, msg);
                else if (cmd.startsWith("!period "))
                    cmd_setPeriod(name, msg);
            }

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
            if (infoee != null && sessions.containsKey(infoee)) {
                sessions.get(infoee).setUsage(message);
                einfoee = infoee;
                ba.sendUnfilteredPrivateMessage(einfoee, "*einfo");
            } else {
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
                            //System.out.println("[ALIAS] " + alias.getName() + " in array already.");
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
        } else if (message.contains("Res: ") && einfoee != null && sessions.containsKey(einfoee)) {
            sessions.get(einfoee).setRes(message);
        }
    }

    private void cmd_alert(String name) {
        if (!trusted.containsKey(name))
            return;
        boolean b = !trusted.get(name);
        trusted.put(name, b);
        ba.sendSmartPrivateMessage(name, "New player alerts: " + (b ? "ENABLED" : "DISABLED"));

        try {
            psSetAlerting.setBoolean(1, b);
            psSetAlerting.setString(2, name);
            psSetAlerting.execute();
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }

    private void cmd_where(String name, String msg) {
        if (msg.length() < 8)
            return;
        msg = msg.substring(msg.indexOf(" ") + 1);
        Player p = ba.getPlayer(msg);
        if (p == null)
            p = ba.getFuzzyPlayer(msg);

        if (p != null) {
            Player s = ba.getPlayer(name);
            long x = (long) Math.floor(((double) p.getXTileLocation()) / 51.2); // Break it up into coords (1024 / 51.2 = 20)
            long y = (long) (Math.floor(((double) p.getYTileLocation()) / 51.2)) + 1; // Don't forget to carry the 0!
            char xchar = (char) (65 + x);
            String str = msg + " @ " + xchar + y + " (" + p.getXTileLocation() + "," + p.getYTileLocation() + ")";
            if (s != null)
                str += "   [YOU: (" + s.getXTileLocation() + "," + s.getYTileLocation() + ")]";
            ba.sendPrivateMessage(name, str);
        } else
            ba.sendPrivateMessage(name, msg + ": NOT FOUND");
    }

    private void cmd_help(String name) {

        ArrayList<String> msgs = new ArrayList<String>();
        msgs.add("+-- Welcome Bot Commands --------------------------------------------------------.");
        msgs.add("| !createRefID          -- Create a referral ID.                                 |");
        msgs.add("| !link                 -- Displays your referral link.                          |");
        if (trusted.containsKey(name))
            msgs.add("| !alert                -- Toggles new player alert messages.                    |");
        if (trusted.containsKey(name) || ops.isZH(name))
            msgs.add("| !newplayer <name>     -- Sends new player helper objon to <name>.              |");
        msgs.add("| !next <name>          -- Sends the next helper objon to <name>.                |");
        msgs.add("| !list                 -- Lists currently active sessions (new players).        |");
        if (ops.isZH(name)) {
            msgs.add("|  ~ZH~                                                                         -+");
            msgs.add("| !where <name>         -- Gives the current coordinates for <name> if possible. |");
            msgs.add("| !end <name>           -- Removes all objons for <name>.                        |");
        }

        if (ops.isSmod(name)) {
            msgs.add("+- ~SMOD~                                                                       -+");
            msgs.add("| !createRefID <name>   -- Creates a referral ID for <name>.                     |");
            msgs.add("| !listops              -- Lists granted operators.                              |");
            msgs.add("| !addop <name>         -- Grants operator priveledge to <name>.                 |");
            msgs.add("| !remop <name>         -- Removes operator priveledge for <name>.               |");
            msgs.add("| !go <arena>           -- Moves bot to <arena>.                                 |");
            msgs.add("| !kill                 -- Disconnects bot WITHOUT saving active sessions.       |");
            msgs.add("| !die                  -- Saves active sessions then disconnects.               |");
        }

        if (ops.isSmod(name) || grantedOps.contains(name)) {
            msgs.add("+- ~Ops~                                                                        -+");
            msgs.add("| !debug                -- Enables or disables debug messages being sent to you. |");
            msgs.add("| !trust <name>         -- Adds <name> to the trusted players list.              |");
            msgs.add("| !untrust <name>       -- Removes <name> from the trusted players list.         |");
            msgs.add("| !trusted              -- Lists trusted players.                                |");
            msgs.add("| !trusted <name>       -- Checks to see if <name> is on the trusted list.       |");
            msgs.add("| !period <seconds>     -- Sets the delay between *info commands to <seconds>.   |");
        }
        msgs.add("`--------------------------------------------------------------------------------'");
        ba.smartPrivateMessageSpam(name, msgs.toArray(new String[msgs.size()]));
    }

    private void cmd_setPeriod(String name, String msg) {
        if (msg.length() < 9)
            return;
        msg = msg.substring(msg.indexOf(" ") + 1);
        try {
            int p = Integer.valueOf(msg);
            if (p > 0 && p < 120) {
                infoer.setPeriod(p);
                ba.sendSmartPrivateMessage(name, "Set delay between info commands to: " + p + " sec");
            } else
                throw new Exception();
        } catch (Exception e) {
            ba.sendSmartPrivateMessage(name, "Failed to set info queue delay. No changes made.");
        }
    }

    private void cmd_listOps(String name) {
        String go = "";
        for (String n : grantedOps)
            go += n + ",";
        go = go.substring(0, go.lastIndexOf(","));
        ba.sendSmartPrivateMessage(name, go);

    }

    private void cmd_addOp(String name, String msg) {
        if (msg.length() < 8)
            return;
        msg = msg.substring(msg.indexOf(" ") + 1);
        if (grantedOps.add(msg)) {
            ba.sendSmartPrivateMessage(name, "Added to granted operators list: " + msg);
            String go = "";
            for (String n : grantedOps)
                go += n + ",";
            go = go.substring(0, go.lastIndexOf(","));
            cfg.put("GrantedOps", go);
            cfg.save();
        } else
            ba.sendSmartPrivateMessage(name, "Already in granted operators list: " + msg);
    }

    private void cmd_remOp(String name, String msg) {
        if (msg.length() < 8)
            return;
        msg = msg.substring(msg.indexOf(" ") + 1);
        if (grantedOps.remove(msg)) {
            ba.sendSmartPrivateMessage(name, "Removed from granted operators list: " + msg);
            String go = "";
            for (String n : grantedOps)
                go += n + ",";
            go = go.substring(0, go.lastIndexOf(","));
            cfg.put("GrantedOps", go);
            cfg.save();
        } else
            ba.sendSmartPrivateMessage(name, "Not found in granted operators list: " + msg);
    }

    private void cmd_list(String name) {
        String msgs = "Sessions: ";
        for (String n : sessions.keySet())
            msgs += n + ", ";
        msgs = msgs.substring(0, msgs.length() - 2);
        ba.sendSmartPrivateMessage(name, msgs);
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

    private void cmd_trusted(String name) {
        String msg = "Trusted(" + trusted.size() + "): ";
        for (String n : trusted.keySet())
            msg += n + ", ";
        msg = msg.substring(0, msg.length() - 2);
        ba.smartPrivateMessageSpam(name, splitString(msg));
    }

    private void cmd_trusted(String name, String msg) {
        if (msg.length() < 10)
            return;
        msg = msg.substring(msg.indexOf(" ") + 1);
        if (trusted.containsKey(msg))
            ba.sendSmartPrivateMessage(name, msg + " is a trusted player.");
        else
            ba.sendSmartPrivateMessage(name, msg + " was NOT found.");
    }

    private void cmd_addTrusted(String name, String msg) {
        if (msg.length() < 8)
            return;
        String p = msg.substring(msg.indexOf(" ") + 1);
        if (trusted.containsKey(p))
            ba.sendSmartPrivateMessage(name, "Player already trusted.");
        else {
            ba.sendSmartPrivateMessage(name, "Trusted player added: " + p);
            trusted.put(p, true);
            try {
                psAddTrusted.setString(1, p);
                psAddTrusted.setString(2, name);
                psAddTrusted.execute();
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
        }
    }

    private void cmd_remTrusted(String name, String msg) {
        if (msg.length() < 10)
            return;
        String p = msg.substring(msg.indexOf(" ") + 1);
        if (!trusted.containsKey(p))
            ba.sendSmartPrivateMessage(name, "Player not found in trusted list.");
        else {
            ba.sendSmartPrivateMessage(name, "Trusted player removed: " + p);
            trusted.remove(p);
            try {
                psRemTrusted.setString(1, p);
                psRemTrusted.execute();
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
        }

    }

    private void cmd_helpTutorial(String sender) {
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
            objons.push(new String[] { "*objoff 2017", "*objon 2018", "*objoff 2019" });
            objons.push(new String[] { "*objoff 2016", "*objon 2017" });
            objons.push(new String[] { "*objoff 2015", "*objon 2016" });
            objons.push(new String[] { "*objoff 2014", "*objon 2015" });
            objons.push(new String[] { "*objoff 2013", "*objon 2014" });
            objons.push(new String[] { "*objoff 2012", "*objon 2013" });
            objons.push(new String[] { "*objoff 2011", "*objon 2012", "*objon 2019" });
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

    /*
     * Marketing referral commands.
     */
    /**
     * !createRefID [<name>]<br>
     * Creates a referral ID.
     * Name optional for SMod+
     * @param name Issuer of command.
     * @param command Command and optional parameters.
     */
    private void cmd_createReferral(String name, String command) {
        String referralName = name;
        boolean forSelf = true;

        // Check if the request is done on someone else's behalf.
        if (ba.getOperatorList().isSmod(name) && command.length() > 13) {
            referralName = command.substring(13);
            ba.sendSmartPrivateMessage(name, "Creating referral ID for " + referralName);
        }

        forSelf = name.equalsIgnoreCase(referralName);

        // It can take a bit sometimes, so let's inform them we're on it.
        if (forSelf) {
            ba.sendSmartPrivateMessage(name, "Generating your referral ID. One moment please..");
        }

        try {
            psGetReferral.clearParameters();
            psGetReferral.setString(1, referralName);

            ResultSet rs = psGetReferral.executeQuery();

            if (rs != null && rs.next()) {
                // Player already exists in database.
                if (forSelf) {
                    ba.sendSmartPrivateMessage(name, "Your referral ID already exists.");
                } else {
                    ba.sendSmartPrivateMessage(name, referralName + " already has a referral ID.");
                }

                displayLink(name, rs.getInt(1));

            } else {

                psAddReferral.clearParameters();
                psAddReferral.setString(1, referralName);
                psAddReferral.execute();

                ResultSet rs2 = psGetReferral.executeQuery();
                if (rs2 != null && rs2.next()) {
                    int refID = rs2.getInt(1);
                    if (forSelf) {
                        ba.sendSmartPrivateMessage(name, "Your referral ID is: " + refID);
                    } else {
                        ba.sendSmartPrivateMessage(name, "Created referral ID " + refID + " for " + referralName);
                    }

                    displayLink(name, refID);

                } else {
                    ba.sendSmartPrivateMessage(name, "Failed to create a referral ID. Please contact an SMod+");
                }

            }
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }

    /**
     * !link<br>
     * Displays referral link.
     * @param name Who to look up the link for.
     */
    private void cmd_displayLink(String name) {
        try {
            psGetReferral.clearParameters();
            psGetReferral.setString(1, name);

            ResultSet rs = psGetReferral.executeQuery();

            if (rs != null && rs.next()) {
                // Player already exists in database.
                displayLink(name, rs.getInt(1));
            } else {
                ba.sendSmartPrivateMessage(name, "Your name couldn't be found in the database. Please use '!createRefID' to create your unique referral ID.");
            }
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }

    /**
     * Creates the referral ID
     * @param name Who to send the message to.
     * @param refID The referral ID.
     */
    private void displayLink(String name, int refID) {
        String link = cfg.getString("ReferralLink");

        if (link.isEmpty()) {
            ba.sendSmartPrivateMessage(name, "An error occured while creating your referral link. Please contact a developer.");
            return;
        }

        ba.sendSmartPrivateMessage(name, "Referral Link: " + link.replace(";RID;", Integer.toString(refID)));
    }

    private void cmd_debug(String name) {
        if (DEBUG) {
            if (debuggers.remove(name)) {
                if (debuggers.isEmpty()) {
                    DEBUG = false;
                    ba.sendSmartPrivateMessage(name, "Debugger: DISABLED");
                } else
                    ba.sendSmartPrivateMessage(name, "Removed you from the debugger list.");
            } else {
                debuggers.add(name);
                ba.sendSmartPrivateMessage(name, "You have been added to the debugger list.");
            }
        } else {
            DEBUG = true;
            debuggers.add(name);
            ba.sendSmartPrivateMessage(name, "Debugger: ENABLED");
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
            ba.sendSmartPrivateMessage(name, "Saving sessions and disconnecting...");
            ba.sendChatMessage("Disconnect with save request: " + name);
            // its weird but avoids concurrent modifications
            Set<String> names = new TreeSet<String>();
            for (String n : sessions.keySet())
                names.add(n);
            for (String ses : names)
                sessions.get(ses).endSession();
            infoer.stop();
            ba.scheduleTask(new TimerTask() {
                public void run() {
                    ba.die();
                }
            }, 3000);
        } else {
            ba.sendSmartPrivateMessage(name, "Disconnecting without saving...");
            ba.sendChatMessage("Disconnect without save request: " + name);
            infoer.stop();
            ba.scheduleTask(new TimerTask() {
                public void run() {
                    ba.die();
                }
            }, 3000);
        }
    }

    /**
     * Sends notifications to all appropriate players regarding new player logins.
     * 
     * @param session
     */
    private void sendTrustedAlerts(Session session) {
        int c = session.getSessionCount();
        for (Entry<String, Boolean> e : trusted.entrySet())
            if (e.getValue())
                ba.sendSmartPrivateMessage(e.getKey(), "New player '" + session.getName() + "' has logged in now " + c + " times. Usage: "
                        + session.getUsage() + " hours");
        ba.sendChatMessage("New player '" + session.getName() + "' has logged in now " + c + " times. Usage: " + session.getUsage() + " hours");
        ba.sendChatMessage(4, "New player '" + session.getName() + "' has logged in now " + c + " times. Usage: " + session.getUsage() + " hours");
    }

    private void debug(String msg) {
        if (DEBUG)
            for (String debugger : debuggers)
                ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
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
        ba.SQLBackgroundQuery(web, "alias:ip:" + alias.getName(), "SELECT DISTINCT(fnIP) "
                + "FROM `tblAlias` INNER JOIN `tblUser` ON `tblAlias`.fnUserID = `tblUser`.fnUserID " + "WHERE fcUserName = '"
                + Tools.addSlashes(alias.getName()) + "'");
        ba.SQLBackgroundQuery(web, "alias:mid:" + alias.getName(), "SELECT DISTINCT(fnMachineID) "
                + "FROM `tblAlias` INNER JOIN `tblUser` ON `tblAlias`.fnUserID = `tblUser`.fnUserID " + "WHERE fcUserName = '"
                + Tools.addSlashes(alias.getName()) + "'");
    }

    String[] splitString(String str) {
        ArrayList<String> l = new ArrayList<String>();
        while (!str.isEmpty()) {
            String s = "";
            if (str.length() > MAX_STRING) {
                s = str.substring(0, MAX_STRING);
                str = str.substring(MAX_STRING);
            } else {
                s = str;
                str = "";
            }
            l.add(s);
        }
        return l.toArray(new String[l.size()]);
    }

    public void handleDisconnect() {
        ready = false; // To prevent any prepared statements from being accessed via events while dcing
        ba.ipcUnSubscribe(IPC_CHANNEL);
        closeAllPrepareds();
        ba.cancelTasks();
    }

    private class InfoQueue extends TimerTask {

        private QueueSet queue;
        private int newPeriod;

        public InfoQueue(int period) {
            queue = new QueueSet();
            this.newPeriod = -1;
            ba.scheduleTask(this, 0, period * Tools.TimeInMillis.SECOND);
        }

        public void run() {
            String name = queue.pop();
            if (name != null) {
                ba.sendUnfilteredPrivateMessage(name, "*info");
            } else if (newPeriod > 0) {
                stop();
                infoer = new InfoQueue(newPeriod);
            }
        }

        public void setPeriod(int p) {
            newPeriod = p;
        }

        public void add(String name) {
            queue.push(name);
        }

        public void stop() {
            ba.cancelTask(this);
            infoer = null;
        }
    }

    /**
     * Session holds all the information required for tracking and updates database.
     * Takes message lines from *info commands that extract usage and player information.
     *
     * @author WingZero
     */
    private class Session {

        DateFormat fromInfo = new SimpleDateFormat("M-d-yyyy HH:mm:ss");
        java.sql.Timestamp created;
        String name;
        String squad;
        String countryCode;
        String res = "";
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
                ba.closePreparedStatement(db, db, psGetSessionCount);
                psGetSessionCount = ba.createPreparedStatement(db, db, "SELECT COUNT(*) FROM tblNewPlayerSession WHERE fcName = ?");
                Tools.printStackTrace("Recreating session count PS in Session class constructor...", e);
            }
            infoer.add(name);
        }

        public String getName() {
            return name;
        }

        public double getUsage() {
            return totalUsage;
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
            if (totalUsage < 20)
                sendTrustedAlerts(this);
            try {
                java.util.Date temp = fromInfo.parse(args[2]);
                created = new Timestamp(temp.getTime());
            } catch (ParseException e) {
                Tools.printStackTrace(e);
            }
            debug("Usage setting for " + name + ": Total:" + totalUsage + " Session:" + sessionUsage + " Created: " + created.toString());
        }

        public void checkPing(String message) {
            if (sessionCount != 1 || totalUsage > 1)
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

        public void setRes(String message) {
            res = message.substring(message.indexOf("Res: ") + 4, message.indexOf("Client: ")).trim();
            doUpdatePlayer();
        }

        public void doUpdatePlayer() {
            try {
                psUpdatePlayer.clearParameters();
                psUpdatePlayer.setString(1, name);
                psUpdatePlayer.setString(2, squad);
                psUpdatePlayer.setDouble(3, totalUsage);
                if (ip == null || ip.equals(""))
                    psUpdatePlayer.setString(4, "?.?.?.?");
                else
                    psUpdatePlayer.setString(4, ip);
                psUpdatePlayer.setInt(5, mid);
                psUpdatePlayer.setString(6, countryCode);
                psUpdatePlayer.setTimestamp(7, created);
                psUpdatePlayer.setString(8, res);
                psUpdatePlayer.execute();
            } catch (SQLException e) {
                ba.closePreparedStatement(db, db, psUpdatePlayer);
                psUpdatePlayer = ba.createPreparedStatement(db, db, "INSERT INTO tblNewPlayer "
                        + "(fcName, fcSquad, ffTotalUsage, fcIP, fnMID, fcCountryCode, fdNameCreated, fcResolution) "
                        + "VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE fcSquad=VALUES(fcSquad), ffTotalUsage=VALUES(ffTotalUsage), fcIP=VALUES(fcIP), fnMID=VALUES(fnMID), fcCountryCode=VALUES(fcCountryCode), fcResolution=VALUES(fcResolution)");
                Tools.printStackTrace("Recreating session update PS in doUpdatePlayer...", e);
                // Ignoring this particular result (not calling recursively, for example)...
                // let's simply try not to die.
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
                ba.closePreparedStatement(db, db, psGetCountryCode);
                psGetCountryCode = ba.createPreparedStatement(db, db, "SELECT country_code3 FROM tblCountryIPs WHERE INET_ATON(?) >= ip_from AND INET_ATON(?) <= ip_to");
                Tools.printStackTrace("Recreating country code select PS in setInfo...", e);
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
            sessionUsage = Math.round((hours + mins / 60.0) * 100) / 100.0 + sessionUsage;
            try {
                psUpdateSession.clearParameters();
                psUpdateSession.setString(1, name);
                psUpdateSession.setDouble(2, sessionUsage);
                psUpdateSession.setInt(3, kills);
                psUpdateSession.setInt(4, deaths);
                psUpdateSession.setString(5, res);
                psUpdateSession.execute();
            } catch (SQLException e) {
                ba.closePreparedStatement(db, db, psUpdateSession);
                psUpdateSession = ba.createPreparedStatement(db, db, "INSERT INTO tblNewPlayerSession (fcName, ffSessionUsage, fnKills, fnDeaths, fcResolution) VALUES(?,?,?,?,?)");
                Tools.printStackTrace("Recreating session update PS in endSession...", e);
            }
            sessions.remove(name);
            debug("Session ending for " + name + ": SessionUsage:" + sessionUsage + " Kills:" + kills + " Deaths:" + deaths);
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
                    psInsertNewb.execute();
                    debug("Sending new player from alias check: " + name);
                    ba.sendAlertMessage("newplayer", name);
                    System.out.println(":YES");
                    Player p = ba.getPlayer(name);
                    if (p != null && !sessions.containsKey(name))
                        sessions.put(name, new Session(p));
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
