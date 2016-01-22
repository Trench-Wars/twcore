package twcore.bots.multibot.jailbreak;

import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.SoccerGoal;
import twcore.core.events.BallPosition;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.util.Tools.Ship;

/**
    jailbreak.java Created: 12/2012

    @author Fusha
    @author K A N E (Ian)
*/
public class jailbreak extends MultiModule {

    // Toggles for game status and balancing teams.
    private boolean isRunning = false;
    private boolean isStarted = false;
    private boolean teamToggle = false;
    private boolean arenaLock = false;
    private boolean freq0Score = false;
    private boolean freq1Score = false;
    private boolean checkTimeStamp0 = false;
    private boolean checkTimeStamp1 = false;
    private int timeStamp0 = 0;
    private int timeStamp1 = 0;
    private int lastBallTimestamp0 = 0;
    private int lastBallTimestamp1 = 0;
    private TimerTask pause;

    // Declares a hashMap for the player info.
    private TreeMap<String, PlayerInfo> m_players;
    private CheckTimer checkTimer;
    private TreeSet<String> freq0Safe = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private TreeSet<String> freq1Safe = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    public void init() {
        m_botAction.setReliableKills(1);
    }

    /**
        This method requests the events used by this module.
    */
    public void requestEvents(ModuleEventRequester eventRequester) {

        eventRequester.request(this, EventRequester.PLAYER_DEATH);
        eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        eventRequester.request(this, EventRequester.PLAYER_LEFT);
        eventRequester.request(this, EventRequester.PLAYER_ENTERED);
        eventRequester.request(this, EventRequester.SOCCER_GOAL);
        eventRequester.request(this, EventRequester.PLAYER_POSITION);
        eventRequester.request(this, EventRequester.BALL_POSITION);
    }

    /**
        Counts arena leaves as DCs to be safe.

        @param event
                  Contains event information on player.
    */
    public void handleEvent(PlayerLeft event) {
        if (isRunning) {
            String p = m_botAction.getPlayerName(event.getPlayerID());

            if (p != null) {
                PlayerInfo player = m_players.get(p);

                if (player != null && player.isPlaying()) {
                    player.lagger();
                    checkWinner();

                    if (freq0Safe.size() > 0) {
                        if (freq0Safe.contains(p))
                            freq0Safe.remove(p);
                    } else if (freq1Safe.size() > 0) {
                        if (freq1Safe.contains(p))
                            freq1Safe.remove(p);
                    }
                }
            }
        }
    }

    /**
        Handles the soccer goal event.

        @param event
                  Contains information on a freq change.
    */
    public void handleEvent(SoccerGoal event) {
        if (isRunning) {
            if (event.getFrequency() == 0) {
                freq0Score = true;
                m_botAction.sendArenaMessage("Freq 0 have scored! Their jail is broken!");
            } else if (event.getFrequency() == 1) {
                freq1Score = true;
                m_botAction.sendArenaMessage("Freq 1 have scored! Their jail is broken!");
            }
        }
    }

    /**
        Using the frequencyChange event to look out for possible lagouts to the spec frequency.

        @param event
                  Contains information on a freq change.
    */
    public void handleEvent(FrequencyShipChange event) {
        if (isRunning) {
            if (event.getShipType() == Ship.SPECTATOR) {
                String name = m_botAction.getPlayerName(event.getPlayerID());
                PlayerInfo player = m_players.get(name);

                if (player != null) {
                    player.lagger();
                    checkWinner();

                    if (freq0Safe.size() > 0) {
                        if (freq0Safe.contains(name))
                            freq0Safe.remove(name);
                    } else if (freq1Safe.size() > 1) {
                        if (freq1Safe.contains(name))
                            freq1Safe.remove(name);
                    }
                }
            }
        }
    }

    /**
        Everyone that joins the arena while the game is in progress will receive a message to !lagout or !enter.

        @param event
                  - contains the variable information for the event.
    */
    public void handleEvent(PlayerEntered event) {
        if (isRunning) {
            if (m_players.containsKey(event.getPlayerName())) {
                m_botAction.sendPrivateMessage(event.getPlayerName(), "Welcome Back! PM me with !lagout to get back in the game.");
            } else {
                m_botAction.sendPrivateMessage(event.getPlayerName(), "Welcome to Jailbreak! There is currently a game in progress. Please PM me with !enter to join!.");
            }
        }
    }

    /**
        Handles ball timestamp (for dropping them at go)
    */

    public void handleEvent(BallPosition event) {
        int id = event.getBallID();

        if (id == 0) {
            lastBallTimestamp0 = event.getTimeStamp();
        } else if (id == 1) {
            lastBallTimestamp1 = event.getTimeStamp();
        } else {
            return;
        }
    }

    /**
        Checks if a player is in safe and warps them out if their freq has scored, or adds/removes them to/from the warping waiting list.

    */
    public void handleEvent(PlayerPosition event) {
        boolean isSafe = false;
        int freq = -1;
        Player p;
        String playerName;

        isSafe = event.isInSafe();
        p = m_botAction.getPlayer(event.getPlayerID());

        if (p == null)
            return;

        freq = p.getFrequency();
        playerName = m_botAction.getPlayerName(p.getPlayerID());

        if (playerName == null)
            return;

        if (isRunning) {
            if (isSafe) {
                if (freq == 0)
                    freq0Safe.add(playerName);
                else if (freq == 1)
                    freq1Safe.add(playerName);
            } else {
                if (freq == 0 && freq0Safe.size() > 0) {
                    if (freq0Safe.contains(playerName))
                        freq0Safe.remove(playerName);
                } else if (freq == 1 && freq1Safe.size() > 0) {
                    if (freq1Safe.contains(playerName))
                        freq1Safe.remove(playerName);
                }
            }
        }
    }

    /**
        This handleEvent accepts msgs from players as well as mods.

        @param event The Message event in question.
    */
    public void handleEvent(Message event) {
        int type = event.getMessageType();
        String message = event.getMessage();

        if (type == Message.PRIVATE_MESSAGE) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            handleCommand(name, message);
        } else if (type == Message.ARENA_MESSAGE) {
            if (message.equals("Arena UNLOCKED") && arenaLock)
                m_botAction.toggleLocked();
            else if (message.equals("Arena LOCKED") && !arenaLock)
                m_botAction.toggleLocked();
        }
    }

    /**
        Handles the Handevent app.

        @param name
        @param command
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
            }
        }
    }

    /**
        Handles the !lagout command

        @param name
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
        Handles the !enter command

        @param name
    */
    private void cmd_enter(String name) {
        if (isRunning) {
            PlayerInfo p = m_players.get(name);

            if (p == null) {
                if (teamToggle) {
                    m_botAction.setShip(name, Ship.WARBIRD);
                    m_botAction.setFreq(name, 1);
                    m_players.put(name, new PlayerInfo(name, 1));
                    freq1Safe.add(name);
                    teamToggle = false;
                } else {
                    m_botAction.setShip(name, Ship.WARBIRD);
                    m_botAction.setFreq(name, 0);
                    m_players.put(name, new PlayerInfo(name, 0));
                    freq0Safe.add(name);
                    teamToggle = true;
                }

            } else {
                m_botAction.sendPrivateMessage(name, "Please Use The !lagout Command!");
            }
        } else {
            m_botAction.sendPrivateMessage(name, "The Game is not currently started.");
        }
    }

    /**
        Start the current game

        @param name
    */
    private void cmd_start(String name) {
        if (!isStarted) {
            startGame();
        } else {
            m_botAction.sendPrivateMessage(name, "The game has already begun.");
        }
    }

    /**
        Stop the current game

        @param name
    */
    private void cmd_stop(String name) {
        if (isStarted) {
            cancel();
            m_botAction.sendArenaMessage("Game has been stopped, arena is unlocked.", 3);
        } else
            m_botAction.sendPrivateMessage(name, "The game has not been started yet.");
    }

    /**
        A method that puts a returning player from lagout into the ship and freq that they left the game as

        @param name
                  name of the player is needed.
    */
    private void returnedFromLagout(String name) {
        PlayerInfo p = m_players.get(name);
        m_botAction.setShip(name, Ship.WARBIRD);
        m_botAction.setFreq(name, p.getFreq());

        if (p.getFreq() == 0) {
            freq0Safe.add(name);
        } else if (p.getFreq() == 1) {
            freq1Safe.add(name);
        }

        m_botAction.sendPrivateMessage(name, "Welcome back!");
        p.isNotLagged();
    }

    /**
        Starts the timer for the heartbeat

    */
    private void startCheckTimer() {
        if (checkTimer != null)
            m_botAction.cancelTask(checkTimer);

        checkTimer = new CheckTimer();
        m_botAction.scheduleTask(checkTimer, Tools.TimeInMillis.SECOND, Tools.TimeInMillis.SECOND);
    }

    /**
        Creates a record for each of the players in the game at the start

    */
    private void createPlayerRecords() {
        m_players = new TreeMap<String, PlayerInfo>(String.CASE_INSENSITIVE_ORDER);

        for (Player p : m_botAction.getPlayingPlayers())
            m_players.put(p.getPlayerName(), new PlayerInfo(p.getPlayerName(), p.getFrequency()));
    }

    /**
        Clears all player records, and cancels all timer tasks.

    */
    private void clearRecords() {
        m_botAction.cancelTask(checkTimer);

        freq0Safe.clear();
        freq1Safe.clear();

        if (m_players == null)
            return;

        if (m_players.values() == null)
            return;

        m_players.clear();
    }

    /**
        Initializes game.

        @param timeLimit
    */
    private void startGame() {
        arenaLock = true;
        isStarted = true;
        m_botAction.toggleLocked();
        m_botAction.sendArenaMessage("Jailbreak started.");
        startCheckTimer();

        if (m_botAction.getNumPlayers() < 2) {
            m_botAction.sendArenaMessage("Game does not have enough people to start. Need at least one player per team.");
            cancel();
        }
    }

    /**
        This method checks to see if there is a winner
    */
    private void checkWinner() {
        if (isRunning) {
            if (m_botAction.getFrequencySize(0) == freq0Safe.size() && !freq0Score) {
                m_botAction.sendArenaMessage("Freq 0 has been completely jailed! Freq 1 are the winners!!!", 5);
                m_botAction.sendArenaMessage("Thank you for playing! Arena UNLOCKED");
                cancel();
            } else if (m_botAction.getFrequencySize(1) == freq1Safe.size() && !freq1Score) {
                m_botAction.sendArenaMessage("Freq 1 has been completely jailed! Freq 0 are the winners!!!", 5);
                m_botAction.sendArenaMessage("Thank you for playing! Arena UNLOCKED");
                cancel();
            }
        }
    }

    /**
        Checks the records and makes any adjustments if needed

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

            if (timerInSeconds == 5)
                doRules();
            else if (timerInSeconds == 30)
                doWarning();
            else if (timerInSeconds == 40)
                doGo();
            else if (timerInSeconds % 30 == 0)
                m_botAction.sendTeamMessage("Please PM me with !enter to join the game or !lagout to return.");

            if (freq0Score)
                doFreqOneScore();

            if (freq1Score)
                doFreqTwoScore();

            if (timerInSeconds > 80)
                checkWinner();
        }

        /**
            The rules that are displayed at the beginning of each game.
        */
        private void doRules() {
            m_botAction.sendArenaMessage("------------------------------ Jailbreak RULES ------------------------------");
            m_botAction.sendArenaMessage("| Jailbreak is a two teams game. Bring your team to victory by killing and  |");
            m_botAction.sendArenaMessage("| scoring goals! If you die, you get locked into your team's jail. The only |");
            m_botAction.sendArenaMessage("| way to get out is to have someone on your team score a goal! The team     |");
            m_botAction.sendArenaMessage("| that jails the entire opposing team wins. Good luck!                      |");
            m_botAction.sendArenaMessage("-----------------------------------------------------------------------------", 4);
        }

        /**
            The 10 second warning before go.
        */
        private void doWarning() {
            m_botAction.sendArenaMessage("Jailbreak is about to begin in about 10 seconds!", 1);
            m_botAction.createNumberOfTeams(2);
        }

        /**
            The go and start commands.

        */
        private void doGo() {
            m_botAction.changeAllShips(Ship.WARBIRD);
            m_botAction.scoreResetAll();
            m_botAction.shipResetAll();
            m_botAction.setDoors(255);
            m_botAction.warpFreqToLocation(0, 270, 426);
            m_botAction.warpFreqToLocation(1, 729, 595);
            freq0Safe.clear();
            freq1Safe.clear();
            isRunning = true;
            createPlayerRecords();
            doBallDrop();
            m_botAction.sendArenaMessage("GO GO GO !!!", 104);
        }

        /**
            Drops both balls right in the middle (All ball-related stuff stolen from Joyrider's balldrop util)
        */

        private void doBallDrop() {
            m_botAction.getShip().setShip(0);
            m_botAction.getShip().move(7984, 8176);
            m_botAction.getBall((byte) 0, lastBallTimestamp0);
            m_botAction.getShip().setShip(8);

            pause = new TimerTask() {
                @Override
                public void run() {
                    m_botAction.getShip().setShip(0);
                    m_botAction.getShip().move(8000, 8208);
                    m_botAction.getBall((byte) 1, lastBallTimestamp1);
                    m_botAction.getShip().setShip(8);
                }
            };

            m_botAction.scheduleTask(pause, 1000);
        }

        /**
            Starts and stops the timers after a goal is scored and warps the players.

        */

        private void doFreqOneScore() {
            for (Player p : m_botAction.getPlayingPlayers()) {
                if (freq0Safe.size() > 0) {
                    if (p.getFrequency() == 0 && freq0Safe.contains(p.getPlayerName())) {
                        m_botAction.warpTo(p.getPlayerName(), 238, 510, 1);
                        freq0Safe.remove(p.getPlayerName());
                    }
                }
            }

            if (!checkTimeStamp0) {
                timeStamp0 = timerInSeconds + 15;
                checkTimeStamp0 = true;
            }

            if (timerInSeconds == timeStamp0) {
                freq0Score = false;
                m_botAction.sendArenaMessage("Freq 0's safe is now locked!", 1);
                checkTimeStamp0 = false;
            }
        }

        private void doFreqTwoScore() {
            for (Player p : m_botAction.getPlayingPlayers()) {
                if (freq1Safe.size() > 0) {
                    if (p.getFrequency() == 1 && freq1Safe.contains(p.getPlayerName())) {
                        m_botAction.warpTo(p.getPlayerName(), 761, 510, 1);
                        freq1Safe.remove(p.getPlayerName());
                    }
                }
            }

            if (!checkTimeStamp1) {
                timeStamp1 = timerInSeconds + 15;
                checkTimeStamp1 = true;
            }

            if (timerInSeconds == timeStamp1) {
                freq1Score = false;
                m_botAction.sendArenaMessage("Freq 1's safe is now locked!", 1);
                checkTimeStamp1 = false;
            }
        }
    }

    /**
        Creates a temporary record for each player to track basic information such as name, shiptype, lagged out or not, scores, kills, saves, and tks.
        information is cleared after each game is completed.
    */
    private class PlayerInfo {

        private String name;
        private int freq;
        private boolean isPlaying = true;
        private boolean laggedOut = false;

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
    }

    /**
        Cleanup.
    */
    public void cancel() {
        isRunning = false;
        isStarted = false;
        arenaLock = false;
        m_botAction.cancelTasks();
        m_botAction.toggleLocked();
        clearRecords();
    }

    /**
        Your typical manual that comes with every robot to make our everyday life a little easier.
    */
    public String[] getModHelpMessage() {

        String[] JBHelp = { "!start         -- Starts the exciting game of JAILBREAK!", "!stop          -- Cancels the game.                     "

                          };
        return JBHelp;
    }

    /**
        Checkout.
    */
    public boolean isUnloadable() {
        clearRecords();
        isRunning = false;
        m_botAction.cancelTasks();
        return true;
    }
}
