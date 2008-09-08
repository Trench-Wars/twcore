package twcore.bots.multibot.rps;

import java.util.StringTokenizer;

import twcore.bots.MultiModule;
import twcore.core.OperatorList;
import twcore.core.events.Message;
import twcore.core.util.ModuleEventRequester;

public class rps extends MultiModule
{
  private RPSGame rpsGame;

  public void init()
  {
    rpsGame = new RPSGame(m_botAction);
  }

  public void requestEvents(ModuleEventRequester events)	{
  }

  public void doHelp(String sender)
  {
    String[] message =
    {
        "!Start player1:player2:rounds",
        "!Help",
        "!Go ArenaName"
    };

    m_botAction.smartPrivateMessageSpam(sender, message);
  }

  public  String[] getModHelpMessage() {
    	String[] helpText = new String[] {
    			"!Start player1:player2:rounds",
    	        "!Help",
    	        "!Go ArenaName"
            };
        return helpText;
    }

    public boolean isUnloadable() {
    	return true;
    }
    
    /**
     * This method is called when the module is unloaded
     */
    public void cancel() {
    	rpsGame.doKillGame();
    	m_botAction.cancelTasks();
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

  public void doGoCmd(String arena)
  {
    m_botAction.changeArena(arena);
  }

  public void handleCommand(String sender, String message)
  {
    String command = message.toLowerCase();
    OperatorList opList = m_botAction.getOperatorList();

    if(opList.isER(sender))
    {
      if(command.equals("!help"))
        doHelp(sender);
      if(command.startsWith("!start "))
        doStart(sender, message.substring(7));
      if(command.startsWith("!go "))
        doGoCmd(message.substring(4));
    }
  }

  public void handleStartedCommand(String sender, String message)
  {
    String command = message.toLowerCase();
    OperatorList opList = m_botAction.getOperatorList();

    if(opList.isER(sender))
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

}