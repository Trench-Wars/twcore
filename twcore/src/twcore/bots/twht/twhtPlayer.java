/**
 * 
 */
package twcore.bots.twht;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Arrays;

import twcore.core.BotAction;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * twhtPlayer class holds all the individual player information for one TWHT Match. All Individual 
 * stats are entered and stored here.
 * 
 * @author Ian
 * 
 */
public class twhtPlayer {
    
    TreeMap<Integer, twhtShip> m_ships = new TreeMap<Integer, twhtShip>();  //Initilizes the map that stores the individual ship data
    
    String dbConn = "website";      //Database
//    BotSettings bs;
    BotAction ba;                   //Some bot action
    twhtTeam m_team;                //The twht team class
    String m_fcPlayerName;          //Players name
    int fnUserID;                   //UsersID

    // 0 - regular
    // 1 - captain
    // 2 - assistant
//    int m_fnRank = 0; 
    int m_fnShipType = 0;           //Current Shiptype
    int penTimeStamp = 0;           //Expiration timestamp for penalty
    int penTimeStampWarning = 0;    //Expiration timestamp for penalty warning
    int startTimeStamp = 0;         //Starts a timer for tracking game time
    int endTimeStamp = 0;           //Ends the timer for tracking game time
    int m_rating = 0;               //Variable to track total rating
    
    /* Playerstate: 0 - Not In Game
                    1 - In Game
                    2 - Substituted
                    3 - Lagged
                    4 - Penalty Box
     */
    int m_fnPlayerState = 0;
    int m_fnPreviousState = 0;
    int m_fnGoals, m_fnSaves, m_fnAssists, m_fnSteals, m_fnPlusMinus, m_fnSOG, m_fnTurnOver, m_fnPuckCarry, m_fnChecksMade, m_fnChecksTaken, m_fnGoalAllowed, m_fnGameTime, m_fnPuckCarryTime, m_fnGoalieTime, m_fnGoalieSteals, m_fnGoalieTurnOvers, m_fnGoalieChecks, m_fnGoalieChecked, g_rating, p_rating;
    boolean m_switchedShip = false;

    /** Creates a new instance of MatchPlayer */
    public twhtPlayer(String fcPlayerName, String teamName, int shipType, int playerState, BotAction botAction, twhtTeam twhtTeam) {
        m_team = twhtTeam;
        m_fcPlayerName = fcPlayerName;
        m_fnShipType = shipType;
        m_fnPlayerState = playerState;
        ba = botAction;
//      bs = ba.getBotSettings();
        m_ships.put(shipType, new twhtShip(shipType));
    }    

    /**
     * Method used to set penalties. A timestamp of the round time when the penalty
     * will expire. Subtracts 10 for a warning timestamp and sets the player's state.
     * 
     * @param penaltyTime (in seconds)
     */
    public void setPenalty(int penaltyTime) {
        this.penTimeStamp = penaltyTime;
        this.penTimeStampWarning = penaltyTime - 10;
        this.m_fnPreviousState = m_fnPlayerState;
        this.m_fnPlayerState = 4;
    }

    /**
     * Method used to set a new ship for the player. First checks if 
     * to make sure a ship change has occured. Closes the game time for the
     * current ship. Then checks to see if a record has already been created for
     * the ship being changed into. If not, then create one.
     * 
     * @param shipType
     */
    public void setShipChange(int shipType) {
        if (m_fnShipType != shipType) {
            setShipEnd(m_team.m_game.m_curRound.getIntTime());
            m_fnShipType = shipType;
            
        if (!m_ships.containsKey(m_fnShipType))
            m_ships.put(shipType, new twhtShip(shipType));
        
        setShipStart(m_team.m_game.m_curRound.getIntTime());
        }
    }    

    /**
     * Starts the game timer to account for the player's time in the game
     * @param time in seconds
     */
    public void setShipStart (int time) {
        startTimeStamp = time;
    }
    
    /**
     * Ends the game timer to account for the player's time in the game
     * @param time in seconds
     */
    public void setShipEnd (int time) {
        twhtShip pS;
        int gameTime;
        
        pS = m_ships.get(m_fnShipType);
        
        if (pS == null)
            m_ships.put(m_fnShipType, new twhtShip(m_fnShipType));
        
        endTimeStamp = time;
        gameTime = endTimeStamp - startTimeStamp;
        
        if (m_fnShipType == 7 || m_fnShipType == 8)
            pS.reportGoalieTime(gameTime);
        else
            pS.reportGameTime(gameTime);
        
        startTimeStamp = 0;
        endTimeStamp = 0;        
    }    

    /**
     * Method used to get a player's current ship type
     * 
     * @return
     */
    public Integer getPlayerShip() {
        return m_fnShipType;
    }   

    /**
     * Method used to get a player's current state
     * 
     * @return
     */
    public Integer getPlayerState() {
        return m_fnPlayerState;
    }

    /**
     * Method used to get a player's previous state
     * 
     * @return
     */
    public Integer getPreviousState() {
        return m_fnPreviousState;
    }

    /**
     * Returns the timestamp of the round time for when the penalty
     * will expire.
     * 
     * @return
     */
    public Integer getPenalty() {
        return penTimeStamp;
    }

    /**
     * Returns the penalty warning timestamp 10 seconds before the penalty timestamp.    * 
     * 
     * @return  Round time in seconds.
     */
    public Integer getPenaltyWarning() {
        return penTimeStampWarning;
    }

    /**
     * Returns the player's game handle.
     * 
     * @return
     */
    public String getPlayerName() {
        return m_fcPlayerName;
    }

    /**
     * A shortcut boolean to check if the player is a goalie by the current ship.
     * 
     * @return true if a goalie and playing, false if not.
     */
    public boolean getIsGoalie() {
        if (m_fnPlayerState == 1 && (m_fnShipType == 7 || m_fnShipType == 8))
            return true;
        else
            return false;
    }
    
    /**
     * Return the player's rating among all ships played over 30 seconds.
     * 
     * @return rating
     */
    public Integer getPlayerRating() {
        for(twhtShip i : m_ships.values())
            m_rating += i.getRating();
        
        return m_rating;
    }
    
    /**
     * Resets the penalty time stamp to 0. Used at the end of periods and to remove penalties.
     */
    public void resetPenalty() {
        penTimeStamp = 0;
        penTimeStampWarning = 0;
    }

    /**
     * Starts the game timer and sets the player state when a player returns from lagout to the game.
     */
    public void returnedToGame() {
        this.m_fnPreviousState = m_fnPlayerState;
        this.m_fnPlayerState = 1;
        setShipStart(m_team.m_game.m_curRound.getIntTime());
    }

    /**
     * Stops the game timer and sets the player state when a player is subbed from the game.
     */
    public void playerSubbed() {
        this.m_fnPreviousState = m_fnPlayerState;
        this.m_fnPlayerState = 2;
        setShipEnd(m_team.m_game.m_curRound.getIntTime());
    }

    /**
     * Stops the game timer and sets the player state when a player lags out from the game.
     */
    public void playerLaggedOut() {
        this.m_fnPreviousState = m_fnPlayerState;
        this.m_fnPlayerState = 3;
        setShipEnd(m_team.m_game.m_curRound.getIntTime());
    }
    
    /**
     * Returns the players stats in an arena message for intermissions and end of game.
     */
    public void reportPlayerStats() {
        String shipString = "|";
        twhtShip sh;
        int m_fnGoals = 0, m_fnAssists = 0, m_fnSteals = 0,m_fnPlusMinus = 0, m_fnSOG = 0, m_fnTurnOver = 0,m_fnPuckCarry = 0, m_fnChecksMade = 0, m_fnChecksTaken = 0, m_fnGoalAllowed = 0, m_fnGameTime = 0, m_fnPuckCarryTime = 0, p_rating = 0;
        
        for (int i = 1; i < 7; i++) 
            if (m_ships.containsKey(i)) {
                sh = m_ships.get(i);
                
                shipString = shipString + Tools.formatString(" " + sh.getShipType(), 2, " ");
                m_fnGoals += sh.fnGoals;
                m_fnAssists += sh.fnAssists;
                m_fnSteals += sh.fnSteals;
                m_fnPlusMinus += sh.fnPlusMinus;
                m_fnSOG += sh.fnSOG;
                m_fnTurnOver += sh.fnTurnOver;
                m_fnPuckCarry += sh.fnPuckCarry;
                m_fnChecksMade += sh.fnChecksMade;
                m_fnChecksTaken += sh.fnChecksTaken;
                m_fnGoalAllowed += sh.fnGoalAllowed;
                m_fnGameTime += sh.fnGameTime;                            
                m_fnPuckCarryTime += sh.fnPuckCarryTime;
                p_rating += sh.getRating();
            }
        
        if (m_fnGameTime > 30) {
        ba.sendArenaMessage(Tools.formatString(shipString, 13," ") + Tools.formatString("|" + getPlayerName(), 10," ") + Tools.formatString("|" + m_fnGoals, 3," ") + 
                            Tools.formatString("|" + m_fnAssists, 3," ") + Tools.formatString("|" + m_fnSOG , 4," ") + Tools.formatString("|" + m_fnSteals , 4," ") + 
                            Tools.formatString("|" + m_fnTurnOver , 4," ") + Tools.formatString("|" + m_fnChecksMade , 4," ") + Tools.formatString("|" + m_fnChecksTaken , 4," ") + 
                            Tools.formatString("|" + m_fnPuckCarry , 4," ") + Tools.formatString("|" + doFormatTimeString(m_fnPuckCarryTime) , 6," ") + Tools.formatString("|" + doFormatTimeString(m_fnGameTime) , 6," ") + 
                            Tools.formatString("|" + m_fnPlusMinus, 5," ") + Tools.formatString("|" + p_rating , 5," "));
        }
    }
        
    /**
     * Returns the players stats as a goalie in an arena message for intermissions and end of game.
     */
    public void reportGoalieStats(){        
            String shipString = "|";
            twhtShip sh;
            double m_getSavePerc = 0.0;
            
            for (int i = 7; i < 9; i ++) 
                if (m_ships.containsKey(i)) {
                    sh = m_ships.get(i);
                    
                    shipString = shipString + Tools.formatString(" " + sh.getShipType(), 2, " ");
                    m_fnSaves += sh.fnSaves;
                    m_fnPlusMinus += sh.fnPlusMinus;
                    m_fnGoalAllowed += sh.fnGoalAllowed;
                    m_fnGoalieTime += sh.fnGoalieTime;
                    m_fnGoalieSteals += sh.fnGoalieSteals;
                    m_fnGoalieTurnOvers += sh.fnGoalieTurnOvers;
                    m_fnGoalieChecks += sh.fnGoalieChecks;
                    m_fnGoalieChecked += sh.fnGoalieChecked;
                    g_rating += sh.getRating();
                }     
            if (!shipString.equals("|") && m_fnGoalieTime > 30){
                if ((m_fnSaves + m_fnGoalAllowed) != 0)
                    m_getSavePerc =  m_fnSaves / (m_fnSaves + m_fnGoalAllowed) * 100.0;
                
                ba.sendArenaMessage(Tools.formatString(shipString, 5, " ") + Tools.formatString("|" + getPlayerName(), 10," ") + Tools.formatString("|" + m_fnSaves, 4," ") + 
                            Tools.formatString("|" + m_getSavePerc, 4," ") + Tools.formatString("|" + m_fnGoalAllowed , 4," ") + Tools.formatString("|" + m_fnGoalieSteals , 4," ") + 
                            Tools.formatString("|" + m_fnGoalieTurnOvers , 4," ") + Tools.formatString("|" + m_fnGoalieChecks , 4," ") + Tools.formatString("|" + m_fnGoalieChecked , 5," ") + 
                            Tools.formatString("|" + doFormatTimeString(m_fnGoalieTime) , 6," ") + Tools.formatString("|" + m_fnAssists , 3," ") + Tools.formatString("|" + m_fnPlusMinus , 4," ") + Tools.formatString("|" + g_rating , 5," "));
            }
    }    
    
    /**
     * Handles the distribution for the stats based on stat type and the current ship for the player.
     * @param statType
     */
    public void doStats(int statType) {
        /*Stat Types
         *1 - Goal
         *2 - Assist
         *3 - Save
         *4 - Steal
         *5 - SOG
         *6 - Turnover
         *7 - PuckCarry
         *8 - CheckMade
         *9 - Checked
         *10 - Goal Allowed
         */
        int stat = statType;
        twhtShip pS;
        pS = m_ships.get(m_fnShipType);
        
        if (pS == null)
            m_ships.put(m_fnShipType, new twhtShip(m_fnShipType));
               
        switch (stat) {
        case 1:
            pS.reportGoal();
            break;
        case 2:
            pS.reportAssist();
            break;
        case 3:
            pS.reportSave();
            break;
        case 4:
            if (m_fnShipType == 7 || m_fnShipType == 8)
                pS.reportGoalieSteal();
            else
                pS.reportSteal();
            break;
        case 5:
            pS.reportShotOnGoal();
            break;
        case 6:
            if (m_fnShipType == 7 || m_fnShipType == 8)
                pS.reportGoalieTurnover();
            else
                pS.reportTurnover();
            break;
        case 7:
            pS.reportPuckCarry();
            break;
        case 8:
            if (m_fnShipType == 7 || m_fnShipType == 8)
                pS.reportGoalieCheckMade();
            else
                pS.reportCheckMade();
            break;
        case 9:
            if (m_fnShipType == 7 || m_fnShipType == 8)
                pS.reportGoalieCheckTaken();
            else
                pS.reportCheckTaken();
            break;
        case 10:
            pS.reportGoalAllowed();
            break;
        case 11:
            pS.reportPuckCarryTime();
            break;
        case 12:
            pS.reportPlusMinus(true);
            break;            
        case 13:
            pS.reportPlusMinus(false);
            break;
        default:
            break;
        }
    }    
        
    /**
     * Used to format integer time values into a string ##:##
     */
    public String doFormatTimeString(int time) {
        int minutes;
        int seconds;

        minutes = time / 60;
        seconds = time % 60;

        return "" + Tools.rightString("" + minutes, 2, '0') + ":" + Tools.rightString("" + seconds, 2, '0');
    }
    
    /**
     * This class keeps track of the stats for each ship the player uses.
     * 
     * @author Ian
     *
     */
    public class twhtShip {
        int lastTimeCheck;
        int shipType;
        int p_rating; 
        int fnGoals, fnSaves, fnAssists, fnSteals, fnPlusMinus, fnSOG, fnTurnOver, fnPuckCarry, fnChecksMade, fnChecksTaken, fnGoalAllowed, fnGameTime, fnPuckCarryTime, fnGoalieTime, fnGoalieSteals, fnGoalieTurnOvers, fnGoalieChecks, fnGoalieChecked;
        
        //Class constructor
        public twhtShip(int shipType) { 
            this.shipType = shipType;
            resetVariables();
        }
        
        /**
         * Initializes the variables
         */
        public void resetVariables() {
            fnGoals = 0;
            fnAssists = 0;
            fnSaves = 0;
            fnSteals = 0;
            fnPlusMinus = 0;
            fnSOG = 0;
            fnTurnOver = 0;
            fnPuckCarry = 0;
            fnChecksMade = 0;
            fnChecksTaken = 0;
            fnGoalAllowed = 0;
            fnGameTime = 0;
            lastTimeCheck = 0;
            fnPuckCarryTime = 0;
            fnGoalieTime = 0;
            fnGoalieSteals = 0;
            fnGoalieTurnOvers = 0;
            fnGoalieChecks = 0;
            fnGoalieChecked = 0;
            p_rating = 0;
        }
        
        /**
         * Records a goal for a player
         */
        public void reportGoal() {
            fnGoals++;
            p_rating += 107;
        }
        
        /**
         * Records a save for a player
         */
        public void reportSave() {
            fnSaves++;
            p_rating += 27;
        }

        /**
         * Records a assist for a player
         */
        public void reportAssist() {
            fnAssists++;
            p_rating += 60;
        }

        /**
         * Records a steal for a player
         */
        public void reportSteal() {
            fnSteals++;
            p_rating += 16;
        }

        /**
         * Records plus/minus for a player
         */
        public void reportPlusMinus(boolean plusMinus) {
            if (plusMinus){
                fnPlusMinus++;
                p_rating++;
            } else { 
                fnPlusMinus--;
                p_rating--;
            }
        }

        /**
         * Records a shot on goal for a player
         */
        public void reportShotOnGoal() {
            fnSOG++;
            p_rating += 13;
        }
        
        /**
         * Records a turnover for a player
         */
        public void reportTurnover() {
            fnTurnOver++;
            p_rating -= 13;
        }

        /**
         * Records a puck carry for a player
         */
        public void reportPuckCarry() {
            fnPuckCarry++;
        }

        /**
         * Records a check made for a player
         */
        public void reportCheckMade() {
            fnChecksMade++;
            p_rating += 8;
        }

        /**
         * Records a check taken for a player
         */
        public void reportCheckTaken() {
            fnChecksTaken++;
            p_rating -= 3;
        }

        /**
         * Records a goal allowed for a player
         */
        public void reportGoalAllowed() {
            fnGoalAllowed++;
            p_rating -= 49;
        }
        
        /**
         * Records a players game time for a player
         */
        public void reportGameTime(int time) {
            fnGameTime += time;            
        }
                
        /**
         * Records the amount of time a player holds the puck
         */
        public void reportPuckCarryTime() {
            fnPuckCarryTime++;
            p_rating++;
        }
        
        /**
         * Records the time spent as goalie for a player
         */
        public void reportGoalieTime(int time) {
            fnGoalieTime += time;            
        }
        
        /**
         * Records when the goalie turns over the puck
         */
        public void reportGoalieTurnover() {
            fnGoalieTurnOvers++;
            p_rating -= 16;
        }
        
        /**
         * Records when the goalie makes a check
         */
        public void reportGoalieCheckMade() {
            fnGoalieChecks++;
            p_rating += 7;
        }

        /**
         * Records when the goalie takes a check
         */
        public void reportGoalieCheckTaken() {
            fnGoalieChecked++;
            p_rating -= 10;
        }
        
        /**
         * Records when the goalie makes a steal
         */
        public void reportGoalieSteal() {
            fnGoalieSteals++;
            p_rating += 20;
        }
        
        /**
         * Calculates the goalies save percentage
         */
        public double getSavePerc() {
            double savePerc = 0;
         
            return savePerc;
        }
        
        /**
         * Calculates the rating for the player's ship
         */
        public Integer getRating() {
            return p_rating;
        }   
        
        /**
         * Returns the ship type that the record is made for
         */
        public Integer getShipType() {
            return shipType;
        }
   }
}