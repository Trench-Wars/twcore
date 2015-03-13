package twcore.bots.elim;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimerTask;

import twcore.bots.elim.ElimGame.GameState;
import twcore.bots.elim.StatType;
import twcore.core.BotAction;
import twcore.core.events.PlayerPosition;
import twcore.core.util.Tools;

/**
 * Data of a player in the current elim game.
 * 
 * @author WingZero
 */
public class ElimPlayer {

    BotAction ba;
    
    enum Status { SPEC, SPAWN, IN, WARNED_OUT, WARNED_IN, OUT, LAGGED };

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

    public static final int BOUNDARY = 222 * 16;
    public static final int BOUNDS_TIME = 20;
    public static final int SPAWN_TIME = 5;      // seconds until respawn after death
    public static final int SPAWN_BOUND = 10;    // seconds after spawn warning is sent
    public static final int STREAK_INIT = 5;
    public static final int STREAK_REPEAT = 2;
    public static final int MULTI_KILL_TIME = 5; // seconds 
    public static final String db = "website";
    public Status status;
    public String name;
    private Spawn spawn;
    private Bounds bounds;
    private ElimStats stats;
    private ElimGame game;
    private int ship, consecutiveKills, lagouts, freq, specAt, lastStreak;
    private long lastKill, lastDeath, lastShot;
    private int currentSeason;
    
    public ElimPlayer(BotAction act, ElimGame elimGame, String name, int ship, int deaths) {
        ba = act;
        currentSeason = ba.getBotSettings().getInt("CurrentSeason");
        this.name = name;
        spawn = null;
        bounds = null;
        lagouts = 3;
        consecutiveKills = 0;
        lastKill = 0;
        lastShot = 0;
        lastDeath = System.currentTimeMillis();
        specAt = deaths;
        freq = 9998;
        this.ship = ship;
        status = Status.SPAWN;
        game = elimGame;
        stats = new ElimStats(ship);
        ba.SQLBackgroundQuery(db, "load:" + name, "SELECT * FROM tblElim__Player WHERE fnShip = " + ship + " AND fcName = '" + Tools.addSlashesToString(name) + "' AND fnSeason = " + currentSeason + " LIMIT 1");
    }
    
    /**
     * Increments and adjusts all relevant death statistics.
     * @param killer The player object that made the kill
     */
    public void handleDeath(ElimPlayer killer) {
        cancelTasks();
        status = Status.SPAWN;
        lastDeath = System.currentTimeMillis();
        if (stats.getStat(StatType.KILL_STREAK) > 1) {
            lastStreak = stats.getStat(StatType.KILL_STREAK);
            stats.setStat(StatType.KILL_STREAK, 0);
            killer.handleKillJoy();
            if (lastStreak >= 5)
                ba.sendArenaMessage("Kill Joy! " + killer.name + " terminates the (" + lastStreak + ":0) kill streak of " + name + "!", Tools.Sound.INCONCEIVABLE);
        }
        stats.handleDeath();
        if (game.bot.gameType == elim.ELIM && stats.getStat(StatType.DEATHS) >= specAt) {
            status = Status.OUT;
            stats.handleLoss();
            ba.spec(name);
            ba.spec(name);
            ba.sendArenaMessage(name + " is out. " + getScore());
            game.removePlayer(this);
            killer.handleKO();
        } else {
            game.handleSpawn(this, false);
            if (game.ship.inBase() && ship != 6) {
                spawn = new Spawn(false);
                ba.scheduleTask(spawn, ((SPAWN_TIME + 2 * SPAWN_BOUND) * Tools.TimeInMillis.SECOND));
            }
        }
    }
    
    /**
     * Increments and adjusts all relevant kill statistics.
     * @param dead The player object for the kill victim
     */
    public int handleKill(ElimPlayer dead) {
        stats.handleKill();
        if (System.currentTimeMillis() - lastKill < (MULTI_KILL_TIME * Tools.TimeInMillis.SECOND)){
            consecutiveKills++;
            multiKill();
        } else if (consecutiveKills > 0) {
            stats.handleMultiKill(consecutiveKills);
            consecutiveKills = 0;
        } else
            consecutiveKills = 0;
        lastKill = System.currentTimeMillis();
        stats.crunchAve(dead.getRating());
        //ba.sendPrivateMessage("WingZero", "name:" + name + " dead:"  + dead.getRating() + " getAve:" + stats.getAve(StatType.AVE));
        int killStreak = stats.getStat(StatType.KILL_STREAK);
        if (killStreak >= STREAK_INIT && (killStreak - STREAK_INIT) % STREAK_REPEAT == 0) {
            int i = (killStreak - STREAK_INIT) / STREAK_REPEAT;
            if (i >= streaks.length)
                i = streaks.length - 1;
            ba.sendArenaMessage(name + " - " + streaks[i] + "(" + killStreak + ":0)");
        }
        return stats.getStat(StatType.KILLS);
    }
    
    public void handlePosition(PlayerPosition event) {
        if (getLastDeath() < SPAWN_TIME || !isPlaying()) return; 
        int y = event.getYLocation();
        // not applicable for FR game (weasel)
        if (ship != 6) { 
            // is y coord above the boundary?
            if (y < BOUNDARY) {
                // inside base
                if (game.getState() == GameState.PLAYING) { 
                    if (status == Status.SPAWN) {
                        status = Status.IN;
                        if (spawn != null)
                            spawn.returned();
                    } else if (status == Status.WARNED_OUT) {
                        status = Status.WARNED_IN;
                        if (bounds != null)
                            bounds.returned();
                    }
                } else
                    status = Status.IN;
            } else {
                // outside base
                if (game.getState() == GameState.PLAYING) { 
                    if (status == Status.IN) {
                        if (bounds != null)
                            ba.cancelTask(bounds);
                        bounds = new Bounds();
                        status = Status.WARNED_OUT;
                        ba.scheduleTask(bounds, BOUNDS_TIME * Tools.TimeInMillis.SECOND);
                    } else if (status == Status.WARNED_IN) {
                        status = Status.OUT;
                        ba.spec(name);
                        ba.spec(name);
                        ba.sendArenaMessage(name + " is out. " + getScore() + " (Out of bounds abuse)");
                        remove();
                    }
                } else
                    status = Status.SPAWN;
            }
        } else {
            if (y < BOUNDARY) return;
            if (status == Status.IN) {
                handleWarp();
            }
        }
    }
    
    public boolean handleLagout() {
    	//Lagout counts as +1 death
    	stats.handleDeath();
    	
    	//Check if player has reached final number of deaths..
        if (game.bot.gameType == elim.ELIM && stats.getStat(StatType.DEATHS) >= specAt) {
            status = Status.OUT;
            stats.handleLoss(); //Reset win streak
            ba.spec(name);
            ba.spec(name);
            lagouts = 0; //Remaining lagouts set to 0 so player can not return.
            game.removePlayer(this);
        }
        
        if (getRemainingLagouts() > 0) {
            status = Status.LAGGED;
            cancelTasks();
            return false;
        } else {
            status = Status.OUT;
            ba.sendArenaMessage(name + " is out. " + getScore() + " (Lagged Out)");
            saveLoss();
            return true;
        }
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
        lastShot = System.currentTimeMillis();
        stats.incrementStat(StatType.SHOTS);
    }
    
    public void handleWarp() {
        if (stats.getStat(StatType.KILL_STREAK) > 1)
            lastStreak = stats.getStat(StatType.KILL_STREAK);
        stats.handleDeath();
        if (stats.getStat(StatType.DEATHS) >= specAt) {
            status = Status.OUT;
            ba.specWithoutLock(name);
            ba.sendArenaMessage(name + " is out. " + getScore() + " (warp abuse)");
            remove();
        } else {
            ba.sendPrivateMessage(name, "Warping is illegal! You gained a death as a result.");
            game.handleSpawn(this, true);
        }
    }
    
    public void handleStart() {
        if (spawn != null)
            spawn.returned();
        if (bounds != null)
            bounds.returned();
        if (status == Status.SPAWN) { 
            spawn = new Spawn(false);
            ba.scheduleTask(spawn, 2 * SPAWN_BOUND * Tools.TimeInMillis.SECOND);
        }
    }
    
    /** Returns this players current rating */
    public int getRating() {
        return stats.getRating();
    }
    
    /** Returns an int array with kills and deaths */
    public int[] getScores() {
        return new int[] { stats.getStat(StatType.KILLS), stats.getStat(StatType.DEATHS) };
    }
    
    public int getKills() {
        if (stats != null)
            return stats.getStat(StatType.KILLS);
        else {
            Tools.printLog("[ELIM] (kills) NullPointer: stats null for '" + name + "'");
            ba.sendSmartPrivateMessage("WingZero", "[ELIM] (kills) NullPointer: stats null for '" + name + "'");
            return 0;
        }
    }
    
    public int getDeaths() {
        if (stats != null)
            return stats.getStat(StatType.DEATHS);
        else {
            Tools.printLog("[ELIM] (deaths) NullPointer: stats null for '" + name + "'");
            ba.sendSmartPrivateMessage("WingZero", "[ELIM] (deaths) NullPointer: stats null for '" + name + "'");
            return 0;
        }
    }
    
    public double getAim() {
        if (stats != null)
            return stats.getAim(StatType.AIM);
        else {
            Tools.printLog("[ELIM] (aim) NullPointer: stats null for '" + name + "'");
            return 0;
        }
    }
    
    public int getLastShot() {
        return ((int)(System.currentTimeMillis() - lastShot) / Tools.TimeInMillis.SECOND);
    }
    
    public int getLastDeath() {
        return ((int)(System.currentTimeMillis() - lastDeath) / Tools.TimeInMillis.SECOND);
    }

    /** Returns the remaining lagouts left */
    public int getRemainingLagouts() {
        return lagouts;
    }
    
    /** Gets the freq this player was originally put on */
    public int getFreq() {
        return freq;
    }
    
    public Status getStatus() {
        return status;
    }
    
    /** Get kills and deaths String */
    public String getScore() {
        return "" + stats.getStat(StatType.KILLS) + " wins " + stats.getStat(StatType.DEATHS) + " losses";   
    }
    
    /** Return stat spam */
    public String[] getStatStrings() {
        return stats.getStats(name);
    }
    
    public String getStreakStats() {
        return stats.getStreak(name);
    }
    
    /** Change player status as specified */
    public void setStatus(Status s) {
        status = s;
    }
    
    /** Sets the freq for this player in the current game */
    public void setFreq(int f) {
        freq = f;
    }
    
    /** Lagout command execution */
    public void lagin() {
        status = Status.SPAWN;
        ba.setShip(name, game.ship.getNum());
        if (ba.getFrequencySize(getFreq()) == 0)
            ba.setFreq(name, getFreq());
        else {
            while (ba.getFrequencySize(game.freq) != 0)
                game.freq += 2;
            ba.setFreq(name, game.freq);
            setFreq(game.freq);
            game.freq += 2;
        }
        lagouts--;
        ba.sendPrivateMessage(name, "You have " + getRemainingLagouts() + " lagouts remaining.");
        game.handleSpawn(this, true);
        if (game.ship.inBase() && ship != 6) {
            spawn = new Spawn(false);
            ba.scheduleTask(spawn, ((SPAWN_TIME + 2 * SPAWN_BOUND) * Tools.TimeInMillis.SECOND));
        }
    }
    
    public boolean isPlaying() {
        return (status == Status.IN || status == Status.SPAWN || status == Status.WARNED_IN || status == Status.WARNED_OUT);
    }
    
    public boolean isLoaded() {
        if (stats == null) return false;
        return stats.isLoaded();
    }
    
    /** Returns this player's stat tracker */
    public ElimStats getStats() {
        return stats;
    }
    
    /** Record the loss of an elimination game and flush dynamic game stats */
    public void saveLoss() {
        cancelTasks();
        stats.handleLoss();
    }

    /** Record the win of an elimination game and flush dynamic game stats */
    public void saveWin() {
        cancelTasks();
        stats.handleWin();
    }
    
    public void cancelTasks() {
        if (spawn != null) {
            ba.cancelTask(spawn);
            spawn = null;
        }
        if (bounds != null) {
            ba.cancelTask(bounds);
            bounds = null;
        }
    }
    
    public void scorereset(int ship) {
        if (stats != null && stats.getShip() == ship)
            stats = null;
    }
    
    /** Feed stats from the database query into the local database stat reference */
    public void loadStats(ResultSet rs) throws SQLException {
        stats.loadStats(rs);
    }
    
    private void remove() {
        saveLoss();
        game.removePlayer(this);
    }
    
    private class Spawn extends TimerTask {
        
        boolean warned;
        
        public Spawn(boolean warning) {
            warned = warning;
        }
        
        @Override
        public void run() {
            if (status != Status.SPAWN) return;
            if (!warned) {
                ba.sendPrivateMessage(name, "GET IN THE BASE! Unless you want to experience the wrath of Robo Ref.");
                warned = true;
                spawn = new Spawn(true);
                ba.scheduleTask(spawn, SPAWN_BOUND * Tools.TimeInMillis.SECOND);
            } else {
                status = Status.OUT;
                ba.spec(name);
                ba.spec(name);
                ba.sendArenaMessage(name + " is out. " + getScore() + " (Too long outside base)");
                remove();
                spawn = null;
            }
        }
        
        public void returned() {
            ba.cancelTask(this);
            spawn = null;
        }
    }
    
    public class Bounds extends TimerTask {
        
        public Bounds() {
            ba.sendPrivateMessage(name, "WARNING: You have " + BOUNDS_TIME + " seconds to return to base or you will be disqualified.");
        }
        
        @Override
        public void run() {
            if (status == Status.WARNED_OUT) {
                status = Status.OUT;
                ba.spec(name);
                ba.spec(name);
                ba.sendArenaMessage(name + " is out. " + getScore() + " (Too long outside base)");
                remove();
            }
            bounds = null;
        }
        
        public void returned() {
            bounds = null;
            ba.sendPrivateMessage(name, "Thank you for returning to base! NOTE: Leaving again will result in automatic disqualification.");
            ba.cancelTask(this);
        }
    }
    
    
    
    /** Alert player's kill of multiple enemies at once */
    private void multiKill() {
        switch (consecutiveKills) {
            case 1: ba.sendArenaMessage(name + " - Double kill!", Tools.Sound.CROWD_OHH); break;
            case 2: ba.sendArenaMessage(name + " - Triple kill!", Tools.Sound.CROWD_GEE); break;
            case 3: ba.sendArenaMessage(name + " - Quadruple kill!", Tools.Sound.INCONCEIVABLE); break;
            case 4: ba.sendArenaMessage(name + " - Quintuple kill!", Tools.Sound.SCREAM); break;
            case 5: ba.sendArenaMessage(name + " - Sextuple kill!", Tools.Sound.CRYING); break;
            case 6: ba.sendArenaMessage(name + " - Septuple kill!", Tools.Sound.GAME_SUCKS); break;      
        }
    }
}
