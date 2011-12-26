package twcore.bots.duel2bot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import twcore.bots.duel2bot.DuelBox;
import twcore.bots.duel2bot.DuelGame;
import twcore.bots.duel2bot.DuelPlayer;
import twcore.bots.duel2bot.DuelTeam;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;

/**
 * TeamDuel bot for TWEL 2v2 scrimmage and league play. Uses a more detailed
 * stat tracking system (from new elim bot). It is built on a simplified game-play
 * setup mechanics where an unlocked arena allows certain duel settings to be 
 * inferred rather than specified as would have been done by typed commands previously. 
 *
 * @author WingZero
 */
public class duel2bot extends SubspaceBot{

    public static final String DB = "website";
    final int MSG_LIMIT = 8;
    
    BotSettings settings;
    OperatorList oplist;
    BotAction ba;
    
    int d_spawnTime;
    int d_spawnLimit;
    int d_season;
    int d_deathTime;
    int d_maxLagouts;
    int d_noCount;
    int d_challengeTime;
    int d_duelLimit;
    int d_duelDays;
    
    // current non-league duel id number
    private int                    scrimID;

    // current league duel id number
    private int                    gameID;

    private String greet = "Welcome! To play, get in a ship and on a freq with a friend. Then, challenge another freq that also has 2 players. Use !help";
    
    private String                 debugger  = "";
    private boolean                DEBUG;
    // non-league teams will be identified by negative numbers :: i may not use
    // this at all and just go by player names
    HashMap<Integer, DuelGame>     games;
    // list of players who are playing tied to their duel id
    HashMap<String, Integer>       playing;
    // list of DuelBoxes
    HashMap<String, DuelBox>       boxes;
    // list of players and associated profile
    HashMap<String, DuelPlayer>    players;

    // ?????????
    HashMap<Integer, DuelTeam>     teams;
    // list of players currently lagged out
    HashMap<String, DuelPlayer>    laggers;
    // list of scrimmage challenges
    HashMap<String, DuelChallenge> challs;
    // list of used frequencies
    Vector<Integer>                freqs;
    
    public duel2bot(BotAction botAction) {
        super(botAction);
        ba = botAction;

        EventRequester events = ba.getEventRequester();
        events.request(EventRequester.MESSAGE);
        events.request(EventRequester.LOGGED_ON);
        events.request(EventRequester.ARENA_JOINED);
        events.request(EventRequester.PLAYER_DEATH);
        events.request(EventRequester.PLAYER_POSITION);
        events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(EventRequester.FREQUENCY_CHANGE);
        events.request(EventRequester.PLAYER_LEFT);
        events.request(EventRequester.PLAYER_ENTERED);

        boxes = new HashMap<String, DuelBox>();
        games = new HashMap<Integer, DuelGame>();
        teams = new HashMap<Integer, DuelTeam>();
        players = new HashMap<String, DuelPlayer>();
        playing = new HashMap<String, Integer>();
        laggers = new HashMap<String, DuelPlayer>();
        challs = new HashMap<String, DuelChallenge>();
        freqs = new Vector<Integer>();
        
        DEBUG = true;
        debugger = "WingZero";
    }

    @Override
    public void handleEvent(LoggedOn event) {
        settings = ba.getBotSettings();
        oplist = ba.getOperatorList();
        ba.joinArena(settings.getString("Arena"));

        // Create new box Objects
        int boxCount = settings.getInt("BoxCount");
        for (int i = 1; i <= boxCount; i++) {
            String boxset[] = settings.getString("Box" + i).split(",");
            String warps[] = settings.getString("Warp" + i).split(",");
            String area[] = settings.getString("Area" + i).split(",");
            if (boxset.length == 17) boxes.put("" + i, new DuelBox(boxset, warps, area, i));
        }

        // Reads in general settings for dueling
        d_season = settings.getInt("Season");
        d_spawnTime = settings.getInt("SpawnAfter");
        d_spawnLimit = settings.getInt("SpawnLimit");
        d_deathTime = settings.getInt("SpawnTime");
        d_noCount = settings.getInt("NoCount");
        d_maxLagouts = settings.getInt("LagLimit");
        d_challengeTime = settings.getInt("ChallengeTime");
        d_duelLimit = settings.getInt("DuelLimit");
        d_duelDays = settings.getInt("DuelDays");
        ba.setReliableKills(1);
        gameID = 0;
        scrimID = 0;
    }

    @Override
    public void handleEvent(ArenaJoined event) {
        // handle player list
        String arena = ba.getArenaName();
        if (!arena.equalsIgnoreCase("duel2")) return;
        ba.shipResetAll();
        ba.warpAllToLocation(512, 502);
        Iterator<Player> i = ba.getPlayerIterator();
        while (i.hasNext()) {
            Player p = i.next();
            players.put(p.getPlayerName().toLowerCase(), new DuelPlayer(p, this));
        }
    }

    @Override
    public void handleEvent(Message event) {
        String msg = event.getMessage();
        String cmd = msg.toLowerCase();
        int type = event.getMessageType();

        if (type == Message.ARENA_MESSAGE) {
            if (msg.startsWith("IP:")) {
                handleInfo(msg);
            }
        }
        
        String name = ba.getPlayerName(event.getPlayerID());
        if (oplist.isBotExact(name)) return;

        if (type == Message.PRIVATE_MESSAGE || type == Message.PUBLIC_MESSAGE) {
            if (cmd.equals("!signup"))
                cmd_signup(name);
            else if (cmd.equals("!disable"))
                cmd_disable(name);
            else if (cmd.equals("!enable"))
                cmd_enable(name);
            else if (cmd.startsWith("!chr ") || cmd.startsWith("!challenge "))
                cmd_challenge(name, splitArgs(msg), true);
            else if (cmd.startsWith("!ch ") || cmd.startsWith("!challenge "))
                cmd_challenge(name, splitArgs(msg), false);
            else if (cmd.startsWith("!a ") || cmd.startsWith("!accept "))
                cmd_accept(name, splitArgs(msg));
            else if (cmd.startsWith("!lagout"))
                cmd_lagout(name);
            else if (cmd.startsWith("!help") || (cmd.startsWith("!h")))
                cmd_help(name);
            else if (cmd.startsWith("!score")) 
                cmd_score(name, msg);
            else if (cmd.equals("!teams"))
                cmd_teams(name);
        }

        if (oplist.isModerator(name)
                && (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE)) {
            if (cmd.startsWith("!die"))
                ba.die();
            else if (cmd.startsWith("!signup ") && cmd.length() > 9)
                cmd_signup(name, msg);
            else if (cmd.startsWith("!debug"))
                cmd_debug(name);
            else if (cmd.startsWith("!cancel"))
                cmd_cancel(name, msg);
            else if (cmd.startsWith("!players"))
                cmd_players();
            else if (cmd.startsWith("!games"))
                cmd_games();
            else if (cmd.startsWith("!freqs"))
                cmd_freqs();
            else if (cmd.startsWith("!challs")) cmd_challs();
        }
    }
    
    @Override
    public void handleEvent(PlayerEntered event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        /* Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null. */
        if (ptest == null) return;
        String name = ptest.getPlayerName();

        // greet
        // add to player list
        // retrieve/create player profile
        // refresh teams list
        if (laggers.containsKey(name.toLowerCase())) {
            players.put(name.toLowerCase(), laggers.get(name.toLowerCase()));
            players.get(name.toLowerCase()).handleReturn();

        } else {
            players.put(name.toLowerCase(), new DuelPlayer(ptest, this));
            ba.sendPrivateMessage(name, greet);
        }
    }

    @Override
    public void handleEvent(PlayerLeft event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        if (ptest == null) return;
        String name = ptest.getPlayerName();
        // remove from player lists
        // refresh teams list
        if (players.containsKey(name.toLowerCase()))
            if (!laggers.containsKey(name.toLowerCase()))
                players.remove(name.toLowerCase()).handleLagout();

    }

    @Override
    public void handleEvent(FrequencyChange event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        /* Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null. */
        if (ptest == null) return;

        String name = ptest.getPlayerName();
        if (players.containsKey(name.toLowerCase()))
            players.get(name.toLowerCase()).handleFreq(event);
    }

    @Override
    public void handleEvent(PlayerDeath event) {
        Player ptest = ba.getPlayer(event.getKilleeID());
        Player ptest2 = ba.getPlayer(event.getKillerID());
        /* Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null. */
        if (ptest == null || ptest2 == null) return;

        // 1. alert player of kill, and death
        // 2a. check for tk - if tk dont add kill - else add kill to score
        // 2b. check for double kill

        String killee = ba.getPlayerName(event.getKilleeID());
        String killer = ba.getPlayerName(event.getKillerID());
        if (killee == null || killer == null) return;
        players.get(killee.toLowerCase()).handleDeath(killer);
    }

    @Override
    public void handleEvent(PlayerPosition event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        /* Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null. */
        if (ptest == null) return;

        String name = ptest.getPlayerName();
        // grab player profile
        // check game status
        // check position -> appropriate action
        if (players.containsKey(name.toLowerCase()))
            players.get(name.toLowerCase()).handlePosition(event);
    }

    @Override
    public void handleEvent(FrequencyShipChange event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        if (ptest == null) return;
        // grab player profile
        // check game status
        // check ship and freq -> appropriate action
        String name = ba.getPlayerName(event.getPlayerID());
        if (players.containsKey(name.toLowerCase()))
            players.get(name.toLowerCase()).handleFSC(event);
    }

    private void handleInfo(String msg) {
        //Sorts information from *info
        String[] pieces = msg.split("  ");
        String name = pieces[3].substring(10);
        DuelPlayer p = getPlayer(name);
        if (p != null) {
            String ip = pieces[0].substring(3);
            String mid = pieces[5].substring(10);
            p.sql_createPlayer(ip, mid);
        }
    }

    /** Handles the !help command */
    private void cmd_help(String name) {
        String[] help = {
                "+-ABOUT-------------------------------------------------------------------------------------------------------+  ",
                "   2v2 scrimmage dueling (non-league) is currently being developed for TWEL. No signup necessary for now, so ",
                "   try it out and let us know what you think! Please message WingZero if there are any bugs/problems, thanks.",
                "   When complete you'll be able to play 2v2 league duels or 2v2 scrim duels (like how it is now but together).",
                "+-COMMANDS----------------------------------------------------------------------------------------------------+",
                "  !ch <player>:<division number>          - Challenges the freq with <player> to a duel in <division num>",
                "                                            * You must have exactly 2 players per freq",
                "                                            * Divisions: 1-Warbird, 2-Javelin, 3-Spider, 4-Lancaster, 5-Mixed",
                "  !a <player>                             - Accepts a challenge from <player>",
                "  !teams                                  - Lists current teams eligible for ranked league play", 
                "  !score <player>                         - Displays the score of <player>'s duel, if dueling",
                "  !signup                                 - Registers for TWEL 2v2 league play",
                "  !disable                                - Disables name to allow for the enabling of another name",
                "  !enable                                 - Enables name if already registered but disabled" 
                };

        ba.privateMessageSpam(name, help);
    }
    
    private void cmd_signup(String name) {
        DuelPlayer p = getPlayer(name);
        if (p == null) {
            Player info = ba.getPlayer(name);
            if (info == null) return;
            p = new DuelPlayer(info, this);
            players.put(name.toLowerCase(), p);
        }
        if (p != null)
            p.doSignup();
    }
    
    private void cmd_signup(String name, String cmd) {
        String pname = cmd.substring(cmd.indexOf(" ") + 1);
        DuelPlayer p = getPlayer(pname);
        if (p == null) {
            Player info = ba.getPlayer(name);
            if (info == null) {
                ba.sendSmartPrivateMessage(name, "Player '" + pname + "' must be in the arena to be registered.");
                return;
            }
            p = new DuelPlayer(info, this);
            players.put(name.toLowerCase(), p);
        }
        if (p != null)
            p.doSignup(name);
    }
    
    private void cmd_disable(String name) {
        DuelPlayer p = getPlayer(name);
        if (p == null) {
            Player info = ba.getPlayer(name);
            if (info == null) return;
            p = new DuelPlayer(info, this);
            players.put(name.toLowerCase(), p);
        }
        if (p != null)
            p.doDisable();
    }
    
    private void cmd_enable(String name) {
        DuelPlayer p = getPlayer(name);
        if (p == null) {
            Player info = ba.getPlayer(name);
            if (info == null) return;
            p = new DuelPlayer(info, this);
            players.put(name.toLowerCase(), p);
        }
        if (p != null)
            p.doEnable();
    }
    
    private void cmd_teams(String name) {
        TreeMap<Short, Player> freqs = new TreeMap<Short, Player>();
        Iterator<Player> i = ba.getPlayingPlayerIterator();
        while (i.hasNext()) {
            Player p = i.next();
            if (ba.getFrequencySize(p.getFrequency()) == 2) {
                DuelPlayer dp = getPlayer(p.getPlayerName());
                if (dp != null) {
                    if (dp.canPlay()) {
                        if (freqs.containsKey(p.getFrequency()))
                            ba.sendSmartPrivateMessage(name, "Team: '" + freqs.get(p.getFrequency()).getPlayerName() + "' and '" + p.getPlayerName() + "'");
                        else
                            freqs.put(p.getFrequency(), p);
                    }
                }
            }
        }
        if (freqs.isEmpty())
            ba.sendSmartPrivateMessage(name, "No eligable teams of two were found.");
    }

    /** Handles the !debug command which enables or disables debug mode */
    private void cmd_debug(String name) {
        if (!DEBUG) {
            debugger = name;
            DEBUG = true;
            ba.sendSmartPrivateMessage(name, "Debugging ENABLED. You are now set as the debugger.");
        } else if (debugger.equalsIgnoreCase(name)) {
            debugger = "";
            DEBUG = false;
            ba.sendSmartPrivateMessage(name, "Debugging DISABLED and debugger reset.");
        } else {
            ba.sendChatMessage(name + " has overriden " + debugger
                    + " as the target of debug messages.");
            ba.sendSmartPrivateMessage(name, "Debugging still ENABLED and you have replaced "
                    + debugger + " as the debugger.");
            debugger = name;
        }
    }

    /** Handles the !players debugging command */
    private void cmd_players() {
        for (String s : players.keySet())
            debug("" + s);
    }

    /** Handles the !games debugging command */
    private void cmd_games() {
        for (Integer s : games.keySet())
            debug("" + s);
    }

    /** Handles the !challs debugging command */
    private void cmd_challs() {
        for (String s : challs.keySet())
            debug("" + s);
    }

    /** Handles the !freqs debugging command */
    private void cmd_freqs() {
        for (Integer s : teams.keySet())
            debug("" + s);

        for (Integer s : freqs)
            debug("" + s);
    }

    /** Handles the !cancel command */
    private void cmd_cancel(String name, String msg) {
        if (msg.contains(" ") && msg.length() > 7) {
            msg = msg.substring(msg.indexOf(" ") + 1);
            if (players.containsKey(msg.toLowerCase()))
                players.get(msg.toLowerCase()).cancelGame(name);
            else {
                String p = ba.getFuzzyPlayerName(msg);
                if (p != null && players.containsKey(p.toLowerCase()))
                    players.get(p.toLowerCase()).cancelGame(name);
                else
                    ba.sendPrivateMessage(name, "Player not found");
            }
        } else
            ba.sendPrivateMessage(name, "Invalid syntax");
    }

    /** Handles the !ch command
     * 
     * @param name
     * @param args */
    private void cmd_challenge(String name, String[] args, boolean ranked) {
        if (args.length != 2) return;
        // TODO: remove when not debugging
        ranked = false;
        int div = -1;
        try {
            div = Integer.valueOf(args[1]);
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Invalid division number.");
            return;
        }

        if (div < 1 || div > 5) {
            ba.sendPrivateMessage(name, "Invalid division number (1><5).");
            return;
        } else if (div == 2 && !boxOpen(2)) {
            ba.sendPrivateMessage(name,
                    "All duel boxes for that division are in use. Please try again later.");
            return;
        } else if (!boxOpen(1)) {
            ba.sendPrivateMessage(name,
                    "All duel boxes for that division are in use. Please try again later.");
            return;
        }

        Player p = ba.getPlayer(name);
        if (ba.getFrequencySize(p.getFrequency()) != 2) {
            ba.sendPrivateMessage(name,
                    "Your freq size must be 2 players exactly to challenge for a 2v2 duel.");
            return;
        }
        Player o = ba.getFuzzyPlayer(args[0]);
        if (o == null || ba.getFrequencySize(o.getFrequency()) != 2) {
            ba.sendPrivateMessage(name,
                    "The enemy freq size must be 2 players exactly to challenge for a 2v2 duel.");
            return;
        }

        String[] names1 = { "", "" };
        String[] names2 = { "", "" };
        int freq1 = p.getFrequency();
        int freq2 = o.getFrequency();
        Iterator<Player> i = ba.getFreqPlayerIterator(freq1);
        Iterator<Player> j = ba.getFreqPlayerIterator(freq2);
        while (i.hasNext() && j.hasNext()) {
            Player p1 = i.next();
            Player p2 = j.next();

            if (!players.containsKey(p1.getPlayerName().toLowerCase())
                    || !players.containsKey(p2.getPlayerName().toLowerCase())) return;

            if (names1[0].length() > 0)
                names1[1] = p1.getPlayerName();
            else
                names1[0] = p1.getPlayerName();

            if (names2[0].length() > 0)
                names2[1] = p2.getPlayerName();
            else
                names2[0] = p2.getPlayerName();
        }
        
        DuelPlayer[] tests = new DuelPlayer[] {
                players.get(names1[0].toLowerCase()),
                players.get(names1[1].toLowerCase()),
                players.get(names2[0].toLowerCase()),
                players.get(names2[1].toLowerCase())
        };
        
        if (ranked) {
            boolean clear = true;
            for (DuelPlayer dp : tests) {
                if (!dp.canPlay()) {
                    clear = false;
                    ba.sendOpposingTeamMessageByFrequency(freq1, "[ERROR] Player not registered for ranked play: " + dp.getName());
                }
            }
            if (!clear)
                return;
        }
        
        tests[0].setDuel(names1[1], freq1);
        tests[1].setDuel(names1[0], freq1);
        tests[2].setDuel(names2[1], freq2);
        tests[3].setDuel(names2[0], freq2);

        final String key = "" + freq1 + " " + freq2 + "";
        if (challs.containsKey(key)) {
            DuelChallenge ch = challs.get(key);
            if (ch.getDiv() != div) {
                ba.cancelTask(ch);
                challs.remove(key);
            } else {
                ba.sendPrivateMessage(name,
                        "This challenge already exists, but you may try it again after it expires.");
                return;
            }
        }

        DuelChallenge chall = new DuelChallenge(this, ba, ranked, freq1, freq2, names1, names2, div);
        challs.put(key, chall);
        ba.scheduleTask(chall, 60000);
        ba.sendOpposingTeamMessageByFrequency(freq1, (ranked ? "[RANKED] " : "[SCRIM] ")
                + "You have challenged " + names2[0] + " and " + names2[1] + " to a "
                + (ranked ? "RANKED " : "CASUAL ") + getDivision(div)
                + " duel. This challenge will expire in 1 minute.", 26);
        ba.sendOpposingTeamMessageByFrequency(freq2, (ranked ? "[RANKED] " : "[SCRIM] ")
                + "You are being challenged to a " + (ranked ? "RANKED " : "CASUAL ")
                + getDivision(div) + " duel by " + names1[0] + " and " + names1[1]
                + ". Use !a <name> (<name> is one of your opponenents) to accept.", 26);
    }

    /** Handles the !a accept challenge command */
    private void cmd_accept(String name, String[] args) {
        if (args.length != 1) return;

        Player nme = ba.getFuzzyPlayer(args[0]);
        if (nme == null) {
            ba.sendPrivateMessage(name, "Player not found.");
            return;
        }

        Player p = ba.getPlayer(name);
        String key = "" + nme.getFrequency() + " " + p.getFrequency() + "";
        if (!challs.containsKey(key)) {
            ba.sendPrivateMessage(name, "Challenge not found.");
            return;
        }

        DuelChallenge chall = challs.remove(key);
        ba.cancelTask(chall);
        if (chall.getDiv() == 2 && boxOpen(2)) {
            DuelGame game = new DuelGame(getDuelBox(2), chall, ba, this);
            game.startGame();
            removeChalls(nme.getFrequency(), p.getFrequency());
        } else if (chall.getDiv() != 2 && boxOpen(1)) {
            DuelGame game = new DuelGame(getDuelBox(1), chall, ba, this);
            game.startGame();
            removeChalls(nme.getFrequency(), p.getFrequency());
        } else {
            ba.sendOpposingTeamMessageByFrequency(
                    chall.freq1(),
                    "No duel boxes are currently available for this division. Please try again later.",
                    26);
            ba.sendOpposingTeamMessageByFrequency(
                    chall.freq2(),
                    "No duel boxes are currently available for this division. Please try again later.",
                    26);
        }
    }

    /** Handles the !score command */
    private void cmd_score(String name, String msg) {
        String p = msg.substring(msg.indexOf(" ") + 1);
        if (p == null || p.length() < 1) {
            ba.sendPrivateMessage(name, "Invalid player name entered");
            return;
        }
        if (playing.containsKey(p.toLowerCase()))
            ba.sendPrivateMessage(name, games.get(playing.get(p.toLowerCase())).getScore());
        else {
            p = ba.getFuzzyPlayerName(p);
            if (p != null && playing.containsKey(p.toLowerCase()))
                ba.sendPrivateMessage(name, games.get(playing.get(p.toLowerCase())).getScore());
            else
                ba.sendPrivateMessage(name, "Player or duel not found");
        }
    }

    /** Handles the !lagout command */
    private void cmd_lagout(String name) {
        if (laggers.containsKey(name.toLowerCase()))
            laggers.get(name.toLowerCase()).doLagout();
        else
            ba.sendPrivateMessage(name, "You are not lagged out.");
    }
    
    public DuelPlayer getPlayer(String name) {
        return players.get(name.toLowerCase());
    }

    /** Removes all challenges involving two specific freqs
     * 
     * @param freq1
     * @param freq2 */
    public void removeChalls(int freq1, int freq2) {
        Vector<String> keys = new Vector<String>();
        for (String k : challs.keySet())
            if (k.contains("" + freq1) || k.contains("" + freq2)) keys.add(k);

        while (!keys.isEmpty()) {
            String k = keys.remove(0);
            ba.cancelTask(challs.remove(k));
        }
    }

    /** Removes all challenges involving a specific freq
     * 
     * @param freq */
    public void removeChalls(int freq) {
        Vector<String> keys = new Vector<String>();
        for (String k : challs.keySet())
            if (k.contains("" + freq)) keys.add(k);

        while (!keys.isEmpty()) {
            String k = keys.remove(0);
            ba.cancelTask(challs.remove(k));
        }
    }

    /** Returns the division name for a given id
     * 
     * @param div
     *            division id number
     * @return division String name */
    public String getDivision(int div) {
        if (div == 1)
            return "Warbird";
        else if (div == 2)
            return "Javelin";
        else if (div == 3)
            return "Spider";
        else if (div == 4 || div == 7)
            return "Lancaster";
        else if (div == 5)
            return "Mixed";
        else
            return "Unknown";
    }

    /** @return current non-league game id */
    public int getScrimID() {
        scrimID--;
        return scrimID;
    }

    /** @return current league game id */
    public int getGameID() {
        gameID++;
        return gameID;
    }

    /** @return current game id */
    public int getID(boolean ranked) {
        if (ranked) {
            gameID++;
            return gameID;
        } else {
            scrimID--;
            return scrimID;
        }
    }

    /** Checks to see if a DuelBox is open for a given division
     * 
     * @param division
     *            the division id number
     * @return true if an open box exists 
     */
    private boolean boxOpen(int division) {
        int i = 0;
        Iterator<String> it = boxes.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            DuelBox b = boxes.get(key);
            if (!b.inUse() && b.gameType(division)) i++;
        }
        if (i == 0)
            return false;
        else
            return true;
    }

    /** Returns an open DuelBox
     * 
     * @param division
     *            the division id number
     * @return an open DuelBox for the given division */
    private DuelBox getDuelBox(int division) {
        Vector<DuelBox> v = new Vector<DuelBox>();
        Iterator<String> it = boxes.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            DuelBox b = boxes.get(key);
            if (!b.inUse() && b.gameType(division)) v.add(b);
        }
        if (v.size() == 0)
            return null;
        else {
            Random generator = new Random();
            return v.elementAt(generator.nextInt(v.size()));
        }
    }

    /** Splits the arguments of a given command separated by colons
     * 
     * @param cmd
     *            command String
     * @return String array of the results args */
    private String[] splitArgs(String cmd) {
        String[] result = null;
        if (cmd.contains(" ")) if (!cmd.contains(":")) {
            result = new String[1];
            result[0] = cmd.substring(cmd.indexOf(" ") + 1);
        } else
            result = cmd.substring(cmd.indexOf(" ") + 1).split(":");
        return result;
    }

    /** Debug message handler */
    public void debug(String msg) {
        if (DEBUG) ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }

}
