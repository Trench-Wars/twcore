package twcore.bots.sbbot;

public abstract class SBEventListener {
    public SBEventListener() {}
    public abstract void notify(SBEventType type, SBEvent event);
}