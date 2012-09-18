package twcore.bots.achievementbot;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import twcore.bots.achievementbot.Requirement.Type;
import twcore.core.*;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.*;
import twcore.core.game.Player;

/**
 * The PubAchievementsModule allows pubsystem to award player achievements based
 * on their actions in pub.
 *
 * @see http://www.twcore.org/ticket/735
 * @author spookedone
 */
public final class achievementbot extends SubspaceBot {

    private static final boolean XML_VALIDATION = false;
    private String xmlFileName = "Achievements.xml";
    private final List<Achievement> achievements;
    private final Map<Short, List<Achievement>> players;
    private boolean running = false;
    private EventRequester events;
    private CommandInterpreter cmds;
    private OperatorList oplist;
    public static BotAction botAction;
    private String xmlPath; 

    /**
     * Standard constructor for AchievementBot of type SubspaceBot
     *
     * @param m_botAction
     * @param context
     */
    public achievementbot(BotAction m_botAction) {
        super(m_botAction);
        botAction = m_botAction;

        achievements = new LinkedList<Achievement>();
        players = Collections.synchronizedMap(new HashMap<Short, List<Achievement>>());

        events = m_botAction.getEventRequester();
        events.request(EventRequester.FREQUENCY_CHANGE);
        events.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(EventRequester.LOGGED_ON);
        events.request(EventRequester.MESSAGE);
        events.request(EventRequester.PLAYER_LEFT);
        events.request(EventRequester.PLAYER_DEATH);
        events.request(EventRequester.PLAYER_ENTERED);

        cmds = new CommandInterpreter(m_botAction);
        oplist = m_botAction.getOperatorList();
        registerCommands();
    }

    @Override
    public void handleEvent(LoggedOn event) {
        BotSettings config = m_botAction.getBotSettings();

        String arena = config.getString("arena");
        if (arena != null && !arena.isEmpty()) {
            m_botAction.changeArena(arena);
        }
        
        String chat = config.getString("chat");
        if (chat != null && !chat.isEmpty()) {
            m_botAction.sendUnfilteredPublicMessage("?chat=" + chat);
        }
        
        xmlPath = m_botAction.getGeneralSettings().getString("Core Location");
        xmlPath += "/twcore/bots/achievementbot/";
        
        synchronized (achievements) {
            reloadConfig(xmlPath + xmlFileName);
        }
    }

    /**
     * Clear the current achievements and reloads the xml file containing the
     * achievements list.
     */
    public void reloadConfig(String configPath) {
        achievements.clear();

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(XML_VALIDATION);

        try {
            SAXParser parser = factory.newSAXParser();

            InputSource input = new InputSource(new FileReader(configPath));
            input.setSystemId("file://" + new File(configPath).getAbsolutePath());

            AchievementHandler handler = new AchievementHandler();

            parser.parse(input, handler);
        } catch (IOException ex) {
            Logger.getLogger(achievementbot.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(achievementbot.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(achievementbot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Load a player into the list of players having achievements. Required to
     * have a player's achievements tracked and should be called upon player
     * entry into arena.
     *
     * TODO needs to load saved achievement from database
     *
     * @param id id of the player
     */
    public void loadPlayer(short id) {
        if (!players.containsKey(id)) {
            synchronized (players) {
                List<Achievement> playerAchievements = new LinkedList<Achievement>();

                for (Achievement a : achievements) {
                    playerAchievements.add(new Achievement(a));
                }

                Player p = m_botAction.getPlayer(id);
                for (Achievement a : playerAchievements) {
                    a.reset();  //forces time to update
                    if ((a.getTypeMask() & Type.ship.value()) == Type.ship.value()) {
                        for (Requirement r : a.getRequirements()) {
                            if (r instanceof Ship) {
                                Ship s = (Ship) r;
                                s.setCurrent(p.getShipType());
                            }
                        }
                    }
                }
                players.put(id, playerAchievements);
            }
        }
    }

    /**
     * The handleAchievement method tolls a players achievements, based on type
     * of requirement being set on whichever callback fires this method. It will
     * pass along the event and type for further review to see if something was
     * progressed to or achieved.
     *
     * @param id id of player
     * @param type type of requirement update
     * @param event the event itself
     *
     * @see twcore.bots.pubsystem.module.achievements.Requirement
     */
    public void handleAchievement(short id, Type type, SubspaceEvent event) {
        if (players.containsKey(id)) {

            for (Achievement a : players.get(id)) {
                boolean complete = a.update(type, event);
                if (complete) {
                    m_botAction.sendPrivateMessage(id, "[Achievement Completed] "
                            + a.getName() + " - " + a.getDescription());

                    //must set all achievements of same id to complete
                    for (Achievement b : players.get(id)) {
                        if (a.getId() == b.getId()) {
                            b.setComplete(true);
                        }
                    }
                }
            }
        }
    }

    /*
     * EVENT
     */
    @Override
    public void handleEvent(ArenaList event) {
    }

    @Override
    public void handleEvent(PlayerEntered event) {
        if (running) {
            if (!players.containsKey(event.getPlayerID())) {
                loadPlayer(event.getPlayerID());
            }
        }
    }

    @Override
    public void handleEvent(PlayerLeft event) {
        if (running) {
            if (players.containsKey(event.getPlayerID())) {
                players.remove(event.getPlayerID());
            }
        }
    }

    @Override
    public void handleEvent(PlayerDeath event) {
        if (running) {
            short killeeId = event.getKilleeID();
            short killerId = event.getKillerID();

            handleAchievement(killeeId, Type.death, event);
            handleAchievement(killerId, Type.kill, event);
        }
    }

    @Override
    public void handleEvent(Prize event) {
    }

    @Override
    public void handleEvent(WeaponFired event) {
    }

    @Override
    public void handleEvent(FrequencyChange event) {
    }

    @Override
    public void handleEvent(FrequencyShipChange event) {
        if (running) {
            short id = event.getPlayerID();
            handleAchievement(id, Type.ship, event);
        }
    }

    @Override
    public void handleEvent(BallPosition event) {
    }

    @Override
    public void handleEvent(TurretEvent event) {
    }

    @Override
    public void handleEvent(FlagPosition event) {
    }

    @Override
    public void handleEvent(FlagVictory event) {
    }

    @Override
    public void handleEvent(FlagClaimed event) {
        if (running) {
            short id = event.getPlayerID();
            handleAchievement(id, Type.flagclaim, event);
        }
    }

    @Override
    public void handleDisconnect() {
    }

    /**
     * Starts the PubAchievementsModule. Load current players, creates a static
     * timer, and sets its running flag for event callbacks.
     */
    public void start() {
        m_botAction.setPlayerPositionUpdating(0);

        Iterator<Player> i = m_botAction.getPlayerIterator();
        while (i.hasNext()) {
            Player player = i.next();
            loadPlayer(player.getPlayerID());
        }

        m_botAction.scheduleTaskAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                Time.increment();
                try {
                    for (short id : players.keySet()) {
                        for (Achievement a : players.get(id)) {
                            handleAchievement(id, Type.time, null);
                        }
                    }
                } catch (Exception e) {
                }
            }
        }, 0, 1000);

        running = true;
    }

    /**
     * Stops the PubAchievementsModule. Clears current players, cancels the
     * timer, and sets its running flag to false for ignoring callbacks.
     */
    public void stop() {
        players.clear();
        m_botAction.cancelTasks();
        running = false;
    }

    @Override
    public void handleEvent(Message event) {
        cmds.handleEvent(event);
    }

    // <editor-fold defaultstate="collapsed" desc="Command support methods for handleEvent(Message event)">
    /**
     * Add commands to the command interpreter for handleEvent(Message event)
     */
    public void registerCommands() {
        int type = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
        int access = OperatorList.SMOD_LEVEL;
        cmds.registerCommand("!help", type, this, "handleHelpMessage");
        cmds.registerCommand("!die", type, this, "handleDie", access);
        cmds.registerCommand("!start", type, this, "handleStart", access);
        cmds.registerCommand("!stop", type, this, "handleStop", access);
        cmds.registerCommand("!reload", type, this, "handleReload", access);
        cmds.registerCommand("!go", type, this, "handleGo", access);
        cmds.registerCommand("!list", type, this, "handleList");
        cmds.registerDefaultCommand(type, this, "handleInvalidMessage");
    }

    /**
     * Handles any invalid commands sent.
     *
     * @param name
     * @param msg
     */
    public void handleInvalidMessage(String name, String msg) {
        m_botAction.sendSmartPrivateMessage(name, "Invalid command, please use !help.");
    }

    /**
     * Handles listing of Achievements for a defined player. If the second
     * parameter is left null or empty, will default to the requestor (name).
     *
     * @param name requestor of achievement info
     * @param msg name of player's achievements to view
     */
    public void handleList(String name, String msg) {
        if (msg == null || msg.isEmpty()) {
            msg = name;
        }

        if (running) {
            Stack<Integer> ids = new Stack<Integer>();
            Player p = m_botAction.getFuzzyPlayer(msg);
            short id = p.getPlayerID();
            if (!players.containsKey(id)) {
                loadPlayer(id);
            }
            m_botAction.sendPrivateMessage(name, "Achievements for "
                    + p.getPlayerName());
            for (Achievement a : players.get(id)) {
                if (!ids.contains(a.getId())) {
                    ids.push(a.getId());
                    m_botAction.sendPrivateMessage(name, "["
                            + (a.isComplete() ? "X] " : " ] ") + a.getName()
                            + " - " + a.getDescription());
                }

            }
        } else {
            m_botAction.sendPrivateMessage(name, "Achievements are not activated.");
        }
    }

    public void handleGo(String name, String msg) {
        if (running) {
            stop();
        }
        m_botAction.changeArena(msg);
    }

    /**
     * Handles request to reload the Achievements XML file. TODO may want to
     * have optional file giving in parameter msg
     *
     * @param name player that requests reload
     * @param msg ignored
     */
    public void handleReload(String name, String msg) {

        if (running) {
            stop();
            reloadConfig(xmlPath + xmlFileName);
            start();
        } else {
            reloadConfig(xmlPath + xmlFileName);
        }

        m_botAction.sendSmartPrivateMessage(name, "Config reloaded from: " +
                xmlPath + xmlFileName);
    }

    /**
     * Handles stopping of AchievementBot
     *
     * @param name
     * @param msg
     */
    public void handleStop(String name, String msg) {

        if (running) {
            stop();
            m_botAction.sendSmartPrivateMessage(name, "AchievementBot stopped.");
        } else {
            m_botAction.sendSmartPrivateMessage(name, "AchievementBot already stopped.");
        }

    }

    /**
     * Handles starting of AchievementBot
     *
     * @param name
     * @param msg
     */
    public void handleStart(String name, String msg) {

        if (running) {
            m_botAction.sendSmartPrivateMessage(name, "AchievementBot already started.");
        } else {
            start();
            m_botAction.sendSmartPrivateMessage(name, "AchievementBot started.");
        }


    }

    /**
     * Handles death requests, only to be used by Mod+
     *
     * @param name
     * @param msg
     */
    public void handleDie(String name, String msg) {

        if (running) {
            stop();
        }
        m_botAction.die();

    }

    /**
     * Handles which help response to send player depending on access level.
     *
     * @param name
     * @param msg
     */
    public void handleHelpMessage(String name, String msg) {
        if (oplist.isModerator(name)) {
            sendModHelpMessage(name);
        }
        sendHelpMessage(name);
    }

    /**
     * Send player help message.
     *
     * @param sender
     */
    public void sendHelpMessage(String sender) {
        m_botAction.sendSmartPrivateMessage(sender, "!list          -- Lists achievements.");
        m_botAction.sendSmartPrivateMessage(sender, "!list <name>   -- Lists player's achievements");
        m_botAction.sendSmartPrivateMessage(sender, "");
    }

    /**
     * Send Mod+ help message.
     *
     * @param sender
     */
    public void sendModHelpMessage(String sender) {
        m_botAction.sendSmartPrivateMessage(sender, "!start         -- Toggles achievements.");
        m_botAction.sendSmartPrivateMessage(sender, "!stop          -- Toggles achievements.");
        m_botAction.sendSmartPrivateMessage(sender, "!reload        -- Reload the achievements.");
        m_botAction.sendSmartPrivateMessage(sender, "!die           -- Reload the achievements.");
    }

    // </editor-fold>
    /**
     * Handles loading Achievements from XML
     */
    private final class AchievementHandler extends DefaultHandler {

        private StringBuffer buffer = new StringBuffer();
        private Stack<Requirement> requirements = new Stack<Requirement>();
        private Achievement achievement = null;

        @Override
        public void characters(char[] buffer, int start, int length) {
            this.buffer.append(buffer, start, length);
        }

        @Override
        public void startElement(String namespace, String name, String fullName, Attributes attributes) {
            buffer.setLength(0);
            if (fullName.equals("achievements")) {
            } else if (fullName.equals("achievement")) {
                achievement = new Achievement(Integer.parseInt(attributes.getValue("id")));
                achievement.setName(attributes.getValue("name"));
            } else if (fullName.equals("description")) {
            } else if (achievement != null) {
                int typeMask = achievement.getTypeMask();
                try {
                    switch (Type.valueOf(fullName)) {
                        case kill:
                            typeMask |= Type.kill.value();
                            KillDeath kill = new KillDeath(Type.kill);

                            String kMin = attributes.getValue("minimum");
                            String kMax = attributes.getValue("maximum");

                            if (kMin != null) {
                                kill.setMinimum(Integer.parseInt(kMin));
                            }
                            if (kMax != null) {
                                kill.setMaximum(Integer.parseInt(kMax));
                            }

                            requirements.push(kill);

                            break;
                        case death:
                            typeMask |= Type.death.value();
                            KillDeath death = new KillDeath(Type.death);

                            String dMin = attributes.getValue("minimum");
                            String dMax = attributes.getValue("maximum");

                            if (dMin != null) {
                                death.setMinimum(Integer.parseInt(dMin));
                            }
                            if (dMax != null) {
                                death.setMaximum(Integer.parseInt(dMax));
                            }

                            requirements.push(death);

                            break;
                        case location:
                            typeMask |= Type.location.value();
                            Location location;
                            if (requirements.isEmpty()) {
                                location = new Location();
                            } else {
                                Type type = requirements.peek().getType();
                                if (type == Type.kill || type == Type.death) {
                                    location = new Location(type);
                                } else {
                                    location = new Location();
                                }
                            }

                            String x = attributes.getValue("x");
                            String y = attributes.getValue("y");
                            String width = attributes.getValue("width");
                            String height = attributes.getValue("height");
                            String minRange = attributes.getValue("minimumRange");
                            String maxRange = attributes.getValue("maximumRange");

                            if (x != null) {
                                location.setX(Integer.parseInt(x));
                            }
                            if (y != null) {
                                location.setY(Integer.parseInt(y));
                            }
                            if (width != null) {
                                location.setWidth(Integer.parseInt(width));
                            }
                            if (height != null) {
                                location.setHeight(Integer.parseInt(height));
                            }
                            if (minRange != null) {
                                location.setMinRange(Integer.parseInt(minRange));
                            }
                            if (maxRange != null) {
                                location.setMaxRange(Integer.parseInt(maxRange));
                            }

                            requirements.push(location);

                            break;
                        case time:
                            typeMask |= Type.time.value();

                            Time time = new Time();

                            String timeMin = attributes.getValue("minimum");
                            String timeMax = attributes.getValue("maximum");

                            if (timeMin != null) {
                                time.setMinimum(Integer.parseInt(timeMin));
                            }
                            if (timeMax != null) {
                                time.setMaximum(Integer.parseInt(timeMax));
                            }

                            requirements.push(time);

                            break;
                        case range:
                            typeMask |= Type.range.value();
                            Range range = new Range();

                            String rangeMin = attributes.getValue("minimum");
                            String rangeMax = attributes.getValue("maximum");

                            if (rangeMin != null) {
                                range.setMinimum(Integer.parseInt(rangeMin));
                            }
                            if (rangeMax != null) {
                                range.setMaximum(Integer.parseInt(rangeMax));
                            }

                            requirements.push(range);
                            break;
                        case flagclaim:
                            typeMask |= Type.flagclaim.value();
                         
                            FlagClaim flagclaim = new FlagClaim();
                         
                            String flagClaimMin = attributes.getValue("minimum");
                            String flagClaimMax = attributes.getValue("maximum");
                         
                            if (flagClaimMin != null) {
                                flagclaim.setMinimum(Integer.parseInt(flagClaimMin));
                            } if (flagClaimMax != null) {
                                flagclaim.setMaximum(Integer.parseInt(flagClaimMax));
                            }
                            
                            requirements.push(flagclaim);
                            
                            break;
                        case flagtime:
                        /*
                         * type |= Type.flagtime.value();
                         *
                         * ValueRequirement flagtime = new ValueRequirement();
                         *
                         * String flagTimeMin = attributes.getValue("minimum");
                         * String flagTimeMax = attributes.getValue("maximum");
                         *
                         * if (flagTimeMin != null) {
                         * flagtime.setMinimum(Integer.parseInt(flagTimeMin)); }
                         * if (flagTimeMax != null) {
                         * flagtime.setMaximum(Integer.parseInt(flagTimeMax)); }
                         *
                         * break;
                         */
                        case prize:
                        /*
                         * type |= Type.prize.value();
                         *
                         * ValueRequirement prize = new ValueRequirement();
                         *
                         * String prizeMin = attributes.getValue("minimum");
                         * String prizeMax = attributes.getValue("maximum");
                         * String prizeType = attributes.getValue("type");
                         *
                         * if (prizeMin != null) {
                         * prize.setMinimum(Integer.parseInt(prizeMin)); } if
                         * (prizeMax != null) {
                         * prize.setMaximum(Integer.parseInt(prizeMax)); } if
                         * (prizeType != null) {
                         * //achievement.setPrizeType(Integer.parseInt(prizeType));
                         * }
                         *
                         * break;
                         */
                        case ship:
                            typeMask |= Type.ship.value();
                            Ship ship;
                            if (requirements.isEmpty()) {
                                ship = new Ship(Type.ship);
                            } else {
                                ship = new Ship(requirements.peek().getType());
                            }

                            String shipType = attributes.getValue("type");

                            if (shipType != null) {
                                ship.setType(Integer.parseInt(shipType));
                            }

                            requirements.push(ship);

                            break;
                    }
                    achievement.setTypeMask(typeMask);
                } catch (Exception e) {
                    System.err.println("Warning: " + fullName + ": " + e.getMessage());
                }
            }
        }

        @Override
        public void endElement(String namespace, String name, String fullName) {
            if (fullName.equals("description")) {
                achievement.setDescription(buffer.toString().trim());
            } else if (fullName.equals("achievement")) {
                achievements.add(achievement);
            } else if (fullName.equals("achievements")) {
            } else if (!requirements.isEmpty()) {
                Requirement requirement = requirements.pop();
                if (requirements.isEmpty()) {
                    achievement.addRequirement(requirement);
                } else {
                    requirements.peek().addRequirement(requirement);
                }
            }
        }

        /**
         * This method is called when warnings occur
         */
        @Override
        public void warning(SAXParseException exception) {
            System.err.println("WARNING: line " + exception.getLineNumber() + ": "
                    + exception.getMessage());
        }

        /**
         * This method is called when errors occur
         */
        @Override
        public void error(SAXParseException exception) {
            System.err.println("ERROR: line " + exception.getLineNumber() + ": "
                    + exception.getMessage());
        }

        /**
         * This method is called when non-recoverable errors occur.
         */
        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            System.err.println("FATAL: line " + exception.getLineNumber() + ": "
                    + exception.getMessage());
        }
    }
}
