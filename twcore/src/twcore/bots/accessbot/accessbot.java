package twcore.bots.accessbot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.stats.DBPlayerData;

/**
 * Used for updating staff access to the TW website.
 */
public class accessbot extends SubspaceBot {

	CommandInterpreter  m_commandInterpreter;

	/**
	 * Initializes the EventRequester to only request Messages to be sent to this bot
	 * 
	 * @param botAction
	 */
	public accessbot( BotAction botAction ) {
        super( botAction );
        EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.MESSAGE );
    }

    /**
     * Handles the messages sent to the bot
     * Available commands: !help !update !die [All SMOD+]
     * 
     * @param event Event object of handled event (packet)
     */
    public void handleEvent( Message event ){
        String message = event.getMessage();
        
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            
            // Checks if the operator is SMod or higher
            if( m_botAction.getOperatorList().isSmod( name ) ) {
            	if( message.startsWith( "!help")) {
            		m_botAction.sendPrivateMessage(name, "Available commands: !help !update !die");
            	}
            	if( message.startsWith( "!update" ) ) {
                	updateAccess( name );
                } else if( message.startsWith( "!die" ) ) {
                	m_botAction.die();
                }
            } else {
            	// Sends a message if the operator isn't the correct level
            	m_botAction.sendPrivateMessage(name, "Only a super moderator or higher can use this bot.");
            }
        }
    }

    /**
     * Handles the spawning of the bot, puts the bot in the correct initial arena
     * @param event LoggedOn 
     */
    public void handleEvent( LoggedOn event ){
        BotSettings m_botSettings = m_botAction.getBotSettings();
        String initialArena = m_botSettings.getString( "Arena" );
        m_botAction.joinArena( initialArena );
    }

    /**
     * Updates access from staff lists to the trenchwars.org database
     * @param name Operator's name
     */
	public void updateAccess( String name ) {
		if( !m_botAction.SQLisOperational()	) {
			m_botAction.sendSmartPrivateMessage( name, "Unable to update staff access, database down." );
			return;
		}
	    
		try {
			//Used to cycle through those who have staff rank on local.
			ResultSet result = m_botAction.SQLQuery( "local", "SELECT fnUserID, fnRankID, fnUserRankID FROM `tblUserRank` WHERE fnRankID > 4 AND fnRankID < 10" );
			
			if(result == null) {
				m_botAction.sendSmartPrivateMessage( name, "Unable to update staff access, database down." );
				return;
			}
			
			m_botAction.sendSmartPrivateMessage( name, "Please hold, updating staff access..." );
			
			while( result.next() ) {
				int userId = result.getInt( "fnUserID" );
				int rankId = result.getInt( "fnRankID" );
				int userRankId = result.getInt( "fnUserRankID" );
				
				ResultSet result2 = m_botAction.SQLQuery( "local", "SELECT fcUserName FROM `tblUser` WHERE fnUserID = \""+userId+"\"" );
				
				if( result2.next() ) {
        			String curPlayer = result2.getString( "fcUserName" );
					DBPlayerData man = new DBPlayerData( m_botAction, "local", result2.getString( "fcUserName" ) );
					if( m_botAction.getOperatorList().isSysop( curPlayer ) ) {
						//if( rankId != 9 )
						//	man.removeRank( userRankId );
					} else if( m_botAction.getOperatorList().isSmod( curPlayer ) ) {
						if( rankId != 8 ) {
							man.removeRank( userRankId );
						}
					} else if( m_botAction.getOperatorList().isModerator( curPlayer ) ) {
						if( rankId != 7 ) {
							man.removeRank( userRankId );
						}
					} else if( m_botAction.getOperatorList().isERExact( curPlayer ) ) {
						if( rankId != 6 ) {
							man.removeRank( userRankId );
						}
					} else if( m_botAction.getOperatorList().isZHExact( curPlayer ) ) {
						if( rankId != 5 ) {
							man.removeRank( userRankId );
						}
					} else {
						man.removeRank( userRankId );
					}
				}
                                m_botAction.SQLClose( result2 );
			}
                        
                        m_botAction.SQLClose( result );
			
			Map access = m_botAction.getOperatorList().getList();
			Set set = access.keySet();
			Iterator it = set.iterator();
			while( it.hasNext() ) {
				String curPlayer = (String) it.next();
					DBPlayerData man = new DBPlayerData( m_botAction, "local", curPlayer, true );
					if( m_botAction.getOperatorList().isSysop( curPlayer ) ) {
						//if( !man.hasRank( 9 ) )
						//	man.giveRank( 9 );
					} else if( m_botAction.getOperatorList().isSmod( curPlayer ) ) {
						if( !man.hasRank( 8 ) )
							man.giveRank( 8 );
					} else if( m_botAction.getOperatorList().isModerator( curPlayer ) ) {
						if( !man.hasRank( 7 ) )
							man.giveRank( 7 );
					} else if( m_botAction.getOperatorList().isERExact( curPlayer )) {
						if( !man.hasRank( 6 ) )
							man.giveRank( 6 );
					} else if( m_botAction.getOperatorList().isZHExact( curPlayer )) {
						if( !man.hasRank( 5 ) )
							man.giveRank( 5 );
					}
			}

			m_botAction.sendSmartPrivateMessage( name, "Done updating staff access" );
			
		} catch (SQLException e ) { 
			// Inform operator about the exception
			m_botAction.sendSmartPrivateMessage(name, "An error has occurred while updating the staff access.");
			m_botAction.sendSmartPrivateMessage(name, "Please contact a bot developer with the following information: " + e.getMessage());
			m_botAction.sendSmartPrivateMessage(name, e.toString());
		}
    }

}
