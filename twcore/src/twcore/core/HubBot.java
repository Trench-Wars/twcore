package twcore.core;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Iterator;

import twcore.core.command.CommandInterpreter;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FileArrived;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**
 * Bot designed to spawn other bots (both manually and automatically from
 * autoload.cfg).  It also sets access privileges based on the server's access
 * lists.  The hub bot is essential for the operation of all other bots
 * in TWCore.
 */
public class HubBot extends SubspaceBot {
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

        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "handleHelp" );
        m_commandInterpreter.registerCommand( "!remove", acceptedMessages, this, "handleRemove" );
        m_commandInterpreter.registerCommand( "!hardremove", acceptedMessages, this, "handleHardRemove" );
        m_commandInterpreter.registerCommand( "!listbots", acceptedMessages, this, "handleListBots" );
        m_commandInterpreter.registerCommand( "!spawn", acceptedMessages, this, "handleSpawnMessage" );
        m_commandInterpreter.registerCommand( "!forcespawn", acceptedMessages, this, "handleForceSpawnMessage" );
        m_commandInterpreter.registerCommand( "!spawnmax", acceptedMessages, this, "handleSpawnMaxMessage" );
        m_commandInterpreter.registerCommand( "!autospawn", acceptedMessages, this, "handleAutoSpawnMessage" );
        m_commandInterpreter.registerCommand( "!updateaccess", acceptedMessages, this, "handleUpdateAccess" );
        m_commandInterpreter.registerCommand( "!listoperators", acceptedMessages, this, "handleListOperators" );
        m_commandInterpreter.registerCommand( "!waitinglist", acceptedMessages, this, "handleShowWaitingList" );
        m_commandInterpreter.registerCommand( "!uptime", acceptedMessages, this, "handleUptimeCommand" );
        m_commandInterpreter.registerCommand( "!billerdown", acceptedMessages, this, "handleBillerDownCommand" );
        m_commandInterpreter.registerCommand( "!recycleserver", acceptedMessages, this, "handleRecycleCommand" );
        m_commandInterpreter.registerCommand( "!shutdowncore", acceptedMessages, this, "handleShutdownCommand" );
        m_commandInterpreter.registerCommand( "!smartshutdown", acceptedMessages, this, "handleSmartShutdownCommand" );
        m_commandInterpreter.registerCommand( "!shutdownidlebots", acceptedMessages, this, "handleShutdownIdleBotsCommand" );
        m_commandInterpreter.registerCommand( "!shutdownallbots", acceptedMessages, this, "handleShutdownAllBotsCommand" );
        m_commandInterpreter.registerCommand( "!sqlstatus", acceptedMessages, this, "handleSQLStatus" );

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
        m_botAction.sendUnfilteredPublicMessage( "*g*misc:alertcommand" );
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Chat name" ) );
        initOperators();
        
        try {
            BufferedReader reader = new BufferedReader( new FileReader( m_botAction.getCoreCfg( "autoload.cfg" ) ) );
            String s = "";
            while( true ){
                s = reader.readLine();
                if( s == null || s.equals( "" ) ){
                    break;
                }
                char firstChar = s.trim().charAt( 0 );
                if( firstChar != '#' && firstChar != '[' ){
                    spawn( "AutoLoader", s );
                }
            }
        } catch( Exception e ){
            Tools.printStackTrace( "Exception while auto-loading bots", e );
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

        if( m_botAction.getOperatorList().isHighmod( messager ) == true ){
            m_botAction.sendPrivateMessage( messager, "attempting to remove " + message + "...  " +
            (m_botAction.getGeneralSettings().getInt( "FastDisconnect" ) == 0?"  This may take 30 seconds or more.":"" ) );
            boolean success = m_botQueue.removeBot( message, "force-disconnected by " + messager );
            if( success ) {
                m_botAction.sendPrivateMessage( messager, "Removed." );
                m_botAction.sendChatMessage( 1, messager + " force-disconnected " + message );
                System.gc();
            } else {
                m_botAction.sendPrivateMessage( messager, "Bot has NOT been removed.  Use exact casing of the name, i.e., !remove TWDBot" );
            }
        } else {
            m_botAction.sendChatMessage( 1, messager + " isn't a HighMod+, but tried !remove " + message );
        }
    }

    /**
     * Removes all bots of a given type.
     * @param messager Name of the player who sent the command
     * @param message Type of bot to remove
     */
    public void handleHardRemove( String messager, String message ){
        message = message.trim();

        if( m_botAction.getOperatorList().isHighmod( messager ) == true ){
            m_botAction.sendPrivateMessage( messager, "Removing all bots of type " + message + "." +
                    (m_botAction.getGeneralSettings().getInt( "FastDisconnect" ) == 0?"  This may take on average 30 seconds per bot ...":"" ) );
            m_botAction.sendChatMessage( 1, messager + " is force-disconnecting all bots of type " + message );
            m_botQueue.hardRemoveAllBotsOfType( message, messager );
            m_botAction.sendPrivateMessage( messager, "Removed all bots of type " + message + " (if possible).  Count reset to 0." );
            System.gc();
        } else {
            m_botAction.sendChatMessage( 1, messager + " isn't a HighMod+, but tried !hardremove " + message );
        }
    }

    /**
     * Displays in PM the list of bots waiting to be spawned.
     * @param messager Name of the player who sent the request
     * @param message Text of the message following the command
     */
    public void handleShowWaitingList( String messager, String message ){
        if( m_botAction.getOperatorList().isER(messager ) == true ){
            m_botQueue.listWaitingList( messager );
        }
    }

    /**
     * Sends a request to update all access levels based on access files.
     * @param messager Name of the player who sent the command
     * @param message Text of the message
     */
    public void handleUpdateAccess( String messager, String message ){
        if( m_botAction.getOperatorList().isSmod( messager ) == true ){
            initOperators();
            m_botAction.sendSmartPrivateMessage( messager, "Updating access levels..." );
            m_botAction.sendChatMessage( 1, "Updating access levels at " + messager + "'s request" );
        } else {
            m_botAction.sendChatMessage( 1, messager + " isn't an SMod, but tried !updateaccess " + message );
        }
    }

    /**
     * Lists bot names of a given bot type
     * @param messager Name of the player who sent the command
     * @param message Bot type to list
     */
    public void handleListBots( String messager, String message ){
        String className = message.trim();

        if( m_botAction.getOperatorList().isModerator( messager ) == true ){
            if( className.length() > 0 ){
                m_botQueue.listBots( className, messager );
            } else {
            	m_botQueue.listBotTypes( messager );
            }
        } else {
            m_botAction.sendChatMessage( 1, messager + " doesn't have access, but tried !listbots " + message );
        }
    }

    /**
     * Lists operators registered with this hub/bots
     * @param messager Name of the player who sent the command
     * @param message Bot type to list
     */
    public void handleListOperators( String messager, String message ){

    	int linelength = 63;

        if( m_botAction.getOperatorList().isHighmod( messager ) == true ){
        	OperatorList list = m_botAction.getOperatorList();

        	// Owners
        	HashSet<String> owners = (list.getAllOfAccessLevel(OperatorList.OWNER_LEVEL));
        	if(owners.size() > 0) {
        		m_botAction.sendSmartPrivateMessage( messager, "Owners ("+owners.size()+"):");

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
        	if(sysops.size() > 0) {
        		m_botAction.sendSmartPrivateMessage( messager, "Sysops ("+sysops.size()+"):");

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
        	if(smods.size() > 0) {
        		m_botAction.sendSmartPrivateMessage( messager, "SMods ("+smods.size()+"):");

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

        	// Mods
        	HashSet<String> mods = (list.getAllOfAccessLevel(OperatorList.MODERATOR_LEVEL));
        	if(mods.size() > 0) {
        		m_botAction.sendSmartPrivateMessage( messager, "Mods ("+mods.size()+"):");

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
        	if(ers.size() > 0) {
        		m_botAction.sendSmartPrivateMessage( messager, "ERs ("+ers.size()+"):");

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

        	// Bots
        	HashSet<String> bots = (list.getAllOfAccessLevel(OperatorList.BOT_LEVEL));
        	if(bots.size() > 0) {
        		m_botAction.sendSmartPrivateMessage( messager, "Operators that have identified themselves as bots ("+bots.size()+"):");

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

        	// Outsiders
        	HashSet<String> outsiders = (list.getAllOfAccessLevel(OperatorList.OUTSIDER_LEVEL));
        	if(outsiders.size() > 0) {
        		m_botAction.sendSmartPrivateMessage( messager, "Outsiders ("+outsiders.size()+"):");

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

        } else {
            m_botAction.sendChatMessage( 1, messager + " isn't a Highmod, but tried !listoperators " + message );
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
        m_botAction.scheduleTaskAtFixedRate(m_billerDownTask, 1000, m_billerDownRate);
    }

    /**
     * Recycles the server if the player has the correct password.
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleRecycleCommand( String messager, String message ){
        if( m_botAction.getOperatorList().isSmod( messager ) ) {
            if( !message.equals(m_botAction.getGeneralSettings().getNonNullString("RecyclePassword"))) {
                m_botAction.sendSmartPrivateMessage( messager, "Invalid password." );
                m_botAction.sendChatMessage( 1, messager + " provided an invalid password for recycling the server.");
                return;
            }
            m_botAction.sendZoneMessage( "NOTICE: Server recycling all users to regain full functionality ... please log in again in a few moments. -TWStaff", Tools.Sound.BEEP1 );
            m_botAction.sendUnfilteredPublicMessage("*recycle");

        }
    }

    /**
     * Shuts down the core immediately.
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleShutdownCommand( String messager, String message ){
        if( m_botAction.getOperatorList().isSysop( messager ) || m_botAction.getOperatorList().isDeveloperExact( messager ) ) {
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
        } else {
            m_botAction.sendChatMessage( 1, messager + " doesn't have access, but tried to shut down the core.");
        }
    }

    /**
     * Shuts down the core gradually by disconnecting bots when they are no longer in use.
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleSmartShutdownCommand( String messager, String message ){
        if( m_botAction.getOperatorList().isSysop( messager ) || m_botAction.getOperatorList().isDeveloperExact( messager ) ) {
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

        } else {
            m_botAction.sendChatMessage( 1, messager + " doesn't have access, but tried to do a smart shut down on the core.");
        }
    }

    /**
     * Shuts down any idle bots, without continuing to check for when busy bots become idle.
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleShutdownIdleBotsCommand( String messager, String message ){
        if( m_botAction.getOperatorList().isSysop( messager ) || m_botAction.getOperatorList().isDeveloperExact( messager ) ) {
            m_botAction.sendSmartPrivateMessage( messager, "Shutting down all idle bots ..." );
            m_botAction.sendChatMessage( 1, "Idle bot shutdown initiated by " + messager + ".  All idle bots will be removed." );
            Tools.printLog( "Idle bot shutdown by " + messager + ".");

            m_botQueue.shutdownIdleBots();
        } else {
            m_botAction.sendChatMessage( 1, messager + " doesn't have access, but tried to shut down all idle bots.");
        }
    }

    /**
     * Shuts down any idle bots, without continuing to check for when busy bots become idle.
     * @param messager Name of the player who sent the command
     * @param message is irrelevant
     */
    public void handleShutdownAllBotsCommand( String messager, String message ){
        if( m_botAction.getOperatorList().isSysop( messager ) || m_botAction.getOperatorList().isDeveloperExact( messager ) ) {
            m_botAction.sendSmartPrivateMessage( messager, "Shutting down all bots ..." );
            m_botAction.sendChatMessage( 1, "Full bot shutdown initiated by " + messager + ".  All bots will be removed, but the Hub will be left online." );
            Tools.printLog( "Full bot shutdown by " + messager + ".");

            m_botQueue.shutdownAllBots();
        } else {
            m_botAction.sendChatMessage( 1, messager + " doesn't have access, but tried to shut down all bots.");
        }
    }

    /**
     * Sends an appropriate help message based on access privileges.
     * @param messager Name of the player who sent the command
     * @param message Text of the message
     */
    public void handleHelp( String messager, String message ){

        if( m_botAction.getOperatorList().isOutsider( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!help              - Displays this message." );
            m_botAction.sendSmartPrivateMessage( messager, "!spawn <bot type>  - spawns a new bot." );
            m_botAction.sendSmartPrivateMessage( messager, "!waitinglist       - Displays the waiting list." );
            m_botAction.sendSmartPrivateMessage( messager, "!billerdown <pwd>  - Sends periodic biller down msg.  Mod+ doesn't need password.");
        }

        if( m_botAction.getOperatorList().isModerator( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!listbots [type]   - Returns the bottypes or lists the names and spawners of a bot [type]." );
            m_botAction.sendSmartPrivateMessage( messager, "!uptime            - Returns the current uptime of this bot." );
            m_botAction.sendSmartPrivateMessage( messager, "!sqlstatus         - Shows status of SQL connections." );
        }

        if( m_botAction.getOperatorList().isHighmod( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!remove <name>     - Removes <name> bot.  MUST USE EXACT CASE!  (i.e., TWDBot)." );
            m_botAction.sendSmartPrivateMessage( messager, "!hardremove <type> - Removes all bots of <type>, and resets the bot's count." );
            m_botAction.sendSmartPrivateMessage( messager, "!spawnmax <type>   - Spawns the maximum amount of bots of type <type>.");
            m_botAction.sendSmartPrivateMessage( messager, "!autospawn         - Spawns all bots on the autoloader that aren't currently spawned.");
        }

        if( m_botAction.getOperatorList().isSmod( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!updateaccess      - Rereads the mod, smod, and sysop file so that all access levels are updated." );
            m_botAction.sendSmartPrivateMessage( messager, "!listoperators     - Lists all registered operators for this bot.");
            m_botAction.sendSmartPrivateMessage( messager, "!recycleserver <pwd>  - Recycles the server, if provided correct password.  Use after biller down.");
        }

        if( m_botAction.getOperatorList().isSysop( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!forcespawn <bot> <login> <password> - Force-spawn a bot (ignore count) with the specified login." );
            m_botAction.sendSmartPrivateMessage( messager, "!smartshutdown     - Shuts down all bots as they become idle, then shuts down the core." );
            m_botAction.sendSmartPrivateMessage( messager, "!shutdowncore      - Does a clean shutdown of the entire core.  (Disable restart scripts first)." );
            m_botAction.sendSmartPrivateMessage( messager, "!shutdownidlebots  - Kills all idle bots, leaving any running bots and the hub online." );
            m_botAction.sendSmartPrivateMessage( messager, "!shutdownallbots   - Kills all bots, leaving the hub online." );
        }

        if( m_botAction.getOperatorList().isDeveloperExact( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!smartshutdown     - Shuts down all bots as they become idle, then shuts down the core." );
            m_botAction.sendSmartPrivateMessage( messager, "!shutdowncore      - Does a clean shutdown of the entire core.  (Disable restart scripts first)." );
            m_botAction.sendSmartPrivateMessage( messager, "!shutdownidlebots  - Kills all idle bots, leaving any running bots and the hub online." );
            m_botAction.sendSmartPrivateMessage( messager, "!shutdownallbots   - Kills all bots, leaving the hub online." );
        }
    }

    /**
     * Spawns a bot of a given type.
     * @param messager Name of the player who sent the command
     * @param message Bot type to spawn
     */
    public void spawn( String messager, String message ){
        String className = message.trim();
        if( className.length() > 0 ){
            m_botQueue.spawnBot( className, messager );
        } else {
            m_botAction.sendSmartPrivateMessage( messager, "Usage: !spawn <bot type>" );
        }
    }

    /**
     * Spawns a bot of a given type.  User interface wrapper for spawn().
     * @param messager Name of the player who sent the command
     * @param message Bot type to spawn
     */
    public void handleSpawnMessage( String messager, String message ){
        if( m_botAction.getOperatorList().isOutsider( messager ) ){
            spawn( messager, message );
        } else {
            if( m_botAction.getOperatorList().isBot(messager) ) {
                int allowSpawn = m_botAction.getGeneralSettings().getInt( "AllowZHSpawning" );
                if( allowSpawn == 2 || allowSpawn == 1 && message.toLowerCase().trim().equals("matchbot") ) {
                    spawn( messager, message );
                } else {
                    m_botAction.sendChatMessage( 1, messager + " doesn't have access (ZHs not allowed to spawn " + (allowSpawn == 0?"bots)":"bots other than matchbot)") +
                            ", but (s)he tried '!spawn " + message + "'");
                }
            } else {
                m_botAction.sendChatMessage( 1, messager + " doesn't have access, but tried '!spawn " + message + "'");
            }
        }
    }

    /**
     * Forces the spawn of a bot of a given type by manually supplying a login.
     * @param messager Name of the player who sent the command
     * @param message Bot to spawn and relevant login info
     */
    public void handleForceSpawnMessage( String messager, String message ){
        if( m_botAction.getOperatorList().isSysop( messager ) ){
            String args[] = message.split( " " );
            if( args.length == 3 ) {
                String className = args[0];
                String login = args[1];
                String password = args[2];
                m_botQueue.spawnBot( className, login, password, messager );
            } else {
                m_botAction.sendSmartPrivateMessage( messager, "Usage: !forcespawn <bot type> <login> <password>" );
            }
        } else {
            m_botAction.sendChatMessage( 1, messager + " doesn't have access, but tried to use !forcespawn." );
        }
    }
    
    /**
     * Spawns the maximum number of a bot of a given type.
     * @param messager Name of the player who sent the command
     * @param message Bot type to spawn
     */
    public void handleSpawnMaxMessage( String messager, String message ){
    	if( !m_botAction.getOperatorList().isHighmod( messager ) )return;
    	BotSettings botInfo = m_botAction.getCoreData().getBotConfig(message.toLowerCase());
    	if( botInfo == null ){
            m_botAction.sendChatMessage( 1, messager + " tried to spawn bot of type " + message + ".  Invalid bot type or missing CFG file." );
            m_botAction.sendSmartPrivateMessage( messager, "That bot type does not exist, or the CFG file for it is missing." );
            return;
        }
    	Integer maxBots = botInfo.getInteger("Max Bots");
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
    	while(m_botQueue.getBotCount(message.toLowerCase()) < maxBots)
    		spawn( messager, message );
    }
    
    /**
     * Spawns all bots on the autoloader that aren't currently spawned.
     * @param messager
     * @param message
     */
    public void handleAutoSpawnMessage( String messager, String message ){
    	if( !m_botAction.getOperatorList().isHighmod( messager ) )return;
    	m_botAction.sendSmartPrivateMessage( messager, "Spawning all bots from the autoloader that are not currently spawned.");
    	m_botAction.sendChatMessage( 1, messager + " is in queue to spawn all bots from the autoloader.");
    	try{
    		BufferedReader reader = new BufferedReader( new FileReader( m_botAction.getCoreCfg( "autoload.cfg" ) ) );
    		TreeMap<String, Integer> autoLoads = new TreeMap<String, Integer>();
    		String s = "";
    		while( true ){
    			s = reader.readLine();
    			if( s == null || s.equals( "" ) )
    				break;
    			char firstChar = s.trim().charAt( 0 );
    			if( firstChar != '#' && firstChar != '[' ){
    				if(autoLoads.containsKey(s)){
    					int curNum = autoLoads.get(s);
    					autoLoads.remove(s);
    					autoLoads.put(s, curNum + 1);
    				} else autoLoads.put(s, 1);
    			}
    		}
    		Iterator<String> it = autoLoads.keySet().iterator();
    		while( it.hasNext() ){
    			String botName = it.next();
    			while(m_botQueue.getBotCount(botName) < autoLoads.get(botName))
    	    		spawn( messager, botName );   			
    		}		
    	}catch(Exception e){
    		m_botAction.sendSmartPrivateMessage( messager, e.getMessage());
    		Tools.printStackTrace(e);
    	}
    }
    
    /**
     * Displays the current SQL pool status.
     * @param messager
     * @param message
     */
    public void handleSQLStatus( String messager, String message ) {
        if( m_botAction.getOperatorList().isModerator( messager ) ){
            m_botAction.smartPrivateMessageSpam( messager, m_botAction.getCoreData().getSQLManager().getPoolStatus() );
        } else {
            m_botAction.sendChatMessage( 1, messager + " doesn't have access, but tried to use !sqlstatus." );
        }
    }
}
