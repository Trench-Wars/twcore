package twcore.bots.duel2bot;

import twcore.bots.duel2bot.DuelPlayer;
import twcore.bots.duel2bot.DuelTeam;
import twcore.bots.duel2bot.duel2bot;
import twcore.core.BotAction;
import twcore.core.BotSettings;

public class DuelGame {

    boolean record;
    
    BotAction m_botAction;
    BotSettings m_rules;
    String db = "website";
    
    DuelTeam m_team1;
    DuelTeam m_team2;
    
    duel2bot m_bot;
    DuelBox m_box;
    
    int m_id;
    int m_type;
    int m_state;
    int[] m_score;
    
    static int d_noCount;
    static int d_season;
    static int d_deathTime;
    static int d_spawnTime;
    static int d_spawnLimit;
    static int d_maxLagouts;
    static int d_toWin;
    
    // constant game states
    static final int SETUP = 0;
    static final int IN_PROGRESS = 1;
    static final int ENDING = 3;
    
    public DuelGame(DuelBox box, String[] team1, String[] team2, int freq1, int freq2, int type, BotAction botaction, duel2bot bot) {
        m_box = box;
        m_type = type;
        m_botAction = botaction;
        m_bot = bot;
        m_state = SETUP;
        d_season = m_bot.d_season;
        d_noCount = m_bot.d_noCount;
        d_deathTime = m_bot.d_deathTime;
        d_spawnTime = m_bot.d_spawnTime;
        d_spawnLimit = m_bot.d_spawnLimit;
        d_maxLagouts = m_bot.d_maxLagouts;
        m_id = m_bot.nlid();
        
        // safeAx1,safeAy1,Ax1,Ay1,safeAx2,safeAy2,Ax2,Ay2
        int[] coords1 = new int[] { m_box.getAX1(), m_box.getAY1(), m_box.getSafeAX1(), m_box.getSafeAY1(), 
                                    m_box.getAX2(), m_box.getAY2(), m_box.getSafeAX2(), m_box.getSafeAY2()};
        int[] coords2 = new int[] { m_box.getBX1(), m_box.getBY1(), m_box.getSafeBX1(), m_box.getSafeBY1(), 
                                    m_box.getBX2(), m_box.getBY2(), m_box.getSafeBX2(), m_box.getSafeBY2()};
        
        // since team wasn't given, add to main collection
        m_team1 = new DuelTeam(m_bot, freq1, freq1, team1, coords1, this );
        m_bot.m_teams.put(m_team2.getTeamID(), m_team1);
        m_team2 = new DuelTeam(m_bot, freq2, freq2, team2, coords2, this );
        m_bot.m_teams.put(m_team2.getTeamID(), m_team2);
        m_bot.m_games.put(m_id, this);
    }
    
    public DuelGame(DuelBox box, Scrim scrim, BotAction botaction, duel2bot bot) {
        m_box = box;
        m_type = scrim.type();
        m_botAction = botaction;
        m_bot = bot;
        m_state = SETUP;
        d_season = m_bot.d_season;
        d_noCount = m_bot.d_noCount;
        d_deathTime = m_bot.d_deathTime;
        d_spawnTime = m_bot.d_spawnTime;
        d_spawnLimit = m_bot.d_spawnLimit;
        d_maxLagouts = m_bot.d_maxLagouts;
        m_id = m_bot.nlid();
        
        // Ax1,Ay1,safeAx1,safeAy1,Ax2,Ay2,safeAx2,safeAy2
        int[] coords1 = new int[] { m_box.getAX1(), m_box.getAY1(), m_box.getSafeAX1(), m_box.getSafeAY1(), 
                                    m_box.getAX2(), m_box.getAY2(), m_box.getSafeAX2(), m_box.getSafeAY2()};
        int[] coords2 = new int[] { m_box.getBX1(), m_box.getBY1(), m_box.getSafeBX1(), m_box.getSafeBY1(), 
                                    m_box.getBX2(), m_box.getBY2(), m_box.getSafeBX2(), m_box.getSafeBY2()};
        
        // since team wasn't given, add to main collection
        m_team1 = new DuelTeam(m_bot, scrim.freq1(), scrim.freq1(), scrim.team1(), coords1, this );
        m_bot.m_teams.put(m_team1.getTeamID(), m_team1);
        m_team2 = new DuelTeam(m_bot, scrim.freq2(), scrim.freq2(), scrim.team2(), coords2, this );
        m_bot.m_teams.put(m_team2.getTeamID(), m_team2);
        m_bot.m_games.put(m_id, this);
    }
    
    public void updateScore() {
        int team2 = m_team1.getDeaths();
        int team1 = m_team2.getDeaths();
        m_team1.setScore(team1);
        m_team2.setScore(team2);
        m_score = new int[] { team1, team2 };
        
        if (m_team1.out() || m_team2.out()) {
            endGame(team1, team2);
            return;
        }
        
        m_botAction.sendOpposingTeamMessageByFrequency(m_team1.getFreq(), "Score: " + m_score[0] + "-" + m_score[1], 26);
        m_botAction.sendOpposingTeamMessageByFrequency(m_team2.getFreq(), "Score: " + m_score[0] + "-" + m_score[1], 26);
    }
    
    public int getState() {
        return m_state;
    }
    
    public String getScore() {
        String[] t1 = m_team1.getNames();
        String[] t2 = m_team2.getNames();
        return "" + m_score[0] + "-" + m_score[1] + " : " + t1[0] + " and " + t1[1] + " vs " + t2[0] + " and " + t2[1];
    }
    
    public void startGame() {
        if (m_team1 == null || m_team2 == null)
            return;
        
        m_state = SETUP;
        String[] names1, names2;
        names1 = m_team1.getNames();
        m_bot.m_playing.put(names1[0].toLowerCase(), m_id);
        m_bot.m_playing.put(names1[1].toLowerCase(), m_id);
        names2 = m_team2.getNames();
        m_bot.m_playing.put(names2[0].toLowerCase(), m_id);
        m_bot.m_playing.put(names2[1].toLowerCase(), m_id);

        m_score = new int[] { 0, 0 };
        if (m_type == 5) {
            m_team1.startGame(true, m_team2.getNames());
            m_team2.startGame(true, m_team1.getNames());
        } else {
            m_team1.startGame(false, m_team2.getNames());
            m_team2.startGame(false, m_team1.getNames());            
        }
        m_botAction.sendTeamMessage("A " + getDivision() + " duel is starting: '" + names1[0] + "' and '" + names1[1] + "' VS '" + names2[0] + "' and '" + names2[1] + "'");
    }
    
    public void endGame(int team1, int team2) {
        m_state = ENDING;
        String[] winner, loser;
        int winnerScore, loserScore;
        if (team1 > team2) {
            winner = m_team1.getNames();
            loser = m_team2.getNames();
            winnerScore = team1;
            loserScore = team2;
        } else {
            winner = m_team2.getNames();
            loser = m_team1.getNames();
            winnerScore = team2;
            loserScore = team1;            
        }
        
        m_botAction.sendPrivateMessage(winner[0], "You and '" + winner[1] + "' have defeated '" + loser[0] + "' and '" + loser[1] + "' score: (" + winnerScore + "-" + loserScore + ")");
        m_botAction.sendPrivateMessage(winner[1], "You and '" + winner[0] + "' have defeated '" + loser[0] + "' and '" + loser[1] + "' score: (" + winnerScore + "-" + loserScore + ")");
        m_botAction.sendPrivateMessage(loser[0], "You and '" + loser[1] + "' have been defeated by '" + winner[0] + "' and '" + winner[1] + "' score: (" + loserScore + "-" + winnerScore + ")");
        m_botAction.sendPrivateMessage(loser[1], "You and '" + loser[0] + "' have been defeated by '" + winner[0] + "' and '" + winner[1] + "' score: (" + loserScore + "-" + winnerScore + ")");
        m_botAction.sendTeamMessage("'" + winner[0] + " and '" + winner[1] + "' defeat '" + loser[0] + "' and '" + loser[1] + "' in " + getDivision() + " score: (" + winnerScore + "-" + loserScore + ")", 21);
        
        team1 = m_team1.getTeamID();
        team2 = m_team2.getTeamID();
        
        m_team1.endGame();
        m_team2.endGame();
        
        if (!record) {
            m_bot.m_teams.remove(team1);
            m_bot.m_teams.remove(team2);
        }
        
        m_bot.m_games.remove(m_id);
        String[] names;
        names = m_team1.getNames();
        m_bot.m_playing.remove(names[0].toLowerCase());
        m_bot.m_playing.remove(names[1].toLowerCase());
        names = m_team2.getNames();
        m_bot.m_playing.remove(names[0].toLowerCase());
        m_bot.m_playing.remove(names[1].toLowerCase());
    }
    
    public void cancelGame(String name) {
        m_team1.endGame();
        m_team2.endGame();
        if (!record) {
            m_bot.m_teams.remove(m_team1.getTeamID());
            m_bot.m_teams.remove(m_team2.getTeamID());
        }
        
        m_bot.m_games.remove(m_id);
        m_botAction.sendPrivateMessage(name, "Game cancelled.");
    }
    
    public void lagout(int id) {
        if (m_team1.getTeamID() == id)
            m_team2.opLagout();
        else
            m_team1.opLagout();
    }
    
    public void returned(int id) {
        if (m_team1.getTeamID() == id)
            m_team2.opReturned();
        else
            m_team1.opReturned();
    }
    
    public String getDivision() {
        if (m_type == 1)
            return "Warbird";
        else if (m_type == 2)
            return "Javelin";
        else if (m_type == 3)
            return "Spider";
        else if (m_type == 4 || m_type == 7)
            return "Lancaster";
        else if (m_type == 5)
            return "Mixed";
        else
            return "Unknown";
    }
    
    // handle player position
        // call Player to warp
    
    public void playerOut(DuelPlayer player) {
        int why = player.getReason();
        if (why == DuelPlayer.NORMAL) { 
            m_botAction.sendOpposingTeamMessageByFrequency(m_team1.getFreq(), "'" + player.getName() + "' is out with " + player.getKills() + ":" + player.getDeaths(), 26);
            m_botAction.sendOpposingTeamMessageByFrequency(m_team2.getFreq(), "'" + player.getName() + "' is out with " + player.getKills() + ":" + player.getDeaths(), 26);
        } else if (why == DuelPlayer.WARPS) {
            m_botAction.sendOpposingTeamMessageByFrequency(m_team1.getFreq(), "'" + player.getName() + "' is out due to warp abuse", 26);
            m_botAction.sendOpposingTeamMessageByFrequency(m_team2.getFreq(), "'" + player.getName() + "' is out due to warp abuse", 26); 
        } else if (why == DuelPlayer.LAGOUTS) {
            m_botAction.sendOpposingTeamMessageByFrequency(m_team1.getFreq(), "'" + player.getName() + "' is out due to lagouts", 26);
            m_botAction.sendOpposingTeamMessageByFrequency(m_team2.getFreq(), "'" + player.getName() + "' is out due to lagouts", 26); 
        } else if (why == DuelPlayer.SPAWNS) {
            m_botAction.sendOpposingTeamMessageByFrequency(m_team1.getFreq(), "'" + player.getName() + "' is out due to spawn kill abuse", 26);
            m_botAction.sendOpposingTeamMessageByFrequency(m_team2.getFreq(), "'" + player.getName() + "' is out due to spawn kill abuse", 26); 
        }
        updateScore();
    }
}
