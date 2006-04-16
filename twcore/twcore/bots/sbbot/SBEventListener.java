package twcore.bots.strikeballbot;
import twcore.core.*;

public abstract class SBEventListener {
    public SBEventListener() {}
    public abstract void notify(SBEventType type, SBEvent event);
}