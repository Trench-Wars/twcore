package twcore.bots.twdt;

import java.text.DecimalFormat;
import java.util.EnumMap;

import twcore.bots.twdt.StatType;

/**
 *
 * @author WingZero
 */
public class DraftStats {

    private EnumMap<StatType, DraftStat> stats;
    private int[] eKills;
    private int[] tKills;
    private int ship;

    public DraftStats(int ship) {
        eKills = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
        tKills = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
        stats = new EnumMap<StatType, DraftStat>(StatType.class);
        for (StatType stat : StatType.values())
            stats.put(stat, new DraftStat(stat));
    }
    
    public int getShip() {
        return ship;
    }
    
    public DraftStat getStat(StatType stat) {
        return stats.get(stat);
    }
    
    public void setStat(StatType stat, int value) {
        stats.get(stat).setValue(value);
    }
    
    public void handleKill(int points, int kill) {
        stats.get(StatType.KILLS).increment();
        stats.get(StatType.KILL_STREAK).increment();
        stats.get(StatType.SCORE).add(points);
        if (stats.get(StatType.KILL_STREAK).getValue() > stats.get(StatType.BEST_KILL_STREAK).getValue())
            stats.get(StatType.BEST_KILL_STREAK).setValue(stats.get(StatType.KILL_STREAK).getValue());
        stats.get(StatType.DEATH_STREAK).setValue(0);
        eKills[kill-1]++;
    }
    
    public void handleTeamKill(int points, int kill) {
        stats.get(StatType.TEAM_KILLS).increment();
        stats.get(StatType.SCORE).add(points);
        tKills[kill-1]++;
    }
    
    public void handleDeath() {
        stats.get(StatType.DEATHS).increment();
        stats.get(StatType.DEATH_STREAK).increment();
        if (stats.get(StatType.DEATH_STREAK).getValue() > stats.get(StatType.WORST_DEATH_STREAK).getValue())
            stats.get(StatType.WORST_DEATH_STREAK).setValue(stats.get(StatType.DEATH_STREAK).getValue());
        stats.get(StatType.KILL_STREAK).setValue(0);
    }

    public int getScore() {
        return getStat(StatType.SCORE).getValue();
    }
    
    public double getRPD() {
        int reps = getStat(StatType.REPELS).getValue() / 2;
        int deaths = getStat(StatType.DEATHS).getValue();
        if (reps > 0 && deaths > 0)
            return (reps / deaths);
        else if (reps > 0 && deaths == 0)
            return (reps);
        else
            return 0;
    }
    
    public String getRPDString() {
        double rpd = getRPD();
        if (rpd == 0)
            return "   ";
        return (new DecimalFormat("0.0").format(rpd));
    }

    /**
     * Method getRating.
     * This returns the rating for the player according to this:
     *
     * warbird: .45Points * (.07wb + .07jav + .05spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
     * jav: .6Points * (.05wb + .06jav + .066spid + 0.14terr + .07x + .05lanc + .09shark - .05deaths - (.07wbTK + .07javTK + .06spiderTK + .13terrTK + .06WeaselTK + .07LancTK + .09SharkTK))
     * spiders: .4points * (.06wb + .06jav + .04spid + .09terr + .05x + .05lanc + .089shark - .05deaths)
     * terr: 2.45points * (.03wb + .03jav + .036spid + .12terr + .35x + .025lanc + .052shark - .21deaths)
     * lanc: .6Points * (.07wb + .07jav + .055spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
     * shark: points * (.65*repels/death + .005terr + .0015shark + sum(.001allotherships) - 0.001deaths - (.07(allothershipstks) + .72spider + .5x + .15terrtk + .08sharkTK)))
     */
    public int getRating() {
        int rating = 0;
        switch(ship) {
            case 1: 
                rating = (int) ((0.45 * getScore()) * ((0.07 * eKills[0]) + (0.07 * eKills[1]) + (0.05 * eKills[2]) + (0.12 * eKills[4]) + (0.06 * eKills[6]) + (0.08 * eKills[7]) - (0.04 * getStat(StatType.DEATHS).getValue())));
                getStat(StatType.RATING).setValue(rating);
                return rating;
            case 2:
                rating = (int) ((0.6 * getScore()) * ((0.05 * eKills[0]) + (0.06 * eKills[1]) + (0.066 * eKills[2]) + (0.14 * eKills[4]) + (0.05 * eKills[6]) + (0.09 * eKills[7]) - (0.05 * getStat(StatType.DEATHS).getValue()) - (0.07 * tKills[0]) + (0.07 * tKills[1]) + (0.06 * tKills[2]) + (0.13 * tKills[4]) + (0.07 * tKills[6]) + (0.09 * tKills[7])));
                getStat(StatType.RATING).setValue(rating);
                return rating;
            case 3:
                rating = (int) ((0.4 * getScore()) * ((0.06 * eKills[0]) + (0.06 * eKills[1]) + (0.04 * eKills[2]) + (0.09 * eKills[4]) + (0.05 * eKills[6]) + (0.089 * eKills[7]) - (0.05 * getStat(StatType.DEATHS).getValue())));
                getStat(StatType.RATING).setValue(rating);
                return rating;
            case 5:
                rating = (int) ((2.45 * getScore()) * ((0.03 * eKills[0]) + (0.03 * eKills[1]) + (0.036 * eKills[2]) + (0.12 * eKills[4]) + (0.025 * eKills[6]) + (0.052 * eKills[7]) - (0.21 * getStat(StatType.DEATHS).getValue())));
                getStat(StatType.RATING).setValue(rating);
                return rating;
            case 7:
                rating = (int) ((0.6 * getScore()) * ((0.07 * eKills[0]) + (0.07 * eKills[1]) + (0.055 * eKills[2]) + (0.12 * eKills[4]) + (0.06 * eKills[6]) + (0.08 * eKills[7]) - (0.04 * getStat(StatType.DEATHS).getValue())));
                getStat(StatType.RATING).setValue(rating);
                return rating;
            case 8:
                rating = (int) ((getScore()) * ((0.65 * getRPD()) + (0.005 * eKills[4]) + (0.0015 * eKills[7]) + (0.001 * (eKills[0] + eKills[1] + eKills[2] + eKills[6]))) - (0.001 * getStat(StatType.DEATHS).getValue()) - ((0.07 * (tKills[0] + tKills[1] + tKills[6])) + (0.72 * tKills[2]) + (0.15 * tKills[4]) + (0.08 * tKills[7])));
                getStat(StatType.RATING).setValue(rating);
                return rating;
            default: return 0;
        }
    }
    
}
