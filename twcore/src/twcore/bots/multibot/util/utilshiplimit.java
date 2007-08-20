package twcore.bots.multibot.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

public class utilshiplimit extends MultiUtil
{
  public static final int UNLIMITED = -1;
  public static final int MAXSHIP = 8;
  public static final int MINSHIP = 1;
  public static final int SPEC = 0;

  private int[] shipLimits;
  private Random rand = new Random();
  
  /**
   * Initializes variables.
   */
  
  public void init()	{
	  shipLimits = new int[MAXSHIP];
	  for(int index = 0; index < MAXSHIP; index++)
		  shipLimits[index] = UNLIMITED;
  }
  
  /**
   * Requests events.
   */
  
  public void requestEvents( ModuleEventRequester events ) {
	  events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
	  events.request(this, EventRequester.FREQUENCY_CHANGE );
  }
  
  /**
   * Turns all limits off.
   */

  public void doLimitsOffCmd()
  {
    for(int index = 0; index < MAXSHIP; index++)
      shipLimits[index] = UNLIMITED;
    m_botAction.sendArenaMessage("Ship Limits have been turned off.");
  }
  
  /**
   * Lists all limits set.
   * 
   * @param sender is the user of the bot.
   */

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
  
  /**
   * Sets ship limits
   * 
   * @param argString is an argument string to be tokenized and
   * translated.
   */

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
  
  /**
   * Checks the player for ship restrictions.
   * 
   * @param playerName
   */
  
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
    	  m_botAction.setShip(playerName, getValidShip());
        m_botAction.sendSmartPrivateMessage(playerName, "The maximum number of ship " + ship + " has been reached on freq " + freq + ".");
      }
    }
  }
  
  /**
   * Gets a non restricted ship.
   * 
   * @return a non restricted ship.
   */
  
  private int getValidShip()	{
	  ArrayList<Integer> acceptable = new ArrayList<Integer>();
	  for ( int i=1 ; i<shipLimits.length ; i++)
		  if (shipLimits[i] == -1)
			  acceptable.add(new Integer(i));
	  return acceptable.get( rand.nextInt(acceptable.size()) ).intValue();
  }
  
  /**
   * Returns the number of a ship type on a specific freq.
   * 
   * @param ship is the ship to be counted.
   * @param freq is the freq to check.
   * @return the number of specified ships on the given freq.
   */
  
  private int getShipTypeOnFreq(int ship, int freq)
  {
    int shipCount = 0;
    Player player;

    Iterator<Player> iterator = m_botAction.getPlayerIterator();
    while(iterator.hasNext())
    {
      player = (Player) iterator.next();
      if(player.getShipType() == ship && player.getFrequency() == freq)
        shipCount++;
    }
    return shipCount;
  }
  
  /**
   * Handles freq ship changes.
   */
  
  public void handleEvent(FrequencyChange event)
  {
    int playerID = event.getPlayerID();
    String playerName = m_botAction.getPlayerName(playerID);
    checkLimit(playerName);
  }
  
  /**
   * Handles ship changes.
   */

  public void handleEvent(FrequencyShipChange event)
  {
    int playerID = event.getPlayerID();
    String playerName = m_botAction.getPlayerName(playerID);
    checkLimit(playerName);
  }
  
  /**
   * Handles all messages.
   */
  
  public void handleEvent(Message event)
  {
    int senderID = event.getPlayerID();
    String sender = m_botAction.getPlayerName(senderID);
    String message = event.getMessage();

    if(event.getMessageType() == Message.PRIVATE_MESSAGE && m_opList.isER(sender))
      handleCommand(sender, message);
  }
  
  /**
   * Handles all ER+ commands
   * 
   * @param sender is the user of the bot.
   * @param message is the command.
   */

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
  
  /**
   * Returns the help messages
   */
  
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
}

