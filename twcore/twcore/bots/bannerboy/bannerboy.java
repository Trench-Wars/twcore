package twcore.bots.bannerboy;

import twcore.core.*;
import twcore.misc.database.*;
import java.sql.*;
import java.util.*;

public class bannerboy extends SubspaceBot {
	
	//mySQL database to use
	private String sqlHost = "website";
	
	//Keep track of the time since last personal banner change
	private long lastBannerSet = 0;
	
	public bannerboy( BotAction botAction ) {
		super( botAction );
		
		EventRequester req = botAction.getEventRequester();
		req.request( EventRequester.PLAYER_BANNER );
		req.request( EventRequester.MESSAGE );
		req.request( EventRequester.ARENA_LIST );

	}
	
	public void handleEvent( PlayerBanner event ) {

	 	String player = m_botAction.getPlayerName( event.getPlayerID() );
	 	byte[] banner = event.getBanner();
	 	
	 	//If banner isn't in db save it
	 	if( !bannerExists( banner ) )
	 		saveBanner( player, banner);
		markSeen( player, banner );
		
		
		
		if( System.currentTimeMillis() - lastBannerSet < 30000 ) return;
		
		m_botAction.setBanner( event.getBanner() );
		lastBannerSet = System.currentTimeMillis();
		
		//m_botAction.sendSmartPrivateMessage( m_botAction.getPlayerName( event.getPlayerID() ), "Hope you don't mind if I wear your banner. Looks good on me doesn't it?" );
		
	}
	
	public void handleEvent( ArenaList event ) {

		String currentPick = "#robopark";
		while( currentPick.startsWith( "#" ) || event.getSizeOfArena( currentPick ) < 10 )  {
			String[] arenaNames = event.getArenaNames();
			int arenaIndex = (int) (Math.random() * arenaNames.length);
			currentPick = arenaNames[arenaIndex];
		}
		m_botAction.changeArena( currentPick );
      
	}
	
	private boolean bannerExists( byte[] b ) {
		
		String banner = getBannerString( b );
		try {
			String query = "SELECT * FROM tblBanner WHERE fcBanner = '"+banner+"'";
			ResultSet result = m_botAction.SQLQuery( sqlHost, query );
			if( result.next() ) return true;
			else return false;
		} catch (Exception e) {
			Tools.printStackTrace( e );
			return true;
		}
	}
	
	private void saveBanner( String player, byte[] b ) {

		int fnUserID = getPlayerID( player );
		String banner = getBannerString( b );
		
		try {
			String query = "INSERT INTO tblBanner (fnUserID, fcBanner, fdDateFound) VALUES ";
			query += "('"+fnUserID+"', '"+banner+"', NOW() )";
			m_botAction.SQLQuery( sqlHost, query );
		} catch (Exception e) {
			Tools.printStackTrace( e );
		}
	}
	
	private int getPlayerID( String player ) {
		/*
		String name = Tools.addSlashesToString( player );
		try {
			String query = "SELECT fnUserID FROM tblUser WHERE fcUserName = '"+name+"'";
			ResultSet result = m_botAction.SQLQuery( slqHost, query );
			if( result.next() ) {
				return result.getInt( "fnUserID" );
			}
		} catch (Exception e) {
			System.out.println( "SELECT fnUserID FROM tblUser WHERE fcUserName = '"+name+"'" );
			return 0;
		}
		
		createPlayer( player );
		return getPlayerID( player );*/
		DBPlayerData dbPlayer = new DBPlayerData( m_botAction, sqlHost, player, true );
		return dbPlayer.getUserID();
	}
	
	private int getBannerID( byte[] b ) {
		
		String banner = getBannerString( b );
		
		try {
			String query = "SELECT fnBannerID FROM tblBanner WHERE fcBanner = '"+banner+"'";
			ResultSet result = m_botAction.SQLQuery( sqlHost, query );
			if( result.next() ) {
				return result.getInt( "fnBannerID" );
			} else return -1;
		} catch (Exception e) {
			Tools.printStackTrace( e );
			return -1;
		}
	}
	
	private void createPlayer( String player ) {
		
		player = Tools.addSlashesToString( player );
		try {
			String query = "INSERT INTO tblUser (fcUserName) VALUES ('"+player+"')";
			m_botAction.SQLQuery( sqlHost, query );
		} catch (Exception e) {
			Tools.printStackTrace( e );
		}
	}
	
	private void markSeen( String player, byte[] banner ) {
		
		int bannerId = getBannerID( banner );
		int userId = getPlayerID( player );
		
		if( alreadyMarked( userId, bannerId ) ) return;
		
		try {
			String query = "INSERT into tblWore (fnUserId, fnBannerId) VALUES ";
			query += "("+userId+", "+bannerId+")";
			m_botAction.SQLQuery( sqlHost, query );
		} catch (Exception e) {
			Tools.printStackTrace( e );
		}
	}

	private boolean alreadyMarked( int userId, int bannerId ) {
		
		try {
			String query = "SELECT fnUserId FROM tblWore WHERE fnUserID = "+userId+" AND fnBannerID = "+bannerId;
			ResultSet result = m_botAction.SQLQuery( sqlHost, query );
			if( result.next() ) return true;
			else return false;
		} catch (Exception e) {
			Tools.printStackTrace( e );
			return true;
		}
	}
	
	private String getBannerString( byte[] banner ) {
		
		String b = "";
	 	for( int i = 0; i < 96; i++ ) {
	        if( (banner[i] & 0xf0) == 0 ){
	            b += 0 + Integer.toHexString( banner[i] & 0xFF );
	        } else {
	            b += Integer.toHexString( banner[i] & 0xFF );
	        }
	 	}
	 	return b;
	}
	
	public void handleEvent( Message event ) {
		
		if( event.getMessageType() != Message.PRIVATE_MESSAGE && 
		    event.getMessageType() != Message.REMOTE_PRIVATE_MESSAGE) return;
		
		String player = m_botAction.getPlayerName( event.getPlayerID() );
		
		//if( player.equals( "2dragons" ) )
		if( event.getMessage().startsWith( "!die" ) )
				m_botAction.die();
			else if( event.getMessage().startsWith( "!go " ) )
				m_botAction.joinArena( event.getMessage().substring( 4 ) );
		
		//if( event.getMessage().startsWith( "!say " ) ) sayCommand( event.getMessage().substring( 5 ) );
		//else if( event.getMessage().startsWith( "!tsay ") ) sayTCommand( event.getMessage().substring( 6 ) );
		//else m_botAction.sendSmartPrivateMessage( "2dragons", player + "> "+event.getMessage() );
	}
	
	private void sayCommand( String message ) {
		
		String pieces[] = message.split( ":" );
		
		try {
		m_botAction.sendSmartPrivateMessage( pieces[0], pieces[1] );
		} catch (Exception e) {
		}
	}
	
	private void sayTCommand( String message ) {
				
		try {
		m_botAction.sendTeamMessage( message );
		} catch (Exception e) {
			System.out.println( e );
		}
	}
	
	public void handleEvent( LoggedOn event ) {
		m_botAction.joinArena( "baseelim" );
		
		TimerTask changeArenas = new TimerTask() {
			public void run() {
				m_botAction.requestArenaList();
			}
		};
		m_botAction.scheduleTaskAtFixedRate( changeArenas, 60000, 240000 );
	}

}