package twcore.core.events;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import twcore.core.SubspaceBot;
import twcore.core.sql.SQLWorker;

/**
    Event used to return the ResultSet of an SQL background query.  All bots
    using SQL background queries must opt to handle this event, even if you
    do not use the ResultSet (as in the event of an INSERT, UPDATE or DELETE).
    In this case you still need to catch the ResultSet so that you may run
    BotAction's SQLClose method in order to free up memory allocated to the set.
*/
public class SQLResultEvent extends SubspaceEvent {

    private String      m_identifier;       // Unique identifier for the query
    private ResultSet   m_set;              // SQL ResultSet returned by query
    private String      m_query;            // Query used to retrieve ResultSet
    private SubspaceBot m_bot;              // Bot that made the query

    /**
        Creates a new instance of SQLResultEvent with the specified query,
        unique identifier, and bot reference.
        @param query SQL query to run
        @param identifier A unique identifier used to retrieve the result set
        @param bot A reference to the bot running the query
    */
    public SQLResultEvent( String query, String identifier, SubspaceBot bot ) {
        m_identifier = identifier;
        m_query = query;
        m_bot = bot;
    }

    /**
        @return The unique identifier used to retrieve this result set
    */
    public String getIdentifier() {
        return m_identifier;
    }

    /**
        @return The query used to retrieve this result set
    */
    public String getQuery() {
        return m_query;
    }

    /**
        @return The bot making the query
    */
    public SubspaceBot getBot() {
        return m_bot;
    }

    /**
        Sets the result set of this event to a specific ResultSet, and then handles
        the SQLResultEvent on the bot that made the query.  If the identifier is
        null, then the event will not be handled, and the ResultSet will be closed
        automatically.  This method is called by SQLWorker.
        @param set Result set to assign to this event (may be null)
        @see SQLWorker
    */
    public void setResultSet( ResultSet set ) {
        if( m_identifier == null ) {
            closeResultSet( set );
            return;
        }

        m_set = set;
        m_bot.handleEvent( this );
    }

    /**
        @return The result set of the query
    */
    public ResultSet getResultSet() {
        return m_set;
    }

    /**
        Internal version of BotAction's SQLClose for closing queries that
        do not need to access a ResultSet.
        @param rs ResultSet to close
    */
    private void closeResultSet( ResultSet rs ) {
        if (rs != null) {
            Statement smt = null;

            try {
                smt = rs.getStatement();
            } catch (SQLException sqlEx) {} // ignore any errors

            try {
                rs.close();
            } catch (SQLException sqlEx) {} // ignore any errors

            rs = null;

            if (smt != null) {
                try {
                    smt.close();
                } catch (SQLException sqlEx) {} // ignore any errors

                smt = null;
            }
        }
    }
}
