package twcore.bots.multibot.ctf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FlagDropped;
import twcore.core.events.FlagPosition;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.TurretEvent;
import twcore.core.game.Flag;
import twcore.core.game.Player;
import twcore.core.game.Ship;
import twcore.core.util.ModuleEventRequester;

/**
 * Capture the Flag bot to be used for the halo arena.
 * 
 * @author WingZero
 */
public class ctf extends MultiModule {

    //Goal0=251,514 - Goal1=772,514
    static final int[] GOALS = { 251, 514, 772, 514 };
    static final int[] WARPS = { 300, 520, 720, 500 };
    Random rand = new Random();

    BotAction ba;
    
    boolean movingFlag;
    boolean DEBUG;
    String debugger;
    String starter;
    
    SpecTask spec;
    FlagTask[] flag;
    Team[] team;
    
    int toWin;
    
    HashMap<String, FlagPlayer> players;

    @Override
    public void init() {
        ba = m_botAction;
        movingFlag = false;
        DEBUG = true;
        debugger = "WingZero";
        toWin = 5;
        ba.setLowPriorityPacketCap(8);
        
        players = new HashMap<String, FlagPlayer>();
        Iterator<Player> j = ba.getPlayerIterator();
        while (j.hasNext())
            new FlagPlayer(j.next());
        ba.shipResetAll();
        Flag flag1 = null, flag2 = null;
        Iterator<Flag> i = ba.getFlagIterator();
        if (i.hasNext()) flag1 = i.next();
        else {
            ba.sendPublicMessage("CTF module failed to detect flags...");
            throw new NullPointerException();
        }
        if (i.hasNext()) flag2 = i.next();
        else {
            ba.sendPublicMessage("CTF module failed to detect flags...");
            throw new NullPointerException();
        }
        if (i.hasNext()) {
            ba.sendPublicMessage("CTF module failed to detect flags...");
            throw new NullPointerException();
        }
        flag = new FlagTask[] { new FlagTask(flag1, 0), new FlagTask(flag2, 1) };
        ba.scheduleTask(flag[0], 1000, 1000);
        ba.scheduleTask(flag[1], 1000, 1000);
        team = new Team[] { new Team(0, flag[0], GOALS[0], GOALS[1]), new Team(1, flag[1], GOALS[2], GOALS[3]) };
        resetFlags();
        spec = new SpecTask();
        ba.scheduleTask(spec, 1000, 2000);
    }

    @Override
    public void cancel() {
        ba.cancelTasks();
    }

    @Override
    public void requestEvents(ModuleEventRequester req) {
        req.request(this, EventRequester.PLAYER_ENTERED);
        req.request(this, EventRequester.PLAYER_LEFT);
        req.request(this, EventRequester.PLAYER_DEATH);
        req.request(this, EventRequester.PLAYER_POSITION);
        req.request(this, EventRequester.TURRET_EVENT);
        req.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(this, EventRequester.FREQUENCY_CHANGE);
        req.request(this, EventRequester.FLAG_CLAIMED);
        req.request(this, EventRequester.FLAG_POSITION);
        req.request(this, EventRequester.FLAG_DROPPED);
    }
    
    public void handleEvent(PlayerEntered event) {
        Player p = ba.getPlayer(event.getPlayerID());
        if (!players.containsKey(low(p.getPlayerName()))) {
            players.put(low(p.getPlayerName()), new FlagPlayer(p.getPlayerName(), p.getShipType()));
        } else {
            FlagPlayer f = getPlayer(p.getPlayerName());
            f.freq = p.getFrequency();
            f.ship = p.getShipType();
        }
    }
    
    public void handleEvent(PlayerLeft event) {
        FlagPlayer p = getPlayer(ba.getPlayerName(event.getPlayerID()));
        if (p != null)
            p.flagReset();
    }
    
    public void handleEvent(FlagClaimed event) {
        debug(ba.getPlayerName(event.getPlayerID()) + " claimed flag " + event.getFlagID());
    }
    
    public void handleEvent(FlagPosition event) {
    }
    
    public void handleEvent(FlagDropped event) {
    }
    
    public void handleEvent(TurretEvent event) {
        String name = ba.getPlayerName(event.getAttacherID());
        if (name == null) return;
        FlagPlayer p = getPlayer(name);
        if (p != null && p.flag != null)
            p.flagReset();
    }
    
    public void handleEvent(FrequencyChange event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (isBot(name)) return;
        int freq = event.getFrequency();
        //debug("FreqChange: " + name + " to " + freq);
        FlagPlayer p = players.get(low(name));
        if (p != null) {
            if (freq < 9999 && freq > 1)
                ba.setFreq(name, p.freq);
            p.getFreq();
            p.flagReset();
        } else
            debug("FP error on " + name);
    }
    
    public void handleEvent(FrequencyShipChange event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (isBot(name)) return;
        int ship = event.getShipType();
        //debug("ShipChange: " + name + " to " + ship);
        FlagPlayer p = players.get(low(name));
        if (p != null) {
            p.ship = ship;
            p.flagReset();
        } else
            debug("FP error on " + name);
    }
    
    public void handleEvent(PlayerPosition event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (isBot(name)) return;
        FlagPlayer p = getPlayer(name);
        if (!movingFlag && p != null && p.flag != null) {
            if (p.flag.id != p.getFreq() && p.getFreq() == 0 || p.getFreq() == 1) {
                Team t = team[p.getFreq()];
                if (t.hasFlag() && t.hasClaimed(event)) {
                    movingFlag = true;
                    if (p.getFreq() == 0)
                        ba.warpTo(name, WARPS[0], WARPS[1]);
                    else
                        ba.warpTo(name, WARPS[2], WARPS[3]);
                    p.captures++;
                    t.score++;
                    ba.sendArenaMessage("Freq " + p.flag.id + "'s flag has been CAPTURED by " + p.name, 104);
                    ba.sendArenaMessage("Score: " + team[0].score + " - " + team[1].score);
                    team[p.flag.id].resetFlag();
                    if (starter != null && t.score >= toWin) {
                        displayStats();
                        ba.sendArenaMessage("GAME OVER! Freq " + p.flag.id + " wins!", 5);
                        if (starter != null)
                            cmd_reset(starter);
                    }
                }
            }
        }
    }
    
    public void handleEvent(PlayerDeath event) {
        String name = ba.getPlayerName(event.getKilleeID());
        String killer = ba.getPlayerName(event.getKillerID());
        if (event.getFlagCount() > 0) {
            int freq = ba.getPlayer(event.getKillerID()).getFrequency();
            if (flag[0].carried && name.equalsIgnoreCase(flag[0].carrier)) {
                flag[0].flag.setPlayerID(event.getKillerID());
                flag[0].flag.setTeam(freq);
            } else if (flag[1].carried && name.equalsIgnoreCase(flag[1].carrier)) {
                flag[1].flag.setPlayerID(event.getKillerID());
                flag[1].flag.setTeam(freq);
            } else
                debug("Error finding player/flag match on death.");
        }
        FlagPlayer f;
        f = getPlayer(name);
        if (f != null)
            f.deaths++;
        f = getPlayer(killer);
        if (f != null) {
            f.kills++;
            f.returns++;
        }
    }
    
    public void handleEvent(Message event) {
        String msg = event.getMessage();
        int type = event.getMessageType();
        
        if (type == Message.PRIVATE_MESSAGE || type == Message.PUBLIC_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) {
            String name = ba.getPlayerName(event.getPlayerID());
            if (name == null)
                name = event.getMessager();
            
            if (msg.startsWith("!stats"))
                cmd_stats(name, msg);
            if (msg.equals("!score"))
                cmd_score(name);
            
            if (opList.isER(name)) {
                if (msg.startsWith("!start"))
                    cmd_start(name, msg);
                if (msg.startsWith("!reset"))
                    cmd_reset(name);
                if (msg.equals("!has"))
                    cmd_has(name);
                if (msg.equals("!f1"))
                    team[0].resetFlag();
                if (msg.equals("!f2"))
                    team[1].resetFlag();
                if (msg.equals("!p1"))
                    flag[0].print();
                if (msg.equals("!p2"))
                    flag[1].print();
                if (msg.equals("!debug"))
                    cmd_debug(name);
                if (msg.equals("!respec"))
                    cmd_respec(name);
            }
        }
    }
    
    private void cmd_respec(String name) {
        ba.cancelTask(spec);
        spec = new SpecTask();
        ba.scheduleTask(spec, 1000, 2000);
        ba.sendPrivateMessage(name, "SpecTask restarted.");
    }
    
    private void cmd_debug(String name) {
        DEBUG = !DEBUG;
        if (DEBUG) {
            debugger = name;
            ba.sendSmartPrivateMessage(name, "Debugging messages ENABLED.");
        } else {
            debugger = "";
            ba.sendSmartPrivateMessage(name, "Debugging messages DISABLED.");
        }
    }
    
    private void cmd_score(String name) {
        ba.sendPrivateMessage(name, "Score: " + team[0].score + " - " + team[1].score);
    }
    
    private void cmd_stats(String name, String cmd) {
        String target;
        if (cmd.contains(" ")) 
            target = cmd.substring(cmd.indexOf(" ") + 1);
        else
            target = name;
        FlagPlayer p = getPlayer(target);
        if (p != null) {
            String[] msg = {
                    "Name:" + p.name + " Freq:" + p.getFreq() + " Ship:" + p.ship,
                    "Kills:" + p.kills + " Deaths:" + p.deaths,
                    "Captures:" + p.captures + " Steals:" + p.steals + " Returns:" + p.returns
            };
            ba.privateMessageSpam(name, msg);
        } else
            ba.sendPrivateMessage(name, "Player '" + target + "' was not found.");
    }
    
    private void cmd_has(String name) {
        if (team[0].hasFlag())
            ba.sendPublicMessage("Freq 0 has their flag.");
        else
            ba.sendPublicMessage("Freq 0 is missing their flag.");
        if (team[1].hasFlag())
            ba.sendPublicMessage("Freq 1 has their flag.");
        else
            ba.sendPublicMessage("Freq 1 is missing their flag.");
    }
    
    private void cmd_start(String name, String cmd) {
        if (cmd.contains(" ") && cmd.length() > 7) {
            try {
                int g = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1));
                if (g > 0 && g < 11)
                    toWin = g;
            } catch (NumberFormatException e) { }
        }
        cmd_reset(name);
        ba.sendArenaMessage("Game to " + toWin + " goals.");
        ba.sendArenaMessage("GO GO GO!!!", 104);
        starter = name;
    }
    
    private void cmd_reset(String name) {
        ba.sendPrivateMessage(name, "Flag game being reset...");
        team[0].score = 0;
        team[1].score = 0;
        ba.scoreResetAll();
        ba.shipResetAll();
        players.clear();
        Iterator<Player> i = ba.getPlayerIterator();
        while (i.hasNext())
            new FlagPlayer(i.next());
        resetFlags();
        ba.cancelTask(spec);
        spec = new SpecTask();
        ba.scheduleTask(spec, 1000, 2000);
    }
    
    private FlagPlayer getPlayer(String name) {
        return players.get(low(name));
    }
    
    private void resetFlags() {
        team[0].resetFlag();
        team[1].resetFlag();
    }
    
    private void displayStats() {
        ArrayList<String> statArray = new ArrayList<String>();
        statArray.add("Result of Freq " + team[0].freq + " vs. Freq " + team[1].freq + ": " + team[0].score + " - " + team[1].score);

        statArray.add(",-------------------------------+------+------+---------+--------+----.");
        statArray.add("|                             S |    K |    D | Returns | Steals |  G |");
        statArray.add("|                          ,----+------+------+---------+--------+----+");
        statArray.add("| Freq " + team[0].freq + "                  /     |" + padNumber(0, 5) + " |" + padNumber(0, 5) + " |" + padNumber(0, 8) + " |"+ padNumber(0, 7) + " |" + padNumber(team[0].score, 3) + " |");
        for (FlagPlayer p : players.values()) {
            if (p.freq == team[0].freq)
                statArray.add("| " + padString(p.name, 25) + " " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.returns, 8) + " |" + padNumber(p.steals, 7) + " |" + padNumber(p.captures, 7) + " |");
        }
        statArray.add("+-------------------------------+------+------+---------+--------+----+");
        statArray.add("|                          ,----+------+------+---------+--------+----+");
        statArray.add("| Freq " + team[1].freq + "                  /     |" + padNumber(0, 5) + " |" + padNumber(0, 5) + " |" + padNumber(0, 8) + " |"+ padNumber(0, 7) + " |" + padNumber(team[1].score, 3) + " |");
        for (FlagPlayer p : players.values()) {
            if (p.freq == team[1].freq)
                statArray.add("| " + padString(p.name, 25) + " " + p.ship + " |" + padNumber(p.kills, 5) + " |" + padNumber(p.deaths, 5) + " |" + padNumber(p.returns, 8) + " |" + padNumber(p.steals, 7) + " |" + padNumber(p.captures, 7) + " |");
        }
        statArray.add("`-------------------------------+------+------+---------+--------+----'");
        ba.arenaMessageSpam(statArray.toArray(new String[statArray.size()]));
    }
    
    /**
     * Helper method adds spaces in front of a number to fit a certain length
     * @param n
     * @param length
     * @return String of length with spaces preceeding a number
     */
    private String padNumber(int n, int length) {
        String str = "";
        String x = "" + n;
        for (int i = 0; i + x.length() < length; i++)
            str += " ";
        return str + x;
    }
    
    /** Helper method adds spaces to a string to meet a certain length **/
    private String padString(String str, int length) {
        for (int i = str.length(); i < length; i++)
            str += " ";
        return str.substring(0, length);
    }
    
    class FlagPlayer {
        Player player;
        String name;
        FlagTask flag;
        int ship;
        int freq;
        
        int kills;
        int deaths;
        int captures;
        int steals;
        int returns;
        
        public FlagPlayer(String name, int ship) {
            this.name = name;
            this.ship = ship;
            flag = null;
            kills = 0;
            deaths = 0;
            captures = 0;
            steals = 0;
            returns = 0;
            player = ba.getPlayer(name);
            freq = player.getFrequency();
        }
        
        public FlagPlayer(Player p) {
            this.name = p.getPlayerName();
            this.ship = p.getShipType();
            freq = p.getFrequency();
            flag = null;
            kills = 0;
            deaths = 0;
            captures = 0;
            steals = 0;
            returns = 0;
            players.put(low(name), this);
            player = p;
        }
        
        public void flagReset() {
            if (flag != null) {
                team[flag.id].resetFlag();
                if (flag != null && flag.flag != null)
                    flag.flag.dropped();
                flag = null;
            }
        }
        
        public int getFreq() {
            freq = player.getFrequency();
            return freq;
        }
        
    }

    class Team {
        int freq;
        int goalX;
        int goalY;
        int score;
        FlagTask flag;

        public Team(int freq, FlagTask flag, int x, int y) {
            this.freq = freq;
            this.flag = flag;
            this.goalX = x * 16;
            this.goalY = y * 16;
            this.score = 0;
        }

        public void resetFlag() {
            int f = rand.nextInt(9998);
            while (f > -1 && f < 2) 
                f = rand.nextInt(9998);
            if (flag.carried) {
                FlagPlayer p = getPlayer(ba.getPlayerName(flag.flag.getPlayerID()));
                if (p != null)
                    p.flag = null;
                ba.shipReset(flag.flag.getPlayerID());
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {}
            }
            Ship s = ba.getShip();
            s.setShip(0);
            s.setFreq(f);
            s.move(flag.flag.getXLocation() * 16, flag.flag.getYLocation() * 16);
            ba.grabFlag(flag.id);
            s.move(goalX, goalY);
            ba.dropFlags();
            s.setFreq(freq);
            ba.grabFlag(flag.id);
            s.move(goalX, goalY);
            s.sendPositionPacket();
            ba.dropFlags();
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {}
            //debug("" + f + ") Flag [" + flag.getFlagID() + "] @ (" + flag.getXLocation() + ", " + flag.getYLocation() + ") Me(" + ba.getShip().getX()/16 + ", " + ba.getShip().getY()/16 + ")");
            debug("Reset flag for freq " + freq);
            s.setShip(8);
            TimerTask wait = new TimerTask() {
                public void run() {
                    movingFlag = false;
                }
            };
            ba.scheduleTask(wait, 2000);
        }
        
        public boolean hasFlag() {
            if (flag.carried) 
                return false;
            int[] pos = { flag.x, flag.y };
            return (pos[0] > ((goalX/16) - 5) && pos[0] < ((goalX/16) + 5) && pos[1] > ((goalY/16) - 5) && pos[1] < ((goalY/16) + 5));
        }
        
        public boolean hasClaimed(PlayerPosition event) {
            int[] pos = { event.getXLocation()/16, event.getYLocation()/16 };
            return (pos[0] > ((goalX/16) - 5) && pos[0] < ((goalX/16) + 5) && pos[1] > ((goalY/16) - 5) && pos[1] < ((goalY/16) + 5));
        }
    }
    
    
    class FlagTask extends TimerTask {
        int id;
        Flag flag;
        String carrier;
        boolean carried;
        int freq;
        int x;
        int y;
        
        public FlagTask(Flag f, int id) {
            this.id = id;
            flag = f;
            carried = flag.carried();
            x = flag.getXLocation();
            y = flag.getYLocation();
            if (carried)
                carrier = ba.getPlayerName(flag.getPlayerID());
            else
                carrier = null;
        }
        
        public boolean hasFlag(int id) {
            return (carrier != null && carrier.equals(ba.getPlayerName(id)));
        }
        
        @Override
        public void run() {
            if (flag.carried() != carried) {
                carried = !carried;
                if (!movingFlag) {
                    if (carried) {
                        carrier = ba.getPlayerName(flag.getPlayerID());
                        //debug(carrier + " grabbed flag.");
                        FlagPlayer p = getPlayer(carrier);
                        p.flag = this;
                        p.steals++;
                        ba.sendArenaMessage("Freq " + id + "'s flag has been STOLEN by " + carrier);
                    } else if (carrier != null) {
                        //debug(carrier + " dropped flag.");
                        FlagPlayer p = getPlayer(carrier);
                        p.flag = null;
                        carrier = null;
                        ba.sendArenaMessage("Freq " + id + "'s flag has been returned to its base");
                    }
                }
            }
            
            if (carried && carrier != null && !carrier.equals(ba.getPlayerName(flag.getPlayerID()))) {
                String lost = carrier;
                FlagPlayer p = getPlayer(lost);
                p.flag = null;
                //String s = "" + carrier + " -> ";
                carrier = ba.getPlayerName(flag.getPlayerID());
                //s += carrier;
                //debug(s);
                if (!isBot(carrier) && !isBot(lost)) {
                    ba.sendArenaMessage("Freq " + id + "'s flag has been RESCUED from " + lost + " by " + carrier);
                    getPlayer(carrier).returns++;
                    team[id].resetFlag();
                }
            }
            
            if (flag.getXLocation() != x || flag.getYLocation() != y) {
                x = flag.getXLocation();
                y = flag.getYLocation();
                //debug("(" + x + ", " + y + ")");
            }
            
            if (flag.getTeam() != freq) {
                //String s = "Freqs: " + freq + " -> ";
                freq = flag.getTeam();
                //s += "" + freq;
                //debug(s);
            }
            
            if (!carried && !team[id].hasFlag()) {
                debug("Displaced flag detected...");
                team[id].resetFlag();
            } else if (freq == -1 && carried && !team[id].hasFlag()) {
                debug("Displaced flag detected...");
                team[id].resetFlag();
            }
        }
        
        public void print() {
            if (carried)
                ba.sendPublicMessage("Flag " + flag.getFlagID() + " carried by " + carrier + " for freq " + freq);
            else
                ba.sendPublicMessage("Flag " + flag.getFlagID() + " non-carried for freq " + freq);
            ba.sendPublicMessage("Position: " + x + ", " + y);
        }
        
    }
    
    class SpecTask extends TimerTask {
        
        boolean on;
        int goal;
        
        public SpecTask() {
            ba.stopReliablePositionUpdating();
            ba.stopSpectatingPlayer();
            goal = 0;
            on = true;
        }
        
        public void pause() {
            on = false;
        }
        
        public void resume() {
            on = true;
        }
        
        @Override
        public void run() {
            if (on && ba.getShip().getShip() == 8) {
                ba.stopReliablePositionUpdating();
                ba.stopSpectatingPlayer();
                if (goal == 0) {
                    ba.moveToTile(GOALS[0], GOALS[1]);
                    goal = 1;
                } else {
                    ba.moveToTile(GOALS[2], GOALS[3]);
                    goal = 0;
                }
            }
        }
        
        @Override
        public boolean cancel() {
            on = false;
            ba.setPlayerPositionUpdating(300);
            return true;
        }
        
    }
    
    boolean isBot(String name) {
        return ba.getBotName().equals(name);
    }
    
    @Override
    public String[] getModHelpMessage() {
        return new String[] {
                "Halo CTF Commands: ",
                " !start <#>       - Start a new game to # goals, or default is 5",
                " !score           - Display current score",
                " !stats <name>    - Displays your current stats or stats of <name>",
                " !reset           - Completely resets the flag game",
                " !f1              - Returns freq 0's flag to its base",
                " !f2              - Returns freq 1's flag to its base",
                " !debug           - Enables/disables debug messages"
        };
    }

    @Override
    public boolean isUnloadable() {
        return true;
    }
    
    private void debug(String msg) {
        if (DEBUG)
            ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }
    
    String low(String s) {
        return s.toLowerCase();
    }

}
