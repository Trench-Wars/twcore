package twcore.core;

/**
 * IPCMessage
 * 
 * Representing class of messages of the Inter-process Communication protocol of TWCore.
 * Used to transmit messages between bots.    
 * 
 */
public class IPCMessage
{
  private String message;
  private String sender;
  private String recipient;

  /**
   * IPCMessage constructor given only a message.
   * @param message String of message to create
   */
  public IPCMessage(String message)
  {
    this.message = message;
    sender = null;
    recipient = null;
  }

  /**
   * IPCMessage constructor given only a message and recipient.
   * @param message String of message to create
   * @param recipient String of recipient to send to
   */
  public IPCMessage(String message, String recipient)
  {
    this.message = message;
    this.recipient = recipient;
    sender = null;
  }

  /**
   * Standard IPCMessage constructor.
   * @param message String of message to create
   * @param recipient String of recipient to send to
   * @param sender String of person sending message
   */
  public IPCMessage(String message, String recipient, String sender)
  {
    this.message = message;
    this.recipient = recipient;
    this.sender = sender;
  }

  /**
   * Return the text of this message.
   * @return String containing the message text.
   */
  public String getMessage()
  {
    return message;
  }

  /**
   * Return the sender of this message.
   * @return String containing the message sender.
   */
  public String getSender()
  {
    return sender;
  }

  /**
   * Return the recipient of this message.
   * @return String containing the message recipient.
   */
  public String getRecipient()
  {
    return recipient;
  }
}