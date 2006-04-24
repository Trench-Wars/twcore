/*
 * Created on Jan 29, 2005
 */
package twcore.bots.zhbot;

import twcore.core.Message;
import twcore.core.PlayerPosition;

/**
 * @author 2dragons
 */
public class twbotsafes extends TWBotExtension {

	private boolean	m_safeSpec;
	
	public twbotsafes() {
		
		m_safeSpec = false;
	}
	
	public String[] getHelpMessages() {
		
		String message[] = {
				"!safespec -          Toggle speccing for going in safety."
		};
		
		return message;
	}

	/** This method handles message events.
	 * @param event is the message event.
	 */
	public void handleEvent(Message event) {
		
		int senderID = event.getPlayerID();
		String sender = m_botAction.getPlayerName(senderID);
		String command = event.getMessage().toLowerCase().trim();
		
		if( event.getMessageType() == Message.PRIVATE_MESSAGE && m_opList.isER(sender) )
			handleCommand( sender, command );
	}
	
	public void handleCommand( String sender, String command ) {

		if( command.startsWith( "!safespec" ) )
			toggleSafeSpec( sender );
	}
	
	public void handleEvent( PlayerPosition _event ) {
		
		String name = m_botAction.getPlayerName( _event.getPlayerID() );
		
		if( m_safeSpec && _event.isInSafe() ) {
			
			m_botAction.spec( _event.getPlayerID() );
			m_botAction.spec( _event.getPlayerID() );
			m_botAction.sendArenaMessage( name + " has been specced for going into a safe area." );
		}
	}
	
	private void toggleSafeSpec( String sender ) {
		
		m_safeSpec = !m_safeSpec;
		
		if( m_safeSpec ) {
			m_botAction.sendSmartPrivateMessage( sender, "Safe speccing enabled." );
		} else {
			m_botAction.sendSmartPrivateMessage( sender, "Safe speccing disabled." );
		}
	}
	
	public void cancel() {
	}

}
