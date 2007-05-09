package twcore.core;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
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
        m_commandInterpreter.registerCommand( "!listbottypes", acceptedMessages, this, "handleListBotTypes" );
        m_commandInterpreter.registerCommand( "!updateaccess", acceptedMessages, this, "handleUpdateAccess" );
        m_commandInterpreter.registerCommand( "!listoperators", acceptedMessages, this, "handleListOperators" );
        m_commandInterpreter.registerCommand( "!waitinglist", acceptedMessages, this, "handleShowWaitingList" );
        m_commandInterpreter.registerCommand( "!restartlog", acceptedMessages, this, "handleClose");

        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "handleInvalidMessage" );
        m_commandInterpreter.registerDefaultCommand( Message.REMOTE_PRIVATE_MESSAGE, this, "handleInvalidMessage" );
    }

    public void handleClose(String name, String message) {
    	if(!m_botAction.getOperatorList().isSmod(name)) {
    		return;
    	}
    	try {
	    	System.out.close();
	    	System.err.close();
	    	File f1 = new File("currentOut.log");
	    	File f2 = new File("currentErr.log");
	    	File f3 = new File("out.log");
	    	File f4 = new File("err.log");
	    	if(!f3.exists()) f3.createNewFile();
	    	if(!f4.exists()) f4.createNewFile();
	    	if(f1.exists()) {
	    		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f3, true )));
	    		BufferedReader in = new BufferedReader(new FileReader(f1));
	    		String inLine;
	    		out.println(Tools.getTimeStamp());
	    		while((inLine = in.readLine()) != null) {
	    			out.println(inLine);
	    		}
	    		out.close();
	    	}
	    	if(f2.exists()) {
	    		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f4, true )));
	    		BufferedReader in = new BufferedReader(new FileReader(f2));
	    		String inLine;
	    		out.println(Tools.getTimeStamp());
	    		while((inLine = in.readLine()) != null) {
	    			out.println(inLine);
	    		}
	    		out.close();
	    	}
	    	f1.delete();
	    	f2.delete();
	    	f1.createNewFile();
	    	f2.createNewFile();
	    	System.setOut(new PrintStream(new FileOutputStream(f1)));
	    	System.setErr(new PrintStream(new FileOutputStream(f2)));
	    } catch(Exception e) {}
    }

    /**
     * Runs necessary setup, loads all access lists, and adds all bots in
     * autoload.cfg to the bot loading queue.
     */
    public void handleEvent( LoggedOn event ){

        m_botAction.joinArena( "#robopark" );
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
        handleClose(m_botAction.getBotName(), "");
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
            boolean success = m_botQueue.removeBot( message );
            if( success ) {
                m_botAction.sendPrivateMessage( messager, "Removed." );
                m_botAction.sendChatMessage( 1, messager + " force-disconnected " + message );
            } else {
                m_botAction.sendPrivateMessage( messager, "Bot has NOT been removed.  Use exact casing of the name, i.e., !remove TWDBot" );
            }
        } else {
            m_botAction.sendChatMessage( 1, messager + " isn't a highmod, but (s)he tried !remove " + message );
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
            m_botAction.sendPrivateMessage( messager, "Removing all bots of type " + message + ".  Please be patient, this may take a while." );
            m_botQueue.hardRemoveAllBotsOfType( message );
            m_botAction.sendPrivateMessage( messager, "Removed all bots of type " + message + " (if possible).  Count reset to 0." );
            m_botAction.sendChatMessage( 1, messager + " force-disconnected all bots of type " + message );
        } else {
            m_botAction.sendChatMessage( 1, messager + " isn't a highmod, but (s)he tried !hardremove " + message );
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
            m_botAction.sendChatMessage( 1, messager + " isn't an ER+, but (s)he tried !waitinglist" );
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
            m_botAction.sendChatMessage( 1, messager + " isn't an smod, but (s)he tried !updateaccess " + message );
        }
    }

    /**
     * Lists the numbers of all bots spawned of each type.
     * @param messager Name of the player who sent the command
     * @param message Text of the message
     */
    public void handleListBotTypes( String messager, String message ){
        if( m_botAction.getOperatorList().isHighmod( messager ) == true ){
            m_botQueue.listBotTypes( messager );
        } else {
            m_botAction.sendChatMessage( 1, messager + " isn't a Highmod, but (s)he tried !listbottypes " + message );
        }
    }

    /**
     * Lists bot names of a given bot type
     * @param messager Name of the player who sent the command
     * @param message Bot type to list
     */
    public void handleListBots( String messager, String message ){
        String className = message.trim();

        if( m_botAction.getOperatorList().isHighmod( messager ) == true ){
            if( className.length() > 0 ){
                m_botQueue.listBots( className, messager );
            } else {
                m_botAction.sendSmartPrivateMessage( messager, "Usage: !listbots <bot type>" );
            }
        } else {
            m_botAction.sendChatMessage( 1, messager + " isn't a Highmod, but (s)he tried !listbots " + message );
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
            m_botAction.sendChatMessage( 1, messager + " isn't a Highmod, but (s)he tried !listoperators " + message );
        }
    }

    /**
     * Sends an appropriate help message based on access privileges.
     * @param messager Name of the player who sent the command
     * @param message Text of the message
     */
    public void handleHelp( String messager, String message ){

        if( m_botAction.getOperatorList().isOutsider( messager ) == true ){
            m_botAction.sendSmartPrivateMessage( messager, "!help              - Displays this message." );
            m_botAction.sendSmartPrivateMessage( messager, "!spawn <bot type>  - spawns a new bot." );
            m_botAction.sendSmartPrivateMessage( messager, "!waitinglist       - Displays the waiting list." );
        }

        if( m_botAction.getOperatorList().isHighmod( messager ) == true ){
            m_botAction.sendSmartPrivateMessage( messager, "!remove <name>     - Removes <name> bot.  MUST USE EXACT CASE!  (i.e., TWDBot)" );
            m_botAction.sendSmartPrivateMessage( messager, "!hardremove <type> - Removes all bots of <type>, and resets the bot's count." );
            m_botAction.sendSmartPrivateMessage( messager, "!listbottypes      - Lists the number of each bot type currently in use." );
            m_botAction.sendSmartPrivateMessage( messager, "!listbots <type>   - Lists the names and spawners of a bot type." );
            m_botAction.sendSmartPrivateMessage( messager, "!listoperators     - Lists all registered operators for this bot.");
        }

        if( m_botAction.getOperatorList().isSmod( messager ) == true ){
            m_botAction.sendSmartPrivateMessage( messager, "!updateaccess      - Rereads the mod, smod, and sysop file so that all access levels are updated." );
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
        if( m_botAction.getOperatorList().isOutsider( messager ) == true ){
            spawn( messager, message );
        } else {
            if( m_botAction.getOperatorList().isZH(messager) && message.toLowerCase() == "matchbot" )
                spawn( messager, message );
            else
                m_botAction.sendChatMessage( 1, messager + " isn't an ER+, but (s)he tried !spawn " + message );
        }
    }
}
