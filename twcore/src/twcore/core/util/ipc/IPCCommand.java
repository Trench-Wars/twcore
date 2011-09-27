package twcore.core.util.ipc;

/**
 *
 * @author WingZero
 */
public class IPCCommand {

    Command type;
    String bot;
    String command;
    
    public IPCCommand(Command cmd, String msg) {
        type = cmd;
        command = msg;
        bot = null;
    }
    
    public IPCCommand(Command cmd, String toBot, String msg) {
        type = cmd;
        bot = toBot;
        command = msg;
    }
    
    public Command getType() {
        return type;
    }
    
    public String getCommand() {
        return command;
    }
    
    public String getBot() {
        return bot;
    }
    
}
