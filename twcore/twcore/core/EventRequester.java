package twcore.core;

/*
 * EventRequester.java
 *
 * Created on October 25, 2002, 9:12 AM
 */

/**
 *
 * @author  harvey
 */
public class EventRequester {
    static final int TOTAL_NUMBER = 23; 
    public static int MESSAGE = 0;
    public static int PLAYER_ENTERED = 1;
    public static int ARENA_LIST = 2;    
    public static int PLAYER_POSITION = 3;
    public static int PLAYER_LEFT = 4;
    public static int PLAYER_DEATH = 5;
    public static int PRIZE = 6;
    public static int SCORE_UPDATE = 7;
    public static int WEAPON_FIRED = 8;
    public static int FREQUENCY_CHANGE = 9;
    public static int FREQUENCY_SHIP_CHANGE = 10;
    public static int LOGGED_ON = 11;
    public static int FILE_ARRIVED = 12;
    public static int ARENA_JOINED = 13;
    public static int FLAG_VICTORY = 14;
    public static int FLAG_REWARD = 15;
    public static int SCORE_RESET = 16;
    public static int WATCH_DAMAGE = 17;
    public static int SOCCER_GOAL = 18;
    public static int BALL_POSITION = 19;
    public static int FLAG_POSITION = 20;
    public static int FLAG_DROPPED = 21;
    public static int FLAG_CLAIMED = 22;
    

    private boolean[] array;
    
    /** Creates a new instance of EventRequester */
    public EventRequester() {
        array = new boolean [TOTAL_NUMBER];
        declineAll();
    }
    
    public void request( int packetType ){
        array[packetType] = true;
    }
    
    public void requestAll(){
        for( int i = 0; i < array.length; i++ ){
            array[i] = true;
        }  
    }
    
    public void declineAll(){
        for( int i = 0; i < array.length; i++ ){
            array[i] = false;
        }        
    }
    
    public void decline( int packetType ){
        array[packetType] = false;
    }
    
    public boolean check( int packetType ){
        return array[packetType];
    }
    
    public void set( int packetType, boolean value ){
        array[packetType] = value;
    }
    
}
