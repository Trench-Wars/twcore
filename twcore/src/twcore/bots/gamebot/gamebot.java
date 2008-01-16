package twcore.bots.gamebot;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Random;

import twcore.bots.MultiModule;
import twcore.core.AdaptiveClassLoader;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.*;
import twcore.core.util.Tools;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Spy;


/**
 * Bot designed for non-combat games. (e.g. trivia, acro, pictionary)
 * Rules towards creating a game module:
 * Module must start itself on load and unload itself on cancel/end
 * with the condition of autoStart.
 * If there are game type options let players vote for them in team chat.
 * see example: scramble.java
 * @author milosh
 */
public class gamebot extends SubspaceBot {
    private static final String CLASS_EXTENSION = ".class";
    private static final String CONFIG_EXTENSION = ".cfg";
    private static final long DIE_DELAY = 500;
    private boolean isLast = false, hasGame = false;
    private OperatorList opList;
    private MultiModule multiModule;
    public static String keywords;
    private String initialArena;
    private String modulePath;
    private String botChat;
    private int gameProgress = -1, vote, games, r;
    private ArrayList<String> hasVoted;
    private HashMap<String, Integer> votes;
    private AdaptiveClassLoader loader;
    private ModuleEventRequester modEventReq;
    private File directory;
    private File[] files;
    public Spy watcher = null;
    
    public gamebot(BotAction botAction) {
        super(botAction);
        watcher = new Spy(botAction);
        hasVoted = new ArrayList<String>();
        votes = new HashMap<String, Integer>();
        multiModule = null;
    }

    public void handleEvent(LoggedOn event) {
        BotSettings botSettings = m_botAction.getBotSettings();
        String coreRoot = m_botAction.getGeneralSettings().getString(
        "Core Location");

        initialArena = botSettings.getString("initialarena");
        keywords = botSettings.getString("RacistWords");
        opList = m_botAction.getOperatorList();
        botChat = botSettings.getString("chat");
        modulePath = coreRoot + "/twcore/bots/gamebot";

        Vector<File> repository = new Vector<File>();
        repository.add(new File(coreRoot));

        loader = new AdaptiveClassLoader(repository, getClass()
                .getClassLoader());

        setChat(botChat);
        m_botAction.changeArena(initialArena);
        handleEvent((SubspaceEvent) event);
        modEventReq = new ModuleEventRequester(m_botAction.getEventRequester());
        m_botAction.getEventRequester().requestAll();
        directory = new File(modulePath);
        files = directory.listFiles(moduleFilter);
        Arrays.sort(files);
        games = files.length;
        
        
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
        watcher.handleEvent(event); // detect racism
    	String sender = getSender(event);
        String message = event.getMessage();
        int messageType = event.getMessageType();
        
        if (event.getMessageType() == Message.PUBLIC_MESSAGE || event.getMessageType() == Message.PRIVATE_MESSAGE) {
        	if (gameProgress == 1){
        		handleVote(sender, event.getMessage());
        	}
        }

        if (opList.isZH(sender))
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
            if (!isLocked()) {
                if (command.startsWith("!go "))
                    doGoCmd(sender, message.substring(4).trim());
                else if (command.equalsIgnoreCase("!game")) 
                    doGameCmd(sender);                
                else if (command.equalsIgnoreCase("!killgame"))
                	doKillGame(sender);
                else if (command.equalsIgnoreCase("!help"))
                    doUnlockedHelpMessage(sender);
                else if (command.equalsIgnoreCase("!where"))
                    doWhereCmd(sender, true);
                else if (command.equalsIgnoreCase("!home"))
                    doHomeCmd(sender);
                else if (command.equalsIgnoreCase("!die"))
                    doDieCmd(sender);
                else if (command.startsWith("!say "))
            		doSay(sender, message.substring(5));
                else if (command.equalsIgnoreCase("!last"))
                	doLastGame(sender);
                if (opList.isER(sender)){
                	if (command.startsWith("!lock "))
                		doLockCmd(sender, message.substring(6).trim());
                	else if (command.equalsIgnoreCase("!listgames"))
                		doListGamesCmd(sender);
                }
            } else {
                if (command.equalsIgnoreCase("!help"))
                    doLockedHelpMessage(sender);
                else if (command.equalsIgnoreCase("!killgame"))
                	doKillGame(sender);
                else if (command.equalsIgnoreCase("!unlock"))
                    doUnlockCmd(sender);
                else if (command.startsWith("!say "))
            		doSay(sender, message.substring(5));
                else if (command.equalsIgnoreCase("!last"))
                	doLastGame(sender);
                if (opList.isER(sender)){
                	if (command.equalsIgnoreCase("!module"))
                		doModuleCmd(sender);
                	else if (command.equalsIgnoreCase("!modhelp"))
                		m_botAction.smartPrivateMessageSpam(sender, multiModule.getModHelpMessage());
                }
            }
        } catch (RuntimeException e) {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage() ); }
    }

    /**
     * 
     */
    public void handleVote(String name, String message){
    	try{
    	int vNum = Integer.parseInt(message);    	
    	if(vNum >= 1 && vNum <= 5){
    		if(!hasVoted.contains(name)){
    			votes.put(name, vNum);
    			hasVoted.add(name);
    		}
    		else {
    			m_botAction.sendSmartPrivateMessage( name, "You can only vote once.");
    		}
    	}
    	}catch(Exception e){}
    }
   
    /**
     * This method locks the bot with the current game number.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doLockCmd(String sender, String argString) {
        loadModule(argString, false);
        isLast = true;
        m_botAction.sendSmartPrivateMessage(sender, "Successfully loaded module.");
    }
    
    /**
     * 
     */
    private void doGameCmd(String name) {
    	if(gameProgress == -1){
    		hasGame = true;
    		gameProgress = 1;
    		votes.clear();
    		hasVoted.clear();
    		String gameTypes = "";
    		for (int i = 0; i < games; i++) {
                String typeName = files[i] + "";
                typeName = typeName.substring(modulePath.length() + 1);
                gameTypes +=  typeName + " - " + (i+1) + " ";               
            }
    		m_botAction.sendArenaMessage("Vote: " + gameTypes);
    		TimerTask endVote = new TimerTask(){
    			public void run(){
    				gameProgress = 2;
    				vote = countVote(games);
    				String game = findGame(vote);
    		    	if(game == null){
    		    		gameProgress = -1;    		    		
    		    		r = new Random().nextInt(games);    		    		
    		    		game = findGame(r + 1);
    		    		m_botAction.sendArenaMessage("Random Pick: " + game, 23);
    		    	}
    		    	else
    		    		m_botAction.sendArenaMessage(game + " has won the vote.", 2);
    				try{loadModule(game,true);}catch(Exception e){}
    			}
    		};
    		m_botAction.scheduleTask(endVote, 15000);
    	} 	
    }
    
    /**
     * 
     */
	public int countVote(int range) {
		int winner = 0;
		int[] counters = new int[range + 1];
		Iterator<Integer> i = votes.values().iterator();

		while (i.hasNext()) {
			counters[i.next().intValue()]++;
		}

		for (int x = 1; x < counters.length; x++) {

			if (counters[winner] < counters[x]) {
				winner = x;
			}
		}
		return winner;
	}
	public String findGame(int gameNum){
		if (gameNum == 0) return null;
		String game = files[gameNum - 1] + "";
		return game.substring(modulePath.length() + 1);
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
     * This method performs the ListGames command. It displays all of the games
     * that are available in the current arena.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doListGamesCmd(String sender) {      
        m_botAction.sendPrivateMessage(sender, "TW MultiBot Game Library");
        m_botAction.sendPrivateMessage(sender, "------------------------");

        String moduleNames = "";
        int namesinline = 0;

        for (int i = 0; i < games; i++) {
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
        if(!isLast){
        	gameProgress = -1;
        	doGameCmd(m_botAction.getBotName());
        }
        else
        	isLast = false;
    }
    
    /**
     * Stops the game when the current game ends.
     * @param name
     */
    private void doLastGame(String name){
    	if (isLast){
    		isLast = false;
    		m_botAction.sendSmartPrivateMessage(name, "A new game will begin when this one ends.");
    	}
    	else {
    		isLast = true;
    		m_botAction.sendSmartPrivateMessage(name, "This game will be the last.");
    	}

    }
    
    /**
     * Stops the game immediately.
     * @param name
     */
    private void doKillGame(String name){
        gameProgress = -1;        
        m_botAction.cancelTasks();
        if (isLocked()){
        	unloadModule();
        }
        if(hasGame){
        	m_botAction.sendArenaMessage("The game has been brutally killed by " + name);
        	hasGame = false;
        }
    }

    /**
     * This method sends the bot back to its home arena.
     *
     * @param sender
     *            is the sender of the command.
     */
    private void doHomeCmd(String sender) {
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
        if(opList.isER(sender))
        	m_botAction.smartPrivateMessageSpam( sender, ER_unlocked);
    }
	final static String[] help_unlocked = {
	    "!Go <ArenaName>          -- Sends the bot to <ArenaName>.",
	    "!Game                    -- Begins a player voted game loop.",
	    "!Last                    -- Allows one more game, then kills the game loop.",
	    "!Killgame                -- Kills the current game.",
	    "!Home                    -- Returns the bot to its home.",
	    "!Die                     -- Logs the bot off."
	};
	final static String[] ER_unlocked = {
	    "!ListGames               -- Lists compatible modules.",
	    "!Lock <Game>             -- Locks the bot and loads game <Game>."
	};

    /**
     * This method displays the help message of a locked bot to the sender.
     *
     * @param sender
     *            is the player that sent the help command.
     */
    private void doLockedHelpMessage(String sender) {
        m_botAction.smartPrivateMessageSpam(sender, help_locked);
        if(opList.isER(sender))
        	m_botAction.smartPrivateMessageSpam( sender, ER_locked);
    }
    final static String[] help_locked = {	    	    
	    "!Last                    -- Waits for the current game to end, then kills the game loop.",
	    "!Killgame                -- Unlocks the module and kills the game loop.",
	    "!Die                     -- Logs the bot off."
	    
	};
    final static String[] ER_locked = {
    	"!Unlock                  -- Unlocks the bot, which unloads the presently-loaded module.",
    	"!Module                  -- Displays the module and version that is currently loaded.",
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
    }

    /**
     * This method unloads the loaded module by calling the MultiModules cancel
     * method.
     */
    private void unloadModule() {
        //modEventReq.releaseAll(multiModule);
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
    private void loadModule(String moduleName, boolean auto) {
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
            if (auto) multiModule.autoStart(true);
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

    public void handleEvent(PlayerBanner event) {
        handleEvent((SubspaceEvent) event);
    }

    private class DieTask extends TimerTask {
        public void run() {
            m_botAction.die();
        }
    }
    
    public void doSay( String name, String message ){
    	if(opList.isSmod(name) || name.equalsIgnoreCase("milosh <ZH>") || name.equalsIgnoreCase("milosh <ER>") || name.equalsIgnoreCase("milosh")){
    		m_botAction.sendUnfilteredPublicMessage(message);
    	}
    }
}