package twcore.core.lag;

import twcore.core.BotAction;

/**
 * This abstract class sets up future PlayerInfo classes.  It ties the info
 * class to a specific player.  It contains an EventHandler as well as a method
 * to add EventListeners.  This is so that the events may be processed and the
 * appropriate player stats may be updated.  The class that is using this object
 * must pass all of the SubspaceEvents into the handleEvent method.  There is
 * also an abstract reset method that will be overridden to reset all of the
 * player stats.  When constructing this object, the user may wish to pass in
 * a FileWriter class so that recordEvent(String) may write to it.  This class
 * may not be instantiated on its own.
 *
 * @author Cpt.Guano!
 * @version 1.2, 20/12/03
 *
 * 17/12/03 - Added functionality to enable and disable logging the information
 *            by putting in getEnabled() and setEnabled(boolean) methods.
 * 20/12/03 - Added the method recordEvent(String) to record events to a logFile
 *            that can be passed into the constructor of the PlayerInfo.
 */

public abstract class PlayerInfo extends GameComponent {

    private String playerName;

    /**
     * This a constructor for the PlayerInfo class.  It takes in a botAction
     * and associates the class with a player.  It also takes in a FileWriter
     * class that is made ready to write to a file.  Derived classes of PlayerInfo
     * may use this logFile to record the events.  The constructor is protected so
     * as to make it so that this class may not be instantiated.
     *
     * @param botAction is the BotAction class of the bot.
     * @param playerName is the name of the player.
     * @param logFile is the logFile to write the events to.  If it is null then
     * no logFile will be used.
     */
    protected PlayerInfo(BotAction botAction, String playerName, LogWriter logFile) {
        super(botAction, logFile);
        this.playerName = playerName;
    }

    /**
     * This a constructor for the PlayerInfo class.  It takes in a botAction
     * and associates the class with a player.  The constructor is protected so as
     * to make it so that this class may not be instantiated.
     *
     * @param botAction is the BotAction class of the bot.
     * @param playerName is the name of the player.
     */
    protected PlayerInfo(BotAction botAction, String playerName) {
        this(botAction, playerName, null);
    }

    /**
     * This method gets the name of the player that the info is associated with.
     *
     * @return the playerName is returned.
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * This protected method checks to see if a certain players name matches that
     * of the player who belongs to the PlayerInfo.
     *
     * @param checkName is the name of the player to check.
     * @return true is returned if checkName matches playerName
     */
    protected boolean isThisPlayer(String checkName) {
        return playerName.equalsIgnoreCase(checkName);
    }

    /**
     * This method checks to see if a certain playerID refers to the same player
     * who belongs to the PlayerInfo.
     *
     * @param checkID is the playerID of the person to check.
     * @return true is returned if checkID refers to the same person as
     * playerName.
     */
    protected boolean isThisPlayer(int checkID) {
        return isThisPlayer(ba.getPlayerName(checkID));
    }

    /**
     * This method resets all the PlayerInfo.
     */
    public abstract void reset();
}