package twcore.bots.multibot.util;

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.OperatorList;
import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.util.CodeCompiler;

/**
 * HotSpots 2.0 - milosh - 2.15.08
 */
public class utilhotspots extends MultiUtil {
    
    public OperatorList opList;
        
    public Vector<HotSpot> hotSpots;
    public HotSpot watch;
    public boolean watching;    
    public TimerTask changeTask;
    public HashMap<String, Long> recentContacts;
    
    public String database = "website";
    
    public int switchTime = 200, repeatTime = 200;
    
    /**
     * Initializes.
     */
    public void init() {
        recentContacts = new HashMap<String, Long>();
        opList = m_botAction.getOperatorList();
        hotSpots = new Vector<HotSpot>();
        // Turn off updating -- we will do it ourselves
        m_botAction.setPlayerPositionUpdating(0);
        m_botAction.stopSpectatingPlayer();
        watching = false;
    }
    
    /**
     * Requests events.
     */
    public void requestEvents(ModuleEventRequester modEventReq) {
        modEventReq.request(this, EventRequester.PLAYER_POSITION);
    }
    
    /**
     * Handles messages
     */
    public void handleEvent(Message event) {
        
        String message = event.getMessage();
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            if (m_opList.isER(name))
                handleCommand(name, message);
        }
    }
    
    /**
     * handles commands
     * 
     * @param sender
     *            is the user of the bot.
     * @param message
     *            is the command.
     */
    public void handleCommand(String sender, String message) {
        String msg = message;
        message = message.toLowerCase();
        if (message.startsWith("!addspot "))
            do_addHotSpot(sender, message.substring(9, message.length()));
        if (message.startsWith("!removespot "))
            do_removeHotSpot(sender, message.substring(12));
        if (message.startsWith("!addmsg "))
            do_addMessage(sender, msg.substring(8));
        if (message.startsWith("!removemsg "))
            do_removeMessage(sender, message.substring(11));
        if (message.startsWith("!listmsg "))
            do_listMessages(sender, message.substring(9));
        if (message.startsWith("!switchtime "))
            do_switchTime(sender, message.substring(12, message.length()));
        if (message.startsWith("!repeattime "))
            do_repeatTime(sender, message.substring(12));
        if (message.startsWith("!listspots"))
            do_listSpots(sender);
        if (message.startsWith("!clearspots"))
            do_clearHotSpots(sender);
        if (message.equalsIgnoreCase("!watch"))
            do_watch(sender);
        if (message.equalsIgnoreCase("!stopwatching"))
            do_stopWatch(sender);
        if (message.equalsIgnoreCase("!loadspots"))
            do_loadSpots(sender);
    }
    
    /**
     * Handles player positions
     */
    public void handleEvent(PlayerPosition event) {
        int x = event.getXLocation() + ((event.getXVelocity() / 160000) * 300);
        int y = event.getYLocation() + ((event.getYVelocity() / 160000) * 300);
        String name = m_botAction.getPlayerName(event.getPlayerID());
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if (name == null || p == null || !watching)
            return;
        if (recentContacts.containsKey(name)) {
            if ((System.currentTimeMillis() - recentContacts.get(name)) < repeatTime) {
                return;
            }
        }
        recentContacts.put(name, System.currentTimeMillis());
        if (watch != null) {
            if (watch.inside(x, y)) {
                // if( watch.needsWarp() )
                // m_botAction.warpTo( event.getPlayerID(), watch.getX2(),
                // watch.getY2() );
                if (watch.getMessages() != null) {
                    Iterator<String> i = watch.getMessages().iterator();
                    while( i.hasNext() )
                        CodeCompiler.handlePrivateTWScript(m_botAction, p, i.next());
                }
            }
        }
    }
    
    /**
     * Starts watching hot spots
     */
    public void do_watch(String name) {
        if (watching) {
            m_botAction.sendSmartPrivateMessage(name, "Already watching spots. Type !stopwatching to deactivate.");
            return;
        }
        if (hotSpots.size() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "There are no spots to watch. Use the !addspot command to create one.");
            return;
        }
        m_botAction.sendSmartPrivateMessage(name, "HotSpots are now active.");
        changeTask = new TimerTask() {
            public void run() {
                try {
                    watch = hotSpots.elementAt(0);
                    hotSpots.removeElementAt(0);
                    hotSpots.addElement(watch);
                } catch (Exception e) {
                    this.cancel();
                    Tools.printStackTrace(e);
                }
                m_botAction.moveToTile(watch.getX(), watch.getY());
            }
        };
        m_botAction.scheduleTaskAtFixedRate(changeTask, 2000, switchTime);
        watching = true;
    }
    
    /**
     * Stops watching hot spots
     */
    public void do_stopWatch(String name) {
        if (!watching) {
            m_botAction.sendSmartPrivateMessage(name, "I'm not currently watching any spots. Type !watch to active.");
            return;
        }
        m_botAction.sendSmartPrivateMessage(name, "HotSpots are no longer active.");
        changeTask.cancel();
        watching = false;
    }
    
    /**
     * Lists spots.
     * 
     * @param sender
     *            is the user of the bot
     */
    public void do_listSpots(String sender) {
        Iterator<HotSpot> it = hotSpots.iterator();
        if (!it.hasNext()) {
            m_botAction.sendPrivateMessage(sender, "No spots loaded!");
            return;
        }
        while (it.hasNext()) {
            HotSpot hs = it.next();
            m_botAction.sendPrivateMessage(sender, hotSpots.indexOf(hs) + ") " + hs.toString());
        }
        
    }
    
    /**
     * Removes a hot spot.
     * 
     * @param sender
     *            is the user of the bot
     * @param message
     *            is the index of the spot
     */
    public void do_removeHotSpot(String sender, String message) {
        try {
            int spot = Integer.parseInt(message);
            hotSpots.remove(spot);
            m_botAction.sendSmartPrivateMessage(sender, "The spot at index '" + spot + "' has been removed.");
            if (hotSpots.size() == 0 && watching) {
                changeTask.cancel();
                m_botAction.sendSmartPrivateMessage(sender, "There are no more hotspots to watch. I'm taking a much deserved break.");
            }
        } catch (NumberFormatException e) {
            m_botAction.sendSmartPrivateMessage(sender, "Impropert Format. Usage: !removespot <index>");
        } catch (ArrayIndexOutOfBoundsException e) {
            m_botAction.sendSmartPrivateMessage(sender, "Index not found. Use !listspots to see indices");
        }
    }
    
    /**
     * Adds a message to a spot
     * 
     * @param sender
     *            is the user of the bot
     * @param message
     *            is the index of the spot and the message to add
     */
    public void do_addMessage(String sender, String message) {
        if (message.indexOf(" ") == -1)
            return;
        String spot = message.substring(0, message.indexOf(" "));
        String msg = message.substring(message.indexOf(" ") + 1);
        
        int index;
        try {
            index = Integer.parseInt(spot);
        } catch (Exception e) {
            m_botAction.sendPrivateMessage(sender, "Input error. Check and try again.");
            return;
        }
        if (index >= hotSpots.size() || index < 0) {
            m_botAction.sendPrivateMessage(sender, "The given HotSpot does not exist.");
            return;
        }
        hotSpots.elementAt(index).addMessage(msg);
        m_botAction.sendSmartPrivateMessage(sender, "Message added to HotSpot at index " + index);
    }
    
    /**
     * Removes a message from a spot
     * 
     * @param sender
     *            is the user of the bot
     * @param message
     *            is the index of the spot and the message to add
     */
    public void do_removeMessage(String sender, String message) {
        
        String pieces[] = message.split(" ");
        if (pieces.length != 2)
            return;
        
        int values[] = new int[2];
        try {
            for (int i = 0; i < 2; i++)
                values[i] = Integer.parseInt(pieces[i]);
        } catch (Exception e) {
            m_botAction.sendPrivateMessage(sender, "Input error. Check and try again.");
            return;
        }
        if (values[0] >= hotSpots.size() || values[0] < 0) {
            m_botAction.sendPrivateMessage(sender, "The given HotSpot does not exist.");
            return;
        }
        if (hotSpots.elementAt(values[0]).getMessage(values[1]) == null) {
            m_botAction.sendPrivateMessage(sender, "There is no message at the specified index.");
        } else {
            hotSpots.elementAt(values[0]).removeMessage(values[1]);
            m_botAction.sendPrivateMessage(sender, "Message " + values[1] + " of HotSpot " + values[0] + " removed.");
        }
    }
    
    /**
     * Lists the messages of a spot
     * 
     * @param sender
     *            is the user of the bot
     * @param message
     *            is the index of the spot
     */
    public void do_listMessages(String sender, String message) {
        int index;
        try {
            index = Integer.parseInt(message);
        } catch (Exception e) {
            m_botAction.sendPrivateMessage(sender, "Input error. Check and try again.");
            return;
        }
        if (index >= hotSpots.size() || index < 0) {
            m_botAction.sendPrivateMessage(sender, "The given HotSpot does not exist.");
            return;
        }
        
        if (hotSpots.elementAt(index).getMessages() == null) {
            m_botAction.sendPrivateMessage(sender, "There are no messages assigned to this spot.");
        } else {
            int i = 0;
            Iterator<String> it = hotSpots.elementAt(index).getMessages().iterator();
            while (it.hasNext()) {
                String msg = it.next();
                m_botAction.sendPrivateMessage(sender, i + ") " + msg);
                i++;
            }
        }
        
    }
    
    /**
     * Adds a spot
     * 
     * @param sender
     *            is the user of the bot.
     * @param message
     *            is the hotspot.
     */
    public void do_addHotSpot(String sender, String message) {
        
        String pieces[] = message.split(" ");
        if (pieces.length < 3 || pieces.length == 4 || pieces.length > 6)
            return;
        
        int values[] = new int[pieces.length];
        try {
            for (int i = 0; i < pieces.length; i++)
                values[i] = Integer.parseInt(pieces[i]);
        } catch (Exception e) {
            m_botAction.sendPrivateMessage(sender, "Input error. Check and try again.");
            return;
        }
        int index = get_hotSpotIndex(values[0], values[1], values[2]);
        if (index == -1) {
            HotSpot newSpot = new HotSpot(values);
            if (watch == null) {
                watch = newSpot;
            }
            hotSpots.add(newSpot);
            m_botAction.sendPrivateMessage(sender, "Hotspot added.");
        } else {
            if (values.length == 3) {
                m_botAction.sendSmartPrivateMessage(sender, "Hotspot already exists at index " + index + ".");
                return;
            }
            if (values.length >= 5)
                hotSpots.elementAt(index).addMessage("*warpto " + values[3] + " " + values[4]);
            if (values.length == 6)
                hotSpots.elementAt(index).addMessage("*prize #" + values[5]);
            m_botAction.sendSmartPrivateMessage(sender, "Changes added to existing hotspot at index " + index + ".");
        }
    }
    
    /**
     * Gets the index of the hotspot.
     * 
     * @param x -
     *            X coordinate of spot
     * @param y -
     *            Y coordinate of spot
     * @param r -
     *            Radius of spot
     * @return index. -1 if spot does not exist
     */
    public int get_hotSpotIndex(int x, int y, int r) {
        Iterator<HotSpot> i = hotSpots.iterator();
        while (i.hasNext()) {
            HotSpot hs = i.next();
            if (hs.getX() == x && hs.getY() == y && hs.getR() == r)
                return hotSpots.indexOf(hs);
        }
        return -1;
    }
    
    /**
     * Loads spots for the arena from database.
     * 
     * @param sender
     *            is the user of the bot.
     */
    public void do_loadSpots(String sender) {
        try {
            ResultSet resultSet = m_botAction.SQLQuery(database, 
                    "SELECT HS.* " + "FROM tblArena A, tblSetupHotspots HS "
                    + "WHERE A.fnArenaID = HS.fnArenaID "
                    + "AND A.fcArenaName = '"
                    + m_botAction.getArenaName() + "'");
            int count = 0;
            while (resultSet.next()) {
                int x = resultSet.getInt("fnX");
                int y = resultSet.getInt("fnY");
                int r = resultSet.getInt("fnR");
                String message = resultSet.getString("fcMessage");
                int index = get_hotSpotIndex(x, y, r);
                if (index == -1) {
                    do_addHotSpot("", x + " " + y + " " + r);
                    count++;
                }
                hotSpots.elementAt(get_hotSpotIndex(x, y, r)).addMessage(message);
            }
            m_botAction.SQLClose(resultSet);
            if (count == 0)
                m_botAction.sendSmartPrivateMessage(sender, "No hotspots are registered for this arena.");
            else
                m_botAction.sendSmartPrivateMessage(sender, count + " hotspots for this arena have been loaded.");
            
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }
    
    /**
     * Clears all hotspots.
     * 
     * @param name
     */
    public void do_clearHotSpots(String name) {
        hotSpots.clear();
        if (watching) {
            changeTask.cancel();
            watching = false;
        }
        watch = null;
        m_botAction.sendPrivateMessage(name, "All hotspots cleared. No longer watching.");
    }
    
    /**
     * Switches time and resets timer task to that interval.
     * 
     * @param sender
     *            is the user of the bot
     * @param message
     *            is the command
     */
    public void do_switchTime(String sender, String message) {
        int time = switchTime;
        try {
            time = Integer.parseInt(message);
        } catch (Exception e) {
            m_botAction.sendPrivateMessage(sender, "Input error. I need a number!");
            return;
        }
        if (time < 200) {
            m_botAction.sendPrivateMessage(sender, "Time cannot be less than 200ms.");
            return;
        }
        
        switchTime = time;
        if (watching) {
            watching = false;
            changeTask.cancel();
            do_watch(sender);
        }
        m_botAction.sendPrivateMessage(sender, "Switch time set to " + time);
    }
    
    /**
     * Changes the amount of time a user must wait to get a response from a
     * hotspot after an initial response.
     * 
     * @param sender
     *            is the user of the bot
     * @param message
     *            is the command
     */
    public void do_repeatTime(String sender, String message) {
        int time = repeatTime;
        try {
            time = Integer.parseInt(message);
        } catch (Exception e) {
            m_botAction.sendPrivateMessage(sender, "Input error. I need a number!");
            return;
        }
        if (time < 200) {
            m_botAction.sendPrivateMessage(sender, "Time cannot be less than 200ms.");
            return;
        }
        
        repeatTime = time;
        m_botAction.sendPrivateMessage(sender, "Repeat time set to " + time);
    }
    
    /**
     * Returns help message
     */
    public String[] getHelpMessages() {
        String help[] = {
                "HotSpot Module V2.0:", 
                "!addspot <x> <y> <r>    - Adds a new hotspot.<x> <y> <radius>",
                "                        - Alternative Use: !addspot <x> <y> <r> <DestX> <DestY>",
                "                        - Alternative Use: !addspot <x> <y> <r> <DestX> <DestY> <prize>",
                "!removespot <index>     - Removes the HotSpot at <index>",
                "!clearspots             - Remove all hotspots.",
                "!listspots              - Lists your hotspots",
                "!addmsg <index> <text>  - Adds a message/command to send the player in contact with this spot",
                "!removemsg <idx> <idx>  - Removes a message from a hotspot. <HotSpot index> <Message index>",
                "!listmsg <index>        - Lists all messages for the specified HotSpot. <HotSpot index>",
                "!repeattime <ms>        - Time after which a hotspot may refire on the same player.",
                "!switchtime <ms>        - How long to watch each spot before moving to the next",
                "!watch                  - Activates the module.",
                "!stopwatching           - Stops watching hot spots."      
        };
        return help;
    }
    
    public void cancel() {
        do_clearHotSpots(null);
        m_botAction.resetReliablePositionUpdating();
    }
}

/**
 * Hot spot class that holds all related values
 */
class HotSpot {
    
    public int x, y, r;
    public ArrayList<String> messages;
    
    /**
     * Creates a new hotspot with the array of values. You can still use 5/6
     * values for warping/prizing, but only 3 values are needed to create a
     * working hotspot.
     */
    public HotSpot(int values[]) {
        if (values.length >= 3) {
            x = values[0];
            y = values[1];
            r = values[2];
        }
        if (values.length >= 5)
            addMessage("*warpto " + values[3] + " " + values[4]);
        if (values.length == 6)
            addMessage("*prize #" + values[5]);
    }
    
    /**
     * Add a message/command a player will receive when coming into contact with
     * this spot.
     */
    public void addMessage(String message) {
        if (messages == null)
            messages = new ArrayList<String>();
        messages.add(message);
    }
    
    /**
     * Get the message at a specified index. If there is none return null.
     */
    public String getMessage(int index) {
        try {
            return messages.get(index);
        } catch (Exception e) {
            return null;
        }// throws ArrayIndexOutOfBounds
    }
    
    /**
     * Remove a message/command a player will receive when coming into contact
     * with this spot.
     */
    public void removeMessage(int index) {
        try {
            messages.remove(index);
            if (messages.size() == 0)
                messages = null;
        } catch (Exception e) {}
    }
    
    /**
     * Get the ArrayList of messages for this spot.
     */
    public ArrayList<String> getMessages() {
        return messages;
    }
    
    /**
     * Return true if the given X,Y is touching this spot. Else return false.
     */
    public boolean inside(int playerX, int playerY) {
        double dist = Math.sqrt(Math.pow(x * 16 - playerX, 2) + Math.pow(y * 16 - playerY, 2));
        if (Math.round(dist) <= r * 16)
            return true;
        else
            return false;
    }
    
    /**
     * Returns a string representation of a Hotspot
     */
    public String toString() {
        return ("X:" + x + " Y:" + y + " Radius:" + r);
    }
    
    /**
     * Returns this Hotspot's X Coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Returns this Hotspot's Y Coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Returns this Hotspot's radius
     */
    public int getR() {
        return r;
    }
}