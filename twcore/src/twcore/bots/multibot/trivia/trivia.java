package twcore.bots.multibot.trivia;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.Message;
import twcore.core.stats.PlayerProfile;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

/**
 * Trivia.
 *
 * Games to 1 work now, rating system added.
 *
 * @author 2dragons - 11.05.02
 */
public class trivia extends MultiModule {

    CommandInterpreter  m_commandInterpreter;

    Random          m_rnd;
    String          mySQLHost = "website";
    TimerTask       startGame, timerQuestion, timerHint, timerAnswer, timerNext;
    TimerTask       timedMessages, timedMessages2;
    TreeMap<String, PlayerProfile> playerMap = new TreeMap<String, PlayerProfile>();
    //HashMap         playerID = new HashMap();
    HashMap<String, String> accessList = new HashMap<String, String>();
    Vector<String>  topTen;

    int             gameProgress = -1, toWin = 10, questionNumber = 1, curLeader = 0;
    int             m_mintimesused = -1;

    int             m_timeQuestion = 0, m_timeHint = 0, m_timeAnswer = 0, m_timeNext = 0;
    int             m_timePer1 = 0, m_timePer2 = 0;

    double          giveTime;
    String          m_prec = "--|-- ";
    String          t_category, t_question, t_answer, f_answer;
    String[]        helpmsg =
    { "To ?chat=trivia or privately:",
      "!help          -- displays this.",
      "!score         -- displays the current scores.",
      "!repeat        -- will repeat the last question given.",
      "!stats         -- will display your statistics.",
      "!stats <name>  -- displays <name>'s statistics.",
      "!topten        -- displays top ten player stats.",
      "To ?chat=trivia only:",
      "!pm            -- bot will pm you for remote play."
    };
    String[]        opmsg =
    { "!start         -- Starts a game of trivia to 10",
      "!start <num>   -- Starts a game of trivia to <num> (1-25)",
      "!cancel        -- Cancels a game of trivia",
      "---------------------------------------------------------"
    };

    public void init() {
        m_commandInterpreter = new CommandInterpreter( m_botAction );
        getTopTen();
        registerCommands();
        m_rnd = new Random();
        BotSettings m_botSettings = moduleSettings;
        m_botAction.sendUnfilteredPublicMessage( "?chat=trivia" );

        //Gets variables from .cfg
        m_timeQuestion =  m_botSettings.getInt("QuestionTime");
        m_timeHint        =  m_botSettings.getInt("HintTime");
        m_timeAnswer   =  m_botSettings.getInt("AnswerTime"); //cumulative w/hint
        m_timeNext       =  m_botSettings.getInt("NextTime");   //Till next question
        m_timePer1     =  m_botSettings.getInt("Periodic1");
        m_timePer2     =  m_botSettings.getInt("Periodic2");
        toWin           =  m_botSettings.getInt("ToWin");
        String access[] =  m_botSettings.getString("SpecialAccess").split( ":" );
        for( int i = 0; i < access.length; i++ )
            accessList.put( access[i], access[i] );
    }

    public void requestEvents(ModuleEventRequester events) {
	}

	public  String[] getModHelpMessage() {
    	return opmsg;
    }

    public boolean isUnloadable() {
    	return true;
    }
    
    public void cancel() {
    	gameProgress = -1;
    	playerMap.clear();
        m_botAction.cancelTasks();
    }

    /****************************************************************/
    /*** Registers the bot commands.                              ***/
    /****************************************************************/
    public void registerCommands() {
        int acceptedMessages;

        acceptedMessages = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand( "!start",     acceptedMessages, this, "doStartGame" );
        m_commandInterpreter.registerCommand( "!cancel",    acceptedMessages, this, "doCancelGame" );

        acceptedMessages = Message.CHAT_MESSAGE | Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand( "!repeat",    acceptedMessages, this, "doRepeat" );
        m_commandInterpreter.registerCommand( "!help",      acceptedMessages, this, "doHelp" );
        m_commandInterpreter.registerCommand( "!topten",    acceptedMessages, this, "doTopTen" );
        m_commandInterpreter.registerCommand( "!stats",     acceptedMessages, this, "doStats" );
        m_commandInterpreter.registerCommand( "!pm",        acceptedMessages, this, "doPm" );
        m_commandInterpreter.registerCommand( "!score",     acceptedMessages, this, "doScore" );

        m_commandInterpreter.registerDefaultCommand( Message.REMOTE_PRIVATE_MESSAGE, this, "doCheckPrivate" );
        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "doCheckPrivate" );
    }

    public void doStartGame( String name, String message ) {
        if( m_botAction.getOperatorList().isER( name ) || accessList.containsKey( name ) && gameProgress == -1 ) {
            curLeader = 0;
            questionNumber = 1;
            toWin = 10;

            try {
                toWin = Integer.parseInt( message );
                if( toWin < 1 || toWin > 500 ) toWin = 10;
            }
            catch (Exception e) {}
            gameProgress = 0;
            m_botAction.sendChatMessage(1, m_prec + "A game of Trivia is starting | Win by getting " + toWin + " pts!");
            m_botAction.sendChatMessage(1, m_prec + "  - This chat is for remote players, please PM me the answer.");
            m_botAction.sendChatMessage(1, m_prec + "  - Use !help in chat to get a list of commands.");
            m_botAction.sendArenaMessage(m_prec + "A game of Trivia is starting | Win by getting " + toWin + " pts!", 22);
            m_botAction.sendArenaMessage(m_prec + "  - Use !help sent privately for a list of commands..");
            startGame = new TimerTask() {
                public void run() {
                    grabQuestion();
                    displayQuestion();
                }
            };
            m_botAction.scheduleTask(startGame, 15000);
            doTimedArena();
        }
    }

    /****************************************************************/
    /*** Cancels the game, stores results.                        ***/
    /****************************************************************/

    public void doCancelGame( String name, String message) {
        if( (m_botAction.getOperatorList().isER( name ) || accessList.containsKey( name ) ) && gameProgress != -1 ){
            gameProgress = -1;
            m_botAction.sendChatMessage( 1, m_prec + "This game of Trivia has been canceled." );
            m_botAction.sendArenaMessage( m_prec + "This game of Trivia has been canceled." );
            playerMap.clear();
            m_botAction.cancelTasks();
        }
    }

    /****************************************************************/
    /*** Displays the Question/Category.                          ***/
    /****************************************************************/
    public void displayQuestion() {
        gameProgress = 1;
        m_botAction.sendChatMessage( 1, m_prec + "Question #" + questionNumber + " | Category: " + t_category );
        m_botAction.sendArenaMessage( m_prec + "Question #" + questionNumber + " | Category: " + t_category,103 );

        timerQuestion = new TimerTask() {
            public void run() {
                if( gameProgress == 1 ){
                    gameProgress = 2;
                    //Date d = new Date();
                    giveTime = new java.util.Date().getTime();
                    m_botAction.sendChatMessage( 1, m_prec + "Question: " + t_question );
                    m_botAction.sendArenaMessage( m_prec + "Question: " + t_question );
                    displayHint();
                }
            }
        };
        m_botAction.scheduleTask( timerQuestion, m_timeQuestion );
    }

    /****************************************************************/
    /*** Displays the Hint.                                       ***/
    /****************************************************************/
    public void displayHint() {
        timerHint = new TimerTask() {
            public void run() {
                if( gameProgress == 2 ) {
                    gameProgress = 3;
                    String hint = "";
                    if( f_answer.length() < 2 ) hint = "hey it is 1 character!";
                    else if( f_answer.length() < 8 ) hint = f_answer.substring(0,1);
                    else if( f_answer.length() < 12 ) hint = f_answer.substring(0,2);
                    else hint = f_answer.substring(0,3);
                    if( f_answer.toLowerCase().startsWith( "an " ) || f_answer.toLowerCase().startsWith( "the " ) || f_answer.toLowerCase().startsWith( "a " ) ) {
                        String pieces[] = f_answer.split( " " );
                        if( pieces.length > 1 )
                        hint = pieces[1].substring(0, 1);
                    }
                    m_botAction.sendChatMessage( 1, m_prec + "Hint: Starts with '" + hint + "'");
                    m_botAction.sendArenaMessage(m_prec + "Hint: Starts with '" + hint + "'");
                    displayAnswer();
                }
            }
        };
        m_botAction.scheduleTask( timerHint, m_timeHint );
    }

    /****************************************************************/
    /*** Displays the Answer.                                     ***/
    /****************************************************************/
    public void displayAnswer() {
        timerAnswer = new TimerTask() {
            public void run() {
                if( gameProgress == 3 ) {
                    gameProgress = 4;
                    m_botAction.sendChatMessage( 1, m_prec + "No one has given the correct answer of '" + f_answer + "'");
                    m_botAction.sendArenaMessage( m_prec + "No one has given the correct answer of '" + f_answer + "'", 103);
                    doCheckScores();
                    startNextRound();
                }
            }
        };
        m_botAction.scheduleTask( timerAnswer, m_timeAnswer );
    }

    /****************************************************************/
    /*** Starts the next round.                                   ***/
    /****************************************************************/
    public void startNextRound() {
        timerNext = new TimerTask() {
            public void run() {
                if( gameProgress == 4 ) {
                    questionNumber++;
                    grabQuestion();
                    displayQuestion();
                }
            }
        };
        m_botAction.scheduleTask( timerNext, m_timeNext );
    }

    /****************************************************************/
    /*** Ends the game, stores results.                           ***/
    /****************************************************************/
    public void doEndGame( String name ) {
        gameProgress = -1;
        curLeader = 0;
        questionNumber = 1;
        m_botAction.sendChatMessage(1, m_prec + name + " got the correct answer, '" + f_answer + "'" );
        m_botAction.sendArenaMessage( m_prec + name + " got the correct answer, '" + f_answer + "'" );
        m_botAction.sendChatMessage( 1, m_prec + "Player: " + name + " has won this round of trivia!" );
        m_botAction.sendArenaMessage( m_prec + "Player: " + name + " has won this round of trivia!", 5 );
        //Save stats
        Iterator<String> it = playerMap.keySet().iterator();
        while (it.hasNext()) {
            String curPlayer = it.next();
            PlayerProfile tempPlayer = playerMap.get(curPlayer);
            if( name.equals(curPlayer) )
                storePlayerStats( curPlayer, tempPlayer.getData( 0 ), true );
            else
                storePlayerStats( curPlayer, tempPlayer.getData( 0 ), false );
        }
        playerMap.clear();
        getTopTen();
        m_botAction.cancelTasks();
        toWin = 10;
    }

    public void doRepeat( String name, String message ){
        if( gameProgress == 4 ){
            m_botAction.sendSmartPrivateMessage( name, m_prec + "Question: " + t_question + "  ANSWER: " + t_answer );
        } else if( gameProgress >= 2 ){
            m_botAction.sendSmartPrivateMessage( name, m_prec + "Question: " + t_question );
        }
    }

    public void doHelp( String name, String message ){
        if( m_botAction.getOperatorList().isER( name ) ){
            m_botAction.remotePrivateMessageSpam( name, opmsg );
        }

        m_botAction.remotePrivateMessageSpam( name, helpmsg );
    }

    public void doTopTen( String name, String message ){
        m_botAction.sendSmartPrivateMessage( name, "Note: You must have at least 100 possible points to be ranked in the top ten.");
        if( topTen.size() == 0 ){
            m_botAction.sendSmartPrivateMessage( name, "No one has qualified yet!");
        } else {
            for( int i=0; i<topTen.size(); i++ ){
                m_botAction.sendSmartPrivateMessage( name, topTen.elementAt( i ) );
            }
        }
    }

    public void doStats( String name, String message ){
        if( gameProgress == -1 ){
            m_botAction.sendSmartPrivateMessage( name, "Displaying stats, please hold.");
            m_botAction.sendSmartPrivateMessage( name, "Note: You must have at least 100 possible points to be ranked in the top ten.");
            if( message.trim().length() > 0 ){
                m_botAction.sendSmartPrivateMessage( name, getPlayerStats( message ) );
            } else {
                m_botAction.sendSmartPrivateMessage( name, getPlayerStats( name ) );
            }
        } else {
            m_botAction.sendSmartPrivateMessage( name, "Please check your stats when Trivia is not running." );
        }
    }

    public void doPm( String name, String message ){
        m_botAction.sendSmartPrivateMessage( name, "Now you can use :: to submit your answers." );
    }

    public void doScore( String name, String message ){

        if( gameProgress != -1 ){
            m_botAction.sendRemotePrivateMessage( name, "This game is to " + toWin + " points." );
            m_botAction.sendRemotePrivateMessage( name, m_prec + doTrimString( "Current Scores", 28 ) + "|" );
            m_botAction.sendRemotePrivateMessage( name, m_prec + doTrimString( "Player Name", 18 ) + doTrimString( "Points", 10 ) + "|" );
            int curPoints = curLeader;
            while( curPoints != 0 ){
                Iterator<String> it = playerMap.keySet().iterator();
                while( it.hasNext() ){
                    String curPlayer = it.next();
                    twcore.core.stats.PlayerProfile tempPlayer;
                    tempPlayer = playerMap.get( curPlayer );
                    if( tempPlayer.getData( 0 ) == curPoints ){
                        m_botAction.sendRemotePrivateMessage( name, "--|-- " + doTrimString( curPlayer, 18 ) + doTrimString( "" + tempPlayer.getData( 0 ), 10 ) + "|" );
                    }
                }
                curPoints--;
            }
        }
    }

    /****************************************************************/
    /*** Checks private commands                                  ***/
    /****************************************************************/

    public void doCheckPrivate( String name, String message ) {
        if((gameProgress == 2) || (gameProgress == 3)) {
            StringTokenizer tokenizer = new StringTokenizer(t_answer, "|");
            while( tokenizer.hasMoreTokens() ) {//&& (gameProgress != 4 || gameProgress != -1)) {
                String curAns = tokenizer.nextToken();
                if((message.toLowerCase().equals(curAns.toLowerCase()))) {
                    if( playerMap.containsKey( name ) ) {
                        twcore.core.stats.PlayerProfile tempP = playerMap.get( name );
                        //data 0 stores the score.
                        tempP.incData( 0 );
                        if( tempP.getData( 0 ) >= toWin ) doEndGame( name );
                    }
                    else {
                        playerMap.put( name, new twcore.core.stats.PlayerProfile( name ) );
                        twcore.core.stats.PlayerProfile tempP = playerMap.get( name );
                        tempP.setData( 0, 1 );
                        if( tempP.getData( 0 ) >= toWin ) doEndGame( name );
                    }
                    twcore.core.stats.PlayerProfile tempP = playerMap.get( name );
                    if( gameProgress == 2 || gameProgress == 3 ) {
                        String trail = getRank( tempP.getData(0) );
                        m_botAction.sendChatMessage(1, m_prec + name + " got the correct answer, '"+ f_answer + "', " + trail);
                        m_botAction.sendArenaMessage(m_prec + name + " got the correct answer, '"+ f_answer + "', " + trail, 103 );
                    }
                    if( gameProgress != -1 ) {
                        gameProgress = 4;
                        doCheckScores();
                        startNextRound();
                    }
                    break;
                }
            }
        }
    }

    /****************************************************************/
    /*** Returns the correct string for pts and lead.             ***/
    /****************************************************************/
    public String getRank( int score ) {
        String speed = "in " + (new java.util.Date().getTime() - giveTime) / 1000.0 + " sec. ";
        String pts = " pts.";
        if( score == 1 ) pts = " pt.";
        if( score > curLeader ) {
            curLeader = score;
            return speed + "and is in the lead with " + score + pts;
        }
        else if( score == curLeader) return speed + "and is tied for the lead with " + score + pts;
        else return speed + "and has " + score + pts;
    }

    /****************************************************************/
    /*** Shows scores.                                            ***/
    /****************************************************************/
    public void doCheckScores() {
        if(Math.round(questionNumber / 5.0) == (questionNumber / 5.0)) {
            int numberShown = 0, curPoints = curLeader;
            m_botAction.sendChatMessage(1, "--|-------------------------------|");
            m_botAction.sendChatMessage(1, "--|-- " + doTrimString("     Current Scores",28) + "|");
            m_botAction.sendChatMessage(1, "--|-- " + doTrimString("Player Name", 20) + doTrimString("Points", 8) + "|");
            while( numberShown < 8 && curPoints != 0) {
                Iterator<String> it = playerMap.keySet().iterator();
                while (it.hasNext()) {
                    String curPlayer = it.next();
                    twcore.core.stats.PlayerProfile tempPlayer;
                    tempPlayer = playerMap.get(curPlayer);
                    if(tempPlayer.getData(0) == curPoints) {
                        numberShown++;
                        m_botAction.sendChatMessage(1, "--|-- " + doTrimString(curPlayer,20) + doTrimString("" + tempPlayer.getData( 0 ), 8) + "|");
                    }
                }
                curPoints--;
            }
            if(curPoints != 0)
                m_botAction.sendChatMessage(1, "--|-- Low scores not shown.       |");
            m_botAction.sendChatMessage(1, "--|-------------------------------|");
            //Public Chat
            numberShown = 0;
            curPoints = curLeader;
            m_botAction.sendArenaMessage("--|-------------------------------|");
            m_botAction.sendArenaMessage("--|-- " + doTrimString("     Current Scores",28) + "|");
            m_botAction.sendArenaMessage("--|-- " + doTrimString("Player Name", 20) + doTrimString("Points", 8) + "|");
            while( numberShown < 8 && curPoints != 0) {
                Iterator<String> it = playerMap.keySet().iterator();
                while (it.hasNext()) {
                    String curPlayer = it.next();
                    twcore.core.stats.PlayerProfile tempPlayer;
                    tempPlayer = playerMap.get(curPlayer);
                    if(tempPlayer.getData(0) == curPoints) {
                        numberShown++;
                        m_botAction.sendArenaMessage("--|-- " + doTrimString(curPlayer,20) + doTrimString("" + tempPlayer.getData( 0 ), 8) + "|");
                    }
                }
                curPoints--;
            }
            if(curPoints != 0)
                m_botAction.sendArenaMessage("--|-- Low scores not shown.       |");
            m_botAction.sendArenaMessage("--|-------------------------------|");
        }
    }

    /****************************************************************/
    /*** Just adds blank space for alignment.                     ***/
    /****************************************************************/
    public String doTrimString(String fragment, int length) {
        if(fragment.length() > length)
            fragment = fragment.substring(0,length-1);
        else {
            for(int i=fragment.length();i<length;i++)
                fragment = fragment + " ";
        }
        return fragment;
    }

    public void handleEvent( Message event ){
        m_commandInterpreter.handleEvent( event );
    }

    /****************************************************************/
    /*** For periodic messages...                                 ***/
    /****************************************************************/

    public void doTimedArena() {
        timedMessages = new TimerTask() {
            public void run() {
                if(gameProgress != -1) {
                    m_botAction.sendArenaMessage("Please PM your answers to TriviaBot.");
                    m_botAction.sendChatMessage(1, "Please PM your answers to TriviaBot. Hint: use !pm");
                    doTimedArena();
                }
            }
        };
        m_botAction.scheduleTask( timedMessages, m_timePer1 );
    }

    /****************************************************************/
    /*** Gets a question from the database.                       ***/
    /****************************************************************/
    public void grabQuestion(){
        if (m_mintimesused == -1) getMinTimesUsed();
        try {
            ResultSet qryQuestionData;
            do {
                qryQuestionData = m_botAction.SQLQuery( mySQLHost, "SELECT fnQuestionID, fcQuestion, fcAnswer, fcQuestionTypeName FROM tblQuestion, tblQuestionType WHERE tblQuestion.fnQuestionTypeID = tblQuestionType.fnQuestionTypeid AND fnTimesUsed="+m_mintimesused+" ORDER BY RAND("+m_rnd.nextInt()+") LIMIT 1");
                if (!qryQuestionData.isBeforeFirst()) getMinTimesUsed();
            } while (!qryQuestionData.isBeforeFirst());

            if( qryQuestionData.next() ) {
                t_category = qryQuestionData.getString("fcQuestionTypeName").replaceAll("\\\\", "");
                t_question = qryQuestionData.getString("fcQuestion").replaceAll("\\\\", "");
                t_answer   = qryQuestionData.getString("fcAnswer").replaceAll("\\\\", "");
                int ID = qryQuestionData.getInt("fnQuestionID");
                m_botAction.SQLQueryAndClose( mySQLHost, "UPDATE tblQuestion SET fnTimesUsed = fnTimesUsed + 1 WHERE fnQuestionID = "+ ID);
            }
            m_botAction.SQLClose( qryQuestionData );
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }

        StringTokenizer tokenizer = new StringTokenizer(t_answer, "|");
        f_answer = tokenizer.nextToken();
    }

    /****************************************************************/
    /*** Gets minimum times used for questions.                   ***/
    /****************************************************************/
    public void getMinTimesUsed() {
        try {
            ResultSet qryMinTimesUsed = m_botAction.SQLQuery( mySQLHost, "SELECT MIN(fnTimesUsed) AS fnMinTimesUsed FROM tblQuestion");
            qryMinTimesUsed.next();
            int minUsed = qryMinTimesUsed.getInt("fnMinTimesUsed");
            m_mintimesused = minUsed;
            m_botAction.SQLClose( qryMinTimesUsed );
        } catch (Exception e) {
            Tools.printStackTrace(e);
            m_mintimesused = -1;
        }
    }

    /****************************************************************/
    /*** Gets the current Topten.                                 ***/
    /****************************************************************/
    public void getTopTen() {
        topTen = new Vector<String>();
        try {
            ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fcUserName, fnPoints, fnPlayed, fnWon, fnPossible, fnRating FROM tblUserTriviaStats WHERE fnPossible >= 100 ORDER BY fnRating DESC LIMIT 10");
            while(result != null && result.next())
                topTen.add(doTrimString(result.getString("fcUsername"), 17 ) + "Games Won ("+ doTrimString(""+result.getInt("fnWon") +":" + result.getInt("fnPlayed") +")",9) + "Pts Scored (" +doTrimString(""+ result.getInt("fnPoints") + ":" + result.getInt("fnPossible") + ")", 10) + "Rating: " + result.getInt("fnRating"));
            m_botAction.SQLClose( result );
        }
        catch (Exception e){}
    }

    /****************************************************************/
    /*** Stores player statistics.                                ***/
    /****************************************************************/

    public void storePlayerStats( String username, int points, boolean won ) {
        try {
            int wonAdd = 0;
            if (won) wonAdd = 1;

            ResultSet qryHasTriviaRecord = m_botAction.SQLQuery( mySQLHost, "SELECT fcUserName, fnPlayed, fnWon, fnPoints, fnPossible FROM tblUserTriviaStats WHERE fcUserName = \"" + username+"\"");
            if (!qryHasTriviaRecord.next()) {
                double rating = ( (points+.0) / toWin * 750.0 ) * ( 1.0 + (wonAdd / 3.0) );
                m_botAction.SQLQueryAndClose( mySQLHost, "INSERT INTO tblUserTriviaStats(fcUserName, fnPlayed, fnWon, fnPoints, fnPossible, fnRating) VALUES (\""+username+"\",1,"+wonAdd+","+points+","+toWin+","+rating+")");
            } else {
                double played = qryHasTriviaRecord.getInt("fnPlayed") + 1.0;
                double wins   = qryHasTriviaRecord.getInt("fnWon") + wonAdd;
                double pts    = qryHasTriviaRecord.getInt("fnPoints") + points;
                double pos    = qryHasTriviaRecord.getInt("fnPossible") + toWin;
                double rating = ( pts / pos * 750.0) * ( 1.0 + (wins / played / 3.0) );
                m_botAction.SQLQueryAndClose( mySQLHost, "UPDATE tblUserTriviaStats SET fnPlayed = fnPlayed+1, fnWon = fnWon + "+wonAdd+", fnPoints = fnPoints + "+ points+", fnPossible = fnPossible + "+ toWin +", fnRating = "+rating+" WHERE fcUserName = \"" + username+"\"");
            }
            m_botAction.SQLClose( qryHasTriviaRecord );
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }

    /****************************************************************/
    /*** Gets player statistics.                                  ***/
    /****************************************************************/
    public String getPlayerStats(String username) {
        try{

            ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fnPoints, fnWon, fnPlayed, fnPossible, fnRating FROM tblUserTriviaStats WHERE fcUserName = \"" + username+"\"");
            String info = "There is no record of player " + username;
            if(result.next())
                info = username + "- Games Won: ("+ result.getInt("fnWon") + ":" + result.getInt("fnPlayed") + ")  Pts Scored: (" +result.getInt("fnPoints") + ":" + result.getInt("fnPossible") + ")  Rating: " + result.getInt("fnRating");
            m_botAction.SQLClose( result );
            return info;
        } catch (Exception e) {
            Tools.printStackTrace(e);
            return "Can't retrieve stats.";
        }
    }
}
