package twcore.bots.loginbot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.*;

/**
 * @description This bot can be used to automatically login each name in a list as read from a text file.
 *              It essentially spawns a dummy bot with each login name taken from the list using a master password
 *              which is defined in the cfg. If a single password differs from the master a temporary password 
 *              can be set to be used in the next login attempt. The bot can determine login success and failure. 
 *              Upon failure it will try a few more times before moving on. The bot can also be used for a quick 
 *              one time login using the name and password you give it.
 * @author      WingZero
 */
public class loginbot extends SubspaceBot {

    BotAction ba;
    BufferedWriter outputStream; // used to write new login information for loginspawn
    int count = 0;               // number of login attempts for the current name
    boolean on = true;           // bot name login process status
    boolean alert = true;        // determines whether or not to send alerts to starter
    boolean login = false;       // true when !login is used (mainly for when the bot is on)
    boolean cut = false;         // used to remember if the bot was on or off before the login command
    String bot = "";             // current bot name
    String starter = "";         // name of the smod+ who did !start (or assigned using !starter)
    String corePath = "";        // the path as set in the core setup cfg
    String password = null;      // temporary break in password if the default isn't working
    String defaultPassword = ""; // primary default password used for most bot names (defined in loginbot's cfg)
    String[] loginJumpin;        // stores login 
    Vector<String> names;        // list of bot names to be logged in
    static final String HUB = "WingHub";
    static final String CFG_PATH = "/twcore/bots/loginspawn/loginspawn.cfg";
    OperatorList ops;
    
    public loginbot(BotAction botAction) {
        super(botAction);
        ba = m_botAction;
        ops = ba.getOperatorList();
        names = new Vector<String>();
        EventRequester er = ba.getEventRequester();
        er.request(EventRequester.MESSAGE);
        er.request(EventRequester.LOGGED_ON);
        er.request(EventRequester.ARENA_JOINED);
    }
    
    public void handleEvent(Message event) {
        int type = event.getMessageType();
        String msg = event.getMessage();
        // the core's chat message alerts will be used to determine if the bot login was successful
        if (type == Message.CHAT_MESSAGE) {
            // normal disconnect means it logged in just fine
            // therefore, no problems so set count back to 0 for next bot name
            if (msg.contains(bot) && msg.contains("disconnected normally.")) {
                count = 0;
                // if !login was used then pause the auto-logins temporarily and execute the !login
                if (!on && login) {
                    on = true;
                    loginHelper();
                } else 
                    next();
            // if process is broken for !login then check to see if it was successful
            } else if (on && login && msg.contains(loginJumpin[0]) && msg.contains("disconnected normally.")) {
                login = false;
                ba.sendSmartPrivateMessage(loginJumpin[2], "" + loginJumpin[0] + " has successfully logged in.");
                loginJumpin = null;
                // if the process was broken, then restore it and continue
                if (!cut) {
                    on = false;
                } else {
                    cut = false;
                    next();
                }
            // login failures are generic so !login doesn't matter
            } else if ((msg.contains("loginspawn") && msg.contains("failed to log in")) || (msg.contains("loginspawn") && msg.contains("tried to spawn"))) {
                // check for a break and report failure
                if (login) {
                    ba.sendSmartPrivateMessage(loginJumpin[2], loginJumpin[0] + " failed to login.");
                    login = false;
                    loginJumpin = null;
                    // if the process was broken, then restore it and continue
                    if (!cut) {
                        on = false;
                    } else {
                        cut = false;
                        on = true;
                        next();
                    }
                    return;
                // if failure count exceeds 2 attempts then skip it
                } else if (count > 1) {
                    count = 0;
                    ba.sendSmartPrivateMessage(starter, "Login error, skipping " + bot);
                    next();
                    return;
                }
                // otherwise record attempt and try again
                count++;
                if (alert)
                    ba.sendSmartPrivateMessage(starter, bot + " spawn failure " + count);
                TimerTask t = new TimerTask() {
                    public void run() {
                        names.add(0, bot);
                        next();
                    }
                };
                ba.scheduleTask(t, 15*1000);
            }
            return;
        }
        
        // command handling
        String name = ba.getPlayerName(event.getPlayerID());
        if (name != null && ops.isSmod(name) && (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE)) {
            if (name == null || name.length() < 1)
                name = starter;
            if (msg.equals("!die")) {
                ba.die();
            } else if(msg.equals("!next")) {
                next();
            } else if(msg.equals("!stop")) {
                stop(name);
            } else if (msg.equals("!load")) {
                loadNames(name);
            } else if (msg.equals("!start")) {
                start(name);
            } else if (msg.equals("!alert")) {
                alert(name);
            } else if (msg.equals("!where")) {
                ba.sendSmartPrivateMessage(name, bot + " and " + names.size() + " left");
            } else if (msg.equals("!help")) {
                help(name);
            } else if (msg.equals("!starter")) {
                starter = name;
            } else if (msg.startsWith("!starter ") && msg.length() > 9) {
                starter = msg.substring(msg.indexOf(" "));
            } else if (msg.startsWith("!pw ") && msg.length() > 4) {
                password(name, msg);
            } else if (msg.startsWith("!login ")) {
                login(name, msg);
            }
        } 
    }
    
    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena("#robopark");
        ba.sendUnfilteredPublicMessage("?chat=robodev");
        corePath = ba.getCoreData().getGeneralSettings().getString("Core Location");
    }
    
    public void handleEvent(ArenaJoined event) {
        defaultPassword = ba.getBotSettings().getString("Default Password");
    }
    
    public void help(String name) {
        String[] msg = {
                "+----LoginBot Commands-----------------------------------------------------+",
                "| !load              -- Loads namelist.txt if it exists                    |",
                "| !start             -- Begins the login spawn process                     |",   
                "| !stop              -- Stops the process where it is                      |",
                "| !next              -- Forces the next spawn in case process haults       |",
                "| !alert             -- Toggles the bot name alerts ON/OFF                 |",
                "| !where             -- Displays the current bot name and remaining        |",
                "| !die               -- Kills the bot                                      |",
                "| !starter           -- Changes the starter to your name                   |",
                "| !starter <name>    -- Changes the starter to <name>                      |",
                "| !pw <password>     -- Sets a temporary password in case of failed logins |",
                "| !login <name>:<pw> -- One time login attempt using <name> and <pw>       |",
                "+--------------------------------------------------------------------------+"
        };
        
        if (ba.getFuzzyPlayerName(name) != null)
            ba.privateMessageSpam(name, msg);
        else {
            for (int i = 0; i < msg.length; i++)
                ba.sendSmartPrivateMessage(name, msg[i]);
        }
    }
    
    /**
     * Loads all the names found in namelist.txt into a Vector for use by the auto-login process
     * @param name
     */
    public void loadNames(String name) {
        try {
            File f = new File(corePath + "/twcore/bots/loginbot/namelist.txt");
            if (f.exists()) {
                BufferedReader input = new BufferedReader(new FileReader(corePath + "/twcore/bots/loginbot/namelist.txt"));
                String e = "";
                while ((e = input.readLine()) != null) {
                    if (!names.contains(e))
                        names.add(e);
                }
                input.close();
                ba.sendSmartPrivateMessage(name, "Names have been loaded successfully.");
            } else
                ba.sendSmartPrivateMessage(name, "namelist.txt could not be found.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    /**
     * Stops the bot from continuing to spawn bot names in the list
     * @param name
     */
    public void stop(String name) {
        on = false;
        if (name != null)
            ba.sendSmartPrivateMessage(name, "Login refresh process stopped.");
    }
    
    /**
     * Starts attempting to login with all the names in the names Vector
     * @param name
     */
    public void start(String name) {
        if (names.isEmpty()) {
            ba.sendSmartPrivateMessage(name, "There are no names loaded. You must first do !load");
            return;
        }
        on = true;
        if (name != null) {
            ba.sendSmartPrivateMessage(name, "Login refresh process started.");
            starter = name;
        } else {
            starter = HUB;
        }
        next();
    }
    
    /**
     * Sets a temporary password to be used on the next bot login attempt
     * @param name Staff member
     * @param msg  Content of the command message
     */
    public void password(String name, String msg) {
        if (!name.equalsIgnoreCase(starter)) {
            ba.sendSmartPrivateMessage(name, "You must be the starter in order to set a temporary password.");
            return;
        }
        
        password = msg.substring(msg.indexOf(" ") + 1);
        ba.sendSmartPrivateMessage(name, "Password temporarily set to: " + password);
    }
    
    /**
     * Logs in using a set name and password one time only
     * Breaks the auto-login process and continues when finished
     * @param name Staff member name
     * @param msg  Content of the command message
     */
    public void login(String name, String msg) {
        if (!msg.contains(" ") || !msg.contains(":")) {
            ba.sendSmartPrivateMessage(name, "Invalid syntax, use !login <name>:<password>");
            return;
        }
        msg += ":" + name;
        
        loginJumpin = msg.substring(msg.indexOf(" ") + 1).split(":");
        if (loginJumpin.length == 3 
                && loginJumpin[0] != null && loginJumpin[1] != null 
                && loginJumpin[0].length() > 1 && loginJumpin[1].length() > 1) {
            
            ba.sendSmartPrivateMessage(name, "Attempting to login using name=" + loginJumpin[0] + " and password=" + loginJumpin[1]);
            if (on) {
                cut = true;
                login = true;
                on = false;
            } else {
                login = true;
            }
        } else
            ba.sendSmartPrivateMessage(name, "An error occured, check your syntax and try again.");
    }
    
    /**
     * If the bot is on then this will execute when the current login attempt haults
     */
    private void loginHelper() {
        try {
            File f = new File(corePath + CFG_PATH);
            f.delete();
            f.createNewFile();
            outputStream = new BufferedWriter(new FileWriter(corePath + CFG_PATH));
            outputStream.newLine();
            outputStream.write("Max Bots=1");
            outputStream.newLine();
            outputStream.write("Name1=" + loginJumpin[0]);
            outputStream.newLine();
            outputStream.write("Password1=" + loginJumpin[1]);
            outputStream.newLine();
            outputStream.close();
            ba.sendSmartPrivateMessage(HUB, "!spawn loginspawn");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Toggles the alert messages that are sent to the starter
     * @param name
     */
    public void alert(String name) {
        if (!name.equalsIgnoreCase(starter)) {
            ba.sendSmartPrivateMessage(name, "You must have been the starter in order to change the alert status.");
            return;
        }
        alert = !alert;
        if (!alert)
            ba.sendSmartPrivateMessage(name, "Alerts have been turned OFF.");
        else
            ba.sendSmartPrivateMessage(name, "Alerts have been turned ON.");
    }
    
    /**
     * Increments next bot name login attempt
     * Used automatically but can also be used manually
     */
    public void next() {
        if (!on && !login) {
            ba.sendSmartPrivateMessage(starter, "The login process is currently OFF. To turn it ON, use !start");
            return;
        }
        // if the Vector is empty then either all the bot names have been logged in or the names were never loaded
        if (names.isEmpty()) {
            ba.sendSmartPrivateMessage(starter, "All done!");
            on = false;
            return;            
        }
        bot = names.remove(0);
        if (alert)
            ba.sendSmartPrivateMessage(starter, "Next up: " + bot);
        try {
            // delete loginspawn's old cfg and replace it with one containing the next login info
            File f = new File(corePath + CFG_PATH);
            f.delete();
            f.createNewFile();
            outputStream = new BufferedWriter(new FileWriter(corePath + CFG_PATH));
            outputStream.newLine();
            outputStream.write("Max Bots=1");
            outputStream.newLine();
            outputStream.write("Name1=" + bot);
            outputStream.newLine();
            if (password == null)
                outputStream.write("Password1=" + defaultPassword);
            else {
                outputStream.write("Password1=" + password);
                password = null;
            }
            outputStream.newLine();
            outputStream.close();
            ba.sendSmartPrivateMessage(HUB, "!spawn loginspawn");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
