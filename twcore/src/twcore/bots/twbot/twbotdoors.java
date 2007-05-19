package twcore.bots.twbot;

import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.TWBotExtension;
import twcore.core.events.Message;

public class twbotdoors extends TWBotExtension
{
  private int doorState;
  private Vector doorList;

  public twbotdoors()
  {
    doorList = new Vector();
  }

  public String[] getHelpMessages()
  {
    String[] message =
    {
        "!Door <Door Value>                        -- Sets the doors to a certain value.",
        "!DoorOn <Door 1>, <Door 2>, ... <Door n>  -- Closes all doors specified.  May use 'All' to close all.",
        "!DoorOff <Door 1>, <Door 2>, ... <Door n> -- Opens all doors specified.  May use 'All' to open all.",
        "!AddDoor <Door #> <On time> <Off time>    -- Makes door <Door #> close for <On time> and open for <Off time>.",
        "!DoorList                                 -- Shows all of the door tasks.",
        "!DoorDel <Task Number>                    -- Removes a door task specified by <Task Number>.",
        "!DoorsOff                                 -- Clears the door tasks."
    };
    return message;
  }

  public void doDoorCmd(String sender, String argString)
  {
    try
    {
      int doorValue = Integer.parseInt(argString);
      if(doorValue < -2 || doorValue > 255)
        throw new IllegalArgumentException("Invalid door value.");
      m_botAction.setDoors(doorValue);
      m_botAction.sendSmartPrivateMessage(sender, "Doors set to " + doorValue + ".");
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Please use the following format: !Door <Door Value>");
    }
  }

  public int closeDoor(int doorState, int doorNumber)
  {
    return doorState | (int) Math.pow(2, (doorNumber - 1));
  }

  public int openDoor(int doorState, int doorNumber)
  {
    return doorState & ~((int) Math.pow(2, (doorNumber - 1)));
  }

  public void doDoorOnCmd(String sender, String argString)
  {
    if(argString.equalsIgnoreCase("all"))
    {
      m_botAction.setDoors(255);
      m_botAction.sendPrivateMessage(sender, "All doors closed.");
    }
    else
    {
      StringTokenizer argTokens = new StringTokenizer(argString);

      try
      {
        int newDoorState = doorState;
        StringBuffer message = new StringBuffer("Doors Closed: ");

        while(argTokens.hasMoreTokens())
        {
          int doorNumber = Integer.parseInt(argTokens.nextToken());
          if(doorNumber < 1 || doorNumber > 8)
            throw new IllegalArgumentException("Invalid door number.");
          newDoorState = closeDoor(newDoorState, doorNumber);
          message.append(doorNumber + " ");
        }
        m_botAction.setDoors(newDoorState);
        m_botAction.sendSmartPrivateMessage(sender, message.toString());
      }
      catch(NumberFormatException e)
      {
        throw new NumberFormatException("Please use the following format: !DoorOn <Door 1>, <Door 2>, ... <Door n>");
      }
    }
  }

  public void doDoorOffCmd(String sender, String argString)
  {
    if(argString.equalsIgnoreCase("all"))
    {
      m_botAction.setDoors(0);
      m_botAction.sendPrivateMessage(sender, "All doors opened.");
    }
    else
    {
      StringTokenizer argTokens = new StringTokenizer(argString);

      try
      {
        int newDoorState = doorState;
        StringBuffer message = new StringBuffer("Doors Opened: ");

        while(argTokens.hasMoreTokens())
        {
          int doorNumber = Integer.parseInt(argTokens.nextToken());
          if(doorNumber < 1 || doorNumber > 8)
            throw new IllegalArgumentException("Invalid door number.");
          newDoorState = openDoor(newDoorState, doorNumber);
          message.append(doorNumber + " ");
        }
        m_botAction.setDoors(newDoorState);
        m_botAction.sendSmartPrivateMessage(sender, message.toString());
      }
      catch(NumberFormatException e)
      {
        throw new NumberFormatException("Please use the following format: !DoorOff <Door 1>, <Door 2>, ... <Door n>");
      }
    }
  }

  public void handleCommand(String sender, String command)
  {
    try
    {
      updateDoorState();
      if(command.startsWith("!door "))
        doDoorCmd(sender, command.substring(6));
      if(command.startsWith("!dooron "))
        doDoorOnCmd(sender, command.substring(8));
      if(command.startsWith("!dooroff "))
        doDoorOffCmd(sender, command.substring(9));
    }
    catch(RuntimeException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }

  public void updateDoorState()
  {
    m_botAction.sendUnfilteredPublicMessage("?get door:doormode");
  }

  public void handleArenaMessage(String message)
  {
    if(message.startsWith("door:doormode="))
    {
      String doorString = message.substring(14);
      doorState = Integer.parseInt(doorString);
    }
  }

  public void handleEvent(Message event)
  {
    int senderID = event.getPlayerID();
    String sender = m_botAction.getPlayerName(senderID);
    String message = event.getMessage().toLowerCase().trim();
    int messageType = event.getMessageType();

    if(messageType == Message.PRIVATE_MESSAGE && m_opList.isER(sender))
      handleCommand(sender, message);
    if(messageType == Message.ARENA_MESSAGE)
      handleArenaMessage(message);
  }

  public void cancel()
  {
  }

  private class DoorTask extends TimerTask
  {
    public static final int MIN_DOOR = 1;
    public static final int MAX_DOOR = 8;
    public static final double MIN_TIME = 0.1;

    private int doorNumber;
    private double onTime;
    private double offTime;

    public DoorTask(int doorNumber, double onTime, double offTime)
    {
      if(doorNumber < MIN_DOOR || doorNumber > MAX_DOOR)
        throw new IllegalArgumentException("Invalid door number.  Must be from 1 to 8.");
      if(onTime < MIN_TIME || offTime < MIN_TIME)
        throw new IllegalArgumentException("Invalid time specified.");
    }

    public void run()
    {
    }
  }
}