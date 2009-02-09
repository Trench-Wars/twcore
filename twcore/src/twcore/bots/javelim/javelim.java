package twcore.bots.javelim;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TimerTask;
import java.util.TreeMap;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.ShortMap;
import twcore.core.util.Tools;

/**
 * Bot for hosting ?go javelim (<a href="http://www.twcore.org/ticket/74">ticket #74</a>)
 * @author  flibb
 */
public final class javelim extends SubspaceBot implements LagoutMan.ExpiredLagoutHandler<String> {

    private BotSettings     m_botSettings;
    private BotAction       m_botAction;
    private OperatorList    m_operatorList;
    private String          m_connectionName;

    private State           m_state = new State(State.STOPPED);

    private String              m_startedBy = "";
    private int                 m_deathsToSpec = 10;
    private boolean             m_ensureLock = false;
    private boolean             m_skipFirstRound;

    private Map<String, KimPlayer>
                                m_allPlayers = Collections.synchronizedMap(new HashMap<String, KimPlayer>(64));
    private KimTeam[]           m_survivingTeams = new KimTeam[4];
    private int                 m_survivorCount = 0;
    private int                 m_maxTeamSize = 1;
    private boolean             m_isTeams;
    private KimTeam[]           m_teams = new KimTeam[64];
    private int[]               m_groupCount = { 0, 0, 0, 0 }; //for tracking how many teams left in each group
    private int                 m_numTeams;
    private KimTeam             m_winner = null;
    private KimPlayer           m_mvp = null;
    private KimPlayer           m_prevMvp = null;
    private KimPlayer           m_survivor = null;
    private SortedMap<Integer, KimPlayer>
                                m_deathMap = Collections.synchronizedSortedMap(new TreeMap<Integer, KimPlayer>());

    private LagoutMan<String>
                                m_lagoutMan = new LagoutMan<String>(this);
    private List<String>        m_startingLagouts = new LinkedList<String>();
    private List<KimPlayer>     m_startingReturns = new LinkedList<KimPlayer>();

    private HashSet<String>     m_access = new HashSet<String>();
    private ShortMap<KimPlayer>
                                m_kpMap = new ShortMap<KimPlayer>(); //id -> KimPlayer
    private LvzObjects          m_lvz = new LvzObjects(30);

    private Poll                m_poll = null;

    /* misc */
    private final static int    MAP_DONOTCROSSLINE_Y = 7474;
    private final static long   OUTSIDE_TIME_LIMIT = 15000; //15 seconds
    private final static long   OUTSIDE_TIME_WARN = 10000;
    private final static int    MAX_OUTSIDE_TIME_INC = 3000;
    public final static String COUNTDOWN_OBJID  = "2758";
    private final static int    MAX_NAMELENGTH = 18;
    private final static int    SCOREBOARD_CHECK_PERIOD = 5000;

    /* player spectating */
    private int                 m_curSpecID = -1;
    private IntQueue            m_watchQueue = new IntQueue();
    private final static int    SPEC_PACKET_SEND_PERIOD = 200; //how often to send spec packet in ms
    private final static int    SPEC_SWITCH_PERIOD = 1000;

    /* delays for each step in pregame and in starting final round in milliseconds */
    private final static int    DELAY_ENTER = 5000;
    private final static int    DELAY_LOCK  = 30000;
    public final static int    DELAY_PRIZE = 31000;
    private final static int    DELAY_SETUP = 34000;
    private final static int    DELAY_COUNTDOWN = 36000;
    private final static int    DELAY_GOGOGO = 40000;
    private final static int    DELAY_SPECTATE = 40500;
    private final static int    DELAY_EXTEND = 60000; //added to delays if team game to allow arranging teams

    public final static int    DELAY_FINAL_PRIZE = 10000;
    private final static int    DELAY_FINAL_SETUP = 15000;
    private final static int    DELAY_FINAL_COUNTDOWN = 21000;
    private final static int    DELAY_FINAL_GOGOGO = 25000;

    /* team grouping markers for scoresheet */
    private final static char   BEG_MARK = '/';
    private final static char   MID_MARK = '|';
    private final static char   END_MARK = '\\';

    private final static int    SPAWN_CIRCLE0_X = 523;
    private final static int    SPAWN_CIRCLE0_Y = 267;
    private final static int    SPAWN_CIRCLE1_X = 523;
    private final static int    SPAWN_CIRCLE1_Y = 287;
    private final static int    SPAWN_CIRCLE2_X = 501;
    private final static int    SPAWN_CIRCLE2_Y = 287;
    private final static int    SPAWN_CIRCLE3_X = 501;
    private final static int    SPAWN_CIRCLE3_Y = 267;


    private final static String[] help_player = {
        "Welcome to Javelim!",
        "Rules: Each base has a separate deathmatch. The survivors of each base then battle",
        "eachother, carrying over their deaths from the first round. Games can be teams or solo.",
        "-- Help --------",
        " !lagout  Return to game.",
        " !spec    Leave game.",
        " !status  Displays current status."
    };

    private final static String[] help_staff = {
        "-- Staff Help --",
        " !start <options>  Starts game. Optional parameters:",
        "                   <number>     a number to specify deaths (eg. !start 3). Default=10",
        "                   <teams=num>  team size (eg. !start teams=2). Default=1 (solo)",
        " !stop             Cancels a game.",
        " !die              Shuts down bot.",
        " !startinfo        Tells who started a game.",
        " !reset            Resets arena (in case bot died).",
        " !remove <name>    Removes player from a game (must specify entire name)."
    };

    private final static String[] help_smod = {
        "-- SMod Help ---",
        " !addstaff <name>  Grant access to bot.",
        " !delstaff <name>  Remove access to bot.",
        " !accesslist       Display access list.",
        " !purge <days>     Purge database of players more than <days> inactive."
    };

    /* warp coords, formula:
     * x = m_safeCoords[freq >> 2 << 1] + m_addToX[freq % 4];
     * y = m_safeCoords[freq >> 2 << 1 + 1]; */
    private final static short[] m_addToX = {
        0, 182, 546, 728
    };
    private final static short[] m_safeCoords = {
         78, 344, //1
        219, 452, //2
        218, 344, //3
         77, 452, //4
        148, 331, //5
        148, 450, //6
        122, 437, //7
        173, 437, //8
        109, 330, //9
        187, 330, //10
         67, 385, //11
        229, 385, //12
         89, 416, //13
        207, 416, //14
        148, 486, //15
        148, 493  //16
    };
    private final static int[] m_goCoords = {
         78, 348, //1
        219, 448, //2
        218, 348, //3
         77, 448, //4
        148, 335, //5
        148, 446, //6
        122, 433, //7
        173, 433, //8
        109, 334, //9
        187, 334, //10
         71, 385, //11
        225, 385, //12
         93, 416, //13
        203, 416, //14
        148, 482, //15
        148, 497  //16
    };

    /* x = m_finalSafeCoords[freq % 4 * 2];
     * y = m_finalSafeCoords[freq % 4 * 2 + 1]; */
    private final static short[] m_finalSafeCoords = {
        431, 385,
        512, 331,
        512, 450,
        593, 385
    };
    private final static int[] m_finalGoCoords = {
        435, 385,
        512, 335,
        512, 446,
        589, 385
    };


    /** Creates a new instance of kimbot **/
    public javelim(BotAction botAction) {
        super(botAction);
        m_botAction = botAction;
        requestEvents();

        // m_botSettings contains the data specified in file <botname>.cfg
        m_botSettings = m_botAction.getBotSettings();
        m_operatorList = m_botAction.getOperatorList();

        m_connectionName = m_botSettings.getString("DBConnection");
        String str = m_botSettings.getString("AccessList");
        if(str != null) {
            String[] names = str.split(":", 0);
            for(int i = 0; i < names.length; i++) {
                m_access.add(names[i].toLowerCase());
            }
        }
    }

    public boolean isIdle() {
        if ( m_state.isMidGame() || m_state.isMidGameFinal() )
            return false;
        return true;
    }


    private void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.ARENA_JOINED);
        req.request(EventRequester.PLAYER_ENTERED);
        req.request(EventRequester.PLAYER_POSITION);
        req.request(EventRequester.PLAYER_LEFT);
        req.request(EventRequester.PLAYER_DEATH);
        //req.request(EventRequester.FREQUENCY_CHANGE);
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(EventRequester.LOGGED_ON);
    }


    public void handleEvent(LoggedOn event) {
        m_botAction.stopReliablePositionUpdating();
        m_botAction.setMessageLimit(8);
        m_botAction.sendUnfilteredPublicMessage("?chat=" + m_botSettings.getString("chat"));
        m_botAction.joinArena(m_botSettings.getString("arena"));
    }


    public void handleEvent(ArenaJoined event) {
        m_botAction.setReliableKills(1);
    }


    public void handleEvent(PlayerEntered event) {
        String name = event.getPlayerName();
        int id = event.getPlayerID();

        m_lvz.turnOn(id);

        if(name.length() > MAX_NAMELENGTH) {
            m_botAction.sendPrivateMessage(id, "NOTICE: Your name is too long. Use a shorter name (18 or less characters) to be able to play.");
            if(event.getShipTypeRaw() != 8) {
                m_botAction.specWithoutLock(id);
            }
            return;
        }

        name = name.toLowerCase();
        if(((m_state.isStarting() || m_state.isStartingFinal()) && m_startingLagouts.contains(name))
        || ((m_state.isMidGame() || m_state.isMidGameFinal()) && m_lagoutMan.contains(name))) {
            m_botAction.sendPrivateMessage(id, "PM me with !lagout to return.");
        }
    }


    private void putPlayerInGame(int id, KimPlayer kp, boolean disableWarping) {
        kp.resetTime();
        kp.m_timeLastPosUpdate = System.currentTimeMillis();

        m_botAction.setShip(id, Tools.Ship.SHARK);
        m_botAction.setFreq(id, kp.m_freq);
        m_botAction.setShip(id, Tools.Ship.JAVELIN);

        if(disableWarping) {
            m_botAction.sendUnfilteredPrivateMessage(id, "*prize #-1");
            m_botAction.sendUnfilteredPrivateMessage(id, "*prize #-13");
        }

        m_kpMap.put(id, kp);
        m_watchQueue.add(id);

        m_startingLagouts.remove(kp.m_lcname);
        m_startingReturns.remove(kp);
        m_lagoutMan.remove(kp.m_lcname);
    }


    public void handleEvent(Message event) {
        //System.out.println("(" + event.getPlayerID() + ":" + event.getMessager() + ")(" + Integer.toHexString(event.getMessageType()) + ":" + event.getSoundCode() + "!) " + event.getMessage());

        if(m_ensureLock && event.getMessageType() == Message.ARENA_MESSAGE && event.getMessage().equals("Arena UNLOCKED")) {
            m_botAction.toggleLocked();
            return;
        }

        if(event.getMessageType() != Message.PRIVATE_MESSAGE) {
            return;
        }

        int id = event.getPlayerID();
        String name = m_botAction.getPlayerName(id);
        String msg = event.getMessage().trim().toLowerCase();
        if(name == null || msg == null || msg.length() == 0) {
            return;
        }
        String lcname = name.toLowerCase();

        boolean isSmod = m_operatorList.isSmod(lcname);
        boolean hasAccess = isSmod || m_access.contains(lcname);

        /* public pm commands */
        //!help
        if(msg.equals("!help")) {
            m_botAction.privateMessageSpam(id, help_player);
            if(hasAccess) {
                m_botAction.privateMessageSpam(id, help_staff);
            }
            if(isSmod) {
                m_botAction.privateMessageSpam(id, help_smod);
                m_botAction.sendPrivateMessage(id, "Database: " + (m_botAction.SQLisOperational() ? "online" : "offline"));
            }

        //!status
        } else if(msg.equals("!status")) {
            cmdStatus(id);

        //!spec
        } else if(msg.equals("!spec")) {
            if(m_state.isStarting() || m_state.isStartingFinal()) {
                m_botAction.specWithoutLock(id);
            }

        //!about
        } else if(msg.equals("!about")) {
            m_botAction.sendPrivateMessage(id, "KimBot! (2007-12-15) by flibb <ER>", 7);

        //!lagout/!return
        } else if(msg.equals("!lagout") || msg.equals("!return")) {
            KimPlayer kp = m_allPlayers.get(lcname);
            if(kp == null) {
                return;
            }

            if(!kp.m_isOut) {
                synchronized(m_state) {
                    if((m_state.isMidGame() || m_state.isMidGameFinal()) && m_lagoutMan.remove(lcname)) {
                        putPlayerInGame(id, kp, false);
                    } else if((m_state.isStarting() || m_state.isStartingFinal()) && m_startingLagouts.remove(lcname)) {
                        m_startingReturns.add(kp);
                        m_botAction.sendPrivateMessage(id, "You will be put in at the start of the game.");
                    }
                }
            } else {
                m_botAction.sendPrivateMessage(id, "Cannot return to game.");
            }

        //poll vote
        } else if(Tools.isAllDigits(msg)) {
            if(m_poll != null) {
                m_poll.handlePollCount(id, name, msg);
            }

        /* staff commands */
        }else if(hasAccess) {
        //!start
            if(msg.equals("!start")) {
                m_deathsToSpec = 10;
                m_maxTeamSize = 1;
                cmdStart(id);
            } else if(msg.startsWith("!start ")) {
                String[] params = msg.substring(7).split(" ");
                m_deathsToSpec = 10;
                m_maxTeamSize = 1;
                for(String p : params) {
                    try {
                        if(Tools.isAllDigits(p)) {
                            m_deathsToSpec = Integer.parseInt(p);
                        } else if(p.startsWith("teams=")) {
                            m_maxTeamSize = Integer.parseInt(p.substring(6));
                        }
                    } catch(NumberFormatException e) {
                        m_botAction.sendPrivateMessage(id, "Can't understand: " + p);
                        return;
                    }
                }

                m_deathsToSpec = Math.max(1, Math.min(99, m_deathsToSpec));
                m_maxTeamSize = Math.max(1, Math.min(99, m_maxTeamSize));
                cmdStart(id);
        //!stop
            } else if(msg.equals("!stop")) {
                cmdStop(id);
        //!die
            } else if(msg.equals("!die")) {
                if(m_state.isStopped()) {
                    m_lvz.clear();
                    try {
                        Thread.sleep(3000);
                    } catch(InterruptedException e) {}
                    m_botAction.die();
                } else {
                    m_botAction.sendPrivateMessage(id, "A game is in progress. Use !stop first.");
                }
        //!startinfo
            } else if(msg.equals("!startinfo")) {
                if(m_state.isStopped() || m_startedBy == null) {
                    m_botAction.sendPrivateMessage(id, "No one started a game.");
                } else {
                    m_botAction.sendPrivateMessage(id, "Game started by " + m_startedBy);
                }
        //!reset
            } else if(msg.equals("!reset")) {
                if(m_state.isStopped()) {
                    resetArena();
                    m_botAction.sendPrivateMessage(id, "Resetted.");
                } else {
                    m_botAction.sendPrivateMessage(id, "A game is in progress. Use !stop first.");
                }
        //!remove
            } else if(msg.startsWith("!remove ")) {
                synchronized(m_state) {
                    if(m_state.isStopped()) {
                        m_botAction.sendPrivateMessage(id, "The game is not running.");
                    } else {
                        cmdRemove(id, msg.substring(8));
                    }
                }
        //!test
            } else if(msg.startsWith("!test")) {
                m_botAction.sendArenaMessage(Tools.addSlashes("test'test\"test\\test"));

            } else if(isSmod) {
        //!addstaff
                if(msg.startsWith("!addstaff ")) {
                    String addname = msg.substring(10);
                    if(addname.length() <= 1 || addname.indexOf(':') >= 0) {
                        m_botAction.sendPrivateMessage(id, "Invalid name. Access list not changed.");
                    } else if(addname.length() > MAX_NAMELENGTH) {
                        m_botAction.sendPrivateMessage(id, "Name too long. Max. 18 characters.");
                    } else if(m_access.add(msg.substring(10))) {
                        updateAccessList(id);
                    } else {
                        m_botAction.sendPrivateMessage(id, "That name already has access.");
                    }
        //!delstaff
                } else if(msg.startsWith("!delstaff ")) {
                    if(m_access.remove(msg.substring(10))) {
                        updateAccessList(id);
                    } else {
                        m_botAction.sendPrivateMessage(id, "Name not found.");
                    }
        //!accesslist
                } else if(msg.equals("!accesslist")) {
                    for(String s : m_access) {
                        m_botAction.sendPrivateMessage(id, s);
                    }
                    m_botAction.sendPrivateMessage(id, "End of list.");
        //!purge
                } else if(msg.equals("!purge")) {
                    cmdPurge(id, 90);
                } else if(msg.startsWith("!purge ")) {
                    try {
                        cmdPurge(id, Integer.parseInt(msg.substring(7)));
                    } catch(NumberFormatException e) {
                        m_botAction.sendPrivateMessage(id, "Nothing done. Check your typing.");
                    }
                }
            }
        }
    }


    private void updateAccessList(int id) {
        StringBuilder sb = new StringBuilder(100);
        for(String s : m_access) {
            sb.append(s).append(':');
        }
        m_botSettings.put("AccessList", sb.toString());
        if(m_botSettings.save()) {
            m_botAction.sendPrivateMessage(id, "Access list updated.");
        } else {
            m_botAction.sendPrivateMessage(id, "Couldn't save to file.");
        }
    }


    public void handleEvent(PlayerDeath event) {
        if(m_state.isMidGame() || m_state.isMidGameFinal()) {
            int victimID = event.getKilleeID();

            KimPlayer killer = m_kpMap.get(event.getKillerID());
            KimPlayer victim = m_kpMap.get(victimID);

            if(killer != null) {
                if(victim != null && victim.m_freq != killer.m_freq) {
                    killer.m_kills++;
                    m_mvp = mvpCompare(m_mvp, killer);

                    Integer key = Integer.valueOf(killer.m_deaths);
                    KimPlayer kp = m_deathMap.get(key);
                    if(kp == null || mvpCompare(killer, kp) == killer) {
                        m_deathMap.put(key, killer);
                    }
                }
            }

            if(victim != null) {
                m_watchQueue.sendToBack(victimID);
                victim.resetTime();
                int oldDeaths = victim.m_deaths++;

                Integer key = Integer.valueOf(oldDeaths);
                KimPlayer kp = m_deathMap.get(key);
                if(kp == victim) {
                    m_deathMap.remove(key);
                }
                key = Integer.valueOf(oldDeaths + 1);
                kp = m_deathMap.get(key);
                if(kp == null || mvpCompare(victim, kp) == victim) {
                    m_deathMap.put(key, victim);
                }

                if(!victim.m_isOut && victim.m_deaths >= m_deathsToSpec) {
                    removePlayerAndCheck(victim, null);
                } else {
                    m_botAction.sendPrivateMessage(victimID, '(' + victim.m_kills + '-' + victim.m_deaths
                        + ") Remaining lives: " + (m_deathsToSpec - victim.m_deaths));
                }
            }
        }
    }


    public void handleEvent(PlayerPosition event) {
        synchronized(m_state) {
            if(m_state.isMidGame() || m_state.isMidGameFinal()) {
                long curTime = System.currentTimeMillis();
                int id = event.getPlayerID();
                KimPlayer kp = m_kpMap.get(id);
                m_watchQueue.sendToBack(id);
                if(kp == null || (m_state.isMidGame() && isSurvivor(kp))) {
                    return;
                }
                if(!kp.m_isOut && event.getYLocation() > MAP_DONOTCROSSLINE_Y) {
                    kp.m_timeOutside += Math.min(MAX_OUTSIDE_TIME_INC, curTime - kp.m_timeLastPosUpdate);
                    if(kp.m_timeOutside > OUTSIDE_TIME_LIMIT) {
                        if(++kp.m_deaths >= m_deathsToSpec) {
                            removePlayerAndCheck(kp, "too long outside base");
                            return;
                        } else {
                            kp.resetTime();
                            m_botAction.sendArenaMessage(kp.m_name + " recieves +1 death for being outside base too long.");
                        }
                    } else if(kp.m_notWarnedYet && kp.m_timeOutside > OUTSIDE_TIME_WARN) {
                        m_botAction.sendPrivateMessage(id, "Warning: spending too much time outside base will cost 1 life.");
                        kp.m_notWarnedYet = false;
                    }
                }
                kp.m_timeLastPosUpdate = curTime;
            }
        }
    }


    public void handleEvent(FrequencyShipChange event) {
        synchronized(m_state) {
            int id = event.getPlayerID();
            if(event.getShipTypeRaw() != 8) {
                if(m_botAction.getPlayerName(id).length() > MAX_NAMELENGTH) {
                    m_botAction.sendPrivateMessage(id, "Notice: Your player name is too long. Use a shorter name (18 or less characters) to be able to play.");
                    m_botAction.specWithoutLock(id);
                }
                return;
            }

            //else changed to spec
            lagoutHelper(event.getPlayerID());
        }
    }


    public void handleEvent(PlayerLeft event) {
        synchronized(m_state) {
            lagoutHelper(event.getPlayerID());
        }
    }


    @SuppressWarnings("fallthrough")
    private void lagoutHelper(int playerID) {
        KimPlayer kp = m_kpMap.remove(playerID);
        if(kp == null || kp.m_isOut) {
            return;
        }

        switch(m_state.getState()) {
            case State.STARTING:
            case State.STARTING_FINAL:
                m_startingLagouts.add(kp.m_lcname);
                break;
            case State.MIDGAME:
                if(isSurvivor(kp)) {
                    m_startingLagouts.add(kp.m_lcname);
                    break;
                }
            case State.MIDGAME_FINAL:
                registerLagout(kp);
                break;
        }

        m_watchQueue.remove(playerID);
    }


    private boolean isSurvivor(KimPlayer kp) {
        if(!kp.m_isOut) {
            KimTeam team = m_survivingTeams[kp.m_freq % 4];
            return (team != null && team.contains(kp));
        }
        return false;
    }


    private void registerLagout(KimPlayer kp) {
        if(kp.m_lagoutsLeft > 0) {
            m_lagoutMan.add(kp.m_lcname);
            kp.m_lagoutsLeft--;
            m_botAction.sendSmartPrivateMessage(kp.m_name, "PM me with !lagout to return within 30 seconds. You have " + kp.m_lagoutsLeft + " lagout(s) left.");
        } else {
            removePlayerAndCheck(kp, "too many lagouts");
        }
    }


    public void handleExpiredLagout(String lcname) {
        KimPlayer kp = m_allPlayers.get(lcname);
        if(kp != null && !kp.m_isOut && (m_state.isMidGame() || m_state.isMidGameFinal())) {
            removePlayerAndCheck(kp, "failed to return from lagout");
        }
    }


    private void cmdStart(int id) {
        if(!m_state.isStopped()) {
            m_botAction.sendPrivateMessage(id, "A game is in progress. Use !stop first.");
            return;
        }

        m_state.setState(State.PREGAME);
        cleanUp();
        m_startedBy = m_botAction.getPlayerName(id);
        m_isTeams = m_maxTeamSize > 1;
        int delayAdjuster = m_isTeams ? DELAY_EXTEND : 0;

        m_botAction.arenaMessageSpam(m_title[(int)(System.currentTimeMillis() % m_title.length)]);

        m_botAction.sendArenaMessage("[RULES] Four games will take place simultaneously in separate bases. The survivors", 2);
        m_botAction.sendArenaMessage("_       of each base will then battle eachother, carrying over their deaths.");
        m_botAction.sendArenaMessage("[TEAMS] " + (m_maxTeamSize > 1 ? "Teams of " + m_maxTeamSize : "No teams"));
        m_botAction.sendArenaMessage("[LIVES] " + m_deathsToSpec + " deaths and you're out.");

        m_botAction.scheduleTask(new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage("Enter if playing.");
                if(m_isTeams) {
                    m_botAction.sendArenaMessage("You have " + ((DELAY_LOCK + DELAY_EXTEND) / 1000) + " seconds to arrange teams.");
                } else {
                    m_botAction.sendArenaMessage("Locking in " + (DELAY_LOCK / 1000) + " seconds.");
                }
            }
        }, DELAY_ENTER);

        //lock arena
        m_botAction.scheduleTask(new TimerTask() {
            public void run() {
                m_state.setState(State.STARTING);
                m_botAction.sendArenaMessage("Game starts in 10 seconds.", 1);
                m_ensureLock = true;
                m_botAction.toggleLocked();
            }
        }, DELAY_LOCK + delayAdjuster);

        //set up players, warp to safes
        m_botAction.scheduleTask(new TimerTask() {
            public void run() {
                int maxPlayers = 64;
                if(m_isTeams)
                {
                    maxPlayers = 64 * m_maxTeamSize;
                }
                List<Player> players = m_botAction.getPlayingPlayers();

                if(players.size() < 4 * m_maxTeamSize) {
                    m_botAction.sendArenaMessage("Not enough players. Minimum of " + (4 * m_maxTeamSize) + " players needed.");
                    cmdStop(-1);
                    return;
                }

                setBaseSpawns();

                if(!m_isTeams) {
                    Collections.shuffle(players);
                }
                HashMap<Integer, KimTeam>   teams = new HashMap<Integer, KimTeam>(80);
                LinkedList<KimPlayer>       needsTeam = new LinkedList<KimPlayer>();
                int count = 0;
                for(Player player : players) {
                    count++;
                    int id = player.getPlayerID();

                    if(count > maxPlayers) {
                        m_botAction.specWithoutLock(id);
                        m_botAction.sendPrivateMessage(id, "Sorry, too many players.");
                        continue;
                    }

                    String name = player.getPlayerName();
                    Integer freq = new Integer(player.getFrequency());

                    KimPlayer kp = new KimPlayer(name, 999);
                    m_allPlayers.put(kp.m_lcname, kp);

                    KimTeam team = teams.get(freq);
                    if(team == null) {
                        team = new KimTeam(m_maxTeamSize);
                        team.add(kp);
                        teams.put(freq, team);
                    } else if(team.size() < m_maxTeamSize) {
                        team.add(kp);
                    } else {
                        needsTeam.add(kp);
                    }

                    m_kpMap.put(id, kp);
                    m_watchQueue.add(id);
                }

                Iterator<KimTeam> iter = teams.values().iterator();
                while(iter.hasNext()) {
                    KimTeam team = iter.next();
                    if(team.size() <= 1) {
                        needsTeam.addAll(team.getPlayers());
                        iter.remove();
                    }
                }

                int freq = 0;
                for(KimTeam team : teams.values()) {
                    if(team.size() + needsTeam.size() >= m_maxTeamSize) {
                        while(team.size() < m_maxTeamSize) {
                            team.add(needsTeam.remove());
                        }
                        team.setFreq(freq);
                        m_teams[freq] = team;
                        m_groupCount[freq % 4]++;
                        freq++;
                    } else {
                        needsTeam.addAll(team.getPlayers());
                    }
                }

                while(!needsTeam.isEmpty()) {
                    KimTeam tempTeam = new KimTeam(m_maxTeamSize);
                    while(tempTeam.size() < m_maxTeamSize && !needsTeam.isEmpty()) {
                        tempTeam.add(needsTeam.remove());
                    }
                    tempTeam.setFreq(freq);
                    m_teams[freq] = tempTeam;
                    m_groupCount[freq % 4]++;
                    freq++;
                }

                m_numTeams = freq;

                for(KimPlayer kp : m_allPlayers.values()) {
                    int id = m_botAction.getPlayerID(kp.m_name);
                    int kpFreq = kp.m_freq;
                    m_botAction.setShip(id, Tools.Ship.JAVELIN);
                    m_botAction.setFreq(id, kpFreq);
                    m_botAction.sendUnfilteredPrivateMessage(id, "*prize #-1");
                    m_botAction.sendUnfilteredPrivateMessage(id, "*prize #-13");
                    m_botAction.warpTo(id, m_safeCoords[(kpFreq >> 2) << 1] + m_addToX[kpFreq % 4], m_safeCoords[((kpFreq >> 2) << 1) + 1]);
                }

                m_skipFirstRound = m_numTeams <= 4;
            }
        }, DELAY_SETUP + delayAdjuster);


        //countdown lvz object
        m_botAction.scheduleTask(new CountdownImage(), DELAY_COUNTDOWN + delayAdjuster);


        //go go go
        m_botAction.scheduleTask(new TimerTask() {
            public void run() {
                synchronized(m_state) {
                    m_state.setState(State.MIDGAME);
                    m_botAction.scoreResetAll();
                    if(!m_skipFirstRound) {
                        m_botAction.sendArenaMessage("GO! GO! GO!", 104);

                        for(KimPlayer kp : m_allPlayers.values()) {
                            if(m_startingLagouts.remove(kp.m_lcname)) {
                                registerLagout(kp);
                            } else {
                                if(m_startingReturns.remove(kp)) {
                                    int retID = m_botAction.getPlayerID(kp.m_name);
                                    if(retID >= 0) {
                                        putPlayerInGame(retID, kp, true);
                                    } else {
                                        registerLagout(kp);
                                        continue;
                                    }
                                }
                                m_botAction.warpTo(kp.m_name, m_goCoords[(kp.m_freq >> 2) << 1] + m_addToX[kp.m_freq % 4], m_goCoords[((kp.m_freq >> 2) << 1) + 1]);
                            }
                        }
                        m_botAction.shipResetAll();
                    } else {
                        m_botAction.sendArenaMessage("Skipping first round.");
                        m_botAction.scheduleTask(new TimerTask() {
                            public void run() {
                                startFinalRound();
                            }
                        }, 5000);
                    }

                    for(int i = 0; i < 4; i++) {
                        if(m_groupCount[i] == 1) {
                            setSurvivingTeam(m_teams[i]);
                        }
                    }
                }
            }
        }, DELAY_GOGOGO + delayAdjuster);


        //spectating tasks
        m_botAction.scheduleTask(new TimerTask() {
            public void run() {
                m_botAction.spectatePlayer(m_curSpecID);
            }
        }, DELAY_SPECTATE + delayAdjuster, SPEC_PACKET_SEND_PERIOD);

        m_botAction.scheduleTask(new TimerTask() {
            public void run() {
                m_curSpecID = m_watchQueue.getAndSendToBack();
            }
        }, DELAY_SPECTATE + delayAdjuster, SPEC_SWITCH_PERIOD);


        //scoreboard checking task
        m_botAction.scheduleTask(new TimerTask() {
            public void run() {
                KimPlayer kp;
                synchronized(m_deathMap) {
                    try {
                        kp = m_deathMap.get(m_deathMap.firstKey());
                    } catch(NoSuchElementException nsee) { // m_deathMap is empty
                        kp = null;
                    }
                }
                if(m_mvp != m_prevMvp || m_survivor != kp) {
                    m_prevMvp = m_mvp;
                    m_survivor = kp;
                    refreshScoreboard();
                }
            }
        }, DELAY_GOGOGO + 5000, SCOREBOARD_CHECK_PERIOD);

    }


    private void cmdStop(int id) {
        if(m_state.isStopped()) {
            m_botAction.sendPrivateMessage(id, "I'm already stopped.");
            return;
        }
        m_state.setState(State.STOPPED);
        cleanUp();
        resetArena();
        String name = id < 0 ? m_botAction.getBotName() : m_botAction.getPlayerName(id);
        m_botAction.sendArenaMessage("This game was brutally killed by " + name, 13);
    }


    private void cmdPurge(int id, int days) {
        if(!m_state.isStopped()) {
            m_botAction.sendPrivateMessage(id, "Stop the game first.");
            return;
        }
        if(m_connectionName != null && m_connectionName.length() > 0 && m_botAction.SQLisOperational()) {
            m_botAction.sendPrivateMessage(id, "Attempting to purge scores inactive for " + days + " days or more.");
            try {
                m_botAction.SQLQueryAndClose(m_connectionName
                    , "DELETE FROM tblJavelim WHERE TO_DAYS(CURRENT_TIMESTAMP-ftLastUpdate) >= " + days);
                m_botAction.sendArenaMessage("Purge done.");
            } catch(SQLException e) {
                m_botAction.sendArenaMessage("Purge failed.");
                Tools.printLog(e.toString());
            }
        } else {
            m_botAction.sendArenaMessage("Not connected to database.");
        }
    }


    private void cmdStatus(int id) {
        switch(m_state.getState()) {
            case State.STOPPED:
                m_botAction.sendPrivateMessage(id, "Stopped.");
                break;
            case State.PREGAME:
                m_botAction.sendPrivateMessage(id, "Preparing a new game.");
                break;
            case State.STARTING:
                m_botAction.sendPrivateMessage(id, "Starting a game.");
                break;
            case State.MIDGAME:
                m_botAction.sendPrivateMessage(id, "Game in progress. Current lagouts:");
                for(String name : m_lagoutMan.getLaggers(new String[m_lagoutMan.size()])) {
                    if(name != null) {
                        m_botAction.sendPrivateMessage(id, name);
                    }
                }
                m_botAction.sendPrivateMessage(id, "End of list.");
                break;
            case State.STARTING_FINAL:
                m_botAction.sendPrivateMessage(id, "Starting final round.");
                break;
            case State.MIDGAME_FINAL:
                m_botAction.sendPrivateMessage(id, "Final round in progress. Survivors:");
                for(KimTeam team : m_survivingTeams) {
                    if(team != null) {
                        m_botAction.sendPrivateMessage(id, team.toString(true));
                    }
                }
                m_botAction.sendPrivateMessage(id, "End of list. Current lagouts:");
                for(String name : m_lagoutMan.getLaggers(new String[m_lagoutMan.size()])) {
                    if(name != null) {
                        m_botAction.sendPrivateMessage(id, name);
                    }
                }
                m_botAction.sendPrivateMessage(id, "End of list.");
                break;
            case State.ENDING_GAME:
                m_botAction.sendPrivateMessage(id, "Ending game.");
                break;
        }
    }


    private void cmdRemove(int id, String nameToRemove) {
        KimPlayer kp = m_allPlayers.get(nameToRemove);
        if(kp != null) {
            removePlayerAndCheck(kp, "removed from game");
        } else {
            m_botAction.sendPrivateMessage(id, "Player not found.");
        }
    }


    private void cleanUp() {
        m_botAction.cancelTasks();
        m_allPlayers.clear();
        m_lagoutMan.clear();
        m_startingLagouts.clear();
        m_startingReturns.clear();
        m_watchQueue.clear();
        m_kpMap.clear();
        m_deathMap.clear();

        Arrays.fill(m_survivingTeams, null);
        Arrays.fill(m_groupCount, 0);
        Arrays.fill(m_teams, null);

        m_survivorCount = 0;
        m_poll = null;
        m_winner = null;
        m_mvp = null;

        m_botAction.stopSpectatingPlayer();
    }


    /**
     * ?set spawns so that players will spawn in the appropriate base based on freq
     */
    private void setBaseSpawns() {
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X:148");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y:517");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius:20");

        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-X:330");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Y:517");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Radius:20");

        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-X:694");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Y:517");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Radius:20");

        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-X:876");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Y:517");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Radius:20");

    }

    private void setFinalSpawns() {
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X:512");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y:517");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius:20");

        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-X:512");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Y:517");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Radius:20");

        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-X:512");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Y:517");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Radius:20");

        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-X:512");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Y:517");
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Radius:20");
    }


    /*
     * ?set spawns to the circles and unlock arena
     */
    private void resetArena() {
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-X:" + SPAWN_CIRCLE0_X);
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Y:" + SPAWN_CIRCLE0_Y);
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team0-Radius:1");

        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-X:" + SPAWN_CIRCLE1_X);
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Y:" + SPAWN_CIRCLE1_Y);
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team1-Radius:1");

        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-X:" + SPAWN_CIRCLE2_X);
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Y:" + SPAWN_CIRCLE2_Y);
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team2-Radius:1");

        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-X:" + SPAWN_CIRCLE3_X);
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Y:" + SPAWN_CIRCLE3_Y);
        m_botAction.sendUnfilteredPublicMessage("?set Spawn:Team3-Radius:1");

        if(m_ensureLock) {
            m_ensureLock = false;
            m_botAction.toggleLocked();
        }
    }


    private void removePlayerAndCheck(KimPlayer kp, String reason) {
        kp.m_isOut = true;
        kp.m_deaths = m_deathsToSpec;
        m_lagoutMan.remove(kp.m_lcname);
        m_startingLagouts.remove(kp.m_lcname);
        m_startingReturns.remove(kp);
        m_botAction.specWithoutLock(kp.m_name);
        int freq = kp.m_freq;
        final int group = freq % 4;
        KimTeam team = m_teams[freq];

        m_botAction.sendArenaMessage(kp.m_name + " is out"
            + (reason == null ? ". " : " (" + reason + "). ")
            + kp.m_kills + " wins, " + kp.m_deaths + " losses.");

        if(team.isOut()) {
            m_groupCount[group]--;
        }

        //if this group is down to 1 team, add to survivors and announce
        if(m_state.isMidGame() && m_groupCount[group] == 1) {
            m_botAction.scheduleTask(new TimerTask() {
                //a local copy of group is automatically provided here
                public void run() {
                    for(int i = group; i < 64; i += 4) {
                        if(!m_teams[i].isOut()) {
                            setSurvivingTeam(m_teams[i]);
                            return;
                        }
                    }
                    m_botAction.sendArenaMessage("No one won for Base " + (group + 1));
                }
            }, 4000);

            //check if we need to start final round
            if(m_groupCount[0] <= 1 && m_groupCount[1] <= 1 && m_groupCount[2] <= 1 && m_groupCount[3] <= 1) {
                m_botAction.scheduleTask(new TimerTask() {
                    public void run() {
                        startFinalRound();
                    }
                }, 8000);
            }

        } else if(m_state.isMidGameFinal()) {
            //check if exactly 1 team left
            if(m_groupCount[0] + m_groupCount[1] + m_groupCount[2] + m_groupCount[3] == 1) {
                m_botAction.scheduleTask(new TimerTask() {
                    public void run() {
                        endGame();
                    }
                }, 4000);
            }
        }
    }


    private void setSurvivingTeam(KimTeam team) {
        if(team == null) {
            throw new RuntimeException("setSurvivingTeam() called on null team");
        }

        int group = team.m_freq % 4;

        if(m_survivingTeams[group] != null) {
            throw new RuntimeException("tried to set another surviving team prev freq:"
                + m_survivingTeams[group].m_freq + " new freq:" + team.m_freq);
        }

        m_survivingTeams[group] = team;
        m_survivorCount++;

        for(KimPlayer kp : team) {
            m_lagoutMan.remove(kp.m_lcname);
            if(!m_skipFirstRound) {
                m_botAction.sendPrivateMessage(kp.m_name, "You may spec and !return for the final round.");
            }
        }

        if(!m_skipFirstRound) {
            m_botAction.sendArenaMessage("Base " + (group + 1) + " winner: " + team.toString(true));
        }
    }


    /* starts the final round (only call if 1 or less teams in each group) */
    private void startFinalRound() {
        if(!m_state.isMidGame()) {
            throw new RuntimeException("startFinalRound() called, but not midgame");
        }

        m_state.setState(State.STARTING_FINAL);

        if(m_survivorCount == 0) {
            m_botAction.sendArenaMessage("No survivors?! \\(o_O)/", 24);
            endGame();
            return;
        } else if(m_survivorCount == 1) {
            for(KimTeam team : m_survivingTeams) {
                if(team != null) {
                    m_botAction.sendArenaMessage("Winner by default: " + team.toString());
                    break;
                }
            }
            endGame();
            return;
        }

        //display survivors
        m_botAction.sendArenaMessage("-------------------------------------------------------------------------------------------------------");
        printFormattedTeams(m_survivingTeams, 4);
        m_botAction.sendArenaMessage("-------------------------------------------------------------------------------------------------------");
        m_botAction.sendArenaMessage("Welcome to the Final Round!", 2);

        //pm leavers
        for(String name : m_startingLagouts) {
            if(name != null) {
                m_botAction.sendSmartPrivateMessage(name, "Final round is starting. You have 15 seconds to return.");
            }
        }

        //warp to final safes and ?set spawns
        m_botAction.scheduleTask(new TimerTask() {
            public void run() {
                for(int i = 0; i < 4; i++) {
                    KimTeam team = m_survivingTeams[i];
                    if(team != null) {
                        for(KimPlayer kp : team) {
                            kp.resetTime();
                            if(m_startingReturns.remove(kp)) {
                                int retID = m_botAction.getPlayerID(kp.m_name);
                                if(retID < 0) {
                                    m_startingLagouts.add(kp.m_lcname);
                                    continue;
                                } else {
                                    putPlayerInGame(retID, kp, true);
                                }
                            }
                            m_botAction.warpTo(kp.m_name, m_finalSafeCoords[i << 1], m_finalSafeCoords[(i << 1) + 1]);
                        }
                    }
                }
                setFinalSpawns();
            }
        }, DELAY_FINAL_SETUP);

        //countdown lvz object
        m_botAction.scheduleTask(new CountdownImage(), DELAY_FINAL_COUNTDOWN);

        //go go go
        m_botAction.scheduleTask(new TimerTask() {
            public void run() {
                synchronized(m_state) {
                    m_state.setState(State.MIDGAME_FINAL);
                    m_botAction.sendArenaMessage("GO! GO! GO!", 104);
                    for(int i = 0; i < 4; i++) {
                        KimTeam team = m_survivingTeams[i];
                        for(KimPlayer kp : team) {
                            if(kp != null) {
                                if(m_startingLagouts.remove(kp.m_lcname)) {
                                    registerLagout(kp);
                                    continue;
                                }
                                if(m_startingReturns.remove(kp)) {
                                    int retID = m_botAction.getPlayerID(kp.m_name);
                                    if(retID < 0) {
                                        registerLagout(kp);
                                        continue;
                                    } else {
                                        putPlayerInGame(retID, kp, true);
                                    }
                                } else {
                                    kp.resetTime();
                                }
                                m_botAction.warpTo(kp.m_name, m_finalGoCoords[i << 1], m_finalGoCoords[(i << 1) + 1]);
                            }
                        }
                    }
                    m_botAction.shipResetAll();
                    m_poll = new Poll(m_survivingTeams, m_botAction);
                }
            }
        }, DELAY_FINAL_GOGOGO);
    }


    private void endGame() {
        /*
        "105 max chars                                                                                           ."
        "--Base1--------------W--L --Base2--------------W--L --Base3--------------W--L --Base4--------------W--L"
        "123456789012345678 123 12|123456789012345678 123 12|123456789012345678 123 12|123456789012345678 123 12"
        "-------------------------|-------------------------|-------------------------|-------------------------"
        "123456789012345678 123 12|123456789012345678 123 12|123456789012345678 123 12|123456789012345678 123 12"
        */

        m_state.setState(State.ENDING_GAME);

        /* find winner */
        for(KimTeam team : m_survivingTeams) {
            if(team != null) {
                if(!team.isOut()) {
                    m_winner = team;
                }
                //remove survivors from m_teams so not to duplicate stats
                m_teams[team.m_freq] = null;
            }
        }

        /* end poll */
        if(m_poll != null) {
            m_poll.endPoll(m_winner, m_connectionName);
            m_poll = null;
        }

        /* display scoresheet */
        m_botAction.sendArenaMessage("--------Base1--------W--L --------Base2--------W--L --------Base3--------W--L --------Base4--------W--L");

        if(!m_skipFirstRound) {
            printFormattedTeams(m_teams, m_numTeams);
            m_botAction.sendArenaMessage("------------------------- ------------------------- ------------------------- -------------------------");
        }

        printFormattedTeams(m_survivingTeams, 4);
        m_botAction.sendArenaMessage("-------------------------------------------------------------------------------------------------------");

        if(m_winner != null) {
            m_botAction.sendArenaMessage("Winner: " + m_winner.toString(), 5);
            for(KimPlayer kp : m_winner) {
                Player p = m_botAction.getPlayer(kp.m_name);
                if(p != null && p.isPlaying()) {
                    m_botAction.warpTo(p.getPlayerID(), 512, 277);
                }
            }
        } else {
            m_botAction.sendArenaMessage("No winner.");
        }

        updateDB();

        /* announce mvp and clean up */
        m_botAction.scheduleTask(new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage("MVP: " + m_mvp.m_name, 7);
                //refreshScoreboard();
                cleanUp();
                resetArena();
                m_state.setState(State.STOPPED);
            }
        }, 4000);
    }


    private void printFormattedTeams(KimTeam[] teams, int maxNumberOfTeamsToPrint) {
        int numTeams = Math.min(maxNumberOfTeamsToPrint, teams.length);
        if(teams == null || numTeams < 1) {
            return;
        }
        boolean findMvp = m_state.isEndingGame();
        int idx[] = { 0, 1, 2, 3 };
        @SuppressWarnings("unchecked")
        Iterator<KimPlayer>[] iter = new Iterator[4];
        StringBuilder sb = new StringBuilder(105);
        Formatter formatter = new Formatter(sb);
        int num[] = new int[4];
        char mark[] = new char[4];

        for(;;) {
            int done = 0;
            sb.setLength(0);
            for(int i = 0; i < 4; i++) {
                while(idx[i] < numTeams && (iter[i] == null || !iter[i].hasNext())) {
                    if(teams[idx[i]] != null) {
                        iter[i] = teams[idx[i]].iterator();
                        num[i] = teams[idx[i]].size();
                        mark[i] = num[i] > 1 ? BEG_MARK : i == 0 ? '_' : ' ';
                    } else {
                        iter[i] = null;
                    }
                    idx[i] += 4;
                }
                if(iter[i] == null || !iter[i].hasNext()) {
                    if(i == 0) {
                        sb.append("_                         ");
                    } else if(i < 3) {
                        sb.append("                          ");
                    }
                    done++;
                    continue;
                }
                KimPlayer kp = iter[i].next();
                formatter.format("%1$c%2$-17.17s%3$4d%4$3d"
                    , mark[i], kp.m_name, Math.min(999, kp.m_kills), Math.min(99, kp.m_deaths));
                mark[i] = --num[i] == 1 ? END_MARK : MID_MARK;
                if(i < 3) {
                    sb.append(' ');
                }

                if(findMvp) {
                    m_mvp = mvpCompare(m_mvp, kp);
                }
            }
            if(done >= 4) {
                break;
            }
            m_botAction.sendArenaMessage(sb.toString());
        }
    }


    public StringBuilder padHelper(StringBuilder sb, String str, int width) {
        if(str.length() > width) {
            return sb.append(str.substring(0, width));
        }
        int pad = width - str.length();
        sb.append(str);
        for(int i = 0; i < pad; i++) {
            sb.append(' ');
        }
        return sb;
    }
    public StringBuilder padHelper(StringBuilder sb, int num, int width) {
        int digits = 1;
        if(num > 0) {
            digits = (int)Math.log10(num) + 1;
        } else if(num < 0) {
            digits = (int)Math.log10(-num) + 2;
        }
        if(digits > width) {
            for(int i = 0; i < width; i++) {
                sb.append('9');
            }
            return sb;
        }
        int pad = width - digits;
        for(int i = 0; i < pad; i++) {
            sb.append(' ');
        }
        return sb.append(num);
    }


    /**
     * Compares 2 KimPlayers for MVP. Returns the more valuable one.
     * @param kp1 first KimPlayer to compare
     * @param kp2 second KimPlayer to compare
     * @return the more valuable KimPlayer, always returns either kp1 or kp2
     */
    private KimPlayer mvpCompare(KimPlayer kp1, KimPlayer kp2) {
        if(kp1 == null) {
            return kp2;
        }
        if(kp2 == null) {
            return kp1;
        }

        if(kp1.m_kills < kp2.m_kills) {
            return kp2;
        }
        if(kp1.m_kills > kp2.m_kills) {
            return kp1;
        }

        if(kp1.m_deaths > kp2.m_deaths) {
            return kp2;
        }
        if(kp1.m_deaths < kp2.m_deaths) {
            return kp1;
        }

        Player p1 = m_botAction.getPlayer(kp1.m_name);
        Player p2 = m_botAction.getPlayer(kp2.m_name);

        if(p1 ==  null) {
            return kp2;
        }
        if(p2 == null) {
            return kp1;
        }

        if(p1.getKillPoints() < p2.getKillPoints()) {
            return kp2;
        }
        if(p1.getKillPoints() > p2.getKillPoints()) {
            return kp1;
        }

        if(kp1.m_lagoutsLeft < kp2.m_lagoutsLeft) {
            return kp2;
        }
        if(kp1.m_lagoutsLeft > kp2.m_lagoutsLeft) {
            return kp1;
        }

        return kp2;
    }


    private void refreshScoreboard() {
        m_lvz.clear();
        StringBuilder sb = new StringBuilder(30);
        Formatter formatter = new Formatter(sb);
        String mvpStr, survStr;
        int mvpKills = 0, mvpDeaths = 0, survKills = 0, survDeaths = 0;

        if(m_mvp == null) {
            mvpStr = "--";
        } else {
            mvpStr = m_mvp.m_lcname;
            mvpKills = m_mvp.m_kills;
            mvpDeaths = m_mvp.m_deaths;
        }
        mvpKills = Math.min(99, mvpKills);
        mvpDeaths = Math.min(99, mvpDeaths);

        if(m_survivor == null) {
            survStr = "--";
        } else {
            survStr = m_survivor.m_lcname;
            survKills = m_survivor.m_kills;
            survDeaths = m_survivor.m_deaths;
        }
        survKills = Math.min(99, survKills);
        survDeaths = Math.min(99, survDeaths);

        /* scoreboard string format:
         * 10--------2-2-10--------2-2-
         * mvpname...w.l.winner....w.l. */

        formatter.format("%1$-10.10s%2$2d%3$2d%4$-10.10s%5$2d%6$2d"
            , mvpStr, mvpKills, mvpDeaths, survStr, survKills, survDeaths);

        int ch;
        for(int i = 0; i < sb.length(); i++) {
            ch = (int)sb.charAt(i) & 0xff;
            if(ch != 32) {
                m_lvz.add(ch + i * 100);
            }
        }

        m_lvz.buildStrings();
        m_lvz.turnOn();
    }


    /**
     * Insert/Update scores in database
     */
    private void updateDB() {
        //if(!m_state.isEndingGame()) return;
        if(m_connectionName == null || m_connectionName.length() == 0 || !m_botAction.SQLisOperational()) {
            m_botAction.sendChatMessage("Database not connected. Not updating scores.");
            return;
        }
        for(KimPlayer kp : m_allPlayers.values()) {
            try{
            m_botAction.SQLQueryAndClose(m_connectionName
                , "INSERT INTO tblJavelim (fcName,fnGames,fnKills,fnDeaths) "
                + "VALUES ('" + Tools.addSlashes(kp.m_name) + "',1," + kp.m_kills + "," + kp.m_deaths + ") "
                + "ON DUPLICATE KEY UPDATE fnGames = fnGames + 1, fnKills = fnKills + VALUES(fnKills), fnDeaths = fnDeaths + VALUES(fnDeaths)");
            }catch(SQLException e){
                Tools.printStackTrace(e);
            }
        }

        if(m_mvp != null) {
            try{
            m_botAction.SQLQueryAndClose(m_connectionName
                , "UPDATE tblJavelim SET fnMVPs = fnMVPs + 1 WHERE fcName='" + Tools.addSlashes(m_mvp.m_name) + "'");
            }catch(SQLException e){
                Tools.printStackTrace(e);
            }
        }

        if(m_winner != null && m_winner.size() != 0) {
            for(KimPlayer kp : m_winner) {
                try{
                m_botAction.SQLQueryAndClose(m_connectionName
                    , "UPDATE tblJavelim SET fnWins= fnWins + 1 WHERE fcName='" + Tools.addSlashes(kp.m_name) + "'");
                }catch(SQLException e){
                    Tools.printStackTrace(e);
                }
            }
        }
    }


    private final class CountdownImage extends TimerTask {
        public void run() {
            if(m_skipFirstRound && m_state.isStarting()) {
                return;
            }
            m_botAction.sendUnfilteredPublicMessage("*objon 2758");
        }
    }


    private final class State {
        private int m_state;
        final static int UNDEFINED = -1;
        final static int STOPPED = 0;
        final static int PREGAME = 1;
        final static int STARTING = 2;
        final static int MIDGAME = 3;
        final static int STARTING_FINAL = 4;
        final static int MIDGAME_FINAL = 5;
        final static int ENDING_GAME = 6;

        State() {
            m_state = UNDEFINED;
        }

        State(int initialState) {
            m_state = initialState;
        }

        synchronized boolean isStopped() { return m_state == STOPPED; }
        synchronized boolean isPreGame() { return m_state == PREGAME; }
        synchronized boolean isStarting() { return m_state == STARTING; }
        synchronized boolean isMidGame() { return m_state == MIDGAME; }
        synchronized boolean isStartingFinal() { return m_state == STARTING_FINAL; }
        synchronized boolean isMidGameFinal() { return m_state == MIDGAME_FINAL; }
        synchronized boolean isEndingGame() { return m_state == ENDING_GAME; }

        synchronized void setState(int newState) { m_state = newState; }
        synchronized int getState() { return m_state; }
    }


    private final static String[][] m_title
        = {{ "_            _____ _   ________   ______  ___",
             "_        __ / / _ | | / / __/ /  /  _/  |/  /",
             "_       / // / __ | |/ / _// /___/ // /|_/ /",
             "_       \\___/_/ |_|___/___/____/___/_/  /_/" },

           { "_          __  __  _  _  ___  __    __  __  __",
             "_         (  )(  )( )( )(  _)(  )  (  )(  \\/  )",
             "_        __)( /__\\ \\\\//  ) _) )(__  )(  )    (",
             "_       (___/(_)(_)(__) (___)(____)(__)(_/\\/\\_)" },

           { "_           __ _____ _____ _____ __    _____ _____",
             "_        __|  |  _  |  |  |   __|  |  |     |     |",
             "_       |  |  |     |  |  |   __|  |__|-   -| | | |",
             "_       |_____|__|__|\\___/|_____|_____|_____|_|_|_|" },

           { "_         .-..---..-..-..---..-.   .-..-.-.-.",
             "_        ,| || | | \\  / | |- | |__ | || | | |",
             "_       `---'`-^-'  `'  `---'`----'`-'`-'-'-'" },

           { "_         _  ___  _ _  ___  _    _  __ __",
             "_        | || . || | || __>| |  | ||  \\  \\",
             "_       _| ||   || ' || _> | |_ | ||     |",
             "_       \\__/|_|_||__/ |___>|___||_||_|_|_|" },

           { "_          _____ _______ ___ ___ _______ _____   _______ _______",
             "_        _|     |   _   |   |   |    ___|     |_|_     _|   |   |",
             "_       |       |       |   |   |    ___|       |_|   |_|       |",
             "_       |_______|___|___|\\_____/|_______|_______|_______|__|_|__|" }};

}
