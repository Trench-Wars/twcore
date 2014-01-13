package twcore.core.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

import twcore.core.events.SQLResultEvent;
import twcore.core.util.Tools;

/**
 * Runs a background SQL query given a connection pool to use and an undelivered
 * SQLResultEvent object to place the results into.  By handling in a separate
 * thread, it frees the bot process of having to wait on a query. 
 */
public class SQLWorker implements Runnable {
    private SQLResultEvent    m_event;      // Event to hand the ResultSet to     
    private SQLConnectionPool m_pool;       // Connection pool to run query on
    private SQLManager        m_manager;    // For interrupting any waits
    
    /**
     * Creates a new SQLWorker and begins a background query, given a connection
     * pool to use for the query, an event to place the result set returned by
     * the query into, and an SQLManager to wake up/interrupt when the process
     * has finished (if it is currently sleeping).
     * @param pool Connection pool to use to run the query
     * @param event Event that will afterward contain the returned ResultSet
     * @param manager Waiting object to interrupt when finished
     */
    public SQLWorker( SQLConnectionPool pool, SQLResultEvent event, SQLManager manager ) {
        m_pool = pool;
        m_manager = manager;
        Thread t = new Thread( this, "SQLWorker" );
        m_event = event;
        m_pool.incrementBackgroundCount();
        t.start();
    }
    
    /**
     * Runs the SQL query found in the SQLResultEvent the SQLWorker was instantiated
     * with.  Sets the returned ResultSet inside the event, which in turn will fire
     * the event in the bot so as to be handled and fetched by unique key.  After
     * this is done, the background queue count of the connection pool used is
     * reduced by one, and the SQLManager that called the worker is interrupted
     * back into consciousness, if it is currently asleep.   
     */
    public void run() {
        try{            
            ResultSet set = m_pool.query( m_event.getQuery() );
            m_event.setResultSet( set );
            m_pool.decrementBackgroundCount();
            m_manager.interrupt();
        } catch( SQLException e ){
            Tools.printLog("SQLException encountered while running background query in SQLWorker.");
            Tools.printStackTrace( e );
        }
    }
}
