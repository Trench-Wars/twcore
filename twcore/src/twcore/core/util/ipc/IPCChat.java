package twcore.core.util.ipc;

/**
 * IPC chat is the base class for all IPC messages made for pubhubchat / pubbotchat
 * 
 * @author MMaverick
 * @see IPCChatMessage
 * @see IPCChatPlayer
 * @see IPCChatArena
 */
public class IPCChat {
	private String arena;		// Arena
	private String ipcSender;	   // Sender of the IPC message
	private String ipcRecipient;   // Intended recipient of the message
	
	/**
	 * This constructor fills the new IPCChat with the arena, ipcSender and ipcRecipient
	 * @param arena the arena where the message originated from
	 * @param ipcSender the sender of this IPC message
	 * @param ipcRecipient the intended recipient of this IPC message
	 */
	public IPCChat(String arena, String ipcSender, String ipcRecipient) {
		this.arena = arena;
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
}
