package twcore.misc.lag;

import twcore.core.*;
import java.util.*;
import java.text.*;

public class lagHandler
{

    BotAction m_botAction;
    BotSettings m_botSettings;

    int currentPing;
    int averagePing;
    int lowPing;
    int highPing;
    int sessionTime;
    int c2SSlowPackets;
    int s2CSlowPackets;
    int spikeSize = 100;
    int numSpikes;
    double s2C;
    double c2S;
    double s2CWeapons;
    double spikeSD;
    double spikeMean;
    double c2SSlowPercent;
    double s2CSlowPercent;

    DecimalFormat medF = new DecimalFormat("0.00");

    boolean botR;
    boolean silent;
    boolean spec;

    TimerTask tITimer;

    boolean free;
    int state;

    String playerName = "";
    String requester = "";
    String lPlayerName = "";
    String lRequester = "";
    String lagReport;

    Vector tinfoValues;

    LinkedList m_lagRequest;

    public lagHandler(BotAction botAction, BotSettings botSettings)
    {
        m_botAction = botAction;
        m_botSettings = botSettings;
        m_lagRequest = new LinkedList();
        tinfoValues = new Vector();

        if (m_botSettings.getInt("CurPing") != 0)
        {
            free = true;
        }
        else
        {
            free = false;
        }
    }

    public void requestLag(String lagger, String req, boolean sil, boolean bot)
    {

        if (lagger == null) { return; }

        if (free)
        {
            startLagRequest(lagger, req, sil, bot);
        }
        else
        {
            m_lagRequest.add(new lagRequest(lagger, req, sil, bot));
        }
    }

    public void handleLagMessage(String message)
    {

        if (!free)
        {

            if (state == 1)
            {

                if (message.startsWith("Ping:"))
                {
                    doGetPing(message);
                }
                if (message.startsWith("LOSS:"))
                {
                    doGetPloss(message);
                }
                if (message.startsWith("C2S CURRENT:"))
                {
                    c2SSlowPackets = getPacketInfo(message);
                    c2SSlowPercent = getPercentInfo(message);
                }
                if (message.startsWith("S2C CURRENT:"))
                {
                    s2CSlowPackets = getPacketInfo(message);
                    s2CSlowPercent = getPercentInfo(message);
                }
                if (message.startsWith("TIME:"))
                {
                    sessionTime = parseSessionTime(getInfoString(message, "Session:"));
                }
                if (message.startsWith("Bytes/Sec:"))
                {
                    state = 2;

                    if (m_botAction.getPlayer(playerName) == null)
                    {
                        spamLagInfo(false, true);
                    }
                    else
                    {
                        m_botAction.sendUnfilteredPrivateMessage(playerName, "*tinfo");
                    }
                }
            }
            else if (state == 2)
            {

                if (message.startsWith("ServerTime"))
                {
                    tITimer = new TimerTask()
                    {
                        public void run()
                        {
                            spamLagInfo(false, true);
                        };
                    };
                    m_botAction.scheduleTask(tITimer, 500);
                }

                try
                {
                    StringTokenizer tinfoTokens = new StringTokenizer(message);

                    if (tinfoTokens.countTokens() == 3)
                    {
                        int serverTime = Integer.parseInt(tinfoTokens.nextToken());
                        int userTime = Integer.parseInt(tinfoTokens.nextToken());
                        int diff = Integer.parseInt(tinfoTokens.nextToken());

                        if (serverTime - userTime == diff)
                        {
                            tinfoValues.add(new Integer(diff));

                            if (tinfoValues.size() >= 32)
                            {
                                tITimer.cancel();
                                spikeMean = calcSpikeMean();
                                spikeSD = calcSpikeSD();
                                numSpikes = calcNumSpikes();
                                spamLagInfo(true, true);
                            }
                        }
                    }
                }
                catch (RuntimeException e)
                {
                }
            }
        }
    }

    private void doGetPing(String message)
    {
        currentPing = parsePing(message, "Ping:");
        averagePing = parsePing(message, "AvePing:");
        lowPing = parsePing(message, "LowPing:");
        highPing = parsePing(message, "HighPing:");
    }

    private void doGetPloss(String message)
    {
        s2C = parsePloss(message, "S2C:");
        c2S = parsePloss(message, "C2S:");
        s2CWeapons = parsePloss(message, "S2CWeapons:");
    }

    private int getPacketInfo(String message)
    {

        try
        {
            String packetString = getInfoString(message, "Slow:");
            int endIndex = packetString.indexOf(' ');

            return Integer.parseInt(packetString.substring(0, endIndex));
        }
        catch (RuntimeException e)
        {
            throw new RuntimeException("ERROR: Could not parse get the number of slow packets.");
        }
    }

    private double getPercentInfo(String message)
    {

        try
        {
            String packetString = getInfoString(message, "Slow:");
            int beginIndex = packetString.lastIndexOf(' ');
            int endIndex = packetString.lastIndexOf("%");

            return Double.parseDouble(packetString.substring(beginIndex, endIndex));
        }
        catch (RuntimeException e)
        {
            throw new RuntimeException("ERROR: Could not parse the percent of slow packets.");
        }
    }

    private int parsePing(String message, String pingType)
    {

        try
        {
            String pingString = getInfoString(message, pingType);
            int msIndex = pingString.lastIndexOf("ms");

            return Integer.parseInt(pingString.substring(0, msIndex));
        }
        catch (RuntimeException e)
        {
            throw new RuntimeException("ERROR: Could not parse the string: " + pingType + ".");
        }
    }

    private double parsePloss(String message, String plossType)
    {

        try
        {
            String plossString = getInfoString(message, plossType);
            int percentIndex = plossString.lastIndexOf("%");
            String percentString = plossString.substring(0, percentIndex);
            int sign = 1;

            if (percentString.indexOf('-') != -1)
                sign = -1;

            return sign * Double.parseDouble(removeChar(percentString, '-'));
        }
        catch (NumberFormatException e)
        {
            throw new NumberFormatException("ERROR: Could not parse the string: " + plossType + ".");
        }
    }

    private String getInfoString(String message, String infoName)
    {
        int beginIndex = message.indexOf(infoName);
        int endIndex;

        if (beginIndex == -1)
            throw new IllegalArgumentException("ERROR: Could not find the field: " + infoName + ".");

        beginIndex = beginIndex + infoName.length();
        endIndex = message.indexOf("  ", findFirstNotOf(message, ' ', beginIndex));

        if (endIndex == -1)
            endIndex = message.length();

        return message.substring(beginIndex, endIndex);
    }

    private String removeChar(String string, char character)
    {
        StringBuffer stringBuffer = new StringBuffer(string);
        int removeIndex;

        for (;;)
        {
            removeIndex = stringBuffer.indexOf(Character.toString(character));

            if (removeIndex == -1)
                break;

            stringBuffer.deleteCharAt(removeIndex);
        }
        return stringBuffer.toString();
    }

    private int findFirstNotOf(String string, char targetChar, int beginIndex)
    {
        char charAt;

        for (int index = beginIndex + 1; index < string.length(); index++)
        {
            charAt = string.charAt(index);

            if (charAt != targetChar)
                return index;
        }
        return -1;
    }

    private int parseSessionTime(String sessionInfo)
    {

        try
        {
            StringTokenizer sessionInfoTokens = new StringTokenizer(sessionInfo, " :");
            int minutes = 0;

            minutes = 60 * Integer.parseInt(sessionInfoTokens.nextToken());
            minutes = minutes + Integer.parseInt(sessionInfoTokens.nextToken());
            return minutes;
        }
        catch (NumberFormatException e)
        {
            throw new NumberFormatException("ERROR: Could not parse the session time.");
        }
    }

    private double calcSpikeMean()
    {
        Integer tinfoValue;
        double mean = 0;

        for (int index = 0; index < tinfoValues.size(); index++)
        {
            tinfoValue = (Integer) tinfoValues.get(index);
            mean = mean + tinfoValue.intValue();
        }
        return mean / tinfoValues.size();
    }

    private double calcSpikeSD()
    {
        Integer tinfoValue;
        double sd = 0;
        double delta;

        for (int index = 0; index < tinfoValues.size(); index++)
        {
            tinfoValue = (Integer) tinfoValues.get(index);
            delta = tinfoValue.intValue() - spikeMean;
            sd = sd + delta * delta;
        }
        return Math.sqrt(sd / tinfoValues.size()) * 10;
    }

    private int calcNumSpikes()
    {
        int spikeCount = 0;
        Integer tinfoValue = (Integer) tinfoValues.get(0);
        int lastTinfo = tinfoValue.intValue();
        int thisTinfo;

        for (int index = 1; index < tinfoValues.size(); index++)
        {
            tinfoValue = (Integer) tinfoValues.get(index);
            thisTinfo = tinfoValue.intValue();

            if (Math.abs(lastTinfo - thisTinfo) * 10 > spikeSize)
                spikeCount++;

            lastTinfo = thisTinfo;
        }
        return spikeCount;
    }

    private boolean checkGrowing()
    {
        Integer tinfoValue = (Integer) tinfoValues.get(0);
        int lastTinfo = tinfoValue.intValue();
        for (int index = 1; index < tinfoValues.size(); index++)
        {
            tinfoValue = (Integer) tinfoValues.get(index);
            if (tinfoValue.intValue() < lastTinfo) { return false; }
        }
        return true;
    }

    private boolean checkDiminishing()
    {
        Integer tinfoValue = (Integer) tinfoValues.get(0);
        int lastTinfo = tinfoValue.intValue();
        for (int index = 0; index < tinfoValues.size(); index++)
        {
            tinfoValue = (Integer) tinfoValues.get(index);
            if (tinfoValue.intValue() > lastTinfo) { return false; }
        }
        return true;
    }

    public void startLagRequest(String pName, String req, boolean sil, boolean bot)
    {

        free = false;
        spec = false;
        playerName = pName;
        requester = req;
        silent = sil;
        botR = bot;
        state = 1;

        if (m_botAction.getFuzzyPlayerName(pName) == null)
        {
            spamLagInfo(false, false);
        }
        else
        {
            playerName = m_botAction.getFuzzyPlayerName(pName);
            m_botAction.sendUnfilteredPrivateMessage(playerName, "*info");
        }
    }

    public void spamLagInfo(boolean tI, boolean present)
    {

        if (present)
        {

            if (!tI)
            {
                String lag[] = {
                        playerName + ": PING Cur: " + currentPing + "ms Ave: " + averagePing + "ms Low: " + lowPing
                                + "ms Hi: " + highPing + "ms PLOSS S2C: " + s2C + "% C2S: " + c2S + "% S2CWeapons: "
                                + s2CWeapons + "%",
                        Tools.formatString("", playerName.length(), "-") + "  SLOW S2C: " + s2CSlowPercent + "% C2S: "
                                + c2SSlowPercent + "% NO SPIKE INFO WAS RETRIEVED" };
                if (!botR)
                {
                    m_botAction.privateMessageSpam(requester, lag);
                }
            }
            else
            {
                String lag[] = {
                        playerName + ": PING Cur: " + currentPing + "ms Ave: " + averagePing + "ms Low: " + lowPing
                                + "ms Hi: " + highPing + "ms PLOSS S2C: " + s2C + "% C2S: " + c2S + "% S2CWeapons: "
                                + s2CWeapons + "%",
                        Tools.formatString("", playerName.length(), "-") + "  SLOW S2C: " + s2CSlowPercent + "% C2S: "
                                + c2SSlowPercent + "% SPIKE Med: " + medF.format(spikeSD) + " Count: " + numSpikes };
                if (!botR)
                {
                    m_botAction.privateMessageSpam(requester, lag);
                }
            }

            if (!silent)
            {
                if (m_botAction.getPlayer(playerName) != null && m_botAction.getPlayer(playerName).getShipType() != 0)
                {

                    if (m_botSettings.getInt("CurPing") < currentPing && !spec)
                    {
                        spec = true;
                        lagReport = "PING Cur. [" + currentPing + "  LIMIT: " + m_botSettings.getInt("CurPing") + "]";
                    }
                    if (m_botSettings.getInt("AvePing") < averagePing && !spec)
                    {
                        spec = true;
                        lagReport = "PING Ave. [" + averagePing + "  LIMIT: " + m_botSettings.getInt("AvePing") + "]";
                    }
                    if (m_botSettings.getInt("S2CPloss") < s2C && !spec)
                    {
                        spec = true;
                        lagReport = "PLOSS S2C. [" + s2C + "  LIMIT: " + m_botSettings.getInt("S2CPloss") + "]";
                    }
                    if (m_botSettings.getInt("C2SPloss") < c2S && !spec)
                    {
                        spec = true;
                        lagReport = "PLOSS C2S. [" + c2S + "  LIMIT: " + m_botSettings.getInt("C2SPloss") + "]";
                    }
                    if (m_botSettings.getInt("WeaponPloss") < s2CWeapons && !spec)
                    {
                        spec = true;
                        lagReport = "PLOSS S2CWeapons [" + s2CWeapons + "  LIMIT: "
                                + m_botSettings.getInt("WeaponPloss") + "]";
                    }
                    if (m_botSettings.getInt("SlowS2C") < s2CSlowPercent && !spec)
                    {
                        spec = true;
                        lagReport = "PLOSS Slow S2C [" + s2CSlowPercent + "  LIMIT: " + m_botSettings.getInt("SlowS2C")
                                + "]";
                    }
                    if (m_botSettings.getInt("SlowC2S") < c2SSlowPercent && !spec)
                    {
                        spec = true;
                        lagReport = "PLOSS Slow C2S [" + c2SSlowPercent + "  LIMIT: " + m_botSettings.getInt("SlowC2S")
                                + "]";
                    }

                    if (tI)
                    {
                        if (m_botSettings.getInt("Med") < spikeSD && !spec && (!checkGrowing() && !checkDiminishing()))
                        {
                            spec = true;
                            lagReport = "SPIKE Med. [" + medF.format(spikeSD) + "  LIMIT: "
                                    + m_botSettings.getInt("Med") + "]";
                        }
                        if (m_botSettings.getInt("SpikeCount") < numSpikes && !spec)
                        {
                            spec = true;
                            lagReport = "SPIKE Count [" + numSpikes + "  LIMIT: " + m_botSettings.getInt("SpikeCount")
                                    + "]";
                        }
                    }

                    if (spec)
                    {
                        if (!botR)
                        {
                            m_botAction.sendPrivateMessage(requester, "LAG REPORT: Too high " + lagReport);
                        }
                        m_botAction.sendPrivateMessage(playerName, "LAG REPORT: Too high " + lagReport);
                        m_botAction.spec(playerName);
                        m_botAction.spec(playerName);
                    }
                }
            }
        }
        else
        {
            if (!botR)
            {
                m_botAction.sendPrivateMessage(requester, "Could not find '" + playerName + "' on the arena.");
            }
        }

        tinfoValues.clear();
        state = 0;

        ListIterator i = m_lagRequest.listIterator();
        while (i.hasNext())
        {
            lagRequest t = (lagRequest) i.next();

            i.remove();
            startLagRequest(t.getPlayerName(), t.getRequester(), t.getSilent(), t.getBot());
            return;
        }
        ;
        free = true;
    }

    public String getStatus()
    {
        String s = "LagHandler Status: ";
        if (free)
        {
            s += "free ";
        }
        else
        {
            s += "not free ";
        }

        s += "State: " + state;

        return s;
    }

    class lagRequest
    {
        String playerName = "";
        String requester = "";
        boolean silent;
        boolean botR;

        public lagRequest(String name, String req, boolean sil, boolean bot)
        {
            playerName = name;
            requester = req;
            silent = sil;
            botR = bot;
        };

        public String getPlayerName()
        {
            return playerName;
        };

        public String getRequester()
        {
            return requester;
        };

        public boolean getSilent()
        {
            return silent;
        };

        public boolean getBot()
        {
            return botR;
        };
    };
}