package twcore.core;

/*
 * EventRequester.java
 *
 * Created on October 25, 2002, 9:12 AM
 */

/**
 *
 * @author harvey, modified by FoN
 */
public class EventRequester
{
    //Changes to this are very detremental.  Please be careful
    //since the recordbot + lots of other classes depends on these numbers
    //Only change them if you know what you are doing!
    //Adding new packets requires the total number to be changed as well
    public static final int TOTAL_NUMBER = 26;
    public static final int MESSAGE = 0;
    public static final int PLAYER_ENTERED = 1;
    public static final int ARENA_LIST = 2;
    public static final int PLAYER_POSITION = 3;
    public static final int PLAYER_LEFT = 4;
    public static final int PLAYER_DEATH = 5;
    public static final int PRIZE = 6;
    public static final int SCORE_UPDATE = 7;
    public static final int WEAPON_FIRED = 8;
    public static final int FREQUENCY_CHANGE = 9;
    public static final int FREQUENCY_SHIP_CHANGE = 10;
    public static final int LOGGED_ON = 11;
    public static final int FILE_ARRIVED = 12;
    public static final int ARENA_JOINED = 13;
    public static final int FLAG_VICTORY = 14;
    public static final int FLAG_REWARD = 15;
    public static final int SCORE_RESET = 16;
    public static final int WATCH_DAMAGE = 17;
    public static final int SOCCER_GOAL = 18;
    public static final int BALL_POSITION = 19;
    public static final int FLAG_POSITION = 20;
    public static final int FLAG_DROPPED = 21;
    public static final int FLAG_CLAIMED = 22;
    public static final int TURF_FLAG_UPDATE = 23;
    public static final int TURRET_EVENT = 24;
    public static final int PLAYER_BANNER = 25;

    private boolean[] array;

    /** Creates a new instance of EventRequester */
    public EventRequester()
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
