package twcore.bots.multibot.racesim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Ship;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

public class racesim extends MultiModule {
    
    private static final String m_path = "twcore/bots/multibot/racesim/records/";

    private CommandInterpreter m_commandInterpreter;
    private ArrayList<WayPoint> m_posData;
    
    private String m_playerName = "";
    private String m_trigger = "";
    
    private long m_startTime = 0;
    private int m_timeStamp = 0;
    
    private short m_trackID = -1;
    
    private byte m_trackShip = 0;
    
    private boolean m_logData = false;
    private boolean m_racing = false;
    private boolean m_recording = false;
    
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
        m_commandInterpreter.registerCommand("!startrec", acceptedMessages, this, "cmd_startRecording");
        m_commandInterpreter.registerCommand("!stoprec", acceptedMessages, this, "cmd_stopRecording");
        m_commandInterpreter.registerCommand("!loadrace", acceptedMessages, this, "cmd_loadRace");
        m_commandInterpreter.registerCommand("!saverace", acceptedMessages, this, "cmd_saveRace");
        m_commandInterpreter.registerCommand("!settrigger", acceptedMessages, this, "cmd_setTrigger");
        m_commandInterpreter.registerCommand("!ship", acceptedMessages, this, "cmd_ship");
        m_commandInterpreter.registerCommand("!spec", acceptedMessages, this, "cmd_spec");
        m_commandInterpreter.registerCommand("!follow", acceptedMessages, this, "cmd_follow");

    }
    
    @Override
    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request(this, EventRequester.PLAYER_POSITION);
    }
    
    @Override
    public void handleEvent(Message event) {
        m_commandInterpreter.handleEvent(event);
        
        if(event.getMessageType() == Message.ARENA_MESSAGE) {
            decideAction(event.getMessage());
        }
    }
    
    public void handleEvent(PlayerPosition event) {
        if(m_logData && event.getPlayerID() == m_trackID) {
            m_posData.add(new WayPoint(event, m_timeStamp));
            if(m_timeStamp == 0) {
                m_startTime = System.currentTimeMillis();
                m_trackShip = m_botAction.getPlayer(m_trackID).getShipType();
            }
            m_timeStamp = event.getTimeStamp();
            m_botAction.spectatePlayer(m_trackID);
        }
            
    }

    public void decideAction(String msg) {
        if(m_trigger == null || m_trigger.isEmpty() || !m_trigger.equalsIgnoreCase(msg))
            return;
        
        if(m_recording && m_botAction.getShip().getShip() == Ship.INTERNAL_SPECTATOR && m_trackID != -1) {
            m_posData = new ArrayList<WayPoint>();
            m_logData = true;
        } else if(m_botAction.getShip().getShip() != Ship.INTERNAL_SPECTATOR) {
            m_botAction.scheduleTask(new MoveTask(), 0);
            m_racing = true;
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
     * Loads logged data for the current arena from disk.
     * @param name Issuer of command.
     * @param message Original message.
     */
    public void cmd_loadRace(String name, String message) {
        if(m_logData || m_recording) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please disable recording mode first.");
        } else if(m_racing) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Cannot load data while racing.");
        } else {
            try {
                FileReader fr = new FileReader(m_path + "racesim_" + m_botAction.getArenaName() + ".dat");
                BufferedReader br = new BufferedReader(fr);
                m_posData = new ArrayList<WayPoint>();
                String data;
                int corruptData = 0;
                int wpRead = 0;
                data = br.readLine();
                if(data != null) {
                    try {
                        m_trackShip = Byte.parseByte(data);
                    } catch(NumberFormatException nfe) {
                        m_botAction.sendSmartPrivateMessage(name, "[ERROR] Incorrect header.");
                        m_trackShip = 1;
                    }
                }
                while((data = br.readLine()) != null) {
                    String[] splitData = data.split(":", 9);
                    try {
                        if(splitData.length == 9) {
                            m_posData.add(new WayPoint(
                                    Short.parseShort(splitData[0]),
                                    Short.parseShort(splitData[1]),
                                    Short.parseShort(splitData[2]),
                                    Short.parseShort(splitData[3]),
                                    Byte.parseByte(splitData[4]),
                                    Byte.parseByte(splitData[5]),
                                    Short.parseShort(splitData[6]),
                                    Short.parseShort(splitData[7]),
                                    Integer.parseInt(splitData[8])));
                            wpRead++;
                        } else {
                            corruptData++;
                        }
                    } catch (NumberFormatException nfe) {
                        corruptData++;
                    }
                }
                br.close();
                fr.close();
                if(corruptData == 0) {
                    m_botAction.sendSmartPrivateMessage(name, "File successfully read. Loaded " + wpRead + " data points.");
                } else {
                    m_botAction.sendSmartPrivateMessage(name, "[ERROR] Data possibly corrupt. Faulty entries: " + corruptData + "; Total entries: " + (corruptData + wpRead));
                }
                if(m_posData != null && !m_posData.isEmpty())
                    m_botAction.getShip().move(m_posData.get(0).getX(), m_posData.get(0).getY());
            } catch(IOException ioe) {
                m_botAction.sendSmartPrivateMessage(name, "[ERROR] Unable to find data on disk.");
                Tools.printStackTrace(ioe);
            }
        }
    }
    
    /**
     * Saves the currently logged data to disk.
     * @param name Issuer of command.
     * @param message Original message.
     */
    public void cmd_saveRace(String name, String message) {
        if(m_logData || m_recording) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please disable recording mode first.");
        } else if(m_racing){
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Cannot change data while racing.");
        } else if(m_posData == null || m_posData.isEmpty()) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] No data to be saved.");
        } else {
            try {
                FileWriter fw = new FileWriter(m_path + "racesim_" + m_botAction.getArenaName() + ".dat");
                fw.write(m_trackShip + System.lineSeparator());
                WayPoint wp;
                while(!m_posData.isEmpty()) {
                    wp = m_posData.remove(0);
                    fw.write(wp.getX()
                            + ":" + wp.getY()
                            + ":" + wp.getVx()
                            + ":" + wp.getVy()
                            + ":" + wp.getDirection()
                            + ":" + wp.getToggables()
                            + ":" + wp.getEnergy()
                            + ":" + wp.getBounty()
                            + ":" + wp.getDT()
                            + System.lineSeparator());
                }
                fw.close();
                m_botAction.sendSmartPrivateMessage(name, "Data has been stored to disk.");
            } catch (IOException ioe) {
                m_botAction.sendSmartPrivateMessage(name, "[ERROR] Unable to write data to file.");
                Tools.printStackTrace(ioe);
            }
        }
    }
    
    /**
     * Sets the message that triggers an action by this bot when it's arena'd.
     * @param name Issuer of command.
     * @param message Trigger message.
     */
    public void cmd_setTrigger(String name, String message) {
        if(message == null || message.isEmpty()) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please provide a valid trigger.");
        } else {
            m_trigger = message;
            m_botAction.sendSmartPrivateMessage(name, "I will start recording or racing when I see the following arena message:");
            m_botAction.sendSmartPrivateMessage(name, m_trigger);
        }
    }
    
    
    public void cmd_ship(String name, String message) {
        if(m_recording || m_logData) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please disable recording mode first.");
        } else if(m_racing) {
            m_botAction.sendSmartPrivateMessage(name, "[ERROR] Cannot change ships while racing.");
        } else if(message == null || message.isEmpty()) {
            m_botAction.spectatePlayerImmediately(-1);
            m_botAction.getShip().setShip(m_trackShip - 1);
            if(m_posData != null && !m_posData.isEmpty())
                m_botAction.getShip().move(m_posData.get(0).getX(), m_posData.get(0).getY(), 0, 0);
            m_botAction.getShip().fire(1);
        } else {
            try {
                int shipNumber = Integer.parseInt(message);
                if(shipNumber < 1 || shipNumber > 8) {
                    m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please provide a valid ship number (1-8).");
                } else {
                    m_botAction.spectatePlayerImmediately(-1);
                    m_botAction.getShip().setShip(shipNumber - 1);
                    if(m_posData != null && !m_posData.isEmpty())
                        m_botAction.getShip().move(m_posData.get(0).getX(), m_posData.get(0).getY(), 0, 0);
                    m_botAction.getShip().fire(1);
                }
            } catch(NumberFormatException nfe) {
                m_botAction.sendSmartPrivateMessage(name, "[ERROR] Please provide a valid ship number (1-8).");
            }
        }
    }
    
    
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
        } else {
            m_botAction.spectatePlayer(m_trackID);
            m_recording = true;
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
    }
    
    private class MoveTask extends TimerTask {
        @Override
        public void run() {
            if(m_posData == null || m_posData.isEmpty() || !m_racing) {
                m_botAction.getShip().move(m_botAction.getShip().getX(), m_botAction.getShip().getY(), 0, 0);
                return;
            }
            
            WayPoint wp = m_posData.remove(0);
            m_botAction.getShip().move(
                    wp.getDirection(),
                    wp.getX(),
                    wp.getY(),
                    wp.getVx(),
                    wp.getVy(),
                    wp.getToggables(),
                    wp.getEnergy(),
                    wp.getBounty());
            m_botAction.scheduleTask(new MoveTask(), wp.getDT());
        }
    }

}
