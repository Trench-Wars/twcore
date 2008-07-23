package twcore.bots.roboreplacement;

//import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
//import java.util.Set;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
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

    boolean isRunning = false;  // True if game is running
    boolean zoneOn = true;      // True if the zoning feature is turned on
    boolean zone = true;        // True if the bot should zone after this game
    boolean shipVoting = false; // True if ships are being voted on
    boolean deathsVote = false; // True if deaths are being voted on
    boolean voting = false;     // True if there is any voting going on (ships or deaths)
    boolean locked = false;     // True if arena should be locked.  (Used for insurance.)

    HashSet<Integer> players = new HashSet<Integer>(); //contains ID's of all the players that started the game
    HashMap<Integer, Integer> deaths = new HashMap<Integer, Integer>();  //key is ID of the person, value is the person's number of deaths
    HashMap<Integer, Integer> kills = new HashMap<Integer, Integer>();   //key is ID of the person, value is the person's number of kills
    HashMap<Integer, Integer> lastDeaths = new HashMap<Integer, Integer>();

    HashMap<Integer, Integer> votes = new HashMap<Integer, Integer>();   //key is ID of the person that voted, value is the person's vote
    ArrayList<Integer> allowedShips = new ArrayList<Integer>();          // Ships allowed in this type of elim

    Vector<String> topTen; //Vector of people with the top ten ratings

    BotSettings m_botSettings; //BotSettings object for the .cfg file

    int elimShip;         //Current elim ship
    int elimDeaths = 10;  //Current deaths
    int voteTally[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; //array for votes
    String mvp = "";
    int mvpKills = 0;
    int numVotes;           // Keeps track of number of votes, for reporting purposes

    String arena;                // name of the arena the bot is in
    String mySQLHost = "local";  // sql stuff
    String ships[] = {"", "Warbird", "Javelin", "Spider", "Leviathan", "Terrier", "Weasel", "Lancaster", "Shark", "Any Ship"}; //array list of ship type names


    /*Creates a new instance of the roboreplacement bot.
     */
    public roboreplacement(BotAction botAction)
    {
        super(botAction);
        EventRequester events = m_botAction.getEventRequester();
        events.request(EventRequester.PLAYER_DEATH);
        events.request(EventRequester.MESSAGE);
        events.request(EventRequester.LOGGED_ON);
        events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(EventRequester.PLAYER_ENTERED);
        events.request(EventRequester.PLAYER_LEFT);
        m_botSettings = m_botAction.getBotSettings();
    }

    public boolean isIdle() {
        return !isRunning;
    }


    /*Called when the bot logs in.
     *
     *Gets all the bot settings required from the cfg file then starts the matches.
     */
    public void handleEvent(LoggedOn event)
    {
        String botNumber = m_botSettings.getString(m_botAction.getBotName());
        arena = m_botSettings.getString("Arena" + botNumber);
        String[] pieces = m_botSettings.getString("AllowedShips" + botNumber).split(",");
        allowedShips.add(new Integer(pieces[0]));   // For anyship elim, the default ship
        for(int k = 0;k < pieces.length;k++)
        {
            try {
                allowedShips.add(new Integer(pieces[k]));
            } catch(Exception e) {}
            /*
            try {
                allowedShips.add(new Integer(pieces[0]));
                pieces = pieces[1].split(",", 2);
            } catch(Exception e) {}
            */
        }
        if(m_botSettings.getInt("CheckY" + botNumber) > 0) setupYPosCheck(m_botSettings.getInt("CheckY" + botNumber));
        m_botAction.joinArena(arena);
        setupZoner();
        m_botAction.sendUnfilteredPublicMessage("*specall");
        m_botAction.scheduleTaskAtFixedRate(zonerTrue, 15 * 60 * 1000, 15 * 60 * 1000);
    }

    /**
     * Called when a player enters the arena.
     *
     * Safety precaution that will spec the person and remove him/her from the
     * list of players if he/she accidently got added upon entry.
     */
    public void handleEvent(PlayerEntered event)
    {
        if( isRunning ) {
            if(voting)
                m_botAction.sendPrivateMessage(event.getPlayerID(), "Welcome to Elimination.  We are currently voting on the next game.");
            else
                m_botAction.sendPrivateMessage(event.getPlayerID(), "Welcome to Elimination.  We are currently in a " + ships[elimShip] + " elim to " + elimDeaths + ".  " + players.size() + " players in the game.");
            m_botAction.specWithoutLock(event.getPlayerID());
        } else {
            m_botAction.sendPrivateMessage(event.getPlayerID(), "Welcome to Elimination.  A game will start when 2 players enter.");
        }
        if(players.contains(new Integer(event.getPlayerID())))
            players.remove(new Integer(event.getPlayerID()));
        m_botAction.sendUnfilteredPrivateMessage( event.getPlayerID(), "*watchdamage" );
    }

    /**
     * Ends the game if a person spec's and only one player is left.
     */
    public void handleEvent(FrequencyShipChange event)
    {
        if(!players.contains(new Integer(event.getPlayerID())) && event.getShipType() != 0)
            players.add(new Integer(event.getPlayerID()));
        if(players.contains(new Integer(event.getPlayerID())) && event.getShipType() == 0)
            players.remove(new Integer(event.getPlayerID()));
        if(players.size() == 1 && isRunning)
        {
            Iterator<Integer> i = players.iterator();
            int pID = i.next().intValue();
            doGameOver( pID );

        } else if(players.size() == 0 && isRunning) {
            m_botAction.sendArenaMessage("Game has been cancelled.  I win.", 1);
            locked = false;
            m_botAction.toggleLocked();
            isRunning = false;

        } else if(players.size() >= 2 && !isRunning && !voting) {
            isRunning = true;
            voting = true;
            startVoting();
        }
    }

    /**
     * Poorly-written command handler.
     */
    public void handleEvent(Message event)
    {
        String message = event.getMessage();                           //Gets the message.
        int pid = event.getPlayerID();
        String name = m_botAction.getPlayerName(pid);  //Gets name of the person that sent the message.

        if( event.getMessageType() == Message.ARENA_MESSAGE ) {
            if( message.equals( "Arena UNLOCKED" ) && locked == true )
                m_botAction.toggleLocked();
            else if( message.equals( "Arena LOCKED" ) && locked == false )
                m_botAction.toggleLocked();
        }

        if( voting ) //Checks to see if there is a vote going on.
        {
            if( !votes.containsKey( pid ) ) //Checks to make sure the person has not voted yet.
            {
                if( Tools.isAllDigits(message) ) {

                    try {
                        int vote = Integer.parseInt(message);
                        if(deathsVote ) {
                            if( vote < 11 ) {
                                votes.put( pid, vote );
                                m_botAction.sendPrivateMessage(name, "Vote added for " + vote + " deaths." );
                            } else {
                                m_botAction.sendPrivateMessage(name, "Number of deaths must be less than 10." );
                            }
                        } else if(shipVoting ) {
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
        }

        if(m_botAction.getOperatorList().isER(name)) //checks for ER+, if the person is they are allowed to use the special commands :)
        {
            if(message.toLowerCase().startsWith("!die")) //Kills the bot :(
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
            if(message.toLowerCase().startsWith("!help")) //Sends the help message
            {
                m_botAction.sendSmartPrivateMessage(name, "!die          - Kills bot");
                m_botAction.sendSmartPrivateMessage(name, "!zoneoff      - Stops the bot from zoning before a game");
                m_botAction.sendSmartPrivateMessage(name, "!zoneon       - Allows the bot to zone before a game");
            }
        }
        if(message.toLowerCase().startsWith("!stats ")) //PM's the person with the requested stats
        {
            String pieces[] = message.split(" ", 2);
            m_botAction.sendSmartPrivateMessage(name, getPlayerStats(pieces[1]));
        }
        else if(message.toLowerCase().startsWith("!stats")) //PM's the person with their stats
            m_botAction.sendSmartPrivateMessage(name, getPlayerStats(name));
        else if(message.toLowerCase().startsWith("!topten")) //PM's the person the top ten.
            topTen(name);
        else if(message.toLowerCase().startsWith("!status"))
        {
            if( isRunning ) {
                if(voting)
                    m_botAction.sendPrivateMessage(event.getPlayerID(), "We are currently voting on the next game.");
                else
                    m_botAction.sendPrivateMessage(event.getPlayerID(), "We are currently in a " + ships[elimShip] + " elim to " + elimDeaths + ".  " + players.size() + " players in the game.");
            } else {
                m_botAction.sendPrivateMessage(event.getPlayerID(), "Elimination has temporarily stopped.  A game will start when 2 players enter.");
            }
        }
        else if(message.toLowerCase().startsWith("!help")) //Sends the help message
        {
            //m_botAction.sendSmartPrivateMessage(name, "!stats        - Gets your stats.");
            //m_botAction.sendSmartPrivateMessage(name, "!stats <name> - Get's <name>'s stats");
            //m_botAction.sendSmartPrivateMessage(name, "!topten       - Get's list of top ten players");
            m_botAction.sendSmartPrivateMessage(name, "!status       - Displays current status of the game");
            m_botAction.sendSmartPrivateMessage(name, "!help         - Displays this message");
        }
    }

    /*Sections individually commented
     */
    public void handleEvent(PlayerDeath event)
    {
        if(isRunning && !(shipVoting || deathsVote)) // Makes sure the game is going.
        {
            Player p = m_botAction.getPlayer(event.getKilleeID());
            if(kills.containsKey(new Integer(event.getKillerID()))) //updates killer's kills
            {
                int winz = kills.get(new Integer(event.getKillerID())).intValue() + 1;
                kills.put(new Integer(event.getKillerID()), new Integer(winz));
            }
            else //adds killer to kills hashmap if they arent there yet
                kills.put(new Integer(event.getKillerID()), new Integer(1));
            if(deaths.containsKey(new Integer(event.getKilleeID()))) //adds the killee's death to their death total
            {
                int lossez = deaths.get(new Integer(event.getKilleeID())).intValue() + 1;
                deaths.put(new Integer(event.getKilleeID()), new Integer(lossez));
            }
            else  //adds the killee to the deaths hashmap if they are not there yet
                deaths.put(new Integer(event.getKilleeID()), new Integer(1));

            if(!kills.containsKey(new Integer(event.getKilleeID())))         //adds killee to kills hashmap to make sure the bot doesn't mess up on db entry
                kills.put(new Integer(event.getKilleeID()), new Integer(0));
            if(!deaths.containsKey(new Integer(event.getKillerID())))        //adds killer to deaths hashmap to make sure the bot doesn't mess up on db entry
                deaths.put(new Integer(event.getKillerID()), new Integer(0));

            if(p.getLosses() >= elimDeaths) //spec's the person if they have more deaths than allowed
            {
                m_botAction.sendArenaMessage(m_botAction.getPlayerName(event.getKilleeID()) + " is out. " + p.getWins() + " wins, " + p.getLosses() + " deaths.");
                if( p.getWins() > mvpKills ) {
                    mvp = p.getPlayerName();
                    mvpKills = p.getWins();
                } else if( p.getWins() > mvpKills )
                    mvp += ", " + p.getPlayerName();

                m_botAction.sendUnfilteredPrivateMessage(event.getKilleeID(), "*spec");
                m_botAction.sendUnfilteredPrivateMessage(event.getKilleeID(), "*spec");
                players.remove(new Integer(event.getKilleeID()));
            }

            if(players.size() == 1 ) {
                doGameOver( event.getKillerID() );
            }

            lastDeaths.put(new Integer(p.getPlayerID()), new Integer(((int)(System.currentTimeMillis() / 1000 % 30))));
        }
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

        if(players.size() == 1 && isRunning) //handles the game over if there is only one person left.
        {
            Iterator<Integer> i = players.iterator();
            int pID = i.next().intValue();
            doGameOver( pID );
        }
        // Cancel game if for some reason nobody is left.
        if( players.size() == 0 && isRunning ) {
            m_botAction.sendArenaMessage("Game has been cancelled.  I win.", 1);
            locked = false;
            m_botAction.toggleLocked();
            isRunning = false;
        }
        m_botAction.sendUnfilteredPrivateMessage( event.getPlayerID(), "*watchdamage" );
    }

    public void nextgame() //stuff individually commented
    {
        m_botAction.sendUnfilteredPublicMessage("*scorereset"); //resets score...
        if( !checkPreGamePlayerStatus() )
            return;

        TimerTask twotenSecs = new TimerTask()
        {
            public void run()
            {
                if( !checkPreGamePlayerStatus() )
                    return;

                Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
                while(i.hasNext()) //adds players to the players hashset
                {
                    Player p = i.next();
                    players.add(new Integer(p.getPlayerID()));
                }

                locked = true;
                m_botAction.toggleLocked(); //locks the arena
                if(elimShip == 9) //checks if it is an anyship elim, if it is it checks everyone to make sure they are in a valid ship for elim type
                {
                    Iterator<Player> it2 = m_botAction.getPlayingPlayerIterator();
                    while(it2.hasNext())
                    {
                        Player p = it2.next();
                        if(!allowedShips.contains(new Integer(p.getShipType()))) //sets person to default (first entered) elim ship type if they are in an illegal ship
                            m_botAction.sendUnfilteredPrivateMessage(p.getPlayerName(), "*setship " + allowedShips.get(0).intValue());
                    }
                }
                else //if it is not an anyship elim it sets everyone to the correct ship then starts
                    m_botAction.changeAllShips(elimShip);
                m_botAction.createRandomTeams(1);
                m_botAction.sendArenaMessage( ships[elimShip] + " Elim to " + elimDeaths + " has begun.  GO! GO! GO!!", 104);
                m_botAction.sendUnfilteredPublicMessage("*scorereset");
                lastDeaths.clear();
                mvp = "";
                mvpKills = 0;
                isRunning = true;
            }
        };

        m_botAction.scheduleTask(twotenSecs, 20 * 1000);
    }


    public void startVoting()
    {
        shipVoting = true;
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

        if( !checkPreGamePlayerStatus() )
            return;

        shipVote = new TimerTask() {
            public void run() {
                if( !checkPreGamePlayerStatus() )
                    return;
                elimShip = allowedShips.get(countVotes(false)).intValue(); //counts the votes and sets the elimShip to the proper thing
                m_botAction.sendArenaMessage("It will be " + ships[elimShip] + " elim. " + (numVotes==-1 ? "(default)" : "(" + numVotes + " votes)") ); //announces the result of the vote and starts death voting
                m_botAction.sendArenaMessage("Vote on deaths (1-10)");
                shipVoting = false;
                deathsVote = true;
            }
        };

        m_botAction.scheduleTask(shipVote, 10 * 1000); //schedules the end of the vote

        deathVoting = new TimerTask() {
            public void run() {
                if( !checkPreGamePlayerStatus() )
                    return;
                voting = false;
                deathsVote = false;
                elimDeaths = countVotes(true); //tallies the votes
                m_botAction.sendArenaMessage(ships[elimShip] + " elim to " + elimDeaths + "  " + (numVotes==-1 ? "(default)" : "(" + numVotes + " votes)"));
                m_botAction.sendArenaMessage("Enter if playing.  Game begins in 30 seconds ...");
                nextgame();
            }
        };

        m_botAction.scheduleTask(deathVoting, 30 * 1000); //schedules the end of the death vote and calls nextgame() to start the elim game
    }


    void setupZoner() //makes the timertask for making zone true.
    {
        zonerTrue = new TimerTask()
        {
            public void run()
            {
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

        if( winner == -1 ) {
            if(votingForDeaths)
                winner = 10;
            else
                winner = allowedShips.get(0);
            numVotes = -1;
        } else {
            numVotes = voteTally[winner];
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


    public void doGameOver( int winnerID ) {
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
        if( !mvp.equals("") )
            m_botAction.sendArenaMessage( "MVP: " + mvp + " ... " + mvpKills + " kills" );
        mvp = "";
        mvpKills = 0;
        locked = false;
        m_botAction.toggleLocked();
        isRunning = false;
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
        m_botAction.setPlayerPositionUpdating(500);
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
        int numPlayers = m_botAction.getPlayingPlayers().size();
        if(numPlayers <= 1) {
            m_botAction.sendArenaMessage("This game has been cancelled because there are not enough players!", Tools.Sound.CRYING);
            isRunning = false;
            voting = false;
            shipVoting = false;
            deathsVote = false;
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