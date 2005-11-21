package twcore.bots.multibot.test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.Vector;

import twcore.bots.shared.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.Message;

public class test extends MultiModule
{
  public static final int ANNOUNCE_DELAY = 30 * 60 * 1000;

  private HashMap voteList;
  private HashMap voteResults;
  private AnnounceTask announceTask;
  private Vector options;
  private String description;
  private boolean isVoting;

  public void init()
  {
    voteList = new HashMap();
    voteResults = new HashMap();
    options = new Vector();
    m_botAction.sendUnfilteredPublicMessage("?chat=" + moduleSettings.getString("chat"));
    isVoting = false;
  }

  public void requestEvents(EventRequester eventRequester)
  {
    eventRequester.request(EventRequester.MESSAGE);
  }

  public boolean isUnloadable()
  {
    return !isVoting;
  }

  public String[] getModHelpMessage()
  {
    String[] message =
    {
      ""
    };
    return message;
  }

  public void handleEvent(Message event)
  {
    String sender = getSender(event);
    String message = event.getMessage();
    int messageType = event.getMessageType();

    if(messageType == Message.CHAT_MESSAGE)
      handleChatMessage(sender, message);
  }

  private void handleChatMessage(String sender, String message)
  {
    String command = message.toLowerCase();

    try
    {
      if(command.startsWith("!vote "))
        doVoteCmd(message.substring(6).trim());
      if(command.equals("!stopvote"))
        doStopVoteCmd();
      if(command.equals("!options"))
        doOptionsCmd(sender);
      if(command.equals("!voteresults"))
        doVoteResultsCmd();
    }
    catch(RuntimeException e)
    {
      m_botAction.sendChatMessage(e.getMessage());
    }
    if(isVoting)
      handleVote(sender, message);
  }

  private void doVoteResultsCmd()
  {
    if(!isVoting)
      throw new RuntimeException("Voting is not in progress.");
    tallyVotes();
    displayResults();
  }

  private void doOptionsCmd(String sender)
  {
    if(isVoting)
      displayVote(sender);
  }

  private void doVoteCmd(String argString)
  {
    StringTokenizer argTokens = new StringTokenizer(argString, ":");

    if(isVoting)
      throw new RuntimeException("Voting already in progress.");
    if(argTokens.countTokens() < 3)
      throw new IllegalArgumentException("Invalid syntax.  Please use !Vote <Description>:<Option1>:<Option2>");

    description = argTokens.nextToken();
    options.clear();
    while(argTokens.hasMoreTokens())
      options.add(argTokens.nextToken());
    announceTask = new AnnounceTask();
    m_botAction.scheduleTaskAtFixedRate(announceTask, 0, ANNOUNCE_DELAY);
    voteList.clear();
    isVoting = true;
  }

  private void doStopVoteCmd()
  {
    if(!isVoting)
      throw new RuntimeException("Voting not in progress.");

    announceTask.cancel();
    tallyVotes();
    displayResults();
    isVoting = false;
  }

  private void handleVote(String sender, String argString)
  {
    Integer oldVote;
    String lowerSender = sender.toLowerCase();
    int newVote = getVote(argString);

    if(newVote > 0 && newVote <= options.size())
    {
      oldVote = (Integer) voteList.get(lowerSender);
      voteList.put(lowerSender, new Integer(newVote));

      if(oldVote == null)
        m_botAction.sendSmartPrivateMessage(sender, "Vote " + newVote + " recorded.");
      else if(oldVote.intValue() == newVote)
        m_botAction.sendSmartPrivateMessage(sender, "Vote not changed.");
      else
        m_botAction.sendSmartPrivateMessage(sender, "Vote changed from " + oldVote + " to " + newVote + ".");
    }
    // reset the timer?
  }

  private int getVote(String argString)
  {
    try
    {
      return Integer.parseInt(argString);
    }
    catch(NumberFormatException e)
    {
      return 0;
    }
  }

  private void displayVote(String sender)
  {
    String option;

    if(sender == null)
      m_botAction.sendChatMessage(description);
    else
      m_botAction.sendSmartPrivateMessage(sender, description);

    for(int index = 0; index < options.size(); index++)
    {
      option = (String) options.get(index);
      if(sender == null)
        m_botAction.sendChatMessage((index + 1) + ") " + option);
      else
        m_botAction.sendSmartPrivateMessage(sender, (index + 1) + ") " + option);
    }
  }

  private void displayVote()
  {
    displayVote(null);
  }

  private void clearResults()
  {
    voteResults.clear();

    for(int index = 0; index < options.size(); index++)
      voteResults.put(new Integer(index + 1), new VoteResult(index + 1));
  }

  private void displayResults()
  {
    TreeSet treeSet = new TreeSet(voteResults.values());
    Iterator iterator = treeSet.iterator();
    VoteResult result;
    String option;

    m_botAction.sendChatMessage("Results for: " + description);

    while(iterator.hasNext())
    {
      result = (VoteResult) iterator.next();
      option = (String) options.get(result.getOption() - 1);

      m_botAction.sendChatMessage(option + ": " + result.getCount());
    }
  }

  private void tallyVotes()
  {
    Collection collection = voteList.values();
    Iterator iterator = collection.iterator();

    clearResults();
    while(iterator.hasNext())
      addVote((Integer) iterator.next());
  }

  private void addVote(Integer option)
  {
    VoteResult result = (VoteResult) voteResults.get(option);

    if(voteResults.containsKey(option))
      result.addVote();
  }

  /**
   * This method gets the sender of the message regardless of the message type.
   *
   * @param event is the message event.
   * @returns the name of the sender of the message is returned.  If there was
   * no sender, then null is returned.
   */
  private String getSender(Message event)
  {
    int messageType = event.getMessageType();
    int senderID;

    if(messageType == Message.ALERT_MESSAGE || messageType == Message.CHAT_MESSAGE ||
       messageType == Message.REMOTE_PRIVATE_MESSAGE)
      return event.getMessager();
    senderID = event.getPlayerID();
    return m_botAction.getPlayerName(senderID);
  }

  private class VoteResult implements Comparable
  {
    private int option;
    private int count;

    public VoteResult(int option)
    {
      this.option = option;
      count = 0;
    }

    public int getOption()
    {
      return option;
    }

    public int getCount()
    {
      return count;
    }

    public void addVote()
    {
      count++;
    }

    public int compareTo(Object o)
    {
      VoteResult otherResult = (VoteResult) o;

      if(otherResult.count != count)
        return otherResult.count - count;
      return option - otherResult.option;
    }

    public boolean equals(Object o)
    {
      VoteResult otherResult = (VoteResult) o;

      return otherResult.option == option;
    }
  }

  private class AnnounceTask extends TimerTask
  {
    public void run()
    {
      displayVote();
    }
  }
}
