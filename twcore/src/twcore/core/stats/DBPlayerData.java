package twcore.core.stats;

import java.sql.ResultSet;
import java.sql.SQLException;

import twcore.core.BotAction;
import twcore.core.util.Tools;

/**
* Used to store information about a player in a database.
* @author  Mythrandir
*/
public class DBPlayerData {
    BotAction m_connection;

    boolean m_playerLoaded = false;
    String m_connName = "website";
    String m_aliasConnName = "local";
    String m_fcUserName = "";
    String m_fcPassword = "";
    int m_fnUserID = 0;
    java.sql.Date m_fdSignedUp = null;

    int m_fnTeamUserID = 0;
    int m_fnTeamID = 0;
    java.sql.Date m_fdTeamSignedUp = null;
    String m_fcTeamName = "";

    String m_fcIP = "";
    int m_fnMID = 0;
    int m_fnStatus = 0;         // 0 - unregistered; 1 - enabled for play; 2 - disabled
    int m_rank = -1;

    long m_lastQuery = 0;

    /**
     * Creates a new instance of DBPlayerData
     */
    public DBPlayerData(BotAction conn, String connName) {
        m_connection = conn;
        m_connName = connName;
    }

    /**
     * Creates a new instance of DBPlayerData<br/>
     * <br/>
     * WARNING: The 3rd parameter can take any length playername. If you specify a 23 long playername (from *info f.ex)
     * 			it will be saved as a 23 long playername. If you try to lookup the name by using the name from the playerlist
     * 			through m_botAction.getPlayerName() then this name will be max 19 characters long and thus not equal.
     * @param conn
     * @param connName
     * @param fcPlayerName
     */
    public DBPlayerData(BotAction conn, String connName, String fcPlayerName) {
        m_connection = conn;
        m_fcUserName = fcPlayerName;
        m_connName = connName;
        getPlayerData();
    }


    public DBPlayerData(BotAction conn, String connName, String fcPlayerName, boolean createIfNotExists) {
        m_connection = conn;
        m_fcUserName = fcPlayerName;
        m_connName = connName;
        if (!getPlayerData() && createIfNotExists && !checkPlayerExists()) createPlayerData();
    }




    public boolean getPlayerSquadData() {
        boolean result = false;
        try {
            ResultSet qryPlayerSquadInfo = m_connection.SQLQuery(m_connName,
            "SELECT TU.fdJoined, TU.fnTeamUserID, T.fnTeamID, T.fcTeamName FROM tblTeam T, tblTeamUser TU " +
            "WHERE TU.fnTeamID = T.fnTeamID AND TU.fnCurrentTeam = 1 AND TU.fnUserID = " + getUserID() + " ORDER BY TU.fdJoined ASC LIMIT 0,1");
            m_lastQuery = System.currentTimeMillis();

            if (qryPlayerSquadInfo != null && qryPlayerSquadInfo.next()) {
                m_fnTeamID = qryPlayerSquadInfo.getInt("fnTeamID");
                m_fcTeamName = qryPlayerSquadInfo.getString("fcTeamName");
                m_fnTeamUserID = qryPlayerSquadInfo.getInt("fnTeamUserID");
                m_fdTeamSignedUp = qryPlayerSquadInfo.getDate("fdJoined");
                result = true;
            }
            m_connection.SQLClose( qryPlayerSquadInfo );
            return result;
        } catch (Exception e) {
            System.out.println("Database error! - " + e.getMessage());
            return false;
        }
    }


    public boolean getPlayerData() {
        boolean result = false;

        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_aliasConnName = "local";

        try {
            ResultSet qryPlayerInfo = m_connection.SQLQuery(m_connName,
            "SELECT U.fnUserID, U.fcUserName, U.fdSignedUp FROM tblUser U WHERE U.fcUserName = '"+Tools.addSlashes(m_fcUserName)+"' ORDER BY U.fdSignedUp ASC LIMIT 0,1");
            m_lastQuery = System.currentTimeMillis();
            if (qryPlayerInfo != null && qryPlayerInfo.next()) {
                m_playerLoaded = true;
                m_fcUserName = qryPlayerInfo.getString("fcUserName");
                m_fnUserID = qryPlayerInfo.getInt("fnUserID");
                m_fdSignedUp = qryPlayerInfo.getDate("fdSignedUp");

                getPlayerAliasData();
                getPlayerSquadData();
                result = true;
            }
            m_connection.SQLClose( qryPlayerInfo );
            return result;
        } catch (Exception e) {
            System.out.println("Database error! - " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the player exists in the database<br/>
     * <br/>
     * WARNING: If the playername in the database is 23 characters long (the name from alias *info can be that long)
     *          and you are looking for a playername with 19 characters (the name from the playerlist (m_botAction.getPlayerName())
     *          then this method will return false while the player IS saved, but under a different name
     * @return
     */
    public boolean checkPlayerExists() {
        boolean result = false;

        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_connName = "website";

        try {
            ResultSet qryPlayerExist = m_connection.SQLQuery(m_connName,
            "SELECT U.fnUserID FROM tblUser U WHERE U.fcUserName = '"+Tools.addSlashes(m_fcUserName)+"' ORDER BY U.fdSignedUp ASC LIMIT 0,1");
            m_lastQuery = System.currentTimeMillis();
            if (qryPlayerExist != null && qryPlayerExist.next()) {
                result = true;
            }
            m_connection.SQLClose( qryPlayerExist );
            return result;
        } catch (Exception e) {
            System.out.println("Database error! - " + e.getMessage());
            return false;
        }
    }


    public boolean getPlayerAliasData() {
        boolean result = false;
        try {
            ResultSet qryPlayerAlias = m_connection.SQLQuery( m_aliasConnName,
            "SELECT fcIP, fnMID, fnStatus FROM tblAliasSuppression WHERE fnUserID = "+m_fnUserID+" ORDER BY fnUserID ASC LIMIT 0,1");
            if( qryPlayerAlias != null && qryPlayerAlias.next() ) {
                m_fcIP = qryPlayerAlias.getString( "fcIP" );
                m_fnMID = qryPlayerAlias.getInt( "fnMID" );
                m_fnStatus = qryPlayerAlias.getInt( "fnStatus" );
                result = true;
            }
            m_connection.SQLClose( qryPlayerAlias );
            return result;
        } catch (Exception e) {
            System.out.println("Database error! - " + e.getMessage());
            return false;
        }
    }


    public boolean createPlayerData() {
        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_connName = "website";

           try {
        	   String query = "INSERT INTO tblUser (fcUserName, fdSignedUp) VALUES ('"+Tools.addSlashes(m_fcUserName)+"', NOW())";
               m_connection.SQLQueryAndClose(m_connName, query );
               m_lastQuery = System.currentTimeMillis();
               return getPlayerData();
           } catch (SQLException e) {
               System.out.println("Couldn't create user");
               return false;
           }
    }



    public boolean createPlayerAccountData(String fcPassword) {
        // FIXME: Seriously, wtf, why would you hack the connection name here
        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_connName = "website";

        if (m_fnUserID != 0) {
            try {
                m_connection.SQLQueryAndClose(m_connName, "INSERT INTO tblUserAccount(fnUserID, fcPassword) VALUES ("+m_fnUserID+",PASSWORD('"+Tools.addSlashes(fcPassword)+"'))");
                m_lastQuery = System.currentTimeMillis();
                m_fcPassword = fcPassword;
                return true;
            } catch (Exception e) {
                System.out.println("Couldn't create useraccount");
                return false;
            }
        } else return false;
    }


    public boolean updatePlayerAccountData(String fcPassword) {
        // FIXME: Seriously, wtf, why would you hack the connection name here
        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_connName = "website";

        if (m_fnUserID != 0) {
            try {
                m_connection.SQLQueryAndClose(m_connName, "UPDATE tblUserAccount SET fcPassword = PASSWORD('"+Tools.addSlashes(fcPassword)+"') WHERE fnUserID = "+m_fnUserID);
                m_lastQuery = System.currentTimeMillis();
                m_fcPassword = fcPassword;
                return true;
            } catch (Exception e) {
                System.out.println("Couldn't update useraccount");
                return false;
            }
        } else return false;
    }

    public boolean getPlayerAccountData() {
        if (m_fnUserID != 0) {
            boolean result = false;
            try {
                ResultSet qryPlayerAccountInfo = m_connection.SQLQuery(m_connName, "SELECT fcPassword FROM tblUserAccount WHERE fnUserID="+m_fnUserID+" ORDER BY fnUserID ASC LIMIT 0,1");
                m_lastQuery = System.currentTimeMillis();
                if (qryPlayerAccountInfo.next()) {
                    m_fcPassword = qryPlayerAccountInfo.getString("fcPassword");
                    result = true;
                }
                m_connection.SQLClose( qryPlayerAccountInfo );
                return result;
            } catch (Exception e) {

            };
        };
        return false;
    }


    public boolean hasRank(int rankNr) {
        if (m_fnUserID != 0) {
            if( m_rank == -1 ) {    // If -1, rank has not been fetched; fetch it and save it
                try {
                    ResultSet qryHasPlayerRank = m_connection.SQLQuery(m_connName, "SELECT fnRankID FROM tblUserRank WHERE fnUserID = "+m_fnUserID+" AND fnRankID >=" + rankNr + " ORDER BY fnRankID DESC");
                    m_lastQuery = System.currentTimeMillis();
                    if( qryHasPlayerRank != null && qryHasPlayerRank.next() ) {
                        m_rank = qryHasPlayerRank.getInt("fnRankID");
                    }
                    m_connection.SQLClose( qryHasPlayerRank );
                } catch (Exception e) {
                }
            }
            return (m_rank == rankNr);
        }
        return false;
    }

    /**
     * @return True if player is at least of rank assistant.
     */
    public boolean isRankAssistantMinimum() {
        if (m_fnUserID != 0) {
            if( m_rank == -1 ) {    // If -1, rank has not been fetched; fetch it and save it
                try {
                    ResultSet qryHasPlayerRank = m_connection.SQLQuery(m_connName, "SELECT fnRankID FROM tblUserRank WHERE fnUserID = "+m_fnUserID+" AND fnRankID >= 3 ORDER BY fnRankID DESC");
                    m_lastQuery = System.currentTimeMillis();
                    if( qryHasPlayerRank != null && qryHasPlayerRank.next() ) {
                        m_rank = qryHasPlayerRank.getInt("fnRankID");
                    }
                    m_connection.SQLClose( qryHasPlayerRank );
                } catch (Exception e) {
                }
            }
            return (m_rank >= 3);
        }
        return false;
    }

    public boolean giveRank(int rankNr) {
        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_connName = "website";

        if (m_fnUserID != 0) {
            try {
                m_connection.SQLQueryAndClose(m_connName, "INSERT tblUserRank (fnUserID, fnRankID) VALUES ("+m_fnUserID+", "+rankNr+")");
                m_lastQuery = System.currentTimeMillis();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public boolean removeRank( int userRankId ) {
        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_connName = "website";

        if(m_fnUserID != 0) {
            try {
                String query = "DELETE FROM tblUserRank WHERE fnUserRankId = "+userRankId;
                m_connection.SQLQueryAndClose(m_connName, query );
                m_connection.SQLQueryAndClose(m_connName, "INSERT tblDeleteLog (fcDeleteQuery) VALUES ('"+query+"')" );
                m_lastQuery = System.currentTimeMillis();
                return true;
            } catch (Exception e) {
                return false;
            }
        };
        return false;
    };

    public boolean register( String ip, String mid ) {
        if( m_fnUserID != 0 ) {
            try {
                String query = "INSERT INTO tblAliasSuppression (fnUserID, fcIP, fnMID, fnStatus) VALUES ";
                query += "("+m_fnUserID+", '"+ip+"', '"+mid+"', 1)";
                m_connection.SQLQueryAndClose( m_aliasConnName, query );
                m_lastQuery = System.currentTimeMillis();
                return true;
            } catch (Exception e) {
                System.out.println( e );
                return false;
            }
        }
        return false;
    }

    public boolean aliasMatch( String ip, String mid ) {
        ip = ip.substring( 0, ip.lastIndexOf( "." ) )+".%";
        boolean result = false;

        try {
            String query = "SELECT fnAliasID FROM tblAliasSuppression WHERE fcIP LIKE '"+ip+"' AND fnMID = '"+mid+"'";
            ResultSet r = m_connection.SQLQuery( m_aliasConnName, query );
            if( r.next() ) result = true;
            m_connection.SQLClose( r );
            return result;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * This method checks for EITHER the MID or IP being in the DB.
     * @param ip
     * @param mid
     * @return
     */
    public boolean aliasMatchCrude( String ip, String mid ) {
        ip = ip.substring( 0, ip.lastIndexOf( "." ) )+".%";
        boolean result = false;

        try {
            String query = "SELECT fnAliasID FROM tblAliasSuppression WHERE fcIP LIKE '"+ip+"'";
            ResultSet r = m_connection.SQLQuery( m_aliasConnName, query );
            if( r.next() ) result = true;
            m_connection.SQLClose( r );
            if( result )
                return result;
            String query2 = "SELECT fnAliasID FROM tblAliasSuppression WHERE fnMID = '"+mid+"'";
            ResultSet r2 = m_connection.SQLQuery( m_aliasConnName, query2 );
            if( r2.next() ) result = true;
            m_connection.SQLClose( r2 );
            return result;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean resetRegistration() {

        try {
            if( m_fnStatus == 2 )
                return false;
            m_connection.SQLQueryAndClose( m_aliasConnName, "DELETE FROM tblAliasSuppression WHERE fnUserID = "+m_fnUserID );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean enableName() {

        try {
            m_connection.SQLQueryAndClose( m_aliasConnName, "UPDATE tblAliasSuppression SET fnStatus = 1 WHERE fnUserID = "+m_fnUserID );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean disableName() {

        try {
            m_connection.SQLQueryAndClose( m_aliasConnName, "UPDATE tblAliasSuppression SET fnStatus = 2 WHERE fnUserID = "+m_fnUserID );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRegistered() {
        if( m_fnStatus != 0 ) return true;
        else return false;
    }

    public boolean isEnabled() {
        if( m_fnStatus == 1 ) return true;
        else return false;
    }

    public boolean hasBeenDisabled() {
        if( m_fnStatus == 2 ) return true;
        else return false;
    }

    public BotAction getBotAction() { return m_connection; };

    public boolean playerLoaded() { return m_playerLoaded; };
    public int getUserID() { return m_fnUserID; };
    public String getUserName() { return m_fcUserName; };
    public String getPassword() { return m_fcPassword; };
    public String getIP() { return m_fcIP; }
    public int getMID() { return m_fnMID; }
    public int getStatus() { return m_fnStatus; }
    public java.util.Date getSignedUp() { return (java.util.Date)m_fdSignedUp; };
    public java.util.Date getTeamSignedUp() { return (java.util.Date)m_fdTeamSignedUp; };
    public String getTeamName() { return m_fcTeamName; };
    public int getTeamID() { return m_fnTeamID; };
    public int getTeamUserID() { return m_fnTeamUserID; };
    public long getLastQuery() { return m_lastQuery; };

    public void setUserName(String fcUserName) { m_fcUserName = fcUserName; };
};

