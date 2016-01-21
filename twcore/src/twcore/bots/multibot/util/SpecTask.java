package twcore.bots.multibot.util;

import twcore.core.game.Player;

/**
    SpecTask class for use with twbotspec2, it has to be a public class in order
    for the reflection in ItemCommand to work. Represents an individual rule for
    eliminating a player or group of players after a certain number of deaths.

    @author D1st0rt
    @version 06.07.08
*/
public class SpecTask
{
    // SpecTask priorities, higher number is higher priority
    private static final int NONE = -1, ALL = 0, FREQ = 1, SHIP = 2,
                             FREQSHIP = 3, PLAYER = 4;

    /** The number of deaths to spec players at. */
    public int deaths;
    /** The frequency (if applicable) to spec at the specified number of deaths. */
    public int freq;
    /** The ship (if applicable) to spec at the specified number of deaths. */
    public int ship;
    /** The player (if applicable) to spec at the specified number of deaths. */
    public Player player;

    /**
        Gets a string describing this task to be used in the task list and
        arena messages.
        @return a string representation of this task.
    */
    public String toString()
    {
        StringBuilder msg = new StringBuilder("Removing ");

        if(freq != -1)
        {
            msg.append("freq ");
            msg.append(freq);
            msg.append(" ");
        }

        if(ship != -1)
        {
            msg.append("ship ");
            msg.append(ship);
            msg.append(" ");
        }

        if(player != null)
        {
            msg.append("player ");
            msg.append(player);
            msg.append(" ");
        }

        if(msg.length() == 9)
        {
            msg.append("all players ");
        }

        msg.append("with ");
        msg.append(deaths);
        msg.append(" deaths.");

        return msg.toString();
    }

    /**
        Determines whether this task applies to a provided player.
        @param f the player's frequency
        @param s the player's ship type
        @param pID the player's player ID
        @return the priority at which this task applies (larger number indicates
               a higher priority)
    */
    public int isApplicable(int f, int s, int pID)
    {
        // spec player
        if(player != null && player.getPlayerID() == pID)
            return PLAYER;

        // spec certain ships in freq
        if(freq != -1 && ship != -1 && ship == s && freq == f)
            return FREQSHIP;

        // spec ship
        if(ship != -1 && ship == s)
            return SHIP;

        // spec freq
        if(freq != -1 && freq == f)
            return FREQ;

        // spec all
        if(freq == -1 && ship == -1 && player == null)
            return ALL;

        // not applicable
        return NONE;
    }
}