package twcore.bots.multibot.twscript;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Date;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import twcore.bots.MultiUtil;
import twcore.bots.TWScript;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.util.ModuleEventRequester;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.util.Tools;

/**
 * @author milosh
 */
public class polls extends MultiUtil {
	
	public OperatorList opList;
	public TWScript m_twscript;
	
	public TreeMap<String, CustomPoll> polls;
	
	public TreeMap<String, String[]> waitingForAction;
	public ArrayList<NameIPMID> voted;
	
	public void init(){
		opList = m_botAction.getOperatorList();
		polls = new TreeMap<String, CustomPoll>();
		waitingForAction = new TreeMap<String, String[]>();
	}
	
	public void initializeTWScript(TWScript tws){
		m_twscript = tws;
	}
	
	public String[] getHelpMessages(){
		String[] message = {
				"+--------------------------------------- Polls ---------------------------------------+",
				"| !addpoll <poll>:<ques>        - Adds a poll named <name> with question <ques>.      |",
				"| !addoption <poll>:<opt>:<msg> - Adds an option to <poll> with answer <opt> and a    |",
				"|                               - TWScript <msg> that's sent to the bot if it wins.   |",
				"| !removepoll <poll>            - Removes <poll> and all of its options.              |",
				"| !removepoll <poll>:<index>    - Removes option at index <index> on poll <poll>      |",
				"| !poll <poll> d:h:m:s          - Starts poll <poll> which will end in d:h:m:s        |",
				"| !poll <poll> MM:DD:YYYY:h:m:s - Starts poll <poll> which will end at a specific date|",
				"| !cancelpoll <poll>            - Cancels the timer and removes votes for <poll>      |",
				"| !vote <poll>:<#>              - Votes for option <#> on poll <poll>.                |",
				"| !results <poll>               - Displays the results of an ended poll.              |",
				"| !listpolls                    - Displays a list of polls and their options          |",
				"+-------------------------------------------------------------------------------------+"
		};
		return message;
	}
	
	public void handleEvent(Message event){
		String message = event.getMessage();
		String name = event.getMessager() == null ? m_botAction.getPlayerName(event.getPlayerID()) : event.getMessager();
		int messageType = event.getMessageType();
		
		if(messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE){
			if(opList.getAccessLevel(name) >= m_twscript.ACCESS_LEVEL || name.equalsIgnoreCase(m_botAction.getBotName()))
				handleERCommands(name, message);
			handlePubCommands(name, message);
		} else if(messageType == Message.ARENA_MESSAGE && message.startsWith("IP:"))
			parseIP(message);
	}
	
	public void handleERCommands(String name, String cmd){
		if(cmd.startsWith("!addpoll "))
			do_addPoll(name, cmd.substring(9));
		else if(cmd.startsWith("!addoption "))
			do_addOption(name, cmd.substring(11));
		else if(cmd.startsWith("!removepoll "))
			do_removePoll(name, cmd.substring(12));
		else if(cmd.startsWith("!poll "))
			do_poll(name, cmd.substring(6));
		else if(cmd.startsWith("!cancelpoll "))
			do_cancelPoll(name, cmd.substring(12));
	}
	
	public void handlePubCommands(String name, String cmd){
		if(cmd.equalsIgnoreCase("!help"))
			do_help(name);
		else if(Tools.isAllDigits(cmd) && polls.size() == 1)
			do_vote(name, polls.firstKey() + ":" + cmd);
		else if(cmd.startsWith("!vote "))
			do_vote(name, cmd.substring(6));
		else if(cmd.startsWith("!results "))
			do_results(name, cmd.substring(9));
		else if(cmd.equalsIgnoreCase("!listpolls"))
			do_listPolls(name);
	}
	
	public void do_help(String name){
		String[] help = {
				"+-------------------------------- Polls --------------------------------+",
				"| !vote <poll>:<vote>   - Casts your <vote> for the poll named <poll>.  |",
				"| !results <poll>       - Shows the results for an ended poll.          |",
				"| !listpolls            - Displays a list of active polls.              |",
				"| !help                 - Displays this help menu.                      |",
				"+-----------------------------------------------------------------------+"
		};
		m_botAction.smartPrivateMessageSpam(name, help);
	}
	
	public void do_vote(String name, String message){
		String[] msg = message.split(":");
		if(msg.length != 2){
			m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !vote favcolor:2");
			return;
		}
		if(!polls.containsKey(msg[0])){
			m_botAction.sendSmartPrivateMessage( name, "Poll '" + msg[0] + "' not found.");
			return;
		}
		if(!polls.get(msg[0]).isScheduled){
			m_botAction.sendSmartPrivateMessage( name, "Poll '" + msg[0] + "' is not active.");
			return;
		}
		waitingForAction.put(name, msg);
		m_botAction.sendUnfilteredPrivateMessage(name, "*info");
	}
	
	public void do_results(String name, String message){
		if(!polls.containsKey(message)){
			m_botAction.sendSmartPrivateMessage( name, "Poll '" + message + "' not found.");
			return;
		}
		String[] results = polls.get(message).getResults();
		if(results == null)
			m_botAction.sendSmartPrivateMessage( name, "Poll '" + message + "' has no results.");
		else{
			m_botAction.sendSmartPrivateMessage( name, "Results for '" + message + "':");
			m_botAction.smartPrivateMessageSpam(name, results);
		}
	}
	
	public void do_addPoll(String name, String message){
		String[] msgs = message.split(":");
		if(msgs.length > 2){
			m_botAction.sendSmartPrivateMessage( name, "Incorrect Usage. Example: !addpoll favcolor:What is your favorite color?");
			return;
		}
		if(polls.containsKey(msgs[0])){
			m_botAction.sendSmartPrivateMessage( name, "Poll '" + msgs[0] + "' already exists. Use !removepoll to remove it.");
			return;
		}
		polls.put(msgs[0], new CustomPoll(msgs[0], msgs[1]));
		m_botAction.sendSmartPrivateMessage( name, "Poll '" + msgs[0] + "' added."); 
		
	}
	
	public void do_addOption(String name, String message){
		int index = message.indexOf(":");
		String[] msgs = message.split(":");
		if(msgs.length < 3){
			m_botAction.sendSmartPrivateMessage( name, "Incorrect Usage. Example: !addoption favcolor:Green:!pub *arena The majority's favorite color is green!");
			return;
		}
		String pollName = message.substring(0, index);
		String option = message.substring(index + 1, message.indexOf(":", index + 1));
		String twsMsg = message.substring(message.indexOf(":", index + 1) + 1);
		if(!polls.containsKey(pollName)){
			m_botAction.sendSmartPrivateMessage( name, "Poll '" + pollName + "' does not exist. Use !addpoll to create it.");
			return;
		}
		polls.get(pollName).addOption(option, twsMsg);
		m_botAction.sendSmartPrivateMessage( name, "Option '" + option + "' added to poll '" + pollName + "'.");
	}

	public void do_removePoll(String name, String message){
		int index = message.indexOf(":");
        if (index == -1) {
        	if (polls.containsKey(message)) {
        		if(polls.get(message).isScheduled)
        			polls.get(message).cancelTask();
                polls.remove(message);
                m_botAction.sendSmartPrivateMessage(name, "Poll '" + message + "' removed.");
            } else
                m_botAction.sendSmartPrivateMessage(name, "Specified poll not found. Use !listpolls to see a list of registered polls.");
        	return;
        }
        String pollString = message.substring(0, index);
        CustomPoll poll;
        int actionIndex;
        try {
            actionIndex = Integer.parseInt(message.substring(index + 1));
        } catch (NumberFormatException e) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect usage. Example: !removepoll <Poll>:<Index of Message>");
            return;
        }
        if (!polls.containsKey(pollString)) {
            m_botAction.sendSmartPrivateMessage(name, "Poll '" + pollString + "' not found.");
            return;
        }
        poll = polls.get(pollString);
        if (poll.options.indexOf(actionIndex) != -1 && poll.messages.indexOf(actionIndex) != -1) {
            poll.removeOption(actionIndex);
            m_botAction.sendSmartPrivateMessage(name, "Option of '" + pollString + "' at index " + actionIndex + " removed.");
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Option of '" + pollString + "' at index " + actionIndex + " not found.");
        }
	}
	
	public void do_poll(String name, String message){
		int index = message.indexOf(" ");
        if (index == -1) {
        	m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !poll <Poll> <Days>:<Hours>:<Minutes>:<Seconds>");
        	return;
        }
        String poll = message.substring(0, index);
        if(!polls.containsKey(poll)){
        	m_botAction.sendSmartPrivateMessage( name, "Poll '" + poll + "' not found.");
        	return;
        }
        if(polls.get(poll).isScheduled){
        	m_botAction.sendSmartPrivateMessage( name, "Poll '" + poll + "' is already scheduled. Use !canceltimer <timer> to cancel.");
        	return;
        }
        String[] timeString = message.substring(index + 1).split(":");
        int[] time = new int[timeString.length];
        try{
        	for(int i=0;i<timeString.length;i++)
        		time[i] = Integer.parseInt(timeString[i]);
        }catch(NumberFormatException e){
        	m_botAction.sendSmartPrivateMessage( name, "Incorrect Usage. Example: !poll favcolor 0:0:2:0");
        	return;
        }
        if(time.length == 4)
        	polls.get(poll).schedule(CodeCompiler.getTimeInMillis(time));
        else if(time.length == 6){
        	GregorianCalendar cal = new GregorianCalendar();
            cal.set( time[2], time[0]-1, time[1], time[3], time[4], time[5]);
            polls.get(poll).schedule(cal);
        }else
        	m_botAction.sendSmartPrivateMessage( name, "Incorrect Usage. Example: !poll favcolor 0:0:2:0");	
	}

	public void do_cancelPoll(String name, String message){
		if(!polls.containsKey(message)){
        	m_botAction.sendSmartPrivateMessage( name, "Poll '" + message + "' not found.");
        	return;
        }
        if(!polls.get(message).isScheduled){
        	m_botAction.sendSmartPrivateMessage( name, "Poll '" + message + "' is not currently scheduled.");
        	return;
        }
        polls.get(message).cancelTask();
        m_botAction.sendSmartPrivateMessage( name, "Poll '" + message + "' has been cancelled.");
	}
	
	public void do_listPolls(String name){
		if (polls.size() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "There are no polls to list.");
            return;
        }
		if(!opList.isER(name)){
			Iterator<CustomPoll> it = polls.values().iterator();
			while( it.hasNext() ){
				CustomPoll p = it.next();
				if(p.isScheduled || p.getResults() != null)
					m_botAction.smartPrivateMessageSpam(name, p.toStringArray());
			}
			return;
		}
        Iterator<CustomPoll> it = polls.values().iterator();
        m_botAction.sendSmartPrivateMessage(name, "==================== Custom Polls ====================");
        while (it.hasNext()) {
            CustomPoll p = it.next();
            m_botAction.sendSmartPrivateMessage(name, "| Poll: " + p.pollName);
            m_botAction.sendSmartPrivateMessage(name, "| - " + p.getScheduleString());
            Iterator<String> i = p.options.iterator();
            while (i.hasNext()) {
                String msg = i.next();
                m_botAction.sendSmartPrivateMessage(name, "|  " + p.options.indexOf(msg) + ") " + msg + " - " + p.votes.get(p.options.indexOf(msg)) + " votes.");
                m_botAction.sendSmartPrivateMessage(name, "|         - " + p.messages.get(p.options.indexOf(msg)));
            }
        }
        m_botAction.sendSmartPrivateMessage(name, "======================================================");
	}
	
	public void requestEvents(ModuleEventRequester req){
		req.request(this, EventRequester.PLAYER_ENTERED);
	}
	
	public void handleEvent(PlayerEntered event){
		String name = m_botAction.getPlayerName(event.getPlayerID());
		if(name == null)return;
        Iterator<CustomPoll> it = polls.values().iterator();
		while( it.hasNext() ){
			CustomPoll poll = it.next();
			if(poll.isScheduled || poll.getResults() != null)
				m_botAction.smartPrivateMessageSpam(name, poll.toStringArray());
		}
		return;
	}
	
private class CustomPoll {
	private String pollName, pollQuestion;
	private ArrayList<String> options = new ArrayList<String>();
	private ArrayList<String> messages = new ArrayList<String>();
	private ArrayList<Integer> votes = new ArrayList<Integer>();
	private ArrayList<String> results = new ArrayList<String>();
	private TreeMap<NameIPMID, Integer> playerVotes = new TreeMap<NameIPMID, Integer>();
	private long start, millis;
	private boolean isScheduled;
	private TimerTask task;
	private GregorianCalendar cal;
	
	private CustomPoll(String name, String question){
		pollName = name;
		pollQuestion = question;
	}
	
	private void updateVotes(){
		for(int i=0;i<votes.size();i++)
			votes.set(i, 0);
		Iterator<Integer> it = playerVotes.values().iterator();
		while( it.hasNext() ){
			int x = it.next();
			votes.set(x, votes.get(x) + 1);
		}
	}
	
	private void countVotes(){
		int max = 0, index = 0;
		updateVotes();
		ArrayList<Integer> winners = new ArrayList<Integer>();		
		Iterator<Integer> i = votes.iterator();
		while( i.hasNext() ){
			int x = i.next();
			if( x > max ){
				max = x;
				winners.clear();
				winners.add(index);
			}
			else if( x == max && max != 0) winners.add(index);
			index++;
		}
		results.clear();
		if(winners.isEmpty()){
			results.add("No votes were collected. The poll is declared void.");
			m_botAction.sendArenaMessage("No votes were collected. The poll is declared void.");
			return;
		}
		results.add("+------------------------ Results ------------------------+");
		results.add("| Poll: " + pollQuestion);
		i = votes.iterator();
		index = 0;
		while( i.hasNext() ){
			results.add("| " + index + ") " + options.get(index) + " ( " + i.next() + " vote(s) )");
			index++;
		}
		results.add("+---------------------------------------------------------+");
		if(winners.size() > 1)
			results.add("There was a tie between votes. No action taken.");
		else{
			results.add("Winner: " + winners.get(0) + " - " + options.get(winners.get(0)));
			CodeCompiler.handleTWScript(m_botAction, messages.get(winners.get(0)), m_botAction.getPlayer(m_botAction.getBotName()), m_twscript, m_twscript.ACCESS_LEVEL);
		}
		m_botAction.arenaMessageSpam(results.toArray(new String[results.size()]));
	}
	
	private String[] getResults(){
		if(results.isEmpty())return null;
		else return results.toArray(new String[results.size()]);
	}
	
	private void addOption(String option, String message){
		options.add(option);
		messages.add(message);
		votes.add(0);
	}
	
	private void removeOption(int index){
		options.remove(index);
		messages.remove(index);
		votes.remove(index);
	}

	private void cancelTask(){
		if(!isScheduled)return;
		isScheduled = false;
		if(cal != null)cal = null;
		task.cancel();
	}
	
	private void schedule(long startTime){
		if(isScheduled)return;
		else isScheduled = true;
		task = new TimerTask(){
			public void run(){
				isScheduled = false;
				m_botAction.sendArenaMessage("Poll '" + pollName + "' has ended. Results:");
				polls.get(pollName).countVotes();				
				
			}
		};
		results.clear();
		m_botAction.scheduleTask(task, startTime);
		setScheduleString(startTime, (new Date()).getTime());
		m_botAction.arenaMessageSpam(this.toStringArray());
	}
	
	public void setScheduleString(long start, long millis){
		this.start = start;
		this.millis = millis;
	}
	
	private void schedule(GregorianCalendar cal){
		if(isScheduled)return;
		else isScheduled = true;
		task = new TimerTask(){
			public void run(){
				isScheduled = false;
				m_botAction.sendArenaMessage("Poll '" + pollName + "' has ended. Results:");
				polls.get(pollName).countVotes();			
			}
		};
		results.clear();
		m_botAction.scheduleTask(task, cal.getTimeInMillis() - System.currentTimeMillis());
		m_botAction.arenaMessageSpam(this.toStringArray());
		this.cal = cal;

	}
	
	private String getScheduleString(){
		if(getResults() != null) return "The poll has ended. Use '!results " + pollName + "' to see the results.";
		if(!isScheduled) return "Not currently scheduled.";
		long timeLeftInMillis;
		long[] time;
		timeLeftInMillis = start - ((new Date()).getTime() - millis);
		time = CodeCompiler.getTimeInFormat(timeLeftInMillis, true);
		if(cal != null)
			return "Poll ends on " + SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(cal.getTime()) + " at " + SimpleDateFormat.getTimeInstance().format(cal.getTime()) + " (" + TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT) + ")";
		return "Poll ends in " + CodeCompiler.getTimeString(time, true);
	}
	
	private String[] toStringArray(){
		ArrayList<String> s = new ArrayList<String>();
		Iterator<String> i = options.iterator();
		s.add("============================ " + pollName + " ============================");
		s.add("| Question: " + pollQuestion);
		s.add("| Options:");
		while( i.hasNext() ){
			String op = i.next();
			s.add("|  " + options.indexOf(op) + ") " + op);
		}
		s.add("| " + getScheduleString());
		s.add("=============================" + toLine(pollName) + "=============================");
		if(polls.size() > 1)
			s.add("PM me with !vote " + pollName + ":<number next to your answer> to vote -" + m_botAction.getBotName());
		else if(polls.size() == 1)
			s.add("PM me with the number next to your answer to vote. -" + m_botAction.getBotName());
		return s.toArray(new String[s.size()]);
	}
	
}

private class NameIPMID implements Comparable<NameIPMID>{
	private String name, IP, MID;
	
	private NameIPMID(String name, String IP, String MID){
		this.name = name;
		this.IP = IP;
		this.MID = MID;
	}
	
	public int compareTo(NameIPMID nim){
		if(nim.name.equalsIgnoreCase(this.name) ||
		   nim.IP.equals(this.IP)               ||
		   nim.MID.equals(this.MID))
		return 1;
		else return -1;
	}
}

	public void parseIP(String message){
		NameIPMID newPlayer;
		String name, IP, MID;
		String[] data;
		int nameIndex, MIDIndex, vote;
		nameIndex = message.indexOf("TypedName:");
		MIDIndex = message.indexOf("MachineId:");
		if(nameIndex == -1 || MIDIndex == -1)return;
		name = message.substring(nameIndex+10,message.indexOf("Demo:")-2);
		IP = message.substring(3, message.indexOf(" "));
		MID = message.substring(MIDIndex+10);
		if(waitingForAction.containsKey(name))
			data = waitingForAction.remove(name);
		else return;
		try{
			vote = Integer.parseInt(data[1]);
		}catch(NumberFormatException e){
			m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Please try again.");
			return;
		}
		newPlayer = new NameIPMID(name, IP, MID);
		Iterator<CustomPoll> i = polls.values().iterator();
		while( i.hasNext() ){
			CustomPoll poll = i.next();
			if(poll.pollName.equalsIgnoreCase(data[0])){
				if(poll.votes.size() <= vote || vote < 0){
					m_botAction.sendSmartPrivateMessage( name, "The option '" + vote + "' is not valid.");
					return;
				}
				Iterator<NameIPMID> it = poll.playerVotes.keySet().iterator();
				while( it.hasNext() ){
					NameIPMID nim = it.next();
					if(nim.compareTo(newPlayer) > 0){
						if(poll.playerVotes.get(nim) == vote)
							m_botAction.sendSmartPrivateMessage( name, "You have already voted.");
						else
							m_botAction.sendSmartPrivateMessage( name, "Your vote has been changed.");
						it.remove();
						poll.playerVotes.put(newPlayer, vote);
						poll.updateVotes();
						return;
					}
				}
				poll.playerVotes.put(newPlayer, vote);
				poll.updateVotes();
				m_botAction.sendSmartPrivateMessage( name, "Your vote has been counted.");
				return;
			}
		}
		m_botAction.sendSmartPrivateMessage( name, "The poll '" + data[0] + "' does not exist.");	
	}
	
	public String toLine(String msg){
		String s = "";
		for(int i=0;i<msg.length();i++){
			s += "=";
		}
		return s;
	}
	
	public void cancel(){
		for(CustomPoll poll:polls.values()){
			poll.cancelTask();
		}
		polls.clear();
	}
}