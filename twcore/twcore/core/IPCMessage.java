package twcore.core;

public class IPCMessage
{
  private String message;
  private String sender;
  private String recipient;

  public IPCMessage(String message)
  {
    this.message = message;
    sender = null;
    recipient = null;
  }

  public IPCMessage(String message, String recipient)
  {
    this.message = message;
    this.recipient = recipient;
    sender = null;
  }

  public IPCMessage(String message, String recipient, String sender)
  {
    this.message = message;
    this.recipient = recipient;
    this.sender = sender;
  }

  public String getMessage()
  {
    return message;
  }

  public String getSender()
  {
    return sender;
  }

  public String getRecipient()
  {
    return recipient;
  }
}