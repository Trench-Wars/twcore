package twcore.bots.twdt;

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
    
    public void handleKill(int kill) {
        eKills[kill-1]++;
    }
    
    public void handleTeamKill(int kill) {
        tKills[kill-1]++;
    }

    public int getScore() {
        return getStat(StatType.SCORE).getValue();
    }

    /**
     * Method getRating.
     * This returns the rating for the player according to this:
     *
     * warbird: .45Points * (.07wb + .07jav + .05spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
     * jav: .6Points * (.05wb + .06jav + .066spid + 0.14terr + .07x + .05lanc + .09shark - .05deaths - (.07wbTK + .07javTK + .06spiderTK + .13terrTK + .06WeaselTK + .07LancTK + .09SharkTK))
     * spiders: .4points * (.06wb + .06jav + .04spid + .09terr + .05x + .05lanc + .089shark - .05deaths)
     * terr: 2.45points * (.03wb + .03jav + .036spid + .12terr + .35x + .025lanc + .052shark - .21deaths)
     * weasel: .8points * (sum(.09allships) - 0.05deaths)
     * lanc: .6Points * (.07wb + .07jav + .055spid + 0.12terr + .05x + .06lanc + .08shark - .04deaths)
     * shark: points * (.65*repels/death + .005terr + .0015shark + sum(.001allotherships) - 0.001deaths - (.07(allothershipstks) + .72spider + .5x + .15terrtk + .08sharkTK)))
     */
    public int getRating() {
        //do later
        return 0;
    }
    
}
