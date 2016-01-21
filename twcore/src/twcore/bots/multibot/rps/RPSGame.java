package twcore.bots.multibot.rps;

import twcore.core.BotAction;

public class RPSGame
{
    public static final String[] weapons = {"rock", "paper", "scissors"};

    private BotAction m_botAction;
    private RPSPlayer player1;
    private RPSPlayer player2;

    private int round;
    private int winsRequired;
    private boolean started;

    public RPSGame(BotAction botAction)
    {
        m_botAction = botAction;
    }

    public boolean isStarted()
    {
        return started;
    }

    public void nextRound()
    {
        round++;
        player1.reset();
        player2.reset();
        m_botAction.sendArenaMessage("Round " + round + ".  FIGHT!");
        m_botAction.sendSmartPrivateMessage(player1.getPlayerName(), "Please private message me with your weapon (Rock, Paper or Scissors).");
        m_botAction.sendSmartPrivateMessage(player2.getPlayerName(), "Please private message me with your weapon (Rock, Paper or Scissors).");
    }

    public void doStart(String player1Name, String player2Name, int winsRequired)
    {
        if(winsRequired < 1)
            throw new IllegalArgumentException("Invalid number of wins required.");

        player1 = new RPSPlayer(player1Name);
        player2 = new RPSPlayer(player2Name);
        this.winsRequired = winsRequired;
        round = 0;
        started = true;
        m_botAction.sendArenaMessage("A game of Rock Paper Scissors to " + winsRequired + " is starting between " + player1Name + " and " + player2Name + ".", 20);
        nextRound();
    }

    public void doScoreCmd(String sender)
    {
        m_botAction.sendSmartPrivateMessage(sender,
                                            player1.getPlayerName() + ": " + player1.getScore() + " - " +
                                            player2.getPlayerName() + ": " + player2.getScore());
    }

    public void getWeapon(RPSPlayer player, String weaponString)
    {
        String currentWeapon;

        for(int index = 0; index < weapons.length; index++)
        {
            currentWeapon = weapons[index];

            if(currentWeapon.startsWith(weaponString))
            {
                player.setWeapon(index);
                m_botAction.sendSmartPrivateMessage(player.getPlayerName(), "Current Weapon: " + currentWeapon);
            }
        }
    }

    public void doDraw(int weapon)
    {
        String weaponName = weapons[weapon];
        m_botAction.sendArenaMessage("The round is a draw.  Both players chose " + weaponName + ".", 22);
        player1.reset();
        player2.reset();
    }

    public void doFinishGame(String winnerName)
    {
        m_botAction.sendArenaMessage(winnerName + " wins this match of Rock Paper Scissors!", 5);
        started = false;
    }

    public void doResults(RPSPlayer winner, RPSPlayer loser)
    {
        String winnerName = winner.getPlayerName();
        String loserName = loser.getPlayerName();
        String winnerWeapon = weapons[winner.getWeapon()];
        String loserWeapon = weapons[loser.getWeapon()];

        m_botAction.sendArenaMessage(winnerName + " wins this round.  " + winnerName + ": " + winnerWeapon + " - " + loserName + ": " + loserWeapon, 19);
        winner.doWin();

        if(winner.getScore() >= winsRequired)
            doFinishGame(winnerName);
    }

    public void checkWinner()
    {
        int weapon1 = player1.getWeapon();
        int weapon2 = player2.getWeapon();

        if(weapon1 != -1 && weapon2 != -1)
        {
            if(weapon1 == weapon2)
                doDraw(weapon1);
            else if((weapon1 == RPSPlayer.ROCK && weapon2 == RPSPlayer.SCISSORS) ||
                    (weapon1 == RPSPlayer.SCISSORS && weapon2 == RPSPlayer.PAPER) ||
                    (weapon1 == RPSPlayer.PAPER && weapon2 == RPSPlayer.ROCK))
                doResults(player1, player2);
            else
                doResults(player2, player1);

            if(started)
            {
                m_botAction.sendArenaMessage("Current Score: " + player1.getPlayerName() + ": " + player1.getScore() + " - " + player2.getPlayerName() + ": " + player2.getScore());
                nextRound();
            }
        }
    }

    public void doSexCmd(String sender)
    {
        m_botAction.sendSmartPrivateMessage(sender, "UNF!", 12);
    }

    public void doHelpCmd(String sender)
    {
        String[] message =
        {
            "Please private message me with either rock, paper or scissors to choose your weapon.",
            "Rules: Paper beats Rock",
            "       Rock beats Scissors",
            "       Scissors beats Paper"
        };

        m_botAction.privateMessageSpam(sender, message);
    }

    public void handleCommand(String sender, String message)
    {
        String command = message.toLowerCase();
        RPSPlayer player = getPlayer(sender);

        if(player != null)
        {
            getWeapon(player, command);

            if(command.equals("!help"))
                doHelpCmd(sender);

            checkWinner();
        }

        if(command.equals("!score"))
            doScoreCmd(sender);

        if(command.equals("!sex"))
            doSexCmd(sender);
    }

    public void doKillGame()
    {
        m_botAction.sendArenaMessage("This game of Rock Paper Scissors has been killed.", 22);
        started = false;
    }

    private RPSPlayer getPlayer(String playerName)
    {
        if(playerName.equals(player1.getPlayerName()))
            return player1;

        if(playerName.equals(player2.getPlayerName()))
            return player2;

        return null;
    }

    private class RPSPlayer
    {
        public static final int NONE = -1;
        public static final int ROCK = 0;
        public static final int PAPER = 1;
        public static final int SCISSORS = 2;

        private String playerName;
        private int score;
        private int weapon;

        public RPSPlayer(String playerName)
        {
            this.playerName = playerName;
            score = 0;
            weapon = NONE;
        }

        public String getPlayerName()
        {
            return playerName;
        }

        public int getScore()
        {
            return score;
        }

        public int getWeapon()
        {
            return weapon;
        }

        public void reset()
        {
            weapon = NONE;
        }

        public void doWin()
        {
            score++;
        }

        public void setWeapon(int weapon)
        {
            if(weapon < -1 || weapon > 2)
                throw new IllegalArgumentException("Illegal Weapon.");

            this.weapon = weapon;
        }
    }
}