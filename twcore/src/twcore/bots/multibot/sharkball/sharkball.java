package twcore.bots.multibot.sharkball;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.BallPosition;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.SoccerGoal;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.StringBag;

/**
 * A MultiBot module made for ?go sharkball to be used with prize module
 * 
 * @author comrad
 * 
 */

public class sharkball extends MultiModule {

    private ArrayList<String> deathBox0 = new ArrayList<String>();
    private ArrayList<String> deathBox1 = new ArrayList<String>();
    private Player lastCarrier;
    private int scoreFreq;
    private boolean isRunning = false;
    private TimerTask gameStart;
    private int freq0Score = 0, freq1Score = 0;
    private Iterator<Player> levFreq, players;
    private String lev1, lev2;
    private int goalsNeeded = 5;
    private StringBag randomBag = new StringBag();

    /* Moderator+ Help Menu */
    public String[] modHelp = { "-----------------------------------------------------------------------",
            "ER+ Commands:", "!start                        -- Starts the game with random shooters.",
            "!start <FirstLev>:<SecondLev> -- Starts with specified players as lev.",
            "!goals # (Default is 5)       -- Sets the amount of goals needed to #.",
            "!stop                         -- Stops/cancels the game.",
            "!pHelp                        -- Displays the player help commands.",
            "NOTE: Be sure to setup the prizes with the prize module before starting",
            "      as this module doesn't handle prizing (Refer to A1 in sharkball."

    };

    public String[] playerHelp = { "Player Commands:", "!help      -- Displays this message.",
            "!rules     -- Displays the rules of Sharkball."

    };

    public void init() {
    }

    /* Required Methods */
    public void cancel() {
        m_botAction.cancelTasks();
    }

    public String[] getModHelpMessage() {
        return modHelp;
    }

    public boolean isUnloadable() {
        return true;
    }

    public void requestEvents(ModuleEventRequester events) {
        events.request(this, EventRequester.SOCCER_GOAL);
    }

    /* Messaging */
    public void handleEvent(Message event) {
        String message = event.getMessage();
        String sender = m_botAction.getPlayerName(event.getPlayerID());
        int messageType = event.getMessageType();

        if (opList.isER(sender) && messageType == Message.PRIVATE_MESSAGE)
            handleERCommands(sender, message);
        if (messageType == Message.PRIVATE_MESSAGE)
            handlePlayerCommands(sender, message);
    }

    /* ER+ Commands */
    public void handleERCommands(String sender, String msg) {
        if (msg.equalsIgnoreCase("!start"))
            doStartGame(sender, null);
        else if (msg.startsWith("!start "))
            doStartGame(sender, msg);
        else if (msg.equalsIgnoreCase("!stop"))
            doStopGame(sender);
        else if (msg.startsWith("!goals "))
            doSetGoals(sender, msg.substring(7));
        else if (msg.equalsIgnoreCase("!phelp"))
            m_botAction.smartPrivateMessageSpam(sender, playerHelp);
    }

    /* Player Commands */
    public void handlePlayerCommands(String sender, String msg) {
        if (msg.equalsIgnoreCase("!help"))
            m_botAction.smartPrivateMessageSpam(sender, playerHelp);
    }

    /* Events */
    public void handleEvent(PlayerDeath event) {

        if (isRunning) {
            Player killee = m_botAction.getPlayer(event.getKilleeID());
            String killeeName = m_botAction.getPlayerName(event.getKilleeID());

            if (killee.getFrequency() == 0) {
                m_botAction.shipReset(killeeName);
                m_botAction.warpTo(killeeName, 406, 511);
                deathBox0.add(killeeName);
            } else if (killee.getFrequency() == 1) {
                m_botAction.shipReset(killeeName);
                m_botAction.warpTo(killeeName, 618, 511);
                deathBox1.add(killeeName);
            }
        }
    }

    public void handleEvent(BallPosition event) {
        lastCarrier = m_botAction.getPlayer(event.getPlayerID());
    }

    public void handleEvent(SoccerGoal event) {
        if (isRunning) {
            scoreFreq = event.getFrequency();
            m_botAction.sendArenaMessage(lastCarrier + " scored! Freq " + scoreFreq + " has been released.", 2);
            if (scoreFreq == 0) {
                freq0Score++;
            } else if (scoreFreq == 1) {
                freq1Score++;
            }
            if (freq0Score >= goalsNeeded || freq1Score >= goalsNeeded) {
                m_botAction.sendArenaMessage("Frequency " + scoreFreq + " has scored " + goalsNeeded + "and has won!",
                        5);
            }
            if (scoreFreq == 0 && !deathBox0.isEmpty()) {
                for (int i = 0; i < deathBox0.size(); i++) {
                    m_botAction.specificPrize(deathBox0.get(i), 7);
                }
                deathBox0.clear();
            } else if (scoreFreq == 1 && !deathBox1.isEmpty()) {
                for (int i = 0; i < deathBox1.size(); i++) {
                    m_botAction.specificPrize(deathBox1.get(i), 7);
                }
                deathBox1.clear();
            }
        }
    }

    /* Commands */

    /* !Start Command */
    public void doStartGame(String name, String msg) {
        if (m_botAction.getNumPlayers() < 4) {
            m_botAction.sendPrivateMessage(name, "There is not enough people in a ship to start. (Min. of 4)");
            m_botAction.cancelTasks();
            isRunning = false;
        } else if (isRunning) {
            m_botAction.sendPrivateMessage(name, "The game has already been started. Please use !stop first.");
        } else {
            m_botAction.sendArenaMessage("Get ready. The game is about to begin in 10 seconds!", 2);
            try {
                String[] splitMsg = msg.substring(7).split(":");
                lev1 = m_botAction.getFuzzyPlayerName(splitMsg[0]);
                lev2 = m_botAction.getFuzzyPlayerName(splitMsg[1]);
            } catch (Exception e) {
                setRandomLevs();
                m_botAction.sendPrivateMessage(name, "Two random levs have been allocated (" + lev1 + "and" + lev2
                        + ")");
            }
            m_botAction.createNumberOfTeams(2);
            m_botAction.changeAllShips(8);
            m_botAction.setFreq(lev1, 2);
            m_botAction.setFreq(lev2, 2);
            m_botAction.setShip(lev1, 4);
            m_botAction.setShip(lev2, 4);
            gameStart = new TimerTask() {
                public void run() {
                    isRunning = true;
                    m_botAction.sendArenaMessage("GO GO GO!", 104);
                    m_botAction.warpFreqToLocation(2, 511, 406);
                    doWarpLevs(); // Warps the other levs to top
                }
            };
            m_botAction.scheduleTask(gameStart, 10000);
        }
    }

    /* !Stop Command */
    public void doStopGame(String name) {
        if (isRunning) {
            m_botAction.sendArenaMessage("The game has been stopped by " + name);
            isRunning = false;
            m_botAction.cancelTasks();
        } else {
            m_botAction.sendPrivateMessage(name, "Currently there is no game running.");
        }
    }

    /* !Goals # Command */
    public void doSetGoals(String name, String msg) {
        try {
            goalsNeeded = Integer.parseInt(msg);
        } catch (Exception e) {
            goalsNeeded = 5;
            m_botAction.sendPrivateMessage(name, "Wrong syntax. Please use !goals #. EG: '!goals 5'");
        }
        m_botAction.sendPrivateMessage(name, "The amount of goals needed is now set to " + goalsNeeded);
    }

    /* Split levs up and warp levis into boxes on go (Called in doStartGame) */
    public void doWarpLevs() {
        int count = 0;
        int i = 0;
        /* Get the number of players on freq 2 (Lev Freq) */
        levFreq = m_botAction.getFreqPlayerIterator(2);
        while (levFreq.hasNext()) {
            count++;
            levFreq.next();
        }

        /* Warp half the levs to the bottom box */
        levFreq = m_botAction.getFreqPlayerIterator(2);
        while (levFreq.hasNext() && i < count / 2) {
            String levName = levFreq.next().getPlayerName();
            m_botAction.warpTo(levName, 511, 618);
            i++;
        }
    }

    /* Make two random levs when no names are specified */
    public void setRandomLevs() {
        players = m_botAction.getPlayingPlayerIterator();
        if (players == null)
            return;
        while (players.hasNext()) {
            String playerName = players.next().getPlayerName();
            randomBag.add(playerName);
        }

        lev1 = randomBag.grabAndRemove();
        lev2 = randomBag.grabAndRemove();
    }
}