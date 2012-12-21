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

/**
 * @author Ian
 *
 */
public class twhtROUND {
    
    BotAction m_ba;
    
    twht twht_bot;
    
    twhtGAME m_game;
    
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
    
    boolean RoundOn        = false;
    boolean voteInProgress = false;
    boolean timeStamp      = false;
    
    twhtTEAM m_team1;
    twhtTEAM m_team2;    

    String dbConn = "website";
   
    int HOST_FREQ = 2;
    
    TimerTask gameTimer;
    TimerTask fo_timerTask;
    
    Stack<String> ballPlayer = new Stack<String>();
    TreeMap<String, TWHTPosition> p_position  = new TreeMap<String, TWHTPosition>();
    
    byte     ballID;
    int      ballTimeStamp;
    short    ballXloc;
    short    ballYloc;
    short    ballXvel;
    short    ballYvel;
    String   p_ballFired;
    String   p_ballCaught;   
    int      ballFireCount    = 0;
    int      ballCatchCount   = 1;   
    
    
    public twhtROUND(int matchID, int fnTeam1ID, int fnTeam2ID, int roundNum, String fcTeam1Name, String fcTeam2Name, String refName, twhtGAME twhtGAME) {
        m_game          = twhtGAME;
        m_ba            = m_game.ba;
        m_fnTeam1ID     = fnTeam1ID;
        m_fnTeam2ID     = fnTeam2ID;
        m_matchID       = matchID;
        m_roundNum      = roundNum;
        m_fcTeam1Name   = fcTeam1Name;
        m_fcTeam2Name   = fcTeam2Name;
        m_fcRefName     = refName;   
   }
        
    public void handleEvent(BallPosition event) {

        
        if (m_fnRoundState == 1 || m_fnRoundState == 3) {
            ballID        = event.getBallID();
            ballTimeStamp = event.getTimeStamp();
            ballXloc      = event.getXLocation();
            ballYloc      = event.getYLocation();
            ballXvel      = event.getXVelocity();
            ballYvel      = event.getYVelocity();
    
            int carrier  = event.getCarrier();
            
            if (!ballMap.containsKey(ballID)) {
                ballMap.put(ballID, new Integer(-1));
            }
            
            int b = ballMap.get(ballID);
                if (m_fnRoundState == 1) {
                if ((carrier == -1) && (carrier < b)) {
                    Player p = m_ba.getPlayer(b);                
                    if (p == null)
                        return; 
                    
                    p_ballFired    = p.getPlayerName();
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
                    ballFireCount   = 0;
                    ballCatchCount  = 0;
                }
            }
            ballMap.remove(ballID);
            ballMap.put(ballID, carrier); 
        }
    }
    
    public void addPlayers(){
            m_fnRoundState = 0;
            if (m_roundNum == 1) {
            m_ba.sendArenaMessage("Captains please enter in your line-up.");
            m_ba.sendArenaMessage("You can private message " + m_ba.getBotName() + " with !help cap for help.");            
            } else if (m_roundNum == 2) {
                m_ba.sendArenaMessage("Captains please submit any line up changes. Sides will be switched for this period.");
                m_ba.sendArenaMessage("You can private message " + m_ba.getBotName() + " with !help cap for help.");            
                m_game.setFrequencyAndSide();
            } else if (m_roundNum == 3) {
                m_ba.sendArenaMessage("Captains please submit any line up changes. Sides will be switched for this period.");
                m_ba.sendArenaMessage("You can private message " + m_ba.getBotName() + " with !help cap for help.");         
                m_game.setFrequencyAndSide();
            } else if (m_roundNum == 4) {
                m_ba.sendArenaMessage("Captains please message the referee with your shootout order.");
                m_game.setFrequencyAndSide();              
            }           
        }
    
    public void ready(){
        
            if (m_fnRoundState == 0) {
                m_fnRoundState = 3; 
                    m_ba.sendArenaMessage("Round " + m_roundNum + " is ready to begin.");                   
                    
                if (m_roundNum == 1)
                    m_ba.sendArenaMessage("( [ " + m_fcTeam1Name + "  (   )  "+ m_fcTeam2Name + " ])");
                else if (m_roundNum == 2)
                    m_ba.sendArenaMessage("( [ " + m_fcTeam2Name + "  (   )  "+ m_fcTeam1Name + " ])"); 
                
                m_ba.sendPrivateMessage(m_fcRefName, "Initiate the faceoff to start the round time.");
            }     
           
    }
    
    public void pause() {
        if (m_fnRoundState == 1) {
            stopTimer();
            m_ba.sendArenaMessage("Referee has paused the game.", 2);
            m_fnRoundState = 2;
        } else if (m_fnRoundState == 2) {
            startTimer();
            m_ba.sendArenaMessage("Referee has unpaused the game.", 2);
            m_fnRoundState = 3;
        }
    }
    
    public void faceOff(String name, String msg) {      
        int min = 0;
        int max = 0;
        String[] splitmsg;

        if (msg.contains(":")) {

            splitmsg = msg.split(":");
            if(splitmsg.length == 2){
                
                try {
                    min = Integer.parseInt(splitmsg[0]);
                    max = Integer.parseInt(splitmsg[1]);
                } catch(NumberFormatException  e) {
                    return;
                }
                
                if((min < 0 || min > 100) || (max < 0 || max > 100)) {
                    m_ba.sendPrivateMessage(name,"Invalid format. Please use !faceoff ##:##");
                    return;
                }
                doGetBall();

                m_ba.sendArenaMessage("Prepare for the Faceoff. Get on your correct sides.", 2);

                double randomDrop = Math.floor(Math.random() * (max + 1)) + min;
                long lRandomDrop  = (new Double(randomDrop)).longValue();

                fo_timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        doDropBall();
                        m_ba.sendArenaMessage("Go! Go! Go!", 104);
                    }
                }; m_ba.scheduleTask(fo_timerTask, lRandomDrop * Tools.TimeInMillis.SECOND);
            } else {
            m_ba.sendPrivateMessage(name,"Invalid format. Please use !faceoff ##:##");
            }
        }
    }
    

    
    /**
     * 
     */
    private void doDropBall() {
        m_ba.getShip().setShip(8);
        m_ba.getShip().setFreq(2);
        m_fnRoundState = 1;
        startTimer();
    }   

    
    /**
     * 
     */
    private void doGetBall() { 
        m_ba.getShip().setShip(0);
        m_ba.getShip().setFreq(2);
        m_ba.getShip().move(8192, 8192);
        m_ba.getShip().updatePosition();
        m_ba.getBall(ballID, ballTimeStamp);
    }
    
    
    /**
     * 
     * @param playerFired
     * @param playerCaught
     */
    private void getCatchAndFire(String playerFired, String playerCaught) {
        if (m_game.getPlayerFreqency(playerFired) != m_game.getPlayerFreqency(playerCaught)) {
            if (ballPlayer.isEmpty())
                return;
            
            ballPlayer.clear();
        }
//        m_ba.sendArenaMessage(playerFired + " gave the puck to " + playerCaught);
        
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
        }; m_ba.scheduleTask(gameTimer, Tools.TimeInMillis.SECOND, Tools.TimeInMillis.SECOND);
        
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
    
    public Integer getDistLeft(String name) {
        TWHTPosition p;
        int sideXloc = 374;
        int distance = 0;
        
        p = p_position.get(name);
        distance = p.getXloc() - sideXloc;
        m_ba.sendArenaMessage("" + p.getXloc());
        if (distance < 0)
            distance = -distance;
        
        return distance;
    }
    
    public Integer getDistRight(String name) {
        TWHTPosition p;
        int sideXloc = 649;
        int distance = 0;
       
        p = p_position.get(name);
        distance = p.getXloc() - sideXloc;
        m_ba.sendArenaMessage("" + p.getXloc());
        if (distance < 0)
            distance = -distance;
        
        return distance;
    }
    
    public void changeTime(String name, String time) {
        int minutes = 0;
        int seconds = 0;
        String[] splitmsg;

        if (time.contains(":")) {
        splitmsg = time.split(":");
        
        if(splitmsg.length == 2) {
            
            try {
                minutes = Integer.parseInt(splitmsg[0]);
                seconds = Integer.parseInt(splitmsg[1]);
            } catch(NumberFormatException  e) {
                return;
            }
            if((minutes < 0 || minutes > 12) || (seconds < 0 || seconds > 60)) {
                m_ba.sendPrivateMessage(name, "Invalid format. Please use !settime ##:##.");
                return;
            }
            gameTime = (minutes * 60) + seconds;
            
            } else {
                m_ba.sendPrivateMessage(name, "Invalid format. Please use !settime ##:##");
            }
        }        
    }
    
    public void gameChecks() {
        int minutes = gameTime / 60;
        int seconds = gameTime % 60;
        
        if ((m_roundNum == 1 || m_roundNum == 2) && gameTime == 120) {
            m_game.reportEndOfRound(m_roundNum);
            m_fnRoundState = -1;
        } else if (m_roundNum == 3 && gameTime == 120){
            m_game.reportEndOfRound(m_roundNum);
            m_fnRoundState = -1;
        }
        
        m_game.m_team1.searchPenalties(gameTime);
        m_game.m_team2.searchPenalties(gameTime);
        
        if (seconds == 0 && minutes > 0)            
        m_ba.sendArenaMessage("" + Tools.rightString("" + minutes, 2, '0') + ":" + Tools.rightString("" + seconds, 2, '0'));
        do_updateScoreBoard();
    }
    
    public void do_updateScoreBoard() {
        
    }
    public void cancel() {
        m_ba.cancelTasks();

//        m_scoreBoard.hideAllObjects();
//
//        m_ba.setObjects();
    }
    
    private class TWHTPosition {
        private String  name;
        private int     playerTimeStamp;
        private short   playerXloc;
        private short   playerYloc;
        private short   playerXvel;
        private short   playerYvel;

        
        //Class Constructor for TWHTPosition
        public TWHTPosition(String name) {
            this.name         = name;
            setPosition();
        }
        
        // Sets the players position
        private void setPosition() {
             playerTimeStamp = ballTimeStamp;
             playerXloc      = ballXloc;
             playerYloc      = ballYloc;
             playerXvel      = ballXvel;
             playerYvel      = ballYvel;
        }
        
        public String getName() {
            return name;
        }
        
        // Returns the player's recorded X location
        public short getXloc(){
            return playerXloc;
        }
        
        // Returns the player's recorded Y location
        public short getYloc(){
            return playerYloc;
        }
        
        // Returns the player's recorded X Velocity
        public short getXVel(){
            return playerXvel;
        }
        
        // Returns the player's recorded Y Velocity
        public short getYvel(){
            return playerYvel;
        }
    } 
}