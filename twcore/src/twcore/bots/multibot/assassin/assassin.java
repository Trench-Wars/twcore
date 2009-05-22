package twcore.bots.multibot.assassin;

import static twcore.core.EventRequester.PLAYER_DEATH;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

/** 
 * Assassin module
 * 
 * Basically, game is divided over number of teams. Each team has specific ships. 
 * Killing a warbird rewards the team 75 points
 * Killing a javelin rewards the team 50 points
 * Killing a spider rewards the team 25 points
 * Killing a leviathan rewards the team 30 points
 * Killing a terrier rewards the team 100 points
 * Killing a weasel rewards the team 250 points
 * Killing a lancaster rewards the team 50 points
 * Killing a shark rewards the team 40 points
 * 
 * Team with most points after 15 minutes wins
 * 
 * Bot responsibilities:
 * (Host setups teams)
 * - host issues !start
 * - bot warps to safe
 *      freq 0 : x=441, y=342, radius=2 
 *      freq 1 : x=568, y=342, radius=2
 * - waits 10 secs
 * - bot *scoreresets
 * - bot warps teams to start
 *      freq 0 : x=505, y=442, radius=1
 *      freq 1 : x=510, y=599, radius=1
 * - gogogo
 * 
 * @author Maverick
 * 
 */
public class assassin extends MultiModule
{
    private boolean running = false;
    private boolean arenaLocked = false;
    
    private final static int[] SHIP_POINTS = { 0, 75, 50, 25, 30, 100, 250, 50, 40 };
    
    private final static int TIME_LIMIT_MINS = 15;
    
    private final static int WAIT_TIME_MS = 10 * Tools.TimeInMillis.SECOND;
    
    //                                            { x  , y  , radius }
    private final static int[] WARP_SAFE_FREQ_0 = { 441, 342, 2};
    private final static int[] WARP_SAFE_FREQ_1 = { 568, 342, 2};
    private final static int[] WARP_START_FREQ_0 = { 505, 442, 1};
    private final static int[] WARP_START_FREQ_1 = { 510, 599, 1};
    
    private HashMap<Short,AssassinPlayer> players = new HashMap<Short, AssassinPlayer>();
    // < Player ID, AssassinPlayer >

    public void init()
    {
        m_botAction.toggleLocked(); // Checks *lock status of arena
    }

    public void requestEvents(ModuleEventRequester events)
    {
        events.request(this, PLAYER_DEATH);
    }

    public void handleEvent(Message event)
    {
        String message = event.getMessage();
        if(event.getMessageType() == Message.PRIVATE_MESSAGE)
        {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            if(name != null && opList.isER(name))
                handleCommand(event.getPlayerID(), name, message.toLowerCase());
            
        } else if(event.getMessageType() == Message.ARENA_MESSAGE) {
            // Check if arena is locked / unlocked and what it should be
            if(message.equals("Arena LOCKED") && !arenaLocked) {
                m_botAction.toggleLocked(); // Arena is locked but it should be unlocked
                
            } else if(message.equals("Arena UNLOCKED") && arenaLocked) {
                m_botAction.toggleLocked(); // Arena is unlocked but it should be locked
            } else if(message.equals("NOTICE: Game over") && event.getSoundCode() == Tools.Sound.HALLELUJAH && running) {
                doGameOver();
            }
        }
    }

    public void handleEvent(PlayerDeath event)
    {
        Player killer = m_botAction.getPlayer(event.getKillerID());
        Player killee = m_botAction.getPlayer(event.getKilleeID());
        AssassinPlayer assassinKiller = players.get(event.getKillerID());
        AssassinPlayer assassinKillee = players.get(event.getKilleeID());
        
        if(killer == null || killee == null)
            return;
        
        // Store number of points gained by killer
        if(assassinKiller != null) {
            assassinKiller.pointsGained += SHIP_POINTS[killee.getShipType()];
        } else {
            // New player
            players.put(event.getKillerID(), new AssassinPlayer(killer.getPlayerName(), killer.getFrequency(), SHIP_POINTS[killee.getShipType()], 0));
        }
        
        // Store number of points lost by killee
        if(assassinKillee != null) {
            assassinKillee.pointsLost += SHIP_POINTS[killee.getShipType()];
        } else {
            // New player
            players.put(event.getKilleeID(), new AssassinPlayer(killee.getPlayerName(), killee.getFrequency(), 0, SHIP_POINTS[killee.getShipType()]));
        }
    }

    public void handleCommand(short playerID, String name, String message)
    {
        if(message.startsWith("!start") && !running) {
            // - Check teams
            // 1) Do we have two teams
            // 2) Does each team have at least 1 player
            // - Warp players to safe
            // - Start TimerTask to start game
            
            
            // Do we have two teams?
            Set<Short> teams = new HashSet<Short>();
            Iterator<Player> players = m_botAction.getPlayingPlayerIterator();
            while(players.hasNext()) {
                teams.add(Short.valueOf(players.next().getFrequency()));
            }
            if(teams.size() < 2) {
                m_botAction.sendPrivateMessage(playerID, "Unable to start Assassin: There are not enough teams of players. You need to have exactly two teams.");
                return;
            } else if(teams.size() > 2) {
                m_botAction.sendPrivateMessage(playerID, "Unable to start Assassin: There are more then two teams of players.");
                return;
            } else if(!teams.contains(Short.valueOf((short)0)) || !teams.contains(Short.valueOf((short)1))) {
                m_botAction.sendPrivateMessage(playerID, "Unable to start Assassin: The teams need to be on frequency 0 and 1.");
                return;
            }
            
            // All checks OK. Initiate starting game
            m_botAction.sendPrivateMessage(playerID, "Starting Assassin...");
            this.players.clear();
            running = true;
            arenaLocked = true;
            m_botAction.toggleLocked();
            m_botAction.receiveAllPlayerDeaths();
            
            // Warp players to safe
            m_botAction.sendArenaMessage("Get ready!", Tools.Sound.BEEP2);
            for(Player player : m_botAction.getPlayingPlayers()) {
                if(player.getFrequency() == 0) {
                    this.doRandomWarp(player.getPlayerID(), WARP_SAFE_FREQ_0[0], WARP_SAFE_FREQ_0[1], WARP_SAFE_FREQ_0[2]);
                } else if(player.getFrequency() == 1) {
                    this.doRandomWarp(player.getPlayerID(), WARP_SAFE_FREQ_1[0], WARP_SAFE_FREQ_1[1], WARP_SAFE_FREQ_1[2]);
                }
            }
            
            // Do another warp to safe after 5 seconds
            TimerTask warpTask = new TimerTask() {
                public void run() {
                    for(Player player : m_botAction.getPlayingPlayers()) {
                        if(player.getFrequency() == 0) {
                            doRandomWarp(player.getPlayerID(), WARP_SAFE_FREQ_0[0], WARP_SAFE_FREQ_0[1], WARP_SAFE_FREQ_0[2]);
                        } else if(player.getFrequency() == 1) {
                            doRandomWarp(player.getPlayerID(), WARP_SAFE_FREQ_1[0], WARP_SAFE_FREQ_1[1], WARP_SAFE_FREQ_1[2]);
                        }
                    }
                }
            };
            m_botAction.scheduleTask(warpTask, Math.round(WAIT_TIME_MS/2));
            
            
            // Wait 10 seconds before starting game
            TimerTask startTask = new TimerTask() {
                public void run() {
                    doStartGame();
                }
            };
            m_botAction.scheduleTask(startTask, WAIT_TIME_MS);
            
        } 
        else if(message.startsWith("!start") && running) {
            m_botAction.sendPrivateMessage(playerID, "Game is already running.");
        }
        else if(message.startsWith("!stop") && running) {
            this.cancel();
            m_botAction.sendArenaMessage("Game cancelled.", Tools.Sound.BEEP1);
        }
        else if(message.startsWith("!stop") && !running) {
            m_botAction.sendPrivateMessage(playerID, "Game is not running.");
        }
        else if(message.startsWith("!rules")) {
            String[] rules = { 
                    "Assassin rules:",
                    "Kill enemy ships to score points! Each ship rewards a different amount of points:",
                    "1) Warbird     " + SHIP_POINTS[1] + " points",
                    "2) Javelin     " + SHIP_POINTS[2] + " points",
                    "3) Spider      " + SHIP_POINTS[3] + " points",
                    "4) Leviathan   " + SHIP_POINTS[4] + " points",
                    "5) Terrier     " + SHIP_POINTS[5] + " points",
                    "6) Weasel      " + SHIP_POINTS[6] + " points",
                    "7) Lancaster   " + SHIP_POINTS[7] + " points",
                    "8) Shark       " + SHIP_POINTS[8] + " points",
                    "The frequency that has the most points after " + TIME_LIMIT_MINS + " minutes wins!"
            };
            m_botAction.arenaMessageSpam(rules);
        }
        else if(message.startsWith("!manual")) {
            String[] manual = {
                    "Assassin manual:",
                    "[1] Host setups two teams on frequency 0 and 1. If needed, host can *lock the arena.",
                    "[2] Host issues !start to "+m_botAction.getBotName(),
                    "[3] Bot warps players to safe",
                    "[4] Bot waits 10 seconds",
                    "[5] Bot issues *scorereset",
                    "[6] Bot warps teams to starting point",
                    "[7] Bot announces 'Go Go Go' and *settimer "+TIME_LIMIT_MINS,
                    "After "+TIME_LIMIT_MINS+" minutes;",
                    "[8] Bot announces winning team",
                    "Note: bot allows additions to teams at any time during game."
            };
            m_botAction.privateMessageSpam(playerID, manual);
        }
        else if(message.startsWith("!scores") && running) {
            int highestCumulativePoints = Integer.MIN_VALUE;
            String highestCumulativePlayer = "?";
            int pointsGainedFreq0 = 0;
            int pointsGainedFreq1 = 0;
            
            for(AssassinPlayer aplayer : players.values()) {
                if(aplayer.freq == 0) {
                    pointsGainedFreq0 += aplayer.pointsGained;
                } else 
                if(aplayer.freq == 1){
                    pointsGainedFreq1 += aplayer.pointsGained;
                }
                int cumulativePoints = (aplayer.pointsGained - aplayer.pointsLost);
                if(cumulativePoints > highestCumulativePoints) {
                    highestCumulativePoints = cumulativePoints;
                    highestCumulativePlayer = aplayer.name;
                }
            }
            
            m_botAction.sendPrivateMessage(playerID, "Current score:   Freq 0 - "+pointsGainedFreq0+" points");
            m_botAction.sendPrivateMessage(playerID, "                 Freq 1 - "+pointsGainedFreq1+" points");
            m_botAction.sendPrivateMessage(playerID, "Current MVP:     "+highestCumulativePlayer+" ("+highestCumulativePoints+" points)");
            
        }
        else if(message.startsWith("!scores") && !running) {
            m_botAction.sendPrivateMessage(playerID, "Game is not running.");
        }
    }

    public void doStartGame() {
        // Scorereset
        m_botAction.scoreResetAll();
        
        // Warp players to starting positions
        // Store players to player list
        for(Player player : m_botAction.getPlayingPlayers()) {
            if(player.getFrequency() == 0) {
                this.doRandomWarp(player.getPlayerID(), WARP_START_FREQ_0[0], WARP_START_FREQ_0[1], WARP_START_FREQ_0[2]);
            } else if(player.getFrequency() == 1) {
                this.doRandomWarp(player.getPlayerID(), WARP_START_FREQ_1[0], WARP_START_FREQ_1[1], WARP_START_FREQ_1[2]);
            }
            
            players.put(player.getPlayerID(), new AssassinPlayer(player.getPlayerName(), player.getFrequency(), 0, 0));
        }
        
        // Initiate timer
        m_botAction.setTimer(TIME_LIMIT_MINS);
        
        // Go Go Go
        m_botAction.sendArenaMessage("Go Go Goooo!",Tools.Sound.GOGOGO);
    }
    
    public void doGameOver() {
        /*
            ,--------------------------------------------+------------.
            |                            Points | Points |     Points |
            |                            Gained |   Lost | Cumulative |
            |                          ,--------+--------+------------+
            | Frequency 0             /   45211 | -57899 |      84251 |
            +------------------------'          |        |            |
            |  12345678901234567890123..1234567 |1234567 | 1234567890 |
            |  F22 Raptor                   451 |     21 |         45 |
            |  L3MU3L                       451 |     21 |         45 |
            |  Refer                        451 |     21 |         45 |
            |  Rubi                         451 |     21 |         45 |
            +-----------------------------------+--------+------------+
            |                          ,--------+--------+------------+
            | Frequency 1             /   45211 | -57899 |      84251 |
            +------------------------'          |        |            |
            |  carter_15                    451 |     21 |         45 |
            |  Orimattilanjymy              451 |     21 |         45 |
            |  Pike                         451 |     21 |         45 |
            |  Puu                          451 |     21 |         45 |
            |  Suede                        451 |     21 |         45 |
            `-----------------------------------+--------+------------+
            Choleric wins round 3!
            
            MVP: Ardour!
         */
        
        
        // Iterate over all the players in the game, count points gained and get the winning team
        int pointsGainedFreq0 = 0, pointsLostFreq0 = 0;
        int pointsGainedFreq1 = 0, pointsLostFreq1 = 0;
        ArrayList<String> statisticsFreq0 = new ArrayList<String>();
        ArrayList<String> statisticsFreq1 = new ArrayList<String>();
        int highestCumulativePoints = Integer.MIN_VALUE;        // MVP points
        String highestCumulativePlayer = "";                    // MVP name
        
        for(AssassinPlayer aplayer : players.values()) {
            int cumulativePoints = (aplayer.pointsGained - aplayer.pointsLost);
            
            String line = "|  ";
            line += Tools.formatString(aplayer.name, 23) + "  ";
            line += Tools.rightString(String.valueOf(aplayer.pointsGained), 7) + " |";
            line += Tools.rightString("-"+String.valueOf(aplayer.pointsLost), 7) + " | ";
            line += Tools.rightString(String.valueOf(cumulativePoints), 10) + " |";
            
            // determine MVP
            if(cumulativePoints > highestCumulativePoints) {
                highestCumulativePoints = cumulativePoints;
                highestCumulativePlayer = aplayer.name;
            }
            
            if(aplayer.freq == 0) {
                pointsGainedFreq0 += aplayer.pointsGained;
                pointsLostFreq0 += aplayer.pointsLost;
                statisticsFreq0.add(line);
                
            } else if(aplayer.freq == 1) {
                pointsGainedFreq1 += aplayer.pointsGained;
                pointsLostFreq1 += aplayer.pointsLost;
                statisticsFreq1.add(line);
            }
        }
        
        // Display statistics & results
        String statisticsLineFreq0 = 
            Tools.rightString(String.valueOf(pointsGainedFreq0), 7) + " |" + 
            Tools.rightString("-"+String.valueOf(pointsLostFreq0), 7) + " | " +
            Tools.rightString(String.valueOf((pointsGainedFreq0-pointsLostFreq0)), 10) + " |";
        String statisticsLineFreq1 =
            Tools.rightString(String.valueOf(pointsGainedFreq1), 7) + " |" + 
            Tools.rightString("-"+String.valueOf(pointsLostFreq1), 7) + " | " +
            Tools.rightString(String.valueOf((pointsGainedFreq1-pointsLostFreq1)), 10) + " |";
        
        String[] headerFreq0 = {
                ",--------------------------------------------+------------.",
                "|                            Points | Points |     Points |",
                "|                            Gained |   Lost | Cumulative |",
                "|                          ,--------+--------+------------+",
                "| Frequency 0             / "+statisticsLineFreq0,
                "+------------------------'          |        |            |"
        };
        String[] headerFreq1 = {
                "+-----------------------------------+--------+------------+",
                "|                          ,--------+--------+------------+",
                "| Frequency 1             / "+statisticsLineFreq1,
                "+------------------------'          |        |            |"
        };
        
        // Determine winner
        String winnerFreq0 = "Frequency 0 wins with "+pointsGainedFreq0+" points!";
        String winnerFreq1 = "Frequency 1 wins with "+pointsGainedFreq1+" points!";
        String tieResult = "The game resulted in a tie!";
        String winner = "Error: unknown winner";
        if(pointsGainedFreq0 == pointsGainedFreq1)
            winner = tieResult;
        else
            winner = pointsGainedFreq0 > pointsGainedFreq1 ? winnerFreq0 : winnerFreq1;
        
        // Create MVP line
        String mvp = "MVP: "+highestCumulativePlayer+" ("+highestCumulativePoints+" points)!";
        String[] footer = {
                "`-----------------------------------+--------+------------+",
                winner,
                "_",
                mvp
        };
        
        // Display statistics in arena messages
        m_botAction.arenaMessageSpam(headerFreq0);
        m_botAction.arenaMessageSpam(statisticsFreq0.toArray(new String[statisticsFreq0.size()]));
        m_botAction.arenaMessageSpam(headerFreq1);
        m_botAction.arenaMessageSpam(statisticsFreq1.toArray(new String[statisticsFreq1.size()]));
        m_botAction.arenaMessageSpam(footer);
        
        players.clear();
        m_botAction.cancelTasks();
        m_botAction.resetTimer();
        running = false;
        arenaLocked = false;
        m_botAction.toggleLocked();
    }


    public String[] getModHelpMessage()
    {
        String[] help = {
                "+---------------------------------------------------------+",
                "|               A  s  s  a  s  s  i  n                    |",
                "+---------------------------------------------------------+",
                "| !start                - Start game                      |",
                "| !stop                 - Stops current game              |",
                "| !rules                - Prints rules in arena messages  |",
                "| !manual               - How to host this event with bot |",
                "| !scores               - Views scores of current game    |",
                "+---------------------------------------------------------+"
        };
        return help;
    }

    public boolean isUnloadable()
    {
        return !running;
    }

    public void cancel()
    {
        players.clear();
        m_botAction.cancelTasks();
        m_botAction.resetTimer();
        running = false;
        arenaLocked = false;
        m_botAction.toggleLocked();
    }
    
    private void doRandomWarp(int playerID, int xCoord, int yCoord, double radius) {
        double randRadians = Math.random() * 2 * Math.PI;
        double randRadius = Math.random() * radius;
        int xWarp = calcXCoord(xCoord, randRadians, randRadius);
        int yWarp = calcYCoord(yCoord, randRadians, randRadius);
        m_botAction.warpTo(playerID, xWarp, yWarp);
    }
    
    private int calcXCoord(int xCoord, double randRadians, double randRadius) {
        return xCoord + (int) Math.round(randRadius * Math.sin(randRadians));
    }
    
    private int calcYCoord(int yCoord, double randRadians, double randRadius) {
        return yCoord + (int) Math.round(randRadius * Math.cos(randRadians));
    }
    
    
    private class AssassinPlayer {
        
        public AssassinPlayer(String name, short freq, int pointsGained, int pointsLost) {
            this.name = name;
            this.freq = freq;
            this.pointsGained = pointsGained;
            this.pointsLost = pointsLost;
        }
        
        protected String name;
        protected short freq;
        protected int pointsGained;
        protected int pointsLost;
    }
}