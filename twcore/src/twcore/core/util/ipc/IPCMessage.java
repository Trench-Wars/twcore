package twcore.core.util.ipc;

/**
    Used as a transmitter of messages between bots, using the IPC protocol.
    Very simple and lightweight class.

    @author  harvey
*/
public class IPCMessage
{
    private String message;       // Message being sent
    private String sender;        // Sender of the message
    private String recipient;     // Intended recipient of the message

    /**
        IPCMessage constructor given only a message.
        @param message String of message to create
    */
    public IPCMessage(String message)
    {
        this.message = message;
        sender = null;
        recipient = null;
    }

    /**
        IPCMessage constructor given only a message and recipient.
        @param message String of message to create
        @param recipient String of recipient to send to
    */
    public IPCMessage(String message, String recipient)
    {
        this.message = message;
        this.recipient = recipient;
        sender = null;
    }

    /**
        Standard IPCMessage constructor.
        @param message String of message to create
        @param recipient String of recipient to send to
        @param sender String of person sending message
    */
    public IPCMessage(String message, String recipient, String sender)
    {
        this.message = message;
        this.recipient = recipient;
        this.sender = sender;
    }

    /**
        Return the text of this message.
        @return String containing the message text.
    */
    public String getMessage()
    {
        return message;
    }

    /**
        Return the sender of this message.
        @return String containing the message sender.
    */
    public String getSender()
    {
        return sender;
    }

    /**
        Return the recipient of this message.
        @return String containing the message recipient.
    */
    public String getRecipient()
    {
        return recipient;
    }
}