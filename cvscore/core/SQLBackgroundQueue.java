package twcore.core;

/*
 * SQLBackgroundQueue.java
 *
 * Created on November 6, 2002, 6:57 AM
 */

/**
 *
 * @author  harvey
 */
import java.util.*;
public class SQLBackgroundQueue {
    private LinkedList queue;
    /** Creates a new instance of SQLBackgroundQueue */
    public SQLBackgroundQueue() {
        queue = new LinkedList();
    }
    
    public synchronized SQLResultEvent getNextInLine(){
        if( queue.isEmpty() ) return null;
        SQLResultEvent event = (SQLResultEvent)queue.getFirst();
        queue.removeFirst();
        return event;
    }
    
    public synchronized void addHighPriority( SQLResultEvent event ){
        queue.addFirst( event );
    }
    
    public synchronized boolean isEmpty(){
        return queue.isEmpty();
    }
    
    public synchronized void addQuery( SQLResultEvent event ){
        queue.addLast( event );
    }
}
