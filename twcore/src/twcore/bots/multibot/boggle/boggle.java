package twcore.bots.multibot.boggle;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.events.Message;

import twcore.bots.multibot.bogglelvz.BoggleStack;
import twcore.bots.multibot.bogglelvz.BoggleBoard;
import twcore.bots.multibot.bogglelvz.BoggleXY;


/**
 * Boggle.
 * 
 * @author milosh and alinea
 */
public class boggle extends MultiModule {
    
    public static final int NO_GAME_IN_PROGRESS = -1;
    public static final int SETTING_UP = 0;
    public static final int BOARD_DISPLAYED = 1;
    public static final int STATS_DISPLAYED = 2;
    
    public Vector<String> topTen;
    public Vector<BoggleStack> arrStacks;
    
    public String mySQLHost = "website";
    
    public TreeMap<String, BogglePlayer> playerMap = new TreeMap<String, BogglePlayer>();
    public int gameProgress = -1, toWin = 5, roundNumber = 0, curLeader = 0, minPlayers = 3, gameLength = 1;
    public String m_prec = "-- ";
    public BoggleBoard m_board = new BoggleBoard();
    
    // GUIs
    public String[] helpmsg = {
            "!help		   -- displays this",
            "!rules        -- displays game rules",
            "!board	       -- displays the current board",
            "!pm     	   -- enables easier game messaging",
            "!stats        -- displays your stats",
            "!stats <name> -- displays <name>'s stats",
            "!topten       -- displays the top ten players."
    };
    public String[] opmsg = {
            "!boggle on		    -- starts a game of Boggle",
            "!boggle off        -- cancels the current game",
            "!displayrules      -- displays game rules to the arena",
            "!help		        -- displays this",
            "!rules             -- displays game rules",
            "!board	            -- displays the current board",
            "!pm     	        -- enables easier game messaging",
            "!stats             -- displays your stats",
            "!stats <name>      -- displays <name>'s stats",
            "!topten            -- displays the top ten players."
    };
    public String[] rulesmsg = {
            "+----------------------------------------BOGGLE--------------------------------------------+",
            "|RULES: A Standard 4 by 4 Boggle board with characters will be displayed. Create words     |",
            "|   by connecting touching characters. Characters can touch vertically, horizontally, or   |",
            "|   diagonally. The player with the most points at the end of round wins. The first player |",
            "|   to win the needed amount of rounds will when the game.                                 |",
            "|KEY: CG - Correct Guess(+10), NoB - Not on board(-2), NaW - Not a Word(-1).               |",
            "+------------------------------------------------------------------------------------------+"
    };
    
    /**
     * Initializes.
     */
    public void init() {
        getTopTen();
        BotSettings m_botSettings = moduleSettings;
        try {
            minPlayers = Integer.parseInt(m_botSettings.getString("Minimum"));
        } catch (Exception e) {
            minPlayers = 3;
        }
        try {
            gameLength = Integer.parseInt(m_botSettings.getString("GameLength"));
        } catch (Exception e) {
            gameLength = 1;
        }
        
    }
    
    /**
     * Handles messaging.
     */
    public void handleEvent(Message event) {
        int messageType = event.getMessageType();
        String message = event.getMessage();
        String sender = m_botAction.getPlayerName(event.getPlayerID());
        if (messageType == Message.ARENA_MESSAGE && message.equals("NOTICE: Game over") && gameProgress == BOARD_DISPLAYED)
            doRoundStats();
        else if(messageType == Message.PRIVATE_MESSAGE)
            handleCommands(sender, message);
    }
    
    /**
     * Required methods.
     */
    public void requestEvents(ModuleEventRequester events) {}
    
    public String[] getModHelpMessage() {
        return opmsg;
    }
    
    public boolean isUnloadable() {
        return true;
    }
    
    public void cancel() {
        gameProgress = NO_GAME_IN_PROGRESS;
        playerMap.clear();
        m_botAction.cancelTasks();
    }
    
    /**
     * Handles commands
     * @param sender - The user issuing the command
     * @param msg - The command/message sent
     */
    public void handleCommands(String sender, String msg) {
        if (msg.equalsIgnoreCase("!help"))
            m_botAction.smartPrivateMessageSpam(sender, helpmsg);
        else if (msg.equalsIgnoreCase("!rules"))
            m_botAction.smartPrivateMessageSpam(sender, rulesmsg);
        else if (msg.equalsIgnoreCase("!board"))
            doBoard(sender);
        else if (msg.equalsIgnoreCase("!pm"))
            m_botAction.sendSmartPrivateMessage(sender, "You can now use :: to submit your answers.");
        else if (msg.equalsIgnoreCase("!boggle on") && opList.isER(sender))
            doStartGame(sender);
        else if (msg.equalsIgnoreCase("!boggle off") && opList.isER(sender))
            doCancelGame(sender);
        else if (msg.equalsIgnoreCase("!displayrules") && opList.isER(sender))
            m_botAction.arenaMessageSpam(rulesmsg);
        else if (msg.equalsIgnoreCase("!stats"))
            doStats(sender, sender);
        else if (msg.startsWith("!stats "))
            doStats(sender, msg.substring(7));
        else if (msg.equalsIgnoreCase("!topten"))
            doTopTen(sender);
        else
            doCheckAnswers(sender, msg);
    }
    
    /** ************************************************************* */
    /** * Starts the game. ** */
    /** ************************************************************* */  
    public void doStartGame(String name) {
        if (gameProgress == NO_GAME_IN_PROGRESS) {
            curLeader = 0;
            roundNumber = 1;
            toWin = 5;
            gameProgress = SETTING_UP;
            if (m_botAction.getArenaSize() >= minPlayers) {
                m_botAction.sendArenaMessage(m_prec + "A game of Boggle is starting | Win " + toWin + " rounds to win!", 22);
                m_botAction.sendArenaMessage(m_prec + "PM your answers to " + m_botAction.getBotName() + " if you want to play.");
                TimerTask startGame = new TimerTask() {
                    public void run() {
                        m_board.fill();
                        displayBoard(m_board.getBoard());
                        gameProgress = BOARD_DISPLAYED;
                        m_botAction.sendArenaMessage("Boggle begins!", 104);
                        m_botAction.setTimer(gameLength);
                    }
                };
                m_botAction.scheduleTask(startGame, 5000);
            } else {
                m_botAction.sendArenaMessage("There aren't enough players to play!", 13);
                int pNeed = minPlayers - m_botAction.getArenaSize();
                if (pNeed > 1) {
                    m_botAction.sendArenaMessage(m_prec + "Boggle will begin when " + pNeed + " more people enter.");
                } else {
                    m_botAction.sendArenaMessage(m_prec + "Boggle will begin when " + pNeed + " more person enters.");
                }
            }
        }
    }
    
    /** ************************************************************* */
    /** * Cancels the game, stores results. ** */
    /** ************************************************************* */
    public void doCancelGame(String name) {
        if (gameProgress != NO_GAME_IN_PROGRESS) {
            gameProgress = NO_GAME_IN_PROGRESS;
            m_botAction.sendArenaMessage(m_prec + "This game of Boggle has been canceled.");
            playerMap.clear();
            toWin = 5;
            roundNumber = 1;
            m_botAction.cancelTasks();
        }
    }
    
    /**
     * This method calculates how many points each player got from the current
     * round and displays it.
     */
    public void doRoundStats() {
        TreeMap<String, Integer> pointMap = new TreeMap<String, Integer>();
        int tPts = 0, tCG = 0, tNoB = 0, tNaW = 0, tR = 0;
        Iterator<BogglePlayer> i = playerMap.values().iterator();
        while (i.hasNext()) {
            BogglePlayer bp = i.next();
            bp.organizeAnswers();
            bp.clearData();
            Iterator<String> it = bp.getAnswers().iterator();
            while (it.hasNext()) {
                String word = it.next();
                if (isOnBoard(word, m_board.getBoard())) {
                    if (isWord(word)) {
                        bp.addCG();
                    } else
                        bp.addNaW();
                } else
                    bp.addNoB();
            }
            bp.setPoints();
            bp.setRating();
            tPts += bp.getPoints();
            tCG += bp.getCG();
            tNoB += bp.getNoB();
            tNaW += bp.getNaW();
            tR += bp.getRating();
            bp.clearAnswers();
            pointMap.put(bp.getName(), bp.getPoints());
        }
        
        String endGame = " ";
        int maxPoints = java.util.Collections.max(pointMap.values());
        ArrayList<String> winners = new ArrayList<String>();
        i = playerMap.values().iterator();
        while (i.hasNext()) {
            BogglePlayer bp = i.next();
            if (bp.getPoints() == maxPoints && maxPoints > 0) {
                winners.add(bp.getName());
                bp.addRoundWin();
                bp.wonRound(true);
                if (bp.getRoundWins() == toWin)
                    endGame = bp.getName();
            }
        }
        
        m_botAction.sendArenaMessage(",---------------------------------+-------+-------+-------+----------.");
        m_botAction.sendArenaMessage("|                             Pts |  CGs  |  NoB  |  NaW  |  Rating  |");
        m_botAction.sendArenaMessage("|                          ,------+-------+-------+-------+----------+");
        m_botAction.sendArenaMessage("| Round " + padInt(roundNumber, 2) + "                / " + padInt(tPts, 5) + " | " + padInt(tCG, 5) + " | " + padInt(tNoB, 5) + " | " + padInt(tNaW, 5) + " | " + padInt(tR, 8) + " |");
        m_botAction.sendArenaMessage("+------------------------'        |       |       |       |          |");
        
        i = playerMap.values().iterator();
        while (i.hasNext()) {
            BogglePlayer bp = i.next();
            m_botAction.sendArenaMessage("|  " + padString(bp.getName(), bp.getRoundWins(), 25) + padInt(bp.getPoints(), 5) + " | " + padInt(bp.getCG(), 5) + " | " + padInt(bp.getNoB(), 5) + " | " + padInt(bp.getNaW(), 5) + " | " + padInt(bp.getRating(), 8) + " |");
            boolean wonGame = false;
            if (bp.getName().equalsIgnoreCase(endGame))
                wonGame = true;
            storePlayerStats(bp, bp.wonRound(), wonGame, endGame);
        }
        m_botAction.sendArenaMessage("`---------------------------------+-------+-------+-------+----------'");
        
        if (winners.isEmpty())
            m_botAction.sendArenaMessage("Winner(s): None.");
        else
            m_botAction.sendArenaMessage("Winner(s): " + winners.toString() + "!");
        gameProgress = STATS_DISPLAYED;
        if (endGame.equals(" "))
            startNextRound();
        else {
            m_botAction.sendArenaMessage(endGame + " has conquered Boggle!", 5);
            doEndGame();
        }
        
    }
    
    /**
     * Pads an Integer with white space for String formatting
     * @param rawr - The integer
     * @param size - How long you want the returned string to be
     * @return - A string representation of the integer with the size parameters length.
     */
    public String padInt(int rawr, int size) {
        String s = rawr + "";
        size -= s.length();
        for (int i = 0; i < size; i++) {
            s = " " + s;
        }
        return s;
    }
    
    /**
     * Pads a name with white space and adds asterisks for round wins.
     * @param rawr - The string
     * @param wins - Number of round wins
     * @param size - The desired size
     * @return
     */
    public String padString(String rawr, int wins, int size) {
        String s = "", longName = rawr, builder = "";
        for (int i = 0; i < wins; i++) {
            s += "*";
        }
        rawr += " - " + s;
        longName += "(" + wins + ")";
        if (rawr.length() < size) {
            size -= rawr.length();
            builder = rawr;
        } else {
            size -= longName.length();
            builder = longName;
        }
        for (int i = 0; i < size; i++) {
            builder += " ";
        }
        return builder;
    }
    
    /** ************************************************************* */
    /** * Starts the next round. ** */
    /** ************************************************************* */
    public void startNextRound() {
        if (gameProgress == STATS_DISPLAYED) {
            gameProgress = SETTING_UP;
            roundNumber++;
            Iterator<String> i = playerMap.keySet().iterator();
            while (i.hasNext()) {
                BogglePlayer p = playerMap.get(i.next());
                p.clearData();
            }
            TimerTask startGame = new TimerTask() {
                public void run() {
                    m_board.fill();
                    displayBoard(m_board.getBoard());
                    gameProgress = BOARD_DISPLAYED;
                    m_botAction.sendArenaMessage("Boggle begins!", 104);
                    m_botAction.setTimer(gameLength);
                }
            };
            m_botAction.scheduleTask(startGame, 10000);
        }
    }
    
    /** ************************************************************* */
    /** * Ends the game, stores results. ** */
    /** ************************************************************* */
    public void doEndGame() {
        gameProgress = NO_GAME_IN_PROGRESS;
        roundNumber = 1;
        playerMap.clear();
        toWin = 5;
        m_botAction.cancelTasks();
        getTopTen();
    }
    
    /**
     * A temporary board display... TODO:to be replaced by lvz.
     */
    public void displayBoard(char[][] b) {
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
     * Displays the board privately to a user
     * @param name - the user
     */
    public void doBoard(String name) {
        m_botAction.sendSmartPrivateMessage(name, "+---+---+---+---+");
        m_botAction.sendSmartPrivateMessage(name, "| " + m_board.getBoard()[0][0] + " | " + m_board.getBoard()[1][0] + " | " + m_board.getBoard()[2][0] + " | " + m_board.getBoard()[3][0] + " |");
        m_botAction.sendSmartPrivateMessage(name, "+---+---+---+---+");
        m_botAction.sendSmartPrivateMessage(name, "| " + m_board.getBoard()[0][1] + " | " + m_board.getBoard()[1][1] + " | " + m_board.getBoard()[2][1] + " | " + m_board.getBoard()[3][1] + " |");
        m_botAction.sendSmartPrivateMessage(name, "+---+---+---+---+");
        m_botAction.sendSmartPrivateMessage(name, "| " + m_board.getBoard()[0][2] + " | " + m_board.getBoard()[1][2] + " | " + m_board.getBoard()[2][2] + " | " + m_board.getBoard()[3][2] + " |");
        m_botAction.sendSmartPrivateMessage(name, "+---+---+---+---+");
        m_botAction.sendSmartPrivateMessage(name, "| " + m_board.getBoard()[0][3] + " | " + m_board.getBoard()[1][3] + " | " + m_board.getBoard()[2][3] + " | " + m_board.getBoard()[3][3] + " |");
        m_botAction.sendSmartPrivateMessage(name, "+---+---+---+---+");
    }
    
    /**
     * This method adds all messages sent during game play to an ArrayList
     * mapped to each players name.
     * 
     * @param name -
     *            The person sending the message.
     * @param message -
     *            The message sent.
     */
    public void doCheckAnswers(String name, String message) {
        if (gameProgress == BOARD_DISPLAYED && name != null && message != null) {
            if (!playerMap.containsKey(name))
                playerMap.put(name, new BogglePlayer(name));
            playerMap.get(name).addAnswer(message);
        }
    }
    
    /**
     * Queries a dictionary table to see if the given word exists.
     * @param word
     * @return - True if the word exists. Else false.
     */
    public boolean isWord(String word) {
        try {
            ResultSet qryWord;
            qryWord = m_botAction.SQLQuery(mySQLHost, "SELECT entry FROM tblBoggle_Dict WHERE entry='" + word + "'");
            if (qryWord.next())
                return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * This method searches a 4x4 char[][] matrix for a word in the style of
     * classic Boggle. Letters can touch vertically, horizontally, or
     * diagonally, but can only be used once.
     * 
     * @param strWord
     *            -the word to find
     * @param arrBoard
     *            -the matrix/board to search
     * @author alinea
     */
    public boolean isOnBoard(String strWord, char[][] arrBoard) {
        int min_length = 3;
        int max_length = 11;
        
        int strLen = strWord.length();
        if (strLen < min_length || strLen > max_length) {
            return false;
        }
        
        strWord = strWord.toLowerCase();
        arrStacks = new Vector<BoggleStack>();
        
        // For each possible path we can take on the game board to come up with
        // the word given, a stack will
        // be created. Each stack is then searched for the next letter needed to
        // complete the word, creating
        // new stacks as new possible routes come up.
        
        // Each stack is defined as:
        // char[][] board : This stacks game board. Letters that have already
        // been 'used'
        // are set to '.' so that the letter cannot be used again.
        // String word : The word we are looking for on this board.
        // String strFound : The current string we have found.
        // Integer row : The row position we found the last character at.
        // Integer col : The col position we found the last character at.
        
        // Example for the word 'ALF'
        // The first stack will have a strFound of '' with all letters available
        // on the board. This stack
        // will be searched and all occurrences of 'A' will cause a new stack to
        // be created with a strFound
        // of 'A' and that particular A (based on row/col position) removed from
        // the new stacks board
        // (by setting it to '.'). These stacks will then be searched for the
        // second letter 'L', in a position
        // that is valid from the previously found A (specified in the stacks
        // row/col variables). Any possible
        // 'L's found will cause yet more stacks to be created, and so on until
        // the entire word is found, or
        // until there are no further stacks to search. In which case, the word
        // was not possible.
        
        // Create the first stack. The row/col position of -1/-1 tells
        // searchStack that it is searching
        // for the first character of the word, and that any position on the
        // board is valid.
        arrStacks.add(new BoggleStack(arrBoard, strWord, "", -1, -1));
        
        // searchStack will create new stacks (add to arrStacks as it finds new
        // possibilities, all we have to do
        // is loop until searchStack returns true (meaning the word was found)
        // or until arrStacks is empty.
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
     * This method is used by the isOnBoard method. See comments within
     * isOnBoard for an explanation.
     * 
     * @param curStack
     * @author alinea
     */
    public boolean searchStack(BoggleStack curStack) {
        boolean isLast = false;
        int x, y;
        
        String curWord = curStack.getWord();
        String curFound = curStack.getFound();
        char curChar = curWord.charAt(curFound.length());
        int curRow = curStack.getRow();
        int curCol = curStack.getCol();
        char[][] curBoard = curStack.getBoard();
        if (curWord.length() == curFound.length() + 1) {
            isLast = true;
        }
        
        Vector<BoggleXY> checkPoints;
        checkPoints = new Vector<BoggleXY>();
        
        // create a list of points on the board to search for the curChar
        if (curRow != -1) {
            // we have a position on the board so we search in squares only
            // linked to it
            if (curCol > 0) {
                checkPoints.add(new BoggleXY(curRow, curCol - 1));
                if (curRow > 0)
                    checkPoints.add(new BoggleXY(curRow - 1, curCol - 1));
                if (curRow < 3)
                    checkPoints.add(new BoggleXY(curRow + 1, curCol - 1));
            }
            if (curCol < 3) {
                checkPoints.add(new BoggleXY(curRow, curCol + 1));
                if (curRow > 0)
                    checkPoints.add(new BoggleXY(curRow - 1, curCol + 1));
                if (curRow < 3)
                    checkPoints.add(new BoggleXY(curRow + 1, curCol + 1));
            }
            if (curRow > 0)
                checkPoints.add(new BoggleXY(curRow - 1, curCol));
            if (curRow < 3)
                checkPoints.add(new BoggleXY(curRow + 1, curCol));
        } else {
            // this is the first character in the word. We can search all
            // positions on the board.
            for (x = 0; x < 4; x++) {
                for (y = 0; y < 4; y++) {
                    checkPoints.add(new BoggleXY(x, y));
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
            if (curChar == curBoard[checkX][checkY]) {
                // character found!
                if (isLast) {
                    return true;
                }
                // If this wasn't the last character in the word, create a stack
                // to search for the next character
                char[][] newBoard = new char[4][4];
                boardCopy(curBoard, newBoard);
                newBoard[checkX][checkY] = '.';
                arrStacks.add(new BoggleStack(newBoard, curWord, curFound + curChar, checkX, checkY));
            }
            
        }
        return false;
    }
    public void boardCopy(char[][] source, char[][] destination) {
        for (int a = 0; a < source.length; a++) {
            System.arraycopy(source[a], 0, destination[a], 0, source[a].length);
        }
    }
    
    /**
     * This method stores a player's statistics in the database.
     * 
     * @param bp -
     *            The BogglePlayer object
     * @param wonRound -
     *            Whether or not the player won the current round.
     * @param wonGame -
     *            Whether or not the player won the current game.
     * @param s -
     *            Whether or not the current game is ending.
     */
    public void storePlayerStats(BogglePlayer bp, boolean wonRound, boolean wonGame, String s) {
        try {
            int wonR = 0, wonG = 0, GP = 0;
            if (wonRound)
                wonR = 1;
            if (wonGame)
                wonG = 1;
            if (!s.equals(" "))
                GP = 1;
            ResultSet qry = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblBoggle_UserStats WHERE fcUserName = \"" + bp.getName() + "\"");
            if (qry.next()) {
                int rp = qry.getInt("fnRoundsPlayed");
                int avgP = ((qry.getInt("fnAveragePoints") * rp) + bp.getPoints()) / (rp + 1);
                int avgR = ((qry.getInt("fnAverageRating") * rp) + bp.getRating()) / (rp + 1);
                m_botAction.SQLQueryAndClose(mySQLHost, "UPDATE tblBoggle_UserStats SET fnGamesPlayed = fnGamesPlayed + " + GP + ", fnGamesWon = fnGamesWon + " + wonG + ", fnRoundsPlayed = fnRoundsPlayed + 1, fnRoundsWon = fnRoundsWon + " + wonR + ", fnAveragePoints = " + avgP + ", fnAverageRating = " + avgR + " WHERE fcUserName = \"" + bp.getName() + "\"");
            } else
                m_botAction.SQLQueryAndClose(mySQLHost, "INSERT INTO tblBoggle_UserStats(fcUserName, fnGamesPlayed, fnGamesWon, fnRoundsPlayed, fnRoundsWon, fnAveragePoints, fnAverageRating) VALUES (\"" + bp.getName() + "\",0,0,1," + wonR + "," + bp.getPoints() + "," + bp.getRating() + ")");
            m_botAction.SQLClose(qry);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }
    
    /**
     * Displays users statistics
     * @param name - The user issuing the command
     * @param message - The user to display statistics for
     */
    public void doStats(String name, String message) {
        if (gameProgress == NO_GAME_IN_PROGRESS) {
            m_botAction.sendSmartPrivateMessage(name, "Displaying stats, please hold.");
            if (message.trim().length() > 0) {
                m_botAction.sendSmartPrivateMessage(name, getPlayerStats(message));
            } else {
                m_botAction.sendSmartPrivateMessage(name, getPlayerStats(name));
            }
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Please check your stats when Boggle is not running.");
        }
    }
    
    /**
     * Returns a string representation of a user's statistics
     * @param username - the user
     * @return - the string
     */
    public String getPlayerStats(String username) {
        try {
            
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblBoggle_UserStats WHERE fcUserName = \"" + username + "\"");
            String info = "There is no record of player '" + username + "'. Please be sure you are using the entire name.";
            if (result.next())
                info = username + "- Games Won: (" + result.getInt("fnGamesWon") + ":" + result.getInt("fnGamesPlayed") + ")  Rounds Won: (" + result.getInt("fnRoundsWon") + ":" + result.getInt("fnRoundsPlayed") + ")  Average Points: " + result.getInt("fnAveragePoints") + " Rating: " + result.getInt("fnAverageRating");
            m_botAction.SQLClose(result);
            return info;
        } catch (Exception e) {
            Tools.printStackTrace(e);
            return "Can't retrieve stats.";
        }
    }
    
    /**
     * Displays the top ten Boggle players
     * @param name - the user issuing the command
     */
    public void doTopTen(String name) {
        if (topTen.size() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "No one has qualified yet!");
        } else {
            for (int i = 0; i < topTen.size(); i++) {
                m_botAction.sendSmartPrivateMessage(name, topTen.elementAt(i));
            }
        }
    }
    
    /**
     * Finds the top ten Boggle players from the database and stores them in a local Vector.
     */
    public void getTopTen() {
        topTen = new Vector<String>();
        try {
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblBoggle_UserStats ORDER BY fnAverageRating DESC LIMIT 10");
            while (result.next())
                topTen.add(result.getString("fcUserName") + "- Games Won: (" + result.getInt("fnGamesWon") + ":" + result.getInt("fnGamesPlayed") + ")  Rounds Won: (" + result.getInt("fnRoundsWon") + ":" + result.getInt("fnRoundsPlayed") + ")  Average Points: " + result.getInt("fnAveragePoints") + " Rating: " + result.getInt("fnAverageRating"));
            m_botAction.SQLClose(result);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }
    
    /*
     * tblBoggle_UserStats 
     * - fcUserName
     * - fnGamesPlayed
     * - fnGamesWon
     * - fnRoundsPlayed
     * - fnRoundsWon
     * - fnAveragePoints
     * - fnAverageRating
     */

    /**
     * Represents an instance of a Boggle player. Holds answers, round statistics and round wins.
     */
    private class BogglePlayer {
        private String name;
        private int roundWins = 0;
        private int CG = 0;
        private int NoB = 0;
        private int NaW = 0;
        private int rating = 0;
        private int points = 0;
        private boolean wonRound = false;
        private ArrayList<String> answers;
        
        /**
         * The BogglePlayer constructor.
         * @param name - Name of the player.
         */
        private BogglePlayer(String name) {
            this.name = name;
            answers = new ArrayList<String>();
        }
        
        /**
         * Returns this Boggle player's name.
         */
        private String getName() {
            return name;
        }
        
        /**
         * Removes duplicate answers
         */
        private void organizeAnswers() {
            ArrayList<String> temp = new ArrayList<String>();
            Iterator<String> a = answers.iterator();
            while (a.hasNext())
                temp.add(a.next());
            Iterator<String> i = temp.iterator();
            while (i.hasNext()) {
                String s = i.next();
                while (answers.contains(s))
                    answers.remove(s);
                answers.add(s);
            }
        }
        
        /**
         * Clears answers for this player.
         */
        private void clearAnswers() {
            answers.clear();
        }
        
        /**
         * Adds an answer to this player's answer list.
         * @param s - The answer to add
         */
        private void addAnswer(String s) {
            answers.add(s);
        }
        
        /**
         * Returns the ArrayList of this player's answers.
         */
        private ArrayList<String> getAnswers() {
            return answers;
        }
        
        /**
         * Adds a round win to this player.
         */
        private void addRoundWin() {
            roundWins++;
        }
        
        /**
         * Returns the number of rounds this player has won.
         */
        private int getRoundWins() {
            return roundWins;
        }
        
        /**
         * Adds a correct guess for this player.
         */
        private void addCG() {
            CG++;
        }
        
        /**
         * Returns the number of correct guesses this player had this round.
         */
        private int getCG() {
            return CG;
        }
        
        /**
         * Adds a 'Not on Board' for this player.
         */
        private void addNoB() {
            NoB++;
        }
        
        /**
         * Returns the number of 'Not on Board's this player had this round.
         */
        private int getNoB() {
            return NoB;
        }
        
        /**
         * Adds a 'Not a Word' for this player.
         */
        private void addNaW() {
            NaW++;
        }
        
        /**
         * Returns the number of 'Not a Word's this player had this round.
         */
        private int getNaW() {
            return NaW;
        }
        
        /**
         * Clears player round data such as CGs, NoBs, NaWs, rating, points, etc.
         * Note: does not clear round wins
         */
        private void clearData() {
            CG = 0;
            NoB = 0;
            NaW = 0;
            rating = 0;
            points = 0;
            wonRound = false;
        }
        
        /**
         * Calculates and sets the round rating for this player.
         */
        private void setRating() {
            if (points > 0)
                rating = (points * 10) + ((CG - (NoB + NaW)) * 100);
            else
                rating = points + ((CG - (NoB + NaW)) * 10);
        }
        
        /**
         * Returns the player's rating for this round.
         */
        private int getRating() {
            return rating;
        }
        
        /**
         * Calculates and sets the round points for this player.
         */
        private void setPoints() {
            points = (CG * 10) - (NoB * 2) - (NaW);
        }
        
        /**
         * Returns the player's points for this round.
         */
        private int getPoints() {
            return points;
        }
        
        /**
         * Sets whether or not the player won this round.
         * @param x - True if the round was won. Else false.
         */
        private void wonRound(boolean x) {
            wonRound = x;
        }
        
        /**
         * Returns whether or not the player won this round.
         * @return - True if the round was won. Else false.
         */
        private boolean wonRound() {
            return wonRound;
        }
        
    }
    
}