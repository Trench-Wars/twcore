package twcore.bots.multibot.scramble;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;
import java.util.ArrayList;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.util.ModuleEventRequester;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.Message;
import twcore.core.stats.PlayerProfile;
import twcore.core.util.Tools;

/**
 * Scramble.
 * 
 * @author 2dragons and milosh - 11.07
 */
public class scramble extends MultiModule {
    
    public CommandInterpreter m_commandInterpreter;
    
    public Random m_rnd;
    public String mySQLHost = "website";
    public StringBuilder builder;
    public TimerTask startGame, timerQuestion, timerHint, timerAnswer, timerNext;
    
    public TreeMap<String, PlayerProfile> playerMap = new TreeMap<String, PlayerProfile>();
    public HashMap<String, String> accessList = new HashMap<String, String>();
    public Vector<String> topTen;
    public ArrayList<String> cantPlay = new ArrayList<String>();
    public int gameProgress = -1, toWin = 10, scrambleNumber = 1, curLeader = 0;
    
    public int m_timeQuestion = 2000, m_timeHint = 15000, m_timeAnswer = 15000, m_timeNext = 5000;
    
    public double giveTime, answerSpeed;
    public String m_prec = "--|-- ";
    public String t_definition, t_word, s_word;
    boolean difficulty = true;
    
    public String[] helpmsg = {
            "Commands:",
            "!help          -- displays this.",
            "!score         -- displays the current scores.",
            "!repeat        -- will repeat the last question given.",
            "!pm            -- the bot will pm you for easier play.",
            "!stats         -- will display your statistics.",
            "!stats <name>  -- displays <name>'s statistics.",
            "!topten        -- displays top ten player stats.",
            "!rules         -- displays the rules."
    };
    public String[] opmsg = {
            "ER Commands:",
            "!start         -- Starts a game of scramble to 10.",
            "!start <num>   -- Starts a game to <num> (1-25).",
            "!cancel        -- Cancels a game of scramble.",
            "!difficulty    -- Toggles normal/nerd mode.",
            "!showanswer    -- Shows you the answer."
    };
    public String[] regrules = {
            "Rules:",
            "-A word with scrambled letters will appear. The first person to PM the word",
            "to the bot before the time is up gains a point. The first player to reach",
            "the needed points for that round wins. After each round player stats are stored.",
            "-Note: You must have at least 100 possible points to gain Top Ten status."
    };
    public String[] oprules = {
            "ER Rules:",
            "-Only start games to (1-25).",
            "-!showanswer will prohibit you from answering that round.",
            "-Do not abuse !showanswer by giving other players or moderators the answer.",
            "-Although it is possible to change difficulties mid-game please do not do so",
            " for the sake of competitive players.",
            "------------------------------------------------------------------------"
    };
    
    /**
     * Initializes.
     */
    public void init() {
        m_commandInterpreter = new CommandInterpreter(m_botAction);
        getTopTen();
        registerCommands();
        m_rnd = new Random();
        BotSettings m_botSettings = moduleSettings;
        String access[] = m_botSettings.getString("SpecialAccess").split(":");
        for (int i = 0; i < access.length; i++)
            accessList.put(access[i], access[i]);
    }
    
    /**
     * Required methods.
     */
    public void requestEvents(ModuleEventRequester events) {}
    
    public String[] getModHelpMessage() {
        List<String> both = new ArrayList<String>();
        Collections.addAll(both, opmsg);
        Collections.addAll(both, helpmsg);
        return both.toArray(new String[] {});
    }
    
    public boolean isUnloadable() {
        return true;
    }
    
    public void cancel() {
        gameProgress = -1;
        playerMap.clear();
        m_botAction.cancelTasks();
    }
    
    /** ************************************************************* */
    /** * Registers the bot commands. ** */
    /** ************************************************************* */
    public void registerCommands() {
        int acceptedMessages;
        
        acceptedMessages = Message.PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand("!start", acceptedMessages, this, "doStartGame");
        m_commandInterpreter.registerCommand("!cancel", acceptedMessages, this, "doCancelGame");
        m_commandInterpreter.registerCommand("!difficulty", acceptedMessages, this, "doDifficulty");
        m_commandInterpreter.registerCommand("!showanswer", acceptedMessages, this, "doShowAnswer");
        
        acceptedMessages = Message.PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand("!repeat", acceptedMessages, this, "doRepeat");
        m_commandInterpreter.registerCommand("!help", acceptedMessages, this, "doHelp");
        m_commandInterpreter.registerCommand("!topten", acceptedMessages, this, "doTopTen");
        m_commandInterpreter.registerCommand("!stats", acceptedMessages, this, "doStats");
        m_commandInterpreter.registerCommand("!score", acceptedMessages, this, "doScore");
        m_commandInterpreter.registerCommand("!pm", acceptedMessages, this, "doPm");
        m_commandInterpreter.registerCommand("!rules", acceptedMessages, this, "doRules");
        
        m_commandInterpreter.registerDefaultCommand(Message.PRIVATE_MESSAGE, this, "doCheckPrivate");
    }
    
    /** ************************************************************* */
    /** * Displays the rules. ** */
    /** ************************************************************* */
    
    public void doRules(String name, String message) {
        if (m_botAction.getOperatorList().isER(name)) {
            m_botAction.smartPrivateMessageSpam(name, oprules);
        }
        
        m_botAction.smartPrivateMessageSpam(name, regrules);
    }
    
    /** ************************************************************* */
    /** * Toggles difficulty level. ** */
    /** ************************************************************* */
    public void doDifficulty(String name, String message) {
        if (m_botAction.getOperatorList().isER(name) || accessList.containsKey(name)) {
            if (gameProgress != -1) {
                if (difficulty)
                    m_botAction.sendArenaMessage("The difficulty has been changed to nerd mode.");
                else
                    m_botAction.sendArenaMessage("The difficulty has been changed to normal mode.");
            }
            if (difficulty) {
                difficulty = false;
                m_botAction.sendSmartPrivateMessage(name, "Difficulty set to nerd mode.");
            } else {
                difficulty = true;
                m_botAction.sendSmartPrivateMessage(name, "Difficulty set to normal mode.");
            }
        }
    }
    
    /** ************************************************************* */
    /** * Starts the game. ** */
    /** ************************************************************* */
    
    public void doStartGame(String name, String message) {
        if (m_botAction.getOperatorList().isER(name) || accessList.containsKey(name) && gameProgress == -1) {
            curLeader = 0;
            scrambleNumber = 1;
            toWin = 10;
            
            try {
                toWin = Integer.parseInt(message);
                if (toWin < 1 || toWin > 500)
                    toWin = 10;
            } catch (Exception e) {}
            gameProgress = 0;
            m_botAction.sendArenaMessage(m_prec + "A game of Scramble is starting | Win by getting " + toWin + " pts!", 22);
            if (difficulty)
                m_botAction.sendArenaMessage(m_prec + "Difficulty set to normal mode.");
            else
                m_botAction.sendArenaMessage(m_prec + "Difficulty set to nerd mode.");
            m_botAction.sendArenaMessage(m_prec + "  - PM !help to " + m_botAction.getBotName() + " for a list of commands...");
            startGame = new TimerTask() {
                public void run() {
                    grabWord();
                    displayWord();
                }
            };
            m_botAction.scheduleTask(startGame, 10000);
        }
    }
    
    /** ************************************************************* */
    /** * Cancels the game, stores results. ** */
    /** ************************************************************* */
    
    public void doCancelGame(String name, String message) {
        if ((m_botAction.getOperatorList().isER(name) || accessList.containsKey(name)) && gameProgress != -1) {
            gameProgress = -1;
            m_botAction.sendArenaMessage(m_prec + "This game of Scramble has been canceled.");
            playerMap.clear();
            m_botAction.cancelTasks();
        }
    }
    
    /** ************************************************************* */
    /** * Scrambles a string. ** */
    /** ************************************************************* */
    public String scrambleWord(String word) {
        StringBuilder builder = new StringBuilder(word.length());
        boolean[] used = new boolean[word.length()];
        
        for (int i = 0; i < word.length(); i++) {
            
            int rndIndex;
            do {
                rndIndex = new Random().nextInt(word.length());
            } while (used[rndIndex]);
            used[rndIndex] = true;
            builder.append(word.charAt(rndIndex));
        }
        return builder.toString();
    }
    
    /** ************************************************************* */
    /** * Displays the Scrambled Word. ** */
    /** ************************************************************* */
    public void displayWord() {
        gameProgress = 1;
        m_botAction.sendArenaMessage(m_prec + "Please PM your answers to " + m_botAction.getBotName() + ".");
        m_botAction.sendArenaMessage(m_prec + "Scramble #" + scrambleNumber + ":");
        timerQuestion = new TimerTask() {
            public void run() {
                if (gameProgress == 1) {
                    gameProgress = 2;
                    // Date d = new Date();
                    giveTime = new java.util.Date().getTime();
                    s_word = t_word;
                    do {
                        s_word = scrambleWord(t_word);
                    } while (s_word.equals(t_word) && s_word.substring(0, 1).equals(t_word.substring(0, 1)));
                    m_botAction.sendArenaMessage(m_prec + "Un-Scramble: " + s_word);
                    displayHint();
                }
            }
        };
        
        m_botAction.scheduleTask(timerQuestion, m_timeQuestion);
    }
    
    /** ************************************************************* */
    /** * Shows the answer(Operator Access) ** */
    /** ************************************************************* */
    public void doShowAnswer(String name, String message) {
        if (m_botAction.getOperatorList().isER(name) || accessList.containsKey(name)) {
            m_botAction.sendSmartPrivateMessage(name, "The un-scrambled word is " + t_word + ".");
            cantPlay.add(name);
        }
    }
    
    /** ************************************************************* */
    /** * Displays the Hint. ** */
    /** ************************************************************* */
    public void displayHint() {
        timerHint = new TimerTask() {
            public void run() {
                if (gameProgress == 2) {
                    gameProgress = 3;
                    m_botAction.sendArenaMessage(m_prec + "Hint: " + t_definition);
                    
                    displayAnswer();
                }
            }
        };
        
        m_botAction.scheduleTask(timerHint, m_timeHint);
    }
    
    /** ************************************************************* */
    /** * Displays the Answer. ** */
    /** ************************************************************* */
    public void displayAnswer() {
        timerAnswer = new TimerTask() {
            public void run() {
                if (gameProgress == 3) {
                    gameProgress = 4;
                    m_botAction.sendArenaMessage(m_prec + "No one has given the correct answer of '" + t_word + "'", 103);
                    
                    doCheckScores();
                    startNextRound();
                }
            }
        };
        
        m_botAction.scheduleTask(timerAnswer, m_timeAnswer);
    }
    
    /** ************************************************************* */
    /** * Starts the next round. ** */
    /** ************************************************************* */
    public void startNextRound() {
        timerNext = new TimerTask() {
            public void run() {
                if (gameProgress == 4) {
                    scrambleNumber++;
                    cantPlay.clear();
                    grabWord();
                    displayWord();
                }
            }
        };
        
        m_botAction.scheduleTask(timerNext, m_timeNext);
    }
    
    /** ************************************************************* */
    /** * Ends the game, stores results. ** */
    /** ************************************************************* */
    public void doEndGame(String name) {
        gameProgress = -1;
        curLeader = 0;
        scrambleNumber = 1;
        m_botAction.sendArenaMessage(m_prec + "Answer: '" + t_word + "'");
        m_botAction.sendArenaMessage(m_prec + "Player: " + name + " has won this round of scramble!", 5);
        Set<String> set = playerMap.keySet();
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String curPlayer = (String) it.next();
            PlayerProfile tempPlayer = playerMap.get(curPlayer);
            if (name.equals(curPlayer))
                storePlayerStats(curPlayer, tempPlayer.getData(0), true);
            else
                storePlayerStats(curPlayer, tempPlayer.getData(0), false);
        }
        playerMap.clear();
        getTopTen();
        m_botAction.cancelTasks();
        toWin = 10;
    }
    
    /** ************************************************************* */
    /** * Shows the last word or answer depending on game progress.** */
    /** ************************************************************* */
    public void doRepeat(String name, String message) {
        if (gameProgress == 4) {
            m_botAction.sendSmartPrivateMessage(name, m_prec + "Un-Scramble: " + s_word + "  ANSWER: " + t_word);
        } else if (gameProgress >= 2) {
            m_botAction.sendSmartPrivateMessage(name, m_prec + "Un-Scramble: " + s_word);
        }
    }
    
    /** ************************************************************* */
    /** * Sends user help message.                                 ** */
    /** ************************************************************* */    
    public void doHelp(String name, String message) {
        m_botAction.smartPrivateMessageSpam(name, helpmsg);
    }

    /** ************************************************************* */
    /** * Sends user private message.                              ** */
    /** ************************************************************* */
    public void doPm(String name, String message) {
        m_botAction.sendSmartPrivateMessage(name, "Now you can use :: to submit your answers.");
    }

    /** ************************************************************* */
    /** * Displays Scramble's Top Ten players.                     ** */
    /** ************************************************************* */
    public void doTopTen(String name, String message) {
        if (topTen.size() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "No one has qualified yet!");
        } else {
            for (int i = 0; i < topTen.size(); i++) {
                m_botAction.sendSmartPrivateMessage(name, topTen.elementAt(i));
            }
        }
    }
    
    /** ************************************************************* */
    /** * Displays a users statistics                              ** */
    /** ************************************************************* */
    public void doStats(String name, String message) {
        if (gameProgress == -1) {
            m_botAction.sendSmartPrivateMessage(name, "Displaying stats, please hold.");
            if (message.trim().length() > 0) {
                m_botAction.sendSmartPrivateMessage(name, getPlayerStats(message));
            } else {
                m_botAction.sendSmartPrivateMessage(name, getPlayerStats(name));
            }
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Please check your stats when Scramble is not running.");
        }
    }
    
    /** ************************************************************* */
    /** * Shows the current score.                                 ** */
    /** ************************************************************* */
    public void doScore(String name, String message) {
        
        if (gameProgress != -1) {
            m_botAction.sendSmartPrivateMessage(name, "This game is to " + toWin + " points.");
            m_botAction.sendSmartPrivateMessage(name, m_prec + "----------------------------");
            m_botAction.sendSmartPrivateMessage(name, m_prec + doTrimString("Current Scores", 28) + "|");
            m_botAction.sendSmartPrivateMessage(name, m_prec + doTrimString("Player Name", 18) + doTrimString("Points", 10) + "|");
            int curPoints = curLeader;
            while (curPoints != 0) {
                Set<String> set = playerMap.keySet();
                Iterator<String> it = set.iterator();
                while (it.hasNext()) {
                    String curPlayer = (String) it.next();
                    twcore.core.stats.PlayerProfile tempPlayer;
                    tempPlayer = playerMap.get(curPlayer);
                    if (tempPlayer.getData(0) == curPoints) {
                        m_botAction.sendSmartPrivateMessage(name, "--|-- " + doTrimString(curPlayer, 18) + doTrimString("" + tempPlayer.getData(0), 10) + "|");
                    }
                }
                curPoints--;
            }
        }
    }
    
    /** ************************************************************* */
    /** * Checks private commands ** */
    /** ************************************************************* */
    
    public void doCheckPrivate(String name, String message) {
        if ((gameProgress == 2) || (gameProgress == 3)) {
            String curAns = t_word;
            if ((message.toLowerCase().equals(curAns.toLowerCase()))) {
                if (!cantPlay.contains(name)) {
                    answerSpeed = (new java.util.Date().getTime() - giveTime) / 1000.0;
                    if (playerMap.containsKey(name)) {
                        twcore.core.stats.PlayerProfile tempP = playerMap.get(name);
                        // data 0 stores the score.
                        tempP.incData(0);
                        if (tempP.getData(0) >= toWin)
                            doEndGame(name);
                    } else {
                        playerMap.put(name, new twcore.core.stats.PlayerProfile(name));
                        twcore.core.stats.PlayerProfile tempP = playerMap.get(name);
                        tempP.setData(0, 1);
                        if (tempP.getData(0) >= toWin)
                            doEndGame(name);
                    }
                    twcore.core.stats.PlayerProfile tempP = playerMap.get(name);
                    if (gameProgress == 2 || gameProgress == 3) {
                        if (answerSpeed < 2) {
                            String trail = getRank(tempP.getData(0));
                            m_botAction.sendArenaMessage(m_prec + "Inconceivable! " + name + " got the correct answer, '" + t_word + "' in only " + trail, 7);
                        } else if (answerSpeed < 5 && answerSpeed > 2) {
                            String trail = getRank(tempP.getData(0));
                            m_botAction.sendArenaMessage(m_prec + "Jeez! " + name + " got the correct answer, '" + t_word + "' in only " + trail, 20);
                        } else {
                            String trail = getRank(tempP.getData(0));
                            m_botAction.sendArenaMessage(m_prec + name + " got the correct answer, '" + t_word + "', " + trail, 103);
                        }
                    }
                    if (gameProgress != -1) {
                        gameProgress = 4;
                        m_botAction.cancelTasks();
                        doCheckScores();
                        startNextRound();
                    }
                } else {
                    m_botAction.sendSmartPrivateMessage(name, "You've seen the answer!");
                }
            }
        }
    }
    
    /** ************************************************************* */
    /** * Returns the correct string for pts and lead. ** */
    /** ************************************************************* */
    public String getRank(int score) {
        String secs = " seconds ";
        if (answerSpeed == 1)
            secs = " second ";
        String speed = answerSpeed + secs;
        String pts = " points.";
        if (score == 1)
            pts = " point.";
        if (score > curLeader) {
            curLeader = score;
            return speed + "and is in the lead with " + score + pts;
        } else if (score == curLeader)
            return speed + "and is tied for the lead with " + score + pts + "!";
        else
            return speed + "and has " + score + pts;
    }
    
    /** ************************************************************* */
    /** * Shows scores. ** */
    /** ************************************************************* */
    public void doCheckScores() {
        if ((scrambleNumber % 5 == 0) && (curLeader != 0)) {
            int numberShown = 0, curPoints = curLeader;
            m_botAction.sendArenaMessage("--|-------------------------------|");
            m_botAction.sendArenaMessage("--|-- " + doTrimString("Top Scores", 28) + "|");
            m_botAction.sendArenaMessage("--|-------------------------------|");
            m_botAction.sendArenaMessage(m_prec + doTrimString("Player Name", 20) + doTrimString("Points", 8) + "|");
            while (numberShown < 5 && curPoints != 0) {
                Set<String> set = playerMap.keySet();
                Iterator<String> it = set.iterator();
                while (it.hasNext() && numberShown < 5) {
                    String curPlayer = (String) it.next();
                    twcore.core.stats.PlayerProfile tempPlayer;
                    tempPlayer = playerMap.get(curPlayer);
                    if (tempPlayer.getData(0) == curPoints) {
                        numberShown++;
                        m_botAction.sendArenaMessage("--|-- " + doTrimString(curPlayer, 20) + doTrimString("" + tempPlayer.getData(0), 8) + "|");
                    }
                }
                curPoints--;
            }
            m_botAction.sendArenaMessage("--|-------------------------------|");
        }
    }
    
    /** ************************************************************* */
    /** * Just adds blank space for alignment. ** */
    /** ************************************************************* */
    public String doTrimString(String fragment, int length) {
        if (fragment.length() > length)
            fragment = fragment.substring(0, length - 1);
        else {
            for (int i = fragment.length(); i < length; i++)
                fragment = fragment + " ";
        }
        return fragment;
    }
    
    public void handleEvent(Message event) {
        m_commandInterpreter.handleEvent(event);
    }
    
    /** ************************************************************* */
    /** * Gets a word from the database. ** */
    /** ************************************************************* */
    public void grabWord() {
        if (difficulty) {
            try {
                ResultSet qryWordData;
                qryWordData = m_botAction.SQLQuery(mySQLHost, "SELECT WordID, Word FROM tblScramble_Norm WHERE TimesUsed=" + getMinTimesUsed() + " AND CHAR_LENGTH(Word) > 4 ORDER BY RAND(" + m_rnd.nextInt() + ") LIMIT 1");
                if (qryWordData.next()) {
                    t_word = qryWordData.getString("Word");
                    t_definition = "The word begins with '" + t_word.substring(0, 1) + "'.";
                    int ID = qryWordData.getInt("WordID");
                    m_botAction.SQLQuery(mySQLHost, "UPDATE tblScramble_Norm SET TimesUsed = TimesUsed + 1 WHERE WordID = " + ID);
                }
                m_botAction.SQLClose(qryWordData);
            } catch (Exception e) {
                Tools.printStackTrace(e);
            }
        } else {
            try {
                ResultSet qryWordData;
                qryWordData = m_botAction.SQLQuery(mySQLHost, "SELECT WordID, Word, WordDef FROM tblScramble_Nerd WHERE TimesUsed=" + getMinTimesUsed() + " ORDER BY RAND(" + m_rnd.nextInt() + ") LIMIT 1");
                if (qryWordData.next()) {
                    t_definition = "'" + qryWordData.getString("WordDef") + "'";
                    t_word = qryWordData.getString("Word");
                    int ID = qryWordData.getInt("WordID");
                    m_botAction.SQLQuery(mySQLHost, "UPDATE tblScramble_Nerd SET TimesUsed = TimesUsed + 1 WHERE WordID = " + ID);
                }
                m_botAction.SQLClose(qryWordData);
            } catch (Exception e) {
                Tools.printStackTrace(e);
            }
        }
        
    }
    
    /** ************************************************************* */
    /** * Gets minimum times used for questions. ** */
    /** ************************************************************* */
    public int getMinTimesUsed() {
        try {
            ResultSet qryMinTimesUsed;
            if (difficulty)
                qryMinTimesUsed = m_botAction.SQLQuery(mySQLHost, "SELECT MIN(TimesUsed) AS MinTimesUsed FROM tblScramble_Norm");
            else
                qryMinTimesUsed = m_botAction.SQLQuery(mySQLHost, "SELECT MIN(TimesUsed) AS MinTimesUsed FROM tblScramble_Nerd");
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
    
    /** ************************************************************* */
    /** * Gets the current Top ten. ** */
    /** ************************************************************* */
    public void getTopTen() {
        topTen = new Vector<String>();
        try {
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT fcUserName, fnPoints, fnPlayed, fnWon, fnPossible, fnRating FROM tblScramble_UserStats WHERE fnPossible >= 100 ORDER BY fnRating DESC LIMIT 10");
            while (result != null && result.next())
                topTen.add(doTrimString(result.getString("fcUsername"), 17) + "Games Won (" + doTrimString("" + result.getInt("fnWon") + ":" + result.getInt("fnPlayed") + ")", 9) + "Pts Scored (" + doTrimString("" + result.getInt("fnPoints") + ":" + result.getInt("fnPossible") + ")", 10) + "Rating: " + result.getInt("fnRating"));
            m_botAction.SQLClose(result);
        } catch (Exception e) {}
    }
    
    /** ************************************************************* */
    /** * Stores player statistics. ** */
    /** ************************************************************* */
    
    public void storePlayerStats(String username, int points, boolean won) {
        try {
            int wonAdd = 0;
            if (won)
                wonAdd = 1;
            
            ResultSet qryHasScrambleRecord = m_botAction.SQLQuery(mySQLHost, "SELECT fcUserName, fnPlayed, fnWon, fnPoints, fnPossible FROM tblScramble_UserStats WHERE fcUserName = \"" + username + "\"");
            if (!qryHasScrambleRecord.next()) {
                double rating = ((points + .0) / toWin * 750.0) * (1.0 + (wonAdd / 3.0));
                m_botAction.SQLQueryAndClose(mySQLHost, "INSERT INTO tblScramble_UserStats(fcUserName, fnPlayed, fnWon, fnPoints, fnPossible, fnRating) VALUES (\"" + username + "\",1," + wonAdd + "," + points + "," + toWin + "," + rating + ")");
            } else {
                double played = qryHasScrambleRecord.getInt("fnPlayed") + 1.0;
                double wins = qryHasScrambleRecord.getInt("fnWon") + wonAdd;
                double pts = qryHasScrambleRecord.getInt("fnPoints") + points;
                double pos = qryHasScrambleRecord.getInt("fnPossible") + toWin;
                double rating = (pts / pos * 750.0) * (1.0 + (wins / played / 3.0));
                m_botAction.SQLQueryAndClose(mySQLHost, "UPDATE tblScramble_UserStats SET fnPlayed = fnPlayed+1, fnWon = fnWon + " + wonAdd + ", fnPoints = fnPoints + " + points + ", fnPossible = fnPossible + " + toWin + ", fnRating = " + rating + " WHERE fcUserName = \"" + username + "\"");
            }
            m_botAction.SQLClose(qryHasScrambleRecord);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }
    
    /** ************************************************************* */
    /** * Gets player statistics. ** */
    /** ************************************************************* */
    public String getPlayerStats(String username) {
        try {
            
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT fnPoints, fnWon, fnPlayed, fnPossible, fnRating FROM tblScramble_UserStats WHERE fcUserName = \"" + username + "\"");
            String info = "There is no record of player " + username;
            if (result.next())
                info = username + "- Games Won: (" + result.getInt("fnWon") + ":" + result.getInt("fnPlayed") + ")  Pts Scored: (" + result.getInt("fnPoints") + ":" + result.getInt("fnPossible") + ")  Rating: " + result.getInt("fnRating");
            m_botAction.SQLClose(result);
            return info;
        } catch (Exception e) {
            Tools.printStackTrace(e);
            return "Can't retrieve stats.";
        }
    }
}
