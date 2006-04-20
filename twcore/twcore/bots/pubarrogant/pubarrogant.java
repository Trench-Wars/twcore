package twcore.bots.pubarrogant;

import twcore.core.*;
import java.util.*;
import java.text.*;
import java.io.*;

public class pubarrogant extends SubspaceBot
{
  public static final int MIN_ROAM_TIME = 1 * 60 * 1000;
  public static final int MAX_ROAM_TIME = 3 * 60 * 1000;
  public static final int IDLE_KICK_TIME = 15 * 60;
  public static final int LOWERSTAFF_IDLE_KICK_TIME = 60 * 60;
  public static final int CHECK_LOG_TIME = 30 * 1000;
  public static final int COMMAND_CLEAR_TIME = 3 * 60 * 60 * 1000;
  public static final int DIE_DELAY = 500;
  public static final int ENTER_DELAY = 5000;
  public static final int DEFAULT_CHECK_TIME = 30;

  private SimpleDateFormat dateFormat;
  private SimpleDateFormat fileNameFormat;
  private OperatorList opList;
  private String currentArena;
  private RoamTask roamTask;
  private HashSet accessList;
  private String target;
  private Date lastLogDate;
  private Vector commandQueue;
  private FileWriter logFile;
  private String logFileName;
  private int year;
  private boolean isStaying;
  private boolean isArroSpy;

  /**
   * This method initializes the mrarrogant class.
   */

  public pubarrogant(BotAction botAction)
  {
    super(botAction);

    requestEvents();
    roamTask = new RoamTask();
    lastLogDate = null;
    commandQueue = new Vector();
    accessList = new HashSet();
    isStaying = false;
  }

  /**
   * This method handles the logon event and sets up the bot.
   *
   * @param event is the LoggedOn event to handle.
   */

  public void handleEvent(LoggedOn event)
  {
    BotSettings botSettings = m_botAction.getBotSettings();
    String initialArena = botSettings.getString("initialarena");
    String accessString = botSettings.getString("accesslist");
    changeArena(initialArena);
    opList = m_botAction.getOperatorList();
    setupAccessList(accessString);
  }

  /**
   * This method handles a message event.
   *
   * @param event is the Message event to handle.
   */

  public void handleEvent(Message event)
  {
    String sender = getSender(event);
    String message = event.getMessage();
    int messageType = event.getMessageType();

    if(messageType == Message.ARENA_MESSAGE)
      handleArenaMessage(message);
    if(messageType == Message.REMOTE_PRIVATE_MESSAGE || messageType == Message.PRIVATE_MESSAGE)
      handleCommand(sender, message);
  }

  /**
   * This method gets a string representation of the message type.
   *
   * @param messageType is the type of message to handle
   * @returns a string representation of the message is returned.
   */
  private String getMessageTypeString(int messageType)
  {
    switch(messageType)
    {
      case Message.PUBLIC_MESSAGE:
        return "Public";
      case Message.PRIVATE_MESSAGE:
        return "Private";
      case Message.TEAM_MESSAGE:
        return "Team";
      case Message.OPPOSING_TEAM_MESSAGE:
        return "Opp. Team";
      case Message.ARENA_MESSAGE:
        return "Arena";
      case Message.PUBLIC_MACRO_MESSAGE:
        return "Pub. Macro";
      case Message.REMOTE_PRIVATE_MESSAGE:
        return "Private";
      case Message.WARNING_MESSAGE:
        return "Warning";
      case Message.SERVER_ERROR:
        return "Serv. Error";
      case Message.ALERT_MESSAGE:
        return "Alert";
    }
    return "Other";
  }

  /**
   * This method handles server output.
   *
   * @param message is the arena message from the server.
   */

  private void handleArenaMessage(String message)
  {
    try
    {
      if(message.startsWith("IP:"))
        updateTarget(message);
      else if(message.startsWith(target + ": " + "UserId: "))
        killIdle(message);
    }
    catch(Exception e)
    {
    }
  }

  /**
   * This method kills a player if he / she is idle.
   *
   * @param message is the arena message from the server.
   */

  private void killIdle(String message)
  {
    if(opList.isSmod(target))
      return;
    int idleTime = getIdleTime(message);
    if(idleTime > LOWERSTAFF_IDLE_KICK_TIME || (idleTime > IDLE_KICK_TIME && !opList.isZH(target)))      
      m_botAction.sendUnfilteredPrivateMessage(target, "*kill");
  }


  /**
   * This method gets the sender of a command.
   *
   * @param message is the message to parse.
   * @return the sender of the command is returned.
   */

  private String getFromPlayer(String message)
  {
    int endIndex = message.indexOf(" (");
    if(endIndex == -1)
      return null;
    return message.substring(0, endIndex);
  }

  /**
   * This method gets the player that the command is destined for.  If there is
   * no target of the command, then an empty string is returned.
   *
   * @param message is the log message to parse.
   * @return the name of the target of the command is returned.  If there is
   * no target then an empty string is returned.
   */

  private String getToPlayer(String message)
  {
    int beginIndex = 0;
    int endIndex;

    for(;;)
    {
      beginIndex = message.indexOf(") to ", beginIndex);
      if(beginIndex == -1)
        return "";
      beginIndex += 5;
      endIndex = message.indexOf(":", beginIndex);
      if(endIndex != -1)
        break;
    }
    return message.substring(beginIndex, endIndex);
  }

  /**
   * This method updates the target based on the *info message that is received.
   *
   * @param message is the info message that was received.
   */

  private void updateTarget(String message)
  {
    int beginIndex = message.indexOf("TypedName:") + 10;
    int endIndex = message.indexOf("  ", beginIndex);

    target = message.substring(beginIndex, endIndex);
  }

  /**
   * This method gets the idle time of a player from an *einfo message.
   *
   * @param message is the einfo message to parse.
   * @return the idle time in seconds is returned.
   */

  private int getIdleTime(String message)
  {
    int beginIndex = message.indexOf("Idle: ") + 6;
    int endIndex = message.indexOf(" s", beginIndex);

    if(beginIndex == -1 || endIndex == -1)
      throw new RuntimeException("Cannot get idle time.");
    return Integer.parseInt(message.substring(beginIndex, endIndex));
  }

  /**
   * This method handles any private message sent to the bot.
   *
   * @param sender is the sender of the message.
   * @param message is the message that was sent.
   */

  private void handleCommand(String sender, String message)
  {
    String command = message.toLowerCase();

    try
    {
      if(opList.isHighmod(sender) || accessList.contains(sender.toLowerCase()))
      {
        if(command.startsWith("!go "))
          doGoCmd(sender, message.substring(4));
        if(command.equals("!help"))
          doPrivHelpCmd(sender);
        if(command.equals("!die"))
          doDieCmd();
      }
    }
    catch(RuntimeException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }


  private void doDieCmd()
  {
    m_botAction.scheduleTask(new DieTask(), DIE_DELAY);
  }

  /**
   * This method handles the !go command by moving the bot from one arena to
   * another.
   *
   * @param sender is the person who sent the command.
   * @param argString is the new arena to go to.
   */

  private void doGoCmd(String sender, String argString)
  {
    if(argString.equalsIgnoreCase(currentArena))
      throw new IllegalArgumentException("Already in that arena.");
    m_botAction.sendSmartPrivateMessage(sender, "Going to " + argString + ".");
    changeArena(argString);
  }

  /**
   * This method changes the arena and updates the currentArena.  It also
   * schedules a new roam task and kills all of the idlers.
   */

  private void changeArena(String arenaName)
  {
    int beginIndex = 0;
    int endIndex;

    beginIndex = arenaName.indexOf("(Public ");
    endIndex = arenaName.indexOf(")");

    if( beginIndex != -1 && endIndex != -1 && beginIndex <= endIndex)
      arenaName = arenaName.substring(beginIndex, endIndex); 
      
    m_botAction.changeArena(arenaName);
    currentArena = arenaName;
    m_botAction.scheduleTask(new KillIdlersTask(), ENTER_DELAY);
    scheduleRoamTask();
  }

  /**
   * This method kills all of the idlers in the arena.
   */

  private void killIdlers()
  {
    Iterator iterator = m_botAction.getPlayerIDIterator();
    Integer playerID;
    String playerName;

    while(iterator.hasNext())
    {
      playerID = (Integer) iterator.next();
      playerName = m_botAction.getPlayerName(playerID.intValue());
      m_botAction.sendUnfilteredPrivateMessage(playerName, "*info");
      m_botAction.sendUnfilteredPrivateMessage(playerName, "*einfo");
    }
  }

  /**
   * This method displays the help message.
   *
   * @param sender is the person to receive the help message.
   */

  private void doPrivHelpCmd(String sender)
  {
    String[] message =
    {
      "!Go <ArenaName>                     -- Moves the bot to <ArenaName>.",
      "!Help                               -- Displays this help message.",
      "!Die                                -- Logs the bot off."
    };
    m_botAction.smartPrivateMessageSpam(sender, message);
  }

  /**
   * This method handles the ArenaList event and takes the bot to a random
   * arena.
   *
   * @param event is the ArenaList to handle.
   */

  public void handleEvent(ArenaList event)
  {
    try
    {
      String[] arenaNames = event.getArenaNames();

        Comparator a = new Comparator()
        {
            public int compare(Object oa, Object ob)
            {
                String a = (String)oa;
                String b = (String)ob;
                if (Tools.isAllDigits(a) && !a.equals("") ) {
                    if (Tools.isAllDigits(b) && !b.equals("") ) {
                        if (Integer.parseInt(a) < Integer.parseInt(b)) {
                            return -1;
                        } else {
                            return 1;
                        }
                    } else {
                        return -1;
                    }
                } else if (Tools.isAllDigits(b)) {
                    return 1;
                } else {
                    return a.compareToIgnoreCase(b);
				}
            };
        };

      Arrays.sort(arenaNames, a);
      int endIndex = 0;
      for(int k = 0;k < arenaNames.length;k++) {
      	if(Tools.isAllDigits(arenaNames[k])) endIndex = k;
      	else break;
      }
      
      int arenaIndex = (int) (Math.random() * endIndex);
      changeArena(arenaNames[arenaIndex]);
    }
    catch(Exception e)
    {
    }
  }

  /**
   * This method requests the events that the bot will use.
   */

  private void requestEvents()
  {
    EventRequester eventRequester = m_botAction.getEventRequester();

    eventRequester.request(EventRequester.MESSAGE);
    eventRequester.request(EventRequester.ARENA_LIST);
  }

  /**
   * This method gets the sender of a message regardless of its type.
   *
   * @param message is the message that was sent.
   * @return the sender is returned.
   */

  private String getSender(Message message)
  {
    int messageType = message.getMessageType();
    if(messageType == Message.REMOTE_PRIVATE_MESSAGE || messageType == Message.CHAT_MESSAGE)
      return message.getMessager();
    int senderID = message.getPlayerID();
    return m_botAction.getPlayerName(senderID);
  }

  /**
   * This method schedules a new roam task that is to happen at a random time.
   */

  private void scheduleRoamTask()
  {
    int roamTime = MIN_ROAM_TIME + (int) (Math.random() * (MAX_ROAM_TIME - MIN_ROAM_TIME));
    roamTask.cancel();
    roamTask = new RoamTask();

    m_botAction.scheduleTask(roamTask, roamTime);
  }

  private void setupAccessList(String accessString)
  {
    StringTokenizer accessTokens = new StringTokenizer(accessString, ":");

    accessList.clear();

    while(accessTokens.hasMoreTokens())
      accessList.add(accessTokens.nextToken().toLowerCase());
  }

  /**
   * This helper method makes the bot move around periodically.
   */

  private class RoamTask extends TimerTask
  {
    public void run()
    {
      if(!isStaying)
        m_botAction.requestArenaList();
      scheduleRoamTask();
    }
  }

  private class DieTask extends TimerTask
  {
    public void run()
    {
      try
      {
        m_botAction.die();
      }
      catch(Exception e)
      {
      }
    }
  }

  private class KillIdlersTask extends TimerTask
  {
    public void run()
    {
      killIdlers();
    }
  }
}
