package twcore.bots.multibot.tankwarfare;

import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.FlagClaimed;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerPosition;
import twcore.core.events.PlayerDeath;
import twcore.core.events.FrequencyShipChange;
import twcore.core.game.Flag;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

/**
 * CTF bot for hosting tankwarfare
 *
 * 
 * @author fantus
 *
 */
public class tankwarfare extends MultiModule {
    private static final int FLAG_ONE = 0;
    private static final int FLAG_TWO = 1;
    private static final int FLAG_BOGUS = 2;
    
    public int state;
    public final static int STOPPED = 0;
    public final static int PLACE_FLAGS = 1;
    public final static int GAME_IN_PROGRESS = 2;
    public final static int GAME_END = 3;
    
    public final static String RULES = "RULES: This will be a capture the flag game. " +
    		"For those who are not familiar with CTF: Players will be divided in two teams. " +
    		"Each team has its own territory. The flag is placed in the rear of your team's area, " +
    		"(you will get spawned near your team's flag at start). The object of the game is " +
    		"to sneak into the opposing team's territory, grab the flag and return with it to " +
    		"your own flag-area. Notice: You can only score when your own flag is in your team's flag-area. " +
    		"So protect it at all cost! Plus terriers cannot enter enemy flagroom, nor can they carry flags.";
    
    public boolean[] returnFlagWarning;
    public boolean allowReplaceFlag;
    
    public TimerTask searchFlag;
    public TimerTask moveAround;
    public TimerTask[] timerReturnFlagWarning;
    public FlagSet flagOne;
    public FlagSet flagTwo;
    public FlagSet flagBogus;
    public int counter;
    public int target;
    
    public int[][][] coords;
    public int[] score;
    public String[] flagCarrier;
    public String[] name;
    public String[] leviathan;
    
    public void cancel() {
        m_botAction.cancelTasks();
    }
    
    public void init() {
        /*
         * Set coords for flagrooms
         * 
         * COORDS:
         * x----x--- y2
         * |    |
         * |    |
         * x----x--- y1
         * |    |
         * |    |
         * x1   x2
         */
        coords = new int[2][2][2];
        //Coords team one
        coords[0][0][0] = 249;  // x1
        coords[0][0][1] = 270;  // x2
        coords[0][1][0] = 530;  // y1
        coords[0][1][1] = 495;  // y2
        //Coords team two
        coords[1][0][0] = 754;  // x1
        coords[1][0][1] = 775;  // x2
        coords[1][1][0] = 530;  // y1 
        coords[1][1][1] = 495;  // y2
        
        //Score
        score = new int[2];
        
        //Team Names
        name = new String[2];
        name[0] = "Team <<<< Left";
        name[1] = "Team >>>> Right";
        
        //Target
        target = 3;
        
        //Flag carrier
        flagCarrier = new String[2];
        
        //Leviathanders
        leviathan = new String[2];
        leviathan[0] = "";
        leviathan[1] = "";
        
        //Warnings
        returnFlagWarning = new boolean[2];
        returnFlagWarning[0] = true;
        returnFlagWarning[1] = true;
        
        //Flag replace lock
        allowReplaceFlag = true;
        
        //Timer
        timerReturnFlagWarning = new TimerTask[2];
    }
    
    private void start() {
        //Reset scores
        score[0] = 0;
        score[1] = 0;
        
        //Set flags
        flagOne = new FlagSet(FLAG_ONE, 263, 512);
        flagTwo = new FlagSet(FLAG_TWO, 761, 512);
        flagBogus = new FlagSet(FLAG_BOGUS, 1000, 1000);
        
        counter = 0;
        m_botAction.sendArenaMessage("A new game will begin soon. " + name[0] + " vs. " + name[1] + "!");
        m_botAction.resetFlagGame();
        searchFlag = new TimerTask() {
            public void run() {
                Iterator<Integer> it = m_botAction.getFlagIDIterator();
                if (it.hasNext())
                    startPlacingFlags();
            }
        };
        m_botAction.sendArenaMessage("Waiting for flags to respawn..");
        m_botAction.scheduleTaskAtFixedRate(searchFlag, 1000, 1000);
        
        //Create two teams
        m_botAction.createNumberOfTeams(2);
        
        //flagCarrier
        flagCarrier[0] = "";
        flagCarrier[1] = "";
    }
    
    private void stop() {
        state = STOPPED;
        m_botAction.cancelTasks();
        
        leviathan[0] = "";
        leviathan[1] = "";
    }
    
    public String[] getModHelpMessage() {
        String[] message = {
                "!start                 -- Starts a game of tankwarfare",
                "!stop                  -- Stops the current game",
                "!target <#>            -- Amount of captured flags needed in order to win. (Default 3)",
                "!team <left>:<right>   -- Renames the teams",
                "!setlevi <name>        -- Sets name to leviathan. (NOTICE: only 1 leviathan per team is allowed, " +
                    "players cannot set themselves to leviathan)",
                "!pmrules               -- Private messages the rules",
                "!displayrules          -- Displays the rules in a arenamessage",     
                "NOTICE: The bot will NOT lock the arena itself, late entries are no problem for this module."
        };
        return message;
    }

    public boolean isUnloadable() {
        if (state == STOPPED) 
            return true;
        else 
            return false;
    }

    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request(this, EventRequester.FLAG_CLAIMED);
        eventRequester.request(this, EventRequester.PLAYER_ENTERED);
        eventRequester.request(this, EventRequester.PLAYER_POSITION);
        eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
    }
    
    public void handleEvent(PlayerEntered event) {
        if (state == GAME_IN_PROGRESS) {
            if (event.getShipType() == Tools.Ship.LEVIATHAN)
                m_botAction.setShip(event.getPlayerID(), 1);
        }
    }
    
    public void handleEvent(PlayerPosition event) {
        /*
         * This method is divided into two parts, the handling terrier location part and the handling of the flag carrier
         * part.
         */
        if (state == GAME_IN_PROGRESS) {
            Player p = m_botAction.getPlayer(event.getPlayerID());
            int playerID = event.getPlayerID();
            int playerFreq = m_botAction.getPlayer(playerID).getFrequency();
            String playerName = m_botAction.getPlayerName(playerID);
            int coord_x = event.getXLocation() / 16;
            int coord_y = event.getYLocation() / 16;
            
            
            //TERRIER
            if (p.getShipType() == Tools.Ship.TERRIER) {
                if (flagCarrier[0].equalsIgnoreCase(playerName)) {
                    flagCarrier[0] = "";
                    m_botAction.shipReset(playerName);
                } else if (flagCarrier[1].equalsIgnoreCase(playerName)) {
                    flagCarrier[1] = "";
                    m_botAction.shipReset(playerName);
                }
                
                //Switch playerFreq to enemy freq
                int enemyFreq;
                if (playerFreq == 0)
                    enemyFreq = 1;
                else
                    enemyFreq = 0;
                
                if (coord_x > coords[enemyFreq][0][0] && coord_x < coords[enemyFreq][0][1]) {
                    if (coord_y < coords[enemyFreq][1][0] && coord_y > coords[enemyFreq][1][1]) {
                        m_botAction.sendPrivateMessage(playerName, "Terriers may not enter the flagroom area!");
                        
                        if (enemyFreq == 0)
                            m_botAction.warpTo(playerName, 200, 513);
                        else
                            m_botAction.warpTo(playerName, 820, 513);
                    }
                }
                
                return;
            }
            
            //FLAG CARRIER
            /* 
             * FlagID   -- Team
             * ----------------
             * 0        -- One
             * 1        -- Two
             * 2        -- Bogus
             * 
             * Coords[Team][x/y][1/2]
             * 
             */
            Flag flagOne = m_botAction.getFlag(FLAG_ONE);
            Flag flagTwo = m_botAction.getFlag(FLAG_TWO);
            
            if (flagOne == null || flagTwo == null)
                return;
            
            if (!flagOne.carried() && !flagTwo.carried())
                return;
            
            boolean hasFlag = false;
            boolean hasEnemyFlag = false;
            int flagID = 2;
            
            if (flagOne.carried()) {
                flagID = FLAG_ONE;
                if (playerName == flagCarrier[0]) {
                    hasFlag = true;
                    
                    if (FLAG_ONE != playerFreq) {
                        hasEnemyFlag = true;
                    }
                }
            }
            
            if (flagTwo.carried()) {
                flagID = FLAG_TWO;
                if (playerName == flagCarrier[1]) {
                    hasFlag = true;
                    
                    if (FLAG_TWO != playerFreq)
                        hasEnemyFlag = true;
                }
            }
            
            //Check if this player carries a flag
            if (!hasFlag)
                return;
            
            //Check if that player isn't a bot
            if (playerName.equals(m_botAction.getBotName()))
                return;
            
            
            //Check if a player enters his team's flag area
            if (coord_x > coords[playerFreq][0][0] && coord_x < coords[playerFreq][0][1]) {
                if (coord_y < coords[playerFreq][1][0] && coord_y > coords[playerFreq][1][1]) {
                    if (hasEnemyFlag) {
                        
                        if (isInFlagroom(playerFreq) && allowReplaceFlag) {
                            //Score
                            score[playerFreq]++;
                            m_botAction.sendArenaMessage(playerName +
                                    " scored for " + name[playerFreq] + "!!", Tools.Sound.GOAL);
                            m_botAction.sendArenaMessage("Score: " + score[0] + " - " + score[1]);
                            replaceFlag(flagID);
                            checkGameOver();
                        } else if (returnFlagWarning[playerFreq] && !isInFlagroom(playerFreq)) {
                            returnFlagWarning[playerFreq] = false;
                            m_botAction.sendArenaMessage(
                                    name[playerFreq] + " almost scored a point, but where is their own flag? " +
                            		"Quick find it before they will retreive their flag!", Tools.Sound.CROWD_GEE);
                            if (playerFreq == 0) {
                                timerReturnFlagWarning[0] = new TimerTask() {
                                    public void run() {
                                        returnFlagWarning[0] = true;
                                    }
                                };
                                m_botAction.scheduleTask(timerReturnFlagWarning[0], 1 * Tools.TimeInMillis.MINUTE);
                            } else {
                                timerReturnFlagWarning[1] = new TimerTask() {
                                    public void run() {
                                        returnFlagWarning[1] = true;
                                    }
                                };
                                m_botAction.scheduleTask(timerReturnFlagWarning[1], 1 * Tools.TimeInMillis.MINUTE);
                            }   
                        }    
                    } else if (allowReplaceFlag) {
                        m_botAction.sendArenaMessage(name[playerFreq] + " retrieved their flag! Good job " +
                                playerName + "!", Tools.Sound.INCONCEIVABLE);
                        replaceFlag(flagID);
                    }
                }
            }
        }
    }
    
    public void handleEvent(PlayerDeath event) {
        if (state == GAME_IN_PROGRESS) {
            int killerID = event.getKillerID();
            int killeeID = event.getKilleeID();
            String killerName = m_botAction.getPlayerName(killerID);
            String killeeName = m_botAction.getPlayerName(killeeID);
            
            if (flagCarrier[0].equals(killeeName))
                flagCarrier[0] = killerName;
            else if (flagCarrier[1].equals(killeeName))
                flagCarrier[1] = killerName;
        }
    }
    
    public void handleEvent(FlagClaimed event) {
        if (state == GAME_IN_PROGRESS) {
            int playerID = event.getPlayerID();
            int flagID = event.getFlagID();
            String playerName = m_botAction.getPlayerName(playerID);
            
            if (playerName.equals(m_botAction.getBotName()))
                return;
            
            flagCarrier[flagID] = playerName;
            
            if (playerID == flagID)
                m_botAction.sendArenaMessage(playerName + " has recaptured " + name[flagID] + "'s flag! " +
                		"Quick return the flag to your base!", Tools.Sound.CROWD_OHH);
            else
                m_botAction.sendArenaMessage(playerName + " has captured " + name[flagID] + "'s flag! " +
                        "Bring their flag to your base to score!", Tools.Sound.CROWD_OOO);
        }
    }
    
    public void handleEvent(FrequencyShipChange event) {
        if (state > STOPPED && event.getShipType() == Tools.Ship.LEVIATHAN) {
            Player p = m_botAction.getPlayer(event.getPlayerID());
            String name = p.getPlayerName();
            int freq = p.getFrequency();
            
            if (!name.equalsIgnoreCase(leviathan[freq])) {
                m_botAction.sendPrivateMessage(name, 
                        "The use of leviathan is prohibited, you have been set to another ship.");
                m_botAction.setShip(name, 1);
            }
        }
    }
    
    public void handleEvent(Message event) {
        if (event.getMessageType() == Message.PRIVATE_MESSAGE)
            handleCommand(m_botAction.getPlayerName(event.getPlayerID()), event.getMessage());
    }
    
    private void handleCommand(String messager, String message) {
        if (m_botAction.getOperatorList().isER(messager)) {
            if (message.equalsIgnoreCase("!start"))
                cmd_start(messager);
            else if (message.equalsIgnoreCase("!stop"))
                cmd_stop(messager);
            else if (message.startsWith("!target "))
                cmd_target(messager, message.substring(8));
            else if (message.startsWith("!team "))
                cmd_nameTeams(messager, message.substring(6));
            else if (message.startsWith("!displayrules"))
                cmd_displayRules();
            else if (message.startsWith("!pmrules"))
                cmd_pmRules(messager);
            else if (message.startsWith("!setlevi "))
                cmd_setLeviathan(messager, message.substring(9));
        }
    }
  
    private void cmd_displayRules() {
        String rules = RULES;
        while (!rules.isEmpty()) {
            int endIndex = 0;
            if (rules.length() < 100)
                endIndex = rules.length();
            else {
                for (int i = 100; i > 0; i--) {
                    if (rules.charAt(i) == ' ') {
                        endIndex = i;
                        break;
                    }   
                }
                if (endIndex == 0)
                    endIndex = rules.length();
            }
            m_botAction.sendArenaMessage(rules.substring(0, endIndex));

            if (endIndex == rules.length())
                rules = rules.substring(endIndex);
            else
                rules = rules.substring(endIndex + 1);
        }
    }
    
    private void cmd_pmRules(String messager) {
        String rules = RULES;
        while (!rules.isEmpty()) {
            int endIndex = 0;
            if (rules.length() < 100)
                endIndex = rules.length();
            else {
                for (int i = 100; i > 0; i--) {
                    if (rules.charAt(i) == ' ') {
                        endIndex = i;
                        break;
                    }   
                }
                if (endIndex == 0)
                    endIndex = rules.length();
            }
            m_botAction.sendPrivateMessage(messager, rules.substring(0, endIndex));
            
            if (endIndex == rules.length())
                rules = rules.substring(endIndex);
            else
                rules = rules.substring(endIndex + 1);
        }
    }
    
    private void cmd_nameTeams(String messager, String names) {
        String[] teamName = names.split(":");
        
        if (state != STOPPED) {
            m_botAction.sendPrivateMessage(messager, "Stop the bot first and try again.");
            return;
        }
        
        if (teamName.length != 2) {
            m_botAction.sendPrivateMessage(messager, "Syntax error. Try !team <left>:<right>");
            return;
        }
        
        name[0] = teamName[0];
        name[1] = teamName[1];
        
        m_botAction.sendPrivateMessage(messager, "New team names: " + name[0] + " and " + name[1]);
    }
    
    private void cmd_setLeviathan(String messager, String name) {
        if (name.isEmpty()) {
            m_botAction.sendPrivateMessage(messager, "Please specify a player");
            return;
        }
        
        Player p = m_botAction.getFuzzyPlayer(name);
        
        if (p == null) {
            m_botAction.sendPrivateMessage(messager, "Player not found");
            return;
        }
        
        //Remove old leviathan
        if (leviathan[p.getFrequency()] != "")
            m_botAction.setShip(leviathan[p.getFrequency()], 0);
        
        //Set new leviathan
        leviathan[p.getFrequency()] = p.getPlayerName().toLowerCase();
        m_botAction.setShip(p.getPlayerName(), Tools.Ship.LEVIATHAN);
        
        //Notify host
        m_botAction.sendPrivateMessage(messager, p.getPlayerName() + " has been set to leviathan for freq "
                + p.getFrequency());
    }
    
    private void cmd_start(String messager) {
        if (state != STOPPED)
            m_botAction.sendPrivateMessage(messager, "Game is already running.");
        else {
            m_botAction.sendPrivateMessage(messager, "Starting a new game.");
            start();
        }
    }
    
    private void cmd_stop(String messager) {
        if (state == STOPPED)
            m_botAction.sendPrivateMessage(messager, "Game has already been stopped.");
        else {
            m_botAction.sendPrivateMessage(messager, "Stopping.");
            stop();
        }
    }

    private void cmd_target(String messager, String targetFlags) {
        if (state == STOPPED) {
            target = Integer.parseInt(targetFlags);
            m_botAction.sendPrivateMessage(messager, "Target changed to " + target + ".");
        } else
            m_botAction.sendPrivateMessage(messager, "Game already in progress," +
            		" target can only be changed when there is no game running.");
    }
  
    private void nextFlag() {
        if (state == PLACE_FLAGS) {
            m_botAction.warpFreqToLocation(0, 175, 513);
            m_botAction.warpFreqToLocation(1, 848, 513);
            
            if (counter == 0) 
                m_botAction.scheduleTask(flagOne, 1000);
            else if (counter == 1) 
                m_botAction.scheduleTask(flagTwo, 1000);
            else if (counter == 2)
                m_botAction.scheduleTask(flagBogus, 1000);
            else if (counter == 3)
                startGame();
            
            counter++;
        }
    }
    
    private void startPlacingFlags() {
        m_botAction.cancelTask(searchFlag);
        
        state = PLACE_FLAGS;
        m_botAction.sendArenaMessage("Flags found, placing flags..");
        nextFlag();
    }
    
    private void startGame() {
        state = GAME_IN_PROGRESS;
        m_botAction.warpFreqToLocation(0, 251, 513);
        m_botAction.warpFreqToLocation(1, 773, 513);
        
        
        for (Iterator<Player> i = m_botAction.getPlayingPlayerIterator(); i.hasNext();) {
            Player p = i.next();
            
            if (p.getShipType() == Tools.Ship.LEVIATHAN) {
                if (!p.getPlayerName().equalsIgnoreCase(leviathan[p.getFrequency()])) {
                    m_botAction.sendPrivateMessage(p.getPlayerName(), 
                        "The use of leviathan is prohibited, you have been set to another ship.");
                    m_botAction.setShip(p.getPlayerID(), 1);
                }   
            }   
        }
        
        m_botAction.sendArenaMessage("Go go go!!", Tools.Sound.GOGOGO);
        
        moveAround = new TimerTask()
        {
            boolean toggleLocation = false;
            
            public void run()
            {
                if (toggleLocation)
                    m_botAction.move(263 * 16, 513 * 16);
                else
                    m_botAction.move(761 * 16, 513 * 16);
                
                toggleLocation = !toggleLocation;
            };
        };
        m_botAction.scheduleTaskAtFixedRate(moveAround, 1000, 300);
    }
    
    private boolean isInFlagroom(int flagID) {
        Flag flag = m_botAction.getFlag(flagID);
        
        if (flag == null)
            return false;
        
        int coord_x;
        int coord_y;
        if (flag.carried()) {
            coord_x = m_botAction.getPlayer(m_botAction.getPlayerID(flagCarrier[flagID])).getXTileLocation();
            coord_y = m_botAction.getPlayer(m_botAction.getPlayerID(flagCarrier[flagID])).getYTileLocation();
        } else {
            coord_x = flag.getXLocation();
            coord_y = flag.getYLocation();
        }
        
        if (coord_x > coords[flagID][0][0] && coord_x < coords[flagID][0][1]) {
            if (coord_y < coords[flagID][1][0] && coord_y > coords[flagID][1][1])
                return true;
        }
        
        return false;
    }
    
    private void replaceFlag(int flagID) {
        if (allowReplaceFlag) {
            allowReplaceFlag = false;
            FlagSet resetFlag;
            m_botAction.cancelTask(moveAround);
            if (flagID == FLAG_ONE)
                resetFlag = new FlagSet(FLAG_ONE, 263, 512);
            else
                resetFlag = new FlagSet(FLAG_TWO, 761, 512);
        
            m_botAction.scheduleTask(resetFlag, 100);
        }
    }
    
    private void checkGameOver() {
        int winner = -1;
        if (score[0] >= target)
            winner = 0;
        else if (score[1] >= target)
            winner = 1;
        
        if (winner == -1)
            return;
        
        gameOver(winner);
    }
    
    private void gameOver(int winner) {
        state = GAME_END;
        m_botAction.cancelTasks();
        
        m_botAction.sendArenaMessage(name[winner] + " has won the game!");
        m_botAction.sendArenaMessage("Final score: " + score[0] + " - " + score[1]);
        
        stop();
    }
    
    private class FlagSet extends TimerTask {
        TimerTask grabFlag;
        TimerTask checkFlag;
        private int flagID;
        private int x_coord;
        private int y_coord;
        
        
        public FlagSet(int flagID, int x_coord, int y_coord) {
            this.flagID = flagID;
            this.x_coord = x_coord * 16;
            this.y_coord = y_coord * 16;
        }
        
        public void run() {
            m_botAction.stopReliablePositionUpdating();
            m_botAction.getShip().setShip(0);
            m_botAction.getShip().setFreq(flagID);
            m_botAction.setFreq(m_botAction.getBotName(), flagID);
            m_botAction.getShip().move(x_coord, y_coord);
            
            if (m_botAction.getFlag(flagID).carried())
                m_botAction.shipReset(flagCarrier[flagID]);
            
            grabFlag(); 
        }
        
        private void dropFlag() {
            m_botAction.dropFlags();
            m_botAction.getShip().setShip(8);
            m_botAction.getShip().setFreq(9999);
            m_botAction.resetReliablePositionUpdating();
            
            allowReplaceFlag = true;
            nextFlag();
        }
        
        private void grabFlag() {
            grabFlag = new TimerTask() {
                public void run() {
                    m_botAction.grabFlag(flagID);
                }
            };
            
            m_botAction.scheduleTask(grabFlag, 1000);
            checkFlag();
        }
        
        private void checkFlag() {
            checkFlag = new TimerTask() {
                public void run() {
                    int id = m_botAction.getFlag(flagID).getPlayerID();
                    int botID = m_botAction.getPlayerID(m_botAction.getBotName());
                    
                    if (id == botID)
                        dropFlag();
                    else {
                        m_botAction.shipReset(flagCarrier[flagID]);
                        grabFlag();
                    }
                }
            };
            m_botAction.scheduleTask(checkFlag, 3000);  
        }
    }
}
