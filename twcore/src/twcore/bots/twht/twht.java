package twcore.bots.twht;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Stack;
import java.util.TimerTask;
import java.util.TreeMap;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.BallPosition;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.SoccerGoal;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.lvz.Objset;
import twcore.core.util.Spy;
import twcore.core.util.Tools;

/**
 * TWHT Bot
 * 
 * @author Ian
 * 
 */
public class twht extends SubspaceBot {

    private TWHTSQL       sql;
    private CheckTimer    checkTimer;
    private TimerTask     fo_timerTask;
    private Objset        scoreboard;    
    private Spy           racismWatcher;
    private TWHTState     state;
    private TWHTOfficials official;

    // Declares a hashMap for the player info.    
    private TreeMap<Integer, TWHTPosition>p_position  = new TreeMap<Integer, TWHTPosition>();
    private TreeMap<String, TWHTPlayer>       m_players    = new TreeMap<String, TWHTPlayer>(String.CASE_INSENSITIVE_ORDER);
    private TreeMap<String, TWHTOfficials>    m_officials  = new TreeMap<String, TWHTOfficials>(String.CASE_INSENSITIVE_ORDER);
    private TreeMap<Byte, Integer>            ballMap      = new TreeMap<Byte, Integer>();
    private Stack<String>                     assisters    = new Stack<String>();
    
    
    private boolean hasDatabase    = true;
    private boolean g_timerOn      = false;
    private boolean r_timerOn      = false;
    private boolean ballPosReq     = false;
    private boolean roundOne       = false;
    private boolean roundTwo       = false;    
    private boolean lockArena      = false;
    private boolean lockLastGame   = false;
    private boolean voteInProgress = false;

    private String m_referee   = "K A N E";
    private String m_team1Name = "PuckERs";
    private String m_team2Name = "Other PuckERs";
    private String timeStarted;
    private String timeEnded;
    private String p_ballFired;
    private String p_ballCaught;    
    private String g_assistOne;
    private String g_assistTwo;
    private String g_scorer;
    private int    assistCount      = 0;
    private int    ballFireCount    = 0;
    private int    ballCatchCount   = 1;
    private int    clVote   = 0;
    private int    crVote   = 0;
    private int    lagVote  = 0;
    private int    gkVote   = 0;
    
    private byte  ballID;
    private int   ballTimeStamp;
    private short ballXloc;
    private short ballYloc;
    private short ballXvel;
    private short ballYvel;
    private long  m_startTime;
    private long  m_endTime;

    private static int HOST_FREQ = 5000;

    LinkedList<TWHTRound> m_rounds;
    TWHTRound m_curRound;
    
    //Keep connection alive workaround
    private  KeepAliveConnection keepAliveConnection = new KeepAliveConnection();

    //Game states
    private enum TWHTState {

        OFF, SETTING_GAME, ADDING_PLAYERS, FACE_OFF, GAME_IN_PROGRESS, PAUSED, GAME_OVER
    };

    /* Creates a new instance of twht */
    public twht(BotAction botAction) {
        super(botAction);;  
        ba = botAction;
        initializeVariables();  //Initialize variables
        requestEvents();        //Request Subspace Events

        //Schedule the timertask to keep alive the database connection
        ba.scheduleTaskAtFixedRate(keepAliveConnection, 5 * Tools.TimeInMillis.MINUTE, 5 * Tools.TimeInMillis.MINUTE);
    }

    /* Initializes all the variables used in this class */
    private void initializeVariables() {
        if (hasDatabase) {
            sql = new TWHTSQL();                    //Game sql methods
        }

        state         = TWHTState.OFF;  
        ballXloc      = 0;
        ballYloc      = 0;
        ballTimeStamp = 0;

        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());



        racismWatcher = new Spy(ba);   //Racism watcher

        lockArena     = false;
        lockLastGame  = false;

        /* LVZ */
        scoreboard    = ba.getObjectSet();

    }

    /*
     *
     *
     *  EVENT HANDLERS
     ****************************************************************************************
     */

    private void requestEvents() {
         EventRequester req = ba.getEventRequester();

        req.request(EventRequester.ARENA_JOINED);           //Bot joined arena
        req.request(EventRequester.FREQUENCY_CHANGE);       //Player changed frequency
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);  //Player changed frequency/ship
        req.request(EventRequester.LOGGED_ON);              //Bot logged on
        req.request(EventRequester.MESSAGE);                //Bot received message
        req.request(EventRequester.PLAYER_ENTERED);         //Player entered arena
        req.request(EventRequester.PLAYER_LEFT);            //Player left arena
        req.request(EventRequester.PLAYER_POSITION);        //Player position
        req.request(EventRequester.BALL_POSITION);          //Watch ball position
        req.request(EventRequester.SOCCER_GOAL);            //A goal has been made
        req.request(EventRequester.PLAYER_DEATH);
    }

    @Override
    public void handleEvent(LoggedOn event) {
        
    }

    @Override
    public void handleEvent(ArenaJoined event) {
        
    }

    @Override
    public void handleEvent(PlayerEntered event) {
        
    }

    @Override
    public void handleEvent(PlayerLeft event) {
        
    }

    @Override
    public void handleEvent(PlayerPosition event) {
        
    }
    
    @Override
    public void handleEvent(PlayerDeath event) {
        
    }

    @Override
    public void handleEvent(SoccerGoal event) {
        TWHTPosition p;
        String scoredBy = "";
        String assistOne = "";
        String assistTwo = "";
        
        p = p_position.get(1);
        scoredBy = p.getName();
        if (p_position.containsKey(2)){
            p = p_position.get(2);
            assistOne = p.getName();
        } else if (p_position.containsKey(3)){
            p = p_position.get(3);
            assistTwo = p.getName();
        }
        doGoalReview(event);
    }

    @Override
    public void handleEvent(BallPosition event) {

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

        if ((carrier == -1) && (carrier < b)) {
            Player p = ba.getPlayer(b);
            
            p_ballFired    = p.getPlayerName();
            setPlayerPosition(p_ballFired);
            ballCatchCount = 0;
            ballFireCount++;
            
        } else if ((b == -1) && (b < carrier)) {
            Player p = ba.getPlayer(carrier);
            
            p_ballCaught = p.getPlayerName();
            ballCatchCount++;
        }
        if ((ballFireCount >= 1) && (ballCatchCount >= 1)) {
            getCatchAndFire(p_ballFired, p_ballCaught);
            ballFireCount   = 0;
            ballCatchCount  = 0;
        }
        ballMap.remove(ballID);
        ballMap.put(ballID, carrier);
    }

    @Override
    public void handleEvent(Message event) { 
        int messageType  = event.getMessageType();
         String message  = event.getMessage();
         String sender   = ba.getPlayerName(event.getPlayerID());        

         if (messageType == Message.PRIVATE_MESSAGE) {
            handleCommand(sender, message);
        }

    }

    /*
     *
     *
     *  COMMANDS
     ****************************************************************************************
     */

    /**
     * 
     * @param name
     * @param command
     */
    private void handleCommand(String name, String command) {
         String cmd = command.toLowerCase();

        if (m_officials.containsKey(name)) {
             TWHTOfficials o = m_officials.get(name);
             
            if (o.getIsRef()) {
                if (cmd.startsWith("!faceoff ")) {
                    cmd_faceOff(name, command);
                } else if (cmd.equals("!startround")) {
                    cmd_startRound(name);
                } else if (cmd.startsWith("!judge ")) {
                    cmd_addJudge(name, command.substring(7));
                }
            
            if (voteInProgress && o.getIsRef()) {
                if (cmd.startsWith("!goal ")) {
                    cmd_setGoal(name, command.substring(6));
                }
            }
                
                if (voteInProgress) {
                    if (o.getIsRef()){
                        if (cmd.startsWith("!goal ")) {
                            cmd_setGoal(name, command.substring(6));
                        }
                        
                    if (cmd.equals("!cl")) 
                        doVote(o, "clean");
                    if (cmd.equals("!lag")) 
                        doVote(o, "lag");
                    if (cmd.equals("!gk"))                
                        doVote(o, "goalieKill");
                    if (cmd.equals("!cr"))
                        doVote(o, "crease");
                    }
                }
            }
        }

        //   if (cmd.startsWith("!setmatch "))
        //       cmd_setMatch(name, command);
        if (cmd.equals("!listOfficials")) {
            cmd_listOfficials(name);
        } else if (cmd.startsWith("!setref ")) {
            cmd_setRef(name, command.substring(8));
        }
    }        
    
    /**
     * 
     * @param name
     * @param cmd
     */
    private void cmd_setRef(String name, String cmd) {
        m_officials.put(name, new TWHTOfficials(name, "referee"));

    }

    /**
     * 
     * @param name
     * @param cmd
     */
    private void cmd_addJudge(String name, String cmd) {
         int judgeID = ba.getPlayerID(cmd);

        if (judgeID == -1) {
            ba.sendPrivateMessage(name, "Player not found please try again.");
        } else {
            m_officials.put(cmd, new TWHTOfficials(cmd, "goalJudge"));
            ba.sendPrivateMessage(name, "Added: " + cmd);
        }
    }

    /**
     * 
     * @param name
     */
    private void cmd_listOfficials(String name) {
        for ( TWHTOfficials o : m_officials.values()) {
            ba.sendPrivateMessage(name, o.getOfficialType() + ": " + o.getOfficialName());
        }
    }

    /**
     * 
     * @param name
     */
    private void cmd_startRound(String name) {
        if (!roundOne) {
            roundOne = true;
            m_rounds = new LinkedList<TWHTRound>();
            m_curRound = new TWHTRound(1, m_team1Name, m_team2Name, this);
            m_rounds.add(m_curRound);
        } else {
            roundOne = false;
            roundTwo = true;
            m_rounds = new LinkedList<TWHTRound>();
            m_curRound = new TWHTRound(2, m_team1Name, m_team2Name, this);
            m_rounds.add(m_curRound);
        }
    }

    /**
     * 
     * @param name
     * @param cmd
     */
    private void cmd_timeOut(String name, String cmd) {

    }

    /**
     * 
     * @param name
     * @param cmd
     */
    private void cmd_pause(String name, String cmd) {

    }

    /**
     * 
     * @param name
     * @param cmd
     */
    private void cmd_setGoal(String name, String cmd) {

        if ((cmd != "cl") || (cmd != "gk") || (cmd != "lag")) {
            ba.sendPrivateMessage(name, "Please set the goal to !goal cl, !goal gk, or !goal lag");
        } else {
            if (cmd == "cl") {
                ba.sendArenaMessage("Review Finished. The Goal was considered clean.", 5);
            } else {
                ba.sendArenaMessage("Review Finished. The Goal will be voided due to: " + cmd, 23);
            }
            voteInProgress = false;
        }
    }

    /**
     * 
     * @param name
     * @param cmd
     */
    private void cmd_faceOff(String name, String cmd) {
        int min = 0;
        int max = 0;
        String[] splitCmd;

        if (cmd.contains(":")) {

            splitCmd = cmd.split(":");
            if ((splitCmd[0] != null) && (splitCmd[1] != null)) {
                try {
                    min = Integer.parseInt(splitCmd[0]);
                    max = Integer.parseInt(splitCmd[1]);
                    doGetBall();

                    ba.sendArenaMessage("Prepare for the Faceoff. Get on your correct sides.", 2);
                    if (roundOne) {
                        ba.sendArenaMessage("( [ " + m_team1Name + "   |   " + m_team2Name + " ] )");
                    }
                    if (roundTwo) {
                        ba.sendArenaMessage("( [ " + m_team2Name + "   |   " + m_team1Name + " ] )");
                    }

                     double randomDrop = Math.floor(Math.random() * (max + 1)) + min;
                     long lRandomDrop  = (new Double(randomDrop)).longValue();

                    fo_timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            doDropBall();
                            ba.sendArenaMessage("Go! Go! Go!", 104);
                        }
                    };
                    ba.scheduleTask(fo_timerTask, lRandomDrop * Tools.TimeInMillis.SECOND);

                } catch ( NumberFormatException e) {
                    ba.sendPrivateMessage(name, "Invalid format. Please use only numbers in the format !faceoff ##:##");
                    return;
                }

            } else {
                ba.sendPrivateMessage(name, "Invalid format. Please use the format !faceoff <min>:<max>");
                return;
            }

        } else {
            ba.sendPrivateMessage(name, "Invalid format. Please use the format !faceoff <min>:<max>");
            return;
        }
    }

    /*
     *
     *   
     *  UTILITIES
     ****************************************************************************************
     */
    
    /**
     * 
     * @param playerFired
     * @param playerCaught
     */
    private void getCatchAndFire(String playerFired, String playerCaught) {
        TWHTPlayer pA = m_players.get(playerFired);
        TWHTPlayer pB = m_players.get(playerCaught);
        
        ba.sendArenaMessage(playerFired + " gave the puck to " + playerCaught);
        
        if (getIsEnemy(pA, pB)) {
           if (pB.getIsGoalie() && getIsEnemy(pA, pB)){
               doPlayerToGoalie(pA, pB);
               p_position.clear();
           } else 
               doPlayerToPlayer(pA, pB);           
        } else if (!getIsEnemy(pA, pB) && playerFired != playerCaught){
            
        }
    }
    
    
    /**
     * 
     * @param playerA
     * @param playerB
     * @return
     */
    private boolean getIsEnemy(TWHTPlayer playerA, TWHTPlayer playerB) {
        if (playerA.getPlayerTeam() != playerB.getPlayerTeam()) {
            return true;
        } else {
            return false;
        }
    }
    
    private void setPlayerPosition(String name){
        boolean nextRecord = false;
        int i = 1;
        
        while (!nextRecord){
            if (p_position.containsKey(i)){
            i++;
            } else {
            p_position.put(i, new TWHTPosition(i, name));
            nextRecord = true;
            }
        }
    }    

    /**
     * 
     * @param pA
     * @param pB
     */
    private void doPlayertoPartner(TWHTPlayer pA, TWHTPlayer pB){
        
    }
    
    /**
     * 
     * @param pA
     * @param pB
     */
    private void doPlayerToPlayer(TWHTPlayer pA, TWHTPlayer pB){
        
    }

    /**
     * 
     * @param pA
     * @param pB
     */
    private void doPlayerToGoalie(TWHTPlayer pA, TWHTPlayer pB){
        
    }
    
    /**
     * 
     */
    private void doDropBall() {
        ba.getShip().setShip(8);
        ba.getShip().setFreq(HOST_FREQ);
    }

    /**
     * 
     */
    private void doGetBall() { 
        ba.getShip().setShip(0);
        ba.getShip().setFreq(HOST_FREQ);
        ba.getShip().move(8192, 8192);
        ba.getShip().updatePosition();
        ba.getBall(ballID, ballTimeStamp);
    }

    /**
     * 
     * @param event
     */
    private void doGoalReview(SoccerGoal event) {
        voteInProgress = true;
        ba.sendTeamMessage("Review for the last goal has started. Please private message me your vote.");
        ba.sendTeamMessage("Commands: !cl (Clean), !lag (Lag), !gk (Goalie Kill)");
        clVote = 0;
        lagVote = 0;
        gkVote = 0;
        crVote = 0;
        for (TWHTOfficials o : m_officials.values()){
            o.hasVote = true;
        }           
    }

    /**
     * 
     * @param name
     * @param vote
     */
    private void doVote(TWHTOfficials name, String vote) {
        
        if (name.getHasVote()){
            name.setVote(vote);
                if (vote == "clean")
                    clVote++;
                if (vote == "lag")
                    lagVote++;
                if (vote == "goaliekill")
                    gkVote++;
                if (vote == "goaliekill")
                    crVote++;
                ba.sendTeamMessage(name.getOfficialName() + " has voted " + name.getVote() + ".");
                ba.sendPrivateMessage(m_referee, "Totals: CLEAN(" + clVote + ")  LAG(" + lagVote + ") GK(" + gkVote + ")  CR(" + crVote + ")");
                }
        }

    /*
     *
     *
     *  OTHER
     ****************************************************************************************
     */
    
    /**
     * 
     */
    private void newGame() {

    }
    
    /**
     * 
     */
    private void startGame() {
        startCheckTimer();
    }
    
    /**
     * 
     */
    private void startCheckTimer() {
        if (checkTimer != null) {
            ba.cancelTask(checkTimer);
        }

        checkTimer = new CheckTimer();
        ba.scheduleTask(checkTimer, Tools.TimeInMillis.SECOND, Tools.TimeInMillis.SECOND);
    }

    
    /*
     *
     *
     *GAME CLASSES
     ****************************************************************************************
     */    
    
    /**
     * 
     * @author Ian
     *
     */
    private class TWHTPosition {
        private int     recordNumber;
        private String  name;
        private int     playerTimeStamp;
        private short   playerXloc;
        private short   playerYloc;
        private short   playerXvel;
        private short   playerYvel;

        
        //Class Constructor for TWHTPosition
        public TWHTPosition(int recordNumber, String name) {
            this.recordNumber = recordNumber;
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
    
    /**
     * 
     * @author Ian
     *
     */
    private class TWHTGame {

        //Class Constructor for TWHTGame
        public TWHTGame(String team1Name, String team2Name, String refName, int players) {

        }
        
        //Resets the variables in TWHTGame
        private void resetVariables() {
        }
    }

    /**
     * 
     * @author Ian
     *
     */
    private class TWHTRound {

        //Class Constructor for TWHTRound
        public TWHTRound(int fnRoundNumber, String team1Name, String team2Name, twht twht) {

        }
        
        //Resets the variables in TWHTRound
        private void resetVariables() {
            
        }
    }

    /**
     * 
     * @author Ian
     *
     */
    private class TWHTPlayer {
        //private LinkedList<TWHTPlayerShip> m_ships;
     //   private TWHTPlayerShip m_currentShip;
        private int     saves;
        private int     goals;
        private int     assist;
        private int     steals;
        private int     plusMinus;
        private int     curShipType;
        private int     shotsOnGoal;
        private int     turnovers;        
        private int     puckCarries;
        private int     checksMade;
        private int     checksTaken;
        private int     goalsAllowed;
        private String  name;
        private String  team;
        private String  iceTime;
        private String  puckTime;
        private String  goalieTime;
        private double  rating;
        private double  efficiency;
        private double  ratingPerMinute;
        private boolean isPlaying;
        private boolean isLaggedOut;
        private boolean isGoalie;
        
       

        //Class Constructor for TWHTPlayer
        public TWHTPlayer(String playerName, TWHTTeam team, int shipType) {
            this.name        = playerName;
            this.team        = team.getTeamName();
            this.curShipType = shipType;
            if (curShipType == 7 || curShipType == 8) 
                isGoalie = true;
             else 
                isGoalie = false;
        }
        
        /**
         * 
         * @author Ian
         *
         */
        private class TWHTPlayerShip {
            
            //Class Constructor for TWHTPlayerShip 
            public TWHTPlayerShip(int fnShipType) {

            }
            
            // Resets the variables in TWHTPlayerShip
            private void resetVariables() {

            }
        }
        
        private boolean getIsGoalie(){
            return isGoalie;
        }
        
        // Checks if player is lagged out 
        private boolean getIsLaggedOut(){
            return isLaggedOut;
        }
        
        // Checks if player is in the game
        private boolean getIsPlaying(){
            return isPlaying;
        }
        
        // Returns the players name
        private String getPlayerName(){
            return name;
        }
        
        // Returns the player's team name
        private String getPlayerTeam(){
            return team;
        }
        
        // Adds time to a players total match time
        private void  addPlayTime(){
        }
        
        // Adds time to a players total match time as goalie
        private void addGoalieTime(){
        }
        
        // Adds a save to the player's game total
        private void addSave(){
            saves++;
        }
        
     // Adds a goal to the player's game total
        private void addGoal(){
            goals++;
        }
        
     // Adds an assist to the player's game total
        private void addAssist(){
            assist++;
        }
        
     // Adds a steal to the player's game total
        private void addSteal(){
            steals++;
        }
        
     // Adds or subtracts from the player's +/- total
        private void addPlusMinus(String action){
            if (action == "plus")
                plusMinus++;
            else if (action == "minus")
                plusMinus--;
        }
        
     // Adds a shot on goal to the player's game total
        private void addShotOnGoal(){
            shotsOnGoal++;            
        }
        
     // Adds a turnover to the player's game total
        private void addTurnover(){
            turnovers++;
        }
        
     // Adds a puck carry to the player's game total
        private void addPuckCarry(){
            puckCarries++;
        }
        
     // Adds a check made to the player's game total
        private void addCheckMade(){
            checksMade++;
        }
        
     // Adds a check taken to the player's game total
        private void addCheckTaken(){
            checksTaken++;
        }
        
     // Adds a goal allowed to the player's game total
        private void addGoalAllowed(){
            goalsAllowed++;
        }
        
     // Computes a player's game rating
        private void crunchRating(){
         }
        
        // Computes a player's game efficiency
        private void crunchEfficiency(){
        }
        
        // Computes a players rating per minute
        private void crunchRatingPerMinute(){
        }
        
        // Resets variables in TWHTPlayer
        private void resetVariables() {

        }
        
    }

    /**
     * 
     * @author Ian
     *
     */
    private class TWHTTeam {
        private boolean timeOutLeft;
        private int     frequency;
        private String  teamName;
        private int     substitutesLeft;

        //Class Constructor for TWHTTeam
        public TWHTTeam(int frequency) {
            this.frequency = frequency;
            resetVariables();
        }
        
        // Gets the team name.
        public String getTeamName() {
            return teamName;
        }

        //Resets the variables for TWHTTeam
        private void resetVariables() {
            timeOutLeft = true;
        }
    }

    /**
     * 
     * @author Ian
     *
     */
    private class TWHTOfficials {
        private String  o_type;
        private String  o_name;
        private String  goalVote;
        private boolean hasVote     = false;
        private boolean isRef       = false;
        private boolean isGoalJudge = false;
        
        //Class Constructor for TWHTOfficials
        public TWHTOfficials(String name, String type) {
            if (type == "goalJudge") {
                isGoalJudge = true;
                o_type      = "Goal Judge";
                
            } else if (type == "referee") {
                isRef       = true;
                isGoalJudge = true;
                o_type      = "Referee";
            }
            this.o_name = name;
        }

        //Sets the current vote for the official.
        private void setVote(String vote) {
            this.goalVote = vote;
            hasVote = false;
        }

        //Returns what the official's vote is.
        private String getVote() {
            return goalVote;
        }

        //Returns if the official has a vote. 
        private boolean getHasVote() {
            return hasVote;
        }

        //Returns if the official is a ref.
        private boolean getIsRef() {
            return isRef;
        }

        //Returns if the official is a goal judge.
        private boolean getIsGoalJudge() {
            return isGoalJudge;
        }

        //Returns the type of official.
        private String getOfficialType() {
            return o_type;
        }
        
        //Returns the name of the official.
        private String getOfficialName() {
            return o_name;
        }
        
        // Resets the variables for TWHTOfficials.
        private void resetVariables() {
            goalVote    = null;
            hasVote     = true;
        }
    }

   /**
    * 
    * @author Ian
    *
    */
    private class TWHTSQL {
        private PreparedStatement psKeepAlive;

        private void keepAlive() {
            try {
                psKeepAlive.execute();
            } catch ( SQLException sqle) {
                // No need to do anything here
            }
        }
    }

    /**
     * 
     * @author Ian
     *
     */
    private class CheckTimer extends TimerTask {
        int gameTime;
        int roundTime;

        //class constructor 
        public CheckTimer() {
            gameTime = 0;
            roundTime = 0;
        }

        @Override
        public void run() {
            timers();
        }

        private void timers() {
            if (g_timerOn) {
                gameTime++;
            }
            if (r_timerOn) {
                roundTime++;
            }
        }

    }
    
    /**
     * 
     * @author Ian
     *
     */
    private class KeepAliveConnection extends TimerTask {
        @Override
        public void run() {
            if (hasDatabase) {
                sql.keepAlive();
            }
        }
    }

}
