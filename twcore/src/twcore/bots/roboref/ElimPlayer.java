package twcore.bots.roboref;

import java.sql.ResultSet;
import java.sql.SQLException;

import twcore.bots.roboref.StatType;
import twcore.core.BotAction;
import twcore.core.util.Tools;

/**
 * Data of a player in the current elim game.
 * 
 * @author WingZero
 */
public class ElimPlayer {

    BotAction ba;
    
    enum Status { SPEC, IN, LAGGED, OUT };
    enum BasePos { NA, SPAWNING, SPAWN, IN, WARNED_OUT, WARNED_IN };

    public String[] streaks = {
            "On Fire!",
            "Killing Spree!",
            "Rampage!",
            "Dominating!",
            "Unstoppable!",
            "God-like!",
            "Cheater!",
            "Juggernaut!",
            "Kill Frenzy!",
            "Running Riot!",
            "Utter Chaos!",
            "Grim Reaper!",
            "Bulletproof!",
            "Invincible!",
            "Certified Veteran!",
            "Trench Wars Most Wanted!",
            "Unforeseeable paradoxes have ripped a hole in the fabric of the universe!"
    };
    
    public static final int STREAK_INIT = 5;
    public static final int STREAK_REPEAT = 2;
    public static final int MULTI_KILL_TIME = 5; // seconds 
    public static final String db = "website";
    public Status status;
    private BasePos pos;
    public String name;
    
    private ElimStats stats;
    private int consecutiveKills, lagouts, freq, specAt, lastStreak;
    private long lastKill;
    
    public ElimPlayer(BotAction act, String name) {
        ba = act;
        this.name = name;
        stats = null;
        status = Status.SPEC;
        lagouts = 3;
        consecutiveKills = 0;
        lastKill = 0;
        specAt = -1;
        freq = 9998;
        pos = BasePos.NA;
    }
    
    /**
     * Increments and adjusts all relevant death statistics.
     * @param killer The player object that made the kill
     * @return boolean array where 0=elimination and 1=KillJoy
     */
    public boolean[] handleDeath(ElimPlayer killer) {
        boolean[] vars = new boolean[] { false, false };
        if (stats.getStat(StatType.KILL_STREAK) > 1) {
            lastStreak = stats.getStat(StatType.KILL_STREAK);
            killer.handleKillJoy();
            if (lastStreak >= 5)
                vars[1] = true;
        }
        stats.handleDeath();
        if (stats.getStat(StatType.DEATHS) >= specAt) {
            status = Status.OUT;
            killer.handleKO();
            vars[0] = true;
        } else
            pos = BasePos.SPAWNING;
        return vars;
    }
    
    /**
     * Increments and adjusts all relevant kill statistics.
     * @param dead The player object for the kill victim
     * @return Returns a String if streak alert triggered or null otherwise
     */
    public String handleKill(ElimPlayer dead) {
        stats.handleKill();
        if(System.currentTimeMillis() - lastKill < (MULTI_KILL_TIME * Tools.TimeInMillis.SECOND)){
            consecutiveKills++;
            multiKill();
        } else if (consecutiveKills > 0) {
            stats.handleMultiKill(consecutiveKills);
            consecutiveKills = 0;
        } else
            consecutiveKills = 0;
        lastKill = System.currentTimeMillis();
        stats.crunchAve(dead.getRating());
        int killStreak = stats.getStat(StatType.KILL_STREAK);
        if(killStreak >= STREAK_INIT && (killStreak - STREAK_INIT) % STREAK_REPEAT == 0) {
            int i = (killStreak - STREAK_INIT) / STREAK_REPEAT;
            if (i >= streaks.length)
                i = streaks.length - 1;
            return name + " - " + streaks[i] + "(" + killStreak + ":0)";
        } else
            return null;
    }
    
    /** Reports a kill that broke the streak of another player or a "KillJoy" */
    public void handleKillJoy() {
        stats.handleKillJoy();
    }
    
    /** Reports the elimination of a player by this player */
    public void handleKO() {
        stats.handleKO();
    }
    
    /** Increment shot fired statistic */
    public void handleShot() {
        stats.incrementStat(StatType.SHOTS);
    }
    
    public boolean handleWarp() {
        if (stats.getStat(StatType.KILL_STREAK) > 1)
            lastStreak = stats.getStat(StatType.KILL_STREAK);
        stats.handleDeath();
        if (stats.getStat(StatType.DEATHS) >= specAt)
            return true;
        else
            return false;
        
    }
    
    /** Returns this players current rating */
    public int getRating() {
        return stats.getStat(StatType.RATING);
    }
    
    /** Returns an int array with kills and deaths */
    public int[] getScores() {
        return new int[] { stats.getStat(StatType.KILLS), stats.getStat(StatType.DEATHS) };
    }
    
    public int getKills() {
        return stats.getStat(StatType.KILLS);
    }
    
    public int getDeaths() {
        return stats.getStat(StatType.DEATHS);
    }
    
    public double getAim() {
        return stats.getAim(StatType.AIM);
    }

    /** Returns the remaining lagouts left */
    public int getLagouts() {
        return lagouts;
    }
    
    public int getLastKillStreak() {
        return lastStreak;
    }
    
    /** Change player status as specified */
    public void setStatus(Status s) {
        status = s;
    }
    
    /** Sets the freq for this player in the current game */
    public void setFreq(int f) {
        freq = f;
    }
    
    /** Gets the freq this player was originally put on */
    public int getFreq() {
        return freq;
    }
    
    /** Lagout command execution */
    public void lagin() {
        status = Status.IN;
        lagouts--;
    }
    
    public BasePos getPosition() {
        return pos;
    }
    
    public void setPosition(BasePos pos) {
        this.pos = pos;
    }
    
    public void sendOutsideWarning(int time) {
        setPosition(BasePos.WARNED_OUT);
        ba.sendPrivateMessage(name, "WARNING: You have " + time + " seconds to return to base or you will be disqualified.");
    }
    
    /** Get kills and deaths String */
    public String getScore() {
        return "" + stats.getStat(StatType.KILLS) + " wins " + stats.getStat(StatType.DEATHS) + " losses";   
    }
    
    /** Return stat spam */
    public String[] getStatStrings() {
        return stats.getStats(name, status.toString());
    }
    
    /** Returns this player's stat tracker */
    public ElimStats getStats() {
        return stats;
    }
    
    /** Record the loss of an elimination game and flush dynamic game stats */
    public void saveLoss() {
        stats.handleLoss();
        stats.unload();
        stats.loaded();
    }

    /** Record the win of an elimination game and flush dynamic game stats */
    public void saveWin() {
        stats.handleWin();
        stats.unload();
        stats.loaded();
    }
    
    /** Send stat load request to database for the given ship */
    public void loadStats(int ship, int spec) {
        if (stats != null && stats.isLoaded()) return;
        status = Status.IN;
        specAt = spec;
        stats = new ElimStats(ship);
    }
    
    /** Feed stats from the database query into the local database stat reference */
    public void loadStats(ResultSet rs) throws SQLException {
        stats.loadStats(rs);
    }
    
    /** Alert player's kill of multiple enemies at once */
    private void multiKill() {
        switch(consecutiveKills) {
            case 1: ba.sendArenaMessage(name + " - Double kill!", Tools.Sound.CROWD_OHH); break;
            case 2: ba.sendArenaMessage(name + " - Triple kill!", Tools.Sound.CROWD_GEE); break;
            case 3: ba.sendArenaMessage(name + " - Quadruple kill!", Tools.Sound.INCONCEIVABLE); break;
            case 4: ba.sendArenaMessage(name + " - Quintuple kill!", Tools.Sound.SCREAM); break;
            case 5: ba.sendArenaMessage(name + " - Sextuple kill!", Tools.Sound.CRYING); break;
            case 6: ba.sendArenaMessage(name + " - Septuple kill!", Tools.Sound.GAME_SUCKS); break;      
        }
    }}
