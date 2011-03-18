package twcore.bots.duel2bot;

import java.sql.ResultSet;
import java.util.TimerTask;

import twcore.bots.duel2bot.DuelGame;
import twcore.bots.duel2bot.DuelTeam;
import twcore.bots.duel2bot.duel2bot;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.Tools;

public class DuelPlayer {

    boolean record;
    BotAction m_botAction;
    Player m_player;
    BotSettings m_rules;
    
    private static final String DB = "website";

    // Regular game stats
    int m_ship = 1;
    int m_specAt = 2;
    int m_freq = 0;
    int m_lagouts = 0;
    int m_status = 0;
    int m_out = -1;
    
    
    
    int m_kills;
    int m_deaths;
    int m_spawns;
    int m_warps;
    
    int m_userID;
    
    DuelTeam m_team;
    duel2bot m_bot;
    
    // holds all registered league team IDs 
    // index = league, 0 = userID
    int[] m_teams;
    
    String m_name;
    
    // TWEL Info
    int d_noCount;
    static int d_season;
    static int d_deathTime;
    static int d_spawnTime;
    static int d_spawnLimit;
    static int d_maxLagouts;
    
    // constant player state values for status
    static final int SPEC = -1;
    static final int IN = 0;
    static final int PLAYING = 1;
    static final int WARPING = 2;
    static final int LAGGED = 3;
    static final int OUT = 4;
    static final int REOUT = 5;
    static final int RETURN = 6;
    
    // constant player values for removal reason
    static final int NORMAL = 0;
    static final int WARPS = 1;
    static final int LAGOUTS = 2;
    static final int SPAWNS = 3;
    
    private long m_lastFoul = 0;
    private long m_lastDeath = 0;
    private String m_lastKiller = "";
    
    String m_challPartner = null;
    int m_scrimFreq = -1;
    
    TimerTask spawner, dying;
    TimerTask lagout;
    
    public DuelPlayer(Player p, duel2bot bot) {
        m_name = p.getPlayerName();
        m_bot = bot;
        m_team = null;
        m_teams = new int[6];
        m_botAction = m_bot.m_botAction;
        m_rules = null;
        m_freq = p.getFrequency();
        m_ship = p.getShipType();
        if (m_ship > 0) 
            m_status = IN;
        else 
            m_status = SPEC;
        d_season = m_bot.d_season;
        d_noCount = m_bot.d_noCount;
        d_deathTime = m_bot.d_deathTime;
        d_spawnTime = m_bot.d_spawnTime;
        d_spawnLimit = m_bot.d_spawnLimit;
        d_maxLagouts = m_bot.d_maxLagouts;
        m_kills = 0;
        m_deaths = 0;
        m_spawns = 0;
        m_warps = 0;
    }
    
    public DuelPlayer(String name, DuelTeam team, duel2bot bot) {
        m_name = name;
        m_bot = bot;
        m_team = team;
        m_teams = new int[6];
        m_botAction = m_bot.m_botAction;
        m_rules = m_team.m_rules;
        m_freq = m_team.m_freq;
        d_season = m_bot.d_season;
        d_noCount = m_bot.d_noCount;
        d_deathTime = m_bot.d_deathTime;
        d_spawnTime = m_bot.d_spawnTime;
        d_spawnLimit = m_bot.d_spawnLimit;
        d_maxLagouts = m_bot.d_maxLagouts;
        m_kills = 0;
        m_deaths = 0;
        m_spawns = 0;
        m_warps = 0;
    }
    
    public DuelPlayer(String name, duel2bot bot) {
        m_name = name;
        m_bot = bot;
        m_team = null;
        m_teams = new int[6];
        m_botAction = m_bot.m_botAction;
        m_rules = null;
        m_freq = 9999;
        d_season = m_bot.d_season;
        d_noCount = m_bot.d_noCount;
        d_deathTime = m_bot.d_deathTime;
        d_spawnTime = m_bot.d_spawnTime;
        d_spawnLimit = m_bot.d_spawnLimit;
        d_maxLagouts = m_bot.d_maxLagouts;
        m_kills = 0;
        m_deaths = 0;
        m_spawns = 0;
        m_warps = 0;
    }
    
    public void team(DuelTeam team) {
        m_team = team;
        m_rules = m_team.m_rules;
        m_freq = m_team.m_freq;
        if (m_team.m_type != 5)
            m_ship = m_team.m_ship;
        else {
            Player p = m_botAction.getPlayer(m_name);
            m_ship = p.getShipType();
        }
    }
    
    public void scrim(String name, int freq) {
        m_challPartner = name;
        m_scrimFreq = freq;
    }
    
    public void cancelScrim() {
        m_challPartner = null;
        m_scrimFreq = -1;
    }
    
    public String getName() {
        return m_name;
    }
    
    public void handlePosition(PlayerPosition event) {
        if (m_status == WARPING || m_status == LAGGED || m_status == OUT || m_status == REOUT || m_status == RETURN)
            return;
        
        int x = event.getXLocation() / 16;
        int y = event.getYLocation() / 16;
        //Player p = m_botAction.getPlayer(m_name);
        // 416 591
        if (m_team != null) {
            if ((x < m_team.m_game.m_box.getAreaMinX()) || (y < m_team.m_game.m_box.getAreaMinY()) || (x > m_team.m_game.m_box.getAreaMaxX()) || (y > m_team.m_game.m_box.getAreaMaxY())) {
                warped(true);
            }
        }
    }
    
    public void warped(boolean pos) {
        if (m_status == WARPING || m_status == RETURN)
            return;        

        status(WARPING);

        long now = System.currentTimeMillis();
        if ((now - m_lastFoul > 500) && (m_team.m_game.m_state == DuelGame.IN_PROGRESS))
            m_warps++;
        
        if (m_warps < 5 && m_team.m_game.m_state == DuelGame.IN_PROGRESS) {
            if (now - m_lastFoul > 500) {
                if (pos)
                    m_botAction.sendPrivateMessage(m_name, "Warping is illegal in this league and if you warp again, you will forfeit.");
                else {
                    m_botAction.sendPrivateMessage(m_name, "Changing freq or ship is illegal in this league and if you do this again, you will forfeit.");
                }
            }
            m_team.warpWarper(this);
        } else if (m_team.m_game.m_state != DuelGame.IN_PROGRESS) {
            m_team.safe(this);
        }else {
            m_botAction.sendPrivateMessage(m_name, "You have forfeited due to warp abuse.");
            remove(WARPS);
        }
        
        m_lastFoul = now;
    }
    
    public void handleFreq(FrequencyChange event) {
        if (m_status == WARPING || m_status == RETURN)
            return;
        int freq = event.getFrequency();
        
        if (m_team != null && m_team.m_game != null) {
            if (freq != m_freq) {
                if (m_status == LAGGED) {
                    status(WARPING);
                    m_botAction.setFreq(m_name, m_freq);
                    status(LAGGED);
                } else if (m_status == PLAYING){
                    status(WARPING);
                    m_botAction.setFreq(m_name, m_freq);
                    m_botAction.specificPrize(m_name, -13);
                    status(PLAYING);
                    warped(false);
                } else if (m_status == OUT) {
                    m_botAction.sendPrivateMessage(m_name, "Please stay on your freq until your duel is finished.");
                    m_botAction.setFreq(m_name, m_freq);
                    status(OUT);
                }
            }
        } else if (m_bot.m_freqs.contains(freq)) {
            if (m_freq == 9999)
                m_botAction.specWithoutLock(m_name);
            m_botAction.setFreq(m_name, m_freq);
        } else {
            if (freq != m_scrimFreq) {
                m_bot.removeScrimChalls(m_scrimFreq);
                m_challPartner = null;
                m_scrimFreq = -1;
            }
            m_freq = freq;
        }
    }
    
    public void handleFSC(FrequencyShipChange event) {
        int ship = event.getShipType();
        if (m_status == WARPING || ((m_status == LAGGED || m_status == OUT) && ship == 0) || m_status == RETURN)
            return;
        int freq = event.getFrequency();
        int status = m_status;
        status(WARPING);
        if (status == OUT) {
            m_botAction.sendPrivateMessage(m_name, "Please stay in spec and on your freq until your duel is finished.");
            m_botAction.specWithoutLock(m_name);
            if (m_freq != freq)
                m_botAction.setFreq(m_name, m_freq);
            status(OUT);
            return;
        } else if (status == LAGGED) {
            m_botAction.specWithoutLock(m_name);
            if (m_freq != freq)
                m_botAction.setFreq(m_name, m_freq);
            m_botAction.sendPrivateMessage(m_name, "Please use !lagout to return to your duel.");
            status(LAGGED);
            return;                
        }
        if (m_team == null) {
            if (ship == 4 || ship == 6) {
                if (m_ship != 0)
                    m_botAction.setShip(m_name, m_ship);
                else
                    m_botAction.specWithoutLock(m_name);
                m_botAction.sendPrivateMessage(m_name, "Invalid ship!");
            } else if (ship == 0 && m_scrimFreq > -1 && m_challPartner != null) {
                m_bot.removeScrimChalls(m_scrimFreq);
                m_scrimFreq = -1;
                m_challPartner = null;
                m_ship = ship;
            } else 
                m_ship = ship;
            
            /*
            if (m_bot.m_freqs.contains(freq))
                m_botAction.setFreq(m_name, m_freq);
            else
                m_freq = freq;
            */
                
        } else {
            
            boolean foul = false;
            if (ship == 0 && (status == PLAYING)) {
                m_botAction.setFreq(m_name, m_freq);
                lagout();
                return;
            }
            
            /*
            if (freq != m_freq) {
                foul = true;
                m_botAction.setFreq(m_name, m_freq);
            }
            */
            
            if ((ship != m_ship) && (m_team.m_game.m_state != DuelGame.SETUP)) {
                foul = true;
                m_botAction.setShip(m_name, m_ship);
                m_botAction.specificPrize(m_name, -13);
            } else if ((m_team.m_game.m_type == 5) && (m_team.m_game.m_state == DuelGame.SETUP)) {
                if (ship == 6 || ship == 4)
                    m_botAction.setShip(m_name, m_ship);
                else
                    m_ship = ship;
            } else if ((ship != m_ship) && (m_team.m_game.m_type != 5) && (m_team.m_game.m_state == DuelGame.SETUP)) {
                m_botAction.setShip(m_name, m_ship);
            }
            
            if (foul || m_team.m_game.m_state == DuelGame.IN_PROGRESS) {
                m_status = status;
                warped(false);
                return;
            } else 
                m_team.safe(this);
        }
        m_status = status;
    }
    
    public void lagout() {
        if (m_team == null)
            return;

        status(LAGGED);
        
        if (m_team.m_game.m_state == DuelGame.IN_PROGRESS)
            m_lagouts++;
        
        if (m_lagouts < d_maxLagouts) {
            m_botAction.sendSmartPrivateMessage(m_name, "You have 1 minute to return (!lagout) to your duel or you will forfeit! (!lagout)");
            lagout = new TimerTask() {
                @Override
                public void run() {
                    m_bot.m_laggers.remove(m_name.toLowerCase());
                    m_botAction.sendSmartPrivateMessage(m_name, "You have forfeited since you have been lagged out for over a minute.");
                    remove(LAGOUTS);                    
                }
            };
            m_botAction.scheduleTask(lagout, 60000);
            m_bot.m_laggers.put(m_name.toLowerCase(), this);
            m_team.lagout(m_name);
        } else {
            m_botAction.sendSmartPrivateMessage(m_name, "You have exceeded the lagout limit and forfeit your duel.");
            remove(LAGOUTS);
        }
    }
    
    public void handleLagout() {
        if (m_status != LAGGED) {
            m_botAction.sendPrivateMessage(m_name, "You are not lagged out.");
            return;
        }
        status(RETURN);
        m_botAction.cancelTask(lagout);
        m_bot.m_laggers.remove(m_name.toLowerCase());
        m_botAction.sendPrivateMessage(m_name, "You have " + (d_maxLagouts - m_lagouts) + " lagouts remaining.");
        m_lastFoul = System.currentTimeMillis();
        m_botAction.setShip(m_name, m_ship);
        m_botAction.setFreq(m_name, m_freq);
        if (m_team.m_game.m_state == DuelGame.IN_PROGRESS)
            m_team.warp(this);
        else if (m_team.m_game.m_state == DuelGame.SETUP)
            m_team.safe(this);
    }
    
    public void handleReturn() {
        status(RETURN);
        m_botAction.specWithoutLock(m_name);
        m_botAction.setFreq(m_name, m_freq);
        m_botAction.sendPrivateMessage(m_name, "To return to your duel, reply with !lagout");
        status(LAGGED);
    }
    
    public void warpDelay(DuelPlayer p) {
        status(WARPING);
        m_team.safe(this);
        
        spawner = new TimerTask() {
            @Override
            public void run() {
                if (m_status == PLAYING)
                    m_team.warp(DuelPlayer.this);
                else if (m_status == OUT)
                    remove(NORMAL);
            }
        };
        m_botAction.scheduleTask(spawner, d_deathTime * 1000);
        
    }
    
    public void handleDeath(String killerName) {
        if (m_team == null)
            return;
        status(WARPING);
        long now = System.currentTimeMillis();
        DuelPlayer killer = m_bot.m_players.get(killerName.toLowerCase());

        m_team.safe(this);
        // DoubleKill check - remember to add a timer in case its the last death
        if ((killer != null) && (killer.timeFromLastDeath() < 2001) && (m_name.equalsIgnoreCase(killer.getLastKiller()))) {
            m_botAction.sendSmartPrivateMessage(m_name, "Double kill, doesn't count.");
            m_botAction.sendSmartPrivateMessage(killerName, "Double kill, doesn't count.");
            killer.removeDeath();        
        } else if (!m_team.wasTK(m_name, killerName)) {
            if ((now - m_lastDeath) < ((d_spawnTime + d_deathTime) * 1000)) {
                m_botAction.sendPrivateMessage(m_name, "Spawn Kill, doesn't count.");
                killer.spawnKill();
            } else {
                m_deaths++;
                killer.addKill();
            }
        } else if (m_team.wasTK(m_name, killerName)) {
            m_deaths++;
        }

        m_lastDeath = now;
        m_lastKiller = killerName;  
        
        if (m_deaths >= m_specAt) {
            status(OUT);
            dying = new TimerTask() {
                @Override
                public void run() {
                    if (m_status == OUT) {
                        remove(NORMAL);
                        m_botAction.cancelTask(spawner);
                    }
                }
            };
            m_botAction.scheduleTask(dying, 2000);  
        }
        
        spawner = new TimerTask() {
            @Override
            public void run() {
                if (m_status == PLAYING)
                    m_team.spawn(DuelPlayer.this);
                else if (m_status == OUT)
                    remove(NORMAL);
            }
        };
        m_botAction.scheduleTask(spawner, d_deathTime * 1000);
        m_team.m_game.updateScore();
    }
    
    public void spawnKill() {
        if (m_team == null)
            return;
        m_spawns++;
        if (m_spawns < d_spawnLimit)
            m_botAction.sendPrivateMessage(m_name, "Spawn killing is illegal. If you should continue to spawn kill you will forfeit your match.");
        else
            remove(SPAWNS);
    }
    
    public long timeFromLastDeath() {
        return System.currentTimeMillis() - m_lastDeath;
    }
    
    public String getLastKiller() {
        return m_lastKiller;
    }
    
    public void endGame() {
        if (m_ship > 0)
            m_status = SPEC;
        else
            m_status = IN;
        m_team = null;
        m_botAction.cancelTask(lagout);
        m_botAction.cancelTask(spawner);
        m_botAction.cancelTask(dying);
        m_botAction.shipReset(m_name);
        m_botAction.warpTo(m_name, 512, 502);
        m_spawns = 0;
        m_deaths = 0;
        m_kills = 0;
        m_lagouts = 0;
        m_warps = 0;
        m_out = -1;
    }
    
    public void removeDeath() {
        if (m_deaths == m_specAt)
            status(PLAYING);
        if (m_deaths > 0)
            m_deaths--;
    }
    
    public void removeKill() {
        if (m_kills > 0) 
            m_kills--;
    }
    
    public void addKill() {
        m_kills++;
    }
    
    public int getKills() {
        return m_kills;
    }
    
    public int getDeaths() {
        return m_deaths;
    }
    
    public int status() {
        return m_status;
    }
    
    public void status(int s) {
        m_status = s;
    }
    
    public void remove(int reason) {
        m_botAction.specWithoutLock(m_name);
        m_botAction.setFreq(m_name, m_freq);
        if (m_status == REOUT) {
            status(OUT);
            return;
        }
        m_out = reason;
        if (m_deaths != m_specAt)
            m_deaths = m_specAt;
        status(OUT);
        m_team.playerOut(this);
    }
    
    public void warp(int x, int y) {
        status(WARPING);
        Player p1 = m_botAction.getPlayer(m_name);
        m_botAction.shipReset(m_name);
        m_botAction.warpTo(m_name, x, y);
        p1.updatePlayerPositionManuallyAfterWarp(x, y);
        status(PLAYING);
    }
    
    public void warpWarper(int x, int y) {
        status(WARPING);
        Player p1 = m_botAction.getPlayer(m_name);
        m_botAction.warpTo(m_name, x, y);
        p1.updatePlayerPositionManuallyAfterWarp(x, y);
        status(PLAYING);
    }
    
    public void starting(int ship, int x, int y) {
        if (m_status == LAGGED)
            return;
        status(WARPING);
        if (ship > -1) {
            m_botAction.setShip(m_name, m_ship);
        } else if (m_ship == 0) {
            m_botAction.setShip(m_name, 1);
            m_ship = 1;
        }
        m_botAction.setFreq(m_name, m_freq);
        warp(x, y);
    }
    
    public void sql_teamPop() {
        ResultSet rs;
        String query = "SELECT fnUserID AS u, fnTeamID AS id, fnLeagueTypeID AS type FROM tblDuel__2league WHERE fnSeason = " + d_season + " AND fnStatus = 1 AND fnUserID = (SELECT fnUserID FROM tblUser WHERE fcUserName = '" + Tools.addSlashesToString(m_name) + "' ORDER BY fnUserID ASC LIMIT 1)";
        
        try {
            rs = m_botAction.SQLQuery(DB, query);

            if (rs.next()) {
                m_userID = rs.getInt("u");
                do {
                    m_teams[rs.getInt("type")] = rs.getInt("id");                    
                } while (rs.next());
                m_teams[0] = m_userID;
            }

            m_botAction.SQLClose(rs);
        } catch (Exception e) {
            System.out.println("SQLException teamPop for " + m_name);
        }
    }

    public int getReason() {
        return m_out;
    }
    
    public void cancelGame(String name) {
        if (m_team == null || m_team.m_game == null) {
            m_botAction.sendPrivateMessage(name, "No game found.");
            return;
        } else {
            m_team.m_game.cancelGame(name);
        }
    }
    
}
