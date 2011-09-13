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
 *
 * @author WingZero
 */
public class twdt extends SubspaceBot {
    
    public BotAction ba;
    public OperatorList oplist;
    public BotSettings rules;

    public static final String db = "website";
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
        }
        if (game != null)
            game.handleEvent(event);
    }
    
    public void cmd_help(String name) {
        String[] msg = {
                " !go <arena>, !die, !help",
                " !load <id>                    - Loads a preset game with the game/match ID of <id>",
                " !load <team1>:<team2>:<type#> - Loads a new game of <type#> (1-WB 2-JAV 3-BASE)",
                " !start                        - Allows the team captains to start adding players if a game is loaded",
                " !addtime                      - During lineup selection will add 2 minute extension",
        };
        ba.smartPrivateMessageSpam(name, msg);
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
                ResultSet rs2 = ba.SQLQuery(db, "SELECT * FROM tblTWDT__Fixtures WHERE fnSeason = 7 AND fnFixtureID = " + gameID + " LIMIT 1");
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
                    ba.SQLQueryAndClose(db, "INSERT INTO tblDraft__Match (fnMatchID, fnSeason, fnWeek, fnType, fnTeam1, fnTeam2, fcTeam1, fcTeam2, fcHost) VALUES(" + gameID + ", 7, " + week + ", " + GameType.getInt(type) + ", " + team1 + ", " + team2 + ", '" + Tools.addSlashesToString(name1) + "', '" + Tools.addSlashesToString(name2) + "', '" + Tools.addSlashesToString(name) + "')");

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
            ResultSet rs = ba.SQLQuery(db, "SELECT * FROM tblDraft__Team WHERE fnSeason = 7 AND (fcName = '" + Tools.addSlashesToString(args[0]) + "' OR fcName = '" + Tools.addSlashesToString(args[1]) + "')");
            if (rs.next()) {
                String team1name = rs.getString("fcName");
                int team1 = rs.getInt("fnTeamID");
                if (rs.next()) {
                    String team2name = rs.getString("fcName");
                    int team2 = rs.getInt("fnTeamID");
                    if (team1 != team2) {
                        ba.SQLQueryAndClose(db, "INSERT INTO tblDraft__Match (fnSeason, fnType, fnTeam1, fnTeam2, fcTeam1, fcTeam2) VALUES(7, " + ship + ", " + team1 + ", " + team2 + ", '" + Tools.addSlashesToString(team1name) + "', '" + Tools.addSlashesToString(team2name) +"')");
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
        if (cmd.length() < 5) return;
        ba.changeArena(cmd.substring(cmd.indexOf(" ") + 1));
    }
    
    /** Kills bot */
    public void cmd_die(String name) {
        ba.sendSmartPrivateMessage(name, "Killing myself...");
        handleDisconnect();
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
