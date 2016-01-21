package twcore.core.lag;

import java.util.Vector;

import twcore.core.events.SubspaceEvent;

/**
    This class handles events that are inherited from SubspaceEvent.  It provides
    functionality to add new EventListeners and to pass a SubspaceEvent to all of
    them.  The EventListeners will be called in a FIFO manner, meaning that the
    first EventListener to be added will be able to handle the event first.  The
    way to change how the events are handled is to extend a class from
    EventListener and override the appropriate handleEvent methods.  After this,
    the class is added to the EventHandler.  In the main bot class, all of the
    handleEvent methods must call the EventHandlers handleEvent method passing in
    the event that is triggered.
*/

public class EventHandler {
    private Vector<EventListener> eventListeners;

    /**
        This method initializes the EventHandler class.
    */

    public EventHandler() {
        eventListeners = new Vector<EventListener>();
    }

    /**
        This method adds an event listener to the event handler.

        @param eventListener is the EventListener to add.
        @throws IllegalArgumentException if the EventListener is null.
    */

    public void addEventListener(EventListener eventListener) {
        if (eventListener == null)
            throw new IllegalArgumentException("ERROR: EventListener can not be null.");

        eventListeners.add(eventListeners.size(), eventListener);
    }

    /**
        This method handles a SubspaceEvent and distributes it to all of the
        EventListeners.

        @param event is the SubspaceEvent to handle.
    */

    public void handleEvent(SubspaceEvent event) {
        EventListener eventListener;

        for (int index = 0; index < eventListeners.size(); index++) {
            eventListener = eventListeners.get(index);
            eventListener.handleEvent(event);
        }
    }
}
