package twcore.bots.bannerboy;

import java.sql.ResultSet;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaList;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerBanner;
import twcore.core.stats.DBPlayerData;
import twcore.core.util.Tools;

/**
 * A bot designed to collect banners from players and place them in a database.
 * 
 * @author 2dragons
 */
public class bannerboy extends SubspaceBot {

	//mySQL database to use
	private String m_sqlHost = "local";

	//Keep track of the time since last personal banner change
	private long m_lastBannerSet = 0;

	//queue for banner checks
	private Vector m_toCheck;

	//Boolean to track if 'talking' mode is on
	private boolean m_talk;

	public bannerboy( BotAction botAction ) {
		super( botAction );

		EventRequester req = botAction.getEventRequester();
		req.request( EventRequester.PLAYER_BANNER );
		req.request( EventRequester.MESSAGE );
		req.request( EventRequester.ARENA_LIST );

		m_toCheck = new Vector();

		m_talk = false;
	}

	public void handleEvent( PlayerBanner event ) {

	 	String player = m_botAction.getPlayerName( event.getPlayerID() );
	 	byte[] banner = event.getBanner();

	 	m_toCheck.add( new BannerCheck( player, banner ) );

		if( System.currentTimeMillis() - m_lastBannerSet < 60000*5 ) return;

		m_botAction.setBanner( event.getBanner() );
		m_lastBannerSet = System.currentTimeMillis();

		if( m_talk )
			m_botAction.sendSmartPrivateMessage( m_botAction.getPlayerName( event.getPlayerID() ), "Hope you don't mind if I wear your banner. Looks good on me doesn't it?" );

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
                        boolean exists = false;
			String query = "SELECT * FROM tblBanner WHERE fcBanner = '"+banner+"'";
			ResultSet result = m_botAction.SQLQuery( m_sqlHost, query );
			if( result.next() ) exists = true;
                        m_botAction.SQLClose( result );
                        return exists;
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
			m_botAction.SQLQueryAndClose( m_sqlHost, query );
		} catch (Exception e) {
			Tools.printStackTrace( e );
		}
	}

	private int getPlayerID( String player ) {

		DBPlayerData dbPlayer = new DBPlayerData( m_botAction, m_sqlHost, player, true );
		return dbPlayer.getUserID();
	}

	private int getBannerID( byte[] b ) {

		String banner = getBannerString( b );

		try {
                        int id = -1;
			String query = "SELECT fnBannerID FROM tblBanner WHERE fcBanner = '"+banner+"'";
			ResultSet result = m_botAction.SQLQuery( m_sqlHost, query );
			if( result.next() )
				id = result.getInt( "fnBannerID" );
                        m_botAction.SQLClose( result );
                        return id;
		} catch (Exception e) {
			Tools.printStackTrace( e );
			return -1;
		}
	}

	private void createPlayer( String player ) {

		player = Tools.addSlashesToString( player );
		try {
			String query = "INSERT INTO tblUser (fcUserName) VALUES ('"+player+"')";
                         m_botAction.SQLQueryAndClose( m_sqlHost, query );
		} catch (Exception e) {
			Tools.printStackTrace( e );
		}
	}

	private void markSeen( String player, byte[] banner ) {

		int bannerId = getBannerID( banner );
		int userId = getPlayerID( player );

		if( bannerId <= 0 ) return;

		if( alreadyMarked( userId, bannerId ) ) return;

		try {
			String query = "INSERT into tblWore (fnUserId, fnBannerId) VALUES ";
			query += "("+userId+", "+bannerId+")";
                         m_botAction.SQLQueryAndClose( m_sqlHost, query );
		} catch (Exception e) {
			Tools.printStackTrace( e );
		}
	}

	private boolean alreadyMarked( int userId, int bannerId ) {

		try {
                        boolean marked = false;
			String query = "SELECT fnUserId FROM tblWore WHERE fnUserID = "+userId+" AND fnBannerID = "+bannerId;
			ResultSet result = m_botAction.SQLQuery( m_sqlHost, query );
			if( result.next() ) marked = true;
                        m_botAction.SQLClose( result );
                        return marked;
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

		if( event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE )
			player = event.getMessager();


		if( player.equals( "2dragons" ) ) {
			if( event.getMessage().startsWith( "!die" ) )
				m_botAction.die();
			else if( event.getMessage().startsWith( "!go " ) )
				m_botAction.joinArena( event.getMessage().substring( 4 ) );
			else if( event.getMessage().startsWith( "!say " ) ) sayCommand( event.getMessage().substring( 5 ) );
			else if( event.getMessage().startsWith( "!tsay ") ) sayTCommand( event.getMessage().substring( 6 ) );
			else if( event.getMessage().startsWith( "!talk" ) ) toggleTalk( player );
		}
		else m_botAction.sendSmartPrivateMessage( "2dragons", player + "> "+event.getMessage() );
	}

	private void toggleTalk( String _name ) {

		m_talk = !m_talk;
		if( m_talk ) m_botAction.sendSmartPrivateMessage( _name, "Talk on" );
		else m_botAction.sendSmartPrivateMessage( _name, "Talk off" );
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


		//Every 3 seconds check another banner
		TimerTask checkBanners = new TimerTask() {
			public void run() {

				if( m_toCheck.size() <= 0 ) return;
				BannerCheck bc = (BannerCheck)m_toCheck.remove(0);
				byte banner[] = bc.getBanner();
				String player = bc.getPlayer();

			 	//If banner isn't in db save it
			 	if( !bannerExists( banner ) )
			 		saveBanner( player, banner);
				markSeen( player, banner );
			}
		};
		m_botAction.scheduleTaskAtFixedRate( checkBanners, 2000, 3000 );
	}
}

	class BannerCheck {

		private String pName;
		private byte[] banner;

		public BannerCheck( String player, byte[] b ) {
			pName = player;
			banner = b;
		}

		public String getPlayer() { return pName; }
		public byte[] getBanner() { return banner; }
	}
