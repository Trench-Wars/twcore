package twcore.bots.multibot.prez;

import java.util.HashMap;
import java.util.Iterator;
import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Point;
import twcore.core.util.StringBag;
import twcore.core.util.Tools.Ship;

/**
    Multibot Module - President (Prez)

    Assists in hosting of president type games.

    @see <a href="http://twcore.org/ticket/739">#739</a>
    @author SpookedOne
*/
public class prez extends MultiModule {

    private CommandInterpreter m_commandInterpreter;

    private int numOfFreqs; //total number of frequencies
    private int citzShip;   //citizen ship
    private int prezShip;   //president ship

    private HashMap<Integer, Point> freqWarpPoints; //warp point for freq
    private HashMap<Integer, String> freqPrezs;     //presidents for freq

    private boolean isRunning;

    /**
        Initialize the module
    */
    @Override
    public void init() {
        m_commandInterpreter = new CommandInterpreter(m_botAction);
        registerCommands();

        numOfFreqs = 2;
        citzShip = 2;
        prezShip = 4;

        freqWarpPoints = new HashMap<Integer, Point>();
        freqPrezs = new HashMap<Integer, String>();

        isRunning = false;
    }

    /**
        Request certain event types for module
        @param events
    */
    @Override
    public void requestEvents(ModuleEventRequester events) {
        events.request(this, EventRequester.PLAYER_DEATH);
        events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        events.request(this, EventRequester.PLAYER_LEFT);
        events.request(this, EventRequester.MESSAGE);
    }

    /**
        Specifies if module can be unloaded
        @return
    */
    @Override
    public boolean isUnloadable() {
        return !isRunning;
    }

    /**
        This method is called when this module is unloaded.
    */
    @Override
    public void cancel() {
        m_botAction.cancelTasks();
    }

    /**
        Register commands for staff using Prez module.
    */
    public void registerCommands() {
        //only allow private messages with ER privs
        int acceptedMessages = Message.PRIVATE_MESSAGE;
        int accessRequired = OperatorList.ER_LEVEL;

        //register commands with appropriate handlers and access
        m_commandInterpreter.registerCommand("!start", acceptedMessages,
                                             this, "doStartGame", accessRequired);
        m_commandInterpreter.registerCommand("!setfreqs", acceptedMessages,
                                             this, "doSetFreqs", accessRequired);
        m_commandInterpreter.registerCommand("!setcitzship", acceptedMessages,
                                             this, "doSetCitzShip", accessRequired);
        m_commandInterpreter.registerCommand("!setprezship", acceptedMessages,
                                             this, "doSetPrezShip", accessRequired);
        m_commandInterpreter.registerCommand("!setfreqwarp", acceptedMessages,
                                             this, "doSetFreqWarp", accessRequired);
        m_commandInterpreter.registerCommand("!save", acceptedMessages,
                                             this, "doSave", accessRequired);
        m_commandInterpreter.registerCommand("!load", acceptedMessages,
                                             this, "doLoad", accessRequired);
        m_commandInterpreter.registerCommand("!stop", acceptedMessages,
                                             this, "doStopGame", accessRequired);
        m_commandInterpreter.registerCommand("!help", acceptedMessages,
                                             this, "doShowHelp", accessRequired);
        m_commandInterpreter.registerCommand("!rules", acceptedMessages,
                                             this, "doShowRules", accessRequired);
    }

    /**
        Command handler for !start Starts the module if all variables
        initialized.

        @param name
        @param message
    */
    public void doStartGame(String name, String message) {
        //lock arena
        m_botAction.toggleLocked();

        //set all players to citz ship
        for (Player p : m_botAction.getPlayingPlayers()) {
            m_botAction.setShip(p.getPlayerID(), citzShip);
        }

        //randomize players and assign to freqs
        createIncrementingTeams();

        //pick random person from each freq and set to prez ship
        for (int i = 0; i < numOfFreqs - 1; i++) {
            pickPresident(i);
        }

        //warp freqs to where they're assigned
        for (int freq : freqWarpPoints.keySet()) {
            Point p = freqWarpPoints.get(freq);
            m_botAction.warpFreqToLocation(freq, p.x, p.y);
        }

        //verify amount of prezs equals amount of freqs
        if (freqPrezs.keySet().size() == numOfFreqs) {
            //set game state to on to start listening to messages
            isRunning = true;
            m_botAction.sendArenaMessage("GOGOGO!!!", 104);
        } else {
            reset();
            m_botAction.sendPrivateMessage(name, "[!] Not enough players to "
                                           + "support [" + numOfFreqs + "] frequencies.");
        }
    }

    /**
        Command handler for !stop, resets the module and notifies arena
        @param name
        @param message
    */
    public void doStopGame(String name, String message) {
        reset();
        m_botAction.sendArenaMessage("This game has been slaughtered by: " + name);
    }

    //<editor-fold defaultstate="collapsed" desc="Database">

    /**
        Command handler for !save Saves the freqs and their warps to database for
        later use.

        @param name
        @param message
    */
    public void doSave(String name, String message) {
        /*  TODO: save info to database table trench_TrenchWars:tblPrezFreqWarps
                fields: fnID (auto), fcArena, fnFreq, fnX, fnY
        */
        m_botAction.sendPrivateMessage(name, "[!] Not implemented at this time."
                                       + " http://www.twcore.org/ticket/739");
    }

    /**
        Command handler for !load Loads the freqs and their warps from database
        to use.

        @param name
        @param message
    */
    public void doLoad(String name, String message) {
        /*  TODO: load info to database table trench_TrenchWars:tblPrezFreqWarps
                fields: fnID (auto), fcArena, fnFreq, fnX, fnY
        */
        m_botAction.sendPrivateMessage(name, "[!] Not implemented at this time."
                                       + " http://www.twcore.org/ticket/739");
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Accessors">
    /**
        Command handler (accessor) for !setfreqs
        @param name
        @param message
    */
    public void doSetFreqs(String name, String message) {

        try {
            numOfFreqs = Integer.parseInt(message);
            m_botAction.sendPrivateMessage(name, "Number of frequencies set to "
                                           + "[" + numOfFreqs + "].");
        } catch (NumberFormatException e) {
            m_botAction.sendPrivateMessage(name, "Unable to parse num of freqs!");
        }

    }

    /**
        Command handler (accessor) for !setcitzship
        @param name
        @param message
    */
    public void doSetCitzShip(String name, String message) {

        try {
            citzShip = Integer.parseInt(message);
            m_botAction.sendPrivateMessage(name, "Citizen ship set to ["
                                           + citzShip + "].");
        } catch (NumberFormatException e) {
            m_botAction.sendPrivateMessage(name, "Unable to parse Citz ship!");
        }

    }

    /**
        Command handler (accessor) for !setprezship
        @param name
        @param message
    */
    public void doSetPrezShip(String name, String message) {

        try {
            prezShip = Integer.parseInt(message);
            m_botAction.sendPrivateMessage(name, "President ship set to ["
                                           + prezShip + "].");
        } catch (NumberFormatException e) {
            m_botAction.sendPrivateMessage(name, "Unable to parse Prez ship!");
        }

    }

    /**
        Command handler (accessor) for !setfreqwarp
        @param name
        @param message
    */
    public void doSetFreqWarp(String name, String message) {

        try {
            //parse freq, x, y
            String[] split = message.split(":");
            int freq = Integer.parseInt(split[0]);
            String[] coord = split[1].split(",");
            int x = Integer.parseInt(coord[0]);
            int y = Integer.parseInt(coord[1]);

            //remove any current warp point
            if (freqWarpPoints.containsKey(freq)) {
                freqWarpPoints.remove(freq);
            }

            //add new point under freq
            freqWarpPoints.put(freq, new Point(x, y));
            m_botAction.sendPrivateMessage(name, "Freq [" + freq + "] warp "
                                           + "point set at [" + x + ", " + y + "].");
        } catch (NumberFormatException e) {
            m_botAction.sendPrivateMessage(name, "Unable to parse set freq warp!");
        }
    }
    // </editor-fold>

    /**
        Reset the module to it's default state
    */
    public void reset() {
        m_botAction.toggleLocked();
        freqPrezs.clear();
        isRunning = false;
    }

    /**
        Notify arena of game over and announce winning prez/freq
        @param prez Player deemed president of winning frequency
    */
    public void gameOver(Player prez) {
        m_botAction.sendArenaMessage("Congrats to President [" + prez.getPlayerName()
                                     + "] and the winning freq [" + prez.getFrequency() + "]!");
        reset();
    }

    @Override
    public String[] getModHelpMessage() {
        String[] help = {
            "PREZ (President) BOT COMMANDS",
            "!start         - Starts a game of Prez.",
            "!stop          - Stops a game currently in progress.",
            "!save          - Saves the freq numbers and warps for this arena.",
            "!load          - Loads the freq numbers and warps for this arena.",
            "!setfreqs      - Sets number of frequencies to use.",
            "!setcitzship   - Sets citizen ship type.",
            "!setprezship   - Sets president ship type.",
            "!setfreqwarp   - Sets warp point for frequency (!setfreqwarp 0:512,100)",
            "!help          - Displays help message.",
            "!rules         - Displays rules of Prez (President) Game Module."
        };
        return help;
    }

    public void doShowRules(String name, String message) {
        String[] help = {
            "PREZ (President) RULES:",
            "All players entered are randomly set to a number of frequencies,",
            " and one player from each frequency is choosen to act as president. ",
            "This player is given one life, while all the other players on freq ",
            " (citizens) are given infinite. Once the president for that freq has ",
            "been defeated, all players on that freq are given a last death.",
            " Once there is only one president remaining, that freq is declared ",
            "winner."
        };
        m_botAction.privateMessageSpam(name, help);
    }

    // <editor-fold defaultstate="collapsed" desc="Events">
    @Override
    public void handleEvent(Message event) {
        m_commandInterpreter.handleEvent(event);
    }

    @Override
    public void handleEvent(PlayerLeft event) {
        if (isRunning) {
            Player changer = m_botAction.getPlayer(event.getPlayerID());

            if (freqPrezs.containsValue(changer.getPlayerName())) {

                int freq = -1;

                for (int i : freqPrezs.keySet()) {
                    if (freqPrezs.get(i).equalsIgnoreCase(changer.getPlayerName())) {
                        freq = i;
                    }
                }

                m_botAction.sendArenaMessage("[" + changer.getPlayerName() + "]"
                                             + " has resigned from freq [" + freq + "]");

                pickPresident(freq);
            }
        }
    }

    @Override
    public void handleEvent(FrequencyShipChange event) {
        if (isRunning) {
            Player changer = m_botAction.getPlayer(event.getPlayerID());

            if (freqPrezs.containsValue(changer.getPlayerName())
                    && event.getShipType() == Ship.SPECTATOR) {

                int freq = -1;

                for (int i : freqPrezs.keySet()) {
                    if (freqPrezs.get(i).equalsIgnoreCase(changer.getPlayerName())) {
                        freq = i;
                    }
                }

                m_botAction.sendArenaMessage("[" + changer.getPlayerName() + "]"
                                             + " has resigned from freq [" + freq + "]");

                pickPresident(freq);
            }
        }
    }

    @Override
    public void handleEvent(PlayerDeath event) {
        if (isRunning) {
            Player killer = m_botAction.getPlayer(event.getKillerID());
            Player killee = m_botAction.getPlayer(event.getKilleeID());

            //check if president died, if so
            if (freqPrezs.containsValue(killee.getPlayerName())) {
                //remove freq from those that have prezs
                freqPrezs.remove((int) killee.getFrequency());

                m_botAction.sendArenaMessage("[" + killer.getPlayerName() + "] "
                                             + "has assignated [" + killee.getPlayerName() + "] of "
                                             + "freq [" + killee.getFrequency() + "]");

                //remove president from map
                m_botAction.specWithoutLock(killee.getPlayerID());
            } else {
                //if not prez and player's prez dead
                if (!freqPrezs.containsKey((int) killee.getFrequency())) {
                    //spec
                    m_botAction.specWithoutLock(killee.getPlayerID());
                }
            }

            //check for count of remaining prezs, if only 1, do game over
            if (freqPrezs.size() == 1) {
                Iterator<String> i = freqPrezs.values().iterator();

                if (i.hasNext()) {
                    gameOver(m_botAction.getPlayer(i.next()));
                }
            }
        }
    }
    // </editor-fold>

    /**
        Creates incremented team frequencies with non-spectating players.
    */
    public void createIncrementingTeams() {
        int teamSize = m_botAction.getNumPlayers() / numOfFreqs;

        StringBag plist = new StringBag();
        int freq = 0;
        String name;

        //stick all of the players in randomizer
        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

        while (i.hasNext()) {
            plist.add(i.next().getPlayerName());
        }

        while (!plist.isEmpty() && freq > -1) {
            for (int x = 0; x < teamSize; x++) {
                name = plist.grabAndRemove();

                if (name != null) {
                    m_botAction.setFreq(name, freq);
                } else {
                    freq = -2;
                    break;
                }
            }

            freq++; //that freq is done, move on to the next
        }
    }

    /**
        Picks at random 1 person from freq to play as president

        @param freq frequency to pick president from
    */
    public void pickPresident(int freq) {
        StringBag plist = new StringBag();

        m_botAction.sendPrivateMessage("SpookedOne", "[DEBUG] Picking Prez's for"
                                       + " Freq [" + freq + "].");

        Iterator<Player> i = m_botAction.getFreqPlayerIterator(freq);

        while (i != null && i.hasNext()) {
            Player p = i.next();
            plist.add(p.getPlayerName());
            //m_botAction.sendPrivateMessage("SpookedOne", "[DEBUG] Adding Player: "
            //        + i.next().getPlayerName());
        }

        if (!plist.isEmpty()) {
            String name = plist.grab();
            freqPrezs.put(freq, name);
            m_botAction.setShip(name, prezShip);
            m_botAction.sendArenaMessage("[" + name + "] has been awarded "
                                         + "presidency of freq [" + freq + "] !");

            //warp prez as they may be reset mid-game
            Point p = freqWarpPoints.get(freq);
            m_botAction.warpTo(name, p.x, p.y);
        }
    }
}
