package twcore.core;

/*
 * MySQLManager.java
 *
 * Created on October 25, 2002, 8:57 AM
 */

/**
 *
 * @author  harvey
 */

import java.util.*;
import java.sql.*;
import java.io.File;
public class SQLManager extends Thread {
    BotSettings sqlcfg;
    HashMap pools;
    HashMap queues;
    boolean operational = true;
    /** Creates a new instance of MySQLManager */
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
                + sqlcfg.getString( "Password" + i ) + "&autoReconnect=true";
                
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
            Tools.printLog( "Failed to load SQL Connection Pools.  "
            + "Driver missing?" );
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
    
    public boolean isOperational(){
        return operational;
    }
    
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
    
    public ResultSet query( String connectionName, String query )
    throws SQLException {
        if( !operational ){
            Tools.printLog( "Unable to process query: " + query );
            return null;
        } else {
            if( !pools.containsKey( connectionName )) return null;
            return ((SQLConnectionPool)pools.get( connectionName )).query( query );
        }
    }
    
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
    
    public void run() {
        while( true ){
            Iterator i = queues.keySet().iterator();
            while( i.hasNext() ){
                String name = (String)i.next();
                SQLBackgroundQueue queue = (SQLBackgroundQueue)queues.get( name );
                SQLConnectionPool pool = (SQLConnectionPool)pools.get( name );
                while( !queue.isEmpty() && !pool.reachedMaxBackground() ){
                    SQLResultEvent event = queue.getNextInLine();
                    SQLWorker worker = new SQLWorker( pool, event, this );
                }
            }
            try{
                Thread.sleep( 60000 );
            } catch( InterruptedException e ){}
        }
    }
    
}
