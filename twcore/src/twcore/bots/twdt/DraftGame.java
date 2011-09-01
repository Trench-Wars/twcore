package twcore.bots.twdt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.util.Tools;

/**
 *
 * @author WingZero
 */
public class DraftGame {

    public static final String db = "website";
    
    twdt bot;
    BotAction ba;
    OperatorList opList;
    BotSettings rules;
    
    public enum GameType { 
        NA, WARBIRD, JAVELIN, BASING;
    
        static GameType getType(int n) {
            switch(n) {
                case 1: return WARBIRD;
                case 2: return JAVELIN;
                case 3: return BASING;
                default: return NA;
            }
        }
    }
    
    GameType type;
    
    LinkedList<DraftRound> oldRounds;
    DraftRound currentRound;
    
    int gameID;
    int round;
    int team1;
    int team2;
    int maxPlayers;
    int minPlayers;
    int maxDeaths;
    int gameTime;
    int[] ships;
    String host, team1Name, team2Name;
    
    public DraftGame(twdt bot) {
        this.bot = bot;
        ba = bot.ba;
        opList = bot.opList;
        rules = bot.rules;
        type = bot.type;
        gameID = -1;
        round = 0;
        ships = rules.getIntArray("Ship", ",");
        gameTime = rules.getInt("Time");
        maxPlayers = rules.getInt("Max");
        minPlayers = rules.getInt("Min");
    }

    public void cmd_loadGame(String name, String cmd) {
        try {
            gameID = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1));
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(name, "Error, please use !load <gameID>");
            return;
        }
        if (gameID < 1) return;
        
        String query = "SELECT * FROM tblDraft__Match WHERE fnMatchID = " + gameID + " LIMIT 1";
        try {
            ResultSet rs = ba.SQLQuery(db, query);
            if (rs.next()) {
                if (type != GameType.getType(rs.getInt("fnType"))) {
                    ba.sendSmartPrivateMessage(name, "Error, arena game type and match game type do not match!");
                    gameID = -1;
                } else {
                    team1 = rs.getInt("fnTeam1");
                    team2 = rs.getInt("fnTeam2");
                    team2Name = rs.getString("fcTeam1");
                    team2Name = rs.getString("fcTeam2");
                    host = name;
                    ba.sendSmartPrivateMessage(name, "Success!");
                }
            } else {
                ba.sendSmartPrivateMessage(name, "No game was found with ID: " + gameID);
                gameID = -1;
            }
            ba.SQLClose(rs);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }        
    }
    
    public void cmd_startPick() {
        round = 1;
        ba.sendArenaMessage("" + type.toString() + " Draft Game: " + team1Name + " vs. " + team2Name);
        currentRound = new DraftRound(this, type, team1, team2, team1Name, team2Name);
    }

    public boolean getLoaded() {
        if (gameID > -1)
            return true;
        else return false;
    }
    
    public void handleRound() {
        
    }
    
    public GameType getType() {
        return type;
    }
}
