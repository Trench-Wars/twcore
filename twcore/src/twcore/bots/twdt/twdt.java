package twcore.bots.twdt;

import java.sql.ResultSet;
import java.sql.SQLException;

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
    public OperatorList opList;
    public BotSettings rules;

    public static final String db = "website";
    public GameType type;
    public DraftGame game;
    
    public twdt(BotAction botAction) {
        super(botAction);
        ba = botAction;
        requestEvents();
        opList = ba.getOperatorList();
        game = null;
        type = null;
    }
    
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
            
            if (msg.startsWith("!load "))
                cmd_load(name, msg);
            
            if (opList.isModerator(name)) {
                if (msg.equals("!die"))
                    cmd_die(name);
                else if (msg.startsWith("!go "))
                    cmd_go(name, msg);
            }
        }
        if (game != null)
            game.handleEvent(event);
    }
    
    public void cmd_load(String name, String cmd) {
        if (game != null) {
            ba.sendSmartPrivateMessage(name, "Cannot load new game with a game currently in progress.");
            return;
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
    
    public void cmd_go(String name, String cmd) {
        if (cmd.length() < 5) return;
        ba.changeArena(cmd.substring(cmd.indexOf(" ") + 1));
    }
    
    public void cmd_die(String name) {
        ba.cancelTasks();
        ba.die();
    }
    
    
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
}
