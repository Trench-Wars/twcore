package twcore.bots.attackbot;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.BallPosition;
import twcore.core.events.LoggedOn;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.SoccerGoal;
import twcore.core.game.Ship;
import twcore.core.util.Tools;

/**
 * This class runs the BallGame event. Two teams trying to score on their
 * enemy's goal. Players may use ships 1, 2, 3 or 7 only. If a team captures all
 * four flags in the middle, the doors at the enemy goal will open. First team
 * to 3 wins.
 * 
 * Made by fLaReD & WingZero.
 */
public class attackbot extends SubspaceBot {
    public BotAction ba;
    public EventRequester events; // event requester
    public OperatorList oplist; // operator list
    public BotSettings rules;
    private Ball ball;
    private int[] attack; // attack arena coords
    private int[] attack2; // attack2 arena coords
    private int goals, pick;
    private boolean autoMode; // if true then game is run with caps who volunteer via !cap
    private LinkedList<String> notplaying;
    private LinkedList<String> lagouts;
    
    private Team[] team;
    
    // Bot states
    public int state;
    public static final int IDLE = 0;
    public static final int STARTING = 1;
    public static final int PLAYING = 2;
    
    public static final int NP_FREQ = 666;
    public static final int SPEC_FREQ = 9999;
    public static int[] SHIP_LIMITS;
    

    /**
     * Requests events, sets up bot.
     */
    public attackbot(BotAction botAction) {
        super(botAction);
        ba = m_botAction;
        oplist = ba.getOperatorList();
        events = ba.getEventRequester();
        rules = ba.getBotSettings();
        events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(EventRequester.LOGGED_ON);
        events.request(EventRequester.MESSAGE);
        events.request(EventRequester.SOCCER_GOAL);
        events.request(EventRequester.BALL_POSITION);
        events.request(EventRequester.ARENA_JOINED);
        events.request(EventRequester.PLAYER_ENTERED);
        events.request(EventRequester.PLAYER_LEFT);
        notplaying  = new LinkedList<String>();
        lagouts = new LinkedList<String>();
        autoMode = true;
    }

    /**
     * Locks certain ships from play. Players can use ships 1, 2, 3 or 7. If
     * they attempt to switch to an illegal ship they will be placed in ship 1.
     */
    public void handleEvent(FrequencyShipChange event) {
        if (state == IDLE) return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null) return;
        if (event.getShipType() == 0 && isPlaying(name) && !lagouts.contains(name.toLowerCase())) {
            lagouts.add(name.toLowerCase());
            ba.sendPrivateMessage(name, "Use !lagout to return to the game.");
            Team t = getTeam(name);
            if (t != null && !t.isCap(name) && t.cap != null)
                ba.sendPrivateMessage(t.cap, name + " has lagged out.");
        }
        
        
        if (state > 0) {
            byte shipType = event.getShipType();
            int playerName = event.getPlayerID();
            if (shipType == 4 || shipType == 6) {
                ba.sendPrivateMessage(playerName,
                        "This ship type is not allowed. You may use any ships besides the leviathan and the weasel.");
                ba.setShip(playerName, 1);
            }
        }
    }

    /**
     * Handles the LoggedOn event
     */
    public void handleEvent(LoggedOn event) {
        ba.joinArena(ba.getBotSettings().getString("InitialArena"));
        state = IDLE;
        attack = new int[] { 478, 511, 544, 511 };
        attack2 = new int[] { 475, 512, 549, 512 };
        team = new Team[] { new Team(0), new Team(1) };
        goals = rules.getInt("Goals");
        SHIP_LIMITS = rules.getIntArray("Ships", ",");
        if (SHIP_LIMITS.length != 8)
            SHIP_LIMITS = new int[] { 1, 1, rules.getInt("MaxPlayers"), 0, 2, 0, rules.getInt("MaxPlayers"), 2 };
    }

    public void handleEvent(ArenaJoined event) {
        ball = new Ball();
        ba.toggleLocked();
        if (autoMode)
            ba.sendArenaMessage("A new game will begin when two players PM me with !cap", Tools.Sound.CROWD_GEE);
        ba.specAll();
        ba.setAlltoFreq(SPEC_FREQ);
    }
    
    /** Handles the PlayerEntered event **/
    public void handleEvent(PlayerEntered event) {
        if (state == IDLE) return;
        String name = event.getPlayerName();
        if (name == null) 
            name = ba.getPlayerName(event.getPlayerID());
        if (name == null) return;
        
        if (state == IDLE)
            ba.sendPrivateMessage(name, "A new game will begin after two players PM me with !cap");
        else if (state == STARTING) {
            String msg = "A game is about to start. ";
            if (team[0].cap != null)
                msg += team[0].cap + " and ";
            else
                msg += "-no captain- and ";
            if (team[1].cap != null)
                msg += team[1].cap + " ";
            else
                msg += "-no captain- ";
            msg += "are picking teams.";
            ba.sendPrivateMessage(name, msg);
        } else if (state == PLAYING) {
            ba.sendPrivateMessage(name, "A game is currently being played. Score: " + team[0].score + " - " + team[1].score);
        }
        
        if (lagouts.contains(name.toLowerCase()))
            ba.sendPrivateMessage(name, "Use !lagout to return to the game.");
        if (notplaying.contains(name.toLowerCase())) {
            ba.setFreq(name, NP_FREQ);
            ba.sendPrivateMessage(name, "You are still set as not playing and captains will be unable to pick you. If you want to play, use !notplaying again.");
        }
    }
    
    /** Handles the PlayerLeft event (lagouts) **/
    public void handleEvent(PlayerLeft event) {
        if (state == IDLE) return;
        String name = ba.getPlayerName(event.getPlayerID());
        if (name == null) return;
        
        if (isPlaying(name) && !lagouts.contains(name.toLowerCase()))
            lagouts.add(name.toLowerCase());
        Team t = getTeam(name);
        if (t != null && t.cap != null && !t.isCap(name))
            ba.sendPrivateMessage(t.cap, name + " has lagged out or left the arena.");
    }

    /**
     * Monitors goals scored.
     */
    public void handleEvent(SoccerGoal event) {
        if (state == PLAYING) {
            short scoringFreq = event.getFrequency();
            if (scoringFreq == 0)
                team[0].score++;
            else if (scoringFreq == 1)
                team[1].score++;
            handleGoal();
        }
    }

    public void handleEvent(BallPosition event) {
        ball.update(event);
    }

    /**
     * Command handler.
     */
    public void handleEvent(Message event) {
        String name = event.getMessager();
        if (name == null || name.length() < 1)
            name = ba.getPlayerName(event.getPlayerID());
        String msg = event.getMessage();
        int type = event.getMessageType();
        
        if (type == Message.ARENA_MESSAGE) {
            if (autoMode && msg.contains("Arena UNLOCKED"))
                ba.toggleLocked();
        }
        
        if (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) {

            if (msg.equalsIgnoreCase("!help"))
                help(name);
            else if (msg.equalsIgnoreCase("!status"))
                getStatus(name);
            else if (msg.equalsIgnoreCase("!caps"))
                getCaps(name);
            else if (msg.equalsIgnoreCase("!ready"))
                ready(name);
            else if (msg.equalsIgnoreCase("!cap"))
                setCaptain(name);
            else if (msg.startsWith("!add "))
                add(name, msg);
            else if (msg.startsWith("!remove "))
                remove(name, msg);
            else if (msg.startsWith("!change "))
                changeShip(name, msg);
            else if (msg.startsWith("!sub "))
                sub(name, msg);
            else if (msg.startsWith("!switch "))
                switchShips(name, msg);
            else if (msg.equalsIgnoreCase("!lagout"))
                lagout(name);
            else if (msg.equalsIgnoreCase("!list"))
                sendTeams(name);
            else if (msg.equalsIgnoreCase("!notplaying") || msg.equalsIgnoreCase("!np"))
                notPlaying(name);
            else if (msg.equalsIgnoreCase("!removecap"))
                removeCap(name);

            if (oplist.isER(name)) {
                if (msg.equalsIgnoreCase("!drop"))
                    dropBall();
                else if (msg.equalsIgnoreCase("!start"))
                    startGame();
                else if (msg.equalsIgnoreCase("!killgame"))
                    stopGame(name);
                else if (msg.equalsIgnoreCase("!die"))
                    die(name);
                else if (msg.startsWith("!go "))
                    go(name, msg);
                else if (msg.startsWith("!setcap "))
                    setCaptain(name, msg);
            }
        }
    }

    /**
     * !help Displays help message.
     */
    public void help(String name) {
        String[] help = {
                "+-- AttackBot Commands ---------------------------------------------------------------------.",
                "| !status                  - Displays the current game status                               |",
                "| !lagout                  - Returns a player to the game after lagging out                 |",
                "| !notplaying              - (!np) Toggles not playing which will prevent being added if on |",
                "| !list                    - Displays list of each teams player information                 |",
                "| !caps                    - Displays the captains of each team                             |",
                "| !cap                     - Volunteers for assignment to captain of a team                 |",
        };
        String[] cap = {
                "+-- Captain Commands -----------------------------------------------------------------------+",
                "| !ready                   - Use after picking when ready to begin the game                 |",
                "| !add <name>:<ship>       - Adds <name> to game in ship number <ship>                      |",
                "| !remove <name>           - Removes <name> from game (during picking)                      |",
                "| !change <name>:<ship>    - Changes <name> to ship number <ship>                           |",
                "| !switch <name1>:<name2>  - Switches the ships of <name1> and <name2>                      |",
                "| !sub <player>:<specer>   - Substitutes <player> for <specer>                              |",
                "| !list                    - Lists players, ships, and statuses for each team               |",
                "| !removecap               - Removes you from captain allowing someone else to cap          |",
        };
        
        String[] staff = {
                "+-- Staff Commands -------------------------------------------------------------------------+",
                "| !setcap <team>:<name>    - Sets <name> as captain of <team> (0 or 1)                      |",
                "| !go <arena>              - Sends bot to <arena>                                           |",
                "| !die                     - Kills bot                                                      |",
                "| !killgame                - Abruptly kills the current game                                |",
        };
        String end = "`-------------------------------------------------------------------------------------------'";
        ba.privateMessageSpam(name, help);
        if (isCaptain(name))
            ba.privateMessageSpam(name, cap);
        if (oplist.isZH(name))
            ba.privateMessageSpam(name, staff);
        ba.sendPrivateMessage(name, end);
    }
    
    /** Handles the !notplaying command which prevents a player from being added and/or removes them from the game **/
    public void notPlaying(String name) {
        if (!isPlaying(name)) {
            if (!notplaying.contains(name.toLowerCase())) {
                notplaying.add(name.toLowerCase());
                ba.setFreq(name, NP_FREQ);
                ba.sendPrivateMessage(name, "You have been added to not playing. Captains will not be able to add you. If you wish to return, do !notplaying again.");
            } else {
                notplaying.remove(name.toLowerCase());
                ba.setShip(name, 1);
                ba.specWithoutLock(name);
                ba.sendPrivateMessage(name, "You have been removed from not playing. Captains will be able to add you.");
            }
        } else {
            getTeam(name).notPlaying(name);
            ba.setFreq(name, NP_FREQ);
            ba.sendPrivateMessage(name, "You have been added to not playing. Captains will not be able to add you. If you wish to return, do !notplaying again.");
        }
    }
    
    /** Handles the !setcap command which assigns a player as captain of a team (0 or 1) **/
    public void setCaptain(String name, String cmd) {
        if (cmd.length() < 8 || !cmd.contains(" ") || !cmd.contains(":")) return;
        String cap = ba.getFuzzyPlayerName(cmd.substring(cmd.indexOf(":") + 1));
        if (cap == null) {
            ba.sendPrivateMessage(name, "Player not found in this arena.");
            return;
        }
        
        if (isCaptain(cap)) {
            ba.sendPrivateMessage(name, cap + " is already the captain of a team.");
            return;
        }
        
        
        try {
            int freq = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":")));
            if (freq == 0 || freq == 1) {
                if (isPlaying(cap) && getTeam(cap).freq == freq) {
                    ba.sendPrivateMessage(name, cap + " is playing on the other team.");
                    return;
                }
                team[freq].cap = cap;
                ba.sendArenaMessage(cap + " has been assigned captain of freq " + team[freq].freq, Tools.Sound.BEEP1);
                if (state == IDLE && team[0].cap != null && team[1].cap != null)
                    startPicking();
            }
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Invalid team number (must be 0 or 1).");
        }
    }
    
    /**
     * Handles the !cap command which will assign the sender as captain of a team without a captain
     * if a game or picking has not yet begun. Otherwise, if a captain has left the arena then the
     * sender will replace the absent captain unless sender is on opposite team.
     * @param name
     */
    public void setCaptain(String name) {
        if (isCaptain(name)) return;
        if (state == IDLE) {
            if (team[0].cap == null) {
                team[0].cap = name;
                ba.sendArenaMessage(name + " has been assigned captain of freq " + team[0].freq, Tools.Sound.BEEP1);
            } else if (team[1].cap == null) {
                team[1].cap = name;
                ba.sendArenaMessage(name + " has been assigned captain of freq " + team[1].freq, Tools.Sound.BEEP1);                
            } else return;
            
            if (team[0].cap != null && team[1].cap != null)
                startPicking();
        } else if (!isCaptain(name)) {
            if (isPlaying(name)) {
                Team t = getTeam(name);
                if (ba.getFuzzyPlayerName(t.cap) == null) {
                    t.cap = name;
                    ba.sendArenaMessage(name + " is now the captain of Freq " + t.freq);
                }
            } else {
                if (ba.getFuzzyPlayerName(team[0].cap) == null) {
                    team[0].cap = name;
                    ba.sendArenaMessage(name + " is now the captain of Freq " + team[0].freq);
                } else if (ba.getFuzzyPlayerName(team[1].cap) == null) {
                    team[1].cap = name;
                    ba.sendArenaMessage(name + " is now the captain of Freq " + team[1].freq);
                }
            }
        }
    }
    
    /** Handles the !removecap command which removes the captain from being captain **/
    public void removeCap(String name) {
        if (!isCaptain(name)) return;
        getTeam(name).removeCap();
    }
    
    /** Handles the !caps command which returns the current team captains **/
    public void getCaps(String name) {
        String msg = "";
        if (team[0].cap != null)
            msg = team[0].cap + " is captain of Freq " + team[0].freq + ".";
        else msg = "Freq " + team[0].freq + " has no captain.";
        ba.sendPrivateMessage(name, msg);
        if (team[1].cap != null)
            msg = team[1].cap + " is captain of Freq " + team[1].freq + ".";
        else msg = "Freq " + team[1].freq + " has no captain.";
        ba.sendPrivateMessage(name, msg);
    }
    
    /** Handles the !list command which sends a list of team players with ships and statuses **/
    public void sendTeams(String name) {
        team[0].sendTeam(name);
        ba.sendPrivateMessage(name, "`");
        team[1].sendTeam(name);
    }

    /** Handles the !add command if given by a captain **/
    public void add(String name, String msg) {
        if (state == IDLE || !isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 5) return;
        Team t = getTeam(name);
        if (t == null) return;
        if (state == STARTING && !t.pick) {
            ba.sendPrivateMessage(name, "It is not your turn");
            return;
        }
        String player = msg.substring(msg.indexOf(" ") + 1, msg.indexOf(":"));
        int ship = 3;
        try {
            ship = Integer.valueOf(msg.substring(msg.indexOf(":") + 1).trim());
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Invalid ship number");
            return;
        }
        if (ship > 0 && ship < 9) {
            String res = t.addPlayer(player, ship);
            ba.sendPrivateMessage(name, res);
            if (state == STARTING && res.contains("added to game"))
                nextPick();
        }
    }

    /** Handles the !remove command if given by a captain **/
    public void remove(String name, String msg) {
        if (state != STARTING || !isCaptain(name) || !msg.contains(" ") || msg.length() < 8) return;
        Team t = getTeam(name);
        if (t == null) return;
        String res = t.remove(msg.substring(msg.indexOf(" ") + 1));
        if (res != null)
            ba.sendPrivateMessage(name, res);
        
    }

    /** Handles the !sub command if given by a captain **/
    public void sub(String name, String msg) {
        if (state == IDLE || !isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 5) return;
        Team t = getTeam(name);
        if (t == null) return;
        String[] players = msg.substring(msg.indexOf(" ") + 1).split(":");
        String res = t.subPlayer(players[0], players[1]);
        if (res != null)
            ba.sendPrivateMessage(name, res);
    }
    
    /** Handles the !change command if given by a captain **/
    public void changeShip(String name, String msg) {
        if (!isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 8) return;
        Team t = getTeam(name);
        if (t == null) return;
        String player = msg.substring(msg.indexOf(" ") + 1, msg.indexOf(":"));
        int ship = 3;
        try {
            ship = Integer.valueOf(msg.substring(msg.indexOf(":") + 1).trim());
        } catch (NumberFormatException e) {
            ba.sendPrivateMessage(name, "Invalid ship number");
            return;
        }
        if (ship > 0 && ship < 9) {
            String res = t.changeShip(player, ship);
            if (res != null)
                ba.sendPrivateMessage(name, res);
        }
    }
    
    /** Handles the !switch command if given by a captain **/
    public void switchShips(String name, String msg) {
        if (!isCaptain(name) || !msg.contains(" ") || !msg.contains(":") || msg.length() < 8) return;
        Team t = getTeam(name);
        if (t == null) return;
        String[] players = msg.substring(msg.indexOf(" ") + 1).split(":");
        String res = t.switchPlayers(players[0], players[1]);
        if (res != null)
            ba.sendPrivateMessage(name, res);
    }
    
    /** Handles the !ready command if given by a team captain **/
    public void ready(String name) {
        if (state != STARTING || !isCaptain(name)) return;
        if (team[0].isCap(name)) {
            if (team[0].ready) {
                team[0].ready = false;
                ba.sendArenaMessage("Freq " + team[0].freq + " is not ready.", Tools.Sound.BEEP1);
            } else if (team[1].ready) {
                team[0].ready = true;
                ba.sendArenaMessage("Freq " + team[0].freq + " is ready.", Tools.Sound.BEEP1);
                // start game
                startGame();
            } else {
                team[0].ready = true;
                ba.sendArenaMessage("Freq " + team[0].freq + " is ready.", Tools.Sound.BEEP1);
            }
        } else if (team[1].isCap(name)) {
            if (team[1].ready) {
                team[1].ready = false;
                ba.sendArenaMessage("Freq " + team[1].freq + " is not ready.", Tools.Sound.BEEP1);
            } else if (team[0].ready) {
                team[1].ready = true;
                ba.sendArenaMessage("Freq " + team[1].freq + " is ready.", Tools.Sound.BEEP1);
                // start game
                startGame();
            } else {
                team[1].ready = true;
                ba.sendArenaMessage("Freq " + team[1].freq + " is ready.", Tools.Sound.BEEP1);
            }
        }
    }
    
    /** Returns a lagged out player to the game **/
    public void lagout(String name) {
        if (lagouts.contains(name.toLowerCase())) {
            lagouts.remove(name.toLowerCase());
            Team t = getTeam(name);
            if (t == null) return;
            ba.setShip(name, t.getShip(name));
            ba.setFreq(name, t.freq);
        } else 
            ba.sendPrivateMessage(name, "You are not lagged out and/or not in the game.");
    }

    /** Sends the bot to a specified arena (only attack and attack2 are currently supported) **/
    public void go(String name, String cmd) {
        String arena = cmd.substring(cmd.indexOf(" ") + 1);
        if (arena.length() > 0) {
            ba.sendSmartPrivateMessage(name, "Moving to " + arena);
            ba.changeArena(arena);
        }
    }

    /**
     * !die Kills bot.
     */
    public void die(String name) {
        ba.sendSmartPrivateMessage(name, "I'm melting! I'm melting...");
        ba.cancelTasks();
        ba.die();
    }

    /**
     * !stop Stops a game if one is running.
     */
    public void stopGame(String name) {
        if (state > 0) {
            ba.sendArenaMessage("This game has been killed by " + name);
            state = IDLE;
            team[0].reset();
            team[1].reset();
            ba.specAll();
        } else if (state == IDLE)
            ba.sendPrivateMessage(name, "There is no game currently running.");
    }

    /**
     * !status Displays the score to player if a game is running.
     */
    public void getStatus(String name) {
        if (state == PLAYING) {
            ba.sendPrivateMessage(name, "[---  SCORE  ---]");
            ba.sendPrivateMessage(name, "[--Freq 0: " + team[0].score + " --]");
            ba.sendPrivateMessage(name, "[--Freq 1: " + team[1].score + " --]");
        } else if (state == IDLE) {
            if (autoMode)
                ba.sendPrivateMessage(name, "A new game will begin after two players volunteer to captain using !cap");
            else 
                ba.sendPrivateMessage(name, "There is no game currently running.");
        } else if (autoMode) {
            String msg = "We are currently picking teams. Captains: ";
            if (team[0].cap != null)
                msg += team[0].cap;
            else
                msg += "[needs captain]";
            msg += " vs ";
            if (team[1].cap != null)
                msg += team[1].cap;
            else
                msg += "[needs captain]";
            ba.sendPrivateMessage(name, msg);
        }
    }
    
    /** Begins the player picking process if both team's have a captain **/
    private void startPicking() {
        if (team[0].cap == null || team[1].cap == null)
            return;
        state = STARTING;
        pick = 0;
        team[0].pick = true;
        team[1].pick = false;
        ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);        
    }

    /**
     * !start Starts a game. Warps players to safe for 10 seconds and calls the
     * runGame() method.
     */
    public void startGame() {
        state = PLAYING;
        ba.sendArenaMessage("Both teams are ready, game will start in 10 seconds!", 1);
        TimerTask t = new TimerTask() {
            public void run() {
                runGame();
            }
        };
        ba.scheduleTask(t, 10000);
    }

    /**
     * Runs the game. Resets all variables, warps players to the arena and
     * begins game.
     */
    public void runGame() {
        team[0].score = 0;
        team[1].score = 0;
        ba.shipResetAll();
        if (ba.getArenaName().equalsIgnoreCase("attack")) {
            ba.warpFreqToLocation(0, attack[0], attack[1]);
            ba.warpFreqToLocation(1, attack[2], attack[3]);
        } else {
            ba.warpFreqToLocation(0, attack2[0], attack2[1]);
            ba.warpFreqToLocation(1, attack2[2], attack2[3]);
        }
        ba.sendArenaMessage("GOGOGO!!!", 104);
        ba.scoreResetAll();
        ba.resetFlagGame();
        dropBall();
    }    
    
    /**
     * Gets the Team object of a player
     * @param name
     * @return Team object or null if player is not on a team
     */
    private Team getTeam(String name) {
        if (team[0].isPlayersTeam(name) || team[0].isCap(name))
            return team[0];
        else if (team[1].isPlayersTeam(name) || team[1].isCap(name))
            return team[1];
        else return null;
    }
    
    /** Determines which team should have the next pick turn and executes **/
    private void nextPick() {
        if (pick == 0) {
            if (team[1].size() <= team[0].size()) {
                if (!team[1].isFull()) {
                    pick = 1;
                    team[1].pick = true;
                    team[0].pick = false;
                    ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);  
                }
            } else if (!team[0].isFull())
                ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);
        } else {
            if (team[0].size() <= team[1].size()) {
                if (!team[0].isFull()) {
                    pick = 0;
                    team[0].pick = true;
                    team[1].pick = false;
                    ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2); 
                }
            } else if (!team[1].isFull())
                ba.sendArenaMessage(team[pick].cap + " pick a player!", Tools.Sound.BEEP2);
        }
    }

    /**
     * Handles goals. Determines total score of each team and if there is a
     * winner.
     */
    public void handleGoal() {
        if (team[0].score == goals) {
            ba.sendArenaMessage("GAME OVER: Freq 0 wins!", 5);
            ba.sendArenaMessage("Final score: " + team[0].score + " - " + team[1].score);
        } else if (team[1].score == goals) {
            ba.sendArenaMessage("GAME OVER: Freq 1 wins!", 5);
            ba.sendArenaMessage("Final score: " + team[0].score + " - " + team[1].score);
        } else {
            ba.sendArenaMessage("Score: " + team[0].score + " - " + team[1].score);
            ba.shipResetAll();
            ba.resetFlagGame();
            if (ba.getArenaName().equalsIgnoreCase("attack")) {
                ba.warpFreqToLocation(0, attack[0], attack[1]);
                ba.warpFreqToLocation(1, attack[2], attack[3]);
            } else {
                ba.warpFreqToLocation(0, attack2[0], attack2[1]);
                ba.warpFreqToLocation(1, attack2[2], attack2[3]);
            }
            dropBall();
            return;
        }
        
        state = IDLE;
        team[0].reset();
        team[1].reset();
        if (autoMode) {
            pick = 0;
            ba.sendArenaMessage("A new game will begin when two players pm me with !cap to captain each team!", Tools.Sound.CROWD_GEE);
            ba.specAll();
        }
    }

    /** Bot grabs the ball regardless of its status and drops it into the center after warping each team **/
    public void dropBall() {
        Ship s = ba.getShip();
        s.setShip(0);
        s.setFreq(1234);
        final TimerTask drop = new TimerTask() {
            public void run() {
                ba.getShip().move(512 * 16, 600 * 16);
                ba.getShip().sendPositionPacket();
                try {
                    Thread.sleep(75);
                } catch (InterruptedException e) {}
                ba.getBall(ball.getBallID(), (int) ball.getTimeStamp());
                ba.getShip().move(512 * 16, 512 * 16);
                ba.getShip().sendPositionPacket();
                try {
                    Thread.sleep(75);
                } catch (InterruptedException e) {}
            }
        };
        drop.run();
        ba.specWithoutLock(ba.getBotName());
    }
    
    /** Determines if the given player is playing in the game or not **/
    private boolean isPlaying(String name) {
        if (state != IDLE && team[0].isPlayersTeam(name) || team[1].isPlayersTeam(name))
            return true;
        else
            return false;
    }
    
    /** Determines if the given player is assigned as captain of a team **/
    private boolean isCaptain(String name) {
        return team[0].isCap(name) || team[1].isCap(name);
    }    
    
    /** Helper determines if player is a bot (oplist doesn't work for bots on other cores) **/
    private boolean isNotBot(String name) {
        if (oplist.isBotExact(name) || (oplist.isSysop(name) && !oplist.isOwner(name) && !name.equalsIgnoreCase("flared") && !name.equalsIgnoreCase("Witness") && !name.equalsIgnoreCase("Pure_Luck")))
            return false;
        else return true;
    }
    
    /** Helper method adds spaces to a string to meet a certain length **/
    private String padString(String str, int length) {
        for (int i = str.length(); i < length; i++)
            str += " ";
        return str.substring(0, length);
    }
    
    /**
     * Holds basic team information and also handles all team-related command executions.
     */
    private class Team {
        // ship counts (must be updated by FSC events)
        int[] ships;
        // player list with ships
        HashMap<String, Integer> players;
        int freq, score;
        String cap;
        boolean ready, pick;
        
        public Team(int f) {
            cap = null;
            freq = f;
            ships = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
            players = new HashMap<String, Integer>();
            score = 0;
            ready = false;
            pick = false;
        }
        
        /**
         * Adds player for this team in a given ship
         * @param name
         * @param ship
         * @return Message for player or null if successful 
         */
        public String addPlayer(String name, int ship) {
            String result = "";
            String p = ba.getFuzzyPlayerName(name);
            if (p == null)
                return "Could not find " + name;
            if (!isNotBot(name))
                return "Unable to add bot " + name;
            if (notplaying.contains(name.toLowerCase()))
                return "" + name + " is currently not playing";
            if (isPlaying(name) || (!p.equalsIgnoreCase(cap) && isCaptain(p)))
                return "" + name + " is already on a team";
            if (ships[ship-1] >= SHIP_LIMITS[ship-1])
                return "Only " + SHIP_LIMITS[ship-1] + " " + Tools.shipName(ship) + "(s) are allowed per team";
            if (players.size() >= rules.getInt("MaxPlayers"))
                return "Team is full";
            ba.setShip(p, ship);
            ba.setFreq(p, freq);
            ba.scoreReset(p);
            ships[ship-1]++;
            players.put(p.toLowerCase(), ship);
            result = "Player " + p + " added to game";
            ba.sendPrivateMessage(p, "You have been put in the game");
            ba.sendArenaMessage(p + " in for " + cap + " with ship " + ship);
            return result;
        }
        
        /**
         * Removes a player from this team (should only be called during picking)
         * @param name
         * @return Message for player or null if successful
         */
        public String remove(String name) {
            String result = "";
            String p = findPlayer(name);
            if (p != null) {
                ba.specWithoutLock(p);
                ships[players.remove(p)-1]--;
                ba.sendArenaMessage(p + " has been removed from Freq " + freq);
                result = null;
            } else
                result = "Player not found.";
            return result;
        }
        
        /** Removes a player from the game after using !notplaying while in **/
        public void notPlaying(String name) {
            ships[players.remove(name.toLowerCase())-1]--;
            ba.specWithoutLock(name);
            ba.sendArenaMessage(name + " has been removed from the game. (not playing)");
            if (isCap(name))
                removeCap();
        }
        
        /**
         * Substitutes a player on this team for a player from spec
         * @param out
         * @param in
         * @return Message for player or null if successful
         */
        public String subPlayer(String out, String in) {
            String result = "";
            String subin = ba.getFuzzyPlayerName(in);
            String subout = findPlayer(out);
            if (subin != null && subout != null) {
                if (!this.isPlayersTeam(subout))
                    return out + " is not on your team or in the game";
                if (isPlaying(subin))
                    return in + " is already playing in this game";
                if (notplaying.contains(subin.toLowerCase()))
                    return "" + subout + " is currently not playing";
                int ship = players.remove(subout.toLowerCase());
                ba.specWithoutLock(subout);
                ba.setFreq(subout, freq);
                ba.setShip(subin, ship);
                ba.setFreq(subin, freq);
                players.put(subin.toLowerCase(), ship);
                ba.sendPrivateMessage(subin, "You have been put in the game");
                ba.sendArenaMessage(subout + " has been substituted by " + subin + ".");
                result = null;
            } else {
                if (subout == null)
                    result = subout + " is not on your team";
                else if (subin == null)
                    result = "Player " + in + " not found";
            }
            return result;
        }
        
        /**
         * Switches the ships of two players from this team
         * @param name1
         * @param name2
         * @return Message for player or null if successful
         */
        public String switchPlayers(String name1, String name2) {
            String result = "";
            String p1 = findPlayer(name1);
            String p2 = findPlayer(name2);
            if (p1 != null && p2 != null) {
                int preship1 = players.get(p1);
                int preship2 = players.get(p2);
                if (preship1 == preship2)
                    result = p1 + " and " + p2 + " are in the same ship";
                else {
                    ba.setShip(p1, preship2);
                    players.put(p1, preship2);
                    ba.setShip(p2, preship1);
                    players.put(p2, preship1);
                    name1 = ba.getFuzzyPlayerName(p1);
                    name2 = ba.getFuzzyPlayerName(p2);
                    if (name1 != null)
                        result += name1 + " (" + preship1 + ") and ";
                    else result += p1 + " (" + preship1 + ") and ";
                    if (name2 != null)
                        result += name2 + " (" + preship2 + ") ";
                    else result += p2 + " (" + preship2 + ") ";
                    result += "switched ships.";
                    ba.sendArenaMessage(result);
                    result = null;
                }
            }
            return result;
        }
        
        /**
         * Changes the ship of a player
         * @param name
         * @param ship
         * @return Message for player or null if successful
         */
        public String changeShip(String name, int ship) {
            String result = "";
            String p = findPlayer(name);
            if (p != null) {
                name = ba.getFuzzyPlayerName(p);
                if (name == null)
                    name = p;
                if (ship == players.get(p))
                    result = name + " is already in ship " + ship;
                else if (ships[ship-1] >= SHIP_LIMITS[ship-1])
                    if (SHIP_LIMITS[ship-1] == 0)
                        result = "Ship not allowed";
                    else result = "Only " + SHIP_LIMITS[ship-1] + " " + Tools.shipName(ship) + " are allowed per team";
                else {
                    ba.setShip(name, ship);
                    ba.sendArenaMessage(name + " changed from ship " + players.get(p) + " to ship " + ship);
                    ships[players.get(p)-1]--;
                    ships[ship-1]++;
                    players.put(p, ship);
                    result = null;
                }
            }
            return result;
        }
        
        /** Used by the current captain to remove themselves from captain **/
        public void removeCap() {
            ba.sendArenaMessage(cap + " has been removed from captain of Freq " + freq + ". Use !cap to claim captainship.", Tools.Sound.GAME_SUCKS);
            cap = null;
        }
        
        /**
         * Searches the team players with a name or part of a name
         * @param name
         * @return The name of the player in lowercase or null if not found
         */
        private String findPlayer(String name) {
            String best = null;
            for (String i : players.keySet()) {
                if (i.startsWith(name.toLowerCase()))
                    if (best == null)
                        best = i;
                    else if (best.compareToIgnoreCase(i) > 0)
                        best = i;
                
                if (best != null && best.equalsIgnoreCase(name))
                    return best.toLowerCase();
            }
            return best;
        }
        
        /** 
         * Returns the ship of a player on the team
         * @param name
         * @return Ship of player or null if not found
         */
        public int getShip(String name) {
            return players.get(name.toLowerCase());
        }
        
        /** Check if the given player is on this team **/
        public boolean isPlayersTeam(String p) {
            if (players.containsKey(p.toLowerCase()))
                return true;
            else
                return false;
        }
        
        /** Check if the given player is the team's captain **/
        public boolean isCap(String name) {
            if (cap != null && cap.equalsIgnoreCase(name))
                return true;
            else return false;
        }
        
        /** Returns the current size of the players in for this team **/
        public int size() {
            return players.size();
        }
        
        public boolean isFull() {
            return players.size() == rules.getInt("MaxPlayers");
        }
        
        /** PMs a list of the current players on this team to name **/
        public void sendTeam(String name) {
            String msg = "Freq " + freq + " (captain: ";
            if (cap != null)
                msg += cap;
            msg += ")";
            ba.sendPrivateMessage(name, msg);
            if (size() < 1) return;
            msg = "Name:                   - Ship:      - Status:";
            ba.sendPrivateMessage(name, msg);
            for (String p : players.keySet()) {
                msg = padString(p, 23);
                msg += " - ";
                msg += padString(Tools.shipName(players.get(p)), 10);
                msg += " - ";
                if (lagouts.contains(p))
                    msg += "LAGGED OUT";
                else 
                    msg += "IN";
                ba.sendPrivateMessage(name, msg);
            }
            
        }
        
        /** Resets all team variables to default **/
        public void reset() {
            cap = null;
            score = 0;
            players.clear();
            ships = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
            ready = false;
        }
    }

    private class Ball {

        private byte ballID;
        private long timestamp;
        private short ballX;
        private short ballY;
        private boolean carried;
        private String carrier;
        private final Stack<String> carriers;
        private boolean holding;

        public Ball() {
            carrier = null;
            carriers = new Stack<String>();
            carried = false;
            holding = false;
        }

        /**
         * Called by handleEvent(BallPosition event)
         * 
         * @param event
         *            the ball position
         */
        public void update(BallPosition event) {
            ballID = event.getBallID();
            this.timestamp = event.getTimeStamp();
            ballX = event.getXLocation();
            ballY = event.getYLocation();
            short carrierID = event.getCarrier();
            if (carrierID != -1) {
                carrier = ba.getPlayerName(carrierID);
            } else {
                carrier = null;
            }

            if (carrier != null && !carrier.equals(ba.getBotName())) {
                if (!carried && state == PLAYING) {
                    carriers.push(carrier);
                }
                carried = true;
            } else if (carrier == null && carried) {
                carried = false;
            } else if (carrier != null && carrier.equals(ba.getBotName())) {
                holding = true;
            } else if (carrier == null && holding) {
                holding = false;
            }
        }

        /**
         * clears local data for puck
         */
        @SuppressWarnings("unused")
        public void clear() {
            carrier = null;
            try {
                carriers.clear();
            } catch (Exception e) {}
        }

        public byte getBallID() {
            return ballID;
        }

        public long getTimeStamp() {
            return timestamp;
        }

        @SuppressWarnings("unused")
        public short getBallX() {
            return ballX;
        }

        @SuppressWarnings("unused")
        public short getBallY() {
            return ballY;
        }

        @SuppressWarnings("unused")
        public boolean isCarried() {
            return carried;
        }
    }

}
