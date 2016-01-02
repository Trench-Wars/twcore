package twcore.bots.pushbulletbot;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.List;
import twcore.core.net.iharder.*;
import twcore.core.util.Tools;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * This is a barebones template bot for use when creating a new bot.  With slight
 * modification, it can suit just about any purpose.  Note that if you are
 * thinking of creating a typical eventbot, you should strongly consider creating
 * a MultiBot module instead, which is more simple; also, you might consider the
 * UltraBot template, which includes a command interpreter for easy command setup.
 *
 * @author  Stefan / Mythrandir
 */
public class pushbulletbot extends SubspaceBot {

    private BotSettings m_botSettings;          // Stores settings for your bot as found in the .cfg file.
    //BotSettings rules;                                            // In this case, it would be settings from pushbulletbot.cfg

    private String connectionID = "pushbulletbot";
    static final String db = "bots";
    
    
    /**
     * Creates a new instance of your bot.
     */
    public pushbulletbot(BotAction botAction) {
        super(botAction);
        requestEvents();

        // m_botSettings contains the data specified in file <botname>.cfg
        m_botSettings = m_botAction.getBotSettings();
    }

    // Push to mobile data
    //private MobilePusher mobilePusher;
    private PushbulletClient pbClient;

    /**
     * This method requests event information from any events your bot wishes
     * to "know" about; if left commented your bot will simply ignore them.
     */
    public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        // req.request(EventRequester.ARENA_LIST);
        req.request(EventRequester.ARENA_JOINED);
        // req.request(EventRequester.PLAYER_ENTERED);
        // req.request(EventRequester.PLAYER_POSITION);
        // req.request(EventRequester.PLAYER_LEFT);
        // req.request(EventRequester.PLAYER_DEATH);
        // req.request(EventRequester.PRIZE);
        // req.request(EventRequester.SCORE_UPDATE);
        // req.request(EventRequester.WEAPON_FIRED);
        // req.request(EventRequester.FREQUENCY_CHANGE);
        // req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(EventRequester.LOGGED_ON);
        // req.request(EventRequester.FILE_ARRIVED);
        // req.request(EventRequester.FLAG_VICTORY);
        // req.request(EventRequester.FLAG_REWARD);
        // req.request(EventRequester.SCORE_RESET);
        // req.request(EventRequester.WATCH_DAMAGE);
        // req.request(EventRequester.SOCCER_GOAL);
        // req.request(EventRequester.BALL_POSITION);
        // req.request(EventRequester.FLAG_POSITION);
        // req.request(EventRequester.FLAG_DROPPED);
        // req.request(EventRequester.FLAG_CLAIMED);
    }
           
    
    
    
    
    
    /**
     * You must write an event handler for each requested event/packet.
     * This is an example of how you can handle a message event.
     */
    public void handleEvent(Message event)  {
        // Retrieve name. If the message is remote, then event.getMessager() returns null, and event.getPlayerID returns a value.
        // If the message is from the same arena, event.getMessager() returns a string, and event.getPlayerID will return 0.
        String name = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());
        if (name == null) name = "-anonymous-";

        /*
        m_botAction.sendPublicMessage(  "I received a Message event type ("+ event.getMessageType()+") from " + name +
                                        " containing the following text: " + event.getMessage());
		*/
        
        // Default implemented command: !die
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!die")) {
            //m_botAction.sendPublicMessage(name + " commanded me to die. Disconnecting...");
            try { Thread.sleep(50); } catch (Exception e) {};
            handleDisconnect();
            m_botAction.die("!die by " + name);
        }
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!getlink")) {
        	String squadChannel = getSquadChannel(name);
			if (squadChannel == "") {return; } //means player's squad doesn't have a registered channel
			m_botAction.sendSmartPrivateMessage(name, "https://www.pushbullet.com/channel?tag=" + squadChannel);
	        // String name = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());
	        m_botAction.sendPublicMessage(name);
	        m_botAction.sendPublicMessage(String.valueOf(event.getPlayerID()));
			
			
        }
        
        //this is a temporary fake challenge 
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!challenge")) {
        	String msg = "(MatchBot3)>Axwell is challenging you for a game of 3vs3 TWJD versus Rage. Captains/assistants, ?go twjd and pm me with '!accept Rage'";
        	
        	//String squadChannel = getSquadChannel(name);
			//if (squadChannel == "") {return; } //means player's squad doesn't have a registered channel
        	
        	//try {
				//pbClient.sendChannelMsg( squadChannel, "", msg);
        		messagePlayerSquadMembers(name, msg);
        		m_botAction.sendPublicMessage(msg);
			//} catch (PushbulletException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			//}
        }
        
        //this is a temporary fake challenge
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!accept")) {
        	String msg = "(MatchBot3)>A game of 3vs3 TWJD versus Rage will start in ?go twjd in 30 seconds";
        	//String squadChannel = getSquadChannel(name);
			//if (squadChannel == "") {return; } //means player's squad doesn't have a registered channel
        	
        	//try {
			//	pbClient.sendChannelMsg(squadChannel, "", msg);
	        	messagePlayerSquadMembers(name, msg);
	        	m_botAction.sendPublicMessage(msg);
			//} catch (PushbulletException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
        }
       
        
        
        
        
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!beep ")) {
        	String msg = event.getMessage().substring(event.getMessage().indexOf(" ") + 1);
        	String channelPost = this.getInterpretBeep(name, msg);
        	//String squadChannel = getSquadChannel(name);
			//if (squadChannel == "") {return; } //means player's squad doesn't have a registered channel
        	
        	if (channelPost != "") {
	        	 //try{
	        		 messagePlayerSquadMembers(name, channelPost);
	        	     //pbClient.sendChannelMsg(squadChannel, "", channelPost);
	        	     m_botAction.sendPublicMessage(channelPost);
	        	 //} catch( PushbulletException e ){
	        	     // Huh, didn't work
	        	 //}
        	}
        }

        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!createchannel ")) {
        	String squadChannel = event.getMessage().substring(event.getMessage().indexOf(" ") + 1);
			if (this.createSquadChannel(name, squadChannel)) { 
				m_botAction.sendPublicMessage(squadChannel + " created successfully!");
			} else {
				m_botAction.sendPublicMessage(squadChannel + " creation failed!");
			}
        }
        
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!push ")) {
         	String msg = event.getMessage().substring(event.getMessage().indexOf(" ") + 1);
        	 try{
        		 pbClient.sendNote( null, getEmailByUserName(name), "", msg);
        		 m_botAction.sendPublicMessage("Private Message: '" + msg + "' Pushed Successfully to " + name + ": " + getEmailByUserName(name));
        	 } catch( PushbulletException e ){
        	     // Huh, didn't work
        	 }
        }
        
        /*if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!notify ")) {
         	String userName = event.getMessage().substring(event.getMessage().indexOf(" ") + 1);
        	String msg = "Reply to me with commands like 'jd' to beep on channel!"; 
         	try{
        		 pbClient.sendNote( null, getEmailByUserName(userName), "", msg);
        		 m_botAction.sendPublicMessage("Private Message: '" + msg + "' Pushed Successfully!");
        	 } catch( PushbulletException e ){
        	     // Huh, didn't work
        	 }
        }*/
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!pushchannel ")) {
        	//m_botAction.sendSmartPrivateMessage(name, "Message: '" + msg + "' Pushed Successfully!");
        	String msg = event.getMessage().substring(event.getMessage().indexOf(" ") + 1);
        	String squadChannel = getSquadChannel(name);
			if (squadChannel == "") {return; } //means player's squad doesn't have a registered channel
        	
        	 try{
        	     pbClient.sendChannelMsg(squadChannel, "", msg);
        	     m_botAction.sendPublicMessage("Channel Message: '" + msg + "' Pushed Successfully!");
        	     //m_botAction.sendSmartPrivateMessage(name, "Channel Message: '" + msg + "' Pushed Successfully!");
        	 } catch( PushbulletException e ){
        	     // Huh, didn't work
        	 }
        }

        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!pushes")) {
	        List<Push> pushes = null;
			try {
				pushes = pbClient.getPushes();
			} catch (PushbulletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//m_botAction.sendSmartPrivateMessage(name, "Number of pushes: " + pushes.size() );
			//m_botAction.sendSmartPrivateMessage(name, pushes.get(0).toString());
			Push lastPush = pushes.get(0);
			
			m_botAction.sendSmartPrivateMessage(name, "getBody                      : " + lastPush.getBody().toString());
			//try {Thread.sleep(100);} catch (InterruptedException e) {// TODO Auto-generated catch block e.printStackTrace();}
			m_botAction.sendSmartPrivateMessage(name, "getIden                      : " + lastPush.getIden().toString());
			//m_botAction.sendSmartPrivateMessage(name, "getOwner_iden                : " + lastPush.getOwner_iden().toString());
			////m_botAction.sendSmartPrivateMessage(name, "getReceiver_email            : " + lastPush.getReceiver_email().toString());
			////m_botAction.sendSmartPrivateMessage(name, "getReceiver_email_normalized : " + lastPush.getReceiver_email_normalized().toString());
			////m_botAction.sendSmartPrivateMessage(name, "getReceiver_iden             : " + lastPush.getReceiver_iden().toString());
			////m_botAction.sendSmartPrivateMessage(name, "getSender_email              : " + lastPush.getSender_email().toString());
			////m_botAction.sendSmartPrivateMessage(name, "getSender_email_normalized   : " + lastPush.getSender_email_normalized().toString());
			////m_botAction.sendSmartPrivateMessage(name, "getSender_iden               : " + lastPush.getSender_iden().toString());
			//m_botAction.sendSmartPrivateMessage(name, "getTitle                     : " + lastPush.getTitle().toString());
			m_botAction.sendSmartPrivateMessage(name, "getType                      : " + lastPush.getType().toString());
			//m_botAction.sendSmartPrivateMessage(name, "getUrl                       : " + lastPush.getUrl().toString());
			//m_botAction.sendSmartPrivateMessage(name, "getClass                     : " + lastPush.getClass().toString());
        
        }
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!listen")) {
        	m_botAction.sendSmartPrivateMessage(name, "Listening Started!");
        	pbClient.startWebsocket();
        }
        
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!stoplisten")) {
        	m_botAction.sendSmartPrivateMessage(name, "Listening Halted!");
        	pbClient.stopWebsocket();
        }
        
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!signup ")) {
        	String email = event.getMessage().substring(event.getMessage().indexOf(" ") + 1);
        	
        	//check if valid email address, if not then exit
        	if (!email.contains("@") || !email.contains(".")) {
        		m_botAction.sendPublicMessage("Invalid Email Adress entered!");
        		return;
        	}
        	
        	//get signup Query
        	PreparedStatement ps_signup = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("signup"));      	
        	
        	//put values in prepared statement
        	try {
				ps_signup.clearParameters();
				ps_signup.setString(1, Tools.addSlashesToString(name));
				ps_signup.setString(2, Tools.addSlashesToString(email));
				m_botAction.sendPublicMessage(ps_signup.toString());
				ps_signup.execute();
				m_botAction.sendPublicMessage("Signed Up " + name + " : " + email + " Successfully!");
			} catch (SQLException e1) {
				try {
					for (Throwable x : ps_signup.getWarnings()) {
						if (x.getMessage().toLowerCase().contains("unique")) {
							m_botAction.sendPublicMessage(email + " is already registered by " + getUserNameByEmail(email));
						} else {
							m_botAction.sendPublicMessage(x.getMessage());
							e1.printStackTrace();
						}
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					m_botAction.sendPublicMessage("Something crazy happened...");
					e.printStackTrace();
				}
			}
        }
        
    }
    
    
    
    
    
    /**
     * 
     * @param statementName = signup : @PlayerName, @PushBulletEmail
     * @param statementName = createchannel : @PlayerName, @ChannelName
     * @param statementName = getusernamebyemail : @PushBulletEmail
     * @param statementName = getemailbyusername : @PlayerName
     * 
     * @return preparedStatement Query
     */
    public String getPreparedStatement(String statementName) {
    	String preparedStatement = "";
    	switch (statementName.toLowerCase()) {
    	case "signup":
    		preparedStatement = 
    				"USE trench_TrenchWars;"
		    	+	"SET @PlayerName = ?, @PushBulletEmail = ?;"
		    	+	"DELETE PBA FROM trench_TrenchWars.tblPBAccount AS PBA "
		    	+	"JOIN trench_TrenchWars.tblUser AS U ON U.fnUserID = PBA.fnPlayerID AND U.fcUserName = @PlayerName;"
		    	+	"INSERT INTO trench_TrenchWars.tblPBAccount (fnPlayerID, fcPushBulletEmail)"
		    	+	"SELECT fnUserID, @PushBulletEmail FROM trench_TrenchWars.tblUser WHERE fcUserName = @PlayerName  AND ISNULL(fdDeleted) LIMIT 1;";
    		break;
		case "createchannel":
			preparedStatement = 
		    		" SET @PlayerName = ?, @ChannelName = ?;"
				+	" DELETE FROM trench_TrenchWars.tblPBSquadChannel WHERE fnSquadID ="
				+	"	(SELECT T.fnTeamID FROM trench_TrenchWars.tblTeam AS T"
				+	"	JOIN trench_TrenchWars.tblTeamUser AS TU ON T.fnTeamID = TU.fnTeamID AND fnCurrentTeam = 1"
				+	"	JOIN trench_TrenchWars.tblUser AS U ON TU.fnUserID = U.fnUserID"
				+	"	WHERE U.fcUserName = @PlayerName AND isnull(T.fdDeleted));"
				+	" INSERT INTO trench_TrenchWars.tblPBSquadChannel (fnSquadID, fcChannelName)"
				+	" SELECT T.fnTeamID, @ChannelName AS fcChannelName"
				+	" FROM trench_TrenchWars.tblTeam AS T"
				+	" JOIN trench_TrenchWars.tblTeamUser AS TU ON T.fnTeamID = TU.fnTeamID AND fnCurrentTeam = 1"
				+	" JOIN trench_TrenchWars.tblUser AS U ON TU.fnUserID = U.fnUserID"
				+	" WHERE U.fcUserName = @PlayerName AND isnull(T.fdDeleted);";
			break;
		case "getusernamebyemail": //can't use @Params if expecting recordset results
			preparedStatement = 
					" SELECT U.fcUserName FROM trench_TrenchWars.tblPBAccount AS PBA"
				+	" JOIN trench_TrenchWars.tblUser AS U ON PBA.fnPlayerID = U.fnUserID WHERE PBA.fcPushBulletEmail = ?;";
			break;
		case "getemailbyusername": //can't use @Params if expecting recordset results
			preparedStatement = 
					" SELECT PBA.fcPushBulletEmail FROM trench_TrenchWars.tblPBAccount AS PBA"
				+	" JOIN trench_TrenchWars.tblUser AS U ON PBA.fnPlayerID = U.fnUserID WHERE U.fcUserName = ?;";
			break;
		case "interpretbeep": //can't use @Params if expecting recordset results
			preparedStatement = 
					" SELECT fcCommand, fcCommandShortDescription FROM trench_TrenchWars.tblPBCommands"
				+	" WHERE INSTR(?, fcCommand) > 0;";
			break;
		case "getsquadchannel": //can't use @Params if expecting recordset results
			preparedStatement = 
					" SELECT PBS.fcChannelName FROM trench_TrenchWars.tblPBSquadChannel AS PBS"
				+	" JOIN trench_TrenchWars.tblTeam AS T ON T.fnTeamID = PBS.fnSquadID"
				+ 	" JOIN trench_TrenchWars.tblTeamUser AS TU ON T.fnTeamID = TU.fnTeamID AND fnCurrentTeam = 1"
				+	" JOIN trench_TrenchWars.tblUser AS U ON TU.fnUserID = U.fnUserID AND U.fcUserName = ? AND isnull(T.fdDeleted);";
			break;    	

		case "getplayersquadmembers": //can't use @Params if expecting recordset results
			preparedStatement = 
					" SELECT U.fnUserID, U.fcUserName, PBA.fcPushBulletEmail FROM trench_TrenchWars.tblUser AS U"
				+	" JOIN trench_TrenchWars.tblTeamUser AS TU ON TU.fnUserID = U.fnUserID"
				+	" JOIN trench_TrenchWars.tblTeam AS T ON T.fnTeamID = TU.fnTeamID AND fnCurrentTeam = 1"
				+	" JOIN (	SELECT T.fnTeamID FROM trench_TrenchWars.tblTeam AS T"
				+	"		JOIN trench_TrenchWars.tblTeamUser AS TU ON T.fnTeamID = TU.fnTeamID AND fnCurrentTeam = 1"
				+	"		JOIN trench_TrenchWars.tblUser AS U ON TU.fnUserID = U.fnUserID AND U.fcUserName = ?"
				+	"	 ) AS SID ON SID.fnTeamID = T.fnTeamID"
				+	" JOIN trench_TrenchWars.tblPBAccount AS PBA ON U.fnUserID = PBA.fnPlayerID;";
			break; 
			
			
    	}
		return preparedStatement;
    }
    
    public String getUserNameByEmail(String email) {
		String userName = "";
		PreparedStatement ps_getusernamebyemail = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("getusernamebyemail"));
		try {
			ps_getusernamebyemail.clearParameters();
			ps_getusernamebyemail.setString(1, Tools.addSlashesToString(email));
			ps_getusernamebyemail.execute();
			try (ResultSet rs = ps_getusernamebyemail.getResultSet()) {
				if (rs.next()) { 	
					userName = rs.getString(1);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return userName;
    }
    
    public String getEmailByUserName(String userName) {
    	String email = "";
		PreparedStatement ps_getemailbyusername = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("getemailbyusername"));
		try {
			ps_getemailbyusername.clearParameters();
			ps_getemailbyusername.setString(1, Tools.addSlashesToString(userName));
			ps_getemailbyusername.execute();
			try (ResultSet rs = ps_getemailbyusername.getResultSet()) {
				if (rs.next()) { 	
					email = rs.getString(1);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return email;
    }

    public String getInterpretBeep(String userName, String userMsg) {
    	String channelPost = userName + " beeped for: ";
    	String channelPostOriginal = channelPost;
		PreparedStatement ps_getemailbyusername = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("interpretbeep"));
		try {
			ps_getemailbyusername.clearParameters();
			ps_getemailbyusername.setString(1, Tools.addSlashesToString(userMsg));
			ps_getemailbyusername.execute();
			try (ResultSet rs = ps_getemailbyusername.getResultSet()) {
				while (rs.next()) {
					if (channelPost != channelPostOriginal) {channelPost += ",";}
					channelPost += rs.getString("fcCommandShortDescription");
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (channelPost == channelPostOriginal) {channelPost = "";}
    	return channelPost;
    }
    
    public String getSquadChannel(String userName) {
    	String squadChannel = "";
		PreparedStatement ps_getemailbyusername = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("getsquadchannel"));
		try {
			ps_getemailbyusername.clearParameters();
			ps_getemailbyusername.setString(1, Tools.addSlashesToString(userName));
			ps_getemailbyusername.execute();
			try (ResultSet rs = ps_getemailbyusername.getResultSet()) {
				if (rs.next()) { 	
					squadChannel = rs.getString(1);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return squadChannel;
    }

    public Boolean createSquadChannel(String userName, String squadChannel) {
		PreparedStatement ps_getemailbyusername = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("createchannel"));
		try {
			ps_getemailbyusername.clearParameters();
			ps_getemailbyusername.setString(1, Tools.addSlashesToString(userName));
			ps_getemailbyusername.setString(2, Tools.addSlashesToString(squadChannel));
			ps_getemailbyusername.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
    	return true;
    }
    
    public void messagePlayerSquadMembers(String userName, String msg) {
		PreparedStatement ps_getemailbyusername = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("getplayersquadmembers"));
		try {
			ps_getemailbyusername.clearParameters();
			ps_getemailbyusername.setString(1, Tools.addSlashesToString(userName));
			ps_getemailbyusername.execute();
			try (ResultSet rs = ps_getemailbyusername.getResultSet()) {
				while (rs.next()) {
					pbClient.sendNote( null, rs.getString("fcPushBulletEmail"), "", msg);
					m_botAction.sendPublicMessage("Push to :" + rs.getString("fcUserName") + " | " + rs.getString("fcPushBulletEmail") );
				}
			} catch (PushbulletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				m_botAction.sendPublicMessage(e.getMessage());
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			m_botAction.sendPublicMessage(e.getMessage());
		}
    }
        
    public void handleDisconnect() {
        //ba.closePreparedStatement(db, connectionID, this.ps_signup);
        ba.cancelTasks();
    }
    
    /**
     * The LoggedOn event is fired when the bot has logged on to the system, but
     * has not yet entered an arena.  A normal SS client would automatically join
     * a pub; since we do things manually we must tell the bot to join an arena
     * after it has successfully logged in.  Usually the arena is stored in the
     * settings file of the bot, which shares the name of the source and class 
     * files, but has a cfg extension.  
     */
    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botSettings.getString("arena"));
        String pushAuth = ba.getGeneralSettings().getString("PushAuth");
        pbClient = new PushbulletClient(pushAuth);
        StartPushbulletListener();
    }


    /**
     * Set ReliableKills 1 (*relkills 1) to make sure your bot receives every packet.
     * If you don't set reliable kills to 1 (which ensures that every death packet
     * of players 1 bounty and higher [all] will be sent to you), your bot may not
     * get a PlayerDeath event for every player that dies.
     */
    public void handleEvent(ArenaJoined event) {
        m_botAction.setReliableKills(1);
    }
    

    public void StartPushbulletListener() {
    	//m_botAction.sendPublicMessage("1");
	    pbClient.addPushbulletListener(new PushbulletListener(){
	        @Override
	        public void pushReceived(PushbulletEvent pushEvent) {
	        	//m_botAction.sendPublicMessage("2");
	        	List<Push> pushes = null;
				pushes = pushEvent.getPushes();
				Push lastPush = pushes.get(0);
				String body = lastPush.getBody().toString();
				//m_botAction.sendPublicMessage("3");
				String senderEmail = lastPush.getSender_email().toString();
				if (senderEmail == "") { return; } //means it came from the channel, no need to push it back to the channel
				//String type = lastPush.getBody()
				//m_botAction.sendPublicMessage("4");
				String playerName = getUserNameByEmail(senderEmail);
				if (playerName == "") { return; } //means it came from the bot account, probably using !push
				//m_botAction.sendPublicMessage("5");
				String squadChannel = getSquadChannel(playerName);
				if (squadChannel == "") {return; } //means player's squad doesn't have a registered channel
				//m_botAction.sendPublicMessage("6");
				String channelPost = getInterpretBeep(playerName, body);
				//m_botAction.sendPublicMessage("7");
				//m_botAction.sendPublicMessage(type);
				if (channelPost != "") {
					//try{
						//m_botAction.sendPublicMessage("8");
		        	     //pbClient.sendChannelMsg(squadChannel, "", channelPost);
		        	     messagePlayerSquadMembers(playerName, channelPost);
		        	     m_botAction.sendPublicMessage(playerName + " :x: " + channelPost);
		        	     //m_botAction.sendPublicMessage("9");
		        	 //} catch( PushbulletException e ){
		        	     // Huh, didn't work
		        		 //m_botAction.sendPublicMessage(e.getMessage());
		        		 //m_botAction.sendPublicMessage(squadChannel);
		        	 //}
				} else {
					m_botAction.sendPublicMessage(playerName + " : " + body);
					//m_botAction.sendPublicMessage("10");
				}
	        }
	
	        @Override
	        public void devicesChanged(PushbulletEvent pushEvent) {
	        	m_botAction.sendPublicMessage("devicesChanged PushEvent received: " + pushEvent);
	        }
	
	        @Override
	        public void websocketEstablished(PushbulletEvent pushEvent) {
	        	m_botAction.sendPublicMessage("websocketEstablished PushEvent received: " + pushEvent);
	        }
	    });
	    
	    m_botAction.sendPublicMessage("Getting previous pushes to find most recent...");
        try {
			pbClient.getPushes(1);
		} catch (PushbulletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        m_botAction.sendPublicMessage("Starting websocket...try sending a push now.");
        
	    pbClient.startWebsocket();
	    m_botAction.sendPublicMessage("Listening Started!");
    }
    
}