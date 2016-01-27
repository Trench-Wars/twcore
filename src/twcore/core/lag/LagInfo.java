package twcore.core.lag;

import java.util.StringTokenizer;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.events.Message;

/**
    This class retrieves detailed lag information about a certain player.  The
    following stats are obtained:

    Current, Average, Low, and High Ping
    C2S, S2C, and S2CWeapons packetloss
    Number and percent of C2S and S2C Slow packets.
    The standard deviation and number of spikes.

    This class obtains this information by parsing *info, and *tinfo.  The spike
    information is for the last 160 seconds of play.  If the player has not been
    in the game for at least 3 minutes, then the standard deviation and number of
    spikes will both be 0.

    When figuring out what the spike is, this class will check if consecutive
    tinfo values are over a certain number of milliseconds.  This number can be
    specified in the constructor of the class.

    The *info and *tinfo commands are synchronized so it is safe to put
    this class into different threads on the same bot.  Because it is
    synchronized, other * commands will not be placed inbetween these 3 commands.

    Updated 08/10/03 by Cpt.Guano!
    - Parsed all of the ping / ploss information out of *info instead of *lag.

    Updated 09/01/11 by WingZero
    - Moved this and all relevant classes to a new home

    @author Cpt.Guano!
    @version 1.1, 08/10/03

*/
public class LagInfo extends PlayerInfo {
    public static final int MIN_TINFO_TIME = 3;
    public static final int DEFAULT_SPIKE_SIZE = 300;

    private PlayerLagState state;
    private int currentPing;
    private int averagePing;
    private int lowPing;
    private int highPing;
    private int sessionTime;
    private int c2SSlowPackets;
    private int s2CSlowPackets;
    private int spikeSize;
    private int numSpikes;
    private double s2C;
    private double c2S;
    private double s2CWeapons;
    private double spikeSD;
    private double spikeMean;
    private double c2SSlowPercent;
    private double s2CSlowPercent;

    private Vector<Integer> tinfoValues;

    /**
        This method initializes a PlayerLag class.  It tracks the lag of a player.
        The object is created using a spike size that is passed as a parameter.

        @param botAction is the botAction of the bot.
        @param playerName is the name of the player whose lag info this is.
        @param spikeSize is the minimum ping jump required to make a spike.
    */
    public LagInfo(BotAction botAction, String playerName, int spikeSize) {
        super(botAction, playerName);

        if (spikeSize < 0)
            throw new IllegalArgumentException("ERROR: Invalid spike size.  Must be a positive number.");

        state = new PlayerLagState();
        tinfoValues = new Vector<Integer>();
        this.spikeSize = spikeSize;
        addEventListener(new MessageListener());
    }

    /**
        This method initializes a PlayerLag class.  It tracks the lag of a player.
        The object is created using the default spike size.

        @param botAction is the botAction of the bot.
        @param playerName is the name of the player whose lag info this is.
    */
    public LagInfo(BotAction botAction, String playerName) {
        this(botAction, playerName, DEFAULT_SPIKE_SIZE);
    }

    /**
        This method returns if the playerLag class has valid lag values to return.

        @return true is returned if the object has gotten the lag.
    */
    public boolean gotLag() {
        return state.isCurrentState(PlayerLagState.GOT_LAG_STATE);
    }

    /**
        This method returns the players current ping.

        @return the current ping is returned.
    */
    public int getCurrentPing() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return currentPing;
    }

    /**
        This method returns the players average ping.

        @return the average ping is returned.
    */
    public int getAveragePing() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return averagePing;
    }

    /**
        This method returns the players low ping.

        @return the low ping is returned.
    */
    public int getLowPing() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return lowPing;
    }

    /**
        This method returns the players high ping.

        @return the high ping is returned.
    */
    public int getHighPing() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return highPing;
    }

    /**
        This method returns the players S2C.

        @return the S2C is returned.
    */
    public double getS2C() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return s2C;
    }

    /**
        This method returns the players S2C.

        @return the C2S is returned.
    */
    public double getC2S() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return c2S;
    }

    /**
        This method returns the players S2CWeapons.

        @return the S2CWeapons is returned.
    */
    public double getS2CWeapons() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return s2CWeapons;
    }

    /**
        This method returns the number of C2S slow packets that they have had
        recently.

        @return the number of C2S slow packets is returned.
    */

    public int getC2SSlowPackets() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return c2SSlowPackets;
    }

    /**
        This method returns the number of S2C slow packets that they have had
        recently.

        @return the number of S2C slow packets is returned.
    */
    public int getS2CSlowPackets() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return s2CSlowPackets;
    }

    /**
        This method returns the percent of C2S slow packets that they have had
        recently.

        @return the percent of C2S slow packets is returned.
    */
    public double getC2SSlowPercent() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return c2SSlowPercent;
    }

    /**
        This method returns the percent of S2C slow packets that they have had
        recently.

        @return the percent of S2C slow packets is returned.
    */
    public double getS2CSlowPercent() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return s2CSlowPercent;
    }

    /**
        This method returns the standard deviation of the players tinfo.  The value
        that it returns is in miliseconds.

        @return the standard deviation of the spike is returned.
    */
    public double getSpikeSD() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return spikeSD;
    }

    /**
        This method returns the spike size (The number of ms jump that is
        considered a spike).

        @return the spikeSize is returned.
    */
    public int getSpikeSize() {
        return spikeSize;
    }

    /**
        This method returns the number of spikes that the player has recently
        experienced.  A spike is defined as a ping jump over spikeSize.

        @return the number of spikes recently made by the player.
    */

    public int getNumSpikes() {
        if (!gotLag())
            throw new InvalidStateException("Lag information for " + getPlayerName() + " has not yet been retrieved.");

        return numSpikes;
    }

    /**
        This sets the PlayerLagInfo class to the state of not having checked the
        players lag.
    */

    public void reset() {
        state.setCurrentState(PlayerLagState.NO_LAG_STATE);
    }

    /**
        This method updates the players lag information.  It is synchronized so
        that the *info and *tinfo commands are not interrupted.
    */

    public synchronized void updateLag() {
        if (isEnabled()) {
            String playerName = getPlayerName();

            if (ba.getPlayer(playerName) == null) {
                state.setCurrentState(PlayerLagState.LAG_ERROR_STATE);
                throw new IllegalArgumentException("Could not find " + playerName + " in the arena.");
            }

            state.setCurrentState(PlayerLagState.REQUESTING_LAG_STATE);
            ba.sendUnfilteredPrivateMessage(playerName, "*info");
            ba.sendUnfilteredPrivateMessage(playerName, "*tinfo");
        }
    }

    /**
        This helper function checks to see if the server has started to process
        the lag request.  It does this by checking the *info and seeing if the
        field "TypedName:" cooresponds to the players name.  Warning:  This could
        cause errors with names longer than 19 characters.

        @param message is the arena message to parse.
    */
    private void handleLagRequest(String message) {
        if (message.startsWith("IP:")) {
            String typedName = getInfoString(message, "TypedName:");

            if (typedName.length() > 19)
                typedName = typedName.substring(0, 19);

            if (isThisPlayer(typedName))
                state.setCurrentState(PlayerLagState.GETTING_INFO_STATE);
        }
    }

    /**
        This helper method handles an arena message.  It will distribute it to
        the appropriate helper functions based on the PlayerLags curent state.

        @param message is the arena message to handle.
    */
    private void handleLagUpdateMessage(String message) {
        try {
            if (state.isCurrentState(PlayerLagState.GETTING_INFO_STATE))
                doGetInfo(message);
            else if (state.isCurrentState(PlayerLagState.UPDATING_TINFO_STATE))
                doGetTinfo(message);
        } catch (RuntimeException e) {
            state.setCurrentState(PlayerLagState.LAG_ERROR_STATE);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
        This helper method gets all of the lag information out of the *info
        command.  This includes: Average, Current, High and Low pings, Slow packet
        information and packetloss information.  It also saves the session time.

        @param message is the message to handle.
    */
    private void doGetInfo(String message) {
        if (message.startsWith("Ping:"))
            doGetPing(message);

        if (message.startsWith("LOSS:"))
            doGetPloss(message);

        if (message.startsWith("C2S CURRENT:")) {
            c2SSlowPackets = getPacketInfo(message);
            c2SSlowPercent = getPercentInfo(message);
        }

        if (message.startsWith("S2C CURRENT:")) {
            s2CSlowPackets = getPacketInfo(message);
            s2CSlowPercent = getPercentInfo(message);
        }

        if (message.startsWith("TIME:")) {
            sessionTime = parseSessionTime(getInfoString(message, "Session:"));
            state.setCurrentState(PlayerLagState.REQUESTING_TINFO_STATE);
        }
    }

    /**
        This helper method returns the number of slow packets from the info string.

        @param message is the info message to parse.
        @param packetType is the type of packet to return.
        @return the number of slow packets that the player experienced is returned.
    */
    private int getPacketInfo(String message) {
        try {
            String packetString = getInfoString(message, "Slow:");
            int endIndex = packetString.indexOf(' ');

            return Integer.parseInt(packetString.substring(0, endIndex));
        } catch (RuntimeException e) {
            throw new RuntimeException("ERROR: Could not parse get the number of slow packets.");
        }
    }

    /**
        This helper method returns the percent of slow packets from the info
        string.

        @param message is the info message to parse.
        @return the percent of slow packets that the player experienced is
        returned.
    */
    private double getPercentInfo(String message) {
        try {
            String packetString = getInfoString(message, "Slow:");
            int beginIndex = packetString.lastIndexOf(' ');
            int endIndex = packetString.lastIndexOf("%");

            return Double.parseDouble(packetString.substring(beginIndex, endIndex));
        } catch (RuntimeException e) {
            throw new RuntimeException("ERROR: Could not parse the percent of slow packets.");
        }
    }

    /**
        This helper method parses the session time string from the *info command
        and returns its value in minutes.

        @param sessionInfo is the string that contains the session info.
        @return the length of the session in minutes is returned.
    */
    private int parseSessionTime(String sessionInfo) {
        try {
            StringTokenizer sessionInfoTokens = new StringTokenizer(sessionInfo, " :");
            int minutes = 0;

            minutes = 60 * Integer.parseInt(sessionInfoTokens.nextToken());
            minutes = minutes + Integer.parseInt(sessionInfoTokens.nextToken());
            return minutes;
        } catch (NumberFormatException e) {
            throw new NumberFormatException("ERROR: Could not parse the session time.");
        }
    }

    /**
        This helper method gets all of the ping information out of a *info command.

        @param message is the *info message to handle.
    */
    private void doGetPing(String message) {
        currentPing = parsePing(message, "Ping:");
        averagePing = parsePing(message, "AvePing:");
        lowPing = parsePing(message, "LowPing:");
        highPing = parsePing(message, "HighPing:");
    }

    /**
        This helper method gets all of the packetloss information out of a *info
        command.

        @param message is the *info message to handle.
    */
    private void doGetPloss(String message) {
        s2C = parsePloss(message, "S2C:");
        c2S = parsePloss(message, "C2S:");
        s2CWeapons = parsePloss(message, "S2CWeapons:");
    }

    /**
        This helper method parses a ping string and returns the ping value.  A
        ping string is of the form: "XXX ms".

        @param message is the *lag message to handle.
        @param pingType is the name of the ping type to parse.  It should be either
        Current, Average, Low, High.
    */
    private int parsePing(String message, String pingType) {
        try {
            String pingString = getInfoString(message, pingType);
            int msIndex = pingString.lastIndexOf("ms");

            return Integer.parseInt(pingString.substring(0, msIndex));
        } catch (RuntimeException e) {
            throw new RuntimeException("ERROR: Could not parse the string: " + pingType + ".");
        }
    }

    /**
        This helper method parses a ploss string and returns the ping value.  A
        ploss string is of the form: "XX.XX%".

        @param message is the *lag message to handle.
        @param plossType is the name of the ploss type to parse.  It should be
        either S2C, C2S, S2CWeapons.
    */
    private double parsePloss(String message, String plossType) {
        try {
            String plossString = getInfoString(message, plossType);
            int percentIndex = plossString.lastIndexOf("%");
            String percentString = plossString.substring(0, percentIndex);
            int sign = 1;

            if (percentString.indexOf('-') != -1)
                sign = -1;

            return sign * Double.parseDouble(removeChar(percentString, '-'));

        } catch (NumberFormatException e) {
            throw new NumberFormatException("ERROR: Could not parse the string: " + plossType + ".");
        }
    }

    /**
        This method removes a character from a string and returns it.

        @param string is the string from which the character is going to be
        removed.
        @param character is the character to remove.
        @return the string with character removed is returned.
    */
    private String removeChar(String string, char character) {
        StringBuffer stringBuffer = new StringBuffer(string);
        int removeIndex;

        for (;;) {
            removeIndex = stringBuffer.indexOf(Character.toString(character));

            if (removeIndex == -1)
                break;

            stringBuffer.deleteCharAt(removeIndex);
        }

        return stringBuffer.toString();
    }

    /**
        This helper method gets the tinfo information and gets it in the form of
        the mean and the standard deviation.

        @param message is the tinfo message to handle.
    */
    private void doGetTinfo(String message) {
        if (state.isCurrentState(PlayerLagState.REQUESTING_TINFO_STATE))
            handleTinfoRequest(message);
        else if (state.isCurrentState(PlayerLagState.GETTING_TINFO_STATE)) {
            getTinfoValue(message);

            if (tinfoValues.size() >= 32) {
                spikeMean = calcSpikeMean();
                spikeSD = calcSpikeSD();
                numSpikes = calcNumSpikes();
                state.setCurrentState(PlayerLagState.GOT_LAG_STATE);
            }
        }
    }

    /**
        This helper method checks to see if the tinfo information is being sent.
        If there are not enoguh tinfos to make an accurate analysis then it sets
        the spikeSD and the spikeMean to 0.

        @param message is the tinfo message to handle.
    */
    private void handleTinfoRequest(String message) {
        if (message.equals("ServerTime    UserTime        Diff")) {
            spikeSD = 0;
            spikeMean = 0;
            numSpikes = 0;
            tinfoValues.clear();

            if (sessionTime < 3)
                state.setCurrentState(PlayerLagState.GOT_LAG_STATE);
            else
                state.setCurrentState(PlayerLagState.GETTING_TINFO_STATE);
        }
    }

    /**
        This method gets a tinfo value from a tinfo message.  It checks to see that
        serverTime - userTime = diff before recording it.

        @param message is the tinfo message to handle.
    */
    private void getTinfoValue(String message) {
        try {
            StringTokenizer tinfoTokens = new StringTokenizer(message);

            if (tinfoTokens.countTokens() == 3) {
                int serverTime = Integer.parseInt(tinfoTokens.nextToken());
                int userTime = Integer.parseInt(tinfoTokens.nextToken());
                int diff = Integer.parseInt(tinfoTokens.nextToken());

                if (serverTime - userTime == diff)
                    tinfoValues.add(new Integer(diff));
            }
        } catch (RuntimeException e) {}
    }

    /**
        This method calculates the spike mean from the tinfo values that were
        recorded.  Mean is calculated in the following way:

        the sum of the tinfo values / the number of tinfo values

        @return the mean is returned.
    */
    private double calcSpikeMean() {
        Integer tinfoValue;
        double mean = 0;

        for (int index = 0; index < tinfoValues.size(); index++) {
            tinfoValue = tinfoValues.get(index);
            mean = mean + tinfoValue.intValue();
        }

        return mean / tinfoValues.size();
    }

    /**
        This method calculates the spike standard deviation from the tinfo values
        that were recorded.  Standard Deviation is calculated in the following way:

        square root of ((sum of (tinfo value - mean) ^ 2) / number of tinfo values)

        @return the standard deviation is returned.
    */
    private double calcSpikeSD() {
        Integer tinfoValue;
        double sd = 0;
        double delta;

        for (int index = 0; index < tinfoValues.size(); index++) {
            tinfoValue = tinfoValues.get(index);
            delta = tinfoValue.intValue() - spikeMean;
            sd = sd + delta * delta;
        }

        return Math.sqrt(sd / tinfoValues.size()) * 10;
    }

    /**
        This method counts the number of spikes that the player has experienced in
        the past 160 seconds.  A spike is defined as a ping jump of + / -
        spikeSize ms.

        @return the number of spikes is returned.
    */
    private int calcNumSpikes() {
        int spikeCount = 0;
        Integer tinfoValue = tinfoValues.get(0);
        int lastTinfo = tinfoValue.intValue();
        int thisTinfo;

        for (int index = 1; index < tinfoValues.size(); index++) {
            tinfoValue = tinfoValues.get(index);
            thisTinfo = tinfoValue.intValue();

            if (Math.abs(lastTinfo - thisTinfo) * 10 > spikeSize)
                spikeCount++;

            lastTinfo = thisTinfo;
        }

        return spikeCount;

    }

    /**
        This helper method gets a value from a *info string.  The value
        that it returns is based on the infoName that is passed in.

        @param message is the info string to handle.
        @param infoName is the name of the value to get.
        @return the value specified by infoName is returned.
    */
    private String getInfoString(String message, String infoName) {
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

    /**
        This method returns the index of the first occurance in the string where
        the character does not match targetChar.  If the whole string is full of
        targetChar then -1 is returned.

        @param string is the message to search.
        @param targetChar is the character to skip.
        @param beginIndex is where in the string to start searching.
        @return the index of the first occurance where the character isnt equal to
        targetChar is returned.
    */
    private int findFirstNotOf(String string, char targetChar, int beginIndex) {
        char charAt;

        for (int index = beginIndex + 1; index < string.length(); index++) {
            charAt = string.charAt(index);

            if (charAt != targetChar)
                return index;
        }

        return -1;
    }

    /**
        This class is the PlayerLag state tree.  It is used in the PlayerLag class
        to monitor what the class is currently doing.  The tree looks as follows:

                                  "Root"
                       /      /            \        \
                   /         |              |          \
            "No Lag"  "Requesting Lag"  "Updating Lag"  "Got Lag"
            |                            /          \
        "Lag Error"                     /              \
                                     /                  \
                             "Getting info"      "Updating tinfo"
                                                     /       \
                                                    /         \
                                       "Requesting tinfo"  "Getting tinfo"

        The tree starts off in the "No Lag" state.
    */
    private class PlayerLagState extends State {
        public static final String NO_LAG_STATE = "No Lag";
        public static final String REQUESTING_LAG_STATE = "Requesting Lag";
        public static final String UPDATING_LAG_STATE = "Updating Lag";
        public static final String GOT_LAG_STATE = "Got Lag";

        public static final String LAG_ERROR_STATE = "Lag Error";

        public static final String GETTING_INFO_STATE = "Getting info";
        public static final String UPDATING_TINFO_STATE = "Updating tinfo";

        public static final String REQUESTING_TINFO_STATE = "Requesting tinfo";
        public static final String GETTING_TINFO_STATE = "Getting tinfo";

        /**
            This method initializes the PlayerLagState tree so that it looks like the
            picture above.  This method also puts the tree in the "No Lag" state.a
        */
        public PlayerLagState() {
            addState(NO_LAG_STATE);
            addState(REQUESTING_LAG_STATE);
            addState(UPDATING_LAG_STATE);
            addState(GOT_LAG_STATE);

            addState(NO_LAG_STATE, LAG_ERROR_STATE);

            addState(UPDATING_LAG_STATE, GETTING_INFO_STATE);
            addState(UPDATING_LAG_STATE, UPDATING_TINFO_STATE);

            addState(UPDATING_TINFO_STATE, REQUESTING_TINFO_STATE);
            addState(UPDATING_TINFO_STATE, GETTING_TINFO_STATE);

            setCurrentState(NO_LAG_STATE);
        }
    }

    private class MessageListener extends EventListener {

        /**
            This method handles message events that are picked up by the bot.

            @param event is the message event to handle.
        */
        public void handleEvent(Message event) {
            int messageType = event.getMessageType();
            String message = event.getMessage();

            if (messageType == Message.ARENA_MESSAGE) {
                if (state.isCurrentState(PlayerLagState.REQUESTING_LAG_STATE))
                    handleLagRequest(message);

                if (state.isCurrentState(PlayerLagState.UPDATING_LAG_STATE))
                    handleLagUpdateMessage(message);
            }
        }
    }
}
