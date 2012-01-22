package twcore.bots.multibot.ctf;

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
    Random rand = new Random();

    BotAction ba;
    boolean ready;
    boolean DEBUG;
    String debugger;
    FlagTask[] flag;
    Team[] team;
    
    HashMap<String, FlagPlayer> players;

    @Override
    public void init() {
        ba = m_botAction;
        ready = false;
        DEBUG = true;
        debugger = "WingZero";
        
        players = new HashMap<String, FlagPlayer>();
        Iterator<Player> j = ba.getPlayerIterator();
        while (j.hasNext())
            new FlagPlayer(j.next());
        
        ba.shipResetAll();
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {}
        
        Flag flag1 = null, flag2 = null;
        Iterator<Flag> i = ba.getFlagIterator();
        if (i.hasNext()) flag1 = i.next();
        else throw new NullPointerException();
        if (i.hasNext()) flag2 = i.next();
        else throw new NullPointerException();
        if (i.hasNext()) throw new NullPointerException();
        flag = new FlagTask[] { new FlagTask(flag1, 0), new FlagTask(flag2, 1) };
        ba.scheduleTask(flag[0], 1000, 1000);
        ba.scheduleTask(flag[1], 1000, 1000);
        team = new Team[] { new Team(0, flag1, GOALS[0], GOALS[1]), new Team(1, flag2, GOALS[2], GOALS[3]) };
        resetFlags();
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
        req.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(this, EventRequester.FREQUENCY_CHANGE);
        req.request(this, EventRequester.FLAG_CLAIMED);
        req.request(this, EventRequester.FLAG_POSITION);
        req.request(this, EventRequester.FLAG_DROPPED);
    }
    
    public void handleEvent(PlayerEntered event) {
        Player p = ba.getPlayer(event.getPlayerID());
        if (!players.containsKey(low(p.getPlayerName()))) {
            players.put(low(p.getPlayerName()), new FlagPlayer(p.getPlayerName(), p.getFrequency(), p.getShipType()));
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
    
    private FlagPlayer getPlayer(String name) {
        return players.get(low(name));
    }
    
    public void handleEvent(FlagClaimed event) {
        if (!ready) return;
        debug("FlagClaim by " + ba.getPlayerName(event.getPlayerID()) + " ID:" + event.getFlagID());
        Team t = getTeam(event.getFlagID());
        Player p = ba.getPlayer(event.getPlayerID());
        if (p.getPlayerName().equals(ba.getBotName())) return;
        if (p.getFrequency() == t.freq) {
            // return flag to home
            t.resetFlag();
            ba.sendArenaMessage("Freq " + t.freq + "'s flag has been RETURNED by " + p.getPlayerName(), 19);
        } else
            ba.sendArenaMessage("Freq " + t.freq + "'s flag has been STOLEN by " + p.getPlayerName(), 19);
    }
    
    public void handleEvent(FlagPosition event) {
        if (!ready) return;
        debug("FlagPos: (" + event.getXLocation() + ", " + event.getYLocation() + ") ID:" + event.getFlagID() + " Team:" + event.getTeam());
        Team t = getTeam(event.getFlagID());
        if (!t.flag.carried() && !t.hasFlag()) {
            t.resetFlag();
            ba.sendArenaMessage("Freq " + t.freq + "'s flag has been returned", 19);
        }
    }
    
    public void handleEvent(FlagDropped event) {
    }
    
    public void handleEvent(FrequencyChange event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (isBot(name)) return;
        int freq = event.getFrequency();
        debug("FreqChange: " + name + " to " + freq);
        FlagPlayer p = players.get(low(name));
        if (p != null) {
            p.freq = freq;
            p.flagReset();
        } else
            debug("FP error on " + name);
    }
    
    public void handleEvent(FrequencyShipChange event) {
        String name = ba.getPlayerName(event.getPlayerID());
        if (isBot(name)) return;
        int ship = event.getShipType();
        debug("ShipChange: " + name + " to " + ship);
        FlagPlayer p = players.get(low(name));
        if (p != null) {
            p.ship = ship;
            p.flagReset();
        } else
            debug("FP error on " + name);
    }
    
    public void handleEvent(PlayerDeath event) {
        if (event.getFlagCount() > 0) {
            String name = ba.getPlayerName(event.getKilleeID());
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
    }
    
    public void handleEvent(Message event) {
        String msg = event.getMessage();
        int type = event.getMessageType();
        
        if (type == Message.PRIVATE_MESSAGE || type == Message.PUBLIC_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) {
            String name = ba.getPlayerName(event.getPlayerID());
            if (name == null)
                name = event.getMessager();
            
            if (!opList.isSmod(name)) return;
            
            if (msg.startsWith("!reset"))
                resetFlags();
            if (msg.equals("!ready"))
                ready = true;
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
        }
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
    
    private Team getTeam(int flag) {
        if (team[0].flag.getFlagID() == flag)
            return team[0];
        else
            return team[1];
    }

    
    public void resetFlags() {
        debug("Resetting flags...");
        team[0].resetFlag();
        team[1].resetFlag();
    }
    
    class FlagPlayer {
        String name;
        FlagTask flag;
        int freq;
        int ship;
        
        int kills;
        int deaths;
        int captures;
        int steals;
        int returns;
        
        public FlagPlayer(String name, int freq, int ship) {
            this.name = name;
            this.freq = freq;
            this.ship = ship;
            flag = null;
            kills = 0;
            deaths = 0;
            captures = 0;
            steals = 0;
            returns = 0;
        }
        
        public FlagPlayer(Player p) {
            this.name = p.getPlayerName();
            this.freq = p.getFrequency();
            this.ship = p.getShipType();
            flag = null;
            kills = 0;
            deaths = 0;
            captures = 0;
            steals = 0;
            returns = 0;
            players.put(low(name), this);
        }
        
        public void flagReset() {
            if (flag != null) {
                team[flag.id].resetFlag();
                flag.flag.dropped();
                flag = null;
            }
        }
    }

    class Team {
        int freq;
        int goalX;
        int goalY;
        int score;
        Flag flag;

        public Team(int freq, Flag flag, int x, int y) {
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
            if (flag.carried()) {
                ba.shipReset(flag.getPlayerID());
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {}
            }
            Ship s = ba.getShip();
            s.setShip(0);
            s.setFreq(f);
            s.move(flag.getXLocation() * 16, flag.getYLocation() * 16);
            ba.grabFlag(flag.getFlagID());
            s.move(goalX, goalY);
            ba.dropFlags();
            s.setFreq(freq);
            ba.grabFlag(flag.getFlagID());
            s.move(goalX, goalY);
            s.sendPositionPacket();
            ba.dropFlags();
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {}
            debug("" + f + ") Flag [" + flag.getFlagID() + "] @ (" + flag.getXLocation() + ", " + flag.getYLocation() + ") Me(" + ba.getShip().getX()/16 + ", " + ba.getShip().getY()/16 + ")");
            debug("Reset flag for freq " + freq);
            s.setShip(8);
            ba.resetReliablePositionUpdating();
        }
        
        public boolean hasFlag() {
            int[] pos = { flag.getXLocation(), flag.getYLocation() };
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
                if (carried) {
                    carrier = ba.getPlayerName(flag.getPlayerID());
                    debug(carrier + " grabbed flag.");
                    ba.sendArenaMessage("Freq " + id + "'s flag has been stolen by " + carrier);
                } else if (carrier != null) {
                    debug(carrier + " dropped flag.");
                    carrier = null;
                    ba.sendArenaMessage("Freq " + id + "'s flag has been returned to base");
                }
            }
            
            //test
            if (carried) {
                int pid = flag.getPlayerID();
                if (carrier != null && !carrier.equals(ba.getPlayerName(pid)))
                    debug("Carrier name discrepency detected...");
            } else if (carrier != null)
                debug("Carrier name not null while flag not carried");
            
            if (carried && carrier != null && !carrier.equals(ba.getPlayerName(flag.getPlayerID()))) {
                String lost = carrier;
                String s = "" + carrier + " -> ";
                carrier = ba.getPlayerName(flag.getPlayerID());
                s += carrier;
                debug(s);
                if (!isBot(carrier) && !isBot(lost))
                    ba.sendArenaMessage("Freq " + id + "'s flag has been rescued from " + lost + " by " + carrier);
            }
            
            if (flag.getXLocation() != x || flag.getYLocation() != y) {
                x = flag.getXLocation();
                y = flag.getYLocation();
                debug("(" + x + ", " + y + ")");
            }
            
            if (flag.getTeam() != freq) {
                String s = "Freqs: " + freq + " -> ";
                freq = flag.getTeam();
                s += "" + freq;
                debug(s);
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
    
    boolean isBot(String name) {
        return ba.getBotName().equals(name);
    }
    
    @Override
    public String[] getModHelpMessage() {
        return null;
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
