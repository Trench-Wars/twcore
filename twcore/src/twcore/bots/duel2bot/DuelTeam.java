package twcore.bots.duel2bot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.BotSettings;

public class DuelTeam {

    BotAction               ba;
    BotSettings             rules;

    static final String db = "website";

    int                     teamID;
    int                     freq;
    int                     score;
    int                     div;
    int                     ship;

    private int[] safe1, safe2, spawn1, spawn2;

    // status constants
    public static final int SOLO = 0;
    public static final int HERE = 1;
    public static final int GAME = 2;

    int                     status;

    boolean                 out;

    boolean                 ranked;

    TimerTask               go;
    String[]                pname;
    int[]                   userID;
    DuelPlayer[]            player;
    DuelGame                game;
    duel2bot                bot;

    public DuelTeam(duel2bot bot, int id, int freq, String[] names, int[] coords, DuelGame game) {
        ranked = game.ranked;
        this.bot = bot;
        this.game = game;
        ba = bot.ba;
        rules = game.rules;
        div = game.type;
        this.freq = freq;

        // BACKTRACK
        bot.freqs.addElement(freq);
        player = new DuelPlayer[2];
        out = false;
        score = 0;
        pname = names;
        teamID = id;
        status = HERE;

        // ships
        if (div == 1)
            ship = 1;
        else if (div == 2)
            ship = 2;
        else if (div == 3)
            ship = 3;
        else if (div == 4)
            ship = 7;
        else if (div == 5) ship = -1;

        // safeAx1,safeAy1,Ax1,Ay1,safeAx2,safeAy2,Ax2,Ay2
        safe1 = new int[] { coords[2], coords[3] };
        spawn1 = new int[] { coords[0], coords[1] };
        safe2 = new int[] { coords[6], coords[7] };
        spawn2 = new int[] { coords[4], coords[5] };

        // BACKTRACK
        player[0] = bot.players.get(names[0].toLowerCase());
        player[1] = bot.players.get(names[1].toLowerCase());
        player[0].setTeam(this);
        player[1].setTeam(this);
    }

    public void setScore(int s) {
        score = s;
    }

    public int getDeaths() {
        return player[0].getDeaths() + player[1].getDeaths();
    }

    public int getTeamID() {
        return teamID;
    }

    public int getFreq() {
        return freq;
    }

    public String[] getNames() {
        return pname;
    }

    public int getShip(String name) {
        if (pname[0].equalsIgnoreCase(name))
            return player[0].ship;
        else if (pname[1].equalsIgnoreCase(name))
            return player[1].ship;
        else
            return -1;
    }

    public boolean wasTK(String killee, String killer) {
        if (killer.equalsIgnoreCase(getPartner(killee)))
            return true;
        else
            return false;
    }

    public void playerOut(DuelPlayer p) {
        if ((player[0].getStatus() == DuelPlayer.OUT) && (player[1].getStatus() == DuelPlayer.OUT))
            out = true;
        // BACKTRACK
        game.playerOut(p);
    }

    public boolean out() {
        return out;
    }

    public void partnerLagout(String name) {
        ba.sendPrivateMessage(getPartner(name),
                "Your partner has lagged out or specced, and has 1 minute to return or will forefeit.");
        // BACKTRACK
        game.lagout(teamID);
    }

    public void opponentLagout() {
        ba.sendPrivateMessage(pname[0],
                "Your opponent has lagged out or specced, and has 1 minute to return or will forfeit.");
        ba.sendPrivateMessage(pname[1],
                "Your opponent has lagged out or specced, and has 1 minute to return or will forfeit.");
    }

    public void partnerReturned(String name) {
        ba.sendPrivateMessage(getPartner(name), "Your partner has returned from being lagged out.");
        // BACKTRACK
        game.returned(teamID);
    }

    public void opponentReturned() {
        ba.sendPrivateMessage(pname[0], "Your opponent has returned from being lagged out.");
        ba.sendPrivateMessage(pname[1], "Your opponent has returned from being lagged out.");
    }

    public String getPartner(String name) {
        if (name.equalsIgnoreCase(pname[0]))
            return pname[1];
        else
            return pname[0];
    }

    public DuelPlayer getPlayer(String name) {
        if (name.equalsIgnoreCase(pname[0]))
            return player[0];
        else if (name.equalsIgnoreCase(pname[1]))
            return player[1];
        else
            return null;
    }

    public void startGame(boolean mixed, String[] nme) {
        status = GAME;
        player[0].starting(ship, safe1[0], safe1[1]);
        player[1].starting(ship, safe2[0], safe2[1]);

        go = new TimerTask() {
            @Override
            public void run() {
                game.state = DuelGame.IN_PROGRESS;
                if (!bot.laggers.containsKey(pname[0].toLowerCase())) {
                    player[0].warp(spawn1[0], spawn1[1]);
                    ba.sendPrivateMessage(pname[0], "GO GO GO!!!", 104);
                }
                if (!bot.laggers.containsKey(pname[1].toLowerCase())) {
                    player[1].warp(spawn2[0], spawn2[1]);
                    ba.sendPrivateMessage(pname[1], "GO GO GO!!!", 104);
                }
            }
        };

        String r = ranked ? "[RANKED] " : "[CASUAL] ";
        if (mixed) {
            ba.sendPrivateMessage(pname[0], r + "Duel Begins in 30 Seconds Against '" + nme[0]
                    + "' and '" + nme[1] + "'", 29);
            ba.sendPrivateMessage(pname[0],
                    "You may change your ship until that time! No ship changes will be allowed after the duel starts.");
            ba.sendPrivateMessage(pname[1], r + "Duel Begins in 30 Seconds Against '" + nme[0]
                    + "' and '" + nme[1] + "'", 29);
            ba.sendPrivateMessage(pname[1],
                    "You may change your ship until that time! No ship changes will be allowed after the duel starts.");
            ba.scheduleTask(go, 30000);
        } else {
            ba.sendPrivateMessage(pname[0], r + "Duel Begins in 15 Seconds Against '" + nme[0]
                    + "' and '" + nme[1] + "'", 27);
            ba.sendPrivateMessage(pname[1], r + "Duel Begins in 15 Seconds Against '" + nme[0]
                    + "' and '" + nme[1] + "'", 27);
            ba.scheduleTask(go, 15000);
        }
    }

    public void endGame() {
        ba.cancelTask(go);
        // BACKTRACK
        bot.freqs.removeElement(freq);
        bot.playing.remove(pname[0].toLowerCase());
        bot.playing.remove(pname[1].toLowerCase());
        freq = 0;
        game = null;
        rules = null;
        score = 0;
        status = HERE;
        ship = 0;
        player[0].endGame();
        player[1].endGame();
    }

    public void spawn(DuelPlayer player) {
        String name = player.getName();
        if (name.equalsIgnoreCase(pname[0]))
            player.warp(spawn1[0], spawn1[1]);
        else
            player.warp(spawn2[0], spawn2[1]);
    }

    public void safe(DuelPlayer player) {
        String name = player.getName();
        if (name.equalsIgnoreCase(pname[0]))
            player.warp(safe1[0], safe1[1]);
        else
            player.warp(safe2[0], safe2[1]);
    }

    public void warpPlayer(DuelPlayer player) {
        WarpPoint wp = game.box.getRandomWarpPoint();
        player.warp(wp.getXCoord(), wp.getYCoord());
    }

    public void warpWarper(DuelPlayer player) {
        WarpPoint wp = game.box.getRandomWarpPoint();
        player.warpWarper(wp.getXCoord(), wp.getYCoord());
    }

    public void warpToSpawns() {
        player[0].warp(spawn1[0], spawn1[1]);
        player[1].warp(spawn2[0], spawn2[1]);
    }

    public void warpToSafes() {
        player[0].warp(safe1[0], safe1[1]);
        player[1].warp(safe2[0], safe2[1]);
    }

    public void sql_setup() {

        ResultSet rs;
        String query = "SELECT fnLeagueTypeID AS type, fnUser1ID AS id1, u1.fcUserName AS p1, fnUser2ID AS id2, u2.fcUserName AS p2 FROM tblDuel__2team JOIN tblUser u1 ON fnUser1ID = u1.fnUserID JOIN tblUser u2 ON fnUser2ID = u2.fnUserID WHERE fnSeason = "
                + bot.d_season + " AND fnStatus = 1 AND fnTeamID = " + teamID + " LIMIT 1";

        try {
            rs = ba.SQLQuery(db, query);

            if (rs.next()) {
                div = rs.getInt("type");
                userID = new int[] { rs.getInt("id1"), rs.getInt("id2") };
                pname = new String[] { rs.getString("p1"), rs.getString("p2") };

                bot.teams.put(teamID, this);
            }

            ba.SQLClose(rs);
        } catch (Exception e) {
            System.out.println("SQLException teamSetup for " + teamID);
        }
    }

    
    public int sql_storeTeam() {
        // TODO: add in functionality for prior team ups
        String query = "INSERT INTO tblDuel2__team (fnMatchID, fnUser1, fnUser2) VALUES(" + userID[0] + ", " + userID[1] + ")";
        try {
            ba.SQLQueryAndClose(db, query);
            ResultSet rs = ba.SQLQuery(db, "SELECT LAST_INSERT_ID()");
            int id = rs.getInt(1);
            ba.SQLClose(rs);
            player[0].sql_storeStats(id);
            player[1].sql_storeStats(id);
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
