package twcore.bots.twdt;

import twcore.bots.twdt.DraftGame.GameType;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.*;

/**
 *
 * @author WingZero
 */
public class twdt extends SubspaceBot {
    
    public BotAction ba;
    public OperatorList opList;
    public BotSettings rules;
    
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
        String arena = ba.getArenaName();
        if (!arena.startsWith("twdt")) return;
        ba.toggleLocked();
        ba.specAll();
        arena = arena.substring(4, 5).toUpperCase();
        rules = new BotSettings(ba.getGeneralSettings().getString("Core Location") + "/data/Rules/" + "TWDT" + arena + ".txt");
        switch (rules.getInt("Type")) {
            case 0: type = GameType.BASING; break;
            case 1: type = GameType.WARBIRD; break;
            case 2: type = GameType.JAVELIN; break;
            case 3: type = GameType.BASING; break;
        }
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
        if (game == null)
            game = new DraftGame(this);
        game.cmd_loadGame(name, cmd);
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
