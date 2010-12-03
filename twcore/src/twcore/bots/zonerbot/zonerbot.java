package twcore.bots.zonerbot;
 
import java.util.TimerTask;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;
import twcore.core.util.ipc.IPCMessage;
 
/**
 * <p>Title: </p>Zonerbot
 * <p>Description: </p>This bot handles zoning for lower staff.
 *
 * To Do:
 * - Add requesting
 *   - Create a arena / event list
 *     - Add event
 *     - Add Arena
 * - Add detailed scorekeeping
 * - Add IPC interface
 * - Add Banning
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>For SSCU Trench Wars
 * @author Cpt.Guano!
 * @version 1.0
 */
 
public class zonerbot extends SubspaceBot
{
  public static final String ZONE_CHANNEL = "Zone Channel";
  public static final String HOSTNAME = "localhost";
  public static final String GO_STRING = "?go ";
  public static final int LINE_LENGTH = 120;
  public static final int REQUEST_LIFE_TIME = 120;
  public static final int NATURAL_LINE_LENGTH = 200;
 
  private VectorMap<String, Advert> advertQueue;
  private VectorMap<Integer, PeriodicZone> PeriodicMsgs;
  private TimedHashSet<String> recentAdvertList;
  private AdvertTimer advertTimer;
  private IdleTimer idleTimer;
  private int advertTime;
  private int idleTime;
  private int recentAdvertTime;
  private int finalAdvertTime;
  private int maxQueueLength;
  boolean isEnabled;
 
  /**
   * This method initializes the zonerbot.
   *
   * @param botAction is the BotAction instance of the bot.
   */
  public zonerbot(BotAction botAction)
  {
    super(botAction);
    requestEvents();
    advertQueue = new VectorMap<String, Advert>();
    PeriodicMsgs = new VectorMap<Integer, PeriodicZone>();
    recentAdvertList = new TimedHashSet<String>();
    advertTime = 10;
    finalAdvertTime = 5;
    idleTime = 5;
    recentAdvertTime = 5;
    maxQueueLength = 5;
    isEnabled = true;
  }
 
  public void handleEvent(Message event)
  {
    OperatorList opList = m_botAction.getOperatorList();
    String sender = getSender(event);
    String message = event.getMessage();
    String alertCommandType = event.getAlertCommandType();
    int messageType = event.getMessageType();
 
    try
    {
      if(sender != null)
      {
        if(opList.isOwner(sender))
          handleOwnerCommands(sender, message, messageType);
        if(opList.isSmod(sender))
          handleSmodCommands(sender, message, messageType);
        if(opList.isModerator(sender))
          handleModCommands(sender, message, messageType);
        if(opList.isER(sender) || advertQueue.containsKey(sender.toLowerCase()))
          handleERCommands(sender, message, messageType, alertCommandType);
      }
      if(messageType == Message.ARENA_MESSAGE)
        handleArenaMessage(message);
    }
    catch(Exception e)
    {
      m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
    }
  }
 
  // This is to catch any IPC events though none of them need to be catched to do something with the results
  public void handleEvent( InterProcessEvent event ) {}
 
  /**
   * This private method handles all of the ER commands.
   *
   * @param sender is the sender of the command.
   * @param message is the message that was sent.
   * @param messageType is the type of message that was sent.
   * @param alertCommandType is the message's alert command type (i.e. ?advert).
   */
  private void handleERCommands(String sender, String message, int messageType, String alertCommandType)
  {
    String command = message.toLowerCase();
 
    if(isEnabled)
    {
      if(messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE)
      {
        if(command.equals("!adv") || command.equals("!advert"))
          doAdvertCmd(sender);
        if(command.startsWith("!adv "))
          doOldAdvertCmd(sender, message.substring(5).trim());
        if (command.startsWith("!advert "))
          doOldAdvertCmd(sender, message.substring(8).trim());
        if (command.startsWith("!readvert "))
            doFinalAdvertCmd(sender, message.substring(9).trim(), messageType);
        if (command.startsWith("!setadvert "))
          doSetAdvertCmd(sender, message.substring(11).trim());
        if (command.startsWith("!setsound "))
          doSetSoundCmd(sender, message.substring(10).trim());
        if (command.equals("!viewadvert"))
          doViewAdvertCmd(sender);
        if (command.equals("!random"))
          throw new RuntimeException("Function not implemented yet.");
        if (command.startsWith("!random "))
          throw new RuntimeException("Function not implemented yet.");
        if (command.equals("!clearadvert"))
          doClearAdvertCmd(sender);
        if (command.equals("!claim"))
          doClaimCmd(sender, false);
        if (command.equals("!free"))
          doFreeCmd(sender);
        if (command.equals("!status"))
          doStatusCmd(sender);
        if (command.equals("!help"))
          doERHelpCmd(sender);
      }
      if(messageType == Message.ALERT_MESSAGE && alertCommandType.equals("advert"))
      {
        if(command.indexOf("free") != -1)
          doFreeCmd(sender);
        else
          doClaimCmd(sender, false);
      }
    }
  }
  

  
  private void doFinalAdvertCmd(String sender, String arena, int soundCode) {
      String hi = sender.toLowerCase();
      String event = arena.toUpperCase();
      Advert hey = advertQueue.get(hi);
      
      if(hey != advertQueue.firstValue())
          throw new RuntimeException("Someone else holds the key to your advert :(.");
      if(event.contains(" "))
          throw new RuntimeException("Please do NOT add spaces or words after your arena/event name.");
      if(hey == null)
          throw new RuntimeException("Nice try! Please claim an advert first.");
      m_botAction.sendZoneMessage("[" +event+"] A hosted event is just about to start! This is the last call! Type ?go " +event+ " to join. -" +sender,1);
      m_botAction.sendSmartPrivateMessage(hi, "Well, That's all I can do for you now. Please get SMod+ authority to !advert again.");
      setAdvertTimer(finalAdvertTime * 60 * 1000);
      recentAdvertList.add(sender, recentAdvertTime * 60 * 1000);
      removeFromQueue(0);
}
 




 
/**
   * This method performs the !advert command.
   *
   * @param sender is the person sending the command.
   */
  private void doAdvertCmd(String sender)
  {
    String lowerSender = sender.toLowerCase();
    Advert advert = advertQueue.get(lowerSender);
 
    if(advert == null)
      throw new RuntimeException("You must claim an advert first.");
    if(advert != advertQueue.firstValue())
      throw new RuntimeException("It is not your turn to advert.");
    if(!advert.canZone())
      throw new RuntimeException("Your advert has not been approved yet.  Please get a moderator to approve your advert.");
    if(advert.getAdvert().length() == 0)
      throw new RuntimeException("You must enter a message to advert.");
    if(!advertTimer.isExpired())
      throw new RuntimeException("The advert is not available: " + advertTimer.toString() + " remaining.");
    if(StringTools.indexOfIgnoreCase(advert.getAdvert(), GO_STRING) == -1)
      throw new RuntimeException("You did not specify what arena to ?go to.");
    zoneAdvert(advert);
  }
 
  /**
   * This method zones the advert, sets the advert timer, cancels the idle
   * timer, and informs RoboHelp that the zoner has been sent.
   *
   * @param advert is the advert to send.
   */
  private void zoneAdvert(Advert advert)
  {
    String adverter = advert.getAdverter().toLowerCase();
    String advertText = advert.toString();
 
    if(advertText.length() <= NATURAL_LINE_LENGTH)
      m_botAction.sendZoneMessage(advertText, advert.getSound());
    else
      zoneMessageSpam(StringTools.wrapString(advertText, LINE_LENGTH), advert.getSound());
    setAdvertTimer(advertTime * 60 * 1000);
    recentAdvertList.add(adverter, recentAdvertTime * 60 * 1000);

    //removeFromQueue(0);
 
    // Send an IPC message to Robohelp to record the advert
    IPCMessage msg = new IPCMessage( adverter + "@ad@" + advert.getArenaName( advertText ) + "@ad@" + advertText);
    m_botAction.ipcTransmit( ZONE_CHANNEL, msg );
  }
 
  /**
   * This method sends out a String array as zone messages.
   *
   * @param message is the message to send.
   * @param sound is the sound to associate with the message.
   */
  private void zoneMessageSpam(String message[], int sound)
  {
    m_botAction.sendZoneMessage(message[0], sound);
    for(int index = 1; index < message.length; index++)
      m_botAction.sendZoneMessage(message[index]);
  }
 
  /**
   * This method performs the old advert command with the format:
   * !Advert &lt;advert text&gt;.
   *
   * @param sender is the person issuing the command.
   * @param argString is the message to advert.
   */
  private void doOldAdvertCmd(String sender, String argString)
  {
    String lowerSender = sender.toLowerCase();
    Advert advert = advertQueue.get(lowerSender);
 
    if(advert == null )
      throw new RuntimeException("You must claim an advert first.");
    if(advert != advertQueue.firstValue())
      throw new RuntimeException("It is not your turn to advert.");
    if(argString.length() == 0)
      throw new RuntimeException("You must enter a message to advert.");
    if(!advertTimer.isExpired())
      throw new RuntimeException("The advert is not available: " + advertTimer.toString() + " remaining.");
    
 
    advert.setAdvert(argString);
    if(!advert.canZone())
      throw new RuntimeException("Your advert needs to be approved.  Please get a moderator to approve your advert.");
 
    zoneAdvert(advert);
    m_botAction.sendSmartPrivateMessage(sender, "You are eligible to !readvert incase you need another *zone. If not, Please !free (2 mins)");
    TimerTask wait2mins = new TimerTask(){ 
        public void run(){
            removeFromQueue(0);
        }};
        try{
            m_botAction.scheduleTask(wait2mins, 2 * Tools.TimeInMillis.MINUTE);
            
        }catch(Exception e){}




  }
 
  /**
   * This method does the setAdvert command.  The sender must have claimed an
   * ad already.
   *
   * @param sender is the sender of the command.
   * @param argString are the arguments of the command.
   */
  private void doSetAdvertCmd(String sender, String argString)
  {
    Advert advert = advertQueue.get(sender.toLowerCase());
 
    if(advert == null)
      throw new RuntimeException("You must claim an advert first.");
    if(argString.length() == 0)
      throw new IllegalArgumentException("You must enter a message to add to your advert.");
    if(!advert.isApproved())
      m_botAction.sendSmartPrivateMessage(sender, "Message part added.  Type !ViewAdvert to view your current advert.");
    else
    {
      advert.unapproveAdvert();
      m_botAction.sendSmartPrivateMessage(sender, "Message part added.  Type !ViewAdvert to view your current advert.  Your advert must be approved again.");
    }
    advert.setAdvert(argString);
  }
 
  /**
   * This method sets the sound of the advert.
   *
   * @param sender is the sender of the command.
   * @param argString is the new sound.
   */
  private void doSetSoundCmd(String sender, String argString)
  {
    try
    {
      Advert advert = advertQueue.get(sender.toLowerCase());
      int sound = Integer.parseInt(argString);
 
      if(advert == null)
        throw new RuntimeException("You must claim an advert first.");
      if(!advert.isApproved())
        m_botAction.sendSmartPrivateMessage(sender, "Advert sound changed.");
      else
      {
        advert.unapproveAdvert();
        m_botAction.sendSmartPrivateMessage(sender, "Advert sound changed.  Your advert must be approved again.");
      }
      advert.setSound(sound);
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Invalid sound value.  Must be between 1 and 255.");
    }
  }
 
  /**
   * This method displays a person's current advert.
   *
   * @param sender is the sender of the command.
   * @param argString are the arguments of the command.
   */
  private void doViewAdvertCmd(String sender, String argString)
  {
    OperatorList opList = m_botAction.getOperatorList();
    Advert advert = advertQueue.get(argString.toLowerCase());
    String advertText;
 
    if(!opList.isBot(argString))
      throw new RuntimeException("You are not authorized to view that advert.");
    if(advert == null)
      throw new RuntimeException(argString + " has not yet claimed the advert.");
    if(advert.getAdvert().length() == 0)
      throw new RuntimeException(argString + " has not yet entered an advert.");
    advertText = advert.toString();
    if(advertText.length() <= NATURAL_LINE_LENGTH)
      m_botAction.sendSmartPrivateMessage(sender, advertText);
    else
    {
      m_botAction.smartPrivateMessageSpam(sender, StringTools.wrapString(advertText, LINE_LENGTH));
      m_botAction.sendSmartPrivateMessage(sender, "Sound: " + advert.getSound());
    }
  }
 
  /**
   * This method displays the advert of the sender.
   *
   * @param sender is the sender of the command.
   */
  private void doViewAdvertCmd(String sender)
  {
    Advert advert = advertQueue.get(sender.toLowerCase());
    String advertText;
 
    if (advert == null)
      throw new RuntimeException("You must claim an advert first.");
    if(advert.getAdvert().length() == 0)
      throw new RuntimeException("You have not yet entered an advert.");
    advertText = advert.toString();
    if(advertText.length() <= NATURAL_LINE_LENGTH)
      m_botAction.sendSmartPrivateMessage(sender, advertText);
    else
    {
      m_botAction.smartPrivateMessageSpam(sender, StringTools.wrapString(advertText, LINE_LENGTH));
      m_botAction.sendSmartPrivateMessage(sender, "Sound: " + advert.getSound());
    }
  }
 
  /**
   * This method does the clearAdvert command.  It clears the sender's current
   * advert.
   *
   * @param sender is the person that sent the command.
   */
  private void doClearAdvertCmd(String sender)
  {
    Advert advert = advertQueue.get(sender.toLowerCase());
 
    if (advert == null)
      throw new RuntimeException("You must claim an advert first.");
    advert.clearAdvert();
    m_botAction.sendSmartPrivateMessage(sender, "Your advert has been cleared.");
  }
 
  /**
   * This method performs the claim command.  It claims an advert for the
   * person.
   *
   * @param sender is the person who issued the command.
   * @param isGrant is true if the claim was granted.
   */
  private void doClaimCmd(String sender, boolean isGrant)
  {
    Advert advert;
    String lowerSender = sender.toLowerCase();
    
    if(advertQueue.containsKey(lowerSender) && !isGrant)
      throw new RuntimeException("You have already claimed an advert.");
    if(advertQueue.containsKey(lowerSender) && isGrant)
      throw new RuntimeException(sender + " has already claimed an advert.");
    if(advertQueue.size() > maxQueueLength)
      throw new RuntimeException("Advert queue full.  Max queue size: " + maxQueueLength + ".");
    if(recentAdvertList.contains(lowerSender) && !isGrant)
      throw new RuntimeException("Not enough time has passed since your last advert.  You must wait " + recentAdvertList.getTimeRemainingString(lowerSender) + ".");
    if(recentAdvertList.contains(lowerSender) && isGrant)
      throw new RuntimeException("Not enough time has passed since " + sender + "'s last advert.  You must wait " + recentAdvertList.getTimeRemainingString(lowerSender) + ".");
 
    advert = new Advert(sender, isGrant);
    advertQueue.put(lowerSender, advert);
 
    if(advertQueue.size() == 1 && (advertTimer == null || advertTimer.isExpired()))
      setIdleTimer(idleTime * 60 * 1000);
    notifyQueuePosition(sender);
  }
 
  /**
   * This method notifies a player of their position in the advert queue.  If
   * it is currently their turn to advert, and the timer is expired, the time
   * until the advert expires is displayed.  If the timer has not expired, then
   * the time till the next advert is displayed.  If the player is in the
   * advert queue then the position is returned.
   *
   * @param playerName is the name of the player to notify.
   */
  private void notifyQueuePosition(String playerName)
  {
    int queuePosition = advertQueue.indexOfKey(playerName.toLowerCase());
 
    if(queuePosition == -1)
      throw new RuntimeException("You have not yet claimed an advert.");
 
    if(queuePosition != 0)
      m_botAction.sendSmartPrivateMessage(playerName, "You are " + StringTools.getCountString(queuePosition) + " in the advert queue.");
    else
    {
      if(advertTimer.isExpired())
        m_botAction.sendSmartPrivateMessage(playerName, "You may use your advert now.  You have " + idleTimer.toString() + " to use it.");
      else
        m_botAction.sendSmartPrivateMessage(playerName, "You have the next advert.  You may zone in " + advertTimer.toString() + ".");
    }
  }
 
  /**
   * This method performs the free command.  It removes a player from the
   * advert queue.
   *
   * @param playerName is the name of the player to remove.
   */
  private void doFreeCmd(String playerName)
  {
    String lowerName = playerName.toLowerCase();
    int removeIndex = advertQueue.indexOfKey(lowerName);
 
    if(removeIndex == -1)
      throw new RuntimeException("You must claim an advert first.");
    removeFromQueue(removeIndex);
    m_botAction.sendSmartPrivateMessage(playerName, "Your advert has been freed.");
  }
 
  /**
   * This method frees an advert at removeIndex and notifies all affected
   * people of their new position in the queue.
   *
   * @param removeIndex is the index to remove.
   */
  private void removeFromQueue(int removeIndex)
  {
    if(removeIndex < 0 || removeIndex > advertQueue.size())
      throw new IllegalArgumentException("Invalid remove index.");
 
    advertQueue.remove(removeIndex);
    if(removeIndex == 0)
      removeCurrentAdverter();
    for(int notifyIndex = removeIndex; notifyIndex < advertQueue.size(); notifyIndex++)
      notifyQueuePosition(advertQueue.getKey(notifyIndex));
  }
 
  /**
   * This method removes the current adverter from the queue.  It resets the
   * idleTimer and notifies all of the affected people.
   */
  private void removeCurrentAdverter()
  {
    if(advertQueue.size() > 0 && advertTimer.isExpired())
      setIdleTimer(idleTime * 60 * 1000);
    else
        m_botAction.cancelTask(idleTimer);
  }
 
  /**
   * This method performs the status command.  It displays who owns the current
   * advert and shows hte people in the advert queue.
   *
   * @param sender is the sender of the command.
   */
  private void doStatusCmd(String sender)
  {
    if(advertQueue.size() > 0)
      m_botAction.sendSmartPrivateMessage(sender, "Current advert belongs to: " + advertQueue.firstKey() + ".");
    if(advertQueue.size() > 1)
    {
      m_botAction.sendSmartPrivateMessage(sender, "People in queue:");
      for(int index = 1; index < advertQueue.size(); index++)
        m_botAction.sendSmartPrivateMessage(sender, index + ". " + advertQueue.getKey(index));
    }
    notifyQueuePosition(sender);
  }
 
  /**
   * This method processes an ER !help command.  It sends the help message to
   * the sender.
   *
   * @param sender is the player that sent the message.
   */
  private void doERHelpCmd(String sender)
  {
    String message[] =
    {
      "=========================================== ER Commands ============================================",
      "!Advert                            -- Zones your advert.",
      "!SetAdvert <Advert Part>           -- Appends <Advert Part> on the end of the current advert.",
      "!SetSound <Sound>                  -- Sets the sound of the advert.",
      "!ViewAdvert                        -- Views your current advert.",
      "!ClearAdvert                       -- Clears your current advert.",
      "!Random <Arena Name>               -- Sets your advert to a random one from the database for",
      "                                      <Arena Name>.  If no arena name is specified then a generic",
      "                                      advert is produced.",
      "!Claim                             -- Claim the advert for use.",
      "!Free                              -- Frees up your claim on an advert.",
      "!Status                            -- Displays the person that has currently claimed the advert, the",
      "                                      time left till the next advert and the advert queue.",
      "!PreAdvert <Arena Name>            -- Sends an advert that will display 3 minutes for the event to begin",
      "!ReAdvert                          -- Sends an advert, displaying event is starting.",
    };
    m_botAction.smartPrivateMessageSpam(sender, message);
  }
 
  /**
   * This method handles mod commands.
   *
   * @param sender is the sender of the command.
   * @param message is the message to handle.
   * @param messageType is tie message type.
   */
  private void handleModCommands(String sender, String message, int messageType)
  {
    String command = message.toLowerCase();
    if(messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE)
    {
      if(command.startsWith("!grant "))
        doGrantCmd(sender, message.substring(7).trim());
      if(command.startsWith("!viewadvert "))
        doViewAdvertCmd(sender, message.substring(12).trim());
      if(command.startsWith("!approve "))
        doApproveCmd(sender ,message.substring(9).trim());
      if(command.equals("!help"))
        doModHelpCmd(sender);
    }
  }
 
  /**
   * This method grants an advert to a staff member by allowing them to claim
   * an advert.
   *
   * @param sender is the sender of the command.
   * @param argString is the argString.
   */
  private void doGrantCmd(String sender, String argString)
  {
    OperatorList opList = m_botAction.getOperatorList();
 
    if(!opList.isBot(argString) || opList.isER(argString))
      throw new IllegalArgumentException("The player that you are granting the advert to must be a ZH.");
    doClaimCmd(argString, true);
    m_botAction.sendSmartPrivateMessage(sender, argString + " granted an advert.");
  }
 
  /**
   * This method approves an advert that was granted to a zh.
   *
   * @param sender is the sender of the command.
   * @param argString is a string containing the arguments of the command.
   */
  private void doApproveCmd(String sender, String argString)
  {
    Advert advert = advertQueue.get(argString.toLowerCase());
 
    if(advert == null)
      throw new RuntimeException(argString + " has not claimed an advert yet.");
    advert.approveAdvert(sender);
    m_botAction.sendSmartPrivateMessage(sender, "Advert approved.");
    m_botAction.sendSmartPrivateMessage(argString, "Advert approved by " + sender + ".  Type !Advert to zone your advert.");
  }
 
  /**
   * This method processes a Mod !help command.  It sends the help message to
   * the sender.
   *
   * @param sender is the player that sent the message.
   */
  private void doModHelpCmd(String sender)
  {
    String message[] =
    {
      "========================================== Mod Commands ============================================",
      "!Grant <Player>                    -- Grants an advert to <Player>.",
      "!ViewAdvert <Player>               -- Views the advert of <Player>.",
      "!Approve <Player>                  -- Approves the advert of <Player>."
    };
    m_botAction.smartPrivateMessageSpam(sender, message);
  }
 
  /**
   * This method handles smod commands.
   *
   * @param sender is the sender of the command.
   * @param message is the message to handle.
   * @param messageType is the message type.
   */
  private void handleSmodCommands(String sender, String message, int messageType)
  {
    String command = message.toLowerCase();
    if(messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE)
    {
      if(command.startsWith("!periodic "))
        doPeriodicZoner(sender, message.substring(10).trim());
      if(command.startsWith("!listperiodic"))
          doListPeriodicZoner(sender);
      if(command.startsWith("!removeperiodic "))
          doRemovePeriodicZoner(sender, message.substring(16).trim());
      if(command.startsWith("!setadvertdelay "))
        doSetAdvertDelayCmd(sender, message.substring(16).trim());
      if(command.startsWith("!setidletime "))
        doSetIdleTimeCmd(sender, message.substring(13).trim());
      if(command.startsWith("!setreadverttime "))
        doSetReAdvertTimeCmd(sender, message.substring(17).trim());
      if(command.startsWith("!setqueuelength "))
        doSetQueueLengthCmd(sender, message.substring(16).trim());
      if(command.equals("!enable"))
        doEnableCmd(sender);
      if(command.equals("!disable"))
        doDisableCmd(sender);
      if(command.equals("!die"))
        doDieCmd();
      if(command.equals("!help"))
        doSmodHelpCmd(sender);
    }
  }
 
  private void handleOwnerCommands(String sender, String message, int messageType) {
        String command = message.toLowerCase();
    if(messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE)
    {
      if(command.startsWith("!staffadv "))
        m_botAction.sendZoneMessage(message.substring(10).trim() + " -TW Staff", 2);
    }
  }
 
  /**
   * This method allows the sender to setup a periodic
   * message to be zoned with a delay for an interval.
   * Syntax <delay>:<interval>:<message>
   *
   * @param sender is the sender of the command.
   * @param argString is the arg string.
   */
 
  private void doPeriodicZoner(String sender, String argString) {
          int delay,life;
          String message;
          try {
                  String[] parts = argString.split(";");
                  delay = Integer.parseInt(parts[0]);
                  life = Integer.parseInt(parts[1]);
                  message = parts[2];
                 
                  int pos = PeriodicMsgs.size()+1;
                  for (int i = 0 ; i < PeriodicMsgs.size() ; i++)       {
                          if (!PeriodicMsgs.containsKey(new Integer(i+1)))
                                  pos = i+1;
                  }
                 
                  PeriodicZone periodicTask = new PeriodicZone(delay,life,pos,sender,message);
                  PeriodicMsgs.put(new Integer(pos), periodicTask);
                  m_botAction.scheduleTask(periodicTask,1000, delay*60000);
                 
                  m_botAction.sendPrivateMessage(sender, "A periodic zoner has been set with" +
                                " a delay of " + delay + " minute(s) for a life of " + life +
                                " hour(s) with the message: \"" + message + "\"");
          } catch (NumberFormatException nfe)   {
                  m_botAction.sendPrivateMessage(sender, "Interval and delay must be numerical.");
          } catch (IllegalArgumentException iae)        {
                  m_botAction.sendPrivateMessage(sender, iae.getMessage());
          } catch (Exception e) {
                  m_botAction.sendPrivateMessage(sender, "Incorrect syntax.");
          }
  }
 
  /**
   * Lists all the periodic zone messages if any to the sender.
   *
   * @param sender is the sender of the command.
   */
 
  private void doListPeriodicZoner(String sender)       {
          if (PeriodicMsgs.isEmpty())   {
                  m_botAction.sendPrivateMessage(sender, "There are currently no periodic zoners.");
                  return;
          }
          Iterator<Integer> idx = PeriodicMsgs.keySet().iterator();
          Iterator<PeriodicZone> zoner = PeriodicMsgs.values().iterator();
         
          while (idx.hasNext()) {
                  m_botAction.sendPrivateMessage(sender, idx.next() + ") " +
                                  zoner.next().toString());
          }
  }
 
  /**
   * This method removes a periodic using it's index.
   * Syntax <index>
   *
   * @param sender is the sender of the command.
   * @param argString is the arg string.
   */
 
  private void doRemovePeriodicZoner(String sender, String argString)   {
          Integer idx;
          try {
                  idx = new Integer(Integer.parseInt(argString));
                  if (!PeriodicMsgs.containsKey(idx))  
                          throw new IllegalArgumentException("No such index.");
                  PeriodicMsgs.get(idx).cancel();
                  m_botAction.sendPrivateMessage(sender, "Periodic zoner: \"" +
                                  PeriodicMsgs.remove(idx).getMsg() + "\" has been removed");
          } catch (NumberFormatException nfe)   {
                  m_botAction.sendPrivateMessage(sender, "Index must be numerical.");
          } catch (IllegalArgumentException iae)        {
                  m_botAction.sendPrivateMessage(sender, iae.getMessage());
          } catch (Exception e) {
                  m_botAction.sendPrivateMessage(sender, "Incorrect syntax.");
          }
  }
 
 
 
  /**
   * This method allows the sender to change the time between adverts.  The
   * change is saved in the configuration file.
   *
   * @param sender is the sender of the command.
   * @param argString is the arg string.
   */
  private void doSetAdvertDelayCmd(String sender, String argString)
  {
    try
    {
//      BotSettings botSettings = m_botAction.getBotSettings();
 
      int newAdvertTime = Integer.parseInt(argString);
      if(newAdvertTime < 0)
        throw new IllegalArgumentException("Advert delay must be greater than 0.");
//      botSettings.put("adverttime", newAdvertTime);
      m_botAction.sendSmartPrivateMessage(sender, "Advert delay changed.");
    }
    catch(NumberFormatException e)
    {
      throw new IllegalArgumentException("Please use the following format: !SetAdvertDelay <Time>");
    }
  }
 
  /**
   * This method allows the sender to change the time before an unused advert
   * expires.  The change is saved in the configuration file.
   *
   * @param sender is the sender of the command.
   * @param argString is the arg string.
   */
  private void doSetIdleTimeCmd(String sender, String argString)
  {
    try
    {
//      BotSettings botSettings = m_botAction.getBotSettings();
 
      int newIdleTime = Integer.parseInt(argString);
      if(newIdleTime < 1)
        throw new IllegalArgumentException("Idle time must be greater than 1.");
//      botSettings.put("idletime", newIdleTime);
      m_botAction.sendSmartPrivateMessage(sender, "Idle time changed.");
    }
    catch(NumberFormatException e)
    {
      throw new IllegalArgumentException("Please use the following format: !SetIdleTime <Time>");
    }
  }
 
  /**
   * This method allows the sender to change the time before an unused advert
   * expires.  The change is saved in the configuration file.
   *
   * @param sender is the sender of the command.
   * @param argString is the arg string.
   */
  private void doSetReAdvertTimeCmd(String sender, String argString)
  {
    try
    {
//      BotSettings botSettings = m_botAction.getBotSettings();
 
      int newReAdvertTime = Integer.parseInt(argString);
      if(newReAdvertTime < 1)
        throw new IllegalArgumentException("Re-advert time must be greater than 1.");
//      botSettings.put("readverttime", newReAdvertTime);
      m_botAction.sendSmartPrivateMessage(sender, "Re-advert time changed.");
    }
    catch(NumberFormatException e)
    {
      throw new IllegalArgumentException("Please use the following format: !SetReAdvertTime <Time>");
    }
  }
 
  /**
   * This method allows the sender to change the time before an unused advert
   * expires.  The change is saved in the configuration file.
   *
   * @param sender is the sender of the command.
   * @param argString is the arg string.
   */
  private void doSetQueueLengthCmd(String sender, String argString)
  {
    try
    {
//      BotSettings botSettings = m_botAction.getBotSettings();
 
      int newQueueLength = Integer.parseInt(argString);
      if(newQueueLength < 0)
        throw new IllegalArgumentException("Queue length must be greater than 0.");
//      botSettings.put("queuelength", newQueueLength);
      m_botAction.sendSmartPrivateMessage(sender, "Queue length changed.");
    }
    catch(NumberFormatException e)
    {
      throw new IllegalArgumentException("Please use the following format: !SetQueueLength <Length>");
    }
  }
 
  /**
   * This method enables advertizing.
   *
   * @param sender is the sender of the command.
   */
  private void doEnableCmd(String sender)
  {
    if(isEnabled)
      throw new RuntimeException("Advertizing is already enabled.");
    isEnabled = true;
    m_botAction.sendSmartPrivateMessage(sender, "Advertizing enabled.");
  }
 
  /**
   * This method disables advertizing.
   *
   * @param sender is the sender of the command.
   */
  private void doDisableCmd(String sender)
  {
    if(!isEnabled)
      throw new RuntimeException("Advertizing is already disabled.");
    isEnabled = false;
    m_botAction.sendSmartPrivateMessage(sender, "Advertizing disabled.");
  }
 
  /**
   * This method logs the bot off.
   */
  private void doDieCmd()
  {
    m_botAction.sendChatMessage(m_botAction.getBotName() + " logging off.");
    m_botAction.scheduleTask(new DieTask(), 1000);
  }
 
  private void doSmodHelpCmd(String sender)
  {
    String message[] =
    {
      "========================================== SMOD Commands ===========================================",
      "!Periodic <delay>;<life>;<message>    -- Adds a periodic zone message. To add a sound use %%# ",
      "                                         at the end. delay is in minutes, life is in hours",
      "!ListPeriodic                         -- Lists all periodic zone messages and their index.",
      "!RemovePeriodic <index>               -- Removes the periodic message at the index.",
      "!SetAdvertDelay <Time>                -- Sets the time between adverts.",
      "!SetIdleTime <Time>                   -- Sets the number of minutes for an advert to expire.",
      "!SetReAdvertTime <Time>               -- Sets the number of minutes required before a player that has",
      "                                         just zoned claims another advert.",
      "!SetQueueLength <Length>              -- Sets the number of people allowed in the advert queue.",
      "!Enable                               -- Enables Advert bot.",
      "!Disable                              -- Disables Advert bot.",
      "!Die                                  -- Logs the bot off."
    };
 
    m_botAction.smartPrivateMessageSpam(sender, message);
  }
 
  /**
   * This method handles an arena message.
   *
   * @param message is the arena message to process.
   */
  private void handleArenaMessage(String message)
  {
    if(message.startsWith("Not online, last seen"))
      removeFromQueue(0);
  }
 
  /**
   * This method handles the logged on event.
   *
   * @param event is the event to handle.
   */
  public void handleEvent(LoggedOn event)
  {
    BotSettings botSettings = m_botAction.getBotSettings();
    String initialArena = botSettings.getString("initialarena");
    // ?obscene is sent automatically from GamePacketInterpreter, and
    //   should not be sent from inside any bot.
        //m_botAction.sendUnfilteredPublicMessage("?obscene");
/*    advertTime = botSettings.getInt("adverttime");
    idleTime = botSettings.getInt("idletime");
    recentAdvertTime = botSettings.getInt("readverttime");
    maxQueueLength = botSettings.getInt("queuelength");*/
 
    m_botAction.ipcSubscribe(ZONE_CHANNEL);
    m_botAction.changeArena(initialArena);
    setAdvertTimer(1000);
  }
 
  /**
   * This private method requests the events that the bot will use.
   */
  private void requestEvents()
  {
    EventRequester eventRequester = m_botAction.getEventRequester();
 
    eventRequester.request(EventRequester.MESSAGE);
  }
 
  /**
   * This method sets the advert timer to a certain length.
   *
   * @param milliseconds is the length of the advert timer.
   */
  private void setAdvertTimer(long milliseconds)
  {
    advertTimer = new AdvertTimer(milliseconds);
    m_botAction.scheduleTask(advertTimer, milliseconds);

  }
 
  private void setIdleTimer(long milliseconds)
  {
    if(idleTimer != null)
        m_botAction.cancelTask(idleTimer);
    idleTimer = new IdleTimer(milliseconds);
    m_botAction.scheduleTask(idleTimer, milliseconds);
  }
 
  /**
   * This private method gets the name of the sender of a message regardless
   * of the message type.  If there was no sender then null is returned.
   *
   * @param message is the message that was sent.
   * @return the sender of the message is returned.  If there was no sender then
   * null is returned.
   */
  private String getSender(Message message)
  {
    int messageType = message.getMessageType();
    int senderID;
 
    if(messageType == Message.CHAT_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE ||
       messageType == Message.ALERT_MESSAGE)
      return message.getMessager();
    senderID = message.getPlayerID();
    return m_botAction.getPlayerName(senderID);
  }
 
  private class PeriodicZone extends TimerTask {
          private int m_delay,m_life,m_index;
          private double m_time=0;
          private String m_sender,m_message;
         
          public PeriodicZone (int delay, int life, int index, String sender, String message)   {
                  m_delay = delay;
                  m_life = life;
                  m_message = message;
                  m_sender = sender;
                  m_index = index;
          }
         
          public String getMsg()        {
                  return m_message;
          }
         
          public String toString()      {
                  return "Set by: "+ m_sender + ", repeats every "
                  + m_delay + " minute(s), for " + m_life + " hour(s). \"" + m_message + "\"" ;
          }
         
          public void run()     {
                  m_time += m_delay;
                  int soundpos;
                  if ( m_time/60 > m_life)      {
                          PeriodicMsgs.remove(new Integer(m_index));
                          this.cancel();
                  }
                  try {
                                if ((soundpos = m_message.indexOf('%')) != -1)
                                        m_botAction.sendZoneMessage(
                                                        m_message.substring(0, soundpos)
                                                        ,Integer.parseInt(m_message.substring(soundpos+1)));
                                else
                                        m_botAction.sendZoneMessage(m_message);
                        } catch (Exception e)   {
                                m_botAction.sendSmartPrivateMessage(m_sender, "There is an" +
                                                " error in your sound code, it should be at the end." +
                                                " Ex: \"!periodic 20;5;hello world! %%1\" . " +
                                                "Your periodic has been removed.");
                                PeriodicMsgs.remove(new Integer(m_index));
                                this.cancel();
                        }
          }
  }
 
  private class IdleTimer extends DetailedTimerTask
  {
    public IdleTimer(long idleTime)
    {
      super(idleTime);
    }
 
    public void run()
    {
      String idleAdverter = advertQueue.firstKey();
      m_botAction.sendSmartPrivateMessage(idleAdverter, "You have failed to use your advert and it has expired.");
      removeFromQueue(0);
    }
  }
 
  /**
   * <p>Title:</p> AdvertTimer
   * <p>Description:</p> This class keeps track of how long is left till the
   * next advert.
   * <p>Copyright: Copyright (c) 2004</p>
   * <p>Company: </p>For SSCU Trench Wars
   * @author Cpt.Guano!
   * @version 1.0
   */
  private class AdvertTimer extends DetailedTimerTask
  {
    public AdvertTimer(long advertTime)
    {
      super(advertTime);
    }
 
    /**
     * This method is executed when the advert timer expires.
     */
    public void run()
    {
      String currentAdverter = advertQueue.firstKey();  
      Advert advert = advertQueue.firstValue();
      if(currentAdverter != null)
      {{
          
            if(advert.getAdvert().isEmpty()) {
               m_botAction.sendSmartPrivateMessage(currentAdverter, "You have not yet entered an advert.");
               recentAdvertList.add(currentAdverter, recentAdvertTime * 60 * 1000);
               removeFromQueue(0);
               return;
            }
            zoneAdvert(advert);
          }
          setIdleTimer(idleTime * 60 * 1000);
          notifyQueuePosition(currentAdverter); //TODO dc'd 10 minutes timer
          m_botAction.sendUnfilteredPublicMessage("?find " + currentAdverter);
      }
    }
  }
 
  /**
   * <p>Title: Advert</p>
   * <p>Description: This class is an advert.  It keeps track of the sound,
   * advert and the person that placed the advert.</p>
   * <p>Copyright: Copyright (c) 2004</p>
   * <p>Company: For SSCU Trench Wars</p>
   * @author Cpt.Guano!
   * @version 1.0
   */
  private class Advert
  {
    public static final int MAX_ADVERT_LENGTH = 400;
    public static final int DEFAULT_SOUND = 2;
    public static final int MIN_SOUND = 1;
    public static final int MAX_SOUND = 255;
    public static final int REGAN_SOUND = 6;
    public static final int SEX_SOUND = 12;
    public static final int PM_SOUND = 26;
    public static final int MUSIC1_SOUND = 100;
    public static final int MUSIC2_SOUND = 102;
 
    public static final int PRE_APPROVED_STATUS = 0;
    public static final int NOT_APPROVED_STATUS = 1;
    public static final int APPROVED_STATUS = 2;
 
    private String adverter;
    private String advertText;
    private int sound;
    private int advertStatus;
 
    /**
     * This method initializes a blank advert to a person with a default
     * sound.
     *
     * @param adverter is the person that owns the advert.
     * @param isGrant is true if the advert is granted.
     */
    public Advert(String adverter, boolean isGrant)
    {
      this.adverter = adverter;
      advertText = new String();
      sound = DEFAULT_SOUND;
 
      if(isGrant)
        advertStatus = NOT_APPROVED_STATUS;
      else
      {
        advertStatus = PRE_APPROVED_STATUS;
      }
 
    }
 
    /**
     * This method gets the person that owns the advert.
     *
     * @return the person that owns the advert is returned.
     */
    public String getAdverter()
    {
      return adverter;
    }
 
    /**
     * This method gets the advert text.
     *
     * @return the advert text is returned.
     */
    public String getAdvert()
    {
      return advertText;
    }
 
    /**
     * This method gets the sound of the advert.
     *
     * @return the sound of the advert is returned.
     */
    public int getSound()
    {
      return sound;
    }
 
    /**
     * This method checks to see if an advert can be zoned or not.
     *
     * @return true is returned if the advert can be zoned.
     */
    public boolean canZone()
    {
      return advertStatus == PRE_APPROVED_STATUS || isApproved();
    }
 
    /**
     * This method checks to see if the advert is approved.
     *
     * @return true is returned if the advert is the approved.
     */
    public boolean isApproved()
    {
      return advertStatus == APPROVED_STATUS;
    }
 
    /**
     * This method appends an advertPart onto the end of the current advert.
     *
     * @param advertPart is the part to add.
     */
    public void setAdvert(String advertPart)
    {
      StringBuffer result = new StringBuffer(advertText);
      if(advertStatus == APPROVED_STATUS)
        throw new IllegalArgumentException("Advert has already been approved.  You may no longer change it.");
      if(advertText.length() + advertPart.length() + 1 > MAX_ADVERT_LENGTH)
        throw new IllegalArgumentException("The advert is too long.  Must be less than " + MAX_ADVERT_LENGTH + " characters long.");
      if(advertPart.length() == 0)
        throw new IllegalArgumentException("You must enter a message to add.");
      if(advertText.length() > 0)
        result.append(' ');
      result.append(advertPart);
      advertText = result.toString();
    }
 
    /**
     * This method approves an advert.  If the advert has already been approved,
     * or if the advert does not need to be approved then an exception is
     * thrown.
     *
     * @param approver is the person that is approving the advert.
     */
    public void approveAdvert(String approver)
    {
      if(advertStatus == PRE_APPROVED_STATUS)
        throw new IllegalArgumentException("Advert does not need to be approved.");
      if(advertStatus == APPROVED_STATUS)
        throw new IllegalArgumentException("Advert has already been approved.");
      advertStatus = APPROVED_STATUS;
    }
 
    /**
     * This method unapproves the advert.  If the advert is already unapproved,
     * or if the advert is preapproved, then an exception is thrown.
     */
    public void unapproveAdvert()
    {
      if(advertStatus == PRE_APPROVED_STATUS)
        throw new IllegalArgumentException("Advert does not need to be approved.");
      if(advertStatus == NOT_APPROVED_STATUS)
        throw new IllegalArgumentException("Advert has not yet been approved.");
      advertStatus = NOT_APPROVED_STATUS;
    }
 
    /**
     * This method clears the advert that the user has entered so far.
     */
    public void clearAdvert()
    {
      if(advertStatus == APPROVED_STATUS)
        throw new IllegalArgumentException("Advert has already been approved.  You may no longer change it.");
      advertText = new String();
    }
 
    /**
     * This method sets the sound of the advert.
     *
     * @param sound is the new sound of the advert.
     */
    public void setSound(int sound)
    {
      if(advertStatus == APPROVED_STATUS)
        throw new IllegalArgumentException("Advert has already been approved.  You may no longer change it.");
      if(sound < MIN_SOUND || sound > MAX_SOUND)
        throw new IllegalArgumentException("Invalid sound.  Must be between " + MIN_SOUND + " and " + MAX_SOUND + ".");
      if(isBannedSound(sound))
        throw new IllegalArgumentException("You are not permitted to use that sound.");
      this.sound = sound;
    }
 
    /**
     * This method appends the adverter's name onto the end of the advert
     * resulting in the full advert string.
     *
     * @return The full advert string is returned.
     */
    public String toString()
    {
      return advertText + " -" + adverter;
    }
 
    /**
     * This method gets the arena name from the advert text.  If no arena name
     * is present then null is returned.
     * Modded by milosh - 2.4.08
     *
     * @param advertText is the advert to parse.
     * @return the name of the arena is returned.
     */
    private String getArenaName(String advertText)
    {
      String lowerAdvert = advertText.toLowerCase();
      int beginIndex = lowerAdvert.lastIndexOf(GO_STRING) + GO_STRING.length();
      String advertTail = lowerAdvert.substring(beginIndex).trim();
      int endIndex = advertTail.indexOf(' ');
 
      if(beginIndex == -1 || endIndex == -1)
        return null;
      return advertTail.substring(0, endIndex);
    }
 
    private boolean isBannedSound(int sound)
    {
      return sound == REGAN_SOUND || sound == SEX_SOUND || sound == PM_SOUND || sound == MUSIC1_SOUND || sound == MUSIC2_SOUND;
    }
  }
 
  private class DieTask extends TimerTask
  {
    public void run()
    {
      m_botAction.die();
    }
  }
}