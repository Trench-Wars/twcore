package twcore.bots.multibot;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.bots.MultiUtil;
import twcore.core.AdaptiveClassLoader;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.*;
import twcore.core.util.Tools;
import twcore.core.util.ModuleEventRequester;

/**
 * Bot designed for single-event hosting and event utility management.
 * Events are requested and released on the fly, cutting down on the
 * amount of packet classes regularly created and destroyed.
 * Replacement for somewhat dated TWBot.
 */
public class multibot extends SubspaceBot {
    private static final String CLASS_EXTENSION = ".class";
    private static final String CONFIG_EXTENSION = ".cfg";
    private static final String UTIL_NAME = "util";
    private static final long DIE_DELAY = 500;
    private OperatorList opList;
    private MultiModule multiModule;
    private String initialArena;
    private String modulePath;
    private String utilPath;
    private String botChat;
    private HashMap<String, MultiUtil> utils;
    AdaptiveClassLoader loader;
    ModuleEventRequester modEventReq;

    public multibot(BotAction botAction) {
        super(botAction);
        multiModule = null;
    }

    public void handleEvent(LoggedOn event) {
        BotSettings botSettings = m_botAction.getBotSettings();
        String coreRoot = m_botAction.getGeneralSettings().getString(
        "Core Location");

        initialArena = botSettings.getString("initialarena");
        opList = m_botAction.getOperatorList();
        botChat = botSettings.getString("chat");
        modulePath = coreRoot + "/twcore/bots/multibot";
        utilPath = coreRoot + "/twcore/bots/multibot/util";

        Vector<File> repository = new Vector<File>();
        repository.add(new File(coreRoot));

        loader = new AdaptiveClassLoader(repository, getClass()
                .getClassLoader());
        utils = new HashMap<String, MultiUtil>();

        setChat(botChat);
        m_botAction.changeArena(initialArena);
        handleEvent((SubspaceEvent) event);
        modEventReq = new ModuleEventRequester(m_botAction.getEventRequester());
    }

    /**
     * This method handles a message event. If the bot is locked, then the event
     * is passed on to the module. The bot also checks to see if the command
     * should be handled.
     *
     * @param event
     *            is the event to handle.
     */
    public void handleEvent(Message event) {
        String sender = getSender(event);
        String message = event.getMessage();
        int messageType = event.getMessageType();

        if (opList.isER(sender))
            handleCommands(sender, message, messageType);
        else if (message.toLowerCase().startsWith("!where"))
            doWhereCmd(sender, false);
        handleEvent((SubspaceEvent) event);
    }

    /**
     * This method handles a command sent specifically to the bot (not the
     * module). If the bot is unlocked then a go, follow, listgames, lock, home,
     * die and help will be supported. Otherwise unlock and help will be
     * supported.
     *
     */
    public void handleCommands(String sender, String message, int messageType) {
        String command = message.toLowerCase();

        try {
            if (command.equals("!listutils"))
                doListUtilsCmd(sender);
            else if (command.startsWith("!load "))
                doLoadCmd(sender, message.substring(6).trim());
            else if (command.equalsIgnoreCase("!load"))
                doLoadDefaultCmd(sender);
            else if (command.startsWith("!unload "))
                doUnloadCmd(sender, message.substring(8).trim());
            else if (command.equalsIgnoreCase("!unloadall"))
                doUnloadAllCmd(sender);
            else if (command.equalsIgnoreCase("!loaded"))
                doListLoadedCmd(sender);
            else if (command.startsWith("!help "))
                doUtilHelpCmd(sender, message.substring(6).trim());
            else if (command.equalsIgnoreCase("!where"))
                doWhereCmd(sender, true);
            else if (command.equalsIgnoreCase("!gtfo"))
                doGTFOCmd(sender);

            if (!isLocked()) {
                if (command.startsWith("!go "))
                    doGoCmd(sender, message.substring(4).trim());
                else if (command.startsWith("!lock "))
                    doLockCmd(sender, message.substring(6).trim());
                else if (command.equalsIgnoreCase("!help"))
                    doUnlockedHelpMessage(sender);
                else if (command.equalsIgnoreCase("!listgames"))
                    doListGamesCmd(sender);
                else if (command.equalsIgnoreCase("!home"))
                    doHomeCmd(sender);
                else if (command.equalsIgnoreCase("!die"))
                    doDieCmd(sender);
                else if (command.equalsIgnoreCase("!follow"))
                    doFollowCmd(sender);
            } else {
                if (command.equalsIgnoreCase("!help"))
                    doLockedHelpMessage(sender);
                if (command.equalsIgnoreCase("!modhelp"))
                    m_botAction.smartPrivateMessageSpam(sender, multiModule.getModHelpMessage());
                else if (command.equalsIgnoreCase("!unlock"))
                    doUnlockCmd(sender);
                else if (command.equalsIgnoreCase("!module"))
                    doModuleCmd(sender);
            }
        } catch (RuntimeException e) {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage() );

        }
    }

    /**
     * This method sends the bot to another arena.
     *
     * @param sender
     *            is the sender of the command.
     * @param argString
     *            is the argument string.
     */
    private void doGoCmd(String sender, String argString) {
        String currentArena = m_botAction.getArenaName();

        if (currentArena.equalsIgnoreCase(argString))
            throw new IllegalArgumentException("Bot is already in that arena.");
        if (isPublicArena(argString))
            throw new IllegalArgumentException("Bot can not go into public arenas.");
        m_botAction.changeArena(argString);
        m_botAction.sendSmartPrivateMessage(sender, "Going to " + argString + ".");
    }

    /**
     * This method performs the follow command. It makes the bot follow the
     * sender.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doFollowCmd(String sender) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    /**
     * This method performs the ListGames command. It displays all of the games
     * that are available in the current arena.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doListGamesCmd(String sender) {
        File directory = new File(modulePath);
        File[] files = directory.listFiles(moduleFilter);

        Arrays.sort(files);

        m_botAction.sendPrivateMessage(sender, "TW MultiBot Game Library");
        m_botAction.sendPrivateMessage(sender, "------------------------");

        String moduleNames = "";
        int namesinline = 0;

        for (int i = 0; i < files.length; i++) {
            String name = files[i] + "";
            name = name.substring(modulePath.length() + 1);
            moduleNames += Tools.formatString(name, 20);
            namesinline++;
            if (namesinline >= 3) {
                m_botAction.sendPrivateMessage(sender, moduleNames);
                namesinline = 0;
                moduleNames = "";
            }
        }
        if (namesinline > 0)
            m_botAction.sendPrivateMessage(sender, moduleNames);
    }

    /**
     * This method performs the ListUtils command. It displays all of the
     * utility modules available (previously known as TWBot modules).
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doListUtilsCmd(String sender) {
        File directory = new File(utilPath);
        File[] files = directory.listFiles(utilFilter);

        Arrays.sort(files);

        m_botAction.sendPrivateMessage(sender, "TW Event Utility Kit");
        m_botAction.sendPrivateMessage(sender, "------------------------");

        String utilNames = "";
        int namesinline = 0;

        for (int i = 0; i < files.length; i++) {
            String name = files[i] + "";
            // Also get rid of "util" header
            name = name.substring(utilPath.length() + 5);
            name = name.substring(0, name.indexOf(".class"));
            utilNames += Tools.formatString(name, 20);
            namesinline++;
            if (namesinline >= 3) {
                m_botAction.sendPrivateMessage(sender, utilNames);
                namesinline = 0;
                utilNames = "";
            }
        }
        if (namesinline > 0)
            m_botAction.sendPrivateMessage(sender, utilNames);
    }

    public FilenameFilter moduleFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            File f = new File(modulePath, name);

            if (f.isDirectory()) {

                String[] fileNames = f.list();

                if (containsString(fileNames, name.toLowerCase()
                        + CLASS_EXTENSION)
                        && containsString(fileNames, name.toLowerCase() + CONFIG_EXTENSION)) {
                    return true;
                }
                return false;
            }
            return false;
        }
    };

    public FilenameFilter utilFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            if (name.toLowerCase().endsWith(CLASS_EXTENSION)
                    && name.toLowerCase().startsWith(UTIL_NAME)
                    && !name.contains("$"))
                return true;
            return false;
        }
    };

    /**
     * This method locks the bot with the current game number.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doLockCmd(String sender, String argString) {
        loadModule(argString);
        m_botAction.sendSmartPrivateMessage(sender, "Module successfully loaded.");
    }

    /**
     * This method unlocks the bot by unloading the currently loaded module.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doUnlockCmd(String sender) {
        if (multiModule == null)
            return;
        if (!multiModule.isUnloadable())
            throw new IllegalArgumentException("Module can not be unloaded at this time.");
        m_botAction.sendSmartPrivateMessage(sender, "Unloading module: " + multiModule.getModuleName());
        unloadModule();
    }

    /**
     * This method sends the bot back to its home arena.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doHomeCmd(String sender) {
        doUnloadAllCmd(sender);
        doGoCmd(sender, initialArena);
    }

    /**
     * Shuts down all modules and utilities, unlocks the bot, and sends it home.
     *
     * @param sender
     * `         Individual sending the command
     */
    private void doGTFOCmd(String sender) {
        m_botAction.sendPrivateMessage(sender, "OK, I'm getting TFO.");
        doUnlockCmd(sender);
        doUnloadAllCmd(sender);
        doGoCmd(sender, initialArena);
    }

    /**
     * This method logs the bot off.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doDieCmd(String sender) {
        m_botAction.sendSmartPrivateMessage(sender, "Logging Off.");
        m_botAction.sendChatMessage(MultiModule.FIRST_CHAT, "Bot Logging Off.");
        m_botAction.scheduleTask(new DieTask(), DIE_DELAY);
    }

    /**
     * This method performs the module command. It messages the sender with the
     * name of the module that is loaded.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doModuleCmd(String sender) {
        m_botAction.sendSmartPrivateMessage(sender, "Current Module: " + multiModule.getModuleName() + " -- Version: " + multiModule.getVersion() + ".");
    }

    /**
     * Loads a specific utility in the /util directory off multibot. A util must
     * start with the "util" prefix, have a compiled .class extension, and not
     * have a $ in the name.
     *
     * @param name
     *            Individual trying to load
     * @param utilType
     *            Type (name) of utility, minus the util prefix
     */
    public void doLoadCmd(String name, String utilType) {
    	if(utils.containsKey(utilType)) {
    		m_botAction.sendPrivateMessage(name, utilType + " util already loaded.");
    		return;
    	}
        try {
            if (loader.shouldReload())
                loader.reinstantiate();
            utilType = utilType.toLowerCase();
            MultiUtil util = (MultiUtil) loader.loadClass("twcore.bots.multibot.util.util" + utilType).newInstance();
            util.initialize(m_botAction, modEventReq);
            utils.put(utilType, util);
            m_botAction.sendPrivateMessage(name, "Successfully loaded utility " + utilType);
        } catch (Exception e) {
            m_botAction.sendPrivateMessage(name, "Failed to load " + utilType + ".  Use !listutils to see a list of available utilities.");
        }
    }

    /**
     * Loads default modules -- standard, warp and spec.
     * @param name
     *          Person trying to load
     */
    public void doLoadDefaultCmd(String name) {
        doLoadCmd(name, "standard");
        doLoadCmd(name, "warp");
        doLoadCmd(name, "spec");
    }

    /**
     * Unload a specific utility.
     *
     * @param name
     *            Name of requestor
     * @param key
     *            Utility to unload
     */
    public void doUnloadCmd(String name, String util) {
        if (utils.containsKey(util)) {
            MultiUtil removedutil = utils.remove(util);
            modEventReq.releaseAll(removedutil);
            removedutil.cancel();
            m_botAction.sendPrivateMessage(name, util + " successfully unloaded.");
        } else {
            m_botAction.sendPrivateMessage( name, util + " is not loaded, so it cannot be removed.  (NOTE: names are case-sensitive.)");
        }
    }

    /**
     * Unloads all utilities.
     */
    private void doUnloadAllCmd(String sender) {
        Iterator i = utils.values().iterator();
        while (i.hasNext())
            ((MultiUtil) i.next()).cancel();
        utils.clear();
        m_botAction.sendSmartPrivateMessage(sender, "All utilities unloaded.");
    }

    /**
     * Lists all loaded utility modules.
     *
     * @param name
     *            Individual requesting info
     */
    public void doListLoadedCmd(String name) {
        if (utils.size() == 0) {
            m_botAction.sendPrivateMessage(name, "There are no loaded utilities.");
        } else {
            m_botAction.sendPrivateMessage(name, "Loaded utilities:");
            Iterator i = utils.keySet().iterator();
            while (i.hasNext()) {
                m_botAction.sendPrivateMessage(name, "- " + (String) i.next());
            }
        }
    }

    /**
     * Display help for a specific utility module.
     *
     * @param name
     *            Individual requesting help
     */
    public void doUtilHelpCmd(String name, String key) {
        key = key.toLowerCase();
        if (utils.containsKey(key)) {
            try {
                String[] helps = (utils.get(key)).getHelpMessages();
                m_botAction.privateMessageSpam(name, helps);
            } catch (Exception e) {
                m_botAction.sendPrivateMessage(name, "There was a problem accessing the " + key + " utility.  Try reloading it.");
            }
        } else {
            m_botAction.sendPrivateMessage(name, "Sorry, but the module " + key + " has not been loaded.");
        }
    }

    /**
     * This method tells the player wehre the bot is.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doWhereCmd(String sender, boolean staff) {
        if (staff)
            m_botAction.sendSmartPrivateMessage(sender, "I'm being used in " + m_botAction.getArenaName() + ".");
        else if (m_botAction.getArenaName().startsWith("#"))
            m_botAction.sendSmartPrivateMessage(sender, "I'm being used in a private arena.");
        else
            m_botAction.sendSmartPrivateMessage(sender, "I'm being used in " + m_botAction.getArenaName() + ".");
    }

    /**
     * This method displays the help message of an unlocked bot to the sender.
     *
     * @param sender
     *            is the player that sent the help command.
     */
    private void doUnlockedHelpMessage(String sender) {
        m_botAction.smartPrivateMessageSpam(sender, help_unlocked);
    }
	final static String[] help_unlocked = {
	    "!Go <ArenaName>          -- Sends the bot to <ArenaName>.",
	    "!Follow                  -- Turns follow mode on.",
	    "!ListGames               -- Lists the games that are available in this arena.",
	    "!ListUtils               -- Lists utility modules (formerly TWBot modules)",
	    "!Lock <Game>             -- Locks the bot and loads game <Game>.",
	    "!Load/!Unload <Utility>  -- Loads or unloads utility <Utility>.",
	    "!Load                    -- Loads standard, warp and spec modules.",
	    "!UnloadAll               -- Unloads all utilities.",
	    "!Loaded                  -- Shows currently loaded utilties.",
	    "!Help <Utility>          -- Shows help for utility <Utility>.",
	    "!Home                    -- Returns the bot to its home.",
	    "!Die                     -- Logs the bot off."
	};

    /**
     * This method displays the help message of a locked bot to the sender.
     *
     * @param sender
     *            is the player that sent the help command.
     */
    private void doLockedHelpMessage(String sender) {
        m_botAction.smartPrivateMessageSpam(sender, help_locked);
    }
    final static String[] help_locked = {
	    "MultiBot Help:              USE !MODHELP for help on the currently loaded module",
	    "!Module                  -- Displays the module and version that is currently loaded.",
	    "!Unlock                  -- Unlocks the bot, which unloads the presently-loaded module.",
	    "!ListUtils               -- Lists utility modules (formerly TWBot modules)",
	    "!Load/!Unload <Utility>  -- Loads/unloads utility <Utility>.",
	    "!UnloadAll               -- Unloads all utilities.",
	    "!Loaded                  -- Shows currently loaded utilties.",
	    "!Help <Utility>          -- Shows help for utility <Utility>.",
	    "!Die                     -- Logs the bot off.",
	    "!ModHelp                 -- Displays help message for currently-loaded module."
	};


    /**
     * This method checks to see if the bot is locked.
     *
     * @param true
     *            is returned if the bot is locked.
     */
    private boolean isLocked() {
        return multiModule != null;
    }

    /**
     * This method returns the name of the player who sent a message, regardless
     * of the message type. If there is no sender then null is returned.
     *
     * @param event
     *            is the Message event to handle.
     */
    private String getSender(Message event) {
        int messageType = event.getMessageType();
        int playerID;

        if (messageType == Message.ALERT_MESSAGE
         || messageType == Message.CHAT_MESSAGE
         || messageType == Message.REMOTE_PRIVATE_MESSAGE)
            return event.getMessager();

        playerID = event.getPlayerID();
        return m_botAction.getPlayerName(playerID);
    }

    /**
     * This method handles all SubspaceEvents. If the bot is locked, then the
     * event is passed onto the module.
     *
     * @param event
     *            is the event to handle.
     */
    public void handleEvent(SubspaceEvent event) {
        try {
            if (isLocked())
                multiModule.handleEvent(event);
        } catch (Exception e) {
            m_botAction.sendChatMessage(MultiModule.FIRST_CHAT, "Bot Module Unloaded due to exception.");
            unloadModule();
            Tools.printStackTrace(e);
        }

        try {
            Iterator i = utils.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                MultiUtil util = (MultiUtil) entry.getValue();
                util.handleEvent(event);
            }
        } catch (Exception e) {
            m_botAction.sendChatMessage(1, "Uncaught exception in utility.  All utilties unloaded.");
            doUnloadAllCmd((String) null);
            Tools.printStackTrace(e);
        }
    }

    /**
     * This method unloads the loaded module by calling the MultiModules cancel
     * method.
     */
    private void unloadModule() {
        modEventReq.releaseAll(multiModule);
        multiModule.cancel();
        multiModule = null;
        setChat(botChat);
    }

    /**
     * This method loads a multiModule into the bot. For the module to be
     * loaded: 1) The module must reside in a subdirectory whose parent
     * directory is the current bot directory. 2) There must be a
     * modulename.class file present (All lower case). 3) There must be a
     * modulename.cfg file present (All lower case).
     *
     * @param moduleName
     *            is the name of the module to load.
     */
    private void loadModule(String moduleName) {
        Vector<File> repository = new Vector<File>();
        BotSettings moduleSettings;
        String lowerName = moduleName.toLowerCase();
        File directory = new File(modulePath, lowerName);
        String[] fileNames;

        if (!directory.isDirectory())
            throw new IllegalArgumentException("Invalid module name.");
        fileNames = directory.list();
        if (!containsString(fileNames, lowerName + CLASS_EXTENSION))
            throw new IllegalArgumentException("Module missing the appropriate class file.");
        if (!containsString(fileNames, lowerName + CONFIG_EXTENSION))
            throw new IllegalArgumentException("Module missing the appropriate config file.");

        try {
            moduleSettings = new BotSettings(new File(directory, lowerName + CONFIG_EXTENSION));
            repository.add(new File(modulePath, lowerName));
            if (loader.shouldReload())
                loader.reinstantiate();
            multiModule = (MultiModule) loader.loadClass(getParentClass() + "." + lowerName + "." + lowerName).newInstance();
            multiModule.initialize(m_botAction, moduleSettings, modEventReq);
        } catch (Exception e) {
            throw new RuntimeException("Error loading " + moduleName + ".");
        }
    }

    /**
     * This method gets the parent class of the multibot.
     *
     * @return the parent class of the multibot is returned.
     */
    private String getParentClass() {
        String className = this.getClass().getName();
        int parentEnd = className.lastIndexOf(".");

        return className.substring(0, parentEnd);
    }

    /**
     * This method searches through an array of Strings to see if there it
     * contains an element that matches target.
     *
     * @param String[]
     *            array is the array to search.
     * @param String
     *            target is the target to look for.
     * @return true is returned if target is contained in array.
     */
    private boolean containsString(String[] array, String target) {
        for (int index = 0; index < array.length; index++)
            if (array[index].equals(target))
                return true;
        return false;
    }

    /**
     * This method checks to see if an arena is a public arena.
     *
     * @param arenaName
     *            is the arena name to check.
     * @return true is returned if the arena is a public arena.
     */
    private boolean isPublicArena(String arenaName) {
        try {
            Integer.parseInt(arenaName);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * This method sets the bots chat.
     *
     * @param chatName
     *            is the bots chat.
     */
    private void setChat(String chatName) {
        m_botAction.sendUnfilteredPublicMessage("?chat=" + chatName);
    }

    public void handleEvent(ArenaJoined event) {
        m_botAction.setReliableKills(1);
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(ArenaList event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(PlayerPosition event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(PlayerLeft event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(PlayerDeath event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(PlayerEntered event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(Prize event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(ScoreUpdate event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(WeaponFired event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(FrequencyChange event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(FrequencyShipChange event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(FileArrived event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(FlagVictory event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(FlagReward event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(ScoreReset event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(WatchDamage event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(SoccerGoal event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(BallPosition event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(FlagPosition event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(FlagDropped event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(FlagClaimed event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(InterProcessEvent event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(TurretEvent event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(SQLResultEvent event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(TurfFlagUpdate event) {
        handleEvent((SubspaceEvent) event);
    }

    private class DieTask extends TimerTask {
        public void run() {
            m_botAction.die();
        }
    }
}