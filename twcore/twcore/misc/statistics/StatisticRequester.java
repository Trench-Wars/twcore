package twcore.misc.statistics;

/**
 *
 * @author FoN / Kirthi Sugnanam
 * Modified Harvey's EventRequester.java
 */
public class StatisticRequester
{
	//If this is modified make sure you implement the statistic + set and get methods
	//in Statistics.java
	public static final int TOTAL_NUMBER = 24;
	public static final int TOTAL_KILLS = 0;
	public static final int DEATHS = 1;
	public static final int SCORE = 2;
	public static final int WARBIRD_KILL = 3;
	public static final int JAVELIN_KILL = 4;
	public static final int SPIDER_KILL = 5;
	public static final int LEVIATHAN_KILL = 6;
	public static final int TERRIER_KILL = 7;
	public static final int WEASEL_KILL = 8;
	public static final int LANCASTER_KILL = 9;
	public static final int SHARK_KILL = 10;
	public static final int WARBIRD_TEAMKILL = 11;
	public static final int JAVELIN_TEAMKILL = 12;
	public static final int SPIDER_TEAMKILL = 13;
	public static final int LEVIATHAN_TEAMKILL = 14;
	public static final int TERRIER_TEAMKILL = 15;
	public static final int WEASEL_TEAMKILL = 16;
	public static final int LANCASTER_TEAMKILL = 17;
	public static final int SHARK_TEAMKILL = 18;
	public static final int FLAG_CLAIMED = 19;
	public static final int SHIP_TYPE = 20;
	public static final int TOTAL_TEAMKILLS = 21;
	public static final int RATING = 22;
	public static final int REPELS_USED = 23;

	private boolean[] array;

	/** Creates a new instance of EventRequester */
	public StatisticRequester()
	{
		array = new boolean[TOTAL_NUMBER];
		declineAll();
	}

	public void request(int packetType)
	{
		array[packetType] = true;
	}

	public void requestAll()
	{
		for (int i = 0; i < array.length; i++)
		{
			array[i] = true;
		}
	}

	public void declineAll()
	{
		for (int i = 0; i < array.length; i++)
		{
			array[i] = false;
		}
	}

	public void decline(int packetType)
	{
		array[packetType] = false;
	}

	public boolean check(int packetType)
	{
		return array[packetType];
	}

	public void set(int packetType, boolean value)
	{
		array[packetType] = value;
	}

}
