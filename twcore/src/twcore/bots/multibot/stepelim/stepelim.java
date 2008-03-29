package twcore.bots.multibot.stepelim;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/**
 * Step-Elim
 * 
 * Make-over of the stepelim twbotmodule (created by Austin)
 * @author fantus
 */
public class stepelim extends MultiModule {
    private boolean isRunning = false;
    private boolean firstRound = true;
    private boolean isBetweenRounds = false;
    private boolean keepDeaths = false;
    private boolean evenTeams = false;
    private boolean isUneven = false;
    private int shipType = 1;
    private int deathLimit = 2;
    private HashSet<String> teamOne;
    private HashSet<String> teamTwo;
    private HashSet<String> freqOne;
    private HashSet<String> freqTwo;
    
    /**
     * Initialize the module
     */
    public void init() {
        teamOne = new HashSet<String>();
        teamTwo = new HashSet<String>();
        freqOne = new HashSet<String>();
        freqTwo = new HashSet<String>();      
    }
    
    /**
     * This method checks to see if the module can be unloaded or not.
     * 
     * @param true is returned if the module is allowed to be unloaded.
     */
    public boolean isUnloadable() {
        return !isRunning;
    }
    
    /**
     * This method requests the events used by this module.
     */
    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request(this, EventRequester.PLAYER_DEATH);
        eventRequester.request(this, EventRequester.PLAYER_LEFT);
        eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
    }
    
    /**
     * This method checks to see what permissions a player has before allowing
     * them to execute certain commands
     * 
     * @param event the message sent to the bot
     */
    public void handleEvent(Message event) {
        String message = event.getMessage();
        if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            if (opList.isER(name))
                handleCommand(name, message, event.getPlayerID());
            else if (isBetweenRounds && isUneven)
                makeEven(event.getPlayerID());
        }
    }
    
    /**
     * This method checks to see what command was sent, and then acts
     * accordingly
     * 
     * @param name      the name of the player who issued the command
     * @param message   the command the player issued
     */
    public void handleCommand( String name, String message, short playerID) {
        if (message.toLowerCase().equals("!start"))
            startGame(name, "1");
        else if (message.toLowerCase().startsWith("!start")) 
            startGame(name, message.substring(7));
        else if (message.toLowerCase().equals("!stop"))
            stopGame(name);
        else if (message.toLowerCase().startsWith("!deathlimit"))
            setDeathLimit(name, message);
        else if (message.toLowerCase().equals("!eventeams")) 
            setEvenTeams(name);
        else if (message.toLowerCase().equals("!keepdeaths"))
            setKeepDeaths(name);
        else if (isBetweenRounds && isUneven)
            makeEven(playerID);
    }
    
    /**
     * This method is called when a PlayerDeath event fires. Every time a
     * player dies, we check to see if a round has ended.
     * 
     * @param event the PlayerDeath event that has fired
     */
    public void handleEvent(PlayerDeath event) {
        if (isRunning && !isBetweenRounds) {
            Player killed = m_botAction.getPlayer(event.getKilleeID());
                        
            if(killed.getLosses() >= deathLimit) {
                String killedName = killed.getPlayerName();
                int freq = killed.getFrequency();
                m_botAction.specWithoutLock(killedName);
                //set the player to his own teamfreq, only when keepDeaths is not set
                //A bit of team bonding here :)
                if (!keepDeaths)
                    m_botAction.setFreq(killed.getPlayerID(), freq);
                m_botAction.sendArenaMessage(killedName + " is out! (" + killed.getWins() + "-" 
                        + killed.getLosses() + ")");
                
                //check if it is the last round. If so the message to stick around does not need to be send.
                if (((killed.getFrequency() == 0 && teamOne.size() > 1) ||
                    (killed.getFrequency() == 1 && teamTwo.size() > 1)) &&
                    !keepDeaths) {
                    m_botAction.sendSmartPrivateMessage(killedName, "Stick around, if your " +
                            "team wins you'll get to play in the next round! But do not leave " +
                            "the arena or you will be removed from the playing list!");
                }
            }
        }
    }
    
    /**
     * This method is called when a FrequencyShipChange event fires. Every time
     * a player switches freq/specs, we need to check if a round has come to an end.
     * 
     * @param event the FrequencyShipChange event that fired
     */
    public void handleEvent(FrequencyShipChange event) {
        if (isRunning) {
            Player player = m_botAction.getPlayer(event.getPlayerID());
            String playerName = player.getPlayerName();
            //This allows players to enter when evenTeams is set and the bot is asking for an extra player
            if (!player.isShip(shipType)) {
                freqOne.remove(playerName);
                freqTwo.remove(playerName);
                // Check if a round has come to an end.
                checkEndRound();
            }
        }
    }
    
    /**
     * This method is called when a PlayerLeft event is fired. If a player
     * flat out leaves the arena without speccing first, the HashSets need
     * to be updated.
     * 
     * @param event the PlayerLeft event that fired
     */
    public void handleEvent(PlayerLeft event) {
        if (isRunning) {
            Player player = m_botAction.getPlayer(event.getPlayerID());
            String playerName = player.getPlayerName();
            teamOne.remove(playerName);
            teamTwo.remove(playerName);
            freqOne.remove(playerName);
            freqTwo.remove(playerName);
            
            // Check if a round has come to an end.
            checkEndRound();
        }
    }
    
    /**
     * This method starts a game of Step-Elim.
     * 
     * @param name the name of the player who issued the !start command
     * @param param the shiptype which will be played in
     */
    private void startGame(String name, String param) {
        //First do a few checks if the game should get started.
        if (!isRunning && m_botAction.getNumPlayers() < 2) {
            m_botAction.sendSmartPrivateMessage(name, "There are not enough players to begin this event.");
        } else if (!isRunning) {
            //Start the Game
            isRunning = true;
            
            //Determine shiptype
            try {
                shipType = Integer.parseInt(param);
            } catch (Exception e) {}
            if (shipType <1 || shipType > 8)
                //Just default to 1
                shipType = 1;
            
            m_botAction.sendSmartPrivateMessage(name,
                    "A round of Step-Elim is starting now! I will lock the arena after the 20 second warning.");
            m_botAction.sendArenaMessage("Step-Elim is starting! Players are eliminated after " +
                    deathLimit + " deaths. When a team has won it will be split up and the game will start " +
                    		"over until 1 person stands.", 2);
            
            if (keepDeaths)
                m_botAction.sendArenaMessage("NOTICE: " + name + " activated keep deaths mode! " +
                		"This means that you will not get a scorereset after each round!");
            
            m_botAction.sendArenaMessage("The arena will be locked in 20 seconds.");
            
            TimerTask waitTwentySeconds = new TimerTask() {
                public void run() {
                    m_botAction.toggleLocked();
                    m_botAction.changeAllShips(shipType);
                    beginRound();
                }
            };
            m_botAction.scheduleTask(waitTwentySeconds, (20 * 1000));
        } else
            m_botAction.sendSmartPrivateMessage(name, "A game is already in progress.");
    }
    
    private void preRound(HashSet<String> team) {
        Iterator<String> it = team.iterator();
        String name;
        while(it.hasNext()) {
            name = it.next();
            m_botAction.setShip(name, shipType);
            m_botAction.setFreq(name, 0);
        }
        beginRound();
    }
    
    /**
     * This method starts a new round.
     */
    private void beginRound() {
        isUneven = false;
        isBetweenRounds = true;
        
        //Players must not kill each other before a round started.
        m_botAction.changeAllInShipToFreq(shipType, 0);
        if (evenTeams) {
            if ((m_botAction.getNumPlayers() % 2) != 0)
                isUneven = true;
            if (isUneven && !firstRound) {
                m_botAction.sendArenaMessage("The teams are uneven, " +
                		"the first to pm me, " + m_botAction.getBotName() + 
                		",  will get to play to even the teams!!!", 3);
                return; //Escape, wait for a player to enter the game to make it even again
            }
        }
        
        //Game is ready to go
        m_botAction.sendArenaMessage("Next round will start in 10 seconds...", 2);
        
        TimerTask preRun = new TimerTask() {
            public void run() {
                m_botAction.createNumberOfTeams(2);
            }
        };
        m_botAction.scheduleTask(preRun, (9 * 1000));
        
        TimerTask tenSecondWarning = new TimerTask() {
            public void run() {
                setTeams();
                isBetweenRounds = false;
                if (!keepDeaths || firstRound)
                    m_botAction.scoreResetAll();
                m_botAction.sendArenaMessage("GO GO GO!!!", 104);
            }
        };
        m_botAction.scheduleTask(tenSecondWarning, (10 * 1000));
        firstRound = false;
    }

    /**
     * This method stops a game of Step-Elim. If no game is currently in
     * progress, it will return an error message.
     * 
     * @param name the name of the player who issued the !stop command
     */
    private void stopGame(String name) {
        if (isRunning) {
            isRunning = false;
            isBetweenRounds = false;
            firstRound = true;
            keepDeaths = false;
            evenTeams = false;
            isUneven = false;
            shipType = 1;
            deathLimit = 2;
            m_botAction.sendPrivateMessage(name, "Step-Elim mode stopped.");
            m_botAction.specAll();
            m_botAction.sendArenaMessage("This game of Step-Elim has been obliterated by " + name + ".", 13);
            m_botAction.toggleLocked();
            cancel();
        } else {
            m_botAction.sendPrivateMessage(name, "Error: Step-Elim mode is not currently running.");
        }
    }
    
    /**
     * This method sets the teams and freq sets up at the start of each round
     */
    private void setTeams() {
        teamOne.clear();
        teamTwo.clear();
        freqOne.clear();
        freqTwo.clear();
        
        Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
        while(it.hasNext()) {
            Player player = it.next();
            if(player.getFrequency() == 0) {
                teamOne.add(player.getPlayerName());
                freqOne.add(player.getPlayerName());
            }                
            else {
                teamTwo.add(player.getPlayerName());
                freqTwo.add(player.getPlayerName());
            }
        }
    }
    
    /**
     * This method checks if a round has ended. If a round has ended
     * it will start a new round or end the game.
     */
    private void checkEndRound() {
        if (freqOne.isEmpty() || freqTwo.isEmpty()) {
            int winningfreq = 0;
            //End of a round
            isBetweenRounds = true;
            
            if (freqOne.isEmpty())
                winningfreq = 1;
            else
                winningfreq = 0;
            
            m_botAction.sendArenaMessage("Freq " + winningfreq + " has won the round!", 20);
            
            TimerTask endRound = new TimerTask() {
                public void run() {
                    if (freqOne.isEmpty()) {
                        if (teamTwo.size() != 1) 
                            preRound(teamTwo);
                        else
                            endGame(teamTwo);
                    }
                    else {
                        if (teamOne.size() != 1) 
                            preRound(teamOne);
                        else
                            endGame(teamOne);
                    }
                }
            };
            
            //Wait 3 seconds between the announcement of the round winner and the next round start or end game
            m_botAction.scheduleTask( endRound, 3000);
        }
    }
    
    /**
     * This method ends the game and announces the winner
     * 
     * @param name the name of the winner of the game
     */
    private void endGame(HashSet<String> name) {
        Iterator<String> it = name.iterator();
        m_botAction.sendArenaMessage( "--= " + it.next() + " has won Step-Elim!!! =--", 5 );
        isRunning = false;
        isBetweenRounds = false;
        firstRound = true;
        keepDeaths = false;
        evenTeams = false;
        isUneven = false;
        shipType = 1;
        deathLimit = 2;
        m_botAction.toggleLocked();  
        cancel();      
    }
    
    /**
     * This method toggles deathlimit after a check if the game is already running. 
     * 
     * @param name the name of the player who issued the !deathlimit command 
     * @param message the command the player issued
     */
    private void setDeathLimit(String name, String message) {
        if (!isRunning) {
            try {
                deathLimit = Integer.parseInt(message.substring(12));
                m_botAction.sendSmartPrivateMessage(name, "Death limit set to " + deathLimit + ".");
            } catch(Exception e) {}
        }
        else
            m_botAction.sendSmartPrivateMessage(name, 
                    "Error: Can only set death limit when the game is not runnning. Use !stop first.");
    }
    
    /**
     * This method toggles eventeams after a check whether the game is already running.
     * 
     * @param name the name of the player who issued the !eventeams command
     */
    private void setEvenTeams(String name) {
        if (!isRunning) {
            evenTeams = !evenTeams;
            if (evenTeams)
                m_botAction.sendSmartPrivateMessage(name,
                        "Eventeams is toggled on! When teams are uneven the first player who messasges me get " +
                        "added.");
            else
                m_botAction.sendSmartPrivateMessage(name,
                        "Eventeams is toggled off! The game will just continue when teams are uneven. (Default)");
        }
        else
            m_botAction.sendSmartPrivateMessage(name, 
                    "Error: Can only toggle eventeams while not running. Use !stop first.");
    }
    
    /**
     * This method toggles keepdeaths after a check whether the game is already running.
     * 
     * @param name the name of the player who issued the !keepdeaths command
     */
    private void setKeepDeaths (String name) {
        if (!isRunning) {
            keepDeaths = !keepDeaths;
            if (keepDeaths)
                m_botAction.sendSmartPrivateMessage(name, 
                        "Keepdeaths is toggled on! Players will keep their deaths during the whole game.");
            else
                m_botAction.sendSmartPrivateMessage(name,
                        "Keepdeaths is toggled off! Players will get a scorereset each round. (Default)");
        }
        else
            m_botAction.sendSmartPrivateMessage(name, 
                    "Error: Can only toggle keepdeaths while not running. Use !stop first.");
    }
    
    /**
     * This method adds a player to the player list, and goes back to beginRound.
     * First it does a little check if the extra player is not already in the game to 
     * prevent cheating.
     * 
     * @param name the name of the player who private messaged me first
     */
    private void makeEven(short playerID) {
        Player player = m_botAction.getPlayer(playerID);
        String name = m_botAction.getPlayerName(playerID);
        //Check if player is a spectator
        if(player.isShip(0)) {
            m_botAction.setShip(name, shipType);
            m_botAction.setFreq(name, 0);
            m_botAction.scoreReset(name);
        }
        else
            return; //wait for the next player to message me
        isUneven = false;
        //wait a second to let the player enter the game
        TimerTask waitForPlayer = new TimerTask() {
          public void run() {
              beginRound();
          }
        };
        m_botAction.scheduleTask(waitForPlayer, 1000);
    }

    /**
     * This method displays the list of command when the !help command is
     * issued for this module
     * 
     * @return stepElimHelp the list of commands used by the module
     */
    public String[] getModHelpMessage() {
        String[] stepElimHelp = {
                "!start               -- Starts a game of Step-Elim with shiptype 1",
                "!start <shiptype>    -- Starts a game of Step-Elim with the specified shiptype.",
                "!deathlimit <#>      -- Sets death limit. (default is 2)",
                "!eventeams           -- Toggles whether to evenout the teams after each round.",
                "                        First one that pm's the bot gets added to make the teams even again.",
                "                        (default is off)",
                "!keepdeaths          -- Toggles whether to have scoreresets between rounds or not. (default is off)",
                "!stop                -- Stops a game of Step-Elim."
        };
        return stepElimHelp;
    }
    
    /**
     * This method is used to cancel any pending TimerTaks should the game be
     * !stop'd prematurely
     */
    public void cancel() {
        m_botAction.cancelTasks();
    }

    
}
