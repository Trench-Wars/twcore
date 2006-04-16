package twcore.bots.sbbot;
import twcore.core.*;

public abstract class SBEventListener {
    public SBEventListener() {}
    public abstract void notify(SBEventType type, SBEvent event);
}