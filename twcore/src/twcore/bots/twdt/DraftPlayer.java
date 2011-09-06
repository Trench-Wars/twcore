package twcore.bots.twdt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.events.Message;
import twcore.core.events.WeaponFired;
import twcore.core.lag.LagInfo;

/**
 *
 * @author WingZero
 */
public class DraftPlayer {

    public static final String db = "website";
    
    BotAction ba;
    OperatorList opList;
    BotSettings rules;
    
    enum Status { NONE, IN, LAGGED, OUT }
    
    private LagInfo lagInfo;
    
    HashMap<Integer, DraftStats> ships;
    Status status;
    DraftStats statTracker;
    String name;
    int freq, ship, lagouts, stars, specAt;
    long lastAttach;
    boolean botSpec;                // true if bot specced player for lag
    
    public DraftPlayer(BotAction botAction, BotSettings gameRules, String name, int freq, int ship, int stars) {
        ba = botAction;
        this.name = name;
        this.freq = freq;
        this.ship = ship;
        this.stars = stars;
        rules = gameRules;
        lastAttach = 0;
        lagouts = 0;
        statTracker = new DraftStats(ship);
        ships = new HashMap<Integer, DraftStats>();
        ships.put(ship, statTracker);
        status = Status.NONE;
        ba.sendArenaMessage("Player created: " + name + " " + ship);
    }
    
    public void handleEvent(Message event) {
        lagInfo.handleEvent(event);
    }

    public void handleEvent(WeaponFired event) {
        int type = event.getWeaponType();
        if (type == WeaponFired.WEAPON_BULLET)
            statTracker.getStat(StatType.SHOTS).increment();
        if (type == WeaponFired.WEAPON_BOMB)
            statTracker.getStat(StatType.BOMBS).increment();
        if (type == WeaponFired.WEAPON_BURST)
            statTracker.getStat(StatType.BURSTS).increment();
        if (type == WeaponFired.WEAPON_REPEL)
            statTracker.getStat(StatType.REPELS).increment();
    }
    
    public void handleDeath(DraftPlayer player) {
        
    }
    
    public void handleKill(DraftPlayer player) {
        
    }
    
    public void handleLagout() {
        
    }
    
    public void handleAttach() {
        lastAttach = System.currentTimeMillis();
    }
    
    public void handleFlagClaim() {
        statTracker.getStat(StatType.FLAG_CLAIMS).increment();
    }
    
    public Status getStatus() {
        return status;
    }
    
    public String getName() {
        return name;
    }
    
    public int getShip() {
        return ship;
    }
    
    public int getDeaths() {
        return statTracker.getStat(StatType.DEATHS).getValue();
    }
    
    public int getScore() {
        return statTracker.getScore();
    }
    
    public int getRating() {
        return statTracker.getRating();
    }
    
    public boolean isPlaying() {
        return (status == Status.IN || status == Status.LAGGED);
    }
    
    public void reportStart() {
    }
    
    public void reportEnd() {
    }
    
    public void saveStats() {
        String[] fields = { "fcName", "fnTeamID", "fnMatchID", "fnRound", "fnShip", "fnKills", "fnDeaths", "fnDoas", "fnTeamKills", "fnTerrKills", "fnMultiKills", "fnTopMultiKill", "fnKnockOuts", "fnKillJoys", "fnFlagClaims", "fnTopKillStreak", "fnTopDeathStreak", "fnShots", "fnBombs", "fnBursts", "fnRepels", "fnLagouts", "fnSubbed", "fnRating" };
        
    }
    
    public void setLagSpec(boolean specced) {
        botSpec = specced;
        if (botSpec)
            status = Status.LAGGED;
    }
    
    public void lagSpec(String reason) {
        ba.sendSmartPrivateMessage(name, "You have been placed into spectator mode due to lag.");
        ba.sendSmartPrivateMessage(name, reason);
        ba.spec(name);
        ba.spec(name);
    }
    
    public void setShip(int s) {
        System.out.println(name + " " + s);
        if (status == Status.IN)
            ba.setShip(name, s);
        ship = s;
        if (ships.containsKey(s))
            statTracker = ships.get(s);
        else {
            statTracker = new DraftStats(s);
            ships.put(s, statTracker);
        }
    }
    
    public void getIn() {
        status = Status.IN;
        setShip(ship);
        ba.setFreq(name, freq);
        ba.scoreReset(name);
        lagInfo = new LagInfo(ba, name);
        lagInfo.updateLag();
    }
    
    public void getIn(int s) {
        ship = s;
        getIn();
    }
    
    public void getOut() {
        status = Status.OUT;
        ba.spec(name);
        ba.spec(name);
        ba.setFreq(name, freq);
    }
}
