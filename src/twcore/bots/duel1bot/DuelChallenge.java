package twcore.bots.duel1bot;

import java.util.TimerTask;

import twcore.core.BotAction;

public class DuelChallenge extends TimerTask {
    int       freq1, freq2, div;
    String  name1, name2;
    duel1bot  bot;
    BotAction ba;
    boolean   ranked;

    public DuelChallenge(duel1bot bot, BotAction action, boolean ranked, int f1, int f2, String n1, String n2, int type) {
        freq1 = f1;
        freq2 = f2;
        name1 = n1;
        name2 = n2;
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

    public String[] getPlayers() {
        return new String[] { name1, name2 };
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
                                                  + " challenge to " + name1 + " and " + name2 + " has expired.", 26);
    }

}
