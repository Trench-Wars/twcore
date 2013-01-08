/**
 * 
 */
package twcore.bots.twht;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.TreeMap;

import twcore.core.BotAction;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * @author Ian
 * 
 */
public class twhtPlayer {

    boolean useDatabase;

    Connection m_connection;
    BotAction ba;
    Player m_player;
//    TotalStatistics m_statTracker;
//    TreeMap<String, TotalStatistics> m_statTracker;
    TreeMap<Integer, twhtShip> m_ships = new TreeMap<Integer, twhtShip>();
    
    String dbConn = "website";

    twhtTeam m_team;

    String m_fcPlayerName;
    int fnUserID;

    // 0 - regular
    // 1 - captain
    // 2 - assistant
    int m_fnRank = 0;

    // Regular game stats

    
    int m_deafaultShip = 1;
    int m_fnShipType = 0;
    int penTimeStamp = 0;
    int penTimeStampWarning = 0;
    int startTimeStamp = 0;
    int endTimeStamp = 0;
    int m_rating = 0;
    
    /* Playerstate: 0 - Not In Game
                    1 - In Game
                    2 - Substituted
                    3 - Lagged
                    4 - Penalty Box
     */
    int m_fnPlayerState = 0;
    int m_fnPreviousState = 0;

    boolean m_switchedShip = false;

    /** Creates a new instance of MatchPlayer */
    public twhtPlayer(String fcPlayerName, String teamName, int shipType, int playerState, BotAction botAction, twhtTeam twhtTeam) {
        useDatabase = false;
        m_team = twhtTeam;
        m_fcPlayerName = fcPlayerName;
        m_fnShipType = shipType;
        m_fnPlayerState = playerState;
        ba = botAction;
        m_ships.put(shipType, new twhtShip(shipType));
//        m_statTracker = new TotalStatistics();

    }

    public Integer getPlayerShip() {
        return m_fnShipType;
    }

    

    public Integer getPlayerState() {
        return m_fnPlayerState;
    }

    public Integer getPreviousState() {
        return m_fnPreviousState;
    }

    public void setPenalty(int penaltyTime) {
        this.penTimeStamp = penaltyTime;
        this.penTimeStampWarning = penaltyTime - 10;
        this.m_fnPreviousState = m_fnPlayerState;
        this.m_fnPlayerState = 4;
    }

    public void setShipChange(int shipType) {
        if (m_fnShipType != shipType) {
        setShipEnd(m_team.m_game.m_curRound.getIntTime());
        m_fnShipType = shipType;
        if (!m_ships.containsKey(m_fnShipType))
            m_ships.put(shipType, new twhtShip(shipType));
        
        setShipStart(m_team.m_game.m_curRound.getIntTime());
//        m_statTracker.changeShip(shipType);
        }
    }
    
    public Integer getPenalty() {
        return penTimeStamp;
    }

    public Integer getPenaltyWarning() {
        return penTimeStampWarning;
    }

    public void resetPenalty() {
        penTimeStamp = 0;
        penTimeStampWarning = 0;
    }

    public void returnedToGame() {
        this.m_fnPreviousState = m_fnPlayerState;
        this.m_fnPlayerState = 1;
        setShipStart(m_team.m_game.m_curRound.getIntTime());
    }

    public void playerSubbed() {
        this.m_fnPreviousState = m_fnPlayerState;
        this.m_fnPlayerState = 2;
        setShipEnd(m_team.m_game.m_curRound.getIntTime());
    }

    public void playerLaggedOut() {
        this.m_fnPreviousState = m_fnPlayerState;
        this.m_fnPlayerState = 3;
        setShipEnd(m_team.m_game.m_curRound.getIntTime());
    }

    public String getPlayerName() {
        return m_fcPlayerName;
    }

    public boolean getIsGoalie() {
        if (m_fnPlayerState == 1 && (m_fnShipType == 7 || m_fnShipType == 8))
            return true;
        else
            return false;
    }
    
    public void getStastics() {
        ba.sendArenaMessage("Test");
    }
    
    public Integer getPlayerRating() {
        for(twhtShip i : m_ships.values())
            m_rating += i.getRating();
        
        return m_rating;
    }
    
    public void setShipStart (int time) {
        startTimeStamp = time;
    }
    
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
        
    }
    
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
        default:
            break;
        }
    }
    
    public void reportPlusMinus(boolean plusMinus) {
        twhtShip pS;
        pS = m_ships.get(m_fnShipType);
        
        if (pS == null)
            m_ships.put(m_fnShipType, new twhtShip(m_fnShipType));
        
        pS.reportPlusMinus(plusMinus);
    }
    
    public void reportPlayerStats() {
        for(twhtShip i : m_ships.values())
            if ((i.getShipType() != 7 && i.getShipType() != 8) && i.fnGameTime > 30)
        ba.sendArenaMessage(Tools.formatString("|" + i.getShipType()  , 3) + Tools.formatString("|" + getPlayerName(), 10) + Tools.formatString("|" + i.fnGoals, 3) + 
                            Tools.formatString("|" + i.fnAssists, 3) + Tools.formatString("|" + i.fnSOG , 4) + Tools.formatString("|" + i.fnSteals , 4) + 
                            Tools.formatString("|" + i.fnTurnOver , 4) + Tools.formatString("|" + i.fnChecksMade , 4) + Tools.formatString("|" + i.fnChecksTaken , 4) + 
                            Tools.formatString("|" + i.fnPuckCarry , 4) + Tools.formatString("|" + i.getPuckTimeString() , 6) + Tools.formatString("|" + i.getPlayingTimeString() , 6) + 
                            Tools.formatString("|" + i.fnPlusMinus, 5) + Tools.formatString("|" + i.getRating() , 5));
    }
    
    public void reportGoalieStats(){
        for(twhtShip i : m_ships.values())
            if ((i.getShipType() == 7 || i.getShipType() == 8) && i.fnGoalieTime > 30)
        ba.sendArenaMessage(Tools.formatString("|" + i.getShipType()  , 3) + Tools.formatString("|" + getPlayerName(), 10) + Tools.formatString("|" + i.fnSaves, 4) + 
                            Tools.formatString("|" + i.getSavePerc(), 4) + Tools.formatString("|" + i.fnGoalAllowed , 4) + Tools.formatString("|" + i.fnGoalieSteals , 4) + 
                            Tools.formatString("|" + i.fnGoalieTurnOvers , 4) + Tools.formatString("|" + i.fnGoalieChecks , 4) + Tools.formatString("|" + i.fnGoalieChecked , 5) + 
                            Tools.formatString("|" + i.goalieTimeString() , 6) + Tools.formatString("|" + i.fnAssists , 3) + Tools.formatString("|" + i.fnPlusMinus , 4) + Tools.formatString("|" + i.getRating() , 5));
    }
    
    public void reportTotalStats() {
        ba.sendArenaMessage(m_fcPlayerName + " - " + getPlayerRating());
        for(twhtShip i : m_ships.values())
            ba.sendArenaMessage(m_fcPlayerName + " - " + i.shipType + ": " + i.fnGoals + " " + i.fnAssists + " " + i.fnSOG + " " + i.fnPuckCarry + " " + i.fnPlusMinus);
        }
    
    public class twhtShip { 
        int shipType;
        
        long timePlayed, lastTimeCheck;
        
        int p_rating; 
        int fnGoals, fnSaves, fnAssists, fnSteals, fnPlusMinus, fnSOG, fnTurnOver, fnPuckCarry, fnChecksMade, fnChecksTaken, fnGoalAllowed, fnGameTime, fnPuckCarryTime, fnGoalieTime, fnGoalieSteals, fnGoalieTurnOvers, fnGoalieChecks, fnGoalieChecked;
        
        public twhtShip(int shipType) { 
            this.shipType = shipType;
            resetVariables();
        }
        
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
            timePlayed = 0;
            lastTimeCheck = 0;
            fnPuckCarryTime = 0;
            fnGoalieTime = 0;
            fnGoalieSteals = 0;
            fnGoalieTurnOvers = 0;
            fnGoalieChecks = 0;
            fnGoalieChecked = 0;
            p_rating = 0;
        }
        
        public void reportGoal() {
            fnGoals++;
            p_rating += 107;
        }

        
        public void reportSave() {
            fnSaves++;
            p_rating += 27;
        }


        public void reportAssist() {
            fnAssists++;
            p_rating += 60;
        }

        public void reportSteal() {
            fnSteals++;
            p_rating += 16;
        }

        public void reportPlusMinus(boolean plusMinus) {
            if (plusMinus){
                fnPlusMinus++;
                p_rating++;
            } else { 
                fnPlusMinus--;
                p_rating--;
            }
        }

        public void reportShotOnGoal() {
            fnSOG++;
            p_rating += 13;
        }
        
        public void reportTurnover() {
            fnTurnOver++;
            p_rating -= 13;
        }

        public void reportPuckCarry() {
            fnPuckCarry++;
        }

        public void reportCheckMade() {
            fnChecksMade++;
            p_rating += 8;
        }

        public void reportCheckTaken() {
            fnChecksTaken++;
            p_rating -= 3;
        }

        public void reportGoalAllowed() {
            fnGoalAllowed++;
            p_rating -= 49;
        }
        
        public void reportGameTime(int time) {
            fnGameTime += time;            
        }
        
        public void reportGoalieTime(int time) {
            fnGoalieTime += time;            
        }
        
        public void reportPuckCarryTime() {
            fnPuckCarryTime++;
            p_rating++;
        }
        
        public void reportGoalieTurnover() {
            fnGoalieTurnOvers++;
            p_rating -= 16;
        }
        
        public void reportGoalieCheckMade() {
            fnGoalieChecks++;
            p_rating += 7;
        }

        public void reportGoalieCheckTaken() {
            fnGoalieChecked++;
            p_rating -= 10;
        }
        
        public void reportGoalieSteal() {
            fnGoalieSteals++;
            p_rating += 20;
        }
        
        public Integer getSavePerc() {
            int savePerc = 0;
            if ((fnSaves + fnGoalAllowed) != 0)
            savePerc = (fnSaves / (fnSaves + fnGoalAllowed)) * 100;
            return savePerc;
        }
        
        public Integer getShipType() {
            return shipType;
        }
        public Integer getRating() {
            return p_rating;
        }
        
        public String goalieTimeString() {
            int minutes;
            int seconds;

            minutes = fnGoalieTime / 60;
            seconds = fnGoalieTime % 60;

            return "" + Tools.rightString("" + minutes, 2, '0') + ":" + Tools.rightString("" + seconds, 2, '0');
        }
        
        public String getPlayingTimeString() {
            int minutes;
            int seconds;

            minutes = fnGameTime / 60;
            seconds = fnGameTime % 60;

            return "" + Tools.rightString("" + minutes, 2, '0') + ":" + Tools.rightString("" + seconds, 2, '0');     
        }
        
        public String getPuckTimeString() {
            int minutes;
            int seconds;

            minutes = fnPuckCarryTime / 60;
            seconds = fnPuckCarryTime % 60;

            return "" + Tools.rightString("" + minutes, 2, '0') + ":" + Tools.rightString("" + seconds, 2, '0');
        }
    }
}