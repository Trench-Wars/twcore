package twcore.bots.bannerboy;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaList;
import twcore.core.events.KotHReset;
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
	private String m_sqlHost = "website";

	//Keep track of the time since last personal banner change
	private long m_lastBannerSet = 0;

	//queue for banner checks
	private Vector<BannerCheck> m_toCheck;

	//Boolean to track if 'talking' mode is on
	private boolean m_talk;
	
	
	// PreparedStatements
	PreparedStatement psGetBannerID;
	PreparedStatement psSaveBanner;
	PreparedStatement psSeenBanner;
	PreparedStatement psCheckSeen;

	public bannerboy( BotAction botAction ) {
		super( botAction );

		EventRequester req = botAction.getEventRequester();
		req.request( EventRequester.PLAYER_BANNER );
		req.request( EventRequester.MESSAGE );
		req.request( EventRequester.ARENA_LIST );
		req.request( EventRequester.KOTH_RESET );

		m_toCheck = new Vector<BannerCheck>();
		m_talk = false;
		psGetBannerID = m_botAction.createPreparedStatement(m_sqlHost, "bannerboy", "SELECT fnBannerID FROM tblBanner WHERE fcBanner = ? LIMIT 0,1");
        psSaveBanner = m_botAction.createPreparedStatement(m_sqlHost, "bannerboy", "INSERT INTO tblBanner (fnUserID, fcBanner, fdDateFound) VALUES ( ? , ? , NOW())");
        psSeenBanner = m_botAction.createPreparedStatement(m_sqlHost, "bannerboy", "INSERT INTO tblWore (fnUserId, fnBannerId) VALUES ( ? , ? )");
        psCheckSeen = m_botAction.createPreparedStatement(m_sqlHost, "bannerboy", "SELECT fnUserId FROM tblWore WHERE fnUserID = ? AND fnBannerID = ? LIMIT 0,1");
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
		// If arena name starts with # or less then 10 players are in it, pick a random arena again.
		// Note: This can create an indefinite loop if there are only # arenas or there are only arenas with less then 10 players in it. 
		while( currentPick.startsWith( "#" ) || event.getSizeOfArena( currentPick ) < 10 )  {
			String[] arenaNames = event.getArenaNames();
			int arenaIndex = (int) (Math.random() * arenaNames.length);
			currentPick = arenaNames[arenaIndex];
		}
		m_botAction.changeArena( currentPick );

	}
	
	public void handleDisconnect() {
	    m_botAction.closePreparedStatement(m_sqlHost, "bannerboy", this.psCheckSeen);
	    m_botAction.closePreparedStatement(m_sqlHost, "bannerboy", this.psGetBannerID);
	    m_botAction.closePreparedStatement(m_sqlHost, "bannerboy", this.psSaveBanner);
	    m_botAction.closePreparedStatement(m_sqlHost, "bannerboy", this.psSeenBanner);
	    m_botAction.cancelTasks();
	}

	private boolean bannerExists( byte[] b ) {
		try {
            psGetBannerID.setString(1, getBannerString( b ));
            ResultSet rs = psGetBannerID.executeQuery();
			
            if( rs.next() ) return true;
            else            return false;
		} catch (SQLException sqle) {
			Tools.printStackTrace( sqle );
			return true;
		}
	}

	private void saveBanner( String player, byte[] b ) {
		try {
			psSaveBanner.setInt(1, getPlayerID( player ));
			psSaveBanner.setString(2, getBannerString( b ));
			psSaveBanner.execute();
		} catch (Exception e) {
			Tools.printStackTrace( e );
		}
	}

	private int getPlayerID( String player ) {
		DBPlayerData dbPlayer = new DBPlayerData( m_botAction, m_sqlHost, player, true );
		return dbPlayer.getUserID();
	}

	private int getBannerID( byte[] b ) {
        int id = -1;
		try {
		    psGetBannerID.setString(1, getBannerString( b ));
		    ResultSet rs = psGetBannerID.executeQuery();
			if( rs.next() ) {
			    id = rs.getInt("fnBannerID");
			}
            return id;
		} catch (SQLException sqle) {
			Tools.printStackTrace( sqle );
			return -1;
		}
	}

	/*private void createPlayer( String player ) {

		player = Tools.addSlashesToString( player );
		try {
			String query = "INSERT INTO tblUser (fcUserName) VALUES ('"+player+"')";
                         m_botAction.SQLQueryAndClose( m_sqlHost, query );
		} catch (Exception e) {
			Tools.printStackTrace( e );
		}
	}*/

	private void markSeen( String player, byte[] banner ) {

		int bannerId = getBannerID( banner );
		int userId = getPlayerID( player );

		if( bannerId <= 0 ) 
		    return;

		if( alreadyMarked( userId, bannerId ) ) 
		    return;

		try {
		    psSeenBanner.setInt(1, userId);
		    psSeenBanner.setInt(2, bannerId);
		    psSeenBanner.execute();
		} catch (SQLException sqle) {
			Tools.printStackTrace( sqle );
		}
	}

	private boolean alreadyMarked( int userId, int bannerId ) {
		try {
		    psCheckSeen.setInt(1, userId);
		    psCheckSeen.setInt(2, bannerId);
		    ResultSet rs = psCheckSeen.executeQuery();
			if( rs.next() ) 
			    return true;
			else
			    return false;
		} catch (SQLException sqle) {
			Tools.printStackTrace( sqle );
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
		    event.getMessageType() != Message.REMOTE_PRIVATE_MESSAGE) 
		    return;

		String player = m_botAction.getPlayerName( event.getPlayerID() );
		String message = event.getMessage();

		if( event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE )
			player = event.getMessager();


		if( m_botAction.getOperatorList().isSmod(player)) {
		    if(message.startsWith("!help")) {
		        String[] help = {
                     "Available commands:",
                     " !go <arena>                 - Makes the bot move to <arena>",
                     " !say <playername>:<message> - Sends PM with <message> to <playername>",
                     " !tsay <message>             - Sends team <message>",
                     " !talk                       - Toggles if the bot talks to the player",
                     "                               when copying/wearing his banner",
                     " !die                        - Disconnects the bot"
                     };
		        m_botAction.smartPrivateMessageSpam(player, help);
		    } else
		        
			if(message.startsWith("!die")) {
			    this.handleDisconnect();
			    m_botAction.die("Disconnected by "+player);
			} else 
			    
			if(message.startsWith("!go ")) {
			    String arena = message.substring(4);
			    
			    if(arena.length() > 0) {
			        m_botAction.sendSmartPrivateMessage(player, "Going to "+arena);
			        m_botAction.joinArena( arena );
			    }
			} else
			    
			if(message.startsWith("!say ")) {
			    if(message.indexOf(':')==-1)
			        return;
			    
			    String pieces[] = message.split( ":" );
		        m_botAction.sendSmartPrivateMessage( pieces[0], pieces[1] );
		        m_botAction.sendSmartPrivateMessage( player, "PM send to "+pieces[0]);
			} else 
			    
			if(message.startsWith("!tsay ")) {
			    if(message.length()>0)
			        m_botAction.sendTeamMessage( message );
			} else 
			    
			 if(message.startsWith("!talk")) {
			     m_talk = !m_talk;
			     if( m_talk ) m_botAction.sendSmartPrivateMessage( player, "Talk on" );
			     else         m_botAction.sendSmartPrivateMessage( player, "Talk off" );
			 }
		}
		else if(!m_botAction.getOperatorList().isSmod(player) && message.equalsIgnoreCase("!help")){
		    String[] helpmsg = {
		            "Hello, I'm a bot that collects banner information from players. I store all of the ",
		            "information I find at http://www.trenchwars.org/SSBE. If you would like me to store",
		            "your banner on the site simply stay in the same arena as me and I'll be happy to do",
		            "so. Have fun in Trench Wars!"
		    };
		    m_botAction.smartPrivateMessageSpam(player, helpmsg);
		}
		else if(!m_botAction.getOperatorList().isBotExact(player)) {
		    m_botAction.sendChatMessage(player + "> "+message);
		}
	}

	public void handleEvent( LoggedOn event ) {
	    m_botAction.joinArena( m_botAction.getGeneralSettings().getString("Arena") );
        m_botAction.sendUnfilteredPublicMessage("?chat="+m_botAction.getGeneralSettings().getString("Chat Name"));

	    if(psGetBannerID == null || psSaveBanner == null || psSeenBanner == null || psCheckSeen == null) {
            //Something went wrong, we can't continue
	        m_botAction.sendChatMessage("Error while creating PreparedStatements, disconnecting...");
            handleDisconnect();
            m_botAction.die("Error while creating PreparedStatements");
            return;
        }
        
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
				BannerCheck bc = m_toCheck.remove(0);
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
	
	/**
     * Handles restarting of the KOTH game
     * 
     * @param event is the event to handle.
     */
    public void handleEvent(KotHReset event) {
        if(event.isEnabled() && event.getPlayerID()==-1) {
            // Make the bot ignore the KOTH game (send that he's out immediately after restarting the game)
            m_botAction.endKOTH();
        }
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
