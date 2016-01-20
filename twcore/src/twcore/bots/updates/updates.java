package twcore.bots.updates;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.TreeSet;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**
 * Updates is a simple bot designed to facilitate communication between
 * the change makers and the public relations representative. This bot will
 * allow updates to be stored as text to be retrieved by the PR rep.
 *
 * @author WingZero
 */
public class updates extends SubspaceBot {

    private static final String DB = "bots";
    private static final String uniqueID = "update";
    
    private PreparedStatement psAddUpdate;
    private PreparedStatement psRemUpdate;
    private PreparedStatement psReadUpdate;
    private PreparedStatement psGetUpdate;
    
    private OperatorList opList; 
    private BotSettings cfg;
    
    private HashMap<Integer, Update> updates;
    private TreeSet<String> reps;
    
    public updates(BotAction botAction) {
        super(botAction);
        
        cfg = ba.getBotSettings();
        opList = ba.getOperatorList();
        updates = new HashMap<Integer, Update>();
        reps = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        
        ba.getBotSettings().getString("Reps");
        
        EventRequester er = ba.getEventRequester();
        er.request(EventRequester.LOGGED_ON);
        er.request(EventRequester.MESSAGE);
        
        psAddUpdate = ba.createPreparedStatement(DB, uniqueID, "INSERT INTO tblUpdate (fcName, fcMessage) VALUES(?,?)");
        psRemUpdate = ba.createPreparedStatement(DB, uniqueID, "DELETE FROM tblUpdate WHERE fnUpdateID = ?");
        psReadUpdate = ba.createPreparedStatement(DB, uniqueID, "UPDATE tblUpdate SET fnRead = ? WHERE fnUpdateID = ?");
        psGetUpdate = ba.createPreparedStatement(DB, uniqueID, "SELECT fnUpdateID, fcName, ftCreated FROM tblUpdate ORDER BY fnUpdateID DESC LIMIT 1");
        
        if (psAddUpdate == null || psRemUpdate == null || psReadUpdate == null || psGetUpdate == null) {
            Tools.printLog("[UpdateBot] Error preparing statements.");
            new Die("Failed to prepare statements. Disconecting...");
        }
        reloadReps();
        reloadUpdates();
    }
    
    /**
     * Handles the LoggedOn event fired upon login.
     */
    public void handleEvent(LoggedOn event) {
        ba.joinArena(cfg.getString("InitialArena"));
    }
    
    /**
     * Handles the Message events.
     */
    public void handleEvent(Message event) {
        int type = event.getMessageType();
        String msg = event.getMessage();
        String cmd = msg.toLowerCase().split(" ")[0];
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null)
            name = event.getMessager();
        if (!opList.isModerator(name))
            return;
        
        if (type == Message.REMOTE_PRIVATE_MESSAGE || type == Message.PRIVATE_MESSAGE) {
            
            if (cmd.startsWith("!h"))
                cmd_help(name);
            else if (cmd.equals("!reload"))
                cmd_reload(name);
            else if (cmd.equals("!list"))
                cmd_list(name);
            else if (cmd.equals("!add"))
                cmd_add(name, msg);
            else if (cmd.equals("!reps"))
                cmd_reps(name);
            
            if (opList.isSmod(name) || reps.contains(name)) {
                if (cmd.equals("!die"))
                    cmd_die(name);
                else if (cmd.startsWith("!addr"))
                    cmd_addRep(name, msg);
                else if (cmd.startsWith("!rem"))
                    cmd_remRep(name, msg);
                else if (cmd.equals("!read"))
                    cmd_read(name, msg);
                else if (cmd.startsWith("!del"))
                    cmd_delete(name, msg);
                
            }
            
        }
    }
    
    /**
     * Displays command help information.
     */
    private void cmd_help(String name) {
        String[] msg = new String[] {
                ",-- Update Tracker --------------------------------------------------------.",
                "| !list            - Displays the currently loaded unread updates          |",
                "| !add <message>   - Adds an unread update message to the database         |",
                "| !reps            - Lists the current representatives                     |",
        };
        ba.smartPrivateMessageSpam(name, msg);
        if (opList.isSmod(name) || reps.contains(name)) {
            msg = new String[] {
                    "| !read <arg>      - Marks <arg> as read/unread where arg is all, an ID#,  |",
                    "|                     a range of IDs (i.e. 4-22), or                       |",
                    "|                     all to mark all unread as read                       |",
                    "| !del <ID>        - Permanently deletes update with ID <ID>               |",
                    "| !addrep <name>   - Adds <name> to the list of representatives            |",
                    "| !remrep <name>   - Removes <name> from the list of representatives       |",
                    "| !reload          - Reloads updates from database and reps from file      |",
                    "| !die             - Kills bot                                             |",
            };
            ba.smartPrivateMessageSpam(name, msg);
        }
        ba.sendSmartPrivateMessage(name, 
                    "`--------------------------------------------------------------------------'");
    }
    
    /**
     * Permanently deletes an update.
     * 
     * @param name
     * @param msg
     */
    private void cmd_delete(String name, String msg) {
        msg = getParameter(msg);
        if (msg != null) {
            try {
                int id = Integer.valueOf(msg.trim());
                if (id > 0 && updates.containsKey(id)) {
                    updates.remove(id);
                    ba.SQLBackgroundQuery(DB, null, "DELETE FROM tblUpdate WHERE fnUpdateID = " + id);
                    ba.sendSmartPrivateMessage(name, "Deleted update " + id + ".");
                } else
                    ba.sendSmartPrivateMessage(name, "No update found with ID number " + id);
            } catch (NumberFormatException e) {
                ba.sendSmartPrivateMessage(name, "Invalid update ID number!");
            }
        }
    }
    
    /**
     * Handles the read command which marks specified messages as read/unread.
     * 
     * @param name
     * @param msg
     */
    private void cmd_read(String name, String msg) {
        String arg = getParameter(msg);
        if (arg != null) {
            arg = arg.trim();
            try {
                if (arg.equalsIgnoreCase("all")) {
                    psReadUpdate.clearBatch();
                    for (Update u : updates.values())
                        u.setRead(false, false);
                    int[] result = psReadUpdate.executeBatch();
                    int count = 0;
                    for (int r : result)
                        if (r > 0)
                            count++;
                    ba.sendSmartPrivateMessage(name, "Successfully marked " + count + " updates as READ.");
                } else if (arg.contains("-")) {
                    String[] args = arg.split("-");
                    if (args.length == 2) {
                        int low = Integer.valueOf(args[0].trim());
                        int high = Integer.valueOf(args[1].trim());
                        if (low > high) {
                            int n = low;
                            low = high;
                            high = n;
                        }
                        int read = 0;
                        int unread = 0;
                        psReadUpdate.clearBatch();
                        Update u = null;
                        for (int i = low; i <= high; i++) {
                            u = updates.get(i);
                            if (u != null) {
                                if (u.getRead()) {
                                    u.setRead(false, false);
                                    unread++;
                                } else {
                                    u.setRead(true, false);
                                    read++;
                                }
                            }
                        }
                        int[] result = psReadUpdate.executeBatch();
                        int count = 0;
                        for (int r : result)
                            if (r > 0)
                                count++;
                        if (count == read + unread)
                            ba.sendSmartPrivateMessage(name, "Successfully READ " + read + " and UNREAD " + unread + " updates.");
                        else
                            ba.sendSmartPrivateMessage(name, "READ " + read + " and UNREAD " + unread + " updates with " + ((read+unread) - count) + " errors.");
                    }
                } else {
                    int id = Integer.valueOf(arg.trim());
                    Update u = updates.get(id);
                    if (u != null) {
                        u.setRead(!u.getRead(), true);
                        ba.sendSmartPrivateMessage(name, "Successfully marked update " + u.getID() + " as " + (u.getRead() ? "READ" : "UNREAD") + ".");
                    } else
                        ba.sendSmartPrivateMessage(name, "Failed to find update " + id + ".");
                }
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            } catch (NumberFormatException e) {
                ba.sendSmartPrivateMessage(name, "Error parsing integer parameters.");
            }
        }
    }
    
    /**
     * Displays list of current public relations representatives.
     * 
     * @param name
     */
    private void cmd_reps(String name) {
        String list = "";
        for (String s : reps)
            list += "," + s;
        if (!list.isEmpty())
            list = list.substring(1);
        ba.sendSmartPrivateMessage(name, "Representatives: " + list);
    }
    
    /**
     * Handles the add rep command which adds a PR rep.
     * 
     * @param name
     * @param msg
     */
    private void cmd_addRep(String name, String msg) {
        String rep = getParameter(msg);
        if (rep != null) {
            if (reps.contains(rep))
                ba.sendSmartPrivateMessage(name, "" + rep + " is already a representative.");
            else {
                String list = rep;
                for (String s : reps)
                    list += "," + s;
                reps.add(rep);
                cfg.put("Reps", list);
                cfg.save();
                ba.sendSmartPrivateMessage(name, "Successfully added representative: " + rep);
            }
        }
    }
    
    /**
     * Handles the remove rep command which removes a PR rep.
     * 
     * @param name
     * @param msg
     */
    private void cmd_remRep(String name, String msg) {
        String rep = getParameter(msg);
        if (rep != null) {
            if (reps.contains(rep)) {
                reps.remove(rep);
                String list = "";
                for (String s : reps)
                    list += "," + s;
                if (!list.isEmpty())
                    list = list.substring(1);
                cfg.put("Reps", list);
                cfg.save();
                ba.sendSmartPrivateMessage(name, "Successfully removed representative: " + rep);
            } else
                ba.sendSmartPrivateMessage(name, "Failed to find representative: " + rep);
        }
    }
    
    /**
     * Adds a new update message to the datebase.
     * 
     * @param name Update message creator name
     * @param msg Message content
     */
    private void cmd_add(String name, String msg) {
        msg = msg.substring(msg.indexOf(" ") + 1);
        try {
            psAddUpdate.clearParameters();
            psAddUpdate.setString(1, name);
            psAddUpdate.setString(2, msg);
            psAddUpdate.executeUpdate();
            ResultSet rs = psGetUpdate.executeQuery();
            if (rs.next() && rs.getString("fcName").equalsIgnoreCase(name)) {
                Update u = new Update(rs.getInt("fnUpdateID"), name, msg, rs.getString("ftCreated"));
                updates.put(u.getID(), u);
                ba.sendSmartPrivateMessage(name, "Success! Your update ID: " + u.getID());
            } else
                ba.sendSmartPrivateMessage(name, "Error retrieving your update ID! :(");
            rs.close();
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }
    
    /**
     * Displays a list of the currently stored updates.
     * 
     * @param name Command sender
     */
    private void cmd_list(String name) {
        ArrayList<String> list = new ArrayList<String>();
        list.add(" ID | R | Updater Name        | Message");
        list.add("----|---|---------------------|---------");

        for (Update u : updates.values()) {
            String[] msg = u.getMessage();
            list.add("" + formatNumber(u.getID(), 3) + 
                    " | " + (u.getRead() ? "x" : " ") + 
                    " | " + Tools.formatString(u.getName(), 19) + 
                    " | " + msg[0]);
            for (int i = 1; i < msg.length; i++)
                list.add(Tools.formatString(" ", 32) + msg[i]);
        }
        ba.smartPrivateMessageSpam(name, list.toArray(new String[list.size()]));
    }
    
    /**
     * Kills bot.
     * 
     * @param name
     */
    private void cmd_die(String name) {
        ba.sendSmartPrivateMessage(name, "Noose tied, bucket kicked...");
        new Die("!die initiated by " + name);
    }
    
    /**
     * Reloads all unread updates from the database.
     * 
     * @param name Command sender
     */
    private void cmd_reload(String name) {
        reloadReps();
        reloadUpdates();
        ba.sendSmartPrivateMessage(name, "Updates and representatives reloaded.");
    }
    
    /**
     * Helper that determines if a command of the form: !command <parameter>
     * is represented by the given String. (i.e. there is a space, and characters following)
     * If so, the command parameter String is returned else null.
     * 
     * @param cmd
     * @return 
     *      Command parameter string
     */
    private String getParameter(String cmd) {
        int index = cmd.indexOf(" ");
        if (cmd.length() > index + 1)
            return cmd.substring(index + 1);
        else
            return null;
    }
    
    /**
     * Helper method takes a number and adds spaces in front of it to meet the
     * specified string size.
     * 
     * @param n
     * @param size
     * @return String padded to size
     */
    private String formatNumber(int n, int size) {
        String str = "" + n;
        while (str.length() < size)
            str = " " + str;
        return str;
    }
    
    /**
     * Loads the list of public relation representatives from the cfg file.
     */
    private void reloadReps() {
        String[] list = cfg.getString("Reps").split(",");
        for (String rep : list)
            if (rep.length() > 0)
                reps.add(rep);
    }
    
    /**
     * Loads all the update messages marked as unread.
     */
    private void reloadUpdates() {
        updates.clear();
        ResultSet rs = null;
        try {
            rs = ba.SQLQuery(DB, "SELECT * FROM tblUpdate");
            while (rs.next()) {
                Update u = new Update(rs.getInt("fnUpdateID"), rs.getString("fcName"), rs.getString("fcMessage"), rs.getString("ftCreated"), (rs.getInt("fnRead") == 1 ? true : false));
                updates.put(u.getID(), u);
            }
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }
    
    /**
     * Update stores all information related to any update stored in the database.
     *
     * @author WingZero
     */
    private class Update {
        
        private static final int SIZE = 75;
        private int id;
        private String msg;
        private String name;
        private String date;
        private boolean read;
        
        public Update(int id, String name, String msg, String date, boolean read) {
            this.id = id;
            this.msg = msg;
            this.name = name;
            this.date = date;
            this.read = read;
        }
        
        public Update(int id, String name, String msg, String date) {
            this(id, name, msg, date, false);
        }
        
        public int getID() {
            return this.id;
        }
        
        @SuppressWarnings("unused")
        public String getFullString() {
            return msg;
        }
        
        public String[] getMessage() {
            String str = msg;
            ArrayList<String> msgs = new ArrayList<String>();
            while (str.length() > SIZE) {
                msgs.add(str.substring(0, SIZE));
                str = str.substring(SIZE);
            }
            msgs.add(str);
            return msgs.toArray(new String[msgs.size()]);
        }
        
        public String getName() {
            return name;
        }

        @SuppressWarnings("unused")
        public String getDate() {
            return date;
        }
        
        public boolean getRead() {
            return read;
        }

        
        public void setRead(boolean hasRead, boolean now) throws SQLException {
            if (read != hasRead) {
                read = hasRead;
                psReadUpdate.clearParameters();
                psReadUpdate.setInt(1, (read ? 1 : 0));
                psReadUpdate.setInt(2, id);
                if (now)
                    psReadUpdate.executeUpdate();
                else
                    psReadUpdate.addBatch();
            }
        }
        
    }
    
    /**
     * Die prepares the bot for death.
     *
     * @author WingZero
     */
    private class Die extends TimerTask {
        
        private String msg;
        
        public Die(String msg) {
            this.msg = msg;
            ba.cancelTasks();
            
            ba.closePreparedStatement(DB, uniqueID, psAddUpdate);
            ba.closePreparedStatement(DB, uniqueID, psRemUpdate);
            ba.closePreparedStatement(DB, uniqueID, psReadUpdate);
            ba.closePreparedStatement(DB, uniqueID, psGetUpdate);
            
            ba.scheduleTask(this, 1500);
        }
        
        public void run() {
            if (msg != null)
                ba.die(msg);
            else
                ba.die("No message given");
        }
    }

}
