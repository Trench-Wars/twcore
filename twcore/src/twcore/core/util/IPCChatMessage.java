package twcore.core.util;


public class IPCChatMessage {
	private String arena;		// Arena
	private int messageType;	// Message type
	private String sender;      // Sender of the message
	private String message;     // Message being sent
	
	private String ipcSender;	   // Sender of the IPC message
	private String ipcRecipient;   // Intended recipient of the message
	  
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
		this.arena = arena;
		this.messageType = messageType;
		this.sender = sender;
		this.message = message;
		
		this.ipcSender = ipcSender;
		this.ipcRecipient = ipcRecipient;
	}

	/**
	 * @return the arena
	 */
	public String getArena() {
		return arena;
	}

	/**
	 * @return the ipcRecipient
	 */
	public String getIpcRecipient() {
		return ipcRecipient;
	}

	/**
	 * @return the ipcSender
	 */
	public String getIpcSender() {
		return ipcSender;
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
