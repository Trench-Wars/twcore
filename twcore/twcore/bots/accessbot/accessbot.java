package twcore.bots.accessbot;

import twcore.core.*;
import twcore.misc.database.DBPlayerData;
import java.util.*;
import java.sql.*;

public class accessbot extends SubspaceBot {
	
	String access_name;
	
	CommandInterpreter  m_commandInterpreter;
	
	public accessbot( BotAction botAction ) {
        super( botAction );
        EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.MESSAGE );
        events.request( EventRequester.PLAYER_POSITION );
    }
    
    public void handleEvent( Message event ){
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_botAction.getOperatorList().isSmod( name ) )
            	if( message.startsWith( "!update" ) ) {
            		access_name = name;
                	updateAccess( name, message );
                } else if( message.startsWith( "!die" ) )
                	m_botAction.die();
            	else if( message.startsWith( "!watch" ) )
            		m_botAction.spectatePlayer( name );
       }   
    }
    
    public void handleEvent( LoggedOn event ){
        BotSettings m_botSettings = m_botAction.getBotSettings();
        String initialArena = m_botSettings.getString( "Arena" );
        m_botAction.joinArena( initialArena );
    }
    
	public void updateAccess( String name, String message ) {
		if( !m_botAction.SQLisOperational()	) {
			m_botAction.sendSmartPrivateMessage( name, "Unable to update staff access, database down." );
			return;
		}	
	    m_botAction.sendSmartPrivateMessage( access_name, "Please hold, updating staff access." );
		try {
			
		//Used to cycle through those who have staff rank on website.
		ResultSet result = m_botAction.SQLQuery( "website", "SELECT fnUserID, fnRankID, fnUserRankID FROM `tblUserRank` WHERE fnRankID > 4 AND fnRankID < 10" );
			while( result.next() ) {
				int userId = result.getInt( "fnUserID" );
				int rankId = result.getInt( "fnRankID" );
				int userRankId = result.getInt( "fnUserRankID" );
				ResultSet result2 = m_botAction.SQLQuery( "website", "SELECT fcUserName FROM `tblUser` WHERE fnUserID = \""+userId+"\"" );
				if( result2.next() ) {
        				String curPlayer = result2.getString( "fcUserName" );
					DBPlayerData man = new DBPlayerData( m_botAction, "website", result2.getString( "fcUserName" ) );
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
					} else if( m_botAction.getOperatorList().isER( curPlayer ) ) {
						if( rankId != 6 ) {
							man.removeRank( userRankId );
						}
					} else if( m_botAction.getOperatorList().isZH( curPlayer ) ) {
						if( rankId != 5 ) {
							man.removeRank( userRankId );
						}
					} else {
						man.removeRank( userRankId );
					}
				}
			}
			Map access = m_botAction.getOperatorList().getList();
			Set set = access.keySet();
			Iterator it = set.iterator();
			while( it.hasNext() ) {
				String curPlayer = (String) it.next();
					DBPlayerData man = new DBPlayerData( m_botAction, "website", curPlayer, true );
					if( m_botAction.getOperatorList().isSysop( curPlayer ) ) {
						//if( !man.hasRank( 9 ) )
						//	man.giveRank( 9 );
					} else if( m_botAction.getOperatorList().isSmod( curPlayer ) ) {
						if( !man.hasRank( 8 ) )
							man.giveRank( 8 );
					} else if( m_botAction.getOperatorList().isModerator( curPlayer ) ) {
						if( !man.hasRank( 7 ) )
							man.giveRank( 7 );
					} else if( m_botAction.getOperatorList().isER( curPlayer ) ) {
						if( !man.hasRank( 6 ) )
							man.giveRank( 6 );
					} else if( m_botAction.getOperatorList().isZH( curPlayer ) && (!m_botAction.getOperatorList().isOutsiderExact( curPlayer ) || m_botAction.getOperatorList().isZHExact( curPlayer ))) {
						if( !man.hasRank( 5 ) )
							man.giveRank( 5 );
					}
			}
			
			m_botAction.sendSmartPrivateMessage( access_name, "Access updated" );
		} catch (Exception e ) { m_botAction.sendArenaMessage( ""+e ); }
    }
    
}