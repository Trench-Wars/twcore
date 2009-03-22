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
 * 
 */
public class stepelim extends MultiModule {
    private int state;
    //States
    private final static int STOP = 0;
    private final static int ROUND_PRE = 1;
    private final static int ROUND_RUN = 2;
    private final static int ROUND_END = 3;
    
    //Game variables
    private int shipType;
    private int deathLimit;
    private int roundNumber;
    private boolean keepDeaths;
    private boolean evenTeams;
    
    //Teams
    private HashSet<String> teamOne;
    private HashSet<String> teamTwo;
    
    //Timers
    private TimerCreateTwoTeams timerCreateTwoTeams;
    private TimerGoSignal timerGoSignal;
    private TimerRoundEnd timerRoundEnd;
    private TimerWaitForPlayer timerWaitForPlayer;
    
    //Frequencies
    private final static int FREQ_TEAMONE = 0;
    private final static int FREQ_TEAMTWO = 1;
    
    //Other
    private boolean lockArena;
    private boolean isUneven;
    
    
    /**
     * This method is used to cancel any pending TimerTaks should the game be
     * !stop'd prematurely
     */
    public void cancel() {
        m_botAction.cancelTasks();
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
                "!pmrules             -- Lists rules in a pm.",
                "!displayrules        -- Lists rules in a arena message.",        
                "!deathlimit <#>      -- Sets death limit. (default is 2)",
                "!eventeams           -- Toggles whether to evenout the teams after each round.",
                "                        First one that pm's the bot gets added to make the teams even again.",
                "                        (default is off)",
                "!keepdeaths          -- Toggles whether to have scoreresets between rounds or not. (default is off)",
                "!stop                -- Stops a game of Step-Elim.",
                "NOTICE: You don't have to set up the teams or lock the arena, this bot does it for you.. All you have to do is: 1) Display the rules 2) (Optional) configure some variables 3) !start."
        };
        return stepElimHelp;
    }
    
    /**
     * This method checks what command was send and acts accordingly.
     * 
     * @param name player who issued the command
     * @param message the command that the player issued
     */
    public void handleCommand(String name, String message) {
        if (message.toLowerCase().equals("!start"))
            cmdStart(name);
        else if (message.toLowerCase().startsWith("!start")) 
            cmdStart(name, message.substring(7));
        else if (message.toLowerCase().equals("!stop"))
            cmdStop(name);
        else if (message.toLowerCase().startsWith("!deathlimit"))
            cmdDeathLimit(name, message.substring(12));
        else if (message.toLowerCase().equals("!eventeams")) 
            cmdEvenTeams(name);
        else if (message.toLowerCase().equals("!keepdeaths"))
            cmdKeepDeaths(name);
        else if (message.toLowerCase().equals("!displayrules"))
            cmdDisplayRules();
        else if (message.toLowerCase().equals("!pmrules"))
            cmdPmRules(name);
        else if (state == ROUND_PRE && isUneven)
            makeEven(name);
    }
    
    /**
     * This method is called when a FrequencyShipChange event fires. Every time
     * a player switches freq/specs, we need to check if a round has come to an end.
     * 
     * @param event the FrequencyShipChange event that fired
     */
    public void handleEvent(FrequencyShipChange event) {
        if (state == ROUND_RUN) {
            Player player;
            
            player = m_botAction.getPlayer(event.getPlayerID());
            
            if (!player.isShip(shipType))
                roundCheckEnd();
        }
    }
    
    /**
     * This method checks to see what permissions a player has before allowing
     * them to execute certain commands
     * 
     * @param event the message sent to the bot
     */
    public void handleEvent(Message event) {
        //Handle private message
        if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
            if (opList.isER(m_botAction.getPlayerName(event.getPlayerID())))
                handleCommand(m_botAction.getPlayerName(event.getPlayerID()), event.getMessage());
            else if (state == ROUND_PRE && isUneven)
                makeEven(m_botAction.getPlayerName(event.getPlayerID()));
        }
        
        //ArenaLock-o-matic
        if(event.getMessageType() == Message.ARENA_MESSAGE) {
            if(event.getMessage().equals("Arena UNLOCKED") && lockArena)    //Arena should be Locked
                m_botAction.toggleLocked();
            if(event.getMessage().equals("Arena LOCKED") && !lockArena)     //Arena should be unlocked
                m_botAction.toggleLocked();
        }
    }
    
    /**
     * This method is called when a PlayerDeath event fires. Every time a
     * player dies, we need to check if a round has come to an end.
     * 
     * @param event the PlayerDeath event that has fired
     */
    public void handleEvent(PlayerDeath event) {
        if (state == ROUND_RUN) {
            Player killed = m_botAction.getPlayer(event.getKilleeID());
            
            if (killed.getLosses() >= deathLimit) {
                String killedName;
                int freq;
                
                killedName = killed.getPlayerName();
                freq = killed.getFrequency();
                
                m_botAction.specWithoutLock(killedName);
                
                //set the player to his own teamfreq, only when keepDeaths is not set
                //A bit of team bonding here :)
                if (!keepDeaths)
                    m_botAction.setFreq(killed.getPlayerID(), freq);
                m_botAction.sendArenaMessage(killedName + " is out! (" + killed.getWins() + "-" 
                        + killed.getLosses() + ")");
                
                //Remove player completely if its keepDeaths mode
                if (keepDeaths) {
                    teamOne.remove(killedName);
                    teamTwo.remove(killedName);
                }
                    
                
                //check if it is the last round. If so the message to stick around does not need to be send.
                if (((killed.getFrequency() == FREQ_TEAMONE && teamOne.size() > 1) ||
                    (killed.getFrequency() == FREQ_TEAMTWO && teamTwo.size() > 1)) &&
                    !keepDeaths) {
                    m_botAction.sendSmartPrivateMessage(killedName, "Stick around, if your " +
                            "team wins you'll get to play in the next round! But do not leave " +
                            "the arena or you will be removed from the playing list!");
                }
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
        if (state != STOP) {
            String playerName;
            
            playerName = m_botAction.getPlayerName(event.getPlayerID());
            
            teamOne.remove(playerName);
            teamTwo.remove(playerName);
            
            if (state == ROUND_RUN)
                roundCheckEnd();
        }
    }
    
    /**
     * Initialize the module
     */
    public void init() {
        //Teams
        teamOne = new HashSet<String>();
        teamTwo = new HashSet<String>();
        
        //Game variables
        shipType = 1;
        deathLimit = 2;
        keepDeaths = false;
        evenTeams = false;
        roundNumber = 1;
        
        //Timers
        timerCreateTwoTeams = new TimerCreateTwoTeams();
        timerGoSignal = new TimerGoSignal();
        timerRoundEnd = new TimerRoundEnd();
        timerWaitForPlayer = new TimerWaitForPlayer();
        
        //Other variables
        lockArena = false;
        isUneven = false;
    }
    
    /**
     * This method checks to see if the module can be unloaded or not.
     * 
     * @param true is returned if the module is allowed to be unloaded.
     */
    public boolean isUnloadable() {
        boolean isUnloadable;
        
        if (state == STOP)
            isUnloadable = true;
        else 
            isUnloadable = false;
        
        return isUnloadable;
    }

    /**
     * This method requests the events used by this module.
     */
    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        eventRequester.request(this, EventRequester.PLAYER_DEATH);
        eventRequester.request(this, EventRequester.PLAYER_LEFT);
    }
    
    /**
     * This command safely locks the arena.
     */
    private void arenaLock() {
        lockArena = true;
        m_botAction.toggleLocked();
    }
    
    /**
     * This command safely unlocks the arena.
     */
    private void arenaUnlock() {
        lockArena = false;
        m_botAction.toggleLocked();
    }
    
    /**
     * This method sets the deathlimit after a check if the game is already running.
     * 
     * @param name name of the player who issued the !deathlimit command
     * @param param deathlimit
     */
    private void cmdDeathLimit(String name, String param) {
        if (state == STOP) {
            try {
                deathLimit = Integer.parseInt(param);
                m_botAction.sendSmartPrivateMessage(name, "Death limit set to " + deathLimit + ".");
            } catch(Exception e) {}
        }
        else
            m_botAction.sendSmartPrivateMessage(name, 
                    "Error: Can only set death limit when the game is not runnning. Use !stop first.");
    }
    
    /**
     * This command displays the rules for step-elim.
     */
    private void cmdDisplayRules() {
        m_botAction.sendArenaMessage("RULES: Two teams battle against elimination. " +
        		"The winning team will get split up again in two seperate teams. " +
        		"This process continues until only one person is left.", 2);
    }
    
    /**
     * This method toggles eventeams after a check whether the game is already running.
     * 
     * @param name the name of the player who issued the !eventeams command
     */
    private void cmdEvenTeams(String name) {
        if (state == STOP) {
            evenTeams = !evenTeams;
            if (evenTeams)
                m_botAction.sendSmartPrivateMessage(name,
                        "Eventeams is toggled on! When teams are uneven the first player who messasges me gets " +
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
    private void cmdKeepDeaths(String name) {
        if (state == STOP) {
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
     * This method sends the rules to the player who issued the !pmrules command
     * 
     * @param name the name of the player who issued the !pmrules command
     */
    private void cmdPmRules(String name) {
        m_botAction.sendPrivateMessage(name, "RULES: Two teams battle against elimination. " +
                "The winning team will get split up again in two seperate teams. " +
                "This process continues until only one person is left.");
    }
    
    /**
     * This method starts a game after the !start command has been issued.
     * 
     * @param name name of the player who issued the !start command
     */
    private void cmdStart(String name) {
        if (state == STOP) {
            if (m_botAction.getNumPlaying() < 2)
                m_botAction.sendSmartPrivateMessage(name, "There are not enough players to begin this event.");
            else {
                gameStart();
            }   
        }
    }
    
    /**
     * This method starts a game after the !start command has been issued.
     * 
     * @param name name of the player who issued the !start command
     * @param param shipType
     */
    private void cmdStart(String name, String param) {
        if (state == STOP) {
            if (m_botAction.getNumPlaying() < 2)
                m_botAction.sendSmartPrivateMessage(name, "There are not enough players to begin this event.");
            else {
                //Determine shiptype
                try { 
                    shipType = Integer.parseInt(param);
                } catch (Exception e) {}
                if (shipType <1 || shipType > 8)
                    shipType = 1;
                gameStart();
            }   
        }
    }
    
    /**
     * This method stops a game of Step-Elim. If no game is currently in
     * progress, it will return an error message.
     * 
     * @param name the name of the player who issued the !stop command
     */
    private void cmdStop(String name) {
        if (state != STOP) {
            gameStop();
            m_botAction.sendPrivateMessage(name, "Step-Elim mode stopped.");
            m_botAction.sendArenaMessage("This game of Step-Elim has been obliterated by " + name + ".", 13);
        } else
            m_botAction.sendPrivateMessage(name, "Error: Step-Elim mode is not currently running.");
    }
    
    /**
     * This method declares the winner of stepelim.
     * 
     * @param team winning team
     */
    private void gameEnd(HashSet<String> team) {
        Iterator<String> itTeam;
        Iterator<Player> itPlayer;
        String winner;
        
        itTeam = team.iterator();
        itPlayer = m_botAction.getPlayingPlayerIterator();
        
        if (keepDeaths)
            winner = itPlayer.next().getPlayerName();
        else
            winner = itTeam.next();
            
        m_botAction.sendArenaMessage( "--= " + winner + " has won Step-Elim!!! =--", 5 );
        gameStop();
    }
    
    /**
     * This method starts the game.
     */
    private void gameStart() {
        m_botAction.sendArenaMessage("Step-Elim is starting! Players are eliminated after " +
                deathLimit + " deaths. When a team has won it will be split up and the game will start " +
                        "over until 1 person stands.", 2);
        
        if (keepDeaths)
            m_botAction.sendArenaMessage("NOTICE: Keepdeaths-mode actived! " +
                    "This means that you will not get a scorereset after each round!");
        
        arenaLock();
        
        m_botAction.shipResetAll();
        m_botAction.changeAllShips(shipType);
        roundStart();
    }
    
    /**
     * This method stops the game.
     */
    private void gameStop() {
        state = STOP;
        
        m_botAction.specAll();
        reset();
        arenaUnlock();
    }

    /**
     * This method adds a player to the player list, and goes back to roundStart().
     * First it does a little check if the extra player is not already in the game to 
     * prevent cheating.
     * 
     * @param name the name of the player who private messaged me first
     */
    private void makeEven(String name) {
        Player player = m_botAction.getPlayer(name);
        if(player.isShip(0)) {
            m_botAction.setShip(name, shipType);
            m_botAction.setFreq(name, 0);
            m_botAction.scoreReset(name);
            isUneven = false;
            timerWaitForPlayer = new TimerWaitForPlayer();
            m_botAction.scheduleTask(timerWaitForPlayer, 1000);
        }
    }
    
    /**
     * This method resets all variables and timers.
     */
    private void reset() {
        //Teams
        teamOne.clear();
        teamTwo.clear();
        
        //Game variables
        shipType = 1;
        deathLimit = 2;
        keepDeaths = false;
        evenTeams = false;
        roundNumber = 1;
        
        //Other variables
        lockArena = false;
        isUneven = false;
        
        //Cancel all timers
        m_botAction.cancelTasks();
    }
    
    /**
     * This method checks if a round has ended.
     */
    private void roundCheckEnd() {
        int teamOneSize;
        int teamTwoSize;
        
        teamOneSize = m_botAction.getPlayingFrequencySize(FREQ_TEAMONE);
        teamTwoSize = m_botAction.getPlayingFrequencySize(FREQ_TEAMTWO);
        
        if ((teamOneSize == 0) || (teamTwoSize == 0))
            roundEnd();
    }
    
    /**
     * This method handles a round end. (ie. declares round winner and continues the game to the next step.)
     */
    private void roundEnd() {
        int winningFreq;
        int teamOneSize;
        
        state = ROUND_END;
        
        teamOneSize = m_botAction.getPlayingFrequencySize(FREQ_TEAMONE);
        if (teamOneSize == 0)
            winningFreq = FREQ_TEAMTWO;
        else
            winningFreq = FREQ_TEAMONE;
        
        m_botAction.sendArenaMessage("Freq " + winningFreq + " has won the round!", 20);
        timerRoundEnd = new TimerRoundEnd();
        m_botAction.scheduleTask(timerRoundEnd, (3 * 1000));
    }
    
    /**
     * This method makes sure that everyone who was in the winning team gets in again.
     * 
     * @param team the winning team of the last round
     */
    private void roundPre(HashSet<String> team) {
        Iterator<String> it;
        String name;
        
        state = ROUND_PRE;
        
        it = team.iterator();
        while(it.hasNext()) {
            name = it.next();
            m_botAction.setShip(name, shipType);
            m_botAction.setFreq(name, 0);
        }
        //Wait one second to be sure that everyone is in.
        timerWaitForPlayer = new TimerWaitForPlayer();
        m_botAction.scheduleTask(timerWaitForPlayer, (1 * 1000));
    }
    
    /**
     * This method begins a new round.
     */
    private void roundStart() {
        state = ROUND_PRE;
        
        m_botAction.changeAllInShipToFreq(shipType, 0);
        
        if (evenTeams && roundNumber != 1) {
            if ((m_botAction.getNumPlaying() % 2) != 0)
                isUneven = true;
            else
                isUneven = false;
            
            if (isUneven) {
                m_botAction.sendArenaMessage("The teams are uneven, " +
                        "the first to pm me, " + m_botAction.getBotName() + 
                        ",  will get to play to even the teams!!! ", 3);
                return; //Escape, wait for a player to enter the game to make it even again
            }
        }
        
        //Round is ready to start
        m_botAction.sendArenaMessage("Round " + roundNumber + " will start in 10 seconds...", 2);
        
        timerCreateTwoTeams = new TimerCreateTwoTeams();
        m_botAction.scheduleTask(timerCreateTwoTeams, (9 * 1000));
        timerGoSignal = new TimerGoSignal();
        m_botAction.scheduleTask(timerGoSignal, (10 * 1000));
    }
    
    /**
     * This method sets the losing team back on the spectator freq.
     * 
     * @param team losing team
     */
    private void setSpectatorFreq(HashSet<String> team) {
        for (String playerName : team) {
            m_botAction.setShip(playerName, 1);
            m_botAction.specWithoutLock(playerName);
        }
    }
    
    /**
     * This method sets the teams and freq sets up at the start of each round
     */
    private void setTeams() {
        Iterator<Player> it;
        Player player;
        
        teamOne.clear();
        teamTwo.clear();
        
        it = m_botAction.getPlayingPlayerIterator();
        while(it.hasNext()) {
            player = it.next();
            if(player.getFrequency() == FREQ_TEAMONE) 
                teamOne.add(player.getPlayerName());
            else 
                teamTwo.add(player.getPlayerName());
        }
    }
    
    private class TimerCreateTwoTeams extends TimerTask {
        public void run() {
            m_botAction.createNumberOfTeams(2);
        }
    }
    
    private class TimerGoSignal extends TimerTask {
        public void run() {
            setTeams();
            if (!keepDeaths || roundNumber == 1)
                m_botAction.scoreResetAll();
            m_botAction.sendArenaMessage("GO GO GO!!!", 104);
            
            state = ROUND_RUN;
        }
    }
    
    private class TimerRoundEnd extends TimerTask {
        public void run() {
            roundNumber++;
            if (m_botAction.getPlayingFrequencySize(FREQ_TEAMONE) == 0) {
                setSpectatorFreq(teamOne);
                if (m_botAction.getFrequencySize(FREQ_TEAMTWO) != 1)
                    roundPre(teamTwo);
                else
                    gameEnd(teamTwo);
            } else {
                setSpectatorFreq(teamTwo);
                if (m_botAction.getFrequencySize(FREQ_TEAMONE) != 1)
                    roundPre(teamOne);
                else
                    gameEnd(teamOne);
            }
        }
    }

    private class TimerWaitForPlayer extends TimerTask {
        public void run() {
            roundStart();
        }
    }
}
