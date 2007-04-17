package twcore.bots.sbbot;

public abstract class Objector {
    public Objector() { }
    public abstract Objection getObjection(MessageType type, Message message);
}