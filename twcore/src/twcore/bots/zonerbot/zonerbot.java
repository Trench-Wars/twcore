package twcore.bots.zonerbot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.SQLResultEvent;
import twcore.core.util.Tools;

/**
 * New ZonerBot
 * 
 * @author WingZero
 *
 */
public class zonerbot extends SubspaceBot {

    public BotAction ba;
    public OperatorList oplist;

    public static final String db = "website";
    public static final int ADVERT_DELAY = 10;
    public static final int READVERT_MAX = 2;
    public static final int EXPIRE_TIME = 5;
    public static final int EXTENSION = 2;
    public static final int MAX_RENEWAL = 3;

    public static final int MAX_LENGTH = 400;
    public static final int NATURAL_LINE = 200;
    public static final int LINE_LENGTH = 120;

    private boolean ZONE_ON_LOAD;

    private boolean DEBUG;
    private String debugger;

    private AdvertTimer advertTimer;
    private ExpireTimer expireTimer;

    private Vectoid<String, Advert> queue;
    private ArrayList<Periodic> periodic;
    private LinkedList<Advert> usedAdverts;
    private LinkedList<String> trainers;

    private String currentUser;

    public zonerbot(BotAction botAction) {
        super(botAction);
        ba = m_botAction;
        oplist = ba.getOperatorList();
        queue = new Vectoid<String, Advert>();
        periodic = new ArrayList<Periodic>();
        trainers = new LinkedList<String>();
        usedAdverts = new LinkedList<Advert>();
        currentUser = null;
        ba.getEventRequester().request(EventRequester.LOGGED_ON);
        ba.getEventRequester().request(EventRequester.MESSAGE);
        loadTrainers();
        DEBUG = false;
        debugger = "";
        ZONE_ON_LOAD = false;
        loadPeriodics();
    }

    /** Handles the LoggedOn event **/
    public void handleEvent(LoggedOn event) {
        ba.joinArena(ba.getBotSettings().getString("InitialArena"));
        ba.sendUnfilteredPublicMessage("?chat=robodev");
    }

    /** Handles the Message event **/
    public void handleEvent(Message event) {
        String name = event.getMessager();
        if (name == null || name.length() < 1)
            name = ba.getPlayerName(event.getPlayerID());
        String msg = event.getMessage();
        int type = event.getMessageType();

        if (type == Message.ALERT_MESSAGE && event.getAlertCommandType().equals("advert") && oplist.isER(name)) {
            if (msg.toLowerCase().contains("free"))
                cmd_free(name);
            else
                cmd_claim(name);
        }

        if (type == Message.ARENA_MESSAGE && currentUser != null && msg.startsWith("Not online, last seen")) {
            debug("currentUser " + currentUser + " is not online");
            removeMissing();
        }

        if (type == Message.REMOTE_PRIVATE_MESSAGE || type == Message.PRIVATE_MESSAGE) {
            if (oplist.isZH(name)) {
                if (msg.toLowerCase().equals("!status"))
                    cmd_status(name);
                else if (msg.toLowerCase().equals("!claim"))
                    cmd_claim(name);
                else if (msg.toLowerCase().equals("!free"))
                    cmd_free(name);
                else if (msg.toLowerCase().startsWith("!set "))
                    cmd_setAdvert(name, msg);
                else if (msg.toLowerCase().startsWith("!claim "))
                    cmd_claim(name, msg);
                else if (msg.toLowerCase().startsWith("!sound "))
                    cmd_setSound(name, msg);
                else if (msg.toLowerCase().startsWith("!view"))
                    cmd_view(name, msg);
                else if (msg.toLowerCase().startsWith("!advert") || msg.toLowerCase().startsWith("!adv"))
                    cmd_advert(name, msg);
                else if (msg.toLowerCase().startsWith("!readvert"))
                    cmd_readvert(name, msg);
                else if (msg.toLowerCase().equals("!renew"))
                    cmd_renew(name);
                else if (msg.toLowerCase().equals("!help"))
                    cmd_help(name);
                else if (msg.toLowerCase().startsWith("!hosted"))
                    cmd_hosted(name, msg);
                else if (msg.toLowerCase().startsWith("!arenas"))
                	cmd_arenas(name, msg);
            }
            if (oplist.isHighmod(name) || trainers.contains(name.toLowerCase())) {
                if (msg.toLowerCase().startsWith("!grant "))
                    cmd_grant(name, msg);
                else if (msg.toLowerCase().startsWith("!approve"))
                    cmd_approve(name, msg);
                else if (msg.toLowerCase().equals("!debug"))
                    cmd_debug(name);
                else if (msg.toLowerCase().equals("!die"))
                    cmd_die(name);
                else if (msg.toLowerCase().startsWith("!grants"))
                    cmd_grants(name, msg);
                else if (msg.toLowerCase().startsWith("!give "))
                    cmd_give(name, msg);
            }
            if (oplist.isSmod(name)) {
                if (msg.toLowerCase().startsWith("!add "))
                    cmd_add(name, msg);
                else if (msg.toLowerCase().startsWith("!remove "))
                    cmd_remove(name, msg);
                else if (msg.toLowerCase().equals("!ops"))
                    cmd_ops(name);
                else if (msg.toLowerCase().startsWith("!per "))
                    cmd_periodic(name, msg);
                else if (msg.toLowerCase().startsWith("!remper "))
                    cmd_removePeriodic(name, msg);
                else if (msg.toLowerCase().equals("!list"))
                    cmd_listPeriodics(name);
                else if (msg.toLowerCase().equals("!autozone"))
                    cmd_autoZone(name);
                else if (msg.toLowerCase().equals("!reload"))
                    cmd_reload(name);
            }
            if (oplist.isOwner(name)) {
                if (msg.toLowerCase().startsWith("!zone "))
                    cmd_zone(name, msg);
            }

        }
    }

    /** Handles ResultSet events created by the !hosted and !grants commands **/
    public void handleEvent(SQLResultEvent event) {
        String[] args = event.getIdentifier().split(":");
        if (args.length == 2) {
            String name = args[0];
            String hours = args[1];
            HashMap<String, Integer> events = new HashMap<String, Integer>();
            ResultSet rs = event.getResultSet();
            try {
                while (rs.next()) {
                    String en = rs.getString("fcEventName");
                    if (!events.containsKey(en.toLowerCase()))
                        events.put(en.toLowerCase(), 1);
                    else
                        events.put(en.toLowerCase(), events.get(en.toLowerCase()) + 1);
                }
            } catch (SQLException e) {
                Tools.printStackTrace("ZonerBot !hosted SQL error.", e);
            }
            ba.SQLClose(rs);
            if (events.size() > 0) {
                ba.sendSmartPrivateMessage(name, "Events hosted in the last " + hours + " hours: ");
                for (String str : events.keySet())
                    ba.sendSmartPrivateMessage(name, " " + padString(str, 15) + " " + events.get(str));
            } else
                ba.sendSmartPrivateMessage(name, "Events hosted in the last " + hours + " hours: none");
        } else if (args.length == 1) {
            String name = args[0];
            boolean total = false;
            if (name.startsWith("*")) {
                total = true;
                name = name.substring(1);
            }
            String granter = "";
            int grants = 0;
            String month = "";
            int year = 2011;
            ResultSet rs = event.getResultSet();
            try {
                if (rs.next()) {
                    if (!total)
                        granter = rs.getString("g");
                    grants = rs.getInt("c");
                    month = rs.getString("m");
                    year = rs.getInt("y");
                    if (grants > 0) {
                        if (!total)
                            ba.sendSmartPrivateMessage(name, "Total grants given by " + granter + " in " + month + ", " + year + ": " + grants);
                        else
                            ba.sendSmartPrivateMessage(name, "Total grants given in " + month + ", " + year + ": " + grants);
                    } else
                        ba.sendSmartPrivateMessage(name, "No records found matching the given parameters.");
                } else
                    ba.sendSmartPrivateMessage(name, "No records found matching the given parameters.");
            } catch (SQLException e) {
                ba.SQLClose(rs);
                ba.sendSmartPrivateMessage(name, "SQL Error!");
                Tools.printStackTrace("ZonerBot !hosted SQL error.", e);
            }
            ba.SQLClose(rs);
        }
    }

    /** Handles the !help command **/
    private void cmd_help(String name) {
        String[] msg = { ",-- ZonerBot Commands --------------------------------------------------------------------------.",
                "| !hosted                - Lists the hosted event counts for last 24 hours                      |",
                "| !hosted <hours>        - Lists the hosted event counts for last <hours> hours                 |",
                "| !status                - Reports your current advert status                                   |",
                "| !claim                 - Claims an advert by adding you to the advert queue                   |",
                "| !free                  - Releases your advert and removes you from the queue                  |",
                "| !set <message>         - Sets <message> as the advert message and must include ?go arena      |",
                "| !sound <#>             - Sets <#> as the sound to be used in the advert                       |",
                "| !view                  - Views your current advert message and sound                          |",
                "| !advert <message>      - (!adv)Auto-adverts using <message> unless <message> is found illegal |",
                "| !advert                - (!adv)Sends the zone message as set using the !set <message> command |",
                "| !readvert              - Sends a last call default advert for the arena in your !advert       |",
                "| !readvert <sub-event>  - Sends a readvert about <sub-event> for the arena in your !advert     |",
                "| !renew                 - Prolongs the expiration of the advert for an extra 2 minutes         |",
                "| !claim <arena>         - For ZHs who are allowed to host unsupervised in a certain arena      |", 
                "| !arenas                - Shows a ZH what arenas are available or shows a specified ZH         |" };
        ba.smartPrivateMessageSpam(name, msg);
        if (trainers.contains(name.toLowerCase()) || oplist.isSmod(name)) {
            msg = new String[] { "+-- ZonerBot Trainer Commands ------------------------------------------------------------------+",
                    "| !grant <name>          - Grants an advert to a ZH allowing them to do a zoner once approved   |",
                    "| !view <name>           - Views the current advert message and sound of <name>                 |",
                    "| !approve               - Allows the earliest ZH you granted an advert to zone                 |",
                    "| !approve <name>        - Allows <name> to zone the advert that was granted                    |",
                    "| !grants                - Displays the total number of grants given for this month             |",
                    "| !grants yyyy-MM        - Displays the total number of grants given for yyyy-MM                |",
                    "| !grants <name>         - Displays the total adverts granted by <name> this month              |",
                    "| !grants <name>:yyyy-MM - Displays total grants by <name> in month MM of year yyyy             |",
                    "| !give <ZH>:<Arena>     - Adds the arena and ZH who is allowed to host unsupervised.           |",};
            ba.smartPrivateMessageSpam(name, msg);
        }
        if (oplist.isSmod(name)) {
            msg = new String[] { "+-- ZonerBot Smod Commands ---------------------------------------------------------------------+",
                    "| !per <del>;<dur>;<msg> - Sets a periodic zoner to repeat every <del> min for <dur> hr  %%#    |",
                    "| !remper <index>        - Removes the periodic zoner at <index>                                |",
                    "| !list                  - List of the currently active periodic zoners                         |",
                    "| !ops                   - List of the current staff trainers                                   |",
                    "| !add <name>            - Adds <name> to the trainer list (allows zh advert granting)          |",
                    "| !remove <name>         - Removes <name> from the trainer list                                 |",
                    "| !autozone              - Toggle periodic zoners to instant-zone when loaded from database     |",
                    "| !reload                - Reloads all the periodic messages from the database                  |", };
            ba.smartPrivateMessageSpam(name, msg);
        }
        ba.sendSmartPrivateMessage(name, "`-----------------------------------------------------------------------------------------------+");
    }

    /** Handles the !reload command which cancels and clears the loaded periodics and then reloads the periodics from the db **/
    private void cmd_reload(String name) {
        loadPeriodics();
        ba.sendSmartPrivateMessage(name, "Periodic zone messages have been reloaded from database.");
    }

    /** Handles the owner command !zone <message> which simply zones a message **/
    private void cmd_zone(String name, String cmd) {
        if (cmd.length() < 7)
            return;
        String zone = cmd.substring(6);
        int sound = -1;
        int index = -1;
        if (zone.contains("%")) {
            index = zone.lastIndexOf("%");
            if (zone.length() - index > 4) {
                ba.sendSmartPrivateMessage(name, "The %sound must be at the very end of the message.");
                return;
            }
            try {
                sound = Integer.valueOf(zone.substring(index + 1));
            } catch (NumberFormatException e) {
                ba.sendSmartPrivateMessage(name, "Error extracting specified sound number. If using a % symbol else where and don't want sound, add %%-1 to the end.");
            }
            if (soundCheck(sound)) {
                zone = zone.substring(0, index);
                if (sound > -1)
                    ba.sendZoneMessage(zone, sound);
                else
                    ba.sendZoneMessage(zone);
            } else
                ba.sendSmartPrivateMessage(name, "Sound " + sound + " is prohibited from use.");
        } else
            ba.sendZoneMessage(zone);
    }
    
    /** Handles the !arenas command which lists the arenas available to a ZH not supervised */
    private void cmd_arenas(String name, String cmd) {
    	String zh = name;
    	if (cmd.contains(" ") && cmd.length() > 8)
    		zh = cmd.substring(cmd.indexOf(" ") + 1);
    	if (!oplist.isZHExact(zh)) {
    		ba.sendSmartPrivateMessage(name, zh + " is not a ZH.");
    		return;
    	}
    	ResultSet rs = null;
    	try {
    		rs = ba.SQLQuery(db, "SELECT fcUserName as s, fcArena as a FROM tblZHGrants WHERE fcZH = '" + Tools.addSlashesToString(zh) + "'");
    		if (rs.next()) {
    			do {
    				ba.sendSmartPrivateMessage(name, "" + padString(rs.getString("a"), 20) + " by " + rs.getString("s"));
    			} while (rs.next());
    		} else
    			ba.sendSmartPrivateMessage(name, "No arenas found for " + zh + ".");
    		ba.SQLClose(rs);
    	} catch (SQLException e) {
    		Tools.printStackTrace(e);
    		ba.SQLClose(rs);
    	}
    }

    /** Handles the give command which is used to give a ZH unsupervised access to host a certain arena */
    private void cmd_give(String name, String msg) {
    	String[] args = msg.substring(msg.indexOf(" ") + 1).split(":");
        if (args.length != 2)
            ba.sendSmartPrivateMessage(name, "Syntax format..Type it right please.");
        else {
            String zh = args[0];
            String arena = args[1];

            try {
                ba.SQLQueryAndClose("website", "INSERT INTO tblZHGrants(fcUserName, fcArena, fcZH, fdCreated) VALUES ('"
                        + Tools.addSlashesToString(name) + "', '" + Tools.addSlashesToString(arena) + "', '" + Tools.addSlashesToString(zh)
                        + "', NOW())");
                ba.sendSmartPrivateMessage(name, zh + " has now has been granted unsupervised access to advert in the arena " + arena);
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
        }
    }

    /** Handles a ZH claim request for a specific arena */
    private void cmd_claim(String name, String msg) {
        String msgs = msg.substring(msg.indexOf(" ") + 1);
        try {
            ResultSet result = ba.SQLQuery(db, "SELECT fcArena FROM tblZHGrants WHERE fcZH = '" + Tools.addSlashesToString(name)
                    + "' AND fcArena = '" + Tools.addSlashesToString(msgs) + "'");
            if (!result.next())
                ba.sendSmartPrivateMessage(name, "That arena has not been found to give you permission to host unsupervised.");
            else {
                if (!queue.containsKey(name)) {
                    debug("Queueing new Advert");
                    queue.put(name, new Advert(name));
                    if (queue.indexOfKey(name) == 0)
                        prepareNext();
                    else
                        sendQueuePosition(name);
                } else {
                    ba.sendSmartPrivateMessage(name, "You have already claimed an advert and cannot claim another until it is used.");
                    sendQueuePosition(name);
                }
            }
            ba.SQLClose(result);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }

    /** Handles the !add trainer command **/
    private void cmd_add(String name, String cmd) {
        if (cmd.length() < 5)
            return;
        String staff = cmd.substring(5).trim().toLowerCase();
        if (!trainers.contains(name)) {
            BotSettings settings = ba.getBotSettings();
            String ops = settings.getString("Trainers");

            if (ops.length() < 1)
                settings.put("Trainers", staff);
            else
                settings.put("Trainers", ops + "," + staff);
            ba.sendSmartPrivateMessage(name, "Trainer added: " + staff);
            settings.save();
            loadTrainers();
        } else
            ba.sendSmartPrivateMessage(name, "" + staff + " is already a trainer.");
    }

    /** Handles the !remove trainer command **/
    private void cmd_remove(String name, String cmd) {
        if (cmd.length() < 8)
            return;
        String staff = cmd.substring(8).trim().toLowerCase();
        if (trainers.contains(staff)) {
            BotSettings settings = ba.getBotSettings();
            String ops = "";
            for (String n : trainers)
                if (!n.equalsIgnoreCase(staff))
                    ops += n + ",";
            if (ops.length() > 1)
                ops = ops.substring(0, ops.length() - 1);
            settings.put("Trainers", ops);
            ba.sendSmartPrivateMessage(name, "Trainer removed: " + staff);
            settings.save();
            loadTrainers();
        } else
            ba.sendSmartPrivateMessage(name, "" + staff + " is not a trainer.");
    }

    /** Handles the !ops trainer list command **/
    private void cmd_ops(String name) {
        ba.sendSmartPrivateMessage(name, "Trainers: " + ba.getBotSettings().getString("Trainers"));
    }

    /** Changes periodic zoners auto-zone after being loaded **/
    private void cmd_autoZone(String name) {
        ZONE_ON_LOAD = !ZONE_ON_LOAD;
        if (ZONE_ON_LOAD)
            ba.sendSmartPrivateMessage(name, "Periodic zone on load: ENABLED");
        else
            ba.sendSmartPrivateMessage(name, "Periodic zone on load: DISABLED");
    }

    /** Handles the !hosted command **/
    private void cmd_hosted(String name, String cmd) {
        int hours = 24;
        try {
            if (cmd.length() > 7) {
                hours = Integer.valueOf(cmd.substring(8).trim());
                if (hours < 1 || hours > 48)
                    hours = 24;
            }
        } catch (NumberFormatException e) {
            hours = 24;
        }
        ba.SQLBackgroundQuery(db, "" + name + ":" + hours, "SELECT fcEventName FROM tblAdvert WHERE fdTime > DATE_SUB(NOW(), INTERVAL " + hours
                + " HOUR) LIMIT " + (hours * 6));
    }

    /** Handles the !grants <name> and !grants <name>:yyyy-MM commands **/
    private void cmd_grants(String name, String cmd) {
        String query = "SELECT COUNT(*) as c, fcGranter as g, MONTHNAME(fdTime) as m, YEAR(fdTime) as y";
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int month = Calendar.getInstance().get(Calendar.MONTH);
        if (cmd.equals("!grants")) {
            debug("1 " + name + ": " + cmd);
            // get total grants for this month
            query += " FROM tblAdvert WHERE YEAR(fdTime) = YEAR(NOW()) AND MONTH(fdTime) = MONTH(NOW()) AND fcGranter IS NOT NULL";
            ba.SQLBackgroundQuery(db, "*" + name, query);
            return;
        } else if (!cmd.contains(":")) {
            cmd = cmd.substring(8);
            if (cmd.length() == 7 && cmd.charAt(4) == '-') {
                debug("2 " + name + ": " + cmd);
                // !grants yyyy-MM total grants for that month
                try {
                    year = Integer.valueOf(cmd.substring(0, 4));
                    month = Integer.valueOf(cmd.substring(5));
                    if (year < 2002 || year > 9999)
                        year = Calendar.getInstance().get(Calendar.YEAR);
                    if (month < 1 || month > 12)
                        month = Calendar.getInstance().get(Calendar.MONTH);
                    query += " FROM tblAdvert WHERE YEAR(fdTime) = " + year + " AND MONTH(fdTime) = " + month + " AND fcGranter IS NOT NULL";
                    ba.SQLBackgroundQuery(db, "*" + name, query);
                } catch (NumberFormatException e) {
                    ba.sendSmartPrivateMessage(name, "Syntax error, please use !grants yyyy-MM");
                }
                return;
            } else {
                debug("3 " + name + ": " + cmd);
                // show grants given by granter using the current month and year
                String granter = cmd;
                query += " FROM tblAdvert WHERE YEAR(fdTime) = YEAR(NOW()) AND MONTH(fdTime) = MONTH(NOW()) AND fcGranter = '"
                        + Tools.addSlashesToString(granter) + "'";
            }
        } else {
            debug("4 " + name + ": " + cmd);
            cmd = cmd.substring(8);
            // show grants given by granter in year yyyy and month MM
            String granter = cmd.substring(0, cmd.indexOf(":"));
            try {
                if (cmd.lastIndexOf("-") == -1)
                    throw new NumberFormatException();
                year = Integer.valueOf(cmd.substring(cmd.indexOf(":") + 1, cmd.lastIndexOf("-")));
                month = Integer.valueOf(cmd.substring(cmd.lastIndexOf("-") + 1));
                if (year < 2002 || year > 9999)
                    year = Calendar.getInstance().get(Calendar.YEAR);
                if (month < 1 || month > 12)
                    month = Calendar.getInstance().get(Calendar.MONTH);
            } catch (NumberFormatException e) {
                ba.sendSmartPrivateMessage(name, "Syntax error, please use !grants <name> or !grants <name>:yyyy-MM");
                return;
            }
            query += " FROM tblAdvert WHERE YEAR(fdTime) = " + year + " AND MONTH(fdTime) = " + month + " AND fcGranter = '"
                    + Tools.addSlashesToString(granter) + "'";
        }
        ba.SQLBackgroundQuery(db, name, query);
    }

    /** Handles the !status command **/
    private void cmd_status(String name) {
        int i = queue.indexOfKey(name);
        if (i > 0 && advertTimer != null)
            ba.sendSmartPrivateMessage(name, "The current advert belongs to: " + queue.firstValue().getName() + " Next advert: "
                    + advertTimer.getTime());
        if (i > -1)
            sendQueuePosition(i);
        else
            ba.sendSmartPrivateMessage(name, "You have not claimed an advert.");
    }

    /** Handles the !claim command or ?advert **/
    private void cmd_claim(String name) {
        if (oplist.isZHExact(name)) {
            ba.sendSmartPrivateMessage(name, "You are not allowed to claim an advert. Have a trainer or high mod !grant you one instead.");
            return;
        }
        if (!queue.containsKey(name)) {
            debug("Queueing new Advert");
            queue.put(name, new Advert(name));
            if (queue.indexOfKey(name) == 0)
                prepareNext();
            else
                sendQueuePosition(name);
        } else {
            ba.sendSmartPrivateMessage(name, "You have already claimed an advert and cannot claim another until it is used.");
            sendQueuePosition(name);
        }
    }

    /** Handles the !grant command **/
    private void cmd_grant(String name, String cmd) {
        if (cmd.length() < 7 || !cmd.contains(" ")) {
            ba.sendSmartPrivateMessage(name, "Failed to evaluate command due to syntax error.");
            return;
        }
        String zh = cmd.substring(cmd.indexOf(" ") + 1);
        if (!oplist.isZHExact(zh)) {
            ba.sendSmartPrivateMessage(name, "Adverts can only be granted to ZHs. Use !claim instead.");
            return;
        }
        if (!queue.containsKey(zh)) {
            queue.put(zh, new Advert(zh, name));
            ba.sendSmartPrivateMessage(name, "Advert granted to " + zh + ".");
            ba.sendSmartPrivateMessage(zh, "You have been granted an advert by " + name + ". Advert must be approved before it can be used.");
            if (queue.indexOfKey(zh) == 0)
                prepareNext();
            else
                sendQueuePosition(zh);
        }
    }

    /** Handles the !free command or ?advert free **/
    private void cmd_free(String name) {
        if (queue.containsKey(name)) {
            Advert advert = queue.get(name);
            if (advert.getStatus() < Advert.ZONED) {
                int index = queue.indexOfKey(name);
                if (index == 0) {
                    queue.remove(0);
                    if (expireTimer != null && !expireTimer.hasExpired()) {
                        expireTimer.endNow();
                    }
                    prepareNext();
                } else {
                    queue.remove(index);
                    if (index < queue.size()) {
                        for (; index < queue.size(); index++)
                            sendQueuePosition(index);
                    }
                }
                ba.sendSmartPrivateMessage(name, "Advert freed.");
            } else
                ba.sendSmartPrivateMessage(name, "An advert must not have been used in order to free it.");
        } else
            ba.sendSmartPrivateMessage(name, "You have not claimed an advert.");
    }

    /** Handles the !set command **/
    private void cmd_setAdvert(String name, String cmd) {
        if (cmd.length() < 6 || !cmd.contains(" "))
            return;
        String msg = cmd.substring(cmd.indexOf(" ") + 1);
        if (queue.containsKey(name))
            ba.sendSmartPrivateMessage(name, queue.get(name).setAdvert(msg));
        else
            ba.sendSmartPrivateMessage(name, "You must have claimed or been granted an advert before you can set its message.");
    }

    /** Handles the !sound command **/
    private void cmd_setSound(String name, String cmd) {
        if (cmd.length() < 7 || !cmd.contains(" "))
            return;
        int sound = Advert.DEFAULT_SOUND;
        try {
            sound = Integer.parseInt(cmd.substring(cmd.indexOf(" ") + 1));
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(name, "The sound must be a number.");
            return;
        }
        if (queue.containsKey(name))
            ba.sendSmartPrivateMessage(name, queue.get(name).setSound(sound));
        else
            ba.sendSmartPrivateMessage(name, "You must have claimed or been granted an advert before you can set its sound.");
    }

    /** Handles the !view command as well as the !view <name> command **/
    private void cmd_view(String name, String cmd) {
        if (cmd.contains(" ") && cmd.length() > 6) {
            String adverter = cmd.substring(cmd.indexOf(" ") + 1);
            if (queue.containsKey(adverter)) {
                Advert advert = queue.get(adverter);
                ba.sendSmartPrivateMessage(name, advert.getMessage());
                ba.sendSmartPrivateMessage(name, "Sound: " + advert.getSound());
            } else
                ba.sendSmartPrivateMessage(name, adverter + " does not have an advert.");
        } else if (queue.containsKey(name)) {
            Advert advert = queue.get(name);
            ba.sendSmartPrivateMessage(name, advert.getMessage());
            ba.sendSmartPrivateMessage(name, "Sound: " + advert.getSound());
        } else
            ba.sendSmartPrivateMessage(name, "Advert was not found.");
    }

    /** Handles the !approve command with or without the zh name **/
    private void cmd_approve(String name, String cmd) {
        if (cmd.length() > 8 && cmd.contains(" ")) {
            String zh = cmd.substring(cmd.indexOf(" ") + 1);
            if (queue.containsKey(zh))
                ba.sendSmartPrivateMessage(name, queue.get(zh).approve());
            else
                ba.sendSmartPrivateMessage(name, "No advert found for " + zh + ".");
        } else if (cmd.length() == 8) {
            for (Advert advert : queue.values()) {
                if (advert.isGranter(name)) {
                    ba.sendSmartPrivateMessage(name, advert.approve());
                    break;
                }
            }
            ba.sendSmartPrivateMessage(name, "You have not granted any adverts.");
        } else
            ba.sendSmartPrivateMessage(name, "Failure to evaluate command syntax.");
    }

    /** Handles the !advert command which executes the current advert if possible **/
    private void cmd_advert(String name, String cmd) {
        if (queue.containsKey(name)) {
            if (queue.indexOfKey(name) > 0)
                sendQueuePosition(name);
            else if (advertTimer != null && !advertTimer.hasExpired())
                sendQueuePosition(name);
            else {
                Advert advert = queue.firstValue();

                if (advert.getStatus() < Advert.READY) {
                    if (!advert.isGranted()) {
                        if (!cmd.contains(" ") || (cmd.indexOf(" ") + 1) >= cmd.length())
                            ba.sendSmartPrivateMessage(name, "You have to set an advert message before you can use it.");
                        else {
                            String msg = cmd.substring(cmd.indexOf(" ") + 1);
                            advert.setAdvert(msg);
                            if (advert.getStatus() == Advert.READY)
                                cmd_advert(name, "!advert");
                            else
                                ba.sendSmartPrivateMessage(name, advert.setAdvert(msg));
                        }
                    } else
                        ba.sendSmartPrivateMessage(name, "Your advert must be set and approved before it can be used.");
                } else if (advert.getStatus() > Advert.READY)
                    ba.sendSmartPrivateMessage(name, "The advert has already been zoned.");
                else if (cmd.contains(" ") && cmd.indexOf(" ") >= cmd.length()) {
                    String msg = cmd.substring(cmd.indexOf(" ") + 1);
                    String res = advert.setAdvert(msg);
                    if (advert.getStatus() == Advert.READY && res.startsWith("Advert message changed"))
                        cmd_advert(name, "!advert");
                    else
                        ba.sendSmartPrivateMessage(name, advert.setAdvert(msg));
                } else {
                    advert.setStatus(Advert.ZONED);
                    String adv = advert.getMessage() + "-" + advert.getName();
                    if (adv.length() > NATURAL_LINE)
                        zoneMessageSpam(splitString(adv, LINE_LENGTH), advert.getSound());
                    else
                        ba.sendZoneMessage(adv, advert.getSound());

                    advertTimer = new AdvertTimer(ADVERT_DELAY);
                    ba.scheduleTask(advertTimer, ADVERT_DELAY * Tools.TimeInMillis.MINUTE);
                    if (expireTimer != null)
                        expireTimer.endNow();
                    storeAdvert(advert);
                    queue.remove(0);
                    usedAdverts.add(0, advert);
                    ba.sendSmartPrivateMessage(name, "A readvert will be available for " + advert.readvertTime() + ".");
                    prepareNext();
                }
            }
        } else
            ba.sendSmartPrivateMessage(name, "You have to claim an advert first before you can use it.");
    }

    /** Handles the !readvert command which sends a default zone message for a given arena **/
    private void cmd_readvert(String name, String cmd) {
        if (cmd.equals("!readvert"))
            cmd = null;
        else if (cmd.length() > (10 + 20)) {
            ba.sendSmartPrivateMessage(name, "Maximum character length for custom sub-event name is 20.");
            return;
        } else if (cmd.length() > 10 && cmd.contains(" "))
            cmd = cmd.substring(10);
        else
            cmd = null;
        if (queue.containsKey(name))
            ba.sendSmartPrivateMessage(name, "The initial advert must be used before readvert can be used.");
        else if (usedAdverts.isEmpty())
            ba.sendSmartPrivateMessage(name, "An advert must be claimed and used before a readvert can be used.");
        else if (!usedAdverts.getFirst().getName().equalsIgnoreCase(name))
            ba.sendSmartPrivateMessage(name, "No advert used by you is currently eligible for a readvert.");
        else {
            Advert advert = usedAdverts.getFirst();
            if (advert.getStatus() == Advert.ZONED && advert.canReadvert()) {
                advert.setStatus(Advert.DONE);
                String arena = advert.getArena().toUpperCase();
                String event;
                if (cmd != null)
                    event = cmd;
                else
                    event = arena;
                String zoners[] = { "Last call for " + event + "." + " Type ?go " + arena + " to play. -" + name,
                        "The event " + event + " is starting. Type ?go " + arena + " to play. -" + name };
                ba.sendZoneMessage(zoners[new Random().nextInt(zoners.length)], 1);
            } else {
                ba.sendSmartPrivateMessage(name, "Your last advert does not have a readvert available. It was used or expired.");
            }
        }
    }

    /** Handles the !renew command which if the advert hasn't been zoned or expired, prolongs the expire time **/
    private void cmd_renew(String name) {
        if (!queue.containsKey(name))
            ba.sendSmartPrivateMessage(name, "There is no advert available for renewal.");
        else if (queue.indexOfKey(name) != 0)
            sendQueuePosition(name);
        else if (advertTimer != null && !advertTimer.hasExpired())
            sendQueuePosition(0);
        else if (expireTimer != null && !expireTimer.hasExpired()) {
            if (expireTimer.canRenew())
                ba.sendSmartPrivateMessage(name, "Advert renewed. Remaining time: " + expireTimer.renewTime());
            else if (queue.size() > 1)
                ba.sendSmartPrivateMessage(name, "Renewals are only allowed if no one is waiting behind you.");
            else
                ba.sendSmartPrivateMessage(name, "No renewals available. Remaining time: " + expireTimer.getTime());
        } else
            ba.sendSmartPrivateMessage(name, "Advert has already expired.");
    }

    /** Handles the !per <delay>;<duration>;<message> command where delay minutes, and duration hours **/
    private void cmd_periodic(String name, String cmd) {
        // !per <delay>;<interval>;<message>
        if (cmd.length() < 9 || !cmd.contains(";"))
            return;
        cmd = cmd.substring(5);
        String[] args = cmd.split(";");
        int delay = 20;
        int duration = 24;
        try {
            delay = Integer.valueOf(args[0]);
            duration = Integer.valueOf(args[1]);
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(name, "Incorrect syntax, please use !per <delay>;<duration>;<message>");
            return;
        }
        String msg = args[2];
        if (msg.length() > MAX_LENGTH) {
            ba.sendSmartPrivateMessage(name, "Advert message must be less than " + MAX_LENGTH + " characters.");
            return;
        } else if (delay < 5) {
            ba.sendSmartPrivateMessage(name, "Delay must be greater than or equal to 5 minutes.");
            return;
        } else if (duration < 1) {
            ba.sendSmartPrivateMessage(name, "Duration must be greater than or equal to 1 hour.");
            return;
        }
        new Periodic(name, msg, delay, duration);
    }

    /** Handles the !list command which lists the active periodic zoners **/
    private void cmd_listPeriodics(String name) {
        if (periodic.isEmpty()) {
            ba.sendSmartPrivateMessage(name, "There are no periodic zoners currently set.");
            return;
        }
        for (Periodic p : periodic)
            ba.smartPrivateMessageSpam(name, splitString("" + p.index + ") " + p.toString(), LINE_LENGTH));
    }

    /** Handles the !remper <index> command which removes periodic zoner at index <index> as shown in !list **/
    private void cmd_removePeriodic(String name, String cmd) {
        // !remper #
        if (cmd.length() < 9)
            return;
        int index = -1;
        try {
            index = Integer.valueOf(cmd.substring(8));
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(name, "Invalid syntax, please use !remper <#>");
            return;
        }
        if (index > -1 && index < periodic.size()) {
            periodic.remove(index).end();
            updateIndices();
            ba.sendSmartPrivateMessage(name, "Periodic zoner #" + index + " has been removed.");
        } else
            ba.sendSmartPrivateMessage(name, "No periodic zoner listed at index: " + index);
    }

    /** Handles the !die command **/
    private void cmd_die(String name) {
        try {
            if (expireTimer != null)
                expireTimer.endNow();
            if (advertTimer != null && !advertTimer.hasExpired())
                ba.cancelTask(advertTimer);
        } catch (Exception e) {}
        ;
        ba.sendChatMessage("Logging off. Requested by: " + name);
        ba.scheduleTask(new Die(), 2000);
    }

    /** Removes the unused advert **/
    private void expireAdvert() {
        debug("Expiring current Advert");
        currentUser = null;
        Advert advert = queue.remove(0);
        if (advert != null && advert.getStatus() < Advert.ZONED) {
            ba.sendSmartPrivateMessage(advert.getName(), "Your advert has expired and has been removed from the queue.");
            prepareNext();
        }
    }

    /** Helper removes the current advert from the queue due to a missing adverter **/
    private void removeMissing() {
        debug("Removing Advert of offline person");
        if (queue.indexOfKey(currentUser) != 0)
            return;
        if (expireTimer != null)
            expireTimer.endNow();
        if (!queue.isEmpty())
            queue.remove(0);
        currentUser = null;
        prepareNext();
    }

    /** Helper sends appropriate queue position information to name **/
    private void sendQueuePosition(String name) {
        int i = queue.indexOfKey(name);
        if (i == 0) {
            if (expireTimer != null && !expireTimer.hasExpired())
                ba.sendSmartPrivateMessage(name, "It is your turn to use your advert. You have " + expireTimer.getTime() + " to use it.");
            else if (advertTimer != null && !advertTimer.hasExpired())
                ba.sendSmartPrivateMessage(name, "You are next in the advert queue. Time remaining: " + advertTimer.getTime());
        } else if (i > 0) {
            ba.sendSmartPrivateMessage(name, "There are " + i + " people currently ahead of you in the advert queue.");
        } else if (!usedAdverts.isEmpty() && usedAdverts.getFirst().getName().equalsIgnoreCase(name)) {
            Advert advert = usedAdverts.getFirst();
            if (advert.canReadvert())
                ba.sendSmartPrivateMessage(name, "Your readvert will be available for " + advert.readvertTime() + ".");
            else
                ba.sendSmartPrivateMessage(name, "No advert available.");
        } else
            ba.sendSmartPrivateMessage(name, "You have not claimed an advert.");
    }

    /** Helper sends appropriate queue position information to name front advert index **/
    private void sendQueuePosition(int index) {
        String name = queue.getKey(index);
        if (index == 0) {
            if (expireTimer != null && !expireTimer.hasExpired())
                ba.sendSmartPrivateMessage(name, "It is your turn to use your advert. It will expire in " + expireTimer.getTime() + ".");
            else if (advertTimer != null && !advertTimer.hasExpired())
                ba.sendSmartPrivateMessage(name, "You are next in the advert queue. Time remaining: " + advertTimer.getTime());
        } else if (index > 0)
            ba.sendSmartPrivateMessage(name, "There are " + index + " people currently ahead of you in the advert queue.");
    }

    /** Called when the first advert in the queue can be used. Starts the idle timer and informs queue positions. **/
    private void prepareNext() {
        debug("Prepare next called");
        if (!queue.isEmpty()) {
            if (advertTimer == null || (advertTimer != null && advertTimer.hasExpired())) {
                Advert advert = queue.get(0);
                expireTimer = new ExpireTimer(advert.getName(), EXPIRE_TIME);
                ba.scheduleTask(expireTimer, EXPIRE_TIME * Tools.TimeInMillis.MINUTE);
                currentUser = advert.getName();
                ba.sendUnfilteredPublicMessage("?find " + currentUser);
            }
            for (String n : queue.keySet())
                sendQueuePosition(n);
        }
    }

    /** Posts advert information to SQL database **/
    private void storeAdvert(Advert advert) {
        java.util.Date day = Calendar.getInstance().getTime();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(day);
        String arena = advert.getArena();
        if (!arena.equalsIgnoreCase("") && !arena.equalsIgnoreCase("elim") && !arena.equalsIgnoreCase("baseelim")
                && !arena.equalsIgnoreCase("tourny")) {
            String query = "";
            if (!advert.isGranted())
                query = "INSERT INTO `tblAdvert` (`fcUserName`, `fcEventName`, `fcAdvert`, `fdTime`) VALUES ('"
                        + Tools.addSlashesToString(advert.getName()) + "', '" + arena + "', '" + Tools.addSlashesToString(advert.getMessage())
                        + "', '" + time + "')";
            else
                query = "INSERT INTO `tblAdvert` (`fcUserName`, `fcGranter`, `fcEventName`, `fcAdvert`, `fdTime`) VALUES ('"
                        + Tools.addSlashesToString(advert.getName()) + "', '" + Tools.addSlashesToString(advert.granter) + "', '" + arena + "', '"
                        + Tools.addSlashesToString(advert.getMessage()) + "', '" + time + "')";
            try {
                ba.SQLBackgroundQuery(db, null, query);
            } catch (Exception e) {
                Tools.printLog("Could not insert advert record.");
            }
        }
    }

    /** Loads the trainers listed in the cfg file **/
    private void loadTrainers() {
        trainers.clear();
        String[] list = ba.getBotSettings().getString("Trainers").split(",");
        for (String n : list)
            trainers.add(n.toLowerCase());
    }

    /** Loads active periodic zoners from the database **/
    private void loadPeriodics() {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            for (Periodic p : periodic)
                ba.cancelTask(p);
            periodic.clear();
            ResultSet rs = ba.SQLQuery(db, "SELECT * FROM tblPeriodic");
            while (rs.next()) {
                long created = 0;
                try {
                    cal.setTime(f.parse(rs.getString("fdCreated")));
                    created = cal.getTimeInMillis();
                } catch (ParseException e) {
                    debug("Parse exception.");
                    continue;
                }
                final int delay = rs.getInt("fnDelay");
                final int duration = rs.getInt("fnDuration");
                final int sound = rs.getInt("fnSound");
                final int id = rs.getInt("fnMessageID");
                final String name = rs.getString("fcUserName");
                final String msg = rs.getString("fcMessage");
                final long c = created;
                new Periodic(id, name, msg, sound, delay, duration, c);
            }
            ba.SQLClose(rs);
        } catch (SQLException e) {
            Tools.printStackTrace("SQL periodic zoner loader failed.", e);
        }
    }

    /** Updates the index numbers for each periodic zoner **/
    private void updateIndices() {
        for (int i = 0; i < periodic.size(); i++)
            periodic.get(i).setIndex(i);
    }

    /** Returns a String with concatenated spaces to meet a certain length **/
    private String padString(String str, int length) {
        for (int i = str.length(); i < length; i++)
            str += " ";
        str = str.substring(0, length);
        return str;
    }

    /** Splits a string into a String array where each is of a certain length **/
    private String[] splitString(String msg, int length) {
        int pieces = msg.length() / length;
        if ((msg.length() % length) > 0)
            pieces++;
        String[] result = new String[pieces];
        for (int i = 0; i < pieces; i++) {
            if (i == 0)
                result[i] = msg.substring(0, length);
            else if (i < (pieces - 1))
                result[i] = msg.substring(i * length, i * length + length);
            else
                result[i] = msg.substring(i * length);
        }
        return result;
    }

    /** Returns true if the sound is legal **/
    private boolean soundCheck(int s) {
        return (s != Advert.REGAN_SOUND && s != Advert.SEX_SOUND && s != Advert.PM_SOUND && s != Advert.MUSIC1_SOUND && s != Advert.MUSIC2_SOUND);
    }

    /** Zones an array of messages but only using a sound on the first message unless there is none **/
    private void zoneMessageSpam(String[] msg, int sound) {
        for (int i = 0; i < msg.length; i++) {
            if (i == 0) {
                if (sound > -1)
                    ba.sendZoneMessage(msg[0], sound);
                else
                    ba.sendZoneMessage(msg[0]);
            } else
                ba.sendZoneMessage(msg[i]);
        }
    }

    /**
     * This is a representation of an advert claim containing all relevant information
     * related to the advert including methods and functions.
     * @author WingZero
     */
    private class Advert {

        String name;
        String granter;
        String advert;
        int sound;
        int status;
        boolean granted;
        long zoned;
        static final int APPROVE = 0;
        static final int READY = 1;
        static final int ZONED = 2;
        static final int DONE = 3;

        // sounds
        public static final int MAX_ADVERT_LENGTH = 400;
        public static final int DEFAULT_SOUND = 2;
        public static final int MIN_SOUND = 1;
        public static final int MAX_SOUND = 255;
        public static final int REGAN_SOUND = 6;
        public static final int SEX_SOUND = 12;
        public static final int PM_SOUND = 26;
        public static final int MUSIC1_SOUND = 100;
        public static final int MUSIC2_SOUND = 102;

        /**
         * New advert constructor
         * @param name Name of staff claimer
         */
        public Advert(String name) {
            this.name = name;
            granter = null;
            advert = null;
            sound = DEFAULT_SOUND;
            status = APPROVE;
            granted = false;
            zoned = -1;
        }

        /**
         * New granted advert constructor
         * @param name Name of ZH granted the advert
         * @param granter Name of staffer who granted
         */
        public Advert(String name, String granter) {
            this.name = name;
            this.granter = granter;
            advert = null;
            sound = DEFAULT_SOUND;
            status = APPROVE;
            granted = true;
            zoned = -1;
        }

        /** Returns claimer name **/
        public String getName() {
            return name;
        }

        /** Checks if the given name is the one who granted this advert **/
        public boolean isGranter(String g) {
            return (granted && g.equalsIgnoreCase(granter));
        }

        /** Returns if this was granted or not **/
        public boolean isGranted() {
            return granted;
        }

        /** Changes advert status variable **/
        public void setStatus(int nextStatus) {
            status = nextStatus;
            if (status == ZONED && zoned == -1)
                zoned = System.currentTimeMillis();
            debug("New status: " + status);
        }

        /** Advert message setter checks for any non-compliance and prepares for advert execution **/
        public String setAdvert(String str) {
            if (str == null || str.length() < 1)
                return "No advert specified.";
            if (str.length() > MAX_ADVERT_LENGTH)
                return "Advert must be less than " + MAX_ADVERT_LENGTH + " characters long.";
            if (!str.contains("?go "))
                return "Advert is required to have ?go ArenaName at the end or towards the end.";
            if (str.toLowerCase().contains("-" + name.toLowerCase()))
                return "Do not include a tag (-Name) at the end because it will be added automatically.";
            if (status == READY) {
                if (!granted) {
                    advert = str + " ";
                    return "Advert message changed and can be seen with !view.";
                } else {
                    advert = str + " ";
                    status = APPROVE;
                    ba.sendSmartPrivateMessage(granter, "" + name + " set an advert message and waits for approval.");
                    return "Advert message changed and must be approved again before it can be used.";
                }
            } else if (status == APPROVE) {
                if (!granted) {
                    advert = str + " ";
                    status = READY;
                    return "Advert message set and can now be seen with !view.";
                } else {
                    advert = str + " ";
                    status = APPROVE;
                    ba.sendSmartPrivateMessage(granter, "" + name + " changed the advert message and needs approval.");
                    return "Advert message set and can now be seen with !view. The advert must now be approved before it can be used.";
                }
            } else
                return "Advert has already been used.";
        }

        /** Sets the sound number to be used if legal **/
        public String setSound(int s) {
            if (s < MIN_SOUND || s > MAX_SOUND)
                return "Invalid, sound number must be between " + MIN_SOUND + " and " + MAX_SOUND + ".";
            if (status > READY)
                return "Advert has already been used.";
            if (soundCheck(s)) {
                sound = s;
                if ((granted && status == APPROVE) || (!granted && status < ZONED))
                    return "Advert sound set to " + s + ".";
                else {
                    unapprove();
                    return "Advert sound set to " + s + " and must be approved again before it can be used.";
                }
            } else
                return "Sound number " + s + " is not allowed.";
        }

        /** Sets advert to approved status **/
        public String approve() {
            if (granted && status == APPROVE) {
                status = READY;
                ba.sendSmartPrivateMessage(name, "Advert approved. Use !advert to zone your advert.");
                return "Advert approved for " + name + ".";
            } else
                return "Advert has already been approved.";
        }

        /** Reverts aproval status **/
        public void unapprove() {
            if (granted && status == READY)
                status = APPROVE;
        }

        /** Returns advert status **/
        public int getStatus() {
            return status;
        }

        // we will probably need to handle messages over length 120
        /** Returns set message text **/
        public String getMessage() {
            if (advert != null)
                return advert;
            else
                return "No advert message has been set.";
        }

        /** Returns sound number **/
        public int getSound() {
            return sound;
        }

        /** Returns arena as specified by ?go <arena> in message text **/
        public String getArena() {
            if (advert != null && advert.contains("?go ")) {
                String arena = advert.substring(advert.lastIndexOf("?go ") + 4);
                if (arena.contains(" "))
                    return arena.substring(0, arena.indexOf(" "));
                else
                    return arena;
            } else
                return "No arena found or advert not set.";
        }

        /** Checks if readvert has expired **/
        public boolean canReadvert() {
            return ((System.currentTimeMillis() - zoned) < (READVERT_MAX * Tools.TimeInMillis.MINUTE));
        }

        /** Returns remaining time until readvert expiration **/
        public String readvertTime() {
            long time = (READVERT_MAX * Tools.TimeInMillis.MINUTE) - (System.currentTimeMillis() - zoned);
            if (time > 0)
                return "" + (time / Tools.TimeInMillis.SECOND) + " seconds";
            else
                return "expired";
        }
    }

    /**
     * The AdvertTimer is used to put time between each set of event zoners. If it is running
     * then that means no one should be able to advert. Only time things move is when this expires.
     * @author WingZero
     */
    private class AdvertTimer extends TimerTask {

        int delay;
        long timestamp;
        boolean active;

        public AdvertTimer(int delayBetweenAdverts) {
            debug("New AdvertTimer created with delay: " + delay);
            delay = delayBetweenAdverts;
            timestamp = System.currentTimeMillis();
            active = true;
        }

        @Override
        public void run() {
            debug("AdvertTimer running run method");
            active = false;
            prepareNext();
        }

        /** Returns the remaining time until expiration **/
        public String getTime() {
            long left = ((delay * Tools.TimeInMillis.MINUTE) - (System.currentTimeMillis() - timestamp));
            if (left > 0) {
                int sec = (int) ((left / 1000) % 60);
                int min = (int) (((left / 1000) - sec) / 60);
                return "" + min + " minutes " + sec + " seconds";
            } else
                return null;
        }

        public boolean hasExpired() {
            return !active;
        }
    }

    /** 
     * This class is used to remove an advert after having not been used. 
     * Even after the advert has been used it will continue running to 
     * provide an expiration for the readvert.
     * @author WingZero
     */
    private class ExpireTimer extends TimerTask {

        int delay, renewal;
        long timestamp;
        boolean active;

        public ExpireTimer(String waiter, int expireDelay) {
            debug("New ExpireTimer created with delay: " + expireDelay);
            delay = expireDelay;
            renewal = 0;
            timestamp = System.currentTimeMillis();
            active = true;
        }

        /** Reconstructs the ExpireTimer in order to reschedule it **/
        public ExpireTimer(int expireDelay, int renewals, long time, boolean act) {
            debug("Renew ExpireTimer created with delay: " + expireDelay);
            delay = expireDelay;
            renewal = renewals;
            timestamp = time;
            active = act;
        }

        /** Returns the remaining time until expiration **/
        public String getTime() {
            return longString(((delay * Tools.TimeInMillis.MINUTE) - (System.currentTimeMillis() - timestamp)));
        }

        /** Converts a long of milliseconds into a String with minutes and seconds **/
        private String longString(long t) {
            if (t > 0) {
                int sec = (int) ((t / 1000) % 60);
                int min = (int) (((t / 1000) - sec) / 60);
                return "" + min + " minutes " + sec + " seconds";
            } else
                return null;
        }

        @Override
        public void run() {
            debug("ExpireTimer run method running");
            active = false;
            expireAdvert();
        }

        /** Executes expiration **/
        public void endNow() {
            if (active)
                ba.cancelTask(this);
            active = false;
        }

        /** Extends time until expiration by canceling itself and rescheduling appropriately **/
        public String renewTime() {
            if (renewal < MAX_RENEWAL) {
                endNow();
                active = true;
                renewal++;
                long time = (((delay * Tools.TimeInMillis.MINUTE) - (System.currentTimeMillis() - timestamp)) + (EXTENSION * Tools.TimeInMillis.MINUTE));
                delay += EXTENSION;
                expireTimer = new ExpireTimer(delay, renewal, timestamp, active);
                ba.scheduleTask(expireTimer, time);
                return longString(time);
            } else
                return "";
        }

        /** Checks if time extension is available **/
        public boolean canRenew() {
            return (renewal < MAX_RENEWAL) && (queue.size() < 2);
        }

        public boolean hasExpired() {
            return !active;
        }
    }

    /**
     * This class holds information for a periodic zoner 
     * which is a zone message that is set to repeat every interval
     * and expires once it reaches a set duration.
     * @author WingZero
     */
    private class Periodic extends TimerTask {

        String name, advert;
        int index, id, sound;
        int delay;
        int duration;
        long created;

        /**
         * Pre-made and reloaded periodic zone message class constructor
         * @param name Staff member creating zoner
         * @param msg Contents of the zone message
         * @param delay Time between each zoner in minutes
         * @param duration Lifetime from time of creation in hours
         */
        public Periodic(String name, String msg, int delay, int duration) {
            this.name = name;
            this.delay = delay;
            this.duration = duration;
            sound = -1;
            String res = setAdvert(msg);
            if (res != null) {
                ba.sendSmartPrivateMessage(name, res);
                return;
            }
            created = System.currentTimeMillis();
            create();
            if (id == -1)
                ba.sendSmartPrivateMessage(name, "There was an error in adding the advert to the database. As a result, it will not be reactivated if the bot should respawn.");
            index = periodic.size();
            periodic.add(index, this);
            if (ZONE_ON_LOAD)
                ba.scheduleTask(this, 1500, delay * Tools.TimeInMillis.MINUTE);
            else
                ba.scheduleTask(this, delay * Tools.TimeInMillis.MINUTE, delay * Tools.TimeInMillis.MINUTE);
        }

        /**
         * New periodic zone message constructor
         * @param name Staff member creating zoner
         * @param msg Contents of the zone message
         * @param delay Time between each zoner in minutes
         * @param duration Lifetime from time of creation in hours
         * @param id SQL row ID
         * @param sound Sound number extracted originally
         * @param created Time of original creation in milliseconds from epoch
         */
        public Periodic(int id, String name, String msg, int sound, int delay, int duration, long created) {
            this.id = id;
            this.name = name;
            this.delay = delay;
            this.duration = duration;
            this.sound = sound;
            String res = setAdvert(msg);
            if (res != null) {
                debug(res);
                return;
            }
            this.created = created;
            index = periodic.size();
            periodic.add(index, this);
            if (ZONE_ON_LOAD)
                ba.scheduleTask(this, 1500, delay * Tools.TimeInMillis.MINUTE);
            else
                ba.scheduleTask(this, delay * Tools.TimeInMillis.MINUTE, delay * Tools.TimeInMillis.MINUTE);
        }

        /** Sets zone message text after extracting the sound if specified and checks legality. Returns null if successful. **/
        public String setAdvert(String msg) {
            int si = msg.lastIndexOf("%");
            if (si < 0) {
                advert = msg;
                return null;
            } else if (msg.length() - si > 4)
                return "The %<sound> must be at the very end of the message.";
            else {
                try {
                    sound = Integer.valueOf(msg.substring(si + 1));
                } catch (NumberFormatException e) {
                    advert = msg.substring(0, si);
                    return "Error extracting percent sound so using the default %2.";
                }

                if (soundCheck(sound)) {
                    advert = msg.substring(0, si);
                    return null;
                } else
                    return "That sound is not allowed.";
            }
        }

        /** Creates a database entry for a new periodic zone message and sets/returns the SQL ID **/
        public int create() {
            if (delay > 0 && duration > 0 && advert.length() > 0) {
                try {
                    ba.SQLQueryAndClose(db, "INSERT INTO tblPeriodic (fcUserName, fcMessage, fnSound, fnDelay, fnDuration) VALUES('" + Tools.addSlashesToString(name) + "', '"
                            + Tools.addSlashesToString(advert) + "', " + sound + ", " + delay + ", " + duration + ")");
                    ResultSet rs = ba.SQLQuery(db, "SELECT LAST_INSERT_ID() as id");
                    if (rs.next())
                        id = rs.getInt("id");
                    ba.SQLClose(rs);
                } catch (SQLException e) {
                    Tools.printStackTrace("Periodic message SQL creation error.", e);
                    id = -1;
                    return id;
                }
                ba.sendSmartPrivateMessage(name, "A periodic zoner has been set to repeat every " + delay + " mins and expire after " + duration
                        + " hrs.");
                ba.sendSmartPrivateMessage(name, "Advert message: " + advert + " %" + sound);
            }
            return id;
        }

        /** Checks if expired then terminates the periodic zoner if so otherwise sends zoner **/
        public void run() {
            long now = System.currentTimeMillis();
            if ((created + (duration * Tools.TimeInMillis.HOUR)) < now)
                end();
            else {
                try {
                    if (advert.length() > NATURAL_LINE)
                        zoneMessageSpam(splitString(advert, LINE_LENGTH), sound);
                    else if (sound > -1)
                        ba.sendZoneMessage(advert, sound);
                    else
                        ba.sendZoneMessage(advert);
                } catch (Exception e) {
                    ba.cancelTask(this);
                    periodic.remove(this);
                    updateIndices();
                }
            }
        }

        /** Terminates the periodic zoner by canceling the TimerTask, removing it from the list, updating list indeces and updating db **/
        public void end() {
            ba.cancelTask(this);
            periodic.remove(this);
            updateIndices();
            if (id != -1)
                ba.SQLBackgroundQuery(db, null, "DELETE FROM tblPeriodic WHERE fnMessageID = " + id);
        }

        /** Sets position index in list **/
        public void setIndex(int i) {
            index = i;
        }

        /** Returns a status message for this periodic zone message **/
        public String toString() {
            if (sound > -1)
                return "Set by: " + name + ", repeats every " + delay + " minute(s), for " + duration + " hour(s). \"" + advert + "%" + sound + "\"";
            else
                return "Set by: " + name + ", repeats every " + delay + " minute(s), for " + duration + " hour(s). \"" + advert + "\"";
        }
    }

    /** Die TimerTask allows for bot to close shop before killing **/
    private class Die extends TimerTask {
        @Override
        public void run() {
            ba.die();
        }
    }

    /** Handles the !debug command which toggles debug mode and the debugger name **/
    private void cmd_debug(String name) {
        if (!DEBUG) {
            debugger = name;
            DEBUG = true;
            ba.sendSmartPrivateMessage(name, "Debugging ENABLED. You are now set as the debugger.");
        } else if (debugger.equalsIgnoreCase(name)) {
            debugger = "";
            DEBUG = false;
            ba.sendSmartPrivateMessage(name, "Debugging DISABLED and debugger reset.");
        } else {
            ba.sendChatMessage(name + " has overriden " + debugger + " as the target of debug messages.");
            ba.sendSmartPrivateMessage(name, "Debugging still ENABLED and you have replaced " + debugger + " as the debugger.");
            debugger = name;
        }
    }

    /** Facilitates debug messaging to fire when enabled to the debugger **/
    public void debug(String msg) {
        if (DEBUG)
            ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }

}
