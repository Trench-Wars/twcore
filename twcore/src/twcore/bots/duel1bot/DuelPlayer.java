package twcore.bots.duel1bot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.Tools;

public class DuelPlayer {

    private static final String db = "website";

    boolean create, registered, banned, enabled;
    BotAction ba;
    Player player;
    BotSettings rules;
    
    // Regular game stats
    PlayerStats stats;
    int ship = 1;
    int specAt = 5;
    int freq = 0;
    int status = 0;
    int out = -1;

    int rating;
    int userID;
    int userMID;
    String userIP;
    
    String staffer;

    UserData user;
    duel1bot bot;
    DuelGame game;

    String name;
    
    int[] safe, spawn;

    // TWEL Info
    int d_noCount;
    int d_deathWarp;
    int d_toWin;
    static int d_season;
    static int d_deathTime;
    static int d_spawnTime;
    static int d_spawnLimit;
    static int d_maxLagouts;

    // constant player state values for status
    static final int SPEC = -1;
    static final int IN = 0;
    static final int PLAYING = 1;
    static final int WARPING = 2;
    static final int LAGGED = 3;
    static final int OUT = 4;
    static final int REOUT = 5;
    static final int RETURN = 6;

    // constant player values for removal reason
    static final int NORMAL = 0;
    static final int WARPS = 1;
    static final int LAGOUTS = 2;
    static final int SPAWNS = 3;

    private long lastFoul = 0;
    private long lastDeath = 0;
    private long lastSpec = 0;
    private String lastKiller = "";

    int duelFreq = -1;
    boolean cancel;

    TimerTask spawner, dying;
    TimerTask lagout;

    public DuelPlayer(Player p, duel1bot bot) {
        staffer = null;
        cancel = false;
        name = p.getPlayerName();
        this.bot = bot;
        ba = bot.ba;
        rules = null;
        freq = p.getFrequency();
        ship = p.getShipType();
        rating = 1000;
        if (ship > 0)
            status = IN;
        else
            status = SPEC;
        create = false;
        registered = false;
        banned = false;
        enabled = false;
        userMID = -1;
        userIP = "";
        bot.lagChecks.add(name.toLowerCase());
        user = new UserData(ba, db, name, true);
        getRules();
        sql_setupUser();
    }

    public DuelPlayer(String p, duel1bot bot) {
        staffer = null;
        cancel = false;
        name = p;
        this.bot = bot;
        ba = bot.ba;
        rules = null;
        freq = -1;
        ship = -1;
        rating = -1;
        if (ship > 0)
            status = IN;
        else
            status = SPEC;
        create = false;
        registered = false;
        banned = false;
        enabled = false;
        userMID = -1;
        userIP = "";
        getRules();
        user = new UserData(ba, db, name);
        if (user == null || user.getUserID() < 1)
            user = null;
        else
            sql_setupUser();
    }

    private void getRules() {
        d_season = bot.d_season;
        d_deathTime = bot.d_deathTime;
        d_spawnTime = bot.d_spawnTime;
        d_spawnLimit = bot.d_spawnLimit;
        d_maxLagouts = bot.d_maxLagouts;
    }
    
    /** Handles position events  */
    public void handlePosition(PlayerPosition event) {
        if (status == WARPING || status == LAGGED || status == OUT || status == REOUT
                || status == RETURN) return;

        int x = event.getXLocation() / 16;
        int y = event.getYLocation() / 16;
        // Player p = ba.getPlayer(name);
        // 416 591
        if (game != null)
            if ((x < game.box.getAreaMinX()) || (y < game.box.getAreaMinY())
                    || (x > game.box.getAreaMaxX()) || (y > game.box.getAreaMaxY()))
                handleWarp(true);
    }

    public void handleFreq(FrequencyChange event) {
        if (status == WARPING || status == RETURN) return;
        int f = event.getFrequency();

        if (game != null) {
            if (f != freq)
                if (status == LAGGED) {
                    setStatus(WARPING);
                    ba.setFreq(name, freq);
                    setStatus(LAGGED);
                } else if (status == PLAYING) {
                    setStatus(WARPING);
                    ba.setFreq(name, freq);
                    //ba.specificPrize(name, -13);
                    setStatus(PLAYING);
                    handleWarp(false);
                } else if (status == OUT) {
                    ba.sendPrivateMessage(name, "Please stay on your freq until your duel is finished.");
                    ba.setFreq(name, freq);
                    setStatus(OUT);
                }
        } else if (bot.freqs.contains(f)) {
            if (freq == 9999) 
                ba.specWithoutLock(name);
            ba.setFreq(name, freq);
        } else {
            if (f != duelFreq) {
                bot.removeChalls(duelFreq);
                duelFreq = -1;
            }
            freq = f;
        }
    }

    public void handleFSC(FrequencyShipChange event) {
        int shipNum = event.getShipType();
        if (status == WARPING || ((status == LAGGED || status == OUT) && shipNum == 0) || status == RETURN) return;
        
        int f = event.getFrequency();
        int statusID = status;
        setStatus(WARPING);
        if (statusID == OUT) {
            ba.sendPrivateMessage(name, "Please stay in spec and on your freq until your duel is finished.");
            ba.specWithoutLock(name);
            if (freq != f) ba.setFreq(name, freq);
            setStatus(OUT);
            return;
        } else if (statusID == LAGGED) {
            ba.specWithoutLock(name);
            if (freq != f) 
                ba.setFreq(name, freq);
            ba.sendPrivateMessage(name, "Please use !lagout to return to your duel.");
            setStatus(LAGGED);
            return;
        }
        
        if (game == null) {
            if (shipNum == 4 || shipNum == 6) {
                if (ship != 0)
                    ba.setShip(name, ship);
                else
                    ba.specWithoutLock(name);
                ba.sendPrivateMessage(name, "Invalid ship!");
            } else if (shipNum == 0 && duelFreq > -1) {
                bot.removeChalls(duelFreq);
                duelFreq = -1;
                ship = shipNum;
            } else if (ship == 0 && shipNum > 0 && !registered) {
                ba.sendPrivateMessage(name, "NOTICE: You will not be able to play in ranked league duels until you have registered using !signup.");
                ship = shipNum;
            } else
                ship = shipNum;
        } else {
            boolean foul = false;
            if (shipNum == 0 && (statusID == PLAYING)) {
                ba.setFreq(name, freq);
                handleLagout();
                return;
            }

            if ((shipNum != ship) && (game.state != DuelGame.SETUP)) {
                foul = true;
                ba.setShip(name, ship);
                ba.specificPrize(name, -13);
            } else if ((game.div == 5) && (game.state == DuelGame.SETUP)) {
                if (shipNum == 6 || shipNum == 4)
                    ba.setShip(name, ship);
                else
                    ship = shipNum;
            } else if ((shipNum != ship) && (game.div != 5) && (game.state == DuelGame.SETUP)) 
                ba.setShip(name, ship);

            if (foul || game.state == DuelGame.IN_PROGRESS) {
                setStatus(statusID);
                handleWarp(false);
                return;
            } else
                warpToSafe();
        }
        setStatus(statusID);
    }
    
    /**
     * Sends a message to a lagged out player with return instructions.
     */
    public void handleReturn() {
        setStatus(RETURN);
        ba.specWithoutLock(name);
        ba.setFreq(name, freq);
        ba.sendPrivateMessage(name, "To return to your duel, reply with !lagout");
        setStatus(LAGGED);
    }

    public void handleDeath(String killerName) {
        long now = System.currentTimeMillis();
        DuelPlayer killer = bot.players.get(killerName.toLowerCase());
        if (game.getDeathWarp())
            warpToSafe();
        // DoubleKill check - remember to add a timer in case its the last death
        if (game.getNoCount() && (killer != null) && (killer.getTimeFromLastDeath() < 2001) && (name.equalsIgnoreCase(killer.getLastKiller()))) {
            ba.sendSmartPrivateMessage(name, "Double kill, doesn't count.");
            ba.sendSmartPrivateMessage(killerName, "Double kill, doesn't count.");
            killer.removeDeath();
            stats.decrementStat(StatType.KILLS);
        }

        lastDeath = now;
        lastKiller = killerName;

        if (stats.getStat(StatType.DEATHS) >= specAt) {
            setStatus(OUT);
            dying = new TimerTask() {
                @Override
                public void run() {
                    if (status == OUT) {
                        remove(NORMAL);
                        ba.cancelTask(spawner);
                    }
                }
            };
            ba.scheduleTask(dying, 2000);
        }

        spawner = new TimerTask() {
            @Override
            public void run() {
                // BACKTRACK
                if (game.getDeathWarp() && status == PLAYING)
                    warpToSpawn();
                else if (status == OUT) 
                    remove(NORMAL);
            }
        };
        ba.scheduleTask(spawner, d_deathTime * 1000);
        // BACKTRACK
        game.updateScore();
    }

    public void handleSpawnKill() {
        stats.handleSpawn();
        if (stats.getStat(StatType.SPAWNS) < d_spawnLimit)
            ba.sendPrivateMessage(name, "Spawn killing is illegal. If you should continue to spawn kill you will forfeit your match.");
        else
            remove(SPAWNS);
    }

    /**
     * Handles the event of a player warping.
     */
    public void handleWarp(boolean pos) {
        if (status == WARPING || status == RETURN) return;
        setStatus(WARPING);
        long now = System.currentTimeMillis();
        if ((now - lastFoul > 500) && (game.state == DuelGame.IN_PROGRESS))
            stats.handleWarp();
        if (stats.getStat(StatType.WARPS) < 5 && game.state == DuelGame.IN_PROGRESS) {
            if (now - lastFoul > 500)
                if (pos)
                    ba.sendPrivateMessage(name, "Warping is illegal in this league and if you warp again, you will forfeit.");
                else
                    ba.sendPrivateMessage(name, "Changing freq or ship is illegal in this league and if you do this again, you will forfeit.");
            warpPlayer();
        } else if (game.state != DuelGame.IN_PROGRESS)
            warpToSafe();
        else {
            ba.sendPrivateMessage(name, "You have forfeited due to warp abuse.");
            remove(WARPS);
        }
        lastFoul = now;
    }

    /**
     * Called when a player lags out of a game.
     */
    public void handleLagout() {
        if (game == null) return;
        setStatus(LAGGED);
        if (game.state == DuelGame.IN_PROGRESS) 
            stats.handleLagout();
        doPlaytime();
        if (stats.getStat(StatType.LAGOUTS) < d_maxLagouts) {
            ba.sendSmartPrivateMessage(name, "You have 1 minute to return (!lagout) to your duel or you will forfeit! (!lagout)");
            lagout = new TimerTask() {
                @Override
                public void run() {
                    bot.laggers.remove(name.toLowerCase());
                    ba.sendSmartPrivateMessage(name, "You have forfeited since you have been lagged out for over a minute.");
                    remove(LAGOUTS);
                    lagout = null;
                }
            };
            ba.scheduleTask(lagout, 60000);
            // BACKTRACK
            bot.laggers.put(name.toLowerCase(), this);
        } else {
            ba.sendSmartPrivateMessage(name, "You have exceeded the lagout limit and forfeit your duel.");
            remove(LAGOUTS);
        }
    }

    /**
     * Handles a player's !lagout command
     */
    public void doLagout() {
        if (status != LAGGED) {
            ba.sendPrivateMessage(name, "You are not lagged out.");
            return;
        }
        setStatus(RETURN);
        ba.cancelTask(lagout);
        bot.laggers.remove(name.toLowerCase());
        ba.sendPrivateMessage(name, "You have " + (d_maxLagouts - stats.getStat(StatType.LAGOUTS)) + " lagouts remaining.");
        lastFoul = System.currentTimeMillis();
        ba.setShip(name, ship);
        ba.setFreq(name, freq);
        if (game.state == DuelGame.IN_PROGRESS) {
            lastSpec = lastFoul;
            warpPlayer();
        } else if (game.state == DuelGame.SETUP) 
            warpToSafe();
    }

    /** Handles a player !signup command */
    public void doSignup() {
        if (registered) {
            ba.sendSmartPrivateMessage(name, "You have already registered to play.");
        } else if (!banned) {
            create = true;
            bot.debug("[signup] Attempting to signup player: " + name);
            ba.sendUnfilteredPrivateMessage(name, "*info");
        }
    }
    
    /** Handles a staffer force !signup command */
    public void doSignup(String staff) {
        if (registered)
            ba.sendSmartPrivateMessage(staff, name + " is already registered to play.");
        else if (!banned) {
            create = true;
            staffer = staff;
            bot.debug("[signup] Attempting to signup player: " + name);
            ba.sendUnfilteredPrivateMessage(name, "*info");
        }        
    }
    
    /** Handles a player !disable command */
    public void doDisable(String staff) {
        if (staff != null && staffer != null) {
            ba.sendSmartPrivateMessage(staff, "A separate command is currently in process, try again later.");
            return;
        } else if (staff != null)
            staffer = staff;
        
        if (registered && enabled && !banned)
            sql_disablePlayer();
        else if (staff == null)  
            ba.sendSmartPrivateMessage(name, "Could not disable because name is not registered/enabled or is banned.");
        else {
            staffer = null;
            ba.sendSmartPrivateMessage(staff, "Could not disable because name is not registered/enabled or is banned.");
        }
    }

    /** Handles a player !enable command */
    public void doEnable(String staff) {
        if (staff != null && staffer != null) {
            ba.sendSmartPrivateMessage(staff, "A separate command is currently in process, try again later.");
            return;
        } else if (staff != null)
            staffer = staff;
        
        if (registered && !enabled && !banned)
            sql_enablePlayer();
        else if (staff == null)  
            ba.sendSmartPrivateMessage(name, "A name can only be enabled if not banned and already registered but disabled");
        else {
            staffer = null;
            ba.sendSmartPrivateMessage(staff, "A name can only be enabled if not banned and already registered but disabled");
        }
    }

    public void doSetRules(String message) {
        String pieces[] = message.toLowerCase().split(" ");
        int winby2 = 0;
        int nc = 0;
        int warp = 0;
        int kills = 10;
        for (int i = 0; i < pieces.length; i++) {
            try {
                int numKills = Integer.parseInt(pieces[i]);
                if (numKills == 5 || numKills == 10)
                    kills = numKills;
            } catch (NumberFormatException e) {
                /*
                if (pieces[i].equals("winby2"))
                    winby2 = 1;
                else*/ if (pieces[i].equals("nc"))
                    nc = 1;
                else if (pieces[i].equals("warp"))
                    warp = 1;
            }
        }
        try {
            ResultSet result = ba.SQLQuery(db, "SELECT * FROM tblDuel1__player WHERE fcUserName = '" + Tools.addSlashesToString(name) + "'");
            if (result.next()) {
                ba.SQLQuery(db, "UPDATE tblDuel1__player  SET fnGameKills = " + kills + ", fnWinBy2 = " + winby2 + ", fnNoCount = " + nc + ", fnDeathWarp = " + warp
                        + " WHERE fcUserName = '" + Tools.addSlashesToString(name) + "'");
                String rules = "Rules: First to " + kills;
                /*
                if (winby2 == 1)
                    rules += ", Win By 2";
                    */
                if (nc == 1)
                    rules += ", No Count (nc) Double Kills";
                if (warp == 1)
                    rules += ", Warp On Deaths";
                ba.sendSmartPrivateMessage(name, rules);
            } else
                ba.sendSmartPrivateMessage(name, "You must be signed up before you can change your rules.");
            ba.SQLClose(result);
        } catch (Exception e) {
            ba.sendSmartPrivateMessage(name, "Unable to change rules.");
        }

    }
    
    public void doRating() {
        ba.sendPrivateMessage(name, "Current rating: " + rating);
    }
    
    public void doRec() {
        ba.sendPrivateMessage(name, "" + getKills() + ":" + getDeaths());
    }

    public void setDuel(DuelGame game, int freq, int[] coords) {
        this.game = game;
        duelFreq = freq;
        cancel = false;
        specAt = game.getSpecAt();
        // BACKTRACK
        bot.freqs.addElement(freq);
        int div = game.div;
        // ships
        if (div == 1)
            ship = 1;
        else if (div == 2)
            ship = 2;
        else if (div == 3)
            ship = 3;
        else if (div == 4)
            ship = 7;
        else if (div == 5) ship = -1;

        safe = new int[] { coords[2], coords[3] };
        spawn = new int[] { coords[0], coords[1] };
    }
    
    public void setRating(int r) {
        rating = r;
    }
    
    /** Resets duel freq (-1). */
    public void cancelDuel() {
        duelFreq = -1;
    }

    public String getName() {
        return name;
    }
    
    /** Determines if a player is eligible for league play */
    public boolean canPlay() {
        return registered && enabled && !banned;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isRegistered() {
        return registered;
    }
    
    public boolean isBanned() {
        return banned;
    }
    
    public boolean isSpecced() {
        return status != SPEC && status != OUT && status != REOUT && status != LAGGED;
    }
    
    public boolean isOut() {
        return out > -1;
    }

    /** Returns the number of milliseconds since the last death */
    public long getTimeFromLastDeath() {
        return System.currentTimeMillis() - lastDeath;
    }

    /** Returns the name of the last player to kill this player */
    public String getLastKiller() {
        return lastKiller;
    }
    
    public void startGame() {
        if (!bot.laggers.containsKey(name.toLowerCase())) {
            warp(spawn[0], spawn[1]);
            ba.sendPrivateMessage(name, "GO GO GO!!!", 104);
        }
        lastSpec = System.currentTimeMillis();
    }
    
    public void endTimePlayed() {
        if (out == -1 && lastSpec > 0) {
            int secs = (int) (System.currentTimeMillis() - lastSpec) / 1000;
            stats.handleTimePlayed(secs);
        }
        lastSpec = 0;
    }

    /** Resets the player tasks and warps to middle */
    public void endGame() {
        if (ship > 0)
            status = SPEC;
        else
            status = IN;
        ba.cancelTask(lagout);
        ba.cancelTask(spawner);
        ba.cancelTask(dying);
        ba.shipReset(name);
        ba.warpTo(name, 512, 502);
        out = -1;
    }

    /** Decrements a death and sets status accordingly */
    public void removeDeath() {
        if (stats.getStat(StatType.DEATHS) == specAt) setStatus(PLAYING);
        if (stats.getStat(StatType.DEATHS) > 0) stats.decrementStat(StatType.DEATHS);
    }

    /** Decrements a kill */
    public void removeKill() {
        if (stats.getStat(StatType.KILLS) > 0) stats.decrementStat(StatType.KILLS);
    }

    /** Increments a kill */
    public void addKill() {
        stats.handleKill();
    }

    /** Returns kills */
    public int getKills() {
        return stats.getStat(StatType.KILLS);
    }

    /** Returns deaths */
    public int getDeaths() {
        return stats.getStat(StatType.DEATHS);
    }
    
    public int getLagouts() {
        return stats.getStat(StatType.LAGOUTS);
    }

    /** Returns player status */
    public int getStatus() {
        return status;
    }
    
    public int getRating() {
        return rating;
    }
    
    public int getTime() {
        return stats.getStat(StatType.PLAYTIME);
    }
    
    public int getFreq() {
        return duelFreq;
    }

    /** Returns the ID of the removal reason */
    public int getReason() {
        return out;
    }

    public boolean getCancel() {
        return cancel;
    }

    /** Sets the player status */
    public void setStatus(int s) {
        status = s;
    }

    /**
     * Removes the player from the duel and reports the reason for it.
     *
     * @param reason
     */
    public void remove(int reason) {
        ba.specWithoutLock(name);
        ba.setFreq(name, freq);
        endTimePlayed();
        if (status == REOUT) {
            setStatus(OUT);
            return;
        }
        out = reason;
        if (stats.getStat(StatType.DEATHS) != specAt) 
            stats.setStat(StatType.DEATHS, specAt);
        setStatus(OUT);
        game.playerOut(this);
    }
    
    private void doPlaytime() {
    	//TODO: check in progress
        long now = System.currentTimeMillis();
        if (lastSpec > 0) {
            int secs = (int) (now - lastSpec) / 1000;
            stats.handleTimePlayed(secs);
            lastSpec = now;
        }
    }

    /** Warps the player to the specified coordinates (in tiles) */
    public void warp(int x, int y) {
        setStatus(WARPING);
        Player p1 = ba.getPlayer(name);
        ba.shipReset(name);
        ba.warpTo(name, x, y);
        p1.updatePlayerPositionManuallyAfterWarp(x, y);
        setStatus(PLAYING);
    }

    /** Warps the player after the player just warped */
    public void warpWarper(int x, int y) {
        setStatus(WARPING);
        Player p1 = ba.getPlayer(name);
        ba.warpTo(name, x, y);
        p1.updatePlayerPositionManuallyAfterWarp(x, y);
        setStatus(PLAYING);
    }

    /** Prepares the player for a duel in the given ship and coordinates. */
    public void starting(int div, int shipNum) {
        if (status == LAGGED) return;
        if (div != -1)
            sql_checkDivision(div);
        setStatus(WARPING);
        if (shipNum > -1)
            ba.setShip(name, ship);
        else if (ship == 0) {
            ba.setShip(name, 1);
            ship = 1;
        }
        ba.setFreq(name, freq);
        stats = new PlayerStats(ship);
        warpToSafe();
    }

    /** Cancels the duel */
    public void cancelGame(String name) {
        if (game == null) {
            ba.sendPrivateMessage(name, "No game found.");
            return;
        } else
            game.cancelDuel(name);
    }
    
    public boolean setCancel() {
        cancel = !cancel;
        return cancel;
    }
    
    private void warpPlayer() {
        WarpPoint wp = game.box.getRandomWarpPoint();
        warp(wp.getXCoord(), wp.getYCoord());
    }

    private void warpToSafe() {
        warp(safe[0], safe[1]);
    }

    private void warpToSpawn() {
        warp(spawn[0], spawn[1]);
    }
    
    public void sql_checkDivision(int div) {
        String query = "SELECT fnUserID, fnRating FROM tblDuel1__league WHERE fnSeason = " + d_season + " AND fnDivision = " + div + " AND fnUserID = " + userID + " LIMIT 1";
        ba.SQLBackgroundQuery(db, "league:" + userID + ":" + div + ":" + name, query);
    }

    public void sql_updateDivision(int div, boolean won, boolean aced) {
        String query = "UPDATE tblDuel1__league SET ";
        query += (won ? ("fnWins = fnWins + 1, ") : ("fnLosses = fnLosses + 1, "));
        query += "fnRating = " + rating + ", ";
        query += "fnKills = fnKills + " + stats.getStat(StatType.KILLS) + ", fnDeaths = fnDeaths + " + stats.getStat(StatType.DEATHS) + ", ";
        // TODO: add other fields (streaks)
        query += "fnWinStreak = " + stats.getStat(StatType.BEST_WIN_STREAK) + ", fnCurrentWinStreak = " + stats.getStat(StatType.WIN_STREAK) + ", ";
        query += (won && aced ? "fnAces = fnAces + 1" : ((!won && aced) ? "fnAced = fnAced + 1" : "")) + ", "; 
        query += "fnSpawns = fnSpawns + " + stats.getStat(StatType.SPAWNS) + ", "; 
        query += "fnLagouts = fnLagouts + " + stats.getStat(StatType.LAGOUTS) + " ";
        query += "WHERE fnSeason = " + d_season + " AND fnUserID = " + userID + " AND fnDivision = " + div;

        try {
            ba.SQLQueryAndClose(db, query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a player profile in the database using the given
     * IP address and MID as long as no active aliases are found
     * (unless a staffer is set to have issued the signup).
     *
     * @param ip
     *      IP String
     * @param mid
     *      machine ID number
     */
    public void sql_createPlayer(String ip, String mid) {
        if (!create) return;
        create = false;
        String query = "SELECT fnUserID FROM tblDuel1__player WHERE fnEnabled = 1 AND (fcIP = '" + ip + "' OR (fcIP = '" + ip + "' AND fnMID = " + mid + ")) OR fnUserID = " + userID;
        ResultSet rs = null;
        try {
            rs = ba.SQLQuery(db, query);
            if (staffer == null && rs.next()) {
                String ids = "" + rs.getInt(1);
                while (rs.next())
                    ids += ", " + rs.getInt(1);
                sql_reportAlias(ids);
            } else {
                query = "INSERT INTO tblDuel1__player (fnUserID, fcUserName, fcIP, fnMID) VALUES(" + userID + ", '" + Tools.addSlashesToString(name) + "', '" + Tools.addSlashesToString(ip) + "', " + mid + ")";
                ba.SQLQueryAndClose(db, query);
                registered = true;
                enabled = true;
                ba.sendSmartPrivateMessage(name, "You have been successfully registered to play ranked team duels!");
                if (staffer != null) {
                    ba.sendSmartPrivateMessage(staffer, "Registration successful for " + name);
                    staffer = null;
                }
            }
        } catch (SQLException e) {
            staffer = null;
            bot.debug("[sql_createPlayer] Error creating player: " + name + " IP: " + ip + " MID: " + mid);
            e.printStackTrace();
        } finally {
            ba.SQLClose(rs);
        }
    }
    
    /** Disables this player from league play and updates database */
    private void sql_disablePlayer() {
        String query = "UPDATE tblDuel1__player SET fnEnabled = 0 WHERE fnUserID = " + userID;
        try {
            ba.SQLQueryAndClose(db, query);
            enabled = false;
            create = false;
            ba.sendSmartPrivateMessage(name, "Successfully disabled name from use in 2v2 TWEL duels. ");
            if (staffer != null) {
                ba.sendSmartPrivateMessage(staffer, "Successfully disabled '" + name + "' from use in 2v2 TWEL duels. ");
                staffer = null;
            }
        } catch (SQLException e) {
            bot.debug("[sql_disablePlayer] Could not disable: " + name);
            e.printStackTrace();
        }
    }
    
    /** Enables the player and updates it in the database */
    private void sql_enablePlayer() {
        String query = "SELECT fnUserID FROM tblDuel2__player WHERE fnEnabled = 1 AND (fcIP = '" + userIP + "' OR (fcIP = '" + userIP + "' AND fnMID = " + userMID + ")) OR fnUserID = " + userID;
        ResultSet rs = null;
        try {
            rs = ba.SQLQuery(db, query);
            if (staffer == null && rs.next()) {
                String ids = "" + rs.getInt(1);
                while (rs.next())
                    ids += ", " + rs.getInt(1);
                sql_reportAlias(ids);
            } else {
                query = "UPDATE tblDuel2__player SET fnEnabled = 1 WHERE fnUserID = " + userID;
                ba.SQLQueryAndClose(db, query);
                enabled = true;
                ba.sendSmartPrivateMessage(name, "You have been successfully registered to play ranked team duels!");
                if (staffer != null) {
                    ba.sendSmartPrivateMessage(staffer, "Successfully enabled '" + name + "' for use in 2v2 TWEL duels. ");
                    staffer = null;
                }
            }
        } catch (SQLException e) {
            bot.debug("[sql_enablePlayer] Could not enable: " + name);
            e.printStackTrace();
        } finally {
            ba.SQLClose(rs);
        }
    }
    
    /**
     * Reports a list of aliases preventing the registration of the player.
     *
     * @param ids
     *      a String of user IDs of all the player's active aliases
     */
    private void sql_reportAlias(String ids) {
        String query = "SELECT fcUserName FROM tblUser WHERE fnUserID IN (" + ids + ")";
        ResultSet rs = null;
        try {
            rs = ba.SQLQuery(db, query);
            if (rs.next()) {
                String msg = "The following name(s) must first be disabled before you can register a different name: ";
                msg += rs.getString(1);
                while (rs.next())
                    msg += ", " + rs.getString(1);
                ba.sendSmartPrivateMessage(name, msg);
            } else
                ba.sendSmartPrivateMessage(name, "[ERROR] Failed to find alias names and/or register player.");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ba.SQLClose(rs);
        }
    }
    
    /** Prepares player information collected from the database */
    private void sql_setupUser() {
        userID = user.getUserID();
        name = user.getUserName();
        // check if registered
        String query = "SELECT fnEnabled, fcIP, fnMID, fnNoCount, fnDeathWarp, fnGameKills FROM tblDuel1__player WHERE fnUserID = " + userID + " LIMIT 1";
        ResultSet rs = null;
        try {
            rs = ba.SQLQuery(db, query);
            if (rs.next()) {
                registered = true;
                if (rs.getInt("fnEnabled") == 1)
                    enabled = true;
                userIP = rs.getString("fcIP");
                userMID = rs.getInt("fnMID");
                d_noCount = rs.getInt("fnNoCount");
                d_deathWarp = rs.getInt("fnNoCount");
                d_toWin = rs.getInt("fnGameKills");
            }
            ba.SQLClose(rs);
            query = "SELECT fnActive FROM tblDuel1__ban WHERE fnUserID = " + userID + " AND fnActive = 1";
            rs = ba.SQLQuery(db, query);
            if (rs.next())
                banned = true;
        } catch (SQLException e) {
            bot.debug("[sql_setupUser] Exception when checking if registered: " + name);
            e.printStackTrace();
        } finally {
            ba.SQLClose(rs);
        }
    }

    public void sql_getRating(String div) {
        String query = "SELECT fnRating FROM tblDuel1__league WHERE fnUserID = " + userID + " AND fnDivision = " + div + " LIMIT 1";
        ResultSet rs = null;
        try {
            rs = ba.SQLQuery(db, query);
            if (rs.next())
                rating = rs.getInt("fnRating");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ba.SQLClose(rs);
        }
    }
}
