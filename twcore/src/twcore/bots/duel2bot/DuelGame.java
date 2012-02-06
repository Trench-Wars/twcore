package twcore.bots.duel2bot;

import java.sql.SQLException;
import java.util.ArrayList;

import twcore.bots.duel2bot.DuelPlayer;
import twcore.bots.duel2bot.DuelTeam;
import twcore.bots.duel2bot.duel2bot;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.util.Tools;

/**
 * Manages and represents a 2v2 TWEL duel. Holds team and player objects
 * and any immediate relevant information as well as methods involved in
 * general duel management.
 *
 * @author WingZero
 */
public class DuelGame {

    BotAction           ba;
    BotSettings         rules;

    static final String db = "website";

    static int          d_noCount;
    static int          d_season;
    static int          d_deathTime;
    static int          d_spawnTime;
    static int          d_spawnLimit;
    static int          d_maxLagouts;
    static int          d_toWin;

    DuelTeam            team1;
    DuelTeam            team2;

    duel2bot            bot;
    DuelBox             box;

    int                 id;
    int                 div;
    int                 state;
    int[]               score;
    boolean             ranked;

    // constant game states
    static final int    SETUP       = 0;
    static final int    IN_PROGRESS = 1;
    static final int    ENDING      = 3;

    public DuelGame(DuelBox box, DuelChallenge chall, BotAction botaction, duel2bot bot) {
        this.box = box;
        div = chall.getDiv();
        ba = botaction;
        this.bot = bot;
        state = SETUP;
        d_season = bot.d_season;
        d_noCount = bot.d_noCount;
        d_deathTime = bot.d_deathTime;
        d_spawnTime = bot.d_spawnTime;
        d_spawnLimit = bot.d_spawnLimit;
        d_maxLagouts = bot.d_maxLagouts;
        ranked = chall.ranked;
        id = bot.getID(ranked);

        // Ax1,Ay1,safeAx1,safeAy1,Ax2,Ay2,safeAx2,safeAy2
        int[] coords1 = new int[] { box.getAX1(), box.getAY1(), box.getSafeAX1(), box.getSafeAY1(),
                box.getAX2(), box.getAY2(), box.getSafeAX2(), box.getSafeAY2() };
        int[] coords2 = new int[] { box.getBX1(), box.getBY1(), box.getSafeBX1(), box.getSafeBY1(),
                box.getBX2(), box.getBY2(), box.getSafeBX2(), box.getSafeBY2() };

        box.toggleUse();
        // since team wasn't given, add to main collection
        team1 = new DuelTeam(bot, chall.freq1(), chall.freq1(), chall.team1(), coords1, this);
        bot.teams.put(chall.freq1, team1);

        team2 = new DuelTeam(bot, chall.freq2(), chall.freq2(), chall.team2(), coords2, this);
        bot.teams.put(chall.freq2, team2);

        bot.games.put(id, this);
    }
    
    public DuelTeam[] getTeams(String name) {
        DuelPlayer p = team1.getPlayer(name);
        if (p != null)
            return new DuelTeam[] { team1, team2 };
        p = team2.getPlayer(name);
        if (p != null)
            return new DuelTeam[] { team2, team1 };
        return null;
    }

    /** Reports the current scores to each team freq */
    public void updateScore() {
        int t2 = team1.getDeaths();
        int t1 = team2.getDeaths();
        team1.setScore(t1);
        team2.setScore(t2);
        score = new int[] { t1, t2 };

        ba.sendOpposingTeamMessageByFrequency(team1.getFreq(), "Score: " + score[0] + "-"
                + score[1], 26);
        ba.sendOpposingTeamMessageByFrequency(team2.getFreq(), "Score: " + score[0] + "-"
                + score[1], 26);

        if (team1.out() || team2.out())
            endGame(t1, t2);
    }

    /** Returns game state */
    public int getState() {
        return state;
    }

    /** Returns a String describing the score of the duel */
    public String getScore() {
        String[] t1 = team1.getNames();
        String[] t2 = team2.getNames();
        return "" + score[0] + "-" + score[1] + " : " + t1[0] + " and " + t1[1] + " vs " + t2[0]
                + " and " + t2[1];
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
            statArray.add("| Challengers      / " + padNum(team1.getKills(), 3) + " |" + padNum(team1.getDeaths(), 3) + " |" + padNum(team1.getLagouts(), 3) + " |" + padNum(team1.getTime(), 9) + " |        |");
            statArray.add("+-----------------'      |    |    |          |        |");
            String[] stats = team1.getStatString(ranked);
            statArray.add(stats[0]);
            statArray.add(stats[1]);

            statArray.add("+------------------------+----+----+----------+--------+");
            statArray.add("|                   ,----+----+----+----------+--------+");
            statArray.add("| Accepters        / " + padNum(team2.getKills(), 3) + " |" + padNum(team2.getDeaths(), 3) + " |" + padNum(team2.getLagouts(), 3) + " |" + padNum(team2.getTime(), 9) + " |        |");
            statArray.add("+-----------------'      |    |    |          |        |");
            stats = team2.getStatString(ranked);
            statArray.add(stats[0]);
            statArray.add(stats[1]);
            statArray.add("`------------------------+----+----+----------+--------'");
        } else {
            statArray.add(",------------------------+----+----+----------.");
            statArray.add("|                      K |  D | LO | PlayTime |");
            statArray.add("|                   ,----+----+----+----------+");
            statArray.add("| Challengers      / " + padNum(team1.getKills(), 3) + " |" + padNum(team1.getDeaths(), 3) + " |" + padNum(team1.getLagouts(), 3) + " |" + padNum(team1.getTime(), 9) + " |");
            statArray.add("+-----------------'      |    |    |          |");
            String[] stats = team1.getStatString(ranked);
            statArray.add(stats[0]);
            statArray.add(stats[1]);

            statArray.add("+------------------------+----+----+----------+");
            statArray.add("|                   ,----+----+----+----------+");
            statArray.add("| Accepters        / " + padNum(team2.getKills(), 3) + " |" + padNum(team2.getDeaths(), 3) + " |" + padNum(team2.getLagouts(), 3) + " |" + padNum(team2.getTime(), 9) + " |");
            statArray.add("+-----------------'      |    |    |          |");
            stats = team2.getStatString(ranked);
            statArray.add(stats[0]);
            statArray.add(stats[1]);
            statArray.add("`------------------------+----+----+----------'");
            
        }
        return statArray.toArray(new String[statArray.size()]);
    }

    /** Starts the duel, updates the bot collections and sends announcements */
    public void startGame() {
        if (team1 == null || team2 == null) return;

        state = SETUP;
        String[] names1, names2;
        names1 = team1.getNames();
        bot.playing.put(names1[0].toLowerCase(), id);
        bot.playing.put(names1[1].toLowerCase(), id);
        names2 = team2.getNames();
        bot.playing.put(names2[0].toLowerCase(), id);
        bot.playing.put(names2[1].toLowerCase(), id);

        score = new int[] { 0, 0 };
        if (div == 5) {
            team1.startGame(true, team2.getNames());
            team2.startGame(true, team1.getNames());
        } else {
            team1.startGame(false, team2.getNames());
            team2.startGame(false, team1.getNames());
        }
        ba.sendTeamMessage("A " + (ranked ? "RANKED " : "CASUAL ") + getDivision() + " duel is starting: '" + names1[0] + "' and '"
                + names1[1] + "' VS '" + names2[0] + "' and '" + names2[1] + "'");
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
        String[] winner, loser;
        DuelPlayer[] win = null;
        DuelPlayer[] loss = null;
        int winnerScore, loserScore;
        if (t1 > t2) {
            winner = team1.getNames();
            loser = team2.getNames();
            win = team1.getPlayers();
            loss = team2.getPlayers();
            winnerScore = t1;
            loserScore = t2;
        } else {
            winner = team2.getNames();
            loser = team1.getNames();
            win = team2.getPlayers();
            loss = team1.getPlayers();
            winnerScore = t2;
            loserScore = t1;
        }
        win[0].endTimePlayed();
        win[1].endTimePlayed();
        loss[0].endTimePlayed();
        loss[1].endTimePlayed();
        int winRatings = win[0].getRating() + win[1].getRating();
        int lossRatings = loss[0].getRating() + loss[1].getRating();
        int diff = lossRatings - winRatings;
        
        double p1 = 1.0 / (1.0 + Math.pow(10, (-diff / 400.0)));
        double p2 = 1.0 - p1;
        
        // losers
        int drLoss = (int) Math.round((10 * 5 * (0.0 - p1)));
        int drWin =  (int) Math.round((10 * 5 * (1.0 - p2)));
        double kills[] = new double[] { loss[0].getKills() + 1, loss[1].getKills() + 1 };
        double deaths[] = new double[] { loss[0].getDeaths() + 1, loss[1].getDeaths() + 1 };
        double ratio[] = new double[3];
        ratio[0] = (kills[0]);
        ratio[1] = (kills[1]);
        ratio[2] = ratio[0] + ratio[1];
        ratio[0] = ratio[0] / ratio[2];
        ratio[1] = ratio[1] / ratio[2];
        
        int drLoser1 = (int) Math.round(drLoss * ratio[1]);
        int drLoser2 = (int) (drLoser1 - drLoss) * -1;
        bot.debug("[RATING] drLoss=" + drLoss + " (" + loser[0] + ") drLoser1=" + drLoser1 + " (" + loser[1] + ") drLoser2=" + drLoser2);
        loss[0].setRating(loss[0].getRating() + drLoser1);
        loss[1].setRating(loss[1].getRating() + drLoser2);
        
        // winners
        kills = new double[] { win[0].getKills() + 1, win[1].getKills() + 1 };
        deaths = new double[] { win[0].getDeaths() + 1, win[1].getDeaths() + 1 };
        ratio[0] = (kills[0] / deaths[0]);
        ratio[1] = (kills[1] / deaths[1]);
        ratio[2] = ratio[0] + ratio[1];
        ratio[0] = ratio[0] / ratio[2];
        ratio[1] = ratio[1] / ratio[2];
        
        int drWinner1 = (int) Math.round(drWin * ratio[0]);
        int drWinner2 = (int) drWin - drWinner1;
        bot.debug("[RATING] drWin=" + drWin + " (" + winner[0] + ") drWinner1=" + drWinner1 + " (" + winner[1] + ") drWinner2=" + drWinner2);
        win[0].setRating(win[0].getRating() + drWinner1);
        win[1].setRating(win[1].getRating() + drWinner2);

        ba.sendPrivateMessage(winner[0], "You and '" + winner[1] + "' have defeated '" + loser[0] + "' and '" + loser[1] + "' score: (" + winnerScore + "-" + loserScore + ")");
        ba.sendPrivateMessage(winner[1], "You and '" + winner[0] + "' have defeated '" + loser[0] + "' and '" + loser[1] + "' score: (" + winnerScore + "-" + loserScore + ")");
        ba.sendPrivateMessage(loser[0], "You and '" + loser[1] + "' have been defeated by '" + winner[0] + "' and '" + winner[1] + "' score: (" + loserScore + "-" + winnerScore + ")");
        ba.sendPrivateMessage(loser[1], "You and '" + loser[0] + "' have been defeated by '" + winner[0] + "' and '" + winner[1] + "' score: (" + loserScore + "-" + winnerScore + ")");
        ba.sendTeamMessage((ranked ? "[RANKED] " : "[CASUAL] ") + "'" + winner[0] + " and '" + winner[1] + "' defeat '" + loser[0] + "' and '" + loser[1] + "' in " + getDivision() + " score: (" + winnerScore + "-" + loserScore + ")", 21);

        String[] stats = getStats();
        team1.sendStats(stats);
        team2.sendStats(stats);
        
        if (ranked)
            sql_storeGame();
        
        t1 = team1.getTeamID();
        t2 = team2.getTeamID();
        
        team1.endGame();
        team2.endGame();

        // BACKTRACK
        bot.games.remove(id);

        String[] names;
        names = team1.getNames();
        bot.playing.remove(names[0].toLowerCase());
        bot.playing.remove(names[1].toLowerCase());

        names = team2.getNames();
        bot.playing.remove(names[0].toLowerCase());
        bot.playing.remove(names[1].toLowerCase());
        box.toggleUse();
    }

    /** Cancels the duel and notifies the name given */
    public void cancelDuel(String name) {
        String msg = "Duel canceled " + (name != null ? "by " + name : "") + " and is declared void.";
        String[] names;
        names = team2.getNames();
        ba.sendSmartPrivateMessage(names[0], msg);
        ba.sendSmartPrivateMessage(names[1], msg);
        names = team1.getNames();
        ba.sendSmartPrivateMessage(names[0], msg);
        ba.sendSmartPrivateMessage(names[1], msg);
        
        String[] stats = getStats();
        team1.sendStats(stats);
        team2.sendStats(stats);
        
        team1.endGame();
        team2.endGame();

        // BACKTRACK
        bot.games.remove(id);

        bot.playing.remove(names[0].toLowerCase());
        bot.playing.remove(names[1].toLowerCase());

        names = team2.getNames();
        bot.playing.remove(names[0].toLowerCase());
        bot.playing.remove(names[1].toLowerCase());
        if (name != null)
            ba.sendPrivateMessage(name, "Duel cancelled.");
        box.toggleUse();
    }

    /** Reports the lagout of a player to the opposing team */
    public void lagout(int id) {
        if (team1.getTeamID() == id)
            team2.opponentLagout();
        else
            team1.opponentLagout();
    }

    /** Reports the return of a lagger to the opposing team */
    public void returned(int id) {
        if (team1.getTeamID() == id)
            team2.opponentReturned();
        else
            team1.opponentReturned();
    }

    // handle player position
    // call Player to warp
    /** Reports a player removal and then updates scores */
    public void playerOut(DuelPlayer player) {
        bot.laggers.remove(player.getName().toLowerCase());
        int why = player.getReason();
        if (why == DuelPlayer.NORMAL) {
            ba.sendOpposingTeamMessageByFrequency(team1.getFreq(), "'" + player.getName()
                    + "' is out with " + player.getKills() + ":" + player.getDeaths(), 26);
            ba.sendOpposingTeamMessageByFrequency(team2.getFreq(), "'" + player.getName()
                    + "' is out with " + player.getKills() + ":" + player.getDeaths(), 26);
        } else if (why == DuelPlayer.WARPS) {
            ba.sendOpposingTeamMessageByFrequency(team1.getFreq(), "'" + player.getName()
                    + "' is out due to warp abuse (" + player.getKills() + ":" + player.getDeaths() + ")", 26);
            ba.sendOpposingTeamMessageByFrequency(team2.getFreq(), "'" + player.getName()
                    + "' is out due to warp abuse (" + player.getKills() + ":" + player.getDeaths() + ")", 26);
        } else if (why == DuelPlayer.LAGOUTS) {
            ba.sendOpposingTeamMessageByFrequency(team1.getFreq(), "'" + player.getName()
                    + "' is out due to lagouts (" + player.getKills() + ":" + player.getDeaths() + ")", 26);
            ba.sendOpposingTeamMessageByFrequency(team2.getFreq(), "'" + player.getName()
                    + "' is out due to lagouts (" + player.getKills() + ":" + player.getDeaths() + ")", 26);
        } else if (why == DuelPlayer.SPAWNS) {
            ba.sendOpposingTeamMessageByFrequency(team1.getFreq(), "'" + player.getName()
                    + "' is out due to spawn kill abuse (" + player.getKills() + ":" + player.getDeaths() + ")", 26);
            ba.sendOpposingTeamMessageByFrequency(team2.getFreq(), "'" + player.getName()
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
    
    /** Records the duel results in the database */
    public void sql_storeGame() {
        //TODO:
        String query = "INSERT INTO tblDuel2__match (fnDivision, fnTeam1, fnTeam2, fnScore1, fnScore2) VALUES(" +
                    "" + div + ", ";
        int t1, t2;
        if (score[0] > score[1]) {
            t1 = team1.sql_storeTeam(true);
            t2 = team2.sql_storeTeam(false);
        } else if (score[0] < score[1]) {
            t1 = team1.sql_storeTeam(false);
            t2 = team2.sql_storeTeam(true);
        } else {
            bot.debug("[TEAMDUEL] Tied duel encountered.");
            Tools.printLog("[TEAMDUEL] Tied duel encountered.");
            return;
        }
            
        if (t1 < 0 || t2 < 0) return;
        query += t1 + ", " + t2 + ", " + score[0] + ", " + score[1] + ")";
        try {
            ba.SQLQueryAndClose(db, query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
