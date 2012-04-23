package twcore.bots.duel1bot;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.util.Tools;

public class DuelGame {

    BotAction           ba;
    BotSettings         rules;

    static final String db = "website";

    static int          d_season;
    static int          d_deathTime;
    static int          d_spawnTime;
    static int          d_spawnLimit;
    static int          d_maxLagouts;
    int                 d_noCount;
    int                 d_deathWarp;
    int                 d_toWin;

    duel1bot            bot;
    DuelBox             box;
    
    DuelPlayer          player1;
    DuelPlayer          player2;

    int                 id;
    int                 div;
    int                 ship;
    int                 state;
    int[]               score;
    boolean             ranked;
    boolean             mixed;

    // constant game states
    static final int    SETUP       = 0;
    static final int    IN_PROGRESS = 1;
    static final int    ENDING      = 3;
    
    TimerTask           go;

    public DuelGame(DuelBox box, DuelChallenge chall, BotAction botaction, duel1bot bot) {
        this.box = box;
        div = chall.getDiv();
        ba = botaction;
        this.bot = bot;
        state = SETUP;
        d_season = bot.d_season;
        d_noCount = bot.d_noCount;
        d_deathWarp = 1;
        d_toWin = 10;
        d_deathTime = bot.d_deathTime;
        d_spawnTime = bot.d_spawnTime;
        d_spawnLimit = bot.d_spawnLimit;
        d_maxLagouts = bot.d_maxLagouts;
        ranked = chall.ranked;
        id = bot.getID(ranked);
        mixed = false;
        if (div == 5) {
            ship = -1;
            mixed = true;
        } else if (div == 4)
            ship = 7;
        else
            ship = div;
        // Ax1,Ay1,safeAx1,safeAy1,Ax2,Ay2,safeAx2,safeAy2
        int[] coords1 = new int[] { box.getAX(), box.getAY(), box.getSafeAX(), box.getSafeAY() };
        int[] coords2 = new int[] { box.getBX(), box.getBY(), box.getSafeBX(), box.getSafeBY() };

        player1 = bot.getPlayer(chall.name1);
        player2 = bot.getPlayer(chall.name2);
        if (player1 == null)
            player1 = new DuelPlayer(ba.getPlayer(chall.name1), bot);
        if (player2 == null)
            player2 = new DuelPlayer(ba.getPlayer(chall.name2), bot);
        if (ranked) {
            d_noCount = player1.d_noCount;
            d_deathWarp = player1.d_deathWarp;
            d_toWin = player1.d_toWin;
        }
        player1.setDuel(this, chall.freq1, coords1);
        player2.setDuel(this, chall.freq2, coords2);
        bot.games.put(id, this);
    }

    /** Reports the current scores to each team freq */
    public void updateScore() {
        int t2 = player1.getDeaths();
        int t1 = player2.getDeaths();
        score = new int[] { t1, t2 };

        ba.sendPrivateMessage(player1.name, "Score: " + score[0] + "-" + score[1]);
        ba.sendPrivateMessage(player2.name, "Score: " + score[0] + "-" + score[1]);

        if (player1.isOut() || player2.isOut())
            endGame(t1, t2);
    }
    
    public DuelPlayer[] getPlayers(String name) {
        if (name.equalsIgnoreCase(player1.name))
            return new DuelPlayer[] { player1, player2 };
        else
            return new DuelPlayer[] { player2, player1 };
    }

    /** Returns game state */
    public int getState() {
        return state;
    }

    /** Returns a String describing the score of the duel */
    public String getScore() {
        return "" + score[0] + "-" + score[1] + " : " + player1.name + " vs " + player2.name;
    }

    /** Converts the division ID into a String */
    public String getDivision() {
        if (div == 1)
            return "Warbird";
        else if (div == 2)
            return "Javelin";
        else if (div == 3)
            return "Spider";
        else if (div == 4 || div == 7)
            return "Lancaster";
        else if (div == 5)
            return "Mixed";
        else
            return "Unknown";
    }
    
    public String[] getStats() {
        ArrayList<String> statArray = new ArrayList<String>();
        if (ranked) {
            statArray.add(",------------------------+----+----+----------+--------.");
            statArray.add("|                      K |  D | LO | PlayTime | Rating |");
            statArray.add("|                   ,----+----+----+----------+--------+");
            statArray.add("| " + padString(player1.name, 17) + "/ " + padNum(player1.getKills(), 3) + " |" + padNum(player1.getDeaths(), 3) + " |" + padNum(player1.getLagouts(), 3) + " |" + padNum(player1.getTime(), 9) + " |        |");
            statArray.add("+-----------------'-,----+----+----+----------+--------+");
            statArray.add("| " + padString(player2.name, 17) + "/ " + padNum(player2.getKills(), 3) + " |" + padNum(player2.getDeaths(), 3) + " |" + padNum(player2.getLagouts(), 3) + " |" + padNum(player2.getTime(), 9) + " |");
            statArray.add("`-----------------'------+----+----+----------+--------'");
        } else {
            statArray.add(",------------------------+----+----+----------.");
            statArray.add("|                      K |  D | LO | PlayTime |");
            statArray.add("|                   ,----+----+----+----------+");
            statArray.add("| " + padString(player1.name, 17) + "/ " + padNum(player1.getKills(), 3) + " |" + padNum(player1.getDeaths(), 3) + " |" + padNum(player1.getLagouts(), 3) + " |" + padNum(player1.getTime(), 9) + " |");
            statArray.add("+-----------------'-,----+----+----+----------+");
            statArray.add("| " + padString(player2.name, 17) + "/ " + padNum(player2.getKills(), 3) + " |" + padNum(player2.getDeaths(), 3) + " |" + padNum(player2.getLagouts(), 3) + " |" + padNum(player2.getTime(), 9) + " |");
            statArray.add("`-----------------'------+----+----+----------'");
            
        }
        return statArray.toArray(new String[statArray.size()]);
    }
    
    public boolean getNoCount() {
        return d_noCount == 1;
    }
    
    public boolean getDeathWarp() {
        return d_deathWarp == 1;
    }
    
    public int getSpecAt() {
        return d_toWin;
    }

    /** Starts the duel, updates the bot collections and sends announcements */
    public void startGame() {
        if (player1 == null || player2 == null) return;

        state = SETUP;
        bot.playing.put(player1.name.toLowerCase(), id);
        bot.playing.put(player2.name.toLowerCase(), id);

        score = new int[] { 0, 0 };

        int divis = div;
        if (!ranked)
            divis = -1;
        player1.starting(divis, ship);
        player2.starting(divis, ship);
        go = new TimerTask() {
            @Override
            public void run() {
                state = DuelGame.IN_PROGRESS;
                player1.startGame();
                player2.startGame();
            }
        };
        String r = ranked ? "[RANKED] " : "[CASUAL] ";
        if (mixed) {
            ba.sendPrivateMessage(player1.name, r + "Duel Begins in 30 Seconds Against '" + player2.name + "'", 29);
            ba.sendPrivateMessage(player1.name, "You may change your ship until that time! No ship changes will be allowed after the duel starts.");
            ba.sendPrivateMessage(player2.name, r + "Duel Begins in 30 Seconds Against '" + player1.name + "'", 29);
            ba.sendPrivateMessage(player2.name, "You may change your ship until that time! No ship changes will be allowed after the duel starts.");
            ba.scheduleTask(go, 30000);
        } else {
            ba.sendPrivateMessage(player1.name, r + "Duel Begins in 15 Seconds Against '" + player2.name + "'", 27);
            ba.sendPrivateMessage(player2.name, r + "Duel Begins in 15 Seconds Against '" + player1.name + "'", 27);
            ba.scheduleTask(go, 15000);
        }
        ba.sendTeamMessage("A " + (ranked ? "RANKED " : "CASUAL ") + getDivision() + " duel is starting: '" + player1.name + "' VS '" + player2.name + "'");
    }

    /**
     * Ends the duel by sending announcements and initiating post-game
     * cleanup and database updating.
     *
     * @param t1
     *      the final score for team 1
     * @param t2
     *      the final score for team 2
     */
    public void endGame(int t1, int t2) {
        state = ENDING;
        score[0] = t1;
        score[1] = t2;
        String winner, loser;
        DuelPlayer win = null;
        DuelPlayer loss = null;
        int winnerScore, loserScore;
        if (t1 > t2) {
            winner = player1.name;
            loser = player2.name;
            win = player1;
            loss = player2;
            winnerScore = t1;
            loserScore = t2;
        } else {
            winner = player2.name;
            loser = player1.name;
            win = player2;
            loss = player1;
            winnerScore = t2;
            loserScore = t1;
        }
        win.endTimePlayed();
        loss.endTimePlayed();

        //Calculate new ratings.

        int loserRatingBefore = loss.getRating();
        int winnerRatingBefore = win.getRating();
        int ratingDifference = loserRatingBefore - winnerRatingBefore;
        double p1 = 1.0 / ( 1 + Math.pow( 10.0, -ratingDifference / 400.0 ) );
        double p2 = 1.0 - p1;
        int loserRatingAfter = (int)(loserRatingBefore + d_toWin*5.0*(0.0 - p1 ));
        int winnerRatingAfter = (int)(winnerRatingBefore + d_toWin*5.0*(1.0 - p2 ));

        ba.sendPrivateMessage(winner, "You have defeated '" + loser + "' score: (" + winnerScore + "-" + loserScore + ")");
        ba.sendPrivateMessage(loser, "You have been defeated by '" + winner + "' score: (" + loserScore + "-" + winnerScore + ")");
        ba.sendTeamMessage((ranked ? "[RANKED] " : "[CASUAL] ") + "'" + winner + " defeats '" + loser + "' in " + getDivision() + " score: (" + winnerScore + "-" + loserScore + ")", 21);

        String[] stats = getStats();
        ba.privateMessageSpam(player1.name, stats);
        ba.privateMessageSpam(player2.name, stats);
        
        if (ranked)
            sql_storeGame(win, loss, winnerRatingBefore, winnerRatingAfter, loserRatingBefore, loserRatingAfter);
        
        player1.endGame();
        player2.endGame();

        // BACKTRACK
        bot.games.remove(id);

        bot.playing.remove(player1.name.toLowerCase());
        bot.playing.remove(player2.name.toLowerCase());
        
        box.toggleUse();
    }

    /** Cancels the duel and notifies the name given */
    public void cancelDuel(String name) {
        String msg = "Duel canceled " + (name != null ? "by " + name : "") + " and is declared void.";

        ba.sendSmartPrivateMessage(player1.name, msg);
        ba.sendSmartPrivateMessage(player2.name, msg);
        
        String[] stats = getStats();
        ba.privateMessageSpam(player1.name, stats);
        ba.privateMessageSpam(player2.name, stats);

        player1.endGame();
        player2.endGame();

        // BACKTRACK
        bot.games.remove(id);

        bot.playing.remove(player1.name.toLowerCase());
        bot.playing.remove(player2.name.toLowerCase());

        if (name != null)
            ba.sendPrivateMessage(name, "Duel cancelled.");
        box.toggleUse();
    }

    // handle player position
    // call Player to warp
    /** Reports a player removal and then updates scores */
    public void playerOut(DuelPlayer player) {
        bot.laggers.remove(player.getName().toLowerCase());
        int why = player.getReason();
        if (why == DuelPlayer.NORMAL) {
            ba.sendOpposingTeamMessageByFrequency(player1.duelFreq, "'" + player.getName()
                    + "' is out with " + player.getKills() + ":" + player.getDeaths(), 26);
            ba.sendOpposingTeamMessageByFrequency(player2.duelFreq, "'" + player.getName()
                    + "' is out with " + player.getKills() + ":" + player.getDeaths(), 26);
        } else if (why == DuelPlayer.WARPS) {
            ba.sendOpposingTeamMessageByFrequency(player1.duelFreq, "'" + player.getName()
                    + "' is out due to warp abuse (" + player.getKills() + ":" + player.getDeaths() + ")", 26);
            ba.sendOpposingTeamMessageByFrequency(player2.duelFreq, "'" + player.getName()
                    + "' is out due to warp abuse (" + player.getKills() + ":" + player.getDeaths() + ")", 26);
        } else if (why == DuelPlayer.LAGOUTS) {
            ba.sendOpposingTeamMessageByFrequency(player1.duelFreq, "'" + player.getName()
                    + "' is out due to lagouts (" + player.getKills() + ":" + player.getDeaths() + ")", 26);
            ba.sendOpposingTeamMessageByFrequency(player2.duelFreq, "'" + player.getName()
                    + "' is out due to lagouts (" + player.getKills() + ":" + player.getDeaths() + ")", 26);
        } else if (why == DuelPlayer.SPAWNS) {
            ba.sendOpposingTeamMessageByFrequency(player1.duelFreq, "'" + player.getName()
                    + "' is out due to spawn kill abuse (" + player.getKills() + ":" + player.getDeaths() + ")", 26);
            ba.sendOpposingTeamMessageByFrequency(player2.duelFreq, "'" + player.getName()
                    + "' is out due to spawn kill abuse (" + player.getKills() + ":" + player.getDeaths() + ")", 26);
        }
        updateScore();
    }
    
    /**
     * Helper method adds spaces in front of a number to fit a certain length
     * @param n
     * @param length
     * @return String of length with spaces preceeding a number
     */
    public String padNum(int n, int length) {
        String str = "";
        String x = "" + n;
        for (int i = 0; i + x.length() < length; i++)
            str += " ";
        return str + x;
    }
    
    /** Helper method adds spaces to a string to meet a certain length **/
    private String padString(String str, int length) {
        for (int i = str.length(); i < length; i++)
            str += " ";
        return str.substring(0, length);
    }
    
    /** Records the duel results in the database */
    public void sql_storeGame(DuelPlayer win, DuelPlayer loss, int winBefore, int winAfter, int lossBefore, int lossAfter) {
        //TODO:
        String query = "INSERT INTO tblDuel1__match (fnSeason, fnDivision, fcWiner, fcLoser, fnWinnerID, fnLoserID, fnWinnerScore, fnLoserScore, fnWinnerRatingBefore, fnWinnerRatingAfter, fnLoserRatingBefore, fnLoserRatingAfter) " +
        		"VALUES(" + d_season + ", " + div + ", " + Tools.addSlashesToString(win.name) + ", " + Tools.addSlashesToString(loss.name) + ", " +
                "" + win.userID + ", " + loss.userID + ", " + loss.getDeaths() + ", " + win.getDeaths() + ", " + 
        		"" + winBefore + ", " + winAfter + ", " + lossBefore + ", " + lossAfter + ")"; 
        		
        win.sql_updateDivision(div, true, loss.getKills() == 0);
        loss.sql_updateDivision(div, false, loss.getKills() == 0);

        try {
            ba.SQLQueryAndClose(db, query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
