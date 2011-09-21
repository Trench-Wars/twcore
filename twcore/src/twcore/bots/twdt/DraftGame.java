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
    
    twdt            bot;
    BotAction       ba;
    OperatorList    opList;
    BotSettings     rules;
    
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
        
        static int getInt(GameType t) {
            switch(t) {
                case WARBIRD: return 1;
                case JAVELIN: return 2;
                case BASING: return 3;
                default: return -1;
            }
        }
    }
    
    LinkedList<DraftRound>  oldRounds;
    DraftRound              currentRound;
    DraftTeam               winner;
    GameType                type;
    
    int     gameID;
    int     round;
    int     team1;
    int     team2;
    int     maxPlayers;
    int     minPlayers;
    int     maxDeaths;
    int     time;
    int     score1;
    int     score2;
    String  team1score;
    String  team2score;
    String  team1Name;
    String  team2Name;
    String  host;
    boolean zoned;

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
        team1score = "0";
        team2score = "0";
        score1 = 0;
        score2 = 0;
        winner = null;
        zoned = false;
        oldRounds = new LinkedList<DraftRound>();
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
        String msg = "";
        if (type == GameType.WARBIRD)
            msg += "Welcome to TWDTD! ";
        else if (type == GameType.JAVELIN)
            msg += "Welcome to TWDTJ! ";
        else if (type == GameType.BASING)
            msg += "Welcome to TWDTB! ";
        
        if (currentRound != null)
            msg += getStatus();
        else
            msg += team1Name + " vs. " + team2Name + " will begin momentarily.";
        
        ba.sendPrivateMessage(name, msg);
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
                else if (msg.equals("!zone"))
                    cmd_zone(name);
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
        host = name;
        round = 1;
        currentRound = new DraftRound(this, type, team1, team2, team1Name, team2Name);
    }
    
    /** Handles the zone command which will send a zoner for the current game unless it has already been zoned once */
    public void cmd_zone(String name) {
        if (zoned) {
            ba.sendSmartPrivateMessage(name, "The zoner for this game has already been used.");
            return;
        }
        zoned = true;
        switch (type) {
            case WARBIRD:
                ba.sendZoneMessage("[TWDT-D] " + team1Name + " vs. " + team2Name + " starting in ?go " + ba.getArenaName());
                break;
            case JAVELIN:
                ba.sendZoneMessage("[TWDT-J] " + team1Name + " vs. " + team2Name + " starting in ?go " + ba.getArenaName());
                break;
            case BASING:
                ba.sendZoneMessage("[TWDT-B] " + team1Name + " vs. " + team2Name + " starting in ?go " + ba.getArenaName());
                break;
        }
    }
    
    /** Returns the default total game time */
    public int getTime() {
        return time;
    }
    
    /** Returns the id number as given from the database */
    public int getMatchID() {
        return gameID;
    }
    
    /** Returns the current round number */
    public int getRound() {
        return round;
    }
    
    /** Reports the end of a round */
    public void handleRound(DraftTeam team) {
        if (type != GameType.BASING) {
            if (team != null) {
                if (team.getID() == team1) {
                    score1++;
                    team1score = "" + score1;
                    if (score1 >= rules.getInt("Rounds")) {
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
                    score2++;
                    team2score = "" + score2;
                    if (score2 >= rules.getInt("Rounds")) {
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
            } else {
                winner = null;
                currentRound.gameOver();
                ba.sendArenaMessage("GAME OVER: Draw!", 5);
                storeResult();
                oldRounds.add(currentRound);
                currentRound = null;
            }
        } else {
            winner = team;
            currentRound.gameOver();
            if (winner != null) {
                ba.sendArenaMessage("GAME OVER: " + winner.getName() + " wins!", 5);
                score1 = currentRound.team1.getScore();
                score2 = currentRound.team2.getScore();
                team1score = currentRound.team1.getTime();
                team2score = currentRound.team2.getTime();
            } else
                ba.sendArenaMessage("GAME OVER: Draw!", 5);
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
            return msg;
        }
        return "";
    }
    
    /** Creates the next round after 4 seconds */
    private void nextRound() {
        TimerTask next = new TimerTask() {
            public void run() {
                newRound();
            }
        };
        ba.scheduleTask(next, 4000);
    }
    
    /** A new round is created and set to currentRound */
    private void newRound() {
        ba.sendArenaMessage("Prepare for round " + round + "!");
        currentRound = new DraftRound(this, type, team1, team2, team1Name, team2Name);
    }
    
    /** Stores the result of the game to the database */
    private void storeResult() {
        try {
            String query = "UPDATE tblDraft__Match SET fdPlayed = NOW(), fcTeam1Score = '" + Tools.addSlashesToString(team1score) + "', fcTeam2Score = '" + Tools.addSlashesToString(team2score) + "', fcHost = '" + Tools.addSlashesToString(host) + "' WHERE fnMatchID = " + gameID;
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
