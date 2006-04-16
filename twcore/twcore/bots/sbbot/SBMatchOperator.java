package twcore.bots.sbbot;
import twcore.core.*;
import java.util.*;
import java.io.*;

public class SBMatchOperator extends SSEventOperator {
    private HashMap<SBEventType,HashSet<SBEventListener>> listeners;
    public static final SBEventType
	GOAL = new SBEventType(),
    //a STATEVENT is any event that should be noted in a player's statistics
	STATEVENT = new SBEventType(),
	THEDROP = new SBEventType(),
	GAMEOVER = new SBEventType(),
	PLAYERLEFT = new SBEventType(),
	POSTGAME = new SBEventType();
    
    public SBMatchOperator() {
	super();
	listeners = new HashMap<SBEventType,HashSet<SBEventListener>>();
    }

    public void notifyEvent (SBEventType type, SBEvent event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SBEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void addListener(SBEventType type, SBEventListener l) {
	assert(l != null && type != null);
	if(!listeners.containsKey(type)) {
	    
	    listeners.put(type, new HashSet<SBEventListener>());
	}
	listeners.get(type).add(l);
	
    }
}