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

public class racesim extends MultiModule {
    
    private static final String PATH = "twcore/bots/multibot/racesim/records/";
    private static final byte VERSION = 0x2;

    private CommandInterpreter m_commandInterpreter;
    private HashMap<String,RecordHeader> m_index;
    private ArrayList<WayPoint> m_posData;
    private ArrayList<WayPoint> m_recData;
    
    private Record m_record;
    private Record m_simData;
    
    private String m_playerName = "";
    private String m_trigger = "";
    
    private long m_startTime = 0;
    private int m_timeStamp = 0;
    
    private short m_trackID = -1;
    
    private boolean m_logData = false;
    private boolean m_racing = false;
    private boolean m_recording = false;
    private boolean m_indexLoaded = false;
    
    @Override
    public void cancel() {
        // TODO Auto-generated method stub
    }

    @Override
    public void init() {
        m_commandInterpreter = new CommandInterpreter(m_botAction);
        registerCommands();
    }

    public void registerCommands() {
        int acceptedMessages = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand("!follow",     acceptedMessages, this, "cmd_follow",           OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!trigger",    acceptedMessages, this, "cmd_trigger",          OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!startrec",   acceptedMessages, this, "cmd_startRecording",   OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!stoprec",    acceptedMessages, this, "cmd_stopRecording",    OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!storedata",  acceptedMessages, this, "cmd_storeData",        OperatorList.SMOD_LEVEL);
        m_commandInterpreter.registerCommand("!loadindex",  acceptedMessages, this, "cmd_loadIndex",        OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!listraces",  acceptedMessages, this, "cmd_listRaces",        OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!loadrace",   acceptedMessages, this, "cmd_loadRace",         OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!ship",       acceptedMessages, this, "cmd_ship",             OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!spec",       acceptedMessages, this, "cmd_spec",             OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!blaat",      acceptedMessages, this, "cmd_debug",            OperatorList.SYSOP_LEVEL);
    }
    
    @Override
    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request(this, EventRequester.PLAYER_POSITION);
        eventRequester.request(this, EventRequester.ARENA_JOINED);
    }
    
    @Override
    public void handleEvent(Message event) {
        m_commandInterpreter.handleEvent(event);
        
        if(event.getMessageType() == Message.ARENA_MESSAGE) {
            decideAction(event.getMessage());
        }
    }
    
    @Override
    public void handleEvent(ArenaJoined event) {
        try {
            loadIndex();
        } catch (RaceSimException e) {
            // Do nothing.
        }
    }
    
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
    
    public void cmd_debug(String name, String message) {
        int i;
        if(message == null || message.isEmpty()) {
            m_botAction.sendSmartPrivateMessage(name, "Provide parameter.");
        }
        
        try {
            i = Integer.parseInt(message);
            
            if(m_simData == null || !m_simData.contains(i)) {
                m_botAction.sendSmartPrivateMessage(name, "No simData present.");
            } else {
                m_botAction.sendSmartPrivateMessage(name, "Contents: " + m_simData.getWaypoint(i).toString());
            }
        } catch (NumberFormatException nfe) {
            m_botAction.sendSmartPrivateMessage(name, "Not a number");
        }

    }
    
    /**
     * Sets which player will be followed.
     * @param name Issuer of command.
     * @param message The name of the player that will be followed.
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
    
    /**
     * Displays the list of stored races.
     * @param name Issuer of the command.
     * @param message Command parameters.
     */
    public void cmd_listRaces(String name, String message) {
        if(m_botAction.getShip().getShip() != Ship.INTERNAL_SPECTATOR) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] This function is disabled when I'm not in spectator mode.");
            return;
        } 
        
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
            m_botAction.sendSmartPrivateMessage(name, "| Name                | Racer name          | Ship      | Length   |");
            for(RecordHeader rec : m_index.values()) {
                // | 19 | 19 | 9 | 8 |
                m_botAction.sendSmartPrivateMessage(name,
                        "| " + Tools.formatString(rec.getTag(), 19)
                        + " | " + Tools.formatString(rec.getRacer(), 19)
                        + " | " + Tools.formatString(Tools.shipName(rec.getShip() + 1), 9)
                        + " | " + Tools.rightString(Tools.getTimeString(rec.getLength(), true), 8)
                        + " |");
            }
        }
    }
    
    /**
     * Loads the index file for this specific arena without displaying it.
     * @param name Player who issued the command.
     * @param message Message parameters.
     */
    public void cmd_loadIndex(String name, String message) {
        try {
            loadIndex();
        } catch (RaceSimException rse) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] " + rse.getMessage());
        }
    }
    
    /**
     * Loads logged data for the current arena from disk.
     * @param name Issuer of command.
     * @param message Original message.
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
                readRecord(m_simData);
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

    public void cmd_ship(String name, String message) {
        if(m_recording || m_logData) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please disable recording mode first.");
        } else if(m_racing) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Cannot change ships while racing.");
        } else if((message == null || message.isEmpty()) && m_simData != null) {
            m_botAction.spectatePlayerImmediately(-1);
            m_botAction.getShip().setShip(m_simData.getShip());
            if(m_simData.getWaypoints() != null && !m_simData.getWaypoints().isEmpty())
                m_botAction.getShip().move(m_simData.getWaypoint(0).getX(), m_simData.getWaypoint(0).getY(), 0, 0);
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
                    if(m_simData != null && m_simData.getWaypoints() != null && !m_simData.getWaypoints().isEmpty())
                        m_botAction.getShip().move(m_simData.getWaypoint(0).getX(), m_simData.getWaypoint(0).getY(), 0, 0);
                    m_botAction.getShip().fire(1);
                }
            } catch(NumberFormatException nfe) {
                m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please provide a valid ship number (1-8).");
            }
        }
    }
    
    /**
     * Puts the bot into spectator mode. This state is needed for several recording functions.
     * @param name Issuer of the command.
     * @param message Original message.
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
     * Puts the bot in a ready mode where it waits for the trigger to happen.
     * When the trigger happens, it will start logging data. 
     * @param name Issuer of command.
     * @param message Original message.
     */
    public void cmd_startRecording(String name, String message) {
        if(m_racing || m_botAction.getShip().getShip() != Ship.INTERNAL_SPECTATOR) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Not in spectator mode.");
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
     * Disables the recording mode and stops any logging of data if this is happening.
     * @param name Issuer of command.
     * @param message Original message.
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
            if(m_recData != null && !m_recData.isEmpty()) {
                m_record.setLength((int) (m_timeStamp - m_startTime) / 1000);
                m_record.setWaypoints(m_recData);
            }
        }
    }
    
    /**
     * Saves the currently logged data to disk.
     * @param name Issuer of command.
     * @param message Optional parameter: "overwrite", will overwrite the file if it already exists.
     */
    public void cmd_storeData(String name, String message) {
        if(m_logData || m_recording) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please disable recording mode first.");
        } else if(m_racing){
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
     * Sets the message that triggers an action by this bot when it's arena'd.
     * @param name Issuer of command.
     * @param message Trigger message.
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
    
    @Override
    public String[] getModHelpMessage() {
        if(m_botAction.getShip().getShip() == Ship.INTERNAL_SPECTATOR) {
            String[] out = {
                    "+------------------------------------------------------------+",
                    "| RaceSimulator v.0.1                       - author ThePAP  |",
                    "+------------------------------------------------------------+",
                    "| How to use:                                                |",
                    "|   Don't use this yet. Still in beta mode...                |",
                    "+------------------------------------------------------------+"
                };
            return out;
        } else {
            String[] out = {
                    "[ERROR] Cannot spam help while in a ship."
            };
            return out;
        }
    }

    @Override
    public boolean isUnloadable() {
        return (!m_logData && !m_recording && !m_racing);
    }
    
    /*
     * File interaction helper functions.
     * 
     * Formatting conventions:
     * 
     * Arena index files:
     * File name: <Arenaname>.txt
     * Formatting: String, \0-delimited, variable length.
     * Details:
     * This file will hold the 'headers' of the recordings made in that specific arena
     * and will be formatted according to the following:
     * <Record_Name>\0<Racer_Name>\0<Shiptype>\0<Length_Recording><Line_Separator>
     * 
     * Race records:
     * File name: arenaname_recordname.dat;
     * Formatting: binary, fixed length fields;
     * Details:
     * Field 0: Version ID, 1 byte;
     * Field 1-end: Repeated data structure (18 bytes wide) with the following properties/fields:
     * - X-coordinate [short]
     * - Y-coordinate [short]
     * - X-velocity [short]
     * - Y-velocity [short]
     * - Direction [byte]
     * - Togglables [byte]
     * - Energy [short]
     * - Bounty [short]
     * - Delta T [integer]
     * 
     */
    private void saveRace(Record rec, boolean overwrite) throws RaceSimException {
        writeRecord(rec, overwrite);
        saveIndex(rec);
    }
    
    private void loadIndex() throws RaceSimException {
        m_index = new HashMap<String, RecordHeader>();
        
        String filename = PATH + m_botAction.getArenaName() + ".txt";
        try {
            File f = new File(filename);
            if(!f.isFile()) {
                m_indexLoaded = true;
                return;
            }
            
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            
            String line = br.readLine();
            if(line == null) {
                m_indexLoaded = true;
                return;
            }
            m_botAction.sendSmartPrivateMessage("ThePAP", "[DEBUG] Line: " + line + "; Version: " + Byte.parseByte(line) + "; " + Integer.parseInt(line));
            switch(Byte.parseByte(line)) {
            case VERSION:
                String[] args;
                while((line = br.readLine()) != null) {
                    args = line.split("\0", 4);
                    if(args.length != 4) {
                        // Malformed data found. For now, just skip it.
                        m_botAction.sendSmartPrivateMessage("ThePAP", "[DEBUG] Arg count invalid: " + args.length);
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
                        m_botAction.sendSmartPrivateMessage("ThePAP", "[DEBUG] Error in args[2] (" + args[2] + ") or [3] (" + args[3] + ")");
                        
                        continue;
                    }
                }
                break;
            default:
                // Unknown version
                br.close();
                fr.close();
                throw new RaceSimException("Unknown version number found.");
            }
            br.close();
            fr.close();
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
    
    private void saveIndex(Record rec) throws RaceSimException {
        if(rec == null) {
            throw new RaceSimException("Record data is null.");
        }
        
        m_index.put(rec.getTag(), new RecordHeader(rec.getTag(), rec.getRacer(), rec.getShip(), rec.getLength()));
        
        String filename = PATH + m_botAction.getArenaName() + ".txt";
        try {
            File f = new File(filename);
            FileWriter fw = new FileWriter(f);
            BufferedWriter bw = new BufferedWriter(fw);
            
            bw.write(Byte.toString(VERSION));
            bw.newLine();
            
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
    
    private void readRecord(Record rec) throws RaceSimException {
        if(rec == null) {
            throw new RaceSimException("Record data is null.");
        }
        
        String filename = PATH + m_botAction.getArenaName() + "_" + rec.getTag() + ".dat";
        try {
            File f = new File(filename);
            if(!f.isFile()) {
                throw new RaceSimException("Record not found on disk: " + filename);
            }
            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);
            m_posData = new ArrayList<WayPoint>();
            switch(bis.read()) {
            case VERSION:
                byte[] data = new byte[18];
                ByteArray bArray;
                int len;
                while((len = bis.read(data)) == 18) {
                    bArray = new ByteArray(data);
                    m_posData.add(new WayPoint(
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
                if(len != -1 && len != 18) {
                    bis.close();
                    fis.close();
                    throw new RaceSimException("Recorded data is corrupt.");
                }
                m_simData.setWaypoints(m_posData);
                break;
            default:
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
     * 
     * @param rec
     * @param overwrite
     * @throws RaceSimException
     */
    private void writeRecord(Record rec, boolean overwrite) throws RaceSimException {
        if(rec == null) {
            throw new RaceSimException("Record data is null.");
        }
        String filename = PATH + m_botAction.getArenaName() + "_" + rec.getTag() +  ".dat";
        try {
            File f = new File(filename);
            if(f.isFile() && !overwrite) {
                throw new RaceSimException("File already exists. Use \"!saveRecord overwrite\" to overwrite file.");
            }
            FileOutputStream fos = new FileOutputStream(f);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            
            ArrayList<WayPoint> waypoints = rec.getWaypoints();
            if(waypoints == null || waypoints.isEmpty()) {
                bos.close();
                fos.close();
                throw new RaceSimException("No waypoints found in recording.");
            }
            
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
     * Helper classes.
     */
    private class Record extends RecordHeader {
        ArrayList<WayPoint> waypoints;
        
        public Record(String tag) {
            super(tag);
            this.ship = Ship.INTERNAL_WARBIRD;
            waypoints = new ArrayList<WayPoint>();
        }

        public Record(RecordHeader rh) {
            super(rh.getTag(), rh.getRacer(), rh.getShip(), rh.getLength());
            this.waypoints = new ArrayList<WayPoint>();
        }
        
        public Record(String tag, String racer, short ship, int length) {
            super(tag, racer, ship, length);
            this.waypoints = new ArrayList<WayPoint>();
        }

        public void setWaypoints(ArrayList<WayPoint> waypoints) {
            this.waypoints.addAll(waypoints);
        }
        
        public ArrayList<WayPoint> getWaypoints() {
            return this.waypoints;
        }

        public WayPoint getWaypoint(int index) {
            if(this.waypoints.contains(index)) {
                return this.waypoints.get(index);
            } else {
                return null;
            }
        }
        
        public boolean contains(int index) {
            return (this.waypoints != null && !this.waypoints.isEmpty() && this.waypoints.size() < index && index >= 0);
        }
    }
    
    private class RecordHeader {
        String tag;
        String racerName;
        short ship;
        int length;
        
        public RecordHeader(String tag) {
            this.tag = tag;
            this.ship = Ship.INTERNAL_WARBIRD;
        }
        
        public RecordHeader(String tag, String racer, short ship, int length) {
            this.tag = tag;
            this.racerName = racer;
            this.ship = ship;
            this.length = length;
        }
        
        public void setRacer(String racer) {
            this.racerName = racer;
        }
        
        public void setShip(short ship) {
            this.ship = ship;
        }
        
        public void setLength(int length) {
            this.length = length;
        }
        
        public String getTag() {
            return this.tag;
        }
        
        public String getRacer() {
            return this.racerName;
        }
        
        public short getShip() {
            return this.ship;
        }
        
        public int getLength() {
            return this.length;
        }
                
        public String toString() {
            String output = this.tag + '\0'
                    + this.racerName + '\0'
                    + this.ship + '\0'
                    + Integer.toString(this.length);
            
            return output;
        }
    }
    
    private class WayPoint {
        private short wp_x;
        private short wp_y;
        private short wp_vx;
        private short wp_vy;
        private byte wp_dir;
        private byte wp_tog;
        private short wp_ene;
        private short wp_bty;
        private int wp_dt;
        
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
                wp_dt = 0;
            } else if(event.getTimeStamp() < timestamp ) {
                // Overflow scenario
                wp_dt = (65535 + event.getTimeStamp() - timestamp) * 10;
            } else {
                wp_dt = (event.getTimeStamp() - timestamp) * 10;
                
            }
        }
        
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

        public short getX() {
            return wp_x;
        }

        public short getY() {
            return wp_y;
        }

        public short getVx() {
            return wp_vx;
        }

        public short getVy() {
            return wp_vy;
        }

        public byte getDirection() {
            return wp_dir;
        }
        
        public byte getToggables() {
            return wp_tog;
        }
        
        public short getEnergy() {
            return wp_ene;
        }
        
        public short getBounty() {
            return wp_bty;
        }

        public int getDT() {
            return wp_dt;
        }
        
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
    
    private class MoveTask extends TimerTask {
        private int index;
        
        public MoveTask(int index) {
            this.index = index;
        }
        
        @Override
        public void run() {
            if(!m_racing || m_simData == null || !m_simData.contains(index)) {
                m_botAction.getShip().move(m_botAction.getShip().getX(), m_botAction.getShip().getY(), 0, 0);
                return;
            }
            
            WayPoint wp = m_simData.getWaypoint(index);
            m_botAction.getShip().move(
                    wp.getDirection(),
                    wp.getX(),
                    wp.getY(),
                    wp.getVx(),
                    wp.getVy(),
                    wp.getToggables(),
                    wp.getEnergy(),
                    wp.getBounty());
            m_botAction.scheduleTask(new MoveTask(++index), wp.getDT());
        }
    }
    
    private class RaceSimException extends Exception {
        /**
         * Auto generated serial ID.
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
