package twcore.core;
import java.util.*;
import java.io.*;

public class HubBot extends SubspaceBot
{
	private BotQueue m_botQueue;
	private CommandInterpreter m_commandInterpreter;
	private ThreadGroup m_kingGroup;
	private SQLManager m_manager;

	public HubBot(BotAction botAction)
	{
		super(botAction);
		m_manager = botAction.getCoreData().getSQLManager();
		m_kingGroup = new ThreadGroup("KingGroup");
		m_commandInterpreter = new CommandInterpreter(botAction);
		registerCommands();
		EventRequester events = m_botAction.getEventRequester();
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.LOGGED_ON);
		events.request(EventRequester.FILE_ARRIVED);
		try
		{
			Thread.sleep(5000);
		}
		catch (InterruptedException ie)
		{
			Tools.printLog("This should not happen");
		}
		m_botQueue = new BotQueue(m_kingGroup, botAction);
		m_botQueue.start();
	}

	public void registerCommands()
	{

		int acceptedMessages = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;

		m_commandInterpreter.registerCommand("!help", acceptedMessages, this, "handleHelp");
		m_commandInterpreter.registerCommand("!remove", acceptedMessages, this, "handleRemove");
		m_commandInterpreter.registerCommand("!listbots", acceptedMessages, this, "handleListBots");
		m_commandInterpreter.registerCommand("!spawn", acceptedMessages, this, "handleSpawnMessage");
		m_commandInterpreter.registerCommand("!listbottypes", acceptedMessages, this, "handleListBotTypes");
		m_commandInterpreter.registerCommand("!updateaccess", acceptedMessages, this, "handleUpdateAccess");
		m_commandInterpreter.registerCommand("!waitinglist", acceptedMessages, this, "handleShowWaitingList");

		m_commandInterpreter.registerDefaultCommand(Message.PRIVATE_MESSAGE, this, "handleInvalidMessage");
		m_commandInterpreter.registerDefaultCommand(Message.REMOTE_PRIVATE_MESSAGE, this, "handleInvalidMessage");
	}

	public void handleEvent(LoggedOn event)
	{
		m_botAction.joinArena("#robopark");
		m_botAction.sendUnfilteredPublicMessage("*g*misc:alertcommand");
		m_botAction.sendUnfilteredPublicMessage("?chat=" + m_botAction.getGeneralSettings().getString("Chat name"));
		LoadAccessLists();
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(m_botAction.getCoreCfg("autoload.cfg")));
			String s = "";
			while (true)
			{
				s = reader.readLine();
				if (s == null || s.equals(""))
				{
					break;
				}
				char firstChar = s.trim().charAt(0);
				if (firstChar != '#' && firstChar != '[')
				{
					spawn("AutoLoader", s);
				}
			}
		}
		catch (Exception e)
		{
			Tools.printStackTrace("Exception while auto-loading bots", e);
		}
	}

	public void handleEvent(Message event)
	{

		m_commandInterpreter.handleEvent(event);
	}

	public void handleEvent(FileArrived event)
	{
		String fileName = event.getFileName();

		if (fileName.compareTo("moderate.txt") == 0)
		{
			m_botAction.getOperatorList().parseFile(m_botAction.getDataFile("moderate.txt"), OperatorList.MODERATOR_LEVEL);
			m_botAction.getOperatorList().changeAllMatches("<ZH>", OperatorList.ZH_LEVEL);
			m_botAction.getOperatorList().changeAllMatches("<ER>", OperatorList.ER_LEVEL);
		}
		else if (fileName.compareTo("smod.txt") == 0)
		{
			m_botAction.getOperatorList().parseFile(m_botAction.getDataFile("smod.txt"), OperatorList.SMOD_LEVEL);
		}
		else if (fileName.compareTo("sysop.txt") == 0)
		{
			m_botAction.getOperatorList().parseFile(m_botAction.getDataFile("sysop.txt"), OperatorList.SYSOP_LEVEL);
		}
	}

	public void LoadAccessLists()
	{

		m_botAction.getOperatorList().clearList();
		m_botAction.getOperatorList().parseFile(m_botAction.getCoreCfg("owners.cfg"), OperatorList.OWNER_LEVEL);
		m_botAction.getOperatorList().parseFile(m_botAction.getCoreCfg("remote.cfg"), OperatorList.REMOTE_LEVEL);
		m_botAction.getOperatorList().parseFile(m_botAction.getCoreCfg("highmod.cfg"), OperatorList.HIGHMOD_LEVEL);
		m_botAction.sendUnfilteredPublicMessage("*getmodlist");
		m_botAction.sendUnfilteredPublicMessage("*getfile smod.txt");
		m_botAction.sendUnfilteredPublicMessage("*getfile sysop.txt");
	}

	public void handleInvalidMessage(String messager, String message)
	{

		m_botAction.sendChatMessage(1, messager + " said this: " + message);
	}

	public void handleRemove(String messager, String message)
	{
		String className = message.trim();

		if (m_botAction.getOperatorList().isHighmod(messager) == true)
		{
			m_botQueue.removeBot(message);
                        m_botAction.sendPrivateMessage(messager, "Removed.");
		}
		else
		{
			m_botAction.sendChatMessage(1, messager + " isn't an High Moderator, but he tried !remove " + message);
		}
	}

	public void handleShowWaitingList(String messager, String message)
	{
		String className = message.trim();

		if (m_botAction.getOperatorList().isER(messager) == true)
		{
			m_botQueue.listWaitingList(messager);
		}
		else
		{
			m_botAction.sendChatMessage(1, messager + " isn't a ER+, but he tried !waitinglist " + message);
		}
	}

	public void handleUpdateAccess(String messager, String message)
	{

		if (m_botAction.getOperatorList().isHighmod(messager) == true)
		{
			LoadAccessLists();
			m_botAction.sendSmartPrivateMessage(messager, "Updating access levels...");
			m_botAction.sendChatMessage(1, "Updating access levels at " + messager + "'s request");
		}
		else
		{
			m_botAction.sendChatMessage(1, messager + " isn't an High Moderator, but he tried !updateaccess " + message);
		}
	}

	public void handleListBotTypes(String messager, String message)
	{

		if (m_botAction.getOperatorList().isSmod(messager) == true)
		{
			m_botQueue.listBotTypes(messager);
		}
		else
		{
			m_botAction.sendChatMessage(1, messager + " isn't an smod, but he tried !listbottypes " + message);
		}
	}

	public void handleListBots(String messager, String message)
	{
		String className = message.trim();

		if (m_botAction.getOperatorList().isSmod(messager) == true)
		{
			if (className.length() > 0)
			{
				m_botQueue.listBots(className, messager);
			}
			else
			{
				m_botAction.sendSmartPrivateMessage(messager, "Usage: !listbots <bot type>");
			}
		}
		else
		{
			m_botAction.sendChatMessage(1, messager + " isn't an smod, but he tried !listbots " + message);
		}
	}

	public void handleHelp(String messager, String message)
	{

		if (m_botAction.getOperatorList().isER(messager) == true)
		{
			m_botAction.sendSmartPrivateMessage(messager, "!help - Displays this message.");
			m_botAction.sendSmartPrivateMessage(messager, "!spawn <bot type> - spawns a new bot.");
			m_botAction.sendSmartPrivateMessage(messager, "!waitinglist - Displays the waiting list.");
		}

		if (m_botAction.getOperatorList().isHighmod(messager) == true)
		{
			m_botAction.sendSmartPrivateMessage(messager, "!updateaccess - Rereads the mod, smod, and sysop file so that all access levels are updated.");
			m_botAction.sendSmartPrivateMessage(messager, "!remove <bot> - Removes <bot> from the zone. Use exact capitalization.");
		}

		if (m_botAction.getOperatorList().isSmod(messager) == true)
		{
			m_botAction.sendSmartPrivateMessage(messager, "!listbottypes - Lists the number of each bot type burrently in use.");
			m_botAction.sendSmartPrivateMessage(messager, "!listbots <bot type> - Lists the names and spawners of a bot type.");
		}
	}

	public void spawn(String messager, String message)
	{
		String className = message.trim();
		if (className.length() > 0)
		{
			m_botQueue.spawnBot(className, messager);
		}
		else
		{
			m_botAction.sendSmartPrivateMessage(messager, "Usage: !spawn <bot type>");
		}
	}

	public void handleSpawnMessage(String messager, String message)
	{
		if (m_botAction.getOperatorList().isER(messager) == true)
		{
			spawn(messager, message);
		}
		else
		{
			m_botAction.sendChatMessage(1, messager + " isn't a ER+, but he tried !spawn " + message);
		}
	}
}
