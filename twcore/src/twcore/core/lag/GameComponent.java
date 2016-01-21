package twcore.core.lag;

import twcore.core.BotAction;
import twcore.core.events.SubspaceEvent;

/**
    This class models a base GameComponent.  It bleh im sleepy...
*/

public abstract class GameComponent {
    private LogWriter logFile;
    private EventHandler eventHandler;
    private boolean enabled;
    protected BotAction ba;

    /**
        This method intializes a new GameComponent.  It sets the component up for
        logging to a log file.

        @param botAction is the BotAction instance of the bot.
        @param logFile is the log file to write to.
    */

    public GameComponent(BotAction botAction, LogWriter logFile) {
        ba = botAction;
        this.logFile = logFile;
        eventHandler = new EventHandler();
        enabled = true;
    }

    /**
        This method checks to see if the GameComponent is enabled.

        @return true is returned if it is enabled.
    */

    public boolean isEnabled() {
        return enabled;
    }

    /**
        This method sets the enabled status of the GameComponent.  If it is set to
        false then it will not process any events.

        @param status is the new status of the GameComponent.
    */

    public void setEnabled(boolean status) {
        enabled = status;
    }

    /**
        This method handles the events that the bot receives.

        @param event is the SubspaceEvent to handle.
    */

    public void handleEvent(SubspaceEvent event) {
        if (enabled)
            eventHandler.handleEvent(event);
    }

    /**
        This protected method records a message in the logFile.  If the PlayerInfo
        class is not logging (the logFile is null) then nothing is recorded.  The
        log entry is in the following format:

        @param message is the message that is to be recorded in the log entry.
    */

    protected void recordEvent(String message) {
        if (logFile != null)
            logFile.write(message);
    }

    /**
        This method adds an event listener to the PlayerInfo class.

        @param eventListener is the EventListener to add.
    */

    protected void addEventListener(EventListener eventListener) {
        eventHandler.addEventListener(eventListener);
    }
}
