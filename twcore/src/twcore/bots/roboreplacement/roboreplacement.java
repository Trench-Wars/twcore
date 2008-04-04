package twcore.bots.roboreplacement;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
import twcore.core.game.Player;

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

    boolean isRunning = false;  //variable that determines if the game is running
    boolean zoneOn = true;      //determines whether to zone or not to zone after a game if zone is true
    boolean zone = false;       //if this is true the bot will zone before the next game
    boolean shipVoting = false; //variable for determining if ships are being voted on
    boolean deathsVote = false; //variable for determining if deaths are being voted on
    boolean voting = false;     //true if there is any voting going on

    HashSet<Integer> players = new HashSet<Integer>(); //contains ID's of all the players that started the game
    HashMap<Integer, Integer> votes = new HashMap<Integer, Integer>();   //key is ID of the person that voted, value is the person's vote
    HashMap<Integer, Integer> deaths = new HashMap<Integer, Integer>();  //key is ID of the person, value is the person's number of deaths
    HashMap<Integer, Integer> kills = new HashMap<Integer, Integer>();   //key is ID of the person, value is the person's number of kills
    HashMap<Integer, Integer> lastDeaths = new HashMap<Integer, Integer>();

    Vector<String> topTen; //Vector of people with the top ten ratings

    BotSettings m_botSettings; //BotSettings object for the .cfg file

    int elimShip;         //Current elim ship
    int elimDeaths = 10;  //Current deaths
    int shipz;            //Number of different ships allowed.
    int voteTally[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; //array for votes

    String arena;                //name of the arena the bot is in
    String shipName;             //name of the current ship
    String mySQLHost = "local";  //sql stuff
    String num;                  //bot #
    String ships[] = {"", "Warbird", "Javelin", "Spider", "Leviathan", "Terrier", "Weasle", "Lancaster", "Shark", "Any Ship"}; //array list of ship type names

    ArrayList<Integer> voters = new ArrayList<Integer>();       //list of the people that have voted already
    ArrayList<Integer> allowedShips = new ArrayList<Integer>(); //list of ships allowed in this type of elim

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
        num = m_botSettings.getString(m_botAction.getBotName());
        arena = m_botSettings.getString("Arena" + num);
        String[] pieces = m_botSettings.getString("Ship" + num).split(",", 2);
        shipz = m_botSettings.getInt("Ships" + num);
        allowedShips.add(new Integer(4));
        for(int k = 1;k <= shipz;k++)
        {
            try {
                allowedShips.add(new Integer(pieces[0]));
                pieces = pieces[1].split(",", 2);
            } catch(Exception e) {}
        }
        shipName = ships[elimShip];
        if(m_botSettings.getInt("CheckY" + num) > 0) setupYPosCheck(m_botSettings.getInt("CheckY" + num));
        m_botAction.joinArena(arena);
        setupZoner();
        m_botAction.sendUnfilteredPublicMessage("*specall");
        m_botAction.scheduleTaskAtFixedRate(zonerTrue, 15 * 60 * 1000, 15 * 60 * 1000);
    }

    /*Called when a player enters the arena.
     *
     *Safety precaution that will spec the person and remove him/her from the
     *list of players if he/she accidently got added upon entry.
     */
    public void handleEvent(PlayerEntered event)
    {
        if(voting)
            m_botAction.sendPrivateMessage(event.getPlayerID(), "We are currently voting on the next game.");
        else
            m_botAction.sendPrivateMessage(event.getPlayerID(), "We are currently in a " + ships[elimShip] + " elim to " + elimDeaths + ". " + players.size() + " players in the game.");
        m_botAction.sendUnfilteredPrivateMessage(event.getPlayerID(), "*spec");
        m_botAction.sendUnfilteredPrivateMessage(event.getPlayerID(), "*spec");
        if(players.contains(new Integer(event.getPlayerID())))
            players.remove(new Integer(event.getPlayerID()));
    }

    /*Called when a person changes ships or frequency
     *
     *Ends the game if a person spec's and only one player is left
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
            int pID = ((Integer)i.next()).intValue();
            m_botAction.sendArenaMessage(m_botAction.getPlayerName(pID) + " has won!", 5);
            handleGameOver(pID);
            m_botAction.toggleLocked();
            isRunning = false;
        }
        if(players.size() == 2 && !isRunning && !voting)
        {
            isRunning = true;
            voting = true;
            startVoting();
        }
    }

    /*Sections individually commented.
     */
    public void handleEvent(Message event)
    {
        String message = event.getMessage();                           //Gets the message.
        String name = m_botAction.getPlayerName(event.getPlayerID());  //Gets name of the person that sent the message.

        if(voting) //Checks to see if there is a vote going on.
        {
            if(!(votes.containsKey(new Integer(event.getPlayerID())))) //Checks to make sure the person has not voted yet.
            {
                try {
                    if(deathsVote && Integer.parseInt(message) < 11) //adds the vote to the votes HashMap
                    {
                        votes.put(new Integer(event.getPlayerID()), new Integer(Integer.parseInt(message)));
                        voters.add(new Integer(event.getPlayerID()));
                    }
                    else if(shipVoting && Integer.parseInt(message) <= shipz) //adds the vote to the votes HashMap
                    {
                        votes.put(new Integer(event.getPlayerID()), new Integer(Integer.parseInt(message)));
                        voters.add(new Integer(event.getPlayerID()));
                    }
                } catch(Exception e) {}
            }
        }

        if(m_botAction.getOperatorList().isER(name)) //checks for ER+, if the person is they are allowed to use the special commands :)
        {
            if(message.toLowerCase().startsWith("!die")) //Kills the bot :(
            {
                m_botAction.sendSmartPrivateMessage(name, "Logging off...");
                m_botAction.die();
            }
            if(message.toLowerCase().startsWith("!zoneoff") && zoneOn) //Turns the zoning ability off
            {
                try {
                    m_botAction.cancelTask(zonerTrue);
                }catch (Exception e) {
                    m_botAction.sendSmartPrivateMessage(name, "Problem turning off zoners.");
                }
                m_botAction.sendSmartPrivateMessage(name, "I promise I won't send any more zoners...<insert evil snicker here>");
                zoneOn = false;
            }
            if(message.toLowerCase().startsWith("!zoneon") && !zoneOn) //Turns the zoning ability on
            {
                try {
                    m_botAction.cancelTask(zonerTrue);
                    setupZoner();
                    m_botAction.scheduleTaskAtFixedRate(zonerTrue, 15 * 60 * 1000, 15 * 60 * 1000);
                }catch (Exception e) {
                    m_botAction.sendSmartPrivateMessage(name, "Problem turning on zoners.");
                }
                m_botAction.sendSmartPrivateMessage(name, name + "'s mom has got it goin on...");
                zoneOn = true;
            }
            if(message.toLowerCase().startsWith("!help")) //Sends the help message
            {
                m_botAction.sendSmartPrivateMessage(name, "!die          - Tells the bot to go parachuting (w/o the parachute)");
                m_botAction.sendSmartPrivateMessage(name, "!zoneoff      - Stops the bot from zoning before a game");
                m_botAction.sendSmartPrivateMessage(name, "!zoneon       - Allows the bot to zone before a game");
                m_botAction.sendSmartPrivateMessage(name, "!help         - Displays this message *shock*");
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
            if(voting)
                m_botAction.sendPrivateMessage(name, "We are currently voting on the next game.");
            else
                m_botAction.sendSmartPrivateMessage(name, "We are currently in a " + ships[elimShip] + " elim to " + elimDeaths + ". " + players.size() + " players in the game.");
        }
        else if(message.toLowerCase().startsWith("!help")) //Sends the help message
        {
            m_botAction.sendSmartPrivateMessage(name, "!stats        - Gets your stats.");
            m_botAction.sendSmartPrivateMessage(name, "!stats <name> - Get's <name>'s stats");
            m_botAction.sendSmartPrivateMessage(name, "!topten       - Get's list of top ten players");
            m_botAction.sendSmartPrivateMessage(name, "!status       - Displays current status of the game");
            m_botAction.sendSmartPrivateMessage(name, "!help         - Displays this message");
        }
    }

    /*Sections individually commented
     */
    public void handleEvent(PlayerDeath event)
    {
        if(isRunning && !(shipVoting && deathsVote)) //Makes sure the game is going.
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
                m_botAction.sendUnfilteredPrivateMessage(event.getKilleeID(), "*spec");
                m_botAction.sendUnfilteredPrivateMessage(event.getKilleeID(), "*spec");
                players.remove(new Integer(event.getKilleeID()));
            }

            if(players.size() == 1) //handles the game over when only one person is left
            {
                m_botAction.sendArenaMessage("GAME OVER: Winner " + m_botAction.getPlayerName(event.getKillerID()), 5);
                handleGameOver(event.getKillerID());
                m_botAction.toggleLocked();
                isRunning = false;
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
            m_botAction.sendArenaMessage("GAME OVER: Winner " + m_botAction.getPlayerName(pID), 5);
            handleGameOver(pID);
            m_botAction.toggleLocked();
            isRunning = false;
        }
    }

    public void nextgame() //stuff individually commented
    {
        m_botAction.sendUnfilteredPublicMessage("*scorereset"); //resets score...

        TimerTask twotenSecs = new TimerTask()
        {
            public void run()
            {
                int numPlayers = m_botAction.getPlayingPlayers().size();
                if(numPlayers <= 1)
                {
                    m_botAction.sendArenaMessage("This game has been cancelled because there are not enough players!", 5);
                    m_botAction.sendUnfilteredPublicMessage("*lock");
                    return;
                }
                Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
                while(i.hasNext()) //adds players to the players hashset
                {
                    Player p = i.next();
                    players.add(new Integer(p.getPlayerID()));
                }

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
                m_botAction.sendArenaMessage("Go! Go! Go!", 104);
                m_botAction.sendUnfilteredPublicMessage("*scorereset");
                lastDeaths.clear();
                isRunning = true;
            }
        };

        m_botAction.scheduleTask(twotenSecs, 20 * 1000);
    }

    public void startVoting() //individual comments again :P
    {
        shipVoting = true;
        if(zone == true) //sends zoner for the game if it has been at least 15 minutes since the last zoner
        {
            zone = false;
            m_botAction.sendZoneMessage("Next elimination match starting in ?go " + arena);
        }

        //creates the message for the ship voting
        String tempShip = "Vote on ship: ";
        for(int k = 1;k <= shipz;k++)
            tempShip += k + " - " + ships[allowedShips.get(k).intValue()] + " ";
        m_botAction.sendArenaMessage(tempShip);


        shipVote = new TimerTask()
        {
            public void run()
            {

                elimShip = allowedShips.get(countVotes(false)).intValue(); //counts the votes and sets the elimShip to the proper thing
                m_botAction.sendArenaMessage("It will be a " + ships[elimShip] + " elim."); //announces the result of the vote and starts death voting
                m_botAction.sendArenaMessage("Vote on deaths (1-10)");
                shipVoting = false;
                deathsVote = true;
            }
        };

        m_botAction.scheduleTask(shipVote, 10 * 1000); //schedules the end of the vote

        deathVoting = new TimerTask()
        {
            public void run()
            {

                voting = false;
                deathsVote = false;
                elimDeaths = countVotes(true); //tallies the votes
                m_botAction.sendArenaMessage(ships[elimShip] + " elim to " + elimDeaths);
                m_botAction.sendArenaMessage("Good luck, game begins in 20 seconds.");
                nextgame();
            }
        };

        m_botAction.scheduleTask(deathVoting, 20 * 1000); //schedules the end of the vote and calls nextgame() to start the elim game
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

    public int countVotes(boolean deathvotez) //counts the votes, if deathvotez is false it starts from 1, if it is true it starts from 10
    {
        for(int k = 0;k < voters.size();k++)
        {
            try {
                int vote = votes.get(voters.get(k)).intValue();
                voters.remove(k);
                voteTally[vote]++;
            } catch(Exception e) {}
        }
        int winner;

        if(deathvotez)
            winner = 10;
        else
            winner = 1;

        for(int k = 2;k < 11;k++)
        {
            if(voteTally[k] > voteTally[winner])
                winner = k;
        }

        votes.clear();

        resetTally();

        return winner; // returns the winner of the tally
    }

    public void resetTally() //resets voteTally values to 0
    {
        voteTally[1] = 0;
        voteTally[2] = 0;
        voteTally[3] = 0;
        voteTally[4] = 0;
        voteTally[5] = 0;
        voteTally[6] = 0;
        voteTally[7] = 0;
        voteTally[8] = 0;
        voteTally[9] = 0;
        voteTally[10] = 0;
    }

    public void handleGameOver(int winnerID) //ends game and uploads the game's data to the database
    {
        try {
            Set<Integer> set = deaths.keySet();
            Iterator<Integer> it = set.iterator();
            while(it.hasNext())
            {
                String username = m_botAction.getPlayerName(((Integer)it.next()).intValue());
                int losses = deaths.get(new Integer(m_botAction.getPlayerID(username))).intValue();
                int killz = kills.get(new Integer(m_botAction.getPlayerID(username))).intValue();
                int winz;
                if(m_botAction.getPlayerID(username) == winnerID)
                    winz = 1;
                else
                    winz = 0;

                ResultSet qryHasElimtwoRecord = m_botAction.SQLQuery(mySQLHost, "SELECT fcUserName, fnPlayed, fnWon, fnKills, fnDeaths FROM tblElimTwoStats"+num+" WHERE fcUserName = \"" + username+"\"");
                if(!qryHasElimtwoRecord.next()) //creates a new record for a person
                {
                    double rating1 = killz / losses;
                    if(rating1 > 1.0)
                        rating1 = 1.0;
                    double rating2 = winz / 1;
                    double rating = (rating1 + rating2) * 500;
                    m_botAction.SQLClose( m_botAction.SQLQuery( mySQLHost, "INSERT INTO tblElimTwoStats"+num+"(fcUserName, fnPlayed, fnWon, fnKills, fnDeaths, fnRating) VALUES (\""+username+"\",1,"+winz+","+killz+","+losses+","+rating+")") );
                }
                else //updates a persons record
                {
                    //double played = qryHasElimtwoRecord.getInt("fnPlayed") + 1.0;
                    double wines = qryHasElimtwoRecord.getInt("fnWon") + winz;
                    double killez = qryHasElimtwoRecord.getInt("fnKills") + killz;
                    double lossez = qryHasElimtwoRecord.getInt("fnDeaths") + losses;
                    double rating1 = killez / lossez;
                    if(rating1 > 1.0)
                        rating1 = 1.0;
                    double rating2 = wines / 1;
                    double rating = (rating1 + rating2) * 500;
                    m_botAction.SQLClose( m_botAction.SQLQuery( mySQLHost, "UPDATE tblElimTwoStats"+num+" SET fnPlayed = fnPlayed+1, fnWon = fnWon + "+winz+", fnKills = fnKills + "+killz+", fnDeaths = fnDeaths + "+ losses + ", fnRating = " +rating+ "  WHERE fcUserName = \"" + username+"\"") );
                    //tblElimTwoStats fnPlayed fnWon fnKills fnDeaths fnRating fcUserName
                }
                m_botAction.SQLClose( qryHasElimtwoRecord );
            }
        } catch(Exception e) {}
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
        }*/ return "";
    }

    public void topTen(String name) //pm's the person the top ten people
    {
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

    public void cancel()
    {
    }
}