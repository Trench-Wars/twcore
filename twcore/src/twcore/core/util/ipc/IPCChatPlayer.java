package twcore.core.util.ipc;

import twcore.core.game.Player;


public class IPCChatPlayer extends IPCChat {
	private Player player;		// Player
	private String action;
	// Should be one of ENTERED, LEFT, FREQCHANGE, FREQSHIPCHANGE, SCOREUPDATE, 
	
	/**
	 * Constructor of IPCChatPlayer.
	 * 
	 * @param arena the arena the player is in
	 * @param player the player
	 * @param ipcSender the sender of this IPC message
	 * @param ipcRecipient the intended recipient of this IPC message
	 */
	public IPCChatPlayer(
			String arena, 
			Player player,
			String action,
			String ipcSender, 
			String ipcRecipient) {
		super(arena, ipcSender, ipcRecipient);
		
		this.player = player;
		this.action = action;
	}

	/**
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}
	
	

	
}
