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
 * TODO:
 * - create two modes, one where deaths are carried along
 * - set number of teams, max number of teams is number of players in.
 * 
 */
public class stepelim extends MultiModule {
    
    private boolean isRunning = false;          //whether the game is running or not
    private boolean isBetweenRounds = true;     //whether it is between a round or not
    private int shipType = 1;                   //type of ship being used
    private int deathLimit = 2;                 //death limit
    private HashSet<String> teamOne;            //team one playerlist
    private HashSet<String> teamTwo;            //team two playerlist
    private HashSet<String> freqOne;            //freq one playerlist
    private HashSet<String> freqTwo;            //freq two playerlist
    
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
            String name = m_botAction.getPlayerName( event.getPlayerID());
            if (opList.isER(name))
                handleCommand(name, message);            
        }
    }
    
    /**
     * This method checks to see what command was sent, and then acts
     * accordingly
     * 
     * @param name      the name of the player who issued the command
     * @param message   the command the player issued
     */
    public void handleCommand( String name, String message) {
        if (message.toLowerCase().equals("!start"))
            startGame(name, "1");
        else if (message.toLowerCase().startsWith("!start")) 
            startGame(name, message.substring(7));
        else if (message.toLowerCase().equals("!stop"))
            stopGame(name);
        else if (message.toLowerCase().startsWith("!deathlimit")) {
            if (!isRunning) {
                try {
                    deathLimit = Integer.parseInt(message.substring(12));
                    m_botAction.sendSmartPrivateMessage(name, "Death limit set to " + 
                            deathLimit + ".");
                } catch(Exception e) {}
            }
            else {
                m_botAction.sendSmartPrivateMessage(name, "Error: Can only set " +
                		"death limit when the game is not runnning.");
                m_botAction.sendSmartPrivateMessage(name, "Use !stop first.");
            }       
        }
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
                m_botAction.spec(killedName);
                m_botAction.spec(killedName);
                m_botAction.sendArenaMessage(killedName + " is out! (" + killed.getWins() 
                        + "-" + killed.getLosses() + ")");
                
                //check if it is the last round. If so the message to stick around does not need to be send.
                if ((killed.getFrequency() == 0 && teamOne.size() != 2) || (killed.getFrequency() == 1 && teamTwo.size() != 2)) {
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
        if (isRunning && !isBetweenRounds) {
            Player player = m_botAction.getPlayer(event.getPlayerID());
            String playerName = player.getPlayerName();
            freqOne.remove(playerName);
            freqTwo.remove(playerName);
            
            // Check if a round has ended.
            checkEndRound();
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
     * @param name the name of the player who issued the !start command
     * @param param the shiptype which will be played in
     */
    private void startGame(String name, String param) {
        if (!isRunning && m_botAction.getNumPlayers() < 4) {
            m_botAction.sendSmartPrivateMessage(name, "There are not enough players to" +
            		" begin this event.");
        } else if (!isRunning) {
            //Lets get this starting
            isRunning = true;
            
            //Determine the shiptype for this round
            try {
                shipType = Integer.parseInt(param);
            } catch(Exception e) {}
            if (shipType <1 || shipType > 8)
                shipType = 1;
            
            m_botAction.sendSmartPrivateMessage( name, "A round of step-elim is starting now." +
            		" I will lock the arena." );
            m_botAction.sendArenaMessage( "Step-Elim is starting. Players are eliminated after " +
                    + deathLimit + " deaths. When a team has won it will be split up and game will start" +
            		" over until 1 person stands.", 2 );
            m_botAction.sendArenaMessage( "The arena will be locked in 20 seconds." );
            
            TimerTask tenSecondWarning = new TimerTask() {
                public void run() {
                    //Lock the arena, set everyone to the appropriate ship, and random the teams
                    m_botAction.toggleLocked();
                    m_botAction.changeAllShips(shipType);
                    m_botAction.createNumberOfTeams(2);
                    m_botAction.sendArenaMessage("The game will begin in 10 seconds... Get ready!", 2);
                }
            };
            m_botAction.scheduleTask(tenSecondWarning, 20000);
            
            TimerTask beginGame = new TimerTask() {
                public void run() {
                    fillTeams();
                    m_botAction.scoreResetAll();
                    isBetweenRounds = false;
                    m_botAction.sendArenaMessage("GO GO GO!!!", 104);
                }
            };
            m_botAction.scheduleTask(beginGame, 30000);
        
            
        } else {
            m_botAction.sendSmartPrivateMessage( name, "A game is already in progress." );
        }
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
            isBetweenRounds = true;
            m_botAction.sendPrivateMessage(name, "Step-Elim mode stopped.");
            m_botAction.specAll();
            m_botAction.sendArenaMessage("This game of Step-Elim has been" +
            		" obliterated by " + name + ".", 13);
            m_botAction.toggleLocked();
            cancel();
        } else {
            m_botAction.sendPrivateMessage(name, "Error: Step-Elim mode is not currently running.");
        }
    }
    
    /**
     * This method fills up the team and freq sets at the start of each round
     */
    private void fillTeams() {
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
        if(freqTwo.isEmpty()) {
            //teamOne has won the round
            isBetweenRounds = true;
            m_botAction.sendArenaMessage("Freq 0 has won the round!", 20);
            if (teamOne.size() != 1) 
                beginNextRound(teamOne);
            else
                endGame(teamOne);
        } else if (freqOne.isEmpty()) {
            //teamTwo has won the round
            isBetweenRounds = true;
            m_botAction.sendArenaMessage("Freq 1 has won the round!", 21);
            if (teamTwo.size() != 1) 
                beginNextRound(teamTwo);
            else
                endGame(teamTwo);
        }
    }
    
    /**
     * This method starts a new round
     * 
     * @param team the winning team of the last round
     */
    private void beginNextRound(HashSet<String> team) {
        Iterator<String> it = team.iterator();
        while(it.hasNext()) {
            m_botAction.setShip(it.next(), shipType);
        }
        
        m_botAction.sendArenaMessage("The next round begins in 10 seconds... Get ready!");
                
        TimerTask preStart = new TimerTask() {
            public void run() {
                m_botAction.createNumberOfTeams(2);
            }
        };
        m_botAction.scheduleTask( preStart, 9000);
        
        TimerTask tenSecondWarning = new TimerTask() {
            public void run() {
                fillTeams();
                isBetweenRounds = false;
                m_botAction.scoreResetAll();
                m_botAction.sendArenaMessage( "Go Go Go!!!", 104 );
            }
        };
        m_botAction.scheduleTask( tenSecondWarning, 10000);
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
        isBetweenRounds = true;
        m_botAction.toggleLocked();  
        cancel();      
    }

    /**
     * This method displays the list of command when the !help command is
     * issued for this module
     * 
     * @return stepElimHelp the list of commands used by the module
     */
    public String[] getModHelpMessage() {
        String[] stepElimHelp = {
                "!start                     -- Starts a game of Step-Elim with shiptype 1",
                "!start <shiptype>          -- Starts a game of Step-Elim with the specified shiptype.",
                "!deathlimit <#>            -- Sets death limit. (default is 2)", 
                "!stop                      -- Stops a game of Step-Elim."
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
