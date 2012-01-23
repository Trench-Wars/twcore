package twcore.bots.duel1bot;

import java.util.ArrayList;

import twcore.core.BotAction;
import twcore.core.BotSettings;

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

    duel1bot            bot;
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

    public DuelGame(DuelBox box, DuelChallenge chall, BotAction botaction, duel1bot bot) {
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
        //ranked = chall.ranked;
        id = bot.getID(ranked);

        // Ax1,Ay1,safeAx1,safeAy1,Ax2,Ay2,safeAx2,safeAy2
        int[] coords1 = new int[] { box.getAX(), box.getAY(), box.getSafeAX(), box.getSafeAY() };
        int[] coords2 = new int[] { box.getBX(), box.getBY(), box.getSafeBX(), box.getSafeBY() };

        bot.games.put(id, this);
    }

    /** Reports the current scores to each team freq */
    public void updateScore() {
    	
    }

    /** Returns game state */
    public int getState() {
        return state;
    }

    /** Returns a String describing the score of the duel */
    public String getScore() {
    	return null;
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
        return statArray.toArray(new String[statArray.size()]);
    }

    /** Starts the duel, updates the bot collections and sends announcements */
    public void startGame() {
    	
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
        
    }

    /** Cancels the duel and notifies the name given */
    public void cancelDuel(String name) {
    	
    }

    /** Reports the lagout of a player to the opposing team */
    public void lagout(int id) {
    	
    }

    /** Reports the return of a lagger to the opposing team */
    public void returned(int id) {
    	
    }

    // handle player position
    // call Player to warp
    /** Reports a player removal and then updates scores */
    public void playerOut(DuelPlayer player) {
        int why = player.getReason();
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
    }
}
