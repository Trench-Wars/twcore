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
 * DraftTeam handles all things team related. Handles all team captain commands
 * and also maintains relevant team information.  
 *
 * @author WingZero
 */
public class DraftTeam {

    public static final String db = "website";
    public static final int MAXRES_X = 1440;
    public static final int MAXRES_Y = 1024;
    
    BotAction       ba;
    OperatorList    oplist;
    BotSettings     rules;
    GameType        type;
    DraftRound      round;
    String[]        caps;
    String          teamName;
    boolean         ready, flag, resChecks;
    int[]           shipMax, ships;
    int             score, freq, teamID, deaths, usedStars, subs, changes, switches;

    HashMap<String, DraftPlayer>    players;
    HashMap<String, ResCheck>       checks;
    Vector<String>                  lagChecks;
    
    public DraftTeam(DraftRound gameRound, String name, int id, int freqNum) {
        players = new HashMap<String, DraftPlayer>();
        checks = new HashMap<String, ResCheck>();
        lagChecks = new Vector<String>();
        round = gameRound;
        ba = round.ba;
        oplist = round.oplist;
        rules = round.rules;
        type = round.type;
        shipMax = rules.getIntArray("Ships", ",");
        ships = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        caps = new String[3];
        teamName = name;
        teamID = id;
        freq = freqNum;
        usedStars = -1;
        subs = rules.getInt("Subs");
        deaths = rules.getInt("Deaths");
        changes = rules.getInt("Changes");
        switches = rules.getInt("Switches");
        score = 0;
        ready = false;
        flag = false;
        resChecks = true;
        loadTeam();
    }

    /** EVENT HANDLERS **/
    public void handleEvent(FlagClaimed event) {
        if (type != GameType.BASING) return; 
        String name = ba.getPlayerName(event.getPlayerID());
        if (name != null) {
            DraftPlayer dp = getPlayer(name);
            if (dp != null && dp.getStatus() == Status.IN) {
            	flag = true;
            	round.getOpposing(this).handleFlagLoss();
                dp.handleFlagClaim();
            }
        }
    }
    
    public void handleFlagReward(int points) {
        if (type != GameType.BASING) return; 
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
                if (checks.containsKey(low(name))) {
                    String res = msg.substring(msg.indexOf("Res: ") + 4, msg.indexOf("Client:"));
                    checks.remove(low(name)).check(res);
                }
            }
        } else if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
            String name = ba.getPlayerName(event.getPlayerID());
            if (name == null)
                name = event.getMessager();
            if (name == null) return;
            
            if (msg.equals("!list"))
                cmd_list(name);
            else if (msg.equals("!myfreq"))
                cmd_myFreq(name);
            
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
                else if (msg.startsWith("!rem"))
                    cmd_remove(name, msg);
                else if (msg.startsWith("!stars"))
                    cmd_stars(name);
            }
        }
    }
    
    /** Reports a lagged out player to team captains */
    public void handleLagout(String name) {
        msgCaptains(name + " has lagged out or specced.");
    }
    
    /** Sets flag to false representing flag possesion */
    public void handleFlagLoss() {
    	flag = false;
    }
    
    /** Handles the ready command which toggles whether or not a team is finished setting up a lineup */
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
    
    public void cmd_stars(String name) {
        ba.sendPrivateMessage(name, "" + teamName + " star count: " + usedStars);
    }
    
    /** Handles the list command which prints a list of each teams captains and current line ups */
    public void cmd_list(String name) {
        ba.sendSmartPrivateMessage(name, teamName + " captains: " + caps[0] + ", " + caps[1] + ", " + caps[2]);
        for (DraftPlayer p : players.values())
            ba.sendSmartPrivateMessage(name, padString(p.getName(), 25) + ": " + p.getShip() + " - " + p.getStatus().toString());
    }
    
    /** Handles the add command which attempts to add a player into the game */
    public void cmd_add(String cap, String cmd) {
        if (type == GameType.BASING) { 
            if (!cmd.contains(":"))
                return;
            // Try to match completely or partially the name of someone in arena with the given pattern
            String name = cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":"));
            String temp = ba.getFuzzyPlayerName(name);
            if (temp != null)
                name = temp;
            else {
                ba.sendSmartPrivateMessage(cap, name + " was not found in this arena.");
                return;
            }
            // Parse the specified ship number
            int ship;
            try {
                ship = Integer.valueOf(cmd.substring(cmd.indexOf(":") + 1));
            } catch (NumberFormatException e) {
                ba.sendSmartPrivateMessage(cap, "Invalid syntax, please use !add <name>:<ship#>");
                return;
            }
            // Check if ship exists and if ship is available
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
            // Check to see if there is room for another player
            if (ships[8] == round.game.maxPlayers) {
                ba.sendSmartPrivateMessage(cap, "The maximum number ships has already been reached.");
                return;
            }
            if (cmd.length() < 6) return;
            // Try to match completely or partially the name of someone in arena with the given pattern
            String name = cmd.substring(cmd.indexOf(" ") + 1);
            String temp = ba.getFuzzyPlayerName(name);
            if (temp != null)
                name = temp;
            else {
                ba.sendSmartPrivateMessage(cap, name + " was not found in this arena.");
                return;
            }
            // For warbird games, create a new ResCheck, otherwise, add player
            if (type == GameType.WARBIRD) {
                if (resChecks)
                    checks.put(low(name), new ResCheck(name, cap, 1));
                else
                    do_add(cap, name, 1);
            } else
                do_add(cap, name, 2);
        }
    }
    
    /** Handles the sub command */
    public void cmd_sub(String cap, String cmd) {
        if (cmd.length() < 5 || !cmd.contains(":")) return;
        // Check for available subs
        if (subs == 0) {
            ba.sendPrivateMessage(cap, "You have no more substitutions available.");
            return;
        } 
        // Command syntax -> out:in
        // Try to match completely or partially the name of someone in arena with the given pattern
        String[] names = cmd.substring(cmd.indexOf(" ") + 1).split(":");
        String temp = ba.getFuzzyPlayerName(names[1]);
        if (temp != null)
            names[1] = temp;
        else {
            ba.sendSmartPrivateMessage(cap, names[1] + " must be in the arena.");
            return;
        }
        // Get and check player to be subbed
        DraftPlayer out = getPlayer(names[0], false);
        if (out == null) {
            ba.sendSmartPrivateMessage(cap, names[0] + " was not found playing.");
            return;
        } else if (!out.isPlaying()) {
            ba.sendSmartPrivateMessage(cap, out.getName() + " is not in the game.");
            return;
        }
        // Execute
        if (type == GameType.WARBIRD) {
            if (resChecks)
                checks.put(low(names[1]), new ResCheck(names[1], cap, out));
            else
                do_sub(cap, names[1], out);
        } else
            do_sub(cap, names[1], out);
    }
    
    /** Handles the remove command which removes a player but only during lineup state */
    public void cmd_remove(String cap, String cmd) {
        if (round.getState() != RoundState.LINEUPS || !cmd.contains(" ")) return;
        String name = cmd.substring(cmd.indexOf(" ") + 1);
        // Try to match completely or partially the name of someone in arena with the given pattern
        String temp = ba.getFuzzyPlayerName(name);
        if (temp != null)
            name = temp;
        DraftPlayer p = getPlayer(name);
        if (p != null) {
            // Decrement ship counts
            ships[p.getShip() - 1]--;
            ships[8]--;
            p.getOut();
            if (p.getStars() > 0) {
                // If player's stars were greater than 0 then it was player's first time added for the week so reset played
                setPlayed(p.getName(), false);
                usedStars -= p.getStars();
            }
            players.remove(low(name));
        }
    }
    
    /** Handles the change command which changes a player's ship in a basing match */
    public void cmd_change(String cap, String cmd) {
        if (type != GameType.BASING || cmd.length() < 9 || !cmd.contains(":")) return;
        if (changes < 1) {
            ba.sendPrivateMessage(cap, "You have no more changes available.");
            return;
        } 
        String name = cmd.substring(cmd.indexOf(" ") + 1, cmd.indexOf(":"));
        // Try to match completely or partially the name of someone in arena with the given pattern
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
        // Parse ship number specified
        int ship;
        try {
            ship = Integer.valueOf(cmd.substring(cmd.indexOf(":") + 1));
        } catch (NumberFormatException e) {
            ba.sendSmartPrivateMessage(cap, "Invalid syntax, please use !change <name>:<ship#>");
            return;
        }
        // Verify acceptable ship number 
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
        // Adjust ship counts
        ships[p.getShip() - 1]--;
        ships[ship - 1]++;
        p.setShip(ship);
        if (round.getState() == RoundState.PLAYING)
            changes--;
        ba.sendArenaMessage(p.getName() + " has been changed to ship " + ship);
    }
    
    /** Handles the switch command which switches the ships of two players in a basing match */
    public void cmd_switch(String cap, String cmd) {
        if (type != GameType.BASING || cmd.length() < 9 || !cmd.contains(":")) return;
        if (switches < 1) {
            ba.sendPrivateMessage(cap, "You have no more changes available.");
            return;
        }
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
        if (round.getState() == RoundState.PLAYING)
            switches--;
    }
    
    /** Handles the myfreq command which puts a player on the team freq */
    public void cmd_myFreq(String name) {
        Player p = ba.getPlayer(name);
        if (p == null) return;
        if (teamName.equalsIgnoreCase(p.getSquadName()))
            ba.setFreq(name, freq);
        else {
            try {
                ResultSet rs = ba.SQLQuery(db, "SELECT fcName FROM tblDraft__Player WHERE fnTeamID = " + teamID + " AND fcName = '" + Tools.addSlashesToString(name) + "' LIMIT 1");
                if (rs.next()) 
                    ba.setFreq(name, freq);
                ba.SQLClose(rs);
            } catch (SQLException e) {
                Tools.printStackTrace(e);
            }
        }
    }
    
    /** Executes the adding of a player by the add command */
    private void do_add(String cap, String name, int ship) {
        DraftPlayer p = null;
        p = getPlayer(name, false);
        if (p != null && p.getStatus() != Status.NONE) {
            ba.sendSmartPrivateMessage(cap, p.getName() + " is already playing.");
            return;
        }
        
        if (p == null) {
            // Player was not previously added so create new
            int stars = getStars(name);
            // If stars are less than 0 then the player was found on the team's roster
            if (stars > -1) {
                if (usedStars + stars <= 50) {
                    // Add player unless teams star max would be exceeded
                    if (stars > 0) // stars will be 0 if this player's stars were already added in a previous game
                        setPlayed(name, true);
                    usedStars += stars;
                    p = new DraftPlayer(ba, this, name, freq, ship, stars);
                } else {
                    ba.sendSmartPrivateMessage(cap, "There are not enough stars remaining to add '" + name + "'. Used stars: " + usedStars);
                    return;
                }
            } else {
                ba.sendSmartPrivateMessage(cap, name + " was not found on the team roster.");
                return;
            }
        }
        lagChecks.add(low(p.getName()));
        players.put(low(p.getName()), p);
        ships[ship - 1]++;
        ships[8]++;
        p.setSpecAt(deaths);
        p.getIn();
        if (type == GameType.BASING)
            ba.sendArenaMessage(name + " has been added in ship " + ship);
        else
            ba.sendArenaMessage(name + " has been added");
        if (round.getState() == RoundState.LINEUPS)
            round.sendLagRequest(name, "!" + teamID);
        msgCaptains("You have " + (50 - usedStars) + " stars remaining this week.");
    }
    
    /** Executes the substitution of one player for another */ 
    private void do_sub(String cap, String in, DraftPlayer out) {
        DraftPlayer p = null;
        p = getPlayer(in, false);
        if (p != null && p.getStatus() != Status.NONE) {
            ba.sendSmartPrivateMessage(cap, p.getName() + " is already playing.");
            return;
        }
        if (p == null) {
            // If player IN has more stars, then count the IN player's stars and disregard OUT's; if OUT has more then nothing
            int stars = getStars(in);
            if (stars > -1) {
                if (stars > out.getStars()) {
                    if (usedStars + (stars - out.getStars()) > 50) {
                        ba.sendSmartPrivateMessage(cap, "There are not enough stars remaining to sub in '" + in + "'. Used stars: " + usedStars);
                        return;
                    }
                    setPlayed(out.getName(), false);
                    setPlayed(in, true);
                    usedStars = usedStars - out.getStars() + stars;
                } else
                    stars = out.getStars();
                p = new DraftPlayer(ba, this, in, freq, out.getShip(), stars);
            } else {
                ba.sendSmartPrivateMessage(cap, in + " was not found on the team roster.");
                return;
            }
        }
        players.put(low(p.getName()), p);
        lagChecks.remove(low(out.getName()));
        lagChecks.add(low(p.getName()));
        p.getIn(out.getShip());
        if (type != GameType.BASING)
            p.setSpecAt(out.getSpecAt() - out.getDeaths());
        out.getOut();
        out.handleSubbed();
        if (subs != -1)
            subs--;
        ba.sendArenaMessage(out.getName() + " has been substituted by " + p.getName());
        msgCaptains("You have " + (50 - usedStars) + " stars remaining this week.");
    }
    
    /** Helper sets a player's "played" status in the database */
    private void setPlayed(String name, boolean played) {
        try {
            if (played)
                ba.SQLQueryAndClose(db, "UPDATE tblDraft__Player SET fnPlayed = 1 WHERE fcName = '" + Tools.addSlashesToString(name) + "'");
            else
                ba.SQLQueryAndClose(db, "UPDATE tblDraft__Player SET fnPlayed = 0 WHERE fcName = '" + Tools.addSlashesToString(name) + "'");
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }
    
    /** Retrieves a player's star count from the database or 0 meaning played player or -1 meaning not found on roster */
    private int getStars(String name) {
        int stars = -1;
        try {
            ResultSet rs = ba.SQLQuery(db, "SELECT * FROM tblDraft__Player WHERE fnSeason = " + 7 + " AND fcName = '" + Tools.addSlashesToString(name) + "' LIMIT 1");
            if (rs.next()) {
                int team = rs.getInt("fnTeamID");
                if (team == teamID) {
                    stars = rs.getInt("fnStars");
                    if (rs.getInt("fnPlayed") == 1)
                        stars = 0;
                }
            }
            ba.SQLClose(rs);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
        return stars;
    }
    
    /** Toggles res checks */
    public void do_resChecks(boolean res) {
        resChecks = res;
    }
    
    /** Returns true if any players are not yet out */
    public boolean isAlive() {
        for (DraftPlayer p : players.values()) {
            if (p.getStatus() == Status.IN)
                return true;
            else if (p.getStatus() == Status.LAGGED && p.getLastLagout() < 60)
                return true;
        }
        return false;
    }
    
    /** Warps team */
    public void warpTeam(int x, int y) {
        ba.warpFreqToLocation(freq, x, y);
    }
    
    /** Updates database with team star information */
    public void reportEnd() {
        for (DraftPlayer p : players.values())
            p.saveStats();
        ba.SQLBackgroundQuery(db, null, "UPDATE tblDraft__Team SET fnUsedStars = " + usedStars + " WHERE fnTeamID = " + teamID + "");
    }
    
    /** Returns ArrayList of formatted end game stats report for whole team and individual players */
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
            stats.add("| " + padString(teamName, 23) + " /  " + padNum(getTotal(StatType.KILLS), 4) + " | " + padNum(getTotal(StatType.DEATHS), 4) + " | " + padNum(getTotal(StatType.TEAM_KILLS), 4) + " | " + padNum(getTotal(StatType.SCORE), 9) + " | " + padNum(getTotal(StatType.FLAG_CLAIMS), 4) + " | " + padNum(getTotal(StatType.TERR_KILLS), 4) + " | " + padNum((getTotal(StatType.REPELS) / 2), 3) + " | " + padNum(getTotal(StatType.RATING), 9) + " | " + padNum(getLagouts(), 2) + " |");
            stats.add("+------------------------'        |      |      |           |      |      |     |           |    |");
            for (DraftPlayer p : players.values())
                stats.add("|  " + padString(p.getName(), 25) + " " + padNum(p.getStat(StatType.KILLS).getValue(), 4) + " | " + padNum(p.getDeaths(), 4) + " | " + padNum(p.getStat(StatType.TEAM_KILLS).getValue(), 4) + " | " + padNum(p.getScore(), 9) + " | " + padNum(p.getStat(StatType.FLAG_CLAIMS).getValue(), 4) + " | " + padNum(p.getStat(StatType.TERR_KILLS).getValue(), 4) + " | " + p.getRPD() + " | " + padNum(p.getRating(), 9) + " | " + padNum(p.getLagouts(), 2) + " |");
        }
        return stats;
    }
    
    /** Gets total  number of lagouts used */
    public int getLagouts() {
        int num = 0;
        for (DraftPlayer p : players.values())
            num += p.getLagouts();
        return num;
    }
    
    /** Removes next player to lag check and then add to back of queue */ 
    public String getNextPlayer() {
        if (lagChecks.isEmpty())
            return null;
        String name = lagChecks.remove(0);
        lagChecks.add(name);
        return name;
    }
    
    /** Returns ready status */
    public boolean getReady() {
        return ready;
    }
    
    /** Returns the sum of a particular stat across each team player */
    public int getTotal(StatType stat) {
        int result = 0;
        for (DraftPlayer p : players.values())
            result += p.getStat(stat).getValue();
        return result;
    }
    
    /** Returns basing second score count or dueling enemy death count */
    public int getScore() {
        if (type == GameType.BASING)
            return score;
        else
            return round.getOpposing(this).getDeaths() + (deaths * (5 - round.getOpposing(this).getPlayerCount()));
    }
    
    /** Get number of players currently playing */
    public int getPlayerCount() {
        int count = 0;
        for (DraftPlayer p : players.values()) {
            if (p.getStatus() == Status.IN || p.getStatus() == Status.OUT || (p.getStatus() == Status.LAGGED && p.getLastLagout() < 60))
                count++;
        }
        return count;
    }
    
    /** Returns String representation of team time score in minutes and second */
    public String getTime() {
        return formatTime(score);
    }
    
    /** Returns team freq */
    public int getFreq() {
        return freq;
    }
    
    /** Returns total deaths for whole team */
    public int getDeaths() {
        int deaths = 0;
        for (DraftPlayer p : players.values())
            deaths += p.getDeaths();
        return deaths;
    }
    
    /** Determines team MVP by comparing scores and returns a DraftPlayer Object */
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
    
    /** Returns team name */
    public String getName() {
        return teamName;
    }
    
    /** Returns team ID number */
    public int getID() {
        return teamID;
    }
    
    /** Returns a DraftPlayer Object of the player specified by name or null if not found */
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
    
    /** Returns a DraftPlayer Object of a player with the exact name specified */
    public DraftPlayer getPlayer(String name) {
        return getPlayer(name, true);
    }
    
    /** Returns the opposing team DraftTeam Object */
    public DraftTeam getOpposing() {
    	return round.getOpposing(this);
    }
    
    /** Increments score */
    public void addPoint() {
        score++;
    }
    
    /** Returns size of players collection */
    public int getSize() {
        return players.size();
    }
    
    /** Returns true if team has possession of flag */
    public boolean hasFlag() {
        return flag;
    }
    
    /** Returns true if a player with the name specified is currently playing */
    public boolean isPlaying(String name) {
        DraftPlayer p = getPlayer(name, false);
        if (p != null) {
            if (p.getStatus() == Status.IN || p.getStatus() == Status.LAGGED)
                return true;
        }
        return false;
    }
    
    /** Checks if name is a team captain */
    public boolean isCaptain(String name) {
        for (String cap : caps)
            if (name.equalsIgnoreCase(cap))
                return true;
        return false;
    }
    
    /** Sends an array of private messages to the team captains */
    public void spamCaptains(String[] msg) {
        if (msg[0].contains("PING") && round.getState() != RoundState.LINEUPS) return;
        for (String name : caps)
            ba.privateMessageSpam(name, msg);
    }
    
    /** Sends a private message to the team captains */
    public void msgCaptains(String msg) {
        for (String name : caps)
            ba.sendPrivateMessage(name, msg);
    }
    
    /** Reloads current captain and assistant names from the database */
    public void reloadCaps() {
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
    
    /** Loads team information from the database */
    private void loadTeam() {
        String query = "SELECT * FROM tblDraft__Team WHERE fnTeamID = " + teamID + " LIMIT 1";
        try {
            ResultSet rs = ba.SQLQuery(db, query);
            if (rs.next()) {
                caps[0] = rs.getString("fcCap");
                caps[1] = rs.getString("fcAss1");
                caps[2] = rs.getString("fcAss2");
                usedStars = rs.getInt("fnUsedStars");
            }
            ba.SQLClose(rs);
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
    }
    
    /** Adds spaces to the end of a String to meet a certain length */
    private String padString(String str, int length) {
        while (str.length() < length)
            str += " ";
        return str;
    }
    
    /** Adds spaces in front of a number to create a String of a certain length */
    private String padNum(int num, int length) {
        String str = "" + num;
        while (str.length() < length)
            str = " " + str;
        return str;
    }
    
    /** Converts time in seconds to time in minutes and seconds MM:ss */
    private String formatTime(int score) {
    	String leadingZero = "";
    	if (score % 60 < 10)
    		leadingZero = "0";
    	return "" + (score / 60) + ":" + leadingZero + (score % 60);
    }
    
    /** Lazy helper returns lower case String */
    private String low(String str) {
        return str.toLowerCase();
    }
    
    /**
     * ResCheck is used to get the resolution a player is using via *einfo and then verifies
     * that the player's resolution complies with the league standard. If it does, then the player
     * is added or subbed as usual. Otherwise, the player and captains are warned about the illegal resolution.
     *
     * @author WingZero
     */
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
