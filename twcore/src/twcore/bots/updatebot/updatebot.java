package twcore.bots.updatebot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**
 * UpdateBot is a simple bot designed to facilitate communication between
 * the change makers and the public relations representative. This bot will
 * allow updates to be stored as text to be retrieved by the PR rep.
 *
 * @author WingZero
 */
public class updatebot extends SubspaceBot {

    private static final String db = "bots";
    private static final String ps = "update";
    
    private PreparedStatement psAddUpdate;
    private PreparedStatement psRemUpdate;
    private PreparedStatement psReadUpdate;
    private PreparedStatement psGetUpdate;
    
    private OperatorList opList; 
    
    private HashMap<Integer, Update> updates;
    
    public updatebot(BotAction botAction) {
        super(botAction);
        
        EventRequester er = ba.getEventRequester();
        er.request(EventRequester.LOGGED_ON);
        er.request(EventRequester.MESSAGE);
        
        psAddUpdate = ba.createPreparedStatement(db, ps, "INSERT INTO tblUpdate (fcName, fcMessage) VALUES(?,?)");
        psRemUpdate = ba.createPreparedStatement(db, ps, "DELETE FROM tblUpdate WHERE fnUpdateID = ?");
        psReadUpdate = ba.createPreparedStatement(db, ps, "UPDATE tblUpdate SET fnRead = ? WHERE fnUpdateID = ?");
        psGetUpdate = ba.createPreparedStatement(db, ps, "SELECT fnUpdateID, fcName, ftCreated FROM tblUpdate WHERE 1=1 ORDER BY fnUpdateID DESC LIMIT 1");
        
        if (psAddUpdate == null || psRemUpdate == null || psReadUpdate == null || psGetUpdate == null) {
            Tools.printLog("[UpdateBot] Error preparing statements.");
            new Die("Failed to prepare statements. Disconecting...");
        }
        updates = new HashMap<Integer, Update>();
        reloadUpdates();
    }
    
    private void reloadUpdates() {
        updates.clear();
        ResultSet rs = null;
        try {
            rs = ba.SQLQuery(db, "SELECT * FROM tblUpdate WHERE fnRead = 1");
            while (rs.next()) {
                Update u = new Update(rs.getInt("fnUpdateID"), rs.getString("fcName"), rs.getString("fcMessage"), rs.getString("ftCreated"));
                updates.put(u.getID(), u);
            }
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
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
            
            if (cmd.equals("!reload"))
                cmd_reload(name);
            else if (cmd.equals("!list"))
                cmd_list(name);
            else if (cmd.equals("!add"))
                cmd_add(name, msg);
            else if (cmd.equals("!more"));
            
            if (opList.isSmod(name)) {
                if (cmd.equals("!die")) {
                    new Die();
                }
            }
            
        }
    }
    
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
        list.add("R | Name               | Message");
        for (Update u : updates.values()) {
            String[] msg = u.getMessage();
            list.add((u.getRead() ? "x" : " ") + " | " + Tools.formatString(u.getName(), 19) + " | ");
            for (String s : msg)
                list.add("    " + s);
        }
        ba.smartPrivateMessageSpam(name, list.toArray(new String[list.size()]));
    }
    
    /**
     * Reloads all unread updates from the database.
     * 
     * @param name Command sender
     */
    private void cmd_reload(String name) {
        reloadUpdates();
        ba.sendSmartPrivateMessage(name, "Updates reloaded.");
    }
    
    /**
     * Update stores all information related to any update stored in the database.
     *
     * @author WingZero
     */
    private class Update {
        
        private static final int size = 225;
        private int id;
        private String msg;
        private String name;
        private String date;
        private boolean read;
        
        public Update(int id, String msg, String name, String date) {
            this.id = id;
            this.msg = msg;
            this.name = name;
            this.date = date;
            this.read = false;
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
            while (str.length() > size) {
                msgs.add(str.substring(0, size));
                str = str.substring(size);
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

        @SuppressWarnings("unused")
        public void setRead(boolean hasRead, boolean now) {
            read = hasRead;
            try {
                psReadUpdate.clearParameters();
                psReadUpdate.setInt(1, (read ? 1 : 0));
                psReadUpdate.setInt(2, id);
                if (now)
                    psReadUpdate.executeUpdate();
                else
                    psReadUpdate.addBatch();
            } catch (SQLException e) {
                Tools.printStackTrace(e);
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
            
            ba.closePreparedStatement(db, ps, psAddUpdate);
            ba.closePreparedStatement(db, ps, psRemUpdate);
            ba.closePreparedStatement(db, ps, psReadUpdate);
            ba.closePreparedStatement(db, ps, psGetUpdate);
            
            ba.scheduleTask(this, 1500);
        }
        
        public Die() {
            this(null);
        }
        
        public void run() {
            if (msg != null)
                ba.die(msg);
            else
                ba.die();
        }
    }

}
