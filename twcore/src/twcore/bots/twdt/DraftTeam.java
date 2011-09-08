package twcore.bots.twdt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.events.FlagClaimed;
import twcore.core.events.Message;
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
    public static final int MAXRES_X = 1440;
    public static final int MAXRES_Y = 1024;
    
    BotAction ba;
    OperatorList oplist;
    BotSettings rules;
    GameType type;
    
    DraftRound round;
    HashMap<String, DraftPlayer> players;
    HashMap<String, DraftPlayer> cache; 
    Vector<String> lagChecks;
    int score, freq, teamID, deaths;
    int[] shipMax, ships;
    String[] caps;
    String teamName;
    boolean ready, flag;
    String cmdTarget;
    ResCheck resCheck;
    
    
    public DraftTeam(DraftRound gameRound, String name, int id, int freqNum) {
        players = new HashMap<String, DraftPlayer>();
        cache = new HashMap<String, DraftPlayer>();
        lagChecks = new Vector<String>();
        round = gameRound;
        ba = round.ba;
        oplist = round.oplist;
        rules = round.rules;
        type = round.type;
        shipMax = rules.getIntArray("ships", ",");
        ships = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        caps = new String[3];
        teamName = name;
        teamID = id;
        freq = freqNum;
        deaths = rules.getInt("deaths");
        score = 0;
        ready = false;
        flag = false;
        cmdTarget = null;
        resCheck = null;
        loadTeam();
    }

    public void handleEvent(FlagClaimed event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (name != null) {
            DraftPlayer dp = getPlayer(name);
            if (dp != null && dp.getStatus() == Status.IN) {
            	flag = true;
                dp.handleFlagClaim();
            } else if (dp == null)
            	flag = false;
        }
    }
    
    public void handleFlagReward(int points) {
        for (DraftPlayer p : players.values()) {
            if (p.getStatus() == Status.IN)
                p.handleFlagReward(points);
        }
    }
    
    public void handleEvent(Message event) {
        String msg = event.getMessage();
        for (DraftPlayer p : players.values())
            p.handleEvent(event);
        if (event.getMessageType() == Message.ARENA_MESSAGE) {
            if (msg.contains("Res:")) {
                String name = msg.substring(0, msg.indexOf(":"));
                if (resCheck != null && name.equalsIgnoreCase(resCheck.name)) {
                    String res = msg.substring(msg.indexOf("Res: ") + 4, msg.indexOf("Client:"));
                    resCheck.check(res);
                    resCheck = null;
                }
            }
        } else if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
            String name = ba.getPlayerName(event.getPlayerID());
            if (name == null)
                name = event.getMessager();
            if (name == null) return;
            
            if (msg.equals("!list"))
                cmd_list(name);
            if (isCaptain(name)) {
                if (msg.equals("!ready"))
                    cmd_ready(name);
                else if (msg.startsWith("!add "))
                    cmd_add(name, msg);
                else if (msg.startsWith("!sub "))
                    cmd_sub(name, msg);
                else if (msg.startsWith("!change "))
                    cmd_change(name, msg);
                else if (msg.startsWith("!switch "))
                    cmd_switch(name, msg);
            }
        }
    }
    
    public void handleLagout(String name) {
        msgCaptains(name + " has lagged out or specced.");
    }
    
    public void cmd_ready(String cap) {
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
    
    public void cmd_list(String name) {
        ba.sendSmartPrivateMessage(name, teamName + " captains: " + caps[0] + ", " + caps[1] + ", " + caps[2]);
        for (DraftPlayer p : players.values()) {
            ba.sendSmartPrivateMessage(name, p.getName() + ": " + p.getShip());
        }
    }

    public void cmd_add(String cap, String cmd) {
        if (type == GameType.BASING) { 
            if (!cmd.contains(":"))
                return;
            String name = cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":"));
            String temp = ba.getFuzzyPlayerName(name);
            if (temp != null)
                name = temp;
            else {
                ba.sendSmartPrivateMessage(cap, name + " was not found in this arena.");
                return;
            }
            int ship;
            try {
                ship = Integer.valueOf(cmd.substring(cmd.indexOf(":") + 1));
            } catch (NumberFormatException e) {
                ba.sendSmartPrivateMessage(cap, "Invalid syntax, please use !add <name>:<ship#>");
                return;
            }

            if (ship < 1 || ship > 8 || ship == 4 || ship == 6) {
                ba.sendSmartPrivateMessage(cap, "Invalid ship: " + ship);
                return;
            } else if (ships[ship - 1] >= shipMax[ship - 1]) {
                ba.sendSmartPrivateMessage(cap, "The maximum number of that ship type has already been reached.");
                return;
            } else if (ships[8] == shipMax[8]) {
                ba.sendSmartPrivateMessage(cap, "The maximum number ships has already been reached.");
                return;
            }
            do_add(cap, name, ship);
        } else {
            if (ships[8] == round.game.maxPlayers) {
                ba.sendSmartPrivateMessage(cap, "The maximum number ships has already been reached.");
                return;
            }
            if (cmd.length() < 6) return;
            String name = cmd.substring(cmd.indexOf(" ") + 1);
            String temp = ba.getFuzzyPlayerName(name);
            if (temp != null)
                name = temp;
            else {
                ba.sendSmartPrivateMessage(cap, name + " was not found in this arena.");
                return;
            }
            if (type == GameType.WARBIRD) {
                if (resCheck == null)
                    resCheck = new ResCheck(name, cap, 1);
                else
                    ba.sendSmartPrivateMessage(cap, "A resolution check is still being processed for: " + resCheck.name);
            } else
                do_add(cap, name, 2);
        }
    }
    
    private void do_add(String cap, String name, int ship) {
        System.out.println(cap + " " + name + " " + ship);
        DraftPlayer p = null;
        p = getPlayer(name, false);
        if (p != null && p.getStatus() != Status.NONE) {
            ba.sendSmartPrivateMessage(cap, p.getName() + " is already playing.");
            return;
        }
        if (p == null && cache.containsKey(low(name)))
            p = cache.get(low(name));
        if (p == null) {
            int stars = getStars(name);
            if (stars > -1) {
                p = new DraftPlayer(ba, this, name, freq, ship, stars);
            } else {
                ba.sendSmartPrivateMessage(cap, name + " was not found on the team roster.");
                return;
            }
        }
        if (checkStars(p)) {
            lagChecks.add(low(p.getName()));
            players.put(low(p.getName()), p);
            ships[ship - 1]++;
            p.setSpecAt(deaths);
            p.getIn();
            if (type == GameType.BASING)
                ba.sendArenaMessage(name + " has been added in ship " + ship);
            else
                ba.sendArenaMessage(name + " has been added");
            if (round.getState() == RoundState.LINEUPS)
                round.sendLagRequest(name, "!" + teamID);
        }
    }
    
    private int getStars(String name) {
        int stars = -1;
        try {
            ResultSet rs = ba.SQLQuery(db, "SELECT * FROM tblDraft__Player WHERE fnSeason = " + 1 + " AND fcName = '" + name + "' LIMIT 1");
            if (rs.next()) {
                int team = rs.getInt("fnTeamID");
                if (team == teamID)
                    stars = rs.getInt("fnStars");
            }
            ba.SQLClose(rs);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
        return stars;
    }
    
    public void cmd_sub(String cap, String cmd) {
        if (cmd.length() < 5 || !cmd.contains(":")) return;
        // out:in
        String[] names = cmd.substring(cmd.indexOf(" ") + 1).split(":");
        String temp = ba.getFuzzyPlayerName(names[1]);
        if (temp != null)
            names[1] = temp;
        else {
            ba.sendSmartPrivateMessage(cap, names[1] + " must be in the arena.");
            return;
        }
        DraftPlayer out = getPlayer(names[0], false);
        if (out == null) {
            ba.sendSmartPrivateMessage(cap, names[0] + " was not found playing.");
            return;
        } else if (!out.isPlaying()) {
            ba.sendSmartPrivateMessage(cap, out.getName() + " is not in the game.");
            return;
        }
        if (type == GameType.WARBIRD) {
            if (resCheck != null) {
                ba.sendSmartPrivateMessage(cap, "A resolution check is in process for: " + resCheck.name);
            } else
                resCheck = new ResCheck(names[1], cap, out);
        } else
            do_sub(cap, names[1], out);
    }
    
    private void do_sub(String cap, String in, DraftPlayer out) {
        DraftPlayer p = null;
        p = getPlayer(in, false);
        if (p != null && p.getStatus() != Status.NONE) {
            ba.sendSmartPrivateMessage(cap, p.getName() + " is already playing.");
            return;
        }
        if (p == null && cache.containsKey(low(in)))
            p = cache.get(low(in));
        if (p == null) {
            int stars = getStars(in);
            if (stars > -1) {
                p = new DraftPlayer(ba, this, in, freq, out.getShip(), stars);
            } else {
                ba.sendSmartPrivateMessage(cap, in + " was not found on the team roster.");
                return;
            }
        }
        if (checkStars(p)) {
            players.put(low(p.getName()), p);
            lagChecks.remove(low(out.getName()));
            lagChecks.add(low(p.getName()));
            p.getIn(out.getShip());
            out.getOut();
            out.handleSubbed();
            ba.sendArenaMessage(out.getName() + " has been substitutded by " + p.getName());
        }
    }
    
    public void cmd_change(String cap, String cmd) {
        if (type != GameType.BASING || cmd.length() < 9 || !cmd.contains(":")) return;
        String name = cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":"));
        String temp = ba.getFuzzyPlayerName(name);
        if (temp != null)
            name = temp;
        else {
            ba.sendSmartPrivateMessage(cap, name + " was not found in this arena.");
            return;
        }
        DraftPlayer p = getPlayer(name);
        if (p == null || !p.isPlaying()) {
            ba.sendSmartPrivateMessage(cap, name + " is not in the game.");
            return;
        }
        
        int ship;
        try {
            ship = Integer.valueOf(cmd.substring(cmd.indexOf(":") + 1));
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(cap, "Invalid syntax, please use !change <name>:<ship#>");
            return;
        }

        if (ship < 1 || ship > 8 || ship == 4 || ship == 6) {
            ba.sendSmartPrivateMessage(cap, "Invalid ship: " + ship);
            return;
        } else if (ships[ship - 1] >= shipMax[ship - 1]) {
            ba.sendSmartPrivateMessage(cap, "The maximum number of that ship type has already been reached.");
            return;
        } else if (p.getShip() == ship) {
            ba.sendSmartPrivateMessage(cap, name + " is already in that ship.");
            return;
        }
        ships[p.getShip() - 1]--;
        ships[ship - 1]++;
        p.setShip(ship);
        ba.sendArenaMessage(p.getName() + " has been changed to ship " + ship);
    }
    
    public void cmd_switch(String cap, String cmd) {
        if (type != GameType.BASING || cmd.length() < 9 || !cmd.contains(":")) return;
        String[] names = cmd.substring(cmd.indexOf(" ") + 1).split(":");
        if (!isPlaying(names[0])) {
            ba.sendSmartPrivateMessage(cap, names[0] + " is not in the game.");
            return;
        }
        if (!isPlaying(names[1])) {
            ba.sendSmartPrivateMessage(cap, names[1] + " is not in the game.");
            return;
        }
        DraftPlayer p1 = getPlayer(names[0], false);
        DraftPlayer p2 = getPlayer(names[1], false);
        if (p1.getShip() == p2.getShip()) {
            ba.sendSmartPrivateMessage(cap, p1.getName() + " and " + p2.getName() + " are in the same ship.");
            return;
        }
        ba.sendArenaMessage(p1.getName() + "(" + p1.getShip() + ") and " + p2.getName() + "(" + p2.getShip() + ") have switched ships.");
        int ship = p1.getShip();
        p1.setShip(p2.getShip());
        p2.setShip(ship);
    }
    
    public void cmd_myFreq(String name) {
        Player p = ba.getPlayer(name);
        if (p != null && name.equalsIgnoreCase(p.getSquadName()))
            ba.setFreq(name, freq);
    }
    
    public void cmd_lagout(String name, String cmd) {
        
    }
    
    public boolean checkStars(DraftPlayer name) {
        return true;
    }
    
    public void checkLineup() {
        
    }
    
    public void warpTeam(int x, int y) {
        ba.warpFreqToLocation(freq, x, y);
    }
    
    public void reportStart() {
        for (DraftPlayer p : players.values()) {
            if (p.getStatus() == Status.IN)
                p.reportStart();
        }
    }
    
    public void reportEnd() {
        for (DraftPlayer p : players.values()) {
            if (p.getStatus() == Status.IN)
                p.reportEnd();
        }
    }
    
    public ArrayList<String> getStats() {
        ArrayList<String> stats = new ArrayList<String>();
        if (type == GameType.WARBIRD) {
            stats.add("|                          ,------+------+-----------+----+");
            stats.add("| " + padString(teamName, 23) + " /  " + padNum(getTotal(StatType.KILLS), 4) + " | " + padNum(getTotal(StatType.DEATHS), 4) + " | " + padNum(getTotal(StatType.SCORE), 9) + " | " + padNum(getLagouts(), 2) + " |");
            stats.add("+------------------------'        |      |           |    |");
            for (DraftPlayer p : players.values())
                stats.add("|  " + padString(p.getName(), 25) + " " + padNum(p.getStat(StatType.KILLS).getValue(), 4) + " | " + padNum(p.getDeaths(), 4) + " | " + padNum(p.getScore(), 9) + " | " + padNum(p.getLagouts(), 2) + " |");
        } else if (type == GameType.JAVELIN) {
            stats.add("|                          ,------+------+------+-----------+----+");
            stats.add("| " + padString(teamName, 23) + " /  " + padNum(getTotal(StatType.KILLS), 4) + " | " + padNum(getTotal(StatType.DEATHS), 4) + " | " + padNum(getTotal(StatType.TEAM_KILLS), 4) + " | " + padNum(getTotal(StatType.SCORE), 9) + " | " + padNum(getLagouts(), 2) + " |");
            stats.add("+------------------------'        |      |      |           |    |");
            for (DraftPlayer p : players.values())
                stats.add("|  " + padString(p.getName(), 25) + " " + padNum(p.getStat(StatType.KILLS).getValue(), 4) + " | " + padNum(p.getDeaths(), 4) + " | " + padNum(p.getStat(StatType.TEAM_KILLS).getValue(), 4) + " | " + padNum(p.getScore(), 9) + " | " + padNum(p.getLagouts(), 2) + " |");
            
        } else {
            stats.add("|                          ,------+------+------+-----------+------+------+-----+-----------+----+");
            stats.add("| " + padString(teamName, 23) + " /  " + padNum(getTotal(StatType.KILLS), 4) + " | " + padNum(getTotal(StatType.DEATHS), 4) + " | " + padNum(getTotal(StatType.TEAM_KILLS), 4) + " | " + padNum(getTotal(StatType.SCORE), 9) + " | " + padNum(getTotal(StatType.FLAG_CLAIMS), 4) + " | " + padNum(getTotal(StatType.TERR_KILLS), 4) + " | " + padNum(getTotal(StatType.REPELS)/2, 3) + " | " + padNum(getTotal(StatType.RATING), 9) + " | " + padNum(getLagouts(), 2) + " |");
            stats.add("+------------------------'        |      |      |           |      |      |     |           |    |");
            for (DraftPlayer p : players.values())
                stats.add("|  " + padString(p.getName(), 25) + " " + padNum(p.getStat(StatType.KILLS).getValue(), 4) + " | " + padNum(p.getDeaths(), 4) + " | " + padNum(p.getStat(StatType.TEAM_KILLS).getValue(), 4) + " | " + padNum(p.getScore(), 9) + " | " + padNum(p.getStat(StatType.FLAG_CLAIMS).getValue(), 4) + " | " + padNum(p.getStat(StatType.TERR_KILLS).getValue(), 4) + " | " + p.getRPD() + " | " + padNum(p.getRating(), 9) + " | " + padNum(p.getLagouts(), 2) + " |");
        }
        return stats;
    }
    
    public int getLagouts() {
        int num = 0;
        for (DraftPlayer p : players.values())
            num += p.getLagouts();
        return num;
    }
    
    public String getNextPlayer() {
        if (lagChecks.isEmpty())
            return null;
        String name = lagChecks.remove(0);
        lagChecks.add(name);
        return name;
    }
    
    public boolean getReady() {
        return ready;
    }
    
    public int getTotal(StatType stat) {
        int result = 0;
        for (DraftPlayer p : players.values())
            result += p.getStat(stat).getValue();
        return result;
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
    
    public int getFreq() {
        return freq;
    }
    
    public int getDeaths() {
        int deaths = 0;
        for (DraftPlayer p : players.values())
            deaths += p.getDeaths();
        return deaths;
    }
    
    public DraftPlayer getMVP() {
        DraftPlayer mvp = null;
        for (DraftPlayer p : players.values()) {
            if (mvp != null) {
                if (type != GameType.BASING) {
                    if (p.getScore() > mvp.getScore())
                        mvp = p;
                } else if (p.getRating() > mvp.getRating())
                    mvp = p;
            } else
                mvp = p;
        }
        return mvp;
    }
    
    public String getName() {
        return teamName;
    }
    
    public int getID() {
        return teamID;
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
    
    public DraftTeam getOpposing() {
    	return round.getOpposing(this);
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
        DraftPlayer p = getPlayer(name, false);
        if (p != null) {
            if (p.getStatus() == Status.IN || p.getStatus() == Status.LAGGED)
                return true;
        }
        return false;
    }
    
    public boolean isCaptain(String name) {
        for (String cap : caps)
            if (name.equalsIgnoreCase(cap))
                return true;
        return false;
    }
    
    public void spamCaptains(String[] msg) {
        if (msg[0].contains("PING") && round.getState() != RoundState.LINEUPS) return;
        for (String name : caps)
            ba.privateMessageSpam(name, msg);
    }
    
    public void msgCaptains(String msg) {
        for (String name : caps)
            ba.sendPrivateMessage(name, msg);
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
    
    private String padString(String str, int length) {
        while (str.length() < length)
            str += " ";
        return str;
    }
    
    private String padNum(int num, int length) {
        String str = "" + num;
        while (str.length() < length)
            str = " " + str;
        return str;
    }
    
    private String low(String str) {
        return str.toLowerCase();
    }
    
    private class ResCheck {
        
        boolean isSub;
        String name, cap;
        DraftPlayer out;
        int ship;
        
        public ResCheck(String name, String cap, int ship) {
            this.name = name;
            this.cap = cap;
            this.ship = ship;
            this.out = null;
            isSub = false;
            ba.sendUnfilteredPrivateMessage(name, "*einfo");
        }
        
        public ResCheck(String name, String cap, DraftPlayer subOut) {
            this.name = name;
            this.cap = cap;
            this.out = subOut;
            this.ship = -1;
            isSub = true;
            ba.sendUnfilteredPrivateMessage(name, "*einfo");
        }
        
        public void check(String res) {
            String[] xy = res.split("x");
            int x = Integer.valueOf(xy[0].trim());
            int y = Integer.valueOf(xy[1].trim());
            if (x <= MAXRES_X && y <= MAXRES_Y) {
                if (!isSub)
                    do_add(cap, name, ship);
                else
                    do_sub(cap, name, out);
            } else {
                ba.sendSmartPrivateMessage(name, "You will not be able to play until your resolution is no greather than " + MAXRES_X  + "x" + MAXRES_Y + ".");
                ba.sendSmartPrivateMessage(cap, name + " has a resolution greather than " + MAXRES_X  + "x" + MAXRES_Y + ".");
            }
        }
        
    }
}
