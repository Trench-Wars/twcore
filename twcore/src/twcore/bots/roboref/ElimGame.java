package twcore.bots.roboref;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.TreeSet;

import twcore.bots.roboref.ElimPlayer.BasePos;
import twcore.bots.roboref.ElimPlayer.Status;
import twcore.bots.roboref.roboref.ShipType;
import twcore.bots.roboref.roboref.State;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * A game of elim
 * 
 * @author WingZero
 */
public class ElimGame {
    
    BotAction ba;
    BotSettings rules;
    roboref bot;

    private static final int BASE_ENTRANCE = 222 * 16;
    static final int HIDER_TIME = 60;                   // minimum seconds between shots before announcing location
    static final int HIDER_CHECK = 30;                   // seconds between hider checks
    static final int MAX_LAG_TIME = 60;                 // seconds
    static final int MIN_LAG_TIME = 10;                 // seconds
    static final int OUT_OF_BOUNDS = 30;                // seconds
    static final int BOUNDARY_TIME = 20;                // max seconds outside base until dq
    static final int BOUND_START = 10;                  // seconds after game starts until player is warned for oob
    static final int SPAWN_TIME = 5;                    // seconds after death until respawn
    public static final String db = "website";
    
    CompareAll comp;
    CompareDeaths compDeath;
    CompareNames compName;
    
    ShipType ship;
    int freq;
    int deaths;
    int playerCount;
    int ratingCount;
    boolean shrap, statCheck, started;
    TimerTask starter;
    
    HashMap<String, ElimPlayer> players;    // holds any ElimPlayer regardless of activity
    HashMap<String, ElimPlayer> played;     // contains only players who actually played in the current game
    HashMap<String, OutOfBounds> outsiders; // player names mapped to out of bounds timers for base games
    HashMap<String, SpawnTimer> spawns;     // used for players who havent entered base ater start
    TreeSet<String> winners;
    HashSet<String> losers;
    HashSet<String> loaded;                 // list used to make sure all players have stat tracking ready
    HashMap<String, Lagout> laggers;
    ElimPlayer winner;
    String mvp;
    
    /**
     * Representation of a single elim event and all relevant player and game info.
     * 
     * @param bot Reference to roboref class
     * @param type ShipTyp for this game
     * @param deaths Death count for elimination 
     * @param shrap Shrap prized on spawn
     */
    public ElimGame(roboref bot, ShipType type, int deaths, boolean shrap) {
        this.bot = bot;
        ba = bot.ba;
        comp = new CompareAll();
        compDeath = new CompareDeaths();
        compName = new CompareNames();
        rules = bot.rules;
        ship = type;
        freq = ship.getFreq();
        this.deaths = deaths;
        this.shrap = shrap;
        ratingCount = 0;
        playerCount = 0;
        statCheck = false;
        started = false;
        winner = null;
        mvp = null;
        players = new HashMap<String, ElimPlayer>();
        played = new HashMap<String, ElimPlayer>();
        winners = new TreeSet<String>();
        losers = new HashSet<String>();
        loaded = new HashSet<String>();
        laggers = new HashMap<String, Lagout>();
        outsiders = new HashMap<String, OutOfBounds>();
        spawns = new HashMap<String, SpawnTimer>();
        Iterator<Player> i = ba.getPlayingPlayerIterator();
        while (i.hasNext()) {
            Player p = i.next();
            String name = p.getPlayerName();
            String low = name.toLowerCase();
            ElimPlayer ep = new ElimPlayer(ba, name);
            ep.loadStats(ship.getNum(), deaths);
            players.put(low, ep);
            winners.add(low);
            requestStats(name);
        }
    }
    
    /** Digest and diffuse player death event and related info */
    public void handleEvent(PlayerDeath event) {
        String killer = ba.getPlayerName(event.getKillerID());
        String killee = ba.getPlayerName(event.getKilleeID());
        if (killer == null || killee == null) return;
        if (spawns.containsKey(low(killee)))
            ba.cancelTask(spawns.remove(low(killee)));
        if (outsiders.containsKey(low(killee)))
            ba.cancelTask(outsiders.remove(low(killee)));
        ElimPlayer win = getPlayer(killer);
        ElimPlayer loss = getPlayer(killee);
        if (win != null && loss != null) {
            String streak = win.handleKill(loss);
            if (streak != null)
                ba.sendArenaMessage(streak);
            boolean[] res = loss.handleDeath(win);
            if (res[1])
                ba.sendArenaMessage("Kill Joy! " + killer + " terminates the (" + loss.getLastKillStreak() + ":0) kill streak of " + killee + "!", Tools.Sound.INCONCEIVABLE);
            if (res[0]) {
                ba.sendArenaMessage(killee + " is out. " + loss.getScore());
                removePlayer(loss);
            } else
                handleSpawn(loss, false);
        }
        
    }
    
    /** Catch potential lagouts and/or update player game status */
    public void handleEvent(FrequencyShipChange event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null) return;
        if (!started) {
            if (event.getShipType() > 0) {
                laggers.remove(name.toLowerCase());
                ElimPlayer ep = getPlayer(name);
                if (ep != null) {
                    ep.loadStats(ship.getNum(), deaths);
                } else {
                    ep = new ElimPlayer(ba, name);
                    players.put(name.toLowerCase(), ep);
                    requestStats(name);
                }
                ep.setStatus(Status.IN);
                if (ship.inBase())
                    ep.setPosition(BasePos.SPAWNING);
                winners.add(name.toLowerCase());
            } else
                winners.remove(name.toLowerCase());
        } else if (event.getShipType() == 0) {
            handleLagout(name);
            if (ship.inBase()) {
                if (outsiders.containsKey(low(name)))
                    ba.cancelTask(outsiders.remove(low(name)));
                if (spawns.containsKey(low(name)))
                    ba.cancelTask(spawns.remove(low(name)));
                ElimPlayer ep = getPlayer(name);
                if (ep != null)
                    ep.setPosition(BasePos.SPAWNING);
            }
        }
    }
    
    public void handleEvent(PlayerPosition event) {
        if (!ship.inBase()) return;
        Player p = ba.getPlayer(event.getPlayerID());
        if (p == null) return;
        int y = event.getYLocation();
        ElimPlayer ep = getPlayer(p.getPlayerName());
        if (ep != null && ep.status == Status.IN) {
            if (y > BASE_ENTRANCE) {
                if (!started) {
                    if (ep.getPosition() != BasePos.SPAWNING)
                        ep.setPosition(BasePos.SPAWNING);
                } else if (ep.getPosition() == BasePos.IN) {
                    if (ship != ShipType.WEASEL)
                        outsiders.put(low(ep.name), new OutOfBounds(ep, true));
                    else {
                        if (ep.handleWarp()) {
                            ba.sendArenaMessage(ep.name + " is out. " + ep.getScore() + " (warp abuse)");
                            removePlayer(ep);
                        } else {
                            ba.sendPrivateMessage(ep.name, "Warping is illegal! You gained a death as a result.");
                            sendWarp(ep.name);
                        }
                    }
                } else if (ep.getPosition() == BasePos.WARNED_IN)
                    removeOutsider(ep);
            } else {
                if (ep.getPosition() == BasePos.SPAWN)
                    ep.setPosition(BasePos.IN);
                else if (ep.getPosition() == BasePos.SPAWNING)
                    ep.setPosition(BasePos.IN);
                if (spawns.containsKey(low(ep.name)))
                    spawns.remove(low(ep.name)).returned();
                if (outsiders.containsKey(low(ep.name)))
                    outsiders.get(low(ep.name)).returned();
            }
        }
    }
    
    /** Handle left event for potential lagouts */
    public void handleEvent(PlayerLeft event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null || !winners.contains(low(name))) return;
        if (started) {
            handleLagout(name);
            if (ship.inBase()) {
                if (outsiders.containsKey(low(name)))
                    ba.cancelTask(outsiders.remove(low(name)));
                if (spawns.containsKey(low(name)))
                    spawns.remove(low(name)).returned();
                ElimPlayer ep = getPlayer(name);
                if (ep != null)
                    ep.setPosition(BasePos.SPAWNING);
            }
        } else {
            winners.remove(low(name));
            ElimPlayer ep = getPlayer(name);
            ep.setStatus(Status.SPEC);
            if (ship.inBase()) 
                ep.setPosition(BasePos.SPAWNING);
        }
    }
    
    public void handleSpawn(ElimPlayer ep, boolean instant) {
        final String name = ep.name;
        ep.setPosition(BasePos.SPAWNING);
        
        if (ship.inBase()) {
            if (ship != ShipType.WEASEL) {
                if (!instant)
                    spawns.put(low(ep.name), new SpawnTimer(ep, true));
                else
                    spawns.put(low(ep.name), new SpawnTimer(ep, false));
            } else {
                if (!instant) {
                    TimerTask warp = new TimerTask() {
                        public void run() {
                            sendWarp(name);
                        }
                    };
                    ba.scheduleTask(warp, SPAWN_TIME * Tools.TimeInMillis.SECOND + 100);
                } else
                    sendWarp(name);
            }
        }
        
        if (!instant) {
            TimerTask prize = new TimerTask() {
                public void run() {
                    sendPrizes(name);
                }
            };
            ba.scheduleTask(prize, SPAWN_TIME * Tools.TimeInMillis.SECOND + 200);
        } else
            sendPrizes(name);
    }
    
    /** Handles a lagged out player */
    public void handleLagout(String name) {
        ElimPlayer ep = getPlayer(name);
        if (ep != null && ep.status == Status.IN) {
            if (ep.getLagouts() > 0) {
                ep.setStatus(Status.LAGGED);
                laggers.put(name.toLowerCase(), new Lagout(name));
            } else {
                ba.sendArenaMessage(name + " is out. " + ep.getScore() + " (Too many lagouts)");
                removePlayer(ep);
            }
            if (ship.inBase()) {
                if (outsiders.containsKey(low(name)))
                    ba.cancelTask(outsiders.remove(low(name)));
                if (spawns.containsKey(low(name)))
                    ba.cancelTask(spawns.remove(low(name)));
            }
        }
        winners.remove(name.toLowerCase());
        if (started)
            checkWinner();
    }
    
    public void handleStats(String name, ResultSet rs) {
        ElimPlayer ep = getPlayer(name);
        if (ep == null) return;
        ep.loadStats(ship.getNum(), deaths);
        try {
            ep.loadStats(rs);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
        if (ep.isLoaded())
            loaded.add(low(name));
        if (statCheck)
            checkStats();
    }
    
    public void checkStats() {
        statCheck = true;
        HashSet<String> errors = new HashSet<String>();
        for (String name : winners) {
            if (!loaded.contains(name))
                errors.add(name);
        }
        if (errors.isEmpty()) {
            bot.debug("All player stats loaded and ready!");
            startGame();
        } else {
            bot.debug("Error, " + errors.size() + " players missing stat records.");
            for (String name : errors)
                requestStats(name);
        }
        ba.sendArenaMessage("All player stats loaded successfully.");
    }
    
    private void requestStats(String name) {
        ElimPlayer ep = getPlayer(name);
        if (ep != null) {
            if (ep.getStats() == null)
                ep.loadStats(ship.getNum(), deaths);
        }
        ba.SQLBackgroundQuery(db, "load:" + name, "SELECT * FROM tblElim__Player WHERE fnShip = " + ship.getNum() + " AND fcName = '" + Tools.addSlashesToString(name) + "' LIMIT 1");
    }
    
    /** Start elim 10 second countdown */
    public void startGame() {
        ba.sendArenaMessage("Get ready. Game will start in 10 seconds!", 1);
        starter = new TimerTask() {
            public void run() {
                if (winners.size() < 2) {
                    bot.abort();
                    return;
                }
                bot.state = State.PLAYING;
                started = true;
                ba.sendArenaMessage("GO GO GO!!!", Tools.Sound.GOGOGO);
                outsiders.clear();
                spawns.clear();
                ba.scoreResetAll();
                ba.shipResetAll();
                if (shrap) {
                    ba.prizeAll(Tools.Prize.SHRAPNEL);
                    ba.prizeAll(Tools.Prize.SHRAPNEL);
                } else {
                    ba.prizeAll(-Tools.Prize.SHRAPNEL);
                    ba.prizeAll(-Tools.Prize.SHRAPNEL);
                }
                ba.prizeAll(Tools.Prize.MULTIFIRE);
                for (String name : winners) {
                    ElimPlayer ep = getPlayer(name);
                    if (ep != null) {
                        if (ship == ShipType.WEASEL)
                            sendWarp(name);
                        else if (ship.inBase() && ep.getPosition() == BasePos.SPAWNING)
                            spawns.put(low(name), new SpawnTimer(ep, false));
                    }
                }
                countStats();
                starter = null;
            }
        };
        ba.scheduleTask(starter, 10 * Tools.TimeInMillis.SECOND);
        setShipFreqs();
    }
    
    /** Handles the !lagout player return command */
    public void do_lagout(String name) {
        if (laggers.containsKey(low(name))) {
            String msg = laggers.get(low(name)).lagin();
            ElimPlayer ep = getPlayer(name);
            if (msg != null)
                ba.sendPrivateMessage(name, msg);
            else if (ep != null) {
                ba.setShip(name, ship.getNum());
                ba.setFreq(name, ep.getFreq());
                ba.sendPrivateMessage(name, "You have " + ep.getLagouts() + " lagouts remaining.");
                if (ship == ShipType.WEASEL)
                    sendWarp(name);
            }
        } else
            ba.sendPrivateMessage(name, "You are not lagged out.");
    }
    
    /** Handles the grunt work for the !deaths command */
    public void do_deaths(String name) {
        List<ElimPlayer> list = getPlayed();
        if (list.size() > 0) {
            ElimPlayer[] best = new ElimPlayer[3];
            ElimPlayer[] worst = new ElimPlayer[3];
            Collections.sort(list, Collections.reverseOrder(compDeath));
            for (int i = 0; i < 3 && i < list.size(); i++)
                best[i] = list.get(i);
            Collections.sort(list, compDeath);
            for (int i = 0; i < 3 && i < list.size(); i++)
                worst[i] = list.get(i);
            ArrayList<String> msg = new ArrayList<String>();
            msg.add(",---- Most Deaths ---- Max: " + deaths + " -----.---- Least Deaths ----------------."); //34
            msg.add("|" + padString(" 1) " + best[0].name + " (" + best[0].getKills() + "-" + best[0].getDeaths() + ")", 34) + 
                    "|" + padString(" 1) " + worst[0].name + " (" + worst[0].getKills() + "-" + worst[0].getDeaths() + ")", 34) + "|");
            if (list.size() > 1) {
                msg.add("|" + padString(" 2) " + best[1].name + " (" + best[1].getKills() + "-" + best[1].getDeaths() + ")", 34) + 
                        "|" + padString(" 2) " + worst[1].name + " (" + worst[1].getKills() + "-" + worst[1].getDeaths() + ")", 34) + "|");
            }
            if (list.size() > 2) {
                msg.add("|" + padString(" 3) " + best[2].name + " (" + best[2].getKills() + "-" + best[2].getDeaths() + ")", 34) + 
                        "|" + padString(" 3) " + worst[2].name + " (" + worst[2].getKills() + "-" + worst[2].getDeaths() + ")", 34) + "|");
            }
            msg.add("`----------------------------------|----------------------------------+");
            ba.privateMessageSpam(name, msg.toArray(new String[msg.size()]));
        } else
            ba.sendPrivateMessage(name, "No death stats available.");
    }
    
    /** Handles the grunt work called for by the !mvp command */
    public void do_mvp(String name) {
        List<ElimPlayer> list = getPlayed();
        ElimPlayer[] best = new ElimPlayer[3];
        ElimPlayer[] worst = new ElimPlayer[3];
        if (list.size() > 0) {
            Collections.sort(list, Collections.reverseOrder(comp));
            for (int i = 0; i < 3 && i < list.size(); i++)
                best[i] = list.get(i);
            Collections.sort(list, comp);
            for (int i = 0; i < 3 && i < list.size(); i++)
                worst[i] = list.get(i);
            ArrayList<String> msg = new ArrayList<String>();
            msg.add(" ,-- Best Records -------------------.-- Worst Records ------------------.");
            msg.add(" |" + padString(" 1) " + best[0].name + " (" + best[0].getKills() + "-" + best[0].getDeaths() + ")", 35) +
                    "|" + padString(" 1) " + worst[0].name + " (" + worst[0].getKills() + "-" + worst[0].getDeaths() + ")", 35) + "|");
            if (list.size() > 1)
                msg.add(" |" + padString(" 2) " + best[1].name + " (" + best[1].getKills() + "-" + best[1].getDeaths() + ")", 35) +
                        "|" + padString(" 2) " + worst[1].name + " (" + worst[1].getKills() + "-" + worst[1].getDeaths() + ")", 35) + "|");
            if (list.size() > 2)
                msg.add(" |" + padString(" 3) " + best[2].name + " (" + best[2].getKills() + "-" + best[2].getDeaths() + ")", 35) +
                        "|" + padString(" 3) " + worst[2].name + " (" + worst[2].getKills() + "-" + worst[2].getDeaths() + ")", 35) + "|");
            msg.add(" `-----------------------------------|-----------------------------------+");
            ba.privateMessageSpam(name, msg.toArray(new String[msg.size()]));
        } else
            ba.sendPrivateMessage(name, "No MVP stats available.");
    }
    
    /** Handles the grunt work called for by the !who command */
    public void do_who(String name) {
        String msg = "" + winners.size() + " players remaining (* means lagged):";
        ba.sendPrivateMessage(name, msg);
        msg = "";
        for (String p : winners) {
            ElimPlayer ep = getPlayer(p);
            if (p != null)
                msg += ep.name + "(" + ep.getKills() + "-" + ep.getDeaths() + "), ";
        }
        for (String p : laggers.keySet()) {
            ElimPlayer ep = getPlayer(p);
            if (p != null)
                msg += "*" + ep.name + "(" + ep.getKills() + "-" + ep.getDeaths() + "), ";            
        }
        if (msg.length() > 0 && msg.contains(","))
            msg = msg.substring(0, msg.lastIndexOf(","));
        ba.sendPrivateMessage(name, msg);
    }
    
    public void do_streak(String name, String cmd) {
        String p = name;
        if (cmd.contains(" ") && cmd.length() > 9)
            p = cmd.substring(cmd.indexOf(" ") + 1);
        ElimPlayer ep = getPlayer(p);
        if (ep != null)
            ba.privateMessageSpam(name, ep.getStreakStats());
        else
            ba.sendPrivateMessage(name, "Error, player not found.");
    }
    
    /** Record losses for anyone still lagged out */
    public void storeLosses() {
        for (Lagout lagger : laggers.values()) {
            ba.cancelTask(lagger);
            ElimPlayer ep = getPlayer(lagger.name);
            if (ep != null) {
                ep.setStatus(Status.SPEC);
                ep.saveLoss();
                bot.updatePlayer(ep);
            }
        }
        laggers.clear();
    } 
    
    /** Stores the finished game information to the database */
    public void storeGame() {
        for (OutOfBounds oob : outsiders.values())
            ba.cancelTask(oob);
        for (SpawnTimer spawn : spawns.values())
            ba.cancelTask(spawn);
        outsiders.clear();
        spawns.clear();
        int aveRating = ratingCount / playerCount;
        String query = "INSERT INTO tblElim__Game (fnShip, fcWinner, fnSpecAt, fnKills, fnDeaths, fnPlayers, fnRating) " +
                "VALUES(" + ship.getNum() + ", '" + Tools.addSlashesToString(winner.name) + "', " + deaths + ", " + winner.getScores()[0] + ", " + winner.getScores()[1] + ", " + playerCount + ", " + aveRating + ")";
        ba.SQLBackgroundQuery(db, null, query);
    }
    
    /** Returns a List of ElimPlayers built from an ElimPlayer array (used for sorting) */
    public List<ElimPlayer> getPlayed() {
        return Arrays.asList(played.values().toArray(new ElimPlayer[played.size()]));
    }
    
    /** Returns the number of players currently in-game */
    public int getPlaying() {
        return winners.size();
    }
    
    /** Get ElimPlayer for the given player name */
    public ElimPlayer getPlayer(String name) {
        return players.get(name.toLowerCase());
    }
    
    /** Confirm player stats updated and return if all players have been updated */
    public boolean gotUpdate(String name) {
        losers.remove(name.toLowerCase());
        winners.remove(name.toLowerCase());
        if (winners.isEmpty() && losers.isEmpty())
            return true;
        else return false;
    }
    
    /** Remove player from the game player list into the loser list and flush stats */
    public void removePlayer(ElimPlayer loser) {
        loser.setStatus(Status.OUT);
        if (ship.inBase()) {
            if (spawns.containsKey(low(loser.name)))
                ba.cancelTask(spawns.remove(low(loser.name)));
            if (outsiders.containsKey(low(loser.name)))
                ba.cancelTask(outsiders.remove(low(loser.name)));
        }
        ba.specWithoutLock(loser.name);
        winners.remove(low(loser.name));
        losers.add(low(loser.name));
        loser.saveLoss();
        bot.updatePlayer(loser);
        checkWinner();
    }
    
    /** Removes a player due to out of bounds violation */
    private void removeOutsider(ElimPlayer player) {
        ba.sendArenaMessage(player.name + " is out. " + player.getScore() + " (Too long outside base)");
        removePlayer(player);
    }
    
    /** Helper method adds spaces to a String to meet a certain length */
    private String padString(String str, int length) {
        for (int i = str.length(); i < length; i++) 
            str += " ";
        return str.substring(0, length);
    }

    /** Count number of players and sum player ratings */
    private void countStats() {
        for (String name : winners) {
            ElimPlayer ep = getPlayer(name);
            played.put(low(name), ep);
            playerCount++;
            ratingCount += ep.getRating();
        }
    }
    
    /** Set all players to correct ship and distribute onto incrementing freqs */
    private void setShipFreqs() {
        if (ship == ShipType.WEASEL)
            ba.setDoors(127);
        else if (winners.size() > 15)
            ba.setDoors(228);
        else
            ba.setDoors(127);
        
        for (String name : winners) {
            getPlayer(name).setFreq(freq);
            ba.setShip(name, ship.getNum());
            ba.setFreq(name, freq);
            freq += 2;
        }
    }
    
    /** Check if game has been won */
    private void checkWinner() {
        if (winners.size() == 1) {
            winner = getPlayer(winners.first());
            winner.saveWin();
            setMVP();
            storeGame();
            bot.setWinner(winner);      
        }
    }
    
    /** Determines MVP using a 3-tier Comparator */
    private void setMVP() {
        List<ElimPlayer> list = getPlayed();
        Collections.sort(list, Collections.reverseOrder(comp));
        mvp = list.get(0).name;
    }
    
    /** Warps a Weasel into the flag room */
    private void sendWarp(String name) {
        int[] coords = rules.getIntArray("XArena" + bot.random.nextInt(4), ",");
        ba.warpTo(name, coords[0], coords[1], coords[2]);
    }
    
    /** Sends the appropriate prizes to player */
    private void sendPrizes(String name) {
        if (shrap)
            ba.specificPrize(name, Tools.Prize.SHRAPNEL);
        else {
            ba.specificPrize(name, -Tools.Prize.SHRAPNEL);
            ba.specificPrize(name, -Tools.Prize.SHRAPNEL);
        }
        ba.specificPrize(name, Tools.Prize.MULTIFIRE);
    }
    
    public void stop() {
        if (starter != null) {
            ba.cancelTask(starter);
            starter = null;
        }
        for (Lagout t : laggers.values())
            ba.cancelTask(t);
        for (SpawnTimer t : spawns.values())
            ba.cancelTask(t);
        for (OutOfBounds t : outsiders.values())
            ba.cancelTask(t);
    }
    
    /** Lazy toLowerCase() String helper */
    private String low(String str) {
        return str.toLowerCase();
    }
    
    /** TimerTask used for tracking a player after lagging out and potential return */
    private class Lagout extends TimerTask {
        
        String name;
        long time;
        boolean active;
        
        /** Create, schedule and alert new lagout */
        public Lagout(String name) {
            this.name = name;
            time = System.currentTimeMillis();
            active = true;
            ba.scheduleTask(this, MAX_LAG_TIME * Tools.TimeInMillis.SECOND);
            ba.sendSmartPrivateMessage(name, "It appears you have lagged out! You have " + timeLeft() + " seconds to return using !lagout.");
        }
        
        /** Record game loss */
        public void run() {
            if (!active) return;
            active = false;
            ElimPlayer ep = getPlayer(name);
            if (ep != null) {
                laggers.remove(low(name));
                removePlayer(ep);
                ba.sendPrivateMessage(name, "You have exceeded the maximum lagout time and are therefore eliminated.");
            }
        }
        
        /** Either puts player back in game having done !lagout or reports too early */
        public String lagin() {
            long now = System.currentTimeMillis();
            if (now - time < MIN_LAG_TIME * Tools.TimeInMillis.SECOND)
                return "You must wait " + (MIN_LAG_TIME - ((now - time) / Tools.TimeInMillis.SECOND)) + " seconds to return.";
            else {
                active = false;
                ba.cancelTask(this);
                laggers.remove(low(name));
                winners.add(low(name));
                ElimPlayer ep = getPlayer(name);
                ep.lagin();
                return null;
            }
        }
        
        /** Seconds remaining until lagout expires */
        public long timeLeft() {
            if (active)
                return MAX_LAG_TIME - ((System.currentTimeMillis() - time) / Tools.TimeInMillis.SECOND);
            else
                return 0;
        }
    }
    
    /** TimerTask used to officiate outside of base violations for in-base elim games */
    private class OutOfBounds extends TimerTask {

        ElimPlayer player;
        boolean lastWarning;
        
        
        /**
         * Constructs and schedules an Out Of Bounds timer for one player
         * @param ep ElimPlayer
         * @param lastWarning Boolean used to indicate if the player has just spawned or was in base before hand
         */
        public OutOfBounds(ElimPlayer ep, boolean lastWarning) {
            bot.debug("OutOfBounds timer created for: " + ep.name);
            ba.scheduleTask(this, BOUNDARY_TIME * Tools.TimeInMillis.SECOND);
            player = ep;
            player.sendOutsideWarning(BOUNDARY_TIME);
            this.lastWarning = lastWarning;
        }
        
        @Override
        public void run() {
            if (player.getPosition() == BasePos.WARNED_OUT)
                removeOutsider(player);
        }
        
        /** Handles the clean up when a player reaches base before the Timer executes */
        public void returned() {
            bot.debug("OutOfBounds timer return canceled for: " + player.name);
            ba.cancelTask(this);
            outsiders.remove(low(player.name));
            if (lastWarning)
                player.setPosition(BasePos.WARNED_IN);
            else
                player.setPosition(BasePos.IN);
        }
    }
    
    /** TimerTask used to prevent players from remaining in the spawn area after dying */
    private class SpawnTimer extends TimerTask {
        
        ElimPlayer player;
        
        /**
         * Constructs and schedules a SpawnTimer for one player
         * @param ep ElimPlayer
         * @param spawning Boolean determines if time should be added to compensate for post-death respawn delay
         */
        public SpawnTimer(ElimPlayer ep, boolean spawning) {
            player = ep;
            bot.debug("Spawn timer created for: " + ep.name);
            if (spawning) {
                player.setPosition(BasePos.SPAWNING);
                ba.scheduleTask(this, (BOUND_START + SPAWN_TIME) * Tools.TimeInMillis.SECOND);
            } else
                ba.scheduleTask(this, BOUND_START * Tools.TimeInMillis.SECOND);
        }
        
        @Override
        public void run() {
            if (player.getPosition() == BasePos.SPAWNING)
                outsiders.put(low(player.name), new OutOfBounds(player, false));
            spawns.remove(low(player.name));
        }

        /** Handles the clean up when a player reaches base before the Timer executes */
        public void returned() {
            bot.debug("Spawn timer return canceled for: " + player.name);
            ba.cancelTask(this);
            spawns.remove(low(player.name));
            if (player.getPosition() != BasePos.IN)
                player.setPosition(BasePos.IN);
        }
    }
    
    private class HiderFinder extends TimerTask {

        public HiderFinder() {
            ba.scheduleTask(this, Tools.TimeInMillis.MINUTE, HIDER_CHECK * Tools.TimeInMillis.SECOND);
        }
        
        @Override
        public void run() {
            
        }
        
    }
    
    /** Comparator used to determine the MVP of a game. Player with highest kills -> least deaths -> best aim -> random */
    public class CompareAll implements Comparator<ElimPlayer> {
        @Override
        public int compare(ElimPlayer p1, ElimPlayer p2) {
            // kills -> least deaths -> accuracy
            if (p1.getKills() > p2.getKills())
                return 1;
            else if (p2.getKills() > p1.getKills())
                return -1;
            else if (p1.getDeaths() < p2.getDeaths())
                return 1;
            else if (p2.getDeaths() < p1.getDeaths())
                return -1;
            else if (p1.getAim() > p2.getAim())
                return 1;
            else if (p2.getAim() > p1.getAim())
                return -1;
            else
                return 0;
        }
    }
    
    /** Comparator used to compare ElimPlayer objects according to death stats */
    public class CompareDeaths implements Comparator<ElimPlayer> {
        @Override
        public int compare(ElimPlayer p1, ElimPlayer p2) {
            if (p1.getDeaths() > p2.getDeaths())
                return 1;
            else if (p1.getDeaths() < p2.getDeaths())
                return -1;
            else
                return 0;
        }
    }
    
    /** Comparator used to compare names alphabetically (unused probably) */
    public class CompareNames implements Comparator<ElimPlayer> {
        @Override
        public int compare(ElimPlayer p1, ElimPlayer p2) {
            return p1.name.compareToIgnoreCase(p2.name);
        }
    }
    
    /** Returns a string describing the current elimination game */
    public String toString() {
        String ret = "We are playing " + ship.toString() + " elim to " + deaths + " deaths";
        if (ship.hasShrap()) {
            if (shrap)
                ret += " with shrap";
            else
                ret += " without shrap";
        }
        ret += ". " + winners.size() + " players remaining";
        return ret;
    }
}
