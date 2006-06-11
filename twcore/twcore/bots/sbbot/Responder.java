package twcore.bots.sbbot;

public abstract class Responder<T> {
    public Responder() { }

    public abstract Response getResponse(MessageType type, Message message);

}