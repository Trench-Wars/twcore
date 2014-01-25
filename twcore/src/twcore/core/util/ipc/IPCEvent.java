package twcore.core.util.ipc;

/**
 * IPCEvent is just an Object used to transmit some time of SubspaceEvent
 * to another bot for analyzing. It includes a timestamp for authenticity
 * and chronology.
 * 
 * @author WingZero
 */
public class IPCEvent {
    
    String name;
    long time;
    int type;
    Object list;
    boolean all;

    /**
     * Constructs an IPC object that gives the player's name,
     * the type of event, and time the event occurred.
     * 
     * @param p Player's name
     * @param t Time occurred
     * @param mtype Type of event.
     */
    public IPCEvent(String p, long t, int mtype) {
        name = p;
        time = t; 
        type = mtype;
        list = null;
        all = false;
    }
    
    /**
     * Constructs an IPC object that gives a list of all,
     * playing players in an arena a bot has entered/left,
     * as well as the time it was sent.
     * 
     * @param l List containing all playing players in an arena.
     * @param t Time occurred
     * @param mtype Type of event.
     */
    public IPCEvent(Object l, long t, int mtype) {
        name = "";
        time = t; 
        type = mtype;
        list = l;
        all = true;
    }
    
    /**
     * @return name of the player causing the event.
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return type of event received by the sending bot.
     */
    public int getType() {
        return type;
    }
    
    /**
     * @return system time in milliseconds the event occurred.
     */
    public long getTime() {
        return time;
    }
    
    /**
     * @return the list of all playing players from an arena
     */
    public Object getList() {
        return list;
    }
    
    /**
     * @return true if sending all players in arena
     */
    public boolean isAll() {
        return all;
    }

}
