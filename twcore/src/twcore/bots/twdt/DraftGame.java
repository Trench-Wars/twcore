package twcore.bots.twdt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FlagReward;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.TurretEvent;
import twcore.core.events.WeaponFired;
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
                case 0: return BASING;
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
    DraftTeam winner;
    
    int gameID;
    int round;
    int team1, team1score;
    int team2, team2score;
    int maxPlayers;
    int minPlayers;
    int maxDeaths;
    int gameTime;
    int[] ships;
    String host, team1Name, team2Name;
    
    public DraftGame(twdt bot, int id, int team1id, int team2id, String name1, String name2, String staffer) {
        this.bot = bot;
        ba = bot.ba;
        opList = bot.opList;
        rules = bot.rules;
        type = bot.type;
        gameID = id;
        round = 0;
        team1 = team1id;
        team2 = team2id;
        team1Name = name1;
        team2Name = name2;
        host = staffer;
        team1score = 0;
        team2score = 0;
        winner = null;
        ships = rules.getIntArray("Ship", ",");
        gameTime = rules.getInt("Time");
        maxPlayers = rules.getInt("MaxPlayers");
        minPlayers = rules.getInt("MinPlayers");
    }
    
    public void handleEvent(PlayerDeath event) {
        if (currentRound != null)
            currentRound.handleEvent(event);
    }

    public void handleEvent(PlayerEntered event) {
        String name = event.getPlayerName();
        if (name == null) return;
        
        if (currentRound != null) {
            if (type == GameType.WARBIRD)
                ba.sendSmartPrivateMessage(name, "Welcome to TWDTD! " + getStatus());
            else if (type == GameType.JAVELIN)
                ba.sendSmartPrivateMessage(name, "Welcome to TWDTJ! " + getStatus());
            else if (type == GameType.BASING)
                ba.sendSmartPrivateMessage(name, "Welcome to TWDTB! " + getStatus());
        }
    }

    public void handleEvent(PlayerLeft event) {
        if (currentRound != null)
            currentRound.handleEvent(event);
    }

    public void handleEvent(WeaponFired event) {
        if (currentRound != null)
            currentRound.handleEvent(event);
    }

    public void handleEvent(FlagClaimed event) {
        if (currentRound != null)
            currentRound.handleEvent(event);
    }

    public void handleEvent(FlagReward event) {
        if (currentRound != null)
            currentRound.handleEvent(event);
    }
    
    public void handleEvent(TurretEvent event) {
        if (currentRound != null)
            currentRound.handleEvent(event);
    }

    public void handleEvent(FrequencyShipChange event) {
        if (currentRound != null)
            currentRound.handleEvent(event);
    }

    public void handleEvent(PlayerPosition event) {
        if (currentRound != null)
            currentRound.handleEvent(event);
    }
    
    public void handleEvent(Message event) {

        if (event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE) {
            String name = ba.getPlayerName(event.getPlayerID());
            if (name == null)
                name = event.getMessager();
            if (name == null) return;
            String msg = event.getMessage();
            
            if (msg.equals("!status"))
                cmd_status(name);
            
            if (opList.isModerator(name)) {
                if (msg.startsWith("!start"))
                    cmd_startPick(name);
            }
        }
        
        if (currentRound != null)
            currentRound.handleEvent(event);
    }
    
    public void cmd_status(String name) {
        if (currentRound != null) 
            ba.sendSmartPrivateMessage(name, getStatus());
    }
    
    private String getStatus() {
        if (currentRound != null) {
            String msg = "Teams: " + team1Name + " vs. " + team2Name + " ";
            msg += currentRound.getStatus();
            if (type != GameType.BASING) 
                msg += "Score: " + team1score + " - " + team2score;
            return msg;
        }
        return "";
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
                type = GameType.getType(rs.getInt("fnType"));
                team1 = rs.getInt("fnTeam1");
                team2 = rs.getInt("fnTeam2");
                team1Name = rs.getString("fcTeam1");
                team2Name = rs.getString("fcTeam2");
                host = name;
                ba.sendSmartPrivateMessage(name, "Success!");
            } else {
                ba.sendSmartPrivateMessage(name, "No game was found with ID: " + gameID);
                gameID = -1;
            }
            ba.SQLClose(rs);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }        
    }
    
    public void cmd_startPick(String name) {
        if (currentRound != null) {
            ba.sendSmartPrivateMessage(name, "This game has already been started.");
            return;
        }
        round = 1;
        ba.sendArenaMessage("" + type.toString() + " Draft Game: " + team1Name + " vs. " + team2Name);
        currentRound = new DraftRound(this, type, team1, team2, team1Name, team2Name);
    }
    
    public int getMatchID() {
        return gameID;
    }

    public boolean getLoaded() {
        if (gameID > -1)
            return true;
        else return false;
    }
    
    public void handleRound(DraftTeam team) {
        if (type != GameType.BASING) {
            if (team != null) {
                if (team.getID() == team1) {
                    team1score++;
                    if (team1score > 1) {
                        winner = team;
                        currentRound.gameOver();
                        ba.sendArenaMessage(team1Name + " wins this game! ");
                        storeResult();
                    } else {
                        round++;
                        ba.sendArenaMessage("Current game standing of " + team1Name + " vs. " + team2Name + ": " + team1score + " - " + team2score);
                        ba.sendArenaMessage("Prepare for round " + round + "!");
                        oldRounds.add(currentRound);
                        currentRound = new DraftRound(this, type, team1, team2, team1Name, team2Name);
                    }
                } else {
                    team2score++;
                    if (team2score > 1) {
                        winner = team;
                        currentRound.gameOver();
                        ba.sendArenaMessage(team2Name + " wins this game! ");
                        storeResult();
                    } else {
                        round++;
                        ba.sendArenaMessage("Current game standing of " + team1Name + " vs. " + team2Name + ": " + team1score + " - " + team2score);
                        ba.sendArenaMessage("Prepare for round " + round + "!");
                        oldRounds.add(currentRound);
                        currentRound = new DraftRound(this, type, team1, team2, team1Name, team2Name);
                    }
                }
            }
        } else {
            winner = team;
            currentRound.gameOver();
            team1score = currentRound.team1.getScore();
            team2score = currentRound.team2.getScore();
            storeResult();
        }
    }
    
    private void storeResult() {
        try {
            String query = "UPDATE tblDraft__Match SET fnTeam1Score = " + team1score + ", fnTeam2Score = " + team2score + " WHERE fnMatchID = " + gameID;
            ba.SQLQueryAndClose(db, query);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }
    
    public GameType getType() {
        return type;
    }
}
