package twcore.core.sql;

import java.util.LinkedList;

import twcore.core.events.SQLResultEvent;

/**
 * Purely a class-specific abstraction of a LinkedList.  SQLBackgroundQueue
 * is an implementation of a background query queue that ensures all that is
 * stored in the queue and returned from the queue is SQLResultEvents.
 */
public class SQLBackgroundQueue {
    
    private LinkedList<SQLResultEvent> queue;

    /**
     * Creates a new background queue to keep track of all background queries.
     */
    public SQLBackgroundQueue() {
        queue = new LinkedList<SQLResultEvent>();
    }

    /**
     * Returns the SQL event object of the background query at the head of the
     * queue.  If the queue is empty, the return is null.
     * @return SQL event of the background query at the head of the queue (null if none)
     */
    public synchronized SQLResultEvent getNextInLine(){
        if( queue.isEmpty() ) return null;
        SQLResultEvent event = queue.getFirst();
        queue.removeFirst();
        return event;
    }

    /**
     * Adds a background query to the head of the queue (high priority).
     * @param event
     */
    public synchronized void addHighPriority( SQLResultEvent event ){
        queue.addFirst( event );
    }

    /**
     * @return True if the queue is empty
     */
    public synchronized boolean isEmpty(){
        return queue.isEmpty();
    }

    /**
     * Adds a background query to the tail end of the queue (standard priority).
     * @param event SQL event object containing the background query to add
     */
    public synchronized void addQuery( SQLResultEvent event ){
        queue.addLast( event );
    }
}
