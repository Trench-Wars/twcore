package twcore.bots.rpsbot;

import java.util.*;
import twcore.core.*;

public class rpsbot extends SubspaceBot
{
  private RPSGame rpsGame;

  public rpsbot(BotAction botAction)
  {
    super(botAction);
    rpsGame = new RPSGame(m_botAction);

    EventRequester eventRequester = m_botAction.getEventRequester();
    eventRequester.request(EventRequester.MESSAGE);
  }

  public void handleEvent(LoggedOn event)
  {
    BotSettings botSettings = m_botAction.getBotSettings();
    String initialArena = botSettings.getString("InitialArena");
    m_botAction.changeArena(initialArena);
  }

  public void doHelp(String sender)
  {
    String[] message =
    {
        "!Start player1:player2:rounds",
        "!Help",
        "!Go ArenaName",
        "!Die"
    };

    m_botAction.smartPrivateMessageSpam(sender, message);
  }

  public void doStart(String sender, String argString)
  {
    StringTokenizer argTokens = new StringTokenizer(argString, ":");
    int numTokens = argTokens.countTokens();
    int pointsTo = 1;

    try
    {
      if(numTokens < 2 || numTokens > 3)
        throw new IllegalArgumentException("Please use the following format: !Start <player1>:<player2>:points");
      String player1Name = m_botAction.getFuzzyPlayerName(argTokens.nextToken());
      String player2Name = m_botAction.getFuzzyPlayerName(argTokens.nextToken());
      if(player1Name == null || player2Name == null)
        throw new IllegalArgumentException("Error.  Player not found.");

      if(numTokens == 3)
        pointsTo = Integer.parseInt(argTokens.nextToken());
      rpsGame.doStart(player1Name, player2Name, pointsTo);
    }
    catch(NumberFormatException e)
    {
      throw new NumberFormatException("Please use the following format: !Start <player1>:<player2>:points");
    }
  }

  public void doDie(String sender)
  {
    m_botAction.sendSmartPrivateMessage(sender, "Bye!.");
    m_botAction.scheduleTask(new DieTask(), 100);
  }

  public void doGoCmd(String arena)
  {
    m_botAction.changeArena(arena);
  }

  public void handleCommand(String sender, String message)
  {
    String command = message.toLowerCase();
    OperatorList opList = m_botAction.getOperatorList();

    if(opList.isModerator(sender))
    {
      if(command.equals("!help"))
        doHelp(sender);
      if(command.startsWith("!start "))
        doStart(sender, message.substring(7));
      if(command.startsWith("!go "))
        doGoCmd(message.substring(4));
      if(command.equals("!die"))
        doDie(sender);
    }
  }

  public void handleStartedCommand(String sender, String message)
  {
    String command = message.toLowerCase();
    OperatorList opList = m_botAction.getOperatorList();

    if(opList.isModerator(sender))
    {
      if(command.equals("!help"))
        m_botAction.sendSmartPrivateMessage(sender, "!Killgame");
      if(command.equals("!killgame"))
        rpsGame.doKillGame();
    }
  }

  public void handleEvent(Message event)
  {
    int senderID = event.getPlayerID();
    String sender = m_botAction.getPlayerName(senderID);
    String message = event.getMessage().trim();

    if(event.getMessageType() == Message.PRIVATE_MESSAGE)
    {
      try
      {
        if(rpsGame.isStarted())
        {
          rpsGame.handleCommand(sender, message);
          handleStartedCommand(sender, message);
        }
        else
          handleCommand(sender, message);
      }
      catch(RuntimeException e)
      {
        m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
      }
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