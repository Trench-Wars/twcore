/*
 * twbotrevenge.java
 *
 * Created on April 6, 2005, 17:40
 *
 */

package twcore.bots.multibot.revenge;

import static twcore.core.EventRequester.FREQUENCY_SHIP_CHANGE;
import static twcore.core.EventRequester.PLAYER_DEATH;
import static twcore.core.EventRequester.PLAYER_LEFT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;

/**
 * This class provides the functionality for a sort of half-breed hunt/deathmatch.
 * It is to be used when hosting a game in ?go revenge.
 *
 * @author  Stultus
 */
public class revenge extends MultiModule
{
    private static final int ANY_SHIP = 0;
    private static final String[] SHIP_LIST =
    {
        "None",
        "Warbirds",
        "Javelins",
        "Spiders",
        "Leviathans",
        "Terriers",
        "Weasels",
        "Lancasters",
        "Sharks"
    };

    private boolean isStarting = false;
    private boolean inProgress = false;
    private int normalReward = 5;
    private int alreadyKilledReward = 2;
    private int revengeReward = 7;
    private int normalPenalty = 0;
    private int revengePenalty = 7;
    private int timeLimit = 10;
    private int shipType = ANY_SHIP;
    private HashMap <String,RevengePlayer>playerMap;

    /** Creates an instance of the module. */
    public void init()
    {
        playerMap = new HashMap<String,RevengePlayer>(30);
    }

    public void requestEvents(ModuleEventRequester events) {
        events.request(this, PLAYER_DEATH);
        events.request(this, PLAYER_LEFT);
        events.request(this, FREQUENCY_SHIP_CHANGE);
    }

    /**
     * Checks the type of incoming message, and passes (local) PMs to the
     * handleCommand method.
     */
    public void handleEvent( Message event )
    {
        String message = event.getMessage().toLowerCase();
        if ( event.getMessageType() == Message.PRIVATE_MESSAGE )
        {
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            handleCommand( name, message );
        }
    }

    /**
     * Watches for players entering mid-game.
     * If the host manually adds someone, this event will fire and create a new
     * RevengePlayer instance for them, and stick it into the playerMap.
     */
    public void handleEvent( FrequencyShipChange event )
    {
        if ( inProgress )
        {
            String playerName = m_botAction.getPlayerName(event.getPlayerID());
            if ( !playerMap.containsKey( playerName ) )
            {
                playerMap.put(playerName, new RevengePlayer( playerName,
                        getPlayerCount() ) );
            }
        }
    }

    /** Removes players from the list if they leave. */
    public void handleEvent( PlayerLeft event )
    {
        if ( inProgress )
        {
            String playerName = m_botAction.getPlayerName( event.getPlayerID() );
            if ( playerMap.containsKey ( playerName ) )
            {
                playerMap.remove( playerName );
            }
        }
    }

    /** Handles point scoring, based upon the involved players' kill histories. */
    public void handleEvent( PlayerDeath event )
    {
        if ( !inProgress )
            return;
        
        String killerName = m_botAction.getPlayerName( event.getKillerID() );
        String killedName = m_botAction.getPlayerName( event.getKilleeID() );

        if ( killerName == null || killedName == null)
            return;
        RevengePlayer killer = playerMap.get( killerName );
        RevengePlayer killed = playerMap.get( killedName );

        if( killer == null || killed == null )
            return;

        if ( killed.hasKilled( killerName ) ) {
            killer.addPoints( revengeReward );
            m_botAction.sendPrivateMessage( killerName, "You have gained "
                    + revengeReward + " for killing " + killedName + ". (Total: "
                    + killer.getScore() + ")" );

            killed.removePoints( revengePenalty );
            m_botAction.sendPrivateMessage( killedName, "You have lost "
                    + revengePenalty + " points for being killed by " + killerName
                    + ". (Total: " + killed.getScore() + ")" );
        } else {
            if ( !killer.hasKilled( killedName ) ) {
                killer.addPoints( normalReward );
                m_botAction.sendPrivateMessage( killerName, "You have "
                        + "gained " + normalReward + " points for killing "
                        + killedName + ". (Total: " + killer.getScore() + ")");
                killer.addKilled(killedName );
            } else {
                killer.addPoints( alreadyKilledReward );
                m_botAction.sendPrivateMessage( killerName, "You have "
                        + "gained " + alreadyKilledReward + " points for killing "
                        + killedName + ". (Total: " + killer.getScore() + ")" );
            }
            killed.removePoints( normalPenalty );
            m_botAction.sendPrivateMessage( killedName, "You have lost "
                    + normalPenalty + " points for being killed by " + killerName
                    + ". (Total: " + killed.getScore() + ")" );
        }
        // These steps should be unnecessary due to how Java always passes references... 
        //playerMap.remove( killerName );
        //playerMap.remove( killedName );
        //playerMap.put( killerName, killer);
        //playerMap.put( killedName, killed );
    }

    /**
     * This method checks the access level of private messagers, and handles their
     * commands accordingly.
     *
     * @param name  the name of the player sending the command
     * @param message  the command being sent
     */
    public void handleCommand( String name, String message )
    {
        if ( opList.isER( name ) )
        {
            if ( message.startsWith( "!nreward" ) )
                setNormalReward( name, message );
            else if ( message.startsWith( "!rreward" ) )
                setRevengeReward( name, message );
            else if ( message.startsWith( "!areward" ) )
                setAlreadyKilledReward( name, message );
            else if ( message.startsWith( "!npenalty" ) )
                setNormalPenalty( name, message );
            else if ( message.startsWith( "!rpenalty" ) )
                setRevengePenalty( name, message );
            else if ( message.startsWith( "!setshiptype" ) )
                setShipTypeLimit( name, message );
            else if ( message.startsWith( "!start" ) )
                startRevenge( name, message );
            else if ( message.equals( "!stop" ) )
                stopRevenge( name );
            else if ( message.equals( "!settings" ) )
                showSettings( name );
        }
        if ( message.equals( "!score" ) )
        {
            tellScore( name );
        }
    }

    /**
     * This method starts a new game of revenge.
     *
     * @param name  the name of the player who sent a !start command
     * @param message  the rest of their message (to be parsed for the game time limit)
     */
    public void startRevenge( String name, String message )
    {
        if ( !isStarting && !inProgress )
        {
            isStarting = true;

            timeLimit = explodeToInt( message, 1, " " );

            /*
             * Check whether the player even supplied an argument for time limit.
             */

            if ( timeLimit == -1 )
            {
                timeLimit = 10;
                m_botAction.sendPrivateMessage( name, "That is not properly "
                        + "formatted. Defaulting to 10 minutes." );
            }

            m_botAction.toggleLocked();
            m_botAction.sendArenaMessage( "Get ready, game starts in 10 "
                    + "seconds.", 2 );

            TimerTask prepare = new TimerTask()
            {
                public void run()
                {
                    if ( shipType != ANY_SHIP )
                    {
                        m_botAction.changeAllShips( shipType );
                    }
                    m_botAction.createRandomTeams( 1 );

                    /*
                     * Populate the map of players with new RevengePlayer instances.
                     */

                    int playerCount = getPlayerCount();
                    Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
                    while ( it.hasNext() )
                    {
                        Player p = it.next();
                        playerMap.put( p.getPlayerName(), new RevengePlayer(
                                p.getPlayerName(), playerCount ) );
                    }
                }
            };

            TimerTask startIt = new TimerTask()
            {
                public void run()
                {
                    isStarting = false;
                    inProgress = true;
                    m_botAction.scoreResetAll();
                    m_botAction.setTimer( timeLimit );
                    m_botAction.sendArenaMessage( "Go go go!", 104 );
                }
            };

            TimerTask endIt = new TimerTask()
            {
                public void run()
                {
                    endRevenge();
                }
            };

            m_botAction.scheduleTask( prepare, 9000 );
            m_botAction.scheduleTask( startIt, 10000 );
            m_botAction.scheduleTask( endIt, ( timeLimit * 60 * 1000 ) + 11000 );
        }
        else
        {
            m_botAction.sendPrivateMessage( name, "There is already a game "
                    + "in progress." );
        }
    }

    /**
     * This method ends a game of revenge.
     * It is called when a game is played cleanly through, rather than being nuked.
     */
    public void endRevenge()
    {
        inProgress = false;
        m_botAction.sendArenaMessage( "MVP: " + getMVP() + "!", 7 );
        m_botAction.toggleLocked();
        playerMap.clear();
    }

    /**
     * This method stops a game of revenge.
     * If there is no game in progress, the host will be notified that they've
     * attempted to kill something which doesn't exist.
     *
     * @param name  the name of the player who sent a !stop command
     */
    public void stopRevenge( String name )
    {
        if ( isStarting || inProgress )
        {
            isStarting = false;
            inProgress = false;
            playerMap.clear();
            cancel();
            m_botAction.sendArenaMessage( "This game has been brutally killed "
                    + "by " + name + ".", 13 );
            m_botAction.toggleLocked();
        }
        else
        {
            m_botAction.sendPrivateMessage( name, "There was no game in "
                    + "progress." );
        }
    }

    /**
     * Sets the point value for `normal kills'.
     * This is the amount rewarded to players when they've killed a player that they
     * have not previously. (Triggered by an !nreward command.)
     *
     * @param name  the name of the player who sent the command
     * @param message  the command being sent (to be parsed for args)
     */
    public void setNormalReward( String name, String message )
    {
        normalReward = explodeToInt( message, 1, " " );
        if ( normalReward == -1 )
        {
            normalReward = 7;
            m_botAction.sendPrivateMessage( name, "That is not properly "
                    + "formatted. Defaulting to 7 points." );
        }
        else
        {
            m_botAction.sendPrivateMessage( name, "Normal reward set." );
        }
    }

    /**
     * Sets the point value for `revenge kills'.
     * This is the amount rewarded to players who kill a player that has previously
     * killed them. (Triggered by an !rreward command.)
     *
     * @param name  the name of the player who sent the command
     * @param message  the command being sent (to be parsed for args)
     */
    public void setRevengeReward( String name, String message )
    {
        revengeReward = explodeToInt( message, 1, " " );
        if ( revengeReward == -1 )
        {
            revengeReward = 7;
            m_botAction.sendPrivateMessage( name, "That is not properly "
                    + "formatted. Defaulting to 7 points." );
        }
        else
        {
            m_botAction.sendPrivateMessage( name, "Revenge kill reward set." );
        }
    }

    /**
     * Sets the point value for `repeat kills'.
     * This is the amount rewarded to those who kill a player they have already killed.
     * (Triggered by an !areward command.)
     *
     * @param name  the name of the player who sent the command
     * @param message  the command being sent (to be parsed for args)
     */
    public void setAlreadyKilledReward( String name, String message )
    {
        alreadyKilledReward = explodeToInt( message, 1, " " );
        if ( alreadyKilledReward == -1 )
        {
            alreadyKilledReward = 2;
            m_botAction.sendPrivateMessage( name, "That is not properly "
                    + "formatted. Defaulting to 2 points." );
        }
        else
        {
            m_botAction.sendPrivateMessage( name, "Already-killed reward set." );
        }
    }

    /**
     * Sets the point penalty for `normal deaths'.
     * This is the amount deducted from a player's score when they have been killed
     * by someone who has not previously killed them. (Triggered by an !npenalty
     * command.)
     *
     * @param name  the name of the player who sent the command
     * @param message  the command being sent (to be parsed for args)
     */
    public void setNormalPenalty( String name, String message )
    {
        normalPenalty = explodeToInt( message, 1, " " );
        if ( normalPenalty == -1 )
        {
            normalPenalty = 0;
            m_botAction.sendPrivateMessage( name, "That is not properly "
                    + "formatted. Defaulting to 0 points." );
        }
        else
        {
            m_botAction.sendPrivateMessage( name, "Normal penalty set." );
        }
    }

    /**
     * Sets the point penalty for `revenge deaths'.
     * This is the amount deducted from a player's score when they have been killed
     * by a player that they have previously killed. (Triggered by an !rpenalty
     * command.)
     *
     * @param name  the name of the player who sent the command
     * @param message  the command being sent (to be parsed for args)
     */
    public void setRevengePenalty( String name, String message )
    {
        revengePenalty = explodeToInt( message, 1, " " );
        if ( revengePenalty == -1 )
        {
            revengePenalty = 7;
            m_botAction.sendPrivateMessage( name, "That is not properly "
                    + " formatted. Defaulting to 7 points." );
        }
        else
        {
            m_botAction.sendPrivateMessage( name, "Revenge death penalty set." );
        }
    }

    /**
     * This method sets the type of ship to be used in the game.
     * Players will be locked into this ship for the duration of the game. (Triggered
     * by a !setship command.)
     *
     * @param name  the name of the player who sent the command
     * @param message  the command being sent (to be parsed for args)
     */
    public void setShipTypeLimit( String name, String message )
    {
        shipType = explodeToInt( message, 1, " " );
        if ( shipType == -1 )
        {
            shipType = ANY_SHIP;
            m_botAction.sendPrivateMessage( name, "That is not properly "
                    + "formatted. Defaulting to any ship." );
        }
        else if ( shipType < 0 || shipType > 8 )
        {
            m_botAction.sendPrivateMessage( name, "That is not a valid ship "
                    + "number. Defaulting to 'any' (0)." );
        }
        else
        {
            m_botAction.sendPrivateMessage( name, "Ship type limit set." );
        }
    }

    /**
     * This method shows the current game settings.
     * The sender of a !settings command will be PM'd with the current options.
     *
     * @param name  the name of the player who sent the command
     */
    public void showSettings( String name )
    {
        String[] settings =
        {
                "Time limit:             " + timeLimit + "minutes",
                "Normal kill reward:     " + normalReward + "pts",
                "Revenge kill reward:    " + revengeReward + "pts",
                "Already-killed reward:  " + alreadyKilledReward + "pts",
                "Normal penalty:         " + normalPenalty + "pts",
                "Revenge penalty:        " + revengePenalty + "pts",
                "Ship type limit:        " + SHIP_LIST[shipType]
        };
        m_botAction.privateMessageSpam( name, settings );
    }

    /**
     * This method shows a player their score.
     * The sender of a !score command will be PM'd with their current point total (or
     * reprimanded for being stupid, if they're not in the game).
     *
     * @param name  the name of the player who sent the command
     */
    public void tellScore( String name )
    {
        if ( playerMap.containsKey( name ) )
        {
            RevengePlayer tempPlayer = playerMap.get( name );
            m_botAction.sendPrivateMessage( name, "You currently have "
                    + tempPlayer.getScore() + " points." );
        }
        else
        {
            m_botAction.sendPrivateMessage( name, "You are not in the game!" );
        }
    }

    /**
     * This method returns an array of help messages, in the event that its parent
     * TWBot receives a !help for this module.
     *
     * @return  a String array of help messages, for the parent TWBot instance
     */
    public String[] getModHelpMessage()
    {
        String[] help =
        {
                "!nreward <#>                - Sets the point value of normal kills.",
                "!rreward <#>                - Sets the point value of 'revenge' kills.",
                "!areward <#>                - Sets the point value of 'rekills'.",
                "!npenalty <#>               - Sets the penalty value for normal deaths.",
                "!rpenalty <#>               - Sets the penalty value for 'revenge' deaths.",
                "!setshiptype <#>            - Allows players to use only this ship.",
                "!start <minutes>            - Starts a game of revenge.",
                "!stop                       - Kills a game in progress.",
                "!settings                   - Shows current settings."
        };
        return help;
    }

    public boolean isUnloadable() {
        return true;
    }

    /** This method cancels any pending TimerTasks, should the game be !stopped. */
    public void cancel()
    {
        m_botAction.cancelTasks();
    }

    /**
     * This method takes a string, splits it into a list according to the provided
     * separatng character, and returns an integer representation of the given index.
     *
     * @param message  the string to be split
     * @param index  the index of the list item to be returned
     * @param separator  the deliniating character
     *
     * @return  an int representation of the given index, after splitting (or -1 in
     * case of an exception)
     */
    private int explodeToInt( String message, int index, String separator )
    {
        try
        {
            return Integer.parseInt( message.split( separator )[index]);
        }
        catch ( Exception e )
        {
            return -1;
        }
    }

    /**
     * This method returns the number of non-spectating players.
     *
     * @return  the number of non-spectating players
     */
    private int getPlayerCount()
    {
        int freq = -1;
        int playerCount = 1;
        Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
        if (it == null)
            return 0;
        while ( it.hasNext() )
        {
            Player p = it.next();
            if ( freq == -1 )
                freq = p.getFrequency();
            if ( p.getFrequency() != freq )
                playerCount++;
        }
        return playerCount;
    }

    /**
     * This method returns the name of the highest-scoring player in the arena.
     *
     * @return  the name of the player with the most points in the arena
     */
    private String getMVP()
    {
        int bestScore = 0;
        String playerName = "";
        Iterator<Player> it = m_botAction.getPlayerIterator();
        while ( it.hasNext() )
        {
            Player p = it.next();
            if ( p.getScore() > bestScore )
            {
                bestScore = p.getScore();
                playerName = p.getPlayerName();
            }
        }
        return playerName;
    }
}

/**
 * This class provides a representation of a ?go revenge player.
 *
 * @author  Stultus
 */
class RevengePlayer
{
    private String playerName;
    private ArrayList <String>playersKilled;
    private int score;

    /**
     * Creates a new instance of RevengePlayer.
     *
     * @param name  the name of the player to be represented
     * @param totalPlayers  the current number of players in the arena
     */
    public RevengePlayer( String name, int totalPlayers )
    {
        playerName = name;
        playersKilled = new ArrayList<String>( totalPlayers - 1 );
        score = 0;
    }

    /**
     * This method increases the player's score by a specified amount.
     *
     * @param points  the number of points to add
     */
    public void addPoints( int points )
    {
        score += points;
    }

    /**
     * This method decreases the player's score by a specified amount.
     *
     * @param points  the number of points to remove
     */
    public void removePoints( int points )
    {
        score -= points;
    }

    /**
     * This method returns the player's current score.
     *
     * @return  the player's current score
     */
    public int getScore()
    {
        return score;
    }

    /**
     * This method adds a name to the player's `already killed' list.
     *
     * @param name  the name of the player to be added
     */
    public void addKilled( String name )
    {
        playersKilled.add( name );
    }

    /**
     * This method checks whether the player has already killed the given person.
     *
     * @param name  the name of the person who may or may not have been killed
     * @return  true if so, false if not
     */
    public boolean hasKilled( String name )
    {
        if ( playersKilled.contains( name ) ) return true;
        return false;
    }

    /**
     * This method returns the name of the player associated with this instance.
     *
     * @return  the name of the player associated with this instance
     */
    public String getName()
    {
        return playerName;
    }
}

