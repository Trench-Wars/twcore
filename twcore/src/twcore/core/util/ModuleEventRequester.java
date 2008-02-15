package twcore.core.util;

import twcore.core.EventRequester;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Manages the requesting of events for a module-based bot by requesting and denying
 * events on the fly, as modules are loaded and unloaded.  If two modules request an
 * event, both must be unloaded before the event is denied again.
 * <p>
 * The bot should instantiate ModuleEventRequester with a reference to EventRequester.
 * <p>
 * All modules need to be given a reference to ModuleEventRequester.  They should
 * request their events by using request(this, eventNum) when they are loaded, and
 * releaseAll(this) when they are unloaded.  All bots still need stub methods
 * in their module interface for events they do not handle.
 * <p>
 * The Message event can not be requested or release.  If a module attempts to
 * request or release Message, a message is sent to the log.
 * 
 * @author gdugwyler
 */
public class ModuleEventRequester {
    EventRequester eventReq;
    ArrayList <Vector<Object>>requested = new ArrayList<Vector<Object>>();
    
    public ModuleEventRequester( EventRequester eventReq ) {
        this.eventReq = eventReq;        
        for(int i = 0; i < EventRequester.TOTAL_NUMBER; i++ )
            requested.add( new Vector<Object>() );        
        eventReq.request(EventRequester.MESSAGE);  // Default, must always be requested
        eventReq.request(EventRequester.KOTH_RESET);
    }
    
    /**
     * Requests an event be requested, and registers the request with
     * a specific module. 
     * @param module Module requesting
     * @param eventNum Event number as defined in EventRequester
     */
    public void request( Object module, int eventNum ) {
        if( eventNum == EventRequester.MESSAGE ) {
            Tools.printLog("Module attempted to request Message event: unnecessary; requested by default" );
            return;
        }
        if( eventNum > EventRequester.MESSAGE && eventNum < EventRequester.TOTAL_NUMBER ) {
            Vector <Object>event = requested.get(eventNum);
            event.add( module );
            eventReq.request( eventNum );
        }
    }

    /**
     * Releases (declines) an event, informing ModuleEventRequester
     * that it is no longer needed by a specific module.  If no other modules
     * are using the event, it is then declined from EventRequester.
     * @param module Module releasing
     * @param eventNum Event number as defined in EventRequester
     */
    public void release( Object module, int eventNum ) {
        if( eventNum == EventRequester.MESSAGE ) {
            Tools.printLog("Module attempted to unrequest Message event: Message event must always be requested" );
            return;
        }
        if( eventNum > EventRequester.MESSAGE && eventNum < EventRequester.TOTAL_NUMBER ) {
            Vector <Object>event = requested.get(eventNum);
            event.remove(module);
            if( event.size() == 0 )
                eventReq.decline( eventNum );
        }
    }
    
    /**
     * Releases all events a given module is requesting.
     * @param module Module releasing
     */
    public void releaseAll( Object module ) {
        for(int i = 1; i < EventRequester.TOTAL_NUMBER; i++ )
            release( module, i );
    }
}