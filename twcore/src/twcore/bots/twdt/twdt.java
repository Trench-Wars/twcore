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
        arena = arena.substring(4, 5).toUpperCase();
        rules = new BotSettings(ba.getGeneralSettings().getString("Core Location") + "/data/Rules/" + "TWDT" + arena + ".txt");
        switch (rules.getInt("Type")) {
            case 0: type = GameType.BASING; break;
            case 1: type = GameType.WARBIRD; break;
            case 2: type = GameType.JAVELIN; break;
        }
    }

    public void handleEvent(LoggedOn event) {
        
    }

    public void handleEvent(PlayerEntered event) {
        
    }

    public void handleEvent(PlayerLeft event) {
        
    }

    public void handleEvent(WeaponFired event) {
        
    }

    public void handleEvent(FlagClaimed event) {
        
    }

    public void handleEvent(FlagReward event) {
        
    }
    
    public void handleEvent(TurretEvent event) {
        
    }

    public void handleEvent(FrequencyShipChange event) {
        
    }

    public void handleEvent(PlayerPosition event) {
        
    }

    public void handleEvent(PlayerDeath event) {
        
    }

    public void handleEvent(Message event) {
        
    }
    
    public void cmd_load(String name, String cmd) {
            
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
