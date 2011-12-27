package twcore.bots.duel2bot;

import java.sql.SQLException;

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

        // since team wasn't given, add to main collection
        team1 = new DuelTeam(bot, chall.freq1(), chall.freq1(), chall.team1(), coords1, this);
        bot.teams.put(team1.getTeamID(), team1);

        team2 = new DuelTeam(bot, chall.freq2(), chall.freq2(), chall.team2(), coords2, this);
        bot.teams.put(team2.getTeamID(), team2);

        bot.games.put(id, this);
    }

    /** Reports the current scores to each team freq */
    public void updateScore() {
        int t2 = team1.getDeaths();
        int t1 = team2.getDeaths();
        team1.setScore(t1);
        team2.setScore(t2);
        score = new int[] { t1, t2 };

        if (team1.out() || team2.out()) {
            endGame(t1, t2);
            return;
        }

        ba.sendOpposingTeamMessageByFrequency(team1.getFreq(), "Score: " + score[0] + "-"
                + score[1], 26);
        ba.sendOpposingTeamMessageByFrequency(team2.getFreq(), "Score: " + score[0] + "-"
                + score[1], 26);
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
        int winnerScore, loserScore;
        if (t1 > t2) {
            winner = team1.getNames();
            loser = team2.getNames();
            winnerScore = t1;
            loserScore = t2;
        } else {
            winner = team2.getNames();
            loser = team1.getNames();
            winnerScore = t2;
            loserScore = t1;
        }

        ba.sendPrivateMessage(winner[0], "You and '" + winner[1] + "' have defeated '" + loser[0]
                + "' and '" + loser[1] + "' score: (" + winnerScore + "-" + loserScore + ")");
        ba.sendPrivateMessage(winner[1], "You and '" + winner[0] + "' have defeated '" + loser[0]
                + "' and '" + loser[1] + "' score: (" + winnerScore + "-" + loserScore + ")");
        ba.sendPrivateMessage(loser[0], "You and '" + loser[1] + "' have been defeated by '"
                + winner[0] + "' and '" + winner[1] + "' score: (" + loserScore + "-" + winnerScore
                + ")");
        ba.sendPrivateMessage(loser[1], "You and '" + loser[0] + "' have been defeated by '"
                + winner[0] + "' and '" + winner[1] + "' score: (" + loserScore + "-" + winnerScore
                + ")");
        ba.sendTeamMessage(ranked ? "[RANKED] " : "[CASUAL] " + "'" + winner[0] + " and '"
                + winner[1] + "' defeat '" + loser[0] + "' and '" + loser[1] + "' in "
                + getDivision() + " score: (" + winnerScore + "-" + loserScore + ")", 21);
        
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
        
        if (ranked)
            sql_storeGame();
    }

    /** Cancels the duel and notifies the name given */
    public void cancelGame(String name) {
        team1.endGame();
        team2.endGame();
        bot.games.remove(id);
        ba.sendPrivateMessage(name, "Game cancelled.");
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

    // handle player position
    // call Player to warp
    /** Reports a player removal and then updates scores */
    public void playerOut(DuelPlayer player) {
        int why = player.getReason();
        if (why == DuelPlayer.NORMAL) {
            ba.sendOpposingTeamMessageByFrequency(team1.getFreq(), "'" + player.getName()
                    + "' is out with " + player.getKills() + ":" + player.getDeaths(), 26);
            ba.sendOpposingTeamMessageByFrequency(team2.getFreq(), "'" + player.getName()
                    + "' is out with " + player.getKills() + ":" + player.getDeaths(), 26);
        } else if (why == DuelPlayer.WARPS) {
            ba.sendOpposingTeamMessageByFrequency(team1.getFreq(), "'" + player.getName()
                    + "' is out due to warp abuse", 26);
            ba.sendOpposingTeamMessageByFrequency(team2.getFreq(), "'" + player.getName()
                    + "' is out due to warp abuse", 26);
        } else if (why == DuelPlayer.LAGOUTS) {
            ba.sendOpposingTeamMessageByFrequency(team1.getFreq(), "'" + player.getName()
                    + "' is out due to lagouts", 26);
            ba.sendOpposingTeamMessageByFrequency(team2.getFreq(), "'" + player.getName()
                    + "' is out due to lagouts", 26);
        } else if (why == DuelPlayer.SPAWNS) {
            ba.sendOpposingTeamMessageByFrequency(team1.getFreq(), "'" + player.getName()
                    + "' is out due to spawn kill abuse", 26);
            ba.sendOpposingTeamMessageByFrequency(team2.getFreq(), "'" + player.getName()
                    + "' is out due to spawn kill abuse", 26);
        }
        updateScore();
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
