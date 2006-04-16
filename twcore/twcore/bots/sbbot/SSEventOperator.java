package twcore.bots.sbbot;
import twcore.core.*;
import java.util.*;
import java.io.*;

public class SSEventOperator extends Operator {
    protected HashMap<SSEventMessageType, HashSet<SSEventListener>> listeners;

    SSEventOperator() {
	super();
	listeners = new HashMap<SSEventMessageType, HashSet<SSEventListener>>();
    }

    public void addListener(SSEventMessageType type, SSEventListener l) {
	assert(l != null && type != null);
	if(!listeners.containsKey(type))
	    listeners.put(type, new HashSet<SSEventListener>());
	listeners.get(type).add(l);
    }

    public void removeListener(SSEventMessageType type, SSEventListener l) {
	assert(type != null && l != null);
	if(!listeners.containsKey(type)) return;
	listeners.get(type).remove(l);
    }
    
    public void notifyEvent(SSEventMessageType type, SubspaceEvent event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }
    
    public void notifyEvent(SSEventMessageType type, WatchDamage event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, ScoreReset event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, PlayerEntered event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, twcore.core.Message event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, PlayerLeft event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, PlayerPosition event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, PlayerDeath event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, ScoreUpdate event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, WeaponFired event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }
    
    public void notifyEvent(SSEventMessageType type, FrequencyChange event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, FrequencyShipChange event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, ArenaJoined event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, FileArrived event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, FlagReward event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, FlagVictory event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, FlagPosition event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, FlagClaimed event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, FlagDropped event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, SoccerGoal event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, BallPosition event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, Prize event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, LoggedOn event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, ArenaList event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, InterProcessEvent event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }

    public void notifyEvent(SSEventMessageType type, SQLResultEvent event) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(SSEventListener l : listeners.get(type)) {
		l.notify(type, event);
	    }
	}
    }
}