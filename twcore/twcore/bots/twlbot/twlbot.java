package twcore.bots.twlbot;

import java.util.*;
import java.io.*;
import twcore.core.*;

/**
 * This is to function as the base class for the TWL bot
 * 
 * The bot gives a base upon which the twl extension is built.
 * Other extensions can be added upon request
 * 
 * @author - FoN
 * 		modified from twbot.java
 * @version 2.1
 */
public class twlbot extends SubspaceBot
{	
    //essential subspace bot classes
    private BotAction m_botAction;
	private OperatorList m_opList;
	private BotSettings m_botSettings;
	private AdaptiveClassLoader m_loader;
	private CommandInterpreter m_commandInterpreter;

	//state variables
	private String m_currentArena;
	private String m_defaultArena = "#robopark"; //initial value only used if initialization fails
	private String m_nameOfHost;
	private boolean locked = false;
	private int m_lastUse = 0;
	private int m_idleTime;

	private TimerTask m_checkTime;
	private HashMap m_extensions;

	//setting files
	private File m_botRoot;
	private File m_coreRoot;
	
	//constants
	private final double VERSION = 2.03;

	/** Creates a new instance of newportabot */
	public twlbot(BotAction botAction)
	{
		super(botAction);
		Vector repository = new Vector();
		m_coreRoot = new File(botAction.getGeneralSettings().getString("Core Location"));
		m_botRoot = new File(m_coreRoot.getPath() + "/twcore/bots/twlbot");
		repository.add(m_coreRoot);
		m_loader = new AdaptiveClassLoader(repository, getClass().getClassLoader());

		m_botAction = botAction;
		m_botSettings = m_botAction.getBotSettings();
		m_extensions = new HashMap();
		
		//register commands
		m_commandInterpreter = new CommandInterpreter(m_botAction);
		registerCommands();

		//bot Settings initialization
		m_idleTime = m_botSettings.getInt("IdleReturnTime");
		m_defaultArena = m_botSettings.getString("InitialArena");
		m_currentArena = m_defaultArena;

		//Checks for an idle unlocked bot.
		m_checkTime = new TimerTask()
		{
			public void run()
			{
				m_lastUse += 30;
				if (!locked && !m_currentArena.equals(m_defaultArena))
				{
					if (m_lastUse > m_idleTime)
						actualGo(m_defaultArena);
				}
				else
					m_lastUse = 0;
			}
		};
		m_botAction.scheduleTaskAtFixedRate(m_checkTime, 0, 30000); //30 secs
		m_botAction.getEventRequester().requestAll();
	}

	private void registerCommands()
	{
        int acceptedMessages;
        acceptedMessages = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;

        /*********Host Commands*********/
        m_commandInterpreter.registerCommand("!help", acceptedMessages, this, "do_help", "!help <module>     - Lists the !help for module");
        m_commandInterpreter.registerCommand("!helpall", acceptedMessages, this, "do_allHelp", "!allhelp           - Lists help from all extensions if loaded");
        m_commandInterpreter.registerCommand( "!load", acceptedMessages, this, "do_load", "!load <module>     - Loads a module" );
        m_commandInterpreter.registerCommand( "!modules", acceptedMessages, this, "do_moduleList", "!modules           - Lists all the modules that can be loaded" );
        m_commandInterpreter.registerCommand( "!unload", acceptedMessages, this, "do_removeModule", "!unload <module>   - Unloads a module");
        m_commandInterpreter.registerCommand( "!loaded", acceptedMessages, this, "do_listLoaded", "!loaded            - Shows all the loaded modules");
        m_commandInterpreter.registerCommand("!go", acceptedMessages, this, "do_go", "!go <arena> - Tells the bot to come to an arena");
        m_commandInterpreter.registerCommand("!home", acceptedMessages, this, "do_home", "!home              - Tells the bot to unlock and go home");
        m_commandInterpreter.registerCommand("!lock", acceptedMessages, this, "do_lock", "!lock              - Locks bot so it can't be moved and loads the standard module");
        m_commandInterpreter.registerCommand("!unlock", acceptedMessages, this, "do_unlock", "!unlock            - Unlocks bot so it can be moved");
        m_commandInterpreter.registerCommand("!version", acceptedMessages, this, "do_version", "!version           - Displays the version of the bot");
        m_commandInterpreter.registerCommand("!mybot", acceptedMessages, this, "do_mybot", "!mybot             - Change of host");
    //    m_commandInterpreter.registerCommand("!die", acceptedMessages, this, "do_die", "!die               - Tells the bot to take a hike... off a cliff.");
    //enable after test is complete     
        
        m_commandInterpreter.registerDefaultCommand(acceptedMessages, this, "do_nothing");
	}
	
	/**
	 * Commands that are issued as messages packets are parsed here
	 * @param name The host
	 * @param message command string
	 */
	public void do_command(String name, String message)
	{
		if (message.startsWith("!die"))
		{
		    do_die(name, message);
		}
	}

	//COMMANDS FUNCTIONS START HERE
	
	/**
	 * Loads a extenstion module if there is one
	 * 
	 * @param name Name of the host loading the extension
	 * @param extensionType Type of extension to be loaded
	 */
	public void do_load(String name, String extensionType)
	{
		if (locked)
		{
			try
			{
				if (m_loader.shouldReload())
					m_loader.reinstantiate();
				extensionType = extensionType.toLowerCase();
				TWLBotExtension extension = (TWLBotExtension) m_loader.loadClass("twcore.bots.twlbot.twlbot" + extensionType).newInstance();
				extension.set(m_botAction, m_opList, this);
				m_extensions.put(extensionType, extension);
				m_botAction.sendSmartPrivateMessage(name, "Successfully loaded " + extensionType);
			}
			catch (Exception e)
			{
				m_botAction.sendSmartPrivateMessage(name, "Failed to load " + extensionType);
			}
		}
		else
		{
			m_botAction.sendSmartPrivateMessage(name, "Please !lock the bot first before loading a module.");
		}
	}

	/**
	 * Lists the modules that can be loaded in the twlbot
	 * 
	 * @param name The host
	 * @param message the command issued
	 */	
	public void do_moduleList(String name, String message)
	{
		String[] s = m_botRoot.list();
		m_botAction.sendSmartPrivateMessage(name, "I contain the following " + "modules:");
		m_botAction.sendSmartPrivateMessage(name, "A * indicates a loaded module");
		for (int i = 0; i < s.length; i++)
		{
			if (s[i].endsWith(".class"))
			{
				s[i] = s[i].substring(0, s[i].lastIndexOf('.'));
				String className = "twlbot";
				if (s[i].startsWith(className) && !s[i].equals(className) && s[i].indexOf('$') == -1)
				{
					String reply = s[i].substring(className.length()); //size of "twlbot"
					if (m_extensions.containsKey(reply))
						reply = reply + " *";
					m_botAction.sendSmartPrivateMessage(name, reply);
				}
			}
		}
	}

	/**
	 * Removes a module if installed
	 * Cannot remove the standard module
	 * @param name The host
	 * @param key The module name
	 */
	public void do_removeModule(String name, String key)
	{
	    if (!key.toLowerCase().equals("standard"))
	    {
	        if (m_extensions.containsKey(key))
	        {
	            ((TWLBotExtension) m_extensions.remove(key)).cancel();
	            m_botAction.sendSmartPrivateMessage(name, key + " Successfully Removed");
	        }
	        else
	        {
	            m_botAction.sendSmartPrivateMessage(name, key + " is not loaded, so it cannot be removed.  Keep in mind the " + "names are case sensitive.");
	        }
	    }
	    else
	    {
	        m_botAction.sendSmartPrivateMessage(name, "Cannot remove the standard module.  Use !unlock");
	    }
	}

	/**
	 * List the modules loaded
	 * Standard should be loaded on default unless unlocked
	 * @param name Host Name
	 * @param any extra parameters the host might have provided
	 */
	public void do_listLoaded(String name, String message)
	{
		if (m_extensions.size() == 0)
		{
		    m_botAction.sendSmartPrivateMessage(name, "There are no modules loaded.  Please !lock to load standard");
		}
		else
		{
			m_botAction.sendSmartPrivateMessage(name, "Loaded modules are:");
			Iterator i = m_extensions.keySet().iterator();
			while (i.hasNext())
			{
				m_botAction.sendSmartPrivateMessage(name, (String) i.next());
			}
		}
	}

	/**
	 * If paramters is "" ie no parameters then the default command list helps are displayed
	 * Lists the help messages for a particular extension
	 * @param name Host Name
	 * @param key Extension Name
	 */
	public void do_help(String name, String key)
	{   
	    if (key == "")
	    {
	        m_botAction.privateMessageSpam(name, m_commandInterpreter.getCommandHelps());
	        return;
	    }
	    
		key = key.toLowerCase();
		if (m_extensions.containsKey(key))
		{
			Collection helps = ((TWLBotExtension) m_extensions.get(key)).getHelpMessages();
			m_botAction.privateMessageSpam(name, helps);
		}
		else
		{
			m_botAction.sendSmartPrivateMessage(name, "You need to !lock the bot to be able to see the help list.");
		}
	}

	/**
	 * List all the help messages from every extension loaded
	 * @param name Name of Host
	 * @param message Extra parameters given by the host. Template for the commandInterpreter.
	 */
	public void do_allHelp(String name, String message)
	{
		m_botAction.privateMessageSpam(name, m_commandInterpreter.getCommandHelps());
		Iterator i = m_extensions.keySet().iterator();
		while (i.hasNext())
		{
			String key = (String) i.next();
			TWLBotExtension ext = (TWLBotExtension) m_extensions.get(key);
			m_botAction.sendSmartPrivateMessage(name, key + " module contains:");
			m_botAction.privateMessageSpam(name, ext.getHelpMessages());
		}
	}

	/**
	 * Makes the bot go to a particular areana.
	 * Can't go to pure number arenas
	 * @param name Host Name
	 * @param arena Arena Name
	 */
	public void do_go(String name, String arena)
	{
		if (!locked && !Tools.isAllDigits(arena))
		{
			actualGo(arena);
		}
		else
		{
			m_botAction.sendSmartPrivateMessage(name, "Sorry, but I am currently " + "locked.  Please !unlock me first.");
		}
	}
	
	/**
	 * Makes the bot go to the default arena
	 * @param name The name of the host who issued the command
	 * @param message Any extra parameters supplied by the host
	 */
	public void do_home(String name, String message)
	{
		if (m_currentArena.equals(m_defaultArena) && !locked)
		{
			m_botAction.sendSmartPrivateMessage(name, "I'm already home.");
		}
		else if (m_currentArena.equals(m_defaultArena) && locked)
		{
		    do_unlock(name, message);
			m_botAction.sendSmartPrivateMessage(name,  "I'm already home, though.");
		}
		else
		{
			m_botAction.sendSmartPrivateMessage(name, "Seeya! It's quittin' time!");
			do_unlock(name, message);
			do_go(name, m_defaultArena);
		}	    
	}

	/**
	 * Sets state to lock and loads default modules
	 * @param name The host
	 * @param message Any parameters that the host might provide
	 */
	public void do_lock(String name, String message)
	{
		if (locked)
		{
			m_botAction.sendSmartPrivateMessage(name, "I'm already locked.  If you want to unlock me, use !unlock.");
			return;
		}
		
		loadDefaultModules(); //loads the twlstandard module
		m_nameOfHost = name;
		m_botAction.sendSmartPrivateMessage(name, "Locked. TWL module loaded.");
	
		locked = true;
	}
	
	/**
	 * Unlocks the bot if it is locked
	 * The bot is also reset and all extensions removed
	 * @param name The name of the person who issued the command
	 * @param message Any parameters given by the host
	 */
	public void do_unlock(String name, String message)
	{
		if (!locked)
		{
			m_botAction.sendSmartPrivateMessage(name, "I'm already unlocked.");
			return;
		}
		
		//clear(); //reset the bot
		m_botAction.sendSmartPrivateMessage(name, "Unlocked");
		m_nameOfHost = null;
		locked = false;
	}
	
	/**
	 * Displays the version of the bot
	 * @param name Name of the host
	 * @param message Any extra parameters given by the host
	 */
	public void do_version(String name, String message)
	{
	    m_botAction.sendSmartPrivateMessage(name, "Version: " + VERSION);
	}
	
	/**
	 * Changes host to the person who has issued the command
	 * @param name The new host
	 * @param message Any extra parameters issued by the host
	 */
	public void do_mybot(String name, String message)
	{
		if (locked)
		{
			if (name.equals(m_nameOfHost))
			{
				m_botAction.sendSmartPrivateMessage(name, "You already own this bot.");
				return;
			}
			m_botAction.sendSmartPrivateMessage(m_nameOfHost, "This bot now belongs to " + name);
			m_botAction.sendSmartPrivateMessage(name, "I'm yours.  Ownership transferred from " + m_nameOfHost);
			m_nameOfHost = name;
		}
		else
		{
			m_botAction.sendSmartPrivateMessage(name, "This bot is unowned, because the bot is unlocked.");
		}
	}
	
	/**
	 * Allows the bot to die in grace.  Ie quit
	 * @param name  Name of the person doing the hidious action
	 * @param message Sick person trying to add injury to insult!
	 */
	public void do_die(String name, String message)
	{
		if (!locked)
		{
			m_botAction.sendChatMessage(1, "I'm dying at " + name + "'s request");
			m_botAction.sendSmartPrivateMessage(name, "Goodbye!");
			try { Thread.sleep(50); } catch (Exception e) {};
			m_botAction.die();
		}
		else
		{
			m_botAction.sendSmartPrivateMessage(name, "I am locked, sorry.");
		}	    
	}
	
	/**
	 * This is a default method if command is wrong for a message type
	 * @param name Name of the host
	 * @param message Any parameters
	 */
	public void do_nothing(String name, String message)
	{
	}
	
	/**
	 * Go to the arena specified
	 * @param arena
	 */
	private void actualGo(String arena)
	{
		m_currentArena = arena;

		try
		{
			m_botAction.changeArena(arena, (short)4096, (short)4096);
		}
		catch (Exception e)
		{
			m_botAction.changeArena(arena);
		}
	}

	/**
	 * Removes all modules from the bot.
	 * Used mainly after changing arenas to reset the bot
	 */
	private void clear()
	{
		Iterator i = m_extensions.values().iterator();
		while (i.hasNext())
		{
			((TWLBotExtension) i.next()).cancel();
		}
		m_extensions.clear();
	}

	/**
	 * Loads Default twl modules
	 */
	private void loadDefaultModules()
	{
	    twlbotstandard std = new twlbotstandard();
	    std.set(m_botAction, m_opList, this);
	    m_extensions.put("standard", std);
	}

	//Event Handlers
	/**
	 * Handles arean joined events
	 * This sets the kills packets to be recorded accurately
	 */
	public void handleEvent(ArenaJoined event)
	{
		m_botAction.setReliableKills(1);
	}
	
	/**
	 * Handles the message Event
	 */
	public void handleEvent(Message event)
	{
		distributeEvent((SubspaceEvent) event);
		String message = event.getMessage();
		if (event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if (message.startsWith("!host"))
			{
				if (m_nameOfHost == null)
					m_botAction.sendPrivateMessage(name, "There is no registered host, or I am not in use currently.");
				else
					m_botAction.sendPrivateMessage(name, "Your current host is: " + m_nameOfHost);
			}
			if (!m_opList.isER(name)) //if normal public quit
				return;
			m_lastUse = 0;
			do_command(name, message);
			m_commandInterpreter.handleEvent(event);
		}
		else if (event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
		{
			String rpmname = event.getMessager();
			String rpmmessage = event.getMessage();
			if (message.startsWith("!games"))
			{
				if (locked)
				{
					if (m_currentArena.startsWith("#"))
					{
						m_botAction.sendSmartPrivateMessage(rpmname, "In use in a private arena.");
					}
					else
					{
						m_botAction.sendSmartPrivateMessage(rpmname, m_nameOfHost + " is hosting " + m_currentArena + ".");
					}
				}
				else
				{
					if (m_currentArena.startsWith("#robopark"))
					{
						m_botAction.sendSmartPrivateMessage(rpmname, "Not in use.");
					}
					else if (m_currentArena.startsWith("#"))
					{
						m_botAction.sendSmartPrivateMessage(rpmname, "Idle in a private arena.");
					}
					else
					{
						m_botAction.sendSmartPrivateMessage(rpmname, "Idle in " + m_currentArena);
					}
				}
			}
			if (!m_opList.isER(rpmname)) //if normal public quit
				return;
			m_lastUse = 0;
			if (message.startsWith("!go "))
			{
				do_go(rpmname, rpmmessage.substring(4)); //arena
			}
			else if (message.startsWith("!come "))
			{
				do_go(rpmname, rpmmessage.substring(6)); //arena
			}
			else if (message.startsWith("!where"))
			{
				if (locked)
					m_botAction.sendSmartPrivateMessage(rpmname, "In use in " + m_currentArena + " by " + m_nameOfHost + ".");
				else
					m_botAction.sendSmartPrivateMessage(rpmname, "In " + m_currentArena);
			}
		}
	}

	/**
	 * Steps done after the bot logs on
	 */
	public void handleEvent(LoggedOn event)
	{
		m_botAction.joinArena(m_defaultArena);
		m_botAction.sendUnfilteredPublicMessage("?chat=robodev");
		m_opList = m_botAction.getOperatorList();
		
		distributeEvent((SubspaceEvent) event);
	}

	/**
	 * Handles player left event
	 */
	public void handleEvent(PlayerLeft event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(SubspaceEvent event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(ScoreReset event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(PlayerEntered event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(PlayerPosition event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(PlayerDeath event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(ScoreUpdate event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(WeaponFired event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(FrequencyChange event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(FrequencyShipChange event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(FileArrived event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(FlagReward event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(FlagVictory event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(WatchDamage event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(SoccerGoal event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(BallPosition event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(Prize event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(FlagPosition event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(FlagClaimed event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(SQLResultEvent event)
	{
		distributeEvent((SubspaceEvent) event);
	}
	public void handleEvent(FlagDropped event)
	{
		distributeEvent((SubspaceEvent) event);
	}

	/**
	 * Distributes the subspace events to any extenstion that is registered 
	 * @param event The event to be distributed 
	 */
	private void distributeEvent(SubspaceEvent event)
	{
		Iterator i = m_extensions.entrySet().iterator();
		while (i.hasNext())
		{
			Map.Entry entry = (Map.Entry) i.next();
			TWLBotExtension ext = (TWLBotExtension) entry.getValue();
			ext.handleEvent(event);
		}
	}	
}

