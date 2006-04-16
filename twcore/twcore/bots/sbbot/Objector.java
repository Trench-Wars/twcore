package twcore.bots.strikeballbot;

public abstract class Objector {
    public Objector() { }
    public abstract Objection getObjection(MessageType type, Message message);
}