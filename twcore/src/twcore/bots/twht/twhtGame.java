/**
 * 
 */
package twcore.bots.twht;

import java.util.LinkedList;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import twcore.core.BotAction;
import twcore.core.events.BallPosition;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.FrequencyChange;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.SoccerGoal;
import twcore.core.events.WatchDamage;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * twhtGame is the class that handles most of the tasks that are used for the whole game. 
 * It also serves as a connection point between the round and the teams.
 * @author Ian
 * 
 */
public class twhtGame {

    BotAction ba;

    TimerTask intermission;
    TimerTask delay;
    TimerTask goalDelay;
    TimerTask taskDelay;
    TimerTask statsDelay;
    TimerTask penShotDelay;

    twhtTeam m_team1;
    twhtTeam m_team2;
    twht twht;

    int m_fnTeam1ID;
    int m_fnTeam2ID;
    int m_matchID;
    int fnTeam1Score;
    int fnTeam2Score;
    int requestRecordNumber;
    int SPEC_FREQ;

    String m_fcTeam1Name;
    String m_fcTeam2Name;
    String m_fcRefName;
    String goalScorer;
    String assistOne = null;
    String assistTwo = null;
    
    boolean voteInProgress;
    boolean isDatabase;
    boolean isIntermission;
    boolean isPShot;
    
    TreeSet<String> m_judge = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    TreeMap<String, Integer> m_penalties = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
    TreeMap<String, String> m_officials = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    TreeMap<Integer, RefRequest> refRequest = new TreeMap<Integer, RefRequest>();

    LinkedList<twhtRound> m_rounds = new LinkedList<twhtRound>();
    twhtRound m_curRound;

    //Creates a new instance of twhtGame 
    public twhtGame(int matchID, String fcRefName, String fcTeam1Name, int fnTeam1ID, int fnTeam2ID, String fcTeam2Name, BotAction botAction, twht TWHT, boolean noDatabase) {
        ba = botAction;
        twht = TWHT;
        isDatabase = noDatabase;
        m_fnTeam1ID = fnTeam1ID;
        m_fnTeam2ID = fnTeam2ID;
        m_matchID = matchID;
        m_fcTeam1Name = fcTeam1Name;
        m_fcTeam2Name = fcTeam2Name;
        m_fcRefName = fcRefName;
        SPEC_FREQ = 8025;
        setupTeams();
        setupRound(1);
    }

    /**
     * This method sets up the two teams at the begining of the game
     */
    public void setupTeams() {
        m_team1 = new twhtTeam(m_fcTeam1Name, m_fnTeam1ID, 0, 1, this, ba);
        m_team2 = new twhtTeam(m_fcTeam2Name, m_fnTeam2ID, 1, 2, this, ba);
    }  

    /**
     * The event that is triggered at the time a player leaves the arena
     */
    public void handleEvent(FrequencyShipChange event) {
        if (m_curRound != null) {
            int shipType;
            Player p;
            int playerID;

            playerID = event.getPlayerID();
            p = ba.getPlayer(playerID);
            if (p == null)
                return;
            
            if (p.equals(ba.getBotName()))
                return;
            
            shipType = p.getShipType();
            if (shipType == Tools.Ship.SPECTATOR) {
                twhtTeam t = null;
                twhtPlayer pA;

                t = getPlayerTeam(p.getPlayerName());
                
                if (t == null)
                    return;                
                
                pA = t.searchPlayer(p.getPlayerName());
                
                if (pA != null && (pA.getPlayerState() == 1 || pA.getPlayerState() == 4)){
                    t.doLagout(pA.getPlayerName());
                    pA.playerLaggedOut();
                }
            }
        }
    }
    
    /**
     * The event that is triggered at the time a player changes freq or ship.
     */    
    public void handleEvent(FrequencyChange event) {
        if (m_curRound != null) {
            int freq;
            Player p;
            int playerID;

            playerID = event.getPlayerID();
            p = ba.getPlayer(playerID);
            if (p == null)
                return;
            
            if (p.equals(ba.getBotName()))
                return;
            
            freq = p.getFrequency();
            if (freq == 8025) {
                twhtTeam t = null;
                twhtPlayer pA;

                t = getPlayerTeam(p.getPlayerName());
                
                if (t == null)
                    return;                
                
                pA = t.searchPlayer(p.getPlayerName());
                
                if (pA != null && (pA.getPlayerState() == 1 || pA.getPlayerState() == 4)){
                    t.doLagout(pA.getPlayerName());
                    pA.playerLaggedOut();
                }
            }
        }   
    }

    /**
     * The event that is triggered at the time a player leaves the arena
     */
    public void handleEvent(PlayerEntered event) {
        int playerID;
        String playerName;
        twhtPlayer pA;
        twhtTeam team;
        
        if (m_curRound != null) {
            playerID = event.getPlayerID();            
                       
            playerName = ba.getPlayerName(playerID);            
            if (playerName == null)
                return;
            
            team = getPlayerTeam(playerName);            
            if (team == null)
                ba.sendPrivateMessage(playerName, "Welcome to Hockey. There is a TWHT match currently in round " + m_curRound.getRoundNum() + " with " + m_fcTeam1Name + "(" + fnTeam1Score + ") vs " + m_fcTeam2Name + "(" + fnTeam2Score + ")");
            else {
                pA = team.searchPlayer(playerName);
                if (pA.getPlayerState() == 3)
                    ba.sendPrivateMessage(playerName, "You are currently lagged out from the game. PM me with !lagout to return.");                    
            }   
        }
    }

    /**
     * The event that is triggered at the time a player leaves the arena
     */
    public void handleEvent(PlayerLeft event) {
        if (m_curRound != null) {
            int playerID;
            String player;

            playerID = event.getPlayerID();
            player = ba.getPlayerName(playerID);

            if (player == null)
                return;

            if (player.equals(ba.getBotName()))
                return;

            twhtTeam t = null;
            twhtPlayer pA;

            t = getPlayerTeam(player);
            
            if (t == null)
                return;                
            
            pA = t.searchPlayer(player);
            
            if (pA != null && (pA.getPlayerState() == 1 || pA.getPlayerState() == 4)){
                t.doLagout(pA.getPlayerName());
                pA.playerLaggedOut();
            }
        }
    }

    /**
     * The event that is triggered at the time a player dies
     */
    public void handleEvent(PlayerDeath event) {
        if (m_curRound != null && m_curRound.getRoundState() == 1) {
            int killerID;
            int killeeID;
            String killer;
            String killee;
            twhtTeam tA;
            twhtTeam tB;
            twhtPlayer pA;
            twhtPlayer pB;            
                
            killerID = event.getKillerID();
            killeeID = event.getKilleeID();
            
            killer = ba.getPlayerName(killerID);
            killee = ba.getPlayerName(killeeID);            
            if (killer == null || killee == null)
                return;
            
            tA = getPlayerTeam(killer);
            tB = getPlayerTeam(killee);            
            if (tA == null || tB ==  null)
                return;
            
            pA = tA.searchPlayer(killer);
            pB = tB.searchPlayer(killee);            
            if (pA == null || pB ==  null)
                return;
            
            pA.doStats(8);
            pB.setLastCheck(m_curRound.getIntTime(),pA.getPlayerName());
        }
    }

    /**
     * The event that is triggered at the time a a soccer goal is scored
     */
    public void handleEvent(SoccerGoal event) {
        if (m_curRound != null) {
            if (m_curRound.getRoundState() == 1 && !m_curRound.ballPlayer.isEmpty()) {
                m_curRound.m_fnRoundState = 3;
                goalScorer = m_curRound.ballPlayer.pop();
                ba.sendArenaMessage("Goal is under review.", 2);
                
                while (m_curRound.ballPlayer.size() > 2 && assistTwo == null ) {
                    if (assistOne == null) {
                        m_curRound.ballPlayer.pop();
                        assistOne = m_curRound.ballPlayer.pop();    
                        
                        if (assistOne == goalScorer)
                                assistOne = null;
                    } else {
                        m_curRound.ballPlayer.pop();
                        assistTwo = m_curRound.ballPlayer.pop();
                        
                            if (assistTwo == goalScorer)
                                assistTwo = null;
                    }                    
                }
                m_curRound.ballPlayer.clear();
                doReview();              
            }
        }
    }

    /**
     * The event that is triggered at the time the ball moves.
     */
    public void handleEvent(BallPosition event) {
        if (m_curRound != null) 
            m_curRound.handleEvent(event);        
    }
    
    /**
     * The event that is triggered at the time a player changes freq or ship.
     */    
    public void handleEvent(WatchDamage event) {
        if (m_curRound != null) 
            m_curRound.handleEvent(event);        
    }
    
    /**
     * This method is ran before every round to setup and open each round. 
     * 
     * @param roundNumber
     */
    public void setupRound(int roundNumber) {
        if (roundNumber == 1) {
            m_curRound = new twhtRound(m_matchID, m_fnTeam1ID, m_fnTeam2ID, roundNumber, m_fcTeam1Name, m_fcTeam2Name, m_fcRefName, this);
            m_rounds.add(m_curRound);
        } else {
            twhtRound newRound = new twhtRound(m_matchID, m_fnTeam1ID, m_fnTeam2ID, roundNumber, m_fcTeam1Name, m_fcTeam2Name, m_fcRefName, this);
            m_curRound = newRound;
            m_rounds.add(newRound);
        }
        m_curRound.doAddPlayers();
    }

    /**
     * This method is ran at the end of everyround and ends the round then moves 
     * into a new one if needed. 
     * 
     * @param roundNum
     */
    public void reportEndOfRound(int roundNum) {
        final int roundNumber = roundNum;
        m_curRound.doRemoveBall();
        m_curRound.cancel();
        m_team1.setEndRound();
        m_team2.setEndRound();
        
        if (roundNumber == 1) {
            ba.sendArenaMessage("Time is up! Round one has ended. ", 5);
            ba.sendArenaMessage("Score after one period: " + m_fcTeam1Name + ": " + fnTeam1Score + " vs " + m_fcTeam2Name + ": " + fnTeam2Score);
            
            delay = new TimerTask() {
                @Override
                public void run() {
                ba.sendArenaMessage("Round two will begin after a 2 minute intermission", 2);
                isIntermission = true;
                
                    intermission = new TimerTask() {
                        public void run() {
                            setupRound(roundNumber + 1);
                            isIntermission = false;
                        }
                    }; ba.scheduleTask(intermission, Tools.TimeInMillis.MINUTE * 2);
                }
            }; ba.scheduleTask(delay, Tools.TimeInMillis.SECOND * 10);
                
                statsDelay = new TimerTask() {
                @Override
                public void run() {
                    m_team1.getTeamStats();
                    m_team2.getTeamStats();
                }
            }; ba.scheduleTask(statsDelay, Tools.TimeInMillis.SECOND * 20);
                
        } else if (roundNumber == 2) {
            if (fnTeam1Score > fnTeam2Score) 
                doEndGame(m_fcTeam1Name, m_fcTeam2Name, fnTeam1Score, fnTeam2Score);
            else if (fnTeam2Score > fnTeam1Score) 
                doEndGame(m_fcTeam2Name, m_fcTeam1Name, fnTeam2Score, fnTeam1Score);
            else if (fnTeam2Score == fnTeam1Score) {
                ba.sendArenaMessage("Time is up! The round is over.", 5);
                ba.sendArenaMessage("Score is tied! This game is going into overtime.");
                isIntermission = true;
                
                delay = new TimerTask() {
                    @Override
                    public void run() {
                    ba.sendArenaMessage("Round two will begin after a 2 minute intermission", 2);
                    
                    intermission = new TimerTask() {
                        public void run() {
                            setupRound(roundNumber + 1);
                            isIntermission = false;
                            }
                        }; ba.scheduleTask(intermission, Tools.TimeInMillis.MINUTE * 2);
                    }
                }; ba.scheduleTask(delay, Tools.TimeInMillis.SECOND * 10);
                        
                statsDelay = new TimerTask() {
                    @Override
                    public void run() {
                        m_team1.getTeamStats();
                        m_team2.getTeamStats();
                    }
                }; ba.scheduleTask(statsDelay, Tools.TimeInMillis.SECOND * 20);
            }
        } else if (roundNumber == 3) {
            if (fnTeam1Score > fnTeam2Score) 
                doEndGame(m_fcTeam1Name, m_fcTeam2Name, fnTeam1Score, fnTeam2Score);
            else if (fnTeam2Score > fnTeam1Score) 
                doEndGame(m_fcTeam2Name, m_fcTeam1Name, fnTeam2Score, fnTeam1Score);
            else if (fnTeam2Score == fnTeam1Score) {
                ba.sendArenaMessage("Time is up! Overtime has ended", 5);
                ba.sendArenaMessage("The score is still tied, Prepare for a shootout.", 2);
                
                delay = new TimerTask() {
                    @Override
                    public void run() {
                    setupRound(roundNumber + 1);
                    }
                }; ba.scheduleTask(delay, Tools.TimeInMillis.SECOND * 10);
            }
        }
    }
    
    /**
     * Adds a penalty on a player.
     * 
     * @param name
     * @param msg
     */
    public void setPenalty(String name, String msg) {
        if (m_curRound != null) {
            twhtTeam team;
            twhtPlayer playerA;
            int freq;
            int penaltyTime;
            int penaltySeconds;
            String[] splitCmd;
            
            if (m_curRound.getRoundState() != 0) {
                if (msg.contains(":")) {
                    splitCmd = msg.split(":");

                    if (splitCmd.length == 3) {
                        try {
                            penaltySeconds = Integer.parseInt(splitCmd[1]);
                        } catch (NumberFormatException e) {
                            return;
                        }

                        team = getPlayerTeam(splitCmd[0]);

                        if (team != null) {
                            playerA = team.searchPlayer(splitCmd[0]);
                            freq = team.getFrequency();
                            penaltyTime = m_curRound.getIntTime() + penaltySeconds;
                            playerA.setPenalty(penaltyTime);
                            ba.sendArenaMessage("Penalty set for " + penaltySeconds + " seconds on " + playerA.getPlayerName() + " for " + splitCmd[2], 23);

                            if (freq == 0)
                                ba.warpTo(playerA.getPlayerName(), 500, 442);
                            if (freq == 1)
                                ba.warpTo(playerA.getPlayerName(), 520, 442);
                            
                            if (splitCmd[2].equalsIgnoreCase("Attacked Goalie"))
                                team.doAddPenalty(1);
                            else if (splitCmd[2].equalsIgnoreCase("Blatant Defensive Crease"))
                                team.doAddPenalty(2);
                            else if (splitCmd[2].equalsIgnoreCase("Defensive Crease"))
                                team.doAddPenalty(3);
                            else if (splitCmd[2].equalsIgnoreCase("Goalie interference"))
                                team.doAddPenalty(4);
                            else if (splitCmd[2].equalsIgnoreCase("Respawn Killing"))
                                team.doAddPenalty(5);
                               
                        } else 
                            ba.sendPrivateMessage(name, "Player not found on either team.");                        
                    } else 
                        ba.sendPrivateMessage(name, "Invalid format. Please use !penalty <name>:seconds#:reason");                    
                }
            }
        }
    }
    
    /**
     * Sets up the frequency and side for both teams
     */
    public void setFrequencyAndSide() {
        m_team1.setFreqAndSide();
        m_team2.setFreqAndSide();
    }

    /**
     * Allows the referee to set the round of the game for replays. This only can be used during
     * the adding players phase.
     * 
     * @param name
     * @param msg
     */
    public void setRound(String name, String msg) {
        int roundNumber;

        if (m_curRound != null && m_curRound.getRoundState() == 0) {
            try {
                roundNumber = Integer.parseInt(msg);
            } catch (NumberFormatException e) {
                return;
            }

            if (roundNumber > 0 && roundNumber < 3)
                setupRound(roundNumber);
        }
    }

    /**
     * Changes the frequency according to the round for both teams.
     * 
     * @param roundNum
     */
    public void setFreqs(int roundNum) {
        if (roundNum == 1 || roundNum == 3) {
            m_team1.setFrequency(0);
            m_team2.setFrequency(1);
        } else if (roundNum == 2) {
            m_team1.setFrequency(1);
            m_team2.setFrequency(0);
        }
    }
   
    /**
     * Ends the game with the current score
     * 
     * @param name
     */
    public void setEndGame(String name) {
        if (fnTeam1Score > fnTeam2Score) 
            doEndGame(m_fcTeam1Name, m_fcTeam2Name, fnTeam1Score, fnTeam2Score);
        else if (fnTeam2Score > fnTeam1Score) 
            doEndGame(m_fcTeam2Name, m_fcTeam1Name, fnTeam2Score, fnTeam1Score);    
    }
    
    /**
     * This method will get the votes of the different players.
     * 
     * @param name
     * @param msg
     */
    public void getVote(String name, String vote) {
        if (m_curRound != null) {
            if (voteInProgress && m_judge.contains(name)) {
                int clVote = 0;
                int lagVote = 0;
                int gkVote = 0;
                int crVote = 0;
                int ogVote = 0;

                m_officials.put(name, vote);

                for (String votes : m_officials.values()) {
                    if (votes == "clean")
                        clVote++;
                    if (votes == "lag")
                        lagVote++;
                    if (votes == "goalieKill")
                        gkVote++;
                    if (votes == "crease")
                        crVote++;
                    if (votes == "ownGoal")
                        ogVote++;
                }
                ba.sendTeamMessage(name + " has voted " + vote + ".");
                ba.sendTeamMessage("Totals: CLEAN(" + clVote + ")  LAG(" + lagVote + ") GK(" + gkVote + ")  CR(" + crVote + ") OG(" + ogVote + ")");
            }
        }
    }

    /**
     * This method checks if player is on a team and then returns the frequency that the team is currently
     * 
     * @param name
     * @return
     */
    public Integer getPlayerFreqency(String name) {
        int frequency = -1;

        if (m_team1.isPlayer(name))
            frequency = m_team1.getFrequency();
        else if (m_team2.isPlayer(name))
            frequency = m_team2.getFrequency();

        return frequency;
    }

    /**
     * Called to find a player's team. Returns as a twhtTeam variable.
     * 
     * @param name
     * @return
     */
    public twhtTeam getPlayerTeam(String name) {
        twhtTeam team = null;
        twhtPlayer playerA = null;

        playerA = m_team1.searchPlayer(name);
        
        if (playerA != null)
            team = m_team1;

        playerA = m_team2.searchPlayer(name);
        
        if (playerA != null)
            team = m_team2;

        return team;
    }

    /**
     * Increrements the record number for the list 
     * 
     * @return
     */
    public Integer getNextRecordNumber() {
        boolean nextRecord = false;
        int i = 1;

        while (!nextRecord) {
            if (refRequest.containsKey(i)) 
                i++;
            else {
                requestRecordNumber = i;
                return requestRecordNumber;
            }
        }

        return -1;
    }

    /**
     * Returns the score and round for the current game
     * 
     * @param name
     * @param msg
     */
    public void getStatus(String name, String msg) {
        if (m_curRound != null) 
            ba.sendPrivateMessage(name, "Currently in round " + m_curRound.getRoundNum() + " with " + m_fcTeam1Name + "(" + fnTeam1Score + ") vs " + m_fcTeam2Name + "(" + fnTeam2Score + ")");        
    }

    /**
     * Removes a penalty on a player.
     * 
     * @param name
     * @param msg
     */
    public void doRemovePenalty(String name, String msg) {
        twhtTeam team;
        twhtPlayer playerA;
        Player p;
        int freq;

        team = getPlayerTeam(msg);

        if (team != null) {
            playerA = team.searchPlayer(msg);
            freq = team.getFrequency();

            if (playerA.getPenalty() > 0) {
                ba.sendArenaMessage("Penalty has expired for: " + playerA.getPlayerName(), 2);

                if (playerA.getPreviousState() == 1) {
                    p = ba.getPlayer(playerA.getPlayerName());

                    if (p != null) {
                        
                        if (freq == 0)
                            ba.warpTo(playerA.getPlayerName(), 508, 449);
                        if (freq == 1)
                            ba.warpTo(playerA.getPlayerName(), 515, 449);

                        playerA.resetPenalty();
                        playerA.returnedToGame();
                    }
                } else if (playerA.getPreviousState() == 3 || playerA.getPlayerState() == 3 || playerA.getPlayerState() == 2) 
                    playerA.resetPenalty();                
            } else 
                ba.sendPrivateMessage(name, "Player does not have a penalty against them.");
        } else 
            ba.sendPrivateMessage(name, "Player not found on either team.");
    }

    /**
     * Warps player to the penalty box.
     */
    public void doWarpPenalty(String name, String msg) {
        twhtTeam team;
        twhtPlayer playerA;
        Player p;
        int freq;

        team = getPlayerTeam(msg);

        if (team != null) {
            playerA = team.searchPlayer(msg);
            freq = team.getFrequency();
            p = ba.getPlayer(playerA.getPlayerName());

            if (p != null) {
                if (freq == 0)
                    ba.warpTo(playerA.getPlayerName(), 500, 442);
                if (freq == 1)
                    ba.warpTo(playerA.getPlayerName(), 520, 442);
            }
        } else 
            ba.sendPrivateMessage(name, "Player not found on either team.");        
    }
    
    /**
     * Clears the penalties for both teams.
     */
    public void doClearPenalties() {
        m_team1.clearTeamPenalties();
        m_team2.clearTeamPenalties();
    }
    
    /**
     * This method is ran if the end of round reports that the game has ended.
     * 
     * @param winner
     * @param loser
     * @param winScore
     * @param loseScore
     */
    public void doEndGame(String winner, String loser, int winScore, int loseScore) {
        ba.sendArenaMessage(" ------- GAME OVER ------- ", 5);
        ba.sendArenaMessage(winner + " vs. " + loser + ": " + winScore + " - " + loseScore);
        ba.sendArenaMessage(winner + " wins this game!");
        resetVariables();
        
        statsDelay = new TimerTask() {
            @Override
            public void run() {
                m_team1.getTeamStats();
                m_team2.getTeamStats();
                twht.endGame();
            }
        }; ba.scheduleTask(statsDelay, Tools.TimeInMillis.SECOND * 20);        
    }

    /**
     * Allows a player to join their team's frequency when they are in spec.
     * 
     * @param name
     * @param msg
     */
    public void doMyfreq(String name, String msg) {
        twhtTeam team = null;
        int frequency;

        if (m_team1.isPlayer(name) || m_team1.isCaptain(name)) 
            team = m_team1;
        else if (m_team2.isPlayer(name) || m_team2.isCaptain(name)) 
            team = m_team2;        

        if (team != null) {
            frequency = team.getFrequency();
            ba.setFreq(name, frequency);
        }
    }

    /**
     * Executes the add command either from the list or forcefully by the ref.
     * 
     * @param teamName
     * @param player
     * @param shipType
     */
    public void doAddPlayer(String teamName, String player, int shipType) {
        Player p;
        p = ba.getPlayer(player);

        if (p != null) {
            if (m_team1.isPlayer(player) || m_team2.isPlayer(player)) {
                ba.sendPrivateMessage(m_fcRefName, "Cannot add player. Player is already in for one of the teams.");
                return;
            } else {
                if (teamName == m_fcTeam1Name)
                    m_team1.addPlayer(player, shipType);
                if (teamName == m_fcTeam2Name)
                    m_team2.addPlayer(player, shipType);
            }
        } else 
            ba.sendPrivateMessage(m_fcRefName, "Cannot add player. Player is no longer here.");        
    }

    /**
     * Executes the sub command from the lists of requests.
     * 
     * @param teamName
     * @param playerA
     * @param playerB
     * @param shipType
     */
    public void doSubPlayer(String teamName, String playerA, String playerB, int shipType) {
        Player p;
        p = ba.getPlayer(playerB);
        twhtTeam t = null;

        if (m_fcTeam1Name == teamName)
            t = m_team1;
        if (m_fcTeam2Name == teamName)
            t = m_team2;

        if (p != null) {
            if (m_team1.isPlayer(playerB) || m_team2.isPlayer(playerB)) {
                ba.sendPrivateMessage(m_fcRefName, "Cannot sub player in. Already on one of the teams.");
                return;
            } else if (t.searchPlayer(playerA) == null || t.getPlayerState(playerA) == 2) {
                ba.sendPrivateMessage(m_fcRefName, "Cannot sub player out. Player is no longer on the team.");
                return;
            } else 
                t.subPlayer(playerA, playerB, shipType);            
        } else 
            ba.sendPrivateMessage(m_fcRefName, "Cannot sub player. Player is no longer here.");        
    }

    /**
     * Executes the switch command from the list of requests.
     * 
     * @param teamName
     * @param playerA
     * @param playerB
     */
    public void doSwitchPlayer(String teamName, String playerA, String playerB) {
        Player pA;
        Player pB;
        twhtTeam t = null;

        pA = ba.getPlayer(playerA);
        pB = ba.getPlayer(playerB);

        if (m_fcTeam1Name == teamName)
            t = m_team1;
        if (m_fcTeam2Name == teamName)
            t = m_team2;

        if (pA != null && pB != null) {
            if (t.searchPlayer(playerA) == null || t.searchPlayer(playerB) == null) {
                ba.sendPrivateMessage(m_fcRefName, "Cannot switch players. One of the players is no longer on the team.");
                return;
            } else if ((t.getPlayerState(playerA) == 3 || t.getPlayerState(playerA) == 2) || (t.getPlayerState(playerB) == 3 || t.getPlayerState(playerB) == 2)) {
                ba.sendPrivateMessage(m_fcRefName, "Cannot switch players. One of the players is lagged out or subbed out.");
                return;
            } else 
                t.switchPlayer(playerA, playerB);            
        } else 
            ba.sendPrivateMessage(m_fcRefName, "Cannot switch players. One of the players is lagged out.");        
    }

    /**
     * Executes the change command either from the list or forcefully by the ref.
     * 
     * @param teamName
     * @param player
     * @param shipType
     */
    public void doChangePlayer(String teamName, String player, int shipType) {
        Player p;
        twhtTeam t = null;

        p = ba.getPlayer(player);

        if (m_fcTeam1Name == teamName)
            t = m_team1;
        if (m_fcTeam2Name == teamName)
            t = m_team2;

        if (p != null) {
            if (t.searchPlayer(player) == null) {
                ba.sendPrivateMessage(m_fcRefName, "Cannot change player. Player is no longer on the team.");
                return;
            } else if (t.getPlayerState(player) != 1 && t.getPlayerState(player) != 4) {
                ba.sendPrivateMessage(m_fcRefName, "Cannot change player. Player is lagged out or substitued.");
                return;
            } else 
                t.changePlayer(player, shipType);            
        } else 
            ba.sendPrivateMessage(m_fcRefName, "Cannot Change player. Player is no longer in the game.");        
    }

    /**
     * Executes the remove command either from the list or forcefully by the ref.
     * 
     * @param teamName
     * @param player
     */
    public void doRemovePlayer(String teamName, String player) {
        twhtTeam t = null;

        if (m_fcTeam1Name == teamName)
            t = m_team1;
        if (m_fcTeam2Name == teamName)
            t = m_team2;

        if (t.searchPlayer(player) == null) {
            ba.sendPrivateMessage(m_fcRefName, "Player is already removed from the team.");
            return;
        } else 
            t.removePlayer(player);        
    }

    /**
     * Executes the timeout request from the list. (Pauses the clock).
     * 
     * @param teamName
     */
    public void doTimeout(String teamName) {
        if (m_curRound != null)
            ba.sendArenaMessage(teamName + " has called a timeout.", 2);
            m_curRound.doPause();
    }

    /**
     * Executes the lagout command from the list
     * 
     * @param teamName
     * @param playerName
     */
    public void doLagOut(String teamName, String playerName) {
        Player p;
        p = ba.getPlayer(playerName);

        if (p != null) {
            if (teamName == m_fcTeam1Name)
                m_team1.lagOut(playerName);
            if (teamName == m_fcTeam2Name)
                m_team2.lagOut(playerName);
        } else 
            ba.sendPrivateMessage(m_fcRefName, "Cannot !lagout player. Player is no longer in the arena.");        
    }

    /**
     * Allows the captain of a team to set a player to automatically be ported
     * to the faceoff circle when a faceoff is executed.
     * 
     * @param name
     * @param msg
     */
    public void doAddCenter(String name, String msg) {
        twhtTeam team;
        twhtPlayer pA;

        if (m_team1.isCaptain(name)) 
            team = m_team1;
        else if (m_team2.isCaptain(name)) 
            team = m_team2;
        else 
            return;        

        pA = team.searchPlayer(msg);

        if (pA != null && pA.getPlayerState() == 1 && !pA.getIsGoalie()) {
            team.setCenter(pA.getPlayerName());
            ba.sendPrivateMessage(name, pA.getPlayerName() + " has been set as your center.");
        } else {
            ba.sendPrivateMessage(name, "Player is unable to be set as center.");
            return;
        }
    }

    /**
     * Allows the ref to cancel the intermission if both captains agree to.
     */
    public void doCancelIntermission() {
        if (isIntermission) {
            ba.cancelTask(intermission);
            isIntermission = false;
            ba.sendArenaMessage("Round intermission has been canceled by the Referee.");
            setupRound(m_curRound.getRoundNum() + 1);
        }
    }
    
    /**
     * This method will add a judge to the list
     * 
     * @param name
     * @param msg
     */
    public void doAddJudge(String name, String msg) {
        String p = ba.getFuzzyPlayerName(msg);
        if (p != null) {
            if (m_judge.contains(p)) 
                ba.sendPrivateMessage(name, "Player is already a judge");
            else {
                m_judge.add(p);
                ba.sendPrivateMessage(name, "Judge Added: " + p);
                ba.setFreq(p,2);
            }
        } else 
            ba.sendPrivateMessage(name, "Player not found.");        
    }

    /**
     * This method will remove a judge from the list
     * 
     * @param name
     * @param msg
     */
    public void doRemoveJudge(String name, String msg) {
        String p = ba.getFuzzyPlayerName(msg);
        if (p != null) {
            if (m_judge.contains(p)) {
                m_judge.remove(p);
                ba.sendPrivateMessage(name, "Judge Removed: " + p);
                ba.setFreq(p, SPEC_FREQ);
            } else 
                ba.sendPrivateMessage(name, "Player is not a judge.");            
        } else 
            ba.sendPrivateMessage(name, "Player not found.");        
    }

    /**
     *Initiates the review period after every goal scored 
     */
    public void doReview() {
        voteInProgress = true;
        
        goalDelay = new TimerTask() {
            @Override
            public void run() {
                m_curRound.doStopTimer();
                m_curRound.doRemoveBall();
            }
        }; ba.scheduleTask(goalDelay, 2 * Tools.TimeInMillis.SECOND);
        
        ba.sendTeamMessage("Review for the last goal has started. Please private message me your vote.");
        ba.sendTeamMessage("Commands: !cl (Clean), !lag (Lag), !gk (Goalie Kill), !og (Own Goal)");
    }

    /**
     * If a captain uses !ready then it will announce that they are ready in an arena message
     * If a referee uses !ready then it will begin the round. 
     * 
     * @param name
     */
    public void doReady(String name) {
        if (m_curRound != null && (m_curRound.getRoundState() == 0 || m_curRound.getRoundState() == 2)) {
            twhtTeam team;

            if (m_team1.isCaptain(name)) {
                team = m_team1;
                ba.sendArenaMessage("" + team.getTeamName() + " is ready to begin.", 2);
            }
            
            if (m_team2.isCaptain(name)) {
                team = m_team2;
                ba.sendArenaMessage("" + team.getTeamName() + " is ready to begin.", 2);
            }
            
            if (name.equals(m_fcRefName)) 
                m_curRound.doReady();            
        }
    }
    
    /**
     * Executes the decision that the referee has made on the goal in question
     * 
     * @param msg
     */
    public void doGoal(String msg) {
        if (voteInProgress) {
            twhtPlayer pA;
            twhtTeam tA;
            
            if (msg.equals("cl")) {
                ba.sendArenaMessage("Goal was considered clean.", 2);
                ba.sendArenaMessage(getPlayerTeam(goalScorer).getTeamName() + "'s Goal by: " + goalScorer);
                
                if (assistOne != null && assistTwo == null) {             
                    ba.sendArenaMessage("Assist: " + assistOne);
                    doPlayerStats(assistOne, 2);
                } else if (assistOne != null && assistTwo != null) {
                    ba.sendArenaMessage("Assist: " + assistOne + " " + assistTwo);
                    doPlayerStats(assistOne, 2);
                    doPlayerStats(assistTwo, 2);
                }
                
                if (m_team1.isPlayer(goalScorer)) {
                    fnTeam1Score++;
                    m_team1.setScoreFor();
                    m_team2.setScoreAgainst();    
                    
                    if (m_team2.getGoalie() != null)
                        doPlayerStats(m_team2.getGoalie(), 10);
                } else if (m_team2.isPlayer(goalScorer)) {
                    fnTeam2Score++;
                    m_team2.setScoreFor();
                    m_team1.setScoreAgainst();
                    
                    if (m_team1.getGoalie() != null)
                        doPlayerStats(m_team1.getGoalie(), 10);
                }                
                doPlayerStats(goalScorer, 1);
                doPlayerStats(goalScorer, 5);
                
                if (m_curRound.getRoundNum() == 3)
                    reportEndOfRound(3);
                
            } else if (msg.equals("lag")) 
                ba.sendArenaMessage("Goal was considered lag and will be voided.", 2);
            else if (msg.equals("cr")) 
                ba.sendArenaMessage("Goal was considered crease and will be voided.", 2);
            else if (msg.equals("gk")) 
                ba.sendArenaMessage("Goal was considered a goalie kill and will be void.", 2);
            else if (msg.equals("og")) {
                ba.sendArenaMessage("Goal was considered an own goal and the other team will be rewarded a goal.", 2);
                
                if (m_team1.isPlayer(goalScorer)) {
                    fnTeam2Score++;
                    m_team2.setScoreFor();
                    m_team1.setScoreAgainst();
                } else if (m_team2.isPlayer(goalScorer)) {                    
                    fnTeam1Score++;
                    m_team1.setScoreFor();
                    m_team2.setScoreAgainst();
                }
                
                if (m_curRound.getRoundNum() == 3)
                    reportEndOfRound(3);
            }
            m_curRound.doUpdateScoreBoard();
            ba.sendArenaMessage("Score: " + m_fcTeam1Name + " " + fnTeam1Score + " - " + m_fcTeam2Name + " " + fnTeam2Score);
            m_curRound.m_fnRoundState = 3;            
            voteInProgress = false;
            assistOne = null;
            assistTwo = null;
            
            if (isPShot)
                isPShot = false;
        }        
    }
    
    /**
     * Figures out the team and player for the stat keeping.
     * 
     * @param name
     * @param statType
     */
    public void doPlayerStats(String name, int statType) {
        twhtPlayer pA = null;
        twhtTeam tA = null;
        
        tA = getPlayerTeam(name);
        
        if (tA != null)
        pA = tA.searchPlayer(name);
        
        if (pA != null)            
        pA.doStats(statType);
    }
    
    /**
     * Executes a forfiet when called by the referee. Assigns one team 10 goals and the other 0 goals.
     * 
     * @param name
     * @param msg
     */
    public void doForfiet(String name, String msg) {
        int teamNum;
        
        try {
            teamNum = Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            return;
        }
        if (teamNum == 1) {
            ba.sendArenaMessage("The Referee has declared " + m_fcTeam2Name + " forfeit winners, therfore win the match 10 - 0.", 5);
            fnTeam1Score = 0;
            fnTeam2Score = 10;
            reportEndOfRound(2);
        } else if (teamNum == 2) {
            ba.sendArenaMessage("The Referee has declared " + m_fcTeam1Name + " forfeit winners, therfore win the match 10 - 0.", 5);
            fnTeam1Score = 10;
            fnTeam2Score = 0;
            reportEndOfRound(2);
        }
    }

    /**
     * Initiates a penalty shot
     * 
     * @param name
     */
    public void doPenShot(String name) {
        if(m_curRound != null) {
            ba.sendArenaMessage("A Penalty shot has been initiated.", 2);
            m_team1.setPenaltyShotWarp();
            m_team2.setPenaltyShotWarp();
            m_curRound.doStopTimer();
            m_curRound.doRemoveBall();
            m_curRound.m_fnRoundState = 1;
            isPShot = true;
        }
    }
         
    /**
     * Adjusts the score for a team.
     * 
     * @param name
     * @param msg
     */
    public void doSetScore(String name, String msg) {
        int team = 0;
        int score = 0;
        String[] splitmsg;

        if (msg.contains(":")) {
            splitmsg = msg.split(":");

            if (splitmsg.length == 2) {
                try {
                    team = Integer.parseInt(splitmsg[0]);
                    score = Integer.parseInt(splitmsg[1]);
                } catch (NumberFormatException e) {
                    return;
                }
                
                if (team <= 0 || team >= 3) {
                    ba.sendPrivateMessage(name, "Invalid format. Please use !setscore <team#>:<score>.");
                    return;
                }
    
                if (team == 1) { 
                    m_team1.m_fnTeamScore = score;
                    fnTeam1Score = score;
                    ba.sendArenaMessage("Team One's score has been changed to " + score, 2);
                } else if (team == 2) {
                    m_team2.m_fnTeamScore = score;
                    fnTeam2Score = score;
                    ba.sendArenaMessage("Team Two's score has been changed to " + score, 2);
                }
                
                m_curRound.doUpdateScoreBoard();
            } else
                ba.sendPrivateMessage(name, "Invalid format. Please use !setscore ##:##");
        }
    }
    
    /**
     * Sets a player that is taking the penalty shot
     * 
     * @param name
     * @param msg
     */
    public void setPenShot(String name, String msg) {
        if(m_curRound != null && isPShot) {
           String pA;
           twhtTeam t;
           
           t = getPlayerTeam(msg);
           
           if (t != null) 
               t.setPlayerShot(msg);
           else 
               ba.sendPrivateMessage(name, "Player cannot be found");
        } else
            ba.sendPrivateMessage(name, "You must initiate the penalty shot before you can add shooters to it.");
    }
    
    /**
     * Cancels a penalty shot 
     * 
     * @param name
     */
    public void cancelPenShot(String name) {
        if(m_curRound != null && isPShot) {
            ba.sendArenaMessage("A Penalty shot has been canceled.", 2);
            m_team1.setFreqAndSide();
            m_team2.setFreqAndSide();
            m_curRound.m_fnRoundState = 3;
            isPShot = false;
        } else
            ba.sendPrivateMessage(name, "There is no penalty shot in progress to cancel.");
    }
    
    /**
     * Starts the penalty shot and brings the puck to the center
     * 
     * @param name
     */
    public void startPenShot(String name) {
        if (m_curRound != null && isPShot) {
            m_curRound.doGetBall(8192, 8192);
            
            penShotDelay = new TimerTask() {
                @Override
                public void run() {
                    m_curRound.doDropBall();
                    ba.sendArenaMessage("Go! Go! Go!", Tools.Sound.VICTORY_BELL);                        
                }
            }; ba.scheduleTask(penShotDelay, Tools.TimeInMillis.SECOND * 2);
        } else 
            ba.sendPrivateMessage(name, "You must initiate the penalty shot before it can start.");
    }
    
    /**
     * This method checks if a lagout request is legal and if so, it will send the request to the referee.
     * 
     * @param name
     * @param msg
     */
    public void reqLagoutPlayer(String name, String msg) {
        twhtTeam team;
        twhtPlayer pA;

        team = getPlayerTeam(name);

        if (team != null) {
            pA = team.searchPlayer(name);

            if (pA.getPlayerState() == 3) 
                refRequest.put(getNextRecordNumber(), new RefRequest(getNextRecordNumber(), m_curRound.getRoundNum(), 6, name, team.getTeamName(), name));
             else 
                ba.sendPrivateMessage(name, "You are not lagged out.");            
        }
    }

    /**
     * This method checks if a add player request is legal and if so, it will send the request to the referee.
     * 
     * @param name
     * @param msg
     */
    public void reqAddPlayer(String name, String msg) {
        String teamName;
        String playerName;
        String[] splitCmd;
        int shipNum;

        if (m_team1.isCaptain(name)) 
            teamName = m_fcTeam1Name;
        else if (m_team2.isCaptain(name)) 
            teamName = m_fcTeam2Name;
        else 
            return;        

        if (msg.contains(":")) {
            splitCmd = msg.split(":");
            
            if (splitCmd.length == 2) {                
                try {
                    shipNum = Integer.parseInt(splitCmd[1]);
                } catch (NumberFormatException e) {
                    return;
                }

                playerName = ba.getFuzzyPlayerName(splitCmd[0]);
                
                if (ba.getOperatorList().isBotExact(playerName)) {
                    ba.sendPrivateMessage(name, "Error: Pick again, bots are not allowed to play.");
                    return;
                }
                
                if (playerName == null || shipNum > 8 || shipNum < 1) {
                    ba.sendPrivateMessage(name, "Player not found or Invalid Ship. Please try again.");
                    return;
                }
                
                if ((m_team1.isPlayer(playerName) && teamName == m_fcTeam2Name) || (m_team2.isPlayer(playerName) && teamName == m_fcTeam1Name)) {
                    ba.sendPrivateMessage(name, "Player is already on the other team");
                    return;
                } else if ((m_team1.isPlayer(playerName) && teamName == m_fcTeam1Name) || (m_team2.isPlayer(playerName) && teamName == m_fcTeam2Name)) {
                    ba.sendPrivateMessage(name, "Player is already on the team.");
                    return;
                }
                refRequest.put(getNextRecordNumber(), new RefRequest(getNextRecordNumber(), m_curRound.getRoundNum(), 0, name, teamName, playerName + ":" + shipNum));
                ba.sendPrivateMessage(name, "Request to add: " + playerName + " in ship " + shipNum + " has been sent to the referee.");
            }
        } else 
            ba.sendPrivateMessage(name, "Invalid format. Please use !add <name>:ship#");        
    }

    /**
     * This method checks if a remove is legal and if so, it will send the request to the referee.
     * 
     * @param name
     * @param msg
     */
    public void reqRemovePlayer(String name, String msg) {
        twhtTeam team;
        twhtPlayer playerA;

        if (m_team1.isCaptain(name)) 
            team = m_team1;
        else if (m_team2.isCaptain(name)) 
            team = m_team2;
        else 
            return;        

        playerA = team.searchPlayer(msg);

        if (playerA != null) {
            refRequest.put(getNextRecordNumber(), new RefRequest(getNextRecordNumber(), m_curRound.getRoundNum(), 4, name, team.getTeamName(), playerA.getPlayerName()));
            ba.sendPrivateMessage(name, "Request to remove player: " + playerA.getPlayerName() + " has been sent to the referee.");
        } else {
            ba.sendPrivateMessage(name, "Player trying to be removed cannot be found on your team.");
            return;
        }
    }

    /**
     * This method checks if a switch is legal and if so, it will send the request to the referee.
     * 
     * @param name
     * @param msg
     */
    public void reqSwitchPlayer(String name, String msg) {
        twhtTeam team;
        twhtPlayer playerA;
        twhtPlayer playerB;

        String[] splitCmd;

        if (m_team1.isCaptain(name)) 
            team = m_team1;
        else if (m_team2.isCaptain(name)) 
            team = m_team2;
        else 
            return;        

        if (msg.contains(":")) {
            splitCmd = msg.split(":");

            if (splitCmd.length == 2) {
                String playerAName = splitCmd[0];
                String playerBName = splitCmd[1];

                playerA = team.searchPlayer(playerAName);
                playerB = team.searchPlayer(playerBName);

                if (playerA != null && playerB != null) {
                    
                    if ((playerA.getPlayerState() == 1 || playerA.getPlayerState() == 4) && (playerB.getPlayerState() == 1 || playerB.getPlayerState() == 4)) {
                        refRequest.put(getNextRecordNumber(), new RefRequest(getNextRecordNumber(), m_curRound.getRoundNum(), 2, name, team.getTeamName(), playerA.getPlayerName() + ":"
                                       + playerB.getPlayerName()));
                        ba.sendPrivateMessage(name, "Request to switch: " + playerA.getPlayerName() + " with " + playerB.getPlayerName() + "has been sent to the referee.");
                    }
                } else 
                    ba.sendPrivateMessage(name, "One of the players cannot be found or cannot be switched at this moment.");                
            } else 
                ba.sendPrivateMessage(name, "Invalid format. Please use !switch <name>:<name>");            
        }
    }

    /**
     * This method checks if a substitution is legal and if so, it will send the request to the referee.
     * 
     * @param name
     * @param msg
     */
    public void reqSubPlayer(String name, String msg) {
        twhtTeam team;
        twhtTeam team2;
        twhtPlayer playerA;
        String[] splitCmd;
        int shipNum;

        if (m_team1.isCaptain(name)) 
            team = m_team1;
        else if (m_team2.isCaptain(name)) 
            team = m_team2;
        else 
            return;        

        if (msg.contains(":")) {
            splitCmd = msg.split(":");
            if (splitCmd.length == 3) {
                try {
                    shipNum = Integer.parseInt(splitCmd[2]);
                } catch (NumberFormatException e) {
                    return;
                }

                String playerAName = splitCmd[0];
                String playerBName;
                
                playerA = team.searchPlayer(playerAName);

                if (playerA != null) {
                    if (playerA.getPlayerState() == 1 || playerA.getPlayerState() == 3) {
                        playerBName = ba.getFuzzyPlayerName(splitCmd[1]);
                        team2 = getPlayerTeam(playerBName);
                        
                        if (team2 == team)
                            team = null;
                    
                        if (playerBName != null && team2 == null) {
                            if (ba.getOperatorList().isBotExact(playerBName)) {
                                ba.sendPrivateMessage(name, "Error: Pick again, bots are not allowed to play.");
                                return;
                            }
                    
                            if (shipNum <= 8 && shipNum >= 1) {
                                refRequest.put(getNextRecordNumber(), new RefRequest(getNextRecordNumber(), m_curRound.m_roundNum, 1, name, team.getTeamName(), playerA.getPlayerName() + ":"
                                               + playerBName + ":" + shipNum));
                                ba.sendPrivateMessage(name, "Request to sub: " + playerA.getPlayerName() + " with " + playerBName + " has been sent to the referee.");
                            } else {
                                ba.sendPrivateMessage(name, "Invalid ship number please try again..");
                                return;
                            }
                        } else {
                            ba.sendPrivateMessage(name, "Player trying to be subbed in cannot be found or is already playing.");
                            return;
                        }
                    }
                } else 
                    ba.sendPrivateMessage(name, "Cannot sub for player, player is not on your team.");                
            } else 
                ba.sendPrivateMessage(name, "Invalid format. Please use !sub <name>:<name>:ship#");            
        }
    }

    /**
     * This method checks if a ship change is legal and if so, it will send the request to the referee.
     * 
     * @param name
     * @param msg
     */
    public void reqChangePlayer(String name, String msg) {
        twhtTeam team;
        twhtPlayer playerA;
        String[] splitCmd;
        int shipNum;

        if (m_team1.isCaptain(name))
            team = m_team1;
        else if (m_team2.isCaptain(name)) 
            team = m_team2;
        else 
            return;        

        if (msg.contains(":")) {
            splitCmd = msg.split(":");

            if (splitCmd.length == 2) {
                try {
                    shipNum = Integer.parseInt(splitCmd[1]);
                } catch (NumberFormatException e) {
                    return;
                }

                String playerAName = splitCmd[0];
                playerA = team.searchPlayer(playerAName);

                if (playerA != null && (shipNum <= 8 && shipNum >= 1)) {
                    if (playerA.getPlayerState() == 1 || playerA.getPlayerState() == 4) {
                        refRequest.put(getNextRecordNumber(), new RefRequest(getNextRecordNumber(), m_curRound.m_roundNum, 3, name, team.getTeamName(), playerA.getPlayerName() + ":" + shipNum));
                        ba.sendPrivateMessage(name, "Request to change: " + playerA.getPlayerName() + " to " + shipNum + " has been sent to the referee.");
                    }
                } else {
                    ba.sendPrivateMessage(name, "PLayer cannot be changed, please try again.");
                    return;
                }
            } else 
                ba.sendPrivateMessage(name, "Invalid format. Please use !change <name>:ship#");            
        }
    }
    
    /**
     * This method handles the timeout when requested by a team.
     * 
     * @param name
     * @param msg
     */
    public void reqTimeOut(String name, String msg) {
        String teamName;
        
        if (m_team1.isCaptain(name)) {
            teamName = m_fcTeam1Name;
            refRequest.put(getNextRecordNumber(), new RefRequest(getNextRecordNumber(), m_curRound.getRoundNum(), 5, name, teamName, name));
        } else if (m_team2.isCaptain(name)) {
            teamName = m_fcTeam2Name;
            refRequest.put(getNextRecordNumber(), new RefRequest(getNextRecordNumber(), m_curRound.getRoundNum(), 5, name, teamName, name));
        } else 
            return;        
    }

    /**
     * This is the method where the bot puts in a request to assign a penalty.
     * 
     * @param teamName
     * @param penalty
     * @param penLength
     * @param playerName
     */
    public void reqPenalty(String penalty, int penLength, String playerName) {
        twhtTeam t = null;
        twhtPlayer p = null;
        
        t = getPlayerTeam(playerName);
        
        if (t != null)
            p = t.searchPlayer(playerName);
        
        refRequest.put(getNextRecordNumber(), new RefRequest(getNextRecordNumber(),m_curRound.getRoundNum(), 7, m_fcRefName, t.getTeamName(), p.getPlayerName() + ":" + penLength + ":" + penalty));
    }
    
    /**
     * Returns the request list to the referee. Possible parameters are all, denied, and accepted
     * Default parameter is to show all open requests.
     * 
     * @param name
     * @param msg
     */
    public void getRequestList(String name, String msg) {
        if (msg.equals("all")) {
            for (RefRequest i : refRequest.values()) {
                i.pmRequestRef();
            }
        } else if (msg.equals("denied")) {
            for (RefRequest i : refRequest.values()) {
                if (i.getRequestState() == 2)
                    i.pmRequestRef();
            }
        } else if (msg.equals("accepted")) {
            for (RefRequest i : refRequest.values()) {
                if (i.getRequestState() == 1)
                    i.pmRequestRef();
            }
        } else {
            for (RefRequest i : refRequest.values()) {
                if (i.getRequestState() == 0)
                    i.pmRequestRef();
            }
        }
    }
    
    /**
     * Accepts a request on the list and executes it.
     * 
     * @param name
     * @param msg
     */
    public void acceptRequest(String name, String msg) {
        RefRequest number;
        int recordNumber;

        try {
            recordNumber = Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            return;
        }

        if (recordNumber > 0 && recordNumber < 500) {
            if (refRequest.containsKey(recordNumber)) {
                number = refRequest.get(recordNumber);
                number.executeRequest();
            }
        } else 
            ba.sendPrivateMessage(name, "Please only use valid numbers.");        
    }

    /**
     * Re-opens a request if it was previously closed.
     * 
     * @param name
     * @param msg
     */
    public void openRequest(String name, String msg) {
        RefRequest number;
        int recordNumber;

        try {
            recordNumber = Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            return;
        }

        if (recordNumber > 0 && recordNumber < 500) {
            if (refRequest.containsKey(recordNumber)) {
                number = refRequest.get(recordNumber);
                number.openRequest();
            }
        } else 
            ba.sendPrivateMessage(name, "Please only use valid numbers.");
    }

    /**
     * Denies a request. Removes it from the list and does not execute the action.
     * 
     * @param name
     * @param msg
     */
    public void denyRequest(String name, String msg) {
        RefRequest number;
        int recordNumber;

        try {
            recordNumber = Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            return;
        }

        if (recordNumber > 0 && recordNumber < 500) {
            if (refRequest.containsKey(recordNumber)) {
                number = refRequest.get(recordNumber);
                number.denyRequest();
            }
        } else 
            ba.sendPrivateMessage(name, "Please only use valid numbers.");        
    }
    
    /**
     * Resets the lists and timers for the game.
     */
    public void resetVariables() {
        ba.cancelTasks();
        m_judge.clear();
        m_penalties.clear();
        m_officials.clear();
        refRequest.clear();
        m_rounds.clear();
    }

    /**
     * The class that is used for the request list that the referee uses to execute commands or actions
     * that need to be reviewed before executed.
     * 
     * @author Ian
     *
     */
    public class RefRequest {

        /*Request Types:
         * 0 - Add
         * 1 - Sub 
         * 2 - Switch
         * 3 - Change
         * 4 - Remove
         * 5 - Timeout
         * 6 - Lagout
         * 7 - Penalty
         */
        int reqType;

        /*Request State: 
         * 0 - Open
         * 1 - Accepted
         * 2 - Denied
         */
        int requestState;
        int callNumber;
        int roundNumber;
        int shipType;
        int penaltyLen;
        String roundTime;
        String requester;
        String teamName;
        String reqString;
        String playerA = "";
        String playerB = "";
        String penType = "";

        //Class constructor
        public RefRequest(int callNum, int roundNum, int requestType, String name, String team, String RequestString) {
            this.callNumber = callNum;
            this.roundNumber = roundNum;
            this.reqType = requestType;
            this.roundTime = m_curRound.getStringTime();
            this.teamName = team;
            this.requester = name;
            this.reqString = RequestString;
            this.requestState = 0;
            breakDownString();
        }

        /**
         * Breaks down the request string that they are sent to the class in.
         */
        private void breakDownString() {
            String[] splitCmd;
            splitCmd = reqString.split(":");

            if (reqType == 0) {
                playerA = splitCmd[0];
                shipType = Integer.parseInt(splitCmd[1]);
            } else if (reqType == 1) {
                playerA = splitCmd[0];
                playerB = splitCmd[1];
                shipType = Integer.parseInt(splitCmd[2]);
            } else if (reqType == 2) {
                playerA = splitCmd[0];
                playerB = splitCmd[1];
            } else if (reqType == 3) {
                playerA = splitCmd[0];
                shipType = Integer.parseInt(splitCmd[1]);
            } else if (reqType == 4) 
                playerA = reqString;
             else if (reqType == 6) 
                playerA = requester;
             else if (reqType == 7) {
                playerA = splitCmd[0];
                penaltyLen = Integer.parseInt(splitCmd[1]);
                penType = splitCmd[2];
             }
            
            ba.sendPrivateMessage(m_fcRefName, "New Request Recieved:");
            pmRequestRef();
        }

        /**
         * Sets the different formats for the different request types when the ref views it.
         */
        private void pmRequestRef() {
            if (reqType == 0) 
                ba.sendPrivateMessage(m_fcRefName, "#" + callNumber + " - Round " + roundNumber + " " + roundTime + " - " + teamName + " - ADD: " + playerA + " in " + shipType);
            else if (reqType == 1) 
                ba.sendPrivateMessage(m_fcRefName, "#" + callNumber + " - Round " + roundNumber + " " + roundTime + " - " + teamName + " - SUB: " + playerA + " for " + playerB + " in " + shipType);
            else if (reqType == 2) 
                ba.sendPrivateMessage(m_fcRefName, "#" + callNumber + " - Round " + roundNumber + " " + roundTime + " - " + teamName + " - SWITCH: " + playerA + " for " + playerB);
            else if (reqType == 3) 
                ba.sendPrivateMessage(m_fcRefName, "#" + callNumber + " - Round " + roundNumber + " " + roundTime + " - " + teamName + " - CHANGE: " + playerA + " to " + shipType);
            else if (reqType == 4) 
                ba.sendPrivateMessage(m_fcRefName, "#" + callNumber + " - Round " + roundNumber + " " + roundTime + " - " + teamName + " - REMOVE: " + playerA);
            else if (reqType == 5) 
                ba.sendPrivateMessage(m_fcRefName, "#" + callNumber + " - Round " + roundNumber + " " + roundTime + " - " + teamName + " - TIMEOUT ");
            else if (reqType == 6) 
                ba.sendPrivateMessage(m_fcRefName, "#" + callNumber + " - Round " + roundNumber + " " + roundTime + " - " + teamName + " - LAGOUT " + requester);
            else if (reqType == 7)
                ba.sendPrivateMessage(m_fcRefName, "#" + callNumber + " - Round " + roundNumber + " " + roundTime + " - " + teamName + " - RECOMMENDED PENALTY " + penType + " ON " +  playerA + " FOR " + penaltyLen + " seconds");
        }

        /**
         * Executes a request when the referee accepts it.
         */
        private void executeRequest() {
            if (this.requestState != 1) {
                this.requestState = 1;
                ba.sendPrivateMessage(m_fcRefName, " Request #" + callNumber + " has been accepted.");
                
                if (reqType == 0)
                    doAddPlayer(teamName, playerA, shipType);
                else if (reqType == 1)
                    doSubPlayer(teamName, playerA, playerB, shipType);
                else if (reqType == 2)
                    doSwitchPlayer(teamName, playerA, playerB);
                else if (reqType == 3)
                    doChangePlayer(teamName, playerA, shipType);
                else if (reqType == 4)
                    doRemovePlayer(teamName, playerA);
                else if (reqType == 5)
                    doTimeout(teamName);
                else if (reqType == 6) 
                    doLagOut(teamName, requester);      
                else if (reqType == 7)
                    setPenalty(m_fcRefName, "" + playerA + ":" + penaltyLen + ":" + penType);
            } else 
                ba.sendPrivateMessage(m_fcRefName, "Request has already been executed once and cannot be executed again.");            
        }

        /**
         * Confirms that the request has been denied and sets the state for it
         */
        private void denyRequest() {
            ba.sendPrivateMessage(m_fcRefName, " Request #" + callNumber + " has been denied.");
            this.requestState = 2;
        }
        
        /**
         * Confirms and sets the state for a request that has been re-opened 
         */
        private void openRequest() {
            ba.sendPrivateMessage(m_fcRefName, " Request #" + callNumber + " has been opened.");
            this.requestState = 0;
        }

        /**
         * Returns the request state
         * 
         * @return
         */
        private Integer getRequestState() {
            return requestState;
        }
    }
}
