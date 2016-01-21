/**

*/
package twcore.bots.twht;

import java.util.Stack;
import java.util.TimerTask;
import java.util.TreeMap;

import twcore.core.BotAction;
import twcore.core.events.BallPosition;
import twcore.core.events.WatchDamage;
import twcore.core.game.Player;
import twcore.core.util.Tools;
import twcore.core.lvz.Objset;;

/**
    This is the round class for thwt. It will control all the actions and store the variables
    that are round specific.

    @author Ian
*/
public class twhtRound {

    BotAction m_ba;
    twht twht_bot;
    twhtGame m_game;
    Objset m_scoreBoard;
    Objset m_scoreBoard2;

    String m_fcTeam1Name;
    String m_fcTeam2Name;
    String m_fcRefName;

    TreeMap<Byte, Integer> ballMap = new TreeMap<Byte, Integer>();

    int m_fnTeam1ID;
    int m_fnTeam2ID;
    int m_matchID;
    int m_roundNum;
    int gameTime = 0;

    /*  Round State:
        -1 - Off
        0 - Adding Players
        1 - Game on / Face-off
        2 - Paused / Time-Out
        3 - Waiting for faceoff
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
    short freq0Xgoal = 378;
    short freq0Xbline = 462;
    short freq1Xgoal = 645;
    short freq1Xbline = 561;

    String p_ballFired;
    String p_ballCaught;
    String p_hasBall;
    int ballFireCount = 0;
    int ballCatchCount = 1;

    //Creates a new instance of twhtRound
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
        m_scoreBoard2 = m_ba.getObjectSet();
    }

    /**
        Tracks the ball position. Event is fired when the ball moves at an interval set
        in the map settings.

        @param event
    */
    public void handleEvent(BallPosition event) {
        ballID = event.getBallID();
        ballTimeStamp = event.getTimeStamp();
        ballXloc = event.getXLocation();
        ballYloc = event.getYLocation();
        ballXvel = event.getXVelocity();
        ballYvel = event.getYVelocity();

        int carrier = event.getCarrier();

        if (!ballMap.containsKey(ballID))
            ballMap.put(ballID, new Integer(-1));

        int b = ballMap.get(ballID);

        if (m_fnRoundState == 1) {
            if ((carrier == -1) && (carrier < b)) {
                Player p = m_ba.getPlayer(b);

                if (p == null)
                    return;

                p_ballFired = p.getPlayerName();
                ballCatchCount = 0;
                ballFireCount++;

                if (!p_ballFired.equals(m_ba.getBotName())) {
                    ballPlayer.push(p_ballFired);
                    p_position.put(p_ballFired, new TWHTPosition(p_ballFired));
                    p_hasBall = null;
                }
            } else if ((b == -1) && (b < carrier)) {
                Player p = m_ba.getPlayer(carrier);
                twhtPlayer pA;

                if (p == null)
                    return;

                p_ballCaught = p.getPlayerName();
                ballCatchCount++;

                if (!p_ballCaught.equals(m_ba.getBotName())) {
                    ballPlayer.push(p_ballCaught);
                    p_position.put(p_ballCaught, new TWHTPosition(p_ballCaught));
                    m_ba.spectatePlayer(p_ballCaught);
                    p_hasBall = p_ballCaught;
                    m_game.doPlayerStats(p_ballCaught, 7);

                    twhtTeam t = null;
                    t = m_game.getPlayerTeam(p.getPlayerName());

                    if (t == null)
                        return;

                    pA = t.searchPlayer(p_ballCaught);

                    if ((pA.getPlayerShip() == 7 || pA.getPlayerShip() == 8) && t.getFrequency() == 0 && (ballXloc / 16) > freq0Xbline || (t.getFrequency() == 1 && (ballXloc / 16) < freq1Xbline)) {
                        m_game.reqPenalty("Goalie interference", t.getPenTime("gcrossing"), p_ballCaught);
                    } else if ((pA.getPlayerShip() != 7 && pA.getPlayerShip() != 8) && ((ballYloc / 16) > 499 && (ballYloc / 16) < 525 && (ballXloc / 16) > 380 && (ballXloc / 16) < 395 && t.getFrequency() == 0))
                        if (getIsInCrease("left", (ballXloc / 16), (ballYloc / 16))) {
                            m_game.reqPenalty("Defensive Crease", t.getPenTime("dc"), p_ballCaught);
                            m_game.reqPenalty("Blatant Defensive Crease", t.getPenTime("bdc"), p_ballCaught);
                        }
                        else if ((pA.getPlayerShip() != 7 && pA.getPlayerShip() != 8) && ((ballYloc / 16) > 499 && (ballYloc / 16) < 525 && (ballXloc / 16) > 630 && (ballXloc / 16) < 645 && t.getFrequency() == 1))
                            if (getIsInCrease("right", (ballXloc / 16), (ballYloc / 16))) {
                                m_game.reqPenalty("Defensive Crease", t.getPenTime("dc"), p_ballCaught);
                                m_game.reqPenalty("Blatant Defensive Crease", t.getPenTime("bdc"), p_ballCaught);
                            }
                }
            }
        }

        if ((ballFireCount >= 1) && (ballCatchCount >= 1)) {
            doCatchAndFire(p_ballFired, p_ballCaught);
            ballFireCount = 0;
            ballCatchCount = 0;
        }

        ballMap.remove(ballID);
        ballMap.put(ballID, carrier);
    }

    /**
        Tracks the ball position. Event is fired when the ball moves at an interval set
        in the map settings.

        @param event
    */
    public void handleEvent(WatchDamage event) {
        short attacker;
        short attacked;
        short xLoc;
        short yLoc;
        Player pA;
        Player pB;

        attacker = event.getAttacker();
        attacked = event.getVictim();

        pA = m_ba.getPlayer(attacker);
        pB = m_ba.getPlayer(attacked);

        if (pA == null || pB == null)
            return;

        twhtTeam t = null;
        t = m_game.getPlayerTeam(pB.getPlayerName());

        if (t == null)
            return;

        xLoc = pB.getXTileLocation();
        yLoc = pB.getYTileLocation();

        if ((pB.getShipType() == 7 || pB.getShipType() == 8) && (yLoc > 499 && yLoc < 525 && xLoc > 630 && xLoc < 645 && pB.getFrequency() == 0)) {
            if (getIsInCrease("left", xLoc, yLoc))
                m_game.reqPenalty("Attacked Goalie", t.getPenTime("gattack"), pA.getPlayerName());
        } else if ((pB.getShipType() == 7 || pB.getShipType() == 8) && (yLoc > 499 && yLoc < 525 && xLoc > 380 && xLoc < 395 && pB.getFrequency() == 1)) {
            if (getIsInCrease("right", xLoc, yLoc))
                m_game.reqPenalty("Attacked Goalie", t.getPenTime("gattack"), pA.getPlayerName());
        }
    }
    /**
        Changes the round time. Accepts parameters in <minutes>:<seconds> form

        @param name
        @param time
    */
    public void setChangeTime(String name, String time) {
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
                doUpdateScoreBoardTime();
            } else
                m_ba.sendPrivateMessage(name, "Invalid format. Please use !settime ##:##");
        }
    }

    /**
        Executes the app player phase before every round.
    */
    public void doAddPlayers() {
        m_fnRoundState = 0;
        doRemoveBall();

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

    /**
        Executes the start of the round after the referee uses !ready
    */
    public void doReady() {
        if (m_fnRoundState == 0) {
            m_fnRoundState = 3;
            m_ba.sendArenaMessage("Round " + m_roundNum + " is ready to begin.", 2);
            m_scoreBoard = m_ba.getObjectSet();
            doUpdateScoreBoardTime();
            m_game.m_team1.setStartRound();
            m_game.m_team2.setStartRound();

            if (m_roundNum == 1)
                m_ba.sendArenaMessage("( [ " + m_fcTeam1Name + "  (   )  " + m_fcTeam2Name + " ])");
            else if (m_roundNum == 2)
                m_ba.sendArenaMessage("( [ " + m_fcTeam2Name + "  (   )  " + m_fcTeam1Name + " ])");

            m_ba.sendPrivateMessage(m_fcRefName, "Initiate the faceoff to start the round time.");
        }

    }

    /**
        Executes the faceoff

        @param name
        @param msg
    */
    public void doFaceOff(String name, String msg) {
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
                            doStartTimer();
                            m_fnRoundState = 1;
                        }
                    };
                    m_ba.scheduleTask(fo_timerTask, lRandomDrop * Tools.TimeInMillis.SECOND);
                } else
                    m_ba.sendPrivateMessage(name, "Invalid format. Please use !faceoff ##:##");
            }
        } else {
            m_ba.sendPrivateMessage(name, "Cannot do faceoff while adding players or while the game is paused. please !ready or unpause the game to continue.");
            return;
        }
    }

    /**
        Everytime the game is in session and the ball transfers from one player or another
        this event is fired.

        @param playerFired
        @param playerCaught
    */
    public void doCatchAndFire(String playerFired, String playerCaught) {
        twhtTeam tA = null;
        twhtTeam tB = null;
        twhtPlayer pA = null;
        twhtPlayer pB = null;
        TWHTPosition posA = null;
        TWHTPosition posB = null;
        int fA = -1;
        //int fB = -1;
        int pAXLoc = 0;
        int pAYLoc = 0;
        int pBXLoc = 0;
        int pBYLoc = 0;
        int pYLoc = 0;
        int distance = 0;
        int estimatedY = 0;

        if (m_fnRoundState == 1) {
            tA = m_game.getPlayerTeam(playerFired);
            tB = m_game.getPlayerTeam(playerCaught);

            if (tA == null || tB == null)
                return;

            pA = tA.searchPlayer(playerFired);
            pB = tB.searchPlayer(playerCaught);
            fA = tA.getFrequency();
            //fB = tB.getFrequency();

            if (pA == null || pB == null)
                return;

            if (tA == tB)
                return;

            if (ballPlayer.isEmpty())
                return;

            if (pB.getIsGoalie()) {
                posA = p_position.get(playerFired);
                posB = p_position.get(playerCaught);

                if (posA == null || posB == null)
                    return;

                pAXLoc = posA.getXloc() / 16;
                pAYLoc = posA.getYloc() / 16;
                pBXLoc = posB.getXloc() / 16;
                pBYLoc = posB.getYloc() / 16;

                if (fA == 1 && pAXLoc < freq0Xbline) {
                    if (pAXLoc <= pBXLoc) {
                        m_game.doPlayerStats(playerCaught, 4);
                        m_game.doPlayerStats(playerFired, 6);
                    } else {
                        if ((pAYLoc - pBYLoc) == 0)
                            pYLoc = 1;
                        else
                            pYLoc = pAYLoc - pBYLoc;

                        distance = ((pAXLoc - freq0Xgoal) * (pYLoc)) / (pAXLoc - pBXLoc);
                    }
                } else if (fA == 0 && pAXLoc > freq1Xbline) {
                    if (pAXLoc >= pBXLoc) {
                        m_game.doPlayerStats(playerCaught, 4);
                        m_game.doPlayerStats(playerFired, 6);
                    } else {
                        if ((pAYLoc - pBYLoc) == 0)
                            pYLoc = 1;
                        else
                            pYLoc = pAYLoc - pBYLoc;

                        distance = ((pAXLoc - freq1Xgoal) * (pYLoc)) / (pAXLoc - pBXLoc);
                    }
                }

                if (distance < 0)
                    distance = -distance;

                if (pAYLoc <= pBYLoc)
                    estimatedY = pAYLoc + distance;
                else if (pAYLoc > pBYLoc)
                    estimatedY = pAYLoc - distance;

                if (estimatedY <= botYGoal && estimatedY >= topYGoal) {
                    m_game.doPlayerStats(playerCaught, 3);
                    m_game.doPlayerStats(playerFired, 5);
                } else {
                    m_game.doPlayerStats(playerCaught, 4);
                    m_game.doPlayerStats(playerFired, 6);
                }
            } else {
                m_game.doPlayerStats(playerCaught, 4);
                m_game.doPlayerStats(playerFired, 6);
            }

            ballPlayer.clear();
        }
    }

    /**
        Updates the scoreboard for the non-timer parts of the lvz
    */
    public void doUpdateScoreBoard() {
        twhtTeam leftT = null;
        twhtTeam rightT = null;
        int lScore;
        int rScore;

        if (m_roundNum == 1 || m_roundNum == 3) {
            leftT = m_game.m_team1;
            rightT = m_game.m_team2;
        } else if (m_roundNum == 2) {
            leftT = m_game.m_team2;
            rightT = m_game.m_team1;
        }

        lScore = leftT.getTeamScore();
        rScore = rightT.getTeamScore();

        if (m_ba.getArenaName().equalsIgnoreCase("hockey")) {
            //Left Score
            if (lScore % 10 > 0)
                m_scoreBoard.showObject(100 + (lScore % 10));
            else
                m_scoreBoard.hideObject(100);

            if (lScore / 10 > 0)
                m_scoreBoard.showObject(110 + (lScore / 10));
            else
                m_scoreBoard.hideObject(110);

            //Right Score
            if (rScore % 10 > 0)
                m_scoreBoard.showObject(200 + (rScore % 10));
            else
                m_scoreBoard.hideObject(200);

            if (rScore / 10 > 0)
                m_scoreBoard.showObject(210 + (rScore / 10));
            else
                m_scoreBoard.hideObject(210);

            //Left Team Name - Team1
            m_scoreBoard.showObject(490);
            m_scoreBoard.showObject(341);
            m_scoreBoard.showObject(302);
            m_scoreBoard.showObject(423);

            if (m_roundNum == 1 || m_roundNum == 3)
                m_scoreBoard.showObject(574);
            else if (m_roundNum == 2)
                m_scoreBoard.showObject(584);

            //Right Team Name
            m_scoreBoard.showObject(495);
            m_scoreBoard.showObject(346);
            m_scoreBoard.showObject(307);
            m_scoreBoard.showObject(428);

            if (m_roundNum == 1 || m_roundNum == 3)
                m_scoreBoard.showObject(589);
            else if (m_roundNum == 2)
                m_scoreBoard.showObject(579);
        } else if (m_ba.getArenaName().equalsIgnoreCase("#hockey")) {
            //Round
            m_scoreBoard.showObject(300 + m_roundNum - 1);

            //Left Score
            m_scoreBoard.showObject(210 + (lScore % 10));
            m_scoreBoard.showObject(200 + (lScore / 10));

            //Right Score
            m_scoreBoard.showObject(230 + (rScore % 10));
            m_scoreBoard.showObject(220 + (rScore / 10));
        }

        m_ba.setObjects();
    }

    /**
        Updates the scoreboard timers
    */
    public void doUpdateScoreBoardTime() {
        if (m_scoreBoard2 != null) {
            m_scoreBoard2.hideAllObjects();
            twhtTeam leftT = (getRoundNum() % 2 == 1 ? m_team1 : m_team2);
            twhtTeam rightT = (getRoundNum() % 2 == 1 ? m_team2 : m_team1);
            int time;
            int lpenTemp = 0, rpenTemp = 0;

            time = getIntTime();

            if (m_ba.getArenaName().equalsIgnoreCase("hockey")) {
                //Time
                m_scoreBoard2.showObject(700 + ((time % 60) % 10));
                m_scoreBoard2.showObject(710 + ((time % 60) / 10));
                m_scoreBoard2.showObject(720 + ((time / 60) % 10));
                m_scoreBoard2.showObject(730 + (time / 600));
            } else if (m_ba.getArenaName().equalsIgnoreCase("#hockey") && leftT != null && rightT != null) {
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
                m_scoreBoard2.showObject(100 + ((time % 60) % 10));
                m_scoreBoard2.showObject(110 + ((time % 60) / 10));
                m_scoreBoard2.showObject(120 + ((time / 60) % 10));
                m_scoreBoard2.showObject(130 + (time / 600));


                //Left Penalty
                m_scoreBoard2.showObject(420 + ((lpenTemp % 60) % 10));
                m_scoreBoard2.showObject(410 + ((lpenTemp % 60) / 10));
                m_scoreBoard2.showObject(400 + (lpenTemp / 60));

                //Right Penalty
                m_scoreBoard2.showObject(450 + ((rpenTemp % 60) % 10));
                m_scoreBoard2.showObject(440 + ((rpenTemp % 60) / 10));
                m_scoreBoard2.showObject(430 + (rpenTemp / 60));
            }
        }

        doUpdateScoreBoard();
    }

    /**
        Removes the ball from play
    */
    public void doRemoveBall() {
        doGetBall(4800, 4800);

        ballDelay = new TimerTask() {
            @Override
            public void run() {
                doDropBall();
            }
        };
        m_ba.scheduleTask(ballDelay, 2 * Tools.TimeInMillis.SECOND);
    }

    /**
        Pauses the current game
    */
    public void doPause() {
        if (m_fnRoundState == 1) {
            doStopTimer();
            m_ba.sendArenaMessage("Referee has paused the game.", 2);
            m_fnRoundState = 2;
            doRemoveBall();
        } else if (m_fnRoundState == 2) {
            m_ba.sendArenaMessage("Referee has unpaused the game.", 2);
            m_fnRoundState = 3;
        }
    }

    /**
        Drops the ball and returns the bot to spec
    */
    public void doDropBall() {
        m_ba.spec(m_ba.getBotName());
        m_ba.spec(m_ba.getBotName());
        m_ba.setFreq(m_ba.getBotName(), 2);
        droppingBall = false;
        m_ba.cancelTask(fo_botUpdateTimer);
    }

    /**
        Causes the bot to grab the ball and goes to a specific location
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
        Checks what needs to be checked every second the round is running
    */
    public void doGameChecks() {
        if ((m_roundNum == 1 || m_roundNum == 2) && gameTime == 600) {
            m_game.reportEndOfRound(m_roundNum);
            m_fnRoundState = -1;
        } else if (m_roundNum == 3 && gameTime == 720) {
            m_game.reportEndOfRound(m_roundNum);
            m_fnRoundState = -1;
        }

        if (p_hasBall != null)
            m_game.doPlayerStats(p_hasBall, 11);

        m_game.m_team1.searchPenalties(gameTime);
        m_game.m_team2.searchPenalties(gameTime);

        if (gameTime % 2 == 1)
            doUpdateScoreBoardTime();
    }

    /**
        Starts the timer for the round
    */
    public void doStartTimer() {
        m_ba.cancelTask(gameTimer);

        gameTimer = new TimerTask() {
            public void run() {
                gameTime++;
                doGameChecks();
            }
        };
        m_ba.scheduleTask(gameTimer, Tools.TimeInMillis.SECOND, Tools.TimeInMillis.SECOND);
    }

    /**
        stops the round timer
    */
    public void doStopTimer() {
        m_ba.cancelTask(gameTimer);
    }

    /**


        @return
    */
    public boolean getIsInCrease(String side, int xLoc, int yLoc) {
        int creaseXLoc = 0;
        int creaseYLoc = 511;
        double distance;

        if (side.equals("left"))
            creaseXLoc = 381;
        else if (side.equals("right"))
            creaseXLoc = 642;

        distance = Math.sqrt(Math.pow((creaseXLoc - xLoc), 2) + Math.pow((creaseYLoc - yLoc), 2));

        if (distance <= 10)
            return true;
        else
            return false;

    }

    /**
        Returns the round time in integer / second form

        @return
    */
    public Integer getIntTime() {
        return gameTime;
    }

    /**
        Returns the round time in string / mm:ss form

        @return
    */
    public String getStringTime() {
        int minutes;
        int seconds;

        minutes = gameTime / 60;
        seconds = gameTime % 60;

        return "" + Tools.rightString("" + minutes, 2, '0') + ":" + Tools.rightString("" + seconds, 2, '0');
    }

    /**
        Returns the round number

        @return
    */
    public Integer getRoundNum() {
        return m_roundNum;
    }

    /**
        Returns the round state

        @return the m_fnRoundState
    */
    public int getRoundState() {
        return m_fnRoundState;
    }

    /**
        Cancels what needs to be canceled when the round is over
    */
    public void cancel() {
        m_ba.cancelTasks();
        m_scoreBoard.hideAllObjects();
        m_ba.setObjects();
    }

    /**
        Position class that stores the ball position when it is fired or released

        @author Ian

    */
    public class TWHTPosition {

        private String name;
        public int playerTimeStamp;
        public short playerXloc;
        public short playerYloc;
        public short playerXvel;
        public short playerYvel;

        //Class Constructor for TWHTPosition
        public TWHTPosition(String name) {
            this.name = name;
            setPosition();
        }

        // Sets the players position
        public void setPosition() {
            this.playerTimeStamp = ballTimeStamp;
            this.playerXloc = ballXloc;
            this.playerYloc = ballYloc;
            this.playerXvel = ballXvel;
            this.playerYvel = ballYvel;
        }

        // Returns the player's name
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
        public short getXvel() {
            return playerXvel;
        }

        // Returns the player's recorded Y Velocity
        public short getYvel() {
            return playerYvel;
        }
    }
}