package twcore.core.sql;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import twcore.core.BotSettings;
import twcore.core.SubspaceBot;
import twcore.core.events.SQLResultEvent;
import twcore.core.util.Tools;

/**
 * Thread-based main class for the core's SQL database functionality.
 * Initializes and manages SQL connection pools and queries, and runs
 * background queries on a semi-regular basis.
 * <p>
 * 
 * Choosing a standard query vs. background/high-priority background query:
 * <p>
 * <b>Standard / foreground</b>    -  Runs exactly when needed.  Does not wait in
 * a queue to execute (unless connections are low).  Does not require a unique
 * identifier or special event handling.  However, a standard query will
 * pause the program thread until the results are returned.  For large queries
 * and bad connections this may result in long delays and unresponsiveness.
 * <p>
 * <b>Background</b>               -  Runs as a separate program thread.  Waits in a
 * queue and can be delayed by other queries waiting to execute.  Requires the
 * bot to catch an SQLResultEvent and use a unique identifier to refer to the
 * query.  As a separate thread, after the background query is run, the bot
 * continues execution as normal, causing no delays.  Ideal when multiple users
 * may need to access large amounts of SQL data from the same bot at the same
 * time without compromising responsiveness to the bot for others.  However,
 * their individual result sets may return more slowly than with a standard query.
 * <p>
 * <b>High-priority background</b> -  Same as a background query, but added to the
 * head of the queue.  Combines the versatility of a background queue with the
 * foreground's ability to return the result set almost instantly.
 */
public class SQLManager extends Thread {
    BotSettings sqlcfg;                          // Reference to SQL config file
    HashMap     pools;                           // Connection pool storage
    HashMap     queues;                          // Background queue storage
    boolean     operational = true;              // Status of SQL system
    final static int THREAD_SLEEP_TIME = 60000;  // Length of time for thread to
                                                 // sleep, in ms, after all
                                                 // background queries are done.
    
    /**
     * Initialize SQL functionality with the information given in the specified
     * configuration file.
     * @param configFile Properly formatted CFG file containing SQL system data
     */
    public SQLManager( File configFile ) {
        pools = new HashMap();
        queues = new HashMap();
        sqlcfg = new BotSettings( configFile );
        try{
            for( int i = 1; i <= sqlcfg.getInt( "ConnectionCount" ); i++ ){
                String name = sqlcfg.getString( "Name" + i );
                
                String dburl = "jdbc:mysql://" + sqlcfg.getString( "Server" + i )
                + ":" + sqlcfg.getInt( "Port" + i ) + "/"
                + sqlcfg.getString( "Database" + i ) + "?user="
                + sqlcfg.getString( "Login" + i ) + "&password="
                + sqlcfg.getString( "Password" + i ) + "&autoReconnect=true" + "&autoReconnectForPools=true" + "&maxReconnects=2147483647" + "&initialTimeout=1" + "&logSlowQueries=true" + "&interactiveClient=true";

                SQLConnectionPool db = new SQLConnectionPool( name, dburl,
                sqlcfg.getInt( "MinPoolSize" + i ),
                sqlcfg.getInt( "MaxPoolSize" + i ),
                sqlcfg.getInt( "WaitIfBusy" + i ),
                sqlcfg.getString( "Driver" + i )
                );
                pools.put( name, db );
                queues.put( name, new SQLBackgroundQueue() );
            }
            Tools.printLog( "SQL Connection Pools Initialized Successfully." );
            for( Iterator i = pools.values().iterator(); i.hasNext(); ){
                Tools.printLog( ((SQLConnectionPool)i.next()).toString() );
            }
        } catch( SQLException e ){
            Tools.printLog( "Failed to load SQL Connection Pools.  Driver missing?" );
            operational = false;
            Tools.printLog( e.getMessage() );
        }
        if( operational ){
            start();
            Tools.printLog( "Background Queues Initialized" );
        } else {
            Tools.printLog( "Background Queues NOT Initialized" );
        }
    }
    
    /**
     * Adds a regular background query to the end of the queue.  If there are no
     * queued queries ahead of it, the background query will be executed nearly
     * as quickly as a regularly executed query, but without delaying the bot's
     * thread to retrieve the result set.  The query is instead run in a new
     * thread and returned to the bot via an SQLResultEvent, and is identified by
     * a unique key (<CODE>identifier</CODE>).
     * @param connName Name of the connection as defined in sql.cfg
     * @param identifier The unique identifier for this query
     * @param query A properly-formed SQL query
     * @param bot The bot requesting the query (if unsure, use <b>this</b>)
     */
    public void queryBackground( String connName, String identifier,
    String query, SubspaceBot bot ){
        if( !operational ){
            Tools.printLog( "Unable to process query: " + query );
        } else {
            if( !pools.containsKey( connName )) return;
            SQLBackgroundQueue queue = (SQLBackgroundQueue)queues.get( connName );
            queue.addQuery( new SQLResultEvent( query, identifier, bot ));
            interrupt();
        }
    }
    
    /**
     * Adds a background query to the front of the queue.  A high-priority
     * background query will be executed nearly as quickly as a regularly
     * executed query, but without delaying the bot's thread to retrieve the
     * results.  The query is instead run in a new thread and returned to the bot
     * via an SQLResultEvent, and is identified by a unique key (<CODE>identifier</CODE>).
     * @param connName Name of the connection as defined in sql.cfg
     * @param identifier The unique identifier for this query
     * @param query A properly-formed SQL query
     * @param bot The bot requesting the query (if unsure, use <b>this</b>)
     */
    public void queryBackgroundHighPriority( String connName, String identifier,
    String query, SubspaceBot bot ){
        if( !operational ){
            Tools.printLog( "Unable to process background high priority query: " + query );
        } else {
            if( !pools.containsKey( connName )) return;
            SQLBackgroundQueue queue = (SQLBackgroundQueue)queues.get( connName );
            queue.addHighPriority( new SQLResultEvent( query, identifier, bot ));
            interrupt();
        }
    }
    
    /**
     * Runs a regular SQL query using the specified database connection.  Your
     * bot's thread will not continue while the query is in effect.  Use a
     * background query if you wish for the thread to continue while the query
     * is executed.
     * @param connectionName Name of the connection as defined in sql.cfg
     * @param query A properly-formed SQL query
     * @return The result set of the query (MAY be null)
     * @throws SQLException
     */
    public ResultSet query( String connectionName, String query ) throws SQLException {
        if( !operational ){
            Tools.printLog( "Unable to process query: " + query );
            return null;
        } else {
            if( !pools.containsKey( connectionName )) return null;
            return ((SQLConnectionPool)pools.get( connectionName )).query( query );
        }
    }
    
    /**
     * @return True if the SQL system is operational
     */
    public boolean isOperational(){
        return operational;
    }

    /**
     * Prints to the log file the status of all connection pools. 
     */
    public void status(){
        if( !operational ){
            Tools.printLog( "SQL Connection Not Operational" );
        } else {
            Iterator i = pools.values().iterator();
            while( i.hasNext() ){
                Tools.printLog( ((SQLConnectionPool)i.next()).toString() );
            }
        }
    }
    
    /**
     * Checks the background queue for queries waiting to be run, and dispatches
     * them each to a separate SQLWorker thread.  After the result is received,
     * it's then returned as an SQLResultEvent to the bot that made the query.  
     * The SQLManager thread will sleep for a defined amount of time, but will
     * interrupt/return when a background query requires processing.
     */
    public void run() {
        while( true ){
            Iterator i = queues.keySet().iterator();
            while( i.hasNext() ){
                String name = (String)i.next();
                SQLBackgroundQueue queue = (SQLBackgroundQueue)queues.get( name );
                SQLConnectionPool pool = (SQLConnectionPool)pools.get( name );
                while( !queue.isEmpty() && !pool.reachedMaxBackground() ){
                    SQLResultEvent event = queue.getNextInLine();
                    new SQLWorker( pool, event, this );
                }
            }
            try{
                Thread.sleep( THREAD_SLEEP_TIME );
            } catch( InterruptedException e ){}
        }
    }
    
}
