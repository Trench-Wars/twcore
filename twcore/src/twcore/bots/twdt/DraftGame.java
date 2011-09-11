package twcore.bots.twdt;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.TimerTask;

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
    int time;
    int[] ships;
    String host, team1Name, team2Name;
    
    public DraftGame(twdt bot, int id, int team1id, int team2id, String name1, String name2, String staffer) {
        this.bot = bot;
        ba = bot.ba;
        opList = bot.oplist;
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
        oldRounds = new LinkedList<DraftRound>();
        ships = rules.getIntArray("Ship", ",");
        time = rules.getInt("Time");
        maxPlayers = rules.getInt("MaxPlayers");
        minPlayers = rules.getInt("MinPlayers");
        ba.sendArenaMessage("" + type.toString() + " Draft Game: " + team1Name + " vs. " + team2Name);
    }
    
    /** EVENT HANDLERS */
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
            String name = event.getMessager();
            if (name == null)
                name = ba.getPlayerName(event.getPlayerID());
            if (name == null) return;
            String msg = event.getMessage();
            
            if (msg.equals("!status"))
                cmd_status(name);
            
            if (opList.isER(name)) {
                if (msg.startsWith("!start"))
                    cmd_startPick(name);
            }
        }
        
        if (currentRound != null)
            currentRound.handleEvent(event);
    }
    
    /** Handles the status command which displays game status */
    public void cmd_status(String name) {
        if (currentRound != null) 
            ba.sendSmartPrivateMessage(name, getStatus());
    }
    
    /** Handles the start command which starts the lineup setup state */
    public void cmd_startPick(String name) {
        if (currentRound != null) {
            ba.sendSmartPrivateMessage(name, "This game has already been started.");
            return;
        }
        round = 1;
        currentRound = new DraftRound(this, type, team1, team2, team1Name, team2Name);
    }
    
    public int getTime() {
        return time;
    }
    
    /** Returns the id number as given from the database */
    public int getMatchID() {
        return gameID;
    }
    
    public int getRound() {
        return round;
    }
    
    /** Reports the end of a round */
    public void handleRound(DraftTeam team) {
        if (type != GameType.BASING) {
            if (team != null) {
                if (team.getID() == team1) {
                    team1score++;
                    if (team1score > 0) {
                        winner = team;
                        currentRound.gameOver();
                        ba.sendArenaMessage("GAME OVER: " + team1Name + " wins!", 5);
                        storeResult();
                        currentRound = null;
                    } else {
                        round++;
                        ba.sendArenaMessage("ROUND OVER: " + team1Name + " wins!", 5);
                        ba.sendArenaMessage("Current game standing of " + team1Name + " vs. " + team2Name + ": " + team1score + " - " + team2score);
                        oldRounds.add(currentRound);
                        currentRound = null;
                        nextRound();
                    }
                } else {
                    team2score++;
                    if (team2score > 0) {
                        winner = team;
                        currentRound.gameOver();
                        ba.sendArenaMessage("GAME OVER: " + team2Name + " wins!", 5);
                        storeResult();
                        currentRound = null;
                    } else {
                        round++;
                        ba.sendArenaMessage("ROUND OVER: " + team2Name + " wins!", 5);
                        ba.sendArenaMessage("Current game standing of " + team1Name + " vs. " + team2Name + ": " + team1score + " - " + team2score);
                        oldRounds.add(currentRound);
                        currentRound = null;
                        nextRound();
                    }
                }
            }
        } else {
            winner = team;
            currentRound.gameOver();
            ba.sendArenaMessage("GAME OVER: " + winner.getName() + " wins!", 5);
            team1score = currentRound.team1.getScore();
            team2score = currentRound.team2.getScore();
            storeResult();
            oldRounds.add(currentRound);
            currentRound = null;
        }
    }
    
    /** Returns a String with relevant game information */
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
    
    private void nextRound() {
        TimerTask next = new TimerTask() {
            public void run() {
                newRound();
            }
        };
        ba.scheduleTask(next, 4000);
    }
    
    private void newRound() {
        ba.sendArenaMessage("Prepare for round " + round + "!");
        currentRound = new DraftRound(this, type, team1, team2, team1Name, team2Name);
    }
    
    /** Stores the result of the game to the database */
    private void storeResult() {
        try {
            String query = "UPDATE tblDraft__Match SET fnTeam1Score = " + team1score + ", fnTeam2Score = " + team2score + " WHERE fnMatchID = " + gameID;
            ba.SQLQueryAndClose(db, query);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }
    
    /** Returns game type */
    public GameType getType() {
        return type;
    }
}
