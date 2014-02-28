package twcore.bots.multibot.nuke;

import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;
import twcore.core.util.MapRegions;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

public final class nuke extends MultiModule {

    private static final int SMALL_OBJON = 1001;
    private static final int MED_OBJON = 1002;
    private static final int LARGE_OBJON = 1003;
    private static final int LEFT_SIDE_DOOR = 1004;
    private static final int RIGHT_SIDE_DOOR = 1005;
    private static final int SMALL_BASE = 6;
    private static final int MED_BASE = 9;
    private static final int LARGE_BASE = 8;
    private static final String MAP_NAME = "pubmap";

    private static final int LARGE_REGION = 2;
    private static final int MED_REGION = 3;
    private static final int SMALL_REGION = 4;
    private static final int FR_REGION = 1;
    private static final int MID_REGION = 0;
    
    private Random rand;
    private MapRegions regions;
    private BotAction ba;
    private int currentBase;
    
    
    @Override
    public void cancel() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void init() {
        ba = m_botAction;
        rand = new Random();
        currentBase = MED_BASE;
        regions = new MapRegions();
        reloadRegions();
        ba.getShip().setSpectatorUpdateTime(200);
    }
    
    public void reloadRegions() {
        try {
            regions.clearRegions();
            regions.loadRegionImage(MAP_NAME + ".png");
            regions.loadRegionCfg(MAP_NAME + ".cfg");
        } catch (FileNotFoundException fnf) {
            Tools.printLog("Error: " + MAP_NAME + ".png and " + MAP_NAME + ".cfg must be in the data/maps folder.");
        } catch (javax.imageio.IIOException iie) {
            Tools.printLog("Error: couldn't read image");
        } catch (Exception e) {
            Tools.printLog("Could not load warps for " + MAP_NAME);
            Tools.printStackTrace(e);
        }
    }
    
    public void handleEvent(Message event) {
        String name = event.getMessager();
        if (name == null)
            name = m_botAction.getPlayerName(event.getPlayerID());
        String msg = event.getMessage();
        if (msg.startsWith("!nuke")) {
            itemCommandNukeBase(name, "");
        } else if (msg.startsWith("!set "))
            cmd_setBase(name, msg);
    }
    
    private void cmd_setBase(String name, String msg) {

        if (msg.length() <= "!set ".length())
            return;
        
        msg = msg.substring(msg.indexOf(" ") + 1).toLowerCase();
        if (msg.equals("small"))
            currentBase = SMALL_BASE;
        else if (msg.equals("med"))
            currentBase = MED_BASE;
        else if (msg.equals("large"))
            currentBase = LARGE_BASE;
        else
            return;
        
        ba.sendSmartPrivateMessage(name, "Changing base to " + msg.toUpperCase());
        
        ba.setDoors(currentBase);
        switch (currentBase) {
            case SMALL_BASE:
                ba.showObject(LEFT_SIDE_DOOR);
                ba.showObject(RIGHT_SIDE_DOOR);
                ba.showObject(SMALL_OBJON);
                ba.hideObject(MED_OBJON);
                ba.hideObject(LARGE_OBJON);
                warpForSmall();
                break;
            case MED_BASE:
                ba.showObject(MED_OBJON);
                ba.hideObject(SMALL_OBJON);
                ba.hideObject(LARGE_OBJON);
                ba.hideObject(LEFT_SIDE_DOOR);
                ba.hideObject(RIGHT_SIDE_DOOR);
                warpForMedium();
                break;
            case LARGE_BASE:
                ba.showObject(LARGE_OBJON);
                ba.hideObject(SMALL_OBJON);
                ba.hideObject(MED_OBJON);
                ba.hideObject(LEFT_SIDE_DOOR);
                ba.hideObject(RIGHT_SIDE_DOOR);
                break;
        }
    }
    
    private void warpForMedium() {
        Iterator<Player> i = ba.getPlayingPlayerIterator();
        while (i.hasNext()) {
            Player p = i.next();
            int reg = regions.getRegion(p);
            if (reg == LARGE_REGION) {
                Point coord = getRandomPoint(FR_REGION);
                ba.warpTo(p.getPlayerID(), (int) coord.getX(), (int) coord.getY());
                ba.sendPublicMessage("Warped " + p.getPlayerName() + ": " + coord.getX() + " " + coord.getY());
            }
        }
    }
    
    private void warpForSmall() {
        Iterator<Player> i = ba.getPlayingPlayerIterator();
        while (i.hasNext()) {
            Player p = i.next();
            int reg = regions.getRegion(p);
            if (reg == LARGE_REGION || reg == MED_REGION) {
                Point coord = getRandomPoint(FR_REGION);
                ba.sendPublicMessage("Warped " + p.getPlayerName() + ": " + coord.getX() + " " + coord.getY());
                ba.warpTo(p.getPlayerID(), (int) coord.getX(), (int) coord.getY());
            } else if (reg == SMALL_REGION) {
                Point coord = getRandomPoint(MID_REGION);
                ba.warpTo(p.getPlayerID(), (int) coord.getX(), (int) coord.getY());
                ba.sendPublicMessage("Warped " + p.getPlayerName() + ": " + coord.getX() + " " + coord.getY());
            }
        }
    }

    /**
     * Generates a random point within the specified region
     * 
     * @param region
     *            the region the point should be in
     * @return A point object, or null if the first 10000 points generated were not within the desired region.
     */
    private Point getRandomPoint(int region) {
        Point p = null;

        int count = 0;
        while (p == null) {
            int x = rand.nextInt(590-430) + 430;
            int y = rand.nextInt(340-250) + 250;

            p = new Point(x, y);
            if (!regions.checkRegion(x, y, region))
                p = null;
            count++;
            if (count > 100000)
                p = new Point(512, 280);
        }

        return p;
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
            // 1 2 3 5
            int reg = regions.getRegion(pl);
            if (reg == 1 || reg == 2 || reg == 3 || reg == 5)
                warps.add(new Warper(id, pl.getXTileLocation(), pl.getYTileLocation()));
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
                m_botAction.move(512*16, 285*16);
                m_botAction.setPlayerPositionUpdating(300);
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
            m_botAction.warpTo(id, 512, 141);
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
        s.add(new Shot(90, 496, 154));
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

        String[] nukeHelp = { "!set <small,med,large>               -- Starts a size of base ",
                              "!nuke                                -- Nuke the base!"
        };
        return nukeHelp;
        
    }

    @Override
    public boolean isUnloadable() {
        return true;
    }

}
