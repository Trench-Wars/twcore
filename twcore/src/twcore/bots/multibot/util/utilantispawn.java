package twcore.bots.multibot.util;

import java.util.TimerTask;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.util.ModuleEventRequester;

public class utilantispawn extends MultiUtil
{
  public static final double SAFE_TIME_DEFAULT = 5;
  public static final int SHIELD_DELAY = 100;
  public static final double SPAWN_DELAY = 4;

  private double safeTime;
  private double spawnDelay;
  private boolean antiSpawnEnabled;

  public void init()
  {
    safeTime = SAFE_TIME_DEFAULT;
    spawnDelay = SPAWN_DELAY;
    antiSpawnEnabled = false;
  }
  
  /**
   * Requests events.
   */
  public void requestEvents( ModuleEventRequester modEventReq ) {
      modEventReq.request(this, EventRequester.PLAYER_DEATH );
  }  

  public String[] getHelpMessages()
  {
    String[] message =
    {
      "AntiSpawn(tm)   Use !SpawnDelay to set spawn time!  And make sure shields can be used.",
      "!SpawnDelay <Seconds>     -- Sets seconds between spawns.  MUST be correctly set.  Default 4.0",
      "!SafeTime <Seconds>       -- Sets milliseconds player is safe for after spawning.  Default 5.0",
      "!AntiSpawn On             -- Activates the antispawn module.",
      "!AntiSpawn Off            -- Deactivates the antispawn module.",
    };

    return message;
  }

  public void doAntiSpawnOnCmd(String sender)
  {
    if(antiSpawnEnabled)
      throw new IllegalArgumentException("AntiSpawn module is already enabled.");
    antiSpawnEnabled = true;
    m_botAction.sendArenaMessage("Antispawn module enabled.  Safe time set at " + safeTime + " seconds.", 2);
  }

  public void doAntiSpawnOffCmd(String sender)
  {
    if(!antiSpawnEnabled)
      throw new IllegalArgumentException("AntiSpawn module is already disabled.");
    antiSpawnEnabled = false;
    m_botAction.sendArenaMessage("Antispawn module disabled.");
  }

  public void doSafeTimeCmd(String sender, String argString)
  {
    try
    {
      safeTime = Double.parseDouble(argString);
      if(safeTime < 0.1)
        throw new IllegalArgumentException("Invalid safe time.");
      m_botAction.sendPrivateMessage(sender, "Safe time set at " + safeTime + " seconds.");
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Please use the following format: !SafeTime <Seconds>.");
    }
  }

  public void doSpawnDelayCmd(String sender, String argString)
  {
    try
    {
      spawnDelay = Double.parseDouble(argString);
      if(spawnDelay < 0.1)
        throw new IllegalArgumentException("Invalid spawn delay.");
      m_botAction.sendPrivateMessage(sender, "Spawn delay set at " + spawnDelay + " seconds.");
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Please use the following format: !SpawnDelay <Seconds>.");
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
      if(command.startsWith("!spawndelay "))
        doSpawnDelayCmd(sender, message.substring(12));
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
      m_botAction.scheduleTask(new DeathDelayTask(playerName), (long)(spawnDelay * 1000));
    }
  }

  public void cancel()
  {
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
        m_botAction.sendUnfilteredPrivateMessage(playerName, "*prize #18");     
        m_botAction.sendUnfilteredPrivateMessage(playerName, "*prize #-8");     
        m_botAction.sendUnfilteredPrivateMessage(playerName, "*prize #-8");     
        m_botAction.sendUnfilteredPrivateMessage(playerName, "*prize #-8");     
        m_botAction.scheduleTask(new ResetTask(playerName), (long)(safeTime * 1000));
    }
  }
}