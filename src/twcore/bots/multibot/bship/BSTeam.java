package twcore.bots.multibot.bship;

import static twcore.bots.multibot.bship.bship.ALL;
import static twcore.bots.multibot.bship.bship.PLAYING;
import static twcore.bots.multibot.bship.bship.SPEC;

import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.game.Player;

/**
    Represents a team of players in the main battleship game. Manages individual
    BSPlayer objects and passes events to the players for stat tracking

    @author D1st0rt
    @version 06.06.21
*/
public class BSTeam
{
    /** The players on this team */
    private Vector<BSPlayer> players;

    /** Cached array of BSPlayer objects for this team */
    private BSPlayer[] plist;

    /** This team's frequency */
    private int freq;

    /** A cached ship count for this team */
    private byte[] shipCount;

    /** Whether the cache is up to date or not */
    private boolean changed;

    /** How many capital ships the team currently has */
    //private int capShipCount;

    /** How many times a capital ship has died */
    private int capShipDeaths;

    /** How many lives each player starts out with. */
    private int initialLives;

    /**
        Creates a new instance of BSTeam
        @param freq the team's frequency
    */
    public BSTeam(int freq, int lives)
    {
        this.freq = freq;
        players = new Vector<BSPlayer>();
        changed = true;
        shipCount = new byte[8];
        plist = null;
        //capShipCount = 0;
        capShipDeaths = 0;
        initialLives = lives;
    }

    /**
        Resets this team to completely empty, just as when it was instantiated
    */
    public void reset()
    {
        players.clear();
        changed = true;
        plist = null;
        shipCount = new byte[8];
        //      capShipCount = 0;
        capShipDeaths = 0;
    }

    /**
        Gets all players on this in a specific ship
        @param ship the desired ship, can also be ALL or PLAYING
        @return an array of all BSPlayers currently in that ship
    */
    public BSPlayer[] getPlayers(byte ship)
    {
        Vector<BSPlayer> bsplayers = new Vector<BSPlayer>();

        for(BSPlayer p : players)
        {
            if(ship == ALL || (ship == PLAYING && p.ship != SPEC) || (p.ship == ship) )
                bsplayers.add(p);
        }

        return bsplayers.toArray(new BSPlayer[bsplayers.size()]);
    }

    /**
        Removes a player from this team
        @param name the name of the player
    */
    public void removePlayer(String name)
    {
        BSPlayer p = getPlayer(name);

        if(p != null)
        {
            players.remove(p);
            changed = true;
            plist = null;
        }
    }

    /**
        Gets all of the players on this team
        @return an array of all BSPlayers currently on the team
    */
    public BSPlayer[] getPlayers()
    {
        if(plist == null || changed)
            plist = players.toArray(new BSPlayer[players.size()]);

        return plist;
    }

    /**
        Updates the ship of a player. If the player does not exist, they are created.
        @param name the name of the player
        @param ship the ship the player is in
    */
    public void setShip(String name, byte ship)
    {
        BSPlayer p = getPlayer(name);

        if(p == null)
        {
            p = new BSPlayer(name, freq);
            p.lives = (short)initialLives;
            players.add(p);
        }

        int pIndex = players.indexOf(p);

        p.ship = ship;

        if(p.ship != bship.SPEC)
            p.ships[ship - 1] = true;

        players.setElementAt(p, pIndex); //is this necessary? probably not, check later

        changed = true;
        plist = null;
    }

    /**
        Gets a player by name
        @param name the name of the desired player
        @return the BSPlayer object for the player with that name, or null if not found
    */
    public BSPlayer getPlayer(String name)
    {
        BSPlayer[] bsplayers = getPlayers();

        for(int x = 0; x < bsplayers.length; x++)
        {
            if(bsplayers[x].toString().equals(name))
                return bsplayers[x];
        }

        return null;
    }

    /**
        Event for notifying a BSPlayer that it has just died. Updates death count and
        rating and checks to see how many lives are left. Decrements lives, and
        if there are lives left, it returns true. If no lives remain, it returns false.
        @param name the name of the player that died
        @return how many lives the player has left
    */
    public int playerDeath(String name)
    {
        BSPlayer p = getPlayer(name);
        p.deaths++;

        switch(p.ship)
        {
        case bship.PLANE:

        //fall through
        case bship.GUN:

        //fall through
        case bship.CANNON:
            if(p.deaths % 2 == 0 && p.deaths > 0)
                p.rating--;

            break;

        default:
            p.rating -= 4;
            p.lives--;
            capShipDeaths++;
        }

        return p.lives;
    }

    /**
        Event for notifying a BSPlayer that it just killed somebody.
        Updates kill count and rating.
        @param name the player that just got a kill
        @param ship the ship of the player that DIED
    */
    public void playerKill(String name, byte ship)
    {
        BSPlayer p = getPlayer(name);

        switch(ship)
        {
        case bship.PLANE:
            p.pkills++;
            p.rating++;
            break;

        case bship.GUN:

        //fall through
        case bship.CANNON:
            p.tkills++;
            p.rating += 2;
            break;

        default:
            p.cskills++;
            p.rating += 5;
        }
    }

    /**
        Event for notifying both BSPlayers involved in an attach. Updates
        attach counts and ratings.
        @param attachee the player that got attached to
        @param attacher the player that attached
    */
    public void attach(String attachee, String attacher)
    {
        BSPlayer ship = getPlayer(attachee);
        BSPlayer turret = getPlayer(attacher);

        if(ship == null)
        {
            Player sp = BotAction.getBotAction().getPlayer(attachee);
            setShip(attachee, sp.getShipType());
            ship = getPlayer(attachee);
        }

        if(turret == null)
        {
            Player tp = BotAction.getBotAction().getPlayer(attacher);
            setShip(attacher, tp.getShipType());
            turret = getPlayer(attachee);
        }

        ship.attaches++;

        if(ship.attaches % 3 == 0 && ship.attaches > 0)
            ship.rating++;

        turret.takeoffs++;
    }

    /**
        Gets the number of players on this team in a particular ship
        @param ship the desired ship, can also be ALL or PLAYING
    */
    public int getShipCount(byte ship)
    {
        return getPlayers(ship).length;
    }

    /**
        Gets a cached array of the ship distribution of this team. If the ship
        distribution has changed since the last calculation, it recalculates.
        @return a byte[] containing the number of players in each ship by array index
    */
    public byte[] getShipCount()
    {
        if(changed)
        {
            //((Session)Thread.currentThread()).getBotAction().sendArenaMessage("Updated ship count");
            for(byte x = 1; x < 9; x++)
                shipCount[x - 1] = (byte)getShipCount(x);
        }

        changed = false;
        return shipCount;
    }

    /**
        Determines if this team is eliminated or not. A team is eliminated if they
        have no remaining capital ships.
        @return 0 if still in, 1 if no team lives left, 2 if no capships left
    */
    public int isOut(int maxTeamLives)
    {
        if(getCapShipDeaths() >= maxTeamLives)
            return 1;
        else if(getCapShipCount() < 1)
            return 2;
        else
            return 0;
    }

    /**
        Sets the locked status of a player. Locked status is used to keep a player from being
        added to another team or changed ships, if for example they attach to a wrong ship or lag out.
        @param name the name of the player
        @param locked whether the player is to be locked or unlocked
    */
    public void lockPlayer(String name, boolean locked)
    {
        BSPlayer p = getPlayer(name);

        if(p != null)
            p.locked = locked;

    }

    /**
        Gets the number of capital ships this team currently has in play.
        @return the number of capital ships
    */
    public int getCapShipCount()
    {
        byte[] ships = getShipCount();
        //capShipCount = ships[3] + ships[4] + ships[5] + ships[6] + ships[7];

        return ships[3] + ships[4] + ships[5] + ships[6] + ships[7];
    }

    /**
        Gets the number of times a capital ship on this team has died.
    */
    public int getCapShipDeaths()
    {
        return capShipDeaths;
    }
}

