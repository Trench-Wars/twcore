package twcore.bots.bwjsbot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.TreeMap;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FlagReward;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;
import twcore.core.lvz.Objset;
import twcore.core.util.Tools;
import twcore.core.util.Spy;

/**
 * A bot for automated-hosting of:
 * - base
 * - wbduel
 * - javduel
 * - spidduel
 * 
 * @author fantus
 * 
 */
public class bwjsbot extends SubspaceBot {
    /* Configuration */
    private BWJSConfig cfg;
    
    /* Lists */
    private ArrayList<String> listAlert;
    private ArrayList<String> listNotplaying;
    private ArrayList<String> listEnoughUsage;
    
    /* Locks */
    private boolean lastGame;
    private boolean lockArena;
    private boolean lockZoner;
    
    /* Racism Watcher */
    private Spy racismWatcher;
    
    /* States */
    private int state;
    private static final int OFF = -1;
    private static final int WAITING_FOR_CAPS = 0;
    private static final int ADDING_PLAYERS = 1;
    private static final int PRE_GAME = 2;
    private static final int GAME_IN_PROGRESS = 3;
    private static final int GAME_OVER = 4;
    
    /* Teams */
    private BWJSTeam[] team;
    private static final int ONE = 0;
    private static final int TWO = 1;
    
    /* Timers */
    private AddingTimer addingTimer;
    private TimerTask announceMVPTimer;
    private CapTimer capTimer;
    private TimerTask capTimerONE;
    private TimerTask capTimerTWO;
    private FiveSecondTimer fiveSecondTimer;
    private GameTimer gameTimer;
    private TimerTask newGameTimer;
    private UpdateTimer updateTimer;
    private StartGameTimer startGameTimer;
    private PreGameTimer preGameTimer;
    private ZonerTimer zonerTimer;
    
    /* Other */
    private Objset scoreboard;
    private String enoughUsageCurrentName;
    
    /* Some Constants */
    private static final int ZONER_WAIT_TIME = 15;
    private static final int FREQ_SPEC = 9999;
    private static final int FREQ_NOTPLAYING = 666;
    
    //Ship states
    private static final int IN = 0;
    private static final int LAGOUT = 1;
    private static final int LAGOUT_OUT = 2; //Still sub-able, but out
    private static final int OUT_BUT_SUBABLE = 3;
    private static final int SUBBED = 4;
    private static final int OUT = 5;
    
    //Game Types
    private static final int BASE = 1;
    private static final int WBDUEL = 2;
    private static final int JAVDUEL = 3;
    private static final int SPIDDUEL = 4;
    
    //Time between lagout use
    private static final long LAGOUT_TIME = 10 * Tools.TimeInMillis.SECOND;
    
    public bwjsbot(BotAction botAction) {
        super(botAction);
        initialize();
    }
    
    private void initialize() {
        /* Initial state */
        state = OFF;
        
        /* Lists */
        listAlert = new ArrayList<String>();
        listEnoughUsage = new ArrayList<String>();
        listNotplaying = new ArrayList<String>();
        listNotplaying.add(m_botAction.getBotName().toLowerCase());
        
        
        /* Spy */
        racismWatcher = new Spy(m_botAction);
        
        /* Zoner */
        lockZoner = false;
        
        /* Configuration */
        cfg = new BWJSConfig();
        
        /* LVZ */
        scoreboard = m_botAction.getObjectSet();
        
        /* Teams */
        team = new BWJSTeam[2];
        team[ONE] = new BWJSTeam(ONE);
        team[TWO] = new BWJSTeam(TWO);
        
        /* Other */
        lastGame = false;
        
        requestEvents();        
    }
    
    /* Handle Events */
    private void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.ARENA_JOINED);
        req.request(EventRequester.FLAG_CLAIMED);
        req.request(EventRequester.FLAG_REWARD);
        req.request(EventRequester.FREQUENCY_CHANGE);
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(EventRequester.LOGGED_ON);
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.PLAYER_DEATH);
        req.request(EventRequester.PLAYER_ENTERED);
        req.request(EventRequester.PLAYER_LEFT);
        req.request(EventRequester.PLAYER_POSITION);
        req.request(EventRequester.WEAPON_FIRED);
    }
    
    public void handleEvent(ArenaJoined event) {
        //set ReliableKills 1 (*relkills 1) to make sure that the bot receives every packet
        m_botAction.setReliableKills(1);
        
        // Join Chats
        m_botAction.sendUnfilteredPublicMessage("?chat=" + cfg.chats);
        
        /*
         * Autostart
         */
        start();
    }
    
    public void handleEvent(FlagClaimed event) {
        if (state == GAME_IN_PROGRESS) {
            if (getTeamNumber(m_botAction.getPlayerName(event.getPlayerID())) == ONE) {
                team[TWO].flagLost();
                team[ONE].flagClaimed(event);
            } else { 
                team[ONE].flagLost();
                team[TWO].flagClaimed(event);
            }
        }
    }
    
    public void handleEvent(FlagReward event) {
        if (state == GAME_IN_PROGRESS) {
            if (event.getFrequency() == team[ONE].frequency)
                team[ONE].flagReward(event.getPoints());
            else
                team[TWO].flagReward(event.getPoints());
        }
    }
    
    public void handleEvent(FrequencyChange event) {
        if (state >= ADDING_PLAYERS && state < GAME_OVER) {
            String name = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
            int teamNumber = getTeamNumber(name);
            
            if (teamNumber != -1) {
                if (team[teamNumber].players.containsKey(name)) {
                    if (team[teamNumber].players.get(name).p_state == IN &&
                            event.getFrequency() != team[teamNumber].frequency)
                        team[teamNumber].players.get(name).lagout();
                }
            }
        }
    }
    
    public void handleEvent(FrequencyShipChange event) {
        if (state >= ADDING_PLAYERS && state < GAME_OVER && event.getShipType() == Tools.Ship.SPECTATOR) {
            String name = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
            int teamNumber = getTeamNumber(name);
            
            if (teamNumber != -1) {
                if (team[teamNumber].players.containsKey(name)) {
                    if (team[teamNumber].players.get(name).p_state == IN && 
                            event.getFrequency() != team[teamNumber].frequency)
                        team[teamNumber].players.get(name).lagout();
                }
            }
        }
    }
    
    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(cfg.arena);
        
        m_botAction.setMessageLimit(10);
    }
    
    public void handleEvent(Message event) {
        /* Racism Watcher */
        racismWatcher.handleEvent(event);
        
        /* Arena Lock-O-Matic */
        if (event.getMessageType() == Message.ARENA_MESSAGE) {
            String message = event.getMessage();
            if (message.equals("Arena UNLOCKED") && lockArena)
                m_botAction.toggleLocked();
            if (message.equals("Arena LOCKED") && !lockArena)
                m_botAction.toggleLocked();
            

            //Feed the enough usage list
            if (cfg.capUsage != 0) {
                if (message.startsWith("IP:"))
                    enoughUsageCurrentName = message.split("  ")[3].substring(10);
                if (message.startsWith("TIME:"))
                    retrieveUsage(message);
            }
        }
        
        /* Handle Commands */
        if (event.getMessageType() == Message.PRIVATE_MESSAGE)
            handleCommand(event);
    }
    
    public void handleEvent(PlayerDeath event) {
        if (state == GAME_IN_PROGRESS) {
            String killeeName = m_botAction.getPlayerName(event.getKilleeID()).toLowerCase();
            String killerName = m_botAction.getPlayerName(event.getKillerID()).toLowerCase();
            
            if (team[ONE].players.containsKey(killeeName)) 
                team[ONE].players.get(killeeName).killed();
            else if (team[TWO].players.containsKey(killeeName))
                team[TWO].players.get(killeeName).killed();
                        
            if (team[ONE].players.containsKey(killerName)) 
                team[ONE].players.get(killerName).killer(event);
            else if (team[TWO].players.containsKey(killerName))
                team[TWO].players.get(killerName).killer(event);
        }
    }
    
    public void handleEvent(PlayerEntered event) {
        String name = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
        
        //Feed the list with people who have enough usage to get captain
        if (!listEnoughUsage.contains(name) && cfg.capUsage != 0)
            m_botAction.sendUnfilteredPrivateMessage(event.getPlayerID(), "*info");
        
        if (state > OFF) {
            
            //Welcome incoming player and send status message
            m_botAction.sendPrivateMessage(name, "Welcome to " + cfg.gameTypeString);
            cmd_status(name);
            
            //Set player to the correct freq
            if (listNotplaying.contains(name)) {
                m_botAction.setFreq(name, FREQ_NOTPLAYING);
                m_botAction.sendPrivateMessage(name, "You are currently listed as not playing. " +
                		"Message me with !notplaying if you want to be able to get picked/subbed in again.");
            }
            
            //A captain has returned! Cancel the remove task
            if (team[ONE].captainName.equalsIgnoreCase(name))
                m_botAction.cancelTask(capTimerONE);
            else if (team[TWO].captainName.equalsIgnoreCase(name))
                m_botAction.cancelTask(capTimerTWO);
        }
    }
    
    public void handleEvent(PlayerLeft event) {
        if (state >= ADDING_PLAYERS && state < GAME_OVER) {
            String name = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
            final int teamNumber = getTeamNumber(name);
            
            //If player was in neither of the teams, do nothing
            if (teamNumber == -1)
                return;
            
            //Player lagged out
            if (team[teamNumber].players.containsKey(name)) {
                if(team[teamNumber].players.get(name).p_state == IN)
                    team[teamNumber].players.get(name).lagout();
            }
            
            /*
             * Captain lagged out
             * Task: remove player as captain after 1 minute; notify arena
             */
            if (isCaptain(name)) {
                if (state == ADDING_PLAYERS) {
                    m_botAction.sendArenaMessage("NOTICE: The captain of " +
                            team[teamNumber].name + " has left the arena. Type !cap to claim captain.");
                    team[teamNumber].removeCap();
                } else if (state == GAME_IN_PROGRESS) {
                    m_botAction.sendOpposingTeamMessageByFrequency(teamNumber, "NOTICE: The captain of your team has " +
                    		"left the arena. Type !cap to claim captain.");
                    team[teamNumber].removeCap();
                } else { 
                    
                    m_botAction.sendArenaMessage("NOTICE: The captain of " +
                            team[teamNumber].name + " has left the arena. " +
                            team[teamNumber].captainName + " will get removed as captain after 1 minute.");
                    
                    if (teamNumber == ONE) {
                        capTimerONE = new TimerTask() {
                            public void run() {
                                team[teamNumber].removeCap();
                            }
                        };
                        m_botAction.scheduleTask(capTimerONE, Tools.TimeInMillis.MINUTE);
                    } else if (teamNumber == TWO) {
                        capTimerTWO = new TimerTask() {
                            public void run() {
                                team[teamNumber].removeCap();
                            }
                        };
                        m_botAction.scheduleTask(capTimerTWO, Tools.TimeInMillis.MINUTE);
                    }
                }
            }
        }
    }
    
    public void handleEvent(PlayerPosition event) {
        /*
         * Warp players back to their safe during pre game
         */
        if (state == PRE_GAME) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            int teamNumber = getTeamNumber(name);
            int x_coord = event.getXLocation() / 16;
            int y_coord = event.getYLocation() / 16;
            
            if (teamNumber == ONE) {
                if (x_coord != cfg.warpSpots[0] || y_coord != cfg.warpSpots[1])
                    m_botAction.warpTo(name, cfg.warpSpots[0], cfg.warpSpots[1]);
            } else if (teamNumber == TWO) {
                if (x_coord != cfg.warpSpots[2] || y_coord != cfg.warpSpots[3])
                    m_botAction.warpTo(name, cfg.warpSpots[2], cfg.warpSpots[3]);
            }
        } 
    }
    
    public void handleEvent(WeaponFired event) {
        if (state == GAME_IN_PROGRESS) {
            String name = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
            int teamNumber = getTeamNumber(name);
            
            //Repel fired
            if (event.getWeaponType() == WeaponFired.WEAPON_REPEL)
                team[teamNumber].players.get(name).repelUsed();
        }
    }
    
    /* Handle Commands */
    private void handleCommand(Message event) {
        String message = event.getMessage().toLowerCase();
        String messager = m_botAction.getPlayerName(event.getPlayerID());
        
        //Captain commands
        if (isCaptain(messager)) {
            if (cfg.gameType == BASE) {
                if (message.startsWith("!change "))
                    cmd_change(event);
                else if (message.startsWith("!switch "))
                    cmd_switch(event);
            }
            
            if (message.startsWith("!add"))
                cmd_add(event);
            else if (message.equals("!ready"))
                cmd_ready(event);
            else if (message.startsWith("!remove "))
                cmd_remove(event);
            else if (message.equals("!removecap"))
                cmd_removecap(event);
            else if (message.startsWith("!sub "))
                cmd_sub(event);
        }
        
        //Player commands
        if (message.startsWith("!cap"))
            cmd_cap(event);
        else if (message.equals("!help"))
            cmd_help(event);
        else if (message.equals("!lagout"))
            cmd_lagout(event);
        else if (message.equals("!return"))
            cmd_lagout(event);
        else if (message.equals("!list"))
            cmd_list(event);
        else if (message.equals("!myfreq"))
            cmd_myfreq(event);
        else if (message.equals("!mvp"))
            cmd_mvp(event);
        else if (message.equals("!rating"))
            cmd_rating(event);
        else if (message.startsWith("!rating "))
            cmd_ratingPlayer(event);
        else if (message.equals("!score"))
            cmd_score(event);
        else if (message.startsWith("!score "))
            cmd_scorePlayer(event);
        else if (message.equals("!notplaying"))
            cmd_notplaying(event);
        else if (message.equals("!status"))
            cmd_status(messager);
        else if (message.equals("!subscribe"))
            cmd_subscribe(event);
        
        //Staff Commands
        if (m_botAction.getOperatorList().isER(messager)) {
            if (message.equals("!start"))
                cmd_start(event);
            else if (message.equals("!stop"))
                cmd_stop(event);
            else if (message.startsWith("!setcaptain "))
                cmd_setCaptain(event);
        }
        
        if (m_botAction.getOperatorList().isModerator(messager)) {
            if (message.equals("!die"))
                m_botAction.die();
            else if (message.equals("!off"))
                cmd_off(event);
        }
    }
    
    private void cmd_add(Message event) {
        if (state > OFF && state < GAME_OVER) {
            String messager = m_botAction.getPlayerName(event.getPlayerID());
            int teamNumber = getTeamNumber(messager);
            
            //Specify player
            if (event.getMessage().length() < 5) {
                m_botAction.sendPrivateMessage(messager, "Error: !add <player>");
                return;
            }
            
            String[] message = event.getMessage().substring(5).split(":");
            Player player = m_botAction.getFuzzyPlayer(message[0]);
            int shipType = cfg.defaultShipType;
            
            //Check if ship type is specified
            if (message.length > 1) {
                try {shipType = Integer.parseInt(message[1]);} catch (Exception e) {};
                if (shipType < 0 || shipType > 8)
                    shipType = cfg.defaultShipType;
            }
            
            //Adding players during PRE_GAME is not allowed
            if (state == PRE_GAME) {
                m_botAction.sendPrivateMessage(messager, "Not possible to add a player at this point of the game." +
                " Wait until the game has started before adding more players.");
                return;
            }
            
            //Check if its his or her turn to pick
            if (state == ADDING_PLAYERS) {
                if (!team[teamNumber].pickingTurn) {
                    m_botAction.sendPrivateMessage(messager, "Not your turn to pick!");
                    return;
                }
            }
            
            //Adding players is allowed in this state, doing further checks
            if (state == GAME_IN_PROGRESS || state == ADDING_PLAYERS) {
                //Check if player is found
                if (player == null) {
                    m_botAction.sendPrivateMessage(messager, "Unknown player, please try again.");
                    return;
                }
                
                String playerName = player.getPlayerName().toLowerCase();
                
                //Check if player is a bot
                if (m_botAction.getOperatorList().isBotExact(playerName)) {
                    m_botAction.sendPrivateMessage(messager, "Bots don't play.");
                    return;
                }
                    
                
                //Check if player is on the not playing list
                if (listNotplaying.contains(playerName)) {
                    m_botAction.sendPrivateMessage(messager, "Cannot add, player is set to not playing.");
                    return;
                }
                
                //Check if maximum player amount is reached
                if (team[teamNumber].sizeIN() == cfg.maxPlayers) {
                    m_botAction.sendPrivateMessage(messager, "Max number of players on your team already reached.");
                    return;
                }
                
                //Check if player is already on the team
                //With exception of BASE games where a subbed player can be re-added.
                if (team[teamNumber].players.containsKey(playerName)) {
                    if (team[teamNumber].players.get(playerName).p_state < OUT_BUT_SUBABLE || 
                            cfg.gameType != BASE) {
                        m_botAction.sendPrivateMessage(messager, "Player already on your team, check !list.");
                        return;
                    }
                }
                
                //Check if player is already on the other team
                if (team[otherTeamNumber(teamNumber)].players.containsKey(playerName)) {
                    m_botAction.sendPrivateMessage(messager, "Player already on the other team.");
                    return;
                }
                
                //Check if player is a captain on the other team
                if (playerName.equalsIgnoreCase(team[otherTeamNumber(teamNumber)].captainName)) {
                    m_botAction.sendPrivateMessage(messager, "Cannot add the captain of the other team");
                    return;
                }
                
                //Check if the ship type max is reached
                if (team[teamNumber].ships(shipType) >= cfg.maxShips[shipType] && cfg.maxShips[shipType] != -1) {
                    m_botAction.sendPrivateMessage(messager, 
                            "Could not add this player as " + Tools.shipName(shipType) +
                            ", team already reached the maximum number of " + Tools.shipName(shipType) + "s allowed.");
                    return;
                }                
                
                /* All checks done */
                team[teamNumber].addPlayer(player, shipType);
                
                //Toggle turn
                if (state == ADDING_PLAYERS) {
                    team[teamNumber].pickingTurn = false;
                    determineNextPick();
                }
            }
        }
    }
    
    private void cmd_cap(Message event) {
        if (state > OFF) {
            String messager = m_botAction.getPlayerName(event.getPlayerID());
            int teamNumber = getTeamNumber(messager);
            String message = event.getMessage();
            
            //Check for people who don't have enough usage to claim captain
            if (!listEnoughUsage.contains(messager.toLowerCase())  && cfg.capUsage != 0) {
                if (team[ONE].captainName.equals("[nobody]") || team[TWO].captainName.equals("[nobody]")) {
                    m_botAction.sendPrivateMessage(messager, "You need atleast " + cfg.capUsage + 
                            " hours of usage in order to claim captain.");
                }
                msgCaps(messager);
                return;
            }
                
            
            //People on not playing list cannot get captain spot.
            if (listNotplaying.contains(messager.toLowerCase())) {
                msgCaps(messager);
                return;
            }
            
            //Captain spots already taken
            if (!team[ONE].captainName.equals("[nobody]") && !team[TWO].captainName.equals("[nobody]")) {
                msgCaps(messager);
                return;
            }
            
            //Messager already on one of the teams
            if (teamNumber != -1) {
                if (!team[teamNumber].captainName.equals("[nobody]")) {
                    msgCaps(messager);
                } else {
                    //Team is in need of a captain!
                    setCaptain(messager, teamNumber);
                }
                return;
            }
            
            //Messager on neither of the teams
            if (teamNumber == -1) {
                
                /*
                 * Set teamNumber to the number of the team that is in need of a captain
                 * Additional check if the messager is not already a captain. (not needed though)
                 */
                if (team[ONE].captainName.equals("[nobody]") && !team[TWO].captainName.equalsIgnoreCase(messager)) 
                    teamNumber = ONE;
                else if (team[TWO].captainName.equals("[nobody]") && !team[ONE].captainName.equalsIgnoreCase(messager))
                    teamNumber = TWO;
                else {
                    msgCaps(messager);
                    return;
                }
                
                if (state == WAITING_FOR_CAPS) {
                    if (message.length() > 5 && !racismWatcher.isRacist(message))
                        team[teamNumber].name = message.substring(5);
                    m_botAction.cancelTask(capTimer);
                    capTimer = new CapTimer();
                    m_botAction.scheduleTask(capTimer, Tools.TimeInMillis.MINUTE);
                }
                setCaptain(messager, teamNumber);
                return;
            }
        }
    }
    
    private void cmd_change(Message event) {
        if ((state == ADDING_PLAYERS || state == GAME_IN_PROGRESS) && cfg.gameType == BASE) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            int teamNumber = getTeamNumber(name);
            int shipType;
            
            //Specify players
            if (event.getMessage().length() < 8) {
                m_botAction.sendPrivateMessage(name, "Error: !change <player1>:<#shiptype>");
                return;
            }
            
            String[] message = event.getMessage().substring(8).split(":");
            
            //Check if command is properly used
            if (message.length < 2) {
                m_botAction.sendPrivateMessage(name, "Error: !change <player1>:<#shiptype>");
                return;
            }
            
            BWJSPlayer player = team[teamNumber].searchPlayer(message[0]);
            
            //Check if player could be found
            if (player == null) {
                m_botAction.sendPrivateMessage(name, "Unknown player");
                return;
            }
            
            //Check if player is already out
            if (!(player.p_state < OUT_BUT_SUBABLE)) {
                m_botAction.sendPrivateMessage(name, "Cannot change ship of a player that is already out.");
                return;
            }
            
            //Check if the shipType is correct
            try { shipType = Integer.parseInt(message[1]);} catch (Exception e) { 
                m_botAction.sendPrivateMessage(name, "Unknown Ship");
                return;
            }
            
            if (shipType < 1 || shipType > 8) {
                m_botAction.sendPrivateMessage(name, "Unknown Ship");
                return;
            }
            
            //Check if the change is allowed
            if (team[teamNumber].ships(shipType) >= cfg.maxShips[shipType] && cfg.maxShips[shipType] != -1) {
                m_botAction.sendPrivateMessage(name, 
                        "Could not change this player to " + Tools.shipName(shipType) +
                        ", team already reached the maximum number of " + Tools.shipName(shipType) + "s allowed.");
                return;
            }
            
            //Check if shiptype is already the same as the players current shiptype
            if (player.p_currentShip == shipType) {
                m_botAction.sendPrivateMessage(name, "Player is already in that type of ship.");
                return;
            }
            
            player.change(shipType);
        }
    }
    
    private void cmd_help(Message event) {
        String name = m_botAction.getPlayerName(event.getPlayerID());
        ArrayList<String> help = new ArrayList<String>();
        if (state == WAITING_FOR_CAPS) {
            help.add("!cap                      -- Become captain of a team!");
            help.add("!cap <teamname>           -- Become captain and set your own teamname!");
            if (isCaptain(name))
                help.add("!removecap                -- Removes you as a captain");
        } else if (state >= ADDING_PLAYERS) {
            if (isCaptain(name)) {
                help.add("!add <player>             -- Adds player");
                if (cfg.gameType == BASE)
                    help.add("!add <player>:<ship>      -- Adds player in the specified ship");
            }
            help.add("!cap                      -- Become captain of a team / shows current captains!");
            if (cfg.gameType == BASE && isCaptain(name))
                help.add("!change <player>:<ship>   -- Sets the player in the specified ship");
            help.add("!lagout                   -- Puts you back into the game if you have lagged out");
            help.add("!list                     -- Lists all players on this team");
            help.add("!myfreq                   -- Puts you on your team's frequency");
            help.add("!mvp                      -- Displays the current mvp");
            help.add("!rating <player>          -- Displays your/<playername> current rating");
            help.add("!score <player>           -- Displays your/<playername> current score");
            if (state == ADDING_PLAYERS && isCaptain(name)) { 
                help.add("!ready                    -- Use this when you're done setting your lineup");
                help.add("!remove <player>          -- Removes specified player)");
            }
            if (isCaptain(name)) {
                help.add("!removecap                -- Removes you as a captain");
                help.add("!sub <playerA>:<playerB>  -- Substitutes <playerA> with <playerB>");
                if (cfg.gameType == BASE)
                    help.add("!switch <player>:<player> -- Exchanges the ship of both players");
            }
        }
        help.add("!status                   -- Display status and score");
        if (state != OFF) {
            help.add("!notplaying               -- Toggles not playing mode");
            help.add("!subscribe                -- Toggles alerts in private messages");
        }
        
        if (m_botAction.getOperatorList().isModerator(name)) {
            help.add("MOD commands:");
            help.add("!off                      -- stops the bot after the current game");
            help.add("!die                      -- disconnects the bot");
        }
        
        if (m_botAction.getOperatorList().isER(name)) {
            help.add("ER commands:");
            help.add("!start                            -- starts the bot");
            help.add("!stop                             -- stops the bot");
            help.add("!setcaptain <teamname>:<player>   -- Sets <player> as captain for <teamname>");
        }
        
        String[] spam = help.toArray(new String[help.size()]);
        m_botAction.privateMessageSpam(name, spam);
    }
    
    private void cmd_lagout(Message event) {
        if (state == ADDING_PLAYERS || state == GAME_IN_PROGRESS) {
            String name = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
            int teamNumber = getTeamNumber(name);
            long currentTime = System.currentTimeMillis();
            long lagoutTime;
            
            //Check if player is in
            if (teamNumber == -1)
                return;
            
            //Extra check, player could be a captain who is not playing
            if (!team[teamNumber].players.containsKey(name))
                return;
            
            if (team[teamNumber].players.get(name).p_state == LAGOUT ||
                    team[teamNumber].players.get(name).p_state == LAGOUT_OUT) {
                //Check if enough time has passed
                lagoutTime = team[teamNumber].players.get(name).p_lagoutTime;
                if ((currentTime - lagoutTime) < LAGOUT_TIME) {
                    m_botAction.sendPrivateMessage(name, 
                            "You must wait for " + (LAGOUT_TIME - (currentTime - lagoutTime))/Tools.TimeInMillis.SECOND
                            + " more seconds before you can return into the game.");
                    return;
                }    
                team[teamNumber].players.get(name).lagin();
            }
        }
    }
    
    private void cmd_list(Message event) {
        if (state >= ADDING_PLAYERS && state < GAME_OVER) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            int teamNumber = getTeamNumber(name);
            ArrayList<String> list = new ArrayList<String>();
            if (teamNumber == -1) {
                for (int i = 0; i < 2; i++) {
                    list.add(team[i].name + " (captain: " + team[i].captainName + ")");
                    list.add(Tools.formatString("Name:", 23) + " - " +
                            Tools.formatString("Ship:", 10) + " - " + "Status:");
                    for (BWJSPlayer p : team[i].players.values()) 
                        list.add(Tools.formatString(p.p_name, 23) + " - " 
                                + Tools.formatString(Tools.shipName(p.p_currentShip), 10) + " - " + p.getStatus());
                    list.add("`");
                }
            } else {
                list.add(team[teamNumber].name + " (captain: " + team[teamNumber].captainName + ")");
                list.add(Tools.formatString("Name:", 23) + " - " +
                        Tools.formatString("Ship:", 10) + " - " + "Status:");
                for (BWJSPlayer p : team[teamNumber].players.values()) 
                    list.add(Tools.formatString(p.p_name, 23) + " - " 
                            + Tools.formatString(Tools.shipName(p.p_currentShip), 10) + " - " + p.getStatus());
            }
            
            String[] spam = list.toArray(new String[list.size()]);
            m_botAction.privateMessageSpam(name, spam);
        }
    }
    
    private void cmd_mvp(Message event) {
        if (state == GAME_IN_PROGRESS) {
           m_botAction.sendPrivateMessage(event.getPlayerID(), "Current MVP: " + getMVP());
        }
    }
    
    private void cmd_myfreq(Message event) {
        if (state > OFF) {
            String name = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
            int teamNumber = getTeamNumber(name);
            
            //Check if player is on either of the teams
            if (teamNumber == -1)
                return;
            
            if (team[teamNumber].players.containsKey(name)) {
                if (team[teamNumber].players.get(name).p_state != IN)
                    m_botAction.setFreq(name, team[teamNumber].frequency);
            } else if (team[teamNumber].captainName.equalsIgnoreCase(name))
                m_botAction.setFreq(name, team[teamNumber].frequency);
        }
    }
    
    private void cmd_notplaying(Message event) {
        if (state != OFF) {
            String name = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
            int teamNumber = getTeamNumber(name);
            
            if (listNotplaying.contains(name)) {
                listNotplaying.remove(name);
                m_botAction.sendPrivateMessage(name, "You have been removed from the not playing list.");
                m_botAction.setShip(name, 1);
                m_botAction.specWithoutLock(name);
                return;
            }
            
            listNotplaying.add(name);
            m_botAction.sendPrivateMessage(name, "You have been added to the not playing list. " +
                    "(Captains will be unable to add or sub you in.)");
            m_botAction.specWithoutLock(name);
            m_botAction.setFreq(name, FREQ_NOTPLAYING);
            
            if (teamNumber == -1)
                return;
            
            if (isCaptain(name))
                team[teamNumber].removeCap();
            
            if (state == ADDING_PLAYERS) {
                if (team[teamNumber].players.containsKey(name)) {
                    m_botAction.sendArenaMessage(name + " has been removed from the game. (not playing)");
                    team[teamNumber].players.remove(name);
                }
            }
            
            if (state > ADDING_PLAYERS && state < GAME_OVER) {
                if (team[teamNumber].players.containsKey(name)) {
                    if (team[teamNumber].players.get(name).p_state < SUBBED) {
                        m_botAction.sendArenaMessage(name + " has been removed from the game. (not playing)");
                        team[teamNumber].players.get(name).out();
                        team[teamNumber].players.get(name).p_state = OUT_BUT_SUBABLE;
                    }
                }
            }
            
            m_botAction.setFreq(name, FREQ_NOTPLAYING);
        }
    }
    
    private void cmd_off(Message event) {
        String name = m_botAction.getPlayerName(event.getPlayerID());
        if (state == OFF)
            m_botAction.sendSmartPrivateMessage(name, "Bot already turned OFF");
        else if (state == WAITING_FOR_CAPS) {
            m_botAction.sendArenaMessage("Bot has been stopped by " + name);
            state = OFF;
            reset();
            unlockArena();
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Turning OFF after this game.");
            lastGame = true;
        }

    }
    
    private void cmd_rating(Message event) {
        if (state == GAME_IN_PROGRESS) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            int teamNumber = getTeamNumber(name);
            
            if (teamNumber == -1)
                return;
            
            if (team[teamNumber].players.containsKey(name)) {
                int rating = team[teamNumber].players.get(name).getRating();
                m_botAction.sendPrivateMessage(event.getPlayerID(), "Current Rating: " + rating);
            }
         }
    }
    
    private void cmd_ratingPlayer(Message event) {
        if (state == GAME_IN_PROGRESS) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            String message = event.getMessage();
            
            if (message.length() < 9)
                return;
            
            BWJSPlayer p = team[ONE].searchPlayer(message.substring(8));
            
            if (p == null)
                p = team[TWO].searchPlayer(message.substring(8));
            
            if (p == null) {
                m_botAction.sendPrivateMessage(name, "Player not found");
                return;
            }
            
            m_botAction.sendPrivateMessage(event.getPlayerID(), "Current Rating of " + p.p_name + ": " + p.getRating());
        }
    }
    
    private void cmd_ready(Message event) {
        if (state == ADDING_PLAYERS) {
            team[getTeamNumber(m_botAction.getPlayerName(event.getPlayerID()))].ready();
            
            if (team[ONE].ready && team[TWO].ready)
                checkLineup();
        } 
    }
    
    private void cmd_remove(Message event) {
        if (state == ADDING_PLAYERS) {
            String messager = m_botAction.getPlayerName(event.getPlayerID());
            int teamNumber = getTeamNumber(messager);
            
            
            //Specify player
            if (event.getMessage().length() < 8) {
                m_botAction.sendPrivateMessage(messager, "Error: !remove <player>");
                return;
            }   
                
            String message = event.getMessage().substring(8);
            
            if (message.equals("")) {
                m_botAction.sendPrivateMessage(messager, "Specify a player.");
                return;
            }
            
            BWJSPlayer player = team[teamNumber].searchPlayer(message);
            
            if (player == null) {
                m_botAction.sendPrivateMessage(messager, "Player could not be found, try again.");
                return;
            }
            
            team[teamNumber].remove(player);
        }
    }
    
    private void cmd_removecap(Message event) {
        if (state > OFF && state < GAME_OVER)
            team[getTeamNumber(m_botAction.getPlayerName(event.getPlayerID()))].removeCap();
    }
    
    private void cmd_scorePlayer(Message event) {
        if (state == GAME_IN_PROGRESS) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            String message = event.getMessage();
            
            if (message.length() < 8)
                return;
            
            BWJSPlayer p = team[ONE].searchPlayer(message.substring(7));
            
            if (p == null)
                p = team[TWO].searchPlayer(message.substring(7));
            
            if (p == null) {
                m_botAction.sendPrivateMessage(name, "Player not found");
                return;
            }
            
            m_botAction.sendPrivateMessage(event.getPlayerID(), "Current Score of " + p.p_name + ": " + p.getScore());
        }
    }
    
    private void cmd_score(Message event) {
        if (state == GAME_IN_PROGRESS) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            int teamNumber = getTeamNumber(name);
            
            if (teamNumber == -1)
                return;
            
            if (team[teamNumber].players.containsKey(name)) {
                int score = team[teamNumber].players.get(name).getScore();
                m_botAction.sendPrivateMessage(event.getPlayerID(), "Current Score: " + score);
            }
         }
    }
    
    private void cmd_setCaptain(Message event) {
        String message = event.getMessage();
        String messager = m_botAction.getPlayerName(event.getPlayerID());
        
        if (message.length() < 13) {
            m_botAction.sendPrivateMessage(messager, "Error: !setcaptain <team>:<name>");
            return;
        }
        
        String[] messageSplit = event.getMessage().substring(12).split(":");
        
        //Check if command is properly used
        if (messageSplit.length < 2) {
            m_botAction.sendPrivateMessage(messager, "Error: !setcaptain <teamname>:<name>");
            return;
        }
        
        Player p = m_botAction.getFuzzyPlayer(messageSplit[1]);
        
        if (p == null) {
            m_botAction.sendPrivateMessage(messager, "Player not found");
            return;
        }
        
        if (team[ONE].name.equalsIgnoreCase(messageSplit[0]))        
            setCaptain(p.getPlayerName(), ONE);
        else if (team[TWO].name.equalsIgnoreCase(messageSplit[0]))        
            setCaptain(p.getPlayerName(), TWO);
        else
            m_botAction.sendPrivateMessage(messager, "Specify correct teamname");
    }
    
    private void cmd_start(Message event) {
        if (state == OFF)
            start();
        else
            m_botAction.sendPrivateMessage(event.getPlayerID(), "Bot already turned on");
    }
    
    private void cmd_status(String name) {
        if (state == OFF) {
            m_botAction.sendPrivateMessage(name, "Bot turned off, no games can be started at this moment.");
        } else if (state == WAITING_FOR_CAPS) {
            m_botAction.sendPrivateMessage(name, "A new game will start when two people message me with !cap");
        } else if (state == ADDING_PLAYERS) {
            m_botAction.sendPrivateMessage(name, "Teams: " + team[ONE].name + " vs. " + team[TWO].name + 
                    ". We are currently arranging lineups");
        } else if (state == PRE_GAME) {
            m_botAction.sendPrivateMessage(name, "Teams: " + team[ONE].name + " vs. " + team[TWO].name + 
                    ". We are currently starting the game");
        } else if (state == GAME_IN_PROGRESS) {
            m_botAction.sendPrivateMessage(name, "Game is in progress, " + (cfg.time - cfg.timeLeft / 60) +
                    " minutes played.");
            m_botAction.sendPrivateMessage(name, "Score " + team[ONE].name + " vs. " + team[TWO].name + ": " +
                    score());
        } else if (state == GAME_OVER) {
            m_botAction.sendPrivateMessage(name, "Teams: " + team[ONE].name + " vs. " + team[TWO].name + 
            ". We are currently ending the game");
        }
    }
    
    private void cmd_stop(Message event) {
        String messager = m_botAction.getPlayerName(event.getPlayerID());
        if (state != OFF) {
            m_botAction.sendArenaMessage("Bot has been stopped by " + messager);
            state = OFF;
            reset();
            unlockArena();
        }
        else
            m_botAction.sendPrivateMessage(messager, "Bot already turned off");
    }
    
    private void cmd_sub(Message event) {
        if (state == GAME_IN_PROGRESS) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            int teamNumber = getTeamNumber(name);
            
            //Specify players
            if (event.getMessage().length() < 5) {
                m_botAction.sendPrivateMessage(name, "Error: !sub <player1>:<player2>");
                return;
            }
            
            String[] message = event.getMessage().substring(5).split(":");
            
            //Check if command is properly used
            if (message.length < 2) {
                m_botAction.sendPrivateMessage(name, "Error: !sub <player1>:<player2>");
                return;
            }
            
            //Check if team has any substitutes left
            if (team[teamNumber].substitutesLeft == 0) {
                m_botAction.sendPrivateMessage(name, "Could not sub, you have 0 substitutes left.");
                return;
            }
            
            BWJSPlayer playerOne = team[teamNumber].searchPlayer(message[0]);
            
            //Check if player can be found
            if (playerOne == null) {
                m_botAction.sendPrivateMessage(name, "Player could not be found, try again.");
                return;
            }
            
            //Check if player is already out and thus cannot be subbed
            if (playerOne.p_state >= SUBBED) {
                m_botAction.sendPrivateMessage(name, "Cannot substitute a player that is already out.");
                return;
            }
            
            Player playerTwo = m_botAction.getFuzzyPlayer(message[1]);
            
            //Check if player two can be found
            if (playerTwo == null) {
                m_botAction.sendPrivateMessage(name, "substitute could not be found, try again.");
                return;
            }
            
            //Check if player is on the notplaying list
            if (listNotplaying.contains(playerTwo.getPlayerName().toLowerCase())) {
                m_botAction.sendPrivateMessage(name, "Error: " + playerTwo.getPlayerName() + " is set to not playing.");
                return;
            }
            
            //Check if subtitute is available
            if (team[otherTeamNumber(teamNumber)].players.containsKey(playerTwo.getPlayerName().toLowerCase()) || 
                    team[otherTeamNumber(teamNumber)].captainName.equalsIgnoreCase(playerTwo.getPlayerName())) {
                m_botAction.sendPrivateMessage(name, "Substitute is already on the other team.");
                return;
            }
            
            //Check if subtitute is available 
            if (team[teamNumber].players.containsKey(playerTwo.getPlayerName().toLowerCase())) {
                int tmp_state = team[teamNumber].players.get(playerTwo.getPlayerName().toLowerCase()).p_state;
                if (cfg.gameType != BASE || tmp_state < OUT_BUT_SUBABLE) {
                    m_botAction.sendPrivateMessage(name, "Substitute is/was already playing for your team.");
                    return;
                }
            }
            
            //Extra check if player should be subbed
            if (cfg.maxDeaths != 0 && playerOne.getDeaths() >= cfg.maxDeaths) {
                m_botAction.sendPrivateMessage(name, "Cannot sub a player that is already out.");
                return;
            }
            team[teamNumber].sub(playerOne, playerTwo);
        }
    }
    
    private void cmd_subscribe(Message event) {
        if (state != OFF) {
            String playerName = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
            if (listAlert.contains(playerName)) {
                listAlert.remove(playerName);
                m_botAction.sendPrivateMessage(playerName, "You have been removed from the alert list.");
            }
            else {
                listAlert.add(playerName);
                m_botAction.sendPrivateMessage(playerName, "You have been added to the alert list.");
            }
        }
    }
    
    private void cmd_switch(Message event) {
        if ((state == ADDING_PLAYERS || state == GAME_IN_PROGRESS) && cfg.gameType == BASE) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            int teamNumber = getTeamNumber(name);
            
            //Specify players
            if (event.getMessage().length() < 8) {
                m_botAction.sendPrivateMessage(name, "Error: !switch <player1>:<player2>");
                return;
            }
            
            String[] message = event.getMessage().substring(8).split(":");
            
            //Check if command is properly used
            if (message.length < 2) {
                m_botAction.sendPrivateMessage(name, "Error: !switch <player1>:<player2>");
                return;
            }
            
            BWJSPlayer playerOne = team[teamNumber].searchPlayer(message[0]);
            BWJSPlayer playerTwo = team[teamNumber].searchPlayer(message[1]);
            
            //Check if players can be found
            if (playerOne == null || playerTwo == null) {
                m_botAction.sendPrivateMessage(name, "Unknown players");
                return;
            }

            //Check if player is already out or subbed
            if (!(playerOne.p_state < OUT_BUT_SUBABLE) || !(playerTwo.p_state < OUT_BUT_SUBABLE)) {
                m_botAction.sendPrivateMessage(name, "Cannot switch a player that is already out.");
                return;
            }
            
            team[teamNumber].switchPlayers(playerOne, playerTwo);
        }
    }
    
    /* Game states */
    private void start() {
        lastGame = false;
        lockArena();
        lockDoors();
        setSpecAndFreq();
        
        startWaitingForCaps();
    }
    
    private void startWaitingForCaps() {
        reset();
        
        state = WAITING_FOR_CAPS;
        m_botAction.sendArenaMessage("A new game will start when two people message me with !cap -" + 
                m_botAction.getBotName(), Tools.Sound.BEEP2);
    }
    
    private void startAddingPlayers() {
        state = ADDING_PLAYERS;
        
        m_botAction.cancelTask(capTimer);
        
        addingTimer = new AddingTimer();
        
        
        if (cfg.gameType == BASE) {
            m_botAction.sendArenaMessage("Captains you have 10 minutes to set up your lineup correctly!", 
                    Tools.Sound.BEEP2);
            m_botAction.scheduleTask(addingTimer, 10 * Tools.TimeInMillis.MINUTE);
        } else {
            m_botAction.sendArenaMessage("Captains you have 5 minutes to set up your lineup correctly!", 
                    Tools.Sound.BEEP2);
            m_botAction.scheduleTask(addingTimer, 5 * Tools.TimeInMillis.MINUTE);
        }
        
        newGameAlert();
        
        determineNextPick();
    }
    
    private void startPreGame() {
        state = PRE_GAME;
        m_botAction.resetFlagGame();
        m_botAction.shipResetAll();
        m_botAction.scoreResetAll();
        
        team[ONE].warpTo(cfg.warpSpots[0], cfg.warpSpots[1]);
        team[TWO].warpTo(cfg.warpSpots[2], cfg.warpSpots[3]);
        
        m_botAction.showObject(cfg.objects[0]);
        
        fiveSecondTimer = new FiveSecondTimer();
        startGameTimer = new StartGameTimer();
        
        m_botAction.scheduleTask(fiveSecondTimer, 5 * Tools.TimeInMillis.SECOND);
        m_botAction.scheduleTask(startGameTimer, 10 * Tools.TimeInMillis.SECOND);
    }
    
    private void startGame() {
        state = GAME_IN_PROGRESS;
        
        scoreboard = m_botAction.getObjectSet();
        
        m_botAction.shipResetAll();
        m_botAction.scoreResetAll();
        
        team[ONE].warpTo(cfg.warpSpots[4], cfg.warpSpots[5]);
        team[TWO].warpTo(cfg.warpSpots[6], cfg.warpSpots[7]);
        
        m_botAction.showObject(cfg.objects[2]);
        m_botAction.sendArenaMessage("Go go go!!!", Tools.Sound.GOGOGO);
        
        cfg.timeLeft = cfg.time * 60;
        updateTimer = new UpdateTimer();
        m_botAction.scheduleTaskAtFixedRate(updateTimer, 2 * Tools.TimeInMillis.SECOND, Tools.TimeInMillis.SECOND);
        
        m_botAction.setTimer(cfg.time);
        gameTimer = new GameTimer();
        m_botAction.scheduleTask(gameTimer, Tools.TimeInMillis.MINUTE * cfg.time);
    }
    
    private void gameOver() {
        state = GAME_OVER;
        
        //Cancel all timers
        m_botAction.setTimer(0);
        m_botAction.cancelTask(gameTimer);
        m_botAction.cancelTask(updateTimer);
        m_botAction.cancelTask(capTimerONE);
        m_botAction.cancelTask(capTimerTWO);
        
        //Determine winner
        int teamNumber = -1;
        
        if (cfg.gameType == BASE) {
            if (team[ONE].flagTime >= cfg.timeTarget * 60)
                teamNumber = ONE;
            else if (team[TWO].flagTime >= cfg.timeTarget * 60)
                teamNumber = TWO;
        } else {
            if (team[ONE].isDead())
                teamNumber = TWO;
            else if (team[TWO].isDead())
                teamNumber = ONE;
        }
        
        m_botAction.sendArenaMessage("Result of " + team[ONE].name + " vs. " + team[TWO].name + ": " + score());
        
        displayScores();
        
        if (teamNumber != -1)
            m_botAction.sendArenaMessage(team[teamNumber].name + " wins the game!", Tools.Sound.HALLELUJAH);
        else 
            m_botAction.sendArenaMessage("A draw?!", Tools.Sound.GIBBERISH);
        
        announceMVPTimer = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage("MVP: " + getMVP() + "!", Tools.Sound.INCONCEIVABLE);
            }
        };
        
        newGameTimer = new TimerTask() {
            public void run() {
                startWaitingForCaps();
            }
        };
        
        m_botAction.scheduleTask(announceMVPTimer, 6 * Tools.TimeInMillis.SECOND);
        if (!lastGame)
            m_botAction.scheduleTask(newGameTimer, 15 * Tools.TimeInMillis.SECOND);
        else {
            TimerTask shutdownTimer = new TimerTask() {
                public void run() {
                    m_botAction.sendArenaMessage("Bot has been shutdown.", Tools.Sound.GAME_SUCKS);
                    reset();
                    unlockArena();
                }
            };
            m_botAction.scheduleTask(shutdownTimer, 8 * Tools.TimeInMillis.SECOND);
        }
    }
    
    /* Other commands */
    private void determineNextPick() {
        if (!team[ONE].pickingTurn && !team[TWO].pickingTurn) {
            int teamNumber = -1; 
            
            if (team[ONE].players.size() <= team[TWO].players.size())
                teamNumber = ONE;
            else if (team[TWO].players.size() < team[ONE].players.size())
                teamNumber = TWO;
            
            if (teamNumber != -1) {
                if (team[teamNumber].players.size() != cfg.maxPlayers) {
                    m_botAction.sendArenaMessage(team[teamNumber].captainName + " pick a player!", Tools.Sound.BEEP2);
                    team[teamNumber].pickingTurn = true;
                }
            }
        }
    }
    
    private void checkIfInBase() {
        for (BWJSTeam i : team) {
            for (BWJSPlayer p : i.players.values())
                p.checkIfInBase();
        }
    }
    
    private void checkLineup() {
        state = PRE_GAME;
        if (team[ONE].players.size() >= cfg.minPlayers && team[TWO].players.size() >= cfg.minPlayers) {
            m_botAction.cancelTask(addingTimer);
            m_botAction.sendArenaMessage("Lineups are ok! Game will start in 30 seconds!", Tools.Sound.CROWD_OOO);
            preGameTimer = new PreGameTimer();
            m_botAction.scheduleTask(preGameTimer, 30 * Tools.TimeInMillis.SECOND);
        } else {
            m_botAction.sendArenaMessage("Lineups are NOT ok! :( Game has been reset.", Tools.Sound.CROWD_GEE);
            startWaitingForCaps();
        }
    }
    
    private void displayScores() {
        ArrayList<String> spam = new ArrayList<String>();
        if (cfg.gameType == BASE) {
            spam.add(",---------------------------------+------+------+-----------+------+------+-----------+----.");
            spam.add("|                               K |    D |   TK |    Points |   FT |  TeK |    Rating | LO |");
        } else  if (cfg.gameType == JAVDUEL){
            spam.add(",---------------------------------+------+------+-----------+----.");
            spam.add("|                               K |    D |   TK |    Rating | LO |");
        } else {
            spam.add(",---------------------------------+------+-----------+----.");
            spam.add("|                               K |    D |    Rating | LO |");
        }
        
        spam.addAll(team[ONE].getScores());
        
        if (cfg.gameType == BASE)
            spam.add("+---------------------------------+------+------+-----------+------+------+-----------+----+");
        else if (cfg.gameType == JAVDUEL)
            spam.add("+---------------------------------+------+------+-----------+----+");
        else
            spam.add("+---------------------------------+------+-----------+----+");
        
        spam.addAll(team[TWO].getScores());
        
        if (cfg.gameType == BASE)
            spam.add("`---------------------------------+------+------+-----------+------+------+-----------+----'");
        else if (cfg.gameType == JAVDUEL)
            spam.add("`---------------------------------+------+------+-----------+----'");
        else
            spam.add("`---------------------------------+------+-----------+----'");
        
        m_botAction.arenaMessageSpam(spam.toArray(new String[spam.size()]));
    }
    
    private String getMVP() {
        String mvp = "";
        int highestRating = 0;
        
        for (BWJSTeam i : team) {
            for (BWJSPlayer p : i.players.values()) {
                if (highestRating < p.getRating()) {
                    highestRating = p.getRating();
                    mvp = p.p_name;
                }
            }   
        }
        
        return mvp;
    }
    
    private int getTeamNumber(String name) {
        String nameL = name.toLowerCase();
        if (team[ONE].players.containsKey(nameL) || team[ONE].captainName.equalsIgnoreCase(nameL))
            return ONE;
        else if (team[TWO].players.containsKey(nameL) || team[TWO].captainName.equalsIgnoreCase(nameL))
            return TWO;
        return -1;
    }
    
    private void retrieveUsage(String message) {
        try {
            int beginIndex = message.indexOf("Total:") + 6;
            int endIndex = message.indexOf("  Created:");
            int usage = Integer.parseInt(message.substring(beginIndex, endIndex).trim().split(":")[0]);
            
            if (usage > cfg.capUsage)
                listEnoughUsage.add(enoughUsageCurrentName.toLowerCase());
        } catch (Exception e) {}
    }
    
    /**
     * Check if playerID is captain or not
     * 
     * @param playerID int playerID
     * @return true: captain; false: not a captain;
     */
    private boolean isCaptain(String name) {
        //Check if "playerID" is on one of the teams
        if (getTeamNumber(name) == -1)
            return false;
        
        //Check if "playerID" is the captain
        if (team[getTeamNumber(name)].captainName.equalsIgnoreCase(name))
            return true;
        else
            return false;
    }
    
    private void lockArena() {
        lockArena = true;
        m_botAction.toggleLocked();
    }
    
    private void unlockArena() {
        lockArena = false;
        m_botAction.toggleLocked();
    }
    
    private void lockDoors() {
        m_botAction.setDoors(255);
    }
    
    private void msgCaps(String name) {
        m_botAction.sendPrivateMessage(name, team[ONE].captainName + " is captain of " + team[ONE].name + ".");
        m_botAction.sendPrivateMessage(name, team[TWO].captainName + " is captain of " + team[TWO].name + ".");
    }
    
    private void newGameAlert() {
        //Alert Chats
        for (int i = 1; i < 11; i++)
            m_botAction.sendChatMessage(i, "A game of " + cfg.gameTypeString + " is starting! Type ?go " +
                    m_botAction.getArenaName() + " to play.");
        
        //Alert Subscribers
        if (listAlert.size() > 0) {
            for (int i = 0; i < listAlert.size(); i++)
                m_botAction.sendSmartPrivateMessage(listAlert.get(i), "A game of " + cfg.gameTypeString +
                        " is starting! Type ?go " + m_botAction.getArenaName() + " to play.");
        }
        
        //Alert zoner, (max once every ZONER_WAIT_TIME (minutes))
        if (!lockZoner && cfg.allowZoner) {
            m_botAction.sendZoneMessage("A game of " + cfg.gameTypeString + " is starting! Type ?go " +
                    m_botAction.getArenaName() + " to play. -" + m_botAction.getBotName(), Tools.Sound.BEEP2);
            lockZoner = true;
            zonerTimer = new ZonerTimer();
            m_botAction.scheduleTask(zonerTimer, ZONER_WAIT_TIME * Tools.TimeInMillis.MINUTE);
        }
    }
    
    private int otherTeamNumber(int teamNumber) {
        if (teamNumber == ONE)
            return TWO;
        else if (teamNumber == TWO)
            return ONE;
        else
            return -1;
    }
    
    private void reset() {
        /* Reset */
        //Teams
        team[ONE].reset();
        team[TWO].reset();
        
        //Scoreboard
        scoreboard.hideAllObjects();
        m_botAction.setObjects();
        
        //Timers
        m_botAction.cancelTask(announceMVPTimer);
        m_botAction.cancelTask(addingTimer);
        m_botAction.cancelTask(capTimer);
        m_botAction.cancelTask(capTimerONE);
        m_botAction.cancelTask(capTimerTWO);
        m_botAction.cancelTask(fiveSecondTimer);
        m_botAction.cancelTask(gameTimer);
        m_botAction.cancelTask(updateTimer);
        m_botAction.cancelTask(startGameTimer);
        m_botAction.cancelTask(preGameTimer);
        m_botAction.cancelTask(newGameTimer);
        
        setSpecAndFreq();
    }
    
    private String score() {
        String score = "";
        if (cfg.timeTarget != 0) {
            String team1Minutes = String.format("%02d", (int)Math.floor( team[ONE].flagTime / 60.0 ));
            String team2Minutes = String.format("%02d", (int)Math.floor( team[TWO].flagTime / 60.0 ));
            String team1Seconds = String.format("%02d", (team[ONE].flagTime - 
                    (int)Math.floor( team[ONE].flagTime / 60.0 ) * 60));
            String team2Seconds = String.format("%02d", (team[TWO].flagTime - 
                    (int)Math.floor( team[TWO].flagTime / 60.0 ) * 60));
            
            score = team1Minutes + ":" + team1Seconds + " - " + team2Minutes + ":" + team2Seconds;  
        } else
            score = team[TWO].getDeaths() + " - " + team[ONE].getDeaths();
        
        return score;
    }
    
    private void setCaptain(String name, int teamNumber) {
        team[teamNumber].captainName = name;
        m_botAction.sendArenaMessage(name + " is assigned as captain for " +
                team[teamNumber].name, Tools.Sound.BEEP1);
        
        if (state == WAITING_FOR_CAPS) {
            if (!team[ONE].captainName.equalsIgnoreCase("[nobody]") && !team[TWO].captainName.equalsIgnoreCase("[nobody]"))
                startAddingPlayers();
        }
    }
    
    /**
     * Scoreboard copy/paste from matchbot code, with small adjustments.
     */
    private void updateScoreboard() {
        scoreboard.hideAllObjects();
        /*
         * Base
         */
        if (cfg.timeTarget != 0) {
            int team1Minutes = (int)Math.floor( team[ONE].flagTime / 60.0 );
            int team2Minutes = (int)Math.floor( team[TWO].flagTime / 60.0 );
            int team1Seconds = team[ONE].flagTime - team1Minutes * 60;
            int team2Seconds = team[TWO].flagTime - team2Minutes * 60;
            
            //Team 1
            scoreboard.showObject( 100 + team1Seconds % 10 );
            scoreboard.showObject( 110 + (team1Seconds - team1Seconds % 10)/10 );
            scoreboard.showObject( 130 + team1Minutes % 10 );
            scoreboard.showObject( 140 + (team1Minutes - team1Minutes % 10)/10 );

            //Team 2
            scoreboard.showObject( 200 + team2Seconds % 10 );
            scoreboard.showObject( 210 + (team2Seconds - team2Seconds % 10)/10 );
            scoreboard.showObject( 230 + team2Minutes % 10 );
            scoreboard.showObject( 240 + (team2Minutes - team2Minutes % 10)/10 );
            
            //Flag status
            if(team[ONE].flag) {
                scoreboard.showObject(740);
                scoreboard.showObject(743);
                scoreboard.hideObject(741);
                scoreboard.hideObject(742);
            } else if(team[TWO].flag) {
                scoreboard.showObject(741);
                scoreboard.showObject(742);
                scoreboard.hideObject(743);
                scoreboard.hideObject(740);
            } else {
                scoreboard.showObject(740);
                scoreboard.showObject(742);
                scoreboard.hideObject(741);
                scoreboard.hideObject(743);
            }
        }
        /* 
         * Wbduel, javduel, spidduel
         */
        else {
            String scoreTeam1 = "" + team[TWO].getDeaths();
            String scoreTeam2 = "" + team[ONE].getDeaths();
            
            for (int i = scoreTeam1.length() - 1; i > -1; i--)
                scoreboard.showObject(
                        Integer.parseInt("" + scoreTeam1.charAt(i)) + 100 + (scoreTeam1.length() - 1 - i) * 10);
            for (int i = scoreTeam2.length() - 1; i > -1; i--)
                scoreboard.showObject(
                        Integer.parseInt("" + scoreTeam2.charAt(i)) + 200 + (scoreTeam2.length() - 1 - i) * 10);
        }
        
        /*
         * Game Time Left
         */
        if (cfg.timeLeft >= 0) {
            int seconds = cfg.timeLeft % 60;
            int minutes = (cfg.timeLeft - seconds) / 60;
            scoreboard.showObject(730 + ((minutes - minutes % 10) / 10));
            scoreboard.showObject(720 + (minutes % 10));
            scoreboard.showObject(710 + ((seconds - seconds % 10) / 10));
            scoreboard.showObject(700 + (seconds % 10));
        }
        
        /*
         * Show Team Names
         */
        String n1 = team[ONE].name.toLowerCase();
        String n2 = team[TWO].name.toLowerCase();
        if (n1.equalsIgnoreCase("Freq 0"))
            n1 = "freq0";
        if (n2.equalsIgnoreCase("Freq 1"))
            n2 = "freq1";
        
        String s1 = "", s2 = "";

        for (int i = 0; i < n1.length(); i++)
            if ((n1.charAt(i) >= '0') && (n1.charAt(i) <= 'z') && (s1.length() < 5))
                s1 = s1 + n1.charAt(i);

        for (int i = 0; i < n2.length(); i++)
            if ((n2.charAt(i) >= '0') && (n2.charAt(i) <= 'z') && (s2.length() < 5))
                s2 = s2 + n2.charAt(i);

        for (int i = 0; i < s1.length(); i++) {
            int t = new Integer(Integer.toString(
                    ((s1.getBytes()[i]) - 97) + 30) + Integer.toString(i + 0)).intValue();
            if (t < -89) {
                t = new Integer(Integer.toString(((s1.getBytes()[i])) + 30) + Integer.toString(i + 0)).intValue();
                t -= 220;
            }
            scoreboard.showObject(t);
        }
        
        for (int i = 0; i < s2.length(); i++) {
            int t = new Integer(Integer.toString(
                    ((s2.getBytes()[i]) - 97) + 30) + Integer.toString(i + 5)).intValue();
            if (t < -89) {
                t = new Integer(Integer.toString(((s2.getBytes()[i])) + 30) + Integer.toString(i + 5)).intValue();
                t -= 220;
            }
            scoreboard.showObject(t);
        }
        
        //Display everything
        m_botAction.setObjects();
    }
    
    /**
     * Specs all the players in the arena and sets them to their frequency. The not playing players on the 
     * "not playing"-freq, the rest on the spectator-freq. 
     * 
     */
    private void setSpecAndFreq() {
        for (Iterator<Player> it = m_botAction.getPlayerIterator(); it.hasNext();) {
            Player i = it.next();
            int id = i.getPlayerID();
            int freq = i.getFrequency();
            if (i.getShipType() != Tools.Ship.SPECTATOR)
                m_botAction.specWithoutLock(id);
            if (listNotplaying.contains(i.getPlayerName().toLowerCase()) && freq != FREQ_NOTPLAYING)
                m_botAction.setFreq(id, FREQ_NOTPLAYING);
            else if (freq != FREQ_SPEC && !listNotplaying.contains(i.getPlayerName().toLowerCase())) {
                m_botAction.setShip(id, 1);
                m_botAction.specWithoutLock(id);
            }
        }
    }
    
    /* Game Classes */
    private class BWJSConfig {
        private BotSettings botSettings;
        private int gameType;
        private boolean allowZoner;
        private boolean announceShipType;
        private boolean inBase;
        private int capUsage;
        private int defaultShipType;
        private int maxDeaths;
        private int maxLagouts;
        private int maxPlayers;
        private int[] maxShips;
        private int maxSubs;
        private int minPlayers;
        private int[] objects;
        private int outOfBorderTime;
        private int time;
        private int timeLeft;
        private int timeTarget;
        private int[] warpSpots;
        private int yborder;
        private String arena;
        private String chats;
        private String gameTypeString; 
        
        private BWJSConfig() {
            botSettings = m_botAction.getBotSettings();
            int botNumber = m_botAction.getBotNumber();
            int tmpAnnounceShipCounter; 
            String[] maxShipsString;
            String[] objectsString;
            String[] warpSpotsString;
            
            //Arena
            arena = botSettings.getString("Arena" + botNumber);
            
            //Game Type
            gameTypeString = botSettings.getString("GameType" + botNumber);
            if (gameTypeString.equals("base"))
                gameType = BASE;
            else if (gameTypeString.equals("wbduel"))
                gameType = WBDUEL;
            else if (gameTypeString.equals("javduel"))
                gameType = JAVDUEL;
            else if (gameTypeString.equals("spidduel"))
                gameType = SPIDDUEL;
            else
                m_botAction.die();
            
            //Allow Zoner
            allowZoner = (botSettings.getInt("SendZoner" + botNumber) == 1);
            
            //Chats
            chats = botSettings.getString("Chats" + gameType);
            
            //Default Ship Type
            defaultShipType = botSettings.getInt("DefaultShipType" + gameType);
            
            //Max Deaths
            maxDeaths = botSettings.getInt("MaxDeaths" + gameType);
            
            //Max Lagouts
            maxLagouts = botSettings.getInt("MaxLagouts" + gameType);
            
            //Max Players
            maxPlayers = botSettings.getInt("MaxPlayers" + gameType);
            
            //Max Ships
            maxShips = new int[9];
            maxShipsString = botSettings.getString("MaxShips" + gameType).split(",");
            tmpAnnounceShipCounter = 0; //Counter for Announce Ship Type
            for (int i = Tools.Ship.WARBIRD; i <= maxShipsString.length; i++) {
                maxShips[i] = Integer.parseInt(maxShipsString[i - 1]);
                if (maxShips[i] != 0)
                    tmpAnnounceShipCounter++;
            }
            
            //Max Amount of Substitutes Allowed
            maxSubs = botSettings.getInt("MaxSubs" + gameType);
            
            //Announce Ship Type
            if (tmpAnnounceShipCounter > 1)
                announceShipType = true;
            else
                announceShipType = false;
            
            //Min Players
            minPlayers = botSettings.getInt("MinPlayers" + gameType);
            
            //LVZ Objects
            objectsString = botSettings.getString("Objects" + gameType).split(",");
            objects = new int[objectsString.length];
            for (int i = 0; i < objectsString.length; i++) 
                objects[i] = Integer.parseInt(objectsString[i]);
            
            //Time
            time = botSettings.getInt("Time" + gameType);

            //Time Target
            timeTarget = botSettings.getInt("TimeTarget" + gameType);
            
            //Warp Spots
            warpSpots = new int[8];
            warpSpotsString = botSettings.getString("WarpSpots" + gameType).split(",");
            for (int i = 0; i < warpSpotsString.length; i++)
                warpSpots[i] = Integer.parseInt(warpSpotsString[i]);
            
            //YBorder
            yborder = botSettings.getInt("Yborder" + gameType);
            if (yborder != -1)
                inBase = true;
            else
                inBase = false;
            outOfBorderTime = botSettings.getInt("OutOfBorderTime" + gameType);
            
            //Captain usage
            capUsage = botSettings.getInt("CaptainUsage" + gameType);
        }
    }
    
    private class BWJSPlayer {
        /* Variables */
        private String p_name;
        private int p_currentShip;
        private int p_state;
        private int p_maxDeaths;
        private int p_lagouts;
        private long p_lagoutTime;
        private int p_frequency;
        private int p_ships[][];
        private int p_outOfBorderTime;
        private boolean p_outOfBorderWarning;
        private TimerTask p_lagoutTimer;
        
        
        /* Constants */
        private final static int SCORE = 0;
        private final static int DEATHS = 1;
        private final static int WARBIRD_KILL = 2;
        private final static int JAVELIN_KILL = 3;
        private final static int SPIDER_KILL = 4;
        private final static int LEVIATHAN_KILL = 5;
        private final static int TERRIER_KILL = 6;
        private final static int WEASEL_KILL = 7;
        private final static int LANCASTER_KILL = 8;
        private final static int SHARK_KILL = 9;
        private final static int WARBIRD_TEAMKILL = 10;
        private final static int JAVELIN_TEAMKILL = 11;
        private final static int SPIDER_TEAMKILL = 12;
        private final static int LEVIATHAN_TEAMKILL = 13;
        private final static int TERRIER_TEAMKILL = 14;
        private final static int WEASEL_TEAMKILL = 15;
        private final static int LANCASTER_TEAMKILL = 16;
        private final static int SHARK_TEAMKILL = 17;
        private final static int FLAGS_CLAIMED = 18;
        //private final static int RATING = 19;
        private final static int REPELS_USED = 20;
        //private final static int SHOTS_FIRED = 22;
        
        private BWJSPlayer (String player, int shipType, int maxDeaths, int frequency) {
            p_ships = new int[9][22];
            p_name = player;
            p_currentShip = shipType;
            p_maxDeaths = maxDeaths;
            p_frequency = frequency;
            p_outOfBorderTime = cfg.outOfBorderTime;
            p_outOfBorderWarning = false;
            p_lagouts = 0;
            
            m_botAction.scoreReset(p_name);
            addPlayer();
        }
        
        private void addPlayer() {
            p_state = IN;
            if (m_botAction.getPlayer(p_name) != null) {
                m_botAction.setShip(p_name, p_currentShip);
                m_botAction.setFreq(p_name, p_frequency);
            }
            p_outOfBorderTime = cfg.outOfBorderTime;
            p_outOfBorderWarning = false;
        }
        
        private void change(int shipType) {
            m_botAction.sendArenaMessage(p_name + " changed from " + Tools.shipName(p_currentShip) +
                    " to " + Tools.shipName(shipType));
            p_currentShip = shipType;
            if (m_botAction.getPlayer(p_name) != null)
                m_botAction.setShip(p_name, shipType);
        }
        
        private void checkIfInBase() {
            if (p_state == IN) {
                if (m_botAction.getPlayer(p_name).getYTileLocation() > cfg.yborder)
                    p_outOfBorderTime--;
                
                if (p_outOfBorderTime == (cfg.outOfBorderTime / 2) && !p_outOfBorderWarning) {
                    m_botAction.sendPrivateMessage(p_name, "Go to base! You have " + p_outOfBorderTime +
                            " seconds before you'll get removed from the game!", Tools.Sound.BEEP3);
                    p_outOfBorderWarning = true;
                }
                else if (p_outOfBorderTime == 0) {
                    if (cfg.maxDeaths != 0)
                        p_ships[p_currentShip][DEATHS] = cfg.maxDeaths;
                    out();
                }
            }
        }
        
        private void checkOut() {
            if (p_maxDeaths <= getDeaths() && cfg.maxDeaths != 0)
                out();
        }
        
        private void flagClaimed() {
            p_ships[p_currentShip][FLAGS_CLAIMED]++;
        }
        
        private void flagReward(short points) {
            p_ships[p_currentShip][SCORE] += points;
        }
        
        private int getDeaths() {
            int deaths = 0;
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++)
                deaths += p_ships[i][DEATHS];
            return deaths;
        }
        
        private int getFlagsClaimed() {
            int flags = 0;
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++) {
                flags += p_ships[i][FLAGS_CLAIMED];
            }
            return flags;
        }
    
        private int getKills() {
            int kills = 0;
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++) {
                for (int j = WARBIRD_KILL; j <= SHARK_KILL; j++)
                    kills += p_ships[i][j];
            }
            return kills;
        }
        
        private int getLagouts() {
            return p_lagouts;
        }
        
        private int getRating() {
            /*
             * From statistics.java
             * warbird: .45Points * (.07wb + .07jav + .05spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
             * jav: .6Points * (.05wb + .06jav + .066spid + 0.14terr + .07x + .05lanc + .09shark - .05deaths - 
             *          (.07wbTK + .07javTK + .06spiderTK + .13terrTK + .06WeaselTK + .07LancTK + .09SharkTK))
             * spiders: .4points * (.06wb + .06jav + .04spid + .09terr + .05x + .05lanc + .089shark - .05deaths)
             * terr: 2.45points * (.03wb + .03jav + .036spid + .12terr + .35x + .025lanc + .052shark - .21deaths)
             * weasel: .8points * (sum(.09allships) - 0.05deaths)
             * lanc: .6Points * (.07wb + .07jav + .055spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
             * shark: points * (.65*repels/death + .005terr + .0015shark + sum(.001allotherships) - 0.001deaths - 
             *          (.07(allothershipstks) + .72spider + .5x + .15terrtk + .08sharkTK)))
             */
            int[] rating = new int[9];
            rating[Tools.Ship.WARBIRD] = (int) (
                    .45 * p_ships[Tools.Ship.WARBIRD][SCORE] * 
                    
                    (.07 * p_ships[Tools.Ship.WARBIRD][WARBIRD_KILL] + 
                    .07 * p_ships[Tools.Ship.WARBIRD][JAVELIN_KILL] + 
                    .05 * p_ships[Tools.Ship.WARBIRD][SPIDER_KILL] +
                    .12 * p_ships[Tools.Ship.WARBIRD][TERRIER_KILL] +
                    .05 * p_ships[Tools.Ship.WARBIRD][WEASEL_KILL] +
                    .06 * p_ships[Tools.Ship.WARBIRD][LANCASTER_KILL] +
                    .08 * p_ships[Tools.Ship.WARBIRD][SHARK_KILL] -
                    .04 * getDeaths())
                    
            );
            
            rating[Tools.Ship.JAVELIN] = (int) (
                    .6 * p_ships[Tools.Ship.JAVELIN][SCORE] * 
                    
                    (.05 * p_ships[Tools.Ship.JAVELIN][WARBIRD_KILL] + 
                    .06 * p_ships[Tools.Ship.JAVELIN][JAVELIN_KILL] + 
                    .066 * p_ships[Tools.Ship.JAVELIN][SPIDER_KILL] +
                    .14 * p_ships[Tools.Ship.JAVELIN][TERRIER_KILL] +
                    .07 * p_ships[Tools.Ship.JAVELIN][WEASEL_KILL] +
                    .05 * p_ships[Tools.Ship.JAVELIN][LANCASTER_KILL] +
                    .09 * p_ships[Tools.Ship.JAVELIN][SHARK_KILL] -
                    .05 * getDeaths() - (
                            .07 * p_ships[Tools.Ship.JAVELIN][WARBIRD_TEAMKILL] + 
                            .07 * p_ships[Tools.Ship.JAVELIN][JAVELIN_TEAMKILL] + 
                            .06 * p_ships[Tools.Ship.JAVELIN][SPIDER_TEAMKILL] +
                            .13 * p_ships[Tools.Ship.JAVELIN][TERRIER_TEAMKILL] +
                            .06 * p_ships[Tools.Ship.JAVELIN][WEASEL_TEAMKILL] +
                            .07 * p_ships[Tools.Ship.JAVELIN][LANCASTER_TEAMKILL] +
                            .09 * p_ships[Tools.Ship.JAVELIN][SHARK_TEAMKILL]
                                        )
                    )
            );
            
            rating[Tools.Ship.SPIDER] = (int) (
                    .4 * p_ships[Tools.Ship.SPIDER][SCORE] * 
                    
                    (.06 * p_ships[Tools.Ship.SPIDER][WARBIRD_KILL] + 
                    .06 * p_ships[Tools.Ship.SPIDER][JAVELIN_KILL] + 
                    .04 * p_ships[Tools.Ship.SPIDER][SPIDER_KILL] +
                    .09 * p_ships[Tools.Ship.SPIDER][TERRIER_KILL] +
                    .05 * p_ships[Tools.Ship.SPIDER][WEASEL_KILL] +
                    .05 * p_ships[Tools.Ship.SPIDER][LANCASTER_KILL] +
                    .089 * p_ships[Tools.Ship.SPIDER][SHARK_KILL] -
                    .05 * getDeaths()
                    )
            );
            
            rating[Tools.Ship.TERRIER] = (int) (
                    2.45 * p_ships[Tools.Ship.TERRIER][SCORE] * 
                    
                    (.03 * p_ships[Tools.Ship.TERRIER][WARBIRD_KILL] + 
                    .03 * p_ships[Tools.Ship.TERRIER][JAVELIN_KILL] + 
                    .036 * p_ships[Tools.Ship.TERRIER][SPIDER_KILL] +
                    .12 * p_ships[Tools.Ship.TERRIER][TERRIER_KILL] +
                    .35 * p_ships[Tools.Ship.TERRIER][WEASEL_KILL] +
                    .025 * p_ships[Tools.Ship.TERRIER][LANCASTER_KILL] +
                    .052 * p_ships[Tools.Ship.TERRIER][SHARK_KILL] -
                    .21 * getDeaths()
                    )
            );
            
            rating[Tools.Ship.WEASEL] = (int) (
                    2.45 * p_ships[Tools.Ship.WEASEL][SCORE] * (.09 * getKills() - .21 * getDeaths()));
            
            rating[Tools.Ship.LANCASTER] = (int) (
                    .6 * p_ships[Tools.Ship.LANCASTER][SCORE] * 
                    
                    (.07 * p_ships[Tools.Ship.LANCASTER][WARBIRD_KILL] + 
                    .07 * p_ships[Tools.Ship.LANCASTER][JAVELIN_KILL] + 
                    .055 * p_ships[Tools.Ship.LANCASTER][SPIDER_KILL] +
                    .12 * p_ships[Tools.Ship.LANCASTER][TERRIER_KILL] +
                    .05 * p_ships[Tools.Ship.LANCASTER][WEASEL_KILL] +
                    .06 * p_ships[Tools.Ship.LANCASTER][LANCASTER_KILL] +
                    .08 * p_ships[Tools.Ship.LANCASTER][SHARK_KILL] -
                    .04 * getDeaths()
                    )
            );
            
            int tmpShark;
            if (getDeaths() != 0)
                tmpShark = p_ships[Tools.Ship.SHARK][REPELS_USED] / getDeaths();
            else
                tmpShark = 0;
                
            rating[Tools.Ship.SHARK] = (int) (
                    p_ships[Tools.Ship.SHARK][SCORE] * 
                    
                    (.065 * (tmpShark) +
                    .001 * p_ships[Tools.Ship.SHARK][WARBIRD_KILL] + 
                    .001 * p_ships[Tools.Ship.SHARK][JAVELIN_KILL] + 
                    .001 * p_ships[Tools.Ship.SHARK][SPIDER_KILL] +
                    .005 * p_ships[Tools.Ship.SHARK][TERRIER_KILL] +
                    .001 * p_ships[Tools.Ship.SHARK][WEASEL_KILL] +
                    .001 * p_ships[Tools.Ship.SHARK][LANCASTER_KILL] +
                    .0015 * p_ships[Tools.Ship.SHARK][SHARK_KILL] -
                    .001 * getDeaths() - (
                            .07 * p_ships[Tools.Ship.SHARK][WARBIRD_TEAMKILL] + 
                            .07 * p_ships[Tools.Ship.SHARK][JAVELIN_TEAMKILL] + 
                            .072 * p_ships[Tools.Ship.SHARK][SPIDER_TEAMKILL] +
                            .15 * p_ships[Tools.Ship.SHARK][TERRIER_TEAMKILL] +
                            .05 * p_ships[Tools.Ship.SHARK][WEASEL_TEAMKILL] +
                            .07 * p_ships[Tools.Ship.SHARK][LANCASTER_TEAMKILL] +
                            .08 * p_ships[Tools.Ship.SHARK][SHARK_TEAMKILL]
                                        )
                    )
            );
            
            int totalRating = 0;
            for (int i = 0; i < rating.length; i++)
                totalRating += rating[i];
            return totalRating;
        }
        
        private int getScore() {
            int score = 0;
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++) {
                score += p_ships[i][SCORE];
            }
            return score;
        }
        
        private int getTeamKills() {
            int kills = 0;
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++) {
                for (int j = WARBIRD_TEAMKILL; j <= SHARK_TEAMKILL; j++)
                    kills += p_ships[i][j];
            }
            return kills;
        }
        
        private int getTerrKills() {
            int kills = 0;
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++) {
                kills += p_ships[i][TERRIER_KILL];
            }
            return kills;
        }
    
        private String getStatus() {
            switch (p_state) {
                case (IN) : return "IN";
                case (LAGOUT) : return "LAGGED OUT";
                case (LAGOUT_OUT) : return "LAGGED OUT";
                case (SUBBED) : return "SUBSTITUTED";
                case (OUT) : return "OUT";
                case (OUT_BUT_SUBABLE) : return "OUT (still substitutable)";
            }
            return "";
        }
        
        private void killed() {
            p_ships[p_currentShip][DEATHS]++;
            p_outOfBorderTime = cfg.outOfBorderTime;
            p_outOfBorderWarning = false;
            checkOut();
        }
        
        private void killer(PlayerDeath event) {
            int ship = 0;
            int killeeShip = m_botAction.getPlayer(event.getKilleeID()).getShipType();
            int killeeFreq = m_botAction.getPlayer(event.getKilleeID()).getFrequency();
            
            if (p_frequency != killeeFreq) {
                switch (killeeShip) {
                    case Tools.Ship.WARBIRD : ship = WARBIRD_KILL; break;
                    case Tools.Ship.JAVELIN : ship = JAVELIN_KILL; break;
                    case Tools.Ship.SPIDER : ship = SPIDER_KILL; break;
                    case Tools.Ship.LEVIATHAN : ship = LEVIATHAN_KILL; break;
                    case Tools.Ship.TERRIER : ship = TERRIER_KILL; break;
                    case Tools.Ship.WEASEL : ship = WEASEL_KILL; break;
                    case Tools.Ship.LANCASTER : ship = LANCASTER_KILL; break;
                    case Tools.Ship.SHARK : ship = SHARK_KILL; break;
                }
            } else {
                switch (killeeShip) {
                    case Tools.Ship.WARBIRD : ship = WARBIRD_TEAMKILL; break;
                    case Tools.Ship.JAVELIN : ship = JAVELIN_TEAMKILL; break;
                    case Tools.Ship.SPIDER : ship = SPIDER_TEAMKILL; break;
                    case Tools.Ship.LEVIATHAN : ship = LEVIATHAN_TEAMKILL; break;
                    case Tools.Ship.TERRIER : ship = TERRIER_TEAMKILL; break;
                    case Tools.Ship.WEASEL : ship = WEASEL_TEAMKILL; break;
                    case Tools.Ship.LANCASTER : ship = LANCASTER_TEAMKILL; break;
                    case Tools.Ship.SHARK : ship = SHARK_TEAMKILL; break;
                }
            }
            p_ships[p_currentShip][ship]++;
            p_ships[p_currentShip][SCORE] += event.getKilledPlayerBounty(); 
        }
        
        private void lagin() {
            m_botAction.sendOpposingTeamMessageByFrequency(p_frequency, p_name + " returned from lagout.");
            m_botAction.cancelTask(p_lagoutTimer);
            addPlayer();
        }
        
        private void lagout() {
            p_state = LAGOUT;
            p_lagoutTime = System.currentTimeMillis();
            if (state == GAME_IN_PROGRESS) { 
                p_lagouts++;
                if (cfg.gameType != BASE) {
                    p_ships[p_currentShip][DEATHS]++;
                    m_botAction.sendOpposingTeamMessageByFrequency(p_frequency, p_name + " lagged out or specced. " +
                    		"(+1 death)");
                    checkOut();
                } else
                    m_botAction.sendOpposingTeamMessageByFrequency(p_frequency, p_name + " lagged out or specced.");
            
                if ((cfg.maxLagouts != -1) && p_lagouts >= cfg.maxLagouts) {
                    if (p_state < OUT)
                        out();
                } else {
                    if (p_state != OUT)
                        m_botAction.sendPrivateMessage(p_name, "PM me \"!lagout\" to get back in.");
                }
            } else {
                m_botAction.sendArenaMessage(p_name + " lagged out or specced.");
                m_botAction.sendPrivateMessage(p_name, "PM me \"!lagout\" to get back in.");
            }
            
            /*
             * Lagout Timer:
             * People will still be able to return from their lagout, 
             * but after one minute the player will be counted out 
             * until he or she returns from his or her lagout.
             */
            p_lagoutTimer = new TimerTask() {
                public void run() {
                    if (p_state == LAGOUT)
                        p_state = LAGOUT_OUT;
                }
            };
            if (p_state != OUT)
                m_botAction.scheduleTask(p_lagoutTimer, Tools.TimeInMillis.MINUTE);
        }
        
        private void out() {
            p_state = OUT;
            if (m_botAction.getPlayer(p_name) != null) {
                m_botAction.specWithoutLock(p_name);
                m_botAction.setFreq(p_name, p_frequency);
            }
            
            if (p_outOfBorderTime == 0 && cfg.yborder != -1) 
                m_botAction.sendArenaMessage(p_name + " is out, (too long outside of base). " + 
                        getKills() + " wins " + getDeaths() + " losses");
            else if ((cfg.maxLagouts != -1) && p_lagouts >= cfg.maxLagouts && getDeaths() != cfg.maxDeaths) {
                m_botAction.sendArenaMessage(p_name+ " is out, (too many lagouts). " + 
                        getKills() + " wins " + getDeaths() + " losses. (NOTICE: player can still be subbed)");
                p_state = OUT_BUT_SUBABLE;
            } else if (getDeaths() != p_maxDeaths && cfg.maxDeaths != 0) {
                m_botAction.sendArenaMessage(p_name + " is out. " + 
                        getKills() + " wins " + getDeaths() + " losses. (NOTICE: player can still be subbed)");
                p_state = OUT_BUT_SUBABLE;
            }
            else if (cfg.maxDeaths != 0) 
                m_botAction.sendArenaMessage(p_name + " is out. " + 
                        getKills() + " wins " + getDeaths() + " losses");
        }
    
        private void repelUsed() {
            p_ships[p_currentShip][REPELS_USED]++;
        }
    
        private void sub() {
            p_state = SUBBED;
            if (m_botAction.getPlayer(p_name) != null) {
                m_botAction.specWithoutLock(p_name);
                if (!listNotplaying.contains(p_name.toLowerCase()))
                    m_botAction.setFreq(p_name, p_frequency);
            }
        }
    }
        
    private class BWJSTeam {
        private boolean pickingTurn;
        private boolean flag;
        private boolean ready;
        private int flagTime;
        private int frequency;
        private int number;
        private int substitutesLeft;
        private String name;
        private String captainName;
        private TreeMap<String, BWJSPlayer> players;
        
        
        private BWJSTeam(int teamNumber) {
            number = teamNumber;
            players = new TreeMap<String, BWJSPlayer>();
            reset();
        }
        
        private void addPlayer(Player player, int shipType) {
            if (cfg.announceShipType) 
                m_botAction.sendArenaMessage(player.getPlayerName() +
                        " is in for " + name + " as a " + Tools.shipName(shipType) + ".");
            else
                m_botAction.sendArenaMessage(player.getPlayerName() +
                        " is in for " + name + ".");
            
            if (!players.containsKey(player.getPlayerName().toLowerCase()))
                players.put(player.getPlayerName().toLowerCase(), 
                        new BWJSPlayer(player.getPlayerName(), shipType, cfg.maxDeaths, frequency));
            else {
                players.get(player.getPlayerName().toLowerCase()).p_currentShip = shipType;
                players.get(player.getPlayerName().toLowerCase()).addPlayer();
            }
        }
        
        private void addTimePoint() {
            if (flag) {
                flagTime++;
                if (((cfg.timeTarget * 60) - flagTime) == 3 * 60)
                    m_botAction.sendArenaMessage(name + " needs 3 mins of flag time to win");
                if (((cfg.timeTarget * 60) - flagTime) == 1 * 60)
                    m_botAction.sendArenaMessage(name + " needs 1 minute of flag time to win");
            }
        }
        
        private void flagClaimed(FlagClaimed event) {
            flag = true;
            players.get(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase()).flagClaimed();
        }
        
        private void flagLost() {
            flag = false;
        }
        
        private void flagReward(short points) {
            for (BWJSPlayer i : players.values())
                i.flagReward(points);
        }
        
        private int getDeaths() {
            int deaths = 0;
            int counter = 0;
            int playersNotHere;
            
            for (BWJSPlayer i : players.values()) {
                deaths += i.getDeaths();
                counter++;
            }
            
            playersNotHere = cfg.maxPlayers - counter;
            
            if (playersNotHere > 0)
                deaths += cfg.maxDeaths * playersNotHere;
            
            return deaths;
        }
        
        private int getFlagsClaimed() {
            int flags = 0;
            
            for (BWJSPlayer i : players.values())
                flags += i.getFlagsClaimed();
            
            return flags;
        }
        
        private int getKills() {
            int kills = 0;
            
            for (BWJSPlayer i : players.values())
                kills += i.getKills();
            
            return kills;
        }
        
        private int getLagouts() {
            int lagouts = 0;
            
            for (BWJSPlayer i : players.values())
                lagouts += i.getLagouts();
            
            return lagouts;
        }
        
        private int getRating() {
            int rating = 0;
            
            for (BWJSPlayer i : players.values())
                rating += i.getRating();
            
            return rating;
        }
        
        private int getTeamKills() {
            int kills = 0;
            
            for (BWJSPlayer i : players.values())
                kills += i.getTeamKills();
            
            return kills;
        }
        
        private int getTerrKills() {
            int kills = 0;
            
            for (BWJSPlayer i : players.values())
                kills += i.getTerrKills();
            
            return kills;
        }
        
        private int getScore() {
            int score = 0;
            
            for (BWJSPlayer i : players.values())
                score += i.getScore();
            
            return score;
        }
        
        private ArrayList<String> getScores() {
            ArrayList<String> out = new ArrayList<String>();
            
            if (cfg.gameType == BASE) {
                out.add("|                          ,------+------+------+-----------+------+------+-----------+----+");
                
                out.add("| " + Tools.formatString(name, 23) + " /  " +
                        Tools.rightString(Integer.toString(getKills()), 4) + " | " +
                        Tools.rightString(Integer.toString(getDeaths()), 4) + " | " +
                        Tools.rightString(Integer.toString(getTeamKills()), 4) + " | " +
                        Tools.rightString(Integer.toString(getScore()), 9) + " | " +
                        Tools.rightString(Integer.toString(getFlagsClaimed()), 4) + " | " +
                        Tools.rightString(Integer.toString(getTerrKills()), 4) + " | " +
                        Tools.rightString(Integer.toString(getRating()), 9) + " | " +
                        Tools.rightString(Integer.toString(getLagouts()), 2) + " |");
                out.add("+------------------------'        |      |      |           |      |      |           |    |");
                
                for (BWJSPlayer p : players.values()) {
                    out.add("|  " + Tools.formatString(p.p_name, 25) + " "
                            + Tools.rightString(Integer.toString(p.getKills()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getDeaths()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getTeamKills()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getScore()), 9) + " | "
                            + Tools.rightString(Integer.toString(p.getFlagsClaimed()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getTerrKills()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getRating()), 9) + " | "
                            + Tools.rightString(Integer.toString(p.getLagouts()), 2) + " |");
                }
            } else  if (cfg.gameType == JAVDUEL){
                out.add("|                          ,------+------+------+-----------+----+");
                out.add("| " + Tools.formatString(name, 23) + " /  "
                        + Tools.rightString(Integer.toString(getKills()), 4) + " | "
                        + Tools.rightString(Integer.toString(getDeaths()), 4) + " | "
                        + Tools.rightString(Integer.toString(getTeamKills()), 4) + " | "
                        + Tools.rightString(Integer.toString(getRating()), 9) + " | "
                        + Tools.rightString(Integer.toString(getLagouts()), 2) + " |");
                out.add("+------------------------'        |      |      |           |    |");

                for (BWJSPlayer p : players.values()) {
                    out.add("|  " + Tools.formatString(p.p_name, 25) + " "
                            + Tools.rightString(Integer.toString(p.getKills()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getDeaths()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getTeamKills()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getRating()), 9) + " | "
                            + Tools.rightString(Integer.toString(p.getLagouts()), 2) + " |");
                }
            } else {
                out.add("|                          ,------+------+-----------+----+");
                out.add("| " + Tools.formatString(name, 23) + " /  "
                        + Tools.rightString(Integer.toString(getKills()), 4) + " | "
                        + Tools.rightString(Integer.toString(getDeaths()), 4) + " | "
                        + Tools.rightString(Integer.toString(getRating()), 9) + " | "
                        + Tools.rightString(Integer.toString(getLagouts()), 2) + " |");
                out.add("+------------------------'        |      |           |    |");

                for (BWJSPlayer p : players.values()) {
                    out.add("|  " + Tools.formatString(p.p_name, 25) + " "
                            + Tools.rightString(Integer.toString(p.getKills()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getDeaths()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getRating()), 9) + " | "
                            + Tools.rightString(Integer.toString(p.getLagouts()), 2) + " |");
                }
            }
            return out;
        }
        
        private boolean isDead() {
            for (BWJSPlayer i : players.values()) {
                if (i.p_state < LAGOUT_OUT)
                    return false;
            }
            return true;
        }
        
        private void notReady() {
            ready = false;
            m_botAction.sendArenaMessage(name + " is NOT ready to begin.");
        }
        
        private void ready() {
            if (!ready) {
                if (players.size() >= cfg.minPlayers) {
                    m_botAction.sendArenaMessage(name + " is ready to begin.");
                    ready = true;
                } else
                    m_botAction.sendPrivateMessage(captainName, "Cannot ready, not enough players in.");
            } else
                notReady();
        }

        private void remove(BWJSPlayer player) {
            players.remove(player.p_name.toLowerCase());
            m_botAction.sendArenaMessage(player.p_name + " has been removed from " + name);
            if (m_botAction.getPlayer(player.p_name) != null) {
                m_botAction.specWithoutLock(player.p_name);
                m_botAction.setFreq(player.p_name, FREQ_SPEC);
            }
            determineNextPick();
        }
        
        private void removeCap() {
            m_botAction.sendArenaMessage(captainName +
                    " has been removed as captain of " + name + ".", Tools.Sound.CROWD_AWW);
            captainName = "[nobody]";
        }
        
        private void reset() {
            captainName = "[nobody]";
            frequency = number;
            flag = false;
            flagTime = 0;
            name = "Freq " + frequency;
            players.clear();
            ready = false;
            substitutesLeft = cfg.maxSubs;
            pickingTurn = false;
        }
        
        private BWJSPlayer searchPlayer(String playerName) {
            BWJSPlayer best = null;
            for (BWJSPlayer p : players.values()) {
                if (p.p_name.toLowerCase().startsWith(playerName.toLowerCase())) {
                    if (best == null)
                        best = p;
                    else if (best.p_name.toLowerCase().compareTo(p.p_name.toLowerCase()) > 0)
                        best = p;
                }
            }
            return best;
        }
        
        private int ships(int shipType) {
            int count = 0;
            for (BWJSPlayer p : players.values()) {
                if (p.p_state < SUBBED && p.p_currentShip == shipType)
                    count++;
            }
            return count;
        }
        
        private int sizeIN() {
            int size = 0;
            for (BWJSPlayer i : players.values()) {
                if (i.p_state != SUBBED)
                    size++;
            }
            return size;
        }
        
        private void sub(BWJSPlayer playerOne, Player playerTwo) {
            int shipType = playerOne.p_currentShip;
            int maxDeaths;
            
            if (playerOne.p_maxDeaths == 0)
                maxDeaths = 0;
            else
                maxDeaths = playerOne.p_maxDeaths - playerOne.getDeaths();
            
            //Removing player
            playerOne.sub();
            
            //Adding substitute
            if (cfg.gameType == BASE) {
                if (players.containsKey(playerTwo.getPlayerName().toLowerCase())) {
                    BWJSPlayer p = players.get(playerTwo.getPlayerName().toLowerCase());
                    p.p_currentShip = shipType;
                    p.addPlayer();
                }
                else
                    players.put(playerTwo.getPlayerName().toLowerCase(), 
                            new BWJSPlayer(playerTwo.getPlayerName(), shipType, maxDeaths, frequency));
            } else {
                players.put(playerTwo.getPlayerName().toLowerCase(), 
                        new BWJSPlayer(playerTwo.getPlayerName(), shipType, maxDeaths, frequency));
            }
            m_botAction.sendPrivateMessage(playerTwo.getPlayerID(), "You are subbed in the game.");
            
            if (cfg.maxDeaths == 0)
                m_botAction.sendArenaMessage(playerOne.p_name + " has been substituted by " +
                        playerTwo.getPlayerName());
            else 
                m_botAction.sendArenaMessage(playerOne.p_name + " has been substituted by " +
                        playerTwo.getPlayerName() + ", with " + maxDeaths + " deaths left");
            
            substitutesLeft--;
            
            if (substitutesLeft >= 0)
                m_botAction.sendSmartPrivateMessage(captainName, "You have " + substitutesLeft + "substitutes left.");
            
        }
        
        private void switchPlayers(BWJSPlayer playerOne, BWJSPlayer playerTwo) {
            m_botAction.sendArenaMessage(playerOne.p_name + " (" + Tools.shipName(playerOne.p_currentShip) + ") and "
                    + playerTwo.p_name + " (" + Tools.shipName(playerTwo.p_currentShip) + ") switched ships.");
            
            int playerOneShipType = playerTwo.p_currentShip;
            int playerTwoShipType = playerOne.p_currentShip;
            
            playerOne.p_currentShip = playerOneShipType;
            playerTwo.p_currentShip = playerTwoShipType;
            
            if (m_botAction.getPlayer(playerOne.p_name) != null)
                playerOne.addPlayer();
            if (m_botAction.getPlayer(playerTwo.p_name) != null)
                playerTwo.addPlayer();
        }
        
        private void warpTo(int x_coord, int y_coord) {
            for (BWJSPlayer i : players.values()) {
                int playerID = m_botAction.getPlayerID(i.p_name);
                m_botAction.warpTo(playerID, x_coord, y_coord);
            }
        }
    }

    /* Timer Classes */
    private class AddingTimer extends TimerTask {
        public void run() {
            m_botAction.sendArenaMessage("Time is up! Checking lineups..");
            checkLineup();
        }
    }
    
    private class CapTimer extends TimerTask {
        public void run() {
            if (team[ONE].captainName.equalsIgnoreCase("[nobody]"))
                team[TWO].removeCap();
            else
                team[ONE].removeCap();
        }
    }
    
    private class FiveSecondTimer extends TimerTask {
        public void run() {
            m_botAction.showObject(cfg.objects[1]);
        }
    }
    
    private class GameTimer extends TimerTask {
        public void run() {
            gameOver();
        }
    }
    
    private class UpdateTimer extends TimerTask {
        public void run() {
            cfg.timeLeft--;
            
            if (cfg.timeTarget != 0) {
                team[ONE].addTimePoint();
                team[TWO].addTimePoint();
                
                if (team[ONE].flagTime == (cfg.timeTarget * 60) || team[TWO].flagTime == (cfg.timeTarget * 60))
                    gameOver();
            } else {
                if (team[ONE].isDead() || team[TWO].isDead())
                    gameOver();
            }
            
            if (cfg.inBase)
                checkIfInBase();
            
            updateScoreboard();
        }
    }
    
    private class StartGameTimer extends TimerTask {
        public void run() {
            startGame();
        }
    }
    
    private class PreGameTimer extends TimerTask {
        public void run() {
            startPreGame();
        }
    }

    private class ZonerTimer extends TimerTask {
        public void run() {
            lockZoner = false;
        }
    }
}
