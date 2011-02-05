package twcore.bots.duel2bot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;
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

    static final String DB = "website";
    final int MSG_LIMIT = 8;
    BotSettings m_botSettings;
    OperatorList m_opList;
    BotAction m_botAction;
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
    
    // non-league teams will be identified by negative numbers :: i may not use this at all and just go by player names
    HashMap<Integer, DuelGame> m_games;
    // list of players who are playing tied to their duel
    HashMap<String, DuelGame> m_playing;
    HashMap<String, DuelBox> m_boxes;
    // list of players and associated profile
    HashMap<String, DuelPlayer> m_players;
    HashMap<Integer, DuelTeam> m_teams;
    // list of players currently lagged out
    HashMap<String, DuelPlayer> m_laggers;
    // list of scrimmage challenges
    HashMap<String, Scrim> m_scrims;
    // list of used frequencies
    Vector<Integer> m_freqs;
    
    
    public duel2bot(BotAction botAction) {
        super(botAction);
        
        m_botAction = botAction;
        
        EventRequester events = m_botAction.getEventRequester();
        events.request(EventRequester.MESSAGE);
        events.request(EventRequester.LOGGED_ON);
        events.request(EventRequester.ARENA_JOINED);
        events.request(EventRequester.PLAYER_DEATH);
        events.request(EventRequester.PLAYER_POSITION);
        events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(EventRequester.FREQUENCY_CHANGE);
        events.request(EventRequester.PLAYER_LEFT);
        events.request(EventRequester.PLAYER_ENTERED);
        
        m_opList = m_botAction.getOperatorList();
        m_boxes = new HashMap<String, DuelBox>();
        m_games = new HashMap<Integer, DuelGame>();
        m_teams = new HashMap<Integer, DuelTeam>();
        m_players = new HashMap<String, DuelPlayer>();
        m_playing = new HashMap<String, DuelGame>();
        m_laggers = new HashMap<String, DuelPlayer>();
        m_scrims = new HashMap<String, Scrim>();
        m_freqs = new Vector<Integer>();

    }
    
    public void handleEvent(LoggedOn event) {

        m_botSettings = m_botAction.getBotSettings();
        
        m_botAction.joinArena(m_botSettings.getString("Arena"));
        
        // Create new box Objects
        int boxCount = m_botSettings.getInt("BoxCount");
        for (int i = 1; i <= boxCount; i++) {
            String boxset[] = m_botSettings.getString("Box" + i).split(",");
            String warps[] = m_botSettings.getString("Warp" + i).split(",");
            String area[] = m_botSettings.getString("Area" + i).split(",");
            if (boxset.length == 17)
                m_boxes.put("" + i, new DuelBox(boxset, warps, area, i));
        }
        
        // Reads in general settings for dueling
        d_season = m_botSettings.getInt("Season");
        d_spawnTime = m_botSettings.getInt("SpawnAfter");
        d_spawnLimit = m_botSettings.getInt("SpawnLimit");
        d_deathTime = m_botSettings.getInt("SpawnTime");
        d_noCount = m_botSettings.getInt("NoCount");
        d_maxLagouts = m_botSettings.getInt("LagLimit");
        d_challengeTime = m_botSettings.getInt("ChallengeTime");
        d_duelLimit = m_botSettings.getInt("DuelLimit");
        d_duelDays = m_botSettings.getInt("DuelDays");
        m_botAction.setReliableKills(1);
        
        nlid = 0;
    }
    
    public void handleEvent(ArenaJoined event) {
        // handle player list
        String arena = m_botAction.getArenaName();
        if (!arena.equalsIgnoreCase("duel2"))
            return;
        Iterator<Player> i = m_botAction.getPlayerIterator();
        while (i.hasNext()) {
            Player p = i.next();
            m_players.put(p.getPlayerName().toLowerCase(), new DuelPlayer(p, this));
        }
    }
    
    public void handleEvent(Message event) {
        String name = m_botAction.getPlayerName(event.getPlayerID());
        String msg = event.getMessage();
        int type = event.getMessageType();

        if (!m_opList.isBotExact(name) && (type == Message.PRIVATE_MESSAGE || type == Message.PUBLIC_MESSAGE)) {
            if (msg.startsWith("!ch "))
                player_challenge(name, command(msg));
            else if (msg.startsWith("!a "))
                player_acceptScrim(name, command(msg));
            else if (msg.startsWith("!lagout"))
                player_lagout(name);
        }
        
        if (type == Message.PRIVATE_MESSAGE && m_opList.isModerator(name)) {
            if (msg.startsWith("!die"))
                m_botAction.die();
            else if (msg.startsWith("!cancel")) {
                command_cancel(name, msg);
            }
        }
    }
    
    public void handleEvent(PlayerEntered event) {
        Player ptest = m_botAction.getPlayer(event.getPlayerID());
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
        if (m_laggers.containsKey(name.toLowerCase())) {
            m_players.put(name.toLowerCase(), m_laggers.get(name.toLowerCase()));
            m_players.get(name.toLowerCase()).handleReturn();
            
        } else {
            m_players.put(name.toLowerCase(), new DuelPlayer(name, this));
            m_botAction.sendPrivateMessage(name, greet);            
        }
    }
    
    public void handleEvent(PlayerLeft event) {
        Player ptest = m_botAction.getPlayer(event.getPlayerID());
        if (ptest == null)
            return;
        String name = ptest.getPlayerName();
        // remove from player lists
        // refresh teams list
        if (m_players.containsKey(name.toLowerCase()))  {
            if (!m_laggers.containsKey(name.toLowerCase()))
                m_players.remove(name.toLowerCase()).lagout();
        }
        
    }
    
    public void player_help(String name) {
        String[] help = { 
                "!ch <player name>:<division number>             - Challenges the freq with that <player name> to a duel",
                "                                                  You may only have 2 players per freq or it won't challenge",
                "                                                  Division #s: 1-Warbird, 2-Javelin, 3-Spider, 4-Lancaster, 5-Mixed",
                "!a <player name>                                - Accepts a challenge from <player name>",
                "... more to come                               ",
                "" 
        };
        
        m_botAction.privateMessageSpam(name, help);
    }
    
    public void handleEvent(FrequencyChange event) {
        Player ptest = m_botAction.getPlayer(event.getPlayerID());
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
        if (m_players.containsKey(name.toLowerCase())) 
            m_players.get(name.toLowerCase()).handleFreq(event);
    }
    
    public void handleEvent(PlayerDeath event) {
        Player ptest = m_botAction.getPlayer(event.getKilleeID());
        Player ptest2 = m_botAction.getPlayer(event.getKillerID());
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
        
        String killee = m_botAction.getPlayerName(event.getKilleeID());
        String killer = m_botAction.getPlayerName(event.getKillerID());
        if (killee == null || killer == null)
            return;
        m_players.get(killee.toLowerCase()).handleDeath(killer);
    }
    
    public void handleEvent(PlayerPosition event) {
        Player ptest = m_botAction.getPlayer(event.getPlayerID());
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
        if (m_players.containsKey(name.toLowerCase())) {
            m_players.get(name.toLowerCase()).handlePosition(event);
        }
    }
    
    public void handleEvent(FrequencyShipChange event) {
        Player ptest = m_botAction.getPlayer(event.getPlayerID());
        if (ptest == null)
            return;
        // grab player profile
        // check game status
        // check ship and freq -> appropriate action
        String name = m_botAction.getPlayerName(event.getPlayerID());
        if (m_players.containsKey(name.toLowerCase()))
            m_players.get(name.toLowerCase()).handleFSC(event);
    }
    
    public void command_cancel(String name, String msg) {
        if (msg.contains(" ") && msg.length() > 7) {
            msg = msg.substring(msg.indexOf(" ") + 1);
            if (m_players.containsKey(msg.toLowerCase()))
                m_players.get(msg.toLowerCase()).cancelGame(name);
            else {
                String p = m_botAction.getFuzzyPlayerName(msg);
                if (p != null && m_players.containsKey(p.toLowerCase()))
                    m_players.get(p.toLowerCase()).cancelGame(name);
                else
                    m_botAction.sendPrivateMessage(name, "Player not found");
            }
        } else
            m_botAction.sendPrivateMessage(name, "Invalid syntax");
    }

    public void player_challenge(String name, String[] args) {
        if (args.length != 2)
            return;

        int type = -1;
        try {
            type = Integer.valueOf(args[1]);
        } catch (NumberFormatException e) {
            m_botAction.sendPrivateMessage(name, "Invalid division number.");
            return;
        }

        if (type < 1 || type > 5) {
            m_botAction.sendPrivateMessage(name, "Invalid division number (1><5).");
            return;                
        } else if (type == 2 && !boxOpen(2)) {
            m_botAction.sendPrivateMessage(name, "All duel boxes for that division are in use. Please try again later.");
            return;
        } else if (!boxOpen(1)) {
            m_botAction.sendPrivateMessage(name, "All duel boxes for that division are in use. Please try again later.");
            return;                
        }

        Player p = m_botAction.getPlayer(name);
        if (m_botAction.getFrequencySize(p.getFrequency()) != 2) {
            m_botAction.sendPrivateMessage(name, "Your freq size must be 2 exactly players to challenge for a 2v2 duel.");
            return;
        }
        Player o = m_botAction.getFuzzyPlayer(args[0]);
        if (o == null || m_botAction.getFrequencySize(o.getFrequency()) != 2) {
            m_botAction.sendPrivateMessage(name, "The enemy freq size must be 2 exactly players to challenge for a 2v2 duel.");
            return;
        }
        
        String[] n1 = { "", "" };
        String[] n2 = { "", "" };
        int freq1 = p.getFrequency();
        int freq2 = o.getFrequency();
        Iterator<Player> i = m_botAction.getFreqPlayerIterator(freq1);
        Iterator<Player> j = m_botAction.getFreqPlayerIterator(freq2);
        while (i.hasNext() && j.hasNext()) {
            Player p1 = i.next();
            Player p2 = j.next();

            if (!m_players.containsKey(p1.getPlayerName().toLowerCase()) || !m_players.containsKey(p2.getPlayerName().toLowerCase()))
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

        m_players.get(n1[0].toLowerCase()).scrim(n1[1], freq1);
        m_players.get(n1[1].toLowerCase()).scrim(n1[0], freq1);
        m_players.get(n2[0].toLowerCase()).scrim(n2[1], freq2);
        m_players.get(n2[1].toLowerCase()).scrim(n2[0], freq2);
        final String key = "" + freq1 + " " + freq2 + "";
        if (m_scrims.containsKey(key)) {
            m_botAction.sendPrivateMessage(name, "This challenge already exists, so try again once it expires.");
            return;
        }
        m_scrims.put(key, new Scrim(freq1, freq2, n1, n2, type));
        TimerTask scrim = new TimerTask() {
            @Override
            public void run() {
                m_scrims.remove(key);
            }
        };
        m_botAction.scheduleTask(scrim, 60000);
        m_botAction.sendOpposingTeamMessageByFrequency(freq1, "You have challenged " + n2[0] + " and " + n2[1] + " to a " + getDivision(type) + " duel. The challenge will expire in 1 minute.");
        m_botAction.sendOpposingTeamMessageByFrequency(freq2, "You are being challenged to a " + getDivision(type) + " duel by " + n1[0] + " and " + n1[1] + ". Use !a and one of their names to accept.");

    }

    public void player_acceptScrim(String name, String[] args) {
        if (args.length != 1)
            return;

        Player nme = m_botAction.getFuzzyPlayer(args[0]);
        if (nme == null) {
            m_botAction.sendPrivateMessage(name, "Player not found.");
            return;
        }

        Player p = m_botAction.getPlayer(name);
        String key = "" + nme.getFrequency() + " " + p.getFrequency() + "";
        if (!m_scrims.containsKey(key)) {
            m_botAction.sendPrivateMessage(name, "Challenge not found.");
            return;
        }
        
        Scrim scrim = m_scrims.remove(key);
        if (scrim.type() == 2 && boxOpen(2)) {
            DuelGame game = new DuelGame(getDuelBox(2), scrim, m_botAction, this);
            game.startGame();
        } else if (scrim.type() != 2 && boxOpen(1)) {
            DuelGame game = new DuelGame(getDuelBox(1), scrim, m_botAction, this);      
            game.startGame();      
        } else {
            m_botAction.sendOpposingTeamMessageByFrequency(scrim.freq1(), "No duel boxes are currently available for this division. Please try again later.");
            m_botAction.sendOpposingTeamMessageByFrequency(scrim.freq2(), "No duel boxes are currently available for this division. Please try again later.");
        }
    }
    
    public void player_lagout(String name) {
        if (m_laggers.containsKey(name.toLowerCase()))
            m_laggers.get(name.toLowerCase()).handleLagout();
        else
            m_botAction.sendPrivateMessage(name, "You are not lagged out.");
    }
    
    private String[] command(String s) {
        String[] result = null;
        if (s.contains(" ")) {
            if (!s.contains(":")) {
                result = new String[1];
                result[0] =  s.substring(s.indexOf(" ") + 1);
            } else 
                result = s.substring(s.indexOf(" ") + 1).split(":");
        }
        return result;
    }
    
    public String getDivision(int type) {
        if (type == 1)
            return "Warbird";
        else if (type == 2)
            return "Javelin";
        else if (type == 3)
            return "Spider";
        else if (type == 4 || type == 7)
            return "Lancaster";
        else if (type == 5)
            return "Mixed";
        else
            return "Unknown";
    }

    private boolean boxOpen(int division) {
        int i = 0;
        Iterator<String> it = m_boxes.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            DuelBox b = m_boxes.get(key);
            if (!b.inUse() && b.gameType(division))
                i++;
        }
        if (i == 0)
            return false;
        else
            return true;
    }

    private DuelBox getDuelBox(int division) {
        Vector<DuelBox> v = new Vector<DuelBox>();
        Iterator<String> it = m_boxes.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            DuelBox b = m_boxes.get(key);
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
    
    public int nlid() {
        nlid--;
        return nlid;
    }

}

class Scrim {
    int freq1, freq2, type;
    String[] team1, team2;
    
    public Scrim(int f1, int f2, String[] n1, String[] n2, int div) {
        freq1 = f1;
        freq2 = f2;
        team1 = n1;
        team2 = n2;
        type = div;
    }
    
    public int freq1() {
        return freq1;
    }
    
    public int freq2() {
        return freq2;
    }
    
    public String[] team1() {
        return team1;
    }
    
    public String[] team2() {
        return team2;
    }
    
    public int type() {
        return type;
    }
}
