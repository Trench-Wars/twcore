package twcore.bots.bwjsbotbeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
 * BWJSBot hosts base, wbduel, javduel and spidduel
 * 
 * @author fantus
 */
public class bwjsbotbeta extends SubspaceBot {
    private boolean lockArena;
    private boolean lockLastGame;
    private BWJSConfig cfg;                                 //Game configuration
    private BWJSState state;                                //Game state
    private BWJSSQL sql;
    private BWJSTeam[] team;                                //Teams
    private Spy racismWatcher;                              //Racism watcher
    
    private ArrayList<String> listNotplaying;               //List of notplaying players
    private ArrayList<String> listAlert;                    //List of players who toggled !subscribe on
    
    private int timeLeft;                                   //Total game time
    private long zonerTimestamp;                            //Timestamp of the last zoner
    private Objset scoreboard;                              //Scoreboard lvz
    
    //Frequencies
    private static final int FREQ_SPEC = 9999;
    private static final int FREQ_NOTPLAYING = 666;
    
    //Static variables
    private static final int ZONER_WAIT_TIME = 15;
    
    //Game ticker
    private Gameticker gameticker;                          //The beating heart of the game
    
    /** Class constructor */
    public bwjsbotbeta(BotAction botAction) {
        super(botAction);
        initializeVariables();  //Initialize variables
        requestEvents();        //Request Subspace Events
    }
    
    /*
     * Events
     */
    
    /**
     * Handles ArenaJoined event
     * - Sets up reliable kills
     * - Sets up chats
     * - Autostarts bot
     */
    public void handleEvent(ArenaJoined event) {
        m_botAction.setReliableKills(1);  //Reliable kills so the bot receives every packet
        m_botAction.sendUnfilteredPublicMessage("?chat=" + cfg.getChats());  //Join all the chats
        start();    //Autostart the bot
    }
    
    /**
     * Handles FlagClaimed event
     * - Notify both teams of a flag claim
     */
    public void handleEvent(FlagClaimed event) {
        if (state.getCurrentState() == BWJSState.GAME_IN_PROGRESS) {
            String playerName;  //Name of the player that claimed the flag
            
            playerName = m_botAction.getPlayerName(event.getPlayerID());
            
            /* Null pointer exception check */
            if (playerName == null) {
                return;
            }
            
            for (BWJSTeam i : team) {
                i.flagClaimed(playerName);
            }
        }
    }
    
    /**
     * Handles FlagReward event
     * - Give flag points to the specified team
     * - Check if the frequency that gets the points is of one of the teams
     */
    public void handleEvent(FlagReward event) {
        if (state.getCurrentState() == BWJSState.GAME_IN_PROGRESS) {
            //Check if points go to one of the teams
            if (event.getFrequency() == 0 || event.getFrequency() == 1) {
                team[event.getFrequency()].flagReward(event.getPoints());
            }   
        }
    }
    
    /**
     * Handles FrequencyChange event
     * - since this event looks almost the same as FrequencyShipChange 
     *   event its passed on to checkFCandFSC(name, frequency, ship).
     */
    public void handleEvent(FrequencyChange event) {
        if (state.getCurrentState() > BWJSState.OFF) {
            Player p;
            
            p = m_botAction.getPlayer(event.getPlayerID());
            
            /* Null pointer exception check */
            if (p == null) {
                return;
            }
            
            checkFCandFSC(p.getPlayerName(), p.getFrequency(), p.getShipType());
        }
    }
    
    /**
     * Handles FrequencyShipChange event
     * - since this event looks almost the same as FrequencyChange 
     *   event its passed on to checkFCandFSC(name, frequency, ship).
     */
    public void handleEvent(FrequencyShipChange event) {
        if (state.getCurrentState() > BWJSState.OFF) {
            Player p;
            
            p = m_botAction.getPlayer(event.getPlayerID());
            
            /* Null pointer exception check */
            if (p == null) {
                return;
            }
            
            checkFCandFSC(p.getPlayerName(), p.getFrequency(), p.getShipType());
        }
    }
    
    /** 
     * Handles LoggedOn event 
     * - Join arena
     * - Set antispam measurements
     */
    public void handleEvent(LoggedOn event) {
        short resolution;   //Screen resolution of the bot
        
        resolution = 3392;  //Set the maximum allowed resolution
        
        /* Join Arena */
        try {
            m_botAction.joinArena(cfg.getArena(), resolution, resolution);
        } catch (Exception e) {
            m_botAction.joinArena(cfg.getArena());
        }
        
        m_botAction.setMessageLimit(10);    //Set antispam measurements
    }
    
    /**
     * Handles Message event
     * - Racism watcher
     * - Arena lock
     * - Player commands
     */
    public void handleEvent(Message event) {
        String message;     //Message
        String sender;      //Sender of the message
        int messageType;    //Message type
        
        message = event.getMessage();
        sender = m_botAction.getPlayerName(event.getPlayerID());
        messageType = event.getMessageType();
        
        racismWatcher.handleEvent(event);   //Racism watcher
        
        if (messageType == Message.ARENA_MESSAGE) {
            checkArenaLock(message);    //Checks if the arena should be locked
        } else if (messageType == Message.PRIVATE_MESSAGE) {
            /* Null pointer exception check */
            if (sender == null) {
                return;
            }
            handleCommand(sender, message, -1);   //Handle commands
        }
    }
    
    /** Handles PlayerDeath event */
    public void handleEvent(PlayerDeath event) {
        if (state.getCurrentState() == BWJSState.GAME_IN_PROGRESS) {
            Player killee;      //Player that got killed
            String killer;      //Name of the player killer
            
            
            killee = m_botAction.getPlayer(event.getKilleeID());
            killer = m_botAction.getPlayerName(event.getKillerID());

            /* Null pointer exception check */
            if (killee == null || killer == null) {
                return;
            }
            
            for (BWJSTeam i : team) {
                i.playerDeath(killee.getPlayerName(), 
                        killer, 
                        killee.getShipType(), 
                        killee.getFrequency(), 
                        event.getKilledPlayerBounty());
            }
        }
    }
    
    /** 
     * Handles PlayerEntered event
     * - Sends welcome message
     * - Puts the player on the corresponding frequency
     */
    public void handleEvent(PlayerEntered event) {
        if (state.getCurrentState() > BWJSState.OFF) {
            String name;    //Name of the player that entered the zone
            
            name = m_botAction.getPlayerName(event.getPlayerID());

            /* Null pointer exception check */
            if (name == null) {
                return;
            }
            
            sendWelcomeMessage(name);   //Sends welcome message with status info to the player
            putOnFreq(name);            //Puts the player on the corresponding frequency
        }
    }
    
    /**
     * Handles PlayerLeft event
     * - Checks if the player that left was a captain
     * - Checks if the player that left lagged out
     */
    public void handleEvent(PlayerLeft event) {
        if (state.getCurrentState() > BWJSState.OFF) {
            String name;    //Name of the player that left
            
            name = m_botAction.getPlayerName(event.getPlayerID());

            /* Null pointer exception check */
            if (name == null) {
                return;
            }
            
            checkCaptainLeft(name); //Check if the player that left was a captain
            checkLagout(name, Tools.Ship.SPECTATOR);    //Check if the player that left was IN the game
        }
    }
    
    /**
     * Handles PlayerPosition event
     * - Warps players back to their safes during PRE_GAME
     * - Timestamps last received position for out of border time
     */
    public void handleEvent(PlayerPosition event) {
        String name;    //Name of the player
        BWJSTeam t;     //Team
        
        name = m_botAction.getPlayerName(event.getPlayerID());

        /* Null pointer exception check */
        if (name == null) {
            return;
        }
        
        t = getTeam(name);

        /* Null pointer exception check */
        if (t == null) {
            return;
        }
        
        switch (state.getCurrentState()) {
            case BWJSState.PRE_GAME :
                int x_coord;    //X location
                int y_coord;    //Y location
                
                x_coord = event.getXLocation() / 16;
                y_coord = event.getYLocation() / 16;
                
                t.checkPostionPreGame(name, x_coord, y_coord);
                break;
            case BWJSState.GAME_IN_PROGRESS :
                if (t != null) {
                    t.timestampLastPosition(name); //Timestamp last position update
                }
                break;
        }
    }
    
    /**
     * Handles WeaponFired event
     * - Notify the team of the weapontype that got fired and who did it
     */
    public void handleEvent(WeaponFired event) {
        if (state.getCurrentState() == BWJSState.GAME_IN_PROGRESS) {
            String name;
            BWJSTeam t;
            
            name = m_botAction.getPlayerName(event.getPlayerID());

            /* Null pointer exception check */
            if (name == null) {
                return;
            }
            
            t = getTeam(name);

            /* Null pointer exception check */
            if (t == null) {
                return;
            }
            
            t.weaponFired(name, event.getWeaponType());
        }
    }
    
    /**
     * Hanldes a disconnect
     * - cancel all tasks
     * - close all sql connections
     */
    public void handleDisconnect(){
        m_botAction.cancelTasks();
        sql.closePreparedStatements();
    }
    
    /*
     * Commands
     */
    
    /**
     * Handles player commands
     * 
     * @param name Sender of the command
     * @param cmd command
     * @param override Override number, -1 for default, 0 for Freq 0, 1 for Freq 1
     */
    private void handleCommand(String name, String cmd, int override) {
        cmd = cmd.toLowerCase();
        
        /* Captain commands */
        if (isCaptain(name) || override != -1) {
            if (cmd.startsWith("!change")) {
                cmd_change(name, cmd, override);
            } else if (cmd.startsWith("!switch")) {
                cmd_switch(name, cmd, override);
            } else if (cmd.startsWith("!add")) {
                cmd_add(name, cmd, override);
            } else if (cmd.equals("!ready")) {
                cmd_ready(name, override);
            } else if (cmd.equals("!removecap")) {
                cmd_removecap(name, override);
            } else if (cmd.startsWith("!remove")) {
                cmd_remove(name, cmd, override);
            } else if (cmd.startsWith("!sub")) {
                cmd_sub(name, cmd, override);
            }
        }
        
        /* Player commands */
        if (cmd.equals("!cap")) {
            cmd_cap(name);
        } else if (cmd.equals("!help")) {
            cmd_help(name);
        } else if (cmd.equals("!return")) {
            cmd_lagout(name);
        } else if (cmd.equals("!lagout")) {
            cmd_lagout(name);
        } else if (cmd.equals("!list")) {
            cmd_list(name);
//        } else if (cmd.startsWith("!listbest")) {
//            cmd_listBest(name, cmd);
        } else if (cmd.equals("!myfreq")) {
            cmd_myfreq(name);
        } else if (cmd.equals("!mvp")) {
            cmd_mvp(name);
        } else if (cmd.startsWith("!rating ")) {
            cmd_rating(name, cmd);
        } else if (cmd.startsWith("!score ")) {
            cmd_score(name, cmd);
        } else if (cmd.equals("!notplaying")) {
            cmd_notplaying(name);
        } else if (cmd.equals("!status")) {
            cmd_status(name);
        } else if (cmd.equals("!subscribe")) {
            cmd_subscribe(name);
//        } else if (cmd.startsWith("!stats ")) {
//            cmd_stats(name, cmd);
//        } else if (cmd.startsWith("!top10")) {
//            cmd_top10(name, cmd);
        }
        
        /* Staff commands ER+ */
        if (m_botAction.getOperatorList().isER(name)) {
            if (cmd.equals("!start")) {
                cmd_start(name);
            } else if (cmd.equals("!stop")) {
                cmd_stop(name);
            } else if (cmd.equals("!zone") &&
                    !cfg.getAllowAutoCaps() &&
                    (state.getCurrentState() == BWJSState.GAME_OVER ||
                            state.getCurrentState() == BWJSState.WAITING_FOR_CAPS ||
                            state.getCurrentState() == BWJSState.ADDING_PLAYERS)) {
                newGameAlert();
            } else if (cmd.equals("!off")) {
                cmd_off(name);
            } else if (cmd.startsWith("!setcaptain")) {
                cmd_setCaptain(name, cmd, override);
            } else if (cmd.startsWith("!t1") || cmd.startsWith("!t2")) {
                cmd_overrideCmd(name, cmd);
            }
        }
        
        /* Staff commands Moderator+ */
        if (m_botAction.getOperatorList().isModerator(name)) {
            if (cmd.equals("!die")) {
                m_botAction.die();
            }
        }
    }
    
    /** Handles the !add command */
    private void cmd_add(String name, String cmd, int override) {
        int shipType;       //Specified shiptype (in cmd)
        Player p;           //Specified player (in cmd)
        String p_lc;        //Specified player's name in lower case
        String[] splitCmd;  //Cmd split up
        BWJSTeam t;         //Team
        
        /* Check if name is a captain or that the command is overriden */
        if (!isCaptain(name) && override == -1) {
            return;
        }
        
        t = getTeam(name, override);    //Retrieve team
        
        /* Check if it is the team's turn to pick during ADDING_PLAYERS */
        if (state.getCurrentState() == BWJSState.ADDING_PLAYERS) {
            if (!t.isTurn()) {
                m_botAction.sendPrivateMessage(name, "Error: Not your turn to pick!");
                return;
            }
        }
        
        /* Check command syntax */
        if (cmd.length() < 5) {
            m_botAction.sendPrivateMessage(name, "Error: Please specify atleast a playername, !add <player>");
            return;
        }
        
        if (state.getCurrentState() >= BWJSState.ADDING_PLAYERS &&
                state.getCurrentState() <= BWJSState.GAME_IN_PROGRESS) {
            splitCmd = cmd.substring(5).split(":"); //Split command (<player>:<shiptype>)
            
            p = m_botAction.getFuzzyPlayer(splitCmd[0]);    //Find <player>
            
            /* Check if p has been found */
            if (p == null) {
                m_botAction.sendPrivateMessage(name, "Error: " + splitCmd[0] + " could not be found.");
                return;
            }
            
            p_lc = p.getPlayerName().toLowerCase();
            
            /* Check if p is a bot */
            if (m_botAction.getOperatorList().isBotExact(p_lc)) {
                m_botAction.sendPrivateMessage(name, "Error: Pick again, bots are not allowed to play.");
                return;
            }
            
            /* Check if the player is set to notplaying */
            if (listNotplaying.contains(p_lc)) {
                m_botAction.sendPrivateMessage(name, "Error: " + p.getPlayerName() + " is set to notplaying.");
                return;
            }
            
            /* Check if the maximum amount of players IN is already reached */
            if (t.getSizeIN() >= cfg.getMaxPlayers()) {
                m_botAction.sendPrivateMessage(name, "Error: Maximum amount of players already reached.");
                return;
            }
            
            /* 
             * Check if the player was already on the team
             * Note: BASE games skip this check
             */
            if (cfg.getGameType() != BWJSConfig.BASE) {
                if (t.isPlayer(p_lc)) {
                    m_botAction.sendPrivateMessage(name, "Error: " + p.getPlayerName() +
                            " is already on your team, check with !list");
                    return;
                }
            }
            
            /* Check if the player is already on the team and playing */
            if (t.isIN(p_lc)) {
                m_botAction.sendPrivateMessage(name, "Error: " + p.getPlayerName() +
                    " is already on your team, check with !list");
                return;
            }
            
            /* Check if the player was already on the other team */
            if (getOtherTeam(name, override).isOnTeam(p_lc)) {
                m_botAction.sendPrivateMessage(name, "Error: Player is already on the other team.");
                return;
            }
            
            /* Check for ship type */
            if (splitCmd.length > 1) {
                try {
                    shipType = Integer.parseInt(splitCmd[1]);
                } catch (Exception e) { 
                    shipType = cfg.getDefaultShipType();
                }
                
                /* Check if the  ship type is valid */
                if (shipType < Tools.Ship.WARBIRD || shipType > Tools.Ship.SHARK) {
                    shipType = cfg.getDefaultShipType();
                }
            } else {
                /* Fall back to default shiptype if not specified */
                shipType = cfg.getDefaultShipType();
            }
            
            /* Check if the maximum amount of ships of this type is reached */
            if (t.getShipCount(shipType) >= cfg.getMaxShips(shipType) && cfg.getMaxShips(shipType) != -1) {
                m_botAction.sendPrivateMessage(name, "Error: Could not add " + p.getPlayerName() + " as " +
                    Tools.shipName(shipType) + ", team has already reached the maximum number of " +
                    Tools.shipName(shipType) + "s allowed.");
                return;
            }
            
            /*
             * All checks are done
             */
            
            /* Add player */
            t.addPlayer(p, shipType);
            
            /* Toggle turn */
            if (state.getCurrentState() == BWJSState.ADDING_PLAYERS) {
                t.picked();
                determineTurn();
            }
        }
    }
    
    /**
     * Handles the !cap command
     * 
     * @param name player that issued the !cap command
     */
    private void cmd_cap(String name) {
        BWJSTeam t;
        name = name.toLowerCase();
        
        /* Check if bot is turned on */
        if (state.getCurrentState() == BWJSState.OFF) {
            return;
        }
        
        /* Check if auto captains is allowed */
        if (!cfg.getAllowAutoCaps()) {
            sendCaptainList(name);
            return;
        }
        
        /* Check if sender is on the not playing list */
        if (listNotplaying.contains(name)) {
            sendCaptainList(name);
            return;
        }
        
        /* Check if captain spots are already taken */
        if (team[0].hasCaptain() && team[1].hasCaptain()) {
            sendCaptainList(name);
            return;
        }
        
        /* 
         * Check if the sender is already on one of the teams 
         * If so he can only get captain of his own team
         */
        t = getTeam(name);
        if (t != null) {
            if (t.hasCaptain()) {
                sendCaptainList(name);
                return;
            } else {
                t.setCaptain(name);
                return;
            }
        }
        
        /* Check if game state is waiting for caps, or adding players */
        if (state.getCurrentState() == BWJSState.WAITING_FOR_CAPS ||
                state.getCurrentState() == BWJSState.ADDING_PLAYERS) {
            if (!team[0].hasCaptain()) {
                team[0].setCaptain(name);
                return;
            } else if (!team[1].hasCaptain()) {
                team[1].setCaptain(name);
                return;
            } else {
                sendCaptainList(name);
                return;
            }
        } else {
            sendCaptainList(name);
            return;
        }
    }
    
    /**
     * Handles the !change command
     * 
     * @param name name of the player that issued the command
     * @param cmd command
     * @param override teamnumber to override, else -1
     */
    private void cmd_change(String name, String cmd, int override) {
        BWJSTeam t;
        String[] splitCmd;
        BWJSPlayer p;
        int shipType;
        
        /* Check if this is a BASE game */
        if (cfg.getGameType() != BWJSConfig.BASE) {
            return;
        }
        
        t = getTeam(name, override);
        
        /* Check if sender is in a team and that the command was not overriden */
        if (t == null && override == -1) {
            return;
        }
        
        /* Check if the sender is a captain and that the command was not overriden */
        if (!t.isCaptain(name) && override == -1) {
            return;
        }
        
        /* Check command syntax */
        if (cmd.length() < 8) {
            m_botAction.sendPrivateMessage(name, 
                    "Error: Please specify a playername and shiptype, !change <player>:<# shiptype>");
            return;
        }
        
        splitCmd = cmd.substring(8).split(":"); //Split command in 1. <player> 2. <# shiptype>
        
        /* Check command syntax */
        if (splitCmd.length < 2) {
            m_botAction.sendPrivateMessage(name,
                    "Error: Please specify a playername and shiptype, !change <player>:<# shiptype>");
            return;
        }
        
        p = t.searchPlayer(splitCmd[0]);    //Search for the player
        
        /* Check if the player has been found */
        if (p == null) {
            m_botAction.sendPrivateMessage(name, "Error: Unknown player");
            return;
        }
        
        /* Check if the player is already out */
        if (p.isOut()) {
            m_botAction.sendPrivateMessage(name, "Error: Player is already out and cannot be shipchanged.");
            return;
        }
        
        /* Check if the shiptype is set correctly */
        try {
            shipType = Integer.parseInt(splitCmd[1]);
        } catch (Exception e) {
            m_botAction.sendPrivateMessage(name, "Error: invalid shiptype");
            return;
        }
        
        if (shipType < 1 || shipType > 8) {
            m_botAction.sendPrivateMessage(name, "Error: invalid shiptype");
            return;
        }
        
        /* Check if the specified shiptype is allowed */
        if (t.getShipCount(shipType) >= cfg.getMaxShips(shipType) && cfg.getMaxShips(shipType) != -1) {
            m_botAction.sendPrivateMessage(name, "Error: Could not change " + p.getName() + " to " +
                    Tools.shipName(shipType) + ", team has already reached the maximum number of " +
                    Tools.shipName(shipType) + "s allowed.");
            return;
        }
        
        /* Check if the player is already on that ship */
        if (p.getCurrentShipType() == shipType) {
            m_botAction.sendPrivateMessage(name, "Error: Could not change " + p.getName() + " to " +
                    Tools.shipName(shipType) + ", player already in that ship.");
            return;
        }
        
        /* Check when the last !change happened and if this change is allowed */
        if (!p.isChangeAllowed()) {
            m_botAction.sendPrivateMessage(name, "Error: Changed not allowed yet, wait " + 
                    p.getTimeUntilNextChange() + " more seconds before next !change");
            return;
        }
        
        /*
         * All checks done
         */
        p.change(shipType); //Change player
        
        /* Notify sender of successful change */
        m_botAction.sendPrivateMessage(name, p.getName() + " has been changed to " + Tools.shipName(shipType));
    }
    
    /**
     * Handles the !help command
     * 
     * @param name name of the player that issued the !help command
     */
    private void cmd_help(String name) {
        ArrayList<String> help = new ArrayList<String>();   //Help messages
        
        if (state.getCurrentState() == BWJSState.WAITING_FOR_CAPS) {
            if (cfg.getAllowAutoCaps()) {
                help.add("!cap                      -- Become captain of a team");
            } else {
                help.add("!cap                      -- List captains");
            }
            if (isCaptain(name)) {
                help.add("!removecap                -- Removes you as a captain");
            }
        } else if (state.getCurrentState() >= BWJSState.ADDING_PLAYERS) {
            if (isCaptain(name)) {
                help.add("!add <player>             -- Adds player");
                if (cfg.getGameType() == BWJSConfig.BASE) {
                    help.add("!add <player>:<ship>      -- Adds player in the specified ship");
                }
            }
            help.add("!cap                      -- Become captain of a team / shows current captains!");
            if (cfg.getGameType() == BWJSConfig.BASE && isCaptain(name)) {
                help.add("!change <player>:<ship>   -- Sets the player in the specified ship");
            }
            help.add("!lagout                   -- Puts you back into the game if you have lagged out");
            help.add("!list                     -- Lists all players on this team");
            help.add("!listbest <ship>          -- Lists the players in the arena and groups them according to their rank");
            help.add("!myfreq                   -- Puts you on your team's frequency");
            help.add("!mvp                      -- Displays the current mvp");
            help.add("!rating <player>          -- Displays your/<player> current rating");
            help.add("!score <player>           -- Displays your/<player> current score");
            help.add("!stats <player>           -- Displays your/<player> stats");
            if (state.getCurrentState() == BWJSState.ADDING_PLAYERS && isCaptain(name)) { 
                help.add("!ready                    -- Use this when you're done setting your lineup");
                help.add("!remove <player>          -- Removes specified player)");
            }
            if (isCaptain(name)) {
                help.add("!removecap                -- Removes you as a captain");
                help.add("!sub <playerA>:<playerB>  -- Substitutes <playerA> with <playerB>");
                if (cfg.getGameType() == BWJSConfig.BASE) {
                    help.add("!switch <player>:<player> -- Exchanges the ship of both players");
                }
            }
        }
        
        help.add("!status                   -- Display status and score");
        
        if (state.getCurrentState() != BWJSState.OFF) {
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
            if(!cfg.getAllowAutoCaps()) {
                help.add("!zone                             -- sends time-restricted advert");
            }
            if (state.getCurrentState() > BWJSState.OFF) {
                help.add("!setcaptain <# freq>:<player>     -- Sets <player> as captain for <# freq>");
                help.add("-- Prepend your command with !t1- for 'Freq 0', !t2- for 'Freq 1' --");
                if ( state.getCurrentState() >= BWJSState.ADDING_PLAYERS) {
                    help.add("!add <player>             -- Adds player");
                    if (cfg.getGameType() == BWJSConfig.BASE) {
                        help.add("!add <player>:<ship>      -- Adds player in the specified ship");
                        help.add("!change <player>:<ship>   -- Sets the player in the specified ship");
                    }
                    if (state.getCurrentState() == BWJSState.ADDING_PLAYERS) { 
                        help.add("!ready                    -- Ready the team");
                        help.add("!remove <player>          -- Removes specified player)");
                    }
                    help.add("!sub <playerA>:<playerB>  -- Substitutes <playerA> with <playerB>");
                    if (cfg.getGameType() == BWJSConfig.BASE) {
                        help.add("!switch <player>:<player> -- Exchanges the ship of both players");
                    }
                }
                help.add("!setcaptain <player>      -- Sets <player> to captain");
                help.add("!removecap                -- Removes the cap of team !t#");
            }
        }
        
        String[] spam = help.toArray(new String[help.size()]);
        m_botAction.privateMessageSpam(name, spam);
    }
    
    /**
     * Handles the !lagout/!return command
     * 
     * @param name name of the player that issued the !lagout command
     */
    private void cmd_lagout(String name) {
        if (state.getCurrentState() == BWJSState.ADDING_PLAYERS || 
                state.getCurrentState() == BWJSState.GAME_IN_PROGRESS || 
                state.getCurrentState() == BWJSState.PRE_GAME) {
            BWJSTeam t;
            
            t = getTeam(name);
            
            /* Check if player was on a team */
            if (t == null) {
                return;
            }
            
            /* Check if the player was at least IN, or LAGGED OUT */
            if (!t.laggedOut(name)) {
                return;
            }
            
            /* Check if a return is possible */
            if (!t.laginAllowed(name)) {
                m_botAction.sendPrivateMessage(name, t.getLaginErrorMessage(name)); //Send error message
                return;
            }
            
            t.lagin(name); //Puts the player in again
        }
    }
    
    /**
     * Handles !list command
     * 
     * @param name player that issued the !list command
     */
    private void cmd_list(String name) {
        BWJSTeam t;
        
        if (state.getCurrentState() >= BWJSState.ADDING_PLAYERS && state.getCurrentState() < BWJSState.GAME_OVER) {
            t = getTeam(name);   //Retrieve teamnumber
            
            /* Check if the player is a staff member (In order to show the list of both teams to the staff member) */
            if (m_botAction.getOperatorList().isER(name)) {
                t = null;
            }
            
            /* Set up sorting */
            Comparator<BWJSPlayer> comparator = new Comparator<BWJSPlayer>() {
                public int compare(BWJSPlayer pa, BWJSPlayer pb) {
                    if (pa.getCurrentState() < pb.getCurrentState())
                        return -1;
                    else if (pa.getCurrentState() > pb.getCurrentState())
                        return 1;
                    else if (pa.getCurrentShipType() < pb.getCurrentShipType())
                        return -1;
                    else if (pa.getCurrentShipType() > pb.getCurrentShipType())
                        return 1;
                    else if (pb.getName().compareTo(pa.getName()) < 0)
                        return 1;
                    else
                        return 0;
                }
            };
            
            /* Display set up */
            ArrayList<String> list = new ArrayList<String>();
            if (t == null) {
                /* Display both teams */
                for (int i = 0; i < 2; i++) {
                    list.add(team[i].getName() + " (captain: " + team[i].captainName + ")");
                    list.add(Tools.formatString("Name:", 23) + " - " +
                            Tools.formatString("Ship:", 10) + " - " + "Status:");

                    BWJSPlayer[] players = team[i].players.values().toArray(
                            new BWJSPlayer[team[i].players.values().size()]);
                    Arrays.sort(players, comparator);
                    
                    for (BWJSPlayer p : players) 
                        list.add(Tools.formatString(p.p_name, 23) + " - " 
                                + Tools.formatString(Tools.shipName(p.getCurrentShipType()), 10) + " - " 
                                + p.getStatus());
                    list.add("`");
                }
            } else {
                /* Display one team */
                list.add(t.getName() + " (captain: " + t.getCaptainName() + ")");
                list.add(Tools.formatString("Name:", 23) + " - " +
                        Tools.formatString("Ship:", 10) + " - " + "Status:");
                
                BWJSPlayer[] players = t.players.values().toArray(
                        new BWJSPlayer[t.players.values().size()]);
                Arrays.sort(players, comparator);
                
                for (BWJSPlayer p : players) 
                    list.add(Tools.formatString(p.p_name, 23) + " - " 
                            + Tools.formatString(Tools.shipName(p.getCurrentShipType()), 10) + " - " + p.getStatus());
            }
            
            String[] spam = list.toArray(new String[list.size()]);
            m_botAction.privateMessageSpam(name, spam);
        }
    }
    
    /**
     * Handles the !mvp command
     * 
     * @param name name of the player that issued the !mvp command
     */
    private void cmd_mvp(String name) {
        if (state.getCurrentState() == BWJSState.GAME_IN_PROGRESS) {
            m_botAction.sendPrivateMessage(name, "Current MVP: " + getMVP());
        }
    }
    
    /**
     * Handles the !myfreq command
     * 
     * @param name name of the player that issued the !myfreq command
     */
    private void cmd_myfreq(String name) {
        BWJSTeam t;
        
        if (state.getCurrentState() > BWJSState.OFF) {
            t = getTeam(name);
            
            /* Check if the player is on one of the teams */
            if (t == null) {
                return;
            }
            
            /* Check if the player is set to not playing */
            if (listNotplaying.contains(name.toLowerCase())) {
                return;
            }
            
            /* Only set frequency if the player is not currently in */
            if (!t.isPlaying(name)) {
                m_botAction.setFreq(name, t.getFrequency());  //Set the player to his frequency
            }
        }
    }
    
    /**
     * Handles the !notplaying command
     * 
     * @param name name of the player that issued the !notplaying command
     */
    private void cmd_notplaying(String name) {
        BWJSTeam t;
        
        if (state.getCurrentState() > BWJSState.OFF) {
            t = getTeam(name);
            
            /* Check if player is on the notplaying list and if so remove him from that list */
            if (listNotplaying.contains(name.toLowerCase())) {
                listNotplaying.remove(name.toLowerCase());  //Remove from him from the notplaying list
                m_botAction.sendPrivateMessage(name, 
                    "You have been removed from the not playing list.");   //Notify the player
                /* Put the player on the spectator frequency */
                m_botAction.setShip(name, 1);
                m_botAction.specWithoutLock(name);
                return;
            }
            
            /* Add the player to the notplaying list */
            listNotplaying.add(name.toLowerCase()); //Add the player to the notplaying list
            m_botAction.sendPrivateMessage(name, "You have been added to the not playing list. " +
                    "(Captains will be unable to add or sub you in.)"); //Notify the player
            m_botAction.specWithoutLock(name);  //Spectate the player
            m_botAction.setFreq(name, FREQ_NOTPLAYING);  //Set the player to the notplaying frequency
            
            /* Check if the player was on one of the teams */
            if (t != null) {
                /* Check if the player was a captain */
                if (isCaptain(name)) {
                    t.captainLeft();   //Remove the player as captain
                }
                
                if (state.getCurrentState() == BWJSState.ADDING_PLAYERS) {
                    if (t.isOnTeam(name)) {
                        m_botAction.sendArenaMessage(name + " has been removed from the game. (not playing)");
                        t.removePlayer(name);
                    }
                }
                
                /* Check if a player was in and set him to "out but subable" status */
                if (state.getCurrentState() > BWJSState.ADDING_PLAYERS &&
                        state.getCurrentState() < BWJSState.GAME_OVER) {
                    if (t.isOnTeam(name)) {
                        if (t.isPlaying(name) || t.laggedOut(name)) {
                            m_botAction.sendArenaMessage(
                                name + " has been removed from the game. (not playing)"); //Notify the player
                            t.setOutNotPlaying(name); //Set player to out, but subable status
                        }
                    }
                }
                
                m_botAction.setFreq(name, FREQ_NOTPLAYING);     //Set the player to the notplaying frequency
            }
        }
    }
    
    /**
     * Handles the !off command
     * 
     * @param name name of the player that issued the !off command
     */
    private void cmd_off(String name) {
        switch (state.getCurrentState()) {
            case BWJSState.OFF:
                m_botAction.sendPrivateMessage(name, "Bot is already OFF");
                break;
            case BWJSState.WAITING_FOR_CAPS:
                cmd_stop(name);
                break;
            default :
                m_botAction.sendPrivateMessage(name, "Turning OFF after this game");
                lockLastGame = true;
        }
    }
    
    /**
     * Handles the override commands (!t1/!t2)
     * 
     * @param name Name of the player that issued the command
     * @param cmd Command that should be overriden
     */
    private void cmd_overrideCmd(String name, String cmd) {
        int override; //Teamnumber of the team that has to be overriden
        
        /* Check what team has to be overriden */
        if (cmd.startsWith("!t1")) {
            override = 0;
        } else if (cmd.startsWith("!t2")) {
            override = 1;
        } else {
            return;
        }
        
        /* Check if the syntax is correct */
        if (cmd.length() < 5) {
            return;
        }
        
        cmd = cmd.substring(4); //Cut off !t1/!t2 part
        
        handleCommand(name, cmd, override); //Handle command
    }
    
    /**
     * Handles the !rating command
     * 
     * @param name name of the player that issued the !rating command
     * @param cmd extra parameters to the command
     */
    private void cmd_rating(String name, String cmd) {
        BWJSPlayer p;
        
        if (state.getCurrentState() == BWJSState.GAME_IN_PROGRESS) {
            cmd = cmd.substring(7).trim();
            
            /* Check if a player is specified else use the sender of the command */
            if (!cmd.isEmpty()) {
                p = searchBWJSPlayer(cmd);
            } else {
                p = searchBWJSPlayer(name);
            }
            
            /* Check if a player has been found */
            if (p == null) {
                m_botAction.sendPrivateMessage(name, "Error: player not found");
                return;
            }
            
            /* Send rating */
            if (p.p_name.equalsIgnoreCase(name)) {
                m_botAction.sendPrivateMessage(name, "Current rating: " + p.getTotalRating());
            } else {
                m_botAction.sendPrivateMessage(name, "Current Rating of " + p.p_name + ": " 
                    + p.getTotalRating());
            }
        }
    }
    
    /**
     * Handles the !ready command 
     * 
     * @param name name of the player that issued the command
     * @param override override 0/1 for teams, -1 if not overriden
     */
    private void cmd_ready(String name, int override) {
        BWJSTeam t;
        
        if (state.getCurrentState() == BWJSState.ADDING_PLAYERS) {
            t = getTeam(name, override); //Retrieve teamnumber
            
            t.ready();   //Ready team
            
            /* Check if both teams are ready */
            if (team[0].isReady() && team[1].isReady()) {
                checkLineup(); //Check lineups
            }
        }
    }
    
    /**
     * Handles the !remove command
     * 
     * @param name name of the player that issued the !remove command
     * @param cmd command parameters
     * @param override override 0/1 for teams, -1 if not overriden
     */
    private void cmd_remove(String name, String cmd, int override) {
        BWJSTeam t;
        BWJSPlayer p;   //Player to be removed
        
        if (state.getCurrentState() == BWJSState.ADDING_PLAYERS) {
            t = getTeam(name, override); //Retrieve team
            
            /* Check command syntax */
            if (cmd.length() < 8) {
                m_botAction.sendPrivateMessage(name, "Error: Please specify a player, !remove <player>");
                return;
            }
            
            cmd = cmd.substring(8);
            
            /* Check command syntax */
            if (cmd.isEmpty()) {
                m_botAction.sendPrivateMessage(name, "Error: Please specify a player, !remove <player>");
                return;
            }
            
            p = t.searchPlayer(cmd); //Search for player to remove
            
            /* Check if player has been found */
            if (p == null) {
                m_botAction.sendPrivateMessage(name, "Error: Player could not be found");
                return;
            }
            
            t.removePlayer(p.getName());
            
            determineTurn();
        }
    }
    
    /**
     * Handles the !removecap command
     * 
     * @param name name of the player that issued the !removecap command
     * @param override override 0/1 for teams, -1 if not overriden
     */
    private void cmd_removecap(String name, int override) {
        BWJSTeam t;
        
        if (state.getCurrentState() > BWJSState.OFF && state.getCurrentState() < BWJSState.GAME_OVER) {
            t = getTeam(name, override); //Retrieve team number
            
            t.captainLeft();   //Remove captain
        }
    }
    
    /**
     * Handles the !score command
     * 
     * @param name name of the player that issued the !score command
     * @param cmd command parameters
     * @param override override 0/1 for teams, -1 if not overriden
     */
    private void cmd_score(String name, String cmd) {
        BWJSPlayer p;
        
        if (state.getCurrentState() == BWJSState.GAME_IN_PROGRESS) {
            cmd = cmd.substring(6).trim();
            
            /* Check if a player is specified else use the sender of the command */
            if (!cmd.isEmpty()) {
                p = searchBWJSPlayer(cmd);
            } else {
                p = searchBWJSPlayer(name);
            }
            
            /* Check if a player has been found */
            if (p == null) {
                m_botAction.sendPrivateMessage(name, "Error: player not found");
                return;
            }
            
            /* Send score */
            if (p.p_name.equalsIgnoreCase(name)) {
                m_botAction.sendPrivateMessage(name, "Current score: " + p.getTotalScore());
            } else {
                m_botAction.sendPrivateMessage(name, "Current score of " + p.p_name + ": " 
                    + p.getTotalScore());
            }
        }
    }
    
    /**
     * Handles the !setcaptain command
     * 
     * @param name name of the player that issued the !setcaptain command
     * @param cmd command parameters
     * @param override override 0/1 for teams, -1 if not overriden
     */
    private void cmd_setCaptain(String name, String cmd, int override) {
        int frequency;
        Player p;
        String[] splitCmd;
        
        /* Alter command if overriden */
        if (override != -1) {
            cmd = "!setcaptain " + override + ":" + cmd.substring(11).trim();
        }
        
        if (state.getCurrentState() > BWJSState.OFF && state.getCurrentState() < BWJSState.GAME_OVER) {
            cmd = cmd.substring(11).trim(); //Cut of !setcaptain part
            
            /* Check command syntax */
            if (cmd.isEmpty()) {
                m_botAction.sendPrivateMessage(name, "Error: please specify a player, " +
                    "'!setcaptain <# freq>:<player>', or '!t1/!t2 !setcaptain <player>'");
                return;
            }
            
            splitCmd = cmd.split(":"); //Split parameters
            
            /* Check command syntax */
            if (splitCmd.length < 2) {
                m_botAction.sendPrivateMessage(name, "Error: please specify a player, " +
                    "'!setcaptain <# freq>:<player>', or '!t1/!t2 !setcaptain <player>'");
                return;
            }
            
            p = m_botAction.getFuzzyPlayer(splitCmd[1]); //Search player
            
            /* Check if player has been found */
            if (p == null) {
                m_botAction.sendPrivateMessage(name, "Error: Unknown player");
                return;
            }
            
            /* Retrieve teamnumber or frequency number */
            try {
                frequency = Integer.parseInt(splitCmd[0]);
            } catch (Exception e) {
                m_botAction.sendPrivateMessage(name, "Error: please specify a correct frequency, " +
                    "'!setcaptain <# freq>:<player>', or '!t1/!t2 !setcaptain <player>'");
                return;
            }
            
            /* Check if frequency is valid */
            if (frequency < 0 || frequency > 1) {
                m_botAction.sendPrivateMessage(name, "Error: please specify a correct frequency, " +
                    "'!setcaptain <# freq>:<player>', or '!t1/!t2 !setcaptain <player>'");
                return;
            }
            
            team[frequency].setCaptain(p.getPlayerName()); //Set player to captain
        }
    }
    
    /**
     * Handles the !start command 
     * 
     * @param name player that issued the !start command
     */
    private void cmd_start(String name) {
        if (state.getCurrentState() == BWJSState.OFF) {
            start();
        } else {
            m_botAction.sendPrivateMessage(name, "Error: Bot is already ON");
        }
    }
    
    /**
     * Handles the !status command
     * 
     * @param name name of the player that issued the command
     */
    private void cmd_status(String name) {
        String[] status;    //Status message
        
        status = new String[2];
        status[0] = ""; //Default value
        status[1] = ""; //Default value
        
        switch(state.getCurrentState()) {
            case BWJSState.OFF :
                status[0] = "Bot turned off, no games can be started at this moment.";
                break;
            case BWJSState.WAITING_FOR_CAPS :
                if (cfg.getAllowAutoCaps()) {
                    status[0] =  "A new game will start when two people message me with !cap";
                } else {
                    status[0] =  "Request a name game with :eventbot:!request " + cfg.getGameTypeString();
                }
                break;
            case BWJSState.ADDING_PLAYERS :
                status[0] = "Teams: " + team[0].getName() + " vs. " + team[1].getName() +
                    ". We are currently arranging lineups";
                break;
            case BWJSState.PRE_GAME :
                status[0] = "Teams: " + team[0].getName() + " vs. " + team[1].getName() +
                    ". We are currently starting the game";
                break;
            case BWJSState.GAME_IN_PROGRESS :
                status[0] = "Game is in progress, " + ((cfg.getTime() - timeLeft) / 60) + " minutes played.";
                status[1] = "Score " + team[0].getName() + " vs. " + team[1].getName() + ": " + score();
                break;
            case BWJSState.GAME_OVER :
                status[0] = "Teams: " + team[0].getName() + " vs. " + team[1].getName() +
                    ". We are currently ending the game";
                break;
        }
        
        /* Send status message */
        if (!status[0].isEmpty()) {
            m_botAction.sendPrivateMessage(name, status[0]);
        }
        
        if (!status[1].isEmpty()) {
            m_botAction.sendPrivateMessage(name, status[1]);
        }
    }
    
    /**
     * Handles the !stop command
     * 
     * @param name player that issued the !stop command
     */
    private void cmd_stop(String name) {
        if (state.getCurrentState() != BWJSState.OFF) {
            m_botAction.sendArenaMessage("Bot has been turned OFF");
            state.setState(BWJSState.OFF);
            reset();
            unlockArena();
        } else {
            m_botAction.sendPrivateMessage(name, "Error: Bot is already OFF");
        }
    }
    
    /**
     * Handles the !sub command
     * 
     * @param name name of the player that issued the !sub command
     * @param cmd command parameters
     * @param override 0/1 for teams, -1 for not overriden
     */
    private void cmd_sub(String name, String cmd, int override) {
        BWJSTeam t;
        String[] splitCmd;
        BWJSPlayer playerA;
        BWJSPlayer playerB;
        Player playerBnew;
        
        if (state.getCurrentState() == BWJSState.GAME_IN_PROGRESS) {
            t = getTeam(name, override); //Retrieve teamnumber
            
            if (t == null) {
                return;
            }
            
            cmd = cmd.substring(4).trim();  //Remove !sub part of the cmd
            
            /* Check command syntax */
            if (cmd.isEmpty()) {
                m_botAction.sendPrivateMessage(name, "Error: Specify players, !sub <playerA>:<playerB>");
                return;
            }
            
            splitCmd = cmd.split(":");
            
            /* Check command syntax */
            if (splitCmd.length < 2) {
                m_botAction.sendPrivateMessage(name, "Error: Specify players, !sub <playerA>:<playerB>");
                return;
            }
            
            /* Check if team has any substitutes left */
            if (!t.hasSubtitutesLeft()) {
                m_botAction.sendPrivateMessage(name, "Error: You have 0 substitutes left.");
                return;
            }
            
            playerA = t.searchPlayer(splitCmd[0]);   //Search for <playerA>
            playerBnew = m_botAction.getFuzzyPlayer(splitCmd[1]);   //Search for <playerB>
            
            /* Check if players can be found */
            if (playerA == null || playerBnew == null) {
                m_botAction.sendPrivateMessage(name, "Error: Player could not be found");
                return;
            }
            
            /* Check if <playerA> is already out and thus cannot be subbed */
            if (playerA.isOut()) {
                m_botAction.sendPrivateMessage(name, "Error: Cannot substitute a player that is already out");
                return;
            }
            
            /* Check if <playerB> is on the notplaying list */
            if (listNotplaying.contains(playerBnew.getPlayerName().toLowerCase())) {
                m_botAction.sendPrivateMessage(name,
                    "Error: " + playerBnew.getPlayerName() + " is set to not playing.");
                return;
            }
            
            /* Check if <playerB> is already on the other team */
            if (getOtherTeam(t).isOnTeam(playerBnew.getPlayerName())) {
                m_botAction.sendPrivateMessage(name, "Error: Substitute is already on the other team");
                return;
            }
            
            /* Check if <playerB> was already on the team */
            playerB = t.searchPlayer(playerBnew.getPlayerName());
            if (playerB != null) {
                /* <playerB> was on the team */
                if (cfg.getGameType() != BWJSConfig.BASE || !playerB.isOut()) {
                    m_botAction.sendPrivateMessage(name, "Error: Substitute is/was already playing for your team");
                    return;
                }
                
                /* Check when last !sub was and if this sub is allowed */
                if (!playerB.isSubAllowed()) {
                    m_botAction.sendPrivateMessage(name, "Error: Sub not allowed yet, wait " +
                        playerB.getTimeUntilNextSub() + " more seconds before next !sub");
                    return;
                }
            }
            
            /* Check if the player should be subbed in case of 0 deaths left */
            if (cfg.maxDeaths != 0 && playerA.getDeaths() >= cfg.maxDeaths) {
                m_botAction.sendPrivateMessage(name, "Error: Cannot substitute a player that is already out");
                return;
            }
            
            t.sub(playerA, playerBnew); //Execute the substitute
        }
    }
    
    /**
     * Handles the !subscribe command
     * 
     * @param name player that issued the !subscribe command
     */
    private void cmd_subscribe(String name) {
        if (state.getCurrentState() > BWJSState.OFF) {
            name = name.toLowerCase();
            
            if (listAlert.contains(name)) {
                listAlert.remove(name);
                m_botAction.sendPrivateMessage(name, "You have been removed from the alert list.");
            } else {
                listAlert.add(name);
                m_botAction.sendPrivateMessage(name, "You have been added to the alert list.");
            }
        }
    }
    
    /**
     * Handles the !switch command 
     * 
     * @param name player that issued the !switch command
     * @param cmd command parameters
     * @param override override 0/1 for teams, -1 for not overriden
     */
    private void cmd_switch(String name, String cmd, int override) {
        BWJSTeam t;
        String[] splitCmd;
        BWJSPlayer playerA;
        BWJSPlayer playerB;
        
        /* Check if game type is of type BASE */
        if (cfg.getGameType() != BWJSConfig.BASE) {
            return;
        }
        
        if (state.getCurrentState() == BWJSState.ADDING_PLAYERS || state.getCurrentState() == BWJSState.GAME_IN_PROGRESS) {
            t = getTeam(name, override); //Retrieve team number
            
            cmd = cmd.substring(7).trim(); //Cut off the !switch part of the command
            
            /* Check command syntax */
            if (cmd.isEmpty()) {
                m_botAction.sendPrivateMessage(name,
                    "Error: Specify players to be switched, !switch <playerA>:<playerB>");
                return;
            }
            
            splitCmd = cmd.split(":"); //Split command parameters
            
            /* Check command syntax */
            if (splitCmd.length < 2) {
                m_botAction.sendPrivateMessage(name,
                    "Error: Specify players to be switched, !switch <playerA>:<playerB>");
                return;
            }
            
            playerA = t.searchPlayer(splitCmd[0]); //Search <playerA>
            playerB = t.searchPlayer(splitCmd[1]); //Search <playerB>
            
            /* Check if both players have been found */
            if (playerA == null || playerB == null) {
                m_botAction.sendPrivateMessage(name, "Error: Unknown players");
                return;
            }
            
            /* Check if player is already out or subbed */
            if (playerA.isOut() || playerB.isOut()) {
                m_botAction.sendPrivateMessage(name, "Error: Cannot switch a player that is already out.");
                return;
            }
            
            /* Check if a switch is allowed timewise */
            if (!playerA.isSwitchAllowed()) {
                m_botAction.sendPrivateMessage(name, "Error: Sub not allowed yet, wait " +
                    playerA.getTimeUntilNextSwitch() + " more seconds before next !switch");
                return;
            }
            if (!playerB.isSwitchAllowed()) {
                m_botAction.sendPrivateMessage(name, "Error: Sub not allowed yet, wait " +
                    playerB.getTimeUntilNextSwitch() + " more seconds before next !switch");
                return;
            }
            
            t.switchPlayers(playerA, playerB);   //Switch players
        } else if (state.getCurrentState() > BWJSState.OFF) {
            m_botAction.sendPrivateMessage(name, "Error: could not switch at this moment of the game");
        }
    }
    
    /*
     * Game modes
     */
    
    /**
     * Starts the bot
     */
    private void start() {
        lockLastGame = false;
        lockArena();
        lockDoors();
        setSpecAndFreq();
        startGameTicker();
                
        startWaitingForCaps();
    }

    /**
     * Starts the gameticker
     */
    private void startGameTicker() {
        try {
            gameticker.cancel();
        } catch (Exception e) {}

        gameticker = new Gameticker();
        m_botAction.scheduleTask(gameticker, Tools.TimeInMillis.SECOND, Tools.TimeInMillis.SECOND);
    }
    
    /**
     * Starts waiting for caps
     */    
    private void startWaitingForCaps() {
        reset();
        
        state.setState(BWJSState.WAITING_FOR_CAPS);
        if (cfg.getAllowAutoCaps()) {
            m_botAction.sendArenaMessage("A new game will start when two people message me with !cap -" +
                    m_botAction.getBotName(), Tools.Sound.BEEP2);
        } else {
            m_botAction.sendArenaMessage("Request a new game with :eventbot:!request " + cfg.getGameTypeString() 
                    + " -" + m_botAction.getBotName(), Tools.Sound.BEEP2);
        }
    }
    
    /**
     * Start adding players state
     * - Notify arena
     * - Notify chats
     * - Determine next pick
     */
    private void startAddingPlayers() {
        state.setState(BWJSState.ADDING_PLAYERS);
        
        //SQL Put match in database
        sql.addGame();
        
        if (cfg.getGameType() == BWJSConfig.BASE) {
            m_botAction.sendArenaMessage("Captains you have 10 minutes to set up your lineup correctly!", 
                    Tools.Sound.BEEP2);
        } else {
            m_botAction.sendArenaMessage("Captains you have 10 minutes to set up your lineup correctly!", 
                    Tools.Sound.BEEP2);
        }
        
        if (cfg.getAllowAutoCaps()) {
            newGameAlert();
        }
        
        for (BWJSTeam t : team) {
            if (t.hasCaptain()) {
                t.putCaptainInList();
            }
        }
        
        determineTurn();
    }
    
    /**
     * Starts pre game
     */
    private void startPreGame() {
        m_botAction.resetFlagGame();
        m_botAction.shipResetAll();
        m_botAction.scoreResetAll();
        
        team[0].warpTo(cfg.getWarpSpot(0), cfg.getWarpSpot(1));
        team[1].warpTo(cfg.getWarpSpot(2), cfg.getWarpSpot(3));
        
        m_botAction.showObject(cfg.getObject(0));
    }
    
    /**
     * Starts a game
     */
    private void startGame() {
        state.setState(BWJSState.GAME_IN_PROGRESS);
        
        scoreboard = m_botAction.getObjectSet();
        
        m_botAction.shipResetAll();
        m_botAction.scoreResetAll();
        
        team[0].warpTo(cfg.getWarpSpot(4), cfg.getWarpSpot(5));
        team[1].warpTo(cfg.getWarpSpot(6), cfg.getWarpSpot(7));
        
        m_botAction.showObject(cfg.getObject(2));
        m_botAction.sendArenaMessage("Go go go!!!", Tools.Sound.GOGOGO);
        
        timeLeft = cfg.getTime() * 60;
    }
    
    /**
     * What to do with when game is over
     */
    private void gameOver() {
        int winningFreq;
        
        state.setState(BWJSState.GAME_OVER);
        winningFreq = -1;
        
        //Cancel timer
        m_botAction.setTimer(0);
        
        
        //Determine winner
        if (cfg.getGameType() == BWJSConfig.BASE) {
            if (team[0].getFlagTime() >= cfg.getTimeTarget() * 60) {
                winningFreq = 0;
            } else if (team[1].flagTime >= cfg.getTimeTarget() * 60) {
                winningFreq = 1;
            }
        } else {
            if (team[0].isDead())
                winningFreq = 1;
            else if (team[1].isDead())
                winningFreq = 0;
        }
        
        //Determine winner on a "draw"
        if (winningFreq == -1) {
            if (cfg.getGameType() == BWJSConfig.BASE) {
                if (team[0].getFlagTime() > team[1].getFlagTime()) {
                    winningFreq = 0;
                } else if (team[0].getFlagTime() < team[1].getFlagTime()) {
                    winningFreq = 1;
                }
            } else {
                if (team[0].getDeaths() < team[1].getDeaths()) {
                    winningFreq = 0;
                } else if (team[0].getDeaths() > team[1].getDeaths()) {
                    winningFreq = 1;
                }
            }
        }
        
        m_botAction.sendArenaMessage("Result of " + team[0].getName() + " vs. " + team[1].getName() + ": " + score());
        
        displayScores();
        
        if (winningFreq != -1)
            m_botAction.sendArenaMessage(team[winningFreq].getName() + " wins the game!", Tools.Sound.HALLELUJAH);
        else 
            m_botAction.sendArenaMessage("A draw?!", Tools.Sound.GIBBERISH);
        
        //SQL update
        sql.endGame(winningFreq);
        sql.putPlayers();
        sql.putCaptains();
    }
    
    /*
     * Tools
     */
    
    /**
     * Returns name of the current MVP
     * 
     * @return name of the MVP
     */
    private String getMVP() {
        String mvp;
        int highestRating;
        
        highestRating = 0;
        mvp = "";
        
        for (BWJSTeam i : team) {
            for (BWJSPlayer p : i.players.values()) {
                if (highestRating < p.getTotalRating()) {
                    highestRating = p.getTotalRating();
                    mvp = p.getName();
                }
            }   
        }
        
        return mvp;
    }
    
    /**
     * Check if there are enough captains to start the game
     */
    private void checkIfEnoughCaps() {
        if (state.getCurrentState() == BWJSState.WAITING_FOR_CAPS) {
            if (team[0].hasCaptain() && team[1].hasCaptain()) {
                startAddingPlayers();
            }
        }
    }
    
    /** 
     * Alerts players that a new game is starting
     * - Send alert to chats
     * - Send alert to subscribers
     * - Send alert to zone 
     */
    private void newGameAlert() {
        //Alert Chats
        for (int i = 1; i < 11; i++) {
            m_botAction.sendChatMessage(i, "A game of " + cfg.getGameTypeString() + " is starting! Type ?go " +
                    m_botAction.getArenaName() + " to play.");
        }
        
        //Alert Subscribers
        if (listAlert.size() > 0) {
            for (int i = 0; i < listAlert.size(); i++) {
                m_botAction.sendSmartPrivateMessage(listAlert.get(i), "A game of " + cfg.getGameTypeString() +
                        " is starting! Type ?go " + m_botAction.getArenaName() + " to play.");
            }
        }
        
        //Alert zoner, (max once every ZONER_WAIT_TIME (minutes))
        if ((!cfg.getAllowAutoCaps() || allowZoner()) && cfg.getAllowZoner()) {
            m_botAction.sendZoneMessage("A game of " + cfg.getGameTypeString() + " is starting! Type ?go " +
                    m_botAction.getArenaName() + " to play. -" + m_botAction.getBotName(), Tools.Sound.BEEP2);
            zonerTimestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Returns if a zoner can be send or not
     * 
     * @return True if a zoner can be send, else false
     */
    private boolean allowZoner() {
        if ((System.currentTimeMillis() - zonerTimestamp) <= (ZONER_WAIT_TIME * Tools.TimeInMillis.MINUTE)) {
            return false;
        } else {
            return true;
        }
    }
        
    /**
     * Returns the score in form of a String
     * 
     * @return game score
     */
    private String score() {
        String score = "";
        if (cfg.getTimeTarget() != 0) {
            String team1Minutes = String.format("%02d", (int)Math.floor( team[0].getFlagTime() / 60.0 ));
            String team2Minutes = String.format("%02d", (int)Math.floor( team[1].getFlagTime() / 60.0 ));
            String team1Seconds = String.format("%02d", (team[0].getFlagTime() - 
                    (int)Math.floor( team[0].getFlagTime() / 60.0 ) * 60));
            String team2Seconds = String.format("%02d", (team[1].getFlagTime() - 
                    (int)Math.floor( team[1].getFlagTime() / 60.0 ) * 60));
            
            score = team1Minutes + ":" + team1Seconds + " - " + team2Minutes + ":" + team2Seconds;  
        } else {
            score = team[1].getDeaths() + " - " + team[0].getDeaths();
        }
        return score;
    }
    
    /**
     * Checks if name was a captain on one of the teams and notifies the team of the leave
     * 
     * @param name name of the player that left the game and could be captain
     */
    private void checkCaptainLeft(String name) {
        for (BWJSTeam i : team) {
            if (i.getCaptainName().equalsIgnoreCase(name)) {
                if (state.getCurrentState() != BWJSState.WAITING_FOR_CAPS) {
                    i.captainLeftArena();
                } else {
                    i.captainLeft();
                }
            }
        }
    }
    
    /** Sends the captain list to the player */
    private void sendCaptainList(String name) {
        m_botAction.sendPrivateMessage(name, team[0].getCaptainName() + " is captain of " + team[0].getName() + ".");
        m_botAction.sendPrivateMessage(name, team[1].getCaptainName() + " is captain of " + team[1].getName() + ".");
    }
    
    /**
     * Puts a player on a frequency if not playing
     * 
     * @param name name of the player that should be put on a frequency
     */
    private void putOnFreq(String name) {
        name = name.toLowerCase();
        
        if (listNotplaying.contains(name)) {
            m_botAction.setFreq(name, FREQ_NOTPLAYING);
            return;
        }
    }
    
    /**
     * Sends a welcome message with status info to the player
     * 
     * @param name Name of the player that should receive the welcome message
     */
    private void sendWelcomeMessage(String name) {
       m_botAction.sendPrivateMessage(name, "Welcome to " + cfg.getGameTypeString());
       cmd_status(name);    //Sends status info to the player
    }
    
    /** Initializes all the variables used in this class */
    private void initializeVariables() {
        cfg = new BWJSConfig();                 //Game configuration
        state = new BWJSState();                //Game state
        sql = new BWJSSQL();                    //Game sql methods
        
        team = new BWJSTeam[2];                 //Teams
        team[0] = new BWJSTeam(0);              //Team: Freq 0
        team[1] = new BWJSTeam(1);              //Team: Freq 1
        
        racismWatcher = new Spy(m_botAction);   //Racism watcher
        
        listNotplaying = new ArrayList<String>();
        listNotplaying.add(m_botAction.getBotName().toLowerCase());
        listAlert = new ArrayList<String>();
        
        lockArena = false;
        lockLastGame = false;
        
        /* LVZ */
        scoreboard = m_botAction.getObjectSet();
    }
    
    /** Requests Subspace events */ 
    private void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        
        req.request(EventRequester.ARENA_JOINED);           //Bot joined arena
        req.request(EventRequester.FLAG_CLAIMED);           //Flag claim
        req.request(EventRequester.FLAG_REWARD);            //Flag Reward
        req.request(EventRequester.FREQUENCY_CHANGE);       //Player changed frequency
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);  //Player changed frequency/ship
        req.request(EventRequester.LOGGED_ON);              //Bot logged on
        req.request(EventRequester.MESSAGE);                //Bot received message
        req.request(EventRequester.PLAYER_DEATH);           //Player died
        req.request(EventRequester.PLAYER_ENTERED);         //Player entered arena
        req.request(EventRequester.PLAYER_LEFT);            //Player left arena
        req.request(EventRequester.PLAYER_POSITION);        //Player position
        req.request(EventRequester.WEAPON_FIRED);           //Player fired weapon
    }
    
    /**
     * Handles FrequencyChange event and FrequencyShipChange event
     * - Checks if the player has lagged out
     * - Checks if the player is allowed in 
     * 
     * @param name Name of the player
     * @param frequency Frequency of the player
     * @param ship Ship type of the player
     */
    private void checkFCandFSC(String name, int frequency, int ship) {
        checkLagout(name, ship);  //Check if the player has lagged out
        checkPlayer(name, frequency, ship);  //Check if the player is allowed in 
    }
    
    /**
     * Checks if a player has lagged out
     * - Check if the player is on one of the teams
     * - Check if the player is in spectator mode
     * - Check if the player is a player on the team 
     * 
     * @param name Name of the player
     * @param frequency Frequency of the player
     * @param ship Ship type of the player
     */
    private void checkLagout(String name, int ship) {
        BWJSTeam t;
        
        name = name.toLowerCase();
        
        t = getTeam(name);  //Retrieve team
        
        //Check if the player is in one of the teams, if not exit method
        if (t == null) {
            return;
        }
        
        //Check if player is in spectator mode, if not exit method
        if (ship != Tools.Ship.SPECTATOR) {
            return;
        }
        
        //Check if the player is in the team, if not it could just be a captain
        if (!t.isPlayer(name)) {
            return;
        }
        
        //Check if player is already listed as lagged out/sub/out
        if (!t.isPlaying(name)) {
            return;
        } 
        
        t.lagout(name); //Notify the team that a player lagged out
    }
    
    /**
     * Checks if a player is allowed in or not
     * 
     * @param name Name of the player
     * @param frequency Frequency of the player
     * @param ship Ship type of the player
     */
    private void checkPlayer(String name, int frequency, int ship) {
        BWJSTeam t;
                
        name = name.toLowerCase();
        
        //Check if the player is in a ship (atleast not in spectating mode)
        if (ship == Tools.Ship.SPECTATOR) {
            return;
        }
        
        t = getTeam(name); //Retrieve team, null if not found
        
        //Check if the player is on one of the teams, if not spectate the player
        if (t == null) {
            m_botAction.specWithoutLock(name);
            return;
        }
        
        if (t.getPlayerState(name) != BWJSPlayer.IN) {
            m_botAction.specWithoutLock(name);
        }
    }
    
    /** Determines who's turn it is to pick a player*/
    private void determineTurn() {
        int teamNumber;
        
        if (!team[0].getTurn() && !team[1].getTurn()) {
            teamNumber = -1;
            
            if (team[0].players.size() <= team[1].players.size()) {
                teamNumber = 0;
            } else if (team[1].players.size() < team[0].players.size()) {
                teamNumber = 1;
            }
            
            if (teamNumber != -1) {
                if (team[teamNumber].players.size() != cfg.getMaxPlayers()) {
                    m_botAction.sendArenaMessage(team[teamNumber].captainName + " pick a player!", Tools.Sound.BEEP2);
                    team[teamNumber].setTurn();
                }
            }
        }
    }
    
    /**
     * Returns BWJSTeam of the player with "name"
     * 
     * @param name name of the player
     * @return BWJSTeam of the player, null if not on any team
     */
    private BWJSTeam getTeam(String name) {
        BWJSTeam t;
        
        if (team[0].isOnTeam(name)) {
            t = team[0];
        } else if (team[1].isOnTeam(name)) {
            t = team[1];
        } else {
            t = null;
        }
        
        return t;
    }
    
    /**
     * Returns the opposite team of the player
     * 
     * @param name name of the player
     * @param override override number 0 for Freq 0, 1 for Freq 1, -1 for normal
     * @return BWJSTeam, null if player doesn't belong to any team
     */
    private BWJSTeam getOtherTeam(String name, int override) {
        if (override == -1) {
            if (team[0].isOnTeam(name)) {
                return team[1];
            } else if (team[1].isOnTeam(name)) {
                return team[0];
            }
        } else if (override == 0) {
            return team[1];
        } else if (override == 1) {
            return team[0];
        }
        
        return null;
    }
    
    /**
     * Returns the opposite team according to BWJSTeam
     * 
     * @param t current team
     * @return other team
     */
    private BWJSTeam getOtherTeam(BWJSTeam t) {
        if (t.getFrequency() == 0) {
            return team[1];
        } else if (t.getFrequency() == 1) {
            return team[0];
        } else {
            return null;
        }
    }
    
    /**
     * Returns BWJSTeam of the player with "name" or according to the override
     * 
     * @param name name of the player
     * @param override override number 0 for Freq 0, 1 for Freq 1, -1 for normal
     * @return BWJSTeam, null if player doesn't belong to any team
     */
    private BWJSTeam getTeam(String name, int override) {
        if (override == -1) {
            return getTeam(name);
        } else if (override == 0) {
            return team[0];
        } else if (override == 1) {
            return team[1];
        } else
            return null;
    }
    
    /**
     * Checks if the arena should be locked or not
     * 
     * @param message Arena message
     */
    private void checkArenaLock(String message) {
        if (message.equals("Arena UNLOCKED") && lockArena)
            m_botAction.toggleLocked();
        if (message.equals("Arena LOCKED") && !lockArena)
            m_botAction.toggleLocked();
    }
    
    /**
     * Checks if name is a captain on one of the teams
     * Returns true if true, else false
     * 
     * @param name Name of the player that could be captain
     * @return true if name is captain, else false
     */
    private boolean isCaptain(String name) {
        boolean isCaptain;
        BWJSTeam t;
        
        isCaptain = false;
        t = getTeam(name);
        
        if (t != null) {
            if (t.getCaptainName().equalsIgnoreCase(name)) {
                isCaptain = true;
            }
        }
        
        return isCaptain;
    }
    
    /**
     * Checks if lineups are ok
     */
    private void checkLineup() {
        state.setState(BWJSState.PRE_GAME);
        
        if (team[0].players.size() >= cfg.getMinPlayers() && team[1].players.size() >= cfg.getMinPlayers()) {
            m_botAction.sendArenaMessage("Lineups are ok! Game will start in 30 seconds!", Tools.Sound.CROWD_OOO);
            startPreGame();
        } else {
            m_botAction.sendArenaMessage("Lineups are NOT ok! :( Game has been reset.", Tools.Sound.CROWD_GEE);
            startWaitingForCaps();
        }
    }
    
    /**
     * Resets variables to their default value
     */
    private void reset() {
        team[0].resetVariables();
        team[1].resetVariables();
        timeLeft = cfg.getTime() * 60;
        
        //Scoreboard
        scoreboard.hideAllObjects();
        m_botAction.setObjects();
        
        setSpecAndFreq();
    }
    
    /**
     * Locks arena
     */
    private void lockArena() {
        lockArena = true;
        m_botAction.toggleLocked();
    }
    
    /**
     * Unlocks arena
     */
    private void unlockArena() {
        lockArena = false;
        m_botAction.toggleLocked();
    }
    
    /**
     * Locks all doors in the arena
     */
    private void lockDoors() {
        m_botAction.setDoors(255);
    }
    
    /**
     * Searches in both teams for player
     * 
     * @param name name of the player
     * @return BWJSPlayer
     */
    private BWJSPlayer searchBWJSPlayer(String name) {
        BWJSPlayer p;
        
        p = team[0].searchPlayer(name);
        
        if (p == null) {
            p = team[1].searchPlayer(name);
        }
        
        return p;
    }
    
    /**
     * Sets everyone in spec and on right frequency
     */
    private void setSpecAndFreq() {
        for (Iterator<Player> it = m_botAction.getPlayerIterator(); it.hasNext();) {
            Player i = it.next();
            int id = i.getPlayerID();
            int freq = i.getFrequency();
            if (i.getShipType() != Tools.Ship.SPECTATOR) {
                m_botAction.specWithoutLock(id);
            }
            if (listNotplaying.contains(i.getPlayerName().toLowerCase()) && freq != FREQ_NOTPLAYING) {
                m_botAction.setFreq(id, FREQ_NOTPLAYING);
            } else if (freq != FREQ_SPEC && !listNotplaying.contains(i.getPlayerName().toLowerCase())) {
                m_botAction.setShip(id, 1);
                m_botAction.specWithoutLock(id);
            }
        }
    }
    
    /**
     * Updates the scoreboard
     */
    private void updateScoreboard() {
        scoreboard.hideAllObjects();
        /*
         * Base
         */
        if (cfg.getTimeTarget() != 0) {
            int team1Minutes = (int)Math.floor( team[0].flagTime / 60.0 );
            int team2Minutes = (int)Math.floor( team[1].flagTime / 60.0 );
            int team1Seconds = team[0].flagTime - team1Minutes * 60;
            int team2Seconds = team[1].flagTime - team2Minutes * 60;
            
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
            if(team[0].flag) {
                scoreboard.showObject(740);
                scoreboard.showObject(743);
                scoreboard.hideObject(741);
                scoreboard.hideObject(742);
            } else if(team[1].flag) {
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
            String scoreTeam1 = "" + team[1].getDeaths();
            String scoreTeam2 = "" + team[0].getDeaths();
            
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
        if (timeLeft >= 0) {
            int seconds = timeLeft % 60;
            int minutes = (timeLeft - seconds) / 60;
            scoreboard.showObject(730 + ((minutes - minutes % 10) / 10));
            scoreboard.showObject(720 + (minutes % 10));
            scoreboard.showObject(710 + ((seconds - seconds % 10) / 10));
            scoreboard.showObject(700 + (seconds % 10));
        }
        
        /*
         * Show Team Names
         */
        String n1 = team[0].getName().toLowerCase();
        String n2 = team[1].getName().toLowerCase();
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
     * Displays the scores
     */
    private void displayScores() {
        ArrayList<String> spam = new ArrayList<String>();
        if (cfg.getGameType() == BWJSConfig.BASE) {
            spam.add(",---------------------------------+------+------+-----------+------+------+-----------+----.");
            spam.add("|                               K |    D |   TK |    Points |   FT |  TeK |    Rating | LO |");
        } else  if (cfg.getGameType() == BWJSConfig.JAVDUEL){
            spam.add(",---------------------------------+------+------+-----------+----.");
            spam.add("|                               K |    D |   TK |    Rating | LO |");
        } else {
            spam.add(",---------------------------------+------+-----------+----.");
            spam.add("|                               K |    D |    Rating | LO |");
        }
        
        spam.addAll(team[0].getScores());
        
        if (cfg.getGameType() == BWJSConfig.BASE) {
            spam.add("+---------------------------------+------+------+-----------+------+------+-----------+----+");
        } else if (cfg.getGameType() == BWJSConfig.JAVDUEL) {
            spam.add("+---------------------------------+------+------+-----------+----+");
        } else {
            spam.add("+---------------------------------+------+-----------+----+");
        }
        
        spam.addAll(team[1].getScores());
        
        if (cfg.getGameType() == BWJSConfig.BASE) {
            spam.add("`---------------------------------+------+------+-----------+------+------+-----------+----'");
        } else if (cfg.getGameType() == BWJSConfig.JAVDUEL) {
            spam.add("`---------------------------------+------+------+-----------+----'");
        } else {
            spam.add("`---------------------------------+------+-----------+----'");
        }
        
        m_botAction.arenaMessageSpam(spam.toArray(new String[spam.size()]));
    }
    
    /* Game classes */
    private class BWJSCaptain {
        private String captainName;
        private long startTime;
        private long endTime;
        
        private BWJSCaptain(String name) {
            captainName = name;
            startTime = (cfg.getTime() * 60) - timeLeft;
            endTime = -1;
        }
        
        private void end() {
            endTime = (cfg.getTime() * 60) - timeLeft;
        }
        
        private boolean hasEnded() {
            if (endTime != -1) {
                return true;
            } else {
                return false;
            }
        }
    }
    
    /**
     * BWJSConfig - Configuration file for BWJS
     * 
     * @author fantus
     *
     */
    private class BWJSConfig {
        private BotSettings botSettings;
        private String chats;
        private String arena;
        private int gameType;
        private int maxDeaths;
        private int maxLagouts;
        private int maxSubs;
        private int maxPlayers;
        private int minPlayers;
        private int outOfBorderTime;
        private int time;
        private int timetarget;
        private int yborder;
        private int[] warpSpots;
        private int[] objects;
        private int defaultShipType;
        private int[] maxShips;
        private boolean announceShipType;
        private boolean allowAutoCaps;
        private boolean allowZoner;
        private boolean inBase;
        
        /* Constants */
        //Game types
        private static final int BASE = 1;
        private static final int WBDUEL = 2;
        private static final int JAVDUEL = 3;
        private static final int SPIDDUEL = 4;
        
        /** Class constructor */
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
            String gameTypeString = botSettings.getString("GameType" + botNumber);
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
            
            //Allow automation of captains
            allowAutoCaps = (botSettings.getInt("AllowAuto" + botNumber) == 1);
            
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
            timetarget = botSettings.getInt("TimeTarget" + gameType);
            
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
        }
        
        /**
         * Returns whether to announce the shiptype
         * 
         * @return true if shiptype should be announced, else false
         */
        private boolean announceShipType() {
            return announceShipType;
        }
        
        /**
         * Returns max amount of ships of shiptype
         * 
         * @param shipType type of ship
         * @return max amount of ships of shiptype
         */
        private int getMaxShips(int shipType) {
            return maxShips[shipType];
        }
        
        /**
         * Returns the default ship type
         * 
         * @return default shiptype
         */
        private int getDefaultShipType() {
            return defaultShipType;
        }
        
        /**
         * Returns string with chats
         * 
         * @return String with all the chats
         */
        private String getChats() {
            return chats;
        }
        
        /**
         * Returns gameType number
         * 
         * @return Game Type number
         */
        private int getGameType(){
            return gameType;
        }
        
        /**
         * Returns the amount of maximum lagouts
         * 
         * @return amount of maximum lagouts, -1 if unlimited
         */
        private int getMaxLagouts() {
            return maxLagouts;
        }
        
        /**
         * Returns the maximum amount of deaths allowed, 0 if unlimited
         * 
         * @return maximum amount of deaths allowed, 0 if unlimited
         */
        private int getMaxDeaths() {
            return maxDeaths;
        }
        
        /**
         * Returns the arena name
         * 
         * @return arena name
         */
        private String getArena() {
            return arena;
        }
        
        /**
         * Returns the game type in the String format
         * 
         * @return Game Type
         */
        private String getGameTypeString() {
            String gameTypeString;
            
            switch (gameType) {
                case WBDUEL : gameTypeString = "WBDUEL"; break;
                case BASE : gameTypeString = "BASE"; break;
                case JAVDUEL : gameTypeString = "JAVDUEL"; break;
                case SPIDDUEL : gameTypeString = "SPIDDUEL"; break;
                default : gameTypeString = "";
            }
            
            return gameTypeString;
        }
        
        /**
         * Returns maximum out of border time
         * 
         * @return maximum out of border time
         */
        private int getOutOfBorderTime() {
            return outOfBorderTime;
        }
        
        /**
         * Returns game time
         * 
         * @return Maximum time of one game
         */
        private int getTime() {
            return time;
        }
    
        /**
         * Returns the time target
         * 
         * @return time target
         */
        private int getTimeTarget() {
            return timetarget;
        }
    
        /**
         * Returns the maximum amount of players allowed
         * 
         * @return maximum amount of players allowed
         */
        private int getMaxPlayers() {
            return maxPlayers;
        }
        
        /**
         * Returns the warpspot
         * 
         * @param index index number
         * @return warpspot location
         */
        private int getWarpSpot(int index) {
            return warpSpots[index];
        }
        
        /**
         * Returns true if auto caps is on, else false
         * 
         * @return Returns true if auto caps is on, else false
         */
        private boolean getAllowAutoCaps() {
            return allowAutoCaps;
        }
    
        /**
         * Returns if a zoner can be send
         * 
         * @return true if a zoner can be send, else false
         */
        private boolean getAllowZoner() {
            return allowZoner;
        }
    
        /**
         * Returns minimal amount of players needed in
         * 
         * @return minimal amount of players
         */
        private int getMinPlayers() {
            return minPlayers;
        }
    
        /**
         * Returns whether players should stay in a base
         * 
         * @return true if players should stau in the base, else false
         */
        private boolean getInBase() {
            return inBase;
        }
    
        /**
         * Returns y border
         * 
         * @return y border
         */
        private int getYBorder() {
            return yborder;
        }
    
        /**
         * Returns object number
         * 
         * @param index index number
         * @return object number
         */
        private int getObject(int index) {
            return objects[index];
        }
    
        /**
         * Returns maximum allowed substitutes
         * 
         * @return maximum allowed substitutes
         */
        private int getMaxSubs() {
            return maxSubs;
        }
    }
    
    /**
     * BWJSPlayer - Player
     * 
     * @author fantus
     *
     */
    private class BWJSPlayer {
        private String p_name;
        private int[][] p_ship;
        private int p_currentShip;
        private int p_state;
        private long p_timestampLagout;
        private long p_timestampChange;
        private long p_timestampSub;
        private long p_timestampSwitch;
        private int p_lagouts;
        private int p_frequency;
        private int p_maxDeaths;
        private int p_outOfBorderTime;
        private boolean p_outOfBorderWarning;
        private long p_lastPositionUpdate;
        private int p_userID;
        
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
        private final static int BULLET_FIRED = 19;
        private final static int REPELS_USED = 20;
        private final static int BOMB_FIRED = 21;
        private final static int MINE_LAYED = 22;
        private final static int BURST_FIRED = 23;
        private final static int USED = 24;
        private final static int PLAY_TIME = 25;
        
        //Ship states
        private static final int IN = 0;
        private static final int LAGOUT = 1;
        private static final int OUT_SUBABLE = 2;
        private static final int SUBBED = 3;
        private static final int OUT = 4;
        
        //Static variables
        private static final int CHANGE_WAIT_TIME = 15; //In seconds
        private static final int SWITCH_WAIT_TIME = 15; //In seconds
        private static final int SUB_WAIT_TIME = 15 ; //In seconds
        private static final int LAGOUT_TIME = 15 * Tools.TimeInMillis.SECOND;  //In seconds
        
        /** Class constructor */
        private BWJSPlayer(String player, int shipType, int maxDeaths, int frequency) {
            p_ship = new int[9][26];
            p_name = player;
            p_currentShip = shipType;
            p_maxDeaths = maxDeaths;
            p_frequency = frequency;
            p_lagouts = 0;
            p_outOfBorderTime = cfg.getOutOfBorderTime();
            p_outOfBorderWarning = false;
            p_userID = sql.getUserID(p_name);
            
            m_botAction.scoreReset(p_name);
            addPlayer();
            
            p_ship[p_currentShip][USED] = 1;
            
            p_timestampLagout = 0;
            p_timestampChange = 0;
            p_timestampSub = 0;
            p_timestampSwitch = 0;
        }
        
        /**
         * Adds a player into the game
         * - Resets out of border time
         */
        private void addPlayer() {
            p_state = IN;
            
            if (m_botAction.getPlayer(p_name) == null) {
                return;
            }
            
            m_botAction.setShip(p_name, p_currentShip);
            m_botAction.setFreq(p_name, p_frequency);
            p_outOfBorderTime = cfg.getOutOfBorderTime();
            p_outOfBorderWarning = false;
        }
        
        /**
         * Puts player IN in shiptype
         * 
         * @param shipType ship type
         */
        private void putIN(int shipType) {
            p_currentShip = shipType;
            addPlayer();
        }
        
        /**
         * Handles a flag claim
         * - add one to the flag claimed counter
         */
        private void flagClaimed() {
            p_ship[p_currentShip][FLAGS_CLAIMED]++;
        }
        
        /**
         * Handles flag reward
         * 
         * @param points points of the flag reward
         */
        private void flagReward(Short points) {
            p_ship[p_currentShip][SCORE] += points;
        }
        
        /**
         * Returns the current ship state
         * 
         * @return int current ship state
         */
        private int getCurrentState() {
            return p_state;
        }
        
        /**
         * Handles a lagout event
         * - Notes down the timestamp of the lagout
         * - Adds one to the lagout counter
         * - Adds a death if gametype is WBDUEL,JAVDUEL or SPIDDUEL
         * - Check if the player hits loss/deaths limit
         * - Check if the player is out due maximum of lagouts
         * - Tell the player how to get back in
         */
        private void lagout() {
            p_state = LAGOUT;
            p_timestampLagout = System.currentTimeMillis();
            
            if (state.getCurrentState() == BWJSState.GAME_IN_PROGRESS) {
                p_lagouts++;
                
                //Add a death on a lagout during gameplay in WBDUEL, JAVDUEL and SPIDDUEL
                if (cfg.getGameType() == BWJSConfig.WBDUEL ||
                        cfg.getGameType() == BWJSConfig.JAVDUEL ||
                        cfg.getGameType() == BWJSConfig.SPIDDUEL) {
                    p_ship[p_currentShip][DEATHS]++;    //Add a loss due the lagout
                    
                    //Notify the team of the lagout
                    m_botAction.sendOpposingTeamMessageByFrequency(p_frequency, p_name + " lagged out or specced. " +
                            "(+1 death)");
                    
                    checkIfOut(); //Check if the player reached the loss limit
                } else {
                    //Notify the team of the lagout
                    m_botAction.sendOpposingTeamMessageByFrequency(p_frequency, p_name + " lagged out or specced.");
                }
                
                //Check if player is out due maximum of lagouts
                if ((cfg.getMaxLagouts() != -1) && p_lagouts >= cfg.getMaxLagouts()) {
                    //Extra check if player is not already set to OUT, due death limit (the +1 death thing)
                    if (p_state < OUT) {
                        out("lagout limit");
                    }
                }
                
                //Message player how to get back in if he is not out
                if (p_state != OUT) {
                    m_botAction.sendPrivateMessage(p_name, "PM me \"!lagout\" to get back in.");
                }
            }
        }
        
        /**
         * Checks if the player reached the death limit, at least if there is a death limit
         */
        private void checkIfOut() {
            if (p_maxDeaths <= getDeaths() && cfg.getMaxDeaths() != 0) {
                out("death limit");
            }
        }
        
        /**
         * Returns the amount of deaths of all ships combined
         * 
         * @return amount of deaths of all ships combined
         */
        private int getDeaths() {
            int deaths;
            
            deaths = 0;
            
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++) {
                deaths += p_ship[i][DEATHS];
            }
            
            return deaths;
        }
        
        /**
         * Returns the total amount of kills of all ships combined
         * 
         * @return amount of kills of all ships combined
         */
        private int getKills() {
            int kills;
            
            kills = 0;
            
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++) {
                for (int j = WARBIRD_KILL; j <= SHARK_KILL; j++) {
                    kills += p_ship[i][j];
                }
            }
            
            return kills;
        }
        
        /**
         * Returns sum of teamkills
         * 
         * @return sum of teamkills
         */
        private int getTeamKills() {
            int teamkills;
            
            teamkills = 0;
            
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++) {
                for (int j = WARBIRD_TEAMKILL; j <= SHARK_TEAMKILL; j++) {
                    teamkills += p_ship[i][j];
                }
            }
            
            return teamkills;
        }
        
        /**
         * This method handles a player going out
         * - Changes player state
         * - Spectates the player
         * - Notifies the arena
         * - Change state according to reason
         * 
         * @param reason Reason why the player went out
         */
        private void out(String reason) {
            String arenaMessage = "";
            
            p_state = OUT;
            
            //Spectate the player if he is in the arena
            if (m_botAction.getPlayer(p_name) != null) {
                m_botAction.specWithoutLock(p_name);
                m_botAction.setFreq(p_name, p_frequency);
            }
            
            //Notify arena and change state if player is still subable
            if (reason.equals("death limit")) {
                arenaMessage = p_name + " is out. " + getKills() + " wins " + getDeaths() + " losses";
            } else if (reason.equals("lagout limit")) {
                arenaMessage = p_name+ " is out, (too many lagouts). " + getKills() + " wins " + getDeaths() +
                    " losses. (NOTICE: player can still be subbed)";
                p_state = OUT_SUBABLE;
            } else if (reason.equals("out of border")) {
                arenaMessage = p_name + " is out, (too long outside of base). " + getKills() + " wins " +
                    getDeaths() + " losses";
            } else if (reason.equals("out not playing")) {
                arenaMessage = p_name + " is out, (set himself to notplaying). NOTICE: Player is still subable.";
                p_state = OUT_SUBABLE;
            }
            
            m_botAction.sendArenaMessage(arenaMessage);
        }
        
        /**
         * Handles a death of the player
         * - Adds death
         * - Checks if out
         * - Reset out of border time
         */
        private void died() {
            p_ship[p_currentShip][DEATHS]++;
            checkIfOut();
            resetOutOfBorderTime();
        }
        
        /**
         * Handles a kill of the player
         * - Check if its a teamkill or not
         * - Keep count of what ship got killed
         * 
         * @param killeeShip Ship of the player that got killed
         * @param killeeFreq Frequency of the player that got killed
         * @param killeeBounty Bounty of the player that got killed
         */
        private void killed(int killeeShip, int killeeFreq, int killeeBounty) {
            int ship;
            
            ship = WARBIRD_KILL; //A default value, real value is selected below
            
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
            p_ship[p_currentShip][ship]++;
            p_ship[p_currentShip][SCORE] += killeeBounty;
        }
        
        /**
         * Resets out of border time and the warning lock
         */
        private void resetOutOfBorderTime() {
            p_outOfBorderTime = cfg.getOutOfBorderTime();
            p_outOfBorderWarning = false;
        }
        
        /**
         * Timestamps last position received
         */
        private void timestampLastPosition() {
            p_lastPositionUpdate = System.currentTimeMillis();
        }
        
        /**
         * Keeps track of the weapons that were fired
         * 
         * @param weaponType weapon type
         */
        private void weaponFired(int weaponType) {
            if (weaponType == WeaponFired.WEAPON_BULLET) {
                p_ship[p_currentShip][BULLET_FIRED]++;
            } else if (weaponType == WeaponFired.WEAPON_BURST) {
                p_ship[p_currentShip][BURST_FIRED]++;
            } else if (weaponType == WeaponFired.WEAPON_REPEL) {
                p_ship[p_currentShip][REPELS_USED]++;
            } else if (weaponType == WeaponFired.WEAPON_BOMB) {
                p_ship[p_currentShip][BOMB_FIRED]++;
            } else if (weaponType == WeaponFired.WEAPON_MINE) {
                p_ship[p_currentShip][MINE_LAYED]++;
            }
        }
    
        /**
         * Returns the name of this player
         * 
         * @return Returns the name of this player
         */
        private String getName() {
            return p_name;
        }
    
        /**
         * Returns whether a player is out or not
         * 
         * @return Returns true if the player is out, else false
         */
        private boolean isOut() {
            if (p_state >= OUT_SUBABLE) {
                return true;
            } else {
                return false;
            }
        }
    
        /**
         * Returns current type of ship
         * 
         * @return Returns current type of ship
         */
        private int getCurrentShipType() {
            return p_currentShip;
        }
    
        /**
         * Returns whether a !change is allowed on this player
         * 
         * @return true if change is allowed, else false
         */
        private boolean isChangeAllowed() {
            if ((System.currentTimeMillis() - p_timestampChange) <= (CHANGE_WAIT_TIME * Tools.TimeInMillis.SECOND)) {
                return false;
            } else {
                return true;
            }
        }
    
        /**
         * Returns time in seconds until next change
         * 
         * @return Returns time in seconds until next change
         */
        private long getTimeUntilNextChange() {
            return (CHANGE_WAIT_TIME - ((System.currentTimeMillis() - p_timestampChange) / Tools.TimeInMillis.SECOND));
        }
    
        /**
         * Returns whether a !sub is allowed on this player
         * 
         * @return true if sub is allowed, else false
         */
        private boolean isSubAllowed() {
            if ((System.currentTimeMillis() - p_timestampSub) <= (SUB_WAIT_TIME * Tools.TimeInMillis.SECOND)) {
                return false;
            } else {
                return true;
            }
        }
        
        /**
         * Returns time in seconds until next sub
         * 
         * @return Returns time in seconds until next sub
         */
        private long getTimeUntilNextSub() {
            return (SUB_WAIT_TIME - ((System.currentTimeMillis() - p_timestampSub) / Tools.TimeInMillis.SECOND));
        }
        
        /**
         * Returns whether a !switch is allowed on this player
         * 
         * @return true if switch is allowed, else false
         */
        private boolean isSwitchAllowed() {
            if ((System.currentTimeMillis() - p_timestampSwitch) <= (SWITCH_WAIT_TIME * Tools.TimeInMillis.SECOND)) {
                return false;
            } else {
                return true;
            }
        }
        
        /**
         * Returns time in seconds until next switch
         * 
         * @return Returns time in seconds until next switch
         */
        private long getTimeUntilNextSwitch() {
            return (SWITCH_WAIT_TIME - ((System.currentTimeMillis() - p_timestampSwitch) / Tools.TimeInMillis.SECOND));
        }
        
        /**
         * Changes player to shipType
         * 
         * @param shipType Shiptype to change to
         */
        private void change(int shipType) {
            m_botAction.sendArenaMessage(p_name + " changed from " + Tools.shipName(p_currentShip) + 
                    " to " + Tools.shipName(shipType));
            p_currentShip = shipType;
            
            p_ship[p_currentShip][USED] = 1;
            
            if (m_botAction.getPlayer(p_name) != null) {
                m_botAction.setShip(p_name, shipType);
            }
            
            p_timestampChange = System.currentTimeMillis();
        }
    
        /**
         * Returns lagout time
         * 
         * @return lagout time
         */
        private long getLagoutTimestamp() {
            return p_timestampLagout;
        }
    
        /**
         * Returns a player into the game
         */
        private void lagin() {
            m_botAction.sendOpposingTeamMessageByFrequency(p_frequency, p_name + " returned from lagout.");
            addPlayer();
        }
    
        /**
         * Returns status
         * 
         * @return status
         */
        private String getStatus() {
            switch (p_state) {
                case (IN) : return "IN";
                case (LAGOUT) : return "LAGGED OUT";
                case (SUBBED) : return "SUBSTITUTED";
                case (OUT) : return "OUT";
                case (OUT_SUBABLE) : return "OUT (still substitutable)";
                default : return "";
        }
    }
    
        /**
         * Returns total rating
         * 
         * @return sum of all ratings
         */
        private int getTotalRating() {
            int totalRating;
            
            totalRating = 0;
            
            for (int i = 0; i <= Tools.Ship.SHARK; i++) {
                totalRating += getRating(i);
            }
            
            return totalRating;
        }
        
        /**
         * Returns rating of shiptype
         * 
         * @param shipType shiptype
         * @return rating
         */
        private int getRating(int shipType) {
            int rating = 0;
            
            switch(shipType) {
                case Tools.Ship.WARBIRD : 
                    rating = (int) (
                            .45 * p_ship[Tools.Ship.WARBIRD][SCORE] * 
                            
                            (.07 * p_ship[Tools.Ship.WARBIRD][WARBIRD_KILL] + 
                            .07 * p_ship[Tools.Ship.WARBIRD][JAVELIN_KILL] + 
                            .05 * p_ship[Tools.Ship.WARBIRD][SPIDER_KILL] +
                            .12 * p_ship[Tools.Ship.WARBIRD][TERRIER_KILL] +
                            .05 * p_ship[Tools.Ship.WARBIRD][WEASEL_KILL] +
                            .06 * p_ship[Tools.Ship.WARBIRD][LANCASTER_KILL] +
                            .08 * p_ship[Tools.Ship.WARBIRD][SHARK_KILL] -
                            .04 * getDeaths())      
                            );
                    break;
                case Tools.Ship.JAVELIN : 
                    rating = (int) (
                            .6 * p_ship[Tools.Ship.JAVELIN][SCORE] * 
                            
                            (.05 * p_ship[Tools.Ship.JAVELIN][WARBIRD_KILL] + 
                            .06 * p_ship[Tools.Ship.JAVELIN][JAVELIN_KILL] + 
                            .066 * p_ship[Tools.Ship.JAVELIN][SPIDER_KILL] +
                            .14 * p_ship[Tools.Ship.JAVELIN][TERRIER_KILL] +
                            .07 * p_ship[Tools.Ship.JAVELIN][WEASEL_KILL] +
                            .05 * p_ship[Tools.Ship.JAVELIN][LANCASTER_KILL] +
                            .09 * p_ship[Tools.Ship.JAVELIN][SHARK_KILL] -
                            .05 * getDeaths() - (
                                    .07 * p_ship[Tools.Ship.JAVELIN][WARBIRD_TEAMKILL] + 
                                    .07 * p_ship[Tools.Ship.JAVELIN][JAVELIN_TEAMKILL] + 
                                    .06 * p_ship[Tools.Ship.JAVELIN][SPIDER_TEAMKILL] +
                                    .13 * p_ship[Tools.Ship.JAVELIN][TERRIER_TEAMKILL] +
                                    .06 * p_ship[Tools.Ship.JAVELIN][WEASEL_TEAMKILL] +
                                    .07 * p_ship[Tools.Ship.JAVELIN][LANCASTER_TEAMKILL] +
                                    .09 * p_ship[Tools.Ship.JAVELIN][SHARK_TEAMKILL]
                                                )
                            )
                            );
                    break;
                case Tools.Ship.SPIDER : 
                    rating = (int) (
                            .4 * p_ship[Tools.Ship.SPIDER][SCORE] * 
                            
                            (.06 * p_ship[Tools.Ship.SPIDER][WARBIRD_KILL] + 
                            .06 * p_ship[Tools.Ship.SPIDER][JAVELIN_KILL] + 
                            .04 * p_ship[Tools.Ship.SPIDER][SPIDER_KILL] +
                            .09 * p_ship[Tools.Ship.SPIDER][TERRIER_KILL] +
                            .05 * p_ship[Tools.Ship.SPIDER][WEASEL_KILL] +
                            .05 * p_ship[Tools.Ship.SPIDER][LANCASTER_KILL] +
                            .089 * p_ship[Tools.Ship.SPIDER][SHARK_KILL] -
                            .05 * getDeaths()
                            )
                            );
                    break;
                case Tools.Ship.TERRIER : 
                    rating = (int) (
                            2.45 * p_ship[Tools.Ship.TERRIER][SCORE] * 
                            
                            (.03 * p_ship[Tools.Ship.TERRIER][WARBIRD_KILL] + 
                            .03 * p_ship[Tools.Ship.TERRIER][JAVELIN_KILL] + 
                            .036 * p_ship[Tools.Ship.TERRIER][SPIDER_KILL] +
                            .12 * p_ship[Tools.Ship.TERRIER][TERRIER_KILL] +
                            .35 * p_ship[Tools.Ship.TERRIER][WEASEL_KILL] +
                            .025 * p_ship[Tools.Ship.TERRIER][LANCASTER_KILL] +
                            .052 * p_ship[Tools.Ship.TERRIER][SHARK_KILL] -
                            .21 * getDeaths()
                            )
                            );
                    break;
                case Tools.Ship.WEASEL : 
                    rating = (int) (
                            2.45 * p_ship[Tools.Ship.WEASEL][SCORE] * (.09 * getKills() - .21 * getDeaths())
                            );
                    break;
                case Tools.Ship.LANCASTER : 
                    rating = (int) (
                            .6 * p_ship[Tools.Ship.LANCASTER][SCORE] * 
                            
                            (.07 * p_ship[Tools.Ship.LANCASTER][WARBIRD_KILL] + 
                            .07 * p_ship[Tools.Ship.LANCASTER][JAVELIN_KILL] + 
                            .055 * p_ship[Tools.Ship.LANCASTER][SPIDER_KILL] +
                            .12 * p_ship[Tools.Ship.LANCASTER][TERRIER_KILL] +
                            .05 * p_ship[Tools.Ship.LANCASTER][WEASEL_KILL] +
                            .06 * p_ship[Tools.Ship.LANCASTER][LANCASTER_KILL] +
                            .08 * p_ship[Tools.Ship.LANCASTER][SHARK_KILL] -
                            .04 * getDeaths()
                            )
                            );
                    break;
                case Tools.Ship.SHARK : 
                    int tmpShark;
                    if (getDeaths() != 0)
                        tmpShark = p_ship[Tools.Ship.SHARK][REPELS_USED] / getDeaths();
                    else
                        tmpShark = 0;
                        
                    rating = (int) (
                            p_ship[Tools.Ship.SHARK][SCORE] * 
                            
                            (.065 * (tmpShark) +
                            .001 * p_ship[Tools.Ship.SHARK][WARBIRD_KILL] + 
                            .001 * p_ship[Tools.Ship.SHARK][JAVELIN_KILL] + 
                            .001 * p_ship[Tools.Ship.SHARK][SPIDER_KILL] +
                            .005 * p_ship[Tools.Ship.SHARK][TERRIER_KILL] +
                            .001 * p_ship[Tools.Ship.SHARK][WEASEL_KILL] +
                            .001 * p_ship[Tools.Ship.SHARK][LANCASTER_KILL] +
                            .0015 * p_ship[Tools.Ship.SHARK][SHARK_KILL] -
                            .001 * getDeaths() - (
                                    .07 * p_ship[Tools.Ship.SHARK][WARBIRD_TEAMKILL] + 
                                    .07 * p_ship[Tools.Ship.SHARK][JAVELIN_TEAMKILL] + 
                                    .072 * p_ship[Tools.Ship.SHARK][SPIDER_TEAMKILL] +
                                    .15 * p_ship[Tools.Ship.SHARK][TERRIER_TEAMKILL] +
                                    .05 * p_ship[Tools.Ship.SHARK][WEASEL_TEAMKILL] +
                                    .07 * p_ship[Tools.Ship.SHARK][LANCASTER_TEAMKILL] +
                                    .08 * p_ship[Tools.Ship.SHARK][SHARK_TEAMKILL]
                                                )
                            )
                            );
                    break;
                default :
                    rating = 0;
            }
            
            return rating;
        }
    
        /**
         * Returns total score
         * 
         * @return total score
         */
        private int getTotalScore() {
            int score = 0;
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++)
                score += p_ship[i][SCORE];
            return score;
        }
    
        /**
         * Subs the player OUT
         */
        private void sub() {
            p_state = SUBBED;
            if (m_botAction.getPlayer(p_name) != null) {
                m_botAction.specWithoutLock(p_name);
                if (!listNotplaying.contains(p_name.toLowerCase())) {
                    m_botAction.setFreq(p_name, p_frequency);
                }
            }
            
            p_timestampSub = System.currentTimeMillis();
        }
    
        /**
         * Checks if player is in the base
         */
        private void checkIfInBase() {
            Player p;
            
            p = m_botAction.getPlayer(p_name);
            
            if (p == null) {
                return;
            }
            
            if (p_state == IN) {
                if ((System.currentTimeMillis() - p_lastPositionUpdate) <= Tools.TimeInMillis.SECOND) {
                    if (p.getYTileLocation() > cfg.getYBorder()) {
                        p_outOfBorderTime--;
                    }
                    
                    if (p_outOfBorderTime == (cfg.getOutOfBorderTime() / 2) && !p_outOfBorderWarning) {
                        m_botAction.sendPrivateMessage(p_name, "Go to base! You have " + p_outOfBorderTime +
                              " seconds before you'll get removed from the game!", Tools.Sound.BEEP3);
                        p_outOfBorderWarning = true;
                    } else if (p_outOfBorderTime <= 0) {
                        if (cfg.getMaxDeaths() != 0) {
                            p_ship[p_currentShip][DEATHS] = cfg.getMaxDeaths();
                        }
                        out("out of border");
                    }
                }
            }
        }
    
        /**
         * Adds a second to playtime
         */
        private void addPlayTime() {
            if (p_state == IN) {
                p_ship[p_currentShip][PLAY_TIME]++;
            }
        }
    
        /**
         * Returns sum of flags claimed
         * 
         * @return sum of flags claimed
         */
        private int getFlagsClaimed() {
            int flags = 0;
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++) {
                flags += p_ship[i][FLAGS_CLAIMED];
            }
            return flags;
        }
    
        /**
         * Returns the sum of terriers killed
         * 
         * @return sum of terriers killed
         */
        private int getTerrKills() {
            int kills = 0;
            for (int i = Tools.Ship.WARBIRD; i <= Tools.Ship.SHARK; i++) {
                kills += p_ship[i][TERRIER_KILL];
            }
            return kills;
        }
    
        /**
         * Returns lagouts
         * 
         * @return lagouts
         */
        private int getLagouts() {
            return p_lagouts;
        }
    
        /**
         * Returns kills made in a specific ship
         * 
         * @param shipType shipType
         * @return kills made in a specific ship
         */
        private int getKills(int shipType) {
            int kills = 0;
            for (int j = WARBIRD_KILL; j <= SHARK_KILL; j++)
                kills += p_ship[shipType][j];
            return kills;
        }
    
        /**
         * Returns teamkills made in a specific ship
         * @param shipType ship
         * @return teamkills made in a specific ship
         */
        private int getTeamKills(int shipType) {
            int kills = 0;
            
            for (int j = WARBIRD_TEAMKILL; j <= SHARK_TEAMKILL; j++)
                kills += p_ship[shipType][j];
            
            return kills;
        }
    
        /**
         * Switches player
         * - puts the player in
         * - timestamps the event
         */
        private void switchPlayer() {
            addPlayer();
            p_timestampSwitch = System.currentTimeMillis();
        }
    }
    
    /**
     * BWJSState - Game state
     * 
     * @author fantus
     *
     */
    private class BWJSState {
        private int current;    //Current state
        private long stateTimestamp;
        
        //States
        private static final int OFF = -1;
        private static final int WAITING_FOR_CAPS = 0;
        private static final int ADDING_PLAYERS = 1;
        private static final int PRE_GAME = 2;
        private static final int GAME_IN_PROGRESS = 3;
        private static final int GAME_OVER = 4;
        
        /** Class constructor */
        private BWJSState() {
            current = OFF;
        }
        
        /**
         * Returns the current state
         * 
         * @return int current state
         */
        private int getCurrentState() {
            return current;
        }
    
        /**
         * Sets state
         * 
         * @param stateNumber state
         */
        private void setState(int stateNumber) {
            current = stateNumber;
            stateTimestamp = System.currentTimeMillis();
        }
    
        /**
         * Returns the timestamp of the current state
         * 
         * @return timestamp of the current state
         */
        private long getTimeStamp() {
            return stateTimestamp;
        }
    }
    
    /**
     * BWJSTeam - Team
     * 
     * @author fantus
     *
     */
    private class BWJSTeam {
        private boolean flag;
        private boolean turnToPick;
        private boolean ready;
        private int flagTime;
        private int frequency;
        private TreeMap<String, BWJSPlayer> players;
        private TreeMap<Short, BWJSCaptain> captains;
        private short captainsIndex;
        private String captainName;
        private String teamName;
        private long captainTimestamp;
        private int substitutesLeft;
        
        /** Class constructor */
        private BWJSTeam(int frequency) {
            this.frequency = frequency;
            
            //teamname
            if (frequency == 0) {
                teamName = "Freq 0";
            } else {
                teamName = "Freq 1";
            }
            
            players = new TreeMap<String, BWJSPlayer>();
            captains = new TreeMap<Short, BWJSCaptain>(); 
            
            resetVariables();
        }
        
        /**
         * Resets all variables except frequency
         */
        private void resetVariables() {
            players.clear();
            flag = false;
            turnToPick = false;
            captainName = "[NONE]";
            captains.clear();
            flagTime = 0;
            ready = false;
            substitutesLeft = cfg.getMaxSubs();
            captainsIndex = -1;
        }
        
        /**
         * Handles flag claim
         * 
         * @param playerName name of the player that claimed a flag
         */
        private void flagClaimed(String playerName) {
            playerName = playerName.toLowerCase();
            
            if (players.containsKey(playerName)) {
                //Flag claimed by team
                flag = true;
                players.get(playerName).flagClaimed();
            } else {
                //Flag lost
                flag = false;
            }
        }
        
        /**
         * Handles flag reward
         * - Give all the players that are IN the points that were rewarded
         * 
         * @param points Rewarded flag points
         */
        private void flagReward(Short points) {
            for (BWJSPlayer i : players.values()) {
                if (i.getCurrentState() == BWJSPlayer.IN) {
                    i.flagReward(points);
                }
            }
        }
        
        /**
         * Returns the teamname
         * 
         * @return teamname
         */
        private String getName() {
            return teamName;
        }
        
        /**
         * Returns the current state of the player
         * 
         * @param name name of the player
         * @return current state of the player
         */
        private int getPlayerState(String name) {
            int playerState;    //Current state of the player
            
            name = name.toLowerCase();
            
            playerState = -1;   //-1 if not found
            
            if (players.containsKey(name)) {
                playerState = players.get(name).getCurrentState();
            }
            
            return playerState;
        }
        
        /**
         * Checks if the player is on this team
         * 
         * @param name name of the player
         * @return true if on team, false if not
         */
        private boolean isOnTeam(String name) {
            boolean isOnTeam;
            
            name = name.toLowerCase();
            
            if (players.containsKey(name)) {
                isOnTeam = true;
            } else if (name.equalsIgnoreCase(captainName)) {
                isOnTeam = true;
            } else {
                isOnTeam = false;
            }
            
            return isOnTeam;
        }
        
        /**
         * Checks if the player is a player on the team
         * 
         * @param name Name of the player
         * @return true if is a player, false if not
         */
        private boolean isPlayer(String name) {
            boolean isPlayer;
            
            name = name.toLowerCase();
            
            if (players.containsKey(name)) {
                isPlayer = true;
            } else {
                isPlayer = false;
            }
            
            return isPlayer;
        }
        
        /**
         * Checks if the player is playing or has played for the team
         * 
         * @param name Name of the player
         * @return return if player was IN, else false
         */
        private boolean isIN(String name) {
            boolean isIN;
            
            name = name.toLowerCase();
            isIN = false;
            
            if (players.containsKey(name)) {
                if (players.get(name).getCurrentState() != BWJSPlayer.SUBBED) {
                    isIN = true;
                }
            }
            
            return isIN;
        }
        
        /**
         * Checks if a player has lagged out or not
         * 
         * @param name name of the player that could have lagged out
         * @return true if player has lagged out, else false
         */
        private boolean laggedOut(String name) {
            BWJSPlayer p;
            Player player;
            name = name.toLowerCase();
            
            if (!players.containsKey(name)) {
                return false;
            }
            
            p = players.get(name);
            
            if (p.getCurrentState() == BWJSPlayer.IN) {
                player = m_botAction.getPlayer(name);
                
                if (player == null) {
                    return false;
                }
                
                if (player.getShipType() == Tools.Ship.SPECTATOR) {
                    return true;
                }
            }
            
            if (p.getCurrentState() == BWJSPlayer.LAGOUT) {
                return true;
            }
            
            return false;
        }
        
        /**
         * Handles a lagout event
         * - Sends a lagout event to the player
         * - Notify the captain
         * 
         * @param name Name of the player that lagged out
         */
        private void lagout(String name) {
            name = name.toLowerCase();
            
            if (players.containsKey(name)) {
                players.get(name).lagout();
            }
            
            //Notify captain if captian is in the arena
            if (m_botAction.getPlayer(captainName) != null) {
                m_botAction.sendPrivateMessage(captainName, name + " lagged out!");
            }
            
            //NOTE: the team is notified in the BWJSPlayer lagout() method
        }
        
        /**
         * This method handles a player death
         * 
         * @param killee player that got killed
         * @param killer the killer
         * @param killeeShip Ship of the player that got killed
         * @param killeeFreq Frequency of the player that got killed
         * @param killeeBounty Bounty of the player that got killed
         */
        private void playerDeath(String killee, String killer, int killeeShip, int killeeFreq, int killeeBounty) {
            killee = killee.toLowerCase(); //Name of the player that got killed
            killer = killer.toLowerCase(); //Name of the killer
            
            //Check if the player that got killed is on this team
            if (players.containsKey(killee)) {
                players.get(killee).died(); //Report death
            }
            
            //Check if the killer is on this team
            if (players.containsKey(killer)) {
                players.get(killer).killed(killeeShip, killeeFreq, killeeBounty); //Report kill
            }
        }
    
        /**
         * Returns the flagtime
         * 
         * @return flag time in seconds
         */
        private int getFlagTime() {
            return flagTime;
        }
    
        /**
         * Returns the total amount of deaths of the team including missing players penalty
         * 
         * @return amount of deaths
         */
        private int getDeaths() {
            int deaths;
            int counter;
            int playersNotHere;
            
            deaths = 0;
            counter = 0;
            
            for (BWJSPlayer i : players.values()) {
                deaths += i.getDeaths();
                counter++;
            }
            
            playersNotHere = cfg.getMaxPlayers() - counter;
            
            if (playersNotHere > 0) {
                deaths += cfg.getMaxDeaths() * playersNotHere;
            }
            
            return deaths;
        }
        
        /**
         * Returns the name of the current captain
         * 
         * @return name of the current captain
         */
        private String getCaptainName() {
            return captainName;
        }
        
        /**
         * Removes captain
         * - Notifies arena
         * - Sets captainName to [NONE]
         */
        private void captainLeft() {
            m_botAction.sendArenaMessage(captainName + " has been removed as captain of " + teamName + ".");
            captainName = "[NONE]";
            
            if  (captains.containsKey(captainsIndex)) {
                captains.get(captainsIndex).end();
            }
        }
        
        /**
         * Notify the arena that the captain has left the arena
         */
        private void captainLeftArena() {
            if (cfg.getAllowAutoCaps()) {
                m_botAction.sendArenaMessage("The captain of " + teamName 
                        + " has left the arena, anyone can claim cap with !cap");
            } else {
                m_botAction.sendArenaMessage("The captain of " + teamName 
                        + " has left the arena.");
            }
        }
        
        /**
         * Checks the position of the player if he or she is still located on the warpspot
         * If not it warps the player back on the warpspot
         * 
         * @param name Name of the player
         * @param x_coord X location of the player
         * @param y_coord Y location of the player
         */
        private void checkPostionPreGame(String name, int x_coord, int y_coord) {
            int x_index;
            int y_index;
            
            if (frequency == 0) {
                x_index = 0;
                y_index = 1;
            } else {
                x_index = 2;
                y_index = 3;
            }
            
            if (x_coord != cfg.getWarpSpot(x_index) || y_coord != cfg.getWarpSpot(y_index)) {
                m_botAction.warpTo(name, cfg.getWarpSpot(x_index), cfg.getWarpSpot(y_index));
            }
        }
        
        /**
         * Timestamps last position of the player
         * 
         * @param name name of the player
         */
        private void timestampLastPosition(String name) {
            name = name.toLowerCase();
            if (players.containsKey(name)) {
                players.get(name).timestampLastPosition();
            }
        }
        
        /**
         * Handles weaponFired event
         * - Passes it on to the player
         * 
         * @param name name of the player that fired the weapon
         * @param weaponType weapon type
         */
        private void weaponFired(String name, int weaponType) {
            name = name.toLowerCase();
            
            if (players.containsKey(name)) {
                players.get(name).weaponFired(weaponType);
            }
        }
        
        /**
         * Returns if its the team's turn to pick
         * 
         * @return true if its the team's turn, else false
         */
        private boolean isTurn() {
            return turnToPick;
        }
        
        /**
         * Returns the amount of players in the team.
         * Meaning all the players but the subbed ones
         * 
         * @return amount of players IN
         */
        private int getSizeIN() {
            int sizeIn;
            
            sizeIn = 0;
            
            for (BWJSPlayer i : players.values()) {
                if (i.p_state != BWJSPlayer.SUBBED) {
                    sizeIn++;
                }
            }
            
            return sizeIn;
        }
        
        /**
         * Returns the amount of ships of shiptype in use
         * 
         * @param shiptype type of ship
         * @return amount of shiptype in
         */
        private int getShipCount(int shiptype) {
            int shipCount;
            
            shipCount = 0;
            
            for (BWJSPlayer i : players.values()) {
                if (i.p_state < BWJSPlayer.SUBBED && i.p_currentShip == shiptype) {
                    shipCount++;
                }
            }
            
            return shipCount;
        }
        
        /**
         * Adds a player to the team
         * - Sending the arena, captain and the player a message
         * - Adding the player
         * 
         * @param p Player that is added
         * @param shipType shiptype 
         */
        private void addPlayer(Player p, int shipType) {
            String arenaMessage;    //Arena message
            String captainMessage;  //Captain message
            String playerMessage;   //Player message
            String p_lc;            //Player name in lowercase
            
            p_lc = p.getPlayerName().toLowerCase();
            
            captainMessage = p.getPlayerName() + " has been added.";
            playerMessage = "You've been added to the game.";
            
            if (cfg.announceShipType()) {
                arenaMessage = p.getPlayerName() + " is in for " + teamName + " as a " + Tools.shipName(shipType) + ".";
            } else {
                arenaMessage = p.getPlayerName() + " is in for " + teamName + ".";
            }
            
            /* Send the messages */
            m_botAction.sendArenaMessage(arenaMessage);
            m_botAction.sendPrivateMessage(captainName, captainMessage);
            m_botAction.sendPrivateMessage(p.getPlayerName(), playerMessage);
            
            if (!players.containsKey(p_lc)) {
                players.put(p_lc, new BWJSPlayer(p.getPlayerName(), shipType, cfg.getMaxDeaths(), frequency));
            } else {
                players.get(p_lc).putIN(shipType);
            }
        }
        
        /**
         * Returns turn value
         * 
         * @return true if its the teams turn to pick, else false
         */
        private boolean getTurn() {
            return turnToPick;
        }
        
        /**
         * Team has picked a player
         * - turnToPick set to false
         */
        private void picked() {
            turnToPick = false;
        }
        
        /**
         * Sets turn to true
         */
        private void setTurn() {
            turnToPick = true;
        }
    
        /**
         * Checks if the team has a captain
         * - Checks if captainName is equal to [NONE]
         * - Checks if captain is in the arena
         * 
         * @return true if the team has a captain, else false
         */
        private boolean hasCaptain() {
            if (captainName.equals("[NONE]")) {
                return false;
            } else if (m_botAction.getPlayer(captainName) == null) {
                return false;
            } else {
                return true;
            }   
        }
    
        /**
         * Sets captain
         * - Sets timestamp
         * - Sends arena message
         * 
         * @param name Name of the captain
         */
        private void setCaptain(String name) {
            Player p;
            
            p = m_botAction.getPlayer(name);
            
            if (p == null) {
                return;
            }
            
            captainName = p.getPlayerName();
            captainTimestamp = System.currentTimeMillis();
            
            m_botAction.sendArenaMessage(captainName + " is assigned as captain for " +
                   teamName, Tools.Sound.BEEP1);
            
            if (captains.containsKey(captainsIndex)) {
                if (!captains.get(captainsIndex).hasEnded()) {
                    captains.get(captainsIndex).end();
                }
            }
            
            if (state.getCurrentState() != BWJSState.WAITING_FOR_CAPS) {
                captainsIndex++;
                captains.put(captainsIndex, new BWJSCaptain(captainName));
            }
        }
        
        /**
         * Sets the current captain in the captainlist
         */
        private void putCaptainInList() {
            captainsIndex++;
            captains.put(captainsIndex, new BWJSCaptain(captainName));
        }
        
        /**
         * Returns if name is a captain or not
         * 
         * @return true if name is captain, else false
         */
        private boolean isCaptain(String name) {
            if (captainName.equalsIgnoreCase(name)) {
                return true;
            } else {
                return false;
            }
        }
        
        /**
         * Searches for name in team
         * 
         * @param name name of the player that needs to get found
         * @return BWJSPlayer if found, else null
         */
        private BWJSPlayer searchPlayer(String name) {
            BWJSPlayer p;
            
            p = null;
            name = name.toLowerCase();
            
            for (BWJSPlayer i : players.values()) {
                if (i.getName().toLowerCase().startsWith(name)) {
                    if (p == null) {
                        p = i;
                    } else if (i.getName().toLowerCase().compareTo(p.getName().toLowerCase()) > 0 ){
                        p = i;
                    }
                }
            }
            
            return p;
        }
        
        /**
         * Determines if a player is allowed back in with !lagout/!return
         * 
         * @param name name of the player that needs to get checked
         * @return true if allowed back in, else false
         */
        private boolean laginAllowed(String name) {
            BWJSPlayer p;
            Player player;
            boolean skipLagoutTime;
            
            skipLagoutTime = false;
            
            name = name.toLowerCase();
            
            if (!players.containsKey(name)) {
                return false;
            }
            
            p = players.get(name);
            
            switch (p.getCurrentState()) {
                case BWJSPlayer.IN : 
                    player = m_botAction.getPlayer(name);
                
                    if (player == null) {
                        return false;
                    }
                
                    if (player.getShipType() != Tools.Ship.SPECTATOR) {
                        return false;
                    }
                    
                    skipLagoutTime = true;
                    break;
                case BWJSPlayer.LAGOUT :
                    break;
                default : return false;
            }
            
            //Check if enough time has passed
            if (!skipLagoutTime) {
                if (System.currentTimeMillis() - p.getLagoutTimestamp() < BWJSPlayer.LAGOUT_TIME) {
                    return false;
                }
            }
            
            /*
             * All checks done
             */
            return true;
        }
    
        /**
         * Returns the corresponding error message of a not allowed !lagout
         * 
         * @param name name of the player that issued the !lagout command
         * @return Error message string
         */
        private String getLaginErrorMessage(String name) {
            BWJSPlayer p;
            Player player;
            boolean skipLagoutTime;
            
            skipLagoutTime = false;
            
            name = name.toLowerCase();
            
            if (!players.containsKey(name)) {
                return "ERROR: You are not on one of the teams.";
            }
            
            p = players.get(name);
            
            switch (p.getCurrentState()) {
                case BWJSPlayer.IN : 
                    player = m_botAction.getPlayer(name);
                
                    if (player == null) {
                        return "ERROR: Unknown";
                    }
                
                    if (player.getShipType() != Tools.Ship.SPECTATOR) {
                        return "Error: You have not lagged out.";
                    }
                    
                    skipLagoutTime = true;
                    break;
                case BWJSPlayer.LAGOUT :
                    break;
                default : return "ERROR: You have not lagged out.";
            }
            
            //Check if enough time has passed
            if (!skipLagoutTime) {
                if (System.currentTimeMillis() - p.getLagoutTimestamp() < BWJSPlayer.LAGOUT_TIME) {
                    return "You must wait for " + (BWJSPlayer.LAGOUT_TIME - 
                            (System.currentTimeMillis() - p.getLagoutTimestamp()))/Tools.TimeInMillis.SECOND
                             + " more seconds before you can return into the game.";
                }
            }
            
            return "ERROR: Unknown";
        }
    
        /**
         * Returns player into the game
         * 
         * @param name name of the player
         */
        private void lagin(String name) {
            name = name.toLowerCase();
            
            if (!players.containsKey(name)) {
                return;
            }
            
            players.get(name).lagin();
        }
     
        /**
         * Returns if player is currently playing or not
         * 
         * @param name name of the player
         * @return true if the player is IN, else false
         */
        private boolean isPlaying(String name) {
            name = name.toLowerCase();
            
            if (!players.containsKey(name)) {
                return false;
            }
            
            if (players.get(name).getCurrentState() == BWJSPlayer.IN) {
                return true;       
            } else {
                return false;
            }
        }
    
        /**
         * Returns team's frequency
         * 
         * @return frequency
         */
        private int getFrequency() {
            return frequency;
        }
    
        /**
         * Completely removes player from the team
         * 
         * @param name name of the player that needs to get removed
         */
        private void removePlayer(String name) {
            Player p;
            name = name.toLowerCase();
            
            if (players.containsKey(name)) {
                players.remove(name);
            }
            
            m_botAction.sendArenaMessage(name + " has been removed from " + teamName);
            
            p = m_botAction.getPlayer(name); 
            
            if (p == null) {
                return;
            }
            
            if (p.getShipType() != Tools.Ship.SPECTATOR) {
                m_botAction.specWithoutLock(name);
            }
            
            if (listNotplaying.contains(name)) {
                m_botAction.setFreq(name, FREQ_NOTPLAYING);
            } else {
                m_botAction.setFreq(name, FREQ_SPEC);
            }
        }
    
        /**
         * Sets player to not playing modus, player will still be subable
         * 
         * @param name Name of the player that should be set to out notplaying
         */
        private void setOutNotPlaying(String name) {
            name = name.toLowerCase();
            
            if (players.containsKey(name)) {
                players.get(name).out("out not playing");
            }
        }
    
        /**
         * Readies the team or sets it to not ready
         */
        private void ready() {
            if (!ready) {
                if (players.size() >= cfg.getMinPlayers()) {
                    m_botAction.sendArenaMessage(teamName + " is ready to begin.");
                    ready = true;
                } else {
                    m_botAction.sendPrivateMessage(captainName, "Cannot ready, not enough players in.");
                }
            } else {
                notReady();
            }
        }
        
        /**
         * Sets the team to not ready
         */
        private void notReady() {
            ready = false;
            m_botAction.sendArenaMessage(teamName + " is NOT ready to begin.");
        }
    
        /**
         * Returns if team is ready or not
         * 
         * @return true if team is ready, else false
         */
        private boolean isReady() {
            if (ready) {
                return true;
            } else {
                return false;
            }
        }
        
        /**
         * Returns if the team has any substitutes left
         * 
         * @return True if team has substitutes left, else false
         */
        private boolean hasSubtitutesLeft() {
            if (substitutesLeft > 0 || substitutesLeft == -1) {
                return true;
            } else {
                return false;
            }
        }
    
        /**
         * Handles the sub further
         * 
         * @param playerOne playerone  
         * @param playerTwo playertwo
         */
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
            if (cfg.getGameType() == BWJSConfig.BASE) {
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
            
            if (substitutesLeft != -1) {
                substitutesLeft--;
            }
            
            if (substitutesLeft >= 0)
                m_botAction.sendSmartPrivateMessage(captainName, "You have " + substitutesLeft + "substitutes left.");
            
        }
    
        /**
         * Switches playerOne with playerTwo
         * 
         * @param playerOne playerOne
         * @param playerTwo playerTwo
         */
        private void switchPlayers(BWJSPlayer playerOne, BWJSPlayer playerTwo) {
            m_botAction.sendArenaMessage(playerOne.p_name + " (" + Tools.shipName(playerOne.p_currentShip) + ") and "
                    + playerTwo.p_name + " (" + Tools.shipName(playerTwo.p_currentShip) + ") switched ships.");
            
            int playerOneShipType = playerTwo.p_currentShip;
            int playerTwoShipType = playerOne.p_currentShip;
            
            playerOne.p_currentShip = playerOneShipType;
            playerTwo.p_currentShip = playerTwoShipType;
            
            if (m_botAction.getPlayer(playerOne.p_name) != null) {
                playerOne.switchPlayer();
            }
            
            if (m_botAction.getPlayer(playerTwo.p_name) != null) {
                playerTwo.switchPlayer();
            }
        }
        
        /**
         * Returns the timestamp when the captain was set
         * 
         * @return timestamp of when the captain was set 
         */
        private long getCaptainTimeStamp() {
            return captainTimestamp;
        }
    
        /**
         * Adds a second to the flag time, and sends a warning on 3 and 1 minute left
         */
        private void addTimePoint() {
            if (flag) {
                flagTime++;
                if (((cfg.getTimeTarget() * 60) - flagTime) == 3 * 60)
                    m_botAction.sendArenaMessage(teamName + " needs 3 mins of flag time to win");
                if (((cfg.getTimeTarget() * 60) - flagTime) == 1 * 60)
                    m_botAction.sendArenaMessage(teamName + " needs 1 minute of flag time to win");
            }
        }
    
        /**
         * Check if team is dead
         * 
         * @return true if team is dead, else false
         */
        private boolean isDead() {
            for (BWJSPlayer i : players.values()) {
                if (i.getCurrentState() < BWJSPlayer.LAGOUT) {
                    return false;
                } else if (i.getCurrentState() == BWJSPlayer.LAGOUT) {
                    if ((System.currentTimeMillis() - i.getLagoutTimestamp()) <= 30 * Tools.TimeInMillis.SECOND) {
                        return false;
                    }
                }
            }
            return true;
        }
    
        /**
         * Returns sum of kills
         * 
         * @return sum of kills
         */
        private int getKills() {
            int kills = 0;
            
            for (BWJSPlayer i : players.values())
                kills += i.getKills();
            
            return kills;
        }
        
        /**
         * Returns sum of teamkills
         * 
         * @return sum of teamkills
         */
        private int getTeamKills() {
            int kills = 0;
            
            for (BWJSPlayer i : players.values())
                kills += i.getTeamKills();
            
            return kills;
        }
        
        /**
         * Returns sum of scores
         * 
         * @return sum of scores
         */
        private int getScore() {
            int score = 0;
            
            for (BWJSPlayer i : players.values())
                score += i.getTotalScore();
            
            return score;
        }
        
        /**
         * Returns sum of flags claimed
         * 
         * @return sum of flags claimed
         */
        private int getFlagsClaimed() {
            int flags = 0;
            
            for (BWJSPlayer i : players.values())
                flags += i.getFlagsClaimed();
            
            return flags;
        }
        
        /**
         * Returns the sum of terriers killed
         * 
         * @return sum of terriers killed
         */
        private int getTerrKills() {
            int kills = 0;
            
            for (BWJSPlayer i : players.values())
                kills += i.getTerrKills();
            
            return kills;
        }
        
        /**
         * Returns the sum of all ratings
         * 
         * @return sum of all ratings
         */
        private int getRating() {
            int rating = 0;
            
            for (BWJSPlayer i : players.values())
                rating += i.getTotalRating();
            
            return rating;
        }
        
        /**
         * Returns the total sum of all lagouts
         * 
         * @return total sum of all lagouts
         */
        private int getLagouts() {
            int lagouts = 0;
            
            for (BWJSPlayer i : players.values())
                lagouts += i.getLagouts();
            
            return lagouts;
        }
        
        /**
         * Get scores
         * 
         * @return returns a array with scores
         */
        private ArrayList<String> getScores() {
            ArrayList<String> out = new ArrayList<String>();
            
            if (cfg.getGameType() == BWJSConfig.BASE) {
                out.add("|                          ,------+------+------+-----------+------+------+-----------+----+");
                
                out.add("| " + Tools.formatString(teamName, 23) + " /  " +
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
                    //Sum of all played ships
                    out.add("|  " + Tools.formatString(p.p_name, 25) + " "
                            + Tools.rightString(Integer.toString(p.getKills()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getDeaths()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getTeamKills()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getTotalScore()), 9) + " | "
                            + Tools.rightString(Integer.toString(p.getFlagsClaimed()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getTerrKills()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getTotalRating()), 9) + " | "
                            + Tools.rightString(Integer.toString(p.getLagouts()), 2) + " |");
                    //Per ship
                    for (int i = 0; i < 9; i++) {
                        if (p.p_ship[i][BWJSPlayer.USED] == 1) {
                            out.add("|  " + Tools.formatString(" `-- " + Tools.shipName(i), 25) + " "
                                    + Tools.rightString(Integer.toString(p.getKills(i)), 4) + " | "
                                    + Tools.rightString(Integer.toString(p.p_ship[i][BWJSPlayer.DEATHS]), 4) + " | "
                                    + Tools.rightString(Integer.toString(p.getTeamKills(i)), 4) + " | "
                                    + Tools.rightString(Integer.toString(p.p_ship[i][BWJSPlayer.SCORE]), 9) + " | "
                                    + Tools.rightString(Integer.toString(p.p_ship[i][BWJSPlayer.FLAGS_CLAIMED]), 4) + " | "
                                    + Tools.rightString(Integer.toString(p.p_ship[i][BWJSPlayer.TERRIER_KILL]), 4) + " | "
                                    + Tools.rightString(Integer.toString(p.getRating(i)), 9) + " | "
                                    + Tools.rightString("-", 2) + " |");
                        }
                    }
                }
            } else  if (cfg.getGameType() == BWJSConfig.JAVDUEL){
                out.add("|                          ,------+------+------+-----------+----+");
                out.add("| " + Tools.formatString(teamName, 23) + " /  "
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
                            + Tools.rightString(Integer.toString(p.getTotalRating()), 9) + " | "
                            + Tools.rightString(Integer.toString(p.getLagouts()), 2) + " |");
                }
            } else {
                out.add("|                          ,------+------+-----------+----+");
                out.add("| " + Tools.formatString(teamName, 23) + " /  "
                        + Tools.rightString(Integer.toString(getKills()), 4) + " | "
                        + Tools.rightString(Integer.toString(getDeaths()), 4) + " | "
                        + Tools.rightString(Integer.toString(getRating()), 9) + " | "
                        + Tools.rightString(Integer.toString(getLagouts()), 2) + " |");
                out.add("+------------------------'        |      |           |    |");

                for (BWJSPlayer p : players.values()) {
                    out.add("|  " + Tools.formatString(p.p_name, 25) + " "
                            + Tools.rightString(Integer.toString(p.getKills()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getDeaths()), 4) + " | "
                            + Tools.rightString(Integer.toString(p.getTotalRating()), 9) + " | "
                            + Tools.rightString(Integer.toString(p.getLagouts()), 2) + " |");
                }
            }
            return out;
        }
    
        /**
         * Warps player to coords
         * 
         * @param x_coord x_coord
         * @param y_coord y_coord
         */
        private void warpTo(int x_coord, int y_coord) {
            for (BWJSPlayer i : players.values()) {
                int playerID = m_botAction.getPlayerID(i.p_name);
                
                if (playerID == -1) {
                    return;
                }
                
                m_botAction.warpTo(playerID, x_coord, y_coord);
            }
        }
    }
 
    /**
     * BWJSSQL - SQL related methods
     * 
     * @author fantus
     */
    private class BWJSSQL {
        private String database = "website";
        private String uniqueID = "bwjs";
        private int matchID = -1;
        
        private PreparedStatement psAddGame;
        private PreparedStatement psEndGame;
        
        private PreparedStatement psGetUserID;
        private PreparedStatement psSetUserID;
        
        private PreparedStatement psPutGamePlayerShipInfo;
        private PreparedStatement psPutGamePlayer;
        private PreparedStatement psPutGameCaptain;
        
        private BWJSSQL() {
            /* Game related */
            psAddGame = m_botAction.createPreparedStatement(database, uniqueID, 
                    "INSERT INTO tblbwjs__game(timeStarted, type) " +
                    "VALUES(NOW(),?)", true);
            psEndGame = m_botAction.createPreparedStatement(database, uniqueID, 
                    "UPDATE tblbwjs__game " +
                    "SET timeEnded = NOW(), winner = ? " +
                    "WHERE matchID = ?");
            
            /* Player related */
            psGetUserID = m_botAction.createPreparedStatement(database, uniqueID, 
                    "SELECT userID FROM tblbwjs__player " +
                    "WHERE playerName = ?");
            psSetUserID = m_botAction.createPreparedStatement(database, uniqueID, 
                    "INSERT INTO tblbwjs__player(playerName) " +
                    "VALUES(?)", true);
            
            /* Stats related */
            psPutGamePlayerShipInfo = m_botAction.createPreparedStatement(database, uniqueID,
                    "INSERT INTO tblbwjs__gameplayershipinfo(" +
                        "matchID, " +       //1
                        "userID, " +        //2
                        "ship, " +          //3
                        "score, " +         //4
                        "deaths, " +        //5
                        "WBkill, " +        //6
                        "JAVkill, " +       //7
                        "SPIDkill, " +      //8
                        "LEVkill, " +       //9
                        "TERRkill, " +      //10
                        "WEASkill, " +      //11
                        "LANCkill, " +      //12
                        "SHARKkill, " +     //13
                        "WBteamkill, " +    //14
                        "JAVteamkill, " +   //15
                        "SPIDteamkill, " +  //16
                        "LEVteamkill, " +   //17
                        "TERRteamkill, " +  //18
                        "WEASteamkill, " +  //19
                        "LANCteamkill, " +  //20
                        "SHARKteamkill, " + //21
                        "flagsclaimed, " +  //22
                        "bullets, " +       //23
                        "repels, " +        //24
                        "bombs, " +         //25
                        "mines, " +         //26
                        "burst, " +         //27
                        "playTime," +       //28
                        "rating) " +        //29
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            psPutGamePlayer = m_botAction.createPreparedStatement(database, uniqueID,
                    "INSERT INTO tblbwjs__gameplayer(" +
                        "matchID, " +       //1
                        "userID, " +        //2
                        "team, " +          //3
                        "lagouts, " +       //4
                        "MVP, " +           //5
                        "status) " +        //6
                    "VALUES(?,?,?,?,?,?)");
            psPutGameCaptain = m_botAction.createPreparedStatement(database, uniqueID,
                    "INSERT INTO tblbwjs__gamecaptain(" +
                        "matchID, " +       //1
                        "userID, " +        //2
                        "team, " +          //3 
                        "startTime, " +     //4
                        "endTime) " +       //5
                    "VALUES(?,?,?,?,?)");
        }
        
        private void addGame() {
            try {
                psAddGame.setString(1, cfg.getGameTypeString());
                psAddGame.execute();
                
                //Get matchID, -1 on database error
                ResultSet rs = psAddGame.getGeneratedKeys();
                if (rs != null && rs.next())
                    matchID = rs.getInt(1);
                else
                    matchID = -1;
            } catch (Exception e) { matchID = -1; }
        }
        
        private void endGame(int winner) {
            try {
                psEndGame.setInt(1, winner);
                psEndGame.setInt(2, matchID);
                psEndGame.execute();
            } catch (Exception e) {}
        }
        
        private int getUserID(String playerName) {
            int userID = -1;
            
            try {
                psGetUserID.setString(1, playerName);
                ResultSet rs = psGetUserID.executeQuery();
                
                if(rs != null && rs.next())
                    userID = rs.getInt(1);
                else
                    userID = setUserID(playerName);
            } catch (Exception e) {}
            
            return userID;
        }
        
        private int setUserID(String playerName) {
            int userID = -1;
            
            try {
                psSetUserID.setString(1, playerName);
                psSetUserID.execute();
                
                ResultSet rs = psSetUserID.getGeneratedKeys();
                if (rs != null && rs.next())
                    userID = rs.getInt(1);
                
            } catch (Exception e) {}
            
            return userID;
        }
        
        private void putPlayers() {
            try {
                for (BWJSTeam t : team) {
                    for (BWJSPlayer p : t.players.values()) {
                        for (int i = 0; i < p.p_ship.length; i++) {
                            if (p.p_ship[i][BWJSPlayer.USED] == 1) {
                                psPutGamePlayerShipInfo.clearParameters();
                                psPutGamePlayerShipInfo.setInt(1, matchID);            //matchID
                                psPutGamePlayerShipInfo.setInt(2, p.p_userID);         //userID
                                psPutGamePlayerShipInfo.setInt(3, i);                  //ship
                                psPutGamePlayerShipInfo.setInt(4, p.p_ship[i][BWJSPlayer.SCORE]);
                                psPutGamePlayerShipInfo.setInt(5, p.p_ship[i][BWJSPlayer.DEATHS]);
                                psPutGamePlayerShipInfo.setInt(6, p.p_ship[i][BWJSPlayer.WARBIRD_KILL]);
                                psPutGamePlayerShipInfo.setInt(7, p.p_ship[i][BWJSPlayer.JAVELIN_KILL]);
                                psPutGamePlayerShipInfo.setInt(8, p.p_ship[i][BWJSPlayer.SPIDER_KILL]);
                                psPutGamePlayerShipInfo.setInt(9, p.p_ship[i][BWJSPlayer.LEVIATHAN_KILL]);
                                psPutGamePlayerShipInfo.setInt(10, p.p_ship[i][BWJSPlayer.TERRIER_KILL]);
                                psPutGamePlayerShipInfo.setInt(11, p.p_ship[i][BWJSPlayer.WEASEL_KILL]);
                                psPutGamePlayerShipInfo.setInt(12, p.p_ship[i][BWJSPlayer.LANCASTER_KILL]);
                                psPutGamePlayerShipInfo.setInt(13, p.p_ship[i][BWJSPlayer.SHARK_KILL]);
                                psPutGamePlayerShipInfo.setInt(14, p.p_ship[i][BWJSPlayer.WARBIRD_TEAMKILL]);
                                psPutGamePlayerShipInfo.setInt(15, p.p_ship[i][BWJSPlayer.JAVELIN_TEAMKILL]);
                                psPutGamePlayerShipInfo.setInt(16, p.p_ship[i][BWJSPlayer.SPIDER_TEAMKILL]);
                                psPutGamePlayerShipInfo.setInt(17, p.p_ship[i][BWJSPlayer.LEVIATHAN_TEAMKILL]);
                                psPutGamePlayerShipInfo.setInt(18, p.p_ship[i][BWJSPlayer.TERRIER_TEAMKILL]);
                                psPutGamePlayerShipInfo.setInt(19, p.p_ship[i][BWJSPlayer.WEASEL_TEAMKILL]);
                                psPutGamePlayerShipInfo.setInt(20, p.p_ship[i][BWJSPlayer.LANCASTER_TEAMKILL]);
                                psPutGamePlayerShipInfo.setInt(21, p.p_ship[i][BWJSPlayer.SHARK_TEAMKILL]);
                                psPutGamePlayerShipInfo.setInt(22, p.p_ship[i][BWJSPlayer.FLAGS_CLAIMED]);
                                psPutGamePlayerShipInfo.setInt(23, p.p_ship[i][BWJSPlayer.BULLET_FIRED]);
                                psPutGamePlayerShipInfo.setInt(24, p.p_ship[i][BWJSPlayer.REPELS_USED]);
                                psPutGamePlayerShipInfo.setInt(25, p.p_ship[i][BWJSPlayer.BOMB_FIRED]);
                                psPutGamePlayerShipInfo.setInt(26, p.p_ship[i][BWJSPlayer.MINE_LAYED]);
                                psPutGamePlayerShipInfo.setInt(27, p.p_ship[i][BWJSPlayer.BURST_FIRED]);
                                psPutGamePlayerShipInfo.setInt(28, p.p_ship[i][BWJSPlayer.PLAY_TIME]);
                                psPutGamePlayerShipInfo.setInt(29, p.getRating(i));
                                
                                psPutGamePlayerShipInfo.execute();
                            }
                        }
                        
                        psPutGamePlayer.clearParameters();
                        psPutGamePlayer.setInt(1, matchID);                    //matchID
                        psPutGamePlayer.setInt(2, p.p_userID);                 //userID
                        psPutGamePlayer.setInt(3, p.p_frequency);              //freq
                        psPutGamePlayer.setInt(4, p.p_lagouts);                //lagouts
                        if (p.p_name.equalsIgnoreCase(getMVP()))
                            psPutGamePlayer.setInt(5, 1);                      //MVP
                        else
                            psPutGamePlayer.setInt(5, 0);                      //MVP
                        psPutGamePlayer.setString(6, p.getStatus());           //State
                        
                        psPutGamePlayer.execute();
                    }
                }
            } catch (Exception e) {}
        }
    
        private void putCaptains() {
            try {
                for (BWJSTeam t : team) {
                    if (!t.captains.isEmpty()) {
                        for (BWJSCaptain cap : t.captains.values()) {
                            psPutGameCaptain.clearParameters();
                            psPutGameCaptain.setInt(1, matchID);
                            psPutGameCaptain.setInt(2, getUserID(cap.captainName));
                            psPutGameCaptain.setInt(3, t.getFrequency());
                            psPutGameCaptain.setLong(4, cap.startTime);
                            psPutGameCaptain.setLong(5, cap.endTime);
                            
                            psPutGameCaptain.execute();
                        }
                    }
                }
            } catch (Exception e) {}
        }
    
        private void closePreparedStatements() {
            m_botAction.closePreparedStatement(database, uniqueID, psAddGame);
            m_botAction.closePreparedStatement(database, uniqueID, psEndGame);
            m_botAction.closePreparedStatement(database, uniqueID, psGetUserID);
            m_botAction.closePreparedStatement(database, uniqueID, psSetUserID);
            m_botAction.closePreparedStatement(database, uniqueID, psPutGamePlayerShipInfo);
            m_botAction.closePreparedStatement(database, uniqueID, psPutGamePlayer);
            m_botAction.closePreparedStatement(database, uniqueID, psPutGameCaptain);
        }
    }
    
    /**
     * Game Ticker
     * - Runs each second 
     * - Checks what needs to get checked
     * - Runs what needs to get runned
     * 
     * @author fantus
     */
    private class Gameticker extends TimerTask {
        public void run() {
            switch (state.getCurrentState()) {
                case BWJSState.OFF : break;
                case BWJSState.WAITING_FOR_CAPS : doWaitingForCaps(); break;
                case BWJSState.ADDING_PLAYERS : doAddingPlayers(); break;
                case BWJSState.PRE_GAME : doPreGame(); break;
                case BWJSState.GAME_IN_PROGRESS : doGameInProgress(); break;
                case BWJSState.GAME_OVER : doGameOver(); break;
            }
        }
        
        private void doWaitingForCaps() {
            /*
             * Need two captains within one minute, else remove captain
             */
            if (team[0].hasCaptain()) {
                if ((System.currentTimeMillis() - team[0].getCaptainTimeStamp()) >= Tools.TimeInMillis.MINUTE) {
                    team[0].captainLeft();
                }
            }
            
            if (team[1].hasCaptain()) {
                if ((System.currentTimeMillis() - team[1].getCaptainTimeStamp()) >= Tools.TimeInMillis.MINUTE) {
                    team[1].captainLeft();
                }
            }
            
            checkIfEnoughCaps();
        }
        
        private void doAddingPlayers() {
            /*
             * Check if time has ended for adding players
             */
            int multiplier;
            
            if (cfg.getGameType() == BWJSConfig.BASE) {
                multiplier = 10;
            } else {
                multiplier = 5;
            }
            
            if ((System.currentTimeMillis() - state.getTimeStamp()) >= Tools.TimeInMillis.MINUTE * multiplier) {
                m_botAction.sendArenaMessage("Time is up! Checking lineups..");
                checkLineup();
            }
        }
        
        private void doPreGame() {
            long time;
            
            time = (System.currentTimeMillis() - state.getTimeStamp()) / Tools.TimeInMillis.SECOND;
            
            if (time == 25) {
                m_botAction.showObject(cfg.getObject(1));
            } else if (time == 30) {
                startGame();
            }
        }
        
        private void doGameInProgress() {
            timeLeft--;
            
            for (BWJSTeam t : team) {
                for (BWJSPlayer p : t.players.values()) {
                    if (cfg.getInBase()) {
                        p.checkIfInBase();
                    }
                    
                    p.addPlayTime();
                }
            }
            
            updateScoreboard();
            
            if (cfg.getTimeTarget() != 0) {
                for (BWJSTeam t : team) {
                    t.addTimePoint();
                    
                    if (t.getFlagTime() == (cfg.getTimeTarget() * 60)) {
                        gameOver();
                    }
                }
            } else {
                if (team[0].isDead() || team[1].isDead()) {
                    gameOver();
                }
            }
            
            if (timeLeft <= 0) {
                gameOver();
            }
        }
        
        private void doGameOver() {
            long time;
            
            time = (System.currentTimeMillis() - state.getTimeStamp()) / Tools.TimeInMillis.SECOND;
            
            //Announce mvp
            if (time == 6 * Tools.TimeInMillis.SECOND) {
                m_botAction.sendArenaMessage("MVP: " + getMVP() + "!", Tools.Sound.INCONCEIVABLE);
            }
            
            if (!lockLastGame && (time == 15)) {
                startWaitingForCaps();
            } else if (time == 15){
                m_botAction.sendArenaMessage("Bot has been shutdown.", Tools.Sound.GAME_SUCKS);
                reset();
                unlockArena();
            }
        }
    }
}
