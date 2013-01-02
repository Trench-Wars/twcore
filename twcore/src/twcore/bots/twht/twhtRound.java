/**
 * 
 */
package twcore.bots.twht;

import java.util.Stack;
import java.util.TimerTask;
import java.util.TreeMap;

import twcore.core.BotAction;
import twcore.core.events.BallPosition;
import twcore.core.game.Player;
import twcore.core.util.Tools;
import twcore.core.lvz.Objset;;

/**
 * @author Ian
 * 
 */
public class twhtRound {

    BotAction m_ba;
    twht twht_bot;
    twhtGame m_game;
    Objset m_scoreBoard;
    
    String m_fcTeam1Name;
    String m_fcTeam2Name;
    String m_fcRefName;

    TreeMap<Byte, Integer> ballMap = new TreeMap<Byte, Integer>();

    int m_fnTeam1ID;
    int m_fnTeam2ID;
    int m_matchID;
    int m_roundNum;
    int m_fnTeam1Score = 0;
    int m_fnTeam2Score = 0;
    int gameTime = 0;

    /*Round State:
     * -1 - Off
     * 0 - Adding Players
     * 1 - Game on / Face-off
     * 2 - Paused / Time-Out
     * 3 - Waiting for faceoff
     */
    int m_fnRoundState = -1;

    boolean RoundOn = false;
    boolean voteInProgress = false;
    boolean timeStamp = false;
    boolean droppingBall = false;

    twhtTeam m_team1;
    twhtTeam m_team2;

    String dbConn = "website";

    int HOST_FREQ = 2;

    TimerTask gameTimer;
    TimerTask ballDelay;
    TimerTask fo_timerTask;
    TimerTask fo_botUpdateTimer;

    Stack<String> ballPlayer = new Stack<String>();
    TreeMap<String, TWHTPosition> p_position = new TreeMap<String, TWHTPosition>(String.CASE_INSENSITIVE_ORDER);

    byte ballID;
    int ballTimeStamp;
    short ballXloc;
    short ballYloc;
    short ballXvel;
    short ballYvel;
    short topYGoal = 502;
    short botYGoal = 521;
    short freq0Xgoal = 380;
    short freq0Xbline = 462;
    short freq1Xgoal = 380;
    short freq1Xbline = 462;

    String p_ballFired;
    String p_ballCaught;
    int ballFireCount = 0;
    int ballCatchCount = 1;

    public twhtRound(int matchID, int fnTeam1ID, int fnTeam2ID, int roundNum, String fcTeam1Name, String fcTeam2Name, String refName, twhtGame twhtGame) {
        m_game = twhtGame;
        m_ba = m_game.ba;
        m_fnTeam1ID = fnTeam1ID;
        m_fnTeam2ID = fnTeam2ID;
        m_matchID = matchID;
        m_roundNum = roundNum;
        m_fcTeam1Name = fcTeam1Name;
        m_fcTeam2Name = fcTeam2Name;
        m_fcRefName = refName;
        m_scoreBoard = m_ba.getObjectSet();
    }

    public void handleEvent(BallPosition event) {

        ballID = event.getBallID();
        ballTimeStamp = event.getTimeStamp();
        ballXloc = event.getXLocation();
        ballYloc = event.getYLocation();
        ballXvel = event.getXVelocity();
        ballYvel = event.getYVelocity();

        int carrier = event.getCarrier();

        if (!ballMap.containsKey(ballID)) {
            ballMap.put(ballID, new Integer(-1));
        }

        int b = ballMap.get(ballID);
        if (m_fnRoundState == 1) {
            if ((carrier == -1) && (carrier < b)) {
                Player p = m_ba.getPlayer(b);
                if (p == null)
                    return;

                p_ballFired = p.getPlayerName();
                ballCatchCount = 0;
                ballFireCount++;
                if (p_ballFired.equals(m_ba.getBotName()))
                    return;

                ballPlayer.push(p_ballFired);
                p_position.put(p_ballFired, new TWHTPosition(p_ballFired));
            } else if ((b == -1) && (b < carrier)) {
                Player p = m_ba.getPlayer(carrier);
                if (p == null)
                    return;

                p_ballCaught = p.getPlayerName();
                ballCatchCount++;
                if (p_ballCaught.equals(m_ba.getBotName()))
                    return;

                ballPlayer.push(p_ballCaught);
                p_position.put(p_ballCaught, new TWHTPosition(p_ballCaught));
            }
            if ((ballFireCount >= 1) && (ballCatchCount >= 1)) {
                getCatchAndFire(p_ballFired, p_ballCaught);
                ballFireCount = 0;
                ballCatchCount = 0;
            }
        }
        ballMap.remove(ballID);
        ballMap.put(ballID, carrier);

    }

    public void addPlayers() {
        m_fnRoundState = 0;
        if (m_roundNum == 1) {
            m_ba.sendArenaMessage("Captains please enter in your line-up.", 2);
            m_ba.sendArenaMessage("You can private message " + m_ba.getBotName() + " with !help cap for help.");
            m_game.setFreqs(m_roundNum);
            m_game.setFrequencyAndSide();        
        } else if (m_roundNum == 2) {
            m_ba.sendArenaMessage("Captains please submit any line up changes. Sides will be switched for this period.", 2);
            m_ba.sendArenaMessage("You can private message " + m_ba.getBotName() + " with !help cap for help.");
            m_game.setFreqs(m_roundNum);
            m_game.doClearPenalties();
            m_game.setFrequencyAndSide();           
        } else if (m_roundNum == 3) {
            m_ba.sendArenaMessage("Captains please submit any line up changes. Sides will be switched for this period.", 2);
            m_ba.sendArenaMessage("You can private message " + m_ba.getBotName() + " with !help cap for help.");
            m_game.setFreqs(m_roundNum);
            m_game.doClearPenalties();
            m_game.setFrequencyAndSide();            
        } else if (m_roundNum == 4) {
            m_ba.sendArenaMessage("Captains please message the referee with your shootout order.", 2);
            m_game.setFrequencyAndSide();
        }
    }

    public void ready() {

        if (m_fnRoundState == 0) {
            m_fnRoundState = 3;
            m_ba.sendArenaMessage("Round " + m_roundNum + " is ready to begin.", 2);
            m_scoreBoard = m_ba.getObjectSet();
            doUpdateScoreBoard();
            if (m_roundNum == 1)
                m_ba.sendArenaMessage("( [ " + m_fcTeam1Name + "  (   )  " + m_fcTeam2Name + " ])");
            else if (m_roundNum == 2)
                m_ba.sendArenaMessage("( [ " + m_fcTeam2Name + "  (   )  " + m_fcTeam1Name + " ])");

            m_ba.sendPrivateMessage(m_fcRefName, "Initiate the faceoff to start the round time.");
        }

    }

    public void pause() {
        if (m_fnRoundState == 1) {
            stopTimer();
            m_ba.sendArenaMessage("Referee has paused the game.", 2);
            doGetBall(4800, 4800);
            m_fnRoundState = 2;

            ballDelay = new TimerTask() {
                @Override
                public void run() {
                    doDropBall();
                }
            };
            m_ba.scheduleTask(ballDelay, 2 * Tools.TimeInMillis.SECOND);

        } else if (m_fnRoundState == 2) {
            m_ba.sendArenaMessage("Referee has unpaused the game.", 2);
            m_fnRoundState = 3;
        }
    }

    public void faceOff(String name, String msg) {
        int min = 0;
        int max = 0;
        String[] splitmsg;

        if (m_fnRoundState != 0) {
            if (droppingBall)
                m_ba.cancelTask(fo_timerTask);

            if (msg.contains(":")) {

                splitmsg = msg.split(":");
                if (splitmsg.length == 2) {

                    try {
                        min = Integer.parseInt(splitmsg[0]);
                        max = Integer.parseInt(splitmsg[1]);
                    } catch (NumberFormatException e) {
                        return;
                    }

                    if ((min < 0 || min > 100) || (max < 0 || max > 100)) {
                        m_ba.sendPrivateMessage(name, "Invalid format. Please use !faceoff ##:##");
                        return;
                    }
                    doGetBall(8192, 8192);

                    m_game.setFrequencyAndSide();
                    m_ba.sendArenaMessage("Prepare for the Faceoff. Get on your correct sides.", 2);

                    double randomDrop = Math.floor(Math.random() * (max + 1)) + min;
                    long lRandomDrop = (new Double(randomDrop)).longValue();

                    fo_timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            doDropBall();
                            m_ba.sendArenaMessage("Go! Go! Go!", Tools.Sound.VICTORY_BELL);
                            startTimer();
                            m_fnRoundState = 1;
                        }
                    };
                    m_ba.scheduleTask(fo_timerTask, lRandomDrop * Tools.TimeInMillis.SECOND);
                } else {
                    m_ba.sendPrivateMessage(name, "Invalid format. Please use !faceoff ##:##");
                }
            }
        } else {
            m_ba.sendPrivateMessage(name, "Cannot do faceoff while adding players or while the game is paused. please !ready or unpause the game to continue.");
            return;
        }
    }

    /**
     * 
     */
    public void doDropBall() {
        m_ba.spec(m_ba.getBotName());
        m_ba.spec(m_ba.getBotName());
        m_ba.setFreq(m_ba.getBotName(), 2);
        droppingBall = false;
        m_ba.cancelTask(fo_botUpdateTimer);
    }

    /**
     * 
     */
    public void doGetBall(int xLoc, int yLoc) {
        fo_botUpdateTimer = new TimerTask() {
            @Override
            public void run() {
                if (m_ba.getShip().needsToBeSent())
                    m_ba.getShip().sendPositionPacket();
            }
        };
        m_ba.scheduleTask(fo_botUpdateTimer, 0, 1000);

        droppingBall = true;
        m_ba.getShip().setShip(0);
        m_ba.getShip().setFreq(2);
        m_ba.getShip().move(xLoc, yLoc);
        m_ba.getBall(ballID, ballTimeStamp);
    }

    /**
     * 
     * @param playerFired
     * @param playerCaught
     */
    private void getCatchAndFire(String playerFired, String playerCaught) {
//        
//        twhtTeam tA = null;
//        twhtTeam tB = null;
//        twhtPlayer pA = null;
//        twhtPlayer pB = null;
//        TWHTPosition posA = null;
//        TWHTPosition posB = null;
//        int fA = -1;
//        int fB = -1;
//        int pAXLoc = 0;
//        int pAYLoc = 0;
//        int pBXLoc = 0;
//        int pBYLoc = 0;
//        int distance = 0;
//        int estimatedY = 0;
//
//        if (m_fnRoundState == 1) {
//
//            tA = m_game.getPlayerTeam(playerFired);
//            tB = m_game.getPlayerTeam(playerCaught);
//
//            if (tA == null || tB == null)
//                return;
//
//            pA = tA.searchPlayer(playerFired);
//            pB = tB.searchPlayer(playerCaught);
//
//            if (pA == null || pB == null)
//                return;
//
//            if (tA == tB)
//                return;
//
//            if (ballPlayer.isEmpty())
//                return;
//
//            if (pB.getIsGoalie()) {
//                posA = p_position.get(playerFired);
//                posB = p_position.get(playerCaught);
//
//                if (posA == null || posB == null)
//                    return;
//
//                pAXLoc = posA.getXloc() / 16;
//                pAYLoc = posA.getYloc() / 16;
//                pBXLoc = posB.getXloc() / 16;
//                pBYLoc = posB.getYloc() / 16;
//
//                fA = tA.getFrequency();
//
//                if (fA == 1 && pAXLoc < freq0Xbline) {
//                    distance = ((pAXLoc - freq0Xgoal) * (pAYLoc - pBYLoc)) / (pAXLoc - pBXLoc);
//
//                    if (distance < 0)
//                        distance = -distance;
//
//                    if (pAYLoc < pBYLoc)
//                        estimatedY = pAXLoc + distance;
//                    else if (pAYLoc > pBYLoc)
//                        estimatedY = pAXLoc - distance;
//
//                    if (estimatedY < botYGoal && estimatedY < botYGoal)
//                       m_ba.sendArenaMessage("Save " + playerCaught + " " + estimatedY);
//                       m_ba.sendArenaMessage("Shot on goal " + playerFired);
//
//                } else if (fA == 0 && pAXLoc > freq1Xbline) {
//                    distance = ((pAXLoc - freq1Xgoal) * (pAYLoc - pBYLoc)) / (pAXLoc - pBXLoc);
//
//                    if (distance < 0)
//                        distance = -distance;
//
//                    if (pAYLoc < pBYLoc)
//                        estimatedY = pAXLoc + distance;
//                    else if (pAYLoc > pBYLoc)
//                        estimatedY = pAXLoc - distance;
//
//                    if (estimatedY < botYGoal && estimatedY < botYGoal)
//                       m_ba.sendArenaMessage("Save " + playerCaught + " " + estimatedY);
//                       m_ba.sendArenaMessage("Shot on goal " + playerFired);
//                }
//
//            } else {
//                m_ba.sendArenaMessage("Steal " + playerFired);
//                m_ba.sendArenaMessage("Turnover " + playerCaught);
//            }
            ballPlayer.clear();
//        }
    }

    public void timeStamp() {
        timeStamp = true;
    }

    public void startTimer() {
        m_ba.cancelTask(gameTimer);

        gameTimer = new TimerTask() {
            public void run() {
                gameTime++;
                gameChecks();
            }
        };
        m_ba.scheduleTask(gameTimer, Tools.TimeInMillis.SECOND, Tools.TimeInMillis.SECOND);
    }
    
    public void doUpdateScoreBoard() {
        if (m_scoreBoard != null) {
            m_scoreBoard.hideAllObjects();
            
            twhtTeam leftT = null;
            twhtTeam rightT = null;
            int lScore;            
            int rScore;
            int time;
            int lpenTemp = 0, rpenTemp = 0;
            
            if (m_roundNum == 1 || m_roundNum == 3) {
                leftT = m_game.m_team1;
                rightT = m_game.m_team2;
            } else if (m_roundNum == 2) {
                leftT = m_game.m_team2;
                rightT = m_game.m_team1;
            }
            
            time = getIntTime();
            lScore = leftT.getTeamScore();
            rScore = rightT.getTeamScore();
            
            for(twhtPlayer i : leftT.m_players.values()) 
                if (i.getPenalty() > 0) {
                    if (lpenTemp == 0)
                        lpenTemp = i.getPenalty();
                    else if (i.getPenalty() < lpenTemp)
                        lpenTemp = i.getPenalty();
                }
            
            if (lpenTemp > 0)
                lpenTemp = lpenTemp - getIntTime();
                    
                        
            for(twhtPlayer i : rightT.m_players.values()) 
                if (i.getPenalty() > 0) {
                    if (rpenTemp == 0)
                        rpenTemp = i.getPenalty();
                    else if (i.getPenalty() < rpenTemp)
                        rpenTemp = i.getPenalty();
                }
            
            if (rpenTemp > 0)
                rpenTemp = rpenTemp - getIntTime();
            
            //Time
            m_scoreBoard.showObject(100 + ((time % 60) % 10));
            m_scoreBoard.showObject(110 + ((time % 60) / 10));
            m_scoreBoard.showObject(120 + ((time / 60) % 10));
            m_scoreBoard.showObject(130 + (time / 600));
            
            //Round 
            m_scoreBoard.showObject(300 + m_roundNum - 1);
            
            //Left Score
            m_scoreBoard.showObject(210 + (lScore % 10));
            m_scoreBoard.showObject(200 + (lScore / 10));

            //Right Score
            m_scoreBoard.showObject(230 + (rScore % 10));
            m_scoreBoard.showObject(220 + (rScore / 10));

            //Left Penalty
            m_scoreBoard.showObject(420 + ((lpenTemp % 60) % 10));
            m_scoreBoard.showObject(410 + ((lpenTemp % 60) / 10));
            m_scoreBoard.showObject(400 + (lpenTemp / 60));
            
            //Right Penalty
            m_scoreBoard.showObject(450 + ((rpenTemp % 60) % 10));
            m_scoreBoard.showObject(440 + ((rpenTemp % 60) / 10));
            m_scoreBoard.showObject(430 + (rpenTemp / 60));
            m_ba.setObjects();
        }
    }
    

    public void stopTimer() {
        m_ba.cancelTask(gameTimer);
    }

    public Integer getIntTime() {
        return gameTime;
    }

    public String getStringTime() {
        int minutes;
        int seconds;

        minutes = gameTime / 60;
        seconds = gameTime % 60;

        return "" + Tools.rightString("" + minutes, 2, '0') + ":" + Tools.rightString("" + seconds, 2, '0');
    }

//    public Integer getDistLeft(String name) {
//        TWHTPosition p;
//        int sideXloc = 374;
//        int distance = 0;
//
//        p = p_position.get(name);
//        distance = p.getXloc() - sideXloc;
//        m_ba.sendArenaMessage("" + p.getXloc());
//        if (distance < 0)
//            distance = -distance;
//
//        return distance;
//    }
//
//    public Integer getDistRight(String name) {
//        TWHTPosition p;
//        int sideXloc = 649;
//        int distance = 0;
//
//        p = p_position.get(name);
//        distance = p.getXloc() - sideXloc;
//        m_ba.sendArenaMessage("" + p.getXloc());
//        if (distance < 0)
//            distance = -distance;
//
//        return distance;
//    }

    public Integer getRoundNum() {
        return m_roundNum;
    }

    /**
     * @return the m_fnRoundState
     */
    public int getRoundState() {
        return m_fnRoundState;
    }

    public void changeTime(String name, String time) {
        int minutes = 0;
        int seconds = 0;
        String[] splitmsg;

        if (time.contains(":")) {
            splitmsg = time.split(":");

            if (splitmsg.length == 2) {

                try {
                    minutes = Integer.parseInt(splitmsg[0]);
                    seconds = Integer.parseInt(splitmsg[1]);
                } catch (NumberFormatException e) {
                    return;
                }
                if ((minutes < 0 || minutes > 12) || (seconds < 0 || seconds > 60)) {
                    m_ba.sendPrivateMessage(name, "Invalid format. Please use !settime ##:##.");
                    return;
                }
                gameTime = (minutes * 60) + seconds;
                doUpdateScoreBoard();

            } else {
                m_ba.sendPrivateMessage(name, "Invalid format. Please use !settime ##:##");
            }
        }
    }

    public void gameChecks() {

        if ((m_roundNum == 1 || m_roundNum == 2) && gameTime == 600) {
            m_game.reportEndOfRound(m_roundNum);
            m_fnRoundState = -1;
        } else if (m_roundNum == 3 && gameTime == 720) {
            m_game.reportEndOfRound(m_roundNum);
            m_fnRoundState = -1;
        }

        m_game.m_team1.searchPenalties(gameTime);
        m_game.m_team2.searchPenalties(gameTime);

        doUpdateScoreBoard();
    }

    public void cancel() {
        m_ba.cancelTasks();

        //        m_scoreBoard.hideAllObjects();
        //
        //        m_ba.setObjects();
    }

    private class TWHTPosition {
        private String name;
        private int playerTimeStamp;
        private short playerXloc;
        private short playerYloc;
        private short playerXvel;
        private short playerYvel;

        //Class Constructor for TWHTPosition
        public TWHTPosition(String name) {
            this.name = name;
            setPosition();
        }

        // Sets the players position
        private void setPosition() {
            this.playerTimeStamp = ballTimeStamp;
            this.playerXloc = ballXloc;
            this.playerYloc = ballYloc;
            this.playerXvel = ballXvel;
            this.playerYvel = ballYvel;
        }

        public String getName() {
            return name;
        }

        // Returns the player's recorded X location
        public short getXloc() {
            return playerXloc;
        }

        // Returns the player's recorded Y location
        public short getYloc() {
            return playerYloc;
        }

        // Returns the player's recorded X Velocity
        public short getXVel() {
            return playerXvel;
        }

        // Returns the player's recorded Y Velocity
        public short getYvel() {
            return playerYvel;
        }
    }
}