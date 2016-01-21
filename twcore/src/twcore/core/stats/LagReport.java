package twcore.core.stats;


public class LagReport {

    String name;
    String requester;

    String[] lagStats;
    String lagReport;

    boolean tinfoIncluded;
    boolean playerPresent;

    public LagReport(String r, String n, String[] l, String lR, boolean t, boolean p) {
        name = n;
        requester = r;
        lagStats = l;
        lagReport = lR;

        tinfoIncluded = t;
        playerPresent = p;
    }

    public String getName() {
        return name;
    }

    public String getRequester() {
        return requester;
    }

    public String[] getLagStats() {
        return lagStats;
    }

    public String getLagReport() {
        return lagReport;
    }

    public boolean isOverLimits() {
        return lagReport != null;
    }

    public boolean isTinfoIncluded() {
        return tinfoIncluded;
    }

    public boolean isPlayerPresent() {
        return playerPresent;
    }

    public boolean isBotRequest() {
        return requester.equals("[BOT]");
    }
};