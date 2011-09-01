package twcore.bots.twdt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.events.FlagClaimed;
import twcore.core.events.PlayerDeath;
import twcore.core.events.TurretEvent;
import twcore.core.game.Player;
import twcore.core.util.Tools;

import twcore.bots.twdt.DraftGame.GameType;
import twcore.bots.twdt.DraftPlayer.Status;
import twcore.bots.twdt.DraftRound.RoundState;

/**
 *
 * @author WingZero
 */
public class DraftTeam {

    public static final String db = "website";
    
    BotAction ba;
    OperatorList opList;
    BotSettings rules;
    GameType type;
    
    DraftRound round;
    HashMap<String, DraftPlayer> players;
    int score, freq, teamID;
    int[] ships;
    String[] caps;
    String teamName;
    boolean ready, flag;
    
    
    public DraftTeam(DraftRound gameRound, String name, int id, int freqNum) {
        players = new HashMap<String, DraftPlayer>();
        round = gameRound;
        ba = round.ba;
        opList = round.opList;
        rules = round.rules;
        type = round.type;
        ships = round.game.ships;
        caps = new String[3];
        teamName = name;
        teamID = id;
        freq = freqNum;
        score = 0;
        ready = false;
        flag = false;
        loadTeam();
    }
    
    public void handleEvent(PlayerDeath event) {
        
    }
    
    public void handleEvent(TurretEvent event) {
        
    }

    public void handleEvent(FlagClaimed event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (name != null) {
            DraftPlayer dp = getPlayer(name);
            if (dp != null && dp.getStatus() == Status.IN) {
                score++;
                dp.handleFlagClaim();
            }
        }
    }
    
    public void handleLagout(String name) {
        
    }
    
    public void do_ready(String cap) {
        if (!isCaptain(cap)) 
            return;
        if (round.getState() == RoundState.LINEUPS) {
            if (ready) {
                ready = false;
                ba.sendArenaMessage(teamName + " is not ready to begin.");
            } else if (players.size() >= round.game.minPlayers) {
                ready = true;
                ba.sendArenaMessage(teamName + " is ready to begin.");
            } else if (!ready)
                ba.sendPrivateMessage(cap, "You must have at least " + round.game.minPlayers + " players.");
        }
    }
    
    public void do_add(String cap, String cmd) {
         
    }
    
    public void do_sub(String cap, String cmd) {
        
    }
    
    public void do_change(String cap, String cmd) {
        
    }
    
    public void do_switch(String cap, String cmd) {
        
    }
    
    public void do_myFreq(String name) {
        Player p = ba.getPlayer(name);
        if (p != null && name.equalsIgnoreCase(p.getSquadName()))
            ba.setFreq(name, freq);
    }
    
    public void do_lagout(String name, String cmd) {
        
    }
    
    public boolean getReady() {
        return ready;
    }
    
    public int getScore() {
        if (type == GameType.BASING)
            return score;
        else
            return getDeaths();
    }
    
    public int getTime() {
        return score;
    }
    
    public int getDeaths() {
        int deaths = 0;
        for (DraftPlayer p : players.values())
            deaths += p.getDeaths();
        return deaths;
    }
    
    public String getName() {
        return teamName;
    }
    
    public DraftPlayer getPlayer(String name, boolean exact) {
        if (exact)
            return players.get(low(name));
        else {
            name = low(name);
            String best = null;
            for (String pName : players.keySet()) {
                if (pName.startsWith(name)) {
                    if (pName.equals(name))
                        return players.get(name);
                    if (best != null) {
                        if (pName.compareTo(best) > 0)
                            best = pName;
                    } else
                        best = pName;
                }
            }
            if (best != null)
                return players.get(best);
            else return null;
        }
    }
    
    public DraftPlayer getPlayer(String name) {
        return getPlayer(name, true);
    }
    
    public void addPoint() {
        score++;
    }
    
    public int getSize() {
        return players.size();
    }
    
    public boolean hasFlag() {
        return flag;
    }
    
    public boolean isPlaying(String name) {
        return players.containsKey(low(name));
    }
    
    public boolean isCaptain(String name) {
        for (String cap : caps)
            if (name.equalsIgnoreCase(cap))
                return true;
        return false;
    }
    
    private void loadTeam() {
        String query = "SELECT * FROM tblDraft__Team WHERE fnTeamID = " + teamID + " LIMIT 1";
        try {
            ResultSet rs = ba.SQLQuery(db, query);
            if (rs.next()) {
                caps[0] = rs.getString("fcCap");
                caps[1] = rs.getString("fcAss1");
                caps[2] = rs.getString("fcAss2");
            }
            ba.SQLClose(rs);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }
    
    private String low(String str) {
        return str.toLowerCase();
    }
}
