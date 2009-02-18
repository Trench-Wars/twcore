/**
 * Prize module for TWBot.
 *
 * This module allows the user 4 methods of prizing:
 * 1) prizing everyone
 * 2) prizing specific freqs
 * 3) prizing specific ships
 * 4) prizing specific people
 *
 * In each case, a timer may be added to prize continuously.
 *
 * The syntax for the prize module is as follows:
 *
 * You are allowed to spell out the names of the prizes.  The module will run
 * a fuzzy name compare on the string, so partial prize names will work.  If the
 * prize name is more than 1 word long then colons must be used otherwise spaces
 * will be accepted.  When prizing individual players, colons must be used as
 * delimiters.  Prizing can now occur in fractions of seconds ie. 2.5 seconds.
 * You are able to send out prizes all the way down to 0.1 seconds apart.
 *
 * Here is the help menu:
 * !Prize <type>                                  -- Grants an individual prize to everyone in the arena
 * !Prize <type>:<interval>                       -- Grants a prize to everyone every <interval> seconds
 * !PrizeAll <type>                               -- Grants an individual prize to everyone in the arena
 * !PrizeAll <type>:<interval>                    -- Grants a prize to everyone every <interval> seconds
 * !PrizeFreq <type>:<freq>                       -- Grants a prize to freq <freq>
 * !PrizeFreq <type>:<freq>:<interval>            -- Grants a prize to freq <freq> every <interval> seconds
 * !PrizeShip <type>:<ship>                       -- Grants a prize to ship <ship>
 * !PrizeShip <type>:<ship>:<interval>            -- Grants a prize to ship <ship> every <interval> seconds
 * !PrizePlayer <type>:<name>                     -- Grants a prize to the player <name>
 * !PrizePlayer <type>:<name1>:<interval>         -- Grants a prize to player <name> every <interval> seconds
 * !PrizeDel <PrizeNumber>                        -- Deletes a specific prize from the list.
 * !PrizesOff                                     -- Clears all prizes.
 * !ListPrizeNames                                -- Lists all the types of prizes
 *
 * Author: Cpt.Guano!
 * April 29 2003
 *
 * Updates:
 * May 01 2003 - Added support for !super and !ufo commands.  Available to smods+, this
 *               will give the sender *super and *ufo.
 * May 04 2003 - Fixed some typos in the error messages.
 * Jun 20 2003 - Fixed the cancel and doPrizesOff methods.
 */

package twcore.bots.multibot.util;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.TurretEvent;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

public class utilprizes extends MultiUtil
{
  public static final String[] PRIZE_NAMES = {
      "Recharge", "Energy", "Rotation", "Stealth",
      "Cloak", "XRadar", "Warp", "Guns", "Bombs",
      "Bounce", "Thruster", "Top Speed", "Full Charge",
      "Engine Shutdown", "MultiFire", "Proximity",
      "Super", "Shields", "Shrapnel", "AntiWarp",
      "Repel", "Burst", "Decoy", "Thor",
      "Multiprize", "Brick", "Rocket", "Portal"
  };

  private static final int[] RESTRICTED_PRIZES = { Tools.Prize.RECHARGE,
                                                   Tools.Prize.ENERGY,
                                                   Tools.Prize.ROTATION,
                                                   Tools.Prize.BOUNCING_BULLETS,
                                                   Tools.Prize.THRUST,
                                                   Tools.Prize.TOPSPEED,
                                                   Tools.Prize.PROXIMITY };

  public double MIN_INTERVAL = 1;
  public int MIN_FREQ = 0;
  public int MAX_FREQ = 9999;
  public int MIN_PRIZE = 1;
  public int MAX_PRIZE = 28;
  public int MAX_SHIP = 8;
  public int MIN_SHIP = 1;


  public boolean[] turret;			//turret variables
  public int atchpz = 13;			//attach prize
  public int detchpz = 13;			//detach prize

  private Vector<TimerTask> timerTasks;

  /**
   * Creates a new twbotprizes instance.
   */

  public void init()
  {
    timerTasks = new Vector<TimerTask>();
    turret = new boolean[3];
    for (int i=0;i<3;i++)		//All turret values initialized as no
  	  turret[i] = false;
  }

  /**
   * Requests turret event
   */

  public void requestEvents( ModuleEventRequester modEventReq ) {
	  modEventReq.request(this, EventRequester.TURRET_EVENT );
  }

  /**
   * handles turret events
   */

  public void handleEvent(TurretEvent event)
	{
		if (turret[0] || turret[1]) // turret prizing on?
		{
			int tID = event.getAttacherID(); //turret ID
			int aID = event.getAttacheeID(); //anchor ID

			if(turret[0] && event.isAttaching())
			{
				if(turret[2])
					prizePlayer(tID,atchpz);	//Prize attaching player
				if (!turret[2])
					prizePlayer(aID,atchpz);	//Prize attached player
			}
			if(turret[1] && !event.isAttaching())
				prizePlayer(tID,detchpz);		//Prize detaching player
		}

	}

  /**
   * Handles all message events sent to the bot.  Only private messages sent by
   * <ER>s or higher will be processed.
   *
   * @param event is the message to process.
   */

  public void handleEvent(Message event)
  {
    if(event.getMessageType() == Message.PRIVATE_MESSAGE)
    {
      int playerID = event.getPlayerID();
      String playerName = m_botAction.getPlayerName(playerID);
      if(m_opList.isER(playerName))
        handleCommand(playerName, event.getMessage().toLowerCase());
      if(m_opList.isSmod(playerName))
        handleSecretCommand(playerName, event.getMessage().toLowerCase());
    }
  }

  /**
   * Implements a fuzzy search on an array of strings.
   *
   * @param stringArray is the array of strings to search.
   * @param target is the target string to find.
   * @return if the target is found, then the index is returned.  If not then -1
   *         is returned.
   */

  public int fuzzyStringArraySearch(String[] stringArray, String target)
  {
    int index;
    String check;
    String best = null;
    int bestIndex = -1;

    for(index = 0; index < stringArray.length; index++)
    {
      check = stringArray[index];
      if(check.toLowerCase().startsWith(target.toLowerCase()))
        if (best == null || best.toLowerCase().compareTo(check.toLowerCase()) > 0)
        {
          best = check;
          bestIndex = index;
        }
      if (check.equalsIgnoreCase(target)) return index;
    }
    return bestIndex;
  }

  /**
   * Gets the name of the prize from the prize number.
   *
   * @param prizeNumber is prize whose name is to be retrieved.
   * @return the name of the prize.  If the prize is negative then the string
   *         "Negative" will be prepended to the name.
   */

  public String getPrizeName(int prizeNumber)
  {
    String prizeName = "";

    if(prizeNumber < 0)
      prizeName = "Negative ";
    prizeName = prizeName + PRIZE_NAMES[Math.abs(prizeNumber) - 1];
    return prizeName;
  }

  /**
   * Gets the argument tokens from a string.  If there are no colons in the
   * string then the delimeter will default to space.
   *
   * @param string is the string to tokenize.
   * @return a tokenizer separating the arguments is returned.
   */

  public StringTokenizer getArgTokens(String string)
  {
    if(string.indexOf((int) ':') != -1)
      return new StringTokenizer(string, ":");
    return new StringTokenizer(string);
  }

  /**
   * Gets the prize number from a string.
   *
   * @Param prizeString is the string to process.
   * @throws IllegalArgumentException if the prize number is invalid.
   */
  public int getPrizeNumber(String prizeString, boolean override)
  {
    int negativePrizing = 1;

    if(prizeString.startsWith("-"))
    {
      negativePrizing = -1;
      prizeString = prizeString.substring(1).trim();
    }

    int prizeNumber = fuzzyStringArraySearch(PRIZE_NAMES, prizeString) + 1;

    if( prizeNumber != 0 ) {
      if( !override && isRestricted(prizeNumber) )
        throw new IllegalArgumentException("Sorry, that prize is restricted (known to cause disconnections), and can't be used.");
      return prizeNumber * negativePrizing;
    }

    prizeNumber = Integer.parseInt(prizeString);
    if( Math.abs(prizeNumber) > MAX_PRIZE || Math.abs(prizeNumber) < MIN_PRIZE )
      throw new IllegalArgumentException("That prize does not exist.");
    if( !override && isRestricted(prizeNumber) )
      throw new IllegalArgumentException("Sorry, that prize is restricted (known to cause disconnections), and can't be used.");
    return prizeNumber * negativePrizing;
  }

  /**
   * Gets the interval between prizing from a string in seconds.
   *
   * @Param intervalString is the string to process.
   * @throws IllegalArgumentException if the interval is smaller than the
   *         minimum interval (MIN_INTERVAL) or is equal or less than 0.
   */

  public double getInterval(String intervalString)
  {
    double interval = Double.parseDouble(intervalString);
    if(interval <= 0)
      throw new IllegalArgumentException("Invalid interval between prizes.");
    if(interval < MIN_INTERVAL)
      throw new IllegalArgumentException("Interval between prizes is too small.");
    return interval;
  }

  /**
   * Gets the freq to prize from a string.
   *
   * @Param freqString is the string to process.
   * @throws IllegalArgumentException if the freq is smaller than 0 or greater
   *         than 9999.
   */

  public int getFreq(String freqString)
  {
    int freq = Integer.parseInt(freqString);
    if(freq < MIN_FREQ || freq > MAX_FREQ)
      throw new IllegalArgumentException("Invalid freq to prize.");
    return freq;
  }

  /**
   * Gets the ship type to prize from a string.
   *
   * @Param shipString is the string to process.
   * @throws IllegalArgumentException if the ship is less than 1 or greater than
   *         8.
   */

  public int getShip(String shipString)
  {
    int ship = Integer.parseInt(shipString);
    if(ship < MIN_SHIP || ship > MAX_SHIP)
      throw new IllegalArgumentException("Invalid ship type.");
    return ship;
  }

  /**
   * Gets the player name from a string.
   *
   * @Param nameString is the string to process.
   * @throws IllegalArgumentException if player name is not found.
   */

  public String getPlayerName(String nameString)
  {
    String playerName = m_botAction.getFuzzyPlayerName(nameString);
    if(playerName == null)
      throw new IllegalArgumentException("Player not found in arena.");
    return playerName;
  }

  /**
   * Checks to see if a given prize number is restricted from use.
   * @param prizeNum Prize to check
   * @return True if prize is not allowed
   */
  public boolean isRestricted(int prizeNum) {
	  for(int i = 0; i < RESTRICTED_PRIZES.length; i++ )
		  if( RESTRICTED_PRIZES[i] == prizeNum )
			  return true;
	  return false;
  }

  /**
   * Adds a PrizeTask to the list of timer tasks.
   *
   * @Param sender is the user of the bot
   * @param prizeTask is the PrizeTask to add
   * @interval is the interval between prizes in seconds.
   */

  public void addPrizeTask(String sender, PrizeTask prizeTask, double interval)
  {
    timerTasks.add(prizeTask);
    m_botAction.scheduleTaskAtFixedRate(prizeTask, 0, (int) (interval * 1000));
    m_botAction.sendSmartPrivateMessage(sender, prizeTask.toString());
  }

  /**
   * Prizes all players in the arena.  There must be either one or two arguments
   * in the argString and they must be separated by either a space or a colon.
   * The arguments are as follows:
   * Argument 1: The prize number.
   * Argument 2: The interval between prizing.
   *
   * @Param sender is the user of the bot
   * @param argString is the string containing the arguments for the command.
   */

  public void doPrizeAll(String sender, String argString)
  {
    StringTokenizer argTokens = getArgTokens(argString);
    int numArgs = argTokens.countTokens();
    int prizeNumber;
    double interval;

    try
    {
      switch(numArgs)
      {
        case 1:
          prizeNumber = getPrizeNumber(argTokens.nextToken(), m_opList.isHighmod(sender));
          m_botAction.prizeAll(prizeNumber);
          m_botAction.sendSmartPrivateMessage(sender, "Prize: " + getPrizeName(prizeNumber) + " sent to all players.");
          break;
        case 2:
          prizeNumber = getPrizeNumber(argTokens.nextToken(), m_opList.isHighmod(sender));
          interval = getInterval(argTokens.nextToken());
          PrizeTask prizeTask = new PrizeTask(prizeNumber, PrizeTask.ALL_STATE, 0, interval);
          addPrizeTask(sender, prizeTask, interval);
          break;
        default:
          m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizeAll <type>:<interval>");
      }
    }
    catch(NumberFormatException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizeAll <type>:<interval>");
    }
    catch(Exception e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }

  /**
   * Prizes all players on a specific freq.  There must be either two or three
   * arguments in the argString and they must be separated by either a space or
   * a colon.
   *
   * The arguments are as follows:
   * Argument 1: The prize number.
   * Argument 2: The freq to prize to.
   * Argument 3: The interval between prizes.
   *
   * @Param sender is the user of the bot
   * @param argString is the string containing the arguments for the command.
   */

  public void doPrizeFreq(String sender, String argString)
  {
    StringTokenizer argTokens = getArgTokens(argString);
    int numArgs = argTokens.countTokens();
    int prizeNumber;
    int freq;
    double interval;

    try
    {
      switch(numArgs)
      {
        case 2:
          prizeNumber = getPrizeNumber(argTokens.nextToken(), m_opList.isHighmod(sender));
          freq = getFreq(argTokens.nextToken());
          prizeFreq(freq, prizeNumber);
          m_botAction.sendSmartPrivateMessage(sender, "Prize: " + getPrizeName(prizeNumber) + " sent to freq " + freq);
          break;
        case 3:
          prizeNumber = getPrizeNumber(argTokens.nextToken(), m_opList.isHighmod(sender));
          freq = getFreq(argTokens.nextToken());
          interval = getInterval(argTokens.nextToken());
          PrizeTask prizeTask = new PrizeTask(prizeNumber, PrizeTask.FREQ_STATE, freq, interval);
          addPrizeTask(sender, prizeTask, interval);
          break;
        default:
          m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizeFreq <type>:<freq>:<interval>");
      }
    }
    catch(NumberFormatException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizeFreq <type>:<freq>:<interval>");
    }
    catch(Exception e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }

  /**
   * Prizes all the ships of a specific type.  There must be either two or three
   * arguments in the argString and they must be separated by either a space or
   * a colon.
   *
   * The arguments are as follows:
   * Argument 1: The prize number.
   * Argument 2: The ship to prize to.
   * Argument 3: The interval between prizes.
   *
   * @Param sender is the user of the bot
   * @param argString is the string containing the arguments for the command.
   */

  public void doPrizeShip(String sender, String argString)
  {
    StringTokenizer argTokens = getArgTokens(argString);
    int numArgs = argTokens.countTokens();
    int prizeNumber;
    int ship;
    double interval;

    try
    {
      switch(numArgs)
      {
        case 2:
          prizeNumber = getPrizeNumber(argTokens.nextToken(), m_opList.isHighmod(sender));
          ship = getShip(argTokens.nextToken());
          prizeShip(ship, prizeNumber);
          m_botAction.sendSmartPrivateMessage(sender, "Prize: " + getPrizeName(prizeNumber) + " sent to ship " + ship);
          break;
        case 3:
          prizeNumber = getPrizeNumber(argTokens.nextToken(), m_opList.isHighmod(sender));
          ship = getShip(argTokens.nextToken());
          interval = getInterval(argTokens.nextToken());
          PrizeTask prizeTask = new PrizeTask(prizeNumber, PrizeTask.SHIP_STATE, ship, interval);
          addPrizeTask(sender, prizeTask, interval);
          break;
        default:
          m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizeShip <type>:<ship>:<interval>");
      }
    }
    catch(NumberFormatException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizeShip <type>:<ship>:<interval>");
    }
    catch(Exception e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }

  /**
   * Prizes a specific player.  There must be either two or three arguments in
   * the argString and they must be separated by a colon.
   *
   * The arguments are as follows:
   * Argument 1: The prize number.
   * Argument 2: The player to prize.
   * Argument 3: The interval between prizes.
   *
   * @Param sender is the user of the bot
   * @param argString is the string containing the arguments for the command.
   */

  public void doPrizePlayer(String sender, String argString)
  {
    StringTokenizer argTokens = new StringTokenizer(argString, ":");
    int numArgs = argTokens.countTokens();
    int prizeNumber;
    String playerName;
    double interval;

    try
    {
      switch(numArgs)
      {
        case 2:
          prizeNumber = getPrizeNumber(argTokens.nextToken(), m_opList.isHighmod(sender));
          playerName = getPlayerName(argTokens.nextToken());
          prizePlayer(playerName, prizeNumber);
          m_botAction.sendSmartPrivateMessage(sender, "Prize: " + getPrizeName(prizeNumber) + " sent to " + playerName);
          break;
        case 3:
          prizeNumber = getPrizeNumber(argTokens.nextToken(), m_opList.isHighmod(sender));
          playerName = getPlayerName(argTokens.nextToken());
          interval = getInterval(argTokens.nextToken());
          PrizeTask prizeTask = new PrizeTask(prizeNumber, PrizeTask.PLAYER_STATE, m_botAction.getPlayerID(playerName), interval);
          addPrizeTask(sender, prizeTask, interval);
          break;
        default:
          m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizePlayer <type>:<name>:<interval>");
      }
    }
    catch(NumberFormatException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizePlayer <type>:<name>:<interval>");
    }
    catch(Exception e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }

  /** prizes the turret or anchor on attach pending wither the
   * second argument is >=1 <0 (turret) or 0 (anchor);
   * @param sender is the user of the bot
   * @param argString is the string containing the arguments of the command.
   */

  public void doPrizeAttach(String sender, String argString)
  {
    StringTokenizer argTokens = new StringTokenizer(argString, ":");
    String var;

    try
    {
    	atchpz = getPrizeNumber(argTokens.nextToken(), m_opList.isHighmod(sender));
      	var = argTokens.nextToken();
    	if (var.equals("y"))
    	{
    		turret[2] = false;
    		m_botAction.sendSmartPrivateMessage(sender,"Prizing attached players with prize: " + getPrizeName(atchpz));
    	}
    	else if (var.equals("x"))
    	{
    		turret[2] = true;
    		m_botAction.sendSmartPrivateMessage(sender,"Prizing attaching players with prize: " + getPrizeName(atchpz));
    	}
    	else
    		throw new IllegalArgumentException("Use X or Y only");
      	turret[0] = true;
    }
    catch(NumberFormatException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizeAtch <type>:<x or y>");
    }
    catch(Exception e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }

  /**
   * Prizes a detaching player
   * @param sender is the user of the bot
   * @param prizetype is a string containing the prize number.
   */

  public void doPrizeDetach(String sender, String prizetype)
  {
    try
    {
      detchpz = getPrizeNumber(prizetype, m_opList.isHighmod(sender));
      turret[1] = true;
      m_botAction.sendSmartPrivateMessage(sender,"Prizing detaching players with prize: " + getPrizeName(detchpz));
    }
    catch(NumberFormatException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizeDetch <type>");
    }
    catch(Exception e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }

  /**
   * Gets the index of the prize in the timerTask list.
   *
   * @param prizeTaskString is the String of the task to get.
   * @return the index of the task in the timerTask list is returned.
   * @throws IllegalArgumentException if the prize number isnt found in the
   *         timerTask list.
   */

  public int getPrizeIndex(String prizeTaskString)
  {
    int prizeIndex = Integer.parseInt(prizeTaskString);
    if(prizeIndex >= timerTasks.size() || prizeIndex < 0)
      throw new IllegalArgumentException("Prize number does not exist.");
    return prizeIndex;
  }

  /**
   * Deletes a prize from the timerTask list.
   *
   * @param sender is the person using the bot.
   * @argString is the string containing the arguments for the command.
   */

  public void doPrizeDel(String sender, String argString)
  {
    StringTokenizer argTokens = new StringTokenizer(argString);
    if(argTokens.countTokens() != 1)
      m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizeDel <PrizeNumber>");
    else
    {
      try
      {
        int prizeIndex = getPrizeIndex(argTokens.nextToken());
        PrizeTask prizeTask = (PrizeTask) timerTasks.remove(prizeIndex);
        m_botAction.cancelTask(prizeTask);
        m_botAction.sendSmartPrivateMessage(sender, "\'" + prizeTask.toString() + "\' was removed");
      }
      catch(NumberFormatException e)
      {
        m_botAction.sendSmartPrivateMessage(sender, "Please use the following syntax: !PrizeDel <PrizeNumber>");
      }
      catch(Exception e)
      {
        m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
      }
    }
  }

  /**
   * Cancels all of the prizes.
   *
   * @param sender is the person using the bot.
   */

  public void doPrizesOff(String sender)
  {
	  if(timerTasks.size() == 0 && !(turret[0]==true || turret[1]==true)) //turret prizes on?
      m_botAction.sendSmartPrivateMessage(sender, "No prizes to clear.");
    else
    {
      cancel();
      for (int i=0;i<3;i++) //Reset'em
  		turret[i] = false;
      timerTasks.clear();
      m_botAction.sendSmartPrivateMessage(sender, "Prizes cleared.");
    }
  }

  /**
   * Lists all of the prizes.
   *
   * @param sender is the person using the bot.
   */

  public void doPrizeList(String sender)
  {
    PrizeTask prizeTask;

    if(timerTasks.isEmpty())
      m_botAction.sendSmartPrivateMessage(sender, "No prizes registered.");
    for(int index = 0; index < timerTasks.size(); index++)
    {
      prizeTask = (PrizeTask) timerTasks.get(index);
      m_botAction.sendSmartPrivateMessage(sender, "Prize " + index + ": " + prizeTask.toString());
    }
  }

  /**
   * Grants *super to the person the sender.
   *
   * @param sender is the person using the bot.
   */

  public void doSuper(String sender)
  {
    m_botAction.sendUnfilteredPrivateMessage(sender, "*super");
    m_botAction.sendSmartPrivateMessage(sender, "TIME TO KILL!!!", 13);
  }

  /**
   * Grants *ufo to the person the sender.
   *
   * @param sender is the person using the bot.
   */

  public void doUfo(String sender)
  {
    Player player = m_botAction.getPlayer(sender);
    if(!player.isPlaying())
      m_botAction.sendSmartPrivateMessage(sender, "You must be in the game to activate ufo.");
    else
    {
      m_botAction.sendUnfilteredPrivateMessage(sender, "*ufo");
      m_botAction.sendSmartPrivateMessage(sender, "Sneaky!", 19);
    }
  }

  /**
   * Lists all of the possible names and numbers of the prizes.
   *
   * @param sender is the person using the bot.
   */

  public void doListPrizeNames(String sender)
  {
    String[] prizeNames = {
        "1) (Recharge)        8) (Guns)           15) MultiFire        22) Burst",
        "2) (Energy)          9) (Bombs)          16) (Proximity)      23) Decoy",
        "3) (Rotation)       10) (Bounce)         17) Super            24) Thor",
        "4) Stealth          11) (Thruster)       18) Shields          25) MultiPrize",
        "5) Cloak            12) (Top Speed)      19) Shrapnel         26) Brick",
        "6) XRadar           13) Full Charge      20) AntiWarp         27) Rocket",
        "7) Warp             14) Engine Shutdown  21) Repel            28) Portal",
        "  (Prizes in parentheses are restricted, being known to cause problems.)"
    };

    m_botAction.smartPrivateMessageSpam(sender, prizeNames);
  }

  /**
   * Handles a command sent to the bot.
   *
   * @param sender is the person using the bot.
   * @param message is the message that is sent.
   */

  public void handleCommand(String sender, String message)
  {
    if(message.startsWith("!prize "))
      doPrizeAll(sender, message.substring(7).trim());
    if(message.startsWith("!prizeall "))
      doPrizeAll(sender, message.substring(10).trim());
    if(message.startsWith("!prizefreq "))
      doPrizeFreq(sender, message.substring(11).trim());
    if(message.startsWith("!prizeship "))
      doPrizeShip(sender, message.substring(11).trim());
    if(message.startsWith("!prizeplayer "))
      doPrizePlayer(sender, message.substring(13).trim());
    if(message.startsWith("!prizedel "))
      doPrizeDel(sender, message.substring(10).trim());
    if(message.startsWith("!prizeatch "))
      doPrizeAttach(sender, message.substring(11).trim());
    if(message.startsWith("!prizedetch "))
      doPrizeDetach(sender, message.substring(12).trim());
    if(message.equals("!prizesoff"))
      doPrizesOff(sender);
    if(message.equals("!prizelist"))
      doPrizeList(sender);
    if(message.equals("!listprizenames"))
      doListPrizeNames(sender);
  }

  /**
   * Handles a command sent to the bot from an Smod +.
   *
   * @param sender is the person using the bot.
   * @param message is the message that is sent.
   */

  public void handleSecretCommand(String sender, String message)
  {
    if(message.equals("!super"))
      doSuper(sender);
    if(message.equals("!ufo"))
      doUfo(sender);
  }

  /**
   * Returns an array of strings containing the help message for the bot.
   */

  public String[] getHelpMessages()
  {
      String[] message = {
          "!Prize <type>                             -- Grants an individual prize to everyone in the arena",
          "!Prize <type>:<interval>                  -- Grants a prize to everyone every <interval> seconds",
          "!PrizeAll <type>                          -- Grants an individual prize to everyone in the arena",
          "!PrizeAll <type>:<interval>               -- Grants a prize to everyone every <interval> seconds",
          "!PrizeAtch <type>:<x/y>                   -- Grants a prize on all attaches to the turret(x) or anchor(y)",
          "!PrizeDetch <type>                        -- Grants a prize on all detaches to the turret",
          "!PrizeFreq <type>:<freq>                  -- Grants a prize to freq <freq>",
          "!PrizeFreq <type>:<freq>:<interval>       -- Grants a prize to freq <freq> every <interval> seconds",
          "!PrizeShip <type>:<ship>                  -- Grants a prize to ship <ship>",
          "!PrizeShip <type>:<ship>:<interval>       -- Grants a prize to ship <ship> every <interval> seconds",
          "!PrizePlayer <type>:<name>                -- Grants a prize to the player <name>",
          "!PrizePlayer <type>:<name1>:<interval>    -- Grants a prize to player <name> every <interval> seconds",
          "!PrizeDel <PrizeNumber>                   -- Deletes a specific prize from the list.",
          "!PrizesOff                                -- Clears all prizes.",
          "!ListPrizeNames                           -- Lists all the types of prizes",
          "Note: some prizes that cause problems are restricted.  HighMod+ can override this."
      };
      return message;
  }

  /**
   * Cancels all of the timerTasks.
   */

  public void cancel()
  {
    PrizeTask prizeTask;

    for(int index = 0; index < timerTasks.size(); index++)
    {
      prizeTask = (PrizeTask) timerTasks.get(index);
      m_botAction.cancelTask(prizeTask);
    }
  }

  /**
   * Prizes a specific player.
   *
   * @param name is the name of the player to prize.
   * @prizeNum is the prize number to prize.
   */

  public void prizePlayer(String name, int prizeNum)
  {
    m_botAction.sendUnfilteredPrivateMessage(name, "*prize #" + prizeNum);
  }

  /**
   * Prizes a specific player.
   *
   * @param playerID is the name of the player to prize.
   * @prizeNum is the prize number to prize.
   */

  public void prizePlayer(int playerID, int prizeNum)
  {
    m_botAction.sendUnfilteredPrivateMessage(playerID, "*prize #" + prizeNum);
  }

  /**
   * Prizes a specific freq.
   *
   * @param freqID is the freq to prize.
   * @prizeNum is the prize number to prize.
   */

  public void prizeFreq(int freqID, int prizeNum)
  {
    try
    {
      Iterator<Player> iterator = m_botAction.getPlayerIterator();
      Player player;
      while(iterator.hasNext())
      {
        player = iterator.next();
        if(player.getFrequency() == freqID)
          prizePlayer(player.getPlayerName(), prizeNum );
      }
    }
    catch(Exception e)
    {
    }
  }

  /**
   * Prizes a specific ship type.
   *
   * @param ship is the ship type to prize
   * @prizeNum is the prize number to prize.
   */

  public void prizeShip(int ship, int prizeNum)
  {
    try
    {
      Iterator<Player> iterator = m_botAction.getPlayerIterator();
      Player player;
      while(iterator.hasNext())
      {
        player = iterator.next();
        if(player.getShipType() == ship)
          prizePlayer(player.getPlayerName(), prizeNum);
      }
    }
    catch(Exception e)
    {
    }
  }

  /**
   * This class is responsible for repeated prizing.  It currently has 4
   * different states, Prize all, Prize freq, Prize ship, and Prize player.
   * The variable thingToPrize is the thing that the PrizeTask is set to prize
   * (freq, ship, playerID).
   */

  private class PrizeTask extends TimerTask
  {
    public static final int ALL_STATE = 0;
    public static final int FREQ_STATE = 1;
    public static final int SHIP_STATE = 2;
    public static final int PLAYER_STATE = 3;

    private int prizeNumber;
    private int prizeState;
    private int thingToPrize;
    private double interval;

    /**
     * Creates a new PrizeTask instance.
     *
     * @param prizeNumber is the prize number to prize.
     * @param prizeState is the state of the prizing.  Can be Prize all, Prize
     *        freq, Prize ship, or Prize player.
     * @param thingToPrize is what is to be prized (freq, ship or playerID)
     * @param interval is the time at which the prizing occurs.
     */

    public PrizeTask(int prizeNumber, int prizeState, int thingToPrize, double interval)
    {
      if(prizeState > PLAYER_STATE || prizeState < ALL_STATE)
        throw new IllegalArgumentException("Invalid prize state.");

      this.prizeNumber = prizeNumber;
      this.prizeState = prizeState;
      this.thingToPrize = thingToPrize;
      this.interval = interval;
    }

    /**
     * Creates a string representation of the PrizeTask instance.
     *
     * @return a string describing the prizing.
     */

    public String toString()
    {
      switch(prizeState)
      {
        case ALL_STATE:
          return getPrizeName(prizeNumber) + " sent to all players every " + interval + " seconds.";
        case FREQ_STATE:
          return getPrizeName(prizeNumber) + " sent to freq " + thingToPrize + " every " + interval + " seconds.";
        case SHIP_STATE:
          return getPrizeName(prizeNumber) + " sent to ship " + thingToPrize + " every " + interval + " seconds.";
        case PLAYER_STATE:
          return getPrizeName(prizeNumber) + " sent to " + m_botAction.getPlayerName(thingToPrize) + " every " + interval + " seconds.";
      }
      return "Error in prize task.";
    }

    /**
     * Handles the prizing.
     */

    public void run()
    {
      switch(prizeState)
      {
        case ALL_STATE:
          m_botAction.prizeAll(prizeNumber);
          break;
        case FREQ_STATE:
          prizeFreq(thingToPrize, prizeNumber);
          break;
        case SHIP_STATE:
          prizeShip(thingToPrize, prizeNumber);
          break;
        case PLAYER_STATE:
          prizePlayer(thingToPrize, prizeNumber);
          break;
      }
    }
  }
}