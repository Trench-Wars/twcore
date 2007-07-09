package twcore.core;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.util.HashSet;

import twcore.core.command.CommandInterpreter;
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
        m_commandInterpreter.registerCommand( "!updateaccess", acceptedMessages, this, "handleUpdateAccess" );
        m_commandInterpreter.registerCommand( "!listoperators", acceptedMessages, this, "handleListOperators" );
        m_commandInterpreter.registerCommand( "!waitinglist", acceptedMessages, this, "handleShowWaitingList" );
        m_commandInterpreter.registerCommand( "!uptime", acceptedMessages, this, "handleUptimeCommand" );
        m_commandInterpreter.registerCommand( "!shutdowncore", acceptedMessages, this, "handleShutdownCommand" );
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
        LoadAccessLists();
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
        String fileName = event.getFileName();

        if( fileName.compareTo( "moderate.txt" ) == 0 ) {
            m_botAction.getOperatorList().parseFile( m_botAction.getDataFile( "moderate.txt" ), OperatorList.MODERATOR_LEVEL );
            m_botAction.getOperatorList().changeAllMatches( "<ZH>", OperatorList.ZH_LEVEL );
            m_botAction.getOperatorList().changeAllMatches( "<ER>", OperatorList.ER_LEVEL );
        } else if( fileName.compareTo( "smod.txt" ) == 0 ){
            m_botAction.getOperatorList().parseFile( m_botAction.getDataFile( "smod.txt" ), OperatorList.SMOD_LEVEL );
        } else if( fileName.compareTo( "sysop.txt" ) == 0 ){
            m_botAction.getOperatorList().parseFile( m_botAction.getDataFile( "sysop.txt" ), OperatorList.SYSOP_LEVEL );
        }
    }

    /**
     * Clears all current access lists, and sets up the new lists based on access
     * CFG files and the three server-based access lists.
     */
    public void LoadAccessLists(){
        m_botAction.getOperatorList().clearList();
        m_botAction.getOperatorList().parseFile( m_botAction.getCoreCfg( "owners.cfg" ), OperatorList.OWNER_LEVEL );
        m_botAction.getOperatorList().parseFile( m_botAction.getCoreCfg( "outsider.cfg" ), OperatorList.OUTSIDER_LEVEL );
        m_botAction.getOperatorList().parseFile( m_botAction.getCoreCfg( "develop.cfg" ), OperatorList.DEV_LEVEL );
        m_botAction.getOperatorList().parseFile( m_botAction.getCoreCfg( "highmod.cfg" ), OperatorList.HIGHMOD_LEVEL );
        m_botAction.sendUnfilteredPublicMessage( "*getmodlist" );
        m_botAction.sendUnfilteredPublicMessage( "*getfile smod.txt" );
        m_botAction.sendUnfilteredPublicMessage( "*getfile sysop.txt" );
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
            boolean success = m_botQueue.removeBot( message );
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
            m_botQueue.hardRemoveAllBotsOfType( message );
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
        if( m_botAction.getOperatorList().isOutsider( messager ) == true ){
            m_botQueue.listWaitingList( messager );
        } else {
            m_botAction.sendChatMessage( 1, messager + " isn't an ER+, but tried !waitinglist" );
        }
    }

    /**
     * Sends a request to update all access levels based on access files.
     * @param messager Name of the player who sent the command
     * @param message Text of the message
     */
    public void handleUpdateAccess( String messager, String message ){
        if( m_botAction.getOperatorList().isSmod( messager ) == true ){
            LoadAccessLists();
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

        	// ZHs
        	HashSet<String> zhs = (list.getAllOfAccessLevel(OperatorList.ZH_LEVEL));
        	if(zhs.size() > 0) {
        		m_botAction.sendSmartPrivateMessage( messager, "ZHs ("+zhs.size()+"):");

        		String pm = "  ";

        		for(String zh:zhs) {
        			if(pm.length() < linelength) {
        				pm += zh + ", ";
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
     * Shuts down the core.
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
     * Sends an appropriate help message based on access privileges.
     * @param messager Name of the player who sent the command
     * @param message Text of the message
     */
    public void handleHelp( String messager, String message ){

        if( m_botAction.getOperatorList().isOutsider( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!help              - Displays this message." );
            m_botAction.sendSmartPrivateMessage( messager, "!spawn <bot type>  - spawns a new bot." );
            m_botAction.sendSmartPrivateMessage( messager, "!waitinglist       - Displays the waiting list." );
        }

        if( m_botAction.getOperatorList().isModerator( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!listbots [type]   - Returns the bottypes or lists the names and spawners of a bot [type]." );
            m_botAction.sendSmartPrivateMessage( messager, "!uptime            - Returns the current uptime of this bot." );
            m_botAction.sendSmartPrivateMessage( messager, "!sqlstatus         - Shows status of SQL connections." );
        }

        if( m_botAction.getOperatorList().isHighmod( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!remove <name>     - Removes <name> bot.  MUST USE EXACT CASE!  (i.e., TWDBot)" );
            m_botAction.sendSmartPrivateMessage( messager, "!hardremove <type> - Removes all bots of <type>, and resets the bot's count." );
        }

        if( m_botAction.getOperatorList().isSmod( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!updateaccess      - Rereads the mod, smod, and sysop file so that all access levels are updated." );
            m_botAction.sendSmartPrivateMessage( messager, "!listoperators     - Lists all registered operators for this bot.");
        }

        if( m_botAction.getOperatorList().isSysop( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!forcespawn <bot> <login> <password> - Force-spawn a bot (ignore count) with the specified login." );
            m_botAction.sendSmartPrivateMessage( messager, "!shutdowncore      - Does a clean shutdown of the entire core.  (Disable restart scripts first)" );
        }

        if( m_botAction.getOperatorList().isDeveloperExact( messager ) ){
            m_botAction.sendSmartPrivateMessage( messager, "!shutdowncore      - Does a clean shutdown of the entire core.  (Disable restart scripts first)" );
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
            if( m_botAction.getOperatorList().isZH(messager) ) {
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
     *
     * @param messager
     * @param message
     */
    public void handleSQLStatus( String messager, String message ) {
        if( m_botAction.getOperatorList().isModerator( messager ) ){
            m_botAction.privateMessageSpam( messager, m_botAction.getCoreData().getSQLManager().getPoolStatus() );
        } else {
            m_botAction.sendChatMessage( 1, messager + " doesn't have access, but tried to use !sqlstatus." );
        }
    }
}
