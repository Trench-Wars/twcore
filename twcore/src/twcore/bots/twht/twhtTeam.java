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
import twcore.core.BotSettings;
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

    public void addPlayer(String name, int shipType) {
        final String playerName = name;
        final int playerShip = shipType;

        m_players.put(name, new twhtPlayer(name.toLowerCase(), m_fcTeamName, shipType, 1, ba, this));
        
        addDelay = new TimerTask() {
            public void run() {
                
                ba.sendArenaMessage(playerName + " has been added for " + m_fcTeamName, 2);
                ba.setShip(playerName, playerShip);
                ba.setFreq(playerName, m_fnFrequency);
            }
        };
        ba.scheduleTask(addDelay, Tools.TimeInMillis.SECOND * 2);

    }

    public void subPlayer(String playerA, String playerB, int shipType) {
        final String playerAName = playerA;
        final String playerBName = playerB;
        final int playerShip = shipType;
        final Player p;

        twhtPlayer pA;

        pA = m_players.get(playerAName);

        pA.playerSubbed();
        p = ba.getPlayer(playerBName);
        m_players.put(playerB, new twhtPlayer(playerBName.toLowerCase(), m_fcTeamName, playerShip, 1, ba, this));

        substituteDelay = new TimerTask() {
            public void run() {
                if (p != null)
                    ba.specWithoutLock(playerAName);
                ba.setShip(playerBName, playerShip);
                ba.setFreq(playerBName, m_fnFrequency);
                ba.sendArenaMessage(playerAName + " has been subbed for " + playerBName, 2);
            }
        };
        ba.scheduleTask(substituteDelay, Tools.TimeInMillis.SECOND * 2);
    }

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
        };
        ba.scheduleTask(switchDelay, Tools.TimeInMillis.SECOND * 2);
    }

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
        };
        ba.scheduleTask(switchDelay, Tools.TimeInMillis.SECOND * 2);
    }

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
        };
        ba.scheduleTask(removeDelay, Tools.TimeInMillis.SECOND * 2);
    }

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
            };
            ba.scheduleTask(lagoutDelay, Tools.TimeInMillis.SECOND * 2);
        }

    }
    
    public void doLagout(String name) {
        String p;
        twhtPlayer pLag;

        for (twhtPlayer i : m_players.values()) {
            p = ba.getFuzzyPlayerName(i.getPlayerName());

            if (p != null)
                ba.sendPrivateMessage(p, name + " has lagged out of the game.");
        }

        pLag = m_players.get(name);
        pLag.playerLaggedOut();
    }

    public boolean isCaptain(String name) {
        if (m_captains.contains(name))
            return true;
        else
            return false;
    }

    public boolean isPlayer(String name) {
        if (m_players.containsKey(name))
            return true;
        else
            return false;
    }

    public boolean isRostered(String name) {
        if (roster.contains(name))
            return true;
        else
            return false;
    }

    public boolean isLaggedOut(String name) {
        twhtPlayer p;
        int playerState;

        if (isPlayer(name)) {
            p = m_players.get(name);

            if (p == null)
                return false;

            playerState = p.getPlayerState();

            if (playerState == 3)
                return true;
            else
                return false;
        } else {
            return false;
        }
    }

    public twhtPlayer searchPlayer(String name) {
        twhtPlayer playerName;
        name = name.toLowerCase();

        playerName = null;

        for (twhtPlayer i : m_players.values()) {
            if (i.getPlayerName().startsWith(name)) {
                if (playerName == null) {
                    playerName = i;
                } else if (i.getPlayerName().compareTo(playerName.getPlayerName()) > 0) {
                    playerName = i;
                }
            }
        }

        return playerName;
    }

    public String getTeamName() {
        return m_fcTeamName;
    }

    public int getFrequency() {
        return m_fnFrequency;
    }
    
       
    public void getTeamStats() {       
               
                ba.sendArenaMessage(Tools.formatString("|" + getTeamName(), 73, "-"));
                ba.sendArenaMessage("|SHIPS       |PLAYERS  |G |A |SH |ST |TO |CM |CT |PC |PCT  |T    |+/-|RATING",1);               
                for(twhtPlayer i : m_players.values())
                    i.reportPlayerStats();
                ba.sendArenaMessage(Tools.formatString("|", 73, " "));
                ba.sendArenaMessage("|    |GOALIES  |SV |SV%|GA |ST |TO |CM |CT  |GT   |A |+/-|RATING");
                for(twhtPlayer i : m_players.values())
                    i.reportGoalieStats();
                ba.sendArenaMessage(Tools.formatString("|", 73, " "));
    }

    public void setFrequency(int freq) {
        m_fnFrequency = freq;
    }

    public void setCenter(String centerName) {
        m_CenterName = centerName;
    }

    public void setFreqAndSide() {
        Player p;

        for (twhtPlayer i : m_players.values()) {
            p = ba.getFuzzyPlayer(i.getPlayerName());
            if (p != null) {
                if (p.getFrequency() != m_fnFrequency && p.getFrequency() != 8025) {
                    ba.setFreq(i.getPlayerName(), m_fnFrequency);
                }

                if (i.getPlayerState() == 1) {

                    if (m_fnFrequency == 0) {

                        if (i.getIsGoalie()) {
                            ba.warpTo(i.getPlayerName(), 388, 512);
                        } else if (i.getPlayerName().equals(m_CenterName)) {
                            ba.warpTo(i.getPlayerName(), 505, 512);
                        } else {
                            ba.warpTo(i.getPlayerName(), 445, 512);
                        }

                    } else if (m_fnFrequency == 1) {

                        if (i.getIsGoalie()) {
                            ba.warpTo(i.getPlayerName(), 635, 512);
                        } else if (i.getPlayerName().equals(m_CenterName)) {
                            ba.warpTo(i.getPlayerName(), 518, 512);
                        } else {
                            ba.warpTo(i.getPlayerName(), 578, 512);
                        }
                    }
                }
            }
        }
    }
    
    public void setPenaltyShotWarp() {
        Player p;

        for (twhtPlayer i : m_players.values()) {
            p = ba.getFuzzyPlayer(i.getPlayerName());
            if (p != null) {
                if (p.getFrequency() != m_fnFrequency && p.getFrequency() != 8025) {
                    ba.setFreq(i.getPlayerName(), m_fnFrequency);
                }

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
    
    public void setPlayerShot(String msg) {
        Player p;
        twhtPlayer pA;

        p = ba.getFuzzyPlayer(msg);
        if (p != null) {
                if (p.getFrequency() != m_fnFrequency && p.getFrequency() != 8025) {
                    ba.setFreq(p.getPlayerName(), m_fnFrequency);
                }
                pA = searchPlayer(p.getPlayerName());
                
                if (pA == null && pA.getPlayerState() != 1)
                    return;
                
                ba.warpTo(pA.getPlayerName(), 510, 560);
            }
        } 
    
    
    public void setScoreFor() {
        m_fnTeamScore++;
        
        for (twhtPlayer i : m_players.values()) {
            if (i.getPlayerState() == 1)
            i.doStats(12);
        }
    }
    
    public void setScoreAgainst() {
        for (twhtPlayer i : m_players.values()) {
            if (i.getPlayerState() == 1)
                i.doStats(13);
        }
    }
    
    public void setStartRound() {
        for (twhtPlayer i : m_players.values()) { 
            if (i.getPlayerState() == 1)
                i.setShipStart(m_game.m_curRound.getIntTime());
        }
    }
    
    public void setEndRound() {
        for (twhtPlayer i : m_players.values()) { 
            if (i.getPlayerState() == 1)
            i.setShipEnd(m_game.m_curRound.getIntTime());
        }
    }
    
    public Integer getTeamScore() {
        return m_fnTeamScore;
    }
    
    public Integer getPlayerState(String player) {
        twhtPlayer p;
        int playerState = -1;

        p = m_players.get(player);

        if (p != null) {
            playerState = p.getPlayerState();
        }

        return playerState;
    }
    
    public String getGoalie() {
        String playerName = null;
        
        for(twhtPlayer pA : m_players.values()) {
            if (pA.getIsGoalie()) 
                 playerName = pA.getPlayerName();
        }
        
        return playerName;
    }
    public void searchPenalties(int gameTime) {
        int penaltyTime;
        int penaltyWarning;

        for (twhtPlayer i : m_players.values()) {
            penaltyTime = i.getPenalty();
            penaltyWarning = i.getPenaltyWarning();

            if (penaltyTime == gameTime) {
                m_game.removePenalty(m_game.m_fcRefName, i.getPlayerName());
            } else if (penaltyWarning == gameTime) {
                ba.sendPrivateMessage(m_game.m_fcRefName, "Penalty on " + i.getPlayerName() + " is about to expire in 10 seconds.");
            }
        }
    }

    public void clearTeamPenalties() {
        for (twhtPlayer i : m_players.values()) {
            if (i.getPenalty() > 0)
                i.resetPenalty();
        }
    }

    public void resetVariables() {

    }

    //    public void storePlayerResults() {
    //        ListIterator<MatchPlayer> i = m_players.listIterator();
    //
    //        while (i.hasNext()) {
    //            i.next().storePlayerResult(m_round.m_fnMatchRoundID, m_fnTeamNumber);
    //        }
    //    }

}
