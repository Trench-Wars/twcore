package twcore.core.lag;

public class LagReport {

    String name;
    String requester;

    String[] lagStats;
    String lagReport;

    boolean tinfoIncluded;
    boolean playerPresent;

    public LagReport(String requesterName, String playerName, String[] lagArray, String report, boolean tinfo, boolean present) {
        name = playerName;
        requester = requesterName;
        lagStats = lagArray;
        lagReport = report;

        tinfoIncluded = tinfo;
        playerPresent = present;
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
}