package twcore.bots.sbbot;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

/**
 * Handles routing of all messages between senders and interested listeners.   I use HashSets
 * to hold the list of listeners because HashSets guarantee that all elements of the collection
 * are unique.  This sidesteps the problem of accidentally adding a listener twice to the same
 * event listener queue. As buckets of HashSets are linked in a linked list, and we do not care
 * about any particular order in which we iterate over the listeners, accessing the list of
 * event listeners is O(1) and iterating over listeners is also O(1).  All of the overhead
 * with this system is in adding of listeners, which generally occurs only at startup.
 */
public class Operator {
    /**
     * Mapping associating the various types of events with listeners listening for those
     * events.
     */
    protected HashMap<MessageType, HashSet<Listener>> listeners;
    /**
     * Mapping associating the various types of objection polls with objectors listening for
     * those objection polls.
     */
    protected HashMap<MessageType, HashSet<Objector>> objectors;
    /**
     * Mapping associating the various types of respondse polls with responders listening for
     * those response polls.
     */
    protected HashMap<MessageType, HashSet<Responder>> responders;

    /**
     * Initializes the mappings of all listener queues.
     */
    public Operator() {
	listeners = new HashMap<MessageType, HashSet<Listener>>();
	objectors = new HashMap<MessageType, HashSet<Objector>>();
	responders = new HashMap<MessageType, HashSet<Responder>>();
    }


    public void notifyEvent(MessageType type, Message message) {
	if(listeners.containsKey(type) && listeners.get(type) != null) {
	    for(Listener l : listeners.get(type)) {
		l.notify(type, message);
	    }
	}
    }

    /**
     * Polls all objectors for any objections.  Returns first Objection if any, otherwise
     * returns null to signal no objections.
     * @param poll The poll being sent to interested objectors.
     */
    public Objection getObjections(MessageType type, Message message) {
	if(objectors.containsKey(type) && objectors.get(type) != null) {
	    for(Objector o : objectors.get(type)) {
		Objection ob = o.getObjection(type, message);
		if(ob != null) return ob;
	    }
	}
	return null;
    }

    /**
     * Polls all responders for responses to the current poll.  This is where the unchecked
     * operation warning comes from during compile (... Responder<T> r ... ).
     * @param poll The poll being sent to interested responders.
     */
    public Vector<Response> getResponses(MessageType type, Message message) {
	Vector<Response> v = new Vector<Response>();
	if(responders.containsKey(type) && responders.get(type) != null) {
	    for(Responder r : responders.get(type)) {
		Response response = r.getResponse(type, message);
		if(response != null) v.add(response);
	    }
	}
	return v;
    }

    /**
     * Adds an object to the list of objects listening for the specified event.
     * @param type The type of event the Listener is interested in.
     * @param l The Listener to be notified.
     */
    public void addListener(MessageType type, Listener l) {
	assert(l != null && type != null);
	if(!listeners.containsKey(type))
	    listeners.put(type, new HashSet<Listener>());
	listeners.get(type).add(l);
    }

    public void removeListener(MessageType type, Listener l) {
	assert(type != null && l != null);
	if(!listeners.containsKey(type)) return;
	listeners.get(type).remove(l);
    }

    /**
     * Adds an Objector to the list of objects listening for the specified poll.
     * @param o The Objector to be notified.
     * @param type The type of poll the Objector is interested in.
     */
    public void addObjector(MessageType type, Objector o) {
	assert(o != null && type != null);
	if(!objectors.containsKey(type))
	    objectors.put(type, new HashSet<Objector>());
	objectors.get(type).add(o);
    }

    public void removeObjector(MessageType type, Objector o) {
	assert(type != null && o != null);
	if(!objectors.containsKey(type)) return;
	objectors.get(type).remove(o);
    }

    /**
     * Adds a Responder to the list of objects listening for the specified poll.
     * @param r The Responder to be notified.
     * @param type The type of poll the responder is interested in.
     */
    public void addResponder(MessageType type, Responder r) {
	assert(r != null && type != null);
	if(!responders.containsKey(type))
	   responders.put(type, new HashSet<Responder>());
	responders.get(type).add(r);
    }

    public void removeResponder(MessageType type, Responder r) {
	assert(type != null && r != null);
	if(!responders.containsKey(type)) return;
	responders.get(type).remove(r);
    }
}