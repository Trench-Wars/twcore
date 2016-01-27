package twcore.bots.multibot.bship;

import twcore.core.BotAction;
import twcore.core.util.Tools;
import java.sql.*;

/**
    Represents a player in the main battleship game and stores statistics
    related to the game in progress.

    @author D1st0rt
    @version 07.01.13
*/
public class BSPlayer
{
    /** Player's name */
    private String name;

    /** Player's frequency */
    private int freq;

    /** Player's current ship */
    public byte ship;

    /** Capital ship kills */
    public short cskills;

    /** Turret kills */
    public short tkills;

    /** Plane kills */
    public short pkills;

    /** Capital ship kills */
    public short deaths;

    /** Lives left (capships) */
    public short lives;

    /** Times attached to a capital ship (turret/plane) */
    public short takeoffs;

    /** Times attached to (capships) */
    public short attaches;

    /** Ships played by this player */
    public boolean[] ships;

    /** Aggregate rating */
    public int rating;

    /** Used to prevent being added to a team when inappropriate */
    public boolean locked;

    /** This player's playerId in the mysql database */
    public int sqlId;

    private static final String[] fields = { "name" };

    /**
        Creates a new instance of BSPlayer
        @param name the Player's name
    */
    public BSPlayer(String name, int freq)
    {
        this.name = name;
        this.freq = freq;
        resetStats();

        BotAction m_botAction = BotAction.getBotAction();

        if(m_botAction.SQLisOperational())
        {
            name = Tools.addSlashesToString(name);

            try {
                ResultSet s = m_botAction.SQLQuery(bship.dbConn,
                                                   "select id from players where name=\'" + name + "\'");

                if (s.next())
                {
                    sqlId = s.getInt("id");
                }
                else
                {
                    m_botAction.SQLInsertInto(bship.dbConn, "players", fields,
                                              new String[] { name });

                    ResultSet r = m_botAction.SQLQuery(bship.dbConn, "SELECT MAX(id) AS sqlId FROM players");

                    if (r.next())
                        sqlId = r.getInt("sqlId");

                    m_botAction.SQLClose(r);
                }

                m_botAction.SQLClose(s);

            } catch (Exception e)
            {
                Tools.printStackTrace(e);
                sqlId = -1;
            }
        }

    }

    /**
        Returns the player's name as a string
        @return a string containing the player's name
    */
    public String toString()
    {
        return name;
    }

    /**
        Gets the team the player is on
        @return the player's frequency
    */
    public int getFreq()
    {
        return freq;
    }

    /**
        Sets all statistics for this player back to 0
    */
    public void resetStats()
    {
        ship = 0;
        deaths = 0;
        lives = 0;
        cskills = 0;
        tkills = 0;
        pkills = 0;
        takeoffs = 0;
        attaches = 0;
        rating = 0;
        sqlId = -1;
        ships = new boolean[8];
    }

    /**
        Gets a string of all ships played by this player. Displays the ship
        number in it's 0-7 index if it has been used.
    */
    public String shipsPlayed()
    {
        String s = "";

        for(int x = 0; x < ships.length; x++)
        {
            if(ships[x])
                s += (x + 1);
            else
                s += " ";
        }

        return s;
    }
}
