/**
 * 
 */
package twcore.bots.twht;

import java.sql.Connection;
import java.util.LinkedList;

import twcore.core.BotAction;
import twcore.core.game.Player;

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
    public twhtPlayer(String fcPlayerName, String teamName, int shipType, int playerState, twhtTeam twhtTeam) {
        useDatabase = false;
        m_team = twhtTeam;
        m_fcPlayerName = fcPlayerName;
        m_fnShipType = shipType;
        m_fnPlayerState = playerState;
//        m_statTracker = new TotalStatistics();
    }

    public Integer getPlayerShip() {
        return m_fnShipType;
    }

    public void changeShip(int shipType) {
        this.m_fnShipType = shipType;
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
    }

    public void playerSubbed() {
        this.m_fnPreviousState = m_fnPlayerState;
        this.m_fnPlayerState = 2;
    }

    public void playerLaggedOut() {
        this.m_fnPreviousState = m_fnPlayerState;
        this.m_fnPlayerState = 3;
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

    

          

//    // Internal classes
//
//    /**
//     * @author FoN if * This class is to congregate all the stats so they can be organized and added + removed easily
//     */
//    private class TotalStatistics {
//        TwhtPlayerShip m_currentShip;
//        LinkedList<TwhtPlayerShip> m_ships;
//
//        public TotalStatistics() {
//            m_ships = new LinkedList<TwhtPlayerShip>();
//            createNewShip(m_fnShipType);
//        }
//
//        public void createNewShip(int fnShipType) {
//            if (m_currentShip == null)
//                m_currentShip = new TwhtPlayerShip(fnShipType);
//                m_ships.add(m_currentShip);
//        }
//        
//        public void reportGoal() {
//            if (m_currentShip != null)
//                m_currentShip.reportGoal();
//        }
//
//        public void reportSave() {
//            if (m_currentShip != null)
//                m_currentShip.reportSave();
//        }
//
//        public void reportAssist() {
//            if (m_currentShip != null)
//                m_currentShip.reportAssist();
//        }
//
//        /**
//         * Method reportPrize.
//         */
//        public void reportSteal() {
//            if (m_currentShip != null)
//                m_currentShip.reportSteal();
//        }
//
//        /**
//         * Method reportDeathOnAttach.
//         */
//        public void reportPlusMinus(boolean plusMinus) {
//            if (m_currentShip != null)
//                m_currentShip.reportPlusMinus(plusMinus);
//        }
//
//        public void reportShotOnGoal() {
//            if (m_currentShip != null)
//                m_currentShip.reportShotOnGoal();
//        }
//
//        public void reportTurnover() {
//            if (m_currentShip != null)
//                m_currentShip.reportTurnover();
//        }
//
//        /**
//         * Method reportDeath.
//         */
//        public void reportPuckCarry() {
//            if (m_currentShip != null)
//                m_currentShip.reportPuckCarry();
//        }
//
//        /**
//         * Method reportSpawnDeath.
//         */
//        public void reportCheckMade() {
//            if (m_currentShip != null)
//                m_currentShip.reportCheckMade();
//        }
//
//        /**
//         * Method reportFlagClaimed.
//         * 
//         * Adds flagclaimed to stats
//         */
//        public void reportCheckTaken() {
//            if (m_currentShip != null)
//                m_currentShip.reportCheckTaken();
//        }
//
//        /**
//         * Adds to the m_score.
//         * 
//         * @param score
//         *            The m_score to set
//         */
//        public void reportGoalAllowed() {
//            if (m_currentShip != null)
//                m_currentShip.reportGoalAllowed();
//        }
//
//        //
//        //      /**
//        //      * Method startNow.
//        //      */
//        //      public void startNow() {
//        //          if (m_currentShip != null)
//        //              m_currentShip.startNow();
//        //      }
//        //
//        //      /**
//        //       * Method endNow.
//        //       */
//        //      public void endNow() {
//        //          if (m_currentShip != null)
//        //              m_currentShip.endNow();
//        //      }
//        //
//        //      /**
//        //       * Method getStatistics.
//        //       * @return String depending on the ship type
//        //       */
//        //      public String[] getTotalStatisticsSummary() {
//        //          Iterator<TwhtPlayerShip> i = m_ships.iterator();
//        //          LinkedList<String> summary = new LinkedList<String>();
//        //          while (i.hasNext()) {
//        //              String[] summ = i.next().getStatisticsSummary();
//        //              for (int j = 0; j < summ.length; j++)
//        //                  summary.add(summ[j]);
//        //          }
//        //
//        //          return (String[]) summary.toArray();
//        //      }
//        //
//        //      /**
//        //       * Method getStatistics.
//        //       * @return String depending on the ship type
//        //       */
//        //      public String[] getStatisticsSummary() {
//        //          return m_currentShip.getStatisticsSummary();
//        //      }
//        //
//        //      /**
//        //       * Method getTotalStatistic.
//        //       * @param i
//        //       * @return int
//        //       */
//        //      public int getTotalStatistic(int statType) {
//        //          Iterator<TwhtPlayerShip> i = m_ships.iterator();
//        //          int total = 0;
//        //
//        //          while (i.hasNext()) {
//        //              total += ((TwhtPlayerShip) i.next()).getStatistic(statType);
//        //          }
//        //
//        //          return total;
//        //      }
//        //
//        //      public int getRepelsUsed() {
//        //          int reps = 0;
//        //          for (TwhtPlayerShip ship : m_ships) {
//        //              if (ship.getShipType() == 8)
//        //                  reps += (ship.getStatistic(Statistics.REPELS_USED) / 2);
//        //          }
//        //          return reps;
//        //      }
//        //
//        //      public int getSharkDeaths() {
//        //          int deaths = 0;
//        //          for (TwhtPlayerShip ship : m_ships) {
//        //              if (ship.getShipType() == 8)
//        //                  deaths += (ship.getStatistic(Statistics.DEATHS));
//        //          }
//        //          return deaths;
//        //      }
//        //
//        //      /**
//        //       * Method getTotalStatistic.
//        //       * @param statType Type of statistic
//        //       * @return int
//        //       */
//        //      public int getStatistic(int statType) {
//        //          return m_currentShip.getStatistic(statType);
//        //      }
//        //
//        //      /**
//        //       * @return shipType the type of current ship
//        //       */
//        //      public int getShipType() {
//        //          if (m_currentShip != null)
//        //              return m_currentShip.getShipType();
//        //          else
//        //              return 0; //error
//    }
//
//    private class TwhtPlayerShip {
//
//        java.util.Date m_ftTimeStarted;
//        java.util.Date m_ftTimeEnded;
//
//        long timePlayed, lastTimeCheck;
//
//        int fnGoals, fnSaves, fnAssists, fnSteals, fnPlusMinus, fnSOG, fnTurnOver, fnPuckCarry, fnChecksMade, fnChecksTaken, fnGoalAllowed;
//        
//        public TwhtPlayerShip(int fnShipType) {
//            m_ftTimeStarted = new java.util.Date();
//            m_ftTimeEnded = new java.util.Date();
//            resetVariables();
//        }
//
//        public void resetVariables() {
//            fnGoals = 0;
//            fnAssists = 0;
//            fnSaves = 0;
//            fnSteals = 0;
//            fnPlusMinus = 0;
//            fnSOG = 0;
//            fnTurnOver = 0;
//            fnPuckCarry = 0;
//            fnChecksMade = 0;
//            fnChecksTaken = 0;
//            fnGoalAllowed = 0;
//            timePlayed = 0;
//            lastTimeCheck = 0;
//        }
//        
//        public void reportGoal() {
//            fnGoals++;
//        }
//
//        
//        public void reportSave() {
//            fnSaves++;
//        }
//
//
//        public void reportAssist() {
//            fnAssists++;
//        }
//
//        public void reportSteal() {
//            fnSteals++;
//        }
//
//        public void reportPlusMinus(boolean plusMinus) {
//            if (plusMinus)
//                fnPlusMinus++;
//            else 
//                fnPlusMinus--;
//        }
//
//        public void reportShotOnGoal() {
//            fnSOG++;
//        }
//
//        //      public void updateLastTimeCheck() {
//        //          lastTimeCheck = System.currentTimeMillis();
//        //      }
//        //
//        //      public void updateTimePlayed() {
//        //          if (m_team.m_round.m_fnRoundState != 3 || m_team.m_round.m_fnRoundState != 4) {
//        //              return;
//        //          }
//        //
//        //          if (lastTimeCheck != 0) {
//        //              timePlayed += System.currentTimeMillis() - lastTimeCheck;
//        //          }
//        //
//        //          lastTimeCheck = System.currentTimeMillis();
//        //      }
//
//        public void reportTurnover() {
//            fnTurnOver++;
//        }
//
//        public void reportPuckCarry() {
//            fnPuckCarry++;
//        }
//
//        public void reportCheckMade() {
//            fnChecksMade++;
//        }
//
//        public void reportCheckTaken() {
//            fnChecksTaken++;
//        }
//
//        public void reportGoalAllowed() {
//            fnGoalAllowed++;
//        }
//
//        //      // report end of playership
//        //      public void endNow() {
//        //          m_ftTimeEnded = new java.util.Date();
//        //          updateTimePlayed();
//        //      }
//        //
//        //      // report start of playership
//        //      public void startNow() {
//        //          m_ftTimeStarted = new java.util.Date();
//        //          updateTimePlayed();
//        //      }
//        //
//        //      public int getShipType() {
//        //          return m_statisticTracker.getShipType();
//        //      }
//        //
//        //      public String[] getStatisticsSummary() {
//        //          return m_statisticTracker.getStatisticsSummary();
//        //      }
//        //
//        //      public int getStatistic(int statType) {
//        //          return m_statisticTracker.getIntStatistic(statType);
//        //      }
//        //
//        //      public java.util.Date getTimeStarted() {
//        //          return m_ftTimeStarted;
//        //      }
//        //
//        //      public java.util.Date getTimeEnded() {
//        //          return m_ftTimeEnded;
//        //      }
//    }

}

//}