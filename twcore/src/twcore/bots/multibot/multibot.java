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
import twcore.bots.TWScript;
import twcore.core.AdaptiveClassLoader;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.*;
import twcore.core.util.Tools;
import twcore.core.util.ModuleEventRequester;
import twcore.core.game.Player;

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
    private static final long OWNER_RESET_TIME = Tools.TimeInMillis.MINUTE * 30;    // Time in ms before
                                                                                    // new owner is set
    private OperatorList m_opList;
    private MultiModule m_eventModule;
    private String m_initialArena;
    private String coreRoot;
    private String m_modulePath;
    private String m_utilPath;
    private String m_botChat;
    private String m_owner;             // Name of present bot-user
    private long m_lastUse;             // Last time bot was used by present user
    private HashMap<String, MultiUtil> m_utils;
    AdaptiveClassLoader m_loader;
    ModuleEventRequester m_modEventReq;
    private boolean m_isLocked = false;
    private boolean m_followEnabled = false;
    private boolean m_doCome = false;
    private boolean m_ownerOverride = false;
    private boolean m_smodLocked = false;
    private boolean m_sysopLocked = false;

    public multibot(BotAction botAction) {
        super(botAction);
        m_eventModule = null;
    }

    public void handleEvent(LoggedOn event) {
        BotSettings botSettings = m_botAction.getBotSettings();
        coreRoot = m_botAction.getGeneralSettings().getString(
        "Core Location");

        m_initialArena = botSettings.getString("initialarena");
        m_opList = m_botAction.getOperatorList();
        m_botChat = botSettings.getString("chat");
        m_modulePath = coreRoot + "/twcore/bots/multibot";
        m_utilPath = coreRoot + "/twcore/bots/multibot/util";

        Vector<File> repository = new Vector<File>();
        repository.add(new File(coreRoot));

        m_loader = new AdaptiveClassLoader(repository, getClass()
                .getClassLoader());
        m_utils = new HashMap<String, MultiUtil>();

        setChat(m_botChat);
        m_botAction.changeArena(m_initialArena);
        handleEvent((SubspaceEvent) event);
        m_modEventReq = new ModuleEventRequester(m_botAction.getEventRequester());
        m_botAction.getEventRequester().requestAll();

        //Checks for an idle unlocked bot.
        TimerTask checkTime = new TimerTask()
        {
            public void run()
            {
                if( !m_isLocked && !m_botAction.getArenaName().equals(m_initialArena) )
                    if( System.currentTimeMillis() > m_lastUse + OWNER_RESET_TIME )
                        doGTFOCmd(m_botAction.getBotName());
            }
        };
        m_botAction.scheduleTaskAtFixedRate(checkTime, 0, 30000);

    }

    public boolean isIdle() {
        if( m_eventModule == null || !m_eventModule.isUnloadable() )
            return false;
        return true;
    }

    /**
     * This method handles a message event. The bot checks to see first if the
     * msg sender owns the bot; if not, commands are not accepted.
     *
     * @param event
     *            is the event to handle.
     */
    public void handleEvent(Message event) {
        String sender = getSender(event);
        String message = event.getMessage();
        int messageType = event.getMessageType();
        boolean foundCmd = false;
        boolean isER = m_opList.isER(sender);
        if( messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE ) {
        	if(m_sysopLocked && !m_opList.isSysop(sender)){
        		m_botAction.sendSmartPrivateMessage( sender, "This bot is currently locked for exclusive Sysop use.");
        		return;
        	}
        	if(m_smodLocked && !m_opList.isSmod(sender)){
            	m_botAction.sendSmartPrivateMessage( sender, "This bot is currently locked for exclusive Smod use.");
            	return;
            }
            // Attempt to handle player commands (with oodles of TWBot backwards compatibility)
            if( message.equalsIgnoreCase("!where") || message.equalsIgnoreCase("!host") || message.equalsIgnoreCase("!games") ) {
                doWhereCmd(sender, isER);
                foundCmd = true;
            } else if( message.equalsIgnoreCase("!help") && !isER ) {
                m_botAction.sendSmartPrivateMessage(sender, "Hi, I'm a bot that helps host Trench Wars games!  Send !where to see who is hosting me, where at, and what they're hosting." );
            } else if( isER ) {
                if( m_owner == null && !sender.equals(m_botAction.getBotName())) {
                    m_owner = sender;
                    m_lastUse = System.currentTimeMillis();
                    m_botAction.sendSmartPrivateMessage(sender, "You are my new owner.  Use !free (or !gtfo) to relinquish ownership." );
                    foundCmd = handleCommands(sender, message, messageType);
                } else {
                    if( m_owner.equals(sender) || sender.equals(m_botAction.getBotName()) ) {
                        m_lastUse = System.currentTimeMillis();
                        foundCmd = handleCommands(sender, message, messageType);
                    } else {
                        if( System.currentTimeMillis() > m_lastUse + OWNER_RESET_TIME ) {
                            m_owner = sender;
                            m_lastUse = System.currentTimeMillis();
                            m_botAction.sendSmartPrivateMessage(sender, "You are my new owner.  Use !free (or !gtfo) to relinquish ownership." );
                            foundCmd = handleCommands(sender, message, messageType);
                        } else {
                            if( m_ownerOverride ) {
                                foundCmd = handleCommands(sender, message, messageType);
                            } else {
                                if( message.startsWith("!") )
                                    m_botAction.sendSmartPrivateMessage(sender, "I am owned by: " + m_owner + " - last use: " + Tools.getTimeDiffString(m_lastUse, true) + "  !mybot to claim; !override to command w/o owner change." );
                                if( message.equalsIgnoreCase("!mybot") || message.equalsIgnoreCase("!override") )
                                    foundCmd = handleCommands(sender, message, messageType);
                            }
                        }
                    }
                }
            }
        }

        // In Follow mode: decipher *locate report and change to arena
        if( (m_followEnabled || m_doCome) && messageType == Message.ARENA_MESSAGE && !m_isLocked ) {
            if( message.startsWith(m_owner + " - ") ) {
                try {
                    String arena = message.substring( message.indexOf("- ") + 2 );
                    if( arena.equalsIgnoreCase( m_botAction.getArenaName()) ) {
                        m_botAction.sendSmartPrivateMessage(m_owner, "I'm already here." );
                        m_doCome = false;
                        return;
                    } else if( isPublicArena(arena) && !m_opList.isSmod(m_owner) ) {
                        m_botAction.sendSmartPrivateMessage(m_owner, "Sorry, I can't go to public arenas." );
                        m_doCome = false;
                        return;
                    }
                    m_botAction.changeArena( arena );
                    m_doCome = false;
                } catch (Exception e) {
                }
            }
        }

        if( !foundCmd )
            handleEvent((SubspaceEvent) event);
    }

    /**
     * This method monitors PlayerLeft events, for use with the follow cmd,
     * and sends out a *locate when the owner of the bot leaves.
     *
     * @param event
     *            is the event to handle.
     */
    public void handleEvent(PlayerLeft event) {
        if( m_followEnabled ) {
            Player p = m_botAction.getPlayer( event.getPlayerID() );
            if( p != null && p.getPlayerName().equals(m_owner) )
                m_botAction.sendUnfilteredPublicMessage("*locate " + m_owner );
        }
        handleEvent((SubspaceEvent) event);
    }


    /**
     * This method handles a command sent specifically to the bot (not the
     * module). If the bot is unlocked then a go, follow, listgames, lock, home,
     * die and help will be supported. Otherwise unlock and help will be
     * supported.
     *
     */
    public boolean handleCommands(String sender, String message, int messageType) {
        String command = message.toLowerCase();
        boolean foundCommand = true;

        try {
            if (command.equals("!listutils") || command.equals("!lu"))
                doListUtilsCmd(sender);
            else if (command.equals("!listgames") || command.equals("!lg"))
                doListGamesCmd(sender);
            else if (command.startsWith("!load "))
                doLoadCmd(sender, message.substring(6).trim());
            else if (command.startsWith("!l "))
                doLoadCmd(sender, message.substring(3).trim());
            else if (command.startsWith("!unload "))
                doUnloadCmd(sender, message.substring(8).trim());
            else if (command.startsWith("!ul "))
                doUnloadCmd(sender, message.substring(4).trim());
            else if (command.equals("!unloadall") || command.equals("!off") || command.equals("!ula") )
                doUnloadAllCmd(sender, false);
            else if (command.equals("!loaded") || command.equals("!ll") || command.startsWith("!module") )
                doListLoadedCmd(sender);
            else if (command.startsWith("!help !"))
                doCommandHelpCmd(sender, message.substring(7).trim(), false);
            else if (command.startsWith("? !"))
                doCommandHelpCmd(sender, message.substring(3).trim(), false);
            else if (command.startsWith("!help "))
                doModuleHelpCmd(sender, message.substring(6).trim());
            else if (command.startsWith("? "))
                doModuleHelpCmd(sender, message.substring(2).trim());
            else if (command.equals("!help") || command.equals("?"))
                doStandardHelpMessage(sender);
            else if (command.equals("!modhelp") || command.equals("!mh"))
                doGameModuleHelpMessage(sender);
            else if (command.equals("!where") || command.equals("!wh") || command.equals("!host") )
                doWhereCmd(sender, true);
            else if (command.equals("!gtfo") || command.equals("!home") || command.equals("!!"))
                doGTFOCmd(sender);
            else if (command.startsWith("!go "))
                doGoCmd(sender, message.substring(4).trim());
            else if (command.startsWith("!come") || command.equals("!c"))
                doComeCmd(sender);
            else if (command.equals("!lock") || command.equals("!lo"))
                doLockCmd(sender);
            else if (command.equals("!unlock") || command.equals("!ulo"))
                doUnlockCmd(sender, false);
            else if (command.equals("!unlockwith") || command.equals("!ulow"))
                doUnlockCmd(sender, true);
            else if (command.equals("!follow") || command.equals("!f"))
                doFollowCmd(sender);
            else if (command.equals("!override") || command.equals("!or"))
                doOverrideCmd(sender);
            else if (command.equals("!mybot") || command.equals("!my"))
                doMybotCmd(sender);
            else if (command.equals("!free") || command.equals("!fr"))
                doFreeCmd(sender);
            else if (command.equals("!die"))
                doDieCmd(sender);
            else if (m_opList.isSmod(sender) && command.equals("!smodlock"))
            	doSmodLock(sender);
            else if (m_opList.isSysop(sender) && command.equals("!sysoplock"))
            	doSysopLock(sender);
            else
                foundCommand = false;
        } catch (RuntimeException e) {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage() );
        }
        return foundCommand;
    }

    /**
     * This method sends the bot to another arena.
     *
     * @param sender
     *            is the sender of the command.
     * @param argString
     *            is the argument string.
     */
    private void doGoCmd(String sender, String argString ) throws IllegalArgumentException {
        if( m_isLocked )
            throw new IllegalArgumentException("I am locked, sorry.  Use !unlock or !unlockwith before trying to move me.");
        String currentArena = m_botAction.getArenaName();
        if (currentArena.equalsIgnoreCase(argString))
            throw new IllegalArgumentException("Bot is already in that arena.");
        if (isPublicArena(argString) && !m_opList.isSmod(sender))
            throw new IllegalArgumentException("Bot can not go into public arenas.");
       	m_botAction.changeArena(argString);
        m_botAction.sendSmartPrivateMessage(sender, "Going to " + argString + ".");
    }

    /**
     * This method tells the bot to come to your current arena.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doComeCmd(String sender) throws IllegalArgumentException {
        if( m_isLocked )
            throw new IllegalArgumentException("I am locked, sorry.  Use !unlock or !unlockwith before trying to move me.");
        m_botAction.sendSmartPrivateMessage(sender, "Coming...");
        m_botAction.sendUnfilteredPublicMessage("*locate " + m_owner );
        m_doCome = true;
    }

    /**
     * This method performs the follow command. It makes the bot follow the
     * sender.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doFollowCmd(String sender) {
        m_followEnabled = !m_followEnabled;
        if( m_followEnabled )
            if( m_isLocked )
                m_botAction.sendSmartPrivateMessage(sender, "Follow ON.  However, I am still locked.  First use !unlock or !unlockwith if you would like me to follow.");
            else
                m_botAction.sendSmartPrivateMessage(sender, "Follow ON.  I will now follow you when you leave.");
        else
            m_botAction.sendSmartPrivateMessage(sender, "Follow OFF.  I will no longer follow you when you leave.");
    }

    /**
     * This method performs the ListGames command. It displays all of the games
     * that are available in the current arena.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doListGamesCmd(String sender) {
        File directory = new File(m_modulePath);
        File[] files = directory.listFiles(moduleFilter);

        Arrays.sort(files);

        m_botAction.sendSmartPrivateMessage(sender, "TW MultiBot Game Library");
        m_botAction.sendSmartPrivateMessage(sender, "------------------------");

        String moduleNames = "";
        int namesinline = 0;

        for (int i = 0; i < files.length; i++) {
            String name = files[i] + "";
            name = name.substring(m_modulePath.length() + 1);
            moduleNames += Tools.formatString(name, 20);
            namesinline++;
            if (namesinline >= 3) {
                m_botAction.sendSmartPrivateMessage(sender, moduleNames);
                namesinline = 0;
                moduleNames = "";
            }
        }
        if (namesinline > 0)
            m_botAction.sendSmartPrivateMessage(sender, moduleNames);
    }

    /**
     * This method performs the ListUtils command. It displays all of the
     * utility modules available (previously known as TWBot modules).
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doListUtilsCmd(String sender) {
        File directory = new File(m_utilPath);
        File[] files = directory.listFiles(utilFilter);

        Arrays.sort(files);

        m_botAction.sendSmartPrivateMessage(sender, "TW Event Utility Kit");
        m_botAction.sendSmartPrivateMessage(sender, "------------------------");

        String utilNames = "";
        int namesinline = 0;

        for (int i = 0; i < files.length; i++) {
            String name = files[i] + "";
            // Also get rid of "util" header
            name = name.substring(m_utilPath.length() + 5);
            name = name.substring(0, name.indexOf(".class"));
            utilNames += Tools.formatString(name, 20);
            namesinline++;
            if (namesinline >= 3) {
                m_botAction.sendSmartPrivateMessage(sender, utilNames);
                namesinline = 0;
                utilNames = "";
            }
        }
        if (namesinline > 0)
            m_botAction.sendSmartPrivateMessage(sender, utilNames);
    }

    public FilenameFilter moduleFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            File f = new File(m_modulePath, name);

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
    private void doLockCmd( String sender ) {
        if( m_isLocked ) {
            m_botAction.sendSmartPrivateMessage(sender, "I'm already locked.");
            return;
        }

        m_isLocked = true;
        boolean defaultUtilsLoaded = true;
        if( !m_utils.containsKey("standard") ) {
            doLoadCmd(sender, "standard", true);
            defaultUtilsLoaded = false;
        }
        if( !m_utils.containsKey("warp") ) {
            doLoadCmd(sender, "warp", true);
            defaultUtilsLoaded = false;
        }
        if( !m_utils.containsKey("spec") ) {
            doLoadCmd(sender, "spec", true);
            defaultUtilsLoaded = false;
        }
        if( defaultUtilsLoaded )
            m_botAction.sendSmartPrivateMessage(sender, "Locked." );
        else
            m_botAction.sendSmartPrivateMessage(sender, "Locked; standard, warp and spec utils loaded." );
    }

    /**
     * This method unlocks the bot and unloads all modules.
     *
     * @param sender
     *            is the sender of the command.
     * @param unlockWith
     *            True if unlock will not unload modules
     */
    private void doUnlockCmd(String sender, boolean unlockWith ) throws IllegalArgumentException {
        if( !unlockWith ) {
            if( eventModuleLoaded() && !m_eventModule.isUnloadable())
                throw new IllegalArgumentException("The loaded game can not be unloaded at this time.  Please try again in a moment.");

            m_botAction.sendSmartPrivateMessage(sender, "Unlocked; all modules unloaded.  (Use !unlockwith to keep modules loaded when unlocking.)" );
            doUnloadAllCmd(sender, true);
        } else {
            m_botAction.sendSmartPrivateMessage(sender, "Unlocked.  All !loaded modules remain with me.  Use !go to move with the modules loaded." );
        }
        m_isLocked = false;
    }

    /**
     * Shuts down all modules and utilities, unlocks the bot, and sends it home.
     *
     * @param sender
     * `         Individual sending the command
     */
    private void doGTFOCmd(String sender) {
        doUnlockCmd(sender, false);
        doGoCmd(sender, m_initialArena);
        m_owner = null;
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
        m_botAction.scheduleTask(new DieTask(sender), DIE_DELAY);
    }
    
    /**
     * This method toggles smod locking.
     * 
     * @param name is the sender of the command.
     */
    private void doSmodLock(String name) {
    	if(m_smodLocked){
    		m_botAction.sendSmartPrivateMessage( name, "Smod locking has been disabled.");
    		m_smodLocked = false;
    	} else {
    		m_botAction.sendSmartPrivateMessage( name, "Smod locking has been enabled.");
    		m_smodLocked = true;
    	}
    }
    
    /**
     * This method toggles sysop locking.
     * 
     * @param name is the sender of the command.
     */
    private void doSysopLock(String name) {
    	if(m_sysopLocked){
    		m_botAction.sendSmartPrivateMessage( name, "Sysop locking has been disabled.");
    		m_sysopLocked = false;
    	} else {
    		m_botAction.sendSmartPrivateMessage( name, "Sysop locking has been enabled.");
    		m_sysopLocked = true;
    	}
    }

    /**
     * Loads a specific module: util or event module.
     *
     * Wrapper for doLoadCmd( String, String, boolean ).
     *
     * @param name
     *            Individual trying to load
     * @param module
     *            Name of module
     */
    public void doLoadCmd(String name, String module) {
        doLoadCmd( name, module, false );
    }

    /**
     * Loads a specific module: util or event module.
     *
     * A utility must be in the /util directory off multibot, must
     * start with the "util" prefix, have a compiled .class extension, and not
     * have a $ in the name.
     *
     * An event module must have its own directory off multibot that is
     * the same as the .class name, and have an appropriate .cfg.
     *
     * @param name
     *            Individual trying to load
     * @param module
     *            Name of module
     * @param quiet
     *            Whether or not to send a success msg when the module is loaded
     */
    public void doLoadCmd(String name, String module, boolean quiet ) throws IllegalArgumentException {
        if( !m_isLocked )
            throw new RuntimeException("You must !lock me before you !load any modules.");

        if(m_utils.containsKey(module)) {
            throw new RuntimeException(module + " util already loaded.");
        } else if( m_eventModule != null && m_eventModule.getClass().getSimpleName().toLowerCase().equals(module) ) {
            throw new RuntimeException(module + " event already loaded.");
        }

        // Attempt event load
        String lowerName = module.toLowerCase();
        File directory = new File(m_modulePath, lowerName);
        if (directory.isDirectory() && !module.equalsIgnoreCase("twscript")) {
            // If the name matches a directory, we're dealing with a game module...
            String[] fileNames = directory.list();
            if (!containsString(fileNames, lowerName + CLASS_EXTENSION))
                throw new IllegalArgumentException("Module missing the appropriate class file.");
            if (!containsString(fileNames, lowerName + CONFIG_EXTENSION))
                throw new IllegalArgumentException("Module missing the appropriate config file.");

            try {
                Vector<File> repository = new Vector<File>();
                BotSettings moduleSettings;
                moduleSettings = new BotSettings(new File(directory, lowerName + CONFIG_EXTENSION));
                repository.add(new File(m_modulePath, lowerName));
                if (m_loader.shouldReload())
                    m_loader.reinstantiate();
                Class<?> classmulti = m_loader.loadClass(getParentClass() + "." + lowerName + "." + lowerName);
                m_eventModule = (MultiModule)classmulti.newInstance();
                m_eventModule.initialize(m_botAction, moduleSettings, m_modEventReq);
                if( !quiet )
                    m_botAction.sendPrivateMessage(name, "Loaded module: " + m_eventModule.getModuleName() );
            } catch (InstantiationException ie) {
                throw new RuntimeException("Unknown problem encountered while attempting to load the module (module: "+module+", error: "+ie.getMessage()+"). Please contact a member of TW Bot Development.");
            } catch (IllegalAccessException iae) {
                throw new RuntimeException("Access problem encountered while attempting to load the module (module: "+module+", error: "+iae.getMessage()+"). Please contact a member of TW Bot Development.");
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException("This game module ('"+module+"') can't be loaded because it isn't found. Use !listgames to see available games; !listutils to see utils.");
            } catch (NullPointerException npe) {
                //NullPointerException can be thrown if classmulti.newInstance() can't instantiate the class
                throw new RuntimeException("Unable to load module '"+module+"' (NullPointerException). Please contact a member of TW Bot Development.");
            }
        } else if(directory.isDirectory() && module.equalsIgnoreCase("twscript")) {
        	loadTWScript(name, quiet);
        }else {
            // Not a game module; try util instead
            try {
                if (m_loader.shouldReload())
                    m_loader.reinstantiate();
                module = module.toLowerCase();
                MultiUtil util = (MultiUtil) m_loader.loadClass("twcore.bots.multibot.util.util" + module).newInstance();
                util.initialize(m_botAction, m_modEventReq);
                m_utils.put(module, util);
                if( !quiet )
                    m_botAction.sendPrivateMessage(name, "Loaded utility: " + module);
            } catch (InstantiationException ie) {
                throw new RuntimeException("Unknown problem encountered while attempting to load the utility (module: "+module+", error: "+ie.getMessage()+"). Please contact a member of TW Bot Development.");
            } catch (IllegalAccessException iae) {
                throw new RuntimeException("Access problem encountered while attempting to load the utility (module: "+module+", error: "+iae.getMessage()+"). Please contact a member of TW Bot Development.");
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException("This utility ('"+module+"') can't be loaded because it isn't found. Use !listgames to see available games; !listutils to see utils.");
            } catch (NullPointerException npe) {
                //NullPointerException can be thrown if classmulti.newInstance() can't instantiate the class
                throw new RuntimeException("Unable to load module '"+module+"' (NullPointerException). Please contact a member of TW Bot Development.");
            }
        }
    }

    /**
     * Unload a specific utility.
     *
     * @param name
     *            Name of requestor
     * @param key
     *            Module or utility to unload
     */
    public void doUnloadCmd(String name, String module) throws IllegalArgumentException {
        if( module.equals("") ) {
            doUnloadAllCmd(name, false);
            return;
        }
        if (m_utils.containsKey(module)) {
            MultiUtil removedutil = m_utils.remove(module);
            //modEventReq.releaseAll(removedutil);
            removedutil.cancel();
            m_botAction.sendPrivateMessage(name, "Unloaded util: " + module );
        } else if( m_eventModule != null && m_eventModule.getClass().getSimpleName().toLowerCase().equals(module) ) {
            if (!m_eventModule.isUnloadable())
                throw new IllegalArgumentException("Module can not be unloaded at this time.  Please hang up and try your call again later.");
            m_botAction.sendSmartPrivateMessage(name, "Unloaded game: " + m_eventModule.getModuleName());
            if(m_eventModule.getClass().getSimpleName().toLowerCase().equals("twscript"))
            	unloadTWScript();
            unloadEventModule();
        } else {
            m_botAction.sendPrivateMessage( name, "Utility or game '" + module + "' not loaded.  (NOTE: names are case-sensitive.)  Use !loaded to see a list of loaded modules.");
        }
    }

    /**
     * Unloads all utilities.
     * @param beQuiet True if the method should not PM the ER
     */
    private void doUnloadAllCmd(String sender, boolean beQuiet ) throws IllegalArgumentException {
        Iterator<MultiUtil> i = m_utils.values().iterator();
        while (i.hasNext())
            ((MultiUtil) i.next()).cancel();
        m_utils.clear();
        if (m_eventModule != null)
            if (!m_eventModule.isUnloadable())
                throw new IllegalArgumentException("Unable to unload " + m_eventModule.getModuleName() + " at this time.  Please !unload manually.");
            else
                unloadEventModule();
        if( !beQuiet )
            m_botAction.sendSmartPrivateMessage(sender, "All modules unloaded.");
    }

    /**
     * Lists loaded game module and all utility modules.
     *
     * @param name
     *            Individual requesting info
     */
    public void doListLoadedCmd(String name) {
        if( m_eventModule != null )
            m_botAction.sendSmartPrivateMessage(name, "Game: " + m_eventModule.getClass().getSimpleName() + " (" + m_eventModule.getModuleName() + "), version " + m_eventModule.getVersion() + "");
        else
            m_botAction.sendSmartPrivateMessage(name, "Game: none" );
        if (m_utils.size() == 0) {
            m_botAction.sendPrivateMessage(name, "Utilities: none");
        } else {
            m_botAction.sendPrivateMessage(name, "Utilities:");
            Iterator<String> i = m_utils.keySet().iterator();
            while (i.hasNext()) {
                m_botAction.sendPrivateMessage(name, "- " + (String) i.next());
            }
        }
    }

    /**
     * This method tells the player where the bot is / who is hosting with it.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doWhereCmd(String sender, boolean staff) {
        if( m_botAction.getArenaName().equals("#robopark") ) {
            m_botAction.sendSmartPrivateMessage(sender, "I'm not in use.");
            return;
        }
        if( m_botAction.getPlayer(sender) != null ) {
            if( m_eventModule != null )
                m_botAction.sendSmartPrivateMessage(sender, m_owner + " is hosting " + m_eventModule.getModuleName() + " here.");
            else
                m_botAction.sendSmartPrivateMessage(sender, m_owner + " is using me here, but for what I don't yet know.");
            return;
        }
        String arenaName = m_botAction.getArenaName();

        // For arenas other than present one
        if(arenaName.startsWith("#") && !staff )
            m_botAction.sendSmartPrivateMessage(sender, "I'm being used in a private arena.");
        else {
            if( m_owner != null ) {
                if( m_eventModule != null )
                    m_botAction.sendSmartPrivateMessage(sender, m_owner + " is using me in " + arenaName + " for " + m_eventModule.getModuleName() +".");
                else
                    m_botAction.sendSmartPrivateMessage(sender, m_owner + " is using me in " + arenaName + ", but for what I don't yet know.");
            } else {
                m_botAction.sendSmartPrivateMessage(sender, "I am stranded in " + arenaName + " with no owner!");
            }
        }
    }

    /**
     * Claims the bot for the sender.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doMybotCmd(String sender) {
        if( m_owner.equals(sender) )
            throw new RuntimeException( "You're already my owner -- I automatically set you as my owner if you send a command and no-one has used me within " + (OWNER_RESET_TIME / Tools.TimeInMillis.MINUTE ) + " minutes." );
        if( m_owner != null )
            m_botAction.sendSmartPrivateMessage(m_owner, sender + " has claimed me as his or her bot." );
        m_owner = sender;
        m_lastUse = System.currentTimeMillis();
        m_botAction.sendSmartPrivateMessage(sender, "I'm yours." );
    }

    /**
     * Frees the bot from ownership.  You're free, FREE!
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doFreeCmd(String sender) {
        if( m_owner.equals(sender) ) {
            m_owner = null;
            m_botAction.sendSmartPrivateMessage(sender, "I am now free from the shackles of human oppression (you no longer own me)." );
        } else {
            // This shouldn't ever happen, but just in case ...
            if( m_owner == null )
                return;
            m_botAction.sendSmartPrivateMessage(sender, "Only my master, " + m_owner + ", can set me free.  Or you could become my new owner with !mybot." );
        }
    }

    /**
     * Starts up the manual override.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doOverrideCmd(String sender) {
        m_ownerOverride = !m_ownerOverride;
        if( m_ownerOverride ) {
            if( m_owner != null && !sender.equals(m_owner) )
                m_botAction.sendSmartPrivateMessage(m_owner, sender + " initiated owner override, allowing other staff members to use commands." );
            m_botAction.sendSmartPrivateMessage(sender, "Override initiated.  Commands now accepted from all staff.  !override again to turn off." );
        } else {
            if( m_owner != null && !sender.equals(m_owner) )
                m_botAction.sendSmartPrivateMessage(m_owner, sender + " disabled owner override.  You are now the only person who can use me." );
            m_botAction.sendSmartPrivateMessage(sender, "Override disabled." );
        }
    }

    /**
     * Display help for a specific module.
     *
     * @param sender
     *            Individual requesting help
     */
    public void doModuleHelpCmd(String sender, String key) {
        key = key.toLowerCase();
        if( m_eventModule != null && m_eventModule.getClass().getSimpleName().toLowerCase().equals(key) ) {
            m_botAction.smartPrivateMessageSpam(sender, m_eventModule.getModHelpMessage());
        } else if (m_utils.containsKey(key)) {
            try {
                String[] helps = (m_utils.get(key)).getHelpMessages();
                m_botAction.privateMessageSpam(sender, helps);
            } catch (Exception e) {
                m_botAction.sendPrivateMessage(sender, "There was a problem accessing the " + key + " utility.  Try reloading it.");
            }
        } else {
            // When nothing is found, try
            doCommandHelpCmd(sender, key, true);
        }
    }

    /**
     * This method displays the help message.
     *
     * @param sender
     *            is the player that sent the help command.
     */
    private void doStandardHelpMessage(String sender) {
        m_botAction.smartPrivateMessageSpam(sender, help_standard);
        if( eventModuleLoaded() )
            m_botAction.smartPrivateMessageSpam( sender, m_eventModule.getModHelpMessage() );
    }
    final static String[] help_standard = {
        "Commands: !go <arena>, !come, !lock, !unlock, !unlockwith, !load, !unload, !unloadall, !listgames, !listutils, "
        + "!loaded, !follow, !gtfo, !die, !help !<cmd>, !help <utility>, !modhelp, !mybot, !override, !free",
        "Use !help !<cmd> for command descriptions and shortcuts.  Ex: !help !go"
	};

    private void doCommandHelpCmd(String sender, String cmd, boolean triedModuleHelp) {
        String returnMsg;
        if( triedModuleHelp )
            returnMsg = "Could not find a module loaded that is named '" + cmd + "', or the general command '!" + cmd + "'";
        else
            returnMsg = "Command not found: !" + cmd;
        if( cmd.equals("go") )
            returnMsg = "!go <arena>  -  Sends the bot to <arena>.";
        else if( cmd.equals("come") )
            returnMsg = "!come  -  Tells the bot to join you in your present arena. (Shortcut: !c)";
        else if( cmd.equals("follow") )
            returnMsg = "!follow  -  Toggles follow mode on and off.  If on, when you leave the arena the bot will follow you.  Now WORKS. (Shortcut: !f)";
        else if( cmd.equals("lock") )
            returnMsg = "!lock  -  Locks the bot in place, and loads the standard util set. (Shortcut: !lo)";
        else if( cmd.equals("unlock") )
            returnMsg = "!unlock  -  Unlocks the bot, unloading the loaded game and any utils.  Use !unlockwith to keep modules loaded when unlocking. (Shortcut: !ulo)";
        else if( cmd.equals("unlockwith") )
            returnMsg = "!unlockwith   -  Unlocks the bot, keeping any modules loaded. (Shortcut: !ulow)";
        else if( cmd.equals("listgames") )
            returnMsg = "!listgames  -  Lists games available to !load.  You can have only one game loaded at any given time. (Shortcut: !lg)";
        else if( cmd.equals("listutils") )
            returnMsg = "!listutils  -  Lists utilities available to !load.  You can !load as many utils at the same time as you want. (Shortcut: !lu)";
        else if( cmd.equals("load") )
            returnMsg = "!load <module>  -  Loads <module>, which can be a game or utility.  See !listgames for a list of games, and !listutils for a list of utilities. (Shortcut: !l)";
        else if( cmd.equals("unload") )
            returnMsg = "!unload <module>  -  Unloads <module>, if it is loaded; <module> can be a game or utility.  Use !loaded to see which games and utilities are loaded. (Shortcut: !ul)";
        else if( cmd.equals("unloadall") )
            returnMsg = "!unloadall  -  Unloads all games and utilities.  Use !loaded to see which games and utilities are loaded. (Shortcut: !ula, !off)";
        else if( cmd.equals("loaded") )
            returnMsg = "!loaded  -  Shows the loaded game, if any, and all loaded utilities. (Shortcut: !ll)";
        else if( cmd.equals("gtfo") )
            returnMsg = "!gtfo  -  Unlocks the bot, sends it back to the park, and unloads all modules.  Also releases you as the owner of the bot. (Shortcut: !!)";
        else if( cmd.equals("help") )
            returnMsg = "!help <module>  -  Shows details help on any loaded game or utility.  Use !loaded to see which games and utils have been loaded. (Shortcut: ?)";
        else if( cmd.equals("modhelp") )
            returnMsg = "!modhelp  -  Shows just help for the currently loaded game, without displaying general bot commands. (Shortcut: !mh)";
        else if( cmd.equals("mybot") )
            returnMsg = "!mybot  -  Claims the bot as yours, taking it from the present owner. (Shortcut: !my)  If a bot is unclaimed, just issuing a command will claim it (no need to !mybot).  Also: !gtfo will release ownership. ";
        else if( cmd.equals("free") )
            returnMsg = "!free  -  Relinquishes control of the bot so that the next person that uses it becomes the owner. (Shortcut: !fr)";
        else if( cmd.equals("override") )
            returnMsg = "!override  -  Overrides the owner rule, allowing any staff to issue commands to the bot.  !override again to turn off.  (Shortcut: !or)";
        else if( cmd.equals("die") )
            returnMsg = "!die  -  Gives me a pair of cement shoes off a fishing boat in the north Pacific.";

        m_botAction.sendSmartPrivateMessage(sender, returnMsg );
    }

    /**
     * Just displays help for the currently loaded game.  For backwards compatibility
     * and for those that don't want to see the normal cmds each time.
     *
     * @param sender
     *            is the player that sent the help command.
     */
    private void doGameModuleHelpMessage(String sender) {
        m_botAction.smartPrivateMessageSpam(sender, help_standard);
        if( eventModuleLoaded() )
            m_botAction.smartPrivateMessageSpam( sender, m_eventModule.getModHelpMessage() );
        else
            m_botAction.sendSmartPrivateMessage(sender, "No game is loaded.  Use !loaded to see a list of loaded games.");
    }



    // ***** Support methods *****

    /**
     * @return true
     *             is returned if an event module is loaded.
     */
    private boolean eventModuleLoaded() {
        return m_eventModule != null;
    }

    /**
     * This method unloads the loaded module by calling the MultiModules cancel
     * method.
     */
    private void unloadEventModule() {
        //modEventReq.releaseAll(multiModule);
        m_eventModule.cancel();
        m_eventModule = null;
        setChat(m_botChat);
    }

    /**
     * Loads the TWScript utilities.
     */
    private void loadTWScript(String name, boolean quiet) {
    	try{
    		File f = new File(coreRoot + "/twcore/bots/multibot/twscript");
    		String[] l = f.list();
    		for(int i=0;i<l.length;i++){
    			l[i] = l[i].replace(".class", "");
    			for(int z=0;z<l[i].length();z++)
    				if(java.lang.Character.isUpperCase(l[i].charAt(z)) || l[i].charAt(z) == '$' || l[i].contains("twscript"))
    					l[i] = "";
    		}
    		String twslocation = "twcore.bots.TWScript";
    		TWScript tws = new TWScript();
    		MultiUtil twsUtil = (MultiUtil) tws;
    		twsUtil.initialize(m_botAction, m_modEventReq);
    		m_utils.put(twslocation, twsUtil);
    		for(int i=0;i<l.length;i++){
    			if(!l[i].equals("")){
    				MultiUtil util = (MultiUtil) m_loader.loadClass("twcore.bots.multibot.twscript." + l[i]).newInstance();
    				util.initialize(m_botAction, m_modEventReq, tws);
    				m_utils.put(l[i], util);
    			}
    		}
    		if(!quiet){
    			String utilityList = "Loaded TWScript utilities: ";
    			for(String u:l){
    				if(!u.equals(""))
    					utilityList += u + ", ";
    			}
    			utilityList = utilityList.substring(0, utilityList.length() - 2) + ".";
    			m_botAction.sendSmartPrivateMessage( name, utilityList);
    		}
    	}catch(Exception e){
    		Tools.printStackTrace(e);
    	}
    }
    
    /**
     * Unloads the TWScript utilities.
     */
    private void unloadTWScript() {
    	try{
    		File f = new File(coreRoot + "/twcore/bots/multibot/twscript");
    		String[] l = f.list();
    		for(int i=0;i<l.length;i++){
    			l[i] = l[i].replace(".class", "");
    			for(int z=0;z<l[i].length();z++)
    				if(java.lang.Character.isUpperCase(l[i].charAt(z)) || l[i].charAt(z) == '$')
    					l[i] = "";
    		}
    		for(int i=0;i<l.length;i++){
    			if(m_utils.containsKey(l[i])){
    				MultiUtil removedutil = m_utils.remove(l[i]);
    	            removedutil.cancel();
    			}
    		}
    	}catch(Exception e){
    		Tools.printStackTrace(e);
    	}
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


    // Event handling

    /**
     * This method handles all SubspaceEvents. If the bot is locked, then the
     * event is passed onto the module.
     *
     * @param event
     *            is the event to handle.
     */
    public void handleEvent(SubspaceEvent event) {
        try {
            if( eventModuleLoaded() )
                m_eventModule.handleEvent(event);
        } catch (Exception e) {
            m_botAction.sendChatMessage(MultiModule.FIRST_CHAT, "Event Module unloaded due to exception.  See log for details.");
            unloadEventModule();
            Tools.printStackTrace(e);
        }

        try {
            Iterator<Map.Entry<String, MultiUtil>> i = m_utils.entrySet().iterator();
            while( i.hasNext() ) {
                Map.Entry<String, MultiUtil> entry =  i.next();
                MultiUtil util = (MultiUtil) entry.getValue();
                util.handleEvent(event);
            }
        } catch (Exception e) {
            m_botAction.sendChatMessage(1, "Uncaught exception in utility; all utilties unloaded.  See log for details.");
            doUnloadAllCmd((String) null, false );
            Tools.printStackTrace(e);
        }
    }

    public void handleEvent(ArenaJoined event) {
    	m_botAction.moveToTile(512, 512);
        m_botAction.setReliableKills(1);
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(ArenaList event) {
        handleEvent((SubspaceEvent) event);
    }

    public void handleEvent(PlayerPosition event) {
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

    public void handleEvent(KotHReset event) {
        if(event.isEnabled() && event.getPlayerID()==-1) {
            // Make the bot ignore the KOTH game (send that he's out immediately after restarting the game)
            m_botAction.endKOTH();
        }
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

    public void handleEvent(PlayerBanner event) {
        handleEvent((SubspaceEvent) event);
    }
    
    private class DieTask extends TimerTask {
        String m_initiator;

        public DieTask( String name ) {
            super();
            m_initiator = name;
        }

        public void run() {
            m_botAction.die( "!die initiated by " + m_initiator );
        }
    }    
}