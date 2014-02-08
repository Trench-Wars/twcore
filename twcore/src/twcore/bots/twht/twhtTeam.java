/**
 * 
 */
package twcore.bots.twht;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import twcore.core.BotAction;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * @author Ian
 * 
 */
public class twhtTeam {

    boolean useDatabase;

    Connection m_connection;
    BotAction ba;
//    BotSettings bs;
    twhtGame m_game;

    String dbConn = "website";

    String m_fcTeamName;
    String m_CenterName;
    int fnTeamID;
    int m_fnTeamNumber;
    int m_fnFrequency;
    int m_fnTeamScore;
    int pen_fnAttackingGoalie = 0;
    int pen_fnBDC = 0;
    int pen_fnDC = 0;
    int pen_fnFaceoff = 0;
    int pen_fnGoalieCrossing = 0;
    int pen_fnRespawnKill = 0;
    
    LinkedList<String> m_captains;
    LinkedList<String> player;
    TreeMap<String, twhtPlayer> m_players = new TreeMap<String, twhtPlayer>(String.CASE_INSENSITIVE_ORDER);
    TreeSet<String> roster = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    twhtPlayer curPlayer;

    TimerTask addDelay;
    TimerTask changeDelay;
    TimerTask substituteDelay;
    TimerTask switchDelay;
    TimerTask removeDelay;
    TimerTask lagoutDelay;

    /** Creates a new instance of MatchTeam */
    public twhtTeam(String fcTeamName, int m_fnTeamID, int fnFrequency, int fnTeamNumber, twhtGame twhtGame, BotAction botAction) {
        this.m_game = twhtGame;
        this.ba = botAction;
        this.m_fcTeamName = fcTeamName;
        this.m_captains = new LinkedList<String>();
        this.m_fnFrequency = fnFrequency;
        this.m_fnTeamNumber = fnTeamNumber;
        this.fnTeamID = m_fnTeamID;
        //        populateRosterandCaptains();
    }

    //    public void populateRosterandCaptains() {
    //        int rank = 0;
    //        String playerName = null;
    //        try {
    //            ResultSet rs = ba.SQLQuery(dbConn,
    //                    "SELECT tbltwht__userrank.fnUserID, tbltwht__userrank.fnRankID , tbluser.fcUserName, tbltwht__team.fsName " + 
    //"FROM tbltwht__userrank, tbltwht__teamuser, tbluser, tbltwht__team " +
    //"WHERE tbltwht__userrank.fnUserID = tbltwht__teamuser.fnUserID " + 
    //"AND tbluser.fnUserID = tbltwht__teamuser.fnUserID " + 
    //"AND tbltwht__userrank.fnUserID = tbluser.fnUserID " + 
    //"AND tbltwht__teamuser.fdQuit IS NULL " + 
    //"AND tbltwht__team.fnTWHTTeamID =" + fnTeamID + 
    //"AND tbltwht__team.fnTWHTTeamId = tbltwht__teamuser.fnTeamID ");
    //
    //            m_captains = new LinkedList<String>();
    //
    //            while (rs.next()) {
    //                rank = rs.getInt("fnRankID");
    //                if (rank >= 3){
    //                    m_captains.add(rs.getString("fcUserName").toLowerCase());
    //                } else if (rank == 2){
    //                    roster.add(rs.getString("fcUserName").toLowerCase());               
    //                }                    
    //            }
    //            ba.SQLClose(rs);
    //        } catch (Exception e) {
    //            System.out.println(e.getMessage());
    //        }
    //    }

    /**
     * Adds a player to the game
     * 
     * @param name
     * @param shipType
     */
    public void addPlayer(String name, int shipType) {
        final String playerName = name;
        final int playerShip = shipType;

        m_players.put(name, new twhtPlayer(name.toLowerCase(), m_fcTeamName, shipType, 1, ba, m_game, this));
        
        addDelay = new TimerTask() {
            public void run() {                
                ba.sendArenaMessage(playerName + " has been added for " + m_fcTeamName, 2);
                ba.setShip(playerName, playerShip);
                ba.setFreq(playerName, m_fnFrequency);
            }
        }; ba.scheduleTask(addDelay, Tools.TimeInMillis.SECOND * 2);
    }

    /**
     * Substitutes two players on the same team.
     * 
     * @param playerA
     * @param playerB
     * @param shipType
     */
    public void subPlayer(String playerA, String playerB, int shipType) {
        final String playerAName = playerA;
        final String playerBName = playerB;
        final int playerShip = shipType;
        final Player p;
        twhtPlayer pA;

        pA = m_players.get(playerAName);

        pA.playerSubbed();
        p = ba.getPlayer(playerBName);
        m_players.put(playerB, new twhtPlayer(playerBName.toLowerCase(), m_fcTeamName, playerShip, 1, ba, m_game, this));

        substituteDelay = new TimerTask() {
            public void run() {
                if (p != null)
                    ba.specWithoutLock(playerAName);
                ba.setShip(playerBName, playerShip);
                ba.setFreq(playerBName, m_fnFrequency);
                ba.sendArenaMessage(playerAName + " has been subbed for " + playerBName, 2);
            }
        }; ba.scheduleTask(substituteDelay, Tools.TimeInMillis.SECOND * 2);
    }

    /**
     * Switches the ship of two players on the same team.
     * 
     * @param playerA
     * @param playerB
     */
    public void switchPlayer(String playerA, String playerB) {
        final String playerAName = playerA;
        final String playerBName = playerB;
        final twhtPlayer pA;
        final twhtPlayer pB;
        final int pAShipType;
        final int pBShipType;

        pA = m_players.get(playerA);
        pB = m_players.get(playerB);
        pAShipType = pA.getPlayerShip();
        pBShipType = pB.getPlayerShip();
        pA.setShipChange(pBShipType);
        pB.setShipChange(pAShipType);

        switchDelay = new TimerTask() {
            public void run() {
                ba.setShip(playerAName, pBShipType);
                ba.setShip(playerBName, pAShipType);
                ba.sendArenaMessage(playerAName + " has been switched swith " + playerBName, 2);
            }
        }; ba.scheduleTask(switchDelay, Tools.TimeInMillis.SECOND * 2);
    }

    /**
     * Changes a player's ship on the team
     * 
     * @param player
     * @param shipType
     */
    public void changePlayer(String player, int shipType) {
        final String playerAName = player;
        final int playerShip = shipType;
        twhtPlayer p;
        
        p = m_players.get(player);
        p.setShipChange(shipType);

        switchDelay = new TimerTask() {
            public void run() {
                ba.setShip(playerAName, playerShip);
                ba.sendArenaMessage(playerAName + " has been changed to ship " + playerShip, 2);
            }
        }; ba.scheduleTask(switchDelay, Tools.TimeInMillis.SECOND * 2);
    }

    /**
     * Removes a player from the game
     * 
     * @param player
     */
    public void removePlayer(String player) {
        final String playerAName = player;
        final Player p;

        m_players.remove(playerAName);
        p = ba.getPlayer(playerAName);

        removeDelay = new TimerTask() {
            public void run() {
                if (p != null)
                    ba.specWithoutLock(playerAName);
                ba.sendArenaMessage(playerAName + " has been removed from the game", 2);
            }
        }; ba.scheduleTask(removeDelay, Tools.TimeInMillis.SECOND * 2);
    }

    /**
     * Returns a lagged out player
     * 
     * @param playerName
     */
    public void lagOut(String playerName) {
        final String name = playerName;
        final int shipType;
        final twhtPlayer p;

        p = m_players.get(name);
        p.returnedToGame();
        shipType = p.getPlayerShip();
        
        if (p != null) {
            lagoutDelay = new TimerTask() {
                public void run() {
                    ba.setShip(name, shipType);
                    ba.setFreq(name, m_fnFrequency);
                }
            }; ba.scheduleTask(lagoutDelay, Tools.TimeInMillis.SECOND * 2);
        }
    }
    
    /**
     * Notifies a player's team and sets him to lagout when they leave play
     * 
     * @param name
     */
    public void doLagout(String name) {
        String p;

        for (twhtPlayer i : m_players.values()) {
            p = ba.getFuzzyPlayerName(i.getPlayerName());

            if (p != null)
                ba.sendPrivateMessage(p, name + " has lagged out of the game.");
        }    
    }
    
    public void doAddPenalty(int penalty) {        
        switch (penalty) {
            case 1:
                pen_fnAttackingGoalie++;
                break;
            case 2:
                pen_fnBDC++;
                break;
            case 3:
                pen_fnDC++;
                break;
            case 4:
                pen_fnGoalieCrossing++;
                break;
            case 5:
                pen_fnRespawnKill++;
                break;
            default:
                break;
            }
    }

    /**
     * Checks to see if a player is captain of the team
     * 
     * @param name
     * @return
     */
    public boolean isCaptain(String name) {
        if (m_captains.contains(name))
            return true;
        else
            return false;
    }

    /**
     * Checks if a player is on the team
     *  
     * @param name
     * @return
     */
    public boolean isPlayer(String name) {
        if (m_players.containsKey(name))
            return true;
        else
            return false;
    }

    /**
     * Checks to see if a player is rostered
     * 
     * @param name
     * @return
     */
    public boolean isRostered(String name) {
        if (roster.contains(name))
            return true;
        else
            return false;
    }

    /**
     * Returns if a player is lagged out
     * 
     * @param name
     * @return
     */
    public boolean isLaggedOut(String name) {
        twhtPlayer p;

        p = searchPlayer(name);

        if (p == null)
            return false;

        if (p.getPlayerState() == 3)
            return true;
        else
            return false;      
    }

    /**
     * Returns the team's name
     * 
     * @return
     */
    public String getTeamName() {
        return m_fcTeamName;
    }

    /**
     * Returs the team's current frequency
     * 
     * @return
     */
    public int getFrequency() {
        return m_fnFrequency;
    }
    
    /**
     * Displays the stats in an arena message
     */
    public void getTeamStats() {
        ba.sendArenaMessage(Tools.formatString("|" + getTeamName(), 73, "-"));
        ba.sendArenaMessage("|SHIPS       |PLAYERS  |G |A |SH |ST |TO |CM |CT |PC |PCT  |T    |+/-|RATING",1);               
        for(twhtPlayer i : m_players.values())
            i.reportPlayerStats();
        ba.sendArenaMessage(Tools.formatString("|", 73, " "));
        ba.sendArenaMessage("|    |GOALIES  |SV |SV%  |GA |ST |TO |CM |CT  |GT   |A |+/-|RATING");
        for(twhtPlayer i : m_players.values())
            i.reportGoalieStats();
        ba.sendArenaMessage(Tools.formatString("|", 73, " "));
    }

    /**
     * Figures out how long the penalty should be set for
     * 
     * @param penalty
     * @return
     */
    public Integer getPenTime (String penalty) {
        if (penalty.equals("respawn")) {
            if (pen_fnRespawnKill == 0)
                return 120;
            else if(pen_fnRespawnKill == 1 || pen_fnRespawnKill == 2)
                return 300;
//        } else if (penalty.equals("faceoff")) {
//            if (pen_fnFaceoff == 0 || pen_fnFaceoff == 1)
//                return 120;
//            else if (pen_fnFaceoff == 2)
//                return 300;
        } else if (penalty.equals("gcrossing")) { 
            if (pen_fnGoalieCrossing == 0)
                return 120;
            else if(pen_fnGoalieCrossing  == 1 || pen_fnGoalieCrossing == 2)
                return 300;            
        } else if (penalty.equals("gattack")) {
            if (pen_fnAttackingGoalie == 0)
                return 120;
            else if(pen_fnAttackingGoalie  == 1 || pen_fnAttackingGoalie == 2)
                return 300;              
        } else if (penalty.equals("dc")) {
            if (pen_fnDC == 0 || pen_fnDC == 1)
                return 120;
            else if (pen_fnDC == 2)
                return 300;             
        } else if (penalty.equals("bdc")) 
                return 300;       
        
        return -1;
    }
    
    /**
     * Sets the team's current frequency
     * 
     * @param freq
     */
    public void setFrequency(int freq) {
        m_fnFrequency = freq;
    }

    /**
     * Sets the player who is ported to the center of the faceoff circle
     * during faceoffs.
     * 
     * @param centerName
     */
    public void setCenter(String centerName) {
        m_CenterName = centerName;
    }

    /**
     * Executes a warp to the correct side and double checks that the frequency of the player is correct
     */
    public void setFreqAndSide() {
        Player p;

        for (twhtPlayer i : m_players.values()) {
        p = ba.getFuzzyPlayer(i.getPlayerName());
            
            if (p != null) {
                if (p.getFrequency() != m_fnFrequency && p.getFrequency() != 8025) 
                    ba.setFreq(i.getPlayerName(), m_fnFrequency);                

                if (i.getPlayerState() == 1) {
                    if (m_fnFrequency == 0) {
                        if (i.getIsGoalie()) 
                            ba.warpTo(i.getPlayerName(), 388, 512);
                        else if (i.getPlayerName().equals(m_CenterName))
                            ba.warpTo(i.getPlayerName(), 505, 512);
                        else 
                            ba.warpTo(i.getPlayerName(), 445, 512);                        
                    } else if (m_fnFrequency == 1) {
                        if (i.getIsGoalie()) 
                            ba.warpTo(i.getPlayerName(), 635, 512);
                        else if (i.getPlayerName().equals(m_CenterName)) 
                            ba.warpTo(i.getPlayerName(), 518, 512);
                        else 
                            ba.warpTo(i.getPlayerName(), 578, 512);                        
                    }
                }
            }
        }
    }
    
    /**
     * Warps a team to the player's bench for a penalty shot
     */
    public void setPenaltyShotWarp() {
        Player p;

        for (twhtPlayer i : m_players.values()) {
            p = ba.getFuzzyPlayer(i.getPlayerName());
            if (p != null) {
                if (p.getFrequency() != m_fnFrequency && p.getFrequency() != 8025) 
                    ba.setFreq(i.getPlayerName(), m_fnFrequency);                

                if (i.getPlayerState() == 1) {
                    if (m_fnFrequency == 0) {
                        if (i.getIsGoalie())
                            ba.warpTo(i.getPlayerName(), 388, 512);
                        else
                            ba.warpTo(i.getPlayerName(), 480, 580);
                    } else if (m_fnFrequency == 1) {
                        if (i.getIsGoalie())
                            ba.warpTo(i.getPlayerName(), 635, 512);
                        else
                            ba.warpTo(i.getPlayerName(), 550, 580);
                    }
                }
            }
        }
    }
    
    /**
     * Sets a player that will take a penalty shot
     * 
     * @param msg
     */
    public void setPlayerShot(String msg) {
        Player p;
        twhtPlayer pA;

        p = ba.getFuzzyPlayer(msg);
        if (p != null) {
            if (p.getFrequency() != m_fnFrequency && p.getFrequency() != 8025) 
                ba.setFreq(p.getPlayerName(), m_fnFrequency);
            
            pA = searchPlayer(p.getPlayerName());
            
            if (pA == null || pA.getPlayerState() != 1)
                return;
            
            ba.warpTo(pA.getPlayerName(), 510, 560);
        }
    }

    /**
     * Sets the proper stats when the team scores a goal
     */
    public void setScoreFor() {
        m_fnTeamScore++;
        
        for (twhtPlayer i : m_players.values()) {
            if (i.getPlayerState() == 1)
            i.doStats(12);
        }
    }
    
    /**
     * Sets the proper stats when a goal is scored agains the team
     */
    public void setScoreAgainst() {
        for (twhtPlayer i : m_players.values()) {
            if (i.getPlayerState() == 1)
                i.doStats(13);
        }
    }
    
    /**
     * Sets the start of the playtime for the players in the game at the begining
     * of the round.
     */
    public void setStartRound() {
        for (twhtPlayer i : m_players.values()) { 
            if (i.getPlayerState() == 1)
                i.setShipStart(m_game.m_curRound.getIntTime());
        }
    }
    
    /**
     * Sets the end of round timestamp when the round ends
     */
    public void setEndRound() {
        for (twhtPlayer i : m_players.values()) { 
            if (i.getPlayerState() == 1)
            i.setShipEnd(m_game.m_curRound.getIntTime());
        }
    }
    
    /**
     * Returns the team's score for the game.
     * 
     * @return
     */
    public Integer getTeamScore() {
        return m_fnTeamScore;
    }
    
    /**
     * Returns the player's current state.
     * 
     * @param player
     * @return
     */
    public Integer getPlayerState(String player) {
        twhtPlayer p;
        int playerState = -1;

        p = m_players.get(player);

        if (p != null) 
            playerState = p.getPlayerState();        

        return playerState;
    }
    
    /**
     * If the team has a goalie in it will return the name of the player
     * 
     * @return
     */
    public String getGoalie() {
        String playerName = null;
        
        for(twhtPlayer pA : m_players.values()) {
            if (pA.getIsGoalie() && pA.getPlayerState() == 1) 
                 playerName = pA.getPlayerName();
        }
        
        return playerName;
    }
    
    /**
     * Watches for possible penalties that are ending or need a warning for the game
     * 
     * @param gameTime
     */
    public void searchPenalties(int gameTime) {
        int penaltyTime;
        int penaltyWarning;

        for (twhtPlayer i : m_players.values()) {
            penaltyTime = i.getPenalty();
            penaltyWarning = i.getPenaltyWarning();

            if (penaltyTime == gameTime) 
                m_game.doRemovePenalty(m_game.m_fcRefName, i.getPlayerName());
            else if (penaltyWarning == gameTime)
                ba.sendPrivateMessage(m_game.m_fcRefName, "Penalty on " + i.getPlayerName() + " is about to expire in 10 seconds.");            
        }
    }    

    /**
     * Searches for a player and returns it as a twhtPlayer variable
     * 
     * @param name
     * @return
     */
    public twhtPlayer searchPlayer(String name) {
        twhtPlayer playerName;
        name = name.toLowerCase();

        playerName = null;

        for (twhtPlayer i : m_players.values()) {
            if (i.getPlayerName().startsWith(name)) {
                if (playerName == null) 
                    playerName = i;
                else if (i.getPlayerName().compareTo(playerName.getPlayerName()) > 0) 
                    playerName = i;                
            }
        }
        return playerName;
    }

    /**
     * Clears all the penalties for the team
     */
    public void clearTeamPenalties() {
        for (twhtPlayer i : m_players.values()) {
            if (i.getPenalty() > 0)
                i.resetPenalty();
        }
    }


    //    public void storePlayerResults() {
    //        ListIterator<MatchPlayer> i = m_players.listIterator();
    //
    //        while (i.hasNext()) {
    //            i.next().storePlayerResult(m_round.m_fnMatchRoundID, m_fnTeamNumber);
    //        }
    //    }

}
