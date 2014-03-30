package twcore.bots.multibot.xdodgeball;

import java.util.TimerTask;
import java.util.TreeSet;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.SoccerGoal;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/**
 * xdodgeball.java Created: 12/2012
 * 
 * @author Fusha
 * 
 *         Simple module for ?go xdodgeball. Opens team's safe when goal is scored, then closes it again.
 * 
 */

public class xdodgeball extends MultiModule {

    //Variables

    private boolean isRunning = false;
    private boolean isStarted = false;
    private int wins = 3;
    private boolean turbo = false;
    private boolean shutup = false;
    private boolean freq0Scored = false;
    private boolean freq1Scored = false;
    private int freq0Score = 0;
    private int freq1Score = 0;
    private int doorTimer = 10;

    private TimerTask startTimer;
    private TimerTask doInvites;
    private TimerTask freq0Timer;
    private TimerTask freq1Timer;
    private TimerTask checkWinner;
    private TimerTask pause;
    private TimerTask pause2;

    private TreeSet<String> freq0Safe = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private TreeSet<String> freq1Safe = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    public void init() {
    }

    /**
     * This method requests the events used by this module.
     */

    public void requestEvents(ModuleEventRequester eventRequester) {

        eventRequester.request(this, EventRequester.SOCCER_GOAL);
        eventRequester.request(this, EventRequester.PLAYER_POSITION);
        eventRequester.request(this, EventRequester.PLAYER_LEFT);
        eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
    }

    /**
     * Handles messages
     */

    public void handleEvent(Message event) {

        int type = event.getMessageType();
        String message = event.getMessage();

        if (type == Message.PRIVATE_MESSAGE) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            if (name != null)
                handleCommand(name, message);
        }
    }

    /**
     * Handles goals.
     */

    public void handleEvent(SoccerGoal event) {

        if (isRunning) {
            if (event.getFrequency() == 0) {
                startFreq0Timer();
                freq0Safe.clear();
            } else if (event.getFrequency() == 1) {
                startFreq1Timer();
                freq1Safe.clear();
            }
        }

    }

    /**
     * Registers players in safes and warps them if their freqs have scored.
     */

    public void handleEvent(PlayerPosition event) {

        boolean isSafe = false;
        int freq = -1;
        Player p;
        String playerName;

        p = m_botAction.getPlayer(event.getPlayerID());

        if (p == null)
            return;

        isSafe = event.isInSafe();
        freq = p.getFrequency();
        playerName = m_botAction.getPlayerName(p.getPlayerID());

        if (playerName == null)
            return;

        if (isRunning) {
            if (freq == 0 && isSafe) {
                if (freq0Scored) {
                    m_botAction.warpTo(playerName, 474, 492, 3);
                } else {
                    freq0Safe.add(playerName);
                }
            } else if (freq == 1 && isSafe) {
                if (freq1Scored) {
                    m_botAction.warpTo(playerName, 550, 532, 3);
                } else {
                    freq1Safe.add(playerName);
                }
            } else if (freq == 0 && !isSafe) {
                if (freq0Safe.size() > 0) {
                    if (freq0Safe.contains(playerName)) {
                        freq0Safe.remove(playerName);
                    }
                }
            } else if (freq == 1 && !isSafe) {
                if (freq1Safe.size() > 0) {
                    if (freq1Safe.contains(playerName)) {
                        freq1Safe.remove(playerName);
                    }
                }
            }
        }
    }

    /**
     * Removes player from lists if they leave
     */

    public void handleEvent(PlayerLeft event) {
        if (isRunning) {
            String p = m_botAction.getPlayerName(event.getPlayerID());
            if (p == null) {
                return;
            }
            if (freq0Safe.size() > 0) {
                if (freq0Safe.contains(p)) {
                    freq0Safe.remove(p);
                }
            }
            if (freq1Safe.size() > 0) {
                if (freq1Safe.contains(p)) {
                    freq1Safe.remove(p);
                }
            }
        }
    }

    /**
     * Same if a player specs
     */

    public void handleEvent(FrequencyShipChange event) {
        if (isRunning) {
            if (event.getShipType() == 0) {
                String p = m_botAction.getPlayerName(event.getPlayerID());
                if (p == null) {
                    return;
                }
                if (freq0Safe.size() > 0) {
                    if (freq0Safe.contains(p)) {
                        freq0Safe.remove(p);
                    }
                }
                if (freq1Safe.size() > 0) {
                    if (freq1Safe.contains(p)) {
                        freq1Safe.remove(p);
                    }
                }
            }
        }
    }

    /**
     * Sees if a message is a command and handles it
     */

    private void handleCommand(String name, String command) {

        String cmd = command.toLowerCase();

        if (opList.isER(name)) {
            if (cmd.equals("!start")) {
                if (!isStarted) {
                    startGame();
                } else {
                    m_botAction.sendPrivateMessage(name, "Game is already started!");
                }

            } else if (cmd.equals("!stop")) {
                if (isStarted) {
                    stopGame(true);
                    m_botAction.sendArenaMessage("Game has been stopped", 101);
                } else {
                    m_botAction.sendPrivateMessage(name, "The game hasn't even started yet!!! If you really want to stop it, do !start first!");
                }

            } else if (cmd.equals("!turbo")) {
                if (!isStarted) {
                    if (!turbo) {
                        turbo = true;
                        m_botAction.sendPrivateMessage(name, "Turbo mode on: games will not stop until you message me with !stop");
                    } else {
                        turbo = false;
                        m_botAction.sendPrivateMessage(name, "Turbo mode off.");
                    }
                } else {
                    m_botAction.sendPrivateMessage(name, "Too late to change that, game's already begun!");
                }

            } else if (cmd.startsWith("!wins ")) {
                if (!isStarted) {
                    if (cmd.length() > 5) {
                        String stringWins = cmd.substring(6);
                        try {
                            wins = Integer.parseInt(stringWins);
                            if (wins < 1 || wins > 50) {
                                m_botAction.sendPrivateMessage(name, "Please choose a more reasonable amount!");
                                wins = 3;
                            }
                            m_botAction.sendPrivateMessage(name, "Game will be played to " + wins + " wins.");
                        } catch (Exception e) {
                            m_botAction.sendPrivateMessage(name, "Formatting error, try again.");
                        }
                    } else {
                        m_botAction.sendPrivateMessage(name, "Formatting error, try again.");
                    }
                } else {
                    m_botAction.sendPrivateMessage(name, "Too late to change that, game's already begun!");
                }

            } else if (cmd.equals("!specall")) {
                m_botAction.specAll();
                m_botAction.sendArenaMessage(name + " has specced all of you! Quick, get back in!", 1);

            } else if (cmd.equals("!shutup")) {
                if (!shutup) {
                    m_botAction.sendPrivateMessage(name, "Okay geeeeeeez, I'll be quiet now :x");
                    m_botAction.cancelTask(doInvites);
                    shutup = true;
                } else {
                    m_botAction.sendPrivateMessage(name, "My voice is now free, and I will never be quiet again >:]");
                    shutup = false;
                }

            } else if (cmd.startsWith("!timer ")) {
                if (cmd.length() > 6) {
                    String stringDoorTimer = cmd.substring(7);
                    try {
                        doorTimer = Integer.parseInt(stringDoorTimer);
                        if (doorTimer < 0 || doorTimer > 50) {
                            m_botAction.sendPrivateMessage(name, "Please choose a more reasonable amount!");
                            doorTimer = 10;
                        }
                        if (isStarted) {
                            m_botAction.sendArenaMessage("Safe timer set to " + doorTimer + " seconds.", 1);
                        } else {
                            m_botAction.sendPrivateMessage(name, "Safe timer set to " + doorTimer + " seconds.");
                        }
                    } catch (Exception e) {
                        m_botAction.sendPrivateMessage(name, "Formatting error, try again.");
                    }
                } else {
                    m_botAction.sendPrivateMessage(name, "Formatting error, try again.");
                }
            } else if (cmd.equals("!spamrules")) {
                doSpamRules();
            }
        }
    }

    /**
     * Spams the rules
     */

    private void doSpamRules() {
        m_botAction.sendArenaMessage("------------------------------DESE R ROOLZ PLZ READ OK-----------------------------");
        m_botAction.sendArenaMessage("| It's really simple! If you die, you're trapped in safe. If your team scores,    |");
        m_botAction.sendArenaMessage("| you get out of safe! If an entire freq is in safe, they lose.                   |");
        m_botAction.sendArenaMessage("| So just kill and don't die, really! And try to score when you can! Teamwork     |");
        m_botAction.sendArenaMessage("| is essential. Greens give you thors. Press F6 to shoot thors btw! Good luck. <3 |");
        m_botAction.sendArenaMessage("-----------------------------------------------------------------------------------", 2);

    }

    /**
     * Starts game, does the warning and warping
     */

    private void startGame() {

        isStarted = true;
        freq0Safe.clear();
        freq1Safe.clear();
        freq0Scored = false;
        freq1Scored = false;
        m_botAction.warpFreqToLocation(0, 473, 485);
        m_botAction.warpFreqToLocation(1, 551, 539);
        m_botAction.setDoors(255);
        m_botAction.shipResetAll();
        if (!turbo) {
            m_botAction.sendArenaMessage("Round is about to begin! We're playing to " + wins + " wins! The safe timer is set to " + doorTimer + " seconds!", 2);
        } else {
            m_botAction.sendArenaMessage("Round is about to begin!");
        }

        if (!shutup) {
            doInvites();
        }

        startTimer = new TimerTask() {

            @Override
            public void run() {

                m_botAction.warpFreqToLocation(0, 474, 492);
                m_botAction.warpFreqToLocation(1, 550, 532);
                isRunning = true;
                startCheckWinner();
                if (!turbo) {
                    m_botAction.sendArenaMessage("GO GO GO !!!", 104);
                } else { //this part makes a random number from 1 to 29 :O
                    double doubleRandomSound = 0;
                    doubleRandomSound = Math.floor((Math.random() * 29) + 1);
                    int randomSound = (int) doubleRandomSound;

                    if (randomSound == 12) {
                        m_botAction.sendArenaMessage("GO GO GO !!!", 104);
                    } else {
                        m_botAction.sendArenaMessage("GO GO GO !!!", randomSound);
                    }

                }
            }
        };

        m_botAction.scheduleTask(startTimer, 10001);
    }

    /**
     * Checks if there's a free spot and if so invites dudes in spec to join game ever 30 seconds
     */

    private void doInvites() {

        doInvites = new TimerTask() {
            @Override
            public void run() {

                if (!shutup) {
                    int freqSize = 0;
                    freqSize = m_botAction.getFrequencySize(0) + m_botAction.getFrequencySize(1);
                    if (freqSize < 12) {
                        m_botAction.sendTeamMessage("There are free spots! Hop in to play!!!");
                    }
                }
            }

        };

        m_botAction.scheduleTask(doInvites, 5000, 30000);
    }

    /**
     * Timers until the bot stops warping players.
     */

    private void startFreq0Timer() {

        freq0Scored = true;

        freq0Timer = new TimerTask() {
            @Override
            public void run() {
                freq0Scored = false;
                m_botAction.sendArenaMessage("Freq 0's safe is now locked!", 29);
            }
        };

        m_botAction.scheduleTask(freq0Timer, doorTimer * 1000);
    }

    private void startFreq1Timer() {

        freq1Scored = true;

        freq1Timer = new TimerTask() {
            @Override
            public void run() {
                freq1Scored = false;
                m_botAction.sendArenaMessage("Freq 1's safe is now locked!", 29);
            }
        };

        m_botAction.scheduleTask(freq1Timer, doorTimer * 1000);
    }

    /**
     * Checks winner ever 7 seconds
     */

    public void startCheckWinner() {

        checkWinner = new TimerTask() {
            @Override
            public void run() {

                int freq0Size = -1;
                int freq1Size = -1;
                int freq0SafeSize = -1;
                int freq1SafeSize = -1;

                freq0Size = m_botAction.getFrequencySize(0);
                freq1Size = m_botAction.getFrequencySize(1);
                freq0SafeSize = freq0Safe.size();
                freq1SafeSize = freq1Safe.size();

                //debug
                //m_botAction.sendArenaMessage("Freq 0 safe: " + freq0Safe.size() + " Freq 1 safe: " + freq1Safe.size() + ".");
                if (isRunning) {
                    if (freq0Size == freq0SafeSize && freq1Size == freq1SafeSize) {
                        m_botAction.sendArenaMessage("A tie? OMG... let's try this again, shall we?", 23);
                        m_botAction.cancelTasks();
                        freq0Safe.clear();
                        freq1Safe.clear();
                        if (!turbo) {
                            pauseAndGo();
                        } else {
                            startGame();
                        }
                    } else if (freq0Size == freq0Safe.size()) {
                        doFreq1Won();
                    } else if (freq1Size == freq1Safe.size()) {
                        doFreq0Won();
                    }
                }
            }
        };

        m_botAction.scheduleTask(checkWinner, 10000, 7000);
    }

    /**
     * These next two handle what happens when a freq has won a round and keep track of the score.
     */

    private void doFreq0Won() {

        freq0Score++;
        int freq0RoundsLeft = wins - freq0Score;
        isRunning = false;
        m_botAction.cancelTasks();
        freq0Safe.clear();
        freq1Safe.clear();
        if (!turbo) {
            if (freq0Score == wins) {
                announceWinner(0);
            } else {
                m_botAction.sendArenaMessage("Freq 0 has won this round! Score: Freq 0: " + freq0Score + " - Freq 1: " + freq1Score + ".", 5);
                m_botAction.sendArenaMessage("Freq 0 needs " + freq0RoundsLeft + " round wins until they win the game!");
                pauseAndGo();
            }
        } else {
            m_botAction.sendArenaMessage("Freq 0 wins this one! But there are still many to come... Score: " + freq0Score + " - " + freq1Score + ".", 5);
            startGame();
        }
    }

    private void doFreq1Won() {

        freq1Score++;
        int freq1RoundsLeft = wins - freq1Score;
        m_botAction.cancelTasks();
        freq0Safe.clear();
        freq1Safe.clear();
        isRunning = false;
        if (!turbo) {
            if (freq1Score == wins) {
                announceWinner(1);
            } else {
                m_botAction.sendArenaMessage("Freq 1 has won this round! Score: " + freq0Score + " - " + freq1Score + ".", 5);
                m_botAction.sendArenaMessage("Freq 1 needs " + freq1RoundsLeft + " round wins until they win the game!");
                pauseAndGo();
            }
        } else {
            m_botAction.sendArenaMessage("Freq 1 wins this one! But there are still many to come... Score: " + freq0Score + " - " + freq1Score + ".", 5);
            startGame();
        }
    }

    /**
     * Announces the winner and stops the game.
     */

    private void announceWinner(int freq) {

        int scoreDifference1 = freq1Score - freq0Score;
        int scoreDifference0 = freq0Score - freq1Score;

        if (freq == 1) {
            m_botAction.sendArenaMessage("FREQUENCY 1 ARE THE WINNERS! They have DEFEATED freq 0 by " + scoreDifference1 + " rounds and are the champions of awesomeness!", 100);
            pauseAndStop();
        } else if (freq == 0) {
            m_botAction.sendArenaMessage("FREQUENCY 0 ARE THE WINNERS! They have DEFEATED freq 1 by " + scoreDifference0 + " rounds and are the champions of awesomeness!", 100);
            pauseAndStop();
        }
    }

    /**
     * Just some small pauses between rounds
     */

    private void pauseAndGo() {

        pause = new TimerTask() {
            @Override
            public void run() {
                startGame();
            }
        };

        m_botAction.sendArenaMessage("Next round begins in ~15 seconds! Take a quick breather. >:)");
        m_botAction.scheduleTask(pause, 5000);
    }

    private void pauseAndStop() {

        pause2 = new TimerTask() {
            @Override
            public void run() {
                stopGame(true);
                m_botAction.sendArenaMessage("Game has been stopped", 101);
            }
        };

        m_botAction.scheduleTask(pause2, 8500);
    }

    /**
     * Stops game and clears all the records.
     */

    private void stopGame(boolean clearScore) {
        cancel();
        if (clearScore) {
            freq0Score = 0;
            freq1Score = 0;
        }
    }

    public void cancel() {
        isRunning = false;
        isStarted = false;
        freq0Safe.clear();
        freq1Safe.clear();
        m_botAction.cancelTasks();
        m_botAction.setDoors(0);
    }

    public String[] getModHelpMessage() { //The help message for mods

        String[] JBHelp = { 
        		"------------------------------------------------------------------------------", 
        		"|!start     -   Starts the exciting game of XDODGEBALL!                      |",
                "|!stop      -   Cancels the game                                             |", 
                "|!wins #    -   Sets how many round wins we're playing to (Default is 3)     |",
                "|!timer #   -   Adjusts the safe timer                                       |", 
                "|                              (How long the safe is open (Default 10)       |",
                "|!turbo     -   Enables UNLIMITED TURBO MODE                                 |", 
                "|!specall   -   Spectates everyone (for team shuffling)                      |",
                "|!spamrules -   I forgot what this does, sorry. :(                           |", 
                "|!shutup    -   Stops the bot from spamming the spec frequency.              |",
                "------------------------------------------------------------------------------" };
        return JBHelp;
    }

    public boolean isUnloadable() { //Some thing I'm required to put
        isRunning = false;
        isStarted = false;
        freq0Safe.clear();
        freq1Safe.clear();
        m_botAction.cancelTasks();
        return true;
    }
}

//I SPENT THE WHOLE NIGHT WRITING THIS YOU GUISES
//AND I AM SO
//PROUD
//<3