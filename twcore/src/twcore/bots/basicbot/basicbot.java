package twcore.bots.basicbot;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;

/**
 * This is a barebones template bot for use when creating a new bot.  With slight
 * modification, it can suit just about any purpose.  Note that if you are
 * thinking of creating a typical eventbot, you should strongly consider creating
 * a MultiBot module instead, which is more simple; also, you might consider the
 * UltraBot template, which includes a command interpreter for easy command setup.
 *
 * @author  Stefan / Mythrandir
 */
public class basicbot extends SubspaceBot {

    private BotSettings m_botSettings;          // Stores settings for your bot as found in the .cfg file.
                                                // In this case, it would be settings from basicbot.cfg

    /**
     * Creates a new instance of your bot.
     */
    public basicbot(BotAction botAction) {
        super(botAction);
        requestEvents();

        // m_botSettings contains the data specified in file <botname>.cfg
        m_botSettings = m_botAction.getBotSettings();
    }


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
    public void handleEvent(Message event) {
        // Retreive name. If the message is remote, then event.getMessager() returns null, and event.getPlayerID returns a value.
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
            m_botAction.die();
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

}