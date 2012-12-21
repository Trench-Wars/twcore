package twcore.bots.twht;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Stack;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.command.CommandInterpreter;
import twcore.core.command.TWCoreException;
import twcore.core.events.ArenaJoined;
import twcore.core.events.BallPosition;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.SoccerGoal;
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
    
     boolean isGameOn    = false;
     boolean isBotOff    = true; 
     boolean lockArena   = true;
     boolean noDatabase  = false;

     int     fnTeam2ID   = 0;
     int     fnRefereeID = 0;     
     String  m_judge     = null;
     String  fcRefName   = null;
     
     Date today;
     Date ftTimeStarted;
     Date ftTimeEnded;
     Date fdDateScheduled;
     Date fdTimeScheduled;
     
     int     fnMatchID;
     int     fnTeam1ID;
     int     fnRankID;
     int     fnUserID;
     String  fcUserName;
     String  fcTeam1Name = null;
     String  fcTeam2Name = null;
     
     String  m_date             = null;
     String  s_fcTeam1Name      = null;
     String  s_fcTeam2Name      = null;
     String  s_fcRefName        = null;
     String  s_fdDateScheduled  = null;
     String  s_fdTimeScheduled  = null;
     int     s_sround           = 0;
    
     String dbConn = "website";
     
     twhtGAME           m_game;
     twhtTEAM           Team1;
     twhtTEAM           Team2;
     Objset             scoreboard;     
     OperatorList       m_opList;
     CommandInterpreter m_commandInterpreter;
     BotSettings        bs;
     Spy                racismWatcher;
     
      Stack<String> twhtOP;
      LinkedList m_players = new LinkedList<twhtPLAYER>();
      LinkedList m_captains = new LinkedList<String>();
     
      Stack<String> twdops;
    
    /* Creates a new instance of twht */
    public twht(BotAction botAction) {
        super(botAction); 
        ba = botAction;
        m_commandInterpreter = new CommandInterpreter(ba);
        bs = ba.getBotSettings();
        initVariables();
        requestEvents();
        registerCommands();
    }
    
    /**
     * Initializing variables at the time the bot is loaded.
     */
    private void initVariables(){
        today         =  new java.util.Date();
        racismWatcher = new Spy(ba);   //Racism watcher

//        m_botSettings = ba.getBotSettings();
//        m_arena = m_botSettings.getString("Arena");
        m_opList = ba.getOperatorList();


        twhtOP = new Stack<String>();
        loadTWHTop();

        requestEvents();

        /* LVZ */
        scoreboard = ba.getObjectSet();

    }
    
    /**
     * Loads the TWHT Ops
     */
    private void loadTWHTop() {
        try {
            ResultSet r = ba.SQLQuery(dbConn, "SELECT tblUser.fcUsername FROM `tblUserRank`, `tblUser` WHERE `fnRankID` = '25' AND tblUser.fnUserID = tblUserRank.fnUserID");

            if (r == null) {
                ba.SQLClose(r);
                return;
            }

            twhtOP.clear();
           
            while (r.next()) {
                String name = r.getString("fcUsername");
                twhtOP.push(name);
            }

            ba.SQLClose(r);
        } catch (Exception e) {}
    }
    
    /**
     * Request events that this bot requires to receive.
     *
     */
    private void requestEvents() {
        EventRequester req = ba.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.ARENA_JOINED);
        req.request(EventRequester.PLAYER_ENTERED);
        req.request(EventRequester.PLAYER_LEFT);
        req.request(EventRequester.PLAYER_DEATH);
        req.request(EventRequester.LOGGED_ON);
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(EventRequester.BALL_POSITION);
        req.request(EventRequester.SOCCER_GOAL);
    }
    

    /**
     * Registers all the ! commands.
     * 
     */
    private void registerCommands() {
        int acceptedMessages = Message.PRIVATE_MESSAGE;
        // Shorthand commands

        
        m_commandInterpreter.registerCommand( "!status", acceptedMessages, this, "cmd_status");
        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "cmd_handleHelp");
        m_commandInterpreter.registerCommand( "!view", acceptedMessages, this, "cmd_view", fcRefName);
        m_commandInterpreter.registerCommand( "!ready", acceptedMessages, this, "cmd_ready");
        m_commandInterpreter.registerCommand( "!lagout", acceptedMessages, this, "cmd_lagout");
        m_commandInterpreter.registerCommand( "!myfreq", acceptedMessages, this, "cmd_myfreq");

        m_commandInterpreter.registerCommand( "!add", acceptedMessages, this, "cmd_add");
        m_commandInterpreter.registerCommand( "!remove", acceptedMessages, this, "cmd_remove");
        m_commandInterpreter.registerCommand( "!change", acceptedMessages, this, "cmd_change");
        m_commandInterpreter.registerCommand( "!switch", acceptedMessages, this, "cmd_switch");
        m_commandInterpreter.registerCommand( "!sub", acceptedMessages, this, "cmd_sub");
        
        m_commandInterpreter.registerCommand( "!faceoff", acceptedMessages, this, "cmd_faceoff", fcRefName);
        m_commandInterpreter.registerCommand( "!pause", acceptedMessages, this, "cmd_pause", fcRefName);
        m_commandInterpreter.registerCommand( "!settime", acceptedMessages, this, "cmd_gameTime", fcRefName);
        m_commandInterpreter.registerCommand( "!pen", acceptedMessages, this, "cmd_setPenalty", fcRefName);
        m_commandInterpreter.registerCommand( "!wpen", acceptedMessages, this, "cmd_warpPenalty", fcRefName);
        m_commandInterpreter.registerCommand( "!rpen", acceptedMessages, this, "cmd_removePenalty", fcRefName);
        m_commandInterpreter.registerCommand( "!goal", acceptedMessages, this, "cmd_setGoal", fcRefName);
        m_commandInterpreter.registerCommand( "!timeout", acceptedMessages, this, "cmd_timeOut", fcRefName);
        m_commandInterpreter.registerCommand( "!judge", acceptedMessages, this, "cmd_setJudge", fcRefName);
        m_commandInterpreter.registerCommand( "!rjudge", acceptedMessages, this, "cmd_removeJudge", fcRefName);
        m_commandInterpreter.registerCommand( "!cap", acceptedMessages, this, "cmd_addCap", fcRefName);
        m_commandInterpreter.registerCommand( "!rcap", acceptedMessages, this, "cmd_removeCap", fcRefName);
        m_commandInterpreter.registerCommand( "!list", acceptedMessages, this, "cmd_handleRequest", fcRefName);
        m_commandInterpreter.registerCommand( "!a", acceptedMessages, this, "cmd_accept", fcRefName);
        m_commandInterpreter.registerCommand( "!o", acceptedMessages, this, "cmd_open", fcRefName);
        m_commandInterpreter.registerCommand( "!d", acceptedMessages, this, "cmd_deny", fcRefName);
       
        
        m_commandInterpreter.registerCommand( "!cl", acceptedMessages, this, "cmd_cl");
        m_commandInterpreter.registerCommand( "!cr", acceptedMessages, this, "cmd_cr");
        m_commandInterpreter.registerCommand( "!lag", acceptedMessages, this, "cmd_lag");
        m_commandInterpreter.registerCommand( "!gk", acceptedMessages, this, "cmd_gk");
        m_commandInterpreter.registerCommand( "!og", acceptedMessages, this, "cmd_og");
        
        m_commandInterpreter.registerCommand( "!load", acceptedMessages, this, "cmd_getMatch", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand( "!select", acceptedMessages, this, "cmd_setMatch", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand( "!start", acceptedMessages, this, "cmd_doMatch", OperatorList.ZH_LEVEL);
        
        m_commandInterpreter.registerCommand( "!sdate", acceptedMessages, this, "cmd_setDate", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand( "!stime", acceptedMessages, this, "cmd_setTime", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand( "!sref", acceptedMessages, this, "cmd_setRef", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand( "!steam", acceptedMessages, this, "cmd_setTeam", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand( "!sround", acceptedMessages, this, "cmd_setWeek", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand( "!smatch", acceptedMessages, this, "cmd_scheduleMatch", OperatorList.ER_LEVEL);
        
        m_commandInterpreter.registerCommand( "!go", acceptedMessages, this, "cmd_go", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand( "!die", acceptedMessages, this, "cmd_die", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand( "!off", acceptedMessages, this, "cmd_off", OperatorList.ZH_LEVEL);
   }
            
   /**
     * Catches unlocking of arena and re-locks.
     * 
     * @param name
     * @param message
     */
    public void handleArenaMessage( String name, String msg ) {        
        if (msg.equals("Arena UNLOCKED") && lockArena)
            ba.toggleLocked();
        else if (msg.equals("Arena LOCKED") && !lockArena)
            ba.toggleLocked();
    }
        
    /**
     * The event that is triggered at the time of login for the bot
     */
    @Override
    public void handleEvent(LoggedOn event) {
        ba.joinArena("kane");
        ba.sendUnfilteredPublicMessage("?chat=areala,KaneDEV");
    }
    
    /**
     * The event that is triggered when the bot joins an arena
     */
    @Override
    public void handleEvent(ArenaJoined event) {
        if (ba.getArenaName() == "hockey")
        ba.toggleLocked();
    }
    
    /**
     * The event that is triggered at the time a player changes freq or ship.
     */
    @Override 
    public void handleEvent(FrequencyShipChange event) {
        if (m_game != null){
            m_game.handleEvent(event);
        }
    }
    
    /**
     * The event that is triggered at the time a player enters the arena
     */
    @Override
    public void handleEvent(PlayerEntered event) {
        if (m_game != null){
            m_game.handleEvent(event);
        }
    }

    /**
     * The event that is triggered at the time a player leaves the arena
     */
    @Override
    public void handleEvent(PlayerLeft event) {
        if (m_game != null){
            m_game.handleEvent(event);
        }
    }
    
    /**
     * The event that is triggered at the time a player dies
     */
    @Override
    public void handleEvent(PlayerDeath event) {
    
    }
    
    /**
     * The event that is triggered at the time a a soccer goal is scored
     */
    @Override
    public void handleEvent(SoccerGoal event) {
        if (m_game != null){
            m_game.handleEvent(event);
        }
    }
    
    /**
     * The event that is triggered at the time the ball moves.
     */
    @Override
    public void handleEvent(BallPosition event) {
        if (m_game != null){
            m_game.handleEvent(event);
        }
    }
       
    /**
     * The event that is triggered at the time when a message is sent.
     */
    @Override
    public void handleEvent(Message event) {
            m_commandInterpreter.handleEvent(event);                        
    }
    
    /**
     * A quick view of helpful stats
     * 
     * @param name
     * @param msg
     */
    public void cmd_view(String name, String msg ) {
        
        String[] smatches = {
                "| Ref",
                "| " + fcRefName,
                "| Teams",
                "| " + fcTeam1Name,
                "| " + fcTeam2Name,
                "|______________________/",
        };
        ba.privateMessageSpam(name, smatches);
        }
   
    
    /**
     * Turns the bot off if there is no game going.
     * 
     * @param name
     * @param msg
     */
    public void cmd_off(String name, String msg) {

        ba.sendArenaMessage("I have been told to shutdown by " + name + ". Bye Bye");
        ba.sendPrivateMessage(name, "You really turn me off you know that.");
        endGame();
    }
    
    public void cmd_status(String name, String msg) {
        if (m_game != null) {
            m_game.getStatus(name, msg);
        } else {
            ba.sendPrivateMessage(name, "Waiting for someone to load a game.");
        }      
    }
    
    /**
     * The handy help files.
     * 
     * @param name
     * @param msg
     */
    public void cmd_handleHelp(String name, String msg) {
        
            String[] help_op = {
                    " ________________________________________________________________________________ ",
                    "|                                                                                |",
                    "|                                LOADING                                         |",
                    "|                                                                                |",
                    "| !load                 Loads the matches that are currently scheduled for today |",
                    "| !select <fnMatchID>     Selects the match you wish to load by it's fnMatchID       |",
                    "| !start                Starts the selected game                                 |",
                    "|_______________________________________________________________________________ |",
                    "|                                                                                |",
                    "|                              SCHEDULING                                        |",
                    "|                                                                                |",
                    "| !sdate  <yyyy-mm-dd>            Sets the date for the match to be scheduled    |",
                    "| !stime  <hh:mm>                 Sets the time for the match to be scheduled    |",
                    "| !sref   <name>                  Sets the referee for the match to be scheduled |",
                    "| !steam  <TeamName1>:<TeamName2> Sets the teams for the match to be scheduled   |",
                    "| !sround <Tourny Round #>        Sets the tournament round to be scheduled for  |",
                    "| !smatch                         Schedules the match for a later time           |",
                    "|________________________________________________________________________________|",
                    
                    
                    
            };
             
            String[] help_game = {  
                    " ________________________________________________________________________________ ",
                    "|                                                                                |",
                    "|                                  GAME                                          |",
                    "|                                                                                |",
                    "|   The following list of commands are the tool set that the referee has to      |",
                    "|   control the game. Most of these utilities are ment to be used at specific    |",
                    "|   times to control the state of the game and the game clock.                   |",
                    "|________________________________________________________________________________|",
                    "|                                                                                |",
                    "| !ready                   Starts the next round.                                |",
                    "| !judge <name>            Sets a goal judge for the current match               |",
                    "| !rjudge <name>           Removes a goal judge from the current match           |",
                    "| !pen name>:<min>         Sets a penalty on a player for a designated time      |",
                    "| !wpen                    Warps a player to the penalty box                     |",
                    "| !rpen <name>             Removes a penalty on a player                         |",
                    "| !goal <cl,cr,gk,lag,og>  Sets the final judgement on a goal thats under review |",
                    "| !faceoff                 Pauses the current timer                              |",
                    "|________________________________________________________________________________|",
                    
                    
                    
            };
            
            String[] help_ref = {  
                    " ________________________________________________________________________________ ",
                    "|                                                                                |",
                    "|                                  REFEREE                                       |",
                    "|                                                                                |",
                    "|   THESE ARE THE COMMANDS THAT ARE USED TO CONTROL THE QUE LIST FOR THE REF     |",
                    "|                    AND A FEW OTHER MISC. COMMANDS                              |",
                    "|________________________________________________________________________________|",
                    "|                                                                                |",
                    "| !settime mm:ss               Manually set the time of the current round.       |",
                    "| !pause                       Pauses the current timer                          |",
                    "| !list all,denied,acccepted   Lists all the requests in que, default lists open |",
                    "| !a                           Accepts and executes an item on the que           |",
                    "| !o                           Re-opens a previously closed item on que          |",
                    "| !d                           Denies an item on the que.                        |",
                    "| !cap                         Manually set caps for a generic game              |",
                    "| !go                          Makes the bot go to a specified arena             |",
                    "| !die                         Sends the bot to a happier place                  |",
                    "| !off                         Turns the bot off                                 |",
                    "|________________________________________________________________________________|",
                    
                    
                    
            };
            
            String[] help_vote = {
                    " ______________________________________________________________________________ ",
                    "|                                                                              |",
                    "|                                 VOTING                                       |",
                    "|                                                                              |",
                    "| When the round is in progress and a goal is scored, voting will be opened up |",
                    "| to anyone that is listed as a judge. You submit your vote by PMing the bot   |",
                    "| with one of the following commands. Voting is open until the ref sets makes  |",
                    "| the final call and sets the decision for the goal in review.                 |",
                    "|______________________________________________________________________________|",
                    "|                                                                              |",
                    "| !cl                                          Sets your vote to clean         |",
                    "| !cr                                          Sets your vote to crease        |",
                    "| !lag                                         Sets your vote to Lag           |",
                    "| !gk                                          Sets your vote to Goalie Killed |",
                    "| !og                                          Sets your vote to Own Goal      |",
                    "|______________________________________________________________________________|",
                    
                    
                    
            };
            
            String[] help_team = {  
                    " ______________________________________________________________________________ ",
                    "|                                                                              |",
                    "|                                 CAPTAIN                                      |",
                    "|                                                                              |",
                    "|  ONCE YOU TYPE A COMMAND TO THE BOT IT WILL BE SUBMITTED TO THE REF FOR      |",
                    "|  REVIEW. THE REFEREE HAS TO VERIFY AND AUTHORIZE THE COMMAND BEFORE ANY      |",
                    "|  ACTION TAKES PLACE.                                                         |", 
                    "|                                                                              |",
                    "|  WARNING: The only ship changes that are allowed during the round are to and |",
                    "|  from the goalie position. All other ship changes must be done before and    |",
                    "|  between rounds.                                                             |",
                    "|______________________________________________________________________________|",
                    "|                                                                              |",
                    "| !add <player name>:<ship>             Adds a player to the game              |",
                    "| !remove <player>                      Removes a player from the game         |",
                    "| !change <player>:<ship>               Changes a players current ship         |",
                    "| !switch <player>:<player>             Switches two players currently playing |",
                    "| !sub <playerA>:<PlayerB>:<New Ship>   Sub a player during the round          |",
                    "| !timeout                              Requests a timeout                     |",
                    "| !ready                                Lets the referee know you're ready     |",
                    "|______________________________________________________________________________|",
                    
                    
                    
            };

            String[] help_regular = {  
                    " ______________________________________________________________________________ ",
                    "|                                                                              |",
                    "|                               HELP MENU                                      |",
                    "|                                                                              |",
                    "|               THE FOLLOWING IS A LIST OF BASIC 411 COMMANDS.                 |",
                    "|          ALSO INCLUDED ARE THE HELPCOMMANDS FOR THE DIFFERENT ROLES.         |",
                    "|                                                                              |",
                    "|______________________________________________________________________________|",
                    "|                                                                              |",
                    "| !status             View information on the current match.                     |",
                    "| !help cap         Help menu for captains with various team controls          |",
                    "| !help vote        Help menu for goal judges and voting commands              |",
                    "| !help op          Help menu for TWHTOps to setup the game                    |",
                    "| !help match       Help menu to display the utility game commands for a ref   |",
                    "| !help ref         More commands for the referee                              |",
                    "|______________________________________________________________________________|",
                    
                    
                    
            };
            
           
//            if(twhtOP.contains(name)){
            if (msg.startsWith("match")) {
                ba.privateMessageSpam(name, help_op);
            } else if (msg.startsWith("op")) {
                ba.privateMessageSpam(name, help_game);
            } else if (msg.startsWith("ref")) {
                ba.privateMessageSpam(name, help_ref);
            } else if (msg.startsWith("cap")) {
                ba.privateMessageSpam(name, help_team);
            } else if (msg.startsWith("vote")) {
                    ba.privateMessageSpam(name, help_vote);
            } else { 
                ba.privateMessageSpam(name, help_regular);
            }
    }
       
    

    
    /**
     * Commands the bot to change arenas
     * 
     * @param name
     * @param msg
     */
    public void cmd_go(String name, String msg) {
        if (!isGameOn) { 
        ba.sendPrivateMessage(name, "Going to " + msg);
        ba.joinArena(msg);
        } else 
            ba.sendPrivateMessage(name, "There is a game in progress. I cannot move while there is a game in progress.");
    }
    
    /*
    *
    *   
    *  Match Scheduling
    ****************************************************************************************
    */
    
//    /**
//     * Sets the ref that will be in charge of the match that is being scheduled.
//     * 
//     * @param name
//     * @param cmd
//     */
//    public void cmd_setRef(String name, String msg) {  //TODO Temporary method fix later
//        if (msg == null){
//            if (getUserID(msg) == -1)
//            ba.sendPrivateMessage(name, "Player not found.");
//        }else {
//            ba.sendPrivateMessage(name, "Scheduled ref set to: " + msg);
//            s_fcRefName = msg;
//        }
//    }
//       
//    /**
//     * Sets the two teams that the match will be scheduled between <Team name>:<Team name>
//     * 
//     * @param name
//     * @param msg
//     */
//    public void cmd_setTeam(String name, String msg) {
//        String[] splitCmd;
//
//        if (msg.contains(":")) {
//                splitCmd = msg.split(":");
//                if(splitCmd[0].length() <= 1 && splitCmd[1].length() <= 1)
//                    throw new TWCoreException( "Invalid format. Please use !team <team one>:<team two>.");
//                s_fcTeam1Name = splitCmd[0];
//                s_fcTeam2Name = splitCmd[1];
//                if (getTeamId(s_fcTeam1Name) == -1 || getTeamId(s_fcTeam2Name) == -1)
//                    throw new TWCoreException( "One of the two teams is not found. Please check spelling and try again.");
//                ba.sendPrivateMessage(name, "Scheduled teams set to: " + s_fcTeam1Name + " vs " + s_fcTeam2Name);
//        }
//        
//    }
//    
//    /**
//     * Sets the date that the match will be scheduled at (yyyy-mm-dd)
//     * 
//     * @param name
//     * @param msg
//     */
//    public void cmd_setDate(String name, String msg) {
//        String[] splitCmd;
//        int year;
//        int months;
//        int days;
//
//        if (msg.contains("-")) {
//                splitCmd = msg.split("-");
//                if(splitCmd[0].length() <= 3 && splitCmd[1].length() <= 1 && splitCmd[2].length() <= 1)
//                    throw new TWCoreException( "Invalid format. Please use !sdate yyyy-mm-dd");
//                year = Integer.parseInt(splitCmd[0]);
//                months = Integer.parseInt(splitCmd[1]);
//                days =  Integer.parseInt(splitCmd[2]);
//                if((year < 0) || (months > 12 || months < 0) || (days > 31 || days < 0))
//                    throw new TWCoreException( "Invalid format. Please use !sdate yyyy-mm-dd");
//                s_fdDateScheduled = "" + year + "-" + months + "-" + days;
//                ba.sendPrivateMessage(name, "Scheduled date set to: " + s_fdDateScheduled);
//        }
//    }
//    
//    /**
//     * Set the time that the match will be scheduled at. hh:mm:ss
//     * 
//     * @param name
//     * @param msg
//     */
//    public void cmd_setTime(String name, String msg) {
//        String[] splitCmd;
//        int hours;
//        int minutes;
//
//        if (msg.contains(":")) {
//                splitCmd = msg.split(":");
//                if(splitCmd[0].length() <= 0 && splitCmd[1].length() <= 0)
//                    throw new TWCoreException( "Invalid format. Please use !stime hh:mm:ss  (In military format)");
//                hours = Integer.parseInt(splitCmd[0]);
//                minutes = Integer.parseInt(splitCmd[1]);
//                if((hours > 25 || hours < 0) || (minutes > 60 || minutes < 0))
//                    throw new TWCoreException( "Invalid format. Please use !stime hh:mm:ss  (In military format)");
//                s_fdTimeScheduled = "" + Tools.formatString(splitCmd[0], 2, "0") + ":" + Tools.formatString(splitCmd[1], 2, "0") + ":00";
//                ba.sendPrivateMessage(name, "Scheduled Time set to: " + s_fdTimeScheduled);
//        }
//    }
//    
//    /**
//     * Set the current round that the hockey tournament is in.
//     * 
//     * @param name
//     * @param msg
//     */
//    public void cmd_setWeek(String name, String msg){
//        int msgNum = Integer.parseInt(msg);
//        if (msgNum > 10 || msgNum < 0)
//            ba.sendPrivateMessage(name, "Incorrect format, please use !sround #");
//        else {
//            s_sround = msgNum;
//            ba.sendPrivateMessage(name, "Round set to: " + s_sround);
//        }
//    }
//      
//    /**
//     * Schedules a match for a later date.
//     * 
//     * @param name
//     * @param msg
//     */
//    public void cmd_scheduleMatch(String name, String msg) {
//        if (s_fcTeam2Name == null){
//            ba.sendPrivateMessage(name, "Please set a scheduled team");
//            return;
//        }
//        if (s_fcTeam1Name == null){
//            ba.sendPrivateMessage(name, "Please set a scheduled team");
//            return;
//        }
//        if (s_fcRefName == null){
//            ba.sendPrivateMessage(name, "Please set a scheduled ref");
//            return;
//        }
//        if (s_fdDateScheduled == null){
//            ba.sendPrivateMessage(name, "Please set a scheduled date");
//            return;
//        }
//        if (s_fdTimeScheduled == null){
//            ba.sendPrivateMessage(name, "Please set a scheduled time");
//            return;
//        }
//        if (s_sround == 0){
//            ba.sendPrivateMessage(name, "Please set a tournament round");
//            return;
//        }
//        
//        try{
//            PreparedStatement psScheduleMatch;
//            
//            psScheduleMatch = 
//                ba.createPreparedStatement(dbConn, "twht", 
//                    "INSERT INTO tbltwht__match ("+
//                    "fnTeam1ID, " +
//                    "fnTeam2ID, " +
//                    "fnRefereeID, " +
//                    "fnTournamentID, " +
//                    "fnRound," +
//                    "fdDateScheduled," +
//                    "fdTimeScheduled)" +                    
//                    "VALUES( ?,?,?,?,?,?,? )");
//            psScheduleMatch.setInt(1, getTeamId(s_fcTeam1Name));
//            psScheduleMatch.setInt(2, getTeamId(s_fcTeam2Name));
//            psScheduleMatch.setInt(3,  getUserID(s_fcRefName));
//            psScheduleMatch.setInt(4, 7);
//            psScheduleMatch.setInt(5, s_sround);
//            psScheduleMatch.setString(6, s_fdDateScheduled);
//            psScheduleMatch.setString(7, s_fdTimeScheduled);
//            psScheduleMatch.executeUpdate();           
//            psScheduleMatch.close();
//            ba.sendPrivateMessage(name, "Match Scheduled.");
//        }catch(SQLException e){
//            Tools.printLog(e.getMessage());
//        }
//        
//    }   
    
    
    

    
  
    
    /**
     * Loads all the matches that are scheduled for that date. 
     * 
     */
    public void cmd_getMatch(String name, String msg) {        
        if (m_game == null) {            
            m_date = new SimpleDateFormat("yyyy-MM-dd").format(today);
            
//            try {
//                ResultSet r = ba.SQLQuery(dbConn, "SELECT tbltwht__match.fnTeam1ID, tbltwht__match.fnTeam2ID, tbltwht__match.fnMatchID FROM`tbltwht__match`  WHERE`fnTeam1ID`=123");
//                
//                if (r == null) {
//                    ba.SQLClose(r);
//                    return;
//                }
//                while (r.next()) {
//                    fnMatchID = r.getInt("fnMatchID");
//                    fnTeam1ID = r.getInt("fnTeam1ID");
//                    fnTeam2ID = r.getInt("fnTeam2ID");
//                    ba.sendPrivateMessage(name, "Match ID: " + fnMatchID + " (" + getTeamName(fnTeam1ID) + " vs. " + getTeamName(fnTeam2ID) + ") ");
//                }
//    
//                ba.SQLClose(r);
//            } catch (Exception e) {}
            ba.sendPrivateMessage(name, "Match ID: -1 (Team 1 vs. Team 2)");
        }
    }
    
    /**
     * Selects one of the matches that is scheduled for that date
     * 
     */
    public void cmd_setMatch(String name, String msg) {
        if(m_game == null) {
          
            int msgNum = Integer.parseInt(msg);
            if (msgNum == -1) {
                noDatabase = true;
                fnMatchID = -1;
                fnTeam1ID = -1;
                fnTeam2ID = -1;
                fnRefereeID = -1;
                fcTeam1Name = "Team 1";
                fcTeam2Name = "Team 2";
                fcRefName = name;            
                ba.sendPrivateMessage(name, "Match Loaded: " + fnMatchID + " (" + fcTeam1Name + " vs. " + fcTeam2Name + ") Ref: " + fcRefName);
            } else {    
//                try {
//                  ResultSet r = ba.SQLQuery(dbConn, "SELECT fnMatchID,fnTeam1ID,fnTeam2ID,fnRefereeID  FROM tbltwht__match WHERE fnMatchID = " + msgNum);
//                  
//                  if (r == null) {
//                      ba.SQLClose(r);
//                  }
//                  while (r.next()) {
//                      fnMatchID = r.getInt("fnMatchID");
//                      fnTeam1ID = r.getInt("fnTeam1ID");
//                      fnTeam2ID = r.getInt("fnTeam2ID");
//                      fnRefereeID = r.getInt("fnRefereeID");
//                      fcTeam1Name = getTeamName(fnTeam1ID);
//                      fcTeam2Name = getTeamName(fnTeam2ID);
//                      fcRefName = getUserName(fnRefereeID);
//                      ba.sendPrivateMessage(name, "Match Loaded: " + fnMatchID + " (" + getTeamName(fnTeam1ID) + " vs. " + getTeamName(fnTeam2ID) + ") Ref: " + fcRefName);
//                      }
//                  ba.SQLClose(r);
//              } catch (Exception e) {}
            }
        }
    }
    
    /**
     * Starts the match if a referee and two teams are set.
     * 
     * @param name
     */
    public void cmd_doMatch(String name, String msg) {        
        if (m_game == null) {
            if (fcRefName == null || fcTeam1Name == null || fcTeam2Name == null) {
                ba.sendPrivateMessage(name, "A game must be loaded before starting a match.");
            } else {            
             ba.sendArenaMessage("A Hockey Match Between " + fcTeam1Name + " and " + fcTeam2Name + " is starting.", 2);             
             ftTimeStarted = new java.util.Date();            
             m_game = new twhtGAME(fnMatchID, fcRefName, fcTeam1Name, fnTeam1ID, fnTeam2ID, fcTeam2Name, ba, this, noDatabase);
            }
        } else { 
            ba.sendPrivateMessage(name, "There is already a match in process here.");
        }
    }
    
    public void cmd_pause(String name, String msg) {
        if (m_game != null)
            m_game.doPause(name, msg); 
    }
    
    public void cmd_add(String name, String msg) {
            if (m_game != null) 
                m_game.reqAddPlayer(name,msg);            
    }
    
     /**
      * Checks the input of the command to set a captain and will set one
      * for the generic game type.
      */
    public void cmd_addCap(String name, String msg) {
        if (m_game != null && noDatabase) {
            int teamNum;            
            String[] splitCmd;
            
            if (msg.contains(":")) {
                splitCmd = msg.split(":");
                if (splitCmd.length != 2) 
                    throw new TWCoreException("Invalid format. Please use !cap <name>:team# - (1)" + fcTeam1Name + " (2)" + fcTeam2Name);
                
                try {
                    teamNum = Integer.parseInt(splitCmd[1]);
                } catch(NumberFormatException  e) {
                return;
                }
                
                String p = ba.getFuzzyPlayerName(splitCmd[0]);
                
                if (p == null || teamNum > 2 || teamNum < 1) 
                    throw new TWCoreException("Player or Team not found. Please try again.");
             
                
                if (teamNum == 1) {
                    if (m_game.m_team2.isCaptain(p)){
                        ba.sendPrivateMessage(name,"Player is already captain for other team");
                    } else {
                    m_game.m_team1.m_captains.add(p);
                    ba.sendArenaMessage(p + " added to captain for " + fcTeam1Name,2);
                    }
                } else if (teamNum == 2) {
                    if (m_game.m_team1.isCaptain(p)){
                        ba.sendPrivateMessage(name,"Player is already captain for other team");
                    } else {
                    m_game.m_team2.m_captains.add(p); 
                    ba.sendArenaMessage(p + " added to captain for " + fcTeam2Name,2);
                    }
                }
            }
        }            
    }

    /**
     * Checks the input of the command to set a captain and will remove one
     * for the generic game type.
     */
   public void cmd_removeCap(String name, String msg) {
       if (m_game != null && noDatabase) {
               
           String p = ba.getFuzzyPlayerName(msg);
               
           if (p == null) 
               throw new TWCoreException("Player not found please try again.");            
                          
           if (m_game.m_team1.isCaptain(p)){
           m_game.m_team1.m_captains.remove(p);
           ba.sendArenaMessage(p + " has been removed from captain of " + fcTeam1Name,2);
           }
           
           if (m_game.m_team2.isCaptain(p)){
           m_game.m_team2.m_captains.remove(p);
           ba.sendArenaMessage(p + " has been removed from captain of " + fcTeam2Name,2);
           }
       }
   }  

    public void cmd_gameTime(String name, String msg) {
        if (m_game != null)
            m_game.setTime(name, msg);
    }
    
    public void cmd_remove(String name, String msg) {
        if (m_game != null) 
            m_game.reqRemovePlayer(name,msg);        
        }

    public void cmd_change(String name, String msg) {
        if (m_game != null) 
            m_game.reqChangePlayer(name,msg);        
    }
    
    public void cmd_switch(String name, String msg) {
        if (m_game != null) 
            m_game.reqSwitchPlayer(name,msg);        
    }

    public void cmd_sub(String name, String msg) {
        if (m_game != null) 
            m_game.reqSubPlayer(name,msg);        
    }
        
    /**
     * This is where the faceoff is handled.
     * 
     * @param name
     * @param msg
     */
    public void cmd_faceoff(String name, String msg) {
        if(m_game != null)
            if (m_game.m_curRound != null){
                if (m_game.m_curRound.m_fnRoundState == 1 || m_game.m_curRound.m_fnRoundState == 3){
                    m_game.m_curRound.faceOff(name, msg);
                }
            }
    }
    
    public void cmd_setPenalty(String name, String msg) {
        if(m_game != null) 
            m_game.setPenalty(name, msg);        
    }

    public void cmd_removePenalty(String name, String msg) {
        if(m_game != null) 
            m_game.removePenalty(name, msg);        
    }   
    
    public void cmd_warpPenalty(String name, String msg) {
        if(m_game != null) 
            m_game.warpPenalty(name, msg);        
    }   
                
    public void cmd_setGoal(String name, String msg) {
        if(m_game != null) 
            m_game.goal(msg);        
    }
                
    public void cmd_timeOut(String name, String msg) {
        if(m_game != null)
            m_game.cmd_timeOut(name,msg);        
    }

    public void cmd_cl(String name, String msg) {
        if(m_game != null)
              m_game.getVote(name, "clean");             
        }

    public void cmd_cr(String name, String msg) {
        if(m_game != null)
              m_game.getVote(name, "crease");             
        }
    
    public void cmd_lag(String name, String msg) {
        if(m_game != null)
              m_game.getVote(name, "lag");            
        }

    public void cmd_gk(String name, String msg) {
        if(m_game != null)
              m_game.getVote(name, "goalieKill");            
        }
    
    public void cmd_og(String name, String msg) {
        if(m_game != null)
              m_game.getVote(name, "ownGoal");            
        }
    
    
    public void cmd_setJudge(String name, String msg) {
        if (m_game != null) 
            m_game.addJudge(name, msg);        
    }
    
    public void cmd_removeJudge(String name, String msg) {
        if (m_game != null) 
            m_game.removeJudge(name, msg);        
    }
    
    public void cmd_ready(String name, String msg){
        if (m_game != null)
            m_game.doReady(name);           
    }
    
    public void cmd_handleRequest (String name, String msg){
        if (m_game !=null) 
            m_game.getRequestList(name, msg);        
    }
    
    public void cmd_accept(String name, String msg) {        
        if (m_game !=null) 
            m_game.acceptRequest(name, msg);           
        
    }
    
    public void cmd_open(String name, String msg) {        
        if (m_game !=null) 
            m_game.openRequest(name, msg);         
    }
    
    public void cmd_deny(String name, String msg) {        
        if (m_game !=null) 
            m_game.denyRequest(name, msg);        
    }

    public void cmd_myfreq(String name, String msg) {
        if (m_game !=null) 
            m_game.doMyfreq(name, msg);
    }
    
    public void cmd_lagout(String name, String msg) {
        if (m_game !=null) 
            m_game.reqLagoutPlayer(name, msg);        
    }
    
    /**
     * Kills the bot. Yes, it is as violent as it sounds. 
     * 
     */
    public void cmd_die(String name, String msg) {
        ba.sendArenaMessage("NOOOOOOOOOO, WHY DON'T YOU LOVE ME? WHY WAS I PROGRAMMED TO FEEL PAIN.");
        ba.die();
    }
//    public void addOp(String name, String opName){
//        twhtOP.add(opName);
//        ba.sendPrivateMessage(name, "Added "+opName+" to the TWH-OPList");
//        
//    }
    
    /**
     * Returns the user's name given the user's id.
     * 
     * @param fnUserID
     * @return
     */
   public String getUserName(int fnUserID) {
        String name;
        
        try {
          ResultSet r = ba.SQLQuery(dbConn, "SELECT fcUserName  FROM`tbluser` WHERE`fnUserID`=" + fnUserID);
         if (r == null) {
              ba.SQLClose(r);
          }
          while (r.next()) {
               name = r.getString("fcUserName");
               return name;
          }
          ba.SQLClose(r);
      } catch (Exception e) {}
        
        return "";       
    }

   /**
    * Returns the user's id given the user's name.
    * 
    * @param playerName
    * @return
    */
    public int getUserID(String playerName) {
        int id;
        
        try {
          ResultSet r = ba.SQLQuery(dbConn, "SELECT fnUserID FROM tbluser WHERE fcUserName = " + playerName);
          
          if (r == null) {
              ba.SQLClose(r);
          }
          while (r.next()) {
              id = r.getInt("fnUserID");
               return id;
          }
          ba.SQLClose(r);
      } catch (Exception e) {}
        
        return -1;
    }
    
    /**
     * Returns the team's name given the team's id.
     * 
     * @param teamID
     * @return
     */
    public String getTeamName(int teamID) {
        String name;
        
        try {
          ResultSet r = ba.SQLQuery(dbConn, "SELECT fsName FROM tbltwht__team WHERE fnTWHTTeamId = " + teamID);
          
          if (r == null) {
              ba.SQLClose(r);
          }
          while (r.next()) {
               name = r.getString("fsName");
               return name;
          }
          ba.SQLClose(r);
      } catch (Exception e) {}
        
        return "";       
    }
    
    
    /**
     * Returns the team's id given the team's name.
     * 
     * @param squadName
     * @return
     */
    public int getTeamId(String squadName){
        try{
            PreparedStatement psGetTeamId;
            psGetTeamId = ba.createPreparedStatement("website", "twht", "SELECT fnTWHTTeamId FROM tblTWHT__Team where fsName = ?");
            psGetTeamId.setString(1, squadName);
            ResultSet rs = psGetTeamId.executeQuery();
            while(rs.next()){
                int id = rs.getInt(1);
                return id;
            }
        }catch (SQLException e){
            Tools.printLog(e.toString());
        }
        
        return -1;
    } 
    
    /**
     * This method clears the variables and makes sure the bot is ready for a new game
     * 
     */
    public void endGame() {
     m_game = null;
     m_players.clear();
     m_captains.clear();
     
     fnTeam2ID   = 0;
     fnRefereeID = 0;     
     m_judge     = null;
     fcRefName   = null;     
     fnMatchID   = 0;
     fnTeam1ID   = 0;
     fnRankID    = 0;
     fnUserID    = 0;
     fcUserName  = null;
     fcTeam1Name = null;
     fcTeam2Name = null;
     
    }
    
//    public void populateGame() {
//        int rank = 0;
//        String playerName = null;
//        try {
//            ResultSet rs = ba.SQLQuery(dbConn,
//                    "SELECT tbltwht__userrank.fnUserID, tbltwht__userrank.fnRankID , tbluser.fcUserName, tbltwht__team.fsName " + 
//                    "FROM tbltwht__userrank, tbltwht__teamuser, tbluser, tbltwht__team " +
//                    "WHERE tbltwht__userrank.fnUserID = tbltwht__teamuser.fnUserID " + 
//                    "AND tbluser.fnUserID = tbltwht__teamuser.fnUserID " + 
//                    "AND tbltwht__userrank.fnUserID = tbluser.fnUserID " + 
//                    "AND tbltwht__teamuser.fdQuit IS NULL " + 
//                    "AND (tbltwht__userrank.fnRankID =4 OR tbltwht__userrank.fnRankID =3) " + 
//                    "AND tbltwht__team.fnTWHTTeamId = tbltwht__teamuser.fnTeamID ");
//    
//            
//    
//            while (rs.next()) {
//                rank = rs.getInt("fnRankID");
//                if (rank >= 3){
//                    m_captains.add(rs.getString("fcUserName").toLowerCase());
//                }               
//            }
//            ba.SQLClose(rs);
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//    }   
    
}
