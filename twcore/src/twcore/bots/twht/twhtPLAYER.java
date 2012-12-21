/**
 * 
 */
package twcore.bots.twht;

import java.sql.Connection;

import twcore.core.BotAction;
import twcore.core.game.Player;

/**
 * @author Ian
 *
 */
public class twhtPLAYER {
    
    boolean useDatabase;


    Connection m_connection;
    BotAction ba;
    Player m_player;
    
    String dbConn = "website";

    twhtTEAM m_team;
    
    String m_fcPlayerName;
    int fnUserID;

    // 0 - regular
    // 1 - captain
    // 2 - assistant
    int m_fnRank = 0;
    
    // Regular game stats
    
    int m_deafaultShip = 1;
    int m_fnFrequency = 0;
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

    boolean m_switchedShip = false;
    
    /** Creates a new instance of MatchPlayer */
    public twhtPLAYER(String fcPlayerName, String teamName, int shipType, twhtTEAM twhtTEAM) {
        useDatabase = false;
        m_team = twhtTEAM;
        m_fcPlayerName = fcPlayerName;
        m_fnShipType = shipType;
        m_fnPlayerState =1;

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
    
    public void changePlayerState (int stateType) {
        this.m_fnPlayerState = stateType;
    }
    
    public void setPenalty (int penaltyTime) {
        penTimeStamp = penaltyTime;
        penTimeStampWarning = penaltyTime - 10;
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
    
    public void returnedFromLagout() {
        this.m_fnPlayerState = 1;
    }
    
    public void playerLaggedOut() {
        this.m_fnPlayerState = 3;
    }
    
    public String getPlayerName() {
         return m_fcPlayerName;
    }
}