package twcore.bots.sbbot;
import java.util.HashMap;
import java.util.HashSet;

import twcore.core.events.ArenaJoined;
import twcore.core.events.ArenaList;
import twcore.core.events.BallPosition;
import twcore.core.events.FileArrived;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FlagDropped;
import twcore.core.events.FlagPosition;
import twcore.core.events.FlagReward;
import twcore.core.events.FlagVictory;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.Prize;
import twcore.core.events.SQLResultEvent;
import twcore.core.events.ScoreReset;
import twcore.core.events.ScoreUpdate;
import twcore.core.events.SoccerGoal;
import twcore.core.events.SubspaceEvent;
import twcore.core.events.WatchDamage;
import twcore.core.events.WeaponFired;

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

    public void notifyEvent(SSEventMessageType type, twcore.core.events.Message event) {
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