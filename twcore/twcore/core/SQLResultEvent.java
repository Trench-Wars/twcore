package twcore.core;

/*
 * SQLResultEvent.java
 *
 * Created on November 3, 2002, 12:45 AM
 */

/**
 *
 * @author  harvey
 */
import java.sql.*;
public class SQLResultEvent extends SubspaceEvent {
    private String m_identifier;
    private ResultSet m_set;
    private String m_query;
    private SubspaceBot m_bot;
    /** Creates a new instance of SQLResultEvent */
    public SQLResultEvent( String query, String identifier, SubspaceBot bot ) {
        m_identifier = identifier;
        m_query = query;
        m_bot = bot;
    }
    
    public String getIdentifier(){
        return m_identifier;
    }
    
    public String getQuery(){
        return m_query;
    }
    
    public SubspaceBot getBot(){
        return m_bot;
    }
    
    public void setResultSet( ResultSet set ){
        m_set = set;
        if( m_identifier == null )
            return;
        m_bot.handleEvent( this );
    }
    
    public ResultSet getResultSet(){
        return m_set;
    }
    
}
