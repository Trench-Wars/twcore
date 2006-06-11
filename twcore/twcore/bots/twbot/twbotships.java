/**
 * Add everyone in spec as ship x, into y freqs...
 * Randomize the teams into x freqs
 * Randomize the teams to have x ships per freq
 * Limit ship x to y per freq
 * Limit ship x to y per arena
 */

package twcore.bots.twbot;

import java.util.*;

import twcore.bots.TWBotExtension;
import twcore.core.*;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.game.Player;

public class twbotships extends TWBotExtension
{
  public static final int UNLIMITED = -1;
  public static final int MAXSHIP = 8;
  public static final int MINSHIP = 1;
  public static final int SPEC = 0;

  private int[] shipLimits;

  public twbotships()
  {
    shipLimits = new int[MAXSHIP];
    for(int index = 0; index < MAXSHIP; index++)
      shipLimits[index] = UNLIMITED;
  }

  public String[] getHelpMessages()
  {
    String[] message =
    {
      "!LimitsOff                                     -- Turns all of the ship limits off.",
      "!ListLimits                                    -- Displays the current ship limits.",
      "!Limit <Ship> <Limit>                          -- Limits the number of <Ship> to <Limit> per freq.",
      "                                                  To turn the limit off, type !Limit <Ship> Off"
    };

    return message;
  }

  private int getShipTypeOnFreq(int ship, int freq)
  {
    int shipCount = 0;
    Player player;

    Iterator iterator = m_botAction.getPlayerIterator();
    while(iterator.hasNext())
    {
      player = (Player) iterator.next();
      if(player.getShipType() == ship && player.getFrequency() == freq)
        shipCount++;
    }
    return shipCount;
  }

  private void checkLimit(String playerName)
  {
    Player player = m_botAction.getPlayer(playerName);
    int ship = player.getShipType();
    int freq = player.getFrequency();
    int shipTypeOnFreq;
    int limit;

    if(ship != SPEC)
    {
      shipTypeOnFreq = getShipTypeOnFreq(ship, freq);
      limit = shipLimits[ship - 1];
      if(shipTypeOnFreq > limit && limit != UNLIMITED)
      {
        m_botAction.spec(playerName);
        m_botAction.spec(playerName);
        m_botAction.sendSmartPrivateMessage(playerName, "The maximum number of ship " + ship + " has been reached on freq " + freq + ".");
      }
    }
  }

  public void handleEvent(FrequencyChange event)
  {
    int playerID = event.getPlayerID();
    String playerName = m_botAction.getPlayerName(playerID);
    checkLimit(playerName);
  }

  public void handleEvent(FrequencyShipChange event)
  {
    int playerID = event.getPlayerID();
    String playerName = m_botAction.getPlayerName(playerID);
    checkLimit(playerName);
  }

  public void doLimitsOffCmd()
  {
    for(int index = 0; index < MAXSHIP; index++)
      shipLimits[index] = UNLIMITED;
    m_botAction.sendArenaMessage("Ship Limits have been turned off.");
  }

  public void doListLimitsCmd(String sender)
  {
    int limit;
    String limitString;

    for(int index = 0; index < MAXSHIP; index++)
    {
      limit = shipLimits[index];
      if(limit == UNLIMITED)
        limitString = "Off";
      else
        limitString = Integer.toString(limit);
      m_botAction.sendSmartPrivateMessage(sender, "Ship: " + (index + 1) + "  -  Limit: " + limitString + ".");
    }
  }

  public void doLimitCmd(String argString)
  {
    StringTokenizer argTokens = new StringTokenizer(argString);
    String limitString;
    int ship;
    int limit;

    if(argTokens.countTokens() != 2)
      throw new IllegalArgumentException("Please use the following format: !Limit <Ship> <Limit>");
    try
    {
      ship = Integer.parseInt(argTokens.nextToken());
      if(ship < MINSHIP || ship > MAXSHIP)
        throw new IllegalArgumentException("Invalid ship number.");
      limitString = argTokens.nextToken();
      if(limitString.equals("off"))
      {
        limit = UNLIMITED;
        m_botAction.sendArenaMessage("Ship limit for ship " + ship + " has been turned off.");
      }
      else
      {
        limit = Integer.parseInt(limitString);
        if(limit < 0)
          throw new IllegalArgumentException("Invalid ship limit.  If you wish to remove the limit type !Limit <Ship> Off");
        m_botAction.sendArenaMessage("Ship limit for ship " + ship + " has been set to " + limit + " per freq.");
      }
      shipLimits[ship - 1] = limit;
    }
    catch(NumberFormatException e)
    {
      throw new IllegalArgumentException("Please use the following format: !Limit <Ship> <Limit>");
    }
  }

  public void handleCommand(String sender, String message)
  {
    String command = message.toLowerCase();

    try
    {
      if(command.equals("!limitsoff"))
        doLimitsOffCmd();
      if(command.equals("!listlimits"))
        doListLimitsCmd(sender);
      if(command.startsWith("!limit "))
        doLimitCmd(message.substring(7));
    }
    catch(Exception e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }

  public void handleEvent(Message event)
  {
    int senderID = event.getPlayerID();
    String sender = m_botAction.getPlayerName(senderID);
    String message = event.getMessage();

    if(event.getMessageType() == Message.PRIVATE_MESSAGE && m_opList.isER(sender))
      handleCommand(sender, message);
  }

  public void cancel()
  {
  }
}