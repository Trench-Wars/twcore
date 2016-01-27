package twcore.bots.duel2bot;

import java.sql.ResultSet;

import twcore.core.BotAction;
import twcore.core.util.Tools;

/**
    Simplified DBPlayerData class takes care of tblUser data.
*/
public class UserData {

    BotAction ba;

    boolean playerLoaded = false;
    String connName = "website";
    String fcUserName = "";
    String fcPassword = "";
    int fnUserID = 0;
    java.sql.Date fdSignedUp = null;

    int fnTeamUserID = 0;
    int fnTeamID = 0;
    java.sql.Date fdTeamSignedUp = null;
    String fcTeamName = "";

    long lastQuery = 0;

    /** Creates a new instance of DBPlayerData
        @param conn BotAction
        @param connName String
     * */

    public UserData(BotAction conn, String connName) {
        ba = conn;
        this.connName = connName;
    }

    public UserData(BotAction conn, String connName, String fcPlayerName) {
        ba = conn;
        fcUserName = fcPlayerName;
        this.connName = connName;
        getPlayerData();
    }

    public UserData(BotAction conn, String connName, String fcPlayerName, boolean createIfNotExists) {
        ba = conn;
        fcUserName = fcPlayerName;
        this.connName = connName;

        if (!getPlayerData() && createIfNotExists) createPlayerData();
    }

    public boolean getPlayerSquadData() {
        try {
            ResultSet qryPlayerSquadInfo = ba
                                           .SQLQuery(connName,
                                                   "SELECT TU.fdJoined, TU.fnTeamUserID, T.fnTeamID, T.fcTeamName FROM tblTeam T, tblTeamUser TU "
                                                   + "WHERE TU.fnTeamID = T.fnTeamID AND TU.fnCurrentTeam = 1 AND TU.fnUserID = "
                                                   + getUserID());
            lastQuery = System.currentTimeMillis();

            if (qryPlayerSquadInfo.next()) {
                fnTeamUserID = qryPlayerSquadInfo.getInt("fnTeamUserID");
                fdTeamSignedUp = qryPlayerSquadInfo.getDate("fdJoined");
                ba.SQLClose(qryPlayerSquadInfo);
                return true;
            } else {
                ba.SQLClose(qryPlayerSquadInfo);
                return false;
            }
        } catch (Exception e) {
            Tools.printLog("Database error on player " + fcUserName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean getPlayerData() {
        try {
            ResultSet qryPlayerInfo = ba.SQLQuery(connName,
                                                  "SELECT U.fnUserID, U.fcUserName, U.fdSignedUp FROM tblUser U WHERE U.fcUserName = '"
                                                  + Tools.addSlashesToString(fcUserName) + "' ORDER BY U.fnUserID ASC");
            lastQuery = System.currentTimeMillis();

            if (qryPlayerInfo.next()) {
                playerLoaded = true;
                fcUserName = qryPlayerInfo.getString("fcUserName");
                fnUserID = qryPlayerInfo.getInt("fnUserID");
                fdSignedUp = qryPlayerInfo.getDate("fdSignedUp");
                ba.SQLClose(qryPlayerInfo);
                getPlayerSquadData();
                return true;
            } else {
                ba.SQLClose(qryPlayerInfo);
                return false;
            }
        } catch (Exception e) {
            Tools.printLog("Database error on player " + fcUserName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean createPlayerData() {
        try {
            ba.SQLQueryAndClose(connName,
                                "INSERT INTO tblUser (fcUserName, fdSignedUp) VALUES ('"
                                + Tools.addSlashesToString(fcUserName)
                                + "', NOW())");
            lastQuery = System.currentTimeMillis();

            if (getPlayerData())
                return true;
            else
                return false;
        } catch (Exception e) {
            Tools.printLog("Couldn't create user: " + fcUserName);
            return false;
        }
    }

    public boolean updatePlayerAccountData(String fcPassword) {
        if (fnUserID != 0)
            try {
                ba.SQLQueryAndClose(connName,
                                    "UPDATE tblUserAccount SET fcPassword = PASSWORD('"
                                    + Tools.addSlashesToString(fcPassword)
                                    + "') WHERE fnUserID = " + fnUserID);
                lastQuery = System.currentTimeMillis();
                this.fcPassword = fcPassword;
                return true;
            } catch (Exception e) {
                Tools.printLog("Couldn't update useraccount");
                return false;
            }
        else
            return false;
    }

    public boolean getPlayerAccountData() {
        if (fnUserID != 0) {
            try {
                ResultSet qryPlayerAccountInfo = ba.SQLQuery(connName,
                                                 "SELECT fcPassword FROM tblUserAccount WHERE fnUserID=" + fnUserID);
                lastQuery = System.currentTimeMillis();

                if (qryPlayerAccountInfo.next()) {
                    fcPassword = qryPlayerAccountInfo.getString("fcPassword");
                    ba.SQLClose(qryPlayerAccountInfo);
                    return true;
                } else {
                    ba.SQLClose(qryPlayerAccountInfo);
                    return false;
                }
            } catch (Exception e) {
            }
        }

        return false;
    }

    public boolean hasRank(int rankNr) {
        if (fnUserID != 0) {
            try {
                boolean hasRank = false;
                ResultSet qryHasPlayerRank = ba.SQLQuery(connName,
                                             "SELECT fnUserID FROM tblUserRank WHERE fnUserID = "
                                             + fnUserID + " AND fnRankID =" + rankNr);
                lastQuery = System.currentTimeMillis();

                if (qryHasPlayerRank.next()) hasRank = true;

                ba.SQLClose(qryHasPlayerRank);
                return hasRank;
            } catch (Exception e) {
            }
        }

        return false;
    }

    public boolean giveRank(int rankNr) {
        if (fnUserID != 0) try {
                ba.SQLQueryAndClose(connName,
                                    "INSERT tblUserRank (fnUserID, fnRankID) VALUES ("
                                    + fnUserID + ", " + rankNr + ")");
                lastQuery = System.currentTimeMillis();
                return true;
            } catch (Exception e) {
            }

        return false;
    }

    public BotAction getBotAction() {
        return ba;
    }

    public boolean playerLoaded() {
        return playerLoaded;
    }

    public int getUserID() {
        return fnUserID;
    }

    public String getUserName() {
        return fcUserName;
    }

    public String getPassword() {
        return fcPassword;
    }

    public java.util.Date getSignedUp() {
        return fdSignedUp;
    }

    public java.util.Date getTeamSignedUp() {
        return fdTeamSignedUp;
    }

    public long getLastQuery() {
        return lastQuery;
    }

    public void setUserName(String fcUserName) {
        this.fcUserName = fcUserName;
    }
}
