package twcore.bots.multibot.acro;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.Message;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Spy;
import twcore.core.util.StringBag;
import twcore.core.util.Tools;

public class acro extends MultiModule {

    CommandInterpreter m_commandInterpreter;
    Random generator;
    // gameState   -1 = pregame; 0 = not playing; 1 = entering acro; 2 = voting, 3 = waiting for host to submit acro
    int gameState = 0;
    int length = 0;
    int intOrder = 0;
    int intAcroCount = 0;
    int round = 1;
    int intCustom = 0; // is set to 1 if each rounds acro will be submitted by host
    String curAcro = "";
    String CustomHost = "";
    HashSet<String> ignoreList = new HashSet<String>();
    HashMap<String, String> playerIdeas = new HashMap<String, String>();
    HashMap<String, Integer> playerVotes = new HashMap<String, Integer>();
    HashMap<String, AcroScore> playerScores = new HashMap<String, AcroScore>();
    HashMap<String, Integer> playerOrder = new HashMap<String, Integer>();
    HashMap<String, Integer> acroDisplay = new HashMap<String, Integer>();
    TreeMap<Integer, Character> chanceTable = new TreeMap<Integer, Character>();    // Lookup table for letter generation.
    StringBag playerNames = new StringBag();
    int votes[];
    Spy racismSpy;

    /**
        This table contains the relative weights of each letter. As long as the total value of the supplied numbers
        are between 0 and 2,147,483,647 (inclusive), and the individual numbers are whole and positive, then it will work.
        Putting a 0 in a field disables the letter.
        <p>
        Example: (Taken from http://en.wikipedia.org/wiki/Letter_frequency, first letter frequencies.)
        <pre>private static final int[] letterWeights = {
        //      A,     B,     C,     D,     E,     F,     G,     H,     I,     J,
           11602,  4702,  3511,  2670,  2007,  3779,  1950,  7232,  6286,   597,
        //      K,     L,     M,     N,     O,     P,     Q,     R,     S,     T,
             590,  2705,  4374,  2365,  6264,  2545,   173,  1653,  7755, 16671,
        //      U,     V,     W,     X,     Y,     Z
            1487,   649,  6753,    37,  1620,    34
        };</pre>
    */
    private static final int[] letterWeights = {
        //  A, B, C, D, E, F, G, H, I, J,
        10, 10, 10, 10, 10, 10, 10, 10, 10, 6,
        //  K, L, M, N, O, P, Q, R, S, T,
        3, 10, 10, 10, 7, 10, 1, 10, 10, 10,
        //  U, V, W, X, Y, Z
        4, 3, 9, 1, 3, 1
    };

    @Override
    public void init() {
        generator = new Random();
        racismSpy = new Spy(m_botAction);

        fillChanceTable();

        m_botAction.sendUnfilteredPublicMessage("?chat=games,acro");
    }

    @Override
    public void requestEvents(ModuleEventRequester events) {
        events.request(this, EventRequester.MESSAGE);
    }

    @Override
    public boolean isUnloadable() {
        return true;
    }

    /*** This method is called when this module is unloaded. */
    @Override
    public void cancel() {
        gamereset();
        m_botAction.cancelTasks();
    }

    private void spamChats(String message) {
        m_botAction.sendChatMessage(1, message);
        m_botAction.sendChatMessage(2, message);
    }

    private void spamMessage(String message) {
        m_botAction.sendArenaMessage(message);
        spamChats(message);
    }

    private void spamMessage(String message, int sound) {
        m_botAction.sendArenaMessage(message, sound);
        spamChats(message);
    }

    public void parseMessage(Message event) {
        String message = event.getMessage();

        String name = event.getMessager();

        if (name == null || name.isEmpty()) {
            name = m_botAction.getPlayerName(event.getPlayerID());
        }

        String lower = message.toLowerCase();

        if (lower.equals("!start")) {
            doStartGame(name, message);
        } else if (lower.startsWith("!startcustom")) {
            doStartCustom(name, message);
        } else if (lower.startsWith("!setacro ")) {
            doSetAcro(name, message.substring(8));
        } else if (lower.startsWith("!stop")) {
            doStopGame(name, message);
        } else if (lower.startsWith("!help")) {
            doShowHelp(name, message);
        } else if (lower.startsWith("!rules")) {
            doShowRules(name, message);
        } else if (lower.startsWith("!changes")) {
            doShowChanges(name, message);
        } else if (lower.startsWith("!ignore")) {
            doAddIgnore(name, message.substring(8));
        } else if (lower.startsWith("!unignore")) {
            doRemoveIgnore(name, message.substring(10));
        } else if (lower.startsWith("!listignore")) {
            doListIgnore(name, message);
        } else if (gameState == 1) {
            doEntry(name, message);
        } else if (gameState == 2) {
            doVote(name, message);
        }
    }

    public void doStartGame(String name, String message) {
        initGame(name);
    }

    public void initGame(String name) {
        if (m_botAction.getOperatorList().isER(name)) {
            if (gameState == 0) {
                gameState = -1;

                if (intCustom == 1) {
                    gameState = 3;
                    CustomHost = name;
                    spamMessage("ACROMANIA BEGINS! Your host will submit acronyms - prepare your wit!  PM me with !rules to learn how to play. " + m_botAction.getBotName(), 22);
                    m_botAction.sendSmartPrivateMessage(name, "Custom Game Initalized.  Send !setacro LETTERS to set the letters for Round #1.");
                } else {
                    spamMessage("ACROMANIA BEGINS! Random acronyms will be generated - prepare your wit!  PM me with !rules to learn how to play. -" + m_botAction.getBotName(), 22);
                    TimerTask preStart = new TimerTask() {
                        public void run() {
                            setUpShow();
                        }
                    };
                    m_botAction.scheduleTask(preStart, 10000);
                }
            }
        }
    }

    public void doSetAcro(String name, String message) {
        if (m_botAction.getOperatorList().isER(name)) {
            if (intCustom == 1) {
                if (gameState == 3) {
                    message = message.trim();
                    message = message.toUpperCase();
                    message = message.replaceAll(" ", "");

                    if (message.length() > 8) {
                        m_botAction.sendSmartPrivateMessage(name, "Please submit an acronym 8 characters or less.");
                    } else {
                        curAcro = "";
                        // convert LETTERS to L E T T E R S
                        char letters[] = message.toCharArray();

                        for (int x = 0; x < letters.length; x++) {
                            curAcro = curAcro + " " + letters[x];
                        }

                        curAcro = curAcro.trim();
                        setUpShow();
                    }
                } else {
                    m_botAction.sendSmartPrivateMessage(name, "Round is not complete, please wait to submit next acronym.");
                }
            } else {
                m_botAction.sendSmartPrivateMessage(name, "Game is currently running in regular mode (!start).  Host-submitted acronyms are not allowed.");
            }
        }
    }

    public void doStartCustom(String name, String message) {
        intCustom = 1;
        initGame(name);
    }

    public void doStopGame(String name, String message) {
        if (m_botAction.getOperatorList().isER(name)) {
            gamereset();
            m_botAction.cancelTasks();
            spamMessage("This game has been slaughtered by: " + name);
        }
    }

    public void gamereset() {
        intCustom = 0;
        intOrder = 0;
        round = 1;
        gameState = 0;
        curAcro = "";
        CustomHost = "";

        playerScores.clear();
        playerIdeas.clear();
        playerVotes.clear();
        playerOrder.clear();
        acroDisplay.clear();
        ignoreList.clear();
    }

    public void doShowAnswers(String name, String message) {
        if (m_botAction.getOperatorList().isModerator(name)) {
            if (gameState == 2) {
                Iterator<String> it = playerIdeas.keySet().iterator();
                String player, answer;

                while (it.hasNext()) {
                    player = it.next();
                    answer = playerIdeas.get(player);
                    m_botAction.sendSmartPrivateMessage(name, player + ":  " + answer);
                }
            } else {
                m_botAction.sendSmartPrivateMessage(name, "Currently the game isn't in the voting stage.");
            }
        }
    }

    public void setUpShow() {
        gameState = 1;

        if (intCustom == 0) {
            length = Math.abs(generator.nextInt()) % 2 + 4;
            curAcro = generateAcro(length);
        } // otherwise, the curAcro global has already been set by doSetAcro

        spamMessage("TO ENTER, PM me a phrase that matches the challenge letters! -" + m_botAction.getBotName());
        spamMessage("ACROMANIA Challenge #" + round + ": " + curAcro);

        TimerTask end = new TimerTask() {
            public void run() {
                gameState = 2;
                spamMessage("ACROMANIA Entries: ");
                int i = 0;

                while (!playerNames.isEmpty()) {
                    i++;
                    String curPlayer = playerNames.grabAndRemove();
                    spamMessage("--- " + i + ": " + playerIdeas.get(curPlayer));
                    acroDisplay.put(curPlayer, i);
                }

                votes = new int[i];
                intAcroCount = i;

                if (intAcroCount > 0) {
                    spamMessage("VOTE: PM me the # of your favorite phrase! -" + m_botAction.getBotName(), 103);
                } else {
                    spamMessage("--- 0 entries submitted.");
                }

                setUpVotes();
            }
        };
        m_botAction.scheduleTask(end, Tools.TimeInMillis.MINUTE);
    }

    public String getPlural(Integer intCount, String strWord) {
        if (intCount > 1 || intCount == 0) {
            strWord += "s";
        } else {
            strWord += " ";
        }

        return intCount + " " + strWord;
    }

    public void setUpVotes() {
        TimerTask vote = new TimerTask() {
            public void run() {
                String strFastPlayer = "";
                String strWinners = "";
                int intMostVotes = 0;
                int numVotes = 0;
                int i = 0;

                numVotes = votes.length;

                // Determine the highest # of votes any acro received
                for (i = 0; i < numVotes; i++) {
                    if (votes[i] > intMostVotes) {
                        intMostVotes = votes[i];
                    }
                }

                // Determine the fastest ACRO that received at least one vote
                int intCurAcro = 0;
                int intCurOrder = 0;
                int intFastest = 100;
                String strCurPlayer = "";
                Set<String> acroSet = acroDisplay.keySet();
                Iterator<String> acroIT = acroSet.iterator();

                while (acroIT.hasNext()) {
                    strCurPlayer = acroIT.next();
                    intCurAcro = acroDisplay.get(strCurPlayer);
                    intCurOrder = playerOrder.get(strCurPlayer);

                    if ((intCurOrder < intFastest) && votes[intCurAcro - 1] > 0) {
                        intFastest = intCurOrder;
                        strFastPlayer = strCurPlayer;
                    }
                }

                //int intPlayerScore = 0;
                int intPlayerBonus = 0;
                int intPlayerVotes = 0;
                int intPlayerTotal = 0;
                int VotedForVotes = 0;
                spamMessage("ROUND " + round + " RESULTS: ");

                acroIT = acroSet.iterator();

                while (acroIT.hasNext()) {
                    strCurPlayer = acroIT.next();
                    intCurAcro = acroDisplay.get(strCurPlayer);
                    intPlayerVotes = votes[intCurAcro - 1];

                    String playerVotedWinner = "-";
                    String playerNotes = "";

                    // Calculate bonus points
                    intPlayerBonus = 0;

                    // +5 pts for receiving the most votes (round winner)
                    if (intPlayerVotes == intMostVotes) {
                        intPlayerBonus += 5;

                        if (strWinners.length() > 0) {
                            strWinners += ", ";
                        }

                        strWinners += strCurPlayer;
                    }

                    // +1 pt for voting for round winner
                    if (playerVotes.containsKey(strCurPlayer)) {
                        VotedForVotes = votes[playerVotes.get(strCurPlayer) - 1];

                        if (VotedForVotes == intMostVotes) {
                            intPlayerBonus += 1;
                            playerVotedWinner = "*";
                        }
                    }

                    // +2 pts if this was the fastest entry with any votes
                    if (strCurPlayer.equals(strFastPlayer)) {
                        intPlayerBonus += 2;
                    }

                    // Update players running score, only if they voted
                    if (playerVotes.containsKey(strCurPlayer)) {
                        AcroScore sc;

                        if (!playerScores.containsKey(strCurPlayer)) {
                            sc = new AcroScore(strCurPlayer, 0);
                        } else {
                            sc = playerScores.get(strCurPlayer);
                        }

                        // Score for round = bonus + number of votes their acro received
                        if( sc == null )
                            sc = new AcroScore(strCurPlayer, 0);

                        sc.updateScore( intPlayerBonus + intPlayerVotes );
                        playerScores.put(strCurPlayer, sc);
                    } else {
                        playerNotes += " [NOVOTE/NOSCORE]";
                    }

                    intPlayerTotal = intPlayerVotes + intPlayerBonus;
                    spamMessage(playerVotedWinner + " " + Tools.formatString(strCurPlayer, 14) + " " + getPlural(intPlayerTotal, "pt") + " (" + getPlural(intPlayerVotes, "vote")
                                + "): " + playerIdeas.get(strCurPlayer) + playerNotes);
                }

                if (!strWinners.equals("")) {
                    spamMessage("* = These players voted for the winner(s).");

                    if (!strFastPlayer.equals("")) {
                        spamMessage("ROUND WINNER(s): " + strWinners + " (most votes), " + strFastPlayer + " (fastest acro with a vote)");
                    } else {
                        spamMessage("ROUND WINNER(s): " + strWinners + " with the most votes");
                    }
                } else {
                    spamMessage("ROUND WINNER(s): None!  You all lose!");
                }

                playerIdeas.clear();
                playerVotes.clear();
                playerOrder.clear();
                acroDisplay.clear();

                round++;

                if (round > 10) {
                    gameOver();
                } else {
                    if (intCustom == 1) {
                        gameState = 3;
                        m_botAction.sendSmartPrivateMessage(CustomHost, "Send !setacro LETTERS to set the letters for the next round.");
                    } else {
                        TimerTask preStart = new TimerTask() {
                            public void run() {
                                setUpShow();
                            }
                        };
                        m_botAction.scheduleTask(preStart, 10000);
                    }
                }
            }
        };
        m_botAction.scheduleTask(vote, 36000);
    }

    public void gameOver() {
        TimerTask game = new TimerTask() {
            public void run() {

                Collection<AcroScore> collfinals = playerScores.values();
                TreeSet<AcroScore> finals = new TreeSet<AcroScore>(collfinals);

                AcroScore winner = finals.last();

                if (winner != null)
                    spamMessage("ACRO GAME OVER! The winner is: >>> " + winner.getName() + " <<<", 5);

                Iterator<AcroScore> it = finals.descendingIterator();

                while (it.hasNext()) {
                    AcroScore sc = it.next();

                    if (sc != null)
                        spamMessage("--- " + Tools.formatString(sc.getName(), 14) + ": " + sc.getScore());
                }

                gamereset();
            }
        };
        m_botAction.scheduleTask(game, 10000);
    }

    private void doEntry(String name, String message) {
        if (ignoreList.contains(name.toLowerCase())) {
            m_botAction.sendSmartPrivateMessage(name, "You are restricted from submitting any answers. Please contact host for more details.");
        } else {

            String pieces[] = message.split(" +");
            String pieces2[] = curAcro.split(" ");

            if (pieces.length == pieces2.length) {
                boolean valid = true;

                if (message.length() > 70) {
                    valid = false;
                } else {
                    for (int i = 0; i < pieces.length; i++) {
                        if (pieces[i].length() == 0 || pieces[i].toLowerCase().charAt(0) != pieces2[i].toLowerCase().charAt(0)) {
                            valid = false;
                        }
                    }
                }

                if (valid) {
                    if (racismSpy.isRacist(message)) {
                        m_botAction.sendUnfilteredPublicMessage("?cheater Racist acro: (" + name + "): " + message);
                        m_botAction.sendSmartPrivateMessage(name, "You have been reported for attempting to use racism in your answer.");
                        return;
                    }

                    if (!playerIdeas.containsKey(name)) {
                        m_botAction.sendSmartPrivateMessage(name, "Your answer has been recorded. Vote for another player's to receive points.");
                        playerNames.add(name);
                    } else {
                        playerIdeas.remove(name);
                        playerOrder.remove(name);
                        m_botAction.sendSmartPrivateMessage(name, "Your answer has been changed.");
                    }

                    intOrder++;
                    playerOrder.put(name, intOrder);
                    playerIdeas.put(name, message);
                } else {
                    m_botAction.sendSmartPrivateMessage(name, "You have submitted an invalid acronym.  It must match the letters given and be 70 characters or less.");
                }
            } else
                m_botAction.sendSmartPrivateMessage(name, "You must use the correct number of letters!");
        }
    }

    private void doVote(String name, String message) {
        int vote = 0;

        try {
            vote = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            return;
        }

        if (vote > 0 && vote <= intAcroCount) {
            //if (playerIdeas.containsKey(name)) {
            boolean valid = acroDisplay.containsKey(name) ? acroDisplay.get(name) != vote : true;

            if (valid) {
                votes[vote - 1]++;

                if (playerVotes.containsKey(name)) {
                    int lastVote = playerVotes.get(name);
                    votes[lastVote - 1]--;
                    playerVotes.remove(name);
                    m_botAction.sendSmartPrivateMessage(name, "Your vote has been changed.");
                } else {
                    m_botAction.sendSmartPrivateMessage(name, "Your vote has been counted.");
                }

                playerVotes.put(name, vote);
            } else {
                m_botAction.sendSmartPrivateMessage(name, "You cannot vote for your own.");
            }

            //} else {m_botAction.sendSmartPrivateMessage(name,"Only players who submitted an entry may vote this round.");}
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Please enter a valid vote.");
        }
    }

    public String generateAcro(int size) {
        String acro = "";
        int total = chanceTable.lastKey();  // Total of all the weights in chanceTable.

        for (int i = 0; i < size; i++) {
            // Generate a number between 1 and total.
            int x = Math.abs(generator.nextInt(total)) + 1;

            try {
                // Grab the letter for which range the randomly generated x is in.
                acro += (chanceTable.get(chanceTable.ceilingKey(x)) + " ");
            } catch (Exception e) {
                // Something went wrong. Try again.
                i--;
            }
        }

        curAcro = acro;
        return acro;

    }

    public void doAddIgnore(String name, String message) {
        if (m_botAction.getOperatorList().isER(name)) {
            if (message == null) {
                m_botAction.sendSmartPrivateMessage(name, "Invalid Entry. Please try again.");
            } else {
                if (ignoreList.contains(message.toLowerCase())) {
                    m_botAction.sendSmartPrivateMessage(name, "Player is already on the ignore list.");
                } else {
                    ignoreList.add(message.toLowerCase());
                    m_botAction.sendSmartPrivateMessage(name, "" + message + " was added to the ignore list.");
                }
            }
        }
    }

    public void doRemoveIgnore(String name, String message) {
        if (m_botAction.getOperatorList().isER(name)) {
            if (message == null) {
                m_botAction.sendSmartPrivateMessage(name, "Invalid Entry. Please try again.");
            } else {
                if (!ignoreList.contains(message.toLowerCase())) {
                    m_botAction.sendSmartPrivateMessage(name, "Player is not found on the ignore list.");
                } else {
                    ignoreList.remove(message.toLowerCase());
                    m_botAction.sendSmartPrivateMessage(name, "" + message + " was removed from the ignore list.");
                }
            }
        }
    }

    public void doListIgnore(String name, String message) {
        if (m_botAction.getOperatorList().isER(name)) {
            if (ignoreList.isEmpty()) {
                m_botAction.sendSmartPrivateMessage(name, "Ignore List is Empty");
            } else {
                Iterator<String> list = ignoreList.iterator();

                while (list.hasNext()) {
                    m_botAction.sendSmartPrivateMessage(name, list.next());
                }
            }
        }
    }

    public void doShowHelp(String name, String message) {
        if (!m_botAction.getOperatorList().isER(name))
            m_botAction.smartPrivateMessageSpam(name, getPlayerHelpMessage());
    }

    public String[] getModHelpMessage() {
        String[] help = {
            "ACROMANIA v2.0 BOT COMMANDS",
            "!start       - Starts a game of acromania.",
            "!stop        - Stops a game currently in progress.",
            "!startcustom - Starts a game of acromania.  Host must",
            "               !setacro each round.",
            "!setacro     - Used to set the acronym for the next round",
            "               (can be used only during a !startcustom game)",
            "!rules       - Displays game rules.",
            "!changes     - Display changes since v1.0.",
            "!showanswers - Shows who has entered which answer.",
            "!ignore <name> - Prevents the player from submitting any answers for this game.",
            "!unignore <name> - Removes player from the ignore list and allows submissions.",
            "!listIgnore  - Lists the current players added to the ignore list."
        };
        return help;
    }

    public String[] getPlayerHelpMessage() {
        String[] help = { "ACROMANIA v2.0 BOT COMMANDS", "!help    - Displays this help message.", "!rules   - Displays game rules.", "!changes - Display changes since v1.0." };
        return help;
    }

    public void doShowRules(String name, String message) {
        String[] help = { "ACROMANIA v2.0 RULES:", "Each game consists of 10 rounds.  At the start of each round,", "a randomly generated acronym will be displayed.  PM me a phrase",
                          "that matches the letters provided.  Then vote for your favorite", "phrase.  You must submit an acro each round to be able to vote",
                          "during that round.  Points will be given as follows:", "+1 point for each vote that your acro receives", "+1 bonus point if you voted for the winning acro",
                          "+2 bonus points for the fastest acro that received a vote", "+5 bonus points for receiving the most votes for the round",
                          "After votes are tallied, all submitted acros are displayed along", "with the # of votes received + the bonus points received.",
                          "Players marked with an asterisk (*) voted for the winning acro.", "NO POINTS are given to players that did not vote.", "Win the game by earning the most points in 10 rounds."
                        };
        m_botAction.smartPrivateMessageSpam(name, help);
    }

    public void doShowChanges(String name, String message) {
        String[] help = { "ACROMANIA CHANGES v1.0 to v2.0:", " - underscore and dash characters now allowed in acros", " - can now change your vote during the voting round",
                          " - only players who submitted an acro can vote each round", " - new scoring system, see !rules", " - voting period now 36 seconds (was 30)",
                          " - fixed end of round display of acros to match original entries", "   (punctuation no longer stripped)", " - length of submitted acros can be no more than 70 characters",
                          " - added option for host to specify each rounds acros, based", "   on user request, random names in the channel, a theme, etc.",
                          " - current entries are now randomized before being displayed."
                        };
        m_botAction.smartPrivateMessageSpam(name, help);
    }

    @Override
    public void handleEvent(Message event) {
        if (event.getMessageType() == Message.PRIVATE_MESSAGE ||
                event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE) {
            parseMessage(event);
        }
    }

    /**
        Fills up the lookup table for the letter generation with the weights defined in {@link #letterWeights}.
    */
    private void fillChanceTable() {
        char counter = 65;  // Letter A.
        int total = 0;      // Total value of weights up to and including the current letter.

        // Clears the table.
        chanceTable.clear();

        for(int current : letterWeights) {
            // Skip the letter, if the weight is 0 or less.
            if(current > 0) {
                total += current;
                chanceTable.put(total, counter);
            }

            counter++;  // Go to the next letter (A -> B -> C -> ...)
        }
    }


    private class AcroScore implements Comparable<AcroScore> {
        String name;
        int score;

        public AcroScore( String name, int score ) {
            this.name = name;
            this.score = score;
        }

        public void updateScore( int amt ) {
            score += amt;
        }

        public int getScore() {
            return score;
        }

        public String getName() {
            return name;
        }

        public int compareTo( AcroScore sc2 ) {
            if( this.getScore() > sc2.getScore() )
                return 1;

            if( this.getScore() < sc2.getScore() )
                return -1;

            return 0;
        }
    }
}

