//medorP bot, written by Lain
//
/*  NOTES
    //The last bug I can identify.....
    //Fix the !start x_deaths thing, so it announces to the host if the
    //paramater is invalid instead of starting anyway with default 2 deaths.
    /*      08-08-07
    Arena locking issue tackled. Because of the toggle, if the game is killed before
    the lock, it will still issue another lock
    (instead of the proper unlock like it would if it were killed mid-game).

    Changed streak texts to read from an array (easier to change them)

    Ship change function is now sleeker and more efficient (no more CASE:, its a quick IF)

    Streak function is now a FOR loop instead of lots of IF's.
    Same function (and excecution speed), its just condensed for easy reading (and debugging...
    because it was crashing the bot)
*/

package twcore.bots.multibot.medorp;

import java.util.Iterator;
import java.util.TimerTask;

import twcore.core.EventRequester;
import twcore.bots.MultiModule;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;


/**

    @author Lain
    @version 1.1
*/
public class medorp extends MultiModule {

    /**
        Whether the bot is enabled or not
    */
    private boolean enabled = false;

    /**
        Has the event been started? True or false
    */
    private boolean started = false;

    /**
        Should bot announce when someone ship changes?
        Controversial ! Prodem has this on by default, should this event?
        (mad spam with low death limit possibly, but prodem limit is 1!!
        and this game is default 2, so less spam (slower))
    */
    private boolean verbose = true ;

    /**
        Number of deaths players get _per ship_(8 ships total, 5 deaths max)
    */
    private int maxDeaths = 2;

    /**
        Makes sure people dont respawn invisible before their respawn timer has expired.
        dugwyler: Unimplemented?  Commented out.
    */
    //private TimerTask respawnTimer;

    /**
        Used to grab specific player data from the hashmap
    */
    private Iterator<Player> playerList;

    /**
        Hold the name of the current MVP
    */
    private String MVPname = "No-one";

    /**
        How many kills does the MVP have?
    */
    private int MVPkills = 0;

    /**
        Which ship did the MVP score the record with
    */
    private int MVPship = 0;

    /**
        Is the arena locked? Lets assume its not
        (as it should be by default)
    */
    private boolean arenaLocked = false ;

    /**
        The streak values for each of the ships.
        [streak value][ship_no] (should be the other way around but BLEH!)
    */
    private int[][] shipStreaks = {
        { 0, 3, 5, 7, 10, 15 }, //[0] unused set, easier than subtracting 1 from everything
        { 0, 3, 5, 7, 10, 15 },
        { 0, 3, 5, 7, 10, 15 },
        { 0, 3, 5, 7, 10, 15 },
        { 0, 4, 6, 8, 10, 15 },
        { 0, 4, 7, 10, 15, 20 },
        { 0, 4, 7, 10, 15, 20 },
        { 0, 5, 8, 15, 20, 25 },
        { 0, 7, 15, 20, 25, 30 }
    };

    /**
        These are the multipliers applied to the streak values
        based on how many deaths the game will be played to.
        TESTING (default, (1,)1,1,1,2,2,3)
    */
    private int[] killMultiplier = { 1, 1, 1, 1, 2, 2 };

    /**
        The streak texts
    */
    private String[] streakText = {"is killing stuff",  //This should never be output
                                   "is causing a commotion",
                                   "is on a spree!",
                                   "is on a rampage!",
                                   "is UNSTOPPABLE",
                                   "must be hacking!"
                                  } ;

    /**
        String representations of the ship types
    */
    private String[] shipTypes = {
        "single ship", "warbird", "javelin", "spider", "leviathan",
        "terrier", "weasel", "lancaster", "shark"
    };

    /**
        Yeah, about that.
    */
    public void init() {

    }

    public void requestEvents(ModuleEventRequester events)
    {
        events.request(this, EventRequester.PLAYER_DEATH);
        events.request(this, EventRequester.PLAYER_LEFT);
        events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(this, EventRequester.FREQUENCY_CHANGE);
    }

    /**
        Event Handler

        @param event Event
    */
    public void handleEvent(Message event) {
        String message = event.getMessage();
        message = message.trim();

        if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
            String name = m_botAction.getPlayerName(event.getPlayerID());

            if (opList.isER(name)) {
                handleCommand(name, message);
            }
        }
    }

    /**
        Handles private messages to the bot

        @param name Who sent the command.
        @param message What text was sent.
    */
    public void handleCommand(String name, String message) {
        //If event is started with a 2nd paramater, parse it to find if its
        //a valid integer.  Possibly add bounds (like >0 && <10)
        if (message.startsWith("!start ")) {
            String[] subStr = Tools.stringChopper(message, ' ');

            if (Tools.isAllDigits(subStr[1])) {
                maxDeaths = Integer.parseInt(subStr[1]);
            } else {
                maxDeaths = 2;
            }

            if ((maxDeaths > 5) || (maxDeaths < 1)) {
                m_botAction.sendPrivateMessage(name, "Valid deaths are 1-5");
            } else {
                handleStart(name, message);
            }
        }
        else if (message.startsWith("!start")) {
            maxDeaths = 2;

            //Just in case the bot is still running from a previous
            //instance, and was initialise with a different value.
            handleStart(name, message);
        }
        //If bot is !stopped
        else if (message.startsWith("!stop")) {
            if (opList.isER(name) && (started != false)) {
                enabled = false;
                m_botAction.sendPrivateMessage(name, "medorP aborted");
                m_botAction.sendArenaMessage("medorP mercilessly killed by " +
                                             name);

                if(arenaLocked != false)
                {
                    m_botAction.toggleLocked();
                    m_botAction.sendArenaMessage("Arena UNLOCKED") ;
                    arenaLocked = false ;
                }

                //Stop medorP
                m_botAction.cancelTasks();

            }
        } else if (message.startsWith("!verbose")) {
            if (opList.isER(name)) {
                //toggle verbose
                verbose = !verbose;

                if (verbose) {
                    m_botAction.sendPrivateMessage(name, "Verbose mode is ON");
                } else {
                    m_botAction.sendPrivateMessage(name, "Verbose mode is OFF");
                }
            }
        }
    }

    /**
        Lets get the ball rolling and start this damn thing !

        @param name Who sent the game start command?
        @param message Unused
    */
    private void handleStart(String name, String message) {
        if ((opList.isER(name)) && (enabled != true)) {
            started = true;
            MVPkills = 0;
            MVPname = "No-one";
            MVPship = 0;

            m_botAction.sendPrivateMessage(name,
                                           "medorP to " + maxDeaths + " death(s) started");
            //This is a cheat, it asks the host to handle unlocking.
            m_botAction.sendPrivateMessage(name,
                                           "Please ensure the arena is UNLOCKED (if players are entering, it is)") ;
            displayRules();

            final TimerTask getReady = new TimerTask() {
                public void run() {
                    m_botAction.sendArenaMessage("Let the medorP begin ! Go go go go....",
                                                 104);
                    m_botAction.scoreResetAll();
                    enabled = true;

                    if (getNumPlayers() <= 1) {
                        enabled = false;
                        m_botAction.sendArenaMessage(
                            "medorP aborted. Not enough players.");

                        if(arenaLocked != false)
                        {
                            m_botAction.toggleLocked();
                            m_botAction.sendArenaMessage("Arena UNLOCKED") ;
                            arenaLocked = false ;
                        }

                        //Stop medorP
                        m_botAction.cancelTasks();
                    }
                }
            };

            final TimerTask arenaSetup = new TimerTask() {
                public void run() {
                    if(arenaLocked != true)
                    {
                        //Lock the arena (Why a toggle? crazy)
                        m_botAction.toggleLocked();
                        m_botAction.sendArenaMessage("Arena LOCKED") ;
                        arenaLocked = true ;
                    }

                    //Set everyone to ship 1
                    m_botAction.changeAllShips(1);

                    //m_botAction.createRandomTeams(1) ;
                    setUniqueFreq();

                    m_botAction.sendArenaMessage("medorP begins in 30 seconds, get safe!",
                                                 1);

                    //When this excecutes, schedule the next task.
                    m_botAction.scheduleTask(getReady, 30000);
                }
            };

            m_botAction.scheduleTask(arenaSetup, 30000);
        }
    }

    //When someone dies, check if they're on or above the death limit for a shipchange
    //Then handle that change
    public void handleEvent(PlayerDeath event) {
        if (enabled) {
            int killerID = event.getKillerID();
            getAnnounceStreak(killerID);
            updateMVP(event.getKillerID());

            int killeeID = event.getKilleeID();
            Player killedID = m_botAction.getPlayer(killeeID);

            if (killedID.getLosses() >= maxDeaths) {
                handleDeathLimit(killedID, event);
            }
        }
    }
    /**
        When someone specs or (other FrequencyShipChange events)
        check to see how many players are left. If 1 THEN wooot ! game over $

        @param event Shipchange Event
    */

    public void handleEvent(FrequencyShipChange event) {
        if (enabled) {
            checkAnnounceWinner();
        }
    }

    /**
        Oh noes a player left the game :(

        @param event Playerlevel event
    */
    public void handleEvent(PlayerLeft event) {
        if (enabled) {
            checkAnnounceWinner();
        }
    }

    /**
        Player changed frequency (also called when player specs)

        @param event Frequencychange event
    */
    public void handleEvent(FrequencyChange event) {
        if (enabled) {
            checkAnnounceWinner();
        }
    }

    /**
        When someone is on or above the death limit, fix them up with a new ship
        using timertask to keep things nice and clean

        @param ID Player object
        @paran event Playerdeath event
    */

    private void handleDeathLimit(Player ID, PlayerDeath event) {
        final int pID = ID.getPlayerID();
        final String pName = ID.getPlayerName();
        int shipType = ID.getShipType();

        if(shipType < 8)
        {
            changeShip(pID, shipType + 1) ;

            if(verbose)
            {
                m_botAction.sendArenaMessage(pName + " has changed ship to " + shipTypes[shipType + 1]) ;
            }
        }

        else if(shipType == 8)
        {
            m_botAction.specWithoutLock(pID);
            m_botAction.sendArenaMessage(pName + " is out !");
        }

    }

    /**
        Display the game rules
    */
    private void displayRules() {
        m_botAction.sendArenaMessage("*> How it works...");
        m_botAction.sendArenaMessage(
            "*> You spawn as a warbird, and after you lose " + maxDeaths +
            " death(s), you change to a javelin.");
        m_botAction.sendArenaMessage(
            "*> This continues through the ships, and when you die as shark, you're out.");
        m_botAction.sendArenaMessage(
            "*> Each ship is more powerful than the last. (Terr+ gain bombs)");
        m_botAction.sendArenaMessage("Unspec to play, game starting soon...", 2);
    }

    /**
        Find out how many players are left

        @return how many players are left.
    */
    private int getNumPlayers() {
        //Iterates to find out how many players are left
        int i = 0;

        playerList = m_botAction.getPlayingPlayerIterator();

        while (playerList.hasNext()) {
            i++;
            playerList.next();
        }

        return i;
    }

    /**
        If there is only 1 player left in-game then announce him as the winner.
    */
    private void checkAnnounceWinner() {
        if (getNumPlayers() == 1) {
            Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
            Player winner = it.next();

            m_botAction.sendArenaMessage("And the winner is ... " +
                                         winner.getPlayerName() + "!", 5);
            enabled = false;

            /*        if(MVPname != "")
                    {
            */
            final TimerTask sendMVP = new TimerTask() {
                public void run() {
                    m_botAction.sendArenaMessage("MVP : " + MVPname +
                                                 ", with " + MVPkills + " kills in a " +
                                                 shipTypes[MVPship], 7);

                    if(arenaLocked != false)
                    {
                        m_botAction.toggleLocked();
                        m_botAction.sendArenaMessage("Arena UNLOCKED") ;
                        arenaLocked = false ;
                    }

                    m_botAction.cancelTasks();
                }
            };

            m_botAction.scheduleTask(sendMVP, 5000);
        }

    }

    /**
        Handle the ship changes

        @param pID Player ID in the hashmap
        @param shipNo Which ship the player was (currently is before change)
    */
    private void changeShip(int pID, int shipNo) {
        final int thispID = pID;
        final int thisshipNo = shipNo;

        //Takes a player ID and sets their ship on a timer
        final TimerTask respawnTimer = new TimerTask() {
            public void run() {
                m_botAction.setShip(thispID, thisshipNo);
                m_botAction.scoreReset(thispID);
            }
        };

        m_botAction.scheduleTask(respawnTimer, 4000);
    }

    /**
        Arrange everyone on their own teams
    */
    private void setUniqueFreq() {
        //m_botAction.createRandomTeams(1) seems to be buggy when I utilise it.
        //so this method will arrange everyone on a unique freq (teams of 1).
        int i = 0;

        Iterator<Player> freqIterator = m_botAction.getPlayingPlayerIterator();
        Player tempPlayer;

        while (freqIterator.hasNext()) {
            i++;
            tempPlayer = (Player) freqIterator.next();
            m_botAction.setFreq(tempPlayer.getPlayerName(), i);
        }
    }

    /**
        Help messages

        @return String array of help messages
    */
    public String[] getModHelpMessage() {
        String[] medorPHelp = {
            "After the event is !started, medorP bot will lock the arena,",
            "put all players on seperate freqs and set ship to warbird.",
            "All you have to do is sit on your hands and watch.",
            "!start     - Starts default medorP, 2 deaths per ship",
            "!start x   - Starts medorP,  x deaths per ship (1-5)",
            "!verbose   - Toggles verbose mode (x has changed to <shipname>)",
            "!stop 	    - Ends medorP abnormally",
            "!help 	    - Displays this",
        };

        return medorPHelp;
    }

    /**
        If a player has hit a streak pre-requisite then announce it
     *  *TODO* this is possibly crashing the bot.
        Think the method over then give it a neat rewrite.

        @param playerID Player ID to check
    */
    private void getAnnounceStreak(int playerID) {
        //find streaking players, and announce them
        Player player = m_botAction.getPlayer(playerID);
        int shipType = player.getShipType();
        int numKills = player.getWins();

        for(int i = 1 ; i <= 5 ; i++)
        {
            if(numKills == (shipStreaks[shipType][i] * killMultiplier[maxDeaths]))
            {
                //Some streak target has been reached here
                m_botAction.sendArenaMessage(m_botAction.getPlayerName(playerID) +
                                             " " + streakText[i] + " with " + numKills + " kills in a " +
                                             shipTypes[shipType]) ;
            }
        }

    }

    /**
        Check to see if a new MVP is called for

        @param name Player ID
    */
    public void updateMVP(int name) {
        //Uses the formula Kills-(ShipType-1) [or it did before things were changed]
        Player player = m_botAction.getPlayer(name);
        int player_kills = player.getWins();

        if ((player_kills - player.getShipType()) > (MVPkills - (MVPship))) {
            MVPkills = player_kills;
            MVPname = player.getPlayerName();
            MVPship = player.getShipType();
        }
    }

    public boolean isUnloadable() {
        return true;
    }

    /**
        Cancel.
    */
    public void cancel() {
    }
}
