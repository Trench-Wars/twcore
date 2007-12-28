package twcore.bots.gamebot.boggle;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.command.CommandInterpreter;
import twcore.core.stats.PlayerProfile;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.lvz.*;

/**
 * Boggle.
 * 
 * @author milosh and alinea
 * 1.08
 */
public class boggle extends MultiModule {

	public Vector<BoggleStack> arrStacks; //alinea's
	
	String          mySQLHost = "local";
	
	CommandInterpreter  m_commandInterpreter;
	LvzManager m_lvz;
	TreeMap<String, ArrayList<String>> playerMap = new TreeMap<String, ArrayList<String>>();
	TreeMap<String, PlayerProfile> pointsMap = new TreeMap<String, PlayerProfile>();
    HashMap<String, String> accessList = new HashMap<String, String>();
    HashMap<String, Integer> votes = new HashMap<String, Integer>();
    ArrayList<String> hasVoted = new ArrayList<String>();
    int gameProgress = -1, toWin = 5, roundNumber = 0, curLeader = 0, minPlayers = 3, gameLength = 1;
    String m_prec = "-- ";
    boolean endGame = false;
    
    //Special classes
    BoggleBoard m_board = new BoggleBoard();

    //GUIs
    String[] helpmsg={
    	" testing 123 help"
    };
    String[] autohelpmsg={
    		""
    };
	String[] opmsg={
		" testing 123 modhelp"
	};
	String[] autoopmsg={
			" "
	};

	public void init(){
		m_commandInterpreter = new CommandInterpreter( m_botAction );
		m_lvz = new LvzManager();
        registerCommands();
        BotSettings m_botSettings = moduleSettings;
        try{minPlayers = Integer.parseInt(m_botSettings.getString("Minimum"));}catch(Exception e){minPlayers=3;}
        try{gameLength = Integer.parseInt(m_botSettings.getString("GameLength"));}catch(Exception e){gameLength=1;}
        String access[] =  m_botSettings.getString("SpecialAccess").split( ":" );
        for( int i = 0; i < access.length; i++ )
            accessList.put( access[i], access[i] );
        if(autoStart)doStartGame(m_botAction.getBotName(),"");
	}
	
	public void doHelp(String name, String message){
		m_botAction.smartPrivateMessageSpam(name, helpmsg);
	}
	
	public void handleEvent( Message event ){
		int messageType = event.getMessageType();
		String message = event.getMessage();
		if ( messageType == Message.ARENA_MESSAGE && message.equals("NOTICE: Game over") && gameProgress == 1)
			doRoundStats();
		else
			m_commandInterpreter.handleEvent( event );
    }
	public void requestEvents(ModuleEventRequester events){}
	public  String[] getModHelpMessage(){if(!autoStart)return opmsg;else return autoopmsg;}
	public boolean isUnloadable() {return true;}

	public void cancel() {
		gameProgress = -1;
		playerMap.clear();
		m_botAction.cancelTasks();
	}
	
	public void registerCommands() {
        int acceptedMessages;

        acceptedMessages = Message.PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand( "!start",     acceptedMessages, this, "doStartGame" );
        m_commandInterpreter.registerCommand( "!cancel",    acceptedMessages, this, "doCancelGame" );
        
        acceptedMessages = Message.PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand( "!help",      acceptedMessages, this, "doHelp" );
        //if(!autoStart)m_commandInterpreter.registerCommand( "!topten",    acceptedMessages, this, "doTopTen" );
        //if(!autoStart)m_commandInterpreter.registerCommand( "!stats",     acceptedMessages, this, "doStats" );
        //m_commandInterpreter.registerCommand( "!rules",     acceptedMessages, this, "doRules" );

        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "doCheckAnswers" );
    }
	/****************************************************************/
    /*** Starts the game.                                         ***/
    /****************************************************************/
    
    public void doStartGame( String name, String message ) {
        if( m_botAction.getOperatorList().isModerator( name ) || accessList.containsKey( name ) && gameProgress == -1 ) {
            curLeader = 0;
            roundNumber = 1;
            toWin = 5;
            gameProgress = 0;            
            if (m_botAction.getArenaSize() >= minPlayers){         
            	m_botAction.sendArenaMessage(m_prec + "A game of Boggle is starting | Win " + toWin + " rounds to win!", 22);
            	m_botAction.sendArenaMessage(m_prec + "PM !notplaying to " + m_botAction.getBotName() + " if you don't want to play.");
            	TimerTask startGame = new TimerTask() {
            		public void run() {
            			m_board.fill();
            			displayBoard(m_board.getBoard());
            			gameProgress = 1;
            			m_botAction.sendArenaMessage("Boggle begins!", 104);
            			m_botAction.setTimer(gameLength);
            		}
            	};
            	m_botAction.scheduleTask(startGame, 5000); 
            }
            else {
            	m_botAction.sendArenaMessage("There aren't enough players to play!", 13);
            	int pNeed = minPlayers - m_botAction.getArenaSize();
            	if ( pNeed > 1 ){
            		m_botAction.sendArenaMessage(m_prec + "Boggle will begin when " + pNeed + " more people enter.");
            	}
            	else {
            		m_botAction.sendArenaMessage(m_prec + "Boggle will begin when " + pNeed + " more person enters.");
            	}
            }
        }
    }   
    
    /****************************************************************/
    /*** Cancels the game, stores results.                        ***/
    /****************************************************************/

    public void doCancelGame( String name, String message ) {
        if( (m_botAction.getOperatorList().isModerator( name ) || accessList.containsKey( name ) ) && gameProgress != -1 ){
            gameProgress = -1;
            m_botAction.sendArenaMessage( m_prec + "This game of Boggle has been canceled." );
            playerMap.clear();
            pointsMap.clear();
            toWin=5;
            roundNumber=1;
            if (autoStart){
            	m_botAction.cancelTasks();
            	m_botAction.sendPrivateMessage(m_botAction.getBotName(), "!unlock");
            }
        }
    }
    
    /**
     * This method calculates how many points each player got from the current round and displays it.
     */
    
    public void doRoundStats() {
    	
    	//player profile data 0-4
    	// 0 = CG
    	// 1 = NoB
    	// 2 = NaW
    	// 3 = Points
    	// 4 = Rating
    	
    	
    	
    	Set set = playerMap.keySet();
    	Iterator i = set.iterator();
    	while ( i.hasNext() ){
    		String temp = (String) i.next();
    		ArrayList<String> rayList = playerMap.get(temp);
    		String sss = rayList.toString();
    		int index = sss.length() - 1;
    		sss = sss.substring(1,index);
    		String[] list = sss.split(", ");
    		if(!pointsMap.containsKey(temp))
    			pointsMap.put(temp, new PlayerProfile(temp));
    		PlayerProfile pp = pointsMap.get(temp);
    		for ( int x = 0; x < list.length; x++){
    			if(isOnBoard(list[x], m_board.getBoard())){
    				if(isWord(list[x])) { 					
    					pp.incData(0);//add a CG   					
    				}else {
    					pp.incData(2);//add a NaW
    				}
    			}else {
    				pp.incData(1);//add a NoB
    			}
    		}
    		int CG = pp.getData(0), NoB = pp.getData(1), NaW = pp.getData(2);
    		pp.setData(3, ((CG * 10) - (NoB * 2) - (NaW))); //Calculate points
    		if(pp.getData(3) > 0)
    			pp.setData(4,((pp.getData(3) * 10) + ((CG - (NoB + NaW)) * 100)));//Calculate Rating if points are positive.
    		else {
    			pp.setData(4,((pp.getData(3)) + ((CG - (NoB + NaW)) * 10)));//Calculate Rating if points are negative.
    		}
    	}
    	
    	Set set2 = pointsMap.keySet(); Set set3 = pointsMap.keySet();
    	Iterator i2 = set2.iterator(); Iterator i3 = set3.iterator();
    	int tPts = 0, tCG = 0, tNoB = 0, tNaW = 0, tR = 0, roundWinnerPts = 0;
    	String roundWinner = null;
    	while (i2.hasNext() ){
    		String temp = (String) i2.next();
    		PlayerProfile p = pointsMap.get(temp);    		
    		tCG += p.getData(0);
    		tNoB += p.getData(1);
    		tNaW += p.getData(2);
    		tPts +=  p.getData(3);
    		tR += p.getData(4);
    		if(p.getData(3) > roundWinnerPts){
    			roundWinnerPts = p.getData(3);
    			roundWinner = p.getName();
    			p.incData(5);
    			if(p.getData(5) == toWin)
    				endGame = true;    				
    		}
    	}
    	
    	m_botAction.sendArenaMessage(",---------------------------------+-------+-------+-------+----------.");
    	m_botAction.sendArenaMessage("|                             Pts |  CGs  |  NoB  |  NaW  |  Rating  |");
    	m_botAction.sendArenaMessage("|                          ,------+-------+-------+-------+----------+");
    	m_botAction.sendArenaMessage("| Round " + roundNumber + "                 / " + padInt(tPts,5) +" | " + padInt(tCG,5) +" | " + padInt(tNoB,5) +" | " + padInt(tNaW,5) +" | "+ padInt(tR,8) +" |");
    	m_botAction.sendArenaMessage("+------------------------'        |       |       |       |          |");
    	while (i3.hasNext() ) {
    		String pName = (String) i3.next();
    		PlayerProfile pProf = pointsMap.get(pName);
    		m_botAction.sendArenaMessage("|  " + padString(pName,pProf.getData(5),25) + padInt(pProf.getData(3),5) + " | " + padInt(pProf.getData(0),5) + " | " + padInt(pProf.getData(1),5) + " | " + padInt(pProf.getData(2),5) + " | " + padInt(pProf.getData(4),8) + " |");
    	}
    	m_botAction.sendArenaMessage("`---------------------------------+-------+-------+-------+----------'");
    	if(!(roundWinner == null))
    		m_botAction.sendArenaMessage(roundWinner + " wins this round with " + roundWinnerPts + " points!");    		
    	else
    		m_botAction.sendArenaMessage("No one won this round!");
    	gameProgress=2;
    	//TODO:Store stats in database.
    	if(endGame){
    		m_botAction.sendArenaMessage(roundWinner + " has conquered Boggle!", 5);
    		doEndGame();
    	}
    	else
    		startNextRound();
    }
   
  //player profile data 0-9
	// 0 = CG
	// 1 = NoB
	// 2 = NaW
	// 3 = Points
	// 4 = Rating  
    
  /* Sample display... Pts = Points, CGs = Correct Guesses, NoB = Not on Board, NaW = Not a Word, Rating.
   * 
   * Calculations: Pts = ((CGs * 10) - (NoB * 2) - (NaW))
   * 			   Rating = ((Pts * 10) + ((CGs - (NoB + NaW)) * 100))
   * 
   * Eventually store ( Game Wins / Games Played / Round Wins / Rounds Played / Avg. Rating ) 
   * 
  ,---------------------------------+-------+-------+-------+----------.
  |                             Pts |  CGs  |  NoB  |  NaW  |  Rating  |
  |                          ,------+-------+-------+-------+----------+
  | Round #                 /   272 |  332  |    1  | 45188 |   15     |
  +------------------------'        |       |       |       |          |
  |  Funeral Procession          49 |   47  |    0  |  6676 |    1     |
  |  Ishie                       36 |   51  |    0  |  5080 |    1     |
  |  Machine of God               0 |    4  |    0  |  1200 |    0     |
  |  milosh <ZH>                  5 |   49  |    1  |  1822 |    0     |
  |  phata$$                     35 |   41  |    0  |  5030 |    1     |
  |  Rogue                       79 |   35  |    0  | 10535 |    1     |
  |  s_s_o_t_e                    2 |   19  |    0  |  2033 |    1     |
  |  sCHOPe                      33 |   16  |    0  |  5627 |    4     |
  |  USOi                         3 |   19  |    0  |  1625 |    0     |
  |  Vatican Assassin             1 |   19  |    0  |  1810 |    5     |
  |  Whispus                     29 |   32  |    0  |  3750 |    1     |
  `---------------------------------+-------+-------+-------+----------'
  Rogue wins this round with 79 points!          */
    
    
    public String padInt(int rawr, int size){
    	String s = rawr + "";
    	size -= s.length();
    	for(int i=0; i < size; i++){
    		s = " " + s;
    	}
    	return s; 	
    }
    public String padString(String rawr, int wins, int size){
    	String s = "", longName = rawr, builder = "";
    	for(int i = 0; i < wins; i++){
    		s += "*";
    	}
    	rawr += " - " + s;
    	longName += "(" + wins + ")";
    	if(rawr.length() < size){    		
    		size -= rawr.length();
    		builder=rawr;
    	}
    	else{
    		size -= longName.length();
    		builder=longName;
    	}
    	for(int i=0; i < size; i++){
    		builder += " ";
    	}
    	return builder; 	
    }
    
    /****************************************************************/
    /*** Starts the next round.                                   ***/
    /****************************************************************/
    public void startNextRound() {
                if( gameProgress == 2 ) {
                	gameProgress = 0;
                    roundNumber++;
                    Set set = pointsMap.keySet();
                    Iterator i = set.iterator();
                    while(i.hasNext()){
                    	PlayerProfile p = pointsMap.get(i.next());
                    	p.setData(0, 0);   	p.setData(1, 0);
                    	p.setData(2, 0);    p.setData(3, 0);
                    	p.setData(4, 0);       	
                    }
                    playerMap.clear();
                    TimerTask startGame = new TimerTask() {
                		public void run() {
                			m_board.fill();
                			displayBoard(m_board.getBoard());
                			gameProgress = 1;
                			m_botAction.sendArenaMessage("Boggle begins!", 104);
                			m_botAction.setTimer(gameLength);            			
                		}
                	};
                    m_botAction.scheduleTask(startGame, 10000);
                }
    }    
    
    /****************************************************************/
    /*** Ends the game, stores results.                           ***/
    /****************************************************************/
    public void doEndGame() {
        gameProgress = -1;
        roundNumber = 1;
        playerMap.clear();
        pointsMap.clear();
        toWin = 5;
        m_botAction.cancelTasks();
        if (autoStart)m_botAction.sendPrivateMessage(m_botAction.getBotName(), "!unlock");        
    }

    /**
     * A temporary board display... TODO:to be replaced by lvz.
     */
    public void displayBoard( char[][] b){
    	m_botAction.sendArenaMessage("+---+---+---+---+");
    	m_botAction.sendArenaMessage("| " + b[0][0] + " | " + b[1][0] + " | " + b[2][0] + " | " + b[3][0] + " |");
    	m_botAction.sendArenaMessage("+---+---+---+---+");
    	m_botAction.sendArenaMessage("| " + b[0][1] + " | " + b[1][1] + " | " + b[2][1] + " | " + b[3][1] + " |");
    	m_botAction.sendArenaMessage("+---+---+---+---+");
    	m_botAction.sendArenaMessage("| " + b[0][2] + " | " + b[1][2] + " | " + b[2][2] + " | " + b[3][2] + " |");
    	m_botAction.sendArenaMessage("+---+---+---+---+");
    	m_botAction.sendArenaMessage("| " + b[0][3] + " | " + b[1][3] + " | " + b[2][3] + " | " + b[3][3] + " |");
    	m_botAction.sendArenaMessage("+---+---+---+---+");
    }
  
    
    /**
     * This method adds all messages sent during game play to an ArrayList mapped
     * to each players name.
     * @param name - The person sending the message.
     * @param message - The message sent.
     */
    public void doCheckAnswers(String name, String message){
    	if( gameProgress == 1 ) {
    		if (!playerMap.containsKey(name))
    			playerMap.put(name, new ArrayList<String>());
    			ArrayList<String> temp = playerMap.get( name );
    			temp.add(message);
    	}
    }
    
    public boolean isWord(String word){
    	try{
    		ResultSet qryWord;
    		qryWord = m_botAction.SQLQuery( mySQLHost, "SELECT entry FROM tblBoggle_Dict WHERE entry='" + word + "'");
    		if ( qryWord.next() )
    			return true;
    		return false;
    	}catch(Exception e){return false;}
    }
/**
 * This method searches a 4x4 char[][] matrix for a word in the style of classic Boggle.
 * Letters can touch vertically, horizontally, or diagonally, but can only be used once.
 * @param strWord -the word to find
 * @param arrBoard -the matrix/board to search
 * @author alinea
 */
public boolean isOnBoard(String strWord, char[][] arrBoard) {
	int min_length = 3;
	int max_length = 11;
	
	int strLen = strWord.length();
	if (strLen < min_length || strLen > max_length) {return false;}

	strWord = strWord.toLowerCase();
	arrStacks = new Vector<BoggleStack>();

	// For each possible path we can take on the game board to come up with the word given, a stack will
	// be created.  Each stack is then searched for the next letter needed to complete the word, creating
	// new stacks as new possible routes come up.
	
	// Each stack is defined as:
	//		char[][] board   : This stacks game board.  Letters that have already been 'used'
	//                         are set to '.' so that the letter cannot be used again.
	//		String word      : The word we are looking for on this board.
	//		String strFound  : The current string we have found.
	//		Integer row      : The row position we found the last character at. 
	//		Integer col      : The col position we found the last character at.

	// Example for the word 'ALF'
	// The first stack will have a strFound of '' with all letters available on the board.  This stack
	// will be searched and all occurrences of 'A' will cause a new stack to be created with a strFound
	// of 'A' and that particular A (based on row/col position) removed from the new stacks board
	// (by setting it to '.').  These stacks will then be searched for the second letter 'L', in a position
	// that is valid from the previously found A (specified in the stacks row/col variables).  Any possible
	// 'L's found will cause yet more stacks to be created, and so on until the entire word is found, or
	// until there are no further stacks to search.  In which case, the word was not possible.

	// Create the first stack.  The row/col position of -1/-1 tells searchStack that it is searching
	// for the first character of the word, and that any position on the board is valid.
	arrStacks.add(new BoggleStack(arrBoard, strWord, "", -1, -1));

	// searchStack will create new stacks (add to arrStacks as it finds new possibilities, all we have to do
	// is loop until searchStack returns true (meaning the word was found) or until arrStacks is empty.
	BoggleStack curStack;
	while (arrStacks.isEmpty() == false) {
		curStack = arrStacks.firstElement();
		arrStacks.remove(curStack);
		if (searchStack(curStack)) {
			arrStacks.clear();
			return true;
		}
	}
	return false;
}

/**
 * This method is used by the isOnBoard method. See comments within isOnBoard for an explanation.
 * @param curStack
 * @author alinea
 */
public boolean searchStack(BoggleStack curStack) {
	boolean isLast = false;
	int x,y;

	String curWord = curStack.getWord();
	String curFound = curStack.getFound();
	char curChar = curWord.charAt(curFound.length());
	int curRow = curStack.getRow();
	int curCol = curStack.getCol();
	char[][] curBoard = curStack.getBoard();
	if (curWord.length() == curFound.length()+1) {isLast = true;}

	Vector<BoggleXY> checkPoints;
	checkPoints = new Vector<BoggleXY>();

	// create a list of points on the board to search for the curChar
	if (curRow != -1) {
		// we have a position on the board so we search in squares only linked to it
		if (curCol > 0) {
    		checkPoints.add(new BoggleXY(curRow,curCol-1));
    		if (curRow > 0) checkPoints.add(new BoggleXY(curRow-1,curCol-1));
    		if (curRow < 3) checkPoints.add(new BoggleXY(curRow+1,curCol-1));
		}
		if (curCol < 3) {
    		checkPoints.add(new BoggleXY(curRow,curCol+1));
    		if (curRow > 0) checkPoints.add(new BoggleXY(curRow-1,curCol+1));
    		if (curRow < 3) checkPoints.add(new BoggleXY(curRow+1,curCol+1));
		}
		if (curRow > 0)
			checkPoints.add(new BoggleXY(curRow-1,curCol));
		if (curRow < 3) 
			checkPoints.add(new BoggleXY(curRow+1,curCol));
	}
	else {
		// this is the first character in the word.  We can search all positions on the board.
		for (x = 0; x < 4; x++) {
			for (y = 0; y < 4; y++) {
				checkPoints.add(new BoggleXY(x,y));
			}
		}
	}
	
	// search the points
	BoggleXY curPoint;
	int checkX, checkY;
	for (x = 0; x < checkPoints.size(); x++) {
		curPoint = checkPoints.elementAt(x);
		checkX = curPoint.getX();
		checkY = curPoint.getY();
		if (curChar == curBoard[checkX][checkY]){
			// character found!
			if (isLast) {
				return true;
			}
			// If this wasn't the last character in the word, create a stack to search for the next character
			char[][] newBoard = new char[4][4];
            boardCopy(curBoard,newBoard);
            newBoard[checkX][checkY] = '.';
            arrStacks.add(new BoggleStack(newBoard,curWord,curFound + curChar,checkX,checkY));
        }
		
	}
	return false;
}

public void boardCopy(char[][] source,char[][] destination) {
    for (int a=0; a<source.length; a++) {
            System.arraycopy(source[a],0,destination[a],0,source[a].length);
    }
}

}