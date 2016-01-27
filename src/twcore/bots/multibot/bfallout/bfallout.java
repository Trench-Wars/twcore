package twcore.bots.multibot.bfallout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.Prize;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;
import twcore.core.game.Projectile;
import twcore.core.game.SpaceShip;

public class bfallout extends MultiModule {

    SpaceShip m_spaceShip;
    CommandInterpreter m_commandInterpreter;

    HashMap<String, BFPlayer> players;

    TimerTask fallOutCheck;

    int m_eventStartTime;
    int m_eventState = 0;       // 0 = nothing, 1 = starting, 2 = playing, 3 = stalling
    int m_shipType = 1;
    boolean m_repsEnabled = false;
    boolean m_decoysEnabled = false;
    String[] opmsg =
    {
        "+------------------------------------------------------------+",
        "| Host Commands:                                             |",
        "|   !start                     - Starts the event            |",
        "|     Params: <#>              - Forces shiptype #           |",
        "|             rep              - Enables reps (wb only)      |",
        "|             decoy            - Enables decoys              |",
        "|   !stop                      - Stops the event             |",
        "|   !spamrules                 - *arena messages the rules   |",
        "+------------------------------------------------------------+"
    };

    BFPlayer bestGreener;

    public void init() {
        m_spaceShip = new SpaceShip(m_botAction, moduleSettings, this, "handleShipEvent", 6, 0);
        m_spaceShip.init();
        m_botAction.getShip().setFreq(0);
        m_commandInterpreter = new CommandInterpreter(m_botAction);
        registerCommands();

        players = new HashMap<String, BFPlayer>();
    }

    public boolean isUnloadable() {
        return m_eventState == 0;
    }

    public String[] getModHelpMessage() {
        return opmsg;
    }

    public void requestEvents(ModuleEventRequester eventRequester) {

        eventRequester.request(this, EventRequester.ARENA_JOINED);
        eventRequester.request(this, EventRequester.PLAYER_ENTERED);
        eventRequester.request(this, EventRequester.PLAYER_LEFT);
        eventRequester.request(this, EventRequester.PLAYER_DEATH);
        eventRequester.request(this, EventRequester.LOGGED_ON);
        eventRequester.request(this, EventRequester.PLAYER_POSITION);
        eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        eventRequester.request(this, EventRequester.PRIZE);
        eventRequester.request(this, EventRequester.WEAPON_FIRED);
    }

    public void registerCommands() {
        int acceptedMessages = Message.PRIVATE_MESSAGE | Message.PUBLIC_MESSAGE | Message.TEAM_MESSAGE | Message.OPPOSING_TEAM_MESSAGE;

        m_commandInterpreter.registerCommand("!help", acceptedMessages, this, "do_help");
        m_commandInterpreter.registerCommand("!start", acceptedMessages, this, "do_start");
        m_commandInterpreter.registerCommand("!stop", acceptedMessages, this, "do_stop");
        m_commandInterpreter.registerCommand("!spamrules", acceptedMessages, this, "do_spamRules");

        //      m_commandInterpreter.registerDefaultCommand(acceptedMessages, this, "do_nothing");
    }

    public void do_help(String name, String message) {
        String[] out = {
            "+------------------------------------------------------------+",
            "| BalanceFalloutBot v.1.0                     - author Sika  |",
            "+------------------------------------------------------------+",
            "| BalanceFallout objectives:                                 |",
            "|   Try to stay inside the moving circle (bot graphic). Last |",
            "|   survivor wins!                                           |",
            "+------------------------------------------------------------+",
            "| Commands:                                                  |",
            "|   !help                      - Brings up this message      |",
            "+------------------------------------------------------------+"
        };
        m_botAction.privateMessageSpam(name, out);

        if (opList.isER(name)) {
            String[] out2 = {
                "| Host Commands:                                             |",
                "|   !start                     - Starts the event            |",
                "|     Params: <#>              - Forces shiptype #           |",
                "|             rep              - Enables reps (wb only)      |",
                "|             decoy            - Enables decoys              |",
                "|   !stop                      - Stops the event             |",
                "|   !spamrules                 - *arena messages the rules   |",
                "+------------------------------------------------------------+"
            };
            m_botAction.privateMessageSpam(name, out2);
        }
    }

    public void do_start(String name, String message) {

        if (!opList.isER(name))
            return;

        if (m_eventState == 0) {
            m_shipType = 1;

            if (message.indexOf("rep") == -1) {
                if (message != null) {
                    int ship;

                    try {
                        String pieces[] = message.split(" ");

                        for (int i = 0; i < pieces.length; i++) {
                            ship = Integer.parseInt(pieces[i]);

                            if (ship <= 8 && ship >= 1)
                                m_shipType = ship;
                        }
                    } catch (NumberFormatException nfe) { }
                }
            } else {
                m_repsEnabled = true;
            }

            if (message.indexOf("decoy") != -1) {
                m_decoysEnabled = true;
            }

            startEvent();
        } else {
            m_botAction.sendPrivateMessage(name, "The event is already in progress!  (!stop it first..?)");
        }
    }

    public void do_stop(String name, String message) {

        if (!opList.isER(name))
            return;

        if (m_eventState == 2) {
            stopEvent();
        } else {
            m_botAction.sendPrivateMessage(name, "The event has not been started yet.");
        }
    }

    public void do_spamRules(String name, String message) {

        if (!opList.isER(name))
            return;

        m_botAction.sendArenaMessage("BalanceFallout rules:  Stay inside the moving circle as long as possible, last player in wins!", 1);
        m_botAction.sendArenaMessage("This event also features a green game:  Pick up the most greens to win!");
    }

    public void startEvent() {
        m_eventState = 1;
        m_botAction.toggleLocked();
        m_botAction.sendArenaMessage("Get ready!  Starting in 10 seconds ..", 2);

        if (m_repsEnabled) {
            m_botAction.sendArenaMessage("This round has reps enabled!  Fire at the bot to make it rep!  Everyone starts with 0 rep uses, pick up greens to get more!");
        }

        if (m_decoysEnabled) {
            m_botAction.sendArenaMessage("Tricky decoy mode enabled!  Choose wisely which circle to follow!");
        }

        TimerTask fiveSeconds = new TimerTask() {
            public void run() {

                Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

                while (i.hasNext()) {
                    Player p = (Player)i.next();

                    if (!p.getPlayerName().equals(m_botAction.getBotName())) {
                        m_botAction.setShip(p.getPlayerID(), m_shipType);
                        m_botAction.setFreq(p.getPlayerID(), 1);
                    }
                }
            }
        };

        TimerTask eightSeconds = new TimerTask() {
            public void run() {
                m_botAction.warpAllRandomly();
                m_botAction.shipResetAll();
            }
        };

        TimerTask tenSeconds = new TimerTask() {
            public void run() {

                Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

                while (i.hasNext()) {
                    Player p = (Player)i.next();

                    if (!p.getPlayerName().equals(m_botAction.getBotName())) {
                        players.put(p.getPlayerName(), new BFPlayer(p.getPlayerName()));
                    }
                }

                if (players.size() <= 0) {
                    m_botAction.sendArenaMessage("Not enough players.");
                    stopEvent();
                } else {
                    m_eventState = 2;
                    m_eventStartTime = (int)(System.currentTimeMillis());
                    m_botAction.sendArenaMessage("GO GO GO!", 104);
                    m_botAction.shipResetAll();

                    changeTarget();

                    fallOutCheck = new TimerTask() {
                        public void run() {
                            Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

                            while (i.hasNext()) {
                                Player p = (Player)i.next();

                                if (players.containsKey(p.getPlayerName()) && !playerInsideRing(p, 2000)) {
                                    handleFallOut(p.getPlayerName());
                                }
                            }
                        }
                    };
                    //                  m_botAction.scheduleTaskAtFixedRate(fallOutCheck, 5000, 5000);
                }
            }
        };
        m_botAction.scheduleTask(fiveSeconds, 5000);
        m_botAction.scheduleTask(eightSeconds, 8000);
        m_botAction.scheduleTask(tenSeconds, 10000);
    }

    public void stopEvent() {
        m_eventState = 0;
        m_botAction.cancelTasks();
        m_botAction.toggleLocked();
        m_spaceShip.reset();
        players.clear();
        m_repsEnabled = false;
        m_decoysEnabled = false;
        bestGreener = null;
        m_botAction.sendArenaMessage("Event has been stopped.");
    }

    public void handleEvent(Message event) {
        m_commandInterpreter.handleEvent(event);

        if (event.getMessageType() == Message.ARENA_MESSAGE) {
            if (event.getMessage().equals("Arena LOCKED")) {
                handleArenaLock(true);
            } else if (event.getMessage().equals("Arena UNLOCKED")) {
                handleArenaLock(false);
            }
        }
    }

    public void handleEvent(ArenaJoined event) {
        m_botAction.setReliableKills(1);
    }

    public void handleEvent(PlayerPosition event) {
        if (m_eventState == 2) {
            String name = m_botAction.getPlayerName(event.getPlayerID());

            if (players.containsKey(name) && !playerInsideRing(m_botAction.getPlayer(event.getPlayerID()), 330)) {
                handleFallOut(name);
            }
        }
    }

    public void handleEvent(PlayerEntered event) {
        String out = "Welcome to BalanceFallout!";

        if (m_eventState != 0) {
            out += " There is a game in progress with " + players.size() + " players in.  [Duration: " + getTimeString() + "]";
        }

        m_botAction.sendPrivateMessage(event.getPlayerID(), out);
    }

    public void handleEvent(PlayerLeft event) {
        String name = m_botAction.getPlayerName(event.getPlayerID());
        handleLagOut(name);
    }

    public void handleEvent(FrequencyShipChange event) {
        String name = m_botAction.getPlayerName(event.getPlayerID());

        if (event.getShipType() == 0) {
            handleLagOut(name);
        }
    }

    public void handleEvent(PlayerDeath event) {
        if (m_eventState == 2) {
            String name = m_botAction.getPlayerName(event.getKilleeID());

            if (players.containsKey(name)) {
                handleFallOut(name);
            }
        }
    }

    public void handleEvent(Prize event) {
        if (m_eventState == 2) {
            String name = m_botAction.getPlayerName(event.getPlayerID());

            if (players.containsKey(name)) {
                BFPlayer p = players.get(name);
                p.incGreens();
            }
        }
    }

    public void handleEvent(WeaponFired event) {
        if (m_eventState == 2 && m_repsEnabled) {
            String name = m_botAction.getPlayerName(event.getPlayerID());

            if (players.containsKey(name)) {
                m_spaceShip.handleEvent(event);
            }
        }
    }

    public boolean playerInsideRing(Player p, int dist) {
        return m_spaceShip.getDistance(p.getXLocation(), p.getYLocation()) < dist;
    }

    public void handleFallOut(String name) {

        BFPlayer p = players.get(name);

        m_botAction.sendArenaMessage(name + " fell out!  Time: " + getTimeString() + "  Greens: " + p.getGreens());

        if (bestGreener == null || bestGreener.getGreens() < p.getGreens()) {
            bestGreener = p;
        }

        players.remove(name);

        m_botAction.spec(name);
        m_botAction.spec(name);

        if (players.size() <= 1) {
            declareWinner();
        }
    }

    public void handleLagOut(String name) {
        if (m_eventState != 0 && players.containsKey(name)) {
            handleFallOut(name);
        }
    }

    public void handleArenaLock(boolean locked) {
        if (locked && m_eventState == 0) {
            m_botAction.toggleLocked();
        } else if (!locked && m_eventState != 0) {
            m_botAction.toggleLocked();
        }
    }

    public void handleShipEvent(Projectile p) {
        if (m_repsEnabled && players.containsKey(p.getOwner())) {
            BFPlayer pl = players.get(p.getOwner());

            if (pl.canUseRep()) {
                m_botAction.getShip().fire(5);
                pl.incRepsUsed();
            }
        }
    }

    public void handleShipEvent(Integer time) {
        changeTarget();
    }

    public void changeTarget() {
        int newX = 1 + (int)(Math.random() * 2000);
        int newY = 1 + (int)(Math.random() * 2000);

        if (Math.random() > 0.5) {
            newX = m_spaceShip.getX() + newX;
        } else {
            newX = m_spaceShip.getX() - newX;
        }

        if (Math.random() > 0.5) {
            newY = m_spaceShip.getY() - newY;
        } else {
            newY = m_spaceShip.getY() + newY;
        }

        if (!m_spaceShip.changeTarget(newX, newY)) {
            changeTarget();
        } else {
            if ((m_decoysEnabled) && (1 + (int)(Math.random() * 3) == 2) && (!getTimeString().equals("0:00"))) {
                m_botAction.getShip().fire(6);
                m_botAction.sendArenaMessage("OoOoOoHh!!", 21);
            }
        }
    }

    public void declareWinner() {
        BFPlayer p;
        String winner = null;
        int g = 0;
        Iterator<String> i = players.keySet().iterator();

        while (i.hasNext()) {
            winner = (String)i.next();
            p = players.get(winner);
            g = p.getGreens();

            if (bestGreener == null || bestGreener.getGreens() < p.getGreens()) {
                bestGreener = p;
            }
        }

        if (winner == null) {
            winner = "-No one (?)-";
        }

        m_botAction.sendArenaMessage("GAME OVER: Winner " + winner + "!  Time: " + getTimeString() + "  Greens: " + g, 5);
        m_botAction.sendArenaMessage("---------  Most Greens: " + bestGreener.getName() + " with " + bestGreener.getGreens() + " green(s)");
        stopEvent();
    }

    public String getTimeString() {
        int time = ((int)(System.currentTimeMillis()) - m_eventStartTime) / 1000;

        if (time <= 0) {
            return "0:00";
        } else {
            int minutes = time / 60;
            int seconds = time % 60;
            return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        }
    }

    public void cancel() {
        m_spaceShip.setShip(8);
        m_spaceShip.interrupt();
        m_botAction.cancelTasks();
    }

    class BFPlayer {

        int greens = 0;
        int repsUsed = 0;
        String name;

        public BFPlayer(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void incGreens() {
            greens++;
        }

        public int getGreens() {
            return greens;
        }

        public void incRepsUsed() {
            repsUsed++;
        }

        public boolean canUseRep() {
            return repsUsed < greens;
        }
    };
}
