package twcore.core.stats;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.util.Tools;

public class lagHandler
{

    BotAction m_botAction;
    BotSettings m_botSettings;

    Object bot;
    String methodName;

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

    TimerTask tITimer;

    boolean lagLimitsInEffect = false;
    boolean free = true;
    int state;
	int lastRequestStarted = 0;

    String playerName = "";
    String requester = "";

    int[] serverTime = new int[2];
    int[] userTime = new int[2];
    Vector<Integer> tinfoValues;

    LinkedList<lagRequest> m_lagRequest;

    public lagHandler(BotAction botAction, BotSettings botSettings, Object b, String mN)
    {
        m_botAction = botAction;
        m_botSettings = botSettings;
        m_lagRequest = new LinkedList<lagRequest>();
        tinfoValues = new Vector<Integer>();
        bot = b;
        methodName = mN;

        if (m_botSettings.getInt("EnableLagLimits") == 1)
        {
            lagLimitsInEffect = true;
        }
    }

    public void requestLag(String lagger, String req)
    {
        if (lagger == null) { return; }
        if (req == null) { req = "[BOT]"; }

        if (free)
        {
            startLagRequest(lagger, req);
        }
        else
        {
			if (((int)System.currentTimeMillis() / 1000) - lastRequestStarted > 3)
			{
				startLagRequest(lagger, req);
			} else {
	            m_lagRequest.add(new lagRequest(lagger, req));
			}
        }
    }

    public void requestLag(String lagger)
    {
        requestLag(lagger, null);
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
                            tInfoCheck();
                        };
                    };
                    m_botAction.scheduleTask(tITimer, 500);
                }

                try
                {
                    StringTokenizer tinfoTokens = new StringTokenizer(message);

                    if (tinfoTokens.countTokens() == 3)
                    {
                        int sTime = Integer.parseInt(tinfoTokens.nextToken());
                        int uTime = Integer.parseInt(tinfoTokens.nextToken());
                        int diff = Integer.parseInt(tinfoTokens.nextToken());

                        if (sTime - uTime == diff)
                        {
							if (tinfoValues.size() == 0) {
								serverTime[0] = sTime;
								userTime[0] = uTime;
							}
                            tinfoValues.add(new Integer(diff));

                            serverTime[1] = sTime;
                            userTime[1] = uTime;

                            if (tinfoValues.size() >= 32)
                            {
                                m_botAction.cancelTask(tITimer);
								tInfoCheck();
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

    public void tInfoCheck()
    {
        if (tinfoValues.size() >= 10)
        {
            adjustTinfoValues();

            spikeMean = calcSpikeMean();
            spikeSD = calcSpikeSD();
            numSpikes = calcNumSpikes();
            spamLagInfo(true, true);
        }
        else
        {
            spamLagInfo(false, true);
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
            tinfoValue = tinfoValues.get(index);
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
            tinfoValue = tinfoValues.get(index);
            delta = tinfoValue.intValue() - spikeMean;
            sd = sd + delta * delta;
        }
        return Math.sqrt(sd / tinfoValues.size()) * 10;
    }

    private int calcNumSpikes()
    {
        int spikeCount = 0;
        Integer tinfoValue = tinfoValues.get(0);
        int lastTinfo = tinfoValue.intValue();
        int thisTinfo;

        for (int index = 1; index < tinfoValues.size(); index++)
        {
            tinfoValue = tinfoValues.get(index);
            thisTinfo = tinfoValue.intValue();

            if (Math.abs(lastTinfo - thisTinfo) * 10 > spikeSize)
                spikeCount++;

            lastTinfo = thisTinfo;
        }
        return spikeCount;
    }

	public void adjustTinfoValues()
	{
		int deltaServerTime = serverTime[1] - serverTime[0];
		int deltaUserTime = userTime[1] - userTime[0];

		double delta = ((double)deltaServerTime - (double)deltaUserTime) / 32.0;

//		m_botAction.sendPrivateMessage("Sika", "Adjusting: " + playerName + " Delta: " + delta + "("+deltaServerTime+"/"+deltaUserTime+")");

        Integer tinfoValue;
		for (int index = 1; index < tinfoValues.size(); index++)
        {
            tinfoValue = tinfoValues.get(index);
            int newValue = tinfoValue.intValue() - (int)(delta * index);
//			m_botAction.sendPrivateMessage("Sika", tinfoValue.intValue() + " -> " + newValue);
            tinfoValues.set(index, new Integer(newValue));
        }
	}

    public void startLagRequest(String pName, String req)
    {
        lastRequestStarted = (int)System.currentTimeMillis() / 1000;
        free = false;
        playerName = pName;
        requester = req;
        state = 1;
        tinfoValues.clear();

        if (m_botAction.getFuzzyPlayerName(playerName) == null)
        {
            spamLagInfo(false, false);
        }
        else
        {
            playerName = m_botAction.getFuzzyPlayerName(playerName);
            m_botAction.sendUnfilteredPrivateMessage(playerName, "*info");
        }
    }

    public void spamLagInfo(boolean tI, boolean present)
    {
        LagReport report;
  
        if (present) {
        	
        	int ship=0;
        	if (m_botAction.getPlayer(playerName) != null) {
        		ship = m_botAction.getPlayer(playerName).getShipType();
        	}

            String[] lag = new String[2];
            lag[0] = playerName + ": PING Cur: " + currentPing + "ms Ave: " + averagePing + "ms Low: " + lowPing + "ms Hi: " + highPing + "ms PLOSS S2C: " + s2C + "% C2S: " + c2S + "% S2CWeapons: " + s2CWeapons + "%";
            if (!tI) {
                lag[1] = Tools.formatString("", playerName.length(), "-") + "  SLOW S2C: " + s2CSlowPercent + "% C2S: " + c2SSlowPercent + "% NO SPIKE INFO WAS RETRIEVED";
            } else {
                lag[1] = Tools.formatString("", playerName.length(), "-") + "  SLOW S2C: " + s2CSlowPercent + "% C2S: " + c2SSlowPercent + "% SPIKE Med: " + medF.format(spikeSD) + " Count: " + numSpikes;
            }

            String lagReport = null;

            if (lagLimitsInEffect) {

                boolean spec = false;

                if (m_botSettings.getInt("CurPing"+ship)!=0 && m_botSettings.getInt("CurPing"+ship) < currentPing && !spec) {
                	spec = true;
                    lagReport = "PING Cur. [" + currentPing + "  LIMIT: " + m_botSettings.getInt("CurPing") + "]";
                }
                else if (m_botSettings.getInt("CurPing") < currentPing && !spec) {
                    spec = true;
                    lagReport = "PING Cur. [" + currentPing + "  LIMIT: " + m_botSettings.getInt("CurPing") + "]";
                }
                
                if (m_botSettings.getInt("AvePing"+ship)!=0 && m_botSettings.getInt("AvePing"+ship) < averagePing && !spec) {
                	spec = true;
                    lagReport = "PING Ave. [" + averagePing + "  LIMIT: " + m_botSettings.getInt("AvePing") + "]";
                }
                else if (m_botSettings.getInt("AvePing") < averagePing && !spec) {
                    spec = true;
                    lagReport = "PING Ave. [" + averagePing + "  LIMIT: " + m_botSettings.getInt("AvePing") + "]";
                }
                
                if (m_botSettings.getInt("S2CPloss"+ship)!=0 && m_botSettings.getInt("S2CPloss"+ship) < s2C && !spec) {
                	spec = true;
                    lagReport = "PLOSS S2C. [" + s2C + "  LIMIT: " + m_botSettings.getDouble("S2CPloss") + "]";
                }
                else if (m_botSettings.getDouble("S2CPloss") < s2C && !spec) {
                    spec = true;
                    lagReport = "PLOSS S2C. [" + s2C + "  LIMIT: " + m_botSettings.getDouble("S2CPloss") + "]";
                }
                
                if (m_botSettings.getInt("C2SPloss"+ship)!=0 && m_botSettings.getInt("C2SPloss"+ship) < c2S && !spec) {
                	spec = true;
                    lagReport = "PLOSS C2S. [" + c2S + "  LIMIT: " + m_botSettings.getDouble("C2SPloss") + "]";
                }
                else if (m_botSettings.getDouble("C2SPloss") < c2S && !spec) {
                    spec = true;
                    lagReport = "PLOSS C2S. [" + c2S + "  LIMIT: " + m_botSettings.getDouble("C2SPloss") + "]";
                }
                
                if (m_botSettings.getInt("WeaponPloss"+ship)!=0 && m_botSettings.getInt("WeaponPloss"+ship) < s2CWeapons && !spec) {
                    spec = true;
                    lagReport = "PLOSS S2CWeapons [" + s2CWeapons + "  LIMIT: " + m_botSettings.getDouble("WeaponPloss") + "]";
                }
                else if (m_botSettings.getDouble("WeaponPloss") < s2CWeapons && !spec) {
                    spec = true;
                    lagReport = "PLOSS S2CWeapons [" + s2CWeapons + "  LIMIT: " + m_botSettings.getDouble("WeaponPloss") + "]";
                }
                
                if (m_botSettings.getInt("SlowS2C"+ship)!=0 && m_botSettings.getInt("SlowS2C"+ship) < s2CSlowPercent && !spec) {
                    spec = true;
                    lagReport = "PLOSS Slow S2C [" + s2CSlowPercent + "  LIMIT: " + m_botSettings.getDouble("SlowS2C") + "]";
                }
                else if (m_botSettings.getDouble("SlowS2C") < s2CSlowPercent && !spec) {
                    spec = true;
                    lagReport = "PLOSS Slow S2C [" + s2CSlowPercent + "  LIMIT: " + m_botSettings.getDouble("SlowS2C") + "]";
                }
                
                if (m_botSettings.getInt("SlowC2S"+ship)!=0 && m_botSettings.getInt("SlowC2S"+ship) < c2SSlowPercent && !spec) {
                    spec = true;
                    lagReport = "PLOSS Slow C2S [" + c2SSlowPercent + "  LIMIT: " + m_botSettings.getDouble("SlowC2S") + "]";
                }
                else if (m_botSettings.getDouble("SlowC2S") < c2SSlowPercent && !spec) {
                    spec = true;
                    lagReport = "PLOSS Slow C2S [" + c2SSlowPercent + "  LIMIT: " + m_botSettings.getDouble("SlowC2S") + "]";
                }

                if (tI) {
                    if (m_botSettings.getInt("Med") < spikeSD && !spec) {
                        spec = true;
                        lagReport = "SPIKE Med. [" + medF.format(spikeSD) + "  LIMIT: " + m_botSettings.getInt("Med") + "]";
                    }
                    if (m_botSettings.getInt("SpikeCount") < numSpikes && !spec) {
                        spec = true;
                        lagReport = "SPIKE Count [" + numSpikes + "  LIMIT: " + m_botSettings.getInt("SpikeCount") + "]";
                    }
                }

                if (spec) {
                    lagReport = "LAG REPORT: Too high " + lagReport;
                } else {
                    lagReport = null;
                }
            }
            report = new LagReport(requester, playerName, lag, lagReport, tI, present);
        } else {
            String[] lag = { "Player ["+playerName+"] was not found in the arena" };
            report = new LagReport(requester, playerName, lag, null, tI, present);
        }

        try
        {
            Class       parameterTypes[] = { report.getClass() };
            Object      lagReportA[] = { report };
            bot.getClass().getMethod(methodName, parameterTypes).invoke(bot, lagReportA);
        }
        catch (Exception e)
        {
            Tools.printLog("Could not invoke method '" + methodName + "()' in class " + bot);
            Tools.printStackTrace( e );
        }

        state = 0;

        ListIterator<lagRequest> i = m_lagRequest.listIterator();
        while (i.hasNext())
        {
            lagRequest t = i.next();

            i.remove();
            startLagRequest(t.getPlayerName(), t.getRequester());
            return;
        };
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

        public lagRequest(String name, String req)
        {
            playerName = name;
            requester = req;
        };

        public String getPlayerName()
        {
            return playerName;
        };

        public String getRequester()
        {
            return requester;
        };
    };
}