package twcore.bots.multibot.wiaw;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.events.Message;

/**
    Words-in-a-Word.

    @author milosh
*/
public class wiaw extends MultiModule {

    public static final int NO_GAME_IN_PROGRESS = -1;
    public static final int SETTING_UP = 0;
    public static final int WORD_DISPLAYED = 1;
    public static final int STATS_DISPLAYED = 2;

    public Vector<String> topTen;

    public String mySQLHost = "website";
    public String t_word, t_definition;
    public Random m_rnd;

    public TreeMap<String, WiawPlayer> playerMap = new TreeMap<String, WiawPlayer>();
    public int gameProgress = -1, toWin = 5, roundNumber = 0, curLeader = 0, minPlayers = 3, gameLength = 1;
    public String m_prec = "-- ";

    // GUIs
    public String[] helpmsg = {
        "!help         -- displays this",
        "!rules        -- displays game rules",
        "!word        -- displays the current word",
        "!pm           -- enables easier game messaging",
        "!stats        -- displays your stats",
        "!stats <name> -- displays <name>'s stats",
        "!topten       -- displays the top ten players."
    };
    public String[] opmsg = {
        "!wiaw on           -- starts a game of Wiaw",
        "!wiaw off          -- cancels the current game",
        "!displayrules      -- displays game rules to the arena",
        "!help              -- displays this",
        "!rules             -- displays game rules",
        "!word             -- displays the current word",
        "!pm                -- enables easier game messaging",
        "!stats             -- displays your stats",
        "!stats <name>      -- displays <name>'s stats",
        "!topten            -- displays the top ten players."
    };
    public String[] rulesmsg = {
        "+----------------------------------Words-in-a-Word-----------------------------------------+",
        "|RULES: At the start of the game a word is displayed. Players must make other words using  |",
        "|    the letters of the initial word. Players gain points for forming words, but are       |",
        "|    penalized for making incorrect or non-existant words. The player with the most points |",
        "|    at the end of a round wins the round. Win the needed amount of rounds to win the game.|",
        "|KEY: CG - Correct Guess(+10), NoB - Not on board(-2), NaW - Not a Word(-1).               |",
        "+------------------------------------------------------------------------------------------+"
    };

    /**
        Initializes.
    */
    public void init() {
        getTopTen();
        m_rnd = new Random();
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
        Handles messaging.
    */
    public void handleEvent(Message event) {
        int messageType = event.getMessageType();
        String message = event.getMessage();
        String sender = m_botAction.getPlayerName(event.getPlayerID());

        if (messageType == Message.ARENA_MESSAGE && message.equals("NOTICE: Game over") && gameProgress == WORD_DISPLAYED)
            doRoundStats();
        else if(messageType == Message.PRIVATE_MESSAGE)
            handleCommands(sender, message);
    }

    /**
        Required methods.
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
        Handles commands
        @param sender - The user issuing the command
        @param msg - The command/message sent
    */
    public void handleCommands(String sender, String msg) {
        if (msg.equalsIgnoreCase("!help"))
            m_botAction.smartPrivateMessageSpam(sender, helpmsg);
        else if (msg.equalsIgnoreCase("!rules"))
            m_botAction.smartPrivateMessageSpam(sender, rulesmsg);
        else if (msg.equalsIgnoreCase("!word"))
            doShowWord(sender);
        else if (msg.equalsIgnoreCase("!pm"))
            m_botAction.sendSmartPrivateMessage(sender, "You can now use :: to submit your answers.");
        else if (msg.equalsIgnoreCase("!wiaw on") && opList.isER(sender))
            doStartGame(sender);
        else if (msg.equalsIgnoreCase("!wiaw off") && opList.isER(sender))
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
                m_botAction.sendArenaMessage(m_prec + "A game of Words-in-a-Word is starting | Win " + toWin + " rounds to win!", 22);
                m_botAction.sendArenaMessage(m_prec + "PM your answers to " + m_botAction.getBotName() + " if you want to play.");
                TimerTask startGame = new TimerTask() {
                    public void run() {
                        grabWord();
                        m_botAction.sendArenaMessage("Words-in-a-Word begins!", 104);
                        m_botAction.sendArenaMessage("Given word: " + t_word.toUpperCase() + " - Definition: " + t_definition);
                        gameProgress = WORD_DISPLAYED;
                        m_botAction.setTimer(gameLength);
                    }
                };
                m_botAction.scheduleTask(startGame, 5000);
            } else {
                m_botAction.sendArenaMessage("There aren't enough players to play!", 13);
                int pNeed = minPlayers - m_botAction.getArenaSize();

                if (pNeed > 1) {
                    m_botAction.sendArenaMessage(m_prec + "Words-in-a-Word will begin when " + pNeed + " more people enter.");
                } else {
                    m_botAction.sendArenaMessage(m_prec + "Words-in-a-Word will begin when " + pNeed + " more person enters.");
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
            m_botAction.sendArenaMessage(m_prec + "This game of Words-in-a-Word has been canceled.");
            playerMap.clear();
            toWin = 5;
            roundNumber = 1;
            m_botAction.cancelTasks();
        }
    }

    /**
        This method calculates how many points each player got from the current
        round and displays it.
    */
    public void doRoundStats() {
        TreeMap<String, Integer> pointMap = new TreeMap<String, Integer>();
        int tPts = 0, tCG = 0, tNoB = 0, tNaW = 0, tR = 0;
        Iterator<WiawPlayer> i = playerMap.values().iterator();

        while (i.hasNext()) {
            WiawPlayer wp = i.next();
            wp.organizeAnswers();
            wp.clearData();
            Iterator<String> it = wp.getAnswers().iterator();

            while (it.hasNext()) {
                String word = it.next();

                if (isInWord(word, t_word)) {
                    if (isWord(word)) {
                        wp.addCG();
                    } else
                        wp.addNaW();
                } else
                    wp.addNoB();
            }

            wp.setPoints();
            wp.setRating();
            tPts += wp.getPoints();
            tCG += wp.getCG();
            tNoB += wp.getNoB();
            tNaW += wp.getNaW();
            tR += wp.getRating();
            wp.clearAnswers();
            pointMap.put(wp.getName(), wp.getPoints());
        }

        String endGame = " ";
        int maxPoints = java.util.Collections.max(pointMap.values());
        ArrayList<String> winners = new ArrayList<String>();
        i = playerMap.values().iterator();

        while (i.hasNext()) {
            WiawPlayer wp = i.next();

            if (wp.getPoints() == maxPoints && maxPoints > 0) {
                winners.add(wp.getName());
                wp.addRoundWin();
                wp.wonRound(true);

                if (wp.getRoundWins() == toWin)
                    endGame = wp.getName();
            }
        }

        m_botAction.sendArenaMessage(",---------------------------------+-------+-------+-------+----------.");
        m_botAction.sendArenaMessage("|                             Pts |  CGs  |  NoB  |  NaW  |  Rating  |");
        m_botAction.sendArenaMessage("|                          ,------+-------+-------+-------+----------+");
        m_botAction.sendArenaMessage("| Round " + padInt(roundNumber, 2) + "                / " + padInt(tPts, 5) + " | " + padInt(tCG, 5) + " | " + padInt(tNoB, 5) + " | " + padInt(tNaW, 5) + " | " + padInt(tR, 8) + " |");
        m_botAction.sendArenaMessage("+------------------------'        |       |       |       |          |");

        i = playerMap.values().iterator();

        while (i.hasNext()) {
            WiawPlayer wp = i.next();
            m_botAction.sendArenaMessage("|  " + padString(wp.getName(), wp.getRoundWins(), 25) + padInt(wp.getPoints(), 5) + " | " + padInt(wp.getCG(), 5) + " | " + padInt(wp.getNoB(), 5) + " | " + padInt(wp.getNaW(), 5) + " | " + padInt(wp.getRating(), 8) + " |");
            boolean wonGame = false;

            if (wp.getName().equalsIgnoreCase(endGame))
                wonGame = true;

            storePlayerStats(wp, wp.wonRound(), wonGame, endGame);
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
            m_botAction.sendArenaMessage(endGame + " has conquered Words-in-a-Word!", 5);
            doEndGame();
        }

    }

    /**
        Pads an Integer with white space for String formatting
        @param rawr - The integer
        @param size - How long you want the returned string to be
        @return - A string representation of the integer with the size parameters length.
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
        Pads a name with white space and adds asterisks for round wins.
        @param rawr - The string
        @param wins - Number of round wins
        @param size - The desired size
        @return
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
                WiawPlayer p = playerMap.get(i.next());
                p.clearData();
            }

            TimerTask startGame = new TimerTask() {
                public void run() {
                    grabWord();
                    m_botAction.sendArenaMessage("Words-in-a-Word begins!", 104);
                    m_botAction.sendArenaMessage("Given word: " + t_word.toUpperCase() + " - Definition: " + t_definition);
                    gameProgress = WORD_DISPLAYED;
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
        Displays the word privately to a user
        @param name - the user
    */
    public void doShowWord(String name) {
        if(gameProgress == WORD_DISPLAYED)
            m_botAction.sendSmartPrivateMessage( name, "The current word is '" + t_word + "'.");
        else
            m_botAction.sendSmartPrivateMessage( name, "The game is not currently in progress.");
    }

    /**
        This method adds all messages sent during game play to an ArrayList
        mapped to each players name.

        @param name -
                  The person sending the message.
        @param message -
                  The message sent.
    */
    public void doCheckAnswers(String name, String message) {
        if (gameProgress == WORD_DISPLAYED && name != null && message != null) {
            if (!playerMap.containsKey(name))
                playerMap.put(name, new WiawPlayer(name));

            playerMap.get(name).addAnswer(message);
        }
    }

    /**
        Queries a dictionary table to see if the given word exists.
        @param word
        @return - True if the word exists. Else false.
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
        This method is the core of Words-in-a-Word. It returns true if the first String parameter can be constructed
        out of the characters of the second String parameter.
        Examples:
        - (low, shallow) returns true
        - (wall, shallow) returns true
        - (whales, shallow) returns false
        - (wallow, shallow) returns false
    */
    public boolean isInWord(String littleWord, String bigWord) {
        ArrayList<Character> chars = new ArrayList<Character>();

        for(int i = 0; i < bigWord.length(); i++)
            chars.add(bigWord.charAt(i));

        for(int i = 0; i < littleWord.length(); i++) {
            if(!chars.contains(littleWord.charAt(i)))
                return false;
            else
                chars.remove(chars.lastIndexOf(littleWord.charAt(i)));
        }

        return true;
    }

    /**
        Grabs a word from the database.
    */
    public void grabWord() {
        try {
            ResultSet qryWordData;
            qryWordData = m_botAction.SQLQuery(mySQLHost, "SELECT WordID, Word, WordDef FROM tblScramble_Nerd WHERE TimesUsed=" + getMinTimesUsed() + " AND CHAR_LENGTH(Word) > 6 ORDER BY RAND(" + m_rnd.nextInt() + ") LIMIT 1");

            if (qryWordData.next()) {
                String temp = qryWordData.getString("WordDef");
                t_definition = "'" + temp.substring(0, 1).toUpperCase() + temp.substring(1) + ".'";
                t_word = qryWordData.getString("Word");
                int ID = qryWordData.getInt("WordID");
                m_botAction.SQLQuery(mySQLHost, "UPDATE tblScramble_Nerd SET TimesUsed = TimesUsed + 1 WHERE WordID = " + ID);
            }

            m_botAction.SQLClose(qryWordData);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }

    /**
        Gets the minimum times any word in the database has been used.
    */
    public int getMinTimesUsed() {
        try {
            ResultSet qryMinTimesUsed = m_botAction.SQLQuery(mySQLHost, "SELECT MIN(TimesUsed) AS MinTimesUsed FROM tblScramble_Nerd");

            if (qryMinTimesUsed.next()) {
                int minUsed = qryMinTimesUsed.getInt("MinTimesUsed");
                m_botAction.SQLClose(qryMinTimesUsed);
                return minUsed;
            }

            return -1;

        } catch (Exception e) {
            Tools.printStackTrace(e);
            return -1;
        }
    }

    /**
        This method stores a player's statistics in the database.

        @param wp -
                  The WiawPlayer object
        @param wonRound -
                  Whether or not the player won the current round.
        @param wonGame -
                  Whether or not the player won the current game.
        @param s -
                  Whether or not the current game is ending.
    */
    public void storePlayerStats(WiawPlayer wp, boolean wonRound, boolean wonGame, String s) {
        try {
            int wonR = 0, wonG = 0, GP = 0;

            if (wonRound)
                wonR = 1;

            if (wonGame)
                wonG = 1;

            if (!s.equals(" "))
                GP = 1;

            ResultSet qry = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblWiaw_UserStats WHERE fcUserName = \"" + wp.getName() + "\"");

            if (qry.next()) {
                int rp = qry.getInt("fnRoundsPlayed");
                int avgP = ((qry.getInt("fnAveragePoints") * rp) + wp.getPoints()) / (rp + 1);
                int avgR = ((qry.getInt("fnAverageRating") * rp) + wp.getRating()) / (rp + 1);
                m_botAction.SQLQueryAndClose(mySQLHost, "UPDATE tblWiaw_UserStats SET fnGamesPlayed = fnGamesPlayed + " + GP + ", fnGamesWon = fnGamesWon + " + wonG + ", fnRoundsPlayed = fnRoundsPlayed + 1, fnRoundsWon = fnRoundsWon + " + wonR + ", fnAveragePoints = " + avgP + ", fnAverageRating = " + avgR + " WHERE fcUserName = \"" + wp.getName() + "\"");
            } else
                m_botAction.SQLQueryAndClose(mySQLHost, "INSERT INTO tblWiaw_UserStats(fcUserName, fnGamesPlayed, fnGamesWon, fnRoundsPlayed, fnRoundsWon, fnAveragePoints, fnAverageRating) VALUES (\"" + wp.getName() + "\",0,0,1," + wonR + "," + wp.getPoints() + "," + wp.getRating() + ")");

            m_botAction.SQLClose(qry);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }

    /**
        Displays users statistics
        @param name - The user issuing the command
        @param message - The user to display statistics for
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
            m_botAction.sendSmartPrivateMessage(name, "Please check your stats when Words-in-a-Word is not running.");
        }
    }

    /**
        Returns a string representation of a user's statistics
        @param username - the user
        @return - the string
    */
    public String getPlayerStats(String username) {
        try {

            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblWiaw_UserStats WHERE fcUserName = \"" + username + "\"");
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
        Displays the top ten Wiaw players
        @param name - the user issuing the command
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
        Finds the top ten Wiaw players from the database and stores them in a local Vector.
    */
    public void getTopTen() {
        topTen = new Vector<String>();

        try {
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblWiaw_UserStats ORDER BY fnAverageRating DESC LIMIT 10");

            while (result.next())
                topTen.add(result.getString("fcUserName") + "- Games Won: (" + result.getInt("fnGamesWon") + ":" + result.getInt("fnGamesPlayed") + ")  Rounds Won: (" + result.getInt("fnRoundsWon") + ":" + result.getInt("fnRoundsPlayed") + ")  Average Points: " + result.getInt("fnAveragePoints") + " Rating: " + result.getInt("fnAverageRating"));

            m_botAction.SQLClose(result);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }

    /*
        tblWiaw_UserStats
        - fcUserName
        - fnGamesPlayed
        - fnGamesWon
        - fnRoundsPlayed
        - fnRoundsWon
        - fnAveragePoints
        - fnAverageRating
    */

    /**
        Represents an instance of a Wiaw player. Holds answers, round statistics and round wins.
    */
    private class WiawPlayer {
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
            The WiawPlayer constructor.
            @param name - Name of the player.
        */
        private WiawPlayer(String name) {
            this.name = name;
            answers = new ArrayList<String>();
        }

        /**
            Returns this Wiaw player's name.
        */
        private String getName() {
            return name;
        }

        /**
            Removes duplicate answers
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
            Clears answers for this player.
        */
        private void clearAnswers() {
            answers.clear();
        }

        /**
            Adds an answer to this player's answer list.
            @param s - The answer to add
        */
        private void addAnswer(String s) {
            answers.add(s);
        }

        /**
            Returns the ArrayList of this player's answers.
        */
        private ArrayList<String> getAnswers() {
            return answers;
        }

        /**
            Adds a round win to this player.
        */
        private void addRoundWin() {
            roundWins++;
        }

        /**
            Returns the number of rounds this player has won.
        */
        private int getRoundWins() {
            return roundWins;
        }

        /**
            Adds a correct guess for this player.
        */
        private void addCG() {
            CG++;
        }

        /**
            Returns the number of correct guesses this player had this round.
        */
        private int getCG() {
            return CG;
        }

        /**
            Adds a 'Not on Board' for this player.
        */
        private void addNoB() {
            NoB++;
        }

        /**
            Returns the number of 'Not on Board's this player had this round.
        */
        private int getNoB() {
            return NoB;
        }

        /**
            Adds a 'Not a Word' for this player.
        */
        private void addNaW() {
            NaW++;
        }

        /**
            Returns the number of 'Not a Word's this player had this round.
        */
        private int getNaW() {
            return NaW;
        }

        /**
            Clears player round data such as CGs, NoBs, NaWs, rating, points, etc.
            Note: does not clear round wins
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
            Calculates and sets the round rating for this player.
        */
        private void setRating() {
            if (points > 0)
                rating = (points * 10) + ((CG - (NoB + NaW)) * 100);
            else
                rating = points + ((CG - (NoB + NaW)) * 10);
        }

        /**
            Returns the player's rating for this round.
        */
        private int getRating() {
            return rating;
        }

        /**
            Calculates and sets the round points for this player.
        */
        private void setPoints() {
            points = (CG * 10) - (NoB * 2) - (NaW);
        }

        /**
            Returns the player's points for this round.
        */
        private int getPoints() {
            return points;
        }

        /**
            Sets whether or not the player won this round.
            @param x - True if the round was won. Else false.
        */
        private void wonRound(boolean x) {
            wonRound = x;
        }

        /**
            Returns whether or not the player won this round.
            @return - True if the round was won. Else false.
        */
        private boolean wonRound() {
            return wonRound;
        }

    }

}