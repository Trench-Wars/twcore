package twcore.core.command;

/**
 * The purpose of this new exception class is to make command handling
 * with CommandInterpreter easier.  When you instantiate CommandInterpreter,
 * specify (BotAction, true) to catch and PM to the user any TWCoreExceptions
 * that are caught.  Then when you have reached a place in your command's code
 * where you would normally PM something to the user and then return, throw a
 * new TWCoreException instead:
 * <code>
 * if( !isRunning )
 *     throw new TWCoreException( "The game has not yet started." );</code>
 * The CommandInterpreter will PM it to them automatically.
 *
 * See DistensionBot for an example of how this is used.
 */
public class TWCoreException extends RuntimeException {
    static final long serialVersionUID = -1092839081278481237L;

    /**
     * Constructs a new TWCore exception with the specified detail message.
     * The purpose of constructing a TWCoreException is to make command
     * handling with CommandInterpreter easier.  When you instantiate
     * CommandInterpreter, specify (BotAction, true) to catch and PM to the user
     * any TWCoreExceptions that are caught.  Then when you have reached a place
     * in your command's code where you would normally PM something to the user
     * and then return, throw a new TWCoreException instead:
     * <code>
     * if( !isRunning )
     *     throw new TWCoreException( "The game has not yet started." );</code>
     * The CommandInterpreter will PM it to them automatically.
     *
     * @param   message   the detail message. The detail message is saved for
     *          later retrieval by the {@link #getMessage()} method.
     */
    public TWCoreException(String message) {
        super(message);
    }
}
