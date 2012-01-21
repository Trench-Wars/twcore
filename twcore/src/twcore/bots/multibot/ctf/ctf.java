package twcore.bots.multibot.ctf;

import java.util.Iterator;
import java.util.Random;

import twcore.bots.MultiModule;
import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.game.Flag;
import twcore.core.game.Ship;
import twcore.core.util.ModuleEventRequester;

/**
 * Capture the Flag bot to be used for the halo arena.
 * 
 * @author WingZero
 */
public class ctf extends MultiModule {

    Random rand;
    
    BotAction ba;

    //Goal0=251,514 - Goal1=772,514
    static final int[] GOALS = { 251, 514, 772, 514 };
    
    boolean DEBUG;
    String debugger;
    
    Team[] team;

    @Override
    public void init() {
        rand = new Random();
        
        ba = m_botAction;
        
        DEBUG = true;
        debugger = "WingZero";
        
        Flag flag1 = null, flag2 = null;
        Iterator<Flag> i = ba.getFlagIterator();
        if (i.hasNext())
            flag1 = i.next();
        else
            ba.die("Detected too few flags.");
        if (i.hasNext())
            flag2 = i.next();
        else
            ba.die("Detected too few flags.");
        if (i.hasNext())
            ba.die("Detected too many flags.");
        
        team = new Team[] { new Team(0, flag1, GOALS[0], GOALS[1]), new Team(1, flag2, GOALS[2], GOALS[3]) };
    }

    @Override
    public void cancel() {
        
    }

    @Override
    public void requestEvents(ModuleEventRequester req) {
        req.request(this, EventRequester.LOGGED_ON);
        req.request(this, EventRequester.ARENA_JOINED);
        req.request(this, EventRequester.FREQUENCY_CHANGE);
        req.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(this, EventRequester.PLAYER_DEATH);
        req.request(this, EventRequester.PLAYER_POSITION);
        req.request(this, EventRequester.PLAYER_ENTERED);
        req.request(this, EventRequester.PLAYER_LEFT);
        req.request(this, EventRequester.FLAG_CLAIMED);
        req.request(this, EventRequester.FLAG_DROPPED);
    }
    
    public void handleEvent(Message event) {
        String msg = event.getMessage();
        int type = event.getMessageType();
        
        if (type == Message.ARENA_MESSAGE) {
            
        } else if (type == Message.PRIVATE_MESSAGE || type == Message.PUBLIC_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) {
            String name = ba.getPlayerName(event.getPlayerID());
            if (name == null)
                name = event.getMessager();
            
            if (!opList.isSmod(name)) return;
            
            if (msg.startsWith("!fgs"))
                resetFlags();
            if (msg.equals("!f1"))
                resetFlag(0);
            if (msg.equals("!f2"))
                resetFlag(1);
            if (msg.equals("!run"))
                ba.getShip().run();
        }
    }

    
    public void resetFlags() {
        debug("Resetting flags...");
        team[0].resetFlag();
        team[1].resetFlag();
    }
    
    public void resetFlag(int x) {
        debug("Resetting flag " + x + "...");
        team[x].resetFlag();
    }



    class Team {

        int freq;
        int goalX;
        int goalY;
        Flag flag;

        public Team(int freq, Flag flag, int x, int y) {
            this.freq = freq;
            this.flag = flag;
            this.goalX = x * 16;
            this.goalY = y * 16;
        }

        public void resetFlag() {
            int f = rand.nextInt(9998);
            while (f < 2 || f == flag.getTeam()) f = rand.nextInt(9998);
            
            flag = ba.getFlag(flag.getFlagID());
            Ship s = ba.getShip();
            s.setShip(0);
            s.setFreq(f);            
            s.move(flag.getXLocation() * 16, flag.getYLocation() * 16);
            s.sendPositionPacket();
            debug("Flag [" + flag.getFlagID() + "] @ (" + flag.getXLocation() + ", " + flag.getYLocation() + ") Me(" + ba.getShip().getX()/16 + ", " + ba.getShip().getY()/16 + ")");
            ba.grabFlag(flag.getFlagID());
            if (ba.getFlag(flag.getFlagID()).carried())
                debug("Flag was grabbed!");
            else
                debug("Flag was not grabbed.");
            s.move(goalX, goalY);
            s.sendPositionPacket();
            
            ba.dropFlags();
            if (flag.carried())
                debug("Flag was not dropped.");
            else
                debug("Flag was dropped!");
            
            s.setFreq(freq);
            s.move(flag.getXLocation() * 16, flag.getYLocation() * 16);
            s.sendPositionPacket();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { };
            
            ba.grabFlag(flag.getFlagID());
            if (flag.carried())
                debug("Flag was grabbed!");
            else
                debug("Flag was not grabbed.");
            
            s.move(goalX, goalY);
            s.sendPositionPacket();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { };
            
            ba.dropFlags();
            if (flag.carried())
                debug("Flag was not dropped.");
            else
                debug("Flag was dropped!");
            
            debug("Flag [" + flag.getFlagID() + "] @ (" + flag.getXLocation() + ", " + flag.getYLocation() + ") Me(" + ba.getShip().getX()/16 + ", " + ba.getShip().getY()/16 + ")");
            debug("Reset flag for freq " + freq);
            s.setShip(8);
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { };
        }

    }
    
    
    @Override
    public String[] getModHelpMessage() {
        return null;
    }

    @Override
    public boolean isUnloadable() {
        return false;
    }
    
    private void debug(String msg) {
        if (DEBUG)
            ba.sendSmartPrivateMessage(debugger, "[DEBUG] " + msg);
    }

}
