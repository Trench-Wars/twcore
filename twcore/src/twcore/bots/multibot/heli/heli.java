package twcore.bots.multibot.heli;

import java.util.ArrayList;
import java.util.Random;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.events.Message;
import twcore.core.game.Ship;

public class heli extends MultiModule {

    // Static settings
    private final static int xMin = 256 * 16;
    private final static int xMax = 767 * 16;
    private final static int yMin = 467 * 16;
    private final static int yMax = 556 * 16;
    private final static int yStart = 515 * 16;
    private final static int startTunnelHeight = 20;
    
    public OperatorList opList;
    
    // Timers
    TimerTask nextWall;
    TimerTask nextBarrier;
    TimerTask timerDelayedStart;
    
    Random rand = new Random();
    
    // Adaptable settings
    private int mineInterval = 3;
    private int speed = 100;
    private int tunnelHeight = 20;
    private int barrierHeight = 6;
    private boolean smoothTunnel;
    
    // Internally used tracking variables.
    private int y = 0;
    private int x = 0;
    private int slope = 3;
    private int yDiff = 0;
    private int currentSlope = 0;
    
    // Debug related variables for live testing.
    private boolean debugEnabled = false;
    private String debugger = "";

    public void init() {
        opList = m_botAction.getOperatorList();
    }

    public String[] getModHelpMessage() {
        String[] blah = {
                "!manual            -Make an educated guess!",
                "!rules             -Displays the rules etc of the game to the players.",
                "!start             -DURRRRRRRRRRRRRRRRRRRRR",
//                "!start #           -Delayed start in #seconds. Everything automated except winner detection.",
                "!setmove #         -Sets distance between barrier mines.",
                "!setslope #        -Sets difficulty increases in difficulty as you get higher.",
                "!setspeed #        -Sets bot's speed in milliseconds.",
                "!setheight #       -Sets the height of the tunnel.",
                "!setwallheight #   -Sets the height of the barriers.",
                "!togglesmooth      -Toggles smooth transitions on slope changes.",
                "!settings          -Displays the current settings.",
                "!stop              -DURRRRRRRRRRRRRRRRRRRRR",
                "!specbot           -Puts bot into spectator mode."
        };
        return blah;
    }

    @Override
    public void handleEvent(Message event) {
        String message = event.getMessage();
        String name = "";

        if (event.getMessageType() == Message.PRIVATE_MESSAGE)
            name = m_botAction.getPlayerName(event.getPlayerID());
        else if (event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
            name = event.getMessager();

        if (opList.isER(name))
            handleCommand(name, message);
    }

    public void handleCommand(String name, String message) {
        message = message.toLowerCase();
        if (message.startsWith("!manual")) {
            dispManual(name);
        } else if (message.equals("!rules")) {
            dispRules();
        } else if (message.equals("!start")) {
            m_botAction.sendPrivateMessage(name, "Starting...");
            startThing();
        } else if (message.startsWith("!start ") && debugEnabled && debugger.equals(name)) {
          //Intended use: Debug mode for live testing by dev without an ER+ present.
            try {
                Integer delay = Integer.parseInt(message.substring(7));
                m_botAction.sendPrivateMessage(name, "Starting in " + delay + " seconds.");
                delayedStart(delay);
            } catch (Exception e) {}
        } else if (message.startsWith("!specbot")) {
            m_botAction.cancelTasks();
            m_botAction.spec(m_botAction.getBotName());
            m_botAction.spec(m_botAction.getBotName());
            m_botAction.resetReliablePositionUpdating();
        } else if (message.startsWith("!setmove ")) {
            try {
                mineInterval = Integer.parseInt(message.substring(9));
                m_botAction.sendPrivateMessage(name, "Set tile distance between mines to: " + mineInterval);
            } catch (Exception e) {}
        } else if (message.startsWith("!setspeed ")) {
            try {
                speed = Integer.parseInt(message.substring(10));
                m_botAction.sendPrivateMessage(name, "Set speed to: " + speed);
            } catch (Exception e) {}
        } else if (message.startsWith("!setslope ")) {
            try {
                slope = Integer.parseInt(message.substring(10));
                m_botAction.sendPrivateMessage(name, "Set slope to: " + slope);
            } catch (Exception e) {}
        } else if (message.startsWith("!setheight ")) {
            try {
                tunnelHeight = Integer.parseInt(message.substring(11));
                m_botAction.sendPrivateMessage(name, "Set height of the tunnel to: " + tunnelHeight);
            } catch (Exception e) {}
        } else if (message.startsWith("!setwallheight ")) {
            try {
                barrierHeight = Integer.parseInt(message.substring(15));
                m_botAction.sendPrivateMessage(name, "Set height of the bariers to: " + barrierHeight);
            } catch (Exception e) {}
        } else if (message.startsWith("!togglesmooth")) {
            smoothTunnel = !smoothTunnel;
            m_botAction.sendSmartPrivateMessage(name, "Smooth walls are now " + (smoothTunnel ? "enabled" : "disabled"));
        } else if (message.startsWith("!settings")) {
            dispSettings(name);
        } else if (message.startsWith("!stop")) {
            m_botAction.sendPrivateMessage(name, "Stopping...");
            m_botAction.cancelTasks();
        } else if (message.equals("!debug") && opList.isSysop(name)) {
            cmd_debug(name);
        }
    }

    public void delayedStart(Integer delay) {
        if(delay == null || delay < 0 || delay > 60)
            return;
        
        m_botAction.specAllOnFreq(8000);
        
        if(delay == 0) {
            m_botAction.changeAllShips(Tools.Ship.WARBIRD);
            m_botAction.setAlltoFreq(0);
            m_botAction.prizeAll(Tools.Prize.ROCKET);
            m_botAction.sendArenaMessage("GOGOGO!!!", Tools.Sound.GOGOGO);
            startThing();
        } else {
            m_botAction.sendArenaMessage("Get ready. Starting in " + delay + " seconds...", Tools.Sound.BEEP1);
            timerDelayedStart = new TimerTask() {
                
                @Override
                public void run() {
                    m_botAction.changeAllShips(Tools.Ship.WARBIRD);
                    m_botAction.setAlltoFreq(0);
                    m_botAction.prizeAll(Tools.Prize.ROCKET);
                    m_botAction.sendArenaMessage("GOGOGO!!!", Tools.Sound.GOGOGO);
                    startThing();                   
                }
            };
            m_botAction.scheduleTask(timerDelayedStart, delay * Tools.TimeInMillis.SECOND);
        }
        
    }
    
    public void startThing() {
        m_botAction.setPlayerPositionUpdating(0);
        Ship ship = m_botAction.getShip();
        // Spec the bot to clear any remaining mines.
        ship.setShip(8);
        try {
            // Giving it some time to update.
            Thread.sleep(100);
        } catch (Exception e) {}  
        
        ship.setShip(7);
        ship.setFreq(8000);
        ship.sendPositionPacket();
        // Instead of 16, 12 is used to make the top 3/4th above the platform, and the bottom stick 1/4th underneath it.
        ship.move(xMin, yStart - startTunnelHeight * 12);
        y = ship.getY();
        x = ship.getX();
        yDiff = 0;
        
        smoothStartWallSection();
        
        nextWall = new TimerTask() {
            public void run() {
                nextWallSection();
            }
        };
        nextBarrier = new TimerTask() {
            public void run() {
                nextBarrier();
            }
        };
        m_botAction.scheduleTaskAtFixedRate(nextWall, speed, speed);
        m_botAction.scheduleTaskAtFixedRate(nextBarrier, speed * 8 + speed / 2, speed * 8);
    }

    /**
     * Creates an initial section with a gradual slope to the set tunnel height.
     * This is to prevent players from being instantly killed when using narrow tunnels.
     */
    public void smoothStartWallSection() {
        int mineDistance = mineInterval * 16;
        int currentTunnelHeight = startTunnelHeight;
        Ship ship = m_botAction.getShip();
        
        // For the first five mines, go straight.
        for(int i = 0; i < 5; i++) {
            ship.moveAndFire(x, y, getWeapon('#'));
            try {
                Thread.sleep(speed / 10);
            } catch (Exception e) {}
            ship.moveAndFire(x, y + currentTunnelHeight * 16, getWeapon('#'));
            try {
                Thread.sleep(speed / 10);
            } catch (Exception e) {}
            
            x += mineDistance;
        }
        
        // Now, slope until we are at the correct height
        while(currentTunnelHeight < tunnelHeight) {
            ship.moveAndFire(x, --y, getWeapon('#'));
            try {
                Thread.sleep(speed / 10);
            } catch (Exception e) {}
            ship.moveAndFire(x, y + (++currentTunnelHeight) * 16, getWeapon('#'));
            try {
                Thread.sleep(speed / 10);
            } catch (Exception e) {}
            
            x += mineDistance;             
        }
        while(currentTunnelHeight > tunnelHeight) {
            ship.moveAndFire(x, ++y, getWeapon('#'));
            try {
                Thread.sleep(speed / 10);
            } catch (Exception e) {}
            ship.moveAndFire(x, y + (--currentTunnelHeight) * 16, getWeapon('#'));
            try {
                Thread.sleep(speed / 10);
            } catch (Exception e) {}
            
            x += mineDistance;
        }

    }
    
    /**
     * Places the next section of the wall.
     */
    public void nextWallSection() {
        int targetSlope = slope * (rand.nextInt(200) - 100) / 100;
        int move = this.mineInterval*16;                        // Conversion tiles -> points.
        if (yDiff > tunnelHeight && targetSlope > 0) {
            targetSlope *= -1;
        } else if (yDiff < -tunnelHeight && targetSlope < 0) {
            targetSlope *= -1;
        }
        targetSlope *= 16;
        
        if(!smoothTunnel)
            currentSlope = targetSlope;
        
        int distance = rand.nextInt(50) / 10;
        Ship ship = m_botAction.getShip();
        
        for (int k = 0; k < distance; k++) {
            ship.moveAndFire(x, y, getWeapon('#'));
            try {
                // Delay tactics!
                Thread.sleep(speed/(2*distance));
            } catch (Exception e) {}
            ship.moveAndFire(x, y + tunnelHeight * 16, getWeapon('#'));
            try {
                // Delay tactics!
                Thread.sleep(speed/(2*distance));
            } catch (Exception e) {}
            
            // Update the current slope.
            if(smoothTunnel) {
                currentSlope += (targetSlope - currentSlope + 1) / 2;
            }
            
            x += move;
            y -= currentSlope;
            // Check height restrictions
            if(y < yMin || (y + tunnelHeight*16) > yMax) {
                y += 2*currentSlope;
            }
            
            yDiff += currentSlope / 16;
            if (x >= xMax) {
                m_botAction.cancelTasks();
                // Just to make sure he aint glitching on the wall.
                ship.move(xMax + 80, yStart);
                break;
            }
        }
    }

    /**
     * Places the next barrier.
     */
    public void nextBarrier() {
        Ship ship = m_botAction.getShip();
        int tiles = rand.nextInt(tunnelHeight);
        int length = barrierHeight;
        if ((tunnelHeight - length) - tiles < 0)
            length += (tunnelHeight - length) - tiles;
        int yStart = y;
        int xStart = x;
        for (int k = 0; k < length; k++) {
            ship.moveAndFire(xStart, yStart + tiles * 16 + k * 16, getWeapon('*'));
            try {
                // Delay tactics!
                Thread.sleep(speed/(2*length));
            } catch (Exception e) {}
        }
    }

    /**
     * Fetches a weapon based on a symbol.
     * @param c Provided symbol.
     * @return Weapon number.
     */
    public int getWeapon(char c) {
        Ship s = m_botAction.getShip();

        if (c == '.')
            return s.getWeaponNumber((byte) 3, (byte) 0, false, false, true, (byte) 8, true);
        if (c == '*')
            return s.getWeaponNumber((byte) 3, (byte) 1, false, false, true, (byte) 8, true);
        if (c == '#')
            return s.getWeaponNumber((byte) 3, (byte) 2, false, false, true, (byte) 8, true);
        if (c == '^')
            return s.getWeaponNumber((byte) 3, (byte) 3, false, false, true, (byte) 8, true);
        if (c == '1')
            return s.getWeaponNumber((byte) 4, (byte) 0, false, false, true, (byte) 8, true);
        if (c == '2')
            return s.getWeaponNumber((byte) 4, (byte) 1, false, false, true, (byte) 8, true);
        if (c == '3')
            return s.getWeaponNumber((byte) 4, (byte) 2, false, false, true, (byte) 8, true);
        if (c == '4')
            return s.getWeaponNumber((byte) 4, (byte) 3, false, false, true, (byte) 8, true);
        return 0;
    }

    public boolean isUnloadable() {
        return true;
    }

    public void requestEvents(ModuleEventRequester req) {
        req.request(this, EventRequester.MESSAGE);
    }

    public void cancel() {
    }
    
    /**
     * Displays a short makeshift manual.
     * @param name Player who requested the manual.
     */
    public void dispManual(String name) {
        Vector<String> spam = new Vector<String>();
        spam.add("Manual:");
        spam.add("This is the heli module, designed for ?go helicopter and originally written by Ikrit.");
        spam.add("The goal of this game is for a group of players to rocket their way through a path of mines created by the bot.");
        spam.add("While doing this, they must stay between the top and bottom wall of the 'tunnel', avoiding any barries of yellow mines.");
        spam.add("For maximum enjoyment, you want all the players on the same freq, but not on 8000, which is the freq the bot uses.");
        spam.add("Some practice is advised with changing the difficutly/settings, so please try it privately beforehand.");
        spam.add("At this moment, you MUST manually handle the rocket prizing, announcing and warping and such.");
        spam.add("We hope you'll enjoy hosting this game!");
        
        m_botAction.privateMessageSpam(name, spam);        
    }
    
    /**
     * Displays the rules to all the players.
     */
    public void dispRules() {
        ArrayList<String> spam = new ArrayList<String>();
        spam.add("Welcome to Helicopter!");
        spam.add("The rules are simple. Dodge all the mines and be the first at the other side.");
        spam.add("For this you will only get one rocket. Good luck!");
        m_botAction.arenaMessageSpam(spam.toArray(new String[spam.size()]));
    }
    
    /**
     * Displays the current and default settings.
     * @param name Person who requested this.
     */
    public void dispSettings(String name) {
        Vector<String> spam = new Vector<String>();
        spam.add("Current settings:");
        spam.add("Mine interval: " + mineInterval + " tiles (default: 3);");
        spam.add("Speed: " + speed + "ms (default: 100);");
        spam.add("slope: " + slope + "% (default: 3);");
        spam.add("Tunnel height: " + tunnelHeight + " tiles (default: 20);");
        spam.add("Barrier height: " + barrierHeight + " tiles (default: 6);");
        spam.add("Smooth walls: " + (smoothTunnel ? "enabled" : "disabled") + " (default: disabled).");
        
        m_botAction.privateMessageSpam(name, spam);
    }
    
    /**
     * Enables/disables debug mode.
     * @param name Person requesting debug mode.
     */
    public void cmd_debug(String name) {
        if(debugEnabled && debugger.equals(name)) {
            debugEnabled = false;
            debugger = "";
            m_botAction.sendSmartPrivateMessage(name, "Debugging disabled.");
        } else if(debugEnabled) {
            m_botAction.sendSmartPrivateMessage(name, "Sorry, " + debugger + " is already using the debug mode.");
        } else {
            debugEnabled = true;
            debugger = name;
            m_botAction.sendSmartPrivateMessage(name, "Debugging mode enabled.");
        }
    }
}