package twcore.bots.multibot.nuke;

import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

public final class nuke extends MultiModule {

    @Override
    public void cancel() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void init() {
        // TODO Auto-generated method stub
        
    }
    
    public void handleEvent(Message event) {
        String name = event.getMessager();
        if (name == null)
            name = m_botAction.getPlayerName(event.getPlayerID());
        String msg = event.getMessage();
        if (msg.startsWith("!nuke")) {
            itemCommandNukeBase(name, "");
        }
    }

    private void itemCommandNukeBase(String sender, String params) {
        Player p = m_botAction.getPlayer(sender);
        final int freq = p.getFrequency();

        final Vector<Shot> shots = getShots();
        final Vector<Warper> warps = new Vector<Warper>();
        Iterator<Integer> i = m_botAction.getFreqIDIterator(freq);
        while (i.hasNext()) {
            int id = i.next();
            Player pl = m_botAction.getPlayer(id);
            int x = pl.getXTileLocation();
            int y = pl.getYTileLocation();
            if (x > 475 && x < 549 && y > 248 && y < 300)
                warps.add(new Warper(id, x, y));
        }

        m_botAction.getShip().setShip(0);
        m_botAction.getShip().setFreq(freq);
        m_botAction.specificPrize(m_botAction.getBotName(), Tools.Prize.SHIELDS);

        m_botAction.sendTeamMessage("Incoming nuke! Anyone inside the FLAGROOM will be WARPED for a moment and then returned when safe.");
        m_botAction.sendArenaMessage(sender + " has sent a nuke in the direction of the flagroom! Impact is imminent!",17);
        final TimerTask timerFire = new TimerTask() {
            public void run() {
                while (!shots.isEmpty()) {
                    m_botAction.getShip().sendPositionPacket();
                    Shot s = shots.remove(0);
                    m_botAction.getShip().rotateDegrees(s.a);
                    m_botAction.getShip().sendPositionPacket();
                    m_botAction.getShip().move(s.x, s.y);
                    m_botAction.getShip().sendPositionPacket();
                    m_botAction.getShip().fire(WeaponFired.WEAPON_THOR);
                    try { Thread.sleep(75); } catch (InterruptedException e) {}
                }
            }
        };
        timerFire.run();

        TimerTask shields = new TimerTask() {
            public void run() {
                for (Warper w: warps)
                    w.save();
            }
        };
        m_botAction.scheduleTask(shields, 3100);

        TimerTask timer = new TimerTask() {
            public void run() {
                m_botAction.specWithoutLock(m_botAction.getBotName());
                m_botAction.move(512*16, 350*16);
                m_botAction.getShip().setSpectatorUpdateTime(100);
                //Iterator<Integer> i = m_botAction.getFreqIDIterator(freq);
                for (Warper w : warps)
                    w.back();
            }
        };
        m_botAction.scheduleTask(timer, 5500);
    }

    private class Warper {
        int id, x, y;

        public Warper(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        public void save() {
            m_botAction.warpTo(id, 512, 205);
        }

        public void back() {
            m_botAction.warpTo(id, x, y);
        }
    }

    class Shot {
        int a, x, y;

        public Shot(int a, int x, int y) {
            this.a = a;
            this.x = x*16;
            this.y = y*16;
        }
    }

    private Vector<Shot> getShots() {
        Vector<Shot> s = new Vector<Shot>();
        s.add(new Shot(15, 396, 219));
        s.add(new Shot(165, 628, 219));
        s.add(new Shot(30, 407, 198));
        s.add(new Shot(150, 617, 198));
        s.add(new Shot(47, 425, 180));
        s.add(new Shot(135, 599, 180));
        s.add(new Shot(65, 450, 160));
        s.add(new Shot(115, 575, 160));
        s.add(new Shot(80, 472, 150));
        s.add(new Shot(100, 552, 150));
        s.add(new Shot(90, 492, 147));
        s.add(new Shot(90, 531, 147));
        return s;
    }

    @Override
    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request(this, EventRequester.MESSAGE);
    }

    @Override
    public String[] getModHelpMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isUnloadable() {
        // TODO Auto-generated method stub
        return false;
    }

}
