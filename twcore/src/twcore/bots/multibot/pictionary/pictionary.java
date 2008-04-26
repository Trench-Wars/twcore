package twcore.bots.multibot.pictionary;

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;
import java.util.ArrayList;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.StringBag;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.stats.PlayerProfile;
import twcore.core.util.Tools;

/**
 * Pictionary.
 * 
 * @author milosh - 12.07
 */
public class pictionary extends MultiModule {
    
    private static final int NO_GAME_IN_PROGRESS = -1;
    private static final int GAME_STARTING = 0;
    private static final int READY_CHECK = 1;
    private static final int DRAWING = 2;
    private static final int HINT_GIVEN = 3;
    private static final int ANSWER_GIVEN = 4;
    
    public String mySQLHost = "website";
    
    public Random m_rnd;
    public TimerTask timerQuestion, timerHint, timerAnswer, timerNext, warn, forcePass;
    public TreeMap<String, PlayerProfile> playerMap = new TreeMap<String, PlayerProfile>();
    public Vector<String> topTen;
    public ArrayList<String> cantPlay = new ArrayList<String>();
    public ArrayList<String> notPlaying = new ArrayList<String>();
    
    public int gameProgress = -1, toWin = 5, pictNumber = 1, curLeader = 0;
    public int minPlayers, XSpot, YSpot;
    public int m_timeQuestion, m_timeHint, m_timeAnswer, admireArt;
    public double giveTime, answerSpeed;
    public boolean custGame = false, ready = false, isVoting = false;
    
    public String m_prec = "-- ", gameType = "Bot's Pick.", game = "default";
    public String t_definition, t_word = " ", curArtist = " ", theWinner, lastWord = " ";
    
    public String[] helpmsg = {
            "Commands:",
            "!help          -- displays this.",
            "!rules         -- displays the rules.",
            "!lagout        -- puts you back in the game if you're drawing.",
            "!pass          -- gives your drawing turn to a random player.",
            "!reset         -- resets your mines if drawing.",
            "!score         -- displays the current scores.",
            "!repeat        -- will repeat the hint or answer.",
            "!stats         -- will display your statistics.",
            "!stats <name>  -- displays <name>'s statistics.",
            "!topten        -- displays top ten player stats."
    };
    public String[] opmsg = {
            "Moderator Commands:",
            "!start         -- Starts a default game of Pictionary to 10.",
            "!start <num>   -- Starts a game to <num> points.",
            "!gametype      -- Toggles between player pick or bot pick.",
            "!cancel        -- Cancels this game of Pictionary.",
            "!showanswer    -- Shows you the answer(You can't win that round).",
            "!displayrules  -- Shows the rules in *arena messages.",
            "!reset         -- Resets the current artist's mines.",
            "!pass          -- gives your drawing turn to a random player.",
            "!score         -- displays the current scores.",
            "!repeat        -- will repeat the hint or answer.",
            "!stats         -- will display your statistics.",
            "!stats <name>  -- displays <name>'s statistics.",
            "!topten        -- displays top ten player stats.",
            "!help          -- displays this."
    };
    public String[] regrules = {
            "Rules: Racism and pornography are strictly forbidden. The bot will designate",
            "an artist. Players attempt to guess what the artist is drawing before the time",
            "ends. The first player to reach the round's needed points wins.",
            "-Note: You must have played at least 30 rounds to qualify for the top ten.",
    };
    public String[] displayrules = {
            "''''RULES: Racism and pornography are strictly forbidden. The bot will designate  ''''",
            "'''  an artist. Players attempt to guess what the artist is drawing before the time'''",
            "''   ends to gain points. If you guess correctly then it is your turn to draw       ''",
            "'    The first player to reach the round's needed points wins.                       '"
    };
    
    /**
     * Initializes.
     */
    public void init() {
        getTopTen();
        m_rnd = new Random();
        BotSettings m_botSettings = moduleSettings;
        try {
            XSpot = Integer.parseInt(m_botSettings.getString("X"));
        } catch (Exception e) {
            XSpot = 512;
        }
        try {
            YSpot = Integer.parseInt(m_botSettings.getString("Y"));
        } catch (Exception e) {
            YSpot = 512;
        }
        try {
            minPlayers = Integer.parseInt(m_botSettings.getString("Minimum"));
        } catch (Exception e) {
            minPlayers = 2;
        }
        try {
            m_timeQuestion = Integer.parseInt(m_botSettings.getString("Question")) * 1000;
        } catch (Exception e) {
            m_timeQuestion = 2000;
        }
        try {
            m_timeHint = Integer.parseInt(m_botSettings.getString("Hint")) * 1000;
        } catch (Exception e) {
            m_timeHint = 60000;
        }
        try {
            m_timeAnswer = Integer.parseInt(m_botSettings.getString("Answer")) * 1000;
        } catch (Exception e) {
            m_timeAnswer = 100000;
        }
        try {
            admireArt = Integer.parseInt(m_botSettings.getString("Admire")) * 1000;
        } catch (Exception e) {
            admireArt = 5000;
        }
    }
    
    /**
     * Required methods.
     */
    public String[] getModHelpMessage() {
        return opmsg;
    }
    
    public void requestEvents(ModuleEventRequester events) {}
    
    public boolean isUnloadable() {
        return true;
    }
    
    public void cancel() {
        try {
            m_botAction.cancelTasks();
        } catch (Exception e) {}
    }
    
    /**
     * Handles messaging.
     */
    public void handleEvent(Message event) {
        String message = event.getMessage();
        String sender = m_botAction.getPlayerName(event.getPlayerID());
        int messageType = event.getMessageType();
        
        if (opList.isER(sender) && messageType == Message.PRIVATE_MESSAGE)
            handleERCommands(sender, message);
        if (messageType == Message.PRIVATE_MESSAGE)
            handlePlayerCommands(sender, message);
        if (messageType == Message.PUBLIC_MESSAGE)
            doCheckAnswers(sender, message);
        
    }
    
    /** ************************************************************* */
    /** * Registers the bot commands for ER+                       ** */
    /** ************************************************************* */
    public void handleERCommands(String name, String msg) {
        if (msg.equalsIgnoreCase("!start"))
            doStartGame(name, null);
        else if (msg.startsWith("!start "))
            doStartGame(name, msg.substring(7));
        else if (msg.equalsIgnoreCase("!cancel"))
            doCancelGame();
        else if (msg.equalsIgnoreCase("!showanswer"))
            doShowAnswer(name);
        else if (msg.equalsIgnoreCase("!gameType"))
            doToggleGameType(name);
        else if (msg.equalsIgnoreCase("!displayrules"))
            m_botAction.arenaMessageSpam(displayrules);
        
    }
    
    /** ************************************************************* */
    /** * Registers the bot commands for players.                  ** */
    /** ************************************************************* */
    public void handlePlayerCommands(String name, String msg) {
        if (msg.equalsIgnoreCase("!ready"))
            doReady(name);
        else if (msg.equalsIgnoreCase("!pass"))
            doPass(name);
        else if (msg.equalsIgnoreCase("!reset"))
            doReset(name);
        else if (msg.equalsIgnoreCase("!lagout"))
            doLagout(name);
        else if (msg.equalsIgnoreCase("!notplaying"))
            doNotPlaying(name);
        else if (msg.equalsIgnoreCase("!topten"))
            doTopTen(name);
        else if (msg.equalsIgnoreCase("!repeat"))
            doRepeat(name);
        else if (msg.equalsIgnoreCase("!stats"))
            doStats(name, name);
        else if (msg.startsWith("!stats "))
            doStats(name, msg.substring(7));
        else if (msg.equalsIgnoreCase("!score"))
            doScore(name);
        else if (msg.equalsIgnoreCase("!myscore"))
            doMyScore(name);
        else if (msg.equalsIgnoreCase("!rules"))
            m_botAction.smartPrivateMessageSpam(name, regrules);
        else if (msg.equalsIgnoreCase("!help"))
            m_botAction.smartPrivateMessageSpam(name, helpmsg);
        else
            doCustomWord(name, msg);
        
    }
    
    /** ************************************************************* */
    /** * Toggles between default/custom game types.               ** */
    /** ************************************************************* */
    public void doToggleGameType(String name) {
        if (gameProgress != NO_GAME_IN_PROGRESS) {
            m_botAction.sendSmartPrivateMessage(name, "Please choose game type before the game.");
            return;
        }
        if (!custGame) {
            custGame = true;
            m_botAction.sendSmartPrivateMessage(name, "Game type set to custom: Players can pick their own words.");
        } else {
            custGame = false;
            m_botAction.sendSmartPrivateMessage(name, "Game type set to default: The bot will pick words.");
        }
    }
    
    /** ************************************************************* */
    /** * Puts the artist back in if he lags out ** */
    /** ************************************************************* */
    
    public void doLagout(String name) {
        if (name.equals(curArtist) && (gameProgress == HINT_GIVEN || gameProgress == DRAWING)) {
            m_botAction.setShip(curArtist, 1);
            m_botAction.warpTo(curArtist, XSpot, YSpot);
        }
        
    }
    
    /** ************************************************************* */
    /** * Resets the artist's mines. ** */
    /** ************************************************************* */
    
    public void doReset(String name) {
        if ((name.equals(curArtist) || opList.isER(name)) && (gameProgress == HINT_GIVEN || gameProgress == DRAWING)) {
            m_botAction.specWithoutLock(curArtist);
            m_botAction.setShip(curArtist, 1);
            m_botAction.warpTo(curArtist, XSpot, YSpot);
        }
        
    }
    
    /** ************************************************************* */
    /** * Shows the last hint or answer depending on progress.     ** */
    /** ************************************************************* */
    public void doRepeat(String name) {
        if (gameProgress == HINT_GIVEN)
            m_botAction.sendSmartPrivateMessage(name, "Hint: " + t_definition);
        else if (gameProgress == NO_GAME_IN_PROGRESS || gameProgress == GAME_STARTING)
            m_botAction.sendSmartPrivateMessage(name, "No information available.");
        else
            m_botAction.sendSmartPrivateMessage(name, "The last answer was '" + lastWord + "'");
    }
    
    /** ************************************************************* */
    /** * Adds a player to notplaying. ** */
    /** ************************************************************* */    
    public void doNotPlaying(String name) {
        if ((gameProgress == HINT_GIVEN || gameProgress == DRAWING) && name.equalsIgnoreCase(curArtist))
            doPass(curArtist);
        if (!notPlaying.contains(name)) {
            notPlaying.add(name);
            m_botAction.sendSmartPrivateMessage(name, "Not playing enabled. You will not be allowed to draw or guess.");
        } else {
            notPlaying.remove(name);
            m_botAction.sendSmartPrivateMessage(name, "Not playing disabled. You are now allowed to draw and guess.");
        }
    }
    
    /** ************************************************************* */
    /** * Starts the game. ** */
    /** ************************************************************* */    
    public void doStartGame(String name, String msg) {
        if (gameProgress == NO_GAME_IN_PROGRESS) {
            curLeader = 0;
            pictNumber = 1;
            cantPlay.clear();
            playerMap.clear();
            try {
                toWin = Integer.parseInt(msg);
                if (toWin < 1 || toWin > 15)
                    toWin = 5;
            } catch (Exception e) {
                toWin = 5;
            }
            gameProgress = GAME_STARTING;
            m_botAction.specAll();
            m_botAction.arenaMessageSpam(displayrules);
            
            if (m_botAction.getArenaSize() > minPlayers) {
                m_botAction.sendArenaMessage(m_prec + "A game of Pictionary is starting | Win by getting " + toWin + " pts!", 22);
                m_botAction.sendArenaMessage(m_prec + "Type your guesses in public chat.");
                m_botAction.sendArenaMessage(m_prec + "PM !notplaying to " + m_botAction.getBotName() + " if you don't want to play.");
                gameProgress = READY_CHECK;
                pickPlayer();
                cantPlay.clear();
                cantPlay.add(curArtist);
                if (custGame)
                    m_botAction.sendSmartPrivateMessage(curArtist, "Private message me what you're drawing or type !ready for me to pick something for you.");
                else {
                    grabWord();
                    doReadyCheck();
                }
                
            } else {
                m_botAction.sendArenaMessage("There aren't enough players to play!", 13);
                int pNeed = minPlayers - m_botAction.getArenaSize();
                if (pNeed > 1) {
                    m_botAction.sendArenaMessage(m_prec + "Pictionary will begin when " + pNeed + " more people enter.");
                } else {
                    m_botAction.sendArenaMessage(m_prec + "Pictionary will begin when " + pNeed + " more person enters.");
                }
            }
        }
    }
    
    /** ************************************************************* */
    /** * Cancels the game, stores results. ** */
    /** ************************************************************* */    
    public void doCancelGame() {
        if (gameProgress != NO_GAME_IN_PROGRESS) {
            gameProgress = NO_GAME_IN_PROGRESS;
            m_botAction.sendArenaMessage(m_prec + "This game of Pictionary has been canceled.");
            try {
                m_botAction.cancelTasks();
            } catch (Exception e) {}
            m_botAction.specAll();
        }
    }
    
    /** ************************************************************* */
    /** * Shows the answer(Operator Access) ** */
    /** ************************************************************* */
    public void doShowAnswer(String name) {
        m_botAction.sendSmartPrivateMessage(name, "The answer is " + t_word + ".");
        cantPlay.add(name);
    }
    
    /** ************************************************************* */
    /** * Passes the artist's turn to a random player. ** */
    /** ************************************************************* */    
    public void doPass(String name) {
        if (!opList.isER(name) && !name.equals(curArtist))
            return;
        String passing = curArtist;
        m_botAction.specWithoutLock(curArtist);
        while (passing.equals(curArtist)) {
            pickPlayer();
        }
        m_botAction.sendArenaMessage(passing + " passes to " + curArtist + ".");
        cantPlay.clear();
        cantPlay.add(curArtist);
        try {
            m_botAction.cancelTasks();
        } catch (Exception e) {}
        grabWord();
        if (custGame)
            m_botAction.sendSmartPrivateMessage(curArtist, "Private message me what you're drawing or type !ready for me to pick something for you.");
        else {
            grabWord();
            doReadyCheck();
        }
    }
    
    /**
     * Selects a random player in the arena.
     */
    public void pickPlayer() {
        // pick a random player.
        Player p;
        String addPlayerName;
        StringBag randomPlayerBag = new StringBag();
        if (m_botAction.getArenaSize() > minPlayers) {
            Iterator<Player> i = m_botAction.getPlayerIterator();
            if (i == null)
                return;
            while (i.hasNext()) {
                p = (Player) i.next();
                addPlayerName = p.getPlayerName();
                randomPlayerBag.add(addPlayerName);
            }
            addPlayerName = randomPlayerBag.grabAndRemove();
            if (!addPlayerName.equals(m_botAction.getBotName()) && !notPlaying.contains(addPlayerName))
                curArtist = addPlayerName;
            else
                pickPlayer();
        } else {
            m_botAction.sendArenaMessage(m_prec + "There are not enough players to procede.");
            doCancelGame();
        }
        
    }
    
    /** ************************************************************* */
    /** * Checks to see if the player is ready to draw. ** */
    /** ************************************************************* */
    public void doReadyCheck() {
        if (ready) {
            try {
                m_botAction.cancelTasks();
            } catch (Exception e) {}
            ready = false;
            doDraw();
        } else {
            warn = new TimerTask() {
                public void run() {
                    m_botAction.sendSmartPrivateMessage(curArtist, "Private message me with !ready or your turn will be forfeited.");
                    forcePass = new TimerTask() {
                        public void run() {
                            doPass(m_botAction.getBotName());
                        }
                    };
                    m_botAction.scheduleTask(forcePass, 15000);
                }
            };
            if (custGame) {
                m_botAction.sendSmartPrivateMessage(curArtist, "Word set. Private message me with !ready to begin or choose a different word.");
                m_botAction.scheduleTask(warn, 15000);
            } else {
                m_botAction.sendSmartPrivateMessage(curArtist, "You've been chosen to draw. Please private message me with !ready to begin or !pass to pass.");
                m_botAction.scheduleTask(warn, 15000);
            }
        }
    }
    
    /** ************************************************************* */
    /** * Let's the bot know the current artist is ready to draw.  ** */
    /** ************************************************************* */
    public void doReady(String name) {
        if (!name.equals(curArtist))
            return;
        ready = true;
        if (t_word.equals(lastWord))
            grabWord();
        try {
            m_botAction.cancelTasks();
        } catch (Exception e) {}
        doReadyCheck();
    }
    
    /** ************************************************************* */
    /** * Drawing Begins. ** */
    /** ************************************************************* */
    public void doDraw() {
        m_botAction.sendArenaMessage(m_prec + "Picture #" + pictNumber + ":");
        m_botAction.setShip(curArtist, 1);
        m_botAction.warpTo(curArtist, XSpot, YSpot);
        m_botAction.sendSmartPrivateMessage(curArtist, "Draw: " + t_word);
        timerQuestion = new TimerTask() {
            public void run() {
                if (gameProgress == READY_CHECK) {
                    gameProgress = DRAWING;
                    // Date d = new Date();
                    giveTime = new java.util.Date().getTime();
                    m_botAction.sendArenaMessage(m_prec + "GO GO GO!!!", 104);
                    lastWord = t_word;
                    displayHint();
                }
            }
        };
        
        m_botAction.scheduleTask(timerQuestion, m_timeQuestion);
    }
    
    /** ************************************************************* */
    /** * Displays the Hint. ** */
    /** ************************************************************* */
    public void displayHint() {
        timerHint = new TimerTask() {
            public void run() {
                if (gameProgress == DRAWING) {
                    gameProgress = HINT_GIVEN;
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
                if (gameProgress == HINT_GIVEN) {
                    gameProgress = ANSWER_GIVEN;
                    m_botAction.sendArenaMessage(m_prec + "No one has given the correct answer of '" + t_word + "'", 103);
                    
                    doCheckScores();
                    theWinner = m_botAction.getBotName();
                    startNextRound();
                }
            }
        };
        
        m_botAction.scheduleTask(timerAnswer, m_timeAnswer);
    }
    
    /** ************************************************************* */
    /** * Gets a word from the database. ** */
    /** ************************************************************* */
    public void grabWord() {
        try {
            ResultSet qryWordData;
            qryWordData = m_botAction.SQLQuery(mySQLHost, "SELECT WordID, Word FROM tblPict_Words WHERE TimesUsed=" + getMinTimesUsed() + " AND CHAR_LENGTH(Word) > 4 ORDER BY RAND(" + m_rnd.nextInt() + ") LIMIT 1");
            if (qryWordData.next()) {
                t_word = qryWordData.getString("Word").toLowerCase();
                if (t_word.trim().split(" ").length > 1)
                    t_definition = t_word.trim().split(" ").length + " words: First word begins with '" + t_word.substring(0, 1) + "'.";
                else
                    t_definition = "Begins with " + t_word.substring(0, 1) + ".";
                int ID = qryWordData.getInt("WordID");
                m_botAction.SQLQueryAndClose(mySQLHost, "UPDATE tblPict_Words SET TimesUsed = TimesUsed + 1 WHERE WordID = " + ID);
            }
            m_botAction.SQLClose(qryWordData);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }
    
    /** ************************************************************* */
    /** * Gets a custom word from the artist. ** */
    /** ************************************************************* */
    public void doCustomWord(String name, String message) {
        if (name.equalsIgnoreCase(curArtist) && custGame && gameProgress == READY_CHECK) {
            if (message.length() < 13) {
                if (isAllLetters(message)) {
                    t_word = message.toLowerCase().trim();
                    t_definition = "The word begins with '" + t_word.substring(0, 1) + "'.";
                    m_botAction.sendSmartPrivateMessage(curArtist, "Word to draw: " + t_word);
                    try {
                        m_botAction.cancelTasks();
                    } catch (Exception e) {}
                    doReadyCheck();
                } else
                    m_botAction.sendSmartPrivateMessage(curArtist, "Please choose one word with no spaces or special characters.");
            } else
                m_botAction.sendSmartPrivateMessage(curArtist, "Please pick a word of 12 letters or less.");
        }
    }
    
    /** ************************************************************* */
    /** * Returns true if the string is entirely made of letters.  ** */
    /** ************************************************************* */
    public boolean isAllLetters(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!java.lang.Character.isLetter(s.charAt(i)))
                return false;
        }
        return true;
    }
    
    /** ************************************************************* */
    /** * Checks Team Chat for answer. ** */
    /** ************************************************************* */    
    public void doCheckAnswers(String name, String message) {
        if ((gameProgress == DRAWING) || (gameProgress == HINT_GIVEN)) {
            String curAns = t_word.replaceAll(" ", "");
            String msg = message.toLowerCase().replaceAll(" ", "");
            if (msg.contains(curAns)) {
                if (!cantPlay.contains(name) && !notPlaying.contains(name)) {
                    theWinner = name;
                    answerSpeed = (new java.util.Date().getTime() - giveTime) / 1000.0;
                    if (playerMap.containsKey(name)) {
                        twcore.core.stats.PlayerProfile tempP = playerMap.get(name);
                        // data 0 stores the score.
                        tempP.incData(0);
                        if (tempP.getData(0) >= toWin) {
                            doEndGame(name);
                            return;
                        }
                    } else {
                        playerMap.put(name, new twcore.core.stats.PlayerProfile(name));
                        twcore.core.stats.PlayerProfile tempP = playerMap.get(name);
                        tempP.setData(0, 1);
                        if (tempP.getData(0) >= toWin) {
                            doEndGame(name);
                            return;
                        }
                    }
                    twcore.core.stats.PlayerProfile tempP = playerMap.get(name);
                    if (answerSpeed < 5) {
                        String trail = getRank(tempP.getData(0));
                        m_botAction.sendArenaMessage(m_prec + "Cheater! " + name + " got the correct answer, '" + t_word + "' in only " + trail, 13);
                    } else if (answerSpeed < 25 && answerSpeed > 5) {
                        String trail = getRank(tempP.getData(0));
                        m_botAction.sendArenaMessage(m_prec + "Inconceivable! " + name + " got the correct answer, '" + t_word + "' in only " + trail, 7);
                    } else {
                        String trail = getRank(tempP.getData(0));
                        m_botAction.sendArenaMessage(m_prec + name + " got the correct answer, '" + t_word + "', " + trail, 103);
                    }
                    if (gameProgress != NO_GAME_IN_PROGRESS) {
                        gameProgress = ANSWER_GIVEN;
                        try {
                            m_botAction.cancelTasks();
                        } catch (Exception e) {}
                        TimerTask adm = new TimerTask() {
                            public void run() {
                                startNextRound();
                            }
                        };
                        m_botAction.scheduleTask(adm, admireArt);
                    }
                } else {
                    if (name.equals(curArtist)) {
                        m_botAction.sendSmartPrivateMessage(name, "A point has been deducted from your score for showing the answer.");
                        if (playerMap.containsKey(curArtist)) {
                            PlayerProfile tempPlayer = playerMap.get(curArtist);
                            tempPlayer.decData(0);
                        } else {
                            playerMap.put(curArtist, new PlayerProfile(curArtist));
                            PlayerProfile tempPlayer = playerMap.get(curArtist);
                            tempPlayer.setData(0, -1);
                        }
                    } else {
                        m_botAction.sendSmartPrivateMessage(name, "You are not allowed to guess.");
                    }
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
        String pts = " points";
        if (score == 1)
            pts = " point";
        if (score > curLeader) {
            curLeader = score;
            return speed + "and is in the lead with " + score + pts;
        } else if (score == curLeader)
            return speed + "and is tied for the lead with " + score + pts + "!";
        else
            return speed + "and has " + score + pts + ".";
    }
    
    /** ************************************************************* */
    /** * Shows scores. ** */
    /** ************************************************************* */
    public void doCheckScores() {
        
        if ((pictNumber % 2 == 0) && (curLeader != 0)) {
            String m_prec2 = m_prec.replaceAll(" ", "");
            int numberShown = 0, curPoints = curLeader;
            m_botAction.sendArenaMessage(m_prec2 + "-----------------------------|");
            m_botAction.sendArenaMessage(m_prec + doTrimString("Top Scores", 28) + "|");
            m_botAction.sendArenaMessage(m_prec2 + "-----------------------------|");
            m_botAction.sendArenaMessage(m_prec + doTrimString("Player Name", 20) + doTrimString("Points", 8) + "|");
            while (numberShown < 5 && curPoints != 0) {
                Iterator<String> it = playerMap.keySet().iterator();
                while (it.hasNext() && numberShown < 5) {
                    String curPlayer = (String) it.next();
                    twcore.core.stats.PlayerProfile tempPlayer;
                    tempPlayer = playerMap.get(curPlayer);
                    if (tempPlayer.getData(0) == curPoints) {
                        numberShown++;
                        m_botAction.sendArenaMessage(m_prec + doTrimString(curPlayer, 20) + doTrimString("" + tempPlayer.getData(0), 8) + "|");
                    }
                }
                curPoints--;
            }
            m_botAction.sendArenaMessage(m_prec2 + "-----------------------------|");
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
    
    /** ************************************************************* */
    /** * Gets minimum times used for questions. ** */
    /** ************************************************************* */
    public int getMinTimesUsed() {
        try {
            int minUsed = -1;
            ResultSet qryMinTimesUsed = m_botAction.SQLQuery(mySQLHost, "SELECT MIN(TimesUsed) AS MinTimesUsed FROM tblPict_Words");
            if (qryMinTimesUsed.next())
                minUsed = qryMinTimesUsed.getInt("MinTimesUsed");
            m_botAction.SQLClose(qryMinTimesUsed);
            return minUsed;
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
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT fcUserName, fnPoints, fnPlayed, fnWon, fnPossible, fnRating FROM tblPict_UserStats WHERE fnPossible >= 30 ORDER BY fnRating DESC LIMIT 10");
            while (result != null && result.next())
                topTen.add(doTrimString(result.getString("fcUsername"), 17) + "Games Won (" + doTrimString("" + result.getInt("fnWon") + ":" + result.getInt("fnPlayed") + ")", 9) + "Pts Scored (" + doTrimString("" + result.getInt("fnPoints") + ":" + result.getInt("fnPossible") + ")", 10) + "Rating: " + result.getInt("fnRating"));
            m_botAction.SQLClose(result);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }
    
    /** ************************************************************* */
    /** * Stores player statistics. ** */
    /** ************************************************************* */    
    public void storePlayerStats(String username, int points, boolean won) {
        try {
            int wonAdd = 0;
            if (won)
                wonAdd = 1;
            
            ResultSet qryHasPictRecord = m_botAction.SQLQuery(mySQLHost, "SELECT fcUserName, fnPlayed, fnWon, fnPoints, fnPossible FROM tblPict_UserStats WHERE fcUserName = \"" + username + "\"");
            if (!qryHasPictRecord.next()) {
                double rating = ((points + .0) / toWin * 750.0) * (1.0 + (wonAdd / 3.0));
                m_botAction.SQLQueryAndClose(mySQLHost, "INSERT INTO tblPict_UserStats(fcUserName, fnPlayed, fnWon, fnPoints, fnPossible, fnRating) VALUES (\"" + username + "\",1," + wonAdd + "," + points + "," + toWin + "," + rating + ")");
            } else {
                double played = qryHasPictRecord.getInt("fnPlayed") + 1.0;
                double wins = qryHasPictRecord.getInt("fnWon") + wonAdd;
                double pts = qryHasPictRecord.getInt("fnPoints") + points;
                double pos = qryHasPictRecord.getInt("fnPossible") + toWin;
                double rating = (pts / pos * 750.0) * (1.0 + (wins / played / 3.0));
                m_botAction.SQLQueryAndClose(mySQLHost, "UPDATE tblPict_UserStats SET fnPlayed = fnPlayed+1, fnWon = fnWon + " + wonAdd + ", fnPoints = fnPoints + " + points + ", fnPossible = fnPossible + " + toWin + ", fnRating = " + rating + " WHERE fcUserName = \"" + username + "\"");
            }
            m_botAction.SQLClose(qryHasPictRecord);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }
    
    /** ************************************************************* */
    /** * Gets player statistics. ** */
    /** ************************************************************* */
    public String getPlayerStats(String username) {
        try {
            
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT fnPoints, fnWon, fnPlayed, fnPossible, fnRating FROM tblPict_UserStats WHERE fcUserName = \"" + username + "\"");
            String info = "There is no record of player '" + username + ";. Please make sure you are using the entire name.";
            if (result.next())
                info = username + "- Games Won: (" + result.getInt("fnWon") + ":" + result.getInt("fnPlayed") + ")  Pts Scored: (" + result.getInt("fnPoints") + ":" + result.getInt("fnPossible") + ")  Rating: " + result.getInt("fnRating");
            m_botAction.SQLClose(result);
            return info;
        } catch (Exception e) {
            Tools.printStackTrace(e);
            return "Can't retrieve stats.";
        }
    }
    
    /** ************************************************************* */
    /** * Displays Pictionary's top ten players.                   ** */
    /** ************************************************************* */
    public void doTopTen(String name) {
        if (topTen.size() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "No one has qualified yet!");
        } else {
            for (int i = 0; i < topTen.size(); i++) {
                m_botAction.sendSmartPrivateMessage(name, topTen.elementAt(i));
            }
        }
    }
    
    /** ************************************************************* */
    /** * Displays a user's statistics. ** */
    /** ************************************************************* */
    public void doStats(String name, String message) {
        if (gameProgress == NO_GAME_IN_PROGRESS) {
            m_botAction.sendSmartPrivateMessage(name, "Displaying stats, please hold.");
            if (message.trim().length() > 0) {
                m_botAction.sendSmartPrivateMessage(name, getPlayerStats(message));
            } else {
                m_botAction.sendSmartPrivateMessage(name, getPlayerStats(name));
            }
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Please check your stats when Pictionary is not running.");
        }
    }
    
    /** ************************************************************* */
    /** * Displays the current score. ** */
    /** ************************************************************* */
    public void doScore(String name) {
        
        if (gameProgress != NO_GAME_IN_PROGRESS) {
            m_botAction.sendSmartPrivateMessage(name, "This game is to " + toWin + " points.");
            m_botAction.sendSmartPrivateMessage(name, m_prec + "----------------------------");
            m_botAction.sendSmartPrivateMessage(name, m_prec + doTrimString("Current Scores", 28) + "|");
            m_botAction.sendSmartPrivateMessage(name, m_prec + doTrimString("Player Name", 18) + doTrimString("Points", 10) + "|");
            int curPoints = curLeader;
            while (curPoints != 0) {
                Iterator<String> it = playerMap.keySet().iterator();
                while (it.hasNext()) {
                    String curPlayer = (String) it.next();
                    twcore.core.stats.PlayerProfile tempPlayer;
                    tempPlayer = playerMap.get(curPlayer);
                    if (tempPlayer.getData(0) == curPoints) {
                        m_botAction.sendSmartPrivateMessage(name, m_prec + doTrimString(curPlayer, 18) + doTrimString("" + tempPlayer.getData(0), 10) + "|");
                    }
                }
                curPoints--;
            }
            m_botAction.sendSmartPrivateMessage(name, m_prec + "----------------------------");
        }
    }
    
    /** ************************************************************* */
    /** * Shows the users score. ** */
    /** ************************************************************* */
    public void doMyScore(String name) {
        if (gameProgress != NO_GAME_IN_PROGRESS) {
            if (playerMap.containsKey(name)) {
                PlayerProfile temp = playerMap.get(name);
                m_botAction.sendSmartPrivateMessage(name, "You currently have " + temp.getData(0) + " points.");
            } else {
                m_botAction.sendSmartPrivateMessage(name, "You currently have 0 points.");
            }
        }
    }
    
    /** ************************************************************* */
    /** * Starts the next round. ** */
    /** ************************************************************* */
    public void startNextRound() {
        if (gameProgress == ANSWER_GIVEN) {
            gameProgress = READY_CHECK;
            pictNumber++;
            m_botAction.specWithoutLock(curArtist);
            if (theWinner.equals(m_botAction.getBotName())) {
                String temp = curArtist;
                while (temp.equals(curArtist)) {
                    pickPlayer();
                }
            } else
                curArtist = theWinner;
            cantPlay.clear();
            cantPlay.add(curArtist);
            if (custGame)
                m_botAction.sendSmartPrivateMessage(curArtist, "Private message me what you're drawing or type !ready for me to pick something for you.");
            else {
                grabWord();
                doReadyCheck();
            }
            
        }
    }
    
    /** ************************************************************* */
    /** * Ends the game, stores results. ** */
    /** ************************************************************* */
    public void doEndGame(String name) {
        gameProgress = NO_GAME_IN_PROGRESS;
        curLeader = 0;
        pictNumber = 1;
        m_botAction.sendArenaMessage(m_prec + "Answer: '" + t_word + "'");
        m_botAction.sendArenaMessage(m_prec + "Player: " + name + " has won this round of Pictionary!", 5);
        // Save statistics
        Iterator<String> it = playerMap.keySet().iterator();
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
        toWin = 10;
        try {
            m_botAction.cancelTasks();
        } catch (Exception e) {}
        m_botAction.specAll();
    }
    
}