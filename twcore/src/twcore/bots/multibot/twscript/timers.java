package twcore.bots.multibot.twscript;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Date;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import twcore.bots.MultiUtil;
import twcore.bots.TWScript;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.OperatorList;
import twcore.core.util.Tools;

/**
 * @author milosh
 */
public class timers extends MultiUtil {
	
	public OperatorList opList;
	public TWScript m_twscript;
	
	private TreeMap<String, CustomTimer> timers;
	
	public void init(){
		opList = m_botAction.getOperatorList();
		timers = new TreeMap<String, CustomTimer>();
	}
	
	public void initializeTWScript(TWScript tws){
		m_twscript = tws;
	}
	
	public String[] getHelpMessages(){
		String[] message = {
				"+--------------------------------------- Timers --------------------------------------+",
				"| !addtimer <timer> <message>     - Adds a TWScript <message> to a timer, <name>.     |",
				"|                                 - If the timer does not exist it is created.        |",
				"| !removetimer <timer>            - Removes the timer, <name>.                        |",
				"| !removetimer <timer> <index>    - Removes message at <index> from <timer>           |",
				"| !sched <timer> d:h:m:s          - Schedules the specified timer to run one time     |",
				"|                                 - at days:hours:minutes:seconds from now.           |",
				"| !sched <timer> d:h:m:s d:h:m:s  - Schedules the specified timer to start running    |",
				"|                                 - to start at dhms and repeate every dhms.          |",
				"| !calendar <t> MM:DD:YYYY:h:m:s  - Schedules timer <t> to occur on a specific date   |",
				"|                                 - and time. For example, 04:28:2009:10:1:0 would    |",
				"|                                 - occur on April 28, 2009 at 10:01AM EST            |",
				"| !canceltimer <timer>            - Cancels <timer> if scheduled.                     |",
				"| !listtimers                     - Displays a list of timers and their messages.     |",
				"+-------------------------------------------------------------------------------------+"
		};
		return message;
	}
	
	public void handleEvent(Message event){
		String message = event.getMessage();
		String name = event.getMessager() == null ? m_botAction.getPlayerName(event.getPlayerID()) : event.getMessager();
		int messageType = event.getMessageType();
		
		if(messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE)
			if(opList.isER(name))
				handleCommands(name, message);
	}
	
	public void handleCommands(String name, String cmd){
		if (cmd.startsWith("!addtimer "))
            do_addTimer(name, cmd.substring(10));
        if (cmd.startsWith("!removetimer "))
            do_removeTimer(name, cmd.substring(13));
        if (cmd.startsWith("!sched "))
        	do_sched(name, cmd.substring(7));
        if (cmd.startsWith("!calendar "))
        	do_calendar(name, cmd.substring(10));
        if (cmd.startsWith("!canceltimer "))
        	do_cancelTimer(name, cmd.substring(13));
        if (cmd.equalsIgnoreCase("!listtimers"))
            do_listTimers(name);
	}
	
	public void do_addTimer(String name, String message){
		int index = message.indexOf(" ");
        if (index == -1) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect usage. Example: !addtimer birthday I was born on this day!");
            return;
        }
        String timer = message.substring(0, index);
        String msg = message.substring(index + 1);
        if (!timers.containsKey(timer))
            timers.put(timer, new CustomTimer(timer));
        timers.get(timer).addMessage(msg);
        m_botAction.sendSmartPrivateMessage(name, "Timer added.");
	}
	
	public void do_removeTimer(String name, String message){
		int index = message.indexOf(" ");
        if (index == -1) {
        	if (timers.containsKey(message)) {
        		if(timers.get(message).isScheduled)
        			timers.get(message).cancelTask();
                timers.remove(message);
                m_botAction.sendSmartPrivateMessage(name, "Timer '" + message + "' removed.");
            } else
                m_botAction.sendSmartPrivateMessage(name, "Specified timer not found. Use !listtimer to see a list of registered timers.");
        	return;
        }
        String timer = message.substring(0, index);
        int actionIndex;
        try {
            actionIndex = Integer.parseInt(message.substring(index + 1));
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect usage. Example: !removetimer <Timer> <Index of Message>");
            return;
        }
        if (!timers.containsKey(timer)) {
            m_botAction.sendSmartPrivateMessage(name, "Timer '" + timer + "' not found.");
            return;
        }
        if (timers.get(timer).hasIndex(actionIndex)) {
            timers.get(timer).removeMessage(actionIndex);
            m_botAction.sendSmartPrivateMessage(name, "Message of " + timer + " at index " + actionIndex + " removed.");
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Message of " + timer + " at index " + actionIndex + " not found.");
        }
	}
	
	public void do_sched(String name, String message){
		int index = message.indexOf(" ");
        if (index == -1) {
        	m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !sched <Timer> <Days>:<Hours>:<Minutes>:<Seconds>");
        	return;
        }
        String timer = message.substring(0, index);
        if(!timers.containsKey(timer)){
        	m_botAction.sendSmartPrivateMessage( name, "Timer '" + timer + "' not found.");
        	return;
        }
        if(timers.get(timer).isScheduled){
        	m_botAction.sendSmartPrivateMessage( name, "Timer '" + timer + "' is already scheduled. Use !canceltimer <timer> to cancel.");
        	return;
        }
        long[] time = new long[8];
        String[] msgs;
        if(message.substring(index + 1).indexOf(" ") == -1)
        	msgs = message.substring(index + 1).split(":");
        else
        	msgs = message.substring(index + 1).replaceFirst(" ", ":").split(":");
        try {
        	if(msgs.length != 4 && msgs.length != 8)
        		throw new IllegalArgumentException();
        	for(int i=0;i<msgs.length;i++)
        		time[i] = Long.parseLong(msgs[i]);
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect usage. Example: !sched <Timer> <Days>:<Hours>:<Minutes>:<Seconds>");
            return;
        }
        long[] start = new long[4];
        long[] repeat = new long[4];
        for(int i=0;i<start.length;i++)
        	start[i] = time[i];
        for(int i=4;i<time.length;i++)
        	repeat[(i-4)] = time[i];
        if(getTimeInMillis(repeat) > 0){
        	timers.get(timer).schedule(getTimeInMillis(start), getTimeInMillis(repeat));
        	m_botAction.sendSmartPrivateMessage( name, "Timer '" + timer + "' " + timers.get(timer).getScheduleString());
        } else{
        	timers.get(timer).schedule(getTimeInMillis(start), -1);
        	m_botAction.sendSmartPrivateMessage( name, "Timer '" + timer + "' " + timers.get(timer).getScheduleString());
        }
        
	}
	
	public void do_calendar(String name, String message){
		int index = message.indexOf(" ");
        if (index == -1) {
        	m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !calendar <Timer> <Month>:<Day>:<Year>:<Hours>:<Minutes>:<Seconds>");
        	return;
        }
        String timer = message.substring(0, index);
        if(!timers.containsKey(timer)){
        	m_botAction.sendSmartPrivateMessage( name, "Timer '" + timer + "' not found.");
        	return;
        }
        if(timers.get(timer).isScheduled){
        	m_botAction.sendSmartPrivateMessage( name, "Timer '" + timer + "' is already scheduled. Use !canceltimer <timer> to cancel.");
        	return;
        }
        int[] time = new int[6];
        String[] msgs;
        msgs = message.substring(index + 1).split(":");
        try {
        	if(msgs.length != 6)
        		throw new IllegalArgumentException();
        	for(int i=0;i<msgs.length;i++)
        		time[i] = Integer.parseInt(msgs[i]);
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect usage. Example: !sched <Timer> <Days>:<Hours>:<Minutes>:<Seconds>");
            return;
        }
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(time[2], time[0]-1, time[1], time[3], time[4], time[5]);
        timers.get(timer).schedule(cal);
    	m_botAction.sendSmartPrivateMessage( name, "Timer '" + timer + "' " + timers.get(timer).getScheduleString());
        
	}
	
	public void do_cancelTimer(String name, String timer){
		if(!timers.containsKey(timer)){
        	m_botAction.sendSmartPrivateMessage( name, "Timer '" + timer + "' not found.");
        	return;
        }
        if(!timers.get(timer).isScheduled){
        	m_botAction.sendSmartPrivateMessage( name, "Timer '" + timer + "' is not currently scheduled.");
        	return;
        }
        timers.get(timer).cancelTask();
        m_botAction.sendSmartPrivateMessage( name, "Timer '" + timer + "' has been cancelled.");
	}
	
	public void do_listTimers(String name){
		if (timers.size() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "There are no timers to list.");
            return;
        }
        Iterator<CustomTimer> it = timers.values().iterator();
        m_botAction.sendSmartPrivateMessage(name, "========== Custom Timers ==========");
        while (it.hasNext()) {
            CustomTimer t = it.next();
            m_botAction.sendSmartPrivateMessage(name, "| Timer: " + t.name);
            m_botAction.sendSmartPrivateMessage(name, "| - " + t.getScheduleString());
            Iterator<String> i = t.getMessages().iterator();
            while (i.hasNext()) {
                String msg = i.next();
                m_botAction.sendSmartPrivateMessage(name, "|  " + t.getMessages().indexOf(msg) + ") " + msg);
            }
        }
        m_botAction.sendSmartPrivateMessage(name, "===================================");
	}
	
private class CustomTimer{
	private String name;
	private ArrayList<String> messages;
	private boolean isScheduled = false;
	private TimerTask task;
	private long st, rep, millis;
	private GregorianCalendar cal;
	
	private CustomTimer(String name){
		this.name = name;
		messages = new ArrayList<String>();
	}
	
	private void addMessage(String message){
		messages.add(message);
	}
	
	private void removeMessage(int index){
		messages.remove(index);
	}
	
	private boolean hasIndex(int index){
		return messages.get(index) != null;
	}
	
	private ArrayList<String> getMessages(){
		return messages;
	}
	
	private void cancelTask(){
		if(!isScheduled)return;
		else isScheduled = false;
		if(cal != null)cal = null;
		task.cancel();
	}
	
	private void schedule(long startTime, long repeatTime){
		if(isScheduled)return;
		else isScheduled = true;
		task = new TimerTask(){
			public void run(){
		        Player p = m_botAction.getPlayer(m_botAction.getBotName());
		        Iterator<String> msgs = messages.iterator();
		        while( msgs.hasNext() )
		        	CodeCompiler.handleTWScript(m_botAction, msgs.next(), p, m_twscript.variables, m_twscript.ACCESS_LEVEL);
		        if(rep < 0)
		        	timers.get(name).cancelTask();
			}
		};
		if(repeatTime < 0){
			m_botAction.scheduleTask(task, startTime);
			setScheduleString(startTime, -1, (new Date()).getTime());
		}
		else {
			m_botAction.scheduleTaskAtFixedRate(task, startTime, repeatTime);
			setScheduleString(startTime, repeatTime, (new Date()).getTime());
		}
	}
	
	private void schedule(GregorianCalendar cal){
		if(isScheduled)return;
		else isScheduled = true;
		task = new TimerTask(){
			public void run(){
				Iterator<Player> i = m_botAction.getPlayerIterator();
		        while( i.hasNext() ){
		        	Player p = i.next();
		        	Iterator<String> msgs = messages.iterator();
		        	while( msgs.hasNext() )
		        		CodeCompiler.handleTWScript(m_botAction, msgs.next(), p, m_twscript.variables, m_twscript.ACCESS_LEVEL);
		        }
		        if(rep < 0)
		        	timers.get(name).cancelTask();
			}
		};
		m_botAction.scheduleTask(task, cal.getTimeInMillis() - System.currentTimeMillis());
		this.cal = cal;

	}
	
	public void setScheduleString(long st, long rep, long millis){
		this.st = st;
		this.rep = rep;
		this.millis = millis;
	}

	public String getScheduleString(){
		if(!isScheduled) return "Timer '" + name + "' is not currently scheduled.";
		long timeLeftInMillis;
		long[] time;
		if(rep > 0){
			timeLeftInMillis = st - ((new Date()).getTime() - millis);
			if(timeLeftInMillis < 0){
				timeLeftInMillis *= -1;
				timeLeftInMillis %= rep;
				timeLeftInMillis = rep - timeLeftInMillis;
			}
		}else
			timeLeftInMillis = st - ((new Date()).getTime() - millis);
		time = getTimeInFormat(timeLeftInMillis, true);
		if(cal != null){
			return "scheduled to fire on " + SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(cal.getTime()) + " at " + SimpleDateFormat.getTimeInstance().format(cal.getTime()) + " (" + TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT) + ")";
		}
		return "scheduled to fire in " + getTimeString(time, false);
		
	}
}

	public long getTimeInMillis(long[] time){
		long sum = 0;
		for(int i=0;i<time.length;i++){
			switch(i){
			case 0:sum += time[i] * Tools.TimeInMillis.DAY;break;
			case 1:sum += time[i] * Tools.TimeInMillis.HOUR;break;
			case 2:sum += time[i] * Tools.TimeInMillis.MINUTE;break;
			case 3:sum += time[i] * Tools.TimeInMillis.SECOND;break;
			}
		}
		return sum;
	}
	public long[] getTimeInFormat(long millis, boolean includeMilli){
		long[] format = new long[5];
		while(millis > Tools.TimeInMillis.DAY){
			millis -= Tools.TimeInMillis.DAY;
			format[0]++;
		}
		while(millis > Tools.TimeInMillis.HOUR){
			millis -= Tools.TimeInMillis.HOUR;
			format[1]++;
		}
		while(millis > Tools.TimeInMillis.MINUTE){
			millis -= Tools.TimeInMillis.MINUTE;
			format[2]++;
		}
		while(millis > Tools.TimeInMillis.SECOND){
			millis -= Tools.TimeInMillis.SECOND;
			format[3]++;
		}
			if(includeMilli){
			while(millis > 0){
				millis--; 
				format[4]++;
			}
		}		
		return format;
	}
	public String getTimeString(long[] time, boolean verbose){
		if(time.length == 4){
		if(verbose)
			return time[0] + " days, " + time[1] + " hours, " + time[2] + " minutes, and " + time[3] + " seconds";
		else
			return time[0] + "d:" + time[1] + "h:" + time[2] + "m:" + time[3] + "s";
		} else
			return time[0] + "d:" + time[1] + "h:" + time[2] + "m:" + time[3] + "s:" + time[4] + "ms";
	}

	public void requestEvents(ModuleEventRequester req){}
	public void cancel(){
	    for(CustomTimer timer:timers.values()) {
	        timer.cancelTask();
	    }
	    timers.clear();
	}
}