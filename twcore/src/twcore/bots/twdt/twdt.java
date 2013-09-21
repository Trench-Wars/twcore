package twcore.bots.twdt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimerTask;

import twcore.bots.twdt.DraftGame.GameType;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.*;
import twcore.core.util.Tools;

/**
 * This bot is used to regulate the league games of TWDT (Draft Tournament). Modeled after MatchBots,
 * the bot makes use of several classes that represent different pieces of a league game. This main 
 * class passes on events and also loads match information from the database using a given ID number.
 * A new DraftGame is created for the loaded match ID. A DraftRound is created inside DraftGame when 
 * the hosting staff member does !start. DraftRound controls most of the routine match states and procedures.
 * DraftRound creates two DraftTeams to represent the opposing teams. DraftTeam maintains a collection of 
 * DraftPlayers and keeps track of relevant team information and acts as a communication mediator in some cases.
 * DraftPlayer bridges game events with player stats and states. DraftRound uses LagHandler to check the lag of
 * a player from each team every 5 seconds. 
 *
 * @author WingZero
 */
public class twdt extends SubspaceBot {
    
    public BotAction ba;
    public OperatorList oplist;
    public BotSettings rules;

    public static final String db = "website";
    public static final Integer dtSeason = 10;
    public GameType type;
    public DraftGame game;
    
    public twdt(BotAction botAction) {
        super(botAction);
        ba = botAction;
        requestEvents();
        oplist = ba.getOperatorList();
        game = null;
        type = null;
    }
    
    /** EVENT HANDLERS */
    public void handleEvent(ArenaJoined event) {
        ba.toggleLocked();
        ba.specAll();
    }

    public void handleEvent(LoggedOn event) {
        ba.joinArena(ba.getBotSettings().getString("InitialArena"));
    }

    public void handleEvent(PlayerEntered event) {
        if (game != null)
            game.handleEvent(event);
    }

    public void handleEvent(PlayerLeft event) {
        if (game != null)
            game.handleEvent(event);
    }

    public void handleEvent(WeaponFired event) {
        if (game != null)
            game.handleEvent(event);
    }

    public void handleEvent(FlagClaimed event) {
        if (game != null)
            game.handleEvent(event);
    }

    public void handleEvent(FlagReward event) {
        if (game != null)
            game.handleEvent(event);
    }
    
    public void handleEvent(TurretEvent event) {
        if (game != null)
            game.handleEvent(event);
    }

    public void handleEvent(FrequencyShipChange event) {
        if (game != null)
            game.handleEvent(event);
    }

    public void handleEvent(PlayerPosition event) {
        if (game != null)
            game.handleEvent(event);
    }

    public void handleEvent(PlayerDeath event) {
        if (game != null)
            game.handleEvent(event);
    }

    public void handleEvent(Message event) {
        String msg = event.getMessage();
        if (msg.equals("Arena UNLOCKED"))
            ba.toggleLocked();
        if (event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE) {
            String name = ba.getPlayerName(event.getPlayerID());
            if (name == null)
                name = event.getMessager();
            if (name == null) return;
            
            if (oplist.isER(name)) {
                if (msg.startsWith("!load "))
                    cmd_load(name, msg);
                else if (msg.equals("!die"))
                    cmd_die(name);
                else if (msg.startsWith("!go "))
                    cmd_go(name, msg);
                else if (msg.equals("!help"))
                    cmd_help(name);
            }
            
            if (oplist.isModerator(name)) {
                if (msg.equals("!reset"))
                    cmd_reset(name);
                else if (msg.equals("!killgame") && game != null)  {
                        m_botAction.sendArenaMessage("The game has been brutally killed by " + name);
                        game.cancel();
                        game = null;
                        try { Thread.sleep(100); } catch (Exception e) {};
                            command_unlock(name);                        
                    } else if (msg.equals("!unload") && game != null)  {
                        game.cancel();
                        game = null;
                        try { Thread.sleep(100); } catch (Exception e) {};
                            command_unlock(name);
                    }
            }
        }
        if (game != null)
            game.handleEvent(event);
    }
    
    /** Handles the help command */
    public void cmd_help(String name) {
        String[] msg = {
                " !go <arena>, !die, !help",
                " !load <id>                    - Loads a preset game with the game/match ID of <id>",
                " !start                        - Allows the team captains to start adding players if a game is loaded",
                " !addtime                      - During lineup selection will add 2 minute extension",
                " !zone                         - Sends a zone message for the current game",
        };
        ba.smartPrivateMessageSpam(name, msg);
        if (oplist.isModerator(name)) {
            ba.sendSmartPrivateMessage(name, " !reset                        - Resets player and team star counts");
        }
    }
    
    public void cmd_reset(String name) {
        try {
            String query = "UPDATE tblDraft__Team SET fnUsedStars = 0 WHERE fnSeason = " + dtSeason;
            ba.SQLQueryAndClose(db, query);
            query = "UPDATE tblDraft__Player SET fnPlayed = 0";
            ba.SQLQueryAndClose(db, query);
            ba.sendSmartPrivateMessage(name, "Player and team star counts have been reset.");
        } catch (SQLException e) {
            ba.sendSmartPrivateMessage(name, "There was an error processing your request.");
            Tools.printStackTrace(e);
        }
    }
    
    /** Handles the load command which retrieves the associated match information from the database */
    public void cmd_load(String name, String cmd) {
        if (game != null) {
            ba.sendSmartPrivateMessage(name, "Cannot load new game with a game currently in progress.");
            return;
        } else if (cmd.contains(":")) {
            String[] args = cmd.substring(cmd.indexOf(" ") + 1).split(":");
            if (args.length == 3) {
                cmd_load(name, args);
                return;
            } else {
                ba.sendSmartPrivateMessage(name, "Error, invalid command parameters. Exiting...");
                return;
            }
        }
        int gameID = -1;
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
                switch (type) {
                    case WARBIRD: 
                        rules = new BotSettings(ba.getGeneralSettings().getString("Core Location") + "/data/Rules/" + "TWDTD.txt");
                        break;
                    case JAVELIN:
                        rules = new BotSettings(ba.getGeneralSettings().getString("Core Location") + "/data/Rules/" + "TWDTJ.txt");
                        break;
                    case BASING:
                        rules = new BotSettings(ba.getGeneralSettings().getString("Core Location") + "/data/Rules/" + "TWDTB.txt");
                        break;
                }
                int team1, team2;
                String name1, name2;
                team1 = rs.getInt("fnTeam1");
                team2 = rs.getInt("fnTeam2");
                name1 = rs.getString("fcTeam1");
                name2 = rs.getString("fcTeam2");
                game = new DraftGame(this, gameID, team1, team2, name1, name2, name);
                ba.sendSmartPrivateMessage(name, "Success! Game loaded from pre-existing information.");
            } else {
                ba.sendSmartPrivateMessage(name, "No pre-existing match information found for: " + gameID + ". Attempting to load match fixture...");
                ResultSet rs2 = ba.SQLQuery(db, "SELECT * FROM tblDraft_Fixtures WHERE fnSeason = " + dtSeason + " AND fnFixtureID = " + gameID + " LIMIT 1");
                if (rs2.next()) {
                    type = GameType.getType(rs2.getInt("fnSubLeague"));
                    switch (type) {
                        case WARBIRD: 
                            rules = new BotSettings(ba.getGeneralSettings().getString("Core Location") + "/data/Rules/" + "TWDTD.txt");
                            break;
                        case JAVELIN:
                            rules = new BotSettings(ba.getGeneralSettings().getString("Core Location") + "/data/Rules/" + "TWDTJ.txt");
                            break;
                        case BASING:
                            rules = new BotSettings(ba.getGeneralSettings().getString("Core Location") + "/data/Rules/" + "TWDTB.txt");
                            break;
                    }
                    int team1, team2;
                    String name1, name2;
                    team1 = rs2.getInt("fnTeamID1");
                    team2 = rs2.getInt("fnTeamID2");
                    name1 = rs2.getString("fcTeam1Name");
                    name2 = rs2.getString("fcTeam2Name");
                    int week = rs2.getInt("fnWeek");
                    ba.SQLQueryAndClose(db, "INSERT INTO tblDraft__Match (fnMatchID, fnSeason, fnWeek, fnType, fnTeam1, fnTeam2, fcTeam1, fcTeam2, fcHost) VALUES(" + gameID + ", " + dtSeason + ", " + week + ", " + GameType.getInt(type) + ", " + team1 + ", " + team2 + ", '" + Tools.addSlashesToString(name1) + "', '" + Tools.addSlashesToString(name2) + "', '" + Tools.addSlashesToString(name) + "')");

                    ba.sendSmartPrivateMessage(name, "Created new match information from TWDT fixture ID: " + gameID);
                    game = new DraftGame(this, gameID, team1, team2, name1, name2, name);
                    ba.SQLClose(rs2);
                } else {
                    gameID = -1;
                    ba.sendSmartPrivateMessage(name, "Failure! You suck!");
                }
            }
            ba.SQLClose(rs);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }        
    }
    
    /** Loads a new game by creating game information from team names and a game type */
    public void cmd_load(String name, String[] args) {
        int ship = -1;
        try {
            ship = Integer.valueOf(args[2]);
            if (ship < 1 || ship > 3)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(name, "Error, invalid game type number. Exiting...");
            return;
        }
        try {
            ResultSet rs = ba.SQLQuery(db, "SELECT * FROM tblDraft__Team WHERE fnSeason = " + dtSeason + " AND (fcName = '" + Tools.addSlashesToString(args[0]) + "' OR fcName = '" + Tools.addSlashesToString(args[1]) + "')");
            if (rs.next()) {
                String team1name = rs.getString("fcName");
                int team1 = rs.getInt("fnTeamID");
                if (rs.next()) {
                    String team2name = rs.getString("fcName");
                    int team2 = rs.getInt("fnTeamID");
                    if (team1 != team2) {
                        ba.SQLQueryAndClose(db, "INSERT INTO tblDraft__Match (fnSeason, fnType, fnTeam1, fnTeam2, fcTeam1, fcTeam2) VALUES(" + dtSeason + ", " + ship + ", " + team1 + ", " + team2 + ", '" + Tools.addSlashesToString(team1name) + "', '" + Tools.addSlashesToString(team2name) +"')");
                        ResultSet id = ba.SQLQuery(db, "SELECT LAST_INSERT_ID()");
                        if (id.next())
                            cmd_load(name, "!load " + id.getInt(1));
                        else
                            ba.sendSmartPrivateMessage(name, "SQL Error!");
                        ba.SQLClose(id);
                    } else
                        ba.sendSmartPrivateMessage(name, "A team cannot play itself.");
                } else
                    ba.sendSmartPrivateMessage(name, "Error, a team could not be found in the database.");
            } else
                ba.sendSmartPrivateMessage(name, "Error, a team could not be found in the database.");
            ba.SQLClose(rs);
        } catch (SQLException e) {
            ba.sendSmartPrivateMessage(name, "SQL Error!");
            Tools.printStackTrace(e);
        }
    }
    
    /** Moves bot to a different arena */
    public void cmd_go(String name, String cmd) {
        if (game != null) {
            ba.sendSmartPrivateMessage(name, "Cannot leave with a game still in progress. Please !unlock to unload the current game");
            return;
        } else {
        if (cmd.length() < 5) return;
        ba.changeArena(cmd.substring(cmd.indexOf(" ") + 1));
        }
    }
    
    /** Kills bot */
    public void cmd_die(String name) {
        ba.sendSmartPrivateMessage(name, "Killing myself...");
        handleDisconnect();
    }
    
    public void command_unlock(String name) {
        m_botAction.sendPrivateMessage(name, "Unlocked, going to ?go twl");
        m_botAction.changeArena("twdt");
    }
    
    /** Helper requests the required events */
    private void requestEvents() {
        EventRequester er = ba.getEventRequester();
        er.request(EventRequester.ARENA_JOINED);
        er.request(EventRequester.FLAG_CLAIMED);
        er.request(EventRequester.FLAG_REWARD);
        er.request(EventRequester.LOGGED_ON);
        er.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        er.request(EventRequester.MESSAGE);
        er.request(EventRequester.PLAYER_DEATH);
        er.request(EventRequester.PLAYER_ENTERED);
        er.request(EventRequester.PLAYER_LEFT);
        er.request(EventRequester.PLAYER_POSITION);
        er.request(EventRequester.WEAPON_FIRED);
        er.request(EventRequester.TURRET_EVENT);
    }
    
    @Override
    public void handleDisconnect() {
        ba.cancelTasks();
        ba.scheduleTask(new Die(), 2000);
    }
    
    public class Die extends TimerTask {
        
        @Override
        public void run() {
            ba.die();
        }
    }
}
