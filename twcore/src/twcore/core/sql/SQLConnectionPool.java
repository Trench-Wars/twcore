package twcore.core.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

/**
 * Main workhorse of the TWCore SQL system.  Handles all connections of a given
 * connection pool and any queries that use it, passing to separate threads to
 * create new connections as necessary.
 * <p>
 * Note that if the WaitIfBusy argument is set to 1 for a given connection pool
 * inside sql.cfg, and no connections are available, the connection pool will
 * wait for a new connection to be created before continuing.  If it is set to 0
 * and no connections are available, instead of waiting an SQLException is thrown.
 */
public class SQLConnectionPool implements Runnable {
    private String  driver;                     // Driver used to make connections
    private String  dburl;                      // URL of the database to access
    private String  poolName;                   // Name used by bots to refer to pool
    private int     maxConnections;             // Max # connections allowed to DB
    private int     initialConnections;         // Min # connections allowed
    private boolean waitIfBusy;                 // True  - wait for a connection if
                                                //         none is available
                                                // False - throw SQLException if no
                                                //         connection is available
    private Vector<Connection>  availableConnections;       // Connections not in use
    private Vector<Connection>  busyConnections;            // Connections currently querying
    private boolean connectionPending = false;  // True if a connection is being made
    private int     currentBackground = 0;      // Total number of background queries

    /**
     * Creates a new connection pool.  A number of connections are made equal
     * to the minimum amount of connections allowed, which are then all added
     * to the available connection list.
     * @param poolName Name of the connection pool
     * @param dbURL URL of the database connection
     * @param minPool Minimum number of active connections in the pool at any one time
     * @param maxPool Maximum number of connections allowed
     * @param wait 0 - do not wait for a connection to unlock; 1 - wait (safer)
     * @param driver
     * @throws SQLException
     */
    public SQLConnectionPool(String poolName, String dbURL, int minPool, int maxPool, int wait, String driver) throws SQLException {

        this.poolName = poolName;
        if( wait == 0 )
            waitIfBusy = false;
        else
            waitIfBusy = true;
        this.driver = driver;
        this.dburl = dbURL;
        maxConnections = maxPool;
        initialConnections = minPool;
        if (initialConnections > maxConnections) {
            initialConnections = maxConnections;
        }
        availableConnections = new Vector<Connection>(initialConnections);
        busyConnections = new Vector<Connection>();
        for(int i=0; i<initialConnections; i++) {
            availableConnections.addElement(makeNewConnection());
        }
    }

    /**
     * Runs a straight SQL query using an available connection.  Once done,
     * the connection is freed back into the available pool and the result
     * set of the query is returned.  Unlike most SQL handlers, the ResultSet
     * MAY be returned null (in the case that an SQLException is encountered).
     * @param query A properly-formed SQL query
     * @return The result set of the query (MAY be null)
     * @throws SQLException
     */
    public ResultSet query( String query ) throws SQLException {
        Connection conn = getConnection();
        try{
            Statement stmt = conn.createStatement();
            stmt.execute( query );
            free( conn );
            return stmt.getResultSet();
        }catch( SQLException e ){
            free( conn );

            //throw e;

            // DEBUG: Returning null to generate NullPointerExceptions in bots that
            // do not catch SQLExceptions
            return null;
        }
    }

    /**
     * Attempts to retrieve a connection from the connection pool.  If there is
     * one available, it will be returned and added to the busy connection list.
     * Otherwise, if the number of total connections is less than the maximum
     * allowed, a new connection will be made in the background and returned to
     * the available list once created, at which point the method will recurse
     * back on itself and grab the newly made connection.
     * @return An available connection
     * @throws SQLException
     */
    public synchronized Connection getConnection() throws SQLException {
        if (!availableConnections.isEmpty()) {
            Connection existingConnection = availableConnections.lastElement();
            int lastIndex = availableConnections.size() - 1;
            availableConnections.removeElementAt(lastIndex);
            if (existingConnection.isClosed()) {
                notifyAll(); // Freed up a spot for anybody waiting
                return(getConnection());
            } else {
                busyConnections.addElement(existingConnection);
                return(existingConnection);
            }
        } else {
            if(( totalConnections() < maxConnections ) && !connectionPending ) {
                makeBackgroundConnection();
            } else if( !waitIfBusy ) {
                throw new SQLException("Connection limit reached");
            }
            try {
                wait();
            } catch(InterruptedException ie) {}
            // Someone freed up a connection, so try again.
            return getConnection();
        }
    }

    /**
     * Creates a new connection in the background (separate thread).
     */
    private void makeBackgroundConnection() {
        connectionPending = true;
        try {
            Thread connectThread = new Thread(this);
            connectThread.start();
        } catch(OutOfMemoryError oome) {
        }
    }

    /**
     * Method for running in a separate thread to make a new connection in the
     * background.  Once the connection is made, it's added to the list of
     * available connections, and any threads waiting (using wait()) for a
     * connection inside getConnection() will be notified that one is now
     * available.
     */
    public void run() {
        try {
            Connection connection = makeNewConnection();
            synchronized(this) {
                availableConnections.addElement(connection);
                connectionPending = false;
                notifyAll();
            }
        } catch(Exception e) {
        }
    }

    /**
     * Makes a new connection and returns a reference to it.  This is done when
     * all initial connections are busy, but the number of total active connections
     * is less than the max allowed.
     * @return A new connection
     * @throws SQLException
     */
    private Connection makeNewConnection() throws SQLException {
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection( dburl );
            return( conn );
        } catch(ClassNotFoundException cnfe) {
            throw new SQLException("Can't find class for driver: " + driver);
        }
    }

    /**
     * Free a connection back to the available connection pool.  This will notify
     * other threads that are waiting (using wait()) for a connection in getConnection()
     * that a new connection is now available.
     * @param connection The connection to free
     */
    public synchronized void free( Connection connection ) {
        busyConnections.removeElement(connection);
        availableConnections.addElement(connection);
        // Wake up threads that are waiting for a connection
        notifyAll();
    }

    /**
     * @return Total number of connections either available or currently busy
     */
    public synchronized int totalConnections() {
        return(availableConnections.size() +
        busyConnections.size());
    }

    /**
     * Closes all of the connections monitored by this connection pool.
     */
    public synchronized void closeAllConnections() {
        closeConnections(availableConnections);
        availableConnections = new Vector<Connection>();
        closeConnections(busyConnections);
        busyConnections = new Vector<Connection>();
    }

    /**
     * Closes all of the connections in a given vector.
     * @param connections Vector containing connections to close
     */
    private void closeConnections(Vector connections) {
        try {
            for(int i=0; i<connections.size(); i++) {
                Connection connection = (Connection)connections.elementAt(i);
                if (!connection.isClosed()) {
                    connection.close();
                }
            }
        } catch(SQLException sqle) {
        }
    }

    /**
     * @return True if there is at least one connection in the pool available.
     */
    public synchronized boolean isAvailable(){
        return !availableConnections.isEmpty();
    }

    /**
     * @return Number of connections busy, available and maximum allowed.
     */
    public synchronized String toString() {
        String info =
        "SQL pool " + poolName + ": " + totalConnections() + "/"
        + maxConnections + " connections online, " + busyConnections.size()
        + " in use";
        return(info);
    }

    /**
     * @return Number of connections running background queries
     */
    public synchronized int getNumBackground(){
        return currentBackground;
    }

    /**
     * XXX: Intentional comparison?  This allows the number of total connections
     * to exceed the maximum allotted connections.  Perhaps:
     *
     * <code>(totalConnections() + currentBackground) == maxConnections</code>
     *
     * is what was really intended?
     * @return True if # background connections is equal to max # connections allowed
     */
    public synchronized boolean reachedMaxBackground(){
        return currentBackground == maxConnections;
    }

    /**
     * Returns the maximum number of connections allowed.  However, background
     * connections don't appear to apply to this figure.
     * @return Maximum number of connections allowed
     */
    public synchronized int getMaxConnections(){
        return maxConnections;
    }

    /**
     * Adds one to the current number of background queries running.
     */
    public synchronized void incrementBackgroundCount(){
        currentBackground++;
    }

    /**
     * Subtracts one from the current number of background queries running.
     */
    public synchronized void decrementBackgroundCount(){
        currentBackground--;
    }
}