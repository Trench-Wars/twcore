package twcore.core.util.ipc;


public class IPCChatMessage extends IPCChat {
	private int messageType;	// Message type
	private String sender;      // Sender of the message
	private String message;     // Message being sent
	
	/**
	 * Constructor
	 */
	public IPCChatMessage(
			String arena, 
			int messageType, 
			String sender, 
			String message, 
			String ipcSender, 
			String ipcRecipient)
	{
		super(arena, ipcSender, ipcRecipient);
		
		this.messageType = messageType;
		this.sender = sender;
		this.message = message;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the messageType
	 */
	public int getMessageType() {
		return messageType;
	}

	/**
	 * @return the sender
	 */
	public String getSender() {
		return sender;
	}

	
}
