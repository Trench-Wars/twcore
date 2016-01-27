package twcore.bots.multibot.util;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

public class utilshiplimit extends MultiUtil
{

    public TreeMap<Integer, Integer> shipLimits = new TreeMap<Integer, Integer>();
    public static final int UNLIMITED = -1;
    public static final int MAXSHIP = 9;
    public static final int MINSHIP = 1;
    public static final int SPEC = 0;

    /**
        Initializes initial shipLimits.
    */
    public void init()    {
        for (int i = MINSHIP; i < MAXSHIP; i++) {
            setLimit(i, UNLIMITED);
        }
    }
    /**
        Gets the ship limit for the specified ship.
        @param ship ship to retrieve the limit for
        @return limit for ship
    */
    private int getLimit(int ship) {
        if (!shipLimits.containsKey(ship)) {
            // for some reason this should arise!
            return -1;
        }
        else {
            return shipLimits.get(ship);
        }
    }
    /**
        Sets the ship's limit to limit
        @param ship ship to set a limit on
        @param limit limit to set for the ship
    */
    private void setLimit(int ship, int limit) {
        shipLimits.put(ship, limit);
    }

    /**
        Requests events.
    */

    public void requestEvents( ModuleEventRequester events ) {
        events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(this, EventRequester.FREQUENCY_CHANGE );
    }

    /**
        Puts a player in a new LEGAL ship, if one does not
        exist it puts the player in spec.
        Added instead of getValidShip.
        @param player Player who's ship needs to be changed.
    */
    public void putPlayerInNewShip(Player player) {
        int freq = player.getFrequency();
        int shipTypeOnFreq;
        int limit = 0;

        for (int i = MINSHIP; i < MAXSHIP; i++) {
            shipTypeOnFreq = getShipTypeOnFreq(i, freq);
            limit = getLimit(i);

            if((shipTypeOnFreq > limit && limit != UNLIMITED) || limit == 0)
            {   // This ship is illegal!
            }
            else
            {
                // Puts them in the first non-illegal ship
                m_botAction.setShip(player.getPlayerID(), i);
                return;
            }
        }

        // No legal ships are legal, spec the player...
        m_botAction.specWithoutLock(player.getPlayerID());
        m_botAction.sendSmartPrivateMessage(player.getPlayerName(), "Ship limiting is enabled in this arena. All of the ships are taken at this time.");
        return;
    }

    /**
        Turns all limits off.
    */

    public void doLimitsOffCmd()
    {
        for(int index = MINSHIP; index < MAXSHIP; index++) {
            setLimit(index, UNLIMITED);
        }

        m_botAction.sendArenaMessage("Ship limiting has been disabled.");
    }

    /**
        Lists all limits set.

        @param sender is the user of the bot.
    */

    public void doListLimitsCmd(String sender)
    {
        int limit;
        String limitString;

        for(int index = MINSHIP; index < MAXSHIP; index++)
        {
            limit = getLimit(index);

            if(limit == UNLIMITED)
                limitString = "Off";
            else
                limitString = Integer.toString(limit);

            m_botAction.sendSmartPrivateMessage(sender, Tools.shipName(index) + "'s ship limit is " + limitString + ".");
        }
    }

    /**
        Sets ship limits

        @param argString is an argument string to be tokenized and
        translated.
    */

    public void doLimitCmd(String argString)
    {
        StringTokenizer argTokens = new StringTokenizer(argString);
        String limitString;
        int ship;
        int limit;

        if(argTokens.countTokens() != 2)
            throw new IllegalArgumentException("Please use the following format: !Limit <Ship> <Limit>");

        try
        {
            ship = Integer.parseInt(argTokens.nextToken());

            if(ship < MINSHIP || ship > MAXSHIP)
                throw new IllegalArgumentException("Invalid ship number.");

            limitString = argTokens.nextToken();

            if(limitString.equals("off"))
            {
                limit = UNLIMITED;
                m_botAction.sendArenaMessage("Ship limiting for " + Tools.shipName(ship) + "'s has been disabled.");
            }
            else
            {
                limit = Integer.parseInt(limitString);

                if(limit == 0)
                {
                    m_botAction.sendArenaMessage("The " + Tools.shipName(ship) + " has been disabled.");
                } else {
                    m_botAction.sendArenaMessage(Tools.shipName(ship) + " is now limited to " + limit + " per frequency.");

                }

            }

            setLimit(ship, limit);
            checkPlayers();
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException("Please use the following format: !Limit <Ship> <Limit>");
        }
    }

    /**
        Enforces NEW limit rules.
    */
    public void checkPlayers() {
        Iterator<Player> p = m_botAction.getPlayingPlayerIterator();
        Player im;

        while(p.hasNext()) {
            im = p.next();
            checkLimit(im.getPlayerID());
        }
    }
    /**
        Checks the player for ship restrictions.

        @param playerID
    */
    private void checkLimit(int playerID)
    {
        Player player = m_botAction.getPlayer(playerID);
        byte ship = player.getShipType();
        int freq = player.getFrequency();
        int shipTypeOnFreq;
        int limit;

        if(ship != SPEC)
        {
            shipTypeOnFreq = getShipTypeOnFreq(ship, freq);
            limit = getLimit(ship);

            if((shipTypeOnFreq > limit && limit != UNLIMITED) || limit == 0)
            {
                if(limit == 0) {
                    m_botAction.sendSmartPrivateMessage(player.getPlayerName(), "The " + Tools.shipName(ship) + " has been disabled in this arena.");
                } else {
                    m_botAction.sendSmartPrivateMessage(player.getPlayerName(), "The maximum number of " + Tools.shipName(ship) + "'s have been reached for frequency " + freq + ".");
                }

                putPlayerInNewShip(player);
            }
        }
    }

    /**
        Returns the number of a ship type on a specific freq.

        @param ship is the ship to be counted.
        @param freq is the freq to check.
        @return the number of specified ships on the given freq.
    */
    private int getShipTypeOnFreq(int ship, int freq)
    {
        int shipCount = 0;
        Player player = null;

        Iterator<Player> iterator = m_botAction.getPlayingPlayerIterator();

        while(iterator.hasNext())
        {
            player = iterator.next();

            if(player.getShipType() == ship && player.getFrequency() == freq)
                shipCount++;
        }

        return shipCount;
    }

    /**
        Handles freq ship changes.
    */

    /**
        Handles freq ship changes.
    */

    public void handleEvent(FrequencyChange event)
    {
        checkLimit(event.getPlayerID());
    }

    /**
        Handles ship changes.
    */

    public void handleEvent(FrequencyShipChange event)
    {
        checkLimit(event.getPlayerID());
    }

    /**
        Handles all messages.
    */

    public void handleEvent(Message event)
    {
        int senderID = event.getPlayerID();
        String sender = m_botAction.getPlayerName(senderID);
        String message = event.getMessage();

        if(event.getMessageType() == Message.PRIVATE_MESSAGE && m_opList.isER(sender))
            handleCommand(sender, message);
    }

    /**
        Handles all ER+ commands

        @param sender is the user of the bot.
        @param message is the command.
    */

    public void handleCommand(String sender, String message)
    {
        String command = message.toLowerCase();

        try
        {
            if(command.equals("!limitsoff"))
                doLimitsOffCmd();

            if(command.equals("!limits") || command.equals("!listlimits"))
                doListLimitsCmd(sender);

            if(command.startsWith("!limit "))
                doLimitCmd(message.substring(7));
        }
        catch(Exception e)
        {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }

    /**
        Returns the help messages
    */

    public String[] getHelpMessages()
    {
        String[] message =
        {
            "!LimitsOff                                     -- Turns all of the ship limits off.",
            "!Limits                                        -- Displays the current ship limits.",
            "!Limit <Ship> <Limit>                          -- Limits the number of <Ship> to <Limit> per freq.",
            "!Limit <Ship> Off                              -- Turns limiting off for <Ship>."
        };

        return message;
    }
}

