package twcore.bots.multibot;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.AdaptiveClassLoader;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.ArenaList;
import twcore.core.events.BallPosition;
import twcore.core.events.FileArrived;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FlagDropped;
import twcore.core.events.FlagPosition;
import twcore.core.events.FlagReward;
import twcore.core.events.FlagVictory;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.Prize;
import twcore.core.events.SQLResultEvent;
import twcore.core.events.ScoreReset;
import twcore.core.events.ScoreUpdate;
import twcore.core.events.SoccerGoal;
import twcore.core.events.SubspaceEvent;
import twcore.core.events.TurretEvent;
import twcore.core.events.WatchDamage;
import twcore.core.events.WeaponFired;
import twcore.core.util.Tools;

public class multibot extends SubspaceBot
{
  private static final String CLASS_EXTENSION = ".class";
  private static final String CONFIG_EXTENSION = ".cfg";
  private static final long DIE_DELAY = 500;

  private OperatorList opList;
  private MultiModule multiModule;
  private String followTarget;
  private String initialArena;
  private String modulePath;
  private String botChat;

  public multibot(BotAction botAction)
  {
    super(botAction);

    multiModule = null;
  }

  public void handleEvent(LoggedOn event)
  {
    BotSettings botSettings = m_botAction.getBotSettings();
    String coreRoot = m_botAction.getGeneralSettings().getString("Core Location");

    initialArena = botSettings.getString("initialarena");
    opList = m_botAction.getOperatorList();
    botChat = botSettings.getString("chat");
    modulePath = coreRoot + "/twcore/bots/multibot";

    setChat(botChat);
    m_botAction.changeArena(initialArena);
    handleEvent((SubspaceEvent) event);
    requestStandardEvents();
  }

  /**
   * This method handles a message event.  If the bot is locked, then the event
   * is passed on to the module.  The bot also checks to see if the command
   * should be handled.
   *
   * @param event is the event to handle.
   */
  public void handleEvent(Message event)
  {
    String sender = getSender(event);
    String message = event.getMessage();
    int messageType = event.getMessageType();

    if(isLocked())
      handleEvent((SubspaceEvent) event);
    if(opList.isER(sender))
      handleCommands(sender, message, messageType);
    else if(message.toLowerCase().startsWith("!where"))
   	  doWhereCmd(sender, false);
  }

  /**
   * This method handles a command sent specifically to the bot (not the
   * module).  If the bot is unlocked then a go, follow, listgames, lock, home,
   * die and help will be supported.  Otherwise unlock and help will be
   * supported.
   *
   */
  public void handleCommands(String sender, String message, int messageType)
  {
    String command = message.toLowerCase();

    try
    {
      if(!isLocked())
      {
        if(command.startsWith("!go "))
          doGoCmd(sender, message.substring(4).trim());
        if(command.equals("!follow"))
          doFollowCmd(sender);
        if(command.equals("!listgames"))
          doListGamesCmd(sender);
        if(command.startsWith("!lock "))
          doLockCmd(sender, message.substring(6).trim());
        if(command.equals("!lock"))
          ;
        if(command.equals("!home"))
          doHomeCmd(sender);
        if(command.equals("!die"))
          doDieCmd(sender);
        if(command.equals("!help"))
          doUnlockedHelpMessage(sender);
      }
      else
      {
        if(command.equals("!module"))
          doModuleCmd(sender);
        if(command.equals("!unlock"))
          doUnlockCmd(sender);
        if(command.equals("!help"))
          doLockedHelpMessage(sender);
        if(command.equals("!where"))
          doWhereCmd(sender, true);
      }
    }
    catch(RuntimeException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, "Runtime exception encountered; please notify a member of the coding staff if you believe you have reached this recording in error." );
    }
  }

  /**
   * This method sends the bot to another arena.
   *
   * @param sender is the sender of the command.
   * @param argString is the argument string.
   */
  private void doGoCmd(String sender, String argString)
  {
    String currentArena = m_botAction.getArenaName();

    if(currentArena.equalsIgnoreCase(argString))
      throw new IllegalArgumentException("Bot is already in that arena.");
    if(isPublicArena(argString))
      throw new IllegalArgumentException("Bot can not go into public arenas.");
    m_botAction.changeArena(argString);
    m_botAction.sendSmartPrivateMessage(sender, "Going to " + argString + ".");
  }

  /**
   * This method performs the follow command.  It makes the bot follow the
   * sender.
   *
   * @param sender is the sender of the command.
   */
  private void doFollowCmd(String sender)
  {
    throw new RuntimeException("Not Implemented Yet.");
  }

  /**
   * This method performs the ListGames command.  It displays all of the games
   * that are available in the current arena.
   *
   * @param sender is the sender of the command.
   */
  private void doListGamesCmd(String sender)
  {
    File directory = new File(modulePath);
	File[] files = directory.listFiles(fnf);
    
    Arrays.sort( files );

    m_botAction.sendPrivateMessage(sender, "TW MULTIBOT GAME LIBRARY" );
    m_botAction.sendPrivateMessage(sender, "------------------------" );

    String moduleNames = "";
    int namesinline = 0;

    for (int i = 0; i < files.length; i++) {
      String name = files[i] + "";
      name = name.substring(modulePath.length() + 1);
      moduleNames += Tools.formatString( name, 20 );
      namesinline++;
      if( namesinline >= 3 ) {
          m_botAction.sendPrivateMessage(sender, moduleNames);
          namesinline = 0;
          moduleNames = "";
      }
	}
  }

  public FilenameFilter fnf = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      File f = new File(modulePath, name);

	  if (f.isDirectory()) {

		String[] fileNames = f.list();

        if (containsString(fileNames, name.toLowerCase() + CLASS_EXTENSION) && containsString(fileNames, name.toLowerCase() + CONFIG_EXTENSION)) {
          return true;
        }
		return false;
      }
	  return false;
	}
  };

  /**
   * This method locks the bot with the current game number.
   *
   * @param sender is the sender of the command.
   */
  private void doLockCmd(String sender, String argString)
  {
    loadModule(argString);
    m_botAction.sendSmartPrivateMessage(sender, "Module successfully loaded.");
  }

  /**
   * This method sends the bot back to its home arena.
   *
   * @param sender is the sender of the command.
   */
  private void doHomeCmd(String sender)
  {
    doGoCmd(sender, initialArena);
  }

  /**
   * This method logs the bot off.
   *
   * @param sender is the sender of the command.
   */
  private void doDieCmd(String sender)
  {
    m_botAction.sendSmartPrivateMessage(sender, "Logging Off.");
    m_botAction.sendChatMessage(MultiModule.FIRST_CHAT, "Bot Logging Off.");
    m_botAction.scheduleTask(new DieTask(), DIE_DELAY);
  }

  /**
   * This method performs the module command.  It messages the sender with the
   * name of the module that is loaded.
   *
   * @param sender is the sender of the command.
   */
  private void doModuleCmd(String sender)
  {
    m_botAction.sendSmartPrivateMessage(sender, "Current Module: " + multiModule.getModuleName() + " -- Version: " + multiModule.getVersion() + ".");
  }

  /**
   * This method unlocks the bot by unloading the currently loaded module.
   *
   * @param sender is the sender of the command.
   */
  private void doUnlockCmd(String sender)
  {
    if(!multiModule.isUnloadable())
      throw new IllegalArgumentException("Module can not be unloaded at this time.");
    m_botAction.sendSmartPrivateMessage(sender, "Unloading module: " + multiModule.getModuleName());
    unloadModule();
  }

  /**
   * This method tells the player wehre the bot is.
   *
   * @param sender is the sender of the command.
   */

   private void doWhereCmd(String sender, boolean staff)
   {
   	if(staff)
   	  m_botAction.sendSmartPrivateMessage(sender, "I'm being used in " + m_botAction.getArenaName() + ".");
   	else if(m_botAction.getArenaName().startsWith("#"))
   	  m_botAction.sendSmartPrivateMessage(sender, "I'm being used in a private arena.");
	else
	  m_botAction.sendSmartPrivateMessage(sender, "I'm being used in " + m_botAction.getArenaName() + ".");
   }
  /**
   * This method displays the help message of an unlocked bot to the sender.
   *
   * @param sender is the player that sent the help command.
   */
  private void doUnlockedHelpMessage(String sender)
  {
    String[] message =
    {
      "!Go <ArenaName>               -- Sends the bot to <ArenaName>.",
      "!Follow                       -- Turns follow mode on.",
      "!ListGames                    -- Lists the games that are available in this arena.",
      "!Lock <Game>                  -- Locks the bot and loads game <Game>.",
      "!Home                         -- Returns the bot to " + initialArena + ".",
      "!Die                          -- Logs the bot off.",
      "!Help                         -- Displays this message."
    };

    m_botAction.smartPrivateMessageSpam(sender, message);
  }

  /**
   * This method displays the help message of a locked bot to the sender.
   *
   * @param sender is the player that sent the help command.
   */
  private void doLockedHelpMessage(String sender)
  {
    String[] message =
    {
      "!Module                       -- Displays the module and version that is currently loaded.",
      "!Unlock                       -- Unlocks the bot.",
      "!Help                         -- Displays this message."
    };

    m_botAction.smartPrivateMessageSpam(sender, multiModule.getModHelpMessage());
    m_botAction.smartPrivateMessageSpam(sender, message);
  }

  /**
   * This method checks to see if the bot is locked.
   *
   * @param true is returned if the bot is locked.
   */
  private boolean isLocked()
  {
    return multiModule != null;
  }

  /**
   * This method returns the name of the player who sent a message, regardless
   * of the message type.  If there is no sender then null is returned.
   *
   * @param event is the Message event to handle.
   */
  private String getSender(Message event)
  {
    int messageType = event.getMessageType();
    int playerID;

    if(messageType == Message.ALERT_MESSAGE || messageType == Message.CHAT_MESSAGE ||
       messageType == Message.REMOTE_PRIVATE_MESSAGE)
      return event.getMessager();

    playerID = event.getPlayerID();
    return m_botAction.getPlayerName(playerID);
  }

  /**
   * This method handles all SubspaceEvents.  If the bot is locked, then the
   * event is passed onto the module.
   *
   * @param event is the event to handle.
   */
  public void handleEvent(SubspaceEvent event)
  {
    try
    {
      if(isLocked())
        multiModule.handleEvent(event);
    }
    catch(Exception e)
    {
      m_botAction.sendChatMessage(MultiModule.FIRST_CHAT, "Bot Module Unloaded due to exception.");
      m_botAction.sendArenaMessage("Fatal Error.  Module Unloaded.");
      unloadModule();
      Tools.printStackTrace(e);
    }
  }

  /**
   * This method unloads the loaded module by calling the MultiModules cancel
   * method.
   */
  private void unloadModule()
  {
    multiModule.cancel();
    multiModule = null;
    requestStandardEvents();
    setChat(botChat);
  }

  /**
   * This method loads a multiModule into the bot.  For the module to be loaded:
   * 1) The module must reside in a subdirectory whose parent directory is the
   * current bot directory.
   * 2) There must be a modulename.class file present (All lower case).
   * 3) There must be a modulename.cfg file present (All lower case).
   *
   * @param moduleName is the name of the module to load.
   */
  private void loadModule(String moduleName)
  {
    AdaptiveClassLoader loader;
    Vector repository = new Vector();
    BotSettings moduleSettings;
    String lowerName = moduleName.toLowerCase();
    File directory = new File(modulePath, lowerName);
    String[] fileNames;

    if(!directory.isDirectory())
      throw new IllegalArgumentException("Invalid module name.");
    fileNames = directory.list();
    if(!containsString(fileNames, lowerName + CLASS_EXTENSION))
      throw new IllegalArgumentException("Module missing the appropriate class file.");
    if(!containsString(fileNames, lowerName + CONFIG_EXTENSION))
      throw new IllegalArgumentException("Module missing the appropriate config file.");

    try
    {
      moduleSettings = new BotSettings(new File(directory, lowerName + CONFIG_EXTENSION));
      repository.add(new File(modulePath, lowerName));
      loader = new AdaptiveClassLoader(repository, getClass().getClassLoader());
      if(loader.shouldReload())
        loader.reinstantiate();
      multiModule = (MultiModule) loader.loadClass(getParentClass() + "." + lowerName + "." + lowerName).newInstance();
      multiModule.initialize(m_botAction, moduleSettings);
    }
    catch(Exception e)
    {
      throw new RuntimeException("Error loading " + moduleName + ".");
    }
  }

  /**
   * This method gets the parent class of the multibot.
   *
   * @return the parent class of the multibot is returned.
   */
  private String getParentClass()
  {
    String className = this.getClass().getName();
    int parentEnd = className.lastIndexOf(".");

    return className.substring(0, parentEnd);
  }

  /**
   * This method requests all of the events that multibot needs.  It also
   * declines any events that are currently being requested.
   */
  private void requestStandardEvents()
  {
    EventRequester eventRequester = m_botAction.getEventRequester();

    eventRequester.declineAll();
    eventRequester.request(EventRequester.MESSAGE);
//    eventRequester.request(EventRequester.PLAYER_LEFT_EVENT);
  }

  /**
   * This method searches through an array of Strings to see if there it
   * contains an element that matches target.
   *
   * @param String[] array is the array to search.
   * @param String target is the target to look for.
   * @return true is returned if target is contained in array.
   */
  private boolean containsString(String[] array, String target)
  {
    for(int index = 0; index < array.length; index++)
      if(array[index].equals(target))
        return true;
    return false;
  }

  /**
   * This method checks to see if an arena is a public arena.
   *
   * @param arenaName is the arena name to check.
   * @return true is returned if the arena is a public arena.
   */
  private boolean isPublicArena(String arenaName)
  {
    try
    {
      Integer.parseInt(arenaName);
      return true;
    }
    catch(NumberFormatException e)
    {
      return false;
    }
  }

  /**
   * This method sets the bots chat.
   *
   * @param chatName is the bots chat.
   */
  private void setChat(String chatName)
  {
    m_botAction.sendUnfilteredPublicMessage("?chat=" + chatName);
  }




  public void handleEvent(ArenaList event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(PlayerPosition event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(PlayerLeft event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(PlayerDeath event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(PlayerEntered event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(Prize event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(ScoreUpdate event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(WeaponFired event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(FrequencyChange event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(FrequencyShipChange event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(FileArrived event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(ArenaJoined event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(FlagVictory event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(FlagReward event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(ScoreReset event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(WatchDamage event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(SoccerGoal event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(BallPosition event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(FlagPosition event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(FlagDropped event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(FlagClaimed event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(InterProcessEvent event){handleEvent((SubspaceEvent) event);}

  public void handleEvent(TurretEvent event){handleEvent((SubspaceEvent) event);}

  public void handleEvent( SQLResultEvent event ){handleEvent((SubspaceEvent) event);}

  private class DieTask extends TimerTask
  {
    public void run()
    {
      m_botAction.die();
    }
  }
}