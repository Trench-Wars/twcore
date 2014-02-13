package twcore.core.util.ipc;

/**
 * Class used to send a specific command to another bot, with specific type enforced.
 *
 * @author WingZero
 */
public class IPCCommand {

    Command type;               // Type of command to send
    String bot;                 // Name of bot this is being sent to. MAY BE NULL
    String commandDescription;  // Description of the command being sent
    
    public IPCCommand(Command cmd, String msg) {
        type = cmd;
        commandDescription = msg;
        bot = null;
    }
    
    public IPCCommand(Command cmd, String toBot, String msg) {
        type = cmd;
        bot = toBot;
        commandDescription = msg;
    }
    
    /**
     * @return The actual Command associated with this IPCCommand.
     */
    public Command getType() {
        return type;
    }
    
    /**
     * @return The String-based command sent -- usually just a description.
     */
    public String getCommand() {
        return commandDescription;
    }
    
    /**
     * @return The name of the bot this IPCCommand is intended for. CHECK FOR NULL
     */
    public String getBot() {
        return bot;
    }
    
}
