package twcore.bots.gamebot.pictionary;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;
import java.util.ArrayList;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.StringBag;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.stats.PlayerProfile;
import twcore.core.util.Tools;

/**
 * Pictionary.
 *
 * @author milosh - 
 * 12.07
 */
public class pictionary extends MultiModule {

    CommandInterpreter  m_commandInterpreter;

    Random          m_rnd;
    String          mySQLHost = "local";
    TimerTask       timerQuestion, timerHint, timerAnswer, timerNext, warn, forcePass;
     
    TreeMap<String, PlayerProfile> playerMap = new TreeMap<String, PlayerProfile>();
    HashMap<String, String> accessList = new HashMap<String, String>();
    HashMap<String, Integer> votes = new HashMap<String, Integer>();
    Vector<String>  topTen;
    ArrayList<String> cantPlay = new ArrayList<String>();
    ArrayList<String> notPlaying = new ArrayList<String>();
    ArrayList<String> hasVoted = new ArrayList<String>();
    int             gameProgress = -1, toWin = 10, pictNumber = 1, curLeader = 0;
    int             m_mintimesused = -1, vote;
    int				minPlayers, XSpot, YSpot;				

    int             m_timeQuestion, m_timeHint, m_timeAnswer, admireArt;
    
    double          giveTime, answerSpeed;
    String          m_prec = "-- ", gameType="Bot's Pick.", game="default";
    String          t_definition, t_word, curArtist, theWinner, lastWord;
    boolean			custGame = false, ready = false, isVoting = false;
    String[]        helpmsg =
    { "Commands:",
      "!help          -- displays this.",
      "!rules         -- displays the rules.",
      "!lagout        -- puts you back in the game if you're drawing.",
      "!pass          -- gives your drawing turn to a random player.",
      "!score         -- displays the current scores.",
      "!repeat        -- will repeat the hint or answer.",
      "!stats         -- will display your statistics.",
      "!stats <name>  -- displays <name>'s statistics.",
      "!topten        -- displays top ten player stats."
    };
    String[]		automsg =
    { "Commands:",
    	      "!help          -- displays this.",
    	      "!rules         -- displays the rules.",
    	      "!lagout        -- puts you back in the game if you're drawing.",
    	      "!pass          -- gives your drawing turn to a random player.",
    	      "!score         -- displays the current scores.",
    	      "!repeat        -- will repeat the hint or answer."/*,
    	      "!stats         -- will display your statistics.",
    	      "!stats <name>  -- displays <name>'s statistics.",
    	      "!topten        -- displays top ten player stats."*/
    	    };
    String[]        opmsg =
    { "Moderator Commands:",
      "!start                  -- Starts a default game of Pictionary to 10.",
      "!start <num>:<type>     -- Starts a game to <num> points of <type>(Custom words/Default).",
      "                        -- (e.g. !start custom:15)",
      "!cancel                 -- Cancels this game of Pictionary.",
      "!showanswer             -- Shows you the answer.",
      "!displayrules		   -- Shows the rules in *arena messages."
    };
    String[]		autoopmsg =
    {"Moderator Commands:",
    	      "!cancel                 -- Cancels this game of Pictionary.",
    	      "!showanswer             -- Shows you the answer."
    		
    };
    String[]        regrules =
    {
    	"Rules: Racism and pornography are strictly forbidden. The bot will designate",
    	"an artist. Players attempt to guess what the artist is drawing before the time",
    	"ends. Get " + toWin + " points to win.",
    	"-Note: You must have at least 100 possible points to gain Top Ten status.",
    	"-Questions? Comments? Suggestions? Bugs?: miloshtw@gmail.com"
    };
    String[]		displayrules =
    {
    	"''''RULES: Racism and pornography are strictly forbidden. The bot will designate  ''''",
        "'''  an artist. Players attempt to guess what the artist is drawing before the time'''",
        "''   ends to gain points. If you guess correctly then it is your turn to draw       ''",
        "'    The first player to reach " + toWin + " points wins the game.                   '"
    };
    String[]        oprules =
    {
    	"Moderator Rules:",
    	"-Only start games to (1-25).",
    	"-!showanswer will prohibit you from answering that round.",
    	"-Do not abuse !showanswer by giving other players or moderators the answer.",
    	"-----------------------------------------------------------------------------"
    };
    String[]        autooprules =
    {
    	"Moderator Rules:",
    	"-!showanswer will prohibit you from answering that round.",
    	"-Do not abuse !showanswer by giving other players or moderators the answer.",
    	"-----------------------------------------------------------------------------"
    };

    public void init() {
        m_commandInterpreter = new CommandInterpreter( m_botAction );
        getTopTen();
        registerCommands();
        m_rnd = new Random();
        BotSettings m_botSettings = moduleSettings;
        try{XSpot = Integer.parseInt(m_botSettings.getString("X"));}catch(Exception e){XSpot = 500;}
        try{YSpot = Integer.parseInt(m_botSettings.getString("Y"));}catch(Exception e){YSpot = 500;}
        try{minPlayers = Integer.parseInt(m_botSettings.getString("Minimum"));}catch(Exception e){minPlayers=5;}
        try{m_timeQuestion = Integer.parseInt(m_botSettings.getString("Question")) * 1000;}catch(Exception e){m_timeQuestion = 2000;}
        try{m_timeHint = Integer.parseInt(m_botSettings.getString("Hint")) * 1000;}catch(Exception e){m_timeHint=60000;}
        try{m_timeAnswer = Integer.parseInt(m_botSettings.getString("Answer")) * 1000;}catch(Exception e){m_timeAnswer=100000;}
        try{admireArt = Integer.parseInt(m_botSettings.getString("Admire")) * 1000;}catch(Exception e){admireArt=5000;}
        String access[] =  m_botSettings.getString("SpecialAccess").split( ":" );
        for( int i = 0; i < access.length; i++ )
            accessList.put( access[i], access[i] );
        if(autoStart)doVote();

    }

    public void doVote(){
		votes.clear();
		isVoting = true;
		m_botAction.sendArenaMessage("Vote game type: 1 - Bot picks words, 2 - Player's choice");
		TimerTask endVote = new TimerTask(){
			public void run(){
				isVoting = false;
				vote = countVote(2);
				if (vote == 1){
					game = "default";
					gameType = "Bot's pick.";
				}
				else if(vote == 2){
					game = "custom";
					gameType = "Player's choice.";
				}
				
				m_botAction.sendArenaMessage("Game Type: " + gameType);
				doStartGame(m_botAction.getBotName(),10 + game);
				
			}
		};
		m_botAction.scheduleTask(endVote, 15000);
    }
    
    public void handleVote(String name, String message){
    	try{
    	int vNum = Integer.parseInt(message);    	
    	if(vNum >= 1 && vNum <= 2){
    		if(!hasVoted.contains(name)){
    			votes.put(name, vNum);
    			hasVoted.add(name);
    		}
    		else {
    			m_botAction.sendSmartPrivateMessage( name, "You can only vote once.");
    		}
    	}
    	}catch(Exception e){}
    }
	public int countVote(int range) {
		int winner = 0;
		int[] counters = new int[range+1];
		Iterator<Integer> i = votes.values().iterator();

		while (i.hasNext()) {
			counters[i.next().intValue()]++;
		}

		for (int x = 1; x < counters.length; x++) {

			if (counters[winner] < counters[x]) {
				winner = x;
			}
		}
		return winner;
	}
    
    public void requestEvents(ModuleEventRequester events) {}
	public  String[] getModHelpMessage(){if(!autoStart)return opmsg;else return autoopmsg;}
	public boolean isUnloadable() {return true;}
    
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
        
        acceptedMessages = Message.PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand( "!start",     acceptedMessages, this, "doStartGame" );
        m_commandInterpreter.registerCommand( "!cancel",    acceptedMessages, this, "doCancelGame" );
        m_commandInterpreter.registerCommand( "!showanswer",acceptedMessages, this, "doShowAnswer" );
        m_commandInterpreter.registerCommand( "!displayrules",acceptedMessages, this, "doArenaRules" );
        
        acceptedMessages = Message.PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand( "!ready",     acceptedMessages, this, "doReady" );
        m_commandInterpreter.registerCommand( "!pass",		acceptedMessages, this, "doPass" );
        m_commandInterpreter.registerCommand( "!help",      acceptedMessages, this, "doHelp" );
        m_commandInterpreter.registerCommand( "!notplaying",acceptedMessages, this, "doNotPlaying" );
        if(!autoStart)m_commandInterpreter.registerCommand( "!topten",    acceptedMessages, this, "doTopTen" );
        if(!autoStart)m_commandInterpreter.registerCommand( "!stats",     acceptedMessages, this, "doStats" );
        m_commandInterpreter.registerCommand( "!score",     acceptedMessages, this, "doScore" );
        m_commandInterpreter.registerCommand( "!myscore",   acceptedMessages, this, "doMyScore" );
        m_commandInterpreter.registerCommand( "!rules",     acceptedMessages, this, "doRules" );
        m_commandInterpreter.registerCommand( "!lagout",    acceptedMessages, this, "doLagout" );

        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "doCustomWord" );
    }
  
    
    /****************************************************************/
    /*** Puts the artist back in if he lags out                   ***/
    /****************************************************************/
    
    public void doLagout ( String name, String message ){
    	if (name.equals(curArtist) && (gameProgress == 3 || gameProgress == 2)){
            m_botAction.setShip(curArtist, 1);
            m_botAction.warpTo(curArtist,XSpot,YSpot);
    	}
    	
    }
    
    /****************************************************************/
    /*** Puts the artist back in if he lags out                   ***/
    /****************************************************************/
    
    public void doNotPlaying ( String name, String message ){
    	if ((gameProgress == 3 || gameProgress == 2) && name.equalsIgnoreCase(curArtist))
    		doPass(curArtist, "");
        if(!notPlaying.contains(name)){
        	notPlaying.add(name);
        	m_botAction.sendSmartPrivateMessage( name, "Not playing enabled. You will not be allowed to draw or guess.");
        }
        else {
        	notPlaying.remove(name);
        	m_botAction.sendSmartPrivateMessage( name, "Not playing disabled. You are now allowed to draw and guess.");
        }
    }
    
    /****************************************************************/
    /*** Displays help message.                                   ***/
    /****************************************************************/
    
    public void doHelp( String name, String message ){
    	if(!autoStart)m_botAction.smartPrivateMessageSpam( name, helpmsg );
    	else m_botAction.smartPrivateMessageSpam( name, automsg );
    }   
    
    /****************************************************************/
    /*** Displays the rules.                                      ***/
    /****************************************************************/
    
    public void doRules( String name, String message) {
        if( m_botAction.getOperatorList().isModerator( name ) ){
            if (!autoStart)m_botAction.smartPrivateMessageSpam( name, oprules );
            else m_botAction.smartPrivateMessageSpam( name, autooprules );
        }

        m_botAction.smartPrivateMessageSpam( name, regrules );
    }
    
    /****************************************************************/
    /*** Displays the rules.                                      ***/
    /****************************************************************/
    
    public void doArenaRules( String name, String message) {
    	if( m_botAction.getOperatorList().isModerator( name ) || accessList.containsKey( name )) {
    		m_botAction.arenaMessageSpam(displayrules);
    	}
    }
    
    /****************************************************************/
    /*** Starts the game.                                         ***/
    /****************************************************************/
    
    public void doStartGame( String name, String message ) {
        if( m_botAction.getOperatorList().isModerator( name ) || accessList.containsKey( name ) && gameProgress == -1 ) {
            curLeader = 0;
            pictNumber = 1;
            toWin = 10;
            String[] msg = message.split(":");
            try {
            	if ( msg[1].trim().equalsIgnoreCase("custom")){
            		custGame = true;
            		
            	}
            	else if ( msg[1].trim().equalsIgnoreCase("default") ){
            		custGame = false;
            	}
            }
            catch (Exception e) {custGame = false;}
            try {
                toWin = Integer.parseInt( msg[0] );
                if( toWin < 1 || toWin > 25 ) toWin = 10;
            }
            catch (Exception e) {toWin = 10;}
            gameProgress = 0;
            m_botAction.specAll();
            if(autoStart)doArenaRules(m_botAction.getBotName(),"");
            
            if (m_botAction.getArenaSize() >= minPlayers){
            	if(!autoStart){
            		m_botAction.sendArenaMessage(m_prec + "A game of Pictionary is starting | Win by getting " + toWin + " pts!", 22);
            		m_botAction.sendArenaMessage(m_prec + "Type your guesses in public chat.");
            		m_botAction.sendArenaMessage(m_prec + "PM !notplaying to " + m_botAction.getBotName() + " if you don't want to play.");
            	}
            	gameProgress = 1;
            	pickPlayer();
            	grabWord();
            	if (!custGame)
            		doReadyCheck();

            }
            else {
            	m_botAction.sendArenaMessage("There aren't enough players to play!", 13);
            	int pNeed = minPlayers - m_botAction.getArenaSize();
            	if ( pNeed > 1 ){
            		m_botAction.sendArenaMessage(m_prec + "Pictionary will begin when " + pNeed + " more people enter.");
            	}
            	else {
            		m_botAction.sendArenaMessage(m_prec + "Pictionary will begin when " + pNeed + " more person enters.");
            	}
            }
        }
    }   
    
    /****************************************************************/
    /*** Cancels the game, stores results.                        ***/
    /****************************************************************/

    public void doCancelGame( String name , String message) {
        if( (m_botAction.getOperatorList().isModerator( name ) || accessList.containsKey( name ) ) && gameProgress != -1 ){
            gameProgress = -1;
            m_botAction.sendArenaMessage( m_prec + "This game of Pictionary has been canceled." );
            playerMap.clear();
            if (autoStart){
            	m_botAction.cancelTasks();
            	m_botAction.sendPrivateMessage(m_botAction.getBotName(), "!unlock");
            }
        }
    }
    
    /****************************************************************/
    /*** Shows the answer(Operator Access)                        ***/
    /****************************************************************/
    public void doShowAnswer( String name, String message ) {
    	if( m_botAction.getOperatorList().isModerator( name ) || accessList.containsKey( name ) ){
    		m_botAction.sendSmartPrivateMessage( name, "The answer is " + t_word + "." );
    		cantPlay.add(name);
    	}
    }
    
    /****************************************************************/
    /*** Passes the artist's turn to a random player.             ***/
    /****************************************************************/

    public void doPass( String name, String message ) {
    	if ((name.equals(curArtist) || opList.isZH(name)) && gameProgress < 4 && gameProgress > 0){
    		String passing = curArtist;
            m_botAction.spec(curArtist);
            m_botAction.spec(curArtist);
            while (passing.equals(curArtist)){pickPlayer();}
    		m_botAction.sendArenaMessage(passing + " passes to " + curArtist + ".");
    		cantPlay.clear();
    		cantPlay.add(curArtist);
    		try{m_botAction.cancelTasks();}catch(Exception e){}
    		grabWord();
            doReadyCheck();    		
    	}
    }
    
    /**
     * Selects a random player in the arena.
     */
    public void pickPlayer() {
    		//pick a random player.   
            Player p;
            String addPlayerName;
            StringBag randomPlayerBag = new StringBag();
            if (m_botAction.getArenaSize() > minPlayers)
            {
            	Iterator<Player> i = m_botAction.getPlayerIterator();
            	if( i == null ) return;
            	while( i.hasNext() ){
            		p = (Player)i.next();
            		addPlayerName = p.getPlayerName();
            		randomPlayerBag.add(addPlayerName);
            	}
            	addPlayerName = randomPlayerBag.grabAndRemove();
            	if (!addPlayerName.equals(m_botAction.getBotName()) && !notPlaying.contains(addPlayerName))
            		curArtist = addPlayerName;
            	else pickPlayer();
            	cantPlay.add(curArtist);
            }
            else {
            	m_botAction.sendArenaMessage(m_prec + "There are not enough players to procede.");
            	doCancelGame(m_botAction.getBotName(), "");
            }
    	
    }
 
    /****************************************************************/
    /*** Checks to see if the player is ready to draw.            ***/
    /****************************************************************/
    public void doReadyCheck(){
    	if (ready){
    		try{m_botAction.cancelTasks();}catch(Exception e){}    		
    		ready = false;
    		doDraw();
    	}
    	else {
    		warn = new TimerTask() {
    			public void run(){
    				m_botAction.sendSmartPrivateMessage(curArtist, "Private message me with !ready or your turn will be forfeited.");
    				m_botAction.scheduleTask(forcePass, 15000);
    			}
    		};
    		forcePass = new TimerTask() {
    			public void run(){
    				doPass(m_botAction.getBotName(), "");
    			}
    		};
    		if (custGame){
    			m_botAction.sendSmartPrivateMessage(curArtist, "Private message me with !ready to begin or !pass to pass.");
    			m_botAction.scheduleTask(warn, 15000);
    		}
    		else {	
    			m_botAction.sendSmartPrivateMessage(curArtist, "You've been chosen to draw. Please private message me with !ready to begin or !pass to pass.");
    			m_botAction.scheduleTask(warn, 15000);
    		}
    	} 	
    }
    public void doReady( String name, String message) {
    	if (name.equals(curArtist)){
    		ready = true;
    		if (t_word.equals(lastWord)) {
    			custGame = false;
    			grabWord();
    			custGame = true;
    		}
    		try{m_botAction.cancelTasks();}catch(Exception e){}
    		doReadyCheck();
    	}
    	
    }
    
    
    /****************************************************************/
    /*** Drawing Begins.                                          ***/
    /****************************************************************/
    public void doDraw() { 
        m_botAction.sendArenaMessage( m_prec + "Picture #" + pictNumber + ":");
        m_botAction.setShip(curArtist, 1);
        m_botAction.warpTo(curArtist, XSpot, YSpot);
        m_botAction.sendSmartPrivateMessage(curArtist, "Draw: " + t_word);
        timerQuestion = new TimerTask() {
            public void run() {
                if( gameProgress == 1 ){
                    gameProgress = 2;
                    //Date d = new Date();
                    giveTime = new java.util.Date().getTime();
                    m_botAction.sendArenaMessage( m_prec + "GO GO GO!!!",104 );
                    lastWord = t_word;
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
                    m_botAction.sendArenaMessage(m_prec + "Hint: " + t_definition);
                	
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
                    m_botAction.sendArenaMessage( m_prec + "No one has given the correct answer of '" + t_word + "'", 103);
                   
                    doCheckScores();
                    theWinner = m_botAction.getBotName();
                    startNextRound();
                }
            }
        };
        
        m_botAction.scheduleTask( timerAnswer, m_timeAnswer );
    }
  
    /****************************************************************/
    /*** Gets a word from the database.                           ***/
    /****************************************************************/
    public void grabWord(){
        if (m_mintimesused == -1) getMinTimesUsed();
        if ( !custGame ) {
        	try {
        		ResultSet qryWordData;
                	qryWordData = m_botAction.SQLQuery( mySQLHost, "SELECT WordID, Word FROM tblPict_Words WHERE TimesUsed=9 ORDER BY RAND("+m_rnd.nextInt()+") LIMIT 1");
                	if ( qryWordData.next() ) {
                		t_word = qryWordData.getString("Word").toLowerCase();
                		if (t_word.length() < 4){
                			grabWord();
                		}
                		if (!(t_word.trim().split(" ").length > 1))
                			t_definition = "The word begins with '" + t_word.substring(0,1) + "'.";
                		else
                			t_definition = t_word.trim().split(" ").length + " words: First word begins with '" + t_word.substring(0,1) + "'.";
                		int ID = qryWordData.getInt("WordID");
                		m_botAction.SQLQuery( mySQLHost, "UPDATE tblPict_Words SET TimesUsed = TimesUsed + 1 WHERE WordID = "+ ID);
                	}
                	m_botAction.SQLClose( qryWordData );                	
        	} catch (Exception e) {
        		Tools.printStackTrace(e);
        	}        	
        }
        else {
        	m_botAction.sendSmartPrivateMessage( curArtist, "Private message me what you're drawing or type !ready for me to pick something for you.");        	
        }

    }    
    
    /****************************************************************/
    /*** Gets a custom word from the artist.                      ***/
    /****************************************************************/
    public void doCustomWord (String name, String message) {
    	if (name.equalsIgnoreCase(curArtist) && custGame && gameProgress == 1){
    		if (message.length() < 13){
    			//TODO:Racism check
    				t_word = message.toLowerCase().trim();
    				t_definition = "The word begins with '" + t_word.substring(0,1) + "'.";
    				m_botAction.sendSmartPrivateMessage(curArtist, "Word to draw: " + t_word);
    				doReadyCheck();
    			/*
    			else {
    				m_botAction.sendSmartPrivateMessage( curArtist, "Racist words are not allowed.");
    			}*/
    		}
    		else
    			m_botAction.sendSmartPrivateMessage(curArtist, "Please pick a word of 12 letters or less.");
    	}
    }
    
    /****************************************************************/
    /*** Checks Team Chat for answer.                             ***/
    /****************************************************************/

    public void doCheckAnswers( String name, String message ) {
    	if(isVoting)
    		handleVote(name,message);
        if((gameProgress == 2) || (gameProgress == 3)) {
        	String curAns = t_word.replaceAll(" ", "");
            String msg = message.toLowerCase().replaceAll(" ", "");
            if(msg.contains(curAns)) {
                if(!cantPlay.contains(name) && !notPlaying.contains(name))
                {
                	theWinner = name;
                    answerSpeed = (new java.util.Date().getTime() - giveTime) / 1000.0;
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
                	if(answerSpeed < 5){
                		String trail = getRank( tempP.getData(0) );
                		m_botAction.sendArenaMessage(m_prec + "Cheater! " + name + " got the correct answer, '"+ t_word + "' in only " + trail, 13 );                    	
                	}
                	else if(answerSpeed < 25 && answerSpeed > 5){
               			String trail = getRank( tempP.getData(0) );
                		m_botAction.sendArenaMessage(m_prec + "Inconceivable! " + name + " got the correct answer, '"+ t_word + "' in only " + trail, 7 );                  		
                	}
               		else {
                		String trail = getRank( tempP.getData(0) );
                		m_botAction.sendArenaMessage(m_prec + name + " got the correct answer, '"+ t_word + "', " + trail, 103 );
               		}
                	if( gameProgress != -1 ) {
                		gameProgress = 4;
                		m_botAction.cancelTasks();
                		TimerTask adm = new TimerTask() {
                			public void run(){
                				startNextRound();
                			}
                		};
               			m_botAction.scheduleTask(adm, admireArt);
               		}
               	}
                else {
               		if (name.equals(curArtist)){
               			m_botAction.sendSmartPrivateMessage(name, "A point has been deducted from your score for showing the answer.");
               			if(playerMap.containsKey(curArtist)){
               				PlayerProfile tempPlayer = playerMap.get(curArtist);
               				tempPlayer.decData(0);
               			}
               			else {
               				playerMap.put(curArtist, new PlayerProfile(curArtist));
               				PlayerProfile tempPlayer = playerMap.get(curArtist);
               				tempPlayer.setData(0, -1);
               			}
               		}
                	else {
                		m_botAction.sendSmartPrivateMessage(name, "You are not allowed to guess.");
                	}
                }
            }
        }
    }
        

    /****************************************************************/
    /*** Returns the correct string for pts and lead.             ***/
    /****************************************************************/
    public String getRank( int score ) {
    	String secs = " seconds ";
    	if (answerSpeed ==1)
    		secs = " second ";
    	String speed = answerSpeed + secs;	
        String pts = " points";
        if( score == 1 ) pts = " point";
        if( score > curLeader ) {
            curLeader = score;
            return speed + "and is in the lead with " + score + pts;
        }
        else if( score == curLeader) return speed + "and is tied for the lead with " + score + pts + "!";
        else return speed + "and has " + score + pts + ".";
    }

    /****************************************************************/
    /*** Shows scores.                                            ***/
    /****************************************************************/
    public void doCheckScores() {

    	if((pictNumber % 5 == 0) && (curLeader != 0)) {
    		String m_prec2 = m_prec.replaceAll(" ","");
    		int numberShown = 0, curPoints = curLeader;    		
            m_botAction.sendArenaMessage(m_prec2 + "-----------------------------|");
            m_botAction.sendArenaMessage(m_prec + doTrimString("Top Scores",28) + "|");
            m_botAction.sendArenaMessage(m_prec2 + "-----------------------------|");
            m_botAction.sendArenaMessage(m_prec + doTrimString("Player Name", 20) + doTrimString("Points", 8) + "|");
            while (numberShown < 5 && curPoints != 0){
                Set<String> set = playerMap.keySet();
                Iterator<String> it = set.iterator();
                while (it.hasNext() && numberShown < 5) {
                    String curPlayer = (String) it.next();
                    twcore.core.stats.PlayerProfile tempPlayer;
                    tempPlayer = playerMap.get(curPlayer);
                    if(tempPlayer.getData(0) == curPoints) {
                        numberShown++;
                        m_botAction.sendArenaMessage(m_prec + doTrimString(curPlayer,20) + doTrimString("" + tempPlayer.getData( 0 ), 8) + "|");
                    }
                }
                curPoints--;
            }
            m_botAction.sendArenaMessage(m_prec2 + "-----------------------------|");
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
        int mType = event.getMessageType();
        if(mType == Message.PUBLIC_MESSAGE ||
           mType == Message.PUBLIC_MACRO_MESSAGE ||
           mType == Message.TEAM_MESSAGE ||
           mType == Message.OPPOSING_TEAM_MESSAGE){
        	doCheckAnswers(m_botAction.getPlayerName(event.getPlayerID()), event.getMessage());
        }
        else {
        	m_commandInterpreter.handleEvent( event );
        }
    }

    /****************************************************************/
    /*** Gets minimum times used for questions.                   ***/
    /****************************************************************/
    public void getMinTimesUsed() {
        try {
            ResultSet qryMinTimesUsed = m_botAction.SQLQuery( mySQLHost, "SELECT MIN(TimesUsed) AS MinTimesUsed FROM tblScramble_Nerd");
            if ( qryMinTimesUsed.next() ) {
            int minUsed = qryMinTimesUsed.getInt("MinTimesUsed");
            m_mintimesused = minUsed;
            m_botAction.SQLClose( qryMinTimesUsed );
            }
            
        } catch (Exception e) {
            Tools.printStackTrace(e);
            m_mintimesused = -1;
        }
    }

    /****************************************************************/
    /*** Gets the current Top ten.                                 ***/
    /****************************************************************/
    public void getTopTen() {
        topTen = new Vector<String>();
        try {
            ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fcUserName, fnPoints, fnPlayed, fnWon, fnPossible, fnRating FROM tblPict_UserStats WHERE fnPossible >= 100 ORDER BY fnRating DESC LIMIT 10");
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

            ResultSet qryHasScrambleRecord = m_botAction.SQLQuery( mySQLHost, "SELECT fcUserName, fnPlayed, fnWon, fnPoints, fnPossible FROM tblPict_UserStats WHERE fcUserName = \"" + username+"\"");
            if (!qryHasScrambleRecord.next()) {
                double rating = ( (points+.0) / toWin * 750.0 ) * ( 1.0 + (wonAdd / 3.0) );
                m_botAction.SQLQueryAndClose( mySQLHost, "INSERT INTO tblPict_UserStats(fcUserName, fnPlayed, fnWon, fnPoints, fnPossible, fnRating) VALUES (\""+username+"\",1,"+wonAdd+","+points+","+toWin+","+rating+")");
            } else {
                double played = qryHasScrambleRecord.getInt("fnPlayed") + 1.0;
                double wins   = qryHasScrambleRecord.getInt("fnWon") + wonAdd;
                double pts    = qryHasScrambleRecord.getInt("fnPoints") + points;
                double pos    = qryHasScrambleRecord.getInt("fnPossible") + toWin;
                double rating = ( pts / pos * 750.0) * ( 1.0 + (wins / played / 3.0) );
                m_botAction.SQLQueryAndClose( mySQLHost, "UPDATE tblPict_UserStats SET fnPlayed = fnPlayed+1, fnWon = fnWon + "+wonAdd+", fnPoints = fnPoints + "+ points+", fnPossible = fnPossible + "+ toWin +", fnRating = "+rating+" WHERE fcUserName = \"" + username+"\"");
            }
            m_botAction.SQLClose( qryHasScrambleRecord );
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }

    /****************************************************************/
    /*** Gets player statistics.                                  ***/
    /****************************************************************/
    public String getPlayerStats(String username) {
        try{

            ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fnPoints, fnWon, fnPlayed, fnPossible, fnRating FROM tblPict_UserStats WHERE fcUserName = \"" + username+"\"");
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
    public void doTopTen( String name, String message ){
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
            if( message.trim().length() > 0 ){
                m_botAction.sendSmartPrivateMessage( name, getPlayerStats( message ) );
            } else {
                m_botAction.sendSmartPrivateMessage( name, getPlayerStats( name ) );
            }
        } else {
            m_botAction.sendSmartPrivateMessage( name, "Please check your stats when Scramble is not running." );
        }
    }

    public void doScore( String name, String message ){

        if( gameProgress != -1 ){
            m_botAction.sendSmartPrivateMessage( name, "This game is to " + toWin + " points." );
            m_botAction.sendSmartPrivateMessage( name, m_prec + "----------------------------");
            m_botAction.sendSmartPrivateMessage( name, m_prec + doTrimString( "Current Scores", 28 ) + "|" );
            m_botAction.sendSmartPrivateMessage( name, m_prec + doTrimString( "Player Name", 18 ) + doTrimString( "Points", 10 ) + "|" );
            int curPoints = curLeader;
            while( curPoints != 0 ){
                Set set = playerMap.keySet();
                Iterator it = set.iterator();
                while( it.hasNext() ){
                    String curPlayer = (String)it.next();
                    twcore.core.stats.PlayerProfile tempPlayer;
                    tempPlayer = playerMap.get( curPlayer );
                    if( tempPlayer.getData( 0 ) == curPoints ){
                        m_botAction.sendSmartPrivateMessage( name, m_prec + doTrimString( curPlayer, 18 ) + doTrimString( "" + tempPlayer.getData( 0 ), 10 ) + "|" );
                    }
                }
                curPoints--;
            }
            m_botAction.sendSmartPrivateMessage( name, m_prec + "----------------------------");
        }
    }
    
    public void doMyScore(String name, String message){
    	if(gameProgress != -1){
    		if(playerMap.containsKey(name)){
    			PlayerProfile temp = playerMap.get(name);
    			m_botAction.sendSmartPrivateMessage( name, "You currently have " + temp.getData(0) + " points.");
    		}
    		else{
    			m_botAction.sendSmartPrivateMessage( name, "You currently have 0 points.");
    		}    			
    	}
    }

    /****************************************************************/
    /*** Starts the next round.                                   ***/
    /****************************************************************/
    public void startNextRound() {
                if( gameProgress == 4 ) {
                	gameProgress = 1;
                    pictNumber++;
                    cantPlay.clear();
                    m_botAction.spec(curArtist);
                    m_botAction.spec(curArtist);
                    if(theWinner.equals(m_botAction.getBotName())){
                    	String temp = curArtist;
                    	while(temp.equals(curArtist)){pickPlayer();}
                    }
                    else
                    	curArtist = theWinner;
                    if(!cantPlay.contains(curArtist))
                    	cantPlay.add(curArtist);
                    grabWord();
                    doReadyCheck();
                }


        
    }    
    
    /****************************************************************/
    /*** Ends the game, stores results.                           ***/
    /****************************************************************/
    public void doEndGame( String name ) {
        gameProgress = -1;
        curLeader = 0;
        pictNumber = 1;
        m_botAction.sendArenaMessage( m_prec + "Answer: '" + t_word + "'" );
        m_botAction.sendArenaMessage( m_prec + "Player: " + name + " has won this round of Pictionary!", 5 );
        //Save statistics
        Set set = playerMap.keySet();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            String curPlayer = (String) it.next();
            PlayerProfile tempPlayer = playerMap.get(curPlayer);
            if( name.equals(curPlayer) )
                storePlayerStats( curPlayer, tempPlayer.getData( 0 ), true );
            else
                storePlayerStats( curPlayer, tempPlayer.getData( 0 ), false );
        }
        playerMap.clear();
        getTopTen();
        toWin = 10;
        m_botAction.cancelTasks();
        if (autoStart)m_botAction.sendPrivateMessage(m_botAction.getBotName(), "!unlock");        
    }
    
}
