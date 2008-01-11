/**
 * Warp module for TWBot.
 *
 * This module allows the user 4 methods of warping:
 *
 * 1) Warping everyone to a location.
 * 2) Warping a freq to a location.
 * 3) Warping a ship type to a location.
 * 4) Setup warping for events.
 *
 * The syntax for the spec module is as follows:
 *
 * This module acts much like one would imagine, however there is now the option
 * to warp within a radius.  This is done by adding a radius to the end of the
 * warp command.  Please be advised that if there are any rocks or walls in the
 * warping area, some ships might end up back in the spawn area.  I will remedy
 * this once the bot is able to read maps.
 *
 * The setup warping feature is done as follows:
 * !Setupwarp <Argument>
 *
 * The arguments can be seen when you type !Setupwarplist.
 * The warp coordinates are stored in the Trench Wars Database and must be added
 * before you use them.  If the arena has not been entered into the database,
 * please tell me in game and i will stick it in.
 *
 * Here is the help menu:
 * !Warpto <X>:<Y>:<Radius>                  -- Warps everyone to <X>, <Y> within a distance of <Radius>."
 * !WarpFreq <Freq>:<X>:<Y>:<Radius>         -- Warps freq <Freq> to <X>, <Y> within a distance of <Radius>."
 * !WarpShip <Ship>:<X>:<Y>:<Radius>         -- Warps ship <Ship> to <X>, <Y> within a distance of <Radius>."
 * !SetupWarp <Argument>                     -- Performs the setup warp for this arena based on the <Argument>."
 * !SetupWarpList                            -- Displays the setup warp information."
 * !Where                                    -- Shows your current coords."
 *
 * NOTE: The !warpto command is removed from the standard module so please !load
 * warp first.
 *
 * Author: Cpt.Guano!
 * July 06, 2003
 * Added Spawn options January 01, 2008 -milosh
 */

package twcore.bots.multibot.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

public class utilwarp extends MultiUtil
{
  public static final int WARP_ALL = 1;
  public static final int WARP_FREQ = 2;
  public static final int WARP_SHIP = 3;
  public static final int WARP_FIRST_FREQ = 4;

  public static final int MAX_FREQ = 9999;
  public static final int MIN_FREQ = 0;
  public static final int MAX_SHIP = 8;
  public static final int MIN_SHIP = 1;

  public static final int MIN_COORD = 1;
  public static final int MAX_COORD = 1022;
  
  public static final int SPAWN_TIME = 5005;

  public static final String COLON = ":";
  
  private Vector <SpawnTask> spawnTasks;
  
  private static final String database = "website";

  public void init()
  {
	  spawnTasks = new Vector<SpawnTask>();
  }

  public void requestEvents( ModuleEventRequester modEventReq ) {
  }
  
  public String[] getHelpMessages()
  {
    String[] message =
    {
          "!Warpto <X>:<Y>:<Radius>                  -- Warps everyone to <X>, <Y> within a distance of <Radius>.",
          "!WarpFreq <Freq>:<X>:<Y>:<Radius>         -- Warps freq <Freq> to <X>, <Y> within a distance of <Radius>.",
          "!WarpShip <Ship>:<X>:<Y>:<Radius>         -- Warps ship <Ship> to <X>, <Y> within a distance of <Radius>.",
          "!Spawn <X>:<Y>:<Radius>                   -- Spawns all players at <X>, <Y> within a distance of <Radius>",
          "!SpawnFreq <Freq>:<X>:<Y>:<Radius>        -- Spawns frequency <Freq> at <X>, <Y> within a distance of <Radius>",
          "!SpawnShip <Ship>:<X>:<Y>:<Radius>        -- Spawns ship <Ship> at <X>, <Y> within a distance of <Radius>",
          "!SpawnList                                -- Shows a list of all spawn tasks.",
          "!SpawnDel <Index>                         -- Deletes the spawn task at index <Index>",
          "!SpawnOff                                 -- Removes all spawn tasks.",
          "!SetupWarp <Argument>                     -- Performs the setup warp for this arena based on the <Argument>.",
          "!SetupWarpList                            -- Displays the setup warp information.",
          "!Whereami                                 -- Shows your current coords."
          };
    return message;
  }

  public void doWarpToCmd(String sender, String argString)
  {
    StringTokenizer argTokens = getArgTokens(argString);
    int numTokens = argTokens.countTokens();

    if(numTokens < 2 || numTokens > 3)
      throw new IllegalArgumentException("Please use the following format: !WarpTo <X>:<Y>:<Radius>.");
    try
    {
      int xCoord = Integer.parseInt(argTokens.nextToken());
      int yCoord = Integer.parseInt(argTokens.nextToken());
      double radius = 0;
      if(numTokens == 3)
        radius = Double.parseDouble(argTokens.nextToken());
      doWarp(WARP_ALL, 0, xCoord, yCoord, radius, false);
      m_botAction.sendSmartPrivateMessage(sender, getWarpString(WARP_ALL, 0, xCoord, yCoord, radius));
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Please use the following format: !WarpTo <X>:<Y>:<Radius>.");
    }
  }

  public void doWarpFreqCmd(String sender, String argString)
  {
    StringTokenizer argTokens = getArgTokens(argString);
    int numTokens = argTokens.countTokens();

    if(numTokens < 3 || numTokens > 4)
      throw new IllegalArgumentException("Please use the following format: !WarpFreq <Freq>:<X>:<Y>:<Radius>.");
    try
    {
      int freq = Integer.parseInt(argTokens.nextToken());
      int xCoord = Integer.parseInt(argTokens.nextToken());
      int yCoord = Integer.parseInt(argTokens.nextToken());
      double radius = 0;
      if(numTokens == 4)
        radius = Double.parseDouble(argTokens.nextToken());
      doWarp(WARP_FREQ, freq, xCoord, yCoord, radius, false);
      m_botAction.sendSmartPrivateMessage(sender, getWarpString(WARP_FREQ, freq, xCoord, yCoord, radius));
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Please use the following format: !WarpFreq <Freq>:<X>:<Y>:<Radius>.");
    }
  }

  public void doWarpShipCmd(String sender, String argString)
  {
    StringTokenizer argTokens = getArgTokens(argString);
    int numTokens = argTokens.countTokens();

    if(numTokens < 3 || numTokens > 4)
      throw new IllegalArgumentException("Please use the following format: !WarpShip <Ship>:<X>:<Y>:<Radius>.");
    try
    {
      int ship = Integer.parseInt(argTokens.nextToken());
      int xCoord = Integer.parseInt(argTokens.nextToken());
      int yCoord = Integer.parseInt(argTokens.nextToken());
      double radius = 0;
      if(numTokens == 4)
        radius = Double.parseDouble(argTokens.nextToken());
      doWarp(WARP_SHIP, ship, xCoord, yCoord, radius, false);
      m_botAction.sendSmartPrivateMessage(sender, getWarpString(WARP_SHIP, ship, xCoord, yCoord, radius));
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Please use the following format: !WarpShip <Ship>:<X>:<Y>:<Radius>.");
    }
  }

  /**
   * This handles the player death event and spawns if needed.
   *
   * @param event is the player death event.
   */
  public void handleEvent(PlayerDeath event)
  {
      int playerID = event.getKilleeID();
      Player curPlayer = m_botAction.getPlayer(playerID);
      int freq = curPlayer.getFrequency();
      int ship = curPlayer.getShipType();
      SpawnTask spawnTask = getValidSpawnTask(freq, ship, playerID);

      if(spawnTask != null) {
    	  new SpawnTimer(m_botAction.getPlayerName(playerID), spawnTask.getX(), spawnTask.getY(), spawnTask.getRadius(), SPAWN_TIME);
      }
  }

  /**
   * This private method adds a spawn task to the task list.
   *
   * @param spawnTask is the Spawn Task to be added.
   */
  private void addTask(SpawnTask spawnTask) {
      int replaceIndex = spawnTasks.indexOf(spawnTask);

      if(replaceIndex != -1)
          spawnTasks.remove(replaceIndex);
      spawnTasks.add(spawnTask);
      Collections.sort(spawnTasks, new SpawnTaskComparator());
  }
  
  /**
   * This method displays a list of all of the spawn tasks.
   *
   * @param sender is the person that messaged the bot.
   */
  public void doSpawnListCmd(String sender)
  {
      int numTasks = spawnTasks.size();
      SpawnTask spawnTask;

      if(numTasks == 0)
          m_botAction.sendSmartPrivateMessage(sender, "There are currently no spawn tasks.");
      else
          for(int index = 0; index < numTasks; index++)
          {
              spawnTask = spawnTasks.get(index);
              m_botAction.sendSmartPrivateMessage(sender, "Task " + index + ") " + spawnTask);
          }
  }
  
  /**
   * This method removes a spawn task.
   *
   * @param sender is the person that messaged the bot.
   * @param argString is the spawn task number that is to be removed.
   */
  public void doSpawnDelCmd(String sender, String argString)
  {
      StringTokenizer argTokens = getArgTokens(argString);

      if(argTokens.countTokens() != 1)
          throw new IllegalArgumentException("Please use the following format: !SpawnDel <Task Number>.");
      try
      {
          int taskNumber = Integer.parseInt(argTokens.nextToken());
          SpawnTask spawnTask = spawnTasks.get(taskNumber);

          spawnTasks.remove(taskNumber);
          m_botAction.sendArenaMessage("Removing Task: " + spawnTask.toString() + " -" + sender);
      }
      catch(NumberFormatException e)
      {
          throw new NumberFormatException("Please use the following format: !SpawnDel <Task Number>.");
      }
      catch(ArrayIndexOutOfBoundsException e)
      {
          throw new IllegalArgumentException("Invalid Spawn Task.");
      }
  }
  
  /**
   * This method clears all of the spawn tasks.
   */
  public void doSpawnOffCmd(String sender)
  {
      if(spawnTasks.isEmpty())
          throw new RuntimeException("No spawn tasks to clear.");
      spawnTasks.clear();
      m_botAction.sendArenaMessage("Removing all spawn tasks. -" + sender);
  }
  
  /**
   * This method spawns all players at a specified location.
   *
   * @param sender is the person operating the bot.
   * @param argString is the string of the arguments.
   */
  public void doSpawnCmd(String sender, String argString)
  {
      StringTokenizer argTokens = getArgTokens(argString);
      if(argTokens.countTokens() != 3)
          throw new IllegalArgumentException("Please use the following format: !Spawn <X>:<Y>:<Radius>.");          
      try
      {
          int x = Integer.parseInt(argTokens.nextToken());
          int y = Integer.parseInt(argTokens.nextToken());
          double radius = Double.parseDouble(argTokens.nextToken());
          SpawnTask spawnTask = new SpawnTask(x,y,radius);

          addTask(spawnTask);
          m_botAction.sendArenaMessage(spawnTask.toString() + " -" + sender);
      }
      catch(NumberFormatException e)
      {
          throw new NumberFormatException("Please use the following format: !Spawn <X>:<Y>:<Radius>.");
      }
  }

  /**
   * This method spawns all players on a specific frequency at a specified location.
   *
   * @param sender is the person operating the bot.
   * @param argString is the string of the arguments.
   */
  public void doSpawnFreqCmd(String sender, String argString)
  {
      StringTokenizer argTokens = getArgTokens(argString);

      if(argTokens.countTokens() != 4)
          throw new IllegalArgumentException("Please use the following format: !SpawnFreq <Freq>:<X>:<Y>:<Radius>.");
      try
      {
          int freq = Integer.parseInt(argTokens.nextToken());
          int x = Integer.parseInt(argTokens.nextToken());
          int y = Integer.parseInt(argTokens.nextToken());
          double radius = Double.parseDouble(argTokens.nextToken());
          SpawnTask spawnTask = new SpawnTask(freq, x, y, radius, SpawnTask.SPAWN_FREQ);

          addTask(spawnTask);
          m_botAction.sendArenaMessage(spawnTask.toString() + " -" + sender);
      }
      catch(NumberFormatException e)
      {
          throw new NumberFormatException("Please use the following format: !SpawnFreq <Freq>:<X>:<Y>:<Radius>.");
      }
  }

  /**
   * This method spawns all players in a specific ship at a specified location.
   *
   * @param sender is the person operating the bot.
   * @param argString is the string of the arguments.
   */
  public void doSpawnShipCmd(String sender, String argString)
  {
      StringTokenizer argTokens = getArgTokens(argString);

      if(argTokens.countTokens() != 4)
          throw new IllegalArgumentException("Please use the following format: !SpawnShip <Ship>:<X>:<Y>:<Radius>.");
      try
      {
          int ship = Integer.parseInt(argTokens.nextToken());
          int x = Integer.parseInt(argTokens.nextToken());
          int y = Integer.parseInt(argTokens.nextToken());
          double radius = Double.parseDouble(argTokens.nextToken());
          SpawnTask spawnTask = new SpawnTask(ship, x, y, radius, SpawnTask.SPAWN_SHIP);

          addTask(spawnTask);
          m_botAction.sendArenaMessage(spawnTask.toString() + " -" + sender);
      }
      catch(NumberFormatException e)
      {
          throw new NumberFormatException("Please use the following format: !SpawnShip <Ship>:<X>:<Y>:<Radius>.");
      }
  }

  /**
   * This method spawns a specific player at a specified location.
   *
   * @param sender is the person operating the bot.
   * @param argString is the string of the arguments.
   */
  public void doSpawnPlayerCmd(String sender, String argString)
  {
      StringTokenizer argTokens = getArgTokens(argString);

      if(argTokens.countTokens() != 4)
          throw new IllegalArgumentException("Please use the following format: !SpawnPlayer <Player>:<X>:<Y>:<Radius>.");
      try
      {
          String playerName = m_botAction.getFuzzyPlayerName(argTokens.nextToken());
          int playerID = m_botAction.getPlayerID(playerName);
          int x = Integer.parseInt(argTokens.nextToken());
          int y = Integer.parseInt(argTokens.nextToken());
          double radius = Double.parseDouble(argTokens.nextToken());
          SpawnTask spawnTask = new SpawnTask(playerID, x, y, radius, SpawnTask.SPAWN_PLAYER);

          if(playerName == null)
              throw new IllegalArgumentException("Player not found in the arena.");
//        check to see if the player is in spec...
          addTask(spawnTask);
          m_botAction.sendArenaMessage(spawnTask.toString() + " -" + sender);
      }
      catch(NumberFormatException e)
      {
          throw new NumberFormatException("Please use the following format: !SpawnPlayer <Player>:<X>:<Y>:<Radius>.");
      }
  }
  
  public void doSetupWarpListCmd(String sender)
  {
    String arenaName = m_botAction.getArenaName();

    try
    {
      ResultSet resultSet = m_botAction.SQLQuery(database,
      "SELECT SW.* "+
      "FROM tblArena A, tblSetupWarp SW "+
      "WHERE A.fnArenaID = SW.fnArenaID " +
      "AND A.fcArenaName = '" + arenaName + "'");

      int count = 0;
      while(resultSet.next())
      {
        String argument = resultSet.getString("fcArgument").trim();
        String description = resultSet.getString("fcDescription").trim();
        if(description.equals(""))
          description = "No Description.";

        m_botAction.sendSmartPrivateMessage(sender, "!SetupWarp " + padSpaces(argument, 31) + "-- " + description);
        count++;
      }
      m_botAction.SQLClose( resultSet );
      if(count == 0)
        m_botAction.sendSmartPrivateMessage(sender, "No setup warps are registered for this arena.");
    }

    catch(SQLException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, "ERROR: Cannot connect to database.");
    }
    catch(NullPointerException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, "ERROR: Cannot connect to database.");
    }
  }

  public void doSetupWarpCmd(String sender, String argument)
  {
    String arenaName = m_botAction.getArenaName();

    try
    {
      ResultSet resultSet = m_botAction.SQLQuery(database,
      "SELECT WP.* " +
      "FROM tblArena A, tblSetupWarp SW, tblWarpPoint WP " +
      "WHERE WP.fnSetupWarpID = SW.fnSetupWarpID " +
      "AND SW.fcArgument = '" + argument + "' " +
      "AND SW.fnArenaID = A.fnArenaID " +
      "AND A.fcArenaName = '" + arenaName + "'");

      int count = 0;
      while(resultSet.next())
      {
        int warpType = resultSet.getInt("fnWarpTypeID");
        int warpID = resultSet.getInt("fnWarpSpecifier");
        int xCoord = resultSet.getInt("fnXCoord");
        int yCoord = resultSet.getInt("fnYCoord");
        double radius = (double) resultSet.getInt("fnRadius");
        doWarp(warpType, warpID, xCoord, yCoord, radius, true);
        count++;
      }
      m_botAction.SQLClose( resultSet );
      if(count == 0)
        m_botAction.sendSmartPrivateMessage(sender, "Invalid argument.  Please use !SetupWarpList to see the setup warps available");
      else
        m_botAction.sendSmartPrivateMessage(sender, "Setup warps completed.");
    }
    catch(SQLException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, "ERROR: Cannot connect to database.");
    }
    catch(NullPointerException e)
    {
      m_botAction.sendSmartPrivateMessage(sender, "ERROR: Cannot connect to database.");
    }
  }

  /**
   * PMs sender's current coords.
   * @param sender Host who wants desperately to know own position
   */
  public void doWhereCmd(String sender) {
      Player p = m_botAction.getPlayer( sender );
      if( p != null ) {
          m_botAction.sendSmartPrivateMessage( sender, "You are at: (" + new Integer(p.getXLocation() / 16) + "," + new Integer(p.getYLocation() / 16) + ")" );
      }
  }

  public void doWarp(int warpType, int warpID, int xCoord, int yCoord, double radius, boolean resetGroup)
  {
    if(warpType < WARP_ALL || warpType > WARP_FIRST_FREQ)
      throw new IllegalArgumentException("ERROR: Unknown warp type.");
    if((warpID < MIN_FREQ || warpID > MAX_FREQ) && warpType == WARP_FREQ)
      throw new IllegalArgumentException("Invalid freq number.");
    if((warpID < MIN_SHIP || warpID > MAX_SHIP) && warpType == WARP_SHIP)
      throw new IllegalArgumentException("Invalid ship type.");
    if(!isValidCoord(xCoord, yCoord))
      throw new IllegalArgumentException("Coordinates are out of bounds.");
    if(radius < 0)
      throw new IllegalArgumentException("Invalid warp radius.");

    if(warpType == WARP_FIRST_FREQ)
    {
      Vector<Integer> freqNumbers = getFreqNumbers();

      if(warpID < freqNumbers.size())
      {
        Integer freq = freqNumbers.get(warpID);
        doWarpGroup(WARP_FIRST_FREQ, freq.intValue(), xCoord, yCoord, radius, resetGroup);
      }
    }
    else
      doWarpGroup(warpType, warpID, xCoord, yCoord, radius, resetGroup);
  }

  public void doWarpGroup(int warpType, int warpID, int xCoord, int yCoord, double radius, boolean resetGroup)
  {
    Iterator<Player> iterator = m_botAction.getPlayingPlayerIterator();
    Player player;

    while(iterator.hasNext())
    {
      player = iterator.next();
      if(isWarpable(player, warpType, warpID))
      {
        if(resetGroup)
          m_botAction.shipReset(player.getPlayerName());
        doRandomWarp(player.getPlayerName(), xCoord, yCoord, radius);
      }
    }
  }

  public void handleCommand(String sender, String message)
  {
    String command = message.toLowerCase();

    try
    {
      if(command.startsWith("!warpto "))
        doWarpToCmd(sender, message.substring(8));
      else if(command.startsWith("!warpfreq "))
        doWarpFreqCmd(sender, message.substring(10));
      else if(command.startsWith("!warpship "))
        doWarpShipCmd(sender, message.substring(10));
      else if(command.startsWith("!spawn "))
    	doSpawnCmd(sender, message.substring(7));
      else if(command.startsWith("!spawnfreq "))
        doSpawnFreqCmd(sender, message.substring(11));
      else if(command.startsWith("!spawnship "))
        doSpawnShipCmd(sender, message.substring(11));
      else if(command.startsWith("!spawnplayer "))
        doSpawnPlayerCmd(sender, message.substring(13));
      else if(command.equalsIgnoreCase("!spawnlist"))
    	doSpawnListCmd(sender);
      else if(command.startsWith("!spawndel "))
    	doSpawnDelCmd(sender, message.substring(10));
      else if(command.equalsIgnoreCase("!spawnoff"))
    	doSpawnOffCmd(sender);
      else if(command.equalsIgnoreCase("!setupwarp"))
        doSetupWarpCmd(sender, "");
      else if(command.startsWith("!setupwarp "))
        doSetupWarpCmd(sender, message.substring(11));
      else if(command.equalsIgnoreCase("!setupwarplist"))
        doSetupWarpListCmd(sender);
      else if(command.equalsIgnoreCase("!whereami"))
        doWhereCmd(sender);
    }
    catch(RuntimeException e){}
  }

  public void handleEvent(Message event)
  {
    int senderID = event.getPlayerID();
    String sender = m_botAction.getPlayerName(senderID);
    String message = event.getMessage().trim();

    if(m_opList.isER(sender))
      handleCommand(sender, message);
  }

  public void cancel()
  {
  }

  /**
   * Gets the argument tokens from a string.  If there are no colons in the
   * string then the delimeter will default to space.
   *
   * @param string is the string to tokenize.
   * @return a tokenizer separating the arguments is returned.
   */

  private StringTokenizer getArgTokens(String string)
  {
    if(string.indexOf((int) ':') != -1)
      return new StringTokenizer(string, ":");
    return new StringTokenizer(string);
  }

  private Vector<Integer> getFreqNumbers()
  {
    TreeSet<Integer> freqNumbers = new TreeSet<Integer>();
    Iterator<Player> iterator = m_botAction.getPlayingPlayerIterator();
    Player player;

    while(iterator.hasNext())
    {
      player = iterator.next();
      freqNumbers.add(new Integer(player.getFrequency()));
    }
    return new Vector<Integer>(freqNumbers);
  }

  private boolean isWarpable(Player player, int warpType, int warpID)
  {
    switch(warpType)
    {
      case WARP_ALL:
        return true;
      case WARP_FIRST_FREQ:
        return warpID == player.getFrequency();
      case WARP_FREQ:
        return warpID == player.getFrequency();
      case WARP_SHIP:
        return warpID == player.getShipType();
    }
    return false;
  }

  private void doRandomWarp(String playerName, int xCoord, int yCoord, double radius)
  {
    double randRadians;
    double randRadius;
    int xWarp = -1;
    int yWarp = -1;

    while(!isValidCoord(xWarp, yWarp))
    {
      randRadians = Math.random() * 2 * Math.PI;
      randRadius = Math.random() * radius;
      xWarp = calcXCoord(xCoord, randRadians, randRadius);
      yWarp = calcYCoord(yCoord, randRadians, randRadius);
    }
    m_botAction.warpTo(playerName, xWarp, yWarp);
  }

  private int calcXCoord(int xCoord, double randRadians, double randRadius)
  {
    return xCoord + (int) Math.round(randRadius * Math.sin(randRadians));
  }

  private int calcYCoord(int yCoord, double randRadians, double randRadius)
  {
    return yCoord + (int) Math.round(randRadius * Math.cos(randRadians));
  }

  private boolean isValidCoord(int xCoord, int yCoord)
  {
    return xCoord >= MIN_COORD && xCoord <= MAX_COORD &&
        yCoord >= MIN_COORD && yCoord <= MAX_COORD;
  }

  /**
   * This method returns a string representation of a warp.
   *
   * @param warpType is the type of warp.
   * @param warpID defines what gets warped.
   * @param xCoord is the x coord to warp to.
   * @param yCoord is the y coord to warp to.
   * @param radius is the radius within to warp to.
   * @return a string describing the warp is returned.
   */

  private String getWarpString(int warpType, int warpID, int xCoord, int yCoord, double radius)
  {
    StringBuffer warpString = new StringBuffer("Warped ");
    switch(warpType)
    {
      case WARP_ALL:
        warpString.append("all players");
        break;
      case WARP_FREQ:
        warpString.append("freq " + warpID);
        break;
      case WARP_SHIP:
        warpString.append("ship " + warpID);
        break;
      default:
        return "ERROR: Unknown Warp Type";
    }
    warpString.append(" to " + xCoord + ", " + yCoord);
    if(radius != 0)
      warpString.append(" with a radius of " + radius);
    return warpString.toString() + ".";
  }

  /**
   * This method makes a string a certain length, padding with spaces or cutting
   * the string if necessary.
   *
   * @param string is the string to pad.
   * @param length is the length that the string is supposed to be.
   * @return the modified string.
   */

  private String padSpaces(String string, int length)
  {
    if(string.length() > length)
      return string.substring(0, length);

    StringBuffer returnString = new StringBuffer(string);

    for(int index = 0; index < length - string.length(); index++)
      returnString.append(" ");
    return returnString.toString();
  }
  
  /**
   * This private method gets the first spawn task that affects a player with the
   * given attributes.
   *
   * @param freq is the freq of the player.
   * @param ship is the ship of the player.
   * @param playerID is the ID of the player.
   * @return The appropriate spawn task is returned.
   */
  private SpawnTask getValidSpawnTask(int freq, int ship, int playerID)
  {
      SpawnTask spawnTask;

      for(int index = 0; index < spawnTasks.size(); index++)
      {
          spawnTask = spawnTasks.get(index);
          if(spawnTask.isSameType(freq, ship, playerID))
              return spawnTask;
      }
      return null;
  }
  
  private class SpawnTask
  {
      public static final int SPAWN_ALL = 0;
      public static final int SPAWN_FREQ = 1;
      public static final int SPAWN_SHIP = 2;
      public static final int SPAWN_PLAYER = 3;

      public static final int MAX_FREQ = 9999;
      public static final int MIN_FREQ = 0;
      public static final int MAX_SHIP = 8;
      public static final int MIN_SHIP = 1;

      private int spawnID;
      private int spawnType;
      private int x;
      private int y;
      private double radius;

      /**
       * Creates a new SpawnTask that spawns all players at (x,y) in a radius of radius upon death.
       * @param x - The x location in tiles.
       * @param y - The y location in tiles.
       * @param radius - The radius in tiles.
       */
      public SpawnTask(int x, int y, double radius)
      {
          if(x <= 0 || y <= 0 || x > 1020 || y > 1020)
              throw new IllegalArgumentException("Invalid coordinates.");
          if(radius < 0)
        	  throw new IllegalArgumentException("Invalid radius.");
          this.x = x;
          this.y = y;
          this.radius = radius;
          this.spawnType = SPAWN_ALL;
      }

      /**
       * This constructor initializes a spawn task of variable type with the given
       * number of deaths.
       *
       * @param specID who is affected by the spawn task.  This could be the freq
       * ID, the ship type or the playerID.
       * @param x - The x location in tiles.
       * @param y - The y location in tiles.
       * @param radius - The radius in tiles.
       * @param specType this is the type of task and defines who the task
       * affects.
       */
      public SpawnTask(int spawnID, int x, int y, double radius, int spawnType)
      {
          if(spawnType < 0 || spawnType > 3)
              throw new IllegalArgumentException("ERROR: Unknown Spawn Type.");
          if((spawnID < MIN_FREQ || spawnID > MAX_FREQ) && spawnType == SPAWN_FREQ)
              throw new IllegalArgumentException("Invalid freq number.");
          if((spawnID < MIN_SHIP || spawnID > MAX_SHIP) && spawnType == SPAWN_SHIP)
              throw new IllegalArgumentException("Invalid ship type.");
          if(x <= 0 || y <= 0 || x > 1020 || y > 1020)
              throw new IllegalArgumentException("Invalid coordinates.");
          if(radius < 0)
        	  throw new IllegalArgumentException("Invalid radius.");

          this.spawnID = spawnID;
          this.x = x;
          this.y = y;
          this.radius = radius;
          this.spawnType = spawnType;
      }

      /**
       * This method gets the spawn type.
       * @return the spawn type is returned.
       */
      public int getSpawnType()
      {
          return spawnType;
      }

      /**
       * This method gets the x coordinate.
       * @return the x coordinate is returned.
       */
      public int getX()
      {
          return x;
      }
      
      /**
       * This method gets the y coordinate.
       * @return the y coordinate is returned.
       */
      public int getY()
      {
          return y;
      }
      
      /**
       * This method gets the radius size.
       * @return the radius is returned.
       */
      public double getRadius()
      {
          return radius;
      }

      /**
       * This method checks to see if a player with the given attributes will
       * be spawned by this particular spawn task.
       *
       * @param freq is the freq of the player.
       * @param ship is the ship of the player.
       * @playerID is the ID of the player.
       * @return True if the player is affected, false if not.
       */
      public boolean isSameType(int freq, int ship, int playerID)
      {
          switch(spawnType)
          {
          case SPAWN_ALL:
              return true;
          case SPAWN_FREQ:
              return freq == spawnID;
          case SPAWN_SHIP:
              return ship == spawnID;
          case SPAWN_PLAYER:
              return playerID == spawnID;
          }
          return false;
      }

      /**
       * This method returns a string representation of the spawn task.
       *
       * @return a string representation of the spawn task is returned.
       */
      public String toString()
      {
          String specTypeName;

          switch(spawnType)
          {
          case SPAWN_ALL:
              specTypeName = "all players";
              break;
          case SPAWN_FREQ:
              specTypeName = "freq " + spawnID;
              break;
          case SPAWN_SHIP:
              specTypeName = "ship " + spawnID;
              break;
          case SPAWN_PLAYER:
              specTypeName = "player " + m_botAction.getPlayerName(spawnID);
              break;
          default:
              specTypeName = "ERROR: Unknown Spec Type";
          }

          return "Spawning " + specTypeName + " at (" + x + "," + y + ") with a radius of " + radius + ".";
      }

      /**
       * This overrides the equals method so as to check the spawn types and the
       * spawn IDs.
       *
       * @param obj is the other spawn task to check.
       * @return True if the spawn tasks have the same specID and the same
       * specType.  False if not.
       */
      public boolean equals(Object obj)
      {
          SpawnTask spawnTask = (SpawnTask) obj;
          return spawnTask.spawnID == spawnID && spawnTask.spawnType == spawnType;
      }
  }
  private class SpawnTaskComparator implements Comparator<SpawnTask>
  {

      /**
       * This method provides a compare function for two spawnTasks.  This is used
       * for ordering the tasks in the list.
       *
       * @param obj1 is the first spawn task to compare.
       * @param obj2 is the second spawn task to compare.
       * @return a numerical representation of the difference of the two spawn
       * tasks.
       */
      public int compare(SpawnTask task1, SpawnTask task2)
      {
          int value1 = task1.getSpawnType() * SpawnTask.MAX_FREQ + task1.spawnID;
          int value2 = task2.getSpawnType() * SpawnTask.MAX_FREQ + task2.spawnID;

          return value2 - value1;
      }
  }
  private class SpawnTimer{
	  String playerName;
	  private int x, y;
	  private double radius;
	  private Timer clock;
	  private TimerTask runIt = new TimerTask(){
		  public void run(){
			  runIt();
		  }
	  };
	  public SpawnTimer(String playerName, int x, int y, double radius, int spawnTime){
		  this.playerName = playerName;
		  this.x = x;
		  this.y = y;
		  this.radius = radius;
		  clock = new Timer();
		  clock.schedule(runIt, spawnTime);
	  }
	  public void runIt(){
		  doRandomWarp(playerName,x,y,radius);
	  }
  }
}