/*
 * DBPlayerData.java
 *
 * Created on June 12, 2002, 10:43 PM
 */

/**
 *
 * @author  Mythrandir
 */

package twcore.misc.database;

import twcore.core.*;
import java.sql.*;

public class DBPlayerData {
    BotAction m_connection;

    boolean m_playerLoaded = false;
    String m_connName = "local";
    String m_aliasConnName = "server";
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
    int m_fnStatus = 0;

    long m_lastQuery = 0;


    /** Creates a new instance of DBPlayerData */

    public DBPlayerData(BotAction conn, String connName) {
        m_connection = conn;
        m_connName = connName;
    };


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

            if (qryPlayerSquadInfo.next()) {
                m_fnTeamID = qryPlayerSquadInfo.getInt("fnTeamID");
                m_fcTeamName = qryPlayerSquadInfo.getString("fcTeamName");
                m_fnTeamUserID = qryPlayerSquadInfo.getInt("fnTeamUserID");
                m_fdTeamSignedUp = qryPlayerSquadInfo.getDate("fdJoined");
                result = true;
            }
            qryPlayerSquadInfo.close();
            return result;
        } catch (Exception e) {
            System.out.println("Database error! - " + e.getMessage());
            return false;
        }
    };


    public boolean getPlayerData() {
        boolean result = false;

        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_aliasConnName = "local";

        try {
            ResultSet qryPlayerInfo = m_connection.SQLQuery(m_connName,
            "SELECT U.fnUserID, U.fcUserName, U.fdSignedUp FROM tblUser U WHERE U.fcUserName = '"+Tools.addSlashesToString(m_fcUserName)+"' ORDER BY U.fdSignedUp ASC LIMIT 0,1");
            m_lastQuery = System.currentTimeMillis();
            if (qryPlayerInfo.next()) {
                m_playerLoaded = true;
                m_fcUserName = qryPlayerInfo.getString("fcUserName");
                m_fnUserID = qryPlayerInfo.getInt("fnUserID");
                m_fdSignedUp = qryPlayerInfo.getDate("fdSignedUp");

                getPlayerAliasData();
                getPlayerSquadData();
                result = true;
            }
            qryPlayerInfo.close();
            return result;
        } catch (Exception e) {
            System.out.println("Database error! - " + e.getMessage());
            return false;
        }
    };

    public boolean checkPlayerExists() {
        boolean result = false;

        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_connName = "website";

        try {
            ResultSet qryPlayerExist = m_connection.SQLQuery(m_connName,
            "SELECT U.fnUserID FROM tblUser U WHERE U.fcUserName = '"+Tools.addSlashesToString(m_fcUserName)+"' ORDER BY U.fdSignedUp ASC LIMIT 0,1");
            m_lastQuery = System.currentTimeMillis();
            if (qryPlayerExist.next()) {
                result = true;
            }
            qryPlayerExist.close();
            return result;
        } catch (Exception e) {
            System.out.println("Database error! - " + e.getMessage());
            return false;
        }
    };


    public boolean getPlayerAliasData() {
        boolean result = false;
        try {
            ResultSet qryPlayerAlias = m_connection.SQLQuery( m_aliasConnName,
            "SELECT fcIP, fnMID, fnStatus FROM tblAliasSuppression WHERE fnUserID = "+m_fnUserID+" ORDER BY fnUserID ASC LIMIT 0,1");
            if( qryPlayerAlias.next() ) {
                m_fcIP = qryPlayerAlias.getString( "fcIP" );
                m_fnMID = qryPlayerAlias.getInt( "fnMID" );
                m_fnStatus = qryPlayerAlias.getInt( "fnStatus" );
                result = true;
            }
            qryPlayerAlias.close();
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
               ResultSet r = m_connection.SQLQuery(m_connName, "INSERT INTO tblUser (fcUserName, fdSignedUp) VALUES ('"+Tools.addSlashesToString(m_fcUserName)+"', NOW())");
               if (r != null) r.close();
               m_lastQuery = System.currentTimeMillis();
               if (getPlayerData()) return true; else return false;
           } catch (Exception e) {
               System.out.println("Couldn't create user");
               return false;
           }
    };



    public boolean createPlayerAccountData(String fcPassword) {
        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_connName = "website";

        if (m_fnUserID != 0) {
            try {
                ResultSet r = m_connection.SQLQuery(m_connName, "INSERT INTO tblUserAccount(fnUserID, fcPassword) VALUES ("+m_fnUserID+",PASSWORD('"+Tools.addSlashesToString(fcPassword)+"'))");
                if (r != null) r.close();
                m_lastQuery = System.currentTimeMillis();
                m_fcPassword = fcPassword;
                return true;
            } catch (Exception e) {
                System.out.println("Couldn't create useraccount");
                return false;
            }
        } else return false;
    };


    public boolean updatePlayerAccountData(String fcPassword) {
        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_connName = "website";

        if (m_fnUserID != 0) {
            try {
                ResultSet r = m_connection.SQLQuery(m_connName, "UPDATE tblUserAccount SET fcPassword = PASSWORD('"+Tools.addSlashesToString(fcPassword)+"') WHERE fnUserID = "+m_fnUserID);
                if (r != null) r.close();
                m_lastQuery = System.currentTimeMillis();
                m_fcPassword = fcPassword;
                return true;
            } catch (Exception e) {
                System.out.println("Couldn't update useraccount");
                return false;
            }
        } else return false;
    };

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
                qryPlayerAccountInfo.close();
                return result;
            } catch (Exception e) {

            };
        };
        return false;
    };


    public boolean hasRank(int rankNr) {
        if (m_fnUserID != 0) {
            boolean result = false;
            try {
                ResultSet qryHasPlayerRank = m_connection.SQLQuery(m_connName, "SELECT fnUserID FROM tblUserRank WHERE fnUserID = "+m_fnUserID+" AND fnRankID =" + rankNr);
                m_lastQuery = System.currentTimeMillis();
                if (qryHasPlayerRank.next()) result = true;
                qryHasPlayerRank.close();
                return result;
            } catch (Exception e) {
            };
        };
        return false;
    };



    public boolean giveRank(int rankNr) {
        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_connName = "website";

        if (m_fnUserID != 0) {
            try {
                ResultSet r = m_connection.SQLQuery(m_connName, "INSERT tblUserRank (fnUserID, fnRankID) VALUES ("+m_fnUserID+", "+rankNr+")");
                if (r != null) r.close();
                m_lastQuery = System.currentTimeMillis();
                return true;
            } catch (Exception e) {
                return false;
            }
        };
        return false;
    };

    public boolean removeRank( int userRankId ) {
        if (m_connection.getCoreData().getGeneralSettings().getString("Server").equals("localhost"))
            m_connName = "website";

        if(m_fnUserID != 0) {
            try {
                String query = "DELETE FROM tblUserRank WHERE fnUserRankId = "+userRankId;
                ResultSet r;
                r = m_connection.SQLQuery(m_connName, query );
                if (r != null) {
                    r.close();
                    r = null;
                }
                r = m_connection.SQLQuery(m_connName, "INSERT tblDeleteLog (fcDeleteQuery) VALUES ('"+query+"')" );
                if (r != null) r.close();
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
                ResultSet r = m_connection.SQLQuery( m_aliasConnName, query );
                if (r != null) r.close();
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
        String pieces[] = ip.split(".");
        ip = ip.substring( 0, ip.lastIndexOf( "." ) )+".%";
        boolean result = false;

        try {
            String query = "SELECT fnAliasID FROM tblAliasSuppression WHERE fcIP LIKE '"+ip+"' AND fnMID = '"+mid+"'";
            ResultSet r = m_connection.SQLQuery( m_aliasConnName, query );
            if( r.next() ) result = true;
            r.close();
            return result;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean resetRegistration() {

        try {
            ResultSet r = m_connection.SQLQuery( m_aliasConnName, "DELETE FROM tblAliasSuppression WHERE fnUserID = "+m_fnUserID );
            if (r != null) r.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean enableName() {

        try {
            ResultSet r = m_connection.SQLQuery( m_aliasConnName, "UPDATE tblAliasSuppression SET fnStatus = 1 WHERE fnUserID = "+m_fnUserID );
            if (r != null) r.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean disableName() {

        try {
            ResultSet r = m_connection.SQLQuery( m_aliasConnName, "UPDATE tblAliasSuppression SET fnStatus = 2 WHERE fnUserID = "+m_fnUserID );
            if (r != null) r.close();
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

