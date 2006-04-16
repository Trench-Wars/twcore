package twcore.bots.strikeballbot;

public abstract class Responder<T> {
    public void Responder() { }

    public abstract Response getResponse(MessageType type, Message message);

}