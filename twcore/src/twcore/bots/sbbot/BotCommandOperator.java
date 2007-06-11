package twcore.bots.sbbot;
import java.util.HashMap;
import java.util.HashSet;

import twcore.core.BotAction;

public class BotCommandOperator extends SSEventOperator {
    private BotAction m_botAction;
    public static final BotCommandType
	DIE = new BotCommandType(),
	ADD = new BotCommandType(),
	SUB = new BotCommandType(),
	NOTPLAYING = new BotCommandType(),
	HELP = new BotCommandType(),
	STARTGAME = new BotCommandType(),
	T1SETCAP = new BotCommandType(),
	T2SETCAP = new BotCommandType(),
	LAGOUT = new BotCommandType(),
	CAP = new BotCommandType(),
	LIST = new BotCommandType(),
	STARTPICK = new BotCommandType(),
	READY = new BotCommandType(),
	REMOVE = new BotCommandType(),
	SUMMON = new BotCommandType(),
	KILLGAME = new BotCommandType(),
	SENDARENAMESSAGE = new BotCommandType(),
	SENDPRIVATEMESSAGE = new BotCommandType();


    private HashMap<BotCommandType,HashSet<BotCommandListener>> listeners;
    public BotCommandOperator() {
	super();
	listeners = new HashMap<BotCommandType,HashSet<BotCommandListener>>();
    }

    public void notifyEvent(BotCommandType type, BotCommandEvent event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(BotCommandListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void addListener(BotCommandType type, BotCommandListener l) {
	assert(l != null && type != null);
	if(!listeners.containsKey(type))
	    listeners.put(type, new HashSet<BotCommandListener>());
	listeners.get(type).add(l);
    }
}