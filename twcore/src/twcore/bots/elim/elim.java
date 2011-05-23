/**
 * This bot runs elim and baseelim without any database backup.
 */
package twcore.bots.elim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.TimerTask;
import java.util.TreeMap;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.util.Spy;
import twcore.core.util.Tools;
import twcore.core.events.*;
import twcore.core.game.Player;

/**
 * @author WingZero
 *
 */
public class elim extends SubspaceBot {

    public OperatorList ops;
    public Spy racismWatcher;
    public Random rand = new Random();
    public BotAction ba;
    
    
    // BotSettings variables
    BotSettings cfg;
    public int cfg_minPlayers, cfg_maxDeaths, cfg_maxKills, cfg_defaultShip, cfg_gameType;
    public int cfg_votingLength, cfg_waitLength, cfg_zone, cfg_border;
    public int[] cfg_safe, cfg_barY, cfg_spawn;
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
    public static final int LAGOUT_TIME = 15;   //Time you must wait before returning from a lagout in seconds
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
    public Game game = new Game();              //Used to keep track of the current game state
    public int botMode = 1;                     //Used to prevent or enable the bot from starting games
    public int gameStyle = 1;                   //Elim or KillRace
    public String lastWinner = "-anonymous-";   
    public int winStreak = 0;                   
    public long lastZoner = 0;
    public long lastChat = 0;
    public ElimPlayer winner;
    public int shrap = OFF;                     //Used for ship types that use shrap
    public int shipType = 1;                    //Current game ship type
    public int deathElim = 10;                  //Used when the game is not a kill race
    public int killElim = 10;                   //Used for games that race to a number of kills (kill race)
    
    // Local collections
    public HashMap<Integer, Integer> votes = new HashMap<Integer, Integer>();       //Keeps track of the current vote tallies
    public HashMap<String, Lagout> lagouts = new HashMap<String, Lagout>();         //Keeps track of lagouts for current game
    public HashMap<String, ElimPlayer> players = new HashMap<String, ElimPlayer>(); //Holds player objects for whole arena
    public TreeMap<String, ElimPlayer> elimers = new TreeMap<String, ElimPlayer>(); //Holds player objects for players in a ship
    public LinkedList<String> alert = new LinkedList<String>();                       //List of players to send new game alerts
    
    /**
     * @param botAction
     */
    public elim(BotAction botAction) {
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
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(EventRequester.PLAYER_DEATH);
        req.request(EventRequester.PLAYER_POSITION);
        req.request(EventRequester.ARENA_JOINED);
    }

    // Helpers
    private void reinstantiateCFG() {
        cfg = ba.getBotSettings();
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
        cfg_spawn = cfg.getIntArray("Competitive" + cfg_gameType, ",");
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
            if (cfg.getString("Name" + i).equalsIgnoreCase(ba.getBotName()))
                return i;
        }
        return 1;
    }
    
    private ElimPlayer check(String name) {
        if (!players.containsKey(name.toLowerCase()))
            players.put(name.toLowerCase(), new ElimPlayer(name));
        return players.get(name.toLowerCase());
    }
    
    public String getStatusMsg(){
        switch(game.state){
            case Game.OFF_MODE:
                return "Elimination is temporarily disabled.";
            case Game.WAITING_FOR_PLAYERS:
                return "A new elimination match will begin when " + cfg_minPlayers + " more player(s) enter.";
            case Game.VOTING_ON_SHIP:
                return "We are currently voting on ship type.";
            case Game.VOTING_ON_DEATHS:
                if(gameStyle == ELIM)
                    return "We are playing " + Tools.shipName(shipType) + " elim. We are currently voting on number of deaths.";
                else return "We are playing " + Tools.shipName(shipType) + " kill race. We are currently voting on number of kills.";
            case Game.VOTING_ON_SHRAP:
                if(gameStyle == ELIM)
                    return "We are playing " + Tools.shipName(shipType) + " elim to " + deathElim + ". We are currently voting on shrap.";
                else return "We are playing " + Tools.shipName(shipType) + " kill race to " + killElim + ". We are currently voting on shrap.";
            case Game.WAITING_TO_START:
                if(gameStyle == ELIM)
                    return "We are playing " + Tools.shipName(shipType) + " elim to " + deathElim + ". The game will start soon. Enter to play!";
                else return "We are playing " + Tools.shipName(shipType) + " kill race to " + killElim + ". The game will start soon. Enter to play!";
            case Game.TEN_SECONDS:
                return "The game will begin in less than ten seconds. No more entries";
            case Game.GAME_IN_PROGRESS:
                if(gameStyle == ELIM)
                    return "We are currently playing " + Tools.shipName(shipType) + " elim to " + deathElim + ". " + elimers.size() + " players left.";
                else return "We are currently playing " + Tools.shipName(shipType) + " kill race to " + killElim + ". " + elimers.size() + " players playing.";
            case Game.GAME_OVER:
                return "A new elimination match will begin shortly.";
            default: return null;
        }
    }
    
    // Event handlers
    public void handleEvent(LoggedOn event) {
        ba.joinArena(cfg_arena);
        ba.specAll();
        ba.receiveAllPlayerDeaths();
        ba.setPlayerPositionUpdating(400);
        ba.sendUnfilteredPublicMessage("?chat=" + cfg_chats);   
        ba.toggleLocked();
        elimers.clear();
    }
    
    public void handleEvent(ArenaJoined event) {
        for (int z = 0; z < 4; z++) {
            ba.sendUnfilteredPublicMessage("?set spawn:team" + z + "-x:" + cfg_spawn[0]);
            ba.sendUnfilteredPublicMessage("?set spawn:team" + z + "-y:" + cfg_spawn[1]);
            ba.sendUnfilteredPublicMessage("?set spawn:team" + z + "-radius:" + cfg_spawn[2]);
        }
       
        Iterator<Player> i = ba.getPlayerIterator();
        while (i.hasNext()) {
            Player p = i.next();
            String name = p.getPlayerName();
            if (name != null && !ops.isBotExact(name)) {
                players.put(name.toLowerCase(), new ElimPlayer(name));
            }
        }
    }
    
    /**
     * Message handler handles all commands
     */
    public void handleEvent(Message event) {
        String msg = event.getMessage();
        String name = event.getMessager() == null ? ba.getPlayerName(event.getPlayerID()) : event.getMessager();
        int type = event.getMessageType();

        if (type == Message.ARENA_MESSAGE) {
            if (msg.startsWith("Arena LOCKED") && !game.isInProgress())
                ba.toggleLocked();
            else if (msg.startsWith("Arena UNLOCKED") && game.isInProgress())
                ba.toggleLocked();
        }

        if (type == Message.PUBLIC_MESSAGE || type == Message.TEAM_MESSAGE || type == Message.OPPOSING_TEAM_MESSAGE || type == Message.PUBLIC_MACRO_MESSAGE)
            racismWatcher.handleEvent(event);
        else if ((type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE)) {
            if (msg.equals("!lagout"))
                cmd_lagout(name);
            else if (msg.equalsIgnoreCase("!status") || msg.equalsIgnoreCase("!s"))
                ba.sendSmartPrivateMessage(name, getStatusMsg());
            else if (msg.equalsIgnoreCase("!help"))
                cmd_help(name);
            else if (msg.equalsIgnoreCase("!alert")) 
                cmd_alert(name);
            
            if (ops.isModerator(name)) {
                if (msg.equals("!die"))
                    ba.scheduleTask(new DieTask(name), 500);
                else if (msg.startsWith("!remove ") && msg.length() > 8)
                    cmd_remove(name, msg.substring(msg.indexOf(" ")));
            }
            if (ops.isSmod(name)) {
                if(msg.equalsIgnoreCase("!zone"))
                    cmd_zone(name);
                else if(msg.equalsIgnoreCase("!stop"))
                    cmd_stop(name);
                else if(msg.equalsIgnoreCase("!off"))
                    cmd_off(name, false);
                else if(msg.equalsIgnoreCase("!on"))
                    cmd_on(name);
                else if(msg.equalsIgnoreCase("!shutdown"))
                    cmd_off(name, true);
                else if(msg.equalsIgnoreCase("!die")){
                    try{
                        ba.scheduleTask(new DieTask(name), 500);
                    }catch(Exception e){}
                }
            }
            if (ops.isSysop(name)) {
                if (msg.startsWith("!greetmsg "))
                    ba.sendUnfilteredPublicMessage("?set misc:greetmessage:" + msg.substring(10));
                else if (msg.equalsIgnoreCase("!updatecfg"))
                    reinstantiateCFG();
            }
        }
        if (type == Message.PUBLIC_MESSAGE && game.isVoting() && Tools.isAllDigits(msg) && msg.length() <= 2)
            vote(name, msg);
    }
    
    public void handleEvent(PlayerEntered event) {
        Player player = ba.getPlayer(event.getPlayerID());
        String name;
        if (player == null)
            return;    
        name = player.getPlayerName();
        
        if (game.state == Game.GAME_IN_PROGRESS && lagouts.containsKey(name.toLowerCase()))
            ba.sendPrivateMessage(name, "To get back in, reply with !lagout");
        else {
            ElimPlayer ep = check(name);
            ba.sendSmartPrivateMessage(name, "Welcome to " + cfg_arena + "! " + getStatusMsg());
            if (game.state != Game.GAME_IN_PROGRESS && game.state != Game.TEN_SECONDS && ep.ship > 0)
                elimers.put(name.toLowerCase(), ep);
        }

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
            if (ep.lagouts <= 0) {
                ep.status = ElimPlayer.OUT;
                elimers.remove(name.toLowerCase());
                ba.sendSmartPrivateMessage(name, "You forfeit because you have run out of lagouts");
                ba.sendArenaMessage(name + " is out with " + ep.wins + " wins and " + ep.losses + " losses (too many lagouts)");
                
            } else {
                ep.status = ElimPlayer.LAGOUT;
                ba.sendSmartPrivateMessage(name, "You have " + LAGOUT_RETURN + " seconds to return (!lagout) or you will forfeit.");
                lagouts.put(name.toLowerCase(), new Lagout(name, System.currentTimeMillis()));
                ba.scheduleTask(lagouts.get(name.toLowerCase()), LAGOUT_RETURN * 1000);
            }
        } else
            players.remove(name.toLowerCase());
        
        if (elimers.size() == 1) {
            winner = elimers.get(elimers.firstKey());
            game.next();
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
        String killerName = ba.getPlayerName(event.getKillerID());
        String killeeName = ba.getPlayerName(event.getKilleeID());
        if (game.state != Game.GAME_IN_PROGRESS || killerName == null || killeeName == null)
            return;
        
        ElimPlayer killer = players.get(killerName.toLowerCase());
        ElimPlayer killee = players.get(killeeName.toLowerCase());
        if (killer == null || killer == null) {
            if (killer == null) {
                ba.specWithoutLock(killerName);
                killer = check(killerName);
                ba.sendPrivateMessage(killerName, "An error has occured and you must be specced for this game.");
                killer.status = ElimPlayer.SPEC;
            }
            if (killee == null) {
                ba.specWithoutLock(killeeName);
                killee = check(killeeName);
                ba.sendPrivateMessage(killeeName, "An error has occured and you must be specced for this game.");
                killee.status = ElimPlayer.SPEC;
            }
            return;
        }
        
        if ((System.currentTimeMillis() - killee.lastSpawn < SPAWN_NC * 1000) || (System.currentTimeMillis() - killer.lastSpawn < SPAWN_NC * 1000)) {
            ba.sendPrivateMessage(killerName, "Spawn kill (no count).");
            ba.sendPrivateMessage(killeeName, "Spawn kill (no count).");
            return;            
        }
        
        killer.addKill();
        
        if (killee.streak >= 5) 
            ba.sendArenaMessage("Streak breaker! " + killeeName + "(" + killee.streak + ":0) broken by " + killerName + "!", Tools.Sound.INCONCEIVABLE);
        
        if (elimers.containsKey(killeeName.toLowerCase()))
            killee.addDeath();
        
        if (gameStyle == ELIM && killee.losses == deathElim && elimers.containsKey(killeeName.toLowerCase())) {
            killee.status = ElimPlayer.OUT;
            elimers.remove(killeeName.toLowerCase());
            ba.specWithoutLock(killeeName);
            ba.sendArenaMessage(killeeName + " is out with " + killee.wins + " wins and " + killee.losses + " losses");
        } else if (gameStyle == KILLRACE && killer.wins == killElim && elimers.containsKey(killerName.toLowerCase())) {
            Iterator<ElimPlayer> i = elimers.values().iterator();
            while (i.hasNext()) {
                ElimPlayer ep = i.next();
                if (!ep.name.equalsIgnoreCase(killerName)) {
                    i.remove();
                }
            }
        } else if (elimers.containsKey(killeeName.toLowerCase())) {
            killee.clearBorderInfo();
            new Spawn(killeeName);
        }
        
        if (elimers.size() == 1) {
            winner = elimers.get(elimers.firstKey());
            game.next();
        }
    }
    
    /**
     * Handles the PlayerPosition events
     *  - deals with the baseelim out of base rule/timer
     *  - ?
     */
    public void handleEvent(PlayerPosition event) {
        Player p = ba.getPlayer(event.getPlayerID());
        if (p == null) return;
        doWarpIntoElim(p.getPlayerName());
        if(cfg_gameType == ELIM)return;
        ElimPlayer ep = elimers.get(p.getPlayerName());
        if(ep != null && game.isInProgress()){
            if(p.getYTileLocation() < cfg_border)
                ep.clearBorderInfo();
            else if(p.getYTileLocation() > cfg_border){         
                if(ep.outOfBounds == 0)
                    ep.outOfBounds = System.currentTimeMillis();
                else if((System.currentTimeMillis() - ep.outOfBounds) > BOUNDARY_TIME * Tools.TimeInMillis.SECOND){
                    ep.status = ElimPlayer.OUT;
                    ba.specWithoutLock(ep.name);
                    elimers.remove(p.getPlayerName().toLowerCase());
                    ba.sendArenaMessage(ep.name + " is out. " + ep.wins + " wins " + ep.losses + " losses (Too long outside base)");
                }
                else if((System.currentTimeMillis() - ep.outOfBounds) > (BOUNDARY_TIME/2) * Tools.TimeInMillis.SECOND && !ep.outsideWarn){
                    ba.sendSmartPrivateMessage( ep.name, "Get in the base!");
                    ep.outsideWarn = true;
                }           
            }
            if(elimers.size() == 1 && game.state == Game.GAME_IN_PROGRESS){
                winner = elimers.get(elimers.firstKey());
                game.next();
            }
        } 
    }
    
    /**
     * Handles the FrequencyShipChange events
     *  - checks for a lagout
     */
    public void handleEvent(FrequencyShipChange event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null)
            return;
        
        ElimPlayer ep = check(name);
        int ship = event.getShipType();
        if (ship == 0 && game.state == Game.GAME_IN_PROGRESS && ep.status == ElimPlayer.IN) {
            if (ep.lagouts <= 0) {
                ep.status = ElimPlayer.OUT;
                ba.sendSmartPrivateMessage(name, "You forfeit because you have run out of lagouts");
                ba.sendArenaMessage(name + " is out with " + ep.wins + " wins and " + ep.losses + " losses (too many lagouts)");
                elimers.remove(name.toLowerCase());
            } else {
                ep.status = ElimPlayer.LAGOUT;
                ba.sendSmartPrivateMessage(name, "You have " + LAGOUT_RETURN + " seconds to return (!lagout) or you will forfeit.");
                lagouts.put(name.toLowerCase(), new Lagout(name, System.currentTimeMillis()));
                ba.scheduleTask(lagouts.get(name.toLowerCase()), LAGOUT_RETURN * 1000);
            }
        } else if (ship > 0 && !game.isInProgress()) {
            ep.status = ElimPlayer.IN;
            elimers.put(name.toLowerCase(), ep);
            if (game.state == Game.WAITING_FOR_PLAYERS && elimers.size() >= cfg_minPlayers)
                game.next();
        } else if (ship == 0 && game.state != Game.GAME_IN_PROGRESS) {
            ep.status = ElimPlayer.SPEC;
            elimers.remove(name.toLowerCase());
        }
        
        if (elimers.size() == 1) {
            winner = elimers.get(elimers.firstKey());
            game.next();
        }
    }
    
    // Bot functions

    /**
     * Checks to see if there are enough players to begin a game
     * and if not then it will wait
     */
    public void doWaitingForPlayers() {
        game.state = Game.WAITING_FOR_PLAYERS;
        int needs = cfg_minPlayers - elimers.size();
        if (needs <= 0) {
            if (cfg_zone == ON)
                doElimZoner();
            for (int i = 1; i <= 2; i++)
                ba.sendChatMessage(i, "Next " + cfg_gameName + " is starting. Type ?go " + cfg_arena + " to play");
            TimerTask wait10Seconds = new TimerTask() {
                public void run() {
                    game.next();
                }
            };
            ba.scheduleTask(wait10Seconds, 10 * Tools.TimeInMillis.SECOND);
        } else
            ba.sendArenaMessage("A new elimination match will begin when " + needs + " more player(s) enter.");

        for (String name : alert)
            ba.sendSmartPrivateMessage(name, "Next " + cfg_gameName + " is starting! Type ?go " + cfg_arena + " to play!");
    }

    /**
     * Initiates the ship voting process
     */
    public void doVotingOnShip() {
        game.state = Game.VOTING_ON_SHIP;
        shipType = cfg_defaultShip;
        shrap = OFF;
        String msg = "Vote: ";
        for(int i=0;i<cfg_shipTypes.size();i++){
            msg += Tools.shipName(cfg_shipTypes.get(i)) + " - " + cfg_shipTypes.get(i) + ", ";
            votes.put(cfg_shipTypes.get(i),0);
            votes.put(cfg_shipTypes.get(i) * 10, 0);
        }
        msg = msg.substring(0, msg.length()-2);
        ba.sendArenaMessage(msg);
        ba.sendArenaMessage("NOTICE: To vote for a kill race add a 0 on the end of your vote.");
        game.next(cfg_votingLength * Tools.TimeInMillis.SECOND);
    }

    /**
     * Initiates the death voting process
     */
    public void doVotingOnDeaths() {
        game.state = Game.VOTING_ON_DEATHS;
        deathElim = -1;
        killElim = -1;
        int min = 1;
        if (winStreak >= LIMIT_STREAK)
            min = MIN_FOR_STREAK;
        if (gameStyle == ELIM)
            for (int i = min; i <= cfg_maxDeaths; i++)
                votes.put(i, 0);
        else
            for (int i = min; i <= cfg_maxKills; i++)
                votes.put(i, 0);
        game.next(cfg_votingLength * Tools.TimeInMillis.SECOND);
    }

    /**
     * Initiates the shrap voting process if the game type allows
     */
    public void doVotingOnShrap() {
        game.state = Game.VOTING_ON_SHRAP;
        votes.put(0,0);
        votes.put(1,0);
        game.next(cfg_votingLength * Tools.TimeInMillis.SECOND);        
    }

    /**
     * Allows time for players to enter the game
     */
    public void doWaitingToStart() {
        game.state = Game.WAITING_TO_START;
        ba.sendArenaMessage("Enter to play. Game will begin in " + (cfg_waitLength + 10) + " seconds");
        ba.toggleLocked();
        if(deathElim == 1 && gameStyle == ELIM)
            ba.sendArenaMessage("Rules: All on own freq, no teaming! Die " + deathElim + " time and you are out");
        else if(gameStyle == ELIM)
            ba.sendArenaMessage("Rules: All on own freq, no teaming! Die " + deathElim + " times and you are out");
        else if(killElim == 1 && gameStyle == KILLRACE)
            ba.sendArenaMessage("Rules: All on own freq, no teaming! Get 1 kill to win");
        else
            ba.sendArenaMessage("Rules: All on own freq, no teaming! Get " + killElim + " kills to win");
        
        game.next(cfg_waitLength * Tools.TimeInMillis.SECOND);
    }

    /**
     * Prepares for the start of the game:
     *  - puts everyone on an individual freq
     *  - puts everyone in the right ship
     *  - sets the doors according to game size
     */
    public void doTenSeconds() {
        game.state = Game.TEN_SECONDS;
        ba.toggleLocked();
        ba.sendArenaMessage("Get ready. Game will start in 10 seconds");
        int freq = 600;
        Iterator<ElimPlayer> i = elimers.values().iterator();
        while(i.hasNext()){
            ElimPlayer ep = i.next();
            Player p = ba.getPlayer(ep.name);
            if(p == null || p.getShipType() == 0){
                i.remove();
                continue;
            }
            ep.freq = freq;
            if(p.getFrequency() != ep.freq)
                ba.setFreq(ep.name, freq);
            ep.ship = p.getShipType();
            if(shipType == ANY_SHIP){
                if(!cfg_shipTypes.contains(p.getShipType())){
                    ba.setShip(p.getPlayerName(), cfg_defaultShip);
                    ep.ship = cfg_defaultShip;
                }
            }
            else if(p.getShipType() != shipType){
                ep.ship = shipType;
                ba.setShip(p.getPlayerName(), shipType);
            }
            freq++;
        }
        if(elimers.size() >= 15)
            ba.setDoors("00001000");
        else
            ba.setDoors(255);
        game.next(10 * Tools.TimeInMillis.SECOND);
    }

    /**
     * Starts the game
     */
    public void doGameInProgress() {
        game.state = Game.GAME_IN_PROGRESS;
        lagouts.clear();

        ba.sendArenaMessage("GO! GO! GO!", Tools.Sound.GOGOGO);
        
        for (ElimPlayer ep : elimers.values()) {
            ep.resetScore();
            ep.shipChangeWarn = false;
            if(shrap == ON)
                ba.specificPrize(ep.name, Tools.Prize.SHRAPNEL);
            ba.specificPrize(ep.name, Tools.Prize.MULTIFIRE);
            ba.scoreReset(ep.name);
        }
    }

    /**
     * Ends the game
     */
    public void doGameOver() {
        game.state = Game.GAME_OVER;
        ba.sendArenaMessage("GAME OVER. Winner: " + winner.name + "!", Tools.Sound.HALLELUJAH);
        if (lastWinner.equalsIgnoreCase(winner.name)) {
            winStreak++;
        } else {
            winStreak = 1;
        }
        
        lastWinner = winner.name;
        if (gameStyle == KILLRACE) {
            Iterator<Player> i = ba.getPlayingPlayerIterator();
            while (i.hasNext()) {
                Player p = i.next();
                String name = p.getPlayerName();
                if (name == null)
                    continue;
                ElimPlayer ep = check(name);
                ep.ship = p.getShipType();
                ep.freq = p.getFrequency();
                ep.status = ElimPlayer.IN;
                ep.resetScore();
                elimers.put(name.toLowerCase(), ep);
            }
        }
        ba.toggleLocked();
        game.next(20 * Tools.TimeInMillis.SECOND);            
    }

    /**
     * Registers the player's vote
     * 
     * @param name
     * @param msg
     */
    public void vote(String name, String msg) {
        if (!elimers.containsKey(name.toLowerCase())) {
            ba.sendPrivateMessage(name, "You must be playing to vote!");
            return;
        }

        int vote = 0;
        ElimPlayer p;
        try {
            vote = Integer.valueOf(msg);
        } catch (NumberFormatException e) {
            Tools.printStackTrace(e);
        }
        p = elimers.get(name.toLowerCase());
        if (votes.containsKey(vote)) {
            if (p.vote != -1)
                votes.put(p.vote, votes.get(p.vote) - 1);
            p.vote = vote;
            votes.put(vote, votes.get(vote) + 1);
        }
    }
    
    /**
     * Counts the votes and sets the appropriate variable (ships or deaths)
     */
    public void doCountVotes() {
        int max = 0;
        ArrayList<Integer> winners = new ArrayList<Integer>();
        Iterator<Integer> i = votes.keySet().iterator();
        while (i.hasNext()) {
            int x = i.next();
            if (votes.get(x) > max) {
                max = votes.get(x);
                winners.clear();
                winners.add(x);
            } else if (votes.get(x) == max && max != 0)
                winners.add(x);
        }
        if (game.state == Game.VOTING_ON_SHIP) {
            if (winners.isEmpty()) {
                shipType = cfg_defaultShip;
            } else if (winners.size() > 1) {
                shipType = winners.get(rand.nextInt(winners.size() - 1));
            } else
                shipType = winners.get(0);
            gameStyle = ELIM;
            if (shipType > 9) {
                shipType /= 10;
                gameStyle = KILLRACE;
            }
            int min = 1;
            if (winStreak >= LIMIT_STREAK)
                min = MIN_FOR_STREAK;
            if (gameStyle == ELIM)
                ba.sendArenaMessage("This will be " + Tools.shipName(shipType) + " elim. VOTE: How many deaths? (" + min + "-" + cfg_maxDeaths + ")");
            else
                ba.sendArenaMessage("This will be " + Tools.shipName(shipType) + " kill race. VOTE: How many kills? (" + min + "-" + cfg_maxKills + ")");
        } else if (game.state == Game.VOTING_ON_DEATHS) {
            if (gameStyle == ELIM) {
                if (winners.isEmpty())
                    deathElim = cfg_maxDeaths;
                else if (winners.size() > 1)
                    deathElim = winners.get(rand.nextInt(winners.size() - 1));
                else
                    deathElim = winners.get(0);
                if (cfg_gameType == ELIM || shipType != 2)
                    ba.sendArenaMessage(Tools.shipName(shipType) + " elim to " + deathElim);
                else
                    ba.sendArenaMessage(Tools.shipName(shipType) + " elim to " + deathElim + ". VOTE: Shrap on or off? (1-on, 0-off)");
            } else {
                if (winners.isEmpty())
                    killElim = cfg_maxKills;
                else if (winners.size() > 1)
                    killElim = winners.get(rand.nextInt(winners.size() - 1));
                else
                    killElim = winners.get(0);
                if (cfg_gameType == ELIM || shipType != 2)
                    ba.sendArenaMessage(Tools.shipName(shipType) + " kill race to " + killElim);
                else
                    ba.sendArenaMessage(Tools.shipName(shipType) + " kill race to " + killElim + ". VOTE: Shrap on or off? (1-on, 0-off)");
            }
        } else if (game.state == Game.VOTING_ON_SHRAP) {
            if (winners.isEmpty() || winners.size() > 1)
                shrap = OFF;
            else
                shrap = winners.get(0);
            if (shrap == OFF)
                ba.sendArenaMessage(Tools.shipName(shipType) + " elim to " + deathElim + ". Shrap: [OFF]");
            else
                ba.sendArenaMessage(Tools.shipName(shipType) + " elim to " + deathElim + ". Shrap: [ON]");
        }
        
        // Clear votes
        votes = new HashMap<Integer, Integer>();
        Iterator<ElimPlayer> it = elimers.values().iterator();
        while (it.hasNext())
            it.next().vote = -1;
    }
    
    /**
     * For BaseElim WEASEL games, warp into proper area
     * @param name Player's name
     */
    public void doWarpIntoElim(String name) {
        if(shipType == 6 && cfg_gameType == BASEELIM){
            int[] xarena = cfg.getIntArray("XArena" + rand.nextInt(3), ",");
            ba.warpTo(name, xarena[0], xarena[1], xarena[2]);
        }
    }
    
    public void doElimZoner() {
        if ((System.currentTimeMillis() - lastZoner) < (MIN_ZONER * Tools.TimeInMillis.MINUTE))
            return;
        if(winStreak == 1)
            ba.sendZoneMessage("Next " + cfg_gameName + " is starting. Last round's winner was " + lastWinner + " (" + winner.wins + ":" + winner.losses + ")! Type ?go " + cfg_arena + " to play -" + ba.getBotName());
        else if(winStreak > 1)
            switch(winStreak){
                case 2:ba.sendZoneMessage("Next " + cfg_gameName + " is starting. " + lastWinner + " (" + winner.wins + ":" + winner.losses + ") has won 2 back to back! Type ?go " + cfg_arena + " to play -" + ba.getBotName());
                    break;
                case 3:ba.sendZoneMessage(cfg_gameName.toUpperCase() + ": " + lastWinner + " (" + winner.wins + ":" + winner.losses + ") is on fire with a triple win! Type ?go " + cfg_arena + " to end the streak! -" + ba.getBotName(), Tools.Sound.CROWD_OOO);
                    break;
                case 4:ba.sendZoneMessage(cfg_gameName.toUpperCase() + ": " + lastWinner + " (" + winner.wins + ":" + winner.losses + ") is on a rampage! 4 wins in a row! Type ?go " + cfg_arena + " to put a stop to the carnage! -" + ba.getBotName(), Tools.Sound.CROWD_GEE);
                    break;
                case 5:ba.sendZoneMessage(cfg_gameName.toUpperCase() + ": " + lastWinner + " (" + winner.wins + ":" + winner.losses + ") is dominating with a 5 game streak! Type ?go " + cfg_arena + " to end this madness! -" + ba.getBotName(), Tools.Sound.SCREAM);
                    break;
                default:ba.sendZoneMessage(cfg_gameName.toUpperCase() + ": " + lastWinner + " (" + winner.wins + ":" + winner.losses + ") is bringing the zone to shame with " + winStreak + " consecutive wins! Type ?go " + cfg_arena + " to redeem yourselves! -" + ba.getBotName(), Tools.Sound.INCONCEIVABLE);
                    break;
            }
        else
            ba.sendZoneMessage("Next " + cfg_gameName + " is starting. Type ?go " + cfg_arena + " to play -" + ba.getBotName());
        lastZoner = System.currentTimeMillis();
    }
    
    // Player commands
    /**
     * Handles the !lagout command
     */
    public void cmd_lagout(String name) {
        ElimPlayer ep = check(name);
        
        if(game.state != Game.GAME_IN_PROGRESS)
            return;
        if(lagouts.containsKey(name.toLowerCase())){
            long time = lagouts.get(name.toLowerCase()).time;
            if(System.currentTimeMillis() - time > LAGOUT_TIME * Tools.TimeInMillis.SECOND){
                ep = elimers.get(name.toLowerCase());
                ba.cancelTask(lagouts.remove(name.toLowerCase()));
                ba.sendSmartPrivateMessage( name, "You have been put back into the game. You have " + ep.lagout() + " lagouts left.");
                ep.clearBorderInfo();
                ba.setShip(name, ep.ship);
                ba.setFreq(name, ep.freq);
                doWarpIntoElim(name);
            } else
                ba.sendSmartPrivateMessage( name, "You must wait for " + (LAGOUT_TIME-((System.currentTimeMillis() - time)/Tools.TimeInMillis.SECOND)) + " more seconds.");
        } else
            ba.sendSmartPrivateMessage( name, "You aren't in the game!");   
        
    }    
    
    public void cmd_alert(String name) {
        if (alert.contains(name.toLowerCase())) {
            alert.remove(name.toLowerCase());
            ba.sendSmartPrivateMessage(name, "Removed from alert list!");
        } else {
            alert.add(name.toLowerCase());
            ba.sendSmartPrivateMessage(name, "Added to alert list!");
        }
    }
    
    public void cmd_help(String name) {
        String[] regHelp = {
                "+=============================== HELP MENU ==================================+",
                "| !help         - Displays this menu. (?)                                    |",
                "| !lagout       - Puts you back into the game if you've lagged out. (!l)     |",
                "| !status       - Displays the game status. (!s)                             |",
                "| !alert        - On the 30 second wait, you will be notified of a game.     |",
        };
        String[] modHelp = {
                "+=============================== MOD MENU ===================================+",
                "| !remove <name> - Removes <name> from the game                              |",
        };
        String[] smodHelp = {
                "+=============================== SMOD MENU ==================================+",
                "| !zone         - Toggles whether or not the bot should zone for games.      |",
                "| !stop         - Ends the current game and prevents new games from starting.|",
                "| !off          - Prevents new games from starting once the current one ends.|",
                "| !on           - Turns off !off mode and allows new games to begin          |",
                "| !shutdown     - Causes the bot to disconnect once the current game ends.   |",
                "| !die          - Causes the bot to disconnect.                              |",
        };
        String[] sysopHelp = {
                "| !updatecfg    - Updates the bot to the most current CFG file.              |",
                "| !greetmsg <m> - Sets arena greet message(Sysop only).                      |"                
        };

        ba.privateMessageSpam(name, regHelp);
        if (ops.isModerator(name))
            ba.privateMessageSpam(name, modHelp);
        if (ops.isSmod(name))
            ba.privateMessageSpam(name, smodHelp);
        if (ops.isSysop(name)) 
            ba.privateMessageSpam(name, sysopHelp);
        ba.sendPrivateMessage(name, "+============================================================================+");
    }
    
    public void cmd_remove(String name, String target) {
        boolean spec = true;
        String player = ba.getFuzzyPlayerName(target);
        
        if (player == null) {
            player = target;
            spec = false;
        }
        
        if(!elimers.containsKey(player.toLowerCase()) && !lagouts.containsKey(player.toLowerCase())) {
            ba.sendPrivateMessage(name, player + " could not be found in the playing players list.");
            return;
        }

        if (lagouts.containsKey(player))
            ba.cancelTask(lagouts.remove(player.toLowerCase()));
        elimers.remove(player.toLowerCase());
        
        if (spec)
            ba.specWithoutLock(player);
        
        ba.sendPrivateMessage(name, player + " has been removed from the game.");
    }

    public void cmd_zone(String name) {
        if (cfg_zone == ON) {
            cfg_zone = OFF;
            ba.sendSmartPrivateMessage(name, "The bot will no longer zone at the start of elimination matches.");
        } else {
            cfg_zone = ON;
            ba.sendSmartPrivateMessage(name, "The bot will now zone at the start of elimination matches.");
        }
    }
    
    public void cmd_stop(String name) {
        if(game.state != Game.OFF_MODE){
            ba.sendArenaMessage("This game has been brutally killed by " + name);
            game.state = Game.OFF_MODE;
            lastWinner = "-anonymous-";
            winStreak = 0;
            lagouts.clear();
            votes.clear();
            ba.cancelTasks();
        }
        else
            ba.sendSmartPrivateMessage( name, "There is currently no game in progress.");
    }

    public void cmd_off(String name, boolean shutdown) {
        if ((botMode == OFF && !shutdown) || (botMode == DISCONNECT && shutdown)) {
            ba.sendSmartPrivateMessage(name, "The bot is already set for that task.");
            return;
        }
        if (!shutdown) {
            botMode = OFF;
            ba.sendSmartPrivateMessage(name, "New games will be prevented from starting once the current game ends.");
        } else {
            botMode = DISCONNECT;
            if (game.state == Game.OFF_MODE || game.state == Game.WAITING_FOR_PLAYERS)
                try {
                    ba.scheduleTask(new DieTask(name), 500);
                } catch (Exception e) { }
            else
                ba.sendSmartPrivateMessage(name, "The bot will disconnect once the current game ends.");
        }
    }
    
    public void cmd_on(String name) {
        if(botMode == ON){
            ba.sendSmartPrivateMessage( name, "The bot is already on.");
            return;
        }
        if(botMode == OFF)
            ba.sendSmartPrivateMessage( name, "Off mode disabled. New games will be allowed to start.");
        else
            ba.sendSmartPrivateMessage( name, "Shutdown mode disabled. New games will be allowed to start.");
        botMode = ON;
        if(game.state == Game.OFF_MODE)
            game.next();
    }    
    
    class Lagout extends TimerTask {
        String name;
        long time;
        
        public Lagout(String n, long t) {
            name = n;
            time = t;
        }
        
        @Override
        public void run() {
            ba.sendPrivateMessage(name, "You have been lagged out for too long and have been removed from the game.");
            ElimPlayer ep = players.get(name.toLowerCase());
            if (ep != null) {
                ep.status = ElimPlayer.OUT;
            }
            elimers.remove(name.toLowerCase());
            lagouts.remove(name.toLowerCase());
            if (elimers.size() == 1) {
                winner = elimers.get(elimers.firstKey());
                game.next();
            }
        }
    }
    
    class Spawn {
        String name;
        
        TimerTask spawn = new TimerTask() {
            public void run() {
                if(shrap == ON)
                    ba.specificPrize(name, Tools.Prize.SHRAPNEL);
                ba.specificPrize(name, Tools.Prize.MULTIFIRE);
                doWarpIntoElim(name);
                if(elimers.containsKey(name.toLowerCase())){
                    elimers.get(name.toLowerCase()).lastSpawn = System.currentTimeMillis();
                    if(elimers.get(name.toLowerCase()).ship == 8){
                        ba.specificPrize(name, Tools.Prize.SHRAPNEL);
                        ba.specificPrize(name, Tools.Prize.SHRAPNEL);
                    }
                }
            }
        };
        
        public Spawn(String n) {
            name = n;
            ba.scheduleTask(spawn, SPAWN_TIME);
        }
    }
    
    public class ElimPlayer {
        String name;
        int wins, losses, streak, lagouts, ship, freq, multiKill;
        int status, vote;
        long lastKill, lastSpawn, outOfBounds;

        boolean here = true;
        boolean shipChangeWarn, outsideWarn;
        
        static final int SPEC = 0;
        static final int IN = 1;
        static final int OUT = 2;
        static final int LAGOUT = 3;
        
        public ElimPlayer(String n) {
            name = n;
            status = 0;
            wins = 0;
            losses = 0;
            streak = 0;
            multiKill = 0;
            vote = -1;
            lastKill = 0;
            lastSpawn = System.currentTimeMillis();
            outOfBounds = 0;
            lagouts = MAX_LAGOUT;
            Player p = ba.getPlayer(name);
            ship = p.getShipType();
            freq = p.getFrequency();
            shipChangeWarn = false;
            outsideWarn = false;
        }
        
        public void resetScore() {
            wins = 0;
            losses = 0;
            lagouts = MAX_LAGOUT;
            multiKill = 0;
        }
        
        public int lagout() {
            lagouts--;
            status = IN;
            return lagouts;
        }
        
        public void addKill() {
            wins++;
            if (System.currentTimeMillis() - lastKill < DOUBLE_KILL * 1000)
                multiKill();
            lastKill = System.currentTimeMillis();
            streak++;
            if (streak >= STREAK_INIT && (streak - STREAK_INIT) % STREAK_REPEAT == 0) {
                int i = (streak - STREAK_INIT) / STREAK_REPEAT;
                if (i >= sMessages.length)
                    i = sMessages.length - 1;
                ba.sendArenaMessage(name + " - " + sMessages[i] + "(" + streak + ":0)");
            }
            
        }
        
        public void multiKill() {
            multiKill++;
            switch (multiKill) {

            case 1:
                ba.sendArenaMessage(name + " - Double kill!", Tools.Sound.CROWD_OHH);
                break;
            case 2:
                ba.sendArenaMessage(name + " - Triple kill!", Tools.Sound.CROWD_GEE);
                break;
            case 3:
                ba.sendArenaMessage(name + " - Quadruple kill!", Tools.Sound.INCONCEIVABLE);
                break;
            case 4:
                ba.sendArenaMessage(name + " - Quintuple kill!", Tools.Sound.SCREAM);
                break;
            case 5:
                ba.sendArenaMessage(name + " - Sextuple kill!", Tools.Sound.CRYING);
                break;
            case 6:
                ba.sendArenaMessage(name + " - Septuple kill!", Tools.Sound.GAME_SUCKS);
                break;
            }
        }
        
        public void addDeath() {
            losses++;
            streak = 0;
        }
        
        public void removeKill() {
            wins--;
        }
        
        public void removeDeath() {
            losses--;
        }
        
        public boolean here() {
            here = !here;
            return here;
        }
        
        private void clearBorderInfo(){
            outOfBounds = 0;
            outsideWarn = false;
        }
    }

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
        
        private TimerTask task;
        private int state = 0;
        
        public Game() {
            state = 0;
        }

        private void next(long millis) {
            ba.cancelTask(task);
            task = new TimerTask() {
                public void run() {
                    next();
                    this.cancel();
                }
            };
            try {
                ba.scheduleTask(task, millis);
            } catch (Exception e) {
            }
        }

        private void next() {
            if (elimers.size() < cfg_minPlayers && state < GAME_IN_PROGRESS) {
                state = OFF_MODE;
                lastWinner = "-anonymous-";
                winStreak = 0;
            }
            switch (state) {
            case OFF_MODE:
                doWaitingForPlayers();
                break;
            case WAITING_FOR_PLAYERS:
                doVotingOnShip();
                break;
            case VOTING_ON_SHIP:
                doCountVotes();
                doVotingOnDeaths();
                break;
            case VOTING_ON_DEATHS:
                doCountVotes();
                if (cfg_gameType == ELIM || shipType != 2) {
                    doWaitingToStart();
                } else {
                    doVotingOnShrap();
                }
                break;
            case VOTING_ON_SHRAP:
                doCountVotes();
                doWaitingToStart();
                break;
            case WAITING_TO_START:
                doTenSeconds();
                break;
            case TEN_SECONDS:
                doGameInProgress();
                break;
            case GAME_IN_PROGRESS:
                doGameOver();
                break;
            case GAME_OVER:
                if (botMode == ON)
                    doWaitingForPlayers();
                else if (botMode == OFF) {
                    state = OFF_MODE;
                    lastWinner = "-anonymous-";
                    winStreak = 0;
                } else if (botMode == DISCONNECT) {
                    try {
                        ba.scheduleTask(new DieTask(ba.getBotName()), 500);
                    } catch (Exception e) {
                    }
                }
                break;
            }
        }
        
        public boolean isVoting(){
            if(state == VOTING_ON_SHIP || state == VOTING_ON_DEATHS || state == VOTING_ON_SHRAP)
                return true;
            else return false;
        }
        
        public boolean isInProgress(){
            if(state == TEN_SECONDS || state == GAME_IN_PROGRESS)
                return true;
            else return false;
        }
    }    

    
    private class DieTask extends TimerTask {
        String m_initiator;

        public DieTask( String name ) {
            super();
            m_initiator = name;
        }

        public void run() {
            ba.die( "!die initiated by " + m_initiator );
        }
    }
    
    public void handleDisconnect(){
        ba.cancelTasks();
    }
}
