/**
 * twbotfreezetag.java
 *
 * Created: 07/05/2004
 *
 * Last Updated: 10/3/2012 by Ian (K A N E)
 * - Revised the whole module to make it possible for player information to be stored during the game.
 * - Fixed the !enter function to only work if they are not previously in the game
 * - Added a !lagout function that returns players to the ship and freq they left the game at.
 * - Added scoring that is displayed at the end of the game. The top 10 is currently what it is set to.
 * 
 * Side Note: The format and structure of this module was based off Qan's Dangerous Game module on multibot. 
 *            Thank you Qan.
 */

package twcore.bots.multibot.freezejavs;

import java.util.TimerTask;
import java.util.TreeMap;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

/**
 * This class is sort of a variant of the zombies module. It is to be used when hosting a game of ?go freezejavs.
 * 
 * @author Jason; Freezetag
 * @author Derek; Freezejavs
 * @author Ian; Completely revised previous module.
 */
public class freezejavs extends MultiModule {

    // Toggles for game status and balancing teams.
    private boolean isRunning = false;
    private boolean teamToggle = false;
    private boolean isTimer = false;
    private boolean arenaLock = false;

    // Declares a hashMap for the player info.
    private TreeMap<String, PlayerInfo> m_players;

    private CheckTimer checkTimer;

    // Decleration of int variables.
    private static int WARBIRD = 1;
    private static int JAVELIN = 2;
    private static int SPIDER = 3;
    private static int LEV = 4;

    // Default frequencies that are used.
    private static int TEAM1_FREQ = 0;
    private static int TEAM2_FREQ = 1;

    // Warpcodes for both freqs.
    private static int TEAM1_WARPX = 363;
    private static int TEAM1_WARPY = 464;
    private static int TEAM2_WARPX = 626;
    private static int TEAM2_WARPY = 458;

    // Default point values for each action.
    private static int m_kills = 1;
    private static int m_saves = 1;
    private static int m_tk = 1;
    private int timeLimit = 0;

    public void init() {
        m_botAction.setReliableKills(1);
    }

    /**
     * Cleanup.
     */
    public void cancel() {
        isRunning = false;
        arenaLock = false;
        m_botAction.cancelTasks();
        m_botAction.toggleLocked();
        clearRecords();
    }

    /**
     * This method requests the events used by this module.
     */
    public void requestEvents(ModuleEventRequester eventRequester) {

        eventRequester.request(this, EventRequester.PLAYER_ENTERED);
        eventRequester.request(this, EventRequester.PLAYER_DEATH);
        eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        eventRequester.request(this, EventRequester.PLAYER_LEFT);
        eventRequester.request(this, EventRequester.PLAYER_ENTERED);

    }

    /**
     * Adds and subtracts time when players die.
     * 
     * @param event
     *            Contains event information on player who died.
     */
    public void handleEvent(PlayerDeath event) {
        if (isRunning) {
            Player killed = m_botAction.getPlayer(event.getKilleeID());
            Player killer = m_botAction.getPlayer(event.getKillerID());

            if (killed != null) {
                PlayerInfo player = m_players.get(killer.getPlayerName());
                if (player != null) {

                    if (killer.getFrequency() == killed.getFrequency()) {
                        player.hadTK();
                        m_botAction.shipReset(event.getKilleeID());
                        m_botAction.warpRandomly(event.getKilleeID());
                        m_botAction.specificPrize(event.getKilleeID(), Tools.Prize.ENERGY_DEPLETED);
                    } else {
                        if (killed.getShipType() != WARBIRD && killed.getShipType() != JAVELIN) {
                            player.hadSave();
                        } else {
                            player.hadKill();
                        }

                        if (killed.getShipType() == WARBIRD) {
                            m_botAction.setShip(killed.getPlayerName(), SPIDER);
                            m_botAction.setFreq(killed.getPlayerName(), TEAM2_FREQ);
                            m_botAction.specificPrize(killed.getPlayerName(), Tools.Prize.ENERGY_DEPLETED);
                        } else if (killed.getShipType() == JAVELIN) {
                            m_botAction.setShip(killed.getPlayerName(), LEV);
                            m_botAction.setFreq(killed.getPlayerName(), TEAM1_FREQ);
                            m_botAction.specificPrize(killed.getPlayerName(), Tools.Prize.ENERGY_DEPLETED);
                        } else if (killed.getShipType() == SPIDER) {
                            m_botAction.setShip(killed.getPlayerName(), WARBIRD);
                            m_botAction.setFreq(killed.getPlayerName(), TEAM1_FREQ);
                        } else if (killed.getShipType() == LEV) {
                            m_botAction.setShip(killed.getPlayerName(), JAVELIN);
                            m_botAction.setFreq(killed.getPlayerName(), TEAM2_FREQ);
                        }
                    }
                }
            }
        }
    }

    /**
     * Counts arena leaves as DCs to be safe.
     * 
     * @param event
     *            Contains event information on player.
     */
    public void handleEvent(PlayerLeft event) {
        if (isRunning) {
            Player p = m_botAction.getPlayer(event.getPlayerID());

            if (p != null) {
                PlayerInfo player = m_players.get(p.getPlayerName());
                if (player != null) {
                    if (player.isPlaying()) {
                        player.lagger();
                    }
                }
            }
        }
    }

    /**
     * Using the frequencyChange event to look out for possible lagouts to the spec frequency.
     * 
     * @param event
     *            Contains information on a freq change.
     */
    public void handleEvent(FrequencyShipChange event) {
        if (isRunning) {
            int checkFreq = event.getFrequency();
            if (checkFreq == 9999) {
                Player p = m_botAction.getPlayer(event.getPlayerID());
                PlayerInfo player = m_players.get(p.getPlayerName());
                if (p != null && player != null) {
                    player.lagger();
                }
            }
        }
    }

    /**
     * Everyone that joins the arena while the game is in progress will recieve a message to !lagout or !enter.
     * 
     * @param event
     *            - contains the valriable information for the event.
     */
    public void handleEvent(PlayerEntered event) {
        if (isRunning) {
            if (m_players.containsKey(event.getPlayerName())) {
                m_botAction.sendPrivateMessage(event.getPlayerName(), "Welcome Back! PM me with !lagout to get back in the game.");
            } else {
                m_botAction.sendPrivateMessage(event.getPlayerName(), "Welcome to FreezeJavs! There is currently a game in progress. Please PM me with !enter to join!.");
            }
        }
    }

    /**
     * This handleEvent accepts msgs from players as well as mods.
     * 
     * @event The Message event in question.
     */
    public void handleEvent(Message event) {

        String message = event.getMessage();
        if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            handleCommand(name, message);
        } else if (event.getMessageType() == Message.ARENA_MESSAGE) {
            if (message.equals("Arena UNLOCKED") && arenaLock)
                m_botAction.toggleLocked();
            else if (message.equals("Arena LOCKED") && !arenaLock)
                m_botAction.toggleLocked();
        }
    }

    private void handleCommand(String name, String command) {
        String cmd = command.toLowerCase();

        if (cmd.equals("!leave")) {
            cmd_leave(name);
        } else if (cmd.equals("!lagout")) {
            cmd_lagout(name);
        } else if (cmd.startsWith("!enter")) {
            cmd_enter(name);
        }

        if (opList.isER(name)) {
            if (cmd.equals("!start")) {
                cmd_start(name);
            } else if (cmd.startsWith("!start ")) {
                cmd_startTimer(name, cmd);
            } else if (cmd.equals("!stop")) {
                cmd_stop(name);
            }
        }

    }

    private void cmd_leave(String name) {
        m_botAction.specWithoutLock(name);
        if (isRunning) {
            PlayerInfo player = m_players.get(name);
            if (player.isPlaying()) {
                player.lagger();
            }
        }
    }

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
            m_botAction.sendPrivateMessage(name, "The Game is not currently started.");
        }
    }

    private void cmd_enter(String name) {
        if (isRunning) {
            PlayerInfo p = m_players.get(name);
            if (p == null) {

                if (teamToggle) {
                    m_botAction.setShip(name, WARBIRD);
                    m_botAction.setFreq(name, TEAM1_FREQ);
                    PlayerInfo player = new PlayerInfo(name, WARBIRD, 0, 0, 0);
                    m_players.put(name, player);
                    teamToggle = false;
                } else {
                    m_botAction.setShip(name, JAVELIN);
                    m_botAction.setFreq(name, TEAM2_FREQ);
                    PlayerInfo player = new PlayerInfo(name, JAVELIN, 0, 0, 0);
                    m_players.put(name, player);
                    teamToggle = true;
                }

            } else {
                m_botAction.sendPrivateMessage(name, "Please Use The !lagout Command!");
            }
        } else {
            m_botAction.sendPrivateMessage(name, "The Game is not currently started.");
        }
    }

    private void cmd_start(String name) {
        if (!isRunning) {
            startGame();
        } else {
            m_botAction.sendPrivateMessage(name, "The game has already begun.");
        }
    }

    private void cmd_startTimer(String name, String cmd) {
        if (!isRunning) {
            String[] parameters = Tools.stringChopper(cmd.substring(7), ' ');
            try {
                timeLimit = Integer.parseInt(parameters[0]) * 60;
                startGame();
            } catch (Exception e) {
                m_botAction.sendPrivateMessage(name, "Error in formatting your command.  Please try again.");
            }
        }
    }

    private void cmd_stop(String name) {
        if (isRunning) {
            m_botAction.sendPrivateMessage(name, "The FreezeTag Game Has Ended");
            clearRecords();
            isRunning = false;
        } else {
            m_botAction.sendPrivateMessage(name, "FreezeTag has not been Enabled");
        }
    }

    /**
     * A method that puts a returning player from lagout into the ship and spec that they left the game as
     * 
     * @param name
     *            name of the player is needed.
     */
    private void returnedFromLagout(String name) {
        Player p = m_botAction.getPlayer(name);
        if (p != null) {
            PlayerInfo pInfo = m_players.get(p.getPlayerName());
            m_botAction.setShip(name, pInfo.shipType);

            if (pInfo.shipType == WARBIRD || pInfo.shipType == LEV)
                m_botAction.setFreq(name, TEAM1_FREQ);
            if (pInfo.shipType == JAVELIN || pInfo.shipType == SPIDER)
                m_botAction.setFreq(name, TEAM2_FREQ);

            m_botAction.sendPrivateMessage(name, "Welcome back!");
            pInfo.isNotLagged();
        } else {
            m_botAction.sendPrivateMessage(name, "Error!  Please ask the host to put you back in manually.");
        }
    }

    private void startCheckTimer() {
        try {
            checkTimer.cancel();
        } catch (Exception e) {}

        checkTimer = new CheckTimer();
        m_botAction.scheduleTask(checkTimer, Tools.TimeInMillis.SECOND, Tools.TimeInMillis.SECOND);
    }

    /**
     * Creates a record for each of the players in the game at the start
     * 
     */
    private void createPlayerRecords() {
        m_players = new TreeMap<String, PlayerInfo>(String.CASE_INSENSITIVE_ORDER);

        for (Player p : m_botAction.getPlayingPlayers()) {
            PlayerInfo player = new PlayerInfo(p.getPlayerName(), p.getShipType(), 0, 0, 0);
            m_players.put(p.getPlayerName(), player);
        }
    }

    /**
     * Clears all player records, and cancels all timer tasks.
     * 
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
     * Initializes game with a time limit if set. The default time limit is 0.
     * 
     * @param timeLimit
     */
    private void startGame() {
        arenaLock = true;
        m_botAction.toggleLocked();
        m_botAction.sendArenaMessage("Freeze Tag mode started.");
        startCheckTimer();
        if (m_botAction.getNumPlayers() < 2) {
            m_botAction.sendArenaMessage("Game does not have enough people to start. Need at least one player per team.");
            cancel();
        }
    }

    /**
     * This method checks is called upon after an event happens that may possibly end the game such as a lagout or death.
     */
    private void checkWinner() {

        int warbirdLeft = 0;
        int javsLeft = 0;

        for (Player p : m_botAction.getPlayingPlayers()) {
            if (p.getShipType() == WARBIRD && p.getFrequency() == TEAM1_FREQ)
                warbirdLeft++;
            else if (p.getShipType() == JAVELIN && p.getFrequency() == TEAM2_FREQ)
                javsLeft++;
        }

        if (!isTimer) {
            if (warbirdLeft == 0)
                doStats(JAVELIN);
            if (javsLeft == 0)
                doStats(WARBIRD);
        } else {
            if (warbirdLeft > javsLeft)
                doStats(WARBIRD);
            if (javsLeft > warbirdLeft)
                doStats(JAVELIN);
            if (javsLeft == warbirdLeft)
                doStats(WARBIRD);
            isTimer = false;
        }

    }

    /**
     * This displays the stats of the game and then clears the record afterwards.
     * 
     * @param winner
     *            Will display the winners at the bottom of the score box.
     */
    private void doStats(int winner) {

        m_botAction.sendArenaMessage("Kill = 1 pt /    Save = 1 pt /    Team Kills = -1 pt   ");
        m_botAction.sendArenaMessage("----------------------------------------------------- TOP PLAYERS");
        m_botAction.sendArenaMessage("   #       Kills        Saves        Score                   ");

        for (int i = 1; i < 11; i++) { // Displays the top 10 players that were in the game                                       

            String name = getTopPlayer();
            PlayerInfo p = m_players.get(name);
            if (p != null) {
                String sKills = "" + p.getKills();
                String sSave = "" + p.getSaves();
                String sScore = "" + p.getScore();
                m_botAction.sendArenaMessage("   " + i + ".  " + Tools.centerString(sKills, 9) + "    " + Tools.centerString(sSave, 9) + "    " + Tools.centerString(sScore, 9) + "    "
                        + Tools.rightString(name, 20));
                m_players.remove(name);
            }
        }

        if (winner == WARBIRD) {
            m_botAction.sendArenaMessage("---------The Warbirds have tagged their way to victory!---------");
            m_botAction.changeAllShips(WARBIRD);
        } else if (winner == JAVELIN) {
            m_botAction.sendArenaMessage("---------The Javelins have tagged their way to victory!---------");
            m_botAction.changeAllShips(JAVELIN);
        }
        isRunning = false;
        cancel();

    }

    /**
     * getTopPlayer is the calculations for ordering the top players in doStats(); by score
     * 
     */
    private String getTopPlayer() {

        PlayerInfo topPlayer = null;

        for (PlayerInfo player : m_players.values()) {
            if (topPlayer == null) {
                topPlayer = player;
            } else if (player.getScore() > topPlayer.getScore()) {
                topPlayer = player;
            }
        }

        if (topPlayer == null)
            return "" + topPlayer;
        else
            return topPlayer.getName();
    }

    /**
     * Checks the records and makes any adjustments if needed
     * 
     */
    private class CheckTimer extends TimerTask {
        int timerInSeconds = 0;

        public void run() {
            timerInSeconds++;

            if (timerInSeconds == 5)
                doRules();
            if (timerInSeconds == 30)
                doWarning();
            if (timerInSeconds == 40)
                doGo();
            if (timerInSeconds > 40)
                recordCheck();
            if (timerInSeconds == timeLimit && timeLimit > 0)
                isTimer = true;

        }

        /**
         * A record check that goes off every second to make sure things are tracked properly.
         */
        private void recordCheck() {
            for (Player p : m_botAction.getPlayingPlayers()) {
                PlayerInfo name = m_players.get(p.getPlayerName());
                if (name == null && p.getFrequency() < 2) {
                    PlayerInfo player = new PlayerInfo(p.getPlayerName(), p.getShipType(), 0, 0, 0);
                    m_players.put(p.getPlayerName(), player);
                } else if (p.getFrequency() < 2 && name.shipType != p.getShipType()) {
                    name.shipType = p.getShipType();
                }
            }
            checkWinner();
        }

        /**
         * The rules that are displayed at the begining of each game.
         */
        private void doRules() {
            m_botAction.sendArenaMessage("---------------------------- FREEZE JAVS RULES ----------------------------");
            m_botAction.sendArenaMessage("|                                                                        |");
            m_botAction.sendArenaMessage("| 1.) There will be two teams, a team of warbirds versus a team of javs. |");
            m_botAction.sendArenaMessage("|                                                                        |");
            m_botAction.sendArenaMessage("| 2.) If you are shot (tagged) by an enemy you will become frozen, which |");
            m_botAction.sendArenaMessage("|     will render you unable to move or fire.                            |");
            m_botAction.sendArenaMessage("|                                                                        |");
            m_botAction.sendArenaMessage("| 3.) To become unfrozen a teammate must shoot (tag) you.                |");
            m_botAction.sendArenaMessage("|                                                                        |");
            m_botAction.sendArenaMessage("| 4.) If no time limit is specified, the first team to entirely freeze   |");
            m_botAction.sendArenaMessage("|     the other team wins.  If a time limit is specified, the team with  |");
            m_botAction.sendArenaMessage("|     the least number of frozen players when time is up wins.           |");
            m_botAction.sendArenaMessage("|                                                                        |");
            m_botAction.sendArenaMessage("--------------------------------------------------------------------------");
            m_botAction.sendArenaMessage("|                                                                        |");
            m_botAction.sendArenaMessage("| NOTE: If you're frozen and you need to leave, you can PM the bot with  |");
            m_botAction.sendArenaMessage("| with !leave to get back in spec.                                       |");
            m_botAction.sendArenaMessage("|                                                                        |");
            m_botAction.sendArenaMessage("--------------------------------------------------------------------------");
            m_botAction.sendArenaMessage("Use ESC to display all of the rules " + "at once.", 2);
        }

        /**
         * The 10 second warning before go.
         */
        private void doWarning() {
            if (timeLimit == 0) {
                m_botAction.sendArenaMessage("This game of freeze javs has no " + "time limit.");
                m_botAction.sendArenaMessage("The freezing will begin in " + "about 10 seconds!", 1);
            } else {
                m_botAction.sendArenaMessage("This game of freeze tag has a " + "time limit of " + (timeLimit * 60) + " minutes.");
                m_botAction.sendArenaMessage("The freezing will begin in " + "about 10 seconds!", 1);
                m_botAction.setTimer(timeLimit * 60);
            }
        }

        private void doGo() {
            m_botAction.changeAllShipsOnFreq(TEAM1_FREQ, WARBIRD);
            m_botAction.changeAllShipsOnFreq(TEAM2_FREQ, JAVELIN);
            m_botAction.warpFreqToLocation(TEAM1_FREQ, TEAM1_WARPX, TEAM1_WARPY);
            m_botAction.warpFreqToLocation(TEAM2_FREQ, TEAM2_WARPX, TEAM2_WARPY);
            m_botAction.scoreResetAll();
            m_botAction.shipResetAll();
            isRunning = true;
            createPlayerRecords();
            m_botAction.sendArenaMessage("GO GO GO !!!", 104);
        }
    }

    /**
     * Creates a temporary record for each player to track basic information such as name, shiptype, lagged out or not, scores, kills, saves, and tks.
     * information is cleared after each game is completed.
     */
    private class PlayerInfo {

        private String name;
        private int shipType;
        private int maxKills;
        private int maxScore;
        private int maxSaves;
        private boolean isPlaying = true;
        private boolean laggedOut = false;

        // Default values added to a record when the record is created.
        private PlayerInfo(String Name, int ShipType, int Kills, int Saves, int Score) {
            this.name = Name;
            this.shipType = ShipType;
            maxKills = 0;
            maxScore = 0;
            maxSaves = 0;
        }

        // Returns a players name.
        private String getName() {
            return name;
        }

        // Adds points for killing a player ( Default: 1)
        private void hadKill() {
            maxKills += m_kills;
            maxScore += m_kills;
        }

        // Subtracts points for tking a player (Default: 1)
        private void hadTK() {
            maxScore -= m_tk;
        }

        // Adds points for saving another player (Default: 1)
        private void hadSave() {
            maxSaves += m_saves;
            maxScore += m_saves;
        }

        // Sets player's lag status to laggedOut
        private void lagger() {
            laggedOut = true;
            m_botAction.sendPrivateMessage(name, "PM me with !lagout to get back in the game.");
        }

        // boolean check if the player is lagged out.
        private boolean isLagged() {
            return laggedOut;
        }

        private void isNotLagged() {
            laggedOut = false;
        }

        // boolean check if the player is playing.
        private boolean isPlaying() {
            return isPlaying;
        }

        // returns how many kills a player has.
        private int getKills() {
            return maxKills;
        }

        // returns how many kills a player has.
        private int getSaves() {
            return maxSaves;
        }

        // returns the score of a player (K+S-TK).
        private int getScore() {
            return maxScore;
        }

    }

    /**
     * Your typical manual that comes with every robot to make our everyday life a little easier.
     */
    public String[] getModHelpMessage() {

        String[] freezeTagHelp = { "!start                        -- Starts a game of freeze javs with no time " + "limit.",
                "!start <time limit>           -- Starts a game of freeze javs with the " + "specified time limit.  (15 minute minimum required)",
                "!stop                         -- Stops a game of freeze javs.", "!leave                        -- (Public Command) Allows frozen players to " + "get into spec.",
                "!enter                        -- (Public Command) Allows frozen players to " + "join the game.",
                "!lagout                       -- (Public Command) Allows frozen players to " + "return from the game."

        };
        return freezeTagHelp;
    }

    /**
     * Checkout.
     */
    public boolean isUnloadable() {
        clearRecords();
        isRunning = false;
        m_botAction.cancelTasks();
        return true;
    }

}