package twcore.bots.multibot.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

public class utildoors extends MultiUtil {
	private int doorState;
	private boolean boolTimer = false;
	private DoorTimerTask m_doortimer;
	private int m_taskNumber = 0;
	
	
	/*** Initializes the module. */
	public void init() {
	}
	  
	/*** Does not need to request events other than Message (default). */
	public void requestEvents( ModuleEventRequester modEventReq ) {
	}

	public String[] getHelpMessages() {
	    String[] message =
	    {
	        "!DoorMode <Door Value>           -- Sets the doors to a certain value.",
	        "!DoorOn Door# Door# ... Door#   -- Closes all doors specified (can use !DoorOn All)",
	        "!DoorOff Door# Door# ... Door#  -- Opens all doors specified. (can use !DoorOff All)",
	        "!AddDoor <On> <Off> Door# Door# -- Adds recurring door task",
	        "   Doors will be closed for <On> seconds and then opened for <Off> secs.",
	        "!DoorList                       -- Shows all of the door tasks.",
	        "!DoorDel Task#                  -- Removes a door task specified by <Task Number>.",
	        "!DoorsOff                       -- Clears the door tasks."
	    };
	    return message;
	}

	public void doDoorCmd(String sender, String argString) {
        if (Tools.isBinary(argString) && argString.length() == 8){
            m_botAction.setDoors(argString);
            m_botAction.sendSmartPrivateMessage(sender, "Doors set to " + argString + ".");
        } else {
            try {
                int doorValue = Integer.parseInt(argString);
                if (doorValue >= -2 && doorValue <= 255) {
                    m_botAction.setDoors(doorValue);
                    m_botAction.sendSmartPrivateMessage(sender, "Doors set to " + doorValue + ".");             
                } else {
                    m_botAction.sendSmartPrivateMessage(sender,"Please use the following format: !DoorMode <Door Value>.  Door Value must be -2 to 255 or an 8-digit binary string (eg: 10110111)");
                }
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(sender,"Use !DoorMode <Door Value>.  Door Value must be -2 to 255 or an 8-digit binary string (eg: 10110111)");
            }
        }
	}
	
	public int closeDoor(int doorState, int doorNumber) {
	    return doorState | (int) Math.pow(2, (doorNumber - 1));
	}
	
	public int openDoor(int doorState, int doorNumber) {
	    return doorState & ~((int) Math.pow(2, (doorNumber - 1)));
	}
	
	public void doDoorOnCmd(String sender, String argString) {
		if (argString.equalsIgnoreCase("all")) {
			m_botAction.setDoors(255);
			m_botAction.sendPrivateMessage(sender, "All doors closed.");
	    } else {
	    	// allow doors to be separated by comma and/or space
	    	argString = argString.replaceAll("(,|, |  )"," ");
	    	String doorNums[] = argString.split(" ");
	    	int x, doorNum;
	    	boolean boolErrors = false;
	
	   		int newDoorState = doorState;
	   		StringBuffer message = new StringBuffer("Doors Closed: ");
	   		for (x = 0; x < doorNums.length; x++) {
	   			doorNum = 0;
	   			try {
	   				doorNum = Integer.parseInt(doorNums[x]);
	   			} catch (NumberFormatException e) {}
	   			if (doorNum < 1 || doorNum > 8) {boolErrors = true; break;}
	  			newDoorState = closeDoor(newDoorState, doorNum);
	   			message.append(doorNum + " ");
	   		}
	   		if (boolErrors) {
	   			m_botAction.sendSmartPrivateMessage(sender,"Please use the following format: !DoorOn <Door 1>, <Door 2>, ... <Door n>");
	   		} else {
	   			m_botAction.setDoors(newDoorState);
	   			m_botAction.sendSmartPrivateMessage(sender, message.toString());
	   		}
	    }
	}
	
	public void doDoorOffCmd(String sender, String argString) {
	    if(argString.equalsIgnoreCase("all")) {
	      m_botAction.setDoors(0);
	      m_botAction.sendPrivateMessage(sender, "All doors opened.");
	    } else {
	    	argString = argString.replaceAll("(,|, |  )"," ");
	    	String doorNums[] = argString.split(" ");
	    	int x, doorNum;
	    	boolean boolErrors = false;
		    	
	   		int newDoorState = doorState;
	   		StringBuffer message = new StringBuffer("Doors Opened: ");
	   		for (x = 0; x < doorNums.length; x++) {
	   			doorNum = 0;
	   			try {
	   				doorNum = Integer.parseInt(doorNums[x]);
	   			} catch (NumberFormatException e) {}
	   			if (doorNum < 1 || doorNum > 8) {boolErrors = true; continue;}
	  			newDoorState = openDoor(newDoorState, doorNum);
	   			message.append(doorNum + " ");
	   		}
	   		if (boolErrors) {
	   			m_botAction.sendSmartPrivateMessage(sender,"Please use the following format: !DoorOn <Door 1>, <Door 2>, ... <Door n>");
	   		} else {
	   			m_botAction.setDoors(newDoorState);
	   			m_botAction.sendSmartPrivateMessage(sender, message.toString());
	   		}
	    }
	}

	public void doAddCmd(String sender,String argString) {
    	argString = argString.replaceAll("(,|, |  )"," ");
    	String args[] = argString.split(" ");
    	int x, doorNum, doorOn, doorOff;
		boolean boolErrors = false;
		int doors[];

		try {
			doorOn = Integer.parseInt(args[0]);
			doorOff = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			m_botAction.sendSmartPrivateMessage(sender,"Please use the following format: !AddDoor <On Seconds> <Off Seconds> <Door 1> <Door 2> ... <Door n>");
			return;
		}
		
		// doorOn and doorOff seconds must be > 1
		if (doorOn < 1 || doorOff < 1) {
			m_botAction.sendSmartPrivateMessage(sender,"Please use the following format: !AddDoor <On Seconds> <Off Seconds> <Door 1> <Door 2> ... <Door n>.  On/Off Seconds must be 1 or more.");
			return;
		}
		doors = new int[args.length-2];
		StringBuffer message = new StringBuffer("Door(s) added to the task list: ");
		for (x = 2; x < args.length; x++) {
			doorNum = 0;
			try {
				doorNum = Integer.parseInt(args[x]);
			} catch (NumberFormatException e) {}
			if (doorNum < 1 || doorNum > 8) {boolErrors = true; break;}
			message.append(doorNum + " ");
			doors[x-2] = doorNum;
		}
		
		if (boolErrors) {
			m_botAction.sendSmartPrivateMessage(sender,"Please use the following format: !AddDoor <On Seconds> <Off Seconds> <Door 1> <Door 2> ... <Door n>");
			return;
		}
		message.append(" [" + doorOn + "s on, " + doorOff + "s off]");
		m_botAction.sendSmartPrivateMessage(sender, message.toString());

		if (boolTimer == false) {
			m_doortimer = new DoorTimerTask();
			m_botAction.scheduleTaskAtFixedRate(m_doortimer, 1000, 1000);
			boolTimer = true;
		}
		m_doortimer.add(doorOn, doorOff, doors);
	}

	public void doListCmd(String sender) {
		if (boolTimer) {
			m_doortimer.list(sender);
		} else {
			m_botAction.sendSmartPrivateMessage(sender,"There are currently no door tasks.");
		}
	}
	
	public void doDelCmd(String sender, String argString) {
		int taskNum = 0;

		try {
			taskNum = Integer.parseInt(argString);
		} catch (NumberFormatException e) {
			m_botAction.sendSmartPrivateMessage(sender,"Please use the following format: !DoorDel <Task#>");
			return;
		}

		if (boolTimer) {
			if (m_doortimer.exists(taskNum)) {
				m_doortimer.remove(taskNum);
				m_botAction.sendSmartPrivateMessage(sender,"Task Number " + taskNum + " has been removed.");
			} else {
				m_botAction.sendSmartPrivateMessage(sender,"Invalid Task Number -- Please use the following format: !DoorDel <Task#>.  use !DoorList to see a list of tasks.");
			}
		} else {
			m_botAction.sendSmartPrivateMessage(sender,"Invalid Task Number -- Please use the following format: !DoorDel <Task#>.  use !DoorList to see a list of tasks.");
		}
	}
	
	public void doClearCmd(String sender) {
		if (boolTimer) {
			m_doortimer.removeAll();
			m_botAction.sendSmartPrivateMessage(sender,"All door tasks removed.");
		} else {
			m_botAction.sendSmartPrivateMessage(sender,"There are no tasks running.");
		}
	}

	public void handleCommand(String sender, String command) {
		updateDoorState();
		if (command.startsWith("!doormode ")) {doDoorCmd(sender, command.substring(10));}
		if (command.startsWith("!dooron ")) {doDoorOnCmd(sender, command.substring(8));}
		if (command.startsWith("!dooroff ")) {doDoorOffCmd(sender, command.substring(9));}
		if (command.startsWith("!adddoor ")) {doAddCmd(sender, command.substring(9));}
		if (command.startsWith("!doorlist")) {doListCmd(sender);}
		if (command.startsWith("!doordel ")) {doDelCmd(sender, command.substring(9));}
		if (command.startsWith("!doorsoff")) {doClearCmd(sender);}
	}
	
	public void updateDoorState() {
	    m_botAction.sendUnfilteredPublicMessage("?get door:doormode");
	}
	
	public void handleArenaMessage(String message) {		
	    if(message.startsWith("door:doormode=")) {
	      String doorString = message.substring(14);
	      doorState = Integer.parseInt(doorString);
	    }
	}

	public void handleEvent(Message event) {
	    int senderID = event.getPlayerID();
	    String sender = m_botAction.getPlayerName(senderID);	    
	    String message = event.getMessage().toLowerCase().trim();
	    int messageType = event.getMessageType();
	    if(messageType == Message.PRIVATE_MESSAGE && m_opList.isER(sender))
	      handleCommand(sender, message);
	    else if(messageType == Message.ARENA_MESSAGE)
	    	handleArenaMessage(message);
	    	
	}
	
	public void cancel() {}

	private class DoorTimerTask extends TimerTask {
		private DoorTask curTask;
        private Iterator<Integer> it;
	    private HashMap<Integer, DoorTask> doorTaskList = new HashMap<Integer, DoorTask>();

	    public void add(int onTime, int offTime, int[] doors) {
	    	m_taskNumber++;
	    	doorTaskList.put(m_taskNumber, new DoorTask(m_taskNumber,onTime,offTime,doors));
	    }

	    public boolean exists(int taskNumber) {
	    	if (doorTaskList.containsKey(taskNumber)) {return true;} else {return false;}
	    }
	
	    public void remove(int taskNumber) {
	    	doorTaskList.remove(taskNumber);
	    	if (doorTaskList.size() == 0 && boolTimer) {
	    		boolTimer = false; m_doortimer.cancel();
	    	}
	    }

	    public void removeAll() {
	    	if (boolTimer == true) {boolTimer = false; m_doortimer.cancel();}
	    	doorTaskList.clear();
	    }

	    public void list(String sender) {
	    	it = doorTaskList.keySet().iterator();
    		m_botAction.sendSmartPrivateMessage(sender,"Door Task List:");
    		m_botAction.sendSmartPrivateMessage(sender,"Task#  onTime  offTime  door(s)");

            while (it.hasNext()) {
	    		curTask = doorTaskList.get(it.next());
	    		m_botAction.sendSmartPrivateMessage(sender,
	    				Tools.formatString("  "+curTask.taskNum(),7) +
	    				Tools.formatString("  "+curTask.onTime(),8) +
	    				Tools.formatString("  "+curTask.offTime(),9) +
	    				curTask.doorList()
	    		);
	    	}	
	    }

		public void run() {
	   		int newDoorState = doorState;
			it = doorTaskList.keySet().iterator();
			while (it.hasNext()) {
            	curTask = doorTaskList.get(it.next());
            	if (curTask.tick() == 0) {
            		newDoorState = curTask.toggle(newDoorState);
            	}
            }
			if (doorState != newDoorState) {m_botAction.setDoors(newDoorState);}
		}
	}
	
	public class DoorTask {
		int taskNum, onTime, offTime, trigger, x;
		int[] doors;
		boolean isOn;
		String strDoors;

		public DoorTask(int taskNum, int onTime, int offTime, int[] doors) {
			// create new door task, setting door to close (ON) on the next doortimer task.
			this.taskNum = taskNum;
			this.isOn = false;
			this.trigger = 1; // seconds before task will trigger/toggle doors
			this.onTime = onTime;
			this.offTime = offTime;
			this.doors = doors;
		}

		public int tick() {trigger--; return trigger;}
		public int taskNum() {return taskNum;}
		public int onTime() {return onTime;}
		public int offTime() {return offTime;}
		public String doorList() {
			strDoors = "";
			for (x = 0; x < doors.length; x++) {strDoors += " " + doors[x];}
			return strDoors;
		}

		// toggles doors and resets trigger time.
		public int toggle(int curDoorState) {
			if (isOn) {
				trigger = offTime;
				isOn = false;
                for (x = 0; x < doors.length; x++) {curDoorState = openDoor(curDoorState,doors[x]);}
			} else {
				trigger = onTime;
				isOn = true;
                for (x = 0; x < doors.length; x++) {curDoorState = closeDoor(curDoorState,doors[x]);}
			}
			return curDoorState;
		}
	}
}


