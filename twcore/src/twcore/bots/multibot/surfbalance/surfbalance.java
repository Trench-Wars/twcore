package twcore.bots.multibot.surfbalance;

import java.util.TimerTask;
import java.util.TreeMap;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.util.Tools.Ship;

/**
 * surfbalance.java Created: 12/2012
 * 
 * @author Fusha With stuff stolen from:
 * @author K A N E (Ian) and
 * @author qan
 * 
 *         This module is for doing a racing event in ?go surfbalance When a player completes part 1, they get assigned to part 2, etc. First player
 *         to get to the end wins.
 * 
 *         THE CODING IS EXTREMELY "GHETTO"(TM) BUT KEEP IN MIND THIS IS MY FIRST BOT OK <3
 */
public class surfbalance extends MultiModule {

    //Used variables
    private boolean isRunning = false;
    private boolean isStarted = false; //isRunning is enabled on GOGOGO, isStarted is enabled on !start
    private boolean arenaLock = false;
    private boolean backwards = false; //backwards game mode.
    private int position = 1;

    //List
    private TreeMap<String, PlayerInfo> m_players;
    private CheckTimer checkTimer;

    /**
     * What's run when module is loaded.
     */
    public void init() {
    }

    /**
     * This method requests the events used by this module.
     */
    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        eventRequester.request(this, EventRequester.PLAYER_LEFT);
        eventRequester.request(this, EventRequester.PLAYER_ENTERED);
        eventRequester.request(this, EventRequester.PLAYER_POSITION);
    }

    /**
     * Sets players who left as lagged out.
     */
    public void handleEvent(PlayerLeft event) {
        if (isRunning) {
            String p = m_botAction.getPlayerName(event.getPlayerID());
            if (p != null) {
                PlayerInfo player = m_players.get(p);
                if (player != null && player.isPlaying())
                    player.lagger();
            }
        }
    }

    /**
     * Sets players who specced as lagged out.
     */
    public void handleEvent(FrequencyShipChange event) {
        if (isRunning) {
            if (event.getShipType() == Ship.SPECTATOR) {
                String name = m_botAction.getPlayerName(event.getPlayerID());
                PlayerInfo player = m_players.get(name);
                if (player != null)
                    player.lagger();
            }
        }
    }

    /**
     * Sends prompt to !lagout or !enter to dudes who joined arena.
     */
    public void handleEvent(PlayerEntered event) {
        if (isRunning) {
            if (m_players.containsKey(event.getPlayerName())) {
                m_botAction.sendPrivateMessage(event.getPlayerName(), "Welcome Back! PM me with !lagout to get back in the game.");
            } else {
                m_botAction.sendPrivateMessage(event.getPlayerName(), "Welcome to Surfbalance! There is currently a race in progress. Please PM me with !enter to join!.");
            }
        }
    }

    /**
     * Handles player position and switches freqs.
     */
    public void handleEvent(PlayerPosition event) {
        boolean isSafe = false;
        int freq = -1;
        int xLoc = -1;
        int yLoc = -1;

        Player p;
        String playerName;

        isSafe = event.isInSafe();
        xLoc = event.getXLocation() / 16; //theses thing return coords in pixels. map is 16*1024 pixels.
        yLoc = event.getYLocation() / 16; //so divide by 16 to get regular coords!

        p = m_botAction.getPlayer(event.getPlayerID());

        if (p == null)
            return;

        freq = p.getFrequency();
        playerName = m_botAction.getPlayerName(p.getPlayerID());
        if (playerName == null)
            return;

        if (isRunning) {
            if (!backwards) {
                if (isSafe && freq == 0 && xLoc > 485 && xLoc < 520 & yLoc > 410 && yLoc < 448) {
                    markReachedSafe(playerName);
                } else if (isSafe && freq != 0 && xLoc > 485 && xLoc < 520 & yLoc > 410 && yLoc < 448) {
                    markNotReachedSafe(playerName);
                }
                if (isSafe && freq == 1 && xLoc > 453 && xLoc < 472 && yLoc > 535 && yLoc < 571) {
                    markReachedSafe(playerName);
                } else if (isSafe && freq != 1 && xLoc > 453 && xLoc < 472 && yLoc > 535 && yLoc < 571) {
                    markNotReachedSafe(playerName);
                }
                if (freq == 2 && xLoc > 528 && xLoc < 768 && yLoc > 655 && yLoc < 679) {
                    markReachedSafe(playerName);
                } else if (freq != 2 && xLoc > 528 && xLoc < 768 && yLoc > 655 && yLoc < 679) {
                    markNotReachedSafe(playerName);
                }
            } else {
                if (isSafe && freq == 3 && xLoc > 453 && xLoc < 472 && yLoc > 535 && yLoc < 571) {
                    markReachedSafe(playerName);
                } else if (isSafe && freq != 3 && xLoc > 453 && xLoc < 472 && yLoc > 535 && yLoc < 571) {
                    markNotReachedSafe(playerName);
                }
                if (isSafe && freq == 2 && xLoc > 485 && xLoc < 520 & yLoc > 410 && yLoc < 448) {
                    markReachedSafe(playerName);
                } else if (isSafe && freq != 2 && xLoc > 485 && xLoc < 520 & yLoc > 410 && yLoc < 448) {
                    markNotReachedSafe(playerName);
                }
                if (isSafe && freq == 1 && xLoc > 418 && xLoc < 433 && yLoc > 239 && yLoc < 247) {
                    markReachedSafe(playerName);
                } else if (isSafe && freq != 1 && xLoc > 418 && xLoc < 433 && yLoc > 239 && yLoc < 247) {
                    markNotReachedSafe(playerName);
                }
            }
        }
    }

    /**
     * Handles messages.
     */
    public void handleEvent(Message event) {
        int type = event.getMessageType();
        String message = event.getMessage();
        if (type == Message.PRIVATE_MESSAGE) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            handleCommand(name, message);
        }
    }

    /**
     * Recognizes commands.
     */
    private void handleCommand(String name, String command) {
        String cmd = command.toLowerCase();

        if (cmd.equals("!lagout")) {
            cmd_lagout(name);
        } else if (cmd.startsWith("!enter")) {
            cmd_enter(name);
        }

        if (opList.isER(name)) {
            if (cmd.equals("!start")) {
                cmd_start(name);
            } else if (cmd.equals("!stop")) {
                cmd_stop(name);
            } else if (cmd.equals("!backwards")) {
                cmd_backwards(name);
            }
        }
    }

    /**
     * Handles the !lagout command
     */
    private void cmd_lagout(String name) {
        if (isRunning) {
            PlayerInfo player = m_players.get(name);
            if (player != null) {
                if (player.isLagged()) {
                    returnedFromLagout(name);
                } else {
                    m_botAction.sendPrivateMessage(name, "You aren't lagged out!");
                }
            } else {
                m_botAction.sendPrivateMessage(name, "Your name was not found in the record. Please try !enter");
            }
        } else {
            m_botAction.sendPrivateMessage(name, "The game is not currently started.");
        }
    }

    /**
     * Handles the !enter command
     */
    private void cmd_enter(String name) {
        if (isRunning) {
            PlayerInfo p = m_players.get(name);
            if (p == null) {
                m_botAction.setShip(name, Ship.WARBIRD);
                m_botAction.setFreq(name, 0);
                m_players.put(name, new PlayerInfo(name, 0));
            } else
                m_botAction.sendPrivateMessage(name, "Please Use The !lagout Command!");
        } else
            m_botAction.sendPrivateMessage(name, "The game is not currently started.");
    }

    /**
     * Switches game mode.
     */
    private void cmd_backwards(String name) {
        if (!isStarted) {
            if (!backwards) {
                backwards = true;
                m_botAction.sendPrivateMessage(name, "Backwards mode enabled; players will race from finish to start.");
            } else {
                m_botAction.sendPrivateMessage(name, "Backwards mode disabled.");
                backwards = false;
            }
        } else
            m_botAction.sendPrivateMessage(name, "Can't switch game modes when game is started!");
    }

    /**
     * Handles !start command.
     */
    private void cmd_start(String name) {
        if (!isStarted) {
            startGame();
        } else {
            m_botAction.sendPrivateMessage(name, "The game has already begun.");
        }
    }

    /**
     * Handles !stop command.
     */
    private void cmd_stop(String name) {
        if (isStarted) {
            m_botAction.sendArenaMessage("Game has been stopped, arena is unlocked.", 3);
            cancel();
        } else
            m_botAction.sendPrivateMessage(name, "The game has not been started yet.");
    }

    /**
     * Marks a player as reached safe
     */
    private void markReachedSafe(String playerName) {
        PlayerInfo p = m_players.get(playerName);
        if (p != null)
            p.setReachedSafe();
    }

    /**
     * Cancels possible accidental marking as reached safe
     */
    private void markNotReachedSafe(String playerName) {
        PlayerInfo p = m_players.get(playerName);
        if (p != null)
            p.setNotReachedSafe();
    }

    /**
     * Changes freqs of those in list
     */
    private void changeFreq() {
        int freq = -1;
        for (Player p : m_botAction.getPlayingPlayers()) {
            if (p == null) {
                return;
            }
            if (!backwards) {
                freq = p.getFrequency() + 1;
            } else {
                freq = p.getFrequency() - 1;
            }
            String playerName = p.getPlayerName();
            if (playerName != null) {
                PlayerInfo player = m_players.get(playerName);
                if (player != null && player.isReachedSafe) {
                    m_botAction.setFreq(playerName, freq);
                    m_players.put(playerName, new PlayerInfo(playerName, freq));
                    player.setNotReachedSafe();
                    announceChange(playerName, freq);
                }
            }
        }
    }

    /**
     * Tells everyone about a dude reaching another part
     */
    private void announceChange(String playerName, int freq) {
        String time;
        if (!isRunning)
            return; //NullPointerException error otherwise
        time = checkTimer.getPlayTimeString();
        if (freq == 1) {
            if (!backwards)
                m_botAction.sendArenaMessage(playerName + " has reached part 2! Time: " + time, 20);
            else
                m_botAction.sendArenaMessage(playerName + " has reached part 3! Time: " + time, 21);
        }
        if (freq == 2) {
            if (!backwards)
                m_botAction.sendArenaMessage(playerName + " has reached part 3! Time: " + time, 21);
            else
                m_botAction.sendArenaMessage(playerName + " has reached part 2! Time: " + time, 20);
        }
        if (freq == 3 || freq == 0)
            doScore(playerName, time);
    }

    /**
     * Starts game.
     */
    private void startGame() {
        arenaLock = true;
        m_botAction.toggleLocked();
        m_botAction.sendArenaMessage("Arena is locked. The race will begin soon!", 1);
        isStarted = true;
        startCheckTimer();
    }

    /**
     * Creates a record for each of the players in the game at the start
     * 
     */
    private void createPlayerRecords() {
        m_players = new TreeMap<String, PlayerInfo>(String.CASE_INSENSITIVE_ORDER);

        for (Player p : m_botAction.getPlayingPlayers())
            m_players.put(p.getPlayerName(), new PlayerInfo(p.getPlayerName(), p.getFrequency()));
    }

    /**
     * A method that puts a returning player from lagout into the ship and freq that they left the game as.
     */
    private void returnedFromLagout(String name) {
        PlayerInfo p = m_players.get(name);
        m_botAction.setShip(name, Ship.WARBIRD);
        m_botAction.setFreq(name, p.getFreq());
        m_botAction.sendPrivateMessage(name, "Welcome back!");
        p.isNotLagged();
    }

    /**
     * Starts the timer for the heartbeat.
     */
    private void startCheckTimer() {
        if (checkTimer != null)
            m_botAction.cancelTask(checkTimer);

        checkTimer = new CheckTimer();
        m_botAction.scheduleTask(checkTimer, Tools.TimeInMillis.SECOND, Tools.TimeInMillis.SECOND);
    }

    /**
     * Arena messages for the winners.
     */
    private void doScore(String playerName, String time) {
        if (position == 1) {
            m_botAction.sendArenaMessage(playerName + " has won!! They are the champion of surfing and balance! Time: " + time, 30);
            if (backwards) {
                m_botAction.sendArenaMessage("This person here managed to get FIRST PLACE even though we were going IN THE WRONG DIRECTION!!!!!!! Can you believe it? I sure can't! :O");
            }
            position++;
        } else if (position == 2) {
            m_botAction.sendArenaMessage(playerName + " has finished in second place! Which is just a bit less awesome than first!!! Time: " + time, 5);
            position++;
        } else if (position == 3) {
            m_botAction.sendArenaMessage(playerName + " got third place... Which is still better than most, so good job! <3 Time: " + time, 32);
            position++;
        } else if (position > 3) {
            m_botAction.sendArenaMessage("Good job, " + playerName + "! You finished in " + position + " place with a time of " + time, 18);
            position++;
        }

    }

    /**
     * Cleanup.
     */
    public void cancel() {
        isRunning = false;
        isStarted = false;
        arenaLock = false;
        m_botAction.cancelTasks();
        m_botAction.toggleLocked();
        position = 1;
        clearRecords();
    }

    /**
     * Clears all player records, and cancels all timer tasks.
     */
    private void clearRecords() {
        m_botAction.cancelTask(checkTimer);

        if (m_players == null)
            return;
        if (m_players.values() == null)
            return;
        m_players.clear();
    }

    /**
     * Checks the records and makes any adjustments if needed
     * 
     */
    private class CheckTimer extends TimerTask {
        int timerInSeconds;

        //class constructor for checkTimer
        public CheckTimer() {
            timerInSeconds = 0;
        }

        //What is ran every second
        public void run() {
            timerInSeconds++;

            if (timerInSeconds == 5) {
                m_botAction.sendArenaMessage("RULES: Surf on the wormholes and get to the end!");
                m_botAction.sendArenaMessage("When you complete a part of the course, stay in the safe area for a bit so the bot can save and track your progress!", 2);
                if (backwards)
                    m_botAction.sendArenaMessage("BACKWARDS MODE ENABLED: you'll be racing from finish to start! Good luck! :)");
            } else if (timerInSeconds == 30)
                doWarning();
            else if (timerInSeconds == 35 && backwards)
                m_botAction.sendArenaMessage("LOL DON'T GO YET YOU'RE STILL GONNA BE WARPED DUDES", 14);
            else if (timerInSeconds == 40)
                doGo();
            else if (timerInSeconds % 120 == 0)
                m_botAction.sendTeamMessage("PM me with !enter to join the game or !lagout to return!");
            if (timerInSeconds % 2 == 0 && timerInSeconds > 40)
                changeFreq();
        }

        //Sets up the racers
        private void doWarning() {
            m_botAction.sendArenaMessage("Race begins in about 10 seconds. Get ready!", 27);
            m_botAction.changeAllShips(Ship.WARBIRD);
            if (!backwards) {
                m_botAction.setDoors(255);
                m_botAction.setAlltoFreq(0);
                m_botAction.warpAllRandomly();
            } else
                m_botAction.setAlltoFreq(3);
        }

        //GO GO GO
        private void doGo() {

            m_botAction.scoreResetAll();
            m_botAction.shipResetAll();
            if (!backwards) {
                m_botAction.warpAllRandomly();
            }
            m_botAction.setDoors(0);
            isRunning = true;
            createPlayerRecords();
            m_botAction.sendArenaMessage("GO GO GO !!!", 104);
        }

        public int getTime() {
            return timerInSeconds;
        }

        //nifty thing that turns seconds into a neat time format (stolen from qan's dangerous)
        public String getPlayTimeString() {
            int timerInSeconds = checkTimer.getTime() - 40;

            if (timerInSeconds <= 0) {
                return "0:00";
            } else {
                int minutes = timerInSeconds / 60;
                int seconds = timerInSeconds % 60;
                return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
            }
        }

    }

    /**
     * Creates a temporary record for each player to track basic information.
     */
    private class PlayerInfo {
        private String name;
        private int freq;
        private boolean isPlaying = true;
        private boolean laggedOut = false;
        private boolean isReachedSafe = false;

        // Default values added to a record when the record is created.
        public PlayerInfo(String name, int freq) {
            this.name = name;
            this.freq = freq;
        }

        // Returns a players frequency
        public int getFreq() {
            return freq;
        }

        // Sets player's lag status to laggedOut
        public void lagger() {
            laggedOut = true;
            m_botAction.sendPrivateMessage(name, "PM me with !lagout to get back in the game.");
        }

        // boolean check if the player is lagged out.
        public boolean isLagged() {
            return laggedOut;
        }

        public void isNotLagged() {
            laggedOut = false;
        }

        // boolean check if the player is playing.
        public boolean isPlaying() {
            return isPlaying;
        }

        //marks a player as reached safe
        public void setReachedSafe() {
            isReachedSafe = true;
        }

        //marks at not reached safe
        public void setNotReachedSafe() {
            isReachedSafe = false;
        }
    }

    /**
     * Your typical manual that comes with every robot to make our everyday life a little easier.
     */
    public String[] getModHelpMessage() {

        String[] JBHelp = { "!start           -- Locks the arena and starts the game!", "!backwards       -- Toggles racing from finish to start.",
                "!stop            -- Cancels the game.                   "

        };
        return JBHelp;
    }

    /**
     * Checkout.
     */
    public boolean isUnloadable() {
        clearRecords();
        if (arenaLock) {
            m_botAction.toggleLocked();
        }
        isRunning = false;
        m_botAction.cancelTasks();
        return true;
    }
}