package twcore.bots.duel2bot;

import java.util.TimerTask;

import twcore.core.BotAction;

class DuelChallenge extends TimerTask {
    int       freq1, freq2, div;
    String[]  team1, team2;
    duel2bot  bot;
    BotAction ba;
    boolean   ranked;

    public DuelChallenge(duel2bot bot, BotAction action, boolean ranked, int f1, int f2,
            String[] n1, String[] n2, int type) {
        freq1 = f1;
        freq2 = f2;
        team1 = n1;
        team2 = n2;
        div = type;
        this.bot = bot;
        this.ranked = ranked;
        ba = action;
    }

    public void setDiv(int d) {
        div = d;
    }

    public int freq1() {
        return freq1;
    }

    public int freq2() {
        return freq2;
    }

    public String[] team1() {
        return team1;
    }

    public String[] team2() {
        return team2;
    }

    public int getDiv() {
        return div;
    }

    public boolean getRanked() {
        return ranked;
    }

    @Override
    public void run() {
        // expire challenge
        DuelChallenge chall = bot.challs.remove("" + freq1 + " " + freq2 + "");
        if (chall != null)
            ba.sendOpposingTeamMessageByFrequency(chall.freq1(), "Your " + bot.getDivision(div)
                    + " challenge to " + team2[0] + " and " + team2[1] + " has expired.", 26);
    }
}