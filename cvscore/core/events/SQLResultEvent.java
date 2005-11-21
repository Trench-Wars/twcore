package twcore.core.events;

import java.sql.ResultSet;

import twcore.core.SubspaceBot;
import twcore.core.sql.SQLWorker;

/**
 * Event used to return the ResultSet of an SQL background query.  All bots
 * using SQL background queries must opt to handle this event.
 */
public class SQLResultEvent extends SubspaceEvent {
    
    private String      m_identifier;       // Unique identifier for the query
    private ResultSet   m_set;              // SQL ResultSet returned by query
    private String      m_query;            // Query used to retrieve ResultSet
    private SubspaceBot m_bot;              // Bot that made the query

    /**
     * Creates a new instance of SQLResultEvent with the specified query,
     * unique identifier, and bot reference.
     * @param query SQL query to run
     * @param identifier A unique identifier used to retrieve the result set
     * @param bot A reference to the bot running the query
     */
    public SQLResultEvent( String query, String identifier, SubspaceBot bot ) {
        m_identifier = identifier;
        m_query = query;
        m_bot = bot;
    }
    
    /**
     * @return The unique identifier used to retrieve this result set
     */
    public String getIdentifier(){
        return m_identifier;
    }
    
    /**
     * @return The query used to retrieve this result set
     */
    public String getQuery(){
        return m_query;
    }
    
    /**
     * @return The bot making the query 
     */
    public SubspaceBot getBot(){
        return m_bot;
    }
    
    /**
     * Sets the result set of this event to a specific ResultSet, and then handles
     * the SQLResultEvent on the bot that made the query.  If the identifier is
     * null, then the event will not be handled.  Called by SQLWorker.
     * @param set Result set to assign to this event (may be null)
     * @see SQLWorker
     */
    public void setResultSet( ResultSet set ){
        m_set = set;
        if( m_identifier == null )
            return;
        m_bot.handleEvent( this );
    }
    
    /**
     * @return The result set of the query
     */
    public ResultSet getResultSet(){
        return m_set;
    }
    
}
