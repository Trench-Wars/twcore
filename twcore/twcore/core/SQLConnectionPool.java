package twcore.core;

import java.sql.*;
import java.util.*;

public class SQLConnectionPool implements Runnable {
    private String driver, dburl, poolName;
    private int maxConnections;
    private int initialConnections;
    private boolean waitIfBusy;
    private Vector availableConnections, busyConnections;
    private boolean connectionPending = false;
    private int currentBackground = 0;

    public SQLConnectionPool(String poolName, String dburl, int minPool, int maxPool, int wait, String driver) throws SQLException {
        
        this.poolName = poolName;
        if( wait == 0 ) waitIfBusy = false;
        else waitIfBusy = true;
        this.driver = driver;
        this.dburl = dburl;
        maxConnections = maxPool;
        initialConnections = minPool;
        if (initialConnections > maxConnections) {
            initialConnections = maxConnections;
        }
        availableConnections = new Vector(initialConnections);
        busyConnections = new Vector();
        for(int i=0; i<initialConnections; i++) {
            availableConnections.addElement(makeNewConnection());
        }
    }
    
    long lastTime = System.currentTimeMillis();
    int total = 0;
    int idnum = 0;
    long totaltime = 0;
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
            
            // DEBUG: Returning null to generate NullPointerExceptions in bots that do not
            // catch SQLExceptions
            return null;
        }        
    }
    
    public synchronized Connection getConnection()
    throws SQLException {
        if (!availableConnections.isEmpty()) {
            Connection existingConnection =
            (Connection)availableConnections.lastElement();
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
            if(( totalConnections() < maxConnections ) &&
            !connectionPending ) {
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
        
    private void makeBackgroundConnection() {
        connectionPending = true;
        try {
            Thread connectThread = new Thread(this);
            connectThread.start();
        } catch(OutOfMemoryError oome) {
        }
    }
    
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
    
    private Connection makeNewConnection() throws SQLException {
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection( dburl );
            return( conn );
        } catch(ClassNotFoundException cnfe) {
            throw new SQLException("Can't find class for driver: " +
            driver);
        }
    }
    
    public synchronized void free(Connection connection) {
        busyConnections.removeElement(connection);
        availableConnections.addElement(connection);
        // Wake up threads that are waiting for a connection
        notifyAll();
    }
    
    public synchronized int totalConnections() {
        return(availableConnections.size() +
        busyConnections.size());
    }
    
    public synchronized void closeAllConnections() {
        closeConnections(availableConnections);
        availableConnections = new Vector();
        closeConnections(busyConnections);
        busyConnections = new Vector();
    }
    
    private void closeConnections(Vector connections) {
        try {
            for(int i=0; i<connections.size(); i++) {
                Connection connection =
                (Connection)connections.elementAt(i);
                if (!connection.isClosed()) {
                    connection.close();
                }
            }
        } catch(SQLException sqle) {
        }
    }
    
    public synchronized boolean isAvailable(){
        return !availableConnections.isEmpty();
    }
    
    public synchronized String toString() {
        String info =
        "SQL Status: " + poolName + " pool has " 
        + (availableConnections.size() + busyConnections.size()) + "/"
        + maxConnections + " connections online.  " + busyConnections.size()
        + " of them are busy.";
        return(info);
    }
    
    public synchronized int getNumBackground(){
        return currentBackground;
    }    
    public synchronized boolean reachedMaxBackground(){
        return currentBackground == maxConnections;
    }    
    
    public synchronized int getMaxConnections(){
        return maxConnections;
    }
    public synchronized void incrementBackgroundCount(){
        currentBackground++;
    }
    public synchronized void decrementBackgroundCount(){
        currentBackground--;
    }
}