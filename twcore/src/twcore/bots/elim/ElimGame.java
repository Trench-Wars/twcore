package twcore.bots.elim;

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
import java.util.Vector;

import twcore.bots.elim.ElimPlayer.Status;
import twcore.bots.elim.elim.ShipType;
import twcore.bots.elim.elim.State;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;
import twcore.core.lag.LagHandler;
import twcore.core.lag.LagReport;
import twcore.core.util.Tools;

/**
 * A game of elim
 * 
 * @author WingZero
 */
public class ElimGame {
    
    BotAction ba;
    BotSettings rules;
    elim bot;

    enum GameState { NA, STATS, STARTING, PLAYING, ENDING };
    
    static final int HIDER_TIME = 60;                   // minimum seconds between shots before announcing location
    static final int HIDER_CHECK = 40;                   // seconds between hider checks
    static final int MAX_LAG_TIME = 60;                 // seconds
    static final int MIN_LAG_TIME = 10;                 // seconds
    static final int BOUNDARY_TIME = 20;                // max seconds outside base until dq
    static final int BOUND_START = 10;                  // seconds after game starts until player is warned for oob
    static final int SPAWN_TIME = 5;                    // seconds after death until respawn
    public static final String db = "website";
    
    CompareAll comp;
    CompareDeaths compDeath;
    CompareNames compName;
    
    GameState state;
    ShipType ship;
    int freq;
    int deaths;
    int playerCount;
    int ratingCount;
    boolean shrap;
    TimerTask starter;
    
    HiderFinder hiderFinder;
    
    Vector<String> lagChecks;
    HashMap<String, ElimPlayer> players;    // holds any ElimPlayer regardless of activity
    HashMap<String, ElimPlayer> played;     // contains only players who actually played in the current game
    TreeSet<String> winners;
    HashSet<String> losers;
    HashSet<String> loaded;                 // list used to make sure all players have stat tracking ready
    HashMap<String, Lagout> laggers;
    ElimPlayer winner;
    String mvp;
    
    TimerTask lagCheck;
    
    public LagHandler lagHandler;
    
    
    /**
     * Representation of a single elim event and all relevant player and game info.
     * 
     * @param bot Reference to elim class
     * @param type ShipTyp for this game
     * @param deaths Death count for elimination 
     * @param shrap Shrap prized on spawn
     */
    public ElimGame(elim bot, ShipType type, int deaths, boolean shrap) {
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
        state = GameState.NA;
        winner = null;
        mvp = null;
        hiderFinder = null;
        players = new HashMap<String, ElimPlayer>();
        played = new HashMap<String, ElimPlayer>();
        winners = new TreeSet<String>();
        losers = new HashSet<String>();
        loaded = new HashSet<String>();
        laggers = new HashMap<String, Lagout>();
        lagChecks = new Vector<String>();
        lagCheck = null;
        lagHandler = new LagHandler(ba, rules, this, "handleLagReport");
        Iterator<Player> i = ba.getPlayingPlayerIterator();
        while (i.hasNext()) {
            Player p = i.next();
            String name = p.getPlayerName();
            String low = name.toLowerCase();
            winners.add(low);
            lagChecks.add(low);
            ElimPlayer ep = new ElimPlayer(ba, this, name, ship.getNum(), deaths);
            players.put(low, ep);
        }
    }
    
    /** Digest and diffuse player death event and related info */
    public void handleEvent(PlayerDeath event) {
        String killer = ba.getPlayerName(event.getKillerID());
        String killee = ba.getPlayerName(event.getKilleeID());
        if (killer == null || killee == null) return;
        ElimPlayer win = getPlayer(killer);
        ElimPlayer loss = getPlayer(killee);
        if (win != null && loss != null) {
            win.handleKill(loss);
            loss.handleDeath(win);
        }
    }
    
    /** Catch potential lagouts and/or update player game status */
    public void handleEvent(FrequencyShipChange event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null) return;
        if (state == GameState.NA) {
            if (event.getShipType() > 0) {
                if (laggers.containsKey(low(name)))
                    ba.cancelTask(laggers.remove(low(name)));
                ElimPlayer ep = getPlayer(name);
                if (ep == null) {
                    ep = new ElimPlayer(ba, this, name, ship.getNum(), deaths);
                    players.put(name.toLowerCase(), ep);
                }
                winners.add(name.toLowerCase());
                lagChecks.add(low(name));
                if (ship.inBase())
                    ep.setStatus(Status.SPAWN);
                else
                    ep.setStatus(Status.IN);
            } else {
                winners.remove(name.toLowerCase());
                lagChecks.remove(low(name));
                played.remove(low(name));
                ElimPlayer ep = getPlayer(name);
                if (ep != null)
                    ep.setStatus(Status.SPEC);                
            }
        } else if (event.getShipType() == 0) {
            if (state == GameState.NA || state == GameState.STARTING) {
                winners.remove(name.toLowerCase());
                lagChecks.remove(low(name));
                played.remove(low(name));
                ElimPlayer ep = getPlayer(name);
                if (ep != null)
                    ep.setStatus(Status.SPEC);                
            } else if (state == GameState.PLAYING)
                handleLagout(name);
        }
    }
    
    /** Passed a PlayerPosition event used to check out of bounds */
    public void handleEvent(PlayerPosition event) {
        if (!ship.inBase()) return;
        Player p = ba.getPlayer(event.getPlayerID());
        if (p == null) return;
        ElimPlayer ep = getPlayer(p.getPlayerName());
        if (ep != null)
            ep.handlePosition(event);
    }
    
    public void handleEvent(WeaponFired event) {
        if (event.getWeaponType() != WeaponFired.WEAPON_BOMB && event.getWeaponType() != WeaponFired.WEAPON_BULLET) 
            return; 
        if (ship == ShipType.JAVELIN && event.getWeaponType() == WeaponFired.WEAPON_BULLET) return; 
        String name = ba.getPlayerName(event.getPlayerID());
        if (name != null) {
            ElimPlayer ep = getPlayer(name);
            if (ep != null)
                ep.handleShot();
        }
    }
    
    /** Handle left event for potential lagouts */
    public void handleEvent(PlayerLeft event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null || !winners.contains(low(name))) return;
        if (state == GameState.PLAYING)
            handleLagout(name);
        else if (state == GameState.NA || state == GameState.STARTING){
            winners.remove(low(name));
            lagChecks.remove(low(name));
            played.remove(low(name));
            ElimPlayer ep = getPlayer(name);
            if (ep != null)
            	ep.setStatus(Status.SPEC);
        }
    }
    
    public void handleEvent(Message event) {
        if (event.getMessageType() == Message.ARENA_MESSAGE)
            lagHandler.handleLagMessage(event.getMessage());
    }
    
    public void cmd_getRating(String name, String cmd) {
        String p = cmd.substring(cmd.indexOf(" ") + 1);
        if (players.containsKey(low(p))) {
            ElimPlayer player = players.get(low(p));
            ElimStats stats = player.getStats();
            if (stats != null) { 
                ElimStat stat = stats.stats.get(StatType.RATING);
                ba.sendSmartPrivateMessage(name, "" + p + ": I:" + stat.getInt() + " D:" + stat.getDouble() + " F:" + stat.getFloat());
                stat = stats.total.get(StatType.RATING);
                ba.sendSmartPrivateMessage(name, "" + p + " Total: I:" + stat.getInt() + " D:" + stat.getDouble() + " F:" + stat.getFloat());
            }
        }
    }
    
    /** Handles a lag report received from the lag handler */
    public void handleLagReport(LagReport report) {
        if (!report.isBotRequest())
            ba.privateMessageSpam(report.getRequester(), report.getLagStats());
        if (report.isOverLimits()) {
            if (!report.isBotRequest())
                ba.sendPrivateMessage(report.getRequester(), report.getLagReport());
            ElimPlayer p = getPlayer(report.getName());
            if (p != null && ba.getPlayer(report.getName()).getShipType() != 0 && p.getStatus() == Status.IN) {
                ba.sendPrivateMessage(report.getName(), report.getLagReport());
                p.handleLagout();
                ba.spec(report.getName());
                ba.spec(report.getName());
            }
        }
    }
    
    /** Handles spawning related tasks like warping and prizing */
    public void handleSpawn(ElimPlayer ep, boolean instant) {
        final String name = ep.name;
        if (ship.inBase()) {
            if (ship == ShipType.WEASEL) {
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
        winners.remove(low(name));
        lagChecks.remove(low(name));
        ElimPlayer ep = getPlayer(name);
        if (ep != null && ep.isPlaying()) {
            if (!ep.handleLagout())
                laggers.put(low(name), new Lagout(name));
            else 
                removePlayer(ep);
        }
        if (state == GameState.PLAYING)
            checkWinner();
    }
    
    /** Passes a ResultSet to a player for stat loading */
    public void handleStats(String name, ResultSet rs) {
        ElimPlayer ep = getPlayer(name);
        if (ep == null) return;
        try {
            bot.debug("Sending stats to: " + name);
            ep.loadStats(rs);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
        if (ep.isLoaded())
            loaded.add(low(name));
        if (state == GameState.STATS)
            checkStats();
    }
    
    /** Receives the result of the *where command used for hider coordinates */
    public void handleHider(String msg) {
        if (hiderFinder != null && msg.contains(":") && msg.length() - msg.indexOf(":") <= 5)
            hiderFinder.revealHider(msg);
    }
    
    /** Ensures that each player playing in the current game has had stats successfully loaded */
    public void checkStats() {
        if (state == GameState.NA)
            state = GameState.STATS;
        HashSet<String> errors = new HashSet<String>();
        for (String name : winners) {
            if (!loaded.contains(name))
                errors.add(name);
        }
        if (errors.isEmpty()) {
            bot.debug("All player stats loaded and ready!");
            state = GameState.STARTING;
            startGame();
        } else {
            bot.debug("Error, " + errors.size() + " players missing stat records.");
            final HashSet<String> errs = errors;
            TimerTask task = new TimerTask() {
                public void run() {
                    for (String name : errs)
                        requestStats(name);
                }
            };
            ba.scheduleTask(task, 3000);            
        }
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
                state = GameState.PLAYING;
                ba.sendArenaMessage("GO GO GO!!!", Tools.Sound.GOGOGO);
                ba.shipResetAll();
                if (shrap)
                    ba.prizeAll(Tools.Prize.SHRAPNEL);
                else {
                    ba.prizeAll(-Tools.Prize.SHRAPNEL);
                    ba.prizeAll(-Tools.Prize.SHRAPNEL);
                }
                ba.prizeAll(Tools.Prize.MULTIFIRE);
                for (String name : winners) {
                    ElimPlayer ep = getPlayer(name);
                    if (ep != null) {
                        ba.scoreReset(name);
                        if (ship.inBase() && ship != ShipType.WEASEL)
                            ep.handleStart();
                        else if (ship == ShipType.WEASEL)
                        	sendWarp(name);
                    }
                }
                if (!ship.inBase())
                    hiderFinder = new HiderFinder();
                countStats();
                
                lagCheck = new TimerTask() {
                    public void run() {
                        String name = lagChecks.remove(0);
                        lagChecks.add(name);
                        lagHandler.requestLag(name);
                    }
                };
                ba.scheduleTask(lagCheck, 3000);
                starter = null;
                if (playerCount > 9)
                    ba.sendArenaMessage("The winner of this game gets pubbux!");
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
                handleSpawn(ep, true);
            }
        } else
            ba.sendPrivateMessage(name, "You are not lagged out.");
    }
    
    public void do_lag(String req, String name) {
        lagHandler.requestLag(name, req);
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
            ba.smartPrivateMessageSpam(name, msg.toArray(new String[msg.size()]));
        } else
            ba.sendSmartPrivateMessage(name, "No death stats available.");
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
            ba.smartPrivateMessageSpam(name, msg.toArray(new String[msg.size()]));
        } else
            ba.sendSmartPrivateMessage(name, "No MVP stats available.");
    }
    
    /** Handles the grunt work called for by the !who command */
    public void do_who(String name) {
        String msg = "" + winners.size() + " players remaining (* prefix == lagged out):";
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
        ba.sendSmartPrivateMessage(name, msg);
    }
    
    /** Handles grunt work for the !streak command which shows current streak stats */
    public void do_streak(String name, String cmd) {
        String p = name;
        if (cmd.contains(" ") && cmd.length() > 9)
            p = cmd.substring(cmd.indexOf(" ") + 1);
        ElimPlayer ep = getPlayer(p);
        if (ep != null)
            ba.sendPrivateMessage(name, ep.getStreakStats());
        else
            ba.sendPrivateMessage(name, "Error, player not found.");
    }
    
    /** Does the grunt work of the !hider command which toggles the HiderFinder task */
    public void do_hiderFinder(String name) {
        if (hiderFinder != null) {
            hiderFinder.stop();
            ba.sendSmartPrivateMessage(name, "HiderFinder DISABLED");
        } else {
            hiderFinder = new HiderFinder();
            ba.sendSmartPrivateMessage(name, "HiderFinder ENABLED");
        }
    }
    
    /** Handles the grunt work for the !remove player command */
    public void do_remove(String name, String player) {
        String temp = ba.getFuzzyPlayerName(player);
        if (temp != null && temp.equalsIgnoreCase(player)) {
            winners.remove(low(player));
            lagChecks.remove(low(player));
            losers.remove(low(player));
            played.remove(low(player));
            ElimPlayer ep = players.remove(low(player));
            if (ep != null)
                ep.cancelTasks();
            ba.spec(player);
            ba.spec(player);
            ba.sendPrivateMessage(player, "You have been forcibly removed from the game.");
            ba.sendPrivateMessage(name, player + " has been removed from the game.");
            checkWinner();
        } else
            ba.sendPrivateMessage(name, "Player '" + player + "' not found in the arena.");
    }
    
    public void do_scorereset(ElimPlayer ep, int ship) {
        ep.cancelTasks();
        if (laggers.containsKey(low(ep.name)))
            ba.cancelTask(laggers.remove(low(ep.name)));
        lagChecks.remove(low(ep.name));
        winners.remove(low(ep.name));
        losers.remove(low(ep.name));
        played.remove(low(ep.name));
        loaded.remove(low(ep.name));
        players.remove(low(ep.name));
        ep.scorereset(ship);
    }
    
    /** Record losses for anyone still lagged out */
    public void storeLosses() {
        for (Lagout lagger : laggers.values()) {
            ba.cancelTask(lagger);
            ElimPlayer ep = getPlayer(lagger.name);
            if (ep != null) {
                ep.setStatus(Status.OUT);
                ep.saveLoss();
                bot.updatePlayer(ep);
            }
        }
        laggers.clear();
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
    
    public GameState getState() {
        return state;
    }
    
    /** Confirm player stats updated and return if all players have been updated */
    public boolean gotUpdate(String name) {
        losers.remove(name.toLowerCase());
        winners.remove(name.toLowerCase());
        lagChecks.remove(low(name));
        if (winners.isEmpty() && losers.isEmpty())
            return true;
        else return false;
    }
    
    /** Remove player from the game player list into the loser list and flush stats */
    public void removePlayer(ElimPlayer loser) {
        winners.remove(low(loser.name));
        lagChecks.remove(low(loser.name));
        losers.add(low(loser.name));
        bot.updatePlayer(loser);
        checkWinner();
    }
    
    /** Cancels all tasks and game related variables */
    public void stop() {
        ba.cancelTasks();
        if (starter != null)
            starter = null;
        if (lagCheck != null)
            lagCheck = null;
    }
    
    /** Prepares the player object for receiving stats and sends sql query */
    private void requestStats(String name) {
        ba.SQLBackgroundQuery(db, "load:" + name, "SELECT * FROM tblElim__Player WHERE fnShip = " + ship.getNum() + " AND fcName = '" + Tools.addSlashesToString(name) + "' LIMIT 1");
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
            if (ep != null) {
                played.put(low(name), ep);
                playerCount++;
                ratingCount += ep.getRating();
            }
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
        if (state != GameState.PLAYING) return;
        if (winners.size() == 1) {
            ba.cancelTask(lagCheck);
            lagCheck = null;
            state = GameState.ENDING;
            if (hiderFinder != null)
                hiderFinder.stop();
            winner = getPlayer(winners.first());
            winner.saveWin();
            storeLosses();
            setMVP();
            bot.storeGame(winner, (ratingCount / playerCount), playerCount);
        } else if (winners.size() == 0) {
            ba.cancelTask(lagCheck);
            lagCheck = null;
            state = GameState.ENDING;
            if (hiderFinder != null)
                hiderFinder.stop();
            storeLosses();
            bot.deadGame();
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
        if (ship != ShipType.WEASEL) {
            bot.debug("SendWarp attempt while ship was not weasel!");
            return;
        }
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
    
    /** Lazy toLowerCase() String helper */
    private String low(String str) {
        return str.toLowerCase();
    }
    
    /** TimerTask used for tracking a player after lagging out and potential return */
    private class Lagout extends TimerTask {
        
        String name;
        long time;
        
        /** Create, schedule and alert new lagout */
        public Lagout(String name) {
            this.name = name;
            time = System.currentTimeMillis();
            ba.scheduleTask(this, MAX_LAG_TIME * Tools.TimeInMillis.SECOND);
            ba.sendSmartPrivateMessage(name, "It appears you have lagged out! You have " + timeLeft() + " seconds to return using !lagout.");
        }
        
        /** Record game loss */
        public void run() {
            played.remove(low(name));
            ElimPlayer ep = getPlayer(name);
            if (ep != null) {
                laggers.remove(low(name));
                ep.saveLoss();
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
                laggers.remove(low(name));
                winners.add(low(name));
                ElimPlayer ep = getPlayer(name);
                ep.lagin();
                ba.cancelTask(this);
                return null;
            }
        }
        
        /** Seconds remaining until lagout expires */
        public long timeLeft() {
            return MAX_LAG_TIME - ((System.currentTimeMillis() - time) / Tools.TimeInMillis.SECOND);
        }
    }
    
    /** Repeating TimerTask that checks for any potential hiding players using last shot and death */ 
    private class HiderFinder extends TimerTask {

        boolean reported;
        HashSet<String> where;
        
        /** Constructs and schedules the HiderFinder task */
        public HiderFinder() {
            where = new HashSet<String>();
            reported = false;
            ba.scheduleTask(this, Tools.TimeInMillis.MINUTE, HIDER_CHECK * Tools.TimeInMillis.SECOND);
        }
        
        @Override
        public void run() {
            if (winners.size() > 2) {
                for (String p : winners) {
                    ElimPlayer ep = getPlayer(p);
                    if (ep != null) {
                        if (isHiding(ep)) 
                            getCoord(ep.name);
                    }
                }
            } else if (winners.size() == 2) {
                ElimPlayer p1 = getPlayer(winners.first());
                ElimPlayer p2 = getPlayer(winners.last());
                if (p1 != null && p2 != null) {
                    if (p1.name.equals(p2.name))
                        return;
                    if (isHiding(p1) && isHiding(p2)) {
                        if (!reported) {
                            reported = true;
                            ba.sendUnfilteredPublicMessage("?cheater " + p1.name + " and " + p2.name + " possibly stalling elim by refusing to fight.");
                        }
                    } else {
                        if (isHiding(p1))
                            getCoord(p1.name);
                        if (isHiding(p2))
                            getCoord(p2.name);
                    }
                }
            }
        }
        
        /** Cancels and nullifies the hider finder */
        public void stop() {
            ba.cancelTask(this);
            hiderFinder = null;
        }
        
        /** Checks last shot and death times and returns true if hiding */
        private boolean isHiding(ElimPlayer ep) {
            return ep.getLastShot() > HIDER_TIME  && ep.getLastDeath() > HIDER_TIME;
        }
        
        /** Request coordinates of a player in order to alert other players */
        private void getCoord(String name) {
            where.add(low(name));
            ba.sendUnfilteredPrivateMessage(name, "*where");
        }
        
        /** Sends the player's coordinates to all other players */
        public void revealHider(String msg) {
            String name = msg.substring(0, msg.indexOf(":"));
            if (!where.remove(low(name)))
                return;
            String coord = msg.substring(msg.indexOf(":") + 1);
            for (String p : winners) {
                if (!p.equalsIgnoreCase(name))
                    ba.sendPrivateMessage(p, name + " is potentially hiding at:" + coord + " - Get em!");
            }
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
