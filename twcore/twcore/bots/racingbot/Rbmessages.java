/**
 * Messages module for TWBot.
 *
 * This module allows the user 3 methods of messaging:
 * 1) Timed arena messaging.
 * 2) Timed spec freq messaging.
 * 3) Private greet messaging.
 *
 * The syntax for the messages module is as follows:
 *
 * You are able to add an unlimited amount of messages to the bot (including
 * greeting messages although this is not recommended.)  The bot is able to
 * display the timed messages in fractions of seconds all the way down to 0.1
 * seconds.  You will notice that in the help menu, there is an argument
 * reserved for any sounds to be attached to the message.  This parameter is
 * optional, so to add a message with no sound just leave that part out.
 *
 * Here is the help menu,
 * !AddMsg <Msg>,<Sound>,<Interval>          -- Arenas the message <Msg> every <Interval> seconds.",
 * !AddSpecMsg <Msg>,<Sound>,<Interval>      -- Displays the message <Msg> every <Interval> seconds in spec chat.",
 * !AddGreetMsg <Msg>,<Sound>                -- Greets a player with <Msg> when they enter the arena.",
 * !MsgList                                  -- Displays all of the current message tasks.",
 * !MsgDel <Msg Number>                      -- Removes message number <Msg Number>",
 * !MsgsOff                                  -- Turns all of the messages off."
 *
 * NOTE: The !AddMsg command is removed from the standard module so don't forget
 * to !load messages before trying to add message tasks.
 *
 * Author: Cpt.Guano!
 * June 20, 2003
 *
 * Updates:
 * June 29, 2003 - Changed the delimeter to a comma.
 */

package twcore.bots.racingbot;

import java.util.*;
import twcore.core.*;

public class Rbmessages extends RBExtender
{
  private Vector msgList;

  /**
   * This method initializes the messages module.
   */

  public Rbmessages()
  {
    msgList = new Vector();
  }

  /**
   * This method returns an array of Strings representing the help message.
   *
   * @return the help menu is returned.
   */

  public String[] getHelpMessages()
  {
    String[] message =
    {
        "!AddMsg <Msg>,<Sound>,<Interval>          -- Arenas the message <Msg> every <Interval> seconds.",
        "!AddSpecMsg <Msg>,<Sound>,<Interval>      -- Displays the message <Msg> every <Interval> seconds in spec chat.",
        "!AddGreetMsg <Msg>,<Sound>                -- Greets a player with <Msg> when they enter the arena.",
        "!MsgList                                  -- Displays all of the current message tasks.",
        "!MsgDel <Msg Number>                      -- Removes message number <Msg Number>",
        "!MsgsOff                                  -- Turns all of the messages off."
    };
    return message;
  }

  /**
   * This method adds an arena message to the message list.
   *
   * @param sender is the person that is using the bot.
   * @param argString are the arguments being supplied.
   */

  public void doAddMsgCmd(String sender, String argString)
  {
    StringTokenizer argTokens = new StringTokenizer(argString, ",");
    int numArgs = argTokens.countTokens();

    if(numArgs < 2 || numArgs > 3)
      throw new IllegalArgumentException("Please use the following format: !AddMsg <Msg>,<Sound>,<Interval>");
    try
    {
      String message = argTokens.nextToken();
      int soundCode = 0;
      if(numArgs == 3)
        soundCode = Integer.parseInt(argTokens.nextToken());
      double interval = Double.parseDouble(argTokens.nextToken());
      MsgTask msgTask = new MsgTask(message, soundCode, MsgTask.ARENA_TYPE, interval);

      msgList.add(msgTask);
      m_botAction.scheduleTaskAtFixedRate(msgTask, 0, (long) (interval * 1000));
      m_botAction.sendSmartPrivateMessage(sender, "Arena message added: \'" + msgTask.printMessage() + "\'");
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Please use the following format: !AddMsg <Msg>,<Sound>,<Interval>");
    }
  }

  /**
   * This method adds a spec message to the message list.
   *
   * @param sender is the person that is using the bot.
   * @param argString are the arguments being supplied.
   */

  public void doAddSpecMsgCmd(String sender, String argString)
  {
    StringTokenizer argTokens = new StringTokenizer(argString, ",");
    int numArgs = argTokens.countTokens();

    if(numArgs < 2 || numArgs > 3)
      throw new IllegalArgumentException("Please use the following format: !AddSpecMsg <Msg>,<Sound>,<Interval>");
    try
    {
      String message = argTokens.nextToken();
      int soundCode = 0;
      if(numArgs == 3)
        soundCode = Integer.parseInt(argTokens.nextToken());
      double interval = Double.parseDouble(argTokens.nextToken());
      MsgTask msgTask = new MsgTask(message, soundCode, MsgTask.SPEC_TYPE, interval);

      msgList.add(msgTask);
      m_botAction.scheduleTaskAtFixedRate(msgTask, 0, (long) (interval * 1000));
      m_botAction.sendSmartPrivateMessage(sender, "Spec message added: \'" + msgTask.printMessage() + "\'");
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Please use the following format: !AddSpecMsg <Msg>,<Sound>,<Interval>");
    }
  }

  /**
   * This method adds a greeting message to the message list.
   *
   * @param sender is the person that is using the bot.
   * @param argString are the arguments being supplied.
   */

  public void doAddGreetMsgCmd(String sender, String argString)
  {
    StringTokenizer argTokens = new StringTokenizer(argString, ",");
    int numArgs = argTokens.countTokens();

    if(numArgs < 1 || numArgs > 2)
      throw new IllegalArgumentException("Please use the following format: !AddGreetMsg <Msg>,<Sound>");
    try
    {
      String message = argTokens.nextToken();
      int soundCode = 0;
      if(numArgs == 2)
        soundCode = Integer.parseInt(argTokens.nextToken());
      MsgTask msgTask = new MsgTask(message, soundCode);

      msgList.add(msgTask);
      m_botAction.sendSmartPrivateMessage(sender, "Greet message added: \'" + msgTask.printMessage() + "\'");
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Please use the following format: !AddGreetMsg <Msg>,<Sound>");
    }
  }

  /**
   * This method displays the list of message tasks currently in the bot.
   *
   * @param sender is the person that is using the bot.
   */

  public void doMsgListCmd(String sender)
  {
    MsgTask msgTask;

    if(msgList.isEmpty())
      m_botAction.sendSmartPrivateMessage(sender, "No messages registered.");
    else
    {
      for(int index = 0; index < msgList.size(); index++)
      {
        msgTask = (MsgTask) msgList.get(index);
        m_botAction.sendSmartPrivateMessage(sender, "Msg " + index + ") " + msgTask.toString());
      }
    }
  }

  /**
   * This method deletes a message task from the message list.
   *
   * @param sender is the person that is using the bot.
   * @param argString are the arguments being supplied.
   */

  public void doMsgDelCmd(String sender, String argString)
  {
    if(argString.equals(""))
      throw new IllegalArgumentException("Please use the following format: !MsgDel <Msg Number>");
    try
    {
      int index = Integer.parseInt(argString);

      if(index >= msgList.size())
        throw new IllegalArgumentException("Invalid message number.");
      MsgTask msgTask = (MsgTask) msgList.get(index);
      msgTask.cancel();
      msgList.remove(index);
      m_botAction.sendSmartPrivateMessage(sender, "\'" + msgTask.toString() + "\' was removed.");
    }
    catch(NumberFormatException e)
    {
      throw new IllegalArgumentException("Please use the following format: !MsgDel <Msg Number>");
    }
  }

  /**
   * This method cancels all of the message tasks.
   *
   * @param sender is the person that is using the bot.
   */

  public void doMsgsOffCmd(String sender)
  {
    if(msgList.isEmpty())
      m_botAction.sendSmartPrivateMessage(sender, "No messages to clear.");
    else
    {
      cancel();
      msgList.clear();
      m_botAction.sendSmartPrivateMessage(sender, "Messages cleared.");
    }
  }

  /**
   * This method handles all of the bot commands.
   *
   * @param sender is the person that is using the bot.
   * @param command is the command that is going to the bot.
   */

  public void handleCommand(String sender, String command)
  {
    String lowerCommand = command.toLowerCase();

    try
    {
      if(lowerCommand.startsWith("!addmsg "))
        doAddMsgCmd(sender, command.substring(8));
      if(lowerCommand.startsWith("!addspecmsg "))
        doAddSpecMsgCmd(sender, command.substring(12));
      if(lowerCommand.startsWith("!addgreetmsg "))
        doAddGreetMsgCmd(sender, command.substring(13));
      if(lowerCommand.equalsIgnoreCase("!msglist"))
        doMsgListCmd(sender);
      if(lowerCommand.startsWith("!msgdel "))
        doMsgDelCmd(sender, command.substring(8));
      if(lowerCommand.equalsIgnoreCase("!msgsoff"))
        doMsgsOffCmd(sender);
    }
    catch(RuntimeException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }

  /**
   * This method handles message events.
   *
   * @param event this is the message that the bot recieves.
   */

  public void handleEvent(Message event)
  {
    int senderID = event.getPlayerID();
    String sender = m_botAction.getPlayerName(senderID);
    String command = event.getMessage().trim();

    if(event.getMessageType() == Message.PRIVATE_MESSAGE && (m_opList.isER(sender) || m_rbBot.twrcOps.contains(sender.toLowerCase())))
      handleCommand(sender, command);
  }

  /**
   * This method handles the player entered event.  It will display the greet
   * messages if there are any that are currently registered.
   *
   * @param event this is the PlayerEntered event.
   */

  public void handleEvent(PlayerEntered event)
  {
    MsgTask msgTask;
    String playerName = event.getPlayerName();

    for(int index = 0; index < msgList.size(); index++)
    {
      msgTask = (MsgTask) msgList.get(index);
      if(msgTask.getType() == MsgTask.GREET_TYPE)
        m_botAction.sendSmartPrivateMessage(playerName, msgTask.getMessage(), msgTask.getSoundCode());
    }
  }

  /**
   * This method cancels all of the message tasks.
   */

  public void cancel()
  {
    MsgTask msgTask;

    for(int index = 0; index < msgList.size(); index++)
    {
      msgTask = (MsgTask) msgList.get(index);
      msgTask.cancel();
    }
  }

  /**
   * This private class describes a message task.  It contains information with
   * the message, the task type, the sound code and the interval of messaging.
   */

  private class MsgTask extends TimerTask
  {
    public static final int ARENA_TYPE = 0;
    public static final int SPEC_TYPE = 1;
    public static final int GREET_TYPE = 2;
    public static final double MIN_INTERVAL = 0.1;

    String message;
    int soundCode;
    int taskType;
    double interval;

    /**
     * This constructor initializes a GREET_TYPE message task.
     *
     * @param message is the message that is to be displayed.
     * @param soundCode is the sound that is to be made.
     */

    public MsgTask(String message, int soundCode)
    {
      if(soundCode < 0 || soundCode > 999)
        throw new IllegalArgumentException("Invalid sound code.");
      this.message = message;
      this.soundCode = soundCode;
      taskType = GREET_TYPE;
    }

    /**
     * This constructor initializes a message task.
     *
     * @param message is the message that is to be displayed.
     * @param soundCode is the sound that is to be made.
     * @param taskType is the type of message task that it is to be.
     * @param interval is the interval between messages.
     */

    public MsgTask(String message, int soundCode, int taskType, double interval)
    {
      if(soundCode < 0 || soundCode > 999)
        throw new IllegalArgumentException("Invalid sound code.");
      if(taskType < ARENA_TYPE || taskType > SPEC_TYPE)
        throw new IllegalArgumentException("ERROR: Invalid task type.");
      if(interval <= 0)
        throw new IllegalArgumentException("Invalid interval between messages.");
      if(interval < MIN_INTERVAL)
        throw new IllegalArgumentException("Interval between messages is too small.");
      this.message = message;
      this.soundCode = soundCode;
      this.taskType = taskType;
      this.interval = interval;
    }

    /**
     * This method returns the message type.
     *
     * @return the taskType is returned.
     */

    public int getType()
    {
      return taskType;
    }

    /**
     * This method returns the message.
     *
     * @return the message is returned.
     */

    public String getMessage()
    {
      return message;
    }

    /**
     * This method returns the sound code.
     *
     * @return the sound code is returned.
     */

    public int getSoundCode()
    {
      return soundCode;
    }

    /**
     * This method returns a string representation of the the message and the
     * sound.
     *
     * @return the message and the sound code is returned.
     */

    public String printMessage()
    {
      if(soundCode == 0)
        return message;
      return message + " %" + soundCode;
    }

    /**
     * This method returns a string representation of the the message task.
     *
     * @return the message type, interval and sound is returned.
     */

    public String toString()
    {
      switch(taskType)
      {
        case ARENA_TYPE:
          return "Arena Message: " + printMessage() + " every " + interval + " seconds.";
        case SPEC_TYPE:
          return "Spec Message: " + printMessage() + " every " + interval + " seconds.";
        case GREET_TYPE:
          return "Greet Message: " + printMessage();
        default:
          return "ERROR: Invalid task type.";
      }
    }

    /**
     * This method displays a timed message based on the message type.
     */

    public void run()
    {
      switch(taskType)
      {
        case ARENA_TYPE:
          m_botAction.sendArenaMessage(message, soundCode);
          break;
        case SPEC_TYPE:
          m_botAction.sendTeamMessage(message, soundCode);
          break;
      }
    }
  }
}