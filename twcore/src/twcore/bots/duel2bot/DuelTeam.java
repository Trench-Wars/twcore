package twcore.bots.duel2bot;

import java.sql.ResultSet;
import java.util.TimerTask;

import twcore.bots.duel2bot.DuelGame;
import twcore.bots.duel2bot.DuelPlayer;
import twcore.bots.duel2bot.duel2bot;
import twcore.core.BotAction;
import twcore.core.BotSettings;

public class DuelTeam {

    BotAction m_botAction;

    BotSettings m_rules;

    String db = "website";

    int m_teamID;
    int m_team;
    int m_freq;
    int m_score;
    int m_type;
    int m_ship;
    
    private int[] safe1, safe2, spawn1, spawn2;
    
    // status constants
    public static final int SOLO = 0;
    public static final int HERE = 1;
    public static final int GAME = 2;
    
    int m_status;
    
    boolean m_out;
    boolean record;

    TimerTask go;
    String[] m_name;
    int[] m_userID;
    DuelPlayer[] m_player;
    DuelGame m_game;
    duel2bot m_bot;
    
    public DuelTeam(duel2bot bot, int id, int freq, String[] player, int[] coords, DuelGame game) {
        record = false;
        m_bot = bot;
        m_game = game;
        m_botAction = m_bot.m_botAction;
        m_rules = m_game.m_rules;
        m_type = m_game.m_type;
        m_freq = freq;
        m_bot.m_freqs.addElement(freq);
        m_player = new DuelPlayer[2];
        m_out = false;
        m_score = 0;
        m_name = player;
        m_teamID = id;
        m_status = HERE;
        // m_ships
        if (m_type == 1)
            m_ship = 1;
        else if (m_type == 2)
            m_ship = 2;
        else if (m_type == 3)
            m_ship = 3;
        else if (m_type == 4)
            m_ship = 7;
        else if (m_type == 5)
            m_ship = -1;

        // safeAx1,safeAy1,Ax1,Ay1,safeAx2,safeAy2,Ax2,Ay2
        safe1 = new int[] { coords[2], coords[3] };
        spawn1 = new int[] { coords[0], coords[1] };
        safe2 = new int[] { coords[6], coords[7] };
        spawn2 = new int[] { coords[4], coords[5] };
        m_player[0] = m_bot.m_players.get(player[0].toLowerCase());
        m_player[1] = m_bot.m_players.get(player[1].toLowerCase());
        m_player[0].team(this);
        m_player[1].team(this);
        
    }
    
    public DuelTeam(int id, String name) {
        record = true;
        m_teamID = id;
        
    }
    
    public void setScore(int s) {
        m_score = s;
    }
    
    public int getDeaths() {
        return m_player[0].getDeaths() + m_player[1].getDeaths();
    }
    
    public int getTeamID() {
        return m_teamID;
    }
    
    public int getFreq() {
        return m_freq;
    }
    
    public String[] getNames() {
        return m_name;
    }
    
    public int getShip(String name) {
        if (m_name[0].equalsIgnoreCase(name))
            return m_player[0].m_ship;
        else if (m_name[1].equalsIgnoreCase(name))
            return m_player[1].m_ship;
        else
            return -1;
    }
    
    public boolean wasTK(String killee, String killer) {
        if (killer.equalsIgnoreCase(getPartner(killee))) {
            return true;
        } else {
            return false;
        }
    }
    
    public void playerOut(DuelPlayer player) {
        if ((m_player[0].status() == DuelPlayer.OUT) && (m_player[1].status() == DuelPlayer.OUT))
            m_out = true;
        
        m_game.playerOut(player);
    }
    
    public boolean out() {
        return m_out;
    }
    
    public void lagout(String name) {
        m_botAction.sendPrivateMessage(getPartner(name), "Your partner has lagged out or specced, and has 1 minute to return or will forefeit.");
        m_game.lagout(m_teamID);
    }
    
    public void opLagout() {
        m_botAction.sendPrivateMessage(m_name[0], "Your opponent has lagged out or specced, and has 1 minute to return or will forfeit.");
        m_botAction.sendPrivateMessage(m_name[1], "Your opponent has lagged out or specced, and has 1 minute to return or will forfeit.");
    }
    
    public void returned(String name) {
        m_botAction.sendPrivateMessage(getPartner(name), "Your partner has returned from being lagged out."); 
        m_game.returned(m_teamID);
    }
    
    public void opReturned() {
        m_botAction.sendPrivateMessage(m_name[0], "Your opponent has returned from being lagged out.");
        m_botAction.sendPrivateMessage(m_name[1], "Your opponent has returned from being lagged out.");
    }
    
    public String getPartner(String name) {
        if (name.equalsIgnoreCase(m_name[0]))
            return m_name[1];
        else
            return m_name[0];
    }
    
    public DuelPlayer getPlayer(String name) {
        if (name.equalsIgnoreCase(m_name[0]))
            return m_player[0];
        else if (name.equalsIgnoreCase(m_name[1]))
            return m_player[1];
        else
            return null;
    }
    
    public void startGame(boolean mixed, String[] nme) {
        m_status = GAME;
        m_player[0].starting(m_ship, safe1[0], safe1[1]);
        m_player[1].starting(m_ship, safe2[0], safe2[1]);
        
        go = new TimerTask() {
            @Override
            public void run() {
                m_game.m_state = DuelGame.IN_PROGRESS;
                if (!m_bot.m_laggers.containsKey(m_name[0].toLowerCase())) {
                    m_player[0].warp(spawn1[0], spawn1[1]);
                    m_botAction.sendPrivateMessage(m_name[0], "GO GO GO!!!", 104);
                }
                if (!m_bot.m_laggers.containsKey(m_name[1].toLowerCase())) {
                    m_player[1].warp(spawn2[0], spawn2[1]);
                    m_botAction.sendPrivateMessage(m_name[1], "GO GO GO!!!", 104);
                }
            }
        };
        if (mixed) {
            m_botAction.sendPrivateMessage(m_name[0], "Duel Begins in 30 Seconds Against '" + nme[0] + "' and '" + nme[1] + "'", 29);
            m_botAction.sendPrivateMessage(m_name[0], "You may change your ship until that time! No ship changes will be allowed after the duel starts.");
            m_botAction.sendPrivateMessage(m_name[1], "Duel Begins in 30 Seconds Against '" + nme[0] + "' and '" + nme[1] + "'", 29);
            m_botAction.sendPrivateMessage(m_name[1], "You may change your ship until that time! No ship changes will be allowed after the duel starts.");
            m_botAction.scheduleTask(go, 30000);
        } else {
            m_botAction.sendPrivateMessage(m_name[0], "Duel Begins in 15 Seconds Against '" + nme[0] + "' and '" + nme[1] + "'", 27);
            m_botAction.sendPrivateMessage(m_name[1], "Duel Begins in 15 Seconds Against '" + nme[0] + "' and '" + nme[1] + "'", 27);
            m_botAction.scheduleTask(go, 15000);
        }
    }
    
    public void endGame() {
        m_botAction.cancelTask(go);
        m_bot.m_freqs.removeElement(m_freq);
        m_bot.m_playing.remove(m_name[0].toLowerCase());
        m_bot.m_playing.remove(m_name[1].toLowerCase());
        m_freq = 0;
        m_game = null;
        m_rules = null;
        m_score = 0;
        m_status = HERE;
        m_ship = 0;
        m_player[0].endGame();
        m_player[1].endGame();
        
    }
    
    public void spawn(DuelPlayer player) {
        String name = player.getName();
        if (name.equalsIgnoreCase(m_name[0])) {
            player.warp(spawn1[0], spawn1[1]);
        } else {
            player.warp(spawn2[0], spawn2[1]);
        }        
    }
    
    public void safe(DuelPlayer player) {
        String name = player.getName();
        if (name.equalsIgnoreCase(m_name[0])) {
            player.warp(safe1[0], safe1[1]);
        } else {
            player.warp(safe2[0], safe2[1]);
        }        
    }
    
    public void warp(DuelPlayer player) {
        WarpPoint wp = m_game.m_box.getRandomWarpPoint();
        player.warp(wp.getXCoord(), wp.getYCoord());
    }
    
    public void warpWarper(DuelPlayer player) {
        WarpPoint wp = m_game.m_box.getRandomWarpPoint();
        player.warpWarper(wp.getXCoord(), wp.getYCoord());
    }
    
    public void spawn() {
        m_player[0].warp(spawn1[0], spawn1[1]);
        m_player[1].warp(spawn2[0], spawn2[1]);
    }
    
    public void safe() {
        m_player[0].warp(safe1[0], safe1[1]);
        m_player[1].warp(safe2[0], safe2[1]);
    }
    
    public void sql_setup() {

        ResultSet rs;
        String query = "SELECT fnLeagueTypeID AS type, fnUser1ID AS id1, u1.fcUserName AS p1, fnUser2ID AS id2, u2.fcUserName AS p2 FROM tblDuel__2team JOIN tblUser u1 ON fnUser1ID = u1.fnUserID JOIN tblUser u2 ON fnUser2ID = u2.fnUserID WHERE fnSeason = " + m_bot.d_season + " AND fnStatus = 1 AND fnTeamID = " + m_teamID + " LIMIT 1";
        
        try {
            rs = m_botAction.SQLQuery(db, query);

            if (rs.next()) {
                m_type = rs.getInt("type");
                m_userID = new int[] { rs.getInt("id1"), rs.getInt("id2") };
                m_name = new String[] { rs.getString("p1"), rs.getString("p2") };
                
                m_bot.m_teams.put(m_teamID, this);
            }
            
            m_botAction.SQLClose(rs);
        } catch (Exception e) {
            System.out.println("SQLException teamSetup for " + m_teamID);
        }
    }
    
}
