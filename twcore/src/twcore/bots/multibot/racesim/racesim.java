package twcore.bots.multibot.racesim;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.ArenaJoined;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Ship;
import twcore.core.util.ByteArray;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

/**
    A multi-purpose MultiBot module.<p>
    This module allows you to record the movement of a specific player, store the data
    and replay it at any point in the future. The original idea was suggested by Fork.
    The bot itself will trigger on certain arena messages, allowing it to mimic a player completely.
    <p>
    At this moment in time, the module has barely undergone proper testing and is therefore marked
    to be in beta state. The order in which this module is to be used is as follows:
    <b>Recording</b>
    <ul>
    <li>Load the database with either !loadindex or !listraces;
    <li>Set a trigger line with !trigger;
    <li>Set a player to follow with !follow;
    <li>Ensure the player is in game, in the correct ship;
    <li>Set the name of the record and set the bot's state to hot with !startrec;
    <li>*arena the trigger to start the recording;
    <li>Stop the recording when the run is done with !stoprec;
    <li>Update the index one more time, to prevent accidental overwriting with !loadindex;
    <li>Store the recording with !storedata.
    </ul>
    <b>Replaying</b>
    <ul>
    <li>Load the database with either !loadindex or !listraces;
    <li>Choose the race you want to replay with !loadrace;
    <li>Put the bot in a ship with !ship;
    <li>If not set yet, provide a trigger with !trigger;
    <li>*arena the trigger to start the replay;
    <li>After the race is done, stop the replay by putting the bot into spectator mode with !spec.
    </ul>
    @author Trancid<p>

    To-Do list:<br>
    TODO Allow the bot record while racing;<br>
    TODO Allow recording of several players at the same time;<br>
    TODO Alter !spec command so it won't conflict with the always loaded spec module;<br>
    TODO Automatic loading of the index file in general, on specific commands and only when the internal data is outdated;<br>
    TODO Correct energy level at the end of the replay;<br>
    TODO Debug this module properly;<br>
    TODO Implement a proper !help and how-to menu;<br>
    TODO Implement alternative methods of triggering, like start/finish recognition;<br>
    TODO Save the position packet received right before the trigger is detected;<br>
    TODO Set rotation when the bots spawns in a ship;<br>
    TODO Smoothen out the replay functionality;<br>
    TODO Write guide on staff forum;<br>
    TODO Write some nice events that use this functionality.
*/
public class racesim extends MultiModule {

    /** Relative path to the folder where the recordings are to be saved. */
    private static final String PATH = "twcore/bots/multibot/racesim/records/";
    /**
        Version number for the storage system.<br>
        It is for the best to only adjust this when you are redoing the storage format of either
        the index file and/or the records.<p>
        <b>NOTE: When changing this, please adjust the parsing/reading/writing functions in:</b>
        <ul>
        <li>{@link #loadIndex()};
        <li>{@link #saveIndex(Record)};
        <li>{@link #readRecord()};
        <li>{@link #writeRecord(Record, boolean)}.
        </ul>
        This is to ensure backwards compatibility.
    */
    private static final byte VERSION = 0x2;

    private CommandInterpreter m_cI;                // Used to register and handle commands.
    private HashMap<String, RecordHeader> m_index;  // Holds the 'headers' of previously recorded races for an arena.
    private ArrayList<WayPoint> m_recData;          // Holds the way-points of the currently being recorded race.

    private Record m_record;                        // Holds the general info for the currently being recorded race.
    private Record m_simData;                       // Holds the replay data for the race the bot will simulate.

    private String m_playerName = "";               // Temporal storage of the name of the player who's being recorded.
    private String m_trigger = "";                  // The *arena message the bot will trigger on.

    private long m_startTime = 0;                   // Point in time at which the bot started recording a race.
    private int m_timeStamp = 0;                    // Point in time of the last recorded way-point.

    private short m_trackID = -1;                   // Player ID of the player who's being recorded.

    private boolean m_logData = false;              // True if the bot is not yet recording, but waiting for its trigger.
    private boolean m_racing = false;               // True if the bot is in a ship and ready to replay simulated data.
    private boolean m_recording = false;            // True when the bot is actually capturing data.
    private boolean m_indexLoaded = false;          // Whether or not the index file containing the headers is loaded.

    /**
        Empty, for now.
    */
    @Override
    public void cancel() {
    }

    /**
        Initializes the module.
    */
    @Override
    public void init() {
        m_cI = new CommandInterpreter(m_botAction);
        registerCommands();
    }

    /**
        Registers all the commands.
    */
    public void registerCommands() {
        int accepts = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
        int access = OperatorList.ER_LEVEL;
        m_cI.registerCommand("!about",      accepts, this, "cmd_about",          access);
        m_cI.registerCommand("!guide",      accepts, this, "cmd_guide",          access);
        m_cI.registerCommand("!info",       accepts, this, "cmd_info",           access);
        m_cI.registerCommand("!follow",     accepts, this, "cmd_follow",         access);
        m_cI.registerCommand("!trigger",    accepts, this, "cmd_trigger",        access);
        m_cI.registerCommand("!startrec",   accepts, this, "cmd_startRecording", access);
        m_cI.registerCommand("!stoprec",    accepts, this, "cmd_stopRecording",  access);
        m_cI.registerCommand("!storedata",  accepts, this, "cmd_storeData",      access);
        m_cI.registerCommand("!loadindex",  accepts, this, "cmd_loadIndex",      access);
        m_cI.registerCommand("!listraces",  accepts, this, "cmd_listRaces",      access);
        m_cI.registerCommand("!loadrace",   accepts, this, "cmd_loadRace",       access);
        m_cI.registerCommand("!ship",       accepts, this, "cmd_ship",           access);
        m_cI.registerCommand("!spec",       accepts, this, "cmd_spec",           access);

    }

    /**
        Default event requester.
        @param eventRequester Default event requester.
    */
    @Override
    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request(this, EventRequester.PLAYER_POSITION);
        eventRequester.request(this, EventRequester.ARENA_JOINED);
    }

    /**
        Passes the message event to the command interpreter and to the action decider.
        @param event The original Message event.
    */
    @Override
    public void handleEvent(Message event) {
        m_cI.handleEvent(event);

        if(event.getMessageType() == Message.ARENA_MESSAGE) {
            decideAction(event.getMessage());
        }
    }

    /**
        Although unlikely that this situation will ever happen, if the module is already loaded and
        the bot changes to another arena, it will attempt to update the list of available recordings.
        @param event The original ArenaJoined event.
    */
    @Override
    public void handleEvent(ArenaJoined event) {
        try {
            loadIndex();
        } catch (RaceSimException e) {
            // Do nothing. If anything fails, the player will just have to do the loading manually.
        }
    }

    /**
        If a position update is received for a player that is being tracked, then store the data.
        @param event Original PlayerPosition event.
    */
    @Override
    public void handleEvent(PlayerPosition event) {
        if(m_logData && event.getPlayerID() == m_trackID) {
            m_recData.add(new WayPoint(event, m_timeStamp));

            if(m_timeStamp == 0) {
                m_startTime = System.currentTimeMillis();
                m_record.setRacer(m_botAction.getPlayerName(m_trackID));
                m_record.setShip((short) (m_botAction.getPlayer(m_trackID).getShipType() - 1));
            }

            m_timeStamp = event.getTimeStamp();
            m_botAction.spectatePlayer(m_trackID);
        }

    }

    /**
        Decides which action the bot needs to take, if any.<p>
        If a valid trigger is found, then the bot will either start recording or replaying a record, or do nothing.
        @param msg The original message that got arena'd.
    */
    public void decideAction(String msg) {
        if(m_trigger == null || m_trigger.isEmpty() || !m_trigger.equalsIgnoreCase(msg))
            return;

        if(m_recording && !m_logData && m_botAction.getShip().getShip() == Ship.INTERNAL_SPECTATOR && m_trackID != -1) {
            m_botAction.spectatePlayer(m_trackID);
            m_logData = true;
        } else if(m_botAction.getShip().getShip() != Ship.INTERNAL_SPECTATOR) {
            m_botAction.scheduleTask(new MoveTask(0), 0);
            m_racing = true;
        }
    }

    public void cmd_about(String name, String message) {
        // Prevent the bot from being kicked for message flooding.
        if(m_botAction.getShip().getShip() != Ship.INTERNAL_SPECTATOR) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] This function is disabled when I'm not in spectator mode. (!spec)");
            return;
        }

        String spam[] = {
            "This module has been written by ThePAP/Trancid by request of Fork with advice from various other people",
            "like POiD. Currently it is in beta 0.2 and should thus be handled with extreme care and caution.",
            "This module allows you to, on a per arena basis, record and replay races, or movement in general.",
            "It is highly adviced, or even mandatory, to read the guide first (!guide) and to use !info on any",
            "command you haven't used before. Each time you load this module, use this command (!about) to check",
            "if it is still the same version. When the version differs, it is very likely that the commands have",
            "changed, or their usage/inner workings are different! When this is the case, it is best to",
            "re-familiarize yourself again through !guide and !info.",
            "At this moment in time, this module is written as a game module. This means you cannot use it in",
            "combination with any other game module, and it is even discouraged at this moment to use it with",
            "any other utility module on the same bot. If you want to run other modules besides this one, it is",
            "highly adviced to get and use a second multibot for this."
        };

        m_botAction.privateMessageSpam(name, spam);
    }

    /**
        Sets which player will be followed.
        @param name Issuer of command.
        @param message The name of the player that will be followed.
    */
    public void cmd_follow(String name, String message) {
        if(message == null || message.isEmpty()) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please provide the exact name of the player who will be followed.");
        } else {
            m_playerName = message;
            m_trackID = (short) m_botAction.getPlayerID(m_playerName);
            m_botAction.sendSmartPrivateMessage(name, "When the race starts, I will follow " + m_playerName + ".");
        }
    }

    public void cmd_guide(String name, String message) {
        // Prevent the bot from being kicked for message flooding.
        if(m_botAction.getShip().getShip() != Ship.INTERNAL_SPECTATOR) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] This function is disabled when I'm not in spectator mode. (!spec)");
            return;
        }

        String[] spam = {
            "At this moment in time, the module has barely undergone proper testing and is therefore marked",
            "to be in beta state. The order in which this module is to be used is as follows:",
            "Recording:",
            " - Load the database with either !loadindex or !listraces;",
            " - Set a trigger line with !trigger <trigger>;",
            " - Set a player to follow with !follow <name>;",
            " - Ensure the player is in game, in the correct ship and the bot is in spec;",
            " - Set the name of the record and set the bot's state to hot with !startrec <tag>;",
            " - *arena the trigger to start the recording;",
            " - Stop the recording when the run is done with !stoprec;",
            " - Update the index one more time, to prevent accidental overwriting with !loadindex;",
            " - Store the recording with !storedata.",
            "Replaying:",
            " - Load the database with either !loadindex or !listraces;",
            " - Choose the race you want to replay with !loadrace <tag>;",
            " - Put the bot in a ship with !ship [ship];",
            " - If not set yet, provide a trigger with !trigger <trigger>;",
            " - *arena the trigger to start the replay;",
            " - After the race is done, stop the replay by putting the bot into spectator mode with !spec.",
            "Using multiple RoboBots:",
            " It is possible to use multiple RoboBots at the same time, but especially when using them to do",
            " multiple recordings at the same time, you must use extreme caution.",
            " To ensure minimal irrevertable mistakes, always do a !loadindex on every RoboBot in the arena",
            " that has this module loaded before and after using the following commands:",
            " - !listraces;",
            " - !loadrace;",
            " - !startrec;",
            " - !stoprec;",
            " - !storedata."
        };

        m_botAction.privateMessageSpam(name, spam);
    }

    /**
        Displays detailed information on the commands used in this module.
        @param name Issuer of the command.
        @param message The command for which the info is requested. If no valid command is found,
        the general !help will be displayed.
    */
    public void cmd_info(String name, String message) {
        // Prevent the bot from being kicked for message flooding.
        if(m_botAction.getShip().getShip() != Ship.INTERNAL_SPECTATOR) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] This function is disabled when I'm not in spectator mode. (!spec)");
            return;
        }

        String[] spam;

        if(message == null || message.isEmpty()) {
            spam = getModHelpMessage();
        } else {
            message = message.toLowerCase();

            if(message.startsWith("!"))
                message = message.substring(1);

            if(message.equals("about")) {
                spam = new String[] {
                    "!about",
                    "  Lists basic information about this module.",
                    "Access: ER+"
                };
            } else if(message.equals("follow")) {
                spam = new String[] {
                    "!follow <name>",
                    "  Sets the bot up to track a specific player from spectator mode.",
                    "  Whenever the bot starts the actual recording, it will record the position data of this player.",
                    "Parameters:",
                    "  <name>   Required; Name of the player which will be followed.",
                    "Access: ER+"
                };
            } else if(message.equals("guide")) {
                spam = new String[] {
                    "!guide",
                    "  Shows a relatively short guide on how to properly use this module.",
                    "  Make sure you have read this at least once, since this module is still",
                    "  in beta mode and thus rather fragile.",
                    "Access: ER+"
                };
            } else if(message.equals("info")) {
                spam = new String[] {
                    "!info [command]",
                    "  Shows detailed information on a specific command.",
                    "  When no (valid) command is specified, it will display the general help.",
                    "Parameters:",
                    "  [command]    Optional; Name of the command you want additional information on.",
                    "Access: ER+"
                };
            } else if(message.equals("listraces")) {
                spam = new String[] {
                    "!listraces",
                    "  Shows a list of races that have already been recorded for the arena you are in.",
                    "  NOTE: It is mandatory to use this command before you can use most commands.",
                    "        This does not update/reload the list. To do so, use !loadindex.",
                    "Access: ER+"
                };
            } else if(message.equals("loadindex")) {
                spam = new String[] {
                    "!loadindex",
                    "  Loads the list of races silently. Useful for when you need to (re)load the current",
                    "  list of races, but don't want to be bothered by the spam from !listraces.",
                    "  Unlike !listraces, this always reloads the database even when it's already loaded.",
                    "Access: ER+"
                };
            } else if(message.equals("loadrace")) {
                spam = new String[] {
                    "!loadrace <tag>",
                    "  Loads a specific race into memory, to be used for replaying.",
                    "  Before being able to use this command, either !listraces or !loadindex must have been",
                    "  used at least once. It cannot be used when the bot is already replaying a race.",
                    "  The bot will start its replay when it receives the trigger set with !trigger.",
                    "Parameters:",
                    "  <tag>    Required; Name of the race you want to load. This tag is case sensitive!",
                    "Access: ER+"
                };
            } else if(message.equals("ship")) {
                spam = new String[] {
                    "!ship [ship]",
                    "  Puts the bot in a ship.",
                    "  Depending on the situation, the bot will try to put himself into the appropiate ship.",
                    "  This command cannot be used when the bot is ready to record, or is already recording",
                    "  a race.",
                    "Parameters:",
                    "  [ship]   Optional; If a recording has already been loaded, it will automatically set",
                    "                     itself in that specific ship. In any other situation, you must",
                    "                     specify a ship number, where 1 = Warbird .. 8 = Shark.",
                    "Access: ER+"
                };
            } else if(message.equals("spec")) {
                spam = new String[] {
                    "!spec",
                    "  Puts the bot into spectator mode.",
                    "  This is the only way to stop the bot midway during a replay of a race.",
                    "  At this moment, this command conflicts with the !spec command of the spec module.",
                    "Access: ER+"
                };
            } else if(message.equals("startrec")) {
                spam = new String[] {
                    "!startrec <tag>",
                    "  Sets the bot up to start recording whenever it receives the trigger specified by",
                    "  !trigger. To be able to use this command, you must have properly set !trigger and",
                    "  !follow first, and the bot needs to be in spectator mode. Please ensure that when",
                    "  doing this command, that the player you want to follow is already in his/her ship.",
                    "  Note that the name of the race is localized per arena and it is case sensitive,",
                    "  So it will only overwrite a race that has the exact same name and has been",
                    "  recorded for the arena you are currently in.",
                    "  This command will also fail when the bot is already recording.",
                    "Parameters:",
                    "  <tag>    Required: Specifieds the name the recording will be saved under.",
                    "                     This name is case sensitive and can only contain the",
                    "                     following characters: a~z, A~Z, 0~9, - and _",
                    "                     NOTE: If a race exists with a similar name for this",
                    "                     arena then this will be overwritten!",
                    "                     Any tag exceeding 19 characters will be trimmed down!",
                    "Access: ER+"
                };
            } else if(message.equals("stoprec")) {
                spam = new String[] {
                    "!stoprec",
                    "  Stops any recording the bot is currently doing.",
                    "  This command does not save the recording to disk. To do so, it is best practice to",
                    "  first do a !loadindex, followed by a !storedata.",
                    "  When this is used to abort a recording, the bot is ready straight away to start",
                    "  a new recording with !startrec. This will overwrite any unsaved previous recordings.",
                    "Access: ER+"
                };
            } else if(message.equals("storedata")) {
                spam = new String[] {
                    "!storedata",
                    "  Stores the recorded data to disk.",
                    "  This is a very dangerous command when used improperly. If a situation arises where",
                    "  multiple bots in the same arena are recording, then you must use !loadindex before",
                    "  using this command, to ensure you will not overwrite any data from the other recordings,",
                    "  even when they have completely other tags.",
                    "  Also, when you try to store a recording to disk with an exactly matching name to previous",
                    "  recording that has been made in this arena, then that previous recording will be overwritten.",
                    "  NOTE: There is no \"Undo\" when a previous record is overwritten. That data is lost forever.",
                    "Access: ER+"
                };
            } else if(message.equals("trigger")) {
                spam = new String[] {
                    "!trigger <trigger>",
                    "  Sets the trigger the bot will react upon and use to either start recording or start",
                    "  replaying a pre-recorded race. At this moment, it is only possibly to trigger through",
                    "  an arena message.",
                    "  It is best to set the trigger to an arena you do at a set time before you give the \"Go!\"",
                    "  because it is possible otherwise that the first captured waypoint will be after the race",
                    "  is started. (Try for yourself to see what this means.)",
                    "Parameters:",
                    "  <trigger>    Required; The exact text that the bot will trigger on when it sees it used",
                    "                         as an arena message. Example: \"!trigger 1..\" will cause the bot",
                    "                         to start recording/replaying when you do \"*arena 1..\".",
                    "Access: ER+"
                };
            } else {
                spam = getModHelpMessage();
            }
        }

        m_botAction.privateMessageSpam(name, spam);
    }

    /**
        Displays the list of stored races.
        @param name Issuer of the command.
        @param message Unused at the moment.
    */
    public void cmd_listRaces(String name, String message) {
        // Prevent the bot from being kicked for message flooding.
        if(m_botAction.getShip().getShip() != Ship.INTERNAL_SPECTATOR) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] This function is disabled when I'm not in spectator mode. (!spec)");
            return;
        }

        // Try to load the index.
        if(!m_indexLoaded) {
            try {
                loadIndex();
            } catch (RaceSimException rse) {
                m_botAction.sendSmartPrivateMessage(name, "[ERROR] " + rse.getMessage());
                return;
            }
        }

        if(m_index == null || m_index.isEmpty()) {
            m_botAction.sendSmartPrivateMessage(name, "There appear to be no saved records for this arena. Be the first to add one!");
        } else {
            // Finally, display the list.
            int i = 0;
            m_botAction.sendSmartPrivateMessage(name, "Listing the recordings for " + m_botAction.getArenaName() + ":");
            m_botAction.sendSmartPrivateMessage(name, "| Name                | Racer name          | Ship      | Length     |");

            for(RecordHeader rec : m_index.values()) {
                // Formatting: | 19 | 19 | 9 | 8 |
                m_botAction.sendSmartPrivateMessage(name,
                                                    "| " + Tools.formatString(rec.getTag(), 19)
                                                    + " | " + Tools.formatString(rec.getRacer(), 19)
                                                    + " | " + Tools.formatString(Tools.shipName(rec.getShip() + 1), 9)
                                                    + " | " + Tools.rightString(Tools.getTimeString(rec.getLength(), true), 10)
                                                    + " |");
                i++;
            }

            m_botAction.sendSmartPrivateMessage(name, "Displayed a total of " + i + " records for this arena.");
        }
    }

    /**
        Loads the index file for this specific arena without displaying it.
        @param name Player who issued the command.
        @param message Not used at this moment.
    */
    public void cmd_loadIndex(String name, String message) {
        try {
            loadIndex();
            m_botAction.sendSmartPrivateMessage(name, "Index reloaded.");
        } catch (RaceSimException rse) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] " + rse.getMessage());
        }
    }

    /**
        Loads logged data for the current arena from disk.
        @param name Issuer of command.
        @param message Name of the recording to load.
    */
    public void cmd_loadRace(String name, String message) {
        if(m_logData || m_recording) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please disable recording mode first.");
        } else if(m_racing) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Cannot load data while racing.");
        } else if(message == null || message.isEmpty()) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please specify which recording you want to load.");
        } else if(!m_indexLoaded) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please load the index first with !loadindex");
        } else if(m_index == null || m_index.isEmpty()) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] No previously recorded races present for this arena.");
        } else if(!m_index.containsKey(message)) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] " + message + " is not a valid name. Names are case sensitive!");
        } else {
            try {
                m_simData = new Record(m_index.get(message));
                readRecord();
                m_botAction.sendSmartPrivateMessage(name, "Succesfully loaded: " + message);
                m_botAction.sendSmartPrivateMessage(name, "Racer: " + m_simData.getRacer()
                                                    + "; Ship: " + Tools.shipName(m_simData.getShip() + 1)
                                                    + "; Duration: " + Tools.getTimeString(m_simData.getLength(), true)
                                                    + "; Waypoints: " + m_simData.getWaypoints().size());
            } catch (RaceSimException rse) {
                m_botAction.sendSmartPrivateMessage(name, "[ERROR] " + rse.getMessage());
            }
        }
    }

    /**
        This command puts the bot in a ship. Depending if any previous data has been loaded,
        the correct ship will be chosen. Otherwise, the ship specified by the user will be used.
        Additionally the bot will be put at the initial location in the recording, if this information is available.
        @param name Issuer of the command.
        @param message Optionally the ship type according to in-game numbering. (1=WB .. 8=Shark, 0=Spectator)
    */
    public void cmd_ship(String name, String message) {
        if(m_recording || m_logData) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please disable recording mode first.");
        } else if(m_racing) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Cannot change ships while racing.");
        } else if((message == null || message.isEmpty()) && m_simData != null) {
            // When possible, put the bot automatically in the ship that was used in the recording.
            m_botAction.spectatePlayerImmediately(-1);
            m_botAction.getShip().setShip(m_simData.getShip());

            if(!m_simData.getWaypoints().isEmpty())
                m_botAction.getShip().move(m_simData.getWaypoints().get(0).getX(), m_simData.getWaypoints().get(0).getY(), 0, 0);

            m_botAction.getShip().fire(1);
        } else if(message == null || message.isEmpty()) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please provide a valid ship number (1-8).");
        } else {
            try {
                int shipNumber = Integer.parseInt(message);

                if(shipNumber < 1 || shipNumber > 8) {
                    m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please provide a valid ship number (1-8).");
                } else {
                    m_botAction.spectatePlayerImmediately(-1);
                    m_botAction.getShip().setShip(shipNumber - 1);
                    m_botAction.getShip().move(m_simData.getWaypoints().get(0).getX(), m_simData.getWaypoints().get(0).getY(), 0, 0);
                    m_botAction.getShip().fire(1);
                }
            } catch(NumberFormatException nfe) {
                m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please provide a valid ship number (1-8).");
            }
        }
    }

    /**
        Puts the bot into spectator mode. This state is needed for several recording functions.
        @param name Issuer of the command.
        @param message Unused at the moment.
    */
    public void cmd_spec(String name, String message) {
        if(m_botAction.getShip().getShip() == Ship.INTERNAL_SPECTATOR) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Already in spectator mode.");
        } else {
            m_racing = false;
            m_botAction.getShip().setShip(Ship.INTERNAL_SPECTATOR);
            m_botAction.sendSmartPrivateMessage(name, "Changed to spectator mode.");
        }
    }

    /**
        Puts the bot in a ready mode where it waits for the trigger to happen and assigns an identifier to the recording.
        When the trigger happens, it will start logging data.
        @param name Issuer of command.
        @param message The tag/identifier for the new record.
    */
    public void cmd_startRecording(String name, String message) {
        if(m_racing || m_botAction.getShip().getShip() != Ship.INTERNAL_SPECTATOR) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Not in spectator mode. (!spec)");
        } else if(m_trigger == null || m_trigger.isEmpty()) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] No trigger message has been set yet.");
        } else if(m_trackID == -1) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] No player set yet.");
        } else if(m_recording) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Already in recording mode.");
        } else if(m_logData) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Already logging data.");
        } else if(message == null || message.isEmpty()) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please specify an identifying name for this record.");
        } else if(!m_indexLoaded) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please load the index first with !loadindex or !listraces");
        } else {
            // Transform any argument past into a suitable name. This basically replace any of the non-mentioned chars in underscores.
            message = message.replaceAll("[^a-zA-Z0-9_-]+", "_").trim();

            if(message.length() > 19) {
                message = message.substring(0, 19);
            }

            if(m_index != null && !m_index.isEmpty() && m_index.containsKey(message)) {
                // Display a warning if the entry already exists.
                m_botAction.sendSmartPrivateMessage(name, "[WARNING] The name you provided is already used. If you choose save this recording, then the previous record will be erased.");
            }

            // Initialize and set the necessary variables.
            m_botAction.spectatePlayer(m_trackID);
            m_recording = true;
            m_recData = new ArrayList<WayPoint>();
            m_record = new Record(message);
            m_startTime = 0;
            m_timeStamp = 0;
            m_botAction.sendSmartPrivateMessage(name, "Recording mode activated. Waiting for trigger to start logging data.");
        }
    }

    /**
        Disables the recording mode and stops any logging of data if this is happening.
        @param name Issuer of command.
        @param message Ignored, for now.
    */
    public void cmd_stopRecording(String name, String message) {
        if(!m_recording) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Not in recording mode.");
        } else if(!m_logData) {
            m_recording = false;
            m_botAction.spectatePlayer(-1);
            m_botAction.sendSmartPrivateMessage(name, "Recording mode deactivated.");
        } else {
            m_recording = false;
            m_logData = false;
            m_botAction.sendSmartPrivateMessage(name, "Recording mode deactivated. Disabling logging of data.");
            m_botAction.sendSmartPrivateMessage(name, "Race duration: " + Tools.getTimeDiffString(m_startTime, false));

            // If anything has been recorded, update the needed statistics and the waypoints.
            if(m_recData != null && !m_recData.isEmpty()) {
                m_record.setLength((int) (System.currentTimeMillis() - m_startTime));
                m_record.setWaypoints(m_recData);
            }
        }
    }

    /**
        Saves the currently logged data to disk.
        @param name Issuer of command.
        @param message Optional parameter: "overwrite", will overwrite the file if it already exists.
    */
    public void cmd_storeData(String name, String message) {
        if(m_logData || m_recording) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please disable recording mode first.");
        } else if(m_racing) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Cannot store data while racing.");
        } else {
            try {
                boolean overwrite = false;

                if(message != null && !message.isEmpty() && message.equalsIgnoreCase("overwrite"));

                overwrite = true;
                saveRace(m_record, overwrite);
                m_botAction.sendSmartPrivateMessage(name, "Succesfully stored recording.");
            } catch (RaceSimException rse) {
                m_botAction.sendSmartPrivateMessage(name, "[ERROR] " + rse.getMessage());
            }
        }
    }

    /**
        Sets the message that triggers an action by this bot when it's arena'd.
        @param name Issuer of command.
        @param message Trigger message.
    */
    public void cmd_trigger(String name, String message) {
        if(message == null || message.isEmpty()) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please provide a valid trigger.");
        } else {
            m_trigger = message;
            m_botAction.sendSmartPrivateMessage(name, "I will start recording or racing when I see the following arena message:");
            m_botAction.sendSmartPrivateMessage(name, m_trigger);
        }
    }

    /**
        Help message place holder.
    */
    @Override
    public String[] getModHelpMessage() {
        if(m_botAction.getShip().getShip() == Ship.INTERNAL_SPECTATOR) {
            String[] out = {
                "+-----------------------------------------------------------------------+",
                "| RaceSimulator v.0.2                                                   |",
                "+-----------------------------------------------------------------------+",
                "| Commands:                                                             |",
                "|   !about             What this module is and how to use it.           |",
                "|   !follow <name>     Sets the bot to follow <name>.                   |",
                "|   !guide             How to use this module. Best to read this first! |",
                "|   !info [cmd]        This menu or for details on command [cmd].       |",
                "|   !listraces         Lists recorded races for this arena.             |",
                "|   !loadindex         Same as !listraces, but silently.                |",
                "|   !loadrace <tag>    Loads the record named <tag>.                    |",
                "|   !ship [type]       Puts the bot in a ship.                          |",
                "|   !spec              Puts the bot into spectator mode.                |",
                "|   !startrec <tag>    Prepares the capturing of data for record <tag>. |",
                "|   !stoprec           Stops the capturing of data.                     |",
                "|   !storedata         Stores the captured record to disk.              |",
                "|   !trigger           Sets the trigger which starts the capture.       |",
                "+-----------------------------------------------------------------------+",
                "| Firt time users:                                                      |",
                "| This module is still in beta mode. It is mandatory that you read      |",
                "| !about and !guide first, as well as using !info [cmd] before using    |",
                "| a command that you've never used before.                              |",
                "+-----------------------------------------------------------------------+",
            };
            return out;
        } else {
            String[] out = {
                "[ERROR] Cannot spam help while in a ship. (!spec)"
            };
            return out;
        }
    }

    /**
        Tells the general module whether or not this module can be unloaded.
    */
    @Override
    public boolean isUnloadable() {
        return (!m_logData && !m_recording && !m_racing);
    }

    /*
        File interaction helper functions.

        Formatting conventions:

        Arena index files:
        File name: <Arenaname>.txt
        Formatting: String, \0-delimited, variable length.
        Details:
        This file will hold the 'headers' of the recordings made in that specific arena
        and will be formatted according to the following:
        <Record_Name>\0<Racer_Name>\0<Shiptype>\0<Length_Recording><Line_Separator>

        Race records:
        File name: arenaname_recordname.dat;
        Formatting: binary, fixed length fields;
        Details:
        Field 0: Version ID, 1 byte;
        Field 1-end: Repeated data structure (18 bytes wide) with the following properties/fields:
        - X-coordinate [short]
        - Y-coordinate [short]
        - X-velocity [short]
        - Y-velocity [short]
        - Direction [byte]
        - Togglables [byte]
        - Energy [short]
        - Bounty [short]
        - Delta T [integer]

    */
    /**
        Saves both the current recording and updates the index list on disk.
        @param rec Record to be written to disk.
        @param overwrite Whether or not any existing recording file (Not header file!) is to be overwritten.
        @throws RaceSimException Forwards any exception thrown by the called functions.
    */
    private void saveRace(Record rec, boolean overwrite) throws RaceSimException {
        writeRecord(rec, overwrite);
        // If the above function throws any exceptions, it is likely that the record isn't stored properly.
        // In this case, the index file shouldn't be updated, which won't due to the exception thrown before.
        saveIndex(rec);
    }

    /**
        Loads an index of recorded races for this particular arena. The indexes will be saved to m_index.
        @throws RaceSimException If anything bad happens, an exception will be thrown containing a readable message.
        If everything goes as planned, no exception will be thrown.
    */
    private void loadIndex() throws RaceSimException {
        // Empty out any current information we have.
        m_index = new HashMap<String, RecordHeader>();

        // Due to the arena name in game being case insensitive in game, ensure that the capitalization has no effect.
        String filename = PATH + m_botAction.getArenaName().toLowerCase() + ".txt";

        try {
            File f = new File(filename);

            if(!f.isFile()) {
                // When no index exists yet, make sure the proper flag is set, but leave the index itself empty.
                m_indexLoaded = true;
                return;
            }

            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);

            // If the file exists, but is empty, also set the proper flag, but leave the index itself empty.
            String line = br.readLine();

            if(line == null) {
                m_indexLoaded = true;
                br.close();
                fr.close();
                return;
            }

            // Switch based on the version number. When updating the file format, please
            // try to keep it backward compatible by making a separate case for the old version number.
            switch(Byte.parseByte(line)) {
            case VERSION:
                // Current version/format.
                String[] args;

                while((line = br.readLine()) != null) {
                    // Read per line and split on the null-terminators.
                    args = line.split("\0", 4);

                    if(args.length != 4) {
                        // Malformed data found. For now, just skip it.
                        continue;
                    }

                    try {
                        m_index.put(args[0], new RecordHeader(
                                        args[0],
                                        args[1],
                                        Short.parseShort(args[2]),
                                        Integer.parseInt(args[3])));
                    } catch (NumberFormatException nfe) {
                        // Malformed data found. For now, just skip it.
                        continue;
                    }
                }

                break;

            default:
                // Unknown version or format.
                br.close();
                fr.close();
                throw new RaceSimException("Unknown version number found.");
            }

            br.close();
            fr.close();
            // Seems everything was a success, set the flag to true.
            m_indexLoaded = true;
        } catch (NullPointerException npe) {
            Tools.printStackTrace(npe);
            throw new RaceSimException("Incorrect path name: " + filename);
        } catch (FileNotFoundException fnfe) {
            Tools.printStackTrace(fnfe);
            throw new RaceSimException("File not found: " + filename);
        } catch (IOException ioe) {
            Tools.printStackTrace(ioe);
            throw new RaceSimException("Unable to read from file: " + filename);
        } catch (NumberFormatException nfe) {
            Tools.printStackTrace(nfe);
            throw new RaceSimException("Corrupt version number: " + filename);
        }
    }

    /**
        Saves the 'header' information of all the records to an index file for a particular arena.
        @param rec The record that is to be added to the list of headers.
        @throws RaceSimException If anything bad happens, an exception will be thrown containing a readable message.
        If everything goes as planned, no exception will be thrown.
    */
    private void saveIndex(Record rec) throws RaceSimException {
        if(rec == null) {
            // Can't store anything if we don't have anything to store.
            throw new RaceSimException("Record data is null.");
        }

        // Put the new record's header to the index list. Will automatically replace if it already exists.
        m_index.put(rec.getTag(), new RecordHeader(rec.getTag(), rec.getRacer(), rec.getShip(), rec.getLength()));

        // Due to the arena name in game being case insensitive in game, ensure that the capitalization has no effect.
        String filename = PATH + m_botAction.getArenaName().toLowerCase() + ".txt";

        try {
            File f = new File(filename);
            FileWriter fw = new FileWriter(f);
            BufferedWriter bw = new BufferedWriter(fw);

            // Write the version number first.
            bw.write(Byte.toString(VERSION));
            bw.newLine();

            // Write the headers on separate lines based on their toStrings.
            for(RecordHeader rh : m_index.values()) {
                bw.write(rh.toString());
                bw.newLine();
            }

            bw.close();
            fw.close();
        } catch (NullPointerException npe) {
            Tools.printStackTrace(npe);
            throw new RaceSimException("Incorrect path name: " + filename);
        } catch (IOException ioe) {
            Tools.printStackTrace(ioe);
            throw new RaceSimException("Unable to write to file: " + filename);
        }
    }

    /**
        Reads a record from file. If adjusted properly, this will be backward compatible.
        @throws RaceSimException If any problems arise, this will be thrown with a readable reason.
        If the read is successful, nothing will be thrown.
    */
    private void readRecord() throws RaceSimException {
        if(m_simData == null) {
            // We need data already present to determine the name.
            throw new RaceSimException("Record data is null.");
        }

        // Due to the arena name in game being case insensitive, do so for the filename as well, but not for the Tag.
        String filename = PATH + m_botAction.getArenaName().toLowerCase() + "_" + m_simData.getTag() + ".dat";

        try {
            File f = new File(filename);

            if(!f.isFile()) {
                // What it says below.
                throw new RaceSimException("Record not found on disk: " + filename);
            }

            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);

            // Clear the current list, to prevent appending new data to the existing data.
            m_simData.getWaypoints().clear();

            // Switch based on the version number. When updating the file format, please
            // try to keep it backward compatible by making a separate case for the old version number.
            switch(bis.read()) {
            case VERSION:
                // Current format.
                byte[] data = new byte[18];
                ByteArray bArray;
                int len;

                // Read in one field at a time and add it to the simulation data.
                while((len = bis.read(data)) == 18) {
                    bArray = new ByteArray(data);
                    m_simData.getWaypoints().add(new WayPoint(
                                                     bArray.readShort(0),
                                                     bArray.readShort(2),
                                                     bArray.readShort(4),
                                                     bArray.readShort(6),
                                                     bArray.readByte(8),
                                                     bArray.readByte(9),
                                                     bArray.readShort(10),
                                                     bArray.readShort(12),
                                                     bArray.readInt(14)));
                }

                // If we exit the while loop on a size that is neither -1 (EoF) or 18 (field length) then one or
                // more records might contain corrupt data. Take the safe method, and dump all data retrieved.
                if(len != -1 && len != 18) {
                    bis.close();
                    fis.close();
                    m_simData.getWaypoints().clear();
                    throw new RaceSimException("Recorded data is corrupt.");
                }

                break;

            default:
                // Unsupported version/format.
                bis.close();
                fis.close();
                throw new RaceSimException("Unknown version number found.");
            }

            bis.close();
            fis.close();
        } catch (NullPointerException npe) {
            Tools.printStackTrace(npe);
            throw new RaceSimException("Incorrect path name: " + filename);
        } catch (SecurityException se) {
            Tools.printStackTrace(se);
            throw new RaceSimException("Access denied to file: " + filename);
        } catch (FileNotFoundException fnfe) {
            Tools.printStackTrace(fnfe);
            throw new RaceSimException("File not found: " + filename);
        } catch (IOException ioe) {
            Tools.printStackTrace(ioe);
            throw new RaceSimException("Unable to read from file: " + filename);
        }
    }

    /**
        Writes recorded data to file.
        @param rec The record that needs to be stored.
        @param overwrite Whether or not any existing files need to be overwritten.
        @throws RaceSimException If any error happens, this will be thrown back containing a readable message of what went wrong.
        When the storage process finished successfully, no exception will be thrown.
    */
    private void writeRecord(Record rec, boolean overwrite) throws RaceSimException {
        if(rec == null) {
            // Nothing to save.
            throw new RaceSimException("Record data is null.");
        }

        // Due to the arena name in game being case insensitive, do so for the filename as well, but not for the Tag.
        String filename = PATH + m_botAction.getArenaName().toLowerCase() + "_" + rec.getTag() +  ".dat";

        try {
            File f = new File(filename);

            if(f.isFile() && !overwrite) {
                // File already exists, but the overwrite flag isn't set.
                throw new RaceSimException("File already exists. Use \"!saveRecord overwrite\" to overwrite file.");
            }

            FileOutputStream fos = new FileOutputStream(f);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            ArrayList<WayPoint> waypoints = rec.getWaypoints();

            if(waypoints == null || waypoints.isEmpty()) {
                // Nothing to save. Should perhaps move this upwards.
                bos.close();
                fos.close();
                throw new RaceSimException("No waypoints found in recording.");
            }

            // Version number first, followed by 18-byte wide fields.
            bos.write(VERSION);
            ByteArray bArray = new ByteArray(18);

            for(WayPoint wp : waypoints) {
                bArray.addShort(wp.getX(), 0);
                bArray.addShort(wp.getY(), 2);
                bArray.addShort(wp.getVx(), 4);
                bArray.addShort(wp.getVy(), 6);
                bArray.addByte(wp.getDirection(), 8);
                bArray.addByte(wp.getToggables(), 9);
                bArray.addShort(wp.getEnergy(), 10);
                bArray.addShort(wp.getBounty(), 12);
                bArray.addInt(wp.getDT(), 14);
                bos.write(bArray.getByteArray());
            }

            bos.close();
            fos.close();
        } catch (NullPointerException npe) {
            Tools.printStackTrace(npe);
            throw new RaceSimException("Incorrect path name: " + filename);
        } catch (SecurityException se) {
            Tools.printStackTrace(se);
            throw new RaceSimException("Access denied to file: " + filename);
        } catch (FileNotFoundException fnfe) {
            Tools.printStackTrace(fnfe);
            throw new RaceSimException("File not found: " + filename);
        } catch (IOException ioe) {
            Tools.printStackTrace(ioe);
            throw new RaceSimException("Unable to write to file: " + filename);
        }
    }
    /*
        Helper classes.
    */
    /**
        Main storage class that will hold basic or detailed information in regard to the recordings.
        @author Trancid

    */
    private class Record extends RecordHeader {
        ArrayList<WayPoint> waypoints;  // Contains a list of waypoints in order of which they should fire.

        /**
            Basic constructor.<p>
            Sets the main id/tag and a default ship type. Also initializes the internal {@link ArrayList}.
            @param tag The tag that will be used for this class. Case sensitive!
        */
        public Record(String tag) {
            super(tag);
            this.ship = Ship.INTERNAL_WARBIRD;
            waypoints = new ArrayList<WayPoint>();
        }

        /**
            Inherited constructor.<p>
            Expands a basic {@link RecordHeader} into a full Record.
            @param rh The RecordHeader that is used as a base.
        */
        public Record(RecordHeader rh) {
            super(rh.getTag(), rh.getRacer(), rh.getShip(), rh.getLength());
            this.waypoints = new ArrayList<WayPoint>();
        }

        /**
            Full constructor.<p>
            Sets every detail there is and initializes the main internal {@link ArrayList}s.
            @param tag Main ID/tag/name for this record. Case sensitive!
            @param racer Name of the racer who has been recorded.
            @param ship Ship type according to INTERNAL numbering. (0=WB .. 7=Shark, 8=Spectator)
            @param length Length of the recording in milliseconds.
        */
        public Record(String tag, String racer, short ship, int length) {
            super(tag, racer, ship, length);
            this.waypoints = new ArrayList<WayPoint>();
        }

        /**
            Adds all members of type {@link WayPoint} from an {@link ArrayList} to the internal list.
            @param waypoints ArrayList with members of type WayPoint.
        */
        public void setWaypoints(ArrayList<WayPoint> waypoints) {
            this.waypoints.addAll(waypoints);
        }

        /**
            Retrieves a handle to the internally stored list of way-points.
            @return A reference to this class' way-points.
        */
        public ArrayList<WayPoint> getWaypoints() {
            return this.waypoints;
        }

        /**
            A safe method to retrieve the delta T of a way-point at the specified index of the internal list.
            Useful for when you are unsure if the index will be out of bounds.
            @param index Index of the way-point you want to retrieve.
            @return The accompanied delta T between this and the previous way-point in milliseconds or 150ms when not present.
        */
        public int getDT(int index) {
            if(index >= 0 && index < this.waypoints.size()) {
                return this.waypoints.get(index).getDT();
            } else {
                return 150;
            }
        }
    }

    /**
        This class is the brains of the {@link Record} class. It is separated from the body to
        minimize the memory footprint when indexing the data of the previously stored races.<p>
        This class itself holds basic, general information in regard to recorded races like
        the name and ship type.
        @author Trancid

    */
    private class RecordHeader {
        String tag;         // The tag/id/name this object will go by when a user wants to access its contents. Case sensitive!
        String racerName;   // The name of the racer who was originally stalked to produce the data.
        short ship;         // The ship the racer was in at the time of recording according to the INTERNAL numbering.
        int length;         // The length/duration of the recording in milliseconds.

        /**
            Default constructor of a basic, empty class. Only sets tag and ship-type.
            @param tag The tag this record will go by. Case sensitive!
        */
        public RecordHeader(String tag) {
            this.tag = tag;
            this.ship = Ship.INTERNAL_WARBIRD;
        }

        /**
            Full constructor.
            @param tag The tag this record will go by. Case sensitive!
            @param racer The name of the racer.
            @param ship The ship of the racer according to internal numbering. 0=Warbird ... 7=Shark, 8=Spectator
            @param length
        */
        public RecordHeader(String tag, String racer, short ship, int length) {
            this.tag = tag;
            this.racerName = racer;
            this.ship = ship;
            this.length = length;
        }

        /*
            Getters and setters.
        */
        /**
            @return Tag of this object.
        */
        public String getTag() {
            return this.tag;
        }

        /**
            @return Name of the original racer.
        */
        public String getRacer() {
            return this.racerName;
        }

        /**
            @return Ship type according to INTERNAL numbering.
        */
        public short getShip() {
            return this.ship;
        }

        /**
            @return Duration of the recording.
        */
        public int getLength() {
            return this.length;
        }

        /**
            Sets the racer's name.
            @param racer Racer's name.
        */
        public void setRacer(String racer) {
            this.racerName = racer;
        }

        /**
            Sets the ship type.
            @param ship Ship type according to INTERNAL numbering.
        */
        public void setShip(short ship) {
            this.ship = ship;
        }

        /**
            Sets the length of the recording.
            @param length Duration in milliseconds.
        */
        public void setLength(int length) {
            this.length = length;
        }

        /**
            Good old customized toString method, mainly used as formatting for writing to files.
        */
        public String toString() {
            String output = this.tag + '\0'
                            + this.racerName + '\0'
                            + this.ship + '\0'
                            + Integer.toString(this.length);

            return output;
        }
    }

    /**
        This class makes up the body of the {@link Record} class through an {@link ArrayList}.<p>
        It holds a single waypoint with the nescessary information to make the bot's movement look real.
        Technically, only the x- and y-coordinate, x- and y-speed and the rotation/direction are needed.
        The other parameters are just to make it look as real as possible, by making the info sent with
        the packet as complete as possible.
        @author Trancid

    */
    private class WayPoint {
        private short wp_x;     // X-coordinate in pixels.
        private short wp_y;     // Y-coordinate in pixels.
        private short wp_vx;    // Horizontal speed in pixels/10 sec.
        private short wp_vy;    // Vertical speed in pixels/10 sec.
        private byte wp_dir;    // Direction/heading/rotation (0~39, 1 point being 40/360 degrees, clockwise from top.)
        private byte wp_tog;    // Set of togglables, see the PlayerPosition packet/event for details.
        private short wp_ene;   // Current energy level.
        private short wp_bty;   // Current bounty.
        private int wp_dt;      // Time that has expired since the previous waypoint, converted to milliseconds.

        /**
            Constructor
            @param event PlayerPosition event that contains the information that needs to be logged.
            @param timestamp Timestamp of the previous log. Used to determine the delta T.
        */
        public WayPoint(PlayerPosition event, int timestamp) {
            wp_x = event.getXLocation();
            wp_y = event.getYLocation();
            wp_vx = event.getXVelocity();
            wp_vy = event.getYVelocity();
            wp_dir = event.getRotation();
            wp_tog = event.getTogglables();
            wp_ene = event.getEnergy();
            wp_bty = event.getBounty();

            if(timestamp == 0) {
                // First waypoint of the series.
                wp_dt = 0;
            } else if(event.getTimeStamp() < timestamp ) {
                // Overflow scenario
                wp_dt = (65535 + event.getTimeStamp() - timestamp) * 10;
            } else {
                // Normal situation.
                wp_dt = (event.getTimeStamp() - timestamp) * 10;

            }
        }

        /**
            Constructor used when manually setting all the parameters. For example, when reading the data back
            from a file instead of from a position packet.
            @param x X-coordinate in pixels.
            @param y Y-coordinate in pixels.
            @param vX X-velocity in pixels/10 sec.
            @param vY Y-velocity in pixels/10 sec.
            @param direction Heading/direction/rotation in points (0~39, 1 point == 40/360 degree, starting north, going clockwise.)
            @param toggles List of togglables, see {@link PlayerPosition} for details.
            @param energy Current energy level.
            @param bounty Current bounty.
            @param dT Time past since previous waypoint in milliseconds.
        */
        public WayPoint(short x, short y, short vX, short vY, byte direction, byte toggles, short energy, short bounty, int dT) {
            wp_x = x;
            wp_y = y;
            wp_vx = vX;
            wp_vy = vY;
            wp_dir = direction;
            wp_tog = toggles;
            wp_ene = energy;
            wp_bty = bounty;
            wp_dt = dT;
        }

        /*
            Getters
        */
        /**
            @return X-coordinate in pixels.
        */
        public short getX() {
            return wp_x;
        }

        /**
            @return Y-coordinate in pixels.
        */
        public short getY() {
            return wp_y;
        }

        /**
            @return X-velocity in pixels/10 sec.
        */
        public short getVx() {
            return wp_vx;
        }

        /**
            @return Y-velocity in pixels/10 sec.
        */
        public short getVy() {
            return wp_vy;
        }

        /**
            @return Rotation in points.
        */
        public byte getDirection() {
            return wp_dir;
        }

        /**
            @return Bitfield containing togglables.
        */
        public byte getToggables() {
            return wp_tog;
        }

        /**
            @return Energy level.
        */
        public short getEnergy() {
            return wp_ene;
        }

        /**
            @return Bounty.
        */
        public short getBounty() {
            return wp_bty;
        }

        /**
            @return Time between this waypoint and the previous waypoint in milliseconds.
        */
        public int getDT() {
            return wp_dt;
        }

        /**
            Old toString function. Was used in old storage format, now is only used for debugging purposes.
        */
        public String toString() {
            return (this.wp_x
                    + ":" + this.wp_y
                    + ":" + this.wp_vx
                    + ":" + this.wp_vy
                    + ":" + this.wp_dir
                    + ":" + this.wp_tog
                    + ":" + this.wp_ene
                    + ":" + this.wp_bty
                    + ":" + this.wp_dt);
        }
    }

    /**
        This class takes care of the replaying of the data.
        <p>
        As the base of the data, the ArrayList from m_simData.getWaypoints() is used.
        As long as there is data in that ArrayList, it will initiate a new timer to fire for the next waypoint.
        Whenever the ArrayList runs out of data or the bot is put into spectator mode, this class will stop initiating
        new timers, and thus ending the loop.
        @author Trancid

    */
    private class MoveTask extends TimerTask {
        private int index;

        /**
            MoveTask constructor.
            @param index Index of the waypoint that is to be used in the move command.
        */
        public MoveTask(int index) {
            this.index = index;
        }

        /**
            Depending on the situation, either stops the bot in its tracks or
            moves the bot to the next waypoint.
        */
        @Override
        public void run() {
            // We've been put in spectator mode. Kill the speed to prevent drifting off.
            if(!m_racing) {
                m_botAction.getShip().move(m_botAction.getShip().getX(), m_botAction.getShip().getY(), 0, 0);
                return;
            }

            try {
                // Load the next waypoint we need to visit, send out the move command and schedule the next waypoint.
                WayPoint wp = m_simData.getWaypoints().get(index);
                m_botAction.getShip().move(
                    wp.getDirection(),
                    wp.getX(),
                    wp.getY(),
                    wp.getVx(),
                    wp.getVy(),
                    wp.getToggables(),
                    wp.getEnergy(),
                    wp.getBounty());
                // The dT of the next waypoint will tell us how long we need to wait to fire the next one.
                m_botAction.scheduleTask(new MoveTask(++index), m_simData.getDT(index));
            } catch(IndexOutOfBoundsException ioobe) {
                // No more data in the array. Kill the bot's speed to prevent it from drifting off.
                m_botAction.getShip().move(m_botAction.getShip().getX(), m_botAction.getShip().getY(), 0, 0);
                return;
            }
        }
    }

    /**
        This class is used to easily propagate error messages throughout the entire bot. The main usage is in the
        file parsing commands.
        @author Trancid

    */
    private class RaceSimException extends Exception {
        /**
            Auto generated serial ID.
        */
        private static final long serialVersionUID = 4793557654994493136L;
        private String message;

        public RaceSimException(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

}
