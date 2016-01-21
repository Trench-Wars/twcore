package twcore.bots.tictactoe;
//If You need lvz ?message Ahmad~:Need Lvz
//Import all of the TWCore classes so you can use them

import java.util.TimerTask;

import twcore.core.*;
import twcore.core.events.ArenaJoined;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.util.Tools;

public class tictactoe extends SubspaceBot {
    //Creates a new mybot

    enum Token {
        X(0), O(1), blank(2);

        int player;
        Token(int p) {
            player = p;
        }

        static Token get(int p) {
            switch (p) {
            case 0:
                return X;

            case 1:
                return O;

            default:
                return blank;
            }
        }
    }

    private BotAction ba;

    //Stores Staff Access Levels
    private OperatorList oplist;
    private String greeting;

    private Token[][] board;
    private String[] players;
    private Challenge challenge;
    private int playerTurn;
    private int [] xx = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private int [] oo = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static int random(int range) {
        return (int)(java.lang.Math.random() * (range + 1));
    }
    public tictactoe(BotAction botAction) {
        //This instantiates your BotAction
        super(botAction);

        ba = botAction;

        //Instantiate your EventRequester
        EventRequester events = ba.getEventRequester();
        //Request PlayerEntered events
        events.request(EventRequester.PLAYER_ENTERED);
        //Request chat message events
        events.request(EventRequester.MESSAGE);
        events.request(EventRequester.LOGGED_ON);
        events.request(EventRequester.ARENA_JOINED);

        //Instantiate your Operator List
        oplist = ba.getOperatorList();
        playerTurn = -1;
        board = null;
        players = null;
        challenge = null;
        greeting = null;
    }

    //What to do when the bot logs on
    public void handleEvent(LoggedOn event) {
        //Get the data from mybot.cfg
        BotSettings config = ba.getBotSettings();
        //Get the initial arena from config and enter it
        ba.joinArena(config.getString("InitialArena"));
    }

    public void handleEvent(ArenaJoined event) {
        greeting = ba.getBotSettings().getString("Greeting");
    }

    //What to do when a player enters the arena
    public void handleEvent(PlayerEntered event) {
        if (greeting == null) return;

        //Get the name of player that just entered
        String name = event.getPlayerName();
        //Greet them
        ba.sendPrivateMessage(name, greeting);
    }

    //What to do when somebody says something
    public void handleEvent(Message event) {
        String name = event.getMessager();

        if (name == null)
            name = ba.getPlayerName(event.getPlayerID());

        String msg = event.getMessage();
        int type = event.getMessageType();
        String cmd = msg.toLowerCase();

        if (type == Message.PUBLIC_MESSAGE || type == Message.PRIVATE_MESSAGE || type == Message.TEAM_MESSAGE) {

            if (cmd.startsWith("!help"))
                cmd_help(name);
            else if (cmd.startsWith("!ch "))
                cmd_challenge(name, msg);
            else if (cmd.startsWith("!a"))
                cmd_accept(name);
            else if (cmd.startsWith("!p "))
                cmd_play(name, msg);
            else if (cmd.startsWith("!cancel"))
                cmd_cancel(name);

            if (oplist.isER(name)) {
                if (cmd.startsWith("!stop"))
                    cmd_stop(name);
            }
        }

        if (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) {
            if (oplist.isER(name)) {
                if (cmd.startsWith("!go "))
                    cmd_go(name, msg);
                else if (cmd.startsWith("!die"))
                    new Die(name);
            }
        }
    }

    private void cmd_go(String name, String msg) {
        String arena = msg.substring(msg.indexOf(" ") + 1);

        if (arena.length() > 0 && !arena.toLowerCase().contains("public") && !arena.contains("(") && !Tools.isAllDigits(arena))
            ba.changeArena(arena);
        else
            ba.sendSmartPrivateMessage(name, "Invalid arena.");
    }
    private void cmd_help(String name) {
        String[] strs = {
            "+-- TicTacToe Commands ---------------------------------------------.",
            "| !ch <name>             - Challenges <name> if possible            |",
            "| !a                     - Accepts challenge if challenged          |",
            "| !cancel                - Cancels challenge if challenger          |",
            "| !p <row>,<col>         - Puts an X or O in the box at <row>,<col> |",
            "|                          as shown below                           |",
            "|                           cols   1   2   3                        |",
            "|                                +---+---+---.                      |",
            "|                           row 1|1,1|1,2|1,3|                      |",
            "|                                +---+---+---+                      |",
            "|                           row 2|2,1|2,2|2,3|                      |",
            "|                                +---+---+---+                      |",
            "|                           row 3|3,1|3,2|3,3|                      |",
            "|                                `---+---+---'                      |",
        };
        String end = "`-------------------------------------------------------------------'";
        String[] staff = {
            "+-- Staff Commands -------------------------------------------------+",
            "| !stop                  - Ends game or cancels a current challenge |",
            "| !go <arena>            - Sends the bot to <arena>                 |",
            "| !die                   - Kills bot                                |",
        };
        ba.privateMessageSpam(name, strs);

        if (oplist.isER(name))
            ba.privateMessageSpam(name, staff);

        ba.sendPrivateMessage(name, end);
    }

    private void cmd_challenge(String name, String msg) {
        if (board != null)
            ba.sendPrivateMessage(name, "There is already a game being played at the moment.");
        else if (challenge != null)
            ba.sendPrivateMessage(name, "Another player has a challenge active. It will expire in less than a minute if not accepted.");
        else {
            String challed = msg.substring(msg.indexOf(" ") + 1);
            challed = ba.getFuzzyPlayerName(challed);

            if (challed != null) {
                if (challed.equalsIgnoreCase(name))
                    ba.sendPrivateMessage(name, "You cannot challenge yourself.");
                else if (challed.equalsIgnoreCase(ba.getBotName()))
                    ba.sendPrivateMessage(name, "You cannot challenge bot");
                else
                    challenge = new Challenge(name, challed);
            } else
                ba.sendPrivateMessage(name, "Player not found in this arena.");
        }
    }
    private void cmd_accept(String name) {
        if (board != null)
            ba.sendPrivateMessage(name, "There is a game in progress so no challenge can be active.");
        else if (challenge != null)
            challenge.accept(name);
        else
            ba.sendPrivateMessage(name, "No one has challenged you.");
    }

    private void cmd_cancel(String name) {
        if (challenge != null) {
            challenge.cancel(name);
        }
    }

    private void cmd_stop(String name) {
        if (board != null) {
            players = null;
            board = null;
            playerTurn = -1;
            // O //
            m_botAction.sendUnfilteredPublicMessage("*objoff 90");
            m_botAction.sendUnfilteredPublicMessage("*objoff 91");
            m_botAction.sendUnfilteredPublicMessage("*objoff 92");
            m_botAction.sendUnfilteredPublicMessage("*objoff 93");
            m_botAction.sendUnfilteredPublicMessage("*objoff 94");
            m_botAction.sendUnfilteredPublicMessage("*objoff 95");
            m_botAction.sendUnfilteredPublicMessage("*objoff 96");
            m_botAction.sendUnfilteredPublicMessage("*objoff 97");
            m_botAction.sendUnfilteredPublicMessage("*objoff 1");
            m_botAction.sendUnfilteredPublicMessage("*objoff 2");
            m_botAction.sendUnfilteredPublicMessage("*objoff 3");
            m_botAction.sendUnfilteredPublicMessage("*objoff 4");
            m_botAction.sendUnfilteredPublicMessage("*objoff 5");
            m_botAction.sendUnfilteredPublicMessage("*objoff 6");
            m_botAction.sendUnfilteredPublicMessage("*objoff 7");
            m_botAction.sendUnfilteredPublicMessage("*objoff 8");
            m_botAction.sendUnfilteredPublicMessage("*objoff 9");
            // X //
            m_botAction.sendUnfilteredPublicMessage("*objoff 10");
            m_botAction.sendUnfilteredPublicMessage("*objoff 11");
            m_botAction.sendUnfilteredPublicMessage("*objoff 12");
            m_botAction.sendUnfilteredPublicMessage("*objoff 13");
            m_botAction.sendUnfilteredPublicMessage("*objoff 14");
            m_botAction.sendUnfilteredPublicMessage("*objoff 15");
            m_botAction.sendUnfilteredPublicMessage("*objoff 16");
            m_botAction.sendUnfilteredPublicMessage("*objoff 17");
            m_botAction.sendUnfilteredPublicMessage("*objoff 18");
            ba.sendArenaMessage("This game has been killed by " + name + ".");
        } else if (challenge != null) {
            challenge.cancel(name);
        }
    }

    private void cmd_play(String name, String msg) {
        if (board == null) return;

        if (getPlayer(name) != playerTurn) {
            ba.sendSmartPrivateMessage(name, "It is not your turn.");
            return;
        }
        else if (getPlayer(name) == playerTurn) {
            msg = msg.substring(msg.indexOf(" ") + 1).trim();
            int x = 0;
            int y = 0;

            try {
                if (msg.length() == 3 && msg.indexOf(",") == 1) {
                    String[] vals = msg.split(",");

                    if (Integer.valueOf(vals[0]) > 0 || Integer.valueOf(vals[1]) > 0 || Integer.valueOf(vals[0]) < 4 || Integer.valueOf(vals[1]) < 4 )
                    {
                        x = Integer.valueOf(vals[0]);
                        y = Integer.valueOf(vals[1]);
                    }

                    if (x < 1 || y < 1 || x > 3 || y > 3)
                        throw new NumberFormatException();
                } else
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                ba.sendSmartPrivateMessage(name, "Invalid syntax, please specify the box using !p <row>,<col>");
                return;
            }

            x--;
            y--;

            if (board[x][y] != Token.blank) {
                ba.sendSmartPrivateMessage(name, "You must choose an empty box.");
            } else {
                // O Player //
                if (name == players[1])
                {
                    // +---+---+---+
                    // | X | X | X |
                    // |   |   |   |
                    // |   |   |   |
                    // +---+---+---+
                    if (x == 0)if(y == 0)
                        {
                            // O //
                            m_botAction.sendUnfilteredPublicMessage("*objon 1");
                            oo[1] = 1;
                        }

                    if (x == 0)if(y == 1)
                        {
                            // O //
                            m_botAction.sendUnfilteredPublicMessage("*objon 4");
                            oo[4] = 1;
                        }

                    if (x == 0)if(y == 2)
                        {
                            // O //
                            m_botAction.sendUnfilteredPublicMessage("*objon 7");
                            oo[7] = 1;
                        }

                    // +---+---+---+
                    // |   |   |   |
                    // | x | x | x |
                    // |   |   |   |
                    // +---+---+---+
                    if (x == 1)if(y == 0)
                        {
                            // O //
                            m_botAction.sendUnfilteredPublicMessage("*objon 2");
                            oo[2] = 1;
                        }

                    if (x == 1)if(y == 1)
                        {
                            // O //
                            m_botAction.sendUnfilteredPublicMessage("*objon 5");
                            oo[5] = 1;
                        }

                    if (x == 1)if(y == 2)
                        {
                            // O //
                            m_botAction.sendUnfilteredPublicMessage("*objon 8");
                            oo[8] = 1;
                        }

                    // +---+---+---+
                    // |   |   |   |
                    // |   |   |   |
                    // | x | x | x |
                    // +---+---+---+
                    if (x == 2)if(y == 0)
                        {
                            // O //
                            m_botAction.sendUnfilteredPublicMessage("*objon 3");
                            oo[3] = 1;
                        }

                    if (x == 2)if(y == 1)
                        {
                            // O //
                            m_botAction.sendUnfilteredPublicMessage("*objon 6");
                            oo[6] = 1;
                        }

                    if (x == 2)if(y == 2)
                        {
                            // O //
                            m_botAction.sendUnfilteredPublicMessage("*objon 9");
                            oo[9] = 1;
                        }
                }

                // X Player //
                else if (name == players[0])
                {
                    // +---+---+---+
                    // | X | X | X |
                    // |   |   |   |
                    // |   |   |   |
                    // +---+---+---+
                    if (x == 0)if(y == 0)
                        {
                            // X //
                            m_botAction.sendUnfilteredPublicMessage("*objon 10");
                            xx[1] = 1;
                        }

                    if (x == 0)if(y == 1)
                        {
                            // X //
                            m_botAction.sendUnfilteredPublicMessage("*objon 13");
                            xx[4] = 1;
                        }

                    if (x == 0)if(y == 2)
                        {
                            // X //
                            xx[7] = 1;
                            m_botAction.sendUnfilteredPublicMessage("*objon 16");
                        }

                    // +---+---+---+
                    // |   |   |   |
                    // | x | x | x |
                    // |   |   |   |
                    // +---+---+---+
                    if (x == 1)if(y == 0)
                        {
                            // X //
                            m_botAction.sendUnfilteredPublicMessage("*objon 11");
                            xx[2] = 1;
                        }

                    if (x == 1)if(y == 1)
                        {
                            // X //
                            xx[5] = 1;
                            m_botAction.sendUnfilteredPublicMessage("*objon 14");
                        }

                    if (x == 1)if(y == 2)
                        {
                            // X //
                            xx[8] = 1;
                            m_botAction.sendUnfilteredPublicMessage("*objon 17");
                        }

                    // +---+---+---+
                    // |   |   |   |
                    // |   |   |   |
                    // | x | x | x |
                    // +---+---+---+
                    if (x == 2)if(y == 0)
                        {
                            // X //
                            xx[3] = 1;
                            m_botAction.sendUnfilteredPublicMessage("*objon 12");
                        }

                    if (x == 2)if(y == 1)
                        {
                            // X //
                            xx[6] = 1;
                            m_botAction.sendUnfilteredPublicMessage("*objon 15");
                        }

                    if (x == 2)if(y == 2)
                        {
                            // X //
                            xx[9] = 1;
                            m_botAction.sendUnfilteredPublicMessage("*objon 18");
                        }
                }

                board[x][y] = Token.get(playerTurn);
                Token winner = getWinner();

                if (winner != null) {
                    if (winner == Token.blank)
                        ba.sendArenaMessage("GAME OVER: Draw!", 5);
                    else
                        ba.sendArenaMessage("GAME OVER: " + players[winner.player] + " wins as " + winner.toString() + "'s!" , 5);

                    if (winner.player == 1)
                    {
                        // O Wins //
                        m_botAction.sendUnfilteredPublicMessage("*objon 21");
                    }

                    if (winner.player == 0)
                    {
                        // X Wins //
                        m_botAction.sendUnfilteredPublicMessage("*objon 20");
                    }

                    players = null;
                    board = null;
                    playerTurn = -1;
                } else
                    getTurn();
            }
        }
    }

    private void newGame() {
        ba.sendArenaMessage("A game of TicTacToe is starting between " + challenge.challer + " and " + challenge.challed + "!", 19);
        board = new Token[][] {
            { Token.blank, Token.blank, Token.blank },
            { Token.blank, Token.blank, Token.blank },
            { Token.blank, Token.blank, Token.blank },
        };
        m_botAction.sendUnfilteredPublicMessage("*objoff 90");
        m_botAction.sendUnfilteredPublicMessage("*objoff 91");
        m_botAction.sendUnfilteredPublicMessage("*objoff 92");
        m_botAction.sendUnfilteredPublicMessage("*objoff 93");
        m_botAction.sendUnfilteredPublicMessage("*objoff 94");
        m_botAction.sendUnfilteredPublicMessage("*objoff 95");
        m_botAction.sendUnfilteredPublicMessage("*objoff 96");
        m_botAction.sendUnfilteredPublicMessage("*objoff 97");
        m_botAction.sendUnfilteredPublicMessage("*objoff 1");
        m_botAction.sendUnfilteredPublicMessage("*objoff 2");
        m_botAction.sendUnfilteredPublicMessage("*objoff 3");
        m_botAction.sendUnfilteredPublicMessage("*objoff 4");
        m_botAction.sendUnfilteredPublicMessage("*objoff 5");
        m_botAction.sendUnfilteredPublicMessage("*objoff 6");
        m_botAction.sendUnfilteredPublicMessage("*objoff 7");
        m_botAction.sendUnfilteredPublicMessage("*objoff 8");
        m_botAction.sendUnfilteredPublicMessage("*objoff 9");
        // X //
        m_botAction.sendUnfilteredPublicMessage("*objoff 10");
        m_botAction.sendUnfilteredPublicMessage("*objoff 11");
        m_botAction.sendUnfilteredPublicMessage("*objoff 12");
        m_botAction.sendUnfilteredPublicMessage("*objoff 13");
        m_botAction.sendUnfilteredPublicMessage("*objoff 14");
        m_botAction.sendUnfilteredPublicMessage("*objoff 15");
        m_botAction.sendUnfilteredPublicMessage("*objoff 16");
        m_botAction.sendUnfilteredPublicMessage("*objoff 17");
        m_botAction.sendUnfilteredPublicMessage("*objoff 18");

        for (int i = 0; i > 10; ++i) {
            xx[i] = 0;
            oo[i] = 0;
        }

        players = new String[] { challenge.challer, challenge.challed };
        ba.cancelTask(challenge);
        challenge = null;
        playerTurn = 1;
        getTurn();
    }

    private void getTurn() {
        String[] msg = new String[] {
            "+---+---+---+",
            "| " + board[0][0].toString() + " | " + board[0][1].toString() + " | " + board[0][2].toString() + " |",
            "| " + board[1][0].toString() + " | " + board[1][1].toString() + " | " + board[1][2].toString() + " |",
            "| " + board[2][0].toString() + " | " + board[2][1].toString() + " | " + board[2][2].toString() + " |",
            "+---+---+---+",
        };
        ba.arenaMessageSpam(msg);
        playerTurn = (playerTurn + 1) % 2;
        ba.sendArenaMessage("" + players[playerTurn] + " (" + Token.get(playerTurn).toString() + "), pick a box! (!p <row>,<col>)", 1);
    }

    private Token getWinner() {
        Token result = null;

        if (board[0][0] != Token.blank) {
            if (board[0][0] == board[0][1] && board[0][1] == board[0][2]) {
                result = board[0][0];
                m_botAction.sendUnfilteredPublicMessage("*objon 90");
                // +---+---+---+
                // | x | x | x |
                // |   |   |   |
                // |   |   |   |
                // +---+---+---+
            }
            else if (board[0][0] == board[1][0] && board[1][0] == board[2][0]) {
                result = board[0][0];
                m_botAction.sendUnfilteredPublicMessage("*objon 91");
                // +---+---+---+
                // | x |   |   |
                // | x |   |   |
                // | x |   |   |
                // +---+---+---+
            }
            else if (board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
                result = board[0][0];
                m_botAction.sendUnfilteredPublicMessage("*objon 92");
                // +---+---+---+
                // | x |   |   |
                // |   | x |   |
                // |   |   | x |
                // +---+---+---+
            }

        }

        if (board[0][2] != Token.blank) {

            if (board[0][2] == board[1][2] && board[1][2] == board[2][2]) {
                result = board[0][2];
                m_botAction.sendUnfilteredPublicMessage("*objon 94");

                // +---+---+---+
                // |   |   | x |
                // |   |   | x |
                // |   |   | x |
                // +---+---+---+
            }
            else if (board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
                result = board[0][2];
                m_botAction.sendUnfilteredPublicMessage("*objon 95");
                // +---+---+---+
                // |   |   | x |
                // |   | x |   |
                // | x |   |   |
                // +---+---+---+
            }
        }

        if (board[2][2] != Token.blank) {
            if (board[2][2] == board[2][1] && board[2][1] == board[2][0]) {
                result = board[2][2];
                m_botAction.sendUnfilteredPublicMessage("*objon 97");
                // +---+---+---+
                // |   |   |   |
                // |   |   |   |
                // | x | x | x |
                // +---+---+---+
            }
        }

        if (board[1][1] != Token.blank) {

            if (board[1][1] == board[1][0] && board[1][0] == board[1][2]) {
                result = board[1][1];
                m_botAction.sendUnfilteredPublicMessage("*objon 96");
                // +---+---+---+
                // |   |   |   |
                // | x | x | x |
                // |   |   |   |
                // +---+---+---+
            }
            else if (board[1][1] == board[0][1] && board[0][1] == board[2][1]) {
                result = board[1][1];
                m_botAction.sendUnfilteredPublicMessage("*objon 93");
                // +---+---+---+
                // |   | x |   |
                // |   | x |   |
                // |   | x |   |
                // +---+---+---+
            }
        }

        if (result != null)
            return result;
        else {
            for (int x = 0; x < 3; x++)
                for (int y = 0; y < 3; y++)
                    if (board[x][y] == Token.blank)
                        return null;

            return Token.blank;
        }
    }

    private int getPlayer(String name) {
        if (players == null)
            return -1;
        else if (name.equalsIgnoreCase(players[0]))
            return 0;
        else if (name.equalsIgnoreCase(players[1]))
            return 1;
        else
            return -1;
    }

    private class Challenge extends TimerTask {

        String challer;
        String challed;

        public Challenge(String challenger, String challenged) {
            challer = challenger;
            challed = challenged;
            challenge = this;
            ba.scheduleTask(challenge, Tools.TimeInMillis.MINUTE);
            ba.sendPrivateMessage(challenger, "You have challenged '" + challenged + "' to TicTacToe. Challenge will expire in 1 minute");
            ba.sendPrivateMessage(challenged, "You have been challenged by '" + challenger + "' to TicTacToe. You have 1 minute to accept.");
        }

        public void run() {
            ba.sendSmartPrivateMessage(challer, "Your challenge has expired.");
            ba.sendSmartPrivateMessage(challed, "The challenge has expired.");
            challenge = null;
            ba.sendArenaMessage("Challenge request now available...");
        }

        public void accept(String name) {
            if (!name.equalsIgnoreCase(challed))
                ba.sendPrivateMessage(name, "You have not been challenged.");
            else
                newGame();
        }

        public void cancel(String name) {
            if (name.equalsIgnoreCase(challer) || oplist.isER(name)) {
                ba.cancelTask(challenge);
                challenge = null;
                ba.sendPrivateMessage(name, "Challenge canceled.");
                ba.sendArenaMessage("Challenge request now available...");
            }
        }
    }

    private class Die extends TimerTask {
        public Die(String name) {
            ba.sendSmartPrivateMessage(name, "Logging off...");
            ba.scheduleTask(this, 3000);
        }

        public void run() {
            ba.cancelTasks();
            ba.die("!die initiated");
        }
    }

}