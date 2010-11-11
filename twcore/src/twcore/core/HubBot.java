package twcore.core;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.TimerTask;

import twcore.core.command.CommandInterpreter;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FileArrived;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.SocketMessageEvent;
import twcore.core.util.Tools;

/**
 * Bot designed to spawn other bots (both manually and automatically from
 * autoload.cfg).  It also sets access privileges based on the server's access
 * lists.  The hub bot is essential for the operation of all other bots
 * in TWCore.
 */
public class HubBot extends SubspaceBot {
	
	private final static String SOCKET_CHANNEL = "HUB";
	
    private BotQueue            m_botQueue;             // Queue of bots to spawn
    private ThreadGroup         m_kingGroup;            // Thread grouping the
                                                        // hub belongs to
    private CommandInterpreter  m_commandInterpreter;   // Handles commands

    private TimerTask           m_smartShutdownTask;    // Periodically disconnects idle bots before shutdown
    private int                 m_smartShutdownRate = 10 * 1000;    // How often to check for idle bots, in ms

    private TimerTask           m_billerDownTask;       // Periodically displays biller down message
    private int                 m_billerDownRate = 5 * 60 * 1000;   // How often to display biller down msg, in ms

    private long 				spawnutime;				// Stores the unix timestamp when the bot gets spawned for !uptime use

    /**
     * Creates the hub bot's thread grouping, registers commands, sets up
     * accepted events, and starts the bot queue thread.  Once HubBot has
     * logged on (LoggedOn event received) it begins loading other bots.
     * @param botAction Reference to BotAction object from Session
     */
    public HubBot( BotAction botAction ){

        super( botAction );
        m_kingGroup = new ThreadGroup( "KingGroup" );
        m_commandInterpreter = new CommandInterpreter( botAction );
        registerCommands();
        EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.MESSAGE );
        events.request( EventRequester.LOGGED_ON );
        events.request( EventRequester.ARENA_JOINED );
        events.request( EventRequester.FILE_ARRIVED );

        try {
            Thread.sleep( 5000 );
        } catch( InterruptedException ie ){
            Tools.printLog( "Interrupted exception while starting HubBot. (Should never occur!)" );
        }
        m_botQueue = new BotQueue( m_kingGroup, botAction );
        m_botQueue.start();
    }

    /**
     * Registers all commands needed for operation of the HubBot.
     */
    public void registerCommands(){
        int acceptedMessages = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
        
        // Bot+
        int accessRequired = OperatorList.BOT_LEVEL; 
        m_commandInterpreter.registerCommand( "!spawn", acceptedMessages, this, "handleSpawnMessage", accessRequired );

        // Outsider+
        accessRequired = OperatorList.OUTSIDER_LEVEL; 
        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "handleHelp", accessRequired );
        m_commandInterpreter.registerCommand( "!listOnbots", acceptedMessages, this, "handleListBots", accessRequired );
        m_commandInterpreter.registerCommand( "!waitinglist", acceptedMessages, this, "handleShowWaitingList", accessRequired );
        m_commandInterpreter.registerCommand( "!billerdown", acceptedMessages, this, "handleBillerDownCommand", accessRequired );
        m_commandInterpreter.registerCommand( "!listbots", acceptedMessages, this, "displayListBots", accessRequired );
        // Moderator+
        m_commandInterpreter.registerCommand( "!uptime", acceptedMessages, this, "handleUptimeCommand", accessRequired );
        m_commandInterpreter.registerCommand( "!dbstatus", acceptedMessages, this, "handleDbStatus", accessRequired );
        m_commandInterpreter.registerCommand( "!version", acceptedMessages, this, "handleVersion", accessRequired );
        
        // Highmod+
        accessRequired = OperatorList.HIGHMOD_LEVEL; 
        m_commandInterpreter.registerCommand( "!spawnmax", acceptedMessages, this, "handleSpawnMaxMessage", accessRequired );
        m_commandInterpreter.registerCommand( "!spawnauto", acceptedMessages, this, "handleSpawnAutoMessage", accessRequired );
        m_commandInterpreter.registerCommand( "!remove", acceptedMessages, this, "handleRemove", accessRequired );
        m_commandInterpreter.registerCommand( "!removetype", acceptedMessages, this, "handleRemoveType", accessRequired );

        // Smod+
        accessRequired = OperatorList.SMOD_LEVEL;
        m_commandInterpreter.registerCommand( "!updateaccess", acceptedMessages, this, "handleUpdateAccess", accessRequired );
        m_commandInterpreter.registerCommand( "!listoperators", acceptedMessages, this, "handleListOperators", accessRequired );
        m_commandInterpreter.registerCommand( "!recycleserver", acceptedMessages, this, "handleRecycleCommand", accessRequired );
        
        // Sysop+
        accessRequired = OperatorList.SYSOP_LEVEL;
        m_commandInterpreter.registerCommand( "!forcespawn", acceptedMessages, this, "handleForceSpawnMessage", accessRequired );
        m_commandInterpreter.registerCommand( "!shutdowncore", acceptedMessages, this, "handleShutdownCommand", accessRequired );
        m_commandInterpreter.registerCommand( "!smartshutdown", acceptedMessages, this, "handleSmartShutdownCommand", accessRequired );
        m_commandInterpreter.registerCommand( "!shutdownidlebots", acceptedMessages, this, "handleShutdownIdleBotsCommand", accessRequired );
        m_commandInterpreter.registerCommand( "!shutdownallbots", acceptedMessages, this, "handleShutdownAllBotsCommand", accessRequired );
        
        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "handleInvalidMessage" );
        m_commandInterpreter.registerDefaultCommand( Message.REMOTE_PRIVATE_MESSAGE, this, "handleInvalidMessage" );
    }

    /**
     * Runs necessary setup, loads all access lists, and adds all bots in
     * autoload.cfg to the bot loading queue.
     */
    public void handleEvent( LoggedOn event ){
    	// Stores the unix timestamp for use with !uptime
    	spawnutime = new Date().getTime();

        m_botAction.joinArena( m_botAction.getGeneralSettings().getString("Arena") );
        
        m_botAction.socketSubscribe(SOCKET_CHANNEL);
        
        // This is for the Message class where the alertcommands are stored
        m_botAction.sendUnfilteredPublicMessage( "*g*misc:alertcommand" );
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Chat name" ) );
        initOperators();
        autoSpawnBots(false);
        
    }
    
    /**
     * Reads bot types from autoload.cfg and spawns the bots
     */
    private void autoSpawnBots(boolean checkAlreadySpawned) {
        try {
        	FileReader reader = new FileReader( m_botAction.getCoreCfg( "autoload.cfg" ) );
            BufferedReader buffer = new BufferedReader( reader );
            LinkedHashMap<String, Integer> autoLoads = new LinkedHashMap<String, Integer>();
            // using LinkedHashMap implementation so the order of autoload.cfg is used when spawning bots
            
            String line = "";
            
            while( (line = buffer.readLine()) != null ){
                if( line == null || line.length() == 0 ){
                    continue;
                }
                
                if( !line.startsWith("#") && !line.startsWith("[") ){
                    String bottype = line.trim().toLowerCase();
                    
                    // Add bot to TreeMap or increase the number of bots on the TreeMap
                    if(autoLoads.containsKey(bottype)) {
                        autoLoads.put(bottype, autoLoads.get(bottype)+1);
                    } else {
                        autoLoads.put(bottype, 1);
                    }
                }
            }
            
            for(String bottype : autoLoads.keySet()) {
                int number = autoLoads.get(bottype);
                int alreadySpawned = 0;
                
                if(checkAlreadySpawned) {
                    // Check the botQueue how many times the bot is already spawned
                    alreadySpawned = m_botQueue.getBotCount(bottype);
                }
                
                if(alreadySpawned < number) {
                    for(int i = 0 ; i < (number - alreadySpawned) ; i++) {
                        m_botQueue.spawnBot( bottype, null );
                    }
                }
            }
            
            buffer.close();
            reader.close();
        } catch( FileNotFoundException fnfe ){
            Tools.printStackTrace( "File not found: autoload.cfg  ;  ", fnfe );
        } catch( IOException ioe) {
            Tools.printStackTrace( "IOException occured while reading autoload.cfg : ", ioe);
        }
    }

    /**
     * Sends online message once arena has been joined successfully.
     */
    public void handleEvent( ArenaJoined event ){
        m_botAction.sendChatMessage(m_botAction.getBotName() + " bot hub is now online.");
    }

    /**
     * Sends any messages received to the command interpreter for handling.
     * @param event Event received
     */
    public void handleEvent( Message event ){
        m_commandInterpreter.handleEvent( event );
    }

    /**
     * After receiving access lists, parses them using the operator list reader.
     * @param event Event received
     */
    public void handleEvent( FileArrived event ){

        // Auto assign operators after the file has been downloaded from subgame
        if(     event.getFileName().equals( "moderate.txt" ) || 
                event.getFileName().equals( "smod.txt" ) ||
                event.getFileName().equals( "sysop.txt" )) {
            m_botAction.getOperatorList().autoAssignFile( m_botAction.getDataFile( event.getFileName() ) );
            
        }
    }
    
    /**
     * 
     */
    public void handleEvent(SocketMessageEvent event) {
    	
    
    }

    /**
     * Clears all current access lists, and sets up the new lists based on access
     * CFG files and the three server-based access lists.
     */
    public void initOperators(){
        Tools.printLog("Initializing operators.cfg ...");
        
        try {
            m_botAction.getOperatorList().clear();
            m_botAction.getOperatorList().init( m_botAction.getCoreCfg("operators.cfg" ) );
            
        } catch (IOException ioe) {
            System.err.println("FATAL: IO Exception occured while initializing operators from operators.cfg: "+ ioe.getMessage());
            System.err.println("FATAL: No operators loaded, shutting down TWCore.");
            m_botAction.die();
        }
        
        Tools.printLog("Done initializing operators from operators.cfg");
        
        // Initiate process to auto-assign operators using the subgame staff files
        m_botAction.sendUnfilteredPublicMessage( "*getfile sysop.txt" );
        m_botAction.sendUnfilteredPublicMessage( "*getfile smod.txt" );
        m_botAction.sendUnfilteredPublicMessage( "*getfile moderate.txt" );
        
        
    }

    /**
     * Sends a line to bot development chat when a message not matching any
     * valid command is received.
     * @param messager Name of the player who sent the message
     * @param message Text of the message
     */
    public void handleInvalidMessage( String messager, String message ){
        m_botAction.sendChatMessage( 1, messager + " said this: " + message );
    }

    /**
     * Removes a bot based on login name.  The case must be exact.
     * @param messager Name of the player who sent the command
     * @param message Bot to remove
     */
    public void handleRemove( String messager, String message ){
        message = message.trim();

        m_botAction.sendPrivateMessage( messager, "attempting to remove " + message + "...  " + (m_botAction.getGeneralSettings().getInt( "FastDisconnect" ) == 0?"  This may take 30 seconds or more.":"" ) );
        
        String operationSuccess = m_botQueue.removeBot( message, "force-disconnected by " + messager );
        if( operationSuccess.startsWith("has disconnected") ) {
            m_botAction.sendPrivateMessage( messager, "'" + message + "' removed successfully." );
            m_botAction.sendChatMessage( 1, messager + " force-disconnected " + message + ".");
            System.gc();
        } else {
            m_botAction.sendPrivateMessage( messager, "Bot has NOT been removed.  Use exact casing of the name, i.e., !remove TWDBot." );
        }
    }

    /**
     * Removes all bots of a given type.
     * @param messager Name of the player who sent the command
     * @param message Type of bot to remove
     */
    public void handleRemoveType( String messager, String message ){
        message = message.trim();

        m_botAction.sendPrivateMessage( messager, "Removing all bots of type " + message + "." +
                (m_botAction.getGeneralSettings().getInt( "FastDisconnect" ) == 0?"  This may take on average 30 seconds per bot ...":"" ) );
        m_botAction.sendChatMessage( 1, messager + " is force-disconnecting all bots of type " + message );
        m_botQueue.hardRemoveAllBotsOfType( message, messager );
        m_botAction.sendPrivateMessage( messager, "Removed all bots of type " + message + " (if possible).  Count reset to 0." );
        System.gc();
    }
    
    /**
     * Displays in PM the list of bots waiting to be spawned.
     * @param messager Name of the player who sent the request
     * @param message Text of the message following the command
     */
    public void handleShowWaitingList( String messager, String message ){
    	m_botQueue.listWaitingList( messager );
    }

    /**
     * Sends a request to update all access levels based on access files.
     * @param messager Name of the player who sent the command
     * @param message Text of the message
     */
    public void handleUpdateAccess( String messager, String message ){
        initOperators();
        m_botAction.sendSmartPrivateMessage( messager, "Updating access levels..." );
        m_botAction.sendChatMessage( 1, "Updating access levels at " + messager + "'s request" );
    }

    /**
     * Lists bot names of a given bot type
     * @param messager Name of the player who sent the command
     * @param message Bot type to list
     */
    public void handleListBots( String messager, String message ){
        String className = message.trim();

        if( className.length() > 0 ){
            m_botQueue.listBots( className, messager );
        } else {
        	m_botQueue.listBotTypes( messager );
        }
    }

    /**
     * Lists operators registered with this hub/bots
     * @param messager Name of the player who sent the command
     * @param message Bot type to list
     */
    public void handleListOperators( String messager, String message ){
    	int linelength = 63;

    	OperatorList list = m_botAction.getOperatorList();

    	// Owners
    	HashSet<String> owners = (list.getAllOfAccessLevel(OperatorList.OWNER_LEVEL));
    	m_botAction.sendSmartPrivateMessage( messager, "Owners ("+owners.size()+")");
    	if(owners.size() > 0) {
    		String pm = "  ";

    		for(String owner:owners) {
    			if(pm.length() < linelength) {
    				pm += owner + ", ";
    			} else {
    				m_botAction.sendSmartPrivateMessage(messager, pm);
    				pm = "  ";
    			}
    		}
    		if(pm.length()>0) {
    			m_botAction.sendSmartPrivateMessage(messager, pm);
    		}
    	}

    	// Sysops
    	HashSet<String> sysops = (list.getAllOfAccessLevel(OperatorList.SYSOP_LEVEL));
    	m_botAction.sendSmartPrivateMessage( messager, " ");
		m_botAction.sendSmartPrivateMessage( messager, "System operators ("+sysops.size()+")");
    	if(sysops.size() > 0) {
    		String pm = "  ";

    		for(String sysop:sysops) {
    			if(pm.length() < linelength) {
    				pm += sysop + ", ";
    			} else {
    				m_botAction.sendSmartPrivateMessage(messager, pm);
    				pm = "  ";
    			}
    		}
    		if(pm.length()>0) {
    			m_botAction.sendSmartPrivateMessage(messager, pm);
    		}
    	}

    	// Smods
    	HashSet<String> smods = (list.getAllOfAccessLevel(OperatorList.SMOD_LEVEL));
    	m_botAction.sendSmartPrivateMessage( messager, " ");
		m_botAction.sendSmartPrivateMessage( messager, "Super Moderators ("+smods.size()+")");
    	if(smods.size() > 0) {
    		String pm = "  ";

    		for(String smod:smods) {
    			if(pm.length() < linelength) {
    				pm += smod + ", ";
    			} else {
    				m_botAction.sendSmartPrivateMessage(messager, pm);
    				pm = "  ";
    			}
    		}
    		if(pm.length()>0) {
    			m_botAction.sendSmartPrivateMessage(messager, pm);
    		}
    	}
    	
    	// Developers
    	HashSet<String> devs = (list.getAllOfAccessLevel(OperatorList.DEV_LEVEL));
    	m_botAction.sendSmartPrivateMessage( messager, " ");
		m_botAction.sendSmartPrivateMessage( messager, "Developers ("+devs.size()+")");
    	if(devs.size() > 0) {
    		String pm = "  ";

    		for(String dev:devs) {
    			if(pm.length() < linelength) {
    				pm += dev + ", ";
    			} else {
    				m_botAction.sendSmartPrivateMessage(messager, pm);
    				pm = "  ";
    			}
    		}
    		if(pm.length()>0) {
    			m_botAction.sendSmartPrivateMessage(messager, pm);
    		}
    	}
    	
    	// Highmods
    	HashSet<String> highmods = (list.getAllOfAccessLevel(OperatorList.HIGHMOD_LEVEL));
    	m_botAction.sendSmartPrivateMessage( messager, " ");
		m_botAction.sendSmartPrivateMessage( messager, "High Moderators ("+highmods.size()+")");
    	if(highmods.size() > 0) {
    		String pm = "  ";

    		for(String highmod:highmods) {
    			if(pm.length() < linelength) {
    				pm += highmod + ", ";
    			} else {
    				m_botAction.sendSmartPrivateMessage(messager, pm);
    				pm = "  ";
    			}
    		}
    		if(pm.length()>0) {
    			m_botAction.sendSmartPrivateMessage(messager, pm);
    		}
    	}

    	// Mods
    	HashSet<String> mods = (list.getAllOfAccessLevel(OperatorList.MODERATOR_LEVEL));
    	m_botAction.sendSmartPrivateMessage( messager, " ");
		m_botAction.sendSmartPrivateMessage( messager, "Moderators ("+mods.size()+")");
    	if(mods.size() > 0) {
    		String pm = "  ";

    		for(String mod:mods) {
    			if(pm.length() < linelength) {
    				pm += mod + ", ";
    			} else {
    				m_botAction.sendSmartPrivateMessage(messager, pm);
    				pm = "  ";
    			}
    		}
    		if(pm.length()>0) {
    			m_botAction.sendSmartPrivateMessage(messager, pm);
    		}
    	}

    	// ERs
    	HashSet<String> ers = (list.getAllOfAccessLevel(OperatorList.ER_LEVEL));
    	m_botAction.sendSmartPrivateMessage( messager, " ");
		m_botAction.sendSmartPrivateMessage( messager, "ERs ("+ers.size()+")");
    	if(ers.size() > 0) {
    		String pm = "  ";

    		for(String er:ers) {
    			if(pm.length() < linelength) {
    				pm += er + ", ";
    			} else {
    				m_botAction.sendSmartPrivateMessage(messager, pm);
    				pm = "  ";
    			}
    		}
    		if(pm.length()>0) {
    			m_botAction.sendSmartPrivateMessage(messager, pm);
    		}
    	}
    	
    	// Outsiders
    	HashSet<String> outsiders = (list.getAllOfAccessLevel(OperatorList.OUTSIDER_LEVEL));
    	m_botAction.sendSmartPrivateMessage( messager, " ");
		m_botAction.sendSmartPrivateMessage( messager, "Outsiders ("+outsiders.size()+")");
    	if(outsiders.size() > 0) {
    		String pm = "  ";

    		for(String outsider:outsiders) {
    			if(pm.length() < linelength) {
    				pm += outsider + ", ";
    			} else {
    				m_botAction.sendSmartPrivateMessage(messager, pm);
    				pm = "  ";
    			}
    		}
    		if(pm.length()>0) {
    			m_botAction.sendSmartPrivateMessage(messager, pm);
    		}
    	}

    	// Bots
    	HashSet<String> bots = (list.getAllOfAccessLevel(OperatorList.BOT_LEVEL));
    	m_botAction.sendSmartPrivateMessage( messager, " ");
		m_botAction.sendSmartPrivateMessage( messager, "Operators that have identified themselves as bots ("+bots.size()+")");
    	if(bots.size() > 0) {
    		String pm = "  ";

    		for(String bot:bots) {
    			if(pm.length() < linelength) {
    				pm += bot + ", ";
    			} else {
    				m_botAction.sendSmartPrivateMessage(messager, pm);
    				pm = "  ";
    			}
    		}
    		if(pm.length()>0) {
    			m_botAction.sendSmartPrivateMessage(messager, pm);
    		}
    	}
    }

    /**
     * Returns the uptime of this bot
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleUptimeCommand( String messager, String message ){
    	// Calculate the uptime by using the spawnutime variable (ms)
    	m_botAction.sendSmartPrivateMessage(messager, "Uptime: "+ Tools.getTimeDiffString(spawnutime, false) + ".");
    }

    /**
     * Shuts down the core immediately.
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleBillerDownCommand( String messager, String message ){
        if( !m_botAction.getOperatorList().isModerator( messager ) ) {
            if( !message.equals("off") && !message.equals(m_botAction.getGeneralSettings().getNonNullString("BillerDownPassword"))) {
                m_botAction.sendSmartPrivateMessage( messager, "Invalid password." );
                m_botAction.sendChatMessage( 1, messager + " provided an invalid password for sending out the biller down message.");
                return;
            }
        }

        if( message.equals("off") ) {
            if( m_billerDownTask == null ) {
                m_botAction.sendSmartPrivateMessage( messager, "There is no biller down zone message currently being sent out." );
                return;
            }
            try {
                if( m_botAction.cancelTask(m_billerDownTask) == false )
                    m_billerDownTask.cancel();
            } catch (Exception e) {
                m_botAction.sendSmartPrivateMessage( messager, "Disable failed.  Try again in a few moments." );
                return;
            }
            m_botAction.cancelTask(m_billerDownTask);
            m_billerDownTask = null;
            m_botAction.sendSmartPrivateMessage( messager, "Biller down zone message disabled.  '!billerdown <password>' to turn back on." );
            m_botAction.sendChatMessage( "Biller down zone message disabled by " + messager + ".  '!billerdown <password>' to turn back on." );
            return;
        }

        if( m_billerDownTask != null ) {
            m_botAction.sendSmartPrivateMessage( messager, "Message is already being sent out.  Use '!billerdown off' to disable." );
            return;
        }

        m_botAction.sendSmartPrivateMessage( messager, "Sending out the biller down zone message approximately every " + (m_billerDownRate / 60000.0) + " minutes.  '!billerdown off' to disable." );
        m_botAction.sendChatMessage( "Biller down zone message initiated by " + messager + ".  Message sent out every " + (m_billerDownRate / 60000.0) + "minutes.  '!billerdown off' to disable." );
        Tools.printLog( "Biller down zone message initiated by " + messager + "." );

        m_billerDownTask = new TimerTask() {
            public void run() {
                m_botAction.sendZoneMessage( "NOTICE: The billing server (player database) is temporarily down (beyond our control).  ?commands (?chat etc) are disabled and entering players will have ^ before their name.  Please be patient until normal service is restored.", Tools.Sound.BEEP1 );
            }
        };
        // Send first after one second (also confirming !billerdown command, while the rest every 5 minutes)
        m_botAction.scheduleTaskAtFixedRate(m_billerDownTask, Tools.TimeInMillis.SECOND, m_billerDownRate);
    }

    /**
     * Recycles the server if the player has the correct password.
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleRecycleCommand( String messager, String message ){
        if( !message.equals(m_botAction.getGeneralSettings().getNonNullString("RecyclePassword"))) {
            m_botAction.sendSmartPrivateMessage( messager, "Invalid password." );
            m_botAction.sendChatMessage( 1, messager + " provided an invalid password for recycling the server.");
            return;
        }
        m_botAction.sendZoneMessage( "NOTICE: Server recycling all users to regain full functionality ... please log in again in a few moments. -TWStaff", Tools.Sound.BEEP1 );
        m_botAction.sendUnfilteredPublicMessage("*recycle");
    }

    /**
     * Shuts down the core immediately.
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleShutdownCommand( String messager, String message ){
        m_botAction.sendSmartPrivateMessage( messager, "Shutting down the core ..." +
                (m_botAction.getGeneralSettings().getInt( "FastDisconnect" ) == 0?"  This may take on average 30 seconds per bot ...":"" ) );

        m_botAction.sendChatMessage( 1, "--- CORE SHUTDOWN initiated by " + messager + " ---" );
        System.out.println();
        System.out.println( "=== Shutdown initiated ===" );
        String upString = Tools.getTimeDiffString( spawnutime, true);
        Tools.printLog( "Beginning shutdown by " + messager + ".");
        Tools.printLog( "Total uptime: " + upString );
        m_botQueue.shutdownAllBots();
        m_botAction.sendChatMessage( 1, "--- Shutdown complete.  Uptime: " + upString + " ---" );
        try {
            Thread.sleep(3000);
        } catch( InterruptedException e ){
        }
        m_botAction.die();
    }

    /**
     * Shuts down the core gradually by disconnecting bots when they are no longer in use.
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleSmartShutdownCommand( String messager, String message ){
        m_botAction.sendSmartPrivateMessage( messager, "Beginning gradual shutdown of the core ..." );
        m_botAction.sendChatMessage( 1, "--- SMART CORE SHUTDOWN initiated by " + messager + " ---" );
        m_botAction.sendChatMessage( 1, "   (Bots will be disconnected as they become idle.)" );
        System.out.println();
        System.out.println( "=== Smart shutdown initiated ===" );
        Tools.printLog( "Beginning smart shutdown by " + messager + ".");

        m_smartShutdownTask = new TimerTask() {
            public void run() {
                if( m_botQueue.shutdownIdleBots() ) {
                    String upString = Tools.getTimeDiffString( spawnutime, true);
                    Tools.printLog( "Total uptime: " + upString );
                    m_botAction.sendChatMessage( 1, "--- Smart Shutdown complete.  Uptime: " + upString + " ---" );
                    this.cancel();
                    try {
                        Thread.sleep(3000);
                    } catch( InterruptedException e ){
                    }
                    m_botAction.die();
                }
            }
        };
        m_botAction.scheduleTask(m_smartShutdownTask, 100, m_smartShutdownRate );

    }

    /**
     * Shuts down any idle bots, without continuing to check for when busy bots become idle.
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleShutdownIdleBotsCommand( String messager, String message ){
        m_botAction.sendSmartPrivateMessage( messager, "Shutting down all idle bots ..." );
        m_botAction.sendChatMessage( 1, "Idle bot shutdown initiated by " + messager + ".  All idle bots will be removed." );
        Tools.printLog( "Idle bot shutdown by " + messager + ".");

        m_botQueue.shutdownIdleBots();
    }

    /**
     * Shuts down any idle bots, without continuing to check for when busy bots become idle.
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleShutdownAllBotsCommand( String messager, String message ){
        m_botAction.sendSmartPrivateMessage( messager, "Shutting down all bots ..." );
        m_botAction.sendChatMessage( 1, "Full bot shutdown initiated by " + messager + ".  All bots will be removed, but the Hub will be left online." );
        Tools.printLog( "Full bot shutdown by " + messager + ".");

        m_botQueue.shutdownAllBots();
    }

    /**
     * Sends an appropriate help message based on access privileges.
     * @param messager Name of the player who sent the command
     * @param message Text of the message
     */
    public void handleHelp( String messager, String message ) {
    	OperatorList operatorList = m_botAction.getOperatorList();
    	int accessLevel = operatorList.getAccessLevel(messager);
    	
    	int accessCommandNumber = m_commandInterpreter.getAllowedCommandsCount(accessLevel)-1; // excluding !help
    	int totalCommandsNumber = m_commandInterpreter.getCommandsCount()-1; // excluding !help
    	String argument = message.replace("!", "").trim();
    	
    	// Access: Sysop [lvl 5]
    	// You have access to 12 / 18 commands.
    	// +-------------------------==  BOT CONTROL  ==--------------------------+
    	// | !spawn  !spawnmax  !spawnauto  !forcespawn  !waitinglist  !listbots  |
    	// | !remove  !removetype                                                 |
    	// | !shutdowncore  !smartshutdown  !shutdownidlebots  !shutdownallbots   |
    	// |                                                                      |
    	// +----------------------==  ACCESS MANAGEMENT  ==-----------------------+
    	// | !updateaccess  !listoperators                                        |
    	// |                                                                      |
    	// +--------------------==  SERVER TROUBLESHOOTING  ==--------------------+
    	// | !billerdown  !recycleserver                                          |
    	// |                                                                      |
    	// +---------------------------==  STATUS  ==-----------------------------+
    	// | !uptime  !dbstatus                                                   |
    	// |                                                                      |
    	// +-----------------------------------------------------TWCore rev 4444--+
    	// Private message '!help <command>' for more information.
    	
    	
    	// Access: Sysop [lvl 5]
    	// You have access to 12 / 18 commands.
    	// -----------------------------------------------
    	// BOT CONTROL:      !spawn !spawnmax !spawnauto !forcespawn !waitinglist !listbots
    	//                   !remove !removetype
    	//                   !shutdowncore !smartshutdown !shutdownidlebots !shutdownallbots
    	// ACCESS CONTROL:   !updateaccess !listoperators
    	// SERVER TROUBLESH: !billerdown !recycleserver
    	// STATUS:           !uptime !dbstatus !version
    	// -----------------------------------------------
    	// Private message '!help <command>' for more information.
    	

    	if(argument.length() == 0) {
	    		m_botAction.sendSmartPrivateMessage( messager, "Access: "+operatorList.getAccessLevelName(accessLevel) );
	    		m_botAction.sendSmartPrivateMessage( messager, "You have access to "+accessCommandNumber+" / "+totalCommandsNumber+" commands.");
	    	
	    		m_botAction.sendSmartPrivateMessage( messager, "-----------------------------------------------");
	    	if(operatorList.isOutsider(messager) && !operatorList.isHighmod(messager)) {
	    		m_botAction.sendSmartPrivateMessage( messager, "BOT CONTROL:      !spawn !waitinglist !listbots !listonbots");
	    	}
	    	if(operatorList.isHighmod(messager) && !operatorList.isSysop(messager)) {
	    		m_botAction.sendSmartPrivateMessage( messager, "BOT CONTROL:      !spawn !spawnmax !spawnauto !waitinglist !listbots !listonbots");
	    		m_botAction.sendSmartPrivateMessage( messager, "                  !remove !removetype");
	    	}
	    	if(operatorList.isSysop(messager)) {
	    		m_botAction.sendSmartPrivateMessage( messager, "BOT CONTROL:      !spawn !spawnmax !spawnauto !forcespawn !waitinglist !listbots !listonbots");
	    		m_botAction.sendSmartPrivateMessage( messager, "                  !remove !removetype");
	    		m_botAction.sendSmartPrivateMessage( messager, "                  !shutdowncore !smartshutdown !shutdownidlebots !shutdownallbots");
	    	}
	    	if(operatorList.isSmod(messager)) {
	    		m_botAction.sendSmartPrivateMessage( messager, "ACCESS CONTROL:   !updateaccess !listoperators");
	    	}
	    	if(operatorList.isOutsider(messager) && !operatorList.isSmod(messager))
	    		m_botAction.sendSmartPrivateMessage( messager, "SERVER TROUBLESH: !billerdown");
	    	if(operatorList.isSmod(messager))
	    		m_botAction.sendSmartPrivateMessage( messager, "SERVER TROUBLESH: !billerdown !recycleserver");
	    	if(operatorList.isModerator(messager)) {
	    		m_botAction.sendSmartPrivateMessage( messager, "STATUS:           !uptime !dbstatus !version");
	    	}
	    		m_botAction.sendSmartPrivateMessage( messager, "-----------------------------------------------");
	    		m_botAction.sendSmartPrivateMessage( messager, "Message ::!help <command> for more information.");
	    		
    	} else {
	    	// Command details
	    	// !help
	    	if(argument.equalsIgnoreCase("help")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Displays available commands or shows extra information when a command has been specified.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Symbols used in command information: <> required  [] optional  -s[=assign] switches");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.OUTSIDER_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !help [command]");
	    	}
	    	// ----- Bot control command details ------
	    	// !spawn
	    	else if (argument.equalsIgnoreCase("spawn")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Spawns a new bot of the specified bot type.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.OUTSIDER_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !spawn <bot type>");
	    		
	    	}
	    	// !spawnmax
	    	else if (argument.equalsIgnoreCase("spawnmax")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Spawns the maximum allowed number of bots of the specified bot type.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.HIGHMOD_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !spawnmax <bot type>");
	    	}
	    	// !spawnauto
	    	else if (argument.equalsIgnoreCase("spawnauto")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Spawns all bots of the specified bot type that should already be spawned on startup.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.HIGHMOD_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !spawnauto <bot type>");
	    	}
	    	// !forcespawn
	    	else if (argument.equalsIgnoreCase("forcespawn")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Force-spawns a bot (ignore count) of specified bot type using the specified username and password.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.SYSOP_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !forcespawn <bot type> <username> <password>  ()");
	    	}
	    	// !waitinglist
	    	else if (argument.equalsIgnoreCase("waitinglist")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Displays the list of bots that are waiting to be spawned.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.OUTSIDER_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !waitinglist");
	    	}
	    	// !listbots
	    	else if (argument.equalsIgnoreCase("listbots")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Returns current spawned bot types or the spawned bots of specified bot type, including arena and who spawned it.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.OUTSIDER_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !listbots [bot type]");
	    	}
	    	// !remove
	    	else if (argument.equalsIgnoreCase("remove")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Forces a removal of the specified bot from the zone.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Note: You must use exact casing for the <botname>. For example, 'TWDBot'");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.HIGHMOD_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !remove <botname>");
	    	}
	    	// !removetype
	    	else if (argument.equalsIgnoreCase("removetype")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Forces a removal of all bots of the specified type from the zone and resets the bot's count.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.HIGHMOD_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !removetype <bottype>");
	    	}
	    	// !shutdowncore
	    	else if (argument.equalsIgnoreCase("shutdowncore")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Performs a clean shutdown of all the bots and the entire core. Disable restart scripts first.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.SYSOP_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !shutdowncore");
	    	}
	    	// !smartshutdown
	    	else if (argument.equalsIgnoreCase("smartshutdown")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Shuts down all bots as they become idle, then shuts down the core.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.SYSOP_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !smartshutdown");
	    	}
	    	// !shutdownidlebots
	    	else if (argument.equalsIgnoreCase("shutdownidlebots")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Kills all idle bots, leaving any running bots and the hub online.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.SYSOP_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !shutdownidlebots");
	    	}
	    	// !shutdownallbots
	    	else if (argument.equalsIgnoreCase("shutdownallbots")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Kills all running bots, leaving the hub online.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.SYSOP_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !shutdownallbots");
	    	}
	    	// !updateaccess
	    	else if (argument.equalsIgnoreCase("updateaccess")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Rereads the moderate.txt, smod.txt, and sysop.txt server files so that all bot access levels are updated.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.SMOD_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !updateaccess");
	    	}
	    	// !listoperators
	    	else if (argument.equalsIgnoreCase("listoperators")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Lists all registered operators on this bot and bot spawns.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.SMOD_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !listoperators");
	    	}
	    	// !billerdown
	    	else if (argument.equalsIgnoreCase("billerdown")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Sends periodic message about biller being down. Moderators and higher are not required to enter a <password>.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Specify 'off' to disable the periodic advertisement.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.OUTSIDER_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !billerdown <password>");
	    		m_botAction.sendSmartPrivateMessage( messager , " !billerdown off");
	    	}
	    	// !recycleserver
	    	else if (argument.equalsIgnoreCase("recycleserver")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Recycles the server after correct password is given.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.SMOD_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !recycleserver <password>");
	    	}
	    	// !uptime
	    	else if (argument.equalsIgnoreCase("uptime")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Returns the current uptime of this core.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.MODERATOR_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !uptime");
	    	}
	    	// !dbstatus
	    	else if (argument.equalsIgnoreCase("dbstatus")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Shows status of database connections.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.MODERATOR_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !dbstatus");
	    	}
	    	// !version
	    	else if (argument.equalsIgnoreCase("version")) {
	    		m_botAction.sendSmartPrivateMessage( messager , "Shows the revision number of this bot.");
	    		m_botAction.sendSmartPrivateMessage( messager , "Access required: " + operatorList.getAccessLevelName(OperatorList.MODERATOR_LEVEL));
	    		m_botAction.sendSmartPrivateMessage( messager , " !version");
	    	}
	    	else {
	    		m_botAction.sendSmartPrivateMessage( messager , "Syntax error. Please message !help <command> for more information.");
	    	}
    	}
    }

    /**
     * Spawns a bot of a given type.  User interface wrapper for spawn().
     * @param messager Name of the player who sent the command
     * @param message Bot type to spawn
     */
    public void handleSpawnMessage( String messager, String message ){
        String className = message.trim();
        
        if( className.length() > 0 ){
            m_botQueue.spawnBot( className, messager );
        } else {
            m_botAction.sendSmartPrivateMessage( messager, "Usage: !spawn <bot type>" );
        }
    }

    /**
     * Forces the spawn of a bot of a given type by manually supplying a login.
     * @param messager Name of the player who sent the command
     * @param message Bot to spawn and relevant login info
     */
    public void handleForceSpawnMessage( String messager, String message ){
        String args[] = message.split( " " );
        if( args.length == 3 ) {
            String className = args[0];
            String login = args[1];
            String password = args[2];
            m_botQueue.spawnBot( className, login, password, messager );
        } else {
            m_botAction.sendSmartPrivateMessage( messager, "Usage: !forcespawn <bot type> <login> <password>" );
        }
    }
    
    /**
     * Spawns the maximum number of a bot of a given type.
     * @param messager Name of the player who sent the command
     * @param message Bot type to spawn
     */
    public void handleSpawnMaxMessage( String messager, String message ){
    	String bottype = message.toLowerCase().trim();
    	BotSettings botInfo = m_botAction.getCoreData().getBotConfig(bottype);
    	Integer maxBots = botInfo.getInteger("Max Bots");
    	
    	if( botInfo == null ){
            m_botAction.sendChatMessage( 1, messager + " tried to spawn bot of type " + message + ".  Invalid bot type or missing CFG file." );
            m_botAction.sendSmartPrivateMessage( messager, "That bot type does not exist, or the CFG file for it is missing." );
            return;
        }
    	if( maxBots == null ){
            m_botAction.sendChatMessage( 1, messager + " tried to spawn bot of type " + message + ".  Invalid settings file. (MaxBots improperly defined)" );
            m_botAction.sendSmartPrivateMessage( messager, "The CFG file for that bot type is invalid. (MaxBots improperly defined)" );
            return;
        }
    	if( m_botQueue.getBotCount(message.toLowerCase()) >= maxBots){
    		m_botAction.sendChatMessage( 1, messager + " tried to spawn a new bot of type " + message + ".  Maximum number already reached (" + maxBots + ")");
    		m_botAction.sendSmartPrivateMessage( messager, "Maximum number of bots of this type (" + maxBots + ") has been reached." );
    		return;
    	}
    	m_botAction.sendSmartPrivateMessage( messager, "Spawning the maximum allowed number bots of type " + message.toLowerCase());
    	m_botAction.sendChatMessage( 1, messager + " is in queue to spawn the maximum allowed number bots of type " + message.toLowerCase());
    	
        if( bottype.length() > 0 ) {
            while(m_botQueue.getBotCount(bottype) < maxBots)
                m_botQueue.spawnBot( bottype, null);
        } else {
            m_botAction.sendSmartPrivateMessage( messager, "Usage: !spawnmax <bot type>" );
        }
    }
    
    /**
     * Spawns all bots on the autoloader that aren't currently spawned.
     * @param messager
     * @param message
     */
    public void handleSpawnAutoMessage( String messager, String message ){
    	m_botAction.sendSmartPrivateMessage( messager, "Spawning all bots from the autoloader that are not currently spawned.");
    	m_botAction.sendChatMessage( 1, messager + " is in queue to spawn all bots from the autoloader.");
    	
    	autoSpawnBots(true);
    	
    	m_botAction.sendSmartPrivateMessage( messager, "Done spawning all bots from the autoloader.");
    }
    
    /**
     * Displays the current SQL pool status.
     * @param messager
     * @param message
     */
    public void handleDbStatus( String messager, String message ) {
    	m_botAction.smartPrivateMessageSpam( messager, m_botAction.getCoreData().getSQLManager().getPoolStatus() );
    }
    
    /**
     * Displays the revision number of this file
     * @param messager
     * @param message
     */
    public void handleVersion( String messager, String message ) {
    	// Sample output of Revision keyword: $Revision$
    	String version = "$Revision$".substring(11).replace(" $","");
    	m_botAction.sendSmartPrivateMessage( messager , "TWCore revision "+version);
    	m_botAction.sendSmartPrivateMessage( messager , "More information about the latest change on http://www.twcore.org/changeset/"+version);
    }
    
    public void displayListBots(String name, String message){
        //method to display the list of bots from the .txt files
        try {
            
            String botname = m_botAction.getBotName();
            
            if(botname.equals("TWCore")){
                String[] twcore = {
                        "------------------------------------",
                        "=========== TWCore =================",
                        "List of bots that can be spawned:",
                        "- Use !spawn <typebot> to spawn it -",
                        "------- NickName / typebot ---------",
                        " ",
                        "Staffbot - [staffbot]",
                        "Zonerbot - [zonerbot]",
                        "Robohelp - [robohelp]",
                        "MessageBot - [messagebot]",
                        "Radiobot - [radiobot]",
                        "Pubbot - [pubbot]",
                        "Pubhub - [pubhub]",
                        "Banner Boy - [bannerboy]"
                };
                m_botAction.remotePrivateMessageSpam(name, twcore);
                }
            
            else if(botname.equals("TWCore-Events")){
                String[] twcoreEvents = {
                        "------------------------------------",
                        "========= TWCore-Events ============",
                        " ",
                        "List of bots that can be spawned:",
                        " ",
                        "- Use !spawn <typebot> to spawn it -",
                        " ",
                        "------- NickName / typebot ---------",
                        " ",
                        "RoboBot* - [multibot]",
                        "TWBot1 / TWBot2 - [elim]",
                        "Basebot/Wbduelbot/Javduelbot/Spiderduelbot - [bwjsbot]",
                        "RoboBoy/RoboGirl - [purepubbot]"
                };
                m_botAction.remotePrivateMessageSpam(name, twcoreEvents);
            }
            
            else if(botname.equals("TWCore-League")){
                String[] twcoreLeague = {
                        "------------------------------------",
                        "========= TWCore-League ============",
                        " ",
                        "List of bots that can be spawned:",
                        " ",
                        "- Use !spawn <typebot> to spawn it -",
                        " ",
                        "------- NickName / typebot ---------",
                        "",
                        "DuelBot - [duelbot]",
                        "MatchBot* - [matchbot]",
                        "TWDBot - [twdbot]",
                        "Tournybot - [tournybot]",
                        "TWDBot - [twdbot]",
                        "TWLBot* - [twl]"
                };
                m_botAction.remotePrivateMessageSpam(name, twcoreLeague);
            }
            
        } catch(Exception e){
            e.printStackTrace();
            m_botAction.sendPrivateMessage(name, "Exception.");
        }
    }
}
