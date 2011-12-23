package twcore.bots.duel2bot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
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
    
    // current non-league id number
    private int nlid;
    
    private String greet = "Welcome! To play, get in a ship and on a freq with a friend. Then, challenge another freq that also has 2 players. Use !help";
    private String debugger = "";
    private boolean DEBUG;
    // non-league teams will be identified by negative numbers :: i may not use this at all and just go by player names
    HashMap<Integer, DuelGame> games;
    // list of players who are playing tied to their duel id
    HashMap<String, Integer> playing;
    HashMap<String, DuelBox> boxes;
    // list of players and associated profile
    HashMap<String, DuelPlayer> players;
    HashMap<Integer, DuelTeam> teams;
    // list of players currently lagged out
    HashMap<String, DuelPlayer> laggers;
    // list of scrimmage challenges
    HashMap<String, DuelChallenge> challs;
    // list of used frequencies
    Vector<Integer> freqs;
    
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
        
        oplist = ba.getOperatorList();
        boxes = new HashMap<String, DuelBox>();
        games = new HashMap<Integer, DuelGame>();
        teams = new HashMap<Integer, DuelTeam>();
        players = new HashMap<String, DuelPlayer>();
        playing = new HashMap<String, Integer>();
        laggers = new HashMap<String, DuelPlayer>();
        challs = new HashMap<String, DuelChallenge>();
        freqs = new Vector<Integer>();
        DEBUG = false;
    }
    
    @Override
    public void handleEvent(LoggedOn event) {

        settings = ba.getBotSettings();
        
        ba.joinArena(settings.getString("Arena"));
        
        // Create new box Objects
        int boxCount = settings.getInt("BoxCount");
        for (int i = 1; i <= boxCount; i++) {
            String boxset[] = settings.getString("Box" + i).split(",");
            String warps[] = settings.getString("Warp" + i).split(",");
            String area[] = settings.getString("Area" + i).split(",");
            if (boxset.length == 17)
                boxes.put("" + i, new DuelBox(boxset, warps, area, i));
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
        
        nlid = 0;
    }
    
    @Override
    public void handleEvent(ArenaJoined event) {
        // handle player list
        String arena = ba.getArenaName();
        if (!arena.equalsIgnoreCase("duel2"))
            return;
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
        String name = ba.getPlayerName(event.getPlayerID());
        String msg = event.getMessage();
        int type = event.getMessageType();

        if (!oplist.isBotExact(name) && (type == Message.PRIVATE_MESSAGE || type == Message.PUBLIC_MESSAGE)) if (msg.startsWith("!ch ") || msg.startsWith("!challenge "))
            player_challenge(name, splitArgs(msg));
        else if (msg.startsWith("!a ") || msg.startsWith("!accept "))
            player_acceptScrim(name, splitArgs(msg));
        else if (msg.startsWith("!lagout"))
            player_lagout(name);
        else if (msg.startsWith("!help") || (msg.startsWith("!h")))
            player_help(name);
        else if (msg.startsWith("!score"))
            player_score(name, msg);
        
        if (!oplist.isModerator(name) || type != Message.PRIVATE_MESSAGE) return;
        if (msg.startsWith("!die"))
            ba.die();
        else if (msg.startsWith("!debug"))
            cmd_debug(name);
        else if (msg.startsWith("!cancel"))
            command_cancel(name, msg);
        else if (msg.startsWith("!players"))
            cmd_players();
        else if (msg.startsWith("!games"))
            cmd_games();
        else if (msg.startsWith("!freqs"))
            cmd_freqs();
        else if (msg.startsWith("!challs")) 
            cmd_challs();
    }
    
    @Override
    public void handleEvent(PlayerEntered event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        /*
         * Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null.
         */
        if (ptest == null)
            return;
        String name = ptest.getPlayerName();
        
        // greet
        // add to player list
        // retrieve/create player profile
        // refresh teams list
        if (laggers.containsKey(name.toLowerCase())) {
            players.put(name.toLowerCase(), laggers.get(name.toLowerCase()));
            players.get(name.toLowerCase()).handleReturn();
            
        } else {
            players.put(name.toLowerCase(), new DuelPlayer(name, this));
            ba.sendPrivateMessage(name, greet);            
        }
    }
    
    @Override
    public void handleEvent(PlayerLeft event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        if (ptest == null)
            return;
        String name = ptest.getPlayerName();
        // remove from player lists
        // refresh teams list
        if (players.containsKey(name.toLowerCase())) if (!laggers.containsKey(name.toLowerCase()))
            players.remove(name.toLowerCase()).handleLagout();
        
    }
    
    @Override
    public void handleEvent(FrequencyChange event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        /*
         * Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null.
         */
        if (ptest == null)
            return;

        String name = ptest.getPlayerName();
        if (players.containsKey(name.toLowerCase())) 
            players.get(name.toLowerCase()).handleFreq(event);
    }
    
    @Override
    public void handleEvent(PlayerDeath event) {
        Player ptest = ba.getPlayer(event.getKilleeID());
        Player ptest2 = ba.getPlayer(event.getKillerID());
        /*
         * Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null.
         */
        if (ptest == null || ptest2 == null)
            return;
        
        // 1. alert player of kill, and death
        // 2a. check for tk - if tk dont add kill - else add kill to score
        // 2b. check for double kill
        
        String killee = ba.getPlayerName(event.getKilleeID());
        String killer = ba.getPlayerName(event.getKillerID());
        if (killee == null || killer == null)
            return;
        players.get(killee.toLowerCase()).handleDeath(killer);
    }
    
    @Override
    public void handleEvent(PlayerPosition event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        /*
         * Sometimes a player leaves the arena just after a position packet is
         * received; while the position event is distributed, the PlayerLeft
         * event is given to Arena, causing all information about the player to
         * be wiped from record. This also occurs frequently with the
         * PlayerDeath event. Moral: check for null.
         */
        if (ptest == null)
            return;

        String name = ptest.getPlayerName();
        // grab player profile
        // check game status
        // check position -> appropriate action
        if (players.containsKey(name.toLowerCase())) players.get(name.toLowerCase()).handlePosition(event);
    }
    
    @Override
    public void handleEvent(FrequencyShipChange event) {
        Player ptest = ba.getPlayer(event.getPlayerID());
        if (ptest == null)
            return;
        // grab player profile
        // check game status
        // check ship and freq -> appropriate action
        String name = ba.getPlayerName(event.getPlayerID());
        if (players.containsKey(name.toLowerCase()))
            players.get(name.toLowerCase()).handleFSC(event);
    }
    
    /** Handles the !debug command which enables or disables debug mode */
    private void cmd_debug(String name) {
        if (!DEBUG) {
            debugger = name;
            DEBUG = true;
            ba.sendSmartPrivateMessage(name, "Debugging ENABLED. You are now set as the debugger.");
        } else if (debugger.equalsIgnoreCase(name)){
            debugger = "";
            DEBUG = false;
            ba.sendSmartPrivateMessage(name, "Debugging DISABLED and debugger reset.");
        } else {
            ba.sendChatMessage(name + " has overriden " + debugger + " as the target of debug messages.");
            ba.sendSmartPrivateMessage(name, "Debugging still ENABLED and you have replaced " + debugger + " as the debugger.");
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
    
    public void command_cancel(String name, String msg) {
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
    
    /**
     * Handles the !help command
     * 
     * @param name
     */
    public void player_help(String name) {
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
                "  !score <player>                         - Displays the score of <player>'s duel, if dueling",
                "   ... more to come                             "
        };
        
        ba.privateMessageSpam(name, help);
    }

    /**
     * Handles the !ch command
     * 
     * @param name
     * @param args
     */
    public void player_challenge(String name, String[] args) {
        if (args.length != 2)
            return;

        int type = -1;
        try {
            type = Integer.valueOf(args[1]);
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Invalid division number.");
            return;
        }

        if (type < 1 || type > 5) {
            ba.sendPrivateMessage(name, "Invalid division number (1><5).");
            return;                
        } else if (type == 2 && !boxOpen(2)) {
            ba.sendPrivateMessage(name, "All duel boxes for that division are in use. Please try again later.");
            return;
        } else if (!boxOpen(1)) {
            ba.sendPrivateMessage(name, "All duel boxes for that division are in use. Please try again later.");
            return;                
        }

        Player p = ba.getPlayer(name);
        if (ba.getFrequencySize(p.getFrequency()) != 2) {
            ba.sendPrivateMessage(name, "Your freq size must be 2 exactly players to challenge for a 2v2 duel.");
            return;
        }
        Player o = ba.getFuzzyPlayer(args[0]);
        if (o == null || ba.getFrequencySize(o.getFrequency()) != 2) {
            ba.sendPrivateMessage(name, "The enemy freq size must be 2 exactly players to challenge for a 2v2 duel.");
            return;
        }
        
        String[] n1 = { "", "" };
        String[] n2 = { "", "" };
        int freq1 = p.getFrequency();
        int freq2 = o.getFrequency();
        Iterator<Player> i = ba.getFreqPlayerIterator(freq1);
        Iterator<Player> j = ba.getFreqPlayerIterator(freq2);
        while (i.hasNext() && j.hasNext()) {
            Player p1 = i.next();
            Player p2 = j.next();

            if (!players.containsKey(p1.getPlayerName().toLowerCase()) || !players.containsKey(p2.getPlayerName().toLowerCase()))
                return;

            if (n1[0].length() > 0)
                n1[1] = p1.getPlayerName();
            else
                n1[0] = p1.getPlayerName();

            if (n2[0].length() > 0)
                n2[1] = p2.getPlayerName();
            else
                n2[0] = p2.getPlayerName();
        }

        players.get(n1[0].toLowerCase()).setScrim(n1[1], freq1);
        players.get(n1[1].toLowerCase()).setScrim(n1[0], freq1);
        players.get(n2[0].toLowerCase()).setScrim(n2[1], freq2);
        players.get(n2[1].toLowerCase()).setScrim(n2[0], freq2);
        final String key = "" + freq1 + " " + freq2 + "";
        if (challs.containsKey(key)) {
            DuelChallenge ch = challs.get(key);
            if (ch.getDiv() != type) {
                ba.cancelTask(ch);
                challs.remove(key);
            } else {
                ba.sendPrivateMessage(name, "This challenge already exists, but you may try it again after it expires.");
                return;
            }
        }
        DuelChallenge chall = new DuelChallenge(this, ba, freq1, freq2, n1, n2, type);
        challs.put(key, chall);
        ba.scheduleTask(chall, 60000);
        ba.sendOpposingTeamMessageByFrequency(freq1, "You have challenged " + n2[0] + " and " + n2[1] + " to a " + getDivision(type) + " duel. This challenge will expire in 1 minute.", 26);
        ba.sendOpposingTeamMessageByFrequency(freq2, "You are being challenged to a " + getDivision(type) + " duel by " + n1[0] + " and " + n1[1] + ". Use !a <name> (<name> is one of your opponenents) to accept.", 26);

    }

    /**
     * Handles the !a accept challenge command
     * 
     * @param name
     * @param args
     */
    public void player_acceptScrim(String name, String[] args) {
        if (args.length != 1)
            return;

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
        
        DuelChallenge scrim = challs.remove(key);
        ba.cancelTask(scrim);
        if (scrim.getDiv() == 2 && boxOpen(2)) {
            DuelGame game = new DuelGame(getDuelBox(2), scrim, ba, this);
            game.startGame();
            removeScrimChalls(nme.getFrequency(), p.getFrequency());
        } else if (scrim.getDiv() != 2 && boxOpen(1)) {
            DuelGame game = new DuelGame(getDuelBox(1), scrim, ba, this);      
            game.startGame();      
            removeScrimChalls(nme.getFrequency(), p.getFrequency());
        } else {
            ba.sendOpposingTeamMessageByFrequency(scrim.freq1(), "No duel boxes are currently available for this division. Please try again later.", 26);
            ba.sendOpposingTeamMessageByFrequency(scrim.freq2(), "No duel boxes are currently available for this division. Please try again later.", 26);
        }
    }
    
    /**
     * Handles the !score command
     * 
     * @param name
     * @param msg
     */
    public void player_score(String name, String msg) {
        String p = msg.substring(msg.indexOf(" ") + 1);
        if (p == null || p.length() < 1) {
            ba.sendPrivateMessage(name, "Invalid player name entered");
            return;
        }
        if (playing.containsKey(p.toLowerCase()))
            ba.sendPrivateMessage(name, games.get(playing.get(p.toLowerCase())).getScore());
        else {
            p = ba.getFuzzyPlayerName(p);
            if (p != null && playing.containsKey(p.toLowerCase())) ba.sendPrivateMessage(name, games.get(playing.get(p.toLowerCase())).getScore());
            else
                ba.sendPrivateMessage(name, "Player or duel not found");
        }
    }
    
    /**
     * Handles the !lagout command
     * 
     * @param name
     */
    public void player_lagout(String name) {
        if (laggers.containsKey(name.toLowerCase()))
            laggers.get(name.toLowerCase()).doLagout();
        else
            ba.sendPrivateMessage(name, "You are not lagged out.");
    }
    
    /**
     * Removes all challenges involving two specific freqs
     * 
     * @param freq1
     * @param freq2
     */
    public void removeScrimChalls(int freq1, int freq2) {
        Vector<String> keys = new Vector<String>();
        for (String k : challs.keySet())
            if (k.contains("" + freq1) || k.contains("" + freq2)) keys.add(k);
        
        while (!keys.isEmpty()) {
            String k = keys.remove(0);
            ba.cancelTask(challs.remove(k));
        }
    }
    
    /**
     * Removes all challenges involving a specific freq
     * 
     * @param freq
     */
    public void removeScrimChalls(int freq) {
        Vector<String> keys = new Vector<String>();
        for (String k : challs.keySet())
            if (k.contains("" + freq)) 
                keys.add(k);
        
        while (!keys.isEmpty()) {
            String k = keys.remove(0);
            ba.cancelTask(challs.remove(k));
        }
    }
    
    /**
     * Returns the division name for a given id
     * 
     * @param div
     *      division id number
     * @return
     *      division String name
     */
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
    
    /**
     * @return
     *      current non-league game id
     */
    public int getScrimID() {
        nlid--;
        return nlid;
    }

    /**
     * Checks to see if a DuelBox is open for a given division
     * 
     * @param division
     *      the division id number
     * @return
     *      true if an open box exists
     */
    private boolean boxOpen(int division) {
        int i = 0;
        Iterator<String> it = boxes.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            DuelBox b = boxes.get(key);
            if (!b.inUse() && b.gameType(division))
                i++;
        }
        if (i == 0)
            return false;
        else
            return true;
    }

    /**
     * Returns an open DuelBox
     * 
     * @param division
     *      the division id number
     * @return
     *      an open DuelBox for the given division      
     */
    private DuelBox getDuelBox(int division) {
        Vector<DuelBox> v = new Vector<DuelBox>();
        Iterator<String> it = boxes.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            DuelBox b = boxes.get(key);
            if (!b.inUse() && b.gameType(division))
                v.add(b);
        }
        if (v.size() == 0)
            return null;
        else {
            Random generator = new Random();
            return v.elementAt(generator.nextInt(v.size()));
        }
    }
    
    /**
     * Splits the arguments of a given command separated by colons
     * 
     * @param cmd
     *          command String
     * @return
     *      String array of the results args
     */
    private String[] splitArgs(String cmd) {
        String[] result = null;
        if (cmd.contains(" ")) if (!cmd.contains(":")) {
            result = new String[1];
            result[0] =  cmd.substring(cmd.indexOf(" ") + 1);
        } else 
            result = cmd.substring(cmd.indexOf(" ") + 1).split(":");
        return result;
    }
    
    /** Debug message handler */
    public void debug(String msg) {
        if (DEBUG)
            ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }

}
