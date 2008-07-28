package twcore.bots.roboreplacement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.WeaponFired;
import twcore.core.events.WatchDamage;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * A replacement for RoboRef in elim, spawned as needed.
 *
 * Terrible indentation fixed. -dugwyler
 */
public class roboreplacement extends SubspaceBot
{
    TimerTask zonerTrue;   //sets the variable zone to true every 15 minutes
    TimerTask shipVote;    //timertask to end the voting on ship for the game
    TimerTask deathVoting; //timertask to end the voting on deaths for the game

    boolean zoneOn = true;      // True if the zoning feature is turned on
    boolean zone = true;        // True if the bot should zone after this game
    int gameStatus = 0;         // 0=No game running; 1=Ship voting; 2=Death voting;
                                // 3=Waiting until game start; 4=Playing; 5=End round, waiting for next vote
    final int STATUS_WAITINGFORPLAYERS = 0;
    final int STATUS_SHIPVOTING = 1;
    final int STATUS_DEATHVOTING = 2;
    final int STATUS_AFTERVOTE = 3;
    final int STATUS_PLAYING = 4;
    final int STATUS_ENDGAME = 5;
    boolean locked = false;     // True if arena should be locked.  (Used for insurance.)

    HashSet<Integer> players = new HashSet<Integer>(); //contains ID's of all the players that started the game
    HashMap<Integer, Integer> deaths = new HashMap<Integer, Integer>();  //key is ID of the person, value is the person's number of deaths
    HashMap<Integer, Integer> kills = new HashMap<Integer, Integer>();   //key is ID of the person, value is the person's number of kills
    HashMap<Integer, Integer> lastDeaths = new HashMap<Integer, Integer>(); // XXX: What's this used for?

    HashMap<Integer, Integer> votes = new HashMap<Integer, Integer>();   //key is ID of the person that voted, value is the person's vote
    ArrayList<Integer> allowedShips = new ArrayList<Integer>();          // Ships allowed in this type of elim

    ConcurrentHashMap<Integer, Integer> bulletsFired = new ConcurrentHashMap<Integer, Integer>();
    ConcurrentHashMap<Integer, Integer> bulletsHit   = new ConcurrentHashMap<Integer, Integer>();

    Vector<String> topTen; //Vector of people with the top ten ratings

    BotSettings m_botSettings; //BotSettings object for the .cfg file

    int elimShip;         //Current elim ship
    int elimDeaths = 10;  //Current deaths
    int voteTally[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; //array for votes
    String mvp = "";
    int mvpKills = 0;
    int numVotes = 0;            // Keeps track of number of votes, for reporting purposes
    int defaultShip = 0;

    String arena;                // name of the arena the bot is in
    String mySQLHost = "local";  // sql stuff
    String ships[] = {"", "Warbird", "Javelin", "Spider", "Leviathan", "Terrier", "Weasel", "Lancaster", "Shark", "Any Ship"}; //array list of ship type names


    /**
     * Start 'er up.
     */
    public roboreplacement(BotAction botAction) {
        super(botAction);
        EventRequester events = m_botAction.getEventRequester();
        events.request(EventRequester.PLAYER_DEATH);
        events.request(EventRequester.MESSAGE);
        events.request(EventRequester.LOGGED_ON);
        events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(EventRequester.PLAYER_ENTERED);
        events.request(EventRequester.PLAYER_LEFT);
        events.request(EventRequester.WEAPON_FIRED);
        events.request(EventRequester.WATCH_DAMAGE);
        events.request(EventRequester.ARENA_JOINED);
        m_botSettings = m_botAction.getBotSettings();
    }


    /**
     * True only if a game is not presently going.
     */
    public boolean isIdle() {
        return gameStatus != 4;
    }


    /**
     * Initial actions.
     */
    public void handleEvent(LoggedOn event) {
        String botNumber = m_botSettings.getString(m_botAction.getBotName());
        arena = m_botSettings.getString("Arena" + botNumber);
        String[] pieces = m_botSettings.getString("AllowedShips" + botNumber).split(",");
        allowedShips.add(new Integer(pieces[0]));   // Placeholder
        defaultShip = m_botSettings.getInt("DefaultShip" + botNumber);
        for(int k = 0;k < pieces.length;k++)
        {
            try {
                allowedShips.add(new Integer(pieces[k]));
            } catch(Exception e) {}
        }
        if(m_botSettings.getInt("CheckY" + botNumber) > 0)
            setupYPosCheck(m_botSettings.getInt("CheckY" + botNumber));
        m_botAction.joinArena(arena);
        setupZoner();
        m_botAction.scheduleTaskAtFixedRate(zonerTrue, 15 * 60 * 1000, 15 * 60 * 1000);
    }


    public void handleEvent( ArenaJoined event ) {
        m_botAction.setPlayerPositionUpdating(200);
        m_botAction.sendArenaMessage( "Elimination Replacement Bot loaded. -" + m_botAction.getBotName(), Tools.Sound.BEEP3 );
        m_botAction.specAll();
    }


    /**
     * Called when a player enters the arena.
     *
     * Safety precaution that will spec the person and remove him/her from the
     * list of players if he/she accidently got added upon entry.
     */
    public void handleEvent(PlayerEntered event) {
        int pid = event.getPlayerID();
        m_botAction.sendPrivateMessage( pid, getStatusMsg() );
        players.remove( pid );
        m_botAction.sendUnfilteredPrivateMessage( pid, "*watchdamage" );
    }


    public String getStatusMsg() {
        switch( gameStatus ) {
            case 1: return "Welcome to Elimination.  We are currently voting on the type of ship.";
            case 2: return "Welcome to Elimination.  We are currently voting on the number of deaths.";
            case 3: return "Welcome to Elimination.  We are about to start " + ships[elimShip] +
                           " elim to " + elimDeaths + ".  Get in now if you wish to play.";
            case 4: return "Welcome to Elimination.  We are currently in a " + ships[elimShip] +
                           " elim to " + elimDeaths + ".  " + players.size() + " players left.";
            case 5: return "Welcome to Elimination.  We are currently waiting for ship voting to begin.";
            default: return "Welcome to Elimination.  A game will start when 2 players enter.";
        }
    }

    /**
     * Ends the game if a person spec's and only one player is left.
     */
    public void handleEvent(FrequencyShipChange event) {
        if(!players.contains(new Integer(event.getPlayerID())) && event.getShipType() != 0)
            players.add(new Integer(event.getPlayerID()));
        if(players.contains(new Integer(event.getPlayerID())) && event.getShipType() == 0)
            players.remove(new Integer(event.getPlayerID()));
        if(players.size() <= 1 ) {
            if( gameStatus == STATUS_PLAYING && players.size() == 1 ) {
                Iterator<Integer> i = players.iterator();
                int pID = i.next().intValue();
                doGameOver( pID );
            } else {
                checkPreGamePlayerStatus();
            }
        } else if(players.size() >= 2 && gameStatus == STATUS_WAITINGFORPLAYERS || gameStatus == STATUS_ENDGAME ) {
            gameStatus = STATUS_SHIPVOTING;
            startVoting();
        } else {
            checkPreGamePlayerStatus();
        }
    }

    /**
     * Poorly-written command handler.
     */
    public void handleEvent(Message event) {
        String message = event.getMessage();
        int pid = event.getPlayerID();
        String name = m_botAction.getPlayerName(pid);

        if( event.getMessageType() == Message.ARENA_MESSAGE ) {
            if( message.equals( "Arena UNLOCKED" ) && locked == true )
                m_botAction.toggleLocked();
            else if( message.equals( "Arena LOCKED" ) && locked == false )
                m_botAction.toggleLocked();
        }

        if( event.getMessageType() == Message.PUBLIC_MESSAGE &&
                gameStatus == STATUS_DEATHVOTING || gameStatus == STATUS_SHIPVOTING ) {
            if( Tools.isAllDigits(message) ) {
                if( votes.containsKey( pid ) ) {
                    votes.remove( pid );
                    m_botAction.sendPrivateMessage(name, "Previous vote removed." );
                }

                try {
                    int vote = Integer.parseInt(message);
                    if( gameStatus == STATUS_DEATHVOTING ) {
                        if( vote < 11 ) {
                            votes.put( pid, vote );
                            m_botAction.sendPrivateMessage(name, "Vote added for " + vote + " deaths." );
                        } else {
                            m_botAction.sendPrivateMessage(name, "Number of deaths must be less than 10." );
                        }
                    } else if( gameStatus == STATUS_SHIPVOTING ) {
                        if( vote > 0 && vote < allowedShips.size() ) { //adds the vote to the votes HashMap
                            votes.put( pid, vote );
                            m_botAction.sendPrivateMessage(name, "Vote added for " + ships[allowedShips.get(vote)] + " elim." );
                        } else {
                            m_botAction.sendPrivateMessage(name, "That is not an allowed number." );
                        }
                    }
                } catch(Exception e) {}
            }
        }

        if( event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE )
        	handleCommands(name, message);
    }


    /**
     * Simple command handler.
     * @param name
     * @param message
     */
    public void handleCommands( String name, String message ) {

        if(m_botAction.getOperatorList().isER(name))
        {
            if(message.toLowerCase().startsWith("!die"))
            {
                m_botAction.sendSmartPrivateMessage(name, "Logging off...");
                m_botAction.die();
            }
            if(message.toLowerCase().startsWith("!zoneoff") && zoneOn) {
                if( !zoneOn ) {
                    m_botAction.sendSmartPrivateMessage(name, "Zoners already deactivated.");
                    return;
                }
                try {
                    m_botAction.cancelTask(zonerTrue);
                }catch (Exception e) {
                    m_botAction.sendSmartPrivateMessage(name, "Problem turning off zoners.");
                }
                m_botAction.sendSmartPrivateMessage(name, "Zoners deactivated.");
                zoneOn = false;
            }
            if(message.toLowerCase().startsWith("!zoneon") ) {
                if( zoneOn ) {
                    m_botAction.sendSmartPrivateMessage(name, "Zoners already active.");
                    return;
                }

                try {
                    m_botAction.cancelTask(zonerTrue);
                    setupZoner();
                    m_botAction.scheduleTaskAtFixedRate(zonerTrue, 15 * 60 * 1000, 15 * 60 * 1000);
                }catch (Exception e) {
                    m_botAction.sendSmartPrivateMessage(name, "Problem turning on zoners.");
                }
                m_botAction.sendSmartPrivateMessage(name, "Zoners active.");
                zoneOn = true;
            }
            if(message.toLowerCase().startsWith("!help"))
            {
                m_botAction.sendSmartPrivateMessage(name, "!die          - Kills bot");
                m_botAction.sendSmartPrivateMessage(name, "!zoneoff      - Stops the bot from zoning before a game");
                m_botAction.sendSmartPrivateMessage(name, "!zoneon       - Allows the bot to zone before a game");
                m_botAction.sendSmartPrivateMessage(name, "!status       - Displays current status of the game");
            }
        }

        // **** Public commands
        if(message.toLowerCase().startsWith("!score")) {
            Player p = m_botAction.getPlayer(name);
            if( p != null )
                showScoreCard( p.getPlayerID(), false );
        }
        if(message.toLowerCase().startsWith("!stats "))
        {
            String pieces[] = message.split(" ", 2);
            m_botAction.sendSmartPrivateMessage(name, getPlayerStats(pieces[1]));
        }
        else if(message.toLowerCase().startsWith("!stats"))
            m_botAction.sendSmartPrivateMessage(name, getPlayerStats(name));
        else if(message.toLowerCase().startsWith("!topten"))
            topTen(name);
        else if(message.toLowerCase().startsWith("!status")) {
            m_botAction.sendPrivateMessage( name, getStatusMsg() );
        }
        else if(message.toLowerCase().startsWith("!help"))
        {
            //m_botAction.sendSmartPrivateMessage(name, "!stats        - Gets your stats.");
            //m_botAction.sendSmartPrivateMessage(name, "!stats <name> - Gets <name>'s stats");
            //m_botAction.sendSmartPrivateMessage(name, "!topten       - Gets list of top ten players");
            m_botAction.sendPrivateMessage(name, "!score        - Shows your current scorecard" );
            m_botAction.sendPrivateMessage(name, "!status       - Displays current status of the game");
            m_botAction.sendPrivateMessage(name, "!help         - Displays this message");
        }
    }


    /**
     * Handles speccing and some win conditions.
     */
    public void handleEvent(PlayerDeath event)
    {
        if( gameStatus == STATUS_PLAYING ) {
            int killeeID = event.getKilleeID();
            int killerID = event.getKillerID();

            Player p = m_botAction.getPlayer(killeeID);
            if( p == null )
                return;

            if(kills.containsKey(new Integer(killerID))) {
                int winz = kills.get(new Integer(killerID)).intValue() + 1;
                kills.put(new Integer(killerID), new Integer(winz));
            }
            else
                kills.put(new Integer(killerID), new Integer(1));
            if(deaths.containsKey(new Integer(killeeID))) {
                int lossez = deaths.get(new Integer(killeeID)).intValue() + 1;
                deaths.put(new Integer(killeeID), new Integer(lossez));
            }
            else  //adds the killee to the deaths hashmap if they are not there yet
                deaths.put(new Integer(killeeID), new Integer(1));

            if(!kills.containsKey(new Integer(killeeID)))         //adds killee to kills hashmap to make sure the bot doesn't mess up on db entry
                kills.put(new Integer(killeeID), new Integer(0));
            if(!deaths.containsKey(new Integer(killerID)))        //adds killer to deaths hashmap to make sure the bot doesn't mess up on db entry
                deaths.put(new Integer(killerID), new Integer(0));

            if(p.getLosses() >= elimDeaths) {
                m_botAction.sendArenaMessage(m_botAction.getPlayerName(killeeID) + " is out. " + p.getWins() + " wins, " + p.getLosses() + " deaths.");
                m_botAction.specWithoutLock(killeeID);
                players.remove(new Integer(killeeID));
                showScoreCard( killeeID, true );

                if( p.getWins() >= mvpKills && p.getWins() > 1 ) {
                    boolean realMVP = true;
                    for( Player p2 : m_botAction.getPlayingPlayers() ) {
                        if( p2.getWins() > p.getWins() ) {
                            realMVP = false;
                            break;
                        }
                    }
                    if( realMVP ) {
                        if( p.getWins() == mvpKills ) {
                            m_botAction.sendPrivateMessage(killeeID, "Though you lost, you're still tied for current MVP (with " + mvp + ") at " + mvpKills + " kills." );
                            mvp += " + " + p.getPlayerName();
                        } else {
                            mvp = p.getPlayerName();
                            mvpKills = p.getWins();
                            m_botAction.sendPrivateMessage(killeeID, "Though you lost, you're still the current MVP at " + mvpKills + " kills." );
                        }
                    }
                }
            }

            if(players.size() == 1 ) {
                doGameOver( killerID );
            }

            lastDeaths.put(new Integer(p.getPlayerID()), new Integer(((int)(System.currentTimeMillis() / 1000 % 30))));
        }
    }


    /**
     * Displays scorecard to player once they're done.
     * @param id
     * @param isFinal True if this is the final scorecard
     */
    public void showScoreCard( int id, boolean isFinal ) {
        Player p = m_botAction.getPlayer(id);
        if( p == null )
            return;
        /*
        Integer hitsobj = bulletsHit.get(id);
        int hits;
        if( hitsobj == null )
            hits = 0;
        else
            hits = hitsobj;
        Integer firedobj = bulletsFired.get(id);
        int fired;
        if( firedobj == null )
            fired = 0;
        else
            fired = firedobj;
        int misses = Math.max(0, fired - hits);

        float percentHits;
        if( hits > 0 && fired > 0 )
            percentHits = Math.max(100, ((float)Math.max(1, hits)) / ((float)Math.max(1, fired)));
        else
            percentHits = 0;
        */
        int kills = p.getWins();
        int deaths = p.getLosses();
        float ratio = ((float)Math.max(1, kills)) / ((float)Math.max(1, deaths));
        java.text.NumberFormat ratioFormat = java.text.NumberFormat.getNumberInstance();
        ratioFormat.setMaximumFractionDigits(2);
        String ratioString = ratioFormat.format(ratio) + ":1";
        // [Final Scorecard]  10-2  (5.0:1 ratio)    Hits: 10   Misses: 490   Accuracy: 90%
        m_botAction.sendPrivateMessage( id, "[" + (isFinal?"Final ":"Current ") + "Scorecard]  " + kills + "-" + deaths + "   (" + ratioString + ")" );
               //"Hits: " + hits + "   Misses: " + misses + "   Accuracy: " + (int)(percentHits * 100.0f) + "%" );
    }


    public void handleEvent(PlayerLeft event)
    {
        if(players.contains(new Integer(event.getPlayerID()))) //removes player from list of players
            players.remove(new Integer(event.getPlayerID()));

        //makes sure db entry goes smoothly by putting the person on both HashMaps if he/she isn't on them yet
        if(!deaths.containsKey(new Integer(event.getPlayerID())))
            deaths.put(new Integer(event.getPlayerID()), new Integer(0));
        if(!kills.containsKey(new Integer(event.getPlayerID())))
            kills.put(new Integer(event.getPlayerID()), new Integer(0));

        if(players.size() == 1 && gameStatus == STATUS_PLAYING )
        {
            Iterator<Integer> i = players.iterator();
            int pID = i.next().intValue();
            doGameOver( pID );
        }
        // Cancel game if for some reason nobody is left.
        if( players.size() == 0 && gameStatus == STATUS_PLAYING ) {
            m_botAction.sendArenaMessage("Game has been cancelled.  I win.", 1);
            locked = false;
            m_botAction.toggleLocked();
            gameStatus = STATUS_WAITINGFORPLAYERS;
        }
        m_botAction.sendUnfilteredPrivateMessage( event.getPlayerID(), "*watchdamage" );
        checkPreGamePlayerStatus();
    }


    /**
     * Counts number of hits.
     */
    public void handleEvent( WatchDamage event ) {
        int id = event.getAttacker();
        Integer hits = bulletsHit.get(id);
        if( hits == null )
            bulletsHit.put(id, 1);
        else
            bulletsHit.put(id, (hits+1));

    }

    /**
     * Counts (approximate) number of bullets fired.
     */
    public void handleEvent( WeaponFired event ) {
        int id = event.getPlayerID();
        Integer fired = bulletsFired.get(id);
        if( fired == null )
            bulletsFired.put(id, 1);
        else
            bulletsFired.put(id, (fired+1));
    }

    /**
     * Begins the next game.
     */
    public void nextgame() {
        m_botAction.sendUnfilteredPublicMessage("*scorereset"); //resets score...
        if( !checkPreGamePlayerStatus() )
            return;

        TimerTask startGame = new TimerTask() {
            public void run() {
                if( !checkPreGamePlayerStatus() )
                    return;

                Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
                while(i.hasNext()) {
                    Player p = i.next();
                    players.add(new Integer(p.getPlayerID()));
                }

                locked = true;
                m_botAction.toggleLocked();
                if(elimShip == 9) {  // Anyship elim?
                    Iterator<Player> it2 = m_botAction.getPlayingPlayerIterator();
                    while(it2.hasNext()) {
                        Player p = it2.next();
                        if(!allowedShips.contains(new Integer(p.getShipType()))) //sets person to default (first entered) elim ship type if they are in an illegal ship
                            m_botAction.sendUnfilteredPrivateMessage(p.getPlayerName(), "*setship " + defaultShip );
                    }
                }
                else //if it is not an anyship elim it sets everyone to the correct ship then starts
                    m_botAction.changeAllShips(elimShip);

                m_botAction.createRandomTeams(1);
                m_botAction.sendArenaMessage( ships[elimShip] + " Elim to " + elimDeaths + " has begun.  GO! GO! GO!!", 104);
                m_botAction.sendUnfilteredPublicMessage("*scorereset");
                lastDeaths.clear();
                bulletsFired.clear();
                bulletsHit.clear();
                mvp = "";
                mvpKills = 0;
                gameStatus = STATUS_PLAYING;
            }
        };
        m_botAction.scheduleTask(startGame, 30 * 1000);
    }


    public void startVoting()
    {
        if( !checkPreGamePlayerStatus() )
            return;

        if(zone == true) //sends zoner for the game if it has been at least 15 minutes since the last zoner
        {
            zone = false;
            m_botAction.sendZoneMessage("Next elimination match starting in ?go " + arena);
        }
        m_botAction.sendUnfilteredPublicMessage("*scorereset");

        //creates the message for the ship voting
        String tempShip = "Vote on ship: ";
        for(int k = 1;k < allowedShips.size();k++)
            tempShip += k + "=" + ships[allowedShips.get(k).intValue()] + "  ";
        m_botAction.sendArenaMessage(tempShip);

        shipVote = new TimerTask() {
            public void run() {
                if( !checkPreGamePlayerStatus() )
                    return;
                int shipNumInList = countVotes(false);
                if( shipNumInList < 0 || shipNumInList >= allowedShips.size() )
                    elimShip = defaultShip;
                else
                    elimShip = allowedShips.get( shipNumInList ).intValue();
                m_botAction.sendArenaMessage("It will be " + ships[elimShip] + " elim. " + (numVotes==-1 ? "(default)" : "(" + numVotes + " votes)") ); //announces the result of the vote and starts death voting
                m_botAction.sendArenaMessage("Vote on deaths (1-10)");
                gameStatus = STATUS_DEATHVOTING;
            }
        };
        m_botAction.scheduleTask(shipVote, 20 * 1000); //schedules the end of the vote

        deathVoting = new TimerTask() {
            public void run() {
                if( !checkPreGamePlayerStatus() )
                    return;
                gameStatus = STATUS_AFTERVOTE;
                elimDeaths = countVotes(true); //tallies the votes
                m_botAction.sendArenaMessage(ships[elimShip] + " elim to " + elimDeaths + "  " + (numVotes==-1 ? "(default)" : "(" + numVotes + " votes)"));
                m_botAction.sendArenaMessage("Enter if playing.  Game begins in 30 seconds ...");
                nextgame();
            }
        };
        m_botAction.scheduleTask(deathVoting, 40 * 1000); //schedules the end of the death vote and calls nextgame() to start the elim game
    }


    /**
     * Sets zone to true, forcing a zone at next round start.
     */
    void setupZoner() {
        zonerTrue = new TimerTask() {
            public void run() {
                zone = true;
            }
        };
    }


    /**
     * Counts votes for deaths.
     * @param votingForDeaths True if we're voting for deaths; false if for ships
     * @return
     */
    public int countVotes(boolean votingForDeaths) //counts the votes, if deathvotez is false it starts from 1, if it is true it starts from 10
    {
        for( Integer voter : votes.keySet() ) {
            int vote = votes.get( voter ).intValue();
            voteTally[vote]++;
        }

        int winner = -1;
        for(int k = 1; k < 11; k++) {
            if( voteTally[k] > 0 ) {
                if( winner == -1 ) {
                    winner = k;
                } else {
                    if(voteTally[k] >= voteTally[winner]) {
                        if( voteTally[k] == voteTally[winner]) {
                            // Flip a coin when votes are tied.
                            int rand = (int)Math.round(Math.random());
                            if( rand == 0 )
                                winner = k;
                        } else {
                            winner = k;
                        }
                    }
                }
            }
        }

        if( winner != -1 ) {
            numVotes = voteTally[winner];
        } else {
            if(votingForDeaths)
                winner = 10;
            numVotes = -1;
        }

        votes.clear();
        resetTally();
        return winner; // returns the winner of the tally
    }


    /**
     * Resets vote tally.
     *
     */
    public void resetTally() {
        for( int i=1; i<11; i++ )
            voteTally[i]=0;
    }


    /**
     * Game over; display winner stats.
     * @param winnerID
     */
    public void doGameOver( int winnerID ) {
        if( gameStatus != STATUS_PLAYING )
            return;
        Player p = m_botAction.getPlayer(winnerID);
        if( p == null ) {
            m_botAction.sendArenaMessage("GAME OVER: Winner ?!?", 5);
        } else {
            String squad = "";
            if(p.getSquadName().length()>0) {
                squad = " ["+p.getSquadName()+"]";
            }
            m_botAction.sendArenaMessage("GAME OVER: Winner " + p.getPlayerName() + squad, 5);
            saveGameStatsToDB( winnerID );
        }
        if( p.getWins() > mvpKills ) {
            mvp = p.getPlayerName();
            mvpKills = p.getWins();
        } else if( p.getWins() == mvpKills ) {
            mvp += ", " + p.getPlayerName();
        }
        if( !mvp.equals("") )
            m_botAction.sendArenaMessage( "MVP: " + mvp + " ... " + mvpKills + " kills" );
        showScoreCard( winnerID, true );
        mvp = "";
        mvpKills = 0;
        bulletsFired.clear();
        bulletsHit.clear();
        locked = false;
        m_botAction.toggleLocked();
        gameStatus = STATUS_ENDGAME;
    }


    /**
     * Uploads game stats to DB.
     * @param winnerID
     */
    public void saveGameStatsToDB(int winnerID) {
        /* Recordkeeping disabled for now.  -GD 7/1/08
        try {
            Set<Integer> set = deaths.keySet();
            Iterator<Integer> it = set.iterator();
            while(it.hasNext())
            {
                String username = m_botAction.getPlayerName(((Integer)it.next()).intValue());
                int losses = deaths.get(new Integer(m_botAction.getPlayerID(username))).intValue();
                int playerKills = kills.get(new Integer(m_botAction.getPlayerID(username))).intValue();
                int playerDeaths;
                if(m_botAction.getPlayerID(username) == winnerID)
                    playerDeaths = 1;
                else
                    playerDeaths = 0;


                ResultSet qryHasElimtwoRecord = m_botAction.SQLQuery(mySQLHost, "SELECT fcUserName, fnPlayed, fnWon, fnKills, fnDeaths FROM tblElimTwoStats"+num+" WHERE fcUserName = \"" + username+"\"");
                if(!qryHasElimtwoRecord.next()) //creates a new record for a person
                {
                    double rating1 = playerKills / losses;
                    if(rating1 > 1.0)
                        rating1 = 1.0;
                    double rating2 = playerDeaths / 1;
                    double rating = (rating1 + rating2) * 500;
                    m_botAction.SQLClose( m_botAction.SQLQuery( mySQLHost, "INSERT INTO tblElimTwoStats"+num+"(fcUserName, fnPlayed, fnWon, fnKills, fnDeaths, fnRating) VALUES (\""+username+"\",1,"+playerDeaths+","+playerKills+","+losses+","+rating+")") );
                }
                else //updates a persons record
                {
                    //double played = qryHasElimtwoRecord.getInt("fnPlayed") + 1.0;
                    double wines = qryHasElimtwoRecord.getInt("fnWon") + playerDeaths;
                    double killez = qryHasElimtwoRecord.getInt("fnKills") + playerKills;
                    double lossez = qryHasElimtwoRecord.getInt("fnDeaths") + losses;
                    double rating1 = killez / lossez;
                    if(rating1 > 1.0)
                        rating1 = 1.0;
                    double rating2 = wines / 1;
                    double rating = (rating1 + rating2) * 500;
                    m_botAction.SQLClose( m_botAction.SQLQuery( mySQLHost, "UPDATE tblElimTwoStats"+num+" SET fnPlayed = fnPlayed+1, fnWon = fnWon + "+playerDeaths+", fnKills = fnKills + "+playerKills+", fnDeaths = fnDeaths + "+ losses + ", fnRating = " +rating+ "  WHERE fcUserName = \"" + username+"\"") );
                    //tblElimTwoStats fnPlayed fnWon fnKills fnDeaths fnRating fcUserName
                }
                m_botAction.SQLClose( qryHasElimtwoRecord );
            }
        } catch(Exception e) {}
         */
    }


    public String getPlayerStats(String username) //returns a string of the user's stats
    {
        /* try{

            ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fnDeaths, fnWon, fnPlayed, fnKills, fnRating FROM tblElimTwoStats"+num+" WHERE fcUserName = \"" + username+"\"");
            if( result == null ) {
                if(result.next()) {
                	return username + "- Wins: "+ result.getInt("fnWon") + "  Games:" + result.getInt("fnPlayed") + ")  Kills: " +result.getInt("fnKills") + "  Deaths:" + result.getInt("fnDeaths") + "  Rating: " + result.getInt("fnRating");
            	} else {
                	return "There is no record of player " + username;
            	}
            } else {
            	return "There is no record of player " + username;
            }
        } catch (Exception e) {
            Tools.printStackTrace(e);
            return "Can't retrieve stats.";
        }*/
        return "Stats have been temporarily disabled.";
    }


    public void topTen(String name) //pm's the person the top ten people
    {
        if( true ) {
            m_botAction.sendSmartPrivateMessage(name, "Sorry, this feature is temporarily disabled.");
            return;
        }

        getTopTen();
        if(topTen.size() == 0)
            m_botAction.sendSmartPrivateMessage(name, "No one has qualified yet!");
        else
        {
            for(int i = 0; i < topTen.size(); i++)
                m_botAction.sendSmartPrivateMessage(name, (String)topTen.elementAt(i));
        }
    }


    public void getTopTen() //gets the top ten people and puts them in the vector topTen
    {
        topTen = new Vector<String>();
        /*try {
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT fcUserName, fnWon, fnPlayed, fnWon, fnDeaths, fnRating FROM tblElimTwoStats"+num+" ORDER BY fnRating DESC LIMIT 10");
            while(result.next())
                topTen.add(result.getString("fcUsername") + "Wins: " + result.getInt("fnWon") +"  Games: " + result.getInt("fnPlayed") + "  Kills: " + result.getInt("fnKills") + "  Deaths: " + result.getInt("fnDeaths") + "  Rating: " + result.getInt("fnRating"));
        }
        catch (Exception e){}*/
    }


    public void setupYPosCheck(int y) {
        final int b = y;
        TimerTask yCheck = new TimerTask() {
            public void run() {
                checkYs(b);
            }
        };
        //m_botAction.setPlayerPositionUpdating(500);
        m_botAction.scheduleTaskAtFixedRate(yCheck, 1000, 1000);
    }


    public void checkYs(int y) {
        /*if(!isRunning) return;
    	if(voting) return;

    	Iterator it = m_botAction.getPlayingPlayerIterator();

    	while(it.hasNext()) {
    		Player p = (Player)it.next();
    		if((p.getYLocation() / 16) > y) {
    			if(!lastDeaths.containsKey(new Integer(p.getPlayerID()))) {
    				lastDeaths.put(new Integer(p.getPlayerID()), new Integer(((int)(System.currentTimeMillis() / 1000 % 30))));
    			} else {
	    			int timeLeft = (int)(System.currentTimeMillis() / 1000 % 30) - ((Integer)lastDeaths.get(new Integer(p.getPlayerID()))).intValue();
	    			if(timeLeft == 15) {
	    				m_botAction.sendPrivateMessage(p.getPlayerID(), "You have 15 seconds to get to base.");
	    			} else if(timeLeft <= 0) {
	    				m_botAction.spec(p.getPlayerID());
	    				m_botAction.spec(p.getPlayerID());
	    			}
	    		}
	    	}
	    }*/
    }


    public boolean checkPreGamePlayerStatus() {
        if( gameStatus >= STATUS_PLAYING || gameStatus == STATUS_WAITINGFORPLAYERS )
            return true;
        int numPlayers = m_botAction.getPlayingPlayers().size();
        if(numPlayers <= 1) {
            m_botAction.sendArenaMessage("This game has been cancelled because there are not enough players!", Tools.Sound.CRYING);
            gameStatus = 0;
            locked = false;
            m_botAction.toggleLocked();
            return false;
        }
        return true;
    }


    public void cancel()
    {
    }
}