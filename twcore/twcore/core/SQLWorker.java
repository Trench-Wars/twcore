package twcore.core;

/*
 * SQLWorker.java
 *
 * Created on November 3, 2002, 1:04 AM
 */

/**
 *
 * @author  harvey
 */
import java.sql.ResultSet;
import java.sql.SQLException;
public class SQLWorker implements Runnable {
    private SQLResultEvent m_event;    
    private SQLConnectionPool m_pool;
    private SQLManager m_manager;    
    /** Creates a new instance of SQLWorker */
    public SQLWorker( SQLConnectionPool pool, SQLResultEvent event,
    SQLManager manager ) {
        m_pool = pool;
        m_manager = manager;
        Thread t = new Thread( this, "SQLWorker" );
        //manager.incrementThreadCount();
        m_event = event;
        m_pool.incrementBackgroundCount();
        t.start();
    }
    
    public void run() {
        try{            
            ResultSet set = m_pool.query( m_event.getQuery() );
            m_event.setResultSet( set );
            m_pool.decrementBackgroundCount();
            m_manager.interrupt();
        } catch( SQLException e ){
            Tools.printStackTrace( e );
        }
    }
}
