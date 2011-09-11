package twcore.bots.twdt;

import java.util.HashMap;

import twcore.bots.twdt.DraftRound.RoundState;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.events.Message;
import twcore.core.events.WeaponFired;
import twcore.core.lag.LagInfo;
import twcore.core.util.Tools;

/**
 *
 * @author WingZero
 */
public class DraftPlayer {

    public static final String db = "website";
    public static final int DOA_TIME = 3; // seconds
    
    BotAction ba;
    OperatorList opList;
    BotSettings rules;
    
    enum Status { NONE, IN, LAGGED, SUBBED, OUT }
    
    private LagInfo lagInfo;
    
    HashMap<Integer, DraftStats> ships;
    Status status;
    DraftStats statTracker;
    DraftTeam team;
    String name;
    int freq, ship, lagouts, stars, specAt, lagoutCount;
    long lastAttach, lastLagout;
    boolean botSpec;                // true if bot specced player for lag
    
    public DraftPlayer(BotAction botAction, DraftTeam team, String name, int freq, int ship, int stars) {
        ba = botAction;
        this.name = name;
        this.freq = freq;
        this.ship = ship;
        this.stars = stars;
        this.team = team;
        rules = team.rules;
        lastAttach = 0;
        lagoutCount = 0;
        botSpec = false;
        lagouts = rules.getInt("lagouts");
        specAt = rules.getInt("deaths");
        statTracker = new DraftStats(ship);
        ships = new HashMap<Integer, DraftStats>();
        ships.put(ship, statTracker);
        status = Status.NONE;
    }
    
    public void handleEvent(Message event) {
        String player = ba.getPlayerName(event.getPlayerID());
        if (player == null)
            player = event.getMessager();
        if (player == null || !name.equalsIgnoreCase(player)) return;
        lagInfo.handleEvent(event);
        String msg = event.getMessage();
        if (msg.equals("!lagout"))
            cmd_lagout();
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
        if (specAt == -1) {
            if (System.currentTimeMillis() - lastAttach <= DOA_TIME * Tools.TimeInMillis.SECOND)
                statTracker.getStat(StatType.DOAS).increment();
        }
        if (statTracker.getStat(StatType.KILL_STREAK).getValue() > 2)
            player.handleKillJoy();
        statTracker.handleDeath();
        if (specAt != -1 && getDeaths() >= specAt) {
            getOut();
            ba.sendArenaMessage(name + " is out. " + statTracker.getStat(StatType.KILLS).getValue() + " wins " + getDeaths() + " losses");
            player.handleKnockOut();
        }
    }
    
    public void handleDeath() {
        statTracker.handleDeath();
        if (specAt != -1 && getDeaths() >= specAt) {
            getOut();
            ba.sendArenaMessage(name + " is out. " + statTracker.getStat(StatType.KILLS).getValue() + " wins " + getDeaths() + " losses");
        }        
    }
    
    public void handleKill(int points, DraftPlayer player) {
        if (team.isPlaying(player.getName())) {
        	statTracker.handleTeamKill(points, player.getShip());
        } else {
            statTracker.handleKill(points, player.getShip());
        }
    }
    
    public void handleFlagReward(int points) {
        statTracker.getStat(StatType.SCORE).add(points);
    }
    
    public void handleKillJoy() {
        statTracker.getStat(StatType.KILL_JOYS).increment();
    }
    
    public void handleKnockOut() {
        statTracker.getStat(StatType.KNOCK_OUTS).increment();
    }
    
    public void handleLagout() {
        if (status != Status.IN) return; 
        status = Status.LAGGED;
        lastLagout = System.currentTimeMillis();
        team.handleLagout(name);
        if (team.round.getState() != RoundState.PLAYING) return;
        statTracker.handleLagout();
        lagoutCount++;
        if (specAt > -1) {
            statTracker.getStat(StatType.DEATHS).increment();
            ba.sendArenaMessage(name + " has changed to spectator mode - +1 death");
            if (getDeaths() >= specAt) {
                getOut();
                ba.sendArenaMessage(name + " is out. " + statTracker.getStat(StatType.KILLS).getValue() + " wins " + getDeaths() + " losses");
            }
        }
    }
    
    public void handleAttach() {
        lastAttach = System.currentTimeMillis();
    }
    
    public void handleFlagClaim() {
        statTracker.getStat(StatType.FLAG_CLAIMS).increment();
    }
    
    public void handleSubbed() {
        status = Status.SUBBED;
        statTracker.handleSubbed();
    }
    
    public void cmd_lagout() {
        if (status == Status.LAGGED) {
            if (team.round.getState() == RoundState.PLAYING) {
                if (specAt != -1) {
                    if (lagouts > 0) {
                        status = Status.IN;
                        lagouts--;
                        ba.sendPrivateMessage(name, "You have " + lagouts + " lagouts remaining.");
                        ba.setShip(name, ship);
                        ba.setFreq(name, freq);
                    } else
                        ba.sendPrivateMessage(name, "You have no more lagouts remaining.");
                } else {
                    if (System.currentTimeMillis() - lastLagout > 10 * 1000) {
                        status = Status.IN;
                        ba.setShip(name, ship);
                        ba.setFreq(name, freq);
                    } else
                        ba.sendPrivateMessage(name, "You must wait " + (10 - (System.currentTimeMillis() - lastLagout) / 1000) + " more seconds.");
                }
            } else if (team.round.getState() == RoundState.LINEUPS || team.round.getState() == RoundState.STARTING) {
                status = Status.IN;
                ba.setShip(name, ship);
                ba.setFreq(name, freq);
            }
        } else
            ba.sendPrivateMessage(name, "You are not lagged out.");
    }
    
    public void setSpecAt(int spec) {
        specAt = spec;
    }
    
    public int getSpecAt() {
        return specAt;
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
    
    public int getLastLagout() {
        return (int)((System.currentTimeMillis() - lastLagout) / 1000);
    }
    
    public int getLagouts() {
        return lagoutCount;
    }
    
    public int getStars() {
        return stars;
    }
    
    public boolean isPlaying() {
        return (status == Status.IN || status == Status.LAGGED);
    }
    
    public void saveStats() {
        String[] fields = { 
                "fcName", 
                "fnTeamID", 
                "fnMatchID", 
                "fnRound", 
                "fnShip", 
                "fnKills", 
                "fnDeaths", 
                "fnDOAs", 
                "fnTeamKills", 
                "fnTerrKills", 
                "fnMultiKills", 
                "fnTopMultiKill", 
                "fnKnockOuts", 
                "fnKillJoys", 
                "fnFlagClaims", 
                "fnTopKillStreak", 
                "fnTopDeathStreak", 
                "fnShots", 
                "fnBombs", 
                "fnBursts", 
                "fnRepels", 
                "fnLagouts", 
                "fnSubbed", 
                "fnScore",
                "fnRating" 
                };
        
        for (DraftStats shipStats : ships.values()) {
            statTracker = shipStats;
            String[] values = getStats();
            ba.SQLBackgroundInsertInto(db, "tblDraft__PlayerRound", fields, values);            
        }
    }
    
    public String[] getStats() {
        String[] values = new String[25];
        values[0] = name;
        values[1] = "" + team.getID();
        values[2] = "" + team.round.game.getMatchID();
        values[3] = "" + team.round.game.round;
        values[4] = "" + statTracker.getShip();
        values[5] = "" + statTracker.getStat(StatType.KILLS).getValue();
        values[6] = "" + statTracker.getStat(StatType.DEATHS).getValue();
        values[7] = "" + statTracker.getStat(StatType.DOAS).getValue();
        values[8] = "" + statTracker.getStat(StatType.TEAM_KILLS).getValue();
        values[9] = "" + statTracker.getStat(StatType.TERR_KILLS).getValue();
        values[10] = "" + statTracker.getStat(StatType.MULTI_KILLS).getValue();
        values[11] = "" + statTracker.getStat(StatType.BEST_MULTI_KILL).getValue();
        values[12] = "" + statTracker.getStat(StatType.KNOCK_OUTS).getValue();
        values[13] = "" + statTracker.getStat(StatType.KILL_JOYS).getValue();
        values[14] = "" + statTracker.getStat(StatType.FLAG_CLAIMS).getValue();
        values[15] = "" + statTracker.getStat(StatType.BEST_KILL_STREAK).getValue();
        values[16] = "" + statTracker.getStat(StatType.WORST_DEATH_STREAK).getValue();
        values[17] = "" + statTracker.getStat(StatType.SHOTS).getValue();
        values[18] = "" + statTracker.getStat(StatType.BOMBS).getValue();
        values[19] = "" + statTracker.getStat(StatType.BURSTS).getValue();
        values[20] = "" + (statTracker.getStat(StatType.REPELS).getValue() / 2);
        values[21] = "" + statTracker.getStat(StatType.LAGOUTS).getValue();
        values[22] = "" + statTracker.getStat(StatType.SUBBED).getValue();
        values[23] = "" + statTracker.getScore();
        values[24] = (specAt == -1) ? "" + statTracker.getRating() : "" + 0;
        return values;
    }
    
    public DraftStat getStat(StatType stat) {
        return statTracker.getStat(stat);
    }
    
    public String getRPD() {
        return statTracker.getRPDString();
    }
    
    public void setLagSpec(boolean specced) {
        botSpec = specced;
        if (botSpec)
            status = Status.LAGGED;
    }
    
    public void lagSpec(String reason) {
        status = Status.LAGGED;
        lagoutCount++;
        ba.sendSmartPrivateMessage(name, "You have been placed into spectator mode due to lag.");
        ba.sendSmartPrivateMessage(name, reason);
        ba.spec(name);
        ba.spec(name);
    }
    
    public void setShip(int s) {
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
