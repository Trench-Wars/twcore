package twcore.bots.twdt;

import java.util.TimerTask;

import twcore.bots.twdt.DraftGame.GameType;
import twcore.bots.twdt.DraftPlayer.Status;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.events.PlayerDeath;
import twcore.core.events.TurretEvent;
import twcore.core.events.WeaponFired;
import twcore.core.lag.LagReport;
import twcore.core.lvz.Objset;
import twcore.core.util.Tools;

/**
 *
 * @author WingZero
 */
public class DraftRound {
    
    BotAction ba;
    OperatorList opList;
    BotSettings rules;
    
    enum RoundState { NONE, LINEUPS, STARTING, PLAYING, FINISHED }
    
    private MasterControl ticker;
    private Objset objects;
    RoundState state;
    GameType type;
    DraftGame game;
    DraftTeam team1;
    DraftTeam team2;
    int target;
    
    public DraftRound(DraftGame draftGame, GameType gameType, int team1ID, int team2ID, String team1Name, String team2Name) {
        game = draftGame;
        ba = game.ba;
        state = RoundState.LINEUPS;
        type = gameType;
        team1 = new DraftTeam(this, team1Name, team1ID, 0);
        team2 = new DraftTeam(this, team2Name, team2ID, 1);
        if (type == GameType.BASING)
            target = 15 * 60;
        else
            target = 50;
        ticker = new MasterControl();
        ba.scheduleTaskAtFixedRate(ticker, 3000, Tools.TimeInMillis.MINUTE);
    }
    
    public void handleEvent(PlayerDeath event) {
        if (state != RoundState.PLAYING) return; 
        String winner = ba.getPlayerName(event.getKillerID());
        String loser = ba.getPlayerName(event.getKilleeID());
        if (winner != null && loser != null) {
            DraftPlayer win = getPlayer(winner);
            DraftPlayer loss = getPlayer(loser);
            if (win != null && loss != null) {
                loss.handleDeath(win);
                win.handleKill(loss);
            }
        }
    }
    
    public void handleEvent(TurretEvent event) {
        if (state != RoundState.PLAYING || !event.isAttaching()) return;
        String name = ba.getPlayerName(event.getAttacherID());
        if (name != null) {
            DraftPlayer p = getPlayer(name);
            if (p != null)
                p.handleAttach();
        }
    }
    
    public void handleEvent(WeaponFired event) {
        if (state != RoundState.PLAYING) return; 
        String name = ba.getPlayerName(event.getPlayerID());
        if (name != null) {
            DraftPlayer p = team1.getPlayer(name);
            if (p == null)
                team2.getPlayer(name);
            if (p != null)
                p.handleEvent(event);
        }
    }
    
    public void handleLagReport(LagReport report) {
        if (!report.isBotRequest())
            ba.privateMessageSpam(report.getRequester(), report.getLagStats());

        DraftPlayer p = team1.getPlayer(report.getName(), true);
        if (p == null)
            p = team2.getPlayer(report.getName(), true);

        if (report.isOverLimits()) {
            if (!report.isBotRequest())
                ba.sendPrivateMessage(report.getRequester(), report.getLagReport());
            
            if (p != null && ba.getPlayer(report.getName()).getShipType() != 0 && p.getStatus() == Status.IN) {
                ba.sendPrivateMessage(report.getName(), report.getLagReport());
                p.setLagSpec(true);
                ba.spec(report.getName());
                ba.spec(report.getName());
            }
        }
    }
    
    public RoundState getState() {
        return state;
    }
    
    public DraftPlayer getPlayer(String name) {
        DraftPlayer p = team1.getPlayer(name);
        if (p == null)
            team2.getPlayer(name);
        return p;        
    }
    
    private class MasterControl extends TimerTask {
        
        int timer;

        public MasterControl() {
            timer = 0;
        }
        
        @Override
        public void run() {
            switch(state) {
                case NONE: doLineups();
                case LINEUPS: doCheck();
                case STARTING: doStarting();
                case PLAYING: doPlaying();
                case FINISHED: doFinished();
            }
        }
        
        private void doLineups() {
            state = RoundState.LINEUPS;
            int time = rules.getInt("LineupTime");
            timer = time * 60;
            ba.sendArenaMessage("Captains, you will have " + time + " minutes to setup your team correctly");
        }
        
        private void doCheck() {
            timer--;
            if ((timer > 0) && !(team1.getReady() && team2.getReady()))
                return;
            if (team1.getSize() < game.minPlayers || team2.getSize() < game.minPlayers) {
                // END GAME
                return;
            } else if (timer < 1)
                ba.sendArenaMessage("Time is up. Lineups are OKAY!");

            timer = 30;
            state = RoundState.STARTING;
            ba.sendArenaMessage("Both teams are ready, game starts in 30 seconds!", 2);
            // warp teams to safes
            // do other pre game stuff
        }
        
        private void doStarting() {
            timer--;
            if (timer < 1) {
                state = RoundState.PLAYING;
                timer = game.gameTime * 60;
                // GO GO GO
            } else if (timer == 10) {
                // signal 10 seconds
            }
        }
        
        private void doPlaying() {
            timer--;
            if (type == GameType.BASING) { 
                if (team1.hasFlag()) {
                    team1.addPoint();
                    checkTime(team1);
                } else if (team2.hasFlag()) {
                    team2.addPoint();
                    checkTime(team2);
                }
            }
            updateScoreboard();
            if (team1.getScore() >= target || team2.getScore() >= target || timer < 1)
                state = RoundState.FINISHED;
        }
        
        private void doFinished() {
            ba.cancelTask(ticker);
            ticker = null;
            game.handleRound();
        }
        
        private void checkTime(DraftTeam team) {
            int score = team.getScore();
            if (score == (3 * 60))
                ba.sendArenaMessage(team.getName() + " needs 3 minutes of flag time to win");
            else if (score == 60)
                ba.sendArenaMessage(team.getName() + " needs 1 minute of flag time to win");
        }
        
        private void updateScoreboard() {
            // handle objons
            if (objects != null) {
                objects.hideAllObjects();
                timer -= 1;
                String team1Score;
                String team2Score;

                team1Score = "" + team1.getScore();
                team2Score = "" + team2.getScore();

                //If lb display twlb scoreboard
                if (type == GameType.BASING) {
                    int t1s = Integer.parseInt(team1Score);
                    int t2s = Integer.parseInt(team2Score);

                    int team1Minutes = (int) Math.floor(t1s / 60.0);
                    int team2Minutes = (int) Math.floor(t2s / 60.0);
                    int team1Seconds = t1s - team1Minutes * 60;
                    int team2Seconds = t2s - team2Minutes * 60;

                    //Team 1
                    objects.showObject(100 + team1Seconds % 10);
                    objects.showObject(110 + (team1Seconds - team1Seconds % 10) / 10);
                    objects.showObject(130 + team1Minutes % 10);
                    objects.showObject(140 + (team1Minutes - team1Minutes % 10) / 10);

                    //Team 2
                    objects.showObject(200 + team2Seconds % 10);
                    objects.showObject(210 + (team2Seconds - team2Seconds % 10) / 10);
                    objects.showObject(230 + team2Minutes % 10);
                    objects.showObject(240 + (team2Minutes - team2Minutes % 10) / 10);
                } else {
                    //Else display ld lj on normal scoreboard
                    // Note: Scoreboard is not logical, scores for team 1 are object ids 200+, scores for team 2 are object ids 100+
                    //       This is only caused in TWLD and TWLJ arenas as they are using the lvz 'twl.lvz'
                    
                    for (int i = team1Score.length() - 1; i > -1; i--)
                        objects.showObject(Integer.parseInt("" + team1Score.charAt(i)) + 200 + (team1Score.length() - 1 - i) * 10);
                    for (int i = team2Score.length() - 1; i > -1; i--)
                        objects.showObject(Integer.parseInt("" + team2Score.charAt(i)) + 100 + (team2Score.length() - 1 - i) * 10);
                }
                if (timer >= 0) {
                    int seconds = timer % 60;
                    int minutes = (timer - seconds) / 60;
                    objects.showObject(730 + ((minutes - minutes % 10) / 10));
                    objects.showObject(720 + (minutes % 10));
                    objects.showObject(710 + ((seconds - seconds % 10) / 10));
                    objects.showObject(700 + (seconds % 10));
                }
                handleNames(team1.getName(), team2.getName());
                ba.setObjects();
            }
        }

        private void handleNames(String n1, String n2) {
            n1 = n1.toLowerCase();
            n2 = n2.toLowerCase();
            if (n1.equalsIgnoreCase("Freq 1")) {
                n1 = "freq1";
            }
            if (n2.equalsIgnoreCase("Freq 2")) {
                n2 = "freq2";
            }
            int i;
            String s1 = "", s2 = "";

            for (i = 0; i < n1.length(); i++)
                if ((n1.charAt(i) >= '0') && (n1.charAt(i) <= 'z') && (s1.length() < 5))
                    s1 = s1 + n1.charAt(i);

            for (i = 0; i < n2.length(); i++)
                if ((n2.charAt(i) >= '0') && (n2.charAt(i) <= 'z') && (s2.length() < 5))
                    s2 = s2 + n2.charAt(i);

            showString(s1, 0, 30);
            showString(s2, 5, 30);
        }

        private void showString(String new_n, int pos_offs, int alph_offs) {
            int i, t;

            for (i = 0; i < new_n.length(); i++) {
                t = new Integer(Integer.toString(((new_n.getBytes()[i]) - 97) + alph_offs) + Integer.toString(i + pos_offs)).intValue();
                if (t < -89) {
                    t = new Integer(Integer.toString(((new_n.getBytes()[i])) + alph_offs) + Integer.toString(i + pos_offs)).intValue();
                    t -= 220;
                }
                objects.showObject(t);
            }
        }

    }
    
}
