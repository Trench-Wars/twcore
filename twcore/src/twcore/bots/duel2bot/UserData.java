package twcore.bots.duel2bot;

import java.sql.ResultSet;

import twcore.core.BotAction;
import twcore.core.util.Tools;

/**
 * Simplified DBPlayerData class takes care of tblUser data.
 */
public class UserData {

    BotAction m_connection;

    boolean m_playerLoaded = false;
    String m_connName = "website";
    String m_fcUserName = "";
    String m_fcPassword = "";
    int m_fnUserID = 0;
    java.sql.Date m_fdSignedUp = null;

    int m_fnTeamUserID = 0;
    int m_fnTeamID = 0;
    java.sql.Date m_fdTeamSignedUp = null;
    String m_fcTeamName = "";

    long m_lastQuery = 0;

    /** Creates a new instance of DBPlayerData */

    public UserData(BotAction conn, String connName) {
        m_connection = conn;
        m_connName = connName;
    }

    public UserData(BotAction conn, String connName, String fcPlayerName) {
        m_connection = conn;
        m_fcUserName = fcPlayerName;
        m_connName = connName;
        getPlayerData();
    }

    public UserData(BotAction conn, String connName, String fcPlayerName, boolean createIfNotExists) {
        m_connection = conn;
        m_fcUserName = fcPlayerName;
        m_connName = connName;
        if (!getPlayerData() && createIfNotExists) createPlayerData();
    }

    public boolean getPlayerSquadData() {
        try {
            ResultSet qryPlayerSquadInfo = m_connection
                    .SQLQuery(m_connName,
                            "SELECT TU.fdJoined, TU.fnTeamUserID, T.fnTeamID, T.fcTeamName FROM tblTeam T, tblTeamUser TU "
                                    + "WHERE TU.fnTeamID = T.fnTeamID AND TU.fnCurrentTeam = 1 AND TU.fnUserID = "
                                    + getUserID());
            m_lastQuery = System.currentTimeMillis();

            if (qryPlayerSquadInfo.next()) {
                m_fnTeamUserID = qryPlayerSquadInfo.getInt("fnTeamUserID");
                m_fdTeamSignedUp = qryPlayerSquadInfo.getDate("fdJoined");
                m_connection.SQLClose(qryPlayerSquadInfo);
                return true;
            } else {
                m_connection.SQLClose(qryPlayerSquadInfo);
                return false;
            }
        } catch (Exception e) {
            Tools.printLog("Database error on player " + m_fcUserName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean getPlayerData() {
        try {
            ResultSet qryPlayerInfo = m_connection.SQLQuery(m_connName,
                            "SELECT U.fnUserID, U.fcUserName, U.fdSignedUp FROM tblUser U WHERE U.fcUserName = '"
                                    + Tools.addSlashesToString(m_fcUserName) + "'");
            m_lastQuery = System.currentTimeMillis();
            if (qryPlayerInfo.next()) {
                m_playerLoaded = true;
                m_fcUserName = qryPlayerInfo.getString("fcUserName");
                m_fnUserID = qryPlayerInfo.getInt("fnUserID");
                m_fdSignedUp = qryPlayerInfo.getDate("fdSignedUp");
                m_connection.SQLClose(qryPlayerInfo);
                getPlayerSquadData();
                return true;
            } else {
                m_connection.SQLClose(qryPlayerInfo);
                return false;
            }
        } catch (Exception e) {
            Tools.printLog("Database error on player " + m_fcUserName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean createPlayerData() {
        try {
            m_connection.SQLQueryAndClose(m_connName,
                    "INSERT INTO tblUser (fcUserName, fdSignedUp) VALUES ('"
                            + Tools.addSlashesToString(m_fcUserName)
                            + "', NOW())");
            m_lastQuery = System.currentTimeMillis();
            if (getPlayerData())
                return true;
            else
                return false;
        } catch (Exception e) {
            Tools.printLog("Couldn't create user: " + m_fcUserName);
            return false;
        }
    }

    public boolean updatePlayerAccountData(String fcPassword) {
        if (m_fnUserID != 0)
            try {
                m_connection.SQLQueryAndClose(m_connName,
                        "UPDATE tblUserAccount SET fcPassword = PASSWORD('"
                                + Tools.addSlashesToString(fcPassword)
                                + "') WHERE fnUserID = " + m_fnUserID);
                m_lastQuery = System.currentTimeMillis();
                m_fcPassword = fcPassword;
                return true;
            } catch (Exception e) {
                Tools.printLog("Couldn't update useraccount");
                return false;
            }
        else
            return false;
    }

    public boolean getPlayerAccountData() {
        if (m_fnUserID != 0) {
            try {
                ResultSet qryPlayerAccountInfo = m_connection.SQLQuery(m_connName,
                        "SELECT fcPassword FROM tblUserAccount WHERE fnUserID=" + m_fnUserID);
                m_lastQuery = System.currentTimeMillis();
                if (qryPlayerAccountInfo.next()) {
                    m_fcPassword = qryPlayerAccountInfo.getString("fcPassword");
                    m_connection.SQLClose(qryPlayerAccountInfo);
                    return true;
                } else {
                    m_connection.SQLClose(qryPlayerAccountInfo);
                    return false;
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    public boolean hasRank(int rankNr) {
        if (m_fnUserID != 0) {
            try {
                boolean hasRank = false;
                ResultSet qryHasPlayerRank = m_connection.SQLQuery(m_connName,
                        "SELECT fnUserID FROM tblUserRank WHERE fnUserID = "
                                + m_fnUserID + " AND fnRankID =" + rankNr);
                m_lastQuery = System.currentTimeMillis();
                if (qryHasPlayerRank.next()) hasRank = true;
                m_connection.SQLClose(qryHasPlayerRank);
                return hasRank;
            } catch (Exception e) {
            }
        }
        return false;
    }

    public boolean giveRank(int rankNr) {
        if (m_fnUserID != 0) try {
            m_connection.SQLQuery(m_connName,
                    "INSERT tblUserRank (fnUserID, fnRankID) VALUES ("
                            + m_fnUserID + ", " + rankNr + ")");
            m_lastQuery = System.currentTimeMillis();
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public BotAction getBotAction() {
        return m_connection;
    }

    public boolean playerLoaded() {
        return m_playerLoaded;
    }

    public int getUserID() {
        return m_fnUserID;
    }

    public String getUserName() {
        return m_fcUserName;
    }

    public String getPassword() {
        return m_fcPassword;
    }

    public java.util.Date getSignedUp() {
        return m_fdSignedUp;
    }

    public java.util.Date getTeamSignedUp() {
        return m_fdTeamSignedUp;
    }

    public long getLastQuery() {
        return m_lastQuery;
    }

    public void setUserName(String fcUserName) {
        m_fcUserName = fcUserName;
    }
}
