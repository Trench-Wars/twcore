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
 * @version 2.0
 */
public class twlbot extends SubspaceBot
{
    //help messages
	static final String[] helps =
	{
		"The following commands are the base TWLBot:",
		"!help <mocule>     - Lists the !help for module",
		"!allhelp           - Lists help from all extensions if loaded",
		"!lock              - Locks bot so it can't be moved and loads the standard module",
		"!unlock            - Unlocks bot so it can be moved and removes all the modules",
		"!modules           - Lists all the modules that can be loaded",
		"!load <module>     - Loads a module",
		"!unload <module>   - Unloads a module",
		"!loaded            - Shows all the loaded modules",
		"!mybot             - Change of host",
		"!come, !go <arena> - Tells the bot to come to an arena",
		"!die               - Tells the bot to take a hike... off a cliff.",
		"!home              - Tells the bot to unlock and go home",
		"!mybot             - Lets everyone know you are hosting instead of the former host.",
		"!version           - Displays the version of the bot"
	};
	
    //essential subspace bot classes
    private BotAction m_botAction;
	private OperatorList m_opList;
	private BotSettings m_botSettings;
	private AdaptiveClassLoader m_loader;

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
	private final double VERSION = 2.02;

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

	/**
	 * Commands that are issued as messages packets are parsed here
	 * @param name The host
	 * @param message command string
	 */
	public void do_command(String name, String message)
	{

		if (message.startsWith("!load "))
		{
			do_load(name, message.substring(6)); //remove !load from string
		}
		else if (message.startsWith("!modules"))
		{
			do_moduleList(name, message);
		}
		else if (message.startsWith("!unload "))
		{
			do_removeModule(name, message.substring(8));
		}
		else if (message.startsWith("!loaded"))
		{
			do_listLoaded(name);
		}
		else if (message.startsWith("!help "))
		{
		    do_help(name, message.substring(6)); //help for modules
		}
		else if (message.startsWith("!help"))
		{
			m_botAction.privateMessageSpam(name, helps);
		}
		else if (message.startsWith("!allhelp"))
		{
			do_allHelp(name);
		}
		else if (message.startsWith("!go "))
		{
			do_go(name, message.substring(4)); //arena name to follow
		}
		else if (message.startsWith("!come ")) //arena name to follow
		{
			do_go(name, message.substring(6));
		}
		else if (message.startsWith("!home"))
		{
			if (m_currentArena.equals(m_defaultArena) && !locked)
			{
				m_botAction.sendPrivateMessage(name, "I'm already home.");
			}
			else if (m_currentArena.equals(m_defaultArena) && locked)
			{
			    do_Unlock(name);
				m_botAction.sendPrivateMessage(name,  "I'm already home, though.");
			}
			else
			{
				m_botAction.sendPrivateMessage(name, "Seeya! It's quittin' time!");
				do_Unlock(name);
				do_go(name, m_defaultArena);
			}
		}
		else if (message.startsWith("!mybot"))
		{
			if (locked)
			{
				if (name.equals(m_nameOfHost))
				{
					m_botAction.sendPrivateMessage(name, "You already own this bot.");
					return;
				}
				m_botAction.sendPrivateMessage(m_nameOfHost, "This bot now belongs to " + name);
				m_botAction.sendPrivateMessage(name, "I'm yours.  Ownership transferred from " + m_nameOfHost);
				m_nameOfHost = name;
			}
			else
			{
				m_botAction.sendPrivateMessage(name, "This bot is unowned, because the bot is unlocked.");
			}
		}
		else if (message.startsWith("!lock"))
		{
		    do_lock(name);
		}
		else if (message.startsWith("!unlock"))
		{
		    do_Unlock(name);
		}
		else if (message.startsWith("!version"))
		{
		    m_botAction.sendPrivateMessage(name, "Version: " + VERSION);
		}
		else if (message.startsWith("!die"))
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
				m_botAction.sendPrivateMessage(name, "I am locked, sorry.");
			}
		}
	}
	
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
				m_botAction.sendPrivateMessage(name, "Successfully loaded " + extensionType);
			}
			catch (Exception e)
			{
				m_botAction.sendPrivateMessage(name, "Failed to load " + extensionType);
			}
		}
		else
		{
			m_botAction.sendPrivateMessage(name, "Please !lock the bot first before loading a module.");
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
		m_botAction.sendPrivateMessage(name, "I contain the following " + "modules:");
		m_botAction.sendPrivateMessage(name, "A * indicates a loaded module");
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
					m_botAction.sendPrivateMessage(name, reply);
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
	            m_botAction.sendPrivateMessage(name, key + " Successfully Removed");
	        }
	        else
	        {
	            m_botAction.sendPrivateMessage(name, key + " is not loaded, so it cannot be removed.  Keep in mind the " + "names are case sensitive.");
	        }
	    }
	    else
	    {
	        m_botAction.sendPrivateMessage(name, "Cannot remove the standard module.  Use !unlock");
	    }
	}

	/**
	 * List the modules loaded
	 * Standard should be loaded on default unless unlocked
	 * @param name Host Name
	 */
	public void do_listLoaded(String name)
	{
		if (m_extensions.size() == 0)
		{
		    m_botAction.sendPrivateMessage(name, "There are no modules loaded.  Please !lock to load standard");
		}
		else
		{
			m_botAction.sendPrivateMessage(name, "Loaded modules are:");
			Iterator i = m_extensions.keySet().iterator();
			while (i.hasNext())
			{
				m_botAction.sendPrivateMessage(name, (String) i.next());
			}
		}
	}

	/**
	 * Lists the help messages for a particular extension
	 * @param name Host Name
	 * @param key Extension Name
	 */
	public void do_help(String name, String key)
	{
		key = key.toLowerCase();
		if (m_extensions.containsKey(key))
		{
			String[] helps = ((TWLBotExtension) m_extensions.get(key)).getHelpMessages();
			m_botAction.privateMessageSpam(name, helps);
		}
		else
		{
			m_botAction.sendPrivateMessage(name, "You need to !lock the bot to be able to see the help list.");
		}
	}

	/**
	 * List all the help messages from every extension loaded
	 * @param name Name of Host
	 */
	public void do_allHelp(String name)
	{
		m_botAction.privateMessageSpam(name, helps);
		Iterator i = m_extensions.keySet().iterator();
		while (i.hasNext())
		{
			String key = (String) i.next();
			TWLBotExtension ext = (TWLBotExtension) m_extensions.get(key);
			m_botAction.sendPrivateMessage(name, key + " module contains:");
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
	 * Sets state to lock and loads default modules
	 * @param name The host
	 */
	public void do_lock(String name)
	{
		if (locked)
		{
			m_botAction.sendPrivateMessage(name, "I'm already locked.  If you want to unlock me, use !unlock.");
			return;
		}
		
		loadDefaultModules(); //loads the twlstandard module
		m_nameOfHost = name;
		m_botAction.sendPrivateMessage(name, "Locked. TWL module loaded.");
	
		locked = true;
	}
	
	/**
	 * Unlocks the bot if it is locked
	 * The bot is also reset and all extensions removed
	 * @param name The name of the person who issued the command
	 */
	public void do_Unlock(String name)
	{
		if (!locked)
		{
			m_botAction.sendPrivateMessage(name, "I'm already unlocked.");
			return;
		}
		
		clear(); //reset the bot
		m_botAction.sendPrivateMessage(name, "Unlocked");
		m_nameOfHost = null;
		locked = false;
	}
	
	/**
	 * Go to the arena specified
	 * @param arena
	 */
	private void actualGo(String arena)
	{
		clear(); //reset the bot
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
		m_botAction.sendUnfilteredPublicMessage("?chat=botdev");
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

