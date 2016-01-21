/*
    twbotbaseassault - Base Assault module - script (nblackman@gmail.com)

    Created on November 11, 2004 - Last modified January 2, 2005
*/


package twcore.bots.multibot.baseassault;

import java.util.Hashtable;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;


/**
    TWBot Extension for use in basing maps.  Lagouts and late adds will be
    possible. Consists of two teams. One team will begin the match with the base
    in their possession. The other team will try to capture the flag as quickly
    as possible. Once the seconds team succeeds, the two teams swap positions.
    Two game modes will exist: a) The team with the fastest time wins. b) the
    game will continue until a time has not been beat.

    @author  script
    @version 0.89
*/
public class baseassault extends MultiModule {

    /** Creates a new instance of twbotbaseassault */
    public void init() {
        makeMapsTable();
        confirmedMap = mapsTable.get("0");
    }

    public void requestEvents(ModuleEventRequester events)  {
        events.request(this, EventRequester.PLAYER_DEATH);
        events.request(this, EventRequester.PLAYER_LEFT);
        events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(this, EventRequester.FLAG_CLAIMED);
    }

    //info
    private String hostName;
    private Hashtable<String, BasingMap> mapsTable = new Hashtable<String, BasingMap>();

    //settings
    private int maxTime = 900000; //15 mins
    private int firstDefendingTeam = 0;
    private int firstAttackingTeam = 1;

    private boolean gameStarted = false;
    private boolean continuousMode = false;
    private boolean manual = false;

    private BasingMap confirmedMap;
    private TimerTask timeExpired;
    private boolean settingsConfirmed = false;
    private long startTime;
    private long endTime;
    private long fastestTime = maxTime;
    private int defendingTeam = firstDefendingTeam;
    private int attackingTeam = firstAttackingTeam;
    private int roundNum = 0;
    private boolean betweenRounds = false;

    /**
        Begins the game of Base Assault itself, after settings have been confirmed.
        @param name Name of the host who started Base Assault.
    */
    public void startBA(String name) {
        hostName = name;

        if (!gameStarted) {
            if (settingsConfirmed) {
                //BasingMap confirmedMap = (BasingMap) mapsTable.get(Integer.toString(selectedMap));

                if (!manual) {
                    //showRules();
                }

                m_botAction.sendArenaMessage("Base Assault begins in 30 secs...");

                TimerTask doStart = new TimerTask() {
                    public void run() {
                        m_botAction.resetFlagGame();
                        m_botAction.scoreResetAll();
                        warpPlayers();
                        startTime = System.currentTimeMillis();
                        gameStarted = true;
                        roundNum = 1;
                        m_botAction.sendArenaMessage("Base Assault has begun! Fight for the base!");
                        detectMaxTime();
                    }
                };

                m_botAction.scheduleTask(doStart, 30000);
            }

            else {
                m_botAction.sendPrivateMessage(hostName, "The settings must be confirmed before beginning Base Assault. Use !settings for more details.");
            }
        }
        else {
            m_botAction.sendPrivateMessage(hostName, "A game is already running!");
        }
    }

    /**
        Stops any currently running games.
    */
    public void stopBA() {
        gameStarted = false;
        roundNum = 0;
        settingsConfirmed = false;
        resetData();
        defaultSettings();
    }


    //--------------------------------------------------------------------------
    // General Methods
    //--------------------------------------------------------------------------


    /**
        Sets all settings to default.
    */
    public void defaultSettings() {
        firstDefendingTeam = 0;
        firstAttackingTeam = 1;
        attackingTeam = 0;
        defendingTeam = 1;
        maxTime = 900000;
        //selectedMap = 0;
        continuousMode = false;
    }


    /**
        Detects whether or not time has expired yet.
    */
    public void detectMaxTime() {
        long currentMaxMillis = maxTime;

        if (roundNum > 1) {
            currentMaxMillis = fastestTime;
        }

        timeExpired = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage("Time is up!");
                winner(defendingTeam);
            }
        };
        m_botAction.scheduleTask(timeExpired, currentMaxMillis);
    }


    /**
        Creates a hashTable of BasingMap objects.
    */
    public void makeMapsTable() {
        mapsTable.put("0", new BasingMap("Default Map", 310, 482, 512, 265));
        mapsTable.put("1", new BasingMap("Weird Base", 314, 703, 511, 262));
    }

    /**
        Converts milliseconds to minutes.
        @param millis The number of milliseconds to convert to minutes.
        @return Returns a string with the conversion result.
    */
    public String millisToMinutes(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long remainingSecs = seconds - (minutes * 60);
        String outputStr = minutes + " minutes " + remainingSecs + " seconds";
        return outputStr;
    }


    /**
        Starts the next round of Base Assault.
    */
    public void newRound() {
        betweenRounds = true;
        long elapsedTime = endTime - startTime;
        int tempNum = attackingTeam;
        attackingTeam = defendingTeam;
        defendingTeam = tempNum;
        m_botAction.sendArenaMessage("Round " + roundNum + " is over! The base was conquered in " + millisToMinutes(elapsedTime));
        roundNum++;
        m_botAction.sendArenaMessage("Round " + roundNum + " begins in 30 seconds.");
        TimerTask nextRound = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage("Round " + roundNum + " has begun!");
                detectMaxTime();
                m_botAction.resetFlagGame();
                warpPlayers();
                startTime = System.currentTimeMillis();
                betweenRounds = false;
            }
        };
        m_botAction.scheduleTask(nextRound, 30000);
    }


    /**
        Resets all data.
    */
    public void resetData() {
        fastestTime = maxTime;
        startTime = 0;
        endTime = 0;
    }


    /**
        Used to create a custom map as specified by the host.
        @param mapName Name of the map.
        @param x1 X coordinate of the safe zone.
        @param y1 Y coordinate of the safe zone.
        @param x2 X coordinate of the flag room.
        @param y2 Y coordinate of the flag room.
    */
    public void createMap(String mapName, int x1, int y1, int x2, int y2) {
        int numMaps = mapsTable.size();
        int newNum = numMaps + 1;
        mapsTable.put(Integer.toString(newNum), new BasingMap(mapName, x1, y1, x2, y2));
    }


    /**
        Used to warp players to the appropriate coordinates.
    */
    public void warpPlayers() {
        int flagRoomX = confirmedMap.getFlagRoomX();
        int flagRoomY = confirmedMap.getFlagRoomY();
        int safeX = confirmedMap.getSafeX();
        int safeY = confirmedMap.getSafeY();
        m_botAction.warpFreqToLocation(defendingTeam, flagRoomX, flagRoomY);
        m_botAction.warpFreqToLocation(attackingTeam, safeX, safeY);
    }

    /**
        Takes the appropriate action if a winner is detected.
        @param freqNum The number of the winning frequency.
    */
    public void winner(int freqNum) {
        if (roundNum > 1) {
            m_botAction.sendArenaMessage("Game over! Freq " + freqNum + " wins after round " + roundNum + " with the fastest time: " + millisToMinutes(fastestTime));
        }
        else if (roundNum == 1) {
            m_botAction.sendArenaMessage("Freq " + attackingTeam + " could not conquer the base in time! Freq " + freqNum + " wins by default!");
        }

        stopBA();
    }


    //--------------------------------------------------------------------------
    // Command Methods
    //--------------------------------------------------------------------------


    /**
        Creates a new map from the command's parameters.
        @param message The message originally sent to the bot.
        @param name The command's submitter.
    */
    public void newMap(String message, String name) {
        try {
            String data = message.trim().substring(8);

            int firstComma = data.indexOf(",");
            int secondComma = data.substring(firstComma + 1).indexOf(",") + firstComma + 1;
            int thirdComma = data.substring(secondComma + 1).indexOf(",") + secondComma + 1;
            int fourthComma = data.substring(thirdComma + 1).indexOf(",") + thirdComma + 1;

            String mapName = data.substring(0, firstComma);

            int x1 = Integer.parseInt(data.substring(firstComma + 1, secondComma));
            int y1 = Integer.parseInt(data.substring(secondComma + 1, thirdComma));
            int x2 = Integer.parseInt(data.substring(thirdComma + 1, fourthComma));
            int y2 = Integer.parseInt(data.substring(fourthComma + 1));

            BasingMap tempMap = new BasingMap(mapName, x1, y1, x2, y2);
            int numMaps = mapsTable.size();
            mapsTable.put("" + numMaps, tempMap);

            m_botAction.sendPrivateMessage(name, "New map created.");
        }
        catch (Exception e) {
            m_botAction.sendPrivateMessage(name, "Syntax error.");
        }
    }


    /**
        Sends the host a list of available maps to select from.
        @param name The command's submitter.
    */
    public void sendMaps(String name) {
        int numMaps = mapsTable.size();
        m_botAction.sendPrivateMessage(name, "Currently available maps: ");
        m_botAction.sendPrivateMessage(name, "------------------------- ");

        for (int i = 0; i < numMaps; i++) {
            BasingMap tempMap = mapsTable.get(Integer.toString(i));
            m_botAction.sendPrivateMessage(name, i + ". " + tempMap);
        }
    }


    /**
        Displays the currently selected settings. Asks the host to confirm them if he has yet to do so.
        @param name Name of player to whom the message will be sent.
    */
    public void sendSettings(String name) {
        //BasingMap tempMap = (BasingMap) mapsTable.get(Integer.toString(selectedMap));
        String maxTimeMinutes = millisToMinutes(maxTime);
        m_botAction.sendPrivateMessage(name, "Your current settings: ");
        m_botAction.sendPrivateMessage(name, "----------------------");
        m_botAction.sendPrivateMessage(name, "Map: " + confirmedMap);
        m_botAction.sendPrivateMessage(name, (continuousMode) ? "Number of rounds: Unlimited" : "Number of rounds: Two");
        m_botAction.sendPrivateMessage(name, "First Defending Freq: " + firstDefendingTeam);
        m_botAction.sendPrivateMessage(name, "First Attacking Freq: " + firstAttackingTeam);
        m_botAction.sendPrivateMessage(name, "Maximum Time to take over base: " + maxTimeMinutes);

        if (!settingsConfirmed) {
            m_botAction.sendPrivateMessage(name, "PM me with !confirm if these settings are correct.");
        }
    }


    /**
        Displays the status of the current game.
        @param name Name of player to whom the message will be sent.
    */
    public void sendStatus(String name) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        m_botAction.sendPrivateMessage(name, "Freq " + attackingTeam + " is attacking in round " + roundNum + ". Elapsed time: " + millisToMinutes(elapsedTime) + ". Time to beat: " + millisToMinutes(fastestTime));
    }


    /**
        Changes which team will be first to attack.
        @param name Name of the host who sent the command.
        @param message Content of the command sent by the host.
    */
    public void setAttacker(String name, String message) {
        String newFreq = message.trim().substring(13);

        try {
            firstAttackingTeam = Integer.parseInt(newFreq);
            attackingTeam = firstAttackingTeam;
            m_botAction.sendPrivateMessage(name, "First attacking frequency changed to " + firstAttackingTeam);
        }
        catch (Exception NumberFormatException) {
            m_botAction.sendPrivateMessage(name, "Freq must be a number.");
        }
    }


    /**
        Changes which team will be first to defend.
        @param name Name of the host who sent the command.
        @param message Content of the command sent by the host.
    */
    public void setDefender(String name, String message) {
        String newFreq = message.trim().substring(10);

        try {
            firstDefendingTeam = Integer.parseInt(newFreq);
            defendingTeam = firstDefendingTeam;
            m_botAction.sendPrivateMessage(name, "First defending frequency changed to " + firstDefendingTeam);
        }
        catch (Exception NumberFormatException) {
            m_botAction.sendPrivateMessage(name, "Freq must be a number.");
        }
    }


    /**
        Changes the selected map to the one specified by the host.
        @param name Name of the host who sent the command.
        @param message Content of the command sent by the host.
    */
    public void setMap(String name, String message) {
        String mapIndex = message.trim().substring(8);

        if (mapsTable.containsKey(mapIndex)) {
            confirmedMap = mapsTable.get(mapIndex);
            m_botAction.sendPrivateMessage(name, "Map number " + mapIndex + " selected.");
        }
        else {
            m_botAction.sendPrivateMessage(name, "Specified map does not exist.");
        }
    }


    /**
        Changes the maxTime to the time specified by the host.
        @param name Name of the host who sent the command.
        @param message Content of the command sent by the host.
    */
    public void setMaxTime(String name, String message) {
        String newMaxTime = message.trim().substring(9);

        try {
            maxTime = Integer.parseInt(newMaxTime);
            fastestTime = maxTime;
            m_botAction.sendPrivateMessage(name, "Maximum time changed to " + maxTime);
        }
        catch (Exception NumberFormatException) {
            m_botAction.sendPrivateMessage(name, "Time must be a number.");
        }
    }


    /**
        Displays the Base Assault rules in an arena message.
    */
    public void showRules() {
        String currentMode = continuousMode ? "B." : "A.";
        String[] rules = {
            "|                      --  General Rules --                         |",
            "|   Base Assault Consists of two teams--one begins with the base    |",
            "|   and the other tries to conquer it as quickly as possible.       |",
            "|   Once the base is conquered, the teams swap positions. Two       |",
            "|   game modes exist: a) Two rounds- the team with the fastest      |",
            "|   time wins. b) The game will continue until a time has not       |",
            "|   been beaten. The current mode is Mode " + currentMode
        };

        for (int i = 0; i < rules.length; i++) {
            m_botAction.sendArenaMessage(rules[i]);
        }
    }


    /**
        Warps host to selected coordinates for testing purposes.
        @param name Name of the host who will be warped.
    */
    public void testWarp(final String name) {
        m_botAction.sendPrivateMessage(name, "Testing coordinates for " + confirmedMap);
        TimerTask safeWarp = new TimerTask() {
            public void run() {
                m_botAction.warpTo(name, confirmedMap.getSafeX(), confirmedMap.getSafeY());
                m_botAction.sendPrivateMessage(name, "Current coords for safe zone: " + confirmedMap.getSafeX() + ", " + confirmedMap.getSafeY());
            }
        };
        TimerTask frWarp = new TimerTask() {
            public void run() {
                m_botAction.warpTo(name, confirmedMap.getFlagRoomX(), confirmedMap.getFlagRoomY());
                m_botAction.sendPrivateMessage(name, "Current coords for flag room: " + confirmedMap.getFlagRoomX() + ", " + confirmedMap.getFlagRoomY());
            }
        };
        m_botAction.scheduleTask(safeWarp, 3000);
        m_botAction.scheduleTask(frWarp, 10000);
    }


    //--------------------------------------------------------------------------
    // Event Handling
    //--------------------------------------------------------------------------


    /**
        Handles event received message, and if from an ER or above, tries to
        parse it as an event mod command. Otherwise, parses as a general command.
        @param event Passed event.
    */
    public void handleEvent(Message event) {
        String message = event.getMessage();

        if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
            String name = m_botAction.getPlayerName(event.getPlayerID());

            if (opList.isER(name)) {
                handleCommand(name, message);
            }
            else {
                handlePublicCommand(name, message);
            }
        }
    }


    /**
        Handles event player death.
        @param event Passed event.
    */
    public void handleEvent(PlayerDeath event) {
    }

    /**
        Handles player leaving events. Details still to be sorted out.
        @param event Contains event information on player who left.
    */
    public void handleEvent(PlayerLeft event) {
    }

    /**
        Handles player changing ship/freq events.
        @param event Contains event information on player who changed ship or freq.
    */
    public void handleEvent(FrequencyShipChange event) {
    }


    /**
        Handles flag claimed event. Used to detect the moment of conquering.
        @param event Contains event information on who claimed the flag.
    */
    public void handleEvent(FlagClaimed event) {
        Player tempPlayer = m_botAction.getPlayer(event.getPlayerID());

        if (tempPlayer.getFrequency() == attackingTeam && gameStarted && !betweenRounds) {
            m_botAction.cancelTask(timeExpired);
            endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;

            if (roundNum > 2) {
                if (elapsedTime < fastestTime) {
                    fastestTime = elapsedTime;
                    newRound();
                }
                else {
                    winner(defendingTeam);
                }
            }

            if (roundNum == 2) {
                if (!continuousMode) {
                    if (elapsedTime < fastestTime) {
                        fastestTime = elapsedTime;
                        winner(attackingTeam);
                    }
                    else {
                        winner(defendingTeam);
                    }
                }

                if (continuousMode) {
                    if (elapsedTime < fastestTime) {
                        fastestTime = elapsedTime;
                        newRound();
                    }
                    else {
                        winner(defendingTeam);
                    }
                }
            }

            if (roundNum == 1) {
                fastestTime = elapsedTime;
                newRound();
            }
        }
    }


    /** Handles commands sent by a bot owner.
        @param name Name of the player who submitted the command.
        @param message The message that the player submitted to the bot.
    */
    public void handleCommand(String name, String message) {
        //---------------
        // !start
        //---------------
        if (message.toLowerCase().startsWith("!start")) {
            if (!gameStarted) {
                startBA(name);
            }
        }

        //--------------
        // !manual
        //--------------
        else if (message.toLowerCase().equals("!manual")) {
            //manual = !manual;
            //m_botAction.sendPrivateMessage(name, (manual) ? "Manual has been been turned on." : "Manual has been turned off");
        }

        //--------------
        // !setmap <num>
        //--------------
        else if (message.toLowerCase().startsWith("!setmap ")) {
            if (!gameStarted) {
                setMap(name, message.toLowerCase());
            }
        }

        //--------------
        // !maps
        //--------------
        else if (message.toLowerCase().equals("!maps")) {
            sendMaps(name);
        }

        //--------------
        // !confirm
        //--------------
        else if (message.toLowerCase().equals("!confirm")) {
            if (!gameStarted) {
                settingsConfirmed = !settingsConfirmed;
                m_botAction.sendPrivateMessage(name, (settingsConfirmed) ? "Settings have been confirmed." : "Settings no longer confirmed.");
            }
        }

        //---------------
        // !firstattack <freq>
        //---------------
        else if (message.toLowerCase().startsWith("!firstattack ")) {
            if (!gameStarted) {
                setAttacker(name, message.toLowerCase());
            }
            else {
                m_botAction.sendPrivateMessage(name, "Settings cannot be altered while a game is in progress.");
            }
        }

        //---------------
        // !firstdef <freq>
        //---------------
        else if (message.toLowerCase().startsWith("!firstdef ")) {
            if (!gameStarted) {
                setDefender(name, message.toLowerCase());
            }
            else {
                m_botAction.sendPrivateMessage(name, "Settings cannot be altered while a game is in progress.");
            }
        }

        //---------------
        // !maxtime <time>
        //---------------
        else if (message.toLowerCase().startsWith("!maxtime ")) {
            if (!gameStarted) {
                setMaxTime(name, message.toLowerCase());
            }
            else {
                m_botAction.sendPrivateMessage(name, "Settings cannot be altered while a game is in progress.");
            }
        }

        //---------------
        // !mode
        //---------------
        else if (message.toLowerCase().equals("!mode")) {
            if (!gameStarted) {
                continuousMode = !continuousMode;
                m_botAction.sendPrivateMessage(name, (continuousMode) ? "Number of rounds set to unlimited." : "Number of rounds set to two.");
            }
        }

        //---------------
        // !rules
        //---------------
        else if (message.toLowerCase().equals("!rules")) {
            showRules();
        }

        //---------------
        // !settings
        //---------------
        else if (message.toLowerCase().equals("!settings")) {
            sendSettings(name);
        }

        //---------------
        // !stop
        //---------------
        else if (message.toLowerCase().startsWith("!stop")) {
            if (gameStarted) {
                m_botAction.sendArenaMessage("Current game of Base Assault has been aborted.");
                stopBA();
            }
        }

        //----------------
        // !testwarp
        //----------------
        else if (message.toLowerCase().equals("!testwarp")) {
            if (!gameStarted) {
                testWarp(name);
            }
        }

        //----------------
        // !status
        //----------------
        else if (message.toLowerCase().equals("!status")) {
            if (gameStarted) {
                sendStatus(name);
            }
        }

        //----------------
        // !newmap
        //----------------
        else if (message.toLowerCase().startsWith("!newmap")) {
            if (!gameStarted) {
                newMap(message, name);
            }
        }
    }

    /**
        Handles all general commands given to the bot.
        @param name Name of player who sent the command.
        @param message Message sent
    */
    public void handlePublicCommand(String name, String message) {

        //-------------
        // !help
        //-------------
        if (message.toLowerCase().startsWith("!help")) {
            m_botAction.privateMessageSpam(name, getGenHelp());
        }

        //----------------
        // !status
        //----------------
        else if (message.toLowerCase().equals("!status")) {
            if (gameStarted) {
                sendStatus(name);
            }
        }

        //----------------
        // !host
        //----------------
        else if (message.toLowerCase().equals("!host")) {
            m_botAction.sendPrivateMessage(name, "Current host is " + hostName + ".");
        }

    }


    //*************************************************************************
    // BasingMap private class
    //*************************************************************************
    private class BasingMap {
        private String mapName;
        private int safeX;
        private int safeY;
        private int flagRoomX;
        private int flagRoomY;

        public BasingMap(String name, int X1, int Y1, int X2, int Y2) {
            mapName = name;
            safeX = X1;
            safeY = Y1;
            flagRoomX = X2;
            flagRoomY = Y2;
        }

        public int getSafeX() {
            return safeX;
        }

        public int getSafeY() {
            return safeY;
        }

        public int getFlagRoomX() {
            return flagRoomX;
        }

        public int getFlagRoomY() {
            return flagRoomY;
        }

        public String toString() {
            return mapName;
        }
    }

    //--------------------------------------------------------------------------
    // Display Info / Help
    //--------------------------------------------------------------------------

    /**
        Gets the module help for bot owners.
        @return Returns a string array consisting of module help information.
    */
    public String[] getModHelpMessage() {
        String[] help = {
            "Commands for the BA module (Author: script)",
            "!start                          - Begins a match of Base Assault if the settings have been confirmed.",
            "!stop                           - Stops any current Base Assault matches.",
            "!settings                       - Displays the current settings. These must be confirmed to begin Base Assault.",
            "   !maps                        - Displays the list of maps to choose from.",
            "   !setmap <num>                - Changes the selected map.",
            "   !newmap name,x1,y1,x2,y2     - Used to create a custom map.",
            "   !testwarp                    - Warps you to the map's associated coordinates to ensure the settings are correct.",
            "   !maxtime <time>              - Changes the maxtime to specified time in milliseconds.",
            "   !mode                        - Alternates between unlimited rounds and two rounds.",
            "   !firstdef <freq>             - Changes which freq will be first to defend.",
            "   !firstattack <freq>          - Changes which freq will be first to attack.",
            "   !confirm                     - Used to confirm the settings.",
            //"!manual                         - Toggles a few automatic hosting features on or off (default is on)",
        };
        return help;
    }

    /**
        getGenHelp
        @return Returns a String array of the help message.
    */
    public String[] getGenHelp() {
        String[] help = {
            "|---------------------------------------------------------------|",
            "|  Commands directed privately:                                 |",
            "|   !status        - Tells the status of the game.              |",
            "|   !host          - Tells who the current host is.             |",
            "|---------------------------------------------------------------|"
        };
        return help;
    }

    /**
        Empty method.
    */
    public void cancel() {
    }

    public boolean isUnloadable() {
        return true;
    }

}
