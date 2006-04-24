package twcore.bots.zhbot;

import java.util.*;
import twcore.core.*;

public class twbotantispawn extends TWBotExtension
{
  public static final double SAFE_TIME_DEFAULT = 5;
  public static final int SHIELD_DELAY = 100;
  public static final int DEATH_DELAY = 1000;

  private HashSet antiSpawnTasks;
  private ShieldTask shieldTask;
  private double safeTime;
  private boolean antiSpawnEnabled;

  public twbotantispawn()
  {
    antiSpawnTasks = new HashSet();
    safeTime = SAFE_TIME_DEFAULT;
    antiSpawnEnabled = false;
  }

  public String[] getHelpMessages()
  {
    String[] message =
    {
      "!AntiSpawn On                             -- Activates the antispawn module",
      "!AntiSpawn Off                            -- Deactivates the antispawn module",
      "!SpawnTime <Time>                         -- Sets the amount of time "
    };

    return message;
  }

  public void doAntiSpawnOnCmd(String sender)
  {
    if(antiSpawnEnabled)
      throw new IllegalArgumentException("AntiSpawn module is already enabled.");
    antiSpawnEnabled = true;
    shieldTask = new ShieldTask();
    m_botAction.scheduleTaskAtFixedRate(shieldTask, 0, SHIELD_DELAY);
    m_botAction.sendArenaMessage("Antispawn module enabled.  Safe time set at " + safeTime + " seconds.", 2);
  }

  public void doAntiSpawnOffCmd(String sender)
  {
    if(!antiSpawnEnabled)
      throw new IllegalArgumentException("AntiSpawn module is already disabled.");
    antiSpawnEnabled = false;
    shieldTask.cancel();
    m_botAction.sendArenaMessage("Antispawn module disabled.");
  }

  public void doSafeTimeCmd(String sender, String argString)
  {
    try
    {
      safeTime = Double.parseDouble(argString);
      if(safeTime < 0.1)
        throw new IllegalArgumentException("Invalid safe time.");
      m_botAction.sendArenaMessage("Safe time set at " + safeTime + " seconds.", 2);
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Please use the following format: !SafeTime <Time>.");
    }
  }

  public void handleCommand(String sender, String message)
  {
    String command = message.toLowerCase();

    try
    {
      if(command.equals("!antispawn on"))
        doAntiSpawnOnCmd(sender);
      if(command.equals("!antispawn off"))
        doAntiSpawnOffCmd(sender);
      if(command.startsWith("!safetime "))
        doSafeTimeCmd(sender, message.substring(10));
    }
    catch(Exception e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }

  public void handleEvent(Message event)
  {
    int messageType = event.getMessageType();
    int senderID = event.getPlayerID();
    String sender = m_botAction.getPlayerName(senderID);
    String message = event.getMessage();

    if(messageType == Message.PRIVATE_MESSAGE && m_opList.isER(sender))
      handleCommand(sender, message);
  }

  public void handleEvent(PlayerDeath event)
  {
    if(antiSpawnEnabled)
    {
      int playerID = event.getKilleeID();
      String playerName = m_botAction.getPlayerName(playerID);
      m_botAction.scheduleTask(new DeathDelayTask(playerName), DEATH_DELAY);
    }
  }

  public void cancel()
  {
    shieldTask.cancel();
  }

  private class ShieldTask extends TimerTask
  {
    public void run()
    {
      Iterator iterator = antiSpawnTasks.iterator();
      Player player;
      String playerName;

      while(iterator.hasNext())
      {
        playerName = (String) iterator.next();
        player = m_botAction.getPlayer(playerName);
        if(player.hasShields())
        {
          antiSpawnTasks.remove(playerName);
          m_botAction.sendArenaMessage("Removing " + playerName + " from the HashSet.");
          m_botAction.scheduleTask(new ResetTask(playerName), (int) (safeTime * 1000));
        }
        else
        {
          m_botAction.sendArenaMessage("Giving shields to " + playerName + ".");
          m_botAction.sendUnfilteredPrivateMessage(playerName, "*prize #18");
          m_botAction.sendUnfilteredPrivateMessage(playerName, "*prize #-8");
          m_botAction.sendUnfilteredPrivateMessage(playerName, "*prize #-8");
          m_botAction.sendUnfilteredPrivateMessage(playerName, "*prize #-8");
        }
      }
    }
  }

  private class ResetTask extends TimerTask
  {
    private String playerName;

    public ResetTask(String playerName)
    {
      this.playerName = playerName;
    }

    public void run()
    {
      m_botAction.sendArenaMessage(playerName + " is vulnerable again.");
      m_botAction.sendUnfilteredPrivateMessage(playerName, "*shipreset");
    }
  }

  private class DeathDelayTask extends TimerTask
  {
    private String playerName;

    public DeathDelayTask(String playerName)
    {
      this.playerName = playerName;
    }

    public void run()
    {
      m_botAction.sendArenaMessage("Adding " + playerName + " to HashSet.");
      antiSpawnTasks.add(playerName);
    }
  }
}