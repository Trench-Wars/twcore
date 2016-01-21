package twcore.core;

/**
    Used to request which events will be sent to a bot.  EventRequester is defined
    as part of Session, and can be retrieved from it using BotAction's
    getEventRequester() method.  Only those events which are specifically
    requested are sent; all others are ignored.
    <p>
    NOTE: See code of this class for a brief description of what each event does,
    and the name of the file associated with it.
    <p>
    If interested in handling an event that isn't covered here, see the instructions
    in the javadoc for GamePacketInterpreter's translateNormalPacket method.
    @author harvey
*/
public class EventRequester {

    /*
        Changes to the following list can have a detrimental effect.  Please be
        careful, as some classes depend on these records and will not work well with
        inserted indeces, etc.  (i.e., only change if you know what you're doing.)
        Adding new packets requires the total number to be changed as well.
    */

    // Total number of events that can possibly be handled
    public static final int TOTAL_NUMBER = 27;

    // Fired on message sent to the bot, including arenas/errors/alerts (Message.java)
    public static final int MESSAGE = 0;
    // Fired when a player enters an arena, and also when the bot does (PlayerEntered.java)
    public static final int PLAYER_ENTERED = 1;
    // Fired when the bot receives the arena list; data used for ?arena (Arena.java)
    public static final int ARENA_LIST = 2;
    // Fired whenever a player who is within radar range of the bot changes position.
    // Note that player position packets have both long and short versions, and that
    // Subspace clients use a positioning algorithm to determine where a player will
    // be rather than transmitting position with every update packet.  TWCore uses
    // no such algorithm (compared to resource cost, it's not worthwhile), and so
    // positions taken from packets are not incredibly reliable or updated very
    // often -- particularly if the player continues in a straight line (PlayerPosition.java)
    public static final int PLAYER_POSITION = 3;
    // Fired whenever a player leaves the arena or quits the game (PlayerLeft.java)
    public static final int PLAYER_LEFT = 4;
    // Fired whenever a player dies.  Note that this does not mean that records
    // of the player who died or the player who made the kill will still exist in the
    // system by the time the event gets to the bot.  Check for null (PlayerDeath.java)
    public static final int PLAYER_DEATH = 5;
    // Fired when a player receives a prize (Prize.java)
    public static final int PRIZE = 6;
    // Fired whenever a player's score is updated (ScoreUpdate.java)
    public static final int SCORE_UPDATE = 7;
    // Fired whenever a player fires a weapon (WeaponFired.java)
    public static final int WEAPON_FIRED = 8;
    // Fired whenever a player changes frequency without changing ship (FrequencyChange.java)
    public static final int FREQUENCY_CHANGE = 9;
    // Fired whenever a player changes both frequency and ship (FrequencyShipChange.java)
    public static final int FREQUENCY_SHIP_CHANGE = 10;
    // Fired when the bot logs on (LoggedOn.java)
    public static final int LOGGED_ON = 11;
    // Fired whenever a requested file successfully arrives (FileArrived.java)
    public static final int FILE_ARRIVED = 12;
    // Fired whenever the bot joins a new arena (ArenaJoined.java)
    public static final int ARENA_JOINED = 13;
    // Fired when a flag game has finished (FlagVictory.java)
    public static final int FLAG_VICTORY = 14;
    // Fired whenever the flag reward is given out (FlagReward.java)
    public static final int FLAG_REWARD = 15;
    // Fired whenever a player's score is reset (ScoreReset.java)
    public static final int SCORE_RESET = 16;
    // Fired for every packet received from the issue of *watchdamage (WatchDamage.java)
    public static final int WATCH_DAMAGE = 17;
    // Fired whenever a soccer goal is made (SoccerGoal.java)
    public static final int SOCCER_GOAL = 18;
    // Fired when a ball position packet is received.  Suffers from the same problems
    // as player position packets (BallPosition.java)
    public static final int BALL_POSITION = 19;
    // Fired when a flag position packet is received -- when a flag is dropped,
    // reset or forcibly moved.  Otherwise follows player holding.  Note that
    // TWCore does not handle flagholding automatically (BallPosition.java)
    public static final int FLAG_POSITION = 20;
    // Fired when a flag is dropped (FlagDropped.java)
    public static final int FLAG_DROPPED = 21;
    // Fired when a flag is claimed (FlagClaimed.java)
    public static final int FLAG_CLAIMED = 22;
    // Fired when the status of a turf flag is changed (TurfFlagUpdate.java)
    public static final int TURF_FLAG_UPDATE = 23;
    // Fired when someone attaches or detaches (TurretEvent.java)
    public static final int TURRET_EVENT = 24;
    // Fired whenever the bot receives a new banner (PlayerBanner.java)
    public static final int PLAYER_BANNER = 25;
    // Fired whenever the bot receives notice that King of the Hill game is reset
    public static final int KOTH_RESET = 26;

    private boolean[] array;

    /**
        Creates a new instance of EventRequester, initializing it with an array
        to store which events are being requested, and declining all by default.
    */
    public EventRequester() {
        array = new boolean[TOTAL_NUMBER];
        declineAll();
    }

    /**
        Enable event requesting for the event type specified.
        @param packetType Event type (use static declarations defined in EventRequester)
    */
    public void request( int packetType ) {
        array[packetType] = true;
    }

    /**
        Disable event requesting for the event type specified.
        @param packetType Event type (use static declarations defined in EventRequester)
    */
    public void decline( int packetType ) {
        array[packetType] = false;
    }

    /**
        Enable event requesting for all event types.
    */
    public void requestAll() {
        for( int i = 0; i < array.length; i++ ) {
            array[i] = true;
        }
    }

    /**
        Disable event requesting for all event types.
    */
    public void declineAll() {
        for( int i = 0; i < array.length; i++ ) {
            array[i] = false;
        }
    }

    /**
        Check if a specific event type is being requested.
        @param packetType Event type (use static declarations defined in EventRequester)
        @return True if the event is being requested
    */
    public boolean check( int packetType ) {
        return array[packetType];
    }

    /**
        Set event requesting for a particular event type.
        @param packetType Event type (use static declarations defined in EventRequester)
        @param value True to request; false to decline
    */
    public void set( int packetType, boolean value ) {
        array[packetType] = value;
    }
}
