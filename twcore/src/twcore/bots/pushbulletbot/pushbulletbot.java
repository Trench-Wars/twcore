package twcore.bots.pushbulletbot;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
//import twcore.core.net.MobilePusher;
//import twcore.core.util.Tools;
import java.util.List;
//import java.util.concurrent.Future;
import twcore.core.net.iharder.*;



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
            m_botAction.die("!die by " + name);
        }
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!getlink")) {
        	m_botAction.sendSmartPrivateMessage(name, "https://www.pushbullet.com/channel?tag=envysquad");
        }
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!challenge")) {
        	String msg;
        	msg = "(MatchBot3)>Axwell is challenging you for a game of 3vs3 TWJD versus Rage. Captains/assistants, ?go twjd and pm me with '!accept Rage'";
        	
        	try {
				pbClient.sendChannelMsg( "envysquad", "", msg);
				m_botAction.sendPublicMessage(msg);
			} catch (PushbulletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!accept")) {
        	String msg;
        	msg = "(MatchBot3)>A game of 3vs3 TWJD versus Rage will start in ?go twjd in 30 seconds";
        	
        	try {
				pbClient.sendChannelMsg( "envysquad", "", msg);
				m_botAction.sendPublicMessage(msg);
			} catch (PushbulletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!beep ")) {
        	String msg;
        	msg = event.getMessage();
        	msg = msg.substring(msg.indexOf(" ") + 1);
        	
        	String postMsg;
        	postMsg = "";
        	
        	if (msg.toLowerCase().contentEquals("jd")) {postMsg = name + " is in for a TWJD match!";}
        	if (msg.toLowerCase().contentEquals("dd")) {postMsg = name + " is in for a TWDD match!";}
        	if (msg.toLowerCase().contentEquals("bd")) {postMsg = name + " is in for a TWBD match!";}
        	if (msg.toLowerCase().contentEquals("sd")) {postMsg = name + " is in for a TWSD match!";}
        	if (msg.toLowerCase().contentEquals("fd")) {postMsg = name + " is in for a TWFD match!";}
        	if (msg.toLowerCase().contentEquals("any")) {postMsg = name + " is in for any TWD match!";}
        	
        	if (postMsg != "") {
	        	 try{
	        	     pbClient.sendChannelMsg( "envysquad", "", postMsg);
	        	     m_botAction.sendPublicMessage(postMsg);
	        	 } catch( PushbulletException e ){
	        	     // Huh, didn't work
	        	 }
        	}
        }
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!push ")) {
        	//m_botAction.sendSmartPrivateMessage(name, "Message: '" + msg + "' Pushed Successfully!");
        	
        	
        	String msg;
        	msg = event.getMessage();
        	msg = msg.substring(msg.indexOf(" ") + 1);
        	
        	
        	 try{
        		 pbClient.sendNote( null, getEmailFromPlayerName(name), "", msg);
        		 m_botAction.sendPublicMessage("Private Message: '" + msg + "' Pushed Successfully!");
        		 //m_botAction.sendSmartPrivateMessage(name, "Private Message: '" + msg + "' Pushed Successfully!");
        	     //pbClient.sendNote( null, "tapeboy27@gmail.com", "", msg);
        	     //pbClient.sendNote( null, "trenchwars24@gmail.com", "", msg);
        	     //pbClient.sendNote( null, "khaitran-@hotmail.com", "", msg);
        	     //pbClient.sendNote( null, "dugwyler@gmail.com", "", msg);
        	     
        	     
        	 } catch( PushbulletException e ){
        	     // Huh, didn't work
        	 }
       	
            
        }
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!pushchannel ")) {
        	//m_botAction.sendSmartPrivateMessage(name, "Message: '" + msg + "' Pushed Successfully!");
        	
        	
        	String msg;
        	msg = event.getMessage();
        	msg = msg.substring(msg.indexOf(" ") + 1);
        	
        	 try{
        	     pbClient.sendChannelMsg( "envysquad", "", msg);
        	     m_botAction.sendPublicMessage("Channel Message: '" + msg + "' Pushed Successfully!");
        	     //m_botAction.sendSmartPrivateMessage(name, "Channel Message: '" + msg + "' Pushed Successfully!");
        	 } catch( PushbulletException e ){
        	     // Huh, didn't work
        	 }
        }

        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!pushchannel2 ")) {
        	//m_botAction.sendSmartPrivateMessage(name, "Message: '" + msg + "' Pushed Successfully!");
        	
        	
        	String msg;
        	msg = event.getMessage();
        	msg = msg.substring(msg.indexOf(" ") + 1);
        	
        	 try{
        	     pbClient.sendChannelMsg( "envysquad", "", msg);
        	     m_botAction.sendSmartPrivateMessage(name, "Channel Message: '" + msg + "' Pushed Successfully!");
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
        String pushChannel = m_botSettings.getString("PushChannel");
        //mobilePusher = new MobilePusher(pushAuth, pushChannel, 1);
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
    
    /**
     * This is temporary and will be replaced with a database call matching up email to player name.
     * @param email
     * @return
     */
    public String getPlayerNameFromEmail(String email) {
    	String playerName = "";
    	if (email.contentEquals("trenchwars24@gmail.com")) { playerName = "24";}
    	if (email.contentEquals("tapeboy27@gmail.com")) { playerName = "Board";}
    	//if (email.contentEquals("khaitritran@gmail.com")) { playerName = "Kado";}
    	if (email.contentEquals("khaitran-@hotmail.com")) { playerName = "Kado";} 
    	if (email.contentEquals("twpushbulletbot@gmail.com")) { playerName = "PushBulletBot";}
    	if (email.contentEquals("dugwyler@gmail.com")) {playerName = "qan";}
    	if (email.contentEquals("buesingftw@gmail.com")) {playerName = "Hack";}
    	
    	//m_botAction.sendPublicMessage(playerName + " : " + email.toLowerCase());
    	return playerName;
    }
    
    /**
     * This is temporary and will be replaced with a database call matching up email to player name.
     * @param email
     * @return
     */
    public String getEmailFromPlayerName(String playerName) {
    	String email = "";
    	if (playerName.toLowerCase().contentEquals("24")) { email = "trenchwars24@gmail.com";}
    	if (playerName.toLowerCase().contentEquals("board")) { email = "tapeboy27@gmail.com";}
    	//if (playerName.toLowerCase().contentEquals("Kado")) { email = "khaitritran@gmail.com";}
    	if (playerName.toLowerCase().contentEquals("kado")) { email = "khaitran-@hotmail.como";} 
    	if (playerName.toLowerCase().contentEquals("pushbulletbot")) { email = "twpushbulletbot@gmail.com";}
    	if (playerName.toLowerCase().contentEquals("qan")) {email = "dugwyler@gmail.com";}
    	if (playerName.toLowerCase().contentEquals("hack")) {email = "buesingftw@gmail.com";}
    	//m_botAction.sendPublicMessage(email + " : " + playerName.toLowerCase());
    	return email;
    }
    
    
    
    
    
    public void StartPushbulletListener() {
	    pbClient.addPushbulletListener(new PushbulletListener(){
	        @Override
	        public void pushReceived(PushbulletEvent pushEvent) {
	        	
	        	List<Push> pushes = null;
				pushes = pushEvent.getPushes();
				Push lastPush = pushes.get(0);
				String body = lastPush.getBody().toString();
				String senderEmail = lastPush.getSender_email().toString();
				if (senderEmail == "") { return; } //means it came from the channel, no need to push it back to the channel
				//String type = lastPush.getBody()
				String playerName = getPlayerNameFromEmail(senderEmail);
	        	String msg = body;
				
				if (body.toLowerCase().contentEquals("jd")) {
					msg = playerName + " is in for a TWJD match!";
				}

				if (body.toLowerCase().contentEquals("bd")) {
					msg = playerName + " is in for a TWBD match!";
				}
				
				if (body.toLowerCase().contentEquals("dd")) {
					msg = playerName + " is in for a TWDD match!";
				}
				
				if (body.toLowerCase().contentEquals("sd")) {
					msg = playerName + " is in for a TWSD match!";
				}

				if (body.toLowerCase().contentEquals("fd")) {
					msg = playerName + " is in for a TWFD match!";
				}
				
				if (body.toLowerCase().contentEquals("any")) {
					msg = playerName + " is in for any TWD match!";
				}
				
				
				//m_botAction.sendPublicMessage(type);
				if (msg != body) {
					try{
		        	     pbClient.sendChannelMsg( "envysquad", "", msg);
		        	     m_botAction.sendPublicMessage(playerName + " :x: " + msg);
		        	 } catch( PushbulletException e ){
		        	     // Huh, didn't work
		        	 }
				} else {
					m_botAction.sendPublicMessage(playerName + " : " + msg);
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