package twcore.bots.multibot.util;

import java.util.StringTokenizer;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.util.ModuleEventRequester;

public class utilremote extends MultiUtil
{
  public static final int MIN_COORD = 0;
  public static final int MAX_COORD = 1024;

  /**
   * This method initializes the module.
   */
  public void init()
  {
  }
  
  /**
   * This method requests all of the events that the module will use.
   */
  public void requestEvents( ModuleEventRequester modEventReq ) {
      modEventReq.request(this, EventRequester.PLAYER_POSITION );
  }

  /**
   * This method checks to see if a module is unloadable or not.
   *
   * @returns true if the module can be unloaded.
   */
  public boolean isUnloadable()
  {
    return true;
  }

  /**
   * This method gets the help message that is displayed to ER+.
   *
   * @returns an array of Strings containing the ER help message is returned.
   */
  public String[] getHelpMessages()
  {
    String[] message =
    {
      "!View <x> <y>                 -- Sets the bot to spec a certain location."
    };
    return message;
  }

  /**
   * This method handles a message event sent to the bot.
   *
   * @param event is the event to handle.
   */
  public void handleEvent(Message event)
  {
    String sender = getSender(event);
    String message = event.getMessage();
    int messageType = event.getMessageType();

    if(m_opList.isSmod(sender) && messageType == Message.PRIVATE_MESSAGE ||
       messageType == Message.REMOTE_PRIVATE_MESSAGE)
      handleSmodCommand(sender, message);
  }

  /**
   * This method handles an smod command that was pmed to the bot.
   *
   * @param sender is the sender of the command.
   * @param message is the message that was sent.
   */
  public void handleSmodCommand(String sender, String message)
  {
    try
    {
      String command = message.toLowerCase();

      if(command.startsWith("!view "))
        doViewCmd(sender, message.substring(6));
    }
    catch(RuntimeException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }

  /**
   * This method performs the view command.  It makes the bot spec at certain
   * coordinates.
   *
   * @param sender is the player that sent the command.
   * @param argString are the arguments passed into the command.
   */
  public void doViewCmd(String sender, String argString)
  {
    StringTokenizer argTokens = new StringTokenizer(argString);
    int xCoord;
    int yCoord;

    if(argTokens.countTokens() != 2)
      throw new IllegalArgumentException("Please use the following format: !View <x> <y>");
    xCoord = parseCoord(argTokens.nextToken());
    yCoord = parseCoord(argTokens.nextToken());
    m_botAction.moveToTile(xCoord, yCoord);
  }

  /**
   * This method converts a string to a coord.  If the string is NAN then an
   * exception is thrown.  If the coordinate is smaller than MIN_COORD or
   * larger than MAX_COORD, then an exception is thrown.
   *
   * @param string is the String to parse.
   * @returns the coordinate is returned.
   */
  public int parseCoord(String string)
  {
    int coord;

    try
    {
      coord = Integer.parseInt(string);
    }
    catch(NumberFormatException e)
    {
      throw new IllegalArgumentException("Invalid coordinate specified.");
    }

    if(coord < MIN_COORD || coord > MAX_COORD)
      throw new IllegalArgumentException("Coordinates must be between " + MIN_COORD + " and " + MAX_COORD + ".");
    return coord;
  }

  /**
   * This method gets the sender of the message regardless of the message type.
   *
   * @param event is the message event.
   * @returns the name of the sender of the message is returned.  If there was
   * no sender, then null is returned.
   */
  private String getSender(Message event)
  {
    int messageType = event.getMessageType();
    int senderID;

    if(messageType == Message.ALERT_MESSAGE || messageType == Message.CHAT_MESSAGE ||
       messageType == Message.REMOTE_PRIVATE_MESSAGE)
      return event.getMessager();
    senderID = event.getPlayerID();
    return m_botAction.getPlayerName(senderID);
  }

  /**
   * This method handles a player position event.
   *
   * @param event is the event to handle.
   */
  public void handleEvent(PlayerPosition event)
  {
    int playerID = event.getPlayerID();
    int xCoord = event.getXLocation();
    int yCoord = event.getYLocation();
    String playerName = m_botAction.getPlayerName(playerID);

    m_botAction.sendChatMessage(playerName + " is at: " + xCoord + ", " + yCoord + ".");
  }
}