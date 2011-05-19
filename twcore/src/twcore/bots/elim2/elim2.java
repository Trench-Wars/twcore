/**
 * This bot runs elim and baseelim without any database backup.
 */
package twcore.bots.elim2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.util.Spy;
import twcore.core.events.*;
import twcore.core.game.Player;

/**
 * @author WingZero
 *
 */
/**
 * @author Tyler
 *
 */
/**
 * @author Tyler
 *
 */
public class elim2 extends SubspaceBot {

    public OperatorList ops;
    public Spy racismWatcher;
    public String db = "website";
    public Random rand = new Random();
    public BotAction ba;
    
    
    // BotSettings variables
    BotSettings cfg;
    public int cfg_minPlayers, cfg_maxDeaths, cfg_maxKills, cfg_defaultShip, cfg_gameType;
    public int cfg_votingLength, cfg_waitLength, cfg_zone, cfg_border;
    public int[] cfg_safe, cfg_barY;
    public int[][] cfg_barspots;
    public String cfg_arena, cfg_chats, cfg_gameName;
    public ArrayList<Integer> cfg_shipTypes = new ArrayList<Integer>();

    // Defaults
    public int[] defaultElimTypes = {1,3,4,7,9};
    public int[] defaultBelimTypes = {2,5,6,8,9};
    public String[]  sMessages = {
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

    // Enums
    public static final int ELIM = 1;           //Game type and game style
    public static final int BASEELIM = 2;       //Game type
    public static final int KILLRACE = 2;       //Game style
    public static final int OFF = 0;            //Simple boolean used for int variables
    public static final int ON = 1;             //Simple boolean used for int variables
    public static final int DISCONNECT = 2;     //Bot mode... bot will disconnect after game finishes
    public static final int SPAWN_TIME = 5005;  //Time to wait before warping a player after a death in milliseconds
    public static final int SPAWN_NC = 1;       //Time that a spawn kill won't count after spawning in seconds
    public static final int LAGOUT_TIME = 30;   //Time you must wait before returning from a lagout in seconds
    public static final int LAGOUT_RETURN = 60; //Time you must return before when returning from a lagout in seconds
    public static final int MAX_LAGOUT = 2;     //Maximum number of lagouts allowed
    public static final int DOUBLE_KILL = 5;    //Number of seconds after a kill for another kill to be considered duplicate
    public static final int MIN_ZONER = 15;     //The minimum amount of minutes that another zoner can be sent
    public static final int BOUNDARY_TIME = 30; //The maximum amount of time you can be outside of base in seconds
    public static final int STREAK_INIT = 5;    //The number of kills needed for the first streak
    public static final int STREAK_REPEAT = 2;  //The number of kills needed for streak messages after the initial streak
    public static final int SAFE_HEIGHT = 14;   //The height of the safety area in tiles
    public static final int ANY_SHIP = 9;       //A ship type... all ships within the cfg_shipTypes are allowed
    public static final int SPEC_FREQ = 9999;   //The spec frequency
    public static final int MAX_FREQ = 99;      //The maximum frequency casual players can be on
    public static final int LIMIT_STREAK = 3;   //The amount of consecutive game wins to limit minimum kill/death voting
    public static final int MIN_FOR_STREAK = 5; //The minimum kills/deaths allowable to vote on during a game winning streak

    // Local variables
    public Game game = new Game();
    public int gameStyle = 1;
    public String lastWinner = "-anonymous-";
    public int winStreak = 0;
    public long lastZoner = 0;
    public ElimPlayer winner;
    
    // Local collections
    public HashMap<String, Integer> votes = new HashMap<String, Integer>();
    public HashMap<String, TimerTask> lagouts = new HashMap<String, TimerTask>();
    public HashMap<String, ElimPlayer> players = new HashMap<String, ElimPlayer>();
    
    /**
     * @param botAction
     */
    public elim2(BotAction botAction) {
        super(botAction);
        ba = m_botAction;
        reinstantiateCFG();
        racismWatcher = new Spy(botAction);
        ops = m_botAction.getOperatorList();

        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.LOGGED_ON);
        req.request(EventRequester.PLAYER_ENTERED);
        req.request(EventRequester.PLAYER_LEFT);
        req.request(EventRequester.WEAPON_FIRED);
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(EventRequester.FREQUENCY_CHANGE);
        req.request(EventRequester.PLAYER_DEATH);
        req.request(EventRequester.PLAYER_POSITION);
        req.request(EventRequester.ARENA_JOINED);
    }

    // Helpers
    private void reinstantiateCFG() {
        cfg = m_botAction.getBotSettings();
        int botnum = getBotNumber(cfg);
        cfg_minPlayers = cfg.getInt("MinPlayers");
        cfg_maxDeaths = cfg.getInt("MaxDeaths");
        cfg_maxKills = cfg.getInt("MaxKills");
        cfg_votingLength = cfg.getInt("VotingLength");
        cfg_waitLength = (cfg.getInt("WaitLength") - 10);
        cfg_zone = cfg.getInt("SendZoner");
        cfg_border = cfg.getInt("Border");
        cfg_arena = cfg.getString("Arena" + botnum);
        cfg_gameType = cfg.getInt("GameType" + botnum);
        cfg_defaultShip = cfg.getInt("DefaultShip" + cfg_gameType);
        cfg_chats = cfg.getString("Chats" + cfg_gameType);
        cfg_safe = cfg.getIntArray("Safe" + cfg_gameType, ",");
        if (cfg_gameType == ELIM)
            cfg_gameName = "elim";
        else {
            cfg_gameName = "belim";
            cfg_barY = cfg.getIntArray("BarwarpY", ",");
            cfg_barspots = new int[cfg.getInt("Barspots")][4];
            for (int i = 0; i < cfg_barspots.length; i++)
                cfg_barspots[i] = cfg.getIntArray("Barspot" + (i + 1), ",");
        }
        String[] types = cfg.getString("ShipTypes" + cfg_gameType).split(",");
        cfg_shipTypes.clear();
        try {
            for (int i = 0; i < types.length; i++)
                cfg_shipTypes.add(Integer.parseInt(types[i]));
        } catch (NumberFormatException e) {
            if (cfg_gameType == ELIM) {
                for (int i = 0; i < defaultElimTypes.length; i++)
                    cfg_shipTypes.add(defaultElimTypes[i]);
            } else {
                for (int i = 0; i < defaultBelimTypes.length; i++)
                    cfg_shipTypes.add(defaultBelimTypes[i]);
            }
        }
    }
    
    private int getBotNumber(BotSettings cfg){ 
        for (int i = 1; i <= cfg.getInt("Max Bots"); i++){
            if (cfg.getString("Name" + i).equalsIgnoreCase(m_botAction.getBotName()))
                return i;
        }
        return 1;
    }
    
    private ElimPlayer check(String name) {
        if (!players.containsKey(name.toLowerCase()))
            players.put(name.toLowerCase(), new ElimPlayer(name));
        return players.get(name.toLowerCase());
    }
    
    // Event handlers
    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(cfg_arena);
        m_botAction.specAll();
        m_botAction.receiveAllPlayerDeaths();
        m_botAction.setPlayerPositionUpdating( 400 );
        m_botAction.sendUnfilteredPublicMessage("?chat=" + cfg_chats);        
    }
    
    public void handleEvent(ArenaJoined event) {
        /* this sets the spawns, need to do this later
        for (int z = 0; z < 4; z++) {
            m_botAction.sendUnfilteredPublicMessage("?set spawn:team" + z + "-x:" + cfg_competitive[0]);
            m_botAction.sendUnfilteredPublicMessage("?set spawn:team" + z + "-y:" + cfg_competitive[1]);
            m_botAction.sendUnfilteredPublicMessage("?set spawn:team" + z + "-radius:" + cfg_competitive[2]);
        }
        */
        
        Iterator<Player> i = ba.getPlayerIterator();
        while (i.hasNext()) {
            Player p = i.next();
            String name = p.getPlayerName();
            if (!ops.isBotExact(name)) {
                players.put(name.toLowerCase(), new ElimPlayer(name));
                // add warper
            }
        }
    }
    
    /**
     * Message handler handles all commands
     */
    public void handleEvent(Message event) {
        
    }
    
    
    public void handleEvent(PlayerEntered event) {
        Player player = ba.getPlayer(event.getPlayerID());
        String name;
        if (player == null)
            return;    
        name = player.getPlayerName();
        
        if (lagouts.containsKey(name.toLowerCase()))
            ba.sendPrivateMessage(name, "To get back in, reply with !lagout");
        else
            check(name);
    }
    
    /**
     * Handles the PlayerLeft events
     *  - does lagouts
     *  - updates player list
     *  - updates ElimPlayer
     */
    public void handleEvent(PlayerLeft event) {
        Player player = ba.getPlayer(event.getPlayerID());
        String name;
        if (player == null)
            return;    
        name = player.getPlayerName();
        
        ElimPlayer ep = check(name);
        if (game.state == Game.GAME_IN_PROGRESS && ep.status == ElimPlayer.IN) {
            
        }
        
        
    }
    
    /**
     * Handles the PlayerDeath events
     *  - increments kills and deaths of the players
     *  - checks for spawn kills
     *  - removes players reaching the death elim count
     *   
     */
    public void handleEvent(PlayerDeath event) {
        
    }
    
    /**
     * Handles the PlayerPosition events
     *  - deals with the baseelim out of base rule/timer
     *  - ?
     */
    public void handleEvent(PlayerPosition event) {
        
    }
    
    /**
     * Handles the FrequencyShipChange events
     *  - checks for a lagout
     */
    public void handleEvent(FrequencyShipChange event) {
        
    }
    
    /**
     * unused
     */
    public void handleEvent(FrequencyChange event) {
        
    }
    
    /**
     * Handles the WeaponFired events
     *  - this will be used for player stats later on
     */
    public void handleEvent(WeaponFired event) {
        
    }
    
    // Bot functions
    public void doVotesShip() {
        
    }
    
    public void doVotesDeaths() {
        
    }
    
    public void doCountVotes() {
        
    }
    
    // Player commands
    @SuppressWarnings("unused")
    public void cmd_lagout(String name) {
        ElimPlayer ep = check(name);
        if (lagouts.containsKey(name.toLowerCase())) {
            ba.cancelTask(lagouts.remove(name.toLowerCase()));
            // needs a lot more stuff
        }
        
    }
    
    public void cmd_notPlaying(String name) {
        ElimPlayer p = check(name);
        
        if (p.notPlaying()) {
            
            ba.sendPrivateMessage(name, "NotPlaying has been turned OFF.");
        } else {
            ba.specWithoutLock(name);
            
            if (game.state != Game.GAME_IN_PROGRESS && game.state != Game.TEN_SECONDS)
                p.status(ElimPlayer.SPEC);
            else
                p.status(ElimPlayer.OUT);
            
            ba.sendPrivateMessage(name, "NotPlaying has been turned ON.");
        }
    }
    
    
    class Lagout extends TimerTask {

        String name;
        
        public Lagout(String n) {
            name = n;
        }
        
        @Override
        public void run() {
            ba.sendPrivateMessage(name, "You have been lagged out for too long and have been removed from the game.");
            ElimPlayer ep = players.remove(name.toLowerCase());
            if (ep != null) {
                if (ep.here)
                    ep.status(ElimPlayer.OUT);
                else
                    players.remove(name.toLowerCase());
            }
            lagouts.remove(name.toLowerCase());
        }
    }
    
    class ElimPlayer {
        String name;
        int wins, losses, lagouts;
        int status;

        boolean playing = true;
        boolean here = true;
        
        static final int SPEC = 0;
        static final int IN = 1;
        static final int OUT = 2;
        static final int LAGOUT = 3;
        
        public ElimPlayer(String n) {
            name = n;
            status = 0;
            wins = 0;
            losses = 0;
            lagouts = elim2.MAX_LAGOUT;
        }
        
        public int lagout() {
            lagouts--;
            return lagouts;
        }
        
        public void addKill() {
            wins++;
        }
        
        public void addDeath() {
            losses++;
        }
        
        public void removeKill() {
            wins--;
        }
        
        public void remoevDeath() {
            losses--;
        }
        
        public void status(int s) {
            status = s;
        }
        
        public boolean notPlaying() {
            playing = !playing;
            return playing;
        }
        
        public boolean here() {
            here = !here;
            return here;
        }
    }

    @SuppressWarnings("unused")
    class Game {
        private static final int OFF_MODE = -1;
        private static final int WAITING_FOR_PLAYERS = 0;
        private static final int VOTING_ON_SHIP = 1;
        private static final int VOTING_ON_DEATHS = 2;
        private static final int VOTING_ON_SHRAP = 3;
        private static final int WAITING_TO_START = 4;
        private static final int TEN_SECONDS = 5;
        private static final int GAME_IN_PROGRESS = 6;
        private static final int GAME_OVER = 7;
        
        private int state = 0;
        
        public Game() {
            state = 0;
        }
        
        public int next() {
            if (state == GAME_OVER)
                state = 0;
            else
                state++;
            return state;
        }
        
        public void setState(int s) {
            state = s;
        }
    }
    
    
    
    
}
