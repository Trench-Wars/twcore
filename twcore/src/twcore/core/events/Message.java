package twcore.core.events;

import twcore.core.util.ByteArray;
import twcore.core.util.Tools;

/**
 * (S2C 0x07) Event fired when a chat message is received. <code><pre>
 * +------------------------------+
 * | Offset  Length  Description  |
 * +------------------------------+
 * | 0       1       Type Byte    |
 * | 1       1       Chat Type    |
 * | 2       1       Sound Byte   |
 * | 3       2       Sender ID    |
 * | 5       *       Chat Message |
 * +------------------------------+</code></pre>
 * 
 * Chat message is null-terminated ('\0')
 */
public class Message extends SubspaceEvent {

    //Variable Declarations
    String m_message;
    String m_messager;
    short m_playerID;
    byte m_soundCode;
    int m_chatNumber;
    int m_messageType;
    String m_alertCommandType;

    //Public Field Declarations
    public static final int ARENA_MESSAGE = 0x01;
    public static final int PUBLIC_MACRO_MESSAGE = 0x02;
    public static final int PUBLIC_MESSAGE = 0x04;
    public static final int TEAM_MESSAGE = 0x08;
    public static final int OPPOSING_TEAM_MESSAGE = 0x10;
    public static final int PRIVATE_MESSAGE = 0x20;
    public static final int WARNING_MESSAGE = 0x40;
    public static final int REMOTE_PRIVATE_MESSAGE = 0x80;
    public static final int SERVER_ERROR = 0x100;
    public static final int CHAT_MESSAGE = 0x200;
    public static final int ALERT_MESSAGE = 0x400;
    public static String[] alertCommands = null;

    /**
     * Creates a new instance of Message, this is called by GamePacketInterpreter when it receives the packet
     * 
     * @param array
     *            the ByteArray containing the packet data
     */
    public Message(ByteArray array) {
        int nameEnding;
        int nameBeginning;
        String stop = "0";
        try {
            m_chatNumber = 0;
            m_messager = null;
            m_messageType = (1 << (int) array.readByte(1));
            m_soundCode = array.readByte(2);
            m_playerID = array.readLittleEndianShort(3);
            m_message = array.readString(5, array.size() - 6);

            if (m_messageType == Message.ARENA_MESSAGE) { 
                if (m_message.startsWith("misc:alertcommand:")) {
                    alertCommands = m_message.substring(18).split(",");
                    for (int i = 0; i < alertCommands.length; i++) {
                        if (m_message.startsWith(alertCommands[i] + ':')) {
                            m_messageType = ALERT_MESSAGE;
                            m_alertCommandType = alertCommands[i];
                            stop += "1";
                            break;
                        }
                    }
                } else if(m_message.startsWith("IP:") && m_message.contains("TypedName:")) {
                    stop += "A";
                    // Trim spaces down in TypedName to make it all match the other server commands.
                    String[] message = new String[3];
                    message[0] = m_message.substring(0, m_message.indexOf("TypedName:") + 11);
                    message[1] = m_message.substring(m_message.indexOf("TypedName:") + 10, m_message.indexOf("  Demo:"));
                    message[2] = m_message.substring(m_message.indexOf("  Demo:"));
                    
                    stop += "B";
                    
                    message[1].replaceAll(" +", " ");
                    m_message = message[0] + message[1] + message[2];
                    
                    stop += "C";
                }
                stop += "2";
            } else {
                // The parsing of the name from the remote private message, private message, and chat message
                // could possibly inaccurate.  The message isn't meant to have the name parsed out of it.
                if (m_messageType == REMOTE_PRIVATE_MESSAGE) {
                    if (alertCommands != null) {
                        for (int i = 0; i < alertCommands.length; i++) {
                            if (m_message.startsWith(alertCommands[i] + ':')) {
                                m_messageType = ALERT_MESSAGE;
                                m_alertCommandType = alertCommands[i];
                                stop += "3";
                                break;
                            }
                        }
                    }

                    stop += "4";
                    if (m_messageType == REMOTE_PRIVATE_MESSAGE) {
                        nameBeginning = m_message.indexOf('(');
                        nameEnding = m_message.indexOf(")>");
                        m_messager = m_message.substring(nameBeginning + 1, nameEnding);
                        m_message = m_message.substring(nameEnding + 2);
                        stop += "5";
                    } else {
                        nameBeginning = m_message.indexOf('(');
                        nameEnding = m_message.indexOf(") (");
                        m_messager = m_message.substring(nameBeginning + 1, nameEnding);
                        m_message = m_message.substring(nameEnding + 2);
                        stop += "6";
                    }
                } else if (m_messageType == CHAT_MESSAGE) {
                    stop += "7";
                    m_chatNumber = Integer.valueOf(m_message.substring(0, 1)).intValue();
                    nameBeginning = 2;
                    nameEnding = m_message.indexOf("> ");
                    if (nameEnding == -1) {
                        m_messager = m_message.substring(nameBeginning);
                        m_message = "";
                        stop += "8";
                    } else {
                        m_messager = m_message.substring(nameBeginning, nameEnding);
                        m_message = m_message.substring(nameEnding + 2);
                        stop += "9";
                    }
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println("OutOfBounds: " + stop);
            Tools.printLog("OutOfBounds: " + stop);
            Tools.printStackTrace("OutOfBounds: " + stop, e);
        }
    }

    /**
     * This gets the ID of the player that sent the message
     * 
     * @return the sender's ID
     */
    public short getPlayerID() {

        return m_playerID;
    }

    /**
     * This gets the sound code (ie %12) that was sent along with the message
     * 
     * @return the code of the sound that was sent (no sound = 0)
     */
    public byte getSoundCode() {

        return m_soundCode;
    }

    /**
     * This gets the type of message that was sent
     * 
     * @return the message type
     */
    public int getMessageType() {

        return m_messageType;
    }

    /**
     * This gets the message that was sent
     * 
     * @return A string containing the message
     */
    public String getMessage() {

        return m_message;
    }

    /**
     * This gets the name of the player that sent the message
     * 
     * @return A string containing the sender's name
     */
    public String getMessager() {

        return m_messager;
    }

    /**
     * This gets the alert command that was sent (ie "?cheater")
     * 
     * @return A string containing the alert command
     */
    public String getAlertCommandType() {

        return m_alertCommandType;
    }

    /**
     * This gets the number of the chat channel the message is on
     * 
     * @return the channel number of the message
     */
    public int getChatNumber() {

        return m_chatNumber;
    }

    /**
     * Special use only.
     * 
     * @param name
     *            The sender's name to set the message to
     */
    public void setMessager(String name) {
        m_messager = name;
    }
}
